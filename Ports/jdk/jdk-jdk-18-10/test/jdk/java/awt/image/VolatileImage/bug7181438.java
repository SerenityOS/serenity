/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;

/**
 * @test
 * @key headful
 * @bug 7181438 8198613
 * @summary Verifies that we get correct alpha, when we draw opaque
 * BufferedImage to non opaque VolatileImage via intermediate opaque texture.
 * @author Sergey Bylokhov
 * @run main/othervm -Dsun.java2d.accthreshold=0 bug7181438
 */
public final class bug7181438 {

    private static final int SIZE = 500;

    public static void main(final String[] args) {

        final BufferedImage bi = createBufferedImage();
        final VolatileImage vi = createVolatileImage();
        final Graphics s2dVi = vi.getGraphics();

        //sw->texture->surface blit
        s2dVi.drawImage(bi, 0, 0, null);

        final BufferedImage results = vi.getSnapshot();
        for (int i = 0; i < SIZE; ++i) {
            for (int j = 0; j < SIZE; ++j) {
                //Image should be opaque: (black color and alpha = 255)
                if (results.getRGB(i, j) != 0xFF000000) {
                    throw new RuntimeException("Failed: Wrong alpha");
                }
            }
        }
        System.out.println("Passed");
    }


    private static VolatileImage createVolatileImage() {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
        return gc.createCompatibleVolatileImage(SIZE, SIZE,
                                                Transparency.TRANSLUCENT);
    }

    private static BufferedImage createBufferedImage() {
        final BufferedImage bi = new BufferedImage(SIZE, SIZE,
                                                   BufferedImage.TYPE_INT_RGB);
        final Graphics bg = bi.getGraphics();
        //Black color and alpha = 0
        bg.setColor(new Color(0, 0, 0, 0));
        bg.fillRect(0, 0, SIZE, SIZE);
        bg.dispose();
        return bi;
    }
}
