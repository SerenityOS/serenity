/*
 * Copyright (C) 2019 JetBrains s.r.o.
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
 * @bug 8210058
 * @summary Algorithmic Italic font leans opposite angle in Printing
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

public class RotatedItalicsTest {
    public static void main(String[] args) throws Exception {
        File fontFile = new File(System.getProperty("test.src", "."), "A.ttf");
        Font baseFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
        Font font = baseFont.deriveFont(Font.ITALIC, 120);

        BufferedImage image = new BufferedImage(100, 100,
                                                BufferedImage.TYPE_INT_RGB);

        Graphics2D g = image.createGraphics();
        g.rotate(Math.PI / 2);
        g.setFont(font);
        g.drawString("A", 10, -10);
        g.dispose();

        if (image.getRGB(50, 76) != Color.white.getRGB()) {
            throw new RuntimeException("Wrong glyph rendering");
        }
    }
}
