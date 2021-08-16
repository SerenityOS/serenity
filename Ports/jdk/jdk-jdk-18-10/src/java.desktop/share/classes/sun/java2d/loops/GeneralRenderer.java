/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @author Charlton Innovations, Inc.
 */

package sun.java2d.loops;

import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.image.ColorModel;
import java.awt.geom.Path2D;
import sun.java2d.pipe.Region;
import sun.java2d.pipe.SpanIterator;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.loops.ProcessPath;
import sun.font.GlyphList;

/**
 * GeneralRenderer collection
 * Basically, a collection of components which permit basic
 * rendering to occur on rasters of any format
 */

public final class GeneralRenderer {
    public static void register() {
        Class<?> owner = GeneralRenderer.class;
        GraphicsPrimitive[] primitives = {
            new  GraphicsPrimitiveProxy(owner, "SetFillRectANY",
                                        FillRect.methodSignature,
                                        FillRect.primTypeID,
                                        SurfaceType.AnyColor,
                                        CompositeType.SrcNoEa,
                                        SurfaceType.Any),
            new  GraphicsPrimitiveProxy(owner, "SetFillPathANY",
                                        FillPath.methodSignature,
                                        FillPath.primTypeID,
                                        SurfaceType.AnyColor,
                                        CompositeType.SrcNoEa,
                                        SurfaceType.Any),
            new  GraphicsPrimitiveProxy(owner, "SetFillSpansANY",
                                        FillSpans.methodSignature,
                                        FillSpans.primTypeID,
                                        SurfaceType.AnyColor,
                                        CompositeType.SrcNoEa,
                                        SurfaceType.Any),
            new  GraphicsPrimitiveProxy(owner, "SetDrawLineANY",
                                        DrawLine.methodSignature,
                                        DrawLine.primTypeID,
                                        SurfaceType.AnyColor,
                                        CompositeType.SrcNoEa,
                                        SurfaceType.Any),
            new  GraphicsPrimitiveProxy(owner, "SetDrawPolygonsANY",
                                        DrawPolygons.methodSignature,
                                        DrawPolygons.primTypeID,
                                        SurfaceType.AnyColor,
                                        CompositeType.SrcNoEa,
                                        SurfaceType.Any),
            new  GraphicsPrimitiveProxy(owner, "SetDrawPathANY",
                                        DrawPath.methodSignature,
                                        DrawPath.primTypeID,
                                        SurfaceType.AnyColor,
                                        CompositeType.SrcNoEa,
                                        SurfaceType.Any),
            new  GraphicsPrimitiveProxy(owner, "SetDrawRectANY",
                                        DrawRect.methodSignature,
                                        DrawRect.primTypeID,
                                        SurfaceType.AnyColor,
                                        CompositeType.SrcNoEa,
                                        SurfaceType.Any),

            new  GraphicsPrimitiveProxy(owner, "XorFillRectANY",
                                        FillRect.methodSignature,
                                        FillRect.primTypeID,
                                        SurfaceType.AnyColor,
                                        CompositeType.Xor,
                                        SurfaceType.Any),
            new  GraphicsPrimitiveProxy(owner, "XorFillPathANY",
                                        FillPath.methodSignature,
                                        FillPath.primTypeID,
                                        SurfaceType.AnyColor,
                                        CompositeType.Xor,
                                        SurfaceType.Any),
            new  GraphicsPrimitiveProxy(owner, "XorFillSpansANY",
                                        FillSpans.methodSignature,
                                        FillSpans.primTypeID,
                                        SurfaceType.AnyColor,
                                        CompositeType.Xor,
                                        SurfaceType.Any),
            new  GraphicsPrimitiveProxy(owner, "XorDrawLineANY",
                                        DrawLine.methodSignature,
                                        DrawLine.primTypeID,
                                        SurfaceType.AnyColor,
                                        CompositeType.Xor,
                                        SurfaceType.Any),
            new  GraphicsPrimitiveProxy(owner, "XorDrawPolygonsANY",
                                        DrawPolygons.methodSignature,
                                        DrawPolygons.primTypeID,
                                        SurfaceType.AnyColor,
                                        CompositeType.Xor,
                                        SurfaceType.Any),
            new  GraphicsPrimitiveProxy(owner, "XorDrawPathANY",
                                        DrawPath.methodSignature,
                                        DrawPath.primTypeID,
                                        SurfaceType.AnyColor,
                                        CompositeType.Xor,
                                        SurfaceType.Any),
            new  GraphicsPrimitiveProxy(owner, "XorDrawRectANY",
                                        DrawRect.methodSignature,
                                        DrawRect.primTypeID,
                                        SurfaceType.AnyColor,
                                        CompositeType.Xor,
                                        SurfaceType.Any),
            new  GraphicsPrimitiveProxy(owner, "XorDrawGlyphListANY",
                                        DrawGlyphList.methodSignature,
                                        DrawGlyphList.primTypeID,
                                        SurfaceType.AnyColor,
                                        CompositeType.Xor,
                                        SurfaceType.Any),
            new  GraphicsPrimitiveProxy(owner, "XorDrawGlyphListAAANY",
                                        DrawGlyphListAA.methodSignature,
                                        DrawGlyphListAA.primTypeID,
                                        SurfaceType.AnyColor,
                                        CompositeType.Xor,
                                        SurfaceType.Any),
        };
        GraphicsPrimitiveMgr.register(primitives);
    }

