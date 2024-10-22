/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/


package ch.agsl.dynarapid.debug;

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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

//This class generates the placement information for the given placer.
import java.util.Map;

public class PlacementInfo {
    
    public static final String tabSpace = "\t";
    public static final String comment = "// ";
    public static final String pointer = "[] - ";
    public static final String comma = ", ";

    public static int coordinates[] = new int[4]; //This holds the coordinates of the smallest possible bounding box. The format of the values in the array is [startRow, startCol, endRow, endCol]. This array is filled by getBoundingBoxCoordinates()

    //This generates the starting information of the placement data
    public static void writeComments(FileWriter fileWriter) throws IOException
    {
        fileWriter.write(comment + "This file has placement information. This file will be parsed by the graph-stitcher to place and stitch the graph\n");
        fileWriter.write(comment + "Following are some pointers that should be used such that the format of this file remains uniform\n");
        fileWriter.write(comment + pointer + "This file should be named - graphName_placerName.place\n");
        fileWriter.write(comment + pointer + "All lines with no characters are skipped\n");
        fileWriter.write(comment + pointer + "Symbol \"//\" can be used for commenting lines out\n");
        fileWriter.write(comment + pointer + "Node names should not have any underscore in them\n");
        fileWriter.write("\n");
        fileWriter.write(comment + pointer + "The file starts with the name of the graph\n");
        fileWriter.write(comment + pointer + "This is followed by the name of the placer. This name will be used for bookkeeping\n");
        fileWriter.write(comment + pointer + "This is followed by the the number of nodes in the graph\n");
        fileWriter.write(comment + pointer + "Each field in the placement line is separated by one or more spaces\n");
        fileWriter.write(comment + pointer + "padding information is space separated and has the order of [left padding, right padding, top padding, bottom padding]\n");
        fileWriter.write("\n");
        fileWriter.write("\n");
    }

    //This generates the first information lines for placement information file
    public static void writeStart(FileWriter fileWriter, String graphName, String placerName, int num) throws IOException
    {
        fileWriter.write("Device: xck26 \n");
        fileWriter.write("Graph Name: " + graphName + "\n");
        fileWriter.write("Placer Name: " + placerName + "\n");
        fileWriter.write("# of Nodes: " + Integer.toString(num) + "\n");
        fileWriter.write("\n");
    }

    //This generates the first information for all the maps and the csv files
    public static void writeStart(FileWriter fileWriter, String graphName, String placerName) throws IOException
    {
        fileWriter.write("Device: xck26 \n");
        fileWriter.write("Graph Name: " + graphName + "\n");
        fileWriter.write("Placer Name: " + placerName + "\n");
        fileWriter.write("\n");
    }

    public static void writeNodeInfo(FileWriter fileWriter, Node node) throws IOException
    {
        String name = node.name;
        String location = "-location " + node.topLeftAnchorLoc;
        String padding = "-padding " + Integer.toString(node.padLeft) + " " + Integer.toString(node.padRight) + " " + Integer.toString(node.padTop) + " " + Integer.toString(node.padBottom);
        String shape = "-shape " + node.shape.pblockName;

        String line = String.format("%-16s %-32s %-24s %-8s \n", name, location, padding, shape);
        fileWriter.write(line);
    }

    //This populates the coordinates array with the coordinates of the bounding box
    //the array has the order coordinates = [startRow, startCol, endRow, endCol]
    public static void populateBoundingBoxCoordinates(Node[][] fabric)
    {
        int startRow, endRow, startCol, endCol;
        int i, j;

        //Getting the starting row
        for(i = 0; i < fabric.length; i++)
        {
            for(j = 0; j < fabric[i].length; j++)
                if(fabric[i][j] != null)
                    break;

            if(j != fabric[i].length)
                break;
        }
        startRow = i;

        //Getting endRow
        for(i = fabric.length-1; i >= startRow; i--)
        {
            for(j = 0; j < fabric[i].length; j++)
                if(fabric[i][j] != null)
                    break;

            if(j != fabric[i].length)
                break;
        }
        endRow = i;

        //Getting startCol
        for(j = 0; j < fabric[0].length; j++)
        {
            for(i = 0; i < fabric.length; i++)
                if(fabric[i][j] != null)
                    break;
                
            if(i != fabric.length)
                break;
        }
        startCol = j;

        //Getting endCol
        for(j = fabric[0].length-1; j >= startCol; j--)
        {
            for(i = 0; i < fabric.length; i++)
                if(fabric[i][j] != null)
                    break;

            if(i != fabric.length)
                break;
        }
        endCol = j;

        coordinates[0] = startRow;
        coordinates[1] = startCol;
        coordinates[2] = endRow;
        coordinates[3] = endCol;
    }

