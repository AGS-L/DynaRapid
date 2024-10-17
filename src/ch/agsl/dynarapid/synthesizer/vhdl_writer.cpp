/*
*  C++ Implementation: dot2Vhdl
*
* Description:
*
*
* Author: Andrea Guerrieri <andrea.guerrieri@epfl.ch (C) 2019
*
* Copyright: See COPYING file that comes with this distribution
*
*/


#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <algorithm> 
#include <list>
#include <cctype>

#include "dot_parser.h"
#include "vhdl_writer.h"
#include "string_utils.h"

string entity_name[] = {
    ENTITY_MERGE, 
    ENTITY_READ_MEMORY, 
    ENTITY_SINGLE_OP,
    ENTITY_GET_PTR,
    ENTITY_INT_MUL,
    ENTITY_INT_ADD,
    ENTITY_INT_SUB,
    ENTITY_BUF,
    ENTITY_TEHB,
    ENTITY_OEHB,
    ENTITY_FIFO,
    ENTITY_FORK,
    ENTITY_ICMP,
    ENTITY_CONSTANT,
    ENTITY_BRANCH,
    ENTITY_END,
    ENTITY_START,
    ENTITY_WRITE_MEMORY,
    ENTITY_SOURCE,
    ENTITY_SINK,
    ENTITY_CTRLMERGE,
    ENTITY_MUX,
    ENTITY_LSQ,
    ENTITY_MC
};

string component_types[] = {
    COMPONENT_MERGE, 
    COMPONENT_READ_MEMORY, 
    COMPONENT_SINGLE_OP,
    COMPONENT_GET_PTR,
    COMPONENT_INT_MUL,
    COMPONENT_INT_ADD,
    COMPONENT_INT_SUB,
    COMPONENT_BUF,
    COMPONENT_TEHB,
    COMPONENT_OEHB,
    COMPONENT_FIFO,
    COMPONENT_FORK,
    COMPONENT_ICMP,
    COMPONENT_CONSTANT_,
    COMPONENT_BRANCH,
    COMPONENT_END,
    COMPONENT_START,
    COMPONENT_WRITE_MEMORY,
    COMPONENT_SOURCE,
    COMPONENT_SINK,
    COMPONENT_CTRLMERGE,
    COMPONENT_MUX,
    COMPONENT_LSQ,
    COMPONENT_MC
};

string inputs_name[] = {
    DATAIN_ARRAY, 
    PVALID_ARRAY, 
    NREADY_ARRAY
    
};
string inputs_type[] = {
    "std_logic_vector(", 
    "std_logic", 
    "std_logic_vector("
};
string outputs_name[] = {
    DATAOUT_ARRAY,
    READY_ARRAY,
    VALID_ARRAY
    
};
string outputs_type[] = {
    "std_logic_vector(",
    "std_logic_vector(",
    "std_logic_vector("
};

vector<string> in_ports_name_generic(inputs_name, inputs_name + sizeof( inputs_name )/sizeof(string));
vector<string> in_ports_type_generic(inputs_type, inputs_type + sizeof(inputs_type)/sizeof(string));
vector<string> out_ports_name_generic(outputs_name, outputs_name + sizeof(outputs_name)/sizeof(string));
vector<string> out_ports_type_generic(outputs_type, outputs_type + sizeof(outputs_type)/sizeof(string));

typedef struct component
{
    int in_ports;
    vector<string> in_ports_name_str;
    vector<string> in_ports_type_str;
    int out_ports;
    vector<string> out_ports_name_str;
    vector<string> out_ports_type_str;
    
}COMPONENT_T;

COMPONENT_T components_type[MAX_COMPONENTS];


ofstream netlist;
#include <bits/stdc++.h>


int get_memory_inputs ( int node_id )
{

    int memory_inputs = nodes[node_id].inputs.size;
    for ( int indx = 0; indx < nodes[node_id].inputs.size; indx++ )
    {
        if ( nodes[node_id].inputs.input[indx].type != "e" )
        {
            memory_inputs--;
        }
    }

    return memory_inputs;
}

//给予component_types，从component_types映射到entity_name，返回entity_name
string get_component_entity ( string component, int component_id )
{
    string component_entity;

    for (int indx = 0; indx < ENTITY_MAX; indx++)
    {
        if ( component.compare(component_types[indx]) == 0 )
        {
            component_entity =  entity_name[indx];
            break;
        }
    }
    if(component_entity == "elasticFifo")
        component_entity = "elasticFifoInner";
    return component_entity;
}

//得到generic参数里面的数据，以下划线隔开 如：2_2_32_32 如果输入输出是0位，不置1
string get_generic_UNDERSCORE ( int node_id )
{
    string generic;

    if ( nodes[node_id].inputs.input[0].bit_size == 0 && !nodes[node_id].inputs.input[0].bitSizeWasZero)
    {
        nodes[node_id].inputs.input[0].bit_size = 32;
    }

    if ( nodes[node_id].outputs.output[0].bit_size == 0 && !nodes[node_id].outputs.output[0].bitSizeWasZero)
    {
        nodes[node_id].outputs.output[0].bit_size = 32;
    }

    if ( nodes[node_id].type.find("Branch") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }
    if ( nodes[node_id].type.find("Buf") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Merge") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Fork") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Constant") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Operator") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.size);
        generic += UNDERSCORE;
        if ( nodes[node_id].component_operator.find("select") != std::string::npos )
        {
            generic += to_string(nodes[node_id].inputs.input[1].bit_size);
        }
        else
        {
            generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        }

        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
//         if ( nodes[node_id].component_operator == COMPONENT_READ_MEMORY || nodes[node_id].component_operator == COMPONENT_WRITE_MEMORY )
//         {
//             generic += UNDERSCORE;
//             generic += to_string(nodes[node_id].outputs.output[0].bit_size);
//         }
        if( nodes[node_id].component_operator.find("getelementptr") != std::string::npos )
        {
            generic += UNDERSCORE;
            generic += string_constant(nodes[node_id].component_value , nodes[node_id].inputs.input[0].bit_size );
        }
        //////////////////////////////////////////////////////ADD HERE///////////////////////////////////////////////
    }

    if ( nodes[node_id].type.find("Entry") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Exit") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size - get_memory_inputs ( node_id ));
        generic += UNDERSCORE;
        generic += to_string( get_memory_inputs ( node_id ) );
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.size);
        generic += UNDERSCORE;

#if 0
        int size_max = 0;
        for ( int indx = 0; indx < nodes[node_id].inputs.size; indx++ )
        {
            if ( nodes[node_id].inputs.input[indx].bit_size > size_max )
            {
                size_max = nodes[node_id].inputs.input[indx].bit_size;
            }
        }
        generic += to_string(size_max);
#endif

        generic += to_string(nodes[node_id].inputs.input[nodes[node_id].inputs.size].bit_size);

        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Sink") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Source") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Fifo") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].slots);
    }

    if ( nodes[node_id].type.find("TEHB") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }
    if ( nodes[node_id].type.find("OEHB") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Mux") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].inputs.input[1].bit_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size); // condition size
    }

    if ( nodes[node_id].type.find("CntrlMerge") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].outputs.output[1].bit_size); // condition size
    }

    if ( nodes[node_id].type.find("MC") != std::string::npos )
    {
        generic += to_string(nodes[node_id].data_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].address_size);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].bbcount);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].load_count);
        generic += UNDERSCORE;
        generic += to_string(nodes[node_id].store_count);


    }

    return generic;
}

