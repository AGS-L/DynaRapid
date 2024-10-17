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
#include <string>

#ifndef _DOT_PARSER_
#define _DOT_PARSER_


using namespace std;

#define COMMENT_CHARACTER '/'


#define MAX_INPUTS  512//64
#define MAX_OUTPUTS 512// 64

#define COMPONENT_GENERIC   0
#define COMPONENT_CONSTANT  1

#define COMPONENT_DESCRIPTION_LINE  0
#define COMPONENT_CONNECTION_LINE  1



#define FALSE   0
#define TRUE    1

#define COMPONENT_NOT_FOUND -1 

typedef struct input
{
    int bit_size;
    int prev_nodes_id = COMPONENT_NOT_FOUND;
    string type;
    int port;
    string info_type;
    bool bitSizeWasZero=false;
} INPUT_T;

typedef struct in
{
    int size;
    INPUT_T input[MAX_OUTPUTS];
} IN_T;

typedef struct output
{
    int bit_size; //width of the channel
    int next_nodes_id = COMPONENT_NOT_FOUND; //bbid of the node to which the channel is connected to
    int next_nodes_port = 0; //The port number of the next node to which the channel is connected to
    string type; //Type of the node to which this channel is connected to
    int port; //port number to which the current node is connected to
    string info_type;
    bool bitSizeWasZero=false; //Weather a data node or control node
}OUTPUT_T;

typedef struct out
{
    int size;
    OUTPUT_T output[MAX_OUTPUTS];
} OUT_T;


typedef struct node
{
    string  name; //name
    string  type; //type
    IN_T    inputs; //arraylist
    OUT_T   outputs; //arraylist
    string  parameters; //params
    int     node_id; //nodeID
    int     component_type; //compType
    string  component_operator; //compOperator
    unsigned long int component_value; //compValue
    bool    component_control; //compControl
    int     slots; //slots
    bool    trasparent; //transparent
    string  memory; //mem
    int     bbcount = -1; //bbCount
    int     load_count = -1; //loadCount
    int     store_count = -1; //storeCount
    int     data_size = 32; //dataSize
    int     address_size = 32; //addressSize
    bool    mem_address; //memAddress
    int     bbId = -1; //bbID
    int     portId = -1; //portID
    int     offset = -1; //offset
} NODE_T;


#define MAX_NODES 16384//4096

void parse_dot ( string filename );

extern NODE_T nodes[MAX_NODES];

extern int components_in_netlist;


#endif
