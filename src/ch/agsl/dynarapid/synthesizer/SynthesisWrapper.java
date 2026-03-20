/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.synthesizer;

import  ch.agsl.dynarapid.*;
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


//This creates a wrapper around the module uniforming all the port names.
//This class is called only with the modules synthesized using the generic method using the synthesisGenerator method

/*
 * Some port name exceptions must be remembered while using rapidwright connect fnction
 * If you have a bus with a bitwidth of 1 named like: busName[0:0], then the connect function goes like: moduleInst.connect("busName[0]", -1, null, portName, -1)
 * 
 */

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
import java.rmi.registry.LocateRegistry;

public class SynthesisWrapper {

    public static final String zeroBus = "[0]";
  
    //This basically creates a port using the appropriate addendum at the end of port names
    //If the bitsize is > 1, then it automatically adds the bus name characteristics [busWidth-1: 0];
    //if bitsize is equal to 1, then a single port is created
    public static void createDesignPort(EDIFCell top, String name, EDIFDirection direction, int bitSize)
    {
        if(bitSize == 1)
            top.createPort(name, direction, 1);

        else
        {
            name += "[" + (bitSize-1) + ":0]";
            top.createPort(name, direction, bitSize);
        }
    }

    //////////////// general ports ////////////////////

    //This creates a design port for clk and connects it to the existing module port named by portName
    //@param portName This is the name of the port in the existing module which needs to be connected to
    //@param index This is the index of the module portname to which this port is to be connected. In case it is not a bus, put value -1
    public static void createClkPort(ModuleInst moduleInst, String portName, int index)
    {
        createDesignPort(moduleInst.getDesign().getTopEDIFCell(), ModulePorts.CLK, EDIFDirection.INPUT, 1);
        moduleInst.connect(portName, index, null, ModulePorts.CLK, -1);
    }

    //This creates a design port for rst and connects it to the existing module port named by portName
    //@param portName This is the name of the port in the existing module which needs to be connected to
    //@param index This is the index of the module portname to which this port is to be connected. In case it is not a bus, put value -1
    public static void createRSTPort(ModuleInst moduleInst, String portName, int index)
    {
        createDesignPort(moduleInst.getDesign().getTopEDIFCell(), ModulePorts.RST, EDIFDirection.INPUT, 1);
        moduleInst.connect(portName, index, null, ModulePorts.RST, -1);
    }

    ///////////////////// input ports ////////////////////////////

    //This creates a design port for DataIn and connects it to the existing module port named by portName
    //@param portIndex This is the index of the port. Like in dataInArray_0, 0 is the portIndex
    //@param bitSize This is the bitsize of the port that needs to be connected
    public static void createDataInPort(ModuleInst moduleInst, int portIndex, String portName, int bitSize)
    {
        if(bitSize == 0)
            return;

        createDesignPort(moduleInst.getDesign().getTopEDIFCell(), ModulePorts.DataIn + Integer.toString(portIndex), EDIFDirection.INPUT, bitSize);

        if(bitSize == 1)
            moduleInst.connect(portName, -1, null, ModulePorts.DataIn + Integer.toString(portIndex), -1);
            
        else
            for(int i = 0; i < bitSize; i++)
                moduleInst.connect(portName, i, null, ModulePorts.DataIn + Integer.toString(portIndex), i);
    }

    //This creates a design port for validIn and connects it to the existing module port named by portName
    //@param portIndex This is the index of the port. Like in dataInArray_0, 0 is the portIndex
    //@param portName This is the name of the port in the existing module which needs to be connected to
    //@param index This is the index of the module portname to which this port is to be connected. In case it is not a bus, put value -1
    public static void createValidInPort(ModuleInst moduleInst, int portIndex, String portName, int index)
    {
        createDesignPort(moduleInst.getDesign().getTopEDIFCell(), ModulePorts.ValidIn + Integer.toString(portIndex), EDIFDirection.INPUT, 1);
        moduleInst.connect(portName, index, null, ModulePorts.ValidIn + Integer.toString(portIndex), -1);
    }

