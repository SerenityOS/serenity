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

/**
 * The {@code MultiPixelPackedSampleModel} class represents
 * one-banded images and can pack multiple one-sample
 * pixels into one data element.  Pixels are not allowed to span data elements.
 * The data type can be DataBuffer.TYPE_BYTE, DataBuffer.TYPE_USHORT,
 * or DataBuffer.TYPE_INT.  Each pixel must be a power of 2 number of bits
 * and a power of 2 number of pixels must fit exactly in one data element.
 * Pixel bit stride is equal to the number of bits per pixel.  Scanline
 * stride is in data elements and the last several data elements might be
 * padded with unused pixels.  Data bit offset is the offset in bits from
 * the beginning of the {@link DataBuffer} to the first pixel and must be
 * a multiple of pixel bit stride.
 * <p>
 * The following code illustrates extracting the bits for pixel
 * <code>x,&nbsp;y</code> from {@code DataBuffer data}
 * and storing the pixel data in data elements of type
 * {@code dataType}:
 * <pre>{@code
 *      int dataElementSize = DataBuffer.getDataTypeSize(dataType);
 *      int bitnum = dataBitOffset + x*pixelBitStride;
 *      int element = data.getElem(y*scanlineStride + bitnum/dataElementSize);
 *      int shift = dataElementSize - (bitnum & (dataElementSize-1))
 *                  - pixelBitStride;
 *      int pixel = (element >> shift) & ((1 << pixelBitStride) - 1);
 * }</pre>
 */

public class MultiPixelPackedSampleModel extends SampleModel
{
    /** The number of bits from one pixel to the next. */
    int pixelBitStride;

    /** Bitmask that extracts the rightmost pixel of a data element. */
    int bitMask;

    /**
      * The number of pixels that fit in a data element.  Also used
      * as the number of bits per pixel.
      */
    int pixelsPerDataElement;

    /** The size of a data element in bits. */
    int dataElementSize;

    /** The bit offset into the data array where the first pixel begins.
     */
    int dataBitOffset;

    /** ScanlineStride of the data buffer described in data array elements. */
    int scanlineStride;

