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

package nsk.jdi.VirtualMachine.classesByName;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This class is used as debugee application for the classesbyname001 JDI test.
 */

public class classesbyname001a {

    //----------------------------------------------------- template section

    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;


     //--------------------------------------------------   log procedures

    static boolean verbose_mode = false;  // debugger may switch to true

    private static void log1(String message) {
        if (verbose_mode)
            System.err.println("**> classesbyname001a: " + message);
    }

    private static void logErr(String message) {
        if (verbose_mode)
            System.err.println("!!**> classesbyname001a: " + message);
    }

    //====================================================== test program

    //------------------------------------------------------ common section

    //----------------------------------------------------   main method

    public static void main (String argv[]) {

        for (int i=0; i<argv.length; i++) {
            if ( argv[i].equals("-vbs") || argv[i].equals("-verbose") ) {
                verbose_mode = true;
                break;
            }
        }
        log1("debugee started!");

        // informing debuger of readyness
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        pipe.println("ready");


        int exitCode = PASSED;
        for (int i = 0; ; i++) {
            String instruction;

            log1("waiting for an instruction from the debuger ...");
            instruction = pipe.readln();
            if (instruction.equals("quit")) {
                log1("'quit' recieved");
                break ;
            }

            if (instruction.equals("newcheck")) {
                switch (i) {

    //------------------------------------------------------  section tested

                // no attempt is made to load a class of given name

                case 0:         // no O/I loaded
                                pipe.println("checkready");
                                break ;
                case 1:         // no O/I loaded
                                pipe.println("checkready");
                                break ;

                // Returns the loaded reference types that match a given name

                case 2:         // 1O/noI loaded
                        Class1ForCheck obj11 = new Class1ForCheck();
                                pipe.println("checkready");
                                break ;
                case 3:
                                pipe.println("checkready");
                                break ;
                case 4:
                                pipe.println("checkready");
                                break ;
                case 5:         // 2O/1I loaded
                        Class2ForCheck obj21 = new Class2ForCheck();
                                pipe.println("checkready");
                                break ;
                case 6:
                                pipe.println("checkready");
                                break ;


                // not a fully qualified name

                case 7:
                                pipe.println("checkready");
                                break ;



    //-------------------------------------------------    standard end section

                default:
                                pipe.println("checkend");
                                break ;
                }

            } else {
                logErr("unexpected instruction: " + instruction);
                logErr("FAILED!");
                exitCode = 2;
                break ;
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
    Object   i_object;
    long[]   i_prim_array;
    Object[] i_ref_array;
}
