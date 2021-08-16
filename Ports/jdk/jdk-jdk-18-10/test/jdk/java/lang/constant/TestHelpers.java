/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.MethodHandles;

/**
 * TestHelpers
 *
 * @author Brian Goetz
 */
class TestHelpers {
    interface TestInterface {
        public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

        public static final int sf = 3;

        static int sm(int  x) { return 0; }
        default int m(int x) { return 0; }
        private int pm(int x) { return 0; }
        private static int psm(int x) { return 0; }
    }

    static class TestSuperclass {
        public int m(int x) { return -1; }
    }

    static class TestClass extends TestSuperclass implements TestInterface {
        public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

        static int sf;
        int f;

        public TestClass()  {}

        public static int sm(int x) { return x; }
        public int m(int x) { return x; }
        private static int psm(int x) { return x; }
        private int pm(int x) { return x; }
    }
}
