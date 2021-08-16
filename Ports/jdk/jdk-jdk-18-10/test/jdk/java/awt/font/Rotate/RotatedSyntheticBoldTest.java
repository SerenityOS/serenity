/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test    RotatedSyntheticBoldTest
 * @bug     8233006 8244113
 * @summary This test verifies that rotated synthetically bolded fonts
 *          do not have a wandering baseline
 * @run     main RotatedSyntheticBoldTest
 */

/*
 * Note this is designed to be run headless. The creation of the UI
 * is meant to be run outside the harness as an visualisaton aid to
 * debugging any failure.
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class RotatedSyntheticBoldTest extends JPanel {

    static String TEXT = "abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLM";
    static int SZ = 1000;
    static Font font;

    public static void main(String[] args) throws Exception {

        if (args.length > 1) {
            if (args[0].equals("-family")) {
                font = new Font(args[1], Font.BOLD, 20);
             } else if (args[0].equals("-file")) {
                font = Font.createFont(Font.TRUETYPE_FONT, new File(args[1]));
             } else {
                font = new Font(Font.DIALOG, Font.BOLD, 20);
             }
             System.out.println("Using " + font);
             createUI();
        } else {
            doTest();
        }
    }

    static void createUI() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame("Synthetic Text Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new RotatedSyntheticBoldTest());
            frame.setSize(SZ, SZ);
            frame.setVisible(true);
       });
    }

    public void paint(Graphics g) {

        Graphics2D g2d = (Graphics2D)g;

        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
              RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

        AffineTransform tx = g2d.getTransform();
        int x = 40, y = 0;
        for (int i=0; i<360; i+=10) {

             g2d.translate(SZ/2, SZ/2);
             g2d.rotate((double)i/360.0 * Math.PI*2.0);
             g2d.setFont(font);

             g2d.setColor(Color.BLUE);
             g2d.drawString(TEXT, x, y);

             FontRenderContext frc = g2d.getFontRenderContext();
             GlyphVector gv = font.createGlyphVector(frc, TEXT);
             Rectangle2D r = gv.getVisualBounds();

             FontMetrics fm = g2d.getFontMetrics();
             if (r.getHeight() > 1.1 * fm.getHeight()) {
                System.out.println("FAIL : r= " + r + " hgt=" + fm.getHeight());
             }
             g2d.setColor(Color.RED);
             g2d.translate(x, y);
             g2d.draw(r);

             g2d.setTransform(tx);
         }
    }

    static void test(Graphics2D g2d, Font font) {
        int x = 40, y = 0;
        g2d.setFont(font);

        g2d.setColor(Color.BLUE);
        g2d.drawString(TEXT, x, y);

        FontRenderContext frc = g2d.getFontRenderContext();
        GlyphVector gv = font.createGlyphVector(frc, TEXT);
        Rectangle2D r = gv.getVisualBounds();

        FontMetrics fm = g2d.getFontMetrics();
        if (r.getHeight() > 1.2 * fm.getHeight()) {
           System.out.println("FAIL : " + r);
        }
        g2d.setColor(Color.RED);
        g2d.translate(x, y);
        g2d.draw(r);
    }

    static void doTest() {
        GraphicsEnvironment ge =
             GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] families = ge.getAvailableFontFamilyNames();
        BufferedImage bi =
             new BufferedImage(SZ, SZ, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bi.createGraphics();
        g2d.rotate( Math.PI/4.0);
        FontRenderContext frc = g2d.getFontRenderContext();

        boolean failed = false;
        for (String s : families) {
            Font font = new Font(s, Font.BOLD, 20);
            if (font.canDisplayUpTo(TEXT) != -1) {
                continue;
            }
            g2d.setFont(font);
            GlyphVector gv = font.createGlyphVector(frc, TEXT);
            Rectangle2D r = gv.getVisualBounds();
            FontMetrics fm = g2d.getFontMetrics();
            if (r.getHeight() > 1.2 * fm.getHeight()) {
                failed = true;
                System.out.println("FAIL : r= " + r + " hgt=" + fm.getHeight() +
                                   " font=" + font);
            }
        }
        if (failed) {
             throw new RuntimeException("test failed");
        }
    }
}
