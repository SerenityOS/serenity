/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AlphaComposite;
import java.awt.BufferCapabilities;
import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import sun.awt.SunHints;
import sun.awt.image.DataBufferNative;
import sun.awt.image.PixelConverter;
import sun.awt.image.SurfaceManager;
import sun.awt.image.WritableRasterNative;
import sun.awt.windows.WComponentPeer;
import sun.java2d.pipe.hw.AccelSurface;
import sun.java2d.InvalidPipeException;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.loops.GraphicsPrimitive;
import sun.java2d.loops.MaskFill;
import sun.java2d.loops.SurfaceType;
import sun.java2d.loops.CompositeType;
import sun.java2d.pipe.ParallelogramPipe;
import sun.java2d.pipe.PixelToParallelogramConverter;
import sun.java2d.pipe.RenderBuffer;
import sun.java2d.pipe.TextPipe;
import sun.java2d.pipe.Region;
import static sun.java2d.pipe.BufferedOpCodes.*;
import static sun.java2d.d3d.D3DContext.D3DContextCaps.*;
import static sun.java2d.pipe.hw.ExtendedBufferCapabilities.VSyncType.*;
import sun.java2d.pipe.hw.ExtendedBufferCapabilities.VSyncType;
import java.awt.BufferCapabilities.FlipContents;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import sun.awt.SunToolkit;
import sun.awt.image.SunVolatileImage;
import sun.awt.windows.WWindowPeer;
import sun.java2d.ScreenUpdateManager;
import sun.java2d.StateTracker;
import sun.java2d.SurfaceDataProxy;
import sun.java2d.pipe.hw.ExtendedBufferCapabilities;

/**
 * This class describes a D3D "surface", that is, a region of pixels
 * managed via D3D.  An D3DSurfaceData can be tagged with one of three
 * different SurfaceType objects for the purpose of registering loops, etc.
 * This diagram shows the hierarchy of D3D SurfaceTypes:
 *
 *                               Any
 *                             /     \
 *                    D3DSurface     D3DTexture
 *                         |
 *                   D3DSurfaceRTT
 *
 * D3DSurface
 * This kind of surface can be rendered to using D3D APIs.  It is also
 * possible to copy a D3DSurface to another D3DSurface (or to itself).
 *
 * D3DTexture
 * This kind of surface cannot be rendered to using D3D (in the same sense
 * as in D3DSurface).  However, it is possible to upload a region of pixels
 * to a D3DTexture object via Lock/UnlockRect().  One can also copy a
 * surface of type D3DTexture to a D3DSurface by binding the texture
 * to a quad and then rendering it to the destination surface (this process
 * is known as "texture mapping").
 *
 * D3DSurfaceRTT
 * This kind of surface can be thought of as a sort of hybrid between
 * D3DSurface and D3DTexture, in that one can render to this kind of
 * surface as if it were of type D3DSurface, but the process of copying
 * this kind of surface to another is more like a D3DTexture.  (Note that
 * "RTT" stands for "render-to-texture".)
 *
 * In addition to these SurfaceType variants, we have also defined some
 * constants that describe in more detail the type of underlying D3D
 * surface.  This table helps explain the relationships between those
 * "type" constants and their corresponding SurfaceType:
 *
 * D3D Type          Corresponding SurfaceType
 * --------          -------------------------
 * RT_PLAIN          D3DSurface
 * TEXTURE           D3DTexture
 * FLIP_BACKBUFFER   D3DSurface
 * RT_TEXTURE        D3DSurfaceRTT
 */
public class D3DSurfaceData extends SurfaceData implements AccelSurface {

    /**
     * To be used with getNativeResource() only.
     * @see #getNativeResource
     */
    public static final int D3D_DEVICE_RESOURCE= 100;
    /*
     * Surface types.
     * We use these surface types when copying from a sw surface
     * to a surface or texture.
     */
    public static final int ST_INT_ARGB        = 0;
    public static final int ST_INT_ARGB_PRE    = 1;
    public static final int ST_INT_ARGB_BM     = 2;
    public static final int ST_INT_RGB         = 3;
    public static final int ST_INT_BGR         = 4;
    public static final int ST_USHORT_565_RGB  = 5;
    public static final int ST_USHORT_555_RGB  = 6;
    public static final int ST_BYTE_INDEXED    = 7;
    public static final int ST_BYTE_INDEXED_BM = 8;
    public static final int ST_3BYTE_BGR       = 9;

