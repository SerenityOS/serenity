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
 * The {@code Point2D} class defines a point representing a location
 * in {@code (x,y)} coordinate space.
 * <p>
 * This class is only the abstract superclass for all objects that
 * store a 2D coordinate.
 * The actual storage representation of the coordinates is left to
 * the subclass.
 *
 * @author      Jim Graham
 * @since 1.2
 */
public abstract class Point2D implements Cloneable {

    /**
     * The {@code Float} class defines a point specified in float
     * precision.
     * @since 1.2
     */
    public static class Float extends Point2D implements Serializable {
        /**
         * The X coordinate of this {@code Point2D}.
         * @since 1.2
         * @serial
         */
        public float x;

        /**
         * The Y coordinate of this {@code Point2D}.
         * @since 1.2
         * @serial
         */
        public float y;

        /**
         * Constructs and initializes a {@code Point2D} with
         * coordinates (0,&nbsp;0).
         * @since 1.2
         */
        public Float() {
        }

        /**
         * Constructs and initializes a {@code Point2D} with
         * the specified coordinates.
         *
         * @param x the X coordinate of the newly
         *          constructed {@code Point2D}
         * @param y the Y coordinate of the newly
         *          constructed {@code Point2D}
         * @since 1.2
         */
        public Float(float x, float y) {
            this.x = x;
            this.y = y;
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
        public void setLocation(double x, double y) {
            this.x = (float) x;
            this.y = (float) y;
        }

        /**
         * Sets the location of this {@code Point2D} to the
         * specified {@code float} coordinates.
         *
         * @param x the new X coordinate of this {@code Point2D}
         * @param y the new Y coordinate of this {@code Point2D}
         * @since 1.2
         */
        public void setLocation(float x, float y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Returns a {@code String} that represents the value
         * of this {@code Point2D}.
         * @return a string representation of this {@code Point2D}.
         * @since 1.2
         */
        public String toString() {
            return "Point2D.Float["+x+", "+y+"]";
        }

        /**
         * Use serialVersionUID from JDK 1.6 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = -2870572449815403710L;
    }

    /**
     * The {@code Double} class defines a point specified in
     * {@code double} precision.
     * @since 1.2
     */
    public static class Double extends Point2D implements Serializable {
        /**
         * The X coordinate of this {@code Point2D}.
         * @since 1.2
         * @serial
         */
        public double x;

        /**
         * The Y coordinate of this {@code Point2D}.
         * @since 1.2
         * @serial
         */
        public double y;

        /**
         * Constructs and initializes a {@code Point2D} with
         * coordinates (0,&nbsp;0).
         * @since 1.2
         */
        public Double() {
        }

        /**
         * Constructs and initializes a {@code Point2D} with the
         * specified coordinates.
         *
         * @param x the X coordinate of the newly
         *          constructed {@code Point2D}
         * @param y the Y coordinate of the newly
         *          constructed {@code Point2D}
         * @since 1.2
         */
        public Double(double x, double y) {
            this.x = x;
            this.y = y;
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
        public void setLocation(double x, double y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Returns a {@code String} that represents the value
         * of this {@code Point2D}.
         * @return a string representation of this {@code Point2D}.
         * @since 1.2
         */
        public String toString() {
            return "Point2D.Double["+x+", "+y+"]";
        }

        /**
         * Use serialVersionUID from JDK 1.6 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 6150783262733311327L;
    }

    /**
     * This is an abstract class that cannot be instantiated directly.
     * Type-specific implementation subclasses are available for
     * instantiation and provide a number of formats for storing
     * the information necessary to satisfy the various accessor
     * methods below.
     *
     * @see java.awt.geom.Point2D.Float
     * @see java.awt.geom.Point2D.Double
     * @see java.awt.Point
     * @since 1.2
     */
    protected Point2D() {
    }

    /**
     * Returns the X coordinate of this {@code Point2D} in
     * {@code double} precision.
     * @return the X coordinate of this {@code Point2D}.
     * @since 1.2
     */
    public abstract double getX();

    /**
     * Returns the Y coordinate of this {@code Point2D} in
     * {@code double} precision.
     * @return the Y coordinate of this {@code Point2D}.
     * @since 1.2
     */
    public abstract double getY();

    /**
     * Sets the location of this {@code Point2D} to the
     * specified {@code double} coordinates.
     *
     * @param x the new X coordinate of this {@code Point2D}
     * @param y the new Y coordinate of this {@code Point2D}
     * @since 1.2
     */
    public abstract void setLocation(double x, double y);

    /**
     * Sets the location of this {@code Point2D} to the same
     * coordinates as the specified {@code Point2D} object.
     * @param p the specified {@code Point2D} to which to set
     * this {@code Point2D}
     * @since 1.2
     */
    public void setLocation(Point2D p) {
        setLocation(p.getX(), p.getY());
    }

    /**
     * Returns the square of the distance between two points.
     *
     * @param x1 the X coordinate of the first specified point
     * @param y1 the Y coordinate of the first specified point
     * @param x2 the X coordinate of the second specified point
     * @param y2 the Y coordinate of the second specified point
     * @return the square of the distance between the two
     * sets of specified coordinates.
     * @since 1.2
     */
    public static double distanceSq(double x1, double y1,
                                    double x2, double y2)
    {
        x1 -= x2;
        y1 -= y2;
        return (x1 * x1 + y1 * y1);
    }

    /**
     * Returns the distance between two points.
     *
     * @param x1 the X coordinate of the first specified point
     * @param y1 the Y coordinate of the first specified point
     * @param x2 the X coordinate of the second specified point
     * @param y2 the Y coordinate of the second specified point
     * @return the distance between the two sets of specified
     * coordinates.
     * @since 1.2
     */
    public static double distance(double x1, double y1,
                                  double x2, double y2)
    {
        x1 -= x2;
        y1 -= y2;
        return Math.sqrt(x1 * x1 + y1 * y1);
    }

    /**
     * Returns the square of the distance from this
     * {@code Point2D} to a specified point.
     *
     * @param px the X coordinate of the specified point to be measured
     *           against this {@code Point2D}
     * @param py the Y coordinate of the specified point to be measured
     *           against this {@code Point2D}
     * @return the square of the distance between this
     * {@code Point2D} and the specified point.
     * @since 1.2
     */
    public double distanceSq(double px, double py) {
        px -= getX();
        py -= getY();
        return (px * px + py * py);
    }

    /**
     * Returns the square of the distance from this
     * {@code Point2D} to a specified {@code Point2D}.
     *
     * @param pt the specified point to be measured
     *           against this {@code Point2D}
     * @return the square of the distance between this
     * {@code Point2D} to a specified {@code Point2D}.
     * @since 1.2
     */
    public double distanceSq(Point2D pt) {
        double px = pt.getX() - this.getX();
        double py = pt.getY() - this.getY();
        return (px * px + py * py);
    }

    /**
     * Returns the distance from this {@code Point2D} to
     * a specified point.
     *
     * @param px the X coordinate of the specified point to be measured
     *           against this {@code Point2D}
     * @param py the Y coordinate of the specified point to be measured
     *           against this {@code Point2D}
     * @return the distance between this {@code Point2D}
     * and a specified point.
     * @since 1.2
     */
    public double distance(double px, double py) {
        px -= getX();
        py -= getY();
        return Math.sqrt(px * px + py * py);
    }

    /**
     * Returns the distance from this {@code Point2D} to a
     * specified {@code Point2D}.
     *
     * @param pt the specified point to be measured
     *           against this {@code Point2D}
     * @return the distance between this {@code Point2D} and
     * the specified {@code Point2D}.
     * @since 1.2
     */
    public double distance(Point2D pt) {
        double px = pt.getX() - this.getX();
        double py = pt.getY() - this.getY();
        return Math.sqrt(px * px + py * py);
    }

    /**
     * Creates a new object of the same class and with the
     * same contents as this object.
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

    /**
     * Returns the hashcode for this {@code Point2D}.
     * @return      a hash code for this {@code Point2D}.
     */
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits ^= java.lang.Double.doubleToLongBits(getY()) * 31;
        return (((int) bits) ^ ((int) (bits >> 32)));
    }

    /**
     * Determines whether or not two points are equal. Two instances of
     * {@code Point2D} are equal if the values of their
     * {@code x} and {@code y} member fields, representing
     * their position in the coordinate space, are the same.
     * @param obj an object to be compared with this {@code Point2D}
     * @return {@code true} if the object to be compared is
     *         an instance of {@code Point2D} and has
     *         the same values; {@code false} otherwise.
     * @since 1.2
     */
    public boolean equals(Object obj) {
        if (obj instanceof Point2D) {
            Point2D p2d = (Point2D) obj;
            return (getX() == p2d.getX()) && (getY() == p2d.getY());
        }
        return super.equals(obj);
    }
}
