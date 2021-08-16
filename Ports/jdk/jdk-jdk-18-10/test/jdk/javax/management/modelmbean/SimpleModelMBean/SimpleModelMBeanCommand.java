/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4874819
 * @summary Test that MBeanInfo classes no longer throw an
 * IllegalArgumentException when attribute names, operation names, and
 * Java type names do not strictly follow the expected Java syntax.
 * @author Eamonn McManus, Daniel Fuchs
 *
 * @run clean SimpleModelMBeanCommand
 * @run build SimpleModelMBeanCommand
 * @run main/othervm/java.security.policy=policy  SimpleModelMBeanCommand
 */

import java.lang.reflect.*;
import java.util.*;
import javax.management.*;
import javax.management.modelmbean.*;

public class SimpleModelMBeanCommand {

    public static class Resource {
        public int getNumber() {
            return number;
        }

        public void setNumber(int n) {
            number = n;
        }

        public int addOne(int x) {
            return x + 1;
        }

        public Object[] getArray() {
            return (Object[]) array.clone();
        }

        // doesn't look like an attribute so not seen by caching logic
        public void tweakArray(Object[] array) {
            this.array = (Object[]) array.clone();
        }

        private int number = 1234;
        private Object[] array = {"hello", "world"};
    }

    public static void main(String[] args) {
        int errorCount = 0;
        for (int i = 0; i < NTESTS; i++) {
            try {
                System.out.println("Test " + i + ":");
                test(i);
            } catch (Throwable e) {
                errorCount++;
                boolean first = true;
                do {
                    System.err.println(first ? "Exception:" : "Caused by:");
                    first = false;
                    e.printStackTrace();
                    Throwable nexte;
                    nexte = e.getCause();
                    if (nexte == null) { // old JMX
                        if (e instanceof MBeanException)
                            nexte = ((MBeanException) e).getTargetException();
                    }
                    e = nexte;
                } while (e != null);
            }
        }
        if (errorCount == 0) {
            System.out.println("All ModelMBean tests successfuly passed");
            System.out.println("Bye! Bye!");
            // JTReg doesn't like System.exit(0);
            return;
        } else {
            System.err.println("ERROR: " + errorCount + " tests failed");
            System.exit(errorCount);
        }

    }

