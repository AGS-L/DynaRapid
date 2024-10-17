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


//This class holds the output pin of a node in the dot graph

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

public class Output implements Serializable
{
    //This holds the VCC and GND ports which can be connected to inputs which is created due to bitwidth mismatch
    public static Output VCC = null;
    public static Output GND = null;

    /*
     * These are the pins in this bundle:
     * dataOut -> output -> bus -----------------------------------------------> bus -> input -> dataIn
     * validArray -> output -> pin ---------------------------------------------> pin -> input -> pValidArray
     * nReadyArray <- input <- pin <---------------------------------------------<- pin <- output <- readyArray
     */

    public static final String underscore = "_";

    public Node node;
    public int index;

    public int bitSize = 0;
    public boolean bitSizeWasZero = false;

    public String type = "";
    public int port = 0;
    public String infoType = "";

    //connection information
    public Input [] dataInArray; //The dataOutArray of this output bundle is connected to which input dataInArray
    public Input pValidArray = null; //The validArray of this output bundle is connected to which input pValidArray
    public Input readyArray = null; //The nReadyArray (input pin) of this output bundle is connected to which readyArray of which input bundle

    private Output(String name) //This constructor is for initialising the VCC and GND Outputs only
    {
        node = null;
        index = -1;

        bitSize = -1;

        type = "*";
        port = -1;
        infoType = "*";

        if(name.equals("VCC"))
            VCC = this;

        else if(name.equals("GND"))
            GND = this;
    }

    public Output(Node n, int i, String token)
    {
        //This creates the VCC and the GND ports
        if(VCC == null)
            new Output("VCC");

        if(GND == null)
            new Output("GND");

        //Setting the node
        node = n;
        index = i;

        //setting the bitSize and bitSizeWasZero
        int start = token.indexOf(":") + 1;
        int end = token.indexOf("*");

        if(end == -1)
            bitSize = Integer.parseInt(token.substring(start));

        else
            bitSize = Integer.parseInt(token.substring(start, end));

        if(bitSize == 0) //if bit size is 0 then it is forced to 1 (apparently not done)
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

        dataInArray = new Input[bitSize];
        for(int j = 0; j < bitSize; j++)
            dataInArray[j] = null;
    }

    public void connectToInput(Node inputNode, int inputIndex)
    {
        for(int i = 0; i < dataInArray.length; i++)
            dataInArray [i] = inputNode.inputs.get(inputIndex);

        pValidArray = inputNode.inputs.get(inputIndex);
        readyArray = inputNode.inputs.get(inputIndex);
    }

    //This diconnects the output from the input connections.
    //This is useful to disconnect from a nnode which is going to be removed
    public void removeInputConnection()
    {
        for(int i = 0; i < dataInArray.length; i++)
            dataInArray[i] = null;

        pValidArray = null;
        readyArray = null;
    }

    //This removes the output of th enode since it is going to be removed
    public void removeItself()
    {
        Input in = null;
        for(int i = 0; i < dataInArray.length; i++)
        {
            if((dataInArray[i] == in) || (dataInArray[i] == Input.dangling) || (dataInArray[i] == null))
                continue;

            in = dataInArray[i];
            in.removeOutputConnection();
        }

        if((pValidArray != in) && (pValidArray != Input.dangling) && (pValidArray != null))
        {
            in = pValidArray;
            in.removeOutputConnection();
        }

        if((readyArray != in) && (readyArray != Input.dangling) && (readyArray != null))
        {
            in = readyArray;
            in.removeOutputConnection();
        }
    }

    public void createOutputPort()
    {
        System.out.println("Creating required output ports for node: " + node.name + " of index: " + index);
        EDIFCell top = node.design.getNetlist().getTopCell();

        //for the dataOutArray
        int i;
        for(i = 0; i < bitSize; i++)
            if(dataInArray[i] == null)
                break;

        if(i != bitSize)
        {
            if(bitSize > 1)
            {
                top.createPort(node.name + underscore + ModulePorts.DataOut + index + "_Port[" + (bitSize-1) + ":0]", EDIFDirection.OUTPUT, bitSize);
                for(int j = 0; j < bitSize; j++)
                    if(dataInArray[j] == null)
                        node.moduleInst.connect(ModulePorts.DataOut + index, j, null, node.name + underscore + ModulePorts.DataOut + index + "_Port", j);
            }
            else
            {
                top.createPort(node.name + underscore + ModulePorts.DataOut + index + "_Port", EDIFDirection.OUTPUT, 1);
                node.moduleInst.connect(ModulePorts.DataOut + index, -1, null, node.name + underscore + ModulePorts.DataOut + index + "_Port", -1);
            }
        }

        //for the validArray
        if(pValidArray == null)
        {
            top.createPort(node.name + underscore + ModulePorts.ValidOut + index + "_Port", EDIFDirection.OUTPUT, 1);
            node.moduleInst.connect(ModulePorts.ValidOut + index, -1, null, node.name + underscore + ModulePorts.ValidOut + index + "_Port", -1);
        }

        //for the nReadyArray
        if(readyArray == null)
        {
            top.createPort(node.name + underscore + ModulePorts.ReadyIn + index + "_Port", EDIFDirection.INPUT, 1);
            node.moduleInst.connect(ModulePorts.ReadyIn + index, -1, null, node.name + underscore + ModulePorts.ReadyIn + index + "_Port", -1);
        }
    }
}
