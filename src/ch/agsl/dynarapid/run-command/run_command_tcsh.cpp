/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/


//This runs the command given in teh args in the tcsh shell

#include <bits/stdc++.h>

using namespace std;

int main(int argc, char* argv[] ) {

    string s = "tcsh -c ";
    s.push_back('"');
    
    for(int i = 1; i < argc; i++)
    {
        string st = argv[i];
        for(int j = 0; j < st.size(); j++)
            s.push_back(st[j]);

        s.push_back(' ');
    }

    s.push_back('"');

    const char *cSysCmd= s.c_str();
    return system(cSysCmd);
}
