/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4915825 4921009 4934965 4977469 8019584
 * @key randomness
 * @summary Tests behavior when client or server gets object of unknown class
 * @author Eamonn McManus
 *
 * @run clean MissingClassTest SingleClassLoader
 * @run build MissingClassTest SingleClassLoader
 * @run main MissingClassTest
 */

/*
  Tests that clients and servers react correctly when they receive
  objects of unknown classes.  This can happen easily due to version
  skew or missing jar files on one end or the other.  The default
  behaviour of causing a connection to die because of the resultant
  IOException is not acceptable!  We try sending attributes and invoke
  parameters to the server of classes it doesn't know, and we try
  sending attributes, exceptions and notifications to the client of
  classes it doesn't know.

  We also test objects that are of known class but not serializable.
  The test cases are similar.
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;

public class MissingClassTest {
    private static final int NNOTIFS = 50;

    private static ClassLoader clientLoader, serverLoader;
    private static Object serverUnknown;
    private static Exception clientUnknown;
    private static ObjectName on;
    private static final Object[] NO_OBJECTS = new Object[0];
    private static final String[] NO_STRINGS = new String[0];

    private static final Object unserializableObject = Thread.currentThread();

    private static boolean isInstance(Object o, String cn) {
        try {
            Class<?> c = Class.forName(cn);
            return c.isInstance(o);
        } catch (ClassNotFoundException x) {
            return false;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Test that the client or server end of a " +
                           "connection does not fail if sent an object " +
                           "it cannot deserialize");

        on = new ObjectName("test:type=Test");

        ClassLoader testLoader = MissingClassTest.class.getClassLoader();
        clientLoader =
            new SingleClassLoader("$ServerUnknown$", HashMap.class,
                                  testLoader);
        serverLoader =
            new SingleClassLoader("$ClientUnknown$", Exception.class,
                                  testLoader);
        serverUnknown =
            clientLoader.loadClass("$ServerUnknown$").newInstance();
        clientUnknown = (Exception)
            serverLoader.loadClass("$ClientUnknown$").newInstance();

        final String[] protos = {"rmi", /*"iiop",*/ "jmxmp"};
        boolean ok = true;
        for (int i = 0; i < protos.length; i++) {
            try {
                ok &= test(protos[i]);
            } catch (Exception e) {
                System.out.println("TEST FAILED WITH EXCEPTION:");
                e.printStackTrace(System.out);
                ok = false;
            }
        }

        if (ok)
            System.out.println("Test passed");
        else {
            throw new RuntimeException("TEST FAILED");
        }
    }

    private static boolean test(String proto) throws Exception {
        System.out.println("Testing for proto " + proto);

        boolean ok = true;

        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        mbs.createMBean(Test.class.getName(), on);

        JMXConnectorServer cs;
        JMXServiceURL url = new JMXServiceURL(proto, null, 0);
        Map<String,Object> serverMap = new HashMap<>();
        serverMap.put(JMXConnectorServerFactory.DEFAULT_CLASS_LOADER,
                      serverLoader);

        // make sure no auto-close at server side
        serverMap.put("jmx.remote.x.server.connection.timeout", "888888888");

        try {
            cs = JMXConnectorServerFactory.newJMXConnectorServer(url,
                                                                 serverMap,
                                                                 mbs);
        } catch (MalformedURLException e) {
            System.out.println("System does not recognize URL: " + url +
                               "; ignoring");
            return true;
        }
        cs.start();
        JMXServiceURL addr = cs.getAddress();
        Map<String,Object> clientMap = new HashMap<>();
        clientMap.put(JMXConnectorFactory.DEFAULT_CLASS_LOADER,
                      clientLoader);

        System.out.println("Connecting for client-unknown test");

        JMXConnector client = JMXConnectorFactory.connect(addr, clientMap);

        // add a listener to verify no failed notif
        CNListener cnListener = new CNListener();
        client.addConnectionNotificationListener(cnListener, null, null);

        MBeanServerConnection mbsc = client.getMBeanServerConnection();

        System.out.println("Getting attribute with class unknown to client");
        try {
            Object result = mbsc.getAttribute(on, "ClientUnknown");
            System.out.println("TEST FAILS: getAttribute for class " +
                               "unknown to client should fail, returned: " +
                               result);
            ok = false;
        } catch (IOException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ClassNotFoundException) {
                System.out.println("Success: got an IOException wrapping " +
                                   "a ClassNotFoundException");
            } else {
                System.out.println("TEST FAILS: Caught IOException (" + e +
                                   ") but cause should be " +
                                   "ClassNotFoundException: " + cause);
                ok = false;
            }
        }

        System.out.println("Doing queryNames to ensure connection alive");
        Set<ObjectName> names = mbsc.queryNames(null, null);
        System.out.println("queryNames returned " + names);

        System.out.println("Provoke exception of unknown class");
        try {
            mbsc.invoke(on, "throwClientUnknown", NO_OBJECTS, NO_STRINGS);
            System.out.println("TEST FAILS: did not get exception");
            ok = false;
        } catch (IOException e) {
            Throwable wrapped = e.getCause();
            if (wrapped instanceof ClassNotFoundException) {
                System.out.println("Success: got an IOException wrapping " +
                                   "a ClassNotFoundException: " +
                                   wrapped);
            } else {
                System.out.println("TEST FAILS: Got IOException but cause " +
                                   "should be ClassNotFoundException: ");
                if (wrapped == null)
                    System.out.println("(null)");
                else
                    wrapped.printStackTrace(System.out);
                ok = false;
            }
        } catch (Exception e) {
            System.out.println("TEST FAILS: Got wrong exception: " +
                               "should be IOException with cause " +
                               "ClassNotFoundException:");
            e.printStackTrace(System.out);
            ok = false;
        }

        System.out.println("Doing queryNames to ensure connection alive");
        names = mbsc.queryNames(null, null);
        System.out.println("queryNames returned " + names);

        ok &= notifyTest(client, mbsc);

        System.out.println("Doing queryNames to ensure connection alive");
        names = mbsc.queryNames(null, null);
        System.out.println("queryNames returned " + names);

        for (int i = 0; i < 2; i++) {
            boolean setAttribute = (i == 0); // else invoke
            String what = setAttribute ? "setAttribute" : "invoke";
            System.out.println("Trying " + what +
                               " with class unknown to server");
            try {
                if (setAttribute) {
                    mbsc.setAttribute(on, new Attribute("ServerUnknown",
                                                        serverUnknown));
                } else {
                    mbsc.invoke(on, "useServerUnknown",
                                new Object[] {serverUnknown},
                                new String[] {"java.lang.Object"});
                }
                System.out.println("TEST FAILS: " + what + " with " +
                                   "class unknown to server should fail " +
                                   "but did not");
                ok = false;
            } catch (IOException e) {
                Throwable cause = e.getCause();
                if (cause instanceof ClassNotFoundException) {
                    System.out.println("Success: got an IOException " +
                                       "wrapping a ClassNotFoundException");
                } else {
                    System.out.println("TEST FAILS: Caught IOException (" + e +
                                       ") but cause should be " +
                                       "ClassNotFoundException: " + cause);
                    e.printStackTrace(System.out); // XXX
                    ok = false;
                }
            }
        }

        System.out.println("Doing queryNames to ensure connection alive");
        names = mbsc.queryNames(null, null);
        System.out.println("queryNames returned " + names);

        System.out.println("Trying to get unserializable attribute");
        try {
            mbsc.getAttribute(on, "Unserializable");
            System.out.println("TEST FAILS: get unserializable worked " +
                               "but should not");
            ok = false;
        } catch (IOException e) {
            System.out.println("Success: got an IOException: " + e +
                               " (cause: " + e.getCause() + ")");
        }

        System.out.println("Doing queryNames to ensure connection alive");
        names = mbsc.queryNames(null, null);
        System.out.println("queryNames returned " + names);

        System.out.println("Trying to set unserializable attribute");
        try {
            Attribute attr =
                new Attribute("Unserializable", unserializableObject);
            mbsc.setAttribute(on, attr);
            System.out.println("TEST FAILS: set unserializable worked " +
                               "but should not");
            ok = false;
        } catch (IOException e) {
            System.out.println("Success: got an IOException: " + e +
                               " (cause: " + e.getCause() + ")");
        }

        System.out.println("Doing queryNames to ensure connection alive");
        names = mbsc.queryNames(null, null);
        System.out.println("queryNames returned " + names);

        System.out.println("Trying to throw unserializable exception");
        try {
            mbsc.invoke(on, "throwUnserializable", NO_OBJECTS, NO_STRINGS);
            System.out.println("TEST FAILS: throw unserializable worked " +
                               "but should not");
            ok = false;
        } catch (IOException e) {
            System.out.println("Success: got an IOException: " + e +
                               " (cause: " + e.getCause() + ")");
        }

        client.removeConnectionNotificationListener(cnListener);
        ok = ok && !cnListener.failed;

        client.close();
        cs.stop();

        if (ok)
            System.out.println("Test passed for protocol " + proto);

        System.out.println();
        return ok;
    }

    private static class TestListener implements NotificationListener {
        TestListener(LostListener ll) {
            this.lostListener = ll;
        }

        public void handleNotification(Notification n, Object h) {
            /* Connectors can handle unserializable notifications in
               one of two ways.  Either they can arrange for the
               client to get a NotSerializableException from its
               fetchNotifications call (RMI connector), or they can
               replace the unserializable notification by a
               JMXConnectionNotification.NOTIFS_LOST (JMXMP
               connector).  The former case is handled by code within
               the connector client which will end up sending a
               NOTIFS_LOST to our LostListener.  The logic here
               handles the latter case by converting it into the
               former.
             */
            if (n instanceof JMXConnectionNotification
                && n.getType().equals(JMXConnectionNotification.NOTIFS_LOST)) {
                lostListener.handleNotification(n, h);
                return;
            }

            synchronized (result) {
                if (!n.getType().equals("interesting")
                    || !n.getUserData().equals("known")) {
                    System.out.println("TestListener received strange notif: "
                                       + notificationString(n));
                    result.failed = true;
                    result.notifyAll();
                } else {
                    result.knownCount++;
                    if (result.knownCount == NNOTIFS)
                        result.notifyAll();
                }
            }
        }

        private LostListener lostListener;
    }

    private static class LostListener implements NotificationListener {
        public void handleNotification(Notification n, Object h) {
            synchronized (result) {
                handle(n, h);
            }
        }

        private void handle(Notification n, Object h) {
            if (!(n instanceof JMXConnectionNotification)) {
                System.out.println("LostListener received strange notif: " +
                                   notificationString(n));
                result.failed = true;
                result.notifyAll();
                return;
            }

            JMXConnectionNotification jn = (JMXConnectionNotification) n;
            if (!jn.getType().equals(jn.NOTIFS_LOST)) {
                System.out.println("Ignoring JMXConnectionNotification: " +
                                   notificationString(jn));
                return;
            }
            final String msg = jn.getMessage();
            if ((!msg.startsWith("Dropped ")
                 || !msg.endsWith("classes were missing locally"))
                && (!msg.startsWith("Not serializable: "))) {
                System.out.println("Surprising NOTIFS_LOST getMessage: " +
                                   msg);
            }
            if (!(jn.getUserData() instanceof Long)) {
                System.out.println("JMXConnectionNotification userData " +
                                   "not a Long: " + jn.getUserData());
                result.failed = true;
            } else {
                int lost = ((Long) jn.getUserData()).intValue();
                result.lostCount += lost;
                if (result.lostCount == NNOTIFS*2)
                    result.notifyAll();
            }
        }
    }

    private static class TestFilter implements NotificationFilter {
        public boolean isNotificationEnabled(Notification n) {
            return (n.getType().equals("interesting"));
        }
    }

    private static class Result {
        int knownCount, lostCount;
        boolean failed;
    }
    private static Result result;

    /* Send a bunch of notifications to exercise the logic to recover
       from unknown notification classes.  We have four kinds of
       notifications: "known" ones are of a class known to the client
       and which match its filters; "unknown" ones are of a class that
       match the client's filters but that the client can't load;
       "tricky" ones are unserializable; and "boring" notifications
       are of a class that the client knows but that doesn't match its
       filters.  We emit NNOTIFS notifications of each kind.  We do a
       random shuffle on these 4*NNOTIFS notifications so it is likely
       that we will cover the various different cases in the logic.

       Specifically, what we are testing here is the logic that
       recovers from a fetchNotifications request that gets a
       ClassNotFoundException.  Because the request can contain
       several notifications, the client doesn't know which of them
       generated the exception.  So it redoes a request that asks for
       just one notification.  We need to be sure that this works when
       that one notification is of an unknown class and when it is of
       a known class, and in both cases when there are filtered
       notifications that are skipped.

       We are also testing the behaviour in the server when it tries
       to include an unserializable notification in the response to a
       fetchNotifications, and in the client when that happens.

       If the test succeeds, the listener should receive the NNOTIFS
       "known" notifications, and the connection listener should
       receive an indication of NNOTIFS lost notifications
       representing the "unknown" notifications.

       We depend on some implementation-specific features here:

       1. The buffer size is sufficient to contain the 4*NNOTIFS
       notifications which are all sent at once, before the client
       gets a chance to start receiving them.

       2. When one or more notifications are dropped because they are
       of unknown classes, the NOTIFS_LOST notification contains a
       userData that is a Long with a count of the number dropped.

       3. If a notification is not serializable on the server, the
       client gets told about it somehow, rather than having it just
       dropped on the floor.  The somehow might be through an RMI
       exception, or it might be by the server replacing the
       unserializable notif by a JMXConnectionNotification.NOTIFS_LOST.
    */
    private static boolean notifyTest(JMXConnector client,
                                      MBeanServerConnection mbsc)
            throws Exception {
        System.out.println("Send notifications including unknown ones");
        result = new Result();
        LostListener ll = new LostListener();
        client.addConnectionNotificationListener(ll, null, null);
        TestListener nl = new TestListener(ll);
        mbsc.addNotificationListener(on, nl, new TestFilter(), null);
        mbsc.invoke(on, "sendNotifs", NO_OBJECTS, NO_STRINGS);

        // wait for the listeners to receive all their notifs
        // or to fail
        long deadline = System.currentTimeMillis() + 60000;
        long remain;
        while ((remain = deadline - System.currentTimeMillis()) >= 0) {
            synchronized (result) {
                if (result.failed
                    || (result.knownCount >= NNOTIFS
                        && result.lostCount >= NNOTIFS*2))
                    break;
                result.wait(remain);
            }
        }
        Thread.sleep(2);  // allow any spurious extra notifs to arrive
        if (result.failed) {
            System.out.println("TEST FAILS: Notification strangeness");
            return false;
        } else if (result.knownCount == NNOTIFS
                   && result.lostCount == NNOTIFS*2) {
            System.out.println("Success: received known notifications and " +
                               "got NOTIFS_LOST for unknown and " +
                               "unserializable ones");
            return true;
        } else if (result.knownCount >= NNOTIFS
                || result.lostCount >= NNOTIFS*2) {
            System.out.println("TEST FAILS: Received too many notifs: " +
                    "known=" + result.knownCount + "; lost=" + result.lostCount);
            return false;
        } else {
            System.out.println("TEST FAILS: Timed out without receiving " +
                               "all notifs: known=" + result.knownCount +
                               "; lost=" + result.lostCount);
            return false;
        }
    }

    public static interface TestMBean {
        public Object getClientUnknown() throws Exception;
        public void throwClientUnknown() throws Exception;
        public void setServerUnknown(Object o) throws Exception;
        public void useServerUnknown(Object o) throws Exception;
        public Object getUnserializable() throws Exception;
        public void setUnserializable(Object un) throws Exception;
        public void throwUnserializable() throws Exception;
        public void sendNotifs() throws Exception;
    }

    public static class Test extends NotificationBroadcasterSupport
            implements TestMBean {

        public Object getClientUnknown() {
            return clientUnknown;
        }

        public void throwClientUnknown() throws Exception {
            throw clientUnknown;
        }

        public void setServerUnknown(Object o) {
            throw new IllegalArgumentException("setServerUnknown succeeded "+
                                               "but should not have");
        }

        public void useServerUnknown(Object o) {
            throw new IllegalArgumentException("useServerUnknown succeeded "+
                                               "but should not have");
        }

        public Object getUnserializable() {
            return unserializableObject;
        }

        public void setUnserializable(Object un) {
            throw new IllegalArgumentException("setUnserializable succeeded " +
                                               "but should not have");
        }

        public void throwUnserializable() throws Exception {
            throw new Exception() {
                private Object unserializable = unserializableObject;
            };
        }

        public void sendNotifs() {
            /* We actually send the same four notification objects
               NNOTIFS times each.  This doesn't particularly matter,
               but note that the MBeanServer will replace "this" by
               the sender's ObjectName the first time.  Since that's
               always the same, no problem.  */
            Notification known =
                new Notification("interesting", this, 1L, 1L, "known");
            known.setUserData("known");
            Notification unknown =
                new Notification("interesting", this, 1L, 1L, "unknown");
            unknown.setUserData(clientUnknown);
            Notification boring =
                new Notification("boring", this, 1L, 1L, "boring");
            Notification tricky =
                new Notification("interesting", this, 1L, 1L, "tricky");
            tricky.setUserData(unserializableObject);

            // check that the tricky notif is indeed unserializable
            try {
                new ObjectOutputStream(new ByteArrayOutputStream())
                    .writeObject(tricky);
                throw new RuntimeException("TEST INCORRECT: tricky notif is " +
                                           "serializable");
            } catch (NotSerializableException e) {
                // OK: tricky notif is not serializable
            } catch (IOException e) {
                throw new RuntimeException("TEST INCORRECT: tricky notif " +
                                            "serialization check failed");
            }

            /* Now shuffle an imaginary deck of cards where K, U, T, and
               B (known, unknown, tricky, boring) each appear NNOTIFS times.
               We explicitly seed the random number generator so we
               can reproduce rare test failures if necessary.  We only
               use a StringBuffer so we can print the shuffled deck --
               otherwise we could just emit the notifications as the
               cards are placed.  */
            long seed = System.currentTimeMillis();
            System.out.println("Random number seed is " + seed);
            Random r = new Random(seed);
            int knownCount = NNOTIFS;   // remaining K cards
            int unknownCount = NNOTIFS; // remaining U cards
            int trickyCount = NNOTIFS;  // remaining T cards
            int boringCount = NNOTIFS;  // remaining B cards
            StringBuffer notifList = new StringBuffer();
            for (int i = NNOTIFS * 4; i > 0; i--) {
                int rand = r.nextInt(i);
                // use rand to pick a card from the remaining ones
                if ((rand -= knownCount) < 0) {
                    notifList.append('k');
                    knownCount--;
                } else if ((rand -= unknownCount) < 0) {
                    notifList.append('u');
                    unknownCount--;
                } else if ((rand -= trickyCount) < 0) {
                    notifList.append('t');
                    trickyCount--;
                } else {
                    notifList.append('b');
                    boringCount--;
                }
            }
            if (knownCount != 0 || unknownCount != 0
                || trickyCount != 0 || boringCount != 0) {
                throw new RuntimeException("TEST INCORRECT: Shuffle failed: " +
                                   "known=" + knownCount +" unknown=" +
                                   unknownCount + " tricky=" + trickyCount +
                                   " boring=" + boringCount +
                                   " deal=" + notifList);
            }
            String notifs = notifList.toString();
            System.out.println("Shuffle: " + notifs);
            for (int i = 0; i < NNOTIFS * 4; i++) {
                Notification n;
                switch (notifs.charAt(i)) {
                case 'k': n = known; break;
                case 'u': n = unknown; break;
                case 't': n = tricky; break;
                case 'b': n = boring; break;
                default:
                    throw new RuntimeException("TEST INCORRECT: Bad shuffle char: " +
                                               notifs.charAt(i));
                }
                sendNotification(n);
            }
        }
    }

    private static String notificationString(Notification n) {
        return n.getClass().getName() + "/" + n.getType() + " \"" +
            n.getMessage() + "\" <" + n.getUserData() + ">";
    }

    //
    private static class CNListener implements NotificationListener {
        public void handleNotification(Notification n, Object o) {
            if (n instanceof JMXConnectionNotification) {
                JMXConnectionNotification jn = (JMXConnectionNotification)n;
                if (JMXConnectionNotification.FAILED.equals(jn.getType())) {
                    failed = true;
                }
            }
        }

        public boolean failed = false;
    }
}