    static void doDrawPoly(SurfaceData sData, PixelWriter pw,
                           int[] xPoints, int[] yPoints, int off, int nPoints,
                           Region clip, int transx, int transy, boolean close)
    {
        int mx, my, x1, y1;
        int[] tmp = null;

        if (nPoints <= 0) {
            return;
        }
        mx = x1 = xPoints[off] + transx;
        my = y1 = yPoints[off] + transy;
        while (--nPoints > 0) {
            ++off;
            int x2 = xPoints[off] + transx;
            int y2 = yPoints[off] + transy;
            tmp = GeneralRenderer.doDrawLine(sData, pw, tmp, clip,
                                             x1, y1, x2, y2);
            x1 = x2;
            y1 = y2;
        }
        if (close && (x1 != mx || y1 != my)) {
            tmp = GeneralRenderer.doDrawLine(sData, pw, tmp, clip,
                                             x1, y1, mx, my);
        }
    }

    static void doSetRect(SurfaceData sData, PixelWriter pw,
                          int x1, int y1, int x2, int y2) {
        WritableRaster dstRast =
            (WritableRaster) sData.getRaster(x1, y1, x2-x1, y2-y1);
        pw.setRaster(dstRast);

        while (y1 < y2) {
            for (int x = x1; x < x2; x++) {
                pw.writePixel(x, y1);
            }
            y1++;
        }
    }

    static int[] doDrawLine(SurfaceData sData, PixelWriter pw, int[] boundPts,
                            Region clip,
                            int origx1, int origy1, int origx2, int origy2)
    {
        if (boundPts == null) {
            boundPts = new int[8];
        }
        boundPts[0] = origx1;
        boundPts[1] = origy1;
        boundPts[2] = origx2;
        boundPts[3] = origy2;
        if (!adjustLine(boundPts,
                        clip.getLoX(), clip.getLoY(),
                        clip.getHiX(), clip.getHiY()))
        {
            return boundPts;
        }
        int x1 = boundPts[0];
        int y1 = boundPts[1];
        int x2 = boundPts[2];
        int y2 = boundPts[3];

        WritableRaster dstRast = (WritableRaster)
            sData.getRaster(Math.min(x1, x2), Math.min(y1, y2),
                            Math.abs(x1 - x2) + 1, Math.abs(y1 - y2) + 1);
        pw.setRaster(dstRast);

        /* this could be made smaller, more elegant, more traditional. */
        if (x1 == x2) {
            if (y1 > y2) {
                do {
                    pw.writePixel(x1, y1);
                    y1--;
                } while (y1 >= y2);
            } else {
                do {
                    pw.writePixel(x1, y1);
                    y1++;
                } while (y1 <= y2);
            }
        } else if (y1 == y2) {
            if (x1 > x2) {
                do {
                    pw.writePixel(x1, y1);
                    x1--;
                } while (x1 >= x2);
            } else {
                do {
                    pw.writePixel(x1, y1);
                    x1++;
                } while (x1 <= x2);
            }
        } else {
            int dx = boundPts[4];
            int dy = boundPts[5];
            int ax = boundPts[6];
            int ay = boundPts[7];
            int steps;
            int bumpmajor;
            int bumpminor;
            int errminor;
            int errmajor;
            int error;
            boolean xmajor;

            if (ax >= ay) {
                /* x is dominant */
                xmajor = true;
                errmajor = ay * 2;
                errminor = ax * 2;
                bumpmajor = (dx < 0) ? -1 : 1;
                bumpminor = (dy < 0) ? -1 : 1;
                ax = -ax; /* For clipping adjustment below */
                steps = x2 - x1;
            } else {
                /* y is dominant */
                xmajor = false;
                errmajor = ax * 2;
                errminor = ay * 2;
                bumpmajor = (dy < 0) ? -1 : 1;
                bumpminor = (dx < 0) ? -1 : 1;
                ay = -ay; /* For clipping adjustment below */
                steps = y2 - y1;
            }
            error = - (errminor / 2);
            if (y1 != origy1) {
                int ysteps = y1 - origy1;
                if (ysteps < 0) {
                    ysteps = -ysteps;
                }
                error += ysteps * ax * 2;
            }
            if (x1 != origx1) {
                int xsteps = x1 - origx1;
                if (xsteps < 0) {
                    xsteps = -xsteps;
                }
                error += xsteps * ay * 2;
            }
            if (steps < 0) {
                steps = -steps;
            }
            if (xmajor) {
                do {
                    pw.writePixel(x1, y1);
                    x1 += bumpmajor;
                    error += errmajor;
                    if (error >= 0) {
                        y1 += bumpminor;
                        error -= errminor;
                    }
                } while (--steps >= 0);
            } else {
                do {
                    pw.writePixel(x1, y1);
                    y1 += bumpmajor;
                    error += errmajor;
                    if (error >= 0) {
                        x1 += bumpminor;
                        error -= errminor;
                    }
                } while (--steps >= 0);
            }
        }
        return boundPts;
    }

