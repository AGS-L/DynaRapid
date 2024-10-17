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

//This takes in the utilization report of the pblocks of the placed and routed dcps and gets the amount of resources uses versus to what is placed.

import java.io.*;
import java.util.*;

import org.python.core.exceptions;
import org.python.core.util.StringUtil;

public class PblockUtilizationParser {
    //NOTE: Sequential parsing
    //Starting lines of log file where the required information is found.
    public final static String checkStrings[] = {
        "| CLB LUTs", //0
        "|   CLBL", //1
        "|   CLBM", //2
        "|   DSP48E2 only" //3
    };

    //Names of resources in order
    public final static String resourceNames[] = {
        "CLB LUTs",
        "CLEL",
        "CLEM",
        "DSP48E2",
    };

    public int resourcesUsed[]; //Amount of resources used in the pblock. Populated by PblockUtilizationParser(). The order of elements are [CLB LUTs, CLEL, CLEM, DSP48E2]
    public int resourcesAvail[]; //Amount of the resources available in the pblock. Populated by function populateResourcesAvailable(). The order of elements are [CLB LUTs, CLEL, CLEM, DSP48E2]
    public boolean status = true; //If the resources list and variable are safe to use or not

    /*
     * This extracts the dimensions of the pblock from its name
     * This calculates the amount of resources available in the given pblock using the map
     * This populates the resourcesAvailable array in order of [CLB LUTs, CLEL, CLEM, DSP48E2]
     */
    public void populateResourcesAvailable(String utilLoc)
    {
        int [] coordinates = StringUtils.getPblockCoordinatesFromPblockName(utilLoc);
        int startRow = coordinates[0];
        int startCol = coordinates[1];
        int endRow = coordinates[2];
        int endCol = coordinates[3];

        int clel = 0;
        int clem = 0;
        int dsp = 0;

        for(int i = startRow; i <= endRow; i++)
            for(int j = startCol; j <= endCol; j++)
            {
                MapElement mapElement = MapElement.map.get(i).get(j);
                if(MapElement.isCLEL(mapElement.leftElementRootname))
                    clel++;
                if(MapElement.isCLEL(mapElement.rightElementRootname))
                    clel++;
                if(MapElement.isCLEM(mapElement.leftElementRootname))
                    clem++;
                if(MapElement.isCLEM(mapElement.rightElementRootname))
                    clem++;
                if((i == startRow) || (!mapElement.leftElementName.equals(MapElement.map.get(i-1).get(j).leftElementName)))
                    if(mapElement.isDSP(mapElement.leftElementRootname))
                        dsp++;
                if((i == startRow) || (!mapElement.rightElementName.equals(MapElement.map.get(i-1).get(j).rightElementName)))
                    if(mapElement.isDSP(mapElement.rightElementRootname))
                        dsp++;
            }

        resourcesAvail = new int [resourceNames.length];
        resourcesAvail[0] = (clel + clem) * 8;
        resourcesAvail[1] = clel;
        resourcesAvail[2] = clem;
        resourcesAvail[3] = dsp*2;
    }

    //This extracts the resourceUsed value of type pos from the line s. The line s is extracted from the util report
    public boolean getValues(String s, int pos)
    {
        ArrayList<Integer> values = new ArrayList<>();
        Scanner in = new Scanner(s);
        while(in.hasNext())
        {
            try
            {
                int val = Integer.parseInt(in.next());
                values.add(val);
            }
            catch(Exception e)
            {
                continue;
            }
        }
        if(values.size() < 4)
            return false;
        resourcesUsed[pos] = values.get(3);
        return true;
    }

    //This checks if the resources available are more than used
    public boolean sanityCheckResources()
    {
        for(int i = 0; i < resourcesUsed.length; i++)
            if(resourcesUsed[i] > resourcesAvail[i])
            {
                System.out.println("ERROR: " + resourceNames[i] + " has resources used " + resourcesUsed[i] + " while resources available is " + resourcesAvail[i] + ", which is not possible");
                return false;
            }
        return true;
    }

    //This basically prints oy all the values of the resources present and used by the pblock
    public void printValues()
    {
        for(int i = 0; i < checkStrings.length; i++)
            System.out.println(resourceNames[i] + " : " + resourcesUsed[i] + " : " + resourcesAvail[i]);
    }

    /*
     * This parses the utilization report of the given pblock and populates the resourcesUsed array.
     * The array is populated in order of [CLB LUTs, CLEL, CLEM, DSP48E2]
     */
    public PblockUtilizationParser(String utilLoc)
    {
        resourcesUsed = new int[resourceNames.length];
        populateResourcesAvailable(utilLoc);

        File file = new File(utilLoc);
        if(!file.exists())
        {
            System.out.println("ERROR: Could not trace file: " + utilLoc);
            status = false;
        }

        StringUtils.printIntro("Parsing Pblock Utilization: " + utilLoc);
        try
        {
            Scanner in = new Scanner (file);
            int pos = 0;
            while(in.hasNextLine() && (pos < resourcesUsed.length))
            {
                String s = in.nextLine();
                if(s.startsWith(checkStrings[pos]))
                    if(!getValues(s, pos++))
                        throw new IOException("ERROR: Could not extract resource used values from the line: " + s);
            }
            printValues();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not read " + utilLoc + ". Exiting utilization parser. See above logs");
            status = false;
        }

        /*
         * It does sanity checks on the resourcesUSed and resourcesAvail array
         * The used resources must be less than equal to available resources
         * This has been disabled since some utlisation reports mention that used resources are more than the present resources
         */
        // if(!sanityCheckResources())
        // {
        //     System.out.println("ERROR: Amount of resources present is less than amount of resources used. See utilization logs: " + utilLoc);
        //     status = false;
        // }
    }
}
