/*
 * Copyright (c) 1997, 2003, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.geom;

import java.util.*;

/**
 * A utility class to iterate over the path segments of an arc
 * through the PathIterator interface.
 *
 * @author      Jim Graham
 */
class ArcIterator implements PathIterator {
    double x, y, w, h, angStRad, increment, cv;
    AffineTransform affine;
    int index;
    int arcSegs;
    int lineSegs;

    ArcIterator(Arc2D a, AffineTransform at) {
        this.w = a.getWidth() / 2;
        this.h = a.getHeight() / 2;
        this.x = a.getX() + w;
        this.y = a.getY() + h;
        this.angStRad = -Math.toRadians(a.getAngleStart());
        this.affine = at;
        double ext = -a.getAngleExtent();
        if (ext >= 360.0 || ext <= -360) {
            arcSegs = 4;
            this.increment = Math.PI / 2;
            // btan(Math.PI / 2);
            this.cv = 0.5522847498307933;
            if (ext < 0) {
                increment = -increment;
                cv = -cv;
            }
        } else {
            arcSegs = (int) Math.ceil(Math.abs(ext) / 90.0);
            this.increment = Math.toRadians(ext / arcSegs);
            this.cv = btan(increment);
            if (cv == 0) {
                arcSegs = 0;
            }
        }
        switch (a.getArcType()) {
        case Arc2D.OPEN:
            lineSegs = 0;
            break;
        case Arc2D.CHORD:
            lineSegs = 1;
            break;
        case Arc2D.PIE:
            lineSegs = 2;
            break;
        }
        if (w < 0 || h < 0) {
            arcSegs = lineSegs = -1;
        }
    }

    /**
     * Return the winding rule for determining the insideness of the
     * path.
     * @see #WIND_EVEN_ODD
     * @see #WIND_NON_ZERO
     */
    public int getWindingRule() {
        return WIND_NON_ZERO;
    }

    /**
     * Tests if there are more points to read.
     * @return true if there are more points to read
     */
    public boolean isDone() {
        return index > arcSegs + lineSegs;
    }

    /**
     * Moves the iterator to the next segment of the path forwards
     * along the primary direction of traversal as long as there are
     * more points in that direction.
     */
    public void next() {
        index++;
    }

    /*
     * btan computes the length (k) of the control segments at
     * the beginning and end of a cubic bezier that approximates
     * a segment of an arc with extent less than or equal to
     * 90 degrees.  This length (k) will be used to generate the
     * 2 bezier control points for such a segment.
     *
     *   Assumptions:
     *     a) arc is centered on 0,0 with radius of 1.0
     *     b) arc extent is less than 90 degrees
     *     c) control points should preserve tangent
     *     d) control segments should have equal length
     *
     *   Initial data:
     *     start angle: ang1
     *     end angle:   ang2 = ang1 + extent
     *     start point: P1 = (x1, y1) = (cos(ang1), sin(ang1))
     *     end point:   P4 = (x4, y4) = (cos(ang2), sin(ang2))
     *
     *   Control points:
     *     P2 = (x2, y2)
     *     | x2 = x1 - k * sin(ang1) = cos(ang1) - k * sin(ang1)
     *     | y2 = y1 + k * cos(ang1) = sin(ang1) + k * cos(ang1)
     *
     *     P3 = (x3, y3)
     *     | x3 = x4 + k * sin(ang2) = cos(ang2) + k * sin(ang2)
     *     | y3 = y4 - k * cos(ang2) = sin(ang2) - k * cos(ang2)
     *
     * The formula for this length (k) can be found using the
     * following derivations:
     *
     *   Midpoints:
     *     a) bezier (t = 1/2)
     *        bPm = P1 * (1-t)^3 +
     *              3 * P2 * t * (1-t)^2 +
     *              3 * P3 * t^2 * (1-t) +
     *              P4 * t^3 =
     *            = (P1 + 3P2 + 3P3 + P4)/8
     *
     *     b) arc
     *        aPm = (cos((ang1 + ang2)/2), sin((ang1 + ang2)/2))
     *
     *   Let angb = (ang2 - ang1)/2; angb is half of the angle
     *   between ang1 and ang2.
     *
     *   Solve the equation bPm == aPm
     *
     *     a) For xm coord:
     *        x1 + 3*x2 + 3*x3 + x4 = 8*cos((ang1 + ang2)/2)
     *
     *        cos(ang1) + 3*cos(ang1) - 3*k*sin(ang1) +
     *        3*cos(ang2) + 3*k*sin(ang2) + cos(ang2) =
     *        = 8*cos((ang1 + ang2)/2)
     *
     *        4*cos(ang1) + 4*cos(ang2) + 3*k*(sin(ang2) - sin(ang1)) =
     *        = 8*cos((ang1 + ang2)/2)
     *
     *        8*cos((ang1 + ang2)/2)*cos((ang2 - ang1)/2) +
     *        6*k*sin((ang2 - ang1)/2)*cos((ang1 + ang2)/2) =
     *        = 8*cos((ang1 + ang2)/2)
     *
     *        4*cos(angb) + 3*k*sin(angb) = 4
     *
     *        k = 4 / 3 * (1 - cos(angb)) / sin(angb)
     *
     *     b) For ym coord we derive the same formula.
     *
     * Since this formula can generate "NaN" values for small
     * angles, we will derive a safer form that does not involve
     * dividing by very small values:
     *     (1 - cos(angb)) / sin(angb) =
     *     = (1 - cos(angb))*(1 + cos(angb)) / sin(angb)*(1 + cos(angb)) =
     *     = (1 - cos(angb)^2) / sin(angb)*(1 + cos(angb)) =
     *     = sin(angb)^2 / sin(angb)*(1 + cos(angb)) =
     *     = sin(angb) / (1 + cos(angb))
     *
     */
    private static double btan(double increment) {
        increment /= 2.0;
        return 4.0 / 3.0 * Math.sin(increment) / (1.0 + Math.cos(increment));
    }

