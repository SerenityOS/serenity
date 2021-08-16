/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;

import sun.awt.image.SunVolatileImage;

/**
 * The {@code GraphicsConfiguration} class describes the
 * characteristics of a graphics destination such as a printer or monitor.
 * There can be many {@code GraphicsConfiguration} objects associated
 * with a single graphics device, representing different drawing modes or
 * capabilities.  The corresponding native structure will vary from platform
 * to platform.  For example, on X11 windowing systems,
 * each visual is a different {@code GraphicsConfiguration}.
 * On Microsoft Windows, {@code GraphicsConfiguration}s represent
 * PixelFormats available in the current resolution and color depth.
 * <p>
 * In a virtual device multi-screen environment in which the desktop
 * area could span multiple physical screen devices, the bounds of the
 * {@code GraphicsConfiguration} objects are relative to the
 * virtual coordinate system.  When setting the location of a
 * component, use {@link #getBounds() getBounds} to get the bounds of
 * the desired {@code GraphicsConfiguration} and offset the location
 * with the coordinates of the {@code GraphicsConfiguration},
 * as the following code sample illustrates:
 * </p>
 *
 * <pre>
 *      Frame f = new Frame(gc);  // where gc is a GraphicsConfiguration
 *      Rectangle bounds = gc.getBounds();
 *      f.setLocation(10 + bounds.x, 10 + bounds.y); </pre>
 *
 * <p>
 * To determine if your environment is a virtual device
 * environment, call {@code getBounds} on all of the
 * {@code GraphicsConfiguration} objects in your system.  If
 * any of the origins of the returned bounds is not (0,&nbsp;0),
 * your environment is a virtual device environment.
 *
 * <p>
 * You can also use {@code getBounds} to determine the bounds
 * of the virtual device.  To do this, first call {@code getBounds} on all
 * of the {@code GraphicsConfiguration} objects in your
 * system.  Then calculate the union of all of the bounds returned
 * from the calls to {@code getBounds}.  The union is the
 * bounds of the virtual device.  The following code sample
 * calculates the bounds of the virtual device.
 *
 * <pre>{@code
 *      Rectangle virtualBounds = new Rectangle();
 *      GraphicsEnvironment ge = GraphicsEnvironment.
 *              getLocalGraphicsEnvironment();
 *      GraphicsDevice[] gs =
 *              ge.getScreenDevices();
 *      for (int j = 0; j < gs.length; j++) {
 *          GraphicsDevice gd = gs[j];
 *          GraphicsConfiguration[] gc =
 *              gd.getConfigurations();
 *          for (int i=0; i < gc.length; i++) {
 *              virtualBounds =
 *                  virtualBounds.union(gc[i].getBounds());
 *          }
 *      } }</pre>
 *
 * @see Window
 * @see Frame
 * @see GraphicsEnvironment
 * @see GraphicsDevice
 */
/*
 * REMIND:  What to do about capabilities?
 * The
 * capabilities of the device can be determined by enumerating the possible
 * capabilities and checking if the GraphicsConfiguration
 * implements the interface for that capability.
 *
 */
public abstract class GraphicsConfiguration {

    private static BufferCapabilities defaultBufferCaps;
    private static ImageCapabilities defaultImageCaps;

    /**
     * This is an abstract class that cannot be instantiated directly.
     * Instances must be obtained from a suitable factory or query method.
     *
     * @see GraphicsDevice#getConfigurations
     * @see GraphicsDevice#getDefaultConfiguration
     * @see GraphicsDevice#getBestConfiguration
     * @see Graphics2D#getDeviceConfiguration
     */
    protected GraphicsConfiguration() {
    }

