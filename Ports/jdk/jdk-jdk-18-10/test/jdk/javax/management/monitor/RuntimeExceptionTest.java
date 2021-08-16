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
 * @bug 6200391
 * @summary Test that the jmx.monitor.error.runtime monitor notification
 *          is emitted when getAttribute throws RuntimeException.
 * @author Luis-Miguel Alventosa
 *
 * @run clean RuntimeExceptionTest MBeanServerBuilderImpl
 *            MBeanServerForwarderInvocationHandler
 * @run build RuntimeExceptionTest MBeanServerBuilderImpl
 *            MBeanServerForwarderInvocationHandler
 * @run main RuntimeExceptionTest
 */

import java.lang.reflect.Proxy;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.monitor.CounterMonitor;
import javax.management.monitor.GaugeMonitor;
import javax.management.monitor.MonitorNotification;
import javax.management.monitor.StringMonitor;

public class RuntimeExceptionTest implements NotificationListener {

    // MBean class
    public class ObservedObject implements ObservedObjectMBean {
        public Integer getIntegerAttribute() {
            return i;
        }
        public void setIntegerAttribute(Integer i) {
            this.i = i;
        }
        public String getStringAttribute() {
            return s;
        }
        public void setStringAttribute(String s) {
            this.s = s;
        }
        private Integer i = 1;
        private String s = "dummy";
    }

    // MBean interface
    public interface ObservedObjectMBean {
        public Integer getIntegerAttribute();
        public void setIntegerAttribute(Integer i);
        public String getStringAttribute();
        public void setStringAttribute(String s);
    }

    // Notification handler
    public void handleNotification(Notification notification, Object handback) {
        echo(">>> Received notification: " + notification);
        if (notification instanceof MonitorNotification) {
            String type = notification.getType();
            if (type.equals(MonitorNotification.RUNTIME_ERROR)) {
                MonitorNotification mn = (MonitorNotification) notification;
                echo("\tType: " + mn.getType());
                echo("\tTimeStamp: " + mn.getTimeStamp());
                echo("\tObservedObject: " + mn.getObservedObject());
                echo("\tObservedAttribute: " + mn.getObservedAttribute());
                echo("\tDerivedGauge: " + mn.getDerivedGauge());
                echo("\tTrigger: " + mn.getTrigger());

                synchronized (this) {
                    messageReceived = true;
                    notifyAll();
                }
            }
        }
    }

    /**
     * Update the counter and check for notifications
     */
    public int counterMonitorNotification() throws Exception {

        CounterMonitor counterMonitor = new CounterMonitor();
        try {
            // Create a new CounterMonitor MBean and add it to the MBeanServer.
            //
            echo(">>> CREATE a new CounterMonitor MBean");
            ObjectName counterMonitorName = new ObjectName(
                            domain + ":type=" + CounterMonitor.class.getName());
            server.registerMBean(counterMonitor, counterMonitorName);

            echo(">>> ADD a listener to the CounterMonitor");
            counterMonitor.addNotificationListener(this, null, null);

            //
            // MANAGEMENT OF A STANDARD MBEAN
            //

            echo(">>> SET the attributes of the CounterMonitor:");

            counterMonitor.addObservedObject(obsObjName);
            echo("\tATTRIBUTE \"ObservedObject\"    = " + obsObjName);

            counterMonitor.setObservedAttribute("IntegerAttribute");
            echo("\tATTRIBUTE \"ObservedAttribute\" = IntegerAttribute");

            counterMonitor.setNotify(false);
            echo("\tATTRIBUTE \"NotifyFlag\"        = false");

            Integer threshold = 2;
            counterMonitor.setInitThreshold(threshold);
            echo("\tATTRIBUTE \"Threshold\"         = " + threshold);

            int granularityperiod = 500;
            counterMonitor.setGranularityPeriod(granularityperiod);
            echo("\tATTRIBUTE \"GranularityPeriod\" = " + granularityperiod);

            echo(">>> START the CounterMonitor");
            counterMonitor.start();

            // Check if notification was received
            //
            doWait();
            if (messageReceived) {
                echo("\tOK: CounterMonitor got RUNTIME_ERROR notification!");
            } else {
                echo("\tKO: CounterMonitor did not get " +
                     "RUNTIME_ERROR notification!");
                return 1;
            }
        } finally {
            messageReceived = false;
            if (counterMonitor != null)
                counterMonitor.stop();
        }

        return 0;
    }

