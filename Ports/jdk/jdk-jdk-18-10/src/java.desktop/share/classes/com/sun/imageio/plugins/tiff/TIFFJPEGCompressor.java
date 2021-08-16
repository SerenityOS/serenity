/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.plugins.tiff.TIFFTag;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * Compressor for encoding compression type 7, TTN2/Adobe JPEG-in-TIFF.
 */
public class TIFFJPEGCompressor extends TIFFBaseJPEGCompressor {

    // Subsampling factor for chroma bands (Cb Cr).
    static final int CHROMA_SUBSAMPLING = 2;

    /**
     * A filter which identifies the ImageReaderSpi of a JPEG reader
     * which supports JPEG native stream metadata.
     */
    private static class JPEGSPIFilter implements ServiceRegistry.Filter {
        JPEGSPIFilter() {}

        public boolean filter(Object provider) {
            ImageReaderSpi readerSPI = (ImageReaderSpi)provider;

            if(readerSPI != null) {
                String streamMetadataName =
                    readerSPI.getNativeStreamMetadataFormatName();
                if(streamMetadataName != null) {
                    return streamMetadataName.equals(STREAM_METADATA_NAME);
                } else {
                    return false;
                }
            }

            return false;
        }
    }

    /**
     * Retrieves a JPEG reader which supports native JPEG stream metadata.
     */
    private static ImageReader getJPEGTablesReader() {
        ImageReader jpegReader = null;

        try {
            IIORegistry registry = IIORegistry.getDefaultInstance();
            Iterator<?> readerSPIs =
                registry.getServiceProviders(ImageReaderSpi.class,
                                             new JPEGSPIFilter(),
                                             true);
            if(readerSPIs.hasNext()) {
                ImageReaderSpi jpegReaderSPI =
                    (ImageReaderSpi)readerSPIs.next();
                jpegReader = jpegReaderSPI.createReaderInstance();
            }
        } catch(Exception e) {
            // Ignore it ...
        }

        return jpegReader;
    }

    public TIFFJPEGCompressor(ImageWriteParam param) {
        super("JPEG", BaselineTIFFTagSet.COMPRESSION_JPEG, false, param);
    }

