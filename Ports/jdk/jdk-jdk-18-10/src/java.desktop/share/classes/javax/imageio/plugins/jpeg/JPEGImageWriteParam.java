/*
 * Copyright (c) 2000, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.imageio.plugins.jpeg;

import java.util.Locale;
import javax.imageio.ImageWriteParam;

import com.sun.imageio.plugins.jpeg.JPEG;

/**
 * This class adds the ability to set JPEG quantization and Huffman
 * tables when using the built-in JPEG writer plug-in, and to request that
 * optimized Huffman tables be computed for an image.  An instance of
 * this class will be returned from the
 * {@code getDefaultImageWriteParam} methods of the built-in JPEG
 * {@code ImageWriter}.

 * <p> The principal purpose of these additions is to allow the
 * specification of tables to use in encoding abbreviated streams.
 * The built-in JPEG writer will also accept an ordinary
 * {@code ImageWriteParam}, in which case the writer will
 * construct the necessary tables internally.
 *
 * <p> In either case, the quality setting in an {@code ImageWriteParam}
 * has the same meaning as for the underlying library: 1.00 means a
 * quantization table of all 1's, 0.75 means the "standard", visually
 * lossless quantization table, and 0.00 means aquantization table of
 * all 255's.
 *
 * <p> While tables for abbreviated streams are often specified by
 * first writing an abbreviated stream containing only the tables, in
 * some applications the tables are fixed ahead of time.  This class
 * allows the tables to be specified directly from client code.
 *
 * <p> Normally, the tables are specified in the
 * {@code IIOMetadata} objects passed in to the writer, and any
 * tables included in these objects are written to the stream.
 * If no tables are specified in the metadata, then an abbreviated
 * stream is written.  If no tables are included in the metadata and
 * no tables are specified in a {@code JPEGImageWriteParam}, then
 * an abbreviated stream is encoded using the "standard" visually
 * lossless tables.  This class is necessary for specifying tables
 * when an abbreviated stream must be written without writing any tables
 * to a stream first.  In order to use this class, the metadata object
 * passed into the writer must contain no tables, and no stream metadata
 * must be provided.  See {@link JPEGQTable JPEGQTable} and
 * {@link JPEGHuffmanTable JPEGHuffmanTable} for more
 * information on the default tables.
 *
 * <p> The default {@code JPEGImageWriteParam} returned by the
 * {@code getDefaultWriteParam} method of the writer contains no
 * tables.  Default tables are included in the default
 * {@code IIOMetadata} objects returned by the writer.
 *
 * <p> If the metadata does contain tables, the tables given in a
 * {@code JPEGImageWriteParam} are ignored.  Furthermore, once a
 * set of tables has been written, only tables in the metadata can
 * override them for subsequent writes, whether to the same stream or
 * a different one.  In order to specify new tables using this class,
 * the {@link javax.imageio.ImageWriter#reset reset}
 * method of the writer must be called.
 *
 * <p>
 * For more information about the operation of the built-in JPEG plug-ins,
 * see the <A HREF="../../metadata/doc-files/jpeg_metadata.html">JPEG
 * metadata format specification and usage notes</A>.
 *
 */
public class JPEGImageWriteParam extends ImageWriteParam {

    private JPEGQTable[] qTables = null;
    private JPEGHuffmanTable[] DCHuffmanTables = null;
    private JPEGHuffmanTable[] ACHuffmanTables = null;
    private boolean optimizeHuffman = false;
    private String[] compressionNames = {"JPEG"};
    private float[] qualityVals = { 0.00F, 0.30F, 0.75F, 1.00F };
    private String[] qualityDescs = {
        "Low quality",       // 0.00 -> 0.30
        "Medium quality",    // 0.30 -> 0.75
        "Visually lossless"  // 0.75 -> 1.00
    };

    /**
     * Constructs a {@code JPEGImageWriteParam}.  Tiling is not
     * supported.  Progressive encoding is supported. The default
     * progressive mode is MODE_DISABLED.  A single form of compression,
     * named "JPEG", is supported.  The default compression quality is
     * 0.75.
     *
     * @param locale a {@code Locale} to be used by the
     * superclass to localize compression type names and quality
     * descriptions, or {@code null}.
     */
    public JPEGImageWriteParam(Locale locale) {
        super(locale);
        this.canWriteProgressive = true;
        this.progressiveMode = MODE_DISABLED;
        this.canWriteCompressed = true;
        this.compressionTypes = compressionNames;
        this.compressionType = compressionTypes[0];
        this.compressionQuality = JPEG.DEFAULT_QUALITY;
    }

