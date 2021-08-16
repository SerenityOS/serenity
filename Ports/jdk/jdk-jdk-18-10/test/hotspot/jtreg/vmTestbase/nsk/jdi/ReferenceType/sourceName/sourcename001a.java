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


/**
 * This class is used as debugee application for the sourcename001 JDI test.
 */

public class sourcename001a {

    static boolean verbose_mode = false;  // debugger may switch to true
                                          // - for more easy failure evaluation

    // Classes must be loaded and linked, so all fields must be
    // initialized
    static class  s_class {}
    s_class s_class_0 = new s_class(), s_class_1[]={s_class_0};

    // Interfaces must be loaded and linked, so classes that implement
    // interfaces must be initialized.
    static interface  s_interf {}
    static class s_interf_impl implements s_interf {}
    s_interf_impl sii0 = new s_interf_impl ();
    s_interf s_interf_0, s_interf_1[]={s_interf_0};

    ClassForCheck class_for_check0 = new ClassForCheck(),
                  class_for_check1[]={class_for_check0};
    InterfaceForCheck_impl interf_for_check_impl0 = new    InterfaceForCheck_impl();
    InterfaceForCheck interf_for_check0,
                      interf_for_check1[]={interf_for_check0};

    sourcename001 sourcename001_0 = new sourcename001(),
                  sourcename001_1[]={sourcename001_0};

    private static void print_log_on_verbose(String message) {
        if ( verbose_mode ) {
            System.err.println(message);
        }
    }

    public static void main (String argv[]) {

        for (int i=0; i<argv.length; i++) {
            if ( argv[i].equals("-vbs") || argv[i].equals("-verbose") ) {
                verbose_mode = true;
                break;
            }
        }

        print_log_on_verbose("**> sourcename001a: debugee started!");
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        sourcename001a sourcename001a_obj = new sourcename001a();

        print_log_on_verbose("**> sourcename001a: waiting for \"quit\" signal...");
        pipe.println("ready");
        String instruction = pipe.readln();
        if (instruction.equals("quit")) {
            print_log_on_verbose("**> sourcename001a: \"quit\" signal recieved!");
            print_log_on_verbose("**> sourcename001a: completed succesfully!");
            System.exit(0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/);
        }
        System.err.println("!!**> sourcename001a: unexpected signal (no \"quit\") - " + instruction);
        System.err.println("!!**> sourcename001a: FAILED!");
        System.exit(2/*STATUS_FAILED*/ + 95/*STATUS_TEMP*/);
    }
}

class ClassForCheck {}

interface InterfaceForCheck {}

class InterfaceForCheck_impl implements InterfaceForCheck{}
