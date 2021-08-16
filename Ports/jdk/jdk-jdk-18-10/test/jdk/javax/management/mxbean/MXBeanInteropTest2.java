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
 * @summary Checks access to test MXBean
 * @author Olivier Lagneau
 * @modules java.management.rmi
 * @library /lib/testlibrary
 * @compile Basic.java
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD MXBeanInteropTest2
 */

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public class MXBeanInteropTest2 {

    private static String BASIC_MXBEAN_CLASS_NAME = "Basic";

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
        MXBeanInteropTest2 test = new MXBeanInteropTest2();
        test.run(map);

    }

    public void run(Map<String, Object> args) {

        System.out.println("MXBeanInteropTest2::run: Start") ;
        int errorCount = 0 ;

        try {
            // JMX MbeanServer used inside single VM as if remote.
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

            JMXServiceURL url = new JMXServiceURL("rmi", null, 0);
            JMXConnectorServer cs =
                JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
            cs.start();

            JMXServiceURL addr = cs.getAddress();
            JMXConnector cc = JMXConnectorFactory.connect(addr);
            MBeanServerConnection mbsc = cc.getMBeanServerConnection();

            // Prints all MBeans whatever the domain is.
            printMBeans(mbsc) ;

            // Call test body
            errorCount += doBasicMXBeanTest(mbsc) ;

            // Terminate the JMX Client
            cc.close();

        } catch(Exception e) {
            Utils.printThrowable(e, true) ;
            throw new RuntimeException(e);
        }

        if ( errorCount == 0 ) {
            System.out.println("MXBeanInteropTest2::run: Done without any error") ;
        } else {
            System.out.println("MXBeanInteropTest2::run: Done with "
                    + errorCount
                    + " error(s)") ;
            throw new RuntimeException("errorCount = " + errorCount);
        }
    }


    /**
     * Prints all MBeans whatever the domain is.
     */
    private static void printMBeans(MBeanServerConnection mbsc) throws Exception {
        Set<ObjectName> set = mbsc.queryNames(null, null);
        System.out.println("---- MBeans found :");

        for (Iterator<ObjectName> iter = set.iterator(); iter.hasNext(); ) {
            System.out.println(iter.next().toString());
        }

        System.out.println("\n") ;
    }


    private final int doBasicMXBeanTest(MBeanServerConnection mbsc) {
        int errorCount = 0 ;
        System.out.println("---- doBasicMXBeanTest") ;

        try {
            ObjectName objName =
                    new ObjectName("sqe:type=BasicMXBean") ;
            mbsc.createMBean(BASIC_MXBEAN_CLASS_NAME, objName);
            MBeanInfo mbInfo = mbsc.getMBeanInfo(objName);
            printMBeanInfo(mbInfo);
            System.out.println("---- OK\n") ;
            System.out.println("getMBeanInfo\t\t"
                    + mbInfo);
            System.out.println("---- OK\n") ;

            System.out.println("Check mxbean field in the MBeanInfo");
            String mxbeanField =
                    (String)mbInfo.getDescriptor().getFieldValue(JMX.MXBEAN_FIELD);

            if ( mxbeanField == null || ! mxbeanField.equals("true")) {
                System.out.println("---- ERROR : Improper mxbean field value "
                        + mxbeanField);
                errorCount++;
            }
            System.out.println("---- OK\n") ;

            System.out.println("Set attribute ObjectNameAtt");
            Attribute att = new Attribute("ObjectNameAtt", objName);
            mbsc.setAttribute(objName, att);
            ObjectName value =
                    (ObjectName)mbsc.getAttribute(objName, "ObjectNameAtt");

            if ( ! value.equals(objName) ) {
                errorCount++;
                System.out.println("---- ERROR : setAttribute failed, got "
                        + value
                        + " while expecting "
                        + objName);
            }
            System.out.println("---- OK\n") ;

            System.out.println("Call operation doNothing");
            mbsc.invoke(objName,  "doNothing", null, null);
            System.out.println("---- OK\n") ;

            System.out.println("Call operation getWeather");
            Object weather = mbsc.invoke(objName,
                    "getWeather",
                    new Object[]{Boolean.TRUE},
                    new String[]{"boolean"});
            System.out.println("Weather is " + weather);
            System.out.println("---- OK\n") ;
        } catch (Exception e) {
            Utils.printThrowable(e, true) ;
            errorCount++ ;
            System.out.println("---- ERROR\n") ;
        }

        return errorCount ;
    }

    private void printMBeanInfo(MBeanInfo mbInfo) {
        System.out.println("Description " + mbInfo.getDescription());

        for (MBeanConstructorInfo ctor : mbInfo.getConstructors()) {
            System.out.println("Constructor " + ctor.getName());
        }

        for (MBeanAttributeInfo att : mbInfo.getAttributes()) {
            System.out.println("Attribute " + att.getName()
            + " [" + att.getType() + "]");
        }

        for (MBeanOperationInfo oper : mbInfo.getOperations()) {
            System.out.println("Operation " + oper.getName());
        }

        for (MBeanNotificationInfo notif : mbInfo.getNotifications()) {
            System.out.println("Notification " + notif.getName());
        }
    }
}
