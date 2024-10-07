/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/


//This holds the node of each node in the dot file
//have the initialising values as necessary later

import com.google.protobuf.MapEntryLite;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Module;
import com.xilinx.rapidwright.design.ModuleInst;
import com.xilinx.rapidwright.design.SiteInst;
import com.xilinx.rapidwright.design.SitePinInst;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.device.Device;
import com.xilinx.rapidwright.device.BEL;
import com.xilinx.rapidwright.device.BELPin;
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
import com.xilinx.rapidwright.tests.CodePerfTracker;
import com.xilinx.rapidwright.examples.SLRCrosserGenerator;
import com.xilinx.rapidwright.design.blocks.PBlock;
import com.xilinx.rapidwright.router.Router;
import com.xilinx.rapidwright.placer.handplacer.HandPlacer;
import com.xilinx.rapidwright.rwroute.RWRoute;

import java.io.*;
import java.util.*;

public class Node implements Serializable
{

    public static final boolean genericComonent = false;
    public static final boolean constantComponent = true;
    public static final String underscore = "_";

    String actualName;
    String name;
    String type;
    String actualString = "";

    //Donot know use of these
    String params;
    int nodeID;

    //filled by constructor
    boolean compType; //Can be either genericComponent or constantComponent
    String compOperator;
    long compValue;
    boolean compControl;
    
    int slots;
    boolean transparent;
    String mem;
    int bbCount = -1;
    int loadCount = -1;
    int storeCount = -1;
    int dataSize = 32;
    int addressSize = 32;
    boolean memAddress;
    int bbID = -1;
    int portID = -1;
    int offset = -1;

    ArrayList<Input> inputs = new ArrayList<>();
    ArrayList<Output> outputs = new ArrayList<>();

    String dcpName;
    Component component;

    //For the graph-placer
    Design design;
    ModuleInst moduleInst;
    EDIFCellInst moduleEDIFCellInst;

    //For Placement algorithm
    Shape shape;
    String topLeftAnchorLoc;
    Site rwAnchorSite;
    Tile rwAnchorTile;
    String rwAnchorSiteName;
    String rwAnchorTileName;

    //padding information
    int padLeft = 0;
    int padRight = 0;
    int padTop = 0;
    int padBottom = 0;

    //has the VCC / GND input information
    boolean isUnconnected = false;

    public Node(String actualNodeName, String nodeName, Map<String, String> params, String actualStr)
    {
        actualName = actualNodeName;
        name = nodeName;
        actualString = actualStr;

        if(params.containsKey("constants"))
            compValue = Long.parseLong(params.get("constants"));

        if(params.containsKey("type"))
        {
            compType = genericComonent;
            type = compOperator = params.get("type");
        }

        if(params.containsKey("in"))
            getInputsFromParamLine(params.get("in"));

        if(params.containsKey("out"))
            getOutputsFromParamLine(params.get("out"));

        if(params.containsKey("op"))
            compOperator = params.get("op");

        if(params.containsKey("value"))
            compValue = Long.parseLong(params.get("value").substring(2), 16); //The hexa-decimal value must not start with 0x as it is in the dot file

        if(params.containsKey("control"))
            compControl = params.get("control").contains("true");

        if(params.containsKey("slots"))
            slots = Integer.parseInt(params.get("slots"));
    
        if(params.containsKey("transparent"))
            transparent = params.get("transparent").contains("true");

        if(params.containsKey("memory"))
            mem = params.get("memory");

        if(params.containsKey("bbcount"))
        {
            bbCount = Integer.parseInt(params.get("bbcount"));
            if(bbCount == 0)
            {
                bbCount++;
                String token = Integer.toString(inputs.size()) + ":32*c";
                //inputs.add(new Input(this, inputs.size(), token));
            }
        }

        if(params.containsKey("ldcount"))
        {
            loadCount = Integer.parseInt(params.get("ldcount"));
            if(loadCount == 0)
            {
                loadCount++;
                String token = Integer.toString(inputs.size()) + ":0*l0a";
                //inputs.add(new Input(this, inputs.size(), token));

                token = Integer.toString(outputs.size()) + ":32*l0a";
                //outputs.add(new Output(this, outputs.size(), token));
            }

        }

        if(params.containsKey("stcount"))
        {
            storeCount = Integer.parseInt(params.get("stcount"));
            if(storeCount == 0)
            {
                storeCount++;
                String token = Integer.toString(inputs.size()) + ":0*s0a";
                //inputs.add(new Input(this, inputs.size(), token));

                token = Integer.toString(outputs.size()) + ":0*s0d";
                //outputs.add(new Output(this, outputs.size(), token));
            }
        }

        if(params.containsKey("mem_address"))
            memAddress = params.get("mem_address").contains("true");

        if(params.containsKey("bbId"))
            bbID = Integer.parseInt(params.get("bbId"));

        if(params.containsKey("portId"))
            portID = Integer.parseInt(params.get("portId"));

        if(params.containsKey("offset"))
            offset = Integer.parseInt(params.get("offset"));

        //offsetting the names of the nodes of the buffer using the number of slots and transparentcy
        //NOTE: Only buffer have the slots feature
        if(type.equals("Buffer"))
            bufferTypeOffset();

        //getting the dcp name of the node
        dcpName = DCPNameGenerator.getDCPName(this);
    }

