/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution 
* For questions please refer to the email below
* Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
* 
*/

//This prints all the lines responsible fro teh component
//Requires dcp name and the names array of the pblocks

/*
 * Format of the component database ---
 * <leave 1 lines above last printed line>
 * 
 * Component Name: <dcpName>
 * Increment Factor: <incFactor>
 * CLEL Required: <clel required>
 * CLEM Required: <clem required>
 * DSP Required: <dsp required>
 * BRAM Required: <bram required>
 * # of Pblocks: <# of pblocks>
 * <list of pblock names 1 in each line>
 * 
 * <leave 1 lines above last printed line>
 */

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

import org.python.antlr.PythonParser.else_clause_return;

public class ComponentDatabase {
    static final String sep = " : ";
    public static final String checkStrings[] = {
        "Component Name", //0
        "Increment Factor", //1
        "CLEL Required", //2
        "CLEM Required", //3
        "DSP Required", //4
        "BRAM Required", //5
        "# of Pblocks", //6
    };

    public static boolean printDatabase(FileWriter dataWriter, String dcpName, ArrayList<String> pblocks) throws Exception
    {
        String vhdlUtilLoc = LocationParser.vhdlSynthDCPs + dcpName + ".util";
        String genericUtilLoc = LocationParser.genericSynthDCPs + dcpName + ".util";

        File vhdlUtilFile = new File(vhdlUtilLoc);
        File genericUtilFile = new File(genericUtilLoc);

        if(!vhdlUtilFile.exists() && !genericUtilFile.exists())
        {
            System.out.println("ERROR: Could not find the utilization report of " + dcpName + ". Maybe pblocks have not been generated yet.");
            return false;
        }

        String utilLoc;
        if(genericUtilFile.exists())
            utilLoc = genericUtilLoc;
        else
            utilLoc = vhdlUtilLoc;

        UtilizationParser obj = new UtilizationParser(utilLoc);
        int incFactor = ((obj.dsp > 0) || (obj.bram > 0)) ? 5 : 1;

        dataWriter.write("\n");
        dataWriter.write(checkStrings[0] + sep + dcpName + "\n");
        dataWriter.write(checkStrings[1] + sep + incFactor + "\n");
        dataWriter.write(checkStrings[2] + sep + obj.clel + "\n");
        dataWriter.write(checkStrings[3] + sep + obj.clem + "\n");
        dataWriter.write(checkStrings[4] + sep + obj.dsp + "\n");
        dataWriter.write(checkStrings[5] + sep + obj.bram + "\n");
        dataWriter.write(checkStrings[6] + sep + pblocks.size() + "\n");

        for(String s : pblocks)
            dataWriter.write(s + "\n");

        return true;
    }
    
    
}
