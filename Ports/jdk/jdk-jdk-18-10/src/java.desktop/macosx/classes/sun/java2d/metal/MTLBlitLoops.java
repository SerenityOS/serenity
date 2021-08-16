/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.metal;

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
import sun.java2d.pipe.hw.AccelSurface;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.lang.annotation.Native;
import java.lang.ref.WeakReference;

import static sun.java2d.pipe.BufferedOpCodes.BLIT;
import static sun.java2d.pipe.BufferedOpCodes.SURFACE_TO_SW_BLIT;

final class MTLBlitLoops {

    static void register() {
        Blit blitIntArgbPreToSurface =
                new MTLSwToSurfaceBlit(SurfaceType.IntArgbPre,
                        MTLSurfaceData.PF_INT_ARGB_PRE);
        Blit blitIntArgbPreToTexture =
                new MTLSwToTextureBlit(SurfaceType.IntArgbPre,
                        MTLSurfaceData.PF_INT_ARGB_PRE);
        TransformBlit transformBlitIntArgbPreToSurface =
                new MTLSwToSurfaceTransform(SurfaceType.IntArgbPre,
                        MTLSurfaceData.PF_INT_ARGB_PRE);
        MTLSurfaceToSwBlit blitSurfaceToIntArgbPre =
                new MTLSurfaceToSwBlit(SurfaceType.IntArgbPre,
                        MTLSurfaceData.PF_INT_ARGB_PRE);

        GraphicsPrimitive[] primitives = {
                // surface->surface ops
                new MTLSurfaceToSurfaceBlit(),
                new MTLSurfaceToSurfaceScale(),
                new MTLSurfaceToSurfaceTransform(),

                // render-to-texture surface->surface ops
                new MTLRTTSurfaceToSurfaceBlit(),
                new MTLRTTSurfaceToSurfaceScale(),
                new MTLRTTSurfaceToSurfaceTransform(),

                // surface->sw ops
                new MTLSurfaceToSwBlit(SurfaceType.IntArgb,
                        MTLSurfaceData.PF_INT_ARGB),
                blitSurfaceToIntArgbPre,

                // sw->surface ops
                blitIntArgbPreToSurface,
                new MTLSwToSurfaceBlit(SurfaceType.IntRgb,
                        MTLSurfaceData.PF_INT_RGB),
                new MTLSwToSurfaceBlit(SurfaceType.IntRgbx,
                        MTLSurfaceData.PF_INT_RGBX),
                new MTLSwToSurfaceBlit(SurfaceType.IntBgr,
                        MTLSurfaceData.PF_INT_BGR),
                new MTLSwToSurfaceBlit(SurfaceType.IntBgrx,
                        MTLSurfaceData.PF_INT_BGRX),
                new MTLGeneralBlit(MTLSurfaceData.MTLSurface,
                        CompositeType.AnyAlpha,
                        blitIntArgbPreToSurface),

                new MTLAnyCompositeBlit(MTLSurfaceData.MTLSurface,
                        blitSurfaceToIntArgbPre,
                        blitSurfaceToIntArgbPre,
                        blitIntArgbPreToSurface),
                new MTLAnyCompositeBlit(SurfaceType.Any,
                        null,
                        blitSurfaceToIntArgbPre,
                        blitIntArgbPreToSurface),

                new MTLSwToSurfaceScale(SurfaceType.IntRgb,
                        MTLSurfaceData.PF_INT_RGB),
                new MTLSwToSurfaceScale(SurfaceType.IntRgbx,
                        MTLSurfaceData.PF_INT_RGBX),
                new MTLSwToSurfaceScale(SurfaceType.IntBgr,
                        MTLSurfaceData.PF_INT_BGR),
                new MTLSwToSurfaceScale(SurfaceType.IntBgrx,
                        MTLSurfaceData.PF_INT_BGRX),
                new MTLSwToSurfaceScale(SurfaceType.IntArgbPre,
                        MTLSurfaceData.PF_INT_ARGB_PRE),

                new MTLSwToSurfaceTransform(SurfaceType.IntRgb,
                        MTLSurfaceData.PF_INT_RGB),
                new MTLSwToSurfaceTransform(SurfaceType.IntRgbx,
                        MTLSurfaceData.PF_INT_RGBX),
                new MTLSwToSurfaceTransform(SurfaceType.IntBgr,
                        MTLSurfaceData.PF_INT_BGR),
                new MTLSwToSurfaceTransform(SurfaceType.IntBgrx,
                        MTLSurfaceData.PF_INT_BGRX),
                transformBlitIntArgbPreToSurface,

                new MTLGeneralTransformedBlit(transformBlitIntArgbPreToSurface),

                // texture->surface ops
                new MTLTextureToSurfaceBlit(),
                new MTLTextureToSurfaceScale(),
                new MTLTextureToSurfaceTransform(),

                // sw->texture ops
                blitIntArgbPreToTexture,
                new MTLSwToTextureBlit(SurfaceType.IntRgb,
                        MTLSurfaceData.PF_INT_RGB),
                new MTLSwToTextureBlit(SurfaceType.IntRgbx,
                        MTLSurfaceData.PF_INT_RGBX),
                new MTLSwToTextureBlit(SurfaceType.IntBgr,
                        MTLSurfaceData.PF_INT_BGR),
                new MTLSwToTextureBlit(SurfaceType.IntBgrx,
                        MTLSurfaceData.PF_INT_BGRX),
                new MTLGeneralBlit(MTLSurfaceData.MTLTexture,
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
            ctxflags |= MTLContext.SRC_IS_OPAQUE;
        }

        MTLRenderQueue rq = MTLRenderQueue.getInstance();
        rq.lock();
        try {
            // make sure the RenderQueue keeps a hard reference to the
            // source (sysmem) SurfaceData to prevent it from being
            // disposed while the operation is processed on the QFT
            rq.addReference(srcData);

            MTLSurfaceData mtlDst = (MTLSurfaceData)dstData;
            if (texture) {
                // make sure we have a current context before uploading
                // the sysmem data to the texture object
                MTLGraphicsConfig gc = mtlDst.getMTLGraphicsConfig();
                MTLContext.setScratchSurface(gc);
            } else {
                MTLContext.validateContext(mtlDst, mtlDst,
                        clip, comp, xform, null, null,
                        ctxflags);
            }

            int packedParams = createPackedParams(false, texture,
                    false /*unused*/, xform != null,
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
    }

    /**
     * Note: The srcImg and biop parameters are only used when invoked
     * from the MTLBufImgOps.renderImageWithOp() method; in all other cases,
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
            ctxflags |= MTLContext.SRC_IS_OPAQUE;
        }

        MTLRenderQueue rq = MTLRenderQueue.getInstance();
        rq.lock();
        try {
            MTLSurfaceData mtlSrc = (MTLSurfaceData)srcData;
            MTLSurfaceData mtlDst = (MTLSurfaceData)dstData;
            int srctype = mtlSrc.getType();
            boolean rtt;
            MTLSurfaceData srcCtxData;
            if (srctype == MTLSurfaceData.TEXTURE) {
                // the source is a regular texture object; we substitute
                // the destination surface for the purposes of making a
                // context current
                rtt = false;
                srcCtxData = mtlDst;
            } else {
                // the source is a pbuffer, backbuffer, or render-to-texture
                // surface; we set rtt to true to differentiate this kind
                // of surface from a regular texture object
                rtt = true;
                if (srctype == AccelSurface.RT_TEXTURE) {
                    srcCtxData = mtlDst;
                } else {
                    srcCtxData = mtlSrc;
                }
            }

            MTLContext.validateContext(srcCtxData, mtlDst,
                    clip, comp, xform, null, null,
                    ctxflags);

            if (biop != null) {
                MTLBufImgOps.enableBufImgOp(rq, mtlSrc, srcImg, biop);
            }

            int packedParams = createPackedParams(true, texture,
                    false /*unused*/, xform != null,
                    hint, 0 /*unused*/);
            enqueueBlit(rq, srcData, dstData,
                    packedParams,
                    sx1, sy1, sx2, sy2,
                    dx1, dy1, dx2, dy2);

            if (biop != null) {
                MTLBufImgOps.disableBufImgOp(rq, biop);
            }

            if (rtt && mtlDst.isOnScreen()) {
                // we only have to flush immediately when copying from a
                // (non-texture) surface to the screen; otherwise Swing apps
                // might appear unresponsive until the auto-flush completes
                rq.flushNow();
            }
        } finally {
            rq.unlock();
        }
    }
}

