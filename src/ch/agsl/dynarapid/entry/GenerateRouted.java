/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.entry;
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


//This routes the designs simply either filly or partially based on teh arguments passed

import com.xilinx.rapidwright.design.DesignTools;
import com.xilinx.rapidwright.design.Cell;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Module;
import com.xilinx.rapidwright.design.ModuleInst;
import com.xilinx.rapidwright.design.SiteInst;
import com.xilinx.rapidwright.design.SitePinInst;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.BEL;
import com.xilinx.rapidwright.device.BELPin;
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
import com.xilinx.rapidwright.router.Router;
import com.xilinx.rapidwright.placer.handplacer.HandPlacer;
import com.xilinx.rapidwright.rwroute.RWRoute;
import com.xilinx.rapidwright.rwroute.PartialRouter;

import java.io.*;
import java.util.*;
import java.lang.*;

public class GenerateRouted {

    //This routes the design partially with the unconnected inputs being grounded
    // public static boolean routeDesignPartially(Design design, Map<String, Node> nodes, String finalLocation)
    // {
    //     StringUtils.printIntro("Started partial routing using RWRouter");
    //     design.getNetlist().resetParentNetMap();
    //     DesignTools.makePhysNetNamesConsistent(design);

    //     DesignTools.createMissingSitePinInsts(design);
    //     for (Map.Entry<String,Node> entry : nodes.entrySet()) 
    //     {
    //         Node node = entry.getValue();
    //         if(node.isUnconnected)
    //         {
    //             ModuleInst inst = node.moduleInst;
    //             inst.tieOffUnconnectedInputs(true);
    //         }
    //     }

    //     boolean softPreserve = true;
    //     design = PartialRouter.routeDesignPartialNonTimingDriven(design, null, softPreserve);
    //     design.writeCheckpoint(finalLocation);
    //     return true;
    // }
    
    //This helps route the design partially and puts the design in the final location
    public static boolean routeDesignPartially(Design design, String finalLocation)
    {
        StringUtils.printIntro("Started partial routing using RWRouter");
        design.getNetlist().resetParentNetMap();
        DesignTools.makePhysNetNamesConsistent(design);
        boolean softPreserve = true;
        design = PartialRouter.routeDesignPartialNonTimingDriven(design, null, softPreserve);
        design.writeCheckpoint(finalLocation);
        return true;
    }

    // public static boolean routeDesignFully(Design design, Map<String, Node> nodes, String finalLocation)
    // {
    //     StringUtils.printIntro("Started full routing using RWRouter");
    //     design.getNetlist().resetParentNetMap();
    //     DesignTools.makePhysNetNamesConsistent(design);

    //     DesignTools.createMissingSitePinInsts(design);
    //     for (Map.Entry<String,Node> entry : nodes.entrySet()) 
    //     {
    //         Node node = entry.getValue();
    //         if(node.isUnconnected)
    //         {
    //             ModuleInst inst = node.moduleInst;
    //             inst.tieOffUnconnectedInputs(true);
    //         }
    //     }

    //     design = RWRoute.routeDesignFullNonTimingDriven(design);
    //     design.writeCheckpoint(finalLocation);
    //     return true;
    // }

    //Thsi routes the design and puts it in teh given final location
    //NOTE: The final location must have the entire loccation including the name
    public static boolean routeDesignFully(Design design, String finalLocation)
    {
        StringUtils.printIntro("Started full routing using RWRouter");
        design.getNetlist().resetParentNetMap();
        DesignTools.makePhysNetNamesConsistent(design);
        design = RWRoute.routeDesignFullNonTimingDriven(design);
        design.writeCheckpoint(finalLocation);
        return true;
    }

    public static void main(String args[])
    {
        if((StringUtils.findInArray("-f", args) ==  -1))
        {
            System.out.println("<usage>: java GenerateRouted [-f] [-partial]");
            System.out.println("-f <arg> - Location of the dcp file which needs to be routed");
            System.out.println("-partial - This routes the design partially and if not given this flag then the design routes the design fully");
            return;
        }

        String dcpLoc = args[StringUtils.findInArray("-f", args) + 1];
        String finalLoc = LocationParser.designs + StringUtils.getGraphName(dcpLoc) + "_routed.dcp";
        Design design = Design.readCheckpoint(dcpLoc);
        boolean status = false;

        if(StringUtils.findInArray("-partial", args) ==  -1)
            status = routeDesignFully(design, finalLoc);

        else    
            status = routeDesignPartially(design, finalLoc);

        if(status)
            System.out.println("Completed Routing Design. You can find design in " + LocationParser.designs + " folder");

        else
            System.out.println("Could not route design");
    }
}
