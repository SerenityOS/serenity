/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4967769 4980655 4980668 4981214
 * @summary Test that ModelMBeanInfoSupport.setDescriptor rejects
 * null descriptor, that empty arrays are handled correctly,
 * that getDescriptors("mbean") works, and that default values for
 * MBean descriptors are correctly assigned.
 * @author Eamonn McManus
 *
 * @run clean InfoSupportTest
 * @run build InfoSupportTest
 * @run main InfoSupportTest
 */

import java.util.*;
import javax.management.Descriptor;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.*;

public class InfoSupportTest {
    public static void main(String[] args) throws Exception {
        boolean ok = true;

        ok &= testSetDescriptorNull();
        ok &= testEmptyArrayParameters();
        ok &= testGetDescriptorsForMBean();

        if (ok)
            System.out.println("Test passed");
        else {
            System.out.println("TEST FAILED");
            System.exit(1);
        }
    }

    private static boolean testSetDescriptorNull() {
        System.out.println("Testing that " +
                           "ModelMBeanInfoSupport.setDescriptor(null, \"x\")" +
                           " throws an exception");

        ModelMBeanAttributeInfo mmbai =
            new ModelMBeanAttributeInfo("attribute",
                                        "java.lang.String",
                                        "an attribute",
                                        true, true, false);
        ModelMBeanInfo mmbi =
            new ModelMBeanInfoSupport("bogus.class.name",
                                      "an MBean",
                                      new ModelMBeanAttributeInfo[] {mmbai},
                                      null, null, null);
        try {
            mmbi.setDescriptor(null, "attribute");
        } catch (RuntimeOperationsException e) {
            if (e.getTargetException() instanceof IllegalArgumentException) {
                System.out.println("Test passes: got a " +
                                   "RuntimeOperationsException wrapping an " +
                                   "IllegalArgumentException");
                return true;
            } else {
                System.out.println("TEST FAILS: wrong exception:");
                e.printStackTrace(System.out);
                return false;
            }
        } catch (Exception e) {
            System.out.println("TEST FAILS: wrong exception:");
            e.printStackTrace(System.out);
            return false;
        }

        System.out.println("TEST FAILS: exception not thrown");
        return false;
    }

    private static boolean testEmptyArrayParameters()
            throws Exception {

        System.out.println("Test that empty and null array parameters " +
                           "produce the right type from getters");

        boolean ok = true;

        ModelMBeanInfoSupport mmbi;

        mmbi =
            new ModelMBeanInfoSupport("bogus.class.name", "description",
                                      null, null, null, null);
        ok &= checkMMBI(mmbi, "null parameters, no descriptor");

        mmbi =
            new ModelMBeanInfoSupport("bogus.class.name", "description",
                                      null, null, null, null, null);
        ok &= checkMMBI(mmbi, "null parameters, null descriptor");

        mmbi =
            new ModelMBeanInfoSupport("bogus.class.name", "description",
                                      new ModelMBeanAttributeInfo[0],
                                      new ModelMBeanConstructorInfo[0],
                                      new ModelMBeanOperationInfo[0],
                                      new ModelMBeanNotificationInfo[0]);
        ok &= checkMMBI(mmbi, "empty parameters, no descriptor");

        mmbi =
            new ModelMBeanInfoSupport("bogus.class.name", "description",
                                      new ModelMBeanAttributeInfo[0],
                                      new ModelMBeanConstructorInfo[0],
                                      new ModelMBeanOperationInfo[0],
                                      new ModelMBeanNotificationInfo[0],
                                      null);
        ok &= checkMMBI(mmbi, "empty parameters, null descriptor");

        return ok;
    }

