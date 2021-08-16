/*
 * Copyright (c) 1995, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.image.ImageFilter;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.util.Hashtable;
import java.awt.image.ColorModel;

/**
 * This class is an implementation of the ImageProducer interface which
 * takes an existing image and a filter object and uses them to produce
 * image data for a new filtered version of the original image. Furthermore,
 * {@code FilteredImageSource} is safe for use by multiple threads.
 * Here is an example which filters an image by swapping the red and
 * blue components:
 * <pre>
 *
 *      Image src = getImage("doc:///demo/images/duke/T1.gif");
 *      ImageFilter colorfilter = new RedBlueSwapFilter();
 *      Image img = createImage(new FilteredImageSource(src.getSource(),
 *                                                      colorfilter));
 *
 * </pre>
 *
 * @see ImageProducer
 *
 * @author      Jim Graham
 */
public class FilteredImageSource implements ImageProducer {
    ImageProducer src;
    ImageFilter filter;

    /**
     * Constructs an ImageProducer object from an existing ImageProducer
     * and a filter object.
     * @param orig the specified {@code ImageProducer}
     * @param imgf the specified {@code ImageFilter}
     * @see ImageFilter
     * @see java.awt.Component#createImage
     */
    public FilteredImageSource(ImageProducer orig, ImageFilter imgf) {
        src = orig;
        filter = imgf;
    }

    private Hashtable<ImageConsumer, ImageFilter> proxies;

    /**
     * Adds the specified {@code ImageConsumer}
     * to the list of consumers interested in data for the filtered image.
     * An instance of the original {@code ImageFilter}
     * is created
     * (using the filter's {@code getFilterInstance} method)
     * to manipulate the image data
     * for the specified {@code ImageConsumer}.
     * The newly created filter instance
     * is then passed to the {@code addConsumer} method
     * of the original {@code ImageProducer}.
     *
     * <p>
     * This method is public as a side effect
     * of this class implementing
     * the {@code ImageProducer} interface.
     * It should not be called from user code,
     * and its behavior if called from user code is unspecified.
     *
     * @param ic  the consumer for the filtered image
     * @see ImageConsumer
     */
    public synchronized void addConsumer(ImageConsumer ic) {
        if (proxies == null) {
            proxies = new Hashtable<>();
        }
        if (!proxies.containsKey(ic)) {
            ImageFilter imgf = filter.getFilterInstance(ic);
            proxies.put(ic, imgf);
            src.addConsumer(imgf);
        }
    }

    /**
     * Determines whether an ImageConsumer is on the list of consumers
     * currently interested in data for this image.
     *
     * <p>
     * This method is public as a side effect
     * of this class implementing
     * the {@code ImageProducer} interface.
     * It should not be called from user code,
     * and its behavior if called from user code is unspecified.
     *
     * @param ic the specified {@code ImageConsumer}
     * @return true if the ImageConsumer is on the list; false otherwise
     * @see ImageConsumer
     */
    public synchronized boolean isConsumer(ImageConsumer ic) {
        return (proxies != null && proxies.containsKey(ic));
    }

    /**
     * Removes an ImageConsumer from the list of consumers interested in
     * data for this image.
     *
     * <p>
     * This method is public as a side effect
     * of this class implementing
     * the {@code ImageProducer} interface.
     * It should not be called from user code,
     * and its behavior if called from user code is unspecified.
     *
     * @see ImageConsumer
     */
    public synchronized void removeConsumer(ImageConsumer ic) {
        if (proxies != null) {
            ImageFilter imgf =  proxies.get(ic);
            if (imgf != null) {
                src.removeConsumer(imgf);
                proxies.remove(ic);
                if (proxies.isEmpty()) {
                    proxies = null;
                }
            }
        }
    }

    /**
     * Starts production of the filtered image.
     * If the specified {@code ImageConsumer}
     * isn't already a consumer of the filtered image,
     * an instance of the original {@code ImageFilter}
     * is created
     * (using the filter's {@code getFilterInstance} method)
     * to manipulate the image data
     * for the {@code ImageConsumer}.
     * The filter instance for the {@code ImageConsumer}
     * is then passed to the {@code startProduction} method
     * of the original {@code ImageProducer}.
     *
     * <p>
     * This method is public as a side effect
     * of this class implementing
     * the {@code ImageProducer} interface.
     * It should not be called from user code,
     * and its behavior if called from user code is unspecified.
     *
     * @param ic  the consumer for the filtered image
     * @see ImageConsumer
     */
    public synchronized void startProduction(ImageConsumer ic) {
        if (proxies == null) {
            proxies = new Hashtable<>();
        }
        ImageFilter imgf = proxies.get(ic);
        if (imgf == null) {
            imgf = filter.getFilterInstance(ic);
            proxies.put(ic, imgf);
        }
        src.startProduction(imgf);
    }

    /**
     * Requests that a given ImageConsumer have the image data delivered
     * one more time in top-down, left-right order.  The request is
     * handed to the ImageFilter for further processing, since the
     * ability to preserve the pixel ordering depends on the filter.
     *
     * <p>
     * This method is public as a side effect
     * of this class implementing
     * the {@code ImageProducer} interface.
     * It should not be called from user code,
     * and its behavior if called from user code is unspecified.
     *
     * @see ImageConsumer
     */
    public synchronized void requestTopDownLeftRightResend(ImageConsumer ic) {
        if (proxies != null) {
            ImageFilter imgf = proxies.get(ic);
            if (imgf != null) {
                imgf.resendTopDownLeftRight(src);
            }
        }
    }
}
