/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.marlin;

import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.Arrays;
import sun.java2d.marlin.Helpers.IndexStack;
import sun.java2d.marlin.Helpers.PolyStack;

final class TransformingPathConsumer2D {

    // smaller uncertainty in double variant
    static final double CLIP_RECT_PADDING = 0.25d;

    private final RendererContext rdrCtx;

    // recycled ClosedPathDetector instance from detectClosedPath()
    private final ClosedPathDetector   cpDetector;

    // recycled PathClipFilter instance from pathClipper()
    private final PathClipFilter       pathClipper;

    // recycled DPathConsumer2D instance from wrapPath2D()
    private final Path2DWrapper        wp_Path2DWrapper        = new Path2DWrapper();

    // recycled DPathConsumer2D instances from deltaTransformConsumer()
    private final DeltaScaleFilter     dt_DeltaScaleFilter     = new DeltaScaleFilter();
    private final DeltaTransformFilter dt_DeltaTransformFilter = new DeltaTransformFilter();

    // recycled DPathConsumer2D instances from inverseDeltaTransformConsumer()
    private final DeltaScaleFilter     iv_DeltaScaleFilter     = new DeltaScaleFilter();
    private final DeltaTransformFilter iv_DeltaTransformFilter = new DeltaTransformFilter();

    // recycled PathTracer instances from tracer...() methods
    private final PathTracer tracerInput      = new PathTracer("[Input]");
    private final PathTracer tracerCPDetector = new PathTracer("ClosedPathDetector");
    private final PathTracer tracerFiller     = new PathTracer("Filler");
    private final PathTracer tracerStroker    = new PathTracer("Stroker");
    private final PathTracer tracerDasher     = new PathTracer("Dasher");

    TransformingPathConsumer2D(final RendererContext rdrCtx) {
        // used by RendererContext
        this.rdrCtx = rdrCtx;
        this.cpDetector = new ClosedPathDetector(rdrCtx);
        this.pathClipper = new PathClipFilter(rdrCtx);
    }

    DPathConsumer2D wrapPath2D(Path2D.Double p2d) {
        return wp_Path2DWrapper.init(p2d);
    }

    DPathConsumer2D traceInput(DPathConsumer2D out) {
        return tracerInput.init(out);
    }

    DPathConsumer2D traceClosedPathDetector(DPathConsumer2D out) {
        return tracerCPDetector.init(out);
    }

    DPathConsumer2D traceFiller(DPathConsumer2D out) {
        return tracerFiller.init(out);
    }

    DPathConsumer2D traceStroker(DPathConsumer2D out) {
        return tracerStroker.init(out);
    }

    DPathConsumer2D traceDasher(DPathConsumer2D out) {
        return tracerDasher.init(out);
    }

    DPathConsumer2D detectClosedPath(DPathConsumer2D out) {
        return cpDetector.init(out);
    }

    DPathConsumer2D pathClipper(DPathConsumer2D out) {
        return pathClipper.init(out);
    }

    DPathConsumer2D deltaTransformConsumer(DPathConsumer2D out,
                                          AffineTransform at)
    {
        if (at == null) {
            return out;
        }
        final double mxx = at.getScaleX();
        final double mxy = at.getShearX();
        final double myx = at.getShearY();
        final double myy = at.getScaleY();

        if (mxy == 0.0d && myx == 0.0d) {
            if (mxx == 1.0d && myy == 1.0d) {
                return out;
            } else {
                // Scale only
                if (rdrCtx.doClip) {
                    // adjust clip rectangle (ymin, ymax, xmin, xmax):
                    rdrCtx.clipInvScale = adjustClipScale(rdrCtx.clipRect,
                        mxx, myy);
                }
                return dt_DeltaScaleFilter.init(out, mxx, myy);
            }
        } else {
            if (rdrCtx.doClip) {
                // adjust clip rectangle (ymin, ymax, xmin, xmax):
                rdrCtx.clipInvScale = adjustClipInverseDelta(rdrCtx.clipRect,
                    mxx, mxy, myx, myy);
            }
            return dt_DeltaTransformFilter.init(out, mxx, mxy, myx, myy);
        }
    }

