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
import java.io.ByteArrayOutputStream;
import javax.imageio.IIOException;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFField;

/**
 * {@code TIFFDecompressor} for "Old JPEG" compression.
 */
public class TIFFOldJPEGDecompressor extends TIFFJPEGDecompressor {

    // Define Huffman Tables
    private static final int DHT = 0xC4;

    // Define Quantisation Tables
    private static final int DQT = 0xDB;

    // Define Restart Interval
    private static final int DRI = 0xDD;

    // Baseline DCT
    private static final int SOF0 = 0xC0;

    // Start of Scan
    private static final int SOS = 0xDA;

    // End of Image
    // private static final int EOI = 0xD9; // now defined in superclass

    // Whether the decompressor has been initialized.
    private boolean isInitialized = false;

    //
    // Instance variables set by the initialize() method.
    //
    // Offset to a complete, contiguous JPEG stream.
    private Long JPEGStreamOffset = null;
    // Offset to the SOF marker.
    private int SOFPosition = -1;
    // Value of the SOS marker.
    private byte[] SOSMarker = null;

    // Horizontal chroma subsampling factor.
    private int subsamplingX = 2;

    // Vertical chroma subsampling factor.
    private int subsamplingY = 2;

    public TIFFOldJPEGDecompressor() {}

    //
    // Intialize instance variables according to an analysis of the
    // TIFF field content. See bug 4929147 for test image information.
    //
    // Case 1: Image contains a single strip or tile and the offset to
    //         that strip or tile points to an SOI marker.
    //
    //         Example:
    //         "Visionshape Inc. Compression Software, version 2.5"
    //         ColorTiffWithBarcode.tif
    //         Color2.tif (pages 2-5 (indexes 1-4)
    //         color3.tif (pages 2-5 (indexes 1-4)
    //
    //         "Kofax standard Multi-Page TIFF Storage Filter v2.01.000"
    //         01.tif (pages 1 and 3(indexes 0 and 2))
    //
    //         Instance variables set: JPEGStreamOffset
    //
    // Case 2: Image contains a single strip or tile and a
    //         JPEGInterchangeFormat field is present but the
    //         JPEGInterchangeFormatLength is erroneously missing.
    //
    //         Example:
    //         "Kofax standard Multi-Page TIFF Storage Filter v2.01.000"
    //         01.tif (pages 1 and 3(indexes 0 and 2))
    //         (but this example also satisfies case 1)
    //
    //         Instance variables set: JPEGStreamOffset
    //
    // Case 3: Image contains a single strip or tile, the
    //         JPEGInterchangeFormat and JPEGInterchangeFormatLength
    //         fields are both present, the value of JPEGInterchangeFormat
    //         is less than the offset to the strip or tile, and the sum
    //         of the values of JPEGInterchangeFormat and
    //         JPEGInterchangeFormatLength is greater than the offset to
    //         the strip or tile.
    //
    //         Instance variables set: JPEGStreamOffset
    //
    //         Example:
    //         "HP IL v1.1"
    //         smallliz.tif from libtiff test data.
    //
    //         Instance variables set: JPEGStreamOffset
    //
    // Cases 4-5 apply if none of cases 1-3 applies or the image has multiple
    // strips or tiles.
    //
    // Case 4: JPEGInterchangeFormat and JPEGInterchangeFormatLength are
    //         present, the value of JPEGInterchangeFormatLength is at least 2,
    //         and the sum of the values of these two fields is at most the
    //         value of the offset to the first strip or tile.
    //
    //         Instance variables set: tables, SOFPosition, SOSMarker
    //
    //         Example:
    //         "Oi/GFS, writer v00.06.00P, (c) Wang Labs, Inc. 1990, 1991"
    //         03.tif (pages 1 and 3(indexes 0 and 2))
    //
    //         "Oi/GFS, writer v00.06.02"
    //         Color2.tif (page 1 (index 0))
    //         color3.tif (page 1 (index 0))
    //
    // Case 5: If none of the foregoing cases apply. For this case the
    //         JPEGQTables, JPEGACTables, and JPEGDCTables must be valid.
    //
    //         Instance variables set: tables, SOFPosition, SOSMarker
    //
    //         Example:
    //         "NeXT"
    //         zackthecat.tif from libtiff test data.
    //
    private synchronized void initialize() throws IOException {
        if(isInitialized) {
            return;
        }

        // Get the TIFF metadata object.
        TIFFImageMetadata tim = (TIFFImageMetadata)metadata;

        // Get the JPEGInterchangeFormat field.
        TIFFField JPEGInterchangeFormatField =
            tim.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT);

