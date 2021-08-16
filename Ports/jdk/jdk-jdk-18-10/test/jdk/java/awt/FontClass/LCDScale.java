/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @bug 6925760
 * @summary Scaled graphics causes overlapped LCD glyphs on Windows
 */

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;

public class LCDScale extends Component {

    public static void main(String args[]) {
        Frame f = new Frame("TL TEST");
        LCDScale td = new LCDScale();
        f.add("Center", td);
        f.pack(); f.setVisible(true);
    }


    public LCDScale() {
        super();
    }

    public Dimension getPreferredSize() {
        return new Dimension(500,500);
    }

    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;
        g2d.setRenderingHint(
                 RenderingHints.KEY_TEXT_ANTIALIASING,
                 RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        Font f = new Font("Dialog", Font.PLAIN, 40);
        g.setFont(f);
        FontRenderContext frc = g2d.getFontRenderContext();
        GlyphVector gv = f.createGlyphVector(frc, "Help");
        g2d.drawGlyphVector(gv, 10f, 50f);
        Rectangle2D bds1 = gv.getLogicalBounds();

        f = new Font("Arial", Font.PLAIN, 25);
        g.setFont(f);
        double s = 2.0;
        AffineTransform tx = AffineTransform.getScaleInstance(s,s);
        g2d.transform(tx);
        frc = g2d.getFontRenderContext();
        gv = f.createGlyphVector(frc, "Help");
        g2d.drawGlyphVector(gv, 10f, 100f);
        Rectangle2D bds2 = gv.getLogicalBounds();

        System.out.println(bds1);
        System.out.println(bds2);
        if (bds2.getWidth()*s < bds1.getWidth()) {
            throw new RuntimeException("Bounds too small");
        }
    }
}
