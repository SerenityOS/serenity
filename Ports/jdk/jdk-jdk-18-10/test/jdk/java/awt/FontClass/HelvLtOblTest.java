/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8064833
 * @summary Test correct font is obtained via famil+style
 * @run main HelvLtOblTest
 */

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;

public class HelvLtOblTest extends JComponent {

    static Font helvFont = null;

    static int[] codes = { 0x23, 0x4a, 0x48, 0x3, 0x4a, 0x55, 0x42, 0x4d,
                    0x4a, 0x44, 0x3,
                    0x53, 0x46, 0x45, 0x3, 0x55, 0x46, 0x59, 0x55, };

    static String str = "Big italic red text";

    public static void main(String[] args) throws Exception {
        String os = System.getProperty("os.name");
        if (!os.startsWith("Mac")) {
             return;
        }
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] fonts = ge.getAllFonts();
        for (int i=0; i<fonts.length; i++) {
            if (fonts[i].getPSName().equals("Helvetica-LightOblique")) {
                 helvFont = fonts[i];
                 break;
            }
        }
        if (helvFont == null) {
            return;
        }
        final HelvLtOblTest test = new HelvLtOblTest();
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.add("Center", test);
            f.pack();
            f.setVisible(true);
        });
        test.compareImages();
    }

    public Dimension getPreferredSize() {
      return new Dimension(400,400);
    }

    public void paintComponent(Graphics g) {
         super.paintComponent(g);
         Graphics2D g2 = (Graphics2D)g;
         FontRenderContext frc = new FontRenderContext(null, true, true);
         Font f = helvFont.deriveFont(Font.PLAIN, 40);
         System.out.println("font = " +f.getFontName());
         GlyphVector gv = f.createGlyphVector(frc, codes);
         g.setFont(f);
         g.setColor(Color.white);
         g.fillRect(0,0,400,400);
         g.setColor(Color.black);
         g2.drawGlyphVector(gv, 5,200);
         g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                             RenderingHints.VALUE_FRACTIONALMETRICS_ON);
         g2.drawString(str, 5, 250);
    }

    void compareImages() {
         BufferedImage bi0 = drawText(false);
         BufferedImage bi1 = drawText(true);
         compare(bi0, bi1);
    }

    BufferedImage drawText(boolean doGV) {
        int w = 400;
        int h = 50;
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0,0,w,h);
        g.setColor(Color.black);
        Font f = helvFont.deriveFont(Font.PLAIN, 40);
        g.setFont(f);
        int x = 5;
        int y = h - 10;
        if (doGV) {
            FontRenderContext frc = new FontRenderContext(null, true, true);
            GlyphVector gv = f.createGlyphVector(frc, codes);
            g.drawGlyphVector(gv, 5, y);
       } else {
           g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                              RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
           g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                              RenderingHints.VALUE_FRACTIONALMETRICS_ON);
           g.drawString(str, x, y);
       }
       return bi;
    }

    // Need to allow for minimal rounding error, so allow each component
    // to differ by 1.
    void compare(BufferedImage bi0, BufferedImage bi1) {
        int wid = bi0.getWidth();
        int hgt = bi0.getHeight();
        for (int x=0; x<wid; x++) {
            for (int y=0; y<hgt; y++) {
                int rgb0 = bi0.getRGB(x, y);
                int rgb1 = bi1.getRGB(x, y);
                if (rgb0 == rgb1) continue;
                int r0 = (rgb0 & 0xff0000) >> 16;
                int r1 = (rgb1 & 0xff0000) >> 16;
                int rdiff = r0-r1; if (rdiff<0) rdiff = -rdiff;
                int g0 = (rgb0 & 0x00ff00) >> 8;
                int g1 = (rgb1 & 0x00ff00) >> 8;
                int gdiff = g0-g1; if (gdiff<0) gdiff = -gdiff;
                int b0 = (rgb0 & 0x0000ff);
                int b1 = (rgb1 & 0x0000ff);
                int bdiff = b0-b1; if (bdiff<0) bdiff = -bdiff;
                if (rdiff > 1 || gdiff > 1 || bdiff > 1) {
                    throw new RuntimeException(
                      "Images differ at x=" + x + " y="+ y + " " +
                      Integer.toHexString(rgb0) + " vs " +
                      Integer.toHexString(rgb1));
                }
            }
        }
    }

}
