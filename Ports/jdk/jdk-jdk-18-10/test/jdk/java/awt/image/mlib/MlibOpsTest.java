/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug     6556332 8011992 8012112
 * @summary Test verifies that on-demnad loading of medialib library does
 *          not break imageing ops based on this library.
 * @modules java.desktop/sun.awt.image
 * @run     main MlibOpsTest
 * @run     main/othervm/policy=mlib.security.policy MlibOpsTest
 */


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ByteLookupTable;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.LookupOp;
import java.util.Arrays;

import sun.awt.image.ImagingLib;

public class MlibOpsTest {

    public static void main(String[] args) {
        System.out.println("AffineTransformOp:");
        BufferedImageOp op = getAffineTransformOp();
        doTest(op);

        System.out.println("ConvolveOp:");
        op = getConvolveOp();
        doTest(op);

        System.out.println("LookupOp:");
        op = getLookupOp();
        doTest(op);
    }

    public static void doTest(BufferedImageOp op) {
        BufferedImage src = createSrcImage();
        BufferedImage dst = createImage();
        BufferedImage ret = null;
        try {
            ret = ImagingLib.filter(op, src, dst);
        } catch (Exception e) {
            throw new RuntimeException("Test FAILED.", e);
        }
        if (ret == null) {
            throw new RuntimeException("Test FAILED: null output");
        }

        System.out.println("ret: " + ret);
        System.out.println("Test PASSED for " + op.getClass().getName());
    }

    private static BufferedImageOp getAffineTransformOp() {
        AffineTransform at = new AffineTransform();
       return new AffineTransformOp(at,
                                    AffineTransformOp.TYPE_BICUBIC);
    }

    private static BufferedImageOp getConvolveOp() {
        int kw = 3;
        int kh = 3;
        int size = kw * kh;
        float[] kdata = new float[size];
        Arrays.fill(kdata, 1.0f / size);

        Kernel k  = new Kernel(kw, kh, kdata);
        return new ConvolveOp(k);
    }

    private static BufferedImageOp getLookupOp() {
        byte[] inv = new byte[256];
        for (int i = 0; i < 256; i++) {
            inv[i] = (byte)(255 - i);
        }
        ByteLookupTable table = new ByteLookupTable(0, inv);
        return new LookupOp(table, null);
    }


    private static int w = 100;
    private static int h = 100;

    private static BufferedImage createImage() {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    }

    private static BufferedImage createSrcImage() {
        BufferedImage img = createImage();

        Graphics2D g = img.createGraphics();
        Color[] colors = { Color.red, Color.green, Color.blue };
        float[] dist = {0.0f, 0.5f, 1.0f };
        Point2D center = new Point2D.Float(0.5f * w, 0.5f * h);

        RadialGradientPaint p =
                new RadialGradientPaint(center, 0.5f * w, dist, colors);
        g.setPaint(p);
        g.fillRect(0, 0, w, h);
        g.dispose();

        return img;
    }
}
