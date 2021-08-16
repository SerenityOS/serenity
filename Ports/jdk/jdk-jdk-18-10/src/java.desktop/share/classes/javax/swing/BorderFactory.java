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
package javax.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import javax.swing.border.*;

/**
 * Factory class for vending standard <code>Border</code> objects.  Wherever
 * possible, this factory will hand out references to shared
 * <code>Border</code> instances.
 * For further information and examples see
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/border.html">How
 to Use Borders</a>,
 * a section in <em>The Java Tutorial</em>.
 *
 * @author David Kloba
 * @since 1.2
 */
public class BorderFactory
{

    /** Don't let anyone instantiate this class */
    private BorderFactory() {
    }


//// LineBorder ///////////////////////////////////////////////////////////////
    /**
     * Creates a line border with the specified color.
     *
     * @param color  a <code>Color</code> to use for the line
     * @return the <code>Border</code> object
     */
    public static Border createLineBorder(Color color) {
        return new LineBorder(color, 1);
    }

    /**
     * Creates a line border with the specified color
     * and width. The width applies to all four sides of the
     * border. To specify widths individually for the top,
     * bottom, left, and right, use
     * {@link #createMatteBorder(int,int,int,int,Color)}.
     *
     * @param color  a <code>Color</code> to use for the line
     * @param thickness  an integer specifying the width in pixels
     * @return the <code>Border</code> object
     */
    public static Border createLineBorder(Color color, int thickness)  {
        return new LineBorder(color, thickness);
    }

    /**
     * Creates a line border with the specified color, thickness, and corner shape.
     *
     * @param color      the color of the border
     * @param thickness  the thickness of the border
     * @param rounded    whether or not border corners should be round
     * @return the {@code Border} object
     *
     * @see LineBorder#LineBorder(Color, int, boolean)
     * @since 1.7
     */
    public static Border createLineBorder(Color color, int thickness, boolean rounded) {
        return new LineBorder(color, thickness, rounded);
    }

//// BevelBorder /////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
    static final Border sharedRaisedBevel = new BevelBorder(BevelBorder.RAISED);
    static final Border sharedLoweredBevel = new BevelBorder(BevelBorder.LOWERED);

    /**
     * Creates a border with a raised beveled edge, using
     * brighter shades of the component's current background color
     * for highlighting, and darker shading for shadows.
     * (In a raised border, highlights are on top and shadows
     *  are underneath.)
     *
     * @return the <code>Border</code> object
     */
    public static Border createRaisedBevelBorder() {
        return createSharedBevel(BevelBorder.RAISED);
    }

    /**
     * Creates a border with a lowered beveled edge, using
     * brighter shades of the component's current background color
     * for highlighting, and darker shading for shadows.
     * (In a lowered border, shadows are on top and highlights
     *  are underneath.)
     *
     * @return the <code>Border</code> object
     */
    public static Border createLoweredBevelBorder() {
        return createSharedBevel(BevelBorder.LOWERED);
    }

    /**
     * Creates a beveled border of the specified type, using
     * brighter shades of the component's current background color
     * for highlighting, and darker shading for shadows.
     * (In a lowered border, shadows are on top and highlights
     *  are underneath.)
     *
     * @param type  an integer specifying either
     *                  <code>BevelBorder.LOWERED</code> or
     *                  <code>BevelBorder.RAISED</code>
     * @return the <code>Border</code> object
     */
    public static Border createBevelBorder(int type) {
        return createSharedBevel(type);
    }

    /**
     * Creates a beveled border of the specified type, using
     * the specified highlighting and shadowing. The outer
     * edge of the highlighted area uses a brighter shade of
     * the highlight color. The inner edge of the shadow area
     * uses a brighter shade of the shadow color.
     *
     * @param type  an integer specifying either
     *                  <code>BevelBorder.LOWERED</code> or
     *                  <code>BevelBorder.RAISED</code>
     * @param highlight  a <code>Color</code> object for highlights
     * @param shadow     a <code>Color</code> object for shadows
     * @return the <code>Border</code> object
     */
    public static Border createBevelBorder(int type, Color highlight, Color shadow) {
        return new BevelBorder(type, highlight, shadow);
    }

