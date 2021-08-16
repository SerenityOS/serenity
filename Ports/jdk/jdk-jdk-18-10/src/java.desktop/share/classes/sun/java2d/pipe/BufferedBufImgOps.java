/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.java2d.pipe;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ByteLookupTable;
import java.awt.image.ColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.IndexColorModel;
import java.awt.image.Kernel;
import java.awt.image.LookupOp;
import java.awt.image.LookupTable;
import java.awt.image.RescaleOp;
import java.awt.image.ShortLookupTable;
import sun.java2d.SurfaceData;
import static sun.java2d.pipe.BufferedOpCodes.*;

public class BufferedBufImgOps {

    public static void enableBufImgOp(RenderQueue rq, SurfaceData srcData,
                                      BufferedImage srcImg,
                                      BufferedImageOp biop)
    {
        if (biop instanceof ConvolveOp) {
            enableConvolveOp(rq, srcData, (ConvolveOp)biop);
        } else if (biop instanceof RescaleOp) {
            enableRescaleOp(rq, srcData, srcImg, (RescaleOp)biop);
        } else if (biop instanceof LookupOp) {
            enableLookupOp(rq, srcData, srcImg, (LookupOp)biop);
        } else {
            throw new InternalError("Unknown BufferedImageOp");
        }
    }

    public static void disableBufImgOp(RenderQueue rq, BufferedImageOp biop) {
        if (biop instanceof ConvolveOp) {
            disableConvolveOp(rq);
        } else if (biop instanceof RescaleOp) {
            disableRescaleOp(rq);
        } else if (biop instanceof LookupOp) {
            disableLookupOp(rq);
        } else {
            throw new InternalError("Unknown BufferedImageOp");
        }
    }

/**************************** ConvolveOp support ****************************/

    public static boolean isConvolveOpValid(ConvolveOp cop) {
        Kernel kernel = cop.getKernel();
        int kw = kernel.getWidth();
        int kh = kernel.getHeight();
        // REMIND: we currently can only handle 3x3 and 5x5 kernels,
        //         but hopefully this is just a temporary restriction;
        //         see native shader comments for more details
        if (!(kw == 3 && kh == 3) && !(kw == 5 && kh == 5)) {
            return false;
        }
        return true;
    }

    private static void enableConvolveOp(RenderQueue rq,
                                         SurfaceData srcData,
                                         ConvolveOp cop)
    {
        // assert rq.lock.isHeldByCurrentThread();
        boolean edgeZero =
            cop.getEdgeCondition() == ConvolveOp.EDGE_ZERO_FILL;
        Kernel kernel = cop.getKernel();
        int kernelWidth = kernel.getWidth();
        int kernelHeight = kernel.getHeight();
        int kernelSize = kernelWidth * kernelHeight;
        int sizeofFloat = 4;
        int totalBytesRequired = 4 + 8 + 12 + (kernelSize * sizeofFloat);

        RenderBuffer buf = rq.getBuffer();
        rq.ensureCapacityAndAlignment(totalBytesRequired, 4);
        buf.putInt(ENABLE_CONVOLVE_OP);
        buf.putLong(srcData.getNativeOps());
        buf.putInt(edgeZero ? 1 : 0);
        buf.putInt(kernelWidth);
        buf.putInt(kernelHeight);
        buf.put(kernel.getKernelData(null));
    }

    private static void disableConvolveOp(RenderQueue rq) {
        // assert rq.lock.isHeldByCurrentThread();
        RenderBuffer buf = rq.getBuffer();
        rq.ensureCapacity(4);
        buf.putInt(DISABLE_CONVOLVE_OP);
    }

/**************************** RescaleOp support *****************************/

    public static boolean isRescaleOpValid(RescaleOp rop,
                                           BufferedImage srcImg)
    {
        int numFactors = rop.getNumFactors();
        ColorModel srcCM = srcImg.getColorModel();

        if (srcCM instanceof IndexColorModel) {
            throw new
                IllegalArgumentException("Rescaling cannot be "+
                                         "performed on an indexed image");
        }
        if (numFactors != 1 &&
            numFactors != srcCM.getNumColorComponents() &&
            numFactors != srcCM.getNumComponents())
        {
            throw new IllegalArgumentException("Number of scaling constants "+
                                               "does not equal the number of"+
                                               " color or color/alpha"+
                                               " components");
        }

        int csType = srcCM.getColorSpace().getType();
        if (csType != ColorSpace.TYPE_RGB &&
            csType != ColorSpace.TYPE_GRAY)
        {
            // Not prepared to deal with other color spaces
            return false;
        }

        if (numFactors == 2 || numFactors > 4) {
            // Not really prepared to handle this at the native level, so...
            return false;
        }

        return true;
    }

