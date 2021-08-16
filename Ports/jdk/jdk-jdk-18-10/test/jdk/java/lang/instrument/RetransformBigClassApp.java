/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;

public class RetransformBigClassApp {
    /**
     * Memory leak is assumed, if application consumes more than specified amount of memory during its execution.
     * The number is given in KB.
     */
    private static final long MEM_LEAK_THRESHOLD = 32 * 1024; // 32MB

    public static void main(String[] args) throws Exception {
        System.out.println("Creating instance of " +
            RetransformBigClassAgent.clz);
        RetransformBigClassAgent.clz.newInstance();

        // Do a short warmup before creating the NMT baseline
        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException ie) {
        }

        NMTHelper.baseline();

        int count = 0;
        while (!RetransformBigClassAgent.doneRetransforming) {
            System.out.println("App loop count: " + ++count);
            try {
                Thread.sleep(10 * 1000);
            } catch (InterruptedException ie) {
            }
        }
        System.out.println("App looped  " + count + " times.");

        long committedDiff = NMTHelper.committedDiff();
        if (committedDiff > MEM_LEAK_THRESHOLD) {
            throw new Exception("FAIL: Committed memory usage increased by " + committedDiff + "KB " +
                               "(greater than " + MEM_LEAK_THRESHOLD + "KB)");
        }
        System.err.println("PASS: Committed memory usage increased by " + committedDiff + "KB " +
                           "(not greater than " + MEM_LEAK_THRESHOLD + "KB)");
    }
}
