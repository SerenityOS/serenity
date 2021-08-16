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

import java.awt.Composite;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.lang.ref.WeakReference;
import java.lang.annotation.Native;
import sun.java2d.ScreenUpdateManager;
import sun.java2d.SurfaceData;
import sun.java2d.loops.Blit;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.GraphicsPrimitive;
import sun.java2d.loops.GraphicsPrimitiveMgr;
import sun.java2d.loops.ScaledBlit;
import sun.java2d.loops.SurfaceType;
import sun.java2d.loops.TransformBlit;
import sun.java2d.pipe.Region;
import sun.java2d.pipe.RenderBuffer;
import sun.java2d.pipe.RenderQueue;
import static sun.java2d.pipe.BufferedOpCodes.*;
import sun.java2d.windows.GDIWindowSurfaceData;

final class D3DBlitLoops {

    static void register() {
        Blit blitIntArgbPreToSurface =
            new D3DSwToSurfaceBlit(SurfaceType.IntArgbPre,
                                   D3DSurfaceData.ST_INT_ARGB_PRE);
        Blit blitIntArgbPreToTexture =
            new D3DSwToTextureBlit(SurfaceType.IntArgbPre,
                                   D3DSurfaceData.ST_INT_ARGB_PRE);
        TransformBlit transformBlitIntArgbPreToSurface =
            new D3DSwToSurfaceTransform(SurfaceType.IntArgbPre,
                                        D3DSurfaceData.ST_INT_ARGB_PRE);
        GraphicsPrimitive[] primitives = {
            // prevent D3DSurface -> Screen blits
            new D3DSurfaceToGDIWindowSurfaceBlit(),
            new D3DSurfaceToGDIWindowSurfaceScale(),
            new D3DSurfaceToGDIWindowSurfaceTransform(),

            // surface->surface ops
            new D3DSurfaceToSurfaceBlit(),
            new D3DSurfaceToSurfaceScale(),
            new D3DSurfaceToSurfaceTransform(),

            // render-to-texture surface->surface ops
            new D3DRTTSurfaceToSurfaceBlit(),
            new D3DRTTSurfaceToSurfaceScale(),
            new D3DRTTSurfaceToSurfaceTransform(),

            // surface->sw ops
            new D3DSurfaceToSwBlit(SurfaceType.IntArgb,
                                   D3DSurfaceData.ST_INT_ARGB),

            // sw->surface ops
            blitIntArgbPreToSurface,
            new D3DSwToSurfaceBlit(SurfaceType.IntArgb,
                                   D3DSurfaceData.ST_INT_ARGB),
            new D3DSwToSurfaceBlit(SurfaceType.IntRgb,
                                   D3DSurfaceData.ST_INT_RGB),
            new D3DSwToSurfaceBlit(SurfaceType.IntBgr,
                                   D3DSurfaceData.ST_INT_BGR),
            new D3DSwToSurfaceBlit(SurfaceType.ThreeByteBgr,
                                   D3DSurfaceData.ST_3BYTE_BGR),
            new D3DSwToSurfaceBlit(SurfaceType.Ushort565Rgb,
                                   D3DSurfaceData.ST_USHORT_565_RGB),
            new D3DSwToSurfaceBlit(SurfaceType.Ushort555Rgb,
                                   D3DSurfaceData.ST_USHORT_555_RGB),
            new D3DSwToSurfaceBlit(SurfaceType.ByteIndexed,
                                   D3DSurfaceData.ST_BYTE_INDEXED),
            // REMIND: we don't have a native sw loop to back this loop up
//            new D3DSwToSurfaceBlit(SurfaceType.ByteIndexedBm,
//                                   D3DSurfaceData.ST_BYTE_INDEXED_BM),
            new D3DGeneralBlit(D3DSurfaceData.D3DSurface,
                               CompositeType.AnyAlpha,
                               blitIntArgbPreToSurface),

            new D3DSwToSurfaceScale(SurfaceType.IntArgb,
                                    D3DSurfaceData.ST_INT_ARGB),
            new D3DSwToSurfaceScale(SurfaceType.IntArgbPre,
                                    D3DSurfaceData.ST_INT_ARGB_PRE),
            new D3DSwToSurfaceScale(SurfaceType.IntRgb,
                                    D3DSurfaceData.ST_INT_RGB),
            new D3DSwToSurfaceScale(SurfaceType.IntBgr,
                                    D3DSurfaceData.ST_INT_BGR),
            new D3DSwToSurfaceScale(SurfaceType.ThreeByteBgr,
                                    D3DSurfaceData.ST_3BYTE_BGR),
            new D3DSwToSurfaceScale(SurfaceType.Ushort565Rgb,
                                    D3DSurfaceData.ST_USHORT_565_RGB),
            new D3DSwToSurfaceScale(SurfaceType.Ushort555Rgb,
                                    D3DSurfaceData.ST_USHORT_555_RGB),
            new D3DSwToSurfaceScale(SurfaceType.ByteIndexed,
                                    D3DSurfaceData.ST_BYTE_INDEXED),
            // REMIND: we don't have a native sw loop to back this loop up
//            new D3DSwToSurfaceScale(SurfaceType.ByteIndexedBm,
//                                    D3DSurfaceData.ST_BYTE_INDEXED_BM),

            new D3DSwToSurfaceTransform(SurfaceType.IntArgb,
                                        D3DSurfaceData.ST_INT_ARGB),
            new D3DSwToSurfaceTransform(SurfaceType.IntRgb,
                                        D3DSurfaceData.ST_INT_RGB),
            new D3DSwToSurfaceTransform(SurfaceType.IntBgr,
                                        D3DSurfaceData.ST_INT_BGR),
            new D3DSwToSurfaceTransform(SurfaceType.ThreeByteBgr,
                                        D3DSurfaceData.ST_3BYTE_BGR),
            new D3DSwToSurfaceTransform(SurfaceType.Ushort565Rgb,
                                        D3DSurfaceData.ST_USHORT_565_RGB),
            new D3DSwToSurfaceTransform(SurfaceType.Ushort555Rgb,
                                        D3DSurfaceData.ST_USHORT_555_RGB),
            new D3DSwToSurfaceTransform(SurfaceType.ByteIndexed,
                                        D3DSurfaceData.ST_BYTE_INDEXED),
            // REMIND: we don't have a native sw loop to back this loop up
//            new D3DSwToSurfaceTransform(SurfaceType.ByteIndexedBm,
//                                        D3DSurfaceData.ST_BYTE_INDEXED_BM),
            transformBlitIntArgbPreToSurface,

            new D3DGeneralTransformedBlit(transformBlitIntArgbPreToSurface),

            // texture->surface ops
            new D3DTextureToSurfaceBlit(),
            new D3DTextureToSurfaceScale(),
            new D3DTextureToSurfaceTransform(),

            // sw->texture ops
            blitIntArgbPreToTexture,
            new D3DSwToTextureBlit(SurfaceType.IntRgb,
                                   D3DSurfaceData.ST_INT_RGB),
            new D3DSwToTextureBlit(SurfaceType.IntArgb,
                                   D3DSurfaceData.ST_INT_ARGB),
            new D3DSwToTextureBlit(SurfaceType.IntBgr,
                                   D3DSurfaceData.ST_INT_BGR),
            new D3DSwToTextureBlit(SurfaceType.ThreeByteBgr,
                                   D3DSurfaceData.ST_3BYTE_BGR),
            new D3DSwToTextureBlit(SurfaceType.Ushort565Rgb,
                                   D3DSurfaceData.ST_USHORT_565_RGB),
            new D3DSwToTextureBlit(SurfaceType.Ushort555Rgb,
                                   D3DSurfaceData.ST_USHORT_555_RGB),
            new D3DSwToTextureBlit(SurfaceType.ByteIndexed,
                                   D3DSurfaceData.ST_BYTE_INDEXED),
            // REMIND: we don't have a native sw loop to back this loop up
//            new D3DSwToTextureBlit(SurfaceType.ByteIndexedBm,
//                                   D3DSurfaceData.ST_BYTE_INDEXED_BM),
            new D3DGeneralBlit(D3DSurfaceData.D3DTexture,
                               CompositeType.SrcNoEa,
                               blitIntArgbPreToTexture),
        };
        GraphicsPrimitiveMgr.register(primitives);
    }

