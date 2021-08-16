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

package nsk.jdi.ClassObjectReference.reflectedType;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * This test checks the method <code>reflectedType()</code>
 * of the JDI interface <code>ClassObjectReference</code> of com.sun.jdi package
 */

public class reflectype001 extends Log {
    static java.io.PrintStream out_stream;
    static boolean verbose_mode = false;  // test argument -vbs or -verbose switches to true
                                          // - for more easy failure evaluation

    /** The main class names of the debugger & debugee applications. */
    private final static String
        package_prefix = "nsk.jdi.ClassObjectReference.reflectedType.",
//        package_prefix = "",    //  for DEBUG without package
        thisClassName = package_prefix + "reflectype001",
        debugeeName   = thisClassName + "a";


    static ArgumentHandler      argsHandler;
    private static Log  logHandler;


    /** Debugee's classes for check **/
    private final static String classes_for_check[][] = {

        {"boolean", "primitive_type", "class"},
        {"byte"   , "primitive_type", "class"},
        {"char"   , "primitive_type", "class"},
        {"double" , "primitive_type", "class"},
        {"float"  , "primitive_type", "class"},
        {"int"    , "primitive_type", "class"},
        {"long"   , "primitive_type", "class"},

        {"java.lang.Boolean"  , "reference_type", "class"},
        {"java.lang.Byte"     , "reference_type", "class"},
        {"java.lang.Character", "reference_type", "class"},
        {"java.lang.Double"   , "reference_type", "class"},
        {"java.lang.Float"    , "reference_type", "class"},
        {"java.lang.Integer"  , "reference_type", "class"},
        {"java.lang.Long"     , "reference_type", "class"},
        {"java.lang.String"   , "reference_type", "class"},
        {"java.lang.Object"   , "reference_type", "class"},

        {debugeeName+"$s_class", "reference_type", "class"},  // static class
        {debugeeName+"$s_interf", "reference_type", "interface"},  // static interface

        {package_prefix + "package_class", "reference_type", "class"},  // class
        {package_prefix + "package_interf", "reference_type", "interface"}  // interface

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

        int v_test_result = new reflectype001().runThis(argv,out_stream);
        if ( v_test_result == 2/*STATUS_FAILED*/ ) {
            logHandler.complain("\n==> nsk/jdi/ClassObjectReference/reflectedType/reflectype001 test FAILED");
        }
        else {
            logHandler.display("\n==> nsk/jdi/ClassObjectReference/reflectedType/reflectype001 test PASSED");
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

        logHandler.display("==> nsk/jdi/ClassObjectReference/reflectedType/reflectype001 test LOG:");
        logHandler.display("==> test checks reflectedType() method of ClassObjectReference interface ");
        logHandler.display("    of the com.sun.jdi package for ArraType, ClassType, InterfaceType\n");


        debugee.redirectStderr(out);
        print_log_on_verbose("--> reflectype001: reflectype001a debugee launched");
        debugee.resume();

        String line = pipe.readln();
        if (line == null) {
            logHandler.complain
                ("##> reflectype001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        if (!line.equals("ready")) {
            logHandler.complain
                ("##> reflectype001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose("--> reflectype001: debugee's \"ready\" signal recieved!");
        }

        logHandler.display
            ("--> reflectype001: check ClassObjectReference.reflectedType() method for debugee's classes...");
        int all_classes_count = 0;
        int class_not_found_errors = 0;
        int classObject_method_errors = 0;
        for (int i=0; i<classes_for_check.length; i++) {
            String basicName = classes_for_check[i][0];
            for (int array_measure=0; array_measure<3; array_measure++) {
                if ( array_measure == 0 ) {  // not array type
                    if ( classes_for_check[i][1].equals("primitive_type") ) {
                        continue;
                    }
                }
                all_classes_count++;
                String brackets[] = {"", "[]", "[][]"};
                String className = basicName + brackets[array_measure];
                ReferenceType refType = debugee.classByName(className);
                if (refType == null) {
                    logHandler.complain("##> reflectype001: Could NOT FIND class: " + className);
                    class_not_found_errors++;
                    continue;
                }
                String s_type = classes_for_check[i][2];
                if ( array_measure != 0 ) {  // array type
                    s_type = "class";
                }
                ClassObjectReference class_obj_ref = refType.classObject();
                ReferenceType reflected_refType = class_obj_ref.reflectedType();
                if ( ! refType.equals(reflected_refType) ) {
                    logHandler.complain
                        ("##> reflectype001: FAILED: source ReferenceType object is NOT equal to reflected object");
                    logHandler.complain
                        ("##>              for " + s_type + ": " + className);
                    classObject_method_errors++;
                }
                else {
                    print_log_on_verbose
                        ("--> reflectype001: PASSED: source ReferenceType object is equal to reflected object");
                    print_log_on_verbose
                        ("-->              for " + s_type + ": " + className);
                }
            }
        }
        logHandler.display("--> reflectype001: check completed!");
        logHandler.display("--> reflectype001: number of checked classes = " + all_classes_count);
        if ( class_not_found_errors > 0 ) {
            logHandler.complain("##> reflectype001: \"class not found ERRORS\" counter = "
                                + class_not_found_errors);
        }
        logHandler.display("##> reflectype001: classObject() method ERRORS counter = "
                            + classObject_method_errors);
        int v_test_result = 0/*STATUS_PASSED*/;
        if (class_not_found_errors + classObject_method_errors > 0) {
            v_test_result = 2/*STATUS_FAILED*/;
        }

        print_log_on_verbose("--> reflectype001: waiting for debugee finish...");
        pipe.println("quit");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != 0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/) {
            logHandler.complain
                ("##> reflectype001: UNEXPECTED Debugee's exit status (not 95) - " + status);
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose
                ("--> reflectype001: expected Debugee's exit status - " + status);
        }

        return v_test_result;
    }
}
