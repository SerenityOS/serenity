/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019 SAP SE. All rights reserved.
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

/**
 * @test
 * @summary Check that methods are printed properly.
 * @compile -encoding UTF-8 TestPrintingMethods.java
 * @compile TeMe3_C.jasm
 * @run main/othervm -Xbootclasspath/a:. test.TestPrintingMethods
 */

package test;

public class TestPrintingMethods {

    private static String expectedErrorMessage_VV       = "void test.TeMe3_B.ma()";
    private static String expectedErrorMessage_integral = "double[][] test.TeMe3_B.ma(int, boolean, byte[][], float)";
    private static String expectedErrorMessage_classes  = "test.TeMe3_B[][] test.TeMe3_B.ma(java.lang.Object[][][])";
    private static String expectedErrorMessage_unicode  = "java.lang.Object test.TeMe3_B.m\u20ac\u00a3a(java.lang.Object)";

    static void checkMsg(Error e, String expected) throws Exception {
        String errorMsg = e.getMessage();
        if (errorMsg == null) {
            throw new RuntimeException("Caught AbstractMethodError with empty message.");
        } else if (errorMsg.contains(expected)) {
            System.out.println("Passed with message: " + errorMsg);
        } else {
            System.out.println("Expected method to be printed as \"" + expected + "\"\n" +
                               "in exception message:  " + errorMsg);
            throw new RuntimeException("Method not printed as expected.");
        }
    }

    // Call various missing methods to check that the exception
    // message contains the proper string for the method name and
    // signature. We expect Java-like printing of parameters etc.
    static void test() throws Exception {
        TeMe3_A c = new TeMe3_C();

        try {
            c.ma();
            throw new RuntimeException("Expected AbstractMethodError was not thrown.");
        } catch (AbstractMethodError e) {
            checkMsg(e, expectedErrorMessage_VV);
        }

        try {
            c.ma(2, true, new byte[2][3], 23.4f);
            throw new RuntimeException("Expected AbstractMethodError was not thrown.");
        } catch (AbstractMethodError e) {
            checkMsg(e, expectedErrorMessage_integral);
        }

        try {
            c.ma(new java.lang.Object[1][2][3]);
            throw new RuntimeException("Expected AbstractMethodError was not thrown.");
        } catch (AbstractMethodError e) {
            checkMsg(e, expectedErrorMessage_classes);
        }

        try {
            c.m\u20ac\u00a3a(new java.lang.Object());
            throw new RuntimeException("Expected AbstractMethodError was not thrown.");
        } catch (AbstractMethodError e) {
            checkMsg(e, expectedErrorMessage_unicode);
        }
    }

    public static void main(String[] args) throws Exception {
        test();
    }
}

// Helper classes to test abstract method error.
//
// Errorneous versions of these classes are implemented in java
// assembler.


// -----------------------------------------------------------------------
// Test AbstractMethod error shadowing existing implementation.
//
// Class hierachy:
//
//           A           // A class implementing m() and similar.
//           |
//           B           // An abstract class defining m() abstract.
//           |
//           C           // An errorneous class lacking an implementation of m().
//
class TeMe3_A {
    public void ma() {
        System.out.print("A.ma()");
    }
    public double[][] ma(int i, boolean z, byte[][] b, float f) {
        return null;
    }
    public TeMe3_B[][] ma(java.lang.Object[][][] o) {
        return null;
    }
    public java.lang.Object m\u20ac\u00a3a(java.lang.Object s) {
        return null;
    }
}

abstract class TeMe3_B extends TeMe3_A {
    public abstract void ma();
    public abstract double[][] ma(int i, boolean z, byte[][] b, float f);
    public abstract TeMe3_B[][] ma(java.lang.Object[][][] o);
    public abstract java.lang.Object m\u20ac\u00a3a(java.lang.Object s);
}

// An errorneous version of this class is implemented in java
// assembler.
class TeMe3_C extends TeMe3_B {
    // These methods are missing in the .jasm implementation.
    public void ma() {
        System.out.print("C.ma()");
    }
    public double[][] ma(int i, boolean z, byte[][] b, float f) {
        return new double[2][2];
    }
    public TeMe3_B[][] ma(java.lang.Object[][][] o) {
        return new TeMe3_C[3][3];
    }
    public java.lang.Object m\u20ac\u00a3a(java.lang.Object s) {
        return new java.lang.Object();
    }
}

