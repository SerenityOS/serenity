/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.imageio.plugins.tiff;

import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import javax.imageio.IIOException;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import com.sun.imageio.plugins.common.ImageUtil;
import com.sun.imageio.plugins.common.BogusColorSpace;
import com.sun.imageio.plugins.common.SimpleCMYKColorSpace;

/**
 * A class defining a pluggable TIFF decompressor.
 *
 * <p> The mapping between source and destination Y coordinates is
 * given by the equations:
 *
 * <pre>
 * dx = (sx - sourceXOffset)/subsampleX + dstXOffset;
 * dy = (sy - sourceYOffset)/subsampleY + dstYOffset;
 * </pre>
 *
 * Note that the mapping from source coordinates to destination
 * coordinates is not one-to-one if subsampling is being used, since
 * only certain source pixels are to be copied to the
 * destination. However, * the inverse mapping is always one-to-one:
 *
 * <pre>
 * sx = (dx - dstXOffset)*subsampleX + sourceXOffset;
 * sy = (dy - dstYOffset)*subsampleY + sourceYOffset;
 * </pre>
 *
 * <p> Decompressors may be written with various levels of complexity.
 * The most complex decompressors will override the
 * {@code decode} method, and will perform all the work of
 * decoding, subsampling, offsetting, clipping, and format conversion.
 * This approach may be the most efficient, since it is possible to
 * avoid the use of extra image buffers, and it may be possible to
 * avoid decoding portions of the image that will not be copied into
 * the destination.
 *
 * <p> Less ambitious decompressors may override the
 * {@code decodeRaw} method, which is responsible for
 * decompressing the entire tile or strip into a byte array (or other
 * appropriate datatype).  The default implementation of
 * {@code decode} will perform all necessary setup of buffers,
 * call {@code decodeRaw} to perform the actual decoding, perform
 * subsampling, and copy the results into the final destination image.
 * Where possible, it will pass the real image buffer to
 * {@code decodeRaw} in order to avoid making an extra copy.
 *
 * <p> Slightly more ambitious decompressors may override
 * {@code decodeRaw}, but avoid writing pixels that will be
 * discarded in the subsampling phase.
 */
public abstract class TIFFDecompressor {

    /**
     * The {@code ImageReader} calling this
     * {@code TIFFDecompressor}.
     */
    protected ImageReader reader;

    /**
     * The {@code IIOMetadata} object containing metadata for the
     * current image.
     */
    protected IIOMetadata metadata;

    /**
     * The value of the {@code PhotometricInterpretation} tag.
     * Legal values are {@link
     * BaselineTIFFTagSet#PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO },
     * {@link
     * BaselineTIFFTagSet#PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO},
     * {@link BaselineTIFFTagSet#PHOTOMETRIC_INTERPRETATION_RGB},
     * {@link
     * BaselineTIFFTagSet#PHOTOMETRIC_INTERPRETATION_PALETTE_COLOR},
     * {@link
     * BaselineTIFFTagSet#PHOTOMETRIC_INTERPRETATION_TRANSPARENCY_MASK},
     * {@link BaselineTIFFTagSet#PHOTOMETRIC_INTERPRETATION_Y_CB_CR},
     * {@link BaselineTIFFTagSet#PHOTOMETRIC_INTERPRETATION_CIELAB},
     * {@link BaselineTIFFTagSet#PHOTOMETRIC_INTERPRETATION_ICCLAB},
     * or other value defined by a TIFF extension.
     */
    protected int photometricInterpretation;

    /**
     * The value of the {@code Compression} tag. Legal values are
     * {@link BaselineTIFFTagSet#COMPRESSION_NONE}, {@link
     * BaselineTIFFTagSet#COMPRESSION_CCITT_RLE}, {@link
     * BaselineTIFFTagSet#COMPRESSION_CCITT_T_4}, {@link
     * BaselineTIFFTagSet#COMPRESSION_CCITT_T_6}, {@link
     * BaselineTIFFTagSet#COMPRESSION_LZW}, {@link
     * BaselineTIFFTagSet#COMPRESSION_OLD_JPEG}, {@link
     * BaselineTIFFTagSet#COMPRESSION_JPEG}, {@link
     * BaselineTIFFTagSet#COMPRESSION_ZLIB}, {@link
     * BaselineTIFFTagSet#COMPRESSION_PACKBITS}, {@link
     * BaselineTIFFTagSet#COMPRESSION_DEFLATE}, or other value
     * defined by a TIFF extension.
     */
    protected int compression;

    /**
     * {@code true} if the image is encoded using separate planes.
     */
    protected boolean planar;

    /**
     * The planar band to decode; ignored for chunky (interleaved) images.
     */
    protected int planarBand = 0;

    /**
     * The value of the {@code SamplesPerPixel} tag.
     */
    protected int samplesPerPixel;

    /**
     * The value of the {@code BitsPerSample} tag.
     *
     */
    protected int[] bitsPerSample;

    /**
     * The value of the {@code SampleFormat} tag.  Legal values
     * are {@link BaselineTIFFTagSet#SAMPLE_FORMAT_UNSIGNED_INTEGER},
     * {@link BaselineTIFFTagSet#SAMPLE_FORMAT_SIGNED_INTEGER}, {@link
     * BaselineTIFFTagSet#SAMPLE_FORMAT_FLOATING_POINT}, {@link
     * BaselineTIFFTagSet#SAMPLE_FORMAT_UNDEFINED}, or other value
     * defined by a TIFF extension.
     */
    protected int[] sampleFormat =
        new int[] {BaselineTIFFTagSet.SAMPLE_FORMAT_UNSIGNED_INTEGER};

    /**
     * The value of the {@code ExtraSamples} tag.  Legal values
     * are {@link BaselineTIFFTagSet#EXTRA_SAMPLES_UNSPECIFIED},
     * {@link BaselineTIFFTagSet#EXTRA_SAMPLES_ASSOCIATED_ALPHA},
     * {@link BaselineTIFFTagSet#EXTRA_SAMPLES_UNASSOCIATED_ALPHA},
     * or other value defined by a TIFF extension.
     */
    protected int[] extraSamples;

    /**
     * The value of the {@code ColorMap} tag.
     *
     */
    protected char[] colorMap;

    // Region of input stream containing the data

    /**
     * The {@code ImageInputStream} containing the TIFF source
     * data.
     */
    protected ImageInputStream stream;

    /**
     * The offset in the source {@code ImageInputStream} of the
     * start of the data to be decompressed.
     */
    protected long offset;

    /**
     * The number of bytes of data from the source
     * {@code ImageInputStream} to be decompressed.
     */
    protected int byteCount;

    // Region of the file image represented in the stream
    // This is unaffected by subsampling

    /**
     * The X coordinate of the upper-left pixel of the source region
     * being decoded from the source stream.  This value is not affected
     * by source subsampling.
     */
    protected int srcMinX;

    /**
     * The Y coordinate of the upper-left pixel of the source region
     * being decoded from the source stream.  This value is not affected
     * by source subsampling.
     */
    protected int srcMinY;

    /**
     * The width of the source region being decoded from the source
     * stream.  This value is not affected by source subsampling.
     */
    protected int srcWidth;

    /**
     * The height of the source region being decoded from the source
     * stream.  This value is not affected by source subsampling.
     */
    protected int srcHeight;

    // Subsampling to be performed

    /**
     * The source X offset used, along with {@code dstXOffset}
     * and {@code subsampleX}, to map between horizontal source
     * and destination pixel coordinates.
     */
    protected int sourceXOffset;

    /**
     * The horizontal destination offset used, along with
     * {@code sourceXOffset} and {@code subsampleX}, to map
     * between horizontal source and destination pixel coordinates.
     * See the comment for {@link #sourceXOffset sourceXOffset} for
     * the mapping equations.
     */
    protected int dstXOffset;

    /**
     * The source Y offset used, along with {@code dstYOffset}
     * and {@code subsampleY}, to map between vertical source and
     * destination pixel coordinates.
     */
    protected int sourceYOffset;

    /**
     * The vertical destination offset used, along with
     * {@code sourceYOffset} and {@code subsampleY}, to map
     * between horizontal source and destination pixel coordinates.
     * See the comment for {@link #sourceYOffset sourceYOffset} for
     * the mapping equations.
     */
    protected int dstYOffset;

    /**
     * The horizontal subsampling factor.  A factor of 1 means that
     * every column is copied to the destination; a factor of 2 means
     * that every second column is copied, etc.
     */
    protected int subsampleX;

    /**
     * The vertical subsampling factor.  A factor of 1 means that
     * every row is copied to the destination; a factor of 2 means
     * that every second row is copied, etc.
     */
    protected int subsampleY;

    // Band subsetting/rearrangement

    /**
     * The sequence of source bands that are to be copied into the
     * destination.
     */
    protected int[] sourceBands;

    /**
     * The sequence of destination bands to receive the source data.
     */
    protected int[] destinationBands;

    // Destination for decodeRaw

    /**
     * A {@code BufferedImage} for the {@code decodeRaw}
     * method to write into.
     */
    protected BufferedImage rawImage;

    // Destination

    /**
     * The final destination image.
     */
    protected BufferedImage image;

    /**
     * The X coordinate of the upper left pixel to be written in the
     * destination image.
     */
    protected int dstMinX;

    /**
     * The Y coordinate of the upper left pixel to be written in the
     * destination image.
     */
    protected int dstMinY;

    /**
     * The width of the region of the destination image to be written.
     */
    protected int dstWidth;

    /**
     * The height of the region of the destination image to be written.
     */
    protected int dstHeight;

    // Region of source contributing to the destination

    /**
     * The X coordinate of the upper-left source pixel that will
     * actually be copied into the destination image, taking into
     * account all subsampling, offsetting, and clipping.  That is,
     * the pixel at ({@code activeSrcMinX},
     * {@code activeSrcMinY}) is to be copied into the
     * destination pixel at ({@code dstMinX},
     * {@code dstMinY}).
     *
     * <p> The pixels in the source region to be copied are
     * those with X coordinates of the form {@code activeSrcMinX +
     * k*subsampleX}, where {@code k} is an integer such
     * that {@code 0 <= k < dstWidth}.
     */
    protected int activeSrcMinX;

    /**
     * The Y coordinate of the upper-left source pixel that will
     * actually be copied into the destination image, taking into account
     * all subsampling, offsetting, and clipping.
     *
     * <p> The pixels in the source region to be copied are
     * those with Y coordinates of the form {@code activeSrcMinY +
     * k*subsampleY}, where {@code k} is an integer such
     * that {@code 0 <= k < dstHeight}.
     */
    protected int activeSrcMinY;

    /**
     * The width of the source region that will actually be copied
     * into the destination image, taking into account all
     * susbampling, offsetting, and clipping.
     *
     * <p> The active source width will always be equal to
     * {@code (dstWidth - 1)*subsampleX + 1}.
     */
    protected int activeSrcWidth;

    /**
     * The height of the source region that will actually be copied
     * into the destination image, taking into account all
     * susbampling, offsetting, and clipping.
     *
     * <p> The active source height will always be equal to
     * {@code (dstHeight - 1)*subsampleY + 1}.
     */
    protected int activeSrcHeight;

    /**
     * A {@code TIFFColorConverter} object describing the color space of
     * the encoded pixel data, or {@code null}.
     */
    protected TIFFColorConverter colorConverter;

