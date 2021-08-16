/*
 * Copyright (c) 2014 Google Inc. All rights reserved.
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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.AttributedString;

import javax.imageio.ImageIO;

/**
 * Manual test for:
 * JDK-8057986: freetype code to get glyph outline does not handle initial control point properly
 *
 * Manual repro recipe:
 * (cd test/java/awt/font/GlyphVector/ && javac GlyphVectorOutline.java && wget -q -O/tmp/msgothic.ttc https://browserlinux-jp.googlecode.com/files/msgothic.ttc && java GlyphVectorOutline /tmp/msgothic.ttc /tmp/katakana.png)
 *
 * Then examine the two rendered Japanese characters in the png file.
 *
 * Renders text to a PNG by
 * 1. using the native Graphics2D#drawGlyphVector implementation
 * 2. filling in the result of GlyphVector#getOutline
 *
 * Should be the same but is different for some CJK characters
 * (e.g. Katakana character \u30AF).
 *
 * @author ikopylov@google.com (Igor Kopylov)
 */
public class GlyphVectorOutline {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new Error("Usage: java GlyphVectorOutline fontfile outputfile");
        }
        writeImage(new File(args[0]),
                   new File(args[1]),
                   "\u30AF");
    }

    public static void writeImage(File fontFile, File outputFile, String value) throws Exception {
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(Color.BLACK);

        Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
        font = font.deriveFont(Font.PLAIN, 72f);
        FontRenderContext frc = new FontRenderContext(null, false, false);
        GlyphVector gv = font.createGlyphVector(frc, value);
        g.drawGlyphVector(gv, 10, 80);
        g.fill(gv.getOutline(10, 180));
        ImageIO.write(image, "png", outputFile);
    }

    private static void drawString(Graphics2D g, Font font, String value, float x, float y) {
        AttributedString str = new AttributedString(value);
        str.addAttribute(TextAttribute.FOREGROUND, Color.BLACK);
        str.addAttribute(TextAttribute.FONT, font);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        TextLayout layout = new LineBreakMeasurer(str.getIterator(), frc).nextLayout(Integer.MAX_VALUE);
        layout.draw(g, x, y);
    }
}
