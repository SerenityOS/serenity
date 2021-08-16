/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8257594
 * @summary Test that failing checkcast does not trigger repeated recompilation until cutoff is hit.
 * @requires vm.compiler2.enabled
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -Xbatch -XX:CompileCommand=dontinline,compiler.uncommontrap.TestNullAssertAtCheckCast::test*
 *                   -XX:CompileCommand=inline,compiler.uncommontrap.TestNullAssertAtCheckCast::cast
 *                   -XX:CompileCommand=inline,compiler.uncommontrap.TestNullAssertAtCheckCast::store
 *                   compiler.uncommontrap.TestNullAssertAtCheckCast
 */

package compiler.uncommontrap;

import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;

public class TestNullAssertAtCheckCast {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    private static final int COMP_LEVEL_FULL_OPTIMIZATION = 4;

    static Long cast(Object val) {
        return (Long)val;
    }

    static void test1() {
        try {
            // Always fails
            cast(new Integer(42));
        } catch (ClassCastException cce) {
            // Ignored
        }
    }

    static void test2(Integer val) {
        try {
            // Always fails
            cast(val);
        } catch (ClassCastException cce) {
            // Ignored
        }
    }

    static void store(Object[] array, Object val) {
        array[0] = val;
    }

    static void test3() {
        try {
            // Always fails
            store(new Long[1], new Integer(42));
        } catch (ArrayStoreException cce) {
            // Ignored
        }
    }

    static void test4(Integer val) {
        try {
            // Always fails
            store(new Long[1], val);
        } catch (ArrayStoreException cce) {
            // Ignored
        }
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 1_000_000; ++i) {
            test1();
            test2((i % 2 == 0) ? null : 42);
            test3();
            test4((i % 2 == 0) ? null : 42);
        }
        Method method = TestNullAssertAtCheckCast.class.getDeclaredMethod("test1");
        if (!WB.isMethodCompilable(method, COMP_LEVEL_FULL_OPTIMIZATION, false)) {
            throw new RuntimeException("TestNullAssertAtCheckCast::test1 not compilable");
        }
        method = TestNullAssertAtCheckCast.class.getDeclaredMethod("test2", Integer.class);
        if (!WB.isMethodCompilable(method, COMP_LEVEL_FULL_OPTIMIZATION, false)) {
            throw new RuntimeException("TestNullAssertAtCheckCast::test2 not compilable");
        }
        method = TestNullAssertAtCheckCast.class.getDeclaredMethod("test3");
        if (!WB.isMethodCompilable(method, COMP_LEVEL_FULL_OPTIMIZATION, false)) {
            throw new RuntimeException("TestNullAssertAtCheckCast::test3 not compilable");
        }
        method = TestNullAssertAtCheckCast.class.getDeclaredMethod("test4", Integer.class);
        if (!WB.isMethodCompilable(method, COMP_LEVEL_FULL_OPTIMIZATION, false)) {
            throw new RuntimeException("TestNullAssertAtCheckCast::test4 not compilable");
        }
    }
}

