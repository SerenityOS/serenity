/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4901808 7183800
 * @summary Check that RMI connection ids include IP address of a client network interface
 * @author Eamonn McManus
 *
 * @run clean RMIConnectionIdTest
 * @run build RMIConnectionIdTest
 * @run main RMIConnectionIdTest
 */

import java.net.*;
import javax.management.*;
import javax.management.remote.*;

public class RMIConnectionIdTest {
    public static void main(String[] args) throws Exception {
        System.out.println("Testing that RMI connection id includes " +
                           "IP address of a client network interface");
        MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        JMXServiceURL url = new JMXServiceURL("rmi", null, 0);
        JMXConnectorServer cs =
            JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
        cs.start();
        JMXServiceURL addr = cs.getAddress();
        JMXConnector cc = JMXConnectorFactory.connect(addr);
        String connectionId = cc.getConnectionId();
        System.out.println("Got connection id: " + connectionId);
        if (!connectionId.startsWith("rmi://")) {
            System.out.println("TEST FAILED: does not begin with \"rmi://\"");
            System.exit(1);
        }
        String rest = connectionId.substring("rmi://".length());
        int spaceIndex = rest.indexOf(' ');
        if (spaceIndex < 0) {
            System.out.println("TEST FAILED: no space");
            System.exit(1);
        }
        String clientAddr = rest.substring(0, spaceIndex);
        InetAddress clientInetAddr = InetAddress.getByName(clientAddr);
        NetworkInterface ni = NetworkInterface.getByInetAddress(clientInetAddr);
        if (ni == null) {
            System.out.println("TEST FAILS: address not found");
            System.exit(1);
        }
        cc.close();
        cs.stop();
        System.out.println("Test passed");
    }
}
