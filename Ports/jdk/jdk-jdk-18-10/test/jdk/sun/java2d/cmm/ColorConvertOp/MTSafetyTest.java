/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6476665
 * @summary Verifies MT safety of color conversions
 * @run main MTSafetyTest
 */

import java.util.Vector;
import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;

public class MTSafetyTest {

    static boolean failed = false;

    static int[] colorSpaceType = {
        ColorSpace.CS_CIEXYZ,
        ColorSpace.CS_GRAY,
        ColorSpace.CS_LINEAR_RGB,
        ColorSpace.CS_PYCC,
        ColorSpace.CS_sRGB
    };

    static private final int[] imageTypes = new int[] {
        BufferedImage.TYPE_INT_RGB,
        BufferedImage.TYPE_INT_ARGB,
        BufferedImage.TYPE_INT_ARGB_PRE,
        BufferedImage.TYPE_INT_BGR,
        BufferedImage.TYPE_3BYTE_BGR,
        BufferedImage.TYPE_4BYTE_ABGR,
        BufferedImage.TYPE_4BYTE_ABGR_PRE,
        BufferedImage.TYPE_USHORT_565_RGB,
        BufferedImage.TYPE_USHORT_555_RGB,
        BufferedImage.TYPE_BYTE_GRAY,
        BufferedImage.TYPE_USHORT_GRAY,
        BufferedImage.TYPE_BYTE_BINARY,
        BufferedImage.TYPE_BYTE_INDEXED
    };


    public static void main(String[] args) {
        int nImgTypes = imageTypes.length;
        int nCSTypes = colorSpaceType.length;
        Vector<Thread> threads =
            new Vector<Thread>(nImgTypes*nCSTypes*nCSTypes);

        for (int i = 0; i < nImgTypes; i++) {
            BufferedImage origImage =
                new BufferedImage(300, 300, imageTypes[i]);

            for (int j = 0; j < nCSTypes; j++) {
                for (int k = 0; k < nCSTypes; k++) {

                    Graphics2D g2 = (Graphics2D) origImage.getGraphics();
                    g2.fillRect(0, 0, 300, 150);

                    ColorConvertOp colorOp = getColorConvertOp(j,k);
                    ColorConvert cc = new ColorConvert(origImage, colorOp);

                    Thread colorThread = new Thread(cc);
                    threads.add(colorThread);
                    colorThread.start();
                }
            }
        }

        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Unexpected exception" + e);
        }

        if (failed) {
            throw new RuntimeException("Unexpected exception");
        }
    }

    private static ColorConvertOp getColorConvertOp(int srcIndex,
                                                    int destIndex)
    {
        ColorSpace srcColorSpace = ColorSpace.getInstance(
            colorSpaceType[srcIndex]);
        ColorSpace destColorSpace = ColorSpace.getInstance(
            colorSpaceType[destIndex]);
        return new ColorConvertOp(srcColorSpace, destColorSpace, null);
    }


    static class ColorConvert implements Runnable {

        BufferedImage original = null;
        ColorConvertOp colorOp = null;

        public ColorConvert(BufferedImage orig, ColorConvertOp ccOp) {
            original = orig;
            colorOp = ccOp;
        }

        public void run() {
            try {
                colorOp.filter(original, null);
            } catch (OutOfMemoryError e) {
            /* Skipping OOM exception. We cannot just enlarge stack and heap
             * because it causes problem to disappear
             */

                e.printStackTrace();

            } catch (Exception e) {
                e.printStackTrace();
                failed = true;
            }
        }
    }
}
