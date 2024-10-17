/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.interrouting.algorithms;
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


//This places the flops a circular fashion around the pblock. There are no input or output direction of the flops
//This only places the flops on the top and bottom of the module
//No LUTs are used.

import com.google.protobuf.MapEntryLite;
import com.xilinx.rapidwright.design.*;
import com.xilinx.rapidwright.design.Module;
import com.xilinx.rapidwright.design.ModuleInst;
import com.xilinx.rapidwright.design.SiteInst;
import com.xilinx.rapidwright.device.*;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.device.Tile;
import com.xilinx.rapidwright.device.SiteTypeEnum;
import com.xilinx.rapidwright.device.TileTypeEnum;
import com.xilinx.rapidwright.device.helper.TileColumnPattern;
import com.xilinx.rapidwright.edif.EDIFCell;
import com.xilinx.rapidwright.edif.EDIFCellInst;
import com.xilinx.rapidwright.edif.EDIFDirection;
import com.xilinx.rapidwright.edif.EDIFNet;
import com.xilinx.rapidwright.edif.EDIFNetlist;
import com.xilinx.rapidwright.edif.EDIFPort;
import com.xilinx.rapidwright.edif.EDIFPortInst;
import com.xilinx.rapidwright.tests.CodePerfTracker;
import com.xilinx.rapidwright.examples.SLRCrosserGenerator;
import com.xilinx.rapidwright.design.blocks.PBlock;
import com.xilinx.rapidwright.router.Router;
import com.xilinx.rapidwright.router.SATRouter;
import com.xilinx.rapidwright.placer.handplacer.HandPlacer;
import com.xilinx.rapidwright.rwroute.RWRoute;

import java.io.*;
import java.util.*;
import java.lang.*;

public class Circular implements PinExposer{
    Design design;
    Pblock pblock;

    ArrayList <RouteSite> routeSites;
    ArrayList <EDIFNet> designRouteNets; //The 0th net is the clk net and the 1st net is the reset net
    ArrayList <EDIFPort> designPorts; //The 0th port is the clk port and the 1st port is the reset port

    int minFlops, minLUTs, minRouteSites;
    EDIFNet clk, rst;

    public Design returnDesign()
    {
        return design;
    }

    public Pblock returnPblock()
    {
        return pblock;
    }

    public ArrayList <RouteSite> returnRouteSites()
    {
        return routeSites;
    }

    public ArrayList <EDIFNet> returnDesignRouteNets()
    {
        return designRouteNets;
    }

    public ArrayList <EDIFPort> returnDesignPorts()
    {
        return designPorts;
    }

    public ArrayList <Site> returnSites()
    {
        ArrayList<Site> sites = new ArrayList<>();

        for(RouteSite r : routeSites)
            sites.add(r.site);
        
        return sites;
    }

    public EDIFNet createCLKNet (Design design, EDIFPort clkPort, EDIFCellInst moduleEDIFCellInst)
    {
        EDIFNet clkNet = design.getNetlist().getTopCell().createNet(clkPort.getName()); //just the name of net is same as the port name. This didnot add the port.
        clkNet.createPortInst(clkPort);
        clkNet.createPortInst(moduleEDIFCellInst.getPort(ModulePorts.CLK), moduleEDIFCellInst);
        return clkNet;
    }

    public EDIFNet createRSTNet (Design design, EDIFPort rstPort, EDIFCellInst moduleEDIFCellInst)
    {
        EDIFNet rstNet = design.getNetlist().getTopCell().createNet(rstPort.getName()); //just the name of net is same as the port name. This didnot add the port.
        rstNet.createPortInst(rstPort);
        rstNet.createPortInst(moduleEDIFCellInst.getPort(ModulePorts.RST), moduleEDIFCellInst);
        return rstNet;
    }

    public int minFlops (Pblock pblock)
    {
        Map<String, ArrayList<Integer>> buses = pblock.component.modulePorts.buses;
        int in = 0;
        int out = 0;

        for (Map.Entry<String,ArrayList<Integer>> entry : buses.entrySet()) 
        {
            String busName = entry.getKey();
            int busWidth = entry.getValue().get(0);
            int direction = entry.getValue().get(1);

            if(busName.equals(ModulePorts.CLK) || busName.equals(ModulePorts.RST))
                continue;

            if(direction == 0) //input bus
                in += busWidth;

            else //output bus
                out += busWidth;
        } 
        
        return Math.max(in, out);
    }

