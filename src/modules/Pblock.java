/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/



//This holds and generates the pblock by placing the flops and then removing them. 

 //////////////////////////////////////////////////
 //NOTE: ONLY MEANT FOR PBLOCK_1 AND NOT PBLOCK_2
 //////////////////////////////////////////////////

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

public class Pblock implements Serializable
{
    //Fields related to the actual component
    public Component component;
    public ResourceElement resourceElement; //This holds the resources actually present inside the pblock. Maybe all the resources are not used.
    
    //Fields related to the gven pblock
    public int starti, startj;
    public int currRow, currCol;
    public String pblockName;
    public int incFactor;

    //Fields related to which stage of execution is the pblock.
    //currStatus gives the currStatus of the pblock -- based on above values
    //stage gives the stage of the pblock -- 0 means place and route without flops and 1 means place and route with pblocks
    int currStatus, stage;

    //Fields related to the anchor site and its relative position to the anchor site
    int reli, relj, side; //This tells the location of the anchor site of the pblock w.r.t the start site. //Side has -1 for left and + 1 for right
    Site anchorSite;
    String anchorName; //This finally stores the name of the tile in which the anchor site is present
    HashSet<Site> validPlaces;

    //remove is set to true if we need to remove the generated files in case the pblock was not successful in placement and routing.
    //Else set it to false.
    public Pblock(Component comp, int i, int j, int r, int c, int s, boolean remove, boolean licence)
    {
        starti = i;
        startj = j;
        currRow = r;
        currCol = c;

        component = comp;
        incFactor = component.incFactor;
        stage = s;

        resourceElement = ResourceElement.getPblock(starti, startj, starti + currRow -1, startj + currCol - 1);
        pblockName = getPblockName();

        StringUtils.printIntro("Creating New Pblock: " + pblockName);

        if(stage == 0) //place and route without flops
            currStatus = placeRouteWithoutFlops(licence); //if successful increments the stage value

        if(stage == 1) //place and route with flops
            currStatus = placeRouteWithFlops(licence); //if successful then updates the anchor position fields

        if(stage == 2) //Using a pblock which is already placed and routed
            currStatus = 7;

        if((currStatus != 7) && remove)
            if(!fileCleaner())
                currStatus = -1;
    }

    //This returns the name of the pblock from the module name
    public String getPblockName()
    {
        return component.dcpName + "_I" + Integer.toString(starti) + "_J" + Integer.toString(startj) + "_R" + Integer.toString(currRow) + "_C" + Integer.toString(currCol);
    }

    //This returns all the sites present in the pblock object
    //This returns only the sites which are in the pblock and may be used 
    public ArrayList<Site> getAllSites()
    {
        HashSet<String> siteSet = new HashSet<>();

        ArrayList<Site> sites = new ArrayList<>();
        Device device = Device.getDevice("xcvu13p-fsga2577-1-i");

        for(int j = startj; j < (startj + currCol); j++)
        {
            for(int i = starti; i < (starti + currRow); i++)
            {
                MapElement tempElement = MapElement.map.get(i).get(j);
                Tile leftTile = device.getTile(tempElement.leftElementName);
                Tile rightTile = device.getTile(tempElement.rightElementName);



                if(tempElement.isPlacementSiteLeft())
                {
                    if((tempElement.isDSP(tempElement.leftElementName) && (component.dcp.dsp != 0)) || (tempElement.isBRAM(tempElement.leftElementName) && (component.dcp.bram == 0)))
                    //Adds DSP / BRAM in the pblock only when it will actually be used.
                        if(i % 5 == 4)
                        {
                            Site siteList[] = leftTile.getSites();
                            for(Site s : siteList)
                                if(!siteSet.contains(s.getName()))
                                    siteSet.add(s.getName());
                        }

                    if(tempElement.isCLB(tempElement.leftElementName))
                    {
                        Site siteList[] = leftTile.getSites();
                        for(Site s : siteList)
                            if(!siteSet.contains(s.getName()))
                                siteSet.add(s.getName());
                    }
                }

                if(tempElement.isPlacementSiteRight())
                {
                    if((tempElement.isDSP(tempElement.rightElementName) && (component.dcp.dsp != 0)) || (tempElement.isBRAM(tempElement.rightElementName) && (component.dcp.bram == 0)))
                    //Adds DSP / BRAM in the pblock only when it will actually be used.
                        if(i % 5 == 4)
                        {
                            Site siteList[] = rightTile.getSites();
                            for(Site s : siteList)
                                if(!siteSet.contains(s.getName()))
                                    siteSet.add(s.getName());
                        }

                    if(tempElement.isCLB(tempElement.rightElementName))
                    {
                        Site siteList[] = rightTile.getSites();
                        for(Site s : siteList)
                            if(!siteSet.contains(s.getName()))
                                siteSet.add(s.getName());
                    }
                }
            }
        }

        for (String s : siteSet)
            sites.add(device.getSite(s));

        return sites;
    }