class MTLSurfaceToSurfaceBlit extends Blit {

    MTLSurfaceToSurfaceBlit() {
        super(MTLSurfaceData.MTLSurface,
                CompositeType.AnyAlpha,
                MTLSurfaceData.MTLSurface);
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy, int w, int h)
    {
        MTLBlitLoops.IsoBlit(src, dst,
                null, null,
                comp, clip, null,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                sx, sy, sx+w, sy+h,
                dx, dy, dx+w, dy+h,
                false);
    }
}

class MTLSurfaceToSurfaceScale extends ScaledBlit {

    MTLSurfaceToSurfaceScale() {
        super(MTLSurfaceData.MTLSurface,
                CompositeType.AnyAlpha,
                MTLSurfaceData.MTLSurface);
    }

    public void Scale(SurfaceData src, SurfaceData dst,
                      Composite comp, Region clip,
                      int sx1, int sy1,
                      int sx2, int sy2,
                      double dx1, double dy1,
                      double dx2, double dy2)
    {
        MTLBlitLoops.IsoBlit(src, dst,
                null, null,
                comp, clip, null,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                sx1, sy1, sx2, sy2,
                dx1, dy1, dx2, dy2,
                false);
    }
}

class MTLSurfaceToSurfaceTransform extends TransformBlit {

