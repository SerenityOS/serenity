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

package p5;

import p3.Foo;
import p3.Lib;

/**
 * This test verifies jlink support of requires static.
 */
public class Main {
    public static void main(String... args) {
        boolean libPresent = ModuleLayer.boot().findModule("m3").isPresent();
        if (LibHelper.libClassFound != libPresent) {
            throw new RuntimeException("Expected module m3 not in the boot layer");
        }

        if (libPresent) {
            // p3.Lib must be present
            LibHelper.concat("x", "y");
        }
    }

    static class LibHelper {
        @Foo
        static final boolean libClassFound;

        static {
            boolean found = false;
            try {
                Class<?> c = Class.forName("p3.Lib");
                found = true;
            } catch (ClassNotFoundException e) {
            }
            libClassFound = found;
        }

        public static String concat(String x, String y) {
            return Lib.concat(x, y);
        }
    }
}
