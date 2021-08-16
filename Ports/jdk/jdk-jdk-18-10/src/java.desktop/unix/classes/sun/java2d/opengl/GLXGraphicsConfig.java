/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.opengl;

import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.ImageCapabilities;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;

import sun.awt.X11ComponentPeer;
import sun.awt.X11GraphicsConfig;
import sun.awt.X11GraphicsDevice;
import sun.awt.X11GraphicsEnvironment;
import sun.awt.image.OffScreenImage;
import sun.awt.image.SunVolatileImage;
import sun.awt.image.SurfaceManager;
import sun.java2d.SunGraphics2D;
import sun.java2d.Surface;
import sun.java2d.SurfaceData;
import sun.java2d.opengl.GLXSurfaceData.GLXVSyncOffScreenSurfaceData;
import sun.java2d.pipe.hw.AccelSurface;
import sun.java2d.pipe.hw.AccelTypedVolatileImage;
import sun.java2d.pipe.hw.ContextCapabilities;

import static sun.java2d.opengl.OGLContext.OGLContextCaps;
import static sun.java2d.opengl.OGLContext.OGLContextCaps.CAPS_DOUBLEBUFFERED;
import static sun.java2d.opengl.OGLContext.OGLContextCaps.CAPS_EXT_FBOBJECT;
import static sun.java2d.opengl.OGLSurfaceData.FBOBJECT;
import static sun.java2d.opengl.OGLSurfaceData.TEXTURE;

