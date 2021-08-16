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
import java.awt.image.DataBufferUShort;
import java.awt.Rectangle;
import java.awt.Point;

/**
 * This class defines a Raster with pixels consisting of one or more 16-bit
 * data elements stored in close proximity to each other in a short integer
 * array.  The bit precision per data element is that
 * of the data type (that is, the bit precision for this Raster is 16).
 * There is only one pixel stride and one scanline stride for all
 * bands.  This type of Raster can be used with a
 * ComponentColorModel if there are multiple bands, or a
 * IndexColorModel if there is only one band.
 * <p>
 * For example, 5-6-5 RGB image data can be represented by a
 * ShortInterleavedRaster using a SinglePixelPackedSampleModel and
 * a ComponentColorModel.
 *
 *
 */
public class ShortInterleavedRaster extends ShortComponentRaster {

    /** A cached copy of minX + width for use in bounds checks. */
    private int maxX;

    /** A cached copy of minY + height for use in bounds checks. */
    private int maxY;

    /**
     *  Constructs a ShortInterleavedRaster with the given SampleModel.
     *  The Raster's upper left corner is origin and it is the same
     *  size as the SampleModel.  A DataBuffer large enough to describe the
     *  Raster is automatically created.  SampleModel must be of type
     *  PixelInterleavedSampleModel or SinglePixelPackedSampleModel.
     *  @param sampleModel     The SampleModel that specifies the layout.
     *  @param origin          The Point that specified the origin.
     */
    public ShortInterleavedRaster(SampleModel sampleModel, Point origin) {
        this(sampleModel,
             (DataBufferUShort) sampleModel.createDataBuffer(),
             new Rectangle(origin.x,
                           origin.y,
                           sampleModel.getWidth(),
                           sampleModel.getHeight()),
             origin,
             null);
    }

