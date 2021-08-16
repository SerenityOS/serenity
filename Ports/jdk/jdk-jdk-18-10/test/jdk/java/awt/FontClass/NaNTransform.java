/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6357987 6513889 8023213
 * @summary Font/Text APIs should not crash if transform includes NaN
 * @author prr
 * @run main NaNTransform
 */
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.*;

public class NaNTransform {

    private static void testShape(String msg, Shape s) {
        if (!(new Area(s).isEmpty())) {
            System.out.println(msg+"="+s);
            throw new RuntimeException("Warning: expected this to be empty");
        }
    }

    public static void main(String [] args) {

        float NaN=0f/0f;
        float[] vals = new float[6];
        for (int i=0;i<6;i++) vals[i]=NaN;
        AffineTransform nanTX = new AffineTransform(vals);

        BufferedImage bi = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bi.createGraphics();

        g2d.rotate(NaN);

        Font font = g2d.getFont();
        FontMetrics fm = g2d.getFontMetrics();
        FontRenderContext frc = g2d.getFontRenderContext();

        int adv = fm.stringWidth("ABCDEF");
        if (adv != 0) {
            System.out.println("strWidth="+adv);
            throw new RuntimeException("Warning: expected this to be zero");
        }
        testShape("strBounds", font.getStringBounds("12345", frc));

        TextLayout tl = new TextLayout("Some text", font, frc);
        testShape("tl PixelBounds 1", tl.getPixelBounds(frc, 20, 10));
        testShape("tl PixelBounds 2", tl.getPixelBounds(frc, NaN, NaN));
        testShape("tl Outline", tl.getOutline(nanTX));

        GlyphVector gv = font.createGlyphVector(frc, "abcdef");
        testShape("gv PixelBounds 1", gv.getPixelBounds(frc, 0, 0));
        testShape("gv PixelBounds 2", gv.getPixelBounds(frc, NaN, NaN));
        testShape("gv Outline", gv.getOutline(NaN, NaN));

        gv.setGlyphTransform(0, nanTX);
        testShape("gv PixelBounds 1A", gv.getPixelBounds(frc, 0, 0));
        testShape("gv PixelBounds 2A", gv.getPixelBounds(frc, NaN, NaN));
        testShape("gv Outline A", gv.getOutline(NaN, NaN));

        g2d.drawString("BOO!", 20, 20);

        Font nanFont;
        for (int i=0; i<5000; i++) {
            nanFont = font.deriveFont(Float.NaN);
            g2d.setFont(nanFont);
            g2d.drawString("abc", 20, 20);
        }
        System.out.println("Test passed (no crash)");
    }
}
