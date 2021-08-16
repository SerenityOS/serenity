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
 * @bug 4997033
 * @summary Test the following in RequiredModelMBean.setAttribute():
 * MBeanException wrapping a ServiceNotFoundException is thrown is setAttribute
 * called but no setMethod field has been provided.
 * @author Jean-Francois Denise
 *
 * @run clean RequiredModelMBeanSetAttributeTest
 * @run build RequiredModelMBeanSetAttributeTest
 * @run main RequiredModelMBeanSetAttributeTest
 */

import javax.management.Descriptor;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.Attribute;
import javax.management.MBeanException;
import javax.management.ServiceNotFoundException;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.RequiredModelMBean;

public class RequiredModelMBeanSetAttributeTest {

    public static void main(String[] args) throws Exception {

        boolean ok = true;

        MBeanServer mbs = MBeanServerFactory.createMBeanServer();

        // ModelMBeanAttributeInfo

        Descriptor somethingAttributeDescriptor =
            new DescriptorSupport(new String[] {
                "name=Something",
                "descriptorType=attribute",
                "getMethod=getSomething"
            });
        ModelMBeanAttributeInfo somethingAttributeInfo =
            new ModelMBeanAttributeInfo("Something",
                                        "java.lang.String",
                                        "Something attribute",
                                        true,
                                        true,
                                        false,
                                        somethingAttributeDescriptor);

        Descriptor somethingCachedAttributeDescriptor =
            new DescriptorSupport(new String[] {
                "name=SomethingCached",
                "descriptorType=attribute",
                "getMethod=getSomethingCached",
                "currencyTimeLimit=5000"
            });
        ModelMBeanAttributeInfo somethingCachedAttributeInfo =
            new ModelMBeanAttributeInfo("SomethingCached",
                                        "java.lang.String",
                                        "Something cached attribute",
                                        true,
                                        true,
                                        false,
                                        somethingCachedAttributeDescriptor);
        // ModelMBeanInfo

        ModelMBeanInfo mmbi = new ModelMBeanInfoSupport(
            Resource.class.getName(),
            "Resource MBean",
            new ModelMBeanAttributeInfo[] { somethingAttributeInfo, somethingCachedAttributeInfo },
            null,
            new ModelMBeanOperationInfo[] {},
            null);

        // RequiredModelMBean

        ModelMBean mmb = new RequiredModelMBean(mmbi);
        mmb.setManagedResource(resource, "ObjectReference");
        ObjectName mmbName = new ObjectName(":type=ResourceMBean");
        mbs.registerMBean(mmb, mmbName);

        // Run tests

        System.out.println("\nTest that we receive ServiceNotFoundException");
        try {
            Attribute attr = new Attribute("Something", "Some string");
            mbs.setAttribute(mmbName, attr);
            System.out.println("TEST FAILED: Didn't caught exception");
            ok = false;
        } catch(MBeanException mbex) {
            Exception e = mbex.getTargetException();
            if(e == null || !(e instanceof ServiceNotFoundException)) {
                System.out.println("TEST FAILED: Caught wrong exception:" + e);
                ok = false;
            } else
                System.out.println("Received expected ServiceNotFoundException");

        } catch (Exception e) {
            System.out.println("TEST FAILED: Caught wrong exception: " + e);
            e.printStackTrace(System.out);
            ok = false;
        }

        //Now check that when caching is enabled, setAttribute is working
        System.out.println("\nTest that we are not receiving ServiceNotFoundException");
        try {
            Attribute attr = new Attribute("SomethingCached", "Some string");
            mbs.setAttribute(mmbName, attr);
            System.out.println("No exception thrown");
       } catch (Exception e) {
            System.out.println("TEST FAILED: Caught an exception: " + e);
            e.printStackTrace(System.out);
            ok = false;
       }

        if (ok)
            System.out.println("Test passed");
        else {
            System.out.println("TEST FAILED");
            throw new Exception("TEST FAILED");
        }
    }

    public static class Resource {
        public String getSomething() {
            return "Something value";
        }
        public String getSomethingCached() {
            return "Something cached value";
        }
    }

    private static Resource resource = new Resource();
}
