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
 * The {@code Ellipse2D} class describes an ellipse that is defined
 * by a framing rectangle.
 * <p>
 * This class is only the abstract superclass for all objects which
 * store a 2D ellipse.
 * The actual storage representation of the coordinates is left to
 * the subclass.
 *
 * @author      Jim Graham
 * @since 1.2
 */
public abstract class Ellipse2D extends RectangularShape {

    /**
     * The {@code Float} class defines an ellipse specified
     * in {@code float} precision.
     * @since 1.2
     */
    public static class Float extends Ellipse2D implements Serializable {
        /**
         * The X coordinate of the upper-left corner of the
         * framing rectangle of this {@code Ellipse2D}.
         * @since 1.2
         * @serial
         */
        public float x;

        /**
         * The Y coordinate of the upper-left corner of the
         * framing rectangle of this {@code Ellipse2D}.
         * @since 1.2
         * @serial
         */
        public float y;

        /**
         * The overall width of this {@code Ellipse2D}.
         * @since 1.2
         * @serial
         */
        public float width;

        /**
         * The overall height of this {@code Ellipse2D}.
         * @since 1.2
         * @serial
         */
        public float height;

        /**
         * Constructs a new {@code Ellipse2D}, initialized to
         * location (0,&nbsp;0) and size (0,&nbsp;0).
         * @since 1.2
         */
        public Float() {
        }

        /**
         * Constructs and initializes an {@code Ellipse2D} from the
         * specified coordinates.
         *
         * @param x the X coordinate of the upper-left corner
         *          of the framing rectangle
         * @param y the Y coordinate of the upper-left corner
         *          of the framing rectangle
         * @param w the width of the framing rectangle
         * @param h the height of the framing rectangle
         * @since 1.2
         */
        public Float(float x, float y, float w, float h) {
            setFrame(x, y, w, h);
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
            return (width <= 0.0 || height <= 0.0);
        }

