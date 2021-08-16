/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @summary verify that first element is a dash
 * @bug 6793344
 */

import java.awt.*;
import java.awt.image.*;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class DashStrokeTest extends Component {

    static BufferedImage bi;
    static boolean printed = false;

    public Dimension getPreferredSize() {
      return new Dimension(200,200);
    }

    public static void drawGui() {
        bi = new BufferedImage(200, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bi.createGraphics();
        BasicStroke dashStroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 1.0f, new float[] { 0.0f, 200 },
                1.0f);

        g2d.setStroke(dashStroke);
        g2d.setColor(Color.RED);
        g2d.drawLine(5,10, 100,10);
        printed =true;
    }

    public static void main(String[] args) {
            try {
            SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                drawGui();
            }

            });
            } catch (Exception e) {
            }

            if (printed) {
                checkBI(bi, Color.RED);
            }
    }

    static void checkBI(BufferedImage bi, Color badColor) {
      int badrgb = badColor.getRGB();

      int col = bi.getRGB(6, 9);
      if (col == badrgb) {
          throw new RuntimeException("A pixel was turned on. ");
      }
   }
}