    //This creates a design port for readyIn and connects it to the existing module port named by portName
    //@param portIndex This is the index of the port. Like in dataInArray_0, 0 is the portIndex
    //@param portName This is the name of the port in the existing module which needs to be connected to
    //@param index This is the index of the module portname to which this port is to be connected. In case it is not a bus, put value -1
    public static void createReadyInPort(ModuleInst moduleInst, int portIndex, String portName, int index)
    {
        createDesignPort(moduleInst.getDesign().getTopEDIFCell(), ModulePorts.ReadyIn + Integer.toString(portIndex), EDIFDirection.INPUT, 1);
        moduleInst.connect(portName, index, null, ModulePorts.ReadyIn + Integer.toString(portIndex), -1);
    }

    //////////////////// output ports //////////////////////////////

    //This creates a design port for DataOut and connects it to the existing module port named by portName
    //@param portIndex This is the index of the port. Like in dataInArray_0, 0 is the portIndex
    //@param bitSize This is the bitsize of the port that needs to be connected
    public static void createDataOutPort(ModuleInst moduleInst, int portIndex, String portName, int bitSize)
    {
        if(bitSize == 0)
            return;

        createDesignPort(moduleInst.getDesign().getTopEDIFCell(), ModulePorts.DataOut + Integer.toString(portIndex), EDIFDirection.OUTPUT, bitSize);
        if(bitSize == 1)
            moduleInst.connect(portName, -1, null, ModulePorts.DataOut + Integer.toString(portIndex), -1);

        else
            for(int i = 0; i < bitSize; i++)
                moduleInst.connect(portName, i, null, ModulePorts.DataOut + Integer.toString(portIndex), i);
    }

    //This creates a design port for validOut and connects it to the existing module port named by portName
    //@param portIndex This is the index of the port. Like in dataInArray_0, 0 is the portIndex
    //@param portName This is the name of the port in the existing module which needs to be connected to
    //@param index This is the index of the module portname to which this port is to be connected. In case it is not a bus, put value -1
    public static void createValidOutPort(ModuleInst moduleInst, int portIndex, String portName, int index)
    {
        createDesignPort(moduleInst.getDesign().getTopEDIFCell(), ModulePorts.ValidOut + Integer.toString(portIndex), EDIFDirection.OUTPUT, 1);
        moduleInst.connect(portName, index, null, ModulePorts.ValidOut + Integer.toString(portIndex), -1);
    }

    //This creates a design port for readyOut and connects it to the existing module port named by portName
    //@param portIndex This is the index of the port. Like in dataInArray_0, 0 is the portIndex
    //@param portName This is the name of the port in the existing module which needs to be connected to
    //@param index This is the index of the module portname to which this port is to be connected. In case it is not a bus, put value -1
    public static void createReadyOutPort(ModuleInst moduleInst, int portIndex, String portName, int index)
    {
        createDesignPort(moduleInst.getDesign().getTopEDIFCell(), ModulePorts.ReadyOut + Integer.toString(portIndex), EDIFDirection.OUTPUT, 1);
        moduleInst.connect(portName, index, null, ModulePorts.ReadyOut + Integer.toString(portIndex), -1);
    }

    //This is the specialized wrapper for the branch component
    public static void createWrapperWithCustomDataPorts(ModuleInst moduleInst,  ch.agsl.dynarapid.modules.Node node, ModulePorts modulePorts, HashMap<Integer, String> dataInPortMapping, HashMap<Integer, String> dataOutPortMapping)//TOOD: modify because name conflict with RapidWright module
    {
        String portName = "";
        int index = 0;

        createClkPort(moduleInst, "clk", -1);
        createRSTPort(moduleInst, "rst", -1);

        int dataInIndex = 0;

        for(int i = 0; i < node.inputs.size(); i++)
        {
            index = (node.inputs.size() == 1) ? -1 : i;

            if(dataInPortMapping.containsKey(i))
                portName = dataInPortMapping.get(i);

            else
                portName = "dataInArray[" + Integer.toString(dataInIndex++) + "]";

            if(node.inputs.get(i).bitSize == 1)
                portName += zeroBus;
            
            createDataInPort(moduleInst, i, portName, node.inputs.get(i).bitSize);

            portName = "pValidArray";   
            if(node.inputs.size() == 1)
                portName += zeroBus;
            createValidInPort(moduleInst, i, portName, index);

            portName = "readyArray";
            if(node.inputs.size() == 1)
                portName += zeroBus;
            createReadyOutPort(moduleInst, i, portName, index);
        }

        int dataOutIndex = 0;

        for(int i = 0; i < node.outputs.size(); i++)
        {
            index = (node.outputs.size() == 1) ? -1 : i;

            if(dataOutPortMapping.containsKey(i))
                portName = dataOutPortMapping.get(i);
            
            else
                portName = "dataOutArray[" + Integer.toString(dataOutIndex++) + "]";

            if(node.outputs.get(i).bitSize == 1)
                portName += zeroBus;
            createDataOutPort(moduleInst, i, portName, node.outputs.get(i).bitSize);

            portName = "validArray";
            if(node.outputs.size() == 1)
                portName += zeroBus;
            createValidOutPort(moduleInst, i, portName, index);

            portName = "nReadyArray";
            if(node.outputs.size() == 1)
                portName += zeroBus;
            createReadyInPort(moduleInst, i, portName, index);
        }
    }

