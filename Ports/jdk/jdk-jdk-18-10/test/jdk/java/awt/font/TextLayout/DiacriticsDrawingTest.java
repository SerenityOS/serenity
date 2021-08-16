/*
 * Copyright 2017 JetBrains s.r.o.
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
 * @bug 8170552
 * @summary verify enabling text layout for complex text on macOS
 * @requires os.family == "mac"
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class DiacriticsDrawingTest {
    private static final Font FONT = new Font("Menlo", Font.PLAIN, 12);
    private static final int IMAGE_WIDTH = 20;
    private static final int IMAGE_HEIGHT = 20;
    private static final int TEXT_X = 5;
    private static final int TEXT_Y = 15;

    public static void main(String[] args) {
        BufferedImage composed = drawString("\u00e1"); // latin small letter a with acute
        BufferedImage decomposed = drawString("a\u0301"); // same letter in decomposed form

        if (!imagesAreEqual(composed, decomposed)) {
            throw new RuntimeException("Text rendering is supposed to be the same");
        }
    }

    private static BufferedImage drawString(String text) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        g.setColor(Color.black);
        g.setFont(FONT);
        g.drawString(text, TEXT_X, TEXT_Y);
        g.dispose();
        return image;
    }

    private static boolean imagesAreEqual(BufferedImage i1, BufferedImage i2) {
        if (i1.getWidth() != i2.getWidth() || i1.getHeight() != i2.getHeight()) return false;
        for (int i = 0; i < i1.getWidth(); i++) {
            for (int j = 0; j < i1.getHeight(); j++) {
                if (i1.getRGB(i, j) != i2.getRGB(i, j)) {
                    return false;
                }
            }
        }
        return true;
    }
}
