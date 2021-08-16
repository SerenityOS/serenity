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
 * @test CloseUnconnectedTest.java 1.1 03/07/28
 * @bug 4897052
 * @summary Tests that opening and immediately closing a connector works
 * @author Eamonn McManus
 *
 * @run clean CloseUnconnectedTest
 * @run build CloseUnconnectedTest
 * @run main CloseUnconnectedTest
 */

/*
 * Test that we can create a connection, but never call connect() on it,
 * then close it again without anything surprising happening.
 */

import java.net.MalformedURLException;

import javax.management.*;
import javax.management.remote.*;

public class CloseUnconnectedTest {
    private static final String[] protocols = {"rmi", "iiop", "jmxmp"};

    public static void main(String[] args) {
        System.out.println("Test that a connection can be opened and " +
                           "immediately closed without any operations");

        MBeanServer mbs = MBeanServerFactory.createMBeanServer();

        boolean ok = true;
        for (int i = 0; i < protocols.length; i++) {
            try {
                if (!test(protocols[i], mbs)) {
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

    private static boolean test(String proto, MBeanServer mbs)
            throws Exception {
        System.out.println("Test immediate client close for protocol " +
                           proto);
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
        JMXConnector c = JMXConnectorFactory.newJMXConnector(a, null);
        c.close();
        s.stop();
        return true;
    }
}
