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

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This class is used as debugee application for the isVerified001 JDI test.
 */

public class isVerified001a {

    static boolean verbose_mode = false;

    isVerified001 a001_0 = new isVerified001();

    not_verif_cls not_verif_cls_0, not_verif_cls_1[] = {not_verif_cls_0};

    not_verif_interf not_verif_interf_0, not_verif_interf_1[] = {not_verif_interf_0};

    verif_class  verif_class_0 = new verif_class();

    verif_subcl  verif_subcl_0 = new verif_subcl();

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

        print_log_on_verbose("**> isVerified001a: debugee started!");
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        isVerified001a isVerified001a_obj = new isVerified001a();

        print_log_on_verbose("**> isVerified001a: waiting for \"quit\" signal...");
        pipe.println("ready");
        String instruction = pipe.readln();
        if (instruction.equals("quit")) {
            print_log_on_verbose("**> isVerified001a: \"quit\" signal recieved!");
            print_log_on_verbose("**> isVerified001a: completed succesfully!");
            System.exit(0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/);
        }
        System.err.println("!!**> isVerified001a: unexpected signal (no \"quit\") - " + instruction);
        System.err.println("!!**> isVerified001a: FAILED!");
        System.exit(2/*STATUS_FAILED*/ + 95/*STATUS_TEMP*/);
    }
}

/** not verified class */
class not_verif_cls {}

/** not verified interface */
interface not_verif_interf {}

/** verified class */
class verif_class {

    static {
        int int_var = 1;
    }

}

/** verified interface */
interface verif_interf {
    static final int int_var = 1;
}

/** verified subclass */
class verif_subcl implements verif_interf {
    static int my_int_var = int_var;

}
