/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.map;

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
//This has the class for each map element. 
//This will help in making shapes

import java.util.*;
import java.lang.*;
import java.io.*;

public class MapElement implements Serializable
{
    public static ArrayList<ArrayList<MapElement>> map;
    
    public String switchBoxName, switchBoxRootname;
    public int switchBoxRow, switchBoxCol;
    
    public String leftElementName = "NULL", leftElementRootname = "NULL";
    public int leftRow = -1, leftCol = -1;

    public String rightElementName = "NULL", rightElementRootname = "NULL";
    public int rightRow = -1, rightCol = -1;

    public static String switchBox = "INT";
    public static String switchBoxRemovals[] = {};

    public static String clel = "CLEL";
    public static String clelRemovals[] = {"TERM", "RBRK"};

    public static String clem = "CLEM";
    public static String clemRemovals[] = {"TERM", "RBRK"};

    public static String dsp = "DSP";
    public static String dspRemovals[] = {"TERM", "RBRK"};

    public static String bram = "BRAM";
    public static String bramRemovals[] = {"TERM", "RBRK"};

    public static String iob = "HPIO";
    public static String iobRemovals[] = {};


    public static HashMap<String, String> mapMap = new HashMap<>(); //Order of value is "i:j"
    public static HashSet<Integer> rowsRemoved = new HashSet<>(); //Stores the rows in the device tile array which has been removed in the map
    public static ArrayList<Integer> colAdded = new ArrayList<>(); //Stores the columns in the device tile array which has been included in the map

    //Additions for the IOB adjoing rows. Solving the antennas problem
    public static Set<Integer> leftMapColToIOB, rightMapColToIOB;

    public static void populateMapMap() //Dont put dsp/BRAM tile names for qery in mapMap.
    {
        for(int i = 0; i < map.size(); i++)
            for(int j = 0; j < map.get(0).size(); j++)
            {
                String s = Integer.toString(i) + ":" + Integer.toString(j);
                mapMap.put(map.get(i).get(j).switchBoxName, s);
                mapMap.put(map.get(i).get(j).leftElementName, s);
                mapMap.put(map.get(i).get(j).rightElementName, s);
            }
    }

    //returns the i and j values from he name of tile that is passed in args
    public static String findInMap(String s)
    {
        if(mapMap.containsKey(s))
            return mapMap.get(s);

        return "";
    }

    public static boolean isSwitchBox(String rootname)
    {
        if(!rootname.startsWith(switchBox))
            return false;

        for(int i = 0; i < switchBoxRemovals.length; i++)
            if(rootname.contains(switchBoxRemovals[i]))
                return false;

        return true;
    }

    public static boolean isCLEL (String rootname)
    {
        if(!rootname.startsWith(clel))
            return false;

        for(int i = 0; i < clelRemovals.length; i++)
            if(rootname.contains(clelRemovals[i]))
                return false;

        return true;
    }

    public static boolean isCLEM (String rootname)
    {
        if(!rootname.startsWith(clem))
            return false;

        for(int i = 0; i < clemRemovals.length; i++)
            if(rootname.contains(clemRemovals[i]))
                return false;

        return true;
    }


    public static boolean isCLB (String rootname)
    {
        if(isCLEL(rootname) || isCLEM(rootname))
            return true;

        return false;
    }

    public static boolean isDSP (String rootname)
    {
        if(!rootname.startsWith(dsp))
            return false;

        for(int i = 0; i < dspRemovals.length; i++)
            if(rootname.contains(dspRemovals[i]))
                return false;

        return true;
    }

    public static boolean isBRAM (String rootname)
    {
        if(!rootname.startsWith(bram))
            return false;

        for(int i = 0; i < bramRemovals.length; i++)
            if(rootname.contains(bramRemovals[i]))
                return false;

        return true;
    }

    public static boolean isIOB (String rootname)
    {
        if(!rootname.contains(iob))
            return false;

        for(int i = 0; i < iobRemovals.length; i++)
            if(rootname.contains(iobRemovals[i]))
                return false;
        
        return true;
    }

    public boolean isPlacementSiteLeft()
    {
        if(isCLB(leftElementRootname) || isBRAM(leftElementRootname) || isDSP(leftElementRootname))
            return true;

        return false;
    }

    public boolean isPlacementSiteRight()
    {
        if(isCLB(rightElementRootname) || isBRAM(rightElementRootname) || isDSP(rightElementRootname))
            return true;

        return false;
    }

}
