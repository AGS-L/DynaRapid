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
//This class holds the input pin of a node in the dot graph

import com.xilinx.rapidwright.design.DesignTools;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Module;
import com.xilinx.rapidwright.design.ModuleInst;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.design.Port;
import com.xilinx.rapidwright.design.SiteInst;
import com.xilinx.rapidwright.design.SitePinInst;
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
import com.xilinx.rapidwright.router.Router;
import com.xilinx.rapidwright.placer.handplacer.HandPlacer;
import com.xilinx.rapidwright.rwroute.RWRoute;

import java.io.*;
import java.util.*;
import java.lang.*;

public class Input implements Serializable
{

    //This is the dangling input port which is connected to some input port
    public static Input dangling = null;
;
    /*
     * dataIn <- input <- bus <--------------------------------------------------<- bus <- output <- dataOut
     * pValidArray <- input <- pin <---------------------------------------------<- bus <- output <- validArray
     * readyArray -> output -> pin ------------------------------------------> pin -> input -> nReadyArray
     */

    public static final String underscore = "_";

    public Node node;
    public int index;

    public int bitSize = 0;
    public boolean bitSizeWasZero = false;

    public String type = "u";
    public int port = 0;
    public String infoType = "u";

    //connection information
    public Output [] dataOutArray; //The dataInArray of this bundle is connected to which dataOutArray
    public Output validArray = null; //The pValidArray of this bundle is connected to which validArray
    public Output nReadyArray = null; //The readyArray (which is output) of this bundle is connected to which nReadyArray of which output bundle

    //Constructor to initialise the dangling input
    private Input()
    {
        node = null;
        index = -1;
        bitSize = -1;

        type = "*";
        port = -1;
        infoType = "*";

        dangling = this;
    }

    public Input(Node n, int i, String token)
    {

        //Creating the dangling port if not created
        if(dangling == null)
            new Input();

        //Setting the node and the index
        node = n;
        index = i;

        //setting the bitSize and bitSizeWasZero
        int start = token.indexOf(":") + 1;
        int end = token.indexOf("*");

        if(end == -1)
            bitSize = Integer.parseInt(token.substring(start));

        else
            bitSize = Integer.parseInt(token.substring(start, end));

        if(bitSize == 0) //if bit size is 0 then it is forced to 1 (not done apparently)
        {
            bitSizeWasZero = true;
            //bitSize++;
        }

        //////////////////////////////////////////////
        if((bitSize != 0) && (bitSize != 1))
            bitSize = 32;
        //////////////////////////////////////////////

        int index = end;

        //sets the type of input
        if((index != -1) && ((index + 1) < token.length()))
            type = token.substring(index + 1, index + 2);

        //sets the port
        if((index != -1) && ((index + 2) < token.length()))
            if(Character.isDigit(token.charAt(index + 2)))
                port = Integer.parseInt(token.substring(index + 2, index + 3));

        //Sets the infoType
        if((index != -1) && ((index + 3) < token.length()))
            infoType = token.substring(index + 3, index + 4);

        dataOutArray = new Output [bitSize];
        for(int j = 0; j < bitSize; j++)
            dataOutArray[j] = null;
    }

    //This makes the connection of this input pin with the given output pin and node
    public void connectToOutput(Node outputNode, int outputIndex)
    {
        for(int i = 0; i < dataOutArray.length; i++)
            dataOutArray[i] = outputNode.outputs.get(outputIndex);

        validArray = outputNode.outputs.get(outputIndex);
        nReadyArray = outputNode.outputs.get(outputIndex);
    }

    //This helps remove the input connection from the input
    //This helps if the output node is to be removed
    public void removeOutputConnection()
    {
        for(int i = 0; i < dataOutArray.length; i++)
            dataOutArray[i] = null;

        validArray = null;
        nReadyArray = null;
    }

    //This removes the input of the node since the database of this node was not found
    public void removeItself()
    {
        Output o = null;
        for(int i = 0; i < dataOutArray.length; i++)
        {
            if((dataOutArray[i] == o) || (dataOutArray[i] == Output.GND) || (dataOutArray[i] == Output.VCC) || (dataOutArray[i] == null))
                continue;

            o = dataOutArray[i];
            o.removeInputConnection();
        }

        if((validArray != o) && (validArray != Output.GND) && (validArray != Output.VCC) && (validArray != null))
        {
            o = validArray;
            o.removeInputConnection();
        }

        if((nReadyArray != o) && (nReadyArray != Output.GND) && (nReadyArray != Output.VCC) && (nReadyArray != null))
        {
            o = nReadyArray;
            o.removeInputConnection();
        }
    }

    //This creates an adjustment buffer in case of bitwidth mismatch
    public AdjustmentBuffer getAdjustmentBufferForBitwidthMismatch()
    {
        System.out.println("Checking connections for bitwidth mismatch of node: " + node.name + " of index: " + index);
        if(bitSize == 0)
            return null;

        if(dataOutArray[0] == null)
            return null;

        if(bitSize == dataOutArray[0].bitSize)
            return null;

        AdjustmentBuffer buffer = new AdjustmentBuffer("_adj_buffer_" + node.actualName + "_" + index, this, dataOutArray[0]);
        System.out.println("Created adjustent buffer for input node: " + node.actualName + ". Name of the buffer: _adj_buffer_" + node.actualName+ "_" + index);
        return buffer;
    }

