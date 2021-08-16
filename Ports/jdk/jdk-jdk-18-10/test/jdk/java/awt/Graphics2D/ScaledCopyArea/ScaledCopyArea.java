/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.image.BufferedImage;

/**
 * @test
 * @bug 8029455
 * @summary Tests that copyarea on offscreen images works as expected when
 *          scaled transform is set
 * @run main ScaledCopyArea
 */
public final class ScaledCopyArea {

    public static void main(final String[] args) {
        final BufferedImage bi = new BufferedImage(100, 300,
                                                   BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = bi.createGraphics();
        g.scale(2, 2);
        g.setColor(Color.RED);
        g.fillRect(0, 0, 100, 300);
        g.setColor(Color.GREEN);
        g.fillRect(0, 100, 100, 100);
        g.copyArea(0, 100, 100, 100, 0, -100);
        g.dispose();
        for (int x = 0; x < 100; ++x) {
            for (int y = 0; y < 100; ++y) {
                final int actual = bi.getRGB(x, y);
                final int exp = Color.GREEN.getRGB();
                if (actual != exp) {
                    System.err.println("Expected:" + Integer.toHexString(exp));
                    System.err.println("Actual:" + Integer.toHexString(actual));
                    throw new RuntimeException("Test " + "failed");
                }
            }
        }
    }
}
