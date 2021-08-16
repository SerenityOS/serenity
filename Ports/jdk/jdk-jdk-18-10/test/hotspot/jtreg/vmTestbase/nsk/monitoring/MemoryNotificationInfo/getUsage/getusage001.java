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

package nsk.monitoring.MemoryNotificationInfo.getUsage;

import java.lang.management.*;
import java.io.*;
import nsk.share.*;

public class getusage001 {
    private static boolean testFailed = false;

    public static void main(String[] argv) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String[] argv, PrintStream out) {

        // Check not-null object
        MemoryUsage expectedUsage = new MemoryUsage(1, 2, 3, 4);

        MemoryNotificationInfo mn
            = new MemoryNotificationInfo("poolName", expectedUsage, 1);
        MemoryUsage realUsage = mn.getUsage();
        long committed = realUsage.getCommitted();
        long init = realUsage.getInit();
        long max = realUsage.getMax();
        long used = realUsage.getUsed();
        String s = realUsage.toString();

        if (committed != 3) {
            out.println("FAILURE 1.");
            out.println("Wrong committed value: " + committed + ", expected: "
                      + "3");
            testFailed = true;
        }

        if (init != 1) {
            out.println("FAILURE 2.");
            out.println("Wrong init value: " + init + ", expected: 1");
            testFailed = true;
        }

        if (max != 4) {
            out.println("FAILURE 3.");
            out.println("Wrong max value: " + max + ", expected: 4");
            testFailed = true;
        }

        if (used != 2) {
            out.println("FAILURE 4.");
            out.println("Wrong used value: " + used + ", expected: 2");
            testFailed = true;
        }

        if (!expectedUsage.toString().equals(s)) {
            out.println("FAILURE 5.");
            out.println("Wrong toString() value: \"" + s + "\", expected: \""
                      + expectedUsage.toString() + "\"");
            testFailed = true;
        }

        if (testFailed)
            out.println("TEST FAILED");
        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }
}
