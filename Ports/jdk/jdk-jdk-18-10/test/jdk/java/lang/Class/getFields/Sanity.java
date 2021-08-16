/*
 * Copyright 2014 Google, Inc.  All Rights Reserved.
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

/* @test
 * @bug 8063147
 * @summary Tests for Class.getFields().
 * @run testng Sanity
 */

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class Sanity {
    public interface EmptyInterface {}
    class EmptyClass {}
    interface BI1 {
        public int i = 1;
        int j = 2;
    }
    interface BI2 {
        int k = 1;
    }
    public interface DI extends BI1, BI2, EmptyInterface {
        int m = 5;
    }
    interface DDI extends DI {
        int n = 6;
    }

    public class D extends EmptyClass {
        public int publicDField;
        protected int protectedDField;
        private int privateDField;
    }

    class DD extends D {
        public int publicDDField;
        protected int protectedDDField;
        private int privateDDField;
    }

    public class Universe extends DD implements DDI {
        public int publicUniverseField;
        protected int protectedUniverseField;
        private int privateUniverseField;
    }

    void assertContainsNoFields(Class<?> clazz) {
        assertEquals(clazz.getFields().length, 0);
    }

    @Test
    public void primitiveTypesHaveNoFields() throws Exception {
        assertContainsNoFields(byte.class);
        assertContainsNoFields(char.class);
        assertContainsNoFields(short.class);
        assertContainsNoFields(int.class);
        assertContainsNoFields(long.class);
        assertContainsNoFields(boolean.class);
        assertContainsNoFields(void.class);
        assertContainsNoFields(double.class);
        assertContainsNoFields(float.class);
    }

    @Test
    public void arrayTypesHaveNoFields() throws Exception {
        assertContainsNoFields(byte[].class);
        assertContainsNoFields(Object[].class);
        assertContainsNoFields(java.util.Map[].class);
        assertContainsNoFields(java.util.HashMap[].class);
    }

    @Test
    public void emptyInterfacesHaveNoFields() throws Exception {
        assertContainsNoFields(EmptyInterface.class);
    }

    @Test
    public void emptyClassesHaveNoFields() throws Exception {
        assertContainsNoFields(EmptyClass.class);
        class EmptyLocalClass {}
        assertContainsNoFields(EmptyLocalClass.class);
    }

    void assertContainsFields(Class<?> clazz, int count) {
         assertEquals(clazz.getFields().length, count);
    }

    @Test
    public void checkFieldCounts() throws Exception {
        assertContainsFields(BI1.class, 2);
        assertContainsFields(BI2.class, 1);
        assertContainsFields(DI.class, 4);
        assertContainsFields(DDI.class, 5);
        assertContainsFields(D.class, 1);
        assertContainsFields(DD.class, 2);
        assertContainsFields(Universe.class, 8);
    }

    void assertContainsFields(Class<?> derived, Class<?> base) {
        List<Field> derivedFields = Arrays.asList(derived.getFields());
        List<Field> baseFields = Arrays.asList(base.getFields());
        assertTrue(derivedFields.containsAll(baseFields));
    }

    List<Class<?>> directSupers(Class<?> clazz) {
        List<Class<?>> directSupers = new ArrayList<>();
        directSupers.addAll(Arrays.asList(clazz.getInterfaces()));
        if (clazz.getSuperclass() != null) {
            directSupers.add(clazz.getSuperclass());
        }
        return directSupers;
    }

    void assertContainsSuperFields(Class<?> clazz) {
        for (Class<?> directSuper : directSupers(clazz)) {
            assertContainsFields(clazz, directSuper);
        }
    }

    List<Class<?>> testClasses() {
        List<Class<?>> testClasses = new ArrayList<>();
        testClasses.add(Sanity.class);
        testClasses.addAll(Arrays.asList(Sanity.class.getDeclaredClasses()));
        assertEquals(testClasses.size(), 10);
        return testClasses;
    }

    @Test
    public void fieldsAreInheritedFromSupers() throws Exception {
        for (Class clazz : testClasses()) {
            assertContainsSuperFields(clazz);
        }
    }

    @Test
    public void getFieldIsConsistentWithGetFields() throws Exception {
        for (Class clazz : testClasses()) {
            for (Field field : clazz.getFields()) {
                assertEquals(field, clazz.getField(field.getName()));
            }
        }
    }
}
