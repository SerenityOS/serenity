/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package nsk.jdi.ReferenceType.methodsByName_s;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This class is used as debugee application for the methbyname_s004 JDI test.
 */

public class methbyname_s004a {

    static boolean verbose_mode = false;  // debugger may switch to true
                                          // - for more easy failure evaluation
    private final static String
        package_prefix = "nsk.jdi.ReferenceType.methodsByName_s.";
//        package_prefix = "";    //  for DEBUG without package
    static String checked_class_name = package_prefix + "methbyname_s004aClassForCheck";


    private static void print_log_on_verbose(String message) {
        if ( verbose_mode ) {
            System.err.println(message);
        }
    }

    public static void main (String argv[]) {

        for (int i=0; i<argv.length; i++) {
            if ( argv[i].equals("-vbs") || argv[i].equals("-verbose") ) {
                verbose_mode = true;
                break;
            }
        }

        print_log_on_verbose("**> methbyname_s004a: debugee started!");
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        Class checked_class_classobj = null;
        try {
            checked_class_classobj =
                Class.forName(checked_class_name, true, methbyname_s004a.class.getClassLoader());
            print_log_on_verbose
                ("--> methbyname_s004a: checked class loaded:" + checked_class_name);
        }
        catch ( Throwable thrown ) {  // ClassNotFoundException
//            System.err.println
//                ("**> methbyname_s004a: load class: Throwable thrown = " + thrown.toString());
            print_log_on_verbose
                ("--> methbyname_s004a: checked class NOT loaded: " + checked_class_name);
            // Debuuger finds this fact itself
        }

        print_log_on_verbose("**> methbyname_s004a: waiting for \"quit\" signal...");
        pipe.println("ready");
        String instruction = pipe.readln();
        if (instruction.equals("quit")) {
            print_log_on_verbose("**> methbyname_s004a: \"quit\" signal recieved!");
            print_log_on_verbose("**> methbyname_s004a: completed succesfully!");
            System.exit(0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/);
        }
        System.err.println("!!**> methbyname_s004a: unexpected signal (no \"quit\") - " + instruction);
        System.err.println("!!**> methbyname_s004a: FAILED!");
        System.exit(2/*STATUS_FAILED*/ + 95/*STATUS_TEMP*/);
    }
}

abstract class methbyname_s004aClassForCheck extends methbyname_s004aSuperClassForCheck implements methbyname_s004aInterfaceForCheck {


    // overloaded static methods
    static void  s_overloaded_method() {}
    static String  s_overloaded_method(String s) {return "string";}
    static Object  s_overloaded_method(Object obj) {return new Object();}
    static Object  s_overloaded_method(long l, String s) {return new Object();}

    static Object  s_super_overloaded_method(long l, String s) {return new Object();}

    // overloaded instance methods
    void  i_overloaded_method() {}
    long  i_overloaded_method(Object obj) {return (long)1;}
    String  i_overloaded_method(String s) {return "string";}
    Object  i_overloaded_method(long l, String s) {return new Object();}

    Object  i_super_overloaded_method(long l, String s) {return new Object();}
    Object  i_interf_overloaded_method(long l, String s) {return new Object();}


}

abstract class methbyname_s004aSuperClassForCheck  {

    static Object  s_super_overloaded_method(long l) {return new Object();}
    Object  i_super_overloaded_method(long l) {return new Object();}

}

interface methbyname_s004aInterfaceForCheck {

    Object  i_interf_overloaded_method(long l);

}
