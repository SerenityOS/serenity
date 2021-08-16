/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7002398
 * @summary Verify that Corrigendum #8 for Unicode 6.0.0 has been applied.
 */
import java.text.*;

public class Bug7002398 {

    private static final int[] directions = {
        Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT,
        Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT,
        Bidi.DIRECTION_LEFT_TO_RIGHT,
        Bidi.DIRECTION_RIGHT_TO_LEFT
    };

    /*
     * Old Bidi class: AL AN AL AN AL
     * New Bidi class:          AL
     */
    private static final String str = "\u0627\u0660\u0710\u070F\u070D";
    private static final int[] expectedLevels = {1, 2, 1, 1, 1};

    public static void main(String[] args) {
        boolean err = false;

        for (int dir = 0; dir < directions.length; dir ++) {
            Bidi bidi = new Bidi(str, directions[dir]);
            for (int index = 0; index < str.length(); index ++) {
                int gotLevel = bidi.getLevelAt(index);
                if (gotLevel != expectedLevels[index]) {
                    err = true;
                    System.err.println("Unexpected level for the character 0x" +
                        Integer.toHexString(str.charAt(index)).toUpperCase() +
                        ": Expected level = " + expectedLevels[index] +
                        ", actual level = " + bidi.getLevelAt(index) +
                        " in direction = " + directions[dir] + ".");
                }
            }
        }

        if (err) {
            throw new RuntimeException("Failed.");
        }
    }

}
