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

import java.util.Arrays;
import sun.java2d.marlin.stats.Histogram;
import sun.java2d.marlin.stats.StatLong;

final class Helpers implements MarlinConst {

    private Helpers() {
        throw new Error("This is a non instantiable class");
    }

    static boolean within(final double x, final double y, final double err) {
        final double d = y - x;
        return (d <= err && d >= -err);
    }

    static double evalCubic(final double a, final double b,
                            final double c, final double d,
                            final double t)
    {
        return t * (t * (t * a + b) + c) + d;
    }

    static double evalQuad(final double a, final double b,
                           final double c, final double t)
    {
        return t * (t * a + b) + c;
    }

    static int quadraticRoots(final double a, final double b, final double c,
                              final double[] zeroes, final int off)
    {
        int ret = off;
        if (a != 0.0d) {
            final double dis = b*b - 4.0d * a * c;
            if (dis > 0.0d) {
                final double sqrtDis = Math.sqrt(dis);
                // depending on the sign of b we use a slightly different
                // algorithm than the traditional one to find one of the roots
                // so we can avoid adding numbers of different signs (which
                // might result in loss of precision).
                if (b >= 0.0d) {
                    zeroes[ret++] = (2.0d * c) / (-b - sqrtDis);
                    zeroes[ret++] = (-b - sqrtDis) / (2.0d * a);
                } else {
                    zeroes[ret++] = (-b + sqrtDis) / (2.0d * a);
                    zeroes[ret++] = (2.0d * c) / (-b + sqrtDis);
                }
            } else if (dis == 0.0d) {
                zeroes[ret++] = -b / (2.0d * a);
            }
        } else if (b != 0.0d) {
            zeroes[ret++] = -c / b;
        }
        return ret - off;
    }

    // find the roots of g(t) = d*t^3 + a*t^2 + b*t + c in [A,B)
    static int cubicRootsInAB(final double d, double a, double b, double c,
                              final double[] pts, final int off,
                              final double A, final double B)
    {
        if (d == 0.0d) {
            final int num = quadraticRoots(a, b, c, pts, off);
            return filterOutNotInAB(pts, off, num, A, B) - off;
        }
        // From Graphics Gems:
        // https://github.com/erich666/GraphicsGems/blob/master/gems/Roots3And4.c
        // (also from awt.geom.CubicCurve2D. But here we don't need as
        // much accuracy and we don't want to create arrays so we use
        // our own customized version).

        // normal form: x^3 + ax^2 + bx + c = 0

        /*
         * TODO: cleanup all that code after reading Roots3And4.c
         */
        a /= d;
        b /= d;
        c /= d;

        //  substitute x = y - A/3 to eliminate quadratic term:
        //     x^3 +Px + Q = 0
        //
        // Since we actually need P/3 and Q/2 for all of the
        // calculations that follow, we will calculate
        // p = P/3
        // q = Q/2
        // instead and use those values for simplicity of the code.
        final double sub = (1.0d / 3.0d) * a;
        final double sq_A = a * a;
        final double p = (1.0d / 3.0d) * ((-1.0d / 3.0d) * sq_A + b);
        final double q = (1.0d / 2.0d) * ((2.0d / 27.0d) * a * sq_A - sub * b + c);

        // use Cardano's formula

        final double cb_p = p * p * p;
        final double D = q * q + cb_p;

        int num;
        if (D < 0.0d) {
            // see: http://en.wikipedia.org/wiki/Cubic_function#Trigonometric_.28and_hyperbolic.29_method
            final double phi = (1.0d / 3.0d) * Math.acos(-q / Math.sqrt(-cb_p));
            final double t = 2.0d * Math.sqrt(-p);

            pts[off    ] = ( t * Math.cos(phi) - sub);
            pts[off + 1] = (-t * Math.cos(phi + (Math.PI / 3.0d)) - sub);
            pts[off + 2] = (-t * Math.cos(phi - (Math.PI / 3.0d)) - sub);
            num = 3;
        } else {
            final double sqrt_D = Math.sqrt(D);
            final double u =   Math.cbrt(sqrt_D - q);
            final double v = - Math.cbrt(sqrt_D + q);

            pts[off    ] = (u + v - sub);
            num = 1;

            if (within(D, 0.0d, 1e-8d)) {
                pts[off + 1] = ((-1.0d / 2.0d) * (u + v) - sub);
                num = 2;
            }
        }

        return filterOutNotInAB(pts, off, num, A, B) - off;
    }