    MTLSurfaceToSurfaceTransform() {
        super(MTLSurfaceData.MTLSurface,
                CompositeType.AnyAlpha,
                MTLSurfaceData.MTLSurface);
    }

    public void Transform(SurfaceData src, SurfaceData dst,
                          Composite comp, Region clip,
                          AffineTransform at, int hint,
                          int sx, int sy, int dx, int dy,
                          int w, int h)
    {
        MTLBlitLoops.IsoBlit(src, dst,
                null, null,
                comp, clip, at, hint,
                sx, sy, sx+w, sy+h,
                dx, dy, dx+w, dy+h,
                false);
    }
}

class MTLRTTSurfaceToSurfaceBlit extends Blit {

    MTLRTTSurfaceToSurfaceBlit() {
        super(MTLSurfaceData.MTLSurfaceRTT,
                CompositeType.AnyAlpha,
                MTLSurfaceData.MTLSurface);
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy, int w, int h)
    {
        MTLBlitLoops.IsoBlit(src, dst,
                null, null,
                comp, clip, null,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                sx, sy, sx+w, sy+h,
                dx, dy, dx+w, dy+h,
                true);
    }
}

class MTLRTTSurfaceToSurfaceScale extends ScaledBlit {

    MTLRTTSurfaceToSurfaceScale() {
        super(MTLSurfaceData.MTLSurfaceRTT,
                CompositeType.AnyAlpha,
                MTLSurfaceData.MTLSurface);
    }

    public void Scale(SurfaceData src, SurfaceData dst,
                      Composite comp, Region clip,
                      int sx1, int sy1,
                      int sx2, int sy2,
                      double dx1, double dy1,
                      double dx2, double dy2)
    {
        MTLBlitLoops.IsoBlit(src, dst,
                null, null,
                comp, clip, null,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                sx1, sy1, sx2, sy2,
                dx1, dy1, dx2, dy2,
                true);
    }
}

class MTLRTTSurfaceToSurfaceTransform extends TransformBlit {

    MTLRTTSurfaceToSurfaceTransform() {
        super(MTLSurfaceData.MTLSurfaceRTT,
                CompositeType.AnyAlpha,
                MTLSurfaceData.MTLSurface);
    }

    public void Transform(SurfaceData src, SurfaceData dst,
                          Composite comp, Region clip,
                          AffineTransform at, int hint,
                          int sx, int sy, int dx, int dy, int w, int h)
    {
        MTLBlitLoops.IsoBlit(src, dst,
                null, null,
                comp, clip, at, hint,
                sx, sy, sx+w, sy+h,
                dx, dy, dx+w, dy+h,
                true);
    }
}

final class MTLSurfaceToSwBlit extends Blit {

    private final int typeval;
    private WeakReference<SurfaceData> srcTmp;

    // destination will actually be ArgbPre or Argb
    MTLSurfaceToSwBlit(final SurfaceType dstType, final int typeval) {
        super(MTLSurfaceData.MTLSurface,
                CompositeType.SrcNoEa,
                dstType);
        this.typeval = typeval;
    }

    private synchronized void complexClipBlit(SurfaceData src, SurfaceData dst,
                                              Composite comp, Region clip,
                                              int sx, int sy, int dx, int dy,
                                              int w, int h) {
        SurfaceData cachedSrc = null;
        if (srcTmp != null) {
            // use cached intermediate surface, if available
            cachedSrc = srcTmp.get();
        }

        // We can convert argb_pre data from MTL surface in two places:
        // - During MTL surface -> SW blit
        // - During SW -> SW blit
        // The first one is faster when we use opaque MTL surface, because in
        // this case we simply skip conversion and use color components as is.
        // Because of this we align intermediate buffer type with type of
        // destination not source.
        final int type = typeval == MTLSurfaceData.PF_INT_ARGB_PRE ?
                BufferedImage.TYPE_INT_ARGB_PRE :
                BufferedImage.TYPE_INT_ARGB;

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
            sx += clip.getLoX() - dx;
            sy += clip.getLoY() - dy;
            dx = clip.getLoX();
            dy = clip.getLoY();
            w = clip.getWidth();
            h = clip.getHeight();

            if (!clip.isRectangular()) {
                complexClipBlit(src, dst, comp, clip, sx, sy, dx, dy, w, h);
                return;
            }
        }

