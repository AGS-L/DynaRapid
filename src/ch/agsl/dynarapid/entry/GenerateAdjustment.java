/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.entry;
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


//This class is supposed to generate

import java.io.*;
import java.util.*;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.python.antlr.PythonParser.else_clause_return;

public class GenerateAdjustment {
    
    public static void main(String args[])
    {
        if(StringUtils.findInArray("-f", args) == -1)
        {
            System.out.println("<usage>: java GenerateDesign [-f]");
            System.out.println("-f <arg> - Location of the .dot file.");
            return;
        }


        if(!Start.start())
            return;

        String dotLoc = args[StringUtils.findInArray("-f", args) + 1];
        String graphName = StringUtils.getGraphName(dotLoc);

        StringUtils.printIntro("Generating graph from: " + dotLoc);
        Map<String, Node> nodes = DotParser.parser(dotLoc);
        if(nodes == null)
        {
            System.out.println("ERROR: Could not parse dot file. See above logs");
            return;
        }

        System.out.println("Completed parsing the dot file. Number of nodes are: " + nodes.size());

        ArrayList<AdjustmentBuffer> adjBuffers = new ArrayList<>();

        StringUtils.printIntro("Checking connections for bitwidth mismatch and creating adjustment buffers");
        for (Map.Entry<String,Node> entry : nodes.entrySet()) 
        {
            Node node = entry.getValue();
            ArrayList<AdjustmentBuffer> bufs = node.getAdjustmentBufferForBitwidthMismatch();
            if(bufs.size() > 0)
                for(int i = 0; i < bufs.size(); i++)
                    adjBuffers.add(bufs.get(i));
        }

        System.out.println("Requirement of: " + adjBuffers.size() + " adjustment buffers detected.");

        StringUtils.printIntro("Creating adjusted dot file");

        File src = new File(dotLoc);
        File dest = new File(LocationParser.dotFiles + graphName + "_adjusted.dot");

        try
        {
            Scanner in = new Scanner(src);
            FileWriter fileWriter = new FileWriter(dest);

            while(in.hasNextLine())
            {
                String line = in.nextLine();
                String s = DotString.removeSpace(DotString.removeComment(line));

                if(DotParser.lineType(s) == DotParser.componentLine)
                {
                    int i;
                    for(i = 0; i < adjBuffers.size(); i++)
                        if(adjBuffers.get(i).fillComponentLine(fileWriter, line))
                            break;

                    if(i == adjBuffers.size())
                        fileWriter.write(line + "\n");
                }

                else if(DotParser.lineType(s) == DotParser.connectionLine)
                {
                    int i;
                    for(i = 0; i < adjBuffers.size(); i++)
                        if(adjBuffers.get(i).fillConnectionLine(fileWriter, line))
                            break;

                    if(i == adjBuffers.size())
                        fileWriter.write(line + "\n");
                }

                else if(DotParser.lineType(s) == DotParser.normalLine)
                    fileWriter.write(line + "\n");

                else
                    throw new Exception("ERROR: Could not find line type: " + line);
            }

            fileWriter.close();
            in.close();
        }

        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not generate adjusted dot file. See above logs");
        }

        System.out.println("Generated adjustment dot file in location: " + LocationParser.dotFiles + graphName + "_adjusted.dot");

        ErrorLogger.printErrorLogs();
    }
}
