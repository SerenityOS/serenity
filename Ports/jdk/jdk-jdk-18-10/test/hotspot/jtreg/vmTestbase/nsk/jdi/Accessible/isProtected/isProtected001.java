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

package nsk.jdi.Accessible.isProtected;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * This test checks the method <code>isProtected()</code>
 * of the JDI interface <code>Accessible</code> of com.sun.jdi package
 * for ArrayType, ClassType, InterfaceType
 */

public class isProtected001 extends Log {
    static java.io.PrintStream out_stream;
    static boolean verbose_mode = false;

    /** The main class names of the debugger & debugee applications. */
    private final static String
        package_prefix = "nsk.jdi.Accessible.isProtected.",
//        package_prefix = "",    //  for DEBUG without package
        thisClassName = package_prefix + "isProtected001",
        debugeeName   = thisClassName + "a";


    static ArgumentHandler      argsHandler;
    private static Log  logHandler;


    /** Debugee's classes for check **/
    private final static String classes_for_check[][] = {
        {"boolean", "public", "primitive_type"},
        {"byte"   , "public", "primitive_type"},
        {"char"   , "public", "primitive_type"},
        {"double" , "public", "primitive_type"},
        {"float"  , "public", "primitive_type"},
        {"int"    , "public", "primitive_type"},
        {"long"   , "public", "primitive_type"},

        {"java.lang.Boolean"  , "public", "reference_type"},
        {"java.lang.Byte"     , "public", "reference_type"},
        {"java.lang.Character", "public", "reference_type"},
        {"java.lang.Double"   , "public", "reference_type"},
        {"java.lang.Float"    , "public", "reference_type"},
        {"java.lang.Integer"  , "public", "reference_type"},
        {"java.lang.Long"     , "public", "reference_type"},
        {"java.lang.String"   , "public", "reference_type"},
        {"java.lang.Object"   , "public", "reference_type"},

        {thisClassName, "public", "reference_type"             },
        {thisClassName+"a", "public", "reference_type"         },
        {package_prefix + "pack_priv_cls", "package private", "reference_type"},  // class
        {package_prefix + "pack_priv_interf", "package private", "reference_type"},  // interface

        {debugeeName+"$s_interf", "protected", "reference_type"},  // static interface

        {debugeeName+"$U", "private", "reference_type"        },
        {debugeeName+"$V", "protected", "reference_type"      },
        {debugeeName+"$W", "public", "reference_type"         },
        {debugeeName+"$P", "package private", "reference_type"}
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

        int v_test_result = new isProtected001().runThis(argv,out_stream);
        if ( v_test_result == 2/*STATUS_FAILED*/ ) {
            logHandler.complain("\n==> nsk/jdi/Accessible/isProtected/isProtected001 test FAILED");
        }
        else {
            logHandler.display("\n==> nsk/jdi/Accessible/isProtected/isProtected001 test PASSED");
        }
        return v_test_result;
    }

    private void print_log_on_verbose(String message) {
        logHandler.display(message);
    }

    /**
     * Non-static variant of the method <code>run(args,out)</code>
     */
    private int runThis (String argv[], PrintStream out) {

        if ( out_stream == null ) {
            out_stream = out;
        }


        Debugee debugee;

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);


        if (argsHandler.verbose()) {
            debugee = binder.bindToDebugee(debugeeName + " -vbs");
        } else {
            debugee = binder.bindToDebugee(debugeeName);
        }

        IOPipe pipe     = new IOPipe(debugee);


        logHandler.display("==> nsk/jdi/Accessible/isProtected/isProtected001 test LOG:");
        logHandler.display("==> test checks the isProtected() method of Accessible interface");
        logHandler.display("    of the com.sun.jdi package for ArrayType, ClassType, InterfaceType\n");


        debugee.redirectStderr(out);
        print_log_on_verbose("--> isProtected001: isProtected001a debugee launched");
        debugee.resume();

        String line = pipe.readln();
        if (line == null) {
            logHandler.complain("##> isProtected001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        if (!line.equals("ready")) {
            logHandler.complain
                ("##> isProtected001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose("--> isProtected001: debugee's \"ready\" signal recieved!");
        }

        logHandler.display
            ("--> isProtected001: checking debugee's classes by Accessible.isProtected() method...");
        int all_classes_count = 0;
        int class_not_found_errors = 0;
        int isProtected_method_errors = 0;
        for (int i=0; i<classes_for_check.length; i++) {
            String basicName = classes_for_check[i][0];
            for (int array_measure=0; array_measure<3; array_measure++) {
                if ( array_measure == 0 ) {  // not array type
                    if ( classes_for_check[i][2].equals("primitive_type") ) {
                        continue;
                    }
                }
                all_classes_count++;
                String brackets[] = {"", "[]", "[][]"};
                String className = basicName + brackets[array_measure];
                ReferenceType refType = debugee.classByName(className);
                if (refType == null) {
                    logHandler.complain("##> isProtected001: Could NOT FIND class: " + className);
                    class_not_found_errors++;
                    continue;
                }
                boolean isProtected = classes_for_check[i][1].equals("protected");
                if (refType.isProtected() != isProtected) {
                    logHandler.complain("##> isProtected001: UNEXPECTED isProtected() method result ("
                                        + !isProtected + ") for class: "
                                        + className + "(" +  classes_for_check[i][1] + ")");
                    isProtected_method_errors++;
                }
                else {
                    print_log_on_verbose("--> isProtected001:  expected isProtected() method result ("
                                        + isProtected + ") for class: "
                                        + className + "(" +  classes_for_check[i][1] + ")");
                }
            }
        }
        logHandler.display("--> isProtected001: checking debugee's classes completed!");
        logHandler.display("--> isProtected001: number of checked classes = " + all_classes_count);
        if ( class_not_found_errors > 0 ) {
            logHandler.complain("##> isProtected001: \"class not found ERRORS\" counter = "
                                + class_not_found_errors);
        }
        if ( isProtected_method_errors > 0 ) {
            logHandler.complain("##> isProtected001: isProtected() method ERRORS counter = "
                            + isProtected_method_errors);
        }
        int v_test_result = 0/*STATUS_PASSED*/;
        if (class_not_found_errors + isProtected_method_errors > 0) {
            v_test_result = 2/*STATUS_FAILED*/;
        }

        print_log_on_verbose("--> isProtected001: waiting for debugee finish...");
        pipe.println("quit");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != 0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/) {
            logHandler.complain
                ("##> isProtected001: UNEXPECTED Debugee's exit status (not 95) - " + status);
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose
                ("--> isProtected001: expected Debugee's exit status - " + status);
        }

        return v_test_result;
    }
}
