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
 * @bug 4719923
 * @summary Test that MBeanInfo.equals works even for mutable subclasses
 * @author Eamonn McManus
 *
 * @run clean MBeanInfoEqualsTest
 * @run build MBeanInfoEqualsTest
 * @run main MBeanInfoEqualsTest
 */

/* Test that MBeanInfo and its referenced classes implement the equals
   and hashCode methods correctly.  These classes include some magic
   to improve performance based on the classes' immutability.  The
   logic checks that any subclasses encountered remain immutable, and
   falls back on less performant code if not.  */

import javax.management.*;
import java.lang.reflect.*;

public class MBeanInfoEqualsTest {
    // Class just used for reflection
    private static class Toy {
        public Toy() {}
        public Toy(int a, String b) {}
        public int getA() {return 0;}
        public void setA(int a) {}
        public boolean isB() {return false;}
        public void setB(boolean b) {}
        public void run() {}
        public void blah(int a, String b) {}
    }

    static final Class toy = Toy.class;
    static final Constructor con1, con2;
    static final Method getA, setA, isB, setB, run, blah;
    static {
        try {
            con1 = toy.getConstructor(new Class[] {});
            con2 = toy.getConstructor(new Class[] {Integer.TYPE,
                                                   String.class});
            getA = toy.getMethod("getA", new Class[] {});
            setA = toy.getMethod("setA", new Class[] {Integer.TYPE});
            isB  = toy.getMethod("isB",  new Class[] {});
            setB = toy.getMethod("setB", new Class[] {Boolean.TYPE});
            run  = toy.getMethod("run",  new Class[] {});
            blah = toy.getMethod("blah", new Class[] {Integer.TYPE,
                                                      String.class});
        } catch (Exception e) {
            throw new Error(e.getMessage());
        }
    }

    private static final MBeanAttributeInfo
        newMBeanAttributeInfo(String name, String description,
                              Method getter, Method setter) {
        try {
            return new MBeanAttributeInfo(name, description, getter, setter);
        } catch (IntrospectionException e) {
            throw new Error(e.getMessage());
        }
    }

    static final MBeanAttributeInfo
        a1a = new MBeanAttributeInfo("thing", "java.foo.bar", "an attribute",
                                     true, true, false),
        a1b = new MBeanAttributeInfo("thing", "java.foo.bar", "an attribute",
                                     true, true, false),
        a2a = new MBeanAttributeInfo("splob", "java.foo.bar", "an attribute",
                                     true, true, false),
        a2b = new MBeanAttributeInfo(a2a.getName(), a2a.getType(),
                                     a2a.getDescription(), a2a.isReadable(),
                                     a2a.isWritable(), a2a.isIs()),
        a3  = new MBeanAttributeInfo("splob", "java.foo.bar", "a whatsit",
                                     true, true, false),
        a4  = new MBeanAttributeInfo("splob", "java.foo.bar", "a whatsit",
                                     false, true, false),
        a5a = newMBeanAttributeInfo("a", "an attribute", getA, setA),
        a5b = new MBeanAttributeInfo("a", "int", "an attribute",
                                     true, true, false),
        a6a = newMBeanAttributeInfo("a", "an attribute", getA, null),
        a6b = new MBeanAttributeInfo("a", "int", "an attribute",
                                     true, false, false),
        a7a = newMBeanAttributeInfo("a", "an attribute", null, setA),
        a7b = new MBeanAttributeInfo("a", "int", "an attribute",
                                     false, true, false),
        a8a = newMBeanAttributeInfo("b", "an attribute", isB, setB),
        a8b = new MBeanAttributeInfo("b", "boolean", "an attribute",
                                     true, true, true),
        a9a = newMBeanAttributeInfo("b", "an attribute", isB, null),
        a9b = new MBeanAttributeInfo("b", "boolean", "an attribute",
                                     true, false, true);

    static final MBeanParameterInfo
        p1a = new MBeanParameterInfo("thing", "java.foo.bar", "a parameter"),
        p1b = new MBeanParameterInfo("thing", "java.foo.bar", "a parameter"),
        p2  = new MBeanParameterInfo("splob", "java.foo.bar", "a parameter"),
        p3  = new MBeanParameterInfo("thing", "java.foo.bax", "a parameter"),
        p4  = new MBeanParameterInfo("thing", "java.foo.bar", "a whatsit");

