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
 * @bug 6238815
 * @summary test the new interface Addressable
 * @author Shanliang JIANG
 *
 * @run clean AddressableTest
 * @run build AddressableTest
 * @run main AddressableTest
 */

import java.util.*;
import java.net.MalformedURLException;
import java.io.IOException;

import javax.management.*;
import javax.management.remote.*;
import javax.management.remote.rmi.*;

public class AddressableTest {
    private static final String[] protocols = {"rmi", "iiop"};
    private static final String[] prefixes = {"stub", "ior"};

    private static final MBeanServer mbs = MBeanServerFactory.createMBeanServer();

    private static boolean isProtocolSupported(String protocol) {
        if (protocol.equals("rmi"))
            return true;
        if (protocol.equals("iiop")) {
            try {
                Class.forName("javax.management.remote.rmi._RMIConnectionImpl_Tie");
                return true;
            } catch (ClassNotFoundException x) { }
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(">>> test the new interface Addressable.");
        boolean ok = true;

        for (int i = 0; i < protocols.length; i++) {
            String protocol = protocols[i];
            if (isProtocolSupported(protocol)) {
                try {
                    test(protocol, prefixes[i]);
                    System.out.println(">>> Test successed for "+protocols[i]);
                } catch (Exception e) {
                    System.out.println(">>> Test failed for "+protocols[i]);
                    e.printStackTrace(System.out);
                    ok = false;
                }
            } else {
                System.out.format(">>> Test skipped for %s, protocol not supported%n",
                    protocol);
            }
        }

        if (ok) {
            System.out.println(">>> All Test passed.");
        } else {
            System.out.println(">>> Some TESTs FAILED");
            throw new RuntimeException("See log for details");
        }
    }

    public static void test(String proto, String prefix) throws Exception {
        JMXServiceURL url = new JMXServiceURL("service:jmx:" + proto + "://");
        JMXConnectorServer server =
                    JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);

        server.start();

        JMXServiceURL serverAddr1 = server.getAddress();
        System.out.println(">>> Created a server with address "+serverAddr1);

        JMXConnector client1 = JMXConnectorFactory.connect(serverAddr1);
        JMXServiceURL clientAddr1 = ((JMXAddressable)client1).getAddress();

        System.out.println(">>> Created a client with address "+clientAddr1);

        if (!serverAddr1.equals(clientAddr1)) {
            throw new RuntimeException("The "+proto+" client does not return correct address.");
        }

        int i = clientAddr1.toString().indexOf(prefix);

        JMXServiceURL clientAddr2 =
            new JMXServiceURL("service:jmx:"+proto+":///"+clientAddr1.toString().substring(i));

        JMXConnector client2 = JMXConnectorFactory.connect(clientAddr2);

        System.out.println(">>> Created a client with address "+clientAddr2);

        if (!clientAddr2.equals(((JMXAddressable)client2).getAddress())) {
            throw new RuntimeException("The "+proto+" client does not return correct address.");
        }

        System.out.println(">>> The new client's host is "+clientAddr2.getHost()
                               +", port is "+clientAddr2.getPort());

        client1.close();
        client2.close();
        server.stop();
    }
}