    //This function returns the pblock definition string as used in Vivado for the given bounding box
    public static String getPblockDefinitionForBoundingBox(Placer obj)
    {
        int [] centerCoordinates = obj.getDesignCenterCoordinates();
        String line = "";
        line += "top-row = " + Integer.toString(coordinates[0]) + " [" + (centerCoordinates[0] - coordinates[0]) + "] ";
        line += "bottom-row = " + Integer.toString(coordinates[2]) + " [" + (coordinates[2] - centerCoordinates[0]) + "] ";
        line += "left-column = " + Integer.toString(coordinates[1]) + " [" + (centerCoordinates[1] - coordinates[1]) + "] ";
        line += "right-column = " + Integer.toString(coordinates[3]) + " [" + (coordinates[3] - centerCoordinates[1]) + "] ";
        return line;
    }

    //This returns the resources present inside the given bounding box
    //This returns ab integer array which is of size 6
    //The values are: [CLB LUTs, DSP48E2, CLEL, CLEM, CLB, DSP]
    public static int[] getResourcesPresentInBoundingBox()
    {
        int startRow = coordinates[0];
        int startCol = coordinates[1];
        int endRow = coordinates[2];
        int endCol = coordinates[3];

        int clel = 0;
        int clem = 0;
        int dsp = 0;

        for(int i = startRow; i <= endRow; i++)
            for(int j = startCol; j <= endCol; j++)
            {
                MapElement mapElement = MapElement.map.get(i).get(j);
                if(MapElement.isCLEL(mapElement.leftElementRootname))
                    clel++;
                if(MapElement.isCLEL(mapElement.rightElementRootname))
                    clel++;
                if(MapElement.isCLEM(mapElement.leftElementRootname))
                    clem++;
                if(MapElement.isCLEM(mapElement.rightElementRootname))
                    clem++;
                if((i == startRow) || (!mapElement.leftElementName.equals(MapElement.map.get(i-1).get(j).leftElementName)))
                    if(mapElement.isDSP(mapElement.leftElementRootname))
                        dsp++;
                if((i == startRow) || (!mapElement.rightElementName.equals(MapElement.map.get(i-1).get(j).rightElementName)))
                    if(mapElement.isDSP(mapElement.rightElementRootname))
                        dsp++;
            }

        int resourcesPresent[] = new int[6];
        resourcesPresent[0] = (clel + clem) * 8;
        resourcesPresent[1] = dsp*2;
        resourcesPresent[2] = clel;
        resourcesPresent[3] = clem;
        resourcesPresent[4] = (clel + clem);
        resourcesPresent[5] = dsp;

        return resourcesPresent;
    }

    //This generates the placement informtion file which can be later fed into the tool to be used from placement of the nodes instead of the in-built placers
    public static boolean generatePlacementInfo(Map<String, Node> nodes, String graphName, String placerName)
    {
        StringUtils.printIntro("Generating Placement Information");
        
        String placeLoc = LocationParser.designs + graphName + "/" + graphName + "_" + placerName + ".place";
        File file = new File(placeLoc);

        try
        {
            FileWriter fileWriter = new FileWriter(file);
            writeComments(fileWriter);
            writeStart(fileWriter, graphName, placerName, nodes.size());

            for (Map.Entry<String,Node> entry : nodes.entrySet()) 
                writeNodeInfo(fileWriter, entry.getValue());

            fileWriter.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not print placement information");
            return false;
        }
        return true; 
    }

