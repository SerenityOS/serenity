/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.cmm.lcms;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.Raster;

import sun.awt.image.ByteComponentRaster;
import sun.awt.image.IntegerComponentRaster;
import sun.awt.image.ShortComponentRaster;

final class LCMSImageLayout {

    public static int BYTES_SH(int x) {
        return x;
    }

    public static int EXTRA_SH(int x) {
        return x << 7;
    }

    public static int CHANNELS_SH(int x) {
        return x << 3;
    }
    public static final int SWAPFIRST = 1 << 14;
    public static final int DOSWAP = 1 << 10;
    public static final int PT_RGB_8 =
            CHANNELS_SH(3) | BYTES_SH(1);
    public static final int PT_GRAY_8 =
            CHANNELS_SH(1) | BYTES_SH(1);
    public static final int PT_GRAY_16 =
            CHANNELS_SH(1) | BYTES_SH(2);
    public static final int PT_RGBA_8 =
            EXTRA_SH(1) | CHANNELS_SH(3) | BYTES_SH(1);
    public static final int PT_ARGB_8 =
            EXTRA_SH(1) | CHANNELS_SH(3) | BYTES_SH(1) | SWAPFIRST;
    public static final int PT_BGR_8 =
            DOSWAP | CHANNELS_SH(3) | BYTES_SH(1);
    public static final int PT_ABGR_8 =
            DOSWAP | EXTRA_SH(1) | CHANNELS_SH(3) | BYTES_SH(1);
    public static final int PT_BGRA_8 = EXTRA_SH(1) | CHANNELS_SH(3)
            | BYTES_SH(1) | DOSWAP | SWAPFIRST;
    public static final int DT_BYTE = 0;
    public static final int DT_SHORT = 1;
    public static final int DT_INT = 2;
    public static final int DT_DOUBLE = 3;
    boolean isIntPacked = false;
    int pixelType;
    int dataType;
    int width;
    int height;
    int nextRowOffset;
    private int nextPixelOffset;
    int offset;

    /* This flag indicates whether the image can be processed
     * at once by doTransfrom() native call. Otherwise, the
     * image is processed scan by scan.
     */
    private boolean imageAtOnce = false;
    Object dataArray;

    private int dataArrayLength; /* in bytes */

    private LCMSImageLayout(int np, int pixelType, int pixelSize)
            throws ImageLayoutException
    {
        this.pixelType = pixelType;
        width = np;
        height = 1;
        nextPixelOffset = pixelSize;
        nextRowOffset = safeMult(pixelSize, np);
        offset = 0;
    }

    private LCMSImageLayout(int width, int height, int pixelType,
                            int pixelSize)
            throws ImageLayoutException
    {
        this.pixelType = pixelType;
        this.width = width;
        this.height = height;
        nextPixelOffset = pixelSize;
        nextRowOffset = safeMult(pixelSize, width);
        offset = 0;
    }


    public LCMSImageLayout(byte[] data, int np, int pixelType, int pixelSize)
            throws ImageLayoutException
    {
        this(np, pixelType, pixelSize);
        dataType = DT_BYTE;
        dataArray = data;
        dataArrayLength = data.length;

        verify();
    }

    public LCMSImageLayout(short[] data, int np, int pixelType, int pixelSize)
            throws ImageLayoutException
    {
        this(np, pixelType, pixelSize);
        dataType = DT_SHORT;
        dataArray = data;
        dataArrayLength = 2 * data.length;

        verify();
    }

    public LCMSImageLayout(int[] data, int np, int pixelType, int pixelSize)
            throws ImageLayoutException
    {
        this(np, pixelType, pixelSize);
        dataType = DT_INT;
        dataArray = data;
        dataArrayLength = 4 * data.length;

        verify();
    }

    public LCMSImageLayout(double[] data, int np, int pixelType, int pixelSize)
            throws ImageLayoutException
    {
        this(np, pixelType, pixelSize);
        dataType = DT_DOUBLE;
        dataArray = data;
        dataArrayLength = 8 * data.length;

        verify();
    }

    private LCMSImageLayout() {
    }

