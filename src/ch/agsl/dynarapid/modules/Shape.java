/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.modules;


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
//This holds all the shapes of a perticular component. Filled by the DatabaseParser

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


public class Shape implements Serializable
{
    //Fields related to the actual component
    public Component component;
    public ResourceElement resourceElement; //This holds the resources actually present inside the pblock. Maybe all the resources are not used.
    
    //Fields related to the gven pblock
    public int starti, startj;
    public int currRow, currCol;
    public String pblockName;
    public int incFactor;
    public double density;
    public int resourceUtilization[][] = null; //This holds resources used by the pblock after its creation. This is populated by getRsourceUtilization()
    //The format in which values are filled are [[CLB LUTs (used), DSP48E2 (used), CLEL (used), CLEM (used), CLB (used), DSP (used)], [CLB LUTs (Present), DSP48E2 (Present), CLEL (Present), CLEM (Present), CLB (Present), DSP (Present)]]

    //Fields related to the anchor site and its relative position to the anchor site
    public int reli, relj, side; //This tells the location of the anchor site of the pblock w.r.t the start site. //Side has -1 for left and + 1 for right
    public int siteIndex; //This tells in which index the site is present in the tile array

    public Site rwAnchorSite = null; //RW anchor site
    public String rwAnchorSiteName; //RW anchor site name

    public Tile rwAnchorTile = null; //RW anchor tile
    public String rwAnchorTileName; //RW anchor site name

    //Fields related to the valid locations where the shape can be placed
    public HashSet<String> validTopLeftAnchorLocations;
    public Map<String, Site> validSiteMap = null; //This maps the top-left anchor location to the actual RW anchor site where the pblock can be placed

    //Fields for the graph-placer
    public Module module = null;
    public Design design = null;

    /*
     * This generates a pblock which is completely isolated from rapidwright. We need to call update shape later to fill rapidwright related details
     * @param comp Component to which the pblock corresponds to
     * @param i j starti and startj of the pblock
     * @param r c currRow and currCol of the pblock
     * @param pb pblockName
     * @param d density of the pblock
     * @param ri rj s reli and relj and side of the pblock
     * @param si Site index of the shape for the RW anchor tile
     * @param as RW Anchor Site of the pblock
     * @param an RW Anchor Tile of the pblock
     * @param vp The hashset of the top-left validPlaces of the shape
     */
    public Shape(Component comp, int i, int j, int r, int c, String pb, Double d, int ri, int rj, int s, int si, String as, String an, HashSet<String> vp) 
    {
        component = comp;
        starti = i;
        startj = j;
        currRow = r;
        currCol = c;
        pblockName = pb;
        density = d;
        incFactor = component.incFactor;

        reli = ri;
        relj = rj;
        side = s;
        siteIndex = si;

        rwAnchorSiteName = as;
        rwAnchorTileName = an;
        validTopLeftAnchorLocations = vp;

        resourceElement = ResourceElement.getPblock(starti, startj, starti + currRow -1, startj + currCol - 1);
    }

    //This fills the validSiteMap.
    //Here the key of the map is the top-left tile location as mentioned in the database
    //The value is the actual site where the site can be placed. This can be passed to the RW module placer
    public void fillValidSiteMap(Device device)
    {
        validSiteMap = new HashMap<>();

        for(String s : validTopLeftAnchorLocations)
        {
            String rowString = s.substring(0, s.indexOf("_"));
            String colString = s.substring(s.indexOf("_") + 1);

            int starti = Integer.parseInt(rowString.substring(1));
            int startj = Integer.parseInt(colString.substring(1));

            int anchori = starti + reli;
            int anchorj = startj + relj;

            Tile rwAnchorTile;
            if(side == -1)
                rwAnchorTile = device.getTile(MapElement.map.get(anchori).get(anchorj).leftElementName);

            else
                rwAnchorTile = device.getTile(MapElement.map.get(anchori).get(anchorj).rightElementName);

            Site rwAnchorSite = rwAnchorTile.getSites()[siteIndex];
            validSiteMap.put(s, rwAnchorSite);
        }
    }

    //This updates the shape and fills all the rapidwright related details. 
    //This is called after reading from binary or normal databases.
    public void updateShape(Device device)
    {
        rwAnchorSite = device.getSite(rwAnchorSiteName);
        rwAnchorTile = device.getTile(rwAnchorTileName);

        fillValidSiteMap(device);
    }

