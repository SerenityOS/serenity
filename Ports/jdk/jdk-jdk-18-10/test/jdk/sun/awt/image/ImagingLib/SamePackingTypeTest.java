/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8019201
 * @summary Test verifies that medialib glue code does not throw
 *          an ImagingOpException for certain pairs of source and
 *          destination images.
 *
 * @run main SamePackingTypeTest
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR;
import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR_PRE;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.ImagingOpException;
import java.awt.image.Kernel;
import java.util.Arrays;


public class SamePackingTypeTest {

    public static void main(String[] args) {
        BufferedImageOp op = createTestOp();

        try {
            System.out.print("Integer-based images... ");
            doTest(op, TYPE_INT_ARGB, TYPE_INT_ARGB_PRE);
            System.out.println("done.");

            System.out.print("Byte-based images... ");
            doTest(op, TYPE_4BYTE_ABGR, TYPE_4BYTE_ABGR_PRE);
            System.out.println("done");
        } catch (ImagingOpException e) {
            throw new RuntimeException("Test FAILED", e);
        }
    }

    private static void doTest(BufferedImageOp op, int stype, int dtype) {
        final int size = 100;

        final BufferedImage src = new BufferedImage(size, size, stype);
        Graphics2D g = src.createGraphics();
        g.setColor(Color.red);
        g.fillRect(0, 0, size, size);
        g.dispose();


        final BufferedImage dst = new BufferedImage(size, size, dtype);
        g = dst.createGraphics();
        g.setColor(Color.blue);
        g.fillRect(0, 0, size, size);
        g.dispose();

        op.filter(src, dst);

        final int rgb = dst.getRGB(size - 1, size - 1);
        System.out.printf("dst: 0x%X ", rgb);

        if (rgb != 0xFFFF0000) {
            throw new RuntimeException(String.format("Wrong color in dst: 0x%X", rgb));
        }
    }

    private static BufferedImageOp createTestOp() {
        final int size = 1;
        final float v = 1f / (size * size);
        final float[] k_data = new float[size * size];
        Arrays.fill(k_data, v);

        Kernel k = new Kernel(size, size, k_data);
        return new ConvolveOp(k);
    }
}
