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

package com.sun.imageio.plugins.png;

import java.awt.Rectangle;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.ImageOutputStreamImpl;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

final class CRC {

    private static final int[] crcTable = new int[256];
    private int crc = 0xffffffff;

    static {
        // Initialize CRC table
        for (int n = 0; n < 256; n++) {
            int c = n;
            for (int k = 0; k < 8; k++) {
                if ((c & 1) == 1) {
                    c = 0xedb88320 ^ (c >>> 1);
                } else {
                    c >>>= 1;
                }

                crcTable[n] = c;
            }
        }
    }

    CRC() {}

    void reset() {
        crc = 0xffffffff;
    }

    void update(byte[] data, int off, int len) {
        int c = crc;
        for (int n = 0; n < len; n++) {
            c = crcTable[(c ^ data[off + n]) & 0xff] ^ (c >>> 8);
        }
        crc = c;
    }

    void update(int data) {
        crc = crcTable[(crc ^ data) & 0xff] ^ (crc >>> 8);
    }

    int getValue() {
        return crc ^ 0xffffffff;
    }
}


final class ChunkStream extends ImageOutputStreamImpl {

    private final ImageOutputStream stream;
    private final long startPos;
    private final CRC crc = new CRC();

    ChunkStream(int type, ImageOutputStream stream) throws IOException {
        this.stream = stream;
        this.startPos = stream.getStreamPosition();

        stream.writeInt(-1); // length, will backpatch
        writeInt(type);
    }

    @Override
    public int read() throws IOException {
        throw new RuntimeException("Method not available");
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        throw new RuntimeException("Method not available");
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        crc.update(b, off, len);
        stream.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        crc.update(b);
        stream.write(b);
    }

    void finish() throws IOException {
        // Write CRC
        stream.writeInt(crc.getValue());

        // Write length
        long pos = stream.getStreamPosition();
        stream.seek(startPos);
        stream.writeInt((int)(pos - startPos) - 12);

        // Return to end of chunk and flush to minimize buffering
        stream.seek(pos);
        stream.flushBefore(pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        // Empty finalizer (for improved performance; no need to call
        // super.finalize() in this case)
    }
}

// Compress output and write as a series of 'IDAT' chunks of
// fixed length.
final class IDATOutputStream extends ImageOutputStreamImpl {

    private static final byte[] chunkType = {
        (byte)'I', (byte)'D', (byte)'A', (byte)'T'
    };

    private final ImageOutputStream stream;
    private final int chunkLength;
    private long startPos;
    private final CRC crc = new CRC();

    private final Deflater def;
    private final byte[] buf = new byte[512];
    // reused 1 byte[] array:
    private final byte[] wbuf1 = new byte[1];

    private int bytesRemaining;

    IDATOutputStream(ImageOutputStream stream, int chunkLength,
                            int deflaterLevel) throws IOException
    {
        this.stream = stream;
        this.chunkLength = chunkLength;
        this.def = new Deflater(deflaterLevel);

        startChunk();
    }

    private void startChunk() throws IOException {
        crc.reset();
        this.startPos = stream.getStreamPosition();
        stream.writeInt(-1); // length, will backpatch

        crc.update(chunkType, 0, 4);
        stream.write(chunkType, 0, 4);

        this.bytesRemaining = chunkLength;
    }

    private void finishChunk() throws IOException {
        // Write CRC
        stream.writeInt(crc.getValue());

        // Write length
        long pos = stream.getStreamPosition();
        stream.seek(startPos);
        stream.writeInt((int)(pos - startPos) - 12);

        // Return to end of chunk and flush to minimize buffering
        stream.seek(pos);
        try {
            stream.flushBefore(pos);
        } catch (IOException e) {
            /*
             * If flushBefore() fails we try to access startPos in finally
             * block of write_IDAT(). We should update startPos to avoid
             * IndexOutOfBoundException while seek() is happening.
             */
            this.startPos = stream.getStreamPosition();
            throw e;
        }
    }

    @Override
    public int read() throws IOException {
        throw new RuntimeException("Method not available");
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        throw new RuntimeException("Method not available");
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return;
        }

        if (!def.finished()) {
            def.setInput(b, off, len);
            while (!def.needsInput()) {
                deflate();
            }
        }
    }

    void deflate() throws IOException {
        int len = def.deflate(buf, 0, buf.length);
        int off = 0;

        while (len > 0) {
            if (bytesRemaining == 0) {
                finishChunk();
                startChunk();
            }

            int nbytes = Math.min(len, bytesRemaining);
            crc.update(buf, off, nbytes);
            stream.write(buf, off, nbytes);

            off += nbytes;
            len -= nbytes;
            bytesRemaining -= nbytes;
        }
    }

    @Override
    public void write(int b) throws IOException {
        wbuf1[0] = (byte)b;
        write(wbuf1, 0, 1);
    }

