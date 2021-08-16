/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4278771 8200200
 * @summary This test verifies that various non-sRGB ColorModels correctly
 *          convert to and from default sRGB ColorModel pixel values
 *
 */

import java.awt.image.*;
import java.awt.Transparency;
import java.awt.color.ColorSpace;

public class Non_sRGBCMTest {
    public static void main(String args[]) {
        int[] nBits = {8, 8, 8, 8};
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);
        ComponentColorModel ccm =
            new ComponentColorModel(cs, nBits, true, true,
                                    Transparency.TRANSLUCENT,
                                    DataBuffer.TYPE_BYTE);
        byte[] pixel = new byte[4];
        float val = (0.1f - cs.getMinValue(0)) /
                    (cs.getMaxValue(0) - cs.getMinValue(0));
        pixel[0] = (byte) ((int) (val * 255.0f));
        val = (0.2f - cs.getMinValue(1)) /
              (cs.getMaxValue(1) - cs.getMinValue(1));
        pixel[1] = (byte) ((int) (val * 255.0f));
        val = (0.3f - cs.getMinValue(2)) /
              (cs.getMaxValue(2) - cs.getMinValue(2));
        pixel[2] = (byte) ((int) (val * 255.0f));
        pixel[3] = (byte) ((int) (0.4f * 255.0f));
        // The theoretically correct value for blue, when converting from
        // the specified XYZ value to sRGB, is 248.  This test allows a
        // two-bit difference.  This may have to be adjusted over time
        // to track evolution of the ICC spec, etc.
        if (Math.abs(ccm.getBlue(pixel) - 248) > 2) {
            throw new Error("Problem with ComponentColorModel.getBlue()");
        }
        ccm = new ComponentColorModel(cs, nBits, true, false,
                                      Transparency.TRANSLUCENT,
                                      DataBuffer.TYPE_BYTE);
        val = ((0.1f / 0.4f) - cs.getMinValue(0)) /
              (cs.getMaxValue(0) - cs.getMinValue(0));
        pixel[0] = (byte) ((int) (val * 255.0f));
        val = ((0.2f / 0.4f) - cs.getMinValue(1)) /
              (cs.getMaxValue(1) - cs.getMinValue(1));
        pixel[1] = (byte) ((int) (val * 255.0f));
        val = ((0.3f / 0.4f) - cs.getMinValue(2)) /
              (cs.getMaxValue(2) - cs.getMinValue(2));
        pixel[2] = (byte) ((int) (val * 255.0f));
        pixel[3] = (byte) ((int) (0.4f * 255.0f));
        if (Math.abs(ccm.getBlue(pixel) - 248) > 2) {
            throw new Error("Problem with ComponentColorModel.getBlue()");
        }
        int[] nBits3 = {8, 8, 8};
        ccm = new ComponentColorModel(cs, nBits3, false, false,
                                      Transparency.TRANSLUCENT,
                                      DataBuffer.TYPE_BYTE);
        val = ((0.1f / 0.4f) - cs.getMinValue(0)) /
              (cs.getMaxValue(0) - cs.getMinValue(0));
        pixel[0] = (byte) ((int) (val * 255.0f));
        val = ((0.2f / 0.4f) - cs.getMinValue(1)) /
              (cs.getMaxValue(1) - cs.getMinValue(1));
        pixel[1] = (byte) ((int) (val * 255.0f));
        val = ((0.3f / 0.4f) - cs.getMinValue(2)) /
              (cs.getMaxValue(2) - cs.getMinValue(2));
        pixel[2] = (byte) ((int) (val * 255.0f));
        if (Math.abs(ccm.getBlue(pixel) - 248) > 2) {
            throw new Error("Problem with ComponentColorModel.getBlue()");
        }

        DirectColorModel dcm = new DirectColorModel(
            ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB), 32,
            0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000, true,
            DataBuffer.TYPE_INT);
        int[] ipixel = new int[1];
        ipixel[0] = (127 << 24) | (127 << 16) | (127 << 8) | 127;
        if (Math.abs(dcm.getBlue(ipixel) - 254) > 1) {
            throw new Error("Problem with DirectColorModel.getBlue()");
        }
        dcm = new DirectColorModel(
            ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB), 32,
            0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000, false,
            DataBuffer.TYPE_INT);
        ipixel[0] = (255 << 24) | (255 << 16) | (255 << 8) | 255;
        if (Math.abs(dcm.getBlue(ipixel) - 254) > 1) {
            throw new Error("Problem with DirectColorModel.getBlue()");
        }
        dcm = new DirectColorModel(
            ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB), 32,
            0x00ff0000, 0x0000ff00, 0x000000ff, 0x00000000, false,
            DataBuffer.TYPE_INT);
        ipixel[0] = (255 << 16) | (255 << 8) | 255;
        if (Math.abs(dcm.getBlue(ipixel) - 254) > 1) {
            throw new Error("Problem with DirectColorModel.getBlue()");
        }

        dcm = new DirectColorModel(
            ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB), 32,
            0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000, true,
            DataBuffer.TYPE_INT);
        ipixel[0] = (127 << 24) | (127 << 16) | (127 << 8) | 127;
        int i = dcm.getRGB(ipixel);
        if (Math.abs(((i & 0x00ff0000) >> 16) - 253) > 2) {
            throw new Error("Problem with DirectColorModel.getRGB()");
        }
        int[] idata = (int []) dcm.getDataElements(i, null);
        if (Math.abs(((idata[0] & 0x00ff0000) >> 16) - 125) > 3) {
            throw new Error("Problem with DirectColorModel.getDataElements()");
        }

        ccm = new ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB), nBits, true,
            true, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        pixel[0] = 127;
        pixel[1] = 127;
        pixel[2] = 127;
        pixel[3] = 127;
        i = ccm.getRGB(pixel);
        if (Math.abs(((i & 0x00ff0000) >> 16) - 253) > 2) {
            throw new Error("Problem with ComponentColorModel.getRGB()");
        }
        byte[] bdata = (byte []) ccm.getDataElements(i, null);
        if (Math.abs((bdata[0] & 0xff) - 125) > 3) {
            throw new Error("Problem with" +
                            "ComponentColorModel.getDataElements()");
        }
  }
}
