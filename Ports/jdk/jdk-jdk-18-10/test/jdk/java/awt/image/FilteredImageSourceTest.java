/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * @test
 * @bug 8188083
 * @summary The test checks whether applying image filters using
 *          FilteredImageSource results in a NullPointerException.
 * @run main FilteredImageSourceTest
 */
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.ColorModel;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageFilter;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.util.Hashtable;

/*
 * An empty image consumer that will be added to the list of consumers
 * interested in image data for the filtered image.
 */
class EmptyImageConsumer implements ImageConsumer {
    @Override
    public void setDimensions(int width, int height) {
    }

    @Override
    public void setProperties(Hashtable<?, ?> props) {
    }

    @Override
    public void setColorModel(ColorModel colorModel) {
    }

    @Override
    public void setHints(int hintFlags) {
    }

    @Override
    public void setPixels(int x, int y, int width, int height,
                          ColorModel colorModel, byte[] pixels,
                          int offset, int scanSize) {
    }

    @Override
    public void setPixels(int x, int y, int width, int height,
                          ColorModel colorModel, int[] pixels,
                          int offset, int scanSize) {
    }

    @Override
    public void imageComplete(int i) {
    }
}

/*
 * An empty image producer whose sole purpose is to provide stub methods
 * that will be invoked while preparing filtered image.
 */
class EmptyImageProducer implements ImageProducer {
    @Override
    public void addConsumer(ImageConsumer imageConsumer) {
    }

    @Override
    public boolean isConsumer(ImageConsumer imageConsumer) {
        return false;
    }

    @Override
    public void removeConsumer(ImageConsumer imageConsumer) {
    }

    @Override
    public void startProduction(ImageConsumer imageConsumer) {
    }

    @Override
    public void requestTopDownLeftRightResend(ImageConsumer imageConsumer) {
    }
}

/*
 * Typically, an Image object will contain an ImageProducer that prepares
 * image data. FilteredImageSource will be set as image producer for images
 * that require image filter applied to image data.
 */
class EmptyFilteredImage extends Image {
    ImageFilter filter = null;
    ImageProducer producer = null;

    public EmptyFilteredImage(ImageProducer imgSource) {
        filter = new ImageFilter();
        producer = new FilteredImageSource(imgSource, filter);
    }

    @Override
    public int getWidth(ImageObserver observer) {
        return 100;
    }

    @Override
    public int getHeight(ImageObserver observer) {
        return 100;
    }

    @Override
    public ImageProducer getSource() {
        return producer;
    }

    @Override
    public Graphics getGraphics() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getProperty(String name, ImageObserver observer) {
        return null;
    }
}

public final class FilteredImageSourceTest {
    // Minimum test duration in ms
    private static final int TEST_MIN_DURATION = 5000;

    /*
     * A throwable object that will hold any exception generated while
     * executing methods on FilteredImageSource. The test passes if the
     * methods execute without any exception
     */
    private static volatile Throwable fail = null;

    public static void main(final String[] args)
            throws InterruptedException {
        final ImageConsumer ic = new EmptyImageConsumer();
        final ImageProducer ip = new EmptyImageProducer();
        final Image image = new EmptyFilteredImage(ip);

        /*
         * Simulate the framework's operations on FilteredImageSource by
         * invoking the concerned methods in multiple threads and observe
         * whether exceptions are generated.
         */
        Thread t1 = new Thread(() -> {
            try {
                while (true) {
                    image.getSource().addConsumer(ic);
                }
            } catch (Throwable t) {
                fail = t;
            }
        });
        t1.setDaemon(true);

        Thread t2 = new Thread(() -> {
            try {
                while (true) {
                    image.getSource().removeConsumer(ic);
                }
            } catch (Throwable t) {
                fail = t;
            }
        });
        t2.setDaemon(true);

        Thread t3 = new Thread(() -> {
            try {
                while (true) {
                    image.getSource().startProduction(ic);
                }
            } catch (Throwable t) {
                fail = t;
            }
        });
        t3.setDaemon(true);

        // Start the threads
        t1.start();
        t2.start();
        t3.start();

        // Wait on one of the threads for a specific duration.
        t1.join(TEST_MIN_DURATION);
        if (fail != null) {
            throw new RuntimeException("Test failed with exception: ", fail);
        }
    }
}