    // returns the index 1 past the last valid element remaining after filtering
    static int filterOutNotInAB(final double[] nums, final int off, final int len,
                                final double a, final double b)
    {
        int ret = off;
        for (int i = off, end = off + len; i < end; i++) {
            if (nums[i] >= a && nums[i] < b) {
                nums[ret++] = nums[i];
            }
        }
        return ret;
    }

    static double fastLineLen(final double x0, final double y0,
                              final double x1, final double y1)
    {
        final double dx = x1 - x0;
        final double dy = y1 - y0;

        // use manhattan norm:
        return Math.abs(dx) + Math.abs(dy);
    }

    static double linelen(final double x0, final double y0,
                          final double x1, final double y1)
    {
        final double dx = x1 - x0;
        final double dy = y1 - y0;
        return Math.sqrt(dx * dx + dy * dy);
    }

    static double fastQuadLen(final double x0, final double y0,
                              final double x1, final double y1,
                              final double x2, final double y2)
    {
        final double dx1 = x1 - x0;
        final double dx2 = x2 - x1;
        final double dy1 = y1 - y0;
        final double dy2 = y2 - y1;

        // use manhattan norm:
        return Math.abs(dx1) + Math.abs(dx2)
             + Math.abs(dy1) + Math.abs(dy2);
    }

    static double quadlen(final double x0, final double y0,
                          final double x1, final double y1,
                          final double x2, final double y2)
    {
        return (linelen(x0, y0, x1, y1)
                + linelen(x1, y1, x2, y2)
                + linelen(x0, y0, x2, y2)) / 2.0d;
    }

    static double fastCurvelen(final double x0, final double y0,
                               final double x1, final double y1,
                               final double x2, final double y2,
                               final double x3, final double y3)
    {
        final double dx1 = x1 - x0;
        final double dx2 = x2 - x1;
        final double dx3 = x3 - x2;
        final double dy1 = y1 - y0;
        final double dy2 = y2 - y1;
        final double dy3 = y3 - y2;

        // use manhattan norm:
        return Math.abs(dx1) + Math.abs(dx2) + Math.abs(dx3)
             + Math.abs(dy1) + Math.abs(dy2) + Math.abs(dy3);
    }

    static double curvelen(final double x0, final double y0,
                           final double x1, final double y1,
                           final double x2, final double y2,
                           final double x3, final double y3)
    {
        return (linelen(x0, y0, x1, y1)
              + linelen(x1, y1, x2, y2)
              + linelen(x2, y2, x3, y3)
              + linelen(x0, y0, x3, y3)) / 2.0d;
    }

    // finds values of t where the curve in pts should be subdivided in order
    // to get good offset curves a distance of w away from the middle curve.
    // Stores the points in ts, and returns how many of them there were.
    static int findSubdivPoints(final Curve c, final double[] pts,
                                final double[] ts, final int type,
                                final double w2)
    {
        final double x12 = pts[2] - pts[0];
        final double y12 = pts[3] - pts[1];
        // if the curve is already parallel to either axis we gain nothing
        // from rotating it.
        if ((y12 != 0.0d) && (x12 != 0.0d)) {
            // we rotate it so that the first vector in the control polygon is
            // parallel to the x-axis. This will ensure that rotated quarter
            // circles won't be subdivided.
            final double hypot = Math.sqrt(x12 * x12 + y12 * y12);
            final double cos = x12 / hypot;
            final double sin = y12 / hypot;
            final double x1 = cos * pts[0] + sin * pts[1];
            final double y1 = cos * pts[1] - sin * pts[0];
            final double x2 = cos * pts[2] + sin * pts[3];
            final double y2 = cos * pts[3] - sin * pts[2];
            final double x3 = cos * pts[4] + sin * pts[5];
            final double y3 = cos * pts[5] - sin * pts[4];

            switch(type) {
            case 8:
                final double x4 = cos * pts[6] + sin * pts[7];
                final double y4 = cos * pts[7] - sin * pts[6];
                c.set(x1, y1, x2, y2, x3, y3, x4, y4);
                break;
            case 6:
                c.set(x1, y1, x2, y2, x3, y3);
                break;
            default:
            }
        } else {
            c.set(pts, type);
        }

        int ret = 0;
        // we subdivide at values of t such that the remaining rotated
        // curves are monotonic in x and y.
        ret += c.dxRoots(ts, ret);
        ret += c.dyRoots(ts, ret);

        // subdivide at inflection points.
        if (type == 8) {
            // quadratic curves can't have inflection points
            ret += c.infPoints(ts, ret);
        }

        // now we must subdivide at points where one of the offset curves will have
        // a cusp. This happens at ts where the radius of curvature is equal to w.
        ret += c.rootsOfROCMinusW(ts, ret, w2, 0.0001d);

        ret = filterOutNotInAB(ts, 0, ret, 0.0001d, 0.9999d);
        isort(ts, ret);
        return ret;
    }

