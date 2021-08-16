/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Rectangle;
import java.awt.Shape;
import java.io.Serial;
import java.io.Serializable;

/**
 * This {@code Line2D} represents a line segment in {@code (x,y)}
 * coordinate space.
 * <p>
 * This class is only the abstract superclass for all objects that
 * store a 2D line segment.
 * The actual storage representation of the coordinates is left to
 * the subclass.
 *
 * @author      Jim Graham
 * @since 1.2
 */
public abstract class Line2D implements Shape, Cloneable {

    /**
     * A line segment specified with float coordinates.
     * @since 1.2
     */
    public static class Float extends Line2D implements Serializable {
        /**
         * The X coordinate of the start point of the line segment.
         * @since 1.2
         * @serial
         */
        public float x1;

        /**
         * The Y coordinate of the start point of the line segment.
         * @since 1.2
         * @serial
         */
        public float y1;

        /**
         * The X coordinate of the end point of the line segment.
         * @since 1.2
         * @serial
         */
        public float x2;

        /**
         * The Y coordinate of the end point of the line segment.
         * @since 1.2
         * @serial
         */
        public float y2;

        /**
         * Constructs and initializes a Line with coordinates (0, 0) &rarr; (0, 0).
         * @since 1.2
         */
        public Float() {
        }

        /**
         * Constructs and initializes a Line from the specified coordinates.
         * @param x1 the X coordinate of the start point
         * @param y1 the Y coordinate of the start point
         * @param x2 the X coordinate of the end point
         * @param y2 the Y coordinate of the end point
         * @since 1.2
         */
        public Float(float x1, float y1, float x2, float y2) {
            setLine(x1, y1, x2, y2);
        }

