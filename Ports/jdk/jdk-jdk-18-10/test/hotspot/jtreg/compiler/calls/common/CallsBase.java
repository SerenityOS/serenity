/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.calls.common;

import compiler.testlibrary.CompilerUtils;
import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * A common class for Invoke* classes
 */
public abstract class CallsBase {
    public static final String CALL_ERR_MSG = "Call insuccessfull";
    protected final Method calleeMethod;
    protected final Method callerMethod;
    protected final WhiteBox wb = WhiteBox.getWhiteBox();
    protected int compileCallee = -1;
    protected int compileCaller = -1;
    protected boolean nativeCallee = false;
    protected boolean nativeCaller = false;
    protected boolean calleeVisited = false;
    protected boolean checkCallerCompilationLevel;
    protected boolean checkCalleeCompilationLevel;
    protected int expectedCallerCompilationLevel;
    protected int expectedCalleeCompilationLevel;

    protected CallsBase() {
        try {
            callerMethod = getClass().getDeclaredMethod("caller");
            calleeMethod = getClass().getDeclaredMethod("callee",
                    getCalleeParametersTypes());
            wb.testSetDontInlineMethod(callerMethod, /* dontinline= */ true);
            wb.testSetDontInlineMethod(calleeMethod, /* dontinline= */ true);
        } catch (NoSuchMethodException e) {
            throw new Error("TEST BUG: can't find test method", e);
        }
    }

    /**
     * Provides callee parameters types to search method
     * @return array of types
     */
    protected Class[] getCalleeParametersTypes() {
        return new Class[] {int.class, long.class, float.class,
            double.class, String.class};
    }

    /**
     * Loads native library(libCallsNative.so)
     */
    protected static void loadNativeLibrary() {
        System.loadLibrary("CallsNative");
    }

    /**
     * Checks if requested compilation levels are inside of current vm capabilities
     * @return true if vm is capable of requested compilation levels
     */
    protected final boolean compilationLevelsSupported() {
        int[] compLevels = CompilerUtils.getAvailableCompilationLevels();
        boolean callerCompLevelSupported = compileCaller <= 0 || (compileCaller > 0
                && Arrays.stream(compLevels)
                        .filter(elem -> elem == compileCaller)
                        .findAny()
                        .isPresent());
        boolean calleeCompLevelSupported = compileCallee <= 0 || (compileCallee > 0
                && Arrays.stream(compLevels)
                        .filter(elem -> elem == compileCallee)
                        .findAny()
                        .isPresent());
        return callerCompLevelSupported && calleeCompLevelSupported;
    }

    /**
     * Parse test arguments
     * @param args test arguments
     */
    protected final void parseArgs(String args[]) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-nativeCallee":
                    nativeCallee = true;
                    break;
                case "-nativeCaller":
                    nativeCaller = true;
                    break;
                case "-compileCallee":
                    compileCallee = Integer.parseInt(args[++i]);
                    break;
                case "-compileCaller":
                    compileCaller = Integer.parseInt(args[++i]);
                    break;
                case "-checkCallerCompileLevel":
                    checkCallerCompilationLevel = true;
                    expectedCallerCompilationLevel = Integer.parseInt(args[++i]);
                    break;
                case "-checkCalleeCompileLevel":
                    checkCalleeCompilationLevel = true;
                    expectedCalleeCompilationLevel = Integer.parseInt(args[++i]);
                    break;
                default:
                    throw new Error("Can't parse test parameter:" + args[i]);
            }
        }
    }

    /**
     * Run basic logic of a test by doing compile
     * action(if needed). An arguments can be -compileCallee
     * $calleeCompilationLevel and/or -compileCaller $callerCompilationLevel
     * and/or -nativeCaller and/or -nativeCallee to indicate that native methods
     * for caller/callee should be used
     * @param args test args
     */
    protected final void runTest(String args[]) {
        parseArgs(args);
        if (compilationLevelsSupported()) {
            if (nativeCaller || nativeCallee) {
                CallsBase.loadNativeLibrary();
            }
            Object lock = getLockObject();
            Asserts.assertNotNull(lock, "Lock object is null");
            /* a following lock is needed in case several instances of this
               test are launched in same vm */
            synchronized (lock) {
                if (compileCaller > 0 || compileCallee > 0) {
                    caller(); // call once to have everything loaded
                    calleeVisited = false; // reset state
                }
                // compile with requested level if needed
                if (compileCallee > 0 && !compileMethod(calleeMethod, compileCallee)) {
                    System.out.println("WARNING: Blocking compilation failed for calleeMethod (timeout?). Skipping.");
                    return;
                }
                if (checkCalleeCompilationLevel) {
                    Asserts.assertEQ(expectedCalleeCompilationLevel,
                            wb.getMethodCompilationLevel(calleeMethod),
                            "Unexpected callee compilation level");
                }
                if (compileCaller > 0 && !compileMethod(callerMethod, compileCaller)) {
                    System.out.println("WARNING: Blocking compilation failed for callerMethod (timeout?). Skipping.");
                    return;
                }
                if (checkCallerCompilationLevel) {
                    Asserts.assertEQ(expectedCallerCompilationLevel,
                            wb.getMethodCompilationLevel(callerMethod),
                            "Unexpected caller compilation level");
                }
                // do calling work
                if (nativeCaller) {
                    callerNative();
                } else {
                    caller();
                }
            }
        } else {
            System.out.println("WARNING: Requested compilation levels are "
                    + "out of current vm capabilities. Skipping.");
        }
    }

    /**
     * A method to compile another method, searching it by name in current class
     * @param method a method to compile
     * @param compLevel a compilation level
     * @return true if method was enqueued for compilation
     */
    protected final boolean compileMethod(Method method, int compLevel) {
        wb.deoptimizeMethod(method);
        Asserts.assertTrue(wb.isMethodCompilable(method, compLevel));
        return wb.enqueueMethodForCompilation(method, compLevel);
    }

    /*
     * @return Object to lock on during execution
     */

    protected abstract Object getLockObject();

    protected abstract void caller();

    protected abstract void callerNative();

    /**
     * A method checking values. Should be used to verify if all parameters are
     * passed as expected. Parameter N should have a value indicating number "N"
     * in respective type representation.
     */
    public static void checkValues(int param1, long param2, float param3,
            double param4, String param5) {
        Asserts.assertEQ(param1, 1);
        Asserts.assertEQ(param2, 2L);
        Asserts.assertEQ(param3, 3.0f);
        Asserts.assertEQ(param4, 4.0d);
        Asserts.assertEQ(param5, "5");
    }
}
