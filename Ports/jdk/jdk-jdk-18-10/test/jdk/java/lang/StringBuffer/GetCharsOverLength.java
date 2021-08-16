/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4230290
 * @summary Test GetChars method parameter checking
 */

public class GetCharsOverLength {

   public static void main (String argv[]) {

    StringBuffer sb = new StringBuffer("sample string buffer");
    char dst[] = new char[30];
    boolean failed = false;

    int a[][] = {
                  {0, 0, dst.length + 1},
                  {0, 0, dst.length + 2},
                  {0, 0, dst.length + 20},
                  {5, 5, dst.length + 1},
                  {5, 5, dst.length + 2},
                  {5, 5, dst.length + 20}
    };

    for (int i = 0; i < a.length; i++) {
        try {
            sb.getChars(a[i][0], a[i][1], dst, a[i][2]);
            throw new RuntimeException("Bounds test failed");
        } catch (IndexOutOfBoundsException iobe) {
            // Test passed
        }
    }
  }
}