    /**
     * Returns the {@link GraphicsDevice} associated with this
     * {@code GraphicsConfiguration}.
     * @return a {@code GraphicsDevice} object that is
     * associated with this {@code GraphicsConfiguration}.
     */
    public abstract GraphicsDevice getDevice();

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
    public BufferedImage createCompatibleImage(int width, int height) {
        ColorModel model = getColorModel();
        WritableRaster raster =
            model.createCompatibleWritableRaster(width, height);
        return new BufferedImage(model, raster,
                                 model.isAlphaPremultiplied(), null);
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
     * @throws IllegalArgumentException if the transparency is not a valid value
     * @see Transparency#OPAQUE
     * @see Transparency#BITMASK
     * @see Transparency#TRANSLUCENT
     */
    public BufferedImage createCompatibleImage(int width, int height,
                                               int transparency)
    {
        if (getColorModel().getTransparency() == transparency) {
            return createCompatibleImage(width, height);
        }

        ColorModel cm = getColorModel(transparency);
        if (cm == null) {
            throw new IllegalArgumentException("Unknown transparency: " +
                                               transparency);
        }
        WritableRaster wr = cm.createCompatibleWritableRaster(width, height);
        return new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
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
     * @since 1.4
     */
    public VolatileImage createCompatibleVolatileImage(int width, int height) {
        VolatileImage vi = null;
        try {
            vi = createCompatibleVolatileImage(width, height,
                                               null, Transparency.OPAQUE);
        } catch (AWTException e) {
            // shouldn't happen: we're passing in null caps
            assert false;
        }
        return vi;
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
     * @param transparency the specified transparency mode
     * @return a {@code VolatileImage} whose data layout and color
     * model is compatible with this {@code GraphicsConfiguration}.
     * @throws IllegalArgumentException if the transparency is not a valid value
     * @see Transparency#OPAQUE
     * @see Transparency#BITMASK
     * @see Transparency#TRANSLUCENT
     * @see Component#createVolatileImage(int, int)
     * @since 1.5
     */
    public VolatileImage createCompatibleVolatileImage(int width, int height,
                                                       int transparency)
    {
        VolatileImage vi = null;
        try {
            vi = createCompatibleVolatileImage(width, height, null, transparency);
        } catch (AWTException e) {
            // shouldn't happen: we're passing in null caps
            assert false;
        }
        return vi;
    }

    /**
     * Returns a {@link VolatileImage} with a data layout and color model
     * compatible with this {@code GraphicsConfiguration}, using
     * the specified image capabilities.
     * If the {@code caps} parameter is null, it is effectively ignored
     * and this method will create a VolatileImage without regard to
     * {@code ImageCapabilities} constraints.
     *
     * The returned {@code VolatileImage} has
     * a layout and color model that is closest to this native device
     * configuration and can therefore be optimally blitted to this
     * device.
     * @return a {@code VolatileImage} whose data layout and color
     * model is compatible with this {@code GraphicsConfiguration}.
     * @param width the width of the returned {@code VolatileImage}
     * @param height the height of the returned {@code VolatileImage}
     * @param caps the image capabilities
     * @exception AWTException if the supplied image capabilities could not
     * be met by this graphics configuration
     * @since 1.4
     */
    public VolatileImage createCompatibleVolatileImage(int width, int height,
        ImageCapabilities caps) throws AWTException
    {
        return createCompatibleVolatileImage(width, height, caps,
                                             Transparency.OPAQUE);
    }

    /**
     * Returns a {@link VolatileImage} with a data layout and color model
     * compatible with this {@code GraphicsConfiguration}, using
     * the specified image capabilities and transparency value.
     * If the {@code caps} parameter is null, it is effectively ignored
     * and this method will create a VolatileImage without regard to
     * {@code ImageCapabilities} constraints.
     *
     * The returned {@code VolatileImage} has
     * a layout and color model that is closest to this native device
     * configuration and can therefore be optimally blitted to this
     * device.
     * @param width the width of the returned {@code VolatileImage}
     * @param height the height of the returned {@code VolatileImage}
     * @param caps the image capabilities
     * @param transparency the specified transparency mode
     * @return a {@code VolatileImage} whose data layout and color
     * model is compatible with this {@code GraphicsConfiguration}.
     * @see Transparency#OPAQUE
     * @see Transparency#BITMASK
     * @see Transparency#TRANSLUCENT
     * @throws IllegalArgumentException if the transparency is not a valid value
     * @exception AWTException if the supplied image capabilities could not
     * be met by this graphics configuration
     * @see Component#createVolatileImage(int, int)
     * @since 1.5
     */
    public VolatileImage createCompatibleVolatileImage(int width, int height,
        ImageCapabilities caps, int transparency) throws AWTException
    {
        VolatileImage vi =
            new SunVolatileImage(this, width, height, transparency, caps);
        if (caps != null && caps.isAccelerated() &&
            !vi.getCapabilities().isAccelerated())
        {
            throw new AWTException("Supplied image capabilities could not " +
                                   "be met by this graphics configuration.");
        }
        return vi;
    }

    /**
     * Returns the {@link ColorModel} associated with this
     * {@code GraphicsConfiguration}.
     * @return a {@code ColorModel} object that is associated with
     * this {@code GraphicsConfiguration}.
     */
    public abstract ColorModel getColorModel();

    /**
     * Returns the {@code ColorModel} associated with this
     * {@code GraphicsConfiguration} that supports the specified
     * transparency.
     * @param transparency the specified transparency mode
     * @return a {@code ColorModel} object that is associated with
     * this {@code GraphicsConfiguration} and supports the
     * specified transparency or null if the transparency is not a valid
     * value.
     * @see Transparency#OPAQUE
     * @see Transparency#BITMASK
     * @see Transparency#TRANSLUCENT
     */
    public abstract ColorModel getColorModel(int transparency);

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
    public abstract AffineTransform getDefaultTransform();

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
     *      GraphicsConfiguration gc = g.getDeviceConfiguration();
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
    public abstract AffineTransform getNormalizingTransform();

    /**
     * Returns the bounds of the {@code GraphicsConfiguration}
     * in the device coordinates. In a multi-screen environment
     * with a virtual device, the bounds can have negative X
     * or Y origins.
     * @return the bounds of the area covered by this
     * {@code GraphicsConfiguration}.
     * @since 1.3
     */
    public abstract Rectangle getBounds();

    private static class DefaultBufferCapabilities extends BufferCapabilities {
        public DefaultBufferCapabilities(ImageCapabilities imageCaps) {
            super(imageCaps, imageCaps, null);
        }
    }

    /**
     * Returns the buffering capabilities of this
     * {@code GraphicsConfiguration}.
     * @return the buffering capabilities of this graphics
     * configuration object
     * @since 1.4
     */
    public BufferCapabilities getBufferCapabilities() {
        if (defaultBufferCaps == null) {
            defaultBufferCaps = new DefaultBufferCapabilities(
                getImageCapabilities());
        }
        return defaultBufferCaps;
    }

    /**
     * Returns the image capabilities of this
     * {@code GraphicsConfiguration}.
     * @return the image capabilities of this graphics
     * configuration object
     * @since 1.4
     */
    public ImageCapabilities getImageCapabilities() {
        if (defaultImageCaps == null) {
            defaultImageCaps = new ImageCapabilities(false);
        }
        return defaultImageCaps;
    }

    /**
     * Returns whether this {@code GraphicsConfiguration} supports
     * the {@link GraphicsDevice.WindowTranslucency#PERPIXEL_TRANSLUCENT
     * PERPIXEL_TRANSLUCENT} kind of translucency.
     *
     * @return whether the given GraphicsConfiguration supports
     *         the translucency effects.
     *
     * @see Window#setBackground(Color)
     *
     * @since 1.7
     */
    public boolean isTranslucencyCapable() {
        // Overridden in subclasses
        return false;
    }
}