    /** Equals to D3DSWAPEFFECT_DISCARD */
    public static final int SWAP_DISCARD       = 1;
    /** Equals to D3DSWAPEFFECT_FLIP    */
    public static final int SWAP_FLIP          = 2;
    /** Equals to D3DSWAPEFFECT_COPY    */
    public static final int SWAP_COPY          = 3;
    /*
     * SurfaceTypes
     */
    private static final String DESC_D3D_SURFACE = "D3D Surface";
    private static final String DESC_D3D_SURFACE_RTT =
        "D3D Surface (render-to-texture)";
    private static final String DESC_D3D_TEXTURE = "D3D Texture";

    // REMIND: regarding ArgbPre??
    static final SurfaceType D3DSurface =
        SurfaceType.Any.deriveSubType(DESC_D3D_SURFACE,
                                      PixelConverter.ArgbPre.instance);
    static final SurfaceType D3DSurfaceRTT =
        D3DSurface.deriveSubType(DESC_D3D_SURFACE_RTT);
    static final SurfaceType D3DTexture =
        SurfaceType.Any.deriveSubType(DESC_D3D_TEXTURE);

    private int type;
    private int width, height;
    private final double scaleX;
    private final double scaleY;
    // these fields are set from the native code when the surface is
    // initialized
    private int nativeWidth, nativeHeight;
    protected WComponentPeer peer;
    private Image offscreenImage;
    protected D3DGraphicsDevice graphicsDevice;

    private int swapEffect;
    private VSyncType syncType;
    private int backBuffersNum;

    private WritableRasterNative wrn;

    protected static D3DRenderer d3dRenderPipe;
    protected static PixelToParallelogramConverter d3dTxRenderPipe;
    protected static ParallelogramPipe d3dAAPgramPipe;
    protected static D3DTextRenderer d3dTextPipe;
    protected static D3DDrawImage d3dImagePipe;

    private native boolean initTexture(long pData, boolean isRTT,
                                       boolean isOpaque);
    private native boolean initFlipBackbuffer(long pData, long pPeerData,
                                              int numbuffers,
                                              int swapEffect, int syncType);
    private native boolean initRTSurface(long pData, boolean isOpaque);
    private native void initOps(int screen, int width, int height);

    static {
        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        d3dImagePipe = new D3DDrawImage();
        d3dTextPipe = new D3DTextRenderer(rq);
        d3dRenderPipe = new D3DRenderer(rq);
        if (GraphicsPrimitive.tracingEnabled()) {
            d3dTextPipe = d3dTextPipe.traceWrap();
            d3dRenderPipe = d3dRenderPipe.traceWrap();
            //The wrapped d3dRenderPipe will wrap the AA pipe as well...
            //d3dAAPgramPipe = d3dRenderPipe.traceWrap();
        }
        d3dAAPgramPipe = d3dRenderPipe.getAAParallelogramPipe();
        d3dTxRenderPipe =
            new PixelToParallelogramConverter(d3dRenderPipe, d3dRenderPipe,
                                              1.0, 0.25, true);

        D3DBlitLoops.register();
        D3DMaskFill.register();
        D3DMaskBlit.register();
    }

    protected D3DSurfaceData(WComponentPeer peer, D3DGraphicsConfig gc,
                             int width, int height, Image image,
                             ColorModel cm, int numBackBuffers,
                             int swapEffect, VSyncType vSyncType,
                             int type)
    {
        super(getCustomSurfaceType(type), cm);
        this.graphicsDevice = gc.getD3DDevice();
        this.scaleX = type == TEXTURE ? 1 : graphicsDevice.getDefaultScaleX();
        this.scaleY = type == TEXTURE ? 1 : graphicsDevice.getDefaultScaleY();
        this.peer = peer;
        this.type = type;

        if (scaleX == 1 && scaleY == 1) {
            this.width = width;
            this.height = height;
        } else if (peer instanceof WWindowPeer) {
            Dimension scaledSize = ((WWindowPeer) peer).getScaledWindowSize();
            this.width = scaledSize.width;
            this.height = scaledSize.height;
        } else {
            this.width = Region.clipRound(width * scaleX);
            this.height = Region.clipRound(height * scaleY);
        }

        this.offscreenImage = image;
        this.backBuffersNum = numBackBuffers;
        this.swapEffect = swapEffect;
        this.syncType = vSyncType;

        initOps(graphicsDevice.getScreen(), this.width, this.height);
        if (type == WINDOW) {
            // we put the surface into the "lost"
            // state; it will be restored by the D3DScreenUpdateManager
            // prior to rendering to it for the first time. This is done
            // so that vram is not wasted for surfaces never rendered to
            setSurfaceLost(true);
        } else {
            initSurface();
        }
        setBlitProxyKey(gc.getProxyKey());
    }

