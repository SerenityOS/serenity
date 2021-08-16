/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1;

/*
 * @test
 * @bug 8169703
 * @summary Regression test to ensure AlwaysPreTouch with multiple threads works at mutator time.
 * Allocates a few humongous objects that will be allocated by expanding the heap, causing concurrent parallel
 * pre-touch.
 * @requires vm.gc.G1
 * @run main/othervm -XX:+UseG1GC -Xms10M -Xmx100m -XX:G1HeapRegionSize=1M -XX:+AlwaysPreTouch -XX:PreTouchParallelChunkSize=512k -Xlog:gc+ergo+heap=debug,gc+heap=debug,gc=debug gc.g1.TestParallelAlwaysPreTouch
 */

public class TestParallelAlwaysPreTouch {
    public static void main(String[] args) throws Exception {
        final int M = 1024 * 1024; // Something guaranteed to be larger than a region to be counted as humongous.

        for (int i = 0; i < 10; i++) {
            Object[] obj = new Object[M];
            System.out.println(obj);
        }
    }
}

