/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.geom.*;

import java.security.AccessController;
import java.security.PrivilegedAction;

import sun.font.*;
import sun.java2d.*;
import sun.java2d.loops.*;

/**
 * Manages per-application resources, e.g. the 1x1 pixmap used for solid color
 * fill as well as per-application state e.g. the currently set source picture
 * used for composition .
 *
 * @author Clemens Eisserer
 */

public class XRCompositeManager {
    private static boolean enableGradCache = true;
    private static XRCompositeManager instance;

    private static final int SOLID = 0;
    private static final int TEXTURE = 1;
    private static final int GRADIENT = 2;

    int srcType;
    XRSolidSrcPict solidSrc32;
    XRSurfaceData texture;
    XRSurfaceData gradient;
    int alphaMask = XRUtils.None;

    XRColor solidColor = new XRColor();
    float extraAlpha = 1.0f;
    byte compRule = XRUtils.PictOpOver;
    XRColor alphaColor = new XRColor();

    XRSurfaceData solidSrcPict;
    int alphaMaskPict;
    int gradCachePixmap;
    int gradCachePicture;

    boolean xorEnabled = false;
    int validatedPixel = 0;
    Composite validatedComp;
    Paint validatedPaint;
    float validatedExtraAlpha = 1.0f;

    XRBackend con;
    MaskTileManager maskBuffer;
    XRTextRenderer textRenderer;
    XRMaskImage maskImage;

    public static synchronized XRCompositeManager getInstance(
            XRSurfaceData surface) {
        if (instance == null) {
            instance = new XRCompositeManager(surface);
        }
        return instance;
    }