        /**
         * Sets the location and size of the framing rectangle of this
         * {@code Shape} to the specified rectangular values.
         *
         * @param x the X coordinate of the upper-left corner of the
         *              specified rectangular shape
         * @param y the Y coordinate of the upper-left corner of the
         *              specified rectangular shape
         * @param w the width of the specified rectangular shape
         * @param h the height of the specified rectangular shape
         * @since 1.2
         */
        public void setFrame(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public void setFrame(double x, double y, double w, double h) {
            this.x = (float) x;
            this.y = (float) y;
            this.width = (float) w;
            this.height = (float) h;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public Rectangle2D getBounds2D() {
            return new Rectangle2D.Float(x, y, width, height);
        }

        /**
         * Use serialVersionUID from JDK 1.6 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = -6633761252372475977L;
    }

    /**
     * The {@code Double} class defines an ellipse specified
     * in {@code double} precision.
     * @since 1.2
     */
    public static class Double extends Ellipse2D implements Serializable {
        /**
         * The X coordinate of the upper-left corner of the
         * framing rectangle of this {@code Ellipse2D}.
         * @since 1.2
         * @serial
         */
        public double x;

        /**
         * The Y coordinate of the upper-left corner of the
         * framing rectangle of this {@code Ellipse2D}.
         * @since 1.2
         * @serial
         */
        public double y;

        /**
         * The overall width of this {@code Ellipse2D}.
         * @since 1.2
         * @serial
         */
        public double width;

        /**
         * The overall height of the {@code Ellipse2D}.
         * @since 1.2
         * @serial
         */
        public double height;

        /**
         * Constructs a new {@code Ellipse2D}, initialized to
         * location (0,&nbsp;0) and size (0,&nbsp;0).
         * @since 1.2
         */
        public Double() {
        }

        /**
         * Constructs and initializes an {@code Ellipse2D} from the
         * specified coordinates.
         *
         * @param x the X coordinate of the upper-left corner
         *        of the framing rectangle
         * @param y the Y coordinate of the upper-left corner
         *        of the framing rectangle
         * @param w the width of the framing rectangle
         * @param h the height of the framing rectangle
         * @since 1.2
         */
        public Double(double x, double y, double w, double h) {
            setFrame(x, y, w, h);
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
            return (width <= 0.0 || height <= 0.0);
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public void setFrame(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        /**
         * {@inheritDoc}
         * @since 1.2
         */
        public Rectangle2D getBounds2D() {
            return new Rectangle2D.Double(x, y, width, height);
        }

        /**
         * Use serialVersionUID from JDK 1.6 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 5555464816372320683L;
    }

    /**
     * This is an abstract class that cannot be instantiated directly.
     * Type-specific implementation subclasses are available for
     * instantiation and provide a number of formats for storing
     * the information necessary to satisfy the various accessor
     * methods below.
     *
     * @see java.awt.geom.Ellipse2D.Float
     * @see java.awt.geom.Ellipse2D.Double
     * @since 1.2
     */
    protected Ellipse2D() {
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public boolean contains(double x, double y) {
        // Normalize the coordinates compared to the ellipse
        // having a center at 0,0 and a radius of 0.5.
        double ellw = getWidth();
        if (ellw <= 0.0) {
            return false;
        }
        double normx = (x - getX()) / ellw - 0.5;
        double ellh = getHeight();
        if (ellh <= 0.0) {
            return false;
        }
        double normy = (y - getY()) / ellh - 0.5;
        return (normx * normx + normy * normy) < 0.25;
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public boolean intersects(double x, double y, double w, double h) {
        if (w <= 0.0 || h <= 0.0) {
            return false;
        }
        // Normalize the rectangular coordinates compared to the ellipse
        // having a center at 0,0 and a radius of 0.5.
        double ellw = getWidth();
        if (ellw <= 0.0) {
            return false;
        }
        double normx0 = (x - getX()) / ellw - 0.5;
        double normx1 = normx0 + w / ellw;
        double ellh = getHeight();
        if (ellh <= 0.0) {
            return false;
        }
        double normy0 = (y - getY()) / ellh - 0.5;
        double normy1 = normy0 + h / ellh;
        // find nearest x (left edge, right edge, 0.0)
        // find nearest y (top edge, bottom edge, 0.0)
        // if nearest x,y is inside circle of radius 0.5, then intersects
        double nearx, neary;
        if (normx0 > 0.0) {
            // center to left of X extents
            nearx = normx0;
        } else if (normx1 < 0.0) {
            // center to right of X extents
            nearx = normx1;
        } else {
            nearx = 0.0;
        }
        if (normy0 > 0.0) {
            // center above Y extents
            neary = normy0;
        } else if (normy1 < 0.0) {
            // center below Y extents
            neary = normy1;
        } else {
            neary = 0.0;
        }
        return (nearx * nearx + neary * neary) < 0.25;
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public boolean contains(double x, double y, double w, double h) {
        return (contains(x, y) &&
                contains(x + w, y) &&
                contains(x, y + h) &&
                contains(x + w, y + h));
    }

    /**
     * Returns an iteration object that defines the boundary of this
     * {@code Ellipse2D}.
     * The iterator for this class is multi-threaded safe, which means
     * that this {@code Ellipse2D} class guarantees that
     * modifications to the geometry of this {@code Ellipse2D}
     * object do not affect any iterations of that geometry that
     * are already in process.
     * @param at an optional {@code AffineTransform} to be applied to
     * the coordinates as they are returned in the iteration, or
     * {@code null} if untransformed coordinates are desired
     * @return    the {@code PathIterator} object that returns the
     *          geometry of the outline of this {@code Ellipse2D},
     *          one segment at a time.
     * @since 1.2
     */
    public PathIterator getPathIterator(AffineTransform at) {
        return new EllipseIterator(this, at);
    }

    /**
     * Returns the hashcode for this {@code Ellipse2D}.
     * @return the hashcode for this {@code Ellipse2D}.
     * @since 1.6
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
     * equal to this {@code Ellipse2D}.  The specified
     * {@code Object} is equal to this {@code Ellipse2D}
     * if it is an instance of {@code Ellipse2D} and if its
     * location and size are the same as this {@code Ellipse2D}.
     * @param obj  an {@code Object} to be compared with this
     *             {@code Ellipse2D}.
     * @return  {@code true} if {@code obj} is an instance
     *          of {@code Ellipse2D} and has the same values;
     *          {@code false} otherwise.
     * @since 1.6
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Ellipse2D) {
            Ellipse2D e2d = (Ellipse2D) obj;
            return ((getX() == e2d.getX()) &&
                    (getY() == e2d.getY()) &&
                    (getWidth() == e2d.getWidth()) &&
                    (getHeight() == e2d.getHeight()));
        }
        return false;
    }
}