    private static void enableRescaleOp(RenderQueue rq,
                                        SurfaceData srcData,
                                        BufferedImage srcImg,
                                        RescaleOp rop)
    {
        // assert rq.lock.isHeldByCurrentThread();
        ColorModel srcCM = srcImg.getColorModel();
        boolean nonPremult =
            srcCM.hasAlpha() &&
            srcCM.isAlphaPremultiplied();

        /*
         * Note: The user-provided scale factors and offsets are arranged
         * in R/G/B/A order, regardless of the raw data order of the
         * underlying Raster/DataBuffer.  The source image data is ultimately
         * converted into RGBA data when uploaded to an OpenGL texture
         * (even for TYPE_GRAY), so the scale factors and offsets are already
         * in the order expected by the native OpenGL code.
         *
         * However, the offsets provided by the user are in a range dictated
         * by the size of each color/alpha band in the source image.  For
         * example, for 8/8/8 data each offset is in the range [0,255],
         * for 5/5/5 data each offset is in the range [0,31], and so on.
         * The OpenGL shader only thinks in terms of [0,1], so below we need
         * to normalize the user-provided offset values into the range [0,1].
         */
        int numFactors = rop.getNumFactors();
        float[] origScaleFactors = rop.getScaleFactors(null);
        float[] origOffsets = rop.getOffsets(null);

        // To make things easier, we will always pass all four bands
        // down to native code...
        float[] normScaleFactors;
        float[] normOffsets;

        if (numFactors == 1) {
            normScaleFactors = new float[4];
            normOffsets      = new float[4];
            for (int i = 0; i < 3; i++) {
                normScaleFactors[i] = origScaleFactors[0];
                normOffsets[i]      = origOffsets[0];
            }
            // Leave alpha untouched...
            normScaleFactors[3] = 1.0f;
            normOffsets[3]      = 0.0f;
        } else if (numFactors == 3) {
            normScaleFactors = new float[4];
            normOffsets      = new float[4];
            for (int i = 0; i < 3; i++) {
                normScaleFactors[i] = origScaleFactors[i];
                normOffsets[i]      = origOffsets[i];
            }
            // Leave alpha untouched...
            normScaleFactors[3] = 1.0f;
            normOffsets[3]      = 0.0f;
        } else { // (numFactors == 4)
            normScaleFactors = origScaleFactors;
            normOffsets      = origOffsets;
        }

        // The user-provided offsets are specified in the range
        // of each source color band, but the OpenGL shader only wants
        // to deal with data in the range [0,1], so we need to normalize
        // each offset value to the range [0,1] here.
        if (srcCM.getNumComponents() == 1) {
            // Gray data
            int nBits = srcCM.getComponentSize(0);
            int maxValue = (1 << nBits) - 1;
            for (int i = 0; i < 3; i++) {
                normOffsets[i] /= maxValue;
            }
        } else {
            // RGB(A) data
            for (int i = 0; i < srcCM.getNumComponents(); i++) {
                int nBits = srcCM.getComponentSize(i);
                int maxValue = (1 << nBits) - 1;
                normOffsets[i] /= maxValue;
            }
        }

        int sizeofFloat = 4;
        int totalBytesRequired = 4 + 8 + 4 + (4 * sizeofFloat * 2);

        RenderBuffer buf = rq.getBuffer();
        rq.ensureCapacityAndAlignment(totalBytesRequired, 4);
        buf.putInt(ENABLE_RESCALE_OP);
        buf.putLong(srcData.getNativeOps());
        buf.putInt(nonPremult ? 1 : 0);
        buf.put(normScaleFactors);
        buf.put(normOffsets);
    }

    private static void disableRescaleOp(RenderQueue rq) {
        // assert rq.lock.isHeldByCurrentThread();
        RenderBuffer buf = rq.getBuffer();
        rq.ensureCapacity(4);
        buf.putInt(DISABLE_RESCALE_OP);
    }

/**************************** LookupOp support ******************************/

