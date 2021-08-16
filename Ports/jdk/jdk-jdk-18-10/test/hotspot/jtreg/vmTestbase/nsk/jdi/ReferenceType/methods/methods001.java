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

package nsk.jdi.ReferenceType.methods;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * This test checks the method <code>methods()</code>
 * of the JDI interface <code>ReferenceType</code> of com.sun.jdi package
 */

public class methods001 {
    static ArgumentHandler argsHandler;
    static Log test_log_handler;
    static boolean verbose_mode = false;  // test argument -verbose switches to true
                                          // - for more easy failure evaluation

    /** The main class names of the debugger & debugee applications. */
    private final static String
        package_prefix = "nsk.jdi.ReferenceType.methods.",
//        package_prefix = "",    //  for DEBUG without package
        thisClassName = package_prefix + "methods001",
        debugeeName   = thisClassName + "a";

    /** Debugee's classes for check **/
    private final static String class_for_check = package_prefix + "methods001aClassForCheck";
    private final static String not_found_sign = "NOT FOUND";
    private final static String passed_sign = "PASSED";
    private static String methods_for_check[][] = {

//        method name                            declaring class        check result

        {"<init>",                               class_for_check,       not_found_sign},
        {"ClassForCheck",                        class_for_check,       not_found_sign},
        {"s_void_method",                        class_for_check,       not_found_sign},
        {"s_boolean_method",                     class_for_check,       not_found_sign},
        {"s_byte_method",                        class_for_check,       not_found_sign},
        {"s_char_method",                        class_for_check,       not_found_sign},
        {"s_double_method",                      class_for_check,       not_found_sign},
        {"s_float_method",                       class_for_check,       not_found_sign},
        {"s_int_method",                         class_for_check,       not_found_sign},
        {"s_long_method",                        class_for_check,       not_found_sign},
        {"s_string_method",                      class_for_check,       not_found_sign},
        {"s_object_method",                      class_for_check,       not_found_sign},
        {"s_prim_array_method",                  class_for_check,       not_found_sign},
        {"s_ref_array_method",                   class_for_check,       not_found_sign},
        {"s_super_hidden_void_method",           class_for_check,       not_found_sign},
        {"s_super_hidden_prim_method",           class_for_check,       not_found_sign},
        {"s_super_hidden_ref_method",            class_for_check,       not_found_sign},
        {"s_void_par_method",                    class_for_check,       not_found_sign},
        {"s_boolean_par_method",                 class_for_check,       not_found_sign},
        {"s_byte_par_method",                    class_for_check,       not_found_sign},
        {"s_char_par_method",                    class_for_check,       not_found_sign},
        {"s_double_par_method",                  class_for_check,       not_found_sign},
        {"s_float_par_method",                   class_for_check,       not_found_sign},
        {"s_int_par_method",                     class_for_check,       not_found_sign},
        {"s_long_par_method",                    class_for_check,       not_found_sign},
        {"s_string_par_method",                  class_for_check,       not_found_sign},
        {"s_object_par_method",                  class_for_check,       not_found_sign},
        {"s_prim_array_par_method",              class_for_check,       not_found_sign},
        {"s_ref_array_par_method",               class_for_check,       not_found_sign},
        {"s_super_hidden_void_par_method",       class_for_check,       not_found_sign},
        {"s_super_hidden_prim_par_method",       class_for_check,       not_found_sign},
        {"s_super_hidden_ref_par_method",        class_for_check,       not_found_sign},
        {"s_native_method",                      class_for_check,       not_found_sign},
        {"s_synchr_method",                      class_for_check,       not_found_sign},
        {"s_final_method",                       class_for_check,       not_found_sign},
        {"s_private_method",                     class_for_check,       not_found_sign},
        {"s_protected_method",                   class_for_check,       not_found_sign},
        {"s_public_method",                      class_for_check,       not_found_sign},
        {"i_void_method",                        class_for_check,       not_found_sign},
        {"i_boolean_method",                     class_for_check,       not_found_sign},
        {"i_byte_method",                        class_for_check,       not_found_sign},
        {"i_char_method",                        class_for_check,       not_found_sign},
        {"i_double_method",                      class_for_check,       not_found_sign},
        {"i_float_method",                       class_for_check,       not_found_sign},
        {"i_int_method",                         class_for_check,       not_found_sign},
        {"i_long_method",                        class_for_check,       not_found_sign},
        {"i_string_method",                      class_for_check,       not_found_sign},
        {"i_object_method",                      class_for_check,       not_found_sign},
        {"i_prim_array_method",                  class_for_check,       not_found_sign},
        {"i_ref_array_method",                   class_for_check,       not_found_sign},
        {"i_super_overridden_void_method",       class_for_check,       not_found_sign},
        {"i_super_overridden_prim_method",       class_for_check,       not_found_sign},
        {"i_super_overridden_ref_method",        class_for_check,       not_found_sign},
        {"i_interf_overridden_void_method",      class_for_check,       not_found_sign},
        {"i_interf_overridden_prim_method",      class_for_check,       not_found_sign},
        {"i_interf_overridden_ref_method",       class_for_check,       not_found_sign},
        {"i_void_par_method",                    class_for_check,       not_found_sign},
        {"i_boolean_par_method",                 class_for_check,       not_found_sign},
        {"i_byte_par_method",                    class_for_check,       not_found_sign},
        {"i_char_par_method",                    class_for_check,       not_found_sign},
        {"i_double_par_method",                  class_for_check,       not_found_sign},
        {"i_float_par_method",                   class_for_check,       not_found_sign},
        {"i_int_par_method",                     class_for_check,       not_found_sign},
        {"i_long_par_method",                    class_for_check,       not_found_sign},
        {"i_string_par_method",                  class_for_check,       not_found_sign},
        {"i_object_par_method",                  class_for_check,       not_found_sign},
        {"i_prim_array_par_method",              class_for_check,       not_found_sign},
        {"i_ref_array_par_method",               class_for_check,       not_found_sign},
        {"i_super_overridden_void_par_method",   class_for_check,       not_found_sign},
        {"i_super_overridden_prim_par_method",   class_for_check,       not_found_sign},
        {"i_super_overridden_ref_par_method",    class_for_check,       not_found_sign},
        {"i_interf_overridden_void_par_method",  class_for_check,       not_found_sign},
        {"i_interf_overridden_prim_par_method",  class_for_check,       not_found_sign},
        {"i_interf_overridden_ref_par_method",   class_for_check,       not_found_sign},
        {"i_abstract_method",                    class_for_check,       not_found_sign},
        {"i_native_method",                      class_for_check,       not_found_sign},
        {"i_synchr_method",                      class_for_check,       not_found_sign},
        {"i_final_method",                       class_for_check,       not_found_sign},
        {"i_private_method",                     class_for_check,       not_found_sign},
        {"i_protected_method",                   class_for_check,       not_found_sign},
        {"i_public_method",                      class_for_check,       not_found_sign},
        {"<clinit>",                             class_for_check,       not_found_sign}

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

        int v_test_result = new methods001().runThis(argv,out);
        if ( v_test_result == 2/*STATUS_FAILED*/ ) {
            print_log_anyway("\n==> nsk/jdi/ReferenceType/methods/methods001 test FAILED");
        }
        else {
            print_log_anyway("\n==> nsk/jdi/ReferenceType/methods/methods001 test PASSED");
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

        print_log_anyway("==> nsk/jdi/ReferenceType/methods/methods001 test LOG:");
        print_log_anyway("==> test checks methods() method of ReferenceType interface ");
        print_log_anyway("    of the com.sun.jdi package\n");

        String debugee_launch_command = debugeeName;

        Binder binder = new Binder(argsHandler,test_log_handler);
        Debugee debugee = binder.bindToDebugee(debugee_launch_command);
        IOPipe pipe = new IOPipe(debugee);

        debugee.redirectStderr(out);
        print_log_on_verbose("--> methods001: methods001a debugee launched");
        debugee.resume();

        String line = pipe.readln();
        if (line == null) {
            print_log_anyway
                ("##> methods001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        if (!line.equals("ready")) {
            print_log_anyway
                ("##> methods001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose("--> methods001: debugee's \"ready\" signal recieved!");
        }

        print_log_anyway
            ("--> methods001: check ReferenceType.methods() method for debugee's "
            + class_for_check + " class...");
        boolean class_not_found_error = false;
        boolean methods_exception_thrown = false;
        int methods_for_check_number = methods_for_check.length;
        int not_found_methods_number = 0;
        int found_methods_number = 0;
        int unexpected_found_methods_number = 0;

        while ( true ) {
            ReferenceType refType = debugee.classByName(class_for_check);
            if (refType == null) {
                print_log_anyway("##> methods001: Could NOT FIND class: " + class_for_check);
                class_not_found_error = true;
                break;
            }
            List<Method> found_methods_list = null;
            try {
                found_methods_list = refType.methods();
            }
            catch (Throwable thrown) {
                print_log_anyway("##> methods001: FAILED: ReferenceType.methods() throws unexpected "
                    + thrown);
                methods_exception_thrown = true;
                break;
            }
            found_methods_number = found_methods_list.size();
            Method found_methods[] = new Method[found_methods_number];
            String unexpected_found_methods[] = new String[found_methods_number];
            found_methods_list.toArray(found_methods);
            for (int i=0; i<found_methods_number; i++) {
                Method checked_found_method = found_methods[i];
                String checked_found_method_name = checked_found_method.name();
                String found_declaring_class = "declaring class NOT defined";
                try {
                    found_declaring_class = checked_found_method.declaringType().name();
                }
                catch (Throwable thrown) {
                }
                String checked_found_method_info = checked_found_method_name +"  (" + found_declaring_class + ")";
                int j=0;
                for (; j<methods_for_check_number; j++) {
                    if ( checked_found_method_name.equals(methods_for_check[j][0]) ) {
                        if ( found_declaring_class.equals(methods_for_check[j][1]) ) {
                            methods_for_check[j][2] = passed_sign;
                            break;
                        }
                    }
                }
                if ( j == methods_for_check_number ) {
                    //  unexpected method found
                    unexpected_found_methods[unexpected_found_methods_number] = checked_found_method_info;
                    unexpected_found_methods_number++;
                }
            }
            for (int i=0; i<methods_for_check_number; i++) {
                String current_method_for_check = methods_for_check[i][0]
                    + "  (" + methods_for_check[i][1] + ")";
                if ( methods_for_check[i][2].equals(not_found_sign) ) {
                    print_log_anyway
                        ("##> methods001: FAILED: method is NOT found: " + current_method_for_check);
                    not_found_methods_number++;
                }
                else {
                    print_log_on_verbose
                        ("--> methods001: PASSED for method: " + current_method_for_check);
                }
            }
            for (int i=0; i<unexpected_found_methods_number; i++) {
                print_log_anyway
                    ("##> methods001: FAILED: unexpected found method: " + unexpected_found_methods[i]);
            }
            break;
        }

        print_log_anyway("--> methods001: check completed!");
        int v_test_result = 0/*STATUS_PASSED*/;
        if ( class_not_found_error || methods_exception_thrown ) {
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_anyway("--> methods001: expected found methods number = " + methods_for_check_number);
            print_log_anyway("--> methods001: in fact found methods number  = "
                + found_methods_number);
            print_log_anyway("--> methods001: expected and in fact found methods number  = "
                + (methods_for_check_number - not_found_methods_number));
            print_log_anyway
                ("##> methods001: NOT found methods number = " + not_found_methods_number);
            print_log_anyway
                ("##> methods001: UNEXPECTED found methods number = " + unexpected_found_methods_number);
        }
        if ( not_found_methods_number + unexpected_found_methods_number > 0 ) {
            v_test_result = 2/*STATUS_FAILED*/;
        }

        print_log_on_verbose("--> methods001: waiting for debugee finish...");
        pipe.println("quit");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != 0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/) {
            print_log_anyway
                ("##> methods001: UNEXPECTED Debugee's exit status (not 95) - " + status);
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose
                ("--> methods001: expected Debugee's exit status - " + status);
        }

        return v_test_result;
    }
}