//得到generic参数里面的数据，以逗号隔开   如：2,2,32,32 如果输入输出是0位，则置1
string get_generic ( int node_id )
{
    string generic;
    int pre_input_bit_size, pre_output_bit_size;

    if ( nodes[node_id].inputs.input[0].bit_size == 0 )
    {
        if (nodes[node_id].inputs.input[0].bitSizeWasZero)
            nodes[node_id].inputs.input[0].bit_size = 1;
        else
            nodes[node_id].inputs.input[0].bit_size = 32;
    }

    if ( nodes[node_id].outputs.output[0].bit_size == 0)
    {
        if (nodes[node_id].outputs.output[0].bitSizeWasZero)
            nodes[node_id].outputs.output[0].bit_size = 1;
        else
            nodes[node_id].outputs.output[0].bit_size = 32;
    }

    if ( nodes[node_id].type.find("Branch") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }
    if ( nodes[node_id].type.find("Buf") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Merge") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Fork") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Constant") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Operator") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.size);
        generic += COMMA;
        if ( nodes[node_id].component_operator.find("select") != std::string::npos )
        {
            generic += to_string(nodes[node_id].inputs.input[1].bit_size);
        }
        else
        {
            generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        }

        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
        
        if( nodes[node_id].component_operator.find("getelementptr") != std::string::npos )
        {
            generic += COMMA;
            generic += string_constant(nodes[node_id].component_value , nodes[node_id].inputs.input[0].bit_size );
        }
        ///////////////////////////////////////////////////////////////ADD HERE/////////////////////////////////////////
    }

    if ( nodes[node_id].type.find("Entry") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Exit") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size - get_memory_inputs ( node_id ));
        generic += COMMA;
        generic += to_string( get_memory_inputs ( node_id ) );
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.size);
        generic += COMMA;

#if 0
        int size_max = 0;
        for ( int indx = 0; indx < nodes[node_id].inputs.size; indx++ )
        {
            if ( nodes[node_id].inputs.input[indx].bit_size > size_max )
            {
                size_max = nodes[node_id].inputs.input[indx].bit_size;
            }
        }
        generic += to_string(size_max);
#endif
        generic += to_string(nodes[node_id].inputs.input[nodes[node_id].inputs.size].bit_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Sink") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Source") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Fifo") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].slots);
    }

    if ( nodes[node_id].type.find("TEHB") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }
    if ( nodes[node_id].type.find("OEHB") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
    }

    if ( nodes[node_id].type.find("Mux") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].inputs.input[1].bit_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size); // condition size
    }

    if ( nodes[node_id].type.find("CntrlMerge") != std::string::npos )
    {
        generic = to_string(nodes[node_id].inputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.size);
        generic += COMMA;
        generic += to_string(nodes[node_id].inputs.input[0].bit_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.output[0].bit_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].outputs.output[1].bit_size); // condition size
    }

    if ( nodes[node_id].type.find("MC") != std::string::npos )
    {
        generic += to_string(nodes[node_id].data_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].address_size);
        generic += COMMA;
        generic += to_string(nodes[node_id].bbcount);
        generic += COMMA;
        generic += to_string(nodes[node_id].load_count);
        generic += COMMA;
        generic += to_string(nodes[node_id].store_count);


    }


    if (nodes[node_id].inputs.input[0].bitSizeWasZero)
        nodes[node_id].inputs.input[0].bit_size = 0;
    if (nodes[node_id].outputs.output[0].bitSizeWasZero)
        nodes[node_id].outputs.output[0].bit_size = 0;

    return generic;
}

//写VHDL文件的开头
void write_intro (  )
{


    netlist << "-- ==============================================================" << endl;
    netlist << "-- Generated Automatically by Dot2RapidWright " << endl;
    netlist << "-- ==============================================================" << endl;

    netlist << "library IEEE; " << endl;
    netlist << "use IEEE.std_logic_1164.all; " << endl;
    netlist << "use IEEE.numeric_std.all; " << endl;
    netlist << "use work.customTypes.all; " << endl;

    netlist << "-- ==============================================================" << endl;

}

