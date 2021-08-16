/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Locale;
import javax.imageio.ImageWriteParam;

/**
 * A subclass of {@link ImageWriteParam ImageWriteParam}
 * allowing control over the TIFF writing process. The set of innately
 * supported compression types is listed in the following table:
 *
 * <table border=1>
 * <caption><b>Supported Compression Types</b></caption>
 * <tr><th>Compression Type</th> <th>Description</th> <th>Reference</th></tr>
 * <tr>
 * <td>CCITT RLE</td>
 * <td>Modified Huffman compression</td>
 * <td>TIFF 6.0 Specification, Section 10</td>
 * </tr>
 * <tr>
 * <td>CCITT T.4</td>
 * <td>CCITT T.4 bilevel encoding/Group 3 facsimile compression</td>
 * <td>TIFF 6.0 Specification, Section 11</td>
 * </tr>
 * <tr>
 * <td>CCITT T.6</td>
 * <td>CCITT T.6 bilevel encoding/Group 4 facsimile compression</td>
 * <td>TIFF 6.0 Specification, Section 11</td></tr>
 * <tr>
 * <td>LZW</td>
 * <td>LZW compression</td>
 * <td>TIFF 6.0 Specification, Section 13</td></tr>
 * <tr>
 * <td>JPEG</td>
 * <td>"New" JPEG-in-TIFF compression</td>
 * <td>TIFF Technical Note #2</td></tr>
 * <tr>
 * <td>ZLib</td>
 * <td>"Deflate/Inflate" compression (see note following this table)</td>
 * <td>Adobe Photoshop&#174; TIFF Technical Notes</td>
 * </tr>
 * <tr>
 * <td>PackBits</td>
 * <td>Byte-oriented, run length compression</td>
 * <td>TIFF 6.0 Specification, Section 9</td>
 * </tr>
 * <tr>
 * <td>Deflate</td>
 * <td>"Zip-in-TIFF" compression (see note following this table)</td>
 * <td><a href="http://www.isi.edu/in-notes/rfc1950.txt">
 * ZLIB Compressed Data Format Specification</a>,
 * <a href="http://www.isi.edu/in-notes/rfc1951.txt">
 * DEFLATE Compressed Data Format Specification</a></td>
 * </tr>
 * <tr>
 * <td>Exif JPEG</td>
 * <td>Exif-specific JPEG compression (see note following this table)</td>
 * <td><a href="http://www.exif.org/Exif2-2.PDF">Exif 2.2 Specification</a>
 * (PDF), section 4.5.5, "Basic Structure of Thumbnail Data"</td>
 * </table>
 *
 * <p>
 * Old-style JPEG compression as described in section 22 of the TIFF 6.0
 * Specification is <i>not</i> supported.
 * </p>
 *
 * <p> The CCITT compression types are applicable to bilevel (1-bit)
 * images only.  The JPEG compression type is applicable to byte
 * grayscale (1-band) and RGB (3-band) images only.</p>
 *
 * <p>
 * ZLib and Deflate compression are identical except for the value of the
 * TIFF Compression field: for ZLib the Compression field has value 8
 * whereas for Deflate it has value 32946 (0x80b2). In both cases each
 * image segment (strip or tile) is written as a single complete zlib data
 * stream.
 * </p>
 *
 * <p>
 * "Exif JPEG" is a compression type used when writing the contents of an
 * APP1 Exif marker segment for inclusion in a JPEG native image metadata
 * tree. The contents appended to the output when this compression type is
 * used are a function of whether an empty or non-empty image is written.
 * If the image is empty, then a TIFF IFD adhering to the specification of
 * a compressed Exif primary IFD is appended. If the image is non-empty,
 * then a complete IFD and image adhering to the specification of a
 * compressed Exif thumbnail IFD and image are appended. Note that the
 * data of the empty image may <i>not</i> later be appended using the pixel
 * replacement capability of the TIFF writer.
 * </p>
 *
 * <p> If ZLib/Deflate or JPEG compression is used, the compression quality
 * may be set. For ZLib/Deflate the supplied floating point quality value is
 * rescaled to the range <tt>[1,&nbsp;9]</tt> and truncated to an integer
 * to derive the Deflate compression level. For JPEG the floating point
 * quality value is passed directly to the JPEG writer plug-in which
 * interprets it in the usual way.</p>
 *
 * <p> The {@code canWriteTiles} and
 * {@code canWriteCompressed} methods will return
 * {@code true}; the {@code canOffsetTiles} and
 * {@code canWriteProgressive} methods will return
 * {@code false}.</p>
 *
 * <p> If tiles are being written, then each of their dimensions will be
 * rounded to the nearest multiple of 16 per the TIFF specification. If
 * JPEG-in-TIFF compression is being used, and tiles are being written
 * each tile dimension will be rounded to the nearest multiple of 8 times
 * the JPEG minimum coded unit (MCU) in that dimension. If JPEG-in-TIFF
 * compression is being used and strips are being written, the number of
 * rows per strip is rounded to a multiple of 8 times the maximum MCU over
 * both dimensions.</p>
 */
public class TIFFImageWriteParam extends ImageWriteParam {

    /**
     * Constructs a {@code TIFFImageWriteParam} instance
     * for a given {@code Locale}.
     *
     * @param locale the {@code Locale} for which messages
     * should be localized.
     */
    public TIFFImageWriteParam(Locale locale) {
        super(locale);
        this.canWriteCompressed = true;
        this.canWriteTiles = true;
        this.compressionTypes = TIFFImageWriter.TIFFCompressionTypes;
    };
}
