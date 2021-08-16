/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8206986
 * @summary Verify behavior when an intersection type is inferred for switch expression.
 * @compile ExpressionSwitchIntersectionTypes.java
 * @run main ExpressionSwitchIntersectionTypes
 */

public class ExpressionSwitchIntersectionTypes<X  extends java.io.Serializable & Runnable> {

    void test1(int i, X x) {
        Runnable r1 = switch (i) {
            default -> x;
        };
        r1.run();
    }

    void test2(int i, X x) {
        (switch (i) {
            default -> x;
        }).run();
    }

    public static void main(String[] args) {
        ExpressionSwitchIntersectionTypes t = new ExpressionSwitchIntersectionTypes();
        try {
            t.test1(0, "");
            throw new AssertionError("Expected exception didn't occur.");
        } catch (ClassCastException ex) {
            //good
        }
        try {
            t.test2(0, "");
            throw new AssertionError("Expected exception didn't occur.");
        } catch (ClassCastException ex) {
            //good
        }
    }

}
