/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.placer;
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

//This goes through all the nodes in the graph in a breadth first search method starting from the node with the largest number of connections
//This goes through all the shapes in greedy fashion and not in the recursive fashion

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

import org.python.antlr.PythonParser.else_clause_return;

import java.lang.*;

public class GreedyPlacer implements Placer {

    //Class to define pair because apparently java does not pair definition
    private class Pair
    {
        int key;
        String value;

        public Pair(int k, String v)
        {
            key = k;
            value = v;
        }

        public int getKey()
        {
            return key;
        }

        public String getValue()
        {
            return value;
        }
    }

    Node fabric[][];
    int centeri, centerj; //Center I and J of the node. I corresponds to number of coulumns and J corresponds to number of rows
    Site centerSite; //Site corresponding to the center i and j
    int topRow, bottomRow, leftCol, rightCol; //These define the boundaries of the fabric within which the design must be placed 

    Device device;
    Queue<Node> nodeQ; //Nodes are placed in this order in the queue

    Shape shape; //temporarily stores the shape and the top left anchor location for the node
    String topLeftAnchorLoc;

    int pad = 0;

    /*
     * Constructor is called from the GenerateDesign class to start the placer algorithm
     * This constructor is invoked only when the fabric unconstrained
     */
    public GreedyPlacer()
    {
        setGlobalFields();
        topRow = 0;
        bottomRow = fabric.length;
        leftCol = 0;
        rightCol = fabric[0].length;
    }

    /*
     * Constructor is called from the GenerateDesign class to start the placer algorithm
     * This constructor is invoked only when the fabric constrained using the topRow, bottomRow, lowColumn, rightColumn
     */
    public GreedyPlacer(int tr, int br, int lc, int rc)
    {
        setGlobalFields();
        topRow = tr;
        bottomRow = br;
        leftCol = lc;
        rightCol = rc;
    }

    /*
     * This is a constructor helper function
     * This sets most of the global fields of the placer object
     */
    public void setGlobalFields()
    {
        device = Device.getDevice("xcvu13p-fsga2577-1-i");
        nodeQ = new LinkedList<>();

        int rows = MapElement.map.size();
        int cols = MapElement.map.get(0).size();
        fabric = new Node [rows][cols];

        String siteName = "SLICE_X67Y624"; //Near the center

        centerSite = device.getSite(siteName);
        String tileName = centerSite.getTile().getName();
        String coord = MapElement.findInMap(tileName);
        centeri = Integer.parseInt(coord.substring(0, coord.indexOf(":")));
        centerj = Integer.parseInt(coord.substring(coord.indexOf(":") + 1));

        //set padding here
        pad = 0;
    }

    public String getPlacerName()
    {
        return "Greedy-Placer";
    }

