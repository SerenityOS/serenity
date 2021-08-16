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


/**
 * This class is used as debugee application for the fields004 JDI test.
 */

public class fields004a {

    static boolean verbose_mode = false;  // debugger may switch to true
                                          // - for more easy failure evaluation


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

        print_log_on_verbose("**> fields004a: debugee started!");
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        fields004aClassForCheck class_for_check = new fields004aClassForCheck();

        print_log_on_verbose("**> fields004a: waiting for \"quit\" signal...");
        pipe.println("ready");
        String instruction = pipe.readln();
        if (instruction.equals("quit")) {
            print_log_on_verbose("**> fields004a: \"quit\" signal recieved!");
            print_log_on_verbose("**> fields004a: completed succesfully!");
            System.exit(0/*STATUS_PASSED*/ + 95/*STATUS_TEMP*/);
        }
        System.err.println("##> fields004a: unexpected signal (no \"quit\") - " + instruction);
        System.err.println("##> fields004a: FAILED!");
        System.exit(2/*STATUS_FAILED*/ + 95/*STATUS_TEMP*/);
    }
}

class fields004aClassForCheck extends fields004aSuperClassForCheck implements fields004aInterfaceForCheck {

}

class fields004aSuperClassForCheck  {

    static boolean s_super_boolean_field;
    static byte    s_super_byte_field;
    static char    s_super_char_field;
    static double  s_super_double_field;
    static float   s_super_float_field;
    static int     s_super_int_field;
    static long    s_super_long_field;
    static Object  s_super_object_field;

    static long    s_super_hidden_prim_field;
    static Object  s_super_hidden_ref_field;

    boolean i_super_boolean_field;
    byte    i_super_byte_field;
    char    i_super_char_field;
    double  i_super_double_field;
    float   i_super_float_field;
    int     i_super_int_field;
    long    i_super_long_field;
    Object  i_super_object_field;

    long    i_super_hidden_prim_field;
    Object  i_super_hidden_ref_field;

    long    ambiguous_prim_field;
    Object  ambiguous_ref_field;

}

interface fields004aInterfaceForCheck {

    static final boolean s_interf_boolean_field = true;
    static final byte    s_interf_byte_field = (byte)1;
    static final char    s_interf_char_field = '1';
    static final double  s_interf_double_field = 999;
    static final float   s_interf_float_field = 99;
    static final int     s_interf_int_field = 100;
    static final long    s_interf_long_field = 1000;
    static final Object  s_interf_object_field = new Object();

    static final long    s_interf_hidden_prim_field = 1;
    static final Object  s_interf_hidden_ref_field = new Object();

    static final long    ambiguous_prim_field = 1;
    static final Object  ambiguous_ref_field = new Object();

}