    //This changes the name of the componennt if it is a buffer based on the number of slots and transperency
    public void bufferTypeOffset()
    {
        if(transparent)
        {
            if(slots >= 2) //means FIFO block
                type = compOperator = "transpFIFO";
            
            else //Means it is a TEHB
                type = compOperator = "TEHB";
        }

        else 
        {
            if(slots > 2)
                type = compOperator = "nonTranspFIFO";

            else
                 type = compOperator = "Buffer";
        }
    }

    //Sets the inputs array based on the inputs given in the value
    public void getInputsFromParamLine(String value)
    {
        ArrayList<String> tokens = StringUtils.stringTokenizer(value, "in");
        int index = 0;

        for(String s : tokens)
        {
            Input in = new Input(this, index, s);
            inputs.add(in);
            if(in.infoType.equalsIgnoreCase("a"))
                addressSize = in.bitSize;

            if(in.infoType.equalsIgnoreCase("d"))
                dataSize = in.bitSize;

            index++;
        }
    }

    //Sets the output array based on the outputs given in value
    public void getOutputsFromParamLine(String value)
    {
        ArrayList<String> tokens = StringUtils.stringTokenizer(value, "out");
        int index = 0;

        for(String s : tokens)
        {
            outputs.add(new Output(this, index, s));
            index++;
        }
    }

    //This function connects the input pin of the node with given output pin
    public void connectInput(int inputIndex, Node outputNode, int outputIndex)
    {
        inputs.get(inputIndex).connectToOutput(outputNode, outputIndex);
    }

    //This function connects the output pin of the node with the given input pin
    public void connectOutput(int outputIndex, Node inputNode, int inputIndex)
    {
        outputs.get(outputIndex).connectToInput(inputNode, inputIndex);
    }

    //This is called from the graph gneerator
    //This removes the nodes whose databases have not been found.
    //This also removes all the input traces and output traces.
    //Better to call before bitwidth mismatch
    public boolean removeItself()
    {
        System.out.println("Removing node: " + name);

        System.out.println("Removing all the inputs of the node");
        for(Input i : inputs)
            i.removeItself();

        System.out.println("Removing all the outputs of the node");
        for(Output o : outputs)
            o.removeItself();

        return true;
    }

    //this adds the component and related fields 
    //called from the GraphGenerator after all the required component objects has been made
    public void updateComponent(Component comp)
    {
        component = comp;
    }

    //This function helps to update the padding information
    //In case some side padding is not to be updated, set that value as -1
    public void updatePadding(int left, int right, int top, int bottom)
    {
        if(left != -1)
            padLeft = left;

        if(right != -1)
            padRight = right;

        if(top != -1)
            padTop = top;

        if(bottom != -1)
            padBottom = bottom;
    }

    //This just tells if the node can be placed given the shape and the site
    //This does not actually place the node
    public boolean canPlace(Node[][] fabric, Shape shape, String topLeftAnchorLoc)
    {
        if(!shape.validTopLeftAnchorLocations.contains(topLeftAnchorLoc))
            return false;

        String rowString = topLeftAnchorLoc.substring(0, topLeftAnchorLoc.indexOf("_"));
        String colString = topLeftAnchorLoc.substring(topLeftAnchorLoc.indexOf("_") + 1);

        int starti = Integer.parseInt(rowString.substring(1));
        int startj = Integer.parseInt(colString.substring(1));

        int rows = shape.currRow + padBottom;
        int cols = shape.currCol + padRight;

        for(int i = -padTop; i < rows; i++)
            for(int j = -padLeft; j < cols; j++)
            {
                try
                {
                    if(fabric[starti + i][startj + j] != null)
                        return false;
                }

                catch (Exception e)
                {
                    continue;
                }
            }

        return true;
    }

