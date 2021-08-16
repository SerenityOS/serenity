/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 6692979
 * @summary Verify no crashes with extreme shears.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
public class Shear extends Component {

    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane().add("Center", new Shear());
        f.pack();
        f.setVisible(true);
    }

    public Dimension getPreferredSize() {
      return new Dimension(400,300);
    }

    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;

        g.setColor(Color.white);
        g.fillRect(0,0,400,300);
        g.setColor(Color.black);
        Font origFont = new Font(Font.DIALOG, Font.BOLD, 30);
        for (int i=0;i<=360;i++) {
            double sv = i*180.0/Math.PI;
            AffineTransform tx = AffineTransform.getShearInstance(sv, sv);
            Font font = origFont.deriveFont(tx);
            g.setFont(font);
            GlyphVector gv =
                  font.createGlyphVector(g2.getFontRenderContext(), "JavaFX");
            //System.out.println(gv.getVisualBounds());
            g.drawString("JavaFX", 100, 100);
        }
    }
}
