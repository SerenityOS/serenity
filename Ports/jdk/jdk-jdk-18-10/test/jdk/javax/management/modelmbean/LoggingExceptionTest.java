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
 * @bug 6471865 6675768
 * @summary DescriptorSupport constructors throw IAE when traces are enabled;
 * RequiredModelMBean.addAttributeChangeNotificationListener throws exception
 * when traces enabled and no attributes.
 * @author Luis-Miguel Alventosa
 * @author Paul Cheeseman
 *
 * @modules java.logging
 *          java.management
 */

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.RequiredModelMBean;

public class LoggingExceptionTest {
    private static final String tests[] = new String[] {
        "DescriptorSupport()",
        "DescriptorSupport(int)",
        "DescriptorSupport(String)",
        "DescriptorSupport(String...)",
        "DescriptorSupport(String[], Object[])",
        "DescriptorSupport(DescriptorSupport)",
        "RequiredModelMBean.addAttributeChangeNotificationListener",
    };
    public static void main(String[] args) {
        Handler handler = new ConsoleHandler();
        Logger logger = Logger.getLogger("javax.management.modelmbean");
        logger.addHandler(handler);
        logger.setLevel(Level.FINEST);
        try {
            for (int i = 0; i < tests.length; i++) {
                System.out.println(">>> DescriptorSupportLoggingTest: Test Case " + i);
                DescriptorSupport ds;
                String msg = "Instantiate " + tests[i];
                System.out.println(msg);
                switch (i) {
                    case 0:
                        ds = new DescriptorSupport();
                        break;
                    case 1:
                        ds = new DescriptorSupport(10);
                        break;
                    case 2:
                        ds = new DescriptorSupport(new DescriptorSupport().toXMLString());
                        break;
                    case 3:
                        ds = new DescriptorSupport("name1=value1", "name2=value2");
                        break;
                    case 4:
                        ds = new DescriptorSupport(new String[] {"name"}, new Object[] {"value"});
                        break;
                    case 5:
                        ds = new DescriptorSupport(new DescriptorSupport());
                        break;
                    case 6:
                        RequiredModelMBean mbean = new RequiredModelMBean();
                        NotificationListener nl = new NotificationListener() {
                            public void handleNotification(Notification notification,
                                                           Object handback) {}
                        };
                        mbean.addAttributeChangeNotificationListener(nl, null, null);
                        break;
                    default:
                        throw new AssertionError();
                }
                System.out.println(msg + " OK");
            }
        } catch (Exception e) {
            System.out.println("Got unexpected exception = " + e);
            String msg = "Test FAILED!";
            System.out.println(msg);
            throw new IllegalArgumentException(msg);
        }
        System.out.println("Test PASSED!");
    }
}
