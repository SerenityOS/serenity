/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4386002 4429245
 * @summary Test fix for: Incorrect values reported for some locals of type long
 * @author Tim Bell
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g FetchLocals.java
 * @run driver FetchLocals
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import java.util.*;

class FetchLocalsDebugee {
    public long testMethod() {
        short s = 12345;
        int i = 8675309;
        boolean pt = true;
        long w = 973230999L;
        byte b = 0x3b;
        long x = w * 1000L;
        char c = '\u005A';      // 005A = "Z"
        long y = 22;
        float f = 6.66f;
        double d = 7.77;

        System.out.print("pt is: ");
        System.out.println(pt);
        System.out.print("b is: ");
        System.out.println(b);
        System.out.print("c is: ");
        System.out.println(c);
        System.out.print("s is: ");
        System.out.println(s);
        System.out.print("i is: ");
        System.out.println(i);

        System.out.print("w is: ");
        System.out.print(w);
        System.out.print("     (0x");
        System.out.print(Long.toHexString(w));
        System.out.println(")");

        System.out.print("x is: ");
        System.out.print(x);
        System.out.print("  (0x");
        System.out.print(Long.toHexString(x));
        System.out.println(")");

        System.out.print("y is: ");
        System.out.print(y);
        System.out.print("            (0x");
        System.out.print(Long.toHexString(y));
        System.out.println(")");

        System.out.print("f is: ");
        System.out.println(f);
        System.out.print("d is: ");
        System.out.println(d);
        System.out.println();   // This is FetchLocals::LINE
        if (w == 0xde00ad00be00ef00L) {
          System.out.print  ("The debugger was here.  w modified to: 0x");
          System.out.println(Long.toHexString(w));
        } else {
          System.out.print  ("w contains : 0x");
          System.out.println(Long.toHexString(w));
        }
        System.out.println();
        return x;
    }
    public static void main(String[] args) {
        System.out.print  ("FetchLocalsDebugee");
        System.out.println(" Starting up...");
        FetchLocalsDebugee my = new FetchLocalsDebugee ();
        long result = my.testMethod();
        System.out.print  ("testMethod() returned: ");
        System.out.print  (result);
        System.out.print  (" (0x");
        System.out.print  (Long.toHexString(result));
        System.out.println(")");

        System.out.print  ("FetchLocalsDebugee");
        System.out.println(" Shutting down...");
    }
}

public class FetchLocals extends TestScaffold {
    static final int LINE = 86;

    FetchLocals (String args[]) {
        super(args);
    }

    public static void main(String[] args)
        throws Exception
    {
        new FetchLocals (args).startTests();
    }

    /** Express a 64 bit double as a hex string
      */
    private static String hexify(double d) {
        long bits = Double.doubleToLongBits(d);
        return (" (0x" + java.lang.Long.toHexString(bits) + ")");
    }
    /** Express a 32 bit float as a hex string
      */
    private static String hexify(float f) {
        int bits = Float.floatToIntBits(f);
        return (" (0x" + java.lang.Integer.toHexString(bits) + ")");
    }

    protected void test(String name, BooleanValue testV, boolean expectV)
        throws Exception
    {
        if (testV.value() != expectV) {
            System.out.println("Error for " + name + " = " + testV.value() +
                                 " should be: " + expectV);
            testFailed = true;
        } else {
            System.out.print  ("Tested OK: ");
            System.out.print  (name);
            System.out.print  (" = ");
            System.out.println(expectV);
        }
    }

    protected void test(String name, ByteValue testV, byte expectV)
        throws Exception
    {
        if (testV.value() != expectV) {
            System.out.println("Error for " + name + " = " + testV.value() +
                                 " should be: " + expectV);
            testFailed = true;
        } else {
            System.out.print  ("Tested OK: ");
            System.out.print  (name);
            System.out.print  (" = ");
            System.out.println(expectV);
        }
    }

