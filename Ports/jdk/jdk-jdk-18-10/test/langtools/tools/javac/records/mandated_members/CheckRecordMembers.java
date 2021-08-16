/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * CheckRecordMembers
 *
 * @test
 * @bug 8246774
 * @summary check that the accessors, equals, hashCode and toString methods
 *          work as expected
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main CheckRecordMembers
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.file.PathFileObject;
import combo.ComboTask;

public class CheckRecordMembers extends combo.ComboInstance<CheckRecordMembers> {

    enum FieldTypeKind implements combo.ComboParameter {
        BYTE("byte", byte.class,
             List.of(Byte.MIN_VALUE, (byte) -4, (byte) -1, (byte) 0, (byte) 1, (byte) 4, Byte.MAX_VALUE)),
        SHORT("short", short.class,
              List.of(Short.MIN_VALUE, (short) -4, (short) -1, (short) 0, (short) 1, (short) 4, Short.MAX_VALUE)),
        CHAR("char", char.class,
             List.of(Character.MIN_VALUE, 'a', 'A', 'z', (char) 0, Character.MAX_VALUE)),
        INT("int", int.class,
            List.of(Integer.MIN_VALUE, (int) -4, (int) -1, (int) 0, (int) 1, (int) 4, Integer.MAX_VALUE)),
        LONG("long", long.class,
             List.of(Long.MIN_VALUE, (long) -4, (long) -1, (long) 0, (long) 1, (long) 4, Long.MAX_VALUE)),
        FLOAT("float", float.class,
              List.of(Float.MIN_VALUE, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 0.0f, -0.0f, 1.0f, -1.0f, 2.0f, -2.0f, Float.MAX_VALUE)),
        DOUBLE("double", double.class,
               List.of(Double.MIN_VALUE, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0d, -0.0d, 1.0d, -1.0d, 2.0d, -2.0d, Double.MAX_VALUE)),
        BOOLEAN("boolean", boolean.class,
                List.of(true, false)),
        OBJECT("Object", Object.class,
               Arrays.asList(null, 3, "foo", new String[] {"a"})),
        STRING("String", String.class,
               Arrays.asList(null, "", "foo", "bar"));

        final String retTypeStr;
        final Class<?> clazz;
        final List<Object> dataValues;

        FieldTypeKind(String retTypeStr, Class<?> clazz, List<Object> values) {
            this.retTypeStr = retTypeStr;
            this.clazz = clazz;
            dataValues = values;
        }

        public String expand(String optParameter) {
            return retTypeStr;
        }
    }

    static final String sourceTemplate =
            "public record Data(#{FT0} f0, #{FT1} f1) { }";

    public static void main(String... args) throws Exception {
        new combo.ComboTestHelper<CheckRecordMembers>()
                .withDimension("FT0", (x, t) -> { x.ft0 = t; }, FieldTypeKind.values())
                .withDimension("FT1", (x, t) -> { x.ft1 = t; }, FieldTypeKind.values())
                .run(CheckRecordMembers::new);
    }

    FieldTypeKind ft0, ft1;

    @Override
    public void doWork() throws Throwable {
        newCompilationTask()
                .withSourceFromTemplate("Data", sourceTemplate)
                .generate(this::check);
    }

