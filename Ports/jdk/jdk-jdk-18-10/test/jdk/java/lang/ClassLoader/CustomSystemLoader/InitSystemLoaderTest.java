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

/*
 * @test
 * @bug 8179950
 * @build CustomLoader InitSystemLoaderTest
 * @run main/othervm -Djava.system.class.loader=CustomLoader InitSystemLoaderTest
 * @summary Test custom system loader initialization and verify their ancestors
 */

public class InitSystemLoaderTest {
    public static void main(String... args) {
        // check that system class loader is the custom loader
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        if (loader != CustomLoader.INSTANCE) {
            throw new RuntimeException("Expected custom loader: "
                + CustomLoader.INSTANCE + " got: " + loader);
        }

        // parent of the custom loader should be builtin system class loader
        ClassLoader builtinSystemLoader = loader.getParent();
        ClassLoader grandparent = builtinSystemLoader.getParent();
        if (grandparent != ClassLoader.getPlatformClassLoader()) {
            throw new RuntimeException("Expected class loader ancestor: "
                + ClassLoader.getPlatformClassLoader() + " got: " + grandparent);
        }
    }
}
