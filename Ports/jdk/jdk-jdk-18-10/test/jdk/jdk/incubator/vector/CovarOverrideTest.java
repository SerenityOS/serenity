/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @modules jdk.incubator.vector
 * @run testng CovarOverrideTest
 *
 */

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.Vector;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.testng.Assert.assertTrue;

public class CovarOverrideTest {

    static final Set<String> NON_COVARIENT_RETURNING_METHOD_NAMES_ON_VECTOR =
            Set.of("convert", "check",
                   "convertShape", "reinterpretShape", "castShape",
                   "viewAsIntegralLanes", "viewAsFloatingLanes",
                   // Deprecated methods (renamed to be more explicit):
                   "cast", "reinterpret", "reshape");

    @DataProvider
    public static Object[][] classesProvider() {
        return List.<Class<? extends Vector>>of(
                ByteVector.class,
                ShortVector.class,
                IntVector.class,
                FloatVector.class,
                DoubleVector.class).
                stream().
                map(c -> new Object[]{c}).
                toArray(Object[][]::new);
    }

    static Class<?> getPublicSuper(Class<?> c) {
        String pkg = c.getPackageName();
        for (;;) {
            Class<?> superClass = c.getSuperclass();
            if (!superClass.getPackageName().equals(pkg)) {
                Class<?> ifc = null;
                for (Class<?> ifc1 : c.getInterfaces()) {
                    if (ifc1 == java.lang.reflect.Proxy.class)
                        continue;
                    if (Modifier.isPublic(ifc1.getModifiers())) {
                        assert(ifc == null) : ifc;
                        ifc = ifc1;
                    }
                }
                assert(ifc != null);
                superClass = ifc;
            }
            if (Modifier.isPublic(superClass.getModifiers()))
                return superClass;
            // ByteVector <: package-private AbstractVector <: Vector
            c = superClass;
        }
    }

    static void assertSamePackage(Class<?> c, Class<?> d) {
        String cp = c.getPackageName();
        String dp = d.getPackageName();
        assert cp.equals(dp) : cp + " != " + dp;
    }

    @Test(dataProvider = "classesProvider")
    public void testCovarientOverridesExist(Class<?> c) {
        assert(c != Vector.class &&
               Vector.class.isAssignableFrom(c));
        Class<?> vectorClass = c;
        Class<?> superClass = getPublicSuper(c);
        assertSamePackage(c, superClass);
        List<Method> notFound = new ArrayList<>();
        List<Method> notCovarientlyOverridden = new ArrayList<>();
        for (Method superMethod : getVectorReturningMethods(superClass)) {
            try {
                Method overrideMethod = c.getDeclaredMethod(superMethod.getName(), superMethod.getParameterTypes());
                if (vectorClass != overrideMethod.getReturnType()) {
                    notCovarientlyOverridden.add(overrideMethod);
                }
            }
            catch (NoSuchMethodException e) {
                notFound.add(superMethod);
            }
        }

        if (!notFound.isEmpty()) {
            System.out.println("  Methods not found on sub-type " + c.getName());
            notFound.forEach(m -> System.out.println("    " + str(m)));
        }

        if (!notCovarientlyOverridden.isEmpty()) {
            System.out.println("  Methods not covariently overridden on sub-type " + c.getName());
            notCovarientlyOverridden.forEach(m -> System.out.println("    " + str(m)));
        }

        assertTrue(notFound.isEmpty() && notCovarientlyOverridden.isEmpty());
    }

    static List<Method> getVectorReturningMethods(Class<?> c) {
        var filteredMethods = Stream.of(c.getDeclaredMethods()).
                filter(m -> Modifier.isPublic(m.getModifiers())).
                filter(m -> Vector.class == m.getReturnType());
        if (c == Vector.class || c == VectorSpecies.class) {
            filteredMethods = filteredMethods.
                    filter(m -> !NON_COVARIENT_RETURNING_METHOD_NAMES_ON_VECTOR.contains(m.getName()));
        }
        return filteredMethods.collect(toList());
    }

    @Test(dataProvider = "classesProvider")
    public void testFinalOrAbstract(Class<?> c) {
        List<Method> badMethods = new ArrayList<>();
        List<Method> noForceInline = new ArrayList<>();
        for (var m : getInstanceMethods(c)) {
            boolean pub = Modifier.isPublic(m.getModifiers());
            boolean pri = Modifier.isPrivate(m.getModifiers());
            boolean abs = Modifier.isAbstract(m.getModifiers());
            boolean fin = Modifier.isFinal(m.getModifiers());
            boolean force = false;
            boolean depre = false;
            for (var a : m.getDeclaredAnnotations()) {
                String name = getPublicSuper(a.getClass()).getSimpleName();
                force |= name.equals("ForceInline");
                depre |= name.equals("Deprecated");
            }
            fin |= pri; // privates are finals too
            if (Throwable.class.isAssignableFrom(m.getReturnType())) {
                continue;  // skip exception creators
            }
            if (depre) {
                continue;  // skip @Deprecated
            }
            if (abs == fin) {
                badMethods.add(m);
                continue;
            }
            if (fin != force) {
                noForceInline.add(m);
                continue;
            }
        }

        if (!badMethods.isEmpty()) {
            System.out.println("  Non-abstract, non-final methods of " + c.getName());
            badMethods.forEach(m -> System.out.println("    " + str(m)));
        }

        if (!noForceInline.isEmpty()) {
            System.out.println("  Misplaced @ForceInline on methods of " + c.getName());
            noForceInline.forEach(m -> System.out.println("    " + str(m)));
        }

        assertTrue(badMethods.isEmpty() && noForceInline.isEmpty());
    }

    static List<Method> getInstanceMethods(Class<?> c) {
        var filteredMethods = Stream.of(c.getDeclaredMethods()).
                filter(m -> !m.isBridge()).
                filter(m -> isInstanceMethod(m));
        return filteredMethods.collect(toList());
    }

    static boolean isInstanceMethod(Method m) {
        if (Modifier.isStatic(m.getModifiers())) {
            assert(!Modifier.isFinal(m.getModifiers())) : m;
            return false;
        }
        return true;
    }

    static final String PKGP = Vector.class.getPackageName().replaceAll("[.]", "[.]") + "[.]";
    static final String INCP = "VectorOperators[$]";
    static String str(Method m) {
        String s = m.toString();
        s = s.replaceAll(PKGP, "");
        s = s.replaceAll(INCP, "");
        return s;
    }

}
