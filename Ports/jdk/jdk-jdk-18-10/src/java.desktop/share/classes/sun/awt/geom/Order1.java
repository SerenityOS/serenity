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
import java.util.Vector;

final class Order1 extends Curve {
    private double x0;
    private double y0;
    private double x1;
    private double y1;
    private double xmin;
    private double xmax;

    public Order1(double x0, double y0,
                  double x1, double y1,
                  int direction)
    {
        super(direction);
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        if (x0 < x1) {
            this.xmin = x0;
            this.xmax = x1;
        } else {
            this.xmin = x1;
            this.xmax = x0;
        }
    }

    public int getOrder() {
        return 1;
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

    public double getX1() {
        return (direction == DECREASING) ? x0 : x1;
    }

    public double getY1() {
        return (direction == DECREASING) ? y0 : y1;
    }

    public double XforY(double y) {
        if (x0 == x1 || y <= y0) {
            return x0;
        }
        if (y >= y1) {
            return x1;
        }
        // assert(y0 != y1); /* No horizontal lines... */
        return (x0 + (y - y0) * (x1 - x0) / (y1 - y0));
    }

    public double TforY(double y) {
        if (y <= y0) {
            return 0;
        }
        if (y >= y1) {
            return 1;
        }
        return (y - y0) / (y1 - y0);
    }

    public double XforT(double t) {
        return x0 + t * (x1 - x0);
    }

    public double YforT(double t) {
        return y0 + t * (y1 - y0);
    }

    public double dXforT(double t, int deriv) {
        switch (deriv) {
        case 0:
            return x0 + t * (x1 - x0);
        case 1:
            return (x1 - x0);
        default:
            return 0;
        }
    }

    public double dYforT(double t, int deriv) {
        switch (deriv) {
        case 0:
            return y0 + t * (y1 - y0);
        case 1:
            return (y1 - y0);
        default:
            return 0;
        }
    }

    public double nextVertical(double t0, double t1) {
        return t1;
    }

    public boolean accumulateCrossings(Crossings c) {
        double xlo = c.getXLo();
        double ylo = c.getYLo();
        double xhi = c.getXHi();
        double yhi = c.getYHi();
        if (xmin >= xhi) {
            return false;
        }
        double xstart, ystart, xend, yend;
        if (y0 < ylo) {
            if (y1 <= ylo) {
                return false;
            }
            ystart = ylo;
            xstart = XforY(ylo);
        } else {
            if (y0 >= yhi) {
                return false;
            }
            ystart = y0;
            xstart = x0;
        }
        if (y1 > yhi) {
            yend = yhi;
            xend = XforY(yhi);
        } else {
            yend = y1;
            xend = x1;
        }
        if (xstart >= xhi && xend >= xhi) {
            return false;
        }
        if (xstart > xlo || xend > xlo) {
            return true;
        }
        c.record(ystart, yend, direction);
        return false;
    }

    public void enlarge(Rectangle2D r) {
        r.add(x0, y0);
        r.add(x1, y1);
    }

    public Curve getSubCurve(double ystart, double yend, int dir) {
        if (ystart == y0 && yend == y1) {
            return getWithDirection(dir);
        }
        if (x0 == x1) {
            return new Order1(x0, ystart, x1, yend, dir);
        }
        double num = x0 - x1;
        double denom = y0 - y1;
        double xstart = (x0 + (ystart - y0) * num / denom);
        double xend = (x0 + (yend - y0) * num / denom);
        return new Order1(xstart, ystart, xend, yend, dir);
    }

    public Curve getReversedCurve() {
        return new Order1(x0, y0, x1, y1, -direction);
    }

    public int compareTo(Curve other, double[] yrange) {
        if (!(other instanceof Order1)) {
            return super.compareTo(other, yrange);
        }
        Order1 c1 = (Order1) other;
        if (yrange[1] <= yrange[0]) {
            throw new InternalError("yrange already screwed up...");
        }
        yrange[1] = Math.min(Math.min(yrange[1], y1), c1.y1);
        if (yrange[1] <= yrange[0]) {
            throw new InternalError("backstepping from "+yrange[0]+" to "+yrange[1]);
        }
        if (xmax <= c1.xmin) {
            return (xmin == c1.xmax) ? 0 : -1;
        }
        if (xmin >= c1.xmax) {
            return 1;
        }
        /*
         * If "this" is curve A and "other" is curve B, then...
         * xA(y) = x0A + (y - y0A) (x1A - x0A) / (y1A - y0A)
         * xB(y) = x0B + (y - y0B) (x1B - x0B) / (y1B - y0B)
         * xA(y) == xB(y)
         * x0A + (y - y0A) (x1A - x0A) / (y1A - y0A)
         *    == x0B + (y - y0B) (x1B - x0B) / (y1B - y0B)
         * 0 == x0A (y1A - y0A) (y1B - y0B) + (y - y0A) (x1A - x0A) (y1B - y0B)
         *    - x0B (y1A - y0A) (y1B - y0B) - (y - y0B) (x1B - x0B) (y1A - y0A)
         * 0 == (x0A - x0B) (y1A - y0A) (y1B - y0B)
         *    + (y - y0A) (x1A - x0A) (y1B - y0B)
         *    - (y - y0B) (x1B - x0B) (y1A - y0A)
         * If (dxA == x1A - x0A), etc...
         * 0 == (x0A - x0B) * dyA * dyB
         *    + (y - y0A) * dxA * dyB
         *    - (y - y0B) * dxB * dyA
         * 0 == (x0A - x0B) * dyA * dyB
         *    + y * dxA * dyB - y0A * dxA * dyB
         *    - y * dxB * dyA + y0B * dxB * dyA
         * 0 == (x0A - x0B) * dyA * dyB
         *    + y * dxA * dyB - y * dxB * dyA
         *    - y0A * dxA * dyB + y0B * dxB * dyA
         * 0 == (x0A - x0B) * dyA * dyB
         *    + y * (dxA * dyB - dxB * dyA)
         *    - y0A * dxA * dyB + y0B * dxB * dyA
         * y == ((x0A - x0B) * dyA * dyB
         *       - y0A * dxA * dyB + y0B * dxB * dyA)
         *    / (-(dxA * dyB - dxB * dyA))
         * y == ((x0A - x0B) * dyA * dyB
         *       - y0A * dxA * dyB + y0B * dxB * dyA)
         *    / (dxB * dyA - dxA * dyB)
         */
        double dxa = x1 - x0;
        double dya = y1 - y0;
        double dxb = c1.x1 - c1.x0;
        double dyb = c1.y1 - c1.y0;
        double denom = dxb * dya - dxa * dyb;
        double y;
        if (denom != 0) {
            double num = ((x0 - c1.x0) * dya * dyb
                          - y0 * dxa * dyb
                          + c1.y0 * dxb * dya);
            y = num / denom;
            if (y <= yrange[0]) {
                // intersection is above us
                // Use bottom-most common y for comparison
                y = Math.min(y1, c1.y1);
            } else {
                // intersection is below the top of our range
                if (y < yrange[1]) {
                    // If intersection is in our range, adjust valid range
                    yrange[1] = y;
                }
                // Use top-most common y for comparison
                y = Math.max(y0, c1.y0);
            }
        } else {
            // lines are parallel, choose any common y for comparison
            // Note - prefer an endpoint for speed of calculating the X
            // (see shortcuts in Order1.XforY())
            y = Math.max(y0, c1.y0);
        }
        return orderof(XforY(y), c1.XforY(y));
    }

    public int getSegment(double[] coords) {
        if (direction == INCREASING) {
            coords[0] = x1;
            coords[1] = y1;
        } else {
            coords[0] = x0;
            coords[1] = y0;
        }
        return PathIterator.SEG_LINETO;
    }
}
