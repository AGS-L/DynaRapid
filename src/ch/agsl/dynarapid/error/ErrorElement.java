/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/


package ch.agsl.dynarapid.error;
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
//This stores the error element of the errorLogger.

import java.util.*;
import java.io.*;

public class ErrorElement
    {
        public ArrayList<Integer> errorCode = new ArrayList<>();
        public ArrayList<String> errorString = new ArrayList<>();

        public void addEntry(int e, String s)
        {
            errorCode.add(e);
            errorString.add(s);
        }

        public void printEntry()
        {
            for(int i = 0; i < errorCode.size(); i++)
            {
                System.out.print(errorCode.get(i) + " : ");
                System.out.println(errorString.get(i) + "\n");
            }
        }
    }
