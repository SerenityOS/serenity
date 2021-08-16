/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jit.graph;

import jdk.test.lib.Utils;
import nsk.share.TestFailure;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import java.util.Vector;

class test5 {
    private final int[] MethodID = {Globals.MethodID_Array[7],
            Globals.MethodID_Array[8],
            Globals.MethodID_Array[9],
            Globals.MethodID_Array[10]};
    private static Random loopNumGen = new Random(Utils.SEED);

    private final int maxLoops = 12;

    private long factorial(int n) {
        if (n > 1) {
            return (n * factorial(n - 1));
        } else {
            return (1);
        }
    }

    private long fibonacci(long num1, long num2, int n) {
        if (n <= 0) {
            return (num2);
        } else {
            return (fibonacci(num2, num1 + num2, n - 1));
        }
    }

    private long combination(int n, int r) {
        if ((r == 0) || (n == r)) {
            return 1;
        } else {
            return (combination(n - 1, r) + combination(n - 1, r - 1));
        }
    }

    private int[] pascalsTriangle(int[] source, int n) {
        if (n > 0) {
            int sourceLength = source.length;
            int[] temp = new int[sourceLength + 1];
            temp[0] = 1;
            temp[sourceLength] = 1;

            int j = 1;
            for (int i = 0; i < (sourceLength - 1); i++) {
                temp[j++] = source[i] + source[i + 1];
            }

            return pascalsTriangle(temp, n - 1);
        } else {
            return source;
        }
    }

    private boolean verifyArray(int[] ArrayToBeVerified, int[] MasterArray) {
        if (ArrayToBeVerified.length != MasterArray.length) {
            return false;
        }

        for (int i = 0; i < MasterArray.length; i++) {
            if (MasterArray[i] != ArrayToBeVerified[i]) {
                return false;
            }
        }
        return true;
    }

