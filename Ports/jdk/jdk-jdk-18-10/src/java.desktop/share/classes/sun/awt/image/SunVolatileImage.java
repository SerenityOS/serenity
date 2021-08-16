/*
 * Copyright (c) 2000, 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.ImageCapabilities;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.VolatileImage;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceManagerFactory;
import sun.java2d.DestSurfaceProvider;
import sun.java2d.Surface;
import sun.java2d.pipe.Region;
import static sun.java2d.pipe.hw.AccelSurface.*;

/**
 * This class is the base implementation of the VolatileImage
 * abstract class.  The class implements most of the standard Image
 * methods (width, height, etc.) but delegates all surface management
 * issues to a platform-specific VolatileSurfaceManager.  When a new instance
 * of SunVolatileImage is created, it automatically creates an
 * appropriate VolatileSurfaceManager for the GraphicsConfiguration
 * under which this SunVolatileImage was created.
 */
public class SunVolatileImage extends VolatileImage
    implements DestSurfaceProvider
{

    protected VolatileSurfaceManager volSurfaceManager;
    protected Component comp;
    private GraphicsConfiguration graphicsConfig;
    private Font defaultFont;
    private int width, height;
    private int forcedAccelSurfaceType;

    protected SunVolatileImage(Component comp,
                               GraphicsConfiguration graphicsConfig,
                               int width, int height, Object context,
                               int transparency, ImageCapabilities caps,
                               int accType)
    {
        this.comp = comp;
        this.graphicsConfig = graphicsConfig;
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width (" + width + ")" +
                              " and height (" + height + ") cannot be <= 0");
        }
        this.width = width;
        this.height = height;
        this.forcedAccelSurfaceType = accType;
        if (!(transparency == Transparency.OPAQUE ||
            transparency == Transparency.BITMASK ||
            transparency == Transparency.TRANSLUCENT))
        {
            throw new IllegalArgumentException("Unknown transparency type:" +
                                               transparency);
        }
        this.transparency = transparency;
        this.volSurfaceManager = createSurfaceManager(context, caps);
        SurfaceManager.setManager(this, volSurfaceManager);

        // post-construction initialization of the surface manager
        volSurfaceManager.initialize();
        // clear the background
        volSurfaceManager.initContents();
    }

    private SunVolatileImage(Component comp,
                             GraphicsConfiguration graphicsConfig,
                             int width, int height, Object context,
                             ImageCapabilities caps)
    {
        this(comp, graphicsConfig,
             width, height, context, Transparency.OPAQUE, caps, UNDEFINED);
    }

    public SunVolatileImage(Component comp, int width, int height) {
        this(comp, width, height, null);
    }

    public SunVolatileImage(Component comp,
                            int width, int height, Object context)
    {
        this(comp, comp.getGraphicsConfiguration(),
             width, height, context, null);
    }

    public SunVolatileImage(GraphicsConfiguration graphicsConfig,
                            int width, int height, int transparency,
                            ImageCapabilities caps)
    {
        this(null, graphicsConfig, width, height, null, transparency,
             caps, UNDEFINED);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public GraphicsConfiguration getGraphicsConfig() {
        return graphicsConfig;
    }

    public void updateGraphicsConfig() {
        // If this VImage is associated with a Component, get an updated
        // graphicsConfig from that component.  Otherwise, keep the one
        // that we were created with
        if (comp != null) {
            GraphicsConfiguration gc = comp.getGraphicsConfiguration();
            if (gc != null) {
                // Could potentially be null in some failure situations;
                // better to keep the old non-null value around than to
                // set graphicsConfig to null
                graphicsConfig = gc;
            }
        }
    }

    public Component getComponent() {
        return comp;
    }

    public int getForcedAccelSurfaceType() {
        return forcedAccelSurfaceType;
    }

    protected VolatileSurfaceManager createSurfaceManager(Object context,
                                                          ImageCapabilities caps)
    {
        /**
         * Platform-specific SurfaceManagerFactories will return a
         * manager suited to acceleration on each platform.  But if
         * the user is asking for a VolatileImage from a BufferedImageGC,
         * then we need to return the appropriate unaccelerated manager.
         * Note: this could change in the future; if some platform would
         * like to accelerate BIGC volatile images, then this special-casing
         * of the BIGC graphicsConfig should live in platform-specific
         * code instead.
         * We do the same for a Printer Device, and if user requested an
         * unaccelerated VolatileImage by passing the capabilities object.
         */
        if (graphicsConfig instanceof BufferedImageGraphicsConfig ||
            graphicsConfig instanceof sun.print.PrinterGraphicsConfig ||
            (caps != null && !caps.isAccelerated()))
        {
            return new BufImgVolatileSurfaceManager(this, context);
        }
        SurfaceManagerFactory smf = SurfaceManagerFactory.getInstance();
        return smf.createVolatileManager(this, context);
    }

    private Color getForeground() {
        if (comp != null) {
            return comp.getForeground();
        } else {
            return Color.black;
        }
    }

    private Color getBackground() {
        if (comp != null) {
            return comp.getBackground();
        } else {
            return Color.white;
        }
    }

    private Font getFont() {
        if (comp != null) {
            return comp.getFont();
        } else {
            if (defaultFont == null) {
                defaultFont = new Font("Dialog", Font.PLAIN, 12);
            }
            return defaultFont;
        }
    }

    public Graphics2D createGraphics() {
        return new SunGraphics2D(volSurfaceManager.getPrimarySurfaceData(),
                                 getForeground(),
                                 getBackground(),
                                 getFont());
    }

    // Image method implementations
    public Object getProperty(String name, ImageObserver observer) {
        if (name == null) {
            throw new NullPointerException("null property name is not allowed");
        }
        return java.awt.Image.UndefinedProperty;
    }

    public int getWidth(ImageObserver observer) {
        return getWidth();
    }

    public int getHeight(ImageObserver observer) {
        return getHeight();
    }

    /**
     * This method creates a BufferedImage intended for use as a "snapshot"
     * or a backup surface.
     */
    public BufferedImage getBackupImage() {
        return getBackupImage(1, 1);
    }

    /**
     * This method creates a BufferedImage intended for use as a "snapshot"
     * or a backup surface with the given horizontal and vertical scale factors.
     */
    public BufferedImage getBackupImage(double scaleX, double scaleY) {
        int w = Region.clipRound(getWidth() * scaleX);
        int h = Region.clipRound(getHeight() * scaleY);
        return graphicsConfig.createCompatibleImage(w, h, getTransparency());
    }

    public BufferedImage getSnapshot() {
        BufferedImage bi = getBackupImage();
        Graphics2D g = bi.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(this, 0, 0, null);
        g.dispose();
        return bi;
    }

    public int validate(GraphicsConfiguration gc) {
        return volSurfaceManager.validate(gc);
    }

    public boolean contentsLost() {
        return volSurfaceManager.contentsLost();
    }

    public ImageCapabilities getCapabilities() {
        return volSurfaceManager.getCapabilities(graphicsConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @see sun.java2d.DestSurfaceProvider#getDestSurface
     */
    @Override
    public Surface getDestSurface() {
        return volSurfaceManager.getPrimarySurfaceData();
    }
}
