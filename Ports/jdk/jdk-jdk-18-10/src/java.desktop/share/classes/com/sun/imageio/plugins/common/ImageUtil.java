/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.common;

import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;

public class ImageUtil {
    /**
     * Creates a {@code ColorModel} that may be used with the
     * specified {@code SampleModel}.  If a suitable
     * {@code ColorModel} cannot be found, this method returns
     * {@code null}.
     *
     * <p> Suitable {@code ColorModel}s are guaranteed to exist
     * for all instances of {@code ComponentSampleModel}.
     * For 1- and 3- banded {@code SampleModel}s, the returned
     * {@code ColorModel} will be opaque.  For 2- and 4-banded
     * {@code SampleModel}s, the output will use alpha transparency
     * which is not premultiplied.  1- and 2-banded data will use a
     * grayscale {@code ColorSpace}, and 3- and 4-banded data a sRGB
     * {@code ColorSpace}. Data with 5 or more bands will have a
     * {@code BogusColorSpace}.</p>
     *
     * <p>An instance of {@code DirectColorModel} will be created for
     * instances of {@code SinglePixelPackedSampleModel} with no more
     * than 4 bands.</p>
     *
     * <p>An instance of {@code IndexColorModel} will be created for
     * instances of {@code MultiPixelPackedSampleModel}. The colormap
     * will be a grayscale ramp with <code>1&nbsp;&lt;&lt;&nbsp;numberOfBits</code>
     * entries ranging from zero to at most 255.</p>
     *
     * @return An instance of {@code ColorModel} that is suitable for
     *         the supplied {@code SampleModel}, or {@code null}.
     *
     * @throws IllegalArgumentException  If {@code sampleModel} is
     *         {@code null}.
     */
    public static final ColorModel createColorModel(SampleModel sampleModel) {
        // Check the parameter.
        if(sampleModel == null) {
            throw new IllegalArgumentException("sampleModel == null!");
        }

        // Get the data type.
        int dataType = sampleModel.getDataType();

        // Check the data type
        switch(dataType) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE:
            break;
        default:
            // Return null for other types.
            return null;
        }

        // The return variable.
        ColorModel colorModel = null;

        // Get the sample size.
        int[] sampleSize = sampleModel.getSampleSize();

        // Create a Component ColorModel.
        if(sampleModel instanceof ComponentSampleModel) {
            // Get the number of bands.
            int numBands = sampleModel.getNumBands();

            // Determine the color space.
            ColorSpace colorSpace = null;
            if(numBands <= 2) {
                colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            } else if(numBands <= 4) {
                colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
            } else {
                colorSpace = new BogusColorSpace(numBands);
            }

            boolean hasAlpha = (numBands == 2) || (numBands == 4);
            boolean isAlphaPremultiplied = false;
            int transparency = hasAlpha ?
                Transparency.TRANSLUCENT : Transparency.OPAQUE;

            colorModel = new ComponentColorModel(colorSpace,
                                                 sampleSize,
                                                 hasAlpha,
                                                 isAlphaPremultiplied,
                                                 transparency,
                                                 dataType);
        } else if (sampleModel.getNumBands() <= 4 &&
                   sampleModel instanceof SinglePixelPackedSampleModel) {
            SinglePixelPackedSampleModel sppsm =
                (SinglePixelPackedSampleModel)sampleModel;

            int[] bitMasks = sppsm.getBitMasks();
            int rmask = 0;
            int gmask = 0;
            int bmask = 0;
            int amask = 0;

            int numBands = bitMasks.length;
            if (numBands <= 2) {
                rmask = gmask = bmask = bitMasks[0];
                if (numBands == 2) {
                    amask = bitMasks[1];
                }
            } else {
                rmask = bitMasks[0];
                gmask = bitMasks[1];
                bmask = bitMasks[2];
                if (numBands == 4) {
                    amask = bitMasks[3];
                }
            }

            int bits = 0;
            for (int i = 0; i < sampleSize.length; i++) {
                bits += sampleSize[i];
            }

            return new DirectColorModel(bits, rmask, gmask, bmask, amask);

        } else if(sampleModel instanceof MultiPixelPackedSampleModel) {
            // Load the colormap with a ramp.
            int bitsPerSample = sampleSize[0];
            int numEntries = 1 << bitsPerSample;
            byte[] map = new byte[numEntries];
            for (int i = 0; i < numEntries; i++) {
                map[i] = (byte)(i*255/(numEntries - 1));
            }

            colorModel = new IndexColorModel(bitsPerSample, numEntries,
                                             map, map, map);

        }