    /**
     * Sets the value of the {@code metadata} field.
     *
     * <p>The implementation in this class also adds the TIFF fields
     * JPEGTables, YCbCrSubSampling, YCbCrPositioning, and
     * ReferenceBlackWhite superseding any prior settings of those
     * fields.</p>
     *
     * @param metadata the {@code IIOMetadata} object for the
     * image being written.
     *
     * @see #getMetadata()
     */
    public void setMetadata(IIOMetadata metadata) {
        super.setMetadata(metadata);

        if (metadata instanceof TIFFImageMetadata) {
            TIFFImageMetadata tim = (TIFFImageMetadata)metadata;
            TIFFIFD rootIFD = tim.getRootIFD();
            BaselineTIFFTagSet base = BaselineTIFFTagSet.getInstance();

            TIFFField f =
                tim.getTIFFField(BaselineTIFFTagSet.TAG_SAMPLES_PER_PIXEL);
            int numBands = f.getAsInt(0);

            if(numBands == 1) {
                // Remove YCbCr fields not relevant for grayscale.

                rootIFD.removeTIFFField(BaselineTIFFTagSet.TAG_Y_CB_CR_SUBSAMPLING);
                rootIFD.removeTIFFField(BaselineTIFFTagSet.TAG_Y_CB_CR_POSITIONING);
                rootIFD.removeTIFFField(BaselineTIFFTagSet.TAG_REFERENCE_BLACK_WHITE);
            } else { // numBands == 3
                // Replace YCbCr fields.

                // YCbCrSubSampling
                TIFFField YCbCrSubSamplingField = new TIFFField
                    (base.getTag(BaselineTIFFTagSet.TAG_Y_CB_CR_SUBSAMPLING),
                     TIFFTag.TIFF_SHORT, 2,
                     new char[] {CHROMA_SUBSAMPLING, CHROMA_SUBSAMPLING});
                rootIFD.addTIFFField(YCbCrSubSamplingField);

                // YCbCrPositioning
                TIFFField YCbCrPositioningField = new TIFFField
                    (base.getTag(BaselineTIFFTagSet.TAG_Y_CB_CR_POSITIONING),
                     TIFFTag.TIFF_SHORT, 1,
                     new char[]
                        {BaselineTIFFTagSet.Y_CB_CR_POSITIONING_CENTERED});
                rootIFD.addTIFFField(YCbCrPositioningField);

                // ReferenceBlackWhite
                TIFFField referenceBlackWhiteField = new TIFFField
                    (base.getTag(BaselineTIFFTagSet.TAG_REFERENCE_BLACK_WHITE),
                     TIFFTag.TIFF_RATIONAL, 6,
                     new long[][] { // no headroon/footroom
                         {0, 1}, {255, 1},
                         {128, 1}, {255, 1},
                         {128, 1}, {255, 1}
                     });
                rootIFD.addTIFFField(referenceBlackWhiteField);
            }

            // JPEGTables field is written if and only if one is
            // already present in the metadata. If one is present
            // and has either zero length or does not represent a
            // valid tables-only stream, then a JPEGTables field
            // will be written initialized to the standard tables-
            // only stream written by the JPEG writer.

            // Retrieve the JPEGTables field.
            TIFFField JPEGTablesField =
                tim.getTIFFField(BaselineTIFFTagSet.TAG_JPEG_TABLES);

            // Initialize JPEG writer to one supporting abbreviated streams.
            if(JPEGTablesField != null) {
                // Intialize the JPEG writer to one that supports stream
                // metadata, i.e., abbreviated streams, and may or may not
                // support image metadata.
                initJPEGWriter(true, false);
            }

            // Write JPEGTables field if a writer supporting abbreviated
            // streams was available.
            if(JPEGTablesField != null && JPEGWriter != null) {
                // Set the abbreviated stream flag.
                this.writeAbbreviatedStream = true;

                //Branch based on field value count.
                if(JPEGTablesField.getCount() > 0) {
                    // Derive the stream metadata from the field.

                    // Get the field values.
                    byte[] tables = JPEGTablesField.getAsBytes();

                    // Create an input stream for the tables.
                    ByteArrayInputStream bais =
                        new ByteArrayInputStream(tables);
                    MemoryCacheImageInputStream iis =
                        new MemoryCacheImageInputStream(bais);

                    // Read the tables stream using the JPEG reader.
                    ImageReader jpegReader = getJPEGTablesReader();
                    jpegReader.setInput(iis);

                    // Initialize the stream metadata object.
                    try {
                        JPEGStreamMetadata = jpegReader.getStreamMetadata();
                    } catch(Exception e) {
                        // Fall back to default tables.
                        JPEGStreamMetadata = null;
                    } finally {
                        jpegReader.reset();
                    }
                }

                if(JPEGStreamMetadata == null) {
                    // Derive the field from default stream metadata.

                    // Get default stream metadata.
                    JPEGStreamMetadata =
                        JPEGWriter.getDefaultStreamMetadata(JPEGParam);

                    // Create an output stream for the tables.
                    ByteArrayOutputStream tableByteStream =
                        new ByteArrayOutputStream();
                    MemoryCacheImageOutputStream tableStream =
                        new MemoryCacheImageOutputStream(tableByteStream);

                    // Write a tables-only stream.
                    JPEGWriter.setOutput(tableStream);
                    try {
                        JPEGWriter.prepareWriteSequence(JPEGStreamMetadata);
                        tableStream.flush();
                        JPEGWriter.endWriteSequence();

                        // Get the tables-only stream content.
                        byte[] tables = tableByteStream.toByteArray();

                        // Add the JPEGTables field.
                        JPEGTablesField = new TIFFField
                            (base.getTag(BaselineTIFFTagSet.TAG_JPEG_TABLES),
                             TIFFTag.TIFF_UNDEFINED,
                             tables.length,
                             tables);
                        rootIFD.addTIFFField(JPEGTablesField);
                    } catch(Exception e) {
                        // Do not write JPEGTables field.
                        rootIFD.removeTIFFField(BaselineTIFFTagSet.TAG_JPEG_TABLES);
                        this.writeAbbreviatedStream = false;
                    }
                }
            } else { // Do not write JPEGTables field.
                // Remove any field present.
                rootIFD.removeTIFFField(BaselineTIFFTagSet.TAG_JPEG_TABLES);

                // Initialize the writer preferring codecLib.
                initJPEGWriter(false, false);
            }
        }
    }
}
