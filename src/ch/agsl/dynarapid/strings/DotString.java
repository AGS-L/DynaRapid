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

//This is responsible for manipulating the strings in the dot file only

import java.util.*;
import java.io.*;

public class DotString {
    
    //Removes spaces in the line
    public static String removeSpace(String s)
    {
        String newStr = "";
        for(int i = 0; i < s.length(); i++)
            if((s.charAt(i) != ' ') && (s.charAt(i) != '\t'))
                newStr += s.substring(i, i+1);

        return newStr;
    }

    //remove any comment lines in the s
    public static String removeComment(String s)
    {
        if(s.indexOf("//") == -1)
            return s;
            
        return s.substring(0, s.indexOf("//"));
    }

    //remove quotes from the string
    public static String removeQuotes(String s)
    {
        int start = -1;
        int end = s.length();

        for(int i = 0; i < s.length(); i++)
            if(s.charAt(i) == '"')
            {
                start = i;
                break;
            }

        for(int i = s.length()-1; i >= 0; i--)
            if(s.charAt(i) == '"')
            {
                end = i;
                break;
            }

        String newStr = s.substring(start+1, end);
        if((newStr.indexOf('"') != -1) || (newStr.indexOf(' ') != -1))
            return "";

        return newStr;
    }

    public static String removeUnderscore(String s)
    {
        String st = "";
        for(int i = 0; i < s.length(); i++)
            if(s.charAt(i) != '_')
                st += s.substring(i, i+1);


        return st;
    }

    public static String removeUnderscoreFromStart(String s)
    {
        for(int i = 0; i < s.length(); i++)
            if(s.charAt(i) != ' ') 
                return s.substring(i);

        return "";
    }

    //This gives the number in the string which is the first occuring number sequence
    public static int getNumber(String param)
    {
        param += " ";
        for(int i = 0; i < param.length(); i++)
        {
            if(!Character.isDigit(param.charAt(i)))
                continue;

            String s = "";
            for(int j = i; j < param.length(); j++)
            {
                if(Character.isDigit(param.charAt(j)))
                    s += param.substring(j, j+1);

                else
                    return Integer.parseInt(s);
            }
        }

        return -1;
    }

    public static String getLeadingSpaces(String line)
    {
        String s = "";
        for(int i = 0; i < line.length(); i++)
        {
            if((line.charAt(i) == ' ') || (line.charAt(i) == '\t'))
                s += line.substring(i, i+1);

            else    
                break;
        }

        return s;
    }
}