        /**
         * Constructs and initializes a {@code Line2D} from the
         * specified {@code Point2D} objects.
         * @param p1 the start {@code Point2D} of this line segment
         * @param p2 the end {@code Point2D} of this line segment
         * @since 1.2
         */
        public Float(Point2D p1, Point2D p2) {
            setLine(p1, p2);
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public double getX1() {
            return (double) x1;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public double getY1() {
            return (double) y1;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public Point2D getP1() {
            return new Point2D.Float(x1, y1);
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public double getX2() {
            return (double) x2;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public double getY2() {
            return (double) y2;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public Point2D getP2() {
            return new Point2D.Float(x2, y2);
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public void setLine(double x1, double y1, double x2, double y2) {
            this.x1 = (float) x1;
            this.y1 = (float) y1;
            this.x2 = (float) x2;
            this.y2 = (float) y2;
        }

        /**
         * Sets the location of the end points of this {@code Line2D}
         * to the specified float coordinates.
         * @param x1 the X coordinate of the start point
         * @param y1 the Y coordinate of the start point
         * @param x2 the X coordinate of the end point
         * @param y2 the Y coordinate of the end point
         * @since 1.2
         */
        public void setLine(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public Rectangle2D getBounds2D() {
            float x, y, w, h;
            if (x1 < x2) {
                x = x1;
                w = x2 - x1;
            } else {
                x = x2;
                w = x1 - x2;
            }
            if (y1 < y2) {
                y = y1;
                h = y2 - y1;
            } else {
                y = y2;
                h = y1 - y2;
            }
            return new Rectangle2D.Float(x, y, w, h);
        }

        /**
         * Use serialVersionUID from JDK 1.6 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 6161772511649436349L;
    }

    /**
     * A line segment specified with double coordinates.
     * @since 1.2
     */
    public static class Double extends Line2D implements Serializable {
        /**
         * The X coordinate of the start point of the line segment.
         * @since 1.2
         * @serial
         */
        public double x1;

        /**
         * The Y coordinate of the start point of the line segment.
         * @since 1.2
         * @serial
         */
        public double y1;

        /**
         * The X coordinate of the end point of the line segment.
         * @since 1.2
         * @serial
         */
        public double x2;

        /**
         * The Y coordinate of the end point of the line segment.
         * @since 1.2
         * @serial
         */
        public double y2;

        /**
         * Constructs and initializes a Line with coordinates (0, 0) &rarr; (0, 0).
         * @since 1.2
         */
        public Double() {
        }

        /**
         * Constructs and initializes a {@code Line2D} from the
         * specified coordinates.
         * @param x1 the X coordinate of the start point
         * @param y1 the Y coordinate of the start point
         * @param x2 the X coordinate of the end point
         * @param y2 the Y coordinate of the end point
         * @since 1.2
         */
        public Double(double x1, double y1, double x2, double y2) {
            setLine(x1, y1, x2, y2);
        }

        /**
         * Constructs and initializes a {@code Line2D} from the
         * specified {@code Point2D} objects.
         * @param p1 the start {@code Point2D} of this line segment
         * @param p2 the end {@code Point2D} of this line segment
         * @since 1.2
         */
        public Double(Point2D p1, Point2D p2) {
            setLine(p1, p2);
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public double getX1() {
            return x1;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public double getY1() {
            return y1;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public Point2D getP1() {
            return new Point2D.Double(x1, y1);
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public double getX2() {
            return x2;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public double getY2() {
            return y2;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public Point2D getP2() {
            return new Point2D.Double(x2, y2);
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public void setLine(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public Rectangle2D getBounds2D() {
            double x, y, w, h;
            if (x1 < x2) {
                x = x1;
                w = x2 - x1;
            } else {
                x = x2;
                w = x1 - x2;
            }
            if (y1 < y2) {
                y = y1;
                h = y2 - y1;
            } else {
                y = y2;
                h = y1 - y2;
            }
            return new Rectangle2D.Double(x, y, w, h);
        }

        /**
         * Use serialVersionUID from JDK 1.6 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 7979627399746467499L;
    }

    /**
     * This is an abstract class that cannot be instantiated directly.
     * Type-specific implementation subclasses are available for
     * instantiation and provide a number of formats for storing
     * the information necessary to satisfy the various accessory
     * methods below.
     *
     * @see java.awt.geom.Line2D.Float
     * @see java.awt.geom.Line2D.Double
     * @since 1.2
     */
    protected Line2D() {
    }

    /**
     * Returns the X coordinate of the start point in double precision.
     * @return the X coordinate of the start point of this
     *         {@code Line2D} object.
     * @since 1.2
     */
    public abstract double getX1();

    /**
     * Returns the Y coordinate of the start point in double precision.
     * @return the Y coordinate of the start point of this
     *         {@code Line2D} object.
     * @since 1.2
     */
    public abstract double getY1();

    /**
     * Returns the start {@code Point2D} of this {@code Line2D}.
     * @return the start {@code Point2D} of this {@code Line2D}.
     * @since 1.2
     */
    public abstract Point2D getP1();

    /**
     * Returns the X coordinate of the end point in double precision.
     * @return the X coordinate of the end point of this
     *         {@code Line2D} object.
     * @since 1.2
     */
    public abstract double getX2();

    /**
     * Returns the Y coordinate of the end point in double precision.
     * @return the Y coordinate of the end point of this
     *         {@code Line2D} object.
     * @since 1.2
     */
    public abstract double getY2();

    /**
     * Returns the end {@code Point2D} of this {@code Line2D}.
     * @return the end {@code Point2D} of this {@code Line2D}.
     * @since 1.2
     */
    public abstract Point2D getP2();

    /**
     * Sets the location of the end points of this {@code Line2D} to
     * the specified double coordinates.
     * @param x1 the X coordinate of the start point
     * @param y1 the Y coordinate of the start point
     * @param x2 the X coordinate of the end point
     * @param y2 the Y coordinate of the end point
     * @since 1.2
     */
    public abstract void setLine(double x1, double y1, double x2, double y2);

    /**
     * Sets the location of the end points of this {@code Line2D} to
     * the specified {@code Point2D} coordinates.
     * @param p1 the start {@code Point2D} of the line segment
     * @param p2 the end {@code Point2D} of the line segment
     * @since 1.2
     */
    public void setLine(Point2D p1, Point2D p2) {
        setLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * Sets the location of the end points of this {@code Line2D} to
     * the same as those end points of the specified {@code Line2D}.
     * @param l the specified {@code Line2D}
     * @since 1.2
     */
    public void setLine(Line2D l) {
        setLine(l.getX1(), l.getY1(), l.getX2(), l.getY2());
    }

    /**
     * Returns an indicator of where the specified point
     * {@code (px,py)} lies with respect to the line segment from
     * {@code (x1,y1)} to {@code (x2,y2)}.
     * The return value can be either 1, -1, or 0 and indicates
     * in which direction the specified line must pivot around its
     * first end point, {@code (x1,y1)}, in order to point at the
     * specified point {@code (px,py)}.
     * <p>A return value of 1 indicates that the line segment must
     * turn in the direction that takes the positive X axis towards
     * the negative Y axis.  In the default coordinate system used by
     * Java 2D, this direction is counterclockwise.
     * <p>A return value of -1 indicates that the line segment must
     * turn in the direction that takes the positive X axis towards
     * the positive Y axis.  In the default coordinate system, this
     * direction is clockwise.
     * <p>A return value of 0 indicates that the point lies
     * exactly on the line segment.  Note that an indicator value
     * of 0 is rare and not useful for determining collinearity
     * because of floating point rounding issues.
     * <p>If the point is colinear with the line segment, but
     * not between the end points, then the value will be -1 if the point
     * lies "beyond {@code (x1,y1)}" or 1 if the point lies
     * "beyond {@code (x2,y2)}".
     *
     * @param x1 the X coordinate of the start point of the
     *           specified line segment
     * @param y1 the Y coordinate of the start point of the
     *           specified line segment
     * @param x2 the X coordinate of the end point of the
     *           specified line segment
     * @param y2 the Y coordinate of the end point of the
     *           specified line segment
     * @param px the X coordinate of the specified point to be
     *           compared with the specified line segment
     * @param py the Y coordinate of the specified point to be
     *           compared with the specified line segment
     * @return an integer that indicates the position of the third specified
     *                  coordinates with respect to the line segment formed
     *                  by the first two specified coordinates.
     * @since 1.2
     */
    public static int relativeCCW(double x1, double y1,
                                  double x2, double y2,
                                  double px, double py)
    {
        x2 -= x1;
        y2 -= y1;
        px -= x1;
        py -= y1;
        double ccw = px * y2 - py * x2;
        if (ccw == 0.0) {
            // The point is colinear, classify based on which side of
            // the segment the point falls on.  We can calculate a
            // relative value using the projection of px,py onto the
            // segment - a negative value indicates the point projects
            // outside of the segment in the direction of the particular
            // endpoint used as the origin for the projection.
            ccw = px * x2 + py * y2;
            if (ccw > 0.0) {
                // Reverse the projection to be relative to the original x2,y2
                // x2 and y2 are simply negated.
                // px and py need to have (x2 - x1) or (y2 - y1) subtracted
                //    from them (based on the original values)
                // Since we really want to get a positive answer when the
                //    point is "beyond (x2,y2)", then we want to calculate
                //    the inverse anyway - thus we leave x2 & y2 negated.
                px -= x2;
                py -= y2;
                ccw = px * x2 + py * y2;
                if (ccw < 0.0) {
                    ccw = 0.0;
                }
            }
        }
        return (ccw < 0.0) ? -1 : ((ccw > 0.0) ? 1 : 0);
    }

    /**
     * Returns an indicator of where the specified point
     * {@code (px,py)} lies with respect to this line segment.
     * See the method comments of
     * {@link #relativeCCW(double, double, double, double, double, double)}
     * to interpret the return value.
     * @param px the X coordinate of the specified point
     *           to be compared with this {@code Line2D}
     * @param py the Y coordinate of the specified point
     *           to be compared with this {@code Line2D}
     * @return an integer that indicates the position of the specified
     *         coordinates with respect to this {@code Line2D}
     * @see #relativeCCW(double, double, double, double, double, double)
     * @since 1.2
     */
    public int relativeCCW(double px, double py) {
        return relativeCCW(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    /**
     * Returns an indicator of where the specified {@code Point2D}
     * lies with respect to this line segment.
     * See the method comments of
     * {@link #relativeCCW(double, double, double, double, double, double)}
     * to interpret the return value.
     * @param p the specified {@code Point2D} to be compared
     *          with this {@code Line2D}
     * @return an integer that indicates the position of the specified
     *         {@code Point2D} with respect to this {@code Line2D}
     * @see #relativeCCW(double, double, double, double, double, double)
     * @since 1.2
     */
    public int relativeCCW(Point2D p) {
        return relativeCCW(getX1(), getY1(), getX2(), getY2(),
                           p.getX(), p.getY());
    }

    /**
     * Tests if the line segment from {@code (x1,y1)} to
     * {@code (x2,y2)} intersects the line segment from {@code (x3,y3)}
     * to {@code (x4,y4)}.
     *
     * @param x1 the X coordinate of the start point of the first
     *           specified line segment
     * @param y1 the Y coordinate of the start point of the first
     *           specified line segment
     * @param x2 the X coordinate of the end point of the first
     *           specified line segment
     * @param y2 the Y coordinate of the end point of the first
     *           specified line segment
     * @param x3 the X coordinate of the start point of the second
     *           specified line segment
     * @param y3 the Y coordinate of the start point of the second
     *           specified line segment
     * @param x4 the X coordinate of the end point of the second
     *           specified line segment
     * @param y4 the Y coordinate of the end point of the second
     *           specified line segment
     * @return {@code true} if the first specified line segment
     *                  and the second specified line segment intersect
     *                  each other; {@code false} otherwise.
     * @since 1.2
     */
    public static boolean linesIntersect(double x1, double y1,
                                         double x2, double y2,
                                         double x3, double y3,
                                         double x4, double y4)
    {
        return ((relativeCCW(x1, y1, x2, y2, x3, y3) *
                 relativeCCW(x1, y1, x2, y2, x4, y4) <= 0)
                && (relativeCCW(x3, y3, x4, y4, x1, y1) *
                    relativeCCW(x3, y3, x4, y4, x2, y2) <= 0));
    }

    /**
     * Tests if the line segment from {@code (x1,y1)} to
     * {@code (x2,y2)} intersects this line segment.
     *
     * @param x1 the X coordinate of the start point of the
     *           specified line segment
     * @param y1 the Y coordinate of the start point of the
     *           specified line segment
     * @param x2 the X coordinate of the end point of the
     *           specified line segment
     * @param y2 the Y coordinate of the end point of the
     *           specified line segment
     * @return {@code true} if this line segment and the specified line segment
     *                  intersect each other; {@code false} otherwise.
     * @since 1.2
     */
    public boolean intersectsLine(double x1, double y1, double x2, double y2) {
        return linesIntersect(x1, y1, x2, y2,
                              getX1(), getY1(), getX2(), getY2());
    }

    /**
     * Tests if the specified line segment intersects this line segment.
     * @param l the specified {@code Line2D}
     * @return {@code true} if this line segment and the specified line
     *                  segment intersect each other;
     *                  {@code false} otherwise.
     * @since 1.2
     */
    public boolean intersectsLine(Line2D l) {
        return linesIntersect(l.getX1(), l.getY1(), l.getX2(), l.getY2(),
                              getX1(), getY1(), getX2(), getY2());
    }

    /**
     * Returns the square of the distance from a point to a line segment.
     * The distance measured is the distance between the specified
     * point and the closest point between the specified end points.
     * If the specified point intersects the line segment in between the
     * end points, this method returns 0.0.
     *
     * @param x1 the X coordinate of the start point of the
     *           specified line segment
     * @param y1 the Y coordinate of the start point of the
     *           specified line segment
     * @param x2 the X coordinate of the end point of the
     *           specified line segment
     * @param y2 the Y coordinate of the end point of the
     *           specified line segment
     * @param px the X coordinate of the specified point being
     *           measured against the specified line segment
     * @param py the Y coordinate of the specified point being
     *           measured against the specified line segment
     * @return a double value that is the square of the distance from the
     *                  specified point to the specified line segment.
     * @see #ptLineDistSq(double, double, double, double, double, double)
     * @since 1.2
     */
    public static double ptSegDistSq(double x1, double y1,
                                     double x2, double y2,
                                     double px, double py)
    {
        // Adjust vectors relative to x1,y1
        // x2,y2 becomes relative vector from x1,y1 to end of segment
        x2 -= x1;
        y2 -= y1;
        // px,py becomes relative vector from x1,y1 to test point
        px -= x1;
        py -= y1;
        double dotprod = px * x2 + py * y2;
        double projlenSq;
        if (dotprod <= 0.0) {
            // px,py is on the side of x1,y1 away from x2,y2
            // distance to segment is length of px,py vector
            // "length of its (clipped) projection" is now 0.0
            projlenSq = 0.0;
        } else {
            // switch to backwards vectors relative to x2,y2
            // x2,y2 are already the negative of x1,y1=>x2,y2
            // to get px,py to be the negative of px,py=>x2,y2
            // the dot product of two negated vectors is the same
            // as the dot product of the two normal vectors
            px = x2 - px;
            py = y2 - py;
            dotprod = px * x2 + py * y2;
            if (dotprod <= 0.0) {
                // px,py is on the side of x2,y2 away from x1,y1
                // distance to segment is length of (backwards) px,py vector
                // "length of its (clipped) projection" is now 0.0
                projlenSq = 0.0;
            } else {
                // px,py is between x1,y1 and x2,y2
                // dotprod is the length of the px,py vector
                // projected on the x2,y2=>x1,y1 vector times the
                // length of the x2,y2=>x1,y1 vector
                projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2);
            }
        }
        // Distance to line is now the length of the relative point
        // vector minus the length of its projection onto the line
        // (which is zero if the projection falls outside the range
        //  of the line segment).
        double lenSq = px * px + py * py - projlenSq;
        if (lenSq < 0) {
            lenSq = 0;
        }
        return lenSq;
    }

    /**
     * Returns the distance from a point to a line segment.
     * The distance measured is the distance between the specified
     * point and the closest point between the specified end points.
     * If the specified point intersects the line segment in between the
     * end points, this method returns 0.0.
     *
     * @param x1 the X coordinate of the start point of the
     *           specified line segment
     * @param y1 the Y coordinate of the start point of the
     *           specified line segment
     * @param x2 the X coordinate of the end point of the
     *           specified line segment
     * @param y2 the Y coordinate of the end point of the
     *           specified line segment
     * @param px the X coordinate of the specified point being
     *           measured against the specified line segment
     * @param py the Y coordinate of the specified point being
     *           measured against the specified line segment
     * @return a double value that is the distance from the specified point
     *                          to the specified line segment.
     * @see #ptLineDist(double, double, double, double, double, double)
     * @since 1.2
     */
    public static double ptSegDist(double x1, double y1,
                                   double x2, double y2,
                                   double px, double py)
    {
        return Math.sqrt(ptSegDistSq(x1, y1, x2, y2, px, py));
    }

    /**
     * Returns the square of the distance from a point to this line segment.
     * The distance measured is the distance between the specified
     * point and the closest point between the current line's end points.
     * If the specified point intersects the line segment in between the
     * end points, this method returns 0.0.
     *
     * @param px the X coordinate of the specified point being
     *           measured against this line segment
     * @param py the Y coordinate of the specified point being
     *           measured against this line segment
     * @return a double value that is the square of the distance from the
     *                  specified point to the current line segment.
     * @see #ptLineDistSq(double, double)
     * @since 1.2
     */
    public double ptSegDistSq(double px, double py) {
        return ptSegDistSq(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    /**
     * Returns the square of the distance from a {@code Point2D} to
     * this line segment.
     * The distance measured is the distance between the specified
     * point and the closest point between the current line's end points.
     * If the specified point intersects the line segment in between the
     * end points, this method returns 0.0.
     * @param pt the specified {@code Point2D} being measured against
     *           this line segment.
     * @return a double value that is the square of the distance from the
     *                  specified {@code Point2D} to the current
     *                  line segment.
     * @see #ptLineDistSq(Point2D)
     * @since 1.2
     */
    public double ptSegDistSq(Point2D pt) {
        return ptSegDistSq(getX1(), getY1(), getX2(), getY2(),
                           pt.getX(), pt.getY());
    }

    /**
     * Returns the distance from a point to this line segment.
     * The distance measured is the distance between the specified
     * point and the closest point between the current line's end points.
     * If the specified point intersects the line segment in between the
     * end points, this method returns 0.0.
     *
     * @param px the X coordinate of the specified point being
     *           measured against this line segment
     * @param py the Y coordinate of the specified point being
     *           measured against this line segment
     * @return a double value that is the distance from the specified
     *                  point to the current line segment.
     * @see #ptLineDist(double, double)
     * @since 1.2
     */
    public double ptSegDist(double px, double py) {
        return ptSegDist(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    /**
     * Returns the distance from a {@code Point2D} to this line
     * segment.
     * The distance measured is the distance between the specified
     * point and the closest point between the current line's end points.
     * If the specified point intersects the line segment in between the
     * end points, this method returns 0.0.
     * @param pt the specified {@code Point2D} being measured
     *          against this line segment
     * @return a double value that is the distance from the specified
     *                          {@code Point2D} to the current line
     *                          segment.
     * @see #ptLineDist(Point2D)
     * @since 1.2
     */
    public double ptSegDist(Point2D pt) {
        return ptSegDist(getX1(), getY1(), getX2(), getY2(),
                         pt.getX(), pt.getY());
    }

    /**
     * Returns the square of the distance from a point to a line.
     * The distance measured is the distance between the specified
     * point and the closest point on the infinitely-extended line
     * defined by the specified coordinates.  If the specified point
     * intersects the line, this method returns 0.0.
     *
     * @param x1 the X coordinate of the start point of the specified line
     * @param y1 the Y coordinate of the start point of the specified line
     * @param x2 the X coordinate of the end point of the specified line
     * @param y2 the Y coordinate of the end point of the specified line
     * @param px the X coordinate of the specified point being
     *           measured against the specified line
     * @param py the Y coordinate of the specified point being
     *           measured against the specified line
     * @return a double value that is the square of the distance from the
     *                  specified point to the specified line.
     * @see #ptSegDistSq(double, double, double, double, double, double)
     * @since 1.2
     */
    public static double ptLineDistSq(double x1, double y1,
                                      double x2, double y2,
                                      double px, double py)
    {
        // Adjust vectors relative to x1,y1
        // x2,y2 becomes relative vector from x1,y1 to end of segment
        x2 -= x1;
        y2 -= y1;
        // px,py becomes relative vector from x1,y1 to test point
        px -= x1;
        py -= y1;
        double dotprod = px * x2 + py * y2;
        // dotprod is the length of the px,py vector
        // projected on the x1,y1=>x2,y2 vector times the
        // length of the x1,y1=>x2,y2 vector
        double projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2);
        // Distance to line is now the length of the relative point
        // vector minus the length of its projection onto the line
        double lenSq = px * px + py * py - projlenSq;
        if (lenSq < 0) {
            lenSq = 0;
        }
        return lenSq;
    }

    /**
     * Returns the distance from a point to a line.
     * The distance measured is the distance between the specified
     * point and the closest point on the infinitely-extended line
     * defined by the specified coordinates.  If the specified point
     * intersects the line, this method returns 0.0.
     *
     * @param x1 the X coordinate of the start point of the specified line
     * @param y1 the Y coordinate of the start point of the specified line
     * @param x2 the X coordinate of the end point of the specified line
     * @param y2 the Y coordinate of the end point of the specified line
     * @param px the X coordinate of the specified point being
     *           measured against the specified line
     * @param py the Y coordinate of the specified point being
     *           measured against the specified line
     * @return a double value that is the distance from the specified
     *                   point to the specified line.
     * @see #ptSegDist(double, double, double, double, double, double)
     * @since 1.2
     */
    public static double ptLineDist(double x1, double y1,
                                    double x2, double y2,
                                    double px, double py)
    {
        return Math.sqrt(ptLineDistSq(x1, y1, x2, y2, px, py));
    }

    /**
     * Returns the square of the distance from a point to this line.
     * The distance measured is the distance between the specified
     * point and the closest point on the infinitely-extended line
     * defined by this {@code Line2D}.  If the specified point
     * intersects the line, this method returns 0.0.
     *
     * @param px the X coordinate of the specified point being
     *           measured against this line
     * @param py the Y coordinate of the specified point being
     *           measured against this line
     * @return a double value that is the square of the distance from a
     *                  specified point to the current line.
     * @see #ptSegDistSq(double, double)
     * @since 1.2
     */
    public double ptLineDistSq(double px, double py) {
        return ptLineDistSq(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    /**
     * Returns the square of the distance from a specified
     * {@code Point2D} to this line.
     * The distance measured is the distance between the specified
     * point and the closest point on the infinitely-extended line
     * defined by this {@code Line2D}.  If the specified point
     * intersects the line, this method returns 0.0.
     * @param pt the specified {@code Point2D} being measured
     *           against this line
     * @return a double value that is the square of the distance from a
     *                  specified {@code Point2D} to the current
     *                  line.
     * @see #ptSegDistSq(Point2D)
     * @since 1.2
     */
    public double ptLineDistSq(Point2D pt) {
        return ptLineDistSq(getX1(), getY1(), getX2(), getY2(),
                            pt.getX(), pt.getY());
    }

    /**
     * Returns the distance from a point to this line.
     * The distance measured is the distance between the specified
     * point and the closest point on the infinitely-extended line
     * defined by this {@code Line2D}.  If the specified point
     * intersects the line, this method returns 0.0.
     *
     * @param px the X coordinate of the specified point being
     *           measured against this line
     * @param py the Y coordinate of the specified point being
     *           measured against this line
     * @return a double value that is the distance from a specified point
     *                  to the current line.
     * @see #ptSegDist(double, double)
     * @since 1.2
     */
    public double ptLineDist(double px, double py) {
        return ptLineDist(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    /**
     * Returns the distance from a {@code Point2D} to this line.
     * The distance measured is the distance between the specified
     * point and the closest point on the infinitely-extended line
     * defined by this {@code Line2D}.  If the specified point
     * intersects the line, this method returns 0.0.
     * @param pt the specified {@code Point2D} being measured
     * @return a double value that is the distance from a specified
     *                  {@code Point2D} to the current line.
     * @see #ptSegDist(Point2D)
     * @since 1.2
     */
    public double ptLineDist(Point2D pt) {
        return ptLineDist(getX1(), getY1(), getX2(), getY2(),
                         pt.getX(), pt.getY());
    }

    /**
     * Tests if a specified coordinate is inside the boundary of this
     * {@code Line2D}.  This method is required to implement the
     * {@link Shape} interface, but in the case of {@code Line2D}
     * objects it always returns {@code false} since a line contains
     * no area.
     * @param x the X coordinate of the specified point to be tested
     * @param y the Y coordinate of the specified point to be tested
     * @return {@code false} because a {@code Line2D} contains
     * no area.
     * @since 1.2
     */
    public boolean contains(double x, double y) {
        return false;
    }

    /**
     * Tests if a given {@code Point2D} is inside the boundary of
     * this {@code Line2D}.
     * This method is required to implement the {@link Shape} interface,
     * but in the case of {@code Line2D} objects it always returns
     * {@code false} since a line contains no area.
     * @param p the specified {@code Point2D} to be tested
     * @return {@code false} because a {@code Line2D} contains
     * no area.
     * @since 1.2
     */
    public boolean contains(Point2D p) {
        return false;
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public boolean intersects(double x, double y, double w, double h) {
        return intersects(new Rectangle2D.Double(x, y, w, h));
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public boolean intersects(Rectangle2D r) {
        return r.intersectsLine(getX1(), getY1(), getX2(), getY2());
    }

    /**
     * Tests if the interior of this {@code Line2D} entirely contains
     * the specified set of rectangular coordinates.
     * This method is required to implement the {@code Shape} interface,
     * but in the case of {@code Line2D} objects it always returns
     * false since a line contains no area.
     * @param x the X coordinate of the upper-left corner of the
     *          specified rectangular area
     * @param y the Y coordinate of the upper-left corner of the
     *          specified rectangular area
     * @param w the width of the specified rectangular area
     * @param h the height of the specified rectangular area
     * @return {@code false} because a {@code Line2D} contains
     * no area.
     * @since 1.2
     */
    public boolean contains(double x, double y, double w, double h) {
        return false;
    }

    /**
     * Tests if the interior of this {@code Line2D} entirely contains
     * the specified {@code Rectangle2D}.
     * This method is required to implement the {@code Shape} interface,
     * but in the case of {@code Line2D} objects it always returns
     * {@code false} since a line contains no area.
     * @param r the specified {@code Rectangle2D} to be tested
     * @return {@code false} because a {@code Line2D} contains
     * no area.
     * @since 1.2
     */
    public boolean contains(Rectangle2D r) {
        return false;
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    /**
     * Returns an iteration object that defines the boundary of this
     * {@code Line2D}.
     * The iterator for this class is not multi-threaded safe,
     * which means that this {@code Line2D} class does not
     * guarantee that modifications to the geometry of this
     * {@code Line2D} object do not affect any iterations of that
     * geometry that are already in process.
     * @param at the specified {@link AffineTransform}
     * @return a {@link PathIterator} that defines the boundary of this
     *          {@code Line2D}.
     * @since 1.2
     */
    public PathIterator getPathIterator(AffineTransform at) {
        return new LineIterator(this, at);
    }

    /**
     * Returns an iteration object that defines the boundary of this
     * flattened {@code Line2D}.
     * The iterator for this class is not multi-threaded safe,
     * which means that this {@code Line2D} class does not
     * guarantee that modifications to the geometry of this
     * {@code Line2D} object do not affect any iterations of that
     * geometry that are already in process.
     * @param at the specified {@code AffineTransform}
     * @param flatness the maximum amount that the control points for a
     *          given curve can vary from colinear before a subdivided
     *          curve is replaced by a straight line connecting the
     *          end points.  Since a {@code Line2D} object is
     *          always flat, this parameter is ignored.
     * @return a {@code PathIterator} that defines the boundary of the
     *                  flattened {@code Line2D}
     * @since 1.2
     */
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new LineIterator(this, at);
    }

    /**
     * Creates a new object of the same class as this object.
     *
     * @return     a clone of this instance.
     * @exception  OutOfMemoryError            if there is not enough memory.
     * @see        java.lang.Cloneable
     * @since      1.2
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
    }
}
