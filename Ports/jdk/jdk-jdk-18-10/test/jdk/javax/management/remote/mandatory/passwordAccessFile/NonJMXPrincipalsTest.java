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
 * @bug 5025141
 * @summary Tests that MBeanServerFileAccessController supports
 *          principals other than JMXPrincipal.
 * @author Luis-Miguel Alventosa
 *
 * @run clean NonJMXPrincipalsTest SimpleStandard SimpleStandardMBean
 * @run build NonJMXPrincipalsTest SimpleStandard SimpleStandardMBean
 * @run main NonJMXPrincipalsTest
 */

import java.io.File;
import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;
import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXPrincipal;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;

public class NonJMXPrincipalsTest {

    private static class OtherPrincipal implements Principal, Serializable {

        private String name;

        public OtherPrincipal(String name) {
            if (name == null)
                throw new NullPointerException("illegal null input");
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String toString() {
            return("OtherPrincipal:  " + name);
        }

        public boolean equals(Object o) {
            if (o == null)
                return false;
            if (this == o)
                return true;
            if (!(o instanceof OtherPrincipal))
                return false;
            OtherPrincipal that = (OtherPrincipal)o;
            return (this.getName().equals(that.getName()));
        }

        public int hashCode() {
            return name.hashCode();
        }
    }

    private static class OtherPrincipalAuthenticator
        implements JMXAuthenticator {
        public Subject authenticate(Object credentials) {
            final String[] aCredentials = (String[]) credentials;
            final String username = (String) aCredentials[0];
            final Subject subject = new Subject();
            subject.getPrincipals().add(new JMXPrincipal("dummy"));
            subject.getPrincipals().add(new OtherPrincipal(username));
            return subject;
        }
    }

    private static class NoPrincipalAuthenticator
        implements JMXAuthenticator {
        public Subject authenticate(Object credentials) {
            return new Subject();
        }
    }

