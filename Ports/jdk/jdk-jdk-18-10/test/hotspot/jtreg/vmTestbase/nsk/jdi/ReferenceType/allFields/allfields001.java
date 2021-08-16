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

package nsk.jdi.ReferenceType.allFields;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * This test checks the method <code>allFields()</code>
 * of the JDI interface <code>ReferenceType</code> of com.sun.jdi package
 */

public class allfields001 extends Log {
    static java.io.PrintStream out_stream;
    static boolean verbose_mode = false;  // test argument -vbs or -verbose switches to static
                                          // - for more easy failure evaluation

    /** The main class names of the debugger & debugee applications. */
    private final static String
        package_prefix = "nsk.jdi.ReferenceType.allFields.",
//        package_prefix = "",    //  for DEBUG without package
        thisClassName = package_prefix + "allfields001",
        debugeeName   = thisClassName + "a";

    /** Debugee's classes for check **/
    private final static String class_for_check = package_prefix + "allfields001aClassForCheck";
    private final static String super_class_for_check = package_prefix + "allfields001aSuperClassForCheck";
    private final static String interf_for_check = package_prefix + "allfields001aInterfaceForCheck";
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
        {"s_interf_hidden_ref_field",  "java.lang.Object",   " static", class_for_check,       not_found_sign },
        {"s_super_boolean_field",      "boolean",            " static", super_class_for_check, not_found_sign },
        {"s_super_byte_field",         "byte",               " static", super_class_for_check, not_found_sign },
        {"s_super_char_field",         "char",               " static", super_class_for_check, not_found_sign },
        {"s_super_double_field",       "double",             " static", super_class_for_check, not_found_sign },
        {"s_super_float_field",        "float",              " static", super_class_for_check, not_found_sign },
        {"s_super_int_field",          "int",                " static", super_class_for_check, not_found_sign },
        {"s_super_long_field",         "long",               " static", super_class_for_check, not_found_sign },
        {"s_super_object_field",       "java.lang.Object",   " static", super_class_for_check, not_found_sign },
        {"s_super_hidden_prim_field",  "long",               " static", super_class_for_check, not_found_sign },
        {"s_super_hidden_ref_field",   "java.lang.Object",   " static", super_class_for_check, not_found_sign },
        {"i_super_boolean_field",      "boolean",            "",        super_class_for_check, not_found_sign },
        {"i_super_byte_field",         "byte",               "",        super_class_for_check, not_found_sign },
        {"i_super_char_field",         "char",               "",        super_class_for_check, not_found_sign },
        {"i_super_double_field",       "double",             "",        super_class_for_check, not_found_sign },
        {"i_super_float_field",        "float",              "",        super_class_for_check, not_found_sign },
        {"i_super_int_field",          "int",                "",        super_class_for_check, not_found_sign },
        {"i_super_long_field",         "long",               "",        super_class_for_check, not_found_sign },
        {"i_super_object_field",       "java.lang.Object",   "",        super_class_for_check, not_found_sign },
        {"i_super_hidden_prim_field",  "long",               "",        super_class_for_check, not_found_sign },
        {"i_super_hidden_ref_field",   "java.lang.Object",   "",        super_class_for_check, not_found_sign },
        {"ambiguous_prim_field",       "long",               "",        super_class_for_check, not_found_sign },
        {"ambiguous_ref_field",        "java.lang.Object",   "",        super_class_for_check, not_found_sign },
        {"s_interf_boolean_field",     "boolean",            " static", interf_for_check,      not_found_sign },
        {"s_interf_byte_field",        "byte",               " static", interf_for_check,      not_found_sign },
        {"s_interf_char_field",        "char",               " static", interf_for_check,      not_found_sign },
        {"s_interf_double_field",      "double",             " static", interf_for_check,      not_found_sign },
        {"s_interf_float_field",       "float",              " static", interf_for_check,      not_found_sign },
        {"s_interf_int_field",         "int",                " static", interf_for_check,      not_found_sign },
        {"s_interf_long_field",        "long",               " static", interf_for_check,      not_found_sign },
        {"s_interf_object_field",      "java.lang.Object",   " static", interf_for_check,      not_found_sign },
        {"s_interf_hidden_prim_field", "long",               " static", interf_for_check,      not_found_sign },
        {"s_interf_hidden_ref_field",  "java.lang.Object",   " static", interf_for_check,      not_found_sign },
        {"ambiguous_prim_field",       "long",               " static", interf_for_check,      not_found_sign },
        {"ambiguous_ref_field",        "java.lang.Object",   " static", interf_for_check,      not_found_sign }

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

