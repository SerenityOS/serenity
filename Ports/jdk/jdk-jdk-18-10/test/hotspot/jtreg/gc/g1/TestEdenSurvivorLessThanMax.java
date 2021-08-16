/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestEdenSurvivorLessThanMax
 * @bug 8152724
 * @summary Check that G1 eden plus survivor max capacity after GC does not exceed maximum number of regions.
 * @requires vm.gc.G1
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -Xlog:gc+heap=debug -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:+UseG1GC -Xmx64M -Xms64M -Xmn50M -XX:SurvivorRatio=1 gc.g1.TestEdenSurvivorLessThanMax
 */

import sun.hotspot.WhiteBox;

// The test fills the heap in a way that previous to 8152724 the maximum number of survivor regions
// for that young gc was higher than there was free space left which is impossible.
public class TestEdenSurvivorLessThanMax {
    private static final long BYTES_TO_FILL = 50 * 1024 * 1024;

    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    public static void main(String[] args) throws Exception {
        Object o = new int[100];

        long objSize = WB.getObjectSize(o);

        // Fill eden to approximately ~90%.
        int numObjs = (int)((BYTES_TO_FILL / objSize) * 9 / 10);
        for (int i = 0; i < numObjs; i++) {
          o = new int[100];
        }

        WB.youngGC();

        System.out.println(o.toString());
    }
}

