/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6232267
 * @requires (os.family == "mac")
 * @summary Test reading a font with only Apple names.
 * @run main AppleFontNameTest
 */

import java.awt.Font;
import java.io.File;

/*
 * This test picks on a font that is known to have only Apple names.
 * So it runs only on MacOS and works only if the font is where it
 * has historically been. Anything else is a silent pass.`
 */

public class AppleFontNameTest {

    static String file = "/System/Library/Fonts/Menlo.ttc";

    public static void main(String[] args) throws Exception {
        String os = System.getProperty("os.name");
        if (!(os.startsWith("Mac"))) {
            return;
        }
        File fontFile = new File(file);
        if (!fontFile.exists()) {
            return;
        }
        Font[] fonts = Font.createFonts(new File(file));
        System.out.println("createFont from file returned " + fonts);

        if (fonts == null || fonts.length == 0) {
            throw new RuntimeException("No fonts");
        }
        for (Font f : fonts) {
            System.out.println(f);
            if (!f.getFamily().equals("Menlo"))
               throw new RuntimeException("Expected Menlo, got " + f.getFamily());
        }
    }
}
