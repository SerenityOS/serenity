/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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


/**
 * This class is used as debugee application for the reflectype001 JDI test.
 */

public class reflectype001a {

    static boolean verbose_mode = false;  // debugger may switch to true
                                          // - for more easy failure evaluation

    boolean z0, z1[]={z0}, z2[][]={z1};
    byte    b0, b1[]={b0}, b2[][]={b1};
    char    c0, c1[]={c0}, c2[][]={c1};
    double  d0, d1[]={d0}, d2[][]={d1};
    float   f0, f1[]={f0}, f2[][]={f1};
    int     i0, i1[]={i0}, i2[][]={i1};
    long    l0, l1[]={l0}, l2[][]={l1};

    // Classes must be loaded and linked, so all fields must be
    // initialized
    Boolean   Z0 = Boolean.valueOf(true),       Z1[]={Z0}, Z2[][]={Z1};
    Byte      B0 = Byte.valueOf((byte)1),       B1[]={B0}, B2[][]={B1};
    Character C0 = Character.valueOf('\u00ff'), C1[]={C0}, C2[][]={C1};
    Double    D0 = Double.valueOf(1.0),         D1[]={D0}, D2[][]={D1};
    Float     F0 = Float.valueOf(1.0f),         F1[]={F0}, F2[][]={F1};
    Integer   I0 = Integer.valueOf(-1),         I1[]={I0}, I2[][]={I1};
    Long      L0 = Long.valueOf(-1l),           L1[]={L0}, L2[][]={L1};
    String    S0 = new String("4434819"),       S1[]={S0}, S2[][]={S1};
    Object    O0 = new Object(),                O1[]={O0}, O2[][]={O1};

    // Interfaces must be loaded and linked, so classes that implement
    // interfaces must be initialized.
    static class  s_class {}
    static interface  s_interf {}
    protected static class s_interf_impl implements s_interf {}
    s_interf_impl sii0 = new s_interf_impl();

    s_class s_class0 = new s_class(), s_class1[]={s_class0},
            s_class2[][]={s_class1};
    s_interf s_interf0, s_interf1[]={s_interf0}, s_interf2[][]={s_interf1};

    package_class package_class0 = new package_class(),
                  package_class1[]={package_class0},
                  package_class2[][]={package_class1};
    class package_interf_impl implements package_interf {}
    package_interf_impl pii0 = new package_interf_impl();
    package_interf package_interf0, package_interf1[]={package_interf0},
                    package_interf2[][]={package_interf1};
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

        print_log_on_verbose("**> reflectype001a: debugee started!");
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        reflectype001a reflectype001a_obj = new reflectype001a();

        print_log_on_verbose("**> reflectype001a: waiting for \"quit\" signal...");
        pipe.println("ready");
        String instruction = pipe.readln();
        if (instruction.equals("quit")) {
            print_log_on_verbose("**> reflectype001a: \"quit\" signal recieved!");
            print_log_on_verbose("**> reflectype001a: completed succesfully!");
            System.exit(0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/);
        }
        System.err.println("!!**> reflectype001a: unexpected signal (no \"quit\") - " + instruction);
        System.err.println("!!**> reflectype001a: FAILED!");
        System.exit(2/*STATUS_FAILED*/ + 95/*STATUS_TEMP*/);
    }
}

/** simple class */
class package_class {}

/** simple interface */
interface package_interf {}
