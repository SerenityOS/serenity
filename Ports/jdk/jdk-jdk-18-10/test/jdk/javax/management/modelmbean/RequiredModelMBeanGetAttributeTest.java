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
 * @bug 5043245
 * @summary Test the following in RequiredModelMBean.getAttribute():
 * The declared type of the attribute is the String returned by
 * ModelMBeanAttributeInfo.getType(). A value is compatible
 * with this type if one of the following is true:
 * - the value is null;
 * - the declared name is a primitive type name (such as "int")
 *   and the value is an instance of the corresponding wrapper
 *   type (such as java.lang.Integer);
 * - the name of the value's class is identical to the declared name;
 * - the declared name can be loaded by the value's class loader and
 *   produces a class to which the value can be assigned.
 * @author Luis-Miguel Alventosa
 *
 * @run clean RequiredModelMBeanGetAttributeTest
 * @run build RequiredModelMBeanGetAttributeTest
 * @run main RequiredModelMBeanGetAttributeTest
 */

import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;
import javax.management.Descriptor;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.RequiredModelMBean;

public class RequiredModelMBeanGetAttributeTest {

    public static void main(String[] args) throws Exception {

        boolean ok = true;

        MBeanServer mbs = MBeanServerFactory.createMBeanServer();

        // Resource methods

        Method nullGetter =
            Resource.class.getMethod("getNull", (Class[]) null);
        Method integerGetter =
            Resource.class.getMethod("getInteger", (Class[]) null);
        Method hashtableGetter =
            Resource.class.getMethod("getHashtable", (Class[]) null);
        Method mapGetter =
            Resource.class.getMethod("getMap", (Class[]) null);

        // ModelMBeanOperationInfo

        Descriptor nullOperationDescriptor =
            new DescriptorSupport(new String[] {
                "name=getNull",
                "descriptorType=operation",
                "role=getter"
            });
        ModelMBeanOperationInfo nullOperationInfo =
            new ModelMBeanOperationInfo("Null attribute",
                                        nullGetter,
                                        nullOperationDescriptor);

        Descriptor integerOperationDescriptor =
            new DescriptorSupport(new String[] {
                "name=getInteger",
                "descriptorType=operation",
                "role=getter"
            });
        ModelMBeanOperationInfo integerOperationInfo =
            new ModelMBeanOperationInfo("Integer attribute",
                                        integerGetter,
                                        integerOperationDescriptor);

        Descriptor hashtableOperationDescriptor =
            new DescriptorSupport(new String[] {
                "name=getHashtable",
                "descriptorType=operation",
                "role=getter"
            });
        ModelMBeanOperationInfo hashtableOperationInfo =
            new ModelMBeanOperationInfo("Hashtable attribute",
                                        hashtableGetter,
                                        hashtableOperationDescriptor);

        Descriptor mapOperationDescriptor =
            new DescriptorSupport(new String[] {
                "name=getMap",
                "descriptorType=operation",
                "role=getter"
            });
        ModelMBeanOperationInfo mapOperationInfo =
            new ModelMBeanOperationInfo("Map attribute",
                                        mapGetter,
                                        mapOperationDescriptor);

        // ModelMBeanAttributeInfo

        Descriptor nullAttributeDescriptor =
            new DescriptorSupport(new String[] {
                "name=Null",
                "descriptorType=attribute",
                "getMethod=getNull"
            });
        ModelMBeanAttributeInfo nullAttributeInfo =
            new ModelMBeanAttributeInfo("Null",
                                        "java.lang.Object",
                                        "Null attribute",
                                        true,
                                        false,
                                        false,
                                        nullAttributeDescriptor);

        Descriptor integerAttributeDescriptor =
            new DescriptorSupport(new String[] {
                "name=Integer",
                "descriptorType=attribute",
                "getMethod=getInteger"
            });
        ModelMBeanAttributeInfo integerAttributeInfo =
            new ModelMBeanAttributeInfo("Integer",
                                        "int",
                                        "Integer attribute",
                                        true,
                                        false,
                                        false,
                                        integerAttributeDescriptor);

        Descriptor hashtableAttributeDescriptor =
            new DescriptorSupport(new String[] {
                "name=Hashtable",
                "descriptorType=attribute",
                "getMethod=getHashtable"
            });
        ModelMBeanAttributeInfo hashtableAttributeInfo =
            new ModelMBeanAttributeInfo("Hashtable",
                                        "java.util.Hashtable",
                                        "Hashtable attribute",
                                        true,
                                        false,
                                        false,
                                        hashtableAttributeDescriptor);

        Descriptor mapAttributeDescriptor =
            new DescriptorSupport(new String[] {
                "name=Map",
                "descriptorType=attribute",
                "getMethod=getMap"
            });
        ModelMBeanAttributeInfo mapAttributeInfo =
            new ModelMBeanAttributeInfo("Map",
                                        "java.util.Map",
                                        "Map attribute",
                                        true,
                                        false,
                                        false,
                                        mapAttributeDescriptor);

        Descriptor null2AttributeDescriptor =
            new DescriptorSupport(new String[] {
                "name=Null2",
                "descriptorType=attribute"
            });
        null2AttributeDescriptor.setField("default", null);
        ModelMBeanAttributeInfo null2AttributeInfo =
            new ModelMBeanAttributeInfo("Null2",
                                        "java.lang.Object",
                                        "Null2 attribute",
                                        true,
                                        false,
                                        false,
                                        null2AttributeDescriptor);

        Descriptor integer2AttributeDescriptor =
            new DescriptorSupport(new String[] {
                "name=Integer2",
                "descriptorType=attribute"
            });
        integer2AttributeDescriptor.setField("default", 10);
        ModelMBeanAttributeInfo integer2AttributeInfo =
            new ModelMBeanAttributeInfo("Integer2",
                                        "int",
                                        "Integer2 attribute",
                                        true,
                                        false,
                                        false,
                                        integer2AttributeDescriptor);

        Descriptor hashtable2AttributeDescriptor =
            new DescriptorSupport(new String[] {
                "name=Hashtable2",
                "descriptorType=attribute"
            });
        hashtable2AttributeDescriptor.setField("default", new Hashtable());
        ModelMBeanAttributeInfo hashtable2AttributeInfo =
            new ModelMBeanAttributeInfo("Hashtable2",
                                        "java.util.Hashtable",
                                        "Hashtable2 attribute",
                                        true,
                                        false,
                                        false,
                                        hashtable2AttributeDescriptor);

        Descriptor map2AttributeDescriptor =
            new DescriptorSupport(new String[] {
                "name=Map2",
                "descriptorType=attribute"
            });
        map2AttributeDescriptor.setField("default", new Hashtable());
        ModelMBeanAttributeInfo map2AttributeInfo =
            new ModelMBeanAttributeInfo("Map2",
                                        "java.util.Map",
                                        "Map2 attribute",
                                        true,
                                        false,
                                        false,
                                        map2AttributeDescriptor);

        // ModelMBeanInfo

        ModelMBeanInfo mmbi = new ModelMBeanInfoSupport(
            Resource.class.getName(),
            "Resource MBean",
            new ModelMBeanAttributeInfo[] { nullAttributeInfo,
                                            integerAttributeInfo,
                                            hashtableAttributeInfo,
                                            mapAttributeInfo,
                                            null2AttributeInfo,
                                            integer2AttributeInfo,
                                            hashtable2AttributeInfo,
                                            map2AttributeInfo },
            null,
            new ModelMBeanOperationInfo[] { nullOperationInfo,
                                            integerOperationInfo,
                                            hashtableOperationInfo,
                                            mapOperationInfo },
            null);

        // RequiredModelMBean

        ModelMBean mmb = new RequiredModelMBean(mmbi);
        mmb.setManagedResource(resource, "ObjectReference");
        ObjectName mmbName = new ObjectName(":type=ResourceMBean");
        mbs.registerMBean(mmb, mmbName);

        // Run tests

        System.out.println("\nTesting that we can call getNull()... ");
        try {
            Object o = mbs.getAttribute(mmbName, "Null");
            System.out.println("getNull() = " + o);
            System.out.println("Attribute's declared type = java.lang.Object");
            System.out.println("Returned value's type = null");
        } catch (Exception e) {
            System.out.println("TEST FAILED: Caught exception:");
            e.printStackTrace(System.out);
            ok = false;
        }

        System.out.println("\nTesting that we can call getInteger()... ");
        try {
            Integer i = (Integer) mbs.getAttribute(mmbName, "Integer");
            System.out.println("getInteger() = " + i);
            System.out.println("Attribute's declared type = int");
            System.out.println("Returned value's type = " +
                               i.getClass().getName());
        } catch (Exception e) {
            System.out.println("TEST FAILED: Caught exception:");
            e.printStackTrace(System.out);
            ok = false;
        }

        System.out.println("\nTesting that we can call getHashtable()... ");
        try {
            Hashtable h = (Hashtable) mbs.getAttribute(mmbName, "Hashtable");
            System.out.println("getHashtable() = " + h);
            System.out.println("Attribute's declared type = " +
                               "java.util.Hashtable");
            System.out.println("Returned value's type = " +
                               h.getClass().getName());
        } catch (Exception e) {
            System.out.println("TEST FAILED: Caught exception:");
            e.printStackTrace(System.out);
            ok = false;
        }

        System.out.println("\nTesting that we can call getMap()... ");
        try {
            Map m = (Map) mbs.getAttribute(mmbName, "Map");
            System.out.println("getMap() = " + m);
            System.out.println("Attribute's declared type = " +
                               "java.util.Map");
            System.out.println("Returned value's type = " +
                               m.getClass().getName());
        } catch (Exception e) {
            System.out.println("TEST FAILED: Caught exception:");
            e.printStackTrace(System.out);
            ok = false;
        }

        System.out.println("\nTesting that we can call getNull2()... ");
        try {
            Object o = mbs.getAttribute(mmbName, "Null2");
            System.out.println("getNull2() = " + o);
            System.out.println("Attribute's declared type = java.lang.Object");
            System.out.println("Returned value's type = null");
        } catch (Exception e) {
            System.out.println("TEST FAILED: Caught exception:");
            e.printStackTrace(System.out);
            ok = false;
        }

        System.out.println("\nTesting that we can call getInteger2()... ");
        try {
            Integer i = (Integer) mbs.getAttribute(mmbName, "Integer2");
            System.out.println("getInteger2() = " + i);
            System.out.println("Attribute's declared type = int");
            System.out.println("Returned value's type = " +
                               i.getClass().getName());
        } catch (Exception e) {
            System.out.println("TEST FAILED: Caught exception:");
            e.printStackTrace(System.out);
            ok = false;
        }

        System.out.println("\nTesting that we can call getHashtable2()... ");
        try {
            Hashtable h = (Hashtable) mbs.getAttribute(mmbName, "Hashtable2");
            System.out.println("getHashtable2() = " + h);
            System.out.println("Attribute's declared type = " +
                               "java.util.Hashtable");
            System.out.println("Returned value's type = " +
                               h.getClass().getName());
        } catch (Exception e) {
            System.out.println("TEST FAILED: Caught exception:");
            e.printStackTrace(System.out);
            ok = false;
        }

        System.out.println("\nTesting that we can call getMap2()... ");
        try {
            Map m = (Map) mbs.getAttribute(mmbName, "Map2");
            System.out.println("getMap2() = " + m);
            System.out.println("Attribute's declared type = " +
                               "java.util.Map");
            System.out.println("Returned value's type = " +
                               m.getClass().getName());
        } catch (Exception e) {
            System.out.println("TEST FAILED: Caught exception:");
            e.printStackTrace(System.out);
            ok = false;
        }

        if (ok)
            System.out.println("\nTest passed.\n");
        else {
            System.out.println("\nTest failed.\n");
            System.exit(1);
        }
    }

    public static class Resource {
        public Object getNull() {
            return null;
        }
        public int getInteger() {
            return 10;
        }
        public Hashtable getHashtable() {
            return new Hashtable();
        }
        public Map getMap() {
            return new Hashtable();
        }
    }

    private static Resource resource = new Resource();
}
