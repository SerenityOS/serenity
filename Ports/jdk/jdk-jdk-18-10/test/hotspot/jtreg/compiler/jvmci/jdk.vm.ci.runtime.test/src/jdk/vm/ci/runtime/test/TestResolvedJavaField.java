/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @requires vm.jvmci
 * @library ../../../../../
 * @ignore 8249621
 * @modules jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *          java.base/jdk.internal.misc
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:-UseJVMCICompiler jdk.vm.ci.runtime.test.TestResolvedJavaField
 */

package jdk.vm.ci.runtime.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.runtime.test.TestResolvedJavaField.TestClassLoader;

/**
 * Tests for {@link ResolvedJavaField}.
 */
public class TestResolvedJavaField extends FieldUniverse {

    public TestResolvedJavaField() {
    }

    @Test
    public void getModifiersTest() {
        for (Map.Entry<Field, ResolvedJavaField> e : fields.entrySet()) {
            int expected = e.getKey().getModifiers();
            int actual = e.getValue().getModifiers();
            assertEquals(expected, actual);
        }
    }

    @Test
    public void isSyntheticTest() {
        for (Map.Entry<Field, ResolvedJavaField> e : fields.entrySet()) {
            boolean expected = e.getKey().isSynthetic();
            boolean actual = e.getValue().isSynthetic();
            assertEquals(expected, actual);
        }
    }

    @Test
    public void getAnnotationsTest() {
        for (Map.Entry<Field, ResolvedJavaField> e : fields.entrySet()) {
            Annotation[] expected = e.getKey().getAnnotations();
            Annotation[] actual = e.getValue().getAnnotations();
            assertArrayEquals(expected, actual);
        }
    }

    @Test
    public void getAnnotationTest() {
        for (Map.Entry<Field, ResolvedJavaField> e : fields.entrySet()) {
            for (Annotation expected : e.getKey().getAnnotations()) {
                if (expected != null) {
                    Annotation actual = e.getValue().getAnnotation(expected.annotationType());
                    assertEquals(expected, actual);
                }
            }
        }
    }

    private Method findTestMethod(Method apiMethod) {
        String testName = apiMethod.getName() + "Test";
        for (Method m : getClass().getDeclaredMethods()) {
            if (m.getName().equals(testName) && m.getAnnotation(Test.class) != null) {
                return m;
            }
        }
        return null;
    }

    // @formatter:off
    private static final String[] untestedApiMethods = {
        "getDeclaringClass",
        "getOffset",
        "isInternal",
        "isFinal"
    };
    // @formatter:on

    /**
     * Ensures that any new methods added to {@link ResolvedJavaMethod} either have a test written
     * for them or are added to {@link #untestedApiMethods}.
     */
    @Test
    public void testCoverage() {
        Set<String> known = new HashSet<>(Arrays.asList(untestedApiMethods));
        for (Method m : ResolvedJavaField.class.getDeclaredMethods()) {
            if (m.isSynthetic()) {
                continue;
            }
            if (findTestMethod(m) == null) {
                assertTrue("test missing for " + m, known.contains(m.getName()));
            } else {
                assertFalse("test should be removed from untestedApiMethods" + m, known.contains(m.getName()));
            }
        }
    }

    private static final String NON_EXISTENT_CLASS_NAME = "XXXXXXXXXXX";

    static class TestClassLoader extends ClassLoader {

        @Override
        protected Class<?> findClass(final String name) throws ClassNotFoundException {
            if (!name.equals(TypeWithUnresolvedFieldType.class.getName())) {
                return super.findClass(name);
            }
            // copy classfile to byte array
            byte[] classData = null;
            try {
                String simpleName = TypeWithUnresolvedFieldType.class.getSimpleName();
                InputStream is = TypeWithUnresolvedFieldType.class.getResourceAsStream(simpleName + ".class");
                assert is != null;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                byte[] buf = new byte[1024];
                int size;
                while ((size = is.read(buf, 0, buf.length)) != -1) {
                    baos.write(buf, 0, size);
                }
                baos.flush();
                classData = baos.toByteArray();
            } catch (IOException e) {
                Assert.fail("can't access class: " + name);
            }

            // replace all occurrences of "PrintStream" in classfile
            int index = -1;

            while ((index = indexOf(classData, index + 1, "PrintStream")) != -1) {
                replace(classData, index, NON_EXISTENT_CLASS_NAME);
            }

            Class<?> c = defineClass(null, classData, 0, classData.length);
            return c;
        }

        private static int indexOf(byte[] b, int index, String find) {
            for (int i = index; i < b.length; i++) {
                boolean match = true;
                for (int j = i; j < i + find.length(); j++) {
                    if (b[j] != (byte) find.charAt(j - i)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return i;
                }
            }
            return -1;
        }

        private static void replace(byte[] b, int index, String replace) {
            for (int i = index; i < index + replace.length(); i++) {
                b[i] = (byte) replace.charAt(i - index);
            }
        }
    }

    /**
     * Tests that calling {@link ResolvedJavaField#getType()} does not cause a linkage error if the
     * type of the field is not resolvable.
     */
    @Test
    public void testGetType() throws ClassNotFoundException {
        Class<?> c = new TestClassLoader().findClass(TypeWithUnresolvedFieldType.class.getName());
        ResolvedJavaType type = metaAccess.lookupJavaType(c);
        for (ResolvedJavaField field : type.getInstanceFields(false)) {
            assertTrue(field.getName().equals("fieldWithUnresolvableType"));
            field.getType();
            field.toString();
            field.getAnnotations();
        }
    }
}

class TypeWithUnresolvedFieldType {
    /**
     * {@link TestClassLoader} will rewrite the type of this field to "Ljava/io/XXXXXXXXXXX;".
     */
    PrintStream fieldWithUnresolvableType;
}
