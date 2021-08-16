/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.io.Serial;

/**
 * The {@code GeneralPath} class represents a geometric path
 * constructed from straight lines, and quadratic and cubic
 * (B&eacute;zier) curves.  It can contain multiple subpaths.
 * <p>
 * {@code GeneralPath} is a legacy final class which exactly
 * implements the behavior of its superclass {@link Path2D.Float}.
 * Together with {@link Path2D.Double}, the {@link Path2D} classes
 * provide full implementations of a general geometric path that
 * support all of the functionality of the {@link Shape} and
 * {@link PathIterator} interfaces with the ability to explicitly
 * select different levels of internal coordinate precision.
 * <p>
 * Use {@code Path2D.Float} (or this legacy {@code GeneralPath}
 * subclass) when dealing with data that can be represented
 * and used with floating point precision.  Use {@code Path2D.Double}
 * for data that requires the accuracy or range of double precision.
 *
 * @author Jim Graham
 * @since 1.2
 */
public final class GeneralPath extends Path2D.Float {
    /**
     * Constructs a new empty single precision {@code GeneralPath} object
     * with a default winding rule of {@link #WIND_NON_ZERO}.
     *
     * @since 1.2
     */
    public GeneralPath() {
        super(WIND_NON_ZERO, INIT_SIZE);
    }

    /**
     * Constructs a new {@code GeneralPath} object with the specified
     * winding rule to control operations that require the interior of the
     * path to be defined.
     *
     * @param rule the winding rule
     * @throws IllegalArgumentException if {@code rule} is not either
     *         {@link #WIND_EVEN_ODD} or {@link #WIND_NON_ZERO}
     * @see #WIND_EVEN_ODD
     * @see #WIND_NON_ZERO
     * @since 1.2
     */
    public GeneralPath(int rule) {
        super(rule, INIT_SIZE);
    }

    /**
     * Constructs a new {@code GeneralPath} object with the specified
     * winding rule and the specified initial capacity to store path
     * coordinates.
     * This number is an initial guess as to how many path segments
     * will be added to the path, but the storage is expanded as
     * needed to store whatever path segments are added.
     *
     * @param rule the winding rule
     * @param initialCapacity the estimate for the number of path segments
     *                        in the path
     * @throws IllegalArgumentException if {@code rule} is not either
     *         {@link #WIND_EVEN_ODD} or {@link #WIND_NON_ZERO}
     * @throws NegativeArraySizeException if {@code initialCapacity} is
     *         negative
     * @see #WIND_EVEN_ODD
     * @see #WIND_NON_ZERO
     * @since 1.2
     */
    public GeneralPath(int rule, int initialCapacity) {
        super(rule, initialCapacity);
    }

    /**
     * Constructs a new {@code GeneralPath} object from an arbitrary
     * {@link Shape} object.
     * All of the initial geometry and the winding rule for this path are
     * taken from the specified {@code Shape} object.
     *
     * @param s the specified {@code Shape} object
     * @throws NullPointerException if {@code s} is {@code null}
     * @since 1.2
     */
    public GeneralPath(Shape s) {
        super(s, null);
    }

    GeneralPath(int windingRule,
                byte[] pointTypes,
                int numTypes,
                float[] pointCoords,
                int numCoords)
    {
        // used to construct from native

        this.windingRule = windingRule;
        this.pointTypes = pointTypes;
        this.numTypes = numTypes;
        this.floatCoords = pointCoords;
        this.numCoords = numCoords;
    }

    /**
     * Use serialVersionUID from JDK 1.6 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -8327096662768731142L;
}