    /**
     * Constructs a ShortInterleavedRaster with the given SampleModel
     * and DataBuffer.  The Raster's upper left corner is origin and
     * it is the same sizes the SampleModel.  The DataBuffer is not
     * initialized and must be a DataBufferUShort compatible with SampleModel.
     * SampleModel must be of type PixelInterleavedSampleModel or
     * SinglePixelPackedSampleModel.
     * @param sampleModel     The SampleModel that specifies the layout.
     * @param dataBuffer      The DataBufferUShort that contains the image data.
     * @param origin          The Point that specifies the origin.
     */
    public ShortInterleavedRaster(SampleModel sampleModel,
                                  DataBufferUShort dataBuffer,
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
     * Constructs a ShortInterleavedRaster with the given SampleModel,
     * DataBuffer, and parent.  DataBuffer must be a DataBufferUShort and
     * SampleModel must be of type PixelInterleavedSampleModel or
     * SinglePixelPackedSampleModel.  When translated into the base Raster's
     * coordinate system, aRegion must be contained by the base Raster.
     * Origin is the coodinate in the new Raster's coordinate system of
     * the origin of the base Raster.  (The base Raster is the Raster's
     * ancestor which has no parent.)
     *
     * Note that this constructor should generally be called by other
     * constructors or create methods, it should not be used directly.
     * @param sampleModel     The SampleModel that specifies the layout.
     * @param dataBuffer      The DataBufferUShort that contains the image data.
     * @param aRegion         The Rectangle that specifies the image area.
     * @param origin          The Point that specifies the origin.
     * @param parent          The parent (if any) of this raster.
     */
    public ShortInterleavedRaster(SampleModel sampleModel,
                                  DataBufferUShort dataBuffer,
                                  Rectangle aRegion,
                                  Point origin,
                                  ShortInterleavedRaster parent)
    {

        super(sampleModel, dataBuffer, aRegion, origin, parent);
        this.maxX = minX + width;
        this.maxY = minY + height;

        this.data = stealData(dataBuffer, 0);

        // REMIND: need case for interleaved ComponentSampleModel
        if ((sampleModel instanceof PixelInterleavedSampleModel) ||
            (sampleModel instanceof ComponentSampleModel &&
             sampleModel.getNumBands() == 1)) {
            ComponentSampleModel csm = (ComponentSampleModel)sampleModel;

            this.scanlineStride = csm.getScanlineStride();
            this.pixelStride = csm.getPixelStride();
            this.dataOffsets = csm.getBandOffsets();
            int xOffset = aRegion.x - origin.x;
            int yOffset = aRegion.y - origin.y;
            for (int i = 0; i < getNumDataElements(); i++) {
                dataOffsets[i] += xOffset*pixelStride+yOffset*scanlineStride;
            }
        } else if (sampleModel instanceof SinglePixelPackedSampleModel) {
            SinglePixelPackedSampleModel sppsm =
                    (SinglePixelPackedSampleModel)sampleModel;
            this.scanlineStride = sppsm.getScanlineStride();
            this.pixelStride    = 1;
            this.dataOffsets = new int[1];
            this.dataOffsets[0] = dataBuffer.getOffset();
            int xOffset = aRegion.x - origin.x;
            int yOffset = aRegion.y - origin.y;
            dataOffsets[0] += xOffset+yOffset*scanlineStride;
        } else {
            throw new RasterFormatException("ShortInterleavedRasters must "+
              "have PixelInterleavedSampleModel, SinglePixelPackedSampleModel"+
              " or 1 band ComponentSampleModel.  Sample model is "+
              sampleModel);
        }
        this.bandOffset = this.dataOffsets[0];
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
     * is the index into the data array in which the first sample
     * of the first scanline is stored.
     * @param band  The band whose offset is returned.
     */
    public int getDataOffset(int band) {
        return dataOffsets[band];
    }

    /**
     * Returns the scanline stride -- the number of data array elements between
     * a given sample and the same sample in the same column of the next row.
     */
    public int getScanlineStride() {
        return scanlineStride;
    }

    /**
     * Returns pixel stride -- the number of data array elements  between two
     * samples for the same band on the same scanline.
     */
    public int getPixelStride() {
        return pixelStride;
    }

    /**
     * Returns a reference to the data array.
     */
    public short[] getDataStorage() {
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
        short[] outData;
        if (obj == null) {
            outData = new short[numDataElements];
        } else {
            outData = (short[])obj;
        }
        int off = (y-minY)*scanlineStride +
                  (x-minX)*pixelStride;

        for (int band = 0; band < numDataElements; band++) {
            outData[band] = data[dataOffsets[band] + off];
        }

        return outData;
    }

    /**
     * Returns an array  of data elements from the specified rectangular
     * region.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinates are out of bounds.
     * A ClassCastException will be thrown if the input object is non null
     * and references anything other than an array of transferType.
     * <pre>
     *       short[] bandData = (short[])Raster.getDataElements(x, y, w, h, null);
     *       int numDataElements = Raster.getBands();
     *       short[] pixel = new short[numDataElements];
     *       // To find the data element at location (x2, y2)
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
        if ((x < this.minX) || (y < this.minY) ||
            (x + w > this.maxX) || (y + h > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        short[] outData;
        if (obj == null) {
            outData = new short[w*h*numDataElements];
        } else {
            outData = (short[])obj;
        }
        int yoff = (y-minY)*scanlineStride +
                   (x-minX)*pixelStride;

        int xoff;
        int off = 0;
        int xstart;
        int ystart;

        for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
            xoff = yoff;
            for (xstart=0; xstart < w; xstart++, xoff += pixelStride) {
                for (int c = 0; c < numDataElements; c++) {
                    outData[off++] = data[dataOffsets[c] + xoff];
                }
            }
        }

        return outData;
    }

    /**
     * Returns a short integer array of data elements from the
     * specified rectangular region.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinates are out of bounds.
     * <pre>
     *       short[] bandData = Raster.getShortData(x, y, w, h, null);
     *       // To find the data element at location (x2, y2)
     *       short dataElenent = bandData[((y2-y)*w + (x2-x))];
     * </pre>
     * @param x        The X coordinate of the upper left pixel location.
     * @param y        The Y coordinate of the upper left pixel location.
     * @param w        Width of the sample rectangle.
     * @param h        Height of the sample rectangle.
     * @param band     The band to return.
     * @param outData  If non-null, data elements for all bands
     *                 at the specified location are returned in this array.
     * @return         Data array with data elements for all bands.
     */
    public short[] getShortData(int x, int y, int w, int h,
                               int band, short[] outData) {
        // Bounds check for 'band' will be performed automatically
        if ((x < this.minX) || (y < this.minY) ||
            (x + w > this.maxX) || (y + h > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        if (outData == null) {
            outData = new short[numDataElements*w*h];
        }
        int yoff =  (y-minY)*scanlineStride +
                    (x-minX)*pixelStride+ dataOffsets[band];
        int xoff;
        int off = 0;
        int xstart;
        int ystart;

        if (pixelStride == 1) {
            if (scanlineStride == w) {
                System.arraycopy(data, yoff, outData, 0, w*h);
            }
            else {
                for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                    System.arraycopy(data, yoff, outData, off, w);
                    off += w;
                }
            }
        }
        else {
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
     * Returns a short integer array  of data elements from the
     * specified rectangular region.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinates are out of bounds.
     * <pre>
     *       short[] bandData = Raster.getShortData(x, y, w, h, null);
     *       int numDataElements = Raster.getNumBands();
     *       short[] pixel = new short[numDataElements];
     *       // To find the data element at location (x2, y2)
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
    public short[] getShortData(int x, int y, int w, int h, short[] outData) {
        if ((x < this.minX) || (y < this.minY) ||
            (x + w > this.maxX) || (y + h > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        if (outData == null) {
            outData = new short[numDataElements*w*h];
        }
        int yoff = (y-minY)*scanlineStride +
                   (x-minX)*pixelStride;
        int xoff;
        int off = 0;
        int xstart;
        int ystart;

        for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
            xoff = yoff;
            for (xstart=0; xstart < w; xstart++, xoff += pixelStride) {
                for (int c = 0; c < numDataElements; c++) {
                    outData[off++] = data[dataOffsets[c] + xoff];
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
        short[] inData = (short[])obj;
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
        int dstOffX = x + inRaster.getMinX();
        int dstOffY = y + inRaster.getMinY();
        int width  = inRaster.getWidth();
        int height = inRaster.getHeight();
        if ((dstOffX < this.minX) || (dstOffY < this.minY) ||
            (dstOffX + width > this.maxX) || (dstOffY + height > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }

        setDataElements(dstOffX, dstOffY, width, height, inRaster);
    }

    /**
     * Stores the Raster data at the specified location.
     * @param dstX The absolute X coordinate of the destination pixel
     * that will receive a copy of the upper-left pixel of the
     * inRaster
     * @param dstY The absolute Y coordinate of the destination pixel
     * that will receive a copy of the upper-left pixel of the
     * inRaster
     * @param width      The number of pixels to store horizontally
     * @param height     The number of pixels to store vertically
     * @param inRaster   Raster of data to place at x,y location.
     */
    private void setDataElements(int dstX, int dstY,
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

//      REMIND: Do something faster!
//      if (inRaster instanceof ShortInterleavedRaster) {
//      }

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
        if ((x < this.minX) || (y < this.minY) ||
            (x + w > this.maxX) || (y + h > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        short[] inData = (short[])obj;
        int yoff = (y-minY)*scanlineStride +
                   (x-minX)*pixelStride;
        int xoff;
        int off = 0;
        int xstart;
        int ystart;

        for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
            xoff = yoff;
            for (xstart=0; xstart < w; xstart++, xoff += pixelStride) {
                for (int c = 0; c < numDataElements; c++) {
                    data[dataOffsets[c] + xoff] = inData[off++];
                }
            }
        }

        markDirty();
    }

    /**
     * Stores a short integer array of data elements into the
     * specified rectangular region.
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
     * @param band     The band to set.
     * @param inData   The data elements to be stored.
     */
    public void putShortData(int x, int y, int w, int h,
                             int band, short[] inData) {
        // Bounds check for 'band' will be performed automatically
        if ((x < this.minX) || (y < this.minY) ||
            (x + w > this.maxX) || (y + h > this.maxY)) {
            throw new ArrayIndexOutOfBoundsException
                ("Coordinate out of bounds!");
        }
        int yoff =  (y-minY)*scanlineStride +
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
     * Stores a short integer array of data elements into the
     * specified rectangular region.
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
    public void putShortData(int x, int y, int w, int h, short[] inData) {
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

        for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
            xoff = yoff;
            for (xstart=0; xstart < w; xstart++, xoff += pixelStride) {
                for (int c = 0; c < numDataElements; c++) {
                    data[dataOffsets[c] + xoff] = inData[off++];
                }
            }
        }

        markDirty();
    }

    /**
     * Creates a subraster given a region of the raster.  The x and y
     * coordinates specify the horizontal and vertical offsets
     * from the upper-left corner of this raster to the upper-left corner
     * of the subraster.  A subset of the bands of the parent Raster may
     * be specified.  If this is null, then all the bands are present in the
     * subRaster. A translation to the subRaster may also be specified.
     * Note that the subraster will reference the same
     * band objects as the parent raster, but using different offsets.
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
    public Raster createChild (int x, int y,
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
     * DataBuffers as the parent Raster, but using different offsets.
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

        return new ShortInterleavedRaster(sm,
                                       (DataBufferUShort) dataBuffer,
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

        return new ShortInterleavedRaster(sm, new Point(0, 0));
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
        return new String ("ShortInterleavedRaster: width = "+width
                           +" height = " + height
                           +" #numDataElements "+numDataElements);
                           // +" xOff = "+xOffset+" yOff = "+yOffset);
    }

}