    /*
     * This just tells if the node of the given shape can be placed in fabric or not with the specified top-left anchor location in the params. 
     * The does not tell if the shape can be actually placed or not. This just sees if all the locations required to place the shape is empty or not
     * The difference between this function and the above function is that here you can specify the limits or bounds on the fabric.
     * This function ensures that the shape in consideration is placed such that all logic is placed within the bounded region of the fabric
     * The padding of the shape can be present outside defined region of the fabric
     * @params fabric This holds the entire map of the fpga. Each location has the node that will be placed there. If no node is yet placed in some location, then it is set as null
     * @param topRow This holds the top-most row boundary of the fabric. In the above function, this topRow is set as 0
     * @param bottomRow This holds the bottom-most row of the fabric . In the above function, this bottomRow is set as fabric.length-1
     * @param leftCol This holds the leftmost column of the fabric. In the above condition, this leftCol is set as 0
     * @param rightCol This holds the right-most column of the fabric. Tn the above condition, this rightCol is set as fabric[0].length-1
     */
    public boolean canPlace(Node[][] fabric, int topRow, int bottomRow, int leftCol, int rightCol, Shape shape, String topLeftAnchorLoc)
    {
        if(!shape.validTopLeftAnchorLocations.contains(topLeftAnchorLoc))
            return false;

        String rowString = topLeftAnchorLoc.substring(0, topLeftAnchorLoc.indexOf("_"));
        String colString = topLeftAnchorLoc.substring(topLeftAnchorLoc.indexOf("_") + 1);
        int starti = Integer.parseInt(rowString.substring(1));
        int startj = Integer.parseInt(colString.substring(1));

        if((starti < topRow) || ((starti + shape.currRow) > bottomRow) || (startj < leftCol) || ((startj + shape.currCol) > rightCol))
            return false;

        int rows = shape.currRow + padBottom;
        int cols = shape.currCol + padRight;
        for(int i = -padTop; i < rows; i++)
            for(int j = -padLeft; j < cols; j++)
            {
                try
                {
                    if(fabric[starti + i][startj + j] != null)
                        return false;
                }

                catch (Exception e)
                {
                    continue;
                }
            }
        return true;
    }

    //this updates the fabric of the design with the nodes.
    //This works only if the palcement data of the node is updated.
    //to update the placement information, look the function below.
    public void updateFabric(Node[][] fabric)
    {
        String rowString = topLeftAnchorLoc.substring(0, topLeftAnchorLoc.indexOf("_"));
        String colString = topLeftAnchorLoc.substring(topLeftAnchorLoc.indexOf("_") + 1);

        int starti = Integer.parseInt(rowString.substring(1));
        int startj = Integer.parseInt(colString.substring(1));

        int rows = shape.currRow + padBottom;
        int cols = shape.currCol + padRight;

        for(int i = -padTop; i < rows; i++)
            for(int j = -padLeft; j < cols; j++)
            {
                try
                {
                    fabric[starti + i][startj + j] = this;
                }

                catch (Exception e)
                {
                    continue;
                }
            }
    }

    //This helps update the placement data called from the placement algorithm
    //This doesnot update the fabric however, this needs to be done separately
    public void updatePlacementData(Shape sh, String topLeftAnchorLoc)
    {
        shape = sh;
        this.topLeftAnchorLoc = topLeftAnchorLoc;

        rwAnchorSite = sh.validSiteMap.get(topLeftAnchorLoc);
        rwAnchorTile = rwAnchorSite.getTile();
        rwAnchorSiteName = rwAnchorSite.getName();
        rwAnchorTileName = rwAnchorTile.getName();
    }

    //This helps update the placement data called from the placement algorithm and also updates the fabric
    public boolean updatePlacementData(Node[][] fabric, Shape sh, String topLeftAnchorLoc)
    {
        if(!sh.validTopLeftAnchorLocations.contains(topLeftAnchorLoc))
        {
            System.out.println("ERROR: Could not update placement data because top-left coordinate " + topLeftAnchorLoc + " is not a valid placement location for " + sh.pblockName);
            return false;
        }

        updatePlacementData(sh, topLeftAnchorLoc);
        updateFabric(fabric);
        return true;
    }