    private static double adjustClipScale(final double[] clipRect,
                                          final double mxx, final double myy)
    {
        // Adjust the clipping rectangle (iv_DeltaScaleFilter):
        final double scaleY = 1.0d / myy;
        clipRect[0] *= scaleY;
        clipRect[1] *= scaleY;

        if (clipRect[1] < clipRect[0]) {
            double tmp = clipRect[0];
            clipRect[0] = clipRect[1];
            clipRect[1] = tmp;
        }

        final double scaleX = 1.0d / mxx;
        clipRect[2] *= scaleX;
        clipRect[3] *= scaleX;

        if (clipRect[3] < clipRect[2]) {
            double tmp = clipRect[2];
            clipRect[2] = clipRect[3];
            clipRect[3] = tmp;
        }

        if (MarlinConst.DO_LOG_CLIP) {
                MarlinUtils.logInfo("clipRect (ClipScale): "
                                    + Arrays.toString(clipRect));
        }
        return 0.5d * (Math.abs(scaleX) + Math.abs(scaleY));
    }

    private static double adjustClipInverseDelta(final double[] clipRect,
                                                 final double mxx, final double mxy,
                                                 final double myx, final double myy)
    {
        // Adjust the clipping rectangle (iv_DeltaTransformFilter):
        final double det = mxx * myy - mxy * myx;
        final double imxx =  myy / det;
        final double imxy = -mxy / det;
        final double imyx = -myx / det;
        final double imyy =  mxx / det;

        double xmin, xmax, ymin, ymax;
        double x, y;
        // xmin, ymin:
        x = clipRect[2] * imxx + clipRect[0] * imxy;
        y = clipRect[2] * imyx + clipRect[0] * imyy;

        xmin = xmax = x;
        ymin = ymax = y;

        // xmax, ymin:
        x = clipRect[3] * imxx + clipRect[0] * imxy;
        y = clipRect[3] * imyx + clipRect[0] * imyy;

        if (x < xmin) { xmin = x; } else if (x > xmax) { xmax = x; }
        if (y < ymin) { ymin = y; } else if (y > ymax) { ymax = y; }

        // xmin, ymax:
        x = clipRect[2] * imxx + clipRect[1] * imxy;
        y = clipRect[2] * imyx + clipRect[1] * imyy;

        if (x < xmin) { xmin = x; } else if (x > xmax) { xmax = x; }
        if (y < ymin) { ymin = y; } else if (y > ymax) { ymax = y; }

        // xmax, ymax:
        x = clipRect[3] * imxx + clipRect[1] * imxy;
        y = clipRect[3] * imyx + clipRect[1] * imyy;

        if (x < xmin) { xmin = x; } else if (x > xmax) { xmax = x; }
        if (y < ymin) { ymin = y; } else if (y > ymax) { ymax = y; }

        clipRect[0] = ymin;
        clipRect[1] = ymax;
        clipRect[2] = xmin;
        clipRect[3] = xmax;

        if (MarlinConst.DO_LOG_CLIP) {
                MarlinUtils.logInfo("clipRect (ClipInverseDelta): "
                                    + Arrays.toString(clipRect));
        }

        final double scaleX = Math.sqrt(imxx * imxx + imxy * imxy);
        final double scaleY = Math.sqrt(imyx * imyx + imyy * imyy);

        return 0.5d * (scaleX + scaleY);
    }

    DPathConsumer2D inverseDeltaTransformConsumer(DPathConsumer2D out,
                                                 AffineTransform at)
    {
        if (at == null) {
            return out;
        }
        double mxx = at.getScaleX();
        double mxy = at.getShearX();
        double myx = at.getShearY();
        double myy = at.getScaleY();

        if (mxy == 0.0d && myx == 0.0d) {
            if (mxx == 1.0d && myy == 1.0d) {
                return out;
            } else {
                return iv_DeltaScaleFilter.init(out, 1.0d / mxx, 1.0d / myy);
            }
        } else {
            final double det = mxx * myy - mxy * myx;
            return iv_DeltaTransformFilter.init(out,
                                                myy / det,
                                               -mxy / det,
                                               -myx / det,
                                                mxx / det);
        }
    }

    static final class DeltaScaleFilter implements DPathConsumer2D {
        private DPathConsumer2D out;
        private double sx, sy;

        DeltaScaleFilter() {}

        DeltaScaleFilter init(DPathConsumer2D out,
                              double mxx, double myy)
        {
            this.out = out;
            sx = mxx;
            sy = myy;
            return this; // fluent API
        }

        @Override
        public void moveTo(double x0, double y0) {
            out.moveTo(x0 * sx, y0 * sy);
        }

        @Override
        public void lineTo(double x1, double y1) {
            out.lineTo(x1 * sx, y1 * sy);
        }

        @Override
        public void quadTo(double x1, double y1,
                           double x2, double y2)
        {
            out.quadTo(x1 * sx, y1 * sy,
                       x2 * sx, y2 * sy);
        }