    static final MBeanConstructorInfo
        c1a = new MBeanConstructorInfo("a constructor", con1),
        c1b = new MBeanConstructorInfo(c1a.getName(), "a constructor",
                                       new MBeanParameterInfo[0]),
        c1c = new MBeanConstructorInfo(c1a.getName(), c1a.getDescription(),
                                       c1a.getSignature()),
        c1d = new MBeanConstructorInfo(c1a.getName(), c1a.getDescription(),
                                       null),
        c2a = new MBeanConstructorInfo("another constructor", con2),
        c2b = new MBeanConstructorInfo(c2a.getName(),
                                       c2a.getDescription(),
                                       c2a.getSignature()),
        c3a = new MBeanConstructorInfo("conName", "a constructor",
                                       new MBeanParameterInfo[] {p2, p3}),
        c3b = new MBeanConstructorInfo("conName", "a constructor",
                                       new MBeanParameterInfo[] {p2, p3}),
        c4  = new MBeanConstructorInfo("conName", "a constructor",
                                       new MBeanParameterInfo[] {p3, p2}),
        c5  = new MBeanConstructorInfo("otherName", "a constructor",
                                       new MBeanParameterInfo[] {p3, p2}),
        c6  = new MBeanConstructorInfo("otherName", "another constructor",
                                       new MBeanParameterInfo[] {p3, p2});

    static final MBeanOperationInfo
        o1a = new MBeanOperationInfo("an operation", run),
        o1b = new MBeanOperationInfo("an operation", run),
        o1c = new MBeanOperationInfo("run", "an operation",
                                     o1a.getSignature(), "void",
                                     o1a.getImpact()),
        o1d = new MBeanOperationInfo("run", "an operation",
                                     new MBeanParameterInfo[0], "void",
                                     o1a.getImpact()),
        o1e = new MBeanOperationInfo("run", "an operation",
                                     null, "void",
                                     o1a.getImpact()),
        o2a = new MBeanOperationInfo("another operation", blah),
        o2b = new MBeanOperationInfo(o2a.getName(), o2a.getDescription(),
                                     o2a.getSignature(), o2a.getReturnType(),
                                     o2a.getImpact());

    static final MBeanNotificationInfo
        n1a = new MBeanNotificationInfo(new String[] {"a.b", "c.d"},
                                        "x.y.z",
                                        "a notification info"),
        n1b = new MBeanNotificationInfo(new String[] {"a.b", "c.d"},
                                        "x.y.z",
                                        "a notification info"),
        n2a = new MBeanNotificationInfo(new String[] {"a.b", "c.d"},
                                        "x.y.z",
                                        "another notification info"),
        n2b = new MBeanNotificationInfo(n2a.getNotifTypes(),
                                        n2a.getName(),
                                        n2a.getDescription()),
        n3  = new MBeanNotificationInfo(new String[] {"a.b", "c.d"},
                                        "x.y.zz",
                                        "a notification info"),
        n4  = new MBeanNotificationInfo(new String[] {"c.d", "a.b"},
                                        "x.y.z",
                                        "a notification info");

    static final MBeanAttributeInfo[]
        xa1a = {a1a, a2a},
        xa1b = {a1b, a2b},
        xa2a = {a2a, a1a};

    static final MBeanConstructorInfo[]
        xc1a = {c1a, c2a},
        xc1b = {c1b, c2b},
        xc2a = {c2a, c1a};

    static final MBeanOperationInfo[]
        xo1a = {o1a, o2a},
        xo1b = {o1b, o2b},
        xo2a = {o2a, o1a};

    static final MBeanNotificationInfo[]
        xn1a = {n1a, n2a},
        xn1b = {n1b, n2b},
        xn2a = {n2a, n1a};

    static final MBeanInfo
        i1a = new MBeanInfo("a.b.c", "an MBean info", xa1a, xc1a, xo1a, xn1a),
        i1b = new MBeanInfo("a.b.c", "an MBean info", xa1a, xc1a, xo1a, xn1a),
        i1c = new MBeanInfo("a.b.c", "an MBean info", xa1b, xc1b, xo1b, xn1b),
        i1d = new MutableMBeanInfo("a.b.c", "an MBean info", xa1b, xc1b, xo1b,
                                   xn1b),
        i1e = new ImmutableMBeanInfo("a.b.c", "an MBean info", xa1b, xc1b,
                                     xo1b, xn1b),
        i1f = new ImmutableMBeanInfo("a.b.c", "an MBean info", xa1b, xc1b,
                                     xo1b, xn1b),
        i2a = new MBeanInfo("a.b.cc", "an MBean info", xa1a, xc1a, xo1a, xn1a),
        i2b = new MBeanInfo(i2a.getClassName(), i2a.getDescription(),
                            i2a.getAttributes(), i2a.getConstructors(),
                            i2a.getOperations(), i2a.getNotifications()),
        i3  = new MBeanInfo("a.b.c", "another MBean info", xa1a, xc1a, xo1a,
                            xn1a),
        i4  = new MBeanInfo("a.b.c", "an MBean info", xa2a, xc1a, xo1a, xn1a),
        i5  = new MBeanInfo("a.b.c", "an MBean info", xa1a, xc2a, xo1a, xn1a),
        i6  = new MBeanInfo("a.b.c", "an MBean info", xa1a, xc1a, xo2a, xn1a),
        i7  = new MBeanInfo("a.b.c", "an MBean info", xa1a, xc1a, xo1a, xn2a);

