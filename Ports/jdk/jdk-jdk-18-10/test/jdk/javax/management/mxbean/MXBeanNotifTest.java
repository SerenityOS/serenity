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
 * @bug 8058865
 * @summary Checks MXBean proper registration both as its implementation class and interface
 * @author Olivier Lagneau
 * @modules java.management.rmi
 * @library /lib/testlibrary
 * @compile Basic.java
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD MXBeanNotifTest -numOfNotifications 239 -timeForNotificationInSeconds 4
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import java.lang.management.ManagementFactory;

import javax.management.Attribute;
import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanServer;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import javax.management.openmbean.CompositeType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

public class MXBeanNotifTest implements NotificationListener {

    private static String BASIC_MXBEAN_CLASS_NAME = "Basic";
    private static String BASIC_MXBEAN_INTERFACE_NAME = "BasicMXBean";

    private long timeForNotificationInSeconds = 3L;
    private int numOfNotifications = 1;
    private BlockingQueue<Notification> notifList = null;
    private int numOfNotifDescriptorElements = 13;

    /*
     * First Debug properties and arguments are collect in expected
     * map  (argName, value) format, then calls original test's run method.
     */
    public static void main(String args[]) throws Exception {

        System.out.println("=================================================");

        // Parses parameters
        Utils.parseDebugProperties();
        Map<String, Object> map = Utils.parseParameters(args) ;

        // Run test
        MXBeanNotifTest test = new MXBeanNotifTest();
        test.run(map);

    }

    protected void parseArgs(Map<String, Object> args) throws Exception {

        String arg = null;

        // Init numOfNotifications
        // It is the number of notifications we should trigger and check.
        arg = (String)args.get("-numOfNotifications") ;
        if (arg != null) {
            numOfNotifications = (new Integer(arg)).intValue();
        }

        // Init timeForNotificationInSeconds
        // It is the maximum time in seconds we wait for each notification.
        arg = (String)args.get("-timeForEachNotificationInSeconds") ;
        if (arg != null) {
            timeForNotificationInSeconds = (new Long(arg)).longValue();
        }

    }

