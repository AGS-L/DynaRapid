/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/


package ch.agsl.dynarapid.vivado;

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

//Simply a class which runs vivado based on the location of tcl script and the location of the output logs file

import java.io.*;
import java.util.*;

import org.python.antlr.PythonParser.else_clause_return;

public class VivadoRun
{
    //checks the exit code for the process to end execution
    public static boolean isProcessTerminated(Process p)
    {
        try
        {
            p.exitValue();
        }
        catch (Exception e)
        {
            return false;
        }
        return true;
    }

    /*
     * Returns status of the vivado process
     * -1 if there was a error
     * 0 if there was segfault
     * 1 all good
     */
    public static int vivadoRun(String tclLoc, String logsLoc, boolean exportLicense, String errorReference, int errorCode, int num)
    {
        StringUtils.printIntro("Vivado Run: " + tclLoc);
        boolean bash = true;

        if(LocationParser.terminal.equals("bash"))
            bash = true;

        else if(LocationParser.terminal.equals("tcsh"))
            bash = false;

        else
        {
            System.out.println("ERROR: Could not find appropriate terminal. Check terminal field in locations.txt");
            return -1; //error
        }

        try { 

            String sysCommand = "";
            if(bash)
                sysCommand = LocationParser.bin + "run_command_bash ";

            else
                sysCommand = LocationParser.bin + "run_command_tcsh ";

            if(exportLicense)
                sysCommand += LocationParser.exportLicense + " && ";

            sysCommand += LocationParser.sourceVivado + " && ";
            //sysCommand += "vivado -mode batch -source " + tclLoc + " > " + logsLoc;
            sysCommand += "/softs/xilinx/Vivado/2020.2/bin/vivado -mode batch -source " + tclLoc + " > " + logsLoc;

            System.out.println("Running command: " + sysCommand);

            Process p = Runtime.getRuntime().exec(sysCommand);

            //running a countdown of 1 hour to end the vivado process
            boolean flag = false;
            for(int i = 0; (i < 60) && (!flag); i++)
            {
                //1 minute process counts
                long end = System.currentTimeMillis() + (60*1000);
                while(System.currentTimeMillis() < end)
                    if(isProcessTerminated(p))
                    {
                        flag = true;
                        break;
                    }
            }

            if(!flag) //taking a lot of time to execute
            {
                System.out.println(tclLoc + " taking a lot of time to execute. Exiting process!");
                ErrorLogger.addError(errorReference, errorCode + num, tclLoc + " taking a lot of time to execute. Exiting process!");
                return 1; //continue execution even though one pblock took a lot of time
            }

            InputStream errorStream = p.getErrorStream();
            String error = "";
            int c = 0;
            while ((c = errorStream.read()) != -1) {
                error += Character.toString((char)c);
            }

            if(error.length() != 0)
            {
                ErrorLogger.addError(errorReference, errorCode + num, error);
                System.out.println(error);

                if(error.startsWith("segfault") || (error.indexOf("google") != -1)) //Means the process went into segfault
                    return 0; //segfault
            }

            else
                System.out.println("Vivado run complete. Success of dcp formation not guaranteed");

            File file = new File (logsLoc);
            if(!file.exists())
            {
                System.out.println("ERROR: Could not locate the logs file");
                return -1; //error
            }
        }

        catch (Exception e) {
            System.out.println("ERROR: Could not run tcl file");
            System.out.println(e.getMessage());
            return -1; //error
        }

        System.out.println("Logs file generated in " + logsLoc);
        return 1; //all good
    }

    public static boolean vivadoRun(String tclLoc, String logsLoc, boolean exportLicense, String errorReference, int errorCode)
    {
        int status = 0;
        int i;

        for(i = 0; i < 5; i++) //Allows only 5 segfaults. Or else logs error
        {
            status = vivadoRun(tclLoc, logsLoc, exportLicense, errorReference, errorCode, i);
            if(status == -1) //some error
                return false;

            if(status == 1) //all good
                break;

            System.out.println("Vivado ran into segfault. Running " + (i+1) + "th time");
        
        }

        if(status == 0)
        {
            String s = "Could not run " + tclLoc + " becuase it went into segfault " + i + " times.";
            System.out.println(s);
            ErrorLogger.addError(errorReference, errorCode + i, s);
        }

        return true;
        
    }
}
