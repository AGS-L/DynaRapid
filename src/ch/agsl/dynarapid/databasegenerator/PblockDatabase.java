/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/

package ch.agsl.dynarapid.databasegenerator;
import ch.agsl.dynarapid.GenerateDesign;
import ch.agsl.dynarapid.map.MapElement;
import ch.agsl.dynarapid.parser.LocationParser;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Module;
import com.xilinx.rapidwright.design.ModuleInst;
import com.xilinx.rapidwright.device.Site;
import com.xilinx.rapidwright.device.Tile;
import com.xilinx.rapidwright.edif.EDIFNetlist;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;

public class PblockDatabase {

    static final String sep = " : ";

    public static final String checkStrings[] = {
        "Pblock Name", //0
        "Top-left Coordinates", //1
        "Bottom-right Coordinates", //2
        "# of Rows", //3
        "# of Columns", //4
        "Anchor Site", //5
        "Anchor Tile", //6
        "Relative Position of Anchor Tile", //7
        "Anchor Site Index", //8
        "Density (%)", //9
        "# of valid places", //10
        "# of postion lines", //11
        "Note", //12
    };

    //Gets the value which starts after c and ends before "_"
    //give the c as _I or _J or _R and _C
    public static int getValue(String s, String c) {
        int startIndex = s.indexOf(c) + 2;
        int endIndex = s.indexOf("_", startIndex);

        if (endIndex == -1)
            endIndex = s.length();

        String val = s.substring(startIndex, endIndex);
        return Integer.parseInt(val);
    }

    public static int[] getCoordinatesFromName(String pblockName) {
        int coord[] = new int [4];
        coord[0] = getValue(pblockName, "_I");
        coord[1] = getValue (pblockName, "_J");
        coord[2] = getValue(pblockName, "_R") + coord[0] - 1;
        coord[3] = getValue(pblockName, "_C") + coord[1] - 1;
        return coord;
    }


    public static int getAnchorIndex(Site s, Tile t) {
        Site sites[] = t.getSites();
        for (int i = 0; i < sites.length; i++)
            if (sites[i].getName().equals(s.getName()))
                return i;
        
        return -1;
    }

    //This returns a list of all the valid locations in R#_C# format which is in the (si, sj) and (ei, ej)
    public static HashSet<String> getActualValidLocations(HashSet<Site> validPlaces, int si, int sj, int ei, int ej,
            int reli, int relj) {
        HashSet<String> actualValidLocations = new HashSet<>();
        for (Site s : validPlaces) {
            Tile tile = s.getTile();
            String coord = MapElement.findInMap(tile.getName());
            int starti = Integer.parseInt(coord.substring(0, coord.indexOf(":"))) - reli;
            int startj = Integer.parseInt(coord.substring(coord.indexOf(":") + 1)) - relj;

            if ((starti >= si) && (starti <= ei) && (startj >= sj) && (startj <= ej))
                actualValidLocations.add("R" + starti + "_C" + startj);
        }

        return actualValidLocations;
    }

    public static boolean printDatabase(FileWriter dataWriter, String pblockName, String dcpName, int si, int sj,
            int ei, int ej) throws Exception {
        System.out.println("\nWriting database for pblock: " + pblockName);
        File file = LocationParser.getPlacedRoutedDCPsPath().resolve(dcpName)
                .resolve(pblockName + "_placedRouted.dcp").toFile();
        if (!file.exists()) {
            System.out.println("ERROR: Could not trace dcp: " + pblockName);
            return false;
        }

        int coordinates[] = getCoordinatesFromName(pblockName); //The values are {starti}, {startj}, {endi}, {endj}

        Design design = new Design("test", GenerateDesign.fpga_part);
		EDIFNetlist netlist = design.getNetlist();

        String dcp = file.getAbsolutePath();
        String meta = LocationParser.getPlacedRoutedDCPsPath().resolve(dcpName)
                .resolve(pblockName + "_placedRouted_0_metadata.txt").toString();

        Module m = new Module(Design.readCheckpoint(dcp), meta);
        netlist.migrateCellAndSubCells(m.getNetlist().getTopCell(), true);
        ModuleInst m1 = design.createModuleInst("m1", m);

        String anchorSiteName = m.getAnchor().getName();
        System.out.println("Name of the anchor site: " + anchorSiteName);
        String anchorTileName = m.getAnchor().getTile().getName();
        System.out.println("Name of the anchor tile: " + anchorTileName);

        int anchori, anchorj;
        anchori = Integer.parseInt(MapElement.mapMap.get(anchorTileName).substring(0, MapElement.mapMap.get(anchorTileName).indexOf(':')));
        anchorj = Integer.parseInt(MapElement.mapMap.get(anchorTileName).substring(MapElement.mapMap.get(anchorTileName).indexOf(':') + 1));

        int reli = anchori - coordinates[0];
        int relj = anchorj - coordinates[1];
        int side;

        if (MapElement.map.get(anchori).get(anchorj).leftElementName.equalsIgnoreCase(anchorTileName))
            side = -1;
        else
            side = 1;

        double density = PblockDensity.density(pblockName, dcpName);
        if (Math.abs(density + 1.0) <= 0.0000001)
            return false;

        HashSet<String> actualValidLocations = getActualValidLocations(new HashSet<>(m1.getAllValidPlacements()), si, sj, ei, ej, reli, relj); //This gets te actual valid places where the shape can be placed.
        if (actualValidLocations.size() == 0) {
            System.out.println("No valid places in the given bounds for pblock: " + pblockName);
            return true;
        }
        int lines = (actualValidLocations.size() / 10) + ((actualValidLocations.size() % 10) == 0 ? 0 : 1); 
        
        dataWriter.write("\n");
        dataWriter.write(checkStrings[0] + sep + pblockName + "\n");
        dataWriter.write(checkStrings[1] + sep + coordinates[0] + " " + coordinates[1] + "\n");
        dataWriter.write(checkStrings[2] + sep + coordinates[2] + " " + coordinates[3] + "\n");
        dataWriter.write(checkStrings[3] + sep + (coordinates[2] - coordinates[0] + 1) + "\n");
        dataWriter.write(checkStrings[4] + sep + (coordinates[3] - coordinates[1] + 1) + "\n");
        dataWriter.write(checkStrings[5] + sep + anchorSiteName + "\n");
        dataWriter.write(checkStrings[6] + sep + anchorTileName + "\n");
        dataWriter.write(checkStrings[7] + sep + reli + " " + relj + " " + side + "\n");
        dataWriter.write(checkStrings[8] + sep + getAnchorIndex(m.getAnchor(), m.getAnchor().getTile()) + "\n");
        dataWriter.write(checkStrings[9] + sep + density + "\n");
        dataWriter.write(checkStrings[10] + sep + actualValidLocations.size() + "\n");
        dataWriter.write(checkStrings[11] + sep + lines + "\n");
        dataWriter.write(checkStrings[12] + sep + "Below are all the top-left coordinates where the shape can be placed. (DONOT pass these to RW module placer)" + "\n");

        int num = 0;
        for (String s : actualValidLocations) {
            dataWriter.write(s + "\t");
            num++;
            if(num == 10)
            {
                num = 0;
                dataWriter.write("\n");
            }
        }

        if ((actualValidLocations.size() % 10 != 0))
            dataWriter.write("\n");

        return true;
    }
}