    // finds values of t where the curve in pts should be subdivided in order
    // to get intersections with the given clip rectangle.
    // Stores the points in ts, and returns how many of them there were.
    static int findClipPoints(final Curve curve, final double[] pts,
                              final double[] ts, final int type,
                              final int outCodeOR,
                              final double[] clipRect)
    {
        curve.set(pts, type);

        // clip rectangle (ymin, ymax, xmin, xmax)
        int ret = 0;

        if ((outCodeOR & OUTCODE_LEFT) != 0) {
            ret += curve.xPoints(ts, ret, clipRect[2]);
        }
        if ((outCodeOR & OUTCODE_RIGHT) != 0) {
            ret += curve.xPoints(ts, ret, clipRect[3]);
        }
        if ((outCodeOR & OUTCODE_TOP) != 0) {
            ret += curve.yPoints(ts, ret, clipRect[0]);
        }
        if ((outCodeOR & OUTCODE_BOTTOM) != 0) {
            ret += curve.yPoints(ts, ret, clipRect[1]);
        }
        isort(ts, ret);
        return ret;
    }

    static void subdivide(final double[] src,
                          final double[] left, final double[] right,
                          final int type)
    {
        switch(type) {
        case 8:
            subdivideCubic(src, left, right);
            return;
        case 6:
            subdivideQuad(src, left, right);
            return;
        default:
            throw new InternalError("Unsupported curve type");
        }
    }

    static void isort(final double[] a, final int len) {
        for (int i = 1, j; i < len; i++) {
            final double ai = a[i];
            j = i - 1;
            for (; j >= 0 && a[j] > ai; j--) {
                a[j + 1] = a[j];
            }
            a[j + 1] = ai;
        }
    }

    // Most of these are copied from classes in java.awt.geom because we need
    // both single and double precision variants of these functions, and Line2D,
    // CubicCurve2D, QuadCurve2D don't provide them.
    /**
     * Subdivides the cubic curve specified by the coordinates
     * stored in the <code>src</code> array at indices <code>srcoff</code>
     * through (<code>srcoff</code>&nbsp;+&nbsp;7) and stores the
     * resulting two subdivided curves into the two result arrays at the
     * corresponding indices.
     * Either or both of the <code>left</code> and <code>right</code>
     * arrays may be <code>null</code> or a reference to the same array
     * as the <code>src</code> array.
     * Note that the last point in the first subdivided curve is the
     * same as the first point in the second subdivided curve. Thus,
     * it is possible to pass the same array for <code>left</code>
     * and <code>right</code> and to use offsets, such as <code>rightoff</code>
     * equals (<code>leftoff</code> + 6), in order
     * to avoid allocating extra storage for this common point.
     * @param src the array holding the coordinates for the source curve
     * @param left the array for storing the coordinates for the first
     * half of the subdivided curve
     * @param right the array for storing the coordinates for the second
     * half of the subdivided curve
     * @since 1.7
     */
    static void subdivideCubic(final double[] src,
                               final double[] left,
                               final double[] right)
    {
        double  x1 = src[0];
        double  y1 = src[1];
        double cx1 = src[2];
        double cy1 = src[3];
        double cx2 = src[4];
        double cy2 = src[5];
        double  x2 = src[6];
        double  y2 = src[7];

        left[0]  = x1;
        left[1]  = y1;

        right[6] = x2;
        right[7] = y2;

        x1 = (x1 + cx1) / 2.0d;
        y1 = (y1 + cy1) / 2.0d;
        x2 = (x2 + cx2) / 2.0d;
        y2 = (y2 + cy2) / 2.0d;

        double cx = (cx1 + cx2) / 2.0d;
        double cy = (cy1 + cy2) / 2.0d;

        cx1 = (x1 + cx) / 2.0d;
        cy1 = (y1 + cy) / 2.0d;
        cx2 = (x2 + cx) / 2.0d;
        cy2 = (y2 + cy) / 2.0d;
        cx  = (cx1 + cx2) / 2.0d;
        cy  = (cy1 + cy2) / 2.0d;

        left[2] = x1;
        left[3] = y1;
        left[4] = cx1;
        left[5] = cy1;
        left[6] = cx;
        left[7] = cy;

        right[0] = cx;
        right[1] = cy;
        right[2] = cx2;
        right[3] = cy2;
        right[4] = x2;
        right[5] = y2;
    }