    public static void doDrawRect(PixelWriter pw,
                                  SunGraphics2D sg2d, SurfaceData sData,
                                  int x, int y, int w, int h)
    {
        if (w < 0 || h < 0) {
            return;
        }
        int x2 = Region.dimAdd(Region.dimAdd(x, w), 1);
        int y2 = Region.dimAdd(Region.dimAdd(y, h), 1);
        Region r = sg2d.getCompClip().getBoundsIntersectionXYXY(x, y, x2, y2);
        if (r.isEmpty()) {
            return;
        }
        int cx1 = r.getLoX();
        int cy1 = r.getLoY();
        int cx2 = r.getHiX();
        int cy2 = r.getHiY();

        if (w < 2 || h < 2) {
            doSetRect(sData, pw, cx1, cy1, cx2, cy2);
            return;
        }


        if (cy1 == y) {
            doSetRect(sData, pw,   cx1,   cy1,   cx2, cy1+1);
        }
        if (cx1 == x) {
            doSetRect(sData, pw,   cx1, cy1+1, cx1+1, cy2-1);
        }
        if (cx2 == x2) {
            doSetRect(sData, pw, cx2-1, cy1+1,   cx2, cy2-1);
        }
        if (cy2 == y2) {
            doSetRect(sData, pw,   cx1, cy2-1,   cx2,   cy2);
        }
    }

    /*
     * REMIND: For now this will field both AA and non-AA requests and
     * use a simple threshold to choose pixels if the supplied grey
     * bits are antialiased.  We should really find a way to disable
     * AA text at a higher level or to have the GlyphList be able to
     * reset the glyphs to non-AA after construction.
     */
    static void doDrawGlyphList(SurfaceData sData, PixelWriter pw,
                                GlyphList gl, int fromGlyph, int toGlyph,
                                Region clip)
    {
        int[] bounds = gl.getBounds(toGlyph);
        clip.clipBoxToBounds(bounds);
        int cx1 = bounds[0];
        int cy1 = bounds[1];
        int cx2 = bounds[2];
        int cy2 = bounds[3];

        WritableRaster dstRast =
            (WritableRaster) sData.getRaster(cx1, cy1, cx2 - cx1, cy2 - cy1);
        pw.setRaster(dstRast);

        for (int i = fromGlyph; i < toGlyph; i++) {
            gl.setGlyphIndex(i);
            int[] metrics = gl.getMetrics();
            int gx1 = metrics[0];
            int gy1 = metrics[1];
            int w = metrics[2];
            int gx2 = gx1 + w;
            int gy2 = gy1 + metrics[3];
            int off = 0;
            if (gx1 < cx1) {
                off = cx1 - gx1;
                gx1 = cx1;
            }
            if (gy1 < cy1) {
                off += (cy1 - gy1) * w;
                gy1 = cy1;
            }
            if (gx2 > cx2) gx2 = cx2;
            if (gy2 > cy2) gy2 = cy2;
            if (gx2 > gx1 && gy2 > gy1) {
                byte[] alpha = gl.getGrayBits();
                w -= (gx2 - gx1);
                for (int y = gy1; y < gy2; y++) {
                    for (int x = gx1; x < gx2; x++) {
                        if (alpha[off++] < 0) {
                            pw.writePixel(x, y);
                        }
                    }
                    off += w;
                }
            }
        }
    }

    static final int OUTCODE_TOP     = 1;
    static final int OUTCODE_BOTTOM  = 2;
    static final int OUTCODE_LEFT    = 4;
    static final int OUTCODE_RIGHT   = 8;

