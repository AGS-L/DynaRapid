/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/


package ch.agsl.dynarapid.tclgenerator;
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

//This generates the tcl scrit required for the synthesis and the generation of the utilities 

import java.io.*;

public class SynthesisTclGenerator {

    //This generates the tcl script for pre-synthesis of the compoenent and then returns the tcl location of teh file
    public static String tclGeneratorUsingGeneric(String dcpName, String nodeTclLine)
    {
        String tclLoc = LocationParser.preSynthDCPs + dcpName + "_preSynth.tcl";
        File file = new File(tclLoc);

        try
        {
            FileWriter tclFileWriter = new FileWriter(file);
            
            //Setting the maaximum number of threads required for Vivado
            tclFileWriter.write("set general.maxThreads 16\n");

            //Sourcing the basic vhdl files

            String srcVHDLFilesLoc = LocationParser.srcVHDLFiles;

            tclFileWriter.write("read_vhdl -vhdl2008 " + srcVHDLFilesLoc + "arithmetic_units.vhd\n");
            tclFileWriter.write("read_vhdl -vhdl2008 " + srcVHDLFilesLoc + "elastic_components.vhd\n");
            tclFileWriter.write("read_vhdl -vhdl2008 " + srcVHDLFilesLoc + "MemCont.vhd\n");
            tclFileWriter.write("read_vhdl -vhdl2008 " + srcVHDLFilesLoc + "delay_buffer.vhd\n");
            tclFileWriter.write("read_vhdl -vhdl2008 " + srcVHDLFilesLoc + "mul_wrapper.vhd\n");
            tclFileWriter.write("read_vhdl -vhdl2008 " + srcVHDLFilesLoc + "multipliers.vhd\n");

            //Getting the syntesis line
            tclFileWriter.write(nodeTclLine + "\n");

            //generating the dcp
            tclFileWriter.write("write_checkpoint -force " + LocationParser.preSynthDCPs + dcpName + "_preSynth.dcp\n");
            tclFileWriter.write("write_edif -force " + LocationParser.preSynthDCPs + dcpName + "_preSynth.edf\n");

            //generating the metadata txt file
            tclFileWriter.write(LocationParser.sourceRW + "\n");
            tclFileWriter.write("generate_metadata " + LocationParser.preSynthDCPs + dcpName + "_preSynth.dcp " + LocationParser.preSynthDCPs + dcpName + " 0\n");
            tclFileWriter.close();
        }

        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not generate the pre-synthesis tcl file for the dcp: " + dcpName);
            tclLoc = "";
        }
        return tclLoc;
    }
    
    public static String tclGeneratorUsingVHDL(String dcpName, String nodeTclLine)
    {
        String tclLoc = LocationParser.vhdlSynthDCPs + dcpName + "_synth.tcl";
        File file = new File(tclLoc);

        try
        {
            FileWriter tclFileWriter = new FileWriter(file);
            
            //Setting the maaximum number of threads required for Vivado
            tclFileWriter.write("set general.maxThreads 16\n");

            //Sourcing the basic vhdl files

            String srcVHDLFilesLoc = LocationParser.srcVHDLFiles;
            String vhdlFiles = LocationParser.vhdlFiles;

            tclFileWriter.write("read_vhdl -vhdl2008 " + srcVHDLFilesLoc + "arithmetic_units.vhd\n");
            tclFileWriter.write("read_vhdl -vhdl2008 " + srcVHDLFilesLoc + "elastic_components.vhd\n");
            tclFileWriter.write("read_vhdl -vhdl2008 " + srcVHDLFilesLoc + "MemCont.vhd\n");
            tclFileWriter.write("read_vhdl -vhdl2008 " + srcVHDLFilesLoc + "delay_buffer.vhd\n");
            tclFileWriter.write("read_vhdl -vhdl2008 " + srcVHDLFilesLoc + "mul_wrapper.vhd\n");
            tclFileWriter.write("read_vhdl -vhdl2008 " + srcVHDLFilesLoc + "multipliers.vhd\n");
            tclFileWriter.write("read_vhdl -vhdl2008 " + vhdlFiles + dcpName + ".vhd\n");

            //Getting the syntesis line
            tclFileWriter.write(nodeTclLine + "\n");

            //generating the dcp
            tclFileWriter.write("write_checkpoint -force " + LocationParser.vhdlSynthDCPs + dcpName + "_synth.dcp\n");
            tclFileWriter.write("write_edif -force " + LocationParser.vhdlSynthDCPs + dcpName + "_synth.edf\n");

            tclFileWriter.close();
        }

        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not generate the synthesis tcl file for the dcp: " + dcpName);
            tclLoc = "";
        }
        return tclLoc;
    }
}
