/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 * @test
 * @bug 4359312 4450091
 * @summary Test PTR 1421 JVM exceptions making a call to LocalVariable.type().name()
 * @author Tim Bell (based on the PTR 1421 report submitted by IBM).
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g GetLocalVariables.java
 * @run driver GetLocalVariables
 */

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

 /********** target program **********/

class GetLocalVariablesTarg {
    private static char s_char1 = 'a';
    private static char s_char2 = (char) 0;
    private static char s_char3 = (char) 1;
    private static char s_char4 = (char) 32;
    private static char s_char5 = '\u7ffe';
    private static char s_char6 = '\u7fff';
    private static char s_char7 = '\u8000';
    private static char s_char8 = '\u8001';
    private static char s_char9 = '\ufffe';
    private static char s_char10 = '\uffff';

    private static byte s_byte1 = (byte) 146;
    private static byte s_byte2 = (byte) 0;
    private static byte s_byte3 = (byte) 1;
    private static byte s_byte4 = (byte) 15;
    private static byte s_byte5 = (byte) 127;
    private static byte s_byte6 = (byte) 128;
    private static byte s_byte7 = (byte) - 1;
    private static byte s_byte8 = (byte) - 15;
    private static byte s_byte9 = (byte) - 127;
    private static byte s_byte10 = (byte) - 128;

    private static short s_short1 = (short) 28123;
    private static short s_short2 = (short) 0;
    private static short s_short3 = (short) 1;
    private static short s_short4 = (short) 15;
    private static short s_short5 = (short) 0x7ffe;
    private static short s_short6 = (short) 0x7fff;
    private static short s_short7 = (short) -1;
    private static short s_short8 = (short) -15;
    private static short s_short9 = (short) -0x7ffe;
    private static short s_short10 = (short) -0x7fff;

    private static int s_int1 = 3101246;
    private static int s_int2 = 0;
    private static int s_int3 = 1;
    private static int s_int4 = 15;
    private static int s_int5 = 0x7ffffffe;
    private static int s_int6 = 0x7fffffff;
    private static int s_int7 = -1;
    private static int s_int8 = -15;
    private static int s_int9 = -0x7ffffffe;
    private static int s_int10 = -0x7fffffff;

    private static long s_long1 = 0x0123456789ABCDEFL;
    private static long s_long2 = 0;
    private static long s_long3 = 1;
    private static long s_long4 = 15;
    private static long s_long5 = 0x000000007fffffffL;
    private static long s_long6 = 0x0000000080000000L;
    private static long s_long7 = 0x0000000100000000L;
    private static long s_long8 = 0x7fffffffffffffffL;
    private static long s_long9 = 0x8000000000000000L;
    private static long s_long10 = -15;

    private static float s_float1 = 2.3145f;
    private static float s_float2 = 0f;

    private static double s_double1 = 1.469d;
    private static double s_double2 = 0;

    private static int s_iarray1[] = {1, 2, 3};
    private static int s_iarray2[] = null;

    private static int s_marray1[][] = {{1, 2, 3}, {3, 4, 5}, null, {6, 7}};
    private static int s_marray2[][] = null;

    private static String s_sarray1[] = {"abc", null, "def", "ghi"};
    private static String s_sarray2[] = null;
    private static Object s_sarray3[] = s_sarray1;

    private static String s_string1 = "abcdef";
    private static String s_string2 = null;
    private static String s_string3 = "a\u1234b\u7777";

    private char i_char;
    private byte i_byte;
    private short i_short;
    private int i_int;
    private long i_long;
    private float i_float;
    private double i_double;
    private int i_iarray[];
    private int i_marray[][];
    private String i_string;

    public GetLocalVariablesTarg()
    {
        int index;

        i_char = 'B';
        i_byte = 120;
        i_short = 12048;
        i_int = 0x192842;
        i_long = 123591230941L;
        i_float = 235.15e5f;
        i_double = 176e-1d;
        i_iarray = new int[5];
        i_marray = new int[7][];
        i_string = "empty";

        for (index = 0; index < i_iarray.length; ++index)
            i_iarray[index] = index + 1;

        i_marray[0] = new int[2];
        i_marray[1] = new int[4];
        i_marray[2] = null;
        i_marray[3] = new int[1];
        i_marray[4] = new int[3];
        i_marray[5] = null;
        i_marray[6] = new int[7];

        for (index = 0; index < i_marray.length; ++index)
            if (i_marray[index] != null)
                for (int index2 = 0; index2 < i_marray[index].length; ++index2)
                    i_marray[index][index2] = index + index2;
    }

