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
 * @bug 4157675
 * @summary Solaris JIT generates bad code for logic expression
 * @author Tom Rodriguez
 * @run main compiler.codegen.BadLogicCode
 */

package compiler.codegen;

public class BadLogicCode {
    static int values[] = {Integer.MIN_VALUE, -1, 0, 1, 4, 16, 31,
                           32, 33, Integer.MAX_VALUE};
    static char b[][] = {null, new char[32]};
    static boolean nullPtr = false, indexOutBnd = false;
    static boolean indexOutBnd2 = false;

    public static void main(String args[]) throws Exception{
        int i = 1, j = 4, k = 9;

        nullPtr = (b[i] == null);

        int bufLen = nullPtr ? 0 : b[i].length;
        indexOutBnd = (values[j] < 0)
            || (values[j] > bufLen)
            || (values[k] < 0)
            || ((values[j] + values[k]) > bufLen)
            ||((values[j] + values[k]) < 0);

        indexOutBnd2 = (values[j] < 0);
        indexOutBnd2 = indexOutBnd2 || (values[j] > bufLen);
        indexOutBnd2 = indexOutBnd2 || (values[k] < 0);
        indexOutBnd2 = indexOutBnd2 || ((values[j] + values[k]) > bufLen);
        indexOutBnd2 = indexOutBnd2 ||((values[j] + values[k]) < 0);
        if (indexOutBnd != indexOutBnd2)
            throw new Error("logic expression generate different results");
    }
}
