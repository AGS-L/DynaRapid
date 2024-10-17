/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.modules;
import  ch.agsl.dynarapid.databasegenerator.*;
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

//This stores all the pblock associated with a perticular componnets.
//This also helps in building the database of all the components

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
import com.xilinx.rapidwright.router.Router;
import com.xilinx.rapidwright.placer.handplacer.HandPlacer;
import com.xilinx.rapidwright.rwroute.RWRoute;

import java.io.*;
import java.util.*;
import java.lang.*;


public class Component implements Serializable
{
    public String dcpName;
    public ResourceElement dcp; //This is the resource element that has resource that are atleast required by the component

    public boolean isVHDL; //if the synthesized component used is generated using the vhdl method then it is true.
                    //If the synthesized componenet used is generated using the generic method then it is false
    
    public String moduleLoc, utilLoc, metaLoc; //this holds the locations of the modules, utilization and the metadata file

    public ModulePorts modulePorts;
    public int incFactor;
    
    //For pblockGenerator
    public ArrayList<Pblock> pblockList = new ArrayList<>();
    public ArrayList<Pblock> rejectPblockList = new ArrayList<>();

    //For block merger
    public ArrayList<Shape> shapeList = new ArrayList<>();

    //Called from the DatabaseParser
    public Component(String dName, int clel, int clem, int dsp, int bram, int inc) //For block merger and placer
    {
        dcpName = dName;

        StringUtils.printIntro("Creating New Component: " + dcpName);

        isVHDL = getIsVHDLValue(dName);

        if(isVHDL)
        {
            moduleLoc = LocationParser.vhdlSynthDCPs + dcpName + "_synth.dcp";
            utilLoc = LocationParser.vhdlSynthDCPs + dcpName + ".util";
            metaLoc = LocationParser.vhdlSynthDCPs + dcpName + "_synth_0_metadata.txt";
        }
        
        else
        {
            moduleLoc = LocationParser.genericSynthDCPs + dcpName + "_synth.dcp";
            utilLoc = LocationParser.genericSynthDCPs + dcpName + ".util";
            metaLoc = LocationParser.genericSynthDCPs + dcpName + "_synth_0_metadata.txt";
        }

        dcp = new ResourceElement(clel, clem, dsp, bram);
        incFactor = inc;

        modulePorts = null; //This is filled while creating modules for shapes
    }

    public Component(String d, boolean license) //For PblockGenerator
    {
        dcpName = d;

        StringUtils.printIntro("Creating New Component: " + dcpName);

        isVHDL = getIsVHDLValue(d);

        if(isVHDL)
        {
            moduleLoc = LocationParser.vhdlSynthDCPs + dcpName + "_synth.dcp";
            utilLoc = LocationParser.vhdlSynthDCPs + dcpName + ".util";
            metaLoc = LocationParser.vhdlSynthDCPs + dcpName + "_synth_0_metadata.txt";
        }
        
        else
        {
            moduleLoc = LocationParser.genericSynthDCPs + dcpName + "_synth.dcp";
            utilLoc = LocationParser.genericSynthDCPs + dcpName + ".util";
            metaLoc = LocationParser.genericSynthDCPs + dcpName + "_synth_0_metadata.txt";
        }

        File utilFile = new File(utilLoc);
        File metaFile = new File(metaLoc);

        if(!utilFile.exists() || !metaFile.exists())
        {
            String tclLoc, logsLoc;

            if(isVHDL)
            {
                tclLoc = LocationParser.vhdlSynthDCPs + dcpName + ".tcl";
                logsLoc = LocationParser.vhdlSynthDCPs + dcpName + ".report";
            }

            else
            {
                tclLoc = LocationParser.genericSynthDCPs + dcpName + ".tcl";
                logsLoc = LocationParser.genericSynthDCPs + dcpName + ".report";
            }
            
            String errorReference = "Component Generation: " + dcpName;
            int errorCode = 0;

            System.out.println("Generating utilization report file and metadata file");
            if(!ComponentTclGenerator.tclGenerator(this) || !VivadoRun.vivadoRun(tclLoc, logsLoc, license, errorReference, errorCode))
                if(!utilFile.exists() || !metaFile.exists())
                    System.out.println("Could not generate utilization and/or metadata file. See error logs");
        }

        UtilizationParser obj = new UtilizationParser(utilLoc);
        dcp = new ResourceElement(obj.clel, obj.clem, obj.dsp, obj.bram);

        System.out.println("Resources required are:");
        System.out.println("CLEL: " + obj.clel);
        System.out.println("CLEM: " + obj.clem);
        System.out.println("DSP: " + obj.dsp);
        System.out.println("BRAM: " + obj.bram);

        if((obj.dsp > 0) || (obj.bram > 0))
            incFactor = 5;
        else
            incFactor = 1;
            
        modulePorts = new ModulePorts(this);
    }

    //This updates the component after being read from the database
    //This has been done so that the binary databases can be read in isolation from the rapidwright classes
    public void updateComponent(Device device)
    {
        for(Shape sh : shapeList)
            sh.updateShape(device);
    }

    public static boolean getIsVHDLValue(String dcpName)
    {
        File vhdlDCP = new File(LocationParser.vhdlSynthDCPs + dcpName + "_synth.dcp");
        File genericDCP = new File(LocationParser.genericSynthDCPs + dcpName + "_synth.dcp");

        if(genericDCP.exists())
        {
            System.out.println("Using the synthesized design from the generic section");
            return false;
        }

        else    
        {
            System.out.println("Using the synthesized design from the vhdl section");
            return true;
        }

    }

    /*
     * This tells if the given resource element has a pblock or not which is already placed and routed
     * @param Propable resource element
     * @return returns the name of the pblock if the pblock exists or else ""
     */
    public String searchPblockList(ResourceElement re)
    {
        String s = "";
        Device device = Device.getDevice("xcvu13p-fsga2577-1-i");

        for(int i = 0; i < pblockList.size(); i++)
        {
            Pblock pblock = pblockList.get(i);

            if((pblock.currRow != re.row) || (pblock.currCol != re.col))
                continue;

            //Supposed location of the anchor tile in the given pblock
            int anchori = re.curri + pblock.reli;
            int anchorj = re.currj + pblock.relj;

            Tile anchorTile; //This has the tile which might be the anchor tile of the pblock
            if(pblock.side == -1) //left side is the anchor site
                anchorTile = device.getTile(MapElement.map.get(anchori).get(anchorj).leftElementName);

            else //right side is the anchor site
                anchorTile = device.getTile(MapElement.map.get(anchori).get(anchorj).rightElementName);

            Site anchorSiteArray[] = anchorTile.getSites();
            for(Site anchorSite : anchorSiteArray)
                if(pblock.validPlaces.contains(anchorSite))
                {
                    s = pblock.pblockName;
                    break;
                }
    
            if(!s.equals(""))
                break;
        }
    
        return s;
    }

    public void printComponent()
    {
        System.out.println("----------------------------");
        System.out.println("DCPName: " + dcpName);
        System.out.println("Increment Factor: " + incFactor);
        System.out.println("Number of Pblocks: " + pblockList.size());
        System.out.println("Number of Shapes: " + shapeList.size());
    }
}
