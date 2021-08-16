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

import sun.java2d.InvalidPipeException;
import sun.java2d.SunGraphics2D;
import sun.java2d.loops.GraphicsPrimitive;
import sun.java2d.pipe.BufferedRenderPipe;
import sun.java2d.pipe.ParallelogramPipe;
import sun.java2d.pipe.RenderQueue;
import sun.java2d.pipe.SpanIterator;

import java.awt.Transparency;
import java.awt.geom.Path2D;

import static sun.java2d.pipe.BufferedOpCodes.COPY_AREA;

class MTLRenderer extends BufferedRenderPipe {

    MTLRenderer(RenderQueue rq) {
        super(rq);
    }

    @Override
    protected void validateContext(SunGraphics2D sg2d) {
        int ctxflags =
                sg2d.paint.getTransparency() == Transparency.OPAQUE ?
                        MTLContext.SRC_IS_OPAQUE : MTLContext.NO_CONTEXT_FLAGS;
        MTLSurfaceData dstData;
        try {
            dstData = (MTLSurfaceData)sg2d.surfaceData;
        } catch (ClassCastException e) {
            throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
        }
        MTLContext.validateContext(dstData, dstData,
                sg2d.getCompClip(), sg2d.composite,
                null, sg2d.paint, sg2d, ctxflags);
    }

    @Override
    protected void validateContextAA(SunGraphics2D sg2d) {
        int ctxflags = MTLContext.NO_CONTEXT_FLAGS;
        MTLSurfaceData dstData;
        try {
            dstData = (MTLSurfaceData)sg2d.surfaceData;
        } catch (ClassCastException e) {
            throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
        }
        MTLContext.validateContext(dstData, dstData,
                sg2d.getCompClip(), sg2d.composite,
                null, sg2d.paint, sg2d, ctxflags);
    }

    void copyArea(SunGraphics2D sg2d,
                  int x, int y, int w, int h, int dx, int dy)
    {
        rq.lock();
        try {
            int ctxflags =
                    sg2d.surfaceData.getTransparency() == Transparency.OPAQUE ?
                            MTLContext.SRC_IS_OPAQUE : MTLContext.NO_CONTEXT_FLAGS;
            MTLSurfaceData dstData;
            try {
                dstData = (MTLSurfaceData)sg2d.surfaceData;
            } catch (ClassCastException e) {
                throw new InvalidPipeException("wrong surface data type: " + sg2d.surfaceData);
            }
            MTLContext.validateContext(dstData, dstData,
                    sg2d.getCompClip(), sg2d.composite,
                    null, null, null, ctxflags);

            rq.ensureCapacity(28);
            buf.putInt(COPY_AREA);
            buf.putInt(x).putInt(y).putInt(w).putInt(h);
            buf.putInt(dx).putInt(dy);
        } finally {
            rq.unlock();
        }
    }

    @Override
    protected native void drawPoly(int[] xPoints, int[] yPoints,
                                   int nPoints, boolean isClosed,
                                   int transX, int transY);

    MTLRenderer traceWrap() {
        return new Tracer(this);
    }

