/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanConstructorInfo;
import javax.management.openmbean.OpenMBeanConstructorInfoSupport;
import javax.management.openmbean.OpenMBeanInfo;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfo;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.SimpleType;

/*
 * @test
 * @bug 8023529
 * @summary Test that OpenMBean*Info.equals do not throw NPE
 * @author Shanliang JIANG
 *
 * @run clean OpenMBeanInfoEqualsNPETest
 * @run build OpenMBeanInfoEqualsNPETest
 * @run main OpenMBeanInfoEqualsNPETest
 */
public class OpenMBeanInfoEqualsNPETest {
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("---OpenMBeanInfoEqualsNPETest-main ...");

        // ----
        System.out.println("\n---Testing on OpenMBeanAttributeInfoSupport...");
        OpenMBeanAttributeInfo openMBeanAttributeInfo0 = new OpenMBeanAttributeInfoSupport(
                "name", "description", SimpleType.INTEGER, true, true, false, 1, new Integer[]{1, 2, 3});
        OpenMBeanAttributeInfo openMBeanAttributeInfo = new OpenMBeanAttributeInfoSupport(
                "name", "description", SimpleType.INTEGER, true, true, false, null, new Integer[]{1, 2, 3});
        test(openMBeanAttributeInfo0, openMBeanAttributeInfo, "defaultValue");

        openMBeanAttributeInfo = new OpenMBeanAttributeInfoSupport(
                "name", "description", SimpleType.INTEGER, true, true, false, 1, null);
        test(openMBeanAttributeInfo0, openMBeanAttributeInfo, "legalValues");

        // ----
        System.out.println("\n---Testing on OpenMBeanConstructorInfoSupport...");
        OpenMBeanConstructorInfo openMBeanConstructorInfo0 = new OpenMBeanConstructorInfoSupport(
                "name", "description", new OpenMBeanParameterInfo[]{}, new DescriptorSupport());
        OpenMBeanConstructorInfo openMBeanConstructorInfo;

        openMBeanConstructorInfo = new OpenMBeanConstructorInfoSupport(
                "name", "description", null, new DescriptorSupport());
        test(openMBeanConstructorInfo0, openMBeanConstructorInfo, "sigs");

        openMBeanConstructorInfo = new OpenMBeanConstructorInfoSupport(
                "name", "description", new OpenMBeanParameterInfo[]{}, null);
        test(openMBeanConstructorInfo0, openMBeanConstructorInfo, "Descriptor");

        // ----
        System.out.println("\n---Testing on OpenMBeanOperationInfoSupport...");
        OpenMBeanOperationInfo openMBeanOperationInfo0 = new OpenMBeanOperationInfoSupport(
                "name", "description", new OpenMBeanParameterInfo[]{},  SimpleType.INTEGER, 1, new DescriptorSupport());
        OpenMBeanOperationInfo openMBeanOperationInfo;

        openMBeanOperationInfo = new OpenMBeanOperationInfoSupport(
                "name", "description", null,  SimpleType.INTEGER, 1, new DescriptorSupport());
        test(openMBeanOperationInfo0, openMBeanOperationInfo, "sigs");

        openMBeanOperationInfo = new OpenMBeanOperationInfoSupport(
                "name", "description", new OpenMBeanParameterInfo[]{},  SimpleType.INTEGER, MBeanOperationInfo.UNKNOWN, null);
        test(openMBeanOperationInfo0, openMBeanOperationInfo, "Descriptor");

        // ----
        System.out.println("\n---Testing on OpenMBeanParameterInfoSupport 1...");
        OpenMBeanParameterInfo openMBeanParameterInfo0 = new OpenMBeanParameterInfoSupport(
                "name", "description", SimpleType.INTEGER, 0, -1, 1);
        OpenMBeanParameterInfo openMBeanParameterInfo;

        openMBeanParameterInfo = new OpenMBeanParameterInfoSupport(
                "name", "description", SimpleType.INTEGER, null, -1, 1);
        test(openMBeanParameterInfo0, openMBeanParameterInfo, "default value");

        openMBeanParameterInfo = new OpenMBeanParameterInfoSupport(
                "name", "description", SimpleType.INTEGER, 0, null, 1);
        test(openMBeanParameterInfo0, openMBeanParameterInfo, "min value");

