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

package nsk.monitoring.MemoryNotificationInfo.getCount;

import java.lang.management.*;
import java.io.*;
import nsk.share.*;

public class getcount001 {
    private static boolean testFailed = false;

    public static void main(String[] argv) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String[] argv, PrintStream out) {
        MemoryUsage mu = new MemoryUsage(1, 2, 3, 4);

        // Check positive count
        MemoryNotificationInfo mn
            = new MemoryNotificationInfo("poolName", mu, 1);
        long count = mn.getCount();
        if (count != 1) {
            out.println("FAILURE 1.");
            out.println("Wrong count: " + count + ", expected: 1");
            testFailed = true;
        }

        // Check negative count
        mn = new MemoryNotificationInfo("poolName", mu, -1);
        count = mn.getCount();
        if (count != -1) {
            out.println("FAILURE 2.");
            out.println("Wrong count: " + count + ", expected: -1");
            testFailed = true;
        }

        // Check zero count
        mn = new MemoryNotificationInfo("poolName", mu, -2);
        mn = new MemoryNotificationInfo("poolName", mu, 0);
        count = mn.getCount();
        if (count != 0) {
            out.println("FAILURE 3.");
            out.println("Wrong count: " + count + ", expected: 0");
            testFailed = true;
        }

        if (testFailed)
            out.println("TEST FAILED");
        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }
}