        @Override
        public void curveTo(double x1, double y1,
                            double x2, double y2,
                            double x3, double y3)
        {
            out.curveTo(x1 * sx, y1 * sy,
                        x2 * sx, y2 * sy,
                        x3 * sx, y3 * sy);
        }

        @Override
        public void closePath() {
            out.closePath();
        }

        @Override
        public void pathDone() {
            out.pathDone();
        }

        @Override
        public long getNativeConsumer() {
            return 0;
        }
    }

    static final class DeltaTransformFilter implements DPathConsumer2D {
        private DPathConsumer2D out;
        private double mxx, mxy, myx, myy;

        DeltaTransformFilter() {}

        DeltaTransformFilter init(DPathConsumer2D out,
                                  double mxx, double mxy,
                                  double myx, double myy)
        {
            this.out = out;
            this.mxx = mxx;
            this.mxy = mxy;
            this.myx = myx;
            this.myy = myy;
            return this; // fluent API
        }

        @Override
        public void moveTo(double x0, double y0) {
            out.moveTo(x0 * mxx + y0 * mxy,
                       x0 * myx + y0 * myy);
        }

        @Override
        public void lineTo(double x1, double y1) {
            out.lineTo(x1 * mxx + y1 * mxy,
                       x1 * myx + y1 * myy);
        }

        @Override
        public void quadTo(double x1, double y1,
                           double x2, double y2)
        {
            out.quadTo(x1 * mxx + y1 * mxy,
                       x1 * myx + y1 * myy,
                       x2 * mxx + y2 * mxy,
                       x2 * myx + y2 * myy);
        }

        @Override
        public void curveTo(double x1, double y1,
                            double x2, double y2,
                            double x3, double y3)
        {
            out.curveTo(x1 * mxx + y1 * mxy,
                        x1 * myx + y1 * myy,
                        x2 * mxx + y2 * mxy,
                        x2 * myx + y2 * myy,
                        x3 * mxx + y3 * mxy,
                        x3 * myx + y3 * myy);
        }

        @Override
        public void closePath() {
            out.closePath();
        }

        @Override
        public void pathDone() {
            out.pathDone();
        }

        @Override
        public long getNativeConsumer() {
            return 0;
        }
    }

    static final class Path2DWrapper implements DPathConsumer2D {
        private Path2D.Double p2d;

        Path2DWrapper() {}

        Path2DWrapper init(Path2D.Double p2d) {
            this.p2d = p2d;
            return this;
        }

        @Override
        public void moveTo(double x0, double y0) {
            p2d.moveTo(x0, y0);
        }

        @Override
        public void lineTo(double x1, double y1) {
            p2d.lineTo(x1, y1);
        }

        @Override
        public void closePath() {
            p2d.closePath();
        }

        @Override
        public void pathDone() {}

        @Override
        public void curveTo(double x1, double y1,
                            double x2, double y2,
                            double x3, double y3)
        {
            p2d.curveTo(x1, y1, x2, y2, x3, y3);
        }

        @Override
        public void quadTo(double x1, double y1, double x2, double y2) {
            p2d.quadTo(x1, y1, x2, y2);
        }

        @Override
        public long getNativeConsumer() {
            throw new InternalError("Not using a native peer");
        }
    }

    static final class ClosedPathDetector implements DPathConsumer2D {

        private final RendererContext rdrCtx;
        private final PolyStack stack;

        private DPathConsumer2D out;

        ClosedPathDetector(final RendererContext rdrCtx) {
            this.rdrCtx = rdrCtx;
            this.stack = (rdrCtx.stats != null) ?
                new PolyStack(rdrCtx,
                        rdrCtx.stats.stat_cpd_polystack_types,
                        rdrCtx.stats.stat_cpd_polystack_curves,
                        rdrCtx.stats.hist_cpd_polystack_curves,
                        rdrCtx.stats.stat_array_cpd_polystack_curves,
                        rdrCtx.stats.stat_array_cpd_polystack_types)
                : new PolyStack(rdrCtx);
        }

        ClosedPathDetector init(DPathConsumer2D out) {
            this.out = out;
            return this; // fluent API
        }

        /**
         * Disposes this instance:
         * clean up before reusing this instance
         */
        void dispose() {
            stack.dispose();
        }

        @Override
        public void pathDone() {
            // previous path is not closed:
            finish(false);
            out.pathDone();

            // TODO: fix possible leak if exception happened
            // Dispose this instance:
            dispose();
        }

