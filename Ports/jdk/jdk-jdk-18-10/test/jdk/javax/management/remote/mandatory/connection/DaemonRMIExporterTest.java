/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6760712
 * @summary test the connector server option that causes it not to prevent the
 * VM from exiting
 * @author Shanliang JIANG, Eamonn McManus
 *
 * @run main/othervm DaemonRMIExporterTest
 */
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;

// Test the connector server option that causes it not to prevent the VM
// from exiting.  It's tricky to test exactly that, though possible.  If
// we're being run from within jtreg, then jtreg has threads of its own
// that will prevent the VM from exiting.  What's more it will kill all
// threads that the test created as soon as the main method returns,
// including the ones that would prevent the VM from exiting without the
// special option.
// Here we check that the test code does not create
// any permanent non-daemon threads, by recording the initial set of
// non-daemon threads (including at least one from jtreg), doing our stuff,
// then waiting for there to be no non-daemon threads that were not in
// the initial set.
public class DaemonRMIExporterTest {
    public static void main(String[] args) throws Exception {
        Set<Thread> initialNonDaemonThreads = getNonDaemonThreads();

        JMXServiceURL addr = new JMXServiceURL("rmi", null, 0);
        System.out.println("DaemonRMIExporterTest: Creating a RMIConnectorServer on " + addr);
        Map<String, ?> env =
            Collections.singletonMap("jmx.remote.x.daemon", "true");
        JMXConnectorServer server =
                JMXConnectorServerFactory.newJMXConnectorServer(addr,
                env,
                MBeanServerFactory.createMBeanServer());
        server.start();
        System.out.println("DaemonRMIExporterTest: Started the server on " + server.getAddress());

        System.out.println("DaemonRMIExporterTest: Connecting a client to the server ...");
        final JMXConnector conn = JMXConnectorFactory.connect(server.getAddress());
        conn.getMBeanServerConnection().getDefaultDomain();

        System.out.println("DaemonRMIExporterTest: Closing the client ...");
        conn.close();

        System.out.println("DaemonRMIExporterTest No more user code to execute, the VM should " +
                "exit normally, otherwise will be blocked forever if the bug is not fixed.");

        long deadline = System.currentTimeMillis() + 10000;
        ok: {
            while (System.currentTimeMillis() < deadline) {
                Set<Thread> nonDaemonThreads = getNonDaemonThreads();
                nonDaemonThreads.removeAll(initialNonDaemonThreads);
                if (nonDaemonThreads.isEmpty())
                    break ok;
                System.out.println("Non-daemon threads: " + nonDaemonThreads);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
            }
            throw new Exception("TEST FAILED: non-daemon threads remain");
        }

        System.out.println("TEST PASSED");
    }

    private static Set<Thread> getNonDaemonThreads() {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        while (tg.getParent() != null)
            tg = tg.getParent();
        Thread[] threads = null;
        for (int size = 10; size < 10000; size *= 2) {
            threads = new Thread[size];
            int n = tg.enumerate(threads, true);
            if (n < size) {
                threads = Arrays.copyOf(threads, n);
                break;
            }
        }
        Set<Thread> ndThreads = new HashSet<Thread>();
        for (Thread t : threads) {
            if (!t.isDaemon())
                ndThreads.add(t);
        }
        return ndThreads;
    }
}
