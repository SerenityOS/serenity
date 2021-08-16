/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.RasterFormatException;
import java.awt.image.SampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.DataBufferByte;
import java.awt.Rectangle;
import java.awt.Point;

/**
 * This class defines a Raster with pixels consisting of one or more
 * 8-bit data elements stored in close proximity to each other in a
 * single byte array.
 * <p>
 * The bit precision per data element is that of the data type (that
 * is, the bit precision for this Raster is 8).  There is only one
 * pixel stride and one scanline stride for all bands.  This type of
 * Raster can be used with a ComponentColorModel if there are multiple
 * bands, or an IndexColorModel if there is only one band.
 *
 */
public class ByteInterleavedRaster extends ByteComponentRaster {

    /** True if the data offsets range from 0 to (pixelStride - 1) in order. */
    boolean inOrder;

    /**
     * The DataBuffer offset, minus sampleModelTranslateX*pixelStride,
     * minus sampleModelTranslateY*scanlineStride, used to calculate
     * pixel offsets.
     */
    int dbOffset;
    int dbOffsetPacked;

    /** True if a SinglePixelPackedSampleModel is being used. */
    boolean packed = false;

    /** If packed == true, the SampleModel's bit masks. */
    int[] bitMasks;

    /** If packed == true, the SampleModel's bit offsets. */
    int[] bitOffsets;

    /** A cached copy of minX + width for use in bounds checks. */
    private int maxX;

    /** A cached copy of minY + height for use in bounds checks. */
    private int maxY;