    /**
     * Creates a beveled border of the specified type, using
     * the specified colors for the inner and outer highlight
     * and shadow areas.
     *
     * @param type  an integer specifying either
     *          <code>BevelBorder.LOWERED</code> or
     *          <code>BevelBorder.RAISED</code>
     * @param highlightOuter  a <code>Color</code> object for the
     *                  outer edge of the highlight area
     * @param highlightInner  a <code>Color</code> object for the
     *                  inner edge of the highlight area
     * @param shadowOuter     a <code>Color</code> object for the
     *                  outer edge of the shadow area
     * @param shadowInner     a <code>Color</code> object for the
     *                  inner edge of the shadow area
     * @return the <code>Border</code> object
     */
    public static Border createBevelBorder(int type,
                        Color highlightOuter, Color highlightInner,
                        Color shadowOuter, Color shadowInner) {
        return new BevelBorder(type, highlightOuter, highlightInner,
                                        shadowOuter, shadowInner);
    }

    static Border createSharedBevel(int type)   {
        if(type == BevelBorder.RAISED) {
            return sharedRaisedBevel;
        } else if(type == BevelBorder.LOWERED) {
            return sharedLoweredBevel;
        }
        return null;
    }

//// SoftBevelBorder ///////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

    private static Border sharedSoftRaisedBevel;
    private static Border sharedSoftLoweredBevel;

    /**
     * Creates a beveled border with a raised edge and softened corners,
     * using brighter shades of the component's current background color
     * for highlighting, and darker shading for shadows.
     * In a raised border, highlights are on top and shadows are underneath.
     *
     * @return the {@code Border} object
     *
     * @since 1.7
     */
    public static Border createRaisedSoftBevelBorder() {
        if (sharedSoftRaisedBevel == null) {
            sharedSoftRaisedBevel = new SoftBevelBorder(BevelBorder.RAISED);
        }
        return sharedSoftRaisedBevel;
    }

    /**
     * Creates a beveled border with a lowered edge and softened corners,
     * using brighter shades of the component's current background color
     * for highlighting, and darker shading for shadows.
     * In a lowered border, shadows are on top and highlights are underneath.
     *
     * @return the {@code Border} object
     *
     * @since 1.7
     */
    public static Border createLoweredSoftBevelBorder() {
        if (sharedSoftLoweredBevel == null) {
            sharedSoftLoweredBevel = new SoftBevelBorder(BevelBorder.LOWERED);
        }
        return sharedSoftLoweredBevel;
    }

    /**
     * Creates a beveled border of the specified type with softened corners,
     * using brighter shades of the component's current background color
     * for highlighting, and darker shading for shadows.
     * The type is either {@link BevelBorder#RAISED} or {@link BevelBorder#LOWERED}.
     *
     * @param type  a type of a bevel
     * @return the {@code Border} object or {@code null}
     *         if the specified type is not valid
     *
     * @see BevelBorder#BevelBorder(int)
     * @since 1.7
     */
    public static Border createSoftBevelBorder(int type) {
        if (type == BevelBorder.RAISED) {
            return createRaisedSoftBevelBorder();
        }
        if (type == BevelBorder.LOWERED) {
            return createLoweredSoftBevelBorder();
        }
        return null;
    }

    /**
     * Creates a beveled border of the specified type with softened corners,
     * using the specified highlighting and shadowing.
     * The type is either {@link BevelBorder#RAISED} or {@link BevelBorder#LOWERED}.
     * The outer edge of the highlight area uses
     * a brighter shade of the {@code highlight} color.
     * The inner edge of the shadow area uses
     * a brighter shade of the {@code shadow} color.
     *
     * @param type       a type of a bevel
     * @param highlight  a basic color of the highlight area
     * @param shadow     a basic color of the shadow area
     * @return the {@code Border} object
     *
     * @see BevelBorder#BevelBorder(int, Color, Color)
     * @since 1.7
     */
    public static Border createSoftBevelBorder(int type, Color highlight, Color shadow) {
        return new SoftBevelBorder(type, highlight, shadow);
    }

    /**
     * Creates a beveled border of the specified type with softened corners,
     * using the specified colors for the inner and outer edges
     * of the highlight and the shadow areas.
     * The type is either {@link BevelBorder#RAISED} or {@link BevelBorder#LOWERED}.
     * Note: The shadow inner and outer colors are switched
     * for a lowered bevel border.
     *
     * @param type            a type of a bevel
     * @param highlightOuter  a color of the outer edge of the highlight area
     * @param highlightInner  a color of the inner edge of the highlight area
     * @param shadowOuter     a color of the outer edge of the shadow area
     * @param shadowInner     a color of the inner edge of the shadow area
     * @return the {@code Border} object
     *
     * @see BevelBorder#BevelBorder(int, Color, Color, Color, Color)
     * @since 1.7
     */
    public static Border createSoftBevelBorder(int type, Color highlightOuter, Color highlightInner, Color shadowOuter, Color shadowInner) {
        return new SoftBevelBorder(type, highlightOuter, highlightInner, shadowOuter, shadowInner);
    }

//// EtchedBorder ///////////////////////////////////////////////////////////

