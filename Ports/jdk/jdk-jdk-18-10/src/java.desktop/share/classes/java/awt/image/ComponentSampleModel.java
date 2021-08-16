/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

/* ****************************************************************
 ******************************************************************
 ******************************************************************
 *** COPYRIGHT (c) Eastman Kodak Company, 1997
 *** As  an unpublished  work pursuant to Title 17 of the United
 *** States Code.  All rights reserved.
 ******************************************************************
 ******************************************************************
 ******************************************************************/

package java.awt.image;

import java.util.Arrays;

/**
 *  This class represents image data which is stored such that each sample
 *  of a pixel occupies one data element of the DataBuffer.  It stores the
 *  N samples which make up a pixel in N separate data array elements.
 *  Different bands may be in different banks of the DataBuffer.
 *  Accessor methods are provided so that image data can be manipulated
 *  directly. This class can support different kinds of interleaving, e.g.
 *  band interleaving, scanline interleaving, and pixel interleaving.
 *  Pixel stride is the number of data array elements between two samples
 *  for the same band on the same scanline. Scanline stride is the number
 *  of data array elements between a given sample and the corresponding sample
 *  in the same column of the next scanline.  Band offsets denote the number
 *  of data array elements from the first data array element of the bank
 *  of the DataBuffer holding each band to the first sample of the band.
 *  The bands are numbered from 0 to N-1.  This class can represent image
 *  data for which each sample is an unsigned integral number which can be
 *  stored in 8, 16, or 32 bits (using {@code DataBuffer.TYPE_BYTE},
 *  {@code DataBuffer.TYPE_USHORT}, or {@code DataBuffer.TYPE_INT},
 *  respectively), data for which each sample is a signed integral number
 *  which can be stored in 16 bits (using {@code DataBuffer.TYPE_SHORT}),
 *  or data for which each sample is a signed float or double quantity
 *  (using {@code DataBuffer.TYPE_FLOAT} or
 *  {@code DataBuffer.TYPE_DOUBLE}, respectively).
 *  All samples of a given ComponentSampleModel
 *  are stored with the same precision.  All strides and offsets must be
 *  non-negative.  This class supports
 *  {@link DataBuffer#TYPE_BYTE TYPE_BYTE},
 *  {@link DataBuffer#TYPE_USHORT TYPE_USHORT},
 *  {@link DataBuffer#TYPE_SHORT TYPE_SHORT},
 *  {@link DataBuffer#TYPE_INT TYPE_INT},
 *  {@link DataBuffer#TYPE_FLOAT TYPE_FLOAT},
 *  {@link DataBuffer#TYPE_DOUBLE TYPE_DOUBLE},
 *  @see java.awt.image.PixelInterleavedSampleModel
 *  @see java.awt.image.BandedSampleModel
 */

public class ComponentSampleModel extends SampleModel
{
    /** Offsets for all bands in data array elements. */
    protected int[] bandOffsets;

    /** Index for each bank storing a band of image data. */
    protected int[] bankIndices;

    /**
     * The number of bands in this
     * {@code ComponentSampleModel}.
     */
    protected int numBands = 1;

    /**
     * The number of banks in this
     * {@code ComponentSampleModel}.
     */
    protected int numBanks = 1;

    /**
     *  Line stride (in data array elements) of the region of image
     *  data described by this ComponentSampleModel.
     */
    protected int scanlineStride;

    /** Pixel stride (in data array elements) of the region of image
     *  data described by this ComponentSampleModel.
     */
    protected int pixelStride;

    /**
     * Constructs a ComponentSampleModel with the specified parameters.
     * The number of bands will be given by the length of the bandOffsets array.
     * All bands will be stored in the first bank of the DataBuffer.
     * @param dataType  the data type for storing samples
     * @param w         the width (in pixels) of the region of
     *     image data described
     * @param h         the height (in pixels) of the region of
     *     image data described
     * @param pixelStride the pixel stride of the region of image
     *     data described
     * @param scanlineStride the line stride of the region of image
     *     data described
     * @param bandOffsets the offsets of all bands
     * @throws IllegalArgumentException if {@code w} and {@code h}
     *         are not both greater than 0
     * @throws IllegalArgumentException if the product of {@code w}
     *         and {@code h} is greater than {@code Integer.MAX_VALUE}
     * @throws IllegalArgumentException if {@code pixelStride} is less than 0
     * @throws IllegalArgumentException if {@code scanlineStride} is less than 0
     * @throws NullPointerException if {@code bandOffsets} is {@code null}
     * @throws IllegalArgumentException if {@code bandOffsets.length} is 0
     * @throws IllegalArgumentException if {@code dataType} is not
     *         one of the supported data types for this sample model.
     */
    public ComponentSampleModel(int dataType,
                                int w, int h,
                                int pixelStride,
                                int scanlineStride,
                                int[] bandOffsets) {
        super(dataType, w, h, bandOffsets.length);
        this.dataType = dataType;
        this.pixelStride = pixelStride;
        this.scanlineStride  = scanlineStride;
        this.bandOffsets = bandOffsets.clone();
        numBands = this.bandOffsets.length;
        if (pixelStride < 0) {
            throw new IllegalArgumentException("Pixel stride must be >= 0");
        }
        // TODO - bug 4296691 - remove this check
        if (scanlineStride < 0) {
            throw new IllegalArgumentException("Scanline stride must be >= 0");
        }
        if ((dataType < DataBuffer.TYPE_BYTE) ||
            (dataType > DataBuffer.TYPE_DOUBLE)) {
            throw new IllegalArgumentException("Unsupported dataType.");
        }
        bankIndices = new int[numBands];
        for (int i=0; i<numBands; i++) {
            bankIndices[i] = 0;
        }
        verify();
    }


