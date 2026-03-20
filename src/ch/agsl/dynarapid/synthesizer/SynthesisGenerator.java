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

//import ch.agsl.dynarapid.*;
//this takes in a synthesis dot file and then generates the required synthesis designs by invoking vivado
//This has 2 methods of creating synthesized components
//1: Using the -generic metgod. This is still under developement and is best not invoked. (Can be invoked using: synthesizeComponents())\
//2: Using the c++ code of V1. This is fairly stable but not readable. (can be invoked using synthesizeComponentsUsingVHDL())

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

import org.python.antlr.PythonParser.else_clause_return;

import java.lang.*;

public class SynthesisGenerator {

    public static final String equals = "=";

    //These are the parameters of the generic part of the vhd files
    public static final String input = "INPUTS";
    public static final String output = "OUTPUTS";

    public static final String inputBitSize = "DATA_SIZE_IN";
    public static final String outputBitSize = "DATA_SIZE_OUT";

    public static final String inputSize = "INPUT_SIZE";
    public static final String outputSize = "OUTPUT_SIZE";

    public static final String inputCount = "INPUT_COUNT";
    public static final String outputCount = "OUTPUT_COUNT";

    public static final String memInputs = "MEM_INPUTS";
    public static final String memOutputs = "MEM_OUTPUTS";

    public static final String size = "SIZE";
    public static final String constantSize = "CONST_SIZE";
    public static final String addressSize = "ADDRESS_SIZE";
    public static final String dataSize = "DATA_SIZE";
    public static final String conditionSize = "COND_SIZE";
    public static final String bbCount = "BB_COUNT";
    public static final String loadCount = "LOAD_COUNT";
    public static final String storeCount = "STORE_COUNT";
    public static final String fifoDepth = "FIFO_DEPTH";

    //This gives the top name for the node
    public static String getTopNameForNode(Node node)
    {
        String topName = "";

        if(node.type.equals("Operator"))
        {
            if(
                node.compOperator.equals("add_op") || 
                node.compOperator.equals("icmp_ult_op") || 
                node.compOperator.equals("shl_op") || 
                node.compOperator.equals("mul_op") || 
                node.compOperator.equals("sub_op") || 
                node.compOperator.equals("icmp_slt_op") || 
                node.compOperator.equals("ret_op") || 
                node.compOperator.equals("zext_op") || 
                node.compOperator.equals("getelementptr_op") || 
                node.compOperator.equals("ashr_op") ||
                node.compOperator.equals("mc_load_op") ||
                node.compOperator.equals("mc_store_op")
            )
            {
                topName = node.compOperator;
            }
        }
            
        else if(node.type.equals("Branch"))
            topName = "branch";

        else if(node.type.equals("Buffer"))
            topName = "elasticBuffer";

        else if(node.type.equals("Entry"))
            topName = "start_node";

        else if(node.type.equals("Fork"))
            topName = "fork";

        else if(node.type.equals("transpFIFO"))
            topName = "transpFIFO";

        else if(node.type.equals("nonTranspFIFO"))
            topName = "nontranspFifo";

        else if(node.type.equals("Mux"))
            topName = "mux";

        else if(node.type.equals("Merge"))
            topName = "merge";

        else if(node.type.equals("MC"))
            topName = "MemCont";

        else if(node.type.equals("CntrlMerge"))
            topName = "cntrlMerge";

        else if(node.type.equals("Source"))
            topName = "source";

        else if(node.type.equals("Exit"))
            topName = "end_node";

        else if(node.type.equals("Sink"))
            topName = "sink";

        else if(node.type.equals("TEHB"))
            topName = "TEHB";

        return topName;
    }

