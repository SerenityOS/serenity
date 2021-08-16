/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * The {@code GridBagConstraints} class specifies constraints
 * for components that are laid out using the
 * {@code GridBagLayout} class.
 *
 * @author Doug Stein
 * @author Bill Spitzak (orignial NeWS &amp; OLIT implementation)
 * @see java.awt.GridBagLayout
 * @since 1.0
 */
public class GridBagConstraints implements Cloneable, java.io.Serializable {

    /**
     * Specifies that this component is the next-to-last component in its
     * column or row ({@code gridwidth}, {@code gridheight}),
     * or that this component be placed next to the previously added
     * component ({@code gridx}, {@code gridy}).
     * @see      java.awt.GridBagConstraints#gridwidth
     * @see      java.awt.GridBagConstraints#gridheight
     * @see      java.awt.GridBagConstraints#gridx
     * @see      java.awt.GridBagConstraints#gridy
     */
    public static final int RELATIVE = -1;

    /**
     * Specifies that this component is the
     * last component in its column or row.
     */
    public static final int REMAINDER = 0;

    /**
     * Do not resize the component.
     */
    public static final int NONE = 0;

    /**
     * Resize the component both horizontally and vertically.
     */
    public static final int BOTH = 1;

    /**
     * Resize the component horizontally but not vertically.
     */
    public static final int HORIZONTAL = 2;

    /**
     * Resize the component vertically but not horizontally.
     */
    public static final int VERTICAL = 3;

    /**
     * Put the component in the center of its display area.
     */
    public static final int CENTER = 10;

    /**
     * Put the component at the top of its display area,
     * centered horizontally.
     */
    public static final int NORTH = 11;

    /**
     * Put the component at the top-right corner of its display area.
     */
    public static final int NORTHEAST = 12;

    /**
     * Put the component on the right side of its display area,
     * centered vertically.
     */
    public static final int EAST = 13;

    /**
     * Put the component at the bottom-right corner of its display area.
     */
    public static final int SOUTHEAST = 14;

    /**
     * Put the component at the bottom of its display area, centered
     * horizontally.
     */
    public static final int SOUTH = 15;

    /**
     * Put the component at the bottom-left corner of its display area.
     */
    public static final int SOUTHWEST = 16;

    /**
     * Put the component on the left side of its display area,
     * centered vertically.
     */
    public static final int WEST = 17;

    /**
     * Put the component at the top-left corner of its display area.
     */
    public static final int NORTHWEST = 18;

    /**
     * Place the component centered along the edge of its display area
     * associated with the start of a page for the current
     * {@code ComponentOrientation}.  Equal to NORTH for horizontal
     * orientations.
     */
    public static final int PAGE_START = 19;

    /**
     * Place the component centered along the edge of its display area
     * associated with the end of a page for the current
     * {@code ComponentOrientation}.  Equal to SOUTH for horizontal
     * orientations.
     */
    public static final int PAGE_END = 20;

    /**
     * Place the component centered along the edge of its display area where
     * lines of text would normally begin for the current
     * {@code ComponentOrientation}.  Equal to WEST for horizontal,
     * left-to-right orientations and EAST for horizontal, right-to-left
     * orientations.
     */
    public static final int LINE_START = 21;

    /**
     * Place the component centered along the edge of its display area where
     * lines of text would normally end for the current
     * {@code ComponentOrientation}.  Equal to EAST for horizontal,
     * left-to-right orientations and WEST for horizontal, right-to-left
     * orientations.
     */
    public static final int LINE_END = 22;

    /**
     * Place the component in the corner of its display area where
     * the first line of text on a page would normally begin for the current
     * {@code ComponentOrientation}.  Equal to NORTHWEST for horizontal,
     * left-to-right orientations and NORTHEAST for horizontal, right-to-left
     * orientations.
     */
    public static final int FIRST_LINE_START = 23;

