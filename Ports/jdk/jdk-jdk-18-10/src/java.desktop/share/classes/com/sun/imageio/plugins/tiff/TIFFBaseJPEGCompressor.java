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

import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;
import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.w3c.dom.Node;

/**
 * Base class for all possible forms of JPEG compression in TIFF.
 */
public abstract class TIFFBaseJPEGCompressor extends TIFFCompressor {

    // Stream metadata format.
    protected static final String STREAM_METADATA_NAME =
        "javax_imageio_jpeg_stream_1.0";

    // Image metadata format.
    protected static final String IMAGE_METADATA_NAME =
        "javax_imageio_jpeg_image_1.0";

    // ImageWriteParam passed in.
    private ImageWriteParam param = null;

    /**
     * ImageWriteParam for JPEG writer.
     * May be initialized by {@link #initJPEGWriter}.
     */
    protected JPEGImageWriteParam JPEGParam = null;

    /**
     * The JPEG writer.
     * May be initialized by {@link #initJPEGWriter}.
     */
    protected ImageWriter JPEGWriter = null;

    /**
     * Whether to write abbreviated JPEG streams (default == false).
     * A subclass which sets this to {@code true} should also
     * initialized {@link #JPEGStreamMetadata}.
     */
    protected boolean writeAbbreviatedStream = false;

    /**
     * Stream metadata equivalent to a tables-only stream such as in
     * the {@code JPEGTables}. Default value is {@code null}.
     * This should be set by any subclass which sets
     * {@link #writeAbbreviatedStream} to {@code true}.
     */
    protected IIOMetadata JPEGStreamMetadata = null;

    // A pruned image metadata object containing only essential nodes.
    private IIOMetadata JPEGImageMetadata = null;

    // Array-based output stream.
    private IIOByteArrayOutputStream baos;

    /**
     * Removes nonessential nodes from a JPEG native image metadata tree.
     * All nodes derived from JPEG marker segments other than DHT, DQT,
     * SOF, SOS segments are removed unless {@code pruneTables} is
     * {@code true} in which case the nodes derived from the DHT and
     * DQT marker segments are also removed.
     *
     * @param tree A <tt>javax_imageio_jpeg_image_1.0</tt> tree.
     * @param pruneTables Whether to prune Huffman and quantization tables.
     * @throws NullPointerException if {@code tree} is
     * {@code null}.
     * @throws IllegalArgumentException if {@code tree} is not the root
     * of a JPEG native image metadata tree.
     */
    private static void pruneNodes(Node tree, boolean pruneTables) {
        if(tree == null) {
            throw new NullPointerException("tree == null!");
        }
        if(!tree.getNodeName().equals(IMAGE_METADATA_NAME)) {
            throw new IllegalArgumentException
                ("root node name is not "+IMAGE_METADATA_NAME+"!");
        }

        // Create list of required nodes.
        List<String> wantedNodes = new ArrayList<String>();
        wantedNodes.addAll(Arrays.asList(new String[] {
            "JPEGvariety", "markerSequence",
            "sof", "componentSpec",
            "sos", "scanComponentSpec"
        }));

        // Add Huffman and quantization table nodes if not pruning tables.
        if(!pruneTables) {
            wantedNodes.add("dht");
            wantedNodes.add("dhtable");
            wantedNodes.add("dqt");
            wantedNodes.add("dqtable");
        }

        IIOMetadataNode iioTree = (IIOMetadataNode)tree;

        List<Node> nodes = getAllNodes(iioTree, null);
        int numNodes = nodes.size();

        for(int i = 0; i < numNodes; i++) {
            Node node = nodes.get(i);
            if(!wantedNodes.contains(node.getNodeName())) {
                node.getParentNode().removeChild(node);
            }
        }
    }

    private static List<Node> getAllNodes(IIOMetadataNode root, List<Node> nodes) {
        if(nodes == null) nodes = new ArrayList<Node>();

        if(root.hasChildNodes()) {
            Node sibling = root.getFirstChild();
            while(sibling != null) {
                nodes.add(sibling);
                nodes = getAllNodes((IIOMetadataNode)sibling, nodes);
                sibling = sibling.getNextSibling();
            }
        }

        return nodes;
    }