    /**
     * The following offsets are used to pack the parameters in
     * createPackedParams().  (They are also used at the native level when
     * unpacking the params.)
     */
    @Native private static final int OFFSET_SRCTYPE = 16;
    @Native private static final int OFFSET_HINT    =  8;
    @Native private static final int OFFSET_TEXTURE =  3;
    @Native private static final int OFFSET_RTT     =  2;
    @Native private static final int OFFSET_XFORM   =  1;
    @Native private static final int OFFSET_ISOBLIT =  0;

    /**
     * Packs the given parameters into a single int value in order to save
     * space on the rendering queue.
     */
    private static int createPackedParams(boolean isoblit, boolean texture,
                                          boolean rtt, boolean xform,
                                          int hint, int srctype)
    {
        return
            ((srctype           << OFFSET_SRCTYPE) |
             (hint              << OFFSET_HINT   ) |
             ((texture ? 1 : 0) << OFFSET_TEXTURE) |
             ((rtt     ? 1 : 0) << OFFSET_RTT    ) |
             ((xform   ? 1 : 0) << OFFSET_XFORM  ) |
             ((isoblit ? 1 : 0) << OFFSET_ISOBLIT));
    }

    /**
     * Enqueues a BLIT operation with the given parameters.  Note that the
     * RenderQueue lock must be held before calling this method.
     */
    private static void enqueueBlit(RenderQueue rq,
                                    SurfaceData src, SurfaceData dst,
                                    int packedParams,
                                    int sx1, int sy1,
                                    int sx2, int sy2,
                                    double dx1, double dy1,
                                    double dx2, double dy2)
    {
        // assert rq.lock.isHeldByCurrentThread();
        RenderBuffer buf = rq.getBuffer();
        rq.ensureCapacityAndAlignment(72, 24);
        buf.putInt(BLIT);
        buf.putInt(packedParams);
        buf.putInt(sx1).putInt(sy1);
        buf.putInt(sx2).putInt(sy2);
        buf.putDouble(dx1).putDouble(dy1);
        buf.putDouble(dx2).putDouble(dy2);
        buf.putLong(src.getNativeOps());
        buf.putLong(dst.getNativeOps());
    }

