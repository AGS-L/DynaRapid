/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/


import java.util.*;
import java.io.*;

import org.python.antlr.PythonParser.continue_stmt_return;

/*
 * This class parses through the locations file and gives the location of the different files.
 * Makes the code relocatable
 */

public class LocationParser {
    static final String checkStrings [] = {
        "./settings.env",  //Has the location of the locations file. Must be always be like this
        "Export License: ",
        "Source Vivado: ",
        "Source RapidWright: ",
        "Map: ",
        "Pre-synthDCPs: ",
        "GenericSynthDCPs: ",
        "VHDLSynthDCPs: ",
        "Pre-exposedDCPs: ",
        "ExposedDCPs: ",
        "PlacedRoutedDCPs: ",
        "VivadoRun: ",
        "DotFiles: ",
        "Designs: ",
        "SrcVHDLFiles: ",
        "ComponentVHDLFiles: ",
        "Binaries: ",
        "Terminal: ",
    };

    static String exportLicense;
    static String sourceVivado;
    static String sourceRW;
    static String map;
    static String preSynthDCPs;
    static String genericSynthDCPs;
    static String vhdlSynthDCPs;
    static String preExposedDCPs;
    static String exposedDCPs; //This has all the dcps and the tcl scripts with flops / LUTs added to it.
    static String placedRoutedDCPs;
    static String vivadoRun;
    static String dotFiles;
    static String designs;
    static String srcVHDLFiles; //This has all the src vhdl files
    static String vhdlFiles; //This has all the vhdl files for the synth designs. Note that this field needs to be filed in the vhdl_writer (line 2112)
    static String bin;
    static String terminal;

    static void setVariable(String s, int i)
    {
        s = s.substring(s.indexOf(": ") + 2);

        switch(i)
        {
            case 1: exportLicense = s; break;
            case 2: sourceVivado = s; break;
            case 3: sourceRW = s; break;
            case 4: map = s; break;
            case 5: preSynthDCPs = s; break;
            case 6: genericSynthDCPs = s; break;
            case 7: vhdlSynthDCPs = s; break;
            case 8: preExposedDCPs = s; break;
            case 9: exposedDCPs = s; break;
            case 10: placedRoutedDCPs = s; break;
            case 11: vivadoRun = s; break;
            case 12: dotFiles = s; break;
            case 13: designs = s; break;
            case 14: srcVHDLFiles = s; break;
            case 15: vhdlFiles = s; break;
            case 16: bin = s; break;
            case 17: terminal = s; break;
            default: System.out.println("ERROR: Unknown direcory location");
        }
    }

    public static void locationParser()
    {
        int i = 0;
        String loc = checkStrings[i++];

        try{
            File file = new File(loc);
            Scanner in = new Scanner(file);

            while(in.hasNextLine() && i < checkStrings.length)
            {
                String s = in.nextLine();
                if((s.length() < 2) || (s.charAt(0) == '#'))
                    continue;

                if(s.startsWith(checkStrings[i]))
                {
                    setVariable(s, i);
                    i++;
                }
            }


        }

        catch (Exception e)
        {
            e.printStackTrace();;
        }
    }
}