    private static void test(int testno) throws Exception {
        // com.sun.jmx.trace.TraceImplementation.init(2);
        Resource resource = new Resource();
        Class resourceClass = Resource.class;
        Class rmmbClass = RequiredModelMBean.class;
        Method setManagedResource =
            rmmbClass.getMethod("setManagedResource",
                                new Class[] {Object.class,
                                             String.class});
        Method sendNotification =
            rmmbClass.getMethod("sendNotification",
                                new Class[] {Notification.class});
        Method addAttributeChangeNL =
            rmmbClass.getMethod("addAttributeChangeNotificationListener",
                                new Class[] {NotificationListener.class,
                                             String.class,
                                             Object.class});
        Method getArray = resourceClass.getMethod("getArray", new Class[0]);
        Method getNumber = resourceClass.getMethod("getNumber", new Class[0]);
        Method setNumber =
            resourceClass.getMethod("setNumber", new Class[] {Integer.TYPE});
        Method tweakArray =
            resourceClass.getMethod("tweakArray",
                                    new Class[] {Object[].class});
        Method addOne =
            resourceClass.getMethod("addOne", new Class[] {Integer.TYPE});
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName on = new ObjectName("a:b=c");
        Descriptor attrDescr = new DescriptorSupport();
        attrDescr.setField("name", "Array");
        attrDescr.setField("descriptorType", "attribute");
        attrDescr.setField("getMethod", "getArray");
        ModelMBeanAttributeInfo attrInfo =
            new ModelMBeanAttributeInfo("Array", "array attr", getArray,
                                        null, attrDescr);
        Descriptor attrDescr2 = new DescriptorSupport();
        attrDescr2.setField("name", "Number");
        attrDescr2.setField("descriptorType", "attribute");
        attrDescr2.setField("getMethod", "getNumber");
        attrDescr2.setField("setMethod", "setNumber");
        ModelMBeanAttributeInfo attrInfo2 =
            new ModelMBeanAttributeInfo("Number", "number attr", getNumber,
                                        setNumber, attrDescr2);
        Descriptor attrDescr3 = new DescriptorSupport();
        attrDescr3.setField("name", "Local");
        attrDescr3.setField("descriptorType", "attribute");
        attrDescr3.setField("currencyTimeLimit", "" + Integer.MAX_VALUE);
        ModelMBeanAttributeInfo attrInfo3 =
            new ModelMBeanAttributeInfo("Local", "java.lang.String",
                                        "local attr", true, true, false,
                                        attrDescr3);
        Descriptor attrDescr4 = new DescriptorSupport();
        attrDescr4.setField("name", "Local2");
        attrDescr4.setField("descriptorType", "attribute");
        ModelMBeanAttributeInfo attrInfo4 =
            new ModelMBeanAttributeInfo("Local2", "java.lang.String",
                                        "local attr 2", true, true, false,
                                        attrDescr4);
        ModelMBeanAttributeInfo[] attrs =
            new ModelMBeanAttributeInfo[] {attrInfo, attrInfo2, attrInfo3,
                                           attrInfo4};
        ModelMBeanOperationInfo operInfo =
            new ModelMBeanOperationInfo("getArray descr", getArray);
        ModelMBeanOperationInfo operInfo2 =
            new ModelMBeanOperationInfo("getNumber descr", getNumber);
        ModelMBeanOperationInfo operInfo3 =
            new ModelMBeanOperationInfo("addOne descr", addOne);
        ModelMBeanOperationInfo operInfo4 =
            new ModelMBeanOperationInfo("setNumber descr", setNumber);
        ModelMBeanOperationInfo operInfo5 =
            new ModelMBeanOperationInfo("tweakArray descr", tweakArray);
        ModelMBeanOperationInfo operInfoSetManagedResource =
            new ModelMBeanOperationInfo("setManagedResource descr",
                                        setManagedResource);
        ModelMBeanOperationInfo operInfoSendNotification =
            new ModelMBeanOperationInfo("sendNotification descr",
                                        sendNotification);
        ModelMBeanOperationInfo operInfoAddAttributeChangeNL =
            new ModelMBeanOperationInfo("AddAttributeChangeNL descr",
                                        addAttributeChangeNL);
        ModelMBeanOperationInfo[] opers =
            new ModelMBeanOperationInfo[] {operInfo, operInfo2, operInfo3,
                                           operInfo4, operInfo5,
                                           operInfoSetManagedResource,
                                           operInfoSendNotification,
                                           operInfoAddAttributeChangeNL};
        ModelMBeanInfo info =
            new ModelMBeanInfoSupport(Resource.class.getName(),
                                      "Resourcish resource",
                                      attrs, null, opers, null,
                                      null);
        mbs.createMBean(RequiredModelMBean.class.getName(),
                        on,
                        new Object[] {info},
                        new String[] {ModelMBeanInfo.class.getName()});
        mbs.invoke(on, "setManagedResource",
                   new Object[] {resource, "objectReference"},
                   new String[] {"java.lang.Object", "java.lang.String"});
        switch (testno) {
        case 0:
            /* Check that we can get an attribute of type Object[] */
            Object[] objs = (Object[]) mbs.getAttribute(on, "Array");
            for (int i = 0; i < objs.length; i++)
                System.out.println(objs[i]);
            break;
        case 1:
            /* Check that we can get an attribute of type int */
            Integer n = (Integer) mbs.getAttribute(on, "Number");
            System.out.println(n);
            break;
        case 2:
            /* Check that we can call an operation that returns int */
            Integer n1 =
                (Integer) mbs.invoke(on, "addOne",
                                     new Integer[] {new Integer(1233)},
                                     new String[] {"int"});
            System.out.println(n1);
            break;
        case 3:
            /* Check that we don't get an exception if you sendNotification
               without any listeners.  */
            Notification notif = new Notification("type", "source", 123L);
            mbs.invoke(on, "sendNotification", new Object[] {notif},
                       new String[] {"javax.management.Notification"});
            System.out.println("Successfully sent notification");
            break;
        case 4:
            /* Check that we can call addAttributeChangeNotificationListener
               with null attribute.  */
            NotificationListener listener = new NotificationListener() {
                public void handleNotification(Notification notif,
                                               Object handback) {
                    System.out.println("Got notif: " + notif +
                                       " with handback: " + handback);
                }
            };
            mbs.invoke(on, "addAttributeChangeNotificationListener",
                       new Object[] {listener, null, "the-handback"},
                       new String[] {
                           "javax.management.NotificationListener",
                           "java.lang.String",
                           "java.lang.Object",
                       });
            mbs.setAttribute(on, new Attribute("Number", new Integer(4321)));
            System.out.println("Attribute value now: " +
                               mbs.getAttribute(on, "Number"));
            break;
        case 5:
            /* Check that the default caching behaviour is not to cache.  */
            Object[] firstGot = (Object[]) mbs.getAttribute(on, "Array");
            System.out.println("First got: " + Arrays.asList(firstGot));
            ModelMBeanInfo mmbi = (ModelMBeanInfo) mbs.getMBeanInfo(on);
            System.out.println(mmbi.getDescriptor("Array", "attribute"));
            mbs.invoke(on, "tweakArray", new Object[] {new Object[] {"x"}},
                       new String[] {Object[].class.getName()});
            Object[] secondGot = (Object[]) mbs.getAttribute(on, "Array");
            System.out.println("Second got: " + Arrays.asList(secondGot));
            if (secondGot.length != 1)
                throw new Exception("Got value: " + Arrays.asList(secondGot));
            break;
        case 6:
            /* Check that attributes without getters or setters work.
               The value is stored in the descriptor.  This test includes
               an explicit currencyTimeLimit attribute.  */
            mbs.setAttribute(on, new Attribute("Local", "string value"));
            ModelMBeanInfo mmbi2 = (ModelMBeanInfo) mbs.getMBeanInfo(on);
            System.out.println(mmbi2.getDescriptor("Local", "attribute"));
            Object gotback = mbs.getAttribute(on, "Local");
            if (!"string value".equals(gotback))
                throw new Exception("Got value: " + gotback);
            break;
        case 7:
            /* Check that attributes without getters or setters work.
               The value is stored in the descriptor.  This test does
               not have an explicit currencyTimeLimit attribute.  */
            mbs.setAttribute(on, new Attribute("Local2", "thing value"));
            ModelMBeanInfo mmbi3 = (ModelMBeanInfo) mbs.getMBeanInfo(on);
            System.out.println(mmbi3.getDescriptor("Local2", "attribute"));
            Object gotback2 = mbs.getAttribute(on, "Local2");
            if (!"thing value".equals(gotback2))
                throw new Exception("Got value: " + gotback2);
            break;
        default:
            System.err.println("UNKNOWN TEST NUMBER " + testno);
            break;
        }
    }

    private static final int NTESTS = 8;

}
