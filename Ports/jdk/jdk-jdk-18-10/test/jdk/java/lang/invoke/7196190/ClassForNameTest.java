/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7196190
 * @summary Improve method of handling MethodHandles
 *
 * @run main/othervm ClassForNameTest
 */

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ClassForNameTest {
    static final String NAME = ClassForNameTest.class.getName();

    public static void main(String[] args) throws Throwable {
        {
            final MethodType mt = MethodType.methodType(Class.class, String.class);
            final MethodHandle mh = MethodHandles.lookup()
                    .findStatic(Class.class, "forName", mt);

            Class.forName(NAME);

            mh.invoke(NAME);
            mh.bindTo(NAME).invoke();
            mh.invokeWithArguments(Arrays.asList(NAME));
            mh.invokeWithArguments(NAME);
            Class cls = (Class) mh.invokeExact(NAME);
        }

        {
            final Method fnMethod = Class.class.getMethod("forName", String.class);
            final MethodType mt = MethodType.methodType(Object.class, Object.class, Object[].class);
            final MethodHandle mh = MethodHandles.lookup()
                    .findVirtual(Method.class, "invoke", mt)
                    .bindTo(fnMethod);

            fnMethod.invoke(null, NAME);

            mh.bindTo(null).bindTo(new Object[]{NAME}).invoke();
            mh.invoke(null, new Object[]{NAME});
            mh.invokeWithArguments(null, new Object[]{NAME});
            mh.invokeWithArguments(Arrays.asList(null, new Object[]{NAME}));
            Object obj = mh.invokeExact((Object) null, new Object[]{NAME});
        }

        {
            final Method fnMethod = Class.class.getMethod("forName", String.class);
            final MethodType mt = MethodType.methodType(Object.class, Object.class, Object[].class);

            final MethodHandle mh = MethodHandles.lookup()
                    .bind(fnMethod, "invoke", mt);

            mh.bindTo(null).bindTo(new Object[]{NAME}).invoke();
            mh.invoke(null, new Object[]{NAME});
            mh.invokeWithArguments(null, NAME);
            mh.invokeWithArguments(Arrays.asList(null, NAME));
            Object obj = mh.invokeExact((Object) null, new Object[]{NAME});
        }

        {
            final Method fnMethod = Class.class.getMethod("forName", String.class);
            final MethodHandle mh = MethodHandles.lookup().unreflect(fnMethod);

            mh.bindTo(NAME).invoke();
            mh.invoke(NAME);
            mh.invokeWithArguments(NAME);
            mh.invokeWithArguments(Arrays.asList(NAME));
            Class cls = (Class) mh.invokeExact(NAME);
        }

        System.out.println("TEST PASSED");
    }
}
