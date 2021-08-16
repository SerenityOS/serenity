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
 * The main package of the Java Image I/O API.
 * <p>
 * Many common image I/O operations may be performed using the static methods of
 * the {@code ImageIO} class.
 * <p>
 * This package contains the basic classes and interfaces for describing the
 * contents of image files, including metadata and thumbnails
 * ({@code IIOImage}); for controlling the image reading process
 * ({@code ImageReader}, {@code ImageReadParam}, and {@code ImageTypeSpecifier})
 * and image writing process ({@code ImageWriter} and {@code ImageWriteParam});
 * for performing transcoding between formats ({@code ImageTranscoder}), and for
 * reporting errors ({@code IIOException}).
 * <p>
 * All implementations of javax.imageio provide the following standard image
 * format plug-ins:
 *
 * <table class="striped">
 * <caption>Standard image format plug-ins</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Image format
 *     <th scope="col">Reading
 *     <th scope="col">Writing
 *     <th scope="col">Notes
 *     <th scope="col">Metadata
 * </thead>
 * <tbody>
 * <!-- BMP plugin -->
 *   <tr>
 *     <th scope="row">
 *     <a href="https://msdn.microsoft.com/en-us/library/dd183391.aspx">BMP</a>
 *     <td>yes
 *     <td>yes
 *     <td>none
 *     <td><a href='metadata/doc-files/bmp_metadata.html'>BMP
 *     metadata format</a>
 * <!-- GIF plugin -->
 *   <tr>
 *     <th scope="row">
 *     <a href="http://www.w3.org/Graphics/GIF/spec-gif89a.txt">GIF</a>
 *     <td>yes
 *     <td>yes
 *     <td><a href="#gif_plugin_notes">GIF plug-in notes</a>
 *     <td><a href='metadata/doc-files/gif_metadata.html'>GIF
 *     metadata format</a>
 * <!-- JPEG plugin -->
 *   <tr>
 *     <th scope="row"><a href="http://www.jpeg.org">JPEG</a>
 *     <td>yes
 *     <td>yes
 *     <td>none
 *     <td><a href='metadata/doc-files/jpeg_metadata.html'>
 *     JPEG metadata format</a>
 * <!-- PNG plugin -->
 *   <tr>
 *     <th scope="row"><a href="http://www.libpng.org/pub/png/spec/">PNG</a>
 *     <td>yes
 *     <td>yes
 *     <td>none
 *     <td><a href='metadata/doc-files/png_metadata.html'>PNG
 *     metadata format</a>
 * <!-- TIFF plugin -->
 *   <tr>
 *     <th scope="row">
 *     <a href="https://www.itu.int/itudoc/itu-t/com16/tiff-fx/docs/tiff6.pdf">
 *     TIFF</a>
 *     <td>yes
 *     <td>yes
 *     <td>
 *     <a href='metadata/doc-files/tiff_metadata.html#Reading'>TIFF plug-in
 *     notes</a>
 *     <td>
 *     <a href='metadata/doc-files/tiff_metadata.html#StreamMetadata'>TIFF
 *     metadata format</a>
 * <!-- WBMP plugin -->
 *   <tr>
 *     <th scope="row">
 *     <a href="http://www.wapforum.org/what/technical/SPEC-WAESpec-19990524.pdf">
 *     WBMP</a>
 *     <td>yes
 *     <td>yes
 *     <td>none
 *     <td><a href='metadata/doc-files/wbmp_metadata.html'>
 *     WBMP metadata format</a>
 * </tbody>
 * </table>
 *
 * <h2> Standard Plug-in Notes</h2>
 *
 * <h3><a id="gif_plugin_notes">Standard plug-in for GIF image format</a></h3>
 * ImageIO provides {@code ImageReader} and {@code ImageWriter}plug-ins for the
 * <a href="http://www.w3.org/Graphics/GIF/spec-gif89a.txt"> Graphics
 * Interchange Format (GIF)</a> image format. These are the "standard" GIF
 * plug-ins, meaning those that are included in the JRE, as distinct from those
 * included in standard extensions, or 3rd party plug-ins. The following notes
 * and metadata specification apply to the standard plug-ins.
 *
 * <h3>Writing GIF images</h3>
 * The GIF image writer plug-in guarantees lossless writing for images which
 * meet the following requirements:
 * <ul>
 *     <li>the number of bands is 1;</li>
 *     <li>the number of bits per sample is not greater than 8;</li>
 *     <li>the size of a color component is not greater than 8;</li>
 * </ul>
 * <p>
 * By default the GIF writer plug-in creates version "89a" images. This can be
 * changed to "87a" by explicitly setting the version in the stream metadata
 * (see
 * <a href="metadata/doc-files/gif_metadata.html#gif_stream_metadata_format">
 * GIF Stream Metadata Format Specification</a>).
 *
 * <!-- animated images -->
 * <p>
 * The GIF writer plug-in supports the creation of animated GIF images through
 * the standard sequence writing methods defined in the {@code ImageWriter}
 * class.
 *
 * <!-- TODO: add example here -->
 *
 * <!--  color tables -->
 * <p>
 * A global color table is written to the output stream if one of the following
 * conditions is met:
 * <ul>
 *     <li>stream metadata containing a GlobalColorTable element is supplied;
 *     </li>
 *     <li>a sequence is being written and image metadata containing a
 *     LocalColorTable element is supplied for the first image in the sequence;
 *     </li>
 *     <li>image metadata is not supplied or does not contain a LocalColorTable
 *     element.</li>
 * </ul>
 * <p>
 * In the first case the global color table in the stream metadata is used, in
 * the second the local color table in the image metadata is used, and in the
 * third a global color table is created from the ColorModel or SampleModel of
 * the (first) image.
 * <p>
 * A local color table is written to the output stream only if image metadata
 * containing a LocalColorTable element is supplied to the writer, or no image
 * metadata is supplied to the writer and the local color table which would be
 * generated from the image itself is not equal to the global color table.
 * <p>
 * A Graphic Control Extension block is written to the output stream only if
 * image metadata containing a GraphicControlExtension element is supplied to
 * the writer, or no image metadata is supplied and the local color table
 * generated from the image requires a transparent index. Application, Plain
 * Text, and Comment Extension blocks are written only if they are supplied to
 * the writer via image metadata.
 *
 * <!-- writing interlaced images -->
 * <p>
 * The writing of interlaced images can be controlled by the progressive mode of
 * the provided {@code ImageWriteParam} instance. If progressive mode is
 * {@code MODE_DISABLED} then a non-interlaced image will be written. If
 * progressive mode is {@code MODE_DEFAULT} then an interlaced image will be
 * written. If progressive mode is {@code MODE_COPY_FROM_METADATA}, then the
 * metadata setting is used (if it is provided, otherwise an interlaced image
 * will be written).
 * <p>
 * The GIF image writer plug-in supports setting output stream metadata from
 * metadata supplied to the writer in either the native GIF stream metadata
 * format
 * <a href="metadata/doc-files/gif_metadata.html#gif_stream_metadata_format">
 * javax_imageio_gif_stream_1.0</a> or the standard metadata format
 * <a href="metadata/doc-files/standard_metadata.html">javax_imageio_1.0</a>,
 * and setting output image metadata from metadata supplied to the writer in
 * either the native GIF image metadata format
 * <a href="metadata/doc-files/gif_metadata.html#gif_image_metadata_format">
 * javax_imageio_gif_image_1.0</a> or the standard metadata format
 * <a href="metadata/doc-files/standard_metadata.html">javax_imageio_1.0</a>.
 * The mapping of standard metadata format to the GIF native stream and image
 * metadata formats is given in the tables
 * <a href="metadata/doc-files/gif_metadata.html#mapping">here</a>.
 *
 * @since 1.4
 */
package javax.imageio;