    public void run(Map<String, Object> args) {

        System.out.println("MXBeanNotifTest::run: Start") ;
        int errorCount = 0 ;

        try {
            parseArgs(args);
            notifList = new ArrayBlockingQueue<Notification>(numOfNotifications);

            // JMX MbeanServer used inside single VM as if remote.
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

            JMXServiceURL url = new JMXServiceURL("rmi", null, 0);
            JMXConnectorServer cs =
                JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
            cs.start();

            JMXServiceURL addr = cs.getAddress();
            JMXConnector cc = JMXConnectorFactory.connect(addr);
            MBeanServerConnection mbsc = cc.getMBeanServerConnection();

            // ----
            System.out.println("MXBeanNotifTest::run: Create and register the MBean");
            ObjectName objName = new ObjectName("sqe:type=Basic,protocol=rmi") ;
            mbsc.createMBean(BASIC_MXBEAN_CLASS_NAME, objName);
            System.out.println("---- OK\n") ;

            // ----
            System.out.println("MXBeanNotifTest::run: Add me as notification listener");
            mbsc.addNotificationListener(objName, this, null, null);

            // ----
            System.out.println("MXBeanNotifTest::run: Retrieve the Descriptor"
                    + " that should be in MBeanNotificationInfo");
            TabularData tabData =
                (TabularData)mbsc.getAttribute(objName, "NotifDescriptorAsMapAtt");
            Map<String, String> descrMap = new HashMap<>();

            for (Iterator<?> it = tabData.values().iterator(); it.hasNext(); ) {
                CompositeData compData = (CompositeData)it.next();
                descrMap.put((String)compData.get("key"),
                        (String)compData.get("value"));
            }

            Descriptor refNotifDescriptor = new ImmutableDescriptor(descrMap);
            System.out.println("---- OK\n") ;

            // ----
            // Because the MBean holding the targeted attribute is MXBean, we
            // should use for the setAttribute a converted form for the
            // attribute value as described by the MXBean mapping rules.
            // This explains all that lovely stuff for creating a
            // TabularDataSupport.
            //
            // WARNING : the MBeanInfo of the MXBean used on opposite side
            // is computed when the MBean is registered.
            // It means the Descriptor considered for the MBeanNotificationInfo
            // is not the one we set in the lines below, it is too late.
            // However, we check that set is harmless when we check
            // the MBeanNotificationInfo.
            //
            System.out.println("MXBeanNotifTest::run: Set a Map<String, String>"
                    + " attribute");
            String typeName =
                    "java.util.Map<java.lang.String,java.lang.String>";
            String[] keyValue = new String[] {"key", "value"};
            OpenType<?>[] openTypes =
                    new OpenType<?>[] {SimpleType.STRING, SimpleType.STRING};
            CompositeType rowType = new CompositeType(typeName, typeName,
                    keyValue, keyValue, openTypes);
            TabularType tabType = new TabularType(typeName, typeName,
                    rowType, new String[]{"key"});
            TabularDataSupport convertedDescrMap =
                    new TabularDataSupport(tabType);

            for (int i = 0; i < numOfNotifDescriptorElements; i++) {
                Object[] descrValue = {"field" + i, "value" + i};
                CompositeData data =
                        new CompositeDataSupport(rowType, keyValue, descrValue);
                convertedDescrMap.put(data);
            }

            Attribute descrAtt =
                    new Attribute("NotifDescriptorAsMapAtt", convertedDescrMap);
            mbsc.setAttribute(objName, descrAtt);
            System.out.println("---- OK\n") ;

            // ----
            System.out.println("MXBeanNotifTest::run: Compare the Descriptor from"
                    + " the MBeanNotificationInfo against a reference");
            MBeanInfo mbInfo = mbsc.getMBeanInfo(objName);
            errorCount += checkMBeanInfo(mbInfo, refNotifDescriptor);
            System.out.println("---- DONE\n") ;

            // ----
            System.out.println("Check isInstanceOf(Basic)");

            if ( ! mbsc.isInstanceOf(objName, BASIC_MXBEAN_CLASS_NAME) ) {
                errorCount++;
                System.out.println("---- ERROR isInstanceOf returned false\n") ;
            } else {
                System.out.println("---- OK\n") ;
            }

            // ----
            System.out.println("Check isInstanceOf(BasicMXBean)");

            if ( ! mbsc.isInstanceOf(objName, BASIC_MXBEAN_INTERFACE_NAME) ) {
                errorCount++;
                System.out.println("---- ERROR isInstanceOf returned false\n") ;
            } else {
                System.out.println("---- OK\n") ;
            }

            // ----
            System.out.println("MXBeanNotifTest::run: Ask for "
                    + numOfNotifications + " notification(s)");
            Object[] sendNotifParam = new Object[1];
            String[] sendNotifSig = new String[]{"java.lang.String"};

            for (int i = 0; i < numOfNotifications; i++) {
                // Select which type of notification we ask for
                if ( i % 2 == 0 ) {
                    sendNotifParam[0] = Basic.NOTIF_TYPE_0;
                } else {
                    sendNotifParam[0] = Basic.NOTIF_TYPE_1;
                }

                // Trigger notification emission
                mbsc.invoke(objName,
                        "sendNotification",
                        sendNotifParam,
                        sendNotifSig);

                // Wait for it then check it when it comes early enough
                Notification notif =
                        notifList.poll(timeForNotificationInSeconds,
                        TimeUnit.SECONDS) ;
                // The very first notification is likely to come in slower than
                // all the others. Because that test isn't targeting the speed
                // notifications are delivered with, we prefer to secure it.
                if (i == 0 && notif == null) {
                    System.out.println("MXBeanNotifTest::run: Wait extra "
                            + timeForNotificationInSeconds + " second(s) the "
                            + " very first notification");
                    notif = notifList.poll(timeForNotificationInSeconds,
                            TimeUnit.SECONDS);
                }

                if ( notif == null ) {
                    errorCount++;
                    System.out.println("---- ERROR No notification received"
                            + " within allocated " + timeForNotificationInSeconds
                            + " second(s) !");
                } else {
                    errorCount +=
                            checkNotification(notif,
                            (String)sendNotifParam[0],
                            Basic.NOTIFICATION_MESSAGE,
                            objName);
                }
            }

            int toc = 0;
            while ( notifList.size() < 2 && toc < 10 ) {
                Thread.sleep(499);
                toc++;
            }
            System.out.println("---- DONE\n") ;
        } catch(Exception e) {
            Utils.printThrowable(e, true) ;
            throw new RuntimeException(e);
        }

        if ( errorCount == 0 ) {
            System.out.println("MXBeanNotifTest::run: Done without any error") ;
        } else {
            System.out.println("MXBeanNotifTest::run: Done with "
                    + errorCount
                    + " error(s)") ;
            throw new RuntimeException("errorCount = " + errorCount);
        }
    }


