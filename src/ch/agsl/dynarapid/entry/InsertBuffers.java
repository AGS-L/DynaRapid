/*
 * DynaRapid
 *
 * This file is part of DynaRapid project
 * Copyright: See COPYING file that comes with this distribution
 * For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
 */

package ch.agsl.dynarapid.entry;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

import ch.agsl.dynarapid.strings.DotString;

public class InsertBuffers {
    private static Map<String, Node> nodes = new LinkedHashMap<>();
    private static List<Edge> edges = new ArrayList<>();

    // Maps each original edge line to the chain of segments that will replace it.
    // A chain starts as a single entry (the line itself) and grows as buffers are inserted.
    private static Map<String, List<String>> edgeReplacementMap = new HashMap<>();

    // Maps each node definition line to the buffer definitions that should follow it in the output.
    private static Map<String, List<String>> lineBufferDefinitions = new LinkedHashMap<>();

    private static boolean debugEnabled = false;

    // Connections between nodes (components)
    private static class Edge {
        String u, v, fromPort, toPort, color;
        int width;              // Data width, resolved from the source node's output port
        String originalLine;    // Key into edgeReplacementMap
        String currentString;   // DOT representation of this specific segment in the chain

        Edge(String u, String v, String fromPort, String toPort, String color, int width, String originalLine, String currentString) {
            this.u = u;
            this.v = v;
            this.fromPort = fromPort;
            this.toPort = toPort;
            this.color = color;
            this.width = width;
            this.originalLine = originalLine;
            this.currentString = currentString;
        }
    }

    // dynamatic components
    private static class Node {
        int latency;
        double delay;
        String type;
        boolean isOperator;
        String definitionLine;           // Trimmed DOT line that declares this node
        Map<String, Integer> portWidths; // Port name -> bit width, e.g. "out1" -> 32

        Node(int latency, double delay, String type, boolean isOperator, String definitionLine, Map<String, Integer> portWidths) {
            this.latency = latency;
            this.delay = delay;
            this.type = type;
            this.isOperator = isOperator;
            this.definitionLine = definitionLine;
            this.portWidths = portWidths;
        }
    }

    /**
     * Buffer insertion preprocessing
     */
    public static void bufferInsertion(String dotLoc, String dotFolder, String graphName, Double targetPeriod, boolean pipeline, boolean debug) throws IOException {
        debugEnabled = debug;

        List<String> lines = Files.readAllLines(Paths.get(dotLoc));

        if (!loadGraph(lines)) {
            System.out.println("ERROR: Failed to parse dot file for preprocessing");

            return;
        }

        if (targetPeriod != null) {
            System.out.println("Starting: Buffer insertion to cut down critical path");

            insertThresholdBuffers(targetPeriod);

            System.out.println("Stopping: Buffer insertion to cut down critical path");

            if (debugEnabled) {
                saveGraph(lines, dotFolder + "/" + graphName + "_buffered.dot");
            }
        }

        if (pipeline) {
            System.out.println("Starting: Buffer insertion to balance latencies");

            balanceLatencies();

            System.out.println("Stopping: Buffer insertion to balance latencies");
        }

        saveGraph(lines, dotLoc);
    }

    /**
     * Splits edges whose cumulative combinational delay exceeds the target period by inserting a buffer.
     */
    public static void insertThresholdBuffers(double threshold) {
        Set<Integer> edgesToSplit = new HashSet<>();
        
        if (debugEnabled) {
            System.out.println("DFS for buffer insertion:");
        }

        for (String id : new ArrayList<>(nodes.keySet())) {
            Node n = nodes.get(id);

            if (n != null && n.isOperator) {
                dfsMarkThreshold(id, n.delay, threshold, edgesToSplit, new LinkedHashSet<>());
            }
        }

        List<Edge> nextEdges = new ArrayList<>();
        int bufferCounter = 0;

        for (int i = 0; i < edges.size(); i++) {
            Edge e = edges.get(i);

            if (edgesToSplit.contains(i)) {
                String bufferName;

                do {
                    bufferName = "_buffer_delay_" + bufferCounter++;
                } while (nodes.containsKey(bufferName));

                defineBuffer(bufferName, "darkolivegreen3", e.u, e.width);

                String s1 = String.format("\"%s\" -> \"%s\" [color=\"%s\", from=\"%s\", to=\"in1\"];", e.u, bufferName, e.color, e.fromPort);
                String s2 = String.format("\"%s\" -> \"%s\" [color=\"%s\", from=\"out1\", to=\"%s\"];", bufferName, e.v, e.color, e.toPort);

                updateReplacementChain(e.originalLine, e.currentString, Arrays.asList(s1, s2));

                nextEdges.add(new Edge(e.u, bufferName, e.fromPort, "in1", e.color, e.width, e.originalLine, s1));
                nextEdges.add(new Edge(bufferName, e.v, "out1", e.toPort, e.color, e.width, e.originalLine, s2));
            } else {
                nextEdges.add(e);
            }
        }

        edges = nextEdges;
    }

