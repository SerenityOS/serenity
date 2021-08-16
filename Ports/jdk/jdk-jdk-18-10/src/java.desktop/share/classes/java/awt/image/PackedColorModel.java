/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.image;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.util.Arrays;

/**
 * The {@code PackedColorModel} class is an abstract
 * {@link ColorModel} class that works with pixel values which represent
 * color and alpha information as separate samples and which pack all
 * samples for a single pixel into a single int, short, or byte quantity.
 * This class can be used with an arbitrary {@link ColorSpace}.  The number of
 * color samples in the pixel values must be the same as the number of color
 * components in the {@code ColorSpace}.  There can be a single alpha
 * sample.  The array length is always 1 for those methods that use a
 * primitive array pixel representation of type {@code transferType}.
 * The transfer types supported are DataBuffer.TYPE_BYTE,
 * DataBuffer.TYPE_USHORT, and DataBuffer.TYPE_INT.
 * Color and alpha samples are stored in the single element of the array
 * in bits indicated by bit masks.  Each bit mask must be contiguous and
 * masks must not overlap.  The same masks apply to the single int
 * pixel representation used by other methods.  The correspondence of
 * masks and color/alpha samples is as follows:
 * <ul>
 * <li> Masks are identified by indices running from 0 through
 * {@link ColorModel#getNumComponents() getNumComponents}&nbsp;-&nbsp;1.
 * <li> The first
 * {@link ColorModel#getNumColorComponents() getNumColorComponents}
 * indices refer to color samples.
 * <li> If an alpha sample is present, it corresponds the last index.
 * <li> The order of the color indices is specified
 * by the {@code ColorSpace}.  Typically, this reflects the name of
 * the color space type (for example, TYPE_RGB), index 0
 * corresponds to red, index 1 to green, and index 2 to blue.
 * </ul>
 * <p>
 * The translation from pixel values to color/alpha components for
 * display or processing purposes is a one-to-one correspondence of
 * samples to components.
 * A {@code PackedColorModel} is typically used with image data
 * that uses masks to define packed samples.  For example, a
 * {@code PackedColorModel} can be used in conjunction with a
 * {@link SinglePixelPackedSampleModel} to construct a
 * {@link BufferedImage}.  Normally the masks used by the
 * {@link SampleModel} and the {@code ColorModel} would be the same.
 * However, if they are different, the color interpretation of pixel data is
 * done according to the masks of the {@code ColorModel}.
 * <p>
 * A single {@code int} pixel representation is valid for all objects
 * of this class since it is always possible to represent pixel values
 * used with this class in a single {@code int}.  Therefore, methods
 * that use this representation do not throw an
 * {@code IllegalArgumentException} due to an invalid pixel value.
 * <p>
 * A subclass of {@code PackedColorModel} is {@link DirectColorModel},
 * which is similar to an X11 TrueColor visual.
 *
 * @see DirectColorModel
 * @see SinglePixelPackedSampleModel
 * @see BufferedImage
 */

public abstract class PackedColorModel extends ColorModel {
    int[] maskArray;
    int[] maskOffsets;
    float[] scaleFactors;
    private volatile int hashCode;

    /**
     * Constructs a {@code PackedColorModel} from a color mask array,
     * which specifies which bits in an {@code int} pixel representation
     * contain each of the color samples, and an alpha mask.  Color
     * components are in the specified {@code ColorSpace}.  The length of
     * {@code colorMaskArray} should be the number of components in
     * the {@code ColorSpace}.  All of the bits in each mask
     * must be contiguous and fit in the specified number of least significant
     * bits of an {@code int} pixel representation.  If the
     * {@code alphaMask} is 0, there is no alpha.  If there is alpha,
     * the {@code boolean isAlphaPremultiplied} specifies
     * how to interpret color and alpha samples in pixel values.  If the
     * {@code boolean} is {@code true}, color samples are assumed
     * to have been multiplied by the alpha sample.  The transparency,
     * {@code trans}, specifies what alpha values can be represented
     * by this color model.  The transfer type is the type of primitive
     * array used to represent pixel values.
     * @param space the specified {@code ColorSpace}
     * @param bits the number of bits in the pixel values
     * @param colorMaskArray array that specifies the masks representing
     *         the bits of the pixel values that represent the color
     *         components
     * @param alphaMask specifies the mask representing
     *         the bits of the pixel values that represent the alpha
     *         component
     * @param isAlphaPremultiplied {@code true} if color samples are
     *        premultiplied by the alpha sample; {@code false} otherwise
     * @param trans specifies the alpha value that can be represented by
     *        this color model
     * @param transferType the type of array used to represent pixel values
     * @throws IllegalArgumentException if {@code bits} is less than
     *         1 or greater than 32
     */
    public PackedColorModel (ColorSpace space, int bits,
                             int[] colorMaskArray, int alphaMask,
                             boolean isAlphaPremultiplied,
                             int trans, int transferType) {
        super(bits, PackedColorModel.createBitsArray(colorMaskArray,
                                                     alphaMask),
              space, (alphaMask == 0 ? false : true),
              isAlphaPremultiplied, trans, transferType);
        if (bits < 1 || bits > 32) {
            throw new IllegalArgumentException("Number of bits must be between"
                                               +" 1 and 32.");
        }
        maskArray   = new int[numComponents];
        maskOffsets = new int[numComponents];
        scaleFactors = new float[numComponents];

        for (int i=0; i < numColorComponents; i++) {
            // Get the mask offset and #bits
            DecomposeMask(colorMaskArray[i], i, space.getName(i));
        }
        if (alphaMask != 0) {
            DecomposeMask(alphaMask, numColorComponents, "alpha");
            if (nBits[numComponents-1] == 1) {
                transparency = Transparency.BITMASK;
            }
        }
    }