    @Override
    public double getDefaultScaleX() {
        return scaleX;
    }

    @Override
    public double getDefaultScaleY() {
        return scaleY;
    }

    @Override
    public SurfaceDataProxy makeProxyFor(SurfaceData srcData) {
        return D3DSurfaceDataProxy.
            createProxy(srcData,
                        (D3DGraphicsConfig)graphicsDevice.getDefaultConfiguration());
    }

    /**
     * Creates a SurfaceData object representing the back buffer of a
     * double-buffered on-screen Window.
     */
    public static D3DSurfaceData createData(WComponentPeer peer, Image image) {
        D3DGraphicsConfig gc = getGC(peer);
        if (gc == null || !peer.isAccelCapable()) {
            return null;
        }
        BufferCapabilities caps = peer.getBackBufferCaps();
        VSyncType vSyncType = VSYNC_DEFAULT;
        if (caps instanceof ExtendedBufferCapabilities) {
            vSyncType = ((ExtendedBufferCapabilities)caps).getVSync();
        }
        Rectangle r = peer.getBounds();
        BufferCapabilities.FlipContents flip = caps.getFlipContents();
        int swapEffect;
        if (flip == FlipContents.COPIED) {
            swapEffect = SWAP_COPY;
        } else if (flip == FlipContents.PRIOR) {
            swapEffect = SWAP_FLIP;
        } else { // flip == FlipContents.UNDEFINED || .BACKGROUND
            swapEffect = SWAP_DISCARD;
        }
        return new D3DSurfaceData(peer, gc, r.width, r.height,
                                  image, peer.getColorModel(),
                                  peer.getBackBuffersNum(),
                                  swapEffect, vSyncType, FLIP_BACKBUFFER);
    }

    /**
     * Returns a WINDOW type of surface - a
     * swap chain which serves as an on-screen surface,
     * handled by the D3DScreenUpdateManager.
     *
     * Note that the native surface is not initialized
     * when the surface is created to avoid using excessive
     * resources, and the surface is placed into the lost
     * state. It will be restored prior to any rendering
     * to it.
     *
     * @param peer peer for which the onscreen surface is to be created
     * @return a D3DWindowSurfaceData (flip chain) surface
     */
    public static D3DSurfaceData createData(WComponentPeer peer) {
        D3DGraphicsConfig gc = getGC(peer);
        if (gc == null || !peer.isAccelCapable()) {
            return null;
        }
        return new D3DWindowSurfaceData(peer, gc);
    }

    /**
     * Creates a SurfaceData object representing an off-screen buffer (either
     * a plain surface or Texture).
     */
    public static D3DSurfaceData createData(D3DGraphicsConfig gc,
                                            int width, int height,
                                            ColorModel cm,
                                            Image image, int type)
    {
        if (type == RT_TEXTURE) {
            boolean isOpaque = cm.getTransparency() == Transparency.OPAQUE;
            int cap = isOpaque ? CAPS_RT_TEXTURE_OPAQUE : CAPS_RT_TEXTURE_ALPHA;
            if (!gc.getD3DDevice().isCapPresent(cap)) {
                type = RT_PLAIN;
            }
        }
        D3DSurfaceData ret = null;
        try {
            ret = new D3DSurfaceData(null, gc, width, height,
                                     image, cm, 0, SWAP_DISCARD, VSYNC_DEFAULT,
                                     type);
        } catch (InvalidPipeException ipe) {
            // try again - we might have ran out of vram, and rt textures
            // could take up more than a plain surface, so it might succeed
            if (type == RT_TEXTURE) {
                // If a RT_TEXTURE was requested do not attempt to create a
                // plain surface. (note that RT_TEXTURE can only be requested
                // from a VI so the cast is safe)
                if (((SunVolatileImage)image).getForcedAccelSurfaceType() !=
                    RT_TEXTURE)
                {
                    type = RT_PLAIN;
                    ret = new D3DSurfaceData(null, gc, width, height,
                                             image, cm, 0, SWAP_DISCARD,
                                             VSYNC_DEFAULT, type);
                }
            }
        }
        return ret;
    }

