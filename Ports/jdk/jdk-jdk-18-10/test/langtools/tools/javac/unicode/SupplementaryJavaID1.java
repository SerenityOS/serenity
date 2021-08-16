/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4914724
 * @summary Check that valid supplementary characters can be used as Java identifiers.
 * @author Naoto Sato
 *
 * @compile SupplementaryJavaID1.java
 * @run main SupplementaryJavaID1
 */

public class SupplementaryJavaID1 {
    public static void main(String[] s) {
        // U+10400 (\ud801\udc00): DESERET CAPITAL LETTER LONG I (can be start or part)
        // U+1D17B (\ud834\udd7b): MUSICAL SYMBOL COMBINING ACCENT (can only be part)
        // U+1D100 (\ud834\udd00): MUSICAL SYMBOL SINGLE BARLINE (can be none of start nor part)
        int \ud801\udc00abc = 1;
        int \ud802\udc00abc = 2;
        int \ud801\udc01abc = 3;
        int def\ud801\udc00 = 4;
        int \ud801\udc00\ud834\udd7b = 5;

        if (\ud801\udc00abc != 1 ||
            \ud802\udc00abc != 2 ||
            \ud801\udc01abc != 3 ||
            def\ud801\udc00 != 4 ||
            \ud801\udc00\ud834\udd7b != 5) {
                throw new RuntimeException("test failed");
        }
    }
}
