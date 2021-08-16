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
 * @summary Checks correct exception and error events from NotificationListener
 * @author Olivier Lagneau
 * @modules java.management.rmi
 * @library /lib/testlibrary
 * @compile Basic.java
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD MXBeanExceptionHandlingTest -timeForNotificationInSeconds 3
 */


import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.MBeanException;
import javax.management.MBeanServerDelegate;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.RuntimeErrorException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public class MXBeanExceptionHandlingTest implements NotificationListener {

    private static String BASIC_MXBEAN_CLASS_NAME = "Basic";

    private long timeForNotificationInSeconds = 3L;
    private int numOfNotifications = 2;
    private BlockingQueue<Notification> notifList = null;


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
        MXBeanExceptionHandlingTest test = new MXBeanExceptionHandlingTest();
        test.run(map);

    }

    protected void parseArgs(Map<String, Object> args) throws Exception {

        String arg = null;

        // Init timeForNotificationInSeconds
        // It is the maximum time in seconds we wait for a notification.
        arg = (String)args.get("-timeForNotificationInSeconds") ;
        if (arg != null) {
            timeForNotificationInSeconds = (new Long(arg)).longValue();
        }

    }

    public void run(Map<String, Object> args) {

        System.out.println("MXBeanExceptionHandlingTest::run: Start") ;
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
            System.out.println("Add me as notification listener");
            mbsc.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME,
                    this, null, null);
            System.out.println("---- OK\n") ;

            // ----
            System.out.println("Create and register the MBean");
            ObjectName objName = new ObjectName("sqe:type=Basic,protocol=rmi") ;
            mbsc.createMBean(BASIC_MXBEAN_CLASS_NAME, objName);
            System.out.println("---- OK\n") ;

            // ----
            System.out.println("Call method throwException on our MXBean");

            try {
                mbsc.invoke(objName, "throwException", null, null);
                errorCount++;
                System.out.println("(ERROR) Did not get awaited MBeanException") ;
            } catch (MBeanException mbe) {
                System.out.println("(OK) Got awaited MBeanException") ;
                Throwable cause = mbe.getCause();

                if ( cause instanceof java.lang.Exception ) {
                    System.out.println("(OK) Cause is of the right class") ;
                    String mess = cause.getMessage();

                    if ( mess.equals(Basic.EXCEPTION_MESSAGE ) ) {
                        System.out.println("(OK) Cause message is fine") ;
                    } else {
                        errorCount++;
                        System.out.println("(ERROR) Cause has message "
                                + cause.getMessage()
                                + " as we expect "
                                + Basic.EXCEPTION_MESSAGE) ;
                    }
                } else {
                    errorCount++;
                    System.out.println("(ERROR) Cause is of  class "
                            + cause.getClass().getName()
                            + " as we expect java.lang.Exception") ;
                }
            } catch (Exception e) {
                errorCount++;
                System.out.println("(ERROR) Did not get awaited MBeanException but "
                        + e) ;
                Utils.printThrowable(e, true);
            }
            System.out.println("---- DONE\n") ;

            // ----
            System.out.println("Call method throwError on our MXBean");

            try {
                mbsc.invoke(objName, "throwError", null, null);
                errorCount++;
                System.out.println("(ERROR) Did not get awaited RuntimeErrorException") ;
            } catch (RuntimeErrorException ree) {
                System.out.println("(OK) Got awaited RuntimeErrorException") ;
                Throwable cause = ree.getCause();

                if ( cause instanceof java.lang.InternalError ) {
                    System.out.println("(OK) Cause is of the right class") ;
                    String mess = cause.getMessage();

                    if ( mess.equals(Basic.EXCEPTION_MESSAGE ) ) {
                        System.out.println("(OK) Cause message is fine") ;
                    } else {
                        errorCount++;
                        System.out.println("(ERROR) Cause has message "
                                + cause.getMessage()
                                + " as we expect "
                                + Basic.EXCEPTION_MESSAGE) ;
                    }
                } else {
                    errorCount++;
                    System.out.println("(ERROR) Cause is of  class "
                            + cause.getClass().getName()
                            + " as we expect java.lang.InternalError") ;
                }
            } catch (Exception e) {
                errorCount++;
                System.out.println("(ERROR) Did not get awaited RuntimeErrorException but "
                        + e) ;
                Utils.printThrowable(e, true);
            }
            System.out.println("---- DONE\n") ;

            // ----
            System.out.println("Unregister the MBean");
            mbsc.unregisterMBean(objName);
            System.out.println("---- OK\n") ;

            Thread.sleep(timeForNotificationInSeconds * 1000);
            int numOfReceivedNotif = notifList.size();

            if ( numOfReceivedNotif == numOfNotifications ) {
                System.out.println("(OK) We received "
                        + numOfNotifications
                        + " Notifications") ;
            } else {
                errorCount++;
                System.out.println("(ERROR) We received "
                        + numOfReceivedNotif
                        + " Notifications in place of "
                        + numOfNotifications) ;
            }
        } catch(Exception e) {
            Utils.printThrowable(e, true) ;
            throw new RuntimeException(e);
        }

        if ( errorCount == 0 ) {
            System.out.println("MXBeanExceptionHandlingTest::run: Done without any error") ;
        } else {
            System.out.println("MXBeanExceptionHandlingTest::run: Done with "
                    + errorCount
                    + " error(s)") ;
            throw new RuntimeException("errorCount = " + errorCount);
        }
    }

    public void handleNotification(Notification notification, Object handback) {
        System.out.println("MXBeanExceptionHandlingTest::handleNotification: Received "
                + notification);
        notifList.add(notification);
    }

}