//写VHDL文件entity到end的内容
void write_entity_single ( string  output_vhdl_entityname, int node_id )
{
    netlist << "entity "<< output_vhdl_entityname << " is " << endl;
    netlist << "port (" << endl;

    int indx;
    string signal;
    int i = node_id;
    //inputs Begin
    for ( indx = 0; indx < nodes[i].inputs.size; indx++ )
    {

        int in_port_indx = 0;

        signal = '\t';
        signal += components_type[0].in_ports_name_str[in_port_indx];
        signal += UNDERSCORE;
        signal += to_string( indx );
        signal += COLOUMN;
        if ( nodes[i].type == "Branch" && indx == 1 )
        {
            signal +=" in std_logic_vector (0 downto 0);";
        }
        else
        {
            signal += " in " + components_type[0].in_ports_type_str[in_port_indx];
            if (nodes[i].inputs.input[indx].bit_size == 0 && nodes[i].inputs.input[indx].bitSizeWasZero)
                signal+="0";
            else if ( nodes[i].inputs.input[indx].bit_size - 1 >= 0 )
                    signal+=to_string(nodes[i].inputs.input[indx].bit_size - 1);
            else
                signal+=to_string(DEFAULT_BITWIDTH-1);
            //signal += to_string( ( nodes[i].inputs.input[indx].bit_size - 1 >= 0 ) ? nodes[i].inputs.input[indx].bit_size - 1 : DEFAULT_BITWIDTH-1 );
            signal += " downto 0);";
        }
        netlist << signal << endl ;
    }
    for ( indx = 0; indx < nodes[i].inputs.size; indx++ )
    {
        signal = '\t';
        signal += PVALID_ARRAY; //Valid
        signal += UNDERSCORE;
        signal += to_string( indx );
        signal += COLOUMN;
        signal += " in " ;
        signal += STD_LOGIC ;
        netlist << signal << endl ;
    }
    for ( indx = 0; indx < nodes[i].inputs.size; indx++ )
    {
        signal = '\t';
        signal += READY_ARRAY;
        signal += UNDERSCORE;
        signal += to_string( indx );
        signal += COLOUMN;
        signal += " out ";
        signal += STD_LOGIC ;
        netlist <<  signal  << endl;

    }

    //outputs Begin
    for ( indx = 0; indx < nodes[i].outputs.size; indx++ )
    {
        signal = '\t';
        signal += NREADY_ARRAY;
        signal += UNDERSCORE;
        signal += to_string( indx );
        signal += COLOUMN;
        signal += " in ";
        signal += STD_LOGIC ;
        netlist << signal << endl ;

        signal = '\t';
        signal += VALID_ARRAY;
        signal += UNDERSCORE;
        signal += to_string( indx );
        signal += COLOUMN;
        signal += " out ";
        signal += STD_LOGIC ;
        netlist << signal << endl ;

        for ( int out_port_indx = 0; out_port_indx < components_type[nodes[i].component_type].out_ports; out_port_indx++)
        {

            signal = '\t';
            signal += components_type[0].out_ports_name_str[out_port_indx];
            signal += UNDERSCORE;
            signal += to_string( indx );
            signal += COLOUMN;
            signal += " out " + components_type[0].out_ports_type_str[out_port_indx];
            if (nodes[i].outputs.output[indx].bit_size == 0 && nodes[i].outputs.output[indx].bitSizeWasZero)
                signal+="0";
            else if ( nodes[i].outputs.output[indx].bit_size - 1 >= 0 )
                signal+=to_string(nodes[i].outputs.output[indx].bit_size - 1);
            else
                signal+=to_string(DEFAULT_BITWIDTH-1);

            //signal += to_string( ( nodes[i].outputs.output[indx].bit_size-1 >=0 ) ? nodes[i].outputs.output[indx].bit_size - 1 : DEFAULT_BITWIDTH-1);
            signal += " downto 0);";

            netlist << signal << endl ;
        }
    }


    if ( nodes[i].type == "LSQ" )
    {
        signal = '\t';
        signal += "io_queueEmpty";
        signal += COLOUMN;
        ////////////////////////////////////////CHANGE HERE/////////////////////////////////////////////////////////
        signal += " out ";
        signal += STD_LOGIC ;
        netlist << signal << endl;
    }

    if ( nodes[i].type == "MC" )
    {
        signal = '\t';
        signal += "we0_ce0";
        signal += COLOUMN;
        //////////////////////////////////////////////////////CHANGE HERE/////////////////////////////////////////////
        signal += " out ";
        signal += STD_LOGIC ;
        netlist << signal << endl;

#if 1
        //Add additional ports to wrapper
        netlist << endl;             
        netlist << "\tdout0    : out std_logic_vector(" << nodes[node_id].data_size-1 << " downto 0);" << endl;
        netlist << "\taddress0    : out std_logic_vector(" << nodes[node_id].address_size-1 << " downto 0);" << endl;
        // netlist << "\tio_storeEnable : out std_logic;" << endl;
        netlist << "\tdin1 : in std_logic_vector(" << nodes[node_id].data_size-1 << " downto 0);" << endl;
        netlist << "\taddress1 : out std_logic_vector(" << nodes[node_id].address_size-1 << " downto 0);" << endl;
        netlist << "\tce1 : out std_logic;" << endl;

        // netlist << "\tio_bbpValids : in std_logic_vector(" << nodes[node_id].bbcount - 1 << " downto 0);" << endl;
        // netlist << "\tio_bb_stCountArray: in data_array (" << nodes[node_id].bbcount - 1 << " downto 0)(31 downto 0);" << endl;
        // netlist << "\tio_bbReadyToPrevs : out std_logic_vector(" << nodes[node_id].bbcount - 1 << " downto 0);" << endl;

        // netlist << "\tio_Empty_Valid: out STD_LOGIC;" << endl;
        // netlist << "\tio_Empty_Ready: in STD_LOGIC;" << endl;

        // netlist << "\tio_rdPortsPrev_valid : in  std_logic_vector(" << nodes[node_id].load_count - 1 << " downto 0);" << endl;
        // netlist << "\tio_rdPortsPrev_bits: in data_array (" << nodes[node_id].load_count - 1 << " downto 0)(" << nodes[node_id].address_size -1 << " downto 0);" << endl;
        // netlist << "\tio_rdPortsPrev_ready : out  std_logic_vector(" << nodes[node_id].load_count - 1 << " downto 0);" << endl;

        // netlist << "\tio_rdPortsNext_bits: out data_array (" << nodes[node_id].load_count - 1 << " downto 0)(" << nodes[node_id].data_size -1 << " downto 0);" << endl;
        // netlist << "\tio_rdPortsNext_valid : out  std_logic_vector(" << nodes[node_id].load_count - 1 << " downto 0);" << endl;
        // netlist << "\tio_rdPortsNext_ready: in  std_logic_vector(" << nodes[node_id].load_count - 1 << " downto 0);" << endl;

        // netlist << "\tio_wrAddrPorts_valid : in  std_logic_vector(" << nodes[node_id].store_count - 1 << " downto 0);" << endl;
        // netlist << "\tio_wrAddrPorts_bits: in data_array (" << nodes[node_id].store_count - 1 << " downto 0)(" << nodes[node_id].address_size -1 << " downto 0);" << endl;
        // netlist << "\tio_wrAddrPorts_ready : out  std_logic_vector(" << nodes[node_id].store_count - 1 << " downto 0);" << endl;

        // netlist << "\tio_wrDataPorts_valid : in  std_logic_vector(" << nodes[node_id].store_count - 1 << " downto 0);" << endl;
        // netlist << "\tio_wrDataPorts_bits: in data_array (" << nodes[node_id].store_count - 1 << " downto 0)(" << nodes[node_id].data_size -1 << " downto 0);" << endl;
        // netlist << "\tio_wrDataPorts_ready : out  std_logic_vector(" << nodes[node_id].store_count - 1 << " downto 0);" << endl;

        netlist << endl;
#endif

    }

    // Add a useless net which is connected to dummy logic. this helps rapidwrite to use add the component in design. This is crucial.
    // If rapidwright finds no logic in a component, then writeCheckpoint()nfails in null ptr error.
    if( nodes[i].type == "Source" )
    {
    	signal = '\t';
        signal += "useless_net";
        signal += COLOUMN;
        signal += " out ";
        signal += STD_LOGIC ;
        netlist << signal << endl;
    }

    //下面是我自己加的
    if ( nodes[i].component_operator == "load_op" )
    {
        netlist << "\t" << "data_from_memory : in std_logic_vector (31 downto 0);" << endl;
        netlist << "\t" << "read_enable : out std_logic;" << endl;
        netlist << "\t" << "read_address : out std_logic_vector (31 downto 0);" << endl;
    }
    if ( nodes[i].component_operator == "store_op" )
    {
        netlist << "\t" << "data_to_memory : out std_logic_vector (31 downto 0);" << endl;
        netlist << "\t" << "write_enable : out std_logic;" << endl;
        netlist << "\t" << "write_address : out std_logic_vector (31 downto 0);" << endl;
    }
    // if ( nodes[i].type == "Exit" )
    // {
    //     netlist << "\t" << "dataOutArray_0 : out std_logic_vector ("<<nodes[i].outputs.output[0].bit_size - 1<<" downto 0);" << endl;
    //     netlist << "\t" << "validArray_0 : out std_logic;" << endl;
    //     netlist << "\t" << "nReadyArray_0 : in std_logic;" << endl;
    // }



    netlist << "\t" << "clk: " << " in std_logic;" << endl;
    netlist << "\t" << "rst: " << " in std_logic" << endl;




    netlist << ");" << endl;
    netlist << "end " << output_vhdl_entityname << ";" << endl << endl;

}