        return colorModel;
    }

    /**
     * For the case of binary data ({@code isBinary()} returns
     * {@code true}), return the binary data as a packed byte array.
     * The data will be packed as eight bits per byte with no bit offset,
     * i.e., the first bit in each image line will be the left-most of the
     * first byte of the line.  The line stride in bytes will be
     * {@code (int)((getWidth()+7)/8)}.  The length of the returned
     * array will be the line stride multiplied by {@code getHeight()}
     *
     * @return the binary data as a packed array of bytes with zero offset
     * of {@code null} if the data are not binary.
     * @throws IllegalArgumentException if {@code isBinary()} returns
     * {@code false} with the {@code SampleModel} of the
     * supplied {@code Raster} as argument.
     */
    public static byte[] getPackedBinaryData(Raster raster,
                                             Rectangle rect) {
        SampleModel sm = raster.getSampleModel();
        if(!isBinary(sm)) {
            throw new IllegalArgumentException(I18N.getString("ImageUtil0"));
        }

        int rectX = rect.x;
        int rectY = rect.y;
        int rectWidth = rect.width;
        int rectHeight = rect.height;

        DataBuffer dataBuffer = raster.getDataBuffer();

        int dx = rectX - raster.getSampleModelTranslateX();
        int dy = rectY - raster.getSampleModelTranslateY();

        MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel)sm;
        int lineStride = mpp.getScanlineStride();
        int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
        int bitOffset = mpp.getBitOffset(dx);

        int numBytesPerRow = (rectWidth + 7)/8;
        if(dataBuffer instanceof DataBufferByte &&
           eltOffset == 0 && bitOffset == 0 &&
           numBytesPerRow == lineStride &&
           ((DataBufferByte)dataBuffer).getData().length ==
           numBytesPerRow*rectHeight) {
            return ((DataBufferByte)dataBuffer).getData();
        }

        byte[] binaryDataArray = new byte[numBytesPerRow*rectHeight];

        int b = 0;

        if(bitOffset == 0) {
            if(dataBuffer instanceof DataBufferByte) {
                byte[] data = ((DataBufferByte)dataBuffer).getData();
                int stride = numBytesPerRow;
                int offset = 0;
                for(int y = 0; y < rectHeight; y++) {
                    System.arraycopy(data, eltOffset,
                                     binaryDataArray, offset,
                                     stride);
                    offset += stride;
                    eltOffset += lineStride;
                }
            } else if(dataBuffer instanceof DataBufferShort ||
                      dataBuffer instanceof DataBufferUShort) {
                short[] data = dataBuffer instanceof DataBufferShort ?
                    ((DataBufferShort)dataBuffer).getData() :
                    ((DataBufferUShort)dataBuffer).getData();

                for(int y = 0; y < rectHeight; y++) {
                    int xRemaining = rectWidth;
                    int i = eltOffset;
                    while(xRemaining > 8) {
                        short datum = data[i++];
                        binaryDataArray[b++] = (byte)((datum >>> 8) & 0xFF);
                        binaryDataArray[b++] = (byte)(datum & 0xFF);
                        xRemaining -= 16;
                    }
                    if(xRemaining > 0) {
                        binaryDataArray[b++] = (byte)((data[i] >>> 8) & 0XFF);
                    }
                    eltOffset += lineStride;
                }
            } else if(dataBuffer instanceof DataBufferInt) {
                int[] data = ((DataBufferInt)dataBuffer).getData();

                for(int y = 0; y < rectHeight; y++) {
                    int xRemaining = rectWidth;
                    int i = eltOffset;
                    while(xRemaining > 24) {
                        int datum = data[i++];
                        binaryDataArray[b++] = (byte)((datum >>> 24) & 0xFF);
                        binaryDataArray[b++] = (byte)((datum >>> 16) & 0xFF);
                        binaryDataArray[b++] = (byte)((datum >>> 8) & 0xFF);
                        binaryDataArray[b++] = (byte)(datum & 0xFF);
                        xRemaining -= 32;
                    }
                    int shift = 24;
                    while(xRemaining > 0) {
                        binaryDataArray[b++] =
                            (byte)((data[i] >>> shift) & 0xFF);
                        shift -= 8;
                        xRemaining -= 8;
                    }
                    eltOffset += lineStride;
                }
            }
        } else { // bitOffset != 0
            if(dataBuffer instanceof DataBufferByte) {
                byte[] data = ((DataBufferByte)dataBuffer).getData();

                if((bitOffset & 7) == 0) {
                    int stride = numBytesPerRow;
                    int offset = 0;
                    for(int y = 0; y < rectHeight; y++) {
                        System.arraycopy(data, eltOffset,
                                         binaryDataArray, offset,
                                         stride);
                        offset += stride;
                        eltOffset += lineStride;
                    }
                } else { // bitOffset % 8 != 0
                    int leftShift = bitOffset & 7;
                    int rightShift = 8 - leftShift;
                    for(int y = 0; y < rectHeight; y++) {
                        int i = eltOffset;
                        int xRemaining = rectWidth;
                        while(xRemaining > 0) {
                            if(xRemaining > rightShift) {
                                binaryDataArray[b++] =
                                    (byte)(((data[i++]&0xFF) << leftShift) |
                                           ((data[i]&0xFF) >>> rightShift));
                            } else {
                                binaryDataArray[b++] =
                                    (byte)((data[i]&0xFF) << leftShift);
                            }
                            xRemaining -= 8;
                        }
                        eltOffset += lineStride;
                    }
                }
            } else if(dataBuffer instanceof DataBufferShort ||
                      dataBuffer instanceof DataBufferUShort) {
                short[] data = dataBuffer instanceof DataBufferShort ?
                    ((DataBufferShort)dataBuffer).getData() :
                    ((DataBufferUShort)dataBuffer).getData();

                for(int y = 0; y < rectHeight; y++) {
                    int bOffset = bitOffset;
                    for(int x = 0; x < rectWidth; x += 8, bOffset += 8) {
                        int i = eltOffset + bOffset/16;
                        int mod = bOffset % 16;
                        int left = data[i] & 0xFFFF;
                        if(mod <= 8) {
                            binaryDataArray[b++] = (byte)(left >>> (8 - mod));
                        } else {
                            int delta = mod - 8;
                            int right = data[i+1] & 0xFFFF;
                            binaryDataArray[b++] =
                                (byte)((left << delta) |
                                       (right >>> (16 - delta)));
                        }
                    }
                    eltOffset += lineStride;
                }
            } else if(dataBuffer instanceof DataBufferInt) {
                int[] data = ((DataBufferInt)dataBuffer).getData();

                for(int y = 0; y < rectHeight; y++) {
                    int bOffset = bitOffset;
                    for(int x = 0; x < rectWidth; x += 8, bOffset += 8) {
                        int i = eltOffset + bOffset/32;
                        int mod = bOffset % 32;
                        int left = data[i];
                        if(mod <= 24) {
                            binaryDataArray[b++] =
                                (byte)(left >>> (24 - mod));
                        } else {
                            int delta = mod - 24;
                            int right = data[i+1];
                            binaryDataArray[b++] =
                                (byte)((left << delta) |
                                       (right >>> (32 - delta)));
                        }
                    }
                    eltOffset += lineStride;
                }
            }
        }

        return binaryDataArray;
    }

    /**
     * Returns the binary data unpacked into an array of bytes.
     * The line stride will be the width of the {@code Raster}.
     *
     * @throws IllegalArgumentException if {@code isBinary()} returns
     * {@code false} with the {@code SampleModel} of the
     * supplied {@code Raster} as argument.
     */
    public static byte[] getUnpackedBinaryData(Raster raster,
                                               Rectangle rect) {
        SampleModel sm = raster.getSampleModel();
        if(!isBinary(sm)) {
            throw new IllegalArgumentException(I18N.getString("ImageUtil0"));
        }

        int rectX = rect.x;
        int rectY = rect.y;
        int rectWidth = rect.width;
        int rectHeight = rect.height;

        DataBuffer dataBuffer = raster.getDataBuffer();

        int dx = rectX - raster.getSampleModelTranslateX();
        int dy = rectY - raster.getSampleModelTranslateY();

        MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel)sm;
        int lineStride = mpp.getScanlineStride();
        int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
        int bitOffset = mpp.getBitOffset(dx);

        byte[] bdata = new byte[rectWidth*rectHeight];
        int maxY = rectY + rectHeight;
        int maxX = rectX + rectWidth;
        int k = 0;

        if(dataBuffer instanceof DataBufferByte) {
            byte[] data = ((DataBufferByte)dataBuffer).getData();
            for(int y = rectY; y < maxY; y++) {
                int bOffset = eltOffset*8 + bitOffset;
                for(int x = rectX; x < maxX; x++) {
                    byte b = data[bOffset/8];
                    bdata[k++] =
                        (byte)((b >>> (7 - bOffset & 7)) & 0x0000001);
                    bOffset++;
                }
                eltOffset += lineStride;
            }
        } else if(dataBuffer instanceof DataBufferShort ||
                  dataBuffer instanceof DataBufferUShort) {
            short[] data = dataBuffer instanceof DataBufferShort ?
                ((DataBufferShort)dataBuffer).getData() :
                ((DataBufferUShort)dataBuffer).getData();
            for(int y = rectY; y < maxY; y++) {
                int bOffset = eltOffset*16 + bitOffset;
                for(int x = rectX; x < maxX; x++) {
                    short s = data[bOffset/16];
                    bdata[k++] =
                        (byte)((s >>> (15 - bOffset % 16)) &
                               0x0000001);
                    bOffset++;
                }
                eltOffset += lineStride;
            }
        } else if(dataBuffer instanceof DataBufferInt) {
            int[] data = ((DataBufferInt)dataBuffer).getData();
            for(int y = rectY; y < maxY; y++) {
                int bOffset = eltOffset*32 + bitOffset;
                for(int x = rectX; x < maxX; x++) {
                    int i = data[bOffset/32];
                    bdata[k++] =
                        (byte)((i >>> (31 - bOffset % 32)) &
                               0x0000001);
                    bOffset++;
                }
                eltOffset += lineStride;
            }
        }

        return bdata;
    }

    /**
     * Sets the supplied {@code Raster}'s data from an array
     * of packed binary data of the form returned by
     * {@code getPackedBinaryData()}.
     *
     * @throws IllegalArgumentException if {@code isBinary()} returns
     * {@code false} with the {@code SampleModel} of the
     * supplied {@code Raster} as argument.
     */
    public static void setPackedBinaryData(byte[] binaryDataArray,
                                           WritableRaster raster,
                                           Rectangle rect) {
        SampleModel sm = raster.getSampleModel();
        if(!isBinary(sm)) {
            throw new IllegalArgumentException(I18N.getString("ImageUtil0"));
        }

        int rectX = rect.x;
        int rectY = rect.y;
        int rectWidth = rect.width;
        int rectHeight = rect.height;

        DataBuffer dataBuffer = raster.getDataBuffer();

        int dx = rectX - raster.getSampleModelTranslateX();
        int dy = rectY - raster.getSampleModelTranslateY();

        MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel)sm;
        int lineStride = mpp.getScanlineStride();
        int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
        int bitOffset = mpp.getBitOffset(dx);

        int b = 0;

        if(bitOffset == 0) {
            if(dataBuffer instanceof DataBufferByte) {
                byte[] data = ((DataBufferByte)dataBuffer).getData();
                if(data == binaryDataArray) {
                    // Optimal case: simply return.
                    return;
                }
                int stride = (rectWidth + 7)/8;
                int offset = 0;
                for(int y = 0; y < rectHeight; y++) {
                    System.arraycopy(binaryDataArray, offset,
                                     data, eltOffset,
                                     stride);
                    offset += stride;
                    eltOffset += lineStride;
                }
            } else if(dataBuffer instanceof DataBufferShort ||
                      dataBuffer instanceof DataBufferUShort) {
                short[] data = dataBuffer instanceof DataBufferShort ?
                    ((DataBufferShort)dataBuffer).getData() :
                    ((DataBufferUShort)dataBuffer).getData();

                for(int y = 0; y < rectHeight; y++) {
                    int xRemaining = rectWidth;
                    int i = eltOffset;
                    while(xRemaining > 8) {
                        data[i++] =
                            (short)(((binaryDataArray[b++] & 0xFF) << 8) |
                                    (binaryDataArray[b++] & 0xFF));
                        xRemaining -= 16;
                    }
                    if(xRemaining > 0) {
                        data[i++] =
                            (short)((binaryDataArray[b++] & 0xFF) << 8);
                    }
                    eltOffset += lineStride;
                }
            } else if(dataBuffer instanceof DataBufferInt) {
                int[] data = ((DataBufferInt)dataBuffer).getData();

                for(int y = 0; y < rectHeight; y++) {
                    int xRemaining = rectWidth;
                    int i = eltOffset;
                    while(xRemaining > 24) {
                        data[i++] =
                            (((binaryDataArray[b++] & 0xFF) << 24) |
                             ((binaryDataArray[b++] & 0xFF) << 16) |
                             ((binaryDataArray[b++] & 0xFF) << 8) |
                             (binaryDataArray[b++] & 0xFF));
                        xRemaining -= 32;
                    }
                    int shift = 24;
                    while(xRemaining > 0) {
                        data[i] |= ((binaryDataArray[b++] & 0xFF) << shift);
                        shift -= 8;
                        xRemaining -= 8;
                    }
                    eltOffset += lineStride;
                }
            }
        } else { // bitOffset != 0
            int stride = (rectWidth + 7)/8;
            int offset = 0;
            if(dataBuffer instanceof DataBufferByte) {
                byte[] data = ((DataBufferByte)dataBuffer).getData();

                if((bitOffset & 7) == 0) {
                    for(int y = 0; y < rectHeight; y++) {
                        System.arraycopy(binaryDataArray, offset,
                                         data, eltOffset,
                                         stride);
                        offset += stride;
                        eltOffset += lineStride;
                    }
                } else { // bitOffset % 8 != 0
                    int rightShift = bitOffset & 7;
                    int leftShift = 8 - rightShift;
                    int leftShift8 = 8 + leftShift;
                    int mask = (byte)(255<<leftShift);
                    int mask1 = (byte)~mask;

                    for(int y = 0; y < rectHeight; y++) {
                        int i = eltOffset;
                        int xRemaining = rectWidth;
                        while(xRemaining > 0) {
                            byte datum = binaryDataArray[b++];

                            if (xRemaining > leftShift8) {
                                // when all the bits in this BYTE will be set
                                // into the data buffer.
                                data[i] = (byte)((data[i] & mask ) |
                                    ((datum&0xFF) >>> rightShift));
                                data[++i] = (byte)((datum & 0xFF) << leftShift);
                            } else if (xRemaining > leftShift) {
                                // All the "leftShift" high bits will be set
                                // into the data buffer.  But not all the
                                // "rightShift" low bits will be set.
                                data[i] = (byte)((data[i] & mask ) |
                                    ((datum&0xFF) >>> rightShift));
                                i++;
                                data[i] =
                                    (byte)((data[i] & mask1) | ((datum & 0xFF) << leftShift));
                            }
                            else {
                                // Less than "leftShift" high bits will be set.
                                int remainMask = (1 << leftShift - xRemaining) - 1;
                                data[i] =
                                    (byte)((data[i] & (mask | remainMask)) |
                                    (datum&0xFF) >>> rightShift & ~remainMask);
                            }
                            xRemaining -= 8;
                        }
                        eltOffset += lineStride;
                    }
                }
            } else if(dataBuffer instanceof DataBufferShort ||
                      dataBuffer instanceof DataBufferUShort) {
                short[] data = dataBuffer instanceof DataBufferShort ?
                    ((DataBufferShort)dataBuffer).getData() :
                    ((DataBufferUShort)dataBuffer).getData();

                int rightShift = bitOffset & 7;
                int leftShift = 8 - rightShift;
                int leftShift16 = 16 + leftShift;
                int mask = (short)(~(255 << leftShift));
                int mask1 = (short)(65535 << leftShift);
                int mask2 = (short)~mask1;

                for(int y = 0; y < rectHeight; y++) {
                    int bOffset = bitOffset;
                    int xRemaining = rectWidth;
                    for(int x = 0; x < rectWidth;
                        x += 8, bOffset += 8, xRemaining -= 8) {
                        int i = eltOffset + (bOffset >> 4);
                        int mod = bOffset & 15;
                        int datum = binaryDataArray[b++] & 0xFF;
                        if(mod <= 8) {
                            // This BYTE is set into one SHORT
                            if (xRemaining < 8) {
                                // Mask the bits to be set.
                                datum &= 255 << 8 - xRemaining;
                            }
                            data[i] = (short)((data[i] & mask) | (datum << leftShift));
                        } else if (xRemaining > leftShift16) {
                            // This BYTE will be set into two SHORTs
                            data[i] = (short)((data[i] & mask1) | ((datum >>> rightShift)&0xFFFF));
                            data[++i] =
                                (short)((datum << leftShift)&0xFFFF);
                        } else if (xRemaining > leftShift) {
                            // This BYTE will be set into two SHORTs;
                            // But not all the low bits will be set into SHORT
                            data[i] = (short)((data[i] & mask1) | ((datum >>> rightShift)&0xFFFF));
                            i++;
                            data[i] =
                                (short)((data[i] & mask2) | ((datum << leftShift)&0xFFFF));
                        } else {
                            // Only some of the high bits will be set into
                            // SHORTs
                            int remainMask = (1 << leftShift - xRemaining) - 1;
                            data[i] = (short)((data[i] & (mask1 | remainMask)) |
                                      ((datum >>> rightShift)&0xFFFF & ~remainMask));
                        }
                    }
                    eltOffset += lineStride;
                }
            } else if(dataBuffer instanceof DataBufferInt) {
                int[] data = ((DataBufferInt)dataBuffer).getData();
                int rightShift = bitOffset & 7;
                int leftShift = 8 - rightShift;
                int leftShift32 = 32 + leftShift;
                int mask = 0xFFFFFFFF << leftShift;
                int mask1 = ~mask;

                for(int y = 0; y < rectHeight; y++) {
                    int bOffset = bitOffset;
                    int xRemaining = rectWidth;
                    for(int x = 0; x < rectWidth;
                        x += 8, bOffset += 8, xRemaining -= 8) {
                        int i = eltOffset + (bOffset >> 5);
                        int mod = bOffset & 31;
                        int datum = binaryDataArray[b++] & 0xFF;
                        if(mod <= 24) {
                            // This BYTE is set into one INT
                            int shift = 24 - mod;
                            if (xRemaining < 8) {
                                // Mask the bits to be set.
                                datum &= 255 << 8 - xRemaining;
                            }
                            data[i] = (data[i] & (~(255 << shift))) | (datum << shift);
                        } else if (xRemaining > leftShift32) {
                            // All the bits of this BYTE will be set into two INTs
                            data[i] = (data[i] & mask) | (datum >>> rightShift);
                            data[++i] = datum << leftShift;
                        } else if (xRemaining > leftShift) {
                            // This BYTE will be set into two INTs;
                            // But not all the low bits will be set into INT
                            data[i] = (data[i] & mask) | (datum >>> rightShift);
                            i++;
                            data[i] = (data[i] & mask1) | (datum << leftShift);
                        } else {
                            // Only some of the high bits will be set into INT
                            int remainMask = (1 << leftShift - xRemaining) - 1;
                            data[i] = (data[i] & (mask | remainMask)) |
                                      (datum >>> rightShift & ~remainMask);
                        }
                    }
                    eltOffset += lineStride;
                }
            }
        }
    }

    /**
     * Copies data into the packed array of the {@code Raster}
     * from an array of unpacked data of the form returned by
     * {@code getUnpackedBinaryData()}.
     *
     * <p> If the data are binary, then the target bit will be set if
     * and only if the corresponding byte is non-zero.
     *
     * @throws IllegalArgumentException if {@code isBinary()} returns
     * {@code false} with the {@code SampleModel} of the
     * supplied {@code Raster} as argument.
     */
    public static void setUnpackedBinaryData(byte[] bdata,
                                             WritableRaster raster,
                                             Rectangle rect) {
        SampleModel sm = raster.getSampleModel();
        if(!isBinary(sm)) {
            throw new IllegalArgumentException(I18N.getString("ImageUtil0"));
        }

        int rectX = rect.x;
        int rectY = rect.y;
        int rectWidth = rect.width;
        int rectHeight = rect.height;

        DataBuffer dataBuffer = raster.getDataBuffer();

        int dx = rectX - raster.getSampleModelTranslateX();
        int dy = rectY - raster.getSampleModelTranslateY();

        MultiPixelPackedSampleModel mpp = (MultiPixelPackedSampleModel)sm;
        int lineStride = mpp.getScanlineStride();
        int eltOffset = dataBuffer.getOffset() + mpp.getOffset(dx, dy);
        int bitOffset = mpp.getBitOffset(dx);

        int k = 0;

        if(dataBuffer instanceof DataBufferByte) {
            byte[] data = ((DataBufferByte)dataBuffer).getData();
            for(int y = 0; y < rectHeight; y++) {
                int bOffset = eltOffset*8 + bitOffset;
                for(int x = 0; x < rectWidth; x++) {
                    if(bdata[k++] != (byte)0) {
                        data[bOffset/8] |=
                            (byte)(0x00000001 << (7 - bOffset & 7));
                    }
                    bOffset++;
                }
                eltOffset += lineStride;
            }
        } else if(dataBuffer instanceof DataBufferShort ||
                  dataBuffer instanceof DataBufferUShort) {
            short[] data = dataBuffer instanceof DataBufferShort ?
                ((DataBufferShort)dataBuffer).getData() :
                ((DataBufferUShort)dataBuffer).getData();
            for(int y = 0; y < rectHeight; y++) {
                int bOffset = eltOffset*16 + bitOffset;
                for(int x = 0; x < rectWidth; x++) {
                    if(bdata[k++] != (byte)0) {
                        data[bOffset/16] |=
                            (short)(0x00000001 <<
                                    (15 - bOffset % 16));
                    }
                    bOffset++;
                }
                eltOffset += lineStride;
            }
        } else if(dataBuffer instanceof DataBufferInt) {
            int[] data = ((DataBufferInt)dataBuffer).getData();
            for(int y = 0; y < rectHeight; y++) {
                int bOffset = eltOffset*32 + bitOffset;
                for(int x = 0; x < rectWidth; x++) {
                    if(bdata[k++] != (byte)0) {
                        data[bOffset/32] |=
                            (0x00000001 << (31 - bOffset % 32));
                    }
                    bOffset++;
                }
                eltOffset += lineStride;
            }
        }
    }

    public static boolean isBinary(SampleModel sm) {
        return sm instanceof MultiPixelPackedSampleModel &&
            ((MultiPixelPackedSampleModel)sm).getPixelBitStride() == 1 &&
            sm.getNumBands() == 1;
    }

    public static ColorModel createColorModel(ColorSpace colorSpace,
                                              SampleModel sampleModel) {
        ColorModel colorModel = null;

        if(sampleModel == null) {
            throw new IllegalArgumentException(I18N.getString("ImageUtil1"));
        }

        int numBands = sampleModel.getNumBands();
        if (numBands < 1 || numBands > 4) {
            return null;
        }

        int dataType = sampleModel.getDataType();
        if (sampleModel instanceof ComponentSampleModel) {
            if (dataType < DataBuffer.TYPE_BYTE ||
                //dataType == DataBuffer.TYPE_SHORT ||
                dataType > DataBuffer.TYPE_DOUBLE) {
                return null;
            }

            if (colorSpace == null)
                colorSpace =
                    numBands <= 2 ?
                    ColorSpace.getInstance(ColorSpace.CS_GRAY) :
                    ColorSpace.getInstance(ColorSpace.CS_sRGB);

            boolean useAlpha = (numBands == 2) || (numBands == 4);
            int transparency = useAlpha ?
                               Transparency.TRANSLUCENT : Transparency.OPAQUE;

            boolean premultiplied = false;

            int dataTypeSize = DataBuffer.getDataTypeSize(dataType);
            int[] bits = new int[numBands];
            for (int i = 0; i < numBands; i++) {
                bits[i] = dataTypeSize;
            }

            colorModel = new ComponentColorModel(colorSpace,
                                                 bits,
                                                 useAlpha,
                                                 premultiplied,
                                                 transparency,
                                                 dataType);
        } else if (sampleModel instanceof SinglePixelPackedSampleModel) {
            SinglePixelPackedSampleModel sppsm =
                (SinglePixelPackedSampleModel)sampleModel;

            int[] bitMasks = sppsm.getBitMasks();
            int rmask = 0;
            int gmask = 0;
            int bmask = 0;
            int amask = 0;

            numBands = bitMasks.length;
            if (numBands <= 2) {
                rmask = gmask = bmask = bitMasks[0];
                if (numBands == 2) {
                    amask = bitMasks[1];
                }
            } else {
                rmask = bitMasks[0];
                gmask = bitMasks[1];
                bmask = bitMasks[2];
                if (numBands == 4) {
                    amask = bitMasks[3];
                }
            }

            int[] sampleSize = sppsm.getSampleSize();
            int bits = 0;
            for (int i = 0; i < sampleSize.length; i++) {
                bits += sampleSize[i];
            }

            if (colorSpace == null)
                colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);

            colorModel =
                new DirectColorModel(colorSpace,
                                     bits, rmask, gmask, bmask, amask,
                                     false,
                                     sampleModel.getDataType());
        } else if (sampleModel instanceof MultiPixelPackedSampleModel) {
            int bits =
                ((MultiPixelPackedSampleModel)sampleModel).getPixelBitStride();
            int size = 1 << bits;
            byte[] comp = new byte[size];

            for (int i = 0; i < size; i++)
                comp[i] = (byte)(255 * i / (size - 1));

            colorModel = new IndexColorModel(bits, size, comp, comp, comp);
        }

        return colorModel;
    }

    public static int getElementSize(SampleModel sm) {
        int elementSize = DataBuffer.getDataTypeSize(sm.getDataType());

        if (sm instanceof MultiPixelPackedSampleModel) {
            MultiPixelPackedSampleModel mppsm =
                (MultiPixelPackedSampleModel)sm;
            return mppsm.getSampleSize(0) * mppsm.getNumBands();
        } else if (sm instanceof ComponentSampleModel) {
            return sm.getNumBands() * elementSize;
        } else if (sm instanceof SinglePixelPackedSampleModel) {
            return elementSize;
        }

        return elementSize * sm.getNumBands();

    }

    public static long getTileSize(SampleModel sm) {
        int elementSize = DataBuffer.getDataTypeSize(sm.getDataType());

        if (sm instanceof MultiPixelPackedSampleModel) {
            MultiPixelPackedSampleModel mppsm =
                (MultiPixelPackedSampleModel)sm;
            return (mppsm.getScanlineStride() * mppsm.getHeight() +
                   (mppsm.getDataBitOffset() + elementSize -1) / elementSize) *
                   ((elementSize + 7) / 8);
        } else if (sm instanceof ComponentSampleModel) {
            ComponentSampleModel csm = (ComponentSampleModel)sm;
            int[] bandOffsets = csm.getBandOffsets();
            int maxBandOff = bandOffsets[0];
            for (int i=1; i<bandOffsets.length; i++)
                maxBandOff = Math.max(maxBandOff, bandOffsets[i]);

            long size = 0;
            int pixelStride = csm.getPixelStride();
            int scanlineStride = csm.getScanlineStride();
            if (maxBandOff >= 0)
                size += maxBandOff + 1;
            if (pixelStride > 0)
                size += pixelStride * (sm.getWidth() - 1);
            if (scanlineStride > 0)
                size += scanlineStride * (sm.getHeight() - 1);

            int[] bankIndices = csm.getBankIndices();
            maxBandOff = bankIndices[0];
            for (int i=1; i<bankIndices.length; i++)
                maxBandOff = Math.max(maxBandOff, bankIndices[i]);
            return size * (maxBandOff + 1) * ((elementSize + 7) / 8);
        } else if (sm instanceof SinglePixelPackedSampleModel) {
            SinglePixelPackedSampleModel sppsm =
                (SinglePixelPackedSampleModel)sm;
            long size = sppsm.getScanlineStride() * (sppsm.getHeight() - 1) +
                        sppsm.getWidth();
            return size * ((elementSize + 7) / 8);
        }

        return 0;
    }

    public static long getBandSize(SampleModel sm) {
        int elementSize = DataBuffer.getDataTypeSize(sm.getDataType());

        if (sm instanceof ComponentSampleModel) {
            ComponentSampleModel csm = (ComponentSampleModel)sm;
            int pixelStride = csm.getPixelStride();
            int scanlineStride = csm.getScanlineStride();
            long size = Math.min(pixelStride, scanlineStride);

            if (pixelStride > 0)
                size += pixelStride * (sm.getWidth() - 1);
            if (scanlineStride > 0)
                size += scanlineStride * (sm.getHeight() - 1);
            return size * ((elementSize + 7) / 8);
        } else
            return getTileSize(sm);
    }
    /**
     * Tests whether the color indices represent a gray-scale image.
     *
     * @param r The red channel color indices.
     * @param g The green channel color indices.
     * @param b The blue channel color indices.
     * @return If all the indices have 256 entries, and are identical mappings,
     *         return {@code true}; otherwise, return {@code false}.
     */
    public static boolean isIndicesForGrayscale(byte[] r, byte[] g, byte[] b) {
        if (r.length != g.length || r.length != b.length)
            return false;

        int size = r.length;

        if (size != 256)
            return false;

        for (int i = 0; i < size; i++) {
            byte temp = (byte) i;

            if (r[i] != temp || g[i] != temp || b[i] != temp)
                return false;
        }

        return true;
    }

    /** Converts the provided object to {@code String} */
    public static String convertObjectToString(Object obj) {
        if (obj == null)
            return "";

        String s = "";
        if (obj instanceof byte[]) {
            byte[] bArray = (byte[])obj;
            for (int i = 0; i < bArray.length; i++)
                s += bArray[i] + " ";
            return s;
        }

        if (obj instanceof int[]) {
            int[] iArray = (int[])obj;
            for (int i = 0; i < iArray.length; i++)
                s += iArray[i] + " " ;
            return s;
        }

        if (obj instanceof short[]) {
            short[] sArray = (short[])obj;
            for (int i = 0; i < sArray.length; i++)
                s += sArray[i] + " " ;
            return s;
        }

        return obj.toString();

    }

    /** Checks that the provided {@code ImageWriter} can encode
     * the provided {@code ImageTypeSpecifier} or not.  If not, an
     * {@code IIOException} will be thrown.
     * @param writer The provided {@code ImageWriter}.
     * @param type The image to be tested.
     * @throws IIOException If the writer cannot encoded the provided image.
     */
    public static final void canEncodeImage(ImageWriter writer,
                                            ImageTypeSpecifier type)
        throws IIOException {
        ImageWriterSpi spi = writer.getOriginatingProvider();

        if(type != null && spi != null && !spi.canEncodeImage(type))  {
            throw new IIOException(I18N.getString("ImageUtil2")+" "+
                                   writer.getClass().getName());
        }
    }

    /** Checks that the provided {@code ImageWriter} can encode
     * the provided {@code ColorModel} and {@code SampleModel}.
     * If not, an {@code IIOException} will be thrown.
     * @param writer The provided {@code ImageWriter}.
     * @param colorModel The provided {@code ColorModel}.
     * @param sampleModel The provided {@code SampleModel}.
     * @throws IIOException If the writer cannot encoded the provided image.
     */
    public static final void canEncodeImage(ImageWriter writer,
                                            ColorModel colorModel,
                                            SampleModel sampleModel)
        throws IIOException {
        ImageTypeSpecifier type = null;
        if (colorModel != null && sampleModel != null)
            type = new ImageTypeSpecifier(colorModel, sampleModel);
        canEncodeImage(writer, type);
    }

    /**
     * Returns whether the image has contiguous data across rows.
     */
    public static final boolean imageIsContiguous(RenderedImage image) {
        SampleModel sm;
        if(image instanceof BufferedImage) {
            WritableRaster ras = ((BufferedImage)image).getRaster();
            sm = ras.getSampleModel();
        } else {
            sm = image.getSampleModel();
        }

        if (sm instanceof ComponentSampleModel) {
            // Ensure image rows samples are stored contiguously
            // in a single bank.
            ComponentSampleModel csm = (ComponentSampleModel)sm;

            if (csm.getPixelStride() != csm.getNumBands()) {
                return false;
            }

            int[] bandOffsets = csm.getBandOffsets();
            for (int i = 0; i < bandOffsets.length; i++) {
                if (bandOffsets[i] != i) {
                    return false;
                }
            }

            int[] bankIndices = csm.getBankIndices();
            for (int i = 0; i < bandOffsets.length; i++) {
                if (bankIndices[i] != 0) {
                    return false;
                }
            }

            return true;
        }

        // Otherwise true if and only if it's a bilevel image with
        // a MultiPixelPackedSampleModel, 1 bit per pixel, and 1 bit
        // pixel stride.
        return ImageUtil.isBinary(sm);
    }

    /**
     * Gets the destination image type.
     */
    public static final ImageTypeSpecifier
        getDestinationType(ImageReadParam param,
                           Iterator<ImageTypeSpecifier> imageTypes) throws IIOException {

        if (imageTypes == null || !imageTypes.hasNext()) {
            throw new IllegalArgumentException("imageTypes null or empty!");
        }

        ImageTypeSpecifier imageType = null;

        // If param is non-null, use it
        if (param != null) {
            imageType = param.getDestinationType();
        }

        // No info from param, use fallback image type
        if (imageType == null) {
            Object o = imageTypes.next();
            if (!(o instanceof ImageTypeSpecifier)) {
                throw new IllegalArgumentException
                    ("Non-ImageTypeSpecifier retrieved from imageTypes!");
            }
            imageType = (ImageTypeSpecifier)o;
        } else {
            boolean foundIt = false;
            while (imageTypes.hasNext()) {
                ImageTypeSpecifier type =
                    imageTypes.next();
                if (type.equals(imageType)) {
                    foundIt = true;
                    break;
                }
            }

            if (!foundIt) {
                throw new IIOException
                    ("Destination type from ImageReadParam does not match!");
            }
        }

        return imageType;
    }

    /**
     * Returns {@code true} if the given {@code ColorSpace} object is an
     * instance of {@code ICC_ColorSpace} but is not one of the standard
     * {@code ColorSpace}s returned by {@code ColorSpace.getInstance()}.
     *
     * @param  cs the {@code ColorSpace} to test
     */
    public static boolean isNonStandardICCColorSpace(ColorSpace cs) {
        return cs instanceof ICC_ColorSpace && !cs.isCS_sRGB()
                && !cs.equals(ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB))
                && !cs.equals(ColorSpace.getInstance(ColorSpace.CS_GRAY))
                && !cs.equals(ColorSpace.getInstance(ColorSpace.CS_CIEXYZ))
                && !cs.equals(ColorSpace.getInstance(ColorSpace.CS_PYCC));
    }
}
