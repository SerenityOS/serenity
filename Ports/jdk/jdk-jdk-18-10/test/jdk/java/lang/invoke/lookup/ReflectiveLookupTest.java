/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @bug 8020801
 * @summary Restriction on reflective call to MethodHandles.lookup method
 * @run main java.base/java.lang.LookupTest
 * @run main ReflectiveLookupTest
 * @run main/othervm -Dsun.reflect.noInflation=true ReflectiveLookupTest
 */

import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;

import static java.lang.invoke.MethodType.*;

/*
 * Lookup object can be obtained statically or reflectively.
 */
public class ReflectiveLookupTest {
    public static void main(String... args) throws Throwable {
        // Get a full power lookup
        Lookup lookup1 =  MethodHandles.lookup();
        MethodHandle mh1 = lookup1.findStatic(lookup1.lookupClass(),
                                              "foo",
                                              methodType(String.class));
        assertEquals((String) mh1.invokeExact(), foo());

        Method lookupMethod =  MethodHandles.class.getMethod("lookup");
        System.out.println("reflection method: " + lookupMethod);
        if (!lookupMethod.getName().equals("lookup")) {
            throw new RuntimeException("Unexpected name: " + lookupMethod.getName());
        }

        // Get a full power Lookup reflectively.
        Lookup lookup2 = (Lookup) lookupMethod.invoke(null);
        assertEquals(lookup1.lookupClass(), lookup2.lookupClass());
        assertEquals(lookup1.lookupModes(), lookup2.lookupModes());
        MethodHandle mh2 = lookup2.findStatic(lookup2.lookupClass(),
                                             "foo",
                                              methodType(String.class));
        assertEquals((String) mh2.invokeExact(), foo());
    }

    static String foo() {
        return "foo!";
    }

    static void assertEquals(Object o1, Object o2) {
        if (!o1.equals(o2)) {
            throw new RuntimeException(o1 + " != " + o2);
        }
    }
}

