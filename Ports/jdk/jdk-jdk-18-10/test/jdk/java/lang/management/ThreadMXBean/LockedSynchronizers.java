/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug     5086470 6358247
 * @summary Basic Test for ThreadInfo.getLockedSynchronizers()
 *          SynchronizerLockingThread is the class that creates threads
 *          and do the checking.
 *
 * @author  Mandy Chung
 *
 * @build Barrier
 * @build SynchronizerLockingThread
 * @build ThreadDump
 * @run main/othervm LockedSynchronizers
 */

import java.lang.management.*;
import java.util.*;

public class LockedSynchronizers {
    public static void main(String[] argv) throws Exception {
        ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        if (!mbean.isSynchronizerUsageSupported()) {
            System.out.println("Monitoring of synchronizer usage not supported");
            return;
        }

        // Start thread and print thread dump
        SynchronizerLockingThread.startLockingThreads();
        ThreadDump.threadDump();

        // Test dumpAllThreads with locked monitors and synchronizers
        ThreadInfo[] tinfos = mbean.dumpAllThreads(true, true);
        SynchronizerLockingThread.checkLocks(tinfos);

        // Test getThreadInfo with locked monitors and synchronizers
        long[] ids = SynchronizerLockingThread.getThreadIds();
        tinfos = mbean.getThreadInfo(ids, true, true);
        if (tinfos.length != ids.length) {
            throw new RuntimeException("Number of ThreadInfo objects = " +
                tinfos.length + " not matched. Expected: " + ids.length);
        }

        SynchronizerLockingThread.checkLocks(tinfos);

        System.out.println("Test passed");
    }
}
