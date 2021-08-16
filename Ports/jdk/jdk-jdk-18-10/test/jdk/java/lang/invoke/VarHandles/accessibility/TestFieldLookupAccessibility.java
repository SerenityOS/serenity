/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8152645 8216558
 * @summary test field lookup accessibility of MethodHandles and VarHandles
 * @compile TestFieldLookupAccessibility.java
 *          pkg/A.java pkg/B_extends_A.java pkg/C.java
 *          pkg/subpkg/B_extends_A.java pkg/subpkg/C.java
 * @run testng/othervm TestFieldLookupAccessibility
 */

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pkg.B_extends_A;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestFieldLookupAccessibility {

    // The set of possible field lookup mechanisms
    enum FieldLookup {
        MH_GETTER() {
            Object lookup(MethodHandles.Lookup l, Field f) throws Exception {
                return l.findGetter(f.getDeclaringClass(), f.getName(), f.getType());
            }

            boolean isAccessible(Field f) {
                return !Modifier.isStatic(f.getModifiers());
            }
        },
        MH_SETTER() {
            Object lookup(MethodHandles.Lookup l, Field f) throws Exception {
                return l.findSetter(f.getDeclaringClass(), f.getName(), f.getType());
            }

            boolean isAccessible(Field f) {
                return !Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers());
            }
        },
        MH_STATIC_GETTER() {
            Object lookup(MethodHandles.Lookup l, Field f) throws Exception {
                return l.findStaticGetter(f.getDeclaringClass(), f.getName(), f.getType());
            }

            boolean isAccessible(Field f) {
                return Modifier.isStatic(f.getModifiers());
            }
        },
        MH_STATIC_SETTER() {
            Object lookup(MethodHandles.Lookup l, Field f) throws Exception {
                return l.findStaticSetter(f.getDeclaringClass(), f.getName(), f.getType());
            }

            boolean isAccessible(Field f) {
                return Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers());
            }
        },
        MH_UNREFLECT_GETTER() {
            Object lookup(MethodHandles.Lookup l, Field f) throws Exception {
                return l.unreflectGetter(f);
            }
        },
        MH_UNREFLECT_GETTER_ACCESSIBLE() {
            Object lookup(MethodHandles.Lookup l, Field f) throws Exception {
                return l.unreflectGetter(cloneAndSetAccessible(f));
            }

            // Setting the accessibility bit of a Field grants access under
            // all conditions for MethodHandle getters.
            Set<String> inaccessibleFields(Set<String> inaccessibleFields) {
                return new HashSet<>();
            }
        },
        MH_UNREFLECT_SETTER() {
            Object lookup(MethodHandles.Lookup l, Field f) throws Exception {
                return l.unreflectSetter(f);
            }

            boolean isAccessible(Field f) {
                return f.isAccessible() && !Modifier.isStatic(f.getModifiers()) || !Modifier.isFinal(f.getModifiers());
            }
        },
        MH_UNREFLECT_SETTER_ACCESSIBLE() {
            Object lookup(MethodHandles.Lookup l, Field f) throws Exception {
                return l.unreflectSetter(cloneAndSetAccessible(f));
            }

            boolean isAccessible(Field f) {
                return !(Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()));
            }

            // Setting the accessibility bit of a Field grants access to non-static
            // final fields for MethodHandle setters.
            Set<String> inaccessibleFields(Set<String>inaccessibleFields) {
                Set<String> result = new HashSet<>();
                inaccessibleFields.stream()
                                  .filter(f -> (f.contains("static") && f.contains("final")))
                                  .forEach(result::add);
                return result;
            }
        },
        VH() {
            Object lookup(MethodHandles.Lookup l, Field f) throws Exception {
                return l.findVarHandle(f.getDeclaringClass(), f.getName(), f.getType());
            }

            boolean isAccessible(Field f) {
                return !Modifier.isStatic(f.getModifiers());
            }
        },
        VH_STATIC() {
            Object lookup(MethodHandles.Lookup l, Field f) throws Exception {
                return l.findStaticVarHandle(f.getDeclaringClass(), f.getName(), f.getType());
            }

            boolean isAccessible(Field f) {
                return Modifier.isStatic(f.getModifiers());
            }
        },
        VH_UNREFLECT() {
            Object lookup(MethodHandles.Lookup l, Field f) throws Exception {
                return l.unreflectVarHandle(f);
            }
        };

        // Look up a handle to a field
        abstract Object lookup(MethodHandles.Lookup l, Field f) throws Exception;

        boolean isAccessible(Field f) {
            return true;
        }

        Set<String> inaccessibleFields(Set<String> inaccessibleFields) {
            return new HashSet<>(inaccessibleFields);
        }

        static Field cloneAndSetAccessible(Field f) throws Exception {
            // Clone to avoid mutating source field
            f = f.getDeclaringClass().getDeclaredField(f.getName());
            f.setAccessible(true);
            return f;
        }
    }

    @DataProvider
    public Object[][] lookupProvider() throws Exception {
        Stream<List<Object>> baseCases = Stream.of(
                // Look up from same package
                List.of(pkg.A.class, pkg.A.lookup(), pkg.A.inaccessibleFields()),
                List.of(pkg.A.class, pkg.A.lookup(), pkg.A.inaccessibleFields()),
                List.of(pkg.A.class, B_extends_A.lookup(), B_extends_A.inaccessibleFields()),
                List.of(pkg.A.class, pkg.C.lookup(), pkg.C.inaccessibleFields()),

                // Look up from sub-package
                List.of(pkg.A.class, pkg.subpkg.B_extends_A.lookup(), pkg.subpkg.B_extends_A.inaccessibleFields()),
                List.of(pkg.A.class, pkg.subpkg.C.lookup(), pkg.subpkg.C.inaccessibleFields())
        );

        // Cross product base cases with the field lookup classes
        return baseCases.
                flatMap(l -> Stream.of(FieldLookup.values()).map(fl -> prepend(fl, l))).
                toArray(Object[][]::new);
    }

    private static Object[] prepend(Object o, List<Object> l) {
        List<Object> pl = new ArrayList<>();
        pl.add(o);
        pl.addAll(l);
        return pl.toArray();
    }

    @Test(dataProvider = "lookupProvider")
    public void test(FieldLookup fl, Class<?> src, MethodHandles.Lookup l, Set<String> inaccessibleFields) {
        // Add to the expected failures all inaccessible fields due to accessibility modifiers
        Set<String> expected = fl.inaccessibleFields(inaccessibleFields);
        Map<Field, Throwable> actual = new HashMap<>();

        for (Field f : fields(src)) {
            // Add to the expected failures all inaccessible fields due to static/final modifiers
            if (!fl.isAccessible(f)) {
                expected.add(f.getName());
            }

            try {
                fl.lookup(l, f);
            }
            catch (Throwable t) {
                // Lookup failed, add to the actual failures
                actual.put(f, t);
            }
        }

        Set<String> actualFieldNames = actual.keySet().stream().map(Field::getName).
                collect(Collectors.toSet());
        if (!actualFieldNames.equals(expected)) {
            if (actualFieldNames.isEmpty()) {
                Assert.assertEquals(actualFieldNames, expected, "No accessibility failures:");
            }
            else {
                Assert.assertEquals(actualFieldNames, expected, "Accessibility failures differ:");
            }
        }
        else {
            if (!actual.values().stream().allMatch(IllegalAccessException.class::isInstance)) {
                Assert.fail("Expecting an IllegalArgumentException for all failures " + actual);
            }
        }
    }

    static List<Field> fields(Class<?> src) {
        return List.of(src.getDeclaredFields());
    }
}