        MTLRenderQueue rq = MTLRenderQueue.getInstance();
        rq.lock();
        try {
            // make sure the RenderQueue keeps a hard reference to the
            // destination (sysmem) SurfaceData to prevent it from being
            // disposed while the operation is processed on the QFT
            rq.addReference(dst);

            RenderBuffer buf = rq.getBuffer();
            MTLContext.validateContext((MTLSurfaceData)src);

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

class MTLSwToSurfaceBlit extends Blit {

    private int typeval;

    MTLSwToSurfaceBlit(SurfaceType srcType, int typeval) {
        super(srcType,
                CompositeType.AnyAlpha,
                MTLSurfaceData.MTLSurface);
        this.typeval = typeval;
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy, int w, int h)
    {
        MTLBlitLoops.Blit(src, dst,
                comp, clip, null,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                sx, sy, sx+w, sy+h,
                dx, dy, dx+w, dy+h,
                typeval, false);
    }
}

class MTLSwToSurfaceScale extends ScaledBlit {

    private int typeval;

    MTLSwToSurfaceScale(SurfaceType srcType, int typeval) {
        super(srcType,
                CompositeType.AnyAlpha,
                MTLSurfaceData.MTLSurface);
        this.typeval = typeval;
    }

    public void Scale(SurfaceData src, SurfaceData dst,
                      Composite comp, Region clip,
                      int sx1, int sy1,
                      int sx2, int sy2,
                      double dx1, double dy1,
                      double dx2, double dy2)
    {
        MTLBlitLoops.Blit(src, dst,
                comp, clip, null,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                sx1, sy1, sx2, sy2,
                dx1, dy1, dx2, dy2,
                typeval, false);
    }
}

class MTLSwToSurfaceTransform extends TransformBlit {

    private int typeval;

    MTLSwToSurfaceTransform(SurfaceType srcType, int typeval) {
        super(srcType,
                CompositeType.AnyAlpha,
                MTLSurfaceData.MTLSurface);
        this.typeval = typeval;
    }

    public void Transform(SurfaceData src, SurfaceData dst,
                          Composite comp, Region clip,
                          AffineTransform at, int hint,
                          int sx, int sy, int dx, int dy, int w, int h)
    {
        MTLBlitLoops.Blit(src, dst,
                comp, clip, at, hint,
                sx, sy, sx+w, sy+h,
                dx, dy, dx+w, dy+h,
                typeval, false);
    }
}

class MTLSwToTextureBlit extends Blit {

    private int typeval;

    MTLSwToTextureBlit(SurfaceType srcType, int typeval) {
        super(srcType,
                CompositeType.SrcNoEa,
                MTLSurfaceData.MTLTexture);
        this.typeval = typeval;
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy, int w, int h)
    {
        MTLBlitLoops.Blit(src, dst,
                comp, clip, null,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                sx, sy, sx+w, sy+h,
                dx, dy, dx+w, dy+h,
                typeval, true);
    }
}

class MTLTextureToSurfaceBlit extends Blit {

    MTLTextureToSurfaceBlit() {
        super(MTLSurfaceData.MTLTexture,
                CompositeType.AnyAlpha,
                MTLSurfaceData.MTLSurface);
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy, int w, int h)
    {
        MTLBlitLoops.IsoBlit(src, dst,
                null, null,
                comp, clip, null,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                sx, sy, sx+w, sy+h,
                dx, dy, dx+w, dy+h,
                true);
    }
}

class MTLTextureToSurfaceScale extends ScaledBlit {

    MTLTextureToSurfaceScale() {
        super(MTLSurfaceData.MTLTexture,
                CompositeType.AnyAlpha,
                MTLSurfaceData.MTLSurface);
    }

    public void Scale(SurfaceData src, SurfaceData dst,
                      Composite comp, Region clip,
                      int sx1, int sy1,
                      int sx2, int sy2,
                      double dx1, double dy1,
                      double dx2, double dy2)
    {
        MTLBlitLoops.IsoBlit(src, dst,
                null, null,
                comp, clip, null,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                sx1, sy1, sx2, sy2,
                dx1, dy1, dx2, dy2,
                true);
    }
}

class MTLTextureToSurfaceTransform extends TransformBlit {

    MTLTextureToSurfaceTransform() {
        super(MTLSurfaceData.MTLTexture,
                CompositeType.AnyAlpha,
                MTLSurfaceData.MTLSurface);
    }

    public void Transform(SurfaceData src, SurfaceData dst,
                          Composite comp, Region clip,
                          AffineTransform at, int hint,
                          int sx, int sy, int dx, int dy,
                          int w, int h)
    {
        MTLBlitLoops.IsoBlit(src, dst,
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
 * IntArgbPre->MTLSurface/Texture loop to get the intermediate
 * (premultiplied) surface down to Metal using simple blit.
 */
class MTLGeneralBlit extends Blit {

    private final Blit performop;
    private WeakReference<SurfaceData> srcTmp;

    MTLGeneralBlit(SurfaceType dstType,
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

        // copy IntArgbPre intermediate surface to Metal surface
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
 * IntArgbPre->MTLSurface/Texture loop to get the intermediate
 * (premultiplied) surface down to Metal using simple transformBlit.
 */
final class MTLGeneralTransformedBlit extends TransformBlit {

    private final TransformBlit performop;
    private WeakReference<SurfaceData> srcTmp;

    MTLGeneralTransformedBlit(final TransformBlit performop) {
        super(SurfaceType.Any, CompositeType.AnyAlpha,
                MTLSurfaceData.MTLSurface);
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

        // transform IntArgbPre intermediate surface to Metal surface
        performop.Transform(src, dst, comp, clip, at, hint, 0, 0, dstx, dsty,
                width, height);

        if (src != cachedSrc) {
            // cache the intermediate surface
            srcTmp = new WeakReference<>(src);
        }
    }
}

/**
 * This general MTLAnyCompositeBlit implementation can convert any source/target
 * surface to an intermediate surface using convertsrc/convertdst loops, applies
 * necessary composite operation, and then uses convertresult loop to get the
 * intermediate surface down to Metal.
 */
final class MTLAnyCompositeBlit extends Blit {

    private WeakReference<SurfaceData> dstTmp;
    private WeakReference<SurfaceData> srcTmp;
    private final Blit convertsrc;
    private final Blit convertdst;
    private final Blit convertresult;

    MTLAnyCompositeBlit(SurfaceType srctype, Blit convertsrc, Blit convertdst,
                        Blit convertresult) {
        super(srctype, CompositeType.Any, MTLSurfaceData.MTLSurface);
        this.convertsrc = convertsrc;
        this.convertdst = convertdst;
        this.convertresult = convertresult;
    }

    public synchronized void Blit(SurfaceData src, SurfaceData dst,
                                  Composite comp, Region clip,
                                  int sx, int sy, int dx, int dy,
                                  int w, int h)
    {
        if (convertsrc != null) {
            SurfaceData cachedSrc = null;
            if (srcTmp != null) {
                // use cached intermediate surface, if available
                cachedSrc = srcTmp.get();
            }
            // convert source to IntArgbPre
            src = convertFrom(convertsrc, src, sx, sy, w, h, cachedSrc,
                    BufferedImage.TYPE_INT_ARGB_PRE);
            if (src != cachedSrc) {
                // cache the intermediate surface
                srcTmp = new WeakReference<>(src);
            }
        }

        SurfaceData cachedDst = null;

        if (dstTmp != null) {
            // use cached intermediate surface, if available
            cachedDst = dstTmp.get();
        }

        // convert destination to IntArgbPre
        SurfaceData dstBuffer = convertFrom(convertdst, dst, dx, dy, w, h,
                cachedDst, BufferedImage.TYPE_INT_ARGB_PRE);
        Region bufferClip =
                clip == null ? null : clip.getTranslatedRegion(-dx, -dy);

        Blit performop = Blit.getFromCache(src.getSurfaceType(),
                CompositeType.Any, dstBuffer.getSurfaceType());
        performop.Blit(src, dstBuffer, comp, bufferClip, sx, sy, 0, 0, w, h);

        if (dstBuffer != cachedDst) {
            // cache the intermediate surface
            dstTmp = new WeakReference<>(dstBuffer);
        }
        // now blit the buffer back to the destination
        convertresult.Blit(dstBuffer, dst, AlphaComposite.Src, clip, 0, 0, dx,
                dy, w, h);
    }
}
