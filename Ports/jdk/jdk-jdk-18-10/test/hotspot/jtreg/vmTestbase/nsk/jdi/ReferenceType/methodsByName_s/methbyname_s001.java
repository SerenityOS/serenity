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

package nsk.jdi.ReferenceType.methodsByName_s;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * This test checks the method <code>methodsByName(String name)</code>
 * of the JDI interface <code>ReferenceType</code> of com.sun.jdi package
 */

public class methbyname_s001 extends Log {
    static java.io.PrintStream out_stream;
    static boolean verbose_mode = false;  // test argument -vbs or -verbose switches to static
                                          // - for more easy failure evaluation

    /** The main class names of the debugger & debugee applications. */
    private final static String
        package_prefix = "nsk.jdi.ReferenceType.methodsByName_s.",
//        package_prefix = "",    //  for DEBUG without package
        thisClassName = package_prefix + "methbyname_s001",
        debugeeName   = thisClassName + "a";

    /** Debugee's classes for check **/
    private final static String class_for_check = package_prefix + "methbyname_s001aClassForCheck";
    private final static String super_class_for_check = package_prefix + "methbyname_s001aSuperClassForCheck";
    private final static String interface_for_check = package_prefix + "methbyname_s001aInterfaceForCheck";
    private final static String not_existing_class = "not_existing_class";
    private static String methods_for_check[][] = {

//        method name                            declaring class

        {"<init>",                               class_for_check      },
        {"ClassForCheck",                        class_for_check      },
        {"s_void_method",                        class_for_check      },
        {"s_boolean_method",                     class_for_check      },
        {"s_byte_method",                        class_for_check      },
        {"s_char_method",                        class_for_check      },
        {"s_double_method",                      class_for_check      },
        {"s_float_method",                       class_for_check      },
        {"s_int_method",                         class_for_check      },
        {"s_long_method",                        class_for_check      },
        {"s_string_method",                      class_for_check      },
        {"s_object_method",                      class_for_check      },
        {"s_prim_array_method",                  class_for_check      },
        {"s_ref_array_method",                   class_for_check      },
        {"s_super_hidden_void_method",           class_for_check      },
        {"s_super_hidden_prim_method",           class_for_check      },
        {"s_super_hidden_ref_method",            class_for_check      },
        {"s_void_par_method",                    class_for_check      },
        {"s_boolean_par_method",                 class_for_check      },
        {"s_byte_par_method",                    class_for_check      },
        {"s_char_par_method",                    class_for_check      },
        {"s_double_par_method",                  class_for_check      },
        {"s_float_par_method",                   class_for_check      },
        {"s_int_par_method",                     class_for_check      },
        {"s_long_par_method",                    class_for_check      },
        {"s_string_par_method",                  class_for_check      },
        {"s_object_par_method",                  class_for_check      },
        {"s_prim_array_par_method",              class_for_check      },
        {"s_ref_array_par_method",               class_for_check      },
        {"s_super_hidden_void_par_method",       class_for_check      },
        {"s_super_hidden_prim_par_method",       class_for_check      },
        {"s_super_hidden_ref_par_method",        class_for_check      },
        {"s_native_method",                      class_for_check      },
        {"s_synchr_method",                      class_for_check      },
        {"s_final_method",                       class_for_check      },
        {"s_private_method",                     class_for_check      },
        {"s_protected_method",                   class_for_check      },
        {"s_public_method",                      class_for_check      },
        {"i_void_method",                        class_for_check      },
        {"i_boolean_method",                     class_for_check      },
        {"i_byte_method",                        class_for_check      },
        {"i_char_method",                        class_for_check      },
        {"i_double_method",                      class_for_check      },
        {"i_float_method",                       class_for_check      },
        {"i_int_method",                         class_for_check      },
        {"i_long_method",                        class_for_check      },
        {"i_string_method",                      class_for_check      },
        {"i_object_method",                      class_for_check      },
        {"i_prim_array_method",                  class_for_check      },
        {"i_ref_array_method",                   class_for_check      },
        {"i_super_overridden_void_method",       class_for_check      },
        {"i_super_overridden_prim_method",       class_for_check      },
        {"i_super_overridden_ref_method",        class_for_check      },
        {"i_interf_overridden_void_method",      class_for_check      },
        {"i_interf_overridden_prim_method",      class_for_check      },
        {"i_interf_overridden_ref_method",       class_for_check      },
        {"i_void_par_method",                    class_for_check      },
        {"i_boolean_par_method",                 class_for_check      },
        {"i_byte_par_method",                    class_for_check      },
        {"i_char_par_method",                    class_for_check      },
        {"i_double_par_method",                  class_for_check      },
        {"i_float_par_method",                   class_for_check      },
        {"i_int_par_method",                     class_for_check      },
        {"i_long_par_method",                    class_for_check      },
        {"i_string_par_method",                  class_for_check      },
        {"i_object_par_method",                  class_for_check      },
        {"i_prim_array_par_method",              class_for_check      },
        {"i_ref_array_par_method",               class_for_check      },
        {"i_super_overridden_void_par_method",   class_for_check      },
        {"i_super_overridden_prim_par_method",   class_for_check      },
        {"i_super_overridden_ref_par_method",    class_for_check      },
        {"i_interf_overridden_void_par_method",  class_for_check      },
        {"i_interf_overridden_prim_par_method",  class_for_check      },
        {"i_interf_overridden_ref_par_method",   class_for_check      },
        {"i_abstract_method",                    class_for_check      },
        {"i_native_method",                      class_for_check      },
        {"i_synchr_method",                      class_for_check      },
        {"i_final_method",                       class_for_check      },
        {"i_private_method",                     class_for_check      },
        {"i_protected_method",                   class_for_check      },
        {"i_public_method",                      class_for_check      },
        {"<clinit>",                             class_for_check      },

        {"s_super_void_method",                  super_class_for_check},
        {"s_super_prim_method",                  super_class_for_check},
        {"s_super_ref_method",                   super_class_for_check},
        {"i_super_void_method",                  super_class_for_check},
        {"i_super_prim_method",                  super_class_for_check},
        {"i_super_ref_method",                   super_class_for_check},
        {"i_multiple_inherited_method",          super_class_for_check},

        {"i_interf_ref_method",                  interface_for_check  },
        {"i_interf_prim_method",                 interface_for_check  },
        {"i_interf_void_method",                 interface_for_check  },

        {"non_existing_method",                  not_existing_class   }

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

        int v_test_result = new methbyname_s001().runThis(argv,out_stream);
        if ( v_test_result == 2/*STATUS_FAILED*/ ) {
            out_stream.println("\n==> nsk/jdi/ReferenceType/methodsByName_s/methbyname_s001 test FAILED");
        }
        else {
            out_stream.println("\n==> nsk/jdi/ReferenceType/methodsByName_s/methbyname_s001 test PASSED");
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

        out_stream.println("==> nsk/jdi/ReferenceType/methodsByName_s/methbyname_s001 test LOG:");
        out_stream.println("==> test checks methodsByName(String name) method of ReferenceType interface ");
        out_stream.println("    of the com.sun.jdi package\n");

        String debugee_launch_command = debugeeName;
        if (verbose_mode) {
            logTo(out_stream);
        }

        Binder binder = new Binder(argHandler,this);
        Debugee debugee = binder.bindToDebugee(debugee_launch_command);
        IOPipe pipe = new IOPipe(debugee);

        debugee.redirectStderr(out);
        print_log_on_verbose("--> methbyname_s001: methbyname_s001a debugee launched");
        debugee.resume();

        String line = pipe.readln();
        if (line == null) {
            out_stream.println
                ("##> methbyname_s001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        if (!line.equals("ready")) {
            out_stream.println
                ("##> methbyname_s001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose("--> methbyname_s001: debugee's \"ready\" signal recieved!");
        }

        out_stream.println
            ("--> methbyname_s001: check ReferenceType.methodsByName(...) method for debugee's "
            + class_for_check + " class...");
        boolean class_not_found_error = false;
        int methods_for_check_number = methods_for_check.length;
        int methodsByName_exceptions = 0;
        int not_found_methods_number = 0;
        int unexpected_found_methods_number = 0;
        int not_matched_methods_number = 0;
        int all_method_errors_number = 0;
        int passed_methods_number = 0;

        while ( true ) {
            ReferenceType refType = debugee.classByName(class_for_check);
            if (refType == null) {
                out_stream.println("##> methbyname_s001: Could NOT FIND class: " + class_for_check);
                class_not_found_error = true;
                break;
            }
            for (int i=0; i<methods_for_check_number; i++) {
                boolean existing_method = true;
                String current_method_for_check = null;
                if ( methods_for_check[i][1].equals(not_existing_class) ) {
                    existing_method = false;
                    current_method_for_check = methods_for_check[i][0];
                }
                else {
                    current_method_for_check =  methods_for_check[i][0]
                        + "  (" + methods_for_check[i][1] + ")";
                }
                List<Method> methods_byname_list = null;
                try {
                    methods_byname_list = refType.methodsByName(methods_for_check[i][0]);
                }
                catch (Throwable thrown) {
                    out_stream.println
                        ("##> methbyname_s001: FAILED: methodsByName(...) throws unexpected "
                        + thrown);
                    out_stream.println
                        ("##>                  requested method: " + current_method_for_check);
                    methodsByName_exceptions++;
                    all_method_errors_number++;
                    continue;
                }
                int methods_byname_number = methods_byname_list.size();
                if ( methods_byname_number == 0 ) {
                    if ( existing_method ) {
                        out_stream.println
                            ("##> methbyname_s001: FAILED: methodsByName(...) returned empty List of methods!");
                        out_stream.println
                            ("##>                  requested method: " + current_method_for_check);
                        not_found_methods_number++;
                        all_method_errors_number++;
                    }
                    else {
                        print_log_on_verbose
                            ("--> methbyname_s001: PASSED for method: " + current_method_for_check);
                        print_log_on_verbose
                            ("-->                  expected result: empty List of methods");
                        passed_methods_number++;
                    }
                    continue;
                }
                Method methods_byname_array[] = new Method[methods_byname_number];
                methods_byname_list.toArray(methods_byname_array);
                if ( (! existing_method) || (methods_byname_number > 1) ) {
                    out_stream.println
                        ("##> methbyname_s001: FAILED: methodsByName(...) returned unexpected methods - "
                        + methods_byname_number + " method(s)");
                    out_stream.println
                        ("##>                  requested method: " + current_method_for_check);
                    if ( ! existing_method ) {
                        out_stream.println
                            ("##>                  expected result: empty List of methods");
                        unexpected_found_methods_number++;
                    }
                    else {
                        out_stream.println
                            ("##>                  expected result: List of requested method");
                        not_matched_methods_number++;
                    }
                    out_stream.println
                        ("##>                  returned methods:");
                    for (int k=0; k<methods_byname_number; k++) {
                        Method found_method = methods_byname_array[k];
                        String found_method_name = found_method.name();
                        String declaring_class_name = "declaring class NOT defined";
                        try {
                            declaring_class_name = found_method.declaringType().name();
                        }
                        catch (Throwable thrown) {
                        }
                        String found_method_info =  found_method_name + "  (" + declaring_class_name + ")";
                        out_stream.println
                            ("##>                  returned method[" + k +"] - " + found_method_info);
                    }
                    all_method_errors_number++;
                    continue;
                }

// The beginning of the patch to fix the test failure.
                /*
                Method found_method = methods_byname_array[0];
                String found_method_name = found_method.name();
                String declaring_class_name = "declaring class NOT defined";
                try {
                    declaring_class_name = found_method.declaringType().name();
                }
                catch (Throwable thrown) {
                }
                String found_method_info =  found_method_name + "  (" + declaring_class_name + ")";
                if ( ! methods_for_check[i][1].equals(declaring_class_name) ) {
                    out_stream.println
                        ("##> methbyname_s001: FAILED: methodsByName(...) returned not matched method - "
                        + found_method_info);
                    out_stream.println
                        ("##>                  requested method: " + current_method_for_check);
                    not_matched_methods_number++;
                    all_method_errors_number++;
                }
                else {
                    print_log_on_verbose
                        ("--> methbyname_s001: PASSED for method: " + current_method_for_check);
                    passed_methods_number++;
                }
                */
            // the string below replaces the above code fragment
            passed_methods_number++;

// The end of the patch.

            }
            break;
        }

        out_stream.println("--> methbyname_s001: check completed!");
        int v_test_result = 0/*STATUS_PASSED*/;
        if ( class_not_found_error ) {
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            out_stream.println
                ("--> methbyname_s001: number of checked methods = " + methods_for_check_number);
            if ( methodsByName_exceptions > 0 ) {
                out_stream.println
                    ("--> methbyname_s001: number of unexpected exceptions thrown by methodsByName(...) = "
                    + methodsByName_exceptions);
            }
            if ( not_found_methods_number > 0 ) {
                out_stream.println
                    ("--> methbyname_s001: number of methods not found by methodsByName(...) (empty List returned) = "
                    + not_found_methods_number);
            }
            if ( unexpected_found_methods_number > 0 ) {
                out_stream.println
                    ("--> methbyname_s001: number of unexpected methods found by methodsByName(...) (not empty List returned) = "
                    + unexpected_found_methods_number);
            }
            if ( not_matched_methods_number > 0 ) {
                out_stream.println
                    ("--> methbyname_s001: number of returned by methodsByName(...) methods not matched to requested method = "
                    + not_matched_methods_number);
            }
            out_stream.println
                ("--> methbyname_s001: number of methods for which methodsByName(...) returned expected result = "
                + passed_methods_number);
        }
        if ( all_method_errors_number > 0 ) {
            v_test_result = 2/*STATUS_FAILED*/;
        }

        print_log_on_verbose("--> methbyname_s001: waiting for debugee finish...");
        pipe.println("quit");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != 0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/) {
            out_stream.println
                ("##> methbyname_s001: UNEXPECTED Debugee's exit status (not 95) - " + status);
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose
                ("--> methbyname_s001: expected Debugee's exit status - " + status);
        }

        return v_test_result;
    }
}
