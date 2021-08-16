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
 * @bug 6281446
 * @summary Test that the exception thrown by DynamicMBean.getMBeanInfo()
 *          keeps the init cause.
 * @author Luis-Miguel Alventosa
 *
 * @run clean GetMBeanInfoExceptionTest
 * @run build GetMBeanInfoExceptionTest
 * @run main GetMBeanInfoExceptionTest
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
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class GetMBeanInfoExceptionTest {

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
            throw new IllegalArgumentException("GetMBeanInfoExceptionTest");
        }
    }

    public static void main(String[] args) throws Exception {

        int error = 0;

        // Instantiate the MBean server
        //
        System.out.println("Create the MBean server");
        MBeanServer mbs = MBeanServerFactory.createMBeanServer();

        // Register the MBean
        //
        System.out.println("Create a TestDynamicMBean");
        TestDynamicMBean obj = new TestDynamicMBean();
        ObjectName n = new ObjectName("d:k=v");
        try {
            mbs.registerMBean(obj, n);
            System.out.println("Didn't get expected NotCompliantMBeanException");
            error++;
        } catch (NotCompliantMBeanException e) {
            boolean found = false;
            Throwable t = e.getCause();
            while (t != null) {
                if (t instanceof IllegalArgumentException &&
                    "GetMBeanInfoExceptionTest".equals(t.getMessage())) {
                    found = true;
                }
                t = t.getCause();
            }
            if (found) {
                System.out.println("Found expected IllegalArgumentException");
            } else {
                System.out.println("Didn't find expected IllegalArgumentException");
                error++;
            }
        } catch (Exception e) {
            System.out.println("Got " + e.getClass().getName() +
                "instead of expected NotCompliantMBeanException");
            error++;
        }

        if (error > 0) {
            System.out.println("Test failed");
            throw new IllegalArgumentException("Test failed");
        } else {
            System.out.println("Test passed");
        }
    }
}
