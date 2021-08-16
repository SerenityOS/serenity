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
package java.awt.font;

import java.awt.Font;

/**
 * The {@code MultipleMaster} interface represents Type 1
 * Multiple Master fonts.
 * A particular {@link Font} object can implement this interface.
 */
public interface MultipleMaster {

  /**
   * Returns the number of multiple master design controls.
   * Design axes include things like width, weight and optical scaling.
   * @return the number of multiple master design controls
   */
  public  int getNumDesignAxes();

  /**
   * Returns an array of design limits interleaved in the form [from&rarr;to]
   * for each axis.  For example,
   * design limits for weight could be from 0.1 to 1.0. The values are
   * returned in the same order returned by
   * {@code getDesignAxisNames}.
   * @return an array of design limits for each axis.
   */
  public  float[]  getDesignAxisRanges();

  /**
   * Returns an array of default design values for each axis.  For example,
   * the default value for weight could be 1.6. The values are returned
   * in the same order returned by {@code getDesignAxisNames}.
   * @return an array of default design values for each axis.
   */
  public  float[]  getDesignAxisDefaults();

  /**
   * Returns the name for each design axis. This also determines the order in
   * which the values for each axis are returned.
   * @return an array containing the names of each design axis.
   */
  public  String[] getDesignAxisNames();

  /**
   * Creates a new instance of a multiple master font based on the design
   * axis values contained in the specified array. The size of the array
   * must correspond to the value returned from
   * {@code getNumDesignAxes} and the values of the array elements
   * must fall within limits specified by
   * {@code getDesignAxesLimits}. In case of an error,
   * {@code null} is returned.
   * @param axes an array containing axis values
   * @return a {@link Font} object that is an instance of
   * {@code MultipleMaster} and is based on the design axis values
   * provided by {@code axes}.
   */
  public Font deriveMMFont(float[] axes);

  /**
   * Creates a new instance of a multiple master font based on detailed metric
   * information. In case of an error, {@code null} is returned.
   * @param glyphWidths an array of floats representing the desired width
   * of each glyph in font space
   * @param avgStemWidth the average stem width for the overall font in
   * font space
   * @param typicalCapHeight the height of a typical upper case char
   * @param typicalXHeight the height of a typical lower case char
   * @param italicAngle the angle at which the italics lean, in degrees
   * counterclockwise from vertical
   * @return a {@code Font} object that is an instance of
   * {@code MultipleMaster} and is based on the specified metric
   * information.
   */
  public Font deriveMMFont(
                                   float[] glyphWidths,
                                   float avgStemWidth,
                                   float typicalCapHeight,
                                   float typicalXHeight,
                                   float italicAngle);


}
