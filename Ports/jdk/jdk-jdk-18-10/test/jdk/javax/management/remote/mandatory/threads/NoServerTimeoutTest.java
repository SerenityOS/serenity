/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6192124
 * @summary Tests that you can turn off the server connection timeout thread
 * @author Eamonn McManus
 *
 * @run clean NoServerTimeoutTest
 * @run build NoServerTimeoutTest
 * @run main NoServerTimeoutTest
 */

import java.lang.management.*;
import java.net.MalformedURLException;
import java.util.*;
import javax.management.*;
import javax.management.remote.*;

public class NoServerTimeoutTest {
    public static void main(String[] args) throws Exception {
        boolean ok = true;

        for (String proto : new String[] {"rmi", "iiop", "jmxmp"}) {
            JMXServiceURL url = new JMXServiceURL(proto, null, 0);
            try {
                MBeanServer mbs = MBeanServerFactory.newMBeanServer();
                // Create server just to see if the protocol is supported
                JMXConnectorServerFactory.newJMXConnectorServer(url,
                                                                null,
                                                                mbs);
            } catch (MalformedURLException e) {
                System.out.println();
                System.out.println("Ignoring protocol: " + proto);
                continue;
            }
            try {
                ok &= test(url);
            } catch (Exception e) {
                System.out.println("TEST FAILED WITH EXCEPTION:");
                e.printStackTrace();
                ok = false;
            }
        }

        System.out.println();
        if (ok)
            System.out.println("Test passed");
        else
            throw new Exception("Test failed");
    }

    private static enum Test {
        NO_ENV("No Map for connector server"),
        EMPTY_ENV("Empty Map for connector server"),
        PLAIN_TIMEOUT("Map with two-minute timeout as int"),
        PLAIN_STRING_TIMEOUT("Map with two-minute timeout as string"),
        INFINITE_TIMEOUT("Map with Long.MAX_VALUE timeout as long"),
        INFINITE_STRING_TIMEOUT("Map with Long.MAX_VALUE timeout as string");

        Test(String description) {
            this.description = description;
        }

        public String toString() {
            return description;
        }

        private final String description;
    }
    // define which tests should see a timeout thread
    private static final Set<Test> expectThread =
        EnumSet.copyOf(Arrays.asList(Test.NO_ENV,
                                     Test.EMPTY_ENV,
                                     Test.PLAIN_TIMEOUT,
                                     Test.PLAIN_STRING_TIMEOUT));

    private static boolean test(JMXServiceURL url) throws Exception {
        System.out.println();
        System.out.println("Test: " + url);

        boolean ok = true;

        for (Test test : Test.values())
            ok &= test(url, test);

        return ok;
    }

    private static final String TIMEOUT =
        "jmx.remote.x.server.connection.timeout";

    private static boolean test(JMXServiceURL url, Test test)
            throws Exception {

        System.out.println("* " + test);

        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        Map<String, Object> env = new HashMap<String, Object>();
        switch (test) {
        case NO_ENV: env = null; break;
        case EMPTY_ENV: break;
        case PLAIN_TIMEOUT: env.put(TIMEOUT, 120 * 1000L); break;
        case PLAIN_STRING_TIMEOUT: env.put(TIMEOUT, (120 * 1000L) + ""); break;
        case INFINITE_TIMEOUT: env.put(TIMEOUT, Long.MAX_VALUE); break;
        case INFINITE_STRING_TIMEOUT: env.put(TIMEOUT, "" + Long.MAX_VALUE);
            break;
        default: throw new AssertionError();
        }

        // In case there's a timeout thread left over from a previous run
        for (int i = 0; i < 10 && countTimeoutThreads() > 0; i++)
            Thread.sleep(500);
        if (countTimeoutThreads() > 0) {
            System.out.println("TIMEOUT THREAD(S) WOULD NOT GO AWAY");
            return false;
        }

        JMXConnectorServer cs =
            JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
        cs.start();
        JMXServiceURL addr = cs.getAddress();
        JMXConnector cc = JMXConnectorFactory.connect(addr);
        MBeanServerConnection mbsc = cc.getMBeanServerConnection();
        mbsc.getDefaultDomain();
        int expectTimeoutThreads = expectThread.contains(test) ? 1 : 0;
        int timeoutThreads = countTimeoutThreads();
        boolean ok = (expectTimeoutThreads == timeoutThreads);
        if (!ok) {
            System.out.println("TEST FAILS: Expected timeout threads: " +
                               expectTimeoutThreads +
                               "; actual timeout threads: " + timeoutThreads);
            ok = false;
        }
        cc.close();
        cs.stop();
        return ok;
    }

    private static int countTimeoutThreads() {
        ThreadMXBean mb = ManagementFactory.getThreadMXBean();
        int count = 0;
        long[] ids = mb.getAllThreadIds();
        for (ThreadInfo ti : mb.getThreadInfo(ids)) {
            if (ti != null &&
                ti.getThreadName().startsWith("JMX server connection timeout"))
                count++;
        }
        return count;
    }
}
