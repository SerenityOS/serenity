/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A package of the Java Image I/O API dealing with reading and writing
 * metadata.
 * <p>
 * When reading an image, its per-stream and per-image metadata is made
 * available as an {@code IIOMetadata} object. The internals of this object are
 * specific to the plug-in that created it. Its contents may be accessed in the
 * form of an XML {@code Document}, which is implemented as a tree of
 * {@code IIOMetadataNode} objects.
 * <p>
 * When writing an image, its metadata may be set by defining or modifying an
 * {@code IIOMetadata} object. Such an object may be obtained from an
 * {@code ImageWriter} or {@code ImageTranscoder} (from the
 * {@code javax.imageio} package). Once such an object has been obtained, its
 * contents may be set of modified via a {@code Document} consisting of
 * {@code IIOMetadataNode}s. The document format may optionally be described
 * using an {@code IIOMetadataFormat} object.
 * <p>
 * The format of the metadata contained in the XML {@code Document} is
 * identified by a string which appears as the root node of the tree of
 * {@code IIOMetadataNode} objects. This string contains a version number, e.g.
 * "javax_imageio_jpeg_image_1.0". Readers and writers may support multiple
 * versions of the same basic format and the Image I/O API has methods that
 * allow specifying which version to use by passing the string to the
 * method/constructor used to obtain an {@code IIOMetadata} object. In some
 * cases, a more recent version may not be strictly compatible with a program
 * written expecting an older version (for an example, see the Native Metadata
 * Format section of the JPEG Metadata Usage Notes below).
 * <p>
 * Plug-ins may choose to support a
 * <A HREF="doc-files/standard_metadata.html">standard (plug-in neutral) format
 * </A>. This format does not provide lossless encoding of metadata, but allows
 * a portion of the metadata to be accessed in a generic manner.
 * <p>
 * Each of the standard plug-ins supports a so-called "native" metadata format,
 * which encodes its metadata losslessly:
 * <ul>
 *     <li><A HREF="doc-files/bmp_metadata.html">BMP metadata</A></li>
 *     <li><A HREF="doc-files/gif_metadata.html">GIF metadata</A></li>
 *     <li><A HREF="doc-files/jpeg_metadata.html">JPEG metadata</A></li>
 *     <li><A HREF="doc-files/png_metadata.html">PNG metadata</A></li>
 *     <li><A HREF="doc-files/tiff_metadata.html#StreamMetadata">
 *         TIFF metadata</A></li>
 *     <li><A HREF="doc-files/wbmp_metadata.html">WBMP metadata</A></li>
 * </ul>
 * @since 1.4
 */
package javax.imageio.metadata;