    public GetLocalVariablesTarg(char p_char, byte p_byte, short p_short,
                        int p_int, long p_long, float p_float,
                        double p_double, int p_iarray[], int p_marray[][],
                        String p_string)
    {
        i_char = p_char;
        i_byte = p_byte;
        i_short = p_short;
        i_int = p_int;
        i_long = p_long;
        i_float = p_float;
        i_double = p_double;
        i_iarray = p_iarray;
        i_marray = p_marray;
        i_string = p_string;
    }

    public static void test_expressions()
    {
        GetLocalVariablesTarg e1 = new GetLocalVariablesTarg();
        GetLocalVariablesTarg e2 = null;
        GetLocalVariablesTarg e3 = e1;
        GetLocalVariablesTarg e4 = new GetLocalVariablesTarg(s_char1, s_byte1, s_short1, s_int1,
                                           s_long1, s_float1, s_double1,
                                           s_iarray1, s_marray1, "e4");
        GetLocalVariablesTarg e5 = new GetLocalVariablesTarg(s_char2, s_byte2, s_short2, s_int2,
                                           s_long2, s_float2, s_double2,
                                           s_iarray2, s_marray2, "e5");

        char l_char = (char) (
                           s_char1 + s_char2 + s_char3 + s_char4 + s_char5 +
                          s_char6 + s_char7 + s_char8 + s_char9 + s_char10);
        byte l_byte = (byte) (
                           s_byte1 + s_byte2 + s_byte3 + s_byte4 + s_byte5 +
                          s_byte6 + s_byte7 + s_byte8 + s_byte9 + s_byte10);
        short l_short = (short) (
                      s_short1 + s_short2 + s_short3 + s_short4 + s_short5 +
                     s_short6 + s_short7 + s_short8 + s_short9 + s_short10);
        int l_int = s_int1 + s_int2 + s_int3 + s_int4 + s_int5 +
        s_int6 + s_int7 + s_int8 + s_int9 + s_int10;
        long l_long = s_long1 + s_long2 + s_long3 + s_long4 + s_long5 +
        s_long6 + s_long7 + s_long8 + s_long9 + s_long10;
        float l_float = s_float1 + s_float2;
        double l_double = s_double1 + s_double2;
        int[] l_iarray = null;
        int[][] l_marray = null;
        String l_string = s_string1 + s_string3 + s_sarray1[0];

        if (s_sarray2 == null)
            l_string += "?";

        if (s_sarray3 instanceof String[])
            l_string += "<io>";

        Object e6 = new GetLocalVariablesTarg(l_char, l_byte, l_short, l_int,
                                     l_long, l_float, l_double, l_iarray,
                                     l_marray, l_string);

        e1.test_1();            // RESUME_TO_LINE
        e3.test_1();
        e4.test_1();
        e5.test_1();
        ((GetLocalVariablesTarg) e6).test_1();

        e3 = null;
        if (e3 == e1)
            e3.test_1();
        e3 = e4;
        if (e3 == e2)
            e3 = e5;
        e3.test_1();

    }

    public void test_1()
    {
        double l_add = i_double + i_short;
        long l_subtract = i_long - i_int;
        long l_multiply = i_byte * i_int;

        i_double = l_add + i_float;
        i_short = (short) l_subtract;
        i_long = l_multiply + i_byte + i_short;
    }

    public static void main(String[] args) {
        System.out.println("Howdy!");
        test_expressions();
        System.out.println("Goodbye from GetLocalVariablesTarg!");
    }
}

 /********** test program **********/

public class GetLocalVariables extends TestScaffold {
    static final int RESUME_TO_LINE = 222;
    ReferenceType targetClass;
    ThreadReference mainThread;

    GetLocalVariables (String args[]) {
        super(args);
    }

    public static void main(String[] args)
        throws Exception
    {
        new GetLocalVariables (args).startTests();
    }

    /********** test core **********/

    protected void runTests()
        throws Exception
    {
        /*
         * Get to the top of main() to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("GetLocalVariablesTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();
        EventRequestManager erm = vm().eventRequestManager();

        bpe = resumeTo("GetLocalVariablesTarg", RESUME_TO_LINE);
        /*
         * We've arrived.  Look around at some variables.
         */
        StackFrame frame = bpe.thread().frame(0);
        List localVars = frame.visibleVariables();
        System.out.println("    Visible variables at this point are: ");
        for (Iterator it = localVars.iterator(); it.hasNext();) {
            LocalVariable lv = (LocalVariable) it.next();
            System.out.print(lv.name());
            System.out.print(" typeName: ");
            System.out.print(lv.typeName());
            System.out.print(" signature: ");
            System.out.print(lv.type().signature());
            System.out.print(" primitive type: ");
            System.out.println(lv.type().name());
        }

        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test if anything has called failure("foo")
         * testFailed will be true
         */
        if (!testFailed) {
            println("GetLocalVariables: passed");
        } else {
            throw new Exception("GetLocalVariables: failed");
        }
    }
}
