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

package compiler.tiered;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Callable;

public class MethodHelper {
    /**
     * Gets method from a specified class using its name
     *
     * @param aClass type method belongs to
     * @param name   the name of the method
     * @return {@link Method} that represents corresponding class method
     */
    public static Method getMethod(Class<?> aClass, String name) {
        Method method;
        try {
            method = aClass.getDeclaredMethod(name);
        } catch (NoSuchMethodException e) {
            throw new Error("TESTBUG: Unable to get method " + name, e);
        }
        return method;
    }

    /**
     * Gets {@link Callable} that invokes given method from the given object
     *
     * @param object the object the specified method is invoked from
     * @param name   the name of the method
     */
    public static Callable<Integer> getCallable(Object object, String name) {
        Method method = getMethod(object.getClass(), name);
        return () -> {
            try {
                return Objects.hashCode(method.invoke(object));
            } catch (ReflectiveOperationException e) {
                throw new Error("TESTBUG: Invocation failure", e);
            }
        };
    }
}
