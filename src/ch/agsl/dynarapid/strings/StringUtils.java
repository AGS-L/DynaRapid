/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.strings;

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

//This has all the functions required for string utilities in dot2RW
//All functions are to be defined as static


import com.google.protobuf.MapEntryLite;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Module;
import com.xilinx.rapidwright.design.ModuleInst;
import com.xilinx.rapidwright.design.SiteInst;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.device.Tile;
import com.xilinx.rapidwright.device.SiteTypeEnum;
import com.xilinx.rapidwright.device.TileTypeEnum;
import com.xilinx.rapidwright.device.helper.TileColumnPattern;
import com.xilinx.rapidwright.edif.EDIFCell;
import com.xilinx.rapidwright.edif.EDIFDirection;
import com.xilinx.rapidwright.edif.EDIFNet;
import com.xilinx.rapidwright.edif.EDIFNetlist;
import com.xilinx.rapidwright.tests.CodePerfTracker;
import com.xilinx.rapidwright.examples.SLRCrosserGenerator;
import com.xilinx.rapidwright.design.blocks.PBlock;
import com.xilinx.rapidwright.router.Router;
import com.xilinx.rapidwright.placer.handplacer.HandPlacer;
import com.xilinx.rapidwright.rwroute.RWRoute;

import java.io.*;
import java.util.*;
import java.lang.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
    
    //gets the name of the dcp from the location of dcp syhtesis
    public static String getDcpName(String loc)
    {
        String dcpName = "NULL";
        if(!loc.substring(loc.length()-4).equals(".dcp"))
        {
            System.out.println("ERROR: Wrong dcp file name");
            return dcpName;
        }

        int i;
        for(i = loc.length()-1; i > 0; i--)
            if(loc.charAt(i) == '/')
                break;

        if(i != 0)
            i++;
        dcpName = loc.substring(i, loc.length()-10);

        System.out.println("Name of dcp is: " + dcpName);
        return dcpName;
    }

    //This returns the tcl pblock string to form the pblock
    //Mantains not more than 5 sites in one line for readability
    public static String getPblockString(ArrayList<Site> sites, String vivadoPblockName)
    {
        if(sites.size() == 0)
        {
            String temp = "";
            return temp;
        }

        int count = 1;
        String pblockString = "resize_pblock " + vivadoPblockName + " -add {";

        for(Site s : sites)
        {
            String name = s.getName();
            if(count % 6 == 0)
            {
                pblockString += "} -locs keep_all\n";
                pblockString += "resize_pblock " + vivadoPblockName + " -add {";
                count = 1;
            }

            pblockString += s + " ";
            count++;
        }

        if(count == 1)
            pblockString = pblockString.substring(0, pblockString.length() - (21 + vivadoPblockName.length()));

        else
            pblockString += "} -locs keep_all\n";

        return pblockString;
    }

    //This removes the [width-1:0] from the port name
    //Apparently while removibg port tcl script for vivado does not allow naming ports with [range-1:0] in the name
    public static String cleanPortName(String portName)
    {
        return portName.substring(0, (portName.indexOf("[")  <= 0) ? portName.length() : portName.indexOf("["));
    }

    public static void printIntro(String s)
    {
        String st = "==\t" + s + "\t==";
        String line = "";
        
        for(int i = 0; i < st.length() + 16; i++)
            line += "=";

        System.out.println(line);
        System.out.println(st);
        System.out.println(line);
    }

    //Finds if the given string is present or not in the array
    public static int findInArray(String s, String [] arr)
    {
        for(int i = 0; i < arr.length; i++)
            if(s.equals(arr[i]))
                return i;

        return -1;
    }

    //Tockenizes the string based on the s as token.
    //Note that the tockens in the array returned does not have tockens
    public static ArrayList<String> stringTokenizer(String param, String tokenizer)
    {
        ArrayList<String> tokens = new ArrayList<>();

        String s = "";
        int i;

        for(i = 0; i < (param.length() + 1 - tokenizer.length()); i++)
        {
            if(param.substring(i, (i + tokenizer.length())).equalsIgnoreCase(tokenizer))
            {
                if(s.length() > 0)
                    tokens.add(s);

                i += (tokenizer.length() - 1);
                s = "";
            }

            else
            {
                s += param.substring(i, i+1);
            }
        }

        if(i < param.length())
            s += param.substring(i, param.length());

        if(s.length() > 0)
            tokens.add(s);

        return tokens;
    }

    public static String getGraphName(String dotLoc)
    {
        int start = dotLoc.lastIndexOf("/") + 1;
        return dotLoc.substring(start, dotLoc.lastIndexOf("."));
    }

    public static String getPlacerNameFromPlaceLoc(String graphName, String placeLoc)
    {
        int start = placeLoc.lastIndexOf("/");
        int end = placeLoc.lastIndexOf(".place");

        if(end == -1)
            return "";

        String name = placeLoc.substring(start+1, end);
        if(!name.startsWith(graphName))
            return "";

        return name.substring(graphName.length() + 1);
    }

    //This returns the coordinates of the pblock from its namr
    //This can accept some file name which has the pblock name in it as well
    //This returns the array as [startRow, startCol, endRow, endCol]
    public static int[] getPblockCoordinatesFromPblockName(String pblockName)
    {
        String pattern = ".*I(\\d+)_J(\\d+)_R(\\d+)_C(\\d+).*";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(pblockName);

        if (matcher.matches()) {
            int I = Integer.parseInt(matcher.group(1));
            int J = Integer.parseInt(matcher.group(2));
            int R = Integer.parseInt(matcher.group(3));
            int C = Integer.parseInt(matcher.group(4));
            int[] coordinates = new int[4];
            coordinates[0] = I;
            coordinates[1] = J;
            coordinates[2] = I + R - 1;
            coordinates[3] = J + C - 1;
            return coordinates;
        }
        return null;
    }
}