    static int outcode(int x, int y, int xmin, int ymin, int xmax, int ymax) {
        int code;
        if (y < ymin) {
            code = OUTCODE_TOP;
        } else if (y > ymax) {
            code = OUTCODE_BOTTOM;
        } else {
            code = 0;
        }
        if (x < xmin) {
            code |= OUTCODE_LEFT;
        } else if (x > xmax) {
            code |= OUTCODE_RIGHT;
        }
        return code;
    }

    public static boolean adjustLine(int [] boundPts,
                                     int cxmin, int cymin, int cx2, int cy2)
    {
        int cxmax = cx2 - 1;
        int cymax = cy2 - 1;
        int x1 = boundPts[0];
        int y1 = boundPts[1];
        int x2 = boundPts[2];
        int y2 = boundPts[3];

        if ((cxmax < cxmin) || (cymax < cymin)) {
            return false;
        }

        if (x1 == x2) {
            if (x1 < cxmin || x1 > cxmax) {
                return false;
            }
            if (y1 > y2) {
                int t = y1;
                y1 = y2;
                y2 = t;
            }
            if (y1 < cymin) {
                y1 = cymin;
            }
            if (y2 > cymax) {
                y2 = cymax;
            }
            if (y1 > y2) {
                return false;
            }
            boundPts[1] = y1;
            boundPts[3] = y2;
        } else if (y1 == y2) {
            if (y1 < cymin || y1 > cymax) {
                return false;
            }
            if (x1 > x2) {
                int t = x1;
                x1 = x2;
                x2 = t;
            }
            if (x1 < cxmin) {
                x1 = cxmin;
            }
            if (x2 > cxmax) {
                x2 = cxmax;
            }
            if (x1 > x2) {
                return false;
            }
            boundPts[0] = x1;
            boundPts[2] = x2;
        } else {
            /* REMIND: This could overflow... */
            int outcode1, outcode2;
            int dx = x2 - x1;
            int dy = y2 - y1;
            int ax = (dx < 0) ? -dx : dx;
            int ay = (dy < 0) ? -dy : dy;
            boolean xmajor = (ax >= ay);

            outcode1 = outcode(x1, y1, cxmin, cymin, cxmax, cymax);
            outcode2 = outcode(x2, y2, cxmin, cymin, cxmax, cymax);
            while ((outcode1 | outcode2) != 0) {
                int xsteps, ysteps;
                if ((outcode1 & outcode2) != 0) {
                    return false;
                }
                if (outcode1 != 0) {
                    if (0 != (outcode1 & (OUTCODE_TOP | OUTCODE_BOTTOM))) {
                        if (0 != (outcode1 & OUTCODE_TOP)) {
                            y1 = cymin;
                        } else {
                            y1 = cymax;
                        }
                        ysteps = y1 - boundPts[1];
                        if (ysteps < 0) {
                            ysteps = -ysteps;
                        }
                        xsteps = 2 * ysteps * ax + ay;
                        if (xmajor) {
                            xsteps += ay - ax - 1;
                        }
                        xsteps = xsteps / (2 * ay);
                        if (dx < 0) {
                            xsteps = -xsteps;
                        }
                        x1 = boundPts[0] + xsteps;
                    } else if (0 !=
                               (outcode1 & (OUTCODE_LEFT | OUTCODE_RIGHT))) {
                        if (0 != (outcode1 & OUTCODE_LEFT)) {
                            x1 = cxmin;
                        } else {
                            x1 = cxmax;
                        }
                        xsteps = x1 - boundPts[0];
                        if (xsteps < 0) {
                            xsteps = -xsteps;
                        }
                        ysteps = 2 * xsteps * ay + ax;
                        if (!xmajor) {
                            ysteps += ax - ay - 1;
                        }
                        ysteps = ysteps / (2 * ax);
                        if (dy < 0) {
                            ysteps = -ysteps;
                        }
                        y1 = boundPts[1] + ysteps;
                    }
                    outcode1 = outcode(x1, y1, cxmin, cymin, cxmax, cymax);
                } else {
                    if (0 != (outcode2 & (OUTCODE_TOP | OUTCODE_BOTTOM))) {
                        if (0 != (outcode2 & OUTCODE_TOP)) {
                            y2 = cymin;
                        } else {
                            y2 = cymax;
                        }
                        ysteps = y2 - boundPts[3];
                        if (ysteps < 0) {
                            ysteps = -ysteps;
                        }
                        xsteps = 2 * ysteps * ax + ay;
                        if (xmajor) {
                            xsteps += ay - ax;
                        } else {
                            xsteps -= 1;
                        }
                        xsteps = xsteps / (2 * ay);
                        if (dx > 0) {
                            xsteps = -xsteps;
                        }
                        x2 = boundPts[2] + xsteps;
                    } else if (0 !=
                               (outcode2 & (OUTCODE_LEFT | OUTCODE_RIGHT))) {
                        if (0 != (outcode2 & OUTCODE_LEFT)) {
                            x2 = cxmin;
                        } else {
                            x2 = cxmax;
                        }
                        xsteps = x2 - boundPts[2];
                        if (xsteps < 0) {
                            xsteps = -xsteps;
                        }
                        ysteps = 2 * xsteps * ay + ax;
                        if (xmajor) {
                            ysteps -= 1;
                        } else {
                            ysteps += ax - ay;
                        }
                        ysteps = ysteps / (2 * ax);
                        if (dy > 0) {
                            ysteps = -ysteps;
                        }
                        y2 = boundPts[3] + ysteps;
                    }
                    outcode2 = outcode(x2, y2, cxmin, cymin, cxmax, cymax);
                }
            }
            boundPts[0] = x1;
            boundPts[1] = y1;
            boundPts[2] = x2;
            boundPts[3] = y2;
            boundPts[4] = dx;
            boundPts[5] = dy;
            boundPts[6] = ax;
            boundPts[7] = ay;
        }
        return true;
    }

