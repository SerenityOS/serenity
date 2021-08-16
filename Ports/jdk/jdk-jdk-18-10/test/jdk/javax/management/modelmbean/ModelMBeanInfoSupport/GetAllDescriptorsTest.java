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
 * @bug 6337061
 * @summary Test that  ModelMBeanInfoSupport.getDescriptors(null) also
 *          returns the MBean's descriptor.
 * @author Eamonn McManus, Daniel Fuchs
 *
 * @run clean GetAllDescriptorsTest
 * @run build GetAllDescriptorsTest
 * @run main/othervm/java.security.policy=policy  GetAllDescriptorsTest
 */

import java.lang.reflect.*;
import java.util.*;
import javax.management.*;
import javax.management.modelmbean.*;

public class GetAllDescriptorsTest {

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
        case 0: {
            /* Check  getDescriptors("") on original MBeanInfo */
            final Descriptor[] desc = info.getDescriptors("");
            checkDescriptors(info,desc,"info.getDescriptors(\"\")");
            break;
        }
        case 1: {
            /* Check  getDescriptors(null) on original MBeanInfo */
            final Descriptor[] desc = info.getDescriptors(null);
            checkDescriptors(info,desc,"info.getDescriptors(null)");
            break;
        }
        case 2: {
            /* Check  getDescriptors("") on retrieved MBeanInfo */
            final MBeanInfo mbi = mbs.getMBeanInfo(on);
            final ModelMBeanInfo model = (ModelMBeanInfo)mbi;
            final Descriptor[] desc = model.getDescriptors("");
            checkDescriptors(info,desc,"model.getDescriptors(\"\")");
            break;
        }
        case 3: {
            /* Check  getDescriptors(null) on retrieved MBeanInfo */
            final MBeanInfo mbi = mbs.getMBeanInfo(on);
            final ModelMBeanInfo model = (ModelMBeanInfo)mbi;
            final Descriptor[] desc = model.getDescriptors(null);
            checkDescriptors(info,desc,"model.getDescriptors(null)");
            break;
        }
        default:
            System.err.println("UNKNOWN TEST NUMBER " + testno);
            break;
        }
    }

    /* Removes descriptor from the list and returns it. Returns {@code null}
       if descriptor is not found */
    private static Descriptor remove(ArrayList<Descriptor> list,
            Descriptor item) {
        if (list.remove(item)) return item;
        else return null;
    }

    /* Check that all descriptors have been returned */
    private static void checkDescriptors(ModelMBeanInfo modelMBeanInfo,
            Descriptor[] descriptors, String string) {
        int errCount = 0;
        final ArrayList<Descriptor> list =
                new ArrayList<Descriptor>(descriptors.length);
        list.addAll(Arrays.asList(descriptors));
        System.out.println("Got " + list.size() + " descriptors for "+string);

        // checks that MBean's descriptor is returned.
        //
        final Descriptor mbd = ((MBeanInfo)modelMBeanInfo).getDescriptor();
        if (!mbd.equals(remove(list,mbd))) {
            System.err.println("modelMBeanInfo.getDescriptor(): not found");
            errCount++;
        }

        // checks that MBean's attributes descriptors are returned.
        //
        final MBeanAttributeInfo[] attrs = modelMBeanInfo.getAttributes();
        for (MBeanAttributeInfo att : attrs) {
            final Descriptor ad = att.getDescriptor();
            final String name = att.getName();
            if (!ad.equals(remove(list,ad))) {
                System.err.println("attInfo.getDescriptor(): not found for "+
                        name);
                errCount++;
            }
        }

        // checks that MBean's operations descriptors are returned.
        //
        final MBeanOperationInfo[] ops = modelMBeanInfo.getOperations();
        for (MBeanOperationInfo op : ops) {
            final Descriptor od = op.getDescriptor();
            final String name = op.getName();
            if (!od.equals(remove(list,od))) {
                System.err.println("opInfo.getDescriptor(): not found for "+
                        name);
                errCount++;
            }
        }

        // checks that MBean's notifications descriptors are returned.
        //
        final MBeanNotificationInfo[] ntfs = modelMBeanInfo.getNotifications();
        for (MBeanNotificationInfo ntf : ntfs) {
            final Descriptor nd = ntf.getDescriptor();
            final String name = ntf.getName();
            if (!nd.equals(remove(list,nd))) {
                System.err.println("notifInfo.getDescriptor(): not found for "+
                        name);
                errCount++;
            }
        }
        if (errCount > 0) {
            throw new RuntimeException(string+": failed with "+errCount+
                    " errors");
        } else if (list.size() != 0) {
            // Check that there are no additional descriptors
            //
            throw new RuntimeException(string+
                    ": Unexpected remaining descriptors: "+list);
        } else System.out.println(string+": PASSED");
    }

    private static final int NTESTS = 4;

}