        @Override
        public void closePath() {
            // path is closed
            finish(true);
            out.closePath();
        }

        @Override
        public void moveTo(double x0, double y0) {
            // previous path is not closed:
            finish(false);
            out.moveTo(x0, y0);
        }

        private void finish(final boolean closed) {
            rdrCtx.closedPath = closed;
            stack.pullAll(out);
        }

        @Override
        public void lineTo(double x1, double y1) {
            stack.pushLine(x1, y1);
        }

        @Override
        public void curveTo(double x3, double y3,
                            double x2, double y2,
                            double x1, double y1)
        {
            stack.pushCubic(x1, y1, x2, y2, x3, y3);
        }

        @Override
        public void quadTo(double x2, double y2, double x1, double y1) {
            stack.pushQuad(x1, y1, x2, y2);
        }

        @Override
        public long getNativeConsumer() {
            throw new InternalError("Not using a native peer");
        }
    }

    static final class PathClipFilter implements DPathConsumer2D {

        private DPathConsumer2D out;

        // Bounds of the drawing region, at pixel precision.
        private final double[] clipRect;

        private final double[] corners = new double[8];
        private boolean init_corners = false;

        private final IndexStack stack;

        // the current outcode of the current sub path
        private int cOutCode = 0;

        // the cumulated (and) outcode of the complete path
        private int gOutCode = MarlinConst.OUTCODE_MASK_T_B_L_R;

        private boolean outside = false;

        // The starting point of the path
        private double sx0, sy0;

        // The current point (TODO stupid repeated info)
        private double cx0, cy0;

        // The current point OUTSIDE
        private double cox0, coy0;

        private boolean subdivide = MarlinConst.DO_CLIP_SUBDIVIDER;
        private final CurveClipSplitter curveSplitter;

        PathClipFilter(final RendererContext rdrCtx) {
            this.clipRect = rdrCtx.clipRect;
            this.curveSplitter = rdrCtx.curveClipSplitter;

            this.stack = (rdrCtx.stats != null) ?
                new IndexStack(rdrCtx,
                        rdrCtx.stats.stat_pcf_idxstack_indices,
                        rdrCtx.stats.hist_pcf_idxstack_indices,
                        rdrCtx.stats.stat_array_pcf_idxstack_indices)
                : new IndexStack(rdrCtx);
        }

        PathClipFilter init(final DPathConsumer2D out) {
            this.out = out;

            if (MarlinConst.DO_CLIP_SUBDIVIDER) {
                // adjust padded clip rectangle:
                curveSplitter.init();
            }

            this.init_corners = true;
            this.gOutCode = MarlinConst.OUTCODE_MASK_T_B_L_R;

            return this; // fluent API
        }

        /**
         * Disposes this instance:
         * clean up before reusing this instance
         */
        void dispose() {
            stack.dispose();
        }

        private void finishPath() {
            if (outside) {
                // criteria: inside or totally outside ?
                if (gOutCode == 0) {
                    finish();
                } else {
                    this.outside = false;
                    stack.reset();
                }
            }
        }

        private void finish() {
            this.outside = false;

            if (!stack.isEmpty()) {
                if (init_corners) {
                    init_corners = false;

                    final double[] _corners = corners;
                    final double[] _clipRect = clipRect;
                    // Top Left (0):
                    _corners[0] = _clipRect[2];
                    _corners[1] = _clipRect[0];
                    // Bottom Left (1):
                    _corners[2] = _clipRect[2];
                    _corners[3] = _clipRect[1];
                    // Top right (2):
                    _corners[4] = _clipRect[3];
                    _corners[5] = _clipRect[0];
                    // Bottom Right (3):
                    _corners[6] = _clipRect[3];
                    _corners[7] = _clipRect[1];
                }
                stack.pullAll(corners, out);
            }
            out.lineTo(cox0, coy0);
            this.cx0 = cox0;
            this.cy0 = coy0;
        }

        @Override
        public void pathDone() {
            finishPath();

            out.pathDone();

            // TODO: fix possible leak if exception happened
            // Dispose this instance:
            dispose();
        }

        @Override
        public void closePath() {
            finishPath();

            out.closePath();

            // back to starting point:
            this.cOutCode = Helpers.outcode(sx0, sy0, clipRect);
            this.cx0 = sx0;
            this.cy0 = sy0;
        }

