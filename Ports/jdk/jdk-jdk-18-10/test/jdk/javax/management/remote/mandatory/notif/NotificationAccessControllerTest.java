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
 * @summary Check the NotificationAccessController methods are properly called.
 * @author Luis-Miguel Alventosa
 * @modules java.management.rmi
 *          java.management/com.sun.jmx.remote.security
 * @run clean NotificationAccessControllerTest
 * @run build NotificationAccessControllerTest
 * @run main NotificationAccessControllerTest
 */

import com.sun.jmx.remote.security.NotificationAccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
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

public class NotificationAccessControllerTest {

    public class NAC implements NotificationAccessController {
        private final boolean throwException;
        public NAC(boolean throwException) {
            this.throwException = throwException;
        }

        @Override
        public void addNotificationListener(
            String connectionId,
            ObjectName name,
            Subject subject)
            throws SecurityException {
            echo("addNotificationListener:");
            echo("\tconnectionId: " +  connectionId);
            echo("\tname: " +  name);
            echo("\tsubject: " +
                 (subject == null ? null : subject.getPrincipals()));
            if (throwException)
                if (name.getCanonicalName().equals("domain:name=1,type=NB")
                    &&
                    subject != null
                    &&
                    subject.getPrincipals().contains(new JMXPrincipal("role")))
                    throw new SecurityException();
        }

        @Override
        public void removeNotificationListener(
            String connectionId,
            ObjectName name,
            Subject subject)
            throws SecurityException {
            echo("removeNotificationListener:");
            echo("\tconnectionId: " +  connectionId);
            echo("\tname: " +  name);
            echo("\tsubject: " +
                 (subject == null ? null : subject.getPrincipals()));
            if (throwException)
                if (name.getCanonicalName().equals("domain:name=2,type=NB")
                    &&
                    subject != null
                    &&
                    subject.getPrincipals().contains(new JMXPrincipal("role")))
                    throw new SecurityException();
        }

        @Override
        public void fetchNotification(
            String connectionId,
            ObjectName name,
            Notification notification,
            Subject subject)
            throws SecurityException {
            echo("fetchNotification:");
            echo("\tconnectionId: " +  connectionId);
            echo("\tname: " +  name);
            echo("\tnotification: " +  notification);
            echo("\tsubject: " +
                 (subject == null ? null : subject.getPrincipals()));
            if (!throwException)
                if (name.getCanonicalName().equals("domain:name=2,type=NB")
                    &&
                    subject != null
                    &&
                    subject.getPrincipals().contains(new JMXPrincipal("role")))
                    throw new SecurityException();
        }
    }

    public class CustomJMXAuthenticator implements JMXAuthenticator {
        @Override
        public Subject authenticate(Object credentials) {
            String role = ((String[]) credentials)[0];
            echo("\nCreate principal with name = " + role);
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
        @Override
        public void emitNotification(int seqnum, ObjectName name) {
            if (name == null) {
                sendNotification(new Notification("nb", this, seqnum));
            } else {
                sendNotification(new Notification("nb", name, seqnum));
            }
        }
    }

    public class Listener implements NotificationListener {
        public final List<Notification> notifs = new CopyOnWriteArrayList<>();

        private final Semaphore s;
        public Listener(Semaphore s) {
            this.s = s;
        }
        @Override
        public void handleNotification(Notification n, Object h) {
            echo("handleNotification:");
            echo("\tNotification = " + n);
            echo("\tNotification.SeqNum = " + n.getSequenceNumber());
            echo("\tHandback = " + h);
            notifs.add(n);
            s.release();
        }
    }

    /**
     * Check received notifications
     */
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

    /**
     * Run test
     */
    public int runTest(boolean enableChecks, boolean throwException)
        throws Exception {

        echo("\n=-=-= " + (enableChecks ? "Enable" : "Disable") +
             " notification access control checks " +
             (!enableChecks ? "" : (throwException ? ": add/remove " :
             ": fetch ")) + "=-=-=");

        JMXConnectorServer server = null;
        JMXConnector client = null;

        /*
        * (!enableChecks)
        * - List must contain three notifs from sources nb1, nb2 and nb3
        * (enableChecks && !throwException)
        * - List must contain one notif from source nb1
        * (enableChecks && throwException)
        * - List must contain two notifs from sources nb2 and nb3
        */
        final int expected_notifs =
            (!enableChecks ? 3 : (throwException ? 2 : 1));

        // Create a new MBeanServer
        //
        final MBeanServer mbs = MBeanServerFactory.createMBeanServer();

        try {
            // Create server environment map
            //
            final Map<String,Object> env = new HashMap<>();
            env.put("jmx.remote.authenticator", new CustomJMXAuthenticator());
            if (enableChecks) {
                env.put("com.sun.jmx.remote.notification.access.controller",
                        new NAC(throwException));
            }

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
            final Map<String,Object> cenv = new HashMap<>();
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
            Semaphore s = new Semaphore(0);

            Listener li = new Listener(s);
            try {
                mbsc.addNotificationListener(nb1, li, null, null);
                if (enableChecks && throwException) {
                    echo("Didn't get expected exception");
                    return 1;
                }
            } catch (SecurityException e) {
                if (enableChecks && throwException) {
                    echo("Got expected exception: " + e);
                } else {
                    echo("Got unexpected exception: " + e);
                    return 1;
                }
            }
            mbsc.addNotificationListener(nb2, li, null, null);

            System.out.println("\n+++ Expecting to receive " + expected_notifs +
                               " notification" + (expected_notifs > 1 ? "s" : "") +
                               " +++");
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

            // Wait for notifications to be emitted
            //
            s.acquire(expected_notifs);

            // Remove notification listener
            //
            if (!throwException)
                mbsc.removeNotificationListener(nb1, li);
            try {
                mbsc.removeNotificationListener(nb2, li);
                if (enableChecks && throwException) {
                    echo("Didn't get expected exception");
                    return 1;
                }
            } catch (SecurityException e) {
                if (enableChecks && throwException) {
                    echo("Got expected exception: " + e);
                } else {
                    echo("Got unexpected exception: " + e);
                    return 1;
                }
            }

            int result = 0;
            List<ObjectName> sources = new ArrayList();
            sources.add(nb1);
            sources.add(nb2);
            sources.add(nb3);
            result = checkNotifs(expected_notifs, li.notifs, sources);
            if (result > 0) {
                return result;
            }
        } catch (Exception e) {
            echo("Failed to perform operation: " + e);
            e.printStackTrace();
            return 1;
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

    /*
     * Print message
     */
    private static void echo(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) throws Exception {

        System.out.println("\nTest notification access control.");

        NotificationAccessControllerTest nact =
            new NotificationAccessControllerTest();

        int error = 0;

        error += nact.runTest(false, false);

        error += nact.runTest(true, false);

        error += nact.runTest(true, true);

        if (error > 0) {
            final String msg = "\nTest FAILED! Got " + error + " error(s)";
            System.out.println(msg);
            throw new IllegalArgumentException(msg);
        } else {
            System.out.println("\nTest PASSED!");
        }
    }
}
