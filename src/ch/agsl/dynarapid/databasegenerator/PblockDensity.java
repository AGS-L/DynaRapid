/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.databasegenerator;

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
///This function calculates the density of the pblock after reading the utilization report of the given pblock
//requires the name of the pblock whose density is to be calculated.

public class PblockDensity {
    
    public static double density(String pblockName, String dcpName)
    {
        String utilLoc = LocationParser.placedRoutedDCPs + dcpName + "/" + pblockName + ".util";
        PblockUtilizationParser obj = new PblockUtilizationParser(utilLoc);
        if(!obj.status)
            return -1.0;

        int resourcesUsed[] = obj.resourcesUsed;
        int resourcesAvail[] = obj.resourcesAvail;

        long totalResourceUsed = 0;
        long totalResourceAvail = 0;
        for(int i = 0; i < resourcesUsed.length; i++)
        {
            totalResourceUsed += resourcesUsed[i];
            totalResourceAvail += resourcesAvail[i];
        }

        double density = (double)(totalResourceUsed * 100) / (double)(totalResourceAvail);
        System.out.println("Density of " + pblockName + " is " + density);
        return density;
    }
}
