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

import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

class test4 extends test2 {
    private final int[] MethodID = {Globals.MethodID_Array[1], Globals.MethodID_Array[5], Globals.MethodID_Array[6]};

    // this method verifies that a child can make a call to its parent
    public void CallCallMe(Vector summation, Vector ID, Long functionDepth, Integer staticFunctionDepth)
            throws InvocationTargetException {
        Globals.appendSumToSummationVector(MethodID[1], summation);

        if (CGT.shouldFinish()) {
            return;
        }

        if (Globals.VERBOSE) {
            System.out.println("test4.CallCallMe");
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

            Globals.addFunctionIDToVector(methodCallStr.id, ID);
        } else {
            numFcalls = Long.valueOf(functionDepth.longValue() - 1);
            staticFcalls = staticFunctionDepth;
            Globals.addFunctionIDToVector(MethodID[0], ID);
            super.callMe(summation, ID, numFcalls, staticFcalls);
            return;
        }


        Globals.callMethod(methodCallStr, summation, ID, numFcalls, staticFcalls);
    }

    // this method makes a Y fork in the method call structure
    public void callMe(Vector summation, Vector ID, Long functionDepth, Integer staticFunctionDepth)
            throws InvocationTargetException {
        Globals.appendSumToSummationVector(MethodID[2], summation);

        if (CGT.shouldFinish()) {
            return;
        }

        if (Globals.VERBOSE) {
            System.out.println("test4.callMe");
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
            long temp = functionDepth.longValue() - 2;
            numFcalls = Long.valueOf(temp / 2);
            staticFcalls = staticFunctionDepth;

            if (Globals.VERBOSE) {
                System.out.println(" test4.callMe - Starting Branch 1");
            }
            methodCallStr = Globals.nextRandomMethod();
            Globals.addFunctionIDToVector(methodCallStr.id, ID);
            Globals.callMethod(methodCallStr, summation, ID, numFcalls, staticFcalls);

            if (CGT.shouldFinish()) {
                return;
            }

            temp -= temp / 2;
            if (temp < 0) {
                if (Globals.VERBOSE) {
                    System.out.println(" test4.callMe - Skipping Branch 2");
                }
                return;
            }
            if (Globals.VERBOSE) {
                System.out.println(" test4.callMe - Starting Branch 2");
            }
            numFcalls = Long.valueOf(temp);
            methodCallStr = Globals.nextRandomMethod();
        }
        Globals.addFunctionIDToVector(methodCallStr.id, ID);
        Globals.callMethod(methodCallStr, summation, ID, numFcalls, staticFcalls);
    }

}
