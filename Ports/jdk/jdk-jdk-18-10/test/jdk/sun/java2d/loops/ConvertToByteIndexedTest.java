/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     7116979
 * @summary Test verifies whether BufferedImage with primary colors are
 *          stored properly when we draw into ByteIndexed BufferedImage.
 * @run     main ConvertToByteIndexedTest
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public class ConvertToByteIndexedTest {
    static final int[] SRC_TYPES = new int[] {
        BufferedImage.TYPE_INT_RGB,
        BufferedImage.TYPE_INT_ARGB,
        BufferedImage.TYPE_INT_ARGB_PRE,
        BufferedImage.TYPE_INT_BGR,
        BufferedImage.TYPE_3BYTE_BGR,
        BufferedImage.TYPE_4BYTE_ABGR,
        BufferedImage.TYPE_4BYTE_ABGR_PRE,
        BufferedImage.TYPE_USHORT_565_RGB,
        BufferedImage.TYPE_USHORT_555_RGB,
        BufferedImage.TYPE_BYTE_INDEXED};

    static final String[] TYPE_NAME = new String[] {
        "INT_RGB",
        "INT_ARGB",
        "INT_ARGB_PRE",
        "INT_BGR",
        "3BYTE_BGR",
        "4BYTE_ABGR",
        "4BYTE_ABGR_PRE",
        "USHORT_565_RGB",
        "USHORT_555_RGB",
        "BYTE_INDEXED"};

    static final Color[] COLORS = new Color[] {
        //Color.WHITE,
        Color.BLACK,
        Color.RED,
        Color.YELLOW,
        Color.GREEN,
        Color.MAGENTA,
        Color.CYAN,
        Color.BLUE};

    static final HashMap<Integer,String> TYPE_TABLE =
            new HashMap<Integer,String>();

    static {
        for (int i = 0; i < SRC_TYPES.length; i++) {
            TYPE_TABLE.put(new Integer(SRC_TYPES[i]), TYPE_NAME[i]);
        }
    }

    static int width = 50;
    static int height = 50;

    public static void ConvertToByteIndexed(Color color, int srcType) {
        // setup source image and graphics for conversion.
        BufferedImage srcImage = new BufferedImage(width, height, srcType);
        Graphics2D srcG2D = srcImage.createGraphics();
        srcG2D.setColor(color);
        srcG2D.fillRect(0, 0, width, height);

        // setup destination image and graphics for conversion.
        int dstType = BufferedImage.TYPE_BYTE_INDEXED;
        BufferedImage dstImage = new BufferedImage(width, height, dstType);
        Graphics2D dstG2D = (Graphics2D)dstImage.getGraphics();
        // draw source image into Byte Indexed destination
        dstG2D.drawImage(srcImage, 0, 0, null);

        // draw into ARGB image to verify individual pixel value.
        BufferedImage argbImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D argbG2D = (Graphics2D)argbImage.getGraphics();
        argbG2D.drawImage(dstImage, 0, 0, null);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (color.getRGB() != argbImage.getRGB(i, j)) {
                    throw new RuntimeException("Conversion from " +
                            TYPE_TABLE.get(srcType) + " to BYTE_INDEXED is not"
                            + " done properly for " + color);
                }
            }
        }
    }

    public static void main(String args[]) {
        for (int srcType : SRC_TYPES) {
            for (Color color : COLORS) {
                ConvertToByteIndexed(color, srcType);
            }
        }
    }
}