    //This function updates the connection if there is a bitsize mis-matchin the data pins
    public void updateConnectionsForBitwidthMismatch() throws IOException
    {
        //0 is taken as a sample port. We can use anny pin which follows: 0 <= pinNum < bitSize
        System.out.println("Checking connections for bitwidth mismatch of node: " + node.name + " of index: " + index);

        if(bitSize == 0)
            return;

        if(dataOutArray[0] == null)
            return;

        if(bitSize == dataOutArray[0].bitSize)
            return;

        //throw new IOException("ERROR: " + node.name + " of index: " + index + " has bitwidth mismatch.\nUse Adjustment buffers to rectify dot file.\nExiting design generation.");

        //Activating the previous bitwidth mismatch solution
        for(int i = 0; i < dataOutArray[0].bitSize; i++)
            dataOutArray[0].dataInArray[i] = null;

        for(int i = 0; i < bitSize; i++)
            dataOutArray[i] = null;

        ErrorLogger.addError("Bitwidth Mismatch", 0, node.name + " has bitwidth mismatch at index " + (index + 1));

        //Disabling the new bitwidth mismatch solution
        // int minBitSize = Math.min(bitSize, dataOutArray[0].bitSize);

        // if((bitSize != 0) && (bitSize != minBitSize))
        //     node.isUnconnected = true;

        // for(int i = minBitSize; i < dataOutArray[0].bitSize; i++)
        //     dataOutArray[0].dataInArray[i] = Input.dangling;

        // for(int i = minBitSize; i < bitSize; i++)
        //     dataOutArray[i] = Output.GND;                
    }

    //This function creates the ports for the input pins which have not been connected after stitching th input pins
    public void createInputPorts()
    {
        System.out.println("Creating input ports: " + node.name + " or index: " + index);
        EDIFCell top = node.design.getNetlist().getTopCell();

        //For dataInArray
        int i;
        for(i = 0; i < bitSize; i++)
            if(dataOutArray[i] == null)
                break;

        if(i != bitSize) //means we need ports
        {
            if(bitSize > 1)
            {
                top.createPort(node.name + underscore + ModulePorts.DataIn + index + "_Port[" + (bitSize-1) + ":0]", EDIFDirection.INPUT, bitSize);
                for(int j = 0; j < bitSize; j++)
                    if(dataOutArray[j] == null)
                        node.moduleInst.connect(ModulePorts.DataIn + index, j, null, node.name + underscore + ModulePorts.DataIn + index + "_Port", j);
            }

            else
            {
                top.createPort(node.name + underscore + ModulePorts.DataIn + index + "_Port", EDIFDirection.INPUT, 1);
                node.moduleInst.connect(ModulePorts.DataIn + index, -1, null, node.name + underscore + ModulePorts.DataIn + index + "_Port", -1);
            }
        }

        //For pValidArray
        if(validArray == null)
        {
            top.createPort(node.name + underscore + ModulePorts.ValidIn + index + "_Port", EDIFDirection.INPUT, 1);
            node.moduleInst.connect(ModulePorts.ValidIn + index, -1, null, node.name + underscore + ModulePorts.ValidIn + index + "_Port", -1);
        }

        //for readyArray
        if(nReadyArray == null)
        {
            top.createPort(node.name + underscore + ModulePorts.ReadyOut + index + "_Port", EDIFDirection.OUTPUT, 1);
            node.moduleInst.connect(ModulePorts.ReadyOut + index, -1, null, node.name + underscore + ModulePorts.ReadyOut + index + "_Port", -1);
        }
    }

    //This stitches this input bundle with the required pins
    //This does not do anything with the ports to which null is connected. 
    //the null pins are to be connected 
    public void stitchToOutput()
    {
        System.out.println("Stitching the inputs of the node: " + node.name + " of index: " + index);

        ModuleInst inputModuleInst = node.moduleInst;
        ModuleInst outputModuleInst;

        //Connecting the dataInArray
        for(int i = 0; i < bitSize; i++)
        {
            if((dataOutArray[i] == Output.GND) || (dataOutArray[i] == Output.VCC) || (dataOutArray[i] == null))
                continue;

            outputModuleInst = dataOutArray[i].node.moduleInst;

            int inputIndex = (bitSize > 1) ? i : -1;

            int outputIndex = i;
            if(dataOutArray[i].bitSize == 1)
                outputIndex = -1;

            else if(dataOutArray[i].bitSize <= i)
                outputIndex = 0;

            outputModuleInst.connect(ModulePorts.DataOut + dataOutArray[i].index, outputIndex, inputModuleInst, ModulePorts.DataIn + index, inputIndex);
        }

        //Connecting the pValidArray
        if(validArray != null)
        {
            outputModuleInst = validArray.node.moduleInst;
            outputModuleInst.connect(ModulePorts.ValidOut + validArray.index, -1, inputModuleInst, ModulePorts.ValidIn + index, -1);
        }

        //Connecting the readyArray
        if(nReadyArray != null)
        {
            outputModuleInst = nReadyArray.node.moduleInst;
            outputModuleInst.connect(ModulePorts.ReadyIn + nReadyArray.index, -1, inputModuleInst, ModulePorts.ReadyOut + index, -1);
        }

        createInputPorts();
    }


}
