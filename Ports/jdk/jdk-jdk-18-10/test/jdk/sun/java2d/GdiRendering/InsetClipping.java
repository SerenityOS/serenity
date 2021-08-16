/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4873505 6588884
 * @author cheth
 * @summary verifies that drawImage behaves the bounds of a complex
 * clip shape.  This was a problem with our GDI renderer on Windows, where
 * we would ignore the window insets.
 * @run main InsetClipping
 */

/**
 * This test works by setting up a clip area that equals the visible area
 * of the Frame.  When we perform any rendering operation to that window,
 * we should not see the results of the operation because they should be
 * clipped out.  We create an Image with one color (red) and use a
 * different background fill color (blue).  We fill the area with the
 * background color, then set the clip, then draw the image; if we detect
 * the image color at pixel (0, 0) then we did not clip correctly and the
 * test fails.
 */

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;

public class InsetClipping extends Frame {
    BufferedImage image;
    Area area;
    static boolean painted = false;
    static Color imageColor = Color.red;
    static Color fillColor = Color.blue;

    public InsetClipping() {

        image  = new BufferedImage( 300, 300,BufferedImage.TYPE_INT_RGB);
        Graphics g2 = image.createGraphics();
        g2.setColor(imageColor);
        g2.fillRect(0,0, 300,300);
    }

    public void paint(Graphics g) {
        Insets insets = getInsets();
        area = new Area( new Rectangle(0,0, getWidth(), getHeight()));
        area.subtract(new Area(new Rectangle(insets.left, insets.top,
                                      getWidth() - insets.right,
                                      getHeight() - insets.bottom)));
        g.setColor(fillColor);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setClip(area);
        g.drawImage(image, 0, 0, null);
        painted = true;
    }

    public static void main(String args[]) {
        InsetClipping clipTest = new InsetClipping();
        clipTest.setSize(300, 300);
        clipTest.setLocationRelativeTo(null);
        clipTest.setVisible(true);
        while (!painted) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {}
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {}
        try {
            Robot robot = new Robot();
            Point clientLoc = clipTest.getLocationOnScreen();
            Insets insets = clipTest.getInsets();
            clientLoc.x += insets.left;
            clientLoc.y += insets.top;
            BufferedImage clientPixels =
                robot.createScreenCapture(new Rectangle(clientLoc.x,
                                                        clientLoc.y,
                                                        clientLoc.x + 2,
                                                        clientLoc.y + 2));
            try {
                Thread.sleep(2000);
            } catch (Exception e) {}
            int pixelVal = clientPixels.getRGB(2, 2);
            clipTest.dispose();
            if ((new Color(pixelVal)).equals(fillColor)) {
                System.out.println("Passed");
            } else {
                throw new Error("Failed: incorrect color in pixel (2, 2)");
            }
        } catch (Exception e) {
            System.out.println("Problems creating Robot");
        }
    }
}
