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
 * @test FailedConnectionTest 1.1 03/11/26
 * @bug 4939578
 * @summary test to get an IOException.
 * @author Shanliang JIANG
 *
 * @run clean FailedConnectionTest
 * @run build FailedConnectionTest
 * @run main FailedConnectionTest
 */

import java.net.MalformedURLException;
import java.io.IOException;
import java.util.HashMap;

import javax.management.*;
import javax.management.remote.*;

public class FailedConnectionTest {
    private static final String[] protocols = {"rmi", "iiop", "jmxmp"};
    private static final MBeanServer mbs = MBeanServerFactory.createMBeanServer();

    public static void main(String[] args) {
        System.out.println(">>> test to get an IOException when calling"+
                          " getConnectionID on a closed connection.");

        boolean ok = true;
        for (int i = 0; i < protocols.length; i++) {
            try {
                if (!test(protocols[i])) {
                    System.out.println(">>> Test failed for " + protocols[i]);
                    ok = false;
                } else {
                    System.out.println(">>> Test successed for " + protocols[i]);
                }
            } catch (Exception e) {
                System.out.println(">>> Test failed for " + protocols[i]);
                e.printStackTrace(System.out);
                ok = false;
            }
        }

        if (ok) {
            System.out.println(">>> Test passed");
        } else {
            System.out.println(">>> TEST FAILED");
            System.exit(1);
        }
    }

    private static boolean test(String proto)
            throws Exception {
        System.out.println(">>> Test for protocol " + proto);

        JMXServiceURL u = null;
        JMXConnectorServer server = null;

        try {
            u = new JMXServiceURL(proto, null, 0);
            server = JMXConnectorServerFactory.newJMXConnectorServer(u, null, mbs);
        } catch (MalformedURLException e) {
            System.out.println("Skipping unsupported URL " + proto);
            return true;
        }

        server.start();

        JMXServiceURL addr = server.getAddress();

        HashMap env = new HashMap(1);
        env.put("jmx.remote.x.client.connection.check.period", "0");

        JMXConnector client = JMXConnectorFactory.connect(addr, env);
        server.stop();
        Thread.sleep(1000);
        try {
            client.getConnectionId();

            System.out.println("Do not get expected IOException, failed.");
            return false;
        } catch (IOException ioe) {
            // Good
            return true;
        }
    }
}
