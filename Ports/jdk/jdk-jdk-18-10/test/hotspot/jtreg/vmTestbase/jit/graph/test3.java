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

import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import java.util.Vector;

class test3 extends test1 {

    private final int[] MethodID = {Globals.MethodID_Array[3], Globals.MethodID_Array[4]};
    private static Random loopNumGen = new Random(Utils.SEED);

    private final int maxLoops = 10;
    private int localNumLoops = loopNumGen.nextInt(maxLoops);

    public void selfRecursion(Vector summation, Vector ID, Long functionDepth, Integer staticFunctionDepth)
            throws InvocationTargetException {
        Globals.appendSumToSummationVector(MethodID[1], summation);

        if (CGT.shouldFinish()) {
            return;
        }

        if (Globals.VERBOSE) {
            System.out.println("test3.selfRecursion");
        }

        if ((functionDepth.longValue() <= 0) && (staticFunctionDepth.intValue() <= 0)) {
            return;
        }

        MethodData methodCallStr;
        Long numFcalls;
        Integer staticFcalls;
        // make a static call
        if (staticFunctionDepth.intValue() > 0) {
            numFcalls = functionDepth;
            staticFcalls = Integer.valueOf(staticFunctionDepth.intValue() - 1);
            methodCallStr = Globals.returnNextStaticMethod(MethodID[1]);
        } else if (localNumLoops > 0) { // make a recursive call
            numFcalls = Long.valueOf(functionDepth.longValue() - 1);
            staticFcalls = staticFunctionDepth;
            Globals.addFunctionIDToVector(MethodID[1], ID);
            localNumLoops--;
            selfRecursion(summation, ID, numFcalls, staticFcalls);
            return;
        } else { // make a random call
            numFcalls = Long.valueOf(functionDepth.longValue() - 1);
            staticFcalls = staticFunctionDepth;
            methodCallStr = Globals.nextRandomMethod();

            // get ready for the next call to this method
            localNumLoops = loopNumGen.nextInt(maxLoops);
        }
        Globals.addFunctionIDToVector(methodCallStr.id, ID);
        Globals.callMethod(methodCallStr, summation, ID, numFcalls, staticFcalls);

    }

    public void callMe(Vector summation, Vector ID, Long functionDepth, Integer staticFunctionDepth)
            throws InvocationTargetException {
        Globals.appendSumToSummationVector(MethodID[0], summation);

        if (CGT.shouldFinish()) {
            return;
        }

        if (Globals.VERBOSE) {
            System.out.println("test3.callMe");
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
        Globals.addFunctionIDToVector(methodCallStr.id, ID);
        Globals.callMethod(methodCallStr, summation, ID, numFcalls, staticFcalls);
    }
}