//写VHDL文件内部封装elastic component的内容
void write_components_single (string output_vhdl_entityname, int node_id )
{
    string entity="";
    string generic="";
    string input_port="";
    string input_signal="";
    string output_port="";
    string output_signal="";

    int i = node_id;
    string entityName;

    netlist << endl;

    if ( nodes[i].type == "Operator" )
        entityName = nodes[i].component_operator;
    else
        entityName= get_component_entity ( nodes[i].component_operator, i );

    entity = entityName + "_sub" + ": entity work." + entityName;

    if ( nodes[i].type != "LSQ" )
    {
        entity += "(arch)";

        generic = " generic map (";
        generic += get_generic ( i );
        generic += ")";

        netlist << entity << generic << endl;
    }
    else
    {
        netlist << entity << endl;
    }

    netlist << "port map (" << endl;

    if ( nodes[i].type != "LSQ" )
    {
        netlist << "\t" << "clk => " <<"clk";
        netlist << COMMA << endl<< "\t" << "rst => " <<"rst";
    }
    else
    {
        netlist << "\t" << "clock => " <<  "clk";
        netlist << COMMA << endl<< "\t" << "reset => " <<  "rst";

    }
    int indx = 0;

    if( nodes[i].type == "MC")
    {

        netlist << COMMA;
        netlist << endl;
        netlist << endl;
        netlist << "\tio_bbReadyToPrevs(0) => readyArray_1," << endl;
        netlist << "\tio_bbpValids(0) => '0'," << endl;
        netlist << "\tio_bb_stCountArray(0) => x\"00000000\"," << endl;
        netlist << "\tio_rdPortsPrev_ready(0) => readyArray_0," << endl;
        netlist << "\tio_rdPortsPrev_valid(0) => pValidArray_0," << endl;
        netlist << "\tio_rdPortsPrev_bits(0) => dataInArray_0," << endl;
        netlist << "\tio_wrAddrPorts_ready(0) => readyArray_2," << endl;
        netlist << "\tio_wrAddrPorts_valid(0) => pValidArray_2," << endl;
        netlist << "\tio_wrAddrPorts_bits(0) => dataInArray_2," << endl;
        netlist << "\tio_wrDataPorts_ready(0) => readyArray_3," << endl;
        netlist << "\tio_wrDataPorts_valid(0) => pValidArray_3," << endl;
        netlist << "\tio_wrDataPorts_bits(0) => dataInArray_3," << endl;
        netlist << "\tio_rdPortsNext_ready(0) => nReadyArray_0," << endl;
        netlist << "\tio_rdPortsNext_valid(0) => validArray_0," << endl;
        netlist << "\tio_rdPortsNext_bits(0) => dataOutArray_0," << endl;
        netlist << "\tio_Empty_Valid => validArray_1," << endl;
        netlist << "\tio_Empty_Ready => nReadyArray_1," << endl;
        netlist << endl;

        netlist << "\tio_storeDataOut => dout0," << endl;
        netlist << "\tio_storeAddrOut => addr0," << endl;
        netlist << "\tio_storeEnable => we0_ce0," << endl;
        netlist << "\tio_loadDataIn => din1," << endl;
        netlist << "\tio_loadAddrOut => addr1," << endl;
        netlist << "\tio_loadEnable => ce1" << endl;        


        // netlist << "\tio_storeDataOut => io_storeDataOut," << endl;
        // netlist << "\tio_storeAddrOut => io_storeAddrOut," << endl;
        // netlist << "\tio_storeEnable => io_storeEnable," << endl;
        // netlist << "\tio_loadDataIn => io_loadDataIn," << endl;
        // netlist << "\tio_loadAddrOut => io_loadAddrOut," << endl;
        // netlist << "\tio_loadEnable => io_loadEnable," << endl;
        // netlist << endl;
        // netlist << "\tio_bbpValids => io_bbpValids," << endl;
        // netlist << "\tio_bb_stCountArray => io_bb_stCountArray," << endl;
        // netlist << "\tio_bbReadyToPrevs => io_bbReadyToPrevs," << endl;
        // netlist << endl;
        // netlist << "\tio_Empty_Valid => io_Empty_Valid," << endl;
        // netlist << "\tio_Empty_Ready => io_Empty_Ready," << endl;
        // netlist << endl;
        // netlist << "\tio_rdPortsPrev_valid => io_rdPortsPrev_valid," << endl;
        // netlist << "\tio_rdPortsPrev_bits => io_rdPortsPrev_bits," << endl;
        // netlist << "\tio_rdPortsPrev_ready => io_rdPortsPrev_ready," << endl;
        // netlist << endl;
        // netlist << "\tio_rdPortsNext_bits => io_rdPortsNext_bits," << endl;
        // netlist << "\tio_rdPortsNext_valid => io_rdPortsNext_valid," << endl;
        // netlist << "\tio_rdPortsNext_ready => io_rdPortsNext_ready," << endl;
        // netlist << endl;
        // netlist << "\tio_wrAddrPorts_valid => io_wrAddrPorts_valid," << endl;
        // netlist << "\tio_wrAddrPorts_bits => io_wrAddrPorts_bits," << endl;
        // netlist << "\tio_wrAddrPorts_ready => io_wrAddrPorts_ready," << endl;
        // netlist << endl;
        // netlist << "\tio_wrDataPorts_valid => io_wrDataPorts_valid," << endl;
        // netlist << "\tio_wrDataPorts_bits => io_wrDataPorts_bits," << endl;
        // netlist << "\tio_wrDataPorts_ready => io_wrDataPorts_ready" << endl; 
    }


    //下面的if的then块未处理

    if ( nodes[i].type == "LSQ" || nodes[i].type == "MC" )
    {
#if 0

        static int load_indx = 0;
        load_indx = 0;

        static int store_add_indx = 0;
        static int store_data_indx = 0;
        store_add_indx = 0;
        store_data_indx = 0;

        for ( int lsq_indx = 0; lsq_indx < nodes[i].inputs.size; lsq_indx++ )
        {
            //cout << "LSQ input "<< lsq_indx << " = " << nodes[i].inputs.input[lsq_indx].type << "port = " << nodes[i].inputs.input[lsq_indx].port << "info_type = " <<nodes[i].inputs.input[lsq_indx].info_type << endl;
        }

        for ( int lsq_indx = 0; lsq_indx < nodes[i].outputs.size; lsq_indx++ )
        {
            //cout << "LSQ output "<< lsq_indx << " = " << nodes[i].outputs.output[lsq_indx].type << "port = " << nodes[i].outputs.output[lsq_indx].port << "info_type = " <<nodes[i].outputs.output[lsq_indx].info_type << endl;
        }

        netlist << "," << endl;
        input_signal = nodes[i].memory;
        input_signal += UNDERSCORE;
        input_signal += "dout0";
        input_signal += COMMA;

        netlist << "\t" << "io_storeDataOut" << " => "   << input_signal << endl;

        input_signal = nodes[i].memory;
        input_signal += UNDERSCORE;
        input_signal += "address0";
        input_signal += COMMA;

        netlist << "\t" << "io_storeAddrOut" << " => "  << input_signal << endl;

        input_signal = nodes[i].name;
        input_signal += UNDERSCORE;
        input_signal += "we0_ce0";
        input_signal += COMMA;

        netlist << "\t" << "io_storeEnable"<< " => "  << input_signal << endl;

        input_signal = nodes[i].memory;
        input_signal += UNDERSCORE;
        input_signal += "din1";
        input_signal += COMMA;

        netlist << "\t" << "io_loadDataIn" << " => "  << input_signal << endl;

        input_signal = nodes[i].memory;
        input_signal += UNDERSCORE;
        input_signal += "address1";
        input_signal += COMMA;

        netlist << "\t" << "io_loadAddrOut"<< " => "  << input_signal  << endl;

        input_signal = nodes[i].memory;
        input_signal += UNDERSCORE;
        input_signal += "ce1";
        //input_signal += COMMA;

        netlist << "\t" << "io_loadEnable" << " => "  << input_signal;


        for ( int lsq_indx = 0; lsq_indx < nodes[i].inputs.size; lsq_indx++ )
        {
            cout << nodes[i].name;
            cout << " LSQ input "<< lsq_indx << " = " << nodes[i].inputs.input[lsq_indx].type << "port = " << nodes[i].inputs.input[lsq_indx].port << "info_type = " <<nodes[i].inputs.input[lsq_indx].info_type << endl;


            //if ( nodes[i].inputs.input[lsq_indx].type == "c" || (nodes[i].bbcount-- > 0 ) )
            if ( nodes[i].inputs.input[lsq_indx].type == "c" )
            {
                netlist << COMMA << endl;
                input_port = "io";
                input_port += UNDERSCORE;
                input_port += "bbpValids";
                //input_port += UNDERSCORE;
                if ( nodes[i].type == "MC" )
                {
                    input_port +="(";
                    input_port += to_string(nodes[i].inputs.input[lsq_indx].port);
                    input_port +=")";

                }
                else
                {
                    input_port += UNDERSCORE;
                    input_port += to_string(nodes[i].inputs.input[lsq_indx].port);

                }

                input_signal = nodes[i].name;
                input_signal += UNDERSCORE;
                input_signal += PVALID_ARRAY;
                input_signal += UNDERSCORE;
                input_signal += to_string(lsq_indx);
                input_signal += COMMA;

                netlist << "\t" << input_port << " => "  << input_signal << endl;


                input_port = "io";
                input_port += UNDERSCORE;
                input_port += "bbReadyToPrevs";
                //input_port += UNDERSCORE;
                if ( nodes[i].type == "MC" )  { input_port +="("; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); input_port +=")"; } else { input_port += UNDERSCORE; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); }

                input_signal = nodes[i].name;
                input_signal += UNDERSCORE;
                input_signal += READY_ARRAY;
                input_signal += UNDERSCORE;
                input_signal += to_string(lsq_indx);
                input_signal += COMMA;

                netlist << "\t" << input_port << " => "  << input_signal << endl;


                if ( nodes[i].type == "MC" )
                {
                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "bb_stCountArray";
                    //input_port += UNDERSCORE;
                    if ( nodes[i].type == "MC" )  { input_port +="("; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); input_port +=")"; } else { input_port += UNDERSCORE; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); }

                    input_signal = nodes[i].name;
                    input_signal += UNDERSCORE;
                    input_signal += DATAIN_ARRAY;
                    input_signal += UNDERSCORE;
                    input_signal += to_string(lsq_indx);
                    //input_signal += COMMA;

                    netlist << "\t" << input_port << " => "  << input_signal;
                }

            }
            else
            if ( nodes[i].inputs.input[lsq_indx].type == "l" )
            {
                netlist << COMMA << endl;
                //static int load_indx = 0;
                //io_rdPortsPrev_0_ready"

                if ( nodes[i].type == "LSQ" )
                {
                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "rdPortsPrev";
                    input_port += UNDERSCORE;
                    //input_port += to_string(load_indx);
                    input_port += to_string(nodes[i].inputs.input[lsq_indx].port);

                    input_port += UNDERSCORE;
                    input_port += "ready";
                }
                else
                {
                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "rdPortsPrev";
                    input_port += UNDERSCORE;
                    input_port += "ready";
                    input_port += "(";
                    input_port += to_string(load_indx);
                    input_port += ")";

                }
                input_signal = nodes[i].name;
                input_signal += UNDERSCORE;
                input_signal += READY_ARRAY;
                input_signal += UNDERSCORE;
                input_signal += to_string(lsq_indx);
                input_signal += COMMA;

                netlist << "\t" << input_port << " => "  << input_signal << endl;


                if ( nodes[i].type == "LSQ" )
                {
                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "rdPortsPrev";
                    input_port += UNDERSCORE;
                    input_port += to_string(load_indx);
                    input_port += UNDERSCORE;
                    input_port += "valid";
                }
                else
                {
                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "rdPortsPrev";
                    input_port += UNDERSCORE;
                    input_port += "valid";
                    input_port += "(";
                    input_port += to_string(load_indx);
                    input_port += ")";

                }
                input_signal = nodes[i].name;
                input_signal += UNDERSCORE;
                input_signal += PVALID_ARRAY;
                input_signal += UNDERSCORE;
                input_signal += to_string(lsq_indx);
                input_signal += COMMA;

                netlist << "\t" << input_port << " => "  << input_signal << endl;


                if ( nodes[i].type == "LSQ" )
                {
                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "rdPortsPrev";
                    input_port += UNDERSCORE;
                    input_port += to_string(load_indx);
                    input_port += UNDERSCORE;
                    input_port += "bits";
                }
                else
                {
                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "rdPortsPrev";
                    input_port += UNDERSCORE;
                    input_port += "bits";
                    input_port += "(";
                    input_port += to_string(load_indx);
                    input_port += ")";

                }
                input_signal = nodes[i].name;
                input_signal += UNDERSCORE;
                input_signal += DATAIN_ARRAY;
                input_signal += UNDERSCORE;
                input_signal += to_string(lsq_indx);
                //input_signal += COMMA;

                netlist << "\t" << input_port << " => "  << input_signal;

                load_indx++;


            }
            else
            if ( nodes[i].inputs.input[lsq_indx].type == "s" )
            {

                netlist << COMMA << endl;
                //static int store_add_indx = 0;
                //static int store_data_indx = 0;

                if ( nodes[i].type == "LSQ" )
                {
                    //"io_wrAddrPorts_0_ready"
                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "wr";
                    if ( nodes[i].inputs.input[lsq_indx].info_type == "a" )
                    {
                        input_port += "Addr";
                        input_port += "Ports";
                        input_port += UNDERSCORE;
                        input_port += to_string(store_add_indx);
                    }
                    else
                    {
                        input_port += "Data";

                        input_port += "Ports";
                        input_port += UNDERSCORE;
                        input_port += to_string(store_data_indx);
                    }


                    input_port += UNDERSCORE;
                    input_port += "valid";
                }
                else
                {
                    //"io_wrAddrPorts_0_ready"
                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "wr";
                    if ( nodes[i].inputs.input[lsq_indx].info_type == "a" )
                    {
                        input_port += "Addr";
                    }
                    else
                    {
                        input_port += "Data";
                    }

                    input_port += "Ports";
                    input_port += UNDERSCORE;
                    input_port += "valid";
                    input_port += "(";
                    input_port += to_string(store_data_indx);
                    input_port += ")";


                }

                input_signal = nodes[i].name;
                input_signal += UNDERSCORE;
                input_signal += PVALID_ARRAY;
                input_signal += UNDERSCORE;
                input_signal += to_string(lsq_indx);
                input_signal += COMMA;

                netlist << "\t" << input_port << " => "  << input_signal << endl;



                if ( nodes[i].type == "LSQ" )
                {

                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "wr";
                    if ( nodes[i].inputs.input[lsq_indx].info_type == "a" )
                    {
                        input_port += "Addr";
                        input_port += "Ports";
                        input_port += UNDERSCORE;
                        //if ( nodes[i].type == "MC" )  { input_port +="("; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); input_port +=")"; } else { input_port += UNDERSCORE; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); }
                        input_port += to_string(store_add_indx);

                    }
                    else
                    {
                        input_port += "Data";

                        input_port += "Ports";
                        input_port += UNDERSCORE;
                        //if ( nodes[i].type == "MC" )  { input_port +="("; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); input_port +=")"; } else { input_port += UNDERSCORE; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); }
                        input_port += to_string(store_data_indx);
                    }


                    input_port += UNDERSCORE;
                    input_port += "ready";
                }
                else
                {
                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "wr";
                    if ( nodes[i].inputs.input[lsq_indx].info_type == "a" )
                    {
                        input_port += "Addr";
                        input_port += "Ports";
                        input_port += UNDERSCORE;
                        input_port += "ready";
                        input_port += "(";
                        input_port += to_string(store_add_indx);
                        input_port += ")";
                    }
                    else
                    {
                        input_port += "Data";
                        input_port += "Ports";
                        input_port += UNDERSCORE;
                        input_port += "ready";
                        input_port += "(";
                        input_port += to_string(store_data_indx);
                        input_port += ")";
                    }

                }

                input_signal = nodes[i].name;
                input_signal += UNDERSCORE;
                input_signal += READY_ARRAY;
                input_signal += UNDERSCORE;
                input_signal += to_string(lsq_indx);
                input_signal += COMMA;

                netlist << "\t" << input_port << " => "  << input_signal << endl;



                if ( nodes[i].type == "LSQ" )
                {

                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "wr";
                    if ( nodes[i].inputs.input[lsq_indx].info_type == "a" )
                    {
                        input_port += "Addr";
                        input_port += "Ports";
                        input_port += UNDERSCORE;
                        //if ( nodes[i].type == "MC" )  { input_port +="("; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); input_port +=")"; } else { input_port += UNDERSCORE; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); }
                        input_port += to_string(store_add_indx);
                        store_add_indx++;


                    }
                    else
                    {
                        input_port += "Data";
                        input_port += "Ports";
                        input_port += UNDERSCORE;
                        //if ( nodes[i].type == "MC" )  { input_port +="("; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); input_port +=")"; } else { input_port += UNDERSCORE; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); }
                        input_port += to_string(store_data_indx);
                        store_data_indx++;


                    }

                    input_port += UNDERSCORE;
                    input_port += "bits";

                }
                else
                {
                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "wr";
                    if ( nodes[i].inputs.input[lsq_indx].info_type == "a" )
                    {
                        input_port += "Addr";
                        input_port += "Ports";
                        input_port += UNDERSCORE;
                        input_port += "bits";
                        input_port += "(";
                        input_port += to_string(store_add_indx);
                        input_port += ")";
                        store_add_indx++;
                    }
                    else
                    {
                        input_port += "Data";
                        input_port += "Ports";
                        input_port += UNDERSCORE;
                        input_port += "bits";
                        input_port += "(";
                        input_port += to_string(store_data_indx);
                        input_port += ")";
                        store_data_indx++;

                    }
                }


                input_signal = nodes[i].name;
                input_signal += UNDERSCORE;
                input_signal += DATAIN_ARRAY;
                input_signal += UNDERSCORE;
                input_signal += to_string(lsq_indx);
                //input_signal += COMMA;

                netlist << "\t" << input_port << " => "  << input_signal;



            }

        }

        for ( int lsq_indx = 0; lsq_indx < nodes[i].outputs.size; lsq_indx++ )
        {
            //cout << "LSQ output "<< lsq_indx << " = " << nodes[i].outputs.output[lsq_indx].type << "port = " << nodes[i].outputs.output[lsq_indx].port << "info_type = " <<nodes[i].outputs.output[lsq_indx].info_type << endl;

            if ( nodes[i].outputs.output[lsq_indx].type == "c" )
            {
                netlist << COMMA << endl;
                input_port = "io";
                input_port += UNDERSCORE;
                input_port += "bbValids";
                //input_port += UNDERSCORE;
                if ( nodes[i].type == "MC" )  { input_port +="("; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); input_port +=")"; } else { input_port += UNDERSCORE; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); }

                input_signal = nodes[i].name;
                input_signal += UNDERSCORE;
                input_signal += VALID_ARRAY;
                input_signal += UNDERSCORE;
                input_signal += to_string(lsq_indx);
                input_signal += COMMA;

                netlist << "\t" << input_port << " => "  << input_signal << endl;


                input_port = "io";
                input_port += UNDERSCORE;
                input_port += "bbReadyToNexts";
                input_port += UNDERSCORE;
                if ( nodes[i].type == "MC" )  { input_port +="("; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); input_port +=")"; } else { input_port += UNDERSCORE; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); }

                input_signal = nodes[i].name;
                input_signal += UNDERSCORE;
                input_signal += NREADY_ARRAY;
                input_signal += UNDERSCORE;
                input_signal += to_string(lsq_indx);
                //input_signal += COMMA;

                netlist << "\t" << input_port << " => "  << input_signal;


            }
            else
            if ( nodes[i].outputs.output[lsq_indx].type == "l" )
            {
                //static int load_indx = 0;

                netlist << COMMA << endl;

                if ( nodes[i].type == "LSQ" )
                {

                    //io_rdPortsPrev_0_ready"
                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "rdPortsNext";
                    input_port += UNDERSCORE;
                    //if ( nodes[i].type == "MC" )  { input_port +="("; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); input_port +=")"; } else { input_port += UNDERSCORE; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); }
                    //input_port += to_string(load_indx);
                    input_port += to_string(nodes[i].inputs.input[lsq_indx].port);

                    input_port += UNDERSCORE;
                    input_port += "ready";
                }
                else
                {
                    //io_rdPortsPrev_0_ready"
                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "rdPortsNext";
                    input_port += UNDERSCORE;
                    input_port += "ready";
                    input_port += "(";
                    input_port += to_string(nodes[i].inputs.input[lsq_indx].port);
                    input_port += ")";

                }
                input_signal = nodes[i].name;
                input_signal += UNDERSCORE;
                input_signal += NREADY_ARRAY;
                input_signal += UNDERSCORE;
                input_signal += to_string(lsq_indx);
                input_signal += COMMA;

                netlist << "\t" << input_port << " => "  << input_signal << endl;



                if ( nodes[i].type == "LSQ" )
                {

                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "rdPortsNext";
                    input_port += UNDERSCORE;
                    //if ( nodes[i].type == "MC" )  { input_port +="("; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); input_port +=")"; } else { input_port += UNDERSCORE; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); }
                    input_port += to_string(nodes[i].inputs.input[lsq_indx].port);

                    input_port += UNDERSCORE;
                    input_port += "valid";
                }
                else
                {
                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "rdPortsNext";
                    input_port += UNDERSCORE;
                    input_port += "valid";
                    input_port += "(";
                    input_port += to_string(nodes[i].inputs.input[lsq_indx].port);
                    input_port += ")";

                }
                input_signal = nodes[i].name;
                input_signal += UNDERSCORE;
                input_signal += VALID_ARRAY;
                input_signal += UNDERSCORE;
                input_signal += to_string(lsq_indx);
                input_signal += COMMA;

                netlist << "\t" << input_port << " => "  << input_signal << endl;


                if ( nodes[i].type == "LSQ" )
                {

                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "rdPortsNext";
                    input_port += UNDERSCORE;
                    //if ( nodes[i].type == "MC" )  { input_port +="("; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); input_port +=")"; } else { input_port += UNDERSCORE; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); }
                    input_port += to_string(nodes[i].inputs.input[lsq_indx].port);

                    input_port += UNDERSCORE;
                    input_port += "bits";
                }
                else
                {
                    input_port = "io";
                    input_port += UNDERSCORE;
                    input_port += "rdPortsNext";
                    input_port += UNDERSCORE;
                    input_port += "bits";
                    input_port += "(";
                    input_port += to_string(nodes[i].inputs.input[lsq_indx].port);
                    input_port += ")";

                }
                input_signal = nodes[i].name;
                input_signal += UNDERSCORE;
                input_signal += DATAOUT_ARRAY;
                input_signal += UNDERSCORE;
                input_signal += to_string(lsq_indx);
                //input_signal += COMMA;

                netlist << "\t" << input_port << " => "  << input_signal;
                load_indx++;


            }
            else
            if ( nodes[i].outputs.output[lsq_indx].type == "s" )
            {
                netlist << COMMA << endl;
                static int store_indx = 0;

                input_port = "io";
                input_port += UNDERSCORE;
                input_port += "wrpValids";
                input_port += UNDERSCORE;
                //if ( nodes[i].type == "MC" )  { input_port +="("; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); input_port +=")"; } else { input_port += UNDERSCORE; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); }
                input_port += to_string(store_indx);


                input_signal = nodes[i].name;
                input_signal += UNDERSCORE;
                input_signal += VALID_ARRAY;
                input_signal += UNDERSCORE;
                input_signal += to_string(lsq_indx);
                input_signal += COMMA;

                netlist << "\t" << input_port << " => "  << input_signal << endl;


                input_port = "io";
                input_port += UNDERSCORE;
                input_port += "wrReadyToPrevs";
                input_port += UNDERSCORE;
                //if ( nodes[i].type == "MC" )  { input_port +="("; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); input_port +=")"; } else { input_port += UNDERSCORE; input_port += to_string(nodes[i].inputs.input[lsq_indx].port); }
                input_port += to_string(store_indx);

                input_signal = nodes[i].name;
                input_signal += UNDERSCORE;
                input_signal += NREADY_ARRAY;
                input_signal += UNDERSCORE;
                input_signal += to_string(lsq_indx);
                //input_signal += COMMA;

                netlist << "\t" << input_port << " => "  << input_signal;

                store_indx++;

            }
            else
            if ( nodes[i].outputs.output[lsq_indx].type == "e" )
            {

                netlist << COMMA << endl;
                static int store_indx = 0;

                input_port = "io";
                input_port += UNDERSCORE;
                input_port += "Empty_Valid";


                input_signal = nodes[i].name;
                input_signal += UNDERSCORE;
                input_signal += VALID_ARRAY;
                input_signal += UNDERSCORE;
                input_signal += to_string(lsq_indx);
                input_signal += COMMA;

                netlist << "\t" << input_port << " => "  << input_signal << endl;


                input_port = "io";
                input_port += UNDERSCORE;
                input_port += "Empty_Ready";

                input_signal = nodes[i].name;
                input_signal += UNDERSCORE;
                input_signal += NREADY_ARRAY;
                input_signal += UNDERSCORE;
                input_signal += to_string(lsq_indx);
                //input_signal += COMMA;

                netlist << "\t" << input_port << " => "  << input_signal;

                store_indx++;

            }


        }

    //             input_signal = nodes[i].name;
    //             input_signal += UNDERSCORE;
    //             input_signal += "io_queueEmpty";
    //
    //
    //             netlist << "\t" << "io_queueEmpty" << " => " << input_signal << endl;
