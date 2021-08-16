/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.jpeg;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormatImpl;
import javax.imageio.plugins.jpeg.JPEGHuffmanTable;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.plugins.jpeg.JPEGQTable;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

import com.sun.imageio.plugins.common.ImageUtil;
import org.w3c.dom.Node;
import sun.java2d.Disposer;
import sun.java2d.DisposerRecord;

public class JPEGImageWriter extends ImageWriter {

    ///////// Private variables

    private boolean debug = false;

    /**
     * The following variable contains a pointer to the IJG library
     * structure for this reader.  It is assigned in the constructor
     * and then is passed in to every native call.  It is set to 0
     * by dispose to avoid disposing twice.
     */
    private long structPointer = 0;


    /** The output stream we write to */
    private ImageOutputStream ios = null;

    /** The Raster we will write from */
    private Raster srcRas = null;

    /** An intermediate Raster holding compressor-friendly data */
    private WritableRaster raster = null;

    /**
     * Set to true if we are writing an image with an
     * indexed ColorModel
     */
    private boolean indexed = false;
    private IndexColorModel indexCM = null;

    private boolean convertTosRGB = false;  // Used by PhotoYCC only
    private WritableRaster converted = null;

    private boolean isAlphaPremultiplied = false;
    private ColorModel srcCM = null;

    /**
     * If there are thumbnails to be written, this is the list.
     */
    private List<? extends BufferedImage> thumbnails = null;

    /**
     * If metadata should include an icc profile, store it here.
     */
    private ICC_Profile iccProfile = null;

    private int sourceXOffset = 0;
    private int sourceYOffset = 0;
    private int sourceWidth = 0;
    private int [] srcBands = null;
    private int sourceHeight = 0;

    /** Used when calling listeners */
    private int currentImage = 0;

    private ColorConvertOp convertOp = null;

    private JPEGQTable [] streamQTables = null;
    private JPEGHuffmanTable[] streamDCHuffmanTables = null;
    private JPEGHuffmanTable[] streamACHuffmanTables = null;

    // Parameters for writing metadata
    private boolean ignoreJFIF = false;  // If it's there, use it
    private boolean forceJFIF = false;  // Add one for the thumbnails
    private boolean ignoreAdobe = false;  // If it's there, use it
    private int newAdobeTransform = JPEG.ADOBE_IMPOSSIBLE;  // Change if needed
    private boolean writeDefaultJFIF = false;
    private boolean writeAdobe = false;
    private JPEGMetadata metadata = null;

    private boolean sequencePrepared = false;

    private int numScans = 0;

    /** The referent to be registered with the Disposer. */
    private Object disposerReferent = new Object();

    /** The DisposerRecord that handles the actual disposal of this writer. */
    private DisposerRecord disposerRecord;

    ///////// End of Private variables

    ///////// Protected variables

    protected static final int WARNING_DEST_IGNORED = 0;
    protected static final int WARNING_STREAM_METADATA_IGNORED = 1;
    protected static final int WARNING_DEST_METADATA_COMP_MISMATCH = 2;
    protected static final int WARNING_DEST_METADATA_JFIF_MISMATCH = 3;
    protected static final int WARNING_DEST_METADATA_ADOBE_MISMATCH = 4;
    protected static final int WARNING_IMAGE_METADATA_JFIF_MISMATCH = 5;
    protected static final int WARNING_IMAGE_METADATA_ADOBE_MISMATCH = 6;
    protected static final int WARNING_METADATA_NOT_JPEG_FOR_RASTER = 7;
    protected static final int WARNING_NO_BANDS_ON_INDEXED = 8;
    protected static final int WARNING_ILLEGAL_THUMBNAIL = 9;
    protected static final int WARNING_IGNORING_THUMBS = 10;
    protected static final int WARNING_FORCING_JFIF = 11;
    protected static final int WARNING_THUMB_CLIPPED = 12;
    protected static final int WARNING_METADATA_ADJUSTED_FOR_THUMB = 13;
    protected static final int WARNING_NO_RGB_THUMB_AS_INDEXED = 14;
    protected static final int WARNING_NO_GRAY_THUMB_AS_INDEXED = 15;

    private static final int MAX_WARNING = WARNING_NO_GRAY_THUMB_AS_INDEXED;

    ///////// End of Protected variables

    ///////// static initializer

    static {
        initStatic();
    }

