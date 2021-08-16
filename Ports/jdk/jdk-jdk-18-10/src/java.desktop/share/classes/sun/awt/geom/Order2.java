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

package sun.awt.geom;

import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.QuadCurve2D;
import java.util.Vector;

final class Order2 extends Curve {
    private double x0;
    private double y0;
    private double cx0;
    private double cy0;
    private double x1;
    private double y1;
    private double xmin;
    private double xmax;

    private double xcoeff0;
    private double xcoeff1;
    private double xcoeff2;
    private double ycoeff0;
    private double ycoeff1;
    private double ycoeff2;

    public static void insert(Vector<Curve> curves, double[] tmp,
                              double x0, double y0,
                              double cx0, double cy0,
                              double x1, double y1,
                              int direction)
    {
        int numparams = getHorizontalParams(y0, cy0, y1, tmp);
        if (numparams == 0) {
            // We are using addInstance here to avoid inserting horisontal
            // segments
            addInstance(curves, x0, y0, cx0, cy0, x1, y1, direction);
            return;
        }
        // assert(numparams == 1);
        double t = tmp[0];
        tmp[0] = x0;  tmp[1] = y0;
        tmp[2] = cx0; tmp[3] = cy0;
        tmp[4] = x1;  tmp[5] = y1;
        split(tmp, 0, t);
        int i0 = (direction == INCREASING)? 0 : 4;
        int i1 = 4 - i0;
        addInstance(curves, tmp[i0], tmp[i0 + 1], tmp[i0 + 2], tmp[i0 + 3],
                    tmp[i0 + 4], tmp[i0 + 5], direction);
        addInstance(curves, tmp[i1], tmp[i1 + 1], tmp[i1 + 2], tmp[i1 + 3],
                    tmp[i1 + 4], tmp[i1 + 5], direction);
    }

    public static void addInstance(Vector<Curve> curves,
                                   double x0, double y0,
                                   double cx0, double cy0,
                                   double x1, double y1,
                                   int direction) {
        if (y0 > y1) {
            curves.add(new Order2(x1, y1, cx0, cy0, x0, y0, -direction));
        } else if (y1 > y0) {
            curves.add(new Order2(x0, y0, cx0, cy0, x1, y1, direction));
        }
    }

    /*
     * Return the count of the number of horizontal sections of the
     * specified quadratic Bezier curve.  Put the parameters for the
     * horizontal sections into the specified {@code ret} array.
     * <p>
     * If we examine the parametric equation in t, we have:
     *     Py(t) = C0*(1-t)^2 + 2*CP*t*(1-t) + C1*t^2
     *           = C0 - 2*C0*t + C0*t^2 + 2*CP*t - 2*CP*t^2 + C1*t^2
     *           = C0 + (2*CP - 2*C0)*t + (C0 - 2*CP + C1)*t^2
     *     Py(t) = (C0 - 2*CP + C1)*t^2 + (2*CP - 2*C0)*t + (C0)
     * If we take the derivative, we get:
     *     Py(t) = At^2 + Bt + C
     *     dPy(t) = 2At + B = 0
     *     2*(C0 - 2*CP + C1)t + 2*(CP - C0) = 0
     *     2*(C0 - 2*CP + C1)t = 2*(C0 - CP)
     *     t = 2*(C0 - CP) / 2*(C0 - 2*CP + C1)
     *     t = (C0 - CP) / (C0 - CP + C1 - CP)
     * Note that this method will return 0 if the equation is a line,
     * which is either always horizontal or never horizontal.
     * Completely horizontal curves need to be eliminated by other
     * means outside of this method.
     */
    public static int getHorizontalParams(double c0, double cp, double c1,
                                          double[] ret) {
        if (c0 <= cp && cp <= c1) {
            return 0;
        }
        c0 -= cp;
        c1 -= cp;
        double denom = c0 + c1;
        // If denom == 0 then cp == (c0+c1)/2 and we have a line.
        if (denom == 0) {
            return 0;
        }
        double t = c0 / denom;
        // No splits at t==0 and t==1
        if (t <= 0 || t >= 1) {
            return 0;
        }
        ret[0] = t;
        return 1;
    }

