#include "dot_parser.h"
#include "vhdl_writer.h"

#include <iostream>
#include <string.h>

using namespace std;

#define PART_NAME "xcvu13p-fsga2577-1-i"
#define CLK_PERIOD "0.500" //time period in ns


string dot_filename;

/*
 * Forces the output bit width to match the input bit width of next component
 */
void fix_interface_sizes(int node_id)
{

    if ( nodes[node_id].type.find("Branch") != std::string::npos )
    {
        int next_node = nodes[node_id].outputs.output[0].next_nodes_id;
        int next_port = nodes[node_id].outputs.output[0].next_nodes_port;
        nodes[node_id].outputs.output[0].bit_size = nodes[next_node].inputs.input[next_port].bit_size;
    }
    else if ( nodes[node_id].type.find("Buf") != std::string::npos )
    {
        int next_node = nodes[node_id].outputs.output[0].next_nodes_id;
        int next_port = nodes[node_id].outputs.output[0].next_nodes_port;
        nodes[node_id].outputs.output[0].bit_size = nodes[next_node].inputs.input[next_port].bit_size;
    }
    else if ( nodes[node_id].type.find("Constant") != std::string::npos )
    {
        int next_node = nodes[node_id].outputs.output[0].next_nodes_id;
        int next_port = nodes[node_id].outputs.output[0].next_nodes_port;
        nodes[node_id].outputs.output[0].bit_size = nodes[next_node].inputs.input[next_port].bit_size;
    }
}

int main(int argc, char* argv[] ) {
    vhdl_writer vhdl_writer;

    dot_filename = argv[1];
    parse_dot (dot_filename);
    
    cout << "Writing VHDL files for all nodes in dot file" << endl;
    for (int i = 0; i <components_in_netlist; i++)
    {
        cout << "Component: " << nodes[i].name << endl;
        //fix_interface_sizes(i);
        vhdl_writer.write_vhdl_of_each_components(i);
    }
    
    return 0;
}
