/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6689505
 * @summary Checks that MBeanServerNotification.toString contains the
 *          MBean name.
 * @author Daniel Fuchs
 * @modules java.management/com.sun.jmx.mbeanserver
 * @compile MBeanServerNotificationTest.java
 * @run main MBeanServerNotificationTest
 */

import com.sun.jmx.mbeanserver.Util;
import javax.management.*;
import java.util.concurrent.*;

public class MBeanServerNotificationTest {
    final static String[] names = {
        ":type=Wombat", "wombat:type=Wombat",null,
    };
    public static void main(String[] args) throws Exception {
        System.out.println("Test that MBeanServerNotification.toString " +
                "contains the name of the MBean being registered " +
                "or unregistered.");
        int failures = 0;
        final MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        for (String str:names) {
            try {
                final ObjectName name = (str==null)?null:new ObjectName(str);
                failures+=test(mbs, name, name!=null);
            } catch(Exception x) {
                x.printStackTrace(System.out);
                System.out.println("Test failed for: "+str);
                failures++;
            }
        }
        if (failures == 0)
            System.out.println("Test passed");
        else {
            System.out.println("TEST FAILED: " + failures + " failure(s)");
            System.exit(1);
        }
    }

    private static enum Registration {
        REGISTER(MBeanServerNotification.REGISTRATION_NOTIFICATION),
        UNREGISTER(MBeanServerNotification.UNREGISTRATION_NOTIFICATION);
        final String type;
        private Registration(String type) {this.type = type;}
        public int test(MBeanServerNotification n, ObjectName name) {
            int failures = 0;
            System.out.println("Testing: "+n);
            if (!n.toString().endsWith("[type="+type+
                "][message="+n.getMessage()+
                "][mbeanName="+name+"]")) {
                System.err.println("Test failed for "+ type+
                   " ["+name+"]: "+n);
                failures++;
            }
            return failures;
        }
        public MBeanServerNotification create(ObjectName name) {
            return new MBeanServerNotification(type,
                MBeanServerDelegate.DELEGATE_NAME, next(), name);
        }
        private static long next = 0;
        private static synchronized long next() {return next++;}

    }

    private static int test(MBeanServer mbs, ObjectName name,
                            boolean register)
            throws Exception {
        System.out.println("--------" + name + "--------");

        int failures = 0;
        for (Registration reg : Registration.values()) {
            failures = reg.test(reg.create(name), name);
        }
        if (!register) return failures;

        final ArrayBlockingQueue<Notification> queue =
                new ArrayBlockingQueue<Notification>(10);
        final NotificationListener listener = new NotificationListener() {
            public void handleNotification(Notification notification,
                    Object handback) {
                try {
                    queue.put(notification);
                } catch(Exception x) {
                    x.printStackTrace(System.out);
                }
            }
        };
        mbs.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME,
                listener, null, name);
        final ObjectInstance oi = mbs.registerMBean(new Wombat(), name);
        try {
            failures+=Registration.REGISTER.test((MBeanServerNotification)
                queue.poll(2, TimeUnit.SECONDS), oi.getObjectName());
        } finally {
            mbs.unregisterMBean(oi.getObjectName());
            failures+=Registration.UNREGISTER.test((MBeanServerNotification)
                queue.poll(2, TimeUnit.SECONDS), oi.getObjectName());
        }
        return failures;
    }

    public static interface WombatMBean {}
    public static class Wombat implements WombatMBean {}

}
