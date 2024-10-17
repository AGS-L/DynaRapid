/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.map;
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

import java.util.*;
import java.io.*;

//This class stores the amount of resources available from a given starting point upto that position in the map
public class ResourceElement implements Serializable
{
    public static int starti = 60, startj = 0, endi = 89, endj = 147;  //Cordinates of the starting and ending tiles of the fpga where the pblock can be created
    //START: CLEL_R_X0Y899 and END: CLEM_R_X147Y870
    public static ArrayList<ArrayList<ResourceElement>> ResourceMap = new ArrayList<>();

    public int clel, clem, dsp, bram;
    public int curri, currj;
    public int row, col;

    public ResourceElement(int i, int j, ArrayList<ArrayList<ResourceElement>> tempMap) //For map ResourceElement
    {
        //Here the resourceElement starts from the starti and startj as the top-left location in map and the curri and currj as the bottom-right location in the map
        //Here the rows and columns is the number of rows and columns in the resourceELement
        curri = i; 
        currj = j;

        row = curri - starti + 1; 
        col = currj - startj + 1;

        modifyResourceElement(tempMap);
    }

    public ResourceElement(int l, int m, int d, int b, int i, int j, int r, int c) //for pblock resource element
    {
        //Here the resourceElement starts from the curri and currj as the top-left cell in the map and the row and col as the number of rows and columns in the given pblock
        clel = l;
        clem = m;
        dsp = d;
        bram = b;

        curri = i;
        currj = j;

        row = r;
        col = c;
    }

    public ResourceElement(int l, int m, int d, int b) //for synth resource element
    {
        //This stores the resources required in a given synth design
        clel = l;
        clem = m;
        dsp = d;
        bram = b;

        curri = -1;
        currj = -1;

        row = -1;
        col = -1;

    }

    public ResourceElement (ResourceElement obj) //copy constructor
    {
        clel = obj.clel;
        clem = obj.clem;
        dsp = obj.dsp;
        bram = obj.bram;

        curri = obj.curri; 
        currj = obj.currj;

        row = obj.row; 
        col = obj.col;
    }

    public ResourceElement() //Empty non-assigned ResourceElement
    {
    }

    //Tells if the given resource element is in range or not
    public boolean inRange() //for map ResourceElement objects
    {
        if((curri >= starti) && (currj >= startj) && (curri <= endi) && (currj <= endj))
            return true;

        return false;
    }

    //function returns a pblock from the left and right rresource map elements
    public static ResourceElement pblockFromMap (ResourceElement left, ResourceElement right) //for pblock objects
    {
        //Here both left and right args is the map resourceElements
        //Both left and right must have left.curri == right.curri and left.row == right.row
        int l, m, d, b, i, j, r, c;
        l = right.clel - left.clel;
        m = right.clem - left.clem;
        d = right.dsp - left.dsp;
        b = right.bram - left.bram;
        r = left.row;
        c = right.col - left.col;
        i = starti;
        j = left.currj + 1;

        return new ResourceElement(l, m, d, b, i, j, r, c);
    }

    //Function returns a pblock of dimesions with the top-left tile mentioned as si and sj and the bottom right tile mentioned as ei and ej
    public static ResourceElement getPblock(int si, int sj, int ei, int ej)
    {
        ResourceElement left, right;
        if(sj == 0)
            left = new ResourceElement(ei, sj-1, ResourceMap);
        else
            left = ResourceMap.get(ei).get(sj-1);

        right = ResourceMap.get(ei).get(ej);
        return pblockFromMap(left, right);
    }

    //Adds the resources of the given resourceElement
    public void modifyResourceElement(ArrayList<ArrayList<ResourceElement>> tempMap) //For map resource
    {
        if(!inRange())
            return;

        MapElement tempElement = MapElement.map.get(curri).get(currj);
    
        ResourceElement top = new ResourceElement();
        ResourceElement left = new ResourceElement();
        ResourceElement daigonalTop = new ResourceElement();

        if(curri > 0)
            top = new ResourceElement(tempMap.get(curri-1).get(currj));

        if(currj > 0)
            left = new ResourceElement(tempMap.get(curri).get(currj-1));

        if((curri > 0) && (currj > 0))
            daigonalTop = new ResourceElement(tempMap.get(curri -1).get(currj -1));

        //Adding the related resource elements
        clel = top.clel + left.clel - daigonalTop.clel;
        clem = top.clem + left.clem - daigonalTop.clem;
        dsp = top.dsp + left.dsp - daigonalTop.dsp;
        bram = top.bram + left.bram - daigonalTop.bram;

        //Adding resources of that cell
        int l, m, d, b;
        l = m = d = b = 0;
        
        if(tempElement.isCLEL(tempElement.leftElementRootname))
            l++;
        if(tempElement.isCLEL(tempElement.rightElementRootname))
            l++;
        if(tempElement.isCLEM(tempElement.leftElementRootname))
            m++;
        if(tempElement.isCLEM(tempElement.rightElementRootname))
            m++;

        if((tempElement.isDSP(tempElement.leftElementRootname)) && (!tempElement.leftElementName.equals(MapElement.map.get(curri + 1).get(currj).leftElementName)))
            d++;

        if((tempElement.isDSP(tempElement.rightElementRootname)) && (!tempElement.rightElementName.equals(MapElement.map.get(curri + 1).get(currj).rightElementName)))
            d++;

        if((tempElement.isBRAM(tempElement.leftElementRootname)) && (!tempElement.leftElementName.equals(MapElement.map.get(curri + 1).get(currj).leftElementName)))
            b++;

        if((tempElement.isBRAM(tempElement.rightElementRootname)) && (!tempElement.rightElementName.equals(MapElement.map.get(curri + 1).get(currj).rightElementName)))
            b++;

        clel += l;
        clem += m;
        dsp += d;
        bram += b;
    }

    //This checks if the associated pblock can fit the give dcp resources or not
    public boolean isPblockAppropriate(ResourceElement dcp) //for pblock objects
    {
        if((dsp < dcp.dsp) || (bram < dcp.bram) || (clem < dcp.clem))
            return false;

        if((clel + clem - dcp.clem) < dcp.clel)
            return false;

        return true;
    }

    //Checks if the first column of the pblock can be removed or not
    public boolean isFirstColRemovable (ResourceElement dcp) //for pblock objects
    {
        ResourceElement left = ResourceMap.get(curri + row - 1).get(currj);
        ResourceElement right = ResourceMap.get(curri + row -1).get(currj + col -1);
        return pblockFromMap(left, right).isPblockAppropriate(dcp);
    }

    //This builds the resource mao from the final map of the device
    public static void resourceMapBuilder()
    {
        System.out.println("Building the resource map");
        ArrayList<ArrayList<ResourceElement>> tempMap = new ArrayList<>();
        for(int i = 0; i < MapElement.map.size(); i++)
        {
            tempMap.add(new ArrayList<ResourceElement>());
            for(int j = 0; j < MapElement.map.get(i).size(); j++)
            {
                //ResourceMap = tempMap;
                ResourceElement newElement = new ResourceElement(i, j, tempMap);
                tempMap.get(tempMap.size()-1).add(newElement);
            }
        }
        ResourceMap = tempMap;
        System.out.println("The dimensions of resource map is: rows: " + ResourceMap.size() + " columns: " + ResourceMap.get(0).size());
    }

}