    static PixelWriter createSolidPixelWriter(SunGraphics2D sg2d,
                                              SurfaceData sData)
    {
        ColorModel dstCM = sData.getColorModel();
        Object srcPixel = dstCM.getDataElements(sg2d.eargb, null);

        return new SolidPixelWriter(srcPixel);
    }

    static PixelWriter createXorPixelWriter(SunGraphics2D sg2d,
                                            SurfaceData sData)
    {
        ColorModel dstCM = sData.getColorModel();

        Object srcPixel = dstCM.getDataElements(sg2d.eargb, null);

        XORComposite comp = (XORComposite)sg2d.getComposite();
        int xorrgb = comp.getXorColor().getRGB();
        Object xorPixel = dstCM.getDataElements(xorrgb, null);

        switch (dstCM.getTransferType()) {
        case DataBuffer.TYPE_BYTE:
            return new XorPixelWriter.ByteData(srcPixel, xorPixel);
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
            return new XorPixelWriter.ShortData(srcPixel, xorPixel);
        case DataBuffer.TYPE_INT:
            return new XorPixelWriter.IntData(srcPixel, xorPixel);
        case DataBuffer.TYPE_FLOAT:
            return new XorPixelWriter.FloatData(srcPixel, xorPixel);
        case DataBuffer.TYPE_DOUBLE:
            return new XorPixelWriter.DoubleData(srcPixel, xorPixel);
        default:
            throw new InternalError("Unsupported XOR pixel type");
        }
    }
}

class SetFillRectANY extends FillRect {
    SetFillRectANY() {
        super(SurfaceType.AnyColor,
              CompositeType.SrcNoEa,
              SurfaceType.Any);
    }

    public void FillRect(SunGraphics2D sg2d, SurfaceData sData,
                         int x, int y, int w, int h)
    {
        PixelWriter pw = GeneralRenderer.createSolidPixelWriter(sg2d, sData);

        Region r = sg2d.getCompClip().getBoundsIntersectionXYWH(x, y, w, h);

        GeneralRenderer.doSetRect(sData, pw,
                                  r.getLoX(), r.getLoY(),
                                  r.getHiX(), r.getHiY());
    }
}

class PixelWriterDrawHandler extends ProcessPath.DrawHandler {
    PixelWriter pw;
    SurfaceData sData;
    Region clip;

    public PixelWriterDrawHandler(SurfaceData sData, PixelWriter pw,
                                  Region clip, int strokeHint) {
        super(clip.getLoX(), clip.getLoY(),
              clip.getHiX(), clip.getHiY(),
              strokeHint);
        this.sData = sData;
        this.pw = pw;
        this.clip = clip;
    }

    public void drawLine(int x0, int y0, int x1, int y1) {
        GeneralRenderer.doDrawLine(sData, pw, null, clip,
                                   x0, y0, x1, y1);
    }

    public void drawPixel(int x0, int y0) {
        GeneralRenderer.doSetRect(sData, pw, x0, y0, x0 + 1, y0 + 1);
    }

    public void drawScanline(int x0, int x1, int y0) {
        GeneralRenderer.doSetRect(sData, pw, x0, y0, x1 + 1, y0 + 1);
    }
}

