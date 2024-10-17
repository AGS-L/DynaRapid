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
//Has the main for the database generator

import java.io.*;
import java.util.*;
import java.lang.*;

public class GenerateDatabase {
    
    public static ArrayList<String> getAllModuleNames()
    {
        ArrayList<String> moduleNames = new ArrayList<>();
        File dir = new File(LocationParser.placedRoutedDCPs);
        File [] files = dir.listFiles();

        for(int i = 0; i < files.length; i++)
        {
            File file = files[i];
            
            if(!file.isDirectory())
                continue;

            if(file.listFiles().length == 0)
                continue;

            String fileName = file.getName();

            if(fileName.charAt(fileName.length()-1) == '/')
                fileName = fileName.substring(0, fileName.length()-1);

            moduleNames.add(fileName.substring(fileName.lastIndexOf("/") + 1));
        }

        return moduleNames;
    }

    public static void main(String args[]) throws IOException
    {
        if((StringUtils.findInArray("-f", args) ==  -1) && ((StringUtils.findInArray("-m", args) ==  -1)) && (StringUtils.findInArray("-all", args) == -1))
        {
            System.out.println("<usage>: java GenerateDatabase [-f] [-m] [-all]");
            System.out.println("-f <arg> - Location of the .txt file which has the names of modules whose pblocks are to be generated.");
            System.out.println("-m <arg> - Name of the module whose database is to be generated");
            System.out.println("-all - Generates fresh databases for all the compoenents in the placedRouted DCP folder");
            System.out.println("-onlyBin - Generates only the binary databases of the databases already present This does not regenerate the databases.");
            System.out.println("-start <CLEL/CLEM tile name> - Generates the databases with start tile as the one mentioned. ");
            System.out.println("-end <CLEL/CLEM tile name> - Generates the databases with the end tile as the one mentioned.");
            return;
        }

        if(!Start.start())
            return;

        int fileIndex = StringUtils.findInArray("-f", args);
        int moduleNameIndex = StringUtils.findInArray("-m", args);

        boolean isAll = (StringUtils.findInArray("-all", args) == -1) ? false : true;
        boolean onlyBin = (StringUtils.findInArray("-onlyBin", args) == -1) ? false : true;

        int starti = 0;
        int startj = 0;
        int endi = MapElement.map.size()-1;
        int endj = MapElement.map.get(endi).size()-1;

        if(StringUtils.findInArray("-start", args) != -1)
        {
            String tileName = args[StringUtils.findInArray("-start", args) + 1];
            if(!MapElement.mapMap.containsKey(tileName))
            {
                System.out.println("ERROR: Tile name mentioned for start of locations of the database: " + tileName + " is not present. Please give a valid CLEL/CLEM tile name");
                return;
            }
            String loc = MapElement.mapMap.get(tileName);
            starti = Integer.parseInt(loc.substring(0, loc.indexOf(":")));
            startj = Integer.parseInt(loc.substring(loc.indexOf(":")+1));
        }

        if(StringUtils.findInArray("-end", args) != -1)
        {
            String tileName = args[StringUtils.findInArray("-end", args) + 1];
            if(!MapElement.mapMap.containsKey(tileName))
            {
                System.out.println("ERROR: Tile name mentioned for end of locations of the database: " + tileName + " is not present. Please give a valid CLEL/CLEM tile name");
                return;
            }
            String loc = MapElement.mapMap.get(tileName);
            endi = Integer.parseInt(loc.substring(0, loc.indexOf(":")));
            endj = Integer.parseInt(loc.substring(loc.indexOf(":")+1));
        }

        if((starti > endi) || (startj > endj))
        {
            System.out.println("ERROR: Cannot generate database with start indices greater than end indices");
            return;
        }

        ArrayList<String> moduleNames = new ArrayList<>();

        if(isAll)
            moduleNames = getAllModuleNames();

        else if(moduleNameIndex != -1)
            moduleNames.add(args[moduleNameIndex + 1]);

        else if(fileIndex != -1)
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

        //This will generate all the databases along with the binary databases
        if(!onlyBin)
        {
            StringUtils.printIntro("Generating databses for specified modules");
            for(String s : moduleNames)
                if(!DatabaseGenerator.printDatabase(s, starti, startj, endi, endj))
                    break;
        }
        
        //This will take all the existing databases and generate the binary databases only
        StringUtils.printIntro("Generating binary databses for specified modules");
        for(String s : moduleNames)
        {
            String dataLoc = LocationParser.placedRoutedDCPs + s + ".data";
            File file = new File(dataLoc);

            //This means the database corresponding to this module name has not been generated yet.
            if(!file.exists())
            {
                ErrorLogger.addError("Module ignored", 0, "Could not generate binary database for module " + s + ". Generate its database without using -onlyBin");
                continue;
            }

            DatabaseParser databaseObj = new DatabaseParser();
            Component component = databaseObj.parseDatabase(dataLoc);

            if (component == null)
            {
                System.out.println("ERROR: Could not parse database: " + dataLoc);
                return;
            }
            
            else if(!DatabaseGenerator.generateBinaryDatabase(component))
            {
                System.out.println("ERROR: Could not generate binary database for module: " + s + ". See above logs");
                return;
            }
        }


        System.out.println("Completed generating normal and binary databases.");
        ErrorLogger.printErrorLogs();
    }
}