    static void Blit(SurfaceData srcData, SurfaceData dstData,
                     Composite comp, Region clip,
                     AffineTransform xform, int hint,
                     int sx1, int sy1,
                     int sx2, int sy2,
                     double dx1, double dy1,
                     double dx2, double dy2,
                     int srctype, boolean texture)
    {
        int ctxflags = 0;
        if (srcData.getTransparency() == Transparency.OPAQUE) {
            ctxflags |= D3DContext.SRC_IS_OPAQUE;
        }

        D3DSurfaceData d3dDst = (D3DSurfaceData)dstData;
        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        rq.lock();
        try {
            // make sure the RenderQueue keeps a hard reference to the
            // source (sysmem) SurfaceData to prevent it from being
            // disposed while the operation is processed on the QFT
            rq.addReference(srcData);

            if (texture) {
                // make sure we have a current context before uploading
                // the sysmem data to the texture object
                D3DContext.setScratchSurface(d3dDst.getContext());
            } else {
                D3DContext.validateContext(d3dDst, d3dDst,
                                           clip, comp, xform, null, null,
                                           ctxflags);
            }

            int packedParams = createPackedParams(false, texture,
                                                  false, xform != null,
                                                  hint, srctype);
            enqueueBlit(rq, srcData, dstData,
                        packedParams,
                        sx1, sy1, sx2, sy2,
                        dx1, dy1, dx2, dy2);

            // always flush immediately, since we (currently) have no means
            // of tracking changes to the system memory surface
            rq.flushNow();
        } finally {
            rq.unlock();
        }

        if (d3dDst.getType() == D3DSurfaceData.WINDOW) {
            // flush immediately when copying to the screen to improve
            // responsiveness of applications using VI or BI backbuffers
            D3DScreenUpdateManager mgr =
                (D3DScreenUpdateManager)ScreenUpdateManager.getInstance();
            mgr.runUpdateNow();
        }
    }

