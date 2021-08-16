/*
 * Copyright (c) 2015 SAP SE. All rights reserved.
 * Copyright (c) 2016, Red Hat, Inc. All rights reserved.
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
 * @bug 8080190
 * @bug 8154537
 * @summary Test that the rotate distance used in the rotate instruction is properly masked with 0x1f
 *
 * @run main/othervm -Xbatch -XX:-UseOnStackReplacement compiler.codegen.IntRotateWithImmediate
 * @author volker.simonis@gmail.com
 */

package compiler.codegen;

public class IntRotateWithImmediate {

    // This is currently the same as Integer.rotateRight()
    static int rotateRight1(int i, int distance) {
        // On some architectures (i.e. x86_64 and ppc64) the following computation is
        // matched in the .ad file into a single MachNode which emmits a single rotate
        // machine instruction. It is important that the shift amount is masked to match
        // corresponding immediate width in the native instruction. On x86_64 the rotate
        // left instruction ('rol') encodes an 8-bit immediate while the corresponding
        // 'rotlwi' instruction on Power only encodes a 5-bit immediate.
        return ((i >>> distance) | (i << -distance));
    }

    static int rotateRight2(int i, int distance) {
        return ((i >>> distance) | (i << (32 - distance)));
    }

    static int compute1(int x) {
        return rotateRight1(x, 3);
    }

    static int compute2(int x) {
        return rotateRight2(x, 3);
    }

    public static void main(String args[]) {
        int val = 4096;

        int firstResult = compute1(val);

        for (int i = 0; i < 100000; i++) {
            int newResult = compute1(val);
            if (firstResult != newResult) {
                throw new InternalError(firstResult + " != " + newResult);
            }
            newResult = compute2(val);
            if (firstResult != newResult) {
                throw new InternalError(firstResult + " != " + newResult);
            }
        }
        System.out.println("OK");
    }

}
