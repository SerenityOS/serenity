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
 * @bug 6254721
 * @bug 6360539
 * @summary Test that Open*MBeanInfo classes include "openType" in descriptor
 *          and also test serial compatibility with Java 5.
 * @author Eamonn McManus
 *
 * @run clean OpenTypeDescriptorTest
 * @run build OpenTypeDescriptorTest
 * @run main OpenTypeDescriptorTest
 */

import java.util.Date;
import javax.management.*;
import javax.management.openmbean.*;
import static javax.management.MBeanOperationInfo.ACTION;

public class OpenTypeDescriptorTest {
    public static void main(String[] args) throws Exception {
        Descriptor constraints =
                new ImmutableDescriptor("defaultValue=25",
                                        "minValue=3");
        Date now = new Date();
        Date then = new Date(System.currentTimeMillis() - 86400000);
        final DescriptorRead[] testObjects = {
            new OpenMBeanAttributeInfoSupport("name", "descr",
                                              SimpleType.STRING,
                                              true, false, false),
            new OpenMBeanAttributeInfoSupport("name", "descr",
                                              SimpleType.INTEGER,
                                              false, true, false, constraints),
            new OpenMBeanAttributeInfoSupport("name", "descr",
                                              SimpleType.BOOLEAN,
                                              true, false, false, true),
            new OpenMBeanAttributeInfoSupport("name", "descr",
                                              SimpleType.FLOAT,
                                              true, true, false,
                                              1.0f, 0.0f, 2.0f),
            new OpenMBeanAttributeInfoSupport("name", "descr",
                                              SimpleType.DATE,
                                              true, false, false,
                                              now, new Date[] {now, then}),
            new OpenMBeanParameterInfoSupport("name", "descr",
                                              SimpleType.STRING),
            new OpenMBeanParameterInfoSupport("name", "descr",
                                              SimpleType.INTEGER, constraints),
            new OpenMBeanParameterInfoSupport("name", "descr",
                                              SimpleType.BOOLEAN, true),
            new OpenMBeanParameterInfoSupport("name", "descr",
                                              SimpleType.FLOAT,
                                              1.0f, 0.0f, 2.0f),
            new OpenMBeanParameterInfoSupport("name", "descr",
                                              SimpleType.DATE,
                                              now, new Date[] {now, then}),
            new OpenMBeanOperationInfoSupport("name", "descr", null,
                                              ArrayType.getPrimitiveArrayType(
                                                      int[][].class),
                                              ACTION),
            new OpenMBeanOperationInfoSupport("name", "descr", null,
                                              ArrayType.getArrayType(
                                                      SimpleType.INTEGER),
                                              ACTION, constraints),
        };

        for (DescriptorRead x : testObjects) {
            OpenType descriptorType = (OpenType)
                    x.getDescriptor().getFieldValue("openType");
            OpenType openType;
            if (x instanceof OpenMBeanParameterInfo)
                openType = ((OpenMBeanParameterInfo) x).getOpenType();
            else if (x instanceof OpenMBeanOperationInfo)
                openType = ((OpenMBeanOperationInfo) x).getReturnOpenType();
            else
                throw new Exception("Can't get OpenType for " + x.getClass());
            if (openType.equals(descriptorType))
                System.out.println("OK: " + x);
            else {
                failure("OpenType is " + openType + ", descriptor says " +
                        descriptorType + " for " + x);
            }
        }

        // Serial compatibility test:
        //
        System.out.println("Testing serial compatibility with Java "+
                MBeanFeatureInfoSerialStore.SERIALIZER_VM_VERSION+
                " "+
                MBeanFeatureInfoSerialStore.SERIALIZER_VM_VENDOR);

        for (String key : MBeanFeatureInfoSerialStore.keySet() ) {
            MBeanFeatureInfo f = MBeanFeatureInfoSerialStore.get(key);
            DescriptorRead x = (DescriptorRead)f;
            OpenType descriptorType = (OpenType)
                    x.getDescriptor().getFieldValue("openType");
            OpenType openType;
            if (x instanceof OpenMBeanParameterInfo)
                openType = ((OpenMBeanParameterInfo) x).getOpenType();
            else if (x instanceof OpenMBeanOperationInfo)
                openType = ((OpenMBeanOperationInfo) x).getReturnOpenType();
            else
                throw new Exception("Can't get OpenType for " + key +": "+
                        x.getClass());
            if (openType.equals(descriptorType))
                System.out.println("OK "+key+": " + x);
            else {
                failure("OpenType for "+key+" is " + openType +
                        ", descriptor says " +
                        descriptorType + " for " + x);
            }

        }

        // Test that we get an exception if "openType" in Descriptor
        // doesn't agree with OpenType parameter
        Descriptor d =
                new ImmutableDescriptor(new String[] {"openType"},
                                        new Object[] {SimpleType.STRING});
        for (Type t : Type.values()) {
            try {
                switch (t) {
                    case ATTR:
                        new OpenMBeanAttributeInfoSupport("name", "descr",
                                                          SimpleType.INTEGER,
                                                          true, true, false, d);
                        break;
                    case PARAM:
                        new OpenMBeanParameterInfoSupport("name", "descr",
                                                          SimpleType.INTEGER, d);
                        break;
                    case OPER:
                        new OpenMBeanOperationInfoSupport("name", "descr", null,
                                                          SimpleType.INTEGER,
                                                          ACTION, d);
                        break;
                }
                failure("did not get expected exception for " + t);
            } catch (IllegalArgumentException e) {
                System.out.println("OK: got expected exception for " + t);
            } catch (Exception e) {
                failure("wrong exception for " + t + ": " + e);
            }
        }

        if (failed != null)
            throw new Exception(failed);
        System.out.println("Test passed");
    }

    private static enum Type {ATTR, PARAM, OPER}

    private static void failure(String what) {
        System.out.println("FAILED: what");
        failed = what;
    }

    private static String failed;
}