    public static void runTest(JMXAuthenticator authenticator)
        throws Exception {
        //------------------------------------------------------------------
        // SERVER
        //------------------------------------------------------------------

        // Instantiate the MBean server
        //
        System.out.println("Create the MBean server");
        MBeanServer mbs = MBeanServerFactory.createMBeanServer();

        // Create SimpleStandard MBean
        //
        ObjectName mbeanName = new ObjectName("MBeans:type=SimpleStandard");
        System.out.println("Create SimpleStandard MBean...");
        mbs.createMBean("SimpleStandard", mbeanName, null, null);

        // Server's environment map
        //
        System.out.println(">>> Initialize the server's environment map");
        HashMap sEnv = new HashMap();

        // Provide a JMX Authenticator
        //
        sEnv.put("jmx.remote.authenticator", authenticator);

        // Provide the access level file used by the connector server to
        // perform user authorization. The access level file is a properties
        // based text file specifying username/access level pairs where
        // access level is either "readonly" or "readwrite" access to the
        // MBeanServer operations. This properties based access control
        // checker has been implemented using the MBeanServerForwarder
        // interface which wraps the real MBean server inside an access
        // controller MBean server which performs the access control checks
        // before forwarding the requests to the real MBean server.
        //
        // This property is implementation-dependent and might not be
        // supported by all implementations of the JMX Remote API.
        //
        sEnv.put("jmx.remote.x.access.file",
                 System.getProperty("test.src") +
                 File.separator +
                 "access.properties");

        // Create an RMI connector server
        //
        System.out.println("Create an RMI connector server");
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
        JMXConnectorServer cs =
            JMXConnectorServerFactory.newJMXConnectorServer(url, sEnv, mbs);

        // Start the RMI connector server
        //
        System.out.println("Start the RMI connector server");
        cs.start();
        System.out.println("RMI connector server successfully started");
        System.out.println("Waiting for incoming connections...");

        //------------------------------------------------------------------
        // CLIENT (admin)
        //------------------------------------------------------------------

        // Admin client environment map
        //
        String[] adminCreds = new String[] { "admin" , "adminPassword" };
        System.out.println(">>> Initialize the client environment map for" +
                           " user [" + adminCreds[0] + "] with " +
                           "password [" + adminCreds[1] + "]");
        HashMap adminEnv = new HashMap();
        adminEnv.put("jmx.remote.credentials", adminCreds);

        // Create an RMI connector client and
        // connect it to the RMI connector server
        //
        System.out.println("Create an RMI connector client and " +
                           "connect it to the RMI connector server");
        JMXConnector adminConnector =
            JMXConnectorFactory.connect(cs.getAddress(), adminEnv);

        // Get an MBeanServerConnection
        //
        System.out.println("Get an MBeanServerConnection");
        MBeanServerConnection adminConnection =
            adminConnector.getMBeanServerConnection();

        // Get the proxy for the Simple MBean
        //
        SimpleStandardMBean adminProxy = (SimpleStandardMBean)
            MBeanServerInvocationHandler.newProxyInstance(
                                                 adminConnection,
                                                 mbeanName,
                                                 SimpleStandardMBean.class,
                                                 false);

        // Get State attribute
        //
        System.out.println("State = " + adminProxy.getState());

        // Set State attribute
        //
        adminProxy.setState("changed state");

        // Get State attribute
        //
        System.out.println("State = " + adminProxy.getState());

        // Invoke "reset" in SimpleStandard MBean
        //
        System.out.println("Invoke reset() in SimpleStandard MBean...");
        adminProxy.reset();

        // Close MBeanServer connection
        //
        System.out.println("Close the admin connection to the server");
        adminConnector.close();

        //------------------------------------------------------------------
        // CLIENT (user)
        //------------------------------------------------------------------

        // User client environment map
        //
        String[] userCreds = new String[] { "user" , "userPassword" };
        System.out.println(">>> Initialize the client environment map for" +
                           " user [" + userCreds[0] + "] with " +
                           "password [" + userCreds[1] + "]");
        HashMap userEnv = new HashMap();
        userEnv.put("jmx.remote.credentials", userCreds);

        // Create an RMI connector client and
        // connect it to the RMI connector server
        //
        System.out.println("Create an RMI connector client and " +
                           "connect it to the RMI connector server");
        JMXConnector userConnector =
            JMXConnectorFactory.connect(cs.getAddress(), userEnv);

        // Get an MBeanServerConnection
        //
        System.out.println("Get an MBeanServerConnection");
        MBeanServerConnection userConnection =
            userConnector.getMBeanServerConnection();

        // Get the proxy for the Simple MBean
        //
        SimpleStandardMBean userProxy = (SimpleStandardMBean)
            MBeanServerInvocationHandler.newProxyInstance(
                                                 userConnection,
                                                 mbeanName,
                                                 SimpleStandardMBean.class,
                                                 false);

        // Get State attribute
        //
        System.out.println("State = " + userProxy.getState());

        // Set State attribute
        //
        try {
            userProxy.setState("changed state");
        } catch (SecurityException e) {
            System.out.println("Got expected security exception: " + e);
        } catch (Exception e) {
            System.out.println("Got unexpected exception: " + e);
            e.printStackTrace(System.out);
        }

        // Get State attribute
        //
        System.out.println("State = " + userProxy.getState());

        // Invoke "reset" in SimpleStandard MBean
        //
        try {
            System.out.println("Invoke reset() in SimpleStandard MBean...");
            userProxy.reset();
        } catch (SecurityException e) {
            System.out.println("Got expected security exception: " + e);
        } catch (Exception e) {
            System.out.println("Got unexpected exception: " + e);
            e.printStackTrace(System.out);
        }

        // Close MBeanServer connection
        //
        System.out.println("Close the user connection to the server");
        userConnector.close();

        //------------------------------------------------------------------
        // SERVER
        //------------------------------------------------------------------

        // Stop the connector server
        //
        System.out.println(">>> Stop the connector server");
        cs.stop();
    }

    public static void main(String[] args) {
        int errorCount = 0;
        // Runt tests
        //
        System.out.println("\n>>> Run NoPrincipalAuthenticator test...");
        try {
            NonJMXPrincipalsTest.runTest(new NoPrincipalAuthenticator());
            System.out.println("Did not get expected SecurityException");
            errorCount++;
        } catch (Exception e) {
            if (e instanceof SecurityException) {
                System.out.println("Got expected exception: " + e);
            } else {
                System.out.println("Got unexpected exception: " + e);
                errorCount++;
            }
            e.printStackTrace(System.out);
        }
        System.out.println("\n>>> Run OtherPrincipalAuthenticator test...");
        try {
            NonJMXPrincipalsTest.runTest(new OtherPrincipalAuthenticator());
        } catch (Exception e) {
            errorCount++;
            System.out.println("Got unexpected exception: " + e);
            e.printStackTrace(System.out);
        }

        if (errorCount > 0) {
            System.out.println("\nTEST FAILED! Error count = " + errorCount);
            System.exit(1);
        }

        System.out.println("\nTEST PASSED!");
        System.out.println("\nBye! Bye!");
    }
}
