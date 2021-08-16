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
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.RasterFormatException;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.DataBufferByte;
import java.awt.Rectangle;
import java.awt.Point;

/**
 * This class defines a Raster with pixels consisting of multiple
 * 8-bit samples stored in possibly separate arrays for each band.
 * Operations on sets of pixels are performed on a given band in the
 * Raster before moving on to the next band.  The arrays used for
 * storage may be distinct or shared between some or all of the bands.
 * Each band additionally has an offset that is added to determine the
 * DataBuffer location of each pixel.
 *
 * There is only one scanline stride for all bands.  The pixel stride
 * is always equal to one.  This type of raster can be used with a
 * ComponentColorModel. This class requires a BandedSampleModel.
 *
 */
public class ByteBandedRaster extends SunWritableRaster {

    /** Data offsets for each band of image data. */
    int[]         dataOffsets;

    /** Scanline stride of the image data contained in this Raster. */
    int           scanlineStride;

    /** The image data array. */
    byte[][]      data;

    /** A cached copy of minX + width for use in bounds checks. */
    private int maxX;

    /** A cached copy of minY + height for use in bounds checks. */
    private int maxY;

    /**
     *  Constructs a ByteBandedRaster with the given sampleModel. The
     *  Raster's upper left corner is origin and it is the same
     *  size as the SampleModel.  A dataBuffer large
     *  enough to describe the Raster is automatically created. SampleModel
     *  must be of type BandedSampleModel.
     *  @param sampleModel     The SampleModel that specifies the layout.
     *  @param origin          The Point that specifies the origin.
     */
    public ByteBandedRaster(SampleModel sampleModel, Point origin) {
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
     *  Constructs a ByteBanded Raster with the given sampleModel
     *  and DataBuffer. The Raster's upper left corner is origin and
     *  it is the same size as the SampleModel.  The DataBuffer is not
     *  initialized and must be a DataBufferShort compatible with SampleModel.
     *  SampleModel must be of type BandedSampleModel.
     *  @param sampleModel     The SampleModel that specifies the layout.
     *  @param dataBuffer      The DataBufferByte that contains the image data.
     *  @param origin          The Point that specifies the origin.
     */
    public ByteBandedRaster(SampleModel sampleModel,
                            DataBufferByte dataBuffer,
                            Point origin)
    {
        this(sampleModel, dataBuffer,
             new Rectangle(origin.x , origin.y,
                           sampleModel.getWidth(),
                           sampleModel.getHeight()),
             origin, null);
    }

    /**
     *  Constructs a ByteBandedRaster with the given sampleModel,
     *  DataBuffer, and parent. DataBuffer must be a DataBufferShort and
     *  SampleModel must be of type BandedSampleModel.
     *  When translated into the base Raster's
     *  coordinate system, aRegion must be contained by the base Raster.
     *  Origin is the coordinate in the new Raster's coordinate system of
     *  the origin of the base Raster.  (The base Raster is the Raster's
     *  ancestor which has no parent.)
     *
     *  Note that this constructor should generally be called by other
     *  constructors or create methods, it should not be used directly.
     *  @param sampleModel     The SampleModel that specifies the layout.
     *  @param dataBuffer      The DataBufferByte that contains the image data.
     *  @param aRegion         The Rectangle that specifies the image area.
     *  @param origin          The Point that specifies the origin.
     *  @param parent          The parent (if any) of this raster.
     */
    public ByteBandedRaster(SampleModel sampleModel,
                            DataBufferByte dataBuffer,
                            Rectangle aRegion,
                            Point origin,
                            ByteBandedRaster parent)
    {
        super(sampleModel, dataBuffer, aRegion, origin, parent);
        this.maxX = minX + width;
        this.maxY = minY + height;

        if (sampleModel instanceof BandedSampleModel) {
            BandedSampleModel bsm = (BandedSampleModel)sampleModel;
            this.scanlineStride = bsm.getScanlineStride();
            int[] bankIndices = bsm.getBankIndices();
            int[] bandOffsets = bsm.getBandOffsets();
            int[] dOffsets = dataBuffer.getOffsets();
            dataOffsets = new int[bankIndices.length];
            data = new byte[bankIndices.length][];
            int xOffset = aRegion.x - origin.x;
            int yOffset = aRegion.y - origin.y;
            for (int i = 0; i < bankIndices.length; i++) {
               data[i] = stealData(dataBuffer, bankIndices[i]);
               dataOffsets[i] = dOffsets[bankIndices[i]] +
                   xOffset + yOffset*scanlineStride + bandOffsets[i];
            }
        } else {
            throw new RasterFormatException("ByteBandedRasters must have"+
                "BandedSampleModels");
        }
        verify();
    }


    /**
     * Returns a copy of the data offsets array. For each band the data
     * offset is the index into the band's data array, of the first sample
     * of the band.
     */
    public int[] getDataOffsets() {
        return dataOffsets.clone();
    }

    /**
     * Returns data offset for the specified band.  The data offset
     * is the index into the band's data array
     * in which the first sample of the first scanline is stored.
     * @param band The band whose offset is returned.
     */
    public int getDataOffset(int band) {
        return dataOffsets[band];
    }

    /**
     * Returns the scanline stride -- the number of data array elements
     * between a given sample and the sample in the same column
     * of the next row in the same band.
     */
    public int getScanlineStride() {
        return scanlineStride;
    }

    /**
     * Returns the pixel stride, which is always equal to one for
     * a Raster with a BandedSampleModel.
     */
    public int getPixelStride() {
        return 1;
    }

    /**
     * Returns a reference to the entire data array.
     */
    public byte[][] getDataStorage() {
        return data;
    }

    /**
     * Returns a reference to the specific band data array.
     */
    public byte[] getDataStorage(int band) {
        return data[band];
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
     * @return An object reference to an array of type defined by
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
        int off = (y-minY)*scanlineStride + (x-minX);

        for (int band = 0; band < numDataElements; band++) {
            outData[band] = data[band][dataOffsets[band] + off];
        }

        return outData;
    }

    /**
     * Returns an  array  of data elements from the specified
     * rectangular region.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinates are out of bounds.
     * A ClassCastException will be thrown if the input object is non null
     * and references anything other than an array of transferType.
     * <pre>
     *       byte[] bandData = (byte[])raster.getDataElement(x, y, w, h, null);
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
        int yoff = (y-minY)*scanlineStride + (x-minX);

        for (int c = 0; c < numDataElements; c++) {
            int off = c;
            byte[] bank = data[c];
            int dataOffset = dataOffsets[c];

            int yoff2 = yoff;
            for (int ystart=0; ystart < h; ystart++, yoff2 += scanlineStride) {
                int xoff = dataOffset + yoff2;
                for (int xstart=0; xstart < w; xstart++) {
                    outData[off] = bank[xoff++];
                    off += numDataElements;
                }
            }
        }

        return outData;
    }

    /**
     * Returns a byte array  of data elements from the specified rectangular
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
            outData = new byte[scanlineStride*h];
        }
        int yoff = (y-minY)*scanlineStride + (x-minX) + dataOffsets[band];

        if (scanlineStride == w) {
            System.arraycopy(data[band], yoff, outData, 0, w*h);
        } else {
            int off = 0;
            for (int ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                System.arraycopy(data[band], yoff, outData, off, w);
                off += w;
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
            outData = new byte[numDataElements*scanlineStride*h];
        }
        int yoff = (y-minY)*scanlineStride + (x-minX);

        for (int c = 0; c < numDataElements; c++) {
            int off = c;
            byte[] bank = data[c];
            int dataOffset = dataOffsets[c];

            // REMIND: Should keep track if dataoffsets are in a nice order
            int yoff2 = yoff;
            for (int ystart=0; ystart < h; ystart++, yoff2 += scanlineStride) {
                int xoff = dataOffset + yoff2;
                for (int xstart=0; xstart < w; xstart++) {
                    outData[off] = bank[xoff++];
                    off += numDataElements;
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
        int off = (y-minY)*scanlineStride + (x-minX);
        for (int i = 0; i < numDataElements; i++) {
            data[i][dataOffsets[i] + off] = inData[i];
        }
        markDirty();
    }

    /**
     * Stores the Raster data at the specified location.
     * An ArrayIndexOutOfBounds exception will be thrown at runtime
     * if the pixel coordinate is out of bounds.
     * @param x          The X coordinate of the pixel location.
     * @param y          The Y coordinate of the pixel location.
     * @param inRaster   Raster of data to place at x,y location.
     */
    public void setDataElements(int x, int y, Raster inRaster) {
        int dstOffX = inRaster.getMinX() + x;
        int dstOffY = inRaster.getMinY() + y;
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

        int srcOffX = inRaster.getMinX();
        int srcOffY = inRaster.getMinY();
        Object tdata = null;

//      // REMIND: Do something faster!
//      if (inRaster instanceof ByteBandedRaster) {
//      }

        for (int startY=0; startY < height; startY++) {
            // Grab one scanline at a time
            tdata = inRaster.getDataElements(srcOffX, srcOffY+startY,
                                             width, 1, tdata);
            setDataElements(dstX, dstY+startY, width, 1, tdata);
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
        byte[] inData = (byte[])obj;
        int yoff = (y-minY)*scanlineStride + (x-minX);

        for (int c = 0; c < numDataElements; c++) {
            int off = c;
            byte[] bank = data[c];
            int dataOffset = dataOffsets[c];

            int yoff2 = yoff;
            for (int ystart=0; ystart < h; ystart++, yoff2 += scanlineStride) {
                int xoff = dataOffset + yoff2;
                for (int xstart=0; xstart < w; xstart++) {
                    bank[xoff++] = inData[off];
                    off += numDataElements;
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
        int yoff = (y-minY)*scanlineStride + (x-minX) + dataOffsets[band];
        int xoff;
        int off = 0;
        int xstart;
        int ystart;

        if (scanlineStride == w) {
            System.arraycopy(inData, 0, data[band], yoff, w*h);
        } else {
            for (ystart=0; ystart < h; ystart++, yoff += scanlineStride) {
                System.arraycopy(inData, off, data[band], yoff, w);
                off += w;
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
        int yoff = (y-minY)*scanlineStride + (x-minX);

        for (int c = 0; c < numDataElements; c++) {
            int off = c;
            byte[] bank = data[c];
            int dataOffset = dataOffsets[c];

            int yoff2 = yoff;
            for (int ystart=0; ystart < h; ystart++, yoff2 += scanlineStride) {
                int xoff = dataOffset + yoff2;
                for (int xstart=0; xstart < w; xstart++) {
                    bank[xoff++] = inData[off];
                    off += numDataElements;
                }
            }
        }

        markDirty();
    }

    /**
     * Creates a Writable subraster given a region of the raster.  The x and y
     * coordinates specify the horizontal and vertical offsets
     * from the upper-left corner of this raster to the upper-left corner
     * of the subraster.  A subset of the bands of the parent Raster may
     * be specified.  If this is null, then all the bands are present in the
     * subRaster. A translation to the subRaster may also be specified.
     * Note that the subraster will reference the same
     * DataBuffers as the parent raster, but using different offsets.
     * @param x               X offset.
     * @param y               Y offset.
     * @param width           Width of the subraster.
     * @param height          Height of the subraster.
     * @param x0              Translated X origin of the subraster.
     * @param y0              Translated Y origin of the subraster.
     * @param bandList        Array of band indices.
     * @exception RasterFormatException
     *            if the specified bounding box is outside of the parent raster.
     */
    public WritableRaster createWritableChild (int x, int y,
                                               int width, int height,
                                               int x0, int y0,
                                               int[] bandList) {

        if (x < this.minX) {
            throw new RasterFormatException("x lies outside raster");
        }
        if (y < this.minY) {
            throw new RasterFormatException("y lies outside raster");
        }
        if ((x+width < x) || (x+width > this.width + this.minX)) {
            throw new RasterFormatException("(x + width) is outside raster") ;
        }
        if ((y+height < y) || (y+height > this.height + this.minY)) {
            throw new RasterFormatException("(y + height) is outside raster");
        }

        SampleModel sm;

        if (bandList != null)
            sm = sampleModel.createSubsetSampleModel(bandList);
        else
            sm = sampleModel;

        int deltaX = x0 - x;
        int deltaY = y0 - y;

        return new ByteBandedRaster(sm,
                                    (DataBufferByte) dataBuffer,
                                    new Rectangle(x0,y0,width,height),
                                    new Point(sampleModelTranslateX+deltaX,
                                              sampleModelTranslateY+deltaY),
                                    this);
    }

    /**
     * Creates a subraster given a region of the raster.  The x and y
     * coordinates specify the horizontal and vertical offsets
     * from the upper-left corner of this raster to the upper-left corner
     * of the subraster.  A subset of the bands of the parent Raster may
     * be specified.  If this is null, then all the bands are present in the
     * subRaster. A translation to the subRaster may also be specified.
     * Note that the subraster will reference the same
     * DataBuffers as the parent raster, but using different offsets.
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
                                   int x0, int y0,
                                   int[] bandList) {
        return createWritableChild(x, y, width, height, x0, y0, bandList);
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

        SampleModel sm = sampleModel.createCompatibleSampleModel(w,h);

        return new ByteBandedRaster(sm, new Point(0,0));
    }

    /**
     * Creates a Raster with the same layout and the same
     * width and height, and with new zeroed data arrays.  If
     * the Raster is a subRaster, this will call
     * createCompatibleRaster(width, height).
     */
    public WritableRaster createCompatibleWritableRaster() {
        return createCompatibleWritableRaster(width, height);
    }

    /**
     * Verify that the layout parameters are consistent with the data.
     * Verifies whether the data buffer has enough data for the raster,
     * taking into account offsets, after ensuring all offsets are >=0.
     * @throws RasterFormatException if a problem is detected.
     */
    private void verify() {

        /* Need to re-verify the dimensions since a sample model may be
         * specified to the constructor
         */
        if (width <= 0 || height <= 0 ||
            height > (Integer.MAX_VALUE / width))
        {
            throw new RasterFormatException("Invalid raster dimension");
        }

        if (scanlineStride < 0 ||
            scanlineStride > (Integer.MAX_VALUE / height))
        {
            // integer overflow
            throw new RasterFormatException("Incorrect scanline stride: "
                    + scanlineStride);
        }

        if ((long)minX - sampleModelTranslateX < 0 ||
            (long)minY - sampleModelTranslateY < 0) {

            throw new RasterFormatException("Incorrect origin/translate: (" +
                    minX + ", " + minY + ") / (" +
                    sampleModelTranslateX + ", " + sampleModelTranslateY + ")");
        }


        if (height > 1 || minY - sampleModelTranslateY > 0) {
            // buffer should contain at least one scanline
            for (int i = 0; i < data.length; i++) {
                if (scanlineStride > data[i].length) {
                    throw new RasterFormatException("Incorrect scanline stride: "
                        + scanlineStride);
                }
            }
        }

        // Make sure data for Raster is in a legal range
        for (int i=0; i < dataOffsets.length; i++) {
            if (dataOffsets[i] < 0) {
                throw new RasterFormatException("Data offsets for band "+i+
                                                "("+dataOffsets[i]+
                                                ") must be >= 0");
            }
        }

        int lastScanOffset = (height - 1) * scanlineStride;

        if ((width - 1) > (Integer.MAX_VALUE - lastScanOffset)) {
            throw new RasterFormatException("Invalid raster dimension");
        }
        int lastPixelOffset = lastScanOffset + (width-1);

        int maxIndex = 0;
        int index;

        for (int i=0; i < numDataElements; i++) {
            if (dataOffsets[i] > (Integer.MAX_VALUE - lastPixelOffset)) {
                throw new RasterFormatException("Invalid raster dimension");
            }
            index = lastPixelOffset + dataOffsets[i];
            if (index > maxIndex) {
                maxIndex = index;
            }
        }

        if (data.length == 1) {
            if (data[0].length <= maxIndex*numDataElements) {
                throw new RasterFormatException("Data array too small "+
                                                "(it is "+data[0].length+
                                                " and should be > "+
                                                (maxIndex*numDataElements)+
                                                " )");
            }
        }
        else {
            for (int i=0; i < numDataElements; i++) {
                if (data[i].length <= maxIndex) {
                    throw new RasterFormatException("Data array too small "+
                                                    "(it is "+data[i].length+
                                                    " and should be > "+
                                                    maxIndex+" )");
                }
            }
        }
    }

    public String toString() {
        return new String ("ByteBandedRaster: width = "+width+" height = "
                           + height
                           +" #bands "+numDataElements
                           +" minX = "+minX+" minY = "+minY);
    }

}