    void finish() throws IOException {
        try {
            if (!def.finished()) {
                def.finish();
                while (!def.finished()) {
                    deflate();
                }
            }
            finishChunk();
        } finally {
            def.end();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        // Empty finalizer (for improved performance; no need to call
        // super.finalize() in this case)
    }
}


final class PNGImageWriteParam extends ImageWriteParam {

    /** Default quality level = 0.5 ie medium compression */
    private static final float DEFAULT_QUALITY = 0.5f;

    private static final String[] compressionNames = {"Deflate"};
    private static final float[] qualityVals = { 0.00F, 0.30F, 0.75F, 1.00F };
    private static final String[] qualityDescs = {
        "High compression",   // 0.00 -> 0.30
        "Medium compression", // 0.30 -> 0.75
        "Low compression"     // 0.75 -> 1.00
    };

    PNGImageWriteParam(Locale locale) {
        super();
        this.canWriteProgressive = true;
        this.locale = locale;
        this.canWriteCompressed = true;
        this.compressionTypes = compressionNames;
        this.compressionType = compressionTypes[0];
        this.compressionMode = MODE_DEFAULT;
        this.compressionQuality = DEFAULT_QUALITY;
    }

    /**
     * Removes any previous compression quality setting.
     *
     * <p> The default implementation resets the compression quality
     * to <code>0.5F</code>.
     *
     * @exception IllegalStateException if the compression mode is not
     * <code>MODE_EXPLICIT</code>.
     */
    @Override
    public void unsetCompression() {
        super.unsetCompression();
        this.compressionType = compressionTypes[0];
        this.compressionQuality = DEFAULT_QUALITY;
    }

    /**
     * Returns <code>true</code> since the PNG plug-in only supports
     * lossless compression.
     *
     * @return <code>true</code>.
     */
    @Override
    public boolean isCompressionLossless() {
        return true;
    }

    @Override
    public String[] getCompressionQualityDescriptions() {
        super.getCompressionQualityDescriptions();
        return qualityDescs.clone();
    }

    @Override
    public float[] getCompressionQualityValues() {
        super.getCompressionQualityValues();
        return qualityVals.clone();
    }
}

/**
 */
public final class PNGImageWriter extends ImageWriter {

    /** Default compression level = 4 ie medium compression */
    private static final int DEFAULT_COMPRESSION_LEVEL = 4;

    ImageOutputStream stream = null;

    PNGMetadata metadata = null;

    // Factors from the ImageWriteParam
    int sourceXOffset = 0;
    int sourceYOffset = 0;
    int sourceWidth = 0;
    int sourceHeight = 0;
    int[] sourceBands = null;
    int periodX = 1;
    int periodY = 1;

    int numBands;
    int bpp;

    RowFilter rowFilter = new RowFilter();
    byte[] prevRow = null;
    byte[] currRow = null;
    byte[][] filteredRows = null;

    // Per-band scaling tables
    //
    // After the first call to initializeScaleTables, either scale and scale0
    // will be valid, or scaleh and scalel will be valid, but not both.
    //
    // The tables will be designed for use with a set of input but depths
    // given by sampleSize, and an output bit depth given by scalingBitDepth.
    //
    int[] sampleSize = null; // Sample size per band, in bits
    int scalingBitDepth = -1; // Output bit depth of the scaling tables

    // Tables for 1, 2, 4, or 8 bit output
    byte[][] scale = null; // 8 bit table
    byte[] scale0 = null; // equivalent to scale[0]

    // Tables for 16 bit output
    byte[][] scaleh = null; // High bytes of output
    byte[][] scalel = null; // Low bytes of output

    int totalPixels; // Total number of pixels to be written by write_IDAT
    int pixelsDone; // Running count of pixels written by write_IDAT

    public PNGImageWriter(ImageWriterSpi originatingProvider) {
        super(originatingProvider);
    }

    @Override
    public void setOutput(Object output) {
        super.setOutput(output);
        if (output != null) {
            if (!(output instanceof ImageOutputStream)) {
                throw new IllegalArgumentException("output not an ImageOutputStream!");
            }
            this.stream = (ImageOutputStream)output;
        } else {
            this.stream = null;
        }
    }

    @Override
    public ImageWriteParam getDefaultWriteParam() {
        return new PNGImageWriteParam(getLocale());
    }

    @Override
    public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
        return null;
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType,
                                               ImageWriteParam param) {
        PNGMetadata m = new PNGMetadata();
        m.initialize(imageType, imageType.getSampleModel().getNumBands());
        return m;
    }

    @Override
    public IIOMetadata convertStreamMetadata(IIOMetadata inData,
                                             ImageWriteParam param) {
        return null;
    }

    @Override
    public IIOMetadata convertImageMetadata(IIOMetadata inData,
                                            ImageTypeSpecifier imageType,
                                            ImageWriteParam param) {
        // TODO - deal with imageType
        if (inData instanceof PNGMetadata) {
            return (PNGMetadata)((PNGMetadata)inData).clone();
        } else {
            return new PNGMetadata(inData);
        }
    }

