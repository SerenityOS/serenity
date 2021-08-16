/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.MemoryUsage.MemoryUsage;

import java.lang.management.*;
import java.io.*;
import nsk.share.*;

public class memoryusage001 {
    private static boolean testFailed = false;

    public static void main(String[] argv) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String[] argv, PrintStream out) {

        // Check all conditions for IllegalArgumentException

        // 1.1. init is negative, but not -1
        try {
            new MemoryUsage(-2, 2, 3, 4);
            out.println("Failure 1.1.");
            out.println("new MemoryUsage(-2, 2, 3, 4) does not throw "
                      + "IllegalArgumentException. init is negative, but not "
                      + "-1.");
            testFailed = true;
        } catch (IllegalArgumentException e) {
        }

        // 1.2. max is negative, but not -1
        try {
            new MemoryUsage(1, 2, 3, -4);
            out.println("Failure 1.2.");
            out.println("new MemoryUsage(1, 2, 3, -4) does not throw "
                      + "IllegalArgumentException. max is negative, but not "
                      + "-1.");
            testFailed = true;
        } catch (IllegalArgumentException e) {
        }

        // 1.3. used is negative
        try {
            new MemoryUsage(1, -1, 3, 4);
            out.println("Failure 1.3.");
            out.println("new MemoryUsage(1, -1, 3, 4) does not throw "
                      + "IllegalArgumentException. used is negative.");
            testFailed = true;
        } catch (IllegalArgumentException e) {
        }

        // 1.4. committed is negative
        try {
            new MemoryUsage(1, 2, -1, 4);
            out.println("Failure 1.4.");
            out.println("new MemoryUsage(1, 2, -1, 4) does not throw "
                      + "IllegalArgumentException. committed is negative.");
            testFailed = true;
        } catch (IllegalArgumentException e) {
        }

        // 1.5. used is greater than committed
        try {
            new MemoryUsage(1, 2, 1, 4);
            out.println("Failure 1.5.");
            out.println("new MemoryUsage(1, 2, 1, 4) does not throw "
                      + "IllegalArgumentException. used is greater than "
                      + "committed.");
            testFailed = true;
        } catch (IllegalArgumentException e) {
        }

        // 1.6. used is greater than max, while max is not -1
        try {
            new MemoryUsage(1, 5, 6, 4);
            out.println("Failure 1.6.");
            out.println("new MemoryUsage(1, 5, 6, 4) does not throw "
                      + "IllegalArgumentException. used is greater than max, "
                      + "while max is not -1.");
            testFailed = true;
        } catch (IllegalArgumentException e) {
        }

        // Check different correct sets of values for MemoryUsage

        // 2.1. init is -1
        try {
            new MemoryUsage(-1, 2, 3, 4);
        } catch (IllegalArgumentException e) {
            out.println("Failure 2.1.");
            out.println("new MemoryUsage(-1, 2, 3, 4) throws " + e + ". init is"
                      + " -1.");
            testFailed = true;
        }

        // 2.2. max is -1
        try {
            new MemoryUsage(1, 2, 3, -1);
        } catch (IllegalArgumentException e) {
            out.println("Failure 2.2.");
            out.println("new MemoryUsage(1, 2, 3, -1) throws " + e + ". max is "
                      + "-1.");
            testFailed = true;
        }

        // 2.3. used is equal to max
        try {
            new MemoryUsage(1, 2, 3, 2);

            out.println("Failure 2.3.");
            out.println("new MemoryUsage(1, 2, 3, 2) does not throw IllegalArgumentException. "
                      + "used is equal to max.");
            testFailed = true;
        } catch (IllegalArgumentException e) {
            // expected exception
        }

        // 2.4. used is less than max
        try {
            new MemoryUsage(1, 2, 3, 4);
        } catch (IllegalArgumentException e) {
            out.println("Failure 2.4.");
            out.println("new MemoryUsage(1, 2, 3, 4) throws " + e + ". used is "
                      + "less than max.");
            testFailed = true;
        }

        // 2.5. committed is less than init
        try {
            new MemoryUsage(4, 2, 3, -1);
        } catch (IllegalArgumentException e) {
            out.println("Failure 2.5.");
            out.println("new MemoryUsage(4, 2, 3, -1) throws " + e + ". "
                      + "committed is less than init.");
            testFailed = true;
        }

        // 2.6. max is less than committed
        try {
            new MemoryUsage(1, 2, 4, 3);
            out.println("Failure 2.6.");
            out.println("new MemoryUsage(1, 2, 4, 3) does not throw IllegalArgumentException. "
                      + "max is less than committed.");
            testFailed = true;
        } catch (IllegalArgumentException e) {
            // expected exception
        }

        if (testFailed)
            out.println("TEST FAILED");
        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }
}
