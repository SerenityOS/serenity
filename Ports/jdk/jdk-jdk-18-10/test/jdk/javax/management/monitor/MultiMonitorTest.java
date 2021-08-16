/*
 * Copyright (c) 2004, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4984057
 * @key randomness
 * @summary Test that monitors can sample a large number of attributes
 * @author Eamonn McManus
 *
 * @run clean MultiMonitorTest
 * @run build MultiMonitorTest
 * @run main MultiMonitorTest
 */

import java.util.*;
import javax.management.*;
import javax.management.monitor.*;

/* We create N MBeans and three monitors, one for each different
   monitor type.  Each monitor monitors a single attribute in each of
   the N MBeans.  We arrange for the trigger condition to be
   satisfied, so the listener we register on each monitor should get N
   notifications.  */
public class MultiMonitorTest {
    static final int N = 100;
    static final ObjectName[] mbeanNames = new ObjectName[N];
    static final Monitored[] monitored = new Monitored[N];
    static final int COUNTER_THRESHOLD = 1000;
    static final int OVER_COUNTER_THRESHOLD = 2000;
    static final double GAUGE_THRESHOLD = 1000.0;
    static final double OVER_GAUGE_THRESHOLD = 2000.0;
    static final String STRING_TO_COMPARE = "chou";
    static final String DIFFERENT_STRING = "chevre";

    public static void main(String[] args) throws Exception {
        System.out.println("Test that monitors can sample a large " +
                           "number of attributes");

        final MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        for (int i = 0; i < N; i++) {
            mbeanNames[i] = new ObjectName(":type=Monitored,instance=" + i);
            monitored[i] = new Monitored();
            mbs.registerMBean(monitored[i], mbeanNames[i]);
        }
        final ObjectName counterMonitor =
            new ObjectName(":type=CounterMonitor");
        final ObjectName gaugeMonitor =
            new ObjectName(":type=GaugeMonitor");
        final ObjectName stringMonitor =
            new ObjectName(":type=StringMonitor");
        final ObjectName[] monitorNames =
            new ObjectName[] {counterMonitor, gaugeMonitor, stringMonitor};
        final String[] attrNames =
            new String[] {"CounterValue", "GaugeValue", "StringValue"};
        mbs.createMBean(CounterMonitor.class.getName(), counterMonitor);
        mbs.createMBean(GaugeMonitor.class.getName(), gaugeMonitor);
        mbs.createMBean(StringMonitor.class.getName(), stringMonitor);
        final CounterMonitorMBean counterProxy = (CounterMonitorMBean)
            MBeanServerInvocationHandler
            .newProxyInstance(mbs, counterMonitor, CounterMonitorMBean.class,
                              false);
        final GaugeMonitorMBean gaugeProxy = (GaugeMonitorMBean)
            MBeanServerInvocationHandler
            .newProxyInstance(mbs, gaugeMonitor, GaugeMonitorMBean.class,
                              false);
        final StringMonitorMBean stringProxy = (StringMonitorMBean)
            MBeanServerInvocationHandler
            .newProxyInstance(mbs, stringMonitor, StringMonitorMBean.class,
                              false);
        final MonitorMBean[] proxies = new MonitorMBean[] {
            counterProxy, gaugeProxy, stringProxy,
        };
        for (int i = 0; i < 3; i++) {
            proxies[i].setGranularityPeriod(1);
            proxies[i].setObservedAttribute(attrNames[i]);
            for (int j = 0; j < N; j++)
                proxies[i].addObservedObject(mbeanNames[j]);
        }

        final CountListener[] listeners = new CountListener[] {
            new CountListener(), new CountListener(), new CountListener()
        };
        for (int i = 0; i < 3; i++) {
            mbs.addNotificationListener(monitorNames[i], listeners[i],
                                        null, null);
        }

        counterProxy.setInitThreshold(new Integer(COUNTER_THRESHOLD));
        counterProxy.setNotify(true);
        gaugeProxy.setThresholds(new Double(GAUGE_THRESHOLD), new Double(0.0));
        gaugeProxy.setNotifyHigh(true);
        stringProxy.setStringToCompare(STRING_TO_COMPARE);
        stringProxy.setNotifyDiffer(true);

        // A couple of granularity periods to detect bad behaviour
        Thread.sleep(2);

        System.out.println("Checking for all listeners to be 0");
        if (!listenersAreAll(0, listeners)) {
            System.out.println("TEST FAILED: listeners not all 0");
            System.exit(1);
        }

        for (int i = 0; i < 3; i++)
            proxies[i].start();

        System.out.println("Waiting for listeners to all : " + N);
        int iterations = 0;
        while (!listenersAreAll(N, listeners)) {
            Thread.sleep(500);

            if (++iterations == 10) {
               for (int i = 0; i < listeners.length; i++) {
                   System.out.print(" " + listeners[i].getCount());
               }
               System.out.println();
               iterations = 0;
            }
        }

        for (int i = 0; i < 3; i++) {
            proxies[i].stop();
            for (int j = 0; j < N; j++)
                proxies[i].removeObservedObject(mbeanNames[j]);
            ObjectName[] observed = proxies[i].getObservedObjects();
            if (observed.length != 0) {
                System.out.println("TEST FAILED: not all observed objects " +
                                   "removed: " + Arrays.asList(observed));
                System.exit(1);
            }
        }

        System.out.println("Test passed");
    }

    public static interface MonitoredMBean {
        public int getCounterValue();
        public double getGaugeValue();
        public String getStringValue();
    }

    public static class Monitored implements MonitoredMBean {
        /* We give a small random number of normal readings (possibly
           zero) before giving a reading that provokes a
           notification.  */
        private int okCounter = randomInt(5);
        private int okGauge = randomInt(5);
        private int okString = randomInt(5);

        public synchronized int getCounterValue() {
            if (--okCounter >= 0)
                return 0;
            else
                return OVER_COUNTER_THRESHOLD;
        }

        public synchronized double getGaugeValue() {
            if (--okGauge >= 0)
                return 0.0;
            else
                return OVER_GAUGE_THRESHOLD;
        }

        public synchronized String getStringValue() {
            if (--okString >= 0)
                return STRING_TO_COMPARE;
            else
                return DIFFERENT_STRING;
        }
    }

    public static class CountListener implements NotificationListener {
        private int count;

        public synchronized void handleNotification(Notification n, Object h) {
            if (!(n instanceof MonitorNotification)) {
                System.out.println("TEST FAILED: bad notif: " +
                                   n.getClass().getName());
                System.exit(1);
            }
            if (n.getType().indexOf("error") >= 0) {
                System.out.println("TEST FAILED: error notif: " + n.getType());
                System.exit(1);
            }
            count++;
        }

        public synchronized int getCount() {
            return count;
        }
    }

    private static boolean listenersAreAll(int n, CountListener[] listeners) {
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i].getCount() != n)
                return false;
        }
        return true;
    }

    private static final Random random = new Random();
    static synchronized int randomInt(int n) {
        return random.nextInt(n);
    }
}
