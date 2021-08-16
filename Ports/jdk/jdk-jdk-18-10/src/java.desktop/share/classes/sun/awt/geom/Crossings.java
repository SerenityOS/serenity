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

import java.awt.geom.PathIterator;
import java.util.Vector;
import java.util.Enumeration;

public abstract class Crossings {
    public static final boolean debug = false;

    int limit = 0;
    double[] yranges = new double[10];

    double xlo, ylo, xhi, yhi;

    public Crossings(double xlo, double ylo, double xhi, double yhi) {
        this.xlo = xlo;
        this.ylo = ylo;
        this.xhi = xhi;
        this.yhi = yhi;
    }

    public final double getXLo() {
        return xlo;
    }

    public final double getYLo() {
        return ylo;
    }

    public final double getXHi() {
        return xhi;
    }

    public final double getYHi() {
        return yhi;
    }

    public abstract void record(double ystart, double yend, int direction);

    public void print() {
        System.out.println("Crossings [");
        System.out.println("  bounds = ["+ylo+", "+yhi+"]");
        for (int i = 0; i < limit; i += 2) {
            System.out.println("  ["+yranges[i]+", "+yranges[i+1]+"]");
        }
        System.out.println("]");
    }

    public final boolean isEmpty() {
        return (limit == 0);
    }

    public abstract boolean covers(double ystart, double yend);

    public static Crossings findCrossings(Vector<? extends Curve> curves,
                                          double xlo, double ylo,
                                          double xhi, double yhi)
    {
        Crossings cross = new EvenOdd(xlo, ylo, xhi, yhi);
        Enumeration<? extends Curve> enum_ = curves.elements();
        while (enum_.hasMoreElements()) {
            Curve c = enum_.nextElement();
            if (c.accumulateCrossings(cross)) {
                return null;
            }
        }
        if (debug) {
            cross.print();
        }
        return cross;
    }