class SetFillPathANY extends FillPath {
    SetFillPathANY() {
        super(SurfaceType.AnyColor, CompositeType.SrcNoEa,
              SurfaceType.Any);
    }

    public void FillPath(SunGraphics2D sg2d, SurfaceData sData,
                         int transx, int transy,
                         Path2D.Float p2df)
    {
        PixelWriter pw = GeneralRenderer.createSolidPixelWriter(sg2d, sData);
        ProcessPath.fillPath(
            new PixelWriterDrawHandler(sData, pw, sg2d.getCompClip(),
                                       sg2d.strokeHint),
            p2df, transx, transy);
    }
}

class SetFillSpansANY extends FillSpans {
    SetFillSpansANY() {
        super(SurfaceType.AnyColor,
              CompositeType.SrcNoEa,
              SurfaceType.Any);
    }

    public void FillSpans(SunGraphics2D sg2d, SurfaceData sData,
                          SpanIterator si)
    {
        PixelWriter pw = GeneralRenderer.createSolidPixelWriter(sg2d, sData);

        int[] span = new int[4];
        while (si.nextSpan(span)) {
            GeneralRenderer.doSetRect(sData, pw,
                                      span[0], span[1], span[2], span[3]);
        }
    }
}

class SetDrawLineANY extends DrawLine {
    SetDrawLineANY() {
        super(SurfaceType.AnyColor,
              CompositeType.SrcNoEa,
              SurfaceType.Any);
    }

    public void DrawLine(SunGraphics2D sg2d, SurfaceData sData,
                         int x1, int y1, int x2, int y2)
    {
        PixelWriter pw = GeneralRenderer.createSolidPixelWriter(sg2d, sData);

        if (y1 >= y2) {
            GeneralRenderer.doDrawLine(sData, pw, null,
                                       sg2d.getCompClip(),
                                       x2, y2, x1, y1);
        } else {
            GeneralRenderer.doDrawLine(sData, pw, null,
                                       sg2d.getCompClip(),
                                       x1, y1, x2, y2);
        }
    }
}

class SetDrawPolygonsANY extends DrawPolygons {
    SetDrawPolygonsANY() {
        super(SurfaceType.AnyColor,
              CompositeType.SrcNoEa,
              SurfaceType.Any);
    }

    public void DrawPolygons(SunGraphics2D sg2d, SurfaceData sData,
                             int[] xPoints, int[] yPoints,
                             int[] nPoints, int numPolys,
                             int transx, int transy,
                             boolean close)
    {
        PixelWriter pw = GeneralRenderer.createSolidPixelWriter(sg2d, sData);

        int off = 0;
        Region clip = sg2d.getCompClip();
        for (int i = 0; i < numPolys; i++) {
            int numpts = nPoints[i];
            GeneralRenderer.doDrawPoly(sData, pw,
                                       xPoints, yPoints, off, numpts,
                                       clip, transx, transy, close);
            off += numpts;
        }
    }
}

class SetDrawPathANY extends DrawPath {
    SetDrawPathANY() {
        super(SurfaceType.AnyColor,
              CompositeType.SrcNoEa,
              SurfaceType.Any);
    }

    public void DrawPath(SunGraphics2D sg2d, SurfaceData sData,
                         int transx, int transy,
                         Path2D.Float p2df)
    {
        PixelWriter pw = GeneralRenderer.createSolidPixelWriter(sg2d, sData);
        ProcessPath.drawPath(
            new PixelWriterDrawHandler(sData, pw, sg2d.getCompClip(),
                                       sg2d.strokeHint),
            p2df, transx, transy
        );
    }
}

class SetDrawRectANY extends DrawRect {
    SetDrawRectANY() {
        super(SurfaceType.AnyColor,
              CompositeType.SrcNoEa,
              SurfaceType.Any);
    }

    public void DrawRect(SunGraphics2D sg2d, SurfaceData sData,
                         int x, int y, int w, int h)
    {
        PixelWriter pw = GeneralRenderer.createSolidPixelWriter(sg2d, sData);

        GeneralRenderer.doDrawRect(pw, sg2d, sData, x, y, w, h);
    }
}

class XorFillRectANY extends FillRect {
    XorFillRectANY() {
        super(SurfaceType.AnyColor,
              CompositeType.Xor,
              SurfaceType.Any);
    }

    public void FillRect(SunGraphics2D sg2d, SurfaceData sData,
                            int x, int y, int w, int h)
    {
        PixelWriter pw = GeneralRenderer.createXorPixelWriter(sg2d, sData);

        Region r = sg2d.getCompClip().getBoundsIntersectionXYWH(x, y, w, h);

        GeneralRenderer.doSetRect(sData, pw,
                                  r.getLoX(), r.getLoY(),
                                  r.getHiX(), r.getHiY());
    }
}