    //This gives the generic params for the node
    //No need to add generic. This is automatically added by the function below
    public static ArrayList<String> getGenericParamsForNode(Node node)
    {
        ArrayList<String> genericParams = new ArrayList<>();

        if(node.type.equals("Operator"))
        {
            if(
                node.compOperator.equals("add_op") || 
                node.compOperator.equals("icmp_ult_op") || 
                node.compOperator.equals("shl_op") || 
                node.compOperator.equals("mul_op") || 
                node.compOperator.equals("sub_op") || 
                node.compOperator.equals("icmp_slt_op") || 
                node.compOperator.equals("ret_op") || 
                node.compOperator.equals("zext_op") ||
                node.compOperator.equals("ashr_op")
            )
            {
                genericParams.add(input + equals + Integer.toString(node.inputs.size()));
                genericParams.add(output + equals + Integer.toString(node.outputs.size()));
                genericParams.add(inputBitSize + equals + Integer.toString(node.inputs.get(0).bitSize));
                genericParams.add(outputBitSize + equals + Integer.toString(node.outputs.get(0).bitSize));
            }

            else if(node.compOperator.equals("getelementptr_op"))
            {
                genericParams.add(input + equals + Integer.toString(node.inputs.size()));
                genericParams.add(output + equals + Integer.toString(node.outputs.size()));
                genericParams.add(inputSize + equals + Integer.toString(node.inputs.get(0).bitSize));
                genericParams.add(outputSize + equals + Integer.toString(node.outputs.get(0).bitSize));
                genericParams.add(constantSize + equals + Long.toString(node.compValue));
            }

            else if(node.compOperator.equals("mc_load_op") || node.compOperator.equals("mc_store_op"))
            {
                genericParams.add(input + equals + Integer.toString(node.inputs.size()));
                genericParams.add(output + equals + Integer.toString(node.outputs.size()));
                genericParams.add(addressSize + equals + Integer.toString(node.addressSize));
                genericParams.add(dataSize + equals + Integer.toString(node.dataSize));
            }
        }

        else if(node.type.equals("Entry"))
        {
            genericParams.add(inputCount + equals + Integer.toString(node.inputs.size()));
            genericParams.add(outputCount + equals + Integer.toString(node.outputs.size()));
            genericParams.add(inputBitSize + equals + Integer.toString(node.inputs.get(0).bitSize));
            genericParams.add(outputBitSize + equals + Integer.toString(node.outputs.get(0).bitSize));
        }

        else if (node.type.equals("nonTranspFIFO"))
        {
            genericParams.add(inputCount + equals + Integer.toString(node.inputs.size()));
            genericParams.add(outputCount + equals + Integer.toString(node.outputs.size()));
            genericParams.add(inputBitSize + equals + Integer.toString(node.inputs.get(0).bitSize));
            genericParams.add(outputBitSize + equals + Integer.toString(node.outputs.get(0).bitSize));
            genericParams.add(fifoDepth+ equals + Integer.toString(node.slots));
        }

        else if (node.type.equals("transpFIFO"))
        {
            genericParams.add(input + equals + Integer.toString(node.inputs.size()));
            genericParams.add(output + equals + Integer.toString(node.outputs.size()));
            genericParams.add(inputBitSize + equals + Integer.toString(node.inputs.get(0).bitSize));
            genericParams.add(outputBitSize + equals + Integer.toString(node.outputs.get(0).bitSize));
            genericParams.add(fifoDepth+ equals + Integer.toString(node.slots));
        }

        else if(node.type.equals("Source"))
        {
            genericParams.add(inputCount + equals + Integer.toString(0));
            genericParams.add(outputCount + equals + Integer.toString(node.outputs.size()));
            genericParams.add(inputBitSize + equals + Integer.toString(0));
            genericParams.add(outputBitSize + equals + Integer.toString(node.outputs.get(0).bitSize));
        }

        else if(node.type.equals("Sink"))
        {
            genericParams.add(inputCount + equals + Integer.toString(node.inputs.size()));
            genericParams.add(outputCount + equals + Integer.toString(0));
            genericParams.add(inputBitSize + equals + Integer.toString(node.inputs.get(0).bitSize));
            genericParams.add(outputBitSize + equals + Integer.toString(0));
        }

        else if(node.type.equals("Branch") || node.type.equals("Fork"))
        {
            genericParams.add(input + equals + Integer.toString(1));
            genericParams.add(size + equals + Integer.toString(node.outputs.size()));
            genericParams.add(inputBitSize + equals + Integer.toString(node.inputs.get(0).bitSize));
            genericParams.add(outputBitSize + equals + Integer.toString(node.outputs.get(0).bitSize));
        }

        else if(node.type.equals("Mux"))
        {
            genericParams.add(input + equals + Integer.toString(node.inputs.size()));
            genericParams.add(output + equals + Integer.toString(node.outputs.size()));
            genericParams.add(inputBitSize + equals + Integer.toString(node.inputs.get(1).bitSize));
            genericParams.add(outputBitSize + equals + Integer.toString(node.outputs.get(0).bitSize));
            genericParams.add(conditionSize + equals + Integer.toString(node.inputs.get(0).bitSize));
        }

        else if(node.type.equals("CntrlMerge"))
        {
            genericParams.add(input + equals + Integer.toString(node.inputs.size()));
            genericParams.add(output + equals + Integer.toString(node.outputs.size()));
            genericParams.add(inputBitSize + equals + Integer.toString(node.inputs.get(0).bitSize));
            genericParams.add(outputBitSize + equals + Integer.toString(node.outputs.get(0).bitSize));
            genericParams.add(conditionSize + equals + Integer.toString(node.inputs.get(1).bitSize));
        }

        else if(node.type.equals("Merge") || node.type.equals("Buffer") || node.type.equals("TEHB"))
        {
            genericParams.add(input + equals + Integer.toString(node.inputs.size()));
            genericParams.add(output + equals + Integer.toString(node.outputs.size()));
            genericParams.add(inputBitSize + equals + Integer.toString(node.inputs.get(0).bitSize));
            genericParams.add(outputBitSize + equals + Integer.toString(node.outputs.get(0).bitSize));
        }

        else if(node.type.equals("MC"))
        {
            genericParams.add(dataSize + equals + Integer.toString(node.dataSize));
            genericParams.add(addressSize + equals + Integer.toString(node.addressSize));
            genericParams.add(bbCount + equals + Integer.toString(node.bbCount));
            genericParams.add(loadCount + equals + Integer.toString(node.loadCount));
            genericParams.add(storeCount + equals + Integer.toString(node.storeCount));
        }

        else if(node.type.equals("Exit"))
        {
            genericParams.add(input + equals + Integer.toString(node.inputs.size()));
            genericParams.add(output + equals + Integer.toString(node.outputs.size()));
            genericParams.add(memInputs + equals + Integer.toString(DCPNameGenerator.getMemoryInputs(node)));
            genericParams.add(inputBitSize + equals + Integer.toString(node.inputs.get(0).bitSize));
            genericParams.add(outputBitSize + equals + Integer.toString(node.outputs.get(0).bitSize));
        }

        return genericParams;
    }

