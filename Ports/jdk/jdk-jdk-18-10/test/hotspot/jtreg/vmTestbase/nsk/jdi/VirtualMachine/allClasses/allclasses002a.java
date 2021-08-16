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
 * This class is used as debugee application for the allclasses002a JDI test.

 */

public class allclasses002a {

    //----------------------------------------------------- template section

    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;


     //--------------------------------------------------   log procedures

    static boolean verbMode = false;

    private static void log1(String message) {
        if (verbMode)
            System.err.println("**> debuggee: " + message);
    }

    private static void logErr(String message) {
        if (verbMode)
            System.err.println("!!**> debuggee: " + message);
    }
    //====================================================== test program

    static final allclasses002aClassForCheck[]     obj1     = { new allclasses002aClassForCheck(),
                                                  new allclasses002aClassForCheck() };

    static final allclasses002aInterfaceForCheck[] ifaceObj = { new allclasses002aClass2ForCheck(),
                                                  new allclasses002aClass2ForCheck() };

    //----------------------------------------------------   main method

    public static void main (String argv[]) {

        for (int i=0; i<argv.length; i++) {
            if ( argv[i].equals("-vbs") || argv[i].equals("-verbose") ) {
                verbMode = true;
                break;
            }
        }
        log1("debuggee started!");

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        pipe.println("ready");

        int exitCode = PASSED;
        for (int i = 0; ; i++) {

            String instruction;

            log1("waiting for an instruction from the debugger ...");
            instruction = pipe.readln();
            if (instruction.equals("quit")) {
                log1("'quit' recieved");
                break ;

            } else if (instruction.equals("newcheck")) {
                switch (i) {

    //------------------------------------------------------  section tested

                case 0:
                        pipe.println("checkready");
                        break ;

    //-------------------------------------------------    standard end section


                default:
                        pipe.println("checkend");
                        break ;
                }

            } else {
                logErr("ERRROR: unexpected instruction: " + instruction);
                exitCode = 2;
                break ;
            }
        }

        System.exit(exitCode + PASS_BASE);
    }
}


interface allclasses002aInterfaceForCheck {

    static final boolean s_iface_boolean = true;
    static final byte    s_iface_byte    = (byte)1;
    static final char    s_iface_char    = '1';
    static final double  s_iface_double  = 999;
    static final float   s_iface_float   = 99;
    static final int     s_iface_int     = 100;
    static final long    s_iface_long    = 1000;
    static final Object  s_iface_object  = new Object();
}

class allclasses002aClass2ForCheck implements allclasses002aInterfaceForCheck {

    static final boolean b1 = true;
    static final int     i1 = 0;

    public        boolean m0() {
        return s_iface_boolean;
    }

    public static int m1() {
        return s_iface_int;
    }
}

class allclasses002aClassForCheck {

    static final boolean b1 = true;
    static final int     i1 = 0;

static final boolean[][][][] bl1 = { {{{true, false}, {true, false}}, {{true, false}, {true, false}} },
                                     {{{true, false}, {true, false}}, {{true, false}, {true, false}} }  };

static final byte   [][][][] bt1 = { {{{0, 1},{0, 1}}, {{0, 1},{0, 1}}},
                                     {{{0, 1},{0, 1}}, {{0, 1},{0, 1}}}  };

static final char   [][][][] ch1 = { {{{0, 1},{0, 1}}, {{0, 1},{0, 1}}},
                                     {{{0, 1},{0, 1}}, {{0, 1},{0, 1}}}  };

static final double [][][][] db1 = { {{{0.0d, 1.0d},{0.0d, 1.0d}}, {{0.0d, 1.0d},{0.0d, 1.0d}}},
                                     {{{0.0d, 1.0d},{0.0d, 1.0d}}, {{0.0d, 1.0d},{0.0d, 1.0d}}}  };

static final float  [][][][] fl1 = { {{{0.0f, 1.0f},{0.0f, 1.0f}}, {{0.0f, 1.0f},{0.0f, 1.0f}}},
                                     {{{0.0f, 1.0f},{0.0f, 1.0f}}, {{0.0f, 1.0f},{0.0f, 1.0f}}}  };

static final int    [][][][] in1 = { {{{0, 1},{0, 1}}, {{0, 1},{0, 1}}},
                                     {{{0, 1},{0, 1}}, {{0, 1},{0, 1}}}  };

static final long   [][][][] ln1 = { {{{0, 1},{0, 1}}, {{0, 1},{0, 1}}},
                                     {{{0, 1},{0, 1}}, {{0, 1},{0, 1}}}  };

static final short  [][][][] sh1 = { {{{0, 1},{0, 1}}, {{0, 1},{0, 1}}},
                                     {{{0, 1},{0, 1}}, {{0, 1},{0, 1}}}  };
}