    /**
     * Place the component in the corner of its display area where
     * the first line of text on a page would normally end for the current
     * {@code ComponentOrientation}.  Equal to NORTHEAST for horizontal,
     * left-to-right orientations and NORTHWEST for horizontal, right-to-left
     * orientations.
     */
    public static final int FIRST_LINE_END = 24;

    /**
     * Place the component in the corner of its display area where
     * the last line of text on a page would normally start for the current
     * {@code ComponentOrientation}.  Equal to SOUTHWEST for horizontal,
     * left-to-right orientations and SOUTHEAST for horizontal, right-to-left
     * orientations.
     */
    public static final int LAST_LINE_START = 25;

    /**
     * Place the component in the corner of its display area where
     * the last line of text on a page would normally end for the current
     * {@code ComponentOrientation}.  Equal to SOUTHEAST for horizontal,
     * left-to-right orientations and SOUTHWEST for horizontal, right-to-left
     * orientations.
     */
    public static final int LAST_LINE_END = 26;

    /**
     * Possible value for the {@code anchor} field.  Specifies
     * that the component should be horizontally centered and
     * vertically aligned along the baseline of the prevailing row.
     * If the component does not have a baseline it will be vertically
     * centered.
     *
     * @since 1.6
     */
    public static final int BASELINE = 0x100;

    /**
     * Possible value for the {@code anchor} field.  Specifies
     * that the component should be horizontally placed along the
     * leading edge.  For components with a left-to-right orientation,
     * the leading edge is the left edge.  Vertically the component is
     * aligned along the baseline of the prevailing row.  If the
     * component does not have a baseline it will be vertically
     * centered.
     *
     * @since 1.6
     */
    public static final int BASELINE_LEADING = 0x200;

    /**
     * Possible value for the {@code anchor} field.  Specifies
     * that the component should be horizontally placed along the
     * trailing edge.  For components with a left-to-right
     * orientation, the trailing edge is the right edge.  Vertically
     * the component is aligned along the baseline of the prevailing
     * row.  If the component does not have a baseline it will be
     * vertically centered.
     *
     * @since 1.6
     */
    public static final int BASELINE_TRAILING = 0x300;

    /**
     * Possible value for the {@code anchor} field.  Specifies
     * that the component should be horizontally centered.  Vertically
     * the component is positioned so that its bottom edge touches
     * the baseline of the starting row.  If the starting row does not
     * have a baseline it will be vertically centered.
     *
     * @since 1.6
     */
    public static final int ABOVE_BASELINE = 0x400;

    /**
     * Possible value for the {@code anchor} field.  Specifies
     * that the component should be horizontally placed along the
     * leading edge.  For components with a left-to-right orientation,
     * the leading edge is the left edge.  Vertically the component is
     * positioned so that its bottom edge touches the baseline of the
     * starting row.  If the starting row does not have a baseline it
     * will be vertically centered.
     *
     * @since 1.6
     */
    public static final int ABOVE_BASELINE_LEADING = 0x500;

    /**
     * Possible value for the {@code anchor} field.  Specifies
     * that the component should be horizontally placed along the
     * trailing edge.  For components with a left-to-right
     * orientation, the trailing edge is the right edge.  Vertically
     * the component is positioned so that its bottom edge touches
     * the baseline of the starting row.  If the starting row does not
     * have a baseline it will be vertically centered.
     *
     * @since 1.6
     */
    public static final int ABOVE_BASELINE_TRAILING = 0x600;

    /**
     * Possible value for the {@code anchor} field.  Specifies
     * that the component should be horizontally centered.  Vertically
     * the component is positioned so that its top edge touches the
     * baseline of the starting row.  If the starting row does not
     * have a baseline it will be vertically centered.
     *
     * @since 1.6
     */
    public static final int BELOW_BASELINE = 0x700;