    static void subdivideCubicAt(final double t,
                                 final double[] src, final int offS,
                                 final double[] pts, final int offL, final int offR)
    {
        double  x1 = src[offS    ];
        double  y1 = src[offS + 1];
        double cx1 = src[offS + 2];
        double cy1 = src[offS + 3];
        double cx2 = src[offS + 4];
        double cy2 = src[offS + 5];
        double  x2 = src[offS + 6];
        double  y2 = src[offS + 7];

        pts[offL    ] = x1;
        pts[offL + 1] = y1;

        pts[offR + 6] = x2;
        pts[offR + 7] = y2;

        x1 =  x1 + t * (cx1 - x1);
        y1 =  y1 + t * (cy1 - y1);
        x2 = cx2 + t * (x2 - cx2);
        y2 = cy2 + t * (y2 - cy2);

        double cx = cx1 + t * (cx2 - cx1);
        double cy = cy1 + t * (cy2 - cy1);

        cx1 =  x1 + t * (cx - x1);
        cy1 =  y1 + t * (cy - y1);
        cx2 =  cx + t * (x2 - cx);
        cy2 =  cy + t * (y2 - cy);
        cx  = cx1 + t * (cx2 - cx1);
        cy  = cy1 + t * (cy2 - cy1);

        pts[offL + 2] = x1;
        pts[offL + 3] = y1;
        pts[offL + 4] = cx1;
        pts[offL + 5] = cy1;
        pts[offL + 6] = cx;
        pts[offL + 7] = cy;

        pts[offR    ] = cx;
        pts[offR + 1] = cy;
        pts[offR + 2] = cx2;
        pts[offR + 3] = cy2;
        pts[offR + 4] = x2;
        pts[offR + 5] = y2;
    }

    static void subdivideQuad(final double[] src,
                              final double[] left,
                              final double[] right)
    {
        double x1 = src[0];
        double y1 = src[1];
        double cx = src[2];
        double cy = src[3];
        double x2 = src[4];
        double y2 = src[5];

        left[0]  = x1;
        left[1]  = y1;

        right[4] = x2;
        right[5] = y2;

        x1 = (x1 + cx) / 2.0d;
        y1 = (y1 + cy) / 2.0d;
        x2 = (x2 + cx) / 2.0d;
        y2 = (y2 + cy) / 2.0d;
        cx = (x1 + x2) / 2.0d;
        cy = (y1 + y2) / 2.0d;

        left[2] = x1;
        left[3] = y1;
        left[4] = cx;
        left[5] = cy;

        right[0] = cx;
        right[1] = cy;
        right[2] = x2;
        right[3] = y2;
    }

    static void subdivideQuadAt(final double t,
                                final double[] src, final int offS,
                                final double[] pts, final int offL, final int offR)
    {
        double x1 = src[offS    ];
        double y1 = src[offS + 1];
        double cx = src[offS + 2];
        double cy = src[offS + 3];
        double x2 = src[offS + 4];
        double y2 = src[offS + 5];

        pts[offL    ] = x1;
        pts[offL + 1] = y1;

        pts[offR + 4] = x2;
        pts[offR + 5] = y2;

        x1 = x1 + t * (cx - x1);
        y1 = y1 + t * (cy - y1);
        x2 = cx + t * (x2 - cx);
        y2 = cy + t * (y2 - cy);
        cx = x1 + t * (x2 - x1);
        cy = y1 + t * (y2 - y1);

        pts[offL + 2] = x1;
        pts[offL + 3] = y1;
        pts[offL + 4] = cx;
        pts[offL + 5] = cy;

        pts[offR    ] = cx;
        pts[offR + 1] = cy;
        pts[offR + 2] = x2;
        pts[offR + 3] = y2;
    }

