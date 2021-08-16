/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8149797
 * @summary node replaced by dominating dead cast during parsing
 *
 * @run main/othervm -XX:-UseOnStackReplacement -XX:-BackgroundCompilation
 *      -XX:TypeProfileLevel=200
 *      -XX:CompileCommand=dontinline,compiler.c2.TestDominatingDeadCheckCast::not_inlined
 *      compiler.c2.TestDominatingDeadCheckCast
 */

package compiler.c2;

public class TestDominatingDeadCheckCast {

    static class A {
        int f;
    }

    static class B extends A {
    }

    static A not_inlined() {
        return new A();
    }

    static void inlined(A param) {
        param.f = 42;
    }

    static A field;

    static void test(boolean flag1, boolean flag2, boolean flag3, boolean flag4, boolean flag5) {
        // Go through memory rather than through a local to defeat C2's replace_in_map
        field = not_inlined();
        // Speculation adds a CheckCast on entry of this inlined
        // method for the parameter
        inlined(field);
        // Walk up the dominators is depth limited, make the CheckCast
        // above unreachable from the last inlined call
        if (flag1) {
            if (flag2) {
                if (flag3) {
                    // Speculation adds a CheckCast on entry of this
                    // inlined method for the parameter. This
                    // CheckCast is replaced by the CheckCast of the
                    // first inlined method call but the replaced
                    // CheckCast is still around during parsing.
                    inlined(field);
                    // Same as above, some useless control
                    if (flag4) {
                        if (flag5) {
                            // Speculation adds a CheckCast on entry
                            // of this inlined method for the
                            // parameter. This CheckCast is replaced
                            // by the dead CheckCast of the previous
                            // inlined() call.
                            inlined(field);
                        }
                    }
                }
            }
        }
    }

    static public void main(String[] args) {
        field = new A();
        for (int i = 0; i < 20000; i++) {
            test(true, true, true, true, true);
        }
    }
}
