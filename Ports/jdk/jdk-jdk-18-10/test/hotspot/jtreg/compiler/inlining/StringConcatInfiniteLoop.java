/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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
 * @test 8214862
 * @summary Multiple passes of PhaseRemoveUseless causes infinite loop to be optimized out
 *
 * @run main/othervm -XX:-TieredCompilation -Xcomp -XX:CompileOnly=StringConcatInfiniteLoop::test -XX:CompileCommand=dontinline,*StringBuilder::* StringConcatInfiniteLoop
 *
 */

public class StringConcatInfiniteLoop {
    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder();
        test(sb, "foo", "bar", true);
    }

    private static void test(Object v, String s1, String s2, boolean flag) {
        if (flag) {
            return;
        }
        int i = 0;
        for (; i < 10; i++);
        if (i == 10) {
            v = null;
        }
        StringBuilder sb = new StringBuilder(s1);
        sb.append(s2);
        while (v == null);
    }

    private static class A {
    }
}
