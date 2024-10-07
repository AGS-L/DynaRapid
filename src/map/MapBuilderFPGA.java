/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/




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
import java.lang.*;

import org.python.antlr.PythonParser.else_clause_return;

public class MapBuilderFPGA
{
    static Device device;
    static Tile tileArray[][];
    static int numRows, numCols;

    //Prints the contents of the map in a CSV file
    public static boolean printMapCSV(Vector<Vector<MapElement>> tempMap, Vector<Vector<ResourceElement>> resourceMap)
    {
        try
        {
            System.out.println("Writing the map");
            FileWriter deviceMap = new FileWriter(LocationParser.map + "deviceMap-xcvu13p-fsga2577-1-i.csv");
            deviceMap.write("Device: xcvu13p-fsga2577-1-i \n");
            for(int i = 0; i < tempMap.size(); i++)
            {
                
                for(int j = 0; j < tempMap.get(i).size(); j++)
                {
                    MapElement temp = tempMap.get(i).get(j);
                    deviceMap.write(temp.leftElementName + " (" + temp.leftRow + "-" + temp.leftCol + ") ,");
                    deviceMap.write(temp.switchBoxName + " (" + temp.switchBoxRow + "-" + temp.switchBoxCol + ") ,");
                    deviceMap.write(temp.rightElementName + " (" + temp.rightRow + "-" + temp.rightCol + ") ,");
                    deviceMap.write("\t,");
                }
                deviceMap.write("\n");
            }
            System.out.println("Completed writing the map");
            deviceMap.close();

            System.out.println("Writing Resource map");
            FileWriter resourceFile =  new FileWriter(LocationParser.map + "resourceMap-xcvu13p-fsga2577-1-i.csv");
            resourceFile.write("Device: xcvu13p-fsga2577-1-i, Start: CLEL_R_X0Y899, End: CLEM_R_X147Y870, l = CLEL, m = CLEM, d = DSP, b = BRAM, r = rows, c = columns\n");
            for(int i = 0; i < resourceMap.size(); i++)
            {
                
                for(int j = 0; j < resourceMap.get(i).size(); j++)
                {
                    ResourceElement temp = resourceMap.get(i).get(j);
                    resourceFile.write("l = " + temp.clel + " m = " + temp.clem + ",d = " + temp.dsp + " b = " + temp.bram + ",r = " + temp.row + " c = " + temp.col + ",");
                    resourceFile.write("\t,");
                }
                resourceFile.write("\n");
            }
            System.out.println("Completed writing the resource map");
            resourceFile.close();
        }

        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not print device map");
            return false;
        }
        return true;
    }

    //This updates the leftMapRowToIOB, rightMapRowToIOB of the mapElement so we know between which rows of the map we have are the IOBs
    //Note: To be called after the mapMap is populated
    public static void updateIOBFields()
    {
        for(int i = 0; i < tileArray.length; i++)
        {
            for(int j = 0; j < tileArray[i].length; j++)
            {
                String rootname = tileArray[i][j].getRootName();
                if(!MapElement.isIOB(rootname)) 
                    continue;

                int leftCol = j - 1;
                while(leftCol >= 0)
                {
                    String tileName = tileArray[i][leftCol].getName();

                    
                }
            }
        }
    }

    //This omits rows in which even one left AND right placement site in the entire row is not a valid site
    public static void omitRows()
    {
        Vector<Vector<MapElement>> tempMap = new Vector<>();

        for(int i = 0; i < numRows; i++)
            for(int j = 0; j < numCols; j++)
                if(!MapElement.map.get(i).get(j).isPlacementSiteLeft() && !MapElement.map.get(i).get(j).isPlacementSiteRight())
                    if(!MapElement.rowsRemoved.contains(i))
                        MapElement.rowsRemoved.add(i);
        
        for(int i = 0; i < numRows; i++)
        {
            if(MapElement.rowsRemoved.contains(i))
                continue;

            tempMap.add(new Vector<MapElement>());
            for(int j = 0; j < numCols; j++)
                tempMap.get(tempMap.size()-1).add(MapElement.map.get(i).get(j));
                
        }

        MapElement.map = tempMap;
        numRows = tempMap.size();
        numCols = tempMap.get(0).size();

        System.out.println("Number of rows in the omitted map is " + numRows);
        System.out.println("Number of columns in the ommitted map is " + numCols);
    }

    //This function corrects for the DSPs and BRAMs in the map.
    //This replicates the DSP sites and BRAM sites 4 rows up
    public static boolean correctDspBram()
    {
        System.out.println("Correcting for DSP and BRAM sites");
        int i, j, k;
        i = j = 0;
        MapElement tempElement = new MapElement();;
        try
        {

            for(i = 0; i < numRows; i++)
            {
                for(j= 0; j < numCols; j++)
                {
                    tempElement = MapElement.map.get(i).get(j);
                    if(tempElement.isDSP(tempElement.leftElementRootname) || tempElement.isBRAM(tempElement.leftElementRootname))
                    {
                        for(k = 1; k <= 4; k++)
                        {
                            MapElement.map.get(i-k).get(j).leftElementName = tempElement.leftElementName;
                            MapElement.map.get(i-k).get(j).leftElementRootname = tempElement.leftElementRootname;
                            MapElement.map.get(i-k).get(j).leftRow = tempElement.leftRow;
                            MapElement.map.get(i-k).get(j).leftCol = tempElement.leftCol;
                        }
                    }

                    if(tempElement.isDSP(tempElement.rightElementRootname) || tempElement.isBRAM(tempElement.rightElementRootname))
                    {
                        for(k = 1; k <= 4; k++)
                        {
                            MapElement.map.get(i-k).get(j).rightElementName = tempElement.rightElementName;
                            MapElement.map.get(i-k).get(j).rightElementRootname = tempElement.rightElementRootname;
                            MapElement.map.get(i-k).get(j).rightRow = tempElement.rightRow;
                            MapElement.map.get(i-k).get(j).rightCol = tempElement.rightCol;
                        }
                    }
                }   
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Irregularity in map. offset of 5 is not followed at tile" + tempElement.switchBoxName);
            return false;
        }

        System.out.println("DSP and BRAM corrections done");
        return true;
    }

    //This forms the basic map from the tile map of RapidWright.
    //This includes all those columns which has INT as their first row's TILE name and no INT tile to its left.
    //This will have a tempMap of the above INT tiles along with the left andright NON-INT tiles.
    //No rows have been ommited.
    public static boolean basicMap()
    {
        int i, j;
        Vector<Vector<MapElement>> tempMap = new Vector<>();
        for(i = 0; i < numRows; i++)
            tempMap.add(new Vector<MapElement>());

        System.out.println("Reading tile map from RW");
        System.out.println("Given tile map from RapidWright has " + numRows + " rows and " + numCols + " columns");
        for(i = 0; i < numCols; i++)
        {

            if(!tileArray[0][i].getName().startsWith("INT")) //Non Switchbox columns
                continue;

            if((i > 0) && (tileArray[0][i-1].getName().startsWith("INT"))) //Switchbox columns with left side as switchbox as well is ignored
                continue;

            Vector <MapElement> colMap = new Vector<>();
            int count = 0; //Counts the number of valid placement sites in the column

            for(j = 0; j < numRows; j++)
            {
                Tile tile = tileArray[j][i];

                MapElement tempElement = new MapElement();
                tempElement.switchBoxName = tile.getName();
                tempElement.switchBoxRootname = tile.getRootName();
                tempElement.switchBoxRow = j;
                tempElement.switchBoxCol = i;

                if(i > 0) //left side analysis
                {
                    Tile leftTile = tileArray[j][i-1];
                    tempElement.leftElementName = leftTile.getName();
                    tempElement.leftElementRootname = leftTile.getRootName();
                    tempElement.leftRow = j;
                    tempElement.leftCol = i-1;
                }

                for(int k = i+1; k < numCols; k++) //There may be INT sites in the right of it
                {
                    if(!tileArray[j][k].getRootName().startsWith("INT"))
                    {
                        Tile rightTile = tileArray[j][k];
                        tempElement.rightElementName = rightTile.getName();
                        tempElement.rightElementRootname =  rightTile.getRootName();
                        tempElement.rightRow = j;
                        tempElement.rightCol = k;
                        break;
                    }
                }

                if(tempElement.isPlacementSiteLeft() || tempElement.isPlacementSiteRight())
                    count++;

                colMap.add(tempElement);
            }

            if(count != 0)
            {
                if(colMap.size() != numRows)
                {
                    System.out.println("ERROR: Number of rows not matching");
                    return false;
                }

                for(j = 0; j < colMap.size(); j++)
                    tempMap.get(j).add(colMap.get(j));
                
                MapElement.colAdded.add(i);
            }
        }
        
        MapElement.map = tempMap;

        System.out.println("Completed Reading tile map from RW");
        System.out.println("Number of rows in the basic map is: " + tempMap.size());
        System.out.println("Number of columns in the basic map is: " + tempMap.get(0).size());

        numRows = MapElement.map.size();
        numCols = MapElement.map.get(0).size();
        return true;

    }

    public static boolean mapBuilderFPGA()
    {
        device = Device.getDevice("xcvu13p-fsga2577-1-i");
        tileArray = device.getTiles();
        numRows = device.getRows();
        numCols = device.getColumns();

        if(!basicMap())
        {
            System.out.println("Could not form basic map");
            return false;
        }

        if(!correctDspBram())
        {
            System.out.println("Could not correct for DSPs and BRAMs");
            return false;
        }

        omitRows();
        ResourceElement.resourceMapBuilder();
        MapElement.populateMapMap();
        return true;
    }

    public static void main(String args[])
    {
        if(!mapBuilderFPGA())
        {
            System.out.println("ERROR: Could not generate the map");
            return;
        }
        
        if(!printMapCSV(MapElement.map, ResourceElement.ResourceMap))
        {
            System.out.println("ERROR: Could not write map");
            return;
        }

        System.out.println("Input the name of TILE to get the i and j or 000 to exit sequence");
        while(true)
        {
            Scanner in = new Scanner (System.in);
            String s = in.nextLine();
            if(s.equalsIgnoreCase("000"))
                break;

            s = MapElement.findInMap(s);
            System.out.println(s);
        }
    }
}
