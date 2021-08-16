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

package nsk.monitoring.GarbageCollectorMXBean.getCollectionCount;

import java.lang.management.*;
import java.io.*;
import nsk.share.*;
import nsk.monitoring.share.*;

public class getcollectioncount001 {

    private static boolean testFailed = false;

    public static void main(String[] args) {

        System.exit(Consts.JCK_STATUS_BASE + run(args, System.out));
    }

    private static Log log;

    static int run(String[] args, PrintStream out) {

        ArgumentHandler argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);

        // Test case 1. check that
        // getCollectionCount() does not throw unexpected exceptions

        System.gc();
        System.gc();
        System.gc();

        GarbageCollectorMonitor gcMonitor =
        Monitor.getGarbageCollectorMonitor(log, argumentHandler);

        Object[] pool = gcMonitor.getGarbageCollectorMXBeans();
        for (int i=0; i<pool.length; i++) {

            String beanName = "";
            long collectionCount = gcMonitor.getCollectionCount(pool[i]);

            if (pool[i] instanceof javax.management.ObjectName) {
                beanName = ((javax.management.ObjectName)pool[i]).toString();
            } else {
                beanName = ((java.lang.management.GarbageCollectorMXBean)
                    pool[i]).getName();
            }
            log.display(beanName+": getCollectionCount() = "+collectionCount);

            if (collectionCount < -1) {
                // value can be non-negative or -1 if if the collection count
                // is undefined for this collector.
                log.complain("FAILURE 1.");
                log.complain("getCollectionCount() returns unexpected value: " +
                    collectionCount);
                testFailed = true;
            }
        }

        if (testFailed)
            log.complain("TEST FAILED");

        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }

}
