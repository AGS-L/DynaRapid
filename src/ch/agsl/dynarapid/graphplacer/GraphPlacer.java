/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/
package ch.agsl.dynarapid.graphplacer;

import ch.agsl.dynarapid.GenerateDesign;
import ch.agsl.dynarapid.debug.TimeProfiler;
import ch.agsl.dynarapid.entry.GenerateRouted;
import ch.agsl.dynarapid.modules.Node;
import ch.agsl.dynarapid.modules.Shape;
import ch.agsl.dynarapid.parser.LocationParser;
import ch.agsl.dynarapid.strings.StringUtils;

import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Module;
import com.xilinx.rapidwright.edif.EDIFCell;
import com.xilinx.rapidwright.edif.EDIFDirection;
import com.xilinx.rapidwright.edif.EDIFNet;
import com.xilinx.rapidwright.edif.EDIFPort;
import com.xilinx.rapidwright.examples.SLRCrosserGenerator;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GraphPlacer {
    
    //This is teh actual placer. Requires the nodes from the placer algorithm
    //The complete param tells if the design is to be routed completely or partially
    public static boolean graphPlacer(Map<String, Node> nodes, String graphName, boolean complete, int threads, boolean debug, boolean noClock)
    {
        //////////////////////////////////////////////////////////////////////////////////////////////////////
        if(!TimeProfiler.addAndStartTimeElement("Module Loading", "Graph Stitching"))
            return false;

        StringUtils.printIntro("Loading modules for graph: " + graphName);

        //create a design
        Design design = new Design(graphName, GenerateDesign.fpga_part);
        EDIFCell top = design.getNetlist().getTopCell();

        HashSet<Shape> shapes = new HashSet<>();
        for (Map.Entry<String,Node> entry : nodes.entrySet()) 
        {
            Node node = entry.getValue();
            if(!shapes.contains(node.shape))
                shapes.add(node.shape);
        }

        Map<Shape, Module> moduleMap = new ConcurrentHashMap<>();
        int customThreadCount = threads; // Change this to your desired number of threads
        ExecutorService executor = Executors.newFixedThreadPool(customThreadCount);

        try 
        {
            for (Shape sh : shapes) 
            {
                executor.submit(() -> 
                {
                    System.out.println("Trying to create module for shape: " + sh.pblockName);

                    Module module = sh.createModule();
                    if (module == null) 
                        System.err.println("ERROR: Could not load module: " + sh.pblockName);
                    else 
                        moduleMap.put(sh, module);
                });
            }
        } 
        finally 
        {
            executor.shutdown();
        }

        try 
        {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } 
        
        catch (InterruptedException e) 
        {
            System.err.println("ERROR: Thread execution interrupted: " + e.getMessage());
            return false;
        }
        System.out.println("Loaded all modules");

        StringUtils.printIntro("Adding modules to shapes");
        for (Map.Entry<Shape, Module> entry : moduleMap.entrySet()) 
            if(!entry.getKey().addModule(design, entry.getValue()))
            {
                System.out.println("ERROR: Could not add module to shape: " + entry.getKey().pblockName);
                return false;

            }

        if(!TimeProfiler.endTimeElement("Module Loading"))
            return false;
        //////////////////////////////////////////////////////////////////////////////////////////////////////
        if(!TimeProfiler.addAndStartTimeElement("Module Placement", "Graph Stitching"))
            return false;

        //create a rst net and clk net
        EDIFNet clkNet = null;
        EDIFNet rstNet = null;

        if(!noClock)
        {
            String bufgInstName = "bufgce_inst";
		    SLRCrosserGenerator.createBUFGCE(design, DesignPorts.CLK, DesignPorts.CLK + "in", DesignPorts.CLK + "out", bufgInstName);
		    SLRCrosserGenerator.placeBUFGCE(design, design.getDevice().getSite("BUFGCE_X0Y8"), bufgInstName);
		    clkNet = top.getNet(DesignPorts.CLK); //clk net
        }
        else
        {
            EDIFPort clkPort = top.createPort(DesignPorts.CLK, EDIFDirection.INPUT, 1);
            clkNet = top.createNet(DesignPorts.CLK);
            clkNet.createPortInst(clkPort);
        }

		EDIFPort rstPort = top.createPort(DesignPorts.RST, EDIFDirection.INPUT, 1); //reset port
        rstNet = top.createNet(DesignPorts.RST);
        rstNet.createPortInst(rstPort);

        //place all the nodes
        StringUtils.printIntro("Placing graph: " + graphName);
        for (Map.Entry<String,Node> entry : nodes.entrySet()) 
        {
            Node node = entry.getValue();
            if(!node.placeNode(design, clkNet, rstNet))
            {
                System.out.println("ERROR: Could not place node: " + node.name + ". Aborting DCP generation. See above logs!");
                return false;
            }
        }

        design.setAutoIOBuffers(false);
        design.getNetlist().resetParentNetMap();

        if(debug)
            design.writeCheckpoint(LocationParser.designs + graphName + "/" + graphName + "_placed.dcp");

        if(!TimeProfiler.endTimeElement("Module Placement"))
            return false;

        //////////////////////////////////////////////////////////////////////////////////////////////////////

        if(!TimeProfiler.addAndStartTimeElement("Stitching Peripherals", "Graph Stitching"))
            return false;

        //Correcting graph for bitwidth mismatch
        StringUtils.printIntro("Checking connections for bitwidth mismatch");
        for (Map.Entry<String,Node> entry : nodes.entrySet()) 
        {
            try
            {
                entry.getValue().updateConnectionsForBitwidthMismatch();
            }

            catch (Exception e)
            {
                e.printStackTrace();
                System.out.println("ERROR: Bitwidth mismatch detected. Exiting design creation");
                return false;
            }
        }

        //stitch all the node peripherals
        StringUtils.printIntro("Started Stitching Graph");
        for (Map.Entry<String,Node> entry : nodes.entrySet()) 
            entry.getValue().stitchPeripherals();
        
        //Adding extras
        design.getNetlist().resetParentNetMap();

        if(!noClock)
            design.addXDCConstraint("create_clock -name " + DesignPorts.CLK + " -period 0.500 [get_nets " + DesignPorts.CLK + "]");

        //the design.writeCheckpoint() function has a bug which changes the design causing probles while routing
        //The temporary workaround solution is to re-read the dcp just written which coonsumes time but since done in debug mode, workaround is appreciable.
        if(debug)
        {
            design.writeCheckpoint(LocationParser.designs + graphName + "/" + graphName + "_stitched.dcp");
            design = Design.readCheckpoint(LocationParser.designs + graphName + "/" + graphName + "_stitched.dcp");
        }

        if(!TimeProfiler.endTimeElement("Stitching Peripherals"))
            return false;
		
        //////////////////////////////////////////////////////////////////////////////////////////////////////

        if(!TimeProfiler.addAndStartTimeElement("Routing Design", "Graph Stitching"))
            return false;
        
        //Starting the routing of the design
        Path dcpFile = GenerateDesign.getDefaultOutputDCPPath(graphName);
        if(!complete)
            return GenerateRouted.routeDesignPartially(design, dcpFile);

        else 
            return GenerateRouted.routeDesignFully(design, dcpFile);
    }
}
