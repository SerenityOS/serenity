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

public class sourcename001 {
    static ArgumentHandler argsHandler;
    static Log test_log_handler;
    static boolean verbose_mode = false;  // test argument -verbose switches to true
                                          // - for more easy failure evaluation

    /** The main class sourceNames of the debugger & debugee applications. */
    private final static String
        package_prefix = "nsk.jdi.ReferenceType.sourceName.",
//        package_prefix = "",    //  for DEBUG without package
        thisClassName = package_prefix + "sourcename001",
        debugeeName   = thisClassName + "a";

        static String this_class_source_name = "sourcename001.java";
        static String debugee_source_name   = "sourcename001a.java";

    /** Debugee's classes for check **/
    private final static String classes_for_check[][] = {

        {thisClassName, this_class_source_name},
        {debugeeName,   debugee_source_name},

        {package_prefix + "ClassForCheck", debugee_source_name},
        {package_prefix + "InterfaceForCheck", debugee_source_name},

        {debugeeName+"$s_class", debugee_source_name},
        {debugeeName+"$s_interf", debugee_source_name}
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

        int v_test_result = new sourcename001().runThis(argv,out);
        if ( v_test_result == 2/*STATUS_FAILED*/ ) {
            print_log_anyway("\n==> nsk/jdi/ReferenceType/sourceName/sourcename001 test FAILED");
        }
        else {
            print_log_anyway("\n==> nsk/jdi/ReferenceType/sourceName/sourcename001 test PASSED");
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

        print_log_anyway("==> nsk/jdi/ReferenceType/sourceName/sourcename001 test LOG:");
        print_log_anyway("==> test checks the sourceName() method of ReferenceType interface");
        print_log_anyway("    of the com.sun.jdi package for ClassType, InterfaceType\n");

        String debugee_launch_command = debugeeName;

        Binder binder = new Binder(argsHandler,test_log_handler);
        Debugee debugee = binder.bindToDebugee(debugee_launch_command);
        IOPipe pipe = new IOPipe(debugee);

        debugee.redirectStderr(out);
        print_log_on_verbose("--> sourcename001: sourcename001a debugee launched");
        debugee.resume();

        String line = pipe.readln();
        if (line == null) {
            print_log_anyway("##> sourcename001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        if (!line.equals("ready")) {
            print_log_anyway("##> sourcename001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose("--> sourcename001: debugee's \"ready\" signal recieved!");
        }

        print_log_anyway
            ("--> sourcename001: check ReferenceType.sourceName() method for debugee's classes...");
        int all_classes_count = 0;
        int class_not_found_errors = 0;
        int sourceName_method_exceptions = 0;
        int sourceName_method_errors = 0;
        for (int i=0; i<classes_for_check.length; i++) {
            String className = classes_for_check[i][0];
            String source_class_name = classes_for_check[i][1];
            all_classes_count++;
            ReferenceType refType = debugee.classByName(className);
            if (refType == null) {
                print_log_anyway("##> sourcename001: Could NOT FIND class: " + className);
                class_not_found_errors++;
                continue;
            }
            String ref_type_source_name = null;
            try {
                ref_type_source_name = refType.sourceName();
            }
            catch (Throwable thrown) {
                print_log_anyway
                    ("##> sourcename001: FAILED: refType.sourceName() threw unexpected exception - "
                    + thrown);
                print_log_anyway
                    ("##>                refType = " + refType);
                sourceName_method_exceptions++;
                continue;
            }
            if ( ! ref_type_source_name.equals(source_class_name) ) {
                print_log_anyway
                    ("##> sourcename001: FAILED: ReferenceType.sourceName() returned unexpected source name:");
                print_log_anyway
                    ("##>                expected source name = " + source_class_name);
                print_log_anyway
                    ("##>                returned source name = " + ref_type_source_name);
                sourceName_method_errors++;
            }
            else {
                print_log_on_verbose
                    ("--> sourcename001: PASSED: ReferenceType.sourceName() returned expected source name:");
                print_log_on_verbose
                    ("-->                checked class source name = " + source_class_name);
                print_log_on_verbose
                    ("-->                returned source name      = " + ref_type_source_name);
            }
        }
        print_log_anyway("--> sourcename001: check completed!");
        print_log_anyway("--> sourcename001: number of checked classes = " + all_classes_count);
        if ( class_not_found_errors > 0 ) {
            print_log_anyway("##> sourcename001: \"class not found ERRORS\" number = "
                                + class_not_found_errors);
        }
        if ( sourceName_method_exceptions > 0 ) {
            print_log_anyway("##> sourcename001: number of unexpected sourceName() methods exceptions = "
                                + sourceName_method_exceptions);
        }
        print_log_anyway("##> sourcename001: sourceName() method errors number = "
                            + sourceName_method_errors);
        int v_test_result = 0/*STATUS_PASSED*/;
        if (class_not_found_errors + sourceName_method_errors + sourceName_method_exceptions > 0) {
            v_test_result = 2/*STATUS_FAILED*/;
        }

        print_log_on_verbose("--> sourcename001: waiting for debugee finish...");
        pipe.println("quit");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != 0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/) {
            print_log_anyway("##> sourcename001: UNEXPECTED Debugee's exit status (not 95) - " + status);
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose("--> sourcename001: expected Debugee's exit status - " + status);
        }

        return v_test_result;
    }
}
