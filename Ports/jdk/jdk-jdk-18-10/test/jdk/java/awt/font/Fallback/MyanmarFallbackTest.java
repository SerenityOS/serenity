/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8223558
 * @summary Verifies Dialog logical font falls back to "Myanmar Text"
 * @requires (os.family == "windows")
 */

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;

public class MyanmarFallbackTest {

    public static void main(String[] args) {
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            System.err.println("This test is for Windows only");
            return;
        }

        String[] fontFamilyNames = GraphicsEnvironment
                                   .getLocalGraphicsEnvironment()
                                   .getAvailableFontFamilyNames();
        if (Arrays.stream(fontFamilyNames)
                  .noneMatch("Myanmar Text"::equals)) {
            System.err.println("Myanmar Text font is not installed");
            return;
        }

        Font dialog = new Font(Font.DIALOG, Font.PLAIN, 12);
        if (-1 != dialog.canDisplayUpTo("\u1000\u103C")) {
            throw new RuntimeException("Cannot display Myanmar characters");
        }
    }
}
