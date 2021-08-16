/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4227192
 * @summary This test verifies that a proxy class can be created with
 * (and defined in) the null class loader.
 * @author Peter Jones
 *
 * @build NullClassLoader
 * @run main NullClassLoader
 */

import java.lang.reflect.*;
import java.util.Observer;

public class NullClassLoader {

    public static void main(String[] args) {

        System.err.println(
            "\nTest creating proxy class with the null class loader.\n");

        try {
            ClassLoader ld = null;
            Class p = Proxy.getProxyClass(ld, new Class[] { Runnable.class, Observer.class });
            System.err.println("proxy class: " + p);

            ClassLoader loader = p.getClassLoader();
            System.err.println("proxy class's class loader: " + loader);

            if (loader != null) {
                throw new RuntimeException(
                    "proxy class not defined in the null class loader");
            }

            System.err.println("\nTEST PASSED");

        } catch (Throwable e) {
            System.err.println("\nTEST FAILED:");
            e.printStackTrace();
            throw new RuntimeException("TEST FAILED: " + e.toString());
        }
    }
}
