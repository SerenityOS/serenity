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
 * @bug 5106721
 * @key intermittent
 * @summary Check the emission of notifications when a Security Manager is
 * installed. Test the property "jmx.remote.x.check.notification.emission".
 * @author Luis-Miguel Alventosa
 *
 * @run clean NotificationEmissionTest
 * @run build NotificationEmissionTest
 * @run main NotificationEmissionTest 1
 * @run main/othervm -Djava.security.manager=allow NotificationEmissionTest 2
 * @run main/othervm -Djava.security.manager=allow NotificationEmissionTest 3
 * @run main/othervm -Djava.security.manager=allow NotificationEmissionTest 4
 * @run main/othervm -Djava.security.manager=allow NotificationEmissionTest 5
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
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

public class NotificationEmissionTest {

    public class CustomJMXAuthenticator implements JMXAuthenticator {
        public Subject authenticate(Object credentials) {
            String role = ((String[]) credentials)[0];
            echo("Create principal with name = " + role);
            return new Subject(true,
                               Collections.singleton(new JMXPrincipal(role)),
                               Collections.EMPTY_SET,
                               Collections.EMPTY_SET);
        }
    }

    public interface NBMBean {
        public void emitNotification(int seqnum, ObjectName name);
    }

    public static class NB
        extends NotificationBroadcasterSupport
        implements NBMBean {
        public void emitNotification(int seqnum, ObjectName name) {
            if (name == null) {
                sendNotification(new Notification("nb", this, seqnum));
            } else {
                sendNotification(new Notification("nb", name, seqnum));
            }
        }
    }

    public class Listener implements NotificationListener {
        public List<Notification> notifs = new ArrayList<Notification>();
        public void handleNotification(Notification n, Object h) {
            echo("handleNotification:");
            echo("\tNotification = " + n);
            echo("\tNotification.SeqNum = " + n.getSequenceNumber());
            echo("\tHandback = " + h);
            notifs.add(n);
        }
    }

    public int checkNotifs(int size,
                           List<Notification> received,
                           List<ObjectName> expected) {
        if (received.size() != size) {
            echo("Error: expecting " + size + " notifications, got " +
                    received.size());
            return 1;
        } else {
            for (Notification n : received) {
                echo("Received notification: " + n);
                if (!n.getType().equals("nb")) {
                    echo("Notification type must be \"nb\"");
                    return 1;
                }
                ObjectName o = (ObjectName) n.getSource();
                int index = (int) n.getSequenceNumber();
                ObjectName nb = expected.get(index);
                if (!o.equals(nb)) {
                    echo("Notification source must be " + nb);
                    return 1;
                }
            }
        }
        return 0;
    }

    public int runTest(int testcase) throws Exception {
        echo("\n=-=-= Running testcase " + testcase + " =-=-=");
        switch (testcase) {
            case 1:
                return testNotificationEmissionProperty();
            case 2:
                return testNotificationEmissionPositive(false);
            case 3:
                return testNotificationEmissionNegative(false);
            case 4:
                return testNotificationEmissionPositive(true);
            case 5:
                return testNotificationEmissionNegative(true);
            default:
                echo("Invalid testcase");
                return 1;
        }
    }

    public int testNotificationEmissionProperty(boolean exception,
                                                Object propValue)
        throws Exception {
        try {
            testNotificationEmission(propValue);
            if (exception) {
                echo("Did not get expected exception for value: " + propValue);
                return 1;
            } else {
                echo("Property has been correctly set to value: " + propValue);
            }
        } catch (Exception e) {
            if (exception) {
                echo("Got expected exception for value: " + propValue);
                echo("Exception: " + e);
            } else {
                echo("Got unexpected exception for value: " + propValue);
                echo("Exception: " + e);
                return 1;
            }
        }
        return 0;
    }

    public int testNotificationEmissionProperty() throws Exception {
        int error = 0;
        error += testNotificationEmissionProperty(true, new Boolean(false));
        error += testNotificationEmissionProperty(true, new Boolean(true));
        error += testNotificationEmissionProperty(true, "dummy");
        error += testNotificationEmissionProperty(false, "false");
        error += testNotificationEmissionProperty(false, "true");
        error += testNotificationEmissionProperty(false, "FALSE");
        error += testNotificationEmissionProperty(false, "TRUE");
        return error;
    }

    public int testNotificationEmissionPositive(boolean prop) throws Exception {
        return testNotificationEmission(prop, "true", true, true);
    }

    public int testNotificationEmissionNegative(boolean prop) throws Exception {
        return testNotificationEmission(prop, "true", true, false);
    }

    public int testNotificationEmission(Object propValue) throws Exception {
        return testNotificationEmission(true, propValue, false, true);
    }

