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
 * @bug 5016508
 * @summary Tests the default JAAS configuration for authenticating RMI clients
 * @author Luis-Miguel Alventosa
 * @modules java.management.rmi
 *          java.management/com.sun.jmx.remote.security
 * @run clean RMIPasswdAuthTest
 * @run build RMIPasswdAuthTest SimpleStandard SimpleStandardMBean
 * @run main RMIPasswdAuthTest
 */

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.Properties;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import com.sun.jmx.remote.security.JMXPluggableAuthenticator;

public class RMIPasswdAuthTest {

    public static void main(String[] args) {
        try {

            // Set the default password file
            //
            final String passwordFile = System.getProperty("test.src") +
                File.separator + "jmxremote.password";
            System.out.println("Password file = " + passwordFile);

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
            // Register the ClassPathClassLoaderMBean
            //
            System.out.println("Create ClassPathClassLoader MBean");
            ObjectName cpcl =
                new ObjectName("ClassLoader:name=ClassPathClassLoader");
            mbs.createMBean("javax.management.loading.MLet", cpcl);
            // Register the SimpleStandardMBean
            //
            System.out.println("Create SimpleStandard MBean");
            mbs.createMBean("SimpleStandard",
                            new ObjectName("MBeans:name=SimpleStandard"));
            // Create Properties containing the location of the password file
            //
            Properties props = new Properties();
            props.setProperty("jmx.remote.x.password.file", passwordFile);
            // Initialize environment map to be passed to the connector server
            //
            System.out.println("Initialize environment map");
            HashMap env = new HashMap();
            env.put("jmx.remote.authenticator",
                    new JMXPluggableAuthenticator(props));
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
            HashMap cli_env = new HashMap();
            // These credentials must match those in the supplied password file
            //
            String[] credentials = new String[] { "monitorRole" , "QED" };
            cli_env.put("jmx.remote.credentials", credentials);
            JMXConnector jmxc = JMXConnectorFactory.connect(url, cli_env);
            MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
            // Get domains from MBeanServer
            //
            System.out.println("Domains:");
            String domains[] = mbsc.getDomains();
            for (int i = 0; i < domains.length; i++) {
                System.out.println("\tDomain[" + i + "] = " + domains[i]);
            }
            // Get MBean count
            //
            System.out.println("MBean count = " + mbsc.getMBeanCount());
            // Get State attribute
            //
            String oldState =
                (String) mbsc.getAttribute(
                              new ObjectName("MBeans:name=SimpleStandard"),
                              "State");
            System.out.println("Old State = \"" + oldState + "\"");
            // Set State attribute
            //
            System.out.println("Set State to \"changed state\"");
            mbsc.setAttribute(new ObjectName("MBeans:name=SimpleStandard"),
                              new Attribute("State", "changed state"));
            // Get State attribute
            //
            String newState =
                (String) mbsc.getAttribute(
                              new ObjectName("MBeans:name=SimpleStandard"),
                              "State");
            System.out.println("New State = \"" + newState + "\"");
            if (!newState.equals("changed state")) {
                System.out.println("Invalid State = \"" + newState + "\"");
                System.exit(1);
            }
            // Add notification listener on SimpleStandard MBean
            //
            System.out.println("Add notification listener...");
            mbsc.addNotificationListener(
                 new ObjectName("MBeans:name=SimpleStandard"),
                 new NotificationListener() {
                     public void handleNotification(Notification notification,
                                                    Object handback) {
                         System.out.println("Received notification: " +
                                            notification);
                     }
                 },
                 null,
                 null);
            // Unregister SimpleStandard MBean
            //
            System.out.println("Unregister SimpleStandard MBean...");
            mbsc.unregisterMBean(new ObjectName("MBeans:name=SimpleStandard"));
            // Close MBeanServer connection
            //
            jmxc.close();
            System.out.println("Bye! Bye!");
        } catch (Exception e) {
            System.out.println("Unexpected exception caught = " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