    public static Crossings findCrossings(PathIterator pi,
                                          double xlo, double ylo,
                                          double xhi, double yhi)
    {
        Crossings cross;
        if (pi.getWindingRule() == PathIterator.WIND_EVEN_ODD) {
            cross = new EvenOdd(xlo, ylo, xhi, yhi);
        } else {
            cross = new NonZero(xlo, ylo, xhi, yhi);
        }
        // coords array is big enough for holding:
        //     coordinates returned from currentSegment (6)
        //     OR
        //         two subdivided quadratic curves (2+4+4=10)
        //         AND
        //             0-1 horizontal splitting parameters
        //             OR
        //             2 parametric equation derivative coefficients
        //     OR
        //         three subdivided cubic curves (2+6+6+6=20)
        //         AND
        //             0-2 horizontal splitting parameters
        //             OR
        //             3 parametric equation derivative coefficients
        double[] coords = new double[23];
        double movx = 0;
        double movy = 0;
        double curx = 0;
        double cury = 0;
        double newx, newy;
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            switch (type) {
            case PathIterator.SEG_MOVETO:
                if (movy != cury &&
                    cross.accumulateLine(curx, cury, movx, movy))
                {
                    return null;
                }
                movx = curx = coords[0];
                movy = cury = coords[1];
                break;
            case PathIterator.SEG_LINETO:
                newx = coords[0];
                newy = coords[1];
                if (cross.accumulateLine(curx, cury, newx, newy)) {
                    return null;
                }
                curx = newx;
                cury = newy;
                break;
            case PathIterator.SEG_QUADTO:
                newx = coords[2];
                newy = coords[3];
                if (cross.accumulateQuad(curx, cury, coords)) {
                    return null;
                }
                curx = newx;
                cury = newy;
                break;
            case PathIterator.SEG_CUBICTO:
                newx = coords[4];
                newy = coords[5];
                if (cross.accumulateCubic(curx, cury, coords)) {
                    return null;
                }
                curx = newx;
                cury = newy;
                break;
            case PathIterator.SEG_CLOSE:
                if (movy != cury &&
                    cross.accumulateLine(curx, cury, movx, movy))
                {
                    return null;
                }
                curx = movx;
                cury = movy;
                break;
            }
            pi.next();
        }
        if (movy != cury) {
            if (cross.accumulateLine(curx, cury, movx, movy)) {
                return null;
            }
        }
        if (debug) {
            cross.print();
        }
        return cross;
    }

    public boolean accumulateLine(double x0, double y0,
                                  double x1, double y1)
    {
        if (y0 <= y1) {
            return accumulateLine(x0, y0, x1, y1, 1);
        } else {
            return accumulateLine(x1, y1, x0, y0, -1);
        }
    }

    public boolean accumulateLine(double x0, double y0,
                                  double x1, double y1,
                                  int direction)
    {
        if (yhi <= y0 || ylo >= y1) {
            return false;
        }
        if (x0 >= xhi && x1 >= xhi) {
            return false;
        }
        if (y0 == y1) {
            return (x0 >= xlo || x1 >= xlo);
        }
        double xstart, ystart, xend, yend;
        double dx = (x1 - x0);
        double dy = (y1 - y0);
        if (y0 < ylo) {
            xstart = x0 + (ylo - y0) * dx / dy;
            ystart = ylo;
        } else {
            xstart = x0;
            ystart = y0;
        }
        if (yhi < y1) {
            xend = x0 + (yhi - y0) * dx / dy;
            yend = yhi;
        } else {
            xend = x1;
            yend = y1;
        }
        if (xstart >= xhi && xend >= xhi) {
            return false;
        }
        if (xstart > xlo || xend > xlo) {
            return true;
        }
        record(ystart, yend, direction);
        return false;
    }

    private Vector<Curve> tmp = new Vector<>();

    public boolean accumulateQuad(double x0, double y0, double[] coords) {
        if (y0 < ylo && coords[1] < ylo && coords[3] < ylo) {
            return false;
        }
        if (y0 > yhi && coords[1] > yhi && coords[3] > yhi) {
            return false;
        }
        if (x0 > xhi && coords[0] > xhi && coords[2] > xhi) {
            return false;
        }
        if (x0 < xlo && coords[0] < xlo && coords[2] < xlo) {
            if (y0 < coords[3]) {
                record(Math.max(y0, ylo), Math.min(coords[3], yhi), 1);
            } else if (y0 > coords[3]) {
                record(Math.max(coords[3], ylo), Math.min(y0, yhi), -1);
            }
            return false;
        }
        Curve.insertQuad(tmp, x0, y0, coords);
        Enumeration<Curve> enum_ = tmp.elements();
        while (enum_.hasMoreElements()) {
            Curve c = enum_.nextElement();
            if (c.accumulateCrossings(this)) {
                return true;
            }
        }
        tmp.clear();
        return false;
    }

    public boolean accumulateCubic(double x0, double y0, double[] coords) {
        if (y0 < ylo && coords[1] < ylo &&
            coords[3] < ylo && coords[5] < ylo)
        {
            return false;
        }
        if (y0 > yhi && coords[1] > yhi &&
            coords[3] > yhi && coords[5] > yhi)
        {
            return false;
        }
        if (x0 > xhi && coords[0] > xhi &&
            coords[2] > xhi && coords[4] > xhi)
        {
            return false;
        }
        if (x0 < xlo && coords[0] < xlo &&
            coords[2] < xlo && coords[4] < xlo)
        {
            if (y0 <= coords[5]) {
                record(Math.max(y0, ylo), Math.min(coords[5], yhi), 1);
            } else {
                record(Math.max(coords[5], ylo), Math.min(y0, yhi), -1);
            }
            return false;
        }
        Curve.insertCubic(tmp, x0, y0, coords);
        Enumeration<Curve> enum_ = tmp.elements();
        while (enum_.hasMoreElements()) {
            Curve c = enum_.nextElement();
            if (c.accumulateCrossings(this)) {
                return true;
            }
        }
        tmp.clear();
        return false;
    }

    public static final class EvenOdd extends Crossings {
        public EvenOdd(double xlo, double ylo, double xhi, double yhi) {
            super(xlo, ylo, xhi, yhi);
        }

        public boolean covers(double ystart, double yend) {
            return (limit == 2 && yranges[0] <= ystart && yranges[1] >= yend);
        }

        public void record(double ystart, double yend, int direction) {
            if (ystart >= yend) {
                return;
            }
            int from = 0;
            // Quickly jump over all pairs that are completely "above"
            while (from < limit && ystart > yranges[from+1]) {
                from += 2;
            }
            int to = from;
            while (from < limit) {
                double yrlo = yranges[from++];
                double yrhi = yranges[from++];
                if (yend < yrlo) {
                    // Quickly handle insertion of the new range
                    yranges[to++] = ystart;
                    yranges[to++] = yend;
                    ystart = yrlo;
                    yend = yrhi;
                    continue;
                }
                // The ranges overlap - sort, collapse, insert, iterate
                double yll, ylh, yhl, yhh;
                if (ystart < yrlo) {
                    yll = ystart;
                    ylh = yrlo;
                } else {
                    yll = yrlo;
                    ylh = ystart;
                }
                if (yend < yrhi) {
                    yhl = yend;
                    yhh = yrhi;
                } else {
                    yhl = yrhi;
                    yhh = yend;
                }
                if (ylh == yhl) {
                    ystart = yll;
                    yend = yhh;
                } else {
                    if (ylh > yhl) {
                        ystart = yhl;
                        yhl = ylh;
                        ylh = ystart;
                    }
                    if (yll != ylh) {
                        yranges[to++] = yll;
                        yranges[to++] = ylh;
                    }
                    ystart = yhl;
                    yend = yhh;
                }
                if (ystart >= yend) {
                    break;
                }
            }
            if (to < from && from < limit) {
                System.arraycopy(yranges, from, yranges, to, limit-from);
            }
            to += (limit-from);
            if (ystart < yend) {
                if (to >= yranges.length) {
                    double[] newranges = new double[to+10];
                    System.arraycopy(yranges, 0, newranges, 0, to);
                    yranges = newranges;
                }
                yranges[to++] = ystart;
                yranges[to++] = yend;
            }
            limit = to;
        }
    }

    public static final class NonZero extends Crossings {
        private int[] crosscounts;

        public NonZero(double xlo, double ylo, double xhi, double yhi) {
            super(xlo, ylo, xhi, yhi);
            crosscounts = new int[yranges.length / 2];
        }

        public boolean covers(double ystart, double yend) {
            int i = 0;
            while (i < limit) {
                double ylo = yranges[i++];
                double yhi = yranges[i++];
                if (ystart >= yhi) {
                    continue;
                }
                if (ystart < ylo) {
                    return false;
                }
                if (yend <= yhi) {
                    return true;
                }
                ystart = yhi;
            }
            return (ystart >= yend);
        }

        public void remove(int cur) {
            limit -= 2;
            int rem = limit - cur;
            if (rem > 0) {
                System.arraycopy(yranges, cur+2, yranges, cur, rem);
                System.arraycopy(crosscounts, cur/2+1,
                                 crosscounts, cur/2,
                                 rem/2);
            }
        }

        public void insert(int cur, double lo, double hi, int dir) {
            int rem = limit - cur;
            double[] oldranges = yranges;
            int[] oldcounts = crosscounts;
            if (limit >= yranges.length) {
                yranges = new double[limit+10];
                System.arraycopy(oldranges, 0, yranges, 0, cur);
                crosscounts = new int[(limit+10)/2];
                System.arraycopy(oldcounts, 0, crosscounts, 0, cur/2);
            }
            if (rem > 0) {
                System.arraycopy(oldranges, cur, yranges, cur+2, rem);
                System.arraycopy(oldcounts, cur/2,
                                 crosscounts, cur/2+1,
                                 rem/2);
            }
            yranges[cur+0] = lo;
            yranges[cur+1] = hi;
            crosscounts[cur/2] = dir;
            limit += 2;
        }

        public void record(double ystart, double yend, int direction) {
            if (ystart >= yend) {
                return;
            }
            int cur = 0;
            // Quickly jump over all pairs that are completely "above"
            while (cur < limit && ystart > yranges[cur+1]) {
                cur += 2;
            }
            if (cur < limit) {
                int rdir = crosscounts[cur/2];
                double yrlo = yranges[cur+0];
                double yrhi = yranges[cur+1];
                if (yrhi == ystart && rdir == direction) {
                    // Remove the range from the list and collapse it
                    // into the range being inserted.  Note that the
                    // new combined range may overlap the following range
                    // so we must not simply combine the ranges in place
                    // unless we are at the last range.
                    if (cur+2 == limit) {
                        yranges[cur+1] = yend;
                        return;
                    }
                    remove(cur);
                    ystart = yrlo;
                    rdir = crosscounts[cur/2];
                    yrlo = yranges[cur+0];
                    yrhi = yranges[cur+1];
                }
                if (yend < yrlo) {
                    // Just insert the new range at the current location
                    insert(cur, ystart, yend, direction);
                    return;
                }
                if (yend == yrlo && rdir == direction) {
                    // Just prepend the new range to the current one
                    yranges[cur] = ystart;
                    return;
                }
                // The ranges must overlap - (yend > yrlo && yrhi > ystart)
                if (ystart < yrlo) {
                    insert(cur, ystart, yrlo, direction);
                    cur += 2;
                    ystart = yrlo;
                } else if (yrlo < ystart) {
                    insert(cur, yrlo, ystart, rdir);
                    cur += 2;
                    yrlo = ystart;
                }
                // assert(yrlo == ystart);
                int newdir = rdir + direction;
                double newend = Math.min(yend, yrhi);
                if (newdir == 0) {
                    remove(cur);
                } else {
                    crosscounts[cur/2] = newdir;
                    yranges[cur++] = ystart;
                    yranges[cur++] = newend;
                }
                ystart = yrlo = newend;
                if (yrlo < yrhi) {
                    insert(cur, yrlo, yrhi, rdir);
                }
            }
            if (ystart < yend) {
                insert(cur, ystart, yend, direction);
            }
        }
    }
}
