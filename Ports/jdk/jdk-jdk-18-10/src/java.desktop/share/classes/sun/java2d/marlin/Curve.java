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

final class Curve {

    double ax, ay, bx, by, cx, cy, dx, dy;
    double dax, day, dbx, dby;

    Curve() {
    }

    void set(final double[] points, final int type) {
        // if instead of switch (perf + most probable cases first)
        if (type == 8) {
            set(points[0], points[1],
                points[2], points[3],
                points[4], points[5],
                points[6], points[7]);
        } else if (type == 4) {
            set(points[0], points[1],
                points[2], points[3]);
        } else {
            set(points[0], points[1],
                points[2], points[3],
                points[4], points[5]);
        }
    }

    void set(final double x1, final double y1,
             final double x2, final double y2,
             final double x3, final double y3,
             final double x4, final double y4)
    {
        final double dx32 = 3.0d * (x3 - x2);
        final double dy32 = 3.0d * (y3 - y2);
        final double dx21 = 3.0d * (x2 - x1);
        final double dy21 = 3.0d * (y2 - y1);
        ax = (x4 - x1) - dx32;  // A = P3 - P0 - 3 (P2 - P1) = (P3 - P0) + 3 (P1 - P2)
        ay = (y4 - y1) - dy32;
        bx = (dx32 - dx21);     // B = 3 (P2 - P1) - 3(P1 - P0) = 3 (P2 + P0) - 6 P1
        by = (dy32 - dy21);
        cx = dx21;              // C = 3 (P1 - P0)
        cy = dy21;
        dx = x1;                // D = P0
        dy = y1;
        dax = 3.0d * ax;
        day = 3.0d * ay;
        dbx = 2.0d * bx;
        dby = 2.0d * by;
    }

    void set(final double x1, final double y1,
             final double x2, final double y2,
             final double x3, final double y3)
    {
        final double dx21 = (x2 - x1);
        final double dy21 = (y2 - y1);
        ax = 0.0d;              // A = 0
        ay = 0.0d;
        bx = (x3 - x2) - dx21;  // B = P3 - P0 - 2 P2
        by = (y3 - y2) - dy21;
        cx = 2.0d * dx21;       // C = 2 (P2 - P1)
        cy = 2.0d * dy21;
        dx = x1;                // D = P1
        dy = y1;
        dax = 0.0d;
        day = 0.0d;
        dbx = 2.0d * bx;
        dby = 2.0d * by;
    }

    void set(final double x1, final double y1,
             final double x2, final double y2)
    {
        final double dx21 = (x2 - x1);
        final double dy21 = (y2 - y1);
        ax = 0.0d;              // A = 0
        ay = 0.0d;
        bx = 0.0d;              // B = 0
        by = 0.0d;
        cx = dx21;              // C = (P2 - P1)
        cy = dy21;
        dx = x1;                // D = P1
        dy = y1;
        dax = 0.0d;
        day = 0.0d;
        dbx = 0.0d;
        dby = 0.0d;
    }

    int dxRoots(final double[] roots, final int off) {
        return Helpers.quadraticRoots(dax, dbx, cx, roots, off);
    }

    int dyRoots(final double[] roots, final int off) {
        return Helpers.quadraticRoots(day, dby, cy, roots, off);
    }

    int infPoints(final double[] pts, final int off) {
        // inflection point at t if -f'(t)x*f''(t)y + f'(t)y*f''(t)x == 0
        // Fortunately, this turns out to be quadratic, so there are at
        // most 2 inflection points.
        final double a = dax * dby - dbx * day;
        final double b = 2.0d * (cy * dax - day * cx);
        final double c = cy * dbx - cx * dby;

        return Helpers.quadraticRoots(a, b, c, pts, off);
    }

    int xPoints(final double[] ts, final int off, final double x)
    {
        return Helpers.cubicRootsInAB(ax, bx, cx, dx - x, ts, off, 0.0d, 1.0d);
    }

    int yPoints(final double[] ts, final int off, final double y)
    {
        return Helpers.cubicRootsInAB(ay, by, cy, dy - y, ts, off, 0.0d, 1.0d);
    }

    // finds points where the first and second derivative are
    // perpendicular. This happens when g(t) = f'(t)*f''(t) == 0 (where
    // * is a dot product). Unfortunately, we have to solve a cubic.
    private int perpendiculardfddf(final double[] pts, final int off) {
        assert pts.length >= off + 4;

        // these are the coefficients of some multiple of g(t) (not g(t),
        // because the roots of a polynomial are not changed after multiplication
        // by a constant, and this way we save a few multiplications).
        final double a = 2.0d * (dax * dax + day * day);
        final double b = 3.0d * (dax * dbx + day * dby);
        final double c = 2.0d * (dax * cx + day * cy) + dbx * dbx + dby * dby;
        final double d = dbx * cx + dby * cy;

        return Helpers.cubicRootsInAB(a, b, c, d, pts, off, 0.0d, 1.0d);
    }

