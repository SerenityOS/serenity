/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8227046
 * @summary reflection test for sealed classes
 * @compile --enable-preview -source ${jdk.version} SealedClassesReflectionTest.java
 * @run testng/othervm --enable-preview SealedClassesReflectionTest
 */

import java.lang.annotation.*;
import java.lang.constant.ClassDesc;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import org.testng.annotations.*;
import static org.testng.Assert.*;

@Test
public class SealedClassesReflectionTest {

    sealed class SealedClass1 permits FinalSubclass1, NonSealedSubclass1 {}
    final class FinalSubclass1 extends SealedClass1 {}
    non-sealed class NonSealedSubclass1 extends SealedClass1 {}

    sealed class SealedClass2 permits FinalSubclass2, FinalSubclass3 {}
    final class FinalSubclass2 extends SealedClass2 {}
    final class FinalSubclass3 extends SealedClass2 {}

    sealed class SealedClass3 permits FinalSubclass4, SealedSubclass1 {}
    final class FinalSubclass4 extends SealedClass3  {}
    sealed class SealedSubclass1 extends SealedClass3 permits FinalSubclass5 {}
    final class FinalSubclass5 extends SealedSubclass1 {}

    sealed interface SealedInt1 permits FinalSubclass6, NonSealedSubInt1 {}
    final class FinalSubclass6 implements SealedInt1 {}
    non-sealed interface NonSealedSubInt1 extends SealedInt1 {}

    sealed interface SealedInt2 permits FinalSubclass7, FinalSubclass8 {}
    final class FinalSubclass7 implements SealedInt2 {}
    final class FinalSubclass8 implements SealedInt2 {}

    sealed interface SealedInt3 permits FinalSubclass9, SealedSubInt1, SealedSubclass2 {}
    final class FinalSubclass9 implements SealedInt3  {}
    sealed interface SealedSubInt1 extends SealedInt3 permits FinalSubclass10 {}
    final class FinalSubclass10 implements SealedSubInt1 {}
    sealed class SealedSubclass2 implements SealedInt3 permits NonSealedSubclass2 {}
    non-sealed class NonSealedSubclass2 extends SealedSubclass2 {}