    static void subdivideLineAt(final double t,
                                final double[] src, final int offS,
                                final double[] pts, final int offL, final int offR)
    {
        double x1 = src[offS    ];
        double y1 = src[offS + 1];
        double x2 = src[offS + 2];
        double y2 = src[offS + 3];

        pts[offL    ] = x1;
        pts[offL + 1] = y1;

        pts[offR + 2] = x2;
        pts[offR + 3] = y2;

        x1 = x1 + t * (x2 - x1);
        y1 = y1 + t * (y2 - y1);

        pts[offL + 2] = x1;
        pts[offL + 3] = y1;

        pts[offR    ] = x1;
        pts[offR + 1] = y1;
    }

    static void subdivideAt(final double t,
                            final double[] src, final int offS,
                            final double[] pts, final int offL, final int type)
    {
        // if instead of switch (perf + most probable cases first)
        if (type == 8) {
            subdivideCubicAt(t, src, offS, pts, offL, offL + type);
        } else if (type == 4) {
            subdivideLineAt(t, src, offS, pts, offL, offL + type);
        } else {
            subdivideQuadAt(t, src, offS, pts, offL, offL + type);
        }
    }

    // From sun.java2d.loops.GeneralRenderer:

    static int outcode(final double x, final double y,
                       final double[] clipRect)
    {
        int code;
        if (y < clipRect[0]) {
            code = OUTCODE_TOP;
        } else if (y >= clipRect[1]) {
            code = OUTCODE_BOTTOM;
        } else {
            code = 0;
        }
        if (x < clipRect[2]) {
            code |= OUTCODE_LEFT;
        } else if (x >= clipRect[3]) {
            code |= OUTCODE_RIGHT;
        }
        return code;
    }

    // a stack of polynomial curves where each curve shares endpoints with
    // adjacent ones.
    static final class PolyStack {
        private static final byte TYPE_LINETO  = (byte) 0;
        private static final byte TYPE_QUADTO  = (byte) 1;
        private static final byte TYPE_CUBICTO = (byte) 2;

        // curves capacity = edges count (8192) = edges x 2 (coords)
        private static final int INITIAL_CURVES_COUNT = INITIAL_EDGES_COUNT << 1;

        // types capacity = edges count (4096)
        private static final int INITIAL_TYPES_COUNT = INITIAL_EDGES_COUNT;

        double[] curves;
        int end;
        byte[] curveTypes;
        int numCurves;

        // curves ref (dirty)
        final DoubleArrayCache.Reference curves_ref;
        // curveTypes ref (dirty)
        final ByteArrayCache.Reference curveTypes_ref;

        // used marks (stats only)
        int curveTypesUseMark;
        int curvesUseMark;

        private final StatLong stat_polystack_types;
        private final StatLong stat_polystack_curves;
        private final Histogram hist_polystack_curves;
        private final StatLong stat_array_polystack_curves;
        private final StatLong stat_array_polystack_curveTypes;

        PolyStack(final RendererContext rdrCtx) {
            this(rdrCtx, null, null, null, null, null);
        }

        PolyStack(final RendererContext rdrCtx,
                  final StatLong stat_polystack_types,
                  final StatLong stat_polystack_curves,
                  final Histogram hist_polystack_curves,
                  final StatLong stat_array_polystack_curves,
                  final StatLong stat_array_polystack_curveTypes)
        {
            curves_ref = rdrCtx.newDirtyDoubleArrayRef(INITIAL_CURVES_COUNT); // 32K
            curves     = curves_ref.initial;

            curveTypes_ref = rdrCtx.newDirtyByteArrayRef(INITIAL_TYPES_COUNT); // 4K
            curveTypes     = curveTypes_ref.initial;
            numCurves = 0;
            end = 0;

            if (DO_STATS) {
                curveTypesUseMark = 0;
                curvesUseMark = 0;
            }
            this.stat_polystack_types = stat_polystack_types;
            this.stat_polystack_curves = stat_polystack_curves;
            this.hist_polystack_curves = hist_polystack_curves;
            this.stat_array_polystack_curves = stat_array_polystack_curves;
            this.stat_array_polystack_curveTypes = stat_array_polystack_curveTypes;
        }