    /**
     * Note: The srcImg and biop parameters are only used when invoked
     * from the D3DBufImgOps.renderImageWithOp() method; in all other cases,
     * this method can be called with null values for those two parameters,
     * and they will be effectively ignored.
     */
    static void IsoBlit(SurfaceData srcData, SurfaceData dstData,
                        BufferedImage srcImg, BufferedImageOp biop,
                        Composite comp, Region clip,
                        AffineTransform xform, int hint,
                        int sx1, int sy1,
                        int sx2, int sy2,
                        double dx1, double dy1,
                        double dx2, double dy2,
                        boolean texture)
    {
        int ctxflags = 0;
        if (srcData.getTransparency() == Transparency.OPAQUE) {
            ctxflags |= D3DContext.SRC_IS_OPAQUE;
        }

        D3DSurfaceData d3dDst = (D3DSurfaceData)dstData;
        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        boolean rtt = false;
        rq.lock();
        try {
            D3DSurfaceData d3dSrc = (D3DSurfaceData)srcData;
            int srctype = d3dSrc.getType();
            D3DSurfaceData srcCtxData = d3dSrc;
            if (srctype == D3DSurfaceData.TEXTURE) {
                rtt = false;
            } else {
                // the source is a backbuffer, or render-to-texture
                // surface; we set rtt to true to differentiate this kind
                // of surface from a regular texture object
                rtt = true;
            }

            D3DContext.validateContext(srcCtxData, d3dDst,
                                       clip, comp, xform, null, null,
                                       ctxflags);

            if (biop != null) {
                D3DBufImgOps.enableBufImgOp(rq, d3dSrc, srcImg, biop);
            }

            int packedParams = createPackedParams(true, texture,
                                                  rtt, xform != null,
                                                  hint, 0 /*unused*/);
            enqueueBlit(rq, srcData, dstData,
                        packedParams,
                        sx1, sy1, sx2, sy2,
                        dx1, dy1, dx2, dy2);

            if (biop != null) {
                D3DBufImgOps.disableBufImgOp(rq, biop);
            }
        } finally {
            rq.unlock();
        }

        if (rtt && (d3dDst.getType() == D3DSurfaceData.WINDOW)) {
            // we only have to flush immediately when copying from a
            // (non-texture) surface to the screen; otherwise Swing apps
            // might appear unresponsive until the auto-flush completes
            D3DScreenUpdateManager mgr =
                (D3DScreenUpdateManager)ScreenUpdateManager.getInstance();
            mgr.runUpdateNow();
        }
    }
}

class D3DSurfaceToSurfaceBlit extends Blit {

    D3DSurfaceToSurfaceBlit() {
        super(D3DSurfaceData.D3DSurface,
              CompositeType.AnyAlpha,
              D3DSurfaceData.D3DSurface);
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy, int w, int h)
    {
        D3DBlitLoops.IsoBlit(src, dst,
                             null, null,
                             comp, clip, null,
                             AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                             sx, sy, sx+w, sy+h,
                             dx, dy, dx+w, dy+h,
                             false);
    }
}

class D3DSurfaceToSurfaceScale extends ScaledBlit {

    D3DSurfaceToSurfaceScale() {
        super(D3DSurfaceData.D3DSurface,
              CompositeType.AnyAlpha,
              D3DSurfaceData.D3DSurface);
    }

    public void Scale(SurfaceData src, SurfaceData dst,
                      Composite comp, Region clip,
                      int sx1, int sy1,
                      int sx2, int sy2,
                      double dx1, double dy1,
                      double dx2, double dy2)
    {
        D3DBlitLoops.IsoBlit(src, dst,
                             null, null,
                             comp, clip, null,
                             AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                             sx1, sy1, sx2, sy2,
                             dx1, dy1, dx2, dy2,
                             false);
    }
}

class D3DSurfaceToSurfaceTransform extends TransformBlit {

    D3DSurfaceToSurfaceTransform() {
        super(D3DSurfaceData.D3DSurface,
              CompositeType.AnyAlpha,
              D3DSurfaceData.D3DSurface);
    }

    public void Transform(SurfaceData src, SurfaceData dst,
                          Composite comp, Region clip,
                          AffineTransform at, int hint,
                          int sx, int sy, int dx, int dy,
                          int w, int h)
    {
        D3DBlitLoops.IsoBlit(src, dst,
                             null, null,
                             comp, clip, at, hint,
                             sx, sy, sx+w, sy+h,
                             dx, dy, dx+w, dy+h,
                             false);
    }
}

class D3DRTTSurfaceToSurfaceBlit extends Blit {

    D3DRTTSurfaceToSurfaceBlit() {
        super(D3DSurfaceData.D3DSurfaceRTT,
              CompositeType.AnyAlpha,
              D3DSurfaceData.D3DSurface);
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy, int w, int h)
    {
        D3DBlitLoops.IsoBlit(src, dst,
                             null, null,
                             comp, clip, null,
                             AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                             sx, sy, sx+w, sy+h,
                             dx, dy, dx+w, dy+h,
                             true);
    }
}