    /**
     * Returns the appropriate SurfaceType corresponding to the given D3D
     * surface type constant (e.g. TEXTURE -> D3DTexture).
     */
    private static SurfaceType getCustomSurfaceType(int d3dType) {
        switch (d3dType) {
        case TEXTURE:
            return D3DTexture;
        case RT_TEXTURE:
            return D3DSurfaceRTT;
        default:
            return D3DSurface;
        }
    }

    private boolean initSurfaceNow() {
        boolean isOpaque = (getTransparency() == Transparency.OPAQUE);
        switch (type) {
            case RT_PLAIN:
                return initRTSurface(getNativeOps(), isOpaque);
            case TEXTURE:
                return initTexture(getNativeOps(), false/*isRTT*/, isOpaque);
            case RT_TEXTURE:
                return initTexture(getNativeOps(), true/*isRTT*/,  isOpaque);
            // REMIND: we may want to pass the exact type to the native
            // level here so that we could choose the right presentation
            // interval for the frontbuffer (immediate vs v-synced)
            case WINDOW:
            case FLIP_BACKBUFFER:
                return initFlipBackbuffer(getNativeOps(), peer.getData(),
                                          backBuffersNum, swapEffect,
                                          syncType.id());
            default:
                return false;
        }
    }

    /**
     * Initializes the appropriate D3D offscreen surface based on the value
     * of the type parameter.  If the surface creation fails for any reason,
     * an OutOfMemoryError will be thrown.
     */
    protected void initSurface() {
        // any time we create or restore the surface, recreate the raster
        synchronized (this) {
            wrn = null;
        }
        // REMIND: somewhere a puppy died
        class Status {
            boolean success = false;
        };
        final Status status = new Status();
        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        rq.lock();
        try {
            rq.flushAndInvokeNow(new Runnable() {
                public void run() {
                    status.success = initSurfaceNow();
                }
            });
            if (!status.success) {
                throw new InvalidPipeException("Error creating D3DSurface");
            }
        } finally {
            rq.unlock();
        }
    }

    /**
     * Returns the D3DContext for the GraphicsConfig associated with this
     * surface.
     */
    public final D3DContext getContext() {
        return graphicsDevice.getContext();
    }

    /**
     * Returns one of the surface type constants defined above.
     */
    public final int getType() {
        return type;
    }

    private static native int  dbGetPixelNative(long pData, int x, int y);
    private static native void dbSetPixelNative(long pData, int x, int y,
                                                int pixel);
    static class D3DDataBufferNative extends DataBufferNative {
        int pixel;
        protected D3DDataBufferNative(SurfaceData sData,
                                      int type, int w, int h)
        {
            super(sData, type, w, h);
        }

        protected int getElem(final int x, final int y,
                              final SurfaceData sData)
        {
            if (sData.isSurfaceLost()) {
                return 0;
            }

            int retPixel;
            D3DRenderQueue rq = D3DRenderQueue.getInstance();
            rq.lock();
            try {
                rq.flushAndInvokeNow(new Runnable() {
                    public void run() {
                        pixel = dbGetPixelNative(sData.getNativeOps(), x, y);
                    }
                });
            } finally {
                retPixel = pixel;
                rq.unlock();
            }
            return retPixel;
        }

        protected void setElem(final int x, final int y, final int pixel,
                               final SurfaceData sData)
        {
            if (sData.isSurfaceLost()) {
                  return;
            }

            D3DRenderQueue rq = D3DRenderQueue.getInstance();
            rq.lock();
            try {
                rq.flushAndInvokeNow(new Runnable() {
                    public void run() {
                        dbSetPixelNative(sData.getNativeOps(), x, y, pixel);
                    }
                });
                sData.markDirty();
            } finally {
                rq.unlock();
            }
        }
    }

    public synchronized Raster getRaster(int x, int y, int w, int h) {
        if (wrn == null) {
            DirectColorModel dcm = (DirectColorModel)getColorModel();
            SampleModel smHw;
            int dataType = 0;
            int scanStride = width;

            if (dcm.getPixelSize() > 16) {
                dataType = DataBuffer.TYPE_INT;
            } else {
                // 15, 16
                dataType = DataBuffer.TYPE_USHORT;
            }

            // note that we have to use the surface width and height here,
            // not the passed w,h
            smHw = new SinglePixelPackedSampleModel(dataType, width, height,
                                                    scanStride, dcm.getMasks());
            DataBuffer dbn = new D3DDataBufferNative(this, dataType,
                                                     width, height);
            wrn = WritableRasterNative.createNativeRaster(smHw, dbn);
        }

        return wrn;
    }