    /**
     * Constructs a ByteInterleavedRaster with the given SampleModel.
     * The Raster's upper left corner is origin and it is the same
     * size as the SampleModel.  A DataBuffer large enough to describe the
     * Raster is automatically created.  SampleModel must be of type
     * SinglePixelPackedSampleModel or InterleavedSampleModel.
     * @param sampleModel     The SampleModel that specifies the layout.
     * @param origin          The Point that specified the origin.
     */
    public ByteInterleavedRaster(SampleModel sampleModel, Point origin) {
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
     * Constructs a ByteInterleavedRaster with the given SampleModel
     * and DataBuffer.  The Raster's upper left corner is origin and
     * it is the same size as the SampleModel.  The DataBuffer is not
     * initialized and must be a DataBufferByte compatible with SampleModel.
     * SampleModel must be of type SinglePixelPackedSampleModel
     * or InterleavedSampleModel.
     * @param sampleModel     The SampleModel that specifies the layout.
     * @param dataBuffer      The DataBufferByte that contains the image data.
     * @param origin          The Point that specifies the origin.
     */
    public ByteInterleavedRaster(SampleModel sampleModel,
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

    /*** Analyzes a ComponentSampleModel to determine if it can function
     * as a PixelInterleavedSampleModel.  In order to do so, it must use
     * only bank 0 of its DataBuffer, and the data offsets must span a range
     * of less than pixelStride.
     *
     * <p> These properties are trivially true for a 1-banded SampleModel.
     */
    private boolean isInterleaved(ComponentSampleModel sm) {
        // Analyze ComponentSampleModel to determine if it has the
        // properties of a PixelInterleavedSampleModel

        int numBands = sampleModel.getNumBands();
        if (numBands == 1) {
            return true;
        }

        // Determine banks used
        int[] bankIndices = sm.getBankIndices();
        for (int i = 0; i < numBands; i++) {
            if (bankIndices[i] != 0) {
                return false;
            }
        }

        // Determine range of band offsets
        int[] bandOffsets = sm.getBandOffsets();
        int minOffset = bandOffsets[0];
        int maxOffset = minOffset;
        for (int i = 1; i < numBands; i++) {
            int offset = bandOffsets[i];
            if (offset < minOffset) {
                minOffset = offset;
            }
            if (offset > maxOffset) {
                maxOffset = offset;
            }
        }
        if (maxOffset - minOffset >= sm.getPixelStride()) {
            return false;
        }

        return true;
    }

    /**
     * Constructs a ByteInterleavedRaster with the given SampleModel,
     * DataBuffer, and parent.  DataBuffer must be a DataBufferByte and
     * SampleModel must be of type SinglePixelPackedSampleModel
     * or InterleavedSampleModel.
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
     */
    public ByteInterleavedRaster(SampleModel sampleModel,
                                 DataBufferByte dataBuffer,
                                 Rectangle aRegion,
                                 Point origin,
                                 ByteInterleavedRaster parent)
    {
        super(sampleModel, dataBuffer, aRegion, origin, parent);
        this.maxX = minX + width;
        this.maxY = minY + height;

        this.data = stealData(dataBuffer, 0);

        int xOffset = aRegion.x - origin.x;
        int yOffset = aRegion.y - origin.y;
        if (sampleModel instanceof PixelInterleavedSampleModel ||
            (sampleModel instanceof ComponentSampleModel &&
             isInterleaved((ComponentSampleModel)sampleModel))) {
            ComponentSampleModel csm = (ComponentSampleModel)sampleModel;
            this.scanlineStride = csm.getScanlineStride();
            this.pixelStride = csm.getPixelStride();
            this.dataOffsets = csm.getBandOffsets();
            for (int i = 0; i < getNumDataElements(); i++) {
                dataOffsets[i] += xOffset*pixelStride+yOffset*scanlineStride;
            }
        } else if (sampleModel instanceof SinglePixelPackedSampleModel) {
            SinglePixelPackedSampleModel sppsm =
                    (SinglePixelPackedSampleModel)sampleModel;
            this.packed = true;
            this.bitMasks = sppsm.getBitMasks();
            this.bitOffsets = sppsm.getBitOffsets();
            this.scanlineStride = sppsm.getScanlineStride();
            this.pixelStride = 1;
            this.dataOffsets = new int[1];
            this.dataOffsets[0] = dataBuffer.getOffset();
            dataOffsets[0] += xOffset*pixelStride+yOffset*scanlineStride;
        } else {
            throw new RasterFormatException("ByteInterleavedRasters must " +
              "have PixelInterleavedSampleModel, SinglePixelPackedSampleModel"+
              " or interleaved ComponentSampleModel.  Sample model is " +
              sampleModel);
        }
        this.bandOffset = this.dataOffsets[0];

        this.dbOffsetPacked = dataBuffer.getOffset() -
            sampleModelTranslateY*scanlineStride -
            sampleModelTranslateX*pixelStride;
        this.dbOffset = dbOffsetPacked -
            (xOffset*pixelStride+yOffset*scanlineStride);

        // Set inOrder to true if the data elements are in order and
        // have no gaps between them
        this.inOrder = false;
        if (numDataElements == pixelStride) {
            inOrder = true;
            for (int i = 1; i < numDataElements; i++) {
                if (dataOffsets[i] - dataOffsets[0] != i) {
                    inOrder = false;
                    break;
                }
            }
        }

        verify();
    }

    /**
     * Returns a copy of the data offsets array. For each band the data offset
     * is the index into the band's data array, of the first sample of the
     * band.
     */
    public int[] getDataOffsets() {
        return dataOffsets.clone();
    }

    /**
     * Returns the data offset for the specified band.  The data offset
     * is the index into the data array
     * in which the first sample of the first scanline is stored.
     * @param band  The band whose offset is returned.
     */
    public int getDataOffset(int band) {
        return dataOffsets[band];
    }

    /**
     * Returns the scanline stride -- the number of data array elements between
     * a given sample and the sample in the same column of the next row in the
     * same band.
     */
    public int getScanlineStride() {
        return scanlineStride;
    }

    /**
     * Returns pixel stride -- the number of data array elements between two
     * samples for the same band on the same scanline.
     */
    public int getPixelStride() {
        return pixelStride;
    }

    /**
     * Returns a reference to the data array.
     */
    public byte[] getDataStorage() {
        return data;
    }

    /**
     * Returns the data elements for all bands at the specified
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
        int off = (y-minY)*scanlineStride +
                  (x-minX)*pixelStride;

        for (int band = 0; band < numDataElements; band++) {
            outData[band] = data[dataOffsets[band] + off];
        }

        return outData;
    }

    /**
     * Returns an array of data elements from the specified rectangular
     * region.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinates are out of bounds.
     * A ClassCastException will be thrown if the input object is non null
     * and references anything other than an array of transferType.
     * <pre>
     *       byte[] bandData = (byte[])raster.getDataElements(x, y, w, h, null);
     *       int numDataElements = raster.getNumDataElements();
     *       byte[] pixel = new byte[numDataElements];
     *       // To find a data element at location (x2, y2)
     *       System.arraycopy(bandData, ((y2-y)*w + (x2-x))*numDataElements,
     *                        pixel, 0, numDataElements);
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
    public Object getDataElements(int x, int y, int w, int h, Object obj) {
        return getByteData(x, y, w, h, (byte[])obj);
    }

    /**
     * Returns a byte array of data elements from the specified rectangular
     * region for the specified band.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinates are out of bounds.
     * <pre>
     *       byte[] bandData = raster.getByteData(x, y, w, h, null);
     *       // To find the data element at location (x2, y2)
     *       byte bandElement = bandData[((y2-y)*w + (x2-x))];
     * </pre>
     * @param x        The X coordinate of the upper left pixel location.
     * @param y        The Y coordinate of the upper left pixel location.
     * @param w        Width of the pixel rectangle.
     * @param h        Height of the pixel rectangle.
     * @param band     The band to return.
     * @param outData  If non-null, data elements for all bands
     *                 at the specified location are returned in this array.
     * @return         Data array with data elements for all bands.
     */
    public byte[] getByteData(int x, int y, int w, int h,
                              int band, byte[] outData) {
        // Bounds check for 'band' will be performed automatically
        if ((x < this.minX) || (y < this.minY) ||
            (x + w > this.maxX) || (y + h > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        if (outData == null) {
            outData = new byte[w*h];
        }
        int yoff = (y-minY)*scanlineStride +
                   (x-minX)*pixelStride + dataOffsets[band];
        int xoff;
        int off = 0;
        int xstart;
        int ystart;

        if (pixelStride == 1) {
            if (scanlineStride == w) {
                System.arraycopy(data, yoff, outData, 0, w*h);
            } else {
                for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                    System.arraycopy(data, yoff, outData, off, w);
                    off += w;
                }
            }
        } else {
            for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                xoff = yoff;
                for (xstart=0; xstart < w; xstart++, xoff += pixelStride) {
                    outData[off++] = data[xoff];
                }
            }
        }

        return outData;
    }

    /**
     * Returns a byte array of data elements from the specified rectangular
     * region.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinates are out of bounds.
     * <pre>
     *       byte[] bandData = raster.getByteData(x, y, w, h, null);
     *       int numDataElements = raster.getnumDataElements();
     *       byte[] pixel = new byte[numDataElements];
     *       // To find a data element at location (x2, y2)
     *       System.arraycopy(bandData, ((y2-y)*w + (x2-x))*numDataElements,
     *                        pixel, 0, numDataElements);
     * </pre>
     * @param x        The X coordinate of the upper left pixel location.
     * @param y        The Y coordinate of the upper left pixel location.
     * @param w        Width of the pixel rectangle.
     * @param h        Height of the pixel rectangle.
     * @param outData  If non-null, data elements for all bands
     *                 at the specified location are returned in this array.
     * @return         Data array with data elements for all bands.
     */
    public byte[] getByteData(int x, int y, int w, int h, byte[] outData) {
        if ((x < this.minX) || (y < this.minY) ||
            (x + w > this.maxX) || (y + h > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        if (outData == null) {
            outData = new byte[numDataElements*w*h];
        }
        int yoff = (y-minY)*scanlineStride +
                   (x-minX)*pixelStride;
        int xoff;
        int off = 0;
        int xstart;
        int ystart;

        if (inOrder) {
            yoff += dataOffsets[0];
            int rowBytes = w*pixelStride;
            if (scanlineStride == rowBytes) {
                System.arraycopy(data, yoff, outData, off, rowBytes*h);
            } else {
                for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                    System.arraycopy(data, yoff, outData, off, rowBytes);
                    off += rowBytes;
                }
            }
        } else if (numDataElements == 1) {
            yoff += dataOffsets[0];
            for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                xoff = yoff;
                for (xstart=0; xstart < w; xstart++, xoff += pixelStride) {
                    outData[off++] = data[xoff];
                }
            }
        } else if (numDataElements == 2) {
            yoff += dataOffsets[0];
            int d1 = dataOffsets[1] - dataOffsets[0];
            for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                xoff = yoff;
                for (xstart=0; xstart < w; xstart++, xoff += pixelStride) {
                    outData[off++] = data[xoff];
                    outData[off++] = data[xoff + d1];
                }
            }
        } else if (numDataElements == 3) {
            yoff += dataOffsets[0];
            int d1 = dataOffsets[1] - dataOffsets[0];
            int d2 = dataOffsets[2] - dataOffsets[0];
            for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                xoff = yoff;
                for (xstart=0; xstart < w; xstart++, xoff += pixelStride) {
                    outData[off++] = data[xoff];
                    outData[off++] = data[xoff + d1];
                    outData[off++] = data[xoff + d2];
                }
            }
        } else if (numDataElements == 4) {
            yoff += dataOffsets[0];
            int d1 = dataOffsets[1] - dataOffsets[0];
            int d2 = dataOffsets[2] - dataOffsets[0];
            int d3 = dataOffsets[3] - dataOffsets[0];
            for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                xoff = yoff;
                for (xstart=0; xstart < w; xstart++, xoff += pixelStride) {
                    outData[off++] = data[xoff];
                    outData[off++] = data[xoff + d1];
                    outData[off++] = data[xoff + d2];
                    outData[off++] = data[xoff + d3];
                }
            }
        } else {
            for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                xoff = yoff;
                for (xstart=0; xstart < w; xstart++, xoff += pixelStride) {
                    for (int c = 0; c < numDataElements; c++) {
                        outData[off++] = data[dataOffsets[c] + xoff];
                    }
                }
            }
        }

        return outData;
    }

    /**
     * Stores the data elements for all bands at the specified location.
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
        int off = (y-minY)*scanlineStride +
                  (x-minX)*pixelStride;

        for (int i = 0; i < numDataElements; i++) {
            data[dataOffsets[i] + off] = inData[i];
        }

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
        int srcOffX = inRaster.getMinX();
        int srcOffY = inRaster.getMinY();
        int dstOffX = x + srcOffX;
        int dstOffY = y + srcOffY;
        int width  = inRaster.getWidth();
        int height = inRaster.getHeight();
        if ((dstOffX < this.minX) || (dstOffY < this.minY) ||
            (dstOffX + width > this.maxX) || (dstOffY + height > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }

        setDataElements(dstOffX, dstOffY, srcOffX, srcOffY,
                        width, height, inRaster);
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
     * @param inRaster   Raster of data to place at x,y location.
     */
    private void setDataElements(int dstX, int dstY,
                                 int srcX, int srcY,
                                 int width, int height,
                                 Raster inRaster) {
        // Assume bounds checking has been performed previously
        if (width <= 0 || height <= 0) {
            return;
        }

        // Write inRaster (minX, minY) to (dstX, dstY)

        int srcOffX = inRaster.getMinX();
        int srcOffY = inRaster.getMinY();
        Object tdata = null;

        if (inRaster instanceof ByteInterleavedRaster) {
            ByteInterleavedRaster bct = (ByteInterleavedRaster) inRaster;
            byte[] bdata = bct.getDataStorage();
            // copy whole scanlines
            if (inOrder && bct.inOrder && pixelStride == bct.pixelStride) {
                int toff = bct.getDataOffset(0);
                int tss  = bct.getScanlineStride();
                int tps  = bct.getPixelStride();

                int srcOffset = toff +
                    (srcY - srcOffY) * tss +
                    (srcX - srcOffX) * tps;
                int dstOffset = dataOffsets[0] +
                    (dstY - minY) * scanlineStride +
                    (dstX - minX) * pixelStride;

                int nbytes = width*pixelStride;
                for (int tmpY=0; tmpY < height; tmpY++) {
                    System.arraycopy(bdata, srcOffset,
                                     data, dstOffset, nbytes);
                    srcOffset += tss;
                    dstOffset += scanlineStride;
                }
                markDirty();
                return;
            }
        }

        for (int startY=0; startY < height; startY++) {
            // Grab one scanline at a time
            tdata = inRaster.getDataElements(srcOffX, srcOffY+startY,
                                             width, 1, tdata);
            setDataElements(dstX, dstY + startY, width, 1, tdata);
        }
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
     * for the nth band at location (x2, y2) would be found at:
     * <pre>
     *      inData[((y2-y)*w + (x2-x))*numDataElements + n]
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
     * region for the specified band.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinates are out of bounds.
     * The data elements in the
     * data array are assumed to be packed.  That is, a data element
     * at location (x2, y2) would be found at:
     * <pre>
     *      inData[((y2-y)*w + (x2-x)) + n]
     * </pre>
     * @param x        The X coordinate of the upper left pixel location.
     * @param y        The Y coordinate of the upper left pixel location.
     * @param w        Width of the pixel rectangle.
     * @param h        Height of the pixel rectangle.
     * @param band     The band to set.
     * @param inData   The data elements to be stored.
     */
    public void putByteData(int x, int y, int w, int h,
                            int band, byte[] inData) {
        // Bounds check for 'band' will be performed automatically
        if ((x < this.minX) || (y < this.minY) ||
            (x + w > this.maxX) || (y + h > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int yoff = (y-minY)*scanlineStride +
                   (x-minX)*pixelStride + dataOffsets[band];
        int xoff;
        int off = 0;
        int xstart;
        int ystart;

        if (pixelStride == 1) {
            if (scanlineStride == w) {
                System.arraycopy(inData, 0, data, yoff, w*h);
            }
            else {
                for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                    System.arraycopy(inData, off, data, yoff, w);
                    off += w;
                }
            }
        }
        else {
            for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                xoff = yoff;
                for (xstart=0; xstart < w; xstart++, xoff += pixelStride) {
                    data[xoff] = inData[off++];
                }
            }
        }

        markDirty();
    }

    /**
     * Stores a byte array of data elements into the specified rectangular
     * region.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinates are out of bounds.
     * The data elements in the
     * data array are assumed to be packed.  That is, a data element
     * for the nth band at location (x2, y2) would be found at:
     * <pre>
     *      inData[((y2-y)*w + (x2-x))*numDataElements + n]
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
        int yoff = (y-minY)*scanlineStride +
                   (x-minX)*pixelStride;

        int xoff;
        int off = 0;
        int xstart;
        int ystart;

        if (inOrder) {
            yoff += dataOffsets[0];
            int rowBytes = w*pixelStride;
            if (rowBytes == scanlineStride) {
                System.arraycopy(inData, 0, data, yoff, rowBytes*h);
            } else {
                for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                    System.arraycopy(inData, off, data, yoff, rowBytes);
                    off += rowBytes;
                }
            }
        } else if (numDataElements == 1) {
            yoff += dataOffsets[0];
            for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                xoff = yoff;
                for (xstart=0; xstart < w; xstart++, xoff += pixelStride) {
                    data[xoff] = inData[off++];
                }
            }
        } else if (numDataElements == 2) {
            yoff += dataOffsets[0];
            int d1 = dataOffsets[1] - dataOffsets[0];
            for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                xoff = yoff;
                for (xstart=0; xstart < w; xstart++, xoff += pixelStride) {
                    data[xoff] = inData[off++];
                    data[xoff + d1] = inData[off++];
                }
            }
        } else if (numDataElements == 3) {
            yoff += dataOffsets[0];
            int d1 = dataOffsets[1] - dataOffsets[0];
            int d2 = dataOffsets[2] - dataOffsets[0];
            for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                xoff = yoff;
                for (xstart=0; xstart < w; xstart++, xoff += pixelStride) {
                    data[xoff] = inData[off++];
                    data[xoff + d1] = inData[off++];
                    data[xoff + d2] = inData[off++];
                }
            }
        } else if (numDataElements == 4) {
            yoff += dataOffsets[0];
            int d1 = dataOffsets[1] - dataOffsets[0];
            int d2 = dataOffsets[2] - dataOffsets[0];
            int d3 = dataOffsets[3] - dataOffsets[0];
            for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                xoff = yoff;
                for (xstart=0; xstart < w; xstart++, xoff += pixelStride) {
                    data[xoff] = inData[off++];
                    data[xoff + d1] = inData[off++];
                    data[xoff + d2] = inData[off++];
                    data[xoff + d3] = inData[off++];
                }
            }
        } else {
            for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                xoff = yoff;
                for (xstart=0; xstart < w; xstart++, xoff += pixelStride) {
                    for (int c = 0; c < numDataElements; c++) {
                        data[dataOffsets[c] + xoff] = inData[off++];
                    }
                }
            }
        }

        markDirty();
    }

    public int getSample(int x, int y, int b) {
        if ((x < this.minX) || (y < this.minY) ||
            (x >= this.maxX) || (y >= this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        if (packed) {
            int offset = y*scanlineStride + x + dbOffsetPacked;
            byte sample = data[offset];
            return (sample & bitMasks[b]) >>> bitOffsets[b];
        } else {
            int offset = y*scanlineStride + x*pixelStride + dbOffset;
            return data[offset + dataOffsets[b]] & 0xff;
        }
    }

    public void setSample(int x, int y, int b, int s) {
        if ((x < this.minX) || (y < this.minY) ||
            (x >= this.maxX) || (y >= this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        if (packed) {
            int offset = y*scanlineStride + x + dbOffsetPacked;
            int bitMask = bitMasks[b];

            byte value = data[offset];
            value &= ~bitMask;
            value |= (s << bitOffsets[b]) & bitMask;
            data[offset] = value;
        } else {
            int offset = y*scanlineStride + x*pixelStride + dbOffset;
            data[offset + dataOffsets[b]] = (byte)s;
        }

        markDirty();
    }

    public int[] getSamples(int x, int y, int w, int h, int b,
                            int[] iArray) {
        if ((x < this.minX) || (y < this.minY) ||
            (x + w > this.maxX) || (y + h > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int[] samples;
        if (iArray != null) {
            samples = iArray;
        } else {
            samples = new int [w*h];
        }

        int lineOffset = y*scanlineStride + x*pixelStride;
        int dstOffset = 0;

        if (packed) {
            lineOffset += dbOffsetPacked;
            int bitMask = bitMasks[b];
            int bitOffset = bitOffsets[b];

            for (int j = 0; j < h; j++) {
                int sampleOffset = lineOffset;
                for (int i = 0; i < w; i++) {
                    int value = data[sampleOffset++];
                    samples[dstOffset++] = ((value & bitMask) >>> bitOffset);
                }
                lineOffset += scanlineStride;
            }
        } else {
            lineOffset += dbOffset + dataOffsets[b];
            for (int j = 0; j < h; j++) {
                int sampleOffset = lineOffset;
                for (int i = 0; i < w; i++) {
                    samples[dstOffset++] = data[sampleOffset] & 0xff;
                    sampleOffset += pixelStride;
                }
                lineOffset += scanlineStride;
            }
        }

        return samples;
    }

    public void setSamples(int x, int y, int w, int h, int b, int[] iArray) {
        if ((x < this.minX) || (y < this.minY) ||
            (x + w > this.maxX) || (y + h > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int lineOffset = y*scanlineStride + x*pixelStride;
        int srcOffset = 0;

        if (packed) {
            lineOffset += dbOffsetPacked;
            int bitMask = bitMasks[b];

            for (int j = 0; j < h; j++) {
                int sampleOffset = lineOffset;
                for (int i = 0; i < w; i++) {
                    byte value = data[sampleOffset];
                    value &= ~bitMask;
                    int sample = iArray[srcOffset++];
                    value |= (sample << bitOffsets[b]) & bitMask;
                    data[sampleOffset++] = value;
                }
                lineOffset += scanlineStride;
            }
        } else {
            lineOffset += dbOffset + dataOffsets[b];
            for (int i = 0; i < h; i++) {
                int sampleOffset = lineOffset;
                for (int j = 0; j < w; j++) {
                    data[sampleOffset] = (byte)iArray[srcOffset++];
                    sampleOffset += pixelStride;
                }
                lineOffset += scanlineStride;
            }
        }

        markDirty();
    }

    public int[] getPixels(int x, int y, int w, int h, int[] iArray) {
        if ((x < this.minX) || (y < this.minY) ||
            (x + w > this.maxX) || (y + h > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int[] pixels;
        if (iArray != null) {
            pixels = iArray;
        } else {
            pixels = new int[w*h*numBands];
        }

        int lineOffset = y*scanlineStride + x*pixelStride;
        int dstOffset = 0;

        if (packed) {
            lineOffset += dbOffsetPacked;
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    int value = data[lineOffset + i];
                    for (int k = 0; k < numBands; k++) {
                        pixels[dstOffset++] =
                            (value & bitMasks[k]) >>> bitOffsets[k];
                    }
                }
                lineOffset += scanlineStride;
            }
        } else {
            lineOffset += dbOffset;
            int d0 = dataOffsets[0];

            if (numBands == 1) {
                for (int j = 0; j < h; j++) {
                    int pixelOffset = lineOffset + d0;
                    for (int i = 0; i < w; i++) {
                        pixels[dstOffset++] = data[pixelOffset] & 0xff;
                        pixelOffset += pixelStride;
                    }
                    lineOffset += scanlineStride;
                }
            } else if (numBands == 2) {
                int d1 = dataOffsets[1] - d0;
                for (int j = 0; j < h; j++) {
                    int pixelOffset = lineOffset + d0;
                    for (int i = 0; i < w; i++) {
                        pixels[dstOffset++] = data[pixelOffset] & 0xff;
                        pixels[dstOffset++] = data[pixelOffset + d1] & 0xff;
                        pixelOffset += pixelStride;
                    }
                    lineOffset += scanlineStride;
                }
            } else if (numBands == 3) {
                int d1 = dataOffsets[1] - d0;
                int d2 = dataOffsets[2] - d0;
                for (int j = 0; j < h; j++) {
                    int pixelOffset = lineOffset + d0;
                    for (int i = 0; i < w; i++) {
                        pixels[dstOffset++] = data[pixelOffset] & 0xff;
                        pixels[dstOffset++] = data[pixelOffset + d1] & 0xff;
                        pixels[dstOffset++] = data[pixelOffset + d2] & 0xff;
                        pixelOffset += pixelStride;
                    }
                    lineOffset += scanlineStride;
                }
            } else if (numBands == 4) {
                int d1 = dataOffsets[1] - d0;
                int d2 = dataOffsets[2] - d0;
                int d3 = dataOffsets[3] - d0;
                for (int j = 0; j < h; j++) {
                    int pixelOffset = lineOffset + d0;
                    for (int i = 0; i < w; i++) {
                        pixels[dstOffset++] = data[pixelOffset] & 0xff;
                        pixels[dstOffset++] = data[pixelOffset + d1] & 0xff;
                        pixels[dstOffset++] = data[pixelOffset + d2] & 0xff;
                        pixels[dstOffset++] = data[pixelOffset + d3] & 0xff;
                        pixelOffset += pixelStride;
                    }
                    lineOffset += scanlineStride;
                }
            } else {
                for (int j = 0; j < h; j++) {
                    int pixelOffset = lineOffset;
                    for (int i = 0; i < w; i++) {
                        for (int k = 0; k < numBands; k++) {
                            pixels[dstOffset++] =
                                data[pixelOffset + dataOffsets[k]] & 0xff;
                        }
                        pixelOffset += pixelStride;
                    }
                    lineOffset += scanlineStride;
                }
            }
        }

        return pixels;
    }

    public void setPixels(int x, int y, int w, int h, int[] iArray) {
        if ((x < this.minX) || (y < this.minY) ||
            (x + w > this.maxX) || (y + h > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int lineOffset = y*scanlineStride + x*pixelStride;
        int srcOffset = 0;

        if (packed) {
            lineOffset += dbOffsetPacked;
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    int value = 0;
                    for (int k = 0; k < numBands; k++) {
                        int srcValue = iArray[srcOffset++];
                        value |= ((srcValue << bitOffsets[k])
                                  & bitMasks[k]);
                    }
                    data[lineOffset + i] = (byte)value;
                }
                lineOffset += scanlineStride;
            }
        } else {
            lineOffset += dbOffset;
            int d0 = dataOffsets[0];

            if (numBands == 1) {
                for (int j = 0; j < h; j++) {
                    int pixelOffset = lineOffset + d0;
                    for (int i = 0; i < w; i++) {
                        data[pixelOffset] = (byte)iArray[srcOffset++];
                        pixelOffset += pixelStride;
                    }
                    lineOffset += scanlineStride;
                }
            } else if (numBands == 2) {
                int d1 = dataOffsets[1] - d0;
                for (int j = 0; j < h; j++) {
                    int pixelOffset = lineOffset + d0;
                    for (int i = 0; i < w; i++) {
                        data[pixelOffset] = (byte)iArray[srcOffset++];
                        data[pixelOffset + d1] = (byte)iArray[srcOffset++];
                        pixelOffset += pixelStride;
                    }
                    lineOffset += scanlineStride;
                }
            } else if (numBands == 3) {
                int d1 = dataOffsets[1] - d0;
                int d2 = dataOffsets[2] - d0;
                for (int j = 0; j < h; j++) {
                    int pixelOffset = lineOffset + d0;
                    for (int i = 0; i < w; i++) {
                        data[pixelOffset] = (byte)iArray[srcOffset++];
                        data[pixelOffset + d1] = (byte)iArray[srcOffset++];
                        data[pixelOffset + d2] = (byte)iArray[srcOffset++];
                        pixelOffset += pixelStride;
                    }
                    lineOffset += scanlineStride;
                }
            } else if (numBands == 4) {
                int d1 = dataOffsets[1] - d0;
                int d2 = dataOffsets[2] - d0;
                int d3 = dataOffsets[3] - d0;
                for (int j = 0; j < h; j++) {
                    int pixelOffset = lineOffset + d0;
                    for (int i = 0; i < w; i++) {
                        data[pixelOffset] = (byte)iArray[srcOffset++];
                        data[pixelOffset + d1] = (byte)iArray[srcOffset++];
                        data[pixelOffset + d2] = (byte)iArray[srcOffset++];
                        data[pixelOffset + d3] = (byte)iArray[srcOffset++];
                        pixelOffset += pixelStride;
                    }
                    lineOffset += scanlineStride;
                }
            } else {
                for (int j = 0; j < h; j++) {
                    int pixelOffset = lineOffset;
                    for (int i = 0; i < w; i++) {
                        for (int k = 0; k < numBands; k++) {
                            data[pixelOffset + dataOffsets[k]] =
                                (byte)iArray[srcOffset++];
                        }
                        pixelOffset += pixelStride;
                    }
                    lineOffset += scanlineStride;
                }
            }
        }

        markDirty();
    }

    public void setRect(int dx, int dy, Raster srcRaster) {
        if (!(srcRaster instanceof ByteInterleavedRaster)) {
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
            int skipX = minX - dstOffX;
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
                        width, height, srcRaster);
    }


    /**
     * Creates a subraster given a region of the raster.  The x and y
     * coordinates specify the horizontal and vertical offsets
     * from the upper-left corner of this raster to the upper-left corner
     * of the subraster.  A subset of the bands of the parent Raster may
     * be specified.  If this is null, then all the bands are present in the
     * subRaster. A translation to the subRaster may also be specified.
     * Note that the subraster will reference the same
     * DataBuffer as the parent raster, but using different offsets.
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
     * of the subRaster.  A subset of the bands of the parent Raster may
     * be specified.  If this is null, then all the bands are present in the
     * subRaster. A translation to the subRaster may also be specified.
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

        if (bandList != null)
            sm = sampleModel.createSubsetSampleModel(bandList);
        else
            sm = sampleModel;

        int deltaX = x0 - x;
        int deltaY = y0 - y;

        return new ByteInterleavedRaster(sm,
                                       (DataBufferByte) dataBuffer,
                                       new Rectangle(x0, y0, width, height),
                                       new Point(sampleModelTranslateX+deltaX,
                                                 sampleModelTranslateY+deltaY),
                                       this);
    }

    /**
     * Creates a Raster with the same layout but using a different
     * width and height, and with new zeroed data arrays.
     */
    public WritableRaster createCompatibleWritableRaster(int w, int h) {
        if (w <= 0 || h <=0) {
            throw new RasterFormatException("negative "+
                                          ((w <= 0) ? "width" : "height"));
        }

        SampleModel sm = sampleModel.createCompatibleSampleModel(w, h);

        return new ByteInterleavedRaster(sm, new Point(0,0));

    }

    /**
     * Creates a Raster with the same layout and the same
     * width and height, and with new zeroed data arrays.  If
     * the Raster is a subRaster, this will call
     * createCompatibleRaster(width, height).
     */
    public WritableRaster createCompatibleWritableRaster() {
        return createCompatibleWritableRaster(width,height);
    }

    public String toString() {
        return new String ("ByteInterleavedRaster: width = "+width+" height = "
                           + height
                           +" #numDataElements "+numDataElements
                           //  +" xOff = "+xOffset+" yOff = "+yOffset
                           +" dataOff[0] = "+dataOffsets[0]);
    }

//    /**
//     * For debugging...  prints a region of a one-band ByteInterleavedRaster
//     */
//    public void print(int x, int y, int w, int h) {
//        // REMIND:  Only works for 1 band!
//        System.out.println(this);
//        int offset = dataOffsets[0] + y*scanlineStride + x*pixelStride;
//        int off;
//        for (int yoff=0; yoff < h; yoff++, offset += scanlineStride) {
//            off = offset;
//            System.out.print("Line "+(y+yoff)+": ");
//            for (int xoff = 0; xoff < w; xoff++, off+= pixelStride) {
//                String s = Integer.toHexString(data[off]);
//                if (s.length() == 8) {
//                    s = s.substring(6,8);
//                }
//                System.out.print(s+" ");
//            }
//            System.out.println("");
//        }
//    }
}
