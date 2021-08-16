/*
 * Copyright (c) 2000, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.imageio;

import java.awt.Point;
import java.awt.Transparency;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.color.ColorSpace;
import java.awt.image.IndexColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Hashtable;

/**
 * A class that allows the format of an image (in particular, its
 * {@code SampleModel} and {@code ColorModel}) to be
 * specified in a convenient manner.
 *
 */
public class ImageTypeSpecifier {

    /**
     * The {@code ColorModel} to be used as a prototype.
     */
    protected ColorModel colorModel;

    /**
     * A {@code SampleModel} to be used as a prototype.
     */
    protected SampleModel sampleModel;

    /**
     * Cached specifiers for all of the standard
     * {@code BufferedImage} types.
     */
    private static ImageTypeSpecifier[] BISpecifier;
    private static ColorSpace sRGB;
    // Initialize the standard specifiers
    static {
        sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);

        BISpecifier =
            new ImageTypeSpecifier[BufferedImage.TYPE_BYTE_INDEXED + 1];
    }

    /**
     * A constructor to be used by inner subclasses only.
     */
    private ImageTypeSpecifier() {}

    /**
     * Constructs an {@code ImageTypeSpecifier} directly
     * from a {@code ColorModel} and a {@code SampleModel}.
     * It is the caller's responsibility to supply compatible
     * parameters.
     *
     * @param colorModel a {@code ColorModel}.
     * @param sampleModel a {@code SampleModel}.
     *
     * @exception IllegalArgumentException if either parameter is
     * {@code null}.
     * @exception IllegalArgumentException if {@code sampleModel}
     * is not compatible with {@code colorModel}.
     */
    public ImageTypeSpecifier(ColorModel colorModel, SampleModel sampleModel) {
        if (colorModel == null) {
            throw new IllegalArgumentException("colorModel == null!");
        }
        if (sampleModel == null) {
            throw new IllegalArgumentException("sampleModel == null!");
        }
        if (!colorModel.isCompatibleSampleModel(sampleModel)) {
            throw new IllegalArgumentException
                ("sampleModel is incompatible with colorModel!");
        }
        this.colorModel = colorModel;
        this.sampleModel = sampleModel;
    }

    /**
     * Constructs an {@code ImageTypeSpecifier} from a
     * {@code RenderedImage}.  If a {@code BufferedImage} is
     * being used, one of the factory methods
     * {@code createFromRenderedImage} or
     * {@code createFromBufferedImageType} should be used instead in
     * order to get a more accurate result.
     *
     * @param image a {@code RenderedImage}.
     *
     * @exception IllegalArgumentException if the argument is
     * {@code null}.
     */
    public ImageTypeSpecifier(RenderedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("image == null!");
        }
        colorModel = image.getColorModel();
        sampleModel = image.getSampleModel();
    }

    // Packed

    static class Packed extends ImageTypeSpecifier {
        ColorSpace colorSpace;
        int redMask;
        int greenMask;
        int blueMask;
        int alphaMask;
        int transferType;
        boolean isAlphaPremultiplied;

        public Packed(ColorSpace colorSpace,
                      int redMask,
                      int greenMask,
                      int blueMask,
                      int alphaMask, // 0 if no alpha
                      int transferType,
                      boolean isAlphaPremultiplied) {
            if (colorSpace == null) {
                throw new IllegalArgumentException("colorSpace == null!");
            }
            if (colorSpace.getType() != ColorSpace.TYPE_RGB) {
                throw new IllegalArgumentException
                    ("colorSpace is not of type TYPE_RGB!");
            }
            if (transferType != DataBuffer.TYPE_BYTE &&
                transferType != DataBuffer.TYPE_USHORT &&
                transferType != DataBuffer.TYPE_INT) {
                throw new IllegalArgumentException
                    ("Bad value for transferType!");
            }
            if (redMask == 0 && greenMask == 0 &&
                blueMask == 0 && alphaMask == 0) {
                throw new IllegalArgumentException
                    ("No mask has at least 1 bit set!");
            }
            this.colorSpace = colorSpace;
            this.redMask = redMask;
            this.greenMask = greenMask;
            this.blueMask = blueMask;
            this.alphaMask = alphaMask;
            this.transferType = transferType;
            this.isAlphaPremultiplied = isAlphaPremultiplied;

            int bits = 32;
            this.colorModel =
                new DirectColorModel(colorSpace,
                                     bits,
                                     redMask, greenMask, blueMask,
                                     alphaMask, isAlphaPremultiplied,
                                     transferType);
            this.sampleModel = colorModel.createCompatibleSampleModel(1, 1);
        }
    }

    /**
     * Returns a specifier for a packed image format that will use a
     * {@code DirectColorModel} and a packed
     * {@code SampleModel} to store each pixel packed into in a
     * single byte, short, or int.
     *
     * @param colorSpace the desired {@code ColorSpace}.
     * @param redMask a contiguous mask indicated the position of the
     * red channel.
     * @param greenMask a contiguous mask indicated the position of the
     * green channel.
     * @param blueMask a contiguous mask indicated the position of the
     * blue channel.
     * @param alphaMask a contiguous mask indicated the position of the
     * alpha channel.
     * @param transferType the desired {@code SampleModel} transfer type.
     * @param isAlphaPremultiplied {@code true} if the color channels
     * will be premultipled by the alpha channel.
     *
     * @return an {@code ImageTypeSpecifier} with the desired
     * characteristics.
     *
     * @exception IllegalArgumentException if {@code colorSpace}
     * is {@code null}.
     * @exception IllegalArgumentException if {@code colorSpace}
     * is not of type {@code TYPE_RGB}.
     * @exception IllegalArgumentException if no mask has at least 1
     * bit set.
     * @exception IllegalArgumentException if
     * {@code transferType} if not one of
     * {@code DataBuffer.TYPE_BYTE},
     * {@code DataBuffer.TYPE_USHORT}, or
     * {@code DataBuffer.TYPE_INT}.
     */
    public static ImageTypeSpecifier
        createPacked(ColorSpace colorSpace,
                     int redMask,
                     int greenMask,
                     int blueMask,
                     int alphaMask, // 0 if no alpha
                     int transferType,
                     boolean isAlphaPremultiplied) {
        return new ImageTypeSpecifier.Packed(colorSpace,
                                             redMask,
                                             greenMask,
                                             blueMask,
                                             alphaMask, // 0 if no alpha
                                             transferType,
                                             isAlphaPremultiplied);
    }

    static ColorModel createComponentCM(ColorSpace colorSpace,
                                        int numBands,
                                        int dataType,
                                        boolean hasAlpha,
                                        boolean isAlphaPremultiplied) {
        int transparency =
            hasAlpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE;

        int[] numBits = new int[numBands];
        int bits = DataBuffer.getDataTypeSize(dataType);

        for (int i = 0; i < numBands; i++) {
            numBits[i] = bits;
        }

        return new ComponentColorModel(colorSpace,
                                       numBits,
                                       hasAlpha,
                                       isAlphaPremultiplied,
                                       transparency,
                                       dataType);
    }

    // Interleaved

    static class Interleaved extends ImageTypeSpecifier {
        ColorSpace colorSpace;
        int[] bandOffsets;
        int dataType;
        boolean hasAlpha;
        boolean isAlphaPremultiplied;

        public Interleaved(ColorSpace colorSpace,
                           int[] bandOffsets,
                           int dataType,
                           boolean hasAlpha,
                           boolean isAlphaPremultiplied) {
            if (colorSpace == null) {
                throw new IllegalArgumentException("colorSpace == null!");
            }
            if (bandOffsets == null) {
                throw new IllegalArgumentException("bandOffsets == null!");
            }
            int numBands = colorSpace.getNumComponents() +
                (hasAlpha ? 1 : 0);
            if (bandOffsets.length != numBands) {
                throw new IllegalArgumentException
                    ("bandOffsets.length is wrong!");
            }
            if (dataType != DataBuffer.TYPE_BYTE &&
                dataType != DataBuffer.TYPE_SHORT &&
                dataType != DataBuffer.TYPE_USHORT &&
                dataType != DataBuffer.TYPE_INT &&
                dataType != DataBuffer.TYPE_FLOAT &&
                dataType != DataBuffer.TYPE_DOUBLE) {
                throw new IllegalArgumentException
                    ("Bad value for dataType!");
            }
            this.colorSpace = colorSpace;
            this.bandOffsets = bandOffsets.clone();
            this.dataType = dataType;
            this.hasAlpha = hasAlpha;
            this.isAlphaPremultiplied = isAlphaPremultiplied;

            this.colorModel =
                ImageTypeSpecifier.createComponentCM(colorSpace,
                                                     bandOffsets.length,
                                                     dataType,
                                                     hasAlpha,
                                                     isAlphaPremultiplied);

            int minBandOffset = bandOffsets[0];
            int maxBandOffset = minBandOffset;
            for (int i = 0; i < bandOffsets.length; i++) {
                int offset = bandOffsets[i];
                minBandOffset = Math.min(offset, minBandOffset);
                maxBandOffset = Math.max(offset, maxBandOffset);
            }
            int pixelStride = maxBandOffset - minBandOffset + 1;

            int w = 1;
            int h = 1;
            this.sampleModel =
                new PixelInterleavedSampleModel(dataType,
                                                w, h,
                                                pixelStride,
                                                w*pixelStride,
                                                bandOffsets);
        }

        public boolean equals(Object o) {
            if ((o == null) ||
                !(o instanceof ImageTypeSpecifier.Interleaved)) {
                return false;
            }

            ImageTypeSpecifier.Interleaved that =
                (ImageTypeSpecifier.Interleaved)o;

            if ((!(this.colorSpace.equals(that.colorSpace))) ||
                (this.dataType != that.dataType) ||
                (this.hasAlpha != that.hasAlpha) ||
                (this.isAlphaPremultiplied != that.isAlphaPremultiplied) ||
                (this.bandOffsets.length != that.bandOffsets.length)) {
                return false;
            }

            for (int i = 0; i < bandOffsets.length; i++) {
                if (this.bandOffsets[i] != that.bandOffsets[i]) {
                    return false;
                }
            }

            return true;
        }

        public int hashCode() {
            return (super.hashCode() +
                    (4 * bandOffsets.length) +
                    (25 * dataType) +
                    (hasAlpha ? 17 : 18));
        }
    }

    /**
     * Returns a specifier for an interleaved image format that will
     * use a {@code ComponentColorModel} and a
     * {@code PixelInterleavedSampleModel} to store each pixel
     * component in a separate byte, short, or int.
     *
     * @param colorSpace the desired {@code ColorSpace}.
     * @param bandOffsets an array of {@code int}s indicating the
     * offsets for each band.
     * @param dataType the desired data type, as one of the enumerations
     * from the {@code DataBuffer} class.
     * @param hasAlpha {@code true} if an alpha channel is desired.
     * @param isAlphaPremultiplied {@code true} if the color channels
     * will be premultipled by the alpha channel.
     *
     * @return an {@code ImageTypeSpecifier} with the desired
     * characteristics.
     *
     * @exception IllegalArgumentException if {@code colorSpace}
     * is {@code null}.
     * @exception IllegalArgumentException if {@code bandOffsets}
     * is {@code null}.
     * @exception IllegalArgumentException if {@code dataType} is
     * not one of the legal {@code DataBuffer.TYPE_*} constants.
     * @exception IllegalArgumentException if
     * {@code bandOffsets.length} does not equal the number of
     * color space components, plus 1 if {@code hasAlpha} is
     * {@code true}.
     */
    public static ImageTypeSpecifier
        createInterleaved(ColorSpace colorSpace,
                          int[] bandOffsets,
                          int dataType,
                          boolean hasAlpha,
                          boolean isAlphaPremultiplied) {
        return new ImageTypeSpecifier.Interleaved(colorSpace,
                                                  bandOffsets,
                                                  dataType,
                                                  hasAlpha,
                                                  isAlphaPremultiplied);
    }

    // Banded

    static class Banded extends ImageTypeSpecifier {
        ColorSpace colorSpace;
        int[] bankIndices;
        int[] bandOffsets;
        int dataType;
        boolean hasAlpha;
        boolean isAlphaPremultiplied;

        public Banded(ColorSpace colorSpace,
                      int[] bankIndices,
                      int[] bandOffsets,
                      int dataType,
                      boolean hasAlpha,
                      boolean isAlphaPremultiplied) {
            if (colorSpace == null) {
                throw new IllegalArgumentException("colorSpace == null!");
            }
            if (bankIndices == null) {
                throw new IllegalArgumentException("bankIndices == null!");
            }
            if (bandOffsets == null) {
                throw new IllegalArgumentException("bandOffsets == null!");
            }
            if (bankIndices.length != bandOffsets.length) {
                throw new IllegalArgumentException
                    ("bankIndices.length != bandOffsets.length!");
            }
            if (dataType != DataBuffer.TYPE_BYTE &&
                dataType != DataBuffer.TYPE_SHORT &&
                dataType != DataBuffer.TYPE_USHORT &&
                dataType != DataBuffer.TYPE_INT &&
                dataType != DataBuffer.TYPE_FLOAT &&
                dataType != DataBuffer.TYPE_DOUBLE) {
                throw new IllegalArgumentException
                    ("Bad value for dataType!");
            }
            int numBands = colorSpace.getNumComponents() +
                (hasAlpha ? 1 : 0);
            if (bandOffsets.length != numBands) {
                throw new IllegalArgumentException
                    ("bandOffsets.length is wrong!");
            }

            this.colorSpace = colorSpace;
            this.bankIndices = bankIndices.clone();
            this.bandOffsets = bandOffsets.clone();
            this.dataType = dataType;
            this.hasAlpha = hasAlpha;
            this.isAlphaPremultiplied = isAlphaPremultiplied;

            this.colorModel =
                ImageTypeSpecifier.createComponentCM(colorSpace,
                                                     bankIndices.length,
                                                     dataType,
                                                     hasAlpha,
                                                     isAlphaPremultiplied);

            int w = 1;
            int h = 1;
            this.sampleModel = new BandedSampleModel(dataType,
                                                     w, h,
                                                     w,
                                                     bankIndices,
                                                     bandOffsets);
        }

        public boolean equals(Object o) {
            if ((o == null) ||
                !(o instanceof ImageTypeSpecifier.Banded)) {
                return false;
            }

            ImageTypeSpecifier.Banded that =
                (ImageTypeSpecifier.Banded)o;

            if ((!(this.colorSpace.equals(that.colorSpace))) ||
                (this.dataType != that.dataType) ||
                (this.hasAlpha != that.hasAlpha) ||
                (this.isAlphaPremultiplied != that.isAlphaPremultiplied) ||
                (this.bankIndices.length != that.bankIndices.length) ||
                (this.bandOffsets.length != that.bandOffsets.length)) {
                return false;
            }

            for (int i = 0; i < bankIndices.length; i++) {
                if (this.bankIndices[i] != that.bankIndices[i]) {
                    return false;
                }
            }

            for (int i = 0; i < bandOffsets.length; i++) {
                if (this.bandOffsets[i] != that.bandOffsets[i]) {
                    return false;
                }
            }

            return true;
        }

        public int hashCode() {
            return (super.hashCode() +
                    (3 * bandOffsets.length) +
                    (7 * bankIndices.length) +
                    (21 * dataType) +
                    (hasAlpha ? 19 : 29));
        }
    }

    /**
     * Returns a specifier for a banded image format that will use a
     * {@code ComponentColorModel} and a
     * {@code BandedSampleModel} to store each channel in a
     * separate array.
     *
     * @param colorSpace the desired {@code ColorSpace}.
     * @param bankIndices an array of {@code int}s indicating the
     * bank in which each band will be stored.
     * @param bandOffsets an array of {@code int}s indicating the
     * starting offset of each band within its bank.
     * @param dataType the desired data type, as one of the enumerations
     * from the {@code DataBuffer} class.
     * @param hasAlpha {@code true} if an alpha channel is desired.
     * @param isAlphaPremultiplied {@code true} if the color channels
     * will be premultipled by the alpha channel.
     *
     * @return an {@code ImageTypeSpecifier} with the desired
     * characteristics.
     *
     * @exception IllegalArgumentException if {@code colorSpace}
     * is {@code null}.
     * @exception IllegalArgumentException if {@code bankIndices}
     * is {@code null}.
     * @exception IllegalArgumentException if {@code bandOffsets}
     * is {@code null}.
     * @exception IllegalArgumentException if the lengths of
     * {@code bankIndices} and {@code bandOffsets} differ.
     * @exception IllegalArgumentException if
     * {@code bandOffsets.length} does not equal the number of
     * color space components, plus 1 if {@code hasAlpha} is
     * {@code true}.
     * @exception IllegalArgumentException if {@code dataType} is
     * not one of the legal {@code DataBuffer.TYPE_*} constants.
     */
    public static ImageTypeSpecifier
        createBanded(ColorSpace colorSpace,
                     int[] bankIndices,
                     int[] bandOffsets,
                     int dataType,
                     boolean hasAlpha,
                     boolean isAlphaPremultiplied) {
        return new ImageTypeSpecifier.Banded(colorSpace,
                                             bankIndices,
                                             bandOffsets,
                                             dataType,
                                             hasAlpha,
                                             isAlphaPremultiplied);
    }

    // Grayscale

    static class Grayscale extends ImageTypeSpecifier {
        int bits;
        int dataType;
        boolean isSigned;
        boolean hasAlpha;
        boolean isAlphaPremultiplied;

        public Grayscale(int bits,
                         int dataType,
                         boolean isSigned,
                         boolean hasAlpha,
                         boolean isAlphaPremultiplied)
        {
            if (bits != 1 && bits != 2 && bits != 4 &&
                bits != 8 && bits != 16)
            {
                throw new IllegalArgumentException("Bad value for bits!");
            }
            if (dataType != DataBuffer.TYPE_BYTE &&
                dataType != DataBuffer.TYPE_SHORT &&
                dataType != DataBuffer.TYPE_USHORT)
            {
                throw new IllegalArgumentException
                    ("Bad value for dataType!");
            }
            if (bits > 8 && dataType == DataBuffer.TYPE_BYTE) {
                throw new IllegalArgumentException
                    ("Too many bits for dataType!");
            }

            this.bits = bits;
            this.dataType = dataType;
            this.isSigned = isSigned;
            this.hasAlpha = hasAlpha;
            this.isAlphaPremultiplied = isAlphaPremultiplied;

            ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);

            if ((bits == 8 && dataType == DataBuffer.TYPE_BYTE) ||
                (bits == 16 &&
                 (dataType == DataBuffer.TYPE_SHORT ||
                  dataType == DataBuffer.TYPE_USHORT))) {
                // Use component color model & sample model

                int numBands = hasAlpha ? 2 : 1;
                int transparency =
                    hasAlpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE;


                int[] nBits = new int[numBands];
                nBits[0] = bits;
                if (numBands == 2) {
                    nBits[1] = bits;
                }
                this.colorModel =
                    new ComponentColorModel(colorSpace,
                                            nBits,
                                            hasAlpha,
                                            isAlphaPremultiplied,
                                            transparency,
                                            dataType);

                int[] bandOffsets = new int[numBands];
                bandOffsets[0] = 0;
                if (numBands == 2) {
                    bandOffsets[1] = 1;
                }

                int w = 1;
                int h = 1;
                this.sampleModel =
                    new PixelInterleavedSampleModel(dataType,
                                                    w, h,
                                                    numBands, w*numBands,
                                                    bandOffsets);
            } else {
                int numEntries = 1 << bits;
                byte[] arr = new byte[numEntries];
                for (int i = 0; i < numEntries; i++) {
                    arr[i] = (byte)(i*255/(numEntries - 1));
                }
                this.colorModel =
                    new IndexColorModel(bits, numEntries, arr, arr, arr);

                this.sampleModel =
                    new MultiPixelPackedSampleModel(dataType, 1, 1, bits);
            }
        }
    }

    /**
     * Returns a specifier for a grayscale image format that will pack
     * pixels of the given bit depth into array elements of
     * the specified data type.
     *
     * @param bits the number of bits per gray value (1, 2, 4, 8, or 16).
     * @param dataType the desired data type, as one of the enumerations
     * from the {@code DataBuffer} class.
     * @param isSigned {@code true} if negative values are to
     * be represented.
     *
     * @return an {@code ImageTypeSpecifier} with the desired
     * characteristics.
     *
     * @exception IllegalArgumentException if {@code bits} is
     * not one of 1, 2, 4, 8, or 16.
     * @exception IllegalArgumentException if {@code dataType} is
     * not one of {@code DataBuffer.TYPE_BYTE},
     * {@code DataBuffer.TYPE_SHORT}, or
     * {@code DataBuffer.TYPE_USHORT}.
     * @exception IllegalArgumentException if {@code bits} is
     * larger than the bit size of the given {@code dataType}.
     */
    public static ImageTypeSpecifier
        createGrayscale(int bits,
                        int dataType,
                        boolean isSigned) {
        return new ImageTypeSpecifier.Grayscale(bits,
                                                dataType,
                                                isSigned,
                                                false,
                                                false);
    }

    /**
     * Returns a specifier for a grayscale plus alpha image format
     * that will pack pixels of the given bit depth into array
     * elements of the specified data type.
     *
     * @param bits the number of bits per gray value (1, 2, 4, 8, or 16).
     * @param dataType the desired data type, as one of the enumerations
     * from the {@code DataBuffer} class.
     * @param isSigned {@code true} if negative values are to
     * be represented.
     * @param isAlphaPremultiplied {@code true} if the luminance channel
     * will be premultipled by the alpha channel.
     *
     * @return an {@code ImageTypeSpecifier} with the desired
     * characteristics.
     *
     * @exception IllegalArgumentException if {@code bits} is
     * not one of 1, 2, 4, 8, or 16.
     * @exception IllegalArgumentException if {@code dataType} is
     * not one of {@code DataBuffer.TYPE_BYTE},
     * {@code DataBuffer.TYPE_SHORT}, or
     * {@code DataBuffer.TYPE_USHORT}.
     * @exception IllegalArgumentException if {@code bits} is
     * larger than the bit size of the given {@code dataType}.
     */
    public static ImageTypeSpecifier
        createGrayscale(int bits,
                        int dataType,
                        boolean isSigned,
                        boolean isAlphaPremultiplied) {
        return new ImageTypeSpecifier.Grayscale(bits,
                                                dataType,
                                                isSigned,
                                                true,
                                                isAlphaPremultiplied);
    }

    // Indexed

    static class Indexed extends ImageTypeSpecifier {
        byte[] redLUT;
        byte[] greenLUT;
        byte[] blueLUT;
        byte[] alphaLUT = null;
        int bits;
        int dataType;

        public Indexed(byte[] redLUT,
                       byte[] greenLUT,
                       byte[] blueLUT,
                       byte[] alphaLUT,
                       int bits,
                       int dataType) {
            if (redLUT == null || greenLUT == null || blueLUT == null) {
                throw new IllegalArgumentException("LUT is null!");
            }
            if (bits != 1 && bits != 2 && bits != 4 &&
                bits != 8 && bits != 16) {
                throw new IllegalArgumentException("Bad value for bits!");
            }
            if (dataType != DataBuffer.TYPE_BYTE &&
                dataType != DataBuffer.TYPE_SHORT &&
                dataType != DataBuffer.TYPE_USHORT &&
                dataType != DataBuffer.TYPE_INT) {
                throw new IllegalArgumentException
                    ("Bad value for dataType!");
            }
            if ((bits > 8 && dataType == DataBuffer.TYPE_BYTE) ||
                (bits > 16 && dataType != DataBuffer.TYPE_INT)) {
                throw new IllegalArgumentException
                    ("Too many bits for dataType!");
            }

            int len = 1 << bits;
            if (redLUT.length != len ||
                greenLUT.length != len ||
                blueLUT.length != len ||
                (alphaLUT != null && alphaLUT.length != len)) {
                throw new IllegalArgumentException("LUT has improper length!");
            }
            this.redLUT = redLUT.clone();
            this.greenLUT = greenLUT.clone();
            this.blueLUT = blueLUT.clone();
            if (alphaLUT != null) {
                this.alphaLUT = alphaLUT.clone();
            }
            this.bits = bits;
            this.dataType = dataType;

            if (alphaLUT == null) {
                this.colorModel = new IndexColorModel(bits,
                                                      redLUT.length,
                                                      redLUT,
                                                      greenLUT,
                                                      blueLUT);
            } else {
                this.colorModel = new IndexColorModel(bits,
                                                      redLUT.length,
                                                      redLUT,
                                                      greenLUT,
                                                      blueLUT,
                                                      alphaLUT);
            }

            if ((bits == 8 && dataType == DataBuffer.TYPE_BYTE) ||
                (bits == 16 &&
                 (dataType == DataBuffer.TYPE_SHORT ||
                  dataType == DataBuffer.TYPE_USHORT))) {
                int[] bandOffsets = { 0 };
                this.sampleModel =
                    new PixelInterleavedSampleModel(dataType,
                                                    1, 1, 1, 1,
                                                    bandOffsets);
            } else {
                this.sampleModel =
                    new MultiPixelPackedSampleModel(dataType, 1, 1, bits);
            }
        }
    }

    /**
     * Returns a specifier for an indexed-color image format that will pack
     * index values of the given bit depth into array elements of
     * the specified data type.
     *
     * @param redLUT an array of {@code byte}s containing
     * the red values for each index.
     * @param greenLUT an array of {@code byte}s containing * the
     *  green values for each index.
     * @param blueLUT an array of {@code byte}s containing the
     * blue values for each index.
     * @param alphaLUT an array of {@code byte}s containing the
     * alpha values for each index, or {@code null} to create a
     * fully opaque LUT.
     * @param bits the number of bits in each index.
     * @param dataType the desired output type, as one of the enumerations
     * from the {@code DataBuffer} class.
     *
     * @return an {@code ImageTypeSpecifier} with the desired
     * characteristics.
     *
     * @exception IllegalArgumentException if {@code redLUT} is
     * {@code null}.
     * @exception IllegalArgumentException if {@code greenLUT} is
     * {@code null}.
     * @exception IllegalArgumentException if {@code blueLUT} is
     * {@code null}.
     * @exception IllegalArgumentException if {@code bits} is
     * not one of 1, 2, 4, 8, or 16.
     * @exception IllegalArgumentException if the
     * non-{@code null} LUT parameters do not have lengths of
     * exactly {@code 1 << bits}.
     * @exception IllegalArgumentException if {@code dataType} is
     * not one of {@code DataBuffer.TYPE_BYTE},
     * {@code DataBuffer.TYPE_SHORT},
     * {@code DataBuffer.TYPE_USHORT},
     * or {@code DataBuffer.TYPE_INT}.
     * @exception IllegalArgumentException if {@code bits} is
     * larger than the bit size of the given {@code dataType}.
     */
    public static ImageTypeSpecifier
        createIndexed(byte[] redLUT,
                      byte[] greenLUT,
                      byte[] blueLUT,
                      byte[] alphaLUT,
                      int bits,
                      int dataType) {
        return new ImageTypeSpecifier.Indexed(redLUT,
                                              greenLUT,
                                              blueLUT,
                                              alphaLUT,
                                              bits,
                                              dataType);
    }

    /**
     * Returns an {@code ImageTypeSpecifier} that encodes
     * one of the standard {@code BufferedImage} types
     * (other than {@code TYPE_CUSTOM}).
     *
     * @param bufferedImageType an int representing one of the standard
     * {@code BufferedImage} types.
     *
     * @return an {@code ImageTypeSpecifier} with the desired
     * characteristics.
     *
     * @exception IllegalArgumentException if
     * {@code bufferedImageType} is not one of the standard
     * types, or is equal to {@code TYPE_CUSTOM}.
     *
     * @see java.awt.image.BufferedImage
     * @see java.awt.image.BufferedImage#TYPE_INT_RGB
     * @see java.awt.image.BufferedImage#TYPE_INT_ARGB
     * @see java.awt.image.BufferedImage#TYPE_INT_ARGB_PRE
     * @see java.awt.image.BufferedImage#TYPE_INT_BGR
     * @see java.awt.image.BufferedImage#TYPE_3BYTE_BGR
     * @see java.awt.image.BufferedImage#TYPE_4BYTE_ABGR
     * @see java.awt.image.BufferedImage#TYPE_4BYTE_ABGR_PRE
     * @see java.awt.image.BufferedImage#TYPE_USHORT_565_RGB
     * @see java.awt.image.BufferedImage#TYPE_USHORT_555_RGB
     * @see java.awt.image.BufferedImage#TYPE_BYTE_GRAY
     * @see java.awt.image.BufferedImage#TYPE_USHORT_GRAY
     * @see java.awt.image.BufferedImage#TYPE_BYTE_BINARY
     * @see java.awt.image.BufferedImage#TYPE_BYTE_INDEXED
     */
    public static
        ImageTypeSpecifier createFromBufferedImageType(int bufferedImageType) {
        if (bufferedImageType >= BufferedImage.TYPE_INT_RGB &&
            bufferedImageType <= BufferedImage.TYPE_BYTE_INDEXED) {
            return getSpecifier(bufferedImageType);
        } else if (bufferedImageType == BufferedImage.TYPE_CUSTOM) {
            throw new IllegalArgumentException("Cannot create from TYPE_CUSTOM!");
        } else {
            throw new IllegalArgumentException("Invalid BufferedImage type!");
        }
    }

    /**
     * Returns an {@code ImageTypeSpecifier} that encodes the
     * layout of a {@code RenderedImage} (which may be a
     * {@code BufferedImage}).
     *
     * @param image a {@code RenderedImage}.
     *
     * @return an {@code ImageTypeSpecifier} with the desired
     * characteristics.
     *
     * @exception IllegalArgumentException if {@code image} is
     * {@code null}.
     */
    public static
        ImageTypeSpecifier createFromRenderedImage(RenderedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("image == null!");
        }

        if (image instanceof BufferedImage) {
            int bufferedImageType = ((BufferedImage)image).getType();
            if (bufferedImageType != BufferedImage.TYPE_CUSTOM) {
                return getSpecifier(bufferedImageType);
            }
        }

        return new ImageTypeSpecifier(image);
    }

    /**
     * Returns an int containing one of the enumerated constant values
     * describing image formats from {@code BufferedImage}.
     *
     * @return an {@code int} representing a
     * {@code BufferedImage} type.
     *
     * @see java.awt.image.BufferedImage
     * @see java.awt.image.BufferedImage#TYPE_CUSTOM
     * @see java.awt.image.BufferedImage#TYPE_INT_RGB
     * @see java.awt.image.BufferedImage#TYPE_INT_ARGB
     * @see java.awt.image.BufferedImage#TYPE_INT_ARGB_PRE
     * @see java.awt.image.BufferedImage#TYPE_INT_BGR
     * @see java.awt.image.BufferedImage#TYPE_3BYTE_BGR
     * @see java.awt.image.BufferedImage#TYPE_4BYTE_ABGR
     * @see java.awt.image.BufferedImage#TYPE_4BYTE_ABGR_PRE
     * @see java.awt.image.BufferedImage#TYPE_USHORT_565_RGB
     * @see java.awt.image.BufferedImage#TYPE_USHORT_555_RGB
     * @see java.awt.image.BufferedImage#TYPE_BYTE_GRAY
     * @see java.awt.image.BufferedImage#TYPE_USHORT_GRAY
     * @see java.awt.image.BufferedImage#TYPE_BYTE_BINARY
     * @see java.awt.image.BufferedImage#TYPE_BYTE_INDEXED
     */
    public int getBufferedImageType() {
        BufferedImage bi = createBufferedImage(1, 1);
        return bi.getType();
    }

    /**
     * Return the number of color components
     * specified by this object.  This is the same value as returned by
     * {@code ColorModel.getNumComponents}
     *
     * @return the number of components in the image.
     */
    public int getNumComponents() {
        return colorModel.getNumComponents();
    }

    /**
     * Return the number of bands
     * specified by this object.  This is the same value as returned by
     * {@code SampleModel.getNumBands}
     *
     * @return the number of bands in the image.
     */
    public int getNumBands() {
        return sampleModel.getNumBands();
    }

    /**
     * Return the number of bits used to represent samples of the given band.
     *
     * @param band the index of the band to be queried, as an
     * int.
     *
     * @return an int specifying a number of bits.
     *
     * @exception IllegalArgumentException if {@code band} is
     * negative or greater than the largest band index.
     */
    public int getBitsPerBand(int band) {
        if (band < 0 || band >= getNumBands()) {
            throw new IllegalArgumentException("band out of range!");
        }
        return sampleModel.getSampleSize(band);
    }

    /**
     * Returns a {@code SampleModel} based on the settings
     * encapsulated within this object.  The width and height of the
     * {@code SampleModel} will be set to arbitrary values.
     *
     * @return a {@code SampleModel} with arbitrary dimensions.
     */
    public SampleModel getSampleModel() {
        return sampleModel;
    }

    /**
     * Returns a {@code SampleModel} based on the settings
     * encapsulated within this object.  The width and height of the
     * {@code SampleModel} will be set to the supplied values.
     *
     * @param width the desired width of the returned {@code SampleModel}.
     * @param height the desired height of the returned
     * {@code SampleModel}.
     *
     * @return a {@code SampleModel} with the given dimensions.
     *
     * @exception IllegalArgumentException if either {@code width} or
     * {@code height} are negative or zero.
     * @exception IllegalArgumentException if the product of
     * {@code width} and {@code height} is greater than
     * {@code Integer.MAX_VALUE}
     */
    public SampleModel getSampleModel(int width, int height) {
        if ((long)width*height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                ("width*height > Integer.MAX_VALUE!");
        }
        return sampleModel.createCompatibleSampleModel(width, height);
    }

    /**
     * Returns the {@code ColorModel} specified by this object.
     *
     * @return a {@code ColorModel}.
     */
    public ColorModel getColorModel() {
        return colorModel;
    }

    /**
     * Creates a {@code BufferedImage} with a given width and
     * height according to the specification embodied in this object.
     *
     * @param width the desired width of the returned
     * {@code BufferedImage}.
     * @param height the desired height of the returned
     * {@code BufferedImage}.
     *
     * @return a new {@code BufferedImage}
     *
     * @exception IllegalArgumentException if either {@code width} or
     * {@code height} are negative or zero.
     * @exception IllegalArgumentException if the product of
     * {@code width} and {@code height} is greater than
     * {@code Integer.MAX_VALUE}, or if the number of array
     * elements needed to store the image is greater than
     * {@code Integer.MAX_VALUE}.
     */
    public BufferedImage createBufferedImage(int width, int height) {
        try {
            SampleModel sampleModel = getSampleModel(width, height);
            WritableRaster raster =
                Raster.createWritableRaster(sampleModel,
                                            new Point(0, 0));
            return new BufferedImage(colorModel, raster,
                                     colorModel.isAlphaPremultiplied(),
                                     new Hashtable<>());
        } catch (NegativeArraySizeException e) {
            // Exception most likely thrown from a DataBuffer constructor
            throw new IllegalArgumentException
                ("Array size > Integer.MAX_VALUE!");
        }
    }

    /**
     * Returns {@code true} if the given {@code Object} is
     * an {@code ImageTypeSpecifier} and has a
     * {@code SampleModel} and {@code ColorModel} that are
     * equal to those of this object.
     *
     * @param o the {@code Object} to be compared for equality.
     *
     * @return {@code true} if the given object is an equivalent
     * {@code ImageTypeSpecifier}.
     */
    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof ImageTypeSpecifier)) {
            return false;
        }

        ImageTypeSpecifier that = (ImageTypeSpecifier)o;
        return (colorModel.equals(that.colorModel)) &&
            (sampleModel.equals(that.sampleModel));
    }

    /**
     * Returns the hash code for this ImageTypeSpecifier.
     *
     * @return a hash code for this ImageTypeSpecifier
     */
    public int hashCode() {
        return (9 * colorModel.hashCode()) + (14 * sampleModel.hashCode());
    }

    private static ImageTypeSpecifier getSpecifier(int type) {
        if (BISpecifier[type] == null) {
            BISpecifier[type] = createSpecifier(type);
        }
        return BISpecifier[type];
    }

    private static ImageTypeSpecifier createSpecifier(int type) {
        switch(type) {
          case BufferedImage.TYPE_INT_RGB:
              return createPacked(sRGB,
                                  0x00ff0000,
                                  0x0000ff00,
                                  0x000000ff,
                                  0x0,
                                  DataBuffer.TYPE_INT,
                                  false);

          case BufferedImage.TYPE_INT_ARGB:
              return createPacked(sRGB,
                                  0x00ff0000,
                                  0x0000ff00,
                                  0x000000ff,
                                  0xff000000,
                                  DataBuffer.TYPE_INT,
                                  false);

          case BufferedImage.TYPE_INT_ARGB_PRE:
              return createPacked(sRGB,
                                  0x00ff0000,
                                  0x0000ff00,
                                  0x000000ff,
                                  0xff000000,
                                  DataBuffer.TYPE_INT,
                                  true);

          case BufferedImage.TYPE_INT_BGR:
              return createPacked(sRGB,
                                  0x000000ff,
                                  0x0000ff00,
                                  0x00ff0000,
                                  0x0,
                                  DataBuffer.TYPE_INT,
                                  false);

          case BufferedImage.TYPE_3BYTE_BGR:
              return createInterleaved(sRGB,
                                       new int[] { 2, 1, 0 },
                                       DataBuffer.TYPE_BYTE,
                                       false,
                                       false);

          case BufferedImage.TYPE_4BYTE_ABGR:
              return createInterleaved(sRGB,
                                       new int[] { 3, 2, 1, 0 },
                                       DataBuffer.TYPE_BYTE,
                                       true,
                                       false);

          case BufferedImage.TYPE_4BYTE_ABGR_PRE:
              return createInterleaved(sRGB,
                                       new int[] { 3, 2, 1, 0 },
                                       DataBuffer.TYPE_BYTE,
                                       true,
                                       true);

          case BufferedImage.TYPE_USHORT_565_RGB:
              return createPacked(sRGB,
                                  0xF800,
                                  0x07E0,
                                  0x001F,
                                  0x0,
                                  DataBuffer.TYPE_USHORT,
                                  false);

          case BufferedImage.TYPE_USHORT_555_RGB:
              return createPacked(sRGB,
                                  0x7C00,
                                  0x03E0,
                                  0x001F,
                                  0x0,
                                  DataBuffer.TYPE_USHORT,
                                  false);

          case BufferedImage.TYPE_BYTE_GRAY:
            return createGrayscale(8,
                                   DataBuffer.TYPE_BYTE,
                                   false);

          case BufferedImage.TYPE_USHORT_GRAY:
            return createGrayscale(16,
                                   DataBuffer.TYPE_USHORT,
                                   false);

          case BufferedImage.TYPE_BYTE_BINARY:
              return createGrayscale(1,
                                     DataBuffer.TYPE_BYTE,
                                     false);

          case BufferedImage.TYPE_BYTE_INDEXED:
          {

              BufferedImage bi =
                  new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_INDEXED);
              IndexColorModel icm = (IndexColorModel)bi.getColorModel();
              int mapSize = icm.getMapSize();
              byte[] redLUT = new byte[mapSize];
              byte[] greenLUT = new byte[mapSize];
              byte[] blueLUT = new byte[mapSize];
              byte[] alphaLUT = new byte[mapSize];

              icm.getReds(redLUT);
              icm.getGreens(greenLUT);
              icm.getBlues(blueLUT);
              icm.getAlphas(alphaLUT);

              return createIndexed(redLUT, greenLUT, blueLUT, alphaLUT,
                                   8,
                                   DataBuffer.TYPE_BYTE);
          }
          default:
              throw new IllegalArgumentException("Invalid BufferedImage type!");
        }
    }

}