    public TIFFBaseJPEGCompressor(String compressionType,
                                  int compressionTagValue,
                                  boolean isCompressionLossless,
                                  ImageWriteParam param) {
        super(compressionType, compressionTagValue, isCompressionLossless);

        this.param = param;
    }

    /**
     * A {@code ByteArrayOutputStream} which allows writing to an
     * {@code ImageOutputStream}.
     */
    private static class IIOByteArrayOutputStream extends ByteArrayOutputStream {
        IIOByteArrayOutputStream() {
            super();
        }

        IIOByteArrayOutputStream(int size) {
            super(size);
        }

        public synchronized void writeTo(ImageOutputStream ios)
            throws IOException {
            ios.write(buf, 0, count);
        }
    }

    /**
     * Initializes the JPEGWriter and JPEGParam instance variables.
     * This method must be called before encode() is invoked.
     *
     * @param supportsStreamMetadata Whether the JPEG writer must
     * support JPEG native stream metadata, i.e., be capable of writing
     * abbreviated streams.
     * @param supportsImageMetadata Whether the JPEG writer must
     * support JPEG native image metadata.
     */
    protected void initJPEGWriter(boolean supportsStreamMetadata,
                                  boolean supportsImageMetadata) {
        // Reset the writer to null if it does not match preferences.
        if(this.JPEGWriter != null &&
           (supportsStreamMetadata || supportsImageMetadata)) {
            ImageWriterSpi spi = this.JPEGWriter.getOriginatingProvider();
            if(supportsStreamMetadata) {
                String smName = spi.getNativeStreamMetadataFormatName();
                if(smName == null || !smName.equals(STREAM_METADATA_NAME)) {
                    this.JPEGWriter = null;
                }
            }
            if(this.JPEGWriter != null && supportsImageMetadata) {
                String imName = spi.getNativeImageMetadataFormatName();
                if(imName == null || !imName.equals(IMAGE_METADATA_NAME)) {
                    this.JPEGWriter = null;
                }
            }
        }

        // Set the writer.
        if(this.JPEGWriter == null) {
            Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");

            while(iter.hasNext()) {
                // Get a writer.
                ImageWriter writer = iter.next();

                // Verify its metadata support level.
                if(supportsStreamMetadata || supportsImageMetadata) {
                    ImageWriterSpi spi = writer.getOriginatingProvider();
                    if(supportsStreamMetadata) {
                        String smName =
                            spi.getNativeStreamMetadataFormatName();
                        if(smName == null ||
                           !smName.equals(STREAM_METADATA_NAME)) {
                            // Try the next one.
                            continue;
                        }
                    }
                    if(supportsImageMetadata) {
                        String imName =
                            spi.getNativeImageMetadataFormatName();
                        if(imName == null ||
                           !imName.equals(IMAGE_METADATA_NAME)) {
                            // Try the next one.
                            continue;
                        }
                    }
                }

                // Set the writer.
                this.JPEGWriter = writer;
                break;
            }

            if(this.JPEGWriter == null) {
                throw new NullPointerException
                    ("No appropriate JPEG writers found!");
            }
        }

        // Initialize the ImageWriteParam.
        if(this.JPEGParam == null) {
            if(param != null && param instanceof JPEGImageWriteParam) {
                JPEGParam = (JPEGImageWriteParam)param;
            } else {
                JPEGParam =
                    new JPEGImageWriteParam(writer != null ?
                                            writer.getLocale() : null);
                if (param != null && param.getCompressionMode()
                    == ImageWriteParam.MODE_EXPLICIT) {
                    JPEGParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    JPEGParam.setCompressionQuality(param.getCompressionQuality());
                }
            }
        }
    }