class D3DRTTSurfaceToSurfaceScale extends ScaledBlit {

    D3DRTTSurfaceToSurfaceScale() {
        super(D3DSurfaceData.D3DSurfaceRTT,
              CompositeType.AnyAlpha,
              D3DSurfaceData.D3DSurface);
    }

    public void Scale(SurfaceData src, SurfaceData dst,
                      Composite comp, Region clip,
                      int sx1, int sy1,
                      int sx2, int sy2,
                      double dx1, double dy1,
                      double dx2, double dy2)
    {
        D3DBlitLoops.IsoBlit(src, dst,
                             null, null,
                             comp, clip, null,
                             AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                             sx1, sy1, sx2, sy2,
                             dx1, dy1, dx2, dy2,
                             true);
    }
}

class D3DRTTSurfaceToSurfaceTransform extends TransformBlit {

    D3DRTTSurfaceToSurfaceTransform() {
        super(D3DSurfaceData.D3DSurfaceRTT,
              CompositeType.AnyAlpha,
              D3DSurfaceData.D3DSurface);
    }

    public void Transform(SurfaceData src, SurfaceData dst,
                          Composite comp, Region clip,
                          AffineTransform at, int hint,
                          int sx, int sy, int dx, int dy, int w, int h)
    {
        D3DBlitLoops.IsoBlit(src, dst,
                             null, null,
                             comp, clip, at, hint,
                             sx, sy, sx+w, sy+h,
                             dx, dy, dx+w, dy+h,
                             true);
    }
}

class D3DSurfaceToSwBlit extends Blit {

    private int typeval;
    private WeakReference<SurfaceData> srcTmp;

    // REMIND: destination will actually be opaque/premultiplied...
    D3DSurfaceToSwBlit(SurfaceType dstType, int typeval) {
        super(D3DSurfaceData.D3DSurface,
              CompositeType.SrcNoEa,
              dstType);
        this.typeval = typeval;
    }

