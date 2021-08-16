/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.bmp;

import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.BufferedImage;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.ByteOrder;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.event.IIOWriteWarningListener;


import javax.imageio.plugins.bmp.BMPImageWriteParam;
import com.sun.imageio.plugins.common.ImageUtil;
import com.sun.imageio.plugins.common.I18N;

/**
 * The Java Image IO plugin writer for encoding a binary RenderedImage into
 * a BMP format.
 *
 * The encoding process may clip, subsample using the parameters
 * specified in the {@code ImageWriteParam}.
 *
 * @see javax.imageio.plugins.bmp.BMPImageWriteParam
 */
public class BMPImageWriter extends ImageWriter implements BMPConstants {
    /** The output stream to write into */
    private ImageOutputStream stream = null;
    private ByteArrayOutputStream embedded_stream = null;
    private int version;
    private int compressionType;
    private boolean isTopDown;
    private int w, h;
    private int compImageSize = 0;
    private int[] bitMasks;
    private int[] bitPos;
    private byte[] bpixels;
    private short[] spixels;
    private int[] ipixels;

    /** Constructs {@code BMPImageWriter} based on the provided
     *  {@code ImageWriterSpi}.
     */
    public BMPImageWriter(ImageWriterSpi originator) {
        super(originator);
    }

    @Override
    public void setOutput(Object output) {
        super.setOutput(output); // validates output
        if (output != null) {
            if (!(output instanceof ImageOutputStream))
                throw new IllegalArgumentException(I18N.getString("BMPImageWriter0"));
            this.stream = (ImageOutputStream)output;
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        } else
            this.stream = null;
    }

    @Override
    public ImageWriteParam getDefaultWriteParam() {
        return new BMPImageWriteParam();
    }

