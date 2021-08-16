/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

public class CheckSealed {

    public static void main(String[] argv) throws Exception {
        boolean fail = true;
        try {
            if ("1".equals(argv[0])) {
                test1();
                test2();
            } else if ("2".equals(argv[0])) {
                test2();
                test1();
            }
        } catch (java.lang.SecurityException e) {
            fail = false;
        }
        if (fail) {
            throw new Exception("Sealing violation undetected.");
        }
    }

    private static void test1() {
        p.A.hello();
    }

    private static void test2() {
        p.B.hello();
    }
}