    static final Object[][] equivalenceClasses = {
        {a1a, a1b}, {a2a, a2b}, {a3}, {a4}, {a5a, a5b}, {a6a, a6b}, {a7a, a7b},
        {a8a, a8b}, {a9a, a9b},
        {c1a, c1b, c1c, c1d}, {c2a, c2b}, {c3a, c3b}, {c4}, {c5}, {c6},
        {o1a, o1b, o1c, o1d, o1e}, {o2a, o2b},
        {p1a, p1b}, {p2}, {p3}, {p4},
        {n1a, n1b}, {n2a, n2b}, {n3}, {n4},
        {i1a, i1b, i1c, i1d, i1e, i1f}, {i2a, i2b}, {i3}, {i4}, {i5}, {i6},
        {i7},
    };

    private static class ImmutableMBeanInfo extends MBeanInfo {
        ImmutableMBeanInfo(String className,
                           String description,
                           MBeanAttributeInfo[] attributes,
                           MBeanConstructorInfo[] constructors,
                           MBeanOperationInfo[] operations,
                           MBeanNotificationInfo[] notifications) {
            super(className, description, attributes, constructors, operations,
                  notifications);
        }
    }

    /* This class checks that the MBeanInfo.equals() method really
       does call getClassName() etc rather than referring to its
       private fields.  */
    private static class MutableMBeanInfo extends MBeanInfo {
        private final String className;
        private final String description;
        private final MBeanAttributeInfo[] attributes;
        private final MBeanOperationInfo[] operations;
        private final MBeanConstructorInfo[] constructors;
        private final MBeanNotificationInfo[] notifications;

        MutableMBeanInfo(String className,
                         String description,
                         MBeanAttributeInfo[] attributes,
                         MBeanConstructorInfo[] constructors,
                         MBeanOperationInfo[] operations,
                         MBeanNotificationInfo[] notifications) {
            super("bogus", null, null, null, null, null);
            this.className = className;
            this.description = description;
            this.attributes = attributes;
            this.constructors = constructors;
            this.operations = operations;
            this.notifications = notifications;
        }

        public String getClassName() {
            return className;
        }

        public String getDescription() {
            return description;
        }

        public MBeanAttributeInfo[] getAttributes() {
            return attributes;
        }

        public MBeanOperationInfo[] getOperations() {
            return operations;
        }

        public MBeanConstructorInfo[] getConstructors() {
            return constructors;
        }

        public MBeanNotificationInfo[] getNotifications() {
            return notifications;
        }
    }

    private static boolean checkEquals(String what, Object[][] equivs) {
        boolean ok = true;
        /* The equivs array is an array of equivalence classes.  The members
           of each equivalence class must be equal among themselves.
           Each member of each equivalence class must be different from
           each member of each other equivalence class.  */
        for (int ei = 0; ei < equivs.length; ei++) {
            Object[] ec1 = equivs[ei];
            ok &= checkSame(what + " equivalence class " + ei, ec1);
            for (int ej = 0; ej < equivs.length; ej++) {
                if (ei == ej)
                    continue;
                Object[] ec2 = equivs[ej];
                ok &= checkDifferent(what + " equivalence classes " +
                                     ei + " and " + ej, ec1, ec2);
            }
        }
        if (ok)
            System.out.println("equals test for " + what + " passed");
        return ok;
    }

    /* We could simplify this test to compare every element with every
       other and choose whether they are supposed to be the same based
       on whether they are in the same equivalence class.  A bit
       simpler, but so what.  */

    private static boolean checkSame(String what, Object[] equiv) {
        boolean ok = true;
        for (int i = 0; i < equiv.length; i++) {
            final Object o1 = equiv[i];
            for (int j = 0; j < equiv.length; j++) {
                final Object o2 = equiv[j];
                if (!o1.equals(o2)) {
                    System.out.println("equals test: " + what +
                                       ": !obj[" + i +
                                       "].equals(obj[" + j + "])");
                    System.out.println("..." + o1 + "  " + o2);
                    ok = false;
                }
                if (o1.hashCode() != o2.hashCode()) {
                    System.out.println("equals test: " + what +
                                       ": obj[" + i +
                                       "].hashCode() != obj[" + j +
                                       "].hashCode()");
                    System.out.println("..." + o1 + "  " + o2);
                    ok = false;
                }
            }
        }
        return ok;
    }

    private static boolean checkDifferent(String what, Object[] equiv1,
                                          Object[] equiv2) {
        boolean ok = true;
        for (int i = 0; i < equiv1.length; i++) {
            final Object o1 = equiv1[i];
            for (int j = 0; j < equiv2.length; j++) {
                final Object o2 = equiv2[j];
                if (o1.equals(o2)) {
                    System.out.println("equals test " + what + ": obj[" +
                                       i + "].equals(obj[" + j + "])");
                    System.out.println("..." + o1 + "  " + o2);
                    ok = false;
                }
            }
        }
        return ok;
    }

    public static void main(String[] args) throws Exception {
        boolean ok = true;
        ok &= checkEquals("equivalence", equivalenceClasses);
        if (ok) {
            System.out.println("all tests passed");
        } else {
            System.out.println("at least one test failed");
            System.exit(1);
        }
    }
}
