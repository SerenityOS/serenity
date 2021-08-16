/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.xr;

import sun.awt.SunToolkit;
import sun.awt.image.*;
import sun.java2d.loops.*;
import sun.java2d.pipe.*;
import sun.java2d.*;
import java.awt.*;
import java.awt.geom.*;
import java.lang.ref.*;

public final class XRPMBlitLoops {

    static WeakReference<SunVolatileImage> argbTmpPM = new WeakReference<SunVolatileImage>(null);
    static WeakReference<SunVolatileImage> rgbTmpPM = new WeakReference<SunVolatileImage>(null);

    private XRPMBlitLoops() {
    }

    public static void register() {
        GraphicsPrimitive[] primitives = { new XRPMBlit(XRSurfaceData.IntRgbX11, XRSurfaceData.IntRgbX11),
                new XRPMBlit(XRSurfaceData.IntRgbX11, XRSurfaceData.IntArgbPreX11),
                new XRPMBlit(XRSurfaceData.IntArgbPreX11, XRSurfaceData.IntRgbX11),
                new XRPMBlit(XRSurfaceData.IntArgbPreX11, XRSurfaceData.IntArgbPreX11),

                new XRPMScaledBlit(XRSurfaceData.IntRgbX11, XRSurfaceData.IntRgbX11),
                new XRPMScaledBlit(XRSurfaceData.IntRgbX11, XRSurfaceData.IntArgbPreX11),
                new XRPMScaledBlit(XRSurfaceData.IntArgbPreX11, XRSurfaceData.IntRgbX11),
                new XRPMScaledBlit(XRSurfaceData.IntArgbPreX11, XRSurfaceData.IntArgbPreX11),

                new XRPMTransformedBlit(XRSurfaceData.IntRgbX11, XRSurfaceData.IntRgbX11),
                new XRPMTransformedBlit(XRSurfaceData.IntRgbX11, XRSurfaceData.IntArgbPreX11),
                new XRPMTransformedBlit(XRSurfaceData.IntArgbPreX11, XRSurfaceData.IntRgbX11),
                new XRPMTransformedBlit(XRSurfaceData.IntArgbPreX11, XRSurfaceData.IntArgbPreX11),

                /* SW -> Surface Blits */
                new XrSwToPMBlit(SurfaceType.IntArgb, XRSurfaceData.IntRgbX11),
                new XrSwToPMBlit(SurfaceType.IntRgb, XRSurfaceData.IntRgbX11),
                new XrSwToPMBlit(SurfaceType.IntBgr, XRSurfaceData.IntRgbX11),
                new XrSwToPMBlit(SurfaceType.ThreeByteBgr, XRSurfaceData.IntRgbX11),
                new XrSwToPMBlit(SurfaceType.Ushort565Rgb, XRSurfaceData.IntRgbX11),
                new XrSwToPMBlit(SurfaceType.Ushort555Rgb, XRSurfaceData.IntRgbX11),
                new XrSwToPMBlit(SurfaceType.ByteIndexed, XRSurfaceData.IntRgbX11),

                new XrSwToPMBlit(SurfaceType.IntArgb, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMBlit(SurfaceType.IntRgb, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMBlit(SurfaceType.IntBgr, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMBlit(SurfaceType.ThreeByteBgr, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMBlit(SurfaceType.Ushort565Rgb, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMBlit(SurfaceType.Ushort555Rgb, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMBlit(SurfaceType.ByteIndexed, XRSurfaceData.IntArgbPreX11),

                /* SW->Surface Scales */
                new XrSwToPMScaledBlit(SurfaceType.IntArgb, XRSurfaceData.IntRgbX11),
                new XrSwToPMScaledBlit(SurfaceType.IntRgb, XRSurfaceData.IntRgbX11),
                new XrSwToPMScaledBlit(SurfaceType.IntBgr, XRSurfaceData.IntRgbX11),
                new XrSwToPMScaledBlit(SurfaceType.ThreeByteBgr, XRSurfaceData.IntRgbX11),
                new XrSwToPMScaledBlit(SurfaceType.Ushort565Rgb, XRSurfaceData.IntRgbX11),
                new XrSwToPMScaledBlit(SurfaceType.Ushort555Rgb, XRSurfaceData.IntRgbX11),
                new XrSwToPMScaledBlit(SurfaceType.ByteIndexed, XRSurfaceData.IntRgbX11),

                new XrSwToPMScaledBlit(SurfaceType.IntArgb, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMScaledBlit(SurfaceType.IntRgb, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMScaledBlit(SurfaceType.IntBgr, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMScaledBlit(SurfaceType.ThreeByteBgr, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMScaledBlit(SurfaceType.Ushort565Rgb, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMScaledBlit(SurfaceType.Ushort555Rgb, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMScaledBlit(SurfaceType.ByteIndexed, XRSurfaceData.IntArgbPreX11),

                /* SW->Surface Transforms */
                new XrSwToPMTransformedBlit(SurfaceType.IntArgb, XRSurfaceData.IntRgbX11),
                new XrSwToPMTransformedBlit(SurfaceType.IntRgb, XRSurfaceData.IntRgbX11),
                new XrSwToPMTransformedBlit(SurfaceType.IntBgr, XRSurfaceData.IntRgbX11),
                new XrSwToPMTransformedBlit(SurfaceType.ThreeByteBgr, XRSurfaceData.IntRgbX11),
                new XrSwToPMTransformedBlit(SurfaceType.Ushort565Rgb, XRSurfaceData.IntRgbX11),
                new XrSwToPMTransformedBlit(SurfaceType.Ushort555Rgb, XRSurfaceData.IntRgbX11),
                new XrSwToPMTransformedBlit(SurfaceType.ByteIndexed, XRSurfaceData.IntRgbX11),

                new XrSwToPMTransformedBlit(SurfaceType.IntArgb, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMTransformedBlit(SurfaceType.IntRgb, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMTransformedBlit(SurfaceType.IntBgr, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMTransformedBlit(SurfaceType.ThreeByteBgr, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMTransformedBlit(SurfaceType.Ushort565Rgb, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMTransformedBlit(SurfaceType.Ushort555Rgb, XRSurfaceData.IntArgbPreX11),
                new XrSwToPMTransformedBlit(SurfaceType.ByteIndexed, XRSurfaceData.IntArgbPreX11), };
        GraphicsPrimitiveMgr.register(primitives);
    }

    /**
     * Caches a SW surface using a temporary pixmap. The pixmap is held by a WeakReference,
     *  allowing it to shrink again after some time.
     */
    protected static XRSurfaceData cacheToTmpSurface(SurfaceData src, XRSurfaceData dst, int w, int h, int sx, int sy) {
        SunVolatileImage vImg;
        SurfaceType vImgSurfaceType;

        if (src.getTransparency() == Transparency.OPAQUE) {
            vImg = rgbTmpPM.get();
            vImgSurfaceType = SurfaceType.IntRgb;
        } else {
            vImg = argbTmpPM.get();
            vImgSurfaceType = SurfaceType.IntArgbPre;
        }

        if (vImg == null || vImg.getWidth() < w || vImg.getHeight() < h ||
            // Sometimes we get volatile image of wrong dest surface type,
            // so recreating it
            !(vImg.getDestSurface() instanceof XRSurfaceData))
        {
            if (vImg != null) {
                vImg.flush();
            }
            vImg = (SunVolatileImage) dst.getGraphicsConfig().createCompatibleVolatileImage(w, h, src.getTransparency());
            vImg.setAccelerationPriority(1.0f);

            if (!(vImg.getDestSurface() instanceof XRSurfaceData)) {
                throw new InvalidPipeException("Could not create XRSurfaceData");
            }
            if (src.getTransparency() == SurfaceData.OPAQUE) {
                rgbTmpPM = new WeakReference<SunVolatileImage>(vImg);
            } else {
                argbTmpPM = new WeakReference<SunVolatileImage>(vImg);
            }
        }

        Blit swToSurfaceBlit = Blit.getFromCache(src.getSurfaceType(), CompositeType.SrcNoEa, vImgSurfaceType);

        if (!(vImg.getDestSurface() instanceof XRSurfaceData)) {
            throw new InvalidPipeException("wrong surface data type: " + vImg.getDestSurface());
        }

        XRSurfaceData vImgSurface = (XRSurfaceData) vImg.getDestSurface();
        swToSurfaceBlit.Blit(src, vImgSurface, AlphaComposite.Src, null,
                             sx, sy, 0, 0, w, h);

        return vImgSurface;
    }
}

class XRPMBlit extends Blit {
    public XRPMBlit(SurfaceType srcType, SurfaceType dstType) {
        super(srcType, CompositeType.AnyAlpha, dstType);
    }

    public void Blit(SurfaceData src, SurfaceData dst, Composite comp, Region clip, int sx, int sy, int dx, int dy, int w, int h) {
        try {
            SunToolkit.awtLock();

            XRSurfaceData x11sdDst = (XRSurfaceData) dst;
            x11sdDst.validateAsDestination(null, clip);
            XRSurfaceData x11sdSrc = (XRSurfaceData) src;
            x11sdSrc.validateAsSource(null, XRUtils.RepeatNone, XRUtils.FAST);

            x11sdDst.maskBuffer.validateCompositeState(comp, null, null, null);

            x11sdDst.maskBuffer.compositeBlit(x11sdSrc, x11sdDst, sx, sy, dx, dy, w, h);
        } finally {
            SunToolkit.awtUnlock();
        }
    }
}

class XRPMScaledBlit extends ScaledBlit {
    public XRPMScaledBlit(SurfaceType srcType, SurfaceType dstType) {
        super(srcType, CompositeType.AnyAlpha, dstType);
    }

    @SuppressWarnings("cast")
    public void Scale(SurfaceData src, SurfaceData dst, Composite comp, Region clip, int sx1, int sy1, int sx2, int sy2, double dx1, double dy1,
            double dx2, double dy2) {
        try {
            SunToolkit.awtLock();

            XRSurfaceData x11sdDst = (XRSurfaceData) dst;
            x11sdDst.validateAsDestination(null, clip);
            XRSurfaceData x11sdSrc = (XRSurfaceData) src;
            x11sdDst.maskBuffer.validateCompositeState(comp, null, null, null);

            double xScale = (dx2 - dx1) / (sx2 - sx1);
            double yScale = (dy2 - dy1) / (sy2 - sy1);

            sx1 *= xScale;
            sx2 *= xScale;
            sy1 *= yScale;
            sy2 *= yScale;

            dx1 = Math.ceil(dx1 - 0.5);
            dy1 = Math.ceil(dy1 - 0.5);
            dx2 = Math.ceil(dx2 - 0.5);
            dy2 = Math.ceil(dy2 - 0.5);

            AffineTransform xForm = AffineTransform.getScaleInstance(1 / xScale, 1 / yScale);

            x11sdSrc.validateAsSource(xForm, XRUtils.RepeatNone, XRUtils.FAST);
            x11sdDst.maskBuffer.compositeBlit(x11sdSrc, x11sdDst, (int) sx1, (int) sy1, (int) dx1, (int) dy1, (int) (dx2 - dx1), (int) (dy2 - dy1));
        } finally {
            SunToolkit.awtUnlock();
        }
    }
}

/**
 * Called also if scale+transform is set
 *
 * @author Clemens Eisserer
 */
class XRPMTransformedBlit extends TransformBlit {
    final Rectangle compositeBounds = new Rectangle();
    final double[] srcCoords = new double[8];
    final double[] dstCoords = new double[8];

    public XRPMTransformedBlit(SurfaceType srcType, SurfaceType dstType) {
        super(srcType, CompositeType.AnyAlpha, dstType);
    }

    /*
     * Calculates the composition-rectangle required for transformed blits.
     * For composite operations where the composition-rectangle defines
     * the modified destination area, coordinates are rounded.
     * Otherwise the composition window rectangle is sized large enough
     * to not clip away any pixels.
     */
    protected void adjustCompositeBounds(boolean isQuadrantRotated, AffineTransform tr,
            int dstx, int dsty, int width, int height) {
        srcCoords[0] = dstx;
        srcCoords[1] = dsty;
        srcCoords[2] = dstx + width;
        srcCoords[3] = dsty + height;

        double minX, minY, maxX, maxY;
        if (isQuadrantRotated) {
            tr.transform(srcCoords, 0, dstCoords, 0, 2);

            minX = Math.min(dstCoords[0], dstCoords[2]);
            minY = Math.min(dstCoords[1], dstCoords[3]);
            maxX = Math.max(dstCoords[0], dstCoords[2]);
            maxY = Math.max(dstCoords[1], dstCoords[3]);

            minX = Math.ceil(minX - 0.5);
            minY = Math.ceil(minY - 0.5);
            maxX = Math.ceil(maxX - 0.5);
            maxY = Math.ceil(maxY - 0.5);
        } else {
            srcCoords[4] = dstx;
            srcCoords[5] = dsty + height;
            srcCoords[6] = dstx + width;
            srcCoords[7] = dsty;

            tr.transform(srcCoords, 0, dstCoords, 0, 4);

            minX = Math.min(dstCoords[0], Math.min(dstCoords[2], Math.min(dstCoords[4], dstCoords[6])));
            minY = Math.min(dstCoords[1], Math.min(dstCoords[3], Math.min(dstCoords[5], dstCoords[7])));
            maxX = Math.max(dstCoords[0], Math.max(dstCoords[2], Math.max(dstCoords[4], dstCoords[6])));
            maxY = Math.max(dstCoords[1], Math.max(dstCoords[3], Math.max(dstCoords[5], dstCoords[7])));

            minX = Math.floor(minX);
            minY = Math.floor(minY);
            maxX = Math.ceil(maxX);
            maxY = Math.ceil(maxY);
        }

        compositeBounds.x = (int) minX;
        compositeBounds.y = (int) minY;
        compositeBounds.width = (int) (maxX - minX);
        compositeBounds.height = (int) (maxY - minY);
    }

    public void Transform(SurfaceData src, SurfaceData dst, Composite comp, Region clip, AffineTransform xform,
            int hint, int srcx, int srcy, int dstx, int dsty, int width, int height) {
        try {
            SunToolkit.awtLock();

            XRSurfaceData x11sdDst = (XRSurfaceData) dst;
            XRSurfaceData x11sdSrc = (XRSurfaceData) src;
            XRCompositeManager xrMgr = XRCompositeManager.getInstance(x11sdSrc);

            float extraAlpha = ((AlphaComposite) comp).getAlpha();
            int filter = XRUtils.ATransOpToXRQuality(hint);
            boolean isQuadrantRotated = XRUtils.isTransformQuadrantRotated(xform);

            adjustCompositeBounds(isQuadrantRotated, xform, dstx, dsty, width, height);

            x11sdDst.validateAsDestination(null, clip);
            x11sdDst.maskBuffer.validateCompositeState(comp, null, null, null);

            AffineTransform trx = AffineTransform.getTranslateInstance(-compositeBounds.x, -compositeBounds.y);
            trx.concatenate(xform);
            AffineTransform maskTX = (AffineTransform) trx.clone();
            trx.translate(-srcx, -srcy);

            try {
                trx.invert();
            } catch (NoninvertibleTransformException ex) {
                trx.setToIdentity();
            }

            if (filter != XRUtils.FAST && (!isQuadrantRotated || extraAlpha != 1.0f)) {
                XRMaskImage mask = x11sdSrc.maskBuffer.getMaskImage();

                // For quadrant-transformed blits geometry is not stored inside the mask
                // therefore we can use a repeating 1x1 mask for applying extra alpha.
                int maskPicture = isQuadrantRotated ? xrMgr.getExtraAlphaMask()
                        : mask.prepareBlitMask(x11sdDst, maskTX, width, height);

                x11sdSrc.validateAsSource(trx, XRUtils.RepeatPad, filter);
                x11sdDst.maskBuffer.con.renderComposite(xrMgr.getCompRule(), x11sdSrc.picture,
                        maskPicture, x11sdDst.picture, 0, 0, 0, 0, compositeBounds.x, compositeBounds.y,
                        compositeBounds.width, compositeBounds.height);
            } else {
                int repeat = filter == XRUtils.FAST ? XRUtils.RepeatNone : XRUtils.RepeatPad;

                x11sdSrc.validateAsSource(trx, repeat, filter);

                // compositeBlit takes care of extra alpha
                x11sdDst.maskBuffer.compositeBlit(x11sdSrc, x11sdDst, 0, 0, compositeBounds.x,
                        compositeBounds.y, compositeBounds.width, compositeBounds.height);
            }
        } finally {
            SunToolkit.awtUnlock();
        }
    }
}

class XrSwToPMBlit extends Blit {
    Blit pmToSurfaceBlit;

    XrSwToPMBlit(SurfaceType srcType, SurfaceType dstType) {
        super(srcType, CompositeType.AnyAlpha, dstType);
        pmToSurfaceBlit = new XRPMBlit(dstType, dstType);
    }

    public void Blit(SurfaceData src, SurfaceData dst, Composite comp, Region clip, int sx, int sy, int dx, int dy, int w, int h) {
        try {
            SunToolkit.awtLock();

            XRSurfaceData vImgSurface = XRPMBlitLoops.cacheToTmpSurface(src, (XRSurfaceData) dst, w, h, sx, sy);
            pmToSurfaceBlit.Blit(vImgSurface, dst, comp, clip, 0, 0, dx, dy, w, h);
        } finally {
            SunToolkit.awtUnlock();
        }
    }
}

class XrSwToPMScaledBlit extends ScaledBlit {
    ScaledBlit pmToSurfaceBlit;

    XrSwToPMScaledBlit(SurfaceType srcType, SurfaceType dstType) {
        super(srcType, CompositeType.AnyAlpha, dstType);
        pmToSurfaceBlit = new XRPMScaledBlit(dstType, dstType);
    }

    public void Scale(SurfaceData src, SurfaceData dst, Composite comp, Region clip, int sx1, int sy1, int sx2, int sy2, double dx1, double dy1,
            double dx2, double dy2) {
        {
            int w = sx2 - sx1;
            int h = sy2 - sy1;

            try {
                SunToolkit.awtLock();
                XRSurfaceData vImgSurface = XRPMBlitLoops.cacheToTmpSurface(src, (XRSurfaceData) dst, w, h, sx1, sy1);
                pmToSurfaceBlit.Scale(vImgSurface, dst, comp, clip, 0, 0, w, h, dx1, dy1, dx2, dy2);
            } finally {
                SunToolkit.awtUnlock();
            }
        }
    }
}

class XrSwToPMTransformedBlit extends TransformBlit {
    TransformBlit pmToSurfaceBlit;

    XrSwToPMTransformedBlit(SurfaceType srcType, SurfaceType dstType) {
        super(srcType, CompositeType.AnyAlpha, dstType);
        pmToSurfaceBlit = new XRPMTransformedBlit(dstType, dstType);
    }

    public void Transform(SurfaceData src, SurfaceData dst, Composite comp, Region clip, AffineTransform xform, int hint, int sx, int sy, int dstx,
            int dsty, int w, int h) {
        try {
            SunToolkit.awtLock();

            XRSurfaceData vImgSurface = XRPMBlitLoops.cacheToTmpSurface(src, (XRSurfaceData) dst, w, h, sx, sy);
            pmToSurfaceBlit.Transform(vImgSurface, dst, comp, clip, xform, hint, 0, 0, dstx, dsty, w, h);
        } finally {
            SunToolkit.awtUnlock();
        }
    }
}
