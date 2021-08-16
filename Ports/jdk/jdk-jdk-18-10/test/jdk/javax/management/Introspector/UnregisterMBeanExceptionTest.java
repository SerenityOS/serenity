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
 * @bug 6328682
 * @summary Test that MBeanServer.unregisterMBean() correctly unregisters
 *          the supplied MBean although DynamicMBean.getMBeanInfo() throws
 *          a runtime exception.
 * @author Luis-Miguel Alventosa
 *
 * @run clean UnregisterMBeanExceptionTest
 * @run build UnregisterMBeanExceptionTest
 * @run main UnregisterMBeanExceptionTest
 */

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

public class UnregisterMBeanExceptionTest {

    public static class TestDynamicMBean implements DynamicMBean {

        public Object getAttribute(String attribute) throws
            AttributeNotFoundException,
            MBeanException,
            ReflectionException {
            return null;
        }

        public void setAttribute(Attribute attribute) throws
            AttributeNotFoundException,
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

        public Object invoke(String op, Object params[], String sign[]) throws
            MBeanException,
            ReflectionException {
            return null;
        }

        public MBeanInfo getMBeanInfo() {
            if (throwException)
                throw new RuntimeException("UnregisterMBeanExceptionTest");
            else
                return new MBeanInfo(this.getClass().getName(), "Test",
                                     null, null, null, null);
        }

        public boolean throwException;
    }

    public static void main(String[] args) throws Exception {

        // Instantiate the MBean server
        //
        System.out.println("Create the MBean server");
        MBeanServer mbs = MBeanServerFactory.createMBeanServer();

        // Register the MBean
        //
        System.out.println("Create a TestDynamicMBean");
        TestDynamicMBean obj = new TestDynamicMBean();
        ObjectName n = new ObjectName("d:k=v");
        System.out.println("Register a TestDynamicMBean");
        mbs.registerMBean(obj, n);
        obj.throwException = true;
        System.out.println("Unregister a TestDynamicMBean");
        try {
            mbs.unregisterMBean(n);
        } catch (Exception e) {
            throw new IllegalArgumentException("Test failed", e);
        }
        boolean isRegistered = mbs.isRegistered(n);
        System.out.println("Is MBean Registered? " + isRegistered);

        if (isRegistered) {
            throw new IllegalArgumentException(
                "Test failed: the MBean is still registered");
        } else {
            System.out.println("Test passed");
        }
    }
}
