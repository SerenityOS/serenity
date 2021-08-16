/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8229212
 * @summary Verify that monitor operations by a non-owner thread throw
 *          IllegalMonitorStateException.
 * @run main NonOwnerOps
 */

public class NonOwnerOps {
    public static void main(String[] args) {
        int error_count = 0;
        Object obj;

        obj = new Object();
        try {
            obj.wait();
            System.err.println("ERROR: wait() by non-owner thread did not " +
                               "throw IllegalMonitorStateException.");
            error_count++;
        } catch (InterruptedException ie) {
            System.err.println("ERROR: wait() by non-owner thread threw " +
                               "InterruptedException which is not expected.");
            error_count++;
        } catch (IllegalMonitorStateException imse) {
            System.out.println("wait() by non-owner thread threw the " +
                               "expected IllegalMonitorStateException:");
            System.out.println("    " + imse);
        }

        obj = new Object();
        try {
            obj.notify();
            System.err.println("ERROR: notify() by non-owner thread did not " +
                               "throw IllegalMonitorStateException.");
            error_count++;
        } catch (IllegalMonitorStateException imse) {
            System.out.println("notify() by non-owner thread threw the " +
                               "expected IllegalMonitorStateException:");
            System.out.println("    " + imse);
        }

        obj = new Object();
        try {
            obj.notifyAll();
            System.err.println("ERROR: notifyAll() by non-owner thread did " +
                               "not throw IllegalMonitorStateException.");
            error_count++;
        } catch (IllegalMonitorStateException imse) {
            System.out.println("notifyAll() by non-owner thread threw the " +
                               "expected IllegalMonitorStateException:");
            System.out.println("    " + imse);
        }

        if (error_count != 0) {
            throw new RuntimeException("Test failed with " + error_count +
                                       " errors.");
        }
        System.out.println("Test PASSED.");
    }
}
