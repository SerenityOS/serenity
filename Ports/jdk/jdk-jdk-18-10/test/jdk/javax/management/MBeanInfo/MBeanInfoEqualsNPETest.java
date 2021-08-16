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

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.openmbean.SimpleType;

/*
 * @test
 * @bug 8023954
 * @summary Test that MBean*Info.equals do not throw NPE
 * @author Shanliang JIANG
 *
 * @run clean MBeanInfoEqualsNPETest
 * @run build MBeanInfoEqualsNPETest
 * @run main MBeanInfoEqualsNPETest
 */
public class MBeanInfoEqualsNPETest {
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("---MBeanInfoEqualsNPETest-main ...");

        // ----
        System.out.println("\n---Testing on MBeanAttributeInfo...");
        MBeanAttributeInfo mbeanAttributeInfo0 = new MBeanAttributeInfo(
                "name", SimpleType.INTEGER.getClassName(), "description", true, true, false);
        MBeanAttributeInfo mbeanAttributeInfo = new MBeanAttributeInfo(
                null, SimpleType.INTEGER.getClassName(), "description", true, true, false);
        test(mbeanAttributeInfo0, mbeanAttributeInfo, "class name");

        mbeanAttributeInfo = new MBeanAttributeInfo(
                "name", null, "description", true, true, false);
        test(mbeanAttributeInfo0, mbeanAttributeInfo, "type");

        mbeanAttributeInfo = new MBeanAttributeInfo(
                "name", SimpleType.INTEGER.getClassName(), null, true, true, false);
        test(mbeanAttributeInfo0, mbeanAttributeInfo, "description");

        // ----
        System.out.println("\n---Testing on MBeanConstructorInfo...");
        MBeanConstructorInfo mbeanConstructorInfo0 = new MBeanConstructorInfo(
                "", "", new MBeanParameterInfo[]{}, new DescriptorSupport());
        MBeanConstructorInfo mbeanConstructorInfo = new MBeanConstructorInfo(
                null, "", new MBeanParameterInfo[]{}, new DescriptorSupport());
        test(mbeanConstructorInfo0, mbeanConstructorInfo, "name");

        mbeanConstructorInfo = new MBeanConstructorInfo(
                "", null, new MBeanParameterInfo[]{}, new DescriptorSupport());
        test(mbeanConstructorInfo0, mbeanConstructorInfo, "description");

        mbeanConstructorInfo = new MBeanConstructorInfo(
                "", "", null, new DescriptorSupport());
        test(mbeanConstructorInfo0, mbeanConstructorInfo, "MBeanParameterInfo");

        mbeanConstructorInfo = new MBeanConstructorInfo(
                "", "", new MBeanParameterInfo[]{}, null);
        test(mbeanConstructorInfo0, mbeanConstructorInfo, "descriptor");

        // ----
        System.out.println("\n---Testing on MBeanOperationInfo...");
        MBeanOperationInfo mbeanOperationInfo0 = new MBeanOperationInfo(
                "name", "description", new MBeanParameterInfo[]{}, "type",
                MBeanOperationInfo.UNKNOWN, new DescriptorSupport());

        MBeanOperationInfo mbeanOperationInfo = new MBeanOperationInfo(
                null, "description", new MBeanParameterInfo[]{}, "type",
                MBeanOperationInfo.UNKNOWN, new DescriptorSupport());
        test(mbeanOperationInfo0, mbeanOperationInfo, "name");

        mbeanOperationInfo = new MBeanOperationInfo(
                "name", null, new MBeanParameterInfo[]{}, "type",
                MBeanOperationInfo.UNKNOWN, new DescriptorSupport());
        test(mbeanOperationInfo0, mbeanOperationInfo, "description");

        mbeanOperationInfo = new MBeanOperationInfo(
                "name", "description", null, "type", 1, new DescriptorSupport());
        test(mbeanOperationInfo0, mbeanOperationInfo, "MBeanParameterInfo");

        mbeanOperationInfo = new MBeanOperationInfo(
                "name", "description", new MBeanParameterInfo[]{}, null,
                MBeanOperationInfo.UNKNOWN, new DescriptorSupport());
        test(mbeanOperationInfo0, mbeanOperationInfo, "type");

        mbeanOperationInfo = new MBeanOperationInfo(
                "name", "description", new MBeanParameterInfo[]{}, null,
                MBeanOperationInfo.UNKNOWN, null);
        test(mbeanOperationInfo0, mbeanOperationInfo, "Descriptor");