    private class Tracer extends MTLRenderer {
        private MTLRenderer mtlr;
        Tracer(MTLRenderer mtlr) {
            super(mtlr.rq);
            this.mtlr = mtlr;
        }
        public ParallelogramPipe getAAParallelogramPipe() {
            final ParallelogramPipe realpipe = mtlr.getAAParallelogramPipe();
            return new ParallelogramPipe() {
                public void fillParallelogram(SunGraphics2D sg2d,
                                              double ux1, double uy1,
                                              double ux2, double uy2,
                                              double x, double y,
                                              double dx1, double dy1,
                                              double dx2, double dy2)
                {
                    GraphicsPrimitive.tracePrimitive("MTLFillAAParallelogram");
                    realpipe.fillParallelogram(sg2d,
                            ux1, uy1, ux2, uy2,
                            x, y, dx1, dy1, dx2, dy2);
                }
                public void drawParallelogram(SunGraphics2D sg2d,
                                              double ux1, double uy1,
                                              double ux2, double uy2,
                                              double x, double y,
                                              double dx1, double dy1,
                                              double dx2, double dy2,
                                              double lw1, double lw2)
                {
                    GraphicsPrimitive.tracePrimitive("MTLDrawAAParallelogram");
                    realpipe.drawParallelogram(sg2d,
                            ux1, uy1, ux2, uy2,
                            x, y, dx1, dy1, dx2, dy2,
                            lw1, lw2);
                }
            };
        }
        protected void validateContext(SunGraphics2D sg2d) {
            mtlr.validateContext(sg2d);
        }
        public void drawLine(SunGraphics2D sg2d,
                             int x1, int y1, int x2, int y2)
        {
            GraphicsPrimitive.tracePrimitive("MTLDrawLine");
            mtlr.drawLine(sg2d, x1, y1, x2, y2);
        }
        public void drawRect(SunGraphics2D sg2d, int x, int y, int w, int h) {
            GraphicsPrimitive.tracePrimitive("MTLDrawRect");
            mtlr.drawRect(sg2d, x, y, w, h);
        }
        protected void drawPoly(SunGraphics2D sg2d,
                                int[] xPoints, int[] yPoints,
                                int nPoints, boolean isClosed)
        {
            GraphicsPrimitive.tracePrimitive("MTLDrawPoly");
            mtlr.drawPoly(sg2d, xPoints, yPoints, nPoints, isClosed);
        }
        public void fillRect(SunGraphics2D sg2d, int x, int y, int w, int h) {
            GraphicsPrimitive.tracePrimitive("MTLFillRect");
            mtlr.fillRect(sg2d, x, y, w, h);
        }
        protected void drawPath(SunGraphics2D sg2d,
                                Path2D.Float p2df, int transx, int transy)
        {
            GraphicsPrimitive.tracePrimitive("MTLDrawPath");
            mtlr.drawPath(sg2d, p2df, transx, transy);
        }
        protected void fillPath(SunGraphics2D sg2d,
                                Path2D.Float p2df, int transx, int transy)
        {
            GraphicsPrimitive.tracePrimitive("MTLFillPath");
            mtlr.fillPath(sg2d, p2df, transx, transy);
        }
        protected void fillSpans(SunGraphics2D sg2d, SpanIterator si,
                                 int transx, int transy)
        {
            GraphicsPrimitive.tracePrimitive("MTLFillSpans");
            mtlr.fillSpans(sg2d, si, transx, transy);
        }
        public void fillParallelogram(SunGraphics2D sg2d,
                                      double ux1, double uy1,
                                      double ux2, double uy2,
                                      double x, double y,
                                      double dx1, double dy1,
                                      double dx2, double dy2)
        {
            GraphicsPrimitive.tracePrimitive("MTLFillParallelogram");
            mtlr.fillParallelogram(sg2d,
                    ux1, uy1, ux2, uy2,
                    x, y, dx1, dy1, dx2, dy2);
        }
        public void drawParallelogram(SunGraphics2D sg2d,
                                      double ux1, double uy1,
                                      double ux2, double uy2,
                                      double x, double y,
                                      double dx1, double dy1,
                                      double dx2, double dy2,
                                      double lw1, double lw2)
        {
            GraphicsPrimitive.tracePrimitive("MTLDrawParallelogram");
            mtlr.drawParallelogram(sg2d,
                    ux1, uy1, ux2, uy2,
                    x, y, dx1, dy1, dx2, dy2, lw1, lw2);
        }
        public void copyArea(SunGraphics2D sg2d,
                             int x, int y, int w, int h, int dx, int dy)
        {
            GraphicsPrimitive.tracePrimitive("MTLCopyArea");
            mtlr.copyArea(sg2d, x, y, w, h, dx, dy);
        }
    }
}