    /*
     * Split the quadratic Bezier stored at coords[pos...pos+5] representing
     * the paramtric range [0..1] into two subcurves representing the
     * parametric subranges [0..t] and [t..1].  Store the results back
     * into the array at coords[pos...pos+5] and coords[pos+4...pos+9].
     */
    public static void split(double[] coords, int pos, double t) {
        double x0, y0, cx, cy, x1, y1;
        coords[pos+8] = x1 = coords[pos+4];
        coords[pos+9] = y1 = coords[pos+5];
        cx = coords[pos+2];
        cy = coords[pos+3];
        x1 = cx + (x1 - cx) * t;
        y1 = cy + (y1 - cy) * t;
        x0 = coords[pos+0];
        y0 = coords[pos+1];
        x0 = x0 + (cx - x0) * t;
        y0 = y0 + (cy - y0) * t;
        cx = x0 + (x1 - x0) * t;
        cy = y0 + (y1 - y0) * t;
        coords[pos+2] = x0;
        coords[pos+3] = y0;
        coords[pos+4] = cx;
        coords[pos+5] = cy;
        coords[pos+6] = x1;
        coords[pos+7] = y1;
    }

    public Order2(double x0, double y0,
                  double cx0, double cy0,
                  double x1, double y1,
                  int direction)
    {
        super(direction);
        // REMIND: Better accuracy in the root finding methods would
        //  ensure that cy0 is in range.  As it stands, it is never
        //  more than "1 mantissa bit" out of range...
        if (cy0 < y0) {
            cy0 = y0;
        } else if (cy0 > y1) {
            cy0 = y1;
        }
        this.x0 = x0;
        this.y0 = y0;
        this.cx0 = cx0;
        this.cy0 = cy0;
        this.x1 = x1;
        this.y1 = y1;
        xmin = Math.min(Math.min(x0, x1), cx0);
        xmax = Math.max(Math.max(x0, x1), cx0);
        xcoeff0 = x0;
        xcoeff1 = cx0 + cx0 - x0 - x0;
        xcoeff2 = x0 - cx0 - cx0 + x1;
        ycoeff0 = y0;
        ycoeff1 = cy0 + cy0 - y0 - y0;
        ycoeff2 = y0 - cy0 - cy0 + y1;
    }

    public int getOrder() {
        return 2;
    }

    public double getXTop() {
        return x0;
    }

    public double getYTop() {
        return y0;
    }

    public double getXBot() {
        return x1;
    }

    public double getYBot() {
        return y1;
    }

    public double getXMin() {
        return xmin;
    }

    public double getXMax() {
        return xmax;
    }

    public double getX0() {
        return (direction == INCREASING) ? x0 : x1;
    }

    public double getY0() {
        return (direction == INCREASING) ? y0 : y1;
    }

    public double getCX0() {
        return cx0;
    }

    public double getCY0() {
        return cy0;
    }

    public double getX1() {
        return (direction == DECREASING) ? x0 : x1;
    }

    public double getY1() {
        return (direction == DECREASING) ? y0 : y1;
    }

    public double XforY(double y) {
        if (y <= y0) {
            return x0;
        }
        if (y >= y1) {
            return x1;
        }
        return XforT(TforY(y));
    }

    public double TforY(double y) {
        if (y <= y0) {
            return 0;
        }
        if (y >= y1) {
            return 1;
        }
        return TforY(y, ycoeff0, ycoeff1, ycoeff2);
    }

    public static double TforY(double y,
                               double ycoeff0, double ycoeff1, double ycoeff2)
    {
        // The caller should have already eliminated y values
        // outside of the y0 to y1 range.
        ycoeff0 -= y;
        if (ycoeff2 == 0.0) {
            // The quadratic parabola has degenerated to a line.
            // ycoeff1 should not be 0.0 since we have already eliminated
            // totally horizontal lines, but if it is, then we will generate
            // infinity here for the root, which will not be in the [0,1]
            // range so we will pass to the failure code below.
            double root = -ycoeff0 / ycoeff1;
            if (root >= 0 && root <= 1) {
                return root;
            }
        } else {
            // From Numerical Recipes, 5.6, Quadratic and Cubic Equations
            double d = ycoeff1 * ycoeff1 - 4.0 * ycoeff2 * ycoeff0;
            // If d < 0.0, then there are no roots
            if (d >= 0.0) {
                d = Math.sqrt(d);
                // For accuracy, calculate one root using:
                //     (-ycoeff1 +/- d) / 2ycoeff2
                // and the other using:
                //     2ycoeff0 / (-ycoeff1 +/- d)
                // Choose the sign of the +/- so that ycoeff1+d
                // gets larger in magnitude
                if (ycoeff1 < 0.0) {
                    d = -d;
                }
                double q = (ycoeff1 + d) / -2.0;
                // We already tested ycoeff2 for being 0 above
                double root = q / ycoeff2;
                if (root >= 0 && root <= 1) {
                    return root;
                }
                if (q != 0.0) {
                    root = ycoeff0 / q;
                    if (root >= 0 && root <= 1) {
                        return root;
                    }
                }
            }
        }
        /* We failed to find a root in [0,1].  What could have gone wrong?
         * First, remember that these curves are constructed to be monotonic
         * in Y and totally horizontal curves have already been eliminated.
         * Now keep in mind that the Y coefficients of the polynomial form
         * of the curve are calculated from the Y coordinates which define
         * our curve.  They should theoretically define the same curve,
         * but they can be off by a couple of bits of precision after the
         * math is done and so can represent a slightly modified curve.
         * This is normally not an issue except when we have solutions near
         * the endpoints.  Since the answers we get from solving the polynomial
         * may be off by a few bits that means that they could lie just a
         * few bits of precision outside the [0,1] range.
         *
         * Another problem could be that while the parametric curve defined
         * by the Y coordinates has a local minima or maxima at or just
         * outside of the endpoints, the polynomial form might express
         * that same min/max just inside of and just shy of the Y coordinate
         * of that endpoint.  In that case, if we solve for a Y coordinate
         * at or near that endpoint, we may be solving for a Y coordinate
         * that is below that minima or above that maxima and we would find
         * no solutions at all.
         *
         * In either case, we can assume that y is so near one of the
         * endpoints that we can just collapse it onto the nearest endpoint
         * without losing more than a couple of bits of precision.
         */
        // First calculate the midpoint between y0 and y1 and choose to
        // return either 0.0 or 1.0 depending on whether y is above
        // or below the midpoint...
        // Note that we subtracted y from ycoeff0 above so both y0 and y1
        // will be "relative to y" so we are really just looking at where
        // zero falls with respect to the "relative midpoint" here.
        double y0 = ycoeff0;
        double y1 = ycoeff0 + ycoeff1 + ycoeff2;
        return (0 < (y0 + y1) / 2) ? 0.0 : 1.0;
    }

