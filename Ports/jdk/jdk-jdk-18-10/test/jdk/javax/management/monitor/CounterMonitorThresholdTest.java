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
 * @bug 6229368 8025207
 * @summary Wrong threshold value in CounterMonitor with offset and modulus.
 * @author Luis-Miguel Alventosa
 *
 * @run clean CounterMonitorThresholdTest
 * @run build CounterMonitorThresholdTest
 * @run main CounterMonitorThresholdTest
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

public class CounterMonitorThresholdTest {

    // Offset = 1
    private static int counter1[]      = { 0, 1, 2, 3, 4, 4, 5, 5, 0, 1, 2, 3, 4, 5, 0, 1 };
    private static int derivedGauge1[] = { 0, 1, 2, 3, 4, 4, 5, 5, 0, 1, 2, 3, 4, 5, 0, 1 };
    private static int threshold1[]    = { 1, 2, 3, 4, 5, 5, 1, 1, 1, 2, 3, 4, 5, 1, 1, 2 };

    // Offset = 3
    private static int counter2[]      = { 0, 1, 2, 3, 3, 4, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1 };
    private static int derivedGauge2[] = { 0, 1, 2, 3, 3, 4, 4, 5, 0, 1, 2, 3, 4, 5, 0, 1 };
    private static int threshold2[]    = { 1, 4, 4, 4, 4, 1, 1, 1, 1, 4, 4, 4, 1, 1, 1, 4 };

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

    public static void runTest(int offset,
                               int counter[],
                               int derivedGauge[],
                               int threshold[]) throws Exception {
        // Retrieve the platform MBean server
        //
        System.out.println("\nRetrieve the platform MBean server");
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        String domain = mbs.getDefaultDomain();

        // Create and register TestMBean
        //
        ObjectName name =
            new ObjectName(domain +
                           ":type=" + Test.class.getName() +
                           ",offset=" + offset);
        mbs.createMBean(Test.class.getName(), name);
        TestMBean mbean = (TestMBean)
            MBeanServerInvocationHandler.newProxyInstance(
                mbs, name, TestMBean.class, false);

        // Create and register CounterMonitorMBean
        //
        ObjectName cmn =
            new ObjectName(domain +
                           ":type=" + CounterMonitor.class.getName() +
                           ",offset=" + offset);
        CounterMonitor m = new CounterMonitor();
        mbs.registerMBean(m, cmn);
        CounterMonitorMBean cm = (CounterMonitorMBean)
            MBeanServerInvocationHandler.newProxyInstance(
                mbs, cmn, CounterMonitorMBean.class, true);
        ((NotificationEmitter) cm).addNotificationListener(
            new Listener(), null, null);
        cm.addObservedObject(name);
        cm.setObservedAttribute("Counter");
        cm.setGranularityPeriod(100);
        cm.setInitThreshold(1);
        cm.setOffset(offset);
        cm.setModulus(5);
        cm.setNotify(true);

        // Start the monitor
        //
        System.out.println("\nStart monitoring...");
        cm.start();

        // Play with counter
        //
        for (int i = 0; i < counter.length; i++) {
            mbean.setCounter(counter[i]);
            System.out.println("\nCounter = " + mbean.getCounter());
            Integer derivedGaugeValue;
            // either pass or test timeout (killed by test harness)
            // see 8025207
            do {
                Thread.sleep(150);
                derivedGaugeValue = (Integer) cm.getDerivedGauge(name);
            } while (derivedGaugeValue.intValue() != derivedGauge[i]);

            Number thresholdValue = cm.getThreshold(name);
            System.out.println("Threshold = " + thresholdValue);
            if (thresholdValue.intValue() != threshold[i]) {
                System.out.println("Wrong threshold! Current value = " +
                    thresholdValue + " Expected value = " + threshold[i]);
                System.out.println("\nStop monitoring...");
                cm.stop();
                throw new IllegalArgumentException("wrong threshold");
            }
        }

        // Stop the monitor
        //
        System.out.println("\nStop monitoring...");
        cm.stop();
    }

    public static void main(String[] args) throws Exception {
        runTest(1, counter1, derivedGauge1, threshold1);
        runTest(3, counter2, derivedGauge2, threshold2);
    }
}