    @DataProvider(name = "sealedClasses")
    public Object[][] sealedClassesData() {
        return List.of(
                SealedClass1.class,
                SealedClass2.class,
                SealedClass3.class,
                SealedSubclass1.class,
                SealedInt1.class,
                SealedInt2.class,
                SealedInt3.class,
                SealedSubInt1.class
        ).stream().map(c -> new Object[] {c}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "sealedClasses")
    public void testSealedClasses(Class<?> cls) {
        assertTrue(cls.isSealed());
        assertTrue(!Modifier.isFinal(cls.getModifiers()));
        assertTrue(cls.getPermittedSubclasses() != null);
        assertTrue(cls.getPermittedSubclasses().length > 0);
    }

    @DataProvider(name = "notSealedClasses")
    public Object[][] notSealedClassesData() {
        return List.of(
                Object.class,
                void.class, Void.class, Void[].class,
                byte.class, byte[].class, Byte.class, Byte[].class,
                short.class, short[].class, Short.class, Short[].class,
                char.class, char[].class, Character.class, Character[].class,
                int.class, int[].class, Integer.class, Integer[].class,
                long.class, long[].class, Long.class, Long[].class,
                float.class, float[].class, Float.class, Float[].class,
                double.class, double[].class, Double.class, Double[].class,
                boolean.class, boolean[].class, Boolean.class, Boolean[].class,
                String.class, String[].class
        ).stream().map(c -> new Object[] {c}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "notSealedClasses")
    public void testNotSealedClasses(Class<?> cls) {
        assertTrue(!cls.isSealed());
        assertTrue(cls.getPermittedSubclasses() == null);
    }

    @DataProvider(name = "non_sealedClasses")
    public Object[][] non_sealedClassesData() {
        return List.of(
                NonSealedSubclass1.class,
                NonSealedSubInt1.class,
                NonSealedSubclass2.class
        ).stream().map(c -> new Object[] {c}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "non_sealedClasses")
    public void testnon_sealedClasses(Class<?> cls) {
        assertTrue(!cls.isSealed());
        assertTrue(!Modifier.isFinal(cls.getModifiers()));
        assertTrue((cls.getSuperclass() != null && cls.getSuperclass().isSealed()) || Arrays.stream(cls.getInterfaces()).anyMatch(Class::isSealed));
        assertTrue(cls.getPermittedSubclasses() == null);
    }

    @DataProvider(name = "reflectionData")
    public Object[][] reflectionData() {
        return new Object[][] {
                new Object[] {
                        SealedClass1.class,
                        2,
                        new String[] {"SealedClassesReflectionTest$FinalSubclass1", "SealedClassesReflectionTest$NonSealedSubclass1"},
                        new Class<?>[] {FinalSubclass1.class, NonSealedSubclass1.class},
                        new SealedStatus[] {SealedStatus.FINAL, SealedStatus.NON_SEALED}},

                new Object[] {
                        SealedClass2.class,
                        2,
                        new String[] {"SealedClassesReflectionTest$FinalSubclass2", "SealedClassesReflectionTest$FinalSubclass3"},
                        new Class<?>[] {FinalSubclass2.class, FinalSubclass3.class},
                        new SealedStatus[] {SealedStatus.FINAL, SealedStatus.FINAL}},

                new Object[] {
                        SealedClass3.class,
                        2,
                        new String[] {"SealedClassesReflectionTest$FinalSubclass4", "SealedClassesReflectionTest$SealedSubclass1"},
                        new Class<?>[] {FinalSubclass4.class, SealedSubclass1.class},
                        new SealedStatus[] {SealedStatus.FINAL, SealedStatus.SEALED}},

                new Object[] {
                        SealedSubclass1.class,
                        1,
                        new String[] {"SealedClassesReflectionTest$FinalSubclass5"},
                        new Class<?>[] {FinalSubclass5.class},
                        new SealedStatus[] {SealedStatus.FINAL}},

                new Object[] {
                        SealedInt1.class,
                        2,
                        new String[] {"SealedClassesReflectionTest$FinalSubclass6", "SealedClassesReflectionTest$NonSealedSubInt1"},
                        new Class<?>[] {FinalSubclass6.class, NonSealedSubInt1.class},
                        new SealedStatus[] {SealedStatus.FINAL, SealedStatus.NON_SEALED}},

                new Object[] {
                        SealedInt2.class,
                        2,
                        new String[] {"SealedClassesReflectionTest$FinalSubclass7", "SealedClassesReflectionTest$FinalSubclass8"},
                        new Class<?>[] {FinalSubclass7.class, FinalSubclass8.class},
                        new SealedStatus[] {SealedStatus.FINAL, SealedStatus.FINAL}},

                new Object[] {
                        SealedInt3.class,
                        3,
                        new String[] {"SealedClassesReflectionTest$FinalSubclass9",
                                "SealedClassesReflectionTest$SealedSubInt1",
                                "SealedClassesReflectionTest$SealedSubclass2"},
                        new Class<?>[] {FinalSubclass9.class, SealedSubInt1.class, SealedSubclass2.class},
                        new SealedStatus[] {SealedStatus.FINAL, SealedStatus.SEALED, SealedStatus.SEALED}},

                new Object[] {
                        SealedSubInt1.class,
                        1,
                        new String[] {"SealedClassesReflectionTest$FinalSubclass10"},
                        new Class<?>[] {FinalSubclass10.class},
                        new SealedStatus[] {SealedStatus.FINAL}},

                new Object[] {
                        SealedSubclass2.class,
                        1,
                        new String[] {"SealedClassesReflectionTest$NonSealedSubclass2"},
                        new Class<?>[] {NonSealedSubclass2.class},
                        new SealedStatus[] {SealedStatus.NON_SEALED}},
        };
    }

    enum SealedStatus {
        SEALED, NON_SEALED, FINAL
    }

    @Test(dataProvider = "reflectionData")
    public void testSealedReflection(Class<?> sealedClass,
                                     int numberOfSubclasses,
                                     String[] subclassDescriptors,
                                     Class<?>[] subclasses,
                                     SealedStatus[] subclassSealedStatus)
            throws ReflectiveOperationException
    {
        assertTrue(sealedClass.isSealed());
        assertTrue(sealedClass.getPermittedSubclasses().length == numberOfSubclasses);
        int i = 0;
        for (Class<?> cd : sealedClass.getPermittedSubclasses()) {
            assertTrue(cd.getName().equals(subclassDescriptors[i]), "expected: " + subclassDescriptors[i] + " found: " + cd.getName());
            i++;
        }
        i = 0;
        for (Class<?> subclass : subclasses) {
            switch (subclassSealedStatus[i++]) {
                case SEALED:
                    assertTrue(subclass.isSealed());
                    break;
                case FINAL:
                    assertTrue(Modifier.isFinal(subclass.getModifiers()));
                    break;
                case NON_SEALED:
                    assertTrue(!subclass.isSealed() && !Modifier.isFinal(subclass.getModifiers()));
                    break;
            }
        }
    }
}
