/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.MultipleGradientPaint.*;
import java.awt.geom.*;
import java.awt.image.*;
import sun.java2d.*;
import sun.java2d.loops.*;
import sun.java2d.xr.XRSurfaceData.XRInternalSurfaceData;

abstract class XRPaints {
    static XRCompositeManager xrCompMan;

    static final XRGradient xrGradient = new XRGradient();
    static final XRLinearGradient xrLinearGradient = new XRLinearGradient();
    static final XRRadialGradient xrRadialGradient = new XRRadialGradient();
    static final XRTexture xrTexture = new XRTexture();

    public static void register(XRCompositeManager xrComp) {
        xrCompMan = xrComp;
    }

    private static XRPaints getXRPaint(SunGraphics2D sg2d) {
        switch (sg2d.paintState) {
        case SunGraphics2D.PAINT_GRADIENT:
            return xrGradient;

        case SunGraphics2D.PAINT_LIN_GRADIENT:
            return xrLinearGradient;

        case SunGraphics2D.PAINT_RAD_GRADIENT:
            return xrRadialGradient;

        case SunGraphics2D.PAINT_TEXTURE:
            return xrTexture;

        default:
            return null;
        }
    }

    /**
     * Attempts to locate an implementation corresponding to the paint state of
     * the provided SunGraphics2D object. If no implementation can be found, or
     * if the paint cannot be accelerated under the conditions of the
     * SunGraphics2D, this method returns false; otherwise, returns true.
     */
    static boolean isValid(SunGraphics2D sg2d) {
        XRPaints impl = getXRPaint(sg2d);
        return (impl != null && impl.isPaintValid(sg2d));
    }

    static void setPaint(SunGraphics2D sg2d, Paint paint) {
        XRPaints impl = getXRPaint(sg2d);
        if (impl != null) {
            impl.setXRPaint(sg2d, paint);
        }
    }

    /**
     * Returns true if this implementation is able to accelerate the Paint
     * object associated with, and under the conditions of, the provided
     * SunGraphics2D instance; otherwise returns false.
     */
    abstract boolean isPaintValid(SunGraphics2D sg2d);

    abstract void setXRPaint(SunGraphics2D sg2d, Paint paint);

    private static class XRGradient extends XRPaints {
        private XRGradient() {
        }

        @Override
        boolean isPaintValid(SunGraphics2D sg2d) {
            GradientPaint paint = (GradientPaint) sg2d.paint;

            return XRUtils.isPointCoordInShortRange(paint.getPoint1())
                    && XRUtils.isPointCoordInShortRange(paint.getPoint2());
        }

        @Override
        void setXRPaint(SunGraphics2D sg2d, Paint pt) {
            GradientPaint paint = (GradientPaint) pt;

            int repeat = paint.isCyclic() ? XRUtils.RepeatReflect : XRUtils.RepeatPad;
            float[] fractions = {0, 1};
            int[] pixels = convertToIntArgbPixels(new Color[] { paint.getColor1(), paint.getColor2() });

            Point2D pt1 = paint.getPoint1();
            Point2D pt2 = paint.getPoint2();

            XRBackend con = xrCompMan.getBackend();
            int gradient = con.createLinearGradient(pt1, pt2, fractions, pixels, repeat);
            xrCompMan.setGradientPaint(new XRSurfaceData.XRInternalSurfaceData(con, gradient));
        }
    }

    public int getGradientLength(Point2D pt1, Point2D pt2) {
           double xDiff = Math.max(pt1.getX(), pt2.getX()) - Math.min(pt1.getX(), pt2.getX());
           double yDiff = Math.max(pt1.getY(), pt2.getY()) - Math.min(pt1.getY(), pt2.getY());
           return (int) Math.ceil(Math.sqrt(xDiff*xDiff + yDiff*yDiff));
    }

    private static class XRLinearGradient extends XRPaints {

        @Override
        boolean isPaintValid(SunGraphics2D sg2d) {
            LinearGradientPaint paint = (LinearGradientPaint) sg2d.getPaint();

            return paint.getColorSpace() == ColorSpaceType.SRGB
                    && XRUtils.isPointCoordInShortRange(paint.getStartPoint())
                    && XRUtils.isPointCoordInShortRange(paint.getEndPoint())
                    && paint.getTransform().getDeterminant() != 0.0;
        }

        @Override
        void setXRPaint(SunGraphics2D sg2d, Paint pt) {
            LinearGradientPaint paint = (LinearGradientPaint) pt;
            Color[] colors = paint.getColors();
            Point2D pt1 = paint.getStartPoint();
            Point2D pt2 = paint.getEndPoint();
            int repeat = XRUtils.getRepeatForCycleMethod(paint.getCycleMethod());
            float[] fractions = paint.getFractions();
            int[] pixels = convertToIntArgbPixels(colors);
            AffineTransform at = paint.getTransform();

            try {
               at.invert();
            } catch (NoninvertibleTransformException ex) {
                ex.printStackTrace();
            }

            XRBackend con = xrCompMan.getBackend();
            int gradient = con.createLinearGradient(pt1, pt2, fractions, pixels, repeat);
            XRInternalSurfaceData x11sd = new XRSurfaceData.XRInternalSurfaceData(con, gradient);
            x11sd.setStaticSrcTx(at);
            xrCompMan.setGradientPaint(x11sd);
        }
    }