        int v_test_result = new allfields001().runThis(argv,out_stream);
        if ( v_test_result == 2/*STATUS_FAILED*/ ) {
            out_stream.println("\n==> nsk/jdi/ReferenceType/allFields/allfields001 test FAILED");
        }
        else {
            out_stream.println("\n==> nsk/jdi/ReferenceType/allFields/allfields001 test PASSED");
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

        out_stream.println("==> nsk/jdi/ReferenceType/allFields/allfields001 test LOG:");
        out_stream.println("==> test checks allFields() method of ReferenceType interface ");
        out_stream.println("    of the com.sun.jdi package\n");

        String debugee_launch_command = debugeeName;
        if (verbose_mode) {
            logTo(out_stream);
        }

        Binder binder = new Binder(argHandler,this);
        Debugee debugee = binder.bindToDebugee(debugee_launch_command);
        IOPipe pipe = new IOPipe(debugee);

        debugee.redirectStderr(out);
        print_log_on_verbose("--> allfields001: allfields001a debugee launched");
        debugee.resume();

        String line = pipe.readln();
        if (line == null) {
            out_stream.println
                ("##> allfields001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        if (!line.equals("ready")) {
            out_stream.println
                ("##> allfields001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose("--> allfields001: debugee's \"ready\" signal recieved!");
        }

        out_stream.println
            ("--> allfields001: check ReferenceType.allFields() method for debugee's "
            + class_for_check + " class...");
        boolean class_not_found_error = false;
        boolean allFields_method_error = false;
        int fields_for_check_number = fields_for_check.length;
        int not_found_fields_number = 0;
        int all_fields_number = 0;
        int unexpected_found_fields_number = 0;

        while ( true ) {
            ReferenceType refType = debugee.classByName(class_for_check);
            if (refType == null) {
                out_stream.println("##> allfields001: Could NOT FIND class: " + class_for_check);
                class_not_found_error = true;
                break;
            }
            List<Field> all_fields_list = null;
            try {
                all_fields_list = refType.allFields();
            }
            catch (Throwable thrown) {
                out_stream.println("##> allfields001: FAILED: ReferenceType.allFields() throws unexpected "
                    + thrown);
                allFields_method_error = true;
                break;
            }
            all_fields_number = all_fields_list.size();
            Field all_fields[] = new Field[all_fields_number];
            String unexpected_all_fields[] = new String[all_fields_number];
            all_fields_list.toArray(all_fields);
            for (int i=0; i<all_fields_number; i++) {
                Field checked_field = all_fields[i];
                String checked_field_name = checked_field.name();
                String checked_field_typename = checked_field.typeName();
                String checked_field_static;
                if ( checked_field.isStatic() ) {
                    checked_field_static = " static";
                }
                else {
                    checked_field_static = "";
                }
                String declaring_class_name = "declaring class NOT defined";
                try {
                    declaring_class_name = checked_field.declaringType().name();
                }
                catch (Throwable thrown) {
                }
                String full_checked_field = checked_field_static
                    + " " + checked_field_typename + " " + checked_field_name
                    + "  (" + declaring_class_name + ")";
                int j=0;
                for (; j<fields_for_check_number; j++) {
                    if ( checked_field_name.equals(fields_for_check[j][0]) ) {
                        if ( (checked_field_typename.equals(fields_for_check[j][1]))
                                && (checked_field_static.equals(fields_for_check[j][2]))
                                && (declaring_class_name.equals(fields_for_check[j][3])) ) {
                            fields_for_check[j][4] = passed_sign;
                            break;
                        }
                    }
                }
                if ( j == fields_for_check_number ) {
                    //  unexpected field found
                    unexpected_all_fields[unexpected_found_fields_number] = full_checked_field;
                    unexpected_found_fields_number++;
                }
            }
            for (int i=0; i<fields_for_check_number; i++) {
                String current_field_for_check = fields_for_check[i][2]
                    + " " + fields_for_check[i][1] + " " + fields_for_check[i][0]
                    + "  (" + fields_for_check[i][3] + ")";
                if ( fields_for_check[i][4].equals(not_found_sign) ) {
                    out_stream.println
                        ("##> allfields001: FAILED: field is NOT found: " + current_field_for_check);
                    not_found_fields_number++;
                }
                else {
                    print_log_on_verbose
                        ("--> allfields001: PASSED for field: " + current_field_for_check);
                }
            }
            for (int i=0; i<unexpected_found_fields_number; i++) {
                out_stream.println
                    ("##> allfields001: FAILED: unexpected found field: " + unexpected_all_fields[i]);
            }
            break;
        }

        out_stream.println("--> allfields001: check completed!");
        int v_test_result = 0/*STATUS_PASSED*/;
        if ( class_not_found_error || allFields_method_error ) {
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            out_stream.println("--> allfields001: expected found fields number = " + fields_for_check_number);
            out_stream.println("--> allfields001: in fact found fields number  = " + all_fields_number);
            out_stream.println("--> allfields001: expected and in fact found fields number  = "
                + (fields_for_check_number - not_found_fields_number));
            out_stream.println
                ("##> allfields001: NOT found fields number = " + not_found_fields_number);
            out_stream.println
                ("##> allfields001: UNEXPECTED found fields number = " + unexpected_found_fields_number);
        }
        if ( not_found_fields_number + unexpected_found_fields_number > 0 ) {
            v_test_result = 2/*STATUS_FAILED*/;
        }

        print_log_on_verbose("--> allfields001: waiting for debugee finish...");
        pipe.println("quit");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != 0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/) {
            out_stream.println
                ("##> allfields001: UNEXPECTED Debugee's exit status (not 95) - " + status);
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose
                ("--> allfields001: expected Debugee's exit status - " + status);
        }

        return v_test_result;
    }
}
