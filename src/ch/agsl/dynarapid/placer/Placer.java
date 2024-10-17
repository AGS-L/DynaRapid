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
//This provides the interface for the placer that is used in this tool
//All placers must implement this placer
//have a constructor for initialising the design.
import com.xilinx.rapidwright.device.Site;

import java.io.*;
import java.util.*;
import java.lang.*;

public interface Placer {

    //This should return the name of the placer
    public String getPlacerName();

    //This sets the center of the design based on the name of the site that is sent
    //returns true or false based on whether the center could be set or not
    public  boolean setDesignCenterUsingSiteName(String siteName);

    //This sets the center of the design based on the i and j coordintaes of the graph
    //returns true or false based on whether the center could be set
    //Here the side is -1 for left and +1 for right side
    //This is supposed to automaticaly take the first site of the given tile
    public boolean setDesignCenterUsingCoordinates(int i, int j, int side);

    //This returns the coordinates of the center of the design in the format [centeri, centerj, side]
    public int[] getDesignCenterCoordinates();

    //This returns the site of the center of the desihn
    public Site getDesignCenterSite();
    
    //This is the main placer algorithm. This takes in a unmodified graph from the graph-generator and fills the graph-placer fields.
    public Map<String, Node> placer(Map<String, Node> nodes);

    //This function is supposed to return the node fabric where each cell in the DDA is either null or the node which is to occupy that cell.
    //This fabirc needs to be a non-pverlapped one.
    public Node[][] getNodeFabric();
}
