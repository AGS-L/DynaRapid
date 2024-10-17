/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/


package ch.agsl.dynarapid.parser;
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

//This parses the pre-synthesis report and checks for errors

import java.util.*;
import java.io.*;

public class SynthesisParser {

    static String checkStrings[] = {
        "source",                                                           //Error: 0
        "Starting synth_design",                                            //Error: 1
        "Synthesis finished with 0 errors, 0 critical warnings",            //ERROR: 2
        "synth_design completed successfully",                              //ERROR: 3                        
    };

    public static String getErrorMsg(int errorVal)
    {
        String errorMsg = "";

        switch (errorVal)
        {
            case 0: errorMsg = "Could not open vivado"; break;
            case 1: errorMsg = "Could not start synthesizing design"; break;
            case 2: errorMsg = "Errors in design. Check reports"; break;
            case 3: errorMsg = "Could not synthesize design successfully"; break;
            case 4: errorMsg = "Synthesized design successfully"; break;
            default: errorMsg = "ERROR: Some error. Read above logs";
        }

        return errorMsg;
    }

    public static int logsChecker(String logFileLoc)
    {
        int i = 0;
        try
        {
            File logFile = new File(logFileLoc);
            Scanner in = new Scanner(logFile);

            while(in.hasNextLine() && (i < checkStrings.length))
                if(in.nextLine().startsWith(checkStrings[i]))
                    i++;
        }

        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not open the logs file");
            return -1;
        }

        return i;
    }
}
