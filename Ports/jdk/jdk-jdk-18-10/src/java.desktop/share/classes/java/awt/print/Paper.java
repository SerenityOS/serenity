/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.print;

import java.awt.geom.Rectangle2D;

/**
 * The {@code Paper} class describes the physical characteristics of
 * a piece of paper.
 * <p>
 * When creating a {@code Paper} object, it is the application's
 * responsibility to ensure that the paper size and the imageable area
 * are compatible.  For example, if the paper size is changed from
 * 11 x 17 to 8.5 x 11, the application might need to reduce the
 * imageable area so that whatever is printed fits on the page.
 * @see #setSize(double, double)
 * @see #setImageableArea(double, double, double, double)
 */
public class Paper implements Cloneable {

 /* Private Class Variables */

    private static final int INCH = 72;
    private static final double LETTER_WIDTH = 8.5 * INCH;
    private static final double LETTER_HEIGHT = 11 * INCH;

 /* Instance Variables */

    /**
     * The height of the physical page in 1/72nds
     * of an inch. The number is stored as a floating
     * point value rather than as an integer
     * to facilitate the conversion from metric
     * units to 1/72nds of an inch and then back.
     * (This may or may not be a good enough reason
     * for a float).
     */
    private double mHeight;

    /**
     * The width of the physical page in 1/72nds
     * of an inch.
     */
    private double mWidth;

    /**
     * The area of the page on which drawing will
     * be visible. The area outside of this
     * rectangle but on the Page generally
     * reflects the printer's hardware margins.
     * The origin of the physical page is
     * at (0, 0) with this rectangle provided
     * in that coordinate system.
     */
    private Rectangle2D mImageableArea;

 /* Constructors */

    /**
     * Creates a letter sized piece of paper
     * with one inch margins.
     */
    public Paper() {
        mHeight = LETTER_HEIGHT;
        mWidth = LETTER_WIDTH;
        mImageableArea = new Rectangle2D.Double(INCH, INCH,
                                                mWidth - 2 * INCH,
                                                mHeight - 2 * INCH);
    }

 /* Instance Methods */

    /**
     * Creates a copy of this {@code Paper} with the same contents
     * as this {@code Paper}.
     * @return a copy of this {@code Paper}.
     */
    public Object clone() {

        Paper newPaper;

        try {
            /* It's okay to copy the reference to the imageable
             * area into the clone since we always return a copy
             * of the imageable area when asked for it.
             */
            newPaper = (Paper) super.clone();

        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            newPaper = null;    // should never happen.
        }

        return newPaper;
    }

    /**
     * Returns the height of the page in 1/72nds of an inch.
     * @return the height of the page described by this
     *          {@code Paper}.
     */
    public double getHeight() {
        return mHeight;
    }

    /**
     * Sets the width and height of this {@code Paper}
     * object, which represents the properties of the page onto
     * which printing occurs.
     * The dimensions are supplied in 1/72nds of
     * an inch.
     * @param width the value to which to set this {@code Paper}
     * object's width
     * @param height the value to which to set this {@code Paper}
     * object's height
     */
    public void setSize(double width, double height) {
        mWidth = width;
        mHeight = height;
    }

    /**
     * Returns the width of the page in 1/72nds
     * of an inch.
     * @return the width of the page described by this
     * {@code Paper}.
     */
    public double getWidth() {
        return mWidth;
    }

    /**
     * Sets the imageable area of this {@code Paper}.  The
     * imageable area is the area on the page in which printing
     * occurs.
     * @param x the X coordinate to which to set the
     * upper-left corner of the imageable area of this {@code Paper}
     * @param y the Y coordinate to which to set the
     * upper-left corner of the imageable area of this {@code Paper}
     * @param width the value to which to set the width of the
     * imageable area of this {@code Paper}
     * @param height the value to which to set the height of the
     * imageable area of this {@code Paper}
     */
    public void setImageableArea(double x, double y,
                                 double width, double height) {
        mImageableArea = new Rectangle2D.Double(x, y, width,height);
    }

    /**
     * Returns the x coordinate of the upper-left corner of this
     * {@code Paper} object's imageable area.
     * @return the x coordinate of the imageable area.
     */
    public double getImageableX() {
        return mImageableArea.getX();
    }

    /**
     * Returns the y coordinate of the upper-left corner of this
     * {@code Paper} object's imageable area.
     * @return the y coordinate of the imageable area.
     */
    public double getImageableY() {
        return mImageableArea.getY();
    }

    /**
     * Returns the width of this {@code Paper} object's imageable
     * area.
     * @return the width of the imageable area.
     */
    public double getImageableWidth() {
        return mImageableArea.getWidth();
    }

    /**
     * Returns the height of this {@code Paper} object's imageable
     * area.
     * @return the height of the imageable area.
     */
    public double getImageableHeight() {
        return mImageableArea.getHeight();
    }
}
