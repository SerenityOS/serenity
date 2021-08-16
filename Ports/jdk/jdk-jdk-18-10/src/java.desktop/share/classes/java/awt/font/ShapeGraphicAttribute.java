/*
 * Copyright (c) 1998, 2006, Oracle and/or its affiliates. All rights reserved.
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

/*
 * (C) Copyright Taligent, Inc. 1996 - 1997, All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998, All Rights Reserved
 *
 * The original version of this source code and documentation is
 * copyrighted and owned by Taligent, Inc., a wholly-owned subsidiary
 * of IBM. These materials are provided under terms of a License
 * Agreement between Taligent and Sun. This technology is protected
 * by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.awt.font;

import java.awt.Shape;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * The {@code ShapeGraphicAttribute} class is an implementation of
 * {@link GraphicAttribute} that draws shapes in a {@link TextLayout}.
 * @see GraphicAttribute
 */
public final class ShapeGraphicAttribute extends GraphicAttribute {

    private Shape fShape;
    private boolean fStroke;

    /**
     * A key indicating the shape should be stroked with a 1-pixel wide stroke.
     */
    public static final boolean STROKE = true;

    /**
     * A key indicating the shape should be filled.
     */
    public static final boolean FILL = false;

    // cache shape bounds, since GeneralPath doesn't
    private Rectangle2D fShapeBounds;

    /**
     * Constructs a {@code ShapeGraphicAttribute} for the specified
     * {@link Shape}.
     * @param shape the {@code Shape} to render.  The
     * {@code Shape} is rendered with its origin at the origin of
     * this {@code ShapeGraphicAttribute} in the
     * host {@code TextLayout}.  This object maintains a reference to
     * {@code shape}.
     * @param alignment one of the alignments from this
     * {@code ShapeGraphicAttribute}.
     * @param stroke {@code true} if the {@code Shape} should be
     * stroked; {@code false} if the {@code Shape} should be
     * filled.
     */
    public ShapeGraphicAttribute(Shape shape,
                                 int alignment,
                                 boolean stroke) {

        super(alignment);

        fShape = shape;
        fStroke = stroke;
        fShapeBounds = fShape.getBounds2D();
    }

    /**
     * Returns the ascent of this {@code ShapeGraphicAttribute}.  The
     * ascent of a {@code ShapeGraphicAttribute} is the positive
     * distance from the origin of its {@code Shape} to the top of
     * bounds of its {@code Shape}.
     * @return the ascent of this {@code ShapeGraphicAttribute}.
     */
    public float getAscent() {

        return (float) Math.max(0, -fShapeBounds.getMinY());
    }

    /**
     * Returns the descent of this {@code ShapeGraphicAttribute}.
     * The descent of a {@code ShapeGraphicAttribute} is the distance
     * from the origin of its {@code Shape} to the bottom of the
     * bounds of its {@code Shape}.
     * @return the descent of this {@code ShapeGraphicAttribute}.
     */
    public float getDescent() {

        return (float) Math.max(0, fShapeBounds.getMaxY());
    }

    /**
     * Returns the advance of this {@code ShapeGraphicAttribute}.
     * The advance of a {@code ShapeGraphicAttribute} is the distance
     * from the origin of its {@code Shape} to the right side of the
     * bounds of its {@code Shape}.
     * @return the advance of this {@code ShapeGraphicAttribute}.
     */
    public float getAdvance() {

        return (float) Math.max(0, fShapeBounds.getMaxX());
    }

    /**
     * {@inheritDoc}
     */
    public void draw(Graphics2D graphics, float x, float y) {

        // translating graphics to draw Shape !!!
        graphics.translate((int)x, (int)y);

        try {
            if (fStroke == STROKE) {
                // REMIND: set stroke to correct size
                graphics.draw(fShape);
            }
            else {
                graphics.fill(fShape);
            }
        }
        finally {
            graphics.translate(-(int)x, -(int)y);
        }
    }

    /**
     * Returns a {@link Rectangle2D} that encloses all of the
     * bits drawn by this {@code ShapeGraphicAttribute} relative to
     * the rendering position.  A graphic can be rendered beyond its
     * origin, ascent, descent, or advance;  but if it does, this method's
     * implementation should indicate where the graphic is rendered.
     * @return a {@code Rectangle2D} that encloses all of the bits
     * rendered by this {@code ShapeGraphicAttribute}.
     */
    public Rectangle2D getBounds() {

        Rectangle2D.Float bounds = new Rectangle2D.Float();
        bounds.setRect(fShapeBounds);

        if (fStroke == STROKE) {
            ++bounds.width;
            ++bounds.height;
        }

        return bounds;
    }

    /**
     * Return a {@link java.awt.Shape} that represents the region that
     * this {@code ShapeGraphicAttribute} renders.  This is used when a
     * {@link TextLayout} is requested to return the outline of the text.
     * The (untransformed) shape must not extend outside the rectangular
     * bounds returned by {@code getBounds}.
     * @param tx an optional {@link AffineTransform} to apply to the
     *   this {@code ShapeGraphicAttribute}. This can be null.
     * @return the {@code Shape} representing this graphic attribute,
     *   suitable for stroking or filling.
     * @since 1.6
     */
    public Shape getOutline(AffineTransform tx) {
        return tx == null ? fShape : tx.createTransformedShape(fShape);
    }

    /**
     * Returns a hashcode for this {@code ShapeGraphicAttribute}.
     * @return  a hash code value for this
     * {@code ShapeGraphicAttribute}.
     */
    public int hashCode() {

        return fShape.hashCode();
    }

    /**
     * Compares this {@code ShapeGraphicAttribute} to the specified
     * {@code Object}.
     * @param rhs the {@code Object} to compare for equality
     * @return {@code true} if this
     * {@code ShapeGraphicAttribute} equals {@code rhs};
     * {@code false} otherwise.
     */
    public boolean equals(Object rhs) {

        try {
            return equals((ShapeGraphicAttribute) rhs);
        }
        catch(ClassCastException e) {
            return false;
        }
    }

    /**
     * Compares this {@code ShapeGraphicAttribute} to the specified
     * {@code ShapeGraphicAttribute}.
     * @param rhs the {@code ShapeGraphicAttribute} to compare for
     * equality
     * @return {@code true} if this
     * {@code ShapeGraphicAttribute} equals {@code rhs};
     * {@code false} otherwise.
     */
    public boolean equals(ShapeGraphicAttribute rhs) {

        if (rhs == null) {
            return false;
        }

        if (this == rhs) {
            return true;
        }

        if (fStroke != rhs.fStroke) {
            return false;
        }

        if (getAlignment() != rhs.getAlignment()) {
            return false;
        }

        if (!fShape.equals(rhs.fShape)) {
            return false;
        }

        return true;
    }
}
