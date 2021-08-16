/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.windows;

import java.awt.Composite;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import sun.java2d.InvalidPipeException;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.pipe.Region;
import sun.java2d.pipe.PixelDrawPipe;
import sun.java2d.pipe.PixelFillPipe;
import sun.java2d.pipe.ShapeDrawPipe;
import sun.java2d.pipe.SpanIterator;
import sun.java2d.pipe.ShapeSpanIterator;
import sun.java2d.pipe.LoopPipe;
import sun.java2d.loops.GraphicsPrimitive;

public class GDIRenderer implements
    PixelDrawPipe,
    PixelFillPipe,
    ShapeDrawPipe
{
    native void doDrawLine(GDIWindowSurfaceData sData,
                           Region clip, Composite comp, int color,
                           int x1, int y1, int x2, int y2);

    public void drawLine(SunGraphics2D sg2d,
                         int x1, int y1, int x2, int y2)
    {
        int transx = sg2d.transX;
        int transy = sg2d.transY;
        try {
            doDrawLine((GDIWindowSurfaceData)sg2d.surfaceData,
                       sg2d.getCompClip(), sg2d.composite, sg2d.eargb,
                       x1+transx, y1+transy, x2+transx, y2+transy);
        } catch (ClassCastException e) {
            throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
        }
    }

    native void doDrawRect(GDIWindowSurfaceData sData,
                           Region clip, Composite comp, int color,
                           int x, int y, int w, int h);

    public void drawRect(SunGraphics2D sg2d,
                         int x, int y, int width, int height)
    {
        try {
            doDrawRect((GDIWindowSurfaceData)sg2d.surfaceData,
                       sg2d.getCompClip(), sg2d.composite, sg2d.eargb,
                       x+sg2d.transX, y+sg2d.transY, width, height);
        } catch (ClassCastException e) {
            throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
        }
    }

    native void doDrawRoundRect(GDIWindowSurfaceData sData,
                                Region clip, Composite comp, int color,
                                int x, int y, int w, int h,
                                int arcW, int arcH);

    public void drawRoundRect(SunGraphics2D sg2d,
                              int x, int y, int width, int height,
                              int arcWidth, int arcHeight)
    {
        try {
            doDrawRoundRect((GDIWindowSurfaceData)sg2d.surfaceData,
                            sg2d.getCompClip(), sg2d.composite, sg2d.eargb,
                            x+sg2d.transX, y+sg2d.transY, width, height,
                            arcWidth, arcHeight);
        } catch (ClassCastException e) {
            throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
        }
    }

    native void doDrawOval(GDIWindowSurfaceData sData,
                           Region clip, Composite comp, int color,
                           int x, int y, int w, int h);

    public void drawOval(SunGraphics2D sg2d,
                         int x, int y, int width, int height)
    {
        try {
            doDrawOval((GDIWindowSurfaceData)sg2d.surfaceData,
                       sg2d.getCompClip(), sg2d.composite, sg2d.eargb,
                       x+sg2d.transX, y+sg2d.transY, width, height);
        } catch (ClassCastException e) {
            throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
        }
    }

    native void doDrawArc(GDIWindowSurfaceData sData,
                          Region clip, Composite comp, int color,
                          int x, int y, int w, int h,
                          int angleStart, int angleExtent);

    public void drawArc(SunGraphics2D sg2d,
                        int x, int y, int width, int height,
                        int startAngle, int arcAngle)
    {
        try {
            doDrawArc((GDIWindowSurfaceData)sg2d.surfaceData,
                      sg2d.getCompClip(), sg2d.composite, sg2d.eargb,
                      x+sg2d.transX, y+sg2d.transY, width, height,
                      startAngle, arcAngle);
        } catch (ClassCastException e) {
            throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
        }
    }

    native void doDrawPoly(GDIWindowSurfaceData sData,
                           Region clip, Composite comp, int color,
                           int transx, int transy,
                           int[] xpoints, int[] ypoints,
                           int npoints, boolean isclosed);

    public void drawPolyline(SunGraphics2D sg2d,
                             int[] xpoints, int[] ypoints,
                             int npoints)
    {
        try {
            doDrawPoly((GDIWindowSurfaceData)sg2d.surfaceData,
                       sg2d.getCompClip(), sg2d.composite, sg2d.eargb,
                       sg2d.transX, sg2d.transY, xpoints, ypoints, npoints, false);
        } catch (ClassCastException e) {
            throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
        }
    }

    public void drawPolygon(SunGraphics2D sg2d,
                            int[] xpoints, int[] ypoints,
                            int npoints)
    {
        try {
            doDrawPoly((GDIWindowSurfaceData)sg2d.surfaceData,
                       sg2d.getCompClip(), sg2d.composite, sg2d.eargb,
                       sg2d.transX, sg2d.transY, xpoints, ypoints, npoints, true);
        } catch (ClassCastException e) {
            throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
        }
    }

    native void doFillRect(GDIWindowSurfaceData sData,
                           Region clip, Composite comp, int color,
                           int x, int y, int w, int h);

    public void fillRect(SunGraphics2D sg2d,
                         int x, int y, int width, int height)
    {
        try {
            doFillRect((GDIWindowSurfaceData)sg2d.surfaceData,
                       sg2d.getCompClip(), sg2d.composite, sg2d.eargb,
                       x+sg2d.transX, y+sg2d.transY, width, height);
        } catch (ClassCastException e) {
            throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
        }
    }

    native void doFillRoundRect(GDIWindowSurfaceData sData,
                                Region clip, Composite comp, int color,
                                int x, int y, int w, int h,
                                int arcW, int arcH);

    public void fillRoundRect(SunGraphics2D sg2d,
                              int x, int y, int width, int height,
                              int arcWidth, int arcHeight)
    {
        try {
            doFillRoundRect((GDIWindowSurfaceData)sg2d.surfaceData,
                            sg2d.getCompClip(), sg2d.composite, sg2d.eargb,
                            x+sg2d.transX, y+sg2d.transY, width, height,
                            arcWidth, arcHeight);
        } catch (ClassCastException e) {
            throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
        }
    }

    native void doFillOval(GDIWindowSurfaceData sData,
                           Region clip, Composite comp, int color,
                           int x, int y, int w, int h);

    public void fillOval(SunGraphics2D sg2d,
                         int x, int y, int width, int height)
    {
        try {
            doFillOval((GDIWindowSurfaceData)sg2d.surfaceData,
                       sg2d.getCompClip(), sg2d.composite, sg2d.eargb,
                       x+sg2d.transX, y+sg2d.transY, width, height);
        } catch (ClassCastException e) {
            throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
        }
    }

    native void doFillArc(GDIWindowSurfaceData sData,
                          Region clip, Composite comp, int color,
                          int x, int y, int w, int h,
                          int angleStart, int angleExtent);

    public void fillArc(SunGraphics2D sg2d,
                        int x, int y, int width, int height,
                        int startAngle, int arcAngle)
    {
        try {
            doFillArc((GDIWindowSurfaceData)sg2d.surfaceData,
                      sg2d.getCompClip(), sg2d.composite, sg2d.eargb,
                      x+sg2d.transX, y+sg2d.transY, width, height,
                      startAngle, arcAngle);
        } catch (ClassCastException e) {
            throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
        }
    }

    native void doFillPoly(GDIWindowSurfaceData sData,
                           Region clip, Composite comp, int color,
                           int transx, int transy,
                           int[] xpoints, int[] ypoints,
                           int npoints);

    public void fillPolygon(SunGraphics2D sg2d,
                            int[] xpoints, int[] ypoints,
                            int npoints)
    {
        try {
            doFillPoly((GDIWindowSurfaceData)sg2d.surfaceData,
                       sg2d.getCompClip(), sg2d.composite, sg2d.eargb,
                       sg2d.transX, sg2d.transY, xpoints, ypoints, npoints);
        } catch (ClassCastException e) {
            throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
        }
    }

    native void doShape(GDIWindowSurfaceData sData,
                        Region clip, Composite comp, int color,
                        int transX, int transY,
                        Path2D.Float p2df, boolean isfill);

    void doShape(SunGraphics2D sg2d, Shape s, boolean isfill) {
        Path2D.Float p2df;
        int transX;
        int transY;
        if (sg2d.transformState <= SunGraphics2D.TRANSFORM_INT_TRANSLATE) {
            if (s instanceof Path2D.Float) {
                p2df = (Path2D.Float)s;
            } else {
                p2df = new Path2D.Float(s);
            }
            transX = sg2d.transX;
            transY = sg2d.transY;
        } else {
            p2df = new Path2D.Float(s, sg2d.transform);
            transX = 0;
            transY = 0;
        }
        try {
            doShape((GDIWindowSurfaceData)sg2d.surfaceData,
                    sg2d.getCompClip(), sg2d.composite, sg2d.eargb,
                    transX, transY, p2df, isfill);
        } catch (ClassCastException e) {
            throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
        }
    }

    // REMIND: This is just a hack to get WIDE lines to honor the
    // necessary hinted pixelization rules.  This should be replaced
    // by a native FillSpans method or a getHintedStrokeGeneralPath()
    // method that could be filled by the doShape method more quickly.
    public void doFillSpans(SunGraphics2D sg2d, SpanIterator si) {
        int[] box = new int[4];
        GDIWindowSurfaceData sd;
        try {
            sd = (GDIWindowSurfaceData)sg2d.surfaceData;
        } catch (ClassCastException e) {
            throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
        }
        Region clip = sg2d.getCompClip();
        Composite comp = sg2d.composite;
        int eargb = sg2d.eargb;
        while (si.nextSpan(box)) {
            doFillRect(sd, clip, comp, eargb,
                       box[0], box[1], box[2]-box[0], box[3]-box[1]);
        }
    }

    public void draw(SunGraphics2D sg2d, Shape s) {
        if (sg2d.strokeState == SunGraphics2D.STROKE_THIN) {
            doShape(sg2d, s, false);
        } else if (sg2d.strokeState < SunGraphics2D.STROKE_CUSTOM) {
            ShapeSpanIterator si = LoopPipe.getStrokeSpans(sg2d, s);
            try {
                doFillSpans(sg2d, si);
            } finally {
                si.dispose();
            }
        } else {
            doShape(sg2d, sg2d.stroke.createStrokedShape(s), true);
        }
    }

    public void fill(SunGraphics2D sg2d, Shape s) {
        doShape(sg2d, s, true);
    }

    public native void devCopyArea(GDIWindowSurfaceData sData,
                                   int srcx, int srcy,
                                   int dx, int dy,
                                   int w, int h);

    public GDIRenderer traceWrap() {
        return new Tracer();
    }

    public static class Tracer extends GDIRenderer {
        void doDrawLine(GDIWindowSurfaceData sData,
                        Region clip, Composite comp, int color,
                        int x1, int y1, int x2, int y2)
        {
            GraphicsPrimitive.tracePrimitive("GDIDrawLine");
            super.doDrawLine(sData, clip, comp, color, x1, y1, x2, y2);
        }
        void doDrawRect(GDIWindowSurfaceData sData,
                        Region clip, Composite comp, int color,
                        int x, int y, int w, int h)
        {
            GraphicsPrimitive.tracePrimitive("GDIDrawRect");
            super.doDrawRect(sData, clip, comp, color, x, y, w, h);
        }
        void doDrawRoundRect(GDIWindowSurfaceData sData,
                             Region clip, Composite comp, int color,
                             int x, int y, int w, int h,
                             int arcW, int arcH)
        {
            GraphicsPrimitive.tracePrimitive("GDIDrawRoundRect");
            super.doDrawRoundRect(sData, clip, comp, color,
                                  x, y, w, h, arcW, arcH);
        }
        void doDrawOval(GDIWindowSurfaceData sData,
                        Region clip, Composite comp, int color,
                        int x, int y, int w, int h)
        {
            GraphicsPrimitive.tracePrimitive("GDIDrawOval");
            super.doDrawOval(sData, clip, comp, color, x, y, w, h);
        }
        void doDrawArc(GDIWindowSurfaceData sData,
                       Region clip, Composite comp, int color,
                       int x, int y, int w, int h,
                       int angleStart, int angleExtent)
        {
            GraphicsPrimitive.tracePrimitive("GDIDrawArc");
            super.doDrawArc(sData, clip, comp, color, x, y, w, h,
                            angleStart, angleExtent);
        }
        void doDrawPoly(GDIWindowSurfaceData sData,
                        Region clip, Composite comp, int color,
                        int transx, int transy,
                        int[] xpoints, int[] ypoints,
                        int npoints, boolean isclosed)
        {
            GraphicsPrimitive.tracePrimitive("GDIDrawPoly");
            super.doDrawPoly(sData, clip, comp, color, transx, transy,
                             xpoints, ypoints, npoints, isclosed);
        }
        void doFillRect(GDIWindowSurfaceData sData,
                        Region clip, Composite comp, int color,
                        int x, int y, int w, int h)
        {
            GraphicsPrimitive.tracePrimitive("GDIFillRect");
            super.doFillRect(sData, clip, comp, color, x, y, w, h);
        }
        void doFillRoundRect(GDIWindowSurfaceData sData,
                             Region clip, Composite comp, int color,
                             int x, int y, int w, int h,
                             int arcW, int arcH)
        {
            GraphicsPrimitive.tracePrimitive("GDIFillRoundRect");
            super.doFillRoundRect(sData, clip, comp, color,
                                  x, y, w, h, arcW, arcH);
        }
        void doFillOval(GDIWindowSurfaceData sData,
                        Region clip, Composite comp, int color,
                        int x, int y, int w, int h)
        {
            GraphicsPrimitive.tracePrimitive("GDIFillOval");
            super.doFillOval(sData, clip, comp, color, x, y, w, h);
        }
        void doFillArc(GDIWindowSurfaceData sData,
                       Region clip, Composite comp, int color,
                       int x, int y, int w, int h,
                       int angleStart, int angleExtent)
        {
            GraphicsPrimitive.tracePrimitive("GDIFillArc");
            super.doFillArc(sData, clip, comp, color, x, y, w, h,
                            angleStart, angleExtent);
        }
        void doFillPoly(GDIWindowSurfaceData sData,
                        Region clip, Composite comp, int color,
                        int transx, int transy,
                        int[] xpoints, int[] ypoints,
                        int npoints)
        {
            GraphicsPrimitive.tracePrimitive("GDIFillPoly");
            super.doFillPoly(sData, clip, comp, color, transx, transy,
                             xpoints, ypoints, npoints);
        }
        void doShape(GDIWindowSurfaceData sData,
                     Region clip, Composite comp, int color,
                     int transX, int transY,
                     Path2D.Float p2df, boolean isfill)
        {
            GraphicsPrimitive.tracePrimitive(isfill
                                             ? "GDIFillShape"
                                             : "GDIDrawShape");
            super.doShape(sData, clip, comp, color,
                          transX, transY, p2df, isfill);
        }
        public void devCopyArea(GDIWindowSurfaceData sData,
                                int srcx, int srcy,
                                int dx, int dy,
                                int w, int h)
        {
            GraphicsPrimitive.tracePrimitive("GDICopyArea");
            super.devCopyArea(sData, srcx, srcy, dx, dy, w, h);
        }
    }
}
