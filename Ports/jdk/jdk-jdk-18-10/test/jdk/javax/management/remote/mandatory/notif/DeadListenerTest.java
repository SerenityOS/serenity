/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6957378
 * @summary Test that a listener can be removed remotely from an MBean that no longer exists.
 * @modules java.management.rmi/javax.management.remote.rmi:open
 *          java.management/com.sun.jmx.remote.internal:+open
 * @author Eamonn McManus
 * @run main/othervm -XX:+UsePerfData DeadListenerTest
 */

import com.sun.jmx.remote.internal.ServerNotifForwarder;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerDelegate;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnection;
import javax.management.remote.rmi.RMIConnectionImpl;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIJRMPServerImpl;
import javax.security.auth.Subject;

public class DeadListenerTest {
    public static void main(String[] args) throws Exception {
        final ObjectName delegateName = MBeanServerDelegate.DELEGATE_NAME;

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        Noddy mbean = new Noddy();
        ObjectName name = new ObjectName("d:k=v");
        mbs.registerMBean(mbean, name);

        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///");
        SnoopRMIServerImpl rmiServer = new SnoopRMIServerImpl();
        RMIConnectorServer cs = new RMIConnectorServer(url, null, rmiServer, mbs);
        cs.start();
        JMXServiceURL addr = cs.getAddress();
        assertTrue("No connections in new connector server", rmiServer.connections.isEmpty());

        JMXConnector cc = JMXConnectorFactory.connect(addr);
        MBeanServerConnection mbsc = cc.getMBeanServerConnection();
        assertTrue("One connection on server after client connect", rmiServer.connections.size() == 1);
        RMIConnectionImpl connection = rmiServer.connections.get(0);
        Method getServerNotifFwdM = RMIConnectionImpl.class.getDeclaredMethod("getServerNotifFwd");
        getServerNotifFwdM.setAccessible(true);
        ServerNotifForwarder serverNotifForwarder = (ServerNotifForwarder) getServerNotifFwdM.invoke(connection);
        Field listenerMapF = ServerNotifForwarder.class.getDeclaredField("listenerMap");
        listenerMapF.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<ObjectName, Set<?>> listenerMap = (Map<ObjectName, Set<?>>) listenerMapF.get(serverNotifForwarder);
        assertTrue("Server listenerMap initially empty", mapWithoutKey(listenerMap, delegateName).isEmpty());

        final AtomicInteger count1Val = new AtomicInteger();
        CountListener count1 = new CountListener(count1Val);
        mbsc.addNotificationListener(name, count1, null, null);
        WeakReference<CountListener> count1Ref = new WeakReference<>(count1);
        count1 = null;

        final AtomicInteger count2Val = new AtomicInteger();
        CountListener count2 = new CountListener(count2Val);
        NotificationFilterSupport dummyFilter = new NotificationFilterSupport();
        dummyFilter.enableType("");
        mbsc.addNotificationListener(name, count2, dummyFilter, "noddy");
        WeakReference<CountListener> count2Ref = new WeakReference<>(count2);
        count2 = null;

        assertTrue("One entry in listenerMap for two listeners on same MBean", mapWithoutKey(listenerMap, delegateName).size() == 1);
        Set<?> set = listenerMap.get(name);
        assertTrue("Set in listenerMap for MBean has two elements", set != null && set.size() == 2);

        assertTrue("Initial value of count1 == 0", count1Val.get() == 0);
        assertTrue("Initial value of count2 == 0", count2Val.get() == 0);

        Notification notif = new Notification("type", name, 0);

        mbean.sendNotification(notif);

        // Make sure notifs are working normally.
        while ((count1Val.get() != 1 || count2Val.get() != 1) ) {
            Thread.sleep(20);
        }
        assertTrue("New value of count1 == 1", count1Val.get() == 1);
        assertTrue("Initial value of count2 == 1", count2Val.get() == 1);

        // Make sure that removing a nonexistent listener from an existent MBean produces ListenerNotFoundException
        CountListener count3 = new CountListener();
        try {
            mbsc.removeNotificationListener(name, count3);
            assertTrue("Remove of nonexistent listener succeeded but should not have", false);
        } catch (ListenerNotFoundException e) {
            // OK: expected
        }

        // Make sure that removing a nonexistent listener from a nonexistent MBean produces ListenerNotFoundException
        ObjectName nonexistent = new ObjectName("foo:bar=baz");
        assertTrue("Nonexistent is nonexistent", !mbs.isRegistered(nonexistent));
        try {
            mbsc.removeNotificationListener(nonexistent, count3);
            assertTrue("Remove of listener from nonexistent MBean succeeded but should not have", false);
        } catch (ListenerNotFoundException e) {
            // OK: expected
        }

        // Now unregister our MBean, and check that notifs it sends no longer go anywhere.
        mbs.unregisterMBean(name);
        mbean.sendNotification(notif);
        Thread.sleep(200);

        assertTrue("New value of count1 == 1", count1Val.get() == 1);
        assertTrue("Initial value of count2 == 1", count2Val.get() == 1);

        // wait for the listener cleanup to take place upon processing notifications
        int countdown = 50; // waiting max. 5 secs
        while (countdown-- > 0 &&
                (count1Ref.get() != null ||
                 count2Ref.get() != null)) {
            System.gc();
            Thread.sleep(100);
            System.gc();
        }
        // listener has been removed or the wait has timed out

        assertTrue("count1 notification listener has not been cleaned up", count1Ref.get() == null);
        assertTrue("count2 notification listener has not been cleaned up", count2Ref.get() == null);

        // Check that there is no trace of the listeners any more in ServerNotifForwarder.listenerMap.
        // THIS DEPENDS ON JMX IMPLEMENTATION DETAILS.
        // If the JMX implementation changes, the code here may have to change too.
        Set<?> setForUnreg = listenerMap.get(name);
        assertTrue("No trace of unregistered MBean: " + setForUnreg, setForUnreg == null);
    }

    private static <K, V> Map<K, V> mapWithoutKey(Map<K, V> map, K key) {
        Map<K, V> copy = new HashMap<K, V>(map);
        copy.remove(key);
        return copy;
    }

    public static interface NoddyMBean {}

    public static class Noddy extends NotificationBroadcasterSupport implements NoddyMBean {}

    public static class CountListener implements NotificationListener {
        final AtomicInteger count;

        public CountListener(AtomicInteger i) {
            count = i;
        }

        public CountListener() {
            this.count = new AtomicInteger();
        }

        int count() {
            return count.get();
        }

        public void handleNotification(Notification notification, Object handback) {
            count.incrementAndGet();
        }
    }

    private static void assertTrue(String what, boolean cond) {
        if (!cond) {
            throw new AssertionError("Assertion failed: " + what);
        }
    }

    private static class SnoopRMIServerImpl extends RMIJRMPServerImpl {
        final List<RMIConnectionImpl> connections = new ArrayList<RMIConnectionImpl>();
        SnoopRMIServerImpl() throws IOException {
            super(0, null, null, null);
        }

        @Override
        protected RMIConnection makeClient(String id, Subject subject) throws IOException {
            RMIConnectionImpl conn = (RMIConnectionImpl) super.makeClient(id, subject);
            connections.add(conn);
            return conn;
        }
    }
}
