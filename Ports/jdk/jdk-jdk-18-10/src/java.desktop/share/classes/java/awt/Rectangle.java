/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

import java.awt.geom.Rectangle2D;
import java.beans.Transient;
import java.io.Serial;

/**
 * A {@code Rectangle} specifies an area in a coordinate space that is
 * enclosed by the {@code Rectangle} object's upper-left point
 * {@code (x,y)}
 * in the coordinate space, its width, and its height.
 * <p>
 * A {@code Rectangle} object's {@code width} and
 * {@code height} are {@code public} fields. The constructors
 * that create a {@code Rectangle}, and the methods that can modify
 * one, do not prevent setting a negative value for width or height.
 * <p>
 * <a id="Empty">
 * A {@code Rectangle} whose width or height is exactly zero has location
 * along those axes with zero dimension, but is otherwise considered empty.</a>
 * The {@link #isEmpty} method will return true for such a {@code Rectangle}.
 * Methods which test if an empty {@code Rectangle} contains or intersects
 * a point or rectangle will always return false if either dimension is zero.
 * Methods which combine such a {@code Rectangle} with a point or rectangle
 * will include the location of the {@code Rectangle} on that axis in the
 * result as if the {@link #add(Point)} method were being called.
 * <p>
 * <a id="NonExistent">
 * A {@code Rectangle} whose width or height is negative has neither
 * location nor dimension along those axes with negative dimensions.
 * Such a {@code Rectangle} is treated as non-existent along those axes.
 * Such a {@code Rectangle} is also empty with respect to containment
 * calculations and methods which test if it contains or intersects a
 * point or rectangle will always return false.
 * Methods which combine such a {@code Rectangle} with a point or rectangle
 * will ignore the {@code Rectangle} entirely in generating the result.
 * If two {@code Rectangle} objects are combined and each has a negative
 * dimension, the result will have at least one negative dimension.
 * </a>
 * <p>
 * Methods which affect only the location of a {@code Rectangle} will
 * operate on its location regardless of whether or not it has a negative
 * or zero dimension along either axis.
 * <p>
 * Note that a {@code Rectangle} constructed with the default no-argument
 * constructor will have dimensions of {@code 0x0} and therefore be empty.
 * That {@code Rectangle} will still have a location of {@code (0,0)} and
 * will contribute that location to the union and add operations.
 * Code attempting to accumulate the bounds of a set of points should
 * therefore initially construct the {@code Rectangle} with a specifically
 * negative width and height or it should use the first point in the set
 * to construct the {@code Rectangle}.
 * For example:
 * <pre>{@code
 *     Rectangle bounds = new Rectangle(0, 0, -1, -1);
 *     for (int i = 0; i < points.length; i++) {
 *         bounds.add(points[i]);
 *     }
 * }</pre>
 * or if we know that the points array contains at least one point:
 * <pre>{@code
 *     Rectangle bounds = new Rectangle(points[0]);
 *     for (int i = 1; i < points.length; i++) {
 *         bounds.add(points[i]);
 *     }
 * }</pre>
 * <p>
 * This class uses 32-bit integers to store its location and dimensions.
 * Frequently operations may produce a result that exceeds the range of
 * a 32-bit integer.
 * The methods will calculate their results in a way that avoids any
 * 32-bit overflow for intermediate results and then choose the best
 * representation to store the final results back into the 32-bit fields
 * which hold the location and dimensions.
 * The location of the result will be stored into the {@link #x} and
 * {@link #y} fields by clipping the true result to the nearest 32-bit value.
 * The values stored into the {@link #width} and {@link #height} dimension
 * fields will be chosen as the 32-bit values that encompass the largest
 * part of the true result as possible.
 * Generally this means that the dimension will be clipped independently
 * to the range of 32-bit integers except that if the location had to be
 * moved to store it into its pair of 32-bit fields then the dimensions
 * will be adjusted relative to the "best representation" of the location.
 * If the true result had a negative dimension and was therefore
 * non-existent along one or both axes, the stored dimensions will be
 * negative numbers in those axes.
 * If the true result had a location that could be represented within
 * the range of 32-bit integers, but zero dimension along one or both
 * axes, then the stored dimensions will be zero in those axes.
 *
 * @author      Sami Shaio
 * @since 1.0
 */
