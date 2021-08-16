/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import static java.awt.geom.Rectangle2D.Double;

/**
 * @test
 * @key headful
 * @bug 8041644 8044788
 * @summary Tests drawing volatile image to BI using different clip.
 *          Results of the blit compatibleImage to BI used for comparison.
 * @author Sergey Bylokhov
 * @run main/othervm IncorrectClipSurface2SW
 */
public final class IncorrectClipSurface2SW {

    private static int[] SCALES = {1, 2, 4};
    private static int[] SIZES = {127, 3, 2, 1};
    private static final Shape[] SHAPES = {new Rectangle(0, 0, 0, 0),
                                           new Rectangle(0, 0, 1, 1),
                                           new Rectangle(0, 1, 1, 1),
                                           new Rectangle(1, 0, 1, 1),
                                           new Rectangle(1, 1, 1, 1),

                                           new Ellipse2D.Double(0, 0, 1, 1),
                                           new Ellipse2D.Double(0, 1, 1, 1),
                                           new Ellipse2D.Double(1, 0, 1, 1),
                                           new Ellipse2D.Double(1, 1, 1, 1),
                                           new Ellipse2D.Double(.25, .25, .5,
                                                                .5),

                                           new Double(0, 0, 0.5, 0.5),
                                           new Double(0, 0.5, 0.5, 0.5),
                                           new Double(0.5, 0, 0.5, 0.5),
                                           new Double(0.5, 0.5, 0.5, 0.5),
                                           new Double(0.25, 0.25, 0.5, 0.5),
                                           new Double(0, 0.25, 1, 0.5),
                                           new Double(0.25, 0, 0.5, 1),

                                           new Double(.10, .10, .20, .20),
                                           new Double(.75, .75, .20, .20),
                                           new Double(.75, .10, .20, .20),
                                           new Double(.10, .75, .20, .20),};

    public static void main(final String[] args) throws IOException {
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        GraphicsConfiguration gc = ge.getDefaultScreenDevice()
                                     .getDefaultConfiguration();
        AffineTransform at;
        for (final int size : SIZES) {
            for (final int scale : SCALES) {
                final int sw = size * scale;
                at = AffineTransform.getScaleInstance(sw, sw);
                for (Shape clip : SHAPES) {
                    clip = at.createTransformedShape(clip);
                    for (Shape to : SHAPES) {
                        to = at.createTransformedShape(to);
                        // Prepare test images
                        VolatileImage vi = getVolatileImage(gc, size);
                        BufferedImage bi = getBufferedImage(sw);
                        // Prepare gold images
                        BufferedImage goldvi = getCompatibleImage(gc, size);
                        BufferedImage goldbi = getBufferedImage(sw);
                        draw(clip, to, vi, bi, scale);
                        draw(clip, to, goldvi, goldbi, scale);
                        validate(bi, goldbi);
                    }
                }
            }
        }
    }

    private static void draw(Shape clip, Shape to, Image vi, BufferedImage bi,
                             int scale) {
        Graphics2D big = bi.createGraphics();
        big.setComposite(AlphaComposite.Src);
        big.setClip(clip);
        Rectangle toBounds = to.getBounds();
        int x1 = toBounds.x;

        int y1 = toBounds.y;
        int x2 = x1 + toBounds.width;
        int y2 = y1 + toBounds.height;
        big.drawImage(vi, x1, y1, x2, y2, 0, 0, toBounds.width / scale,
                      toBounds.height / scale, null);
        big.dispose();
        vi.flush();
    }

    private static BufferedImage getBufferedImage(int sw) {
        BufferedImage bi = new BufferedImage(sw, sw,
                                             BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bi.createGraphics();
        g2d.setColor(Color.RED);
        g2d.fillRect(0, 0, sw, sw);
        return bi;
    }

    private static VolatileImage getVolatileImage(GraphicsConfiguration gc,
                                                  int size) {
        VolatileImage vi = gc.createCompatibleVolatileImage(size, size);
        Graphics2D g2d = vi.createGraphics();
        g2d.setColor(Color.GREEN);
        g2d.fillRect(0, 0, size, size);
        return vi;
    }

    private static BufferedImage getCompatibleImage(GraphicsConfiguration gc,
                                                    int size) {
        BufferedImage image = gc.createCompatibleImage(size, size);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.GREEN);
        g2d.fillRect(0, 0, size, size);
        return image;
    }

    private static void validate(BufferedImage bi, BufferedImage goldbi)
            throws IOException {
        for (int x = 0; x < bi.getWidth(); ++x) {
            for (int y = 0; y < bi.getHeight(); ++y) {
                if (goldbi.getRGB(x, y) != bi.getRGB(x, y)) {
                    ImageIO.write(bi, "png", new File("actual.png"));
                    ImageIO.write(goldbi, "png", new File("expected.png"));
                    throw new RuntimeException("Test failed.");
                }
            }
        }
    }
}