    protected void test(String name, CharValue testV, char expectV)
        throws Exception
    {
        if (testV.value() != expectV) {
            System.out.println("Error for " + name + " = " + testV.value() +
                                 " should be: " + expectV);
            testFailed = true;
        } else {
            System.out.print  ("Tested OK: ");
            System.out.print  (name);
            System.out.print  (" = ");
            System.out.println(expectV);
        }
    }

    protected void test(String name, ShortValue testV, short expectV)
        throws Exception
    {
        if (testV.value() != expectV) {
            System.out.println("Error for " + name + " = " + testV.value() +
                                 " should be: " + expectV);
            testFailed = true;
        } else {
            System.out.print  ("Tested OK: ");
            System.out.print  (name);
            System.out.print  (" = ");
            System.out.println(expectV);
        }
    }

    protected void test(String name, IntegerValue testV, int expectV)
        throws Exception
    {
        if (testV.value() != expectV) {
            System.out.println("Error for " + name + " = " + testV.value() +
                                 " should be: " + expectV);
            testFailed = true;
        } else {
            System.out.print  ("Tested OK: ");
            System.out.print  (name);
            System.out.print  (" = ");
            System.out.println(expectV);
        }
    }

    protected void test(String name, LongValue testV, long expectV)
        throws Exception
    {
        if (testV.value() != expectV) {
            System.out.println("Error for " + name + " = 0x" +
                                 Long.toHexString(testV.value()) +
                                 " should be: 0x" +
                                 Long.toHexString(expectV));
            testFailed = true;
        } else {
            System.out.print  ("Tested OK: ");
            System.out.print  (name);
            System.out.print  (" = ");
            System.out.println(expectV);
        }
    }

    protected void test(String name, FloatValue testV, float expectV)
        throws Exception
    {
        if (testV.value() != expectV) {
            System.out.println("Error for " + name + " = " + testV.value() +
                               hexify(testV.value()) +
                               " should be: " + expectV +
                               hexify(expectV));
            testFailed = true;
        } else {
            System.out.print  ("Tested OK: ");
            System.out.print  (name);
            System.out.print  (" = ");
            System.out.println(expectV);
        }
    }

    protected void test(String name, DoubleValue testV, double expectV)
        throws Exception
    {
        if (testV.value() != expectV) {
            System.out.println("Error for " + name + " = " + testV.value() +
                               hexify(testV.value()) +
                               " should be: " + expectV +
                               hexify(expectV));
            testFailed = true;
        } else {
            System.out.print  ("Tested OK: ");
            System.out.print  (name);
            System.out.print  (" = ");
            System.out.println(expectV);
        }
    }

