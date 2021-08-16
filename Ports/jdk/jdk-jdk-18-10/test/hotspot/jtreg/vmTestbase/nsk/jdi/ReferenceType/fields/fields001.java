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

public class fields001 {
    static ArgumentHandler argsHandler;
    static Log test_log_handler;
    static boolean verbose_mode = false;  // test argument -verbose switches to true
                                          // - for more easy failure evaluation

    /** The main class names of the debugger & debugee applications. */
    private final static String
        package_prefix = "nsk.jdi.ReferenceType.fields.",
//        package_prefix = "",    //  for DEBUG without package
        thisClassName = package_prefix + "fields001",
        debugeeName   = thisClassName + "a";

    /** Debugee's classes for check **/
    private final static String class_for_check = package_prefix + "fields001aClassForCheck";
    private final static String not_found_sign = "NOT FOUND";
    private final static String passed_sign = "PASSED";
    private static String fields_for_check[][] = {

//        field name                    type name             static    declaring class        check result

        {"s_boolean_field",            "boolean",            " static", class_for_check,       not_found_sign },
        {"s_byte_field",               "byte",               " static", class_for_check,       not_found_sign },
        {"s_char_field",               "char",               " static", class_for_check,       not_found_sign },
        {"s_double_field",             "double",             " static", class_for_check,       not_found_sign },
        {"s_float_field",              "float",              " static", class_for_check,       not_found_sign },
        {"s_int_field",                "int",                " static", class_for_check,       not_found_sign },
        {"s_long_field",               "long",               " static", class_for_check,       not_found_sign },
        {"s_object_field",             "java.lang.Object",   " static", class_for_check,       not_found_sign },
        {"s_prim_array_field",         "long[]",             " static", class_for_check,       not_found_sign },
        {"s_ref_array_field",          "java.lang.Object[]", " static", class_for_check,       not_found_sign },
        {"i_boolean_field",            "boolean",            "",        class_for_check,       not_found_sign },
        {"i_byte_field",               "byte",               "",        class_for_check,       not_found_sign },
        {"i_char_field",               "char",               "",        class_for_check,       not_found_sign },
        {"i_double_field",             "double",             "",        class_for_check,       not_found_sign },
        {"i_float_field",              "float",              "",        class_for_check,       not_found_sign },
        {"i_int_field",                "int",                "",        class_for_check,       not_found_sign },
        {"i_long_field",               "long",               "",        class_for_check,       not_found_sign },
        {"i_object_field",             "java.lang.Object",   "",        class_for_check,       not_found_sign },
        {"i_prim_array_field",         "long[]",             "",        class_for_check,       not_found_sign },
        {"i_ref_array_field",          "java.lang.Object[]", "",        class_for_check,       not_found_sign },
        {"s_super_hidden_prim_field",  "long",               " static", class_for_check,       not_found_sign },
        {"s_super_hidden_ref_field",   "java.lang.Object",   " static", class_for_check,       not_found_sign },
        {"i_super_hidden_prim_field",  "long",               " static", class_for_check,       not_found_sign },
        {"i_super_hidden_ref_field",   "java.lang.Object",   " static", class_for_check,       not_found_sign },
        {"s_interf_hidden_prim_field", "long",               " static", class_for_check,       not_found_sign },
        {"s_interf_hidden_ref_field",  "java.lang.Object",   " static", class_for_check,       not_found_sign }

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

        int v_test_result = new fields001().runThis(argv,out);
        if ( v_test_result == 2/*STATUS_FAILED*/ ) {
            print_log_anyway("\n==> nsk/jdi/ReferenceType/fields/fields001 test FAILED");
        }
        else {
            print_log_anyway("\n==> nsk/jdi/ReferenceType/fields/fields001 test PASSED");
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

        print_log_anyway("==> nsk/jdi/ReferenceType/fields/fields001 test LOG:");
        print_log_anyway("==> test checks fields() method of ReferenceType interface ");
        print_log_anyway("    of the com.sun.jdi package\n");

        String debugee_launch_command = debugeeName;

        Binder binder = new Binder(argsHandler,test_log_handler);
        Debugee debugee = binder.bindToDebugee(debugee_launch_command);
        IOPipe pipe = new IOPipe(debugee);

        debugee.redirectStderr(out);
        print_log_on_verbose("--> fields001: fields001a debugee launched");
        debugee.resume();

        String line = pipe.readln();
        if (line == null) {
            print_log_anyway
                ("##> fields001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        if (!line.equals("ready")) {
            print_log_anyway
                ("##> fields001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose("--> fields001: debugee's \"ready\" signal recieved!");
        }

        print_log_anyway
            ("--> fields001: check ReferenceType.fields() method for debugee's "
            + class_for_check + " class...");
        boolean class_not_found_error = false;
        boolean fields_method_error = false;
        int fields_for_check_number = fields_for_check.length;
        int not_found_fields_number = 0;
        int returned_fields_number = 0;
        int unexpected_found_fields_number = 0;

        while ( true ) {
            ReferenceType refType = debugee.classByName(class_for_check);
            if (refType == null) {
                print_log_anyway("##> fields001: Could NOT FIND class: " + class_for_check);
                class_not_found_error = true;
                break;
            }
            List<Field> returned_fields_list = null;
            try {
                returned_fields_list = refType.fields();
            }
            catch (Throwable thrown) {
                print_log_anyway("##> fields001: FAILED: ReferenceType.fields() throws unexpected "
                    + thrown);
                fields_method_error = true;
                break;
            }
            returned_fields_number = returned_fields_list.size();
            Field returned_fields_array[] = new Field[returned_fields_number];
            String unexpected_returned_fields_array[] = new String[returned_fields_number];
            returned_fields_list.toArray(returned_fields_array);
            for (int i=0; i<returned_fields_number; i++) {
                Field returned_field = returned_fields_array[i];
                String returned_field_name = returned_field.name();
                String returned_field_typename = returned_field.typeName();
                String returned_field_static;
                if ( returned_field.isStatic() ) {
                    returned_field_static = " static";
                }
                else {
                    returned_field_static = "";
                }
                String returned_declaring_class = "declaring class NOT defined";
                try {
                    returned_declaring_class = returned_field.declaringType().name();
                }
                catch (Throwable thrown) {
                }
                String returned_field_info = returned_field_static
                    + " " + returned_field_typename + " " + returned_field_name
                    + "  (" + returned_declaring_class + ")";
                int j=0;
                for (; j<fields_for_check_number; j++) {
                    if ( returned_field_name.equals(fields_for_check[j][0]) ) {
                        if ( (returned_field_typename.equals(fields_for_check[j][1]))
                                && (returned_field_static.equals(fields_for_check[j][2]))
                                && (returned_declaring_class.equals(fields_for_check[j][3])) ) {
                            fields_for_check[j][4] = passed_sign;
                            break;
                        }
                    }
                }
                if ( j == fields_for_check_number ) {
                    //  unexpected field found
                    unexpected_returned_fields_array[unexpected_found_fields_number] = returned_field_info;
                    unexpected_found_fields_number++;
                }
            }
            for (int i=0; i<fields_for_check_number; i++) {
                String field_for_check_info = fields_for_check[i][2]
                    + " " + fields_for_check[i][1] + " " + fields_for_check[i][0]
                    + "  (" + fields_for_check[i][3] + ")";
                if ( fields_for_check[i][4].equals(not_found_sign) ) {
                    print_log_anyway
                        ("##> fields001: FAILED: field is NOT found: " + field_for_check_info);
                    not_found_fields_number++;
                }
                else {
                    print_log_on_verbose
                        ("--> fields001: PASSED for field: " + field_for_check_info);
                }
            }
            for (int i=0; i<unexpected_found_fields_number; i++) {
                print_log_anyway
                    ("##> fields001: FAILED: unexpected found field: " + unexpected_returned_fields_array[i]);
            }
            break;
        }

        print_log_anyway("--> fields001: check completed!");
        int v_test_result = 0/*STATUS_PASSED*/;
        if ( class_not_found_error || fields_method_error ) {
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_anyway("--> fields001: expected found fields number = " + fields_for_check_number);
            print_log_anyway("--> fields001: in fact found fields number  = " + returned_fields_number);
            print_log_anyway("--> fields001: expected and in fact found fields number  = "
                + (fields_for_check_number - not_found_fields_number));
            print_log_anyway
                ("##> fields001: NOT found fields number = " + not_found_fields_number);
            print_log_anyway
                ("##> fields001: UNEXPECTED found fields number = " + unexpected_found_fields_number);
        }
        if ( not_found_fields_number + unexpected_found_fields_number > 0 ) {
            v_test_result = 2/*STATUS_FAILED*/;
        }

        print_log_on_verbose("--> fields001: waiting for debugee finish...");
        pipe.println("quit");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != 0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/) {
            print_log_anyway
                ("##> fields001: UNEXPECTED Debugee's exit status (not 95) - " + status);
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose
                ("--> fields001: expected Debugee's exit status - " + status);
        }

        return v_test_result;
    }
}
