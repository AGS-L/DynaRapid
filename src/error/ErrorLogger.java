/*
* DynaRapid
*
* This file is part of DynaRapid project
* Copyright: See COPYING file that comes with this distribution
* For any questions, please contact Andrea Guerrieri <andrea.guerrieri@ieee.org> (C) 2024
*/




//This helps to log errors throughout the software wherever necessary
//This has a error log reference and the error logs.
//The logs can be cleared and entered and printed whenever we want.

import java.util.*;
import java.io.*;

public class ErrorLogger {
    public static HashMap<String,ErrorElement> errorMap;

    public static void reinitializeErrorMap()
    {
        errorMap = new HashMap<>();
    }
    /*
     * Requires:
     * ref: Reference String of error
     * e: error code
     * error: Error String
     */
    public static void addError(String ref, int e, String error)
    {
        if(errorMap.containsKey(ref))
            errorMap.get(ref).addEntry(e, error);
        
        else
        {
            ErrorElement obj = new ErrorElement();
            obj.addEntry(e, error);
            errorMap.put(ref, obj);
        }
    }

    public static void printErrorLogs()
    {
        System.out.println("------------Printing Warning Logs-------------");
        for (Map.Entry<String, ErrorElement> set : errorMap.entrySet()) 
        {
            System.out.println(set.getKey());
            set.getValue().printEntry();
            System.out.println("\n");
        }

        System.out.println("--------------Warning Logs Printed above. Number of warning logs were: " + errorMap.size() + " ---------------------");
    }


}
