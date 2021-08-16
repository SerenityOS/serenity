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

/**
 * @test
 * @key headful
 * @bug 6685312
 * @summary Check advance of LCD text on a scaled graphics.
 */

import javax.swing.*;
import java.awt.*;
import static java.awt.RenderingHints.*;

public class ScaledLCDTextMetrics extends Component {

    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add("Center", new ScaledLCDTextMetrics());
        f.pack();
        f.setVisible(true);
    }

    public Dimension getPreferredSize() {
      return new Dimension(200,100);
    }
    public void paint(Graphics g) {
       Graphics2D g2 = (Graphics2D)g;

       Font f = new Font("Tahoma", Font.PLAIN, 11);
       g.setFont(f);
       g.setColor(Color.white);
       g.fillRect(0,0,400,300);
       g.setColor(Color.black);
       g2.setRenderingHint(KEY_TEXT_ANTIALIASING,VALUE_TEXT_ANTIALIAS_LCD_HRGB);
       String text = "ABCDEFGHIJKLI";

       FontMetrics fm1 = g2.getFontMetrics();
       int adv1 = fm1.stringWidth(text);
       g.drawString(text, 5, 20);

       g2.scale(2,2);

       FontMetrics fm2 = g2.getFontMetrics();
       int adv2 = fm2.stringWidth(text);
       g.drawString(text, 5, 40);

       double frac = Math.abs(adv1/(double)adv2);

       System.out.println("scalex1: " + adv1);
       System.out.println("scalex2: " + adv2);
       System.out.println("Fraction : "+ frac);

       // adv1 will not be exactly the same as adv2, but should differ
       // only by a fraction.

       if (frac < 0.8 || frac > 1.2) {
           throw new RuntimeException("Metrics differ " +
           "Adv1="+adv1+" Adv2="+adv2+" Fraction="+frac);
       }
    }
}
