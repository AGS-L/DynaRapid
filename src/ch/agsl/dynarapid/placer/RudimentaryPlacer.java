/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.placer;

import  ch.agsl.dynarapid.*;
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
//This is a rudimentary placer.
//This tries to pack all the graph nodes as close to each other as possible
//For each node it checks all the shapes and all the positions

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

public class RudimentaryPlacer implements Placer{
    
    Node fabric[][];
    int centeri, centerj;
    Site centerSite;
    int topRow, bottomRow, leftCol, rightCol; //These define the boundaries of the fabric within which the design must be placed

    Device device;
    int pad = 0;

    public RudimentaryPlacer()
    {
        setGlobalFields();
        topRow = 0;
        bottomRow = fabric.length;
        leftCol = 0;
        rightCol = fabric[0].length;
    }

    public RudimentaryPlacer(int tr, int br, int lc, int rc)
    {
        setGlobalFields();
        topRow = tr;
        bottomRow = br;
        leftCol = lc;
        rightCol = rc;
    }

    /*
     * This is a constructor helper function
     * This sets most of the global fields of the placer object
     */
    public void setGlobalFields()
    {
        device = Device.getDevice(GenerateDesign.fpga_part);

        int rows = MapElement.map.size();
        int cols = MapElement.map.get(0).size();
        fabric = new Node [rows][cols];

        String siteName = "SLICE_X67Y624"; //Near the center

        centerSite = device.getSite(siteName);
        String tileName = centerSite.getTile().getName();
        String coord = MapElement.findInMap(tileName);
        centeri = Integer.parseInt(coord.substring(0, coord.indexOf(":")));
        centerj = Integer.parseInt(coord.substring(coord.indexOf(":") + 1));

        //set padding here
        pad = 0;
    }

    public String getPlacerName()
    {
        return "Rudimentary-Placer";
    }

    public  boolean setDesignCenterUsingSiteName(String siteName)
    {
        try
        {
            centerSite = device.getSite(siteName);
            String tileName = centerSite.getTile().getName();
            String coord = MapElement.findInMap(tileName);
            centeri = Integer.parseInt(coord.substring(0, coord.indexOf(":")));
            centerj = Integer.parseInt(coord.substring(coord.indexOf(":") + 1));
        }

        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not set the center of the design as " + siteName + ". See ablve logs");
            return false;
        }

        return true;
    }

    public boolean setDesignCenterUsingCoordinates(int i, int j, int side)
    {
        int n = MapElement.map.size();
        int m = MapElement.map.get(0).size();

        if((i < 0) || (i >= n) || (j < 0) || (j >= m))
        {
            System.out.println("ERROR: Mentioned center coordinates row = " + i + " column = " + j + " in not in range of the map");
            return false;
        }

        if((side != -1) && (side != 1))
        {
            System.out.println("ERROR: Side provided as " + side + " is not valid. Must be +1 or -1 only");
            return false;
        }

        MapElement element = MapElement.map.get(i).get(j);
        String tileName = "";

        if(!element.leftElementName.startsWith("NULL") && !element.rightElementName.startsWith("NULL"))
            tileName = (side == -1) ? element.leftElementName : element.rightElementName;
        
        else if(!element.leftElementName.startsWith("NULL"))
            tileName = element.leftElementName;

        else if(!element.rightElementName.startsWith("NULL"))
            tileName = element.rightElementName;
        else 
        {
            System.out.println("ERROR: Both sides of the mentioned coordinates as row = " + i + " and column = " + j + " is NULL");
            return false;
        }

        Tile tile = device.getTile(tileName);

        centerSite = tile.getSites()[0];
        centeri = i;
        centerj = j;
        return true;
    }

    public int[] getDesignCenterCoordinates()
    {
        String tileName = centerSite.getTile().getName();
        MapElement element = MapElement.map.get(centeri).get(centerj);
        int side = 1;
        if(tileName.equals(element.leftElementName))
            side = -1;

        int[] center = new int[3];
        center[0] = centeri;
        center[1] = centerj;
        center[2] = side;
        return center;
    }

    public Site getDesignCenterSite()
    {
        return centerSite;
    }

    //This returns the manhatten distance between the given site and the center site
    public int getManhattenDistance(String topLeftAnchorLoc)
    {
        String rowString = topLeftAnchorLoc.substring(0, topLeftAnchorLoc.indexOf("_"));
        String colString = topLeftAnchorLoc.substring(topLeftAnchorLoc.indexOf("_") + 1);

        int starti = Integer.parseInt(rowString.substring(1));
        int startj = Integer.parseInt(colString.substring(1));

        return (Math.abs(starti - centeri) + Math.abs(startj - centerj));  
    }

    public Map<String, Node> placer(Map<String, Node> nodes)
    {
        if((centeri < topRow) || (centeri > bottomRow) || (centerj < leftCol) || (centerj > rightCol))
        {
            System.out.println("Design center of: I = " + centeri + " J = " + centerj + ". is not within the constrained region of the FPGA fabric. Exiting design placement");
            return null;
        }

        for (Map.Entry<String,Node> entry : nodes.entrySet()) 
        {
            Node node = entry.getValue();
            node.updatePadding(pad, pad, pad, pad);
            Component component = node.component;
            
            Shape shape = null;
            String topLeftAnchorLoc = "";
            int distance = 0;

            System.out.println("Trying to decide placement of " + node.name);

            for(Shape sh : component.shapeList)
            {
                //Condition to be applied for the image_resized_optimized design. Apparently it does not work with all the pblocks of getelementptr
                if(component.dcpName.startsWith("getelementptr_"))
                    if(!sh.pblockName.equals("getelementptr_op_3_1_32_32_00000000000000000000000000000001_I60_J136_R25_C2"))
                        continue;

                for(String s : sh.validTopLeftAnchorLocations)
                {
                    if(!node.canPlace(fabric, topRow, bottomRow, leftCol, rightCol, sh, s))
                        continue;

                    if(shape == null)
                    {
                        shape = sh;
                        topLeftAnchorLoc = s;
                        distance = getManhattenDistance(s);
                        continue;
                    }

                    int tempDist = getManhattenDistance(s);
                    if((tempDist < distance) || ((tempDist == distance) && (sh.density > shape.density)))
                    {
                        shape = sh;
                        topLeftAnchorLoc = s;
                        distance = tempDist;
                    }
                }
            }

            if((shape == null) || (topLeftAnchorLoc.equals("")))
            {
                System.out.println("ERROR: Could not find any placement shape and site for node: " + node.name + ". Reduce contraints on fabric dimensions and re-run GenerateDesign");
                return null;
            }

            if(!node.updatePlacementData(fabric, shape, topLeftAnchorLoc))
            {
                System.out.println("ERROR: Could not update placement data for node: " + node.name);
                return null;
            }
            System.out.println(node.name + " is placed at top-left anchor location of: " + node.topLeftAnchorLoc + " at RW anchor site: " + node.rwAnchorSiteName);
        }
        System.out.println("Completed graph placement");
        return nodes;
    }

    public Node[][] getNodeFabric()
    {
        return fabric;
    }
}
