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

package nsk.jdi.Accessible.modifiers;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The modifiers001 test checks the method <code>modifiers()</code>
 * of the JDI interface <code>Accessible</code> of com.sun.jdi package
 * for ClassType, InterfaceType
 */

public class modifiers001 extends Log {
    static java.io.PrintStream out_stream;
    static boolean verbose_mode = false;

    /** The main class names of the debugger & debugee applications. */
    private final static String
        package_prefix = "nsk.jdi.Accessible.modifiers.",
//        package_prefix = "",    //  for DEBUG without package
        thisClassName = package_prefix + "modifiers001",
        debugeeName   = thisClassName + "a";


    static ArgumentHandler      argsHandler;
    private static Log  logHandler;


    /** Debugee's classes for check **/
    private final static String classes_for_check[][] = {

        {"java.lang.Boolean"  , "public, final", "class"},
        {"java.lang.Byte"     , "public, final", "class"},
        {"java.lang.Character", "public, final", "class"},
        {"java.lang.Double"   , "public, final", "class"},
        {"java.lang.Float"    , "public, final", "class"},
        {"java.lang.Integer"  , "public, final", "class"},
        {"java.lang.Long"     , "public, final", "class"},
        {"java.lang.String"   , "public, final", "class"},
        {"java.lang.Object"   , "public ", "class"},

        {thisClassName, "public", "class"},
        {thisClassName+"a", "public", "class"},

        {debugeeName+"$fin_s_cls", "final, static", "class"},
        {debugeeName+"$abs_s_cls", "abstract, static", "class"},
        {debugeeName+"$s_interf", "abstract, static, interface", "interface"},

        {package_prefix + "simple_class", "<no modifiers>", "class"},
        {package_prefix + "abstract_class", "abstract", "class"},
        {package_prefix + "final_class", "final", "class"},
        {package_prefix + "interf", "abstract, interface", "interface"}
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

        int v_test_result = new modifiers001().runThis(argv,out_stream);
        if ( v_test_result == 2/*STATUS_FAILED*/ ) {
            logHandler.complain("\n==> nsk/jdi/Accessible/modifiers/modifiers001 test FAILED");
        }
        else {
            logHandler.display("\n==> nsk/jdi/Accessible/modifiers/modifiers001 test PASSED");
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


        logHandler.display("==> nsk/jdi/Accessible/modifiers/modifiers001 test LOG:");
        logHandler.display("==> test checks the modifiers() method of Accessible interface");
        logHandler.display("    of the com.sun.jdi package for ClassType, InterfaceType\n");


        debugee.redirectStderr(out);
        print_log_on_verbose("--> modifiers001: modifiers001a debugee launched");
        debugee.resume();

        String line = pipe.readln();
        if (line == null) {
            logHandler.complain
                ("##> modifiers001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        if (!line.equals("ready")) {
            logHandler.complain
                ("##> modifiers001: UNEXPECTED debugee's signal (not \"ready\") - " + line);
            return 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose("--> modifiers001: debugee's \"ready\" signal recieved!");
        }

        logHandler.display
            ("--> modifiers001: checking debugee's classes by Accessible.modifiers() method...\n");
        int all_classes_count = 0;
        int class_not_found_errors = 0;
        int class_not_found_exceptions = 0;
        int modifiers_method_errors = 0;
//        modifiers001a obj = new modifiers001a();
        for (int i=0; i<classes_for_check.length; i++) {
            String className = classes_for_check[i][0];
            all_classes_count++;
            ReferenceType refType = debugee.classByName(className);
            if (refType == null) {
                logHandler.complain("##> isPublic001: Could NOT FIND class: " + className);
                class_not_found_errors++;
                continue;
            }
            Class class_obj;
            try {
                class_obj = Class.forName(className);
            }
            catch (ClassNotFoundException e) {
                logHandler.complain
                    ("##> modifiers001: Class.forName("+className+") - "+e.toString());
                class_not_found_exceptions++;
                continue;
            }
            int expected_modifiers = class_obj.getModifiers();
            String s_type = classes_for_check[i][2];
            String s_modifiers = classes_for_check[i][1];
            int got_modifiers = refType.modifiers();
            // Class.getModifiers() will never return ACC_SUPER
            // but Accessible.modifers() can, so ignore this bit
            got_modifiers &= ~0x20; // 0x20 == ACC_SUPER
            logHandler.display("");
            if ( got_modifiers != expected_modifiers ) {
                logHandler.complain("##> modifiers001: UNEXPECTED modifiers() method result ("
                                    + "0x" + Integer.toHexString(got_modifiers) + ") for " + s_type + ": " + className
                                    + "(" +  s_modifiers + ")");
                logHandler.complain("##>               expected modifiers() method result = "
                                    + "0x" + Integer.toHexString(expected_modifiers));
                modifiers_method_errors++;
            }
            else {
                print_log_on_verbose("--> modifiers001:  expected modifiers() method result ("
                                    + "0x" + Integer.toHexString(got_modifiers) + ") for " + s_type + ": " + className
                                    + "(" +  s_modifiers + ")");
            }
        }
        logHandler.display
            ("--> modifiers001: checking debugee's classes completed!");
        logHandler.display
            ("--> modifiers001: number of checked classes = " + all_classes_count);
        if ( class_not_found_errors > 0 ) {
            logHandler.complain("##> modifiers001: \"class not found ERRORS\" counter = "
                                + class_not_found_errors);
        }
        if ( class_not_found_exceptions > 0 ) {
            logHandler.complain("##> modifiers001: \"class not found EXCEPTIONS\" counter = "
                                + class_not_found_exceptions);
        }
        logHandler.display("##> modifiers001: modifiers() method ERRORS counter = "
                            + modifiers_method_errors);
        int v_test_result = 0/*STATUS_PASSED*/;
        if (class_not_found_errors + class_not_found_exceptions + modifiers_method_errors > 0) {
            v_test_result = 2/*STATUS_FAILED*/;
        }

        print_log_on_verbose("--> modifiers001: waiting for debugee finish...");
        pipe.println("quit");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != 0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/) {
            logHandler.complain
                ("##> modifiers001: UNEXPECTED Debugee's exit status (not 95) - " + status);
            v_test_result = 2/*STATUS_FAILED*/;
        }
        else {
            print_log_on_verbose
                ("--> modifiers001: expected Debugee's exit status - " + status);
        }

        return v_test_result;
    }
}
