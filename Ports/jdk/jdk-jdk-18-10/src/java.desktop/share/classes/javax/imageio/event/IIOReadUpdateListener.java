/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javax.imageio.event;

import java.awt.image.BufferedImage;
import java.util.EventListener;
import javax.imageio.ImageReader;

/**
 * An interface used by {@code ImageReader} implementations to
 * notify callers of their image and thumbnail reading methods of
 * pixel updates.
 *
 * @see javax.imageio.ImageReader#addIIOReadUpdateListener
 * @see javax.imageio.ImageReader#removeIIOReadUpdateListener
 *
 */
public interface IIOReadUpdateListener extends EventListener {

    /**
     * Reports that the current read operation is about to begin a
     * progressive pass.  Readers of formats that support progressive
     * encoding should use this to notify clients when each pass is
     * completed when reading a progressively encoded image.
     *
     * <p> An estimate of the area that will be updated by the pass is
     * indicated by the {@code minX}, {@code minY},
     * {@code width}, and {@code height} parameters.  If the
     * pass is interlaced, that is, it only updates selected rows or
     * columns, the {@code periodX} and {@code periodY}
     * parameters will indicate the degree of subsampling.  The set of
     * bands that may be affected is indicated by the value of
     * {@code bands}.
     *
     * @param source the {@code ImageReader} object calling this
     * method.
     * @param theImage the {@code BufferedImage} being updated.
     * @param pass the number of the pass that is about to begin,
     * starting with 0.
     * @param minPass the index of the first pass that will be decoded.
     * @param maxPass the index of the last pass that will be decoded.
     * @param minX the X coordinate of the leftmost updated column
     * of pixels.
     * @param minY the Y coordinate of the uppermost updated row
     * of pixels.
     * @param periodX the horizontal spacing between updated pixels;
     * a value of 1 means no gaps.
     * @param periodY the vertical spacing between updated pixels;
     * a value of 1 means no gaps.
     * @param bands an array of {@code int}s indicating the
     * set bands that may be updated.
     */
    void passStarted(ImageReader source,
                     BufferedImage theImage,
                     int pass,
                     int minPass, int maxPass,
                     int minX, int minY,
                     int periodX, int periodY,
                     int[] bands);

    /**
     * Reports that a given region of the image has been updated.
     * The application might choose to redisplay the specified area,
     * for example, in order to provide a progressive display effect,
     * or perform other incremental processing.
     *
     * <p> Note that different image format readers may produce
     * decoded pixels in a variety of different orders.  Many readers
     * will produce pixels in a simple top-to-bottom,
     * left-to-right-order, but others may use multiple passes of
     * interlacing, tiling, etc.  The sequence of updates may even
     * differ from call to call depending on network speeds, for
     * example.  A call to this method does not guarantee that all the
     * specified pixels have actually been updated, only that some
     * activity has taken place within some subregion of the one
     * specified.
     *
     * <p> The particular {@code ImageReader} implementation may
     * choose how often to provide updates.  Each update specifies
     * that a given region of the image has been updated since the
     * last update.  A region is described by its spatial bounding box
     * ({@code minX}, {@code minY}, {@code width}, and
     * {@code height}); X and Y subsampling factors
     * ({@code periodX} and {@code periodY}); and a set of
     * updated bands ({@code bands}).  For example, the update:
     *
     * <pre>
     * minX = 10
     * minY = 20
     * width = 3
     * height = 4
     * periodX = 2
     * periodY = 3
     * bands = { 1, 3 }
     * </pre>
     *
     * would indicate that bands 1 and 3 of the following pixels were
     * updated:
     *
     * <pre>
     * (10, 20) (12, 20) (14, 20)
     * (10, 23) (12, 23) (14, 23)
     * (10, 26) (12, 26) (14, 26)
     * (10, 29) (12, 29) (14, 29)
     * </pre>
     *
     * @param source the {@code ImageReader} object calling this method.
     * @param theImage the {@code BufferedImage} being updated.
     * @param minX the X coordinate of the leftmost updated column
     * of pixels.
     * @param minY the Y coordinate of the uppermost updated row
     * of pixels.
     * @param width the number of updated pixels horizontally.
     * @param height the number of updated pixels vertically.
     * @param periodX the horizontal spacing between updated pixels;
     * a value of 1 means no gaps.
     * @param periodY the vertical spacing between updated pixels;
     * a value of 1 means no gaps.
     * @param bands an array of {@code int}s indicating which
     * bands are being updated.
     */
    void imageUpdate(ImageReader source,
                     BufferedImage theImage,
                     int minX, int minY,
                     int width, int height,
                     int periodX, int periodY,
                     int[] bands);

