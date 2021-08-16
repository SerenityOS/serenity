/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6359661 6366196
 * @summary Unit test for corner cases of position encoding
 * @author  Wei Tao
 * @modules jdk.compiler/com.sun.tools.javac.util
 * @run main T6366196
 */

import com.sun.tools.javac.util.*;

public class T6366196 {
    public static final int MIDLINE = Position.MAXPOS>>>Position.LINESHIFT; // 0x00200000

    public static void main(String[] args) {
        positiveTest(10, Position.MAXCOLUMN);
        negativeTest(20, Position.MAXCOLUMN + 1);
        positiveTest(MIDLINE, Position.MAXCOLUMN);
        positiveTest(Position.MAXLINE, 40);
        negativeTest(Position.MAXLINE, Position.MAXCOLUMN);
        negativeTest(Position.MAXLINE + 1, 1);
    }

    public static void positiveTest(int line, int col) {
        if (Position.encodePosition(line, col) == Position.NOPOS) {
            throw new Error("test failed at line = " + line + ", column = " + col);
        } else {
            System.out.println("test passed at line = " + line + ", column = " + col);
        }
    }

    public static void negativeTest(int line, int col) {
        if (Position.encodePosition(line, col) != Position.NOPOS) {
            throw new Error("test failed at line = " + line + ", column = " + col);
        } else {
            System.out.println("test passed at line = " + line + ", column = " + col);
        }
    }
}
