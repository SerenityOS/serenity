/*
 * Copyright (c) 2004, 2017, Oracle and/or its affiliates. All rights reserved.
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
package sun.swing;

import sun.awt.image.SurfaceManager;
import sun.java2d.SurfaceData;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AbstractMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.VolatileImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A base class used for icons or images that are expensive to paint.
 * A subclass will do the following:
 * <ol>
 * <li>Invoke <code>paint</code> when you want to paint the image,
 *     if you are implementing <code>Icon</code> you'll invoke this from
 *     <code>paintIcon</code>.
 *     The args argument is useful when additional state is needed.
 * <li>Override <code>paintToImage</code> to render the image.  The code that
 *     lives here is equivalent to what previously would go in
 *     <code>paintIcon</code>, for an <code>Icon</code>.
 * </ol>
 * The two ways to use this class are:
 * <ol>
 * <li>Invoke <code>paint</code> to draw the cached reprensentation at
 *     the specified location.
 * <li>Invoke <code>getImage</code> to get the cached reprensentation and
 *     draw the image yourself.  This is primarly useful when you are not
 *     using <code>VolatileImage</code>.
 * </ol>
 *
 *
 */
public abstract class CachedPainter {
    // CacheMap maps from class to ImageCache.
    private static final Map<Object,ImageCache> cacheMap = new HashMap<>();

    private static ImageCache getCache(Object key) {
        synchronized(CachedPainter.class) {
            ImageCache cache = cacheMap.get(key);
            if (cache == null) {
                if (key == PainterMultiResolutionCachedImage.class) {
                    cache = new ImageCache(32);
                } else {
                    cache = new ImageCache(1);
                }
                cacheMap.put(key, cache);
            }
            return cache;
        }
    }

    /**
     * Creates an instance of <code>CachedPainter</code> that will cache up
     * to <code>cacheCount</code> images of this class.
     *
     * @param cacheCount Max number of images to cache
     */
    public CachedPainter(int cacheCount) {
        getCache(getClass()).setMaxCount(cacheCount);
    }

    /**
     * Renders the cached image to the passed in <code>Graphic</code>.
     * If there is no cached image <code>paintToImage</code> will be invoked.
     * <code>paintImage</code> is invoked to paint the cached image.
     *
     * @param c Component rendering to, this may be null.
     * @param g Graphics to paint to
     * @param x X-coordinate to render to
     * @param y Y-coordinate to render to
     * @param w Width to render in
     * @param h Height to render in
     * @param args Variable arguments that will be passed to paintToImage
     */
    public void paint(Component c, Graphics g, int x,
                         int y, int w, int h, Object... args) {
        if (w <= 0 || h <= 0) {
            return;
        }
        synchronized (CachedPainter.class) {
            paint0(c, g, x, y, w, h, args);
        }
    }

    private Image getImage(Object key, Component c,
                           int baseWidth, int baseHeight,
                           int w, int h, Object... args) {
        GraphicsConfiguration config = getGraphicsConfiguration(c);
        ImageCache cache = getCache(key);
        Image image = cache.getImage(key, config, w, h, args);
        int attempts = 0;
        VolatileImage volatileImage = (image instanceof VolatileImage)
                ? (VolatileImage) image
                : null;
        do {
            boolean draw = false;
            if (volatileImage != null) {
                // See if we need to recreate the image
                switch (volatileImage.validate(config)) {
                case VolatileImage.IMAGE_INCOMPATIBLE:
                    volatileImage.flush();
                    image = null;
                    break;
                case VolatileImage.IMAGE_RESTORED:
                    draw = true;
                    break;
                }
            }
            if (image == null) {
                // Recreate the image
                if( config != null && (w != baseHeight || h != baseWidth)) {
                    AffineTransform tx = config.getDefaultTransform();
                    double sx = tx.getScaleX();
                    double sy = tx.getScaleY();
                    if ( Double.compare(sx, 1) != 0 ||
                                                   Double.compare(sy, 1) != 0) {
                        if (Math.abs(sx * baseWidth - w) < 1 &&
                            Math.abs(sy * baseHeight - h) < 1) {
                            w = baseWidth;
                            h = baseHeight;
                        } else {
                            w = (int)Math.ceil(w / sx);
                            h = (int)Math.ceil(w / sy);
                        }
                    }
                }
                image = createImage(c, w, h, config, args);
                cache.setImage(key, config, w, h, args, image);
                draw = true;
                volatileImage = (image instanceof VolatileImage)
                        ? (VolatileImage) image
                        : null;
            }
            if (draw) {
                // Render to the Image
                Graphics2D g2 = (Graphics2D) image.getGraphics();
                if (volatileImage == null) {
                    if ((w != baseWidth || h != baseHeight)) {
                        g2.scale((double) w / baseWidth,
                                (double) h / baseHeight);
                    }
                    paintToImage(c, image, g2, baseWidth, baseHeight, args);
                } else {
                    SurfaceData sd = SurfaceManager.getManager(volatileImage)
                            .getPrimarySurfaceData();
                    double sx = sd.getDefaultScaleX();
                    double sy = sd.getDefaultScaleY();
                    if ( Double.compare(sx, 1) != 0 ||
                                                   Double.compare(sy, 1) != 0) {
                        g2.scale(1 / sx, 1 / sy);
                    }
                    paintToImage(c, image, g2, (int)Math.ceil(w * sx),
                                               (int)Math.ceil(h * sy), args);
                }
                g2.dispose();
            }

            // If we did this 3 times and the contents are still lost
            // assume we're painting to a VolatileImage that is bogus and
            // give up.  Presumably we'll be called again to paint.
        } while ((volatileImage != null) &&
                 volatileImage.contentsLost() && ++attempts < 3);

        return image;
    }