    private int[] verifyPascal(int n) {
        int[] pascalOut = new int[n + 1];
        int[][] dataArray = new int[n + 1][n + 1];

        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= n; j++) {
                if (j == 0) {
                    dataArray[i][0] = 1;
                } else if (j == i) {
                    dataArray[i][i] = 1;
                } else if (j < i) {
                    dataArray[i][j] = dataArray[i - 1][j - 1] + dataArray[i - 1][j];
                }
            }
        }

        // could be a little more efficient, but not that important
        int j = n;
        for (int i = 0; i <= n; i++) {
            pascalOut[i] = dataArray[j][i];
        }
        return pascalOut;
    }

    private long verifyFact(int n) {
        long answer = 1;
        for (int i = 2; i <= n; i++) {
            answer *= i;
        }
        return answer;
    }

    private long verifyFibo(int n) {
        long num1 = 1;
        long num2 = 1;

        for (int i = 0; i < n; i++) {
            long temp = num1 + num2;
            num1 = num2;
            num2 = temp;
        }

        return num2;
    }

    private long verifyComb(int n, int r) {
        return (verifyFact(n) / (verifyFact(n - r) * verifyFact(r)));
    }

    public void factTest(Vector summation, Vector ID, Long functionDepth, Integer staticFunctionDepth)
            throws InvocationTargetException {
        Globals.appendSumToSummationVector(MethodID[0], summation);

        if (CGT.shouldFinish()) {
            return;
        }

        if (Globals.VERBOSE) {
            System.out.println("test5.factTest");
        }

        if ((functionDepth.longValue() <= 0) && (staticFunctionDepth.intValue() <= 0)) {
            return;
        }
        MethodData methodCallStr;
        Long numFcalls;
        Integer staticFcalls;

        if (staticFunctionDepth.intValue() > 0) {
            numFcalls = functionDepth;
            staticFcalls = Integer.valueOf(staticFunctionDepth.intValue() - 1);
            methodCallStr = Globals.returnNextStaticMethod(MethodID[0]);
        } else {
            numFcalls = Long.valueOf(functionDepth.longValue() - 1);
            staticFcalls = staticFunctionDepth;
            methodCallStr = Globals.nextRandomMethod();
        }

        int localNumLoops = loopNumGen.nextInt(maxLoops);
        long facFunctionValue = factorial(localNumLoops);
        long facVerValue = verifyFact(localNumLoops);
        if (facFunctionValue != facVerValue) {
            System.out.println("Factorial Computed Incorrectly");
            System.out.println("Specific Factorial Requested " + localNumLoops + "!");
            throw new TestFailure("Expected: " + facVerValue + " Actual " + facFunctionValue);
        }

        Globals.addFunctionIDToVector(methodCallStr.id, ID);
        Globals.callMethod(methodCallStr, summation, ID, numFcalls, staticFcalls);
    }

    public void fiboTest(Vector summation, Vector ID, Long functionDepth, Integer staticFunctionDepth)
            throws InvocationTargetException {
        Globals.appendSumToSummationVector(MethodID[1], summation);

        if (CGT.shouldFinish()) {
            return;
        }

        if (Globals.VERBOSE) {
            System.out.println("test5.fiboTest");
        }

        if ((functionDepth.longValue() <= 0) && (staticFunctionDepth.intValue() <= 0)) {
            return;
        }
        MethodData methodCallStr;
        Long numFcalls;
        Integer staticFcalls;
        if (staticFunctionDepth.intValue() > 0) {
            numFcalls = functionDepth;
            staticFcalls = Integer.valueOf(staticFunctionDepth.intValue() - 1);
            methodCallStr = Globals.returnNextStaticMethod(MethodID[1]);
        } else {
            numFcalls = Long.valueOf(functionDepth.longValue() - 1);
            staticFcalls = staticFunctionDepth;
            methodCallStr = Globals.nextRandomMethod();
        }
        int localNumLoops = loopNumGen.nextInt(maxLoops * 3);
        long fiboFunctionValue = fibonacci(1, 1, localNumLoops);
        long fiboVerValue = verifyFibo(localNumLoops);
        if (fiboFunctionValue != fiboVerValue) {
            System.out.println("Fibonacci Series Computed Incorrectly");
            System.out.println("Specific Digit Requested " + localNumLoops);
            throw new TestFailure("Expected: " + fiboVerValue + " Actual " + fiboFunctionValue);
        }

        Globals.addFunctionIDToVector(methodCallStr.id, ID);
        Globals.callMethod(methodCallStr, summation, ID, numFcalls, staticFcalls);
    }


    public void combTest(Vector summation, Vector ID, Long functionDepth, Integer staticFunctionDepth)
            throws InvocationTargetException {
        Globals.appendSumToSummationVector(MethodID[2], summation);

        if (CGT.shouldFinish()) {
            return;
        }

        if (Globals.VERBOSE) {
            System.out.println("test5.combTest");
        }

        if ((functionDepth.longValue() <= 0) && (staticFunctionDepth.intValue() <= 0)) {
            return;
        }
        MethodData methodCallStr;
        Long numFcalls;
        Integer staticFcalls;
        if (staticFunctionDepth.intValue() > 0) {
            numFcalls = functionDepth;
            staticFcalls = Integer.valueOf(staticFunctionDepth.intValue() - 1);
            methodCallStr = Globals.returnNextStaticMethod(MethodID[2]);
        } else {
            numFcalls = Long.valueOf(functionDepth.longValue() - 1);
            staticFcalls = staticFunctionDepth;
            methodCallStr = Globals.nextRandomMethod();
        }
        int n = loopNumGen.nextInt(maxLoops);
        int k = (n > 0) ? loopNumGen.nextInt(n) : 0;
        long combFunctionValue = combination(n, k);
        long combVerValue = verifyComb(n, k);
        if (combFunctionValue != combVerValue) {
            System.out.println("Combination Computed Incorrectly");
            System.out.println("N = " + n + "K = " + k);
            throw new TestFailure("Expected: " + combVerValue + " Actual " + combFunctionValue);
        }

        Globals.addFunctionIDToVector(methodCallStr.id, ID);
        Globals.callMethod(methodCallStr, summation, ID, numFcalls, staticFcalls);
    }


    public void pascalTest(Vector summation, Vector ID, Long functionDepth, Integer staticFunctionDepth)
            throws InvocationTargetException {
        Globals.appendSumToSummationVector(MethodID[3], summation);

        if (CGT.shouldFinish()) {
            return;
        }

        if (Globals.VERBOSE) {
            System.out.println("test5.pascalTest");
        }

        int[] x = new int[1 << 30];
        x[1 << 24] = 1;

        if ((functionDepth.longValue() <= 0) && (staticFunctionDepth.intValue() <= 0)) {
            return;
        }
        MethodData methodCallStr;
        Long numFcalls;
        Integer staticFcalls;
        if (staticFunctionDepth.intValue() > 0) {
            numFcalls = functionDepth;
            staticFcalls = Integer.valueOf(staticFunctionDepth.intValue() - 1);
            methodCallStr = Globals.returnNextStaticMethod(MethodID[3]);
        } else {
            numFcalls = Long.valueOf(functionDepth.longValue() - 1);
            staticFcalls = staticFunctionDepth;
            methodCallStr = Globals.nextRandomMethod();
        }
        int num = loopNumGen.nextInt(maxLoops);

        int[] pascalFunctionValue = pascalsTriangle(new int[]{1}, num);
        int[] pascalVerValue = verifyPascal(num);
        if (!verifyArray(pascalFunctionValue, pascalVerValue)) {
            StringBuilder temp = new StringBuilder("Expected: ");
            for (int aPascalVerValue : pascalVerValue) {
                temp.append(aPascalVerValue)
                    .append(", ");
            }
            temp.append(" Actual ");
            for (int aPascalFunctionValue : pascalFunctionValue) {
                temp.append(aPascalFunctionValue)
                    .append(", ");
            }
            System.out.println("Pascal Tringle Row Computed Incorrectly");
            System.out.println("Row Number " + num);
            throw new TestFailure(temp.toString());
        }

        Globals.addFunctionIDToVector(methodCallStr.id, ID);
        Globals.callMethod(methodCallStr, summation, ID, numFcalls, staticFcalls);
    }
}