        @Override
        public void moveTo(final double x0, final double y0) {
            finishPath();

            out.moveTo(x0, y0);

            // update starting point:
            this.cOutCode = Helpers.outcode(x0, y0, clipRect);
            this.cx0 = x0;
            this.cy0 = y0;

            this.sx0 = x0;
            this.sy0 = y0;
        }

        @Override
        public void lineTo(final double xe, final double ye) {
            final int outcode0 = this.cOutCode;
            final int outcode1 = Helpers.outcode(xe, ye, clipRect);

            // Should clip
            final int orCode = (outcode0 | outcode1);
            if (orCode != 0) {
                final int sideCode = (outcode0 & outcode1);

                // basic rejection criteria:
                if (sideCode == 0) {
                    // overlap clip:
                    if (subdivide) {
                        // avoid reentrance
                        subdivide = false;
                        boolean ret;
                        // subdivide curve => callback with subdivided parts:
                        if (outside) {
                            ret = curveSplitter.splitLine(cox0, coy0, xe, ye,
                                                          orCode, this);
                        } else {
                            ret = curveSplitter.splitLine(cx0, cy0, xe, ye,
                                                          orCode, this);
                        }
                        // reentrance is done:
                        subdivide = true;
                        if (ret) {
                            return;
                        }
                    }
                    // already subdivided so render it
                } else {
                    this.cOutCode = outcode1;
                    this.gOutCode &= sideCode;
                    // keep last point coordinate before entering the clip again:
                    this.outside = true;
                    this.cox0 = xe;
                    this.coy0 = ye;

                    clip(sideCode, outcode0, outcode1);
                    return;
                }
            }

            this.cOutCode = outcode1;
            this.gOutCode = 0;

            if (outside) {
                finish();
            }
            // clipping disabled:
            out.lineTo(xe, ye);
            this.cx0 = xe;
            this.cy0 = ye;
        }

        private void clip(final int sideCode,
                          final int outcode0,
                          final int outcode1)
        {
            // corner or cross-boundary on left or right side:
            if ((outcode0 != outcode1)
                    && ((sideCode & MarlinConst.OUTCODE_MASK_L_R) != 0))
            {
                // combine outcodes:
                final int mergeCode = (outcode0 | outcode1);
                final int tbCode = mergeCode & MarlinConst.OUTCODE_MASK_T_B;
                final int lrCode = mergeCode & MarlinConst.OUTCODE_MASK_L_R;
                final int off = (lrCode == MarlinConst.OUTCODE_LEFT) ? 0 : 2;

                // add corners to outside stack:
                switch (tbCode) {
                    case MarlinConst.OUTCODE_TOP:
                        stack.push(off); // top
                        return;
                    case MarlinConst.OUTCODE_BOTTOM:
                        stack.push(off + 1); // bottom
                        return;
                    default:
                        // both TOP / BOTTOM:
                        if ((outcode0 & MarlinConst.OUTCODE_TOP) != 0) {
                            // top to bottom
                            stack.push(off); // top
                            stack.push(off + 1); // bottom
                        } else {
                            // bottom to top
                            stack.push(off + 1); // bottom
                            stack.push(off); // top
                        }
                }
            }
        }

        @Override
        public void curveTo(final double x1, final double y1,
                            final double x2, final double y2,
                            final double xe, final double ye)
        {
            final int outcode0 = this.cOutCode;
            final int outcode1 = Helpers.outcode(x1, y1, clipRect);
            final int outcode2 = Helpers.outcode(x2, y2, clipRect);
            final int outcode3 = Helpers.outcode(xe, ye, clipRect);

            // Should clip
            final int orCode = (outcode0 | outcode1 | outcode2 | outcode3);
            if (orCode != 0) {
                final int sideCode = outcode0 & outcode1 & outcode2 & outcode3;

                // basic rejection criteria:
                if (sideCode == 0) {
                    // overlap clip:
                    if (subdivide) {
                        // avoid reentrance
                        subdivide = false;
                        // subdivide curve => callback with subdivided parts:
                        boolean ret;
                        if (outside) {
                            ret = curveSplitter.splitCurve(cox0, coy0, x1, y1,
                                                           x2, y2, xe, ye,
                                                           orCode, this);
                        } else {
                            ret = curveSplitter.splitCurve(cx0, cy0, x1, y1,
                                                           x2, y2, xe, ye,
                                                           orCode, this);
                        }
                        // reentrance is done:
                        subdivide = true;
                        if (ret) {
                            return;
                        }
                    }
                    // already subdivided so render it
                } else {
                    this.cOutCode = outcode3;
                    this.gOutCode &= sideCode;
                    // keep last point coordinate before entering the clip again:
                    this.outside = true;
                    this.cox0 = xe;
                    this.coy0 = ye;

                    clip(sideCode, outcode0, outcode3);
                    return;
                }
            }

            this.cOutCode = outcode3;
            this.gOutCode = 0;

            if (outside) {
                finish();
            }
            // clipping disabled:
            out.curveTo(x1, y1, x2, y2, xe, ye);
            this.cx0 = xe;
            this.cy0 = ye;
        }

