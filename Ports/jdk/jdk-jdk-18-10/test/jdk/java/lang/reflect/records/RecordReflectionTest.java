/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8235369 8235550 8247444
 * @summary reflection test for records
 * @compile RecordReflectionTest.java
 * @run testng/othervm RecordReflectionTest
 * @run testng/othervm/java.security.policy=allPermissions.policy RecordReflectionTest
 */

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.List;
import org.testng.annotations.*;
import static org.testng.Assert.*;

@Test
public class RecordReflectionTest {

    class NoRecord {}

    record R1() {}

    record R2(int i, int j) {}

    record R3(List<String> ls) {}

    record R4(R1 r1, R2 r2, R3 r3) {}

    record R5(String... args) {}

    record R6(long l, String... args) implements java.io.Serializable {}

    record R7(String s1, String s2, String... args) {}

    record R8<A, B>(A a, B b) implements java.io.Serializable { }

    @DataProvider(name = "recordClasses")
    public Object[][] recordClassData() {
        return List.of(R1.class,
                       R2.class,
                       R3.class,
                       R4.class,
                       R5.class,
                       R6.class,
                       R7.class,
                       R8.class)
                   .stream().map(c -> new Object[] {c}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "recordClasses")
    public void testIsRecord(Class<?> cls) {
        String message = cls.toGenericString();
        assertTrue(cls.isRecord());
        assertTrue(cls.getSuperclass() == java.lang.Record.class);
        assertTrue(cls.getRecordComponents() != null);
        assertTrue(message.contains("record"), message);
    }

    @DataProvider(name = "notRecordClasses")
    public Object[][] notRecordClasses() {
        return List.of(NoRecord.class,
                       NoRecord[].class,
                       Record.class,  // java.lang.Record is not itself a record class
                       Record[].class,
                       byte.class,
                       byte[].class,
                       int.class,
                       int[].class,
                       long.class,
                       long[].class)
                   .stream().map(c -> new Object[] {c}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "notRecordClasses")
    public void testNotARecordClass(Class<?> cls) {
        assertFalse(cls.isRecord());
        assertFalse(cls.getSuperclass() == java.lang.Record.class);
        assertTrue(cls.getRecordComponents() == null);
    }

    @DataProvider(name = "reflectionData")
    public Object[][] reflectionData() {
        return new Object[][] {
            new Object[] { new R1(),
                           0,
                           null,
                           null,
                           null },
            new Object[] { new R2(1, 2),
                           2,
                           new Object[]{ 1, 2 },
                           new String[]{ "i", "j" },
                           new String[]{ "int", "int"} },
            new Object[] { new R3(List.of("1")),
                           1,
                           new Object[]{ List.of("1") },
                           new String[]{ "ls" },
                           new String[]{ "java.util.List<java.lang.String>"} },
            new Object[] { new R4(new R1(), new R2(6, 7), new R3(List.of("s"))),
                           3,
                           new Object[]{ new R1(), new R2(6, 7), new R3(List.of("s")) },
                           new String[]{ "r1", "r2", "r3" },
                           new String[]{ R1.class.toString(), R2.class.toString(), R3.class.toString()} },
        };
    }

    @Test(dataProvider = "reflectionData")
    public void testRecordReflection(Object recordOb,
                                     int numberOfComponents,
                                     Object[] values,
                                     String[] names,
                                     String[] signatures)
        throws ReflectiveOperationException
    {
        Class<?> recordClass = recordOb.getClass();
        assertTrue(recordClass.isRecord());
        RecordComponent[] recordComponents = recordClass.getRecordComponents();
        assertEquals(recordComponents.length, numberOfComponents);
        int i = 0;
        for (RecordComponent rc : recordComponents) {
            assertEquals(rc.getName(), names[i]);
            assertEquals(rc.getType(), rc.getAccessor().getReturnType());
            assertEquals(rc.getAccessor().invoke(recordOb), values[i]);
            assertEquals(rc.getAccessor().getGenericReturnType().toString(), signatures[i],
                         String.format("signature of method \"%s\" different from expected signature \"%s\"",
                                 rc.getAccessor().getGenericReturnType(), signatures[i]));
            i++;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.RECORD_COMPONENT, ElementType.FIELD })
    @interface RCA {}

    record AnnotatedRec(@RCA int i) {}

    public void testDeclAnnotationsInRecordComp() throws Throwable {
        Class<?> recordClass = AnnotatedRec.class;
        RecordComponent rc = recordClass.getRecordComponents()[0];
        Annotation[] annos = rc.getAnnotations();
        assertEquals(annos.length, 1);
        assertEquals(annos[0].toString(), "@RecordReflectionTest$RCA()");

        Field f = recordClass.getDeclaredField("i");
        assertEquals(f.getAnnotations().length, 1);
        assertEquals(f.getAnnotations()[0].toString(), annos[0].toString());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE_USE})
    @interface TYPE_USE {}

    record TypeAnnotatedRec(@TYPE_USE int i) {}

    public void testTypeAnnotationsInRecordComp() throws Throwable {
        Class<?> recordClass = TypeAnnotatedRec.class;
        RecordComponent rc = recordClass.getRecordComponents()[0];
        AnnotatedType at = rc.getAnnotatedType();
        Annotation[] annos = at.getAnnotations();
        assertEquals(annos.length, 1);
        assertEquals(annos[0].toString(), "@RecordReflectionTest$TYPE_USE()");

        Field f = recordClass.getDeclaredField("i");
        assertEquals(f.getAnnotatedType().getAnnotations().length, 1);
        assertEquals(f.getAnnotatedType().getAnnotations()[0].toString(), annos[0].toString());
    }

    public void testReadOnlyFieldInRecord() throws Throwable {
        R2 o = new R2(1, 2);
        Class<?> recordClass = R2.class;
        String fieldName = "i";
        Field f = recordClass.getDeclaredField(fieldName);
        assertTrue(f.trySetAccessible());
        assertTrue(f.get(o) != null);
        try {
            f.set(o, null);
            assertTrue(false, "should fail to set " + fieldName);
        } catch (IllegalAccessException e) {
        }
    }

}
