/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package compiler.c2.cr6589834;

public class InlinedArrayCloneTestCase implements Runnable {
    private Test_ia32 executionController;

    public InlinedArrayCloneTestCase(Test_ia32 executionController) {
        this.executionController = executionController;
    }

    /*
     * Please leave following two methods (invokeArrayClone and verifyArguments)
     * static.
     *
     * It does not really matter if these methods are static or instance,
     * original issue could be reproduced in both cases, but if these methods
     * are static then it is much easier to understand that reproduced issue
     * is actually interpreter's stack corruption.
     *
     * If these methods are non-static, then interpreter's stack will contain
     * invalid 'this' pointer required for instance's method call and
     * verifyArguments' call may throw NullPointerException. There was another
     * issue w/ NPE after deoptimization addressed by JDK-6833129, so NPE looks
     * a little bit confusing.
     *
     * If these methods are static then after deptimization we'll get incorrect
     * arguments values in verifyArguments.
     * Something like "2, -1289936896, 3, 4" instead of "1, 2, 3, 4".
     * This information tells much more about actual issue comparing to NPE,
     * so it's preferable to leave these methods static.
     */
    private static int verifyArguments(int i1, int i2, LoadedClass[] arr,
            int i3, int i4) {
        if (!(i1==1 && i2==2 && i3==3 && i4==4)) {
            throw new RuntimeException(String.format(
                    "Arguments have unexpected values: %d, %d, %d, %d",
                    i1, i2, i3, i4));
        }
        return arr.length;
    }

    private static int invokeArrayClone(LoadedClass[] a) {
        return InlinedArrayCloneTestCase.verifyArguments(1, 2, a.clone(), 3, 4);
    }

    @Override
    public void run() {
        LoadedClass[] array = executionController.getArray();
        int length;

        while (executionController.continueExecution()) {
            try {
                length = InlinedArrayCloneTestCase.invokeArrayClone(array);
            } catch (Throwable e) {
                e.printStackTrace();
                executionController.setTestFailed();
                return;
            }
            if (length != array.length) {
                System.out.println(String.format("f(array) returned %d "
                        + "instead of %d.", length, array.length));
                executionController.setTestFailed();
            }
        }
    }
}