        @Override
        public void quadTo(final double x1, final double y1,
                           final double xe, final double ye)
        {
            final int outcode0 = this.cOutCode;
            final int outcode1 = Helpers.outcode(x1, y1, clipRect);
            final int outcode2 = Helpers.outcode(xe, ye, clipRect);

            // Should clip
            final int orCode = (outcode0 | outcode1 | outcode2);
            if (orCode != 0) {
                final int sideCode = outcode0 & outcode1 & outcode2;

                // basic rejection criteria:
                if (sideCode == 0) {
                    // overlap clip:
                    if (subdivide) {
                        // avoid reentrance
                        subdivide = false;
                        // subdivide curve => callback with subdivided parts:
                        boolean ret;
                        if (outside) {
                            ret = curveSplitter.splitQuad(cox0, coy0, x1, y1,
                                                          xe, ye, orCode, this);
                        } else {
                            ret = curveSplitter.splitQuad(cx0, cy0, x1, y1,
                                                          xe, ye, orCode, this);
                        }
                        // reentrance is done:
                        subdivide = true;
                        if (ret) {
                            return;
                        }
                    }
                    // already subdivided so render it
                } else {
                    this.cOutCode = outcode2;
                    this.gOutCode &= sideCode;
                    // keep last point coordinate before entering the clip again:
                    this.outside = true;
                    this.cox0 = xe;
                    this.coy0 = ye;

                    clip(sideCode, outcode0, outcode2);
                    return;
                }
            }

            this.cOutCode = outcode2;
            this.gOutCode = 0;

            if (outside) {
                finish();
            }
            // clipping disabled:
            out.quadTo(x1, y1, xe, ye);
            this.cx0 = xe;
            this.cy0 = ye;
        }

        @Override
        public long getNativeConsumer() {
            throw new InternalError("Not using a native peer");
        }
    }

    static final class CurveClipSplitter {

        static final double LEN_TH = MarlinProperties.getSubdividerMinLength();
        static final boolean DO_CHECK_LENGTH = (LEN_TH > 0.0d);

        private static final boolean TRACE = false;

        private static final int MAX_N_CURVES = 3 * 4;

        private final RendererContext rdrCtx;

        // scaled length threshold:
        private double minLength;

        // clip rectangle (ymin, ymax, xmin, xmax):
        final double[] clipRect;

        // clip rectangle (ymin, ymax, xmin, xmax) including padding:
        final double[] clipRectPad = new double[4];
        private boolean init_clipRectPad = false;

        // This is where the curve to be processed is put. We give it
        // enough room to store all curves.
        final double[] middle = new double[MAX_N_CURVES * 8 + 2];
        // t values at subdivision points
        private final double[] subdivTs = new double[MAX_N_CURVES];

        // dirty curve
        private final Curve curve;

        CurveClipSplitter(final RendererContext rdrCtx) {
            this.rdrCtx = rdrCtx;
            this.clipRect = rdrCtx.clipRect;
            this.curve = rdrCtx.curve;
        }

        void init() {
            this.init_clipRectPad = true;

            if (DO_CHECK_LENGTH) {
                this.minLength = (this.rdrCtx.clipInvScale == 0.0d) ? LEN_TH
                                    : (LEN_TH * this.rdrCtx.clipInvScale);

                if (MarlinConst.DO_LOG_CLIP) {
                    MarlinUtils.logInfo("CurveClipSplitter.minLength = "
                                            + minLength);
                }
            }
        }

        private void initPaddedClip() {
            // bounds as half-open intervals: minX <= x < maxX and minY <= y < maxY
            // adjust padded clip rectangle (ymin, ymax, xmin, xmax):
            // add a rounding error (curve subdivision ~ 0.1px):
            final double[] _clipRect = clipRect;
            final double[] _clipRectPad = clipRectPad;

            _clipRectPad[0] = _clipRect[0] - CLIP_RECT_PADDING;
            _clipRectPad[1] = _clipRect[1] + CLIP_RECT_PADDING;
            _clipRectPad[2] = _clipRect[2] - CLIP_RECT_PADDING;
            _clipRectPad[3] = _clipRect[3] + CLIP_RECT_PADDING;

            if (TRACE) {
                MarlinUtils.logInfo("clip: X [" + _clipRectPad[2] + " .. " + _clipRectPad[3] +"] "
                                        + "Y [" + _clipRectPad[0] + " .. " + _clipRectPad[1] +"]");
            }
        }

