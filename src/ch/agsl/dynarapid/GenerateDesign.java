/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid;


import ch.agsl.dynarapid.map.MapElement;
import ch.agsl.dynarapid.debug.TimeProfiler;

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


//This has the main function of the designGenerator.
import com.xilinx.rapidwright.device.Site;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.lang.*;
import java.nio.file.*;



public class GenerateDesign {

    public static String fpga_part = "xcvu13p-fsga2577-1-i";
    
    public static void deleteDirectory(File sourceDir)
    {
        for (File subfile : sourceDir.listFiles()) 
        {
            if (subfile.isDirectory()) {
                deleteDirectory(subfile);
            }
 
            subfile.delete();
        }

        sourceDir.delete();
    }

    public static void getConstrainValues(int [] constrainCoordinates) throws IOException
    {
        Scanner in = new Scanner(System.in);
        boolean confirm = false;

        StringUtils.printIntro("Starting FPGA constrain directive");
        System.out.println("> FPGA map has " + MapElement.map.size() + " rows. So the top-row constrain and bottom-row constrain must range between [0:" + (MapElement.map.size()-1) + "]");
        System.out.println("> FPGA map has " + MapElement.map.get(0).size() + " columns. So the left-column constrain and right-column constrain must range between [0:" + (MapElement.map.get(0).size()-1) + "]");
        System.out.println("> Make sure that the design center is within the specified constraints or else an ERROR will be reported and design generation will be aborted");

        int topRow, bottomRow, leftCol, rightCol;
        topRow = 0;
        bottomRow = MapElement.map.size()-1;
        leftCol = 0;
        rightCol = MapElement.map.get(0).size()-1;

        while(!confirm)
        {
            System.out.println();
            System.out.print("> Top-most row constrain: [0:" + (MapElement.map.size()-1) + "]: ");
            topRow = in.nextInt();
            if(topRow >= MapElement.map.size())
                topRow = MapElement.map.size()-1;
            else if(topRow < 0)
                topRow = 0;

            System.out.print("> Bottom-most row constrain: [" + (topRow + 1) + ":" + (MapElement.map.size()-1) + "]: ");
            bottomRow = in.nextInt();
            if(bottomRow >= MapElement.map.size())
                bottomRow = MapElement.map.size()-1;
            else if(bottomRow <= topRow)
                bottomRow = topRow+1;

            System.out.print("> Left-most column constrain: [0:" + (MapElement.map.get(0).size()-1) + "]: ");
            leftCol = in.nextInt();
            if(leftCol >= MapElement.map.get(0).size())
                leftCol = MapElement.map.get(0).size()-1;
            else if(leftCol < 0)
                leftCol = 0;

            System.out.print("> right-most column constrain: [" + (leftCol+1) + ":" + (MapElement.map.get(0).size()-1) + "]: ");
            rightCol = in.nextInt();
            if(rightCol >= MapElement.map.get(0).size())
                rightCol = MapElement.map.get(0).size()-1;
            else if(rightCol <= leftCol)
                rightCol = leftCol+1;

            System.out.println();
            System.out.println("> The applied constraints on the fabric are: ");
            System.out.println("> Top-most row: " + topRow);
            System.out.println("> Bottom-most row: " + bottomRow);
            System.out.println("> Left-most column: " + leftCol);
            System.out.println("> Right-most columns: " + rightCol);
            System.out.print("> Confirm? [y/n]: ");
            String s = in.next();
            if(s.equalsIgnoreCase("y"))
                confirm = true;
        }

        System.out.println("FPGA constrained");
        constrainCoordinates[0] = topRow;
        constrainCoordinates[1] = bottomRow;
        constrainCoordinates[2] = leftCol;
        constrainCoordinates[3] = rightCol;
    }

