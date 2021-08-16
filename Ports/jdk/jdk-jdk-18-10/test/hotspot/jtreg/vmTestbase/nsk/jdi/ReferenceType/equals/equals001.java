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

package nsk.jdi.ReferenceType.equals;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * This test checks the method <code>equals()</code>
 * of the JDI interface <code>ReferenceType</code> of com.sun.jdi package
 */

public class equals001 {
    static ArgumentHandler argsHandler;
    static Log test_log_handler;
    static boolean verbose_mode = false;  // test argument -verbose switches to true
                                          // - for more easy failure evaluation

    /** The main class names of the debugger & debugee applications. */
    private final static String
        package_prefix = "nsk.jdi.ReferenceType.equals.",
//        package_prefix = "",    //  for DEBUG without package
        thisClassName = package_prefix + "equals001",
        debugeeName   = thisClassName + "a";

    private final static String primitive_type_sign = "aprimitive_type";
    private final static String reference_type_sign = "areference_type";
    /** Debugee's classes for check **/
    private final static String classes_for_check[][] = {
        {"boolean", primitive_type_sign},
        {"byte"   , primitive_type_sign},
        {"char"   , primitive_type_sign},
        {"double" , primitive_type_sign},
        {"float"  , primitive_type_sign},
        {"int"    , primitive_type_sign},
        {"long"   , primitive_type_sign},

        {"java.lang.Boolean"  , reference_type_sign},
        {"java.lang.Byte"     , reference_type_sign},
        {"java.lang.Character", reference_type_sign},
        {"java.lang.Double"   , reference_type_sign},
        {"java.lang.Float"    , reference_type_sign},
        {"java.lang.Integer"  , reference_type_sign},
        {"java.lang.Long"     , reference_type_sign},
        {"java.lang.String"   , reference_type_sign},
        {"java.lang.Object"   , reference_type_sign},

        {package_prefix + "ClassForCheck", reference_type_sign},
        {package_prefix + "InterfaceForCheck", reference_type_sign},

        {debugeeName+"$s_class", reference_type_sign},
        {debugeeName+"$s_interf", reference_type_sign}
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

        int v_test_result = new equals001().runThis(argv,out);
        if ( v_test_result == 2/*STATUS_FAILED*/ ) {
            print_log_anyway("\n==> nsk/jdi/ReferenceType/equals/equals001 test FAILED");
        }
        else {
            print_log_anyway("\n==> nsk/jdi/ReferenceType/equals/equals001 test PASSED");
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

        print_log_anyway("==> nsk/jdi/ReferenceType/equals/equals001 test LOG:");
        print_log_anyway("==> test checks the equals() method of ReferenceType interface");
        print_log_anyway("    of the com.sun.jdi package for ArrayType, ClassType, InterfaceType\n");

        String debugee_launch_command = debugeeName;

        Binder binder = new Binder(argsHandler,test_log_handler);
        Debugee debugee = binder.bindToDebugee(debugee_launch_command);
        IOPipe pipe = new IOPipe(debugee);

        debugee.redirectStderr(out);
        print_log_on_verbose("--> equals001: equals001a debugee launched");
        debugee.resume();

        String line = pipe.readln();
        if (line == null) {
            print_log_anyway("##> equals001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        if (!line.equals("ready")) {
            print_log_anyway("##> equals001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose("--> equals001: debugee's \"ready\" signal recieved!");
        }

        print_log_anyway
            ("--> equals001: checking debugee's classes by ReferenceType.equals() method...");
        int all_classes_count = 0;
        int class_not_found_errors = 0;
        int equals_method_errors_for_equal_objects = 0;
        int equals_method_errors_for_unequal_objects = 0;
        ReferenceType unequal_refType = null;
        for (int i=0; i<classes_for_check.length; i++) {
            String basicName = classes_for_check[i][0];
            for (int array_measure=0; array_measure<3; array_measure++) {
                if ( array_measure == 0 ) {  // not array type
                    if ( classes_for_check[i][1].equals(primitive_type_sign) ) {
                        continue;
                    }
                }
                all_classes_count++;
                String brackets[] = {"", "[]", "[][]"};
                String className = basicName + brackets[array_measure];
                ReferenceType refType_1 = debugee.classByName(className);
                if (refType_1 == null) {
                    print_log_anyway("##> equals001: Could NOT FIND class: " + className);
                    class_not_found_errors++;
                    continue;
                }
                ReferenceType refType_2 = debugee.classByName(className);
                if ( ! refType_1.equals(refType_2) ) {
                    print_log_anyway("##> equals001: FAILED: refType_1.equals(refType_2) returned FALSE");
                    print_log_anyway("##>            refType_1 = " + refType_1);
                    print_log_anyway("##>            refType_2 = " + refType_2);
                    equals_method_errors_for_equal_objects++;
                }
                else {

                    print_log_on_verbose("--> equals001: PASSED: refType_1.equals(refType_2) returned TRUE");
                    print_log_on_verbose("-->            refType_1 = " + refType_1);
                    print_log_on_verbose("-->            refType_2 = " + refType_2);

                }
                boolean is_equal = true;
                String unequal_refType_str = null;
                if ( unequal_refType == null ) {
                    Object object_for_compare = new Object();
                    is_equal = refType_1.equals(object_for_compare);
                    unequal_refType_str = object_for_compare.toString();
                }
                else {
                    is_equal = refType_1.equals(unequal_refType);
                    unequal_refType_str = unequal_refType.toString();
                }
                if ( is_equal ) {
                    print_log_anyway("##> equals001: FAILED: refType_1.equals(unequal_refType) returned TRUE");
                    print_log_anyway("##>            refType_1 = " + refType_1);
                    print_log_anyway("##>            unequal_refType = " + unequal_refType_str);
                    equals_method_errors_for_unequal_objects++;
                }
                else {

                    print_log_on_verbose("--> equals001: PASSED: refType_1.equals(unequal_refType) returned FALSE");
                    print_log_on_verbose("-->            refType_1 = " + refType_1);
                    print_log_on_verbose("-->            unequal_refType = " + unequal_refType_str);

                }
                unequal_refType = refType_1;
            }
        }
        print_log_anyway("--> equals001: checking debugee's classes completed!");
        print_log_anyway("--> equals001: number of checked classes = " + all_classes_count);
        if ( class_not_found_errors > 0 ) {
            print_log_anyway("##> equals001: \"class not found ERRORS\" number = "
                                + class_not_found_errors);
        }
        print_log_anyway("##> equals001: errors number of equals() method for equal objects = "
                            + equals_method_errors_for_equal_objects);
        print_log_anyway("##> equals001: errors number of equals() method for unequal objects = "
                            + equals_method_errors_for_unequal_objects);
        int v_test_result = 0/*STATUS_PASSED*/;
        if (class_not_found_errors + equals_method_errors_for_equal_objects
                + equals_method_errors_for_unequal_objects > 0) {
            v_test_result = 2/*STATUS_FAILED*/;
        }

        print_log_on_verbose("--> equals001: waiting for debugee finish...");
        pipe.println("quit");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != 0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/) {
            print_log_anyway("##> equals001: UNEXPECTED Debugee's exit status (not 95) - " + status);
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose("--> equals001: expected Debugee's exit status - " + status);
        }

        return v_test_result;
    }
}
