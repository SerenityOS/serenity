/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Image;
import java.awt.image.ImageObserver;
import java.awt.image.MultiResolutionImage;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

public class MultiResolutionToolkitImage extends ToolkitImage implements MultiResolutionImage {

    Image resolutionVariant;

    public MultiResolutionToolkitImage(Image lowResolutionImage, Image resolutionVariant) {
        super(lowResolutionImage.getSource());
        this.resolutionVariant = resolutionVariant;
    }

    @Override
    public Image getResolutionVariant(double destWidth, double destHeight) {
        checkSize(destWidth, destHeight);
        return ((destWidth <= getWidth() && destHeight <= getHeight()))
                ? this : resolutionVariant;
    }

    public static Image map(MultiResolutionToolkitImage mrImage,
                            Function<Image, Image> mapper) {
        Image baseImage = mapper.apply(mrImage);
        Image rvImage = mapper.apply(mrImage.resolutionVariant);
        return new MultiResolutionToolkitImage(baseImage, rvImage);
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

    public Image getResolutionVariant() {
        return resolutionVariant;
    }

    @Override
    public List<Image> getResolutionVariants() {
        return Arrays.<Image>asList(this, resolutionVariant);
    }

    private static final int BITS_INFO = ImageObserver.SOMEBITS
            | ImageObserver.FRAMEBITS | ImageObserver.ALLBITS;

    public static ImageObserver getResolutionVariantObserver(
            final Image image, final ImageObserver observer,
            final int imgWidth, final int imgHeight,
            final int rvWidth, final int rvHeight) {
        return getResolutionVariantObserver(image, observer,
                imgWidth, imgHeight, rvWidth, rvHeight, false);
    }

    public static ImageObserver getResolutionVariantObserver(
            final Image image, final ImageObserver observer,
            final int imgWidth, final int imgHeight,
            final int rvWidth, final int rvHeight, boolean concatenateInfo) {

        if (observer == null) {
            return null;
        }

        synchronized (ObserverCache.INSTANCE) {
            return ObserverCache.INSTANCE.computeIfAbsent(observer,
                    key -> new ObserverCache(key, concatenateInfo, image));
        }
    }

    private static final class ObserverCache implements ImageObserver {

        private static final Map<ImageObserver, ImageObserver> INSTANCE =
                new WeakHashMap<>();

        private final boolean concat;
        private final WeakReference<Image> imageRef;
        private final WeakReference<ImageObserver> observerRef;

        private ObserverCache(ImageObserver obs, boolean concat, Image img) {
            this.concat = concat;
            imageRef = new WeakReference<>(img);
            observerRef = new WeakReference<>(obs);
        }

        @Override
        public boolean imageUpdate(Image img, int infoflags,
                                   int x, int y, int width, int height) {
            ImageObserver observer = observerRef.get();
            Image image = imageRef.get();

            if (observer == null || image == null) {
                return false;
            }

            if ((infoflags & (ImageObserver.WIDTH | BITS_INFO)) != 0) {
                width = (width + 1) / 2;
            }

            if ((infoflags & (ImageObserver.HEIGHT | BITS_INFO)) != 0) {
                height = (height + 1) / 2;
            }

            if ((infoflags & BITS_INFO) != 0) {
                x /= 2;
                y /= 2;
            }

            if (concat) {
                infoflags &= ((ToolkitImage) image).getImageRep().check(null);
            }
            return observer.imageUpdate(image, infoflags, x, y, width, height);
        }
    }
}