    public static boolean isLookupOpValid(LookupOp lop,
                                          BufferedImage srcImg)
    {
        LookupTable table = lop.getTable();
        int numComps = table.getNumComponents();
        ColorModel srcCM = srcImg.getColorModel();

        if (srcCM instanceof IndexColorModel) {
            throw new
                IllegalArgumentException("LookupOp cannot be "+
                                         "performed on an indexed image");
        }
        if (numComps != 1 &&
            numComps != srcCM.getNumComponents() &&
            numComps != srcCM.getNumColorComponents())
        {
            throw new IllegalArgumentException("Number of arrays in the "+
                                               " lookup table ("+
                                               numComps+
                                               ") is not compatible with"+
                                               " the src image: "+srcImg);
        }

        int csType = srcCM.getColorSpace().getType();
        if (csType != ColorSpace.TYPE_RGB &&
            csType != ColorSpace.TYPE_GRAY)
        {
            // Not prepared to deal with other color spaces
            return false;
        }

        if (numComps == 2 || numComps > 4) {
            // Not really prepared to handle this at the native level, so...
            return false;
        }

        // The LookupTable spec says that "all arrays must be the
        // same size" but unfortunately the constructors do not
        // enforce that.  Also, our native code only works with
        // arrays no larger than 256 elements, so check both of
        // these restrictions here.
        if (table instanceof ByteLookupTable) {
            byte[][] data = ((ByteLookupTable)table).getTable();
            for (int i = 1; i < data.length; i++) {
                if (data[i].length > 256 ||
                    data[i].length != data[i-1].length)
                {
                    return false;
                }
            }
        } else if (table instanceof ShortLookupTable) {
            short[][] data = ((ShortLookupTable)table).getTable();
            for (int i = 1; i < data.length; i++) {
                if (data[i].length > 256 ||
                    data[i].length != data[i-1].length)
                {
                    return false;
                }
            }
        } else {
            return false;
        }

        return true;
    }

    private static void enableLookupOp(RenderQueue rq,
                                       SurfaceData srcData,
                                       BufferedImage srcImg,
                                       LookupOp lop)
    {
        // assert rq.lock.isHeldByCurrentThread();
        boolean nonPremult =
            srcImg.getColorModel().hasAlpha() &&
            srcImg.isAlphaPremultiplied();

        LookupTable table = lop.getTable();
        int numBands = table.getNumComponents();
        int offset = table.getOffset();
        int bandLength;
        int bytesPerElem;
        boolean shortData;

        if (table instanceof ShortLookupTable) {
            short[][] data = ((ShortLookupTable)table).getTable();
            bandLength = data[0].length;
            bytesPerElem = 2;
            shortData = true;
        } else { // (table instanceof ByteLookupTable)
            byte[][] data = ((ByteLookupTable)table).getTable();
            bandLength = data[0].length;
            bytesPerElem = 1;
            shortData = false;
        }

        // Adjust the LUT length so that it ends on a 4-byte boundary
        int totalLutBytes = numBands * bandLength * bytesPerElem;
        int paddedLutBytes = (totalLutBytes + 3) & (~3);
        int padding = paddedLutBytes - totalLutBytes;
        int totalBytesRequired = 4 + 8 + 20 + paddedLutBytes;

        RenderBuffer buf = rq.getBuffer();
        rq.ensureCapacityAndAlignment(totalBytesRequired, 4);
        buf.putInt(ENABLE_LOOKUP_OP);
        buf.putLong(srcData.getNativeOps());
        buf.putInt(nonPremult ? 1 : 0);
        buf.putInt(shortData ? 1 : 0);
        buf.putInt(numBands);
        buf.putInt(bandLength);
        buf.putInt(offset);
        if (shortData) {
            short[][] data = ((ShortLookupTable)table).getTable();
            for (int i = 0; i < numBands; i++) {
                buf.put(data[i]);
            }
        } else {
            byte[][] data = ((ByteLookupTable)table).getTable();
            for (int i = 0; i < numBands; i++) {
                buf.put(data[i]);
            }
        }
        if (padding != 0) {
            buf.position(buf.position() + padding);
        }
    }

    private static void disableLookupOp(RenderQueue rq) {
        // assert rq.lock.isHeldByCurrentThread();
        RenderBuffer buf = rq.getBuffer();
        rq.ensureCapacity(4);
        buf.putInt(DISABLE_LOOKUP_OP);
    }
}
