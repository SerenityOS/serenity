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

package java.lang;

import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.InvocationTargetException;

import static java.lang.invoke.MethodType.*;

/*
 * Verify that a Lookup object can be obtained statically from java.base
 * but fails when it's obtained via reflection from java.base.
 */
public class LookupTest {
    public static void main(String... args) throws Throwable {
        // Get a full power lookup
        Lookup lookup1 =  MethodHandles.lookup();
        MethodHandle mh1 = lookup1.findStatic(lookup1.lookupClass(),
                                              "foo",
                                              methodType(String.class));
        assertEquals((String) mh1.invokeExact(), foo());

        // access protected member
        MethodHandle mh2 = lookup1.findVirtual(java.lang.ClassLoader.class,
                                               "getPackage",
                                               methodType(Package.class, String.class));
        ClassLoader loader = ClassLoader.getPlatformClassLoader();
        Package pkg = (Package)mh2.invokeExact(loader, "java.lang");
        assertEquals(pkg.getName(), "java.lang");

        // MethodHandles.lookup will fail if it's called reflectively
        try {
            MethodHandles.class.getMethod("lookup").invoke(null);
        } catch (InvocationTargetException e) {
            if (!(e.getCause() instanceof IllegalArgumentException)) {
                throw e.getCause();
            }
        }
    }

    static String foo() { return "foo!"; }

    static void assertEquals(Object o1, Object o2) {
        if (!o1.equals(o2)) {
            throw new RuntimeException(o1 + " != " + o2);
        }
    }
}
