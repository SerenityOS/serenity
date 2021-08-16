/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.imageio.plugins.tiff;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFField;

public class TIFFJPEGDecompressor extends TIFFDecompressor {
    // Start of Image
    protected static final int SOI = 0xD8;

    // End of Image
    protected static final int EOI = 0xD9;

    protected ImageReader JPEGReader = null;
    protected ImageReadParam JPEGParam;

    protected boolean hasJPEGTables = false;
    protected byte[] tables = null;

    private byte[] data = new byte[0];

    public TIFFJPEGDecompressor() {}

    public void beginDecoding() {
        // Initialize the JPEG reader if needed.
        if(this.JPEGReader == null) {
            // Get all JPEG readers.
            Iterator<ImageReader> iter = ImageIO.getImageReadersByFormatName("jpeg");

            if(!iter.hasNext()) {
                throw new IllegalStateException("No JPEG readers found!");
            }

            // Initialize reader to the first one.
            this.JPEGReader = iter.next();

            this.JPEGParam = JPEGReader.getDefaultReadParam();
        }

        // Get the JPEGTables field.
        TIFFImageMetadata tmetadata = (TIFFImageMetadata)metadata;
        TIFFField f =
            tmetadata.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_TABLES);

        if (f != null) {
            this.hasJPEGTables = true;
            this.tables = f.getAsBytes();
        } else {
            this.hasJPEGTables = false;
        }
    }

    public void decodeRaw(byte[] b,
                          int dstOffset,
                          int bitsPerPixel,
                          int scanlineStride) throws IOException {
        // Seek to the data position for this segment.
        stream.seek(offset);

        // Set the stream variable depending on presence of JPEGTables.
        ImageInputStream is;
        if(this.hasJPEGTables) {
            // The current strip or tile is an abbreviated JPEG stream.

            // Reallocate memory if there is not enough already.
            int dataLength = tables.length + byteCount;
            if(data.length < dataLength) {
                data = new byte[dataLength];
            }

            // Copy the tables ignoring any EOI and subsequent bytes.
            int dataOffset = tables.length;
            for(int i = tables.length - 2; i > 0; i--) {
                if((tables[i] & 0xff) == 0xff &&
                   (tables[i+1] & 0xff) == EOI) {
                    dataOffset = i;
                    break;
                }
            }
            System.arraycopy(tables, 0, data, 0, dataOffset);

            // Check for SOI and skip it if present.
            byte byte1 = (byte)stream.read();
            byte byte2 = (byte)stream.read();
            if(!((byte1 & 0xff) == 0xff && (byte2 & 0xff) == SOI)) {
                data[dataOffset++] = byte1;
                data[dataOffset++] = byte2;
            }

            // Read remaining data.
            stream.readFully(data, dataOffset, byteCount - 2);

            // Create ImageInputStream.
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            is = new MemoryCacheImageInputStream(bais);
        } else {
            // The current strip or tile is a complete JPEG stream.
            is = stream;
        }

        // Set the stream on the reader.
        JPEGReader.setInput(is, false, true);

        // Set the destination to the raw image ignoring the parameters.
        JPEGParam.setDestination(rawImage);

        // Read the strip or tile.
        JPEGReader.read(0, JPEGParam);
    }

    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        super.finalize();
        JPEGReader.dispose();
    }
}
