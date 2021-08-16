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

package nsk.jdi.ReferenceType.failedToInitialize;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * This test checks the method <code>failedToInitialize()</code>
 * of the JDI interface <code>ReferenceType</code> of com.sun.jdi package
 * for ClassType, InterfaceType
 */

public class failedToInitialize001 extends Log {
    static java.io.PrintStream out_stream;
    static boolean verbose_mode = false;

    /** The main class names of the debugger & debugee applications. */
    private final static String
        package_prefix = "nsk.jdi.ReferenceType.failedToInitialize.",
//        package_prefix = "",    //  for DEBUG without package
        thisClassName = package_prefix + "failedToInitialize001",
        debugeeName   = thisClassName + "a";

    /** Debugee's classes for check **/
    private final static String classes_for_check[][] = {

        {thisClassName, "no failed init", "class"},
        {thisClassName+"a", "no failed init", "class"},

        {package_prefix + "fail_init_class", "failed init", "class"},

        {package_prefix + "interf", "no failed init", "interface"},

        {package_prefix + "fail_init_interf", "failed init", "interface"},

        {package_prefix + "fail_init_subcl", "failed init", "class"}

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

        int v_test_result = new failedToInitialize001().runThis(argv,out_stream);
        if ( v_test_result == 2/*STATUS_FAILED*/ ) {
            out_stream.println
                ("\n==> nsk/jdi/ReferenceType/failedToInitialize/failedToInitialize001 test FAILED");
        }
        else {
            out_stream.println
                ("\n==> nsk/jdi/ReferenceType/failedToInitialize/failedToInitialize001 test PASSED");
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

        out_stream.println("==> nsk/jdi/ReferenceType/failedToInitialize/failedToInitialize001 test LOG:");
        out_stream.println("==> test checks failedToInitialize() method of ReferenceType interface ");
        out_stream.println("    of the com.sun.jdi package for ClassType, InterfaceType\n");

        String debugee_launch_command = debugeeName;
        if (verbose_mode) {
            logTo(out_stream);
        }

        Binder binder = new Binder(argHandler,this);
        Debugee debugee = binder.bindToDebugee(debugee_launch_command);
        IOPipe pipe = new IOPipe(debugee);

        debugee.redirectStderr(out);
        print_log_on_verbose("--> failedToInitialize001: failedToInitialize001a debugee launched");
        debugee.resume();

        String line = pipe.readln();
        if (line == null) {
            out_stream.println
                ("##> failedToInitialize001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        if (!line.equals("ready")) {
            out_stream.println
                ("##> failedToInitialize001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose
                ("--> failedToInitialize001: debugee's \"ready\" signal recieved!");
        }

        out_stream.println
            ("--> failedToInitialize001: checking debugee's classes by ReferenceType.failedToInitialize() method...");
        int all_classes_count = 0;
        int class_not_found_errors = 0;
        int failedToInitialize_method_errors = 0;
        for (int i=0; i<classes_for_check.length; i++) {
            String className = classes_for_check[i][0];
            all_classes_count++;
            ReferenceType refType = debugee.classByName(className);
            if (refType == null) {
                out_stream.println("##> failedToInitialize001: Could NOT FIND class: " + className);
                class_not_found_errors++;
                continue;
            }
            String s_failed_sign = classes_for_check[i][1];
            String s_type = classes_for_check[i][2];
            boolean failedToInitialize = s_failed_sign.equals("failed init");
            if (refType.failedToInitialize() != failedToInitialize) {
                out_stream.println("##> failedToInitialize001: UNEXPECTED failedToInitialize() method result ("
                                    + !failedToInitialize + ") for " + s_type + ": "
                                    + className + "(" +  s_failed_sign + ")");
                failedToInitialize_method_errors++;
            }
            else {
                print_log_on_verbose("--> failedToInitialize001:  expected failedToInitialize() method result ("
                                    + failedToInitialize + ") for " + s_type + ": "
                                    + className + "(" +  s_failed_sign + ")");
            }
        }
        out_stream.println("--> failedToInitialize001: checking debugee's classes completed!");
        out_stream.println("--> failedToInitialize001: number of checked classes = " + all_classes_count);
        if ( class_not_found_errors > 0 ) {
            out_stream.println("##> failedToInitialize001: \"class not found ERRORS\" counter = "
                                + class_not_found_errors);
        }
        out_stream.println("##> failedToInitialize001: failedToInitialize() method ERRORS counter = "
                            + failedToInitialize_method_errors);
        int v_test_result = 0/*STATUS_PASSED*/;
        if (class_not_found_errors + failedToInitialize_method_errors > 0) {
            v_test_result = 2/*STATUS_FAILED*/;
        }

        print_log_on_verbose("--> failedToInitialize001: waiting for debugee finish...");
        pipe.println("quit");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != 0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/) {
            out_stream.println
                ("##> failedToInitialize001: UNEXPECTED Debugee's exit status (not 95) - " + status);
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose
                ("--> failedToInitialize001: expected Debugee's exit status - " + status);
        }

        return v_test_result;
    }
}
