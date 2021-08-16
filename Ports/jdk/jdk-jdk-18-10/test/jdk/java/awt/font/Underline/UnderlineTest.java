/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6216010
 * @summary check to see that underline thickness scales.
 * @run main UnderlineTest
 */

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

public class UnderlineTest {
    static class FontsPanel extends Container {
        FontsPanel(Font[] fonts) {
            setLayout(new GridLayout(0, 1));
            for (int i = 0; i < fonts.length; ++i) {
              add(new FontPanel(fonts[i]));
            }
        }
    }

    static String fps = "Stellar glyphs";
    static Dimension fpd = new Dimension(600, 120);
    static class FontPanel extends JComponent {
        Font f;
        FontPanel(Font f) {
            this.f = f;
            setPreferredSize(fpd);
            setMinimumSize(fpd);
            setMaximumSize(fpd);
            setSize(fpd);
        }

        public void paintComponent(Graphics g) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, fpd.width, fpd.height);

            g.setColor(Color.RED);
            FontRenderContext frc = ((Graphics2D)g).getFontRenderContext();
            LineMetrics lm = f.getLineMetrics(fps, frc);
            int h = (int)(fpd.height - 20 - lm.getAscent());
            g.drawLine(20, h, fpd.width - 20, h);
            h = fpd.height - 20;
            g.drawLine(20, h, fpd.width - 20, h);
            h = (int)(fpd.height - 20 + lm.getDescent());
            g.drawLine(20, h, fpd.width - 20, h);

            g.setColor(Color.BLACK);
            g.setFont(f);
            g.drawString(fps, 50, fpd.height - 20);
        }
    }

    public static void main(String args[]) {
        String fontName = Font.DIALOG;
        if (args.length > 0) {
            fontName = args[0];
        }
        FontRenderContext frc = new FontRenderContext(null, false, false);
        FontRenderContext frc2 = new FontRenderContext(AffineTransform.getScaleInstance(1.5, 1.5), false, false);

        Font font0 = new Font(fontName, 0, 20);
        HashMap map = new HashMap();
        map.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        map.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        Font font = font0.deriveFont(map);

        System.out.println("Using font: " + font);

        double rot = -Math.PI/4;
        AffineTransform scrtx = AffineTransform.getRotateInstance(rot);
        scrtx.scale(1, 2);

        Font[] fonts = {
            font.deriveFont(1f),
            font.deriveFont(20f),
            font.deriveFont(40f),
            font.deriveFont(80f),
            font.deriveFont(AffineTransform.getRotateInstance(rot)),
            font.deriveFont(AffineTransform.getScaleInstance(1, 2)),
            font.deriveFont(AffineTransform.getScaleInstance(2, 4)),
            font.deriveFont(scrtx),
        };

        LineMetrics[] metrics = new LineMetrics[fonts.length * 2];
        for (int i = 0; i < metrics.length; ++i) {
            Font f = fonts[i % fonts.length];
            FontRenderContext frcx = i < fonts.length ? frc : frc2;
            metrics[i] = f.getLineMetrics("X", frcx);
      //       dumpMetrics("Metrics for " + f.getSize2D() + " pt. font,\n  tx: " +
      //       f.getTransform() + ",\n   frctx: " + frcx.getTransform(), metrics[i]);
        }

        // test for linear scale
        // this seems to work, might need to get fancy to deal with last-significant-bit issues?
        double ds1 = metrics[2].getStrikethroughOffset() - metrics[1].getStrikethroughOffset();
        double du1 = metrics[2].getUnderlineThickness() - metrics[1].getUnderlineThickness();
        double ds2 = metrics[3].getStrikethroughOffset() - metrics[2].getStrikethroughOffset();
        double du2 = metrics[3].getUnderlineThickness() - metrics[2].getUnderlineThickness();
        if (ds2 != ds1 * 2 || du2 != du1 * 2) {
            throw new IllegalStateException("non-linear scale: " + ds1 + " / " + ds2 + ", " +
                                            du1 + " / " + du2);
        }

        JFrame jf = new JFrame("Fonts");
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.add(new JScrollPane(new FontsPanel(fonts)));
        jf.pack();
        jf.setVisible(true);
    }

    static void dumpMetrics(String header, LineMetrics lm) {
        if (header != null) {
            System.out.println(header);
        }
        System.out.println("asc: " + lm.getAscent());
        System.out.println("dsc: " + lm.getDescent());
        System.out.println("ulo: " + lm.getUnderlineOffset());
        System.out.println("ult: " + lm.getUnderlineThickness());
        System.out.println("sto: " + lm.getStrikethroughOffset());
        System.out.println("stt: " + lm.getStrikethroughThickness());
    }
}