    //This creates a module corresponding to the shape. This basically loads the module
    public Module createModule(Design des)
    {
        if((module != null) && (design != null))
        {
            System.out.println("Module for shape " + pblockName + " has already been created");
            return module;
        }

        design = des;

        String dcpLoc = LocationParser.placedRoutedDCPs + component.dcpName + "/" + pblockName + "_placedRouted.dcp";
        String metaLoc = LocationParser.placedRoutedDCPs + component.dcpName + "/" + pblockName + "_placedRouted_0_metadata.txt";

        module = new Module(Design.readCheckpoint(dcpLoc), metaLoc);
        design.getNetlist().migrateCellAndSubCells(module.getNetlist().getTopCell(), true);
        System.out.println("Created module for " + pblockName);

        //Setting all the module ports of the component
        if(component.modulePorts == null)
            component.modulePorts = new ModulePorts(module);

        return module;
    }

    //This just creates a module and returns it. This module is not stored in the shape and the design is not updated
    public Module createModule()
    {
        if((module != null) && (design != null))
        {
            System.out.println("Module for shape " + pblockName + " has already been created");
            return module;
        }

        String dcpLoc = LocationParser.placedRoutedDCPs + component.dcpName + "/" + pblockName + "_placedRouted.dcp";
        String metaLoc = LocationParser.placedRoutedDCPs + component.dcpName + "/" + pblockName + "_placedRouted_0_metadata.txt";

        return new Module(Design.readCheckpoint(dcpLoc), metaLoc);
    }

    //This adds a module to the shape
    public boolean addModule(Design des, Module m)
    {
        if((module != null) && (design != null))
        {
            System.out.println("Module for shape " + pblockName + " has already been added to the shape");
            return false;
        }

        module = m;
        design = des;

        design.getNetlist().migrateCellAndSubCells(module.getNetlist().getTopCell(), true);
        System.out.println("Added module for " + pblockName);

        //Setting all the module ports of the component
        if(component.modulePorts == null)
            component.modulePorts = new ModulePorts(module);

        return true;

    }

    //This returns the top left anchor location for the shape if the center of the shape was represented by i and j
    //NOTE THIS DOESNOT SAY IF THE SHAPE CAN BE PLACED IN THE GIVEN TOP LEFT ANCHOR LOCATION OR NOT. FOR THAT YOU NEED TO CHECK IT SEPARATELY
    public String getTopLeftCornerStringFromCenterLoc(int i, int j)
    {
        int starti = i - (currRow / 2);
        int startj = j - (currCol / 2);
        String topLeftAnchorLoc = "R" + Integer.toString(starti) + "_C" + Integer.toString(startj);
        return topLeftAnchorLoc;
    }

    //This returns the aspect ratio of the shape
    public double getAspectRatio()
    {
        return ((double)(currRow) / (double)(currCol));
    }

    /*
     * This parses the resource utilisation of this specific shape and returns statistics regarding the resources utilisation by this specific pblock
     * Note the resources required in the component.dcp is the minimum number of sites required by each pblock of that component
     * Often times it has been seen that the amount of resources used is more than the minimum amount of resources mentioned in component.dcp
     * This populates the resourceUtilization array and once filled, it does not repopulate it.
     * This calls the PblockUtilizationParser to parse the util logs
     * This returns the resourceUtilization in this format
     * [[CLB LUTs (used), DSP48E2 (used), CLEL (used), CLEM (used), CLB (used), DSP (used)], [CLB LUTs (Present), DSP48E2 (Present), CLEL (Present), CLEM (Present), CLB (Present), DSP (Present)]]
     * This is called by the PlacementInfo.generatePlacementDensity() function during design generation in debug mode
     */
    public int[][] getResourceUtilization()
    {
        if(resourceUtilization != null)
            return resourceUtilization;

        String utilLoc = LocationParser.placedRoutedDCPs + component.dcpName + "/" + pblockName + ".util";
        PblockUtilizationParser obj = new PblockUtilizationParser(utilLoc);
        if(!obj.status)
            return resourceUtilization;

        int resourcesUsed[] = obj.resourcesUsed;
        int resourcesAvail[] = obj.resourcesAvail;
        resourceUtilization = new int[6][2];

        resourceUtilization[0][0] = resourcesUsed[0];
        resourceUtilization[1][0] = resourcesUsed[3];
        resourceUtilization[2][0] = resourcesUsed[1];
        resourceUtilization[3][0] = resourcesUsed[2];
        resourceUtilization[4][0] = resourcesUsed[1] + resourcesUsed[2];
        resourceUtilization[5][0] = (resourcesUsed[3] / 2) + (resourcesUsed[3] % 2);

        resourceUtilization[0][1] = resourcesAvail[0];
        resourceUtilization[1][1] = resourcesAvail[3];
        resourceUtilization[2][1] = resourcesAvail[1];
        resourceUtilization[3][1] = resourcesAvail[2];
        resourceUtilization[4][1] = resourcesAvail[1] + resourcesAvail[2];
        resourceUtilization[5][1] = (resourcesAvail[3] / 2) + (resourcesAvail[3] % 2);

        return resourceUtilization;
    }

    public void printShape()
    {
        System.out.println("---------------------------------");
        System.out.println("Printing Shape: " + pblockName);
        
    } 
}