    //This checks if the pre-synth dcp is present or not. If this is present we can go directly to wrapping the module
    public static boolean checkIfPreSynthDCPPresent(String dcpName)
    {
        File dcp = new File(LocationParser.preSynthDCPs + dcpName + "_preSynth.dcp");
        File meta = new File(LocationParser.preSynthDCPs + dcpName + "_preSynth_0_metadata.txt");

        if(dcp.exists() && meta.exists())
            return true;

        return false;
    }

    //This checks if the synthesized DCP is already present or not in the genericSynth folder
    public static boolean checkIfGenericSynthDCPPresent(String dcpName)
    {
        File file = new File(LocationParser.genericSynthDCPs + dcpName + "_synth.dcp");
        if(file.exists())
            return true;

        return false;
    }

    //This checks if the synthesis dcp is already present or not in the VHDLSynth folder
    public static boolean checkIfVHDLSynthDCPPresent(String dcpName)
    {
        File file = new File(LocationParser.vhdlSynthDCPs + dcpName + "_synth.dcp");
        if(file.exists())
            return true;

        return false;
    }

    public static boolean checkIfVHDLPresent(String dcpName)
    {
        File file = new File(LocationParser.vhdlFiles + dcpName + ".vhd");
        return file.exists();
    }
    
    //This generates the synthesis using the generic method (under developement)
    public static boolean generateSynthesisForNodeUsingGeneric(Node node, boolean unwrap, boolean exportLicense)
    {
        StringUtils.printIntro("Trying synthesizing DCP: " + node.dcpName + " using generic method");

        if(checkIfGenericSynthDCPPresent(node.dcpName))
        {
            System.out.println(node.dcpName + " has already been synthesized using the generic method");
            return true;
        }

        String deviceName = GenerateDesign.fpga_part;
        String topName = getTopNameForNode(node);
        if(topName.equals(""))
        {
            System.out.println("ERROR: Could not get the top name of the dcpName: " + node.dcpName);
            return false;
        }

        //Means we can go directly for wrapping the dcps
        if(checkIfPreSynthDCPPresent(node.dcpName))
        {
            if(!unwrap)
            {
                System.out.println("Pre-synthesis of the compoenent already present. Starting creating wrapper for the design");
                if(!SynthesisWrapper.wrapModule(topName, node))
                {
                    System.out.println("ERROR: Could not wrap node: " + node.name + ". See above logs. Stopping synthesizer");
                    return false;
                }
            }

            else
                System.out.println("Pre-synthesis of the compoenent already present. Exiting component without wrapping it");

            return true;
        }

        String nodeTclLine = "synth_design -mode out_of_context -flatten_hierarchy none -part " + deviceName + " -top " + topName;
        
        ArrayList<String> genericParams = getGenericParamsForNode(node);
        if(genericParams.size() == 0)
        {
            System.out.println("ERROR: Could not get the generic parameters for dcpName: " + node.dcpName);
            return false;
        }
        for(String s : genericParams)
            nodeTclLine += " -generic " + s;

        System.out.println("Generating tcl file for the synthesis of DCP: " + node.dcpName);

        String tclLoc = SynthesisTclGenerator.tclGeneratorUsingGeneric(node.dcpName, nodeTclLine);
        if(tclLoc.equals(""))
        {
            System.out.println("ERROR: Could not generate tcl file for the dcpName: " + node.dcpName + ". See above logs");
            return false;
        }

        String logsLoc = LocationParser.preSynthDCPs + node.dcpName + ".report";
        String errorReference = "Pre-synth: " + node.dcpName;
        int errorCode = 0;

        System.out.println("Running Vivado for the tcl file: " + tclLoc);
        if(!VivadoRun.vivadoRun(tclLoc, logsLoc, exportLicense, errorReference, errorCode))
        {
            System.out.println("ERROR: Could not run vivado successfully for dcp: " + node.dcpName);
            return false;
        }

        File file = new File(logsLoc);
        if(!file.exists())
        {
            System.out.println("ERROR: Could not trace log file.");
            return false;
        }

        int errorVal = SynthesisParser.logsChecker(logsLoc);
        if(errorVal != 4)
        {
            System.out.println("ERROR: Could not generate synthesized dcp due to error: " + SynthesisParser.getErrorMsg(errorVal));
            return false;
        }

        if(!unwrap)
        {
            System.out.println("Generated synthesis of the component. Starting creating wrapper for the design");
            if(!SynthesisWrapper.wrapModule(topName, node))
            {
                System.out.println("ERROR: Could not wrap node: " + node.name + ". See above logs. Stopping synthesizer");
                return false;
            }
            
        }

        else
            System.out.println("Generated synthesis of the component. Exiting component without wrapping it");

        return true;
    }