    public int minLUTs (Pblock pblock)
    {
        return 0;
    }

    public int minRouteSites(int minFlops, int minLUTs, Pblock pblock)
    {
        int numFlopsPerSite = 16;
        return (int)(Math.ceil((double)(minFlops) / (double)(numFlopsPerSite)));
    }

    /*
     * This function forms one layer of routeSites around the module whose corners are described by the dimenstions (starti, startj) and (endi, endj)
     * @param map This is teh map of teh FPGA
     * @param (starti, endi) top left corner index of the already occupied sites
     * @param (endi, endj) bottom right corner index of the already occupied sites 
     * @param currLayer This gives the laye in which sites are to placed.
     * @return the name of the sites
     */
    public ArrayList<String> formUniLayer (ArrayList<ArrayList<MapElement>> map, Device device, int starti, int startj, int endi, int endj, int tileIndex, int currLayer)
    {
        ArrayList<String> routeSiteNames = new ArrayList<>();

        if(currLayer > 0)
        {
            //forming top-layer
            //left to right movement
            int curri = starti - currLayer; 
            if(curri < 0)
                return null;

            for(int currj = startj; currj <= endj; currj++)
            {
                MapElement mapElement = map.get(curri).get(currj);

                if(mapElement.isCLEL(mapElement.leftElementRootname) || mapElement.isCLEM(mapElement.leftElementRootname))
                {
                    String siteName = device.getTile(mapElement.leftElementName).getSites()[tileIndex].getName();
                    if(!siteName.startsWith("SLICE"))
                        return null;

                    routeSiteNames.add(siteName);
                }

                if(mapElement.isCLEL(mapElement.rightElementRootname) || mapElement.isCLEM(mapElement.rightElementRootname))
                {
                    String siteName = device.getTile(mapElement.rightElementName).getSites()[tileIndex].getName();
                    if(!siteName.startsWith("SLICE"))
                        return null;

                    routeSiteNames.add(siteName);
                }
            }

        }

        else
        {
            //forming bottom-layer
            //right - left movement
            int curri = endi - currLayer; //note currLayer is itself -ve so subtraction makes sense
            if(curri >= map.size())
                return null;

            for(int currj = endj; currj >= startj; currj--)
            {
                MapElement mapElement = map.get(curri).get(currj);
                if(mapElement.isCLEL(mapElement.rightElementRootname) || mapElement.isCLEM(mapElement.rightElementRootname))
                {
                    String siteName = device.getTile(mapElement.rightElementName).getSites()[tileIndex].getName();
                    if(!siteName.startsWith("SLICE"))
                        return null;

                    routeSiteNames.add(siteName);
                }

                if(mapElement.isCLEL(mapElement.leftElementRootname) || mapElement.isCLEM(mapElement.leftElementRootname))
                {
                    String siteName = device.getTile(mapElement.leftElementName).getSites()[tileIndex].getName();
                    if(!siteName.startsWith("SLICE"))
                        return null;

                    routeSiteNames.add(siteName);
                }
            }
        }

        return routeSiteNames;
    }

    /*
     * This sets the correct value of currLayer based on the increment value of the pblock and the previous currLayer value
     * @param currLayer This holds the previous currLayer value
     * @return This holds the new currLayer value
     */
    public int incrementLayer(Pblock pblock, int currLayer)
    {
        int inc = pblock.incFactor;

        if(currLayer == 0) //first layer
            return 1;

        if(currLayer > 0)
        {
            if((currLayer % inc) != 0) //Means the increment layer has not yet matched with the increment factor
                return (currLayer + 1); 

            return (((-1) * (currLayer - inc)) - 1);
        }

        //currlayer is -ve
        if((Math.abs(currLayer) % inc) != 0)
            return (currLayer - 1);

        return (Math.abs(currLayer) + 1);

    }