    /**
     * Computes the latest-arrival time at each node and inserts pipeline buffers on edges, where the source arrives earlier than needed, so all inputs to a node are co-timed.
     */
    public static void balanceLatencies() {
        List<String> sorted = performTopologicalSort(nodes.keySet(), edges);
        Map<String, Integer> timing = new HashMap<>();

        nodes.keySet().forEach(n -> timing.put(n, 0));

        // Propagate the maximum arrival time forward through the graph
        for (String node : sorted) {
            List<Edge> preds = edges.stream().filter(e -> e.v.equals(node)).collect(Collectors.toList());

            if (!preds.isEmpty()) {
                int maxT = 0;

                for (Edge e : preds) {
                    maxT = Math.max(maxT, timing.get(e.u) + nodes.get(e.u).latency);
                }

                timing.put(node, maxT);
            }
        }

        int bufferCounter = 0;

        for (Edge e : edges) {
            if (debugEnabled) {
                System.out.println(" Working on '" + e.currentString + "'");
            }

            int slack = timing.get(e.v) - (timing.get(e.u) + nodes.get(e.u).latency);

            String uNodeType = nodes.get(e.u).type;
            String vNodeType = nodes.get(e.v).type;

            // Constants and sources have no meaningful latency path, skip them
            if (slack <= 0 || uNodeType.equalsIgnoreCase("Source") || uNodeType.equalsIgnoreCase("Constant") || vNodeType.equalsIgnoreCase("Constant")) {
                if (debugEnabled) {
                    System.out.println(" Skipping: reasons - " + (slack <= 0) + " - "  + uNodeType + " - " + vNodeType);
                }

                continue;
            }

            // Insert `slack` pipeline buffers in series to absorb the timing difference
            List<String> newSubChain = new ArrayList<>();
            String currentSource = e.u;
            String currentPort = e.fromPort;

            for (int i = 0; i < slack; i++) {
                String bufferName;

                do {
                    bufferName = "_buffer_pipeline_" + bufferCounter++;
                } while (nodes.containsKey(bufferName));

                defineBuffer(bufferName, "yellow", e.u, e.width);
                newSubChain.add(String.format("\"%s\" -> \"%s\" [color=\"%s\", from=\"%s\",to=\"in1\"];", currentSource, bufferName, e.color, currentPort));
                currentSource = bufferName;
                currentPort = "out1";
            }

            newSubChain.add(String.format("\"%s\" -> \"%s\" [color=\"%s\", from=\"out1\", to=\"%s\"];", currentSource, e.v, e.color, e.toPort));

            updateReplacementChain(e.originalLine, e.currentString, newSubChain);
        }
    }

    /**
     * Replaces one segment of an edge chain with a list of new segments.
     * An original edge line may be split multiple times across the two passes, so we operate on the chain rather than the original string directly.
     */
    private static void updateReplacementChain(String originalLine, String targetSegment, List<String> replacements) {
        List<String> currentChain = edgeReplacementMap.get(originalLine);
        List<String> nextChain = new ArrayList<>();

        for (String segment : currentChain) {
            if (segment.trim().equals(targetSegment.trim())) {
                nextChain.addAll(replacements);
            } else {
                nextChain.add(segment);
            }
        }

        edgeReplacementMap.put(originalLine, nextChain);
    }

    /**
     * Writes buffer definitions after their parent, recursing to handle chains where a delay buffer is itself the parent of one or more pipeline buffers.
     */
    private static void writeBufferDefsRecursive(PrintWriter writer, List<String> defs, String leadingSpaces) {
        for (String def : defs) {
            writer.println(leadingSpaces + def);

            List<String> children = lineBufferDefinitions.get(def);

            if (children != null) {
                writeBufferDefsRecursive(writer, children, leadingSpaces);
            }
        }
    }

