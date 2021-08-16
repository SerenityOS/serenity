/*
 * Copyright (c) 2003, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4838310
 * @summary This test verifies that the restrictions on proxy interface
 * methods with the same signature but different return types are
 * correctly enforced.
 * @author Peter Jones
 *
 * @build GetObject GetSerializable GetCloneable GetArray
 * @run main Test
 */

import java.lang.reflect.Proxy;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Test {

    // additional test cases may be added to both of these lists:

    private static final Class<?>[][] GOOD = {
        { Collection.class },
        { Iterable.class, Collection.class },
        { Iterable.class, Collection.class, List.class },
        { GetSerializable.class, GetCloneable.class, GetArray.class },
        { GetObject.class, GetSerializable.class, GetCloneable.class,
          GetArray.class }
    };

    private static final Class<?>[][] BAD = {
        { Runnable.class, PrivilegedAction.class },
        { GetSerializable.class, GetCloneable.class },
        { GetObject.class, GetSerializable.class, GetCloneable.class }
    };

    public static void main(String[] args) throws Exception {
        System.err.println("\nRegression test for bug 4838310\n");

        ClassLoader loader = Test.class.getClassLoader();

        System.err.println("Testing GOOD combinations:");

        for (int i = 0; i < GOOD.length; i++) {
            Class<?>[] interfaces = GOOD[i];
            System.err.println(Arrays.asList(interfaces));
            Proxy.getProxyClass(loader, interfaces);
            System.err.println("--- OK.");
        }

        System.err.println("Testing BAD combinations:");

        for (int i = 0; i < BAD.length; i++) {
            Class<?>[] interfaces = BAD[i];
            System.err.println(Arrays.asList(interfaces));
            try {
                Proxy.getProxyClass(loader, interfaces);
                throw new RuntimeException(
                    "TEST FAILED: bad combination succeeded");
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                System.err.println("--- OK.");
            }
        }

        System.err.println("TEST PASSED");
    }
}
