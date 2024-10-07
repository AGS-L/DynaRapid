/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/



//This holds all the sites which are used for routing exposure of the module.
//This holds the flops and luts used in the given site
//Note that only SLICE sites can be used for the routeSites

import com.google.protobuf.MapEntryLite;
import com.xilinx.rapidwright.design.*;
import com.xilinx.rapidwright.design.Module;
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

import org.python.antlr.base.mod;

import java.lang.*;

public class RouteSite {
    static final int maxLUTs = 8;
    static final int maxFlops = 16;

    Site site;
    String siteName; //This is not the device name but the name of the site user-defined
    EDIFNet clkNet, rstNet;

    Flop flops [] = new Flop [maxFlops];
    LUT luts [] = new LUT [maxLUTs];
    int flopsPlaced, lutsPlaced;
    int flopsRouted, lutsRouted;

    int layer; //This tells this site is in which layer of the pblock. +ve means in the top layer and -ve means in the bottom layer

    boolean isFilled = false;

    public RouteSite(Site s, String sn, EDIFNet clk, EDIFNet rst, int l)
    {
        site = s;
        siteName = sn;
        clkNet = clk;
        rstNet = rst;
        flopsPlaced = lutsPlaced = flopsRouted = lutsRouted = 0;
        layer = l;

        for(int i = 0; i < flops.length; i++)
            flops[i] = null;

        for(int j = 0; j < luts.length; j++)
            luts[j] = null;
    }

    //Returns space separted list of flops
    public String returnFlopsString()
    {
        String flopString = "";
        for(int i = 0; i < flops.length; i++)
            if(flops[i] != null)
                flopString += flops[i].flopName + " ";

        return flopString;
    }

    //Returns space separated list of LUTs
    public String returnLUTString()
    {
        String LUTString = "";
        for(int i = 0; i < luts.length; i++)
            if(luts[i] != null)
                LUTString += luts[i].lutName + " ";

        return LUTString;
    }

    //Return true if the routeSite is suitable for routing
    public boolean isRouteSiteSuitable()
    {
        return site.getName().startsWith("SLICE");
    }

    //Flop names are: siteName_fIndex
    public Flop createAndPlaceFlop(Design design, int index)
    {
        if(index >= maxFlops)
        {
            System.out.println("ERROR: index " + index + " of flop is not suitable");
            return null;
        }

        if(flops[index] != null)
        {
            System.out.println("ERROR: flop at index" + index + "has already been filled");
            return null;
        }

        String flopName = siteName + "_f" + Integer.toString(index);
        
        String belName = Character.toString((char)((int)('A') + (index/2))) + "FF" + ((index % 2 == 0) ? "" : "2");
        BEL bel = site.getBEL(belName);
        flops[index] = new Flop (design, flopName, bel, site, clkNet, rstNet);

        if(flops[index] != null)
        {
            flopsPlaced++;
            isFilled = (flopsPlaced < maxFlops) && (lutsPlaced < maxLUTs);
        }

        return flops[index];
    }

    //LUT names are: siteName_lIndex
    public LUT createAndPlaceLUT(Design design, int index)
    {
        if(index >= maxLUTs)
        {
            System.out.println("ERROR: index " + index + " of LUT is not suitable");
            return null;
        }

        if(luts[index] != null)
        {
            System.out.println("ERROR: LUT at index" + index + "has already been filled");
            return null;
        }

        String lutName = siteName + "_l" + Integer.toString(index);

        String belName = Integer.toString((char)((int)('A') + index)) + "6LUT";
        BEL bel = site.getBEL(belName);
        luts[index] = new LUT (design, lutName, bel, site);

        if(luts[index] != null)
        {
            lutsPlaced++;
            isFilled = (flopsPlaced < maxFlops) && (lutsPlaced < maxLUTs);
        }

        return luts[index];
    }
}
