/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8238178
 * @summary Checks the C1 RangeCheckEliminator::Verification code for nested exceptions in loops that are always executed once on the non-exceptional path.
 *
 * @run main/othervm -Xbatch -XX:TieredStopAtLevel=1 -XX:CompileCommand=dontinline,compiler.c1.RangeCheckVerificationOfIR::throwException*
 *                   -XX:CompileCommand=dontinline,compiler.c1.RangeCheckVerificationOfIR::test* compiler.c1.RangeCheckVerificationOfIR
 */

package compiler.c1;

public class RangeCheckVerificationOfIR {

    int a;
    int i1;
    int i2;
    int i3;

    public static void main(String[] args) {
        RangeCheckVerificationOfIR instance = new RangeCheckVerificationOfIR();
        instance.resetValues();
        for (int i = 0; i < 1000; i++) {
            instance.testSimple();
            instance.resetValues();
            instance.testDominatedByXhandler();
            instance.resetValues();
            instance.testThrowOneException();
            instance.resetValues();
            instance.testNestedExceptions();
            instance.resetValues();
            instance.testTriplyNestedExceptions();
            instance.resetValues();
            instance.testTriplyNestedExceptions2();
            instance.resetValues();
            instance.testTriplyNestedMultipleHandlers();
            instance.resetValues();
            instance.testTriplyNestedNoExceptionThrown();
            instance.resetValues();
        }
    }

    private void resetValues() {
        i1 = 0;
        i2 = 0;
        i3 = 0;
    }

    // Is handled by current code (xhandler equals a pred of loop header block)
    public void testSimple() {
        int[] iArr = new int[8];
        for (int i = 0; i < 8; i++) {
            iArr[0] = 4;
        }

        while (true) {
            try {
                throwException();
                break;
            } catch(Exception ex1) {
                i1++;
            }
        }

        for (int i = 0; i < 10; i++) {
            a = 5;
        }
    }

    // Is handled by current code (xhandler dominates a pred of loop header block)
    public void testDominatedByXhandler() {
        int[] iArr = new int[8];
        for (int i = 0; i < 8; i++) {
            iArr[0] = 4;
        }

        while (true) {
            try {
                throwException();
                break;
            } catch (Exception ex1) {
                if (i1 < i2) {
                    a = 3;
                } else {
                    a = 4;
                }
                i1++;
            }
        }

        for (int i = 0; i < 10; i++) {
            a = 5;
        }
    }

    // Not a problem, since no backbranch and therefore no loop
    public void testThrowOneException() {
        int[] iArr = new int[8];
        for (int i = 0; i < 8; i++) {
            iArr[0] = 4;
        }

        try {
            for (int i = 0; i < iArr[4]; i++) {
                throwException();
            }
        } catch (Exception ex) {
            a = 345;
        }

        try {
            while (true) {
                throwException();
                break;
            }
        } catch (Exception e) {
            a = 45;
        }

        for (int i = 0; i < 10; i++) {
            a = 5;
        }
    }

    // All following cases are not handled yet. Need to walk backbranch of loop header block
    // to find one of the exception handlers of loop header block somewhere. Must exist.
    public void testNestedExceptions() {
        int[] iArr = new int[8];
        for (int i = 0; i < 8; i++) {
            iArr[0] = 4;
        }

        // The block from the backbranch, lets say B, is no xhandler block and not dominated by either of the two xhandler blocks, lets say
        // E1 for the outer and E2 for the inner try/catch block: If no exception occurs in E1, then E1 is completely executed without
        // executing E2. But if an exception occurs, then only parts of E1 is executed and E2 is executed completely.
        // Therefore, neither of them dominates B.
        while (true) {
            try {
                throwException();
                break;
            } catch (Exception ex1) {
                i1++;
                try {
                    throwException2();
                } catch (Exception ex2) {
                    if (i1 < i2) {
                        a = 3;
                    } else {
                        a = 4;
                    }
                    i2++;
                }
                if (i1 < i2) {
                    a = 3;
                } else {
                    a = 4;
                }
                i1++;
            }
        }

        for (int i = 0; i < 10; i++) {
            a = 5;
        }
    }

