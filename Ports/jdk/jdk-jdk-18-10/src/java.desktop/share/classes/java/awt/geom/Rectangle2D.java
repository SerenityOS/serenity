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

import java.io.Serial;
import java.io.Serializable;

/**
 * The {@code Rectangle2D} class describes a rectangle
 * defined by a location {@code (x,y)} and dimension
 * {@code (w x h)}.
 * <p>
 * This class is only the abstract superclass for all objects that
 * store a 2D rectangle.
 * The actual storage representation of the coordinates is left to
 * the subclass.
 *
 * @author      Jim Graham
 * @since 1.2
 */
public abstract class Rectangle2D extends RectangularShape {
    /**
     * The bitmask that indicates that a point lies to the left of
     * this {@code Rectangle2D}.
     * @since 1.2
     */
    public static final int OUT_LEFT = 1;

    /**
     * The bitmask that indicates that a point lies above
     * this {@code Rectangle2D}.
     * @since 1.2
     */
    public static final int OUT_TOP = 2;

    /**
     * The bitmask that indicates that a point lies to the right of
     * this {@code Rectangle2D}.
     * @since 1.2
     */
    public static final int OUT_RIGHT = 4;

    /**
     * The bitmask that indicates that a point lies below
     * this {@code Rectangle2D}.
     * @since 1.2
     */
    public static final int OUT_BOTTOM = 8;

    /**
     * The {@code Float} class defines a rectangle specified in float
     * coordinates.
     * @since 1.2
     */
    public static class Float extends Rectangle2D implements Serializable {
        /**
         * The X coordinate of this {@code Rectangle2D}.
         * @since 1.2
         * @serial
         */
        public float x;

        /**
         * The Y coordinate of this {@code Rectangle2D}.
         * @since 1.2
         * @serial
         */
        public float y;

        /**
         * The width of this {@code Rectangle2D}.
         * @since 1.2
         * @serial
         */
        public float width;

        /**
         * The height of this {@code Rectangle2D}.
         * @since 1.2
         * @serial
         */
        public float height;

        /**
         * Constructs a new {@code Rectangle2D}, initialized to
         * location (0.0,&nbsp;0.0) and size (0.0,&nbsp;0.0).
         * @since 1.2
         */
        public Float() {
        }

