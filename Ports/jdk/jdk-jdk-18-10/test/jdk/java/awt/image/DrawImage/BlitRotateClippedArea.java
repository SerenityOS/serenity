/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import static java.awt.Transparency.TRANSLUCENT;

/**
 * @test
 * @bug 8255722
 * @key headful
 */
public class BlitRotateClippedArea {

    /**
     * The test use case:
     * 1. The destination image is created of size 1000x1000
     * 2. The source image is created of size 2000x2000
     * 3. The source image is painted by the pattern outsize of 1000x1000
     * 4. If the source image is painted as-is to the destination then the
     *    pattern in the source will be ignored, but the test sets some
     *    specific rotation that the pattern will hit the source.
     * Note that rotation is used not a scale/translate.
     */
    public static void main(String[] args) throws Exception {
        // the test check the exact pixels location
        System.setProperty("sun.java2d.uiScale", "1");
        var ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        var gc = ge.getDefaultScreenDevice().getDefaultConfiguration();

        var gold = gc.createCompatibleImage(1000, 1000, TRANSLUCENT);
        var dstVI2BI = gc.createCompatibleImage(1000, 1000, TRANSLUCENT);
        var dstVI2VI = gc.createCompatibleVolatileImage(1000, 1000, TRANSLUCENT);
        var dstBI2VI = gc.createCompatibleVolatileImage(1000, 1000, TRANSLUCENT);

        var srcBI = gc.createCompatibleImage(2000, 2000, TRANSLUCENT);
        var srcVI = gc.createCompatibleVolatileImage(2000, 2000, TRANSLUCENT);

        int attempt = 0;
        BufferedImage snapshotVI2VI;
        BufferedImage snapshotBI2VI;
        do {
            if (++attempt > 10) {
                throw new RuntimeException("Too many attempts: " + attempt);
            }
            dstVI2VI.validate(gc);
            dstBI2VI.validate(gc);
            srcVI.validate(gc);

            fill(srcBI);
            fill(srcVI);

            init(gold);
            init(dstVI2BI);
            init(dstVI2VI);
            init(dstBI2VI);

            draw(gold, srcBI);
            draw(dstVI2BI, srcVI);
            draw(dstVI2VI, srcVI);
            draw(dstBI2VI, srcBI);

            snapshotVI2VI = dstVI2VI.getSnapshot();
            snapshotBI2VI = dstBI2VI.getSnapshot();
        } while (dstVI2VI.contentsLost() || dstBI2VI.contentsLost()
                || srcVI.contentsLost());

        validate(gold, snapshotVI2VI);
        validate(gold, snapshotBI2VI);
        validate(gold, dstVI2BI);
    }

    private static void validate(BufferedImage gold, BufferedImage img)
            throws IOException {
        for (int x = 0; x < gold.getWidth(); ++x) {
            for (int y = 0; y < gold.getHeight(); ++y) {
                if (gold.getRGB(x, y) != img.getRGB(x, y)) {
                    ImageIO.write(gold, "png", new File("gold.png"));
                    ImageIO.write(img, "png", new File("snapshot.png"));
                    throw new RuntimeException("Test failed.");
                }
            }
        }
    }

    private static void draw(Image dstBI, Image src) {
        Graphics2D g = (Graphics2D) dstBI.getGraphics();
        g.rotate(Math.toRadians(180), 1250, 1150);
        g.drawImage(src, 0, 0, null);
        g.dispose();
    }

    private static void init(Image image) {
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.setColor(Color.YELLOW);
        graphics.fillRect(0, 0, image.getWidth(null), image.getHeight(null));
        graphics.dispose();
    }

    private static void fill(Image image) {
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setComposite(AlphaComposite.Src);
        for (int x = 1000; x < image.getWidth(null); ++x) {
            for (int y = 1000; y < image.getHeight(null); ++y) {
                graphics.setColor(new Color(x % 256, 0, y % 256, 125));
                graphics.fillRect(x, y, 1, 1);
            }
        }
        graphics.dispose();
    }
}