    private boolean isBilevel;
    private boolean isContiguous;
    private boolean isImageSimple;
    private boolean adjustBitDepths;
    private int[][] bitDepthScale;

    // source pixel at (sx, sy) should map to dst pixel (dx, dy), where:
    //
    // dx = (sx - sourceXOffset)/subsampleX + dstXOffset;
    // dy = (sy - sourceYOffset)/subsampleY + dstYOffset;
    //
    // Note that this mapping is many-to-one.  Source pixels such that
    // (sx - sourceXOffset) % subsampleX != 0 should not be copied
    // (and similarly for y).
    //
    // The backwards mapping from dest to source is one-to-one:
    //
    // sx = (dx - dstXOffset)*subsampleX + sourceXOffset;
    // sy = (dy - dstYOffset)*subsampleY + sourceYOffset;
    //
    // The reader will always hand us the full source region as it
    // exists in the file.  It will take care of clipping the dest region
    // to exactly those dest pixels that are present in the source region.

    /**
     * Create a {@code PixelInterleavedSampleModel} for use in creating
     * an {@code ImageTypeSpecifier}.  Its dimensions will be 1x1 and
     * it will have ascending band offsets as {0, 1, 2, ..., numBands}.
     *
     * @param dataType The data type (DataBuffer.TYPE_*).
     * @param numBands The number of bands.
     * @return A {@code PixelInterleavedSampleModel}.
     */
    static SampleModel createInterleavedSM(int dataType,
                                           int numBands) {
        int[] bandOffsets = new int[numBands];
        for(int i = 0; i < numBands; i++) {
            bandOffsets[i] = i;
        }
        return new PixelInterleavedSampleModel(dataType,
                                               1, // width
                                               1, // height
                                               numBands, // pixelStride,
                                               numBands, // scanlineStride
                                               bandOffsets);
    }

    /**
     * Create a {@code ComponentColorModel} for use in creating
     * an {@code ImageTypeSpecifier}.
     */
    // This code was inspired by the method of the same name in
    // javax.imageio.ImageTypeSpecifier
    static ColorModel createComponentCM(ColorSpace colorSpace,
                                        int numBands,
                                        int[] bitsPerSample,
                                        int dataType,
                                        boolean hasAlpha,
                                        boolean isAlphaPremultiplied) {
        int transparency =
            hasAlpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE;

        return new ComponentColorModel(colorSpace,
                                       bitsPerSample,
                                       hasAlpha,
                                       isAlphaPremultiplied,
                                       transparency,
                                       dataType);
    }

    private static int createMask(int[] bitsPerSample, int band) {
        int mask = (1 << bitsPerSample[band]) - 1;
        for (int i = band + 1; i < bitsPerSample.length; i++) {
            mask <<= bitsPerSample[i];
        }

        return mask;
    }

    private static int getDataTypeFromNumBits(int numBits, boolean isSigned) {
        int dataType;

        if (numBits <= 8) {
            dataType = DataBuffer.TYPE_BYTE;
        } else if (numBits <= 16) {
            dataType = isSigned ?
                DataBuffer.TYPE_SHORT : DataBuffer.TYPE_USHORT;
        } else {
            dataType = DataBuffer.TYPE_INT;
        }

        return dataType;
    }