    /**
     * For now, we can only render LCD text if:
     *   - the pixel shaders are available, and
     *   - blending is disabled, and
     *   - the source color is opaque
     *   - and the destination is opaque
     */
    public boolean canRenderLCDText(SunGraphics2D sg2d) {
        return
            graphicsDevice.isCapPresent(CAPS_LCD_SHADER) &&
            sg2d.compositeState <= SunGraphics2D.COMP_ISCOPY &&
            sg2d.paintState <= SunGraphics2D.PAINT_OPAQUECOLOR   &&
            sg2d.surfaceData.getTransparency() == Transparency.OPAQUE;
    }

    /**
     * If acceleration should no longer be used for this surface.
     * This implementation flags to the manager that it should no
     * longer attempt to re-create a D3DSurface.
     */
    void disableAccelerationForSurface() {
        if (offscreenImage != null) {
            SurfaceManager sm = SurfaceManager.getManager(offscreenImage);
            if (sm instanceof D3DVolatileSurfaceManager) {
                setSurfaceLost(true);
                ((D3DVolatileSurfaceManager)sm).setAccelerationEnabled(false);
            }
        }
    }

    public void validatePipe(SunGraphics2D sg2d) {
        TextPipe textpipe;
        boolean validated = false;

        // REMIND: the D3D pipeline doesn't support XOR!, more
        // fixes will be needed below. For now we disable D3D rendering
        // for the surface which had any XOR rendering done to.
        if (sg2d.compositeState >= SunGraphics2D.COMP_XOR) {
            super.validatePipe(sg2d);
            sg2d.imagepipe = d3dImagePipe;
            disableAccelerationForSurface();
            return;
        }

        // D3DTextRenderer handles both AA and non-AA text, but
        // only works with the following modes:
        // (Note: For LCD text we only enter this code path if
        // canRenderLCDText() has already validated that the mode is
        // CompositeType.SrcNoEa (opaque color), which will be subsumed
        // by the CompositeType.SrcNoEa (any color) test below.)

        if (/* CompositeType.SrcNoEa (any color) */
            (sg2d.compositeState <= SunGraphics2D.COMP_ISCOPY &&
             sg2d.paintState <= SunGraphics2D.PAINT_ALPHACOLOR)        ||

            /* CompositeType.SrcOver (any color) */
            (sg2d.compositeState == SunGraphics2D.COMP_ALPHA    &&
             sg2d.paintState <= SunGraphics2D.PAINT_ALPHACOLOR &&
             (((AlphaComposite)sg2d.composite).getRule() ==
              AlphaComposite.SRC_OVER))                       ||

            /* CompositeType.Xor (any color) */
            (sg2d.compositeState == SunGraphics2D.COMP_XOR &&
             sg2d.paintState <= SunGraphics2D.PAINT_ALPHACOLOR))
        {
            textpipe = d3dTextPipe;
        } else {
            // do this to initialize textpipe correctly; we will attempt
            // to override the non-text pipes below
            super.validatePipe(sg2d);
            textpipe = sg2d.textpipe;
            validated = true;
        }

        PixelToParallelogramConverter txPipe = null;
        D3DRenderer nonTxPipe = null;

        if (sg2d.antialiasHint != SunHints.INTVAL_ANTIALIAS_ON) {
            if (sg2d.paintState <= SunGraphics2D.PAINT_ALPHACOLOR) {
                if (sg2d.compositeState <= SunGraphics2D.COMP_XOR) {
                    txPipe = d3dTxRenderPipe;
                    nonTxPipe = d3dRenderPipe;
                }
            } else if (sg2d.compositeState <= SunGraphics2D.COMP_ALPHA) {
                if (D3DPaints.isValid(sg2d)) {
                    txPipe = d3dTxRenderPipe;
                    nonTxPipe = d3dRenderPipe;
                }
                // custom paints handled by super.validatePipe() below
            }
        } else {
            if (sg2d.paintState <= SunGraphics2D.PAINT_ALPHACOLOR) {
                if (graphicsDevice.isCapPresent(CAPS_AA_SHADER) &&
                    (sg2d.imageComp == CompositeType.SrcOverNoEa ||
                     sg2d.imageComp == CompositeType.SrcOver))
                {
                    if (!validated) {
                        super.validatePipe(sg2d);
                        validated = true;
                    }
                    PixelToParallelogramConverter aaConverter =
                        new PixelToParallelogramConverter(sg2d.shapepipe,
                                                          d3dAAPgramPipe,
                                                          1.0/8.0, 0.499,
                                                          false);
                    sg2d.drawpipe = aaConverter;
                    sg2d.fillpipe = aaConverter;
                    sg2d.shapepipe = aaConverter;
                } else if (sg2d.compositeState == SunGraphics2D.COMP_XOR) {
                    // install the solid pipes when AA and XOR are both enabled
                    txPipe = d3dTxRenderPipe;
                    nonTxPipe = d3dRenderPipe;
                }
            }
            // other cases handled by super.validatePipe() below
        }

        if (txPipe != null) {
            if (sg2d.transformState >= SunGraphics2D.TRANSFORM_TRANSLATESCALE) {
                sg2d.drawpipe = txPipe;
                sg2d.fillpipe = txPipe;
            } else if (sg2d.strokeState != SunGraphics2D.STROKE_THIN) {
                sg2d.drawpipe = txPipe;
                sg2d.fillpipe = nonTxPipe;
            } else {
                sg2d.drawpipe = nonTxPipe;
                sg2d.fillpipe = nonTxPipe;
            }
            // Note that we use the transforming pipe here because it
            // will examine the shape and possibly perform an optimized
            // operation if it can be simplified.  The simplifications
            // will be valid for all STROKE and TRANSFORM types.
            sg2d.shapepipe = txPipe;
        } else {
            if (!validated) {
                super.validatePipe(sg2d);
            }
        }

        // install the text pipe based on our earlier decision
        sg2d.textpipe = textpipe;

        // always override the image pipe with the specialized D3D pipe
        sg2d.imagepipe = d3dImagePipe;
    }

