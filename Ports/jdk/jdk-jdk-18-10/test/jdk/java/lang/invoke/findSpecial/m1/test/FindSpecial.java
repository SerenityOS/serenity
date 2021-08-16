/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;

/*
 * java.util.Comparator is in java.base.  MyComparator is a Comparator
 * subclass and it's in a different module.
 *
 * Test findSpecial and unreflectSpecial with Comparator and MyComparator
 * as the special caller.
 */
public class FindSpecial {
    private static final Lookup LOOKUP = MethodHandles.lookup();

    public static void main(String... args) throws Throwable {
        findSpecialTest();
        unreflectSpecialTest();
        reflectMethodInvoke();
    }

    static void findSpecialTest() throws Throwable {
        Method m = Comparator.class.getMethod("reversed");
        MethodType mt = MethodType.methodType(m.getReturnType(), m.getParameterTypes());
        // refc and specialCaller are both in java.base
        MethodHandle mh = LOOKUP.findSpecial(Comparator.class, m.getName(), mt, Comparator.class);
        // refc in java.base, specialCaller in this module
        MethodHandle mh1 = LOOKUP.findSpecial(m.getDeclaringClass(), m.getName(), mt,
                                                MyComparator.class);
        Comparator<Object> cmp = new MyComparator();
        // invokespecial to invoke Comparator::reversed.
        Object o = mh.invoke(cmp);
        Object o1 = mh1.invoke(cmp);
    }

    static void unreflectSpecialTest() throws Throwable {
        Method m = Comparator.class.getMethod("reversed");
        // refc and specialCaller are both in java.base
        MethodHandle mh = LOOKUP.unreflectSpecial(m, Comparator.class);
        // refc in java.base, specialCaller in this module
        MethodHandle mh1 = LOOKUP.unreflectSpecial(m, MyComparator.class);
        Comparator<Object> cmp = new MyComparator();
        // invokespecial to invoke Comparator::reversed.
        Object o = mh.invoke(cmp);
        Object o1 = mh1.invoke(cmp);
    }

    static void reflectMethodInvoke() throws Throwable {
        Method m = Comparator.class.getMethod("reversed");
        try {
            // invokevirtual dispatch
            Object o = m.invoke(new MyComparator());
            throw new RuntimeException("should throw an exception");
        } catch (InvocationTargetException e) {
            if (!(e.getCause() instanceof Error &&
                  e.getCause().getMessage().equals("should not reach here"))) {
                throw e.getCause();
            }
        }
    }

    static class MyComparator implements Comparator<Object> {
        public int compare(Object o1, Object o2) {
            return 0;
        }

        @Override
        public Comparator<Object> reversed() {
            throw new Error("should not reach here");
        }
    }
}

