/*
 * Copyright (c) 2015 SAP SE. All rights reserved.
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
 * @bug 8141624
 * @summary Limit calculation of pre loop during super word optimization is wrong
 * @run main/othervm compiler.loopopts.superword.TestBestAlign
 * @author gunter.haug@sap.com
 */

package compiler.loopopts.superword;

public class TestBestAlign {

    static final int initVal = -1;
    static int intArray [];
    static boolean boolArray[];
    static int limit;
    static public void clear() {
        for (int i = 0; i < limit; i++) {
            boolArray[1] = true;
            intArray[i] = initVal;
            boolArray[2] = true;
        }
    }

    public static void main(String argv[]) throws Exception {
        limit = 64;
        boolArray = new boolean[8];
        intArray = new int[limit + 4];
        for (int i = 0; i < 10000000; ++i) {
            if(i % 1000000 == 0)
                System.out.println(i);
            clear();
        }
    }
}