    private void write_magic() throws IOException {
        // Write signature
        byte[] magic = { (byte)137, 80, 78, 71, 13, 10, 26, 10 };
        stream.write(magic);
    }

    private void write_IHDR() throws IOException {
        // Write IHDR chunk
        ChunkStream cs = new ChunkStream(PNGImageReader.IHDR_TYPE, stream);
        cs.writeInt(metadata.IHDR_width);
        cs.writeInt(metadata.IHDR_height);
        cs.writeByte(metadata.IHDR_bitDepth);
        cs.writeByte(metadata.IHDR_colorType);
        if (metadata.IHDR_compressionMethod != 0) {
            throw new IIOException(
"Only compression method 0 is defined in PNG 1.1");
        }
        cs.writeByte(metadata.IHDR_compressionMethod);
        if (metadata.IHDR_filterMethod != 0) {
            throw new IIOException(
"Only filter method 0 is defined in PNG 1.1");
        }
        cs.writeByte(metadata.IHDR_filterMethod);
        if (metadata.IHDR_interlaceMethod < 0 ||
            metadata.IHDR_interlaceMethod > 1) {
            throw new IIOException(
"Only interlace methods 0 (node) and 1 (adam7) are defined in PNG 1.1");
        }
        cs.writeByte(metadata.IHDR_interlaceMethod);
        cs.finish();
    }

    private void write_cHRM() throws IOException {
        if (metadata.cHRM_present) {
            ChunkStream cs = new ChunkStream(PNGImageReader.cHRM_TYPE, stream);
            cs.writeInt(metadata.cHRM_whitePointX);
            cs.writeInt(metadata.cHRM_whitePointY);
            cs.writeInt(metadata.cHRM_redX);
            cs.writeInt(metadata.cHRM_redY);
            cs.writeInt(metadata.cHRM_greenX);
            cs.writeInt(metadata.cHRM_greenY);
            cs.writeInt(metadata.cHRM_blueX);
            cs.writeInt(metadata.cHRM_blueY);
            cs.finish();
        }
    }

    private void write_gAMA() throws IOException {
        if (metadata.gAMA_present) {
            ChunkStream cs = new ChunkStream(PNGImageReader.gAMA_TYPE, stream);
            cs.writeInt(metadata.gAMA_gamma);
            cs.finish();
        }
    }

    private void write_iCCP() throws IOException {
        if (metadata.iCCP_present) {
            ChunkStream cs = new ChunkStream(PNGImageReader.iCCP_TYPE, stream);
            if (metadata.iCCP_profileName.length() > 79) {
                throw new IIOException("iCCP profile name is longer than 79");
            }
            cs.writeBytes(metadata.iCCP_profileName);
            cs.writeByte(0); // null terminator

            cs.writeByte(metadata.iCCP_compressionMethod);
            cs.write(metadata.iCCP_compressedProfile);
            cs.finish();
        }
    }

    private void write_sBIT() throws IOException {
        if (metadata.sBIT_present) {
            ChunkStream cs = new ChunkStream(PNGImageReader.sBIT_TYPE, stream);
            int colorType = metadata.IHDR_colorType;
            if (metadata.sBIT_colorType != colorType) {
                processWarningOccurred(0,
"sBIT metadata has wrong color type.\n" +
"The chunk will not be written.");
                return;
            }

            if (colorType == PNGImageReader.PNG_COLOR_GRAY ||
                colorType == PNGImageReader.PNG_COLOR_GRAY_ALPHA) {
                cs.writeByte(metadata.sBIT_grayBits);
            } else if (colorType == PNGImageReader.PNG_COLOR_RGB ||
                       colorType == PNGImageReader.PNG_COLOR_PALETTE ||
                       colorType == PNGImageReader.PNG_COLOR_RGB_ALPHA) {
                cs.writeByte(metadata.sBIT_redBits);
                cs.writeByte(metadata.sBIT_greenBits);
                cs.writeByte(metadata.sBIT_blueBits);
            }

            if (colorType == PNGImageReader.PNG_COLOR_GRAY_ALPHA ||
                colorType == PNGImageReader.PNG_COLOR_RGB_ALPHA) {
                cs.writeByte(metadata.sBIT_alphaBits);
            }
            cs.finish();
        }
    }

    private void write_sRGB() throws IOException {
        if (metadata.sRGB_present) {
            ChunkStream cs = new ChunkStream(PNGImageReader.sRGB_TYPE, stream);
            cs.writeByte(metadata.sRGB_renderingIntent);
            cs.finish();
        }
    }

