/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package java.awt.image;

import java.awt.Image;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

/**
 * This class is an array-based implementation of
 * the {@code AbstractMultiResolutionImage} class.
 *
 * This class will implement the
 * {@code getResolutionVariant(double destImageWidth, double destImageHeight)}
 * method using a simple algorithm which will return the first image variant
 * in the array that is large enough to satisfy the rendering request. The
 * last image in the array will be returned if no suitable image is found
 * that is as large as the rendering request.
 * <p>
 * For best effect the array of images should be sorted with each image being
 * both wider and taller than the previous image.  The base image need not be
 * the first image in the array. No exception will be thrown if the images
 * are not sorted as suggested.
 *
 * @see java.awt.Image
 * @see java.awt.image.MultiResolutionImage
 * @see java.awt.image.AbstractMultiResolutionImage
 *
 * @since 9
 */
public class BaseMultiResolutionImage extends AbstractMultiResolutionImage {

    private final int baseImageIndex;
    private final Image[] resolutionVariants;

    /**
     * Creates a multi-resolution image with the given resolution variants.
     * The first resolution variant is used as the base image.
     *
     * @param resolutionVariants array of resolution variants sorted by image size
     * @throws IllegalArgumentException if null or zero-length array is passed
     * @throws NullPointerException if the specified {@code resolutionVariants}
     *          contains one or more null elements
     *
     * @since 9
     */
    public BaseMultiResolutionImage(Image... resolutionVariants) {
        this(0, resolutionVariants);
    }

    /**
     * Creates a multi-resolution image with the given base image index and
     * resolution variants.
     *
     * @param baseImageIndex the index of base image in the resolution variants
     *        array
     * @param resolutionVariants array of resolution variants sorted by image size
     * @throws IllegalArgumentException if null or zero-length array is passed
     * @throws NullPointerException if the specified {@code resolutionVariants}
     *          contains one or more null elements
     * @throws IndexOutOfBoundsException if {@code baseImageIndex} is
     *          negative or greater than or equal to {@code resolutionVariants}
     *          length.
     *
     * @since 9
     */
    public BaseMultiResolutionImage(int baseImageIndex,
                                    Image... resolutionVariants) {

        if (resolutionVariants == null || resolutionVariants.length == 0) {
            throw new IllegalArgumentException(
                    "Null or zero-length array is passed");
        }

        if (baseImageIndex < 0 || baseImageIndex >= resolutionVariants.length) {
            throw new IndexOutOfBoundsException("Invalid base image index: "
                    + baseImageIndex);
        }

        this.baseImageIndex = baseImageIndex;
        this.resolutionVariants = Arrays.copyOf(resolutionVariants,
                                                resolutionVariants.length);

        for (Image resolutionVariant : this.resolutionVariants) {
            Objects.requireNonNull(resolutionVariant,
                                   "Resolution variant can't be null");
        }
    }

    @Override
    public Image getResolutionVariant(double destImageWidth,
                                      double destImageHeight) {

        checkSize(destImageWidth, destImageHeight);

        for (Image rvImage : resolutionVariants) {
            if (destImageWidth <= rvImage.getWidth(null)
                    && destImageHeight <= rvImage.getHeight(null)) {
                return rvImage;
            }
        }
        return resolutionVariants[resolutionVariants.length - 1];
    }

    private static void checkSize(double width, double height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(String.format(
                    "Width (%s) or height (%s) cannot be <= 0", width, height));
        }

        if (!Double.isFinite(width) || !Double.isFinite(height)) {
            throw new IllegalArgumentException(String.format(
                    "Width (%s) or height (%s) is not finite", width, height));
        }
    }

    @Override
    public List<Image> getResolutionVariants() {
        return Collections.unmodifiableList(Arrays.asList(resolutionVariants));
    }

    @Override
    protected Image getBaseImage() {
        return resolutionVariants[baseImageIndex];
    }
}