    /*
     * Clip value is ignored in D3D SurfaceToSw blit.
     * Root Cause: The native interfaces to D3D use StretchRect API followed
     * by custom copy of pixels from Surface to Sysmem. As a result, clipping
     * in D3DSurfaceToSw works 'only' for Rect clips, provided, proper srcX,
     * srcY, dstX, dstY, width and height are passed to native interfaces.
     * Non rect clips (For example: Shape clips) are ignored completely.
     *
     * Solution: There are three solutions possible to fix this issue.
     * 1. Convert the entire Surface to Sysmem and perform regular Blit.
     *    An optimized version of this is to take up the conversion only
     *    when Shape clips are needed. Existing native interface will suffice
     *    for supporting Rect clips.
     * 2. With help of existing classes we could perform SwToSurface,
     *    SurfaceToSurface (implements clip) and SurfaceToSw (complete copy)
     *    in order.
     * 3. Modify the native D3D interface to accept clip and perform same logic
     *    as the second approach but at native side.
     *
     * Upon multiple experiments, the first approach has been found to be
     * faster than the others as it deploys 1-draw/copy operation for rect clip
     * and 2-draw/copy operations for shape clip compared to 3-draws/copy
     * operations deployed by the remaining approaches.
     *
     * complexClipBlit method helps to convert or copy the contents from
     * D3DSurface onto Sysmem and perform a regular Blit with the clip
     * information as required. This method is used when non-rectangular
     * clip is needed.
     */
    private synchronized void complexClipBlit(SurfaceData src, SurfaceData dst,
                                              Composite comp, Region clip,
                                              int sx, int sy, int dx, int dy,
                                              int w, int h) {
        SurfaceData cachedSrc = null;
        if (srcTmp != null) {
            // use cached intermediate surface, if available
            cachedSrc = srcTmp.get();
        }

        // Type- indicates the pixel format of Sysmem based BufferedImage.
        // Native d3d interfaces support on the fly conversion of pixels from
        // d3d surface to destination sysmem memory of type IntARGB only.
        final int type = BufferedImage.TYPE_INT_ARGB;
        src = convertFrom(this, src, sx, sy, w, h, cachedSrc, type);

        // copy intermediate SW to destination SW using complex clip
        final Blit performop = Blit.getFromCache(src.getSurfaceType(),
                                                 CompositeType.SrcNoEa,
                                                 dst.getSurfaceType());
        performop.Blit(src, dst, comp, clip, 0, 0, dx, dy, w, h);

        if (src != cachedSrc) {
            // cache the intermediate surface
            srcTmp = new WeakReference<>(src);
        }
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy,
                     int w, int h)
    {
        if (clip != null) {
            clip = clip.getIntersectionXYWH(dx, dy, w, h);
            // At the end this method will flush the RenderQueue, we should exit
            // from it as soon as possible.
            if (clip.isEmpty()) {
                return;
            }

            // Adjust final dst(x,y) and src(x,y) based on the clip. The
            // logic is that, when clip limits drawing on the destination,
            // corresponding pixels from the src should be skipped.
            sx += clip.getLoX() - dx;
            sy += clip.getLoY() - dy;
            dx = clip.getLoX();
            dy = clip.getLoY();
            w = clip.getWidth();
            h = clip.getHeight();

            // Check if the clip is Rectangular. For non-rectangular clips
            // complexClipBlit will convert Surface To Sysmem and perform
            // regular Blit.
            if (!clip.isRectangular()) {
                complexClipBlit(src, dst, comp, clip,
                                sx, sy, dx, dy,
                                w, h);
                return;
            }
        }

        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        rq.lock();
        try {
            // make sure the RenderQueue keeps a hard reference to the
            // destination (sysmem) SurfaceData to prevent it from being
            // disposed while the operation is processed on the QFT
            rq.addReference(dst);

            RenderBuffer buf = rq.getBuffer();
            D3DContext.setScratchSurface(((D3DSurfaceData)src).getContext());

            rq.ensureCapacityAndAlignment(48, 32);
            buf.putInt(SURFACE_TO_SW_BLIT);
            buf.putInt(sx).putInt(sy);
            buf.putInt(dx).putInt(dy);
            buf.putInt(w).putInt(h);
            buf.putInt(typeval);
            buf.putLong(src.getNativeOps());
            buf.putLong(dst.getNativeOps());

            // always flush immediately
            rq.flushNow();
        } finally {
            rq.unlock();
        }
    }
}

class D3DSwToSurfaceBlit extends Blit {

    private int typeval;

    D3DSwToSurfaceBlit(SurfaceType srcType, int typeval) {
        super(srcType,
              CompositeType.AnyAlpha,
              D3DSurfaceData.D3DSurface);
        this.typeval = typeval;
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy, int w, int h)
    {
        D3DBlitLoops.Blit(src, dst,
                          comp, clip, null,
                          AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                          sx, sy, sx+w, sy+h,
                          dx, dy, dx+w, dy+h,
                          typeval, false);
    }
}

class D3DSwToSurfaceScale extends ScaledBlit {

    private int typeval;

    D3DSwToSurfaceScale(SurfaceType srcType, int typeval) {
        super(srcType,
              CompositeType.AnyAlpha,
              D3DSurfaceData.D3DSurface);
        this.typeval = typeval;
    }

    public void Scale(SurfaceData src, SurfaceData dst,
                      Composite comp, Region clip,
                      int sx1, int sy1,
                      int sx2, int sy2,
                      double dx1, double dy1,
                      double dx2, double dy2)
    {
        D3DBlitLoops.Blit(src, dst,
                          comp, clip, null,
                          AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                          sx1, sy1, sx2, sy2,
                          dx1, dy1, dx2, dy2,
                          typeval, false);
    }
}

class D3DSwToSurfaceTransform extends TransformBlit {

    private int typeval;

    D3DSwToSurfaceTransform(SurfaceType srcType, int typeval) {
        super(srcType,
              CompositeType.AnyAlpha,
              D3DSurfaceData.D3DSurface);
        this.typeval = typeval;
    }

    public void Transform(SurfaceData src, SurfaceData dst,
                          Composite comp, Region clip,
                          AffineTransform at, int hint,
                          int sx, int sy, int dx, int dy, int w, int h)
    {
        D3DBlitLoops.Blit(src, dst,
                          comp, clip, at, hint,
                          sx, sy, sx+w, sy+h,
                          dx, dy, dx+w, dy+h,
                          typeval, false);
    }
}

