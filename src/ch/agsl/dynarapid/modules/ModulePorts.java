/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/


package ch.agsl.dynarapid.modules;
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
//This holds the ports of the module in the post-synthesis flow

import com.google.protobuf.MapEntryLite;
import com.trolltech.qt.gui.QAbstractSpinBox.ButtonSymbols;
import com.xilinx.rapidwright.design.*;
import com.xilinx.rapidwright.design.Module;
import com.xilinx.rapidwright.design.ModuleInst;
import com.xilinx.rapidwright.design.SiteInst;
import com.xilinx.rapidwright.device.*;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.device.Tile;
import com.xilinx.rapidwright.device.SiteTypeEnum;
import com.xilinx.rapidwright.device.TileTypeEnum;
import com.xilinx.rapidwright.device.helper.TileColumnPattern;
import com.xilinx.rapidwright.edif.EDIFCell;
import com.xilinx.rapidwright.edif.EDIFCellInst;
import com.xilinx.rapidwright.edif.EDIFDirection;
import com.xilinx.rapidwright.edif.EDIFNet;
import com.xilinx.rapidwright.edif.EDIFNetlist;
import com.xilinx.rapidwright.edif.EDIFPort;
import com.xilinx.rapidwright.edif.EDIFPortInst;
import com.xilinx.rapidwright.tests.CodePerfTracker;
import com.xilinx.rapidwright.examples.SLRCrosserGenerator;
import com.xilinx.rapidwright.design.blocks.PBlock;
import com.xilinx.rapidwright.router.Router;
import com.xilinx.rapidwright.router.SATRouter;
import com.xilinx.rapidwright.placer.handplacer.HandPlacer;
import com.xilinx.rapidwright.rwroute.RWRoute;

import java.io.*;
import java.util.*;
import java.lang.*;

public class ModulePorts implements Serializable
{

    //Input Ports
    public static final String CLK = "clk";
	public static final String RST = "rst";
    public static final String DataIn = "dataInArray_";
    public static final String ReadyIn = "nReadyArray_";
    public static final String ValidIn = "pValidArray_";

    //Output Ports
    public static final String DataOut = "dataOutArray_";
    public static final String ReadyOut = "readyArray_";
    public static final String ValidOut = "validArray_";

    //Ports reserved for only MC module
    //input ports
    public static final String DIn = "din"; //This does not have any underscore. Has a number appended to it in the end like din1[31:0]
    
    //output ports
    public static final String WC = "we0_ce0"; //This port does not have other number appended to it
    public static final String Address = "address"; //This does not have any underscore. Has a number appended to it in the end
    public static final String CE = "ce"; //This does not have any underscore. Has a number appended to it in the end
    public static final String DOut = "dout"; //This does not have any underscore. Has a number appended to it in the end


    public Map<String, ArrayList<Integer>> buses; //TODO: see again, it was a vector before//This vector is of size 2. 1st integer is busSize and 2nd Integer is 0 - input and 1 - output
    public ArrayList<String> portNames;

    public ModulePorts (Component component)
    {
        Module module = new Module(Design.readCheckpoint(component.moduleLoc), component.metaLoc);
        portNames = getAllPortNames(module);
        buses = getBuses(module);
    }

    public ModulePorts(Module module)
    {
        portNames = getAllPortNames(module);
        buses = getBuses(module);
    }
    
    //returns true if it is a bus
    public static boolean isBusFromName(String portName)
    {
        if(portName.startsWith(DataIn) || (portName.startsWith(DataOut)) || (portName.startsWith(DIn)) || (portName.startsWith(Address)) || (portName.startsWith(DOut)))
            return true;

        return false;
    }
    
    //This gets the buses in the module
    //Here for the bus the value is ArrayList<Integer>. 
    //This ArrayList is of size 2. 1st integer is busSize and 2nd Integer is 0 - input and 1 - output
    public static Map<String, ArrayList<Integer>> getBuses(Module module)
    {
        Collection<Port> ports = module.getPorts();
        System.out.println("Number of ports in this module: " + ports.size());
        Map<String, ArrayList<Integer>> busMap = new HashMap<String,ArrayList<Integer>>();

        for(Port p : ports)
        {
            String portName = p.getName();
            int index = portName.lastIndexOf("[");
            String busName;
            
            if(index == -1)
                busName = portName;

            else
                busName = portName.substring(0, index);

            if(busMap.containsKey(busName))
                busMap.get(busName).set(0, busMap.get(busName).get(0) + 1);

            else
            {
                ArrayList<Integer> temp = new ArrayList<>();
                temp.add(1);
                if(p.isOutPort()) //output port
                    temp.add(1);

                else //input port
                    temp.add(0);

                busMap.put(busName, temp);
            }
        }
        return busMap;
    }

    //This function gets all the ports of the module
    //This returns all the ports in singular fashion (no buses are retured)
    //Like for dataInArray bus, all 32 ports are shown here.
    public static ArrayList<String> getAllPortNames(Module module)
    {
        Collection<Port> ports = module.getPorts();
        ArrayList<String> portNames = new ArrayList<>();
        for(Port p : ports)
            portNames.add(p.getName());

        return portNames;
    }

    //This prints all the buses of this object
    public void printAllBuses()
    {
        System.out.println("The buses in this module are: ");
        for (Map.Entry<String, ArrayList<Integer>> entry : buses.entrySet()) 
        {
            String busName = entry.getKey();
            int busWidth = entry.getValue().get(0);
            int direction = entry.getValue().get(1);

            String dir = (direction == 0) ? "input" : "output";
            System.out.println(busName + "\t" + (busWidth) + "\t" + dir);
        }
    }
}