    /**
     * Update the gauge and check for notifications
     */
    public int gaugeMonitorNotification() throws Exception {

        GaugeMonitor gaugeMonitor = new GaugeMonitor();
        try {
            // Create a new GaugeMonitor MBean and add it to the MBeanServer.
            //
            echo(">>> CREATE a new GaugeMonitor MBean");
            ObjectName gaugeMonitorName = new ObjectName(
                            domain + ":type=" + GaugeMonitor.class.getName());
            server.registerMBean(gaugeMonitor, gaugeMonitorName);

            echo(">>> ADD a listener to the GaugeMonitor");
            gaugeMonitor.addNotificationListener(this, null, null);

            //
            // MANAGEMENT OF A STANDARD MBEAN
            //

            echo(">>> SET the attributes of the GaugeMonitor:");

            gaugeMonitor.addObservedObject(obsObjName);
            echo("\tATTRIBUTE \"ObservedObject\"    = " + obsObjName);

            gaugeMonitor.setObservedAttribute("IntegerAttribute");
            echo("\tATTRIBUTE \"ObservedAttribute\" = IntegerAttribute");

            gaugeMonitor.setNotifyLow(false);
            gaugeMonitor.setNotifyHigh(false);
            echo("\tATTRIBUTE \"Notify Low  Flag\"  = false");
            echo("\tATTRIBUTE \"Notify High Flag\"  = false");

            Integer highThreshold = 3, lowThreshold = 2;
            gaugeMonitor.setThresholds(highThreshold, lowThreshold);
            echo("\tATTRIBUTE \"Low  Threshold\"    = " + lowThreshold);
            echo("\tATTRIBUTE \"High Threshold\"    = " + highThreshold);

            int granularityperiod = 500;
            gaugeMonitor.setGranularityPeriod(granularityperiod);
            echo("\tATTRIBUTE \"GranularityPeriod\" = " + granularityperiod);

            echo(">>> START the GaugeMonitor");
            gaugeMonitor.start();

            // Check if notification was received
            //
            doWait();
            if (messageReceived) {
                echo("\tOK: GaugeMonitor got RUNTIME_ERROR notification!");
            } else {
                echo("\tKO: GaugeMonitor did not get " +
                     "RUNTIME_ERROR notification!");
                return 1;
            }
        } finally {
            messageReceived = false;
            if (gaugeMonitor != null)
                gaugeMonitor.stop();
        }

        return 0;
    }

    /**
     * Update the string and check for notifications
     */
    public int stringMonitorNotification() throws Exception {

        StringMonitor stringMonitor = new StringMonitor();
        try {
            // Create a new StringMonitor MBean and add it to the MBeanServer.
            //
            echo(">>> CREATE a new StringMonitor MBean");
            ObjectName stringMonitorName = new ObjectName(
                            domain + ":type=" + StringMonitor.class.getName());
            server.registerMBean(stringMonitor, stringMonitorName);

            echo(">>> ADD a listener to the StringMonitor");
            stringMonitor.addNotificationListener(this, null, null);

            //
            // MANAGEMENT OF A STANDARD MBEAN
            //

            echo(">>> SET the attributes of the StringMonitor:");

            stringMonitor.addObservedObject(obsObjName);
            echo("\tATTRIBUTE \"ObservedObject\"    = " + obsObjName);

            stringMonitor.setObservedAttribute("StringAttribute");
            echo("\tATTRIBUTE \"ObservedAttribute\" = StringAttribute");

            stringMonitor.setNotifyMatch(false);
            echo("\tATTRIBUTE \"NotifyMatch\"       = false");

            stringMonitor.setNotifyDiffer(false);
            echo("\tATTRIBUTE \"NotifyDiffer\"      = false");

            stringMonitor.setStringToCompare("dummy");
            echo("\tATTRIBUTE \"StringToCompare\"   = \"dummy\"");

            int granularityperiod = 500;
            stringMonitor.setGranularityPeriod(granularityperiod);
            echo("\tATTRIBUTE \"GranularityPeriod\" = " + granularityperiod);

            echo(">>> START the StringMonitor");
            stringMonitor.start();

            // Check if notification was received
            //
            doWait();
            if (messageReceived) {
                echo("\tOK: StringMonitor got RUNTIME_ERROR notification!");
            } else {
                echo("\tKO: StringMonitor did not get " +
                     "RUNTIME_ERROR notification!");
                return 1;
            }
        } finally {
            messageReceived = false;
            if (stringMonitor != null)
                stringMonitor.stop();
        }

        return 0;
    }

    /**
     * Test the monitor notifications.
     */
    public int monitorNotifications() throws Exception {

        server = MBeanServerFactory.newMBeanServer();

        MBeanServerForwarderInvocationHandler mbsfih =
            (MBeanServerForwarderInvocationHandler)
            Proxy.getInvocationHandler(server);

        mbsfih.setGetAttributeException(
            new RuntimeException("Test RuntimeException"));

        domain = server.getDefaultDomain();

        obsObjName = ObjectName.getInstance(domain + ":type=ObservedObject");
        server.registerMBean(new ObservedObject(), obsObjName);

        echo(">>> ----------------------------------------");
        int error = counterMonitorNotification();
        echo(">>> ----------------------------------------");
        error += gaugeMonitorNotification();
        echo(">>> ----------------------------------------");
        error += stringMonitorNotification();
        echo(">>> ----------------------------------------");
        return error;
    }

    /*
     * Print message
     */
    private static void echo(String message) {
        System.out.println(message);
    }

    /*
     * Standalone entry point.
     *
     * Run the test and report to stdout.
     */
    public static void main (String args[]) throws Exception {
        System.setProperty("javax.management.builder.initial",
                           MBeanServerBuilderImpl.class.getName());
        RuntimeExceptionTest test = new RuntimeExceptionTest();
        int error = test.monitorNotifications();
        if (error > 0) {
            echo(">>> Unhappy Bye, Bye!");
            throw new IllegalStateException("Test FAILED: Didn't get all " +
                                            "the notifications that were " +
                                            "expected by the test!");
        } else {
            echo(">>> Happy Bye, Bye!");
        }
    }

    /*
     * Wait messageReceived to be true
     */
    synchronized void doWait() {
        while (!messageReceived) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println("Got unexpected exception: " + e);
                e.printStackTrace();
                break;
            }
        }
    }

    // Flag to notify that a message has been received
    private volatile boolean messageReceived = false;

    private MBeanServer server;
    private ObjectName obsObjName;
    private String domain;
}
