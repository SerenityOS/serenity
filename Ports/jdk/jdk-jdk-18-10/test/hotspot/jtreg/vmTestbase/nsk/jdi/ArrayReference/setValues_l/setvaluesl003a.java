/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ArrayReference.setValues_l;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class setvaluesl003a {

    static setvaluesl003aClassToCheck testedObj = new setvaluesl003aClassToCheck();


    public static final String[] VALID_CONVERSIONS = {
                                "B",        // primitive types to byte
                                "C",        // primitive types to char
                                "BCDFIJS", // primitive types to double
                                "BCFIJS",  // primitive types to float
                                "BCIS",    // primitive types to int
                                "BCIJS",   // primitive types to long
                                "BS"       // primitive types to short
    };


    public final static String[] COMBINE = {
                                                "BYTE_VALUES",
                                                "CHAR_VALUES",
                                                "DBL_VALUES",
                                                "FLT_VALUES",
                                                "INT_VALUES",
                                                "LNG_VALUES",
                                                "SHORT_VALUES"
    };

    // Value parameter's array for debugee's arrays
    public static  byte[] BYTE_VALUES = {
                                            Byte.MIN_VALUE,
                                            -1,
                                            0,
                                            1,
                                            Byte.MAX_VALUE
    };

    public static char[] CHAR_VALUES = {
                                            Character.MIN_VALUE,
                                            'a',
                                            'z',
                                            'A',
                                            'Z',
                                           Character.MAX_VALUE
    };

    public static double[] DBL_VALUES = {
                                            Double.NEGATIVE_INFINITY,
                                            Double.MIN_VALUE,
                                            -1,
                                            0,
                                            1,
                                            Double.MAX_VALUE,
                                            Double.POSITIVE_INFINITY
    };

    public static float[] FLT_VALUES = {
                                            Float.NEGATIVE_INFINITY,
                                            Float.MIN_VALUE,
                                            -1,
                                            0,
                                            1,
                                            Float.MAX_VALUE,
                                            Float.POSITIVE_INFINITY
    };

    public static int[]  INT_VALUES = {
                                        Integer.MIN_VALUE,
                                        -1,
                                        0,
                                        1,
                                        Integer.MAX_VALUE
    };

    public static long[] LNG_VALUES = {
                                        Long.MIN_VALUE,
                                        -1,
                                        0,
                                        1,
                                        Long.MAX_VALUE
    };

    public static short[] SHORT_VALUES = {
                                            Short.MIN_VALUE,
                                            -1,
                                            0,
                                            1,
                                            Short.MAX_VALUE
    };

    public static void main (String argv[]) {

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        pipe.println("ready");

        String instruction = pipe.readln();

        if ( instruction.equals("quit") ) {
            log.display("DEBUGEE> \"quit\" signal received.");
            log.display("DEBUGEE> completed succesfully.");
            System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
        }
        log.complain("DEBUGEE FAILURE> unexpected signal "
                         + "(no \"quit\") - " + instruction);
        log.complain("DEBUGEE FAILURE> TEST FAILED");
        System.exit(Consts.TEST_FAILED + Consts.JCK_STATUS_BASE);
    }
}

class setvaluesl003aClassToCheck {
    public      byte[] publicByteArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    protected   byte[] protecByteArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private     byte[] privatByteArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    public      char[] publicCharArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    protected   char[] protecCharArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private     char[] privatCharArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    public      double[] publicDoubleArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    protected   double[] protecDoubleArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private     double[] privatDoubleArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    public      float[] publicFloatArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    protected   float[] protecFloatArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private     float[] privatFloatArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    public      int[] publicIntArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    protected   int[] protecIntArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private     int[] privatIntArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    public      long[] publicLongArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    protected   long[] protecLongArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private     long[] privatLongArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    public      short[] publicShortArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    protected   short[] protecShortArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private     short[] privatShortArr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
}