    private void write_PLTE() throws IOException {
        if (metadata.PLTE_present) {
            if (metadata.IHDR_colorType == PNGImageReader.PNG_COLOR_GRAY ||
              metadata.IHDR_colorType == PNGImageReader.PNG_COLOR_GRAY_ALPHA) {
                // PLTE cannot occur in a gray image

                processWarningOccurred(0,
"A PLTE chunk may not appear in a gray or gray alpha image.\n" +
"The chunk will not be written");
                return;
            }

            ChunkStream cs = new ChunkStream(PNGImageReader.PLTE_TYPE, stream);

            int numEntries = metadata.PLTE_red.length;
            byte[] palette = new byte[numEntries*3];
            int index = 0;
            for (int i = 0; i < numEntries; i++) {
                palette[index++] = metadata.PLTE_red[i];
                palette[index++] = metadata.PLTE_green[i];
                palette[index++] = metadata.PLTE_blue[i];
            }

            cs.write(palette);
            cs.finish();
        }
    }

    private void write_hIST() throws IOException, IIOException {
        if (metadata.hIST_present) {
            ChunkStream cs = new ChunkStream(PNGImageReader.hIST_TYPE, stream);

            if (!metadata.PLTE_present) {
                throw new IIOException("hIST chunk without PLTE chunk!");
            }

            cs.writeChars(metadata.hIST_histogram,
                          0, metadata.hIST_histogram.length);
            cs.finish();
        }
    }

    private void write_tRNS() throws IOException, IIOException {
        if (metadata.tRNS_present) {
            ChunkStream cs = new ChunkStream(PNGImageReader.tRNS_TYPE, stream);
            int colorType = metadata.IHDR_colorType;
            int chunkType = metadata.tRNS_colorType;

            // Special case: image is RGB and chunk is Gray
            // Promote chunk contents to RGB
            int chunkRed = metadata.tRNS_red;
            int chunkGreen = metadata.tRNS_green;
            int chunkBlue = metadata.tRNS_blue;
            if (colorType == PNGImageReader.PNG_COLOR_RGB &&
                chunkType == PNGImageReader.PNG_COLOR_GRAY) {
                chunkType = colorType;
                chunkRed = chunkGreen = chunkBlue =
                    metadata.tRNS_gray;
            }

            if (chunkType != colorType) {
                processWarningOccurred(0,
"tRNS metadata has incompatible color type.\n" +
"The chunk will not be written.");
                return;
            }

            if (colorType == PNGImageReader.PNG_COLOR_PALETTE) {
                if (!metadata.PLTE_present) {
                    throw new IIOException("tRNS chunk without PLTE chunk!");
                }
                cs.write(metadata.tRNS_alpha);
            } else if (colorType == PNGImageReader.PNG_COLOR_GRAY) {
                cs.writeShort(metadata.tRNS_gray);
            } else if (colorType == PNGImageReader.PNG_COLOR_RGB) {
                cs.writeShort(chunkRed);
                cs.writeShort(chunkGreen);
                cs.writeShort(chunkBlue);
            } else {
                throw new IIOException("tRNS chunk for color type 4 or 6!");
            }
            cs.finish();
        }
    }

    private void write_bKGD() throws IOException {
        if (metadata.bKGD_present) {
            ChunkStream cs = new ChunkStream(PNGImageReader.bKGD_TYPE, stream);
            int colorType = metadata.IHDR_colorType & 0x3;
            int chunkType = metadata.bKGD_colorType;

            int chunkRed = metadata.bKGD_red;
            int chunkGreen = metadata.bKGD_green;
            int chunkBlue = metadata.bKGD_blue;
            // Special case: image is RGB(A) and chunk is Gray
            // Promote chunk contents to RGB
            if (colorType == PNGImageReader.PNG_COLOR_RGB &&
                chunkType == PNGImageReader.PNG_COLOR_GRAY) {
                // Make a gray bKGD chunk look like RGB
                chunkType = colorType;
                chunkRed = chunkGreen = chunkBlue =
                    metadata.bKGD_gray;
            }

            // Ignore status of alpha in colorType
            if (chunkType != colorType) {
                processWarningOccurred(0,
"bKGD metadata has incompatible color type.\n" +
"The chunk will not be written.");
                return;
            }

            if (colorType == PNGImageReader.PNG_COLOR_PALETTE) {
                cs.writeByte(metadata.bKGD_index);
            } else if (colorType == PNGImageReader.PNG_COLOR_GRAY ||
                       colorType == PNGImageReader.PNG_COLOR_GRAY_ALPHA) {
                cs.writeShort(metadata.bKGD_gray);
            } else { // colorType == PNGImageReader.PNG_COLOR_RGB ||
                     // colorType == PNGImageReader.PNG_COLOR_RGB_ALPHA
                cs.writeShort(chunkRed);
                cs.writeShort(chunkGreen);
                cs.writeShort(chunkBlue);
            }
            cs.finish();
        }
    }

    private void write_pHYs() throws IOException {
        if (metadata.pHYs_present) {
            ChunkStream cs = new ChunkStream(PNGImageReader.pHYs_TYPE, stream);
            cs.writeInt(metadata.pHYs_pixelsPerUnitXAxis);
            cs.writeInt(metadata.pHYs_pixelsPerUnitYAxis);
            cs.writeByte(metadata.pHYs_unitSpecifier);
            cs.finish();
        }
    }

