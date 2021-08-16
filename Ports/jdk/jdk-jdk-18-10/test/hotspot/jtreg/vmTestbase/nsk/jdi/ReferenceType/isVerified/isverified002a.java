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

import java.lang.reflect.*;
import java.io.*;
import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This class is used as debugee application for the isverified002 JDI test.
 */

public class isverified002a {

    static boolean verbose_mode = false;  // debugger may switch to true
                                          // - for more easy failure evaluation

    static String package_prefix = "nsk.jdi.ReferenceType.isVerified.";
    static String checked_class_name = package_prefix + "isverified002b";

    private static void print_log_on_verbose(String message) {
        if ( verbose_mode ) {
            System.err.println(message);
        }
    }

    public static void main (String argv[]) {

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        verbose_mode = argHandler.verbose();

        print_log_on_verbose("**> isverified002a: debugee started!");
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        print_log_on_verbose("**> isverified002a: waiting for \"checked class dir\" info...");
        pipe.println("ready0");
        String checked_class_dir = (argHandler.getArguments())[0] + File.separator + "loadclass";


        ClassUnloader classUnloader = new ClassUnloader();

        try {
            classUnloader.loadClass(checked_class_name, checked_class_dir);
            print_log_on_verbose
                ("--> isverified002a: checked class loaded: " + checked_class_name);
        }
        catch ( Exception e ) {  // ClassNotFoundException
            System.err.println
                ("**> isverified002a: load class: exception thrown = " + e.toString());
            print_log_on_verbose
                ("--> isverified002a: checked class NOT loaded: " + checked_class_name);
            // Debuuger finds this fact itself
        }

        print_log_on_verbose("**> isverified002a: waiting for \"continue\" or \"quit\" signal...");
        pipe.println("ready1");
        String instruction = pipe.readln();
        if (instruction.equals("quit")) {
            print_log_on_verbose("**> isverified002a: \"quit\" signal recieved!");
            print_log_on_verbose("**> isverified002a: completed!");
            System.exit(0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/);
        }
        if ( ! instruction.equals("continue")) {
            System.err.println
                ("!!**> isverified002a: unexpected signal (no \"continue\" or \"quit\") - " + instruction);
            System.err.println("!!**> isverified002a: FAILED!");
            System.exit(2/*STATUS_FAILED*/ + 95/*STATUS_TEMP*/);
        }

        print_log_on_verbose("**> isverified002a: \"continue\" signal recieved!");
        print_log_on_verbose("**> isverified002a: enforce to unload checked class...");

        boolean test_class_loader_finalized = classUnloader.unloadClass();

        if ( ! test_class_loader_finalized ) {
            print_log_on_verbose("**> isverified002a: checked class may be NOT unloaded!");
            pipe.println("not_unloaded");
        }
        else {
            print_log_on_verbose("**> isverified002a: checked class unloaded!");
            pipe.println("ready2");
        }
        print_log_on_verbose("**> isverified002a: waiting for \"quit\" signal...");
        instruction = pipe.readln();
        if (instruction.equals("quit")) {
            print_log_on_verbose("**> isverified002a: \"quit\" signal recieved!");
            print_log_on_verbose("**> isverified002a: completed!");
            System.exit(0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/);
        }
        System.err.println("!!**> isverified002a: unexpected signal (no \"quit\") - " + instruction);
        System.err.println("!!**> isverified002a: FAILED!");
        System.exit(2/*STATUS_FAILED*/ + 95/*STATUS_TEMP*/);
    }
}  // end of isverified002a class