    /**
     * Retrieves image metadata with non-core nodes removed.
     */
    private IIOMetadata getImageMetadata(boolean pruneTables)
        throws IIOException {
        if(JPEGImageMetadata == null &&
           IMAGE_METADATA_NAME.equals(JPEGWriter.getOriginatingProvider().getNativeImageMetadataFormatName())) {
            TIFFImageWriter tiffWriter = (TIFFImageWriter)this.writer;

            // Get default image metadata.
            JPEGImageMetadata =
                JPEGWriter.getDefaultImageMetadata(tiffWriter.getImageType(),
                                                   JPEGParam);

            // Get the DOM tree.
            Node tree = JPEGImageMetadata.getAsTree(IMAGE_METADATA_NAME);

            // Remove unwanted marker segments.
            try {
                pruneNodes(tree, pruneTables);
            } catch(IllegalArgumentException e) {
                throw new IIOException("Error pruning unwanted nodes", e);
            }

            // Set the DOM back into the metadata.
            try {
                JPEGImageMetadata.setFromTree(IMAGE_METADATA_NAME, tree);
            } catch(IIOInvalidTreeException e) {
                throw new IIOException
                    ("Cannot set pruned image metadata!", e);
            }
        }

        return JPEGImageMetadata;
    }

    public final int encode(byte[] b, int off,
            int width, int height,
            int[] bitsPerSample,
            int scanlineStride) throws IOException {
        if (this.JPEGWriter == null) {
            throw new IIOException("JPEG writer has not been initialized!");
        }
        if (!((bitsPerSample.length == 3
                && bitsPerSample[0] == 8
                && bitsPerSample[1] == 8
                && bitsPerSample[2] == 8)
                || (bitsPerSample.length == 1
                && bitsPerSample[0] == 8))) {
            throw new IIOException("Can only JPEG compress 8- and 24-bit images!");
        }

        // Set the stream.
        // The stream has to be wrapped as the Java Image I/O JPEG
        // ImageWriter flushes the stream at the end of each write()
        // and this causes problems for the TIFF writer.
        if (baos == null) {
            baos = new IIOByteArrayOutputStream();
        } else {
            baos.reset();
        }
        ImageOutputStream ios = new MemoryCacheImageOutputStream(baos);
        JPEGWriter.setOutput(ios);

        // Create a DataBuffer.
        DataBufferByte dbb;
        if (off == 0) {
            dbb = new DataBufferByte(b, b.length);
        } else {
            //
            // Workaround for bug in core Java Image I/O JPEG
            // ImageWriter which cannot handle non-zero offsets.
            //
            int bytesPerSegment = scanlineStride * height;
            byte[] btmp = new byte[bytesPerSegment];
            System.arraycopy(b, off, btmp, 0, bytesPerSegment);
            dbb = new DataBufferByte(btmp, bytesPerSegment);
            off = 0;
        }

        // Set up the ColorSpace.
        int[] offsets;
        ColorSpace cs;
        if (bitsPerSample.length == 3) {
            offsets = new int[]{off, off + 1, off + 2};
            cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        } else {
            offsets = new int[]{off};
            cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        }

        // Create the ColorModel.
        ColorModel cm = new ComponentColorModel(cs,
                false,
                false,
                Transparency.OPAQUE,
                DataBuffer.TYPE_BYTE);

        // Create the SampleModel.
        SampleModel sm
                = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
                        width, height,
                        bitsPerSample.length,
                        scanlineStride,
                        offsets);

        // Create the WritableRaster.
        WritableRaster wras
                = Raster.createWritableRaster(sm, dbb, new Point(0, 0));

        // Create the BufferedImage.
        BufferedImage bi = new BufferedImage(cm, wras, false, null);

        // Get the pruned JPEG image metadata (may be null).
        IIOMetadata imageMetadata = getImageMetadata(writeAbbreviatedStream);

        // Compress the image into the output stream.
        int compDataLength;
        if (writeAbbreviatedStream) {
            // Write abbreviated JPEG stream

            // First write the tables-only data.
            JPEGWriter.prepareWriteSequence(JPEGStreamMetadata);
            ios.flush();

            // Rewind to the beginning of the byte array.
            baos.reset();

            // Write the abbreviated image data.
            IIOImage image = new IIOImage(bi, null, imageMetadata);
            JPEGWriter.writeToSequence(image, JPEGParam);
            JPEGWriter.endWriteSequence();
        } else {
            // Write complete JPEG stream
            JPEGWriter.write(null,
                    new IIOImage(bi, null, imageMetadata),
                    JPEGParam);
        }

        compDataLength = baos.size();
        baos.writeTo(stream);
        baos.reset();

        return compDataLength;
    }

    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        super.finalize();
        if(JPEGWriter != null) {
            JPEGWriter.dispose();
        }
    }
}
