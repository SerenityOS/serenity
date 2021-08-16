/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6636650
 * @summary Uninitialized class loaders should not be a parent of other
 *          class loaders.
 * @run main/othervm -Djava.security.manager=allow UninitializedParent
 */


import java.net.*;

public class UninitializedParent {
    private static ClassLoader loader;
    public static void main(String[] args) throws Exception {
        System.setSecurityManager(new SecurityManager());

        // Create an uninitialized class loader
        try {
            new ClassLoader(null) {
                @Override
                protected void finalize() {
                    loader = this;
                }
            };
        } catch (SecurityException exc) {
            // Expected
        }
        System.gc();
        System.runFinalization();

        // if 'loader' isn't null, need to ensure that it can't be used as
        // parent
        if (loader != null) {
            try {
                // Create a class loader with 'loader' being the parent
                URLClassLoader child = URLClassLoader.newInstance
                    (new URL[0], loader);
                throw new RuntimeException("Test Failed!");
            } catch (SecurityException se) {
                System.out.println("Test Passed: Exception thrown");
            }
        } else {
            System.out.println("Test Passed: Loader is null");
        }
    }
}