    public  boolean setDesignCenterUsingSiteName(String siteName)
    {
        try
        {
            centerSite = device.getSite(siteName);
            String tileName = centerSite.getTile().getName();
            String coord = MapElement.findInMap(tileName);
            centeri = Integer.parseInt(coord.substring(0, coord.indexOf(":")));
            centerj = Integer.parseInt(coord.substring(coord.indexOf(":") + 1));
        }

        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not set the center of the design as " + siteName + ". See above logs");
            return false;
        }
        return true;
    }

    public boolean setDesignCenterUsingCoordinates(int i, int j, int side)
    {
        int n = MapElement.map.size();
        int m = MapElement.map.get(0).size();

        if((i < 0) || (i >= n) || (j < 0) || (j >= m))
        {
            System.out.println("ERROR: Mentioned center coordinates row = " + i + " column = " + j + " in not in range of the map");
            return false;
        }

        if((side != -1) && (side != 1))
        {
            System.out.println("ERROR: Side provided as " + side + " is not valid. Must be +1 or -1 only");
            return false;
        }

        MapElement element = MapElement.map.get(i).get(j);
        String tileName = "";

        if(!element.leftElementName.startsWith("NULL") && !element.rightElementName.startsWith("NULL"))
            tileName = (side == -1) ? element.leftElementName : element.rightElementName;
        else if(!element.leftElementName.startsWith("NULL"))
            tileName = element.leftElementName;
        else if(!element.rightElementName.startsWith("NULL"))
            tileName = element.rightElementName;
        else 
        {
            System.out.println("ERROR: Both sides of the mentioned coordinates as row = " + i + " and column = " + j + " is NULL");
            return false;
        }

        Tile tile = device.getTile(tileName);
        centerSite = tile.getSites()[0];
        centeri = i;
        centerj = j;
        return true;
    }

    public int[] getDesignCenterCoordinates()
    {
        String tileName = centerSite.getTile().getName();
        MapElement element = MapElement.map.get(centeri).get(centerj);
        int side = 1;
        if(tileName.equals(element.leftElementName))
            side = -1;

        int[] center = new int[3];
        center[0] = centeri;
        center[1] = centerj;
        center[2] = side;
        return center;
    }

    public Site getDesignCenterSite()
    {
        return centerSite;
    }

    //function responsible tp populating the queue. The nodes will be placed in the order of occurance in the queue
    /*
     * First we arrange all the nodes in descending order of number of connections. These ordered list of nodes is stored in orderedNodes arrayList
     * Then we traverse the orderedNodes list and start with the node that has the maximum number of connections.
     * We add that node in the nodeList if it is not in the visited list. If it has already been visited, then we ignore that node
     * Now we traverse through all the nodes in the nodeList using loop variable i.
     * for all the nodes connected to nodeList[i], if the connected nodes is not in the visited list, we add the node in the nodeList
     * We iterate i and then go to previous step
     * If we reach a node where all its connections has been visited, we select the next node in the orderedNodes list and continue from 3rd step
     */
    public boolean populateQueue(Map<String, Node> nodes)
    {
        System.out.println("Starting populating node queue");

        ArrayList<String> nodeList = new ArrayList<>(); //Temporary queue of nodes. This is fed in to the nodesQ
        HashSet<String> visited = new HashSet<>(); //Array of nodes already present in the nodeList
        ArrayList<Pair> orderedNodes = new ArrayList<>(); //Here the nodes are arranged in decreasing order of number of peripherals

        for(Map.Entry<String,Node> entry : nodes.entrySet()) 
        {
            Node node = entry.getValue();
            int peripherals = node.inputs.size() + node.outputs.size();
            Pair p = new Pair(peripherals, entry.getKey());
            orderedNodes.add(p);
        }

        Collections.sort(orderedNodes, new Comparator<Pair>() {
            @Override
            public int compare(Pair p1, Pair p2)
            {
                return p2.getKey()- p1.getKey();
            }
        });

        int pos = -1;
        for(int i = 0; i < orderedNodes.size(); i++)
        {
            String nodeName = orderedNodes.get(i).getValue();
            if(visited.contains(nodeName))
                continue;

            nodeList.add(nodeName);
            visited.add(nodeName);
            pos++; //at this stage will be always be equal to nodeList.size() - 1

            while(pos < nodeList.size())
            {
                String name = nodeList.get(pos);
                Node node = nodes.get(name);

                //checking all the inputs of node
                for(Input in : node.inputs)
                {
                    if(in.validArray == null) //Means a starting node
                        continue;

                    Node outputNode = in.validArray.node;
                    String outputNodeName = outputNode.name;
                    if(!visited.contains(outputNodeName))
                    {
                        nodeList.add(outputNodeName);
                        visited.add(outputNodeName);
                    }
                }

                //checking all the outputs of the node
                for(Output out : node.outputs)
                {
                    if(out.pValidArray == null) //Means a ending node
                        continue;

                    Node inputNode = out.pValidArray.node;
                    String inputNodeName = inputNode.name;
                    if(!visited.contains(inputNodeName))
                    {
                        nodeList.add(inputNodeName);
                        visited.add(inputNodeName);
                    }
                }
                pos++;
            }
        }

        for(int i = 0; i < nodeList.size(); i++)
            nodeQ.add(nodes.get(nodeList.get(i)));

        if(nodeQ.size() != nodes.size())
        {
            System.out.println("ERROR: Node Queue size mismatch. The node Queue has size: " + nodeQ.size() + " whereas we have " + nodes.size() + " nodes");
            return false;
        }

        System.out.println("Node Queue populated.");
        return true;
    }

    /*
     * @param node The node that is to be placed
     * @param centerLoc The preferred center of the node. This was calculated in getPreferredCenterCoordinates()
     * @param i This is the i coordinate of the center where node might be placed
     * @param j This is the j coordinate of the center where the node might be placed
     * @return true if i, j is within the fabric region and false if the i, j is not within the fabric region. This does not return whether a suitable shape for the given i and j was found or not
     * You have 2 global variables: shape, topLeftAnchorLoc. These store the details of the shape that has been considered to be best till that point.
     * If no shape has yet been choosen, then the values are null and ""
     * Given the centerLoc that was previously choosem center of placement and the i and j (referring to the position in consideration)
     * The steps to decide whether the existing shape and topLeftAnchorLoc is best or the new shape, topLeftAnchorLoc is best, we follow the steps
     * 0). If the i, j position is already occupied by some other shape for placement, we return
     * 1). We iterate through all possible shapes of the node
     * 2). If the shape cannot be placed with its center at i, j, then we continue to next shape (move to step 1)
     * 3). If the shape is null and topLeftAnchorLoc is "", then it means we have no existing shape and location for reference, so we store the given shape and topLeftAnchorLoc
     * 4). if the shape is not null, then we have a shape, so we need to decide whether the existing shape and location is better or the new one
     * 5). If the new shape is same as the previous shape, then the location which is closer to the centerLoc is choosen and the shape remains the same
     * 6). If the shape is different, then the same which is longer (more aspect ratio, is choosen) and the topLeftnchorLoc becomes the topLEftAnchorLoc from i, j
     */
    public boolean getBestPlacementDataInloc(Node node, int[] centerLoc, int i, int j)
    {
        if((i < topRow) || (i > bottomRow) || (j < leftCol) || (j > rightCol)) //outside the contrained region of the fabric
            return false;

        if(fabric[i][j] != null) //already placed on the center location
            return true;

        for(Shape sh : node.component.shapeList)
        {
            String topLeftLoc = sh.getTopLeftCornerStringFromCenterLoc(i, j);
            if(!node.canPlace(fabric, topRow, bottomRow, leftCol, rightCol, sh, topLeftLoc))
                continue;

            if((shape == null) && (topLeftAnchorLoc.equals(""))) //Means we dont have any shape
            {
                shape = sh;
                topLeftAnchorLoc = topLeftLoc;
            }

            else //Means we alraedy have a shape
            {
                if(shape == sh) //Means we have the same shape, just a different location
                {
                    String rowString = topLeftAnchorLoc.substring(0, topLeftAnchorLoc.indexOf("_"));
                    String colString = topLeftAnchorLoc.substring(topLeftAnchorLoc.indexOf("_") + 1);
                    int starti = Integer.parseInt(rowString.substring(1));
                    int startj = Integer.parseInt(colString.substring(1));
                    int currDistance = Math.abs(starti - centerLoc[0]) + Math.abs(startj - centerLoc[1]);

                    rowString = topLeftLoc.substring(0, topLeftLoc.indexOf("_"));
                    colString = topLeftLoc.substring(topLeftLoc.indexOf("_") + 1);
                    starti = Integer.parseInt(rowString.substring(1));
                    startj = Integer.parseInt(colString.substring(1));

                    int newDistance = Math.abs(starti - centerLoc[0]) + Math.abs(startj - centerLoc[1]);
                    if(newDistance < currDistance)
                    {
                        shape = sh;
                        topLeftAnchorLoc = topLeftLoc;
                    }
                }

                else //Means we have a different shape
                {
                    double currAspectRatio = shape.getAspectRatio();
                    double newAspectRatio = sh.getAspectRatio();

                    if(newAspectRatio > currAspectRatio) //preferring longer shapes over wider shapes
                    {
                        shape = sh;
                        topLeftAnchorLoc = topLeftLoc;
                    }
                }
            }
        }
        return true;
    }

    //Thsi returns the best placement information of rthe given layer defined by starti, startj as the top - left position of the layer and the rows and cols as the number of rows and columns of the layer
    //here we move clockwise in the layer
    //This returns if the shape and the topLeftAnchorLoc field has been updated or not.
    //This function takes a shape field and topLeftAnchorLoc field both with defualt values an then fills it up.
    //@param centerLoc This is the center location on the map around which the layer revolves. This center is the most appropriate place to place a node
    public boolean getBestPlacementDataInLayer(Node node, int[] centerLoc, int starti, int startj, int rows, int cols)
    {
        /*
         * //This tells if any part of this layer was within the constarined region of the fabric. 
         * If it is true, then some part of the layer was within the constarined region. 
         * If false, then not. There is no point in enlarging the layer at this point.
         */
        boolean wasLayerInsideConstrainRegion = false; 
        int i = starti;
        int j = startj;
        
        if((rows == 0) && (cols == 0))
            wasLayerInsideConstrainRegion |= getBestPlacementDataInloc(node, centerLoc, i, j);
             
        //right movement
        j++;
        for(; j <= (startj + cols); j++)
            wasLayerInsideConstrainRegion |= getBestPlacementDataInloc(node, centerLoc, i, j);

        //down movement
        j--;
        i++;
        for(; i <= (starti + rows); i++)
            wasLayerInsideConstrainRegion |= getBestPlacementDataInloc(node, centerLoc, i, j);

        //left movement
        i--;
        j--;
        for(; j >= startj; j--)
            wasLayerInsideConstrainRegion |= getBestPlacementDataInloc(node, centerLoc, i, j);

        //Up movement
        j++;
        i--;
        for(; i >= starti; i--)
            wasLayerInsideConstrainRegion |= getBestPlacementDataInloc(node, centerLoc, i, j);

        return wasLayerInsideConstrainRegion;
    }

    /*
     * This gets the preferred center coordinates for a perticular node
     * It gets the center coordinates of all the nodes that are connected to itand have already been placed.
     * It finds the mean of the these center coordinates with the center of the design 
     * The resultant coordinate is the center of the given node
     */
    public int[] getPreferredCenterCoordinates(Node node)
    {
        int count = 1;
        int center[] = new int[2];
        center[0] = centeri;
        center[1] = centerj;

        //checking all the inputs of node
        for(Input in : node.inputs)
        {
            if(in.validArray == null) //Means a starting node
                continue;

            Node outputNode = in.validArray.node;
            if(!outputNode.hasPlacementData())
                continue;

            count++;
            String centerLoc = outputNode.getCenterCoordinates();
            String rowString = centerLoc.substring(0, centerLoc.indexOf("_"));
            String colString = centerLoc.substring(centerLoc.indexOf("_") + 1);
            int starti = Integer.parseInt(rowString.substring(1));
            int startj = Integer.parseInt(colString.substring(1));
            center[0] += starti;
            center[1] += startj;
        }

        //checking all the outputs of node
        for(Output out : node.outputs)
        {
            if(out.pValidArray == null) //Means a ending node
                        continue;

            Node inputNode = out.pValidArray.node;
            if(!inputNode.hasPlacementData())
                continue;

            count++;
            String centerLoc = inputNode.getCenterCoordinates();
            String rowString = centerLoc.substring(0, centerLoc.indexOf("_"));
            String colString = centerLoc.substring(centerLoc.indexOf("_") + 1);
            int starti = Integer.parseInt(rowString.substring(1));
            int startj = Integer.parseInt(colString.substring(1));
            center[0] += starti;
            center[1] += startj;
        }

        center[0] = center[0] / count;
        center[1] = center[1] / count;
        return center;
    }

    //This places all the nodes based on the nodeQ order. So this iterates the nodeQ list
    public boolean placeAllNodes(Map<String, Node> nodes)
    {
        while(!nodeQ.isEmpty())
        {
            Node node = nodeQ.peek();
            node.updatePadding(pad, pad, pad, pad); //This updates the padding of the node to uniform padding
            System.out.print("Calculating preferred center location of node: " + node.name);
            int centerLoc[] = getPreferredCenterCoordinates(node); 
            System.out.println("Preferred Coordinates are : R" + centerLoc[0] + "_C" + centerLoc[1]);
            //This returns the preferred coordiates of the center of the node
            //The coord[0] is the i / R values and coord[1] is the j / C value

            shape = null;
            topLeftAnchorLoc = "";
            int starti = centerLoc[0];
            int startj = centerLoc[1];

            int rows = 0;
            int cols = 0;
            System.out.println("Calculating best placement shape and location for the node: " + node.name);
            while(shape == null)
            {
                if(!getBestPlacementDataInLayer(node, centerLoc, starti, startj, rows, cols))
                {
                    System.out.println("ERROR: Could not find any placement shape and site for node: " + node.name + ". Reduce contraints on fabric dimensions and re-run GenerateDesign");
                    return false;
                }

                starti -= 1;
                startj -= 1;
                rows += 2;
                cols += 2;
            }

            if(!node.updatePlacementData(fabric, shape, topLeftAnchorLoc))
            {
                System.out.println("ERROR: Could not update placement data of node: " + node.name + " with shape: " + shape.pblockName + " at top left anchor location: " + topLeftAnchorLoc);
                return false;
            }
            System.out.println(node.name + " is placed at top-left anchor location of: " + node.topLeftAnchorLoc + " at RW anchor site: " + node.rwAnchorSiteName);
            nodeQ.remove();
        }
        return true;
    }

    public Map<String, Node> placer(Map<String, Node> nodes)
    {
        if((centeri < topRow) || (centeri > bottomRow) || (centerj < leftCol) || (centerj > rightCol))
        {
            System.out.println("Design center of: I = " + centeri + " J = " + centerj + ". is not within the constrained region of the FPGA fabric. Exiting design placement");
            return null;
        }

        if(!populateQueue(nodes))
        {
            System.out.println("ERROR: Could not populate the queue. See above logs");
            return null;
        }

        if(!placeAllNodes(nodes))
        {
            System.out.println("ERROR: Could not find placement position / shape for all the nodes. See above logs");
            return null;
        }

        System.out.println("Completed graph placement");
        return nodes;
    }

    public Node[][] getNodeFabric()
    {
        return fabric;
    }
    
}
