/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.MethodHandles;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @test
 * @bug 8215510
 * @compile ClassDescTest.java
 * @run testng ClassDescTest
 * @summary unit tests for java.lang.constant.ClassDesc
 */
@Test
public class ClassDescTest extends SymbolicDescTest {

    private void testClassDesc(ClassDesc r) throws ReflectiveOperationException {
        testSymbolicDesc(r);

        // Test descriptor accessor, factory, equals
        assertEquals(r, ClassDesc.ofDescriptor(r.descriptorString()));

        if (!r.descriptorString().equals("V")) {
            assertEquals(r, r.arrayType().componentType());
            // Commutativity: array -> resolve -> componentType -> toSymbolic
            assertEquals(r, ((Class<?>) r.arrayType().resolveConstantDesc(LOOKUP)).getComponentType().describeConstable().orElseThrow());
            // Commutativity: resolve -> array -> toSymbolic -> component type
            assertEquals(r, Array.newInstance(((Class<?>) r.resolveConstantDesc(LOOKUP)), 0).getClass().describeConstable().orElseThrow().componentType());
        }

        if (r.isArray()) {
            assertEquals(r, r.componentType().arrayType());
            assertEquals(r, ((Class<?>) r.resolveConstantDesc(LOOKUP)).getComponentType().describeConstable().orElseThrow().arrayType());
            assertEquals(r, Array.newInstance(((Class<?>) r.componentType().resolveConstantDesc(LOOKUP)), 0).getClass().describeConstable().orElseThrow());
        }
    }

    private void testClassDesc(ClassDesc r, Class<?> c) throws ReflectiveOperationException {
        testClassDesc(r);

        assertEquals(r.resolveConstantDesc(LOOKUP), c);
        assertEquals(c.describeConstable().orElseThrow(), r);
        assertEquals(ClassDesc.ofDescriptor(c.descriptorString()), r);
    }

    public void testSymbolicDescsConstants() throws ReflectiveOperationException {
        int tested = 0;
        Field[] fields = ConstantDescs.class.getDeclaredFields();
        for (Field f : fields) {
            try {
                if (f.getType().equals(ClassDesc.class)
                    && ((f.getModifiers() & Modifier.STATIC) != 0)
                    && ((f.getModifiers() & Modifier.PUBLIC) != 0)) {
                    ClassDesc cr = (ClassDesc) f.get(null);
                    Class c = (Class)cr.resolveConstantDesc(MethodHandles.lookup());
                    testClassDesc(cr, c);
                    ++tested;
                }
            }
            catch (Throwable e) {
                System.out.println(e.getMessage());
                fail("Error testing field " + f.getName(), e);
            }
        }

        assertTrue(tested > 0);
    }

    public void testPrimitiveClassDesc() throws ReflectiveOperationException {
        for (Primitives p : Primitives.values()) {
            List<ClassDesc> descs = List.of(ClassDesc.ofDescriptor(p.descriptor),
                                           p.classDesc,
                                           (ClassDesc) p.clazz.describeConstable().orElseThrow());
            for (ClassDesc c : descs) {
                testClassDesc(c, p.clazz);
                assertTrue(c.isPrimitive());
                assertEquals(p.descriptor, c.descriptorString());
                assertEquals(p.name, c.displayName());
                descs.forEach(cc -> assertEquals(c, cc));
                if (p != Primitives.VOID) {
                    testClassDesc(c.arrayType(), p.arrayClass);
                    assertEquals(c, ((ClassDesc) p.arrayClass.describeConstable().orElseThrow()).componentType());
                    assertEquals(c, p.classDesc.arrayType().componentType());
                }
            }

            for (Primitives other : Primitives.values()) {
                ClassDesc otherDescr = ClassDesc.ofDescriptor(other.descriptor);
                if (p != other)
                    descs.forEach(c -> assertNotEquals(c, otherDescr));
                else
                    descs.forEach(c -> assertEquals(c, otherDescr));
            }
        }
    }

