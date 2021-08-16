/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8141591
 * @summary Tests if notifications are received after executor is shutdown
 * @author Harsha Wardhana B
 *
 * @run clean ExecutorShutdownTest
 * @run build ExecutorShutdownTest
 * @run main ExecutorShutdownTest
 */
import java.util.*;
import java.util.concurrent.*;
import javax.management.*;
import javax.management.remote.*;

/*
  When you create a JMXConnector client, you can supply a
  "fetch-notifications Executor", which is a
  java.util.concurrent.Executor that will be used each time the
  connector client wants to call RMIConnection.fetchNotifications.
  If such executor is not supplies, the connector client will fallback
  on default LinearExecutor. This test checks if user supplied executor
  is shutdown abruptly, LinearExecutor is used to handle notifications.
 */
public class ExecutorShutdownTest {

    private static final String EXECUTOR_PROPERTY
            = "jmx.remote.x.fetch.notifications.executor";
    private static final String NOTIF_TYPE = "test.type";

    public static void main(String[] args) throws Exception {

        // Start JMXConnector Server
        JMXServiceURL url = new JMXServiceURL("rmi", null, 0);
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName emitName = new ObjectName("blah:type=Emitter");
        mbs.registerMBean(new Emitter(), emitName);
        JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url,
                null,
                mbs);
        cs.start();

        // Create executor to provide to JMXConnector client
        ExecutorService executor = Executors.newCachedThreadPool();
        Map<String, Executor> env = new HashMap<>();
        env.put(EXECUTOR_PROPERTY, executor);
        JMXServiceURL addr = cs.getAddress();

        try (JMXConnector cc = JMXConnectorFactory.connect(addr, env)) {
            MBeanServerConnection mbsc = cc.getMBeanServerConnection();
            EmitterMBean emitter = (EmitterMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc,
                    emitName,
                    EmitterMBean.class,
                    false);
            SemaphoreListener listener = new SemaphoreListener();
            NotificationFilterSupport filter = new NotificationFilterSupport();
            filter.enableType(NOTIF_TYPE);
            mbsc.addNotificationListener(emitName, listener, filter, null);

            final int NOTIF_COUNT = 3;
            for (int i = 0; i < NOTIF_COUNT; i++) {
                emitter.emit();
                listener.await();
            }
            Thread.sleep(1);
            listener.checkUnavailable();
            System.out.println("Got notifications with client provided executor");

            // After shutting down executor, notifications are handled by linear executor
            executor.shutdown();
            for (int i = 0; i < NOTIF_COUNT; i++) {
                emitter.emit();
                listener.await();
            }
            Thread.sleep(1);
            listener.checkUnavailable();
            System.out.println("Got notifications with linear executor");
        }
        cs.stop();
        System.out.println("TEST PASSED !!!");
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
            if (semaphore.tryAcquire()) {
                throw new Exception("Got extra notifications!");
            }
        }

        public void handleNotification(Notification n, Object h) {
            semaphore.release();
        }

        private final Semaphore semaphore = new Semaphore(0);
    }
}