    void check(ComboTask.Result<Iterable<? extends JavaFileObject>> result) {
        if (result.hasErrors() || result.hasWarnings())
            fail("Compilation errors not expected: " + result.compilationInfo());

        List<Object> f0s = ft0.dataValues;
        List<Object> f1s = ft1.dataValues;

        Iterable<? extends PathFileObject> pfoIt = (Iterable<? extends PathFileObject>) result.get();
        PathFileObject pfo = pfoIt.iterator().next();
        Class<?> clazz;
        Constructor<?> ctor;
        Method getterF0, getterF1, hashCodeMethod, equalsMethod, toStringMethod;
        Field fieldF0, fieldF1;

        try {
            URL[] urls = new URL[] {pfo.getPath().getParent().toUri().toURL()};
            ClassLoader cl = new URLClassLoader(urls);
            clazz = cl.loadClass("Data");

            ctor = clazz.getConstructor(ft0.clazz, ft1.clazz);
            getterF0 = clazz.getMethod("f0");
            getterF1 = clazz.getMethod("f1");
            fieldF0 = clazz.getDeclaredField("f0");
            fieldF1 = clazz.getDeclaredField("f1");
            equalsMethod = clazz.getMethod("equals", Object.class);
            hashCodeMethod = clazz.getMethod("hashCode");
            toStringMethod = clazz.getMethod("toString");

            if (getterF0.getReturnType() != ft0.clazz
                || getterF1.getReturnType() != ft1.clazz
                || fieldF0.getType() != ft0.clazz
                || fieldF1.getType() != ft1.clazz)
                fail("Unexpected field or getter type: " + result.compilationInfo());

            for (Object f0 : f0s) {
                for (Object f1 : f1s) {
                    // Create object
                    Object datum = ctor.newInstance(f0, f1);

                    // Test getters
                    Object actualF0 = getterF0.invoke(datum);
                    Object actualF1 = getterF1.invoke(datum);
                    if (!Objects.equals(f0, actualF0) || !Objects.equals(f1, actualF1))
                        fail(String.format("Getters don't report back right values for %s %s/%s, %s %s/%s",
                                           ft0.clazz, f0, actualF0,
                                           ft1.clazz, f1, actualF1));

                    int hashCode = (int) hashCodeMethod.invoke(datum);
                    int expectedHash = Objects.hash(f0, f1);
                    // @@@ fail
                    if (hashCode != expectedHash) {
                        System.err.println(String.format("Hashcode not as expected: expected=%d, actual=%d",
                                           expectedHash, hashCode));
                    }

                    String toString = (String) toStringMethod.invoke(datum);
                    String expectedToString = String.format("Data[f0=%s, f1=%s]", f0, f1);
                    if (!toString.equals(expectedToString)) {
                        fail(String.format("ToString not as expected: expected=%s, actual=%s",
                                           expectedToString, toString));
                    }

                    // Test equals
                    for (Object f2 : f0s) {
                        for (Object f3 : f1s) {
                            Object other = ctor.newInstance(f2, f3);
                            boolean isEqual = (boolean) equalsMethod.invoke(datum, other);
                            boolean isEqualReverse = (boolean) equalsMethod.invoke(other, datum);
                            boolean f0f2Equal = Objects.equals(f0, f2);
                            boolean f1f3Equal = Objects.equals(f1, f3);
                            if (ft0 == FieldTypeKind.FLOAT) {
                                f0f2Equal = Float.compare((float)f0, (float)f2) == 0;
                            } else if (ft0 == FieldTypeKind.DOUBLE) {
                                f0f2Equal = Double.compare((double)f0, (double)f2) == 0;
                            }
                            if (ft1 == FieldTypeKind.FLOAT) {
                                f1f3Equal = Float.compare((float)f1, (float)f3) == 0;
                            } else if (ft1 == FieldTypeKind.DOUBLE) {
                                f1f3Equal = Double.compare((double)f1, (double)f3) == 0;
                            }
                            boolean shouldEqual = f0f2Equal && f1f3Equal;
                            // @@@ fail
                            if (shouldEqual != isEqual)
                                System.err.println(String.format("Equals not as expected: %s %s/%s, %s %s/%s",
                                                   ft0.clazz, f0, f2,
                                                   ft1.clazz, f1, f3));
                            if (isEqualReverse != isEqual)
                                fail(String.format("Equals not symmetric: %s %s/%s, %s %s/%s",
                                                   ft0.clazz, f0, f2,
                                                   ft1.clazz, f1, f3));

                        }
                    }
                }
            }
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }
}