        /**
         * Disposes this PolyStack:
         * clean up before reusing this instance
         */
        void dispose() {
            end = 0;
            numCurves = 0;

            if (DO_STATS) {
                stat_polystack_types.add(curveTypesUseMark);
                stat_polystack_curves.add(curvesUseMark);
                hist_polystack_curves.add(curvesUseMark);

                // reset marks
                curveTypesUseMark = 0;
                curvesUseMark = 0;
            }

            // Return arrays:
            // curves and curveTypes are kept dirty
            curves     = curves_ref.putArray(curves);
            curveTypes = curveTypes_ref.putArray(curveTypes);
        }

        private void ensureSpace(final int n) {
            // use substraction to avoid integer overflow:
            if (curves.length - end < n) {
                if (DO_STATS) {
                    stat_array_polystack_curves.add(end + n);
                }
                curves = curves_ref.widenArray(curves, end, end + n);
            }
            if (curveTypes.length <= numCurves) {
                if (DO_STATS) {
                    stat_array_polystack_curveTypes.add(numCurves + 1);
                }
                curveTypes = curveTypes_ref.widenArray(curveTypes,
                                                       numCurves,
                                                       numCurves + 1);
            }
        }

        void pushCubic(double x0, double y0,
                       double x1, double y1,
                       double x2, double y2)
        {
            ensureSpace(6);
            curveTypes[numCurves++] = TYPE_CUBICTO;
            // we reverse the coordinate order to make popping easier
            final double[] _curves = curves;
            int e = end;
            _curves[e++] = x2;    _curves[e++] = y2;
            _curves[e++] = x1;    _curves[e++] = y1;
            _curves[e++] = x0;    _curves[e++] = y0;
            end = e;
        }

        void pushQuad(double x0, double y0,
                      double x1, double y1)
        {
            ensureSpace(4);
            curveTypes[numCurves++] = TYPE_QUADTO;
            final double[] _curves = curves;
            int e = end;
            _curves[e++] = x1;    _curves[e++] = y1;
            _curves[e++] = x0;    _curves[e++] = y0;
            end = e;
        }

        void pushLine(double x, double y) {
            ensureSpace(2);
            curveTypes[numCurves++] = TYPE_LINETO;
            curves[end++] = x;    curves[end++] = y;
        }

        void pullAll(final DPathConsumer2D io) {
            final int nc = numCurves;
            if (nc == 0) {
                return;
            }
            if (DO_STATS) {
                // update used marks:
                if (numCurves > curveTypesUseMark) {
                    curveTypesUseMark = numCurves;
                }
                if (end > curvesUseMark) {
                    curvesUseMark = end;
                }
            }
            final byte[]  _curveTypes = curveTypes;
            final double[] _curves = curves;
            int e = 0;

            for (int i = 0; i < nc; i++) {
                switch(_curveTypes[i]) {
                case TYPE_LINETO:
                    io.lineTo(_curves[e], _curves[e+1]);
                    e += 2;
                    continue;
                case TYPE_CUBICTO:
                    io.curveTo(_curves[e],   _curves[e+1],
                               _curves[e+2], _curves[e+3],
                               _curves[e+4], _curves[e+5]);
                    e += 6;
                    continue;
                case TYPE_QUADTO:
                    io.quadTo(_curves[e],   _curves[e+1],
                              _curves[e+2], _curves[e+3]);
                    e += 4;
                    continue;
                default:
                }
            }
            numCurves = 0;
            end = 0;
        }