    static final Border sharedEtchedBorder = new EtchedBorder();
    private static Border sharedRaisedEtchedBorder;

    /**
     * Creates a border with an "etched" look using
     * the component's current background color for
     * highlighting and shading.
     *
     * @return the <code>Border</code> object
     */
    public static Border createEtchedBorder()    {
        return sharedEtchedBorder;
    }

    /**
     * Creates a border with an "etched" look using
     * the specified highlighting and shading colors.
     *
     * @param highlight  a <code>Color</code> object for the border highlights
     * @param shadow     a <code>Color</code> object for the border shadows
     * @return the <code>Border</code> object
     */
    public static Border createEtchedBorder(Color highlight, Color shadow)    {
        return new EtchedBorder(highlight, shadow);
    }

    /**
     * Creates a border with an "etched" look using
     * the component's current background color for
     * highlighting and shading.
     *
     * @param type      one of <code>EtchedBorder.RAISED</code>, or
     *                  <code>EtchedBorder.LOWERED</code>
     * @return the <code>Border</code> object
     * @exception IllegalArgumentException if type is not either
     *                  <code>EtchedBorder.RAISED</code> or
     *                  <code>EtchedBorder.LOWERED</code>
     * @since 1.3
     */
    public static Border createEtchedBorder(int type)    {
        switch (type) {
        case EtchedBorder.RAISED:
            if (sharedRaisedEtchedBorder == null) {
                sharedRaisedEtchedBorder = new EtchedBorder
                                           (EtchedBorder.RAISED);
            }
            return sharedRaisedEtchedBorder;
        case EtchedBorder.LOWERED:
            return sharedEtchedBorder;
        default:
            throw new IllegalArgumentException("type must be one of EtchedBorder.RAISED or EtchedBorder.LOWERED");
        }
    }

    /**
     * Creates a border with an "etched" look using
     * the specified highlighting and shading colors.
     *
     * @param type      one of <code>EtchedBorder.RAISED</code>, or
     *                  <code>EtchedBorder.LOWERED</code>
     * @param highlight  a <code>Color</code> object for the border highlights
     * @param shadow     a <code>Color</code> object for the border shadows
     * @return the <code>Border</code> object
     * @since 1.3
     */
    public static Border createEtchedBorder(int type, Color highlight,
                                            Color shadow)    {
        return new EtchedBorder(type, highlight, shadow);
    }

//// TitledBorder ////////////////////////////////////////////////////////////
    /**
     * Creates a new titled border with the specified title,
     * the default border type (determined by the current look and feel),
     * the default text position (determined by the current look and feel),
     * the default justification (leading), and the default
     * font and text color (determined by the current look and feel).
     *
     * @param title      a <code>String</code> containing the text of the title
     * @return the <code>TitledBorder</code> object
     */
    public static TitledBorder createTitledBorder(String title)     {
        return new TitledBorder(title);
    }

    /**
     * Creates a new titled border with an empty title,
     * the specified border object,
     * the default text position (determined by the current look and feel),
     * the default justification (leading), and the default
     * font and text color (determined by the current look and feel).
     *
     * @param border     the <code>Border</code> object to add the title to; if
     *                   <code>null</code> the <code>Border</code> is determined
     *                   by the current look and feel.
     * @return the <code>TitledBorder</code> object
     */
    public static TitledBorder createTitledBorder(Border border)       {
        return new TitledBorder(border);
    }

    /**
     * Adds a title to an existing border,
     * with default positioning (determined by the current look and feel),
     * default justification (leading) and the default
     * font and text color (determined by the current look and feel).
     *
     * @param border     the <code>Border</code> object to add the title to
     * @param title      a <code>String</code> containing the text of the title
     * @return the <code>TitledBorder</code> object
     */
    public static TitledBorder createTitledBorder(Border border,
                                                   String title) {
        return new TitledBorder(border, title);
    }

