/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.awt.image.VolatileImage;

import sun.java2d.SunGraphics2D;

/**
 * @test
 * @key headful
 * @bug 8000629
 * @summary Set/get transform should work on constrained graphics.
 * @author Sergey Bylokhov
 * @modules java.desktop/sun.java2d
 */
public class TransformSetGet {

    public static void main(final String[] args) {
        final GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsConfiguration gc =
                ge.getDefaultScreenDevice().getDefaultConfiguration();
        final VolatileImage vi = gc.createCompatibleVolatileImage(200, 200);
        final SunGraphics2D sg2d = (SunGraphics2D) vi.createGraphics();

        sg2d.constrain(0, 61, 100, 100);
        final AffineTransform expected = sg2d.cloneTransform();
        sg2d.setTransform(sg2d.getTransform());
        final AffineTransform actual = sg2d.cloneTransform();
        sg2d.dispose();
        vi.flush();
        if (!expected.equals(actual)) {
            System.out.println("Expected = " + expected);
            System.out.println("Actual = " + actual);
            throw new RuntimeException("Wrong transform");
        }
    }
}
