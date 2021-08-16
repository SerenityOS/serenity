/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.gif;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.IIOException;
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import com.sun.imageio.plugins.common.ReaderUtil;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SampleModel;

public class GIFImageReader extends ImageReader {

    // The current ImageInputStream source.
    ImageInputStream stream = null;

    // Per-stream settings

    // True if the file header including stream metadata has been read.
    boolean gotHeader = false;

    // Global metadata, read once per input setting.
    GIFStreamMetadata streamMetadata = null;

    // The current image index
    int currIndex = -1;

    // Metadata for image at 'currIndex', or null.
    GIFImageMetadata imageMetadata = null;

    // A List of Longs indicating the stream positions of the
    // start of the metadata for each image.  Entries are added
    // as needed.
    List<Long> imageStartPosition = new ArrayList<>();

    // Length of metadata for image at 'currIndex', valid only if
    // imageMetadata != null.
    int imageMetadataLength;

    // The number of images in the stream, if known, otherwise -1.
    int numImages = -1;

    // Variables used by the LZW decoding process
    byte[] block = new byte[255];
    int blockLength = 0;
    int bitPos = 0;
    int nextByte = 0;
    int initCodeSize;
    int clearCode;
    int eofCode;

    // 32-bit lookahead buffer
    int next32Bits = 0;

    // Try if the end of the data blocks has been found,
    // and we are simply draining the 32-bit buffer
    boolean lastBlockFound = false;

    // The image to be written.
    BufferedImage theImage = null;

    // The image's tile.
    WritableRaster theTile = null;

    // The image dimensions (from the stream).
    int width = -1, height = -1;

    // The pixel currently being decoded (in the stream's coordinates).
    int streamX = -1, streamY = -1;

    // The number of rows decoded
    int rowsDone = 0;

    // The current interlace pass, starting with 0.
    int interlacePass = 0;

    private byte[] fallbackColorTable = null;

    // End per-stream settings

    // Constants used to control interlacing.
    static final int[] interlaceIncrement = { 8, 8, 4, 2, -1 };
    static final int[] interlaceOffset = { 0, 4, 2, 1, -1 };

    public GIFImageReader(ImageReaderSpi originatingProvider) {
        super(originatingProvider);
    }

    // Take input from an ImageInputStream
    @Override
    public void setInput(Object input,
                         boolean seekForwardOnly,
                         boolean ignoreMetadata) {
        super.setInput(input, seekForwardOnly, ignoreMetadata);
        if (input != null) {
            if (!(input instanceof ImageInputStream)) {
                throw new IllegalArgumentException
                    ("input not an ImageInputStream!");
            }
            this.stream = (ImageInputStream)input;
        } else {
            this.stream = null;
        }

        // Clear all values based on the previous stream contents
        resetStreamSettings();
    }

    @Override
    public int getNumImages(boolean allowSearch) throws IIOException {
        if (stream == null) {
            throw new IllegalStateException("Input not set!");
        }
        if (seekForwardOnly && allowSearch) {
            throw new IllegalStateException
                ("seekForwardOnly and allowSearch can't both be true!");
        }

        if (numImages > 0) {
            return numImages;
        }
        if (allowSearch) {
            this.numImages = locateImage(Integer.MAX_VALUE) + 1;
        }
        return numImages;
    }

    // Throw an IndexOutOfBoundsException if index < minIndex,
    // and bump minIndex if required.
    private void checkIndex(int imageIndex) {
        if (imageIndex < minIndex) {
            throw new IndexOutOfBoundsException("imageIndex < minIndex!");
        }
        if (seekForwardOnly) {
            minIndex = imageIndex;
        }
    }

    @Override
    public int getWidth(int imageIndex) throws IIOException {
        checkIndex(imageIndex);

        int index = locateImage(imageIndex);
        if (index != imageIndex) {
            throw new IndexOutOfBoundsException();
        }
        readMetadata();
        return imageMetadata.imageWidth;
    }

