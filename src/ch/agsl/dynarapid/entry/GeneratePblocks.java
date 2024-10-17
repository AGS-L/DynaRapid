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

//This holds the main of the pblock generator

import java.io.*;
import java.util.*;
import java.lang.*;

public class GeneratePblocks {
    
    public static void main(String args[]) throws IOException
    {
        if((StringUtils.findInArray("-f", args) ==  -1) && ((StringUtils.findInArray("-m", args) ==  -1)))
        {
            System.out.println("<usage>: java GeneratePblocks [-f] [-m] [-q] [-r]");
            System.out.println("-f <arg> - Location of the .txt file which has the names of modules whose pblocks are to be generated.");
            System.out.println("-m <arg> - Name of the module whose pblock is to be generated");
            System.out.println("-q - Quick run of the pblock generator. Runs only with pin-exposed flow");
            System.out.println("-r - Removes all the files which were not successful");
            System.out.println("-num <arg> - Maximum number of shapes required from the pblock generator. If this argument is not present, maximum number of shapes are generated");
            System.out.println("-lic - If we need to add licence, add this argument");
            return;
        }

        if(!Start.start())
            return;

        boolean remove = false;
        boolean quick = false;
        int numOfShapes = Integer.MAX_VALUE;
        boolean lic = false;

        int fileIndex = StringUtils.findInArray("-f", args);
        int moduleNameIndex = StringUtils.findInArray("-m", args);
        int numOfShapesIndex = StringUtils.findInArray("-num", args);

        if(StringUtils.findInArray("-r", args) != -1) remove = true;
        if(StringUtils.findInArray("-q", args) != -1) quick = true;
        if(StringUtils.findInArray("-lic", args) != -1) lic = true;
        if(numOfShapesIndex != -1) numOfShapes = Integer.parseInt(args[numOfShapesIndex + 1]);

        ArrayList<String> moduleNames = new ArrayList<>();

        if(moduleNameIndex != -1)
            moduleNames.add(args[moduleNameIndex + 1]);

        if(fileIndex != -1)
        {
            File file = new File(args[fileIndex + 1]);
            if(!file.exists())
            {
                System.out.println("ERROR: Could not trace file mentioned: " + args[fileIndex + 1]);
                return;
            }

            Scanner in = new Scanner (file);

            while(in.hasNextLine())
                moduleNames.add(in.nextLine());
        }

        System.out.println("Number of pblocks to be generated is: " + moduleNames.size());
    
        PblockGenerator obj;
        for(String s : moduleNames)
        {
            File vhdlDCP = new File(LocationParser.vhdlSynthDCPs + s + "_synth.dcp");
            File genericDCP = new File(LocationParser.genericSynthDCPs + s + "_synth.dcp");

            if(!vhdlDCP.exists() && !genericDCP.exists())
            {
                System.out.println("ERROR: Could not find synthesized DCP for the module: " + s + ". Please run synthesizer with appropriate dot file representation");
                return;
            }
            
            obj = new PblockGenerator(s, remove, quick, numOfShapes, lic);
        }

        ErrorLogger.printErrorLogs();
    }
}
