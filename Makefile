#
# DynaRapid
#
# This file is part of DynaRapid project
# Copyright: See COPYING file that comes with this distribution 
# For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
# 


main: | bin
	g++ -std=c++11 ./src/ch/agsl/dynarapid/synthesizer/*.cpp -o ./bin/main
	g++ -std=c++11 ./src/ch/agsl/dynarapid/run-command/run_command_bash.cpp -o ./bin/run_command_bash
	g++ -std=c++11 ./src/ch/agsl/dynarapid/run-command/run_command_tcsh.cpp -o ./bin/run_command_tcsh

bin:
	mkdir -p $@
		
clean:
	rm -rf ./bin/*
	clear
