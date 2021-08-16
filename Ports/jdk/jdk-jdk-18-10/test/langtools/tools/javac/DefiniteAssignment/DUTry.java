/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @test %E
 * @bug 4458717
 * @summary Check correct handling of DU in try statements
 * @author Neal Gafter (gafter)
 *
 * @run compile/fail DUTry.java
 */

class DUTry {
    void foo() {
        int c = 1;
        int a = 3;
        final int a1;

        try {
            if (a == 3)
                throw new Exception();
        } catch (Throwable e) {
            System.out.println(e);
            a1 = 6;
            System.out.println(a1);
        } finally {
            c = (a1=8) - 1;
            System.out.println(a1);
        }
    }
}
