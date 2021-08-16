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

import javax.imageio.ImageReadParam;

/**
 * This class adds the ability to set JPEG quantization and Huffman
 * tables when using the built-in JPEG reader plug-in.  An instance of
 * this class will be returned from the
 * {@code getDefaultImageReadParam} methods of the built-in JPEG
 * {@code ImageReader}.
 *
 * <p> The sole purpose of these additions is to allow the
 * specification of tables for use in decoding abbreviated streams.
 * The built-in JPEG reader will also accept an ordinary
 * {@code ImageReadParam}, which is sufficient for decoding
 * non-abbreviated streams.
 *
 * <p> While tables for abbreviated streams are often obtained by
 * first reading another abbreviated stream containing only the
 * tables, in some applications the tables are fixed ahead of time.
 * This class allows the tables to be specified directly from client
 * code.  If no tables are specified either in the stream or in a
 * {@code JPEGImageReadParam}, then the stream is presumed to use
 * the "standard" visually lossless tables.  See {@link JPEGQTable JPEGQTable}
 * and {@link JPEGHuffmanTable JPEGHuffmanTable} for more information
 *  on the default tables.
 *
 * <p> The default {@code JPEGImageReadParam} returned by the
 * {@code getDefaultReadParam} method of the builtin JPEG reader
 * contains no tables.  Default tables may be obtained from the table
 * classes {@link JPEGQTable JPEGQTable} and
 * {@link JPEGHuffmanTable JPEGHuffmanTable}.
 *
 * <p> If a stream does contain tables, the tables given in a
 * {@code JPEGImageReadParam} are ignored.  Furthermore, if the
 * first image in a stream does contain tables and subsequent ones do
 * not, then the tables given in the first image are used for all the
 * abbreviated images.  Once tables have been read from a stream, they
 * can be overridden only by tables subsequently read from the same
 * stream.  In order to specify new tables, the {@link
 * javax.imageio.ImageReader#setInput setInput} method of
 * the reader must be called to change the stream.
 *
 * <p> Note that this class does not provide a means for obtaining the
 * tables found in a stream.  These may be extracted from a stream by
 * consulting the IIOMetadata object returned by the reader.
 *
 * <p>
 * For more information about the operation of the built-in JPEG plug-ins,
 * see the <A HREF="../../metadata/doc-files/jpeg_metadata.html">JPEG
 * metadata format specification and usage notes</A>.
 *
 */
public class JPEGImageReadParam extends ImageReadParam {

    private JPEGQTable[] qTables = null;
    private JPEGHuffmanTable[] DCHuffmanTables = null;
    private JPEGHuffmanTable[] ACHuffmanTables = null;

    /**
     * Constructs a {@code JPEGImageReadParam}.
     */
    public JPEGImageReadParam() {
        super();
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
     * Sets the quantization and Huffman tables to use in decoding
     * abbreviated streams.  There may be a maximum of 4 tables of
     * each type.  These tables are ignored once tables are
     * encountered in the stream.  All arguments must be
     * non-{@code null}.  The two arrays of Huffman tables must
     * have the same number of elements.  The table specifiers in the
     * frame and scan headers in the stream are assumed to be
     * equivalent to indices into these arrays.  The argument arrays
     * are copied by this method.
     *
     * @param qTables an array of quantization table objects.
     * @param DCHuffmanTables an array of Huffman table objects.
     * @param ACHuffmanTables an array of Huffman table objects.
     *
     * @exception IllegalArgumentException if any of the arguments
     * is {@code null}, has more than 4 elements, or if the
     * numbers of DC and AC tables differ.
     *
     * @see #unsetDecodeTables
     */
    public void setDecodeTables(JPEGQTable[] qTables,
                                JPEGHuffmanTable[] DCHuffmanTables,
                                JPEGHuffmanTable[] ACHuffmanTables) {
        if ((qTables == null) ||
            (DCHuffmanTables == null) ||
            (ACHuffmanTables == null) ||
            (qTables.length > 4) ||
            (DCHuffmanTables.length > 4) ||
            (ACHuffmanTables.length > 4) ||
            (DCHuffmanTables.length != ACHuffmanTables.length)) {
                throw new IllegalArgumentException
                    ("Invalid JPEG table arrays");
        }
        this.qTables = qTables.clone();
        this.DCHuffmanTables = DCHuffmanTables.clone();
        this.ACHuffmanTables = ACHuffmanTables.clone();
    }

    /**
     * Removes any quantization and Huffman tables that are currently
     * set.
     *
     * @see #setDecodeTables
     */
    public void unsetDecodeTables() {
        this.qTables = null;
        this.DCHuffmanTables = null;
        this.ACHuffmanTables = null;
    }

    /**
     * Returns a copy of the array of quantization tables set on the
     * most recent call to {@code setDecodeTables}, or
     * {@code null} if tables are not currently set.
     *
     * @return an array of {@code JPEGQTable} objects, or
     * {@code null}.
     *
     * @see #setDecodeTables
     */
    public JPEGQTable[] getQTables() {
        return (qTables != null) ? qTables.clone() : null;
    }

    /**
     * Returns a copy of the array of DC Huffman tables set on the
     * most recent call to {@code setDecodeTables}, or
     * {@code null} if tables are not currently set.
     *
     * @return an array of {@code JPEGHuffmanTable} objects, or
     * {@code null}.
     *
     * @see #setDecodeTables
     */
    public JPEGHuffmanTable[] getDCHuffmanTables() {
        return (DCHuffmanTables != null)
            ? DCHuffmanTables.clone()
            : null;
    }

    /**
     * Returns a copy of the array of AC Huffman tables set on the
     * most recent call to {@code setDecodeTables}, or
     * {@code null} if tables are not currently set.
     *
     * @return an array of {@code JPEGHuffmanTable} objects, or
     * {@code null}.
     *
     * @see #setDecodeTables
     */
    public JPEGHuffmanTable[] getACHuffmanTables() {
        return (ACHuffmanTables != null)
            ? ACHuffmanTables.clone()
            : null;
    }
}
