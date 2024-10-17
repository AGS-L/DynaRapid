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


//Interface which must be implemented for flop and LUT placement and routing algorithms.
//Have a constructor which orchestrates all the functions

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

public interface PinExposer {

    /*
     * @return Returns the final design after making the design.
     */
    public Design returnDesign ();

    /*
     * @return Returns thepblock associated
     */
    public Pblock returnPblock();

    /*
     * @return Returns the list of routeSites used for the pblock
     */
    public ArrayList <RouteSite> returnRouteSites(); 

    /*
     * @return Returns the routing nets used in the design.
     * The 0th and the 1st net is the clk and rst net respectively
     */
    public ArrayList <EDIFNet> returnDesignRouteNets();

    /*
     * @return Returns the design ports used in the design
     * The 0th and the 1st port is the clk and rst port respectively
     */
    public ArrayList <EDIFPort> returnDesignPorts();

    /*
     * @return Returns the sites that have been used for exposing the I/Os
     */
    public ArrayList <Site> returnSites();

    /*
     * This gives a net for the clk. This net is connected to all clk pins. This also connects the moduleInst clk pin to the net
     * @param clkPort this is the clkport of the design
     * @return returns the clk net
     */
    public EDIFNet createCLKNet (Design design, EDIFPort clkPort, EDIFCellInst moduleEDIFCellInst);

    /*
     * This gives a net for the rst. This net is connected to all rst pins. This also connects the moduleInst rst pin to the net
     * @param rstPort this is the rstport of the design
     * @return returns the rst net
     */
    public EDIFNet createRSTNet (Design design, EDIFPort rstPort, EDIFCellInst moduleEDIFCellInst);

    /*
     * This gives the minimum number of flops required according to the algorithm and the busMap of the device
     * @param busMap map of the buses of the module
     * @return The number of flops required
     */
    public int minFlops (Pblock pblock);

    /*
     * This gives the minimum number of LUTs required according to the algorithm and the busMap of the device
     * @param busMap map of the buses of the module
     * @return The number of LUTs required
     */
    public int minLUTs (Pblock pblock);

    /*
     * This gives the minimum number RouteSites required according to the algorithm and the minFlops and minLUTs
     * @param minFlops minimum number of the flops required in the design
     * @param minLUTs minimum number of the LUTs required in the design
     * @return minimum number of the sites
     */
    public int minRouteSites (int minFlops, int minLUTs, Pblock pblock);

    /*
     * This selects the sites that should be used for placing the flops and the LUTs in them.
     * @param pblock The pblock of the component around which the flops are to be placed
     * @return list of routeSites to be used around the component
     */
    public ArrayList<RouteSite> siteSelector (Pblock pblock, Design design); 

    /*
     * This has the placer algorithm for the placement of the LUTs and the flops.
     * @param routeSites This has the sites which can be used for placing the flops / LUTs
     * @return returns the total number of flops and LUTs placed. Returns -1 in case of some error
     */
    public int sitesPlacer (ArrayList<RouteSite> routeSites, Design design);

    /*
     * This has the algorithm to connect the flops and the LUTs with the module
     * @param routeSites Has the list of sites where the luts and the flops have been placed
     * @param PBlock The pblock of the component around which the flops are to be placed
     * @return returns the total number of connections made. Returns -1 in case of any error
     */
    public int sitesConnector (Pblock pblock, ArrayList<RouteSite> routeSites, EDIFCellInst moduleEDIFCellInst, Design design, boolean isVHDL);

    /*
     * This allows the module, flop and LUT pins to be connected with design ports
     * @param routeSites Has the list of sites where the luts and the flops have been placed
     * @return This returns the design EDIFPorts.
     */
    public ArrayList<EDIFPort> connectRemainingPins (ArrayList<RouteSite> routeSites, Design design, boolean isVHDL);
}