class D3DSwToTextureBlit extends Blit {

    private int typeval;

    D3DSwToTextureBlit(SurfaceType srcType, int typeval) {
        super(srcType,
              CompositeType.SrcNoEa,
              D3DSurfaceData.D3DTexture);
        this.typeval = typeval;
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy, int w, int h)
    {
        D3DBlitLoops.Blit(src, dst,
                          comp, clip, null,
                          AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                          sx, sy, sx+w, sy+h,
                          dx, dy, dx+w, dy+h,
                          typeval, true);
    }
}

class D3DTextureToSurfaceBlit extends Blit {

    D3DTextureToSurfaceBlit() {
        super(D3DSurfaceData.D3DTexture,
              CompositeType.AnyAlpha,
              D3DSurfaceData.D3DSurface);
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy, int w, int h)
    {
        D3DBlitLoops.IsoBlit(src, dst,
                             null, null,
                             comp, clip, null,
                             AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                             sx, sy, sx+w, sy+h,
                             dx, dy, dx+w, dy+h,
                             true);
    }
}

class D3DTextureToSurfaceScale extends ScaledBlit {

    D3DTextureToSurfaceScale() {
        super(D3DSurfaceData.D3DTexture,
              CompositeType.AnyAlpha,
              D3DSurfaceData.D3DSurface);
    }

    public void Scale(SurfaceData src, SurfaceData dst,
                      Composite comp, Region clip,
                      int sx1, int sy1,
                      int sx2, int sy2,
                      double dx1, double dy1,
                      double dx2, double dy2)
    {
        D3DBlitLoops.IsoBlit(src, dst,
                             null, null,
                             comp, clip, null,
                             AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                             sx1, sy1, sx2, sy2,
                             dx1, dy1, dx2, dy2,
                             true);
    }
}

class D3DTextureToSurfaceTransform extends TransformBlit {

    D3DTextureToSurfaceTransform() {
        super(D3DSurfaceData.D3DTexture,
              CompositeType.AnyAlpha,
              D3DSurfaceData.D3DSurface);
    }

    public void Transform(SurfaceData src, SurfaceData dst,
                          Composite comp, Region clip,
                          AffineTransform at, int hint,
                          int sx, int sy, int dx, int dy,
                          int w, int h)
    {
        D3DBlitLoops.IsoBlit(src, dst,
                             null, null,
                             comp, clip, at, hint,
                             sx, sy, sx+w, sy+h,
                             dx, dy, dx+w, dy+h,
                             true);
    }
}

/**
 * This general Blit implementation converts any source surface to an
 * intermediate IntArgbPre surface, and then uses the more specific
 * IntArgbPre->D3DSurface/Texture loop to get the intermediate
 * (premultiplied) surface down to D3D using simple blit.
 */
class D3DGeneralBlit extends Blit {

    private final Blit performop;
    private WeakReference<SurfaceData> srcTmp;

    D3DGeneralBlit(SurfaceType dstType,
                   CompositeType compType,
                   Blit performop)
    {
        super(SurfaceType.Any, compType, dstType);
        this.performop = performop;
    }

    public synchronized void Blit(SurfaceData src, SurfaceData dst,
                                  Composite comp, Region clip,
                                  int sx, int sy, int dx, int dy,
                                  int w, int h)
    {
        Blit convertsrc = Blit.getFromCache(src.getSurfaceType(),
                                            CompositeType.SrcNoEa,
                                            SurfaceType.IntArgbPre);

        SurfaceData cachedSrc = null;
        if (srcTmp != null) {
            // use cached intermediate surface, if available
            cachedSrc = srcTmp.get();
        }

        // convert source to IntArgbPre
        src = convertFrom(convertsrc, src, sx, sy, w, h,
                          cachedSrc, BufferedImage.TYPE_INT_ARGB_PRE);

        // copy IntArgbPre intermediate surface to D3D surface
        performop.Blit(src, dst, comp, clip,
                       0, 0, dx, dy, w, h);

        if (src != cachedSrc) {
            // cache the intermediate surface
            srcTmp = new WeakReference<>(src);
        }
    }
}

/**
 * This general TransformedBlit implementation converts any source surface to an
 * intermediate IntArgbPre surface, and then uses the more specific
 * IntArgbPre->D3DSurface/Texture loop to get the intermediate
 * (premultiplied) surface down to D3D using simple transformBlit.
 */