    public boolean updatePlacementData(String topLeftAnchorLoc, int leftPad, int rightPad, int topPad, int bottomPad, Shape shape)
    {
        if(!shape.validTopLeftAnchorLocations.contains(topLeftAnchorLoc))
        {
            System.out.println(" ERROR: " + topLeftAnchorLoc + " is not a valid top-left location of the shape " + shape.pblockName);
            return false;
        }

        updatePlacementData(shape, topLeftAnchorLoc);
        updatePadding(leftPad, rightPad, topPad, bottomPad);
        return true;
    }

    //This returns true if the placement data of the node has been updated or not
    public boolean hasPlacementData()
    {
        if((shape == null) || (topLeftAnchorLoc.equals("")))
            return false;

        return true;
    }

    //This returns the database form of the location (ie. R#_C#) of the center of the node whose placement data has been updated.
    //If for this node the placement data has not been updated, it returns ""
    public String getCenterCoordinates()
    {
        if(!hasPlacementData())
            return "";

        String rowString = topLeftAnchorLoc.substring(0, topLeftAnchorLoc.indexOf("_"));
        String colString = topLeftAnchorLoc.substring(topLeftAnchorLoc.indexOf("_") + 1);
    
        int starti = Integer.parseInt(rowString.substring(1)) - padTop;
        int startj = Integer.parseInt(colString.substring(1)) - padLeft;

        int endi = Integer.parseInt(rowString.substring(1)) + padBottom + shape.currRow;
        int endj = Integer.parseInt(colString.substring(1)) + padRight + shape.currCol;

        int midi = (starti + endi)/2;
        int midj = (startj + endj)/2;

        String coord = "R" + Integer.toString(midi) + "_C" + Integer.toString(midj);
        return coord;
    }

    //This places the shape in the desired location
    //returns true if it has been able to place the shape or else returns false
    public boolean placeNode(Design des, EDIFNet clkNet, EDIFNet rstNet)
    {
        System.out.println("Trying to place the node: " + name + " of shape: " + shape.pblockName + " at top-left anchor location: " + topLeftAnchorLoc);

        design = des;
        Module module = shape.createModule(design);
        if(module == null)
        {
            System.out.println("ERROR: Could not form a module");
            return false;
        }

        moduleInst = design.createModuleInst(name, module);
        moduleEDIFCellInst = moduleInst.getCellInst();

        if(moduleInst == null)
        {
            System.out.println("ERROR: Could not create a module instance");
            return false;
        }

        if(moduleEDIFCellInst == null)
        {
            System.out.println("ERROR: Could create a moduleEDIFCell cell instance");
            return false;
        }

        if(!moduleInst.place(rwAnchorSite, false, false))
        {
            System.out.println("ERROR: Could not place node in anchor site");
            return false;
        }

        clkNet.createPortInst(DesignPorts.CLK, moduleInst.getCellInst());
        rstNet.createPortInst(DesignPorts.RST, moduleInst.getCellInst());

        System.out.println("Placed node successfuly at anchor site: " + rwAnchorSiteName);
        return true;
    }

    //This helps to make buffers in the connects with bitwidth mismatch
    public ArrayList<AdjustmentBuffer> getAdjustmentBufferForBitwidthMismatch()
    {
        ArrayList<AdjustmentBuffer> adj = new ArrayList<>();
        for(Input i : inputs)
        {
            AdjustmentBuffer buf = i.getAdjustmentBufferForBitwidthMismatch();
            if(buf != null)
                adj.add(buf);
        }

        return adj;
    }

    //This helps to make corrections in the connections of the node for bitwidth mismatch
    public void updateConnectionsForBitwidthMismatch() throws IOException
    {
        for(Input i : inputs)
            i.updateConnectionsForBitwidthMismatch();
    }