    public void testTriplyNestedExceptions() {
        int[] iArr = new int[8];
        for (int i = 0; i < 8; i++) {
            iArr[0] = 4;
        }

        while (true) {
            try {
                throwException();
                break;
            } catch (Exception ex1) {
                i1++;
                try {
                    throwException2();
                } catch (Exception ex2) {
                    if (i1 < i2) {
                        a = 3;
                    } else {
                        a = 4;
                    }
                    try {
                        throwException3();
                    } catch (Exception ex3) {
                        i3++;
                    }
                    try {
                        throwException3();
                    } catch (Exception ex3) {
                        i3++;
                    }
                    i2++;
                }
                if (i1 < i2) {
                    a = 3;
                } else {
                    a = 4;
                }
                i1++;
            }
        }

        for (int i = 0; i < 10; i++) {
            a = 5;
        }
    }

    public void testTriplyNestedExceptions2() {
        int[] iArr = new int[8];
        for (int i = 0; i < 8; i++) {
            iArr[0] = 4;
        }

        try {
            for (int i = 0; i < iArr[4]; i++) {
                throwException();
            }
        } catch (Exception ex) {
            a = 345;
        }

        while (true) {
            try {
                throwException();
                break;
            } catch (Exception ex1) {
                i1++;
                try {
                    throwException2();
                } catch (Exception ex2) {
                    if (i1 < i2) {
                        a = 3;
                    } else {
                        a = 4;
                    }
                    try {
                        throwException3();
                    } catch (Exception ex3) {
                        i3++;
                    }
                    try {
                        throwException3();
                    } catch (Exception ex3) {
                        i3++;
                    }
                    i2++;
                }
                if (i1 < i2) {
                    a = 3;
                } else {
                    a = 4;
                }
                i1++;
            }
        }

        for (int i = 0; i < 10; i++) {
            a = 5;
        }
    }

    public void testTriplyNestedMultipleHandlers() {
        int[] iArr = new int[8];
        for (int i = 0; i < 8; i++) {
            iArr[0] = 4;
        }

        try {
            for (int i = 0; i < iArr[4]; i++) {
                throwException();
            }
        } catch (Exception ex) {
            a = 345;
        }

        try {
            while (true) {
                try {
                    throwException();
                    break;
                } catch (MyInnerException ie) {
                    i1++;
                    try {
                        throwException2();
                    } catch (Exception ex2) {
                        if (i1 < i2) {
                            a = 3;
                        } else {
                            a = 4;
                        }
                        try {
                            throwException3();
                        } catch (Exception ex3) {
                            i3++;
                        }
                        try {
                            throwException3();
                        } catch (Exception ex3) {
                            i3++;
                        }
                        i2++;
                    }
                    if (i1 < i2) {
                        a = 3;
                    } else {
                        a = 4;
                    }
                    i1++;
                }
            }
        } catch (MyOuterException oe) {
            a = 45;
        }

        for (int i = 0; i < 10; i++) {
            a = 5;
        }
    }

    public void testTriplyNestedNoExceptionThrown() {
        int[] iArr = new int[8];
        for (int i = 0; i < 8; i++) {
            iArr[0] = 4;
        }

        try {
            for (int i = 0; i < iArr[4]; i++) {
                throwException();
            }
        } catch (Exception ex) {
            a = 345;
        }

        try {
            while (true) {
                try {
                    a = 4;
                    break;
                } catch (RuntimeException ie) {
                    i1++;
                    try {
                        throwException2();
                    } catch (Exception ex2) {
                        if (i1 < i2) {
                            a = 3;
                        } else {
                            a = 4;
                        }
                        try {
                            throwException3();
                        } catch (Exception ex3) {
                            i3++;
                        }
                        try {
                            throwException3();
                        } catch (Exception ex3) {
                            i3++;
                        }
                        i2++;
                    }
                    if (i1 < i2) {
                        a = 3;
                    } else {
                        a = 4;
                    }
                    i1++;
                }
            }
        } catch (Exception e) {
            a = 45;
        }

        for (int i = 0; i < 10; i++) {
            a = 5;
        }
    }

    void throwException() throws MyInnerException, MyOuterException {
        if (i1 < 3) {
            throw new MyInnerException();
        }
        if (i1 < 5) {
            throw new MyOuterException();
        }
    }

    public void throwException2() throws Exception {
        if (i2 < 3) {
            throw new RuntimeException();
        }
    }

    public void throwException3() throws Exception {
        if (i3 < 2) {
            throw new RuntimeException();
        }
    }

    class MyInnerException extends Exception { }

    class MyOuterException extends Exception { }
}
