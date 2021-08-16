/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
 * @bug 8244663
 * @summary Shenandoah: C2 assertion fails in Matcher::collect_null_checks
 * @requires vm.flavor == "server"
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:-TieredCompilation -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:CompileCommand=dontinline,TestShenandoahCmpPAfterCall::not_inlined TestShenandoahCmpPAfterCall
 *
 */

public class TestShenandoahCmpPAfterCall {
    private static Object field1 = new Object();
    private static Object field2 = new Object();
    private static Object o3;
    private static volatile int barrier;

    public static void main(String[] args) {
        for (int i = 0; i < 20_000; i++) {
            test();
        }
    }

    private static void test() {
        Object o1 = null;
        Object o2 = field2;
        try {
            not_inlined();
            o1 = field1;
            if (o1 == o2) {

            }
        } catch (Exception1 ex1) {
            o1 = field1;
            if (o1 == o2) {

            }
        }
        barrier = 42;
        if (o1 == o2) {

        }
    }

    static int count = 0;
    private static void not_inlined() throws Exception1 {
        count++;
        if ((count % 100) == 0) {
            throw new Exception1();
        }
    }

    private static class Exception1 extends Exception {
    }
}