        void popAll(final DPathConsumer2D io) {
            int nc = numCurves;
            if (nc == 0) {
                return;
            }
            if (DO_STATS) {
                // update used marks:
                if (numCurves > curveTypesUseMark) {
                    curveTypesUseMark = numCurves;
                }
                if (end > curvesUseMark) {
                    curvesUseMark = end;
                }
            }
            final byte[]  _curveTypes = curveTypes;
            final double[] _curves = curves;
            int e  = end;

            while (nc != 0) {
                switch(_curveTypes[--nc]) {
                case TYPE_LINETO:
                    e -= 2;
                    io.lineTo(_curves[e], _curves[e+1]);
                    continue;
                case TYPE_CUBICTO:
                    e -= 6;
                    io.curveTo(_curves[e],   _curves[e+1],
                               _curves[e+2], _curves[e+3],
                               _curves[e+4], _curves[e+5]);
                    continue;
                case TYPE_QUADTO:
                    e -= 4;
                    io.quadTo(_curves[e],   _curves[e+1],
                              _curves[e+2], _curves[e+3]);
                    continue;
                default:
                }
            }
            numCurves = 0;
            end = 0;
        }

        @Override
        public String toString() {
            String ret = "";
            int nc = numCurves;
            int last = end;
            int len;
            while (nc != 0) {
                switch(curveTypes[--nc]) {
                case TYPE_LINETO:
                    len = 2;
                    ret += "line: ";
                    break;
                case TYPE_QUADTO:
                    len = 4;
                    ret += "quad: ";
                    break;
                case TYPE_CUBICTO:
                    len = 6;
                    ret += "cubic: ";
                    break;
                default:
                    len = 0;
                }
                last -= len;
                ret += Arrays.toString(Arrays.copyOfRange(curves, last, last+len))
                                       + "\n";
            }
            return ret;
        }
    }

    // a stack of integer indices
    static final class IndexStack {

        // integer capacity = edges count / 4 ~ 1024
        private static final int INITIAL_COUNT = INITIAL_EDGES_COUNT >> 2;

        private int end;
        private int[] indices;

        // indices ref (dirty)
        private final IntArrayCache.Reference indices_ref;

        // used marks (stats only)
        private int indicesUseMark;

        private final StatLong stat_idxstack_indices;
        private final Histogram hist_idxstack_indices;
        private final StatLong stat_array_idxstack_indices;

        IndexStack(final RendererContext rdrCtx) {
            this(rdrCtx, null, null, null);
        }

        IndexStack(final RendererContext rdrCtx,
                   final StatLong stat_idxstack_indices,
                   final Histogram hist_idxstack_indices,
                   final StatLong stat_array_idxstack_indices)
        {
            indices_ref = rdrCtx.newDirtyIntArrayRef(INITIAL_COUNT); // 4K
            indices     = indices_ref.initial;
            end = 0;

            if (DO_STATS) {
                indicesUseMark = 0;
            }
            this.stat_idxstack_indices = stat_idxstack_indices;
            this.hist_idxstack_indices = hist_idxstack_indices;
            this.stat_array_idxstack_indices = stat_array_idxstack_indices;
        }

        /**
         * Disposes this PolyStack:
         * clean up before reusing this instance
         */
        void dispose() {
            end = 0;

            if (DO_STATS) {
                stat_idxstack_indices.add(indicesUseMark);
                hist_idxstack_indices.add(indicesUseMark);

                // reset marks
                indicesUseMark = 0;
            }

            // Return arrays:
            // values is kept dirty
            indices = indices_ref.putArray(indices);
        }

        boolean isEmpty() {
            return (end == 0);
        }

        void reset() {
            end = 0;
        }

        void push(final int v) {
            // remove redundant values (reverse order):
            int[] _values = indices;
            final int nc = end;
            if (nc != 0) {
                if (_values[nc - 1] == v) {
                    // remove both duplicated values:
                    end--;
                    return;
                }
            }
            if (_values.length <= nc) {
                if (DO_STATS) {
                    stat_array_idxstack_indices.add(nc + 1);
                }
                indices = _values = indices_ref.widenArray(_values, nc, nc + 1);
            }
            _values[end++] = v;

            if (DO_STATS) {
                // update used marks:
                if (end > indicesUseMark) {
                    indicesUseMark = end;
                }
            }
        }

        void pullAll(final double[] points, final DPathConsumer2D io) {
            final int nc = end;
            if (nc == 0) {
                return;
            }
            final int[] _values = indices;

            for (int i = 0, j; i < nc; i++) {
                j = _values[i] << 1;
                io.lineTo(points[j], points[j + 1]);
            }
            end = 0;
        }
    }
}
