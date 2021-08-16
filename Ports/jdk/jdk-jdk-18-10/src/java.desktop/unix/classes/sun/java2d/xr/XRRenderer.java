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
import java.awt.geom.*;
import sun.awt.SunToolkit;
import sun.java2d.InvalidPipeException;
import sun.java2d.SunGraphics2D;
import sun.java2d.loops.*;
import sun.java2d.pipe.Region;
import sun.java2d.pipe.PixelDrawPipe;
import sun.java2d.pipe.PixelFillPipe;
import sun.java2d.pipe.ShapeDrawPipe;
import sun.java2d.pipe.SpanIterator;
import sun.java2d.pipe.ShapeSpanIterator;
import sun.java2d.pipe.LoopPipe;

import static sun.java2d.xr.XRUtils.clampToShort;
import static sun.java2d.xr.XRUtils.clampToUShort;

/**
 * XRender provides only accalerated rectangles. To emulate higher "order"
 *  geometry we have to pass everything else to DoPath/FillSpans.
 *
 * TODO: DrawRect could be instrified
 *
 * @author Clemens Eisserer
 */

public class XRRenderer implements PixelDrawPipe, PixelFillPipe, ShapeDrawPipe {
    XRDrawHandler drawHandler;
    MaskTileManager tileManager;
    XRDrawLine lineGen;
    GrowableRectArray rectBuffer;

    public XRRenderer(MaskTileManager tileManager) {
        this.tileManager = tileManager;
        this.rectBuffer = tileManager.getMainTile().getRects();

        this.drawHandler = new XRDrawHandler();
        this.lineGen = new XRDrawLine();
    }

    /**
     * Common validate method, used by all XRRender functions to validate the
     * destination context.
     */
    private void validateSurface(SunGraphics2D sg2d) {
        XRSurfaceData xrsd;
        try {
            xrsd = (XRSurfaceData) sg2d.surfaceData;
        } catch (ClassCastException e) {
            throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
        }
        xrsd.validateAsDestination(sg2d, sg2d.getCompClip());
        xrsd.maskBuffer.validateCompositeState(sg2d.composite, sg2d.transform,
                                               sg2d.paint, sg2d);
    }

