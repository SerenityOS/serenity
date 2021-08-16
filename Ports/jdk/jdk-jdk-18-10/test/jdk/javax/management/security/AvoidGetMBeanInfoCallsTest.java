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
 * @bug 6331783
 * @summary Test that MBeanServer.queryNames doesn't call getMBeanInfo on every
 *          resultant MBean when there is no security manager installed.
 * @author Luis-Miguel Alventosa
 *
 * @run clean AvoidGetMBeanInfoCallsTest
 * @run build AvoidGetMBeanInfoCallsTest
 * @run main AvoidGetMBeanInfoCallsTest
 */

import java.util.Set;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class AvoidGetMBeanInfoCallsTest {

    /**
     * Test DynamicMBean class
     */
    public static class Test implements DynamicMBean {

        public Object getAttribute(String attribute)
            throws AttributeNotFoundException,
                   MBeanException,
                   ReflectionException {
            return null;
        }

        public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException,
                   InvalidAttributeValueException,
                   MBeanException,
                   ReflectionException {
        }

        public AttributeList getAttributes(String[] attributes) {
            return null;
        }

        public AttributeList setAttributes(AttributeList attributes) {
            return null;
        }

        public Object invoke(String actionName,
                             Object params[],
                             String signature[])
            throws MBeanException,
                   ReflectionException {
            return null;
        }

        public MBeanInfo getMBeanInfo() {
            entered = true;
            return new MBeanInfo(Test.class.getName(),
                                 "Test description",
                                 null, null, null, null);
        }

        public boolean entered;
    }

    /*
     * Print message
     */
    private static void echo(String message) {
        System.out.println(message);
    }

    /**
     * Standalone entry point.
     *
     * Run the test and report to stdout.
     */
    public static void main(String args[]) throws Exception {

        echo(">>> Create MBeanServer");
        MBeanServer server = MBeanServerFactory.newMBeanServer();

        echo(">>> Default Domain: " + server.getDefaultDomain());

        echo(">>> Create and register Test MBean");
        Test mbean = new Test();
        ObjectName name = ObjectName.getInstance(":type=Test");
        server.registerMBean(mbean, name);

        echo(">>> Set entered flag to false in Test MBean");
        mbean.entered = false;

        echo(">>> Query Names:");
        Set<ObjectName> names = server.queryNames(null, null);
        for (ObjectName on : names) {
            echo("\t" + on.toString());
        }

        echo(">>> Entered flag = " + mbean.entered);

        if (mbean.entered) {
            echo(">>> Test FAILED!");
            throw new IllegalArgumentException("getMBeanInfo got called");
        } else {
            echo(">>> Test PASSED!");
        }
    }
}
