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

//This holds the time element for time profiling in the tooflow

import java.io.*;
import java.util.*;
import java.time.*;

public class TimeElement {

    String name;

    Instant startInstant = null;
    Instant stopInstant = null;

    int level; //This denotes the level at which this time element is at to the root process. The root process will have level as 0
    TimeElement parentTimeElement;
    ArrayList<TimeElement> childTimeElements = new ArrayList<>();

    public TimeElement(String name, TimeElement parentTimeElment)
    {
        this.name = name;
        this.parentTimeElement = parentTimeElment;

        if(parentTimeElment == null)
            level = 0;

        else
        {
            level = parentTimeElment.level + 1;
            parentTimeElement.childTimeElements.add(this);
        }
    }

    //This strts the time element at the current clock time
    public boolean start()
    {
        if(startInstant != null)
        {
            System.out.println("ERROR: Already started the time element of name: " + name + " by itself or by some of its child time elemnets");
            return false;
        }

        StringUtils.printIntro("Starting : " + name);

        if((parentTimeElement != null) && (parentTimeElement.startInstant == null))
        {
            startInstant = Instant.now();
            if(!parentTimeElement.start(startInstant))
                return false;
        }

        else
            startInstant = Instant.now();

        return true;
    }

    //This starts the time lemenet at the given time only
    public boolean start(Instant startInst)
    {
        if(startInstant != null)
        {
            System.out.println("ERROR: Already started the time element of name: " + name + " by itself or by some of its child time elemnets");
            return false;
        }

        StringUtils.printIntro("Starting : " + name);
        if((parentTimeElement != null) && (parentTimeElement.startInstant == null))
        {
            startInstant = startInst;
            if(!parentTimeElement.start(startInstant))
                return false;
        }

        else
            startInstant = startInst;

        return true;
    }

    public boolean stop()
    {
        if(stopInstant != null)
        {
            System.out.println("ERROR: Already stopped the time element of name: " + name + " by itself or by some of its parent time elemnets");
            return false;
        }

        StringUtils.printIntro("Stopping : " + name);
        stopInstant = Instant.now();

        if(childTimeElements.size() == 0)
            return true;

        for(TimeElement t : childTimeElements)
            if(t.stopInstant == null)
                if(!t.stop(stopInstant))
                {
                    System.out.println("ERROR: Could stop all the child time elements. See above logs");
                    return false;
                }
                
        return true;
    }

    public boolean stop(Instant stopInst)
    {
        if(stopInstant != null)
        {
            System.out.println("ERROR: Already stopped the time element of name: " + name + " by itself or by some of its parent time elemnets");
            return false;
        }

        StringUtils.printIntro("Stopping : " + name);
        stopInstant = stopInst;

        for(TimeElement t : childTimeElements)
            if(t.stopInstant == null)
                if(!t.stop(stopInstant))
                {
                    System.out.println("ERROR: Could stop all the child time elements. See above logs");
                    return false;
                }
                
        return true;
    }

    public boolean isActive()
    {
        if((startInstant != null) && (stopInstant == null))
            return true;

        return false;
    }

    public void printTimeString(FileWriter fileWriter) throws IOException
    {
        String s = "";

        if(level <= 1)
            s += "\n";
            
        for(int i = 0; i < level; i++)
            s += "\t";

        s += name + " : " +  Duration.between(startInstant, stopInstant).toString() + "\n";
        fileWriter.write(s);

        for(TimeElement t : childTimeElements)
            t.printTimeString(fileWriter);
    }
}
