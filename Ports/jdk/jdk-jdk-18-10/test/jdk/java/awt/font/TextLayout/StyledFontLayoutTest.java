/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8139176
 * @summary Test layout uses correct styled font.
 * @run main StyledFontLayoutTest
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class StyledFontLayoutTest extends JPanel {

    static final int W=600, H=400;
    static boolean interactive;
    static BufferedImage im;
    public static void main(String[] args) {

        interactive = args.length > 0;

        runTest();

        if (!interactive) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Styled Font Layout Test");
            frame.add(new StyledFontLayoutTest());
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setSize(W, H);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(im, 0, 0, null);
    }

    private static void runTest() {
        im = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = im.createGraphics();
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, W, H);
        g2d.setColor(Color.black);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        char[] chs = "Sample Text.".toCharArray();
        int len = chs.length;

        int x = 50, y = 100;

        FontRenderContext frc = g2d.getFontRenderContext();
        Font plain = new Font("Serif", Font.PLAIN, 48);
        GlyphVector pgv = plain.layoutGlyphVector(frc, chs, 0, len, 0);
        g2d.setFont(plain);
        g2d.drawChars(chs, 0, len, x, y); y +=50;

        g2d.drawGlyphVector(pgv, x, y); y += 50;
        Rectangle2D plainStrBounds = plain.getStringBounds(chs, 0, len, frc);
        Rectangle2D plainGVBounds = pgv.getLogicalBounds();
        Font bold = new Font("Serif", Font.BOLD, 48);
        GlyphVector bgv = bold.layoutGlyphVector(frc, chs, 0, len, 0);
        Rectangle2D boldStrBounds = bold.getStringBounds(chs, 0, len, frc);
        Rectangle2D boldGVBounds = bgv.getLogicalBounds();
        g2d.setFont(bold);
        g2d.drawChars(chs, 0, len, x, y); y +=50;
        g2d.drawGlyphVector(bgv, x, y);
        System.out.println("Plain String Bounds = " + plainStrBounds);
        System.out.println("Bold String Bounds = " + boldStrBounds);
        System.out.println("Plain GlyphVector Bounds = " + plainGVBounds);
        System.out.println("Bold GlyphVector Bounds = " + boldGVBounds);
        if (!plainStrBounds.equals(boldStrBounds) &&
             plainGVBounds.equals(boldGVBounds))
        {
            System.out.println("Test failed: Plain GV bounds same as Bold");
            if (!interactive) {
                throw new RuntimeException("Plain GV bounds same as Bold");
            }
        }

    };
}
