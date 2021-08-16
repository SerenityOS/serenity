/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.awt.ImageCapabilities;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;

import sun.awt.image.OffScreenImage;
import sun.awt.image.SunVolatileImage;
import sun.awt.image.SurfaceManager;
import sun.java2d.Disposer;
import sun.java2d.DisposerRecord;
import sun.java2d.SurfaceData;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.RenderLoops;
import sun.java2d.loops.SurfaceType;
import sun.java2d.pipe.Region;
import sun.java2d.x11.X11SurfaceData;

/**
 * This is an implementation of a GraphicsConfiguration object for a
 * single X11 visual.
 *
 * @see java.awt.GraphicsEnvironment
 * @see GraphicsDevice
 */
public class X11GraphicsConfig extends GraphicsConfiguration
    implements SurfaceManager.ProxiedGraphicsConfig
{
    private final X11GraphicsDevice device;
    protected int visual;
    int depth;
    int colormap;
    ColorModel colorModel;
    long aData;
    boolean doubleBuffer;
    private Object disposerReferent = new Object();
    private BufferCapabilities bufferCaps;
    private static ImageCapabilities imageCaps =
        new ImageCapabilities(X11SurfaceData.isAccelerationEnabled());

    // will be set on native level from init()
    protected int bitsPerPixel;

    protected SurfaceType surfaceType;

    public RenderLoops solidloops;

    public static X11GraphicsConfig getConfig(X11GraphicsDevice device,
                                              int visualnum, int depth,
                                              int colormap,
                                              boolean doubleBuffer)
    {
        return new X11GraphicsConfig(device, visualnum, depth, colormap, doubleBuffer);
    }

    /*
     * Note this method is currently here for backward compatibility
     * as this was the method used in jdk 1.2 beta4 to create the
     * X11GraphicsConfig objects. Java3D code had called this method
     * explicitly so without this, if a user tries to use JDK1.2 fcs
     * with Java3D beta1, a NoSuchMethod execption is thrown and
     * the program exits. REMOVE this method after Java3D fcs is
     * released!
     */
    public static X11GraphicsConfig getConfig(X11GraphicsDevice device,
                                              int visualnum, int depth,
                                              int colormap, int type)
    {
        return new X11GraphicsConfig(device, visualnum, depth, colormap, false);
    }

    private native int getNumColors();
    private native void init(int visualNum, int screen);
    private native ColorModel makeColorModel();

    protected X11GraphicsConfig(X11GraphicsDevice device,
                                int visualnum, int depth,
                                int colormap, boolean doubleBuffer)
    {
        this.device = device;
        this.visual = visualnum;
        this.doubleBuffer = doubleBuffer;
        this.depth = depth;
        this.colormap = colormap;
        init (visualnum, device.getScreen());

        // add a record to the Disposer so that we destroy the native
        // AwtGraphicsConfigData when this object goes away (i.e. after a
        // display change event)
        long x11CfgData = getAData();
        Disposer.addRecord(disposerReferent,
                           new X11GCDisposerRecord(x11CfgData));
    }

    /**
     * Return the graphics device associated with this configuration.
     */
    @Override
    public X11GraphicsDevice getDevice() {
        return device;
    }

    /**
     * Returns the visual id associated with this configuration.
     */
    public int getVisual () {
        return visual;
    }


    /**
     * Returns the depth associated with this configuration.
     */
    public int getDepth () {
        return depth;
    }

    /**
     * Returns the colormap associated with this configuration.
     */
    public int getColormap () {
        return colormap;
    }

    /**
     * Returns a number of bits allocated per pixel
     * (might be different from depth)
     */
    public int getBitsPerPixel() {
        return bitsPerPixel;
    }

    public synchronized SurfaceType getSurfaceType() {
        if (surfaceType != null) {
            return surfaceType;
        }

        surfaceType = X11SurfaceData.getSurfaceType(this, Transparency.OPAQUE);
        return surfaceType;
    }

    @Override
    public Object getProxyKey() {
        return device.getProxyKeyFor(getSurfaceType());
    }

    /**
     * Return the RenderLoops this type of destination uses for
     * solid fills and strokes.
     */
    public synchronized RenderLoops getSolidLoops(SurfaceType stype) {
        if (solidloops == null) {
            solidloops = SurfaceData.makeRenderLoops(SurfaceType.OpaqueColor,
                                                     CompositeType.SrcNoEa,
                                                     stype);
        }
        return solidloops;
    }

    /**
     * Returns the color model associated with this configuration.
     */
    @Override
    public synchronized ColorModel getColorModel() {
        if (colorModel == null)  {
            // Force SystemColors to be resolved before we create the CM
            java.awt.SystemColor.window.getRGB();
            // This method, makeColorModel(), can return null if the
            // toolkit is not initialized yet.
            // The toolkit will then call back to this routine after it
            // is initialized and makeColorModel() should return a non-null
            // colorModel.
            colorModel = makeColorModel();
            if (colorModel == null)
                colorModel = Toolkit.getDefaultToolkit ().getColorModel ();
        }

        return colorModel;
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

    public static DirectColorModel createDCM32(int rMask, int gMask, int bMask,
                                               int aMask, boolean aPre) {
        return new DirectColorModel(
            ColorSpace.getInstance(ColorSpace.CS_sRGB),
            32, rMask, gMask, bMask, aMask, aPre, DataBuffer.TYPE_INT);
    }

    public static ComponentColorModel createABGRCCM() {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int[] nBits = {8, 8, 8, 8};
        int[] bOffs = {3, 2, 1, 0};
        return new ComponentColorModel(cs, nBits, true, true,
                                       Transparency.TRANSLUCENT,
                                       DataBuffer.TYPE_BYTE);
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
        double scale = getScale();
        return AffineTransform.getScaleInstance(scale, scale);
    }

    public int getScale() {
        return getDevice().getScaleFactor();
    }

    public int scaleUp(int x) {
        return Region.clipRound(x * (double)getScale());
    }

    public int scaleDown(int x) {
        return Region.clipRound(x / (double)getScale());
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
        double xscale = getXResolution(device.getScreen()) / 72.0;
        double yscale = getYResolution(device.getScreen()) / 72.0;
        return new AffineTransform(xscale, 0.0, 0.0, yscale, 0.0, 0.0);
    }

    private native double getXResolution(int screen);
    private native double getYResolution(int screen);

    public long getAData() {
        return aData;
    }

    public String toString() {
        return ("X11GraphicsConfig[dev="+device+
                ",vis=0x"+Integer.toHexString(visual)+
                "]");
    }

    /*
     * Initialize JNI field and method IDs for fields that may be
     *  accessed from C.
     */
    private static native void initIDs();

    static {
        initIDs ();
    }

    @Override
    public final Rectangle getBounds() {
        return device.getBounds();
    }

    private static class XDBECapabilities extends BufferCapabilities {
        public XDBECapabilities() {
            super(imageCaps, imageCaps, FlipContents.UNDEFINED);
        }
    }

    @Override
    public BufferCapabilities getBufferCapabilities() {
        if (bufferCaps == null) {
            if (doubleBuffer) {
                bufferCaps = new XDBECapabilities();
            } else {
                bufferCaps = super.getBufferCapabilities();
            }
        }
        return bufferCaps;
    }

    @Override
    public ImageCapabilities getImageCapabilities() {
        return imageCaps;
    }

    public boolean isDoubleBuffered() {
        return doubleBuffer;
    }

    private static native void dispose(long x11ConfigData);

    private static class X11GCDisposerRecord implements DisposerRecord {
        private long x11ConfigData;
        public X11GCDisposerRecord(long x11CfgData) {
            this.x11ConfigData = x11CfgData;
        }
        @Override
        public synchronized void dispose() {
            if (x11ConfigData != 0L) {
                X11GraphicsConfig.dispose(x11ConfigData);
                x11ConfigData = 0L;
            }
        }
    }

    /**
     * The following methods are invoked from {M,X}Toolkit.java and
     * X11ComponentPeer.java rather than having the X11-dependent
     * implementations hardcoded in those classes.  This way the appropriate
     * actions are taken based on the peer's GraphicsConfig, whether it is
     * an X11GraphicsConfig or a GLXGraphicsConfig.
     */

    /**
     * Creates a new SurfaceData that will be associated with the given
     * X11ComponentPeer.
     */
    public SurfaceData createSurfaceData(X11ComponentPeer peer) {
        return X11SurfaceData.createData(peer);
    }

    /**
     * Creates a new hidden-acceleration image of the given width and height
     * that is associated with the target Component.
     */
    public Image createAcceleratedImage(Component target,
                                        int width, int height)
    {
        // As of 1.7 we no longer create pmoffscreens here...
        ColorModel model = getColorModel(Transparency.OPAQUE);
        WritableRaster wr =
            model.createCompatibleWritableRaster(width, height);
        return new OffScreenImage(target, model, wr,
                                  model.isAlphaPremultiplied());
    }

    /**
     * The following methods correspond to the multibuffering methods in
     * X11ComponentPeer.java...
     */

    private native long createBackBuffer(long window, int swapAction);
    private native void swapBuffers(long window, int swapAction);

    /**
     * Attempts to create an XDBE-based backbuffer for the given peer.  If
     * the requested configuration is not natively supported, an AWTException
     * is thrown.  Otherwise, if the backbuffer creation is successful, a
     * handle to the native backbuffer is returned.
     */
    public long createBackBuffer(X11ComponentPeer peer,
                                 int numBuffers, BufferCapabilities caps)
        throws AWTException
    {
        if (!X11GraphicsDevice.isDBESupported()) {
            throw new AWTException("Page flipping is not supported");
        }
        if (numBuffers > 2) {
            throw new AWTException(
                "Only double or single buffering is supported");
        }
        BufferCapabilities configCaps = getBufferCapabilities();
        if (!configCaps.isPageFlipping()) {
            throw new AWTException("Page flipping is not supported");
        }

        long window = peer.getContentWindow();
        int swapAction = getSwapAction(caps.getFlipContents());

        return createBackBuffer(window, swapAction);
    }

    /**
     * Destroys the backbuffer object represented by the given handle value.
     */
    public native void destroyBackBuffer(long backBuffer);

    /**
     * Creates a VolatileImage that essentially wraps the target Component's
     * backbuffer, using the provided backbuffer handle.
     */
    public VolatileImage createBackBufferImage(Component target,
                                               long backBuffer)
    {
        // it is possible for the component to have size 0x0, adjust it to
        // be at least 1x1 to avoid IAE
        int w = Math.max(1, target.getWidth());
        int h = Math.max(1, target.getHeight());
        return new SunVolatileImage(target,
                                    w, h,
                                    Long.valueOf(backBuffer));
    }

    /**
     * Performs the native XDBE flip operation for the given target Component.
     */
    public void flip(X11ComponentPeer peer,
                     Component target, VolatileImage xBackBuffer,
                     int x1, int y1, int x2, int y2,
                     BufferCapabilities.FlipContents flipAction)
    {
        long window = peer.getContentWindow();
        int swapAction = getSwapAction(flipAction);
        swapBuffers(window, swapAction);
    }

    /**
     * Maps the given FlipContents constant to the associated XDBE swap
     * action constant.
     */
    private static int getSwapAction(
        BufferCapabilities.FlipContents flipAction) {
        if (flipAction == BufferCapabilities.FlipContents.BACKGROUND) {
            return 0x01;
        } else if (flipAction == BufferCapabilities.FlipContents.PRIOR) {
            return 0x02;
        } else if (flipAction == BufferCapabilities.FlipContents.COPIED) {
            return 0x03;
        } else {
            return 0x00; // UNDEFINED
        }
    }

    @Override
    public boolean isTranslucencyCapable() {
        return isTranslucencyCapable(getAData());
    }

    private native boolean isTranslucencyCapable(long x11ConfigData);
}
