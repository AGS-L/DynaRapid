/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.tclgenerator;

import  ch.agsl.dynarapid.*;
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
     
import ch.agsl.dynarapid.interrouting.algorithms.*;
import ch.agsl.dynarapid.strings.*;
import ch.agsl.dynarapid.synthesizer.*;
import ch.agsl.dynarapid.tclgenerator.*;
import ch.agsl.dynarapid.vivado.*;
//This generates the tcl scripts for the pblock

import com.google.protobuf.MapEntryLite;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location;
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

import org.python.netty.util.internal.IntegerHolder;

import java.lang.*;

public class PinExposedTclGenerator {
    
    /*
     * This generates the tcl script that is to be used pre-route checkup
     * The tcl script is kept in the direcory of exposedDCPs/moduleName
     */
    public static boolean preRouteTclGenerator (PinExposer obj)
    {
        File file  = new File (LocationParser.exposedDCPs + obj.returnPblock().component.dcpName);
        if(!file.exists())
            file.mkdirs();

        System.out.println("Writing pre-route tcl script");

        file = new File (LocationParser.exposedDCPs + obj.returnPblock().component.dcpName + "/" + obj.returnPblock().pblockName + "_preRoute.tcl");
        try{
            FileWriter tclFileWriter = new FileWriter(file);

            tclFileWriter.write("set general.maxThreads 16\n");

            //Opening the design
            tclFileWriter.write("open_checkpoint " + LocationParser.exposedDCPs + obj.returnPblock().component.dcpName + "/" + obj.returnPblock().pblockName + "_connected.dcp\n");
            
            //Creating clock
            tclFileWriter.write("create_clock -period 2.5 -name clk -waveform {0.000 1.25} [get_ports clk]\n");
            tclFileWriter.write("set_property HD.CLK_SRC BUFGCTRL_X0Y2 [get_ports clk]\n");

            //Fixing the flops and LUTs
            ArrayList <RouteSite> routeSites = obj.returnRouteSites();
            for(RouteSite r : routeSites)
            {
                String flopString = r.returnFlopsString();

                if(!flopString.equals(""))
                {
                    tclFileWriter.write("set_property is_bel_fixed true [get_cells [list " + flopString + "]]\n");
                    tclFileWriter.write("set_property is_loc_fixed true [get_cells [list " + flopString + "]]\n");
                }

                String lutString = r.returnLUTString();

                if(!lutString.equals(""))
                {
                    tclFileWriter.write("set_property is_bel_fixed true [get_cells [list " + lutString + "]]\n");
                    tclFileWriter.write("set_property is_loc_fixed true [get_cells [list " + lutString + "]]\n");
                }
            }

            //Creating pblock_1
            tclFileWriter.write("create_pblock pblock_1\n");
            tclFileWriter.write(StringUtils.getPblockString(obj.returnPblock().getAllSites(), "pblock_1")); //This gets the sites for pblock_1 for module
            tclFileWriter.write("add_cells_to_pblock pblock_1 [get_cells [list " + obj.returnPblock().pblockName + "_cell" + "]] -clear_locs\n"); //Assigning the pblock_1 for the given module
            tclFileWriter.write("set_property CONTAIN_ROUTING 1 [get_pblocks pblock_1]\n");

            //Place design
            tclFileWriter.write("place_design\n");

            //creating the pblock_2
            tclFileWriter.write("create_pblock pblock_2\n");
            tclFileWriter.write(StringUtils.getPblockString(obj.returnPblock().getAllSites(), "pblock_2")); //This gets the sites for pblock_2 for module
            tclFileWriter.write(StringUtils.getPblockString(obj.returnSites(), "pblock_2")); //This gets the sites for pblock_2 for routing
            tclFileWriter.write("set_property CONTAIN_ROUTING 1 [get_pblocks pblock_2]\n");

            //route design
            tclFileWriter.write("route_design\n");
            tclFileWriter.write("report_route_status\n");
            tclFileWriter.write("write_checkpoint -force " + LocationParser.exposedDCPs + obj.returnPblock().component.dcpName + "/" + obj.returnPblock().pblockName + "_placedRouted.dcp\n");
            tclFileWriter.close();
        }

        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not write pre-route tcl script");
            return false;
        }

