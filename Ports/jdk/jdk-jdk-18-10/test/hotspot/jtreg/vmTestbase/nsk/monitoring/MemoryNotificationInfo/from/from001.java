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

package nsk.monitoring.MemoryNotificationInfo.from;

import java.lang.management.*;
import javax.management.*;
import javax.management.openmbean.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.SynchronousQueue;
import nsk.share.*;
import nsk.share.gc.Algorithms;
import nsk.share.gc.Memory;
import nsk.share.gc.gp.GarbageUtils;
import nsk.monitoring.share.*;
import nsk.share.test.Stresser;

public class from001 {

    private static boolean testFailed = false;

    public static void main(String[] args) {

        ArgumentHandler argHandler = new ArgumentHandler(args);
        Log log = new Log(System.out, argHandler);

        log.display("MemoryNotificationInfo/from/from001/from001.java test started.");

        MemoryMonitor monitor = Monitor.getMemoryMonitor(log, argHandler);
        MBeanServer mbs = Monitor.getMBeanServer();

        // 1. Check null CompositeData - null must be returned
        MemoryNotificationInfo result = MemoryNotificationInfo.from(null);

        if (result != null) {
            log.complain("FAILURE 1.");
            log.complain("MemoryNotificationInfo.from(null) returned " + result
                      + ", expected: null.");
            testFailed = true;
        }

        log.display("null CompositeData check passed.");

        // 2. Check CompositeData that doest not represnt
        // MemoryNotificationInfo - IllegalArgumentException must be thrown

        ObjectName mbeanObjectName = null;
        CompositeData cdata = null;
        try {
            mbeanObjectName = new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME);
            cdata = (CompositeData )mbs.getAttribute(mbeanObjectName,
                                                                "HeapMemoryUsage");
        } catch (Exception e) {
            log.complain("Unexpected exception " + e);
            e.printStackTrace(log.getOutStream());
            testFailed = true;
        }

        try {
            result = MemoryNotificationInfo.from(cdata);
            log.complain("FAILURE 2.");
            log.complain("MemoryNotificationInfo.from(CompositeData) returned "
                      + result + ", expected: IllegalArgumentException.");
            testFailed = true;
        } catch (IllegalArgumentException e) {

            // Expected: CompositeData doest not represnt MemoryNotificationInfo
        }

        log.display("check for CompositeData doest not represnt MemoryNotificationInfo passed.");

        // 3. Check correct CompositeData
        Object poolObject = null;
        try {
            mbeanObjectName = new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME);
            mbs.addNotificationListener(mbeanObjectName, new from001Listener(),
                                                              null, null);
            List<?> pools = monitor.getMemoryPoolMBeans();
            if (pools.isEmpty()) {
               log.complain("No Memory Pool Beans found. Test case will hang/fail.");
               testFailed = true;
            }

            for (int i = 0; i < pools.size(); i++) {
                Object pool = pools.get(i);
                if (monitor.isUsageThresholdSupported(pool)) {
                    if (monitor.getType(pool).equals(MemoryType.HEAP)) {
                        poolObject = pool;
                        monitor.setUsageThreshold(pool, 1);
                        log.display("Usage threshold set for pool :" + poolObject);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.complain("Unexpected exception " + e);
            e.printStackTrace(log.getOutStream());
            testFailed = true;
        }

        if (testFailed) {
            throw new TestFailure("TEST FAILED. See log.");
        }

        // eat memory just to emmit notification
        Stresser stresser = new Stresser(args) {

            @Override
            public boolean continueExecution() {
                return from001Listener.data.get() == null
                        && super.continueExecution();
            }
        };
        stresser.start(0);// we use timeout, not iterations
        GarbageUtils.eatMemory(stresser);

        boolean messageNotRecieved = true;
        while(messageNotRecieved) {
            try {
                from001Listener.queue.take();
                messageNotRecieved = false;
            } catch (InterruptedException e) {
                messageNotRecieved = true;
            }
        }

        result = MemoryNotificationInfo.from(from001Listener.data.get());
        try {
           ObjectName poolObjectName = new ObjectName(monitor.getName(poolObject));
           ObjectName resultObjectName = new ObjectName(
                     ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE +
                     ",name=" + result.getPoolName());

           log.display("poolObjectName : " + poolObjectName +
                              " resultObjectName : " + resultObjectName);

           if (!poolObjectName.equals(resultObjectName)) {
              log.complain("FAILURE 3.");
              log.complain("Wrong pool name : " + resultObjectName +
                           ", expected : " + poolObjectName);
              testFailed = true;
           }

        } catch (Exception e) {
            log.complain("Unexpected exception " + e);
            e.printStackTrace(log.getOutStream());
            testFailed = true;
        }

        if (testFailed) {
            throw new TestFailure("TEST FAILED. See log.");
        }

        log.display("Test passed.");
    }
}


class from001Listener implements NotificationListener {

    static AtomicReference<CompositeData> data = new AtomicReference<CompositeData>();
    static SynchronousQueue<Object> queue = new SynchronousQueue<Object>();

    public void handleNotification(Notification notification, Object handback) {
        if (data.get() != null)
            return;
        data.set((CompositeData) notification.getUserData());

        boolean messageNotSent = true;
        while(messageNotSent){
            try {
                queue.put(new Object());
                messageNotSent = false;
            } catch(InterruptedException e) {
                messageNotSent = true;
            }
        }
    }

}
