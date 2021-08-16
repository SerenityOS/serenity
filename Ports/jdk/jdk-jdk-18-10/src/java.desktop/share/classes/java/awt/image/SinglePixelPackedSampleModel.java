/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
 *  This class represents pixel data packed such that the N samples which make
 *  up a single pixel are stored in a single data array element, and each data
 *  data array element holds samples for only one pixel.
 *  This class supports
 *  {@link DataBuffer#TYPE_BYTE TYPE_BYTE},
 *  {@link DataBuffer#TYPE_USHORT TYPE_USHORT},
 *  {@link DataBuffer#TYPE_INT TYPE_INT} data types.
 *  All data array elements reside
 *  in the first bank of a DataBuffer.  Accessor methods are provided so
 *  that the image data can be manipulated directly. Scanline stride is the
 *  number of data array elements between a given sample and the corresponding
 *  sample in the same column of the next scanline. Bit masks are the masks
 *  required to extract the samples representing the bands of the pixel.
 *  Bit offsets are the offsets in bits into the data array
 *  element of the samples representing the bands of the pixel.
 * <p>
 * The following code illustrates extracting the bits of the sample
 * representing band {@code b} for pixel {@code x,y}
 * from DataBuffer {@code data}:
 * <pre>{@code
 *      int sample = data.getElem(y * scanlineStride + x);
 *      sample = (sample & bitMasks[b]) >>> bitOffsets[b];
 * }</pre>
 */

public class SinglePixelPackedSampleModel extends SampleModel
{
    /** Bit masks for all bands of the image data. */
    private int[] bitMasks;

    /** Bit Offsets for all bands of the image data. */
    private int[] bitOffsets;

    /** Bit sizes for all the bands of the image data. */
    private int[] bitSizes;

    /** Maximum bit size. */
    private int maxBitSize;

    /** Line stride of the region of image data described by this
     *  SinglePixelPackedSampleModel.
     */
    private int scanlineStride;

    private static native void initIDs();
    static {
        ColorModel.loadLibraries();
        initIDs();
    }

    /**
     * Constructs a SinglePixelPackedSampleModel with bitMasks.length bands.
     * Each sample is stored in a data array element in the position of
     * its corresponding bit mask.  Each bit mask must be contiguous and
     * masks must not overlap. Bit masks exceeding data type capacity are
     * truncated.
     * @param dataType  The data type for storing samples.
     * @param w         The width (in pixels) of the region of the
     *                  image data described.
     * @param h         The height (in pixels) of the region of the
     *                  image data described.
     * @param bitMasks  The bit masks for all bands.
     * @throws IllegalArgumentException if {@code dataType} is not
     *         either {@code DataBuffer.TYPE_BYTE},
     *         {@code DataBuffer.TYPE_USHORT}, or
     *         {@code DataBuffer.TYPE_INT}
     */
    public SinglePixelPackedSampleModel(int dataType, int w, int h,
                                   int[] bitMasks) {
        this(dataType, w, h, w, bitMasks);
        if (dataType != DataBuffer.TYPE_BYTE &&
            dataType != DataBuffer.TYPE_USHORT &&
            dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Unsupported data type "+
                                               dataType);
        }
    }

    /**
     * Constructs a SinglePixelPackedSampleModel with bitMasks.length bands
     * and a scanline stride equal to scanlineStride data array elements.
     * Each sample is stored in a data array element in the position of
     * its corresponding bit mask.  Each bit mask must be contiguous and
     * masks must not overlap. Bit masks exceeding data type capacity are
     * truncated.
     * @param dataType  The data type for storing samples.
     * @param w         The width (in pixels) of the region of
     *                  image data described.
     * @param h         The height (in pixels) of the region of
     *                  image data described.
     * @param scanlineStride The line stride of the image data.
     * @param bitMasks The bit masks for all bands.
     * @throws IllegalArgumentException if {@code w} or
     *         {@code h} is not greater than 0
     * @throws IllegalArgumentException if any mask in
     *         {@code bitMask} is not contiguous
     * @throws IllegalArgumentException if {@code dataType} is not
     *         either {@code DataBuffer.TYPE_BYTE},
     *         {@code DataBuffer.TYPE_USHORT}, or
     *         {@code DataBuffer.TYPE_INT}
     */
    public SinglePixelPackedSampleModel(int dataType, int w, int h,
                                   int scanlineStride, int[] bitMasks) {
        super(dataType, w, h, bitMasks.length);
        if (dataType != DataBuffer.TYPE_BYTE &&
            dataType != DataBuffer.TYPE_USHORT &&
            dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Unsupported data type "+
                                               dataType);
        }
        this.dataType = dataType;
        this.bitMasks = bitMasks.clone();
        this.scanlineStride = scanlineStride;

        this.bitOffsets = new int[numBands];
        this.bitSizes = new int[numBands];

        int maxMask = (int)((1L << DataBuffer.getDataTypeSize(dataType)) - 1);

        this.maxBitSize = 0;
        for (int i=0; i<numBands; i++) {
            int bitOffset = 0, bitSize = 0, mask;
            this.bitMasks[i] &= maxMask;
            mask = this.bitMasks[i];
            if (mask != 0) {
                while ((mask & 1) == 0) {
                    mask = mask >>> 1;
                    bitOffset++;
                }
                while ((mask & 1) == 1) {
                    mask = mask >>> 1;
                    bitSize++;
                }
                if (mask != 0) {
                    throw new IllegalArgumentException("Mask "+bitMasks[i]+
                                                       " must be contiguous");
                }
            }
            bitOffsets[i] = bitOffset;
            bitSizes[i] = bitSize;
            if (bitSize > maxBitSize) {
                maxBitSize = bitSize;
            }
        }
    }

    /**
     * Returns the number of data elements needed to transfer one pixel
     * via the getDataElements and setDataElements methods.
     * For a SinglePixelPackedSampleModel, this is one.
     */
    public int getNumDataElements() {
        return 1;
    }

    /**
     * Returns the size of the buffer (in data array elements)
     * needed for a data buffer that matches this
     * SinglePixelPackedSampleModel.
     */
    private long getBufferSize() {
      long size = scanlineStride * (height-1) + width;
      return size;
    }

    /**
     * Creates a new SinglePixelPackedSampleModel with the specified
     * width and height.  The new SinglePixelPackedSampleModel will have the
     * same storage data type and bit masks as this
     * SinglePixelPackedSampleModel.
     * @param w the width of the resulting {@code SampleModel}
     * @param h the height of the resulting {@code SampleModel}
     * @return a {@code SinglePixelPackedSampleModel} with the
     *         specified width and height.
     * @throws IllegalArgumentException if {@code w} or
     *         {@code h} is not greater than 0
     */
    public SampleModel createCompatibleSampleModel(int w, int h) {
      SampleModel sampleModel = new SinglePixelPackedSampleModel(dataType, w, h,
                                                              bitMasks);
      return sampleModel;
    }

    /**
     * Creates a DataBuffer that corresponds to this
     * SinglePixelPackedSampleModel.  The DataBuffer's data type and size
     * will be consistent with this SinglePixelPackedSampleModel.  The
     * DataBuffer will have a single bank.
     */
    public DataBuffer createDataBuffer() {
        DataBuffer dataBuffer = null;

        int size = (int)getBufferSize();
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            dataBuffer = new DataBufferByte(size);
            break;
        case DataBuffer.TYPE_USHORT:
            dataBuffer = new DataBufferUShort(size);
            break;
        case DataBuffer.TYPE_INT:
            dataBuffer = new DataBufferInt(size);
            break;
        }
        return dataBuffer;
    }

    /** Returns the number of bits per sample for all bands. */
    public int[] getSampleSize() {
        return bitSizes.clone();
    }

    /** Returns the number of bits per sample for the specified band. */
    public int getSampleSize(int band) {
        return bitSizes[band];
    }

    /** Returns the offset (in data array elements) of pixel (x,y).
     *  The data element containing pixel {@code x,y}
     *  can be retrieved from a DataBuffer {@code data} with a
     *  SinglePixelPackedSampleModel {@code sppsm} as:
     * <pre>
     *        data.getElem(sppsm.getOffset(x, y));
     * </pre>
     * @param x the X coordinate of the specified pixel
     * @param y the Y coordinate of the specified pixel
     * @return the offset of the specified pixel.
     */
    public int getOffset(int x, int y) {
        int offset = y * scanlineStride + x;
        return offset;
    }

    /** Returns the bit offsets into the data array element representing
     *  a pixel for all bands.
     *  @return the bit offsets representing a pixel for all bands.
     */
    public int [] getBitOffsets() {
      return bitOffsets.clone();
    }

    /** Returns the bit masks for all bands.
     *  @return the bit masks for all bands.
     */
    public int [] getBitMasks() {
      return bitMasks.clone();
    }

    /** Returns the scanline stride of this SinglePixelPackedSampleModel.
     *  @return the scanline stride of this
     *          {@code SinglePixelPackedSampleModel}.
     */
    public int getScanlineStride() {
      return scanlineStride;
    }

    /**
     * This creates a new SinglePixelPackedSampleModel with a subset of the
     * bands of this SinglePixelPackedSampleModel.  The new
     * SinglePixelPackedSampleModel can be used with any DataBuffer that the
     * existing SinglePixelPackedSampleModel can be used with.  The new
     * SinglePixelPackedSampleModel/DataBuffer combination will represent
     * an image with a subset of the bands of the original
     * SinglePixelPackedSampleModel/DataBuffer combination.
     * @exception RasterFormatException if the length of the bands argument is
     *                                  greater than the number of bands in
     *                                  the sample model.
     */
    public SampleModel createSubsetSampleModel(int[] bands) {
        if (bands.length > numBands)
            throw new RasterFormatException("There are only " +
                                            numBands +
                                            " bands");
        int[] newBitMasks = new int[bands.length];
        for (int i=0; i<bands.length; i++)
            newBitMasks[i] = bitMasks[bands[i]];

        return new SinglePixelPackedSampleModel(this.dataType, width, height,
                                           this.scanlineStride, newBitMasks);
    }

    /**
     * Returns data for a single pixel in a primitive array of type
     * TransferType.  For a SinglePixelPackedSampleModel, the array will
     * have one element, and the type will be the same as the storage
     * data type.  Generally, obj
     * should be passed in as null, so that the Object will be created
     * automatically and will be of the right primitive data type.
     * <p>
     * The following code illustrates transferring data for one pixel from
     * DataBuffer {@code db1}, whose storage layout is described by
     * SinglePixelPackedSampleModel {@code sppsm1}, to
     * DataBuffer {@code db2}, whose storage layout is described by
     * SinglePixelPackedSampleModel {@code sppsm2}.
     * The transfer will generally be more efficient than using
     * getPixel/setPixel.
     * <pre>
     *       SinglePixelPackedSampleModel sppsm1, sppsm2;
     *       DataBufferInt db1, db2;
     *       sppsm2.setDataElements(x, y, sppsm1.getDataElements(x, y, null,
     *                              db1), db2);
     * </pre>
     * Using getDataElements/setDataElements to transfer between two
     * DataBuffer/SampleModel pairs is legitimate if the SampleModels have
     * the same number of bands, corresponding bands have the same number of
     * bits per sample, and the TransferTypes are the same.
     * <p>
     * If obj is non-null, it should be a primitive array of type TransferType.
     * Otherwise, a ClassCastException is thrown.  An
     * ArrayIndexOutOfBoundsException may be thrown if the coordinates are
     * not in bounds, or if obj is non-null and is not large enough to hold
     * the pixel data.
     * @param x         The X coordinate of the pixel location.
     * @param y         The Y coordinate of the pixel location.
     * @param obj       If non-null, a primitive array in which to return
     *                  the pixel data.
     * @param data      The DataBuffer containing the image data.
     * @return the data for the specified pixel.
     * @see #setDataElements(int, int, Object, DataBuffer)
     */
    public Object getDataElements(int x, int y, Object obj, DataBuffer data) {
        // Bounds check for 'b' will be performed automatically
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }

        int type = getTransferType();

        switch(type) {

        case DataBuffer.TYPE_BYTE:

            byte[] bdata;

            if (obj == null)
                bdata = new byte[1];
            else
                bdata = (byte[])obj;

            bdata[0] = (byte)data.getElem(y * scanlineStride + x);

            obj = (Object)bdata;
            break;

        case DataBuffer.TYPE_USHORT:

            short[] sdata;

            if (obj == null)
                sdata = new short[1];
            else
                sdata = (short[])obj;

            sdata[0] = (short)data.getElem(y * scanlineStride + x);

            obj = (Object)sdata;
            break;

        case DataBuffer.TYPE_INT:

            int[] idata;

            if (obj == null)
                idata = new int[1];
            else
                idata = (int[])obj;

            idata[0] = data.getElem(y * scanlineStride + x);

            obj = (Object)idata;
            break;
        }

        return obj;
    }

    /**
     * Returns all samples in for the specified pixel in an int array.
     * ArrayIndexOutOfBoundsException may be thrown if the coordinates are
     * not in bounds.
     * @param x         The X coordinate of the pixel location.
     * @param y         The Y coordinate of the pixel location.
     * @param iArray    If non-null, returns the samples in this array
     * @param data      The DataBuffer containing the image data.
     * @return all samples for the specified pixel.
     * @see #setPixel(int, int, int[], DataBuffer)
     */
    public int [] getPixel(int x, int y, int[] iArray, DataBuffer data) {
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int[] pixels;
        if (iArray == null) {
            pixels = new int [numBands];
        } else {
            pixels = iArray;
        }

        int value = data.getElem(y * scanlineStride + x);
        for (int i=0; i<numBands; i++) {
            pixels[i] = (value & bitMasks[i]) >>> bitOffsets[i];
        }
        return pixels;
    }

    /**
     * Returns all samples for the specified rectangle of pixels in
     * an int array, one sample per array element.
     * ArrayIndexOutOfBoundsException may be thrown if the coordinates are
     * not in bounds.
     * @param x         The X coordinate of the upper left pixel location.
     * @param y         The Y coordinate of the upper left pixel location.
     * @param w         The width of the pixel rectangle.
     * @param h         The height of the pixel rectangle.
     * @param iArray    If non-null, returns the samples in this array.
     * @param data      The DataBuffer containing the image data.
     * @return all samples for the specified region of pixels.
     * @see #setPixels(int, int, int, int, int[], DataBuffer)
     */
    public int[] getPixels(int x, int y, int w, int h,
                           int[] iArray, DataBuffer data) {
        int x1 = x + w;
        int y1 = y + h;

        if (x < 0 || x >= width || w > width || x1 < 0 || x1 > width ||
            y < 0 || y >= height || h > height || y1 < 0 || y1 >  height)
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
        int lineOffset = y*scanlineStride + x;
        int dstOffset = 0;

        for (int i = 0; i < h; i++) {
           for (int j = 0; j < w; j++) {
              int value = data.getElem(lineOffset+j);
              for (int k=0; k < numBands; k++) {
                  pixels[dstOffset++] =
                     ((value & bitMasks[k]) >>> bitOffsets[k]);
              }
           }
           lineOffset += scanlineStride;
        }
        return pixels;
    }

    /**
     * Returns as int the sample in a specified band for the pixel
     * located at (x,y).
     * ArrayIndexOutOfBoundsException may be thrown if the coordinates are
     * not in bounds.
     * @param x         The X coordinate of the pixel location.
     * @param y         The Y coordinate of the pixel location.
     * @param b         The band to return.
     * @param data      The DataBuffer containing the image data.
     * @return the sample in a specified band for the specified
     *         pixel.
     * @see #setSample(int, int, int, int, DataBuffer)
     */
    public int getSample(int x, int y, int b, DataBuffer data) {
        // Bounds check for 'b' will be performed automatically
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int sample = data.getElem(y * scanlineStride + x);
        return ((sample & bitMasks[b]) >>> bitOffsets[b]);
    }

    /**
     * Returns the samples for a specified band for the specified rectangle
     * of pixels in an int array, one sample per array element.
     * ArrayIndexOutOfBoundsException may be thrown if the coordinates are
     * not in bounds.
     * @param x         The X coordinate of the upper left pixel location.
     * @param y         The Y coordinate of the upper left pixel location.
     * @param w         The width of the pixel rectangle.
     * @param h         The height of the pixel rectangle.
     * @param b         The band to return.
     * @param iArray    If non-null, returns the samples in this array.
     * @param data      The DataBuffer containing the image data.
     * @return the samples for the specified band for the specified
     *         region of pixels.
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
        int lineOffset = y*scanlineStride + x;
        int dstOffset = 0;

        for (int i = 0; i < h; i++) {
           for (int j = 0; j < w; j++) {
              int value = data.getElem(lineOffset+j);
              samples[dstOffset++] =
                 ((value & bitMasks[b]) >>> bitOffsets[b]);
           }
           lineOffset += scanlineStride;
        }
        return samples;
    }

    /**
     * Sets the data for a single pixel in the specified DataBuffer from a
     * primitive array of type TransferType.  For a
     * SinglePixelPackedSampleModel, only the first element of the array
     * will hold valid data, and the type of the array must be the same as
     * the storage data type of the SinglePixelPackedSampleModel.
     * <p>
     * The following code illustrates transferring data for one pixel from
     * DataBuffer {@code db1}, whose storage layout is described by
     * SinglePixelPackedSampleModel {@code sppsm1},
     * to DataBuffer {@code db2}, whose storage layout is described by
     * SinglePixelPackedSampleModel {@code sppsm2}.
     * The transfer will generally be more efficient than using
     * getPixel/setPixel.
     * <pre>
     *       SinglePixelPackedSampleModel sppsm1, sppsm2;
     *       DataBufferInt db1, db2;
     *       sppsm2.setDataElements(x, y, sppsm1.getDataElements(x, y, null,
     *                              db1), db2);
     * </pre>
     * Using getDataElements/setDataElements to transfer between two
     * DataBuffer/SampleModel pairs is legitimate if the SampleModels have
     * the same number of bands, corresponding bands have the same number of
     * bits per sample, and the TransferTypes are the same.
     * <p>
     * obj must be a primitive array of type TransferType.  Otherwise,
     * a ClassCastException is thrown.  An
     * ArrayIndexOutOfBoundsException may be thrown if the coordinates are
     * not in bounds, or if obj is not large enough to hold the pixel data.
     * @param x         The X coordinate of the pixel location.
     * @param y         The Y coordinate of the pixel location.
     * @param obj       A primitive array containing pixel data.
     * @param data      The DataBuffer containing the image data.
     * @see #getDataElements(int, int, Object, DataBuffer)
     */
    public void setDataElements(int x, int y, Object obj, DataBuffer data) {
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }

        int type = getTransferType();

        switch(type) {

        case DataBuffer.TYPE_BYTE:

            byte[] barray = (byte[])obj;
            data.setElem(y*scanlineStride+x, ((int)barray[0])&0xff);
            break;

        case DataBuffer.TYPE_USHORT:

            short[] sarray = (short[])obj;
            data.setElem(y*scanlineStride+x, ((int)sarray[0])&0xffff);
            break;

        case DataBuffer.TYPE_INT:

            int[] iarray = (int[])obj;
            data.setElem(y*scanlineStride+x, iarray[0]);
            break;
        }
    }

    /**
     * Sets a pixel in the DataBuffer using an int array of samples for input.
     * ArrayIndexOutOfBoundsException may be thrown if the coordinates are
     * not in bounds.
     * @param x         The X coordinate of the pixel location.
     * @param y         The Y coordinate of the pixel location.
     * @param iArray    The input samples in an int array.
     * @param data      The DataBuffer containing the image data.
     * @see #getPixel(int, int, int[], DataBuffer)
     */
    public void setPixel(int x, int y,
                         int[] iArray,
                         DataBuffer data) {
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int lineOffset = y * scanlineStride + x;
        int value = data.getElem(lineOffset);
        for (int i=0; i < numBands; i++) {
            value &= ~bitMasks[i];
            value |= ((iArray[i] << bitOffsets[i]) & bitMasks[i]);
        }
        data.setElem(lineOffset, value);
    }

    /**
     * Sets all samples for a rectangle of pixels from an int array containing
     * one sample per array element.
     * ArrayIndexOutOfBoundsException may be thrown if the coordinates are
     * not in bounds.
     * @param x         The X coordinate of the upper left pixel location.
     * @param y         The Y coordinate of the upper left pixel location.
     * @param w         The width of the pixel rectangle.
     * @param h         The height of the pixel rectangle.
     * @param iArray    The input samples in an int array.
     * @param data      The DataBuffer containing the image data.
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

        int lineOffset = y*scanlineStride + x;
        int srcOffset = 0;

        for (int i = 0; i < h; i++) {
           for (int j = 0; j < w; j++) {
               int value = data.getElem(lineOffset+j);
               for (int k=0; k < numBands; k++) {
                   value &= ~bitMasks[k];
                   int srcValue = iArray[srcOffset++];
                   value |= ((srcValue << bitOffsets[k])
                             & bitMasks[k]);
               }
               data.setElem(lineOffset+j, value);
           }
           lineOffset += scanlineStride;
        }
    }

    /**
     * Sets a sample in the specified band for the pixel located at (x,y)
     * in the DataBuffer using an int for input.
     * ArrayIndexOutOfBoundsException may be thrown if the coordinates are
     * not in bounds.
     * @param x         The X coordinate of the pixel location.
     * @param y         The Y coordinate of the pixel location.
     * @param b         The band to set.
     * @param s         The input sample as an int.
     * @param data      The DataBuffer containing the image data.
     * @see #getSample(int, int, int, DataBuffer)
     */
    public void setSample(int x, int y, int b, int s,
                          DataBuffer data) {
        // Bounds check for 'b' will be performed automatically
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int value = data.getElem(y*scanlineStride + x);
        value &= ~bitMasks[b];
        value |= (s << bitOffsets[b]) & bitMasks[b];
        data.setElem(y*scanlineStride + x,value);
    }

    /**
     * Sets the samples in the specified band for the specified rectangle
     * of pixels from an int array containing one sample per array element.
     * ArrayIndexOutOfBoundsException may be thrown if the coordinates are
     * not in bounds.
     * @param x         The X coordinate of the upper left pixel location.
     * @param y         The Y coordinate of the upper left pixel location.
     * @param w         The width of the pixel rectangle.
     * @param h         The height of the pixel rectangle.
     * @param b         The band to set.
     * @param iArray    The input samples in an int array.
     * @param data      The DataBuffer containing the image data.
     * @see #getSamples(int, int, int, int, int, int[], DataBuffer)
     */
    public void setSamples(int x, int y, int w, int h, int b,
                          int[] iArray, DataBuffer data) {
        // Bounds check for 'b' will be performed automatically
        if ((x < 0) || (y < 0) || (x + w > width) || (y + h > height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int lineOffset = y*scanlineStride + x;
        int srcOffset = 0;

        for (int i = 0; i < h; i++) {
           for (int j = 0; j < w; j++) {
              int value = data.getElem(lineOffset+j);
              value &= ~bitMasks[b];
              int sample = iArray[srcOffset++];
              value |= (sample << bitOffsets[b]) & bitMasks[b];
              data.setElem(lineOffset+j,value);
           }
           lineOffset += scanlineStride;
        }
    }

    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof SinglePixelPackedSampleModel)) {
            return false;
        }

        SinglePixelPackedSampleModel that = (SinglePixelPackedSampleModel)o;
        return this.width == that.width &&
            this.height == that.height &&
            this.numBands == that.numBands &&
            this.dataType == that.dataType &&
            Arrays.equals(this.bitMasks, that.bitMasks) &&
            Arrays.equals(this.bitOffsets, that.bitOffsets) &&
            Arrays.equals(this.bitSizes, that.bitSizes) &&
            this.maxBitSize == that.maxBitSize &&
            this.scanlineStride == that.scanlineStride;
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
        for (int i = 0; i < bitMasks.length; i++) {
            hash ^= bitMasks[i];
            hash <<= 8;
        }
        for (int i = 0; i < bitOffsets.length; i++) {
            hash ^= bitOffsets[i];
            hash <<= 8;
        }
        for (int i = 0; i < bitSizes.length; i++) {
            hash ^= bitSizes[i];
            hash <<= 8;
        }
        hash ^= maxBitSize;
        hash <<= 8;
        hash ^= scanlineStride;
        return hash;
    }
}