    /**
     * Constructs a ComponentSampleModel with the specified parameters.
     * The number of bands will be given by the length of the bandOffsets array.
     * Different bands may be stored in different banks of the DataBuffer.
     *
     * @param dataType  the data type for storing samples
     * @param w         the width (in pixels) of the region of
     *     image data described
     * @param h         the height (in pixels) of the region of
     *     image data described
     * @param pixelStride the pixel stride of the region of image
     *     data described
     * @param scanlineStride The line stride of the region of image
     *     data described
     * @param bankIndices the bank indices of all bands
     * @param bandOffsets the band offsets of all bands
     * @throws IllegalArgumentException if {@code w} and {@code h}
     *         are not both greater than 0
     * @throws IllegalArgumentException if the product of {@code w}
     *         and {@code h} is greater than {@code Integer.MAX_VALUE}
     * @throws IllegalArgumentException if {@code pixelStride} is less than 0
     * @throws IllegalArgumentException if {@code scanlineStride} is less than 0
     * @throws NullPointerException if {@code bankIndices} is {@code null}
     * @throws NullPointerException if {@code bandOffsets} is {@code null}
     * @throws IllegalArgumentException if {@code bandOffsets.length} is 0
     * @throws IllegalArgumentException if the length of
     *         {@code bankIndices} does not equal the length of
     *         {@code bandOffsets}
     * @throws IllegalArgumentException if any of the bank indices
     *         of {@code bandIndices} is less than 0
     * @throws IllegalArgumentException if {@code dataType} is not
     *         one of the supported data types for this sample model
     */
    public ComponentSampleModel(int dataType,
                                int w, int h,
                                int pixelStride,
                                int scanlineStride,
                                int[] bankIndices,
                                int[] bandOffsets) {
        super(dataType, w, h, bandOffsets.length);
        this.dataType = dataType;
        this.pixelStride = pixelStride;
        this.scanlineStride  = scanlineStride;
        this.bandOffsets = bandOffsets.clone();
        this.bankIndices = bankIndices.clone();
        if (this.bandOffsets.length != this.bankIndices.length) {
            throw new IllegalArgumentException("Length of bandOffsets must "+
                                               "equal length of bankIndices.");
        }
        if (pixelStride < 0) {
            throw new IllegalArgumentException("Pixel stride must be >= 0");
        }
        // TODO - bug 4296691 - remove this check
        if (scanlineStride < 0) {
            throw new IllegalArgumentException("Scanline stride must be >= 0");
        }
        if ((dataType < DataBuffer.TYPE_BYTE) ||
            (dataType > DataBuffer.TYPE_DOUBLE)) {
            throw new IllegalArgumentException("Unsupported dataType.");
        }
        int maxBank = this.bankIndices[0];
        if (maxBank < 0) {
            throw new IllegalArgumentException("Index of bank 0 is less than "+
                                               "0 ("+maxBank+")");
        }
        for (int i=1; i < this.bankIndices.length; i++) {
            if (this.bankIndices[i] > maxBank) {
                maxBank = this.bankIndices[i];
            }
            else if (this.bankIndices[i] < 0) {
                throw new IllegalArgumentException("Index of bank "+i+
                                                   " is less than 0 ("+
                                                   maxBank+")");
            }
        }
        numBanks         = maxBank+1;
        numBands         = this.bandOffsets.length;
        verify();
    }

    private void verify() {
        int requiredSize = getBufferSize();
    }

    /**
     * Returns the size of the data buffer (in data elements) needed
     * for a data buffer that matches this ComponentSampleModel.
     */
     private int getBufferSize() {
         int maxBandOff=bandOffsets[0];
         for (int i=1; i<bandOffsets.length; i++) {
             maxBandOff = Math.max(maxBandOff,bandOffsets[i]);
         }

         if (maxBandOff < 0 || maxBandOff > (Integer.MAX_VALUE - 1)) {
             throw new IllegalArgumentException("Invalid band offset");
         }

         if (pixelStride < 0 || pixelStride > (Integer.MAX_VALUE / width)) {
             throw new IllegalArgumentException("Invalid pixel stride");
         }

         if (scanlineStride < 0 || scanlineStride > (Integer.MAX_VALUE / height)) {
             throw new IllegalArgumentException("Invalid scanline stride");
         }

         int size = maxBandOff + 1;

         int val = pixelStride * (width - 1);

         if (val > (Integer.MAX_VALUE - size)) {
             throw new IllegalArgumentException("Invalid pixel stride");
         }

         size += val;

         val = scanlineStride * (height - 1);

         if (val > (Integer.MAX_VALUE - size)) {
             throw new IllegalArgumentException("Invalid scan stride");
         }

         size += val;

         return size;
     }