    public int testNotificationEmission(boolean prop,
                                        Object propValue,
                                        boolean sm,
                                        boolean policyPositive)
        throws Exception {

        JMXConnectorServer server = null;
        JMXConnector client = null;

        // Set policy file
        //
        String policyFile =
            System.getProperty("test.src") + File.separator +
            (policyPositive ? "policy.positive" : "policy.negative");
        echo("\nSetting policy file " + policyFile);
        System.setProperty("java.security.policy", policyFile);

        // Create a new MBeanServer
        //
        final MBeanServer mbs = MBeanServerFactory.createMBeanServer();

        try {
            // Create server environment map
            //
            final Map<String,Object> env = new HashMap<String,Object>();
            env.put("jmx.remote.authenticator", new CustomJMXAuthenticator());
            if (prop)
                env.put("jmx.remote.x.check.notification.emission", propValue);

            // Create the JMXServiceURL
            //
            final JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");

            // Create a JMXConnectorServer
            //
            server = JMXConnectorServerFactory.newJMXConnectorServer(url,
                                                                     env,
                                                                     mbs);

            // Start the JMXConnectorServer
            //
            server.start();

            // Create server environment map
            //
            final Map<String,Object> cenv = new HashMap<String,Object>();
            String[] credentials = new String[] { "role" , "password" };
            cenv.put("jmx.remote.credentials", credentials);

            // Create JMXConnector and connect to JMXConnectorServer
            //
            client = JMXConnectorFactory.connect(server.getAddress(), cenv);

            // Get non-secure MBeanServerConnection
            //
            final MBeanServerConnection mbsc =
                client.getMBeanServerConnection();

            // Create NB MBean
            //
            ObjectName nb1 = ObjectName.getInstance("domain:type=NB,name=1");
            ObjectName nb2 = ObjectName.getInstance("domain:type=NB,name=2");
            ObjectName nb3 = ObjectName.getInstance("domain:type=NB,name=3");
            mbsc.createMBean(NB.class.getName(), nb1);
            mbsc.createMBean(NB.class.getName(), nb2);
            mbsc.createMBean(NB.class.getName(), nb3);

            // Add notification listener
            //
            Listener li = new Listener();
            mbsc.addNotificationListener(nb1, li, null, null);
            mbsc.addNotificationListener(nb2, li, null, null);

            // Set security manager
            //
            if (sm) {
                echo("Setting SM");
                System.setSecurityManager(new SecurityManager());
            }

            // Invoke the "sendNotification" method
            //
            mbsc.invoke(nb1, "emitNotification",
                new Object[] {0, null},
                new String[] {"int", "javax.management.ObjectName"});
            mbsc.invoke(nb2, "emitNotification",
                new Object[] {1, null},
                new String[] {"int", "javax.management.ObjectName"});
            mbsc.invoke(nb2, "emitNotification",
                new Object[] {2, nb3},
                new String[] {"int", "javax.management.ObjectName"});

            // If the check is effective and we're using policy.negative,
            // then we should see the two notifs sent by nb2 (of which one
            // has a getSource() that is nb3), but not the notif sent by nb1.
            // Otherwise we should see all three notifs.  The check is only
            // effective if the property jmx.remote.x.check.notification.emission
            // is explicitly true and there is a security manager.
            int expectedNotifs =
                    (prop && sm && !policyPositive) ? 2 : 3;

            // Wait for notifications to be emitted
            //
            long deadline = System.currentTimeMillis() + 2000;
            while (li.notifs.size() < expectedNotifs &&
                    System.currentTimeMillis() < deadline)
                Thread.sleep(1);

            // Remove notification listener
            //
            mbsc.removeNotificationListener(nb1, li);
            mbsc.removeNotificationListener(nb2, li);

            int result = 0;
            List<ObjectName> sources = new ArrayList<ObjectName>();
            sources.add(nb1);
            sources.add(nb2);
            sources.add(nb3);

            result = checkNotifs(expectedNotifs, li.notifs, sources);
            if (result > 0) {
                echo("...SecurityManager=" + sm + "; policy=" + policyPositive);
                return result;
            }
        } finally {
            // Close the connection
            //
            if (client != null)
                client.close();

            // Stop the connector server
            //
            if (server != null)
                server.stop();

            // Release the MBeanServer
            //
            if (mbs != null)
                MBeanServerFactory.releaseMBeanServer(mbs);
        }

        return 0;
    }

    private static void echo(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) throws Exception {

        echo("\n--- Check the emission of notifications " +
             "when a Security Manager is installed");

        NotificationEmissionTest net = new NotificationEmissionTest();

        int error = 0;

        error += net.runTest(Integer.parseInt(args[0]));

        if (error > 0) {
            final String msg = "\nTest FAILED! Got " + error + " error(s)";
            echo(msg);
            throw new IllegalArgumentException(msg);
        } else {
            echo("\nTest PASSED!");
        }
    }
}
