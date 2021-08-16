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

/**
 * @test
 * @bug 8219807
 * @summary Test IfNode::up_one_dom() with dead regions.
 * @compile -XDstringConcat=inline TestIfWithDeadRegion.java
 * @run main/othervm -XX:CompileCommand=compileonly,compiler.c2.TestIfWithDeadRegion::test
 *                   compiler.c2.TestIfWithDeadRegion
 */

package compiler.c2;

import java.util.function.Supplier;

public class TestIfWithDeadRegion {

    static String msg;

    static String getString(String s, int i) {
        String current = s + String.valueOf(i);
        System.nanoTime();
        return current;
    }

    static void test(Supplier<String> supplier) {
        msg = supplier.get();
    }

    public static void main(String[] args) {
        for (int i = 0; i < 20_000; ++i) {
            test(() -> getString("Test1", 42));
            test(() -> getString("Test2", 42));
        }
    }
}