        /**
         * Constructs and initializes a {@code Rectangle2D}
         * from the specified {@code float} coordinates.
         *
         * @param x the X coordinate of the upper-left corner
         *          of the newly constructed {@code Rectangle2D}
         * @param y the Y coordinate of the upper-left corner
         *          of the newly constructed {@code Rectangle2D}
         * @param w the width of the newly constructed
         *          {@code Rectangle2D}
         * @param h the height of the newly constructed
         *          {@code Rectangle2D}
         * @since 1.2
        */
        public Float(float x, float y, float w, float h) {
            setRect(x, y, w, h);
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public double getX() {
            return (double) x;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public double getY() {
            return (double) y;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public double getWidth() {
            return (double) width;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public double getHeight() {
            return (double) height;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public boolean isEmpty() {
            return (width <= 0.0f) || (height <= 0.0f);
        }

        /**
         * Sets the location and size of this {@code Rectangle2D}
         * to the specified {@code float} values.
         *
         * @param x the X coordinate of the upper-left corner
         *          of this {@code Rectangle2D}
         * @param y the Y coordinate of the upper-left corner
         *          of this {@code Rectangle2D}
         * @param w the width of this {@code Rectangle2D}
         * @param h the height of this {@code Rectangle2D}
         * @since 1.2
         */
        public void setRect(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public void setRect(double x, double y, double w, double h) {
            this.x = (float) x;
            this.y = (float) y;
            this.width = (float) w;
            this.height = (float) h;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public void setRect(Rectangle2D r) {
            this.x = (float) r.getX();
            this.y = (float) r.getY();
            this.width = (float) r.getWidth();
            this.height = (float) r.getHeight();
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public int outcode(double x, double y) {
            /*
             * Note on casts to double below.  If the arithmetic of
             * x+w or y+h is done in float, then some bits may be
             * lost if the binary exponents of x/y and w/h are not
             * similar.  By converting to double before the addition
             * we force the addition to be carried out in double to
             * avoid rounding error in the comparison.
             *
             * See bug 4320890 for problems that this inaccuracy causes.
             */
            int out = 0;
            if (this.width <= 0) {
                out |= OUT_LEFT | OUT_RIGHT;
            } else if (x < this.x) {
                out |= OUT_LEFT;
            } else if (x > this.x + (double) this.width) {
                out |= OUT_RIGHT;
            }
            if (this.height <= 0) {
                out |= OUT_TOP | OUT_BOTTOM;
            } else if (y < this.y) {
                out |= OUT_TOP;
            } else if (y > this.y + (double) this.height) {
                out |= OUT_BOTTOM;
            }
            return out;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public Rectangle2D getBounds2D() {
            return new Float(x, y, width, height);
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public Rectangle2D createIntersection(Rectangle2D r) {
            Rectangle2D dest;
            if (r instanceof Float) {
                dest = new Rectangle2D.Float();
            } else {
                dest = new Rectangle2D.Double();
            }
            Rectangle2D.intersect(this, r, dest);
            return dest;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public Rectangle2D createUnion(Rectangle2D r) {
            Rectangle2D dest;
            if (r instanceof Float) {
                dest = new Rectangle2D.Float();
            } else {
                dest = new Rectangle2D.Double();
            }
            Rectangle2D.union(this, r, dest);
            return dest;
        }

        /**
         * Returns the {@code String} representation of this
         * {@code Rectangle2D}.
         * @return a {@code String} representing this
         * {@code Rectangle2D}.
         * @since 1.2
         */
        public String toString() {
            return getClass().getName()
                + "[x=" + x +
                ",y=" + y +
                ",w=" + width +
                ",h=" + height + "]";
        }

        /**
         * Use serialVersionUID from JDK 1.6 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 3798716824173675777L;
    }

    /**
     * The {@code Double} class defines a rectangle specified in
     * double coordinates.
     * @since 1.2
     */
    public static class Double extends Rectangle2D implements Serializable {
        /**
         * The X coordinate of this {@code Rectangle2D}.
         * @since 1.2
         * @serial
         */
        public double x;

        /**
         * The Y coordinate of this {@code Rectangle2D}.
         * @since 1.2
         * @serial
         */
        public double y;

        /**
         * The width of this {@code Rectangle2D}.
         * @since 1.2
         * @serial
         */
        public double width;

        /**
         * The height of this {@code Rectangle2D}.
         * @since 1.2
         * @serial
         */
        public double height;

        /**
         * Constructs a new {@code Rectangle2D}, initialized to
         * location (0,&nbsp;0) and size (0,&nbsp;0).
         * @since 1.2
         */
        public Double() {
        }

        /**
         * Constructs and initializes a {@code Rectangle2D}
         * from the specified {@code double} coordinates.
         *
         * @param x the X coordinate of the upper-left corner
         *          of the newly constructed {@code Rectangle2D}
         * @param y the Y coordinate of the upper-left corner
         *          of the newly constructed {@code Rectangle2D}
         * @param w the width of the newly constructed
         *          {@code Rectangle2D}
         * @param h the height of the newly constructed
         *          {@code Rectangle2D}
         * @since 1.2
         */
        public Double(double x, double y, double w, double h) {
            setRect(x, y, w, h);
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public double getX() {
            return x;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public double getY() {
            return y;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public double getWidth() {
            return width;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public double getHeight() {
            return height;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public boolean isEmpty() {
            return (width <= 0.0) || (height <= 0.0);
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public void setRect(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public void setRect(Rectangle2D r) {
            this.x = r.getX();
            this.y = r.getY();
            this.width = r.getWidth();
            this.height = r.getHeight();
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public int outcode(double x, double y) {
            int out = 0;
            if (this.width <= 0) {
                out |= OUT_LEFT | OUT_RIGHT;
            } else if (x < this.x) {
                out |= OUT_LEFT;
            } else if (x > this.x + this.width) {
                out |= OUT_RIGHT;
            }
            if (this.height <= 0) {
                out |= OUT_TOP | OUT_BOTTOM;
            } else if (y < this.y) {
                out |= OUT_TOP;
            } else if (y > this.y + this.height) {
                out |= OUT_BOTTOM;
            }
            return out;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public Rectangle2D getBounds2D() {
            return new Double(x, y, width, height);
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public Rectangle2D createIntersection(Rectangle2D r) {
            Rectangle2D dest = new Rectangle2D.Double();
            Rectangle2D.intersect(this, r, dest);
            return dest;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public Rectangle2D createUnion(Rectangle2D r) {
            Rectangle2D dest = new Rectangle2D.Double();
            Rectangle2D.union(this, r, dest);
            return dest;
        }

        /**
         * Returns the {@code String} representation of this
         * {@code Rectangle2D}.
         * @return a {@code String} representing this
         * {@code Rectangle2D}.
         * @since 1.2
         */
        public String toString() {
            return getClass().getName()
                + "[x=" + x +
                ",y=" + y +
                ",w=" + width +
                ",h=" + height + "]";
        }

        /**
         * Use serialVersionUID from JDK 1.6 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 7771313791441850493L;
    }

    /**
     * This is an abstract class that cannot be instantiated directly.
     * Type-specific implementation subclasses are available for
     * instantiation and provide a number of formats for storing
     * the information necessary to satisfy the various accessor
     * methods below.
     *
     * @see java.awt.geom.Rectangle2D.Float
     * @see java.awt.geom.Rectangle2D.Double
     * @see java.awt.Rectangle
     * @since 1.2
     */
    protected Rectangle2D() {
    }

    /**
     * Sets the location and size of this {@code Rectangle2D}
     * to the specified {@code double} values.
     *
     * @param x the X coordinate of the upper-left corner
     *          of this {@code Rectangle2D}
     * @param y the Y coordinate of the upper-left corner
     *          of this {@code Rectangle2D}
     * @param w the width of this {@code Rectangle2D}
     * @param h the height of this {@code Rectangle2D}
     * @since 1.2
     */
    public abstract void setRect(double x, double y, double w, double h);

    /**
     * Sets this {@code Rectangle2D} to be the same as the specified
     * {@code Rectangle2D}.
     * @param r the specified {@code Rectangle2D}
     * @since 1.2
     */
    public void setRect(Rectangle2D r) {
        setRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /**
     * Tests if the specified line segment intersects the interior of this
     * {@code Rectangle2D}.
     *
     * @param x1 the X coordinate of the start point of the specified
     *           line segment
     * @param y1 the Y coordinate of the start point of the specified
     *           line segment
     * @param x2 the X coordinate of the end point of the specified
     *           line segment
     * @param y2 the Y coordinate of the end point of the specified
     *           line segment
     * @return {@code true} if the specified line segment intersects
     * the interior of this {@code Rectangle2D}; {@code false}
     * otherwise.
     * @since 1.2
     */
    public boolean intersectsLine(double x1, double y1, double x2, double y2) {
        int out1, out2;
        if ((out2 = outcode(x2, y2)) == 0) {
            return true;
        }
        while ((out1 = outcode(x1, y1)) != 0) {
            if ((out1 & out2) != 0) {
                return false;
            }
            if ((out1 & (OUT_LEFT | OUT_RIGHT)) != 0) {
                double x = getX();
                if ((out1 & OUT_RIGHT) != 0) {
                    x += getWidth();
                }
                y1 = y1 + (x - x1) * (y2 - y1) / (x2 - x1);
                x1 = x;
            } else {
                double y = getY();
                if ((out1 & OUT_BOTTOM) != 0) {
                    y += getHeight();
                }
                x1 = x1 + (y - y1) * (x2 - x1) / (y2 - y1);
                y1 = y;
            }
        }
        return true;
    }

    /**
     * Tests if the specified line segment intersects the interior of this
     * {@code Rectangle2D}.
     * @param l the specified {@link Line2D} to test for intersection
     * with the interior of this {@code Rectangle2D}
     * @return {@code true} if the specified {@code Line2D}
     * intersects the interior of this {@code Rectangle2D};
     * {@code false} otherwise.
     * @since 1.2
     */
    public boolean intersectsLine(Line2D l) {
        return intersectsLine(l.getX1(), l.getY1(), l.getX2(), l.getY2());
    }

    /**
     * Determines where the specified coordinates lie with respect
     * to this {@code Rectangle2D}.
     * This method computes a binary OR of the appropriate mask values
     * indicating, for each side of this {@code Rectangle2D},
     * whether or not the specified coordinates are on the same side
     * of the edge as the rest of this {@code Rectangle2D}.
     * @param x the specified X coordinate
     * @param y the specified Y coordinate
     * @return the logical OR of all appropriate out codes.
     * @see #OUT_LEFT
     * @see #OUT_TOP
     * @see #OUT_RIGHT
     * @see #OUT_BOTTOM
     * @since 1.2
     */
    public abstract int outcode(double x, double y);

    /**
     * Determines where the specified {@link Point2D} lies with
     * respect to this {@code Rectangle2D}.
     * This method computes a binary OR of the appropriate mask values
     * indicating, for each side of this {@code Rectangle2D},
     * whether or not the specified {@code Point2D} is on the same
     * side of the edge as the rest of this {@code Rectangle2D}.
     * @param p the specified {@code Point2D}
     * @return the logical OR of all appropriate out codes.
     * @see #OUT_LEFT
     * @see #OUT_TOP
     * @see #OUT_RIGHT
     * @see #OUT_BOTTOM
     * @since 1.2
     */
    public int outcode(Point2D p) {
        return outcode(p.getX(), p.getY());
    }

    /**
     * Sets the location and size of the outer bounds of this
     * {@code Rectangle2D} to the specified rectangular values.
     *
     * @param x the X coordinate of the upper-left corner
     *          of this {@code Rectangle2D}
     * @param y the Y coordinate of the upper-left corner
     *          of this {@code Rectangle2D}
     * @param w the width of this {@code Rectangle2D}
     * @param h the height of this {@code Rectangle2D}
     * @since 1.2
     */
    public void setFrame(double x, double y, double w, double h) {
        setRect(x, y, w, h);
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public Rectangle2D getBounds2D() {
        return (Rectangle2D) clone();
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public boolean contains(double x, double y) {
        double x0 = getX();
        double y0 = getY();
        return (x >= x0 &&
                y >= y0 &&
                x < x0 + getWidth() &&
                y < y0 + getHeight());
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public boolean intersects(double x, double y, double w, double h) {
        if (isEmpty() || w <= 0 || h <= 0) {
            return false;
        }
        double x0 = getX();
        double y0 = getY();
        return (x + w > x0 &&
                y + h > y0 &&
                x < x0 + getWidth() &&
                y < y0 + getHeight());
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public boolean contains(double x, double y, double w, double h) {
        if (isEmpty() || w <= 0 || h <= 0) {
            return false;
        }
        double x0 = getX();
        double y0 = getY();
        return (x >= x0 &&
                y >= y0 &&
                (x + w) <= x0 + getWidth() &&
                (y + h) <= y0 + getHeight());
    }

    /**
     * Returns a new {@code Rectangle2D} object representing the
     * intersection of this {@code Rectangle2D} with the specified
     * {@code Rectangle2D}.
     * @param r the {@code Rectangle2D} to be intersected with
     * this {@code Rectangle2D}
     * @return the largest {@code Rectangle2D} contained in both
     *          the specified {@code Rectangle2D} and in this
     *          {@code Rectangle2D}.
     * @since 1.2
     */
    public abstract Rectangle2D createIntersection(Rectangle2D r);

    /**
     * Intersects the pair of specified source {@code Rectangle2D}
     * objects and puts the result into the specified destination
     * {@code Rectangle2D} object.  One of the source rectangles
     * can also be the destination to avoid creating a third Rectangle2D
     * object, but in this case the original points of this source
     * rectangle will be overwritten by this method.
     * @param src1 the first of a pair of {@code Rectangle2D}
     * objects to be intersected with each other
     * @param src2 the second of a pair of {@code Rectangle2D}
     * objects to be intersected with each other
     * @param dest the {@code Rectangle2D} that holds the
     * results of the intersection of {@code src1} and
     * {@code src2}
     * @since 1.2
     */
    public static void intersect(Rectangle2D src1,
                                 Rectangle2D src2,
                                 Rectangle2D dest) {
        double x1 = Math.max(src1.getMinX(), src2.getMinX());
        double y1 = Math.max(src1.getMinY(), src2.getMinY());
        double x2 = Math.min(src1.getMaxX(), src2.getMaxX());
        double y2 = Math.min(src1.getMaxY(), src2.getMaxY());
        dest.setFrame(x1, y1, x2-x1, y2-y1);
    }

    /**
     * Returns a new {@code Rectangle2D} object representing the
     * union of this {@code Rectangle2D} with the specified
     * {@code Rectangle2D}.
     * @param r the {@code Rectangle2D} to be combined with
     * this {@code Rectangle2D}
     * @return the smallest {@code Rectangle2D} containing both
     * the specified {@code Rectangle2D} and this
     * {@code Rectangle2D}.
     * @since 1.2
     */
    public abstract Rectangle2D createUnion(Rectangle2D r);

    /**
     * Unions the pair of source {@code Rectangle2D} objects
     * and puts the result into the specified destination
     * {@code Rectangle2D} object.  One of the source rectangles
     * can also be the destination to avoid creating a third Rectangle2D
     * object, but in this case the original points of this source
     * rectangle will be overwritten by this method.
     * @param src1 the first of a pair of {@code Rectangle2D}
     * objects to be combined with each other
     * @param src2 the second of a pair of {@code Rectangle2D}
     * objects to be combined with each other
     * @param dest the {@code Rectangle2D} that holds the
     * results of the union of {@code src1} and
     * {@code src2}
     * @since 1.2
     */
    public static void union(Rectangle2D src1,
                             Rectangle2D src2,
                             Rectangle2D dest) {
        double x1 = Math.min(src1.getMinX(), src2.getMinX());
        double y1 = Math.min(src1.getMinY(), src2.getMinY());
        double x2 = Math.max(src1.getMaxX(), src2.getMaxX());
        double y2 = Math.max(src1.getMaxY(), src2.getMaxY());
        dest.setFrameFromDiagonal(x1, y1, x2, y2);
    }

    /**
     * Adds a point, specified by the double precision arguments
     * {@code newx} and {@code newy}, to this
     * {@code Rectangle2D}.  The resulting {@code Rectangle2D}
     * is the smallest {@code Rectangle2D} that
     * contains both the original {@code Rectangle2D} and the
     * specified point.
     * <p>
     * After adding a point, a call to {@code contains} with the
     * added point as an argument does not necessarily return
     * {@code true}. The {@code contains} method does not
     * return {@code true} for points on the right or bottom
     * edges of a rectangle. Therefore, if the added point falls on
     * the right or bottom edge of the enlarged rectangle,
     * {@code contains} returns {@code false} for that point.
     * @param newx the X coordinate of the new point
     * @param newy the Y coordinate of the new point
     * @since 1.2
     */
    public void add(double newx, double newy) {
        double x1 = Math.min(getMinX(), newx);
        double x2 = Math.max(getMaxX(), newx);
        double y1 = Math.min(getMinY(), newy);
        double y2 = Math.max(getMaxY(), newy);
        setRect(x1, y1, x2 - x1, y2 - y1);
    }

    /**
     * Adds the {@code Point2D} object {@code pt} to this
     * {@code Rectangle2D}.
     * The resulting {@code Rectangle2D} is the smallest
     * {@code Rectangle2D} that contains both the original
     * {@code Rectangle2D} and the specified {@code Point2D}.
     * <p>
     * After adding a point, a call to {@code contains} with the
     * added point as an argument does not necessarily return
     * {@code true}. The {@code contains}
     * method does not return {@code true} for points on the right
     * or bottom edges of a rectangle. Therefore, if the added point falls
     * on the right or bottom edge of the enlarged rectangle,
     * {@code contains} returns {@code false} for that point.
     * @param     pt the new {@code Point2D} to add to this
     * {@code Rectangle2D}.
     * @since 1.2
     */
    public void add(Point2D pt) {
        add(pt.getX(), pt.getY());
    }

    /**
     * Adds a {@code Rectangle2D} object to this
     * {@code Rectangle2D}.  The resulting {@code Rectangle2D}
     * is the union of the two {@code Rectangle2D} objects.
     * @param r the {@code Rectangle2D} to add to this
     * {@code Rectangle2D}.
     * @since 1.2
     */
    public void add(Rectangle2D r) {
        double x1 = Math.min(getMinX(), r.getMinX());
        double x2 = Math.max(getMaxX(), r.getMaxX());
        double y1 = Math.min(getMinY(), r.getMinY());
        double y2 = Math.max(getMaxY(), r.getMaxY());
        setRect(x1, y1, x2 - x1, y2 - y1);
    }

    /**
     * Returns an iteration object that defines the boundary of this
     * {@code Rectangle2D}.
     * The iterator for this class is multi-threaded safe, which means
     * that this {@code Rectangle2D} class guarantees that
     * modifications to the geometry of this {@code Rectangle2D}
     * object do not affect any iterations of that geometry that
     * are already in process.
     * @param at an optional {@code AffineTransform} to be applied to
     * the coordinates as they are returned in the iteration, or
     * {@code null} if untransformed coordinates are desired
     * @return    the {@code PathIterator} object that returns the
     *          geometry of the outline of this
     *          {@code Rectangle2D}, one segment at a time.
     * @since 1.2
     */
    public PathIterator getPathIterator(AffineTransform at) {
        return new RectIterator(this, at);
    }

    /**
     * Returns an iteration object that defines the boundary of the
     * flattened {@code Rectangle2D}.  Since rectangles are already
     * flat, the {@code flatness} parameter is ignored.
     * The iterator for this class is multi-threaded safe, which means
     * that this {@code Rectangle2D} class guarantees that
     * modifications to the geometry of this {@code Rectangle2D}
     * object do not affect any iterations of that geometry that
     * are already in process.
     * @param at an optional {@code AffineTransform} to be applied to
     * the coordinates as they are returned in the iteration, or
     * {@code null} if untransformed coordinates are desired
     * @param flatness the maximum distance that the line segments used to
     * approximate the curved segments are allowed to deviate from any
     * point on the original curve.  Since rectangles are already flat,
     * the {@code flatness} parameter is ignored.
     * @return    the {@code PathIterator} object that returns the
     *          geometry of the outline of this
     *          {@code Rectangle2D}, one segment at a time.
     * @since 1.2
     */
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new RectIterator(this, at);
    }

    /**
     * Returns the hashcode for this {@code Rectangle2D}.
     * @return the hashcode for this {@code Rectangle2D}.
     * @since 1.2
     */
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits += java.lang.Double.doubleToLongBits(getY()) * 37;
        bits += java.lang.Double.doubleToLongBits(getWidth()) * 43;
        bits += java.lang.Double.doubleToLongBits(getHeight()) * 47;
        return (((int) bits) ^ ((int) (bits >> 32)));
    }

    /**
     * Determines whether or not the specified {@code Object} is
     * equal to this {@code Rectangle2D}.  The specified
     * {@code Object} is equal to this {@code Rectangle2D}
     * if it is an instance of {@code Rectangle2D} and if its
     * location and size are the same as this {@code Rectangle2D}.
     * @param obj an {@code Object} to be compared with this
     * {@code Rectangle2D}.
     * @return     {@code true} if {@code obj} is an instance
     *                     of {@code Rectangle2D} and has
     *                     the same values; {@code false} otherwise.
     * @since 1.2
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Rectangle2D) {
            Rectangle2D r2d = (Rectangle2D) obj;
            return ((getX() == r2d.getX()) &&
                    (getY() == r2d.getY()) &&
                    (getWidth() == r2d.getWidth()) &&
                    (getHeight() == r2d.getHeight()));
        }
        return false;
    }
}
