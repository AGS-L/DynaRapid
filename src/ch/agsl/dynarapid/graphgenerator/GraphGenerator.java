/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.graphgenerator;
import ch.agsl.dynarapid.databasegenerator.*;
import ch.agsl.dynarapid.debug.*;
import ch.agsl.dynarapid.entry.*;
import ch.agsl.dynarapid.error.*;
import ch.agsl.dynarapid.graphgenerator.*;
import ch.agsl.dynarapid.graphplacer.*;
import ch.agsl.dynarapid.interrouting.*;
import ch.agsl.dynarapid.map.*;
import ch.agsl.dynarapid.modules.*;
import ch.agsl.dynarapid.parser.*;
import ch.agsl.dynarapid.pblockgenerator.*;
import ch.agsl.dynarapid.placer.*;
     
import ch.agsl.dynarapid.strings.*;
import ch.agsl.dynarapid.synthesizer.*;
import ch.agsl.dynarapid.tclgenerator.*;
import ch.agsl.dynarapid.vivado.*;

//This uses the dot parser and the database parser to generate an un-modified graph of nodes or a list of components for which we need to run the pblock generator / database generator

import com.google.protobuf.MapEntryLite;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Module;
import com.xilinx.rapidwright.design.ModuleInst;
import com.xilinx.rapidwright.design.SiteInst;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.device.Tile;
import com.xilinx.rapidwright.device.SiteTypeEnum;
import com.xilinx.rapidwright.device.TileTypeEnum;
import com.xilinx.rapidwright.device.helper.TileColumnPattern;
import com.xilinx.rapidwright.edif.EDIFCell;
import com.xilinx.rapidwright.edif.EDIFDirection;
import com.xilinx.rapidwright.edif.EDIFNet;
import com.xilinx.rapidwright.edif.EDIFNetlist;
import com.xilinx.rapidwright.tests.CodePerfTracker;
import com.xilinx.rapidwright.examples.SLRCrosserGenerator;
import com.xilinx.rapidwright.design.blocks.PBlock;
import com.xilinx.rapidwright.router.Router;
import com.xilinx.rapidwright.placer.handplacer.HandPlacer;
import com.xilinx.rapidwright.rwroute.RWRoute;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.*;

public class GraphGenerator {

    //This generates the dot file for the required non-synthesized components
    public static void generateDotFile(HashSet<String> notFoundSynth, Map<String, Node> nodes, String graphName)
    {
        String dotLoc = LocationParser.designs + graphName + "/" + graphName + "_synth.dot"; 
        StringUtils.printIntro("Generating Synthesis Dot file: " + dotLoc);

        try
        {
            File file = new File(dotLoc);
            FileWriter fileWriter = new FileWriter(file);

            for(String s : notFoundSynth)
                for (Map.Entry<String,Node> entry : nodes.entrySet()) 
                    if(entry.getValue().dcpName.equals(s))
                    {
                        fileWriter.write(entry.getValue().actualString + "\n");
                        break;
                    }
        

            fileWriter.close();
        }

        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not generate the dot file for the un-synthesized componenets");
            return;
        }