     /**
      * Preserves band ordering with new step factor...
      */
    int []orderBands(int[] orig, int step) {
        int[] map = new int[orig.length];
        int[] ret = new int[orig.length];

        for (int i=0; i<map.length; i++) map[i] = i;

        for (int i = 0; i < ret.length; i++) {
            int index = i;
            for (int j = i+1; j < ret.length; j++) {
                if (orig[map[index]] > orig[map[j]]) {
                    index = j;
                }
            }
            ret[map[index]] = i*step;
            map[index]  = map[i];
        }
        return ret;
    }

    /**
     * Creates a new {@code ComponentSampleModel} with the specified
     * width and height.  The new {@code SampleModel} will have the same
     * number of bands, storage data type, interleaving scheme, and
     * pixel stride as this {@code SampleModel}.
     * @param w the width of the resulting {@code SampleModel}
     * @param h the height of the resulting {@code SampleModel}
     * @return a new {@code ComponentSampleModel} with the specified size
     * @throws IllegalArgumentException if {@code w} or
     *         {@code h} is not greater than 0
     */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        SampleModel ret=null;
        long size;
        int minBandOff=bandOffsets[0];
        int maxBandOff=bandOffsets[0];
        for (int i=1; i<bandOffsets.length; i++) {
            minBandOff = Math.min(minBandOff,bandOffsets[i]);
            maxBandOff = Math.max(maxBandOff,bandOffsets[i]);
        }
        maxBandOff -= minBandOff;

        int bands   = bandOffsets.length;
        int[] bandOff;
        int pStride = Math.abs(pixelStride);
        int lStride = Math.abs(scanlineStride);
        int bStride = Math.abs(maxBandOff);

        if (pStride > lStride) {
            if (pStride > bStride) {
                if (lStride > bStride) { // pix > line > band
                    bandOff = new int[bandOffsets.length];
                    for (int i=0; i<bands; i++)
                        bandOff[i] = bandOffsets[i]-minBandOff;
                    lStride = bStride+1;
                    pStride = lStride*h;
                } else { // pix > band > line
                    bandOff = orderBands(bandOffsets,lStride*h);
                    pStride = bands*lStride*h;
                }
            } else { // band > pix > line
                pStride = lStride*h;
                bandOff = orderBands(bandOffsets,pStride*w);
            }
        } else {
            if (pStride > bStride) { // line > pix > band
                bandOff = new int[bandOffsets.length];
                for (int i=0; i<bands; i++)
                    bandOff[i] = bandOffsets[i]-minBandOff;
                pStride = bStride+1;
                lStride = pStride*w;
            } else {
                if (lStride > bStride) { // line > band > pix
                    bandOff = orderBands(bandOffsets,pStride*w);
                    lStride = bands*pStride*w;
                } else { // band > line > pix
                    lStride = pStride*w;
                    bandOff = orderBands(bandOffsets,lStride*h);
                }
            }
        }

        // make sure we make room for negative offsets...
        int base = 0;
        if (scanlineStride < 0) {
            base += lStride*h;
            lStride *= -1;
        }
        if (pixelStride    < 0) {
            base += pStride*w;
            pStride *= -1;
        }

