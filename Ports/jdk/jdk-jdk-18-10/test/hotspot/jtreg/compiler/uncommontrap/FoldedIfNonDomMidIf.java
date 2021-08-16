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

/**
 * @test
 * @bug 8201368
 * @summary IfNode::fold_compares() may lead to incorrect execution
 *
 * @run main/othervm -XX:-TieredCompilation -XX:-UseOnStackReplacement -XX:-BackgroundCompilation FoldedIfNonDomMidIf
 *
 */

public class FoldedIfNonDomMidIf {
    public static void main(String[] args) {
        for (int i = 0; i < 20_000; i++) {
            test_helper(0, 0);
            test_helper(20, 0);
            test(12);
        }
        if (test(14) != null) {
            throw new RuntimeException("Incorrect code execution");
        }
    }

    private static Object test(int i) {
        return test_helper(i, 0x42);
    }

    static class A {

    }

    static final MyException myex = new MyException();

    private static Object test_helper(int i, int j) {
        Object res = null;
        try {
            if (i < 10) {
                throw myex;
            }

            if (i == 14) {

            }

            if (i > 15) {
                throw myex;
            }
        } catch (MyException e) {
            if (j == 0x42) {
                res = new A();
            }
        }
        return res;
    }

    private static class MyException extends Exception {
    }
}