    public void testSimpleClassDesc() throws ReflectiveOperationException {

        List<ClassDesc> stringClassDescs = Arrays.asList(ClassDesc.ofDescriptor("Ljava/lang/String;"),
                                                        ClassDesc.of("java.lang", "String"),
                                                        ClassDesc.of("java.lang.String"),
                                                        ClassDesc.of("java.lang.String").arrayType().componentType(),
                                                        String.class.describeConstable().orElseThrow());
        for (ClassDesc r : stringClassDescs) {
            testClassDesc(r, String.class);
            assertFalse(r.isPrimitive());
            assertEquals("Ljava/lang/String;", r.descriptorString());
            assertEquals("String", r.displayName());
            assertEquals(r.arrayType().resolveConstantDesc(LOOKUP), String[].class);
            stringClassDescs.forEach(rr -> assertEquals(r, rr));
        }

        testClassDesc(ClassDesc.of("java.lang.String").arrayType(), String[].class);
        testClassDesc(ClassDesc.of("java.util.Map").nested("Entry"), Map.Entry.class);

        ClassDesc thisClassDesc = ClassDesc.ofDescriptor("LClassDescTest;");
        assertEquals(thisClassDesc, ClassDesc.of("", "ClassDescTest"));
        assertEquals(thisClassDesc, ClassDesc.of("ClassDescTest"));
        assertEquals(thisClassDesc.displayName(), "ClassDescTest");
        testClassDesc(thisClassDesc, ClassDescTest.class);
    }

    public void testPackageName() {
        assertEquals("com.foo", ClassDesc.of("com.foo.Bar").packageName());
        assertEquals("com.foo", ClassDesc.of("com.foo.Bar").nested("Baz").packageName());
        assertEquals("", ClassDesc.of("Bar").packageName());
        assertEquals("", ClassDesc.of("Bar").nested("Baz").packageName());
        assertEquals("", ClassDesc.of("Bar").nested("Baz", "Foo").packageName());

        assertEquals("", ConstantDescs.CD_int.packageName());
        assertEquals("", ConstantDescs.CD_int.arrayType().packageName());
        assertEquals("", ConstantDescs.CD_String.arrayType().packageName());
        assertEquals("", ClassDesc.of("Bar").arrayType().packageName());
    }

    private void testBadArrayRank(ClassDesc cr) {
        try {
            cr.arrayType(-1);
            fail("");
        } catch (IllegalArgumentException e) {
            // good
        }
        try {
            cr.arrayType(0);
            fail("");
        } catch (IllegalArgumentException e) {
            // good
        }
    }

    public void testArrayClassDesc() throws ReflectiveOperationException {
        for (String d : basicDescs) {
            ClassDesc a0 = ClassDesc.ofDescriptor(d);
            ClassDesc a1 = a0.arrayType();
            ClassDesc a2 = a1.arrayType();

            testClassDesc(a0);
            testClassDesc(a1);
            testClassDesc(a2);
            assertFalse(a0.isArray());
            assertTrue(a1.isArray());
            assertTrue(a2.isArray());
            assertFalse(a1.isPrimitive());
            assertFalse(a2.isPrimitive());
            assertEquals(a0.descriptorString(), d);
            assertEquals(a1.descriptorString(), "[" + a0.descriptorString());
            assertEquals(a2.descriptorString(), "[[" + a0.descriptorString());

            assertNull(a0.componentType());
            assertEquals(a0, a1.componentType());
            assertEquals(a1, a2.componentType());

            assertNotEquals(a0, a1);
            assertNotEquals(a1, a2);

            assertEquals(a1, ClassDesc.ofDescriptor("[" + d));
            assertEquals(a2, ClassDesc.ofDescriptor("[[" + d));
            assertEquals(classToDescriptor((Class<?>) a0.resolveConstantDesc(LOOKUP)), a0.descriptorString());
            assertEquals(classToDescriptor((Class<?>) a1.resolveConstantDesc(LOOKUP)), a1.descriptorString());
            assertEquals(classToDescriptor((Class<?>) a2.resolveConstantDesc(LOOKUP)), a2.descriptorString());

            testBadArrayRank(ConstantDescs.CD_int);
            testBadArrayRank(ConstantDescs.CD_String);
            testBadArrayRank(ClassDesc.of("Bar"));
        }
    }