        // ----
        System.out.println("\n---Testing on MBeanParameterInfo...");
        MBeanParameterInfo mbeanParameterInfo0 = new MBeanParameterInfo(
                "name", "type", "description", new DescriptorSupport());
        MBeanParameterInfo mbeanParameterInfo = new MBeanParameterInfo(
                null, "type", "description", new DescriptorSupport());
        test(mbeanParameterInfo0, mbeanParameterInfo, "name");

        mbeanParameterInfo = new MBeanParameterInfo(
                "name", null, "description", new DescriptorSupport());
        test(mbeanParameterInfo0, mbeanParameterInfo, "type");

        mbeanParameterInfo = new MBeanParameterInfo(
                "name", "type", null, new DescriptorSupport());
        test(mbeanParameterInfo0, mbeanParameterInfo, "description");

        mbeanParameterInfo = new MBeanParameterInfo(
                "name", "type", "description", null);
        test(mbeanParameterInfo0, mbeanParameterInfo, "Descriptor");

        // ----
        System.out.println("\n---Testing on MBeanFeatureInfo ...");
        MBeanFeatureInfo mbeanFeatureInfo0 = new MBeanFeatureInfo(
                "name", "description", new DescriptorSupport());
        MBeanFeatureInfo mbeanFeatureInfo = new MBeanFeatureInfo(
                null, "description", new DescriptorSupport());
        test(mbeanFeatureInfo0, mbeanFeatureInfo, "name");

        mbeanFeatureInfo = new MBeanFeatureInfo(
                "name", null, new DescriptorSupport());
        test(mbeanParameterInfo0, mbeanParameterInfo, "description");

        mbeanFeatureInfo = new MBeanFeatureInfo(
                "name", "description", null);
        test(mbeanParameterInfo0, mbeanParameterInfo, "Descriptor");

        // ----
        System.out.println("\n---Testing on MBeanInfo...");
        String className = "toto";
        String description = "titi";
        MBeanAttributeInfo[] attrInfos = new MBeanAttributeInfo[]{};
        MBeanConstructorInfo[] constrInfos = new MBeanConstructorInfo[]{};
        MBeanOperationInfo[] operaInfos = new MBeanOperationInfo[]{};
        MBeanNotificationInfo[] notifInfos = new MBeanNotificationInfo[]{};

        MBeanInfo minfo0 = new MBeanInfo("toto", description, attrInfos, constrInfos, operaInfos, notifInfos);
        MBeanInfo minfo = new MBeanInfo(null, description, attrInfos, constrInfos, operaInfos, notifInfos);
        test(minfo0, minfo, "class name");

        minfo = new MBeanInfo(className, null, attrInfos, constrInfos, operaInfos, notifInfos);
        test(minfo0, minfo, "description");

        minfo = new MBeanInfo(className, description, null, constrInfos, operaInfos, notifInfos);
        test(minfo0, minfo, "attrInfos");

        minfo = new MBeanInfo(className, description, attrInfos, null, operaInfos, notifInfos);
        test(minfo0, minfo, "constrInfos");

        minfo = new MBeanInfo(className, description, attrInfos, constrInfos, null, notifInfos);
        test(minfo0, minfo, "operaInfos");

        minfo = new MBeanInfo(className, description, attrInfos, constrInfos, operaInfos, null);
        test(minfo0, minfo, "notifInfos");

        if (failed > 0) {
            throw new RuntimeException("Test failed: "+failed);
        } else {
            System.out.println("---Test: PASSED");
        }
    }

    private static void test(Object obj1, Object obj2, String param) {
        try {
            obj1.equals(obj2);
            System.out.println("OK-1: "+obj1.getClass().getSimpleName()+".equals worked with a null paramer: "+param);
        } catch (NullPointerException npe) {
            System.out.println("--->KO-1!!! "+obj1.getClass().getSimpleName()+".equals got NPE with a null paramer: "+param);
            npe.printStackTrace();
            failed++;
        }

        try {
            obj2.equals(obj1);
            System.out.println("OK-2: "+obj2.getClass().getSimpleName()+".equals worked with a null paramer: "+param);
        } catch (NullPointerException npe) {
            System.out.println("--->KO-2!!! "+obj2.getClass().getSimpleName()+".equals got NPE with a null paramer: "+param);
            npe.printStackTrace();
            failed++;
        }

        try {
            obj1.equals(null);
            obj2.equals(null);

            System.out.println("OK-3: "+obj1.getClass().getSimpleName()+".equals worked with a null field.");
        } catch (NullPointerException npe) {
            System.out.println("--->KO-3!!! "+obj1.getClass().getSimpleName()+".equals got NPE with a null field.");
            npe.printStackTrace();
            failed++;
        }
    }
}
