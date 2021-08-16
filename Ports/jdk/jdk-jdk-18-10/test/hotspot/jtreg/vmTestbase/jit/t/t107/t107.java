/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase jit/t/t107.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t107.t107
 */

package jit.t.t107;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

public class t107 {

        public static final GoldChecker goldChecker = new GoldChecker( "t107" );

        public static void main(String args[]) {
                long l = - 2l;
                int m = 0x1F;
                int s1 = 0;
                int s2 = 0x40;
                int s3 = 63;
                int s4 = 0x7F;
                int s5 = 2;
                int s6 = 0xFFFFFFC2;

                if (l << 0x40 == l << (0 & 0x1F)) {
                        t107.goldChecker.print("l << 0x40 == ");
                        t107.goldChecker.println(l << s2);
                        t107.goldChecker.print("l << (0x40 & 0x1F) == ");
                        t107.goldChecker.println(l << (s2 & m));
                        t107.goldChecker.print('\n');
                }
                if (l >>> 0xFFFFFFC2 == l >>> (0xFFFFFFC2 & 0x1F)) {
                        t107.goldChecker.print("l >>> 0xFFFFFFC2 == ");
                        t107.goldChecker.println(l >>> s6);
                        t107.goldChecker.print("l >>> (0xFFFFFFC2 & 0x1F) == ");
                        t107.goldChecker.println(l >>> (s6 & m));
                }
                t107.goldChecker.check();
        }

}
