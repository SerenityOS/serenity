/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Support class for QualifiedAccess tests.
 */

package pack1;

public class P1 {

    public P1() {}

    protected P1(String s) {}

    public P2 p2 = new P2();

    // Not accessible outside package 'pack1'.
    class P3 {
        // Cannot be accessed via qualified name from 'P3'
        // outside of 'pack1', as 'P3' is not accessible.
        public class P4 {
            // Not accessible outside package 'pack1'.
            class P5 {}
        }
    }

    static class Foo {
        public static class Bar {}
    }

    public Foo a[] = null;

    protected static class Q {
        protected Q (String s) {}
    }

    protected static class R {
        private static class S {
            public static class T {}
        }
    }
}