    private static boolean areIntArraysEqual(int[] a, int[] b) {
        if(a == null || b == null) {
            if(a == null && b == null) {
                return true;
            } else { // one is null and one is not
                return false;
            }
        }

        if(a.length != b.length) {
            return false;
        }

        int length = a.length;
        for(int i = 0; i < length; i++) {
            if(a[i] != b[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Return the number of bits occupied by {@code dataType}
     * which must be one of the {@code DataBuffer} {@code TYPE}s.
     */
    private static int getDataTypeSize(int dataType) throws IIOException {
        int dataTypeSize = 0;
        switch(dataType) {
        case DataBuffer.TYPE_BYTE:
            dataTypeSize = 8;
            break;
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
            dataTypeSize = 16;
            break;
        case DataBuffer.TYPE_INT:
        case DataBuffer.TYPE_FLOAT:
            dataTypeSize = 32;
            break;
        case DataBuffer.TYPE_DOUBLE:
            dataTypeSize = 64;
            break;
        default:
            throw new IIOException("Unknown data type "+dataType);
        }

        return dataTypeSize;
    }

    /**
     * Returns the number of bits per pixel.
     */
    private static int getBitsPerPixel(SampleModel sm) {
        int bitsPerPixel = 0;
        int[] sampleSize = sm.getSampleSize();
        int numBands = sampleSize.length;
        for(int i = 0; i < numBands; i++) {
            bitsPerPixel += sampleSize[i];
        }
        return bitsPerPixel;
    }

    /**
     * Returns whether all samples have the same number of bits.
     */
    private static boolean areSampleSizesEqual(SampleModel sm) {
        boolean allSameSize = true;
        int[] sampleSize = sm.getSampleSize();
        int sampleSize0 = sampleSize[0];
        int numBands = sampleSize.length;

        for(int i = 1; i < numBands; i++) {
            if(sampleSize[i] != sampleSize0) {
                allSameSize = false;
                break;
            }
        }

        return allSameSize;
    }

    /**
     * Determines whether the {@code DataBuffer} is filled without
     * any interspersed padding bits.
     */
    private static boolean isDataBufferBitContiguous(SampleModel sm,
        int[] bitsPerSample)
        throws IIOException {
        int dataTypeSize = getDataTypeSize(sm.getDataType());

        if(sm instanceof ComponentSampleModel) {
            int numBands = sm.getNumBands();
            for(int i = 0; i < numBands; i++) {
                if(bitsPerSample[i] != dataTypeSize) {
                    // Sample does not fill data element.
                    return false;
                }
            }
        } else if(sm instanceof MultiPixelPackedSampleModel) {
            MultiPixelPackedSampleModel mppsm =
                (MultiPixelPackedSampleModel)sm;
            if(dataTypeSize % mppsm.getPixelBitStride() != 0) {
                // Pixels do not fill the data element.
                return false;
            }
        } else if(sm instanceof SinglePixelPackedSampleModel) {
            SinglePixelPackedSampleModel sppsm =
                (SinglePixelPackedSampleModel)sm;
            int numBands = sm.getNumBands();
            int numBits = 0;
            for(int i = 0; i < numBands; i++) {
                numBits += sm.getSampleSize(i);
            }
            if(numBits != dataTypeSize) {
                // Pixel does not fill the data element.
                return false;
            }
        } else {
            // Unknown SampleModel class.
            return false;
        }

        return true;
    }

    /**
     * Reformats data read as bytes into a short or int buffer.
     */
    private static void reformatData(byte[] buf,
                                     int bytesPerRow,
                                     int numRows,
                                     short[] shortData,
                                     int[] intData,
                                     int outOffset,
                                     int outStride)
        throws IIOException {

        if(shortData != null) {
            int inOffset = 0;
            int shortsPerRow = bytesPerRow/2;
            int numExtraBytes = bytesPerRow % 2;
            for(int j = 0; j < numRows; j++) {
                int k = outOffset;
                for(int i = 0; i < shortsPerRow; i++) {
                    shortData[k++] =
                        (short)(((buf[inOffset++]&0xff) << 8) |
                                (buf[inOffset++]&0xff));
                }
                if(numExtraBytes != 0) {
                    shortData[k++] = (short)((buf[inOffset++]&0xff) << 8);
                }
                outOffset += outStride;
            }
        } else if(intData != null) {
            int inOffset = 0;
            int intsPerRow = bytesPerRow/4;
            int numExtraBytes = bytesPerRow % 4;
            for(int j = 0; j < numRows; j++) {
                int k = outOffset;
                for(int i = 0; i < intsPerRow; i++) {
                    intData[k++] =
                        ((buf[inOffset++]&0xff) << 24) |
                        ((buf[inOffset++]&0xff) << 16) |
                        ((buf[inOffset++]&0xff) << 8) |
                        (buf[inOffset++]&0xff);
                }
                if(numExtraBytes != 0) {
                    int shift = 24;
                    int ival = 0;
                    for(int b = 0; b < numExtraBytes; b++) {
                        ival |= (buf[inOffset++]&0xff) << shift;
                        shift -= 8;
                    }
                    intData[k++] = ival;
                }
                outOffset += outStride;
            }
        } else {
            throw new IIOException("shortData == null && intData == null!");
        }
    }

    /**
     * Reformats bit-discontiguous data into the {@code DataBuffer}
     * of the supplied {@code WritableRaster}.
     */
    private static void reformatDiscontiguousData(byte[] buf,
                                                  int[] bitsPerSample,
                                                  int stride,
                                                  int w,
                                                  int h,
                                                  WritableRaster raster)
        throws IOException {

        // Get SampleModel info.
        SampleModel sm = raster.getSampleModel();
        int numBands = sm.getNumBands();

        // Initialize input stream.
        ByteArrayInputStream is = new ByteArrayInputStream(buf);
        ImageInputStream iis = new MemoryCacheImageInputStream(is);

        // Reformat.
        long iisPosition = 0L;
        int y = raster.getMinY();
        for(int j = 0; j < h; j++, y++) {
            iis.seek(iisPosition);
            int x = raster.getMinX();
            for(int i = 0; i < w; i++, x++) {
                for(int b = 0; b < numBands; b++) {
                    long bits = iis.readBits(bitsPerSample[b]);
                    raster.setSample(x, y, b, (int)bits);
                }
            }
            iisPosition += stride;
        }
    }

    /**
     * A utility method that returns an
     * {@code ImageTypeSpecifier} suitable for decoding an image
     * with the given parameters.
     *
     * @param photometricInterpretation the value of the
     * {@code PhotometricInterpretation} field.
     * @param compression the value of the {@code Compression} field.
     * @param samplesPerPixel the value of the
     * {@code SamplesPerPixel} field.
     * @param bitsPerSample the value of the {@code BitsPerSample} field.
     * @param sampleFormat the value of the {@code SampleFormat} field.
     * @param extraSamples the value of the {@code ExtraSamples} field.
     * @param colorMap the value of the {@code ColorMap} field.
     *
     * @return a suitable {@code ImageTypeSpecifier}, or
     * {@code null} if it is not possible to create one.
     */
    public static ImageTypeSpecifier
        getRawImageTypeSpecifier(int photometricInterpretation,
                                 int compression,
                                 int samplesPerPixel,
                                 int[] bitsPerSample,
                                 int[] sampleFormat,
                                 int[] extraSamples,
                                 char[] colorMap) {

        //
        // Types to support:
        //
        // 1, 2, 4, 8, or 16 bit grayscale or indexed
        // 8,8-bit gray+alpha
        // 16,16-bit gray+alpha
        // 8,8,8-bit RGB
        // 8,8,8,8-bit RGB+alpha
        // 16,16,16-bit RGB
        // 16,16,16,16-bit RGB+alpha
        // R+G+B = 8-bit RGB
        // R+G+B+A = 8-bit RGB
        // R+G+B = 16-bit RGB
        // R+G+B+A = 16-bit RGB
        // 8X-bits/sample, arbitrary numBands.
        // Arbitrary non-indexed, non-float layouts (discontiguous).
        //

        // Band-sequential

        // 1, 2, 4, 8, or 16 bit grayscale or indexed images
        if (samplesPerPixel == 1 &&
            (bitsPerSample[0] == 1 ||
             bitsPerSample[0] == 2 ||
             bitsPerSample[0] == 4 ||
             bitsPerSample[0] == 8 ||
             bitsPerSample[0] == 16)) {

            // 2 and 16 bits images are not in the baseline
            // specification, but we will allow them anyway
            // since they fit well into Java2D
            //
            // this raises the issue of how to write such images...

            if (colorMap == null) {
                // Grayscale
                boolean isSigned = (sampleFormat[0] ==
                              BaselineTIFFTagSet.SAMPLE_FORMAT_SIGNED_INTEGER);
                int dataType;
                if (bitsPerSample[0] <= 8) {
                    dataType = DataBuffer.TYPE_BYTE;
                } else {
                    dataType = sampleFormat[0] ==
                        BaselineTIFFTagSet.SAMPLE_FORMAT_SIGNED_INTEGER ?
                        DataBuffer.TYPE_SHORT :
                        DataBuffer.TYPE_USHORT;
                }

                return ImageTypeSpecifier.createGrayscale(bitsPerSample[0],
                                                          dataType,
                                                          isSigned);
            } else {
                // Indexed
                int mapSize = 1 << bitsPerSample[0];
                byte[] redLut = new byte[mapSize];
                byte[] greenLut = new byte[mapSize];
                byte[] blueLut = new byte[mapSize];
                byte[] alphaLut = null;

                int idx = 0;
                for (int i = 0; i < mapSize; i++) {
                    redLut[i] = (byte)((colorMap[i]*255)/65535);
                    greenLut[i] = (byte)((colorMap[mapSize + i]*255)/65535);
                    blueLut[i] = (byte)((colorMap[2*mapSize + i]*255)/65535);
                }

                int dataType;
                if (bitsPerSample[0] <= 8) {
                    dataType = DataBuffer.TYPE_BYTE;
                } else if (sampleFormat[0] ==
                    BaselineTIFFTagSet.SAMPLE_FORMAT_SIGNED_INTEGER) {
                    dataType = DataBuffer.TYPE_SHORT;
                } else {
                    dataType = DataBuffer.TYPE_USHORT;
                }
                return ImageTypeSpecifier.createIndexed(redLut,
                                                        greenLut,
                                                        blueLut,
                                                        alphaLut,
                                                        bitsPerSample[0],
                                                        dataType);
            }
        }

        // 8-bit gray-alpha
        if (samplesPerPixel == 2 &&
            bitsPerSample[0] == 8 &&
            bitsPerSample[1] == 8) {
            int dataType = DataBuffer.TYPE_BYTE;
            boolean alphaPremultiplied = false;
            if (extraSamples != null &&
                extraSamples[0] ==
                BaselineTIFFTagSet.EXTRA_SAMPLES_ASSOCIATED_ALPHA) {
                alphaPremultiplied = true;
            }
            return ImageTypeSpecifier.createGrayscale(8,
                                                      dataType,
                                                      false,
                                                      alphaPremultiplied);
        }

        // 16-bit gray-alpha
        if (samplesPerPixel == 2 &&
            bitsPerSample[0] == 16 &&
            bitsPerSample[1] == 16) {
            int dataType = sampleFormat[0] ==
                BaselineTIFFTagSet.SAMPLE_FORMAT_SIGNED_INTEGER ?
                DataBuffer.TYPE_SHORT :
                DataBuffer.TYPE_USHORT;
            boolean alphaPremultiplied = false;
            if (extraSamples != null &&
                extraSamples[0] ==
                BaselineTIFFTagSet.EXTRA_SAMPLES_ASSOCIATED_ALPHA) {
                alphaPremultiplied = true;
            }
            boolean isSigned = dataType == DataBuffer.TYPE_SHORT;
            return ImageTypeSpecifier.createGrayscale(16,
                                                      dataType,
                                                      isSigned,
                                                      alphaPremultiplied);
        }

        ColorSpace rgb = ColorSpace.getInstance(ColorSpace.CS_sRGB);

        // 8-bit RGB
        if (samplesPerPixel == 3 &&
            bitsPerSample[0] == 8 &&
            bitsPerSample[1] == 8 &&
            bitsPerSample[2] == 8) {
            int[] bandOffsets = new int[3];
            bandOffsets[0] = 0;
            bandOffsets[1] = 1;
            bandOffsets[2] = 2;
            int dataType = DataBuffer.TYPE_BYTE;
            ColorSpace theColorSpace;
            if((photometricInterpretation ==
                BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_Y_CB_CR &&
                compression != BaselineTIFFTagSet.COMPRESSION_JPEG &&
                compression != BaselineTIFFTagSet.COMPRESSION_OLD_JPEG) ||
               photometricInterpretation ==
               BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_CIELAB) {
                theColorSpace =
                    ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
            } else {
                theColorSpace = rgb;
            }
            return ImageTypeSpecifier.createInterleaved(theColorSpace,
                                                        bandOffsets,
                                                        dataType,
                                                        false,
                                                        false);
        }

        // 8-bit RGBA
        if (samplesPerPixel == 4 &&
            bitsPerSample[0] == 8 &&
            bitsPerSample[1] == 8 &&
            bitsPerSample[2] == 8 &&
            bitsPerSample[3] == 8) {
            int[] bandOffsets = new int[4];
            bandOffsets[0] = 0;
            bandOffsets[1] = 1;
            bandOffsets[2] = 2;
            bandOffsets[3] = 3;
            int dataType = DataBuffer.TYPE_BYTE;

            ColorSpace theColorSpace;
            boolean hasAlpha;
            boolean alphaPremultiplied = false;
            if(photometricInterpretation ==
               BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_CMYK) {
                theColorSpace = SimpleCMYKColorSpace.getInstance();
                hasAlpha = false;
            } else {
                theColorSpace = rgb;
                hasAlpha = true;
                if (extraSamples != null &&
                    extraSamples[0] ==
                    BaselineTIFFTagSet.EXTRA_SAMPLES_ASSOCIATED_ALPHA) {
                    alphaPremultiplied = true;
                }
            }

            return ImageTypeSpecifier.createInterleaved(theColorSpace,
                                                        bandOffsets,
                                                        dataType,
                                                        hasAlpha,
                                                        alphaPremultiplied);
        }

        // 16-bit RGB
        if (samplesPerPixel == 3 &&
            bitsPerSample[0] == 16 &&
            bitsPerSample[1] == 16 &&
            bitsPerSample[2] == 16) {
            int[] bandOffsets = new int[3];
            bandOffsets[0] = 0;
            bandOffsets[1] = 1;
            bandOffsets[2] = 2;
            int dataType = sampleFormat[0] ==
                BaselineTIFFTagSet.SAMPLE_FORMAT_SIGNED_INTEGER ?
                DataBuffer.TYPE_SHORT :
                DataBuffer.TYPE_USHORT;
            return ImageTypeSpecifier.createInterleaved(rgb,
                                                        bandOffsets,
                                                        dataType,
                                                        false,
                                                        false);
        }

        // 16-bit RGBA
        if (samplesPerPixel == 4 &&
            bitsPerSample[0] == 16 &&
            bitsPerSample[1] == 16 &&
            bitsPerSample[2] == 16 &&
            bitsPerSample[3] == 16) {
            int[] bandOffsets = new int[4];
            bandOffsets[0] = 0;
            bandOffsets[1] = 1;
            bandOffsets[2] = 2;
            bandOffsets[3] = 3;
            int dataType = sampleFormat[0] ==
                BaselineTIFFTagSet.SAMPLE_FORMAT_SIGNED_INTEGER ?
                DataBuffer.TYPE_SHORT :
                DataBuffer.TYPE_USHORT;

            boolean alphaPremultiplied = false;
            if (extraSamples != null &&
                extraSamples[0] ==
                BaselineTIFFTagSet.EXTRA_SAMPLES_ASSOCIATED_ALPHA) {
                alphaPremultiplied = true;
            }
            return ImageTypeSpecifier.createInterleaved(rgb,
                                                        bandOffsets,
                                                        dataType,
                                                        true,
                                                        alphaPremultiplied);
        }

        // Compute bits per pixel.
        int totalBits = 0;
        for (int i = 0; i < bitsPerSample.length; i++) {
            totalBits += bitsPerSample[i];
        }

        // Packed: 3- or 4-band, 8- or 16-bit.
        if ((samplesPerPixel == 3 || samplesPerPixel == 4) &&
            (totalBits == 8 || totalBits == 16)) {
            int redMask = createMask(bitsPerSample, 0);
            int greenMask = createMask(bitsPerSample, 1);
            int blueMask = createMask(bitsPerSample, 2);
            int alphaMask = (samplesPerPixel == 4) ?
                createMask(bitsPerSample, 3) : 0;
            int transferType = totalBits == 8 ?
                DataBuffer.TYPE_BYTE : DataBuffer.TYPE_USHORT;
            boolean alphaPremultiplied = false;
            if (extraSamples != null &&
                extraSamples[0] ==
                BaselineTIFFTagSet.EXTRA_SAMPLES_ASSOCIATED_ALPHA) {
                alphaPremultiplied = true;
            }
            return ImageTypeSpecifier.createPacked(rgb,
                                                   redMask,
                                                   greenMask,
                                                   blueMask,
                                                   alphaMask,
                                                   transferType,
                                                   alphaPremultiplied);
        }

        // Generic components with 8X bits per sample.
        if(bitsPerSample[0] % 8 == 0) {
            // Check whether all bands have same bit depth.
            boolean allSameBitDepth = true;
            for(int i = 1; i < bitsPerSample.length; i++) {
                if(bitsPerSample[i] != bitsPerSample[i-1]) {
                    allSameBitDepth = false;
                    break;
                }
            }

            // Proceed if all bands have same bit depth.
            if(allSameBitDepth) {
                // Determine the data type.
                int dataType = -1;
                boolean isDataTypeSet = false;
                switch(bitsPerSample[0]) {
                case 8:
                    if(sampleFormat[0] !=
                       BaselineTIFFTagSet.SAMPLE_FORMAT_FLOATING_POINT) {
                        // Ignore whether signed or unsigned:
                        // treat all as unsigned.
                        dataType = DataBuffer.TYPE_BYTE;
                        isDataTypeSet = true;
                    }
                    break;
                case 16:
                    if(sampleFormat[0] !=
                       BaselineTIFFTagSet.SAMPLE_FORMAT_FLOATING_POINT) {
                        if(sampleFormat[0] ==
                           BaselineTIFFTagSet.SAMPLE_FORMAT_SIGNED_INTEGER) {
                            dataType = DataBuffer.TYPE_SHORT;
                        } else {
                            dataType = DataBuffer.TYPE_USHORT;
                        }
                        isDataTypeSet = true;
                    }
                    break;
                case 32:
                    if(sampleFormat[0] ==
                       BaselineTIFFTagSet.SAMPLE_FORMAT_FLOATING_POINT) {
                        dataType = DataBuffer.TYPE_FLOAT;
                    } else {
                        dataType = DataBuffer.TYPE_INT;
                    }
                    isDataTypeSet = true;
                    break;
                case 64:
                    if(sampleFormat[0] ==
                       BaselineTIFFTagSet.SAMPLE_FORMAT_FLOATING_POINT) {
                        dataType = DataBuffer.TYPE_DOUBLE;
                        isDataTypeSet = true;
                    }
                    break;
                }

                if(isDataTypeSet) {
                    // Create the SampleModel.
                    SampleModel sm = createInterleavedSM(dataType,
                                                         samplesPerPixel);

                    // Create the ColorModel.
                    ColorModel cm;
                    if(samplesPerPixel >= 1 && samplesPerPixel <= 4 &&
                       (dataType == DataBuffer.TYPE_INT ||
                        dataType == DataBuffer.TYPE_FLOAT)) {
                        // Handle the 32-bit cases for 1-4 bands.
                        ColorSpace cs = samplesPerPixel <= 2 ?
                            ColorSpace.getInstance(ColorSpace.CS_GRAY) : rgb;
                        boolean hasAlpha = ((samplesPerPixel % 2) == 0);
                        boolean alphaPremultiplied = false;
                        if(hasAlpha && extraSamples != null &&
                           extraSamples[0] ==
                           BaselineTIFFTagSet.EXTRA_SAMPLES_ASSOCIATED_ALPHA) {
                            alphaPremultiplied = true;
                        }

                        cm = createComponentCM(cs,
                                               samplesPerPixel,
                                               bitsPerSample,
                                               dataType,
                                               hasAlpha,
                                               alphaPremultiplied);
                    } else {
                        ColorSpace cs = new BogusColorSpace(samplesPerPixel);
                        cm = createComponentCM(cs,
                                               samplesPerPixel,
                                               bitsPerSample,
                                               dataType,
                                               false, // hasAlpha
                                               false); // alphaPremultiplied
                    }
                    return new ImageTypeSpecifier(cm, sm);
                }
            }
        }

        // Other more bizarre cases including discontiguous DataBuffers
        // such as for the image in bug 4918959.

        if(colorMap == null &&
           sampleFormat[0] !=
           BaselineTIFFTagSet.SAMPLE_FORMAT_FLOATING_POINT) {

            // Determine size of largest sample.
            int maxBitsPerSample = 0;
            for(int i = 0; i < bitsPerSample.length; i++) {
                if(bitsPerSample[i] > maxBitsPerSample) {
                    maxBitsPerSample = bitsPerSample[i];
                }
            }

            // Determine whether data are signed.
            boolean isSigned =
                (sampleFormat[0] ==
                 BaselineTIFFTagSet.SAMPLE_FORMAT_SIGNED_INTEGER);

            // Grayscale
            if(samplesPerPixel == 1 &&
                (bitsPerSample[0] == 1 || bitsPerSample[0] == 2 ||
                 bitsPerSample[0] == 4 || bitsPerSample[0] == 8 ||
                 bitsPerSample[0] == 16)) {
                int dataType = getDataTypeFromNumBits(maxBitsPerSample, isSigned);

                return ImageTypeSpecifier.createGrayscale(bitsPerSample[0],
                                                          dataType,
                                                          isSigned);
            }

            // Gray-alpha
            if (samplesPerPixel == 2 &&
                bitsPerSample[0] == bitsPerSample[1] &&
                (bitsPerSample[0] == 1 || bitsPerSample[0] == 2 ||
                 bitsPerSample[0] == 4 || bitsPerSample[0] == 8 ||
                 bitsPerSample[0] == 16)) {
                boolean alphaPremultiplied = false;
                if (extraSamples != null &&
                    extraSamples[0] ==
                    BaselineTIFFTagSet.EXTRA_SAMPLES_ASSOCIATED_ALPHA) {
                    alphaPremultiplied = true;
                }

                int dataType =
                    getDataTypeFromNumBits(maxBitsPerSample, isSigned);

                return ImageTypeSpecifier.createGrayscale(maxBitsPerSample,
                                                          dataType,
                                                          false,
                                                          alphaPremultiplied);
            }

            if (samplesPerPixel == 3 || samplesPerPixel == 4) {
                int dataType = getDataTypeFromNumBits(maxBitsPerSample, isSigned);
                int dataTypeSize;
                try {
                    dataTypeSize = getDataTypeSize(dataType);
                } catch (IIOException ignored) {
                    dataTypeSize = maxBitsPerSample;
                }
                if(totalBits <= 32 && !isSigned) {
                    // Packed RGB or RGBA
                    int redMask = createMask(bitsPerSample, 0);
                    int greenMask = createMask(bitsPerSample, 1);
                    int blueMask = createMask(bitsPerSample, 2);
                    int alphaMask = (samplesPerPixel == 4) ?
                        createMask(bitsPerSample, 3) : 0;
                    int transferType =
                        getDataTypeFromNumBits(totalBits, false);
                    boolean alphaPremultiplied = false;
                    if (extraSamples != null &&
                        extraSamples[0] ==
                        BaselineTIFFTagSet.EXTRA_SAMPLES_ASSOCIATED_ALPHA) {
                        alphaPremultiplied = true;
                    }
                    return ImageTypeSpecifier.createPacked(rgb,
                                                           redMask,
                                                           greenMask,
                                                           blueMask,
                                                           alphaMask,
                                                           transferType,
                                                           alphaPremultiplied);
                } else if(samplesPerPixel == 3 &&
                    dataTypeSize == bitsPerSample[0] &&
                    bitsPerSample[0] == bitsPerSample[1] &&
                    bitsPerSample[1] == bitsPerSample[2]) {
                    // Interleaved RGB
                    int[] bandOffsets = new int[] {0, 1, 2};
                    return ImageTypeSpecifier.createInterleaved(rgb,
                                                                bandOffsets,
                                                                dataType,
                                                                false,
                                                                false);
                } else if(samplesPerPixel == 4 &&
                    dataTypeSize == bitsPerSample[0] &&
                    bitsPerSample[0] == bitsPerSample[1] &&
                    bitsPerSample[1] == bitsPerSample[2] &&
                    bitsPerSample[2] == bitsPerSample[3]) {
                    // Interleaved RGBA
                    int[] bandOffsets = new int[] {0, 1, 2, 3};
                    boolean alphaPremultiplied = false;
                    if (extraSamples != null &&
                        extraSamples[0] ==
                        BaselineTIFFTagSet.EXTRA_SAMPLES_ASSOCIATED_ALPHA) {
                        alphaPremultiplied = true;
                    }
                    return ImageTypeSpecifier.createInterleaved(rgb,
                                                                bandOffsets,
                                                                dataType,
                                                                true,
                                                                alphaPremultiplied);
                }
            }

            // Arbitrary Interleaved.
            int dataType =
                getDataTypeFromNumBits(maxBitsPerSample, isSigned);
            SampleModel sm = createInterleavedSM(dataType,
                                                 samplesPerPixel);
            ColorSpace cs;
            if (samplesPerPixel <= 2) {
                cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            } else if (samplesPerPixel <= 4) {
                cs = rgb;
            } else {
                cs = new BogusColorSpace(samplesPerPixel);
            }
            ColorModel cm = createComponentCM(cs,
                                              samplesPerPixel,
                                              bitsPerSample,
                                              dataType,
                                              false, // hasAlpha
                                              false); // alphaPremultiplied
            return new ImageTypeSpecifier(cm, sm);
        }

        return null;
    }

    /**
     * Sets the value of the {@code reader} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param reader the current {@code ImageReader}.
     */
    public void setReader(ImageReader reader) {
        this.reader = reader;
    }

    /**
     * Sets the value of the {@code metadata} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param metadata the {@code IIOMetadata} object for the
     * image being read.
     */
    public void setMetadata(IIOMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Sets the value of the {@code photometricInterpretation}
     * field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param photometricInterpretation the photometric interpretation
     * value.
     */
    public void setPhotometricInterpretation(int photometricInterpretation) {
        this.photometricInterpretation = photometricInterpretation;
    }

    /**
     * Sets the value of the {@code compression} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param compression the compression type.
     */
    public void setCompression(int compression) {
        this.compression = compression;
    }

    /**
     * Sets the value of the {@code planar} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param planar {@code true} if the image to be decoded is
     * stored in planar format.
     */
    public void setPlanar(boolean planar) {
        this.planar = planar;
    }

    /**
     * Sets the index of the planar configuration band to be decoded. This value
     * is ignored for chunky (interleaved) images.
     *
     * @param planarBand the index of the planar band to decode
     */
    public void setPlanarBand(int planarBand) { this.planarBand = planarBand; }

    /**
     * Sets the value of the {@code samplesPerPixel} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param samplesPerPixel the number of samples in each source
     * pixel.
     */
    public void setSamplesPerPixel(int samplesPerPixel) {
        this.samplesPerPixel = samplesPerPixel;
    }

    /**
     * Sets the value of the {@code bitsPerSample} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param bitsPerSample the number of bits for each source image
     * sample.
     */
    public void setBitsPerSample(int[] bitsPerSample) {
        this.bitsPerSample = bitsPerSample == null ?
            null : bitsPerSample.clone();
    }

    /**
     * Sets the value of the {@code sampleFormat} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param sampleFormat the format of the source image data,
     * for example unsigned integer or floating-point.
     */
    public void setSampleFormat(int[] sampleFormat) {
        this.sampleFormat = sampleFormat == null ?
            new int[] {BaselineTIFFTagSet.SAMPLE_FORMAT_UNSIGNED_INTEGER} :
            sampleFormat.clone();
    }

    /**
     * Sets the value of the {@code extraSamples} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param extraSamples the interpretation of any samples in the
     * source file beyond those used for basic color or grayscale
     * information.
     */
    public void setExtraSamples(int[] extraSamples) {
        this.extraSamples = extraSamples == null ?
            null : extraSamples.clone();
    }

    /**
     * Sets the value of the {@code colorMap} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param colorMap the color map to apply to the source data,
     * as an array of {@code char}s.
     */
    public void setColorMap(char[] colorMap) {
        this.colorMap = colorMap == null ?
            null : colorMap.clone();
    }

    /**
     * Sets the value of the {@code stream} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param stream the {@code ImageInputStream} to be read.
     */
    public void setStream(ImageInputStream stream) {
        this.stream = stream;
    }

    /**
     * Sets the value of the {@code offset} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param offset the offset of the beginning of the compressed
     * data.
     */
    public void setOffset(long offset) {
        this.offset = offset;
    }

    /**
     * Sets the value of the {@code byteCount} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param byteCount the number of bytes of compressed data.
     */
    public void setByteCount(int byteCount) {
        this.byteCount = byteCount;
    }

    // Region of the file image represented in the stream

    /**
     * Sets the value of the {@code srcMinX} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param srcMinX the minimum X coordinate of the source region
     * being decoded, irrespective of how it will be copied into the
     * destination.
     */
    public void setSrcMinX(int srcMinX) {
        this.srcMinX = srcMinX;
    }

    /**
     * Sets the value of the {@code srcMinY} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param srcMinY the minimum Y coordinate of the source region
     * being decoded, irrespective of how it will be copied into the
     * destination.
     */
    public void setSrcMinY(int srcMinY) {
        this.srcMinY = srcMinY;
    }

    /**
     * Sets the value of the {@code srcWidth} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param srcWidth the width of the source region being decoded,
     * irrespective of how it will be copied into the destination.
     */
    public void setSrcWidth(int srcWidth) {
        this.srcWidth = srcWidth;
    }

    /**
     * Sets the value of the {@code srcHeight} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param srcHeight the height of the source region being decoded,
     * irrespective of how it will be copied into the destination.
     */
    public void setSrcHeight(int srcHeight) {
        this.srcHeight = srcHeight;
    }

    // First source pixel to be read

    /**
     * Sets the value of the {@code sourceXOffset} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param sourceXOffset the horizontal source offset to be used when
     * mapping between source and destination coordinates.
     */
    public void setSourceXOffset(int sourceXOffset) {
        this.sourceXOffset = sourceXOffset;
    }

    /**
     * Sets the value of the {@code dstXOffset} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param dstXOffset the horizontal destination offset to be
     * used when mapping between source and destination coordinates.
     */
    public void setDstXOffset(int dstXOffset) {
        this.dstXOffset = dstXOffset;
    }

    /**
     * Sets the value of the {@code sourceYOffset}.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param sourceYOffset the vertical source offset to be used when
     * mapping between source and destination coordinates.
     */
    public void setSourceYOffset(int sourceYOffset) {
        this.sourceYOffset = sourceYOffset;
    }

    /**
     * Sets the value of the {@code dstYOffset} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param dstYOffset the vertical destination offset to be
     * used when mapping between source and destination coordinates.
     */
    public void setDstYOffset(int dstYOffset) {
        this.dstYOffset = dstYOffset;
    }

    // Subsampling to be performed

    /**
     * Sets the value of the {@code subsampleX} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param subsampleX the horizontal subsampling factor.
     *
     * @throws IllegalArgumentException if {@code subsampleX} is
     * less than or equal to 0.
     */
    public void setSubsampleX(int subsampleX) {
        if (subsampleX <= 0) {
            throw new IllegalArgumentException("subsampleX <= 0!");
        }
        this.subsampleX = subsampleX;
    }

    /**
     * Sets the value of the {@code subsampleY} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param subsampleY the vertical subsampling factor.
     *
     * @throws IllegalArgumentException if {@code subsampleY} is
     * less than or equal to 0.
     */
    public void setSubsampleY(int subsampleY) {
        if (subsampleY <= 0) {
            throw new IllegalArgumentException("subsampleY <= 0!");
        }
        this.subsampleY = subsampleY;
    }

    // Band subsetting/rearrangement

    /**
     * Sets the value of the {@code sourceBands} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param sourceBands an array of {@code int}s
     * specifying the source bands to be read.
     */
    public void setSourceBands(int[] sourceBands) {
        this.sourceBands = sourceBands == null ?
            null : sourceBands.clone();
    }

    /**
     * Sets the value of the {@code destinationBands} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param destinationBands an array of {@code int}s
     * specifying the destination bands to be written.
     */
    public void setDestinationBands(int[] destinationBands) {
        this.destinationBands = destinationBands == null ?
            null : destinationBands.clone();
    }

    // Destination image and region

    /**
     * Sets the value of the {@code image} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param image the destination {@code BufferedImage}.
     */
    public void setImage(BufferedImage image) {
        this.image = image;
    }

    /**
     * Sets the value of the {@code dstMinX} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param dstMinX the minimum X coordinate of the destination
     * region.
     */
    public void setDstMinX(int dstMinX) {
        this.dstMinX = dstMinX;
    }

    /**
     * Sets the value of the {@code dstMinY} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param dstMinY the minimum Y coordinate of the destination
     * region.
     */
    public void setDstMinY(int dstMinY) {
        this.dstMinY = dstMinY;
    }

    /**
     * Sets the value of the {@code dstWidth} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param dstWidth the width of the destination region.
     */
    public void setDstWidth(int dstWidth) {
        this.dstWidth = dstWidth;
    }

    /**
     * Sets the value of the {@code dstHeight} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param dstHeight the height of the destination region.
     */
    public void setDstHeight(int dstHeight) {
        this.dstHeight = dstHeight;
    }

    // Active source region

    /**
     * Sets the value of the {@code activeSrcMinX} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param activeSrcMinX the minimum X coordinate of the active
     * source region.
     */
    public void setActiveSrcMinX(int activeSrcMinX) {
        this.activeSrcMinX = activeSrcMinX;
    }

    /**
     * Sets the value of the {@code activeSrcMinY} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param activeSrcMinY the minimum Y coordinate of the active
     * source region.
     */
    public void setActiveSrcMinY(int activeSrcMinY) {
        this.activeSrcMinY = activeSrcMinY;
    }

    /**
     * Sets the value of the {@code activeSrcWidth} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param activeSrcWidth the width of the active source region.
     */
    public void setActiveSrcWidth(int activeSrcWidth) {
        this.activeSrcWidth = activeSrcWidth;
    }

    /**
     * Sets the value of the {@code activeSrcHeight} field.
     *
     * <p> If this method is called, the {@code beginDecoding}
     * method must be called prior to calling any of the decode
     * methods.
     *
     * @param activeSrcHeight the height of the active source region.
     */
    public void setActiveSrcHeight(int activeSrcHeight) {
        this.activeSrcHeight = activeSrcHeight;
    }

    /**
     * Sets the {@code TIFFColorConverter} object describing the color
     * space of the encoded data in the input stream.  If no
     * {@code TIFFColorConverter} is set, no conversion will be performed.
     *
     * @param colorConverter a {@code TIFFColorConverter} object, or
     * {@code null}.
     */
    public void setColorConverter(TIFFColorConverter colorConverter) {
        this.colorConverter = colorConverter;
    }

    /**
     * Returns an {@code ImageTypeSpecifier} describing an image
     * whose underlying data array has the same format as the raw
     * source pixel data.
     *
     * @return an {@code ImageTypeSpecifier}.
     */
    public ImageTypeSpecifier getRawImageType() {
        ImageTypeSpecifier its =
            getRawImageTypeSpecifier(photometricInterpretation,
                                     compression,
                                     samplesPerPixel,
                                     bitsPerSample,
                                     sampleFormat,
                                     extraSamples,
                                     colorMap);
        return its;
    }

    /**
     * Creates a {@code BufferedImage} whose underlying data
     * array will be suitable for holding the raw decoded output of
     * the {@code decodeRaw} method.
     *
     * <p> The default implementation calls
     * {@code getRawImageType}, and calls the resulting
     * {@code ImageTypeSpecifier}'s
     * {@code createBufferedImage} method.
     *
     * @return a {@code BufferedImage} whose underlying data
     * array has the same format as the raw source pixel data, or
     * {@code null} if it is not possible to create such an
     * image.
     */
    public BufferedImage createRawImage() {
        if (planar) {
            // Create a single-banded image of the appropriate data type.

            // Get the number of bits per sample.
            int bps = bitsPerSample[sourceBands[0]];

            // Determine the data type.
            int dataType;
            if(sampleFormat[0] ==
               BaselineTIFFTagSet.SAMPLE_FORMAT_FLOATING_POINT) {
                if(bps <= 32) {
                    dataType = DataBuffer.TYPE_FLOAT;
                } else {
                    dataType = DataBuffer.TYPE_DOUBLE;
                }
            } else if(bps <= 8) {
                dataType = DataBuffer.TYPE_BYTE;
            } else if(bps <= 16) {
                if(sampleFormat[0] ==
                   BaselineTIFFTagSet.SAMPLE_FORMAT_SIGNED_INTEGER) {
                    dataType = DataBuffer.TYPE_SHORT;
                } else {
                    dataType = DataBuffer.TYPE_USHORT;
                }
            } else {
                dataType = DataBuffer.TYPE_INT;
            }

            ColorSpace csGray = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            ImageTypeSpecifier its =
                ImageTypeSpecifier.createInterleaved(csGray,
                                                     new int[] {0},
                                                     dataType,
                                                     false,
                                                     false);

            return its.createBufferedImage(srcWidth, srcHeight);
        } else {
            ImageTypeSpecifier its = getRawImageType();
            if (its == null) {
                return null;
            }

            BufferedImage bi = its.createBufferedImage(srcWidth, srcHeight);
            return bi;
        }
    }

    /**
     * Decodes the source data into the provided {@code byte}
     * array {@code b}, starting at the offset given by
     * {@code dstOffset}.  Each pixel occupies
     * {@code bitsPerPixel} bits, with no padding between pixels.
     * Scanlines are separated by {@code scanlineStride}
     * {@code byte}s.
     *
     * @param b a {@code byte} array to be written.
     * @param dstOffset the starting offset in {@code b} to be
     * written.
     * @param bitsPerPixel the number of bits for each pixel.
     * @param scanlineStride the number of {@code byte}s to
     * advance between that starting pixels of each scanline.
     *
     * @throws IOException if an error occurs reading from the source
     * {@code ImageInputStream}.
     */
    public abstract void decodeRaw(byte[] b,
                                   int dstOffset,
                                   int bitsPerPixel,
                                   int scanlineStride) throws IOException;

    /**
     * Decodes the source data into the provided {@code short}
     * array {@code s}, starting at the offset given by
     * {@code dstOffset}.  Each pixel occupies
     * {@code bitsPerPixel} bits, with no padding between pixels.
     * Scanlines are separated by {@code scanlineStride}
     * {@code short}s
     *
     * <p> The default implementation calls {@code decodeRaw(byte[] b,
     * ...)} and copies the resulting data into {@code s}.
     *
     * @param s a {@code short} array to be written.
     * @param dstOffset the starting offset in {@code s} to be
     * written.
     * @param bitsPerPixel the number of bits for each pixel.
     * @param scanlineStride the number of {@code short}s to
     * advance between that starting pixels of each scanline.
     *
     * @throws IOException if an error occurs reading from the source
     * {@code ImageInputStream}.
     */
    public void decodeRaw(short[] s,
                          int dstOffset,
                          int bitsPerPixel,
                          int scanlineStride) throws IOException {
        int bytesPerRow = (srcWidth*bitsPerPixel + 7)/8;
        int shortsPerRow = bytesPerRow/2;

        byte[] b = new byte[bytesPerRow*srcHeight];
        decodeRaw(b, 0, bitsPerPixel, bytesPerRow);

        int bOffset = 0;
        if(stream.getByteOrder() == ByteOrder.BIG_ENDIAN) {
            for (int j = 0; j < srcHeight; j++) {
                for (int i = 0; i < shortsPerRow; i++) {
                    short hiVal = b[bOffset++];
                    short loVal = b[bOffset++];
                    short sval = (short)((hiVal << 8) | (loVal & 0xff));
                    s[dstOffset + i] = sval;
                }

                dstOffset += scanlineStride;
            }
        } else { // ByteOrder.LITLE_ENDIAN
            for (int j = 0; j < srcHeight; j++) {
                for (int i = 0; i < shortsPerRow; i++) {
                    short loVal = b[bOffset++];
                    short hiVal = b[bOffset++];
                    short sval = (short)((hiVal << 8) | (loVal & 0xff));
                    s[dstOffset + i] = sval;
                }

                dstOffset += scanlineStride;
            }
        }
    }

    /**
     * Decodes the source data into the provided {@code int}
     * array {@code i}, starting at the offset given by
     * {@code dstOffset}.  Each pixel occupies
     * {@code bitsPerPixel} bits, with no padding between pixels.
     * Scanlines are separated by {@code scanlineStride}
     * {@code int}s.
     *
     * <p> The default implementation calls {@code decodeRaw(byte[] b,
     * ...)} and copies the resulting data into {@code i}.
     *
     * @param i an {@code int} array to be written.
     * @param dstOffset the starting offset in {@code i} to be
     * written.
     * @param bitsPerPixel the number of bits for each pixel.
     * @param scanlineStride the number of {@code int}s to
     * advance between that starting pixels of each scanline.
     *
     * @throws IOException if an error occurs reading from the source
     * {@code ImageInputStream}.
     */
    public void decodeRaw(int[] i,
                          int dstOffset,
                          int bitsPerPixel,
                          int scanlineStride) throws IOException {
        int numBands = bitsPerPixel/32;
        int intsPerRow = srcWidth*numBands;
        int bytesPerRow = intsPerRow*4;

        byte[] b = new byte[bytesPerRow*srcHeight];
        decodeRaw(b, 0, bitsPerPixel, bytesPerRow);

        int bOffset = 0;
        if(stream.getByteOrder() == ByteOrder.BIG_ENDIAN) {
            for (int j = 0; j < srcHeight; j++) {
                for (int k = 0; k < intsPerRow; k++) {
                    int v0 = b[bOffset++] & 0xff;
                    int v1 = b[bOffset++] & 0xff;
                    int v2 = b[bOffset++] & 0xff;
                    int v3 = b[bOffset++] & 0xff;
                    int ival = (v0 << 24) | (v1 << 16) | (v2 << 8) | v3;
                    i[dstOffset + k] = ival;
                }

                dstOffset += scanlineStride;
            }
        } else { // ByteOrder.LITLE_ENDIAN
            for (int j = 0; j < srcHeight; j++) {
                for (int k = 0; k < intsPerRow; k++) {
                    int v3 = b[bOffset++] & 0xff;
                    int v2 = b[bOffset++] & 0xff;
                    int v1 = b[bOffset++] & 0xff;
                    int v0 = b[bOffset++] & 0xff;
                    int ival = (v0 << 24) | (v1 << 16) | (v2 << 8) | v3;
                    i[dstOffset + k] = ival;
                }

                dstOffset += scanlineStride;
            }
        }
    }

    /**
     * Decodes the source data into the provided {@code float}
     * array {@code f}, starting at the offset given by
     * {@code dstOffset}.  Each pixel occupies
     * {@code bitsPerPixel} bits, with no padding between pixels.
     * Scanlines are separated by {@code scanlineStride}
     * {@code float}s.
     *
     * <p> The default implementation calls {@code decodeRaw(byte[] b,
     * ...)} and copies the resulting data into {@code f}.
     *
     * @param f a {@code float} array to be written.
     * @param dstOffset the starting offset in {@code f} to be
     * written.
     * @param bitsPerPixel the number of bits for each pixel.
     * @param scanlineStride the number of {@code float}s to
     * advance between that starting pixels of each scanline.
     *
     * @throws IOException if an error occurs reading from the source
     * {@code ImageInputStream}.
     */
    public void decodeRaw(float[] f,
                          int dstOffset,
                          int bitsPerPixel,
                          int scanlineStride) throws IOException {
        int numBands = bitsPerPixel/32;
        int floatsPerRow = srcWidth*numBands;
        int bytesPerRow = floatsPerRow*4;

        byte[] b = new byte[bytesPerRow*srcHeight];
        decodeRaw(b, 0, bitsPerPixel, bytesPerRow);

        int bOffset = 0;
        if(stream.getByteOrder() == ByteOrder.BIG_ENDIAN) {
            for (int j = 0; j < srcHeight; j++) {
                for (int i = 0; i < floatsPerRow; i++) {
                    int v0 = b[bOffset++] & 0xff;
                    int v1 = b[bOffset++] & 0xff;
                    int v2 = b[bOffset++] & 0xff;
                    int v3 = b[bOffset++] & 0xff;
                    int ival = (v0 << 24) | (v1 << 16) | (v2 << 8) | v3;
                    float fval = Float.intBitsToFloat(ival);
                    f[dstOffset + i] = fval;
                }

                dstOffset += scanlineStride;
            }
        } else { // ByteOrder.LITLE_ENDIAN
            for (int j = 0; j < srcHeight; j++) {
                for (int i = 0; i < floatsPerRow; i++) {
                    int v3 = b[bOffset++] & 0xff;
                    int v2 = b[bOffset++] & 0xff;
                    int v1 = b[bOffset++] & 0xff;
                    int v0 = b[bOffset++] & 0xff;
                    int ival = (v0 << 24) | (v1 << 16) | (v2 << 8) | v3;
                    float fval = Float.intBitsToFloat(ival);
                    f[dstOffset + i] = fval;
                }

                dstOffset += scanlineStride;
            }
        }
    }

    /**
     * Decodes the source data into the provided {@code double}
     * array {@code f}, starting at the offset given by
     * {@code dstOffset}.  Each pixel occupies
     * {@code bitsPerPixel} bits, with no padding between pixels.
     * Scanlines are separated by {@code scanlineStride}
     * {@code double}s.
     *
     * <p> The default implementation calls {@code decodeRaw(byte[] b,
     * ...)} and copies the resulting data into {@code f}.
     *
     * @param d a {@code double} array to be written.
     * @param dstOffset the starting offset in {@code f} to be
     * written.
     * @param bitsPerPixel the number of bits for each pixel.
     * @param scanlineStride the number of {@code double}s to
     * advance between that starting pixels of each scanline.
     *
     * @throws IOException if an error occurs reading from the source
     * {@code ImageInputStream}.
     */
    public void decodeRaw(double[] d,
                          int dstOffset,
                          int bitsPerPixel,
                          int scanlineStride) throws IOException {
        int numBands = bitsPerPixel/64;
        int doublesPerRow = srcWidth*numBands;
        int bytesPerRow = doublesPerRow*8;

        byte[] b = new byte[bytesPerRow*srcHeight];
        decodeRaw(b, 0, bitsPerPixel, bytesPerRow);

        int bOffset = 0;
        if(stream.getByteOrder() == ByteOrder.BIG_ENDIAN) {
            for (int j = 0; j < srcHeight; j++) {
                for (int i = 0; i < doublesPerRow; i++) {
                    long v0 = b[bOffset++] & 0xff;
                    long v1 = b[bOffset++] & 0xff;
                    long v2 = b[bOffset++] & 0xff;
                    long v3 = b[bOffset++] & 0xff;
                    long v4 = b[bOffset++] & 0xff;
                    long v5 = b[bOffset++] & 0xff;
                    long v6 = b[bOffset++] & 0xff;
                    long v7 = b[bOffset++] & 0xff;
                    long lval =
                        (v0 << 56) | (v1 << 48) | (v2 << 40) | (v3 << 32)
                        | (v4 << 24) | (v5 << 16) | (v6 << 8) | v7;
                    double dval = Double.longBitsToDouble(lval);
                    d[dstOffset + i] = dval;
                }

                dstOffset += scanlineStride;
            }
        } else { // ByteOrder.LITLE_ENDIAN
            for (int j = 0; j < srcHeight; j++) {
                for (int i = 0; i < doublesPerRow; i++) {
                    long v7 = b[bOffset++] & 0xff;
                    long v6 = b[bOffset++] & 0xff;
                    long v5 = b[bOffset++] & 0xff;
                    long v4 = b[bOffset++] & 0xff;
                    long v3 = b[bOffset++] & 0xff;
                    long v2 = b[bOffset++] & 0xff;
                    long v1 = b[bOffset++] & 0xff;
                    long v0 = b[bOffset++] & 0xff;
                    long lval =
                        (v0 << 56) | (v1 << 48) | (v2 << 40) | (v3 << 32)
                        | (v4 << 24) | (v5 << 16) | (v6 << 8) | v7;
                    double dval = Double.longBitsToDouble(lval);
                    d[dstOffset + i] = dval;
                }

                dstOffset += scanlineStride;
            }
        }
    }

    //
    // Values used to prevent unneeded recalculation of bit adjustment table.
    //
    private boolean isFirstBitDepthTable = true;
    private boolean planarCache = false;
    private int[] destBitsPerSampleCache = null;
    private int[] sourceBandsCache = null;
    private int[] bitsPerSampleCache = null;
    private int[] destinationBandsCache = null;

    /**
     * This routine is called prior to a sequence of calls to the
     * {@code decode} method, in order to allow any necessary
     * tables or other structures to be initialized based on metadata
     * values.  This routine is guaranteed to be called any time the
     * metadata values have changed.
     *
     * <p> The default implementation computes tables used by the
     * {@code decode} method to rescale components to different
     * bit depths.  Thus, if this method is overridden, it is
     * important for the subclass method to call {@code super()},
     * unless it overrides {@code decode} as well.
     */
    public void beginDecoding() {
        // Note: This method assumes that sourceBands, destinationBands,
        // and bitsPerSample are all non-null which is true as they are
        // set up that way in TIFFImageReader. Also the lengths and content
        // of sourceBands and destinationBands are checked in TIFFImageReader
        // before the present method is invoked.

        // Determine if all of the relevant output bands have the
        // same bit depth as the source data
        this.adjustBitDepths = false;
        int numBands = destinationBands.length;
        int[] destBitsPerSample = null;
        if(planar) {
            int totalNumBands = bitsPerSample.length;
            destBitsPerSample = new int[totalNumBands];
            int dbps = image.getSampleModel().getSampleSize(0);
            for(int b = 0; b < totalNumBands; b++) {
                destBitsPerSample[b] = dbps;
            }
        } else {
            destBitsPerSample = image.getSampleModel().getSampleSize();
        }

        for (int b = 0; b < numBands; b++) {
            if (destBitsPerSample[destinationBands[b]] !=
                bitsPerSample[sourceBands[b]]) {
                adjustBitDepths = true;
                break;
            }
        }

        // If the bit depths differ, create a lookup table
        // per band to perform the conversion
        if(adjustBitDepths) {
            // Compute the table only if this is the first time one is
            // being computed or if any of the variables on which the
            // table is based have changed.
            if(this.isFirstBitDepthTable ||
               planar != planarCache ||
               !areIntArraysEqual(destBitsPerSample,
                                  destBitsPerSampleCache) ||
               !areIntArraysEqual(sourceBands,
                                  sourceBandsCache) ||
               !areIntArraysEqual(bitsPerSample,
                                  bitsPerSampleCache) ||
               !areIntArraysEqual(destinationBands,
                                  destinationBandsCache)) {

                this.isFirstBitDepthTable = false;

                // Cache some variables.
                this.planarCache = planar;
                this.destBitsPerSampleCache =
                    destBitsPerSample.clone(); // never null ...
                this.sourceBandsCache = sourceBands == null ?
                    null : sourceBands.clone();
                this.bitsPerSampleCache = bitsPerSample == null ?
                    null : bitsPerSample.clone();
                this.destinationBandsCache = destinationBands.clone();

                // Allocate and fill the table.
                bitDepthScale = new int[numBands][];
                for (int b = 0; b < numBands; b++) {
                    int maxInSample = (1 << bitsPerSample[sourceBands[b]]) - 1;
                    int halfMaxInSample = maxInSample/2;

                    int maxOutSample =
                        (1 << destBitsPerSample[destinationBands[b]]) - 1;

                    bitDepthScale[b] = new int[maxInSample + 1];
                    for (int s = 0; s <= maxInSample; s++) {
                        bitDepthScale[b][s] =
                            (s*maxOutSample + halfMaxInSample)/
                            maxInSample;
                    }
                }
            }
        } else { // !adjustBitDepths
            // Clear any prior table.
            this.bitDepthScale = null;
        }

        // Determine whether source and destination band lists are ramps.
        // Note that these conditions will be true for planar images if
        // and only if samplesPerPixel == 1, sourceBands[0] == 0, and
        // destinationBands[0] == 0. For the purposes of this method, the
        // only difference between such a planar image and a chunky image
        // is the setting of the PlanarConfiguration field.
        boolean sourceBandsNormal = false;
        boolean destinationBandsNormal = false;
        if (numBands == samplesPerPixel) {
            sourceBandsNormal = true;
            destinationBandsNormal = true;
            for (int i = 0; i < numBands; i++) {
                if (sourceBands[i] != i) {
                    sourceBandsNormal = false;
                }
                if (destinationBands[i] != i) {
                    destinationBandsNormal = false;
                }
            }
        }

        // Determine whether the image is bilevel and/or contiguous.
        // Note that a planar image could be bilevel but it will not
        // be contiguous unless it has a single component band stored
        // in a single bank.
        this.isBilevel =
            ImageUtil.isBinary(this.image.getRaster().getSampleModel());
        this.isContiguous = this.isBilevel ?
            true : ImageUtil.imageIsContiguous(this.image);

        // Analyze destination image to see if we can copy into it
        // directly

        this.isImageSimple =
            (colorConverter == null) &&
            (subsampleX == 1) && (subsampleY == 1) &&
            (srcWidth == dstWidth) && (srcHeight == dstHeight) &&
            ((dstMinX + dstWidth) <= image.getWidth()) &&
            ((dstMinY + dstHeight) <= image.getHeight()) &&
            sourceBandsNormal && destinationBandsNormal &&
            !adjustBitDepths;
    }

    /**
     * Decodes the input bit stream (located in the
     * {@code ImageInputStream} {@code stream}, at offset
     * {@code offset}, and continuing for {@code byteCount}
     * bytes) into the output {@code BufferedImage}
     * {@code image}.
     *
     * <p> The default implementation analyzes the destination image
     * to determine if it is suitable as the destination for the
     * {@code decodeRaw} method.  If not, a suitable image is
     * created.  Next, {@code decodeRaw} is called to perform the
     * actual decoding, and the results are copied into the
     * destination image if necessary.  Subsampling and offsetting are
     * performed automatically.
     *
     * <p> The precise responsibilities of this routine are as
     * follows.  The input bit stream is defined by the instance
     * variables {@code stream}, {@code offset}, and
     * {@code byteCount}.  These bits contain the data for the
     * region of the source image defined by {@code srcMinX},
     * {@code srcMinY}, {@code srcWidth}, and
     * {@code srcHeight}.
     *
     * <p> The source data is required to be subsampling, starting at
     * the {@code sourceXOffset}th column and including
     * every {@code subsampleX}th pixel thereafter (and similarly
     * for {@code sourceYOffset} and
     * {@code subsampleY}).
     *
     * <p> Pixels are copied into the destination with an addition shift of
     * ({@code dstXOffset}, {@code dstYOffset}).  The complete
     * set of formulas relating the source and destination coordinate spaces
     * are:
     *
     * <pre>
     * dx = (sx - sourceXOffset)/subsampleX + dstXOffset;
     * dy = (sy - sourceYOffset)/subsampleY + dstYOffset;
     * </pre>
     *
     * Only source pixels such that {@code (sx - sourceXOffset) %
     * subsampleX == 0} and {@code (sy - sourceYOffset) %
     * subsampleY == 0} are copied.
     *
     * <p> The inverse mapping, from destination to source coordinates,
     * is one-to-one:
     *
     * <pre>
     * sx = (dx - dstXOffset)*subsampleX + sourceXOffset;
     * sy = (dy - dstYOffset)*subsampleY + sourceYOffset;
     * </pre>
     *
     * <p> The region of the destination image to be updated is given
     * by the instance variables {@code dstMinX},
     * {@code dstMinY}, {@code dstWidth}, and
     * {@code dstHeight}.
     *
     * <p> It is possible that not all of the source data being read
     * will contribute to the destination image.  For example, the
     * destination offsets could be set such that some of the source
     * pixels land outside of the bounds of the image.  As a
     * convenience, the bounds of the active source region (that is,
     * the region of the strip or tile being read that actually
     * contributes to the destination image, taking clipping into
     * account) are available as {@code activeSrcMinX},
     * {@code activeSrcMinY}, {@code activeSrcWidth} and
     * {@code activeSrcHeight}.  Thus, the source pixel at
     * ({@code activeSrcMinX}, {@code activeSrcMinY}) will
     * map to the destination pixel ({@code dstMinX},
     * {@code dstMinY}).
     *
     * <p> The sequence of source bands given by
     * {@code sourceBands} are to be copied into the sequence of
     * bands in the destination given by
     * {@code destinationBands}.
     *
     * <p> Some standard tag information is provided the instance
     * variables {@code photometricInterpretation},
     * {@code compression}, {@code samplesPerPixel},
     * {@code bitsPerSample}, {@code sampleFormat},
     * {@code extraSamples}, and {@code colorMap}.
     *
     * <p> In practice, unless there is a significant performance
     * advantage to be gained by overriding this routine, most users
     * will prefer to use the default implementation of this routine,
     * and instead override the {@code decodeRaw} and/or
     * {@code getRawImageType} methods.
     *
     * @exception IOException if an error occurs in
     * {@code decodeRaw}.
     */
    public void decode() throws IOException {
        byte[] byteData = null;
        short[] shortData = null;
        int[] intData = null;
        float[] floatData = null;
        double[] doubleData = null;

        int dstOffset = 0;
        int pixelBitStride = 1;
        int scanlineStride = 0;

        // Analyze raw image

        this.rawImage = null;
        if(isImageSimple) {
            if(isBilevel) {
                rawImage = this.image;
            } else if (isContiguous) {
                rawImage =
                    image.getSubimage(dstMinX, dstMinY, dstWidth, dstHeight);
            }
        }

        boolean isDirectCopy = rawImage != null;

        if(rawImage == null) {
            rawImage = createRawImage();
            if (rawImage == null) {
                throw new IIOException("Couldn't create image buffer!");
            }
        }

        WritableRaster ras = rawImage.getRaster();

        if(isBilevel) {
            Rectangle rect = isImageSimple ?
                new Rectangle(dstMinX, dstMinY, dstWidth, dstHeight) :
                ras.getBounds();
            byteData = ImageUtil.getPackedBinaryData(ras, rect);
            dstOffset = 0;
            pixelBitStride = 1;
            scanlineStride = (rect.width + 7)/8;
        } else {
            SampleModel sm = ras.getSampleModel();
            DataBuffer db = ras.getDataBuffer();

            boolean isSupportedType = false;

            if (sm instanceof ComponentSampleModel) {
                ComponentSampleModel csm = (ComponentSampleModel)sm;
                dstOffset = csm.getOffset(-ras.getSampleModelTranslateX(),
                                          -ras.getSampleModelTranslateY());
                scanlineStride = csm.getScanlineStride();
                if(db instanceof DataBufferByte) {
                    DataBufferByte dbb = (DataBufferByte)db;

                    byteData = dbb.getData();
                    pixelBitStride = csm.getPixelStride()*8;
                    isSupportedType = true;
                } else if(db instanceof DataBufferUShort) {
                    DataBufferUShort dbus = (DataBufferUShort)db;

                    shortData = dbus.getData();
                    pixelBitStride = csm.getPixelStride()*16;
                    isSupportedType = true;
                } else if(db instanceof DataBufferShort) {
                    DataBufferShort dbs = (DataBufferShort)db;

                    shortData = dbs.getData();
                    pixelBitStride = csm.getPixelStride()*16;
                    isSupportedType = true;
                } else if(db instanceof DataBufferInt) {
                    DataBufferInt dbi = (DataBufferInt)db;

                    intData = dbi.getData();
                    pixelBitStride = csm.getPixelStride()*32;
                    isSupportedType = true;
                } else if(db instanceof DataBufferFloat) {
                    DataBufferFloat dbf = (DataBufferFloat)db;

                    floatData = dbf.getData();
                    pixelBitStride = csm.getPixelStride()*32;
                    isSupportedType = true;
                } else if(db instanceof DataBufferDouble) {
                    DataBufferDouble dbd = (DataBufferDouble)db;

                    doubleData = dbd.getData();
                    pixelBitStride = csm.getPixelStride()*64;
                    isSupportedType = true;
                }
            } else if (sm instanceof MultiPixelPackedSampleModel) {
                MultiPixelPackedSampleModel mppsm =
                    (MultiPixelPackedSampleModel)sm;
                dstOffset =
                    mppsm.getOffset(-ras.getSampleModelTranslateX(),
                                    -ras.getSampleModelTranslateY());
                pixelBitStride = mppsm.getPixelBitStride();
                scanlineStride = mppsm.getScanlineStride();
                if(db instanceof DataBufferByte) {
                    DataBufferByte dbb = (DataBufferByte)db;

                    byteData = dbb.getData();
                    isSupportedType = true;
                } else if(db instanceof DataBufferUShort) {
                    DataBufferUShort dbus = (DataBufferUShort)db;

                    shortData = dbus.getData();
                    isSupportedType = true;
                } else if(db instanceof DataBufferInt) {
                    DataBufferInt dbi = (DataBufferInt)db;

                    intData = dbi.getData();
                    isSupportedType = true;
                }
            } else if (sm instanceof SinglePixelPackedSampleModel) {
                SinglePixelPackedSampleModel sppsm =
                    (SinglePixelPackedSampleModel)sm;
                dstOffset =
                    sppsm.getOffset(-ras.getSampleModelTranslateX(),
                                    -ras.getSampleModelTranslateY());
                scanlineStride = sppsm.getScanlineStride();
                if(db instanceof DataBufferByte) {
                    DataBufferByte dbb = (DataBufferByte)db;

                    byteData = dbb.getData();
                    pixelBitStride = 8;
                    isSupportedType = true;
                } else if(db instanceof DataBufferUShort) {
                    DataBufferUShort dbus = (DataBufferUShort)db;

                    shortData = dbus.getData();
                    pixelBitStride = 16;
                    isSupportedType = true;
                } else if(db instanceof DataBufferInt) {
                    DataBufferInt dbi = (DataBufferInt)db;

                    intData = dbi.getData();
                    pixelBitStride = 32;
                    isSupportedType = true;
                }
            }

            if(!isSupportedType) {
                throw new IIOException
                    ("Unsupported raw image type: SampleModel = "+sm+
                     "; DataBuffer = "+db);
            }
        }

        if(isBilevel) {
            // Bilevel data are always in a contiguous byte buffer.
            decodeRaw(byteData, dstOffset, pixelBitStride, scanlineStride);
        } else {
            SampleModel sm = ras.getSampleModel();

            // Branch based on whether data are bit-contiguous, i.e.,
            // data are packaed as tightly as possible leaving no unused
            // bits except at the end of a row.
            if(isDataBufferBitContiguous(sm, bitsPerSample)) {
                // Use byte or float data directly.
                if (byteData != null) {
                    decodeRaw(byteData, dstOffset,
                              pixelBitStride, scanlineStride);
                } else if (floatData != null) {
                    decodeRaw(floatData, dstOffset,
                              pixelBitStride, scanlineStride);
                } else if (doubleData != null) {
                    decodeRaw(doubleData, dstOffset,
                              pixelBitStride, scanlineStride);
                } else {
                    if (shortData != null) {
                        if(areSampleSizesEqual(sm) &&
                           sm.getSampleSize(0) == 16) {
                            // Decode directly into short data.
                            decodeRaw(shortData, dstOffset,
                                      pixelBitStride, scanlineStride);
                        } else {
                            // Decode into bytes and reformat into shorts.
                            int bpp = getBitsPerPixel(sm);
                            int bytesPerRow = (bpp*srcWidth + 7)/8;
                            byte[] buf = new byte[bytesPerRow*srcHeight];
                            decodeRaw(buf, 0, bpp, bytesPerRow);
                            reformatData(buf, bytesPerRow, srcHeight,
                                         shortData, null,
                                         dstOffset, scanlineStride);
                        }
                    } else if (intData != null) {
                        if(areSampleSizesEqual(sm) &&
                           sm.getSampleSize(0) == 32) {
                            // Decode directly into int data.
                            decodeRaw(intData, dstOffset,
                                      pixelBitStride, scanlineStride);
                        } else {
                            // Decode into bytes and reformat into ints.
                            int bpp = getBitsPerPixel(sm);
                            int bytesPerRow = (bpp*srcWidth + 7)/8;
                            byte[] buf = new byte[bytesPerRow*srcHeight];
                            decodeRaw(buf, 0, bpp, bytesPerRow);
                            reformatData(buf, bytesPerRow, srcHeight,
                                         null, intData,
                                         dstOffset, scanlineStride);
                        }
                    }
                }
            } else {
                // Read discontiguous data into bytes and set the samples
                // into the Raster.
                int bpp;
                if (planar) {
                    bpp = bitsPerSample[planarBand];
                } else {
                    bpp = 0;
                    for (int bps : bitsPerSample) {
                        bpp += bps;
                    }
                }
                int bytesPerRow = (bpp*srcWidth + 7)/8;
                byte[] buf = new byte[bytesPerRow*srcHeight];
                decodeRaw(buf, 0, bpp, bytesPerRow);
                reformatDiscontiguousData(buf, bitsPerSample, bytesPerRow,
                                          srcWidth, srcHeight,
                                          ras);
            }
        }

        if (colorConverter != null) {
            float[] rgb = new float[3];

            if(byteData != null) {
                for (int j = 0; j < dstHeight; j++) {
                    int idx = dstOffset;
                    for (int i = 0; i < dstWidth; i++) {
                        float x0 = (float)(byteData[idx] & 0xff);
                        float x1 = (float)(byteData[idx + 1] & 0xff);
                        float x2 = (float)(byteData[idx + 2] & 0xff);

                        colorConverter.toRGB(x0, x1, x2, rgb);

                        byteData[idx] = (byte)(rgb[0]);
                        byteData[idx + 1] = (byte)(rgb[1]);
                        byteData[idx + 2] = (byte)(rgb[2]);

                        idx += 3;
                    }

                    dstOffset += scanlineStride;
                }
            } else if(shortData != null) {
                if(sampleFormat[0] ==
                   BaselineTIFFTagSet.SAMPLE_FORMAT_SIGNED_INTEGER) {
                    for (int j = 0; j < dstHeight; j++) {
                        int idx = dstOffset;
                        for (int i = 0; i < dstWidth; i++) {
                            float x0 = (float)shortData[idx];
                            float x1 = (float)shortData[idx + 1];
                            float x2 = (float)shortData[idx + 2];

                            colorConverter.toRGB(x0, x1, x2, rgb);

                            shortData[idx] = (short)(rgb[0]);
                            shortData[idx + 1] = (short)(rgb[1]);
                            shortData[idx + 2] = (short)(rgb[2]);

                            idx += 3;
                        }

                        dstOffset += scanlineStride;
                    }
                } else {
                    for (int j = 0; j < dstHeight; j++) {
                        int idx = dstOffset;
                        for (int i = 0; i < dstWidth; i++) {
                            float x0 = (float)(shortData[idx] & 0xffff);
                            float x1 = (float)(shortData[idx + 1] & 0xffff);
                            float x2 = (float)(shortData[idx + 2] & 0xffff);

                            colorConverter.toRGB(x0, x1, x2, rgb);

                            shortData[idx] = (short)(rgb[0]);
                            shortData[idx + 1] = (short)(rgb[1]);
                            shortData[idx + 2] = (short)(rgb[2]);

                            idx += 3;
                        }

                        dstOffset += scanlineStride;
                    }
                }
            } else if(intData != null) {
                for (int j = 0; j < dstHeight; j++) {
                    int idx = dstOffset;
                    for (int i = 0; i < dstWidth; i++) {
                        float x0 = (float)intData[idx];
                        float x1 = (float)intData[idx + 1];
                        float x2 = (float)intData[idx + 2];

                        colorConverter.toRGB(x0, x1, x2, rgb);

                        intData[idx] = (int)(rgb[0]);
                        intData[idx + 1] = (int)(rgb[1]);
                        intData[idx + 2] = (int)(rgb[2]);

                        idx += 3;
                    }

                    dstOffset += scanlineStride;
                }
            } else if(floatData != null) {
                for (int j = 0; j < dstHeight; j++) {
                    int idx = dstOffset;
                    for (int i = 0; i < dstWidth; i++) {
                        float x0 = floatData[idx];
                        float x1 = floatData[idx + 1];
                        float x2 = floatData[idx + 2];

                        colorConverter.toRGB(x0, x1, x2, rgb);

                        floatData[idx] = rgb[0];
                        floatData[idx + 1] = rgb[1];
                        floatData[idx + 2] = rgb[2];

                        idx += 3;
                    }

                    dstOffset += scanlineStride;
                }
            } else if(doubleData != null) {
                for (int j = 0; j < dstHeight; j++) {
                    int idx = dstOffset;
                    for (int i = 0; i < dstWidth; i++) {
                        // Note: Possible loss of precision.
                        float x0 = (float)doubleData[idx];
                        float x1 = (float)doubleData[idx + 1];
                        float x2 = (float)doubleData[idx + 2];

                        colorConverter.toRGB(x0, x1, x2, rgb);

                        doubleData[idx] = rgb[0];
                        doubleData[idx + 1] = rgb[1];
                        doubleData[idx + 2] = rgb[2];

                        idx += 3;
                    }

                    dstOffset += scanlineStride;
                }
            }
        }

        if (photometricInterpretation ==
            BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO) {
            if(byteData != null) {
                int bytesPerRow = (srcWidth*pixelBitStride + 7)/8;
                for (int y = 0; y < srcHeight; y++) {
                    int offset = dstOffset + y*scanlineStride;
                    for (int i = 0; i < bytesPerRow; i++) {
                        byteData[offset + i] ^= 0xff;
                    }
                }
            } else if(shortData != null) {
                int shortsPerRow = (srcWidth*pixelBitStride + 15)/16;
                if(sampleFormat[0] ==
                   BaselineTIFFTagSet.SAMPLE_FORMAT_SIGNED_INTEGER) {
                    for (int y = 0; y < srcHeight; y++) {
                        int offset = dstOffset + y*scanlineStride;
                        for (int i = 0; i < shortsPerRow; i++) {
                            int shortOffset = offset + i;
                            shortData[shortOffset] =
                                (short)(Short.MAX_VALUE -
                                        shortData[shortOffset]);
                        }
                    }
                } else {
                    for (int y = 0; y < srcHeight; y++) {
                        int offset = dstOffset + y*scanlineStride;
                        for (int i = 0; i < shortsPerRow; i++) {
                            shortData[offset + i] ^= 0xffff;
                        }
                    }
                }
            } else if(intData != null) {
                int intsPerRow = (srcWidth*pixelBitStride + 31)/32;
                for (int y = 0; y < srcHeight; y++) {
                    int offset = dstOffset + y*scanlineStride;
                    for (int i = 0; i < intsPerRow; i++) {
                        int intOffset = offset + i;
                        intData[intOffset] =
                            Integer.MAX_VALUE - intData[intOffset];
                    }
                }
            } else if(floatData != null) {
                int floatsPerRow = (srcWidth*pixelBitStride + 31)/32;
                for (int y = 0; y < srcHeight; y++) {
                    int offset = dstOffset + y*scanlineStride;
                    for (int i = 0; i < floatsPerRow; i++) {
                        int floatOffset = offset + i;
                        floatData[floatOffset] =
                            1.0F - floatData[floatOffset];
                    }
                }
            } else if(doubleData != null) {
                int doublesPerRow = (srcWidth*pixelBitStride + 63)/64;
                for (int y = 0; y < srcHeight; y++) {
                    int offset = dstOffset + y*scanlineStride;
                    for (int i = 0; i < doublesPerRow; i++) {
                        int doubleOffset = offset + i;
                        doubleData[doubleOffset] =
                            1.0F - doubleData[doubleOffset];
                    }
                }
            }
        }

        if(isBilevel) {
            Rectangle rect = isImageSimple ?
                new Rectangle(dstMinX, dstMinY, dstWidth, dstHeight) :
                ras.getBounds();
            ImageUtil.setPackedBinaryData(byteData, ras, rect);
        }

        if (isDirectCopy) { // rawImage == image) {
            return;
        }

        // Copy the raw image data into the true destination image
        Raster src = rawImage.getRaster();

        // Create band child of source
        Raster srcChild = src.createChild(0, 0,
                                          srcWidth, srcHeight,
                                          srcMinX, srcMinY,
                                          planar ? null : sourceBands);

        WritableRaster dst = image.getRaster();

        // Create dst child covering area and bands to be written
        WritableRaster dstChild = dst.createWritableChild(dstMinX, dstMinY,
                                                          dstWidth, dstHeight,
                                                          dstMinX, dstMinY,
                                                          destinationBands);

        if (subsampleX == 1 && subsampleY == 1 && !adjustBitDepths) {
            srcChild = srcChild.createChild(activeSrcMinX,
                                            activeSrcMinY,
                                            activeSrcWidth, activeSrcHeight,
                                            dstMinX, dstMinY,
                                            null);

            dstChild.setRect(srcChild);
        } else if (subsampleX == 1 && !adjustBitDepths) {
            int sy = activeSrcMinY;
            int dy = dstMinY;
            while (sy < srcMinY + srcHeight) {
                Raster srcRow = srcChild.createChild(activeSrcMinX, sy,
                                                     activeSrcWidth, 1,
                                                     dstMinX, dy,
                                                     null);
                dstChild.setRect(srcRow);

                sy += subsampleY;
                ++dy;
            }
        } else {
            int[] p = srcChild.getPixel(srcMinX, srcMinY, (int[])null);
            int numBands = p.length;

            int sy = activeSrcMinY;
            int dy = dstMinY;

            while (sy < activeSrcMinY + activeSrcHeight) {
                int sx = activeSrcMinX;
                int dx = dstMinX;

                while (sx < activeSrcMinX + activeSrcWidth) {
                    srcChild.getPixel(sx, sy, p);
                    if (adjustBitDepths) {
                        for (int band = 0; band < numBands; band++) {
                            p[band] = bitDepthScale[band][p[band]];
                        }
                    }
                    dstChild.setPixel(dx, dy, p);

                    sx += subsampleX;
                    ++dx;
                }

                sy += subsampleY;
                ++dy;
            }
        }
    }
}
