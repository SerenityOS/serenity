/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;

/**
 * The {@code PaintContext} interface defines the encapsulated
 * and optimized environment to generate color patterns in device
 * space for fill or stroke operations on a
 * {@link Graphics2D}.  The {@code PaintContext} provides
 * the necessary colors for {@code Graphics2D} operations in the
 * form of a {@link Raster} associated with a {@link ColorModel}.
 * The {@code PaintContext} maintains state for a particular paint
 * operation.  In a multi-threaded environment, several
 * contexts can exist simultaneously for a single {@link Paint} object.
 * @see Paint
 */

public interface PaintContext {
    /**
     * Releases the resources allocated for the operation.
     */
    public void dispose();

    /**
     * Returns the {@code ColorModel} of the output.  Note that
     * this {@code ColorModel} might be different from the hint
     * specified in the
     * {@link Paint#createContext(ColorModel, Rectangle, Rectangle2D,
AffineTransform, RenderingHints) createContext} method of
     * {@code Paint}.  Not all {@code PaintContext} objects are
     * capable of generating color patterns in an arbitrary
     * {@code ColorModel}.
     * @return the {@code ColorModel} of the output.
     */
    ColorModel getColorModel();

    /**
     * Returns a {@code Raster} containing the colors generated for
     * the graphics operation.
     * @param x the x coordinate of the area in device space
     * for which colors are generated.
     * @param y the y coordinate of the area in device space
     * for which colors are generated.
     * @param w the width of the area in device space
     * @param h the height of the area in device space
     * @return a {@code Raster} representing the specified
     * rectangular area and containing the colors generated for
     * the graphics operation.
     */
    Raster getRaster(int x,
                     int y,
                     int w,
                     int h);

}