    /**
     * Creates a buffer node and registers its definition to be emitted after its parent node.
     * The buffer's own definition line is stored as its definitionLine so it can itself act as a parent if pipeline buffers are later chained off it.
     */
    private static void defineBuffer(String name, String color, String parentNodeId, int width) {
        String type = "Buffer";
        String bufferDefinition = String.format("\"%s\" [type = \"%s\", in = \"in1:%d\", out = \"out1:%d\", latency = 1, slots = 1, fillcolor = %s, shape = box, style = filled];", name, type, width, width, color);

        Node parent = nodes.get(parentNodeId);
        String parentDefLine = (parent != null && parent.definitionLine != null) ? parent.definitionLine : parentNodeId;
        lineBufferDefinitions.computeIfAbsent(parentDefLine, k -> new ArrayList<>()).add(bufferDefinition);

        Map<String, Integer> portWidths = new HashMap<>();
        portWidths.put("in1", width);
        portWidths.put("out1", width);
        nodes.put(name, new Node(1, 0.0, type, false, bufferDefinition, portWidths));
    }

    /**
     * Parses port declarations of the form 'in = "in1:32 in2:1"' and returns a port -> width map
     */
    private static Map<String, Integer> parsePortWidths(String attributes) {
        Map<String, Integer> widths = new HashMap<>();
        Matcher portAttributeMatcher = Pattern.compile("(?:in|out)\\s*=\\s*\"([^\"]+)\"").matcher(attributes);
        Pattern portTokenPattern = Pattern.compile("([a-zA-Z][a-zA-Z0-9]*):(\\d+)");

        while (portAttributeMatcher.find()) {
            Matcher tokenMatcher = portTokenPattern.matcher(portAttributeMatcher.group(1));

            while (tokenMatcher.find()) {
                widths.put(tokenMatcher.group(1), Integer.parseInt(tokenMatcher.group(2)));
            }
        }

        return widths;
    }

    /**
     * Load a file (String split into lines) as a graph.
     */
    private static boolean loadGraph(List<String> lines) {
        Pattern nodeLinePattern = Pattern.compile("\"([^\"]+)\"\\s*\\[(.*?type\\s*=.*?)\\];");
        Pattern latencyPattern = Pattern.compile("latency\\s*=\\s*(\\d+)");
        Pattern delayPattern = Pattern.compile("delay\\s*=\\s*\"?([0-9]*\\.?[0-9]+)");
        Pattern typePattern = Pattern.compile("type\\s*=\\s*\"?([a-zA-Z]*\\.?[a-zA-Z]+)");

        for (String line : lines) {
            Matcher nodeMatcher = nodeLinePattern.matcher(line);

            if (nodeMatcher.find()) {
                String id = nodeMatcher.group(1);
                String attributes = nodeMatcher.group(2);

                Matcher typeMatcher = typePattern.matcher(attributes);

                if (!typeMatcher.find()) {
                    System.out.println("ERROR: Failed to parse node type");
                    System.out.println("Line: " + attributes);

                    return false;
                }

                String type = typeMatcher.group(1);

                Matcher latencyMatcher = latencyPattern.matcher(attributes);
                int latency = latencyMatcher.find() ? Integer.parseInt(latencyMatcher.group(1)) : 0;

                if (type.equalsIgnoreCase("Buffer") && latency == 0) {
                    System.out.println("WARNING: Buffer '" + id + "' has undefined latency, using latency 1 as default for buffer insertion analysis");

                    latency = 1;
                }

                Matcher delayMatcher = delayPattern.matcher(attributes);
                double delay = delayMatcher.find() ? Double.parseDouble(delayMatcher.group(1)) : 0.0;

                nodes.put(id, new Node(latency, delay, type, attributes.toLowerCase().contains("operator"), line.trim(), parsePortWidths(attributes)));
            }
        }

        Pattern edgePattern = Pattern.compile("\"([^\"]+)\"\\s*->\\s*\"([^\"]+)\"\\s*\\[(.*?)from\\s*=\\s*\"([^\"]+)\",\\s*to\\s*=\\s*\"([^\"]+)\"(.*?)\\];");
        Pattern colorPattern = Pattern.compile("color\\s*=\\s*\"?([^\"\\s,\\]]+)\"?");

        for (String line : lines) {
            Matcher edgeMatcher = edgePattern.matcher(line);

            if (edgeMatcher.find()) {
                String trimmed = line.trim();
                String srcNode = edgeMatcher.group(1);
                String fromPort = edgeMatcher.group(4);

                // color may appear before or after from/to, so search both surrounding attribute groups
                String remainingAttributes = edgeMatcher.group(3) + edgeMatcher.group(6);
                Matcher colorMatcher = colorPattern.matcher(remainingAttributes);
                String color = colorMatcher.find() ? colorMatcher.group(1) : null;

                // Width comes from the source node's declared output port; fall back to 32 if absent
                int width = 32;
                Node srcNodeObj = nodes.get(srcNode);

                if (srcNodeObj != null && srcNodeObj.portWidths.containsKey(fromPort)) {
                    width = srcNodeObj.portWidths.get(fromPort);
                }

                edges.add(new Edge(srcNode, edgeMatcher.group(2), fromPort, edgeMatcher.group(5), color, width, trimmed, trimmed));
                edgeReplacementMap.put(trimmed, new ArrayList<>(Collections.singletonList(trimmed)));
            }
        }

        return true;
    }

