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

package sun.awt.image;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RasterFormatException;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

/**
 * This class is useful for describing 1, 2, or 4 bit image data
 * elements.  This raster has one band whose pixels are packed
 * together into individual bytes in a single byte array.  This type
 * of raster can be used with an IndexColorModel. This raster uses a
 * MultiPixelPackedSampleModel.
 *
 */
public class BytePackedRaster extends SunWritableRaster {

    /** The data bit offset for each pixel. */
    int           dataBitOffset;

    /** Scanline stride of the image data contained in this Raster. */
    int           scanlineStride;

    /**
     * The bit stride of a pixel, equal to the total number of bits
     * required to store a pixel.
     */
    int           pixelBitStride;

    /** The bit mask for extracting the pixel. */
    int           bitMask;

    /** The image data array. */
    byte[]        data;

    /** 8 minus the pixel bit stride. */
    int shiftOffset;

    int type;

    /** A cached copy of minX + width for use in bounds checks. */
    private int maxX;

    /** A cached copy of minY + height for use in bounds checks. */
    private int maxY;

    private static native void initIDs();
    static {
        /* ensure that the necessary native libraries are loaded */
        NativeLibLoader.loadLibraries();
        initIDs();
    }

    /**
     * Constructs a BytePackedRaster with the given SampleModel.
     * The Raster's upper left corner is origin and it is the same
     * size as the SampleModel.  A DataBuffer large enough to describe the
     * Raster is automatically created.  SampleModel must be of type
     * MultiPixelPackedSampleModel.
     * @param sampleModel     The SampleModel that specifies the layout.
     * @param origin          The Point that specified the origin.
     */
    public BytePackedRaster(SampleModel sampleModel, Point origin) {
        this(sampleModel,
             (DataBufferByte) sampleModel.createDataBuffer(),
             new Rectangle(origin.x,
                           origin.y,
                           sampleModel.getWidth(),
                           sampleModel.getHeight()),
             origin,
             null);
    }

    /**
     * Constructs a BytePackedRaster with the given SampleModel
     * and DataBuffer.  The Raster's upper left corner is origin and
     * it is the same size as the SampleModel.  The DataBuffer is not
     * initialized and must be a DataBufferByte compatible with SampleModel.
     * SampleModel must be of type MultiPixelPackedSampleModel.
     * @param sampleModel     The SampleModel that specifies the layout.
     * @param dataBuffer      The DataBufferByte that contains the image data.
     * @param origin          The Point that specifies the origin.
     */
    public BytePackedRaster(SampleModel sampleModel,
                            DataBufferByte dataBuffer,
                            Point origin)
    {
        this(sampleModel,
             dataBuffer,
             new Rectangle(origin.x,
                           origin.y,
                           sampleModel.getWidth(),
                           sampleModel.getHeight()),
             origin,
             null);
    }

    /**
     * Constructs a BytePackedRaster with the given SampleModel,
     * DataBuffer, and parent.  DataBuffer must be a DataBufferByte and
     * SampleModel must be of type MultiPixelPackedSampleModel.
     * When translated into the base Raster's
     * coordinate system, aRegion must be contained by the base Raster.
     * Origin is the coordinate in the new Raster's coordinate system of
     * the origin of the base Raster.  (The base Raster is the Raster's
     * ancestor which has no parent.)
     *
     * Note that this constructor should generally be called by other
     * constructors or create methods, it should not be used directly.
     * @param sampleModel     The SampleModel that specifies the layout.
     * @param dataBuffer      The DataBufferByte that contains the image data.
     * @param aRegion         The Rectangle that specifies the image area.
     * @param origin          The Point that specifies the origin.
     * @param parent          The parent (if any) of this raster.
     *
     * @exception RasterFormatException if the parameters do not conform
     * to requirements of this Raster type.
     */
    public BytePackedRaster(SampleModel sampleModel,
                            DataBufferByte dataBuffer,
                            Rectangle aRegion,
                            Point origin,
                            BytePackedRaster parent)
    {
        super(sampleModel,dataBuffer,aRegion,origin, parent);
        this.maxX = minX + width;
        this.maxY = minY + height;

        this.data = stealData(dataBuffer, 0);
        if (dataBuffer.getNumBanks() != 1) {
            throw new
                RasterFormatException("DataBuffer for BytePackedRasters"+
                                      " must only have 1 bank.");
        }
        int dbOffset = dataBuffer.getOffset();

        if (sampleModel instanceof MultiPixelPackedSampleModel) {
            MultiPixelPackedSampleModel mppsm =
                (MultiPixelPackedSampleModel)sampleModel;
            this.type = IntegerComponentRaster.TYPE_BYTE_BINARY_SAMPLES;
            pixelBitStride = mppsm.getPixelBitStride();
            if (pixelBitStride != 1 &&
                pixelBitStride != 2 &&
                pixelBitStride != 4) {
                throw new RasterFormatException
                  ("BytePackedRasters must have a bit depth of 1, 2, or 4");
            }
            scanlineStride = mppsm.getScanlineStride();
            dataBitOffset = mppsm.getDataBitOffset() + dbOffset*8;
            int xOffset = aRegion.x - origin.x;
            int yOffset = aRegion.y - origin.y;
            dataBitOffset += xOffset*pixelBitStride + yOffset*scanlineStride*8;
            bitMask = (1 << pixelBitStride) -1;
            shiftOffset = 8 - pixelBitStride;
        } else {
            throw new RasterFormatException("BytePackedRasters must have"+
                "MultiPixelPackedSampleModel");
        }
        verify(false);
    }

    /**
     * Returns the data bit offset for the Raster.  The data
     * bit offset is the bit index into the data array element
     * corresponding to the first sample of the first scanline.
     */
    public int getDataBitOffset() {
        return dataBitOffset;
    }

    /**
     * Returns the scanline stride -- the number of data array elements between
     * a given sample and the sample in the same column
     * of the next row.
     */
    public int getScanlineStride() {
        return scanlineStride;
    }

