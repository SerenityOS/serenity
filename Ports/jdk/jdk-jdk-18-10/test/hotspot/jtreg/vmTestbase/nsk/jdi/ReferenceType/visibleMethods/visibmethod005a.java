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

package nsk.jdi.ReferenceType.visibleMethods;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This class is used as debugee application for the visibmethod005 JDI test.
 */

public class visibmethod005a {

    static boolean verbose_mode = false;  // debugger may switch to true
                                          // - for more easy failure evaluation
    private final static String
        package_prefix = "nsk.jdi.ReferenceType.visibleMethods.";
//        package_prefix = "";    //  for DEBUG without package
    static String checked_class_name = package_prefix + "visibmethod005aInterfaceForCheck";


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

        print_log_on_verbose("**> visibmethod005a: debugee started!");
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        Class checked_class_classobj = null;
        try {
            checked_class_classobj =
                Class.forName(checked_class_name, true, visibmethod005a.class.getClassLoader());
            print_log_on_verbose
                ("--> visibmethod005a: checked class loaded:" + checked_class_name);
        }
        catch ( Throwable thrown ) {  // ClassNotFoundException
//            System.err.println
//                ("**> visibmethod005a: load class: Throwable thrown = " + thrown.toString());
            print_log_on_verbose
                ("--> visibmethod005a: checked class NOT loaded: " + checked_class_name);
            // Debuuger finds this fact itself
        }

        print_log_on_verbose("**> visibmethod005a: waiting for \"quit\" signal...");
        pipe.println("ready");
        String instruction = pipe.readln();
        if (instruction.equals("quit")) {
            print_log_on_verbose("**> visibmethod005a: \"quit\" signal recieved!");
            print_log_on_verbose("**> visibmethod005a: completed succesfully!");
            System.exit(0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/);
        }
        System.err.println("!!**> visibmethod005a: unexpected signal (no \"quit\") - " + instruction);
        System.err.println("!!**> visibmethod005a: FAILED!");
        System.exit(2/*STATUS_FAILED*/ + 95/*STATUS_TEMP*/);
    }
}

interface visibmethod005aInterfaceForCheck extends visibmethod005aSuperInterfaceForCheck_1, visibmethod005aSuperInterfaceForCheck_2 {

}

interface visibmethod005aSuperInterfaceForCheck_1  {

    Object i_multiple_inherited_abstract_method_1(Object obj);
}

interface visibmethod005aSuperInterfaceForCheck_2 {

    Object i_multiple_inherited_abstract_method_2(Object obj);
}