    /**
     * Possible value for the {@code anchor} field.  Specifies
     * that the component should be horizontally placed along the
     * leading edge.  For components with a left-to-right orientation,
     * the leading edge is the left edge.  Vertically the component is
     * positioned so that its top edge touches the baseline of the
     * starting row.  If the starting row does not have a baseline it
     * will be vertically centered.
     *
     * @since 1.6
     */
    public static final int BELOW_BASELINE_LEADING = 0x800;

    /**
     * Possible value for the {@code anchor} field.  Specifies
     * that the component should be horizontally placed along the
     * trailing edge.  For components with a left-to-right
     * orientation, the trailing edge is the right edge.  Vertically
     * the component is positioned so that its top edge touches the
     * baseline of the starting row.  If the starting row does not
     * have a baseline it will be vertically centered.
     *
     * @since 1.6
     */
    public static final int BELOW_BASELINE_TRAILING = 0x900;

    /**
     * Specifies the cell containing the leading edge of the component's
     * display area, where the first cell in a row has {@code gridx=0}.
     * The leading edge of a component's display area is its left edge for
     * a horizontal, left-to-right container and its right edge for a
     * horizontal, right-to-left container.
     * The value
     * {@code RELATIVE} specifies that the component be placed
     * immediately following the component that was added to the container
     * just before this component was added.
     * <p>
     * The default value is {@code RELATIVE}.
     * {@code gridx} should be a non-negative value.
     * @serial
     * @see #clone()
     * @see java.awt.GridBagConstraints#gridy
     * @see java.awt.ComponentOrientation
     */
    public int gridx;

    /**
     * Specifies the cell at the top of the component's display area,
     * where the topmost cell has {@code gridy=0}. The value
     * {@code RELATIVE} specifies that the component be placed just
     * below the component that was added to the container just before
     * this component was added.
     * <p>
     * The default value is {@code RELATIVE}.
     * {@code gridy} should be a non-negative value.
     * @serial
     * @see #clone()
     * @see java.awt.GridBagConstraints#gridx
     */
    public int gridy;

    /**
     * Specifies the number of cells in a row for the component's
     * display area.
     * <p>
     * Use {@code REMAINDER} to specify that the component's
     * display area will be from {@code gridx} to the last
     * cell in the row.
     * Use {@code RELATIVE} to specify that the component's
     * display area will be from {@code gridx} to the next
     * to the last one in its row.
     * <p>
     * {@code gridwidth} should be non-negative and the default
     * value is 1.
     * @serial
     * @see #clone()
     * @see java.awt.GridBagConstraints#gridheight
     */
    public int gridwidth;

    /**
     * Specifies the number of cells in a column for the component's
     * display area.
     * <p>
     * Use {@code REMAINDER} to specify that the component's
     * display area will be from {@code gridy} to the last
     * cell in the column.
     * Use {@code RELATIVE} to specify that the component's
     * display area will be from {@code gridy} to the next
     * to the last one in its column.
     * <p>
     * {@code gridheight} should be a non-negative value and the
     * default value is 1.
     * @serial
     * @see #clone()
     * @see java.awt.GridBagConstraints#gridwidth
     */
    public int gridheight;

    /**
     * Specifies how to distribute extra horizontal space.
     * <p>
     * The grid bag layout manager calculates the weight of a column to
     * be the maximum {@code weightx} of all the components in a
     * column. If the resulting layout is smaller horizontally than the area
     * it needs to fill, the extra space is distributed to each column in
     * proportion to its weight. A column that has a weight of zero receives
     * no extra space.
     * <p>
     * If all the weights are zero, all the extra space appears between
     * the grids of the cell and the left and right edges.
     * <p>
     * The default value of this field is {@code 0}.
     * {@code weightx} should be a non-negative value.
     * @serial
     * @see #clone()
     * @see java.awt.GridBagConstraints#weighty
     */
    public double weightx;