    /**
     * Constructs a {@code MultiPixelPackedSampleModel} with the
     * specified data type, width, height and number of bits per pixel.
     * @param dataType  the data type for storing samples
     * @param w         the width, in pixels, of the region of
     *                  image data described
     * @param h         the height, in pixels, of the region of
     *                  image data described
     * @param numberOfBits the number of bits per pixel
     * @throws IllegalArgumentException if {@code dataType} is not
     *         either {@code DataBuffer.TYPE_BYTE},
     *         {@code DataBuffer.TYPE_USHORT}, or
     *         {@code DataBuffer.TYPE_INT}
     */
    public MultiPixelPackedSampleModel(int dataType,
                                       int w,
                                       int h,
                                       int numberOfBits) {
        this(dataType,w,h,
             numberOfBits,
            (w*numberOfBits+DataBuffer.getDataTypeSize(dataType)-1)/
                DataBuffer.getDataTypeSize(dataType),
             0);
        if (dataType != DataBuffer.TYPE_BYTE &&
            dataType != DataBuffer.TYPE_USHORT &&
            dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Unsupported data type "+
                                               dataType);
        }
    }

    /**
     * Constructs a {@code MultiPixelPackedSampleModel} with
     * specified data type, width, height, number of bits per pixel,
     * scanline stride and data bit offset.
     * @param dataType  the data type for storing samples
     * @param w         the width, in pixels, of the region of
     *                  image data described
     * @param h         the height, in pixels, of the region of
     *                  image data described
     * @param numberOfBits the number of bits per pixel
     * @param scanlineStride the line stride of the image data
     * @param dataBitOffset the data bit offset for the region of image
     *                  data described
     * @exception RasterFormatException if the number of bits per pixel
     *                  is not a power of 2 or if a power of 2 number of
     *                  pixels do not fit in one data element.
     * @throws IllegalArgumentException if {@code w} or
     *         {@code h} is not greater than 0
     * @throws IllegalArgumentException if {@code dataType} is not
     *         either {@code DataBuffer.TYPE_BYTE},
     *         {@code DataBuffer.TYPE_USHORT}, or
     *         {@code DataBuffer.TYPE_INT}
     */
    public MultiPixelPackedSampleModel(int dataType, int w, int h,
                                       int numberOfBits,
                                       int scanlineStride,
                                       int dataBitOffset) {
        super(dataType, w, h, 1);
        if (dataType != DataBuffer.TYPE_BYTE &&
            dataType != DataBuffer.TYPE_USHORT &&
            dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Unsupported data type "+
                                               dataType);
        }
        this.dataType = dataType;
        this.pixelBitStride = numberOfBits;
        this.scanlineStride = scanlineStride;
        this.dataBitOffset = dataBitOffset;
        this.dataElementSize = DataBuffer.getDataTypeSize(dataType);
        this.pixelsPerDataElement = dataElementSize/numberOfBits;
        if (pixelsPerDataElement*numberOfBits != dataElementSize) {
           throw new RasterFormatException("MultiPixelPackedSampleModel " +
                                             "does not allow pixels to " +
                                             "span data element boundaries");
        }
        this.bitMask = (1 << numberOfBits) - 1;
    }


    /**
     * Creates a new {@code MultiPixelPackedSampleModel} with the
     * specified width and height.  The new
     * {@code MultiPixelPackedSampleModel} has the
     * same storage data type and number of bits per pixel as this
     * {@code MultiPixelPackedSampleModel}.
     * @param w the specified width
     * @param h the specified height
     * @return a {@link SampleModel} with the specified width and height
     * and with the same storage data type and number of bits per pixel
     * as this {@code MultiPixelPackedSampleModel}.
     * @throws IllegalArgumentException if {@code w} or
     *         {@code h} is not greater than 0
     */
    public SampleModel createCompatibleSampleModel(int w, int h) {
      SampleModel sampleModel =
            new MultiPixelPackedSampleModel(dataType, w, h, pixelBitStride);
      return sampleModel;
    }

    /**
     * Creates a {@code DataBuffer} that corresponds to this
     * {@code MultiPixelPackedSampleModel}.  The
     * {@code DataBuffer} object's data type and size
     * is consistent with this {@code MultiPixelPackedSampleModel}.
     * The {@code DataBuffer} has a single bank.
     * @return a {@code DataBuffer} with the same data type and
     * size as this {@code MultiPixelPackedSampleModel}.
     */
    public DataBuffer createDataBuffer() {
        DataBuffer dataBuffer = null;

        int size = scanlineStride*height;
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            dataBuffer = new DataBufferByte(size+(dataBitOffset+7)/8);
            break;
        case DataBuffer.TYPE_USHORT:
            dataBuffer = new DataBufferUShort(size+(dataBitOffset+15)/16);
            break;
        case DataBuffer.TYPE_INT:
            dataBuffer = new DataBufferInt(size+(dataBitOffset+31)/32);
            break;
        }
        return dataBuffer;
    }

    /**
     * Returns the number of data elements needed to transfer one pixel
     * via the {@link #getDataElements} and {@link #setDataElements}
     * methods.  For a {@code MultiPixelPackedSampleModel}, this is
     * one.
     * @return the number of data elements.
     */
    public int getNumDataElements() {
        return 1;
    }

    /**
     * Returns the number of bits per sample for all bands.
     * @return the number of bits per sample.
     */
    public int[] getSampleSize() {
        int[] sampleSize = {pixelBitStride};
        return sampleSize;
    }

    /**
     * Returns the number of bits per sample for the specified band.
     * @param band the specified band
     * @return the number of bits per sample for the specified band.
     */
    public int getSampleSize(int band) {
        return pixelBitStride;
    }

    /**
     * Returns the offset of pixel (x,&nbsp;y) in data array elements.
     * @param x the X coordinate of the specified pixel
     * @param y the Y coordinate of the specified pixel
     * @return the offset of the specified pixel.
     */
    public int getOffset(int x, int y) {
        int offset = y * scanlineStride;
        offset +=  (x*pixelBitStride+dataBitOffset)/dataElementSize;
        return offset;
    }

    /**
     *  Returns the offset, in bits, into the data element in which it is
     *  stored for the {@code x}th pixel of a scanline.
     *  This offset is the same for all scanlines.
     *  @param x the specified pixel
     *  @return the bit offset of the specified pixel.
     */
    public int getBitOffset(int x){
       return  (x*pixelBitStride+dataBitOffset)%dataElementSize;
    }

    /**
     * Returns the scanline stride.
     * @return the scanline stride of this
     * {@code MultiPixelPackedSampleModel}.
     */
    public int getScanlineStride() {
        return scanlineStride;
    }

    /**
     * Returns the pixel bit stride in bits.  This value is the same as
     * the number of bits per pixel.
     * @return the {@code pixelBitStride} of this
     * {@code MultiPixelPackedSampleModel}.
     */
    public int getPixelBitStride() {
        return pixelBitStride;
    }

    /**
     * Returns the data bit offset in bits.
     * @return the {@code dataBitOffset} of this
     * {@code MultiPixelPackedSampleModel}.
     */
    public int getDataBitOffset() {
        return dataBitOffset;
    }

    /**
     *  Returns the TransferType used to transfer pixels by way of the
     *  {@code getDataElements} and {@code setDataElements}
     *  methods. The TransferType might or might not be the same as the
     *  storage DataType.  The TransferType is one of
     *  DataBuffer.TYPE_BYTE, DataBuffer.TYPE_USHORT,
     *  or DataBuffer.TYPE_INT.
     *  @return the transfertype.
     */
    public int getTransferType() {
        if (pixelBitStride > 16)
            return DataBuffer.TYPE_INT;
        else if (pixelBitStride > 8)
            return DataBuffer.TYPE_USHORT;
        else
            return DataBuffer.TYPE_BYTE;
    }

    /**
     * Creates a new {@code MultiPixelPackedSampleModel} with a
     * subset of the bands of this
     * {@code MultiPixelPackedSampleModel}.  Since a
     * {@code MultiPixelPackedSampleModel} only has one band, the
     * bands argument must have a length of one and indicate the zeroth
     * band.
     * @param bands the specified bands
     * @return a new {@code SampleModel} with a subset of bands of
     * this {@code MultiPixelPackedSampleModel}.
     * @exception RasterFormatException if the number of bands requested
     * is not one.
     * @throws IllegalArgumentException if {@code w} or
     *         {@code h} is not greater than 0
     */
    public SampleModel createSubsetSampleModel(int[] bands) {
        if (bands != null) {
           if (bands.length != 1)
            throw new RasterFormatException("MultiPixelPackedSampleModel has "
                                            + "only one band.");
        }
        SampleModel sm = createCompatibleSampleModel(width, height);
        return sm;
    }

    /**
     * Returns as {@code int} the sample in a specified band for the
     * pixel located at (x,&nbsp;y).  An
     * {@code ArrayIndexOutOfBoundsException} is thrown if the
     * coordinates are not in bounds.
     * @param x         the X coordinate of the specified pixel
     * @param y         the Y coordinate of the specified pixel
     * @param b         the band to return, which is assumed to be 0
     * @param data      the {@code DataBuffer} containing the image
     *                  data
     * @return the specified band containing the sample of the specified
     * pixel.
     * @exception ArrayIndexOutOfBoundsException if the specified
     *          coordinates are not in bounds.
     * @see #setSample(int, int, int, int, DataBuffer)
     */
    public int getSample(int x, int y, int b, DataBuffer data) {
        // 'b' must be 0
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height) ||
            (b != 0)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int bitnum = dataBitOffset + x*pixelBitStride;
        int element = data.getElem(y*scanlineStride + bitnum/dataElementSize);
        int shift = dataElementSize - (bitnum & (dataElementSize-1))
                    - pixelBitStride;
        return (element >> shift) & bitMask;
    }

    /**
     * Sets a sample in the specified band for the pixel located at
     * (x,&nbsp;y) in the {@code DataBuffer} using an
     * {@code int} for input.
     * An {@code ArrayIndexOutOfBoundsException} is thrown if the
     * coordinates are not in bounds.
     * @param x the X coordinate of the specified pixel
     * @param y the Y coordinate of the specified pixel
     * @param b the band to return, which is assumed to be 0
     * @param s the input sample as an {@code int}
     * @param data the {@code DataBuffer} where image data is stored
     * @exception ArrayIndexOutOfBoundsException if the coordinates are
     * not in bounds.
     * @see #getSample(int, int, int, DataBuffer)
     */
    public void setSample(int x, int y, int b, int s,
                          DataBuffer data) {
        // 'b' must be 0
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height) ||
            (b != 0)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int bitnum = dataBitOffset + x * pixelBitStride;
        int index = y * scanlineStride + (bitnum / dataElementSize);
        int shift = dataElementSize - (bitnum & (dataElementSize-1))
                    - pixelBitStride;
        int element = data.getElem(index);
        element &= ~(bitMask << shift);
        element |= (s & bitMask) << shift;
        data.setElem(index,element);
    }

    /**
     * Returns data for a single pixel in a primitive array of type
     * TransferType.  For a {@code MultiPixelPackedSampleModel},
     * the array has one element, and the type is the smallest of
     * DataBuffer.TYPE_BYTE, DataBuffer.TYPE_USHORT, or DataBuffer.TYPE_INT
     * that can hold a single pixel.  Generally, {@code obj}
     * should be passed in as {@code null}, so that the
     * {@code Object} is created automatically and is the
     * correct primitive data type.
     * <p>
     * The following code illustrates transferring data for one pixel from
     * {@code DataBuffer db1}, whose storage layout is
     * described by {@code MultiPixelPackedSampleModel}
     * {@code mppsm1}, to {@code DataBuffer db2},
     * whose storage layout is described by
     * {@code MultiPixelPackedSampleModel mppsm2}.
     * The transfer is generally more efficient than using
     * {@code getPixel} or {@code setPixel}.
     * <pre>
     *       MultiPixelPackedSampleModel mppsm1, mppsm2;
     *       DataBufferInt db1, db2;
     *       mppsm2.setDataElements(x, y, mppsm1.getDataElements(x, y, null,
     *                              db1), db2);
     * </pre>
     * Using {@code getDataElements} or {@code setDataElements}
     * to transfer between two {@code DataBuffer/SampleModel} pairs
     * is legitimate if the {@code SampleModels} have the same number
     * of bands, corresponding bands have the same number of
     * bits per sample, and the TransferTypes are the same.
     * <p>
     * If {@code obj} is not {@code null}, it should be a
     * primitive array of type TransferType.  Otherwise, a
     * {@code ClassCastException} is thrown.  An
     * {@code ArrayIndexOutOfBoundsException} is thrown if the
     * coordinates are not in bounds, or if {@code obj} is not
     * {@code null} and is not large enough to hold the pixel data.
     * @param x the X coordinate of the specified pixel
     * @param y the Y coordinate of the specified pixel
     * @param obj a primitive array in which to return the pixel data or
     *          {@code null}.
     * @param data the {@code DataBuffer} containing the image data.
     * @return an {@code Object} containing data for the specified
     *  pixel.
     * @exception ClassCastException if {@code obj} is not a
     *  primitive array of type TransferType or is not {@code null}
     * @exception ArrayIndexOutOfBoundsException if the coordinates are
     * not in bounds, or if {@code obj} is not {@code null} or
     * not large enough to hold the pixel data
     * @see #setDataElements(int, int, Object, DataBuffer)
     */
    public Object getDataElements(int x, int y, Object obj, DataBuffer data) {
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }

        int type = getTransferType();
        int bitnum = dataBitOffset + x*pixelBitStride;
        int shift = dataElementSize - (bitnum & (dataElementSize-1))
                    - pixelBitStride;
        int element = 0;

        switch(type) {

        case DataBuffer.TYPE_BYTE:

            byte[] bdata;

            if (obj == null)
                bdata = new byte[1];
            else
                bdata = (byte[])obj;

            element = data.getElem(y*scanlineStride +
                                    bitnum/dataElementSize);
            bdata[0] = (byte)((element >> shift) & bitMask);

            obj = (Object)bdata;
            break;

        case DataBuffer.TYPE_USHORT:

            short[] sdata;

            if (obj == null)
                sdata = new short[1];
            else
                sdata = (short[])obj;

            element = data.getElem(y*scanlineStride +
                                   bitnum/dataElementSize);
            sdata[0] = (short)((element >> shift) & bitMask);

            obj = (Object)sdata;
            break;

        case DataBuffer.TYPE_INT:

            int[] idata;

            if (obj == null)
                idata = new int[1];
            else
                idata = (int[])obj;

            element = data.getElem(y*scanlineStride +
                                   bitnum/dataElementSize);
            idata[0] = (element >> shift) & bitMask;

            obj = (Object)idata;
            break;
        }

        return obj;
    }

    /**
     * Returns the specified single band pixel in the first element
     * of an {@code int} array.
     * {@code ArrayIndexOutOfBoundsException} is thrown if the
     * coordinates are not in bounds.
     * @param x the X coordinate of the specified pixel
     * @param y the Y coordinate of the specified pixel
     * @param iArray the array containing the pixel to be returned or
     *  {@code null}
     * @param data the {@code DataBuffer} where image data is stored
     * @return an array containing the specified pixel.
     * @exception ArrayIndexOutOfBoundsException if the coordinates
     *  are not in bounds
     * @see #setPixel(int, int, int[], DataBuffer)
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
        int bitnum = dataBitOffset + x*pixelBitStride;
        int element = data.getElem(y*scanlineStride + bitnum/dataElementSize);
        int shift = dataElementSize - (bitnum & (dataElementSize-1))
                    - pixelBitStride;
        pixels[0] = (element >> shift) & bitMask;
        return pixels;
    }

    /**
     * Sets the data for a single pixel in the specified
     * {@code DataBuffer} from a primitive array of type
     * TransferType.  For a {@code MultiPixelPackedSampleModel},
     * only the first element of the array holds valid data,
     * and the type must be the smallest of
     * DataBuffer.TYPE_BYTE, DataBuffer.TYPE_USHORT, or DataBuffer.TYPE_INT
     * that can hold a single pixel.
     * <p>
     * The following code illustrates transferring data for one pixel from
     * {@code DataBuffer db1}, whose storage layout is
     * described by {@code MultiPixelPackedSampleModel}
     * {@code mppsm1}, to {@code DataBuffer db2},
     * whose storage layout is described by
     * {@code MultiPixelPackedSampleModel mppsm2}.
     * The transfer is generally more efficient than using
     * {@code getPixel} or {@code setPixel}.
     * <pre>
     *       MultiPixelPackedSampleModel mppsm1, mppsm2;
     *       DataBufferInt db1, db2;
     *       mppsm2.setDataElements(x, y, mppsm1.getDataElements(x, y, null,
     *                              db1), db2);
     * </pre>
     * Using {@code getDataElements} or {@code setDataElements} to
     * transfer between two {@code DataBuffer/SampleModel} pairs is
     * legitimate if the {@code SampleModel} objects have
     * the same number of bands, corresponding bands have the same number of
     * bits per sample, and the TransferTypes are the same.
     * <p>
     * {@code obj} must be a primitive array of type TransferType.
     * Otherwise, a {@code ClassCastException} is thrown.  An
     * {@code ArrayIndexOutOfBoundsException} is thrown if the
     * coordinates are not in bounds, or if {@code obj} is not large
     * enough to hold the pixel data.
     * @param x the X coordinate of the pixel location
     * @param y the Y coordinate of the pixel location
     * @param obj a primitive array containing pixel data
     * @param data the {@code DataBuffer} containing the image data
     * @see #getDataElements(int, int, Object, DataBuffer)
     */
    public void setDataElements(int x, int y, Object obj, DataBuffer data) {
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }

        int type = getTransferType();
        int bitnum = dataBitOffset + x * pixelBitStride;
        int index = y * scanlineStride + (bitnum / dataElementSize);
        int shift = dataElementSize - (bitnum & (dataElementSize-1))
                    - pixelBitStride;
        int element = data.getElem(index);
        element &= ~(bitMask << shift);

        switch(type) {

        case DataBuffer.TYPE_BYTE:

            byte[] barray = (byte[])obj;
            element |= ( ((int)(barray[0])&0xff) & bitMask) << shift;
            data.setElem(index, element);
            break;

        case DataBuffer.TYPE_USHORT:

            short[] sarray = (short[])obj;
            element |= ( ((int)(sarray[0])&0xffff) & bitMask) << shift;
            data.setElem(index, element);
            break;

        case DataBuffer.TYPE_INT:

            int[] iarray = (int[])obj;
            element |= (iarray[0] & bitMask) << shift;
            data.setElem(index, element);
            break;
        }
    }

    /**
     * Sets a pixel in the {@code DataBuffer} using an
     * {@code int} array for input.
     * {@code ArrayIndexOutOfBoundsException} is thrown if
     * the coordinates are not in bounds.
     * @param x the X coordinate of the pixel location
     * @param y the Y coordinate of the pixel location
     * @param iArray the input pixel in an {@code int} array
     * @param data the {@code DataBuffer} containing the image data
     * @see #getPixel(int, int, int[], DataBuffer)
     */
    public void setPixel(int x, int y, int[] iArray, DataBuffer data) {
        if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int bitnum = dataBitOffset + x * pixelBitStride;
        int index = y * scanlineStride + (bitnum / dataElementSize);
        int shift = dataElementSize - (bitnum & (dataElementSize-1))
                    - pixelBitStride;
        int element = data.getElem(index);
        element &= ~(bitMask << shift);
        element |= (iArray[0] & bitMask) << shift;
        data.setElem(index,element);
    }

    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof MultiPixelPackedSampleModel)) {
            return false;
        }

        MultiPixelPackedSampleModel that = (MultiPixelPackedSampleModel)o;
        return this.width == that.width &&
            this.height == that.height &&
            this.numBands == that.numBands &&
            this.dataType == that.dataType &&
            this.pixelBitStride == that.pixelBitStride &&
            this.bitMask == that.bitMask &&
            this.pixelsPerDataElement == that.pixelsPerDataElement &&
            this.dataElementSize == that.dataElementSize &&
            this.dataBitOffset == that.dataBitOffset &&
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
        hash ^= pixelBitStride;
        hash <<= 8;
        hash ^= bitMask;
        hash <<= 8;
        hash ^= pixelsPerDataElement;
        hash <<= 8;
        hash ^= dataElementSize;
        hash <<= 8;
        hash ^= dataBitOffset;
        hash <<= 8;
        hash ^= scanlineStride;
        return hash;
    }
}