        for (int i=0; i<bands; i++)
            bandOff[i] += base;
        return new ComponentSampleModel(dataType, w, h, pStride,
                                        lStride, bankIndices, bandOff);
    }

    /**
     * Creates a new ComponentSampleModel with a subset of the bands
     * of this ComponentSampleModel.  The new ComponentSampleModel can be
     * used with any DataBuffer that the existing ComponentSampleModel
     * can be used with.  The new ComponentSampleModel/DataBuffer
     * combination will represent an image with a subset of the bands
     * of the original ComponentSampleModel/DataBuffer combination.
     * @param bands a subset of bands from this
     *              {@code ComponentSampleModel}
     * @return a {@code ComponentSampleModel} created with a subset
     *          of bands from this {@code ComponentSampleModel}.
     */
    public SampleModel createSubsetSampleModel(int[] bands) {
       if (bands.length > bankIndices.length)
            throw new RasterFormatException("There are only " +
                                            bankIndices.length +
                                            " bands");
        int[] newBankIndices = new int[bands.length];
        int[] newBandOffsets = new int[bands.length];

        for (int i=0; i<bands.length; i++) {
            newBankIndices[i] = bankIndices[bands[i]];
            newBandOffsets[i] = bandOffsets[bands[i]];
        }

        return new ComponentSampleModel(this.dataType, width, height,
                                        this.pixelStride,
                                        this.scanlineStride,
                                        newBankIndices, newBandOffsets);
    }

    /**
     * Creates a {@code DataBuffer} that corresponds to this
     * {@code ComponentSampleModel}.
     * The {@code DataBuffer} object's data type, number of banks,
     * and size are be consistent with this {@code ComponentSampleModel}.
     * @return a {@code DataBuffer} whose data type, number of banks
     *         and size are consistent with this
     *         {@code ComponentSampleModel}.
     */
    public DataBuffer createDataBuffer() {
        DataBuffer dataBuffer = null;

        int size = getBufferSize();
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            dataBuffer = new DataBufferByte(size, numBanks);
            break;
        case DataBuffer.TYPE_USHORT:
            dataBuffer = new DataBufferUShort(size, numBanks);
            break;
        case DataBuffer.TYPE_SHORT:
            dataBuffer = new DataBufferShort(size, numBanks);
            break;
        case DataBuffer.TYPE_INT:
            dataBuffer = new DataBufferInt(size, numBanks);
            break;
        case DataBuffer.TYPE_FLOAT:
            dataBuffer = new DataBufferFloat(size, numBanks);
            break;
        case DataBuffer.TYPE_DOUBLE:
            dataBuffer = new DataBufferDouble(size, numBanks);
            break;
        }

        return dataBuffer;
    }


    /** Gets the offset for the first band of pixel (x,y).
     *  A sample of the first band can be retrieved from a
     * {@code DataBuffer}
     *  {@code data} with a {@code ComponentSampleModel}
     * {@code csm} as
     * <pre>
     *        data.getElem(csm.getOffset(x, y));
     * </pre>
     * @param x the X location of the pixel
     * @param y the Y location of the pixel
     * @return the offset for the first band of the specified pixel.
     */
    public int getOffset(int x, int y) {
        int offset = y*scanlineStride + x*pixelStride + bandOffsets[0];
        return offset;
    }

    /** Gets the offset for band b of pixel (x,y).
     *  A sample of band {@code b} can be retrieved from a
     *  {@code DataBuffer data}
     *  with a {@code ComponentSampleModel csm} as
     * <pre>
     *       data.getElem(csm.getOffset(x, y, b));
     * </pre>
     * @param x the X location of the specified pixel
     * @param y the Y location of the specified pixel
     * @param b the specified band
     * @return the offset for the specified band of the specified pixel.
     */
    public int getOffset(int x, int y, int b) {
        int offset = y*scanlineStride + x*pixelStride + bandOffsets[b];
        return offset;
    }

    /** Returns the number of bits per sample for all bands.
     *  @return an array containing the number of bits per sample
     *          for all bands, where each element in the array
     *          represents a band.
     */
    public final int[] getSampleSize() {
        int[] sampleSize = new int [numBands];
        int sizeInBits = getSampleSize(0);

        for (int i=0; i<numBands; i++)
            sampleSize[i] = sizeInBits;

        return sampleSize;
    }

    /** Returns the number of bits per sample for the specified band.
     *  @param band the specified band
     *  @return the number of bits per sample for the specified band.
     */
    public final int getSampleSize(int band) {
        return DataBuffer.getDataTypeSize(dataType);
    }

    /** Returns the bank indices for all bands.
     *  @return the bank indices for all bands.
     */
    public final int [] getBankIndices() {
        return bankIndices.clone();
    }

    /** Returns the band offset for all bands.
     *  @return the band offsets for all bands.
     */
    public final int [] getBandOffsets() {
        return bandOffsets.clone();
    }

    /** Returns the scanline stride of this ComponentSampleModel.
     *  @return the scanline stride of this {@code ComponentSampleModel}.
     */
    public final int getScanlineStride() {
        return scanlineStride;
    }

    /** Returns the pixel stride of this ComponentSampleModel.
     *  @return the pixel stride of this {@code ComponentSampleModel}.
     */
    public final int getPixelStride() {
        return pixelStride;
    }

    /**
     * Returns the number of data elements needed to transfer a pixel
     * with the
     * {@link #getDataElements(int, int, Object, DataBuffer) } and
     * {@link #setDataElements(int, int, Object, DataBuffer) }
     * methods.
     * For a {@code ComponentSampleModel}, this is identical to the
     * number of bands.
     * @return the number of data elements needed to transfer a pixel with
     *         the {@code getDataElements} and
     *         {@code setDataElements} methods.
     * @see java.awt.image.SampleModel#getNumDataElements
     * @see #getNumBands
     */
    public final int getNumDataElements() {
        return getNumBands();
    }

    /**
     * Returns data for a single pixel in a primitive array of type
     * {@code TransferType}.  For a {@code ComponentSampleModel},
     * this is the same as the data type, and samples are returned
     * one per array element.  Generally, {@code obj} should
     * be passed in as {@code null}, so that the {@code Object}
     * is created automatically and is the right primitive data type.
     * <p>
     * The following code illustrates transferring data for one pixel from
     * {@code DataBuffer db1}, whose storage layout is
     * described by {@code ComponentSampleModel csm1},
     * to {@code DataBuffer db2}, whose storage layout
     * is described by {@code ComponentSampleModel csm2}.
     * The transfer is usually more efficient than using
     * {@code getPixel} and {@code setPixel}.
     * <pre>
     *       ComponentSampleModel csm1, csm2;
     *       DataBufferInt db1, db2;
     *       csm2.setDataElements(x, y,
     *                            csm1.getDataElements(x, y, null, db1), db2);
     * </pre>
     *
     * Using {@code getDataElements} and {@code setDataElements}
     * to transfer between two {@code DataBuffer/SampleModel}
     * pairs is legitimate if the {@code SampleModel} objects have
     * the same number of bands, corresponding bands have the same number of
     * bits per sample, and the {@code TransferType}s are the same.
     * <p>
     * If {@code obj} is not {@code null}, it should be a
     * primitive array of type {@code TransferType}.
     * Otherwise, a {@code ClassCastException} is thrown.  An
     * {@code ArrayIndexOutOfBoundsException} might be thrown if the
     * coordinates are not in bounds, or if {@code obj} is not
     * {@code null} and is not large enough to hold
     * the pixel data.
     *
     * @param x         the X coordinate of the pixel location
     * @param y         the Y coordinate of the pixel location
     * @param obj       if non-{@code null}, a primitive array
     *                  in which to return the pixel data
     * @param data      the {@code DataBuffer} containing the image data
     * @return the data of the specified pixel
     * @see #setDataElements(int, int, Object, DataBuffer)
     *
     * @throws NullPointerException if data is null.
     * @throws ArrayIndexOutOfBoundsException if the coordinates are
     * not in bounds, or if obj is too small to hold the output.
     */
    public Object getDataElements(int x, int y, Object obj, DataBuffer data) {
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }

        int type = getTransferType();
        int numDataElems = getNumDataElements();
        int pixelOffset = y*scanlineStride + x*pixelStride;

        switch(type) {

        case DataBuffer.TYPE_BYTE:

            byte[] bdata;

            if (obj == null)
                bdata = new byte[numDataElems];
            else
                bdata = (byte[])obj;

            for (int i=0; i<numDataElems; i++) {
                bdata[i] = (byte)data.getElem(bankIndices[i],
                                              pixelOffset + bandOffsets[i]);
            }

            obj = (Object)bdata;
            break;

        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:

            short[] sdata;

            if (obj == null)
                sdata = new short[numDataElems];
            else
                sdata = (short[])obj;

            for (int i=0; i<numDataElems; i++) {
                sdata[i] = (short)data.getElem(bankIndices[i],
                                               pixelOffset + bandOffsets[i]);
            }

            obj = (Object)sdata;
            break;

        case DataBuffer.TYPE_INT:

            int[] idata;

            if (obj == null)
                idata = new int[numDataElems];
            else
                idata = (int[])obj;

            for (int i=0; i<numDataElems; i++) {
                idata[i] = data.getElem(bankIndices[i],
                                        pixelOffset + bandOffsets[i]);
            }

            obj = (Object)idata;
            break;

        case DataBuffer.TYPE_FLOAT:

            float[] fdata;

            if (obj == null)
                fdata = new float[numDataElems];
            else
                fdata = (float[])obj;

            for (int i=0; i<numDataElems; i++) {
                fdata[i] = data.getElemFloat(bankIndices[i],
                                             pixelOffset + bandOffsets[i]);
            }

            obj = (Object)fdata;
            break;

        case DataBuffer.TYPE_DOUBLE:

            double[] ddata;

            if (obj == null)
                ddata = new double[numDataElems];
            else
                ddata = (double[])obj;

            for (int i=0; i<numDataElems; i++) {
                ddata[i] = data.getElemDouble(bankIndices[i],
                                              pixelOffset + bandOffsets[i]);
            }

            obj = (Object)ddata;
            break;
        }

        return obj;
    }

    /**
     * Returns all samples for the specified pixel in an int array,
     * one sample per array element.
     * An {@code ArrayIndexOutOfBoundsException} might be thrown if
     * the coordinates are not in bounds.
     * @param x         the X coordinate of the pixel location
     * @param y         the Y coordinate of the pixel location
     * @param iArray    If non-null, returns the samples in this array
     * @param data      The DataBuffer containing the image data
     * @return the samples of the specified pixel.
     * @see #setPixel(int, int, int[], DataBuffer)
     *
     * @throws NullPointerException if data is null.
     * @throws ArrayIndexOutOfBoundsException if the coordinates are
     * not in bounds, or if iArray is too small to hold the output.
     */
    public int[] getPixel(int x, int y, int[] iArray, DataBuffer data) {
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int[] pixels;
        if (iArray != null) {
           pixels = iArray;
        } else {
           pixels = new int [numBands];
        }
        int pixelOffset = y*scanlineStride + x*pixelStride;
        for (int i=0; i<numBands; i++) {
            pixels[i] = data.getElem(bankIndices[i],
                                     pixelOffset + bandOffsets[i]);
        }
        return pixels;
    }

    /**
     * Returns all samples for the specified rectangle of pixels in
     * an int array, one sample per array element.
     * An {@code ArrayIndexOutOfBoundsException} might be thrown if
     * the coordinates are not in bounds.
     * @param x         The X coordinate of the upper left pixel location
     * @param y         The Y coordinate of the upper left pixel location
     * @param w         The width of the pixel rectangle
     * @param h         The height of the pixel rectangle
     * @param iArray    If non-null, returns the samples in this array
     * @param data      The DataBuffer containing the image data
     * @return the samples of the pixels within the specified region.
     * @see #setPixels(int, int, int, int, int[], DataBuffer)
     */
    public int[] getPixels(int x, int y, int w, int h,
                           int[] iArray, DataBuffer data) {
        int x1 = x + w;
        int y1 = y + h;

        if (x < 0 || x >= width || w > width || x1 < 0 || x1 > width ||
            y < 0 || y >= height || y > height || y1 < 0 || y1 >  height)
        {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int[] pixels;
        if (iArray != null) {
           pixels = iArray;
        } else {
           pixels = new int [w*h*numBands];
        }
        int lineOffset = y*scanlineStride + x*pixelStride;
        int srcOffset = 0;

        for (int i = 0; i < h; i++) {
           int pixelOffset = lineOffset;
           for (int j = 0; j < w; j++) {
              for (int k=0; k < numBands; k++) {
                 pixels[srcOffset++] =
                    data.getElem(bankIndices[k], pixelOffset + bandOffsets[k]);
              }
              pixelOffset += pixelStride;
           }
           lineOffset += scanlineStride;
        }
        return pixels;
    }

    /**
     * Returns as int the sample in a specified band for the pixel
     * located at (x,y).
     * An {@code ArrayIndexOutOfBoundsException} might be thrown if
     * the coordinates are not in bounds.
     * @param x         the X coordinate of the pixel location
     * @param y         the Y coordinate of the pixel location
     * @param b         the band to return
     * @param data      the {@code DataBuffer} containing the image data
     * @return the sample in a specified band for the specified pixel
     * @see #setSample(int, int, int, int, DataBuffer)
     */
    public int getSample(int x, int y, int b, DataBuffer data) {
        // Bounds check for 'b' will be performed automatically
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int sample = data.getElem(bankIndices[b],
                                  y*scanlineStride + x*pixelStride +
                                  bandOffsets[b]);
        return sample;
    }

    /**
     * Returns the sample in a specified band
     * for the pixel located at (x,y) as a float.
     * An {@code ArrayIndexOutOfBoundsException} might be
     * thrown if the coordinates are not in bounds.
     * @param x         The X coordinate of the pixel location
     * @param y         The Y coordinate of the pixel location
     * @param b         The band to return
     * @param data      The DataBuffer containing the image data
     * @return a float value representing the sample in the specified
     * band for the specified pixel.
     */
    public float getSampleFloat(int x, int y, int b, DataBuffer data) {
        // Bounds check for 'b' will be performed automatically
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }

        float sample = data.getElemFloat(bankIndices[b],
                                         y*scanlineStride + x*pixelStride +
                                         bandOffsets[b]);
        return sample;
    }

    /**
     * Returns the sample in a specified band
     * for a pixel located at (x,y) as a double.
     * An {@code ArrayIndexOutOfBoundsException} might be
     * thrown if the coordinates are not in bounds.
     * @param x         The X coordinate of the pixel location
     * @param y         The Y coordinate of the pixel location
     * @param b         The band to return
     * @param data      The DataBuffer containing the image data
     * @return a double value representing the sample in the specified
     * band for the specified pixel.
     */
    public double getSampleDouble(int x, int y, int b, DataBuffer data) {
        // Bounds check for 'b' will be performed automatically
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }

        double sample = data.getElemDouble(bankIndices[b],
                                           y*scanlineStride + x*pixelStride +
                                           bandOffsets[b]);
        return sample;
    }

    /**
     * Returns the samples in a specified band for the specified rectangle
     * of pixels in an int array, one sample per data array element.
     * An {@code ArrayIndexOutOfBoundsException} might be thrown if
     * the coordinates are not in bounds.
     * @param x         The X coordinate of the upper left pixel location
     * @param y         The Y coordinate of the upper left pixel location
     * @param w         the width of the pixel rectangle
     * @param h         the height of the pixel rectangle
     * @param b         the band to return
     * @param iArray    if non-{@code null}, returns the samples
     *                  in this array
     * @param data      the {@code DataBuffer} containing the image data
     * @return the samples in the specified band of the specified pixel
     * @see #setSamples(int, int, int, int, int, int[], DataBuffer)
     */
    public int[] getSamples(int x, int y, int w, int h, int b,
                            int[] iArray, DataBuffer data) {
        // Bounds check for 'b' will be performed automatically
        if ((x < 0) || (y < 0) || (x + w > width) || (y + h > height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int[] samples;
        if (iArray != null) {
           samples = iArray;
        } else {
           samples = new int [w*h];
        }
        int lineOffset = y*scanlineStride + x*pixelStride +  bandOffsets[b];
        int srcOffset = 0;

        for (int i = 0; i < h; i++) {
           int sampleOffset = lineOffset;
           for (int j = 0; j < w; j++) {
              samples[srcOffset++] = data.getElem(bankIndices[b],
                                                  sampleOffset);
              sampleOffset += pixelStride;
           }
           lineOffset += scanlineStride;
        }
        return samples;
    }

    /**
     * Sets the data for a single pixel in the specified
     * {@code DataBuffer} from a primitive array of type
     * {@code TransferType}.  For a {@code ComponentSampleModel},
     * this is the same as the data type, and samples are transferred
     * one per array element.
     * <p>
     * The following code illustrates transferring data for one pixel from
     * {@code DataBuffer db1}, whose storage layout is
     * described by {@code ComponentSampleModel csm1},
     * to {@code DataBuffer db2}, whose storage layout
     * is described by {@code ComponentSampleModel csm2}.
     * The transfer is usually more efficient than using
     * {@code getPixel} and {@code setPixel}.
     * <pre>
     *       ComponentSampleModel csm1, csm2;
     *       DataBufferInt db1, db2;
     *       csm2.setDataElements(x, y, csm1.getDataElements(x, y, null, db1),
     *                            db2);
     * </pre>
     * Using {@code getDataElements} and {@code setDataElements}
     * to transfer between two {@code DataBuffer/SampleModel} pairs
     * is legitimate if the {@code SampleModel} objects have
     * the same number of bands, corresponding bands have the same number of
     * bits per sample, and the {@code TransferType}s are the same.
     * <p>
     * A {@code ClassCastException} is thrown if {@code obj} is not
     * a primitive array of type {@code TransferType}.
     * An {@code ArrayIndexOutOfBoundsException} might be thrown if
     * the coordinates are not in bounds, or if {@code obj} is not large
     * enough to hold the pixel data.
     * @param x         the X coordinate of the pixel location
     * @param y         the Y coordinate of the pixel location
     * @param obj       a primitive array containing pixel data
     * @param data      the DataBuffer containing the image data
     * @see #getDataElements(int, int, Object, DataBuffer)
     */
    public void setDataElements(int x, int y, Object obj, DataBuffer data) {
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }

        int type = getTransferType();
        int numDataElems = getNumDataElements();
        int pixelOffset = y*scanlineStride + x*pixelStride;

        switch(type) {

        case DataBuffer.TYPE_BYTE:

            byte[] barray = (byte[])obj;

            for (int i=0; i<numDataElems; i++) {
                data.setElem(bankIndices[i], pixelOffset + bandOffsets[i],
                           ((int)barray[i])&0xff);
            }
            break;

        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:

            short[] sarray = (short[])obj;

            for (int i=0; i<numDataElems; i++) {
                data.setElem(bankIndices[i], pixelOffset + bandOffsets[i],
                           ((int)sarray[i])&0xffff);
            }
            break;

        case DataBuffer.TYPE_INT:

            int[] iarray = (int[])obj;

            for (int i=0; i<numDataElems; i++) {
                data.setElem(bankIndices[i],
                             pixelOffset + bandOffsets[i], iarray[i]);
            }
            break;

        case DataBuffer.TYPE_FLOAT:

            float[] farray = (float[])obj;

            for (int i=0; i<numDataElems; i++) {
                data.setElemFloat(bankIndices[i],
                             pixelOffset + bandOffsets[i], farray[i]);
            }
            break;

        case DataBuffer.TYPE_DOUBLE:

            double[] darray = (double[])obj;

            for (int i=0; i<numDataElems; i++) {
                data.setElemDouble(bankIndices[i],
                             pixelOffset + bandOffsets[i], darray[i]);
            }
            break;

        }
    }

    /**
     * Sets a pixel in the {@code DataBuffer} using an int array of
     * samples for input.  An {@code ArrayIndexOutOfBoundsException}
     * might be thrown if the coordinates are
     * not in bounds.
     * @param x         The X coordinate of the pixel location
     * @param y         The Y coordinate of the pixel location
     * @param iArray    The input samples in an int array
     * @param data      The DataBuffer containing the image data
     * @see #getPixel(int, int, int[], DataBuffer)
     */
    public void setPixel(int x, int y, int[] iArray, DataBuffer data) {
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
       int pixelOffset = y*scanlineStride + x*pixelStride;
       for (int i=0; i<numBands; i++) {
           data.setElem(bankIndices[i],
                        pixelOffset + bandOffsets[i],iArray[i]);
       }
    }

    /**
     * Sets all samples for a rectangle of pixels from an int array containing
     * one sample per array element.
     * An {@code ArrayIndexOutOfBoundsException} might be thrown if the
     * coordinates are not in bounds.
     * @param x         The X coordinate of the upper left pixel location
     * @param y         The Y coordinate of the upper left pixel location
     * @param w         The width of the pixel rectangle
     * @param h         The height of the pixel rectangle
     * @param iArray    The input samples in an int array
     * @param data      The DataBuffer containing the image data
     * @see #getPixels(int, int, int, int, int[], DataBuffer)
     */
    public void setPixels(int x, int y, int w, int h,
                          int[] iArray, DataBuffer data) {
        int x1 = x + w;
        int y1 = y + h;

        if (x < 0 || x >= width || w > width || x1 < 0 || x1 > width ||
            y < 0 || y >= height || h > height || y1 < 0 || y1 >  height)
        {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }

        int lineOffset = y*scanlineStride + x*pixelStride;
        int srcOffset = 0;

        for (int i = 0; i < h; i++) {
           int pixelOffset = lineOffset;
           for (int j = 0; j < w; j++) {
              for (int k=0; k < numBands; k++) {
                 data.setElem(bankIndices[k], pixelOffset + bandOffsets[k],
                              iArray[srcOffset++]);
              }
              pixelOffset += pixelStride;
           }
           lineOffset += scanlineStride;
        }
    }

    /**
     * Sets a sample in the specified band for the pixel located at (x,y)
     * in the {@code DataBuffer} using an int for input.
     * An {@code ArrayIndexOutOfBoundsException} might be thrown if the
     * coordinates are not in bounds.
     * @param x         The X coordinate of the pixel location
     * @param y         The Y coordinate of the pixel location
     * @param b         the band to set
     * @param s         the input sample as an int
     * @param data      the DataBuffer containing the image data
     * @see #getSample(int, int, int, DataBuffer)
     */
    public void setSample(int x, int y, int b, int s,
                          DataBuffer data) {
        // Bounds check for 'b' will be performed automatically
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        data.setElem(bankIndices[b],
                     y*scanlineStride + x*pixelStride + bandOffsets[b], s);
    }

    /**
     * Sets a sample in the specified band for the pixel located at (x,y)
     * in the {@code DataBuffer} using a float for input.
     * An {@code ArrayIndexOutOfBoundsException} might be thrown if
     * the coordinates are not in bounds.
     * @param x         The X coordinate of the pixel location
     * @param y         The Y coordinate of the pixel location
     * @param b         The band to set
     * @param s         The input sample as a float
     * @param data      The DataBuffer containing the image data
     * @see #getSample(int, int, int, DataBuffer)
     */
    public void setSample(int x, int y, int b,
                          float s ,
                          DataBuffer data) {
        // Bounds check for 'b' will be performed automatically
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        data.setElemFloat(bankIndices[b],
                          y*scanlineStride + x*pixelStride + bandOffsets[b],
                          s);
    }

    /**
     * Sets a sample in the specified band for the pixel located at (x,y)
     * in the {@code DataBuffer} using a double for input.
     * An {@code ArrayIndexOutOfBoundsException} might be thrown if
     * the coordinates are not in bounds.
     * @param x         The X coordinate of the pixel location
     * @param y         The Y coordinate of the pixel location
     * @param b         The band to set
     * @param s         The input sample as a double
     * @param data      The DataBuffer containing the image data
     * @see #getSample(int, int, int, DataBuffer)
     */
    public void setSample(int x, int y, int b,
                          double s,
                          DataBuffer data) {
        // Bounds check for 'b' will be performed automatically
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        data.setElemDouble(bankIndices[b],
                          y*scanlineStride + x*pixelStride + bandOffsets[b],
                          s);
    }

    /**
     * Sets the samples in the specified band for the specified rectangle
     * of pixels from an int array containing one sample per data array element.
     * An {@code ArrayIndexOutOfBoundsException} might be thrown if the
     * coordinates are not in bounds.
     * @param x         The X coordinate of the upper left pixel location
     * @param y         The Y coordinate of the upper left pixel location
     * @param w         The width of the pixel rectangle
     * @param h         The height of the pixel rectangle
     * @param b         The band to set
     * @param iArray    The input samples in an int array
     * @param data      The DataBuffer containing the image data
     * @see #getSamples(int, int, int, int, int, int[], DataBuffer)
     */
    public void setSamples(int x, int y, int w, int h, int b,
                           int[] iArray, DataBuffer data) {
        // Bounds check for 'b' will be performed automatically
        if ((x < 0) || (y < 0) || (x + w > width) || (y + h > height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int lineOffset = y*scanlineStride + x*pixelStride + bandOffsets[b];
        int srcOffset = 0;

        for (int i = 0; i < h; i++) {
           int sampleOffset = lineOffset;
           for (int j = 0; j < w; j++) {
              data.setElem(bankIndices[b], sampleOffset, iArray[srcOffset++]);
              sampleOffset += pixelStride;
           }
           lineOffset += scanlineStride;
        }
    }

    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof ComponentSampleModel)) {
            return false;
        }

        ComponentSampleModel that = (ComponentSampleModel)o;
        return this.width == that.width &&
            this.height == that.height &&
            this.numBands == that.numBands &&
            this.dataType == that.dataType &&
            Arrays.equals(this.bandOffsets, that.bandOffsets) &&
            Arrays.equals(this.bankIndices, that.bankIndices) &&
            this.numBanks == that.numBanks &&
            this.scanlineStride == that.scanlineStride &&
            this.pixelStride == that.pixelStride;
    }

    // If we implement equals() we must also implement hashCode
    public int hashCode() {
        int hash = 0;
        hash = width;
        hash <<= 8;
        hash ^= height;
        hash <<= 8;
        hash ^= numBands;
        hash <<= 8;
        hash ^= dataType;
        hash <<= 8;
        for (int i = 0; i < bandOffsets.length; i++) {
            hash ^= bandOffsets[i];
            hash <<= 8;
        }
        for (int i = 0; i < bankIndices.length; i++) {
            hash ^= bankIndices[i];
            hash <<= 8;
        }
        hash ^= numBanks;
        hash <<= 8;
        hash ^= scanlineStride;
        hash <<= 8;
        hash ^= pixelStride;
        return hash;
    }
}
