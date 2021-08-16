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
 * @bug 7654321
 * @summary Tests the use of the custom RMI socket factories.
 * @author Luis-Miguel Alventosa
 *
 * @run clean RMISocketFactoriesTest
 * @run build RMISocketFactoriesTest RMIClientFactory RMIServerFactory
 * @run main RMISocketFactoriesTest test_server_factory
 * @run main RMISocketFactoriesTest test_client_factory
 */

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer; // for constants

public class RMISocketFactoriesTest {

    public static void main(String[] args) {
        System.out.println("Test RMI factories : " + args[0]);
        try {
            // Create an RMI registry
            //
            System.out.println("Start RMI registry...");
            Registry reg = null;
            int port = 5800;
            while (port++ < 6000) {
                try {
                    reg = LocateRegistry.createRegistry(port);
                    System.out.println("RMI registry running on port " + port);
                    break;
                } catch (RemoteException e) {
                    // Failed to create RMI registry...
                    System.out.println("Failed to create RMI registry " +
                                       "on port " + port);
                }
            }
            if (reg == null) {
                System.exit(1);
            }

            // Instantiate the MBean server
            //
            System.out.println("Create the MBean server");
            MBeanServer mbs = MBeanServerFactory.createMBeanServer();
            // Initialize environment map to be passed to the connector server
            //
            System.out.println("Initialize environment map");
            HashMap env = new HashMap();
            env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE,
                    new RMIServerFactory(args[0]));
            env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE,
                    new RMIClientFactory(args[0]));
            // Create an RMI connector server
            //
            System.out.println("Create an RMI connector server");
            JMXServiceURL url =
                new JMXServiceURL("rmi", null, 0,
                                  "/jndi/rmi://:" + port + "/server" + port);
            JMXConnectorServer rcs =
                JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
            rcs.start();

            // Create an RMI connector client
            //
            System.out.println("Create an RMI connector client");
            JMXConnector jmxc = JMXConnectorFactory.connect(url, new HashMap());

            // If this line is executed then the test failed
            //
            System.exit(1);
        } catch (Exception e) {
            if (e.getMessage().equals(args[0])) {
                System.out.println("Expected exception caught = " + e);
                System.out.println("Bye! Bye!");
            } else {
                System.out.println("Unexpected exception caught = " + e);
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