    /**
     * Adds a title to an existing border, with the specified
     * positioning and using the default
     * font and text color (determined by the current look and feel).
     *
     * @param border      the <code>Border</code> object to add the title to
     * @param title       a <code>String</code> containing the text of the title
     * @param titleJustification  an integer specifying the justification
     *        of the title -- one of the following:
     *<ul>
     *<li><code>TitledBorder.LEFT</code>
     *<li><code>TitledBorder.CENTER</code>
     *<li><code>TitledBorder.RIGHT</code>
     *<li><code>TitledBorder.LEADING</code>
     *<li><code>TitledBorder.TRAILING</code>
     *<li><code>TitledBorder.DEFAULT_JUSTIFICATION</code> (leading)
     *</ul>
     * @param titlePosition       an integer specifying the vertical position of
     *        the text in relation to the border -- one of the following:
     *<ul>
     *<li><code> TitledBorder.ABOVE_TOP</code>
     *<li><code>TitledBorder.TOP</code> (sitting on the top line)
     *<li><code>TitledBorder.BELOW_TOP</code>
     *<li><code>TitledBorder.ABOVE_BOTTOM</code>
     *<li><code>TitledBorder.BOTTOM</code> (sitting on the bottom line)
     *<li><code>TitledBorder.BELOW_BOTTOM</code>
     *<li><code>TitledBorder.DEFAULT_POSITION</code> (the title position
     *  is determined by the current look and feel)
     *</ul>
     * @return the <code>TitledBorder</code> object
     */
    public static TitledBorder createTitledBorder(Border border,
                        String title,
                        int titleJustification,
                        int titlePosition)      {
        return new TitledBorder(border, title, titleJustification,
                        titlePosition);
    }

    /**
     * Adds a title to an existing border, with the specified
     * positioning and font, and using the default text color
     * (determined by the current look and feel).
     *
     * @param border      the <code>Border</code> object to add the title to
     * @param title       a <code>String</code> containing the text of the title
     * @param titleJustification  an integer specifying the justification
     *        of the title -- one of the following:
     *<ul>
     *<li><code>TitledBorder.LEFT</code>
     *<li><code>TitledBorder.CENTER</code>
     *<li><code>TitledBorder.RIGHT</code>
     *<li><code>TitledBorder.LEADING</code>
     *<li><code>TitledBorder.TRAILING</code>
     *<li><code>TitledBorder.DEFAULT_JUSTIFICATION</code> (leading)
     *</ul>
     * @param titlePosition       an integer specifying the vertical position of
     *        the text in relation to the border -- one of the following:
     *<ul>
     *<li><code> TitledBorder.ABOVE_TOP</code>
     *<li><code>TitledBorder.TOP</code> (sitting on the top line)
     *<li><code>TitledBorder.BELOW_TOP</code>
     *<li><code>TitledBorder.ABOVE_BOTTOM</code>
     *<li><code>TitledBorder.BOTTOM</code> (sitting on the bottom line)
     *<li><code>TitledBorder.BELOW_BOTTOM</code>
     *<li><code>TitledBorder.DEFAULT_POSITION</code> (the title position
     *  is determined by the current look and feel)
     *</ul>
     * @param titleFont           a Font object specifying the title font
     * @return the TitledBorder object
     */
    public static TitledBorder createTitledBorder(Border border,
                        String title,
                        int titleJustification,
                        int titlePosition,
                        Font titleFont) {
        return new TitledBorder(border, title, titleJustification,
                        titlePosition, titleFont);
    }