    /**
     * Returns the coordinates and type of the current path segment in
     * the iteration.
     * The return value is the path segment type:
     * SEG_MOVETO, SEG_LINETO, SEG_QUADTO, SEG_CUBICTO, or SEG_CLOSE.
     * A float array of length 6 must be passed in and may be used to
     * store the coordinates of the point(s).
     * Each point is stored as a pair of float x,y coordinates.
     * SEG_MOVETO and SEG_LINETO types will return one point,
     * SEG_QUADTO will return two points,
     * SEG_CUBICTO will return 3 points
     * and SEG_CLOSE will not return any points.
     * @see #SEG_MOVETO
     * @see #SEG_LINETO
     * @see #SEG_QUADTO
     * @see #SEG_CUBICTO
     * @see #SEG_CLOSE
     */
    public int currentSegment(float[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("arc iterator out of bounds");
        }
        double angle = angStRad;
        if (index == 0) {
            coords[0] = (float) (x + Math.cos(angle) * w);
            coords[1] = (float) (y + Math.sin(angle) * h);
            if (affine != null) {
                affine.transform(coords, 0, coords, 0, 1);
            }
            return SEG_MOVETO;
        }
        if (index > arcSegs) {
            if (index == arcSegs + lineSegs) {
                return SEG_CLOSE;
            }
            coords[0] = (float) x;
            coords[1] = (float) y;
            if (affine != null) {
                affine.transform(coords, 0, coords, 0, 1);
            }
            return SEG_LINETO;
        }
        angle += increment * (index - 1);
        double relx = Math.cos(angle);
        double rely = Math.sin(angle);
        coords[0] = (float) (x + (relx - cv * rely) * w);
        coords[1] = (float) (y + (rely + cv * relx) * h);
        angle += increment;
        relx = Math.cos(angle);
        rely = Math.sin(angle);
        coords[2] = (float) (x + (relx + cv * rely) * w);
        coords[3] = (float) (y + (rely - cv * relx) * h);
        coords[4] = (float) (x + relx * w);
        coords[5] = (float) (y + rely * h);
        if (affine != null) {
            affine.transform(coords, 0, coords, 0, 3);
        }
        return SEG_CUBICTO;
    }

    /**
     * Returns the coordinates and type of the current path segment in
     * the iteration.
     * The return value is the path segment type:
     * SEG_MOVETO, SEG_LINETO, SEG_QUADTO, SEG_CUBICTO, or SEG_CLOSE.
     * A double array of length 6 must be passed in and may be used to
     * store the coordinates of the point(s).
     * Each point is stored as a pair of double x,y coordinates.
     * SEG_MOVETO and SEG_LINETO types will return one point,
     * SEG_QUADTO will return two points,
     * SEG_CUBICTO will return 3 points
     * and SEG_CLOSE will not return any points.
     * @see #SEG_MOVETO
     * @see #SEG_LINETO
     * @see #SEG_QUADTO
     * @see #SEG_CUBICTO
     * @see #SEG_CLOSE
     */
    public int currentSegment(double[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("arc iterator out of bounds");
        }
        double angle = angStRad;
        if (index == 0) {
            coords[0] = x + Math.cos(angle) * w;
            coords[1] = y + Math.sin(angle) * h;
            if (affine != null) {
                affine.transform(coords, 0, coords, 0, 1);
            }
            return SEG_MOVETO;
        }
        if (index > arcSegs) {
            if (index == arcSegs + lineSegs) {
                return SEG_CLOSE;
            }
            coords[0] = x;
            coords[1] = y;
            if (affine != null) {
                affine.transform(coords, 0, coords, 0, 1);
            }
            return SEG_LINETO;
        }
        angle += increment * (index - 1);
        double relx = Math.cos(angle);
        double rely = Math.sin(angle);
        coords[0] = x + (relx - cv * rely) * w;
        coords[1] = y + (rely + cv * relx) * h;
        angle += increment;
        relx = Math.cos(angle);
        rely = Math.sin(angle);
        coords[2] = x + (relx + cv * rely) * w;
        coords[3] = y + (rely - cv * relx) * h;
        coords[4] = x + relx * w;
        coords[5] = y + rely * h;
        if (affine != null) {
            affine.transform(coords, 0, coords, 0, 3);
        }
        return SEG_CUBICTO;
    }
}
