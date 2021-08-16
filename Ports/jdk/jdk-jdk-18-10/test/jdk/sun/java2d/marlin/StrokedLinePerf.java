/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;

/**
 * @test
 * @bug 7018932
 * @summary fix LoopPipe.getStrokedSpans() performance (clipping enabled by Marlin renderer)
 * @run main/othervm/timeout=10 -Dsun.java2d.renderer=sun.java2d.marlin.DMarlinRenderingEngine StrokedLinePerf
 */
public class StrokedLinePerf {

    public static void main(String[] unused) {
        BufferedImage bi = new BufferedImage(400, 400, TYPE_INT_ARGB_PRE);
        test(bi, true);
        test(bi, false);
    }

    private static void test(BufferedImage bi, boolean useAA) {
        final long start = System.nanoTime();

        final Graphics2D g2d = bi.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                (useAA) ? RenderingHints.VALUE_ANTIALIAS_ON
                        : RenderingHints.VALUE_ANTIALIAS_OFF);

        Stroke stroke = new BasicStroke(2.0f, 1, 0, 1.0f, new float[]{0.0f, 4.0f}, 0.0f);
        g2d.setStroke(stroke);

        //Large values to trigger crash / infinite loop.
        g2d.draw(new Line2D.Double(4.0, 1.794369841E9, 567.0, -2.147483648E9));

        System.out.println("StrokedLinePerf(" + useAA + "): Test duration= " + (1e-6 * (System.nanoTime() - start)) + " ms.");
        g2d.dispose();
    }
}
