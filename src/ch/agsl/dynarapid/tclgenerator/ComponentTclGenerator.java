/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.tclgenerator;
//this creates the tcl files responsible for creating the metadata file of the synth design and generating the tcl report

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

import java.io.*;

public class ComponentTclGenerator {

    public static boolean tclGenerator(Component component)
    {
        File file; 

        file = new File(component.utilLoc);
        if(file.exists() && !file.delete())
        {
            System.out.println("ERROR: Could not delete file: " + component.utilLoc);
            return false;
        }
        
        file = new File(component.metaLoc);
        if(file.exists() && !file.delete())
        {
            System.out.println("ERROR: Could not delete file: " + component.metaLoc);
            return false;
        }
        
        if(component.isVHDL)
            file = new File (LocationParser.vhdlSynthDCPs + component.dcpName + ".tcl");

        else
            file = new File (LocationParser.genericSynthDCPs + component.dcpName + ".tcl");

        try{
            FileWriter tclFileWriter = new FileWriter(file);

            tclFileWriter.write("set general.maxThreads 16\n");

            //Opening the design
            tclFileWriter.write("open_checkpoint " + component.moduleLoc + "\n");

            //Generating utilization report
            tclFileWriter.write("report_utilization -packthru -file " + component.utilLoc + "\n");

            //Generating the metadata file
            tclFileWriter.write(LocationParser.sourceRW + "\n");

            if(component.isVHDL)
                tclFileWriter.write("generate_metadata " + LocationParser.vhdlSynthDCPs + component.dcpName + "_synth.dcp " + LocationParser.vhdlSynthDCPs + " 0\n");

            else
                tclFileWriter.write("generate_metadata " + LocationParser.genericSynthDCPs + component.dcpName + "_synth.dcp " + LocationParser.genericSynthDCPs + " 0\n");

            tclFileWriter.close();
        }

        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: could not generate tcl script for the component");
            return false;
        }

        return true;
    }
    
}
