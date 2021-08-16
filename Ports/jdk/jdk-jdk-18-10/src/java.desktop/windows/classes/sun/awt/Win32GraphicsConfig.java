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

package sun.awt;

import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;

import sun.awt.image.OffScreenImage;
import sun.awt.image.SunVolatileImage;
import sun.awt.image.SurfaceManager;
import sun.awt.windows.WComponentPeer;
import sun.java2d.SurfaceData;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.RenderLoops;
import sun.java2d.loops.SurfaceType;
import sun.java2d.windows.GDIWindowSurfaceData;

/**
 * This is an implementation of a GraphicsConfiguration object for a
 * single Win32 visual.
 *
 * @see GraphicsEnvironment
 * @see GraphicsDevice
 */
public class Win32GraphicsConfig extends GraphicsConfiguration
    implements DisplayChangedListener, SurfaceManager.ProxiedGraphicsConfig
{
    private final Win32GraphicsDevice device;
    protected int visual;  //PixelFormatID
    protected RenderLoops solidloops;

    private static native void initIDs();

    static {
        initIDs();
    }

    /**
     * Returns a Win32GraphicsConfiguration object with the given device
     * and PixelFormat.  Note that this method does NOT check to ensure that
     * the returned Win32GraphicsConfig will correctly support rendering into a
     * Java window.  This method is provided so that client code can do its
     * own checking as to the appropriateness of a particular PixelFormat.
     * Safer access to Win32GraphicsConfigurations is provided by
     * Win32GraphicsDevice.getConfigurations().
     */
    public static Win32GraphicsConfig getConfig(Win32GraphicsDevice device,
                                                int pixFormatID)
    {
        return new Win32GraphicsConfig(device, pixFormatID);
    }

    /**
     * @deprecated as of JDK version 1.3
     * replaced by {@code getConfig()}
     */
    @Deprecated
    public Win32GraphicsConfig(GraphicsDevice device, int visualnum) {
        this.device = (Win32GraphicsDevice)device;
        this.visual = visualnum;
        ((Win32GraphicsDevice)device).addDisplayChangedListener(this);
    }

    /**
     * Return the graphics device associated with this configuration.
     */
    @Override
    public Win32GraphicsDevice getDevice() {
        return device;
    }

    /**
     * Return the PixelFormatIndex this GraphicsConfig uses
     */
    public int getVisual() {
        return visual;
    }

    @Override
    public Object getProxyKey() {
        return device;
    }

    /**
     * Return the RenderLoops this type of destination uses for
     * solid fills and strokes.
     */
    private SurfaceType sTypeOrig = null;
    public synchronized RenderLoops getSolidLoops(SurfaceType stype) {
        if (solidloops == null || sTypeOrig != stype) {
            solidloops = SurfaceData.makeRenderLoops(SurfaceType.OpaqueColor,
                                                     CompositeType.SrcNoEa,
                                                     stype);
            sTypeOrig = stype;
        }
        return solidloops;
    }

    /**
     * Returns the color model associated with this configuration.
     */
    @Override
    public synchronized ColorModel getColorModel() {
        return device.getColorModel();
    }

    /**
     * Returns a new color model for this configuration.  This call
     * is only used internally, by images and components that are
     * associated with the graphics device.  When attributes of that
     * device change (for example, when the device palette is updated),
     * then this device-based color model will be updated internally
     * to reflect the new situation.
     */
    public ColorModel getDeviceColorModel() {
        return device.getDynamicColorModel();
    }

    /**
     * Returns the color model associated with this configuration that
     * supports the specified transparency.
     */
    @Override
    public ColorModel getColorModel(int transparency) {
        switch (transparency) {
        case Transparency.OPAQUE:
            return getColorModel();
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
        double scaleX = device.getDefaultScaleX();
        double scaleY = device.getDefaultScaleY();
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
        Win32GraphicsEnvironment ge = (Win32GraphicsEnvironment)
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        double xscale = ge.getXResolution() / 72.0;
        double yscale = ge.getYResolution() / 72.0;
        return new AffineTransform(xscale, 0.0, 0.0, yscale, 0.0, 0.0);
    }

    public String toString() {
        return (super.toString()+"[dev="+device+",pixfmt="+visual+"]");
    }

    private native Rectangle getBounds(int screen);

    @Override
    public Rectangle getBounds() {
        return getBounds(device.getScreen());
    }

    @Override
    public synchronized void displayChanged() {
        solidloops = null;
    }

    @Override
    public void paletteChanged() {}

    /**
     * The following methods are invoked from WComponentPeer.java rather
     * than having the Win32-dependent implementations hardcoded in that
     * class.  This way the appropriate actions are taken based on the peer's
     * GraphicsConfig, whether it is a Win32GraphicsConfig or a
     * WGLGraphicsConfig.
     */

    /**
     * Creates a new SurfaceData that will be associated with the given
     * WComponentPeer.
     */
    public SurfaceData createSurfaceData(WComponentPeer peer,
                                         int numBackBuffers)
    {
        return GDIWindowSurfaceData.createData(peer);
    }

    /**
     * Creates a new managed image of the given width and height
     * that is associated with the target Component.
     */
    public Image createAcceleratedImage(Component target,
                                        int width, int height)
    {
        ColorModel model = getColorModel(Transparency.OPAQUE);
        WritableRaster wr =
            model.createCompatibleWritableRaster(width, height);
        return new OffScreenImage(target, model, wr,
                                  model.isAlphaPremultiplied());
    }

    /**
     * The following methods correspond to the multibuffering methods in
     * WComponentPeer.java...
     */

    /**
     * Checks that the requested configuration is natively supported; if not,
     * an AWTException is thrown.
     */
    public void assertOperationSupported(Component target,
                                         int numBuffers,
                                         BufferCapabilities caps)
        throws AWTException
    {
        // the default pipeline doesn't support flip buffer strategy
        throw new AWTException(
            "The operation requested is not supported");
    }

    /**
     * This method is called from WComponentPeer when a surface data is replaced
     * REMIND: while the default pipeline doesn't support flipping, it may
     * happen that the accelerated device may have this graphics config
     * (like if the device restoration failed when one device exits fs mode
     * while others remain).
     */
    public VolatileImage createBackBuffer(WComponentPeer peer) {
        Component target = (Component)peer.getTarget();
        return new SunVolatileImage(target,
                                    target.getWidth(), target.getHeight(),
                                    Boolean.TRUE);
    }

    /**
     * Performs the native flip operation for the given target Component.
     *
     * REMIND: we should really not get here because that would mean that
     * a FLIP BufferStrategy has been created, and one could only be created
     * if accelerated pipeline is present but in some rare (and transitional)
     * cases it may happen that the accelerated graphics device may have a
     * default graphics configuraiton, so this is just a precaution.
     */
    public void flip(WComponentPeer peer,
                     Component target, VolatileImage backBuffer,
                     int x1, int y1, int x2, int y2,
                     BufferCapabilities.FlipContents flipAction)
    {
        if (flipAction == BufferCapabilities.FlipContents.COPIED ||
            flipAction == BufferCapabilities.FlipContents.UNDEFINED) {
            Graphics g = peer.getGraphics();
            try {
                g.drawImage(backBuffer,
                            x1, y1, x2, y2,
                            x1, y1, x2, y2,
                            null);
            } finally {
                g.dispose();
            }
        } else if (flipAction == BufferCapabilities.FlipContents.BACKGROUND) {
            Graphics g = backBuffer.getGraphics();
            try {
                g.setColor(target.getBackground());
                g.fillRect(0, 0,
                           backBuffer.getWidth(),
                           backBuffer.getHeight());
            } finally {
                g.dispose();
            }
        }
        // the rest of the flip actions are not supported
    }

    @Override
    public boolean isTranslucencyCapable() {
        //XXX: worth checking if 8-bit? Anyway, it doesn't hurt.
        return true;
    }
}