        boolean splitLine(final double x0, final double y0,
                          final double x1, final double y1,
                          final int outCodeOR,
                          final DPathConsumer2D out)
        {
            if (TRACE) {
                MarlinUtils.logInfo("divLine P0(" + x0 + ", " + y0 + ") P1(" + x1 + ", " + y1 + ")");
            }

            if (DO_CHECK_LENGTH && Helpers.fastLineLen(x0, y0, x1, y1) <= minLength) {
                return false;
            }

            final double[] mid = middle;
            mid[0] = x0;  mid[1] = y0;
            mid[2] = x1;  mid[3] = y1;

            return subdivideAtIntersections(4, outCodeOR, out);
        }

        boolean splitQuad(final double x0, final double y0,
                          final double x1, final double y1,
                          final double x2, final double y2,
                          final int outCodeOR,
                          final DPathConsumer2D out)
        {
            if (TRACE) {
                MarlinUtils.logInfo("divQuad P0(" + x0 + ", " + y0 + ") P1(" + x1 + ", " + y1 + ") P2(" + x2 + ", " + y2 + ")");
            }

            if (DO_CHECK_LENGTH && Helpers.fastQuadLen(x0, y0, x1, y1, x2, y2) <= minLength) {
                return false;
            }

            final double[] mid = middle;
            mid[0] = x0;  mid[1] = y0;
            mid[2] = x1;  mid[3] = y1;
            mid[4] = x2;  mid[5] = y2;

            return subdivideAtIntersections(6, outCodeOR, out);
        }

        boolean splitCurve(final double x0, final double y0,
                           final double x1, final double y1,
                           final double x2, final double y2,
                           final double x3, final double y3,
                           final int outCodeOR,
                           final DPathConsumer2D out)
        {
            if (TRACE) {
                MarlinUtils.logInfo("divCurve P0(" + x0 + ", " + y0 + ") P1(" + x1 + ", " + y1 + ") P2(" + x2 + ", " + y2 + ") P3(" + x3 + ", " + y3 + ")");
            }

            if (DO_CHECK_LENGTH && Helpers.fastCurvelen(x0, y0, x1, y1, x2, y2, x3, y3) <= minLength) {
                return false;
            }

            final double[] mid = middle;
            mid[0] = x0;  mid[1] = y0;
            mid[2] = x1;  mid[3] = y1;
            mid[4] = x2;  mid[5] = y2;
            mid[6] = x3;  mid[7] = y3;

            return subdivideAtIntersections(8, outCodeOR, out);
        }

        private boolean subdivideAtIntersections(final int type, final int outCodeOR,
                                                 final DPathConsumer2D out)
        {
            final double[] mid = middle;
            final double[] subTs = subdivTs;

            if (init_clipRectPad) {
                init_clipRectPad = false;
                initPaddedClip();
            }

            final int nSplits = Helpers.findClipPoints(curve, mid, subTs, type,
                                                        outCodeOR, clipRectPad);

            if (TRACE) {
                MarlinUtils.logInfo("nSplits: " + nSplits);
                MarlinUtils.logInfo("subTs: " + Arrays.toString(Arrays.copyOfRange(subTs, 0, nSplits)));
            }
            if (nSplits == 0) {
                // only curve support shortcut
                return false;
            }
            double prevT = 0.0d;

            for (int i = 0, off = 0; i < nSplits; i++, off += type) {
                final double t = subTs[i];

                Helpers.subdivideAt((t - prevT) / (1.0d - prevT),
                                     mid, off, mid, off, type);
                prevT = t;
            }

            for (int i = 0, off = 0; i <= nSplits; i++, off += type) {
                if (TRACE) {
                    MarlinUtils.logInfo("Part Curve " + Arrays.toString(Arrays.copyOfRange(mid, off, off + type)));
                }
                emitCurrent(type, mid, off, out);
            }
            return true;
        }