public final class GLXGraphicsConfig
    extends X11GraphicsConfig
    implements OGLGraphicsConfig
{
    private static ImageCapabilities imageCaps = new GLXImageCaps();
    private BufferCapabilities bufferCaps;
    private long pConfigInfo;
    private ContextCapabilities oglCaps;
    private final OGLContext context;

    private static native long getGLXConfigInfo(int screennum, int visualnum);
    private static native int getOGLCapabilities(long configInfo);
    private native void initConfig(long aData, long ctxinfo);

    private GLXGraphicsConfig(X11GraphicsDevice device, int visualnum,
                              long configInfo, ContextCapabilities oglCaps)
    {
        super(device, visualnum, 0, 0,
              (oglCaps.getCaps() & CAPS_DOUBLEBUFFERED) != 0);
        pConfigInfo = configInfo;
        initConfig(getAData(), configInfo);
        this.oglCaps = oglCaps;
        context = new OGLContext(OGLRenderQueue.getInstance());
    }

    @Override
    public Object getProxyKey() {
        return this;
    }

    @Override
    public SurfaceData createManagedSurface(int w, int h, int transparency) {
        return GLXSurfaceData.createData(this, w, h,
                                         getColorModel(transparency),
                                         null,
                                         OGLSurfaceData.TEXTURE);
    }

    public static GLXGraphicsConfig getConfig(X11GraphicsDevice device,
                                              int visualnum)
    {
        if (!X11GraphicsEnvironment.isGLXAvailable()) {
            return null;
        }

        long cfginfo = 0;
        final String[] ids = new String[1];
        OGLRenderQueue rq = OGLRenderQueue.getInstance();
        rq.lock();
        try {
            // getGLXConfigInfo() creates and destroys temporary
            // surfaces/contexts, so we should first invalidate the current
            // Java-level context and flush the queue...
            OGLContext.invalidateCurrentContext();
            GLXGetConfigInfo action =
                new GLXGetConfigInfo(device.getScreen(), visualnum);
            rq.flushAndInvokeNow(action);
            cfginfo = action.getConfigInfo();
            if (cfginfo != 0L) {
                OGLContext.setScratchSurface(cfginfo);
                rq.flushAndInvokeNow(new Runnable() {
                    public void run() {
                        ids[0] = OGLContext.getOGLIdString();
                    }
                });
            }
        } finally {
            rq.unlock();
        }
        if (cfginfo == 0) {
            return null;
        }

        int oglCaps = getOGLCapabilities(cfginfo);
        ContextCapabilities caps = new OGLContextCaps(oglCaps, ids[0]);

        return new GLXGraphicsConfig(device, visualnum, cfginfo, caps);
    }

    /**
     * This is a small helper class that allows us to execute
     * getGLXConfigInfo() on the queue flushing thread.
     */
    private static class GLXGetConfigInfo implements Runnable {
        private int screen;
        private int visual;
        private long cfginfo;
        private GLXGetConfigInfo(int screen, int visual) {
            this.screen = screen;
            this.visual = visual;
        }
        public void run() {
            cfginfo = getGLXConfigInfo(screen, visual);
        }
        public long getConfigInfo() {
            return cfginfo;
        }
    }

    /**
     * Returns true if the provided capability bit is present for this config.
     * See OGLContext.java for a list of supported capabilities.
     */
    @Override
    public final boolean isCapPresent(int cap) {
        return ((oglCaps.getCaps() & cap) != 0);
    }

    @Override
    public final long getNativeConfigInfo() {
        return pConfigInfo;
    }

    @Override
    public final OGLContext getContext() {
        return context;
    }

    @Override
    public BufferedImage createCompatibleImage(int width, int height) {
        ColorModel model = new DirectColorModel(24, 0xff0000, 0xff00, 0xff);
        WritableRaster
            raster = model.createCompatibleWritableRaster(width, height);
        return new BufferedImage(model, raster, model.isAlphaPremultiplied(),
                                 null);
    }

    @Override
    public ColorModel getColorModel(int transparency) {
        switch (transparency) {
        case Transparency.OPAQUE:
            // REMIND: once the ColorModel spec is changed, this should be
            //         an opaque premultiplied DCM...
            return new DirectColorModel(24, 0xff0000, 0xff00, 0xff);
        case Transparency.BITMASK:
            return new DirectColorModel(25, 0xff0000, 0xff00, 0xff, 0x1000000);
        case Transparency.TRANSLUCENT:
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
            return new DirectColorModel(cs, 32,
                                        0xff0000, 0xff00, 0xff, 0xff000000,
                                        true, DataBuffer.TYPE_INT);
        default:
            return null;
        }
    }

    public String toString() {
        return ("GLXGraphicsConfig[dev="+getDevice()+
                ",vis=0x"+Integer.toHexString(visual)+
                "]");
    }

    /**
     * The following methods are invoked from MToolkit or XToolkit.java and
     * X11ComponentPeer.java rather than having the X11-dependent
     * implementations hardcoded in those classes.  This way the appropriate
     * actions are taken based on the peer's GraphicsConfig, whether it is
     * an X11GraphicsConfig or a GLXGraphicsConfig.
     */

    /**
     * Creates a new SurfaceData that will be associated with the given
     * X11ComponentPeer.
     */
    @Override
    public SurfaceData createSurfaceData(X11ComponentPeer peer) {
        return GLXSurfaceData.createData(peer);
    }

    /**
     * Creates a new hidden-acceleration image of the given width and height
     * that is associated with the target Component.
     */
    @Override
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
     * X11ComponentPeer.java...
     */

    /**
     * Attempts to create a GLX-based backbuffer for the given peer.  If
     * the requested configuration is not natively supported, an AWTException
     * is thrown.  Otherwise, if the backbuffer creation is successful, a
     * value of 1 is returned.
     */
    @Override
    public long createBackBuffer(X11ComponentPeer peer,
                                 int numBuffers, BufferCapabilities caps)
        throws AWTException
    {
        if (numBuffers > 2) {
            throw new AWTException(
                "Only double or single buffering is supported");
        }
        BufferCapabilities configCaps = getBufferCapabilities();
        if (!configCaps.isPageFlipping()) {
            throw new AWTException("Page flipping is not supported");
        }
        if (caps.getFlipContents() == BufferCapabilities.FlipContents.PRIOR) {
            throw new AWTException("FlipContents.PRIOR is not supported");
        }

        // non-zero return value means backbuffer creation was successful
        // (checked in X11ComponentPeer.flip(), etc.)
        return 1;
    }

    /**
     * Destroys the backbuffer object represented by the given handle value.
     */
    @Override
    public void destroyBackBuffer(long backBuffer) {
    }

    /**
     * Creates a VolatileImage that essentially wraps the target Component's
     * backbuffer (the provided backbuffer handle is essentially ignored).
     */
    @Override
    public VolatileImage createBackBufferImage(Component target,
                                               long backBuffer)
    {
        return new SunVolatileImage(target,
                                    target.getWidth(), target.getHeight(),
                                    Boolean.TRUE);
    }

    /**
     * Performs the native GLX flip operation for the given target Component.
     */
    @Override
    public void flip(X11ComponentPeer peer,
                     Component target, VolatileImage xBackBuffer,
                     int x1, int y1, int x2, int y2,
                     BufferCapabilities.FlipContents flipAction)
    {
        if (flipAction == BufferCapabilities.FlipContents.COPIED) {
            SurfaceManager vsm = SurfaceManager.getManager(xBackBuffer);
            SurfaceData sd = vsm.getPrimarySurfaceData();

            if (sd instanceof GLXVSyncOffScreenSurfaceData) {
                GLXVSyncOffScreenSurfaceData vsd =
                    (GLXVSyncOffScreenSurfaceData)sd;
                SurfaceData bbsd = vsd.getFlipSurface();
                Graphics2D bbg =
                    new SunGraphics2D(bbsd, Color.black, Color.white, null);
                try {
                    bbg.drawImage(xBackBuffer, 0, 0, null);
                } finally {
                    bbg.dispose();
                }
            } else {
                Graphics g = peer.getGraphics();
                try {
                    g.drawImage(xBackBuffer,
                                x1, y1, x2, y2,
                                x1, y1, x2, y2,
                                null);
                } finally {
                    g.dispose();
                }
                return;
            }
        } else if (flipAction == BufferCapabilities.FlipContents.PRIOR) {
            // not supported by GLX...
            return;
        }

        OGLSurfaceData.swapBuffers(peer.getContentWindow());

        if (flipAction == BufferCapabilities.FlipContents.BACKGROUND) {
            Graphics g = xBackBuffer.getGraphics();
            try {
                g.setColor(target.getBackground());
                g.fillRect(0, 0,
                           xBackBuffer.getWidth(),
                           xBackBuffer.getHeight());
            } finally {
                g.dispose();
            }
        }
    }

    private static class GLXBufferCaps extends BufferCapabilities {
        public GLXBufferCaps(boolean dblBuf) {
            super(imageCaps, imageCaps,
                  dblBuf ? FlipContents.UNDEFINED : null);
        }
    }

    @Override
    public BufferCapabilities getBufferCapabilities() {
        if (bufferCaps == null) {
            bufferCaps = new GLXBufferCaps(isDoubleBuffered());
        }
        return bufferCaps;
    }

    private static class GLXImageCaps extends ImageCapabilities {
        private GLXImageCaps() {
            super(true);
        }
        public boolean isTrueVolatile() {
            return true;
        }
    }

    @Override
    public ImageCapabilities getImageCapabilities() {
        return imageCaps;
    }

    @Override
    public VolatileImage
        createCompatibleVolatileImage(int width, int height,
                                      int transparency, int type)
    {
        if ((type != FBOBJECT && type != TEXTURE)
                || transparency == Transparency.BITMASK
                || type == FBOBJECT && !isCapPresent(CAPS_EXT_FBOBJECT)) {
            return null;
        }
        SunVolatileImage vi = new AccelTypedVolatileImage(this, width, height,
                                                          transparency, type);
        Surface sd = vi.getDestSurface();
        if (!(sd instanceof AccelSurface) ||
            ((AccelSurface)sd).getType() != type)
        {
            vi.flush();
            vi = null;
        }

        return vi;
    }

    @Override
    public ContextCapabilities getContextCapabilities() {
        return oglCaps;
    }
}