#endif
    }
    else
    if ( nodes[i].type == "Exit" )
    {
        for ( indx = 0; indx < nodes[i].inputs.size; indx++ )
        {
            if ( nodes[i].inputs.input[indx].type != "e" )
            {
                input_port = components_type[0].in_ports_name_str[0];
                input_port += "(";
                input_port += to_string(indx - get_memory_inputs(i));
                input_port += ")";
                if (nodes[i].inputs.input[indx].bit_size==0 && nodes[node_id].inputs.input[indx].bitSizeWasZero)
                {
                    input_signal="\"0\"";
                } else
                {
                    input_signal = components_type[nodes[i].component_type].in_ports_name_str[0];
                    input_signal += UNDERSCORE;
                    input_signal += to_string(indx);
                }

                netlist << COMMA << endl << "\t" << input_port << " => " << input_signal;
            }
        }
        for ( indx = 0; indx < nodes[i].inputs.size; indx++ )
        {

            if ( nodes[i].inputs.input[indx].type != "e" )
            {
                //Write the Ready ports
                input_port = PVALID_ARRAY;
                input_port += "(";
                input_port += to_string(indx- get_memory_inputs(i));
                input_port += ")";
            }
            else
            {
                //Write the Ready ports
                input_port = "eValidArray";
                input_port += "(";
                input_port += to_string(indx);
                input_port += ")";
            }
            input_signal = PVALID_ARRAY;
            input_signal += UNDERSCORE;
            input_signal += to_string(indx);

            netlist << COMMA << endl << "\t" << input_port << " => " << input_signal;
        }
        for ( indx = 0; indx < nodes[i].inputs.size; indx++ )
        {
            if ( nodes[i].inputs.input[indx].type != "e" )
            {
                //Write the Ready ports
                input_port = READY_ARRAY;
                input_port += "(";
                input_port += to_string(indx- get_memory_inputs(i));
                input_port += ")";
            }
            else
            {
                //Write the Ready ports
                input_port = "eReadyArray";
                input_port += "(";
                input_port += to_string(indx);
                input_port += ")";

            }

            input_signal = READY_ARRAY;
            input_signal += UNDERSCORE;
            input_signal += to_string(indx);

            netlist << COMMA << endl << "\t" << input_port << " => " << input_signal;
        }
        //下面这一行是我加的 待定
        //for ( indx = 0; indx < nodes[i].outputs.size; indx++ )
        {

            input_port = components_type[0].out_ports_name_str[0];
            input_port += "(";
            input_port += "0";
            input_port += ")";

            input_signal = components_type[nodes[i].component_type].out_ports_name_str[0];
            input_signal += UNDERSCORE;
            input_signal += "0";



            netlist << COMMA << endl << "\t" << input_port << " => " << input_signal;


            input_port = VALID_ARRAY;
            input_port += "(";
            input_port += "0";
            input_port += ")";

            input_signal = VALID_ARRAY;
            input_signal += UNDERSCORE;
            input_signal += "0";


            netlist << COMMA << endl << "\t" << input_port << " => " << input_signal;

            input_port = NREADY_ARRAY;
            input_port += "(";
            input_port += "0";
            input_port += ")";

            input_signal = NREADY_ARRAY;
            input_signal += UNDERSCORE;
            input_signal += "0";

            netlist << COMMA << endl << "\t" << input_port << " => " << input_signal;
        }
    }
    else
    {
        for ( indx = 0; indx < nodes[i].inputs.size; indx++ )
        {
            int in_port_indx = 0;

            if ( ( nodes[i].type.find("Branch") != std::string::npos && indx == 1 ) || ( (nodes[i].component_operator.find("select") != std::string::npos ) && indx == 0 ) || ( (nodes[i].component_operator.find("Mux") != std::string::npos ) && indx == 0 ))
            {
                input_port = "Condition(0)";
            }
            else if ( ( ( nodes[i].name.find("store") != std::string::npos ) || ( nodes[i].name.find("load") != std::string::npos ) ) && indx == 1 )
            {
                input_port = "input_addr";
            }
            else
            {
                input_port = components_type[0].in_ports_name_str[in_port_indx];
                input_port += "(";
                if ( ( nodes[i].component_operator.find("select") != std::string::npos ) || ( (nodes[i].component_operator.find("Mux") != std::string::npos ) ) )
                {
                    input_port += to_string( indx - 1 );
                }
                else
                {
                    input_port += to_string( indx );
                }
                input_port += ")";
            }

            if (nodes[i].inputs.input[indx].bit_size==0 && nodes[node_id].inputs.input[indx].bitSizeWasZero )
            {
                input_signal="\"0\"";
            } else {
                input_signal = components_type[nodes[i].component_type].in_ports_name_str[in_port_indx];
                input_signal += UNDERSCORE;
                input_signal += to_string(indx);
            }

            if ( nodes[i].type.find("Constant") != std::string::npos )
            {
                input_signal = "\"";
                input_signal += string_constant ( nodes[i].component_value , nodes[i].inputs.input[0].bit_size );
                input_signal += "\"";
            }
            netlist << COMMA << endl << "\t" << input_port << " => " << input_signal;
        }

        for ( indx = 0; indx < nodes[i].inputs.size; indx++ )
        {
            input_port = PVALID_ARRAY;
            input_port += "(";
            input_port += to_string( indx );
            input_port += ")";

            input_signal = PVALID_ARRAY;
            input_signal += UNDERSCORE;
            input_signal += to_string( indx );

            netlist << COMMA << endl << "\t" << input_port << " => " << input_signal;
        }

        for ( indx = 0; indx < nodes[i].inputs.size; indx++ )
        {
            input_port = READY_ARRAY;
            input_port += "(";
            input_port += to_string( indx );
            input_port += ")";

            input_signal = READY_ARRAY;
            input_signal += UNDERSCORE;
            input_signal += to_string( indx );

            netlist << COMMA << endl << "\t" << input_port << " => " << input_signal;
        }

        if ( nodes[i].component_operator == "load_op" )
        {
            input_port = "read_enable";
            input_signal =  "read_enable";
            netlist << COMMA << endl << "\t" << input_port << " => " << input_signal;

            input_port = "read_address";
            input_signal =  "read_address";
            netlist << COMMA << endl << "\t" << input_port << " => " << input_signal;

            input_port = "data_from_memory";
            input_signal =  "data_from_memory";
            netlist << COMMA << endl << "\t" << input_port << " => " << input_signal;
        }

        if ( nodes[i].component_operator == "store_op" )
        {
            input_port = "write_enable";
            input_signal =  "write_enable";
            netlist << COMMA << endl << "\t" << input_port << " => " << input_signal;

            input_port = "write_address";
            input_signal =  "write_address";
            netlist << COMMA << endl << "\t" << input_port << " => " << input_signal;

            input_port = "data_to_memory";
            input_signal =  "data_to_memory";
            netlist << COMMA << endl << "\t" << input_port << " => " << input_signal;
        }

        for ( indx = 0; indx < nodes[i].outputs.size; indx++ )
        {
            input_port = NREADY_ARRAY;
            input_port += "(";
            input_port += to_string( indx );
            input_port += ")";

            input_signal = NREADY_ARRAY;
            input_signal += UNDERSCORE;
            input_signal += to_string( indx );

            netlist << COMMA << endl << "\t" << input_port << " => " << input_signal;
        }
        for ( indx = 0; indx < nodes[i].outputs.size; indx++ )
        {
            input_port = VALID_ARRAY;
            input_port += "(";
            input_port += to_string( indx );
            input_port += ")";

            input_signal = VALID_ARRAY;
            input_signal += UNDERSCORE;
            input_signal += to_string( indx );

            netlist << COMMA << endl << "\t" << input_port << " => " << input_signal;
        }
        for ( indx = 0; indx < nodes[i].outputs.size; indx++ )
        {

            for ( int out_port_indx = 0; out_port_indx < components_type[nodes[i].component_type].out_ports; out_port_indx++)
            {


                if ( ( nodes[i].type.find(COMPONENT_CTRLMERGE) != std::string::npos && indx == 1 ) )
                {
                    output_port = "Condition(0)";
                }
                else if ( ( ( nodes[i].name.find("store") != std::string::npos ) || ( nodes[i].name.find("load") != std::string::npos ) ) && indx == 1 )
                {
                    output_port = "output_addr";
                }
                else
                {
                    output_port = components_type[nodes[i].component_type].out_ports_name_str[out_port_indx];
                    output_port += "(";
                    output_port += to_string( indx );
                    output_port += ")";
                }

                output_signal = components_type[nodes[i].component_type].out_ports_name_str[out_port_indx];
                output_signal += UNDERSCORE;
                output_signal += to_string( indx );
                netlist << COMMA << endl << "\t" << output_port << " => " << output_signal;
            }
        }

    }
    netlist << endl << ");" << endl;

    if( nodes[i].type == "MC" )
    {
            netlist << "address0 <= std_logic_vector(resize(unsigned(addr0), address0'length));" << endl;
            netlist << "address1 <= std_logic_vector(resize(unsigned(addr1), address1'length));" << endl;
    }
    // Add dummy logic to useless net. This logic doesn't perform any usefull function.
    // It is required by rapidwright to add the component in design
    if(  nodes[i].type == "Source" )
    {
    	string dummy = "";
    	dummy = '\t';
        dummy += "useless_net";
        dummy += " <= ";
        dummy += "not rst;";
        netlist << dummy << endl;
    }

}
void write_vhdl_normal(string output_vhdl_entityname, int node_id ){
    write_intro ( );

    write_entity_single ( output_vhdl_entityname, node_id );

    netlist << "architecture behavioral of " << output_vhdl_entityname << " is " << endl;

    if(nodes[node_id].type == "MC")
    {
            netlist << "\n";
            netlist << "\tsignal addr0: std_logic_vector(31 downto 0); " << endl;
            netlist << "\tsignal addr1: std_logic_vector(31 downto 0); " << endl;
    }
    netlist  << endl << "begin" << endl;

    write_components_single(output_vhdl_entityname, node_id);

    netlist  << endl << "end behavioral; "<< endl;
}
void write_vhdl_icmp_for_subBuffer()
{
    netlist<<endl<<"-- ==============================================================\n"
                   "library IEEE; \n"
                   "use IEEE.std_logic_1164.all; \n"
                   "use IEEE.numeric_std.all; \n"
                   "use work.customTypes.all; \n"
                   "-- ==============================================================\n"
                   "entity Buffer_1_1_1_1_s is \n"
                   "port (\n"
                   "\tdataInArray_0 :  in std_logic_vector(0 downto 0);\n"
                   "\tpValidArray_0 :  in std_logic;\n"
                   "\treadyArray_0 :  out std_logic;\n"
                   "\tnReadyArray_0 :  in std_logic;\n"
                   "\tvalidArray_0 :  out std_logic;\n"
                   "\tdataOutArray_0 :  out std_logic_vector(0 downto 0);\n"
                   "\tclk:  in std_logic;\n"
                   "\trst:  in std_logic\n"
                   ");\n"
                   "end Buffer_1_1_1_1_s;\n"
                   "\n"
                   "architecture behavioral of Buffer_1_1_1_1_s is \n"
                   "\n"
                   "begin\n"
                   "\n"
                   "elasticBuffer_sub: entity work.elasticBuffer(arch) generic map (1,1,1,1)\n"
                   "port map (\n"
                   "\tclk => clk,\n"
                   "\trst => rst,\n"
                   "\tdataInArray(0) => dataInArray_0,\n"
                   "\tpValidArray(0) => pValidArray_0,\n"
                   "\treadyArray(0) => readyArray_0,\n"
                   "\tnReadyArray(0) => nReadyArray_0,\n"
                   "\tvalidArray(0) => validArray_0,\n"
                   "\tdataOutArray(0) => dataOutArray_0\n"
                   ");\n"
                   "\n"
                   "end behavioral; "<<endl;
}

