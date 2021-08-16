/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8220416
 * @summary This test uses a class pointer comparison optimization which was not applied anymore since JDK-6964458 resulting in a better IR.
 *
 * @run main/othervm -Xbatch -XX:-TieredCompilation -XX:+IgnoreUnrecognizedVMOptions -XX:+AlwaysIncrementalInline
 *                   -XX:CompileOnly=compiler.c2.CompareKlassPointersTest::test compiler.c2.CompareKlassPointersTest
 */

package compiler.c2;



public class CompareKlassPointersTest {

    static A a;

    public static void main(String[] args) {
        a = new C();
        for (int i = 0; i < 10_000; i++) {
            test();
        }
    }

    public static int test() {
        Object a = getA();

        /*
         * This check is now optimized away which was not the case before anymore since JDK-6964458:
         * The klass pointer comparison optimization sees that the check is always false since a and B are always unrelated klasses
         */
        if (a instanceof B) {
            return 1;
        }
        return 0;
    }

    private static Object getA() {
        return a;
    }
}

class A { }

class B { }

class C extends A { }