    @Override
    public int getHeight(int imageIndex) throws IIOException {
        checkIndex(imageIndex);

        int index = locateImage(imageIndex);
        if (index != imageIndex) {
            throw new IndexOutOfBoundsException();
        }
        readMetadata();
        return imageMetadata.imageHeight;
    }

    // We don't check all parameters as ImageTypeSpecifier.createIndexed do
    // since this method is private and we pass consistent data here
    private ImageTypeSpecifier createIndexed(byte[] r, byte[] g, byte[] b,
                                             int bits) {
        ColorModel colorModel;
        if (imageMetadata.transparentColorFlag) {
            // Some files erroneously have a transparent color index
            // of 255 even though there are fewer than 256 colors.
            int idx = Math.min(imageMetadata.transparentColorIndex,
                    r.length - 1);
            colorModel = new IndexColorModel(bits, r.length, r, g, b, idx);
        } else {
            colorModel = new IndexColorModel(bits, r.length, r, g, b);
        }

        SampleModel sampleModel;
        if (bits == 8) {
            int[] bandOffsets = {0};
            sampleModel =
                    new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
                    1, 1, 1, 1,
                    bandOffsets);
        } else {
            sampleModel =
                    new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE,
                    1, 1, bits);
        }
        return new ImageTypeSpecifier(colorModel, sampleModel);
    }

    @Override
    public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex)
            throws IIOException {
        checkIndex(imageIndex);

        int index = locateImage(imageIndex);
        if (index != imageIndex) {
            throw new IndexOutOfBoundsException();
        }
        readMetadata();

        List<ImageTypeSpecifier> l = new ArrayList<>(1);

        byte[] colorTable;
        if (imageMetadata.localColorTable != null) {
            colorTable = imageMetadata.localColorTable;
            fallbackColorTable = imageMetadata.localColorTable;
        } else {
            colorTable = streamMetadata.globalColorTable;
        }

        if (colorTable == null) {
            if (fallbackColorTable == null) {
                this.processWarningOccurred("Use default color table.");

                // no color table, the spec allows to use any palette.
                fallbackColorTable = getDefaultPalette();
            }

            colorTable = fallbackColorTable;
        }

        // Normalize color table length to 2^1, 2^2, 2^4, or 2^8
        int length = colorTable.length/3;
        int bits;
        if (length == 2) {
            bits = 1;
        } else if (length == 4) {
            bits = 2;
        } else if (length == 8 || length == 16) {
            // Bump from 3 to 4 bits
            bits = 4;
        } else {
            // Bump to 8 bits
            bits = 8;
        }
        int lutLength = 1 << bits;
        byte[] r = new byte[lutLength];
        byte[] g = new byte[lutLength];
        byte[] b = new byte[lutLength];

        // Entries from length + 1 to lutLength - 1 will be 0
        int rgbIndex = 0;
        for (int i = 0; i < length; i++) {
            r[i] = colorTable[rgbIndex++];
            g[i] = colorTable[rgbIndex++];
            b[i] = colorTable[rgbIndex++];
        }

        l.add(createIndexed(r, g, b, bits));
        return l.iterator();
    }

    @Override
    public ImageReadParam getDefaultReadParam() {
        return new ImageReadParam();
    }

    @Override
    public IIOMetadata getStreamMetadata() throws IIOException {
        readHeader();
        return streamMetadata;
    }

    @Override
    public IIOMetadata getImageMetadata(int imageIndex) throws IIOException {
        checkIndex(imageIndex);

        int index = locateImage(imageIndex);
        if (index != imageIndex) {
            throw new IndexOutOfBoundsException("Bad image index!");
        }
        readMetadata();
        return imageMetadata;
    }

    // BEGIN LZW STUFF

    private void initNext32Bits() {
        next32Bits = block[0] & 0xff;
        next32Bits |= (block[1] & 0xff) << 8;
        next32Bits |= (block[2] & 0xff) << 16;
        next32Bits |= block[3] << 24;
        nextByte = 4;
    }

    // Load a block (1-255 bytes) at a time, and maintain
    // a 32-bit lookahead buffer that is filled from the left
    // and extracted from the right.
    //
    // When the last block is found, we continue to
    //
    private int getCode(int codeSize, int codeMask) throws IOException {
        if (bitPos + codeSize > 32) {
            return eofCode; // No more data available
        }

        int code = (next32Bits >> bitPos) & codeMask;
        bitPos += codeSize;

        // Shift in a byte of new data at a time
        while (bitPos >= 8 && !lastBlockFound) {
            next32Bits >>>= 8;
            bitPos -= 8;

            // Check if current block is out of bytes
            if (nextByte >= blockLength) {
                // Get next block size
                blockLength = stream.readUnsignedByte();
                if (blockLength == 0) {
                    lastBlockFound = true;
                    return code;
                } else {
                    int left = blockLength;
                    int off = 0;
                    while (left > 0) {
                        int nbytes = stream.read(block, off, left);
                        if (nbytes == -1) {
                            throw new IIOException("Invalid block length for " +
                                    "LZW encoded image data");
                        }
                        off += nbytes;
                        left -= nbytes;
                    }
                    nextByte = 0;
                }
            }

            next32Bits |= block[nextByte++] << 24;
        }

        return code;
    }

    public void initializeStringTable(int[] prefix,
                                      byte[] suffix,
                                      byte[] initial,
                                      int[] length) {
        int numEntries = 1 << initCodeSize;
        for (int i = 0; i < numEntries; i++) {
            prefix[i] = -1;
            suffix[i] = (byte)i;
            initial[i] = (byte)i;
            length[i] = 1;
        }

        // Fill in the entire table for robustness against
        // out-of-sequence codes.
        for (int i = numEntries; i < 4096; i++) {
            prefix[i] = -1;
            length[i] = 1;
        }

        // tableIndex = numEntries + 2;
        // codeSize = initCodeSize + 1;
        // codeMask = (1 << codeSize) - 1;
    }

    Rectangle sourceRegion;
    int sourceXSubsampling;
    int sourceYSubsampling;
    int sourceMinProgressivePass;
    int sourceMaxProgressivePass;

    Point destinationOffset;
    Rectangle destinationRegion;

    // Used only if IIOReadUpdateListeners are present
    int updateMinY;
    int updateYStep;

    boolean decodeThisRow = true;
    int destY = 0;

    byte[] rowBuf;

    private void outputRow() {
        // Clip against ImageReadParam
        int width = Math.min(sourceRegion.width,
                             destinationRegion.width*sourceXSubsampling);
        int destX = destinationRegion.x;

        if (sourceXSubsampling == 1) {
            theTile.setDataElements(destX, destY, width, 1, rowBuf);
        } else {
            for (int x = 0; x < width; x += sourceXSubsampling, destX++) {
                theTile.setSample(destX, destY, 0, rowBuf[x] & 0xff);
            }
        }

        // Update IIOReadUpdateListeners, if any
        if (updateListeners != null) {
            int[] bands = { 0 };
            // updateYStep will have been initialized if
            // updateListeners is non-null
            processImageUpdate(theImage,
                               destX, destY,
                               width, 1, 1, updateYStep,
                               bands);
        }
    }

    private void computeDecodeThisRow() {
        this.decodeThisRow =
            (destY < destinationRegion.y + destinationRegion.height) &&
            (streamY >= sourceRegion.y) &&
            (streamY < sourceRegion.y + sourceRegion.height) &&
            (((streamY - sourceRegion.y) % sourceYSubsampling) == 0);
    }

    private void outputPixels(byte[] string, int len) {
        if (interlacePass < sourceMinProgressivePass ||
            interlacePass > sourceMaxProgressivePass) {
            return;
        }

        for (int i = 0; i < len; i++) {
            if (streamX >= sourceRegion.x) {
                rowBuf[streamX - sourceRegion.x] = string[i];
            }

            // Process end-of-row
            ++streamX;
            if (streamX == width) {
                // Update IIOReadProgressListeners
                ++rowsDone;
                processImageProgress(100.0F*rowsDone/height);
                if (abortRequested()) {
                    return;
                }

                if (decodeThisRow) {
                    outputRow();
                }

                streamX = 0;
                if (imageMetadata.interlaceFlag) {
                    streamY += interlaceIncrement[interlacePass];
                    if (streamY >= height) {
                        // Inform IIOReadUpdateListeners of end of pass
                        if (updateListeners != null) {
                            processPassComplete(theImage);
                        }

                        ++interlacePass;
                        if (interlacePass > sourceMaxProgressivePass) {
                            return;
                        }
                        streamY = interlaceOffset[interlacePass];
                        startPass(interlacePass);
                    }
                } else {
                    ++streamY;
                }

                // Determine whether pixels from this row will
                // be written to the destination
                this.destY = destinationRegion.y +
                    (streamY - sourceRegion.y)/sourceYSubsampling;
                computeDecodeThisRow();
            }
        }
    }

    // END LZW STUFF

    private void readHeader() throws IIOException {
        if (gotHeader) {
            return;
        }
        if (stream == null) {
            throw new IllegalStateException("Input not set!");
        }

        // Create an object to store the stream metadata
        this.streamMetadata = new GIFStreamMetadata();

        try {
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);

            byte[] signature = new byte[6];
            stream.readFully(signature);

            StringBuilder version = new StringBuilder(3);
            version.append((char)signature[3]);
            version.append((char)signature[4]);
            version.append((char)signature[5]);
            streamMetadata.version = version.toString();

            streamMetadata.logicalScreenWidth = stream.readUnsignedShort();
            streamMetadata.logicalScreenHeight = stream.readUnsignedShort();

            int packedFields = stream.readUnsignedByte();
            boolean globalColorTableFlag = (packedFields & 0x80) != 0;
            streamMetadata.colorResolution = ((packedFields >> 4) & 0x7) + 1;
            streamMetadata.sortFlag = (packedFields & 0x8) != 0;
            int numGCTEntries = 1 << ((packedFields & 0x7) + 1);

            streamMetadata.backgroundColorIndex = stream.readUnsignedByte();
            streamMetadata.pixelAspectRatio = stream.readUnsignedByte();

            if (globalColorTableFlag) {
                streamMetadata.globalColorTable = new byte[3*numGCTEntries];
                stream.readFully(streamMetadata.globalColorTable);
            } else {
                streamMetadata.globalColorTable = null;
            }

            // Found position of metadata for image 0
            imageStartPosition.add(Long.valueOf(stream.getStreamPosition()));
        } catch (IOException e) {
            throw new IIOException("I/O error reading header!", e);
        }

        gotHeader = true;
    }

    private boolean skipImage() throws IIOException {
        // Stream must be at the beginning of an image descriptor
        // upon exit

        try {
            while (true) {
                int blockType = stream.readUnsignedByte();

                if (blockType == 0x2c) {
                    stream.skipBytes(8);

                    int packedFields = stream.readUnsignedByte();
                    if ((packedFields & 0x80) != 0) {
                        // Skip color table if any
                        int bits = (packedFields & 0x7) + 1;
                        stream.skipBytes(3*(1 << bits));
                    }

                    stream.skipBytes(1);

                    int length = 0;
                    do {
                        length = stream.readUnsignedByte();
                        stream.skipBytes(length);
                    } while (length > 0);

                    return true;
                } else if (blockType == 0x3b) {
                    return false;
                } else if (blockType == 0x21) {
                    int label = stream.readUnsignedByte();

                    int length = 0;
                    do {
                        length = stream.readUnsignedByte();
                        stream.skipBytes(length);
                    } while (length > 0);
                } else if (blockType == 0x0) {
                    // EOF
                    return false;
                } else {
                    int length = 0;
                    do {
                        length = stream.readUnsignedByte();
                        stream.skipBytes(length);
                    } while (length > 0);
                }
            }
        } catch (EOFException e) {
            return false;
        } catch (IOException e) {
            throw new IIOException("I/O error locating image!", e);
        }
    }

    private int locateImage(int imageIndex) throws IIOException {
        readHeader();

        try {
            // Find closest known index
            int index = Math.min(imageIndex, imageStartPosition.size() - 1);

            // Seek to that position
            Long l = imageStartPosition.get(index);
            stream.seek(l.longValue());

            // Skip images until at desired index or last image found
            while (index < imageIndex) {
                if (!skipImage()) {
                    --index;
                    return index;
                }

                Long l1 = stream.getStreamPosition();
                imageStartPosition.add(l1);
                ++index;
            }
        } catch (IOException e) {
            throw new IIOException("Couldn't seek!", e);
        }

        if (currIndex != imageIndex) {
            imageMetadata = null;
        }
        currIndex = imageIndex;
        return imageIndex;
    }

    // Read blocks of 1-255 bytes, stop at a 0-length block
    private byte[] concatenateBlocks() throws IOException {
        byte[] data = new byte[0];
        while (true) {
            int length = stream.readUnsignedByte();
            if (length == 0) {
                break;
            }
            byte[] newData = new byte[data.length + length];
            System.arraycopy(data, 0, newData, 0, data.length);
            stream.readFully(newData, data.length, length);
            data = newData;
        }

        return data;
    }

    // Stream must be positioned at start of metadata for 'currIndex'
    private void readMetadata() throws IIOException {
        if (stream == null) {
            throw new IllegalStateException("Input not set!");
        }

        try {
            // Create an object to store the image metadata
            this.imageMetadata = new GIFImageMetadata();

            long startPosition = stream.getStreamPosition();
            while (true) {
                int blockType = stream.readUnsignedByte();
                if (blockType == 0x2c) { // Image Descriptor
                    imageMetadata.imageLeftPosition =
                        stream.readUnsignedShort();
                    imageMetadata.imageTopPosition =
                        stream.readUnsignedShort();
                    imageMetadata.imageWidth = stream.readUnsignedShort();
                    imageMetadata.imageHeight = stream.readUnsignedShort();

                    int idPackedFields = stream.readUnsignedByte();
                    boolean localColorTableFlag =
                        (idPackedFields & 0x80) != 0;
                    imageMetadata.interlaceFlag = (idPackedFields & 0x40) != 0;
                    imageMetadata.sortFlag = (idPackedFields & 0x20) != 0;
                    int numLCTEntries = 1 << ((idPackedFields & 0x7) + 1);

                    if (localColorTableFlag) {
                        // Read color table if any
                        imageMetadata.localColorTable =
                            new byte[3*numLCTEntries];
                        stream.readFully(imageMetadata.localColorTable);
                    } else {
                        imageMetadata.localColorTable = null;
                    }

                    // Record length of this metadata block
                    this.imageMetadataLength =
                        (int)(stream.getStreamPosition() - startPosition);

                    // Now positioned at start of LZW-compressed pixels
                    return;
                } else if (blockType == 0x21) { // Extension block
                    int label = stream.readUnsignedByte();

                    if (label == 0xf9) { // Graphics Control Extension
                        int gceLength = stream.readUnsignedByte(); // 4
                        int gcePackedFields = stream.readUnsignedByte();
                        imageMetadata.disposalMethod =
                            (gcePackedFields >> 2) & 0x3;
                        imageMetadata.userInputFlag =
                            (gcePackedFields & 0x2) != 0;
                        imageMetadata.transparentColorFlag =
                            (gcePackedFields & 0x1) != 0;

                        imageMetadata.delayTime = stream.readUnsignedShort();
                        imageMetadata.transparentColorIndex
                            = stream.readUnsignedByte();

                        int terminator = stream.readUnsignedByte();
                    } else if (label == 0x1) { // Plain text extension
                        int length = stream.readUnsignedByte();
                        imageMetadata.hasPlainTextExtension = true;
                        imageMetadata.textGridLeft =
                            stream.readUnsignedShort();
                        imageMetadata.textGridTop =
                            stream.readUnsignedShort();
                        imageMetadata.textGridWidth =
                            stream.readUnsignedShort();
                        imageMetadata.textGridHeight =
                            stream.readUnsignedShort();
                        imageMetadata.characterCellWidth =
                            stream.readUnsignedByte();
                        imageMetadata.characterCellHeight =
                            stream.readUnsignedByte();
                        imageMetadata.textForegroundColor =
                            stream.readUnsignedByte();
                        imageMetadata.textBackgroundColor =
                            stream.readUnsignedByte();
                        imageMetadata.text = concatenateBlocks();
                    } else if (label == 0xfe) { // Comment extension
                        byte[] comment = concatenateBlocks();
                        if (imageMetadata.comments == null) {
                            imageMetadata.comments = new ArrayList<>();
                        }
                        imageMetadata.comments.add(comment);
                    } else if (label == 0xff) { // Application extension
                        int blockSize = stream.readUnsignedByte();
                        byte[] applicationID = new byte[8];
                        byte[] authCode = new byte[3];

                        // read available data
                        byte[] blockData = new byte[blockSize];
                        stream.readFully(blockData);

                        int offset = copyData(blockData, 0, applicationID);
                        offset = copyData(blockData, offset, authCode);

                        byte[] applicationData = concatenateBlocks();

                        if (offset < blockSize) {
                            int len = blockSize - offset;
                            byte[] data =
                                new byte[len + applicationData.length];

                            System.arraycopy(blockData, offset, data, 0, len);
                            System.arraycopy(applicationData, 0, data, len,
                                             applicationData.length);

                            applicationData = data;
                        }

                        // Init lists if necessary
                        if (imageMetadata.applicationIDs == null) {
                            imageMetadata.applicationIDs = new ArrayList<>();
                            imageMetadata.authenticationCodes =
                                new ArrayList<>();
                            imageMetadata.applicationData = new ArrayList<>();
                        }
                        imageMetadata.applicationIDs.add(applicationID);
                        imageMetadata.authenticationCodes.add(authCode);
                        imageMetadata.applicationData.add(applicationData);
                    } else {
                        // Skip over unknown extension blocks
                        int length = 0;
                        do {
                            length = stream.readUnsignedByte();
                            stream.skipBytes(length);
                        } while (length > 0);
                    }
                } else if (blockType == 0x3b) { // Trailer
                    throw new IndexOutOfBoundsException
                        ("Attempt to read past end of image sequence!");
                } else {
                    throw new IIOException("Unexpected block type " +
                                           blockType + "!");
                }
            }
        } catch (IIOException iioe) {
            throw iioe;
        } catch (IOException ioe) {
            throw new IIOException("I/O error reading image metadata!", ioe);
        }
    }

    private int copyData(byte[] src, int offset, byte[] dst) {
        int len = dst.length;
        int rest = src.length - offset;
        if (len > rest) {
            len = rest;
        }
        System.arraycopy(src, offset, dst, 0, len);
        return offset + len;
    }

    private void startPass(int pass) {
        if (updateListeners == null || !imageMetadata.interlaceFlag) {
            return;
        }

        int y = interlaceOffset[interlacePass];
        int yStep = interlaceIncrement[interlacePass];

        int[] vals = ReaderUtil.
            computeUpdatedPixels(sourceRegion,
                                 destinationOffset,
                                 destinationRegion.x,
                                 destinationRegion.y,
                                 destinationRegion.x +
                                 destinationRegion.width - 1,
                                 destinationRegion.y +
                                 destinationRegion.height - 1,
                                 sourceXSubsampling,
                                 sourceYSubsampling,
                                 0,
                                 y,
                                 destinationRegion.width,
                                 (destinationRegion.height + yStep - 1)/yStep,
                                 1,
                                 yStep);

        // Initialized updateMinY and updateYStep
        this.updateMinY = vals[1];
        this.updateYStep = vals[5];

        // Inform IIOReadUpdateListeners of new pass
        int[] bands = { 0 };

        processPassStarted(theImage,
                           interlacePass,
                           sourceMinProgressivePass,
                           sourceMaxProgressivePass,
                           0,
                           updateMinY,
                           1,
                           updateYStep,
                           bands);
    }

    @Override
    public BufferedImage read(int imageIndex, ImageReadParam param)
        throws IIOException {
        if (stream == null) {
            throw new IllegalStateException("Input not set!");
        }
        checkIndex(imageIndex);

        int index = locateImage(imageIndex);
        if (index != imageIndex) {
            throw new IndexOutOfBoundsException("imageIndex out of bounds!");
        }

        readMetadata();

        // A null ImageReadParam means we use the default
        if (param == null) {
            param = getDefaultReadParam();
        }

        // Initialize the destination image
        Iterator<ImageTypeSpecifier> imageTypes = getImageTypes(imageIndex);
        this.theImage = getDestination(param,
                                       imageTypes,
                                       imageMetadata.imageWidth,
                                       imageMetadata.imageHeight);
        this.theTile = theImage.getWritableTile(0, 0);
        this.width = imageMetadata.imageWidth;
        this.height = imageMetadata.imageHeight;
        this.streamX = 0;
        this.streamY = 0;
        this.rowsDone = 0;
        this.interlacePass = 0;

        // Get source region, taking subsampling offsets into account,
        // and clipping against the true source bounds

        this.sourceRegion = new Rectangle(0, 0, 0, 0);
        this.destinationRegion = new Rectangle(0, 0, 0, 0);
        computeRegions(param, width, height, theImage,
                       sourceRegion, destinationRegion);
        this.destinationOffset = new Point(destinationRegion.x,
                                           destinationRegion.y);

        this.sourceXSubsampling = param.getSourceXSubsampling();
        this.sourceYSubsampling = param.getSourceYSubsampling();
        this.sourceMinProgressivePass =
            Math.max(param.getSourceMinProgressivePass(), 0);
        this.sourceMaxProgressivePass =
            Math.min(param.getSourceMaxProgressivePass(), 3);

        this.destY = destinationRegion.y +
            (streamY - sourceRegion.y)/sourceYSubsampling;
        computeDecodeThisRow();

        clearAbortRequest();
        // Inform IIOReadProgressListeners of start of image
        processImageStarted(imageIndex);
        if (abortRequested()) {
            processReadAborted();
            return theImage;
        }
        startPass(0);

        this.rowBuf = new byte[width];

        try {
            // Read and decode the image data, fill in theImage
            this.initCodeSize = stream.readUnsignedByte();
            // GIF allows max 8 bpp, so anything larger is bogus for the roots.
            if (this.initCodeSize < 1 || this.initCodeSize > 8) {
                throw new IIOException("Bad code size:" + this.initCodeSize);
            }

            // Read first data block
            this.blockLength = stream.readUnsignedByte();
            int left = blockLength;
            int off = 0;
            while (left > 0) {
                int nbytes = stream.read(block, off, left);
                if (nbytes == -1) {
                    throw new IIOException("Invalid block length for " +
                            "LZW encoded image data");
                }
                left -= nbytes;
                off += nbytes;
            }

            this.bitPos = 0;
            this.nextByte = 0;
            this.lastBlockFound = false;
            this.interlacePass = 0;

            // Init 32-bit buffer
            initNext32Bits();

            this.clearCode = 1 << initCodeSize;
            this.eofCode = clearCode + 1;

            int code, oldCode = 0;

            int[] prefix = new int[4096];
            byte[] suffix = new byte[4096];
            byte[] initial = new byte[4096];
            int[] length = new int[4096];
            byte[] string = new byte[4096];

            initializeStringTable(prefix, suffix, initial, length);
            int tableIndex = (1 << initCodeSize) + 2;
            int codeSize = initCodeSize + 1;
            int codeMask = (1 << codeSize) - 1;

            do {
                code = getCode(codeSize, codeMask);

                if (code == clearCode) {
                    initializeStringTable(prefix, suffix, initial, length);
                    tableIndex = (1 << initCodeSize) + 2;
                    codeSize = initCodeSize + 1;
                    codeMask = (1 << codeSize) - 1;

                    code = getCode(codeSize, codeMask);
                    if (code == eofCode) {
                        // Inform IIOReadProgressListeners of end of image
                        processImageComplete();
                        return theImage;
                    }
                } else if (code == eofCode) {
                    // Inform IIOReadProgressListeners of end of image
                    processImageComplete();
                    return theImage;
                } else {
                    int newSuffixIndex;
                    if (code < tableIndex) {
                        newSuffixIndex = code;
                    } else { // code == tableIndex
                        newSuffixIndex = oldCode;
                        if (code != tableIndex) {
                            // warning - code out of sequence
                            // possibly data corruption
                            processWarningOccurred("Out-of-sequence code!");
                        }
                    }

                    int ti = tableIndex;
                    int oc = oldCode;

                    prefix[ti] = oc;
                    suffix[ti] = initial[newSuffixIndex];
                    initial[ti] = initial[oc];
                    length[ti] = length[oc] + 1;

                    ++tableIndex;
                    if ((tableIndex == (1 << codeSize)) &&
                        (tableIndex < 4096)) {
                        ++codeSize;
                        codeMask = (1 << codeSize) - 1;
                    }
                }

                // Reverse code
                int c = code;
                int len = length[c];
                for (int i = len - 1; i >= 0; i--) {
                    string[i] = suffix[c];
                    c = prefix[c];
                }

                outputPixels(string, len);
                oldCode = code;
            } while (!abortRequested());

            processReadAborted();
            return theImage;
        } catch (IOException e) {
            throw new IIOException("I/O error reading image!", e);
        }
    }

    /**
     * Remove all settings including global settings such as
     * {@code Locale}s and listeners, as well as stream settings.
     */
    @Override
    public void reset() {
        super.reset();
        resetStreamSettings();
    }

    /**
     * Remove local settings based on parsing of a stream.
     */
    private void resetStreamSettings() {
        gotHeader = false;
        streamMetadata = null;
        currIndex = -1;
        imageMetadata = null;
        imageStartPosition = new ArrayList<>();
        numImages = -1;

        // No need to reinitialize 'block'
        blockLength = 0;
        bitPos = 0;
        nextByte = 0;

        next32Bits = 0;
        lastBlockFound = false;

        theImage = null;
        theTile = null;
        width = -1;
        height = -1;
        streamX = -1;
        streamY = -1;
        rowsDone = 0;
        interlacePass = 0;

        fallbackColorTable = null;
    }

    private static byte[] defaultPalette = null;

    private static synchronized byte[] getDefaultPalette() {
        if (defaultPalette == null) {
            BufferedImage img = new BufferedImage(1, 1,
                    BufferedImage.TYPE_BYTE_INDEXED);
            IndexColorModel icm = (IndexColorModel) img.getColorModel();

            final int size = icm.getMapSize();
            byte[] r = new byte[size];
            byte[] g = new byte[size];
            byte[] b = new byte[size];
            icm.getReds(r);
            icm.getGreens(g);
            icm.getBlues(b);

            defaultPalette = new byte[size * 3];

            for (int i = 0; i < size; i++) {
                defaultPalette[3 * i + 0] = r[i];
                defaultPalette[3 * i + 1] = g[i];
                defaultPalette[3 * i + 2] = b[i];
            }
        }
        return defaultPalette;
    }
}
