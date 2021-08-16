/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * Abstract class for testing of used compilation levels correctness.
 *
 * @author igor.ignatyev@oracle.com
 */

package compiler.tiered;

import compiler.whitebox.CompilerWhiteBoxTest;

public abstract class CompLevelsTest extends CompilerWhiteBoxTest {
    protected CompLevelsTest(TestCase testCase) {
        super(testCase);
        // to prevent inlining of #method
        WHITE_BOX.testSetDontInlineMethod(method, true);
    }

    /**
     * Checks that level is available.
     * @param compLevel level to check
     */
    protected void testAvailableLevel(int compLevel, int bci) {
        if (IS_VERBOSE) {
            System.out.printf("testAvailableLevel(level = %d, bci = %d)%n",
                    compLevel, bci);
        }
        WHITE_BOX.enqueueMethodForCompilation(method, compLevel, bci);
        checkCompiled();
        checkLevel(compLevel, getCompLevel());
        deoptimize();
    }

    /**
     * Checks that level is unavailable.
     * @param compLevel level to check
     */
    protected void testUnavailableLevel(int compLevel, int bci) {
        if (IS_VERBOSE) {
            System.out.printf("testUnavailableLevel(level = %d, bci = %d)%n",
                    compLevel, bci);
        }
        WHITE_BOX.enqueueMethodForCompilation(method, compLevel, bci);
        checkNotCompiled();
    }

    /**
     * Checks validity of compilation level.
     * @param expected expected level
     * @param actual actually level
     */
    protected void checkLevel(int expected, int actual) {
        if (expected != actual) {
            throw new RuntimeException("expected[" + expected + "] != actual["
                    + actual + "]");
        }
    }
}
