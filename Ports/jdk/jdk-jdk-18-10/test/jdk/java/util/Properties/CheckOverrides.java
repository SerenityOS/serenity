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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * @test
 * @bug 8029891
 * @summary Test that the Properties class overrides all public+protected
 *          methods of all ancestor classes and interfaces
 * @run main CheckOverrides
 */
public class CheckOverrides {

    public static void main(String[] args) {
        Set<MethodSignature> pMethodSignatures =
            Stream.of(Properties.class.getDeclaredMethods())
                .filter(CheckOverrides::isMethodOfInterest)
                .map(MethodSignature::new)
                .collect(Collectors.toSet());

        Map<MethodSignature, Method> unoverriddenMethods = new HashMap<>();
        for (Class<?> superclass = Properties.class.getSuperclass();
             superclass != Object.class;
             superclass = superclass.getSuperclass()) {
            Stream.of(superclass.getDeclaredMethods())
                .filter(CheckOverrides::isMethodOfInterest)
                .forEach(m -> unoverriddenMethods.putIfAbsent(new MethodSignature(m), m));
        }
        unoverriddenMethods.keySet().removeAll(pMethodSignatures);

        if (!unoverriddenMethods.isEmpty()) {
            throw new RuntimeException(
                "The following methods should be overridden by Properties class:\n" +
                    unoverriddenMethods.values().stream()
                        .map(Method::toString)
                        .collect(Collectors.joining("\n  ", "  ", "\n"))
            );
        }
    }

    static boolean isMethodOfInterest(Method method) {
        int mods = method.getModifiers();
        return !Modifier.isStatic(mods) &&
            (Modifier.isPublic(mods) || Modifier.isProtected(mods));
    }

    static class MethodSignature {
        final Class<?> returnType;
        final String name;
        final Class<?>[] parameterTypes;

        MethodSignature(Method method) {
            this(method.getReturnType(), method.getName(), method.getParameterTypes());
        }

        private MethodSignature(Class<?> returnType, String name, Class<?>[] parameterTypes) {
            this.returnType = returnType;
            this.name = name;
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodSignature that = (MethodSignature) o;
            if (!returnType.equals(that.returnType)) return false;
            if (!name.equals(that.name)) return false;
            return Arrays.equals(parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            int result = returnType.hashCode();
            result = 31 * result + name.hashCode();
            result = 31 * result + Arrays.hashCode(parameterTypes);
            return result;
        }
    }
}