    private static void saveGraph(List<String> lines, String outputPath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            for (String line : lines) {
                String trimmedLine = line.trim();
                String leadingSpaces = DotString.getLeadingSpaces(line);

                if (edgeReplacementMap.containsKey(trimmedLine)) {
                    List<String> chain = edgeReplacementMap.get(trimmedLine);

                    if (chain.size() > 1 || !chain.get(0).equals(trimmedLine)) {
                        for (String edge : chain) {
                            writer.println(leadingSpaces + edge);
                        }
                        continue;
                    }
                }

                writer.println(line);

                // Emit any buffer definitions whose parent is this node definition
                List<String> defs = lineBufferDefinitions.get(trimmedLine);

                if (defs != null) {
                    writeBufferDefsRecursive(writer, defs, leadingSpaces);
                }
            }
        }
    }

    /**
     * DFS that accumulates combinational delay along Buffer-to-Buffer paths.
     * When adding the next component's delay would exceed the limit, the connecting edge is marked for splitting.
     * The DFS then resets the accumulator to just that component's own delay, since the inserted buffer acts as a pipeline stage boundary.
     */
    private static void dfsMarkThreshold(String uId, double cumulative, double limit, Set<Integer> marked, Set<String> visited) {
        if (visited.contains(uId)) {
            return;
        }

        visited.add(uId);

        for (int i = 0; i < edges.size(); i++) {
            Edge e = edges.get(i);

            if (e.u.equals(uId)) {
                Node v = nodes.get(e.v);

                if (debugEnabled) {
                    System.out.println(String.format(" %.3f - ", cumulative) + visited);
                }

                // Take already inserted buffers into account
                if (v != null && !marked.contains(i) && !v.type.equalsIgnoreCase("Buffer") && !v.type.equalsIgnoreCase("Exit")) {
                    // If the next component adds too much delay, insert a buffer
                    if (cumulative + v.delay > limit && cumulative > 0.0) {
                        marked.add(i);

                        if (debugEnabled) {
                            System.out.println(String.format("  Inserted buffer: %s - %s (%.3f + %.3f)", e.u, e.v, cumulative, v.delay));

                            if (v.delay > limit) {
                                System.out.println(String.format("  WARNING: %s has a longer delay (%.3f) than the specified period (%.3f)", e.v, v.delay, limit));
                            }
                        }

                        dfsMarkThreshold(e.v, v.delay, limit, marked, visited);
                    } else {
                        dfsMarkThreshold(e.v, cumulative + v.delay, limit, marked, visited);
                    }
                }
            }
        }

        visited.remove(uId);
    }

    private static List<String> performTopologicalSort(Set<String> allNodes, List<Edge> edges) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();

        allNodes.forEach(n -> {
            inDegree.put(n, 0);
            adj.put(n, new ArrayList<>());
        });

        for (Edge e : edges) {
            adj.get(e.u).add(e.v);
            inDegree.put(e.v, inDegree.get(e.v) + 1);
        }

        // Get the top nodes, which don't have parents
        Queue<String> q = new LinkedList<>();
        inDegree.forEach((id, deg) -> {
            if (deg == 0) {
                q.add(id);
            }
        });

        List<String> sorted = new ArrayList<>();

        while (!q.isEmpty()) {
            String u = q.poll();
            sorted.add(u);

            for (String v : adj.get(u)) {
                inDegree.put(v, inDegree.get(v) - 1);

                if (inDegree.get(v) == 0) {
                    q.add(v);
                }
            }
        }

        return sorted;
    }
}