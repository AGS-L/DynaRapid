#
# DynaRapid
#
# This file is part of DynaRapid project
# Copyright: See COPYING file that comes with this distribution 
# For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
# 


main:
	javac -d ./bin/ ./src/*/*.java ./src/*/*/*.java
	g++ -std=c++11 ./src/synthesizer/*.cpp -o ./bin/main
	g++ -std=c++11 ./src/run-command/run_command_bash.cpp -o ./bin/run_command_bash
	g++ -std=c++11 ./src/run-command/run_command_tcsh.cpp -o ./bin/run_command_tcsh
	
lib:
	cd library && unzip ./library.zip 
	
clean:
	rm -rf ./bin/*
	clear