    /**
     * Constructs a {@code PackedColorModel} from the specified
     * masks which indicate which bits in an {@code int} pixel
     * representation contain the alpha, red, green and blue color samples.
     * Color components are in the specified {@code ColorSpace}, which
     * must be of type ColorSpace.TYPE_RGB.  All of the bits in each
     * mask must be contiguous and fit in the specified number of
     * least significant bits of an {@code int} pixel representation.  If
     * {@code amask} is 0, there is no alpha.  If there is alpha,
     * the {@code boolean isAlphaPremultiplied}
     * specifies how to interpret color and alpha samples
     * in pixel values.  If the {@code boolean} is {@code true},
     * color samples are assumed to have been multiplied by the alpha sample.
     * The transparency, {@code trans}, specifies what alpha values
     * can be represented by this color model.
     * The transfer type is the type of primitive array used to represent
     * pixel values.
     * @param space the specified {@code ColorSpace}
     * @param bits the number of bits in the pixel values
     * @param rmask specifies the mask representing
     *         the bits of the pixel values that represent the red
     *         color component
     * @param gmask specifies the mask representing
     *         the bits of the pixel values that represent the green
     *         color component
     * @param bmask specifies the mask representing
     *         the bits of the pixel values that represent
     *         the blue color component
     * @param amask specifies the mask representing
     *         the bits of the pixel values that represent
     *         the alpha component
     * @param isAlphaPremultiplied {@code true} if color samples are
     *        premultiplied by the alpha sample; {@code false} otherwise
     * @param trans specifies the alpha value that can be represented by
     *        this color model
     * @param transferType the type of array used to represent pixel values
     * @throws IllegalArgumentException if {@code space} is not a
     *         TYPE_RGB space
     * @see ColorSpace
     */
    public PackedColorModel(ColorSpace space, int bits, int rmask, int gmask,
                            int bmask, int amask,
                            boolean isAlphaPremultiplied,
                            int trans, int transferType) {
        super (bits, PackedColorModel.createBitsArray(rmask, gmask, bmask,
                                                      amask),
               space, (amask == 0 ? false : true),
               isAlphaPremultiplied, trans, transferType);

        if (space.getType() != ColorSpace.TYPE_RGB) {
            throw new IllegalArgumentException("ColorSpace must be TYPE_RGB.");
        }
        maskArray = new int[numComponents];
        maskOffsets = new int[numComponents];
        scaleFactors = new float[numComponents];

        DecomposeMask(rmask, 0, "red");

        DecomposeMask(gmask, 1, "green");

        DecomposeMask(bmask, 2, "blue");

        if (amask != 0) {
            DecomposeMask(amask, 3, "alpha");
            if (nBits[3] == 1) {
                transparency = Transparency.BITMASK;
            }
        }
    }

