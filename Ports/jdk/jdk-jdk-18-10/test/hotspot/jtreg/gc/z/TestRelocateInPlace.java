/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.z;

/*
 * @test TestRelocateInPlace
 * @requires vm.gc.Z
 * @summary Test ZGC in-place relocateion
 * @run main/othervm -XX:+UseZGC -Xlog:gc*,gc+stats=off -Xmx256M -XX:+UnlockDiagnosticVMOptions -XX:+ZStressRelocateInPlace gc.z.TestRelocateInPlace
 */

import java.util.ArrayList;

public class TestRelocateInPlace {
    private static final int allocSize = 100 * 1024 * 1024; // 100M
    private static final int smallObjectSize = 4 * 1024; // 4K
    private static final int mediumObjectSize = 2 * 1024 * 1024; // 2M

    private static volatile ArrayList<byte[]> keepAlive;

    private static void allocate(int objectSize) {
        keepAlive = new ArrayList<>();
        for (int i = 0; i < allocSize; i+= objectSize) {
            keepAlive.add(new byte[objectSize]);
        }
    }

    private static void fragment() {
        // Release every other reference to cause lots of fragmentation
        for (int i = 0; i < keepAlive.size(); i += 2) {
            keepAlive.set(i, null);
        }
    }

    private static void test(int objectSize) throws Exception {
        System.out.println("Allocating");
        allocate(objectSize);

        System.out.println("Fragmenting");
        fragment();

        System.out.println("Reclaiming");
        System.gc();
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 10; i++) {
            System.out.println("Iteration " + i);
            test(smallObjectSize);
            test(mediumObjectSize);
        }
    }
}
