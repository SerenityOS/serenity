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

import java.awt.Component;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Window;
import java.awt.image.BufferedImage;

import sun.java2d.SunGraphics2D;

import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;
import static java.awt.Transparency.BITMASK;
import static java.awt.Transparency.OPAQUE;
import static java.awt.Transparency.TRANSLUCENT;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * @test
 * @key headful
 * @bug 8134603
 * @modules java.desktop/sun.java2d
 * @run main/othervm SurfaceDestination
 */
public final class SurfaceDestination {

    public static void main(final String[] args) {
        final GraphicsEnvironment lge = getLocalGraphicsEnvironment();
        final GraphicsDevice dev = lge.getDefaultScreenDevice();
        final GraphicsConfiguration config = dev.getDefaultConfiguration();

        test(config.createCompatibleImage(10, 10).getGraphics());
        test(config.createCompatibleImage(10, 10, OPAQUE).getGraphics());
        test(config.createCompatibleImage(10, 10, BITMASK).getGraphics());
        test(config.createCompatibleImage(10, 10, TRANSLUCENT).getGraphics());

        test(new BufferedImage(10, 10, TYPE_INT_ARGB).getGraphics());

        final Window frame = new Frame();
        frame.pack();
        try {
            test(frame.getGraphics());
            test(frame.createImage(10, 10).getGraphics());
        } finally {
            frame.dispose();
        }
    }

    private static void test(final Graphics graphics) {
        try {
            if (graphics instanceof SunGraphics2D) {
                final Object dst = ((SunGraphics2D) graphics).getDestination();
                if (!(dst instanceof Image) && !(dst instanceof Component)) {
                    throw new RuntimeException("Wrong type:" + dst);
                }
            }
        } finally {
            graphics.dispose();
        }
    }
}