    /**
     * Returns the mask indicating which bits in a pixel
     * contain the specified color/alpha sample.  For color
     * samples, {@code index} corresponds to the placement of color
     * sample names in the color space.  Thus, an {@code index}
     * equal to 0 for a CMYK ColorSpace would correspond to
     * Cyan and an {@code index} equal to 1 would correspond to
     * Magenta.  If there is alpha, the alpha {@code index} would be:
     * <pre>
     *      alphaIndex = numComponents() - 1;
     * </pre>
     * @param index the specified color or alpha sample
     * @return the mask, which indicates which bits of the {@code int}
     *         pixel representation contain the color or alpha sample specified
     *         by {@code index}.
     * @throws ArrayIndexOutOfBoundsException if {@code index} is
     *         greater than the number of components minus 1 in this
     *         {@code PackedColorModel} or if {@code index} is
     *         less than zero
     */
    public final int getMask(int index) {
        return maskArray[index];
    }

    /**
     * Returns a mask array indicating which bits in a pixel
     * contain the color and alpha samples.
     * @return the mask array , which indicates which bits of the
     *         {@code int} pixel
     *         representation contain the color or alpha samples.
     */
    public final int[] getMasks() {
        return maskArray.clone();
    }

    /*
     * A utility function to compute the mask offset and scalefactor,
     * store these and the mask in instance arrays, and verify that
     * the mask fits in the specified pixel size.
     */
    private void DecomposeMask(int mask,  int idx, String componentName) {
        int off = 0;
        int count = nBits[idx];

        // Store the mask
        maskArray[idx]   = mask;

        // Now find the shift
        if (mask != 0) {
            while ((mask & 1) == 0) {
                mask >>>= 1;
                off++;
            }
        }

        if (off + count > pixel_bits) {
            throw new IllegalArgumentException(componentName + " mask "+
                                        Integer.toHexString(maskArray[idx])+
                                               " overflows pixel (expecting "+
                                               pixel_bits+" bits");
        }

        maskOffsets[idx] = off;
        if (count == 0) {
            // High enough to scale any 0-ff value down to 0.0, but not
            // high enough to get Infinity when scaling back to pixel bits
            scaleFactors[idx] = 256.0f;
        } else {
            scaleFactors[idx] = 255.0f / ((1 << count) - 1);
        }

    }

