/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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

package package1.package3;

public class Class3 {
    public void testAssert(boolean assertsShouldBeOn) {
        boolean assertsEnabled = false;
        assert(assertsEnabled = true);
        if (assertsEnabled != assertsShouldBeOn)
            throw new RuntimeException("Failure of Asserts Facility.");

        Class3 anonTest =  new Class3() {
            public void testAssert(boolean assertsShouldBeOn) {
                boolean assertsEnabled = false;
                assert(assertsEnabled = true);
                if (assertsEnabled != assertsShouldBeOn)
                    throw new RuntimeException("Failure of Asserts Facility.");
            }
        };
        anonTest.testAssert(assertsShouldBeOn);
    }

    // Named inner class
    public static class Class31 {
        public static void testAssert(boolean assertsShouldBeOn) {
            boolean assertsEnabled = false;
            assert(assertsEnabled = true);
            if (assertsEnabled != assertsShouldBeOn)
                throw new RuntimeException("Failure of Asserts Facility.");
        }
    }
}