    private int checkMBeanInfo(MBeanInfo mbi, Descriptor refDescr) {
        MBeanNotificationInfo[] notifsInfo = mbi.getNotifications();
        int res = 0;

        for (MBeanNotificationInfo mbni : notifsInfo) {
            if ( mbni.getDescriptor().equals(refDescr) ) {
                System.out.println("(OK)");
            } else {
                System.out.println("(ERROR) Descriptor of the notification is "
                        + mbni.getDescriptor()
                        + " as we expect "
                        + refDescr);
                res++;
            }
        }

        return res;
    }


    private int checkNotification(Notification notif,
            String refType,
            String refMessage,
            ObjectName refSource) {
        int res = 0;

        Utils.debug(Utils.DEBUG_VERBOSE,
                "\t getSource " + notif.getSource());
        Utils.debug(Utils.DEBUG_VERBOSE,
                "\t getMessage " + notif.getMessage());
        Utils.debug(Utils.DEBUG_VERBOSE,
                "\t getSequenceNumber " + notif.getSequenceNumber());
        Utils.debug(Utils.DEBUG_VERBOSE,
                "\t getTimeStamp " + notif.getTimeStamp());
        Utils.debug(Utils.DEBUG_VERBOSE,
                "\t getType " + notif.getType());
        Utils.debug(Utils.DEBUG_VERBOSE,
                "\t getUserData " + notif.getUserData());

        if ( ! notif.getType().equals(refType) ) {
            res++;
            System.out.println("(ERROR) Type is not "
                    + refType + " in notification\n" + notif);
        } else {
            if ( notif.getType().equals(Basic.NOTIF_TYPE_0)
            && ! (notif instanceof javax.management.Notification) ) {
                res++;
                System.out.println("(ERROR) Notification is not instance of "
                        + " javax.management.Notification but rather "
                        + notif.getClass().getName());
            } else if ( notif.getType().equals(Basic.NOTIF_TYPE_1)
            && ! (notif instanceof SqeNotification) ) {
                res++;
                System.out.println("(ERROR) Notification is not instance of "
                        + " javasoft.sqe.jmx.share.SqeNotification but rather "
                        + notif.getClass().getName());
            }
        }

        if ( ! notif.getMessage().equals(refMessage) ) {
            res++;
            System.out.println("(ERROR) Message is not "
                    + refMessage + " in notification\n" + notif);
        }

        if ( ! notif.getSource().equals(refSource) ) {
            res++;
            System.out.println("(ERROR) Source is not "
                    + refSource + " in notification\n" + notif);
        }

        return res;
    }

    public void handleNotification(Notification notification, Object handback) {
        Utils.debug(Utils.DEBUG_VERBOSE,
                "MXBeanNotifTest::handleNotification: Received "
                + notification);
        notifList.add(notification);
    }

}