        // Get the tile or strip offsets.
        TIFFField segmentOffsetField =
            tim.getTIFFField(BaselineTIFFTagSet.TAG_TILE_OFFSETS);
        if(segmentOffsetField == null) {
            segmentOffsetField =
                tim.getTIFFField(BaselineTIFFTagSet.TAG_STRIP_OFFSETS);
            if(segmentOffsetField == null) {
                segmentOffsetField = JPEGInterchangeFormatField;
            }
        }
        long[] segmentOffsets = segmentOffsetField.getAsLongs();

            // Determine whether the image has more than one strip or tile.
        boolean isTiled = segmentOffsets.length > 1;

        if(!isTiled) {
            //
            // If the image has only a single strip or tile and it looks
            // as if a complete JPEG stream is present then set the value
            // of JPEGStreamOffset to the offset of the JPEG stream;
            // otherwise leave JPEGStreamOffset set to null.
            //

            stream.seek(offset);
            stream.mark();
            if(stream.read() == 0xff && stream.read() == SOI) {
                // Tile or strip offset points to SOI.
                JPEGStreamOffset = Long.valueOf(offset);

                // Set initialization flag and return.
                ((TIFFImageReader)reader).forwardWarningMessage("SOI marker detected at start of strip or tile.");
                isInitialized = true;
                return;
            }
            stream.reset();

            if(JPEGInterchangeFormatField != null) {
                // Get the value of JPEGInterchangeFormat.
                long jpegInterchangeOffset =
                    JPEGInterchangeFormatField.getAsLong(0);

                // Get the JPEGInterchangeFormatLength field.
                TIFFField JPEGInterchangeFormatLengthField =
                    tim.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);

                if(JPEGInterchangeFormatLengthField == null) {
                    // JPEGInterchangeFormat stream is of indeterminate
                    // length so use it as a complete JPEG stream.
                    JPEGStreamOffset = Long.valueOf(jpegInterchangeOffset);

                    // Set initialization flag and return.
                    ((TIFFImageReader)reader).forwardWarningMessage("JPEGInterchangeFormatLength field is missing");
                    isInitialized = true;
                    return;
                } else {
                    // Get the JPEGInterchangeFormatLength field's value.
                    long jpegInterchangeLength =
                        JPEGInterchangeFormatLengthField.getAsLong(0);

                    if(jpegInterchangeOffset < segmentOffsets[0] &&
                       (jpegInterchangeOffset + jpegInterchangeLength) >
                       segmentOffsets[0]) {
                        // JPEGInterchangeFormat points to a position
                        // below the segment start position and ends at
                        // a position after the segment start position.
                        JPEGStreamOffset = Long.valueOf(jpegInterchangeOffset);

                        // Set initialization flag and return.
                        isInitialized = true;
                        return;
                    }
                }
            }
        }

        // Get the subsampling factors.
        TIFFField YCbCrSubsamplingField =
            tim.getTIFFField(BaselineTIFFTagSet.TAG_Y_CB_CR_SUBSAMPLING);
        if(YCbCrSubsamplingField != null) {
            subsamplingX = YCbCrSubsamplingField.getAsChars()[0];
            subsamplingY = YCbCrSubsamplingField.getAsChars()[1];
        }

        //
        // Initialize the 'tables' instance variable either for later
        // use in prepending to individual abbreviated strips or tiles.
        //
        if(JPEGInterchangeFormatField != null) {
            // Get the value of JPEGInterchangeFormat.
            long jpegInterchangeOffset =
                JPEGInterchangeFormatField.getAsLong(0);

            // Get the JPEGInterchangeFormatLength field.
            TIFFField JPEGInterchangeFormatLengthField =
                tim.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);

            if(JPEGInterchangeFormatLengthField != null) {
                // Get the JPEGInterchangeFormatLength field's value.
                long jpegInterchangeLength =
                    JPEGInterchangeFormatLengthField.getAsLong(0);

                if(jpegInterchangeLength >= 2 &&
                   jpegInterchangeOffset + jpegInterchangeLength <=
                   segmentOffsets[0]) {
                    // Determine the length excluding any terminal EOI marker
                    // and allocate table memory.
                    stream.mark();
                    stream.seek(jpegInterchangeOffset+jpegInterchangeLength-2);
                    if(stream.read() == 0xff && stream.read() == EOI) {
                        this.tables = new byte[(int)(jpegInterchangeLength-2)];
                    } else {
                        this.tables = new byte[(int)jpegInterchangeLength];
                    }
                    stream.reset();

                    // Read the tables.
                    stream.mark();
                    stream.seek(jpegInterchangeOffset);
                    stream.readFully(tables);
                    stream.reset();

                    ((TIFFImageReader)reader).forwardWarningMessage("Incorrect JPEG interchange format: using JPEGInterchangeFormat offset to derive tables.");
                } else {
                    ((TIFFImageReader)reader).forwardWarningMessage("JPEGInterchangeFormat+JPEGInterchangeFormatLength > offset to first strip or tile.");
                }
            }
        }

        if(this.tables == null) {
            //
            // Create tables-only stream in tables[] consisting of
            // SOI+DQTs+DHTs
            //

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Save stream length;
            long streamLength = stream.length();

            // SOI
            baos.write(0xff);
            baos.write(SOI);

            // Quantization Tables
            TIFFField f =
                tim.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_Q_TABLES);
            if(f == null) {
                throw new IIOException("JPEGQTables field missing!");
            }
            long[] off = f.getAsLongs();

            for(int i = 0; i < off.length; i++) {
                baos.write(0xff); // Marker ID
                baos.write(DQT);

                char markerLength = (char)67;
                baos.write((markerLength >>> 8) & 0xff); // Length
                baos.write(markerLength & 0xff);

                baos.write(i); // Table ID and precision

                byte[] qtable = new byte[64];
                if(streamLength != -1 && off[i] > streamLength) {
                    throw new IIOException("JPEGQTables offset for index "+
                                           i+" is not in the stream!");
                }
                stream.seek(off[i]);
                stream.readFully(qtable);

                baos.write(qtable); // Table data
            }

            // Huffman Tables (k == 0 ? DC : AC).
            for(int k = 0; k < 2; k++) {
                int tableTagNumber = k == 0 ?
                    BaselineTIFFTagSet.TAG_JPEG_DC_TABLES :
                    BaselineTIFFTagSet.TAG_JPEG_AC_TABLES;
                f = tim.getTIFFField(tableTagNumber);
                String fieldName =
                    tableTagNumber ==
                    BaselineTIFFTagSet.TAG_JPEG_DC_TABLES ?
                    "JPEGDCTables" : "JPEGACTables";

                if(f == null) {
                    throw new IIOException(fieldName+" field missing!");
                }
                off = f.getAsLongs();

                for(int i = 0; i < off.length; i++) {
                    baos.write(0xff); // Marker ID
                    baos.write(DHT);

                    byte[] blengths = new byte[16];
                    if(streamLength != -1 && off[i] > streamLength) {
                        throw new IIOException(fieldName+" offset for index "+
                                               i+" is not in the stream!");
                    }
                    stream.seek(off[i]);
                    stream.readFully(blengths);
                    int numCodes = 0;
                    for(int j = 0; j < 16; j++) {
                        numCodes += blengths[j]&0xff;
                    }

                    char markerLength = (char)(19 + numCodes);

                    baos.write((markerLength >>> 8) & 0xff); // Length
                    baos.write(markerLength & 0xff);

                    baos.write(i | (k << 4)); // Table ID and type

                    baos.write(blengths); // Number of codes

                    byte[] bcodes = new byte[numCodes];
                    stream.readFully(bcodes);
                    baos.write(bcodes); // Codes
                }
            }

            // SOF0
            baos.write((byte)0xff); // Marker identifier
            baos.write((byte)SOF0);
            short sval = (short)(8 + 3*samplesPerPixel); // Length
            baos.write((byte)((sval >>> 8) & 0xff));
            baos.write((byte)(sval & 0xff));
            baos.write((byte)8); // Data precision
            sval = (short)srcHeight; // Tile/strip height
            baos.write((byte)((sval >>> 8) & 0xff));
            baos.write((byte)(sval & 0xff));
            sval = (short)srcWidth; // Tile/strip width
            baos.write((byte)((sval >>> 8) & 0xff));
            baos.write((byte)(sval & 0xff));
            baos.write((byte)samplesPerPixel); // Number of components
            if(samplesPerPixel == 1) {
                baos.write((byte)1); // Component ID
                baos.write((byte)0x11); // Subsampling factor
                baos.write((byte)0); // Quantization table ID
            } else { // 3
                for(int i = 0; i < 3; i++) {
                    baos.write((byte)(i + 1)); // Component ID
                    baos.write((i != 0) ?
                               (byte)0x11 :
                               (byte)(((subsamplingX & 0x0f) << 4) |
                                      (subsamplingY & 0x0f)));

                    baos.write((byte)i); // Quantization table ID
                }
            };


            // DRI (optional).
            f = tim.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_RESTART_INTERVAL);
            if(f != null) {
                char restartInterval = f.getAsChars()[0];

                if(restartInterval != 0) {
                    baos.write((byte)0xff); // Marker identifier
                    baos.write((byte)DRI);

                    sval = 4;
                    baos.write((byte)((sval >>> 8) & 0xff)); // Length
                    baos.write((byte)(sval & 0xff));

                    // RestartInterval
                    baos.write((byte)((restartInterval >>> 8) & 0xff));
                    baos.write((byte)(restartInterval & 0xff));
                }
            }

            tables = baos.toByteArray();
        }

        //
        // Check for presence of SOF marker and save its position.
        //
        int idx = 0;
        int idxMax = tables.length - 1;
        while(idx < idxMax) {
            if((tables[idx]&0xff) == 0xff &&
               (tables[idx+1]&0xff) == SOF0) {
                SOFPosition = idx;
                break;
            }
            idx++;
        }

        //
        // If no SOF marker, add one.
        //
        if(SOFPosition == -1) {
            byte[] tmpTables =
                new byte[tables.length + 10 + 3*samplesPerPixel];
            System.arraycopy(tables, 0, tmpTables, 0, tables.length);
            int tmpOffset = tables.length;
            SOFPosition = tables.length;
            tables = tmpTables;

            tables[tmpOffset++] = (byte)0xff; // Marker identifier
            tables[tmpOffset++] = (byte)SOF0;
            short sval = (short)(8 + 3*samplesPerPixel); // Length
            tables[tmpOffset++] = (byte)((sval >>> 8) & 0xff);
            tables[tmpOffset++] = (byte)(sval & 0xff);
            tables[tmpOffset++] = (byte)8; // Data precision
            sval = (short)srcHeight; // Tile/strip height
            tables[tmpOffset++] = (byte)((sval >>> 8) & 0xff);
            tables[tmpOffset++] = (byte)(sval & 0xff);
            sval = (short)srcWidth; // Tile/strip width
            tables[tmpOffset++] = (byte)((sval >>> 8) & 0xff);
            tables[tmpOffset++] = (byte)(sval & 0xff);
            tables[tmpOffset++] = (byte)samplesPerPixel; // Number of components
            if(samplesPerPixel == 1) {
                tables[tmpOffset++] = (byte)1; // Component ID
                tables[tmpOffset++] = (byte)0x11; // Subsampling factor
                tables[tmpOffset++] = (byte)0; // Quantization table ID
            } else { // 3
                for(int i = 0; i < 3; i++) {
                    tables[tmpOffset++] = (byte)(i + 1); // Component ID
                    tables[tmpOffset++] = (i != 0) ?
                        (byte)0x11 :
                        (byte)(((subsamplingX & 0x0f) << 4) |
                               (subsamplingY & 0x0f));

                    tables[tmpOffset++] = (byte)i; // Quantization table ID
                }
            };
        }

        //
        // Initialize SOSMarker.
        //
        stream.mark();
        stream.seek(segmentOffsets[0]);
        if(stream.read() == 0xff && stream.read() == SOS) {
            //
            // If the first segment starts with an SOS marker save it.
            //
            int SOSLength = (stream.read()<<8)|stream.read();
            SOSMarker = new byte[SOSLength+2];
            SOSMarker[0] = (byte)0xff;
            SOSMarker[1] = (byte)SOS;
            SOSMarker[2] = (byte)((SOSLength & 0xff00) >> 8);
            SOSMarker[3] = (byte)(SOSLength & 0xff);
            stream.readFully(SOSMarker, 4, SOSLength - 2);
        } else {
            //
            // Manufacture an SOS marker.
            //
            SOSMarker = new byte[2 + 6 + 2*samplesPerPixel];
            int SOSMarkerIndex = 0;
            SOSMarker[SOSMarkerIndex++] = (byte)0xff; // Marker identifier
            SOSMarker[SOSMarkerIndex++] = (byte)SOS;
            short sval = (short)(6 + 2*samplesPerPixel); // Length
            SOSMarker[SOSMarkerIndex++] = (byte)((sval >>> 8) & 0xff);
            SOSMarker[SOSMarkerIndex++] = (byte)(sval & 0xff);
            // Number of components in scan
            SOSMarker[SOSMarkerIndex++] = (byte)samplesPerPixel;
            if(samplesPerPixel == 1) {
                SOSMarker[SOSMarkerIndex++] = (byte)1; // Component ID
                SOSMarker[SOSMarkerIndex++] = (byte)0; // Huffman table ID
            } else { // 3
                for(int i = 0; i < 3; i++) {
                    SOSMarker[SOSMarkerIndex++] =
                        (byte)(i + 1); // Component ID
                    SOSMarker[SOSMarkerIndex++] =
                        (byte)((i << 4) | i); // Huffman table IDs
                }
            };
            SOSMarker[SOSMarkerIndex++] = (byte)0;
            SOSMarker[SOSMarkerIndex++] = (byte)0x3f;
            SOSMarker[SOSMarkerIndex++] = (byte)0;
        }
        stream.reset();

        // Set initialization flag.
        isInitialized = true;
    }

    //
    // The strategy for reading cases 1-3 is to treat the data as a complete
    // JPEG interchange stream located at JPEGStreamOffset.
    //
    // The strategy for cases 4-5 is to concatenate a tables stream created
    // in initialize() with the entropy coded data in each strip or tile.
    //
    public void decodeRaw(byte[] b,
                          int dstOffset,
                          int bitsPerPixel,
                          int scanlineStride) throws IOException {

        initialize();

        TIFFImageMetadata tim = (TIFFImageMetadata)metadata;

        if(JPEGStreamOffset != null) {
            stream.seek(JPEGStreamOffset.longValue());
            JPEGReader.setInput(stream, false, true);
        } else {
            // Determine buffer length and allocate.
            int tableLength = tables.length;
            int bufLength =
                tableLength + SOSMarker.length + byteCount + 2; // 2 for EOI.
            byte[] buf = new byte[bufLength];
            System.arraycopy(tables, 0, buf, 0, tableLength);
            int bufOffset = tableLength;

            // Update the SOF dimensions.
            short sval = (short)srcHeight; // Tile/strip height
            buf[SOFPosition + 5] = (byte)((sval >>> 8) & 0xff);
            buf[SOFPosition + 6] = (byte)(sval & 0xff);
            sval = (short)srcWidth; // Tile/strip width
            buf[SOFPosition + 7] = (byte)((sval >>> 8) & 0xff);
            buf[SOFPosition + 8] = (byte)(sval & 0xff);

            // Seek to data.
            stream.seek(offset);

            // Add SOS marker if data segment does not start with one.
            byte[] twoBytes = new byte[2];
            stream.readFully(twoBytes);
            if(!((twoBytes[0]&0xff) == 0xff && (twoBytes[1]&0xff) == SOS)) {
                // Segment does not start with SOS marker;
                // use the pre-calculated SOS marker.
                System.arraycopy(SOSMarker, 0, buf, bufOffset,
                                 SOSMarker.length);
                bufOffset += SOSMarker.length;
            }

            // Copy the segment data into the buffer.
            buf[bufOffset++] = twoBytes[0];
            buf[bufOffset++] = twoBytes[1];
            stream.readFully(buf, bufOffset, byteCount - 2);
            bufOffset += byteCount - 2;

            // EOI.
            buf[bufOffset++] = (byte)0xff; // Marker identifier
            buf[bufOffset++] = (byte)EOI;

            ByteArrayInputStream bais =
                new ByteArrayInputStream(buf, 0, bufOffset);
            ImageInputStream is = new MemoryCacheImageInputStream(bais);

            JPEGReader.setInput(is, true, true);
        }

        // Read real image
        JPEGParam.setDestination(rawImage);
        JPEGReader.read(0, JPEGParam);
    }

    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        super.finalize();
        JPEGReader.dispose();
    }
}