    // Tries to find the roots of the function ROC(t)-w in [0, 1). It uses
    // a variant of the false position algorithm to find the roots. False
    // position requires that 2 initial values x0,x1 be given, and that the
    // function must have opposite signs at those values. To find such
    // values, we need the local extrema of the ROC function, for which we
    // need the roots of its derivative; however, it's harder to find the
    // roots of the derivative in this case than it is to find the roots
    // of the original function. So, we find all points where this curve's
    // first and second derivative are perpendicular, and we pretend these
    // are our local extrema. There are at most 3 of these, so we will check
    // at most 4 sub-intervals of (0,1). ROC has asymptotes at inflection
    // points, so roc-w can have at least 6 roots. This shouldn't be a
    // problem for what we're trying to do (draw a nice looking curve).
    int rootsOfROCMinusW(final double[] roots, final int off, final double w2, final double err) {
        // no OOB exception, because by now off<=6, and roots.length >= 10
        assert off <= 6 && roots.length >= 10;

        int ret = off;
        final int end = off + perpendiculardfddf(roots, off);
        roots[end] = 1.0d; // always check interval end points

        double t0 = 0.0d, ft0 = ROCsq(t0) - w2;

        for (int i = off; i <= end; i++) {
            double t1 = roots[i], ft1 = ROCsq(t1) - w2;
            if (ft0 == 0.0d) {
                roots[ret++] = t0;
            } else if (ft1 * ft0 < 0.0d) { // have opposite signs
                // (ROC(t)^2 == w^2) == (ROC(t) == w) is true because
                // ROC(t) >= 0 for all t.
                roots[ret++] = falsePositionROCsqMinusX(t0, t1, w2, err);
            }
            t0 = t1;
            ft0 = ft1;
        }

        return ret - off;
    }

    private static double eliminateInf(final double x) {
        return (x == Double.POSITIVE_INFINITY ? Double.MAX_VALUE :
               (x == Double.NEGATIVE_INFINITY ? Double.MIN_VALUE : x));
    }

    // A slight modification of the false position algorithm on wikipedia.
    // This only works for the ROCsq-x functions. It might be nice to have
    // the function as an argument, but that would be awkward in java6.
    // TODO: It is something to consider for java8 (or whenever lambda
    // expressions make it into the language), depending on how closures
    // and turn out. Same goes for the newton's method
    // algorithm in Helpers.java
    private double falsePositionROCsqMinusX(final double t0, final double t1,
                                            final double w2, final double err)
    {
        final int iterLimit = 100;
        int side = 0;
        double t = t1, ft = eliminateInf(ROCsq(t) - w2);
        double s = t0, fs = eliminateInf(ROCsq(s) - w2);
        double r = s, fr;

        for (int i = 0; i < iterLimit && Math.abs(t - s) > err * Math.abs(t + s); i++) {
            r = (fs * t - ft * s) / (fs - ft);
            fr = ROCsq(r) - w2;
            if (sameSign(fr, ft)) {
                ft = fr; t = r;
                if (side < 0) {
                    fs /= (1 << (-side));
                    side--;
                } else {
                    side = -1;
                }
            } else if (fr * fs > 0.0d) {
                fs = fr; s = r;
                if (side > 0) {
                    ft /= (1 << side);
                    side++;
                } else {
                    side = 1;
                }
            } else {
                break;
            }
        }
        return r;
    }

    private static boolean sameSign(final double x, final double y) {
        // another way is to test if x*y > 0. This is bad for small x, y.
        return (x < 0.0d && y < 0.0d) || (x > 0.0d && y > 0.0d);
    }

    // returns the radius of curvature squared at t of this curve
    // see http://en.wikipedia.org/wiki/Radius_of_curvature_(applications)
    private double ROCsq(final double t) {
        final double dx = t * (t * dax + dbx) + cx;
        final double dy = t * (t * day + dby) + cy;
        final double ddx = 2.0d * dax * t + dbx;
        final double ddy = 2.0d * day * t + dby;
        final double dx2dy2 = dx * dx + dy * dy;
        final double ddx2ddy2 = ddx * ddx + ddy * ddy;
        final double ddxdxddydy = ddx * dx + ddy * dy;
        return dx2dy2 * ((dx2dy2 * dx2dy2) / (dx2dy2 * ddx2ddy2 - ddxdxddydy * ddxdxddydy));
    }
}
