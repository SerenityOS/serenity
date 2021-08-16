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

package nsk.jdi.ReferenceType.methods;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * This test checks the method <code>methods()</code>
 * of the JDI interface <code>ReferenceType</code> of com.sun.jdi package
 */

public class methods004 {
    static ArgumentHandler argsHandler;
    static Log test_log_handler;
    static boolean verbose_mode = false;  // test argument -verbose switches to true
                                          // - for more easy failure evaluation

    /** The main class names of the debugger & debugee applications. */
    private final static String
        package_prefix = "nsk.jdi.ReferenceType.methods.",
//        package_prefix = "",    //  for DEBUG without package
        thisClassName = package_prefix + "methods004",
        debugeeName   = thisClassName + "a";

    /** Debugee's classes for check **/
    private final static String class_for_check = package_prefix + "methods004aInterfaceForCheck";

    /**
     * Re-call to <code>run(args,out)</code>, and exit with
     * either status 95 or 97 (JCK-like exit status).
     */
    public static void main (String argv[]) {
        int exitCode = run(argv,System.out);
        System.exit(exitCode + 95/*STATUS_TEMP*/);
    }

    /**
     * JCK-like entry point to the test: perform testing, and
     * return exit code 0 (PASSED) or either 2 (FAILED).
     */
    public static int run (String argv[], PrintStream out) {

        int v_test_result = new methods004().runThis(argv,out);
        if ( v_test_result == 2/*STATUS_FAILED*/ ) {
            print_log_anyway("\n==> nsk/jdi/ReferenceType/methods/methods004 test FAILED");
        }
        else {
            print_log_anyway("\n==> nsk/jdi/ReferenceType/methods/methods004 test PASSED");
        }
        return v_test_result;
    }

    private static void print_log_on_verbose(String message) {
        test_log_handler.display(message);
    }

    private static void print_log_anyway(String message) {
        test_log_handler.println(message);
    }

    /**
     * Non-static variant of the method <code>run(args,out)</code>
     */
    private int runThis (String argv[], PrintStream out) {
        argsHandler = new ArgumentHandler(argv);
        verbose_mode = argsHandler.verbose();
        test_log_handler = new Log(out, argsHandler);

        print_log_anyway("==> nsk/jdi/ReferenceType/methods/methods004 test LOG:");
        print_log_anyway("==> test checks methods() method of ReferenceType interface ");
        print_log_anyway("    of the com.sun.jdi package for class without any methods\n");

        String debugee_launch_command = debugeeName;

        Binder binder = new Binder(argsHandler,test_log_handler);
        Debugee debugee = binder.bindToDebugee(debugee_launch_command);
        IOPipe pipe = new IOPipe(debugee);

        debugee.redirectStderr(out);
        print_log_on_verbose("--> methods004: methods004a debugee launched");
        debugee.resume();

        String line = pipe.readln();
        if (line == null) {
            print_log_anyway
                ("##> methods004: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        if (!line.equals("ready")) {
            print_log_anyway
                ("##> methods004: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose("--> methods004: debugee's \"ready\" signal recieved!");
        }

        print_log_anyway
            ("--> methods004: check ReferenceType.methods() method for debugee's "
            + class_for_check + " class...");
        boolean class_not_found_error = false;
        boolean methods_method_error = false;
        int returned_methods_number = 0;

        while ( true ) {
            ReferenceType refType = debugee.classByName(class_for_check);
            if (refType == null) {
                print_log_anyway("##> methods004: Could NOT FIND class: " + class_for_check);
                class_not_found_error = true;
                break;
            }
            List<Method> methods_list = null;
            try {
                methods_list = refType.methods();
            }
            catch (Throwable thrown) {
                print_log_anyway("##> methods004: FAILED: ReferenceType.methods() throws unexpected "
                    + thrown);
                methods_method_error = true;
                break;
            }
            returned_methods_number = methods_list.size();
            if ( returned_methods_number == 0 ) {
                break;
            }
            Method returned_methods[] = new Method[returned_methods_number];
            methods_list.toArray(returned_methods);
            for (int i=0; i<returned_methods_number; i++) {
                Method returned_method = returned_methods[i];
                String returned_method_name = returned_method.name();
                String declaring_class_name = "declaring class NOT defined";
                try {
                    declaring_class_name = returned_method.declaringType().name();
                }
                catch (Throwable thrown) {
                }
                String returned_method_info = returned_method_name +"  (" + declaring_class_name + ")";
                print_log_anyway
                    ("##> methods004: FAILED: unexpected found method: " + returned_method_info);
            }
            break;
        }

        int v_test_result = 0/*STATUS_PASSED*/;
        if ( class_not_found_error || methods_method_error ) {
            v_test_result = 2/*STATUS_FAILED*/;
        }

        if ( returned_methods_number > 0 ) {
            print_log_anyway
                ("##> methods004: UNEXPECTED all methods number = " + returned_methods_number);
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_anyway
                ("--> methods004: PASSED: returned list of methods is empty!");
        }

        print_log_on_verbose("--> methods004: waiting for debugee finish...");
        pipe.println("quit");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != 0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/) {
            print_log_anyway
                ("##> methods004: UNEXPECTED Debugee's exit status (not 95) - " + status);
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose
                ("--> methods004: expected Debugee's exit status - " + status);
        }

        return v_test_result;
    }
}
