/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.lang.ref.WeakReference;
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
import java.lang.annotation.Native;

final class OGLBlitLoops {

    static void register() {
        Blit blitIntArgbPreToSurface =
            new OGLSwToSurfaceBlit(SurfaceType.IntArgbPre,
                                   OGLSurfaceData.PF_INT_ARGB_PRE);
        Blit blitIntArgbPreToTexture =
            new OGLSwToTextureBlit(SurfaceType.IntArgbPre,
                                   OGLSurfaceData.PF_INT_ARGB_PRE);
        TransformBlit transformBlitIntArgbPreToSurface =
            new OGLSwToSurfaceTransform(SurfaceType.IntArgbPre,
                                        OGLSurfaceData.PF_INT_ARGB_PRE);
        OGLSurfaceToSwBlit blitSurfaceToIntArgbPre =
            new OGLSurfaceToSwBlit(SurfaceType.IntArgbPre,
                                   OGLSurfaceData.PF_INT_ARGB_PRE);

        GraphicsPrimitive[] primitives = {
            // surface->surface ops
            new OGLSurfaceToSurfaceBlit(),
            new OGLSurfaceToSurfaceScale(),
            new OGLSurfaceToSurfaceTransform(),

            // render-to-texture surface->surface ops
            new OGLRTTSurfaceToSurfaceBlit(),
            new OGLRTTSurfaceToSurfaceScale(),
            new OGLRTTSurfaceToSurfaceTransform(),

            // surface->sw ops
            new OGLSurfaceToSwBlit(SurfaceType.IntArgb,
                                   OGLSurfaceData.PF_INT_ARGB),
            blitSurfaceToIntArgbPre,

            // sw->surface ops
            blitIntArgbPreToSurface,
            new OGLSwToSurfaceBlit(SurfaceType.IntRgb,
                                   OGLSurfaceData.PF_INT_RGB),
            new OGLSwToSurfaceBlit(SurfaceType.IntRgbx,
                                   OGLSurfaceData.PF_INT_RGBX),
            new OGLSwToSurfaceBlit(SurfaceType.IntBgr,
                                   OGLSurfaceData.PF_INT_BGR),
            new OGLSwToSurfaceBlit(SurfaceType.IntBgrx,
                                   OGLSurfaceData.PF_INT_BGRX),
            new OGLSwToSurfaceBlit(SurfaceType.ThreeByteBgr,
                                   OGLSurfaceData.PF_3BYTE_BGR),
            new OGLSwToSurfaceBlit(SurfaceType.Ushort565Rgb,
                                   OGLSurfaceData.PF_USHORT_565_RGB),
            new OGLSwToSurfaceBlit(SurfaceType.Ushort555Rgb,
                                   OGLSurfaceData.PF_USHORT_555_RGB),
            new OGLSwToSurfaceBlit(SurfaceType.Ushort555Rgbx,
                                   OGLSurfaceData.PF_USHORT_555_RGBX),
            new OGLSwToSurfaceBlit(SurfaceType.ByteGray,
                                   OGLSurfaceData.PF_BYTE_GRAY),
            new OGLSwToSurfaceBlit(SurfaceType.UshortGray,
                                   OGLSurfaceData.PF_USHORT_GRAY),
            new OGLGeneralBlit(OGLSurfaceData.OpenGLSurface,
                               CompositeType.AnyAlpha,
                               blitIntArgbPreToSurface),

            new OGLAnyCompositeBlit(OGLSurfaceData.OpenGLSurface,
                                    blitSurfaceToIntArgbPre,
                                    blitSurfaceToIntArgbPre,
                                    blitIntArgbPreToSurface),
            new OGLAnyCompositeBlit(SurfaceType.Any,
                                    null,
                                    blitSurfaceToIntArgbPre,
                                    blitIntArgbPreToSurface),

            new OGLSwToSurfaceScale(SurfaceType.IntRgb,
                                    OGLSurfaceData.PF_INT_RGB),
            new OGLSwToSurfaceScale(SurfaceType.IntRgbx,
                                    OGLSurfaceData.PF_INT_RGBX),
            new OGLSwToSurfaceScale(SurfaceType.IntBgr,
                                    OGLSurfaceData.PF_INT_BGR),
            new OGLSwToSurfaceScale(SurfaceType.IntBgrx,
                                    OGLSurfaceData.PF_INT_BGRX),
            new OGLSwToSurfaceScale(SurfaceType.ThreeByteBgr,
                                    OGLSurfaceData.PF_3BYTE_BGR),
            new OGLSwToSurfaceScale(SurfaceType.Ushort565Rgb,
                                    OGLSurfaceData.PF_USHORT_565_RGB),
            new OGLSwToSurfaceScale(SurfaceType.Ushort555Rgb,
                                    OGLSurfaceData.PF_USHORT_555_RGB),
            new OGLSwToSurfaceScale(SurfaceType.Ushort555Rgbx,
                                    OGLSurfaceData.PF_USHORT_555_RGBX),
            new OGLSwToSurfaceScale(SurfaceType.ByteGray,
                                    OGLSurfaceData.PF_BYTE_GRAY),
            new OGLSwToSurfaceScale(SurfaceType.UshortGray,
                                    OGLSurfaceData.PF_USHORT_GRAY),
            new OGLSwToSurfaceScale(SurfaceType.IntArgbPre,
                                    OGLSurfaceData.PF_INT_ARGB_PRE),

            new OGLSwToSurfaceTransform(SurfaceType.IntRgb,
                                        OGLSurfaceData.PF_INT_RGB),
            new OGLSwToSurfaceTransform(SurfaceType.IntRgbx,
                                        OGLSurfaceData.PF_INT_RGBX),
            new OGLSwToSurfaceTransform(SurfaceType.IntBgr,
                                        OGLSurfaceData.PF_INT_BGR),
            new OGLSwToSurfaceTransform(SurfaceType.IntBgrx,
                                        OGLSurfaceData.PF_INT_BGRX),
            new OGLSwToSurfaceTransform(SurfaceType.ThreeByteBgr,
                                        OGLSurfaceData.PF_3BYTE_BGR),
            new OGLSwToSurfaceTransform(SurfaceType.Ushort565Rgb,
                                        OGLSurfaceData.PF_USHORT_565_RGB),
            new OGLSwToSurfaceTransform(SurfaceType.Ushort555Rgb,
                                        OGLSurfaceData.PF_USHORT_555_RGB),
            new OGLSwToSurfaceTransform(SurfaceType.Ushort555Rgbx,
                                        OGLSurfaceData.PF_USHORT_555_RGBX),
            new OGLSwToSurfaceTransform(SurfaceType.ByteGray,
                                        OGLSurfaceData.PF_BYTE_GRAY),
            new OGLSwToSurfaceTransform(SurfaceType.UshortGray,
                                        OGLSurfaceData.PF_USHORT_GRAY),
            transformBlitIntArgbPreToSurface,

            new OGLGeneralTransformedBlit(transformBlitIntArgbPreToSurface),

            // texture->surface ops
            new OGLTextureToSurfaceBlit(),
            new OGLTextureToSurfaceScale(),
            new OGLTextureToSurfaceTransform(),

            // sw->texture ops
            blitIntArgbPreToTexture,
            new OGLSwToTextureBlit(SurfaceType.IntRgb,
                                   OGLSurfaceData.PF_INT_RGB),
            new OGLSwToTextureBlit(SurfaceType.IntRgbx,
                                   OGLSurfaceData.PF_INT_RGBX),
            new OGLSwToTextureBlit(SurfaceType.IntBgr,
                                   OGLSurfaceData.PF_INT_BGR),
            new OGLSwToTextureBlit(SurfaceType.IntBgrx,
                                   OGLSurfaceData.PF_INT_BGRX),
            new OGLSwToTextureBlit(SurfaceType.ThreeByteBgr,
                                   OGLSurfaceData.PF_3BYTE_BGR),
            new OGLSwToTextureBlit(SurfaceType.Ushort565Rgb,
                                   OGLSurfaceData.PF_USHORT_565_RGB),
            new OGLSwToTextureBlit(SurfaceType.Ushort555Rgb,
                                   OGLSurfaceData.PF_USHORT_555_RGB),
            new OGLSwToTextureBlit(SurfaceType.Ushort555Rgbx,
                                   OGLSurfaceData.PF_USHORT_555_RGBX),
            new OGLSwToTextureBlit(SurfaceType.ByteGray,
                                   OGLSurfaceData.PF_BYTE_GRAY),
            new OGLSwToTextureBlit(SurfaceType.UshortGray,
                                   OGLSurfaceData.PF_USHORT_GRAY),
            new OGLGeneralBlit(OGLSurfaceData.OpenGLTexture,
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
            ctxflags |= OGLContext.SRC_IS_OPAQUE;
        }

        OGLRenderQueue rq = OGLRenderQueue.getInstance();
        rq.lock();
        try {
            // make sure the RenderQueue keeps a hard reference to the
            // source (sysmem) SurfaceData to prevent it from being
            // disposed while the operation is processed on the QFT
            rq.addReference(srcData);

            OGLSurfaceData oglDst = (OGLSurfaceData)dstData;
            if (texture) {
                // make sure we have a current context before uploading
                // the sysmem data to the texture object
                OGLGraphicsConfig gc = oglDst.getOGLGraphicsConfig();
                OGLContext.setScratchSurface(gc);
            } else {
                OGLContext.validateContext(oglDst, oglDst,
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
    }

    /**
     * Note: The srcImg and biop parameters are only used when invoked
     * from the OGLBufImgOps.renderImageWithOp() method; in all other cases,
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
            ctxflags |= OGLContext.SRC_IS_OPAQUE;
        }

        OGLRenderQueue rq = OGLRenderQueue.getInstance();
        rq.lock();
        try {
            OGLSurfaceData oglSrc = (OGLSurfaceData)srcData;
            OGLSurfaceData oglDst = (OGLSurfaceData)dstData;
            int srctype = oglSrc.getType();
            boolean rtt;
            OGLSurfaceData srcCtxData;
            if (srctype == OGLSurfaceData.TEXTURE) {
                // the source is a regular texture object; we substitute
                // the destination surface for the purposes of making a
                // context current
                rtt = false;
                srcCtxData = oglDst;
            } else {
                // the source is a pbuffer, backbuffer, or render-to-texture
                // surface; we set rtt to true to differentiate this kind
                // of surface from a regular texture object
                rtt = true;
                if (srctype == OGLSurfaceData.FBOBJECT) {
                    srcCtxData = oglDst;
                } else {
                    srcCtxData = oglSrc;
                }
            }

            OGLContext.validateContext(srcCtxData, oglDst,
                                       clip, comp, xform, null, null,
                                       ctxflags);

            if (biop != null) {
                OGLBufImgOps.enableBufImgOp(rq, oglSrc, srcImg, biop);
            }

            int packedParams = createPackedParams(true, texture,
                                                  rtt, xform != null,
                                                  hint, 0 /*unused*/);
            enqueueBlit(rq, srcData, dstData,
                        packedParams,
                        sx1, sy1, sx2, sy2,
                        dx1, dy1, dx2, dy2);

            if (biop != null) {
                OGLBufImgOps.disableBufImgOp(rq, biop);
            }

            if (rtt && oglDst.isOnScreen()) {
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

class OGLSurfaceToSurfaceBlit extends Blit {

    OGLSurfaceToSurfaceBlit() {
        super(OGLSurfaceData.OpenGLSurface,
              CompositeType.AnyAlpha,
              OGLSurfaceData.OpenGLSurface);
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy, int w, int h)
    {
        OGLBlitLoops.IsoBlit(src, dst,
                             null, null,
                             comp, clip, null,
                             AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                             sx, sy, sx+w, sy+h,
                             dx, dy, dx+w, dy+h,
                             false);
    }
}

class OGLSurfaceToSurfaceScale extends ScaledBlit {

    OGLSurfaceToSurfaceScale() {
        super(OGLSurfaceData.OpenGLSurface,
              CompositeType.AnyAlpha,
              OGLSurfaceData.OpenGLSurface);
    }

    public void Scale(SurfaceData src, SurfaceData dst,
                      Composite comp, Region clip,
                      int sx1, int sy1,
                      int sx2, int sy2,
                      double dx1, double dy1,
                      double dx2, double dy2)
    {
        OGLBlitLoops.IsoBlit(src, dst,
                             null, null,
                             comp, clip, null,
                             AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                             sx1, sy1, sx2, sy2,
                             dx1, dy1, dx2, dy2,
                             false);
    }
}

class OGLSurfaceToSurfaceTransform extends TransformBlit {

    OGLSurfaceToSurfaceTransform() {
        super(OGLSurfaceData.OpenGLSurface,
              CompositeType.AnyAlpha,
              OGLSurfaceData.OpenGLSurface);
    }

    public void Transform(SurfaceData src, SurfaceData dst,
                          Composite comp, Region clip,
                          AffineTransform at, int hint,
                          int sx, int sy, int dx, int dy,
                          int w, int h)
    {
        OGLBlitLoops.IsoBlit(src, dst,
                             null, null,
                             comp, clip, at, hint,
                             sx, sy, sx+w, sy+h,
                             dx, dy, dx+w, dy+h,
                             false);
    }
}

class OGLRTTSurfaceToSurfaceBlit extends Blit {

    OGLRTTSurfaceToSurfaceBlit() {
        super(OGLSurfaceData.OpenGLSurfaceRTT,
              CompositeType.AnyAlpha,
              OGLSurfaceData.OpenGLSurface);
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy, int w, int h)
    {
        OGLBlitLoops.IsoBlit(src, dst,
                             null, null,
                             comp, clip, null,
                             AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                             sx, sy, sx+w, sy+h,
                             dx, dy, dx+w, dy+h,
                             true);
    }
}

class OGLRTTSurfaceToSurfaceScale extends ScaledBlit {

    OGLRTTSurfaceToSurfaceScale() {
        super(OGLSurfaceData.OpenGLSurfaceRTT,
              CompositeType.AnyAlpha,
              OGLSurfaceData.OpenGLSurface);
    }

    public void Scale(SurfaceData src, SurfaceData dst,
                      Composite comp, Region clip,
                      int sx1, int sy1,
                      int sx2, int sy2,
                      double dx1, double dy1,
                      double dx2, double dy2)
    {
        OGLBlitLoops.IsoBlit(src, dst,
                             null, null,
                             comp, clip, null,
                             AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                             sx1, sy1, sx2, sy2,
                             dx1, dy1, dx2, dy2,
                             true);
    }
}

class OGLRTTSurfaceToSurfaceTransform extends TransformBlit {

    OGLRTTSurfaceToSurfaceTransform() {
        super(OGLSurfaceData.OpenGLSurfaceRTT,
              CompositeType.AnyAlpha,
              OGLSurfaceData.OpenGLSurface);
    }

    public void Transform(SurfaceData src, SurfaceData dst,
                          Composite comp, Region clip,
                          AffineTransform at, int hint,
                          int sx, int sy, int dx, int dy, int w, int h)
    {
        OGLBlitLoops.IsoBlit(src, dst,
                             null, null,
                             comp, clip, at, hint,
                             sx, sy, sx+w, sy+h,
                             dx, dy, dx+w, dy+h,
                             true);
    }
}

final class OGLSurfaceToSwBlit extends Blit {

    private final int typeval;
    private WeakReference<SurfaceData> srcTmp;

    // destination will actually be ArgbPre or Argb
    OGLSurfaceToSwBlit(final SurfaceType dstType,final int typeval) {
        super(OGLSurfaceData.OpenGLSurface,
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

        // We can convert argb_pre data from OpenGL surface in two places:
        // - During OpenGL surface -> SW blit
        // - During SW -> SW blit
        // The first one is faster when we use opaque OGL surface, because in
        // this case we simply skip conversion and use color components as is.
        // Because of this we align intermediate buffer type with type of
        // destination not source.
        final int type = typeval == OGLSurfaceData.PF_INT_ARGB_PRE ?
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

        OGLRenderQueue rq = OGLRenderQueue.getInstance();
        rq.lock();
        try {
            // make sure the RenderQueue keeps a hard reference to the
            // destination (sysmem) SurfaceData to prevent it from being
            // disposed while the operation is processed on the QFT
            rq.addReference(dst);

            RenderBuffer buf = rq.getBuffer();
            OGLContext.validateContext((OGLSurfaceData)src);

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

class OGLSwToSurfaceBlit extends Blit {

    private int typeval;

    OGLSwToSurfaceBlit(SurfaceType srcType, int typeval) {
        super(srcType,
              CompositeType.AnyAlpha,
              OGLSurfaceData.OpenGLSurface);
        this.typeval = typeval;
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy, int w, int h)
    {
        OGLBlitLoops.Blit(src, dst,
                          comp, clip, null,
                          AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                          sx, sy, sx+w, sy+h,
                          dx, dy, dx+w, dy+h,
                          typeval, false);
    }
}

class OGLSwToSurfaceScale extends ScaledBlit {

    private int typeval;

    OGLSwToSurfaceScale(SurfaceType srcType, int typeval) {
        super(srcType,
              CompositeType.AnyAlpha,
              OGLSurfaceData.OpenGLSurface);
        this.typeval = typeval;
    }

    public void Scale(SurfaceData src, SurfaceData dst,
                      Composite comp, Region clip,
                      int sx1, int sy1,
                      int sx2, int sy2,
                      double dx1, double dy1,
                      double dx2, double dy2)
    {
        OGLBlitLoops.Blit(src, dst,
                          comp, clip, null,
                          AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                          sx1, sy1, sx2, sy2,
                          dx1, dy1, dx2, dy2,
                          typeval, false);
    }
}

class OGLSwToSurfaceTransform extends TransformBlit {

    private int typeval;

    OGLSwToSurfaceTransform(SurfaceType srcType, int typeval) {
        super(srcType,
              CompositeType.AnyAlpha,
              OGLSurfaceData.OpenGLSurface);
        this.typeval = typeval;
    }

    public void Transform(SurfaceData src, SurfaceData dst,
                          Composite comp, Region clip,
                          AffineTransform at, int hint,
                          int sx, int sy, int dx, int dy, int w, int h)
    {
        OGLBlitLoops.Blit(src, dst,
                          comp, clip, at, hint,
                          sx, sy, sx+w, sy+h,
                          dx, dy, dx+w, dy+h,
                          typeval, false);
    }
}

class OGLSwToTextureBlit extends Blit {

    private int typeval;

    OGLSwToTextureBlit(SurfaceType srcType, int typeval) {
        super(srcType,
              CompositeType.SrcNoEa,
              OGLSurfaceData.OpenGLTexture);
        this.typeval = typeval;
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy, int w, int h)
    {
        OGLBlitLoops.Blit(src, dst,
                          comp, clip, null,
                          AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                          sx, sy, sx+w, sy+h,
                          dx, dy, dx+w, dy+h,
                          typeval, true);
    }
}

class OGLTextureToSurfaceBlit extends Blit {

    OGLTextureToSurfaceBlit() {
        super(OGLSurfaceData.OpenGLTexture,
              CompositeType.AnyAlpha,
              OGLSurfaceData.OpenGLSurface);
    }

    public void Blit(SurfaceData src, SurfaceData dst,
                     Composite comp, Region clip,
                     int sx, int sy, int dx, int dy, int w, int h)
    {
        OGLBlitLoops.IsoBlit(src, dst,
                             null, null,
                             comp, clip, null,
                             AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                             sx, sy, sx+w, sy+h,
                             dx, dy, dx+w, dy+h,
                             true);
    }
}

class OGLTextureToSurfaceScale extends ScaledBlit {

    OGLTextureToSurfaceScale() {
        super(OGLSurfaceData.OpenGLTexture,
              CompositeType.AnyAlpha,
              OGLSurfaceData.OpenGLSurface);
    }

    public void Scale(SurfaceData src, SurfaceData dst,
                      Composite comp, Region clip,
                      int sx1, int sy1,
                      int sx2, int sy2,
                      double dx1, double dy1,
                      double dx2, double dy2)
    {
        OGLBlitLoops.IsoBlit(src, dst,
                             null, null,
                             comp, clip, null,
                             AffineTransformOp.TYPE_NEAREST_NEIGHBOR,
                             sx1, sy1, sx2, sy2,
                             dx1, dy1, dx2, dy2,
                             true);
    }
}

class OGLTextureToSurfaceTransform extends TransformBlit {

    OGLTextureToSurfaceTransform() {
        super(OGLSurfaceData.OpenGLTexture,
              CompositeType.AnyAlpha,
              OGLSurfaceData.OpenGLSurface);
    }

    public void Transform(SurfaceData src, SurfaceData dst,
                          Composite comp, Region clip,
                          AffineTransform at, int hint,
                          int sx, int sy, int dx, int dy,
                          int w, int h)
    {
        OGLBlitLoops.IsoBlit(src, dst,
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
 * IntArgbPre->OpenGLSurface/Texture loop to get the intermediate
 * (premultiplied) surface down to OpenGL using simple blit.
 */
class OGLGeneralBlit extends Blit {

    private final Blit performop;
    private WeakReference<SurfaceData> srcTmp;

    OGLGeneralBlit(SurfaceType dstType,
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

        // copy IntArgbPre intermediate surface to OpenGL surface
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
 * IntArgbPre->OpenGLSurface/Texture loop to get the intermediate
 * (premultiplied) surface down to OpenGL using simple transformBlit.
 */
final class OGLGeneralTransformedBlit extends TransformBlit {

    private final TransformBlit performop;
    private WeakReference<SurfaceData> srcTmp;

    OGLGeneralTransformedBlit(final TransformBlit performop) {
        super(SurfaceType.Any, CompositeType.AnyAlpha,
              OGLSurfaceData.OpenGLSurface);
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

        // transform IntArgbPre intermediate surface to OpenGL surface
        performop.Transform(src, dst, comp, clip, at, hint, 0, 0, dstx, dsty,
                            width, height);

        if (src != cachedSrc) {
            // cache the intermediate surface
            srcTmp = new WeakReference<>(src);
        }
    }
}

/**
 * This general OGLAnyCompositeBlit implementation can convert any source/target
 * surface to an intermediate surface using convertsrc/convertdst loops, applies
 * necessary composite operation, and then uses convertresult loop to get the
 * intermediate surface down to OpenGL.
 */
final class OGLAnyCompositeBlit extends Blit {

    private WeakReference<SurfaceData> dstTmp;
    private WeakReference<SurfaceData> srcTmp;
    private final Blit convertsrc;
    private final Blit convertdst;
    private final Blit convertresult;

    OGLAnyCompositeBlit(SurfaceType srctype, Blit convertsrc, Blit convertdst,
                        Blit convertresult) {
        super(srctype, CompositeType.Any, OGLSurfaceData.OpenGLSurface);
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
