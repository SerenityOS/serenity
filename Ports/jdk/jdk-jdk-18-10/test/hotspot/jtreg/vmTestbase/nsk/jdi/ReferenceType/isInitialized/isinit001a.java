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

package nsk.jdi.ReferenceType.isInitialized;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This class is used as debugee application for the isinit001 JDI test.
 */

public class isinit001a {

    static boolean verbose_mode = false;  // debugger may switch to true
                                          // - for more easy failure evaluation

    NotInitializedClass not_initialized_class_0,
        not_initialized_class_1[] = {not_initialized_class_0};

    NotInitializedInterface not_initialized_interface_0,
        not_initialized_interface_1[] = {not_initialized_interface_0};

    InitializedClass  initialized_class_0 = new InitializedClass();

    int copy_super_class_int_var = SubClass.super_class_int_var;

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

        print_log_on_verbose("**> isinit001a: debugee started!");
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        isinit001a isinit001a_obj = new isinit001a();

        print_log_on_verbose("**> isinit001a: waiting for \"quit\" signal...");
        pipe.println("ready");
        String instruction = pipe.readln();
        if (instruction.equals("quit")) {
            print_log_on_verbose("**> isinit001a: \"quit\" signal recieved!");
            print_log_on_verbose("**> isinit001a: completed succesfully!");
            System.exit(0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/);
        }
        System.err.println("!!**> isinit001a: unexpected signal (no \"quit\") - " + instruction);
        System.err.println("!!**> isinit001a: FAILED!");
        System.exit(2/*STATUS_FAILED*/ + 95/*STATUS_TEMP*/);
    }
}

// not initialized class
class NotInitializedClass {}

// not initialized interface
interface NotInitializedInterface {}


// initialized interface
interface InitializedInterface {
    static final int int_var = 1;
}

// initialized class
class InitializedClass implements InitializedInterface {
    static int my_int_var = int_var;

}

// initialized super class
class InitializedSuperClass {
    static int super_class_int_var = 999;

}

// subclass
class SubClass extends InitializedSuperClass {
    static String dummy_string;

}
