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

package nsk.jdi.ReferenceType.fields;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * This test checks the method <code>fields()</code>
 * of the JDI interface <code>ReferenceType</code> of com.sun.jdi package
 */

public class fields003 {
    static ArgumentHandler argsHandler;
    static Log test_log_handler;
    static boolean verbose_mode = false;  // test argument -verbose switches to true
                                          // - for more easy failure evaluation

    /** The main class names of the debugger & debugee applications. */
    private static final String package_prefix = "nsk.jdi.ReferenceType.fields.";
    private static final String thisClassName = package_prefix + "fields003";
    private static final String debugeeName   = thisClassName + "a";

    /** Debugee's class for check **/
    private final static String checked_class =  package_prefix + "fields003b";


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

        int v_test_result = new fields003().runThis(argv,out);
        if ( v_test_result == 2/*STATUS_FAILED*/ ) {
            print_log_anyway
                ("\n==> nsk/jdi/ReferenceType/fields/fields003 test FAILED");
        }
        else {
            print_log_anyway
                ("\n==> nsk/jdi/ReferenceType/fields/fields003 test PASSED");
        }
        return v_test_result;
    }

    private static void print_log_on_verbose(String message) {
        test_log_handler.display(message);
    }

    private static void print_log_without_verbose(String message) {
        test_log_handler.comment(message);
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
        argv = argsHandler.getArguments();
        test_log_handler = new Log(out, argsHandler);

        print_log_anyway("==> nsk/jdi/ReferenceType/fields/fields003 test LOG:");
        print_log_anyway("--> test checks fields() method of ReferenceType interface ");
        print_log_anyway("    of the com.sun.jdi package for UNLOADED class\n");

        String debugee_launch_command = debugeeName;

        Binder binder = new Binder(argsHandler,test_log_handler);
        Debugee debugee = binder.bindToDebugee(debugee_launch_command);
        IOPipe pipe = debugee.createIOPipe();

        debugee.redirectStderr(out);
        print_log_on_verbose("--> fields003: fields003a debugee launched");
        debugee.resume();

        String debugee_signal = pipe.readln();
        if (debugee_signal == null) {
            print_log_anyway
                ("##> fields003: UNEXPECTED debugee's signal (not \"ready0\") - " + debugee_signal);
            return 2/*STATUS_FAILED*/;
        }
        if (!debugee_signal.equals("ready0")) {
            print_log_anyway
                ("##> fields003: UNEXPECTED debugee's signal (not \"ready0\") - " + debugee_signal);
            return 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose("--> fields003: debugee's \"ready0\" signal recieved!");
        }

        //  pass to debugee checked_class_dir...
        debugee_signal = pipe.readln();
        if (debugee_signal == null) {
            print_log_anyway
                ("##> fields003: UNEXPECTED debugee's signal (not \"ready1\") - " + debugee_signal);
            return 2/*STATUS_FAILED*/;
        }
        if (!debugee_signal.equals("ready1")) {
            print_log_anyway
                ("##> fields003: UNEXPECTED debugee's signal (not \"ready1\") - " + debugee_signal);
            return 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose("--> fields003: debugee's \"ready1\" signal recieved!");
        }

        boolean class_not_found_error = false;
        boolean fields_method_error = false;
        while ( true ) {
            print_log_on_verbose
                ("--> fields003: getting ReferenceType object for loaded checked class...");
            ReferenceType refType = debugee.classByName(checked_class);
            if (refType == null) {
                print_log_without_verbose
                    ("--> fields003: getting ReferenceType object for loaded checked class...");
                print_log_anyway("##> fields003: FAILED: Could NOT FIND checked class: " + checked_class);
                class_not_found_error = true;
                break;
            }
            else {
                print_log_on_verbose("--> fields003: checked class FOUND: " + checked_class);
            }
            print_log_on_verbose
                ("--> fields003: waiting for \"ready2\" or \"not_unloaded\" signal from debugee...");
            pipe.println("continue");
            debugee_signal = pipe.readln();
            if (debugee_signal == null) {
                print_log_anyway
                    ("##> fields003: UNEXPECTED debugee's signal - " + debugee_signal);
                return 2/*STATUS_FAILED*/;
            }
            if ( debugee_signal.equals("not_unloaded")) {
                print_log_anyway
                    ("--> fields003: debugee's \"not_unloaded\" signal recieved!");
                print_log_without_verbose
                    ("-->            checked class may be NOT unloaded!");
                print_log_anyway
                    ("-->            ReferenceType.fields() method can NOT be checked!");
                break;
            }
            if (!debugee_signal.equals("ready2")) {
                print_log_anyway
                    ("##> fields003: UNEXPECTED debugee's signal (not \"ready2\") - " + debugee_signal);
                return 2/*STATUS_FAILED*/;
            }
            else {
                print_log_on_verbose("--> fields003: debugee's \"ready2\" signal recieved!");
            }
            print_log_on_verbose
                ("--> fields003: check that checked class has been unloaded realy...");
            ReferenceType refType2 = debugee.classByName(checked_class);
            if (refType2 == null) {
                print_log_on_verbose
                    ("--> fields003: checked class has been unloaded realy: " + checked_class);
            }
            else {
                print_log_without_verbose
                    ("--> fields003: check that checked class has been unloaded realy...");
                print_log_anyway
                    ("--> fields003: checked class FOUND: " + checked_class
                    + " => it has NOT been unloaded!");
                print_log_anyway
                    ("-->            ReferenceType.fields() method can NOT be checked!");
                break;
            }

            print_log_anyway
                ("--> fields003: check ReferenceType.fields() method for unloaded class...");
            List fields_list = null;
            try {
                fields_list = refType.fields();
            }
            catch (Exception expt) {
                if (expt instanceof com.sun.jdi.ObjectCollectedException) {
                    print_log_anyway
                        ("--> fields003: PASSED: expected Exception thrown - " + expt.toString());
                }
                else {
                    print_log_anyway
                        ("##> fields003: FAILED: unexpected Exception thrown - " + expt.toString());
                    print_log_anyway
                        ("##>            expected Exception - com.sun.jdi.ObjectCollectedException");
                    fields_method_error = true;
                }
            }
            break;
        }
        int v_test_result = 0/*STATUS_PASSED*/;
        if ( class_not_found_error || fields_method_error ) {
            v_test_result = 2/*STATUS_FAILED*/;
        }

        print_log_on_verbose("--> fields003: waiting for debugee finish...");
        pipe.println("quit");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != 0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/) {
            print_log_anyway
                ("##> fields003: UNEXPECTED Debugee's exit status (not 95) - " + status);
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose
                ("--> fields003: expected Debugee's exit status - " + status);
        }

        return v_test_result;
    }
}  // end of fields003 class
