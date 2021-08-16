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
 * @bug 6204469
 * @summary Check that descriptors have not broken serial interop.
 * @author Eamonn McManus
 *
 * @run clean MBeanInfoInteropTest SerializedInfo
 * @run build MBeanInfoInteropTest SerializedInfo
 * @run main MBeanInfoInteropTest SerializedInfo
 */

/*
 * When run with just a classname argument ("SerializedInfo" above),
 * this reads the serialized objects from that class.
 * When run with the argument "generate" and a classname, say "SerializedInfo",
 * this creates the class SerializedInfo.java.  The idea is to do that on JDK 5.0
 * then run this test on the latest JDK, or vice versa.  The
 * copy included in this source directory was generated on JDK 5.0
 * so that we continue to check forward interop (serialize on JDK 5,
 * deserialize on JDK 6).  There is no easy way to automate backward
 * interop checking.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import static javax.management.MBeanOperationInfo.*;
import static javax.management.openmbean.SimpleType.INTEGER;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanConstructorInfo;
import javax.management.openmbean.OpenMBeanConstructorInfoSupport;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfo;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;

public class MBeanInfoInteropTest {
    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("generate"))
            generate(args[1]);
        else if (args.length == 1)
            test(args[0]);
        else {
            final String usage =
                "Usage: MBeanInfoInteropTest [generate] ClassName";
            throw new Exception(usage);
        }
    }

    private static void test(String className) throws Exception {
        Class<?> c = Class.forName(className);
        Field f = c.getField("BYTES");
        byte[] bytes = (byte[]) f.get(null);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        boolean matched = true;
        for (Serializable s : objects) {
            Object o = ois.readObject();
            if (!o.equals(s)) {
                showMismatch(o, s);
                matched = false;
            }
        }
        if (!matched)
            throw new Exception("Read objects did not match");
        System.out.println("Test passed");
    }

    private static void showMismatch(Object read, Serializable expected)
    throws Exception {
        String name = "<unknown>";
        Field[] fs = MBeanInfoInteropTest.class.getDeclaredFields();
        for (Field f : fs) {
            if (!Modifier.isStatic(f.getModifiers()))
                continue;
            Object x = f.get(null);
            if (x == expected) {
                name = f.getName();
                break;
            }
        }
        System.out.println("Read object mismatch for field " + name);
        System.out.println("...read:     " + read);
        System.out.println("...expected: " + expected);
    }

    private static void generate(String className) throws Exception {
        System.out.println("Generating " + className + ".java");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        for (Serializable s : objects)
            oos.writeObject(s);
        oos.close();
        byte[] bytes = bos.toByteArray();
        PrintWriter pw = new PrintWriter(className + ".java");
        pw.printf(
            "// Generated by: MBeanInfoInteropTest generate %s\n" +
            "import java.io.*;\n" +
            "public class %s {\n" +
            "public static final byte[] BYTES = {\n", className, className);
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            pw.printf("%d,", b);
            if (i % 16 == 15)
                pw.printf("\n");
        }
        pw.printf("\n");
        pw.printf(
            "};\n" +
            "}\n");
        pw.close();
        System.out.println("...done");
    }

    private static MBeanAttributeInfo mbai;
    private static MBeanParameterInfo mbpi;
    private static MBeanConstructorInfo mbci1, mbci2;
    private static MBeanNotificationInfo mbni1, mbni2;
    private static MBeanOperationInfo mboi1, mboi2;
    private static MBeanInfo mbi1, mbi2;
    private static OpenMBeanAttributeInfoSupport ombai1, ombai2, ombai3, ombai4;
    private static OpenMBeanParameterInfoSupport ombpi1, ombpi2, ombpi3, ombpi4;
    private static OpenMBeanConstructorInfoSupport ombci1, ombci2;
    private static OpenMBeanOperationInfoSupport omboi1, omboi2;
    private static OpenMBeanInfoSupport ombi1, ombi2;
    private static ModelMBeanAttributeInfo mmbai1, mmbai2;
    private static ModelMBeanConstructorInfo mmbci1, mmbci2, mmbci3, mmbci4;
    private static ModelMBeanOperationInfo mmboi1, mmboi2, mmboi3, mmboi4;
    private static ModelMBeanNotificationInfo mmbni1, mmbni2, mmbni3, mmbni4;
    private static ModelMBeanInfoSupport mmbi1, mmbi2, mmbi3, mmbi4;
    private static Serializable[] objects;
    static {
        try {
            init();
        } catch (Exception e) {
            throw new IllegalArgumentException("unexpected", e);
        }
    }
    private static void init() throws Exception {
        mbai =
            new MBeanAttributeInfo("name", "type", "descr", true, false, false);
        mbpi =
            new MBeanParameterInfo("name", "type", "descr");
        MBeanParameterInfo[] params = new MBeanParameterInfo[] {mbpi};
        mbci1 =
            new MBeanConstructorInfo("name", "descr", null);
        mbci2 =
            new MBeanConstructorInfo("name", "descr", params);
        mbni1 =
            new MBeanNotificationInfo(null, "name", "descr");
        mbni2 =
            new MBeanNotificationInfo(new String[] {"type"}, "name", "descr");
        mboi1 =
            new MBeanOperationInfo("name", "descr", null, "type", ACTION);
        mboi2 =
            new MBeanOperationInfo("name", "descr", params, "type", INFO);
        mbi1 =
            new MBeanInfo("class", "descr", null, null, null, null);
        mbi2 =
            new MBeanInfo(
                "class", "descr",
                new MBeanAttributeInfo[] {mbai},
                new MBeanConstructorInfo[] {mbci1, mbci2},
                new MBeanOperationInfo[] {mboi1, mboi2},
                new MBeanNotificationInfo[] {mbni1, mbni2});

        ombai1 =
            new OpenMBeanAttributeInfoSupport("name", "descr", INTEGER,
                                              true, false, false);
        ombai2 =
            new OpenMBeanAttributeInfoSupport("name", "descr", INTEGER,
                                              true, false, false, 5);
        ombai3 =
            new OpenMBeanAttributeInfoSupport("name", "descr", INTEGER,
                                              true, false, false, 5, 1, 6);
        ombai4 =
            new OpenMBeanAttributeInfoSupport("name", "descr", INTEGER,
                                              true, false, false, 5,
                                              new Integer[] {2, 3, 5, 7});
        ombpi1 =
            new OpenMBeanParameterInfoSupport("name1", "descr", INTEGER);
        ombpi2 =
            new OpenMBeanParameterInfoSupport("name2", "descr", INTEGER, 5);
        ombpi3 =
            new OpenMBeanParameterInfoSupport("name3", "descr", INTEGER, 5, 1, 6);
        ombpi4 =
            new OpenMBeanParameterInfoSupport("name4", "descr", INTEGER, 5,
                                              new Integer[] {2, 3, 5, 7});
        OpenMBeanParameterInfo[] oparams = {ombpi1, ombpi2, ombpi3, ombpi4};
        ombci1 =
            new OpenMBeanConstructorInfoSupport("name", "descr", null);
        ombci2 =
            new OpenMBeanConstructorInfoSupport("name", "descr", oparams);
        omboi1 =
            new OpenMBeanOperationInfoSupport("name", "descr", null,
                                              INTEGER, ACTION);
        omboi2 =
            new OpenMBeanOperationInfoSupport("name", "descr", oparams,
                                              INTEGER, ACTION);
        ombi1 =
            new OpenMBeanInfoSupport("class", "descr", null, null, null, null);
        ombi2 =
            new OpenMBeanInfoSupport(
                "class", "descr",
                new OpenMBeanAttributeInfo[] {ombai1, ombai2, ombai3, ombai4},
                new OpenMBeanConstructorInfo[] {ombci1, ombci2},
                new OpenMBeanOperationInfo[] {omboi1, omboi2},
                new MBeanNotificationInfo[] {mbni1, mbni2});

        Descriptor attrd = new DescriptorSupport(new String[] {
            "name=name", "descriptorType=attribute",
        });
        mmbai1 =
            new ModelMBeanAttributeInfo("name", "type", "descr",
                                        true, false, false);
        mmbai2 =
            new ModelMBeanAttributeInfo("name", "type", "descr",
                                        true, false, false, attrd);
        Descriptor constrd = new DescriptorSupport(new String[] {
            "name=name", "descriptorType=operation", "role=constructor",
        });
        mmbci1 =
            new ModelMBeanConstructorInfo("name", "descr", null);
        mmbci2 =
            new ModelMBeanConstructorInfo("name", "descr", null, constrd);
        mmbci3 =
            new ModelMBeanConstructorInfo("name", "descr", params);
        mmbci4 =
            new ModelMBeanConstructorInfo("name", "descr", params, constrd);
        Descriptor operd = new DescriptorSupport(new String[] {
            "name=name", "descriptorType=operation",
        });
        mmboi1 =
            new ModelMBeanOperationInfo("name", "descr", null, "type", ACTION);
        mmboi2 =
            new ModelMBeanOperationInfo("name", "descr", null, "type", ACTION,
                                        operd);
        mmboi3 =
            new ModelMBeanOperationInfo("name", "descr", params, "type", ACTION);
        mmboi4 =
            new ModelMBeanOperationInfo("name", "descr", params, "type", ACTION,
                                        operd);
        Descriptor notifd = new DescriptorSupport(new String[] {
            "name=name", "descriptorType=notification",
        });
        mmbni1 =
            new ModelMBeanNotificationInfo(null, "name", "descr");
        mmbni2 =
            new ModelMBeanNotificationInfo(null, "name", "descr", notifd);
        mmbni3 =
            new ModelMBeanNotificationInfo(new String[] {"type"}, "name", "descr");
        mmbni4 =
            new ModelMBeanNotificationInfo(new String[] {"type"}, "name",
                                           "descr", notifd);
        Descriptor mbeand = new DescriptorSupport(new String[] {
            "name=name", "descriptorType=mbean",
        });
        mmbi1 =
            new ModelMBeanInfoSupport("class", "descr", null, null, null, null);
        mmbi2 =
            new ModelMBeanInfoSupport("class", "descr", null, null, null, null,
                                      mbeand);
        mmbi3 =
            new ModelMBeanInfoSupport(
                "class", "descr",
                new ModelMBeanAttributeInfo[] {mmbai1, mmbai2},
                new ModelMBeanConstructorInfo[] {mmbci1, mmbci2, mmbci3, mmbci4},
                new ModelMBeanOperationInfo[] {mmboi1, mmboi2, mmboi3, mmboi4},
                new ModelMBeanNotificationInfo[] {mmbni1, mmbni2, mmbni3, mmbni4});
        mmbi4 =
            new ModelMBeanInfoSupport(
                "class", "descr",
                new ModelMBeanAttributeInfo[] {mmbai1, mmbai2},
                new ModelMBeanConstructorInfo[] {mmbci1, mmbci2, mmbci3, mmbci4},
                new ModelMBeanOperationInfo[] {mmboi1, mmboi2, mmboi3, mmboi4},
                new ModelMBeanNotificationInfo[] {mmbni1, mmbni2, mmbni3, mmbni4},
                mbeand);

        objects = new Serializable[] {
            mbai, mbpi, mbci1, mbci2, mbni1, mbni2, mboi1, mboi2, mbi1, mbi2,

            ombai1, ombai2, ombai3, ombai4,
            ombpi1, ombpi2, ombpi3, ombpi4,
            ombci1, ombci2,
            omboi1, omboi2,
            ombi1, ombi2,

            mmbai1, mmbai2,
            mmbci1, mmbci2, mmbci3, mmbci4,
            mmboi1, mmboi2, mmboi3, mmboi4,
            mmbni1, mmbni2, mmbni3, mmbni4,
        };
    }
}
