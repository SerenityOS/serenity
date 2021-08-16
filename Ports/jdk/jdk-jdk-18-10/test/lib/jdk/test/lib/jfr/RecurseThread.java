/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.test.lib.jfr;

import jdk.test.lib.Asserts;

public class RecurseThread extends Thread {

    public int totalDepth;
    public long dummy = 0; // Just to make sure the optimizer does not remove the test code.
    private volatile boolean timeToQuit = false;
    private volatile boolean isInRunLoop = false;

    public RecurseThread(int totalDepth) {
        this.totalDepth = totalDepth;
    }

    @Override
    public void run() {
        // totalDepth includes functions run() and recurse() and runloop().
        // Remove 3 from totalDepth when recursing.
        final int minDepth = 3;
        Asserts.assertGreaterThanOrEqual(totalDepth, minDepth, "totalDepth too small");
        int recurseDepth = totalDepth - minDepth;

        // We want the last function before runloop() to be recurseA().
        boolean startWithRecurseA = (totalDepth % 2) != 0;
        dummy = startWithRecurseA ? recurseA(recurseDepth) : recurseB(recurseDepth);
    }

    public void quit() {
        timeToQuit = true;
    }

    public boolean isInRunLoop() {
        return isInRunLoop;
    }

    private long recurseA(int depth) {
        if (depth == 0) {
            return recurseEnd();
        } else {
            return recurseB(depth - 1);
        }
    }

    private long recurseB(int depth) {
        if (depth == 0) {
            return recurseEnd();
        } else {
            return recurseA(depth - 1);
        }
    }

    // Test expects this function to be at the top of the stack.
    // We should not call other functions from here.
    private long recurseEnd() {
        isInRunLoop = true;
        long[] dummyTable = new long[] { 0, 2, 4, 8, 16 };
        long dummyTotal = 0;
        while (!timeToQuit) {
            dummyTotal = 0;
            for (int i = 0; i < 5; ++i) {
                dummyTotal += dummyTable[i];
            }
        }
        return dummyTotal;
    }

}
