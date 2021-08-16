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
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * @author chrisn@google.com (Chris Nokleberg)
 * @author yamauchi@google.com (Hiroshi Yamauchi)
 */
public class ThinLineTest {
  private static final int PIXEL = 381;

  public static void main(String[] args) throws Exception {
    BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setPaint(Color.WHITE);
    g.fill(new Rectangle(image.getWidth(), image.getHeight()));

    g.scale(0.5 / PIXEL, 0.5 / PIXEL);
    g.setPaint(Color.BLACK);
    g.setStroke(new BasicStroke(PIXEL));
    g.draw(new Ellipse2D.Double(PIXEL * 50, PIXEL * 50, PIXEL * 300, PIXEL * 300));

    // To visually check it
    //ImageIO.write(image, "PNG", new File(args[0]));

    boolean nonWhitePixelFound = false;
    for (int x = 0; x < 200; ++x) {
      if (image.getRGB(x, 100) != Color.WHITE.getRGB()) {
        nonWhitePixelFound = true;
        break;
      }
    }
    if (!nonWhitePixelFound) {
      throw new RuntimeException("The thin line disappeared.");
    }
  }
}