    //This cleans the files which have been generated but is not required anymore
    public boolean fileCleaner()
    {
        File file;
        ArrayList<String> fileLocs = new ArrayList<>();

        //Pre-exposed
        fileLocs.add(LocationParser.preExposedDCPs + component.dcpName + "/" + pblockName + ".tcl");
        fileLocs.add(LocationParser.preExposedDCPs + component.dcpName + "/" + pblockName + "_placedRouted.dcp");
        fileLocs.add(LocationParser.preExposedDCPs + component.dcpName + "/" + pblockName + ".report");

        //Post-exposed
        fileLocs.add(LocationParser.exposedDCPs + component.dcpName + "/" + pblockName + "_preRoute.tcl");
        fileLocs.add(LocationParser.exposedDCPs + component.dcpName + "/" + pblockName + "_connected.dcp");
        fileLocs.add(LocationParser.exposedDCPs + component.dcpName + "/" + pblockName + "_placedRouted.dcp");
        fileLocs.add(LocationParser.exposedDCPs + component.dcpName + "/" + pblockName + "_postRoute.tcl");
        fileLocs.add(LocationParser.exposedDCPs + component.dcpName + "/" + pblockName + ".report");

        fileLocs.add(LocationParser.placedRoutedDCPs + component.dcpName + "/" + pblockName + "_placedRouted.dcp");
        fileLocs.add(LocationParser.placedRoutedDCPs + component.dcpName + "/" + pblockName + "_placedRouted.edf");
        fileLocs.add(LocationParser.placedRoutedDCPs + component.dcpName + "/" + pblockName + "_placedRouted_0_metadata.txt");
        fileLocs.add(LocationParser.placedRoutedDCPs + component.dcpName + "/" + pblockName + ".report");

        String notPresent = " : File not present";
        String notDeleted = " : File could not be deleted";
        String deleted = " : File deleted";


        try
        {
            for(String s : fileLocs)
            {
                file = new File(s);
                if(!file.exists())
                    System.out.println(s + notPresent);

                else
                {
                    if(!file.delete())
                        throw new Exception(s + notDeleted);
                    System.out.println(s + deleted);
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not run file cleaner successfully. See above logs");
            return false;
        }

        return true;
    }

    public void setAnchorPosition()
    {
        Design design = new Design("test", "xcvu13p-fsga2577-1-i");
        Device device = design.getDevice();
		EDIFNetlist netlist = design.getNetlist();
		EDIFCell top = netlist.getTopCell();

        String dcp = LocationParser.placedRoutedDCPs + component.dcpName + "/" + pblockName + "_placedRouted.dcp";
        String meta = LocationParser.placedRoutedDCPs + component.dcpName + "/" + pblockName + "_placedRouted_0_metadata.txt";

        Module m = new Module(Design.readCheckpoint(dcp), meta);
        netlist.migrateCellAndSubCells(m.getNetlist().getTopCell(), true);
        ModuleInst m1 = design.createModuleInst("m1", m);
        validPlaces = new HashSet<>(m1.getAllValidPlacements());

        anchorSite = m.getAnchor();
        anchorName = anchorSite.getName();
        System.out.println("The name of the anchor site is: " + anchorName);
        anchorName = anchorSite.getTile().getName();
        System.out.println("The name of the anchor tile is: " + anchorName);
        System.out.println("The number of valid placement sites are " + validPlaces.size());

        int anchori, anchorj;
        anchori = Integer.parseInt(MapElement.mapMap.get(anchorName).substring(0, MapElement.mapMap.get(anchorName).indexOf(':')));
        anchorj = Integer.parseInt(MapElement.mapMap.get(anchorName).substring(MapElement.mapMap.get(anchorName).indexOf(':') + 1));

        reli = anchori - starti;
        relj = anchorj - startj;

        if(MapElement.map.get(anchori).get(anchorj).leftElementName.equalsIgnoreCase(anchorName))
            side = -1;
        else
            side = 1;
    }

    /*
     * Function places and routes the pblock without the use of flops
     * If successful, then it increments the stage value
     * @return Returns the currStatus value (-1 in case of error)
     */
    public int placeRouteWithoutFlops(boolean lic)
    {
        System.out.println("Generating pre-exposed DCP of pblock: " + pblockName);

        if(!PreExposedTclGenerator.tclGenerator(this))
            return -1;

        String tclLoc = LocationParser.preExposedDCPs + component.dcpName + "/" + pblockName + ".tcl";
        String logsLoc = LocationParser.preExposedDCPs + component.dcpName + "/" + pblockName + ".report";
        String errorReference = "pre-exposed: " + pblockName;
        int errorCode = 0;

        if(!VivadoRun.vivadoRun(tclLoc, logsLoc, lic, errorReference, errorCode))
            return -1;

        Set<Integer> omitSet = new HashSet<Integer>();
        omitSet.add(6); //These checks are to be ommited by the placed and routed logs checker

        currStatus = PlacedRoutedLogParser.logsChecker(logsLoc, omitSet);
        if(currStatus == 7)
        {
            System.out.println("Pblock: " + pblockName + " placed and routed successfully. Going on to exposing the pins of pblock");
            stage++;
        }

        else
            System.out.println(PlacedRoutedLogParser.getErrorMsg(currStatus));

        return currStatus;
    }

    /*
     * Function places and routes the pblock with the use of flops
     * If suceessful, then it updates the anchor position value
     * @return Returns the currStatus value (-1 in case of error)
     */
    public int placeRouteWithFlops(boolean lic)
    {
        System.out.println("Generating exposed DCP of pblock: " + pblockName);

        PinExposer obj = new Circular (this);

        String tclLoc = LocationParser.exposedDCPs + component.dcpName + "/" + pblockName + "_preRoute.tcl";
        String logsLoc = LocationParser.exposedDCPs + component.dcpName + "/" + pblockName + ".report";
        String errorReference = "post-exposed: " + pblockName;
        int errorCode = 0;
    
        if(!VivadoRun.vivadoRun(tclLoc, logsLoc, lic, errorReference, errorCode))
            return -1;
    
        Set<Integer> omitSet = new HashSet<Integer>();
        omitSet.add(6); //These checks are to be ommited by the placed and routed logs checker
    
        currStatus = PlacedRoutedLogParser.logsChecker(logsLoc, omitSet);
        if(currStatus == 7)
        {
            System.out.println("Pblock: " + pblockName + " placed and routed successfully. Going on to repairing the pblock");
            
            tclLoc = LocationParser.exposedDCPs + component.dcpName + "/" + pblockName + "_postRoute.tcl";
            logsLoc = LocationParser.placedRoutedDCPs + component.dcpName + "/" + pblockName + ".report";
            errorReference = "post-exposed: " + pblockName;
            errorCode = 1;

            if(!VivadoRun.vivadoRun(tclLoc, logsLoc, false, errorReference, errorCode))
                return -1;

            omitSet = new HashSet<Integer>();
            omitSet.add(2);

            currStatus = PlacedRoutedLogParser.logsChecker(logsLoc, omitSet);
            if(currStatus != 7)
            {
                System.out.println("ERROR: Could not remove error");
                System.out.println(PlacedRoutedLogParser.getErrorMsg(currStatus));
                return -1;

            }

            System.out.println("Successfully generated placed and routed pblock: " + pblockName);
            setAnchorPosition();
        }

        else
            System.out.println(PlacedRoutedLogParser.getErrorMsg(currStatus));

        return currStatus;
    }
}