    @Override
    public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
        return null;
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType,
                                               ImageWriteParam param) {
        BMPMetadata meta = new BMPMetadata();
        meta.bmpVersion = VERSION_3;
        meta.compression = getPreferredCompressionType(imageType);
        if (param != null
            && param.getCompressionMode() == ImageWriteParam.MODE_EXPLICIT) {
            meta.compression = BMPCompressionTypes.getType(param.getCompressionType());
        }
        meta.bitsPerPixel = (short)imageType.getColorModel().getPixelSize();
        return meta;
    }

    @Override
    public IIOMetadata convertStreamMetadata(IIOMetadata inData,
                                             ImageWriteParam param) {
        return null;
    }

    @Override
    public IIOMetadata convertImageMetadata(IIOMetadata metadata,
                                            ImageTypeSpecifier type,
                                            ImageWriteParam param) {
        return null;
    }

    @Override
    public boolean canWriteRasters() {
        return true;
    }

    @Override
    public void write(IIOMetadata streamMetadata,
                      IIOImage image,
                      ImageWriteParam param) throws IOException {

        if (stream == null) {
            throw new IllegalStateException(I18N.getString("BMPImageWriter7"));
        }

        if (image == null) {
            throw new IllegalArgumentException(I18N.getString("BMPImageWriter8"));
        }

        clearAbortRequest();
        processImageStarted(0);
        if (abortRequested()) {
            processWriteAborted();
            return;
        }
        if (param == null)
            param = getDefaultWriteParam();

        BMPImageWriteParam bmpParam = (BMPImageWriteParam)param;

        // Default is using 24 bits per pixel.
        int bitsPerPixel = 24;
        boolean isPalette = false;
        int paletteEntries = 0;
        IndexColorModel icm = null;

        RenderedImage input = null;
        Raster inputRaster = null;
        boolean writeRaster = image.hasRaster();
        Rectangle sourceRegion = param.getSourceRegion();
        SampleModel sampleModel = null;
        ColorModel colorModel = null;

        compImageSize = 0;

        if (writeRaster) {
            inputRaster = image.getRaster();
            sampleModel = inputRaster.getSampleModel();
            colorModel = ImageUtil.createColorModel(null, sampleModel);
            if (sourceRegion == null)
                sourceRegion = inputRaster.getBounds();
            else
                sourceRegion = sourceRegion.intersection(inputRaster.getBounds());
        } else {
            input = image.getRenderedImage();
            sampleModel = input.getSampleModel();
            colorModel = input.getColorModel();
            Rectangle rect = new Rectangle(input.getMinX(), input.getMinY(),
                                           input.getWidth(), input.getHeight());
            if (sourceRegion == null)
                sourceRegion = rect;
            else
                sourceRegion = sourceRegion.intersection(rect);
        }

        IIOMetadata imageMetadata = image.getMetadata();
        BMPMetadata bmpImageMetadata = null;
        if (imageMetadata != null
            && imageMetadata instanceof BMPMetadata)
        {
            bmpImageMetadata = (BMPMetadata)imageMetadata;
        } else {
            ImageTypeSpecifier imageType =
                new ImageTypeSpecifier(colorModel, sampleModel);

            bmpImageMetadata = (BMPMetadata)getDefaultImageMetadata(imageType,
                                                                    param);
        }

        if (sourceRegion.isEmpty())
            throw new RuntimeException(I18N.getString("BMPImageWrite0"));

        int scaleX = param.getSourceXSubsampling();
        int scaleY = param.getSourceYSubsampling();
        int xOffset = param.getSubsamplingXOffset();
        int yOffset = param.getSubsamplingYOffset();

        // cache the data type;
        int dataType = sampleModel.getDataType();

        sourceRegion.translate(xOffset, yOffset);
        sourceRegion.width -= xOffset;
        sourceRegion.height -= yOffset;

        int minX = sourceRegion.x / scaleX;
        int minY = sourceRegion.y / scaleY;
        w = (sourceRegion.width + scaleX - 1) / scaleX;
        h = (sourceRegion.height + scaleY - 1) / scaleY;
        xOffset = sourceRegion.x % scaleX;
        yOffset = sourceRegion.y % scaleY;

        Rectangle destinationRegion = new Rectangle(minX, minY, w, h);
        boolean noTransform = destinationRegion.equals(sourceRegion);

        // Raw data can only handle bytes, everything greater must be ASCII.
        int[] sourceBands = param.getSourceBands();
        boolean noSubband = true;
        int numBands = sampleModel.getNumBands();

        if (sourceBands != null) {
            sampleModel = sampleModel.createSubsetSampleModel(sourceBands);
            colorModel = null;
            noSubband = false;
            numBands = sampleModel.getNumBands();
        } else {
            sourceBands = new int[numBands];
            for (int i = 0; i < numBands; i++)
                sourceBands[i] = i;
        }

        int[] bandOffsets = null;
        boolean bgrOrder = true;

        if (sampleModel instanceof ComponentSampleModel) {
            bandOffsets = ((ComponentSampleModel)sampleModel).getBandOffsets();
            if (sampleModel instanceof BandedSampleModel) {
                // for images with BandedSampleModel we can not work
                //  with raster directly and must use writePixels()
                bgrOrder = false;
            } else {
                // we can work with raster directly only in case of
                // BGR component order.
                // In any other case we must use writePixels()
                for (int i = 0; i < bandOffsets.length; i++) {
                    bgrOrder &= (bandOffsets[i] == (bandOffsets.length - i - 1));
                }
            }
        } else {
            if (sampleModel instanceof SinglePixelPackedSampleModel) {

                // BugId 4892214: we can not work with raster directly
                // if image have different color order than RGB.
                // We should use writePixels() for such images.
                int[] bitOffsets = ((SinglePixelPackedSampleModel)sampleModel).getBitOffsets();
                for (int i=0; i<bitOffsets.length-1; i++) {
                    bgrOrder &= bitOffsets[i] > bitOffsets[i+1];
                }
            }
        }

        if (bandOffsets == null) {
            // we will use getPixels() to extract pixel data for writePixels()
            // Please note that getPixels() provides rgb bands order.
            bandOffsets = new int[numBands];
            for (int i = 0; i < numBands; i++)
                bandOffsets[i] = i;
        }

        noTransform &= bgrOrder;

        int[] sampleSize = sampleModel.getSampleSize();

        //XXX: check more

        // Number of bytes that a scanline for the image written out will have.
        int destScanlineBytes = w * numBands;

        switch(bmpParam.getCompressionMode()) {
        case ImageWriteParam.MODE_EXPLICIT:
            compressionType = BMPCompressionTypes.getType(bmpParam.getCompressionType());
            break;
        case ImageWriteParam.MODE_COPY_FROM_METADATA:
            compressionType = bmpImageMetadata.compression;
            break;
        case ImageWriteParam.MODE_DEFAULT:
            compressionType = getPreferredCompressionType(colorModel, sampleModel);
            break;
        default:
            // ImageWriteParam.MODE_DISABLED:
            compressionType = BI_RGB;
        }

        if (!canEncodeImage(compressionType, colorModel, sampleModel)) {
            throw new IOException("Image can not be encoded with compression type "
                                  + BMPCompressionTypes.getName(compressionType));
        }

        byte[] r = null, g = null, b = null, a = null;

        if (compressionType == BI_BITFIELDS) {
            bitsPerPixel =
                DataBuffer.getDataTypeSize(sampleModel.getDataType());

            if (bitsPerPixel != 16 && bitsPerPixel != 32) {
                // we should use 32bpp images in case of BI_BITFIELD
                // compression to avoid color conversion artefacts
                bitsPerPixel = 32;

                // Setting this flag to false ensures that generic
                // writePixels() will be used to store image data
                noTransform = false;
            }

            destScanlineBytes = w * bitsPerPixel + 7 >> 3;

            isPalette = true;
            paletteEntries = 3;
            r = new byte[paletteEntries];
            g = new byte[paletteEntries];
            b = new byte[paletteEntries];
            a = new byte[paletteEntries];

            int rmask = 0x00ff0000;
            int gmask = 0x0000ff00;
            int bmask = 0x000000ff;

            if (bitsPerPixel == 16) {
                /* NB: canEncodeImage() ensures we have image of
                 * either USHORT_565_RGB or USHORT_555_RGB type here.
                 * Technically, it should work for other direct color
                 * model types but it might be non compatible with win98
                 * and friends.
                 */
                if (colorModel instanceof DirectColorModel) {
                    DirectColorModel dcm = (DirectColorModel)colorModel;
                    rmask = dcm.getRedMask();
                    gmask = dcm.getGreenMask();
                    bmask = dcm.getBlueMask();
                } else {
                    // it is unlikely, but if it happens, we should throw
                    // an exception related to unsupported image format
                    throw new IOException("Image can not be encoded with " +
                                          "compression type " +
                                          BMPCompressionTypes.getName(compressionType));
                }
            }
            writeMaskToPalette(rmask, 0, r, g, b, a);
            writeMaskToPalette(gmask, 1, r, g, b, a);
            writeMaskToPalette(bmask, 2, r, g, b, a);

            if (!noTransform) {
                // prepare info for writePixels procedure
                bitMasks = new int[3];
                bitMasks[0] = rmask;
                bitMasks[1] = gmask;
                bitMasks[2] = bmask;

                bitPos = new int[3];
                bitPos[0] = firstLowBit(rmask);
                bitPos[1] = firstLowBit(gmask);
                bitPos[2] = firstLowBit(bmask);
            }

            if (colorModel instanceof IndexColorModel) {
                icm = (IndexColorModel)colorModel;
            }
        } else { // handle BI_RGB compression
            if (colorModel instanceof IndexColorModel) {
                isPalette = true;
                icm = (IndexColorModel)colorModel;
                paletteEntries = icm.getMapSize();

                if (paletteEntries <= 2) {
                    bitsPerPixel = 1;
                    destScanlineBytes = w + 7 >> 3;
                } else if (paletteEntries <= 16) {
                    bitsPerPixel = 4;
                    destScanlineBytes = w + 1 >> 1;
                } else if (paletteEntries <= 256) {
                    bitsPerPixel = 8;
                } else {
                    // Cannot be written as a Palette image. So write out as
                    // 24 bit image.
                    bitsPerPixel = 24;
                    isPalette = false;
                    paletteEntries = 0;
                    destScanlineBytes = w * 3;
                }

                if (isPalette == true) {
                    r = new byte[paletteEntries];
                    g = new byte[paletteEntries];
                    b = new byte[paletteEntries];
                    a = new byte[paletteEntries];

                    icm.getAlphas(a);
                    icm.getReds(r);
                    icm.getGreens(g);
                    icm.getBlues(b);
                }

            } else {
                // Grey scale images
                if (numBands == 1) {

                    isPalette = true;
                    paletteEntries = 256;
                    bitsPerPixel = sampleSize[0];

                    destScanlineBytes = (w * bitsPerPixel + 7 >> 3);

                    r = new byte[256];
                    g = new byte[256];
                    b = new byte[256];
                    a = new byte[256];

                    for (int i = 0; i < 256; i++) {
                        r[i] = (byte)i;
                        g[i] = (byte)i;
                        b[i] = (byte)i;
                        a[i] = (byte)255;
                    }

                } else {
                    if (sampleModel instanceof SinglePixelPackedSampleModel &&
                        noSubband)
                    {
                        /* NB: the actual pixel size can be smaller than
                         * size of used DataBuffer element.
                         * For example: in case of TYPE_INT_RGB actual pixel
                         * size is 24 bits, but size of DataBuffere element
                         * is 32 bits
                         */
                        int[] sample_sizes = sampleModel.getSampleSize();
                        bitsPerPixel = 0;
                        for (int size : sample_sizes) {
                            bitsPerPixel += size;
                        }
                        bitsPerPixel = roundBpp(bitsPerPixel);
                        if (bitsPerPixel != DataBuffer.getDataTypeSize(sampleModel.getDataType())) {
                            noTransform = false;
                        }
                        destScanlineBytes = w * bitsPerPixel + 7 >> 3;
                    }
                }
            }
        }

        // actual writing of image data
        int fileSize = 0;
        int offset = 0;
        int headerSize = 0;
        int imageSize = 0;
        int xPelsPerMeter = 0;
        int yPelsPerMeter = 0;
        int colorsUsed = 0;
        int colorsImportant = paletteEntries;

        // Calculate padding for each scanline
        int padding = destScanlineBytes % 4;
        if (padding != 0) {
            padding = 4 - padding;
        }


        // FileHeader is 14 bytes, BitmapHeader is 40 bytes,
        // add palette size and that is where the data will begin
        offset = 54 + paletteEntries * 4;

        imageSize = (destScanlineBytes + padding) * h;
        fileSize = imageSize + offset;
        headerSize = 40;

        long headPos = stream.getStreamPosition();

        writeFileHeader(fileSize, offset);

        /* According to MSDN description, the top-down image layout
         * is allowed only if compression type is BI_RGB or BI_BITFIELDS.
         * Images with any other compression type must be wrote in the
         * bottom-up layout.
         */
        if (compressionType == BI_RGB ||
            compressionType == BI_BITFIELDS)
        {
            isTopDown = bmpParam.isTopDown();
        } else {
            isTopDown = false;
        }

        writeInfoHeader(headerSize, bitsPerPixel);

        // compression
        stream.writeInt(compressionType);

        // imageSize
        stream.writeInt(imageSize);

        // xPelsPerMeter
        stream.writeInt(xPelsPerMeter);

        // yPelsPerMeter
        stream.writeInt(yPelsPerMeter);

        // Colors Used
        stream.writeInt(colorsUsed);

        // Colors Important
        stream.writeInt(colorsImportant);

        // palette
        if (isPalette == true) {

            // write palette
            if (compressionType == BI_BITFIELDS) {
                // write masks for red, green and blue components.
                for (int i=0; i<3; i++) {
                    int mask = (a[i]&0xFF) + ((r[i]&0xFF)*0x100) + ((g[i]&0xFF)*0x10000) + ((b[i]&0xFF)*0x1000000);
                    stream.writeInt(mask);
                }
            } else {
                for (int i=0; i<paletteEntries; i++) {
                    stream.writeByte(b[i]);
                    stream.writeByte(g[i]);
                    stream.writeByte(r[i]);
                    stream.writeByte(a[i]);
                }
            }
        }

        // Writing of actual image data
        int scanlineBytes = w * numBands;

        // Buffer for up to 8 rows of pixels
        int[] pixels = new int[scanlineBytes * scaleX];

        // Also create a buffer to hold one line of the data
        // to be written to the file, so we can use array writes.
        bpixels = new byte[destScanlineBytes];

        int l;

        if (compressionType == BI_JPEG ||
            compressionType == BI_PNG) {

            // prepare embedded buffer
            embedded_stream = new ByteArrayOutputStream();
            writeEmbedded(image, bmpParam);
            // update the file/image Size
            embedded_stream.flush();
            imageSize = embedded_stream.size();

            long endPos = stream.getStreamPosition();
            fileSize = offset + imageSize;
            stream.seek(headPos);
            writeSize(fileSize, 2);
            stream.seek(headPos);
            writeSize(imageSize, 34);
            stream.seek(endPos);
            stream.write(embedded_stream.toByteArray());
            embedded_stream = null;

            processImageComplete();
            stream.flushBefore(stream.getStreamPosition());

            return;
        }

        int maxBandOffset = bandOffsets[0];
        for (int i = 1; i < bandOffsets.length; i++)
            if (bandOffsets[i] > maxBandOffset)
                maxBandOffset = bandOffsets[i];

        int[] pixel = new int[maxBandOffset + 1];

        int destScanlineLength = destScanlineBytes;

        if (noTransform && noSubband) {
            destScanlineLength = destScanlineBytes / (DataBuffer.getDataTypeSize(dataType)>>3);
        }
        for (int i = 0; i < h; i++) {

            int row = minY + i;

            if (!isTopDown)
                row = minY + h - i -1;

            // Get the pixels
            Raster src = inputRaster;

            Rectangle srcRect =
                new Rectangle(minX * scaleX + xOffset,
                              row * scaleY + yOffset,
                              (w - 1)* scaleX + 1,
                              1);
            if (!writeRaster)
                src = input.getData(srcRect);

            if (noTransform && noSubband) {
                SampleModel sm = src.getSampleModel();
                int pos = 0;
                int startX = srcRect.x - src.getSampleModelTranslateX();
                int startY = srcRect.y - src.getSampleModelTranslateY();
                if (sm instanceof ComponentSampleModel) {
                    ComponentSampleModel csm = (ComponentSampleModel)sm;
                    pos = csm.getOffset(startX, startY, 0);
                    for(int nb=1; nb < csm.getNumBands(); nb++) {
                        if (pos > csm.getOffset(startX, startY, nb)) {
                            pos = csm.getOffset(startX, startY, nb);
                        }
                    }
                } else if (sm instanceof MultiPixelPackedSampleModel) {
                    MultiPixelPackedSampleModel mppsm =
                        (MultiPixelPackedSampleModel)sm;
                    pos = mppsm.getOffset(startX, startY);
                } else if (sm instanceof SinglePixelPackedSampleModel) {
                    SinglePixelPackedSampleModel sppsm =
                        (SinglePixelPackedSampleModel)sm;
                    pos = sppsm.getOffset(startX, startY);
                }

                if (compressionType == BI_RGB || compressionType == BI_BITFIELDS){
                    switch(dataType) {
                    case DataBuffer.TYPE_BYTE:
                        byte[] bdata =
                            ((DataBufferByte)src.getDataBuffer()).getData();
                        stream.write(bdata, pos, destScanlineLength);
                        break;

                    case DataBuffer.TYPE_SHORT:
                        short[] sdata =
                            ((DataBufferShort)src.getDataBuffer()).getData();
                        stream.writeShorts(sdata, pos, destScanlineLength);
                        break;

                    case DataBuffer.TYPE_USHORT:
                        short[] usdata =
                            ((DataBufferUShort)src.getDataBuffer()).getData();
                        stream.writeShorts(usdata, pos, destScanlineLength);
                        break;

                    case DataBuffer.TYPE_INT:
                        int[] idata =
                            ((DataBufferInt)src.getDataBuffer()).getData();
                        stream.writeInts(idata, pos, destScanlineLength);
                        break;
                    }

                    for(int k=0; k<padding; k++) {
                        stream.writeByte(0);
                    }
                } else if (compressionType == BI_RLE4) {
                    if (bpixels == null || bpixels.length < scanlineBytes)
                        bpixels = new byte[scanlineBytes];
                    src.getPixels(srcRect.x, srcRect.y,
                                  srcRect.width, srcRect.height, pixels);
                    for (int h=0; h<scanlineBytes; h++) {
                        bpixels[h] = (byte)pixels[h];
                    }
                    encodeRLE4(bpixels, scanlineBytes);
                } else if (compressionType == BI_RLE8) {
                    //byte[] bdata =
                    //    ((DataBufferByte)src.getDataBuffer()).getData();
                    //System.out.println("bdata.length="+bdata.length);
                    //System.arraycopy(bdata, pos, bpixels, 0, scanlineBytes);
                    if (bpixels == null || bpixels.length < scanlineBytes)
                        bpixels = new byte[scanlineBytes];
                    src.getPixels(srcRect.x, srcRect.y,
                                  srcRect.width, srcRect.height, pixels);
                    for (int h=0; h<scanlineBytes; h++) {
                        bpixels[h] = (byte)pixels[h];
                    }

                    encodeRLE8(bpixels, scanlineBytes);
                }
            } else {
                src.getPixels(srcRect.x, srcRect.y,
                              srcRect.width, srcRect.height, pixels);

                if (scaleX != 1 || maxBandOffset != numBands - 1) {
                    for (int j = 0, k = 0, n=0; j < w;
                         j++, k += scaleX * numBands, n += numBands)
                    {
                        System.arraycopy(pixels, k, pixel, 0, pixel.length);

                        for (int m = 0; m < numBands; m++) {
                            // pixel data is provided here in RGB order
                            pixels[n + m] = pixel[sourceBands[m]];
                        }
                    }
                }
                writePixels(0, scanlineBytes, bitsPerPixel, pixels,
                            padding, numBands, icm);
            }

            processImageProgress(100.0f * (((float)i) / ((float)h)));
            if (abortRequested()) {
                break;
            }
        }

        if (compressionType == BI_RLE4 ||
            compressionType == BI_RLE8) {
            // Write the RLE EOF marker and
            stream.writeByte(0);
            stream.writeByte(1);
            incCompImageSize(2);
            // update the file/image Size
            imageSize = compImageSize;
            fileSize = compImageSize + offset;
            long endPos = stream.getStreamPosition();
            stream.seek(headPos);
            writeSize(fileSize, 2);
            stream.seek(headPos);
            writeSize(imageSize, 34);
            stream.seek(endPos);
        }

        if (abortRequested()) {
            processWriteAborted();
        } else {
            processImageComplete();
            stream.flushBefore(stream.getStreamPosition());
        }
    }

    private void writePixels(int l, int scanlineBytes, int bitsPerPixel,
                             int[] pixels,
                             int padding, int numBands,
                             IndexColorModel icm) throws IOException {
        int pixel = 0;
        int k = 0;
        switch (bitsPerPixel) {

        case 1:

            for (int j=0; j<scanlineBytes/8; j++) {
                bpixels[k++] = (byte)((pixels[l++]  << 7) |
                                      (pixels[l++]  << 6) |
                                      (pixels[l++]  << 5) |
                                      (pixels[l++]  << 4) |
                                      (pixels[l++]  << 3) |
                                      (pixels[l++]  << 2) |
                                      (pixels[l++]  << 1) |
                                      pixels[l++]);
            }

            // Partially filled last byte, if any
            if (scanlineBytes%8 > 0) {
                pixel = 0;
                for (int j=0; j<scanlineBytes%8; j++) {
                    pixel |= (pixels[l++] << (7 - j));
                }
                bpixels[k++] = (byte)pixel;
            }
            stream.write(bpixels, 0, (scanlineBytes+7)/8);

            break;

        case 4:
            if (compressionType == BI_RLE4){
                byte[] bipixels = new byte[scanlineBytes];
                for (int h=0; h<scanlineBytes; h++) {
                    bipixels[h] = (byte)pixels[l++];
                }
                encodeRLE4(bipixels, scanlineBytes);
            }else {
                for (int j=0; j<scanlineBytes/2; j++) {
                    pixel = (pixels[l++] << 4) | pixels[l++];
                    bpixels[k++] = (byte)pixel;
                }
                // Put the last pixel of odd-length lines in the 4 MSBs
                if ((scanlineBytes%2) == 1) {
                    pixel = pixels[l] << 4;
                    bpixels[k++] = (byte)pixel;
                }
                stream.write(bpixels, 0, (scanlineBytes+1)/2);
            }
            break;

        case 8:
            if(compressionType == BI_RLE8) {
                for (int h=0; h<scanlineBytes; h++) {
                    bpixels[h] = (byte)pixels[l++];
                }
                encodeRLE8(bpixels, scanlineBytes);
            }else {
                for (int j=0; j<scanlineBytes; j++) {
                    bpixels[j] = (byte)pixels[l++];
                }
                stream.write(bpixels, 0, scanlineBytes);
            }
            break;

        case 16:
            if (spixels == null)
                spixels = new short[scanlineBytes / numBands];
            /*
             * We expect that pixel data comes in RGB order.
             * We will assemble short pixel taking into account
             * the compression type:
             *
             * BI_RGB        - the RGB order should be maintained.
             * BI_BITFIELDS  - use bitPos array that was built
             *                 according to bitfields masks.
             */
            for (int j = 0, m = 0; j < scanlineBytes; m++) {
                spixels[m] = 0;
                if (compressionType == BI_RGB) {
                    /*
                     * please note that despite other cases,
                     * the 16bpp BI_RGB requires the RGB data order
                     */
                    spixels[m] = (short)
                        (((0x1f & pixels[j    ]) << 10) |
                         ((0x1f & pixels[j + 1]) <<  5) |
                         ((0x1f & pixels[j + 2])      ));
                     j += 3;
                } else {
                    for(int i = 0 ; i < numBands; i++, j++) {
                        spixels[m] |=
                            (((pixels[j]) << bitPos[i]) & bitMasks[i]);
                    }
                }
            }
            stream.writeShorts(spixels, 0, spixels.length);
            break;

        case 24:
            if (numBands == 3) {
                for (int j=0; j<scanlineBytes; j+=3) {
                    // Since BMP needs BGR format
                    bpixels[k++] = (byte)(pixels[l+2]);
                    bpixels[k++] = (byte)(pixels[l+1]);
                    bpixels[k++] = (byte)(pixels[l]);
                    l+=3;
                }
                stream.write(bpixels, 0, scanlineBytes);
            } else {
                // Case where IndexColorModel had > 256 colors.
                int entries = icm.getMapSize();

                byte[] r = new byte[entries];
                byte[] g = new byte[entries];
                byte[] b = new byte[entries];

                icm.getReds(r);
                icm.getGreens(g);
                icm.getBlues(b);
                int index;

                for (int j=0; j<scanlineBytes; j++) {
                    index = pixels[l];
                    bpixels[k++] = b[index];
                    bpixels[k++] = g[index];
                    bpixels[k++] = b[index];
                    l++;
                }
                stream.write(bpixels, 0, scanlineBytes*3);
            }
            break;

        case 32:
            if (ipixels == null)
                ipixels = new int[scanlineBytes / numBands];
            if (numBands == 3) {
                /*
                 * We expect that pixel data comes in RGB order.
                 * We will assemble int pixel taking into account
                 * the compression type.
                 *
                 * BI_RGB        - the BGR order should be used.
                 * BI_BITFIELDS  - use bitPos array that was built
                 *                 according to bitfields masks.
                 */
                for (int j = 0, m = 0; j < scanlineBytes; m++) {
                    ipixels[m] = 0;
                    if (compressionType == BI_RGB) {
                        ipixels[m] =
                            ((0xff & pixels[j + 2]) << 16) |
                            ((0xff & pixels[j + 1]) <<  8) |
                            ((0xff & pixels[j    ])      );
                        j += 3;
                    } else {
                        for(int i = 0 ; i < numBands; i++, j++) {
                            ipixels[m] |=
                                (((pixels[j]) << bitPos[i]) & bitMasks[i]);
                        }
                    }
                }
            } else {
                // We have two possibilities here:
                // 1. we are writing the indexed image with bitfields
                //    compression (this covers also the case of BYTE_BINARY)
                //    => use icm to get actual RGB color values.
                // 2. we are writing the gray-scaled image with BI_BITFIELDS
                //    compression
                //    => just replicate the level of gray to color components.
                for (int j = 0; j < scanlineBytes; j++) {
                    if (icm != null) {
                        ipixels[j] = icm.getRGB(pixels[j]);
                    } else {
                        ipixels[j] =
                            pixels[j] << 16 | pixels[j] << 8 | pixels[j];
                    }
                }
            }
            stream.writeInts(ipixels, 0, ipixels.length);
            break;
        }

        // Write out the padding
        if (compressionType == BI_RGB ||
            compressionType == BI_BITFIELDS)
        {
            for(k=0; k<padding; k++) {
                stream.writeByte(0);
            }
        }
    }

    private void encodeRLE8(byte[] bpixels, int scanlineBytes)
      throws IOException{

        int runCount = 1, absVal = -1, j = -1;
        byte runVal = 0, nextVal =0 ;

        runVal = bpixels[++j];
        byte[] absBuf = new byte[256];

        while (j < scanlineBytes-1) {
            nextVal = bpixels[++j];
            if (nextVal == runVal ){
                if(absVal >= 3 ){
                    /// Check if there was an existing Absolute Run
                    stream.writeByte(0);
                    stream.writeByte(absVal);
                    incCompImageSize(2);
                    for(int a=0; a<absVal;a++){
                        stream.writeByte(absBuf[a]);
                        incCompImageSize(1);
                    }
                    if (!isEven(absVal)){
                        //Padding
                        stream.writeByte(0);
                        incCompImageSize(1);
                    }
                }
                else if(absVal > -1){
                    /// Absolute Encoding for less than 3
                    /// treated as regular encoding
                    /// Do not include the last element since it will
                    /// be inclued in the next encoding/run
                    for (int b=0;b<absVal;b++){
                        stream.writeByte(1);
                        stream.writeByte(absBuf[b]);
                        incCompImageSize(2);
                    }
                }
                absVal = -1;
                runCount++;
                if (runCount == 256){
                    /// Only 255 values permitted
                    stream.writeByte(runCount-1);
                    stream.writeByte(runVal);
                    incCompImageSize(2);
                    runCount = 1;
                }
            }
            else {
                if (runCount > 1){
                    /// If there was an existing run
                    stream.writeByte(runCount);
                    stream.writeByte(runVal);
                    incCompImageSize(2);
                } else if (absVal < 0){
                    // First time..
                    absBuf[++absVal] = runVal;
                    absBuf[++absVal] = nextVal;
                } else if (absVal < 254){
                    //  0-254 only
                    absBuf[++absVal] = nextVal;
                } else {
                    stream.writeByte(0);
                    stream.writeByte(absVal+1);
                    incCompImageSize(2);
                    for(int a=0; a<=absVal;a++){
                        stream.writeByte(absBuf[a]);
                        incCompImageSize(1);
                    }
                    // padding since 255 elts is not even
                    stream.writeByte(0);
                    incCompImageSize(1);
                    absVal = -1;
                }
                runVal = nextVal;
                runCount = 1;
            }

            if (j == scanlineBytes-1){ // EOF scanline
                // Write the run
                if (absVal == -1){
                    stream.writeByte(runCount);
                    stream.writeByte(runVal);
                    incCompImageSize(2);
                    runCount = 1;
                }
                else {
                    // write the Absolute Run
                    if(absVal >= 2){
                        stream.writeByte(0);
                        stream.writeByte(absVal+1);
                        incCompImageSize(2);
                        for(int a=0; a<=absVal;a++){
                            stream.writeByte(absBuf[a]);
                            incCompImageSize(1);
                        }
                        if (!isEven(absVal+1)){
                            //Padding
                            stream.writeByte(0);
                            incCompImageSize(1);
                        }

                    }
                    else if(absVal > -1){
                        for (int b=0;b<=absVal;b++){
                            stream.writeByte(1);
                            stream.writeByte(absBuf[b]);
                            incCompImageSize(2);
                        }
                    }
                }
                /// EOF scanline

                stream.writeByte(0);
                stream.writeByte(0);
                incCompImageSize(2);
            }
        }
    }

    private void encodeRLE4(byte[] bipixels, int scanlineBytes)
      throws IOException {

        int runCount=2, absVal=-1, j=-1, pixel=0, q=0;
        byte runVal1=0, runVal2=0, nextVal1=0, nextVal2=0;
        byte[] absBuf = new byte[256];


        runVal1 = bipixels[++j];
        runVal2 = bipixels[++j];

        while (j < scanlineBytes-2){
            nextVal1 = bipixels[++j];
            nextVal2 = bipixels[++j];

            if (nextVal1 == runVal1 ) {

                //Check if there was an existing Absolute Run
                if(absVal >= 4){
                    stream.writeByte(0);
                    stream.writeByte(absVal - 1);
                    incCompImageSize(2);
                    // we need to exclude  last 2 elts, similarity of
                    // which caused to enter this part of the code
                    for(int a=0; a<absVal-2;a+=2){
                        pixel = (absBuf[a] << 4) | absBuf[a+1];
                        stream.writeByte((byte)pixel);
                        incCompImageSize(1);
                    }
                    // if # of elts is odd - read the last element
                    if(!(isEven(absVal-1))){
                        q = absBuf[absVal-2] << 4| 0;
                        stream.writeByte(q);
                        incCompImageSize(1);
                    }
                    // Padding to word align absolute encoding
                    if ( !isEven((int)Math.ceil((absVal-1)/2)) ) {
                        stream.writeByte(0);
                        incCompImageSize(1);
                    }
                } else if (absVal > -1){
                    stream.writeByte(2);
                    pixel = (absBuf[0] << 4) | absBuf[1];
                    stream.writeByte(pixel);
                    incCompImageSize(2);
                }
                absVal = -1;

                if (nextVal2 == runVal2){
                    // Even runlength
                    runCount+=2;
                    if(runCount == 256){
                        stream.writeByte(runCount-1);
                        pixel = ( runVal1 << 4) | runVal2;
                        stream.writeByte(pixel);
                        incCompImageSize(2);
                        runCount =2;
                        if(j< scanlineBytes - 1){
                            runVal1 = runVal2;
                            runVal2 = bipixels[++j];
                        } else {
                            stream.writeByte(01);
                            int r = runVal2 << 4 | 0;
                            stream.writeByte(r);
                            incCompImageSize(2);
                            runCount = -1;/// Only EOF required now
                        }
                    }
                } else {
                    // odd runlength and the run ends here
                    // runCount wont be > 254 since 256/255 case will
                    // be taken care of in above code.
                    runCount++;
                    pixel = ( runVal1 << 4) | runVal2;
                    stream.writeByte(runCount);
                    stream.writeByte(pixel);
                    incCompImageSize(2);
                    runCount = 2;
                    runVal1 = nextVal2;
                    // If end of scanline
                    if (j < scanlineBytes -1){
                        runVal2 = bipixels[++j];
                    }else {
                        stream.writeByte(01);
                        int r = nextVal2 << 4 | 0;
                        stream.writeByte(r);
                        incCompImageSize(2);
                        runCount = -1;/// Only EOF required now
                    }

                }
            } else{
                // Check for existing run
                if (runCount > 2){
                    pixel = ( runVal1 << 4) | runVal2;
                    stream.writeByte(runCount);
                    stream.writeByte(pixel);
                    incCompImageSize(2);
                } else if (absVal < 0){ // first time
                    absBuf[++absVal] = runVal1;
                    absBuf[++absVal] = runVal2;
                    absBuf[++absVal] = nextVal1;
                    absBuf[++absVal] = nextVal2;
                } else if (absVal < 253){ // only 255 elements
                    absBuf[++absVal] = nextVal1;
                    absBuf[++absVal] = nextVal2;
                } else {
                    stream.writeByte(0);
                    stream.writeByte(absVal+1);
                    incCompImageSize(2);
                    for(int a=0; a<absVal;a+=2){
                        pixel = (absBuf[a] << 4) | absBuf[a+1];
                        stream.writeByte((byte)pixel);
                        incCompImageSize(1);
                    }
                    // Padding for word align
                    // since it will fit into 127 bytes
                    stream.writeByte(0);
                    incCompImageSize(1);
                    absVal = -1;
                }

                runVal1 = nextVal1;
                runVal2 = nextVal2;
                runCount = 2;
            }
            // Handle the End of scanline for the last 2 4bits
            if (j >= scanlineBytes-2 ) {
                if (absVal == -1 && runCount >= 2){
                    if (j == scanlineBytes-2){
                        if(bipixels[++j] == runVal1){
                            runCount++;
                            pixel = ( runVal1 << 4) | runVal2;
                            stream.writeByte(runCount);
                            stream.writeByte(pixel);
                            incCompImageSize(2);
                        } else {
                            pixel = ( runVal1 << 4) | runVal2;
                            stream.writeByte(runCount);
                            stream.writeByte(pixel);
                            stream.writeByte(01);
                            pixel =  bipixels[j]<<4 |0;
                            stream.writeByte(pixel);
                            int n = bipixels[j]<<4|0;
                            incCompImageSize(4);
                        }
                    } else {
                        stream.writeByte(runCount);
                        pixel =( runVal1 << 4) | runVal2 ;
                        stream.writeByte(pixel);
                        incCompImageSize(2);
                    }
                } else if(absVal > -1){
                    if (j == scanlineBytes-2){
                        absBuf[++absVal] = bipixels[++j];
                    }
                    if (absVal >=2){
                        stream.writeByte(0);
                        stream.writeByte(absVal+1);
                        incCompImageSize(2);
                        for(int a=0; a<absVal;a+=2){
                            pixel = (absBuf[a] << 4) | absBuf[a+1];
                            stream.writeByte((byte)pixel);
                            incCompImageSize(1);
                        }
                        if(!(isEven(absVal+1))){
                            q = absBuf[absVal] << 4|0;
                            stream.writeByte(q);
                            incCompImageSize(1);
                        }

                        // Padding
                        if ( !isEven((int)Math.ceil((absVal+1)/2)) ) {
                            stream.writeByte(0);
                            incCompImageSize(1);
                        }

                    } else {
                        switch (absVal){
                        case 0:
                            stream.writeByte(1);
                            int n = absBuf[0]<<4 | 0;
                            stream.writeByte(n);
                            incCompImageSize(2);
                            break;
                        case 1:
                            stream.writeByte(2);
                            pixel = (absBuf[0] << 4) | absBuf[1];
                            stream.writeByte(pixel);
                            incCompImageSize(2);
                            break;
                        }
                    }

                }
                stream.writeByte(0);
                stream.writeByte(0);
                incCompImageSize(2);
            }
        }
    }


    private synchronized void incCompImageSize(int value){
        compImageSize = compImageSize + value;
    }

    private boolean isEven(int number) {
        return (number%2 == 0 ? true : false);
    }

    private void writeFileHeader(int fileSize, int offset) throws IOException {
        // magic value
        stream.writeByte('B');
        stream.writeByte('M');

        // File size
        stream.writeInt(fileSize);

        // reserved1 and reserved2
        stream.writeInt(0);

        // offset to image data
        stream.writeInt(offset);
    }


    private void writeInfoHeader(int headerSize,
                                 int bitsPerPixel) throws IOException {
        // size of header
        stream.writeInt(headerSize);

        // width
        stream.writeInt(w);

        // height
        stream.writeInt(isTopDown ? -h : h);

        // number of planes
        stream.writeShort(1);

        // Bits Per Pixel
        stream.writeShort(bitsPerPixel);
    }

    private void writeSize(int dword, int offset) throws IOException {
        stream.skipBytes(offset);
        stream.writeInt(dword);
    }

    @Override
    public void reset() {
        super.reset();
        stream = null;
    }

    private void writeEmbedded(IIOImage image,
                               ImageWriteParam bmpParam) throws IOException {
        String format =
            compressionType == BI_JPEG ? "jpeg" : "png";
        Iterator<ImageWriter> iterator =
               ImageIO.getImageWritersByFormatName(format);
        ImageWriter writer = null;
        if (iterator.hasNext())
            writer = iterator.next();
        if (writer != null) {
            if (embedded_stream == null) {
                throw new RuntimeException("No stream for writing embedded image!");
            }

            writer.addIIOWriteProgressListener(new IIOWriteProgressAdapter() {
                    @Override
                    public void imageProgress(ImageWriter source, float percentageDone) {
                        processImageProgress(percentageDone);
                    }
                });

            writer.addIIOWriteWarningListener(new IIOWriteWarningListener() {
                    @Override
                    public void warningOccurred(ImageWriter source, int imageIndex, String warning) {
                        processWarningOccurred(imageIndex, warning);
                    }
                });

            writer.setOutput(ImageIO.createImageOutputStream(embedded_stream));
            ImageWriteParam param = writer.getDefaultWriteParam();
            //param.setDestinationBands(bmpParam.getDestinationBands());
            param.setDestinationOffset(bmpParam.getDestinationOffset());
            param.setSourceBands(bmpParam.getSourceBands());
            param.setSourceRegion(bmpParam.getSourceRegion());
            param.setSourceSubsampling(bmpParam.getSourceXSubsampling(),
                                       bmpParam.getSourceYSubsampling(),
                                       bmpParam.getSubsamplingXOffset(),
                                       bmpParam.getSubsamplingYOffset());
            writer.write(null, image, param);
        } else
            throw new RuntimeException(I18N.getString("BMPImageWrite5") + " " + format);

    }

    private int firstLowBit(int num) {
        int count = 0;
        while ((num & 1) == 0) {
            count++;
            num >>>= 1;
        }
        return count;
    }

    private static class IIOWriteProgressAdapter implements IIOWriteProgressListener {

        @Override
        public void imageComplete(ImageWriter source) {
        }

        @Override
        public void imageProgress(ImageWriter source, float percentageDone) {
        }

        @Override
        public void imageStarted(ImageWriter source, int imageIndex) {
        }

        @Override
        public void thumbnailComplete(ImageWriter source) {
        }

        @Override
        public void thumbnailProgress(ImageWriter source, float percentageDone) {
        }

        @Override
        public void thumbnailStarted(ImageWriter source, int imageIndex, int thumbnailIndex) {
        }

        @Override
        public void writeAborted(ImageWriter source) {
        }
    }

    /*
     * Returns preferred compression type for given image.
     * The default compression type is BI_RGB, but some image types can't be
     * encodeed with using default compression without cahnge color resolution.
     * For example, TYPE_USHORT_565_RGB may be encodeed only by using BI_BITFIELDS
     * compression type.
     *
     * NB: we probably need to extend this method if we encounter other image
     * types which can not be encoded with BI_RGB compression type.
     */
    protected int getPreferredCompressionType(ColorModel cm, SampleModel sm) {
        ImageTypeSpecifier imageType = new ImageTypeSpecifier(cm, sm);
        return getPreferredCompressionType(imageType);
    }

    protected int getPreferredCompressionType(ImageTypeSpecifier imageType) {
        if (imageType.getBufferedImageType() == BufferedImage.TYPE_USHORT_565_RGB) {
            return  BI_BITFIELDS;
        }
        return BI_RGB;
    }

    /*
     * Check whether we can encode image of given type using compression method in question.
     *
     * For example, TYPE_USHORT_565_RGB can be encodeed with BI_BITFIELDS compression only.
     *
     * NB: method should be extended if other cases when we can not encode
     *     with given compression will be discovered.
     */
    protected boolean canEncodeImage(int compression, ColorModel cm, SampleModel sm) {
        ImageTypeSpecifier imgType = new ImageTypeSpecifier(cm, sm);
        return canEncodeImage(compression, imgType);
    }

    protected boolean canEncodeImage(int compression, ImageTypeSpecifier imgType) {
        ImageWriterSpi spi = this.getOriginatingProvider();
        if (!spi.canEncodeImage(imgType)) {
            return false;
        }
        int biType = imgType.getBufferedImageType();
        int bpp = imgType.getColorModel().getPixelSize();
        if (compressionType == BI_RLE4 && bpp != 4) {
            // only 4bpp images can be encoded as BI_RLE4
            return false;
        }
        if (compressionType == BI_RLE8 && bpp != 8) {
            // only 8bpp images can be encoded as BI_RLE8
            return false;
        }
        if (bpp == 16) {
            /*
             * Technically we expect that we may be able to
             * encode only some of SinglePixelPackedSampleModel
             * images here.
             *
             * In addition we should take into account following:
             *
             * 1. BI_RGB case, according to the MSDN description:
             *
             *     The bitmap has a maximum of 2^16 colors. If the
             *     biCompression member of the BITMAPINFOHEADER is BI_RGB,
             *     the bmiColors member of BITMAPINFO is NULL. Each WORD
             *     in the bitmap array represents a single pixel. The
             *     relative intensities of red, green, and blue are
             *     represented with five bits for each color component.
             *
             * 2. BI_BITFIELDS case, according ot the MSDN description:
             *
             *     Windows 95/98/Me: When the biCompression member is
             *     BI_BITFIELDS, the system supports only the following
             *     16bpp color masks: A 5-5-5 16-bit image, where the blue
             *     mask is 0x001F, the green mask is 0x03E0, and the red mask
             *     is 0x7C00; and a 5-6-5 16-bit image, where the blue mask
             *     is 0x001F, the green mask is 0x07E0, and the red mask is
             *     0xF800.
             */
            boolean canUseRGB = false;
            boolean canUseBITFIELDS = false;

            SampleModel sm = imgType.getSampleModel();
            if (sm instanceof SinglePixelPackedSampleModel) {
                int[] sizes =
                    ((SinglePixelPackedSampleModel)sm).getSampleSize();

                canUseRGB = true;
                canUseBITFIELDS = true;
                for (int i = 0; i < sizes.length; i++) {
                    canUseRGB       &=  (sizes[i] == 5);
                    canUseBITFIELDS &= ((sizes[i] == 5) ||
                                        (i == 1 && sizes[i] == 6));
                }
            }

            return (((compressionType == BI_RGB) && canUseRGB) ||
                    ((compressionType == BI_BITFIELDS) && canUseBITFIELDS));
        }
        return true;
    }

    protected void writeMaskToPalette(int mask, int i,
                                      byte[] r, byte[]g, byte[] b, byte[]a) {
        b[i] = (byte)(0xff & (mask >> 24));
        g[i] = (byte)(0xff & (mask >> 16));
        r[i] = (byte)(0xff & (mask >> 8));
        a[i] = (byte)(0xff & mask);
    }

    private int roundBpp(int x) {
        if (x <= 8) {
            return 8;
        } else if (x <= 16) {
            return 16;
        } if (x <= 24) {
            return 24;
        } else {
            return 32;
        }
    }
}