    //This generates the placement map which has the names of all the nodes in locations where they are being placed
    public static boolean generatePlacementMap(String graphName, String placerName, Placer obj)
    {
        try
        {
            System.out.println("Writing the placement map");
            FileWriter placementMap = new FileWriter(LocationParser.designs + graphName + "/" + graphName + "_" + placerName + ".csv");
            writeStart(placementMap, graphName, placerName);

            Node[][] fabric = obj.getNodeFabric();
            for(int i = 0; i < fabric.length; i++)
            {
                for(int j = 0; j < fabric[i].length; j++)
                {
                    String s = "-";
                    if(fabric[i][j] != null)
                        s = fabric[i][j].name;
                    placementMap.write("R" + i + "_C" + j + " : " + s + comma);
                }
                placementMap.write("\n");
            }

            placementMap.close();
            System.out.println("Completed writing the placement map");
        }

        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not print placement map");
            return false;
        }
        return true;
    }

    //This generates the focussed placement map where only those map locations are shown which are withing the bounding box
    public static boolean generateFocussedPlacementMap(String graphName, String placerName, Placer obj)
    {
        try
        {
            System.out.println("Writing the focussed placement map");
            FileWriter placementMap = new FileWriter(LocationParser.designs + graphName + "/" + graphName + "_" + placerName + "_focussed.csv");
            writeStart(placementMap, graphName, placerName);

            Node[][] fabric = obj.getNodeFabric();
            populateBoundingBoxCoordinates(fabric);

            //Printing the map
            for(int i = coordinates[0]; i <= coordinates[2]; i++)
            {
                placementMap.write(comma);
                for(int j = coordinates[1];  j <= coordinates[3]; j++)
                {
                    if(fabric[i][j] == null)
                        placementMap.write(comma);
                    else 
                        placementMap.write(fabric[i][j].name + comma);
                }
                placementMap.write("\n");
            }
            placementMap.close();
            System.out.println("Completed writing the focussed placement map");
        }

        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not print focussd placement map");
            return false;
        }
        return true;
    }

    //This is just a helper function to generate the used and present utilisation resource for one resource in the utilisation table
    public static void writePlacementDensityTableStringForOneResource(FileWriter fileWriter, int resourceUtilization[][], int index) throws IOException
    {
        int used = resourceUtilization[index][0];
        int present = resourceUtilization[index][1];
        double util = (double)(used*100) / (double)(present);

        if(present != 0)
            fileWriter.write(Integer.toString(used) + comma + Integer.toString(present) + comma + String.format("%.2f", util) + comma + comma);
        else
            fileWriter.write(Integer.toString(used) + comma + Integer.toString(present) + comma + "-NA-" + comma + comma);
    }

    //This generates the placement density table which has the density of shapes used and the density of the bounding box in site level
    public static boolean generatePlacementDensity(String graphName, String placerName, Placer obj, Map<String, Node> nodes)
    {
        try
        {
            int totalResourceUtilization[][] = new int[6][2];
            System.out.println("Writing the placement density table");
            FileWriter placementDensity = new FileWriter(LocationParser.designs + graphName + "/" + graphName + "_" + placerName + "_density.csv");
            writeStart(placementDensity, graphName, placerName, nodes.size());
            placementDensity.write("Node name" + comma + "DCP Name" + comma + comma + comma + comma + "Granularity: LUT" + comma + comma + comma + comma + comma + comma + comma + comma + comma + comma + comma + comma + "Granularity: Site\n");
            placementDensity.write(comma + comma);
            placementDensity.write("CLB LUTs (Used)" + comma + "CLB LUTs (Present)" + comma + "Density (%)" + comma + comma);
            placementDensity.write("DSP48E2 (Used)" + comma + "DSP48E2 (Present)" + comma + "Density (%)" + comma + comma);
            placementDensity.write("CLEL (Used)" + comma + "CLEL (Present)" + comma + "Density (%)" + comma + comma);
            placementDensity.write("CLEM (Used)" + comma + "CLEM (Present)" + comma + "Density (%)" + comma + comma);
            placementDensity.write("CLB (Used)" + comma + "CLB (Present)" + comma + "Density (%)" + comma + comma);
            placementDensity.write("DSP(Used)" + comma + "DSP (Present)" + comma + "Density (%)" + comma + comma);
            placementDensity.write("\n");

            for (Map.Entry<String,Node> entry : nodes.entrySet())
            {
                Node node = entry.getValue();
                Shape shape = node.shape;
                int resourceUtilization[][] = shape.getResourceUtilization();
                if(resourceUtilization == null)
                    throw new Exception("ERROR: Could not generate resource utilization for node: " + node.name + " and for DCP: " + shape.pblockName + ".  See above logs");
                
                placementDensity.write(node.name + comma + shape.pblockName + comma);
                for(int i = 0; i < 6; i++)
                {
                    writePlacementDensityTableStringForOneResource(placementDensity, resourceUtilization, i);
                    totalResourceUtilization[i][0] += resourceUtilization[i][0];
                    totalResourceUtilization[i][1] += resourceUtilization[i][1];
                }
                placementDensity.write("\n");
            }

            placementDensity.write("\n");
            placementDensity.write("Total Pblock Area" + comma + "-" + comma);
            for(int i = 0; i < 6; i++)
                writePlacementDensityTableStringForOneResource(placementDensity, totalResourceUtilization, i);
            placementDensity.write("\n");

            placementDensity.write("Bounding Box" + comma + getPblockDefinitionForBoundingBox(obj) + comma);
            int [] resourcesPresent = getResourcesPresentInBoundingBox();
            for(int i = 0; i < 6; i++)
            {
                totalResourceUtilization[i][1] = resourcesPresent[i];
                writePlacementDensityTableStringForOneResource(placementDensity, totalResourceUtilization, i);
            }
            placementDensity.write("\n");
            int [] center = obj.getDesignCenterCoordinates();
            placementDensity.write(comma + "center: I = " + center[0] + " J = " + center[1] + " side = " + center[2] + comma);
            placementDensity.write("\n");
            placementDensity.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not print placement density table. See above logs");
            return false;
        }
        return true;
    }
}
