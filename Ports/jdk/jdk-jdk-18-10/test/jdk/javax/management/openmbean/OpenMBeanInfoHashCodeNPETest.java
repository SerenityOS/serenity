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
 * @summary Test that OpenMBean*Info.hashCode do not throw NPE
 * @author Shanliang JIANG
 *
 * @run clean OpenMBeanInfoHashCodeNPETest
 * @run build OpenMBeanInfoHashCodeNPETest
 * @run main OpenMBeanInfoHashCodeNPETest
 */
public class OpenMBeanInfoHashCodeNPETest {
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("---OpenMBeanInfoHashCodeNPETest-main ...");

        // ----
        System.out.println("\n---Testing on OpenMBeanInfohashCodeTest...");
        OpenMBeanAttributeInfo openMBeanAttributeInfo = new OpenMBeanAttributeInfoSupport(
                "name", "description", SimpleType.INTEGER, true, true, false, null, new Integer[]{1, 2, 3});
        test(openMBeanAttributeInfo, "defaultValue");

        openMBeanAttributeInfo = new OpenMBeanAttributeInfoSupport(
                "name", "description", SimpleType.INTEGER, true, true, false, 1, null);
        test(openMBeanAttributeInfo, "legalValues");

        // ----
        System.out.println("\n---Testing on OpenMBeanConstructorInfoSupport...");
        OpenMBeanConstructorInfo openMBeanConstructorInfo;

        openMBeanConstructorInfo = new OpenMBeanConstructorInfoSupport(
                "name", "description", null, new DescriptorSupport());
        test(openMBeanConstructorInfo, "sigs");

        openMBeanConstructorInfo = new OpenMBeanConstructorInfoSupport(
                "name", "description", new OpenMBeanParameterInfo[]{}, null);
        test(openMBeanConstructorInfo, "Descriptor");

        // ----
        System.out.println("\n---Testing on OpenMBeanOperationInfoSupport...");
        OpenMBeanOperationInfo openMBeanOperationInfo;

        openMBeanOperationInfo = new OpenMBeanOperationInfoSupport(
                "name", "description", null,  SimpleType.INTEGER, 1, new DescriptorSupport());
        test(openMBeanOperationInfo, "sigs");

        openMBeanOperationInfo = new OpenMBeanOperationInfoSupport(
                "name", "description", new OpenMBeanParameterInfo[]{},  SimpleType.INTEGER, 1, null);
        test(openMBeanOperationInfo, "Descriptor");

        // ----
        System.out.println("\n---Testing on OpenMBeanParameterInfoSupport 1...");
        OpenMBeanParameterInfo openMBeanParameterInfo;

        openMBeanParameterInfo = new OpenMBeanParameterInfoSupport(
                "name", "description", SimpleType.INTEGER, null, -1, 1);
        test(openMBeanParameterInfo, "default value");

        openMBeanParameterInfo = new OpenMBeanParameterInfoSupport(
                "name", "description", SimpleType.INTEGER, 0, null, 1);
        test(openMBeanParameterInfo, "min value");

        openMBeanParameterInfo = new OpenMBeanParameterInfoSupport(
                "name", "description", SimpleType.INTEGER, 0, -1, null);
        test(openMBeanParameterInfo, "max value");

        // ----
        System.out.println("\n---Testing on OpenMBeanParameterInfoSupport 2...");
        openMBeanParameterInfo = new OpenMBeanParameterInfoSupport(
                "name", "description", SimpleType.INTEGER, 1, new Integer[]{-1, 1, 2});

        openMBeanParameterInfo = new OpenMBeanParameterInfoSupport(
                "name", "description", SimpleType.INTEGER, null, new Integer[]{-1, 1, 2});
        test(openMBeanParameterInfo, "default value");

        openMBeanParameterInfo = new OpenMBeanParameterInfoSupport(
                "name", "description", SimpleType.INTEGER, 1, null);
        test(openMBeanParameterInfo, "legal values");

        // ----
        System.out.println("\n---Testing on OpenMBeanInfoSupport...");
        String className = "toto";
        String description = "titi";
        OpenMBeanAttributeInfo[] attrInfos = new OpenMBeanAttributeInfo[]{};
        OpenMBeanConstructorInfo[] constrInfos = new OpenMBeanConstructorInfo[]{};
        OpenMBeanOperationInfo[] operaInfos = new OpenMBeanOperationInfo[]{};
        MBeanNotificationInfo[] notifInfos = new MBeanNotificationInfo[]{};

        OpenMBeanInfo ominfo = new OpenMBeanInfoSupport(null, description, attrInfos, constrInfos, operaInfos, notifInfos);
        test(ominfo, "class name");

        ominfo = new OpenMBeanInfoSupport(className, null, attrInfos, constrInfos, operaInfos, notifInfos);
        test(ominfo, "description");

        ominfo = new OpenMBeanInfoSupport(className, description, null, constrInfos, operaInfos, notifInfos);
        test(ominfo, "attrInfos");

        ominfo = new OpenMBeanInfoSupport(className, description, attrInfos, null, operaInfos, notifInfos);
        test(ominfo, "constructor infos");

        ominfo = new OpenMBeanInfoSupport(className, description, attrInfos, constrInfos, null, notifInfos);
        test(ominfo, "operation infos");

        ominfo = new OpenMBeanInfoSupport(className, description, attrInfos, constrInfos, operaInfos, null);
        test(ominfo, "notif infos");

        if (failed > 0) {
            throw new RuntimeException("Test failed: "+failed);
        } else {
            System.out.println("---Test: PASSED");
        }
    }

    private static void test(Object obj, String param) {
        try {
            obj.hashCode();
            System.out.println("OK-1: "+obj.getClass().getSimpleName()+
                    ".hashCode worked with a null paramer: "+param);
        } catch (NullPointerException npe) {
            System.out.println("--->KO-1!!! "+obj.getClass().getSimpleName()+
                    ".hashCode got NPE with null paramer: "+param);
            npe.printStackTrace();
            failed++;
        }

        try {
            obj.toString();
            System.out.println("OK-1: "+obj.getClass().getSimpleName()+
                    ".toString worked with a null paramer: "+param);
        } catch (NullPointerException npe) {
            System.out.println("--->KO-1!!! "+obj.getClass().getSimpleName()+
                    ".toString got NPE with null paramer: "+param);
            npe.printStackTrace();
            failed++;
        }
    }
}