class XorFillPathANY extends FillPath {
    XorFillPathANY() {
        super(SurfaceType.AnyColor, CompositeType.Xor,
              SurfaceType.Any);
    }

    public void FillPath(SunGraphics2D sg2d, SurfaceData sData,
                         int transx, int transy,
                         Path2D.Float p2df)
    {
        PixelWriter pw = GeneralRenderer.createXorPixelWriter(sg2d, sData);
        ProcessPath.fillPath(
            new PixelWriterDrawHandler(sData, pw, sg2d.getCompClip(),
                                       sg2d.strokeHint),
            p2df, transx, transy);
    }
}

class XorFillSpansANY extends FillSpans {
    XorFillSpansANY() {
        super(SurfaceType.AnyColor,
              CompositeType.Xor,
              SurfaceType.Any);
    }

    public void FillSpans(SunGraphics2D sg2d, SurfaceData sData,
                          SpanIterator si)
    {
        PixelWriter pw = GeneralRenderer.createXorPixelWriter(sg2d, sData);

        int[] span = new int[4];
        while (si.nextSpan(span)) {
            GeneralRenderer.doSetRect(sData, pw,
                                      span[0], span[1], span[2], span[3]);
        }
    }
}

class XorDrawLineANY extends DrawLine {
    XorDrawLineANY() {
        super(SurfaceType.AnyColor,
              CompositeType.Xor,
              SurfaceType.Any);
    }

    public void DrawLine(SunGraphics2D sg2d, SurfaceData sData,
                         int x1, int y1, int x2, int y2)
    {
        PixelWriter pw = GeneralRenderer.createXorPixelWriter(sg2d, sData);

        if (y1 >= y2) {
            GeneralRenderer.doDrawLine(sData, pw, null,
                                       sg2d.getCompClip(),
                                       x2, y2, x1, y1);
        } else {
            GeneralRenderer.doDrawLine(sData, pw, null,
                                       sg2d.getCompClip(),
                                       x1, y1, x2, y2);
        }
    }
}

class XorDrawPolygonsANY extends DrawPolygons {
    XorDrawPolygonsANY() {
        super(SurfaceType.AnyColor,
              CompositeType.Xor,
              SurfaceType.Any);
    }

    public void DrawPolygons(SunGraphics2D sg2d, SurfaceData sData,
                             int[] xPoints, int[] yPoints,
                             int[] nPoints, int numPolys,
                             int transx, int transy,
                             boolean close)
    {
        PixelWriter pw = GeneralRenderer.createXorPixelWriter(sg2d, sData);

        int off = 0;
        Region clip = sg2d.getCompClip();
        for (int i = 0; i < numPolys; i++) {
            int numpts = nPoints[i];
            GeneralRenderer.doDrawPoly(sData, pw,
                                       xPoints, yPoints, off, numpts,
                                       clip, transx, transy, close);
            off += numpts;
        }
    }
}

class XorDrawPathANY extends DrawPath {
    XorDrawPathANY() {
        super(SurfaceType.AnyColor,
              CompositeType.Xor,
              SurfaceType.Any);
    }

    public void DrawPath(SunGraphics2D sg2d, SurfaceData sData,
                         int transx, int transy, Path2D.Float p2df)
    {
        PixelWriter pw = GeneralRenderer.createXorPixelWriter(sg2d, sData);
        ProcessPath.drawPath(
            new PixelWriterDrawHandler(sData, pw, sg2d.getCompClip(),
                                       sg2d.strokeHint),
            p2df, transx, transy
        );
    }
}

class XorDrawRectANY extends DrawRect {
    XorDrawRectANY() {
        super(SurfaceType.AnyColor,
              CompositeType.Xor,
              SurfaceType.Any);
    }

    public void DrawRect(SunGraphics2D sg2d, SurfaceData sData,
                         int x, int y, int w, int h)
    {
        PixelWriter pw = GeneralRenderer.createXorPixelWriter(sg2d, sData);

        GeneralRenderer.doDrawRect(pw, sg2d, sData, x, y, w, h);
    }
}

class XorDrawGlyphListANY extends DrawGlyphList {
    XorDrawGlyphListANY() {
        super(SurfaceType.AnyColor,
              CompositeType.Xor,
              SurfaceType.Any);
    }

    public void DrawGlyphList(SunGraphics2D sg2d, SurfaceData sData,
                              GlyphList gl, int fromGlyph, int toGlyph)
    {
        PixelWriter pw = GeneralRenderer.createXorPixelWriter(sg2d, sData);
        GeneralRenderer.doDrawGlyphList(sData, pw, gl, fromGlyph, toGlyph,
                sg2d.getCompClip());
    }
}