    public void drawLine(SunGraphics2D sg2d, int x1, int y1, int x2, int y2) {
        Region compClip = sg2d.getCompClip();
        int transX1 = Region.clipAdd(x1, sg2d.transX);
        int transY1 = Region.clipAdd(y1, sg2d.transY);
        int transX2 = Region.clipAdd(x2, sg2d.transX);
        int transY2 = Region.clipAdd(y2, sg2d.transY);

        SunToolkit.awtLock();
        try {
            validateSurface(sg2d);
            lineGen.rasterizeLine(rectBuffer, transX1, transY1,
                    transX2, transY2, compClip.getLoX(), compClip.getLoY(),
                    compClip.getHiX(), compClip.getHiY(), true, true);
            tileManager.fillMask((XRSurfaceData) sg2d.surfaceData);
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    public void drawRect(SunGraphics2D sg2d,
                         int x, int y, int width, int height) {
        draw(sg2d, new Rectangle2D.Float(x, y, width, height));
    }

    public void drawPolyline(SunGraphics2D sg2d,
                             int[] xpoints, int[] ypoints, int npoints) {
        Path2D.Float p2d = new Path2D.Float();
        if (npoints > 1) {
            p2d.moveTo(xpoints[0], ypoints[0]);
            for (int i = 1; i < npoints; i++) {
                p2d.lineTo(xpoints[i], ypoints[i]);
            }
        }

        draw(sg2d, p2d);
    }

    public void drawPolygon(SunGraphics2D sg2d,
                            int[] xpoints, int[] ypoints, int npoints) {
        draw(sg2d, new Polygon(xpoints, ypoints, npoints));
    }

    public void fillRect(SunGraphics2D sg2d, int x, int y, int width, int height) {
        x = Region.clipAdd(x, sg2d.transX);
        y = Region.clipAdd(y, sg2d.transY);

        /*
         * Limit x/y to signed short, width/height to unsigned short,
         * to match the X11 coordinate limits for rectangles.
         * Correct width/height in case x/y have been modified by clipping.
         */
        if (x > Short.MAX_VALUE || y > Short.MAX_VALUE) {
            return;
        }

        int x2 = Region.dimAdd(x, width);
        int y2 = Region.dimAdd(y, height);

        if (x2 < Short.MIN_VALUE || y2 < Short.MIN_VALUE) {
            return;
        }

        x = clampToShort(x);
        y = clampToShort(y);
        width = clampToUShort(x2 - x);
        height = clampToUShort(y2 - y);

        if (width == 0 || height == 0) {
            return;
        }

        SunToolkit.awtLock();
        try {
            validateSurface(sg2d);
            rectBuffer.pushRectValues(x, y, width, height);
            tileManager.fillMask((XRSurfaceData) sg2d.surfaceData);
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    public void fillPolygon(SunGraphics2D sg2d,
                            int[] xpoints, int[] ypoints, int npoints) {
        fill(sg2d, new Polygon(xpoints, ypoints, npoints));
    }

    public void drawRoundRect(SunGraphics2D sg2d,
                              int x, int y, int width, int height,
                              int arcWidth, int arcHeight) {
        draw(sg2d, new RoundRectangle2D.Float(x, y, width, height,
                                              arcWidth, arcHeight));
    }

    public void fillRoundRect(SunGraphics2D sg2d, int x, int y,
                              int width, int height,
                              int arcWidth, int arcHeight) {
        fill(sg2d, new RoundRectangle2D.Float(x, y, width, height,
                                              arcWidth, arcHeight));
    }

    public void drawOval(SunGraphics2D sg2d,
                         int x, int y, int width, int height) {
        draw(sg2d, new Ellipse2D.Float(x, y, width, height));
    }

    public void fillOval(SunGraphics2D sg2d,
                         int x, int y, int width, int height) {
        fill(sg2d, new Ellipse2D.Float(x, y, width, height));
    }

    public void drawArc(SunGraphics2D sg2d,
                       int x, int y, int width, int height,
                        int startAngle, int arcAngle) {
        draw(sg2d, new Arc2D.Float(x, y, width, height,
                                   startAngle, arcAngle, Arc2D.OPEN));
    }

    public void fillArc(SunGraphics2D sg2d,
                         int x, int y, int width, int height,
                         int startAngle, int arcAngle) {
        fill(sg2d, new Arc2D.Float(x, y, width, height,
             startAngle, arcAngle, Arc2D.PIE));
    }

    private class XRDrawHandler extends ProcessPath.DrawHandler {
        DirtyRegion region;

        XRDrawHandler() {
            // these are bogus values; the caller will use validate()
            // to ensure that they are set properly prior to each usage
            super(0, 0, 0, 0);
            this.region = new DirtyRegion();
        }

        /**
         * This method needs to be called prior to each draw/fillPath()
         * operation to ensure the clip bounds are up to date.
         */
        void validate(SunGraphics2D sg2d) {
            Region clip = sg2d.getCompClip();
            setBounds(clip.getLoX(), clip.getLoY(),
                      clip.getHiX(), clip.getHiY(), sg2d.strokeHint);
            validateSurface(sg2d);
        }

        public void drawLine(int x1, int y1, int x2, int y2) {
            region.setDirtyLineRegion(x1, y1, x2, y2);
            int xDiff = region.x2 - region.x;
            int yDiff = region.y2 - region.y;

            if (xDiff == 0 || yDiff == 0) {
                // horizontal / diagonal lines can be represented by a single
                // rectangle
                rectBuffer.pushRectValues(region.x, region.y, region.x2 - region.x
                        + 1, region.y2 - region.y + 1);
            } else if (xDiff == 1 && yDiff == 1) {
                // fast path for pattern commonly generated by
                // ProcessPath.DrawHandler
                rectBuffer.pushRectValues(x1, y1, 1, 1);
                rectBuffer.pushRectValues(x2, y2, 1, 1);
            } else {
                lineGen.rasterizeLine(rectBuffer, x1, y1, x2, y2, 0, 0,
                                      0, 0, false, false);
            }
        }

        public void drawPixel(int x, int y) {
            rectBuffer.pushRectValues(x, y, 1, 1);
        }

        public void drawScanline(int x1, int x2, int y) {
            rectBuffer.pushRectValues(x1, y, x2 - x1 + 1, 1);
        }
    }

    protected void drawPath(SunGraphics2D sg2d, Path2D.Float p2df,
                            int transx, int transy) {
        SunToolkit.awtLock();
        try {
            validateSurface(sg2d);
            drawHandler.validate(sg2d);
            ProcessPath.drawPath(drawHandler, p2df, transx, transy);
            tileManager.fillMask(((XRSurfaceData) sg2d.surfaceData));
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    protected void fillPath(SunGraphics2D sg2d, Path2D.Float p2df,
                            int transx, int transy) {
        SunToolkit.awtLock();
        try {
            validateSurface(sg2d);
            drawHandler.validate(sg2d);
            ProcessPath.fillPath(drawHandler, p2df, transx, transy);
            tileManager.fillMask(((XRSurfaceData) sg2d.surfaceData));
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    protected void fillSpans(SunGraphics2D sg2d, SpanIterator si,
                             int transx, int transy) {
        SunToolkit.awtLock();
        try {
            validateSurface(sg2d);
            int[] spanBox = new int[4];
            while (si.nextSpan(spanBox)) {
                rectBuffer.pushRectValues(spanBox[0] + transx,
                                    spanBox[1] + transy,
                                    spanBox[2] - spanBox[0],
                                    spanBox[3] - spanBox[1]);
            }
            tileManager.fillMask(((XRSurfaceData) sg2d.surfaceData));
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    public void draw(SunGraphics2D sg2d, Shape s) {
        if (sg2d.strokeState == SunGraphics2D.STROKE_THIN) {
            Path2D.Float p2df;
            int transx, transy;
            if (sg2d.transformState <= SunGraphics2D.TRANSFORM_INT_TRANSLATE) {
                if (s instanceof Path2D.Float) {
                    p2df = (Path2D.Float) s;
                } else {
                    p2df = new Path2D.Float(s);
                }
                transx = sg2d.transX;
                transy = sg2d.transY;
            } else {
                p2df = new Path2D.Float(s, sg2d.transform);
                transx = 0;
                transy = 0;
            }
            drawPath(sg2d, p2df, transx, transy);
        } else if (sg2d.strokeState < SunGraphics2D.STROKE_CUSTOM) {
            ShapeSpanIterator si = LoopPipe.getStrokeSpans(sg2d, s);
            try {
                fillSpans(sg2d, si, 0, 0);
            } finally {
                si.dispose();
            }
        } else {
            fill(sg2d, sg2d.stroke.createStrokedShape(s));
        }
    }

    public void fill(SunGraphics2D sg2d, Shape s) {
        int transx, transy;

        if (sg2d.strokeState == SunGraphics2D.STROKE_THIN) {
            // Here we are able to use fillPath() for
            // high-quality fills.
            Path2D.Float p2df;
            if (sg2d.transformState <= SunGraphics2D.TRANSFORM_INT_TRANSLATE) {
                if (s instanceof Path2D.Float) {
                    p2df = (Path2D.Float) s;
                } else {
                    p2df = new Path2D.Float(s);
                }
                transx = sg2d.transX;
                transy = sg2d.transY;
            } else {
                p2df = new Path2D.Float(s, sg2d.transform);
                transx = 0;
                transy = 0;
            }
            fillPath(sg2d, p2df, transx, transy);
            return;
        }

        AffineTransform at;
        if (sg2d.transformState <= SunGraphics2D.TRANSFORM_INT_TRANSLATE) {
            // Transform (translation) will be done by FillSpans
            at = null;
            transx = sg2d.transX;
            transy = sg2d.transY;
        } else {
            // Transform will be done by the PathIterator
            at = sg2d.transform;
            transx = transy = 0;
        }

        ShapeSpanIterator ssi = LoopPipe.getFillSSI(sg2d);
        try {
            // Subtract transx/y from the SSI clip to match the
            // (potentially untranslated) geometry fed to it
            Region clip = sg2d.getCompClip();
            ssi.setOutputAreaXYXY(clip.getLoX() - transx,
                                  clip.getLoY() - transy,
                                  clip.getHiX() - transx,
                                  clip.getHiY() - transy);
            ssi.appendPath(s.getPathIterator(at));
            fillSpans(sg2d, ssi, transx, transy);
        } finally {
            ssi.dispose();
        }
    }
}