    /*if
     * This simply retrieves the names of the sites from the routeSiteNames and updates routeSitesArray
     * @return true if process was successful or else false
     */
    public void updateRouteSites(Design design, ArrayList<String> routeSiteNames, int currLayer)
    {
        for(String s : routeSiteNames)
        {
            Site site = design.getDevice().getSite(s);
            String siteName = "s" + Integer.toString(routeSites.size());
            routeSites.add(new RouteSite(site, siteName, clk, rst, currLayer));
        }
    }

    public ArrayList<RouteSite> siteSelector (Pblock pblock, Design design)
    {
        int incFactor = pblock.incFactor;
        ArrayList<ArrayList<MapElement>> map = MapElement.map;

        int starti = pblock.starti;
        int startj = pblock.startj;
        int endi = starti + pblock.currRow - 1;
        int endj = startj + pblock.currCol - 1;
        int numSitePerTile = 1;

        int currLayer = 0;

        while(routeSites.size() < minRouteSites)
        {
            currLayer = incrementLayer(pblock, currLayer);

            ArrayList<String> routeSiteNames = formUniLayer(map, design.getDevice(), starti, startj, endi, endj, numSitePerTile - 1, currLayer);
            if(routeSiteNames == null)
                return null;
        
            updateRouteSites(design, routeSiteNames, currLayer);
        }
        
        return routeSites;
    }

    public int sitesPlacer (ArrayList<RouteSite> routeSites, Design design)
    {
        int index = 0;
        int currLayer = 0;

        for(int i = 0; i < minFlops; i++)
        {
            Flop flop = null;
            int count = 0;

            while(flop == null)
            {
                index = index % routeSites.size();
                RouteSite tempRouteSite = routeSites.get(index++);

                if((tempRouteSite.layer == currLayer) && (tempRouteSite.flopsPlaced < RouteSite.maxFlops))
                {
                    flop = tempRouteSite.createAndPlaceFlop(design, tempRouteSite.flopsPlaced);
                    if(flop == null)
                        return -1;
                }

                else
                {
                    count++;
                    if(count == routeSites.size())
                    {
                        currLayer = incrementLayer(pblock, currLayer);
                        count = 0;
                    }
                }
            }
        }

        return minFlops;
    }

    //This connects the input pins of the module with the flops
    public int connectInputPins(Pblock pblock, ArrayList<RouteSite> routeSites, EDIFCellInst moduleEDIFCellInst, EDIFCell top, boolean isVHDL)
    {
        Map<String, ArrayList<Integer>> buses = pblock.component.modulePorts.buses;

        int currLayer = 1;
        int index = 0;
        int num = 0;

        for (Map.Entry<String,ArrayList<Integer>> entry : buses.entrySet()) 
        {
            String busName = entry.getKey();
            int busWidth = entry.getValue().get(0);
            int direction = entry.getValue().get(1);

            if(direction == 1) //Means output pin
                continue;

            if(busName.equals(ModulePorts.CLK) || busName.equals(ModulePorts.RST))
                continue;

            //Apparently some buses have width of 0 and RW cannot read these buses without "[0]" added in the end of the bus name
            //This [0] is present in the synthesized designs generated using the vhdl method
            if(ModulePorts.isBusFromName(busName) && isVHDL && (busWidth == 1))
                busName += "[0]";
    
            for(int i = 0; i < busWidth; i++)
            {
                EDIFNet inputNet = null;
                int count = 0;

                while(inputNet == null)
                {
                    index = index % routeSites.size();
                    RouteSite tempRouteSite = routeSites.get(index++);

                    if((tempRouteSite.layer == currLayer) && (tempRouteSite.flopsRouted < RouteSite.maxFlops))
                    {
                        Flop flop = tempRouteSite.flops[tempRouteSite.flopsRouted];
                        inputNet = flop.connectToFlopOutput(busName, i, moduleEDIFCellInst, top);
                        if(inputNet == null)
                            return -1;

                        num++;
                        designRouteNets.add(inputNet);
                        tempRouteSite.flopsRouted++;
                    }

                    else
                    {
                        count++;
                        if(count == routeSites.size())
                        {
                            currLayer = incrementLayer(pblock, currLayer);
                            count = 0;
                        }
                    }
                }
            }
        }
        return num;
    }