    //This generates synthesized dcp using the vhdl files
    public static boolean generateSynthesisForNodeUsingVHDL(Node node, boolean exportLicense)
    {
        StringUtils.printIntro("Trying synthesizing DCP: " + node.dcpName + " using VHDL method");

        if(checkIfVHDLSynthDCPPresent(node.dcpName))
        {
            System.out.println(node.dcpName + " has already been genered using the vhdl method");
            return true;
        }

        if(!checkIfVHDLPresent(node.dcpName))
        {
            System.out.println("ERROR: Could not find the source vhdl files to synthesize DCP: " + node.dcpName);
            return false;
        }

        String deviceName = GenerateDesign.fpga_part;
        String nodeTclLine = "synth_design -mode out_of_context -flatten_hierarchy none -part " + deviceName + " -top " + node.dcpName;

        System.out.println("Generating tcl file for the synthesis of DCP: " + node.dcpName);

        String tclLoc = SynthesisTclGenerator.tclGeneratorUsingVHDL(node.dcpName, nodeTclLine);
        if(tclLoc.equals(""))
        {
            System.out.println("ERROR: Could not generate tcl file for the dcpName: " + node.dcpName + ". See above logs");
            return false;
        }

        String logsLoc = LocationParser.vhdlSynthDCPs + node.dcpName + "_synth.report";
        String errorReference = "Synth: " + node.dcpName;
        int errorCode = 0;

        System.out.println("Running Vivado for the tcl file: " + tclLoc);
        if(!VivadoRun.vivadoRun(tclLoc, logsLoc, exportLicense, errorReference, errorCode))
        {
            System.out.println("ERROR: Could not run vivado successfully for dcp: " + node.dcpName);
            return false;
        }

        File file = new File(logsLoc);
        if(!file.exists())
        {
            System.out.println("ERROR: Could not trace log file.");
            return false;
        }

        int errorVal = SynthesisParser.logsChecker(logsLoc);
        if(errorVal != 4)
        {
            System.out.println("ERROR: Could not generate synthesized dcp due to error: " + SynthesisParser.getErrorMsg(errorVal));
            return false;
        }

        System.out.println("Synthesized DCP for: " + node.dcpName);
        return true;
    }

