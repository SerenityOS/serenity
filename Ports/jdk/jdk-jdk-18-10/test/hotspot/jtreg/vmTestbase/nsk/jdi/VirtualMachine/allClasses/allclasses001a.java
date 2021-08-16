/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.VirtualMachine.allClasses;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * This class is used as debugee application for the allclasses001a JDI test.

 */

public class allclasses001a {

    //----------------------------------------------------- templete section

    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;


     //--------------------------------------------------   log procedures

    static boolean verbMode = false;

    private static void log1(String message) {
        if (verbMode)
            System.out.println("**> debuggee: " + message);
    }

    private static void logErr(String message) {
        if (verbMode)
            System.out.println("!!**> debuggee: " + message);
    }

    //====================================================== test program

    static int instruction = 1;
    static int end         = 0;
                                   //    static int quit        = 0;
                                   //    static int continue    = 2;
    static int maxInstr    = 1;    // 2;

    static int lineForComm = 2;

    private static void methodForCommunication() {
        int i1 = instruction;
        int i2 = i1;
        int i3 = i2;
    }

    //----------------------------------------------------   main method

    public static void main (String argv[]) {

        for (int i=0; i<argv.length; i++) {
            if ( argv[i].equals("-vbs") || argv[i].equals("-verbose") ) {
                verbMode = true;
                break;
            }
        }
        log1("debuggee started!");

        int exitCode = PASSED;

        label0:
        for (int i = 0; ; i++) {

    //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^  globals for switch-block

        Class1ForCheck obj11;
        Class2ForCheck obj21;
        Class2ForCheck obj22;
        Class3ForCheck obj31;

    //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

            if (instruction > maxInstr || instruction < 0) {
                logErr("ERROR: unexpected instruction: " + instruction);
                exitCode = FAILED;
                break ;
            }

            switch (i) {

    //------------------------------------------------------  section tested

                case 0:         // Objects# =0;; Classes# =0; Interfaces# =0

                                methodForCommunication();
                                break ;

                case 1:         // Objects# =1; Classes# =1; Interfaces# =0;

                                obj11 = new Class1ForCheck();
                                methodForCommunication();
                                break ;

                case 2:         // Objects# =2; Classes# =2; Interfaces# =1;

                                obj21 = new Class2ForCheck();
                                methodForCommunication();
                                break ;

                case 3:         // Objects# =3; Classes# =2; Interfaces# =1;

                                obj22 = new Class2ForCheck();
                                methodForCommunication();
                                break ;

                case 4:         // Objects# =4; Classes# =3; Interfaces# =1;

                                obj31 = new Class3ForCheck();
                                methodForCommunication();
                                break ;


    //-------------------------------------------------    standard end section


                default:
                                instruction = end;
                                methodForCommunication();
                                break label0;
            }
        }

        System.exit(exitCode + PASS_BASE);
    }
}


class Class1ForCheck {

    // static fields

    static boolean   s_boolean;
    static byte      s_byte;
    static char      s_char;
    static double    s_double;
    static float     s_float;
    static int       s_int;
    static long      s_long;
    static short     s_short;
    static Object    s_object;
    static long[]    s_prim_array;
    static Object[]  s_ref_array;

    // instance fields

    boolean  i_boolean;
    byte     i_byte;
    char     i_char;
    double   i_double;
    float    i_float;
    int      i_int;
    long     i_long;
    short    i_short;
    Object   i_object;
    long[]   i_prim_array;
    Object[] i_ref_array;
}


interface InterfaceForCheck {

    static final boolean s_iface_boolean = true;
    static final byte    s_iface_byte    = (byte)1;
    static final char    s_iface_char    = '1';
    static final double  s_iface_double  = 999;
    static final float   s_iface_float   = 99;
    static final int     s_iface_int     = 100;
    static final long    s_iface_long    = 1000;
    static final short   s_iface_short   = 1000;
    static final Object  s_iface_object  = new Object();
}



class Class2ForCheck implements InterfaceForCheck {

    // static fields

    static boolean   s_boolean;
    static byte      s_byte;
    static char      s_char;
    static double    s_double;
    static float     s_float;
    static int       s_int;
    static long      s_long;
    static short     s_short;
    static Object    s_object;
    static long[]    s_prim_array;
    static Object[]  s_ref_array;

    // instance fields

    boolean  i_boolean;
    byte     i_byte;
    char     i_char;
    double   i_double;
    float    i_float;
    int      i_int;
    long     i_long;
    short    i_short;
    Object   i_object;
    long[]   i_prim_array;
    Object[] i_ref_array;
}

class Class3ForCheck implements InterfaceForCheck {

    // static fields

    static boolean   s_boolean;
    static byte      s_byte;
    static char      s_char;
    static double    s_double;
    static float     s_float;
    static int       s_int;
    static long      s_long;
    static short     s_short;
    static Object    s_object;
    static long[]    s_prim_array;
    static Object[]  s_ref_array;

    // instance fields

    boolean  i_boolean;
    byte     i_byte;
    char     i_char;
    double   i_double;
    float    i_float;
    int      i_int;
    long     i_long;
    short    i_short;
    Object   i_object;
    long[]   i_prim_array;
    Object[] i_ref_array;
}