    /**
     * Specifies how to distribute extra vertical space.
     * <p>
     * The grid bag layout manager calculates the weight of a row to be
     * the maximum {@code weighty} of all the components in a row.
     * If the resulting layout is smaller vertically than the area it
     * needs to fill, the extra space is distributed to each row in
     * proportion to its weight. A row that has a weight of zero receives no
     * extra space.
     * <p>
     * If all the weights are zero, all the extra space appears between
     * the grids of the cell and the top and bottom edges.
     * <p>
     * The default value of this field is {@code 0}.
     * {@code weighty} should be a non-negative value.
     * @serial
     * @see #clone()
     * @see java.awt.GridBagConstraints#weightx
     */
    public double weighty;

    /**
     * This field is used when the component is smaller than its
     * display area. It determines where, within the display area, to
     * place the component.
     * <p> There are three kinds of possible values: orientation
     * relative, baseline relative and absolute.  Orientation relative
     * values are interpreted relative to the container's component
     * orientation property, baseline relative values are interpreted
     * relative to the baseline and absolute values are not.  The
     * absolute values are:
     * {@code CENTER}, {@code NORTH}, {@code NORTHEAST},
     * {@code EAST}, {@code SOUTHEAST}, {@code SOUTH},
     * {@code SOUTHWEST}, {@code WEST}, and {@code NORTHWEST}.
     * The orientation relative values are: {@code PAGE_START},
     * {@code PAGE_END},
     * {@code LINE_START}, {@code LINE_END},
     * {@code FIRST_LINE_START}, {@code FIRST_LINE_END},
     * {@code LAST_LINE_START} and {@code LAST_LINE_END}.  The
     * baseline relative values are:
     * {@code BASELINE}, {@code BASELINE_LEADING},
     * {@code BASELINE_TRAILING},
     * {@code ABOVE_BASELINE}, {@code ABOVE_BASELINE_LEADING},
     * {@code ABOVE_BASELINE_TRAILING},
     * {@code BELOW_BASELINE}, {@code BELOW_BASELINE_LEADING},
     * and {@code BELOW_BASELINE_TRAILING}.
     * The default value is {@code CENTER}.
     * @serial
     * @see #clone()
     * @see java.awt.ComponentOrientation
     */
    public int anchor;

    /**
     * This field is used when the component's display area is larger
     * than the component's requested size. It determines whether to
     * resize the component, and if so, how.
     * <p>
     * The following values are valid for {@code fill}:
     *
     * <ul>
     * <li>
     * {@code NONE}: Do not resize the component.
     * <li>
     * {@code HORIZONTAL}: Make the component wide enough to fill
     *         its display area horizontally, but do not change its height.
     * <li>
     * {@code VERTICAL}: Make the component tall enough to fill its
     *         display area vertically, but do not change its width.
     * <li>
     * {@code BOTH}: Make the component fill its display area
     *         entirely.
     * </ul>
     * <p>
     * The default value is {@code NONE}.
     * @serial
     * @see #clone()
     */
    public int fill;

    /**
     * This field specifies the external padding of the component, the
     * minimum amount of space between the component and the edges of its
     * display area.
     * <p>
     * The default value is {@code new Insets(0, 0, 0, 0)}.
     * @serial
     * @see #clone()
     */
    public Insets insets;

    /**
     * This field specifies the internal padding of the component, how much
     * space to add to the minimum width of the component. The width of
     * the component is at least its minimum width plus
     * {@code ipadx} pixels.
     * <p>
     * The default value is {@code 0}.
     * @serial
     * @see #clone()
     * @see java.awt.GridBagConstraints#ipady
     */
    public int ipadx;

    /**
     * This field specifies the internal padding, that is, how much
     * space to add to the minimum height of the component. The height of
     * the component is at least its minimum height plus
     * {@code ipady} pixels.
     * <p>
     * The default value is 0.
     * @serial
     * @see #clone()
     * @see java.awt.GridBagConstraints#ipadx
     */
    public int ipady;

