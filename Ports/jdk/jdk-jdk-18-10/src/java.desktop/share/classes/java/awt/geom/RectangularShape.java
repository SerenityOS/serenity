/*
 * Copyright (c) 1997, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Shape;
import java.awt.Rectangle;
import java.beans.Transient;

/**
 * {@code RectangularShape} is the base class for a number of
 * {@link Shape} objects whose geometry is defined by a rectangular frame.
 * This class does not directly specify any specific geometry by
 * itself, but merely provides manipulation methods inherited by
 * a whole category of {@code Shape} objects.
 * The manipulation methods provided by this class can be used to
 * query and modify the rectangular frame, which provides a reference
 * for the subclasses to define their geometry.
 *
 * @author      Jim Graham
 * @since 1.2
 */
public abstract class RectangularShape implements Shape, Cloneable {

    /**
     * This is an abstract class that cannot be instantiated directly.
     *
     * @see Arc2D
     * @see Ellipse2D
     * @see Rectangle2D
     * @see RoundRectangle2D
     * @since 1.2
     */
    protected RectangularShape() {
    }

    /**
     * Returns the X coordinate of the upper-left corner of
     * the framing rectangle in {@code double} precision.
     * @return the X coordinate of the upper-left corner of
     * the framing rectangle.
     * @since 1.2
     */
    public abstract double getX();

    /**
     * Returns the Y coordinate of the upper-left corner of
     * the framing rectangle in {@code double} precision.
     * @return the Y coordinate of the upper-left corner of
     * the framing rectangle.
     * @since 1.2
     */
    public abstract double getY();

    /**
     * Returns the width of the framing rectangle in
     * {@code double} precision.
     * @return the width of the framing rectangle.
     * @since 1.2
     */
    public abstract double getWidth();

    /**
     * Returns the height of the framing rectangle
     * in {@code double} precision.
     * @return the height of the framing rectangle.
     * @since 1.2
     */
    public abstract double getHeight();

    /**
     * Returns the smallest X coordinate of the framing
     * rectangle of the {@code Shape} in {@code double}
     * precision.
     * @return the smallest X coordinate of the framing
     *          rectangle of the {@code Shape}.
     * @since 1.2
     */
    public double getMinX() {
        return getX();
    }

    /**
     * Returns the smallest Y coordinate of the framing
     * rectangle of the {@code Shape} in {@code double}
     * precision.
     * @return the smallest Y coordinate of the framing
     *          rectangle of the {@code Shape}.
     * @since 1.2
     */
    public double getMinY() {
        return getY();
    }

    /**
     * Returns the largest X coordinate of the framing
     * rectangle of the {@code Shape} in {@code double}
     * precision.
     * @return the largest X coordinate of the framing
     *          rectangle of the {@code Shape}.
     * @since 1.2
     */
    public double getMaxX() {
        return getX() + getWidth();
    }

    /**
     * Returns the largest Y coordinate of the framing
     * rectangle of the {@code Shape} in {@code double}
     * precision.
     * @return the largest Y coordinate of the framing
     *          rectangle of the {@code Shape}.
     * @since 1.2
     */
    public double getMaxY() {
        return getY() + getHeight();
    }

    /**
     * Returns the X coordinate of the center of the framing
     * rectangle of the {@code Shape} in {@code double}
     * precision.
     * @return the X coordinate of the center of the framing rectangle
     *          of the {@code Shape}.
     * @since 1.2
     */
    public double getCenterX() {
        return getX() + getWidth() / 2.0;
    }

    /**
     * Returns the Y coordinate of the center of the framing
     * rectangle of the {@code Shape} in {@code double}
     * precision.
     * @return the Y coordinate of the center of the framing rectangle
     *          of the {@code Shape}.
     * @since 1.2
     */
    public double getCenterY() {
        return getY() + getHeight() / 2.0;
    }

    /**
     * Returns the framing {@link Rectangle2D}
     * that defines the overall shape of this object.
     * @return a {@code Rectangle2D}, specified in
     * {@code double} coordinates.
     * @see #setFrame(double, double, double, double)
     * @see #setFrame(Point2D, Dimension2D)
     * @see #setFrame(Rectangle2D)
     * @since 1.2
     */
    @Transient
    public Rectangle2D getFrame() {
        return new Rectangle2D.Double(getX(), getY(), getWidth(), getHeight());
    }

    /**
     * Determines whether the {@code RectangularShape} is empty.
     * When the {@code RectangularShape} is empty, it encloses no
     * area.
     * @return {@code true} if the {@code RectangularShape} is empty;
     *          {@code false} otherwise.
     * @since 1.2
     */
    public abstract boolean isEmpty();

    /**
     * Sets the location and size of the framing rectangle of this
     * {@code Shape} to the specified rectangular values.
     *
     * @param x the X coordinate of the upper-left corner of the
     *          specified rectangular shape
     * @param y the Y coordinate of the upper-left corner of the
     *          specified rectangular shape
     * @param w the width of the specified rectangular shape
     * @param h the height of the specified rectangular shape
     * @see #getFrame
     * @since 1.2
     */
    public abstract void setFrame(double x, double y, double w, double h);

