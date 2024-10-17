/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/


package ch.agsl.dynarapid.graphgenerator;
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

//This generates the dcp names required for the node values

public class DCPNameGenerator {
    public static final String underscore = "_";

    //Know shit what this function does. Copied from vhdl_writer (func: get_memory_inputs)
    public static int getMemoryInputs(Node node)
    {
        int memInputs = node.inputs.size();
        for(int i = 0; i < node.inputs.size(); i++)
            if(!node.inputs.get(i).type.equals("e"))   
                memInputs--;

        return memInputs;
    }


    //This gets the string to be appended for the constant type of nodes
    public static String formConstantStringForDCPName(Node node)
    {
        String temp = "";
        long value = node.compValue;
        String val = "";

        for(int i = 0; i < node.inputs.get(0).bitSize; i++)
        {
            temp += Long.toString(value % 2);
            value = value / 2;
        }

        for(int i = temp.length()-1; i >= 0; i--)
            val += temp.substring(i, i+1);

        return val;
    }


    //This gets the dcpName which is the generic dcpName
    public static String formGenericStringForDCPName(Node node)
    {
        String name = "";

        if(node.inputs.size() == 0)
            name += "32" + underscore;
        else
            name += Integer.toString(node.inputs.get(0).bitSize) + underscore;

        if(node.outputs.size() == 0)
            name += "32";
        else
            name += Integer.toString(node.outputs.get(0).bitSize);

        return name;
    }

    //gets the name of the dcp from the filled values of the node.
    //Run by the constructor itself
    public static String getDCPName(Node node)
    {
        String name = ""; //temporarily stores the name of the dcp

        if(node.type.equals("Operator"))
            name = node.compOperator + underscore;
        else
            name = node.type + underscore;

        if(!node.type.equals("MC") && !node.type.equals("Exit"))
        {
            name += Integer.toString(node.inputs.size()) + underscore;
            name += Integer.toString(node.outputs.size()) + underscore;
        }
    
        if(!node.type.equals("Mux") && !node.type.equals("CntrlMerge") && !node.type.equals("MC") && !node.type.equals("Exit"))
            name += formGenericStringForDCPName(node);
        
        if(node.type.equals("Constant"))
            name += underscore + formConstantStringForDCPName(node);

        if(node.type.equals("transpFIFO") || node.type.equals("nonTranspFIFO"))
            name += underscore + node.slots;

        if(node.compOperator.equals("getelementptr_op"))
        {
            //System.out.println("bitsize = " + node.inputs.get(0).bitSize + " and value: " + node.compValue);
            name += underscore + formConstantStringForDCPName(node);
        }

        if(node.type.equals("Mux"))
        {
            name += Integer.toString(node.inputs.get(1).bitSize) + underscore;
            name += Integer.toString(node.outputs.get(0).bitSize) + underscore;
            name += Integer.toString(node.inputs.get(0).bitSize);
        }

        if(node.type.equals("CntrlMerge"))
        {
            name += Integer.toString(node.inputs.get(0).bitSize) + underscore;
            name += Integer.toString(node.outputs.get(0).bitSize) + underscore;
            name += Integer.toString(node.outputs.get(1).bitSize);
        }

        if(node.type.equals("MC"))
        {
            name += Integer.toString(node.dataSize) + underscore;
            name += Integer.toString(node.addressSize) + underscore;
            name += Integer.toString(node.bbCount) + underscore;
            name += Integer.toString(node.loadCount) + underscore;
            name += Integer.toString(node.storeCount);
        }

        if(node.type.equals("Exit"))
        {
            name += Integer.toString(node.inputs.size() - getMemoryInputs(node)) + underscore;
            name += Integer.toString(getMemoryInputs(node)) + underscore;
            name += Integer.toString(node.outputs.size()) + underscore;
            name += "0" + underscore;
            name += Integer.toString(node.outputs.get(0).bitSize);
        }

        //System.out.println(node.name + " corresponds to component: " + name);
        return name;
    }
    
}
