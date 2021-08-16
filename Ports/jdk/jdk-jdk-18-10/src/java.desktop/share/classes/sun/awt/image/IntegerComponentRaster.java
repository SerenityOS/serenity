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
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.DataBufferInt;
import java.awt.Rectangle;
import java.awt.Point;

/**
 * This class defines a Raster with pixels consisting of one or more 32-bit
 * data elements stored in close proximity to each other in a integer array.
 * The bit precision per data element is that
 * of the data type (that is, the bit precision for this raster is 32).
 * There is only one pixel stride and one scanline stride for all
 * bands.  For a given pixel, all samples fit in N data elements and these
 * N data elements hold samples for only one pixel.  This type of Raster
 * can be used with a PackedColorModel.
 * <p>
 * For example, if there is only one data element per pixel, a
 * SinglePixelPackedSampleModel can be used to represent multiple
 * bands with a PackedColorModel (including a DirectColorModel) for
 * color interpretation.
 *
 */
public class IntegerComponentRaster extends SunWritableRaster {

    static final int TYPE_CUSTOM                = 0;
    static final int TYPE_BYTE_SAMPLES          = 1;
    static final int TYPE_USHORT_SAMPLES        = 2;
    static final int TYPE_INT_SAMPLES           = 3;
    static final int TYPE_BYTE_BANDED_SAMPLES   = 4;
    static final int TYPE_USHORT_BANDED_SAMPLES = 5;
    static final int TYPE_INT_BANDED_SAMPLES    = 6;
    static final int TYPE_BYTE_PACKED_SAMPLES   = 7;
    static final int TYPE_USHORT_PACKED_SAMPLES = 8;
    static final int TYPE_INT_PACKED_SAMPLES    = 9;
    static final int TYPE_INT_8BIT_SAMPLES      = 10;
    static final int TYPE_BYTE_BINARY_SAMPLES   = 11;

    /** private band offset for use by native code */
    protected int bandOffset;

    /** Data offsets for each band of image data. */
    protected int[]         dataOffsets;

    /** Scanline stride of the image data contained in this Raster. */
    protected int           scanlineStride;

    /** Pixel stride of the image data contained in this Raster. */
    protected int           pixelStride;

    /** The image data array. */
    protected int[]         data;

    /** The number of data elements required to store a pixel. */
    protected int           numDataElems;

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
     *  Constructs a IntegerComponentRaster with the given SampleModel.
     *  The Raster's upper left corner is origin and it is the same
     *  size as the SampleModel.  A DataBuffer large enough to describe the
     *  Raster is automatically created.  SampleModel must be of type
     *  SinglePixelPackedSampleModel.
     *  @param sampleModel     The SampleModel that specifies the layout.
     *  @param origin          The Point that specified the origin.
     */
    public IntegerComponentRaster(SampleModel sampleModel, Point origin) {
        this(sampleModel,
             (DataBufferInt) sampleModel.createDataBuffer(),
             new Rectangle(origin.x,
                           origin.y,
                           sampleModel.getWidth(),
                           sampleModel.getHeight()),
             origin,
             null);
    }