public class Rectangle extends Rectangle2D
    implements Shape, java.io.Serializable
{

    /**
     * The X coordinate of the upper-left corner of the {@code Rectangle}.
     *
     * @serial
     * @see #setLocation(int, int)
     * @see #getLocation()
     * @since 1.0
     */
    public int x;

    /**
     * The Y coordinate of the upper-left corner of the {@code Rectangle}.
     *
     * @serial
     * @see #setLocation(int, int)
     * @see #getLocation()
     * @since 1.0
     */
    public int y;

    /**
     * The width of the {@code Rectangle}.
     * @serial
     * @see #setSize(int, int)
     * @see #getSize()
     * @since 1.0
     */
    public int width;

    /**
     * The height of the {@code Rectangle}.
     *
     * @serial
     * @see #setSize(int, int)
     * @see #getSize()
     * @since 1.0
     */
    public int height;

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
     @Serial
     private static final long serialVersionUID = -4345857070255674764L;

    /**
     * Initialize JNI field and method IDs
     */
    private static native void initIDs();

    static {
        /* ensure that the necessary native libraries are loaded */
        Toolkit.loadLibraries();
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }
    }

    /**
     * Constructs a new {@code Rectangle} whose upper-left corner
     * is at (0,&nbsp;0) in the coordinate space, and whose width and
     * height are both zero.
     */
    public Rectangle() {
        this(0, 0, 0, 0);
    }

    /**
     * Constructs a new {@code Rectangle}, initialized to match
     * the values of the specified {@code Rectangle}.
     * @param r  the {@code Rectangle} from which to copy initial values
     *           to a newly constructed {@code Rectangle}
     * @since 1.1
     */
    public Rectangle(Rectangle r) {
        this(r.x, r.y, r.width, r.height);
    }

    /**
     * Constructs a new {@code Rectangle} whose upper-left corner is
     * specified as
     * {@code (x,y)} and whose width and height
     * are specified by the arguments of the same name.
     * @param     x the specified X coordinate
     * @param     y the specified Y coordinate
     * @param     width    the width of the {@code Rectangle}
     * @param     height   the height of the {@code Rectangle}
     * @since 1.0
     */
    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Constructs a new {@code Rectangle} whose upper-left corner
     * is at (0,&nbsp;0) in the coordinate space, and whose width and
     * height are specified by the arguments of the same name.
     * @param width the width of the {@code Rectangle}
     * @param height the height of the {@code Rectangle}
     */
    public Rectangle(int width, int height) {
        this(0, 0, width, height);
    }

    /**
     * Constructs a new {@code Rectangle} whose upper-left corner is
     * specified by the {@link Point} argument, and
     * whose width and height are specified by the
     * {@link Dimension} argument.
     * @param p a {@code Point} that is the upper-left corner of
     * the {@code Rectangle}
     * @param d a {@code Dimension}, representing the
     * width and height of the {@code Rectangle}
     */
    public Rectangle(Point p, Dimension d) {
        this(p.x, p.y, d.width, d.height);
    }

    /**
     * Constructs a new {@code Rectangle} whose upper-left corner is the
     * specified {@code Point}, and whose width and height are both zero.
     * @param p a {@code Point} that is the top left corner
     * of the {@code Rectangle}
     */
    public Rectangle(Point p) {
        this(p.x, p.y, 0, 0);
    }

    /**
     * Constructs a new {@code Rectangle} whose top left corner is
     * (0,&nbsp;0) and whose width and height are specified
     * by the {@code Dimension} argument.
     * @param d a {@code Dimension}, specifying width and height
     */
    public Rectangle(Dimension d) {
        this(0, 0, d.width, d.height);
    }

    /**
     * Returns the X coordinate of the bounding {@code Rectangle} in
     * {@code double} precision.
     * @return the X coordinate of the bounding {@code Rectangle}.
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the Y coordinate of the bounding {@code Rectangle} in
     * {@code double} precision.
     * @return the Y coordinate of the bounding {@code Rectangle}.
     */
    public double getY() {
        return y;
    }

    /**
     * Returns the width of the bounding {@code Rectangle} in
     * {@code double} precision.
     * @return the width of the bounding {@code Rectangle}.
     */
    public double getWidth() {
        return width;
    }

    /**
     * Returns the height of the bounding {@code Rectangle} in
     * {@code double} precision.
     * @return the height of the bounding {@code Rectangle}.
     */
    public double getHeight() {
        return height;
    }

    /**
     * Gets the bounding {@code Rectangle} of this {@code Rectangle}.
     * <p>
     * This method is included for completeness, to parallel the
     * {@code getBounds} method of
     * {@link Component}.
     * @return    a new {@code Rectangle}, equal to the
     * bounding {@code Rectangle} for this {@code Rectangle}.
     * @see       java.awt.Component#getBounds
     * @see       #setBounds(Rectangle)
     * @see       #setBounds(int, int, int, int)
     * @since     1.1
     */
    @Transient
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public Rectangle2D getBounds2D() {
        return new Rectangle(x, y, width, height);
    }

    /**
     * Sets the bounding {@code Rectangle} of this {@code Rectangle}
     * to match the specified {@code Rectangle}.
     * <p>
     * This method is included for completeness, to parallel the
     * {@code setBounds} method of {@code Component}.
     * @param r the specified {@code Rectangle}
     * @see       #getBounds
     * @see       java.awt.Component#setBounds(java.awt.Rectangle)
     * @since     1.1
     */
    public void setBounds(Rectangle r) {
        setBounds(r.x, r.y, r.width, r.height);
    }

    /**
     * Sets the bounding {@code Rectangle} of this
     * {@code Rectangle} to the specified
     * {@code x}, {@code y}, {@code width},
     * and {@code height}.
     * <p>
     * This method is included for completeness, to parallel the
     * {@code setBounds} method of {@code Component}.
     * @param x the new X coordinate for the upper-left
     *                    corner of this {@code Rectangle}
     * @param y the new Y coordinate for the upper-left
     *                    corner of this {@code Rectangle}
     * @param width the new width for this {@code Rectangle}
     * @param height the new height for this {@code Rectangle}
     * @see       #getBounds
     * @see       java.awt.Component#setBounds(int, int, int, int)
     * @since     1.1
     */
    public void setBounds(int x, int y, int width, int height) {
        reshape(x, y, width, height);
    }

    /**
     * Sets the bounds of this {@code Rectangle} to the integer bounds
     * which encompass the specified {@code x}, {@code y}, {@code width},
     * and {@code height}.
     * If the parameters specify a {@code Rectangle} that exceeds the
     * maximum range of integers, the result will be the best
     * representation of the specified {@code Rectangle} intersected
     * with the maximum integer bounds.
     * @param x the X coordinate of the upper-left corner of
     *                  the specified rectangle
     * @param y the Y coordinate of the upper-left corner of
     *                  the specified rectangle
     * @param width the width of the specified rectangle
     * @param height the new height of the specified rectangle
     */
    public void setRect(double x, double y, double width, double height) {
        int newx, newy, neww, newh;

        if (x > 2.0 * Integer.MAX_VALUE) {
            // Too far in positive X direction to represent...
            // We cannot even reach the left side of the specified
            // rectangle even with both x & width set to MAX_VALUE.
            // The intersection with the "maximal integer rectangle"
            // is non-existent so we should use a width < 0.
            // REMIND: Should we try to determine a more "meaningful"
            // adjusted value for neww than just "-1"?
            newx = Integer.MAX_VALUE;
            neww = -1;
        } else {
            newx = clip(x, false);
            if (width >= 0) width += x-newx;
            neww = clip(width, width >= 0);
        }

        if (y > 2.0 * Integer.MAX_VALUE) {
            // Too far in positive Y direction to represent...
            newy = Integer.MAX_VALUE;
            newh = -1;
        } else {
            newy = clip(y, false);
            if (height >= 0) height += y-newy;
            newh = clip(height, height >= 0);
        }

        reshape(newx, newy, neww, newh);
    }
    // Return best integer representation for v, clipped to integer
    // range and floor-ed or ceiling-ed, depending on the boolean.
    private static int clip(double v, boolean doceil) {
        if (v <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (v >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) (doceil ? Math.ceil(v) : Math.floor(v));
    }

    /**
     * Sets the bounding {@code Rectangle} of this
     * {@code Rectangle} to the specified
     * {@code x}, {@code y}, {@code width},
     * and {@code height}.
     *
     * @param x the new X coordinate for the upper-left
     *                    corner of this {@code Rectangle}
     * @param y the new Y coordinate for the upper-left
     *                    corner of this {@code Rectangle}
     * @param width the new width for this {@code Rectangle}
     * @param height the new height for this {@code Rectangle}
     * @deprecated As of JDK version 1.1,
     * replaced by {@code setBounds(int, int, int, int)}.
     */
    @Deprecated
    public void reshape(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Returns the location of this {@code Rectangle}.
     * <p>
     * This method is included for completeness, to parallel the
     * {@code getLocation} method of {@code Component}.
     * @return the {@code Point} that is the upper-left corner of
     *                  this {@code Rectangle}.
     * @see       java.awt.Component#getLocation
     * @see       #setLocation(Point)
     * @see       #setLocation(int, int)
     * @since     1.1
     */
    public Point getLocation() {
        return new Point(x, y);
    }

    /**
     * Moves this {@code Rectangle} to the specified location.
     * <p>
     * This method is included for completeness, to parallel the
     * {@code setLocation} method of {@code Component}.
     * @param p the {@code Point} specifying the new location
     *                for this {@code Rectangle}
     * @see       java.awt.Component#setLocation(java.awt.Point)
     * @see       #getLocation
     * @since     1.1
     */
    public void setLocation(Point p) {
        setLocation(p.x, p.y);
    }

    /**
     * Moves this {@code Rectangle} to the specified location.
     * <p>
     * This method is included for completeness, to parallel the
     * {@code setLocation} method of {@code Component}.
     * @param x the X coordinate of the new location
     * @param y the Y coordinate of the new location
     * @see       #getLocation
     * @see       java.awt.Component#setLocation(int, int)
     * @since     1.1
     */
    public void setLocation(int x, int y) {
        move(x, y);
    }

    /**
     * Moves this {@code Rectangle} to the specified location.
     *
     * @param x the X coordinate of the new location
     * @param y the Y coordinate of the new location
     * @deprecated As of JDK version 1.1,
     * replaced by {@code setLocation(int, int)}.
     */
    @Deprecated
    public void move(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Translates this {@code Rectangle} the indicated distance,
     * to the right along the X coordinate axis, and
     * downward along the Y coordinate axis.
     * @param dx the distance to move this {@code Rectangle}
     *                 along the X axis
     * @param dy the distance to move this {@code Rectangle}
     *                 along the Y axis
     * @see       java.awt.Rectangle#setLocation(int, int)
     * @see       java.awt.Rectangle#setLocation(java.awt.Point)
     */
    public void translate(int dx, int dy) {
        int oldv = this.x;
        int newv = oldv + dx;
        if (dx < 0) {
            // moving leftward
            if (newv > oldv) {
                // negative overflow
                // Only adjust width if it was valid (>= 0).
                if (width >= 0) {
                    // The right edge is now conceptually at
                    // newv+width, but we may move newv to prevent
                    // overflow.  But we want the right edge to
                    // remain at its new location in spite of the
                    // clipping.  Think of the following adjustment
                    // conceptually the same as:
                    // width += newv; newv = MIN_VALUE; width -= newv;
                    width += newv - Integer.MIN_VALUE;
                    // width may go negative if the right edge went past
                    // MIN_VALUE, but it cannot overflow since it cannot
                    // have moved more than MIN_VALUE and any non-negative
                    // number + MIN_VALUE does not overflow.
                }
                newv = Integer.MIN_VALUE;
            }
        } else {
            // moving rightward (or staying still)
            if (newv < oldv) {
                // positive overflow
                if (width >= 0) {
                    // Conceptually the same as:
                    // width += newv; newv = MAX_VALUE; width -= newv;
                    width += newv - Integer.MAX_VALUE;
                    // With large widths and large displacements
                    // we may overflow so we need to check it.
                    if (width < 0) width = Integer.MAX_VALUE;
                }
                newv = Integer.MAX_VALUE;
            }
        }
        this.x = newv;

        oldv = this.y;
        newv = oldv + dy;
        if (dy < 0) {
            // moving upward
            if (newv > oldv) {
                // negative overflow
                if (height >= 0) {
                    height += newv - Integer.MIN_VALUE;
                    // See above comment about no overflow in this case
                }
                newv = Integer.MIN_VALUE;
            }
        } else {
            // moving downward (or staying still)
            if (newv < oldv) {
                // positive overflow
                if (height >= 0) {
                    height += newv - Integer.MAX_VALUE;
                    if (height < 0) height = Integer.MAX_VALUE;
                }
                newv = Integer.MAX_VALUE;
            }
        }
        this.y = newv;
    }

    /**
     * Gets the size of this {@code Rectangle}, represented by
     * the returned {@code Dimension}.
     * <p>
     * This method is included for completeness, to parallel the
     * {@code getSize} method of {@code Component}.
     * @return a {@code Dimension}, representing the size of
     *            this {@code Rectangle}.
     * @see       java.awt.Component#getSize
     * @see       #setSize(Dimension)
     * @see       #setSize(int, int)
     * @since     1.1
     */
    public Dimension getSize() {
        return new Dimension(width, height);
    }

    /**
     * Sets the size of this {@code Rectangle} to match the
     * specified {@code Dimension}.
     * <p>
     * This method is included for completeness, to parallel the
     * {@code setSize} method of {@code Component}.
     * @param d the new size for the {@code Dimension} object
     * @see       java.awt.Component#setSize(java.awt.Dimension)
     * @see       #getSize
     * @since     1.1
     */
    public void setSize(Dimension d) {
        setSize(d.width, d.height);
    }

    /**
     * Sets the size of this {@code Rectangle} to the specified
     * width and height.
     * <p>
     * This method is included for completeness, to parallel the
     * {@code setSize} method of {@code Component}.
     * @param width the new width for this {@code Rectangle}
     * @param height the new height for this {@code Rectangle}
     * @see       java.awt.Component#setSize(int, int)
     * @see       #getSize
     * @since     1.1
     */
    public void setSize(int width, int height) {
        resize(width, height);
    }

    /**
     * Sets the size of this {@code Rectangle} to the specified
     * width and height.
     *
     * @param width the new width for this {@code Rectangle}
     * @param height the new height for this {@code Rectangle}
     * @deprecated As of JDK version 1.1,
     * replaced by {@code setSize(int, int)}.
     */
    @Deprecated
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Checks whether or not this {@code Rectangle} contains the
     * specified {@code Point}.
     * @param p the {@code Point} to test
     * @return    {@code true} if the specified {@code Point}
     *            is inside this {@code Rectangle};
     *            {@code false} otherwise.
     * @since     1.1
     */
    public boolean contains(Point p) {
        return contains(p.x, p.y);
    }

    /**
     * Checks whether or not this {@code Rectangle} contains the
     * point at the specified location {@code (x,y)}.
     *
     * @param  x the specified X coordinate
     * @param  y the specified Y coordinate
     * @return    {@code true} if the point
     *            {@code (x,y)} is inside this
     *            {@code Rectangle};
     *            {@code false} otherwise.
     * @since     1.1
     */
    public boolean contains(int x, int y) {
        return inside(x, y);
    }

    /**
     * Checks whether or not this {@code Rectangle} entirely contains
     * the specified {@code Rectangle}.
     *
     * @param     r   the specified {@code Rectangle}
     * @return    {@code true} if the {@code Rectangle}
     *            is contained entirely inside this {@code Rectangle};
     *            {@code false} otherwise
     * @since     1.2
     */
    public boolean contains(Rectangle r) {
        return contains(r.x, r.y, r.width, r.height);
    }

    /**
     * Checks whether this {@code Rectangle} entirely contains
     * the {@code Rectangle}
     * at the specified location {@code (X,Y)} with the
     * specified dimensions {@code (W,H)}.
     * @param     X the specified X coordinate
     * @param     Y the specified Y coordinate
     * @param     W   the width of the {@code Rectangle}
     * @param     H   the height of the {@code Rectangle}
     * @return    {@code true} if the {@code Rectangle} specified by
     *            {@code (X, Y, W, H)}
     *            is entirely enclosed inside this {@code Rectangle};
     *            {@code false} otherwise.
     * @since     1.1
     */
    public boolean contains(int X, int Y, int W, int H) {
        int w = this.width;
        int h = this.height;
        if ((w | h | W | H) < 0) {
            // At least one of the dimensions is negative...
            return false;
        }
        // Note: if any dimension is zero, tests below must return false...
        int x = this.x;
        int y = this.y;
        if (X < x || Y < y) {
            return false;
        }
        w += x;
        W += X;
        if (W <= X) {
            // X+W overflowed or W was zero, return false if...
            // either original w or W was zero or
            // x+w did not overflow or
            // the overflowed x+w is smaller than the overflowed X+W
            if (w >= x || W > w) return false;
        } else {
            // X+W did not overflow and W was not zero, return false if...
            // original w was zero or
            // x+w did not overflow and x+w is smaller than X+W
            if (w >= x && W > w) return false;
        }
        h += y;
        H += Y;
        if (H <= Y) {
            if (h >= y || H > h) return false;
        } else {
            if (h >= y && H > h) return false;
        }
        return true;
    }

    /**
     * Checks whether or not this {@code Rectangle} contains the
     * point at the specified location {@code (X,Y)}.
     *
     * @param  X the specified X coordinate
     * @param  Y the specified Y coordinate
     * @return    {@code true} if the point
     *            {@code (X,Y)} is inside this
     *            {@code Rectangle};
     *            {@code false} otherwise.
     * @deprecated As of JDK version 1.1,
     * replaced by {@code contains(int, int)}.
     */
    @Deprecated
    public boolean inside(int X, int Y) {
        int w = this.width;
        int h = this.height;
        if ((w | h) < 0) {
            // At least one of the dimensions is negative...
            return false;
        }
        // Note: if either dimension is zero, tests below must return false...
        int x = this.x;
        int y = this.y;
        if (X < x || Y < y) {
            return false;
        }
        w += x;
        h += y;
        //    overflow || intersect
        return ((w < x || w > X) &&
                (h < y || h > Y));
    }

    /**
     * Determines whether or not this {@code Rectangle} and the specified
     * {@code Rectangle} intersect. Two rectangles intersect if
     * their intersection is nonempty.
     *
     * @param r the specified {@code Rectangle}
     * @return    {@code true} if the specified {@code Rectangle}
     *            and this {@code Rectangle} intersect;
     *            {@code false} otherwise.
     */
    public boolean intersects(Rectangle r) {
        int tw = this.width;
        int th = this.height;
        int rw = r.width;
        int rh = r.height;
        if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
            return false;
        }
        int tx = this.x;
        int ty = this.y;
        int rx = r.x;
        int ry = r.y;
        rw += rx;
        rh += ry;
        tw += tx;
        th += ty;
        //      overflow || intersect
        return ((rw < rx || rw > tx) &&
                (rh < ry || rh > ty) &&
                (tw < tx || tw > rx) &&
                (th < ty || th > ry));
    }

    /**
     * Computes the intersection of this {@code Rectangle} with the
     * specified {@code Rectangle}. Returns a new {@code Rectangle}
     * that represents the intersection of the two rectangles.
     * If the two rectangles do not intersect, the result will be
     * an empty rectangle.
     *
     * @param     r   the specified {@code Rectangle}
     * @return    the largest {@code Rectangle} contained in both the
     *            specified {@code Rectangle} and in
     *            this {@code Rectangle}; or if the rectangles
     *            do not intersect, an empty rectangle.
     */
    public Rectangle intersection(Rectangle r) {
        int tx1 = this.x;
        int ty1 = this.y;
        int rx1 = r.x;
        int ry1 = r.y;
        long tx2 = tx1; tx2 += this.width;
        long ty2 = ty1; ty2 += this.height;
        long rx2 = rx1; rx2 += r.width;
        long ry2 = ry1; ry2 += r.height;
        if (tx1 < rx1) tx1 = rx1;
        if (ty1 < ry1) ty1 = ry1;
        if (tx2 > rx2) tx2 = rx2;
        if (ty2 > ry2) ty2 = ry2;
        tx2 -= tx1;
        ty2 -= ty1;
        // tx2,ty2 will never overflow (they will never be
        // larger than the smallest of the two source w,h)
        // they might underflow, though...
        if (tx2 < Integer.MIN_VALUE) tx2 = Integer.MIN_VALUE;
        if (ty2 < Integer.MIN_VALUE) ty2 = Integer.MIN_VALUE;
        return new Rectangle(tx1, ty1, (int) tx2, (int) ty2);
    }

    /**
     * Computes the union of this {@code Rectangle} with the
     * specified {@code Rectangle}. Returns a new
     * {@code Rectangle} that
     * represents the union of the two rectangles.
     * <p>
     * If either {@code Rectangle} has any dimension less than zero
     * the rules for <a href=#NonExistent>non-existent</a> rectangles
     * apply.
     * If only one has a dimension less than zero, then the result
     * will be a copy of the other {@code Rectangle}.
     * If both have dimension less than zero, then the result will
     * have at least one dimension less than zero.
     * <p>
     * If the resulting {@code Rectangle} would have a dimension
     * too large to be expressed as an {@code int}, the result
     * will have a dimension of {@code Integer.MAX_VALUE} along
     * that dimension.
     * @param r the specified {@code Rectangle}
     * @return    the smallest {@code Rectangle} containing both
     *            the specified {@code Rectangle} and this
     *            {@code Rectangle}.
     */
    public Rectangle union(Rectangle r) {
        long tx2 = this.width;
        long ty2 = this.height;
        if ((tx2 | ty2) < 0) {
            // This rectangle has negative dimensions...
            // If r has non-negative dimensions then it is the answer.
            // If r is non-existent (has a negative dimension), then both
            // are non-existent and we can return any non-existent rectangle
            // as an answer.  Thus, returning r meets that criterion.
            // Either way, r is our answer.
            return new Rectangle(r);
        }
        long rx2 = r.width;
        long ry2 = r.height;
        if ((rx2 | ry2) < 0) {
            return new Rectangle(this);
        }
        int tx1 = this.x;
        int ty1 = this.y;
        tx2 += tx1;
        ty2 += ty1;
        int rx1 = r.x;
        int ry1 = r.y;
        rx2 += rx1;
        ry2 += ry1;
        if (tx1 > rx1) tx1 = rx1;
        if (ty1 > ry1) ty1 = ry1;
        if (tx2 < rx2) tx2 = rx2;
        if (ty2 < ry2) ty2 = ry2;
        tx2 -= tx1;
        ty2 -= ty1;
        // tx2,ty2 will never underflow since both original rectangles
        // were already proven to be non-empty
        // they might overflow, though...
        if (tx2 > Integer.MAX_VALUE) tx2 = Integer.MAX_VALUE;
        if (ty2 > Integer.MAX_VALUE) ty2 = Integer.MAX_VALUE;
        return new Rectangle(tx1, ty1, (int) tx2, (int) ty2);
    }

    /**
     * Adds a point, specified by the integer arguments {@code newx,newy}
     * to the bounds of this {@code Rectangle}.
     * <p>
     * If this {@code Rectangle} has any dimension less than zero,
     * the rules for <a href=#NonExistent>non-existent</a>
     * rectangles apply.
     * In that case, the new bounds of this {@code Rectangle} will
     * have a location equal to the specified coordinates and
     * width and height equal to zero.
     * <p>
     * After adding a point, a call to {@code contains} with the
     * added point as an argument does not necessarily return
     * {@code true}. The {@code contains} method does not
     * return {@code true} for points on the right or bottom
     * edges of a {@code Rectangle}. Therefore, if the added point
     * falls on the right or bottom edge of the enlarged
     * {@code Rectangle}, {@code contains} returns
     * {@code false} for that point.
     * If the specified point must be contained within the new
     * {@code Rectangle}, a 1x1 rectangle should be added instead:
     * <pre>
     *     r.add(new Rectangle(newx, newy, 1, 1));
     * </pre>
     * @param newx the X coordinate of the new point
     * @param newy the Y coordinate of the new point
     */
    public void add(int newx, int newy) {
        if ((width | height) < 0) {
            this.x = newx;
            this.y = newy;
            this.width = this.height = 0;
            return;
        }
        int x1 = this.x;
        int y1 = this.y;
        long x2 = this.width;
        long y2 = this.height;
        x2 += x1;
        y2 += y1;
        if (x1 > newx) x1 = newx;
        if (y1 > newy) y1 = newy;
        if (x2 < newx) x2 = newx;
        if (y2 < newy) y2 = newy;
        x2 -= x1;
        y2 -= y1;
        if (x2 > Integer.MAX_VALUE) x2 = Integer.MAX_VALUE;
        if (y2 > Integer.MAX_VALUE) y2 = Integer.MAX_VALUE;
        reshape(x1, y1, (int) x2, (int) y2);
    }

    /**
     * Adds the specified {@code Point} to the bounds of this
     * {@code Rectangle}.
     * <p>
     * If this {@code Rectangle} has any dimension less than zero,
     * the rules for <a href=#NonExistent>non-existent</a>
     * rectangles apply.
     * In that case, the new bounds of this {@code Rectangle} will
     * have a location equal to the coordinates of the specified
     * {@code Point} and width and height equal to zero.
     * <p>
     * After adding a {@code Point}, a call to {@code contains}
     * with the added {@code Point} as an argument does not
     * necessarily return {@code true}. The {@code contains}
     * method does not return {@code true} for points on the right
     * or bottom edges of a {@code Rectangle}. Therefore if the added
     * {@code Point} falls on the right or bottom edge of the
     * enlarged {@code Rectangle}, {@code contains} returns
     * {@code false} for that {@code Point}.
     * If the specified point must be contained within the new
     * {@code Rectangle}, a 1x1 rectangle should be added instead:
     * <pre>
     *     r.add(new Rectangle(pt, new Dimension(1, 1)));
     * </pre>
     * @param pt the new {@code Point} to add to this
     *           {@code Rectangle}
     */
    public void add(Point pt) {
        add(pt.x, pt.y);
    }

    /**
     * Adds a {@code Rectangle} to this {@code Rectangle}.
     * The resulting {@code Rectangle} is the union of the two
     * rectangles.
     * <p>
     * If either {@code Rectangle} has any dimension less than 0, the
     * result will have the dimensions of the other {@code Rectangle}.
     * If both {@code Rectangle}s have at least one dimension less
     * than 0, the result will have at least one dimension less than 0.
     * <p>
     * If either {@code Rectangle} has one or both dimensions equal
     * to 0, the result along those axes with 0 dimensions will be
     * equivalent to the results obtained by adding the corresponding
     * origin coordinate to the result rectangle along that axis,
     * similar to the operation of the {@link #add(Point)} method,
     * but contribute no further dimension beyond that.
     * <p>
     * If the resulting {@code Rectangle} would have a dimension
     * too large to be expressed as an {@code int}, the result
     * will have a dimension of {@code Integer.MAX_VALUE} along
     * that dimension.
     * @param  r the specified {@code Rectangle}
     */
    public void add(Rectangle r) {
        long tx2 = this.width;
        long ty2 = this.height;
        if ((tx2 | ty2) < 0) {
            reshape(r.x, r.y, r.width, r.height);
        }
        long rx2 = r.width;
        long ry2 = r.height;
        if ((rx2 | ry2) < 0) {
            return;
        }
        int tx1 = this.x;
        int ty1 = this.y;
        tx2 += tx1;
        ty2 += ty1;
        int rx1 = r.x;
        int ry1 = r.y;
        rx2 += rx1;
        ry2 += ry1;
        if (tx1 > rx1) tx1 = rx1;
        if (ty1 > ry1) ty1 = ry1;
        if (tx2 < rx2) tx2 = rx2;
        if (ty2 < ry2) ty2 = ry2;
        tx2 -= tx1;
        ty2 -= ty1;
        // tx2,ty2 will never underflow since both original
        // rectangles were non-empty
        // they might overflow, though...
        if (tx2 > Integer.MAX_VALUE) tx2 = Integer.MAX_VALUE;
        if (ty2 > Integer.MAX_VALUE) ty2 = Integer.MAX_VALUE;
        reshape(tx1, ty1, (int) tx2, (int) ty2);
    }

    /**
     * Resizes the {@code Rectangle} both horizontally and vertically.
     * <p>
     * This method modifies the {@code Rectangle} so that it is
     * {@code h} units larger on both the left and right side,
     * and {@code v} units larger at both the top and bottom.
     * <p>
     * The new {@code Rectangle} has {@code (x - h, y - v)}
     * as its upper-left corner,
     * width of {@code (width + 2h)},
     * and a height of {@code (height + 2v)}.
     * <p>
     * If negative values are supplied for {@code h} and
     * {@code v}, the size of the {@code Rectangle}
     * decreases accordingly.
     * The {@code grow} method will check for integer overflow
     * and underflow, but does not check whether the resulting
     * values of {@code width} and {@code height} grow
     * from negative to non-negative or shrink from non-negative
     * to negative.
     * @param h the horizontal expansion
     * @param v the vertical expansion
     */
    public void grow(int h, int v) {
        long x0 = this.x;
        long y0 = this.y;
        long x1 = this.width;
        long y1 = this.height;
        x1 += x0;
        y1 += y0;

        x0 -= h;
        y0 -= v;
        x1 += h;
        y1 += v;

        if (x1 < x0) {
            // Non-existent in X direction
            // Final width must remain negative so subtract x0 before
            // it is clipped so that we avoid the risk that the clipping
            // of x0 will reverse the ordering of x0 and x1.
            x1 -= x0;
            if (x1 < Integer.MIN_VALUE) x1 = Integer.MIN_VALUE;
            if (x0 < Integer.MIN_VALUE) x0 = Integer.MIN_VALUE;
            else if (x0 > Integer.MAX_VALUE) x0 = Integer.MAX_VALUE;
        } else { // (x1 >= x0)
            // Clip x0 before we subtract it from x1 in case the clipping
            // affects the representable area of the rectangle.
            if (x0 < Integer.MIN_VALUE) x0 = Integer.MIN_VALUE;
            else if (x0 > Integer.MAX_VALUE) x0 = Integer.MAX_VALUE;
            x1 -= x0;
            // The only way x1 can be negative now is if we clipped
            // x0 against MIN and x1 is less than MIN - in which case
            // we want to leave the width negative since the result
            // did not intersect the representable area.
            if (x1 < Integer.MIN_VALUE) x1 = Integer.MIN_VALUE;
            else if (x1 > Integer.MAX_VALUE) x1 = Integer.MAX_VALUE;
        }

        if (y1 < y0) {
            // Non-existent in Y direction
            y1 -= y0;
            if (y1 < Integer.MIN_VALUE) y1 = Integer.MIN_VALUE;
            if (y0 < Integer.MIN_VALUE) y0 = Integer.MIN_VALUE;
            else if (y0 > Integer.MAX_VALUE) y0 = Integer.MAX_VALUE;
        } else { // (y1 >= y0)
            if (y0 < Integer.MIN_VALUE) y0 = Integer.MIN_VALUE;
            else if (y0 > Integer.MAX_VALUE) y0 = Integer.MAX_VALUE;
            y1 -= y0;
            if (y1 < Integer.MIN_VALUE) y1 = Integer.MIN_VALUE;
            else if (y1 > Integer.MAX_VALUE) y1 = Integer.MAX_VALUE;
        }

        reshape((int) x0, (int) y0, (int) x1, (int) y1);
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public boolean isEmpty() {
        return (width <= 0) || (height <= 0);
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public int outcode(double x, double y) {
        /*
         * Note on casts to double below.  If the arithmetic of
         * x+w or y+h is done in int, then we may get integer
         * overflow. By converting to double before the addition
         * we force the addition to be carried out in double to
         * avoid overflow in the comparison.
         *
         * See bug 4320890 for problems that this can cause.
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
    public Rectangle2D createIntersection(Rectangle2D r) {
        if (r instanceof Rectangle) {
            return intersection((Rectangle) r);
        }
        Rectangle2D dest = new Rectangle2D.Double();
        Rectangle2D.intersect(this, r, dest);
        return dest;
    }

    /**
     * {@inheritDoc}
     * @since 1.2
     */
    public Rectangle2D createUnion(Rectangle2D r) {
        if (r instanceof Rectangle) {
            return union((Rectangle) r);
        }
        Rectangle2D dest = new Rectangle2D.Double();
        Rectangle2D.union(this, r, dest);
        return dest;
    }

    /**
     * Checks whether two rectangles are equal.
     * <p>
     * The result is {@code true} if and only if the argument is not
     * {@code null} and is a {@code Rectangle} object that has the
     * same upper-left corner, width, and height as
     * this {@code Rectangle}.
     * @param obj the {@code Object} to compare with
     *                this {@code Rectangle}
     * @return    {@code true} if the objects are equal;
     *            {@code false} otherwise.
     */
    public boolean equals(Object obj) {
        if (obj instanceof Rectangle) {
            Rectangle r = (Rectangle)obj;
            return ((x == r.x) &&
                    (y == r.y) &&
                    (width == r.width) &&
                    (height == r.height));
        }
        return super.equals(obj);
    }

    /**
     * Returns a {@code String} representing this
     * {@code Rectangle} and its values.
     * @return a {@code String} representing this
     *               {@code Rectangle} object's coordinate and size values.
     */
    public String toString() {
        return getClass().getName() + "[x=" + x + ",y=" + y + ",width=" + width + ",height=" + height + "]";
    }
}
