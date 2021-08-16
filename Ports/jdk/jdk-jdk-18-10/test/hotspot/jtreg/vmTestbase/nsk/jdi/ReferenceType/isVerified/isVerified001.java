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

package nsk.jdi.ReferenceType.isVerified;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * This test checks the method <code>isVerified()</code>
 * of the JDI interface <code>ReferenceType</code> of com.sun.jdi package
 * for ClassType, InterfaceType
 */

public class isVerified001 extends Log {
    static java.io.PrintStream out_stream;
    static boolean verbose_mode = false;

    /** The main class names of the debugger & debugee applications. */
    private final static String
        package_prefix = "nsk.jdi.ReferenceType.isVerified.",
//        package_prefix = "",    //  for DEBUG without package
        thisClassName = package_prefix + "isVerified001",
        debugeeName   = thisClassName + "a";


    static ArgumentHandler      argsHandler;
    private static Log  logHandler;

    /** Debugee's classes for check **/
    private final static String classes_for_check[][] = {

        {thisClassName, "verified", "class"},
        {thisClassName+"a", "verified", "class"},

//        {package_prefix + "not_verif_cls", "not verified", "class"},

//        {package_prefix + "not_verif_interf", "not verified", "interface"},

        {package_prefix + "verif_class", "verified", "class"},

        {package_prefix + "verif_interf", "verified", "interface"},

        {package_prefix + "verif_subcl", "verified", "class"}

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

        int v_test_result = new isVerified001().runThis(argv,out_stream);
        if ( v_test_result == 2/*STATUS_FAILED*/ ) {
            logHandler.complain("\n==> nsk/jdi/ReferenceType/isVerified/isVerified001 test FAILED");
        }
        else {
            logHandler.display("\n==> nsk/jdi/ReferenceType/isVerified/isVerified001 test PASSED");
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

        argsHandler      = new ArgumentHandler(argv);
        logHandler = new Log(out, argsHandler);
        Binder binder    = new Binder(argsHandler, logHandler);

        print_log_on_verbose("==> nsk/jdi/ReferenceType/isInitialized/isinit001 test LOG:");
        print_log_on_verbose("==> test checks the isInitialized() method of ReferenceType interface");
        print_log_on_verbose("    of the com.sun.jdi package for ClassType, InterfaceType\n");

        String debugee_launch_command = debugeeName;
        if (verbose_mode) {
            debugee_launch_command = debugeeName + " -vbs";
        }

        Debugee debugee = binder.bindToDebugee(debugee_launch_command);
        IOPipe pipe = new IOPipe(debugee);


        debugee.redirectStderr(out);
        print_log_on_verbose("--> isVerified001: isVerified001a debugee launched");
        debugee.resume();

        String line = pipe.readln();
        if (line == null) {
            logHandler.complain
                ("##> isVerified001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        if (!line.equals("ready")) {
            logHandler.complain
                ("##> isVerified001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose("--> isVerified001: debugee's \"ready\" signal recieved!");
        }

        logHandler.display
            ("--> isVerified001: checking debugee's classes by ReferenceType.isVerified() method...");
        int all_classes_count = 0;
        int class_not_found_errors = 0;
        int isVerified_method_errors = 0;
        for (int i=0; i<classes_for_check.length; i++) {
            String className = classes_for_check[i][0];
            all_classes_count++;
            ReferenceType refType = debugee.classByName(className);
            if (refType == null) {
                logHandler.complain("##> isVerified001: Could NOT FIND class: " + className);
                class_not_found_errors++;
                continue;
            }
            String s_type = classes_for_check[i][2];
            String s_verified_sign = classes_for_check[i][1];
            boolean isVerified = s_verified_sign.equals("verified");
            if (refType.isVerified() != isVerified) {
                logHandler.complain("##> isVerified001: UNEXPECTED isVerified() method result ("
                                    + !isVerified + ") for " + s_type + ": "
                                    + className + "(" +  s_verified_sign + ")");
                isVerified_method_errors++;
            }
            else {
                print_log_on_verbose("--> isVerified001:  expected isVerified() method result ("
                                    + isVerified + ") for " + s_type + ": "
                                    + className + "(" +  s_verified_sign + ")");
            }
        }
        logHandler.display("--> isVerified001: checking debugee's classes completed!");
        logHandler.display("--> isVerified001: number of checked classes = " + all_classes_count);
        if ( class_not_found_errors > 0 ) {
            logHandler.complain("##> isVerified001: \"class not found ERRORS\" counter = "
                                + class_not_found_errors);
        }
        if (isVerified_method_errors > 0) {
            logHandler.complain("##> isVerified001: isVerified() method ERRORS counter = "
                            + isVerified_method_errors);
        }
        int v_test_result = 0/*STATUS_PASSED*/;
        if (class_not_found_errors + isVerified_method_errors > 0) {
            v_test_result = 2/*STATUS_FAILED*/;
        }

        print_log_on_verbose("--> isVerified001: waiting for debugee finish...");
        pipe.println("quit");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != 0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/) {
            logHandler.complain
                ("##> isVerified001: UNEXPECTED Debugee's exit status (not 95) - " + status);
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose
                ("--> isVerified001: expected Debugee's exit status - " + status);
        }

        return v_test_result;
    }
}