    /**
     * Constructs a IntegerComponentRaster with the given SampleModel
     * and DataBuffer.  The Raster's upper left corner is origin and
     * it is the same sizes the SampleModel.  The DataBuffer is not
     * initialized and must be a DataBufferInt compatible with SampleModel.
     * SampleModel must be of type SinglePixelPackedSampleModel.
     * @param sampleModel     The SampleModel that specifies the layout.
     * @param dataBuffer      The DataBufferInt that contains the image data.
     * @param origin          The Point that specifies the origin.
     */
    public IntegerComponentRaster(SampleModel sampleModel,
                                  DataBufferInt dataBuffer,
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
     * Constructs a IntegerComponentRaster with the given SampleModel,
     * DataBuffer, and parent.  DataBuffer must be a DataBufferInt and
     * SampleModel must be of type SinglePixelPackedSampleModel.
     * When translated into the base Raster's
     * coordinate system, aRegion must be contained by the base Raster.
     * Origin is the coodinate in the new Raster's coordinate system of
     * the origin of the base Raster.  (The base Raster is the Raster's
     * ancestor which has no parent.)
     *
     * Note that this constructor should generally be called by other
     * constructors or create methods, it should not be used directly.
     * @param sampleModel     The SampleModel that specifies the layout.
     * @param dataBuffer      The DataBufferInt that contains the image data.
     * @param aRegion         The Rectangle that specifies the image area.
     * @param origin          The Point that specifies the origin.
     * @param parent          The parent (if any) of this raster.
     */
    public IntegerComponentRaster(SampleModel sampleModel,
                                  DataBufferInt dataBuffer,
                                  Rectangle aRegion,
                                  Point origin,
                                  IntegerComponentRaster parent)
    {
        super(sampleModel,dataBuffer,aRegion,origin,parent);
        this.maxX = minX + width;
        this.maxY = minY + height;

        if (dataBuffer.getNumBanks() != 1) {
            throw new
                RasterFormatException("DataBuffer for IntegerComponentRasters"+
                                      " must only have 1 bank.");
        }
        this.data = stealData(dataBuffer, 0);

        if (sampleModel instanceof SinglePixelPackedSampleModel) {
            SinglePixelPackedSampleModel sppsm =
                    (SinglePixelPackedSampleModel)sampleModel;
            int[] boffsets = sppsm.getBitOffsets();
            boolean notByteBoundary = false;
            for (int i=1; i < boffsets.length; i++) {
                if ((boffsets[i]%8) != 0) {
                    notByteBoundary = true;
                }
            }
            this.type = (notByteBoundary
                         ? IntegerComponentRaster.TYPE_INT_PACKED_SAMPLES
                         : IntegerComponentRaster.TYPE_INT_8BIT_SAMPLES);

            this.scanlineStride = sppsm.getScanlineStride();
            this.pixelStride    = 1;
            this.dataOffsets = new int[1];
            this.dataOffsets[0] = dataBuffer.getOffset();
            this.bandOffset = this.dataOffsets[0];
            int xOffset = aRegion.x - origin.x;
            int yOffset = aRegion.y - origin.y;
            dataOffsets[0] += xOffset+yOffset*scanlineStride;
            this.numDataElems = sppsm.getNumDataElements();
        } else {
            throw new RasterFormatException("IntegerComponentRasters must have"+
                                            " SinglePixelPackedSampleModel");
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
     * Returns data offset for the specified band.  The data offset
     * is the index into the data array in which the first sample
     * of the first scanline is stored.
     */
    public int getDataOffset(int band) {
        return dataOffsets[band];
    }


    /**
     * Returns the scanline stride -- the number of data array elements between
     * a given sample and the sample in the same column of the next row.
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
    public int[] getDataStorage() {
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
        int[] outData;
        if (obj == null) {
            outData = new int[numDataElements];
        } else {
            outData = (int[])obj;
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
     <pre>
     *       int[] bandData = (int[])raster.getDataElements(x, y, w, h, null);
     *       int numDataElements = raster.getNumDataElements();
     *       int[] pixel = new int[numDataElements];
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
        int[] outData;
        if (obj instanceof int[]) {
            outData = (int[])obj;
        } else {
            outData = new int[numDataElements*w*h];
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
        int[] inData = (int[])obj;

        int off = (y-minY)*scanlineStride +
                  (x-minX)*pixelStride;

        for (int i = 0; i < numDataElements; i++) {
            data[dataOffsets[i] + off] = inData[i];
        }

        markDirty();
    }


    /**
     * Stores the Raster data at the specified location.
     * The transferType of the inputRaster must match this raster.
     * An ArrayIndexOutOfBoundsException will be thrown at runtime
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
        int[] tdata = null;

        if (inRaster instanceof IntegerComponentRaster &&
            (pixelStride == 1) && (numDataElements == 1)) {
            IntegerComponentRaster ict = (IntegerComponentRaster) inRaster;
            if (ict.getNumDataElements() != 1) {
                throw new ArrayIndexOutOfBoundsException("Number of bands"+
                                                         " does not match");
            }

            // Extract the raster parameters
            tdata    = ict.getDataStorage();
            int tss  = ict.getScanlineStride();
            int toff = ict.getDataOffset(0);

            int srcOffset = toff;

            int dstOffset = dataOffsets[0]+(dstY-minY)*scanlineStride+
                                           (dstX-minX);


            // Fastest case.  We can copy scanlines
            if (ict.getPixelStride() == pixelStride) {
                width *= pixelStride;

                // Loop through all of the scanlines and copy the data
                for (int startY=0; startY < height; startY++) {
                    System.arraycopy(tdata, srcOffset, data, dstOffset, width);
                    srcOffset += tss;
                    dstOffset += scanlineStride;
                }
                markDirty();
                return;
            }
        }

        Object odata = null;
        for (int startY=0; startY < height; startY++) {
            odata = inRaster.getDataElements(srcOffX, srcOffY+startY,
                                             width, 1, odata);
            setDataElements(dstX, dstY+startY,
                            width, 1, odata);
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
        int[] inData = (int[])obj;

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
        if ((x+width < x) || (x+width > this.minX + this.width)) {
            throw new RasterFormatException("(x + width) is outside raster");
        }
        if ((y+height < y) || (y+height > this.minY + this.height)) {
            throw new RasterFormatException("(y + height) is outside raster");
        }

        SampleModel sm;

        if (bandList != null)
            sm = sampleModel.createSubsetSampleModel(bandList);
        else
            sm = sampleModel;

        int deltaX = x0 - x;
        int deltaY = y0 - y;

        return new IntegerComponentRaster(sm,
                                          (DataBufferInt) dataBuffer,
                                          new Rectangle(x0,y0,width,height),
                                          new Point(sampleModelTranslateX+deltaX,
                                                    sampleModelTranslateY+deltaY),
                                          this);
    }


    /**
     * Creates a subraster given a region of the raster.  The x and y
     * coordinates specify the horizontal and vertical offsets
     * from the upper-left corner of this raster to the upper-left corner
     * of the subraster.  A subset of the bands of the parent raster may
     * be specified. If this is null, then all the bands are present in the
     * subRaster. Note that the subraster will reference the same
     * DataBuffer as the parent raster, but using different offsets.
     * @param x               X offset.
     * @param y               Y offset.
     * @param width           Width (in pixels) of the subraster.
     * @param height          Height (in pixels) of the subraster.
     * @param x0              Translated X origin of the subRaster.
     * @param y0              Translated Y origin of the subRaster.
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
     * Creates a raster with the same band layout but using a different
     * width and height, and with new zeroed data arrays.
     */
    public WritableRaster createCompatibleWritableRaster(int w, int h) {
        if (w <= 0 || h <=0) {
            throw new RasterFormatException("negative "+
                                          ((w <= 0) ? "width" : "height"));
        }

        SampleModel sm = sampleModel.createCompatibleSampleModel(w,h);

        return new IntegerComponentRaster(sm, new Point(0,0));
    }

    /**
     * Creates a raster with the same data layout and the same
     * width and height, and with new zeroed data arrays.  If
     * the raster is a subraster, this will call
     * createCompatibleRaster(width, height).
     */
    public WritableRaster createCompatibleWritableRaster() {
        return createCompatibleWritableRaster(width,height);
    }

    /**
     * Verify that the layout parameters are consistent with the data.
     *
     * The method verifies whether scanline stride and pixel stride do not
     * cause an integer overflow during calculation of a position of the pixel
     * in data buffer. It also verifies whether the data buffer has enough data
     *  to correspond the raster layout attributes.
     *
     * @throws RasterFormatException if an integer overflow is detected,
     * or if data buffer has not enough capacity.
     */
    protected final void verify() {
        /* Need to re-verify the dimensions since a sample model may be
         * specified to the constructor
         */
        if (width <= 0 || height <= 0 ||
            height > (Integer.MAX_VALUE / width))
        {
            throw new RasterFormatException("Invalid raster dimension");
        }

        if (dataOffsets[0] < 0) {
            throw new RasterFormatException("Data offset ("+dataOffsets[0]+
                                            ") must be >= 0");
        }

        if ((long)minX - sampleModelTranslateX < 0 ||
            (long)minY - sampleModelTranslateY < 0) {

            throw new RasterFormatException("Incorrect origin/translate: (" +
                    minX + ", " + minY + ") / (" +
                    sampleModelTranslateX + ", " + sampleModelTranslateY + ")");
        }

        // we can be sure that width and height are greater than 0
        if (scanlineStride < 0 ||
            scanlineStride > (Integer.MAX_VALUE / height))
        {
            // integer overflow
            throw new RasterFormatException("Incorrect scanline stride: "
                    + scanlineStride);
        }

        if (height > 1 || minY - sampleModelTranslateY > 0) {
            // buffer should contain at least one scanline
            if (scanlineStride > data.length) {
                throw new RasterFormatException("Incorrect scanline stride: "
                        + scanlineStride);
            }
        }

        int lastScanOffset = (height - 1) * scanlineStride;

        if (pixelStride < 0 ||
            pixelStride > (Integer.MAX_VALUE / width) ||
            pixelStride > data.length)
        {
            // integer overflow
            throw new RasterFormatException("Incorrect pixel stride: "
                    + pixelStride);
        }
        int lastPixelOffset = (width - 1) * pixelStride;

        if (lastPixelOffset > (Integer.MAX_VALUE - lastScanOffset)) {
            // integer overflow
            throw new RasterFormatException("Incorrect raster attributes");
        }
        lastPixelOffset += lastScanOffset;

        int index;
        int maxIndex = 0;
        for (int i = 0; i < numDataElements; i++) {
            if (dataOffsets[i] > (Integer.MAX_VALUE - lastPixelOffset)) {
                throw new RasterFormatException("Incorrect band offset: "
                            + dataOffsets[i]);
            }

            index = lastPixelOffset + dataOffsets[i];

            if (index > maxIndex) {
                maxIndex = index;
            }
        }
        if (data.length <= maxIndex) {
            throw new RasterFormatException("Data array too small (should be > "
                    + maxIndex + " )");
        }
    }

    public String toString() {
        return new String ("IntegerComponentRaster: width = "+width
                           +" height = " + height
                           +" #Bands = " + numBands
                           +" #DataElements "+numDataElements
                           +" xOff = "+sampleModelTranslateX
                           +" yOff = "+sampleModelTranslateY
                           +" dataOffset[0] "+dataOffsets[0]);
    }

//    /**
//     * For debugging...  prints a region of a one-band IntegerComponentRaster
//     */
//    public void print(int x, int y, int w, int h) {
//        // REMIND:  Only works for 1 band!
//        System.out.println(this);
//        int offset = dataOffsets[0] + y*scanlineStride + x*pixelStride;
//        int off;
//        for (int yoff=0; yoff < h; yoff++, offset += scanlineStride) {
//            off = offset;
//            System.out.print("Line "+(sampleModelTranslateY+y+yoff)+": ");
//            for (int xoff = 0; xoff < w; xoff++, off+= pixelStride) {
//                System.out.print(Integer.toHexString(data[off])+" ");
//            }
//            System.out.println("");
//        }
//    }

}