        openMBeanParameterInfo = new OpenMBeanParameterInfoSupport(
                "name", "description", SimpleType.INTEGER, 0, -1, null);
        test(openMBeanParameterInfo0, openMBeanParameterInfo, "max value");

        // ----
        System.out.println("\n---Testing on OpenMBeanParameterInfoSupport 2...");
        openMBeanParameterInfo0 = new OpenMBeanParameterInfoSupport(
                "name", "description", SimpleType.INTEGER, 1, new Integer[]{-1, 1, 2});

        openMBeanParameterInfo = new OpenMBeanParameterInfoSupport(
                "name", "description", SimpleType.INTEGER, null, new Integer[]{-1, 1, 2});
        test(openMBeanParameterInfo0, openMBeanParameterInfo, "default value");

        openMBeanParameterInfo = new OpenMBeanParameterInfoSupport(
                "name", "description", SimpleType.INTEGER, 1, null);
        test(openMBeanParameterInfo0, openMBeanParameterInfo, "legal values");

        // ----
        System.out.println("\n---Testing on OpenMBeanInfoSupport...");
        String className = "toto";
        String description = "titi";
        OpenMBeanAttributeInfo[] attrInfos = new OpenMBeanAttributeInfo[]{};
        OpenMBeanConstructorInfo[] constrInfos = new OpenMBeanConstructorInfo[]{};
        OpenMBeanOperationInfo[] operaInfos = new OpenMBeanOperationInfo[]{};
        MBeanNotificationInfo[] notifInfos = new MBeanNotificationInfo[]{};

        OpenMBeanInfo ominfo0 = new OpenMBeanInfoSupport("toto", description, attrInfos, constrInfos, operaInfos, notifInfos);
        OpenMBeanInfo ominfo = new OpenMBeanInfoSupport(null, description, attrInfos, constrInfos, operaInfos, notifInfos);
        test(ominfo0, ominfo, "class name");

        ominfo = new OpenMBeanInfoSupport(className, null, attrInfos, constrInfos, operaInfos, notifInfos);
        test(ominfo0, ominfo, "description");

        ominfo = new OpenMBeanInfoSupport(className, description, null, constrInfos, operaInfos, notifInfos);
        test(ominfo0, ominfo, "attrInfos");

        ominfo = new OpenMBeanInfoSupport(className, description, attrInfos, null, operaInfos, notifInfos);
        test(ominfo0, ominfo, "constructor infos");

        ominfo = new OpenMBeanInfoSupport(className, description, attrInfos, constrInfos, null, notifInfos);
        test(ominfo0, ominfo, "operation infos");

        ominfo = new OpenMBeanInfoSupport(className, description, attrInfos, constrInfos, operaInfos, null);
        test(ominfo0, ominfo, "notif infos");

        if (failed > 0) {
            throw new RuntimeException("Test failed: "+failed);
        } else {
            System.out.println("---Test: PASSED");
        }
    }

    private static void test(Object obj1, Object obj2, String param) {
        try {
            obj1.equals(obj2);
            System.out.println("OK-1: "+obj1.getClass().getSimpleName()+
                    ".equals worked with a null field: "+param);
        } catch (NullPointerException npe) {
            System.out.println("--->KO-1!!! "+obj1.getClass().getSimpleName()+
                    ".equals got NPE with a null field: "+param);
            npe.printStackTrace();
            failed++;
        }

        try {
            obj2.equals(obj1);
            System.out.println("OK-2: "+obj2.getClass().getSimpleName()+
                    ".equals worked with a null field: "+param);
        } catch (NullPointerException npe) {
            System.out.println("--->KO-2!!! "+obj2.getClass().getSimpleName()+
                    ".equals got NPE with a null field: "+param);
            npe.printStackTrace();
            failed++;
        }

        try {
            obj1.equals(null);
            obj2.equals(null);

            System.out.println("OK-3: "+obj1.getClass().getSimpleName()+
                    ".equals worked with a null object.");
        } catch (NullPointerException npe) {
            System.out.println("--->KO-3!!! "+obj1.getClass().getSimpleName()+
                    ".equals got NPE with a null object.");
            npe.printStackTrace();
            failed++;
        }
    }
}
