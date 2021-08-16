/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt.macosx;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.VolatileImage;
import java.awt.print.PageFormat;

public final class CPrinterGraphicsConfig extends GraphicsConfiguration {

    public static CPrinterGraphicsConfig getConfig(PageFormat pf) {
        return new CPrinterGraphicsConfig(pf);
    }

    private final GraphicsDevice device;
    private final PageFormat pf;

    public CPrinterGraphicsConfig(PageFormat pf) {
        this.device = new CPrinterDevice(this);
        this.pf = pf;
    }

    public PageFormat getPageFormat() {
        return pf;
    }

    /**
     * Returns the {@link GraphicsDevice} associated with this
     * {@code GraphicsConfiguration}.
     * @return a {@code GraphicsDevice} object that is
     * associated with this {@code GraphicsConfiguration}.
     */
    @Override
    public GraphicsDevice getDevice() {
        return device;
    }

    /**
     * Returns a {@link BufferedImage} with a data layout and color model
     * compatible with this {@code GraphicsConfiguration}.  This
     * method has nothing to do with memory-mapping
     * a device.  The returned {@code BufferedImage} has
     * a layout and color model that is closest to this native device
     * configuration and can therefore be optimally blitted to this
     * device.
     * @param width the width of the returned {@code BufferedImage}
     * @param height the height of the returned {@code BufferedImage}
     * @return a {@code BufferedImage} whose data layout and color
     * model is compatible with this {@code GraphicsConfiguration}.
     */
    @Override
    public BufferedImage createCompatibleImage(int width, int height) {
        return createCompatibleImage(width, height, Transparency.OPAQUE);
    }

    /**
     * Returns a {@link VolatileImage} with a data layout and color model
     * compatible with this {@code GraphicsConfiguration}.
     * The returned {@code VolatileImage}
     * may have data that is stored optimally for the underlying graphics
     * device and may therefore benefit from platform-specific rendering
     * acceleration.
     * @param width the width of the returned {@code VolatileImage}
     * @param height the height of the returned {@code VolatileImage}
     * @return a {@code VolatileImage} whose data layout and color
     * model is compatible with this {@code GraphicsConfiguration}.
     * @see Component#createVolatileImage(int, int)
     */
    @Override
    public VolatileImage createCompatibleVolatileImage(int width, int height) {
        return createCompatibleVolatileImage(width, height, Transparency.OPAQUE);
    }

    // empty implementation (this should not be called)
    @Override
    public VolatileImage createCompatibleVolatileImage(int width, int height, int transparency) {
        return null;
    }

    /**
     * Returns a {@code BufferedImage} that supports the specified
     * transparency and has a data layout and color model
     * compatible with this {@code GraphicsConfiguration}.  This
     * method has nothing to do with memory-mapping
     * a device. The returned {@code BufferedImage} has a layout and
     * color model that can be optimally blitted to a device
     * with this {@code GraphicsConfiguration}.
     * @param width the width of the returned {@code BufferedImage}
     * @param height the height of the returned {@code BufferedImage}
     * @param transparency the specified transparency mode
     * @return a {@code BufferedImage} whose data layout and color
     * model is compatible with this {@code GraphicsConfiguration}
     * and also supports the specified transparency.
     * @see Transparency#OPAQUE
     * @see Transparency#BITMASK
     * @see Transparency#TRANSLUCENT
     */
    @Override
    public BufferedImage createCompatibleImage(int width, int height, int transparency) {
        //+++gdb what to do?
        return null;
    }

    /**
     * Returns the {@link ColorModel} associated with this
     * {@code GraphicsConfiguration}.
     * @return a {@code ColorModel} object that is associated with
     * this {@code GraphicsConfiguration}.
     */
    @Override
    public ColorModel getColorModel() {
        return getColorModel(Transparency.OPAQUE);
    }

    /**
     * Returns the {@code ColorModel} associated with this
     * {@code GraphicsConfiguration} that supports the specified
     * transparency.
     * @param transparency the specified transparency mode
     * @return a {@code ColorModel} object that is associated with
     * this {@code GraphicsConfiguration} and supports the
     * specified transparency.
     */
    @Override
    public ColorModel getColorModel(int transparency) {
        return ColorModel.getRGBdefault();
    }

    /**
     * Returns the default {@link AffineTransform} for this
     * {@code GraphicsConfiguration}. This
     * {@code AffineTransform} is typically the Identity transform
     * for most normal screens.  The default {@code AffineTransform}
     * maps coordinates onto the device such that 72 user space
     * coordinate units measure approximately 1 inch in device
     * space.  The normalizing transform can be used to make
     * this mapping more exact.  Coordinates in the coordinate space
     * defined by the default {@code AffineTransform} for screen and
     * printer devices have the origin in the upper left-hand corner of
     * the target region of the device, with X coordinates
     * increasing to the right and Y coordinates increasing downwards.
     * For image buffers not associated with a device, such as those not
     * created by {@code createCompatibleImage},
     * this {@code AffineTransform} is the Identity transform.
     * @return the default {@code AffineTransform} for this
     * {@code GraphicsConfiguration}.
     */
    @Override
    public AffineTransform getDefaultTransform() {
        return new AffineTransform();
    }

    /**
     * Returns an {@code AffineTransform} that can be concatenated
     * with the default {@code AffineTransform}
     * of a {@code GraphicsConfiguration} so that 72 units in user
     * space equals 1 inch in device space.
     * <p>
     * For a particular {@link Graphics2D}, g, one
     * can reset the transformation to create
     * such a mapping by using the following pseudocode:
     * <pre>
     *      GraphicsConfiguration gc = g.getGraphicsConfiguration();
     *
     *      g.setTransform(gc.getDefaultTransform());
     *      g.transform(gc.getNormalizingTransform());
     * </pre>
     * Note that sometimes this {@code AffineTransform} is identity,
     * such as for printers or metafile output, and that this
     * {@code AffineTransform} is only as accurate as the information
     * supplied by the underlying system.  For image buffers not
     * associated with a device, such as those not created by
     * {@code createCompatibleImage}, this
     * {@code AffineTransform} is the Identity transform
     * since there is no valid distance measurement.
     * @return an {@code AffineTransform} to concatenate to the
     * default {@code AffineTransform} so that 72 units in user
     * space is mapped to 1 inch in device space.
     */
    @Override
    public AffineTransform getNormalizingTransform() {
        return new AffineTransform();
    }

    /**
     * Returns the bounds of the {@code GraphicsConfiguration}
     * in the device coordinates. In a multi-screen environment
     * with a virtual device, the bounds can have negative X
     * or Y origins.
     * @return the bounds of the area covered by this
     * {@code GraphicsConfiguration}.
     * @since 1.3
     */
    @Override
    public Rectangle getBounds() {
        return new Rectangle(0, 0, (int)pf.getWidth(), (int)pf.getHeight());
    }
}