final class D3DGeneralTransformedBlit extends TransformBlit {

    private final TransformBlit performop;
    private WeakReference<SurfaceData> srcTmp;

    D3DGeneralTransformedBlit(final TransformBlit performop) {
        super(SurfaceType.Any, CompositeType.AnyAlpha,
                D3DSurfaceData.D3DSurface);
        this.performop = performop;
    }

    @Override
    public synchronized void Transform(SurfaceData src, SurfaceData dst,
                                       Composite comp, Region clip,
                                       AffineTransform at, int hint, int srcx,
                                       int srcy, int dstx, int dsty, int width,
                                       int height){
        Blit convertsrc = Blit.getFromCache(src.getSurfaceType(),
                                            CompositeType.SrcNoEa,
                                            SurfaceType.IntArgbPre);
        // use cached intermediate surface, if available
        final SurfaceData cachedSrc = srcTmp != null ? srcTmp.get() : null;
        // convert source to IntArgbPre
        src = convertFrom(convertsrc, src, srcx, srcy, width, height, cachedSrc,
                          BufferedImage.TYPE_INT_ARGB_PRE);

        // transform IntArgbPre intermediate surface to D3D surface
        performop.Transform(src, dst, comp, clip, at, hint, 0, 0, dstx, dsty,
                            width, height);

        if (src != cachedSrc) {
            // cache the intermediate surface
            srcTmp = new WeakReference<>(src);
        }
    }
}

/*
 * The following classes prohibit copying D3DSurfaces to the screen
 * (the D3D->sysmem->GDI path is known to be very very slow).
 *
 * Note: we used to disable hw acceleration for the surafce manager associated
 * with the source surface in these loops but it proved to be too cautious.
 *
 * In most cases d3d->screen copy happens only during some transitional
 * period where the accelerated destination surface is being recreated or
 * restored (for example, when Swing's backbuffer VI is copied to the screen
 * but the D3DScreenSurfaceManager couldn't restore its surface).
 *
 * An exception is if for some reason we could not enable accelerated on-screen
 * rendering for this window for some permanent reason (like window being too
 * small, or a present BufferStrategy).
 *
 * This meant that we'd disable hw acceleration after the first failure
 * completely (at least until the src image is recreated which in case of
 * Swing back-buffer happens only after resize).
 *
 * Now we delegate to the VISM to figure out if the acceleration needs to
 * be disabled or if we can wait for a while until the onscreen accelerated
 * can resume (by marking the source surface lost and making sure the
 * VISM has a chance to use the backup surface).
 *
 */

class D3DSurfaceToGDIWindowSurfaceBlit extends Blit {

    D3DSurfaceToGDIWindowSurfaceBlit() {
        super(D3DSurfaceData.D3DSurface,
              CompositeType.AnyAlpha,
              GDIWindowSurfaceData.AnyGdi);
    }
    @Override
    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy, int w, int h)
    {
        // see comment above
        D3DVolatileSurfaceManager.handleVItoScreenOp(src, dst);
    }

}

class D3DSurfaceToGDIWindowSurfaceScale extends ScaledBlit {

    D3DSurfaceToGDIWindowSurfaceScale() {
        super(D3DSurfaceData.D3DSurface,
              CompositeType.AnyAlpha,
              GDIWindowSurfaceData.AnyGdi);
    }
    @Override
    public void Scale(SurfaceData src, SurfaceData dst,
                      Composite comp, Region clip,
                      int sx1, int sy1,
                      int sx2, int sy2,
                      double dx1, double dy1,
                      double dx2, double dy2)
    {
        // see comment above
        D3DVolatileSurfaceManager.handleVItoScreenOp(src, dst);
    }
}

class D3DSurfaceToGDIWindowSurfaceTransform extends TransformBlit {

    D3DSurfaceToGDIWindowSurfaceTransform() {
        super(D3DSurfaceData.D3DSurface,
              CompositeType.AnyAlpha,
              GDIWindowSurfaceData.AnyGdi);
    }
    @Override
    public void Transform(SurfaceData src, SurfaceData dst,
                          Composite comp, Region clip,
                          AffineTransform at, int hint,
                          int sx, int sy, int dx, int dy,
                          int w, int h)
    {
        // see comment above
        D3DVolatileSurfaceManager.handleVItoScreenOp(src, dst);
    }
}