    @Override
    protected MaskFill getMaskFill(SunGraphics2D sg2d) {
        if (sg2d.paintState > SunGraphics2D.PAINT_ALPHACOLOR) {
            /*
             * We can only accelerate non-Color MaskFill operations if
             * all of the following conditions hold true:
             *   - there is an implementation for the given paintState
             *   - the current Paint can be accelerated for this destination
             *   - multitexturing is available (since we need to modulate
             *     the alpha mask texture with the paint texture)
             *
             * In all other cases, we return null, in which case the
             * validation code will choose a more general software-based loop.
             */
            if (!D3DPaints.isValid(sg2d) ||
                !graphicsDevice.isCapPresent(CAPS_MULTITEXTURE))
            {
                return null;
            }
        }
        return super.getMaskFill(sg2d);
    }

    @Override
    public boolean copyArea(SunGraphics2D sg2d, int x, int y, int w, int h,
                            int dx, int dy) {
        if (sg2d.compositeState >= SunGraphics2D.COMP_XOR) {
            return false;
        }
        d3dRenderPipe.copyArea(sg2d, x, y, w, h, dx, dy);
        return true;
    }

    @Override
    public void flush() {
        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        rq.lock();
        try {
            RenderBuffer buf = rq.getBuffer();
            rq.ensureCapacityAndAlignment(12, 4);
            buf.putInt(FLUSH_SURFACE);
            buf.putLong(getNativeOps());

            // this call is expected to complete synchronously, so flush now
            rq.flushNow();
        } finally {
            rq.unlock();
        }
    }

    /**
     * Disposes the native resources associated with the given D3DSurfaceData
     * (referenced by the pData parameter).  This method is invoked from
     * the native Dispose() method from the Disposer thread when the
     * Java-level D3DSurfaceData object is about to go away.
     */
    static void dispose(long pData) {
        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        rq.lock();
        try {
            RenderBuffer buf = rq.getBuffer();
            rq.ensureCapacityAndAlignment(12, 4);
            buf.putInt(DISPOSE_SURFACE);
            buf.putLong(pData);

            // this call is expected to complete synchronously, so flush now
            rq.flushNow();
        } finally {
            rq.unlock();
        }
    }