    /**
     * Creates a {@code SampleModel} with the specified width and
     * height that has a data layout compatible with this
     * {@code ColorModel}.
     * @param w the width (in pixels) of the region of the image data
     *          described
     * @param h the height (in pixels) of the region of the image data
     *          described
     * @return the newly created {@code SampleModel}.
     * @throws IllegalArgumentException if {@code w} or
     *         {@code h} is not greater than 0
     * @see SampleModel
     */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        return new SinglePixelPackedSampleModel(transferType, w, h,
                                                maskArray);
    }

    /**
     * Checks if the specified {@code SampleModel} is compatible
     * with this {@code ColorModel}.  If {@code sm} is
     * {@code null}, this method returns {@code false}.
     * @param sm the specified {@code SampleModel},
     * or {@code null}
     * @return {@code true} if the specified {@code SampleModel}
     *         is compatible with this {@code ColorModel};
     *         {@code false} otherwise.
     * @see SampleModel
     */
    public boolean isCompatibleSampleModel(SampleModel sm) {
        if (! (sm instanceof SinglePixelPackedSampleModel)) {
            return false;
        }

        // Must have the same number of components
        if (numComponents != sm.getNumBands()) {
            return false;
        }

        // Transfer type must be the same
        if (sm.getTransferType() != transferType) {
            return false;
        }

        SinglePixelPackedSampleModel sppsm = (SinglePixelPackedSampleModel) sm;
        // Now compare the specific masks
        int[] bitMasks = sppsm.getBitMasks();
        if (bitMasks.length != maskArray.length) {
            return false;
        }

        /* compare 'effective' masks only, i.e. only part of the mask
         * which fits the capacity of the transfer type.
         */
        int maxMask = (int)((1L << DataBuffer.getDataTypeSize(transferType)) - 1);
        for (int i=0; i < bitMasks.length; i++) {
            if ((maxMask & bitMasks[i]) != (maxMask & maskArray[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a {@link WritableRaster} representing the alpha channel of
     * an image, extracted from the input {@code WritableRaster}.
     * This method assumes that {@code WritableRaster} objects
     * associated with this {@code ColorModel} store the alpha band,
     * if present, as the last band of image data.  Returns {@code null}
     * if there is no separate spatial alpha channel associated with this
     * {@code ColorModel}.  This method creates a new
     * {@code WritableRaster}, but shares the data array.
     * @param raster a {@code WritableRaster} containing an image
     * @return a {@code WritableRaster} that represents the alpha
     *         channel of the image contained in {@code raster}.
     */
    public WritableRaster getAlphaRaster(WritableRaster raster) {
        if (hasAlpha() == false) {
            return null;
        }

        int x = raster.getMinX();
        int y = raster.getMinY();
        int[] band = new int[1];
        band[0] = raster.getNumBands() - 1;
        return raster.createWritableChild(x, y, raster.getWidth(),
                                          raster.getHeight(), x, y,
                                          band);
    }

    /**
     * Tests if the specified {@code Object} is an instance
     * of {@code PackedColorModel} and equals this
     * {@code PackedColorModel}.
     * @param obj the {@code Object} to test for equality
     * @return {@code true} if the specified {@code Object}
     * is an instance of {@code PackedColorModel} and equals this
     * {@code PackedColorModel}; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PackedColorModel)) {
            return false;
        }

        PackedColorModel cm = (PackedColorModel) obj;

        if (supportsAlpha != cm.hasAlpha() ||
            isAlphaPremultiplied != cm.isAlphaPremultiplied() ||
            pixel_bits != cm.getPixelSize() ||
            transparency != cm.getTransparency() ||
            numComponents != cm.getNumComponents() ||
            (!(colorSpace.equals(cm.colorSpace))) ||
            transferType != cm.transferType)
        {
            return false;
        }

        int numC = cm.getNumComponents();
        for(int i=0; i < numC; i++) {
            if (maskArray[i] != cm.getMask(i)) {
                return false;
            }
        }

        if (!(Arrays.equals(nBits, cm.getComponentSize()))) {
            return false;
        }

        return true;
    }

    /**
     * Returns the hash code for this PackedColorModel.
     *
     * @return    a hash code for this PackedColorModel.
     */
    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = 7;
            result = 89 * result + this.pixel_bits;
            result = 89 * result + Arrays.hashCode(this.nBits);
            result = 89 * result + this.transparency;
            result = 89 * result + (this.supportsAlpha ? 1 : 0);
            result = 89 * result + (this.isAlphaPremultiplied ? 1 : 0);
            result = 89 * result + this.numComponents;
            result = 89 * result + this.colorSpace.hashCode();
            result = 89 * result + this.transferType;
            result = 89 * result + Arrays.hashCode(this.maskArray);
            hashCode = result;
        }
        return result;
    }

    private static final int[] createBitsArray(int[]colorMaskArray,
                                               int alphaMask) {
        int numColors = colorMaskArray.length;
        int numAlpha = (alphaMask == 0 ? 0 : 1);
        int[] arr = new int[numColors+numAlpha];
        for (int i=0; i < numColors; i++) {
            arr[i] = countBits(colorMaskArray[i]);
            if (arr[i] < 0) {
                throw new IllegalArgumentException("Noncontiguous color mask ("
                                     + Integer.toHexString(colorMaskArray[i])+
                                     "at index "+i);
            }
        }
        if (alphaMask != 0) {
            arr[numColors] = countBits(alphaMask);
            if (arr[numColors] < 0) {
                throw new IllegalArgumentException("Noncontiguous alpha mask ("
                                     + Integer.toHexString(alphaMask));
            }
        }
        return arr;
    }

    private static final int[] createBitsArray(int rmask, int gmask, int bmask,
                                         int amask) {
        int[] arr = new int[3 + (amask == 0 ? 0 : 1)];
        arr[0] = countBits(rmask);
        arr[1] = countBits(gmask);
        arr[2] = countBits(bmask);
        if (arr[0] < 0) {
            throw new IllegalArgumentException("Noncontiguous red mask ("
                                     + Integer.toHexString(rmask));
        }
        else if (arr[1] < 0) {
            throw new IllegalArgumentException("Noncontiguous green mask ("
                                     + Integer.toHexString(gmask));
        }
        else if (arr[2] < 0) {
            throw new IllegalArgumentException("Noncontiguous blue mask ("
                                     + Integer.toHexString(bmask));
        }
        if (amask != 0) {
            arr[3] = countBits(amask);
            if (arr[3] < 0) {
                throw new IllegalArgumentException("Noncontiguous alpha mask ("
                                     + Integer.toHexString(amask));
            }
        }
        return arr;
    }

    private static final int countBits(int mask) {
        int count = 0;
        if (mask != 0) {
            while ((mask & 1) == 0) {
                mask >>>= 1;
            }
            while ((mask & 1) == 1) {
                mask >>>= 1;
                count++;
            }
        }
        if (mask != 0) {
            return -1;
        }
        return count;
    }

}