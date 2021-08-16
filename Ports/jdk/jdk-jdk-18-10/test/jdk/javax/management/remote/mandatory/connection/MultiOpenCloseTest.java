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
 * @test CloseUnconnectedTest.java 1.1 03/08/28
 * @bug 1234567
 * @summary Open, connect then close multi-connectors.
 * @author Shanliang JIANG
 *
 * @run clean MultiOpenCloseTest
 * @run build MultiOpenCloseTest
 * @run main MultiOpenCloseTest
 */

/*
 * Test that we can create a connection, call connect() on it,
 * then close it without anything surprising happening.
 */

import java.net.MalformedURLException;
import java.io.IOException;

import javax.management.*;
import javax.management.remote.*;

public class MultiOpenCloseTest {
    private static final String[] protocols = {"rmi", "iiop", "jmxmp"};
    private static final MBeanServer mbs = MBeanServerFactory.createMBeanServer();

    public static void main(String[] args) {
        System.out.println("Open, connect then close multi-connectors.");

        boolean ok = true;
        for (int i = 0; i < protocols.length; i++) {
            try {
                if (!test(protocols[i])) {
                    System.out.println("Test failed for " + protocols[i]);
                    ok = false;
                } else {
                    System.out.println("Test successed for " + protocols[i]);
                }
            } catch (Exception e) {
                System.out.println("Test failed for " + protocols[i]);
                e.printStackTrace(System.out);
                ok = false;
            }
        }

        if (ok) {
            System.out.println("Test passed");
            return;
        } else {
            System.out.println("TEST FAILED");
            System.exit(1);
        }
    }

    private static boolean test(String proto)
            throws Exception {
        System.out.println("Test for protocol " + proto);
        JMXServiceURL u = new JMXServiceURL(proto, null, 0);
        JMXConnectorServer s;
        try {
            s = JMXConnectorServerFactory.newJMXConnectorServer(u, null, mbs);
        } catch (MalformedURLException e) {
            System.out.println("Skipping unsupported URL " + u);
            return true;
        }
        s.start();
        JMXServiceURL a = s.getAddress();

        final int MAX_ITERS = 10;
        System.out.println("Looping for " + MAX_ITERS + "iterations...");

        for (int i=0; i<MAX_ITERS; i++) {
            JMXConnector c = JMXConnectorFactory.newJMXConnector(a, null);
            c.connect(null);
            c.close();
        }

        JMXConnector[] cs = new JMXConnector[MAX_ITERS];
        for (int i=0; i<MAX_ITERS; i++) {
            cs[i] = JMXConnectorFactory.newJMXConnector(a, null);
            cs[i].connect(null);
        }

        for (int i=0; i<MAX_ITERS; i++) {
            cs[i].close();
        }

        try {
            Thread.sleep(100);
        } catch (Exception ee) {
            // should not
        }

        // check state
        for (int i=0; i<MAX_ITERS; i++) {
            try {
                cs[i].getMBeanServerConnection(null);
                // no exception
                System.out.println("Did not get an IOException as expected, failed to close a client.");
                return false;
            } catch (IOException ioe) {
                // as expected
            }
        }

        s.stop();
        return true;
    }
}