    /* This method creates a layout object for given image.
     * Returns null if the image is not supported by current implementation.
     */
    public static LCMSImageLayout createImageLayout(BufferedImage image) throws ImageLayoutException {
        LCMSImageLayout l = new LCMSImageLayout();

        switch (image.getType()) {
            case BufferedImage.TYPE_INT_RGB:
                l.pixelType = PT_ARGB_8;
                l.isIntPacked = true;
                break;
            case BufferedImage.TYPE_INT_ARGB:
                l.pixelType = PT_ARGB_8;
                l.isIntPacked = true;
                break;
            case BufferedImage.TYPE_INT_BGR:
                l.pixelType = PT_ABGR_8;
                l.isIntPacked = true;
                break;
            case BufferedImage.TYPE_3BYTE_BGR:
                l.pixelType = PT_BGR_8;
                break;
            case BufferedImage.TYPE_4BYTE_ABGR:
                l.pixelType = PT_ABGR_8;
                break;
            case BufferedImage.TYPE_BYTE_GRAY:
                l.pixelType = PT_GRAY_8;
                break;
            case BufferedImage.TYPE_USHORT_GRAY:
                l.pixelType = PT_GRAY_16;
                break;
            default:
                /* ColorConvertOp creates component images as
                 * default destination, so this kind of images
                 * has to be supported.
                 */
                ColorModel cm = image.getColorModel();
                /* todo
                 * Our generic code for rasters does not support alpha channels,
                 * but it would be good to improve it when it is used from here.
                 * See "createImageLayout(image.getRaster())" below.
                 */
                if (!cm.hasAlpha() && cm instanceof ComponentColorModel) {
                    ComponentColorModel ccm = (ComponentColorModel) cm;

                    // verify whether the component size is fine
                    int[] cs = ccm.getComponentSize();
                    for (int s : cs) {
                        if (s != 8) {
                            return null;
                        }
                    }

                    return createImageLayout(image.getRaster());

                }
                return null;
        }

        l.width = image.getWidth();
        l.height = image.getHeight();

        switch (image.getType()) {
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_BGR:
                do {
                    IntegerComponentRaster intRaster = (IntegerComponentRaster)
                            image.getRaster();
                    l.nextRowOffset = safeMult(4, intRaster.getScanlineStride());
                    l.nextPixelOffset = safeMult(4, intRaster.getPixelStride());
                    l.offset = safeMult(4, intRaster.getDataOffset(0));
                    l.dataArray = intRaster.getDataStorage();
                    l.dataArrayLength = 4 * intRaster.getDataStorage().length;
                    l.dataType = DT_INT;

                    if (l.nextRowOffset == l.width * 4 * intRaster.getPixelStride()) {
                        l.imageAtOnce = true;
                    }
                } while (false);
                break;

            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_4BYTE_ABGR:
                do {
                    ByteComponentRaster byteRaster = (ByteComponentRaster)
                            image.getRaster();
                    l.nextRowOffset = byteRaster.getScanlineStride();
                    l.nextPixelOffset = byteRaster.getPixelStride();

                    int firstBand = image.getSampleModel().getNumBands() - 1;
                    l.offset = byteRaster.getDataOffset(firstBand);
                    l.dataArray = byteRaster.getDataStorage();
                    l.dataArrayLength = byteRaster.getDataStorage().length;
                    l.dataType = DT_BYTE;
                    if (l.nextRowOffset == l.width * byteRaster.getPixelStride()) {
                        l.imageAtOnce = true;
                    }
                } while (false);
                break;

            case BufferedImage.TYPE_BYTE_GRAY:
                do {
                    ByteComponentRaster byteRaster = (ByteComponentRaster)
                            image.getRaster();
                    l.nextRowOffset = byteRaster.getScanlineStride();
                    l.nextPixelOffset = byteRaster.getPixelStride();

                    l.dataArrayLength = byteRaster.getDataStorage().length;
                    l.offset = byteRaster.getDataOffset(0);
                    l.dataArray = byteRaster.getDataStorage();
                    l.dataType = DT_BYTE;

                    if (l.nextRowOffset == l.width * byteRaster.getPixelStride()) {
                        l.imageAtOnce = true;
                    }
                } while (false);
                break;

            case BufferedImage.TYPE_USHORT_GRAY:
                do {
                    ShortComponentRaster shortRaster = (ShortComponentRaster)
                            image.getRaster();
                    l.nextRowOffset = safeMult(2, shortRaster.getScanlineStride());
                    l.nextPixelOffset = safeMult(2, shortRaster.getPixelStride());

                    l.offset = safeMult(2, shortRaster.getDataOffset(0));
                    l.dataArray = shortRaster.getDataStorage();
                    l.dataArrayLength = 2 * shortRaster.getDataStorage().length;
                    l.dataType = DT_SHORT;

                    if (l.nextRowOffset == l.width * 2 * shortRaster.getPixelStride()) {
                        l.imageAtOnce = true;
                    }
                } while (false);
                break;
            default:
                return null;
        }
        l.verify();
        return l;
    }

    private static enum BandOrder {
        DIRECT,
        INVERTED,
        ARBITRARY,
        UNKNOWN;