void write_vhdl_icmp(string output_vhdl_entityname, int node_id){

    string entityname_icmp=output_vhdl_entityname+"_s";
    write_vhdl_normal(entityname_icmp, node_id);
    write_vhdl_icmp_for_subBuffer();

    write_intro ( );
    write_entity_single (output_vhdl_entityname , node_id );
    netlist << "architecture behavioral of " << output_vhdl_entityname << " is " << endl;
    netlist << "\tsignal\ti_nReadyArray_0 :   std_logic;\n"
               "\tsignal\ti_validArray_0 :   std_logic;\n"
               "\tsignal\ti_dataOutArray_0 : std_logic_vector(0 downto 0);\n"
               "\tsignal  i_dataInArray_0 :   std_logic_vector(0 downto 0);\n"
               "\tsignal  i_pValidArray_0 :   std_logic;\n"
               "\tsignal  i_readyArray_0 :    std_logic;"<<endl;
    netlist  << endl << "begin" << endl;
    netlist <<entityname_icmp<<": entity work."<<entityname_icmp<<"(behavioral)\n"
              "port map (\n"
              "\tclk => clk,\n"
              "\trst => rst,\n"
              "\tdataInArray_0 => dataInArray_0,\n"
              "\tdataInArray_1 => dataInArray_1,\n"
              "\tpValidArray_0 => pValidArray_0,\n"
              "\tpValidArray_1 => pValidArray_1,\n"
              "\treadyArray_0 => readyArray_0,\n"
              "\treadyArray_1 => readyArray_1,\n"
              "\tnReadyArray_0 => i_nReadyArray_0,\n"
              "\tvalidArray_0 => i_validArray_0,\n"
              "\tdataOutArray_0 => i_dataOutArray_0\n"
              ");\n"
              "Buffer_1_1_1_1_s: entity work.Buffer_1_1_1_1_s(behavioral) \n"
              "port map (\n"
              "\tclk => clk,\n"
              "\trst => rst,\n"
              "\tdataInArray_0 => i_dataInArray_0,\n"
              "\tpValidArray_0 => i_pValidArray_0,\n"
              "\treadyArray_0 => i_readyArray_0,\n"
              "\tnReadyArray_0 => nReadyArray_0,\n"
              "\tvalidArray_0 => validArray_0,\n"
              "\tdataOutArray_0 => dataOutArray_0\n"
              ");\n"
              "\n"
              "\ti_nReadyArray_0 <= i_readyArray_0;\n"
              "\ti_validArray_0 <= i_pValidArray_0;\n"
              "\ti_dataOutArray_0 <= i_dataInArray_0;\n"
              "end behavioral; "<<endl;

}

