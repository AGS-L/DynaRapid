/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.databasegenerator;

import ch.agsl.dynarapid.modules.Component;
import ch.agsl.dynarapid.parser.LocationParser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class DatabaseGenerator {

    static final String sep = " : ";
    public static final String checkStrings[] = {
        "Device Name", //0
    };

    public static ArrayList<String> getAllPblocks (String dcpName)
    {
        File file = new File(LocationParser.getPlacedRoutedDCPsPath().resolve(dcpName) + File.separator);
        if(!file.exists())
        {
            System.out.println("ERROR: Could not find the component " + dcpName + ". Maybe pblocks have not been generated");
            return null;
        }

        ArrayList<String> pblocks = new ArrayList<>();

        File files[] = file.listFiles();
        for(int i = 0; i < files.length; i++)
        {
            String name = files[i].getName();
            int index = name.lastIndexOf("/");
            if(index == -1)
                index++;

            if(name.endsWith("_placedRouted.dcp"))
                pblocks.add(name.substring(index, name.lastIndexOf("_placedRouted.dcp")));
        }

        if(pblocks.size() == 0)
        {
            System.out.println("ERROR: Could not find any placed and routed file. Please run pblock generator again");
            return null;
        }

        return pblocks;
    }

    public static boolean printDatabase(String dcpName, int starti, int startj, int endi, int endj)
    {
        System.out.println("Generating database for the component: " + dcpName);
        try
        {
            ArrayList<String> pblocks = getAllPblocks(dcpName);
            if(pblocks == null)
                throw new Exception();

            String databaseLoc = LocationParser.getPlacedRoutedDCPsPath().resolve(dcpName + ".data").toString();
            FileWriter dataWriter = new FileWriter(new File(databaseLoc));

            dataWriter.write(checkStrings[0] + sep + "xck26\n");

            if(!ComponentDatabase.printDatabase(dataWriter, dcpName, pblocks))
                throw new Exception();

            for(String s : pblocks)
                if(!PblockDatabase.printDatabase(dataWriter, s, dcpName, starti, startj, endi, endj))
                    throw new Exception();

            dataWriter.close();
        }

        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not generate database for the component " + dcpName + ". See above logs");
            return false;
        }
        
        System.out.println("Completed Generating the database");
        return true;
    }

    //This function is supposed to generate the binary databases of the component
    public static boolean generateBinaryDatabase(Component component)
    {
        System.out.println("Generating binary database for the component: " + component.dcpName);
        String binaryDatabaseLoc = LocationParser.getPlacedRoutedDCPsPath()
                    .resolve(component.dcpName + ".bin.data").toString();

        try 
        {
            // Create a FileOutputStream and ObjectOutputStream
            FileOutputStream fileOutputStream = new FileOutputStream(binaryDatabaseLoc);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

            // Write the component to the binary file
            objectOutputStream.writeObject(component);

            // Close the streams
            objectOutputStream.close();
            fileOutputStream.close();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            System.out.println("ERROR: Could not generate binary database of component: " + component.dcpName);
            return false;
        }

        System.out.println("Completed Generating the binary database");
        return true;
    }
}
