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

package nsk.jdi.ReferenceType.sourceName;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * This test checks the method <code>sourceName()</code>
 * of the JDI interface <code>ReferenceType</code> of com.sun.jdi package
 */

public class sourcename003 {
    static ArgumentHandler argsHandler;
    static Log test_log_handler;
    static boolean verbose_mode = false;  // test argument -verbose switches to true
                                          // - for more easy failure evaluation

    /** The main class sourceNames of the debugger & debugee applications. */
    private final static String
        package_prefix = "nsk.jdi.ReferenceType.sourceName.",
//        package_prefix = "",    //  for DEBUG without package
        thisClassName = package_prefix + "sourcename003",
        debugeeName   = thisClassName + "a";

    /** Debugee's class for check **/
    private final static String checked_class = thisClassName + "[]";


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

        int v_test_result = new sourcename003().runThis(argv,out);
        if ( v_test_result == 2/*STATUS_FAILED*/ ) {
            print_log_anyway
                ("\n==> nsk/jdi/ReferenceType/sourceName/sourcename003 test FAILED");
        }
        else {
            print_log_anyway
                ("\n==> nsk/jdi/ReferenceType/sourceName/sourcename003 test PASSED");
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
        test_log_handler = new Log(out, argsHandler);

        print_log_anyway("==> nsk/jdi/ReferenceType/sourceName/sourcename003 test LOG:");
        print_log_anyway("--> test checks sourceName() method of ReferenceType interface ");
        print_log_anyway("    of the com.sun.jdi package for class with unknown source name\n");

        String debugee_launch_command = debugeeName;

        Binder binder = new Binder(argsHandler,test_log_handler);
        Debugee debugee = binder.bindToDebugee(debugee_launch_command);
        IOPipe pipe = new IOPipe(debugee);

        debugee.redirectStderr(out);
        print_log_on_verbose("--> sourcename003: sourcename003a debugee launched");
        debugee.resume();

        String debugee_signal = pipe.readln();
        if (debugee_signal == null) {
            print_log_anyway
                ("##> sourcename003: UNEXPECTED debugee's signal (not \"ready\") - " + debugee_signal);
            return 2/*STATUS_FAILED*/;
        }
        if (!debugee_signal.equals("ready")) {
            print_log_anyway
                ("##> sourcename003: UNEXPECTED debugee's signal (not \"ready\") - " + debugee_signal);
            return 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose("--> sourcename003: debugee's \"ready\" signal recieved!");
        }

        boolean class_not_found_error = false;
        boolean sourceName_method_error = false;
        while ( true ) {
            print_log_on_verbose
                ("--> sourcename003: getting ReferenceType object for loaded checked class...");
            ReferenceType refType = debugee.classByName(checked_class);
            if (refType == null) {
                print_log_without_verbose
                    ("--> sourcename003: getting ReferenceType object for loaded checked class...");
                print_log_anyway("##> sourcename003: FAILED: Could NOT FIND checked class: " + checked_class);
                class_not_found_error = true;
                break;
            }
            else {
                print_log_on_verbose("--> sourcename003: checked class FOUND: " + checked_class);
            }
            print_log_anyway
                ("--> sourcename003: check ReferenceType.sourceName() method for class with unknown source name...");
            print_log_anyway
                ("-->                checked class - " + checked_class);
            String ref_type_sourceName = null;
            try {
                ref_type_sourceName = refType.sourceName();
                print_log_anyway
                    ("##> sourcename003: FAILED: NO any Exception thrown!");
                print_log_anyway
                    ("##>                returned sourceName = " + ref_type_sourceName);
                print_log_anyway
                    ("##>                expected Exception - com.sun.jdi.AbsentInformationException");
                sourceName_method_error = true;
            }
            catch (Exception expt) {
                if (expt instanceof com.sun.jdi.AbsentInformationException) {
                    print_log_anyway
                        ("--> sourcename003: PASSED: expected Exception thrown - " + expt.toString());
                }
                else {
                    print_log_anyway
                        ("##> sourcename003: FAILED: unexpected Exception thrown - " + expt.toString());
                    print_log_anyway
                        ("##>                expected Exception - com.sun.jdi.AbsentInformationException");
                    sourceName_method_error = true;
                }
            }
            break;
        }
        int v_test_result = 0/*STATUS_PASSED*/;
        if ( class_not_found_error || sourceName_method_error ) {
            v_test_result = 2/*STATUS_FAILED*/;
        }

        print_log_on_verbose("--> sourcename003: waiting for debugee finish...");
        pipe.println("quit");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != 0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/) {
            print_log_anyway
                ("##> sourcename003: UNEXPECTED Debugee's exit status (not 95) - " + status);
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose
                ("--> sourcename003: expected Debugee's exit status - " + status);
        }

        return v_test_result;
    }
}  // end of sourcename003 class
