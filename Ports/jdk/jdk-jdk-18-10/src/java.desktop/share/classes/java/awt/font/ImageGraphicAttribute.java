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

import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/**
 * The {@code ImageGraphicAttribute} class is an implementation of
 * {@link GraphicAttribute} which draws images in
 * a {@link TextLayout}.
 * @see GraphicAttribute
 */

public final class ImageGraphicAttribute extends GraphicAttribute {

    private Image fImage;
    private float fImageWidth, fImageHeight;
    private float fOriginX, fOriginY;

    /**
     * Constructs an {@code ImageGraphicAttribute} from the specified
     * {@link Image}.  The origin is at (0,&nbsp;0).
     * @param image the {@code Image} rendered by this
     * {@code ImageGraphicAttribute}.
     * This object keeps a reference to {@code image}.
     * @param alignment one of the alignments from this
     * {@code ImageGraphicAttribute}
     */
    public ImageGraphicAttribute(Image image, int alignment) {

        this(image, alignment, 0, 0);
    }

    /**
     * Constructs an {@code ImageGraphicAttribute} from the specified
     * {@code Image}. The point
     * ({@code originX},&nbsp;{@code originY}) in the
     * {@code Image} appears at the origin of the
     * {@code ImageGraphicAttribute} within the text.
     * @param image the {@code Image} rendered by this
     * {@code ImageGraphicAttribute}.
     * This object keeps a reference to {@code image}.
     * @param alignment one of the alignments from this
     * {@code ImageGraphicAttribute}
     * @param originX the X coordinate of the point within
     * the {@code Image} that appears at the origin of the
     * {@code ImageGraphicAttribute} in the text line.
     * @param originY the Y coordinate of the point within
     * the {@code Image} that appears at the origin of the
     * {@code ImageGraphicAttribute} in the text line.
     */
    public ImageGraphicAttribute(Image image,
                                 int alignment,
                                 float originX,
                                 float originY) {

        super(alignment);

        // Can't clone image
        // fImage = (Image) image.clone();
        fImage = image;

        fImageWidth = image.getWidth(null);
        fImageHeight = image.getHeight(null);

        // ensure origin is in Image?
        fOriginX = originX;
        fOriginY = originY;
    }

    /**
     * Returns the ascent of this {@code ImageGraphicAttribute}.  The
     * ascent of an {@code ImageGraphicAttribute} is the distance
     * from the top of the image to the origin.
     * @return the ascent of this {@code ImageGraphicAttribute}.
     */
    public float getAscent() {

        return Math.max(0, fOriginY);
    }

    /**
     * Returns the descent of this {@code ImageGraphicAttribute}.
     * The descent of an {@code ImageGraphicAttribute} is the
     * distance from the origin to the bottom of the image.
     * @return the descent of this {@code ImageGraphicAttribute}.
     */
    public float getDescent() {

        return Math.max(0, fImageHeight-fOriginY);
    }

    /**
     * Returns the advance of this {@code ImageGraphicAttribute}.
     * The advance of an {@code ImageGraphicAttribute} is the
     * distance from the origin to the right edge of the image.
     * @return the advance of this {@code ImageGraphicAttribute}.
     */
    public float getAdvance() {

        return Math.max(0, fImageWidth-fOriginX);
    }

    /**
     * Returns a {@link Rectangle2D} that encloses all of the
     * bits rendered by this {@code ImageGraphicAttribute}, relative
     * to the rendering position.  A graphic can be rendered beyond its
     * origin, ascent, descent, or advance;  but if it is, this
     * method's implementation must indicate where the graphic is rendered.
     * @return a {@code Rectangle2D} that encloses all of the bits
     * rendered by this {@code ImageGraphicAttribute}.
     */
    public Rectangle2D getBounds() {

        return new Rectangle2D.Float(
                        -fOriginX, -fOriginY, fImageWidth, fImageHeight);
    }

    /**
     * {@inheritDoc}
     */
    public void draw(Graphics2D graphics, float x, float y) {

        graphics.drawImage(fImage, (int) (x-fOriginX), (int) (y-fOriginY), null);
    }

    /**
     * Returns a hashcode for this {@code ImageGraphicAttribute}.
     * @return  a hash code value for this object.
     */
    public int hashCode() {

        return fImage.hashCode();
    }

    /**
     * Compares this {@code ImageGraphicAttribute} to the specified
     * {@link Object}.
     * @param rhs the {@code Object} to compare for equality
     * @return {@code true} if this
     * {@code ImageGraphicAttribute} equals {@code rhs};
     * {@code false} otherwise.
     */
    public boolean equals(Object rhs) {

        try {
            return equals((ImageGraphicAttribute) rhs);
        }
        catch(ClassCastException e) {
            return false;
        }
    }

    /**
     * Compares this {@code ImageGraphicAttribute} to the specified
     * {@code ImageGraphicAttribute}.
     * @param rhs the {@code ImageGraphicAttribute} to compare for
     * equality
     * @return {@code true} if this
     * {@code ImageGraphicAttribute} equals {@code rhs};
     * {@code false} otherwise.
     */
    public boolean equals(ImageGraphicAttribute rhs) {

        if (rhs == null) {
            return false;
        }

        if (this == rhs) {
            return true;
        }

        if (fOriginX != rhs.fOriginX || fOriginY != rhs.fOriginY) {
            return false;
        }

        if (getAlignment() != rhs.getAlignment()) {
            return false;
        }

        if (!fImage.equals(rhs.fImage)) {
            return false;
        }

        return true;
    }
}
