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

/**
 * @test
 * @bug     6887494
 *
 * @summary Verifies that no NullPointerException is thrown in Pisces Renderer
 *          under certain circumstances.
 *
 * @run     main TestNPE
 */

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

public class TestNPE {

    private static void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setClip(0, 0, 0, 0);
        g2d.setTransform(
               new AffineTransform(4.0f, 0.0f, 0.0f, 4.0f, -1248.0f, -744.0f));
        g2d.draw(new Line2D.Float(131.21428571428572f, 33.0f,
                                  131.21428571428572f, 201.0f));
    }

    public static void main(String[] args) {
        BufferedImage im = new BufferedImage(100, 100,
                                             BufferedImage.TYPE_INT_ARGB);

        // Trigger exception in main thread.
        Graphics g = im.getGraphics();
        paint(g);
    }
}
