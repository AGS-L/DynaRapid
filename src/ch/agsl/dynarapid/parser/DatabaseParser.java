/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.parser;

import ch.agsl.dynarapid.GenerateDesign;
import ch.agsl.dynarapid.databasegenerator.ComponentDatabase;
import ch.agsl.dynarapid.databasegenerator.DatabaseGenerator;
import ch.agsl.dynarapid.databasegenerator.PblockDatabase;
import ch.agsl.dynarapid.modules.Component;
import ch.agsl.dynarapid.modules.Shape;
import com.xilinx.rapidwright.device.Device;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * This class parses through the database and builds a component with all the values and shapes.
 */
public class DatabaseParser {

    int shapeNum;

    public DatabaseParser()
    {
        shapeNum = 0;
    }

    //Gets all the for num lines 
    public ArrayList<String> getValues(BufferedReader in, String s, int num) throws IOException
    {
        ArrayList<String> values = new ArrayList<>();
        values.add(s.substring(s.indexOf(" : ") + 3));

        for(int i = 1; i < num; i++)
        {
            s = in.readLine();
            s = s.substring(s.indexOf(" : ") + 3);
            values.add(s);
        }

        return values;
    }

    //parses the database part of the database
    public boolean databaseParser(String s)
    {
        String deviceName = s.substring(s.indexOf(" : ") + 3);
        if(deviceName.equals(GenerateDesign.fpga_part))
            return true;

        return false;
    }

    //parses the component part of the database and returns the component
    public Component componentParser(BufferedReader in, String s) throws Exception
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
        String line = null;
        while ((line = in.readLine()) != null) {
            if (line.length() > 2)
                num++;
            else
                break;
        }

        if(num != numShapes)
        {
            System.out.println("ERROR: Cold not find all the pblock names from the component database. Only found " + num + " pblocks");
            return null;
        }

        shapeNum = numShapes;
        return component;
    }

    //parses the pblock part of the database and sets the shape in the shapes list in the component
    public boolean pblockParser(BufferedReader in, String s, Component component) throws IOException
    {
        ArrayList<String> values = getValues(in, s, PblockDatabase.checkStrings.length);

        String[] lineValues;

        String pblockName = values.get(0);
        
        lineValues = values.get(1).split(" ");
        int starti = Integer.parseInt(lineValues[0]);
        int startj = Integer.parseInt(lineValues[1]);

        int rows = Integer.parseInt(values.get(3));
        int cols = Integer.parseInt(values.get(4));

        String anchorSiteName = values.get(5);
        String anchorTileName = values.get(6);

        lineValues = values.get(7).split(" ");
        int reli = Integer.parseInt(lineValues[0]);
        int relj = Integer.parseInt(lineValues[1]);
        int side = Integer.parseInt(lineValues[2]);

        int siteIndex = Integer.parseInt(values.get(8));

        Double density = Double.parseDouble(values.get(9));
        int validPlacesNum = Integer.parseInt(values.get(10));
        int lines = Integer.parseInt(values.get(11));

        HashSet<String> validTopLeftAnchorLocations = new HashSet<>();
        for(int i = 0; i < lines; i++)
        {
            lineValues = in.readLine().split("\t");

            if ((lineValues.length != 10) && (i != lines - 1))
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

        try (BufferedReader in = new BufferedReader(new FileReader(dataLoc))) {
            String s = null;
            while ((s = in.readLine()) != null) {
                if(s.length() <= 1)
                    continue;

                if(s.startsWith(DatabaseGenerator.checkStrings[0]))
                    if (!databaseParser(s))
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
        String databaseLoc = LocationParser.getPlacedRoutedDCPsPath().resolve(dcpName + ".data").toString();
        String binaryDatabaseLoc = LocationParser.getPlacedRoutedDCPsPath().resolve(dcpName + ".bin.data").toString();

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