    //This connects the output pins of the module with the flops
    public int connectOutputPins(Pblock pblock, ArrayList<RouteSite> routeSites, EDIFCellInst moduleEDIFCellInst, EDIFCell top, boolean isVHDL)
    {
        Map<String, ArrayList<Integer>> buses = pblock.component.modulePorts.buses;

        int currLayer = 1;
        int index = 0;
        int num = 0;

        for (Map.Entry<String,ArrayList<Integer>> entry : buses.entrySet()) 
        {
            String busName = entry.getKey();
            int busWidth = entry.getValue().get(0);
            int direction = entry.getValue().get(1);

            if(direction == 0) //Means input pin
                continue;

            //Apparently some buses have width of 0 and RW cannot read these buses without "[0]" added in the end of the bus name
            if(ModulePorts.isBusFromName(busName) && isVHDL && (busWidth == 1))
                busName += "[0]";

            for(int i = 0; i < busWidth; i++)
            {
                //Checking if the output port of the module is a hanging port or not.
                //If it is a hanging port, then we can skip connecting he port to a flop
                EDIFPort modulePort = moduleEDIFCellInst.getPort(busName);
                EDIFNet internalNet;

                //Previous checks
                // if(ModulePorts.isBusFromName(busName))
                //     internalNet = modulePort.getInternalNet(i);
                // else
                //     internalNet = modulePort.getInternalNet();

                if(busWidth == 1)
                    internalNet = modulePort.getInternalNet();

                else
                    internalNet = modulePort.getInternalNet(i);

                if(internalNet == null)
                    continue;

                EDIFNet outputNet = null;
                int count = 0;

                while(outputNet == null)
                {
                    index = index % routeSites.size();
                    RouteSite tempRouteSite = routeSites.get(index++);

                    if((tempRouteSite.layer == currLayer) && (tempRouteSite.flopsRouted < RouteSite.maxFlops))
                    {
                        Flop flop = tempRouteSite.flops[tempRouteSite.flopsRouted];
                        
                        outputNet = flop.connectToFlopInput(busName, i, moduleEDIFCellInst, top);
                        if(outputNet == null)
                            return -1;

                        num++;
                        designRouteNets.add(outputNet);
                        tempRouteSite.flopsRouted++;
                    }

                    else
                    {
                        count++;
                        if(count == routeSites.size())
                        {
                            currLayer = incrementLayer(pblock, currLayer);
                            count = 0;
                        }
                    }
                }
            }
        }
        return num;
    }

    //Connects the I/O pins of the module
    public int sitesConnector (Pblock pblock, ArrayList<RouteSite> routeSites, EDIFCellInst moduleEDIFCellInst, Design design, boolean isVHDL)
    {
        int inputNets = connectInputPins(pblock, routeSites, moduleEDIFCellInst, design.getNetlist().getTopCell(), isVHDL); //Connects the input pins of the module
        if(inputNets == -1)
            return -1;

        for(RouteSite r : routeSites)
            r.flopsRouted = 0;
        
        int outputNets = connectOutputPins(pblock, routeSites, moduleEDIFCellInst, design.getNetlist().getTopCell(), isVHDL); //Connects the output pins of the module
        if(outputNets == -1)
            return -1;

        return inputNets + outputNets;
    }

