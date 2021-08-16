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

package nsk.monitoring.ThreadInfo.isInNative;

import java.lang.management.*;
import java.io.*;
import nsk.share.*;

public class isinnative001 {
    public static void main(String[] argv) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String[] argv, PrintStream out) {
        ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        Thread thread = Thread.currentThread();
        long id = thread.getId();
        ThreadInfo info = mbean.getThreadInfo(id, Integer.MAX_VALUE);
        boolean isInNative = info.isInNative();

        if (isInNative) {
            out.println("TEST FAILED");
            out.println("ThreadInfo.isInNative() returned true, in pure java "
                      + "thread.");
            return Consts.TEST_FAILED;
        }
        return Consts.TEST_PASSED;
    }
}
