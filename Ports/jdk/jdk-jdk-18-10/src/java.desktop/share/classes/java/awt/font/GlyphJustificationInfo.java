/*
 * Copyright (c) 1997, 1999, Oracle and/or its affiliates. All rights reserved.
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
 */

package java.awt.font;

/**
 * The {@code GlyphJustificationInfo} class represents information
 * about the justification properties of a glyph.  A glyph is the visual
 * representation of one or more characters.  Many different glyphs can
 * be used to represent a single character or combination of characters.
 * The four justification properties represented by
 * {@code GlyphJustificationInfo} are weight, priority, absorb and
 * limit.
 * <p>
 * Weight is the overall 'weight' of the glyph in the line.  Generally it is
 * proportional to the size of the font.  Glyphs with larger weight are
 * allocated a correspondingly larger amount of the change in space.
 * <p>
 * Priority determines the justification phase in which this glyph is used.
 * All glyphs of the same priority are examined before glyphs of the next
 * priority.  If all the change in space can be allocated to these glyphs
 * without exceeding their limits, then glyphs of the next priority are not
 * examined. There are four priorities, kashida, whitespace, interchar,
 * and none.  KASHIDA is the first priority examined. NONE is the last
 * priority examined.
 * <p>
 * Absorb determines whether a glyph absorbs all change in space.  Within a
 * given priority, some glyphs may absorb all the change in space.  If any of
 * these glyphs are present, no glyphs of later priority are examined.
 * <p>
 * Limit determines the maximum or minimum amount by which the glyph can
 * change. Left and right sides of the glyph can have different limits.
 * <p>
 * Each {@code GlyphJustificationInfo} represents two sets of
 * metrics, which are <i>growing</i> and <i>shrinking</i>.  Growing
 * metrics are used when the glyphs on a line are to be
 * spread apart to fit a larger width.  Shrinking metrics are used when
 * the glyphs are to be moved together to fit a smaller width.
 */

public final class GlyphJustificationInfo {

    /**
     * Constructs information about the justification properties of a
     * glyph.
     * @param weight the weight of this glyph when allocating space.  Must be non-negative.
     * @param growAbsorb if {@code true} this glyph absorbs
     * all extra space at this priority and lower priority levels when it
     * grows
     * @param growPriority the priority level of this glyph when it
     * grows
     * @param growLeftLimit the maximum amount by which the left side of this
     * glyph can grow.  Must be non-negative.
     * @param growRightLimit the maximum amount by which the right side of this
     * glyph can grow.  Must be non-negative.
     * @param shrinkAbsorb if {@code true}, this glyph absorbs all
     * remaining shrinkage at this and lower priority levels when it
     * shrinks
     * @param shrinkPriority the priority level of this glyph when
     * it shrinks
     * @param shrinkLeftLimit the maximum amount by which the left side of this
     * glyph can shrink.  Must be non-negative.
     * @param shrinkRightLimit the maximum amount by which the right side
     * of this glyph can shrink.  Must be non-negative.
     */
     public GlyphJustificationInfo(float weight,
                                  boolean growAbsorb,
                                  int growPriority,
                                  float growLeftLimit,
                                  float growRightLimit,
                                  boolean shrinkAbsorb,
                                  int shrinkPriority,
                                  float shrinkLeftLimit,
                                  float shrinkRightLimit)
    {
        if (weight < 0) {
            throw new IllegalArgumentException("weight is negative");
        }

        if (!priorityIsValid(growPriority)) {
            throw new IllegalArgumentException("Invalid grow priority");
        }
        if (growLeftLimit < 0) {
            throw new IllegalArgumentException("growLeftLimit is negative");
        }
        if (growRightLimit < 0) {
            throw new IllegalArgumentException("growRightLimit is negative");
        }

        if (!priorityIsValid(shrinkPriority)) {
            throw new IllegalArgumentException("Invalid shrink priority");
        }
        if (shrinkLeftLimit < 0) {
            throw new IllegalArgumentException("shrinkLeftLimit is negative");
        }
        if (shrinkRightLimit < 0) {
            throw new IllegalArgumentException("shrinkRightLimit is negative");
        }

        this.weight = weight;
        this.growAbsorb = growAbsorb;
        this.growPriority = growPriority;
        this.growLeftLimit = growLeftLimit;
        this.growRightLimit = growRightLimit;
        this.shrinkAbsorb = shrinkAbsorb;
        this.shrinkPriority = shrinkPriority;
        this.shrinkLeftLimit = shrinkLeftLimit;
        this.shrinkRightLimit = shrinkRightLimit;
    }

    private static boolean priorityIsValid(int priority) {

        return priority >= PRIORITY_KASHIDA && priority <= PRIORITY_NONE;
    }

    /** The highest justification priority. */
    public static final int PRIORITY_KASHIDA = 0;

    /** The second highest justification priority. */
    public static final int PRIORITY_WHITESPACE = 1;

    /** The second lowest justification priority. */
    public static final int PRIORITY_INTERCHAR = 2;

    /** The lowest justification priority. */
    public static final int PRIORITY_NONE = 3;

    /**
     * The weight of this glyph.
     */
    public final float weight;

    /**
     * The priority level of this glyph as it is growing.
     */
    public final int growPriority;

    /**
     * If {@code true}, this glyph absorbs all extra
     * space at this and lower priority levels when it grows.
     */
    public final boolean growAbsorb;

    /**
     * The maximum amount by which the left side of this glyph can grow.
     */
    public final float growLeftLimit;

    /**
     * The maximum amount by which the right side of this glyph can grow.
     */
    public final float growRightLimit;

    /**
     * The priority level of this glyph as it is shrinking.
     */
    public final int shrinkPriority;

    /**
     * If {@code true},this glyph absorbs all remaining shrinkage at
     * this and lower priority levels as it shrinks.
     */
    public final boolean shrinkAbsorb;

    /**
     * The maximum amount by which the left side of this glyph can shrink
     * (a positive number).
     */
    public final float shrinkLeftLimit;

    /**
     * The maximum amount by which the right side of this glyph can shrink
     * (a positive number).
     */
    public final float shrinkRightLimit;
}