    protected void testLocalVariables (StackFrame sf)
        throws Exception
    {
        /*
         * Test values in the local method testMethod ():
         *   1) Read current data
         *   2) Test against the expected value
         *   3) Set to a new value
         *   4) Read again
         *   5) Test against the expected value
         */
        LocalVariable lv = sf.visibleVariableByName("pt");
        BooleanValue booleanV = (BooleanValue) sf.getValue(lv);
        test("pt", booleanV, true);
        booleanV = vm().mirrorOf(false);
        sf.setValue(lv, booleanV);
        booleanV = (BooleanValue) sf.getValue(lv);
        test("pt", booleanV, false);

        lv = sf.visibleVariableByName("b");
        ByteValue byteV = (ByteValue) sf.getValue(lv);
        byte bTmp = 0x3b;
        test("b", byteV, bTmp);
        bTmp = 0x7e;
        byteV = vm().mirrorOf(bTmp);
        sf.setValue(lv, byteV);
        byteV = (ByteValue) sf.getValue(lv);
        test("b", byteV, bTmp);

        lv = sf.visibleVariableByName("c");
        CharValue charV = (CharValue) sf.getValue(lv);
        char cTmp = '\u005A';
        test("c", charV, cTmp);
        cTmp = 'A';
        charV = vm().mirrorOf(cTmp);
        sf.setValue(lv, charV);
        charV = (CharValue) sf.getValue(lv);
        test("c", charV, cTmp);

        lv = sf.visibleVariableByName("s");
        ShortValue shortV = (ShortValue) sf.getValue(lv);
        short sTmp = 12345;
        test("s", shortV, sTmp);
        sTmp = -32766;
        shortV = vm().mirrorOf(sTmp);
        sf.setValue(lv, shortV);
        shortV = (ShortValue) sf.getValue(lv);
        test("s", shortV, sTmp);

        lv = sf.visibleVariableByName("i");
        IntegerValue integerV = (IntegerValue) sf.getValue(lv);
        int iTmp = 8675309;
        test("i", integerV, iTmp);
        iTmp = -42;
        integerV = vm().mirrorOf(iTmp);
        sf.setValue(lv, integerV);
        integerV = (IntegerValue) sf.getValue(lv);
        test("i", integerV, iTmp);

        lv = sf.visibleVariableByName("w");
        LongValue longV = (LongValue) sf.getValue(lv);
        long wTmp = 973230999L;
        test("w", longV, wTmp);
        wTmp = 0xde00ad00be00ef00L;
        longV = vm().mirrorOf(wTmp);
        sf.setValue(lv, longV);
        longV = (LongValue) sf.getValue(lv);
        test("w", longV, wTmp);

        lv = sf.visibleVariableByName("x");
        longV = (LongValue) sf.getValue(lv);
        long xTmp = 973230999L * 1000L;
        test("x", longV, xTmp);
        xTmp = 0xca00fe00ba00be00L;
        longV = vm().mirrorOf(xTmp);
        sf.setValue(lv, longV);
        longV = (LongValue) sf.getValue(lv);
        test("x", longV, xTmp);

        lv = sf.visibleVariableByName("y");
        longV = (LongValue) sf.getValue(lv);
        long yTmp = 22;
        test("y", longV, yTmp);
        yTmp = 0xdeadbeefcafebabeL;
        longV = vm().mirrorOf(yTmp);
        sf.setValue(lv, longV);
        longV = (LongValue) sf.getValue(lv);
        test("x", longV, yTmp);

        lv = sf.visibleVariableByName("f");
        FloatValue floatV = (FloatValue) sf.getValue(lv);
        float fTmp = 6.66f;
        test("f", floatV, fTmp);
        fTmp = (float)java.lang.Math.PI;
        floatV = vm().mirrorOf(fTmp);
        sf.setValue(lv, floatV);
        floatV = (FloatValue)sf.getValue(lv);
        test("f", floatV, fTmp);

        lv = sf.visibleVariableByName("d");
        DoubleValue doubleV = (DoubleValue) sf.getValue(lv);
        double dTmp = 7.77;
        test("d", doubleV, dTmp);
        dTmp = java.lang.Math.E;
        doubleV = vm().mirrorOf(dTmp);
        sf.setValue(lv, doubleV);
        doubleV = (DoubleValue) sf.getValue(lv);
        test("d", doubleV, dTmp);
    }

    protected void runTests()
        throws Exception
    {
        startToMain("FetchLocalsDebugee");
        /*
         * Get to the bottom of testMethod():
         */
        try {
            BreakpointEvent bpe = resumeTo("FetchLocalsDebugee", LINE);
            /*
             * Fetch values from fields; what did we get?
             */
            StackFrame sf = bpe.thread().frame(0);
            testLocalVariables (sf);

        } catch(Exception ex) {
            ex.printStackTrace();
            testFailed = true;
        } finally {
            // Allow application to complete and shut down
            resumeToVMDisconnect();
        }
        if (!testFailed) {
            System.out.println("FetchLocals: passed");
        } else {
            throw new Exception("FetchLocals: failed");
        }
    }
}
