/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @requires vm.compiler2.enabled
 * @bug 8240281
 * @summary Test which needs to select the memory state of the first load in a load pack in SuperWord::co_locate_pack.
 *
 * @run main/othervm -Xcomp -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestPickFirstMemoryState::*
 *                   compiler.loopopts.superword.TestPickFirstMemoryState
 * @run main/othervm -XX:CompileCommand=dontinline,compiler.loopopts.superword.TestPickFirstMemoryState::*
 *                   compiler.loopopts.superword.TestPickFirstMemoryState
 */

package compiler.loopopts.superword;

public class TestPickFirstMemoryState {

    static int iArrFld[] = new int[50];

    static int test() {
        int x = 2;
        for (int i = 50; i > 9; i--) {
            // We create an AddI pack and a LoadI pack for the following loop. The StoreI pack gets filtered. When
            // finding the memory state for the load pack, we cannot pick the memory state from the last load as
            // the stores (which are not vectorized) could interfere and be executed before the load pack (already
            // writing 'j' to iArrFld which is afterwards loaded by the load vector operation). This leads to a
            // wrong result for 'x'. We must take the memory state of the first load where we have not yet assigned
            // any new values ('j') to the iArrFld array.
            x = 2;
            for (int j = 10; j < 50; j++) {
                x += iArrFld[j]; // AddI pack + LoadI pack
                iArrFld[j] = j; // StoreI pack that gets removed while filtering packs
            }
            reset();
        }

        return x;
    }

    static int test2() {
        int x = 2;
        int y = 3;
        for (int i = 50; i > 9; i--) {
            x = 2;
            for (int j = 10; j < 50; j++) {
                x += iArrFld[j];
                iArrFld[j] = (y++);
            }
            reset();
        }
        return x;
    }

    static int test3() {
        int x = 2;
        for (int i = 50; i > 9; i--) {
            x = 2;
            int y = i;
            for (int j = 10; j < 50; j++) {
                y++;
                x += iArrFld[j];
                iArrFld[j] = y;
            }
            reset();
        }
        return x;
    }

    static int test4() {
        int x = 2;
        long y = 3L;
        for (int i = 50; i > 9; i--) {
            x = 2;
            for (int j = 10; j < 50; j++) {
                x += iArrFld[j];
                iArrFld[j] = (int)(y++);
            }
            reset();
        }
        return x;
    }

    public static void main(String[] strArr) {
        for (int i = 0; i < 5000; i++) {
            reset();
            int x = test();
            if (x != 202) {
                throw new RuntimeException("test() wrong result: " + x);
            }
            x = test2();
            if (x != 202) {
                throw new RuntimeException("test2() wrong result: " + x);
            }
            x = test3();
            if (x != 202) {
                throw new RuntimeException("test3() wrong result: " + x);
            }
            x = test4();
            if (x != 202) {
                throw new RuntimeException("test4() wrong result: " + x);
            }
        }
    }

    public static void reset() {
        for (int i = 0; i < iArrFld.length; i++) {
            iArrFld[i] = 5;
        }
    }
}

