/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test that we can create proxies which are NotificationEmitters.
 * @bug 6411747
 * @author Daniel Fuchs
 *
 * @run clean NotificationEmitterProxy
 * @run build NotificationEmitterProxy
 * @run main NotificationEmitterProxy
 */

import java.lang.management.ManagementFactory;

import javax.management.*;
import javax.management.remote.*;
import javax.naming.NoPermissionException;

public class NotificationEmitterProxy {

    public static class Counter {
        int count;
        public synchronized int count() {
            count++;
            notifyAll();
            return count;
        }
        public synchronized int peek() {
            return count;
        }
        public synchronized int waitfor(int max, long timeout)
            throws InterruptedException {
            final long start = System.currentTimeMillis();
            while (count < max && timeout > 0) {
                final long rest = timeout -
                        (System.currentTimeMillis() - start);
                if (rest <= 0) break;
                wait(rest);
            }
            return count;
        }
    }

    public static class CounterListener
            implements NotificationListener {
        final private Counter counter;
        public CounterListener(Counter counter) {
            this.counter = counter;
        }
        public void handleNotification(Notification notification,
                        Object handback) {
               System.out.println("Received notif from " + handback +
                                  ":\n\t" + notification);
               counter.count();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("<<< Register for notification from a proxy");

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final ObjectName name = new ObjectName(":class=Simple");

        JMXServiceURL url = new JMXServiceURL("rmi", null, 0);
        final JMXConnectorServer server =
            JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
        server.start();
        url = server.getAddress();

        final JMXConnector client = JMXConnectorFactory.connect(url);

        final Counter counter = new Counter();
        final CounterListener listener = new CounterListener(counter);
        final Counter mxcounter = new Counter();
        final CounterListener mxlistener = new CounterListener(mxcounter);
        final NotificationFilterSupport filter =
                new NotificationFilterSupport();
        filter.enableType(MBeanServerNotification.REGISTRATION_NOTIFICATION);
        filter.enableType(MBeanServerNotification.UNREGISTRATION_NOTIFICATION);
        int registered = 0;
        try {
            final MBeanServerDelegateMBean delegate =
                JMX.newMBeanProxy(client.getMBeanServerConnection(),
                       MBeanServerDelegate.DELEGATE_NAME,
                       MBeanServerDelegateMBean.class,
                       true);

            NotificationEmitter emitter = (NotificationEmitter)delegate;
            emitter.addNotificationListener(listener,filter,"JMX.newMBeanProxy");
        } catch (Exception x) {
            throw new RuntimeException("Failed to register listener with "+
                    " JMX.newMBeanProxy: " + x, x);
        }

        try {
            final MBeanServerDelegateMBean delegate =
                MBeanServerInvocationHandler.newProxyInstance(mbs,
                       MBeanServerDelegate.DELEGATE_NAME,
                       MBeanServerDelegateMBean.class,
                       true);

            NotificationEmitter emitter = (NotificationEmitter)delegate;
            emitter.addNotificationListener(listener,filter,
                    "MBeanServerInvocationHandler.newProxyInstance");
        } catch (Exception x) {
            throw new RuntimeException("Failed to register listener with "+
                    " MBeanServerInvocationHandler.newProxyInstance: " + x, x);
        }

        System.out.println("<<< Register an MBean.");

        final Simple simple = new Simple();
        mbs.registerMBean(simple, name);
        registered++;

        SimpleMXBean simple0 =
           JMX.newMXBeanProxy(client.getMBeanServerConnection(),
                              name,
                              SimpleMXBean.class,
                              true);

        SimpleMXBean simple1 =
            JMX.newMXBeanProxy(mbs,
                               name,
                               SimpleMXBean.class,
                               false);

        final int expected = 2*registered;
        final int reg = counter.waitfor(expected,3000);
        if (reg != expected)
            throw new RuntimeException("Bad notification count: " + reg +
                    ", expected " +expected);
        System.out.println("Received expected "+reg+
                " notifs after registerMBean");

        ((NotificationEmitter)simple0)
            .addNotificationListener(mxlistener,null,name);
        simple1.equals("Are you a Wombat?");
        final int mxnotifs = mxcounter.waitfor(1,3000);
        if (mxnotifs != 1)
             throw new RuntimeException("Bad MXBean notification count: " +
                     mxnotifs);
        System.out.println("Received expected "+mxnotifs+
                " notifs from MXBean");

        mbs.unregisterMBean(name);
        final int unreg = counter.waitfor(expected+reg,3000);
        if (unreg != (expected+reg))
            throw new RuntimeException("Bad notification count: " + unreg +
                    ", expected " +expected+reg);
        System.out.println("Received expected "+(unreg-reg)+
                " notifs after unregisterMBean");
        System.out.println("Total notifs received: " + unreg);


    }

    public static interface Simplest {

    }

    public static interface SimpleMXBean extends Simplest {
        public String equals(String x);
    }

    private static class Simple extends NotificationBroadcasterSupport
            implements SimpleMXBean {
        public static final String NOTIF_TYPE = "simple.equals";
        private static long seq=0;
        private static synchronized long seq() { return ++seq; };
        public String equals(String x) {
            sendNotification(new Notification(NOTIF_TYPE,this,seq(),x));
            return x;
        }

    }



 }
