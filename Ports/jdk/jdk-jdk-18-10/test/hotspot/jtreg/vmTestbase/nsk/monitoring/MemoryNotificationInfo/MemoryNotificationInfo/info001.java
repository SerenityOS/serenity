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

package nsk.monitoring.MemoryNotificationInfo.MemoryNotificationInfo;

import java.lang.management.*;
import java.io.*;
import nsk.share.*;

public class info001 {
    private static boolean testFailed = false;

    public static void main(String[] argv) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String[] argv, PrintStream out) {
        MemoryUsage mu = new MemoryUsage(1, 2, 3, 4);

        test("name", mu, 1, out, "if correct parameters are passed.");
        test("", mu, 1, out, "if empty pool name is passed.");
        test(null, mu, 1, out, "if null pool name is passed.");
        test("name", null, 1, out, "if null MemoryUsage is passed.");
        test("name", mu, -2, out, "if negative count is passed.");
        test("name", mu, 0, out, "if zero count is passed.");
        test("name", mu, Long.MAX_VALUE, out, "if Long.MAX_VALUE, count is "
                                            + "passed.");
        test("name", mu, Long.MIN_VALUE, out, "if Long.MIN_VALUE, count is "
                                            + "passed.");

        if (testFailed)
            out.println("TEST FAILED");
        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }

    private static void test(String name, MemoryUsage usage, long count,
                                              PrintStream out, String message) {
        MemoryNotificationInfo info;

        try {
            info = new MemoryNotificationInfo(name, usage, count);
            if ((name == null) || (usage == null)) {
                out.println("NullPointerException was not thrown, " + message);
                testFailed = true;
            }
        } catch (NullPointerException e) {
            if ((name != null) && (usage != null)) {
                out.println("Unexpected NullPointerException, " + message);
                e.printStackTrace(out);
                testFailed = true;
            }
        } catch (Exception e) {
            out.println("Exception, " + message);
            e.printStackTrace(out);
            testFailed = true;
        }
    } // test()

}