    //Connects the remianing pins of the flops to the design ports
    public ArrayList<EDIFPort> connectRemainingPins (ArrayList<RouteSite> routeSites, Design design, boolean isVHDL)
    {
        EDIFCell top = design.getNetlist().getTopCell();

        int inputPins, outputPins;
        inputPins = outputPins = 0;

        for(RouteSite r : routeSites)
        {
            for(int i = 0; i < r.flops.length; i++)
            {
                Flop flop = r.flops[i];
                if(flop == null)
                    continue;

                if(flop.inputNet == null)
                    inputPins++;

                if(flop.outputNet == null)
                    outputPins++;

                if(flop.ceNet == null)
                    inputPins++;
            }
        }

        EDIFPort inputPort, outputPort;
        inputPort = outputPort = null;
        EDIFNet inputNet = null;
        int outputIndex = 0;

        if(inputPins > 0)
        {
            inputPort = top.createPort("inputPort", EDIFDirection.INPUT, 1);
            inputNet = top.createNet("inputNet");
            inputNet.createPortInst(inputPort);
            designRouteNets.add(inputNet);
            designPorts.add(inputPort);
        }

        if(outputPins > 0)
        {
            if(outputPins == 1)
                outputPort = top.createPort("outputPort", EDIFDirection.OUTPUT, outputPins);

            else
                outputPort = top.createPort("outputPort[" + (outputPins-1) + ":0]", EDIFDirection.OUTPUT, outputPins);

            designPorts.add(outputPort);
        }

        for(RouteSite r : routeSites)
        {
            for(int i = 0; i < r.flops.length; i++)
            {
                Flop flop = r.flops[i];
                if(flop == null)
                    continue;

                if(flop.inputNet == null)
                    flop.connectToFlopInput(inputNet);

                if(flop.ceNet == null)
                    flop.connectToFlopCE(inputNet);

                if(flop.outputNet == null)
                {
                    EDIFNet outputNet = flop.connectToFlopOutput(outputPort, outputIndex, top);
                    outputIndex++;
                    designRouteNets.add(outputNet);
                }
                
            }
        }

        return designPorts;

    }

    //Main function which orchestrates the connecting of the flops with the module and then connecting the remianing pins of the flops to design ports
    public Circular(Pblock p)
    {
        pblock = p;
        boolean isVHDL = pblock.component.isVHDL;

        design = new Design(pblock.pblockName, "xcvu13p-fsga2577-1-i");

        Module module = new Module(Design.readCheckpoint(pblock.component.moduleLoc), pblock.component.metaLoc);
        design.createModuleInst(pblock.pblockName + "_cell", module);
        EDIFCellInst moduleEDIFCellInst = module.getNetlist().getTopCell().createCellInst(pblock.pblockName + "_cell", module.getNetlist().getTopCell());

        designPorts = new ArrayList<>();
        designRouteNets = new ArrayList<>();
        routeSites = new ArrayList<>();

        EDIFPort clkPort = design.getNetlist().getTopCell().createPort(ModulePorts.CLK, EDIFDirection.INPUT, 1);
        designPorts.add(clkPort);
        designRouteNets.add(createCLKNet(design, clkPort, moduleEDIFCellInst));
        clk = designRouteNets.get(0);

        
        EDIFPort rstPort = design.getNetlist().getTopCell().createPort(ModulePorts.RST, EDIFDirection.INPUT, 1);
        designPorts.add(rstPort);
        designRouteNets.add(createRSTNet(design, rstPort, moduleEDIFCellInst));
        rst = designRouteNets.get(1);

        minFlops = minFlops(pblock);
        minLUTs = minLUTs(pblock);
        minRouteSites = minRouteSites(minFlops, minLUTs, pblock);

        if(siteSelector(pblock, design) == null)
            System.out.println("ERROR: Could not select sites appropriately");

        if(sitesPlacer(routeSites, design) == -1)
            System.out.println("ERROR: Could not place sites appropriately");

        if(sitesConnector(pblock, routeSites, moduleEDIFCellInst, design, isVHDL) == -1)
            System.out.println("ERROR: Could not connect sites appropriately");

        if(connectRemainingPins(routeSites, design, isVHDL) == null)
            System.out.println("ERROR: Could not connect remaining pins appropriately");

        if(!PinExposedTclGenerator.preRouteTclGenerator(this))
            System.out.println("ERROR: Could not generate pre-route tcl script");

        if(!PinExposedTclGenerator.postRouteTclGenerator(this))
            System.out.println("ERROR: Could not generate post-route tcl script");

        design.getNetlist().resetParentNetMap();
        design.setAutoIOBuffers(false);
        design.writeCheckpoint(LocationParser.exposedDCPs + pblock.component.dcpName + "/" + pblock.pblockName + "_connected.dcp");
    }
}
