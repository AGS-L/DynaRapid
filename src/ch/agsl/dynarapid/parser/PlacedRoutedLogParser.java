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
/*
 * This class parses the placed and routed logs file and gives outputs on whether it is placed and routed successfully.
 * The outputs are as follows:
 * 0: Could not open vivado
 * 1: Could not open dcp file
 * 2: Could not place design
 * 3: Could not route design successfully
 * 4: Could not route design with unrouted nets
 * 5: Could not trace the dcp file
 * 6: Could not trace the edif file
 * 7: Placed and routed successfully
 */


import java.io.*;
import java.util.*;

public class PlacedRoutedLogParser {
    static String checkStrings[] = {
        "source",                                                           //Error: 0
        "open_checkpoint:",                                                 //ERROR: 1
        "place_design completed successfully",                              //ERROR: 2                         
        "route_design completed successfully",                              //ERROR: 3
        "       # of nets with routing errors.......... :           0 :",   //ERROR: 4
        "Write XDEF Complete",                                              //ERROR: 5
        "Netlist sorting complete",                                         //ERROR: 6
    };

    public static String getErrorMsg(int errorVal)
    {
        String errorMsg = "";

        switch (errorVal)
        {
            case 0: errorMsg = "Could not open vivado"; break;
            case 1: errorMsg = "Could not open given checkpoint"; break;
            case 2: errorMsg = "Could not place design successfully"; break;
            case 3: errorMsg = "Could not route design successfully"; break;
            case 4: errorMsg = "Could not route design without routing errors"; break;
            case 5: errorMsg = "Could not write placed and routed checkpoint"; break;
            case 6: errorMsg = "Could not write placed and routed edif"; break;
            case 7: errorMsg = "Placed and routed successfully"; break;
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

    public static int logsChecker(String logFileLoc, Set<Integer> omitChecks)
    {
        int i = 0;
        try
        {
            File logFile = new File(logFileLoc);
            Scanner in = new Scanner(logFile);

            while(in.hasNextLine() && (i < checkStrings.length))
                if(in.nextLine().startsWith(checkStrings[i]) || omitChecks.contains(i))
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

    public static void main(String args[])
    {
        String logsFileLoc = args[0];
        int errorVal = logsChecker(logsFileLoc);
        System.out.println(getErrorMsg(errorVal));
    }
}


