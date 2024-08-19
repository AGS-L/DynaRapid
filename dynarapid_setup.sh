cd ..
export RAPIDWRIGHT_INSTALL_DIR=`pwd`
cd DynaRapid
export DYNARAPID_INSTALL_DIR=`pwd`

export envfile=settings.env

rm $envfile

echo    "#"
echo    "# DynaRapid"                                                                                  >>  $envfile
echo    "#  "                                                                                          >>  $envfile
echo    "# This file is part of DynaRapid project"                                                     >>  $envfile
echo    "# Copyright: See COPYING file that comes with this distribution "                             >>  $envfile
echo    "# For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024 "   >>  $envfile
echo    "# "                                                                                         >>  $envfile
echo                                                                                                 >>  $envfile
echo                                                                                                 >>  $envfile
echo    "# This will give the location of all the directories and source files. "                      >>  $envfile
echo    "# Have all / as required. The code does not add it   "                                        >>  $envfile
echo    "# all lines to be commented starts with # "                                                   >>  $envfile
echo    "# Tag and location to be separated by :<single space>"                                      >>  $envfile
echo    "#"                                                                                            >>  $envfile

echo    Export License: export XILINXD_LICENSE_FILE=                                                 >>  $envfile
echo    Source Vivado: echo "Andrea"                                                                 >>  $envfile
echo    Source RapidWright: source $RAPIDWRIGHT_INSTALL_DIR/tcl/rapidwright.tcl                      >>  $envfile
echo    Map: $DYNARAPID_INSTALL_DIR/map/                                                                                 >>  $envfile
echo    Pre-synthDCPs: $DYNARAPID_INSTALL_DIR/library/preSynthDCPs/                                                      >>  $envfile
echo    GenericSynthDCPs: $DYNARAPID_INSTALL_DIR/library/genericSynthDCPs/                                               >>  $envfile
echo    VHDLSynthDCPs: $DYNARAPID_INSTALL_DIR/library/vhdlSynthDCPs/                                                     >>  $envfile
echo    Pre-exposedDCPs: $DYNARAPID_INSTALL_DIR/library/pre-exposedDCPs/                                                 >>  $envfile
echo    ExposedDCPs: $DYNARAPID_INSTALL_DIR/exposedDCPs/                                                                 >>  $envfile
echo    PlacedRoutedDCPs: $DYNARAPID_INSTALL_DIR/library/placedRoutedDCPs/                                               >>  $envfile
echo    VivadoRun: $DYNARAPID_INSTALL_DIR/vivadoRun/                                                                     >>  $envfile
echo    DotFiles: $DYNARAPID_INSTALL_DIR/dotFiles/                                                                       >>  $envfile
echo    Designs: $DYNARAPID_INSTALL_DIR/designs/                                                                         >>  $envfile
echo    SrcVHDLFiles: $DYNARAPID_INSTALL_DIR/library/vhdlFiles/                                                        >>  $envfile
echo    ComponentVHDLFiles: $DYNARAPID_INSTALL_DIR/library/vhdlFiles/                                                    >>  $envfile
echo    Binaries: $DYNARAPID_INSTALL_DIR/bin/                                                                            >>  $envfile
echo    Terminal: bash                                                                               >>  $envfile

mkdir bin
make