    //This stitches the remaining pins of the node that were not standard
    public void stitchRemainingPins()
    {
        Map<String, ArrayList<Integer>> buses = component.modulePorts.buses;
        EDIFCell top = design.getNetlist().getTopCell();

        //Connect input pins
        for (Map.Entry<String,ArrayList<Integer>> entry : buses.entrySet()) 
        {
            String busName = entry.getKey();
            int busWidth = entry.getValue().get(0);
            int direction = entry.getValue().get(1);

            if(direction == 1) //Means output pin
                continue;
            

            if(busName.startsWith(ModulePorts.CLK) || busName.startsWith(ModulePorts.RST)) //clk pins and reset pins
                continue;

            if(busName.startsWith(ModulePorts.DataIn) || busName.startsWith(ModulePorts.ValidIn)) //Standard data in pins
            {
                int busIndex = Integer.parseInt(busName.substring(busName.indexOf(underscore) + 1));
                if(busIndex < inputs.size())
                    continue;
            }

            if(busName.startsWith(ModulePorts.ReadyIn)) //Standard data in pin
            {
                int busIndex = Integer.parseInt(busName.substring(busName.indexOf(underscore) + 1));
                if(busIndex < outputs.size())
                    continue;
            }

            System.out.println(busName);

            if(busWidth > 1)
            {
                top.createPort(name + underscore + busName + "_Port[" + (busWidth-1) + ":0]", EDIFDirection.INPUT, busWidth);
                for(int i = 0; i < busWidth; i++)
                    moduleInst.connect(busName, i, null, name + underscore + busName + "_Port", i);
            }

            else
            {
                top.createPort(name + underscore + busName + "_Port", EDIFDirection.INPUT, 1);
                moduleInst.connect(busName, -1, null, name + underscore + busName + "_Port", -1);
            }
        }

        //Connect the output pins
        for (Map.Entry<String,ArrayList<Integer>> entry : buses.entrySet()) 
        {
            String busName = entry.getKey();
            int busWidth = entry.getValue().get(0);
            int direction = entry.getValue().get(1);

            if(direction == 0) //Means input pin
                continue;

            if(busName.startsWith(ModulePorts.DataOut) || busName.startsWith(ModulePorts.ValidOut)) //Standard data out pins
            {
                int busIndex = Integer.parseInt(busName.substring(busName.indexOf(underscore) + 1));
                if(busIndex < outputs.size())
                    continue;
            }

            if(busName.startsWith(ModulePorts.ReadyOut)) //Standard data out pin
            {
                int busIndex = Integer.parseInt(busName.substring(busName.indexOf(underscore) + 1));
                if(busIndex < inputs.size())
                    continue;
            }

            System.out.println(busName);

            if(busWidth > 1)
            {
                top.createPort(name + underscore + busName + "_Port[" + (busWidth-1) + ":0]", EDIFDirection.OUTPUT, busWidth);
                for(int i = 0; i < busWidth; i++)
                    moduleInst.connect(busName, i, null, name + underscore + busName + "_Port", i);
            }

            else
            {
                top.createPort(name + underscore + busName + "_Port", EDIFDirection.OUTPUT, 1);
                moduleInst.connect(busName, -1, null, name + underscore + busName + "_Port", -1);
            }
        }

    }

    //This stiches all the peripherals of the node. Creates ports wherever necessary
    //Necessary to create an input port and place the node before calling this function
    public void stitchPeripherals()
    {
        System.out.println("Stitching inputs of node: " + name);
        for(int i = 0; i < inputs.size(); i++)
            inputs.get(i).stitchToOutput();

        System.out.println("Creating ports for outputs of node: " + name);
        for(int i = 0; i < outputs.size(); i++)
            outputs.get(i).createOutputPort();

        System.out.println("Creating ports for non-standard pins of node: " + name);
        stitchRemainingPins();
    }

    //This dumps the data of the node. Required for testing and debugging
    public void printNode()
    {
        System.out.println("-----------------------------------------");
        System.out.println("Printing node");
        System.out.println("Name: " + name);
        System.out.println("Type: " + type);
        System.out.println("Node ID: " + nodeID);
        System.out.println("Component Operator: " + compOperator);
        System.out.println("Component Value: " + compValue);
        System.out.println("Slots: " + slots);
        System.out.println("Memory: " + mem);
        System.out.println("BB Count: " + bbCount);
        System.out.println("Load Count: " + loadCount);
        System.out.println("Store Count: " + storeCount);
        System.out.println("Data Size: " + dataSize);
        System.out.println("Input Size: " + inputs.size());
        System.out.println("Outputs Size: " + outputs.size());
        System.out.println("DCP Name: " + dcpName);
    }
}
