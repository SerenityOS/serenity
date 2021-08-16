/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public final class BufferedImageGraphicsConfig extends GraphicsConfiguration {

    private static final int numconfigs = BufferedImage.TYPE_BYTE_BINARY;
    private static BufferedImageGraphicsConfig[] standardConfigs =
        new BufferedImageGraphicsConfig[numconfigs];
    private static BufferedImageGraphicsConfig[] scaledConfigs =
        new BufferedImageGraphicsConfig[numconfigs];

    public static BufferedImageGraphicsConfig getConfig(BufferedImage bImg) {
        return getConfig(bImg, 1, 1);
    }

    public static BufferedImageGraphicsConfig getConfig(BufferedImage bImg,
                                                        double scaleX,
                                                        double scaleY)
    {
        BufferedImageGraphicsConfig ret;
        int type = bImg.getType();

        BufferedImageGraphicsConfig[] configs = (scaleX == 1 && scaleY == 1)
                ? standardConfigs : scaledConfigs;

        if (type > 0 && type < numconfigs) {
            ret = configs[type];
            if (ret != null && ret.scaleX == scaleX && ret.scaleY == scaleY) {
                return ret;
            }
        }
        ret = new BufferedImageGraphicsConfig(bImg, null, scaleX, scaleY);
        if (type > 0 && type < numconfigs) {
            configs[type] = ret;
        }
        return ret;
    }

    private final GraphicsDevice device;
    private final ColorModel model;
    private final Raster raster;
    private final double scaleX;
    private final double scaleY;

    public BufferedImageGraphicsConfig(BufferedImage bufImg, Component comp,
                                       double scaleX, double scaleY)
    {
        if (comp == null) {
            device = new BufferedImageDevice(this);
        } else {
            Graphics2D g2d = (Graphics2D)comp.getGraphics();
            device = g2d.getDeviceConfiguration().getDevice();
        }
        this.model = bufImg.getColorModel();
        this.raster = bufImg.getRaster().createCompatibleWritableRaster(1, 1);
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    /**
     * Return the graphics device associated with this configuration.
     */
    @Override
    public GraphicsDevice getDevice() {
        return device;
    }

    /**
     * Returns a BufferedImage with channel layout and color model
     * compatible with this graphics configuration.  This method
     * has nothing to do with memory-mapping
     * a device.  This BufferedImage has
     * a layout and color model
     * that is closest to this native device configuration and thus
     * can be optimally blitted to this device.
     */
    @Override
    public BufferedImage createCompatibleImage(int width, int height) {
        WritableRaster wr = raster.createCompatibleWritableRaster(width, height);
        return new BufferedImage(model, wr, model.isAlphaPremultiplied(), null);
    }

    /**
     * Returns the color model associated with this configuration.
     */
    @Override
    public ColorModel getColorModel() {
        return model;
    }

    /**
     * Returns the color model associated with this configuration that
     * supports the specified transparency.
     */
    @Override
    public ColorModel getColorModel(int transparency) {

        if (model.getTransparency() == transparency) {
            return model;
        }
        switch (transparency) {
        case Transparency.OPAQUE:
            return new DirectColorModel(24, 0xff0000, 0xff00, 0xff);
        case Transparency.BITMASK:
            return new DirectColorModel(25, 0xff0000, 0xff00, 0xff, 0x1000000);
        case Transparency.TRANSLUCENT:
            return ColorModel.getRGBdefault();
        default:
            return null;
        }
    }

    /**
     * Returns the default Transform for this configuration.  This
     * Transform is typically the Identity transform for most normal
     * screens.  Device coordinates for screen and printer devices will
     * have the origin in the upper left-hand corner of the target region of
     * the device, with X coordinates
     * increasing to the right and Y coordinates increasing downwards.
     * For image buffers, this Transform will be the Identity transform.
     */
    @Override
    public AffineTransform getDefaultTransform() {
        return AffineTransform.getScaleInstance(scaleX, scaleY);
    }

    /**
     *
     * Returns a Transform that can be composed with the default Transform
     * of a Graphics2D so that 72 units in user space will equal 1 inch
     * in device space.
     * Given a Graphics2D, g, one can reset the transformation to create
     * such a mapping by using the following pseudocode:
     * <pre>
     *      GraphicsConfiguration gc = g.getGraphicsConfiguration();
     *
     *      g.setTransform(gc.getDefaultTransform());
     *      g.transform(gc.getNormalizingTransform());
     * </pre>
     * Note that sometimes this Transform will be identity (e.g. for
     * printers or metafile output) and that this Transform is only
     * as accurate as the information supplied by the underlying system.
     * For image buffers, this Transform will be the Identity transform,
     * since there is no valid distance measurement.
     */
    @Override
    public AffineTransform getNormalizingTransform() {
        return new AffineTransform();
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }
}
