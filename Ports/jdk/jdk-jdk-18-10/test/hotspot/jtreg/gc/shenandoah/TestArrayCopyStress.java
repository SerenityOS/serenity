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
 *
 */

import java.util.Random;
import jdk.test.lib.Utils;

/*
 * @test
 * @key randomness
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 *
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:TieredStopAtLevel=0 -Xmx16m TestArrayCopyStress
 */
public class TestArrayCopyStress {

    private static final int ARRAY_SIZE = 1000;
    private static final int ITERATIONS = 10000;

    static class Foo {
        int num;

        Foo(int num) {
            this.num = num;
        }
    }

    static class Bar {}

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < ITERATIONS; i++) {
            testConjoint();
        }
    }

    private static void testConjoint() {
        Foo[] array = new Foo[ARRAY_SIZE];
        for (int i = 0; i < ARRAY_SIZE; i++) {
            array[i] = new Foo(i);
        }
        Random rng = Utils.getRandomInstance();
        int src_idx = rng.nextInt(ARRAY_SIZE);
        int dst_idx = rng.nextInt(ARRAY_SIZE);
        int len = rng.nextInt(Math.min(ARRAY_SIZE - src_idx, ARRAY_SIZE - dst_idx));
        System.arraycopy(array, src_idx, array, dst_idx, len);

        for (int i = 0; i < ARRAY_SIZE; i++) {
            if (i >= dst_idx && i < dst_idx + len) {
                assertEquals(array[i].num, i - (dst_idx - src_idx));
            } else {
                assertEquals(array[i].num, i);
            }
        }
    }

    private static void assertEquals(int a, int b) {
        if (a != b) throw new RuntimeException("assert failed");
    }

}