        static void emitCurrent(final int type, final double[] pts,
                                final int off, final DPathConsumer2D out)
        {
            // if instead of switch (perf + most probable cases first)
            if (type == 8) {
                out.curveTo(pts[off + 2], pts[off + 3],
                            pts[off + 4], pts[off + 5],
                            pts[off + 6], pts[off + 7]);
            } else if (type == 4) {
                out.lineTo(pts[off + 2], pts[off + 3]);
            } else {
                out.quadTo(pts[off + 2], pts[off + 3],
                           pts[off + 4], pts[off + 5]);
            }
        }
    }

    static final class CurveBasicMonotonizer {

        private static final int MAX_N_CURVES = 11;

        // squared half line width (for stroker)
        private double lw2;

        // number of splitted curves
        int nbSplits;

        // This is where the curve to be processed is put. We give it
        // enough room to store all curves.
        final double[] middle = new double[MAX_N_CURVES * 6 + 2];
        // t values at subdivision points
        private final double[] subdivTs = new double[MAX_N_CURVES - 1];

        // dirty curve
        private final Curve curve;

        CurveBasicMonotonizer(final RendererContext rdrCtx) {
            this.curve = rdrCtx.curve;
        }

        void init(final double lineWidth) {
            this.lw2 = (lineWidth * lineWidth) / 4.0d;
        }

        CurveBasicMonotonizer curve(final double x0, final double y0,
                                    final double x1, final double y1,
                                    final double x2, final double y2,
                                    final double x3, final double y3)
        {
            final double[] mid = middle;
            mid[0] = x0;  mid[1] = y0;
            mid[2] = x1;  mid[3] = y1;
            mid[4] = x2;  mid[5] = y2;
            mid[6] = x3;  mid[7] = y3;

            final double[] subTs = subdivTs;
            final int nSplits = Helpers.findSubdivPoints(curve, mid, subTs, 8, lw2);

            double prevT = 0.0d;
            for (int i = 0, off = 0; i < nSplits; i++, off += 6) {
                final double t = subTs[i];

                Helpers.subdivideCubicAt((t - prevT) / (1.0d - prevT),
                                          mid, off, mid, off, off + 6);
                prevT = t;
            }

            this.nbSplits = nSplits;
            return this;
        }

        CurveBasicMonotonizer quad(final double x0, final double y0,
                                   final double x1, final double y1,
                                   final double x2, final double y2)
        {
            final double[] mid = middle;
            mid[0] = x0;  mid[1] = y0;
            mid[2] = x1;  mid[3] = y1;
            mid[4] = x2;  mid[5] = y2;

            final double[] subTs = subdivTs;
            final int nSplits = Helpers.findSubdivPoints(curve, mid, subTs, 6, lw2);

            double prevt = 0.0d;
            for (int i = 0, off = 0; i < nSplits; i++, off += 4) {
                final double t = subTs[i];
                Helpers.subdivideQuadAt((t - prevt) / (1.0d - prevt),
                                         mid, off, mid, off, off + 4);
                prevt = t;
            }

            this.nbSplits = nSplits;
            return this;
        }
    }

    static final class PathTracer implements DPathConsumer2D {
        private final String prefix;
        private DPathConsumer2D out;

        PathTracer(String name) {
            this.prefix = name + ": ";
        }

        PathTracer init(DPathConsumer2D out) {
            this.out = out;
            return this; // fluent API
        }

        @Override
        public void moveTo(double x0, double y0) {
            log("p.moveTo(" + x0 + ", " + y0 + ");");
            out.moveTo(x0, y0);
        }

        @Override
        public void lineTo(double x1, double y1) {
            log("p.lineTo(" + x1 + ", " + y1 + ");");
            out.lineTo(x1, y1);
        }

        @Override
        public void curveTo(double x1, double y1,
                            double x2, double y2,
                            double x3, double y3)
        {
            log("p.curveTo(" + x1 + ", " + y1 + ", " + x2 + ", " + y2  + ", " + x3 + ", " + y3 + ");");
            out.curveTo(x1, y1, x2, y2, x3, y3);
        }

        @Override
        public void quadTo(double x1, double y1,
                           double x2, double y2) {
            log("p.quadTo(" + x1 + ", " + y1 + ", " + x2 + ", " + y2  + ");");
            out.quadTo(x1, y1, x2, y2);
        }

        @Override
        public void closePath() {
            log("p.closePath();");
            out.closePath();
        }

        @Override
        public void pathDone() {
            log("p.pathDone();");
            out.pathDone();
        }

        private void log(final String message) {
            MarlinUtils.logInfo(prefix + message);
        }

        @Override
        public long getNativeConsumer() {
            throw new InternalError("Not using a native peer");
        }
    }
}