    /**
     * Adds a title to an existing border, with the specified
     * positioning, font and color.
     *
     * @param border      the <code>Border</code> object to add the title to
     * @param title       a <code>String</code> containing the text of the title
     * @param titleJustification  an integer specifying the justification
     *        of the title -- one of the following:
     *<ul>
     *<li><code>TitledBorder.LEFT</code>
     *<li><code>TitledBorder.CENTER</code>
     *<li><code>TitledBorder.RIGHT</code>
     *<li><code>TitledBorder.LEADING</code>
     *<li><code>TitledBorder.TRAILING</code>
     *<li><code>TitledBorder.DEFAULT_JUSTIFICATION</code> (leading)
     *</ul>
     * @param titlePosition       an integer specifying the vertical position of
     *        the text in relation to the border -- one of the following:
     *<ul>
     *<li><code> TitledBorder.ABOVE_TOP</code>
     *<li><code>TitledBorder.TOP</code> (sitting on the top line)
     *<li><code>TitledBorder.BELOW_TOP</code>
     *<li><code>TitledBorder.ABOVE_BOTTOM</code>
     *<li><code>TitledBorder.BOTTOM</code> (sitting on the bottom line)
     *<li><code>TitledBorder.BELOW_BOTTOM</code>
     *<li><code>TitledBorder.DEFAULT_POSITION</code> (the title position
     *  is determined by the current look and feel)
     *</ul>
     * @param titleFont   a <code>Font</code> object specifying the title font
     * @param titleColor  a <code>Color</code> object specifying the title color
     * @return the <code>TitledBorder</code> object
     */
    public static TitledBorder createTitledBorder(Border border,
                        String title,
                        int titleJustification,
                        int titlePosition,
                        Font titleFont,
                        Color titleColor)       {
        return new TitledBorder(border, title, titleJustification,
                        titlePosition, titleFont, titleColor);
    }
//// EmptyBorder ///////////////////////////////////////////////////////////
    static final Border emptyBorder = new EmptyBorder(0, 0, 0, 0);

    /**
     * Creates an empty border that takes up no space. (The width
     * of the top, bottom, left, and right sides are all zero.)
     *
     * @return the <code>Border</code> object
     */
    public static Border createEmptyBorder() {
        return emptyBorder;
    }

    /**
     * Creates an empty border that takes up space but which does
     * no drawing, specifying the width of the top, left, bottom, and
     * right sides.
     *
     * @param top     an integer specifying the width of the top,
     *                  in pixels
     * @param left    an integer specifying the width of the left side,
     *                  in pixels
     * @param bottom  an integer specifying the width of the bottom,
     *                  in pixels
     * @param right   an integer specifying the width of the right side,
     *                  in pixels
     * @return the <code>Border</code> object
     */
    public static Border createEmptyBorder(int top, int left,
                                                int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }

//// CompoundBorder ////////////////////////////////////////////////////////
    /**
     * Creates a compound border with a <code>null</code> inside edge and a
     * <code>null</code> outside edge.
     *
     * @return the <code>CompoundBorder</code> object
     */
    public static CompoundBorder createCompoundBorder() {
        return new CompoundBorder();
    }

    /**
     * Creates a compound border specifying the border objects to use
     * for the outside and inside edges.
     *
     * @param outsideBorder  a <code>Border</code> object for the outer
     *                          edge of the compound border
     * @param insideBorder   a <code>Border</code> object for the inner
     *                          edge of the compound border
     * @return the <code>CompoundBorder</code> object
     */
    public static CompoundBorder createCompoundBorder(Border outsideBorder,
                                                Border insideBorder) {
        return new CompoundBorder(outsideBorder, insideBorder);
    }

//// MatteBorder ////////////////////////////////////////////////////////
    /**
     * Creates a matte-look border using a solid color. (The difference between
     * this border and a line border is that you can specify the individual
     * border dimensions.)
     *
     * @param top     an integer specifying the width of the top,
     *                          in pixels
     * @param left    an integer specifying the width of the left side,
     *                          in pixels
     * @param bottom  an integer specifying the width of the right side,
     *                          in pixels
     * @param right   an integer specifying the width of the bottom,
     *                          in pixels
     * @param color   a <code>Color</code> to use for the border
     * @return the <code>MatteBorder</code> object
     */
    public static MatteBorder createMatteBorder(int top, int left, int bottom, int right,
                                                Color color) {
        return new MatteBorder(top, left, bottom, right, color);
    }

    /**
     * Creates a matte-look border that consists of multiple tiles of a
     * specified icon. Multiple copies of the icon are placed side-by-side
     * to fill up the border area.
     * <p>
     * Note:<br>
     * If the icon doesn't load, the border area is painted gray.
     *
     * @param top     an integer specifying the width of the top,
     *                          in pixels
     * @param left    an integer specifying the width of the left side,
     *                          in pixels
     * @param bottom  an integer specifying the width of the right side,
     *                          in pixels
     * @param right   an integer specifying the width of the bottom,
     *                          in pixels
     * @param tileIcon  the <code>Icon</code> object used for the border tiles
     * @return the <code>MatteBorder</code> object
     */
    public static MatteBorder createMatteBorder(int top, int left, int bottom, int right,
                                                Icon tileIcon) {
        return new MatteBorder(top, left, bottom, right, tileIcon);
    }

//// StrokeBorder //////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a border of the specified {@code stroke}.
     * The component's foreground color will be used to render the border.
     *
     * @param stroke  the {@link BasicStroke} object used to stroke a shape
     * @return the {@code Border} object
     *
     * @throws NullPointerException if the specified {@code stroke} is {@code null}
     *
     * @since 1.7
     */
    public static Border createStrokeBorder(BasicStroke stroke) {
        return new StrokeBorder(stroke);
    }

