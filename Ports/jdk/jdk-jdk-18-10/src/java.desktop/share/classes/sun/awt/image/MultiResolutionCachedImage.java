/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package sun.awt.image;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.geom.Dimension2D;
import java.awt.image.ImageObserver;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.awt.image.MultiResolutionImage;
import java.awt.image.AbstractMultiResolutionImage;

public class MultiResolutionCachedImage extends AbstractMultiResolutionImage {

    private final int baseImageWidth;
    private final int baseImageHeight;
    private final Dimension2D[] sizes;
    private final BiFunction<Integer, Integer, Image> mapper;
    private int availableInfo;

    public MultiResolutionCachedImage(int baseImageWidth, int baseImageHeight,
                                      BiFunction<Integer, Integer, Image> mapper)
    {
        this(baseImageWidth, baseImageHeight,
             new Dimension[]{new Dimension( baseImageWidth, baseImageHeight)
        }, mapper);
    }

    public MultiResolutionCachedImage(int baseImageWidth, int baseImageHeight,
                                      Dimension2D[] sizes,
                                      BiFunction<Integer, Integer, Image> mapper)
    {
        this(baseImageWidth, baseImageHeight, sizes, mapper, true);
    }

    private MultiResolutionCachedImage(int baseImageWidth, int baseImageHeight,
                                       Dimension2D[] sizes,
                                       BiFunction<Integer, Integer, Image> mapper,
                                       boolean copySizes)
    {
        this.baseImageWidth = baseImageWidth;
        this.baseImageHeight = baseImageHeight;
        this.sizes = (copySizes && sizes != null)
                                ? Arrays.copyOf(sizes, sizes.length)
                                : sizes;
        this.mapper = mapper;
    }

    @Override
    public Image getResolutionVariant(double destWidth, double destHeight) {
        checkSize(destWidth, destHeight);
        int width = (int) Math.ceil(destWidth);
        int height = (int) Math.ceil(destHeight);
        ImageCache cache = ImageCache.getInstance();
        ImageCacheKey key = new ImageCacheKey(this, width, height);
        Image resolutionVariant = cache.getImage(key);
        if (resolutionVariant == null) {
            resolutionVariant = mapper.apply(width, height);
            cache.setImage(key, resolutionVariant);
        }
        preload(resolutionVariant, availableInfo);
        return resolutionVariant;
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
        return Arrays.stream(sizes).map((Function<Dimension2D, Image>) size
                -> getResolutionVariant(size.getWidth(), size.getHeight()))
                .collect(Collectors.toList());
    }

    public MultiResolutionCachedImage map(Function<Image, Image> mapper) {
        return new MultiResolutionCachedImage(baseImageWidth, baseImageHeight,
                sizes, (width, height) ->
                        mapper.apply(getResolutionVariant(width, height)));
    }

    public static Image map(MultiResolutionImage mrImage,
                            Function<Image, Image> mapper) {

        if (mrImage instanceof MultiResolutionToolkitImage) {
            MultiResolutionToolkitImage mrtImage =
                    (MultiResolutionToolkitImage) mrImage;
            return MultiResolutionToolkitImage.map(mrtImage, mapper);
        }

        BiFunction<Integer, Integer, Image> sizeMapper
                = (w, h) -> mapper.apply(mrImage.getResolutionVariant(w, h));

        if (mrImage instanceof MultiResolutionCachedImage) {
            MultiResolutionCachedImage mrcImage
                    = (MultiResolutionCachedImage) mrImage;

            return new MultiResolutionCachedImage(mrcImage.baseImageWidth,
                                                  mrcImage.baseImageHeight,
                                                  mrcImage.sizes,
                                                  sizeMapper,
                                                  false);
        }

        Image image = (Image) mrImage;
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        return new MultiResolutionCachedImage(width, height, sizeMapper);
    }

    @Override
    public int getWidth(ImageObserver observer) {
        updateInfo(observer, ImageObserver.WIDTH);
        return baseImageWidth;
    }

    @Override
    public int getHeight(ImageObserver observer) {
        updateInfo(observer, ImageObserver.HEIGHT);
        return baseImageHeight;
    }

    @Override
    public Object getProperty(String name, ImageObserver observer) {
        updateInfo(observer, ImageObserver.PROPERTIES);
        return Image.UndefinedProperty;
    }

    @Override
    public Image getScaledInstance(int width, int height, int hints) {
        return getResolutionVariant(width, height);
    }

    @Override
    protected Image getBaseImage() {
        return getResolutionVariant(baseImageWidth, baseImageHeight);
    }

    private void updateInfo(ImageObserver observer, int info) {
        availableInfo |= (observer == null) ? ImageObserver.ALLBITS : info;
    }

    private static int getInfo(Image image) {
        if (image instanceof ToolkitImage) {
            return ((ToolkitImage) image).getImageRep().check(
                    (img, infoflags, x, y, w, h) -> false);
        }
        return 0;
    }

    private static void preload(Image image, int availableInfo) {
        if (availableInfo != 0 && image instanceof ToolkitImage) {
            ((ToolkitImage) image).preload(new ImageObserver() {
                int flags = availableInfo;

                @Override
                public boolean imageUpdate(Image img, int infoflags,
                        int x, int y, int width, int height) {
                    flags &= ~infoflags;
                    return (flags != 0) && ((infoflags
                            & (ImageObserver.ERROR | ImageObserver.ABORT)) == 0);
                }
            });
        }
    }

    private static class ImageCacheKey implements ImageCache.PixelsKey {

        private final int pixelCount;
        private final int hash;

        private final int w;
        private final int h;
        private final Image baseImage;

        ImageCacheKey(final Image baseImage,
                final int w, final int h) {
            this.baseImage = baseImage;
            this.w = w;
            this.h = h;
            this.pixelCount = w * h;
            hash = hash();
        }

        @Override
        public int getPixelCount() {
            return pixelCount;
        }

        private int hash() {
            int hash = baseImage.hashCode();
            hash = 31 * hash + w;
            hash = 31 * hash + h;
            return hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ImageCacheKey) {
                ImageCacheKey key = (ImageCacheKey) obj;
                return baseImage == key.baseImage && w == key.w && h == key.h;
            }
            return false;
        }
    }
}