        public static BandOrder getBandOrder(int[] bandOffsets) {
            BandOrder order = UNKNOWN;

            int numBands = bandOffsets.length;

            for (int i = 0; (order != ARBITRARY) && (i < bandOffsets.length); i++) {
                switch (order) {
                    case UNKNOWN:
                        if (bandOffsets[i] == i) {
                            order = DIRECT;
                        } else if (bandOffsets[i] == (numBands - 1 - i)) {
                            order = INVERTED;
                        } else {
                            order = ARBITRARY;
                        }
                        break;
                    case DIRECT:
                        if (bandOffsets[i] != i) {
                            order = ARBITRARY;
                        }
                        break;
                    case INVERTED:
                        if (bandOffsets[i] != (numBands - 1 - i)) {
                            order = ARBITRARY;
                        }
                        break;
                }
            }
            return order;
        }
    }

    private void verify() throws ImageLayoutException {

        if (offset < 0 || offset >= dataArrayLength) {
            throw new ImageLayoutException("Invalid image layout");
        }

        if (nextPixelOffset != getBytesPerPixel(pixelType)) {
            throw new ImageLayoutException("Invalid image layout");
        }

        int lastScanOffset = safeMult(nextRowOffset, (height - 1));

        int lastPixelOffset = safeMult(nextPixelOffset, (width -1 ));

        lastPixelOffset = safeAdd(lastPixelOffset, lastScanOffset);

        int off = safeAdd(offset, lastPixelOffset);

        if (off < 0 || off >= dataArrayLength) {
            throw new ImageLayoutException("Invalid image layout");
        }
    }

    static int safeAdd(int a, int b) throws ImageLayoutException {
        long res = a;
        res += b;
        if (res < Integer.MIN_VALUE || res > Integer.MAX_VALUE) {
            throw new ImageLayoutException("Invalid image layout");
        }
        return (int)res;
    }

    static int safeMult(int a, int b) throws ImageLayoutException {
        long res = a;
        res *= b;
        if (res < Integer.MIN_VALUE || res > Integer.MAX_VALUE) {
            throw new ImageLayoutException("Invalid image layout");
        }
        return (int)res;
    }

    @SuppressWarnings("serial") // JDK-implementation class
    public static class ImageLayoutException extends Exception {
        public ImageLayoutException(String message) {
            super(message);
        }
    }
    public static LCMSImageLayout createImageLayout(Raster r) {
        LCMSImageLayout l = new LCMSImageLayout();
        if (r instanceof ByteComponentRaster &&
                r.getSampleModel() instanceof ComponentSampleModel) {
            ByteComponentRaster br = (ByteComponentRaster)r;

            ComponentSampleModel csm = (ComponentSampleModel)r.getSampleModel();

            l.pixelType = CHANNELS_SH(br.getNumBands()) | BYTES_SH(1);

            int[] bandOffsets = csm.getBandOffsets();
            BandOrder order = BandOrder.getBandOrder(bandOffsets);

            int firstBand = 0;
            switch (order) {
                case INVERTED:
                    l.pixelType |= DOSWAP;
                    firstBand  = csm.getNumBands() - 1;
                    break;
                case DIRECT:
                    // do nothing
                    break;
                default:
                    // unable to create the image layout;
                    return null;
            }

            l.nextRowOffset = br.getScanlineStride();
            l.nextPixelOffset = br.getPixelStride();

            l.offset = br.getDataOffset(firstBand);
            l.dataArray = br.getDataStorage();
            l.dataType = DT_BYTE;

            l.width = br.getWidth();
            l.height = br.getHeight();

            if (l.nextRowOffset == l.width * br.getPixelStride()) {
                l.imageAtOnce = true;
            }
            return l;
        }
        return null;
    }

    /**
     * Derives number of bytes per pixel from the pixel format.
     * Following bit fields are used here:
     *  [0..2] - bytes per sample
     *  [3..6] - number of color samples per pixel
     *  [7..9] - number of non-color samples per pixel
     *
     * A complete description of the pixel format can be found
     * here: lcms2.h, lines 651 - 667.
     *
     * @param pixelType pixel format in lcms2 notation.
     * @return number of bytes per pixel for given pixel format.
     */
    private static int getBytesPerPixel(int pixelType) {
        int bytesPerSample = (0x7 & pixelType);
        int colorSamplesPerPixel = 0xF & (pixelType >> 3);
        int extraSamplesPerPixel = 0x7 & (pixelType >> 7);

        return bytesPerSample * (colorSamplesPerPixel + extraSamplesPerPixel);
    }
}