void vhdl_writer::write_vhdl_of_each_components ( int node_id )
{
    const string outputDir = "../vhdlFiles/";

    string output_vhdl_entityname;
    output_vhdl_entityname=nodes[node_id].component_operator + UNDERSCORE + get_generic_UNDERSCORE(node_id);

    if ( nodes[node_id].type.find("Constant") != std::string::npos )
        output_vhdl_entityname += UNDERSCORE + string_constant(nodes[node_id].component_value , nodes[node_id].inputs.input[0].bit_size );

    string output_vhdl_filename = outputDir + output_vhdl_entityname +".vhd";

    cout << "writing: " << output_vhdl_filename << endl;

    if ( nodes[node_id].component_operator.find("icmp") != std::string::npos )
        cout<<"Note: To avoid routing failed, a elastic buffer would be added after the output of "<< output_vhdl_entityname <<endl;


    components_type[COMPONENT_GENERIC].in_ports = 2;
    components_type[COMPONENT_GENERIC].out_ports = 1;
    components_type[COMPONENT_GENERIC].in_ports_name_str = in_ports_name_generic;
    components_type[COMPONENT_GENERIC].in_ports_type_str = in_ports_type_generic;
    components_type[COMPONENT_GENERIC].out_ports_name_str = out_ports_name_generic;
    components_type[COMPONENT_GENERIC].out_ports_type_str = out_ports_type_generic;

    components_type[COMPONENT_CONSTANT].in_ports = 1;
    components_type[COMPONENT_CONSTANT].out_ports = 1;
    components_type[COMPONENT_CONSTANT].in_ports_name_str = in_ports_name_generic;
    components_type[COMPONENT_CONSTANT].in_ports_type_str = in_ports_type_generic;
    components_type[COMPONENT_CONSTANT].out_ports_name_str = out_ports_name_generic;
    components_type[COMPONENT_CONSTANT].out_ports_type_str = out_ports_type_generic;

    netlist.open (output_vhdl_filename);
        
    if ( nodes[node_id].component_operator.find("icmp") != std::string::npos )
        write_vhdl_icmp(output_vhdl_entityname, node_id);
    else
        write_vhdl_normal(output_vhdl_entityname, node_id);

    netlist.close();
}