    private void write_sPLT() throws IOException {
        if (metadata.sPLT_present) {
            ChunkStream cs = new ChunkStream(PNGImageReader.sPLT_TYPE, stream);

            if (metadata.sPLT_paletteName.length() > 79) {
                throw new IIOException("sPLT palette name is longer than 79");
            }
            cs.writeBytes(metadata.sPLT_paletteName);
            cs.writeByte(0); // null terminator

            cs.writeByte(metadata.sPLT_sampleDepth);
            int numEntries = metadata.sPLT_red.length;

            if (metadata.sPLT_sampleDepth == 8) {
                for (int i = 0; i < numEntries; i++) {
                    cs.writeByte(metadata.sPLT_red[i]);
                    cs.writeByte(metadata.sPLT_green[i]);
                    cs.writeByte(metadata.sPLT_blue[i]);
                    cs.writeByte(metadata.sPLT_alpha[i]);
                    cs.writeShort(metadata.sPLT_frequency[i]);
                }
            } else { // sampleDepth == 16
                for (int i = 0; i < numEntries; i++) {
                    cs.writeShort(metadata.sPLT_red[i]);
                    cs.writeShort(metadata.sPLT_green[i]);
                    cs.writeShort(metadata.sPLT_blue[i]);
                    cs.writeShort(metadata.sPLT_alpha[i]);
                    cs.writeShort(metadata.sPLT_frequency[i]);
                }
            }
            cs.finish();
        }
    }

    private void write_tIME() throws IOException {
        if (metadata.tIME_present) {
            ChunkStream cs = new ChunkStream(PNGImageReader.tIME_TYPE, stream);
            cs.writeShort(metadata.tIME_year);
            cs.writeByte(metadata.tIME_month);
            cs.writeByte(metadata.tIME_day);
            cs.writeByte(metadata.tIME_hour);
            cs.writeByte(metadata.tIME_minute);
            cs.writeByte(metadata.tIME_second);
            cs.finish();
        }
    }

    private void write_tEXt() throws IOException {
        Iterator<String> keywordIter = metadata.tEXt_keyword.iterator();
        Iterator<String> textIter = metadata.tEXt_text.iterator();

        while (keywordIter.hasNext()) {
            ChunkStream cs = new ChunkStream(PNGImageReader.tEXt_TYPE, stream);
            String keyword = keywordIter.next();
            if (keyword.length() > 79) {
                throw new IIOException("tEXt keyword is longer than 79");
            }
            cs.writeBytes(keyword);
            cs.writeByte(0);

            String text = textIter.next();
            cs.writeBytes(text);
            cs.finish();
        }
    }