        System.out.println("Generated pre-route tcl script");
        return true;
    }

    //It gets all the required nets for the module
    public static void getNetsForModule (PinExposer obj, FileWriter tclFileWriter) throws IOException
    {
        Map<String, ArrayList<Integer>> buses = obj.returnPblock().component.modulePorts.buses;
        String moduleName = obj.returnPblock().pblockName + "_cell";

        for (Map.Entry<String, ArrayList<Integer>> entry : buses.entrySet()) 
        {
            String busName = entry.getKey();
            int busWidth = entry.getValue().get(0);
            String netName = busName + "_net";

            if(busName.startsWith(ModulePorts.CLK) || (busName.startsWith(ModulePorts.RST))) //already connected 
                continue;

            if(busWidth == 1) //Means net is not a bus
            {
                tclFileWriter.write("create_net {" + netName + "}\n");
                tclFileWriter.write("connect_net -hierarchical -net {" + netName + "} -objects [list {" + moduleName + "/" + busName + "}]\n");
            }

            else
            {
                tclFileWriter.write("create_net {" + netName + "} -from " + Integer.toString(busWidth -1) + " -to 0\n");
                for(int i = 0; i < busWidth; i++)
                    tclFileWriter.write("connect_net -hierarchical -net {" + netName + "[" + i + "]} -objects [list {" + moduleName + "/" + busName + "[" + i + "]}]\n");
            }
        }
    }

    //This function generates ports for the design
    public static void getPortsForNets (PinExposer obj, FileWriter tclFileWriter) throws IOException
    {
        Map<String, ArrayList<Integer>> buses = obj.returnPblock().component.modulePorts.buses;
        String moduleName = obj.returnPblock().pblockName + "_cell";

        for (Map.Entry<String, ArrayList<Integer>> entry : buses.entrySet()) 
        {
            String busName = entry.getKey();
            int busWidth = entry.getValue().get(0);
            String busDirection = (entry.getValue().get(1) == 0) ? "in" : "out";
            String portName = busName;
            String netName = busName + "_net";

            if(busName.startsWith(ModulePorts.CLK) || (busName.startsWith(ModulePorts.RST))) //already connected 
                continue;

            if(busWidth == 1) //Means net is not a bus
            {
                //Port Creation
                tclFileWriter.write("create_port " + portName + " -direction " + busDirection + "\n");
                tclFileWriter.write("set_property iostandard LVCMOS18 [get_ports [list " + portName + "]]\n");
                tclFileWriter.write("set_property pulltype NONE [get_ports [list " + portName + "]]\n");

                //Port-Net connection
                tclFileWriter.write("connect_net -hierarchical -net {" + netName + "} -objects [list {" + portName + "}]\n");
            }

            else
            {
                //Port creation
                tclFileWriter.write("create_port " + portName + " -direction " + busDirection + " -from " + Integer.toString(busWidth - 1) + " -to 0\n");
                String portString = "";
                for(int i = 0; i < busWidth; i++)
                    portString += "{" + portName + "[" + Integer.toString(i) + "]} ";

                tclFileWriter.write("set_property iostandard LVCMOS18 [get_ports [list " + portString + "]]\n");
                tclFileWriter.write("set_property pulltype NONE [get_ports [list " + portString + "]]\n");

                //Port-Net connections
                for(int i = 0; i < busWidth; i++)
                    tclFileWriter.write("connect_net -hierarchical -net {" + netName + "[" + i + "]} -objects [list {" + portName + "[" + i + "]}]\n");
            }
        }
    }

    public static boolean postRouteTclGenerator (PinExposer obj)
    {
        System.out.println("Writing post-route tcl script");

        File file = new File (LocationParser.exposedDCPs + obj.returnPblock().component.dcpName + "/" + obj.returnPblock().pblockName + "_postRoute.tcl");

        try
        {
            FileWriter tclFileWriter = new FileWriter(file);

            tclFileWriter.write("set general.maxThreads 16\n");

            //Opening the design
            tclFileWriter.write("open_checkpoint " + LocationParser.exposedDCPs + obj.returnPblock().component.dcpName + "/" + obj.returnPblock().pblockName + "_placedRouted.dcp\n");

            //Removing pblock_2
            tclFileWriter.write("delete_pblocks pblock_2\n");
            tclFileWriter.write("set_property CONTAIN_ROUTING 1 [get_pblocks pblock_1]\n");

            //Removing the flops and LUTs
            ArrayList <RouteSite> routeSites = obj.returnRouteSites();
            for(RouteSite r : routeSites)
            {
                String flopString = r.returnFlopsString();

                if(!flopString.equals(""))
                    tclFileWriter.write("remove_cell " + flopString + "\n");

                String lutString = r.returnLUTString();

                if(!lutString.equals(""))
                    tclFileWriter.write("remove_cell " + lutString + "\n");
            }

            //Removing the nets of the design apart from the clk and the reset net
            ArrayList<EDIFNet> designRouteNets = obj.returnDesignRouteNets();
            for(int i = 2; i < designRouteNets.size(); i += 10)
            {
                String netString = "";
                for(int j = i; (j < (i+10)) && (j < designRouteNets.size()); j++)
                    netString += designRouteNets.get(j).getName() + " ";

                tclFileWriter.write("remove_net " + netString + "\n");
            }

            //Removing all the design ports
            ArrayList<EDIFPort> designPorts = obj.returnDesignPorts();
            String portString = "";
            for(int i = 2; i < designPorts.size(); i++)
                portString += StringUtils.cleanPortName(designPorts.get(i).getName()) + " ";

            tclFileWriter.write("remove_port " + portString + "\n");

            //Adding Nets
            getNetsForModule(obj, tclFileWriter);

            //Adding ports for nets
            getPortsForNets(obj, tclFileWriter);

            //repair routing 
            tclFileWriter.write("route_design -preserve\n");
            tclFileWriter.write("report_route_status\n");

            //Export files
            File f = new File (LocationParser.placedRoutedDCPs + obj.returnPblock().component.dcpName + "/");
            if(!f.exists())
                f.mkdirs();
              
            tclFileWriter.write("report_utilization -pblocks pblock_1 -packthru -file " + LocationParser.placedRoutedDCPs + obj.returnPblock().component.dcpName + "/" + obj.returnPblock().pblockName + ".util\n");
            tclFileWriter.write("write_checkpoint -force " + LocationParser.placedRoutedDCPs + obj.returnPblock().component.dcpName + "/" + obj.returnPblock().pblockName + "_placedRouted.dcp\n");
            tclFileWriter.write("write_edif -force " + LocationParser.placedRoutedDCPs + obj.returnPblock().component.dcpName + "/" + obj.returnPblock().pblockName + "_placedRouted.edf\n");
            tclFileWriter.write(LocationParser.sourceRW + "\n");
            tclFileWriter.write("generate_metadata " + LocationParser.placedRoutedDCPs + obj.returnPblock().component.dcpName + "/" + obj.returnPblock().pblockName + "_placedRouted.dcp " + LocationParser.placedRoutedDCPs + obj.returnPblock().component.dcpName + "/ 0\n");


            tclFileWriter.close();
            System.out.println("Generated post-route tcl script");
        }

        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not write post-route tcl script");
            return false;
        }

        System.out.println("Generated post-route tcl script");
        return true;
    }
}
