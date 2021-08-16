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

import java.io.PrintStream;

/*
 * Custom system class loader.
 */
public class CustomLoader extends ClassLoader {
    private static PrintStream out = System.out;
    public  static ClassLoader INSTANCE;

    public CustomLoader(ClassLoader classLoader) {
        super("CustomSystemLoader", classLoader);
        assert INSTANCE == null;
        INSTANCE = this;

        // test cases to validate that ClassLoader::getSystemClassLoader
        // is not triggered during custom system class loader initialization
        testEnumValueOf();
    }

    static void testEnumValueOf() {
        TestEnum e = java.lang.Enum.valueOf(TestEnum.class, "C1");
        if (e != TestEnum.C1) {
            throw new RuntimeException("Expected: " + TestEnum.C1 + " got: " + e);
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        out.println("CustomLoader: loading class: " + name);
        return super.loadClass(name);
    }

    static enum TestEnum {
        C1, C2, C3
    }
}