    static void swapBuffers(D3DSurfaceData sd,
                            final int x1, final int y1,
                            final int x2, final int y2)
    {
        long pData = sd.getNativeOps();
        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        // swapBuffers can be called from the toolkit thread by swing, we
        // should detect this and prevent the deadlocks
        if (D3DRenderQueue.isRenderQueueThread()) {
            if (!rq.tryLock()) {
                // if we could not obtain the lock, repaint the area
                // that was supposed to be swapped, and no-op this swap
                final Component target = (Component)sd.getPeer().getTarget();
                SunToolkit.executeOnEventHandlerThread(target, new Runnable() {
                    public void run() {
                        double scaleX = sd.getDefaultScaleX();
                        double scaleY = sd.getDefaultScaleY();
                        if (scaleX > 1 || scaleY > 1) {
                            int sx1 = (int) Math.floor(x1 / scaleX);
                            int sy1 = (int) Math.floor(y1 / scaleY);
                            int sx2 = (int) Math.ceil(x2 / scaleX);
                            int sy2 = (int) Math.ceil(y2 / scaleY);
                            target.repaint(sx1, sy1, sx2 - sx1, sy2 - sy1);
                        } else {
                            target.repaint(x1, y1, x2 - x1, y2 - y1);
                        }
                    }
                });
                return;
            }
        } else {
            rq.lock();
        }
        try {
            RenderBuffer buf = rq.getBuffer();
            rq.ensureCapacityAndAlignment(28, 4);
            buf.putInt(SWAP_BUFFERS);
            buf.putLong(pData);
            buf.putInt(x1);
            buf.putInt(y1);
            buf.putInt(x2);
            buf.putInt(y2);
            rq.flushNow();
        } finally {
            rq.unlock();
        }
    }

    /**
     * Returns destination Image associated with this SurfaceData.
     */
    public Object getDestination() {
        return offscreenImage;
    }

    public Rectangle getBounds() {
        if (type == FLIP_BACKBUFFER || type == WINDOW) {
            double scaleX = getDefaultScaleX();
            double scaleY = getDefaultScaleY();
            Rectangle r = peer.getBounds();
            r.x = r.y = 0;
            r.width = Region.clipRound(r.width * scaleX);
            r.height = Region.clipRound(r.height * scaleY);
            return r;
        } else {
            return new Rectangle(width, height);
        }
    }

    public Rectangle getNativeBounds() {
        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        // need to lock to make sure nativeWidth and Height are consistent
        // since they are set from the render thread from the native
        // level
        rq.lock();
        try {
            // REMIND: use xyoffsets?
            return new Rectangle(nativeWidth, nativeHeight);
        } finally {
            rq.unlock();
        }
    }


    public GraphicsConfiguration getDeviceConfiguration() {
        return graphicsDevice.getDefaultConfiguration();
    }

    public SurfaceData getReplacement() {
        return restoreContents(offscreenImage);
    }

    private static D3DGraphicsConfig getGC(WComponentPeer peer) {
        GraphicsConfiguration gc;
        if (peer != null) {
            gc =  peer.getGraphicsConfiguration();
        } else {
            GraphicsEnvironment env =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = env.getDefaultScreenDevice();
            gc = gd.getDefaultConfiguration();
        }
        return (gc instanceof D3DGraphicsConfig) ? (D3DGraphicsConfig)gc : null;
    }

    /**
     * Attempts to restore the surface by initializing the native data
     */
    void restoreSurface() {
        initSurface();
    }

    WComponentPeer getPeer() {
        return peer;
    }

    /**
     * We need to let the surface manager know that the surface is lost so
     * that for example BufferStrategy.contentsLost() returns correct result.
     * Normally the status of contentsLost is set in validate(), but in some
     * cases (like Swing's buffer per window) we intentionally don't call
     * validate from the toolkit thread but only check for the BS status.
     */
    @Override
    public void setSurfaceLost(boolean lost) {
        super.setSurfaceLost(lost);
        if (lost && offscreenImage != null) {
            SurfaceManager sm = SurfaceManager.getManager(offscreenImage);
            sm.acceleratedSurfaceLost();
        }
    }