    public double XforT(double t) {
        return (xcoeff2 * t + xcoeff1) * t + xcoeff0;
    }

    public double YforT(double t) {
        return (ycoeff2 * t + ycoeff1) * t + ycoeff0;
    }

    public double dXforT(double t, int deriv) {
        switch (deriv) {
        case 0:
            return (xcoeff2 * t + xcoeff1) * t + xcoeff0;
        case 1:
            return 2 * xcoeff2 * t + xcoeff1;
        case 2:
            return 2 * xcoeff2;
        default:
            return 0;
        }
    }

    public double dYforT(double t, int deriv) {
        switch (deriv) {
        case 0:
            return (ycoeff2 * t + ycoeff1) * t + ycoeff0;
        case 1:
            return 2 * ycoeff2 * t + ycoeff1;
        case 2:
            return 2 * ycoeff2;
        default:
            return 0;
        }
    }

    public double nextVertical(double t0, double t1) {
        double t = -xcoeff1 / (2 * xcoeff2);
        if (t > t0 && t < t1) {
            return t;
        }
        return t1;
    }

    public void enlarge(Rectangle2D r) {
        r.add(x0, y0);
        double t = -xcoeff1 / (2 * xcoeff2);
        if (t > 0 && t < 1) {
            r.add(XforT(t), YforT(t));
        }
        r.add(x1, y1);
    }

    public Curve getSubCurve(double ystart, double yend, int dir) {
        double t0, t1;
        if (ystart <= y0) {
            if (yend >= y1) {
                return getWithDirection(dir);
            }
            t0 = 0;
        } else {
            t0 = TforY(ystart, ycoeff0, ycoeff1, ycoeff2);
        }
        if (yend >= y1) {
            t1 = 1;
        } else {
            t1 = TforY(yend, ycoeff0, ycoeff1, ycoeff2);
        }
        double[] eqn = new double[10];
        eqn[0] = x0;
        eqn[1] = y0;
        eqn[2] = cx0;
        eqn[3] = cy0;
        eqn[4] = x1;
        eqn[5] = y1;
        if (t1 < 1) {
            split(eqn, 0, t1);
        }
        int i;
        if (t0 <= 0) {
            i = 0;
        } else {
            split(eqn, 0, t0 / t1);
            i = 4;
        }
        return new Order2(eqn[i+0], ystart,
                          eqn[i+2], eqn[i+3],
                          eqn[i+4], yend,
                          dir);
    }

    public Curve getReversedCurve() {
        return new Order2(x0, y0, cx0, cy0, x1, y1, -direction);
    }

    public int getSegment(double[] coords) {
        coords[0] = cx0;
        coords[1] = cy0;
        if (direction == INCREASING) {
            coords[2] = x1;
            coords[3] = y1;
        } else {
            coords[2] = x0;
            coords[3] = y0;
        }
        return PathIterator.SEG_QUADTO;
    }

    public String controlPointString() {
        return ("("+round(cx0)+", "+round(cy0)+"), ");
    }
}
