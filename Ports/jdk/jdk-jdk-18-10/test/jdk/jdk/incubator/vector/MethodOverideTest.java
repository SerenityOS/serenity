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
 * @run testng MethodOverideTest
 *
 */

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorShape;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.Vector;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodOverideTest {

    @DataProvider
    public static Object[][] vectorClassesProvider() {
        return Stream.<Class<?>>of(
                ByteVector.class,
                ShortVector.class,
                IntVector.class,
                FloatVector.class,
                DoubleVector.class).
                map(c -> new Object[]{c}).
                toArray(Object[][]::new);
    }

    static List<Object> getConcreteSpeciesInstances(Class<?> primitiveVectorClass) {
        try {
            List<Object> csis = new ArrayList<>();
            for (Field sf : primitiveVectorClass.getFields()) {
                if (VectorSpecies.class.isAssignableFrom(sf.getType())) {
                    csis.add(sf.get(null));
                }
            }
            return csis;
        }
        catch (ReflectiveOperationException e) {
            throw new InternalError(e);
        }
    }

    static List<Class<?>> getConcreteVectorClasses(Class<?> primitiveVectorClass) {
        try {
            List<Class<?>> cvcs = new ArrayList<>();
            for (Object speciesInstance : getConcreteSpeciesInstances(primitiveVectorClass)) {
                    Method zero = primitiveVectorClass.getMethod("zero", VectorSpecies.class);
                    Object vectorInstance = zero.invoke(null, speciesInstance);

                    cvcs.add(vectorInstance.getClass());
            }
            return cvcs;
        }
        catch (ReflectiveOperationException e) {
            throw new InternalError(e);
        }
    }

    static List<Method> getDeclaredPublicAndNonAbstractMethods(Class<?> c) {
        return Stream.of(c.getDeclaredMethods()).
                filter(cc -> Modifier.isPublic(cc.getModifiers())).
                filter(cc -> !Modifier.isAbstract(cc.getModifiers())).
                filter(cc -> !Modifier.isFinal(cc.getModifiers())).
                filter(cc -> !cc.isSynthetic()).
                collect(Collectors.toList());
    }

    static int checkMethods(Class<?> primitiveClass, List<Class<?>> concreteClasses) {
        List<Method> publicNonAbstractMethods = getDeclaredPublicAndNonAbstractMethods(primitiveClass);

        int notOverriddenMethodsCount = 0;
        for (Class<?> cc : concreteClasses) {
            List<Method> overriddenMethods = new ArrayList<>();
            List<Method> notOverriddenMethods = new ArrayList<>();

            for (Method m : publicNonAbstractMethods) {
                try {
                    Method ccm = cc.getDeclaredMethod(m.getName(), m.getParameterTypes());
                    // Method overridden by concrete vector
                    // This method can be made abstract
                    overriddenMethods.add(m);
                }
                catch (NoSuchMethodException e) {
                    // Method implemented on primitive vector but not concrete vector
                    // Method is not intrinsic
                    notOverriddenMethods.add(m);
                }
            }

            System.out.println(cc.getName() + " <- " + primitiveClass.getName());
            System.out.println("--Methods overridden that can be abstract");
            overriddenMethods.stream().forEach(m -> System.out.println("    " + m));

            System.out.println("--Methods not overridden that may need to be so and use intrinsics");
            notOverriddenMethods.stream().forEach(m -> System.out.println("    " + m));
            notOverriddenMethodsCount += notOverriddenMethods.size();
        }

        return notOverriddenMethodsCount;
    }

    @Test(dataProvider = "vectorClassesProvider")
    public void checkMethodsOnPrimitiveVector(Class<?> primitiveVector) {
        int nonIntrinsicMethods = checkMethods(primitiveVector, getConcreteVectorClasses(primitiveVector));

//        Assert.assertEquals(nonIntrinsicMethods, 0);
    }
}