    //This is the main wrappe which wraps the module  
    public static boolean wrapModule(String topName, ch.agsl.dynarapid.modules.Node node)//TOOD: modify because name conflict with RapidWright module
    {
        String dcpLoc = LocationParser.preSynthDCPs + node.dcpName + "_preSynth.dcp";
        String metaLoc = LocationParser.preSynthDCPs + node.dcpName + "_preSynth_0_metadata.txt";

        Module module = new Module(Design.readCheckpoint(dcpLoc), metaLoc);
        ModulePorts modulePorts = new ModulePorts(module);
        modulePorts.printAllBuses();

        Design design = new Design(node.dcpName, GenerateDesign.fpga_part);
        ModuleInst moduleInst = design.createModuleInst(topName, module);
        
        // --- Component matching starts here --- //

        HashMap<Integer, String> dataInPortMapping = new HashMap<>();
        HashMap<Integer, String> dataOutPortMapping = new HashMap<>();

        if(node.type.equals("Operator"))
        {
            if(
                node.compOperator.equals("add_op") || 
                node.compOperator.equals("ashr_op") ||
                node.compOperator.equals("getelementptr_op") ||
                node.compOperator.equals("icmp_slt_op") ||
                node.compOperator.equals("icmp_ult_op") ||
                node.compOperator.equals("mul_op") ||
                node.compOperator.equals("ret_op") || 
                node.compOperator.equals("sub_op") ||
                node.compOperator.equals("zext_op") ||
                node.compOperator.equals("shl_op")
            )
            {
                createWrapperWithCustomDataPorts(moduleInst, node, modulePorts, dataInPortMapping, dataOutPortMapping);
            }

            else if(node.compOperator.equals("mc_load_op") || node.compOperator.equals("mc_store_op"))
            {
                dataInPortMapping.put(1, "input_addr");
                dataOutPortMapping.put(1, "output_addr");
                createWrapperWithCustomDataPorts(moduleInst, node, modulePorts, dataInPortMapping, dataOutPortMapping);
            }
        }

        else if(node.type.equals("Branch"))
        {  
            dataInPortMapping.put(1, "condition[0]");
            createWrapperWithCustomDataPorts(moduleInst, node, modulePorts, dataInPortMapping, dataOutPortMapping);
        }

        else if(
            node.type.equals("Entry") ||
            node.type.equals("Fork") ||
            node.type.equals("Merge") ||
            node.type.equals("Sink") ||
            node.type.equals("Source") ||
            node.type.equals("Buffer") ||
            node.type.equals("TEHB") ||
            node.type.equals("transpFIFO") ||
            node.type.equals("nonTranspFIFO")
        )
        {
            createWrapperWithCustomDataPorts(moduleInst, node, modulePorts, dataInPortMapping, dataOutPortMapping);
        }

        else if(node.type.equals("Mux"))
        {
            dataInPortMapping.put(0, "condition[0]");
            createWrapperWithCustomDataPorts(moduleInst, node, modulePorts, dataInPortMapping, dataOutPortMapping);
        }

        else if(node.type.equals("CntrlMerge"))
        {
            dataOutPortMapping.put(1, "condition[0]");
            createWrapperWithCustomDataPorts(moduleInst, node, modulePorts, dataInPortMapping, dataOutPortMapping);
        }

        else
        {
            System.out.println("ERROR: Could not find wrapper specifications for node: " + node.name + ". Add specifications for the same");
            return false;
        }

        // --- Component matching starts here --- //

        System.out.println("Module Wrapper created. Generating final design");

        design.setAutoIOBuffers(false);
        design.getNetlist().resetParentNetMap();
        design.writeCheckpoint(LocationParser.genericSynthDCPs + node.dcpName + "_synth.dcp");
        return true;
    }   
}