    /**
     * Sets the location and size of the framing rectangle of this
     * {@code Shape} to the specified {@link Point2D} and
     * {@link Dimension2D}, respectively.  The framing rectangle is used
     * by the subclasses of {@code RectangularShape} to define
     * their geometry.
     * @param loc the specified {@code Point2D}
     * @param size the specified {@code Dimension2D}
     * @see #getFrame
     * @since 1.2
     */
    public void setFrame(Point2D loc, Dimension2D size) {
        setFrame(loc.getX(), loc.getY(), size.getWidth(), size.getHeight());
    }

    /**
     * Sets the framing rectangle of this {@code Shape} to
     * be the specified {@code Rectangle2D}.  The framing rectangle is
     * used by the subclasses of {@code RectangularShape} to define
     * their geometry.
     * @param r the specified {@code Rectangle2D}
     * @see #getFrame
     * @since 1.2
     */
    public void setFrame(Rectangle2D r) {
        setFrame(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /**
     * Sets the diagonal of the framing rectangle of this {@code Shape}
     * based on the two specified coordinates.  The framing rectangle is
     * used by the subclasses of {@code RectangularShape} to define
     * their geometry.
     *
     * @param x1 the X coordinate of the start point of the specified diagonal
     * @param y1 the Y coordinate of the start point of the specified diagonal
     * @param x2 the X coordinate of the end point of the specified diagonal
     * @param y2 the Y coordinate of the end point of the specified diagonal
     * @since 1.2
     */
    public void setFrameFromDiagonal(double x1, double y1,
                                     double x2, double y2) {
        if (x2 < x1) {
            double t = x1;
            x1 = x2;
            x2 = t;
        }
        if (y2 < y1) {
            double t = y1;
            y1 = y2;
            y2 = t;
        }
        setFrame(x1, y1, x2 - x1, y2 - y1);
    }

    /**
     * Sets the diagonal of the framing rectangle of this {@code Shape}
     * based on two specified {@code Point2D} objects.  The framing
     * rectangle is used by the subclasses of {@code RectangularShape}
     * to define their geometry.
     *
     * @param p1 the start {@code Point2D} of the specified diagonal
     * @param p2 the end {@code Point2D} of the specified diagonal
     * @since 1.2
     */
    public void setFrameFromDiagonal(Point2D p1, Point2D p2) {
        setFrameFromDiagonal(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    /**
     * Sets the framing rectangle of this {@code Shape}
     * based on the specified center point coordinates and corner point
     * coordinates.  The framing rectangle is used by the subclasses of
     * {@code RectangularShape} to define their geometry.
     *
     * @param centerX the X coordinate of the specified center point
     * @param centerY the Y coordinate of the specified center point
     * @param cornerX the X coordinate of the specified corner point
     * @param cornerY the Y coordinate of the specified corner point
     * @since 1.2
     */
    public void setFrameFromCenter(double centerX, double centerY,
                                   double cornerX, double cornerY) {
        double halfW = Math.abs(cornerX - centerX);
        double halfH = Math.abs(cornerY - centerY);
        setFrame(centerX - halfW, centerY - halfH, halfW * 2.0, halfH * 2.0);
    }

    /**
     * Sets the framing rectangle of this {@code Shape} based on a
     * specified center {@code Point2D} and corner
     * {@code Point2D}.  The framing rectangle is used by the subclasses
     * of {@code RectangularShape} to define their geometry.
     * @param center the specified center {@code Point2D}
     * @param corner the specified corner {@code Point2D}
     * @since 1.2
     */
    public void setFrameFromCenter(Point2D center, Point2D corner) {
        setFrameFromCenter(center.getX(), center.getY(),
                           corner.getX(), corner.getY());
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public Rectangle getBounds() {
        double width = getWidth();
        double height = getHeight();
        if (width < 0 || height < 0) {
            return new Rectangle();
        }
        double x = getX();
        double y = getY();
        double x1 = Math.floor(x);
        double y1 = Math.floor(y);
        double x2 = Math.ceil(x + width);
        double y2 = Math.ceil(y + height);
        return new Rectangle((int) x1, (int) y1,
                                      (int) (x2 - x1), (int) (y2 - y1));
    }

    /**
     * Returns an iterator object that iterates along the
     * {@code Shape} object's boundary and provides access to a
     * flattened view of the outline of the {@code Shape}
     * object's geometry.
     * <p>
     * Only SEG_MOVETO, SEG_LINETO, and SEG_CLOSE point types will
     * be returned by the iterator.
     * <p>
     * The amount of subdivision of the curved segments is controlled
     * by the {@code flatness} parameter, which specifies the
     * maximum distance that any point on the unflattened transformed
     * curve can deviate from the returned flattened path segments.
     * An optional {@link AffineTransform} can
     * be specified so that the coordinates returned in the iteration are
     * transformed accordingly.
     * @param at an optional {@code AffineTransform} to be applied to the
     *          coordinates as they are returned in the iteration,
     *          or {@code null} if untransformed coordinates are desired.
     * @param flatness the maximum distance that the line segments used to
     *          approximate the curved segments are allowed to deviate
     *          from any point on the original curve
     * @return a {@code PathIterator} object that provides access to
     *          the {@code Shape} object's flattened geometry.
     * @since 1.2
     */
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new FlatteningPathIterator(getPathIterator(at), flatness);
    }

    /**
     * Creates a new object of the same class and with the same
     * contents as this object.
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
