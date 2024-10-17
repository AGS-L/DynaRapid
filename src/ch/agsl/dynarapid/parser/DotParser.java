/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/
package ch.agsl.dynarapid.parser;
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

//This parses the dot file and generates a map of the nodes in the graph

import java.util.*;
import java.io.*;

public class DotParser {
    public static final int normalLine = 0;
    public static final int componentLine = 1;
    public static final int connectionLine = 2;

    public static int numNodes = 0;
    public static Map<String, Node> nodes = new HashMap<>();

    /*
     * This gives the line type in the dot file.
     * There are two types of lines in the dot file.
     * One which defines a node in the dot file. (Has word type in the line)
     * Second describes the connections between the nodes. (has -> in the line)
     * @param line The line that needs to be checked
     * @return true if the line defines nodes, else false;
     */
    public static int lineType(String line)
    {
        if(line.indexOf("type") != -1)
            return componentLine;

        if(line.indexOf("->") != -1)
            return connectionLine;

        return normalLine;
    }

    //This gives a map of all the parameters in teh param line
    //The key of the map is the parameter tag and the value is the value associated with the tag
    public static Map<String, String> getParameters(String paramLine)
    {
        ArrayList<String> parameters = new ArrayList<>();
        Map<String, String> params = new HashMap<>();

        paramLine += ",";
        String s = "";
        
        for(int i = 0; i < paramLine.length(); i++)
        {
            if(paramLine.charAt(i) == ',')
            {
                parameters.add(s);
                s = "";
                continue;
            }

            s += paramLine.substring(i, i+1);
        }

        for(String st : parameters)
        {
            if(st.length() < 2)
                continue;
                
            String tag = st.substring(0, st.indexOf('='));
            String value = DotString.removeQuotes(st.substring(st.indexOf('=') + 1));
            params.put(tag, value);
        }

        return params;
    }

    //////////////////////////////////////////////////////////////////
    //Start of coonection line parsing

    //This function returns the output node name of the connection line
    public static String getOutputNodeName(String line)
    {
        return DotString.removeUnderscore(DotString.removeQuotes(line.substring(0, line.indexOf("->"))));
    }

    //This function returns the input node name of the connection line
    public static String getInputNodeName(String line)
    {
        return DotString.removeUnderscore(DotString.removeQuotes(line.substring(line.indexOf("->") + 2, line.indexOf("["))));
    }

    //This function parses through the conection line.
    public static boolean parseConnectionLine(String line)
    {
        String outputNodeName = getOutputNodeName(line);
        String inputNodeName = getInputNodeName(line);

        if(!nodes.containsKey(inputNodeName) || !nodes.containsKey(outputNodeName))
        {
            System.out.println("ERROR: " + inputNodeName + " or " + outputNodeName + " has not been defined before line " + line);
            return false;
        }

        Node inputNode = nodes.get(inputNodeName);
        Node outputNode = nodes.get(outputNodeName);

        String paramLine = line.substring(line.indexOf('[') + 1, line.indexOf(']'));
        Map<String, String> params = getParameters(paramLine);

        if(!params.containsKey("from"))
        {
            System.out.println("ERROR: No output parameter in the line " + line);
            return false;
        }

        int outputIndex = DotString.getNumber(params.get("from")) - 1;

        if(!params.containsKey("to"))
        {
            System.out.println("ERROR: No input parameter in the line " + line);
            return false;
        }

        int inputIndex = DotString.getNumber(params.get("to")) - 1;

        if((inputIndex == -2) || (outputIndex == -2))
        {
            System.out.println("ERROR: input index or output index not present in line " + line);
            return false;
        }

        inputNode.connectInput(inputIndex, outputNode, outputIndex);
        outputNode.connectOutput(outputIndex, inputNode, inputIndex);
        return true;
    }



    //////////////////////////////////////////////////////////////////
    //Start of the component line parsing

    //Function returns the actual node name of the component with the underscores
    public static String getActualNodeName(String line)
    {
        return DotString.removeQuotes(line.substring(0, line.indexOf('[')));
    }

    //Function returns the name of node from the component line
    public static String getNodeName(String line)
    {
        return DotString.removeUnderscore(DotString.removeQuotes(line.substring(0, line.indexOf('['))));
    }

    //This parses the component line and creates a node and sets it in the map of nodes
    public static void parseComponentLine(String line, String actualString)
    {
        String nodeName = getNodeName(line);
        String actualNodeName = getActualNodeName(line);

        String paramLine = line.substring(line.indexOf('[') + 1, line.lastIndexOf(']'));
        Map<String, String> params = getParameters(paramLine);

        Node node = new Node(actualNodeName, nodeName, params, actualString);
        nodes.put(nodeName, node);
    }

    //This parses through the dotFile and gives the associated node map
    public static Map<String, Node> parser(String dotLoc)
    {
        StringUtils.printIntro("Parsing " + dotLoc);
        
        File file = new File(dotLoc);

        try
        {
            Scanner in = new Scanner(file);
            while(in.hasNextLine())
            {
                String actualString = in.nextLine();
                String s = DotString.removeSpace(DotString.removeComment(actualString));
                if(s.length() < 2)
                    continue;

                if(lineType(s) == componentLine) //component description line
                    parseComponentLine(s, actualString);

                if(lineType(s) == connectionLine)
                    if(!parseConnectionLine(s))
                        throw new Exception("ERROR: Could not make connection in line " + s + ". See above logs");
            }
        }

        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not parse the dot file");
            return null;
        }

        return nodes;
    }
}
