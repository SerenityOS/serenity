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
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.VolatileImage;

import static java.awt.Transparency.TRANSLUCENT;

/**
 * @test
 * @key headful
 * @bug 8062164
 * @summary We should get correct alpha, when we draw to/from VolatileImage and
 *          bicubic interpolation is enabled
 * @author Sergey Bylokhov
 */
public final class IncorrectAlphaConversionBicubic {

    private static final Color RGB = new Color(200, 255, 7, 123);
    private static final int SIZE = 100;

    public static void main(final String[] args) {
        final GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice gd = ge.getDefaultScreenDevice();
        final GraphicsConfiguration gc = gd.getDefaultConfiguration();
        final VolatileImage vi =
                gc.createCompatibleVolatileImage(SIZE, SIZE, TRANSLUCENT);
        final BufferedImage bi = makeUnmanagedBI(gc, TRANSLUCENT);
        final int expected = bi.getRGB(2, 2);

        int attempt = 0;
        BufferedImage snapshot;
        while (true) {
            if (++attempt > 10) {
                throw new RuntimeException("Too many attempts: " + attempt);
            }
            vi.validate(gc);
            final Graphics2D g2d = vi.createGraphics();
            g2d.setComposite(AlphaComposite.Src);
            g2d.scale(2, 2);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                 RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.drawImage(bi, 0, 0, null);
            g2d.dispose();

            snapshot = vi.getSnapshot();
            if (vi.contentsLost()) {
                continue;
            }
            break;
        }
        final int actual = snapshot.getRGB(2, 2);
        if (actual != expected) {
            System.err.println("Actual: " + Integer.toHexString(actual));
            System.err.println("Expected: " + Integer.toHexString(expected));
            throw new RuntimeException("Test failed");
        }
    }

    private static BufferedImage makeUnmanagedBI(GraphicsConfiguration gc,
                                                 int type) {
        BufferedImage img = gc.createCompatibleImage(SIZE, SIZE, type);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(RGB);
        g2d.fillRect(0, 0, SIZE, SIZE);
        g2d.dispose();
        final DataBuffer db = img.getRaster().getDataBuffer();
        if (db instanceof DataBufferInt) {
            ((DataBufferInt) db).getData();
        } else if (db instanceof DataBufferShort) {
            ((DataBufferShort) db).getData();
        } else if (db instanceof DataBufferByte) {
            ((DataBufferByte) db).getData();
        } else {
            try {
                img.setAccelerationPriority(0.0f);
            } catch (final Throwable ignored) {
            }
        }
        return img;
    }
}