    /**
     * Removes any previous compression quality setting.
     *
     * <p> The default implementation resets the compression quality
     * to {@code 0.75F}.
     *
     * @exception IllegalStateException if the compression mode is not
     * {@code MODE_EXPLICIT}.
     */
    public void unsetCompression() {
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException
                ("Compression mode not MODE_EXPLICIT!");
        }
        this.compressionQuality = JPEG.DEFAULT_QUALITY;
    }

    /**
     * Returns {@code false} since the JPEG plug-in only supports
     * lossy compression.
     *
     * @return {@code false}.
     *
     * @exception IllegalStateException if the compression mode is not
     * {@code MODE_EXPLICIT}.
     */
    public boolean isCompressionLossless() {
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException
                ("Compression mode not MODE_EXPLICIT!");
        }
        return false;
    }

    public String[] getCompressionQualityDescriptions() {
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException
                ("Compression mode not MODE_EXPLICIT!");
        }
        if ((getCompressionTypes() != null) &&
            (getCompressionType() == null)) {
            throw new IllegalStateException("No compression type set!");
        }
        return qualityDescs.clone();
    }

    public float[] getCompressionQualityValues() {
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException
                ("Compression mode not MODE_EXPLICIT!");
        }
        if ((getCompressionTypes() != null) &&
            (getCompressionType() == null)) {
            throw new IllegalStateException("No compression type set!");
        }
        return qualityVals.clone();
    }
    /**
     * Returns {@code true} if tables are currently set.
     *
     * @return {@code true} if tables are present.
     */
    public boolean areTablesSet() {
        return (qTables != null);
    }

    /**
     * Sets the quantization and Huffman tables to use in encoding
     * abbreviated streams.  There may be a maximum of 4 tables of
     * each type.  These tables are ignored if tables are specified in
     * the metadata.  All arguments must be non-{@code null}.
     * The two arrays of Huffman tables must have the same number of
     * elements.  The table specifiers in the frame and scan headers
     * in the metadata are assumed to be equivalent to indices into
     * these arrays.  The argument arrays are copied by this method.
     *
     * @param qTables An array of quantization table objects.
     * @param DCHuffmanTables An array of Huffman table objects.
     * @param ACHuffmanTables An array of Huffman table objects.
     *
     * @exception IllegalArgumentException if any of the arguments
     * is {@code null} or has more than 4 elements, or if the
     * numbers of DC and AC tables differ.
     *
     * @see #unsetEncodeTables
     */
    public void setEncodeTables(JPEGQTable[] qTables,
                                JPEGHuffmanTable[] DCHuffmanTables,
                                JPEGHuffmanTable[] ACHuffmanTables) {
        if ((qTables == null) ||
            (DCHuffmanTables == null) ||
            (ACHuffmanTables == null) ||
            (qTables.length > 4) ||
            (DCHuffmanTables.length > 4) ||
            (ACHuffmanTables.length > 4) ||
            (DCHuffmanTables.length != ACHuffmanTables.length)) {
                throw new IllegalArgumentException("Invalid JPEG table arrays");
        }
        this.qTables = qTables.clone();
        this.DCHuffmanTables = DCHuffmanTables.clone();
        this.ACHuffmanTables = ACHuffmanTables.clone();
    }

    /**
     * Removes any quantization and Huffman tables that are currently
     * set.
     *
     * @see #setEncodeTables
     */
    public void unsetEncodeTables() {
        this.qTables = null;
        this.DCHuffmanTables = null;
        this.ACHuffmanTables = null;
    }

    /**
     * Returns a copy of the array of quantization tables set on the
     * most recent call to {@code setEncodeTables}, or
     * {@code null} if tables are not currently set.
     *
     * @return an array of {@code JPEGQTable} objects, or
     * {@code null}.
     *
     * @see #setEncodeTables
     */
    public JPEGQTable[] getQTables() {
        return (qTables != null) ? qTables.clone() : null;
    }

    /**
     * Returns a copy of the array of DC Huffman tables set on the
     * most recent call to {@code setEncodeTables}, or
     * {@code null} if tables are not currently set.
     *
     * @return an array of {@code JPEGHuffmanTable} objects, or
     * {@code null}.
     *
     * @see #setEncodeTables
     */
    public JPEGHuffmanTable[] getDCHuffmanTables() {
        return (DCHuffmanTables != null)
            ? DCHuffmanTables.clone()
            : null;
    }

    /**
     * Returns a copy of the array of AC Huffman tables set on the
     * most recent call to {@code setEncodeTables}, or
     * {@code null} if tables are not currently set.
     *
     * @return an array of {@code JPEGHuffmanTable} objects, or
     * {@code null}.
     *
     * @see #setEncodeTables
     */
    public JPEGHuffmanTable[] getACHuffmanTables() {
        return (ACHuffmanTables != null)
            ? ACHuffmanTables.clone()
            : null;
    }

    /**
     * Tells the writer to generate optimized Huffman tables
     * for the image as part of the writing process.  The
     * default is {@code false}.  If this flag is set
     * to {@code true}, it overrides any tables specified
     * in the metadata.  Note that this means that any image
     * written with this flag set to {@code true} will
     * always contain Huffman tables.
     *
     * @param optimize A boolean indicating whether to generate
     * optimized Huffman tables when writing.
     *
     * @see #getOptimizeHuffmanTables
     */
    public void setOptimizeHuffmanTables(boolean optimize) {
        optimizeHuffman = optimize;
    }

    /**
     * Returns the value passed into the most recent call
     * to {@code setOptimizeHuffmanTables}, or
     * {@code false} if {@code setOptimizeHuffmanTables}
     * has never been called.
     *
     * @return {@code true} if the writer will generate optimized
     * Huffman tables.
     *
     * @see #setOptimizeHuffmanTables
     */
    public boolean getOptimizeHuffmanTables() {
        return optimizeHuffman;
    }
}