    @SuppressWarnings("removal")
    private static void initStatic() {
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    System.loadLibrary("javajpeg");
                    return null;
                }
            });
        initWriterIDs(JPEGQTable.class,
                      JPEGHuffmanTable.class);
    }

    //////// Public API

    public JPEGImageWriter(ImageWriterSpi originator) {
        super(originator);
        structPointer = initJPEGImageWriter();
        disposerRecord = new JPEGWriterDisposerRecord(structPointer);
        Disposer.addRecord(disposerReferent, disposerRecord);
    }

    @Override
    public void setOutput(Object output) {
        setThreadLock();
        try {
            cbLock.check();

            super.setOutput(output); // validates output
            resetInternalState();
            ios = (ImageOutputStream) output; // so this will always work
            // Set the native destination
            setDest(structPointer);
        } finally {
            clearThreadLock();
        }
    }

    @Override
    public ImageWriteParam getDefaultWriteParam() {
        return new JPEGImageWriteParam(null);
    }

    @Override
    public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
        setThreadLock();
        try {
            return new JPEGMetadata(param, this);
        } finally {
            clearThreadLock();
        }
    }

    @Override
    public IIOMetadata
        getDefaultImageMetadata(ImageTypeSpecifier imageType,
                                ImageWriteParam param) {
        setThreadLock();
        try {
            return new JPEGMetadata(imageType, param, this);
        } finally {
            clearThreadLock();
        }
    }

    @Override
    public IIOMetadata convertStreamMetadata(IIOMetadata inData,
                                             ImageWriteParam param) {
        // There isn't much we can do.  If it's one of ours, then
        // return it.  Otherwise just return null.  We use it only
        // for tables, so we can't get a default and modify it,
        // as this will usually not be what is intended.
        if (inData instanceof JPEGMetadata) {
            JPEGMetadata jpegData = (JPEGMetadata) inData;
            if (jpegData.isStream) {
                return inData;
            }
        }
        return null;
    }

    @Override
    public IIOMetadata
        convertImageMetadata(IIOMetadata inData,
                             ImageTypeSpecifier imageType,
                             ImageWriteParam param) {
        setThreadLock();
        try {
            return convertImageMetadataOnThread(inData, imageType, param);
        } finally {
            clearThreadLock();
        }
    }

    private IIOMetadata
        convertImageMetadataOnThread(IIOMetadata inData,
                                     ImageTypeSpecifier imageType,
                                     ImageWriteParam param) {
        // If it's one of ours, just return it
        if (inData instanceof JPEGMetadata) {
            JPEGMetadata jpegData = (JPEGMetadata) inData;
            if (!jpegData.isStream) {
                return inData;
            } else {
                // Can't convert stream metadata to image metadata
                // XXX Maybe this should put out a warning?
                return null;
            }
        }
        // If it's not one of ours, create a default and set it from
        // the standard tree from the input, if it exists.
        if (inData.isStandardMetadataFormatSupported()) {
            String formatName =
                IIOMetadataFormatImpl.standardMetadataFormatName;
            Node tree = inData.getAsTree(formatName);
            if (tree != null) {
                JPEGMetadata jpegData = new JPEGMetadata(imageType,
                                                         param,
                                                         this);
                try {
                    jpegData.setFromTree(formatName, tree);
                } catch (IIOInvalidTreeException e) {
                    // Other plug-in generates bogus standard tree
                    // XXX Maybe this should put out a warning?
                    return null;
                }

                return jpegData;
            }
        }
        return null;
    }

    @Override
    public int getNumThumbnailsSupported(ImageTypeSpecifier imageType,
                                         ImageWriteParam param,
                                         IIOMetadata streamMetadata,
                                         IIOMetadata imageMetadata) {
        // Check whether sufficient data is available.
        if (imageType == null && imageMetadata == null) {
            // The method has been invoked with insufficient data. Henceforth
            // we return -1 as recommended by ImageWriter specification.
            return -1;
        }

        // Check if the image type and metadata are JFIF compatible.
        if (jfifOK(imageType, param, streamMetadata, imageMetadata)) {
            return Integer.MAX_VALUE;
        }
        return 0;
    }

    static final Dimension [] preferredThumbSizes = {new Dimension(1, 1),
                                                     new Dimension(255, 255)};
    @Override
    public Dimension[] getPreferredThumbnailSizes(ImageTypeSpecifier imageType,
                                                  ImageWriteParam param,
                                                  IIOMetadata streamMetadata,
                                                  IIOMetadata imageMetadata) {
        if (jfifOK(imageType, param, streamMetadata, imageMetadata)) {
            return preferredThumbSizes.clone();
        }
        return null;
    }

    private boolean jfifOK(ImageTypeSpecifier imageType,
                           ImageWriteParam param,
                           IIOMetadata streamMetadata,
                           IIOMetadata imageMetadata) {
        // If the image type and metadata are JFIF compatible, return true
        if ((imageType != null) &&
            (!JPEG.isJFIFcompliant(imageType, true))) {
            return false;
        }
        if (imageMetadata != null) {
            JPEGMetadata metadata = null;
            if (imageMetadata instanceof JPEGMetadata) {
                metadata = (JPEGMetadata) imageMetadata;
            } else {
                metadata = (JPEGMetadata)convertImageMetadata(imageMetadata,
                                                              imageType,
                                                              param);
            }
            // metadata must have a jfif node
            if (metadata.findMarkerSegment
                (JFIFMarkerSegment.class, true) == null){
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canWriteRasters() {
        return true;
    }

    @Override
    public void write(IIOMetadata streamMetadata,
                      IIOImage image,
                      ImageWriteParam param) throws IOException {
        setThreadLock();
        try {
            cbLock.check();

            writeOnThread(streamMetadata, image, param);
        } finally {
            clearThreadLock();
        }
    }

    private void writeOnThread(IIOMetadata streamMetadata,
                      IIOImage image,
                      ImageWriteParam param) throws IOException {

        if (ios == null) {
            throw new IllegalStateException("Output has not been set!");
        }

        if (image == null) {
            throw new IllegalArgumentException("image is null!");
        }

        // if streamMetadata is not null, issue a warning
        if (streamMetadata != null) {
            warningOccurred(WARNING_STREAM_METADATA_IGNORED);
        }

        // Obtain the raster and image, if there is one
        boolean rasterOnly = image.hasRaster();

        RenderedImage rimage = null;
        if (rasterOnly) {
            srcRas = image.getRaster();
        } else {
            rimage = image.getRenderedImage();
            if (rimage instanceof BufferedImage) {
                // Use the Raster directly.
                srcRas = ((BufferedImage)rimage).getRaster();
            } else if (rimage.getNumXTiles() == 1 &&
                       rimage.getNumYTiles() == 1)
            {
                // Get the unique tile.
                srcRas = rimage.getTile(rimage.getMinTileX(),
                                        rimage.getMinTileY());

                // Ensure the Raster has dimensions of the image,
                // as the tile dimensions might differ.
                if (srcRas.getWidth() != rimage.getWidth() ||
                    srcRas.getHeight() != rimage.getHeight())
                {
                    srcRas = srcRas.createChild(srcRas.getMinX(),
                                                srcRas.getMinY(),
                                                rimage.getWidth(),
                                                rimage.getHeight(),
                                                srcRas.getMinX(),
                                                srcRas.getMinY(),
                                                null);
                }
            } else {
                // Image is tiled so get a contiguous raster by copying.
                srcRas = rimage.getData();
            }
        }

        // Now determine if we are using a band subset

        // By default, we are using all source bands
        int numSrcBands = srcRas.getNumBands();
        indexed = false;
        indexCM = null;
        ColorModel cm = null;
        ColorSpace cs = null;
        isAlphaPremultiplied = false;
        srcCM = null;
        if (!rasterOnly) {
            cm = rimage.getColorModel();
            if (cm != null) {
                cs = cm.getColorSpace();
                if (cm instanceof IndexColorModel) {
                    indexed = true;
                    indexCM = (IndexColorModel) cm;
                    numSrcBands = cm.getNumComponents();
                }
                if (cm.isAlphaPremultiplied()) {
                    isAlphaPremultiplied = true;
                    srcCM = cm;
                }
            }
        }

        srcBands = JPEG.bandOffsets[numSrcBands-1];
        int numBandsUsed = numSrcBands;
        // Consult the param to determine if we're writing a subset

        if (param != null) {
            int[] sBands = param.getSourceBands();
            if (sBands != null) {
                if (indexed) {
                    warningOccurred(WARNING_NO_BANDS_ON_INDEXED);
                } else {
                    srcBands = sBands;
                    numBandsUsed = srcBands.length;
                    if (numBandsUsed > numSrcBands) {
                        throw new IIOException
                        ("ImageWriteParam specifies too many source bands");
                    }
                }
            }
        }

        boolean usingBandSubset = (numBandsUsed != numSrcBands);
        boolean fullImage = ((!rasterOnly) && (!usingBandSubset));

        int [] bandSizes = null;
        if (!indexed) {
            bandSizes = srcRas.getSampleModel().getSampleSize();
            // If this is a subset, we must adjust bandSizes
            if (usingBandSubset) {
                int [] temp = new int [numBandsUsed];
                for (int i = 0; i < numBandsUsed; i++) {
                    temp[i] = bandSizes[srcBands[i]];
                }
                bandSizes = temp;
            }
        } else {
            int [] tempSize = srcRas.getSampleModel().getSampleSize();
            bandSizes = new int [numSrcBands];
            for (int i = 0; i < numSrcBands; i++) {
                bandSizes[i] = tempSize[0];  // All the same
            }
        }

        for (int i = 0; i < bandSizes.length; i++) {
            // 4450894 part 1: The IJG libraries are compiled so they only
            // handle <= 8-bit samples.  We now check the band sizes and throw
            // an exception for images, such as USHORT_GRAY, with > 8 bits
            // per sample.
            if (bandSizes[i] <= 0 || bandSizes[i] > 8) {
                throw new IIOException("Illegal band size: should be 0 < size <= 8");
            }
            // 4450894 part 2: We expand IndexColorModel images to full 24-
            // or 32-bit in grabPixels() for each scanline.  For indexed
            // images such as BYTE_BINARY, we need to ensure that we update
            // bandSizes to account for the scaling from 1-bit band sizes
            // to 8-bit.
            if (indexed) {
                bandSizes[i] = 8;
            }
        }

        if (debug) {
            System.out.println("numSrcBands is " + numSrcBands);
            System.out.println("numBandsUsed is " + numBandsUsed);
            System.out.println("usingBandSubset is " + usingBandSubset);
            System.out.println("fullImage is " + fullImage);
            System.out.print("Band sizes:");
            for (int i = 0; i< bandSizes.length; i++) {
                System.out.print(" " + bandSizes[i]);
            }
            System.out.println();
        }

        // Destination type, if there is one
        ImageTypeSpecifier destType = null;
        if (param != null) {
            destType = param.getDestinationType();
            // Ignore dest type if we are writing a complete image
            if ((fullImage) && (destType != null)) {
                warningOccurred(WARNING_DEST_IGNORED);
                destType = null;
            }
        }

        // Examine the param

        sourceXOffset = srcRas.getMinX();
        sourceYOffset = srcRas.getMinY();
        int imageWidth = srcRas.getWidth();
        int imageHeight = srcRas.getHeight();
        sourceWidth = imageWidth;
        sourceHeight = imageHeight;
        int periodX = 1;
        int periodY = 1;
        int gridX = 0;
        int gridY = 0;
        JPEGQTable [] qTables = null;
        JPEGHuffmanTable[] DCHuffmanTables = null;
        JPEGHuffmanTable[] ACHuffmanTables = null;
        boolean optimizeHuffman = false;
        JPEGImageWriteParam jparam = null;
        int progressiveMode = ImageWriteParam.MODE_DISABLED;

        if (param != null) {

            Rectangle sourceRegion = param.getSourceRegion();
            if (sourceRegion != null) {
                Rectangle imageBounds = new Rectangle(sourceXOffset,
                                                      sourceYOffset,
                                                      sourceWidth,
                                                      sourceHeight);
                sourceRegion = sourceRegion.intersection(imageBounds);
                sourceXOffset = sourceRegion.x;
                sourceYOffset = sourceRegion.y;
                sourceWidth = sourceRegion.width;
                sourceHeight = sourceRegion.height;
            }

            if (sourceWidth + sourceXOffset > imageWidth) {
                sourceWidth = imageWidth - sourceXOffset;
            }
            if (sourceHeight + sourceYOffset > imageHeight) {
                sourceHeight = imageHeight - sourceYOffset;
            }

            periodX = param.getSourceXSubsampling();
            periodY = param.getSourceYSubsampling();
            gridX = param.getSubsamplingXOffset();
            gridY = param.getSubsamplingYOffset();

            switch(param.getCompressionMode()) {
            case ImageWriteParam.MODE_DISABLED:
                throw new IIOException("JPEG compression cannot be disabled");
            case ImageWriteParam.MODE_EXPLICIT:
                float quality = param.getCompressionQuality();
                quality = JPEG.convertToLinearQuality(quality);
                qTables = new JPEGQTable[2];
                qTables[0] = JPEGQTable.K1Luminance.getScaledInstance
                    (quality, true);
                qTables[1] = JPEGQTable.K2Chrominance.getScaledInstance
                    (quality, true);
                break;
            case ImageWriteParam.MODE_DEFAULT:
                qTables = new JPEGQTable[2];
                qTables[0] = JPEGQTable.K1Div2Luminance;
                qTables[1] = JPEGQTable.K2Div2Chrominance;
                break;
            // We'll handle the metadata case later
            }

            progressiveMode = param.getProgressiveMode();

            if (param instanceof JPEGImageWriteParam) {
                jparam = (JPEGImageWriteParam)param;
                optimizeHuffman = jparam.getOptimizeHuffmanTables();
            }
        }

        // Now examine the metadata
        IIOMetadata mdata = image.getMetadata();
        if (mdata != null) {
            if (mdata instanceof JPEGMetadata) {
                metadata = (JPEGMetadata) mdata;
                if (debug) {
                    System.out.println
                        ("We have metadata, and it's JPEG metadata");
                }
            } else {
                if (!rasterOnly) {
                    ImageTypeSpecifier type = destType;
                    if (type == null) {
                        type = new ImageTypeSpecifier(rimage);
                    }
                    metadata = (JPEGMetadata) convertImageMetadata(mdata,
                                                                   type,
                                                                   param);
                } else {
                    warningOccurred(WARNING_METADATA_NOT_JPEG_FOR_RASTER);
                }
            }
        }

        // First set a default state

        ignoreJFIF = false;  // If it's there, use it
        ignoreAdobe = false;  // If it's there, use it
        newAdobeTransform = JPEG.ADOBE_IMPOSSIBLE;  // Change if needed
        writeDefaultJFIF = false;
        writeAdobe = false;

        // By default we'll do no conversion:
        int inCsType = JPEG.JCS_UNKNOWN;
        int outCsType = JPEG.JCS_UNKNOWN;

        JFIFMarkerSegment jfif = null;
        AdobeMarkerSegment adobe = null;
        SOFMarkerSegment sof = null;

        if (metadata != null) {
            jfif = (JFIFMarkerSegment) metadata.findMarkerSegment
                (JFIFMarkerSegment.class, true);
            adobe = (AdobeMarkerSegment) metadata.findMarkerSegment
                (AdobeMarkerSegment.class, true);
            sof = (SOFMarkerSegment) metadata.findMarkerSegment
                (SOFMarkerSegment.class, true);
        }

        iccProfile = null;  // By default don't write one
        convertTosRGB = false;  // PhotoYCC does this
        converted = null;

        if (destType != null) {
            if (numBandsUsed != destType.getNumBands()) {
                throw new IIOException
                    ("Number of source bands != number of destination bands");
            }
            cs = destType.getColorModel().getColorSpace();
            // Check the metadata against the destination type
            if (metadata != null) {
                checkSOFBands(sof, numBandsUsed);

                checkJFIF(jfif, destType, false);
                // Do we want to write an ICC profile?
                if ((jfif != null) && (ignoreJFIF == false)) {
                    if (ImageUtil.isNonStandardICCColorSpace(cs)) {
                        iccProfile = ((ICC_ColorSpace) cs).getProfile();
                    }
                }
                checkAdobe(adobe, destType, false);

            } else { // no metadata, but there is a dest type
                // If we can add a JFIF or an Adobe marker segment, do so
                if (JPEG.isJFIFcompliant(destType, false)) {
                    writeDefaultJFIF = true;
                    // Do we want to write an ICC profile?
                    if (ImageUtil.isNonStandardICCColorSpace(cs)) {
                        iccProfile = ((ICC_ColorSpace) cs).getProfile();
                    }
                } else {
                    int transform = JPEG.transformForType(destType, false);
                    if (transform != JPEG.ADOBE_IMPOSSIBLE) {
                        writeAdobe = true;
                        newAdobeTransform = transform;
                    }
                }
                // re-create the metadata
                metadata = new JPEGMetadata(destType, null, this);
            }
            inCsType = getSrcCSType(destType);
            outCsType = getDefaultDestCSType(destType);
        } else { // no destination type
            if (metadata == null) {
                if (fullImage) {  // no dest, no metadata, full image
                    // Use default metadata matching the image and param
                    metadata = new JPEGMetadata(new ImageTypeSpecifier(rimage),
                                                param, this);
                    if (metadata.findMarkerSegment
                        (JFIFMarkerSegment.class, true) != null) {
                        cs = rimage.getColorModel().getColorSpace();
                        if (ImageUtil.isNonStandardICCColorSpace(cs)) {
                            iccProfile = ((ICC_ColorSpace) cs).getProfile();
                        }
                    }

                    inCsType = getSrcCSType(rimage);
                    outCsType = getDefaultDestCSType(rimage);
                }
                // else no dest, no metadata, not an image,
                // so no special headers, no color conversion
            } else { // no dest type, but there is metadata
                checkSOFBands(sof, numBandsUsed);
                if (fullImage) {  // no dest, metadata, image
                    // Check that the metadata and the image match

                    ImageTypeSpecifier inputType =
                        new ImageTypeSpecifier(rimage);

                    inCsType = getSrcCSType(rimage);

                    if (cm != null) {
                        boolean alpha = cm.hasAlpha();
                        switch (cs.getType()) {
                        case ColorSpace.TYPE_GRAY:
                            if (!alpha) {
                                outCsType = JPEG.JCS_GRAYSCALE;
                            } else {
                                if (jfif != null) {
                                    ignoreJFIF = true;
                                    warningOccurred
                                    (WARNING_IMAGE_METADATA_JFIF_MISMATCH);
                                }
                                // out colorspace remains unknown
                            }
                            if ((adobe != null)
                                && (adobe.transform != JPEG.ADOBE_UNKNOWN)) {
                                newAdobeTransform = JPEG.ADOBE_UNKNOWN;
                                warningOccurred
                                (WARNING_IMAGE_METADATA_ADOBE_MISMATCH);
                            }
                            break;
                        case ColorSpace.TYPE_RGB:
                            if (jfif != null) {
                                outCsType = JPEG.JCS_YCbCr;
                                if (ImageUtil.isNonStandardICCColorSpace(cs)
                                    || ((cs instanceof ICC_ColorSpace)
                                        && (jfif.iccSegment != null))) {
                                    iccProfile =
                                        ((ICC_ColorSpace) cs).getProfile();
                                }
                            } else if (adobe != null) {
                                switch (adobe.transform) {
                                case JPEG.ADOBE_UNKNOWN:
                                    outCsType = JPEG.JCS_RGB;
                                    break;
                                case JPEG.ADOBE_YCC:
                                    outCsType = JPEG.JCS_YCbCr;
                                    break;
                                default:
                                    warningOccurred
                                    (WARNING_IMAGE_METADATA_ADOBE_MISMATCH);
                                    newAdobeTransform = JPEG.ADOBE_UNKNOWN;
                                    outCsType = JPEG.JCS_RGB;
                                    break;
                                }
                            } else {
                                // consult the ids
                                int outCS = sof.getIDencodedCSType();
                                // if they don't resolve it,
                                // consult the sampling factors
                                if (outCS != JPEG.JCS_UNKNOWN) {
                                    outCsType = outCS;
                                } else {
                                    boolean subsampled =
                                    isSubsampled(sof.componentSpecs);
                                    if (subsampled) {
                                        outCsType = JPEG.JCS_YCbCr;
                                    } else {
                                        outCsType = JPEG.JCS_RGB;
                                    }
                                }
                            }
                            break;
                        }
                    }
                } // else no dest, metadata, not an image.  Defaults ok
            }
        }

        boolean metadataProgressive = false;
        int [] scans = null;

        if (metadata != null) {
            if (sof == null) {
                sof = (SOFMarkerSegment) metadata.findMarkerSegment
                    (SOFMarkerSegment.class, true);
            }
            if ((sof != null) && (sof.tag == JPEG.SOF2)) {
                metadataProgressive = true;
                if (progressiveMode == ImageWriteParam.MODE_COPY_FROM_METADATA) {
                    scans = collectScans(metadata, sof);  // Might still be null
                } else {
                    numScans = 0;
                }
            }
            if (jfif == null) {
                jfif = (JFIFMarkerSegment) metadata.findMarkerSegment
                    (JFIFMarkerSegment.class, true);
            }
        }

        thumbnails = image.getThumbnails();
        int numThumbs = image.getNumThumbnails();
        forceJFIF = false;
        // determine if thumbnails can be written
        // If we are going to add a default JFIF marker segment,
        // then thumbnails can be written
        if (!writeDefaultJFIF) {
            // If there is no metadata, then we can't write thumbnails
            if (metadata == null) {
                thumbnails = null;
                if (numThumbs != 0) {
                    warningOccurred(WARNING_IGNORING_THUMBS);
                }
            } else {
                // There is metadata
                // If we are writing a raster or subbands,
                // then the user must specify JFIF on the metadata
                if (fullImage == false) {
                    if (jfif == null) {
                        thumbnails = null;  // Or we can't include thumbnails
                        if (numThumbs != 0) {
                            warningOccurred(WARNING_IGNORING_THUMBS);
                        }
                    }
                } else {  // It is a full image, and there is metadata
                    if (jfif == null) {  // Not JFIF
                        // Can it have JFIF?
                        if ((outCsType == JPEG.JCS_GRAYSCALE)
                            || (outCsType == JPEG.JCS_YCbCr)) {
                            if (numThumbs != 0) {
                                forceJFIF = true;
                                warningOccurred(WARNING_FORCING_JFIF);
                            }
                        } else {  // Nope, not JFIF-compatible
                            thumbnails = null;
                            if (numThumbs != 0) {
                                warningOccurred(WARNING_IGNORING_THUMBS);
                            }
                        }
                    }
                }
            }
        }

        // Set up a boolean to indicate whether we need to call back to
        // write metadata
        boolean haveMetadata =
            ((metadata != null) || writeDefaultJFIF || writeAdobe);

        // Now that we have dealt with metadata, finalize our tables set up

        // Are we going to write tables?  By default, yes.
        boolean writeDQT = true;
        boolean writeDHT = true;

        // But if the metadata has no tables, no.
        DQTMarkerSegment dqt = null;
        DHTMarkerSegment dht = null;

        int restartInterval = 0;

        if (metadata != null) {
            dqt = (DQTMarkerSegment) metadata.findMarkerSegment
                (DQTMarkerSegment.class, true);
            dht = (DHTMarkerSegment) metadata.findMarkerSegment
                (DHTMarkerSegment.class, true);
            DRIMarkerSegment dri =
                (DRIMarkerSegment) metadata.findMarkerSegment
                (DRIMarkerSegment.class, true);
            if (dri != null) {
                restartInterval = dri.restartInterval;
            }

            if (dqt == null) {
                writeDQT = false;
            }
            if (dht == null) {
                writeDHT = false;  // Ignored if optimizeHuffman is true
            }
        }

        // Whether we write tables or not, we need to figure out which ones
        // to use
        if (qTables == null) { // Get them from metadata, or use defaults
            if (dqt != null) {
                qTables = collectQTablesFromMetadata(metadata);
            } else if (streamQTables != null) {
                qTables = streamQTables;
            } else if ((jparam != null) && (jparam.areTablesSet())) {
                qTables = jparam.getQTables();
            } else {
                qTables = JPEG.getDefaultQTables();
            }

        }

        // If we are optimizing, we don't want any tables.
        if (optimizeHuffman == false) {
            // If they were for progressive scans, we can't use them.
            if ((dht != null) && (metadataProgressive == false)) {
                DCHuffmanTables = collectHTablesFromMetadata(metadata, true);
                ACHuffmanTables = collectHTablesFromMetadata(metadata, false);
            } else if (streamDCHuffmanTables != null) {
                DCHuffmanTables = streamDCHuffmanTables;
                ACHuffmanTables = streamACHuffmanTables;
            } else if ((jparam != null) && (jparam.areTablesSet())) {
                DCHuffmanTables = jparam.getDCHuffmanTables();
                ACHuffmanTables = jparam.getACHuffmanTables();
            } else {
                DCHuffmanTables = JPEG.getDefaultHuffmanTables(true);
                ACHuffmanTables = JPEG.getDefaultHuffmanTables(false);
            }
        }

        // By default, ids are 1 - N, no subsampling
        int [] componentIds = new int[numBandsUsed];
        int [] HsamplingFactors = new int[numBandsUsed];
        int [] VsamplingFactors = new int[numBandsUsed];
        int [] QtableSelectors = new int[numBandsUsed];
        for (int i = 0; i < numBandsUsed; i++) {
            componentIds[i] = i+1; // JFIF compatible
            HsamplingFactors[i] = 1;
            VsamplingFactors[i] = 1;
            QtableSelectors[i] = 0;
        }

        // Now override them with the contents of sof, if there is one,
        if (sof != null) {
            for (int i = 0; i < numBandsUsed; i++) {
                if (forceJFIF == false) {  // else use JFIF-compatible default
                    componentIds[i] = sof.componentSpecs[i].componentId;
                }
                HsamplingFactors[i] = sof.componentSpecs[i].HsamplingFactor;
                VsamplingFactors[i] = sof.componentSpecs[i].VsamplingFactor;
                QtableSelectors[i] = sof.componentSpecs[i].QtableSelector;
            }
        }

        sourceXOffset += gridX;
        sourceWidth -= gridX;
        sourceYOffset += gridY;
        sourceHeight -= gridY;

        int destWidth = (sourceWidth + periodX - 1)/periodX;
        int destHeight = (sourceHeight + periodY - 1)/periodY;

        // Create an appropriate 1-line databuffer for writing
        int lineSize = sourceWidth*numBandsUsed;

        DataBufferByte buffer = new DataBufferByte(lineSize);

        // Create a raster from that
        int [] bandOffs = JPEG.bandOffsets[numBandsUsed-1];

        raster = Raster.createInterleavedRaster(buffer,
                                                sourceWidth, 1,
                                                lineSize,
                                                numBandsUsed,
                                                bandOffs,
                                                null);

        // Call the writer, who will call back for every scanline

        clearAbortRequest();
        cbLock.lock();
        try {
            processImageStarted(currentImage);
        } finally {
            cbLock.unlock();
        }

        boolean aborted = false;

        if (debug) {
            System.out.println("inCsType: " + inCsType);
            System.out.println("outCsType: " + outCsType);
        }

        // Note that getData disables acceleration on buffer, but it is
        // just a 1-line intermediate data transfer buffer that does not
        // affect the acceleration of the source image.
        aborted = writeImage(structPointer,
                             buffer.getData(),
                             inCsType, outCsType,
                             numBandsUsed,
                             bandSizes,
                             sourceWidth,
                             destWidth, destHeight,
                             periodX, periodY,
                             qTables,
                             writeDQT,
                             DCHuffmanTables,
                             ACHuffmanTables,
                             writeDHT,
                             optimizeHuffman,
                             (progressiveMode
                              != ImageWriteParam.MODE_DISABLED),
                             numScans,
                             scans,
                             componentIds,
                             HsamplingFactors,
                             VsamplingFactors,
                             QtableSelectors,
                             haveMetadata,
                             restartInterval);

        cbLock.lock();
        try {
            if (aborted) {
                processWriteAborted();
            } else {
                processImageComplete();
            }

            ios.flush();
        } finally {
            cbLock.unlock();
        }
        currentImage++;  // After a successful write
    }

    @Override
    public boolean canWriteSequence() {
        return true;
    }

    @Override
    public void prepareWriteSequence(IIOMetadata streamMetadata)
        throws IOException {
        setThreadLock();
        try {
            cbLock.check();

            prepareWriteSequenceOnThread(streamMetadata);
        } finally {
            clearThreadLock();
        }
    }

    private void prepareWriteSequenceOnThread(IIOMetadata streamMetadata)
        throws IOException {
        if (ios == null) {
            throw new IllegalStateException("Output has not been set!");
        }

        /*
         * from jpeg_metadata.html:
         * If no stream metadata is supplied to
         * {@code ImageWriter.prepareWriteSequence}, then no
         * tables-only image is written.  If stream metadata containing
         * no tables is supplied to
         * {@code ImageWriter.prepareWriteSequence}, then a tables-only
         * image containing default visually lossless tables is written.
         */
        if (streamMetadata != null) {
            if (streamMetadata instanceof JPEGMetadata) {
                // write a complete tables-only image at the beginning of
                // the stream.
                JPEGMetadata jmeta = (JPEGMetadata) streamMetadata;
                if (jmeta.isStream == false) {
                    throw new IllegalArgumentException
                        ("Invalid stream metadata object.");
                }
                // Check that we are
                // at the beginning of the stream, or can go there, and haven't
                // written out the metadata already.
                if (currentImage != 0) {
                    throw new IIOException
                        ("JPEG Stream metadata must precede all images");
                }
                if (sequencePrepared == true) {
                    throw new IIOException("Stream metadata already written!");
                }

                // Set the tables
                // If the metadata has no tables, use default tables.
                streamQTables = collectQTablesFromMetadata(jmeta);
                if (debug) {
                    System.out.println("after collecting from stream metadata, "
                                       + "streamQTables.length is "
                                       + streamQTables.length);
                }
                if (streamQTables == null) {
                    streamQTables = JPEG.getDefaultQTables();
                }
                streamDCHuffmanTables =
                    collectHTablesFromMetadata(jmeta, true);
                if (streamDCHuffmanTables == null) {
                    streamDCHuffmanTables = JPEG.getDefaultHuffmanTables(true);
                }
                streamACHuffmanTables =
                    collectHTablesFromMetadata(jmeta, false);
                if (streamACHuffmanTables == null) {
                    streamACHuffmanTables = JPEG.getDefaultHuffmanTables(false);
                }

                // Now write them out
                writeTables(structPointer,
                            streamQTables,
                            streamDCHuffmanTables,
                            streamACHuffmanTables);
            } else {
                throw new IIOException("Stream metadata must be JPEG metadata");
            }
        }
        sequencePrepared = true;
    }

    @Override
    public void writeToSequence(IIOImage image, ImageWriteParam param)
        throws IOException {
        setThreadLock();
        try {
            cbLock.check();

            if (sequencePrepared == false) {
                throw new IllegalStateException("sequencePrepared not called!");
            }
            // In the case of JPEG this does nothing different from write
            write(null, image, param);
        } finally {
            clearThreadLock();
        }
    }

    @Override
    public void endWriteSequence() throws IOException {
        setThreadLock();
        try {
            cbLock.check();

            if (sequencePrepared == false) {
                throw new IllegalStateException("sequencePrepared not called!");
            }
            sequencePrepared = false;
        } finally {
            clearThreadLock();
        }
    }

    @Override
    public synchronized void abort() {
        setThreadLock();
        try {
            /**
             * NB: we do not check the call back lock here, we allow to abort
             * the reader any time.
             */
            super.abort();
            abortWrite(structPointer);
        } finally {
            clearThreadLock();
        }
    }

    @Override
    protected synchronized void clearAbortRequest() {
        setThreadLock();
        try {
            cbLock.check();
            if (abortRequested()) {
                super.clearAbortRequest();
                // reset C structures
                resetWriter(structPointer);
                // reset the native destination
                setDest(structPointer);
            }
        } finally {
            clearThreadLock();
        }
    }

    private void resetInternalState() {
        // reset C structures
        resetWriter(structPointer);

        // reset local Java structures
        srcRas = null;
        raster = null;
        convertTosRGB = false;
        currentImage = 0;
        numScans = 0;
        metadata = null;
    }

    @Override
    public void reset() {
        setThreadLock();
        try {
            cbLock.check();

            super.reset();
        } finally {
            clearThreadLock();
        }
    }

    @Override
    public void dispose() {
        setThreadLock();
        try {
            cbLock.check();

            if (structPointer != 0) {
                disposerRecord.dispose();
                structPointer = 0;
            }
        } finally {
            clearThreadLock();
        }
    }

    ////////// End of public API

    ///////// Package-access API

    /**
     * Called by the native code or other classes to signal a warning.
     * The code is used to lookup a localized message to be used when
     * sending warnings to listeners.
     */
    void warningOccurred(int code) {
        cbLock.lock();
        try {
            if ((code < 0) || (code > MAX_WARNING)){
                throw new InternalError("Invalid warning index");
            }
            processWarningOccurred
                (currentImage,
                 "com.sun.imageio.plugins.jpeg.JPEGImageWriterResources",
                Integer.toString(code));
        } finally {
            cbLock.unlock();
        }
    }

    /**
     * The library has it's own error facility that emits warning messages.
     * This routine is called by the native code when it has already
     * formatted a string for output.
     * XXX  For truly complete localization of all warning messages,
     * the sun_jpeg_output_message routine in the native code should
     * send only the codes and parameters to a method here in Java,
     * which will then format and send the warnings, using localized
     * strings.  This method will have to deal with all the parameters
     * and formats (%u with possibly large numbers, %02d, %02x, etc.)
     * that actually occur in the JPEG library.  For now, this prevents
     * library warnings from being printed to stderr.
     */
    void warningWithMessage(String msg) {
        cbLock.lock();
        try {
            processWarningOccurred(currentImage, msg);
        } finally {
            cbLock.unlock();
        }
    }

    void thumbnailStarted(int thumbnailIndex) {
        cbLock.lock();
        try {
            processThumbnailStarted(currentImage, thumbnailIndex);
        } finally {
            cbLock.unlock();
        }
    }

    // Provide access to protected superclass method
    void thumbnailProgress(float percentageDone) {
        cbLock.lock();
        try {
            processThumbnailProgress(percentageDone);
        } finally {
            cbLock.unlock();
        }
    }

    // Provide access to protected superclass method
    void thumbnailComplete() {
        cbLock.lock();
        try {
            processThumbnailComplete();
        } finally {
            cbLock.unlock();
        }
    }

    ///////// End of Package-access API

    ///////// Private methods

    ///////// Metadata handling

    private void checkSOFBands(SOFMarkerSegment sof, int numBandsUsed)
        throws IIOException {
        // Does the metadata frame header, if any, match numBandsUsed?
        if (sof != null) {
            if (sof.componentSpecs.length != numBandsUsed) {
                throw new IIOException
                    ("Metadata components != number of destination bands");
            }
        }
    }

    private void checkJFIF(JFIFMarkerSegment jfif,
                           ImageTypeSpecifier type,
                           boolean input) {
        if (jfif != null) {
            if (!JPEG.isJFIFcompliant(type, input)) {
                ignoreJFIF = true;  // type overrides metadata
                warningOccurred(input
                                ? WARNING_IMAGE_METADATA_JFIF_MISMATCH
                                : WARNING_DEST_METADATA_JFIF_MISMATCH);
            }
        }
    }

    private void checkAdobe(AdobeMarkerSegment adobe,
                           ImageTypeSpecifier type,
                           boolean input) {
        if (adobe != null) {
            int rightTransform = JPEG.transformForType(type, input);
            if (adobe.transform != rightTransform) {
                warningOccurred(input
                                ? WARNING_IMAGE_METADATA_ADOBE_MISMATCH
                                : WARNING_DEST_METADATA_ADOBE_MISMATCH);
                if (rightTransform == JPEG.ADOBE_IMPOSSIBLE) {
                    ignoreAdobe = true;
                } else {
                    newAdobeTransform = rightTransform;
                }
            }
        }
    }

    /**
     * Collect all the scan info from the given metadata, and
     * organize it into the scan info array required by the
     * IJG libray.  It is much simpler to parse out this
     * data in Java and then just copy the data in C.
     */
    private int [] collectScans(JPEGMetadata metadata,
                                SOFMarkerSegment sof) {
        List<SOSMarkerSegment> segments = new ArrayList<>();
        int SCAN_SIZE = 9;
        int MAX_COMPS_PER_SCAN = 4;
        for (Iterator<MarkerSegment> iter = metadata.markerSequence.iterator();
             iter.hasNext();) {
            MarkerSegment seg = iter.next();
            if (seg instanceof SOSMarkerSegment) {
                segments.add((SOSMarkerSegment) seg);
            }
        }
        int [] retval = null;
        numScans = 0;
        if (!segments.isEmpty()) {
            numScans = segments.size();
            retval = new int [numScans*SCAN_SIZE];
            int index = 0;
            for (int i = 0; i < numScans; i++) {
                SOSMarkerSegment sos = segments.get(i);
                retval[index++] = sos.componentSpecs.length; // num comps
                for (int j = 0; j < MAX_COMPS_PER_SCAN; j++) {
                    if (j < sos.componentSpecs.length) {
                        int compSel = sos.componentSpecs[j].componentSelector;
                        for (int k = 0; k < sof.componentSpecs.length; k++) {
                            if (compSel == sof.componentSpecs[k].componentId) {
                                retval[index++] = k;
                                break; // out of for over sof comps
                            }
                        }
                    } else {
                        retval[index++] = 0;
                    }
                }
                retval[index++] = sos.startSpectralSelection;
                retval[index++] = sos.endSpectralSelection;
                retval[index++] = sos.approxHigh;
                retval[index++] = sos.approxLow;
            }
        }
        return retval;
    }

    /**
     * Finds all DQT marker segments and returns all the q
     * tables as a single array of JPEGQTables.
     */
    private JPEGQTable [] collectQTablesFromMetadata
        (JPEGMetadata metadata) {
        ArrayList<DQTMarkerSegment.Qtable> tables = new ArrayList<>();
        for (MarkerSegment seg : metadata.markerSequence) {
            if (seg instanceof DQTMarkerSegment) {
                DQTMarkerSegment dqt =
                    (DQTMarkerSegment) seg;
                tables.addAll(dqt.tables);
            }
        }
        JPEGQTable [] retval = null;
        if (tables.size() != 0) {
            retval = new JPEGQTable[tables.size()];
            for (int i = 0; i < retval.length; i++) {
                retval[i] =
                    new JPEGQTable(tables.get(i).data);
            }
        }
        return retval;
    }

    /**
     * Finds all DHT marker segments and returns all the q
     * tables as a single array of JPEGQTables.  The metadata
     * must not be for a progressive image, or an exception
     * will be thrown when two Huffman tables with the same
     * table id are encountered.
     */
    private JPEGHuffmanTable[] collectHTablesFromMetadata
        (JPEGMetadata metadata, boolean wantDC) throws IIOException {
        ArrayList<DHTMarkerSegment.Htable> tables = new ArrayList<>();
        for (MarkerSegment seg : metadata.markerSequence) {
            if (seg instanceof DHTMarkerSegment) {
                DHTMarkerSegment dht = (DHTMarkerSegment) seg;
                for (int i = 0; i < dht.tables.size(); i++) {
                    DHTMarkerSegment.Htable htable = dht.tables.get(i);
                    if (htable.tableClass == (wantDC ? 0 : 1)) {
                        tables.add(htable);
                    }
                }
            }
        }
        JPEGHuffmanTable [] retval = null;
        if (tables.size() != 0) {
            DHTMarkerSegment.Htable [] htables =
                new DHTMarkerSegment.Htable[tables.size()];
            tables.toArray(htables);
            retval = new JPEGHuffmanTable[tables.size()];
            for (int i = 0; i < retval.length; i++) {
                retval[i] = null;
                for (int j = 0; j < tables.size(); j++) {
                    if (htables[j].tableID == i) {
                        if (retval[i] != null) {
                            throw new IIOException("Metadata has duplicate Htables!");
                        }
                        retval[i] = new JPEGHuffmanTable(htables[j].numCodes,
                                                         htables[j].values);
                    }
                }
            }
        }

        return retval;
    }

    /////////// End of metadata handling

    ////////////// ColorSpace conversion

    private int getSrcCSType(ImageTypeSpecifier type) {
         return getSrcCSType(type.getColorModel());
    }

    private int getSrcCSType(RenderedImage rimage) {
        return getSrcCSType(rimage.getColorModel());
    }

    private int getSrcCSType(ColorModel cm) {
        int retval = JPEG.JCS_UNKNOWN;
        if (cm != null) {
            boolean alpha = cm.hasAlpha();
            ColorSpace cs = cm.getColorSpace();
            switch (cs.getType()) {
            case ColorSpace.TYPE_GRAY:
                retval = JPEG.JCS_GRAYSCALE;
                break;
            case ColorSpace.TYPE_RGB:
                retval = JPEG.JCS_RGB;
                break;
            case ColorSpace.TYPE_YCbCr:
                retval = JPEG.JCS_YCbCr;
                break;
            case ColorSpace.TYPE_CMYK:
                retval = JPEG.JCS_CMYK;
                break;
            }
        }
        return retval;
    }

    private int getDestCSType(ImageTypeSpecifier destType) {
        ColorModel cm = destType.getColorModel();
        boolean alpha = cm.hasAlpha();
        ColorSpace cs = cm.getColorSpace();
        int retval = JPEG.JCS_UNKNOWN;
        switch (cs.getType()) {
        case ColorSpace.TYPE_GRAY:
                retval = JPEG.JCS_GRAYSCALE;
                break;
            case ColorSpace.TYPE_RGB:
                retval = JPEG.JCS_RGB;
                break;
            case ColorSpace.TYPE_YCbCr:
                retval = JPEG.JCS_YCbCr;
                break;
            case ColorSpace.TYPE_CMYK:
                retval = JPEG.JCS_CMYK;
                break;
            }
        return retval;
        }

    private int getDefaultDestCSType(ImageTypeSpecifier type) {
        return getDefaultDestCSType(type.getColorModel());
    }

    private int getDefaultDestCSType(RenderedImage rimage) {
        return getDefaultDestCSType(rimage.getColorModel());
    }

    private int getDefaultDestCSType(ColorModel cm) {
        int retval = JPEG.JCS_UNKNOWN;
        if (cm != null) {
            boolean alpha = cm.hasAlpha();
            ColorSpace cs = cm.getColorSpace();
            switch (cs.getType()) {
            case ColorSpace.TYPE_GRAY:
                retval = JPEG.JCS_GRAYSCALE;
                break;
            case ColorSpace.TYPE_RGB:
                retval = JPEG.JCS_YCbCr;
                break;
            case ColorSpace.TYPE_YCbCr:
                retval = JPEG.JCS_YCbCr;
                break;
            case ColorSpace.TYPE_CMYK:
                retval = JPEG.JCS_YCCK;
                break;
            }
        }
        return retval;
    }

    private boolean isSubsampled(SOFMarkerSegment.ComponentSpec [] specs) {
        int hsamp0 = specs[0].HsamplingFactor;
        int vsamp0 = specs[0].VsamplingFactor;
        for (int i = 1; i < specs.length; i++) {
            if ((specs[i].HsamplingFactor != hsamp0) ||
                (specs[i].VsamplingFactor != vsamp0))
                return true;
        }
        return false;
    }

    ////////////// End of ColorSpace conversion

    ////////////// Native methods and callbacks

    /** Sets up static native structures. */
    private static native void initWriterIDs(Class<?> qTableClass,
                                             Class<?> huffClass);

    /** Sets up per-writer native structure and returns a pointer to it. */
    private native long initJPEGImageWriter();

    /** Sets up native structures for output stream */
    private native void setDest(long structPointer);

    /**
     * Returns {@code true} if the write was aborted.
     */
    private native boolean writeImage(long structPointer,
                                      byte [] data,
                                      int inCsType, int outCsType,
                                      int numBands,
                                      int [] bandSizes,
                                      int srcWidth,
                                      int destWidth, int destHeight,
                                      int stepX, int stepY,
                                      JPEGQTable [] qtables,
                                      boolean writeDQT,
                                      JPEGHuffmanTable[] DCHuffmanTables,
                                      JPEGHuffmanTable[] ACHuffmanTables,
                                      boolean writeDHT,
                                      boolean optimizeHuffman,
                                      boolean progressive,
                                      int numScans,
                                      int [] scans,
                                      int [] componentIds,
                                      int [] HsamplingFactors,
                                      int [] VsamplingFactors,
                                      int [] QtableSelectors,
                                      boolean haveMetadata,
                                      int restartInterval);


    /**
     * Writes the metadata out when called by the native code,
     * which will have already written the header to the stream
     * and established the library state.  This is simpler than
     * breaking the write call in two.
     */
    private void writeMetadata() throws IOException {
        if (metadata == null) {
            if (writeDefaultJFIF) {
                JFIFMarkerSegment.writeDefaultJFIF(ios,
                                                   thumbnails,
                                                   iccProfile,
                                                   this);
            }
            if (writeAdobe) {
                AdobeMarkerSegment.writeAdobeSegment(ios, newAdobeTransform);
            }
        } else {
            metadata.writeToStream(ios,
                                   ignoreJFIF,
                                   forceJFIF,
                                   thumbnails,
                                   iccProfile,
                                   ignoreAdobe,
                                   newAdobeTransform,
                                   this);
        }
    }

    /**
     * Write out a tables-only image to the stream.
     */
    private native void writeTables(long structPointer,
                                    JPEGQTable [] qtables,
                                    JPEGHuffmanTable[] DCHuffmanTables,
                                    JPEGHuffmanTable[] ACHuffmanTables);

    /**
     * Put the scanline y of the source ROI view Raster into the
     * 1-line Raster for writing.  This handles ROI and band
     * rearrangements, and expands indexed images.  Subsampling is
     * done in the native code.
     * This is called by the native code.
     */
    private void grabPixels(int y) {

        Raster sourceLine = null;
        if (indexed) {
            sourceLine = srcRas.createChild(sourceXOffset,
                                            sourceYOffset+y,
                                            sourceWidth, 1,
                                            0, 0,
                                            new int [] {0});
            // If the image has BITMASK transparency, we need to make sure
            // it gets converted to 32-bit ARGB, because the JPEG encoder
            // relies upon the full 8-bit alpha channel.
            boolean forceARGB =
                (indexCM.getTransparency() != Transparency.OPAQUE);
            BufferedImage temp = indexCM.convertToIntDiscrete(sourceLine,
                                                              forceARGB);
            sourceLine = temp.getRaster();
        } else {
            sourceLine = srcRas.createChild(sourceXOffset,
                                            sourceYOffset+y,
                                            sourceWidth, 1,
                                            0, 0,
                                            srcBands);
        }
        if (convertTosRGB) {
            if (debug) {
                System.out.println("Converting to sRGB");
            }
            // The first time through, converted is null, so
            // a new raster is allocated.  It is then reused
            // on subsequent lines.
            converted = convertOp.filter(sourceLine, converted);
            sourceLine = converted;
        }
        if (isAlphaPremultiplied) {
            WritableRaster wr = sourceLine.createCompatibleWritableRaster();
            int[] data = null;
            data = sourceLine.getPixels(sourceLine.getMinX(), sourceLine.getMinY(),
                                        sourceLine.getWidth(), sourceLine.getHeight(),
                                        data);
            wr.setPixels(sourceLine.getMinX(), sourceLine.getMinY(),
                         sourceLine.getWidth(), sourceLine.getHeight(),
                         data);
            srcCM.coerceData(wr, false);
            sourceLine = wr.createChild(wr.getMinX(), wr.getMinY(),
                                        wr.getWidth(), wr.getHeight(),
                                        0, 0,
                                        srcBands);
        }
        raster.setRect(sourceLine);
        if ((y > 7) && (y%8 == 0)) {  // Every 8 scanlines
            cbLock.lock();
            try {
                processImageProgress((float) y / (float) sourceHeight * 100.0F);
            } finally {
                cbLock.unlock();
            }
        }
    }

    /** Aborts the current write in the native code */
    private native void abortWrite(long structPointer);

    /** Resets native structures */
    private native void resetWriter(long structPointer);

    /** Releases native structures */
    private static native void disposeWriter(long structPointer);

    private static class JPEGWriterDisposerRecord implements DisposerRecord {
        private long pData;

        public JPEGWriterDisposerRecord(long pData) {
            this.pData = pData;
        }

        @Override
        public synchronized void dispose() {
            if (pData != 0) {
                disposeWriter(pData);
                pData = 0;
            }
        }
    }

    /**
     * This method is called from native code in order to write encoder
     * output to the destination.
     *
     * We block any attempt to change the writer state during this
     * method, in order to prevent a corruption of the native encoder
     * state.
     */
    private void writeOutputData(byte[] data, int offset, int len)
            throws IOException
    {
        cbLock.lock();
        try {
            ios.write(data, offset, len);
        } finally {
            cbLock.unlock();
        }
    }

    private Thread theThread = null;
    private int theLockCount = 0;

    private synchronized void setThreadLock() {
        Thread currThread = Thread.currentThread();
        if (theThread != null) {
            if (theThread != currThread) {
                // it looks like that this reader instance is used
                // by multiple threads.
                throw new IllegalStateException("Attempt to use instance of " +
                                                this + " locked on thread " +
                                                theThread + " from thread " +
                                                currThread);
            } else {
                theLockCount ++;
            }
        } else {
            theThread = currThread;
            theLockCount = 1;
        }
    }

    private synchronized void clearThreadLock() {
        Thread currThread = Thread.currentThread();
        if (theThread == null || theThread != currThread) {
            throw new IllegalStateException("Attempt to clear thread lock form wrong thread. " +
                                            "Locked thread: " + theThread +
                                            "; current thread: " + currThread);
        }
        theLockCount --;
        if (theLockCount == 0) {
            theThread = null;
        }
    }

    private CallBackLock cbLock = new CallBackLock();

    private static class CallBackLock {

        private State lockState;

        CallBackLock() {
            lockState = State.Unlocked;
        }

        void check() {
            if (lockState != State.Unlocked) {
                throw new IllegalStateException("Access to the writer is not allowed");
            }
        }

        private void lock() {
            lockState = State.Locked;
        }

        private void unlock() {
            lockState = State.Unlocked;
        }

        private static enum State {
            Unlocked,
            Locked
        }
    }
}
