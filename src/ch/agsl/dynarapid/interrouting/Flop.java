/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/


package ch.agsl.dynarapid.interrouting;
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
//This class holds the flops to make the module routable
//The input pins are: clk, rst, CE, D
//Output pin is: Q


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

public class Flop {

    public String flopName;
    public Cell ffcell;
    public EDIFCellInst ffCellInst;
    public BEL bel;
    public Site site;
    
    public EDIFNet inputNet, rstNet, clkNet, outputNet, ceNet;

    //Flop Inputs
    public EDIFCellInst inputEDIF, ceEDIF; //Incase the flop input is connected to a design port then this holds null
    public String inputPortName, cePortName; //Incase the flop input is connected to a design port then this holds ""

    //Flop Outputs
    public ArrayList<EDIFCellInst> outputEDIF; //In case the flop output holds a design port then it is of size 0
    public ArrayList<String> outputPortName; //In case the flop is connected to a design port, the name is mentioned here.

    public Flop(Design design, String name, BEL b, Site s, EDIFNet clk, EDIFNet rst)
    {  
        flopName = name;
        bel = b;
        site = s;
        clkNet = clk;
        rstNet = rst;
        inputNet = outputNet = ceNet = null;
        ffcell = design.createAndPlaceCell(design.getNetlist().getTopCell(), name, Unisim.FDRE, s, b);
        ffCellInst = ffcell.getEDIFCellInst();

        clkNet.createPortInst(ffCellInst.getPort("C"), ffCellInst);
        rstNet.createPortInst(ffCellInst.getPort("R"), ffCellInst);

        inputEDIF = null;
        inputPortName = "";

        ceEDIF = null;
        cePortName = "";

        outputEDIF = new ArrayList<>();
        outputPortName = new ArrayList<>();
        
    }

    /*
     * This connects the flop D pin with the port name mentioned in EDIFPortName in the module moduleEDIFCellInst
     * @param EDIFPortName Name of the port to which the flop D pin is connected to. Here the port must not be a bus
     * @param moduleEDIFCellInst the module which is mentioned above
     * @return Creates and gives the net which connectes the flop with the module
     */
    public EDIFNet connectToFlopInput (String EDIFPortName, EDIFCellInst moduleEDIFCellInst, EDIFCell top)
    {
        inputNet = top.createNet(flopName + "InputNet");
        
        inputNet.createPortInst(ffCellInst.getPort("D"), ffCellInst);
        inputEDIF = moduleEDIFCellInst;
        inputPortName = EDIFPortName;

        if(inputNet.createPortInst(moduleEDIFCellInst.getPort(EDIFPortName), moduleEDIFCellInst) == null)
        {
            inputNet = null;
            inputEDIF = null;
            inputPortName = "";
        }
        
        return inputNet;
    }

    /*
     * This connects the flop D pin with the port name mentioned in EDIFPortName in the bus index mentioned in the module moduleEDIFCellInst 
     * @param EDIFPortName Name of the port to which the flop D pin is connected to. Here the port may or may not be a bus
     * @param moduleEDIFCellInst the module which is mentioned above
     * @return Creates and gives the net which connectes the flop with the module
     */
    public EDIFNet connectToFlopInput (String EDIFPortName, int index, EDIFCellInst moduleEDIFCellInst, EDIFCell top)
    {
        if(!moduleEDIFCellInst.getPort(EDIFPortName).isBus())
            return connectToFlopInput(EDIFPortName, moduleEDIFCellInst, top);

        inputNet = top.createNet(flopName + "InputNet");
        
        inputNet.createPortInst(ffCellInst.getPort("D"), ffCellInst);
        inputEDIF = moduleEDIFCellInst;
        inputPortName = EDIFPortName;

        if(inputNet.createPortInst(moduleEDIFCellInst.getPort(EDIFPortName), index, moduleEDIFCellInst) == null)
        {
            inputNet = null;
            inputEDIF = null;
            inputPortName = "";
        }

        return inputNet;
    }

    /*
     * Connects the input pin of the flop (D pin) with the already created net. 
     * This function makes no changes to the inputEDIF or inputPortName. 
     * @param net The input net to which the pin is connected
     */
    public EDIFNet connectToFlopInput (EDIFNet net) //This does not add the net to  inputEDIF and inputPortsName
    {
        inputNet = net;
        inputNet.createPortInst(ffCellInst.getPort("D"), ffCellInst);
        return inputNet;
    }

     /*
     * This connects the flop Q pin with the port name mentioned in EDIFPortName in the module moduleEDIFCellInst
     * @param EDIFPortName Name of the port to which the flop Q pin is connected to. Here the port must not be a bus
     * @param moduleEDIFCellInst the module which is mentioned above
     * @return Creates and gives the net which connectes the flop with the module
     */
    public EDIFNet connectToFlopOutput (String EDIFPortName, EDIFCellInst moduleEDIFCellInst, EDIFCell top)
    {
        outputNet = top.createNet(flopName + "OutputNet");

        outputNet.createPortInst(ffCellInst.getPort("Q"), ffCellInst);
        outputEDIF.add(moduleEDIFCellInst);
        outputPortName.add(EDIFPortName);
        if(outputNet.createPortInst(moduleEDIFCellInst.getPort(EDIFPortName), moduleEDIFCellInst) == null)
        {
            outputNet = null;
            outputEDIF.remove(outputEDIF.size() - 1);
            outputPortName.remove(outputPortName.size() - 1);
        }

        return outputNet;
    }