    public void testBadClassDescs() {
        List<String> badDescriptors = List.of("II", "I;", "Q", "L", "",
                                              "java.lang.String", "[]", "Ljava/lang/String",
                                              "Ljava.lang.String;", "java/lang/String");

        for (String d : badDescriptors) {
            try {
                ClassDesc constant = ClassDesc.ofDescriptor(d);
                fail(d);
            }
            catch (IllegalArgumentException e) {
                // good
            }
        }

        List<String> badBinaryNames = List.of("I;", "[]", "Ljava/lang/String",
                "Ljava.lang.String;", "java/lang/String");
        for (String d : badBinaryNames) {
            try {
                ClassDesc constant = ClassDesc.of(d);
                fail(d);
            } catch (IllegalArgumentException e) {
                // good
            }
        }

        for (Primitives p : Primitives.values()) {
            testBadNestedClasses(ClassDesc.ofDescriptor(p.descriptor), "any");
            testBadNestedClasses(ClassDesc.ofDescriptor(p.descriptor), "any", "other");
        }

        ClassDesc stringDesc = ClassDesc.ofDescriptor("Ljava/lang/String;");
        ClassDesc stringArrDesc = stringDesc.arrayType(255);
        try {
            ClassDesc arrGreaterThan255 = stringArrDesc.arrayType();
            fail("can't create an array type descriptor with more than 255 dimensions");
        } catch (IllegalStateException e) {
            // good
        }
        String descWith255ArrayDims = new String(new char[255]).replace('\0', '[');
        try {
            ClassDesc arrGreaterThan255 = ClassDesc.ofDescriptor(descWith255ArrayDims + "[Ljava/lang/String;");
            fail("can't create an array type descriptor with more than 255 dimensions");
        } catch (IllegalArgumentException e) {
            // good
        }
        try {
            ClassDesc arrWith255Dims = ClassDesc.ofDescriptor(descWith255ArrayDims + "Ljava/lang/String;");
            arrWith255Dims.arrayType(1);
            fail("can't create an array type descriptor with more than 255 dimensions");
        } catch (IllegalArgumentException e) {
            // good
        }
    }

    private void testBadNestedClasses(ClassDesc cr, String firstNestedName, String... moreNestedNames) {
        try {
            cr.nested(firstNestedName, moreNestedNames);
            fail("");
        } catch (IllegalStateException e) {
            // good
        }
    }

    public void testLangClasses() {
        Double d = 1.0;
        assertEquals(d.resolveConstantDesc(LOOKUP), d);
        assertEquals(d.describeConstable().get(), d);

        Integer i = 1;
        assertEquals(i.resolveConstantDesc(LOOKUP), i);
        assertEquals(i.describeConstable().get(), i);

        Float f = 1.0f;
        assertEquals(f.resolveConstantDesc(LOOKUP), f);
        assertEquals(f.describeConstable().get(), f);

        Long l = 1L;
        assertEquals(l.resolveConstantDesc(LOOKUP), l);
        assertEquals(l.describeConstable().get(), l);

        String s = "";
        assertEquals(s.resolveConstantDesc(LOOKUP), s);
        assertEquals(s.describeConstable().get(), s);
    }

    public void testNullNestedClasses() {
        ClassDesc cd = ClassDesc.of("Bar");
        try {
            cd.nested(null);
            fail("");
        } catch (NullPointerException e) {
            // good
        }

        try {
            cd.nested("good", null);
            fail("");
        } catch (NullPointerException e) {
            // good
        }

        try {
            cd.nested("good", "goodToo", null);
            fail("");
        } catch (NullPointerException e) {
            // good
        }
    }
}
