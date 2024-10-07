/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/



//This class holds the LUTs to make the module routable
//This holds the LUT of type LUT6.
//Can fit in SLICEL as well as SLICEM.
//Input pins are: I0 - I5
//Output pins is: O

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

public class LUT {
    
    String lutName;
    Cell lutCell;
    EDIFCellInst lutCellInst;
    BEL bel;
    Site site;

    //LUT inputs
    EDIFNet inputNet [] = new EDIFNet[6];
    EDIFCellInst inputEDIF [] = new EDIFCellInst[6];
    String inputPortName [] = new String [6];

    //LUT outputs
    EDIFNet outputNet;
    ArrayList<EDIFCellInst> outputEDIF;
    ArrayList<String> outputPortName;

    public LUT (Design design, String name, BEL b, Site s)
    {
        lutName = name;
        bel = b;
        site = s;

        outputNet = null;

        lutCell = design.createAndPlaceCell(design.getNetlist().getTopCell(), name, Unisim.LUT6, s, b);
        lutCellInst = lutCell.getEDIFCellInst();

        outputEDIF = new ArrayList<>();
        outputPortName = new ArrayList<>();
    }

    /*
     * This connects the LUT input pin of lutIndex with the port name mentioned in EDIFPortName in the module moduleEDIFCellInst
     * @param lutIndex Index of the input pin of the LUT where the module input needs to be connected
     * @param EDIFPortName Name of the port to which the flop D pin is connected to. Here the port must not be a bus
     * @param moduleEDIFCellInst the module which is mentioned above
     * @return Creates and gives the net which connectes the flop with the module
     */
    public EDIFNet connectToLUTInput (int lutIndex, String EDIFPortName, EDIFCellInst moduleEDIFCellInst, EDIFCell top)
    {
        if(inputNet[lutIndex] != null)
        {
            System.out.println("ERROR: Trying to connect " + EDIFPortName + " of module " + moduleEDIFCellInst.getName() + " with LUT " + lutName + " input " + lutIndex + " which is already connected");
            return null;
        }

        String portName = "I" + lutIndex;
        inputNet[lutIndex] = top.createNet(lutName + "InputNet");
        inputNet[lutIndex].createPortInst(lutCellInst.getPort(portName), lutCellInst);
        inputEDIF[lutIndex] = moduleEDIFCellInst;
        inputPortName[lutIndex] = EDIFPortName;

        if(inputNet[lutIndex].createPortInst(moduleEDIFCellInst.getPort(EDIFPortName), moduleEDIFCellInst) == null)
        {
            inputNet[lutIndex] = null;
            inputEDIF[lutIndex] = null;
            inputPortName[lutIndex] = "";
        }

        return inputNet[lutIndex];
    }

    /*
     * This connects the LUT input pin of lutIndex with the port name mentioned in EDIFPortName in the bus index mentioned in the module moduleEDIFCellInst 
     * @param lutIndex Index of the input pin of the LUT where the module input needs to be connected
     * @param EDIFPortName Name of the port to which the flop D pin is connected to. Here the port may or may not be a bus
     * @param moduleEDIFCellInst the module which is mentioned above
     * @return Creates and gives the net which connectes the flop with the module
     */
    public EDIFNet connectToLUTInput (int lutIndex, String EDIFPortName, int index, EDIFCellInst moduleEDIFCellInst, EDIFCell top)
    {
        if(inputNet[lutIndex] != null)
        {
            System.out.println("ERROR: Trying to connect " + EDIFPortName + " of module " + moduleEDIFCellInst.getName() + " of index " + index + " with LUT " + lutName + " input " + lutIndex + " which is already connected");
            return null;
        }

        if(!moduleEDIFCellInst.getPort(EDIFPortName).isBus())
            return connectToLUTInput(lutIndex, EDIFPortName, moduleEDIFCellInst, top);

        String portName = "I" + lutIndex;
        inputNet[lutIndex] = top.createNet(lutName + "InputNet");
        inputNet[lutIndex].createPortInst(lutCellInst.getPort(portName), lutCellInst);
        inputEDIF[lutIndex] = moduleEDIFCellInst;
        inputPortName[lutIndex] = EDIFPortName;
    
        if(inputNet[lutIndex].createPortInst(moduleEDIFCellInst.getPort(EDIFPortName), index, moduleEDIFCellInst) == null)
        {
            inputNet[lutIndex] = null;
            inputEDIF[lutIndex] = null;
            inputPortName[lutIndex] = "";
        }
    
        return inputNet[lutIndex];
    }

