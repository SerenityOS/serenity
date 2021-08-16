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
 * @bug 6239400
 * @summary Tests NotificationBuffer doesn't hold locks when adding listeners,
 *  if test times out then deadlock is suspected.
 * @author Eamonn McManus
 *
 * @run clean NotificationBufferDeadlockTest
 * @run build NotificationBufferDeadlockTest
 * @run main NotificationBufferDeadlockTest
 */

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import javax.management.*;
import javax.management.remote.*;

/*
 * Regression test for a rare but not unheard-of deadlock condition in
 * the notification buffer support for connector servers.
 * See bug 6239400 for the description of the bug and the example that
 * showed it up.
 *
 * Here we test that, when the connector server adds its listener to an
 * MBean, it is not holding a lock that would prevent another thread from
 * emitting a notification (from that MBean or another one).  This is
 * important, because we don't know how user MBeans might implement
 * NotificationBroadcaster.addNotificationListener, and in particular we
 * can't be sure that the method is well-behaved and can never do a
 * blocking operation, such as attempting to acquire a lock that is also
 * acquired when notifications are emitted.
 *
 * The test creates a special MBean whose addNotificationListener method
 * does the standard addNotificationListener logic inherited
 * from NotificationBroadcasterSupport, then
 * creates another thread that emits a notification from the same MBean.
 * The addNotificationListener method waits for this thread to complete.
 * If the notification buffer logic is incorrect, then emitting the
 * notification will attempt to acquire the lock on the buffer, but that
 * lock is being held by the thread that called addNotificationListener,
 * so there will be deadlock.
 *
 * We use this DeadlockMBean several times.  First, we create one and then
 * add a remote listener to it.  The first time you add a remote listener
 * through a connector server, the connector server adds its own listener
 * to all NotificationBroadcaster MBeans.  If it holds a lock while doing
 * this, we will see deadlock.
 *
 * Then we create a second DeadlockMBean.  When a new MBean is created that
 * is a NotificationBroadcaster, the connector server adds its listener to
 * that MBean too.  Again if it holds a lock while doing this, we will see
 * deadlock.
 *
 * Finally, we do some magic with MBeanServerForwarders so that while
 * queryNames is running (to find MBeans to which listeners must be added)
 * we will create new MBeans.  This tests that this tricky situation is
 * handled correctly.  It also tests the queryNames that is run when the
 * notification buffer is being destroyed (to remove the listeners).
 *
 * We cause all of our test MBeans to emit exactly one notification and
 * check that we have received exactly one notification from each MBean.
 * If the logic for adding the notification buffer's listener is incorrect
 * we could remove zero or two notifications from an MBean.
 */
public class NotificationBufferDeadlockTest {
    public static void main(String[] args) throws Exception {
        System.out.println("Check no deadlock if notif sent while initial " +
                           "remote listeners being added");
        final String[] protos = {"rmi", "iiop", "jmxmp"};
        for (String p : protos) {
            try {
                test(p);
            } catch (Exception e) {
                System.out.println("TEST FAILED: GOT EXCEPTION:");
                e.printStackTrace(System.out);
                failure = e.toString();
            }
        }
        if (failure == null)
            return;
        else
            throw new Exception("TEST FAILED: " + failure);
    }

    private static void test(String proto) throws Exception {
        System.out.println("Testing protocol " + proto);
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName testName = newName();
        DeadlockTest test = new DeadlockTest();
        mbs.registerMBean(test, testName);
        JMXServiceURL url = new JMXServiceURL("service:jmx:" + proto + ":///");
        JMXConnectorServer cs;
        try {
            cs =
                JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
        } catch (MalformedURLException e) {
            System.out.println("...protocol not supported, ignoring");
            return;
        }

        MBeanServerForwarder createDuringQueryForwarder = (MBeanServerForwarder)
            Proxy.newProxyInstance(new Object() {}.getClass().getClassLoader(),
                                   new Class[] {MBeanServerForwarder.class},
                                   new CreateDuringQueryInvocationHandler());
        cs.setMBeanServerForwarder(createDuringQueryForwarder);
        cs.start();
        JMXServiceURL addr = cs.getAddress();
        JMXConnector cc = JMXConnectorFactory.connect(addr);
        MBeanServerConnection mbsc = cc.getMBeanServerConnection();
        try {
            String fail = test(mbsc, testName);
            if (fail != null)
                System.out.println("FAILED: " + fail);
            failure = fail;
        } finally {
            cc.close();
            cs.stop();
        }
    }

