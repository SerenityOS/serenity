/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Smoke-test class specializer, used to create BoundMethodHandle classes
 * @compile/module=java.base java/lang/invoke/ClassSpecializerHelper.java
 * @run testng/othervm/timeout=250 -ea -esa ClassSpecializerTest
 */

// Useful diagnostics to try:
//   -Djava.lang.invoke.MethodHandle.TRACE_RESOLVE=true
//   -Djava.lang.invoke.MethodHandle.DUMP_CLASS_FILES=true


import org.testng.annotations.*;
import java.lang.invoke.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.invoke.ClassSpecializerHelper.*;


public class ClassSpecializerTest {
    @Test
    public void testFindSpecies() throws Throwable {
        System.out.println("testFindSpecies");
        System.out.println("test = " + SPEC_TEST);
        ArrayList<Object> args = new ArrayList<>();
        for (int key = 0; key <= Kind.MAX_KEY; key++) {
            Kind k = SpecTest.kind(key);
            System.out.println("k = " + k);
            MethodHandle mh = k.factory();
            System.out.println("k.f = " + mh);
            args.clear();
            for (Class<?> pt : mh.type().parameterList()) {
                args.add(coughUpA(pt));
            }
            args.set(0, key * 1000 + 42);
            Frob f = (Frob) mh.invokeWithArguments(args.toArray());
            assert(f.kind() == k);
            System.out.println("k.f(...) = " + f.toString());
            List<Object> l = f.asList();
            System.out.println("f.l = " + l);
            args.subList(0,1).clear();  // drop label
            assert(args.equals(l));
        }
    }
    private static Object coughUpA(Class<?> pt) throws Throwable {
        if (pt == String.class)  return "foo";
        if (pt.isArray()) return java.lang.reflect.Array.newInstance(pt.getComponentType(), 2);
        if (pt == Integer.class)  return 42;
        if (pt == Double.class)  return 3.14;
        if (pt.isAssignableFrom(List.class))
            return Arrays.asList("hello", "world", "from", pt.getSimpleName());
        return MethodHandles.zero(pt).invoke();
    }
    public static void main(String... av) throws Throwable {
        System.out.println("TEST: ClassSpecializerTest");
        new ClassSpecializerTest().testFindSpecies();
    }
}