    private XRCompositeManager(XRSurfaceData surface) {
        con = new XRBackendNative();

        @SuppressWarnings("removal")
        String gradProp =
            AccessController.doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty("sun.java2d.xrgradcache");
                }
            });

        enableGradCache = gradProp == null ||
                          !(gradProp.equalsIgnoreCase("false") ||
                          gradProp.equalsIgnoreCase("f"));

        XRPaints.register(this);

        initResources(surface);

        maskBuffer = new MaskTileManager(this, surface.getXid());
        textRenderer = new XRTextRenderer(this);
        maskImage = new XRMaskImage(this, surface.getXid());
    }

    public void initResources(XRSurfaceData surface) {
        int parentXid = surface.getXid();

        solidSrc32 = new XRSolidSrcPict(con, parentXid);
        setForeground(0);

        int extraAlphaMask = con.createPixmap(parentXid, 8, 1, 1);
        alphaMaskPict = con.createPicture(extraAlphaMask,
                XRUtils.PictStandardA8);
        con.setPictureRepeat(alphaMaskPict, XRUtils.RepeatNormal);
        con.renderRectangle(alphaMaskPict, XRUtils.PictOpClear,
                XRColor.NO_ALPHA, 0, 0, 1, 1);

        if (enableGradCache) {
            gradCachePixmap = con.createPixmap(parentXid, 32,
                    MaskTileManager.MASK_SIZE, MaskTileManager.MASK_SIZE);
            gradCachePicture = con.createPicture(gradCachePixmap,
                    XRUtils.PictStandardARGB32);
        }
    }

    public void setForeground(int pixel) {
        solidColor.setColorValues(pixel);
    }

    public void setGradientPaint(XRSurfaceData gradient) {
        if (this.gradient != null) {
            con.freePicture(this.gradient.picture);
        }
        this.gradient = gradient;
        srcType = GRADIENT;
    }

    public void setTexturePaint(XRSurfaceData texture) {
        this.texture = texture;
        this.srcType = TEXTURE;
    }

    public void XRResetPaint() {
        srcType = SOLID;
    }

    public void validateCompositeState(Composite comp, AffineTransform xform,
            Paint paint, SunGraphics2D sg2d) {
        boolean updatePaint = (paint != validatedPaint) || paint == null;

        // validate composite
        if ((comp != validatedComp)) {
            if (comp != null) {
                setComposite(comp);
            } else {
                comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
                setComposite(comp);
            }
            // the paint state is dependent on the composite state, so make
            // sure we update the color below
            updatePaint = true;
            validatedComp = comp;
        }

        if (sg2d != null && (validatedPixel != sg2d.pixel  || updatePaint)) {
            validatedPixel = sg2d.pixel;
            setForeground(validatedPixel);
        }

        // validate paint
        if (updatePaint) {
            if (paint != null && sg2d != null
                    && sg2d.paintState >= SunGraphics2D.PAINT_GRADIENT) {
                XRPaints.setPaint(sg2d, paint);
            } else {
                XRResetPaint();
            }
            validatedPaint = paint;
        }

        if (srcType != SOLID) {
            AffineTransform at = (AffineTransform) xform.clone();
            try {
                at.invert();
            } catch (NoninvertibleTransformException e) {
                at.setToIdentity();
            }
            getCurrentSource().validateAsSource(at, -1, XRUtils.ATransOpToXRQuality(sg2d.interpolationType));
        }
    }

    private void setComposite(Composite comp) {
        if (comp instanceof AlphaComposite) {
            AlphaComposite aComp = (AlphaComposite) comp;
            validatedExtraAlpha = aComp.getAlpha();

            this.compRule = XRUtils.j2dAlphaCompToXR(aComp.getRule());
            this.extraAlpha = validatedExtraAlpha;

            if (extraAlpha == 1.0f) {
                alphaMask = XRUtils.None;
                alphaColor.alpha = XRColor.FULL_ALPHA.alpha;
            } else {
                alphaColor.alpha = XRColor
                        .byteToXRColorValue((int) (extraAlpha * 255));
                alphaMask = alphaMaskPict;
                con.renderRectangle(alphaMaskPict, XRUtils.PictOpSrc,
                        alphaColor, 0, 0, 1, 1);
            }

            xorEnabled = false;
        } else if (comp instanceof XORComposite) {
            /* XOR composite validation is handled in XRSurfaceData */
            xorEnabled = true;
        } else {
            throw new InternalError(
                    "Composite accaleration not implemented for: "
                            + comp.getClass().getName());
        }
    }

    public boolean maskRequired() {
        return (!xorEnabled)
                && ((srcType != SOLID)
                        || (srcType == SOLID && (solidColor.alpha != 0xffff) || (extraAlpha != 1.0f)));
    }

    public void XRComposite(int src, int mask, int dst, int srcX, int srcY,
            int maskX, int maskY, int dstX, int dstY, int width, int height) {
        int cachedSrc = (src == XRUtils.None) ? getCurrentSource().picture : src;
        int cachedX = srcX;
        int cachedY = srcY;

        if (enableGradCache && gradient != null
                && cachedSrc == gradient.picture) {
            con.renderComposite(XRUtils.PictOpSrc, gradient.picture,
                    XRUtils.None, gradCachePicture, srcX, srcY, 0, 0, 0, 0,
                    width, height);
            cachedX = 0;
            cachedY = 0;
            cachedSrc = gradCachePicture;
        }

        con.renderComposite(compRule, cachedSrc, mask, dst, cachedX, cachedY,
                maskX, maskY, dstX, dstY, width, height);
    }

    public void XRRenderRectangles(XRSurfaceData dst, GrowableRectArray rects) {
        if (xorEnabled) {
            con.GCRectangles(dst.getXid(), dst.getGC(), rects);
        } else {
            if (rects.getSize() == 1) {
                con.renderRectangle(dst.getPicture(), compRule, solidColor,
                        rects.getX(0), rects.getY(0), rects.getWidth(0), rects.getHeight(0));
            } else {
                con.renderRectangles(dst.getPicture(), compRule, solidColor, rects);
            }
        }
    }

    public void XRCompositeRectangles(XRSurfaceData dst, GrowableRectArray rects) {
        int srcPict = getCurrentSource().picture;

        for(int i=0; i < rects.getSize(); i++) {
            int x = rects.getX(i);
            int y = rects.getY(i);
            int width = rects.getWidth(i);
            int height = rects.getHeight(i);

            con.renderComposite(compRule, srcPict, XRUtils.None, dst.picture, x, y, 0, 0, x, y, width, height);
        }
    }

    protected XRSurfaceData getCurrentSource() {
        switch(srcType) {
        case SOLID:
            return solidSrc32.prepareSrcPict(validatedPixel);
        case TEXTURE:
            return texture;
        case GRADIENT:
            return gradient;
        }

        return null;
    }

    public void compositeBlit(XRSurfaceData src, XRSurfaceData dst, int sx,
            int sy, int dx, int dy, int w, int h) {
        con.renderComposite(compRule, src.picture, alphaMask, dst.picture, sx,
                sy, 0, 0, dx, dy, w, h);
    }

    public void compositeText(XRSurfaceData dst, int sx, int sy, int glyphSet,
            int maskFormat, GrowableEltArray elts) {
        /*
         * Try to emulate the SRC blend mode with SRC_OVER.
         * We bail out during pipe validation for cases where this is not possible.
         */
        byte textCompRule = (compRule != XRUtils.PictOpSrc) ? compRule : XRUtils.PictOpOver;
        con.XRenderCompositeText(textCompRule, getCurrentSource().picture, dst.picture,
                maskFormat, sx, sy, 0, 0, glyphSet, elts);
    }

    public XRColor getMaskColor() {
        return !isTexturePaintActive() ? XRColor.FULL_ALPHA : getAlphaColor();
    }

    public int getExtraAlphaMask() {
        return alphaMask;
    }

    public boolean isTexturePaintActive() {
        return srcType == TEXTURE;
    }

    public boolean isSolidPaintActive() {
        return srcType == SOLID;
    }

    public XRColor getAlphaColor() {
        return alphaColor;
    }

    public XRBackend getBackend() {
        return con;
    }

    public float getExtraAlpha() {
        return validatedExtraAlpha;
    }

    public byte getCompRule() {
        return compRule;
    }

    public XRTextRenderer getTextRenderer() {
        return textRenderer;
    }

    public MaskTileManager getMaskBuffer() {
        return maskBuffer;
    }

    public XRMaskImage getMaskImage() {
        return maskImage;
    }
}