    private static class XRRadialGradient extends XRPaints {

        @Override
        boolean isPaintValid(SunGraphics2D sg2d) {
            RadialGradientPaint grad = (RadialGradientPaint) sg2d.paint;

            return grad.getColorSpace() == ColorSpaceType.SRGB
                   && grad.getFocusPoint().equals(grad.getCenterPoint())
                   && XRUtils.isPointCoordInShortRange(grad.getCenterPoint())
                   && grad.getRadius() <= Short.MAX_VALUE
                   && grad.getTransform().getDeterminant() != 0.0;
        }

        @Override
        void setXRPaint(SunGraphics2D sg2d, Paint pt) {
            RadialGradientPaint paint = (RadialGradientPaint) pt;
            Color[] colors = paint.getColors();
            Point2D center = paint.getCenterPoint();
            float cx = (float) center.getX();
            float cy = (float) center.getY();

            AffineTransform at = paint.getTransform();
            int repeat = XRUtils.getRepeatForCycleMethod(paint.getCycleMethod());
            float[] fractions = paint.getFractions();
            int[] pixels = convertToIntArgbPixels(colors);
            float radius = paint.getRadius();

            try {
               at.invert();
            } catch (NoninvertibleTransformException ex) {
                ex.printStackTrace();
            }

            XRBackend con = xrCompMan.getBackend();
            int gradient = con.createRadialGradient(cx, cy, 0, radius, fractions, pixels, repeat);
            XRInternalSurfaceData x11sd = new XRSurfaceData.XRInternalSurfaceData(con, gradient);
            x11sd.setStaticSrcTx(at);
            xrCompMan.setGradientPaint(x11sd);
        }
    }

    private static class XRTexture extends XRPaints {

        private XRSurfaceData getAccSrcSurface(XRSurfaceData dstData, BufferedImage bi) {
            // REMIND: this is a hack that attempts to cache the system
            // memory image from the TexturePaint instance into an
            // XRender pixmap...
            SurfaceData srcData = dstData.getSourceSurfaceData(bi, SunGraphics2D.TRANSFORM_ISIDENT, CompositeType.SrcOver, null);
            if (!(srcData instanceof XRSurfaceData)) {
                srcData = dstData.getSourceSurfaceData(bi, SunGraphics2D.TRANSFORM_ISIDENT, CompositeType.SrcOver, null);
                if (!(srcData instanceof XRSurfaceData)) {
                    throw new InternalError("Surface not cachable");
                }
            }

            return (XRSurfaceData) srcData;
        }

        @Override
        boolean isPaintValid(SunGraphics2D sg2d) {
            TexturePaint paint = (TexturePaint) sg2d.paint;
            BufferedImage bi = paint.getImage();
            XRSurfaceData dstData = (XRSurfaceData) sg2d.getDestSurface();

            return getAccSrcSurface(dstData, bi) != null;
        }

        @Override
        void setXRPaint(SunGraphics2D sg2d, Paint pt) {
            TexturePaint paint = (TexturePaint) pt;
            BufferedImage bi = paint.getImage();
            Rectangle2D anchor = paint.getAnchorRect();

            XRSurfaceData dstData = (XRSurfaceData) sg2d.surfaceData;
            XRSurfaceData srcData = getAccSrcSurface(dstData, bi);

            AffineTransform at = new AffineTransform();
            at.translate(anchor.getX(), anchor.getY());
            at.scale(anchor.getWidth() / ((double) bi.getWidth()), anchor.getHeight() / ((double) bi.getHeight()));

            try {
                at.invert();
            } catch (NoninvertibleTransformException ex) {
                at.setToIdentity();
            }
            srcData.setStaticSrcTx(at);

            srcData.validateAsSource(at, XRUtils.RepeatNormal, XRUtils.ATransOpToXRQuality(sg2d.interpolationType));
            xrCompMan.setTexturePaint(srcData);
        }
    }

    public int[] convertToIntArgbPixels(Color[] colors) {
        int[] pixels = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            pixels[i] = colorToIntArgbPixel(colors[i]);
        }
        return pixels;
    }

    public int colorToIntArgbPixel(Color c) {
        int rgb = c.getRGB();
        int a = Math.round(xrCompMan.getExtraAlpha() * (rgb >>> 24));
        return ((a << 24) | (rgb & 0x00FFFFFF));
    }
}
