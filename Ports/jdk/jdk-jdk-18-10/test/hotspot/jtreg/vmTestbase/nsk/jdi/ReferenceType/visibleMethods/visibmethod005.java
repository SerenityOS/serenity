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

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * This test checks the method <code>visibleMethods()</code>
 * of the JDI interface <code>ReferenceType</code> of com.sun.jdi package
 */

public class visibmethod005 extends Log {
    static java.io.PrintStream out_stream;
    static boolean verbose_mode = false;  // test argument -vbs or -verbose switches to static
                                          // - for more easy failure evaluation

    /** The main class names of the debugger & debugee applications. */
    private final static String
        package_prefix = "nsk.jdi.ReferenceType.visibleMethods.",
//        package_prefix = "",    //  for DEBUG without package
        thisClassName = package_prefix + "visibmethod005",
        debugeeName   = thisClassName + "a";

    /** Debugee's classes for check **/
    private final static String class_for_check = package_prefix + "visibmethod005aInterfaceForCheck";
    private final static String super_interf_for_check_1 = package_prefix + "visibmethod005aSuperInterfaceForCheck_1";
    private final static String super_interf_for_check_2 = package_prefix + "visibmethod005aSuperInterfaceForCheck_2";
    private final static String not_visible_sign = "NOT VISIBLE";
    private final static String passed_sign = "PASSED";
    private static String methods_for_check[][] = {

//        method name                            declaring class           check result

        {"i_multiple_inherited_abstract_method_1", super_interf_for_check_1, not_visible_sign},
        {"i_multiple_inherited_abstract_method_2", super_interf_for_check_2, not_visible_sign}

    };

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
        out_stream = out;

        int v_test_result = new visibmethod005().runThis(argv,out_stream);
        if ( v_test_result == 2/*STATUS_FAILED*/ ) {
            out_stream.println("\n==> nsk/jdi/ReferenceType/visibleMethods/visibmethod005 test FAILED");
        }
        else {
            out_stream.println("\n==> nsk/jdi/ReferenceType/visibleMethods/visibmethod005 test PASSED");
        }
        return v_test_result;
    }

    private void print_log_on_verbose(String message) {
        display(message);
    }

    /**
     * Non-static variant of the method <code>run(args,out)</code>
     */
    private int runThis (String argv[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        verbose_mode = argHandler.verbose();

        if ( out_stream == null ) {
            out_stream = out;
        }

        out_stream.println("==> nsk/jdi/ReferenceType/visibleMethods/visibmethod005 test LOG:");
        out_stream.println("==> test checks visibleMethods() method of ReferenceType interface ");
        out_stream.println("    of the com.sun.jdi package for multiple inherited abstract methods\n");

        String debugee_launch_command = debugeeName;
        if (verbose_mode) {
            logTo(out_stream);
        }

        Binder binder = new Binder(argHandler,this);
        Debugee debugee = binder.bindToDebugee(debugee_launch_command);
        IOPipe pipe = new IOPipe(debugee);

        debugee.redirectStderr(out);
        print_log_on_verbose("--> visibmethod005: visibmethod005a debugee launched");
        debugee.resume();

        String line = pipe.readln();
        if (line == null) {
            out_stream.println
                ("##> visibmethod005: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        if (!line.equals("ready")) {
            out_stream.println
                ("##> visibmethod005: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose("--> visibmethod005: debugee's \"ready\" signal recieved!");
        }

        out_stream.println
            ("--> visibmethod005: check ReferenceType.visibleMethods() method for debugee's "
            + class_for_check + " class...");
        boolean class_not_found_error = false;
        boolean visibleMethods_method_error = false;
        int methods_for_check_number = methods_for_check.length;
        int not_visible_methods_number = 0;
        int visible_methods_number = 0;
        int unexpected_visible_methods_number = 0;

        while ( true ) {
            ReferenceType refType = debugee.classByName(class_for_check);
            if (refType == null) {
                out_stream.println("##> visibmethod005: Could NOT FIND class: " + class_for_check);
                class_not_found_error = true;
                break;
            }
            List<Method> visible_methods_list = null;
            try {
                visible_methods_list = refType.visibleMethods();
            }
            catch (Throwable thrown) {
                out_stream.println("##> visibmethod005: FAILED: ReferenceType.visibleMethods() throws unexpected "
                    + thrown);
                visibleMethods_method_error = true;
                break;
            }
            visible_methods_number = visible_methods_list.size();
            Method visible_methods[] = new Method[visible_methods_number];
            String unexpected_visible_methods[] = new String[visible_methods_number];
            visible_methods_list.toArray(visible_methods);
            for (int i=0; i<visible_methods_number; i++) {
                Method visible_method = visible_methods[i];
                String visible_method_name = visible_method.name();
                String declaring_class_name = "declaring class NOT defined";
                try {
                    declaring_class_name = visible_method.declaringType().name();
                }
                catch (Throwable thrown) {
                }

                String full_visible_method_info = visible_method_name +"  (" + declaring_class_name + ")";
                int j=0;
                for (; j<methods_for_check_number; j++) {
                    if ( visible_method_name.equals(methods_for_check[j][0]) ) {
                        if ( declaring_class_name.equals(methods_for_check[j][1]) ) {
                            methods_for_check[j][2] = passed_sign;
                            break;
                        }
                    }
                }
                if ( j == methods_for_check_number ) {
                    //  unexpected visible method found
                    unexpected_visible_methods[unexpected_visible_methods_number] = full_visible_method_info;
                    unexpected_visible_methods_number++;
                }
            }
            for (int i=0; i<methods_for_check_number; i++) {
                String current_method_for_check = methods_for_check[i][0]
                    + "  (" + methods_for_check[i][1] + ")";
                if ( methods_for_check[i][2].equals(not_visible_sign) ) {
                    out_stream.println
                        ("##> visibmethod005: FAILED: method is NOT visible: " + current_method_for_check);
                    not_visible_methods_number++;
                }
                else {
                    print_log_on_verbose
                        ("--> visibmethod005: PASSED for method: " + current_method_for_check);
                }
            }
            for (int i=0; i<unexpected_visible_methods_number; i++) {
                out_stream.println
                    ("##> visibmethod005: FAILED: unexpected visible method: " + unexpected_visible_methods[i]);
            }
            break;
        }

        out_stream.println("--> visibmethod005: check completed!");
        int v_test_result = 0/*STATUS_PASSED*/;
        if ( class_not_found_error || visibleMethods_method_error ) {
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            out_stream.println("--> visibmethod005: expected visible methods number = " + methods_for_check_number);
            out_stream.println("--> visibmethod005: in fact visible methods number  = "
                + visible_methods_number);
            out_stream.println("--> visibmethod005: expected and in fact visible methods number  = "
                + (methods_for_check_number - not_visible_methods_number));
            out_stream.println
                ("##> visibmethod005: NOT visible methods number = " + not_visible_methods_number);
            out_stream.println
                ("##> visibmethod005: UNEXPECTED visible methods number = " + unexpected_visible_methods_number);
        }
        if ( not_visible_methods_number + unexpected_visible_methods_number > 0 ) {
            v_test_result = 2/*STATUS_FAILED*/;
        }

        print_log_on_verbose("--> visibmethod005: waiting for debugee finish...");
        pipe.println("quit");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != 0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/) {
            out_stream.println
                ("##> visibmethod005: UNEXPECTED Debugee's exit status (not 95) - " + status);
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose
                ("--> visibmethod005: expected Debugee's exit status - " + status);
        }

        return v_test_result;
    }
}