    private static String test(MBeanServerConnection mbsc,
                               ObjectName testName) throws Exception {

        NotificationListener dummyListener = new NotificationListener() {
            public void handleNotification(Notification n, Object h) {
            }
        };
        thisFailure = null;
        mbsc.addNotificationListener(testName, dummyListener, null, null);
        if (thisFailure != null)
            return thisFailure;
        ObjectName newName = newName();
        mbsc.createMBean(DeadlockTest.class.getName(), newName);
        if (thisFailure != null)
            return thisFailure;
        Set<ObjectName> names =
            mbsc.queryNames(new ObjectName("d:type=DeadlockTest,*"), null);
        System.out.printf("...found %d test MBeans\n", names.size());

        sources.clear();
        countListener = new MyListener(names.size());

        for (ObjectName name : names)
            mbsc.addNotificationListener(name, countListener, null, null);
        if (thisFailure != null)
            return thisFailure;
        for (ObjectName name : names)
            mbsc.invoke(name, "send", null, null);

        countListener.waiting();

        if (!sources.containsAll(names))
            return "missing names: " + sources;
        return thisFailure;
    }

    public static interface DeadlockTestMBean {
        public void send();
    }

    public static class DeadlockTest extends NotificationBroadcasterSupport
            implements DeadlockTestMBean {
        @Override
        public void addNotificationListener(NotificationListener listener,
                                            NotificationFilter filter,
                                            Object handback) {
            super.addNotificationListener(listener, filter, handback);
            Thread t = new Thread() {
                @Override
                public void run() {
                    Notification n =
                        new Notification("type", DeadlockTest.this, 0L);
                    DeadlockTest.this.sendNotification(n);
                }
            };
            t.start();
            System.out.println("DeadlockTest-addNotificationListener waiting for the sending thread to die...");
            try {
                t.join(); //if times out here then deadlock is suspected
                System.out.println("DeadlockTest-addNotificationListener OK.");
            } catch (Exception e) {
                thisFailure = "Join exception: " + e;
            }
        }

        public void send() {
            sendNotification(new Notification(TESTING_TYPE, DeadlockTest.this, 1L));
        }
    }

    private static class CreateDuringQueryInvocationHandler
            implements InvocationHandler {
        public Object invoke(Object proxy, Method m, Object[] args)
                throws Throwable {
            if (m.getName().equals("setMBeanServer")) {
                mbs = (MBeanServer) args[0];
                return null;
            }
            createMBeanIfQuery(m);
            Object ret = m.invoke(mbs, args);
            createMBeanIfQuery(m);
            return ret;
        }

        private void createMBeanIfQuery(Method m) throws InterruptedException {
            if (m.getName().equals("queryNames")) {
                Thread t = new Thread() {
                    public void run() {
                        try {
                            mbs.createMBean(DeadlockTest.class.getName(),
                                            newName());
                        } catch (Exception e) {
                            e.printStackTrace();
                            thisFailure = e.toString();
                        }
                    }
                };
                t.start();
                System.out.println("CreateDuringQueryInvocationHandler-createMBeanIfQuery waiting for the creating thread to die...");
                t.join();  // if times out here then deadlock is suspected
                System.out.println("CreateDuringQueryInvocationHandler-createMBeanIfQuery OK");
            }
        }

        private MBeanServer mbs;
    }

    private static synchronized ObjectName newName() {
        try {
            return new ObjectName("d:type=DeadlockTest,instance=" +
                                  ++nextNameIndex);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("bad ObjectName", e);
        }
    }

    private static class MyListener implements NotificationListener {
        public MyListener(int waitNB) {
            count = new CountDownLatch(waitNB);
        }

        public void handleNotification(Notification n, Object h) {
            System.out.println("MyListener got: " + n.getSource() + " " + n.getType());

            if (TESTING_TYPE.equals(n.getType())) {
                sources.add((ObjectName) n.getSource());
                count.countDown();
            }
        }

        public void waiting() throws InterruptedException {
            System.out.println("MyListener-waiting ...");
            count.await(); // if times out here then deadlock is suspected
            System.out.println("MyListener-waiting done!");
        }

        private final CountDownLatch count;
    }

    static String thisFailure;
    static String failure;
    static int nextNameIndex;

    private static MyListener countListener;
    private static final List<ObjectName> sources = new Vector();

    private static final String TESTING_TYPE = "testing_type";
}