    private static boolean checkMMBI(ModelMBeanInfoSupport mmbi, String what)
            throws Exception {
        String bad = "";

        if (!(mmbi.getAttributes() instanceof ModelMBeanAttributeInfo[]))
            bad += " attributes";
        if (!(mmbi.getConstructors() instanceof ModelMBeanConstructorInfo[]))
            bad += " constructors";
        if (!(mmbi.getOperations() instanceof ModelMBeanOperationInfo[]))
            bad += " operations";
        if (!(mmbi.getNotifications() instanceof ModelMBeanNotificationInfo[]))
            bad += " notifications";

        if (bad.equals("")) {
            System.out.println("..." + what + ": OK");
            return true;
        } else {
            System.out.println("..." + what + ": FAILS for:" + bad);
            return false;
        }
    }

    private static boolean testGetDescriptorsForMBean()
            throws Exception {
        System.out.println("Test getDescriptors(\"mbean\")");

        boolean ok = true;

        ModelMBeanInfo mmbi;
        Descriptor[] mbeanDescrs;

        mmbi = new ModelMBeanInfoSupport("bogus.class.name", "description",
                                         null, null, null, null);
        try {
            mbeanDescrs = mmbi.getDescriptors("mbean");
            if (mbeanDescrs.length == 1 && mbeanDescrs[0] != null)
                System.out.println("...default MBean descriptor: OK");
            else {
                System.out.println("...default MBean descriptor: bad array: " +
                                   Arrays.asList(mbeanDescrs));
                ok = false;
            }
        } catch (Exception e) {
            System.out.println("...default MBean descriptor: got exception:");
            e.printStackTrace(System.out);
            ok = false;
        }

        String[] fields = new String[] {"descriptorType", "name"};
        String[] values = new String[] {"mbean", "whatsit"};
        String[] defaultFields = new String[] {
            "displayName", "persistPolicy", "log", "visibility",
        };
        String[] defaultValues = new String[] {
            "bogus.class.name", "never", "F", "1",
        };
        Descriptor d = new DescriptorSupport(fields, values);

        mmbi = new ModelMBeanInfoSupport("bogus.class.name", "description",
                                         null, null, null, null, d);
        try {
            mbeanDescrs = mmbi.getDescriptors("mbean");
        } catch (Exception e) {
            System.out.println("...explicit MBean descriptor: got exception:");
            e.printStackTrace(System.out);
            mbeanDescrs = new Descriptor[] {mmbi.getMBeanDescriptor()};
        }

        if (mbeanDescrs.length == 1) {
            Descriptor dd = mbeanDescrs[0];
            boolean thisok = true;

            // Check that the fields we supplied survived in the copy
            // and were not changed in the original
            for (int i = 0; i < fields.length; i++) {
                String field = fields[i];
                String value;
                value = (String) dd.getFieldValue(field);
                if (!values[i].equals(value)) {
                    System.out.println("...explicit MBean descriptor: " +
                                       "value of " + field + " mutated: " +
                                       value);
                    thisok = false;
                }
                value = (String) d.getFieldValue(field);
                if (!values[i].equals(value)) {
                    System.out.println("...explicit MBean descriptor: " +
                                       "value of " + field + " changed in " +
                                       "original: " + value);
                    thisok = false;
                }
            }

            // Check that the default fields were added
            for (int i = 0; i < defaultFields.length; i++) {
                String field = defaultFields[i];
                String value = (String) dd.getFieldValue(field);
                if (!defaultValues[i].equals(value)) {
                    System.out.println("...explicit MBean descriptor: " +
                                       "default value of " + field +
                                       " wrong: " + value + " should be " +
                                       defaultValues[i]);
                    thisok = false;
                }
            }

            // Check that the original did not acquire new fields
            if (d.getFieldNames().length != fields.length) {
                Collection c = new TreeSet(Arrays.asList(d.getFieldNames()));
                c.removeAll(Arrays.asList(fields));
                System.out.println("...explicit MBean descriptor: " +
                                   "acquired new fields: " + c);
                thisok = false;
            }

            if (thisok)
                System.out.println("...explicit MBean descriptor: OK");
            else
                ok = false;
        } else {
            System.out.println("...explicit MBean descriptor: bad array: " +
                               Arrays.asList(mbeanDescrs));
            ok = false;
        }

        return ok;
    }
}
