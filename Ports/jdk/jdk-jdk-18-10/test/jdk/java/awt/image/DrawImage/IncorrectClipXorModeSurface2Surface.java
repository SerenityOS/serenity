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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import static java.awt.geom.Rectangle2D.Double;

/**
 * @test
 * @key headful
 * @bug 8061831 8130400
 * @summary Tests drawing volatile image to volatile image using different
 *          clips + xor mode. Results of the blit compatibleImage to
 *          compatibleImage is used for comparison.
 * @run main/othervm -Dsun.java2d.uiScale=1 IncorrectClipXorModeSurface2Surface
 * @run main/othervm -Dsun.java2d.uiScale=2 IncorrectClipXorModeSurface2Surface
 * @run main/othervm -Dsun.java2d.uiScale=4 IncorrectClipXorModeSurface2Surface
 */
public final class IncorrectClipXorModeSurface2Surface {

    private static int[] SIZES = {2, 10, 100};
    private static final Shape[] SHAPES = {
            new Rectangle(0, 0, 0, 0),
            new Rectangle(0, 0, 1, 1),
            new Rectangle(0, 1, 1, 1),
            new Rectangle(1, 0, 1, 1),
            new Rectangle(1, 1, 1, 1),

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
            new Double(.10, .75, .20, .20),
    };

    public static void main(final String[] args) throws IOException {
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        GraphicsConfiguration gc = ge.getDefaultScreenDevice()
                .getDefaultConfiguration();
        AffineTransform at;
        for (int size : SIZES) {
            at = AffineTransform.getScaleInstance(size, size);
            for (Shape clip : SHAPES) {
                clip = at.createTransformedShape(clip);
                for (Shape to : SHAPES) {
                    to = at.createTransformedShape(to);
                    // Prepare test images
                    BufferedImage snapshot;
                    VolatileImage source = getVolatileImage(gc, size);
                    VolatileImage target = getVolatileImage(gc, size);
                    int attempt = 0;
                    while (true) {
                        if (++attempt > 10) {
                            throw new RuntimeException("Too many attempts: " + attempt);
                        }
                        // Prepare source images
                        source.validate(gc);
                        Graphics2D g2d = source.createGraphics();
                        g2d.setColor(Color.RED);
                        g2d.fillRect(0, 0, size, size);
                        g2d.dispose();
                        if (source.validate(gc) != VolatileImage.IMAGE_OK) {
                            continue;
                        }
                        // Prepare target images
                        target.validate(gc);
                        g2d = target.createGraphics();
                        g2d.setColor(Color.GREEN);
                        g2d.fillRect(0, 0, size, size);
                        g2d.dispose();
                        if (target.validate(gc) != VolatileImage.IMAGE_OK) {
                            continue;
                        }

                        draw(clip, to, source, target);
                        snapshot = target.getSnapshot();
                        if (source.contentsLost() || target.contentsLost()) {
                            continue;
                        }
                        break;
                    }
                    // Prepare gold images
                    BufferedImage goldS = getSourceGold(gc, size);
                    BufferedImage goldT = getTargetGold(gc, size);
                    draw(clip, to, goldS, goldT);
                    validate(snapshot, goldT);
                    source.flush();
                    target.flush();
                }
            }
        }
    }

    private static void draw(Shape clip, Shape shape, Image from, Image to) {
        Graphics2D g2d = (Graphics2D) to.getGraphics();
        g2d.setXORMode(Color.BLACK);
        g2d.setClip(clip);
        Rectangle toBounds = shape.getBounds();
        g2d.drawImage(from, toBounds.x, toBounds.y, toBounds.width,
                      toBounds.height, null);
        g2d.dispose();
    }

    private static BufferedImage getSourceGold(GraphicsConfiguration gc,
                                               int size) {
        final BufferedImage bi = gc.createCompatibleImage(size, size);
        Graphics2D g2d = bi.createGraphics();
        g2d.setColor(Color.RED);
        g2d.fillRect(0, 0, size, size);
        g2d.dispose();
        return bi;
    }

    private static BufferedImage getTargetGold(GraphicsConfiguration gc,
                                               int size) {
        BufferedImage image = gc.createCompatibleImage(size, size);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.GREEN);
        g2d.fillRect(0, 0, size, size);
        g2d.dispose();
        return image;
    }

    private static VolatileImage getVolatileImage(GraphicsConfiguration gc,
                                                  int size) {
        return gc.createCompatibleVolatileImage(size, size);
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
