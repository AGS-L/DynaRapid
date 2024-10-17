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
//Thses are the adjustment buffers that are used to temporarily solve bitwidth mismatch problem.
//These are basically buffers with only 1 input, one output of different bitwidth
//These have only one slot and is non-transparent.

import java.io.*;
import java.util.*;

import org.apache.commons.io.output.FileWriterWithEncoding;

public class AdjustmentBuffer implements Serializable
{

    //_Buffer_1 [type=Buffer, in="in1:32", out="out1:32", bbID = 2, slots=1, transparent=false, label="_Buffer_1 [1]",  shape=box, style=filled, fillcolor=darkolivegreen3, height = 0.4];
    //phi_1 -> _Buffer_1 [from=out1, to=in1, arrowhead=normal, color=red];

    //buffer names
    String actualName; //This holds the actual name of the buffer with underscore)
    String name; //This holds actual name which is without underscore used by the dot parser

    //type definitions
    String type;
    String compOperator;
    int slots;
    boolean transparent;
    int bbID;

    //asthetics values
    String label;
    String shape;
    String style;
    String fillcolor;
    double height;

    //input of the buffer
    Input input;
    Output output;

    //in and out descriptions
    String inLine;
    String outLine;

    //component description
    String componentLine;

    //connection line
    String inputConnectionLine;
    String outputConnectionLine;

    public AdjustmentBuffer(String actualName, Input input, Output output)
    {
        this.actualName = actualName;
        name = DotString.removeUnderscore(actualName);

        compOperator = type = "Buffer";
        slots = 1;
        transparent = false;
        bbID = 2;

        String transpString = "false";
        if(transparent)
            transpString = "true";

        label = "\"" + actualName + " [1]\"";
        shape = "box";
        style = "filled";
        fillcolor = "darkolivegreen3";
        height = 0.4;

        this.input = input;
        this.output = output;

        inLine = "\"in1:" + output.bitSize + "\"";
        outLine = "\"out1:" + input.bitSize + "\"";

        //_Buffer_1 [type=Buffer, in="in1:32", out="out1:32", bbID = 2, slots=1, transparent=false, label="_Buffer_1 [1]",  shape=box, style=filled, fillcolor=darkolivegreen3, height = 0.4];
        componentLine = actualName + " [";
        componentLine += "type=" + type + ", ";
        componentLine += "in=" + inLine + ", ";
        componentLine += "out=" + outLine + ", ";
        componentLine += "bbID=" + bbID + ", ";
        componentLine += "slots=" + slots + ", ";
        componentLine += "transparent=" + transpString + ", ";
        componentLine += "label=" + label + ", ";
        componentLine += "shape=" + shape + ", ";
        componentLine += "style=" + style + ", ";
        componentLine += "fillcolor=" + fillcolor + ", ";
        componentLine += "height=" + height + "];";

        inputConnectionLine = output.node.actualName + " -> " + actualName + " [from=out" + (output.index+1) + ", to=in1, arrowhead=normal, color=red];";
        outputConnectionLine = actualName + " -> " + input.node.actualName + " [from=out1, to=in" + (input.index+1) + ", arrowhead=normal, color=red];";
    }

    //this inputs the component line in the filewriter.
    //If this line needs a replacement or addition. This is done here. and return ed true
    //If this line does not need a replacement then it is not printed and returned false;
    public boolean fillComponentLine(FileWriter fileWriter, String line) throws IOException
    {

        String s = DotString.removeSpace(DotString.removeComment(line));
        if(input.node.name.equals(DotParser.getNodeName(s)))
        {
            fileWriter.write(line + "\n");
            fileWriter.write(DotString.getLeadingSpaces(line) + componentLine + "\n");
            return true;
        }

        return false;
    }

    //this inputs the connection line in the fileWriter
    //If this line needs any alteration, this is done here and return ed true
    //else this line is not printed and it is returned false
    public boolean fillConnectionLine(FileWriter fileWriter, String line) throws IOException
    {
        String s = DotString.removeSpace(DotString.removeComment(line));
        boolean s1 = output.node.name.equals(DotParser.getOutputNodeName(s));
        boolean s2 = input.node.name.equals(DotParser.getInputNodeName(s));
        String s3 = "from=out" + (output.index+1);
        String s4 = "to=in" + (input.index+1);

        if(s1 && s2 && line.contains(s3) && line.contains(s4))
        {
            fileWriter.write(DotString.getLeadingSpaces(line) + inputConnectionLine + "\n");
            fileWriter.write(DotString.getLeadingSpaces(line) + outputConnectionLine + "\n");
            return true;
        }

        return false;
    }
    
}