    public static void main(String args[]) throws IOException
    {

        if(!TimeProfiler.addAndStartTimeElement("Design Generation", ""))
            return;

        if((StringUtils.findInArray("-f", args) == -1) || ((StringUtils.findInArray("-place", args) == -1) && (StringUtils.findInArray("-placer", args) == -1)))
        {
            System.out.println("<usage>: java GenerateDesign [-f] [-place] [-placer]");
            System.out.println("-f <arg> - Location of the .dot file.");
            System.out.println("-debug - Runs the toolflow in debug mode generating more information and files.");
            System.out.println("-complete - Rips out the design and then routes it completely");
            System.out.println("-constrain - Allows you to start the design constrain flow to constrain the design within a specific boundary on the fabric.");
            System.out.println("-threads <args> - Number of maximum threads to be used for multi-threading processes. (Without this fields, 16 threads are used)");
            System.out.println("-noClock - This generates the design such that it is generated out of context. The clock pins of all the modules are connected to design ports");
            System.out.println("-place <arg> - Location of the placement information if required");
            System.out.println("-placer <args> - Name of the placer. Options are:");
            System.out.println("\trudimentary");
            System.out.println("\tgreedy");
            System.out.println("-center <arg> - Center of the design.");
            System.out.println("\t<arg> : Can be the name of SLICE site like SLICE_X#_Y#");
            System.out.println("\t<arg> : Can be R<row_number>_C<column_number>_Side<side>. The side can be -1 for left and +1 for right");
            return;
        }

        if(!Start.start("Design Generation"))
            return;

        String dotLoc = args[StringUtils.findInArray("-f", args) + 1];
        String graphName = StringUtils.getGraphName(dotLoc);

        String placeLoc = "";
        String placerName = "";
        int threads = 16;

        if(StringUtils.findInArray("-place", args) != -1)
        {
            placeLoc = args[StringUtils.findInArray("-place", args) + 1];
            placerName = StringUtils.getPlacerNameFromPlaceLoc(graphName, placeLoc);

            if(placerName.equals(""))
            {
                System.out.println("ERROR: Could not find the placer name for the graph name" + graphName + ". Correct placement file naming format");
                return;
            }
        }

        if(StringUtils.findInArray("-placer", args) != -1)
        {
            placerName = args[StringUtils.findInArray("-placer", args) + 1];

            if(!placerName.equals("rudimentary") && !placerName.equals("greedy"))
            {
                System.out.println("ERROR: Could not identify placer name. The options for placer name are :");
                System.out.println("\trudimentary");
                System.out.println("\tgreedy");
                return;
            }
        }

        if(StringUtils.findInArray("-threads", args) != -1)
        {
            threads = Integer.parseInt(args[StringUtils.findInArray("-threads", args) + 1]);
            if(threads <= 0)
            {
                System.out.println("ERROR: Invalid number of threads");
                return;
            }
        }

        boolean noClock = (StringUtils.findInArray("-noClock", args) != -1);
        boolean complete = (StringUtils.findInArray("-complete", args) != -1);
        boolean debug = (StringUtils.findInArray("-debug", args) != -1);

        boolean constrain = (StringUtils.findInArray("-constrain", args) != -1);
        int[] constrainCoordinates = new int[4]; //This has the coordinates of the constraints applied on the fabric. The order of values are: [topRow, bottomRow, leftCol, rightCol]
        constrainCoordinates[0] = 0;
        constrainCoordinates[1] = MapElement.map.size()-1;
        constrainCoordinates[2] = 0;
        constrainCoordinates[3] = MapElement.map.get(0).size()-1;

        boolean isCenterSpecified = false;
        String centerSiteName = "";
        int centerRow, centerCol, centerSide;
        centerRow = centerCol = centerSide = 0;

        if(StringUtils.findInArray("-center", args) != -1)
        {
            isCenterSpecified = true;
            String center = args[StringUtils.findInArray("-center", args) + 1];
            if(center.contains("SLICE"))
                centerSiteName = center;

            else if(center.contains("R") && center.contains("_C") && center.contains("_Side"))
            {
                String pattern = "R(\\d+)_C(\\d+)_Side([-+]?\\d+)";
                Pattern regexPattern = Pattern.compile(pattern);

                Matcher matcher = regexPattern.matcher(center);
                if (matcher.matches()) 
                {
                    centerRow = Integer.parseInt(matcher.group(1));
                    centerCol = Integer.parseInt(matcher.group(2));
                    centerSide = Integer.parseInt(matcher.group(3));
                } 
                else 
                {
                    System.out.println("ERROR: Could not match format of the center string with the desired string format. The desired center format is:");
                    System.out.println("\t<arg> : Can be the name of SLICE site like SLICE_X#_Y#");
                    System.out.println("\t<arg> : Can be R<row_number>_C<column_number>_Side<side>. The side can be -1 for left and +1 for right");
                    return;
                }
            }

            else 
            {
                System.out.println("ERROR: Format of the center argument is not correct. It is mentioned as: " + center + ". The possible arguments can be: ");
                System.out.println("\t<arg> : Can be the name of SLICE site like SLICE_X#_Y#");
                System.out.println("\t<arg> : Can be R<row_number>_C<column_number>_Side<side>. The side can be -1 for left and +1 for right");
                return;
            }
        }

        File sourceDir = new File(LocationParser.designs + graphName + "/");

        ////////////////////////////////////////////////////////////////////////////////////////////////////// 

        if(!TimeProfiler.addAndStartTimeElement("Constraining FPGA", "Design Generation"))
            return;

        if(constrain)
            getConstrainValues(constrainCoordinates);

        if(!TimeProfiler.endTimeElement("Constraining FPGA"))
        {
            deleteDirectory(sourceDir);
            return;
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////// 
        if(!TimeProfiler.addAndStartTimeElement("Environment Creation", "Design Generation"))
            return;

        System.out.println("Creating design directory of location: " + (LocationParser.designs + graphName));
        try
        {
            if(sourceDir.exists())
                deleteDirectory(sourceDir);

            sourceDir.mkdirs();

            File src = new File(dotLoc);
            File dest = new File(LocationParser.designs + graphName + "/" + graphName + ".dot");
            
            System.out.println("Copying dot file");
            if(!TimeProfiler.addAndStartTimeElement("Shifting Dot File", "Environment Creation"))
                return;

            if(!dest.exists())
                Files.copy(src.toPath(), dest.toPath());

            dotLoc = LocationParser.designs + graphName + "/" + graphName + ".dot";
            if(!TimeProfiler.endTimeElement("Shifting Dot File"));

            if(!placeLoc.equals(""))
            {
                if(!TimeProfiler.addAndStartTimeElement("Shifting Placement File", "Environment Creation"))
                    return;

                System.out.println("Copying placement information");
                src = new File(placeLoc);
                dest = new File(LocationParser.designs + graphName + "/" + graphName + "_" + placerName + ".place");

                if(!dest.exists())
                    Files.copy(src.toPath(), dest.toPath());

                placeLoc = LocationParser.designs + graphName + "/" + graphName + "_" + placerName + ".place";

                if(!TimeProfiler.endTimeElement("Shifting Placement File"))
                    return;
            }

        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not make design directory. Exiting design generation");
            deleteDirectory(sourceDir);
            return;
        }
        if(!TimeProfiler.endTimeElement("Environment Creation"))
            return;

        //////////////////////////////////////////////////////////////////////////////////////////////////////

        if(!TimeProfiler.addAndStartTimeElement("Graph Generation", "Design Generation"))
            return;

        StringUtils.printIntro("Starting generating graph for dot file: " + dotLoc);

        System.out.println("Generating graph");
        Map<String, Node> nodes = GraphGenerator.generateGraph(graphName, dotLoc, threads, debug);
        if(nodes == null)
        {
            System.out.println("ERROR: Could not generate the graph. See above logs");
            deleteDirectory(sourceDir);
            return;
        }

        if(!TimeProfiler.endTimeElement("Graph Generation"))
        {
            deleteDirectory(sourceDir);
            return;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////

        Placer obj = null;

        if(placeLoc.equals(""))
        {
            if(!TimeProfiler.addAndStartTimeElement("Graph Placement", "Design Generation"))
            {
                deleteDirectory(sourceDir);
                return;
            }
            
            if(placerName.equals("rudimentary"))
                obj = new RudimentaryPlacer(constrainCoordinates[0], constrainCoordinates[1], constrainCoordinates[2], constrainCoordinates[3]);

            else if(placerName.equals("greedy"))
                obj = new GreedyPlacer(constrainCoordinates[0], constrainCoordinates[1], constrainCoordinates[2], constrainCoordinates[3]);

            else
            {
                System.out.println("ERROR: Could not identify placer name. The options for placer name are :");
                System.out.println("\trudimentary");
                System.out.println("\tgreedy");
                deleteDirectory(sourceDir);
                return;

            }
            
            if(isCenterSpecified)
            {
                boolean isCenterSet = false;

                if(!centerSiteName.equals(""))
                    isCenterSet = obj.setDesignCenterUsingSiteName(centerSiteName);
                else
                    isCenterSet = obj.setDesignCenterUsingCoordinates(centerRow, centerCol, centerSide);

                if(!isCenterSet)
                {
                    System.out.println("ERROR: Could not set the center of the design. Stopping flow. See above logs");
                    deleteDirectory(sourceDir);
                    return;
                }
            }

            StringUtils.printIntro("Starting deciding placement using placer: " + obj.getPlacerName());
            System.out.println("Center Site of the Design is: " + obj.getDesignCenterSite().getName());
            int [] center = obj.getDesignCenterCoordinates();
            System.out.println("Center coordinates of the design is I = " + center[0] + " J = " + center[1] + " side = " + center[2]);

            nodes = obj.placer(nodes);
            placerName = obj.getPlacerName();

            if(!TimeProfiler.endTimeElement("Graph Placement"))
            {
                deleteDirectory(sourceDir);
                return;
            }
        }

        else
        {
            if(!TimeProfiler.addAndStartTimeElement("Parsing Placement Information", "Design Generation"))
            {
                deleteDirectory(sourceDir);
                return;
            }
            System.out.println("Using placement information mentioned in: " + placeLoc);
            nodes = PlacementParser.parser(nodes, placeLoc, placerName, graphName);
            if(!TimeProfiler.endTimeElement("Parsing Placement Information"))
            {
                deleteDirectory(sourceDir);
                return;
            }
        }

        if(nodes == null)
        {
            System.out.println("ERROR: Could not generate placement positions for all the nodes. See above logs");
            deleteDirectory(sourceDir);
            return;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////

        if(!TimeProfiler.addAndStartTimeElement("Placement Statistics", "Design Generation"))
        {
            deleteDirectory(sourceDir);
            return;
        }

        if(debug)
        {

            if(placeLoc.equals(""))
            {
                if(!TimeProfiler.addAndStartTimeElement("Generating Placement Information File", "Placement Statistics"))
                {
                    deleteDirectory(sourceDir);
                    return;
                }

                if(!PlacementInfo.generatePlacementInfo(nodes, graphName, placerName))
                {
                    System.out.println("ERROR: Could not generate the placement information. Exiting design generation");
                    deleteDirectory(sourceDir);
                    return;
                }

                if(!TimeProfiler.endTimeElement("Generating Placement Information File"))
                {
                    deleteDirectory(sourceDir);
                    return;
                }
            }

            if(!TimeProfiler.addAndStartTimeElement("Generating Placement Map", "Placement Statistics"))
            {
                deleteDirectory(sourceDir);
                return;
            }
            if(!PlacementInfo.generatePlacementMap(graphName, placerName, obj))
            {
                System.out.println("ERROR: Could not generate the placement Map. Exiting design generation");
                deleteDirectory(sourceDir);
                return;
            }
            if(!TimeProfiler.endTimeElement("Generating Placement Map"))
            {
                deleteDirectory(sourceDir);
                return;
            }

            if(!TimeProfiler.addAndStartTimeElement("Generating Focussed Placement Map", "Placement Statistics"))
            {
                deleteDirectory(sourceDir);
                return;
            }
            if(!PlacementInfo.generateFocussedPlacementMap(graphName, placerName, obj))
            {
                System.out.println("ERROR: Could not generate the focussed placement Map. Exiting design generation");
                deleteDirectory(sourceDir);
                return;
            }
            if(!TimeProfiler.endTimeElement("Generating Focussed Placement Map"))
            {
                deleteDirectory(sourceDir);
                return;
            }

            if(!TimeProfiler.addAndStartTimeElement("Generating Placement Density Table", "Placement Statistics"))
            {
                deleteDirectory(sourceDir);
                return;
            }
            if(!PlacementInfo.generatePlacementDensity(graphName, placerName, obj, nodes))
            {
                System.out.println("ERROR: Could not generate the placement density table. Exiting design generation");
                deleteDirectory(sourceDir);
                return;
            }
            if(!TimeProfiler.endTimeElement("Generating Placement Density Table"))
            {
                deleteDirectory(sourceDir);
                return;
            }
        }

        if(!TimeProfiler.endTimeElement("Placement Statistics"))
        {
            deleteDirectory(sourceDir);
            return;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////

        if(!TimeProfiler.addAndStartTimeElement("Graph Stitching", "Design Generation"))
        {
            deleteDirectory(sourceDir);
            return;
        }

        StringUtils.printIntro("Starting graph placement and stitching");
        if(!GraphPlacer.graphPlacer(nodes, graphName, complete, threads, debug, noClock))
        {
            System.out.println("ERROR: Could not place graph on FPGA. See above logs");
            deleteDirectory(sourceDir);
            return;
        }

        if(!TimeProfiler.endTimeElement("Graph Stitching"))
        {
            deleteDirectory(sourceDir);
            return;
        }

        System.out.println("Generated DCP");

        //////////////////////////////////////////////////////////////////////////////////////////////////////

        if(!TimeProfiler.endTimeElement("Design Generation"))
        {
            deleteDirectory(sourceDir);
            return;
        }

        String timingLoc = LocationParser.designs + graphName + "/" + graphName + "_" + placerName + ".time";

        if(debug)
            TimeProfiler.printTimingProfile(timingLoc);

        ErrorLogger.printErrorLogs();
    }
}
