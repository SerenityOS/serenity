/*
 * Copyright 2016 JetBrains s.r.o.
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
 * @bug 8169202
 * @summary verify font fallback for surrogate pairs on macOS
 * @requires os.family == "mac"
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class SurrogatesFallbackTest {
    private static final int CHARACTER = 0x1d400; // MATHEMATICAL BOLD CAPITAL A
    private static final Font FONT = new Font("Menlo", // expected to fallback to STIXGeneral for the character above
                                              Font.PLAIN,
                                              12);
    private static final int IMAGE_WIDTH = 20;
    private static final int IMAGE_HEIGHT = 20;
    private static final int GLYPH_X = 5;
    private static final int GLYPH_Y = 15;

    public static void main(String[] args) {
        BufferedImage noGlyph = createImage(g -> {});
        BufferedImage missingGlyph = createImage(g -> {
            GlyphVector gv = FONT.createGlyphVector(g.getFontRenderContext(), new int[]{FONT.getMissingGlyphCode()});
            g.drawGlyphVector(gv, GLYPH_X, GLYPH_Y);
        });
        BufferedImage surrogateCharGlyph = createImage(g -> {
            g.setFont(FONT);
            g.drawString(new String(Character.toChars(CHARACTER)), GLYPH_X, GLYPH_Y);
        });

        if (imagesAreEqual(surrogateCharGlyph, noGlyph)) {
            throw new RuntimeException("Character was not rendered");
        }
        if (imagesAreEqual(surrogateCharGlyph, missingGlyph)) {
            throw new RuntimeException("Character is rendered as missing");
        }
    }

    private static BufferedImage createImage(Consumer<Graphics2D> drawing) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        g.setColor(Color.black);
        drawing.accept(g);
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