    private void paint0(Component c, Graphics g, int x,
                        int y, int w, int h, Object... args) {
        Object key = getClass();
        GraphicsConfiguration config = getGraphicsConfiguration(c);
        ImageCache cache = getCache(key);
        Image image = cache.getImage(key, config, w, h, args);

        if (image == null) {
            image = new PainterMultiResolutionCachedImage(w, h);
            cache.setImage(key, config, w, h, args, image);
        }

        if (image instanceof PainterMultiResolutionCachedImage) {
            ((PainterMultiResolutionCachedImage) image).setParams(c, args);
        }

        // Render to the passed in Graphics
        paintImage(c, g, x, y, w, h, image, args);
    }

    /**
     * Paints the representation to cache to the supplied Graphics.
     *
     * @param c Component painting to, may be null.
     * @param image Image to paint to
     * @param g Graphics to paint to, obtained from the passed in Image.
     * @param w Width to paint to
     * @param h Height to paint to
     * @param args Arguments supplied to <code>paint</code>
     */
    protected abstract void paintToImage(Component c, Image image, Graphics g,
                                         int w, int h, Object[] args);


    /**
     * Paints the image to the specified location.
     *
     * @param c Component painting to
     * @param g Graphics to paint to
     * @param x X coordinate to paint to
     * @param y Y coordinate to paint to
     * @param w Width to paint to
     * @param h Height to paint to
     * @param image Image to paint
     * @param args Arguments supplied to <code>paint</code>
     */
    protected void paintImage(Component c, Graphics g,
                              int x, int y, int w, int h, Image image,
                              Object[] args) {
        g.drawImage(image, x, y, null);
    }

    /**
     * Creates the image to cache.  This returns an opaque image, subclasses
     * that require translucency or transparency will need to override this
     * method.
     *
     * @param c Component painting to
     * @param w Width of image to create
     * @param h Height to image to create
     * @param config GraphicsConfiguration that will be
     *        rendered to, this may be null.
     * @param args Arguments passed to paint
     */
    protected Image createImage(Component c, int w, int h,
                                GraphicsConfiguration config, Object[] args) {
        if (config == null) {
            return new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        }
        return config.createCompatibleVolatileImage(w, h);
    }

    /**
     * Clear the image cache
     */
    protected void flush() {
        synchronized(CachedPainter.class) {
            getCache(getClass()).flush();
        }
    }

    private GraphicsConfiguration getGraphicsConfiguration(Component c) {
        if (c == null) {
            return null;
        }
        return c.getGraphicsConfiguration();
    }

    class PainterMultiResolutionCachedImage extends AbstractMultiResolutionImage {

        private final int baseWidth;
        private final int baseHeight;
        private Component c;
        private Object[] args;

        public PainterMultiResolutionCachedImage(int baseWidth, int baseHeight) {
            this.baseWidth = baseWidth;
            this.baseHeight = baseHeight;
        }

        public void setParams(Component c, Object[] args) {
            this.c = c;
            this.args = args;
        }

        @Override
        public int getWidth(ImageObserver observer) {
            return baseWidth;
        }

        @Override
        public int getHeight(ImageObserver observer) {
            return baseHeight;
        }

        @Override
        public Image getResolutionVariant(double destWidth, double destHeight) {
            int w = (int) Math.ceil(destWidth);
            int h = (int) Math.ceil(destHeight);
            return getImage(PainterMultiResolutionCachedImage.class,
                    c, baseWidth, baseHeight, w, h, args);
        }

        @Override
        protected Image getBaseImage() {
            return getResolutionVariant(baseWidth, baseHeight);
        }

        @Override
        public java.util.List<Image> getResolutionVariants() {
            return Arrays.asList(getResolutionVariant(baseWidth, baseHeight));
        }
    }
}
