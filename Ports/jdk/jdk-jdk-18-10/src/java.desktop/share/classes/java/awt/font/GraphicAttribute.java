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

import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * This class is used with the CHAR_REPLACEMENT attribute.
 * <p>
 * The {@code GraphicAttribute} class represents a graphic embedded
 * in text. Clients subclass this class to implement their own char
 * replacement graphics.  Clients wishing to embed shapes and images in
 * text need not subclass this class.  Instead, clients can use the
 * {@link ShapeGraphicAttribute} and {@link ImageGraphicAttribute}
 * classes.
 * <p>
 * Subclasses must ensure that their objects are immutable once they
 * are constructed.  Mutating a {@code GraphicAttribute} that
 * is used in a {@link TextLayout} results in undefined behavior from the
 * {@code TextLayout}.
 */
public abstract class GraphicAttribute {

    private int fAlignment;

    /**
     * Aligns top of graphic to top of line.
     */
    public static final int TOP_ALIGNMENT = -1;

    /**
     * Aligns bottom of graphic to bottom of line.
     */
    public static final int BOTTOM_ALIGNMENT = -2;

    /**
     * Aligns origin of graphic to roman baseline of line.
     */
    public static final int ROMAN_BASELINE = Font.ROMAN_BASELINE;

    /**
     * Aligns origin of graphic to center baseline of line.
     */
    public static final int CENTER_BASELINE = Font.CENTER_BASELINE;

    /**
     * Aligns origin of graphic to hanging baseline of line.
     */
    public static final int HANGING_BASELINE = Font.HANGING_BASELINE;

    /**
     * Constructs a {@code GraphicAttribute}.
     * Subclasses use this to define the alignment of the graphic.
     * @param alignment an int representing one of the
     * {@code GraphicAttribute} alignment fields
     * @throws IllegalArgumentException if alignment is not one of the
     * five defined values.
     */
    protected GraphicAttribute(int alignment) {
        if (alignment < BOTTOM_ALIGNMENT || alignment > HANGING_BASELINE) {
          throw new IllegalArgumentException("bad alignment");
        }
        fAlignment = alignment;
    }

    /**
     * Returns the ascent of this {@code GraphicAttribute}.  A
     * graphic can be rendered above its ascent.
     * @return the ascent of this {@code GraphicAttribute}.
     * @see #getBounds()
     */
    public abstract float getAscent();


    /**
     * Returns the descent of this {@code GraphicAttribute}.  A
     * graphic can be rendered below its descent.
     * @return the descent of this {@code GraphicAttribute}.
     * @see #getBounds()
     */
    public abstract float getDescent();

    /**
     * Returns the advance of this {@code GraphicAttribute}.  The
     * {@code GraphicAttribute} object's advance is the distance
     * from the point at which the graphic is rendered and the point where
     * the next character or graphic is rendered.  A graphic can be
     * rendered beyond its advance
     * @return the advance of this {@code GraphicAttribute}.
     * @see #getBounds()
     */
    public abstract float getAdvance();

    /**
     * Returns a {@link Rectangle2D} that encloses all of the
     * bits drawn by this {@code GraphicAttribute} relative to the
     * rendering position.
     * A graphic may be rendered beyond its origin, ascent, descent,
     * or advance;  but if it is, this method's implementation must
     * indicate where the graphic is rendered.
     * Default bounds is the rectangle (0, -ascent, advance, ascent+descent).
     * @return a {@code Rectangle2D} that encloses all of the bits
     * rendered by this {@code GraphicAttribute}.
     */
    public Rectangle2D getBounds() {
        float ascent = getAscent();
        return new Rectangle2D.Float(0, -ascent,
                                        getAdvance(), ascent+getDescent());
    }

    /**
     * Return a {@link java.awt.Shape} that represents the region that
     * this {@code GraphicAttribute} renders.  This is used when a
     * {@link TextLayout} is requested to return the outline of the text.
     * The (untransformed) shape must not extend outside the rectangular
     * bounds returned by {@code getBounds}.
     * The default implementation returns the rectangle returned by
     * {@link #getBounds}, transformed by the provided {@link AffineTransform}
     * if present.
     * @param tx an optional {@link AffineTransform} to apply to the
     *   outline of this {@code GraphicAttribute}. This can be null.
     * @return a {@code Shape} representing this graphic attribute,
     *   suitable for stroking or filling.
     * @since 1.6
     */
    public Shape getOutline(AffineTransform tx) {
        Shape b = getBounds();
        if (tx != null) {
            b = tx.createTransformedShape(b);
        }
        return b;
    }

    /**
     * Renders this {@code GraphicAttribute} at the specified
     * location.
     * @param graphics the {@link Graphics2D} into which to render the
     * graphic
     * @param x the user-space X coordinate where the graphic is rendered
     * @param y the user-space Y coordinate where the graphic is rendered
     */
    public abstract void draw(Graphics2D graphics, float x, float y);

    /**
     * Returns the alignment of this {@code GraphicAttribute}.
     * Alignment can be to a particular baseline, or to the absolute top
     * or bottom of a line.
     * @return the alignment of this {@code GraphicAttribute}.
     */
    public final int getAlignment() {

        return fAlignment;
    }

    /**
     * Returns the justification information for this
     * {@code GraphicAttribute}.  Subclasses
     * can override this method to provide different justification
     * information.
     * @return a {@link GlyphJustificationInfo} object that contains the
     * justification information for this {@code GraphicAttribute}.
     */
    public GlyphJustificationInfo getJustificationInfo() {

        // should we cache this?
        float advance = getAdvance();

        return new GlyphJustificationInfo(
                                     advance,   // weight
                                     false,     // growAbsorb
                                     2,         // growPriority
                                     advance/3, // growLeftLimit
                                     advance/3, // growRightLimit
                                     false,     // shrinkAbsorb
                                     1,         // shrinkPriority
                                     0,         // shrinkLeftLimit
                                     0);        // shrinkRightLimit
    }
}