    private static native long getNativeResourceNative(long sdops, int resType);
    /**
     * Returns a pointer to the native resource of specified {@code resType}
     * associated with this surface.
     *
     * Specifically, for {@code D3DSurfaceData} this method returns pointers of
     * the following:
     * <pre>
     * TEXTURE              - (IDirect3DTexture9*)
     * RT_TEXTURE, RT_PLAIN - (IDirect3DSurface9*)
     * FLIP_BACKBUFFER      - (IDirect3DSwapChain9*)
     * D3D_DEVICE_RESOURCE  - (IDirect3DDevice9*)
     * </pre>
     *
     * Multiple resources may be available for some types (i.e. for render to
     * texture one could retrieve both a destination surface by specifying
     * RT_TEXTURE, and a texture by using TEXTURE).
     *
     * Note: the pointer returned by this method is only valid on the rendering
     * thread.
     *
     * @return pointer to the native resource of specified type or 0L if
     * such resource doesn't exist or can not be retrieved.
     * @see sun.java2d.pipe.hw.AccelSurface#getNativeResource
     */
    public long getNativeResource(int resType) {
        return getNativeResourceNative(getNativeOps(), resType);
    }

    /**
     * Class representing an on-screen d3d surface. Since d3d can't
     * render to the screen directly, it is implemented as a swap chain,
     * controlled by D3DScreenUpdateManager.
     *
     * @see D3DScreenUpdateManager
     */
    public static class D3DWindowSurfaceData extends D3DSurfaceData {
        StateTracker dirtyTracker;

        public D3DWindowSurfaceData(WComponentPeer peer,
                                    D3DGraphicsConfig gc)
        {
            super(peer, gc,
                  peer.getBounds().width, peer.getBounds().height,
                  null, peer.getColorModel(), 1, SWAP_COPY, VSYNC_DEFAULT,
                  WINDOW);
            dirtyTracker = getStateTracker();
        }

        /**
         * {@inheritDoc}
         *
         * Overridden to use ScreenUpdateManager to obtain the replacement
         * surface.
         *
         * @see sun.java2d.ScreenUpdateManager#getReplacementScreenSurface
         */
        @Override
        public SurfaceData getReplacement() {
            ScreenUpdateManager mgr = ScreenUpdateManager.getInstance();
            return mgr.getReplacementScreenSurface(peer, this);
        }

        /**
         * Returns destination Component associated with this SurfaceData.
         */
        @Override
        public Object getDestination() {
            return peer.getTarget();
        }

        @Override
        void disableAccelerationForSurface() {
            // for on-screen surfaces we need to make sure a backup GDI surface is
            // is used until a new one is set (which may happen during a resize). We
            // don't want the screen update maanger to replace the surface right way
            // because it causes repainting issues in Swing, so we invalidate it,
            // this will prevent SUM from issuing a replaceSurfaceData call.
            setSurfaceLost(true);
            invalidate();
            flush();
            peer.disableAcceleration();
            ScreenUpdateManager.getInstance().dropScreenSurface(this);
        }

        @Override
        void restoreSurface() {
            if (!peer.isAccelCapable()) {
                throw new InvalidPipeException("Onscreen acceleration " +
                                               "disabled for this surface");
            }
            Window fsw = graphicsDevice.getFullScreenWindow();
            if (fsw != null && fsw != peer.getTarget()) {
                throw new InvalidPipeException("Can't restore onscreen surface"+
                                               " when in full-screen mode");
            }
            super.restoreSurface();
            // if initialization was unsuccessful, an IPE will be thrown
            // and the surface will remain lost
            setSurfaceLost(false);

            // This is to make sure the render target is reset after this
            // surface is restored. The reason for this is that sometimes this
            // surface can be restored from multiple threads (the screen update
            // manager's thread and app's rendering thread) at the same time,
            // and when that happens the second restoration will create the
            // native resource which will not be set as render target because
            // the BufferedContext's validate method will think that since the
            // surface data object didn't change then the current render target
            // is correct and no rendering will appear on the screen.
            D3DRenderQueue rq = D3DRenderQueue.getInstance();
            rq.lock();
            try {
                getContext().invalidateContext();
            } finally {
                rq.unlock();
            }
        }

        public boolean isDirty() {
            return !dirtyTracker.isCurrent();
        }

        public void markClean() {
            dirtyTracker = getStateTracker();
        }
    }

    /**
     * Updates the layered window with the contents of the surface.
     *
     * @param pd3dsd pointer to the D3DSDOps structure
     * @param pData pointer to the AwtWindow peer data
     * @param w width of the window
     * @param h height of the window
     * @see sun.awt.windows.TranslucentWindowPainter
     */
    public static native boolean updateWindowAccelImpl(long pd3dsd, long pData,
                                                       int w, int h);
}
