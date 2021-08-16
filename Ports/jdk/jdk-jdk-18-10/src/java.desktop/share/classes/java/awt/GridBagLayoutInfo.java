/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Serial;

/**
 * The {@code GridBagLayoutInfo} is an utility class for
 * {@code GridBagLayout} layout manager.
 * It stores align, size and baseline parameters for every component within a container.
 *
 * @see       java.awt.GridBagLayout
 * @see       java.awt.GridBagConstraints
 * @since 1.6
 */
public class GridBagLayoutInfo implements java.io.Serializable {

    /**
     * Use serialVersionUID from JDK 1.6 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -4899416460737170217L;

    /**
     * The number of cells: horizontal and vertical.
     */
    int width, height;

    /**
     * The starting point for layout.
     */
    int startx, starty;

    /**
     * The largest minWidth in each column.
     */
    int[] minWidth;

    /**
     * The largest minHeight in each row.
     */
    int[] minHeight;

    /**
     * The largest weight in each column.
     */
    double[] weightX;

    /**
     * The largest weight in each row.
     */
    double[] weightY;

    /**
     * Whether or not baseline layout has been requested and one of the
     * components has a valid baseline.
     */
    boolean hasBaseline;

    // These are only valid if hasBaseline is true and are indexed by
    // row.
    /**
     * The type of baseline for a particular row. A mix of the
     * BaselineResizeBehavior constants {@code (1 << ordinal())}
     */
    short[] baselineType;

    /**
     * Max ascent (baseline).
     */
    int[] maxAscent;

    /**
     * Max descent (height - baseline)
     */
    int[] maxDescent;

    /**
     * Creates an instance of GridBagLayoutInfo representing {@code GridBagLayout}
     * grid cells with it's own parameters.
     * @param width the columns
     * @param height the rows
     * @since 1.6
     */
    GridBagLayoutInfo(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Returns true if the specified row has any component aligned on the
     * baseline with a baseline resize behavior of CONSTANT_DESCENT.
     */
    boolean hasConstantDescent(int row) {
        return ((baselineType[row] & (1 << Component.BaselineResizeBehavior.
                                      CONSTANT_DESCENT.ordinal())) != 0);
    }

    /**
     * Returns true if there is a baseline for the specified row.
     */
    boolean hasBaseline(int row) {
        return (hasBaseline && baselineType[row] != 0);
    }
}