    /*
     * This connects the flop Q pin with the port name mentioned in EDIFPortName in the bus index mentioned in the module moduleEDIFCellInst 
     * @param EDIFPortName Name of the port to which the flop Q pin is connected to. Here the port may or may not be a bus
     * @param moduleEDIFCellInst the module which is mentioned above
     * @return Creates and gives the net which connectes the flop with the module
     */
    public EDIFNet connectToFlopOutput (String EDIFPortName, int index, EDIFCellInst moduleEDIFCellInst, EDIFCell top)
    {
        if(!moduleEDIFCellInst.getPort(EDIFPortName).isBus())
            return connectToFlopOutput(EDIFPortName, moduleEDIFCellInst, top);

        outputNet = top.createNet(flopName + "OutputNet");

        outputNet.createPortInst(ffCellInst.getPort("Q"), ffCellInst);
        outputEDIF.add(moduleEDIFCellInst);
        outputPortName.add(EDIFPortName);
        if(outputNet.createPortInst(moduleEDIFCellInst.getPort(EDIFPortName), index, moduleEDIFCellInst) == null)
        {
            outputNet = null;
            outputEDIF.remove(outputEDIF.size() - 1);
            outputPortName.remove(outputPortName.size() - 1);
        }

        return outputNet;
    }

    /*
     * This connects the Q pin of teh flop to the design port. Note that the port is not of a module.
     * @param outputPort This is the design port and a bus as well
     * @param index The bus index
     * @return This creates and returns the net which connects the design port to the flop pin
     */
    public EDIFNet connectToFlopOutput (EDIFPort outputPort, int index, EDIFCell top) //This does not add the net to  outputEDIF
    {
        outputNet = top.createNet(flopName + "OutputNet");
        outputPortName.add(outputPort.getBusName());

        if(outputPort.isBus())
            outputNet.createPortInst(outputPort, index);

        else
            outputNet.createPortInst(outputPort);
            
        outputNet.createPortInst(ffCellInst.getPort("Q"), ffCellInst);
        return outputNet;
    }

    /*
     * This connects the flop CE pin with the port name mentioned in EDIFPortName in the module moduleEDIFCellInst
     * @param EDIFPortName Name of the port to which the flop D pin is connected to. Here the port must not be a bus
     * @param moduleEDIFCellInst the module which is mentioned above
     * @return Creates and gives the net which connectes the flop with the module
     */
    public EDIFNet connectToFlopCE (String EDIFPortName, EDIFCellInst moduleEDIFCellInst, EDIFCell top)
    {
        ceNet = top.createNet(flopName + "CENet");
        
        ceNet.createPortInst(ffCellInst.getPort("CE"), ffCellInst);
        ceEDIF = moduleEDIFCellInst;
        cePortName = EDIFPortName;

        if(ceNet.createPortInst(moduleEDIFCellInst.getPort(EDIFPortName), moduleEDIFCellInst) == null)
        {
            ceNet = null;
            ceEDIF = null;
            cePortName = "";
        }

        return ceNet;
    }

    /*
     * This connects the flop CE pin with the port name mentioned in EDIFPortName in the bus index mentioned in the module moduleEDIFCellInst 
     * @param EDIFPortName Name of the port to which the flop CE pin is connected to. Here the port may or may not be a bus
     * @param moduleEDIFCellInst the module which is mentioned above
     * @return Creates and gives the net which connectes the flop with the module
     */
    public EDIFNet connectToFlopCE (String EDIFPortName, int index, EDIFCellInst moduleEDIFCellInst, EDIFCell top)
    {
        if(!moduleEDIFCellInst.getPort(EDIFPortName).isBus())
            return connectToFlopCE(EDIFPortName, moduleEDIFCellInst, top);

        ceNet = top.createNet(flopName + "CENet");
        
        ceNet.createPortInst(ffCellInst.getPort("CE"), ffCellInst);
        ceEDIF = moduleEDIFCellInst;
        cePortName = EDIFPortName;

        if(ceNet.createPortInst(moduleEDIFCellInst.getPort(EDIFPortName), index, moduleEDIFCellInst) == null)
        {
            ceNet = null;
            ceEDIF = null;
            cePortName = "";
        }

        return ceNet;
    }

    /*
     * Connects the ce pin of the flop (Q pin) with the already created net. 
     * This function makes no changes to the inputEDIF or inputPortName. 
     * @param net The input net to which the pin is connected
     */
    public EDIFNet connectToFlopCE (EDIFNet net) //This does not add the net to ceEDIF and cePortsName
    {
        ceNet = net;
        ceNet.createPortInst(ffCellInst.getPort("CE"), ffCellInst);
        return ceNet;
    }

}