    /**
     * Temporary place holder for the x coordinate.
     * @serial
     */
    int tempX;
    /**
     * Temporary place holder for the y coordinate.
     * @serial
     */
    int tempY;
    /**
     * Temporary place holder for the Width of the component.
     * @serial
     */
    int tempWidth;
    /**
     * Temporary place holder for the Height of the component.
     * @serial
     */
    int tempHeight;
    /**
     * The minimum width of the component.  It is used to calculate
     * {@code ipady}, where the default will be 0.
     * @serial
     * @see #ipady
     */
    int minWidth;
    /**
     * The minimum height of the component. It is used to calculate
     * {@code ipadx}, where the default will be 0.
     * @serial
     * @see #ipadx
     */
    int minHeight;

    // The following fields are only used if the anchor is
    // one of BASELINE, BASELINE_LEADING or BASELINE_TRAILING.
    // ascent and descent include the insets and ipady values.
    transient int ascent;
    transient int descent;
    transient Component.BaselineResizeBehavior baselineResizeBehavior;
    // The following two fields are used if the baseline type is
    // CENTER_OFFSET.
    // centerPadding is either 0 or 1 and indicates if
    // the height needs to be padded by one when calculating where the
    // baseline lands
    transient int centerPadding;
    // Where the baseline lands relative to the center of the component.
    transient int centerOffset;

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -1000070633030801713L;

    /**
     * Creates a {@code GridBagConstraint} object with
     * all of its fields set to their default value.
     */
    public GridBagConstraints () {
        gridx = RELATIVE;
        gridy = RELATIVE;
        gridwidth = 1;
        gridheight = 1;

        weightx = 0;
        weighty = 0;
        anchor = CENTER;
        fill = NONE;

        insets = new Insets(0, 0, 0, 0);
        ipadx = 0;
        ipady = 0;
    }

    /**
     * Creates a {@code GridBagConstraints} object with
     * all of its fields set to the passed-in arguments.
     *
     * Note: Because the use of this constructor hinders readability
     * of source code, this constructor should only be used by
     * automatic source code generation tools.
     *
     * @param gridx     The initial gridx value.
     * @param gridy     The initial gridy value.
     * @param gridwidth The initial gridwidth value.
     * @param gridheight        The initial gridheight value.
     * @param weightx   The initial weightx value.
     * @param weighty   The initial weighty value.
     * @param anchor    The initial anchor value.
     * @param fill      The initial fill value.
     * @param insets    The initial insets value.
     * @param ipadx     The initial ipadx value.
     * @param ipady     The initial ipady value.
     *
     * @see java.awt.GridBagConstraints#gridx
     * @see java.awt.GridBagConstraints#gridy
     * @see java.awt.GridBagConstraints#gridwidth
     * @see java.awt.GridBagConstraints#gridheight
     * @see java.awt.GridBagConstraints#weightx
     * @see java.awt.GridBagConstraints#weighty
     * @see java.awt.GridBagConstraints#anchor
     * @see java.awt.GridBagConstraints#fill
     * @see java.awt.GridBagConstraints#insets
     * @see java.awt.GridBagConstraints#ipadx
     * @see java.awt.GridBagConstraints#ipady
     *
     * @since 1.2
     */
    public GridBagConstraints(int gridx, int gridy,
                              int gridwidth, int gridheight,
                              double weightx, double weighty,
                              int anchor, int fill,
                              Insets insets, int ipadx, int ipady) {
        this.gridx = gridx;
        this.gridy = gridy;
        this.gridwidth = gridwidth;
        this.gridheight = gridheight;
        this.fill = fill;
        this.ipadx = ipadx;
        this.ipady = ipady;
        this.insets = insets;
        this.anchor  = anchor;
        this.weightx = weightx;
        this.weighty = weighty;
    }

    /**
     * Creates a copy of this grid bag constraint.
     * @return     a copy of this grid bag constraint
     */
    public Object clone () {
        try {
            GridBagConstraints c = (GridBagConstraints)super.clone();
            c.insets = (Insets)insets.clone();
            return c;
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
    }

    boolean isVerticallyResizable() {
        return (fill == BOTH || fill == VERTICAL);
    }
}