        System.out.println("Completed generating dot file for un-synthesized components");
    }
    
    //This generates the list of databases not found
    public static HashSet<String> getDatabaseNotFound(HashSet<String> dcpNames)
    {
        HashSet<String> notFound = new HashSet<>();
        String dataLoc = "";
        String binaryDataLoc = "";

        for(String s : dcpNames)
        {
            dataLoc = LocationParser.placedRoutedDCPs + s + ".data";
            binaryDataLoc = LocationParser.placedRoutedDCPs + s + ".bin.data";

            File dataFile = new File(dataLoc);
            File binFile = new File(binaryDataLoc);

            if(!dataFile.exists() && !binFile.exists())
                notFound.add(s);
        }

        return notFound;
    }

    //this generates all the dcpNames of the components in the graph
    public static HashSet<String> getAllDCPNames(Map<String, Node> nodes)
    {
        HashSet<String> dcpNames = new HashSet<>();

        for (Map.Entry<String,Node> entry : nodes.entrySet()) 
        {
            Node node = entry.getValue();
            String dcpName = node.dcpName;

            if(!dcpNames.contains(dcpName))
                dcpNames.add(dcpName);
        }

        return dcpNames;
    }

    public static Map<String, Node> removeUnfoundNodes(HashSet<String> notFound, HashSet<String> notFoundSynth, HashSet<String> dcpNames, Map<String, Node> nodes)
    {
        ArrayList<String> toRemove = new ArrayList<>();

        for (Map.Entry<String,Node> entry : nodes.entrySet()) 
        {
            Node node = entry.getValue();
            String dcpName = node.dcpName;
            String name = node.name;

            if(notFoundSynth.contains(dcpName))
            {
                dcpNames.remove(dcpName);
                ErrorLogger.addError("Node ignored", 0, name + " has been ignored since its dcp: " + dcpName + " has not been synthesized");
                if(!node.removeItself())
                {
                    System.out.println("ERROR: Could not remove node: " + name + ". See above logs");
                    return null;
                }

                toRemove.add(name);
            }

            else if(notFound.contains(dcpName))
            {
                dcpNames.remove(dcpName);
                ErrorLogger.addError("Nodes ignored", 0, name + " has been ignored since its dcp: " + dcpName + " has not been generated");
                if(!node.removeItself())
                {
                    System.out.println("ERROR: Could not remove node: " + name + ". See above logs");
                    return null;
                }
                toRemove.add(name);
            }
        }

        for(String s : toRemove)
            nodes.remove(s);

        return nodes;
    }

    //This generates the un-modified graph from the dot file and fills all the fields ready for the placement algorithm
    public static Map<String, Node> generateGraph(String graphName, String dotLoc, int threads, boolean debug)
    {

        //////////////////////////////////////////////////////////////////////////////////////////////////////
        if(!TimeProfiler.addAndStartTimeElement("Parsing Dot File", "Graph Generation"))
            return null;

        StringUtils.printIntro("Generating graph from: " + dotLoc);

        Map<String, Node> nodes = DotParser.parser(dotLoc);

        if(nodes == null)
        {
            System.out.println("ERROR: Could not parse dot file. See above logs");
            return null;
        }

        System.out.println("Completed parsing the dot file. Number of nodes are: " + nodes.size());
        if(!TimeProfiler.endTimeElement("Parsing Dot File"))
            return null;

        //////////////////////////////////////////////////////////////////////////////////////////////////////
        
        if(!TimeProfiler.addAndStartTimeElement("Checking For Valid Databases", "Graph Generation"))
            return null;

        HashSet<String> dcpNames = getAllDCPNames(nodes);
        HashSet<String> notFound = getDatabaseNotFound(dcpNames);
        HashSet<String> notFoundSynth = new HashSet<>();
        
        if(notFound.size() != 0)
        {
            StringUtils.printIntro("Generating  Information for un-found databases");

            System.out.println("Following DCPs donot have their databases but have the synthesized DCPs: ");
            for(String s : notFound)
            {
                File vhdlSynth = new File(LocationParser.vhdlSynthDCPs + s + "_synth.dcp");
                File genericSynth = new File(LocationParser.genericSynthDCPs + s + "_synth.dcp");

                if(vhdlSynth.exists() || genericSynth.exists())
                    System.out.println(s);

                else
                    notFoundSynth.add(s);
    
            }

            if(notFoundSynth.size() > 0)
                System.out.println("\nFollowing DCPs donot have their synthesized DCPs (in both generic and vhdl form): ");
                
            for(String s : notFoundSynth)
                System.out.println(s);

            if(debug)
                generateDotFile(notFoundSynth, nodes, graphName);

            
            StringUtils.printIntro("Removing nodes whose databases were absent");
            nodes = removeUnfoundNodes(notFound, notFoundSynth, dcpNames, nodes);
            if(nodes == null)
            {
                System.out.println("ERROR: Could not remove all the nodes from the graph whose dcps were not found. See above logs");
                return null;
            }
        }

        System.out.println("Going on to parse the databases");

        if(!TimeProfiler.endTimeElement("Checking For Valid Databases"))
            return null;

        //////////////////////////////////////////////////////////////////////////////////////////////////////

        if(!TimeProfiler.addAndStartTimeElement("Parsing Databases", "Graph Generation"))
            return null;

        Device device = Device.getDevice("xcvu13p-fsga2577-1-i");

        StringUtils.printIntro("Parsing all required databases");

        int customThreadCount = threads; // Change this to your desired number of threads
        ExecutorService executor = Executors.newFixedThreadPool(customThreadCount);
        Map<String, Component> components = new ConcurrentHashMap<>();

        try 
        {
            for (String s : dcpNames) 
            {
                executor.submit(() -> 
                {
                    DatabaseParser databaseObj = new DatabaseParser();
                    Component component = databaseObj.parser(s, device);

                    if (component == null) 
                        System.err.println("ERROR: Could not parse module: " + s);
                    
                    else 
                        components.put(s, component);
                });
            }
        } 
        finally 
        {
            executor.shutdown();
        }

        try 
        {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } 
        
        catch (InterruptedException e) 
        {
            System.err.println("ERROR: Thread execution interrupted: " + e.getMessage());
            return null;
        }

        for (Map.Entry<String,Node> entry : nodes.entrySet()) 
        {
            Node node = entry.getValue();
            String dcpName = node.dcpName;

            node.updateComponent(components.get(dcpName));
        }

        StringUtils.printIntro("Completed parsing all the required databases.");

        if(!TimeProfiler.endTimeElement("Parsing Databases"))
            return null;

        //////////////////////////////////////////////////////////////////////////////////////////////////////
            
        return nodes;
    }

    //This helps to print the graph
    public static void printGraph(Map<String, Node> nodes)
    {
        StringUtils.printIntro("Printing graph");
        for (Map.Entry<String,Node> entry : nodes.entrySet()) 
        {
            Node node = entry.getValue();
            node.printNode();
        }
    }
}