class XorDrawGlyphListAAANY extends DrawGlyphListAA {
    XorDrawGlyphListAAANY() {
        super(SurfaceType.AnyColor,
              CompositeType.Xor,
              SurfaceType.Any);
    }

    public void DrawGlyphListAA(SunGraphics2D sg2d, SurfaceData sData,
                                GlyphList gl, int fromGlyph, int toGlyph)
    {
        PixelWriter pw = GeneralRenderer.createXorPixelWriter(sg2d, sData);
        GeneralRenderer.doDrawGlyphList(sData, pw, gl, fromGlyph, toGlyph,
                sg2d.getCompClip());
    }
}

abstract class PixelWriter {
    protected WritableRaster dstRast;

    public void setRaster(WritableRaster dstRast) {
        this.dstRast = dstRast;
    }

    public abstract void writePixel(int x, int y);
}

class SolidPixelWriter extends PixelWriter {
    protected Object srcData;

    SolidPixelWriter(Object srcPixel) {
        this.srcData = srcPixel;
    }

    public void writePixel(int x, int y) {
        dstRast.setDataElements(x, y, srcData);
    }
}

abstract class XorPixelWriter extends PixelWriter {
    protected ColorModel dstCM;

    public void writePixel(int x, int y) {
        Object dstPixel = dstRast.getDataElements(x, y, null);
        xorPixel(dstPixel);
        dstRast.setDataElements(x, y, dstPixel);
    }

    protected abstract void xorPixel(Object pixData);

    public static class ByteData extends XorPixelWriter {
        byte[] xorData;

        ByteData(Object srcPixel, Object xorPixel) {
            this.xorData = (byte[]) srcPixel;
            xorPixel(xorPixel);
            this.xorData = (byte[]) xorPixel;
        }

        protected void xorPixel(Object pixData) {
            byte[] dstData = (byte[]) pixData;
            for (int i = 0; i < dstData.length; i++) {
                dstData[i] ^= xorData[i];
            }
        }
    }

    public static class ShortData extends XorPixelWriter {
        short[] xorData;

        ShortData(Object srcPixel, Object xorPixel) {
            this.xorData = (short[]) srcPixel;
            xorPixel(xorPixel);
            this.xorData = (short[]) xorPixel;
        }

        protected void xorPixel(Object pixData) {
            short[] dstData = (short[]) pixData;
            for (int i = 0; i < dstData.length; i++) {
                dstData[i] ^= xorData[i];
            }
        }
    }

    public static class IntData extends XorPixelWriter {
        int[] xorData;

        IntData(Object srcPixel, Object xorPixel) {
            this.xorData = (int[]) srcPixel;
            xorPixel(xorPixel);
            this.xorData = (int[]) xorPixel;
        }

        protected void xorPixel(Object pixData) {
            int[] dstData = (int[]) pixData;
            for (int i = 0; i < dstData.length; i++) {
                dstData[i] ^= xorData[i];
            }
        }
    }

    public static class FloatData extends XorPixelWriter {
        int[] xorData;

        FloatData(Object srcPixel, Object xorPixel) {
            float[] srcData = (float[]) srcPixel;
            float[] xorData = (float[]) xorPixel;
            this.xorData = new int[srcData.length];
            for (int i = 0; i < srcData.length; i++) {
                this.xorData[i] = (Float.floatToIntBits(srcData[i]) ^
                                   Float.floatToIntBits(xorData[i]));
            }
        }

        protected void xorPixel(Object pixData) {
            float[] dstData = (float[]) pixData;
            for (int i = 0; i < dstData.length; i++) {
                int v = Float.floatToIntBits(dstData[i]) ^ xorData[i];
                dstData[i] = Float.intBitsToFloat(v);
            }
        }
    }

    public static class DoubleData extends XorPixelWriter {
        long[] xorData;

        DoubleData(Object srcPixel, Object xorPixel) {
            double[] srcData = (double[]) srcPixel;
            double[] xorData = (double[]) xorPixel;
            this.xorData = new long[srcData.length];
            for (int i = 0; i < srcData.length; i++) {
                this.xorData[i] = (Double.doubleToLongBits(srcData[i]) ^
                                   Double.doubleToLongBits(xorData[i]));
            }
        }

        protected void xorPixel(Object pixData) {
            double[] dstData = (double[]) pixData;
            for (int i = 0; i < dstData.length; i++) {
                long v = Double.doubleToLongBits(dstData[i]) ^ xorData[i];
                dstData[i] = Double.longBitsToDouble(v);
            }
        }
    }
}
