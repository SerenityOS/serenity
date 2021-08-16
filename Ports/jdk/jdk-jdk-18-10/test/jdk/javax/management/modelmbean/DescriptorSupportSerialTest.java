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
 * @bug 6332962
 * @summary Test that DescriptorSupport does not serialize targetObject
 * @author Eamonn McManus
 *
 * @run clean DescriptorSupportSerialTest
 * @run build DescriptorSupportSerialTest
 * @run main DescriptorSupportSerialTest
 */

import java.lang.reflect.Method;
import javax.management.*;
import javax.management.remote.*;
import javax.management.modelmbean.*;

public class DescriptorSupportSerialTest {
    public static void main(String[] args) throws Exception {
        if (java.io.Serializable.class.isAssignableFrom(Thread.class))
            throw new Exception("TEST BAD: Thread is Serializable!");
        Method getName = Thread.class.getMethod("getName");
        Descriptor d = new DescriptorSupport();
        d.setField("name", "getName");
        d.setField("descriptorType", "operation");
        d.setField("TARGETObject", Thread.currentThread());
        d.setField("foo", "bar");
        ModelMBeanOperationInfo getNameInfo =
            new ModelMBeanOperationInfo("Get name", getName, d);
        ModelMBeanInfo mmbi =
            new ModelMBeanInfoSupport(Thread.class.getName(),
                                      "Thread!",
                                      null,  // no attributes
                                      null,  // no constructors
                                      new ModelMBeanOperationInfo[] {getNameInfo},
                                      null);  // no notifications
        ModelMBean mmb = new RequiredModelMBean(mmbi);

        ObjectName on = new ObjectName("d:type=Thread");
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        mbs.registerMBean(mmb, on);

        JMXServiceURL url = new JMXServiceURL("rmi", null, 0);
        JMXConnectorServer cs =
            JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
        cs.start();
        JMXServiceURL addr = cs.getAddress();
        JMXConnector cc = JMXConnectorFactory.connect(addr);
        MBeanServerConnection mbsc = cc.getMBeanServerConnection();

        try {
            test(mbsc, on);
            System.out.println("TEST PASSED");
        } finally {
            cc.close();
            cs.stop();
        }
    }

    private static void test(MBeanServerConnection mbsc, ObjectName on)
            throws Exception {
        ModelMBeanInfo mmbi2 = (ModelMBeanInfo) mbsc.getMBeanInfo(on);
        // previous line will fail if serialization includes targetObject

        Descriptor d2 = mmbi2.getDescriptor("getName", "operation");
        if (!"bar".equals(d2.getFieldValue("foo")))
            throw new Exception("TEST FAILED: bad descriptor: " + d2);

        String name = (String) mbsc.invoke(on, "getName", null, null);
        String thisName = Thread.currentThread().getName();
        if (!thisName.equals(name)) {
            throw new Exception("TEST FAILED: wrong thread name: " +
                                name + " should be " + thisName);
        }
    }
}