    /*
     * Connects the input pin of the LUT (lutIndex pin) with the already created net. 
     * This function makes no changes to the inputEDIF or inputPortName. 
     * @param net The input net to which the pin is connected
     */
    public EDIFNet connectToLUTInput (int lutIndex, EDIFNet net)
    {
        if(inputNet[lutIndex] != null)
        {
            System.out.println("ERROR: Trying to connect net " + net + " with LUT " + lutName + " input " + lutIndex + " which is already connected");
            return null;
        }

        String portName = "I" + lutIndex;
        inputNet[lutIndex] = net;
        inputNet[lutIndex].createPortInst(lutCellInst.getPort(portName), lutCellInst);
        return inputNet[lutIndex];
    }

    /*
     * Connects the array of ports mentioned in teh arrays with the input pins of the LUT with index matching
     * If a perticular LUT pin is not to be connected. EDIFPortName of that index must be ""
     */
    public EDIFNet[] connectToLUTInputArray (String EDIFPortName[], int index[], EDIFCellInst moduleEDIFCellInst[], EDIFCell top)
    {
        for(int i = 0; i < 6; i++)
            connectToLUTInput(i, EDIFPortName[i], index[i], moduleEDIFCellInst[i], top);

        for(int i = 0; i < 6; i++)
            if(!EDIFPortName[i].equals("") && (inputNet[i] == null))
                System.out.println("ERROR: Could not connect input port I" + i + " of LUT " + lutName);

        return inputNet;
    }

    /*
    * Connects the array of nets mentioned in the arrays with the input pins of the LUT with index matching
    * If a perticular LUT pin is not to be connected. EDIFNet of that index must be null
    */
    public EDIFNet[] connectToLUTInputArray (EDIFNet net[])
    {
        for(int i = 0; i < 6; i++)
            connectToLUTInput(i, inputNet[i]);

        for(int i = 0; i < 6; i++)
            if((net[i] != null) && (inputNet[i] == null))
                System.out.println("ERROR: Could not connect input port I" + i + " of LUT " + lutName);

        return inputNet;
    }

    /*
     * This connects the LUT O pin with the port name mentioned in EDIFPortName in the module moduleEDIFCellInst
     * @param EDIFPortName Name of the port to which the flop Q pin is connected to. Here the port must not be a bus
     * @param moduleEDIFCellInst the module which is mentioned above
     * @return Creates and gives the net which connectes the flop with the module
     */
    public EDIFNet connectToLUTOutput (String EDIFPortName, EDIFCellInst moduleEDIFCellInst, EDIFCell top)
    {

        if(outputNet != null)
        {
            System.out.println("ERROR: Trying to connect " + EDIFPortName + " of module " + moduleEDIFCellInst.getName() + " with LUT " + lutName + " output which is already connected");
            return null;
        }

        outputNet = top.createNet(lutName + "outputNet");
        outputNet.createPortInst(lutCellInst.getPort("O"), lutCellInst);
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
     * This connects the LUT O pin with the port name mentioned in EDIFPortName in the bus index mentioned in the module moduleEDIFCellInst 
     * @param EDIFPortName Name of the port to which the flop Q pin is connected to. Here the port may or may not be a bus
     * @param moduleEDIFCellInst the module which is mentioned above
     * @return Creates and gives the net which connectes the flop with the module
     */
    public EDIFNet connectToLUTOutput (String EDIFPortName, int index, EDIFCellInst moduleEDIFCellInst, EDIFCell top)
    {
        if(outputNet != null)
        {
            System.out.println("ERROR: Trying to connect " + EDIFPortName + " of module " + moduleEDIFCellInst.getName() + " of index " + index + " with LUT " + lutName + " output which is already connected");
            return null;
        }

        if(!moduleEDIFCellInst.getPort(EDIFPortName).isBus())
            return connectToLUTOutput(EDIFPortName, moduleEDIFCellInst, top);

        outputNet = top.createNet(lutName + "outputNet");
        outputNet.createPortInst(lutCellInst.getPort("O"), lutCellInst);
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
     * This connects the O pin of teh flop to the design port. Note that the port is not of a module.
     * @param outputPort This is the design port and a bus as well
     * @param index The bus index
     * @return This creates and returns the net which connects the design port to the flop pin
     */
    public EDIFNet connectToLUTOutput (EDIFPort outputPort, int index, EDIFCell top) //This does not add the net to  outputEDIF
    {
        outputNet = top.createNet(lutName + "OutputNet");
        outputPortName.add(outputPort.getBusName());

        outputNet.createPortInst(outputPort, index);
        outputNet.createPortInst(lutCellInst.getPort("O"), lutCellInst);
        return outputNet;
    }
}