    /**
     * Returns pixel bit stride -- the number of bits between two
     * samples on the same scanline.
     */
    public int getPixelBitStride() {
        return pixelBitStride;
    }

    /**
     * Returns a reference to the entire data array.
     */
    public byte[] getDataStorage() {
        return data;
    }

    /**
     * Returns the data element at the specified
     * location.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinate is out of bounds.
     * A ClassCastException will be thrown if the input object is non null
     * and references anything other than an array of transferType.
     * @param x        The X coordinate of the pixel location.
     * @param y        The Y coordinate of the pixel location.
     * @param obj      An object reference to an array of type defined by
     *                 getTransferType() and length getNumDataElements().
     *                 If null an array of appropriate type and size will be
     *                 allocated.
     * @return         An object reference to an array of type defined by
     *                 getTransferType() with the request pixel data.
     */
    public Object getDataElements(int x, int y, Object obj) {
        if ((x < this.minX) || (y < this.minY) ||
            (x >= this.maxX) || (y >= this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        byte[] outData;
        if (obj == null) {
            outData = new byte[numDataElements];
        } else {
            outData = (byte[])obj;
        }
        int bitnum = dataBitOffset + (x-minX) * pixelBitStride;
        // Fix 4184283
        int element = data[(y-minY) * scanlineStride + (bitnum >> 3)] & 0xff;
        int shift = shiftOffset - (bitnum & 7);
        outData[0] = (byte)((element >> shift) & bitMask);
        return outData;
    }

    /**
     * Returns the pixel data for the specified rectangle of pixels in a
     * primitive array of type TransferType.
     * For image data supported by the Java 2D API, this
     * will be one of DataBuffer.TYPE_BYTE, DataBuffer.TYPE_USHORT, or
     * DataBuffer.TYPE_INT.  Data may be returned in a packed format,
     * thus increasing efficiency for data transfers.
     *
     * An ArrayIndexOutOfBoundsException may be thrown
     * if the coordinates are not in bounds.
     * A ClassCastException will be thrown if the input object is non null
     * and references anything other than an array of TransferType.
     * @see java.awt.image.SampleModel#getDataElements(int, int, int, int, Object, DataBuffer)
     * @param x        The X coordinate of the upper left pixel location.
     * @param y        The Y coordinate of the upper left pixel location.
     * @param w        Width of the pixel rectangle.
     * @param h        Height of the pixel rectangle.
     * @param outData  An object reference to an array of type defined by
     *                 getTransferType() and length w*h*getNumDataElements().
     *                 If null, an array of appropriate type and size will be
     *                 allocated.
     * @return         An object reference to an array of type defined by
     *                 getTransferType() with the requested pixel data.
     */
    public Object getDataElements(int x, int y, int w, int h,
                                  Object outData) {
        return getByteData(x, y, w, h, (byte[])outData);
    }

    /**
     * Returns an array  of data elements from the specified rectangular
     * region.
     *
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinates are out of bounds.
     * A ClassCastException will be thrown if the input object is non null
     * and references anything other than an array of transferType.
     * <pre>
     *       byte[] bandData = (byte[])raster.getPixelData(x, y, w, h, null);
     *       int pixel;
     *       // To find a data element at location (x2, y2)
     *       pixel = bandData[((y2-y)*w + (x2-x))];
     * </pre>
     * @param x        The X coordinate of the upper left pixel location.
     * @param y        The Y coordinate of the upper left pixel location.
     * @param w        Width of the pixel rectangle.
     * @param h        Height of the pixel rectangle.
     * @param obj      An object reference to an array of type defined by
     *                 getTransferType() and length w*h*getNumDataElements().
     *                 If null an array of appropriate type and size will be
     *                 allocated.
     * @return         An object reference to an array of type defined by
     *                 getTransferType() with the request pixel data.
     */
    public Object getPixelData(int x, int y, int w, int h, Object obj) {
        if ((x < this.minX) || (y < this.minY) ||
            (x + w > this.maxX) || (y + h > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        byte[] outData;
        if (obj == null) {
            outData = new byte[numDataElements*w*h];
        } else {
            outData = (byte[])obj;
        }
        int pixbits = pixelBitStride;
        int scanbit = dataBitOffset + (x-minX) * pixbits;
        int index = (y-minY) * scanlineStride;
        int outindex = 0;
        byte[] data = this.data;

        for (int j = 0; j < h; j++) {
            int bitnum = scanbit;
            for (int i = 0; i < w; i++) {
                int shift = shiftOffset - (bitnum & 7);
                outData[outindex++] =
                    (byte)(bitMask & (data[index + (bitnum >> 3)] >> shift));
                bitnum += pixbits;
            }
            index += scanlineStride;
        }
        return outData;
    }

    /**
     * Returns a byte array containing the specified data elements
     * from the data array.  The band index will be ignored.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinates are out of bounds.
     * <pre>
     *       byte[] byteData = getByteData(x, y, band, w, h, null);
     *       // To find a data element at location (x2, y2)
     *       byte element = byteData[(y2-y)*w + (x2-x)];
     * </pre>
     * @param x        The X coordinate of the upper left pixel location.
     * @param y        The Y coordinate of the upper left pixel location.
     * @param w        Width of the pixel rectangle.
     * @param h        Height of the pixel rectangle.
     * @param band     The band to return, is ignored.
     * @param outData  If non-null, data elements
     *                 at the specified locations are returned in this array.
     * @return         Byte array with data elements.
     */
    public byte[] getByteData(int x, int y, int w, int h,
                              int band, byte[] outData) {
        return getByteData(x, y, w, h, outData);
    }

    /**
     * Returns a byte array containing the specified data elements
     * from the data array.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinates are out of bounds.
     * <pre>
     *       byte[] byteData = raster.getByteData(x, y, w, h, null);
     *       byte pixel;
     *       // To find a data element at location (x2, y2)
     *       pixel = byteData[((y2-y)*w + (x2-x))];
     * </pre>
     * @param x        The X coordinate of the upper left pixel location.
     * @param y        The Y coordinate of the upper left pixel location.
     * @param w        Width of the pixel rectangle.
     * @param h        Height of the pixel rectangle.
     * @param outData  If non-null, data elements
     *                 at the specified locations are returned in this array.
     * @return         Byte array with data elements.
     */
    public byte[] getByteData(int x, int y, int w, int h, byte[] outData) {
        if ((x < this.minX) || (y < this.minY) ||
            (x + w > this.maxX) || (y + h > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        if (outData == null) {
            outData = new byte[w * h];
        }
        int pixbits = pixelBitStride;
        int scanbit = dataBitOffset + (x-minX) * pixbits;
        int index = (y-minY) * scanlineStride;
        int outindex = 0;
        byte[] data = this.data;

        for (int j = 0; j < h; j++) {
            int bitnum = scanbit;
            int element;

            // Process initial portion of scanline
            int i = 0;
            while ((i < w) && ((bitnum & 7) != 0)) {
                int shift = shiftOffset - (bitnum & 7);
                outData[outindex++] =
                    (byte)(bitMask & (data[index + (bitnum >> 3)] >> shift));
                bitnum += pixbits;
                i++;
            }

            // Process central portion of scanline 8 pixels at a time
            int inIndex = index + (bitnum >> 3);
            switch (pixbits) {
            case 1:
                for (; i < w - 7; i += 8) {
                    element = data[inIndex++];
                    outData[outindex++] = (byte)((element >> 7) & 1);
                    outData[outindex++] = (byte)((element >> 6) & 1);
                    outData[outindex++] = (byte)((element >> 5) & 1);
                    outData[outindex++] = (byte)((element >> 4) & 1);
                    outData[outindex++] = (byte)((element >> 3) & 1);
                    outData[outindex++] = (byte)((element >> 2) & 1);
                    outData[outindex++] = (byte)((element >> 1) & 1);
                    outData[outindex++] = (byte)(element & 1);
                    bitnum += 8;
                }
                break;

            case 2:
                for (; i < w - 7; i += 8) {
                    element = data[inIndex++];
                    outData[outindex++] = (byte)((element >> 6) & 3);
                    outData[outindex++] = (byte)((element >> 4) & 3);
                    outData[outindex++] = (byte)((element >> 2) & 3);
                    outData[outindex++] = (byte)(element & 3);

                    element = data[inIndex++];
                    outData[outindex++] = (byte)((element >> 6) & 3);
                    outData[outindex++] = (byte)((element >> 4) & 3);
                    outData[outindex++] = (byte)((element >> 2) & 3);
                    outData[outindex++] = (byte)(element & 3);

                    bitnum += 16;
                }
                break;

            case 4:
                for (; i < w - 7; i += 8) {
                    element = data[inIndex++];
                    outData[outindex++] = (byte)((element >> 4) & 0xf);
                    outData[outindex++] = (byte)(element & 0xf);

                    element = data[inIndex++];
                    outData[outindex++] = (byte)((element >> 4) & 0xf);
                    outData[outindex++] = (byte)(element & 0xf);

                    element = data[inIndex++];
                    outData[outindex++] = (byte)((element >> 4) & 0xf);
                    outData[outindex++] = (byte)(element & 0xf);

                    element = data[inIndex++];
                    outData[outindex++] = (byte)((element >> 4) & 0xf);
                    outData[outindex++] = (byte)(element & 0xf);

                    bitnum += 32;
                }
                break;
            }

            // Process final portion of scanline
            for (; i < w; i++) {
                int shift = shiftOffset - (bitnum & 7);
                outData[outindex++] =
                    (byte) (bitMask & (data[index + (bitnum >> 3)] >> shift));
                bitnum += pixbits;
            }

            index += scanlineStride;
        }

        return outData;
    }

    /**
     * Stores the data elements at the specified location.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinate is out of bounds.
     * A ClassCastException will be thrown if the input object is non null
     * and references anything other than an array of transferType.
     * @param x        The X coordinate of the pixel location.
     * @param y        The Y coordinate of the pixel location.
     * @param obj      An object reference to an array of type defined by
     *                 getTransferType() and length getNumDataElements()
     *                 containing the pixel data to place at x,y.
     */
    public void setDataElements(int x, int y, Object obj) {
        if ((x < this.minX) || (y < this.minY) ||
            (x >= this.maxX) || (y >= this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        byte[] inData = (byte[])obj;
        int bitnum = dataBitOffset + (x-minX) * pixelBitStride;
        int index = (y-minY) * scanlineStride + (bitnum >> 3);
        int shift = shiftOffset - (bitnum & 7);

        byte element = data[index];
        element &= ~(bitMask << shift);
        element |= (inData[0] & bitMask) << shift;
        data[index] = element;

        markDirty();
    }

    /**
     * Stores the Raster data at the specified location.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinates are out of bounds.
     * @param x          The X coordinate of the pixel location.
     * @param y          The Y coordinate of the pixel location.
     * @param inRaster   Raster of data to place at x,y location.
     */
    public void setDataElements(int x, int y, Raster inRaster) {
        // Check if we can use fast code
        if (!(inRaster instanceof BytePackedRaster) ||
            ((BytePackedRaster)inRaster).pixelBitStride != pixelBitStride) {
            super.setDataElements(x, y, inRaster);
            return;
        }

        int srcOffX = inRaster.getMinX();
        int srcOffY = inRaster.getMinY();
        int dstOffX = srcOffX + x;
        int dstOffY = srcOffY + y;
        int width = inRaster.getWidth();
        int height = inRaster.getHeight();
        if ((dstOffX < this.minX) || (dstOffY < this.minY) ||
            (dstOffX + width > this.maxX) || (dstOffY + height > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        setDataElements(dstOffX, dstOffY,
                        srcOffX, srcOffY,
                        width, height,
                        (BytePackedRaster)inRaster);
    }

    /**
     * Stores the Raster data at the specified location.
     * @param dstX The absolute X coordinate of the destination pixel
     * that will receive a copy of the upper-left pixel of the
     * inRaster
     * @param dstY The absolute Y coordinate of the destination pixel
     * that will receive a copy of the upper-left pixel of the
     * inRaster
     * @param srcX The absolute X coordinate of the upper-left source
     * pixel that will be copied into this Raster
     * @param srcY The absolute Y coordinate of the upper-left source
     * pixel that will be copied into this Raster
     * @param width      The number of pixels to store horizontally
     * @param height     The number of pixels to store vertically
     * @param inRaster   BytePackedRaster of data to place at x,y location.
     */
    private void setDataElements(int dstX, int dstY,
                                 int srcX, int srcY,
                                 int width, int height,
                                 BytePackedRaster inRaster) {
        // Assume bounds checking has been performed previously
        if (width <= 0 || height <= 0) {
            return;
        }

        byte[] inData = inRaster.data;
        byte[] outData = this.data;

        int inscan = inRaster.scanlineStride;
        int outscan = this.scanlineStride;
        int inbit = inRaster.dataBitOffset +
                      8 * (srcY - inRaster.minY) * inscan +
                      (srcX - inRaster.minX) * inRaster.pixelBitStride;
        int outbit = (this.dataBitOffset +
                      8 * (dstY - minY) * outscan +
                      (dstX - minX) * this.pixelBitStride);
        int copybits = width * pixelBitStride;

        // Check whether the same bit alignment is present in both
        // Rasters; if so, we can copy whole bytes using
        // System.arraycopy.  If not, we must do a "funnel shift"
        // where adjacent bytes contribute to each destination byte.
        if ((inbit & 7) == (outbit & 7)) {
            // copy is bit aligned
            int bitpos = outbit & 7;
            if (bitpos != 0) {
                int bits = 8 - bitpos;
                // Copy partial bytes on left
                int inbyte = inbit >> 3;
                int outbyte = outbit >> 3;
                int mask = 0xff >> bitpos;
                if (copybits < bits) {
                    // Fix bug 4399076: previously had '8 - copybits' instead
                    // of 'bits - copybits'.
                    //
                    // Prior to the this expression, 'mask' has its rightmost
                    // 'bits' bits set to '1'.  We want it to have a total
                    // of 'copybits' bits set, therefore we want to introduce
                    // 'bits - copybits' zeroes on the right.
                    mask &= 0xff << (bits - copybits);
                    bits = copybits;
                }
                for (int j = 0; j < height; j++) {
                    int element = outData[outbyte];
                    element &= ~mask;
                    element |= (inData[inbyte] & mask);
                    outData[outbyte] = (byte) element;
                    inbyte += inscan;
                    outbyte += outscan;
                }
                inbit += bits;
                outbit += bits;
                copybits -= bits;
            }
            if (copybits >= 8) {
                // Copy whole bytes
                int inbyte = inbit >> 3;
                int outbyte = outbit >> 3;
                int copybytes = copybits >> 3;
                if (copybytes == inscan && inscan == outscan) {
                    System.arraycopy(inData, inbyte,
                                     outData, outbyte,
                                     inscan * height);
                } else {
                    for (int j = 0; j < height; j++) {
                        System.arraycopy(inData, inbyte,
                                         outData, outbyte,
                                         copybytes);
                        inbyte += inscan;
                        outbyte += outscan;
                    }
                }

                int bits = copybytes*8;
                inbit += bits;
                outbit += bits;
                copybits -= bits;
            }
            if (copybits > 0) {
                // Copy partial bytes on right
                int inbyte = inbit >> 3;
                int outbyte = outbit >> 3;
                int mask = (0xff00 >> copybits) & 0xff;
                for (int j = 0; j < height; j++) {
                    int element = outData[outbyte];
                    element &= ~mask;
                    element |= (inData[inbyte] & mask);
                    outData[outbyte] = (byte) element;
                    inbyte += inscan;
                    outbyte += outscan;
                }
            }
        } else {
            // Unaligned case, see RFE #4284166
            // Note that the code in that RFE is not correct

            // Insert bits into the first byte of the output
            // if either the starting bit position is not zero or
            // we are writing fewer than 8 bits in total
            int bitpos = outbit & 7;
            if (bitpos != 0 || copybits < 8) {
                int bits = 8 - bitpos;
                int inbyte = inbit >> 3;
                int outbyte = outbit >> 3;

                int lshift = inbit & 7;
                int rshift = 8 - lshift;
                int mask = 0xff >> bitpos;
                if (copybits < bits) {
                    // Fix mask if we're only writing a partial byte
                    mask &= 0xff << (bits - copybits);
                    bits = copybits;
                }
                int lastByte = inData.length - 1;
                for (int j = 0; j < height; j++) {
                    // Read two bytes from the source if possible
                    // Don't worry about going over a scanline boundary
                    // since any extra bits won't get used anyway
                    byte inData0 = inData[inbyte];
                    byte inData1 = (byte)0;
                    if (inbyte < lastByte) {
                        inData1 = inData[inbyte + 1];
                    }

                    // Insert the new bits into the output
                    int element = outData[outbyte];
                    element &= ~mask;
                    element |= (((inData0 << lshift) |
                                 ((inData1 & 0xff) >> rshift))
                                >> bitpos) & mask;
                    outData[outbyte] = (byte)element;
                    inbyte += inscan;
                    outbyte += outscan;
                }

                inbit += bits;
                outbit += bits;
                copybits -= bits;
            }

            // Now we have outbit & 7 == 0 so we can write
            // complete bytes for a while

            // Make sure we have work to do in the central loop
            // to avoid reading past the end of the scanline
            if (copybits >= 8) {
                int inbyte = inbit >> 3;
                int outbyte = outbit >> 3;
                int copybytes = copybits >> 3;
                int lshift = inbit & 7;
                int rshift = 8 - lshift;

                for (int j = 0; j < height; j++) {
                    int ibyte = inbyte + j*inscan;
                    int obyte = outbyte + j*outscan;

                    int inData0 = inData[ibyte];
                    // Combine adjacent bytes while 8 or more bits left
                    for (int i = 0; i < copybytes; i++) {
                        int inData1 = inData[ibyte + 1];
                        int val = (inData0 << lshift) |
                            ((inData1 & 0xff) >> rshift);
                        outData[obyte] = (byte)val;
                        inData0 = inData1;

                        ++ibyte;
                        ++obyte;
                    }
                }

                int bits = copybytes*8;
                inbit += bits;
                outbit += bits;
                copybits -= bits;
            }

            // Finish last byte
            if (copybits > 0) {
                int inbyte = inbit >> 3;
                int outbyte = outbit >> 3;
                int mask = (0xff00 >> copybits) & 0xff;
                int lshift = inbit & 7;
                int rshift = 8 - lshift;

                int lastByte = inData.length - 1;
                for (int j = 0; j < height; j++) {
                    byte inData0 = inData[inbyte];
                    byte inData1 = (byte)0;
                    if (inbyte < lastByte) {
                        inData1 = inData[inbyte + 1];
                    }

                    // Insert the new bits into the output
                    int element = outData[outbyte];
                    element &= ~mask;
                    element |= ((inData0 << lshift) |
                                ((inData1 & 0xff) >> rshift)) & mask;
                    outData[outbyte] = (byte)element;

                    inbyte += inscan;
                    outbyte += outscan;
                }
            }
        }

        markDirty();
    }

    /**
     * Copies pixels from Raster srcRaster to this WritableRaster.
     * For each (x, y) address in srcRaster, the corresponding pixel
     * is copied to address (x+dx, y+dy) in this WritableRaster,
     * unless (x+dx, y+dy) falls outside the bounds of this raster.
     * srcRaster must have the same number of bands as this WritableRaster.
     * The copy is a simple copy of source samples to the corresponding
     * destination samples.  For details, see
     * {@link WritableRaster#setRect(Raster)}.
     *
     * @param dx        The X translation factor from src space to dst space
     *                  of the copy.
     * @param dy        The Y translation factor from src space to dst space
     *                  of the copy.
     * @param srcRaster The Raster from which to copy pixels.
     */
    public void setRect(int dx, int dy, Raster srcRaster) {
        // Check if we can use fast code
        if (!(srcRaster instanceof BytePackedRaster) ||
            ((BytePackedRaster)srcRaster).pixelBitStride != pixelBitStride) {
            super.setRect(dx, dy, srcRaster);
            return;
        }

        int width  = srcRaster.getWidth();
        int height = srcRaster.getHeight();
        int srcOffX = srcRaster.getMinX();
        int srcOffY = srcRaster.getMinY();
        int dstOffX = dx+srcOffX;
        int dstOffY = dy+srcOffY;

        // Clip to this raster
        if (dstOffX < this.minX) {
            int skipX = this.minX - dstOffX;
            width -= skipX;
            srcOffX += skipX;
            dstOffX = this.minX;
        }
        if (dstOffY < this.minY) {
            int skipY = this.minY - dstOffY;
            height -= skipY;
            srcOffY += skipY;
            dstOffY = this.minY;
        }
        if (dstOffX+width > this.maxX) {
            width = this.maxX - dstOffX;
        }
        if (dstOffY+height > this.maxY) {
            height = this.maxY - dstOffY;
        }

        setDataElements(dstOffX, dstOffY,
                        srcOffX, srcOffY,
                        width, height,
                        (BytePackedRaster)srcRaster);
    }

    /**
     * Stores an array of data elements into the specified rectangular
     * region.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinates are out of bounds.
     * A ClassCastException will be thrown if the input object is non null
     * and references anything other than an array of transferType.
     * The data elements in the
     * data array are assumed to be packed.  That is, a data element
     * at location (x2, y2) would be found at:
     * <pre>
     *      inData[((y2-y)*w + (x2-x))]
     * </pre>
     * @param x        The X coordinate of the upper left pixel location.
     * @param y        The Y coordinate of the upper left pixel location.
     * @param w        Width of the pixel rectangle.
     * @param h        Height of the pixel rectangle.
     * @param obj      An object reference to an array of type defined by
     *                 getTransferType() and length w*h*getNumDataElements()
     *                 containing the pixel data to place between x,y and
     *                 x+h, y+h.
     */
    public void setDataElements(int x, int y, int w, int h, Object obj) {
        putByteData(x, y, w, h, (byte[])obj);
    }

    /**
     * Stores a byte array of data elements into the specified rectangular
     * region.  The band index will be ignored.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinates are out of bounds.
     * The data elements in the
     * data array are assumed to be packed.  That is, a data element
     * at location (x2, y2) would be found at:
     * <pre>
     *      inData[((y2-y)*w + (x2-x))]
     * </pre>
     * @param x        The X coordinate of the upper left pixel location.
     * @param y        The Y coordinate of the upper left pixel location.
     * @param w        Width of the pixel rectangle.
     * @param h        Height of the pixel rectangle.
     * @param band     The band to set, is ignored.
     * @param inData   The data elements to be stored.
     */
    public void putByteData(int x, int y, int w, int h,
                            int band, byte[] inData) {
        putByteData(x, y, w, h, inData);
    }

    /**
     * Stores a byte array of data elements into the specified rectangular
     * region.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinates are out of bounds.
     * The data elements in the
     * data array are assumed to be packed.  That is, a data element
     * at location (x2, y2) would be found at:
     * <pre>
     *      inData[((y2-y)*w + (x2-x))]
     * </pre>
     * @param x        The X coordinate of the upper left pixel location.
     * @param y        The Y coordinate of the upper left pixel location.
     * @param w        Width of the pixel rectangle.
     * @param h        Height of the pixel rectangle.
     * @param inData   The data elements to be stored.
     */
    public void putByteData(int x, int y, int w, int h, byte[] inData) {
        if ((x < this.minX) || (y < this.minY) ||
            (x + w > this.maxX) || (y + h > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        if (w == 0 || h == 0) {
            return;
        }

        int pixbits = pixelBitStride;
        int scanbit = dataBitOffset + (x - minX) * pixbits;
        int index = (y - minY) * scanlineStride;
        int outindex = 0;
        byte[] data = this.data;
        for (int j = 0; j < h; j++) {
            int bitnum = scanbit;
            int element;

            // Process initial portion of scanline
            int i = 0;
            while ((i < w) && ((bitnum & 7) != 0)) {
                int shift = shiftOffset - (bitnum & 7);
                element = data[index + (bitnum >> 3)];
                element &= ~(bitMask << shift);
                element |= (inData[outindex++] & bitMask) << shift;
                data[index + (bitnum >> 3)] = (byte)element;

                bitnum += pixbits;
                i++;
            }

            // Process central portion of scanline 8 pixels at a time
            int inIndex = index + (bitnum >> 3);
            switch (pixbits) {
            case 1:
                for (; i < w - 7; i += 8) {
                    element = (inData[outindex++] & 1) << 7;
                    element |= (inData[outindex++] & 1) << 6;
                    element |= (inData[outindex++] & 1) << 5;
                    element |= (inData[outindex++] & 1) << 4;
                    element |= (inData[outindex++] & 1) << 3;
                    element |= (inData[outindex++] & 1) << 2;
                    element |= (inData[outindex++] & 1) << 1;
                    element |= (inData[outindex++] & 1);

                    data[inIndex++] = (byte)element;

                    bitnum += 8;
                }
                break;

            case 2:
                for (; i < w - 7; i += 8) {
                    element = (inData[outindex++] & 3) << 6;
                    element |= (inData[outindex++] & 3) << 4;
                    element |= (inData[outindex++] & 3) << 2;
                    element |= (inData[outindex++] & 3);
                    data[inIndex++] = (byte)element;

                    element = (inData[outindex++] & 3) << 6;
                    element |= (inData[outindex++] & 3) << 4;
                    element |= (inData[outindex++] & 3) << 2;
                    element |= (inData[outindex++] & 3);
                    data[inIndex++] = (byte)element;

                    bitnum += 16;
                }
                break;

            case 4:
                for (; i < w - 7; i += 8) {
                    element = (inData[outindex++] & 0xf) << 4;
                    element |= (inData[outindex++] & 0xf);
                    data[inIndex++] = (byte)element;

                    element = (inData[outindex++] & 0xf) << 4;
                    element |= (inData[outindex++] & 0xf);
                    data[inIndex++] = (byte)element;

                    element = (inData[outindex++] & 0xf) << 4;
                    element |= (inData[outindex++] & 0xf);
                    data[inIndex++] = (byte)element;

                    element = (inData[outindex++] & 0xf) << 4;
                    element |= (inData[outindex++] & 0xf);
                    data[inIndex++] = (byte)element;

                    bitnum += 32;
                }
                break;
            }

            // Process final portion of scanline
            for (; i < w; i++) {
                int shift = shiftOffset - (bitnum & 7);

                element = data[index + (bitnum >> 3)];
                element &= ~(bitMask << shift);
                element |= (inData[outindex++] & bitMask) << shift;
                data[index + (bitnum >> 3)] = (byte)element;

                bitnum += pixbits;
            }

            index += scanlineStride;
        }

        markDirty();
    }

    /**
     * Returns an int array containing all samples for a rectangle of pixels,
     * one sample per array element.
     * An ArrayIndexOutOfBoundsException may be thrown
     * if the coordinates are not in bounds.
     * @param x,&nbsp;y   the coordinates of the upper-left pixel location
     * @param w      Width of the pixel rectangle
     * @param h      Height of the pixel rectangle
     * @param iArray An optionally pre-allocated int array
     * @return the samples for the specified rectangle of pixels.
     */
    public int[] getPixels(int x, int y, int w, int h, int[] iArray) {
        if ((x < this.minX) || (y < this.minY) ||
            (x + w > this.maxX) || (y + h > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        if (iArray == null) {
            iArray = new int[w * h];
        }
        int pixbits = pixelBitStride;
        int scanbit = dataBitOffset + (x-minX) * pixbits;
        int index = (y-minY) * scanlineStride;
        int outindex = 0;
        byte[] data = this.data;

        for (int j = 0; j < h; j++) {
            int bitnum = scanbit;
            int element;

            // Process initial portion of scanline
            int i = 0;
            while ((i < w) && ((bitnum & 7) != 0)) {
                int shift = shiftOffset - (bitnum & 7);
                iArray[outindex++] =
                    bitMask & (data[index + (bitnum >> 3)] >> shift);
                bitnum += pixbits;
                i++;
            }

            // Process central portion of scanline 8 pixels at a time
            int inIndex = index + (bitnum >> 3);
            switch (pixbits) {
            case 1:
                for (; i < w - 7; i += 8) {
                    element = data[inIndex++];
                    iArray[outindex++] = (element >> 7) & 1;
                    iArray[outindex++] = (element >> 6) & 1;
                    iArray[outindex++] = (element >> 5) & 1;
                    iArray[outindex++] = (element >> 4) & 1;
                    iArray[outindex++] = (element >> 3) & 1;
                    iArray[outindex++] = (element >> 2) & 1;
                    iArray[outindex++] = (element >> 1) & 1;
                    iArray[outindex++] = element & 1;
                    bitnum += 8;
                }
                break;

            case 2:
                for (; i < w - 7; i += 8) {
                    element = data[inIndex++];
                    iArray[outindex++] = (element >> 6) & 3;
                    iArray[outindex++] = (element >> 4) & 3;
                    iArray[outindex++] = (element >> 2) & 3;
                    iArray[outindex++] = element & 3;

                    element = data[inIndex++];
                    iArray[outindex++] = (element >> 6) & 3;
                    iArray[outindex++] = (element >> 4) & 3;
                    iArray[outindex++] = (element >> 2) & 3;
                    iArray[outindex++] = element & 3;

                    bitnum += 16;
                }
                break;

            case 4:
                for (; i < w - 7; i += 8) {
                    element = data[inIndex++];
                    iArray[outindex++] = (element >> 4) & 0xf;
                    iArray[outindex++] = element & 0xf;

                    element = data[inIndex++];
                    iArray[outindex++] = (element >> 4) & 0xf;
                    iArray[outindex++] = element & 0xf;

                    element = data[inIndex++];
                    iArray[outindex++] = (element >> 4) & 0xf;
                    iArray[outindex++] = element & 0xf;

                    element = data[inIndex++];
                    iArray[outindex++] = (element >> 4) & 0xf;
                    iArray[outindex++] = element & 0xf;

                    bitnum += 32;
                }
                break;
            }

            // Process final portion of scanline
            for (; i < w; i++) {
                int shift = shiftOffset - (bitnum & 7);
                iArray[outindex++] =
                    bitMask & (data[index + (bitnum >> 3)] >> shift);
                bitnum += pixbits;
            }

            index += scanlineStride;
        }

        return iArray;
    }

    /**
     * Sets all samples for a rectangle of pixels from an int array containing
     * one sample per array element.
     * An ArrayIndexOutOfBoundsException may be thrown if the coordinates are
     * not in bounds.
     * @param x        The X coordinate of the upper left pixel location.
     * @param y        The Y coordinate of the upper left pixel location.
     * @param w        Width of the pixel rectangle.
     * @param h        Height of the pixel rectangle.
     * @param iArray   The input int pixel array.
     */
    public void setPixels(int x, int y, int w, int h, int[] iArray) {
        if ((x < this.minX) || (y < this.minY) ||
            (x + w > this.maxX) || (y + h > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int pixbits = pixelBitStride;
        int scanbit = dataBitOffset + (x - minX) * pixbits;
        int index = (y - minY) * scanlineStride;
        int outindex = 0;
        byte[] data = this.data;
        for (int j = 0; j < h; j++) {
            int bitnum = scanbit;
            int element;

            // Process initial portion of scanline
            int i = 0;
            while ((i < w) && ((bitnum & 7) != 0)) {
                int shift = shiftOffset - (bitnum & 7);
                element = data[index + (bitnum >> 3)];
                element &= ~(bitMask << shift);
                element |= (iArray[outindex++] & bitMask) << shift;
                data[index + (bitnum >> 3)] = (byte)element;

                bitnum += pixbits;
                i++;
            }

            // Process central portion of scanline 8 pixels at a time
            int inIndex = index + (bitnum >> 3);
            switch (pixbits) {
            case 1:
                for (; i < w - 7; i += 8) {
                    element = (iArray[outindex++] & 1) << 7;
                    element |= (iArray[outindex++] & 1) << 6;
                    element |= (iArray[outindex++] & 1) << 5;
                    element |= (iArray[outindex++] & 1) << 4;
                    element |= (iArray[outindex++] & 1) << 3;
                    element |= (iArray[outindex++] & 1) << 2;
                    element |= (iArray[outindex++] & 1) << 1;
                    element |= (iArray[outindex++] & 1);
                    data[inIndex++] = (byte)element;

                    bitnum += 8;
                }
                break;

            case 2:
                for (; i < w - 7; i += 8) {
                    element = (iArray[outindex++] & 3) << 6;
                    element |= (iArray[outindex++] & 3) << 4;
                    element |= (iArray[outindex++] & 3) << 2;
                    element |= (iArray[outindex++] & 3);
                    data[inIndex++] = (byte)element;

                    element = (iArray[outindex++] & 3) << 6;
                    element |= (iArray[outindex++] & 3) << 4;
                    element |= (iArray[outindex++] & 3) << 2;
                    element |= (iArray[outindex++] & 3);
                    data[inIndex++] = (byte)element;

                    bitnum += 16;
                }
                break;

            case 4:
                for (; i < w - 7; i += 8) {
                    element = (iArray[outindex++] & 0xf) << 4;
                    element |= (iArray[outindex++] & 0xf);
                    data[inIndex++] = (byte)element;

                    element = (iArray[outindex++] & 0xf) << 4;
                    element |= (iArray[outindex++] & 0xf);
                    data[inIndex++] = (byte)element;

                    element = (iArray[outindex++] & 0xf) << 4;
                    element |= (iArray[outindex++] & 0xf);
                    data[inIndex++] = (byte)element;

                    element = (iArray[outindex++] & 0xf) << 4;
                    element |= (iArray[outindex++] & 0xf);
                    data[inIndex++] = (byte)element;

                    bitnum += 32;
                }
                break;
            }

            // Process final portion of scanline
            for (; i < w; i++) {
                int shift = shiftOffset - (bitnum & 7);

                element = data[index + (bitnum >> 3)];
                element &= ~(bitMask << shift);
                element |= (iArray[outindex++] & bitMask) << shift;
                data[index + (bitnum >> 3)] = (byte)element;

                bitnum += pixbits;
            }

            index += scanlineStride;
        }

        markDirty();
    }

    /**
     * Creates a subraster given a region of the raster.  The x and y
     * coordinates specify the horizontal and vertical offsets
     * from the upper-left corner of this raster to the upper-left corner
     * of the subraster.  Note that the subraster will reference the same
     * DataBuffer as the parent raster, but using different offsets. The
     * bandList is ignored.
     * @param x               X offset.
     * @param y               Y offset.
     * @param width           Width (in pixels) of the subraster.
     * @param height          Height (in pixels) of the subraster.
     * @param x0              Translated X origin of the subraster.
     * @param y0              Translated Y origin of the subraster.
     * @param bandList        Array of band indices.
     * @exception RasterFormatException
     *            if the specified bounding box is outside of the parent raster.
     */
    public Raster createChild(int x, int y,
                              int width, int height,
                              int x0, int y0, int[] bandList) {
        WritableRaster newRaster = createWritableChild(x, y,
                                                       width, height,
                                                       x0, y0,
                                                       bandList);
        return (Raster) newRaster;
    }

    /**
     * Creates a Writable subRaster given a region of the Raster. The x and y
     * coordinates specify the horizontal and vertical offsets
     * from the upper-left corner of this Raster to the upper-left corner
     * of the subRaster.  The bandList is ignored.
     * A translation to the subRaster may also be specified.
     * Note that the subRaster will reference the same
     * DataBuffer as the parent Raster, but using different offsets.
     * @param x               X offset.
     * @param y               Y offset.
     * @param width           Width (in pixels) of the subraster.
     * @param height          Height (in pixels) of the subraster.
     * @param x0              Translated X origin of the subraster.
     * @param y0              Translated Y origin of the subraster.
     * @param bandList        Array of band indices.
     * @exception RasterFormatException
     *            if the specified bounding box is outside of the parent Raster.
     */
    public WritableRaster createWritableChild(int x, int y,
                                              int width, int height,
                                              int x0, int y0,
                                              int[] bandList) {
        if (x < this.minX) {
            throw new RasterFormatException("x lies outside the raster");
        }
        if (y < this.minY) {
            throw new RasterFormatException("y lies outside the raster");
        }
        if ((x+width < x) || (x+width > this.minX + this.width)) {
            throw new RasterFormatException("(x + width) is outside of Raster");
        }
        if ((y+height < y) || (y+height > this.minY + this.height)) {
            throw new RasterFormatException("(y + height) is outside of Raster");
        }

        SampleModel sm;

        if (bandList != null) {
            sm = sampleModel.createSubsetSampleModel(bandList);
        }
        else {
            sm = sampleModel;
        }

        int deltaX = x0 - x;
        int deltaY = y0 - y;

        return new BytePackedRaster(sm,
                                    (DataBufferByte) dataBuffer,
                                    new Rectangle(x0, y0, width, height),
                                    new Point(sampleModelTranslateX+deltaX,
                                              sampleModelTranslateY+deltaY),
                                    this);
    }

    /**
     * Creates a raster with the same layout but using a different
     * width and height, and with new zeroed data arrays.
     */
    public WritableRaster createCompatibleWritableRaster(int w, int h) {
        if (w <= 0 || h <=0) {
            throw new RasterFormatException("negative "+
                                          ((w <= 0) ? "width" : "height"));
        }

        SampleModel sm = sampleModel.createCompatibleSampleModel(w,h);

        return new BytePackedRaster(sm, new Point(0,0));
    }

    /**
     * Creates a raster with the same layout and the same
     * width and height, and with new zeroed data arrays.
     */
    public WritableRaster createCompatibleWritableRaster () {
        return createCompatibleWritableRaster(width,height);
    }

    /**
     * Verify that the layout parameters are consistent with
     * the data.  If strictCheck
     * is false, this method will check for ArrayIndexOutOfBounds conditions.
     * If strictCheck is true, this method will check for additional error
     * conditions such as line wraparound (width of a line greater than
     * the scanline stride).
     * @return   String   Error string, if the layout is incompatible with
     *                    the data.  Otherwise returns null.
     */
    private void verify (boolean strictCheck) {
        // Make sure data for Raster is in a legal range
        if (dataBitOffset < 0) {
            throw new RasterFormatException("Data offsets must be >= 0");
        }

        /* Need to re-verify the dimensions since a sample model may be
         * specified to the constructor
         */
        if (width <= 0 || height <= 0 ||
            height > (Integer.MAX_VALUE / width))
        {
            throw new RasterFormatException("Invalid raster dimension");
        }


        /*
         * pixelBitstride was verified in constructor, so just make
         * sure that it is safe to multiply it by width.
         */
        if ((width - 1) > Integer.MAX_VALUE / pixelBitStride) {
            throw new RasterFormatException("Invalid raster dimension");
        }

        if ((long)minX - sampleModelTranslateX < 0 ||
            (long)minY - sampleModelTranslateY < 0) {

            throw new RasterFormatException("Incorrect origin/translate: (" +
                    minX + ", " + minY + ") / (" +
                    sampleModelTranslateX + ", " + sampleModelTranslateY + ")");
        }

        if (scanlineStride < 0 ||
            scanlineStride > (Integer.MAX_VALUE / height))
        {
            throw new RasterFormatException("Invalid scanline stride");
        }

        if (height > 1 || minY - sampleModelTranslateY > 0) {
            // buffer should contain at least one scanline
            if (scanlineStride > data.length) {
                throw new RasterFormatException("Incorrect scanline stride: "
                        + scanlineStride);
            }
        }

        long lastbit = (long) dataBitOffset
                       + (long) (height - 1) * (long) scanlineStride * 8
                       + (long) (width - 1) * (long) pixelBitStride
                       + (long) pixelBitStride - 1;
        if (lastbit < 0 || lastbit / 8 >= data.length) {
            throw new RasterFormatException("raster dimensions overflow " +
                                            "array bounds");
        }
        if (strictCheck) {
            if (height > 1) {
                lastbit = width * pixelBitStride - 1;
                if (lastbit / 8 >= scanlineStride) {
                    throw new RasterFormatException("data for adjacent" +
                                                    " scanlines overlaps");
                }
            }
        }
    }

    public String toString() {
        return new String ("BytePackedRaster: width = "+width+" height = "+height
                           +" #channels "+numBands
                           +" xOff = "+sampleModelTranslateX
                           +" yOff = "+sampleModelTranslateY);
    }
}
