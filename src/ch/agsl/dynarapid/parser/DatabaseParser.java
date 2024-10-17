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

//This class parses through the database and builds a component with all the values and shapes.

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

public class DatabaseParser {

    int shapeNum;

    public DatabaseParser()
    {
        shapeNum = 0;
    }

    //Gets all the for num lines 
    public ArrayList<String> getValues(Scanner in, String s, int num)
    {
        ArrayList<String> values = new ArrayList<>();
        values.add(s.substring(s.indexOf(" : ") + 3));

        for(int i = 1; i < num; i++)
        {
            s = in.nextLine();
            s = s.substring(s.indexOf(" : ") + 3);
            values.add(s);
        }

        return values;
    }

    //parses the database part of the database
    public boolean databaseParser(Scanner in, String s) throws Exception
    {
        String deviceName = s.substring(s.indexOf(" : ") + 3);
        if(deviceName.equals("xcvu13p-fsga2577-1-i"))
            return true;

        return false;
    }

    //parses the component part of the database and returns the component
    public Component componentParser(Scanner in, String s) throws Exception
    {
        ArrayList<String> values = getValues(in, s, ComponentDatabase.checkStrings.length);
        
        String dcpName = values.get(0);
        int incFactor = Integer.parseInt(values.get(1));
        int clel = Integer.parseInt(values.get(2));
        int clem = Integer.parseInt(values.get(3));
        int dsp = Integer.parseInt(values.get(4));
        int bram = Integer.parseInt(values.get(5));

        Component component = new Component(dcpName, clel, clem, dsp, bram, incFactor);
        int numShapes = Integer.parseInt(values.get(6));

        int num = 0;
        while((in.hasNextLine() && in.nextLine().length() > 2))
            num++;

        if(num != numShapes)
        {
            System.out.println("ERROR: Cold not find all the pblock names from the component database. Only found " + num + " pblocks");
            return null;
        }

        shapeNum = numShapes;
        return component;
    }

    //parses the pblock part of the database and sets the shape in the shapes list in the component
    public boolean pblockParser(Scanner in, String s, Component component)
    {
        ArrayList<String> values = getValues(in, s, PblockDatabase.checkStrings.length);

        ArrayList<String> lineValues;

        String pblockName = values.get(0);
        
        lineValues = StringUtils.stringTokenizer(values.get(1), " ");
        int starti = Integer.parseInt(lineValues.get(0));
        int startj = Integer.parseInt(lineValues.get(1));

        int rows = Integer.parseInt(values.get(3));
        int cols = Integer.parseInt(values.get(4));

        String anchorSiteName = values.get(5);
        String anchorTileName = values.get(6);

        lineValues = StringUtils.stringTokenizer(values.get(7), " ");
        int reli = Integer.parseInt(lineValues.get(0));
        int relj = Integer.parseInt(lineValues.get(1));
        int side = Integer.parseInt(lineValues.get(2));

        int siteIndex = Integer.parseInt(values.get(8));

        Double density = Double.parseDouble(values.get(9));
        int validPlacesNum = Integer.parseInt(values.get(10));
        int lines = Integer.parseInt(values.get(11));

        HashSet<String> validTopLeftAnchorLocations = new HashSet<>();
        for(int i = 0; i < lines; i++)
        {
            lineValues = StringUtils.stringTokenizer(in.nextLine(), "\t");

            if((lineValues.size() != 10) && (i != lines-1))
            {
                System.out.println("ERROR: Line " + i + " does not have 10 positions");
                return false;
            }

            for(String st : lineValues)
                validTopLeftAnchorLocations.add(st);
        }

        /*
         * Note that sometimes the number of validTopLeftAnchorLocations size may not be same as validPlacesNum.
         * Sometimes the same R#_C# occurs multiple times. This needs to be ignored. Remember This happens is very small pblocks due to side replicability
         */
        
        Shape shape = new Shape(component, starti, startj, rows, cols, pblockName, density, reli, relj, side, siteIndex, anchorSiteName, anchorTileName, validTopLeftAnchorLocations);
        component.shapeList.add(shape);
        return true;
    }

    //This is responsible for parsing normal databases
    public Component parseDatabase(String dataLoc)
    {
        System.out.println("Parsing normal database: " + dataLoc);
        Component component = null;
        shapeNum = -1;

        try
        {
            File file = new File(dataLoc);
            if(!file.exists())
                throw new IOException("ERROR: Could not trace database: " + dataLoc);
            
            Scanner in = new Scanner (file);

            while(in.hasNextLine())
            {
                String s = in.nextLine();
                if(s.length() <= 1)
                    continue;

                if(s.startsWith(DatabaseGenerator.checkStrings[0]))
                    if(!databaseParser(in, s))
                        throw new Exception("ERROR: Device Name is not matching with the design required");

                if(s.startsWith(ComponentDatabase.checkStrings[0]))
                    component = componentParser(in, s);

                if(s.startsWith(PblockDatabase.checkStrings[0]) && (component ==  null))
                    throw new Exception("ERROR: Didnot read component. So do not know number of pblocks");

                if(s.startsWith(PblockDatabase.checkStrings[0]) && (component != null))
                    if(!pblockParser(in, s, component))
                        throw new IOException("ERROR: Could generate " + s);                
            }

            //The number of shapes may be different from what is mentioned in the shapList becuase some of them have 0 valid locations so are avoided
            // if(component.shapeList.size() != shapeNum)
            //     throw new Exception("ERROR: Shape numbers not matching");
        }

        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
        
        return component;
    }

    public Component parseBinaryDatabase(String dataLoc)
    {
        System.out.println("Parsing binary database: " + dataLoc);
        Component component = null;

        try 
        {
            // Create a FileInputStream and ObjectInputStream
            FileInputStream fileInputStream = new FileInputStream(dataLoc);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

            // Read the component from the binary file
            component = (Component) objectInputStream.readObject();

            // Close the streams
            objectInputStream.close();
            fileInputStream.close();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not parse binary database: " + dataLoc + ". See above logs");
            return null;

        }

        return component;
    }

    //this selects the appropriate database and then returns the UPDATED COMPONENT
    //This returns the updated component
    //Prioritizes binary databases over normal ones
    public Component parser(String dcpName, Device device)
    {
        String databaseLoc = LocationParser.placedRoutedDCPs + dcpName + ".data";
        String binaryDatabaseLoc = LocationParser.placedRoutedDCPs + dcpName + ".bin.data";

        // File file = new File(binaryDatabaseLoc);
        // Component component;

        // if(file.exists())
        //     component = parseBinaryDatabase(binaryDatabaseLoc);
        
        // else 
        // {
        //     file = new File(databaseLoc);
        //     component = parseDatabase(databaseLoc);
        // }

        File file = new File(databaseLoc);
        Component component = parseDatabase(databaseLoc);

        if(component == null)
        {
            System.out.println("ERROR: Could not parse any database of module: " + dcpName);
            return null;
        }

        System.out.println("Updating component of module: " + dcpName);
        component.updateComponent(device);
        return component;
    }
}
