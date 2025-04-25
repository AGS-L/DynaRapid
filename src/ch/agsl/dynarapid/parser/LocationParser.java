/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.parser;

import ch.agsl.dynarapid.GenerateDesign;
import com.xilinx.rapidwright.util.FileTools;
import com.xilinx.rapidwright.util.Installer;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

/**
 * This class parses through the locations file and gives the location of the
 * different files. Makes the code relocatable
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

    public static String exportLicense;
    public static String sourceVivado;
    public static String sourceRW;
    public static String map;
    public static String preSynthDCPs;
    public static String genericSynthDCPs;
    public static String vhdlSynthDCPs;
    public static String preExposedDCPs;
    public static String exposedDCPs; //This has all the dcps and the tcl scripts with flops / LUTs added to it.
    private static String placedRoutedDCPs;
    public static String vivadoRun;
    public static String dotFiles;
    public static String designs;
    public static String srcVHDLFiles; //This has all the src vhdl files
    public static String vhdlFiles; //This has all the vhdl files for the synth designs. Note that this field needs to be filed in the vhdl_writer (line 2112)
    public static String bin;
    public static String terminal;

    public static final String DYNARAPID_ROOT = getDynaRapidRoot();

    public static final String RELEASE_VERSION = "0.1.1";

    public static final String GH_RELEASE_DIR_URL = "https://github.com/AGS-L/DynaRapid/releases/download/";

    private static Path placedRoutedDCPsPath = null;

    static void setVariable(String s, int i) {
        s = s.substring(s.indexOf(": ") + 2);

        switch (i) {
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

    /**
     * Gets the location of where DynaRapid is located.
     * 
     * @return The path to DynaRapid's root directory
     */
    private static String getDynaRapidRoot() {
        File f = new File(LocationParser.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        while (f.isDirectory() && !Arrays.stream(f.list()).anyMatch(s -> s.equals("dynarapid_setup.sh"))) {
            f = f.getParentFile();
        }
        return f == null ? "" : f.getAbsolutePath();
    }

    public static void locationParser() {
        int i = 0;
        String loc = DYNARAPID_ROOT + File.separator + checkStrings[i++];

        try (Scanner in = new Scanner(new File(loc))) {
            while (in.hasNextLine() && i < checkStrings.length) {
                String s = in.nextLine();
                if ((s.length() < 2) || (s.charAt(0) == '#'))
                    continue;

                if (s.startsWith(checkStrings[i])) {
                    // Check if it is a file path that isn't set properly
                    String possiblePath = s.substring(s.indexOf(": ") + 2);
                    if (i < 4 || i > 16 || new File(possiblePath).exists()) {
                        setVariable(s, i);
                    } else {
                        // settings.env is likely not set correctly, let's use the default from the root
                        // directory
                        setVariable(s.replace(": ", ": " + DYNARAPID_ROOT + File.separator), i);
                    }
                    i++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized Path getPlacedRoutedDCPsPath() {
        if (placedRoutedDCPsPath == null) {
            String dirName = placedRoutedDCPs + File.separator + GenerateDesign.part + "-" + RELEASE_VERSION;
            if (!new File(dirName).exists()) {
                // We need to download the placed and routed DCPs from GitHub
                String zipFile = GenerateDesign.part + "-" + RELEASE_VERSION + ".zip";
                String dstZipFile = placedRoutedDCPs + File.separator + zipFile;
                Installer.downloadFile(GH_RELEASE_DIR_URL + "v" + RELEASE_VERSION + "/" + zipFile, dstZipFile);
                String md5File = RELEASE_VERSION + ".md5";
                String dstMD5File = placedRoutedDCPs + File.separator + md5File;
                Installer.downloadFile(GH_RELEASE_DIR_URL + "v" + RELEASE_VERSION + "/" + md5File, dstMD5File);
                String md5 = Installer.calculateMD5OfFile(placedRoutedDCPs + File.separator + zipFile);
                if (validateMD5(md5, zipFile, dstMD5File)) {
                    Installer.unzipFile(dstZipFile, placedRoutedDCPs);
                    FileTools.deleteFile(dstZipFile);
                    FileTools.deleteFile(dstMD5File);
                } else {
                    throw new RuntimeException("ERROR: Couldn't validate " + zipFile + " download.  Please try again.");
                }
            }
            placedRoutedDCPsPath = Paths.get(dirName);
        }
        return placedRoutedDCPsPath;
    }

    private static boolean validateMD5(String md5, String fileName, String md5File) {
        for (String line : FileTools.getLinesFromTextFile(md5File)) {
            String[] parts = line.split("\\s+");
            if (parts[1].equals(fileName)) {
                return md5.equals(parts[0]);
            }
        }
        System.err.println("ERROR: The MD5 file " + md5File + " does not contain an entry for " + fileName);
        return false;
    }
}
