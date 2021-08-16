/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7043054
 * @summary Verifies that Paint objects receive the appropriate user space
 *          bounds in their createContext() method
 * @run main PgramUserBoundsTest
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

public class PgramUserBoundsTest {
    static final int MinX = 10;
    static final int MinY = 20;
    static final int MaxX = 30;
    static final int MaxY = 50;
    static AffineTransform identity = new AffineTransform();

    public static void main(String argv[]) {
        BufferedImage bimg =
            new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bimg.createGraphics();
        g2d.setPaint(new BoundsCheckerPaint(MinX, MinY, MaxX, MaxY));
        testAll(g2d);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
        testAll(g2d);
    }

    static void testAll(Graphics2D g2d) {
        g2d.setTransform(identity);
        g2d.translate(100, 100);
        testPrimitives(g2d);

        g2d.setTransform(identity);
        g2d.scale(10, 10);
        testPrimitives(g2d);

        g2d.setTransform(identity);
        g2d.rotate(Math.PI/6);
        testPrimitives(g2d);
    }

    static void testPrimitives(Graphics2D g2d) {
        testLine(g2d);
        testRect(g2d);
    }

    static void testLine(Graphics2D g2d) {
        testLine(g2d, MinX, MinY, MaxX, MaxY);
        testLine(g2d, MaxX, MinY, MinX, MaxY);
        testLine(g2d, MinX, MaxY, MaxX, MinY);
        testLine(g2d, MaxX, MaxY, MinX, MinY);
    }

    static void testRect(Graphics2D g2d) {
        g2d.fillRect(MinX, MinY, MaxX - MinX, MaxY - MinY);
        g2d.fill(new Rectangle(MinX, MinY, MaxX - MinX, MaxY - MinY));
    }

    static void testLine(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        g2d.drawLine(x1, y1, x2, y2);
        g2d.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    static class BoundsCheckerPaint implements Paint {
        private Color c = Color.WHITE;
        private Rectangle2D expectedBounds;

        public BoundsCheckerPaint(double x1, double y1,
                                  double x2, double y2)
        {
            expectedBounds = new Rectangle2D.Double();
            expectedBounds.setFrameFromDiagonal(x1, y1, x2, y2);
        }

        public int getTransparency() {
            return c.getTransparency();
        }

        public PaintContext createContext(ColorModel cm,
                                          Rectangle deviceBounds,
                                          Rectangle2D userBounds,
                                          AffineTransform xform,
                                          RenderingHints hints)
        {
            System.out.println("user bounds = "+userBounds);
            if (!userBounds.equals(expectedBounds)) {
                throw new RuntimeException("bounds fail to match");
            }
            return c.createContext(cm, deviceBounds, userBounds, xform, hints);
        }
    }
}
