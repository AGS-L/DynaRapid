/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/



//This helps to parse the placement information and update the placement information

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
import org.python.core.util.StringUtil;


public class PlacementParser {
    
    public static String checkStrings[] = {
        "Graph Name: ",
        "Placer Name: ",
        "# of Nodes: ",
    };

    public static String removeExtraSpaces(String s)
    {
        return s.trim().replaceAll(" +", " ");
    }

    public static Map<String, Node> parsePlacementLine(Map<String, Node> nodes, String line)
    {
        Scanner in = new Scanner(line);
        String nodeName = "";
        String topLeftAnchorLoc = "";
        String pblockName = "";
        int leftPad, rightPad, topPad, bottomPad;

        leftPad = rightPad = topPad = bottomPad = -1;

        nodeName = in.next();
        if(in.next().equals("-location"));
            topLeftAnchorLoc = in.next();

        if(in.next().equals("-padding"))
        {
            leftPad = Integer.parseInt(in.next());
            rightPad = Integer.parseInt(in.next());
            topPad = Integer.parseInt(in.next());
            bottomPad = Integer.parseInt(in.next());
        }

        if(in.next().equals("-shape"))
            pblockName = in.next();

        if(nodeName.equals("") || topLeftAnchorLoc.equals("") || (leftPad == -1) || (rightPad == -1) || (topPad == -1) || (bottomPad == -1) || pblockName.equals(""))
            return null;

        Node node = nodes.get(nodeName);
        if(node == null)
        {
            System.out.println("ERROR: Could not find the node of name: " + nodeName);
            return null;
        }

        if(!pblockName.startsWith(node.component.dcpName))
        {
            System.out.println("ERROR: Could not match the name of the component with the node");
            return null;
        }

        Device device = Device.getDevice("xcvu13p-fsga2577-1-i");

        Shape shape = null;

        for(Shape sh : node.component.shapeList)
            if(sh.pblockName.equals(pblockName))
            {
                shape = sh;
                break;
            }

        if(shape == null)
        {
            System.out.println("ERROR: Could not match the shape name with any valid shapes of the design");
            return null;
        }

        if(!node.updatePlacementData(topLeftAnchorLoc, leftPad, rightPad, topPad, bottomPad, shape))
        {
            System.out.println("ERROR: Could not update placement information of node name: " + nodeName + ". See above logs");
            return null;
        }

        return nodes;
    }

    public static Map<String, Node> parser(Map<String, Node> nodes, String placeLoc, String placerName, String graphName)
    {
        StringUtils.printIntro("Parsing Placement Information");

        int index = 0;

        try
        {
            File file = new File(placeLoc);
            Scanner in = new Scanner(file);

            while(in.hasNextLine())
            {
                String line = removeExtraSpaces(DotString.removeComment(in.nextLine()));
                if(line.length() < 2)
                    continue;

                if(index == 0)
                {
                    if(!line.startsWith(checkStrings[0]))
                    {
                        System.out.println("ERROR: Could not find graph name.");
                        return null;
                    }

                    if(!graphName.equals(line.substring(line.indexOf(": ") + 2)))
                    {
                        System.out.println("ERROR: Graph name not matching");
                        return null;
                    }

                    index++;
                    continue;
                }

                if(index == 1)
                {
                    if(!line.startsWith(checkStrings[1]))
                    {
                        System.out.println("ERROR: Could not find placer name.");
                        return null;
                    }

                    if(!placerName.equals(line.substring(line.indexOf(": ") + 2)))
                    {
                        System.out.println("ERROR: placer name not matching");
                        return null;
                    }

                    index++;
                    continue;
                }

                if(index == 2)
                {
                    if(!line.startsWith(checkStrings[2]))
                    {
                        System.out.println("ERROR: Could not find number of nodes.");
                        return null;
                    }

                    if(nodes.size() != Integer.parseInt(line.substring(line.indexOf(": ") + 2)))
                    {
                        System.out.println("ERROR: number of nodes not matching with size of graph");
                        return null;
                    }

                    index++;
                    continue;
                }

                if(index == 3)
                {
                    if(parsePlacementLine(nodes, line) == null)
                    {
                        System.out.println("ERROR: Could not parse placement line: " + line);
                        return null;
                    }
                }
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not parse the placement information successfully.");
            return null;
        }

        return nodes;
    }
}