    /**
     * Creates a border of the specified {@code stroke} and {@code paint}.
     * If the specified {@code paint} is {@code null},
     * the component's foreground color will be used to render the border.
     *
     * @param stroke  the {@link BasicStroke} object used to stroke a shape
     * @param paint   the {@link Paint} object used to generate a color
     * @return the {@code Border} object
     *
     * @throws NullPointerException if the specified {@code stroke} is {@code null}
     *
     * @since 1.7
     */
    public static Border createStrokeBorder(BasicStroke stroke, Paint paint) {
        return new StrokeBorder(stroke, paint);
    }

//// DashedBorder //////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

    private static Border sharedDashedBorder;

    /**
     * Creates a dashed border of the specified {@code paint}.
     * If the specified {@code paint} is {@code null},
     * the component's foreground color will be used to render the border.
     * The width of a dash line is equal to {@code 1}.
     * The relative length of a dash line and
     * the relative spacing between dash lines are equal to {@code 1}.
     * A dash line is not rounded.
     *
     * @param paint  the {@link Paint} object used to generate a color
     * @return the {@code Border} object
     *
     * @since 1.7
     */
    public static Border createDashedBorder(Paint paint) {
        return createDashedBorder(paint, 1.0f, 1.0f, 1.0f, false);
    }

    /**
     * Creates a dashed border of the specified {@code paint},
     * relative {@code length}, and relative {@code spacing}.
     * If the specified {@code paint} is {@code null},
     * the component's foreground color will be used to render the border.
     * The width of a dash line is equal to {@code 1}.
     * A dash line is not rounded.
     *
     * @param paint    the {@link Paint} object used to generate a color
     * @param length   the relative length of a dash line
     * @param spacing  the relative spacing between dash lines
     * @return the {@code Border} object
     *
     * @throws IllegalArgumentException if the specified {@code length} is less than {@code 1}, or
     *                                  if the specified {@code spacing} is less than {@code 0}
     * @since 1.7
     */
    public static Border createDashedBorder(Paint paint, float length, float spacing) {
        return createDashedBorder(paint, 1.0f, length, spacing, false);
    }

    /**
     * Creates a dashed border of the specified {@code paint}, {@code thickness},
     * line shape, relative {@code length}, and relative {@code spacing}.
     * If the specified {@code paint} is {@code null},
     * the component's foreground color will be used to render the border.
     *
     * @param paint      the {@link Paint} object used to generate a color
     * @param thickness  the width of a dash line
     * @param length     the relative length of a dash line
     * @param spacing    the relative spacing between dash lines
     * @param rounded    whether or not line ends should be round
     * @return the {@code Border} object
     *
     * @throws IllegalArgumentException if the specified {@code thickness} is less than {@code 1}, or
     *                                  if the specified {@code length} is less than {@code 1}, or
     *                                  if the specified {@code spacing} is less than {@code 0}
     * @since 1.7
     */
    public static Border createDashedBorder(Paint paint, float thickness, float length, float spacing, boolean rounded) {
        boolean shared = !rounded && (paint == null) && (thickness == 1.0f) && (length == 1.0f) && (spacing == 1.0f);
        if (shared && (sharedDashedBorder != null)) {
            return sharedDashedBorder;
        }
        if (thickness < 1.0f) {
            throw new IllegalArgumentException("thickness is less than 1");
        }
        if (length < 1.0f) {
            throw new IllegalArgumentException("length is less than 1");
        }
        if (spacing < 0.0f) {
            throw new IllegalArgumentException("spacing is less than 0");
        }
        int cap = rounded ? BasicStroke.CAP_ROUND : BasicStroke.CAP_SQUARE;
        int join = rounded ? BasicStroke.JOIN_ROUND : BasicStroke.JOIN_MITER;
        float[] array = { thickness * (length - 1.0f), thickness * (spacing + 1.0f) };
        Border border = createStrokeBorder(new BasicStroke(thickness, cap, join, thickness * 2.0f, array, 0.0f), paint);
        if (shared) {
            sharedDashedBorder = border;
        }
        return border;
    }
}
