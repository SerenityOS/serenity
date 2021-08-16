/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8011646
 * @summary SEGV in compiled code with loop predication
 *
 * @run main/othervm  -XX:-TieredCompilation
 *      -XX:CompileCommand=compileonly,java.lang.Object::hashCode
 *      -XX:CompileCommand=compileonly,compiler.intrinsics.object.TestHashCode::m1
 *      compiler.intrinsics.object.TestHashCode
 */

package compiler.intrinsics.object;

public class TestHashCode {
    static class A {
        int i;
    }

    static class B extends A {
    }

    static boolean crash = false;

    static A m2() {
        if (crash) {
            return null;
        }
        return new A();
    }

    static int m1(A aa) {
        int res = 0;
        for (int i = 0; i < 10; i++) {
            A a = m2();
            int j = a.i;
            if (aa instanceof B) {
            }
            res += a.hashCode();
        }
        return res;
    }

    public static void main(String[] args) {
        A a = new A();
        for (int i = 0; i < 20000; i++) {
            m1(a);
        }
        crash = true;
        try {
          m1(a);
        } catch (NullPointerException e) {
            System.out.println("Test passed");
        }
    }
}
