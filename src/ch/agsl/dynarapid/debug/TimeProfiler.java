/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.debug;

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
import ch.agsl.dynarapid.debug.*;

//This helps to profile the toolflow while its execution for the amount of tie=me it takes.

import java.io.File;
import java.io.FileWriter;
import java.time.*;
import java.util.HashMap;

public class TimeProfiler {

    static TimeElement rootTimeElement = null;
    static HashMap<String, TimeElement> timeElements = new HashMap<>();

    //This just addds a new time in the timeElements map. This does not start the time element
    //In case the parent element name is "", then it is considered the root element
    public static boolean addTimeElement(String name, String parentName)
    {
        if(timeElements.containsKey(name))
        {
            System.out.println("ERROR: Could not create time element with name: " + name + " as it already exists");
            return false;
        }

        if(!parentName.equals("") && !timeElements.containsKey(parentName))
        {
            System.out.println("ERROR: Could not find the parent name mentioned: " + parentName);
            return false;
        }

        TimeElement timeElement;

        //Means this is the root element
        if(parentName.equals(""))
        {
            timeElement = new TimeElement(name, null);
            rootTimeElement = timeElement;
        }

        else
        {
            TimeElement parentTimeElement = timeElements.get(parentName);
            if(parentTimeElement.stopInstant != null)
            {
                System.out.println("ERROR: The given parent time element : " + parentName + " has already stopped.");
                return false;
            }
            
            timeElement = new TimeElement(name, parentTimeElement);
        }
        
        timeElements.put(name, timeElement);
        return true;
    }

    //This allows you to start the time element of the given name
    public static boolean startTimeElement(String name)
    {
        if(!timeElements.containsKey(name))
        {
            System.out.println("ERROR: Could not find time element with name: " + name);
            return false;
        }

        TimeElement timeElement = timeElements.get(name);
        if(!timeElement.start())
        {
            System.out.println("ERROR: Could not start the time element : " + name + ". See above logs");
            return false;
        }

        return true;
    }

    public static boolean addAndStartTimeElement(String name, String parentName)
    {
        if(!addTimeElement(name, parentName) || !startTimeElement(name))
        {
            System.out.println("ERROR: Could not add / start thet ime element: " + name + ". See above logs for details");
            return false;
        }

        return true;
    }

    //This allows you to stop the time element of the given time
    public static boolean endTimeElement(String name)
    {
        if(!timeElements.containsKey(name))
        {
            System.out.println("ERROR: Could not find time element with name: " + name);
            return false;
        }

        TimeElement timeElement = timeElements.get(name);

        if(!timeElement.stop())
        {
            System.out.println("ERROR: Could not stop the time element : " + name + ". See above logs");
            return false;
        }

        return true;
    }

    public static void printTimingProfile(String loc)
    {
        File file = new File(loc);
        
        try
        {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write("----------------------------------------------\n");
            fileWriter.write("                Timing Profile                \n");
            fileWriter.write("----------------------------------------------\n");
            rootTimeElement.printTimeString(fileWriter);
            fileWriter.close();
        }
        
        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not generate timing profile");
        }
    }
}
