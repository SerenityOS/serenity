/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6695810
 * @summary null oop passed to encode_heap_oop_not_null
 *
 * @run main/othervm -Xbatch compiler.c2.Test6695810
 */

package compiler.c2;

public class Test6695810 {
    Test6695810 _t;

    static void test(Test6695810 t1, Test6695810 t2) {
        if (t2 != null)
            t1._t = t2;

        if (t2 != null)
            t1._t = t2;
    }

    public static void main(String[] args) {
        Test6695810 t = new Test6695810();
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 100; j++) {
                test(t, t);
            }
            test(t, null);
        }
        for (int i = 0; i < 10000; i++) {
            test(t, t);
        }
        test(t, null);
    }
}
