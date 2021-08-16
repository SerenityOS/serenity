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

import nsk.share.TestFailure;

import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

class test2 {
    private final int[] MethodID = {Globals.MethodID_Array[1], Globals.MethodID_Array[2]};

    public void CallCallMe(Vector summation, Vector ID, Long functionDepth, Integer staticFunctionDepth)
            throws InvocationTargetException {
        Globals.appendSumToSummationVector(MethodID[1], summation);

        if (CGT.shouldFinish()) {
            return;
        }

        if (Globals.VERBOSE) {
            System.out.println("test2.CallCallMe");
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
            callMe(summation, ID, numFcalls, staticFcalls);
            return;
        }


        try {
            methodCallStr.nextMethod.invoke(methodCallStr.instance,
                    new Object[]{summation, ID, numFcalls, staticFcalls});
        } catch (IllegalAccessException iax) {
            throw new TestFailure("Illegal Access Exception");
        }
    }

    public void callMe(Vector summation, Vector ID, Long functionDepth, Integer staticFunctionDepth)
            throws InvocationTargetException {
        Globals.appendSumToSummationVector(MethodID[0], summation);

        if (CGT.shouldFinish()) {
            return;
        }

        if (Globals.VERBOSE) {
            System.out.println("test2.callMe");
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


        try {
            methodCallStr.nextMethod.invoke(methodCallStr.instance,
                    new Object[]{summation, ID, numFcalls, staticFcalls});
        } catch (IllegalAccessException iax) {
            throw new TestFailure("Illegal Access Exception");
        }

    }
}