    //This parses the dot file and initiates the componentSynthesizer for each component.
    public static boolean synthesizeComponentsUsingGeneric(String dotLoc, boolean unwrap, boolean exportLicense)
    {
        StringUtils.printIntro("Starting the synthesis process for dotfile: " + dotLoc);
        Map<String, Node> nodes = DotParser.parser(dotLoc);
        if(nodes == null)
            return false;

        for (Map.Entry<String,Node> entry : nodes.entrySet()) 
        {
            Node node = entry.getValue();
            if(!generateSynthesisForNodeUsingGeneric(node, unwrap, exportLicense))
            {
                System.out.println("ERROR: Could not synthesize all components. See above logs");
                return false;
            } 
        }

        System.out.println("Synthesized all the components required");
        return true;
    }

    //This generates the synthesis files using the vhdl files
    public static boolean synthesizeComponentsUsingVHDL(String dotLoc, boolean exportLicense)
    {
        StringUtils.printIntro("Staring synthesizing vhdl files for dot file: " + dotLoc);
        String sysCommand = LocationParser.bin + "main " + dotLoc;

        try
        {
            Process p = Runtime.getRuntime().exec(sysCommand);
            p.waitFor();

            InputStream errorStream = p.getErrorStream();
            String error = "";
            int c = 0;
            while ((c = errorStream.read()) != -1) {
                error += Character.toString((char)c);
            }

            if(error.length() != 0)
            {
                System.out.println("ERROR: Could not generate the VHDL files of all the nodes in the dot file. See error below");
                System.out.println(error);
                return false;
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not generate vhdl file. See above logs");
            return false;
        }
  
        StringUtils.printIntro("Starting the synthesis process for dotfile: " + dotLoc);
        Map<String, Node> nodes = DotParser.parser(dotLoc);
        if(nodes == null)
            return false;

        for (Map.Entry<String,Node> entry : nodes.entrySet()) 
        {
            Node node = entry.getValue();
            if(!generateSynthesisForNodeUsingVHDL(node, exportLicense))
            {
                System.out.println("ERROR: Could not synthesize above component. See above logs");
                return false;
            } 
        }

        System.out.println("Synthesized all the components required");
        return true;
    }

}
