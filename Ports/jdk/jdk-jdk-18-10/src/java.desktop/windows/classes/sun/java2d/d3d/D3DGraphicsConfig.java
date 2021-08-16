/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.d3d;

import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.ImageCapabilities;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.VolatileImage;
import sun.awt.Win32GraphicsConfig;
import sun.awt.image.SunVolatileImage;
import sun.awt.image.SurfaceManager;
import sun.awt.windows.WComponentPeer;
import sun.java2d.Surface;
import sun.java2d.SurfaceData;
import sun.java2d.pipe.hw.AccelTypedVolatileImage;
import sun.java2d.pipe.hw.AccelGraphicsConfig;
import sun.java2d.pipe.hw.AccelSurface;
import sun.java2d.pipe.hw.ContextCapabilities;
import static sun.java2d.pipe.hw.AccelSurface.*;
import static sun.java2d.d3d.D3DContext.D3DContextCaps.*;

public final class D3DGraphicsConfig
    extends Win32GraphicsConfig
    implements AccelGraphicsConfig
{
    private static ImageCapabilities imageCaps = new D3DImageCaps();

    private BufferCapabilities bufferCaps;
    private final D3DGraphicsDevice device;

    @SuppressWarnings("deprecation")
    protected D3DGraphicsConfig(D3DGraphicsDevice device) {
        super(device, 0);
        this.device = device;
    }

    public SurfaceData createManagedSurface(int w, int h, int transparency) {
        return D3DSurfaceData.createData(this, w, h,
                                         getColorModel(transparency),
                                         null,
                                         D3DSurfaceData.TEXTURE);
    }

    @Override
    public synchronized void displayChanged() {
        super.displayChanged();
        // the context could hold a reference to a D3DSurfaceData, which in
        // turn has a reference back to this D3DGraphicsConfig, so in order
        // for this instance to be disposed we need to break the connection
        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        rq.lock();
        try {
            D3DContext.invalidateCurrentContext();
        } finally {
            rq.unlock();
        }
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

    @Override
    public String toString() {
        return ("D3DGraphicsConfig[dev="+device+",pixfmt="+visual+"]");
    }

    /**
     * The following methods are invoked from WComponentPeer.java rather
     * than having the Win32-dependent implementations hardcoded in that
     * class.  This way the appropriate actions are taken based on the peer's
     * GraphicsConfig, whether it is a Win32GraphicsConfig or a
     * D3DGraphicsConfig.
     */

    /**
     * Creates a new SurfaceData that will be associated with the given
     * WComponentPeer. D3D9 doesn't allow rendering to the screen,
     * so a GDI surface will be returned.
     */
    @Override
    public SurfaceData createSurfaceData(WComponentPeer peer,
                                         int numBackBuffers)
    {
        return super.createSurfaceData(peer, numBackBuffers);
    }

    /**
     * The following methods correspond to the multibuffering methods in
     * WComponentPeer.java...
     */

    /**
     * Checks that the requested configuration is natively supported; if not,
     * an AWTException is thrown.
     */
    @Override
    public void assertOperationSupported(Component target,
                                         int numBuffers,
                                         BufferCapabilities caps)
        throws AWTException
    {
        if (numBuffers < 2 || numBuffers > 4) {
            throw new AWTException("Only 2-4 buffers supported");
        }
        if (caps.getFlipContents() == BufferCapabilities.FlipContents.COPIED &&
            numBuffers != 2)
        {
            throw new AWTException("FlipContents.COPIED is only" +
                                   "supported for 2 buffers");
        }
    }

    /**
     * Creates a D3D-based backbuffer for the given peer and returns the
     * image wrapper.
     */
    @Override
    public VolatileImage createBackBuffer(WComponentPeer peer) {
        Component target = (Component)peer.getTarget();
        // it is possible for the component to have size 0x0, adjust it to
        // be at least 1x1 to avoid IAE
        int w = Math.max(1, target.getWidth());
        int h = Math.max(1, target.getHeight());
        return new SunVolatileImage(target, w, h, Boolean.TRUE);
    }

    /**
     * Performs the native D3D flip operation for the given target Component.
     */
    @Override
    public void flip(WComponentPeer peer,
                     Component target, VolatileImage backBuffer,
                     int x1, int y1, int x2, int y2,
                     BufferCapabilities.FlipContents flipAction)
    {
        // REMIND: we should actually get a surface data for the
        // backBuffer's VI
        SurfaceManager d3dvsm =
            SurfaceManager.getManager(backBuffer);
        SurfaceData sd = d3dvsm.getPrimarySurfaceData();
        if (sd instanceof D3DSurfaceData) {
            D3DSurfaceData d3dsd = (D3DSurfaceData)sd;
            double scaleX = sd.getDefaultScaleX();
            double scaleY = sd.getDefaultScaleY();
            if (scaleX > 1 || scaleY > 1) {
                int sx1 = (int) Math.floor(x1 * scaleX);
                int sy1 = (int) Math.floor(y1 * scaleY);
                int sx2 = (int) Math.ceil(x2 * scaleX);
                int sy2 = (int) Math.ceil(y2 * scaleY);
                D3DSurfaceData.swapBuffers(d3dsd, sx1, sy1, sx2, sy2);
            } else {
                D3DSurfaceData.swapBuffers(d3dsd, x1, y1, x2, y2);
            }
        } else {
            // the surface was likely lost could not have been restored
            Graphics g = peer.getGraphics();
            try {
                g.drawImage(backBuffer,
                            x1, y1, x2, y2,
                            x1, y1, x2, y2,
                            null);
            } finally {
                g.dispose();
            }
        }

        if (flipAction == BufferCapabilities.FlipContents.BACKGROUND) {
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
    }

    private static class D3DBufferCaps extends BufferCapabilities {
        public D3DBufferCaps() {
            // REMIND: should we indicate that the front-buffer
            // (the on-screen rendering) is not accelerated?
            super(imageCaps, imageCaps, FlipContents.UNDEFINED);
        }
        @Override
        public boolean isMultiBufferAvailable() {
            return true;
        }

    }

    @Override
    public BufferCapabilities getBufferCapabilities() {
        if (bufferCaps == null) {
            bufferCaps = new D3DBufferCaps();
        }
        return bufferCaps;
    }

    private static class D3DImageCaps extends ImageCapabilities {
        private D3DImageCaps() {
            super(true);
        }
        @Override
        public boolean isTrueVolatile() {
            return true;
        }
    }

    @Override
    public ImageCapabilities getImageCapabilities() {
        return imageCaps;
    }

    D3DGraphicsDevice getD3DDevice() {
        return device;
    }

    @Override
    public D3DContext getContext() {
        return device.getContext();
    }

    @Override
    public VolatileImage
        createCompatibleVolatileImage(int width, int height,
                                      int transparency, int type)
    {
        if (type == FLIP_BACKBUFFER || type == WINDOW || type == UNDEFINED ||
            transparency == Transparency.BITMASK)
        {
            return null;
        }
        boolean isOpaque = transparency == Transparency.OPAQUE;
        if (type == RT_TEXTURE) {
            int cap = isOpaque ? CAPS_RT_TEXTURE_OPAQUE : CAPS_RT_TEXTURE_ALPHA;
            if (!device.isCapPresent(cap)) {
                return null;
            }
        } else if (type == RT_PLAIN) {
            if (!isOpaque && !device.isCapPresent(CAPS_RT_PLAIN_ALPHA)) {
                return null;
            }
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
        return device.getContextCapabilities();
    }
}
