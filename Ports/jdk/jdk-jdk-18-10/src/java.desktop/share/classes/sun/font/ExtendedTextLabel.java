/*
 * Copyright (c) 1998, 2003, Oracle and/or its affiliates. All rights reserved.
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
 *
 * (C) Copyright IBM Corp. 1998-2003- All Rights Reserved.
 */

package sun.font;

import java.awt.Font;

import java.awt.font.GlyphJustificationInfo;
import java.awt.font.LineMetrics;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * An extension of TextLabel that maintains information
 * about characters.
 */

public abstract class ExtendedTextLabel extends TextLabel
                            implements TextLineComponent{
  /**
   * Return the number of characters represented by this label.
   */
  public abstract int getNumCharacters();

  /**
   * Return the line metrics for all text in this label.
   */
  public abstract CoreMetrics getCoreMetrics();

  /**
   * Return the x location of the character at the given logical index.
   */
  public abstract float getCharX(int logicalIndex);

  /**
   * Return the y location of the character at the given logical index.
   */
  public abstract float getCharY(int logicalIndex);

  /**
   * Return the advance of the character at the given logical index.
   */
  public abstract float getCharAdvance(int logicalIndex);

  /**
   * Return the visual bounds of the character at the given logical index.
   * This bounds encloses all the pixels of the character when the label is rendered
   * at x, y.
   */
  public abstract Rectangle2D getCharVisualBounds(int logicalIndex, float x, float y);

  /**
   * Return the visual index of the character at the given logical index.
   */
  public abstract int logicalToVisual(int logicalIndex);

  /**
   * Return the logical index of the character at the given visual index.
   */
  public abstract int visualToLogical(int visualIndex);

  /**
   * Return the logical index of the character, starting with the character at
   * logicalStart, whose accumulated advance exceeds width.  If the advances of
   * all characters do not exceed width, return getNumCharacters.  If width is
   * less than zero, return logicalStart - 1.
   */
  public abstract int getLineBreakIndex(int logicalStart, float width);

  /**
   * Return the accumulated advances of all characters between logicalStart and
   * logicalLimit.
   */
  public abstract float getAdvanceBetween(int logicalStart, int logicalLimit);

  /**
   * Return whether a caret can exist on the leading edge of the
   * character at offset.  If the character is part of a ligature
   * (for example) a caret may not be appropriate at offset.
   */
  public abstract boolean caretAtOffsetIsValid(int offset);

  /**
   * A convenience overload of getCharVisualBounds that defaults the label origin
   * to 0, 0.
   */
  public Rectangle2D getCharVisualBounds(int logicalIndex) {
    return getCharVisualBounds(logicalIndex, 0, 0);
  }

  public abstract TextLineComponent getSubset(int start, int limit, int dir);

  /**
   * Return the number of justification records this uses.
   */
  public abstract int getNumJustificationInfos();

  /**
   * Return GlyphJustificationInfo objects for the characters between
   * charStart and charLimit, starting at offset infoStart.  Infos
   * will be in visual order.  All positions between infoStart and
   * getNumJustificationInfos will be set.  If a position corresponds
   * to a character outside the provided range, it is set to null.
   */
  public abstract void getJustificationInfos(GlyphJustificationInfo[] infos, int infoStart, int charStart, int charLimit);

  /**
   * Apply deltas to the data in this component, starting at offset
   * deltaStart, and return the new component.  There are two floats
   * for each justification info, for a total of 2 * getNumJustificationInfos.
   * The first delta is the left adjustment, the second is the right
   * adjustment.
   * <p>
   * If flags[0] is true on entry, rejustification is allowed.  If
   * the new component requires rejustification (ligatures were
   * formed or split), flags[0] will be set on exit.
   */
  public abstract TextLineComponent applyJustificationDeltas(float[] deltas, int deltaStart, boolean[] flags);
}
