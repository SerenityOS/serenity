/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.x11;

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Path2D;
import java.awt.geom.IllegalPathStateException;
import sun.awt.SunToolkit;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.loops.GraphicsPrimitive;
import sun.java2d.pipe.Region;
import sun.java2d.pipe.PixelDrawPipe;
import sun.java2d.pipe.PixelFillPipe;
import sun.java2d.pipe.ShapeDrawPipe;
import sun.java2d.pipe.SpanIterator;
import sun.java2d.pipe.ShapeSpanIterator;
import sun.java2d.pipe.LoopPipe;

public class X11Renderer implements
    PixelDrawPipe,
    PixelFillPipe,
    ShapeDrawPipe
{
    public static X11Renderer getInstance() {
        return (GraphicsPrimitive.tracingEnabled()
                ? new X11TracingRenderer()
                : new X11Renderer());
    }

    private long validate(SunGraphics2D sg2d) {
        // NOTE: getCompClip() will revalidateAll() if the
        // surfaceData is invalid.  This should ensure that
        // the clip and pixel that we are validating against
        // are the most current.
        //
        // The assumption is that the pipeline after that
        // revalidation will either be another X11 pipe
        // (because the drawable format never changes on X11)
        // or a null pipeline if the surface is disposed.
        //
        // Since we do not get the ops structure of the SurfaceData
        // until the actual call down to the native level we will
        // pick up the most recently validated copy.
        // Note that if the surface is disposed, a NullSurfaceData
        // (with null native data structure) will be set in
        // sg2d, so we have to protect against it in native code.

        X11SurfaceData x11sd = (X11SurfaceData)sg2d.surfaceData;
        return x11sd.getRenderGC(sg2d.getCompClip(),
                                 sg2d.compositeState, sg2d.composite,
                                 sg2d.pixel);
    }

    native void XDrawLine(long pXSData, long xgc,
                          int x1, int y1, int x2, int y2);

    public void drawLine(SunGraphics2D sg2d, int x1, int y1, int x2, int y2) {
        SunToolkit.awtLock();
        try {
            long xgc = validate(sg2d);
            int transx = sg2d.transX;
            int transy = sg2d.transY;
            XDrawLine(sg2d.surfaceData.getNativeOps(), xgc,
                      x1+transx, y1+transy, x2+transx, y2+transy);
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    native void XDrawRect(long pXSData, long xgc,
                          int x, int y, int w, int h);

    public void drawRect(SunGraphics2D sg2d,
                         int x, int y, int width, int height)
    {
        SunToolkit.awtLock();
        try {
            long xgc = validate(sg2d);
            XDrawRect(sg2d.surfaceData.getNativeOps(), xgc,
                      x+sg2d.transX, y+sg2d.transY, width, height);
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    native void XDrawRoundRect(long pXSData, long xgc,
                               int x, int y, int w, int h,
                               int arcW, int arcH);

    public void drawRoundRect(SunGraphics2D sg2d,
                              int x, int y, int width, int height,
                              int arcWidth, int arcHeight)
    {
        SunToolkit.awtLock();
        try {
            long xgc = validate(sg2d);
            XDrawRoundRect(sg2d.surfaceData.getNativeOps(), xgc,
                           x+sg2d.transX, y+sg2d.transY, width, height,
                           arcWidth, arcHeight);
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    native void XDrawOval(long pXSData, long xgc,
                          int x, int y, int w, int h);

    public void drawOval(SunGraphics2D sg2d,
                         int x, int y, int width, int height)
    {
        SunToolkit.awtLock();
        try {
            long xgc = validate(sg2d);
            XDrawOval(sg2d.surfaceData.getNativeOps(), xgc,
                      x+sg2d.transX, y+sg2d.transY, width, height);
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    native void XDrawArc(long pXSData, long xgc,
                         int x, int y, int w, int h,
                         int angleStart, int angleExtent);

    public void drawArc(SunGraphics2D sg2d,
                        int x, int y, int width, int height,
                        int startAngle, int arcAngle)
    {
        SunToolkit.awtLock();
        try {
            long xgc = validate(sg2d);
            XDrawArc(sg2d.surfaceData.getNativeOps(), xgc,
                     x+sg2d.transX, y+sg2d.transY, width, height,
                     startAngle, arcAngle);
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    native void XDrawPoly(long pXSData, long xgc,
                          int transx, int transy,
                          int[] xpoints, int[] ypoints,
                          int npoints, boolean isclosed);

    public void drawPolyline(SunGraphics2D sg2d,
                             int[] xpoints, int[] ypoints,
                             int npoints)
    {
        SunToolkit.awtLock();
        try {
            long xgc = validate(sg2d);
            XDrawPoly(sg2d.surfaceData.getNativeOps(), xgc,
                      sg2d.transX, sg2d.transY,
                      xpoints, ypoints, npoints, false);
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    public void drawPolygon(SunGraphics2D sg2d,
                            int[] xpoints, int[] ypoints,
                            int npoints)
    {
        SunToolkit.awtLock();
        try {
            long xgc = validate(sg2d);
            XDrawPoly(sg2d.surfaceData.getNativeOps(), xgc,
                      sg2d.transX, sg2d.transY,
                      xpoints, ypoints, npoints, true);
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    native void XFillRect(long pXSData, long xgc,
                          int x, int y, int w, int h);

    public void fillRect(SunGraphics2D sg2d,
                         int x, int y, int width, int height)
    {
        SunToolkit.awtLock();
        try {
            long xgc = validate(sg2d);
            XFillRect(sg2d.surfaceData.getNativeOps(), xgc,
                      x+sg2d.transX, y+sg2d.transY, width, height);
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    native void XFillRoundRect(long pXSData, long xgc,
                               int x, int y, int w, int h,
                               int arcW, int arcH);

    public void fillRoundRect(SunGraphics2D sg2d,
                              int x, int y, int width, int height,
                              int arcWidth, int arcHeight)
    {
        SunToolkit.awtLock();
        try {
            long xgc = validate(sg2d);
            XFillRoundRect(sg2d.surfaceData.getNativeOps(), xgc,
                           x+sg2d.transX, y+sg2d.transY, width, height,
                           arcWidth, arcHeight);
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    native void XFillOval(long pXSData, long xgc,
                          int x, int y, int w, int h);

    public void fillOval(SunGraphics2D sg2d,
                         int x, int y, int width, int height)
    {
        SunToolkit.awtLock();
        try {
            long xgc = validate(sg2d);
            XFillOval(sg2d.surfaceData.getNativeOps(), xgc,
                      x+sg2d.transX, y+sg2d.transY, width, height);
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    native void XFillArc(long pXSData, long xgc,
                         int x, int y, int w, int h,
                         int angleStart, int angleExtent);

    public void fillArc(SunGraphics2D sg2d,
                        int x, int y, int width, int height,
                        int startAngle, int arcAngle)
    {
        SunToolkit.awtLock();
        try {
            long xgc = validate(sg2d);
            XFillArc(sg2d.surfaceData.getNativeOps(), xgc,
                     x+sg2d.transX, y+sg2d.transY, width, height,
                     startAngle, arcAngle);
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    native void XFillPoly(long pXSData, long xgc,
                          int transx, int transy,
                          int[] xpoints, int[] ypoints,
                          int npoints);

    public void fillPolygon(SunGraphics2D sg2d,
                            int[] xpoints, int[] ypoints,
                            int npoints)
    {
        SunToolkit.awtLock();
        try {
            long xgc = validate(sg2d);
            XFillPoly(sg2d.surfaceData.getNativeOps(), xgc,
                      sg2d.transX, sg2d.transY, xpoints, ypoints, npoints);
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    native void XFillSpans(long pXSData, long xgc,
                           SpanIterator si, long iterator,
                           int transx, int transy);

    native void XDoPath(SunGraphics2D sg2d, long pXSData, long xgc,
                        int transX, int transY, Path2D.Float p2df,
                        boolean isFill);

    private void doPath(SunGraphics2D sg2d, Shape s, boolean isFill) {
        Path2D.Float p2df;
        int transx, transy;
        if (sg2d.transformState <= SunGraphics2D.TRANSFORM_INT_TRANSLATE) {
            if (s instanceof Path2D.Float) {
                p2df = (Path2D.Float)s;
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
        SunToolkit.awtLock();
        try {
            long xgc = validate(sg2d);
            XDoPath(sg2d, sg2d.surfaceData.getNativeOps(), xgc,
                    transx, transy, p2df, isFill);
        } finally {
            SunToolkit.awtUnlock();
        }
    }

    public void draw(SunGraphics2D sg2d, Shape s) {
        if (sg2d.strokeState == SunGraphics2D.STROKE_THIN) {
            // Delegate to drawPolygon() if possible...
            if (s instanceof Polygon &&
                sg2d.transformState < SunGraphics2D.TRANSFORM_TRANSLATESCALE)
            {
                Polygon p = (Polygon) s;
                drawPolygon(sg2d, p.xpoints, p.ypoints, p.npoints);
                return;
            }

            // Otherwise we will use drawPath() for
            // high-quality thin paths.
            doPath(sg2d, s, false);
        } else if (sg2d.strokeState < SunGraphics2D.STROKE_CUSTOM) {
            // REMIND: X11 can handle uniform scaled wide lines
            // and dashed lines itself if we set the appropriate
            // XGC attributes (TBD).
            ShapeSpanIterator si = LoopPipe.getStrokeSpans(sg2d, s);
            try {
                SunToolkit.awtLock();
                try {
                    long xgc = validate(sg2d);
                    XFillSpans(sg2d.surfaceData.getNativeOps(), xgc,
                               si, si.getNativeIterator(),
                               0, 0);
                } finally {
                    SunToolkit.awtUnlock();
                }
            } finally {
                si.dispose();
            }
        } else {
            fill(sg2d, sg2d.stroke.createStrokedShape(s));
        }
    }

    public void fill(SunGraphics2D sg2d, Shape s) {
        if (sg2d.strokeState == SunGraphics2D.STROKE_THIN) {
            // Delegate to fillPolygon() if possible...
            if (s instanceof Polygon &&
                sg2d.transformState < SunGraphics2D.TRANSFORM_TRANSLATESCALE)
            {
                Polygon p = (Polygon) s;
                fillPolygon(sg2d, p.xpoints, p.ypoints, p.npoints);
                return;
            }

            // Otherwise we will use fillPath() for
            // high-quality fills.
            doPath(sg2d, s, true);
            return;
        }

        AffineTransform at;
        int transx, transy;
        if (sg2d.transformState < SunGraphics2D.TRANSFORM_TRANSLATESCALE) {
            // Transform (translation) will be done by XFillSpans
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
            SunToolkit.awtLock();
            try {
                long xgc = validate(sg2d);
                XFillSpans(sg2d.surfaceData.getNativeOps(), xgc,
                           ssi, ssi.getNativeIterator(),
                           transx, transy);
            } finally {
                SunToolkit.awtUnlock();
            }
        } finally {
            ssi.dispose();
        }
    }

    native void devCopyArea(long sdOps, long xgc,
                            int srcx, int srcy,
                            int dstx, int dsty,
                            int w, int h);

    public static class X11TracingRenderer extends X11Renderer {
        void XDrawLine(long pXSData, long xgc,
                       int x1, int y1, int x2, int y2)
        {
            GraphicsPrimitive.tracePrimitive("X11DrawLine");
            super.XDrawLine(pXSData, xgc, x1, y1, x2, y2);
        }
        void XDrawRect(long pXSData, long xgc,
                       int x, int y, int w, int h)
        {
            GraphicsPrimitive.tracePrimitive("X11DrawRect");
            super.XDrawRect(pXSData, xgc, x, y, w, h);
        }
        void XDrawRoundRect(long pXSData, long xgc,
                            int x, int y, int w, int h,
                            int arcW, int arcH)
        {
            GraphicsPrimitive.tracePrimitive("X11DrawRoundRect");
            super.XDrawRoundRect(pXSData, xgc, x, y, w, h, arcW, arcH);
        }
        void XDrawOval(long pXSData, long xgc,
                       int x, int y, int w, int h)
        {
            GraphicsPrimitive.tracePrimitive("X11DrawOval");
            super.XDrawOval(pXSData, xgc, x, y, w, h);
        }
        void XDrawArc(long pXSData, long xgc,
                      int x, int y, int w, int h,
                      int angleStart, int angleExtent)
        {
            GraphicsPrimitive.tracePrimitive("X11DrawArc");
            super.XDrawArc(pXSData, xgc,
                           x, y, w, h, angleStart, angleExtent);
        }
        void XDrawPoly(long pXSData, long xgc,
                       int transx, int transy,
                       int[] xpoints, int[] ypoints,
                       int npoints, boolean isclosed)
        {
            GraphicsPrimitive.tracePrimitive("X11DrawPoly");
            super.XDrawPoly(pXSData, xgc, transx, transy,
                            xpoints, ypoints, npoints, isclosed);
        }
        void XDoPath(SunGraphics2D sg2d, long pXSData, long xgc,
                     int transX, int transY, Path2D.Float p2df,
                     boolean isFill)
        {
            GraphicsPrimitive.tracePrimitive(isFill ?
                                             "X11FillPath" :
                                             "X11DrawPath");
            super.XDoPath(sg2d, pXSData, xgc, transX, transY, p2df, isFill);
        }
        void XFillRect(long pXSData, long xgc,
                       int x, int y, int w, int h)
        {
            GraphicsPrimitive.tracePrimitive("X11FillRect");
            super.XFillRect(pXSData, xgc, x, y, w, h);
        }
        void XFillRoundRect(long pXSData, long xgc,
                            int x, int y, int w, int h,
                            int arcW, int arcH)
        {
            GraphicsPrimitive.tracePrimitive("X11FillRoundRect");
            super.XFillRoundRect(pXSData, xgc, x, y, w, h, arcW, arcH);
        }
        void XFillOval(long pXSData, long xgc,
                       int x, int y, int w, int h)
        {
            GraphicsPrimitive.tracePrimitive("X11FillOval");
            super.XFillOval(pXSData, xgc, x, y, w, h);
        }
        void XFillArc(long pXSData, long xgc,
                      int x, int y, int w, int h,
                      int angleStart, int angleExtent)
        {
            GraphicsPrimitive.tracePrimitive("X11FillArc");
            super.XFillArc(pXSData, xgc,
                           x, y, w, h, angleStart, angleExtent);
        }
        void XFillPoly(long pXSData, long xgc,
                       int transx, int transy,
                       int[] xpoints, int[] ypoints,
                       int npoints)
        {
            GraphicsPrimitive.tracePrimitive("X11FillPoly");
            super.XFillPoly(pXSData, xgc,
                            transx, transy, xpoints, ypoints, npoints);
        }
        void XFillSpans(long pXSData, long xgc,
                        SpanIterator si, long iterator, int transx, int transy)
        {
            GraphicsPrimitive.tracePrimitive("X11FillSpans");
            super.XFillSpans(pXSData, xgc,
                             si, iterator, transx, transy);
        }
        void devCopyArea(long sdOps, long xgc,
                         int srcx, int srcy,
                         int dstx, int dsty,
                         int w, int h)
        {
            GraphicsPrimitive.tracePrimitive("X11CopyArea");
            super.devCopyArea(sdOps, xgc, srcx, srcy, dstx, dsty, w, h);
        }
    }
}
