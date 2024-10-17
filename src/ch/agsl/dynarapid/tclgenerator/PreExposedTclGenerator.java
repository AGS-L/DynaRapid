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


import java.io.*;

public class PreExposedTclGenerator {
    
    //this returns the tcl script for exposing the pins of the module
    public static boolean tclGenerator(Pblock pblock)
    {
        File file  = new File (LocationParser.preExposedDCPs + pblock.component.dcpName);
        if(!file.exists())
            file.mkdirs();

        System.out.println("Writing pre-exposed tcl script");
        file = new File (LocationParser.preExposedDCPs + pblock.component.dcpName + "/" + pblock.pblockName + ".tcl");

        try{
            FileWriter tclFileWriter = new FileWriter(file);

            tclFileWriter.write("set general.maxThreads 16\n");

            //Opening the design

            tclFileWriter.write("open_checkpoint " + pblock.component.moduleLoc + "\n");
            
            //Creating clock (in some of the cases where clock is not connected to any cell, it causes placement issues)
            // tclFileWriter.write("create_clock -period 2.5 -name clk -waveform {0.000 1.25} [get_ports clk]\n");
            // tclFileWriter.write("set_property HD.CLK_SRC BUFGCTRL_X0Y2 [get_ports clk]\n");

            //Creating pblock_1
            tclFileWriter.write("create_pblock pblock_1\n");
            tclFileWriter.write(StringUtils.getPblockString(pblock.getAllSites(), "pblock_1")); //This gets the sites for pblock_1 for module
            tclFileWriter.write("add_cells_to_pblock pblock_1 -top\n");
            tclFileWriter.write("set_property CONTAIN_ROUTING 1 [get_pblocks pblock_1]\n");

            //place and route design
            tclFileWriter.write("place_design\n");
            tclFileWriter.write("route_design\n");
            tclFileWriter.write("report_route_status\n");

            //Export files
            tclFileWriter.write("write_checkpoint -force " + LocationParser.preExposedDCPs + pblock.component.dcpName + "/" + pblock.pblockName + "_placedRouted.dcp\n");
        
            tclFileWriter.close();
            System.out.println("Completed Writing tcl script for pre-exposed DCPs");
        }

        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not generate the tcl script for pre-exposed pblock");
            return false;
        }

        return true;
    }
}
