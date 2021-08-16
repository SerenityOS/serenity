/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6475157
 * @summary Tests deadlock in simultaneous connection and connector-server close
 * @author Eamonn McManus
 */

/* This test is somewhat dependent on implementation details.  If it suddenly
 * starts failing after a rewrite of the RMIConnectorServer code, you should
 * consider whether it is still relevant.
 */

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Exchanger;
import javax.management.MBeanServer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnection;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIJRMPServerImpl;

public class ConnectorStopDeadlockTest {
    private static String failure;
    private static RMIConnectorServer connectorServer;

    public static void main(String[] args) throws Exception {
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        RMIJRMPServerImplSub impl = new RMIJRMPServerImplSub();

        System.out.println("Creating connectorServer");
        connectorServer = new RMIConnectorServer(url, null, impl, mbs);
        System.out.println("Starting connectorServer");
        connectorServer.start();
        System.out.println("Making client");
        RMIConnection cc = impl.newClient(null);
        System.out.println("Closing client");
        cc.close();
        if (connectorServer.isActive()) {
            System.out.println("Stopping connectorServer");
            connectorServer.stop();
        }
        if (failure == null)
            System.out.println("TEST PASSED, no deadlock");
        else
            System.out.println("TEST FAILED");
    }

    static void fail(Throwable e) {
        System.out.println("FAILED WITH EXCEPTION: " + e);
        e.printStackTrace(System.out);
        failure = e.toString();
    }

    static void fail(String s) {
        System.out.println("FAILED: " + s);
        failure = s;
    }

//    static MonitorInfo[] threadLocks(Thread t) {
//        ThreadMXBean tm = ManagementFactory.getThreadMXBean();
//        ThreadInfo[] tis = tm.getThreadInfo(new long[] {t.getId()}, true, true);
//        if (tis[0] == null)
//            return null;
//        else
//            return tis[0].getLockedMonitors();
//    }
//
//    static void showLocks(Thread t) {
//        System.out.println("Locks for " + t.getName() + ":");
//        MonitorInfo[] mis = threadLocks(t);
//        if (mis == null)
//            System.out.println("  (no longer exists)");
//        else if (mis.length == 0)
//            System.out.println("  (none)");
//        else {
//            for (MonitorInfo mi : mis)
//                System.out.println("  " + mi);
//        }
//    }

    // Wait until thread t blocks waiting for a lock held by the calling thread,
    // or until it exits.
    static void waitForBlock(Thread t) {
        Thread currentThread = Thread.currentThread();
        System.out.println("waiting for thread " + t.getName() + " to block " +
                "on a lock held by thread " + currentThread.getName());
        ThreadMXBean tm = ManagementFactory.getThreadMXBean();
        while (true) {
            ThreadInfo ti = tm.getThreadInfo(t.getId());
            if (ti == null) {
                System.out.println("  thread has exited");
                return;
            }
            if (ti.getLockOwnerId() == currentThread.getId()) {
                System.out.println("  thread now blocked");
                return;
            }
            Thread.yield();
        }
    }

    public static class RMIJRMPServerImplSub extends RMIJRMPServerImpl {
        RMIJRMPServerImplSub() throws IOException {
            super(0, null, null, null);
        }

        public RMIConnection makeClient() throws IOException {
            return super.makeClient("connection id", null);
        }

        @Override
        protected void clientClosed(RMIConnection conn) throws IOException {
            System.out.println("clientClosed, will call connectorServer.stop");
            final Exchanger<Void> x = new Exchanger<Void>();
            Thread t = new Thread() {
                public void run() {
                    try {
                        connectorServer.stop();
                    } catch (Exception e) {
                        fail(e);
                    }
                }
            };
            t.setName("connectorServer.stop");
            t.start();
            waitForBlock(t);
            /* If this thread is synchronized on RMIServerImpl, then
             * the thread that does connectorServer.stop will acquire
             * the clientList lock and then block waiting for the RMIServerImpl
             * lock.  Our call to super.clientClosed will then deadlock because
             * it needs to acquire the clientList lock.
             */
            System.out.println("calling super.clientClosed");
            System.out.flush();
            super.clientClosed(conn);
        }
    }
}
