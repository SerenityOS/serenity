/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8165346
 * @summary Basic test for ClassLoader::getDefinedPackage
 */

public class GetDefinedPackage {
    public static void main(String... args) {
        TestClassLoader loader = new TestClassLoader();
        Package pkg = loader.getDefinedPackage(TestClassLoader.PKG_NAME);
        if (pkg == null) {
            throw new RuntimeException("package foo not found");
        }

        try {
            loader.getDefinedPackage(null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException e) {
        }
    }

    static class TestClassLoader extends ClassLoader {
        public static final String PKG_NAME = "foo";

        public TestClassLoader() {
            super();
            definePackage(PKG_NAME);
        }

        public Package definePackage(String name) {
            return definePackage(name, null, null, null, null, null, null, null);
        }
    }
}
