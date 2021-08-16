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
 * @bug 6190873
 * @summary Tests that thread creation can use a user-supplied Executor
 * @author Eamonn McManus
 *
 * @run clean ExecutorTest
 * @run build ExecutorTest
 * @run main ExecutorTest
 */

import java.lang.reflect.*;
import java.net.MalformedURLException;

import java.util.*;
import java.util.concurrent.*;

import javax.management.*;
import javax.management.remote.*;

/*
  When you create a JMXConnector client, you can supply a
  "fetch-notifications Executor", which is a
  java.util.concurrent.Executor that will be used each time the
  connector client wants to call RMIConnection.fetchNotifications.
  This is a hook that allows users to make that potentially-blocking
  call from within a thread pool or the like.  If you have very many
  connections, you can potentially share the work of
  fetchNotifications calls among a number of threads that is less than
  the number of connections, decreasing thread usage at the expense of
  increased latency.

  This test checks that the environment property does in fact work.
  It creates a connection without that property and ensures that
  notification sending does in fact work (with the default Executor).
  Then it creates a connection with the property set to an Executor
  that records how many times its execute method is invoked.
  Notifications are sent one by one and each time the test waits for
  the notification to be received.  This implies that
  fetchNotifications will be executed at least as many times as there
  are notifications.  Since each fetchNotifications call is supposed
  to happen as an Executor.execute call, if Executor.execute has been
  called fewer times then there were notifications, we know that the
  Executor is not being used correctly.
 */
public class ExecutorTest {
    private static final String EXECUTOR_PROPERTY =
        "jmx.remote.x.fetch.notifications.executor";
    private static final String NOTIF_TYPE = "test.type";

    public static void main(String[] args) throws Exception {
        String lastfail = null;
        for (String proto : new String[] {"rmi", "iiop", "jmxmp"}) {
            JMXServiceURL url = new JMXServiceURL(proto, null, 0);
            JMXConnectorServer cs;
            MBeanServer mbs = MBeanServerFactory.newMBeanServer();
            try {
                // Create server just to see if the protocol is supported
                cs = JMXConnectorServerFactory.newJMXConnectorServer(url,
                                                                     null,
                                                                     mbs);
            } catch (MalformedURLException e) {
                System.out.println();
                System.out.println("Ignoring protocol: " + proto);
                continue;
            }
            String fail;
            try {
                fail = test(proto);
                if (fail != null)
                    System.out.println("TEST FAILED: " + fail);
            } catch (Exception e) {
                e.printStackTrace(System.out);
                fail = e.toString();
            }
            if (lastfail == null)
                lastfail = fail;
        }
        if (lastfail == null)
            return;
        System.out.println();
        System.out.println("TEST FAILED");
        throw new Exception("Test failed: " + lastfail);
    }

    private static enum TestType {NO_EXECUTOR, NULL_EXECUTOR, COUNT_EXECUTOR};

    private static String test(String proto) throws Exception {
        System.out.println();
        System.out.println("TEST: " + proto);
        ClassLoader myClassLoader =
            CountInvocationHandler.class.getClassLoader();
        ExecutorService wrappedExecutor = Executors.newCachedThreadPool();
        CountInvocationHandler executorHandler =
            new CountInvocationHandler(wrappedExecutor);
        Executor countExecutor = (Executor)
            Proxy.newProxyInstance(myClassLoader,
                                   new Class<?>[] {Executor.class},
                                   executorHandler);

        JMXServiceURL url = new JMXServiceURL(proto, null, 0);

        for (TestType test : TestType.values()) {
            Map<String, Executor> env = new HashMap<String, Executor>();
            switch (test) {
            case NO_EXECUTOR:
                System.out.println("Test with no executor in env");
                break;
            case NULL_EXECUTOR:
                System.out.println("Test with null executor in env");
                env.put(EXECUTOR_PROPERTY, null);
                break;
            case COUNT_EXECUTOR:
                System.out.println("Test with non-null executor in env");
                env.put(EXECUTOR_PROPERTY, countExecutor);
                break;
            }
            MBeanServer mbs = MBeanServerFactory.newMBeanServer();
            ObjectName emitName = new ObjectName("blah:type=Emitter");
            mbs.registerMBean(new Emitter(), emitName);
            JMXConnectorServer cs =
                JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
            cs.start();
            JMXServiceURL addr = cs.getAddress();
            JMXConnector cc = JMXConnectorFactory.connect(addr, env);
            MBeanServerConnection mbsc = cc.getMBeanServerConnection();
            EmitterMBean emitter = (EmitterMBean)
                MBeanServerInvocationHandler.newProxyInstance(mbsc,
                                                              emitName,
                                                              EmitterMBean.class,
                                                              false);
            SemaphoreListener listener = new SemaphoreListener();
            NotificationFilterSupport filter = new NotificationFilterSupport();
            filter.enableType(NOTIF_TYPE);
            mbsc.addNotificationListener(emitName, listener, filter, null);
            final int NOTIF_COUNT = 10;
            for (int i = 0; i < NOTIF_COUNT; i++) {
                emitter.emit();
                listener.await();
            }
            Thread.sleep(1);
            listener.checkUnavailable();
            System.out.println("Got notifications");
            cc.close();
            cs.stop();
            if (test == TestType.COUNT_EXECUTOR) {
                Method m = Executor.class.getMethod("execute", Runnable.class);
                Integer count = executorHandler.methodCount.get(m);
                if (count == null || count < NOTIF_COUNT)
                    return "Incorrect method count for execute: " + count;
                System.out.println("Executor was called enough times");
            }
        }

        wrappedExecutor.shutdown();
        return null;
    }

    /* Simple MBean that sends a notification every time we ask it to.  */
    public static interface EmitterMBean {
        public void emit();
    }

    public static class Emitter
            extends NotificationBroadcasterSupport implements EmitterMBean {
        public void emit() {
            sendNotification(new Notification(NOTIF_TYPE, this, seq++));
        }

        private long seq = 1;
    }

    /* Simple NotificationListener that allows you to wait until a
       notification has been received.  Since it uses a semaphore, you
       can wait either before or after the notification has in fact
       been received and it will work in either case.  */
    private static class SemaphoreListener implements NotificationListener {
        void await() throws InterruptedException {
            semaphore.acquire();
        }

        /* Ensure no extra notifications were received.  If we can acquire
           the semaphore, that means its release() method was called more
           times than its acquire() method, which means there were too
           many notifications.  */
        void checkUnavailable() throws Exception {
            if (semaphore.tryAcquire())
                throw new Exception("Got extra notifications!");
        }

        public void handleNotification(Notification n, Object h) {
            semaphore.release();
        }

        private final Semaphore semaphore = new Semaphore(0);
    }

    /* Generic InvocationHandler that forwards all methods to a wrapped
       object, but counts for each method how many times it was invoked.  */
    private static class CountInvocationHandler implements InvocationHandler {
        final Map<Method, Integer> methodCount =
            new HashMap<Method, Integer>();
        private final Object wrapped;

        public CountInvocationHandler(Object wrapped) {
            this.wrapped = wrapped;
        }

        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            synchronized (methodCount) {
                Integer count = methodCount.get(method);
                if (count == null)
                    count = 0;
                methodCount.put(method, count + 1);
            }
            return method.invoke(wrapped, (Object[]) args);
        }
    }
}
