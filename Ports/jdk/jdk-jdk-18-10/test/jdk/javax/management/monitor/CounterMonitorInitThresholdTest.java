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
 * @bug 6333528
 * @summary Check that the initial threshold is properly used by the observed
 *          objects added before the counter monitor is started as well as by
 *          the observed objects which are added once the monitor is started.
 * @author Luis-Miguel Alventosa
 *
 * @run clean CounterMonitorInitThresholdTest
 * @run build CounterMonitorInitThresholdTest
 * @run main CounterMonitorInitThresholdTest
 */

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.monitor.CounterMonitor;
import javax.management.monitor.CounterMonitorMBean;
import javax.management.monitor.MonitorNotification;

public class CounterMonitorInitThresholdTest {

    public interface TestMBean {
        public int getCounter();
        public void setCounter(int count);
    }

    public static class Test implements TestMBean {
        public int getCounter() {
            return count;
        }
        public void setCounter(int count) {
            this.count = count;
        }
        private int count = 0;
    }

    public static class Listener implements NotificationListener {
        public void handleNotification(Notification n, Object hb) {
            System.out.println("\tReceived notification: " + n.getType());
            if (n instanceof MonitorNotification) {
                MonitorNotification mn = (MonitorNotification) n;
                System.out.println("\tSource: " +
                    mn.getSource());
                System.out.println("\tType: " +
                    mn.getType());
                System.out.println("\tTimeStamp: " +
                    mn.getTimeStamp());
                System.out.println("\tObservedObject: " +
                    mn.getObservedObject());
                System.out.println("\tObservedAttribute: " +
                    mn.getObservedAttribute());
                System.out.println("\tDerivedGauge: " +
                    mn.getDerivedGauge());
                System.out.println("\tTrigger: " +
                    mn.getTrigger());
            }
        }
    }

    public static void runTest() throws Exception {
        // Retrieve the platform MBean server
        //
        System.out.println("\nRetrieve the platform MBean server");
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        String domain = mbs.getDefaultDomain();

        // Create and register TestMBeans
        //
        ObjectName name1 =
            new ObjectName(domain +
                           ":type=" + Test.class.getName() +
                           ",name=1");
        mbs.createMBean(Test.class.getName(), name1);
        TestMBean mbean1 = (TestMBean)
            MBeanServerInvocationHandler.newProxyInstance(
                mbs, name1, TestMBean.class, false);
        ObjectName name2 =
            new ObjectName(domain +
                           ":type=" + Test.class.getName() +
                           ",name=2");
        mbs.createMBean(Test.class.getName(), name2);
        TestMBean mbean2 = (TestMBean)
            MBeanServerInvocationHandler.newProxyInstance(
                mbs, name2, TestMBean.class, false);

        // Create and register CounterMonitorMBean
        //
        ObjectName cmn =
            new ObjectName(domain +
                           ":type=" + CounterMonitor.class.getName());
        CounterMonitor m = new CounterMonitor();
        mbs.registerMBean(m, cmn);
        CounterMonitorMBean cm = (CounterMonitorMBean)
            MBeanServerInvocationHandler.newProxyInstance(
                mbs, cmn, CounterMonitorMBean.class, true);
        ((NotificationEmitter) cm).addNotificationListener(
            new Listener(), null, null);
        cm.setObservedAttribute("Counter");
        cm.setGranularityPeriod(100);
        cm.setInitThreshold(3);
        cm.setNotify(true);

        // Add observed object name1
        //
        System.out.println("\nObservedObject \"" + name1 +
            "\" registered before starting the monitor");
        cm.addObservedObject(name1);

        // Start the monitor
        //
        System.out.println("\nStart monitoring...");
        cm.start();

        // Play with counter for name1
        //
        System.out.println("\nTest ObservedObject \"" + name1 + "\"");
        for (int i = 0; i < 4; i++) {
            mbean1.setCounter(i);
            System.out.println("\nCounter = " + mbean1.getCounter());
            Thread.sleep(300);
            Number thresholdValue = cm.getThreshold(name1);
            System.out.println("Threshold = " + thresholdValue);
            if (thresholdValue.intValue() != 3) {
                System.out.println("Wrong threshold! Current value = " +
                    thresholdValue + " Expected value = 3");
                System.out.println("\nStop monitoring...");
                cm.stop();
                throw new IllegalArgumentException("wrong threshold");
            }
            Thread.sleep(300);
        }

        // Add observed object name2
        //
        System.out.println("\nObservedObject \"" + name2 +
            "\" registered after starting the monitor");
        cm.addObservedObject(name2);

        // Play with counter for name2
        //
        System.out.println("\nTest ObservedObject \"" + name2 + "\"");
        for (int i = 0; i < 4; i++) {
            mbean2.setCounter(i);
            System.out.println("\nCounter = " + mbean2.getCounter());
            Thread.sleep(300);
            Number thresholdValue = cm.getThreshold(name2);
            System.out.println("Threshold = " + thresholdValue);
            if (thresholdValue.intValue() != 3) {
                System.out.println("Wrong threshold! Current value = " +
                    thresholdValue + " Expected value = 3");
                System.out.println("\nStop monitoring...");
                cm.stop();
                throw new IllegalArgumentException("wrong threshold");
            }
            Thread.sleep(300);
        }

        // Stop the monitor
        //
        System.out.println("\nStop monitoring...");
        cm.stop();
    }

    public static void main(String[] args) throws Exception {
        runTest();
    }
}
