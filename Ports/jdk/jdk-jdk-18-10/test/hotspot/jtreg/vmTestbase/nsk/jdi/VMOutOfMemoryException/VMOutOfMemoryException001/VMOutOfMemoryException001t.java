/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.VMOutOfMemoryException.VMOutOfMemoryException001;

import nsk.share.Consts;
import nsk.share.jdi.AbstractJDIDebuggee;

/*
 * This test verifies that a VMOutOfMemoryException is thrown when the debugge runs out of memory.
 * The debugger will allocate memory in this debugge until it gets a VMOutOfMemoryException.
 * This means that we are likely to get an OutOfMemoryError during the test.
 * Any OutOfMemoryErrors is just ignored, since they are expected.
 */
public class VMOutOfMemoryException001t extends AbstractJDIDebuggee {

    public static void main(String args[]) {
        new VMOutOfMemoryException001t().doTest(args);
    }

    // Just call normal doTest() function, but hide any OutOfMemoryErrors.
    public void doTest() {
        boolean isOutOfMemory = false;

        try {
            super.doTest();
        } catch (OutOfMemoryError e) {
            // Don't log anything. We are out of memory.
            // A println is likely to genereate a new OutOfMemoryError
            isOutOfMemory = true;
        }

        // Normally the super class handles the return value.
        // If we got here after an OutOfMemoryError, we consider the test passed.
        if (isOutOfMemory && callExit) {
            System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_PASSED);
        }
    }

}