    private byte[] deflate(byte[] b) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(baos);
        dos.write(b);
        dos.close();
        return baos.toByteArray();
    }

    private void write_iTXt() throws IOException {
        Iterator<String> keywordIter = metadata.iTXt_keyword.iterator();
        Iterator<Boolean> flagIter = metadata.iTXt_compressionFlag.iterator();
        Iterator<Integer> methodIter = metadata.iTXt_compressionMethod.iterator();
        Iterator<String> languageIter = metadata.iTXt_languageTag.iterator();
        Iterator<String> translatedKeywordIter =
            metadata.iTXt_translatedKeyword.iterator();
        Iterator<String> textIter = metadata.iTXt_text.iterator();

        while (keywordIter.hasNext()) {
            ChunkStream cs = new ChunkStream(PNGImageReader.iTXt_TYPE, stream);

            String keyword = keywordIter.next();
            if (keyword.length() > 79) {
                throw new IIOException("iTXt keyword is longer than 79");
            }
            cs.writeBytes(keyword);
            cs.writeByte(0);

            Boolean compressed = flagIter.next();
            cs.writeByte(compressed ? 1 : 0);

            cs.writeByte(methodIter.next().intValue());

            cs.writeBytes(languageIter.next());
            cs.writeByte(0);

            cs.write(translatedKeywordIter.next().getBytes(UTF_8));
            cs.writeByte(0);

            String text = textIter.next();
            if (compressed) {
                cs.write(deflate(text.getBytes(UTF_8)));
            } else {
                cs.write(text.getBytes(UTF_8));
            }
            cs.finish();
        }
    }

    private void write_zTXt() throws IOException {
        Iterator<String> keywordIter = metadata.zTXt_keyword.iterator();
        Iterator<Integer> methodIter = metadata.zTXt_compressionMethod.iterator();
        Iterator<String> textIter = metadata.zTXt_text.iterator();

        while (keywordIter.hasNext()) {
            ChunkStream cs = new ChunkStream(PNGImageReader.zTXt_TYPE, stream);
            String keyword = keywordIter.next();
            if (keyword.length() > 79) {
                throw new IIOException("zTXt keyword is longer than 79");
            }
            cs.writeBytes(keyword);
            cs.writeByte(0);

            int compressionMethod = (methodIter.next()).intValue();
            cs.writeByte(compressionMethod);

            String text = textIter.next();
            cs.write(deflate(text.getBytes(ISO_8859_1)));
            cs.finish();
        }
    }

    private void writeUnknownChunks() throws IOException {
        Iterator<String> typeIter = metadata.unknownChunkType.iterator();
        Iterator<byte[]> dataIter = metadata.unknownChunkData.iterator();

        while (typeIter.hasNext() && dataIter.hasNext()) {
            String type = typeIter.next();
            ChunkStream cs = new ChunkStream(chunkType(type), stream);
            byte[] data = dataIter.next();
            cs.write(data);
            cs.finish();
        }
    }

    private static int chunkType(String typeString) {
        char c0 = typeString.charAt(0);
        char c1 = typeString.charAt(1);
        char c2 = typeString.charAt(2);
        char c3 = typeString.charAt(3);

        int type = (c0 << 24) | (c1 << 16) | (c2 << 8) | c3;
        return type;
    }

    private void encodePass(ImageOutputStream os,
                            RenderedImage image,
                            int xOffset, int yOffset,
                            int xSkip, int ySkip) throws IOException {
        int minX = sourceXOffset;
        int minY = sourceYOffset;
        int width = sourceWidth;
        int height = sourceHeight;

        // Adjust offsets and skips based on source subsampling factors
        xOffset *= periodX;
        xSkip *= periodX;
        yOffset *= periodY;
        ySkip *= periodY;

        // Early exit if no data for this pass
        int hpixels = (width - xOffset + xSkip - 1)/xSkip;
        int vpixels = (height - yOffset + ySkip - 1)/ySkip;
        if (hpixels == 0 || vpixels == 0) {
            return;
        }

        // Convert X offset and skip from pixels to samples
        xOffset *= numBands;
        xSkip *= numBands;

        // Create row buffers
        int samplesPerByte = 8/metadata.IHDR_bitDepth;
        int numSamples = width*numBands;
        int[] samples = new int[numSamples];

        int bytesPerRow = hpixels*numBands;
        if (metadata.IHDR_bitDepth < 8) {
            bytesPerRow = (bytesPerRow + samplesPerByte - 1)/samplesPerByte;
        } else if (metadata.IHDR_bitDepth == 16) {
            bytesPerRow *= 2;
        }

        IndexColorModel icm_gray_alpha = null;
        if (metadata.IHDR_colorType == PNGImageReader.PNG_COLOR_GRAY_ALPHA &&
            image.getColorModel() instanceof IndexColorModel)
        {
            // reserve space for alpha samples
            bytesPerRow *= 2;

            // will be used to calculate alpha value for the pixel
            icm_gray_alpha = (IndexColorModel)image.getColorModel();
        }

        currRow = new byte[bytesPerRow + bpp];
        prevRow = new byte[bytesPerRow + bpp];
        filteredRows = new byte[5][bytesPerRow + bpp];

        int bitDepth = metadata.IHDR_bitDepth;
        for (int row = minY + yOffset; row < minY + height; row += ySkip) {
            Rectangle rect = new Rectangle(minX, row, width, 1);
            Raster ras = image.getData(rect);
            if (sourceBands != null) {
                ras = ras.createChild(minX, row, width, 1, minX, row,
                                      sourceBands);
            }

            ras.getPixels(minX, row, width, 1, samples);

            if (image.getColorModel().isAlphaPremultiplied()) {
                WritableRaster wr = ras.createCompatibleWritableRaster();
                wr.setPixels(wr.getMinX(), wr.getMinY(),
                             wr.getWidth(), wr.getHeight(),
                             samples);

                image.getColorModel().coerceData(wr, false);
                wr.getPixels(wr.getMinX(), wr.getMinY(),
                             wr.getWidth(), wr.getHeight(),
                             samples);
            }

            // Reorder palette data if necessary
            int[] paletteOrder = metadata.PLTE_order;
            if (paletteOrder != null) {
                for (int i = 0; i < numSamples; i++) {
                    samples[i] = paletteOrder[samples[i]];
                }
            }

            int count = bpp; // leave first 'bpp' bytes zero
            int pos = 0;
            int tmp = 0;

            switch (bitDepth) {
            case 1: case 2: case 4:
                // Image can only have a single band

                int mask = samplesPerByte - 1;
                for (int s = xOffset; s < numSamples; s += xSkip) {
                    byte val = scale0[samples[s]];
                    tmp = (tmp << bitDepth) | val;

                    if ((pos++ & mask) == mask) {
                        currRow[count++] = (byte)tmp;
                        tmp = 0;
                        pos = 0;
                    }
                }

                // Left shift the last byte
                if ((pos & mask) != 0) {
                    tmp <<= ((8/bitDepth) - pos)*bitDepth;
                    currRow[count++] = (byte)tmp;
                }
                break;

            case 8:
                if (numBands == 1) {
                    for (int s = xOffset; s < numSamples; s += xSkip) {
                        currRow[count++] = scale0[samples[s]];
                        if (icm_gray_alpha != null) {
                            currRow[count++] =
                                scale0[icm_gray_alpha.getAlpha(0xff & samples[s])];
                        }
                    }
                } else {
                    for (int s = xOffset; s < numSamples; s += xSkip) {
                        for (int b = 0; b < numBands; b++) {
                            currRow[count++] = scale[b][samples[s + b]];
                        }
                    }
                }
                break;

            case 16:
                for (int s = xOffset; s < numSamples; s += xSkip) {
                    for (int b = 0; b < numBands; b++) {
                        currRow[count++] = scaleh[b][samples[s + b]];
                        currRow[count++] = scalel[b][samples[s + b]];
                    }
                }
                break;
            }

            // Perform filtering
            int filterType = rowFilter.filterRow(metadata.IHDR_colorType,
                                                 currRow, prevRow,
                                                 filteredRows,
                                                 bytesPerRow, bpp);

            os.write(filterType);
            os.write(filteredRows[filterType], bpp, bytesPerRow);

            // Swap current and previous rows
            byte[] swap = currRow;
            currRow = prevRow;
            prevRow = swap;

            pixelsDone += hpixels;
            processImageProgress(100.0F*pixelsDone/totalPixels);

            // If write has been aborted, just return;
            // processWriteAborted will be called later
            if (abortRequested()) {
                return;
            }
        }
    }

    // Use sourceXOffset, etc.
    private void write_IDAT(RenderedImage image, int deflaterLevel)
        throws IOException
    {
        IDATOutputStream ios = new IDATOutputStream(stream, 32768,
                                                    deflaterLevel);
        try {
            if (metadata.IHDR_interlaceMethod == 1) {
                for (int i = 0; i < 7; i++) {
                    encodePass(ios, image,
                               PNGImageReader.adam7XOffset[i],
                               PNGImageReader.adam7YOffset[i],
                               PNGImageReader.adam7XSubsampling[i],
                               PNGImageReader.adam7YSubsampling[i]);
                    if (abortRequested()) {
                        break;
                    }
                }
            } else {
                encodePass(ios, image, 0, 0, 1, 1);
            }
        } finally {
            ios.finish();
        }
    }

    private void writeIEND() throws IOException {
        ChunkStream cs = new ChunkStream(PNGImageReader.IEND_TYPE, stream);
        cs.finish();
    }

    // Check two int arrays for value equality, always returns false
    // if either array is null
    private boolean equals(int[] s0, int[] s1) {
        if (s0 == null || s1 == null) {
            return false;
        }
        if (s0.length != s1.length) {
            return false;
        }
        for (int i = 0; i < s0.length; i++) {
            if (s0[i] != s1[i]) {
                return false;
            }
        }
        return true;
    }

    // Initialize the scale/scale0 or scaleh/scalel arrays to
    // hold the results of scaling an input value to the desired
    // output bit depth
    private void initializeScaleTables(int[] sampleSize) {
        int bitDepth = metadata.IHDR_bitDepth;

        // If the existing tables are still valid, just return
        if (bitDepth == scalingBitDepth &&
            equals(sampleSize, this.sampleSize)) {
            return;
        }

        // Compute new tables
        this.sampleSize = sampleSize;
        this.scalingBitDepth = bitDepth;
        int maxOutSample = (1 << bitDepth) - 1;
        if (bitDepth <= 8) {
            scale = new byte[numBands][];
            for (int b = 0; b < numBands; b++) {
                int maxInSample = (1 << sampleSize[b]) - 1;
                int halfMaxInSample = maxInSample/2;
                scale[b] = new byte[maxInSample + 1];
                for (int s = 0; s <= maxInSample; s++) {
                    scale[b][s] =
                        (byte)((s*maxOutSample + halfMaxInSample)/maxInSample);
                }
            }
            scale0 = scale[0];
            scaleh = scalel = null;
        } else { // bitDepth == 16
            // Divide scaling table into high and low bytes
            scaleh = new byte[numBands][];
            scalel = new byte[numBands][];

            for (int b = 0; b < numBands; b++) {
                int maxInSample = (1 << sampleSize[b]) - 1;
                int halfMaxInSample = maxInSample/2;
                scaleh[b] = new byte[maxInSample + 1];
                scalel[b] = new byte[maxInSample + 1];
                for (int s = 0; s <= maxInSample; s++) {
                    int val = (s*maxOutSample + halfMaxInSample)/maxInSample;
                    scaleh[b][s] = (byte)(val >> 8);
                    scalel[b][s] = (byte)(val & 0xff);
                }
            }
            scale = null;
            scale0 = null;
        }
    }

    @Override
    public void write(IIOMetadata streamMetadata,
                      IIOImage image,
                      ImageWriteParam param) throws IIOException {
        if (stream == null) {
            throw new IllegalStateException("output == null!");
        }
        if (image == null) {
            throw new IllegalArgumentException("image == null!");
        }
        if (image.hasRaster()) {
            throw new UnsupportedOperationException("image has a Raster!");
        }

        RenderedImage im = image.getRenderedImage();
        SampleModel sampleModel = im.getSampleModel();
        this.numBands = sampleModel.getNumBands();

        // Set source region and subsampling to default values
        this.sourceXOffset = im.getMinX();
        this.sourceYOffset = im.getMinY();
        this.sourceWidth = im.getWidth();
        this.sourceHeight = im.getHeight();
        this.sourceBands = null;
        this.periodX = 1;
        this.periodY = 1;

        if (param != null) {
            // Get source region and subsampling factors
            Rectangle sourceRegion = param.getSourceRegion();
            if (sourceRegion != null) {
                Rectangle imageBounds = new Rectangle(im.getMinX(),
                                                      im.getMinY(),
                                                      im.getWidth(),
                                                      im.getHeight());
                // Clip to actual image bounds
                sourceRegion = sourceRegion.intersection(imageBounds);
                sourceXOffset = sourceRegion.x;
                sourceYOffset = sourceRegion.y;
                sourceWidth = sourceRegion.width;
                sourceHeight = sourceRegion.height;
            }

            // Adjust for subsampling offsets
            int gridX = param.getSubsamplingXOffset();
            int gridY = param.getSubsamplingYOffset();
            sourceXOffset += gridX;
            sourceYOffset += gridY;
            sourceWidth -= gridX;
            sourceHeight -= gridY;

            // Get subsampling factors
            periodX = param.getSourceXSubsampling();
            periodY = param.getSourceYSubsampling();

            int[] sBands = param.getSourceBands();
            if (sBands != null) {
                sourceBands = sBands;
                numBands = sourceBands.length;
            }
        }

        // Compute output dimensions
        int destWidth = (sourceWidth + periodX - 1)/periodX;
        int destHeight = (sourceHeight + periodY - 1)/periodY;
        if (destWidth <= 0 || destHeight <= 0) {
            throw new IllegalArgumentException("Empty source region!");
        }

        // Compute total number of pixels for progress notification
        this.totalPixels = destWidth*destHeight;
        this.pixelsDone = 0;

        // Create metadata
        IIOMetadata imd = image.getMetadata();
        if (imd != null) {
            metadata = (PNGMetadata)convertImageMetadata(imd,
                               ImageTypeSpecifier.createFromRenderedImage(im),
                                                         null);
        } else {
            metadata = new PNGMetadata();
        }

        // reset compression level to default:
        int deflaterLevel = DEFAULT_COMPRESSION_LEVEL;

        if (param != null) {
            switch(param.getCompressionMode()) {
            case ImageWriteParam.MODE_DISABLED:
                deflaterLevel = Deflater.NO_COMPRESSION;
                break;
            case ImageWriteParam.MODE_EXPLICIT:
                float quality = param.getCompressionQuality();
                if (quality >= 0f && quality <= 1f) {
                    deflaterLevel = 9 - Math.round(9f * quality);
                }
                break;
            default:
            }

            // Use Adam7 interlacing if set in write param
            switch (param.getProgressiveMode()) {
            case ImageWriteParam.MODE_DEFAULT:
                metadata.IHDR_interlaceMethod = 1;
                break;
            case ImageWriteParam.MODE_DISABLED:
                metadata.IHDR_interlaceMethod = 0;
                break;
                // MODE_COPY_FROM_METADATA should already be taken care of
                // MODE_EXPLICIT is not allowed
            default:
            }
        }

        // Initialize bitDepth and colorType
        metadata.initialize(new ImageTypeSpecifier(im), numBands);

        // Overwrite IHDR width and height values with values from image
        metadata.IHDR_width = destWidth;
        metadata.IHDR_height = destHeight;

        this.bpp = numBands*((metadata.IHDR_bitDepth == 16) ? 2 : 1);

        // Initialize scaling tables for this image
        initializeScaleTables(sampleModel.getSampleSize());

        clearAbortRequest();

        processImageStarted(0);
        if (abortRequested()) {
            processWriteAborted();
        } else {
            try {
                write_magic();
                write_IHDR();

                write_cHRM();
                write_gAMA();
                write_iCCP();
                write_sBIT();
                write_sRGB();

                write_PLTE();

                write_hIST();
                write_tRNS();
                write_bKGD();

                write_pHYs();
                write_sPLT();
                write_tIME();
                write_tEXt();
                write_iTXt();
                write_zTXt();

                writeUnknownChunks();

                write_IDAT(im, deflaterLevel);

                if (abortRequested()) {
                    processWriteAborted();
                } else {
                    // Finish up and inform the listeners we are done
                    writeIEND();
                    processImageComplete();
                }
            } catch (IOException e) {
                throw new IIOException("I/O error writing PNG file!", e);
            }
        }
    }
}
