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
//parses the utilization logs to find the utilization of LUTs, DSPs, Flipflops.

import java.io.*;
import java.util.*;

public class UtilizationParser {
    
    //NOTE: Sequential parsing
    //Starting lines of log file where the required information is found.
    public static int numResources;
    public final static String checkStrings[] = {
        "| CLB LUTs*",
        "|   LUT as Logic",
        "|   LUT as Memory",
        "| CLB Registers",
        "|   Register as Flip Flop",
        "|   Register as Latch",
        "| CARRY8",
        "| F7 Muxes",
        "| F8 Muxes",
        "| F9 Muxes",
        "| Block RAM Tile",
        "|   RAMB36/FIFO*",
        "|   RAMB18",
        "| DSPs",
    };

    //Names of resources in order
    public final static String resourceNames[] = {
        "Total LUTs", //0
        "Logic LUTs", //1
        "Memory LUTs", //2
        "CLB Registers", //3
        "CLB Flipflops", //4
        "CLB Latches", //5
        "Carry Chains", //6
        "F7 Muxes", //7
        "F8 Muxes", //8
        "F9 Muxes", //9
        "BRAM Tiles", //10
        "BRAM36", //11
        "BRAM18", //12
        "DSPs", //13
    };

    //Array of resources used
    public static int resourceUsed[];

    public int clel, clem, bram, dsp;
    public boolean status;

    public UtilizationParser(String utilLoc)
    {
        resourceUsed = new int[resourceNames.length];
        clel = clem = dsp = bram = 0;
        status = true;
        
        if(resourceNames.length != checkStrings.length)
            status = false;

        File file = new File(utilLoc);

        try
        {
            Scanner in = new Scanner(file);

            int i = 0;
            while(in.hasNextLine() && (i < resourceNames.length))
            {
                String s = in.nextLine();
                if(!s.startsWith(checkStrings[i]))
                    continue;

                resourceUsed[i] = extractValue(s);
                i++;
            }

            if(i != resourceNames.length)
                status = false;

            calculateValues();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not parse utilisation report");
            status = false;
        }
        
    }

    //Extarcts the number of used components from the given line
    public int extractValue(String line) throws Exception
    {
        int value = 0;
        Scanner sc = new Scanner(line);
        while(sc.hasNext()){
            try {
                value = Integer.parseInt(sc.next());
                break;
            }

            catch (Exception e){
                continue;
            }
        } 
        return value;  
    }


    public void calculateValues()
    {
        dsp = (resourceUsed[13] / 2) + (resourceUsed[13] % 2) ;
        bram = 0;

        int logic = (resourceUsed[1] / 8) + (resourceUsed[1] % 8 == 0 ? 0 : 1);
        int memory = (resourceUsed[2] / 8) + (resourceUsed[2] % 8 == 0 ? 0 : 1);
        int ff = (resourceUsed[3]  / 16) + (resourceUsed[3] % 16 == 0 ? 0 : 1);

        System.out.println(ff);
        int carry8 = (resourceUsed[6]);

        clel = logic;
        clem = memory;

        int ffPresent = (clel + clem);
        if(ffPresent < ff)
            clel += (ff - ffPresent);

        int carryPresent = (clel + clem);
        if(carryPresent < carry8)
            clel += (carryPresent - carry8);

    }

    public static void main(String args[])
    {
        String loc = "./mul_op_2_1_32_32.util";
        UtilizationParser obj = new UtilizationParser(loc);
        for(int i = 0; i < obj.resourceUsed.length; i++)
            System.out.println(obj.resourceNames[i] + " : " + resourceUsed[i]);

        System.out.println("clel: " + obj.clel + " clem: " + obj.clem + " dsp: " + obj.dsp + " bram: " + obj.bram);
    }

    
}
