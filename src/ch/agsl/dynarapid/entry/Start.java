/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.entry;
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
//This calls all the starting functions thatis required for any process entry

public class Start {
    
    //This starts all the processis in the code. This does not nequire the root time element name. 
    //This does not measure the time required for execution
    public static boolean start()
    {

        if(!MapBuilderFPGA.mapBuilderFPGA())
        {
            System.out.println("ERROR: Could not form map of FPGA");
            return false;
        }

        System.out.println("Parsing locations");
        LocationParser.locationParser();

        System.out.println("Re-initializing error logger");
        ErrorLogger.reinitializeErrorMap();

        return true;
    }

    //This requires the name of the root elemement time instant name.
    //This naturally meausres dfferent segments of the time measurement
    public static boolean start(String rootTimeElementName)
    {

        if(!TimeProfiler.addAndStartTimeElement("Preliminary Processes", rootTimeElementName))
            return false;

        if(!TimeProfiler.addAndStartTimeElement("Map Builder", "Preliminary Processes"))
            return false;

        if(!MapBuilderFPGA.mapBuilderFPGA())
        {
            System.out.println("ERROR: Could not form map of FPGA");
            return false;
        }

        if(!TimeProfiler.endTimeElement("Map Builder"))
            return false;

        System.out.println("Parsing locations");
        LocationParser.locationParser();

        System.out.println("Re-initializing error logger");
        ErrorLogger.reinitializeErrorMap();

        if(!TimeProfiler.endTimeElement("Preliminary Processes"))
            return false;

        return true;
    }


}