    /**
     * Reports that the current read operation has completed a
     * progressive pass.  Readers of formats that support
     * progressive encoding should use this to notify clients when
     * each pass is completed when reading a progressively
     * encoded image.
     *
     * @param source the {@code ImageReader} object calling this
     * method.
     * @param theImage the {@code BufferedImage} being updated.
     *
     * @see javax.imageio.ImageReadParam#setSourceProgressivePasses(int, int)
     */
    void passComplete(ImageReader source, BufferedImage theImage);

    /**
     * Reports that the current thumbnail read operation is about to
     * begin a progressive pass.  Readers of formats that support
     * progressive encoding should use this to notify clients when
     * each pass is completed when reading a progressively encoded
     * thumbnail image.
     *
     * @param source the {@code ImageReader} object calling this
     * method.
     * @param theThumbnail the {@code BufferedImage} thumbnail
     * being updated.
     * @param pass the number of the pass that is about to begin,
     * starting with 0.
     * @param minPass the index of the first pass that will be decoded.
     * @param maxPass the index of the last pass that will be decoded.
     * @param minX the X coordinate of the leftmost updated column
     * of pixels.
     * @param minY the Y coordinate of the uppermost updated row
     * of pixels.
     * @param periodX the horizontal spacing between updated pixels;
     * a value of 1 means no gaps.
     * @param periodY the vertical spacing between updated pixels;
     * a value of 1 means no gaps.
     * @param bands an array of {@code int}s indicating the
     * set bands that may be updated.
     *
     * @see #passStarted
     */
    void thumbnailPassStarted(ImageReader source,
                              BufferedImage theThumbnail,
                              int pass,
                              int minPass, int maxPass,
                              int minX, int minY,
                              int periodX, int periodY,
                              int[] bands);

    /**
     * Reports that a given region of a thumbnail image has been updated.
     * The application might choose to redisplay the specified area,
     * for example, in order to provide a progressive display effect,
     * or perform other incremental processing.
     *
     * @param source the {@code ImageReader} object calling this method.
     * @param theThumbnail the {@code BufferedImage} thumbnail
     * being updated.
     * @param minX the X coordinate of the leftmost updated column
     * of pixels.
     * @param minY the Y coordinate of the uppermost updated row
     * of pixels.
     * @param width the number of updated pixels horizontally.
     * @param height the number of updated pixels vertically.
     * @param periodX the horizontal spacing between updated pixels;
     * a value of 1 means no gaps.
     * @param periodY the vertical spacing between updated pixels;
     * a value of 1 means no gaps.
     * @param bands an array of {@code int}s indicating which
     * bands are being updated.
     *
     * @see #imageUpdate
     */
    void thumbnailUpdate(ImageReader source,
                         BufferedImage theThumbnail,
                         int minX, int minY,
                         int width, int height,
                         int periodX, int periodY,
                         int[] bands);

    /**
     * Reports that the current thumbnail read operation has completed
     * a progressive pass.  Readers of formats that support
     * progressive encoding should use this to notify clients when
     * each pass is completed when reading a progressively encoded
     * thumbnail image.
     *
     * @param source the {@code ImageReader} object calling this
     * method.
     * @param theThumbnail the {@code BufferedImage} thumbnail
     * being updated.
     *
     * @see #passComplete
     */
    void thumbnailPassComplete(ImageReader source, BufferedImage theThumbnail);
}
