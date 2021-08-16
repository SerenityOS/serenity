/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.pipe;

import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.loops.FillParallelogram;
import sun.java2d.loops.DrawParallelogram;
import sun.awt.SunHints;

public class LoopPipe
    implements PixelDrawPipe,
               PixelFillPipe,
               ParallelogramPipe,
               ShapeDrawPipe,
               LoopBasedPipe
{
    static final RenderingEngine RenderEngine = RenderingEngine.getInstance();

    public void drawLine(SunGraphics2D sg2d,
                         int x1, int y1, int x2, int y2)
    {
        int tX = sg2d.transX;
        int tY = sg2d.transY;
        sg2d.loops.drawLineLoop.DrawLine(sg2d, sg2d.getSurfaceData(),
                                         x1 + tX, y1 + tY,
                                         x2 + tX, y2 + tY);
    }

    public void drawRect(SunGraphics2D sg2d,
                         int x, int y, int width, int height)
    {
        sg2d.loops.drawRectLoop.DrawRect(sg2d, sg2d.getSurfaceData(),
                                         x + sg2d.transX,
                                         y + sg2d.transY,
                                         width, height);
    }

    public void drawRoundRect(SunGraphics2D sg2d,
                              int x, int y, int width, int height,
                              int arcWidth, int arcHeight)
    {
        sg2d.shapepipe.draw(sg2d,
                            new RoundRectangle2D.Float(x, y, width, height,
                                                       arcWidth, arcHeight));
    }

    public void drawOval(SunGraphics2D sg2d,
                         int x, int y, int width, int height)
    {
        sg2d.shapepipe.draw(sg2d, new Ellipse2D.Float(x, y, width, height));
    }

    public void drawArc(SunGraphics2D sg2d,
                        int x, int y, int width, int height,
                        int startAngle, int arcAngle)
    {
        sg2d.shapepipe.draw(sg2d, new Arc2D.Float(x, y, width, height,
                                                  startAngle, arcAngle,
                                                  Arc2D.OPEN));
    }

    public void drawPolyline(SunGraphics2D sg2d,
                             int[] xPoints, int[] yPoints,
                             int nPoints)
    {
        int[] nPointsArray = { nPoints };
        sg2d.loops.drawPolygonsLoop.DrawPolygons(sg2d, sg2d.getSurfaceData(),
                                                 xPoints, yPoints,
                                                 nPointsArray, 1,
                                                 sg2d.transX, sg2d.transY,
                                                 false);
    }

    public void drawPolygon(SunGraphics2D sg2d,
                            int[] xPoints, int[] yPoints,
                            int nPoints)
    {
        int[] nPointsArray = { nPoints };
        sg2d.loops.drawPolygonsLoop.DrawPolygons(sg2d, sg2d.getSurfaceData(),
                                                 xPoints, yPoints,
                                                 nPointsArray, 1,
                                                 sg2d.transX, sg2d.transY,
                                                 true);
    }

    public void fillRect(SunGraphics2D sg2d,
                         int x, int y, int width, int height)
    {
        sg2d.loops.fillRectLoop.FillRect(sg2d, sg2d.getSurfaceData(),
                                         x + sg2d.transX,
                                         y + sg2d.transY,
                                         width, height);
    }

    public void fillRoundRect(SunGraphics2D sg2d,
                              int x, int y, int width, int height,
                              int arcWidth, int arcHeight)
    {
        sg2d.shapepipe.fill(sg2d,
                            new RoundRectangle2D.Float(x, y, width, height,
                                                       arcWidth, arcHeight));
    }

    public void fillOval(SunGraphics2D sg2d,
                         int x, int y, int width, int height)
    {
        sg2d.shapepipe.fill(sg2d, new Ellipse2D.Float(x, y, width, height));
    }

    public void fillArc(SunGraphics2D sg2d,
                        int x, int y, int width, int height,
                        int startAngle, int arcAngle)
    {
        sg2d.shapepipe.fill(sg2d, new Arc2D.Float(x, y, width, height,
                                                  startAngle, arcAngle,
                                                  Arc2D.PIE));
    }

    public void fillPolygon(SunGraphics2D sg2d,
                            int[] xPoints, int[] yPoints,
                            int nPoints)
    {
        ShapeSpanIterator sr = getFillSSI(sg2d);

        try {
            sr.setOutputArea(sg2d.getCompClip());
            sr.appendPoly(xPoints, yPoints, nPoints, sg2d.transX, sg2d.transY);
            fillSpans(sg2d, sr);
        } finally {
            sr.dispose();
        }
    }


    public void draw(SunGraphics2D sg2d, Shape s) {
        if (sg2d.strokeState == SunGraphics2D.STROKE_THIN) {
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
            sg2d.loops.drawPathLoop.DrawPath(sg2d, sg2d.getSurfaceData(),
                                             transX, transY, p2df);
            return;
        }

        if (sg2d.strokeState == SunGraphics2D.STROKE_CUSTOM) {
            fill(sg2d, sg2d.stroke.createStrokedShape(s));
            return;
        }

        ShapeSpanIterator sr = getStrokeSpans(sg2d, s);

        try {
            fillSpans(sg2d, sr);
        } finally {
            sr.dispose();
        }
    }

    /**
     * Return a ShapeSpanIterator instance that normalizes as
     * appropriate for a fill operation as per the settings in
     * the specified SunGraphics2D object.
     *
     * The ShapeSpanIterator will be newly constructed and ready
     * to start taking in geometry.
     *
     * Note that the caller is responsible for calling dispose()
     * on the returned ShapeSpanIterator inside a try/finally block:
     * <pre>
     *     ShapeSpanIterator ssi = LoopPipe.getFillSSI(sg2d);
     *     try {
     *         ssi.setOutputArea(clip);
     *         ssi.appendPath(...); // or appendPoly
     *         // iterate the spans from ssi and operate on them
     *     } finally {
     *         ssi.dispose();
     *     }
     * </pre>
     */
    public static ShapeSpanIterator getFillSSI(SunGraphics2D sg2d) {
        boolean adjust = ((sg2d.stroke instanceof BasicStroke) &&
                          sg2d.strokeHint != SunHints.INTVAL_STROKE_PURE);
        return new ShapeSpanIterator(adjust);
    }

    /*
     * Return a ShapeSpanIterator ready to iterate the spans of the wide
     * outline of Shape s using the attributes of the SunGraphics2D
     * object.
     *
     * The ShapeSpanIterator returned will be fully constructed
     * and filled with the geometry from the Shape widened by the
     * appropriate BasicStroke and normalization parameters taken
     * from the SunGraphics2D object and be ready to start returning
     * spans.
     *
     * Note that the caller is responsible for calling dispose()
     * on the returned ShapeSpanIterator inside a try/finally block.
     * <pre>
     *     ShapeSpanIterator ssi = LoopPipe.getStrokeSpans(sg2d, s);
     *     try {
     *         // iterate the spans from ssi and operate on them
     *     } finally {
     *         ssi.dispose();
     *     }
     * </pre>
     *
     * REMIND: This should return a SpanIterator interface object
     * but the caller needs to dispose() the object and that method
     * is only on ShapeSpanIterator.
     * TODO: Add a dispose() method to the SpanIterator interface.
     */
    public static ShapeSpanIterator getStrokeSpans(SunGraphics2D sg2d,
                                                   Shape s)
    {
        ShapeSpanIterator sr = new ShapeSpanIterator(false);

        try {
            final Region clip = sg2d.getCompClip();
            sr.setOutputArea(clip);
            sr.setRule(PathIterator.WIND_NON_ZERO);

            BasicStroke bs = (BasicStroke) sg2d.stroke;
            boolean thin = (sg2d.strokeState <= SunGraphics2D.STROKE_THINDASHED);
            boolean normalize =
                (sg2d.strokeHint != SunHints.INTVAL_STROKE_PURE);

            RenderEngine.strokeTo(s,
                                  sg2d.transform, clip, bs,
                                  thin, normalize, false, sr);
        } catch (Throwable t) {
            sr.dispose();
            sr = null;
            throw new InternalError("Unable to Stroke shape ("+
                                    t.getMessage()+")", t);
        }
        return sr;
    }

    public void fill(SunGraphics2D sg2d, Shape s) {
        if (sg2d.strokeState == SunGraphics2D.STROKE_THIN) {
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
            sg2d.loops.fillPathLoop.FillPath(sg2d, sg2d.getSurfaceData(),
                                             transX, transY, p2df);
            return;
        }

        ShapeSpanIterator sr = getFillSSI(sg2d);
        try {
            sr.setOutputArea(sg2d.getCompClip());
            AffineTransform at =
                ((sg2d.transformState == SunGraphics2D.TRANSFORM_ISIDENT)
                 ? null
                 : sg2d.transform);
            sr.appendPath(s.getPathIterator(at));
            fillSpans(sg2d, sr);
        } finally {
            sr.dispose();
        }
    }

    private static void fillSpans(SunGraphics2D sg2d, SpanIterator si) {
        // REMIND: Eventually, the plan is that it will not be possible for
        // fs to be null since the FillSpans loop will be the fundamental
        // loop implemented for any destination type...
        if (sg2d.clipState == SunGraphics2D.CLIP_SHAPE) {
            si = sg2d.clipRegion.filter(si);
            // REMIND: Region.filter produces a Java-only iterator
            // with no native counterpart...
        } else {
            sun.java2d.loops.FillSpans fs = sg2d.loops.fillSpansLoop;
            if (fs != null) {
                fs.FillSpans(sg2d, sg2d.getSurfaceData(), si);
                return;
            }
        }
        int[] spanbox = new int[4];
        SurfaceData sd = sg2d.getSurfaceData();
        while (si.nextSpan(spanbox)) {
            int x = spanbox[0];
            int y = spanbox[1];
            int w = spanbox[2] - x;
            int h = spanbox[3] - y;
            sg2d.loops.fillRectLoop.FillRect(sg2d, sd, x, y, w, h);
        }
    }

    public void fillParallelogram(SunGraphics2D sg2d,
                                  double ux1, double uy1,
                                  double ux2, double uy2,
                                  double x, double y,
                                  double dx1, double dy1,
                                  double dx2, double dy2)
    {
        FillParallelogram fp = sg2d.loops.fillParallelogramLoop;
        fp.FillParallelogram(sg2d, sg2d.getSurfaceData(),
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
        DrawParallelogram dp = sg2d.loops.drawParallelogramLoop;
        dp.DrawParallelogram(sg2d, sg2d.getSurfaceData(),
                             x, y, dx1, dy1, dx2, dy2, lw1, lw2);
    }
}
