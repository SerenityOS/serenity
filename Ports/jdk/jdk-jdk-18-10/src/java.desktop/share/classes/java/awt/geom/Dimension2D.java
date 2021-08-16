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

/**
 * The {@code Dimension2D} class is to encapsulate a width
 * and a height dimension.
 * <p>
 * This class is only the abstract superclass for all objects that
 * store a 2D dimension.
 * The actual storage representation of the sizes is left to
 * the subclass.
 *
 * @author      Jim Graham
 * @since 1.2
 */
public abstract class Dimension2D implements Cloneable {

    /**
     * This is an abstract class that cannot be instantiated directly.
     * Type-specific implementation subclasses are available for
     * instantiation and provide a number of formats for storing
     * the information necessary to satisfy the various accessor
     * methods below.
     *
     * @see java.awt.Dimension
     * @since 1.2
     */
    protected Dimension2D() {
    }

    /**
     * Returns the width of this {@code Dimension} in double
     * precision.
     * @return the width of this {@code Dimension}.
     * @since 1.2
     */
    public abstract double getWidth();

    /**
     * Returns the height of this {@code Dimension} in double
     * precision.
     * @return the height of this {@code Dimension}.
     * @since 1.2
     */
    public abstract double getHeight();

    /**
     * Sets the size of this {@code Dimension} object to the
     * specified width and height.
     * This method is included for completeness, to parallel the
     * {@link java.awt.Component#getSize getSize} method of
     * {@link java.awt.Component}.
     * @param width  the new width for the {@code Dimension}
     * object
     * @param height  the new height for the {@code Dimension}
     * object
     * @since 1.2
     */
    public abstract void setSize(double width, double height);

    /**
     * Sets the size of this {@code Dimension2D} object to
     * match the specified size.
     * This method is included for completeness, to parallel the
     * {@code getSize} method of {@code Component}.
     * @param d  the new size for the {@code Dimension2D}
     * object
     * @since 1.2
     */
    public void setSize(Dimension2D d) {
        setSize(d.getWidth(), d.getHeight());
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
