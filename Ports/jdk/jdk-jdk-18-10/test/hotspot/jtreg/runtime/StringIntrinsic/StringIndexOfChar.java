/*
 * Copyright (c) 2020, Azul Systems, Inc. All rights reserved.
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

/* @test
 * @bug 8239787
 * @summary String.indexOf(char) for empty string must give -1
 * @run main/othervm -XX:-CompactStrings StringIndexOfChar
 */
public class StringIndexOfChar {
    public static void main(String[] args) throws Exception {
        String emptyString = "";
        for (int i = 0; i < 100; i++) {
            for(int c = 0; c < 0xFFFF; c++) {
                int result = emptyString.indexOf((char)c, -1);
                if (result != -1) {
                    throw new Exception("new String(\"\").indexOf(char, -1) must be -1, but got " + result);
                }
            }
        }
    }
}
