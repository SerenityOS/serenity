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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;

/**
 * A flow layout arranges components in a directional flow, much
 * like lines of text in a paragraph. The flow direction is
 * determined by the container's {@code componentOrientation}
 * property and may be one of two values:
 * <ul>
 * <li>{@code ComponentOrientation.LEFT_TO_RIGHT}
 * <li>{@code ComponentOrientation.RIGHT_TO_LEFT}
 * </ul>
 * Flow layouts are typically used
 * to arrange buttons in a panel. It arranges buttons
 * horizontally until no more buttons fit on the same line.
 * The line alignment is determined by the {@code align}
 * property. The possible values are:
 * <ul>
 * <li>{@link #LEFT LEFT}
 * <li>{@link #RIGHT RIGHT}
 * <li>{@link #CENTER CENTER}
 * <li>{@link #LEADING LEADING}
 * <li>{@link #TRAILING TRAILING}
 * </ul>
 * <p>
 * For example, the following picture shows an applet using the flow
 * layout manager (its default layout manager) to position three buttons:
 * <p>
 * <img src="doc-files/FlowLayout-1.gif"
 * ALT="Graphic of Layout for Three Buttons"
 * style="margin: 7px 10px;">
 * <p>
 * Here is the code for this applet:
 *
 * <hr><blockquote><pre>
 * import java.awt.*;
 * import java.applet.Applet;
 *
 * public class myButtons extends Applet {
 *     Button button1, button2, button3;
 *     public void init() {
 *         button1 = new Button("Ok");
 *         button2 = new Button("Open");
 *         button3 = new Button("Close");
 *         add(button1);
 *         add(button2);
 *         add(button3);
 *     }
 * }
 * </pre></blockquote><hr>
 * <p>
 * A flow layout lets each component assume its natural (preferred) size.
 *
 * @author      Arthur van Hoff
 * @author      Sami Shaio
 * @since       1.0
 * @see ComponentOrientation
 */
public class FlowLayout implements LayoutManager, java.io.Serializable {

    /**
     * This value indicates that each row of components
     * should be left-justified.
     */
    public static final int LEFT        = 0;

    /**
     * This value indicates that each row of components
     * should be centered.
     */
    public static final int CENTER      = 1;

    /**
     * This value indicates that each row of components
     * should be right-justified.
     */
    public static final int RIGHT       = 2;

    /**
     * This value indicates that each row of components
     * should be justified to the leading edge of the container's
     * orientation, for example, to the left in left-to-right orientations.
     *
     * @see     java.awt.Component#getComponentOrientation
     * @see     java.awt.ComponentOrientation
     * @since   1.2
     */
    public static final int LEADING     = 3;

    /**
     * This value indicates that each row of components
     * should be justified to the trailing edge of the container's
     * orientation, for example, to the right in left-to-right orientations.
     *
     * @see     java.awt.Component#getComponentOrientation
     * @see     java.awt.ComponentOrientation
     * @since   1.2
     */
    public static final int TRAILING = 4;

    /**
     * {@code align} is the property that determines
     * how each row distributes empty space.
     * It can be one of the following values:
     * <ul>
     * <li>{@code LEFT}
     * <li>{@code RIGHT}
     * <li>{@code CENTER}
     * </ul>
     *
     * @serial
     * @see #getAlignment
     * @see #setAlignment
     */
    int align;          // This is for 1.1 serialization compatibility

    /**
     * {@code newAlign} is the property that determines
     * how each row distributes empty space for the Java 2 platform,
     * v1.2 and greater.
     * It can be one of the following three values:
     * <ul>
     * <li>{@code LEFT}
     * <li>{@code RIGHT}
     * <li>{@code CENTER}
     * <li>{@code LEADING}
     * <li>{@code TRAILING}
     * </ul>
     *
     * @serial
     * @since 1.2
     * @see #getAlignment
     * @see #setAlignment
     */
    int newAlign;       // This is the one we actually use

    /**
     * The flow layout manager allows a separation of
     * components with gaps.  The horizontal gap will
     * specify the space between components and between
     * the components and the borders of the
     * {@code Container}.
     *
     * @serial
     * @see #getHgap()
     * @see #setHgap(int)
     */
    int hgap;

    /**
     * The flow layout manager allows a separation of
     * components with gaps.  The vertical gap will
     * specify the space between rows and between the
     * the rows and the borders of the {@code Container}.
     *
     * @serial
     * @see #getHgap()
     * @see #setHgap(int)
     */
    int vgap;

    /**
     * If true, components will be aligned on their baseline.
     */
    private boolean alignOnBaseline;

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
     @Serial
     private static final long serialVersionUID = -7262534875583282631L;

    /**
     * Constructs a new {@code FlowLayout} with a centered alignment and a
     * default 5-unit horizontal and vertical gap.
     */
    public FlowLayout() {
        this(CENTER, 5, 5);
    }

    /**
     * Constructs a new {@code FlowLayout} with the specified
     * alignment and a default 5-unit horizontal and vertical gap.
     * The value of the alignment argument must be one of
     * {@code FlowLayout.LEFT}, {@code FlowLayout.RIGHT},
     * {@code FlowLayout.CENTER}, {@code FlowLayout.LEADING},
     * or {@code FlowLayout.TRAILING}.
     * @param align the alignment value
     */
    public FlowLayout(int align) {
        this(align, 5, 5);
    }

    /**
     * Creates a new flow layout manager with the indicated alignment
     * and the indicated horizontal and vertical gaps.
     * <p>
     * The value of the alignment argument must be one of
     * {@code FlowLayout.LEFT}, {@code FlowLayout.RIGHT},
     * {@code FlowLayout.CENTER}, {@code FlowLayout.LEADING},
     * or {@code FlowLayout.TRAILING}.
     * @param      align   the alignment value
     * @param      hgap    the horizontal gap between components
     *                     and between the components and the
     *                     borders of the {@code Container}
     * @param      vgap    the vertical gap between components
     *                     and between the components and the
     *                     borders of the {@code Container}
     */
    public FlowLayout(int align, int hgap, int vgap) {
        this.hgap = hgap;
        this.vgap = vgap;
        setAlignment(align);
    }

    /**
     * Gets the alignment for this layout.
     * Possible values are {@code FlowLayout.LEFT},
     * {@code FlowLayout.RIGHT}, {@code FlowLayout.CENTER},
     * {@code FlowLayout.LEADING},
     * or {@code FlowLayout.TRAILING}.
     * @return     the alignment value for this layout
     * @see        java.awt.FlowLayout#setAlignment
     * @since      1.1
     */
    public int getAlignment() {
        return newAlign;
    }

    /**
     * Sets the alignment for this layout.
     * Possible values are
     * <ul>
     * <li>{@code FlowLayout.LEFT}
     * <li>{@code FlowLayout.RIGHT}
     * <li>{@code FlowLayout.CENTER}
     * <li>{@code FlowLayout.LEADING}
     * <li>{@code FlowLayout.TRAILING}
     * </ul>
     * @param      align one of the alignment values shown above
     * @see        #getAlignment()
     * @since      1.1
     */
    public void setAlignment(int align) {
        this.newAlign = align;

        // this.align is used only for serialization compatibility,
        // so set it to a value compatible with the 1.1 version
        // of the class

        switch (align) {
        case LEADING:
            this.align = LEFT;
            break;
        case TRAILING:
            this.align = RIGHT;
            break;
        default:
            this.align = align;
            break;
        }
    }

    /**
     * Gets the horizontal gap between components
     * and between the components and the borders
     * of the {@code Container}
     *
     * @return     the horizontal gap between components
     *             and between the components and the borders
     *             of the {@code Container}
     * @see        java.awt.FlowLayout#setHgap
     * @since      1.1
     */
    public int getHgap() {
        return hgap;
    }

    /**
     * Sets the horizontal gap between components and
     * between the components and the borders of the
     * {@code Container}.
     *
     * @param hgap the horizontal gap between components
     *             and between the components and the borders
     *             of the {@code Container}
     * @see        java.awt.FlowLayout#getHgap
     * @since      1.1
     */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /**
     * Gets the vertical gap between components and
     * between the components and the borders of the
     * {@code Container}.
     *
     * @return     the vertical gap between components
     *             and between the components and the borders
     *             of the {@code Container}
     * @see        java.awt.FlowLayout#setVgap
     * @since      1.1
     */
    public int getVgap() {
        return vgap;
    }

    /**
     * Sets the vertical gap between components and between
     * the components and the borders of the {@code Container}.
     *
     * @param vgap the vertical gap between components
     *             and between the components and the borders
     *             of the {@code Container}
     * @see        java.awt.FlowLayout#getVgap
     * @since      1.1
     */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    /**
     * Sets whether or not components should be vertically aligned along their
     * baseline.  Components that do not have a baseline will be centered.
     * The default is false.
     *
     * @param alignOnBaseline whether or not components should be
     *                        vertically aligned on their baseline
     * @since 1.6
     */
    public void setAlignOnBaseline(boolean alignOnBaseline) {
        this.alignOnBaseline = alignOnBaseline;
    }

    /**
     * Returns true if components are to be vertically aligned along
     * their baseline.  The default is false.
     *
     * @return true if components are to be vertically aligned along
     *              their baseline
     * @since 1.6
     */
    public boolean getAlignOnBaseline() {
        return alignOnBaseline;
    }

    /**
     * Adds the specified component to the layout.
     * Not used by this class.
     * @param name the name of the component
     * @param comp the component to be added
     */
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * Removes the specified component from the layout.
     * Not used by this class.
     * @param comp the component to remove
     * @see       java.awt.Container#removeAll
     */
    public void removeLayoutComponent(Component comp) {
    }

    /**
     * Returns the preferred dimensions for this layout given the
     * <i>visible</i> components in the specified target container.
     *
     * @param target the container that needs to be laid out
     * @return    the preferred dimensions to lay out the
     *            subcomponents of the specified container
     * @see Container
     * @see #minimumLayoutSize
     * @see       java.awt.Container#getPreferredSize
     */
    public Dimension preferredLayoutSize(Container target) {
      synchronized (target.getTreeLock()) {
        Dimension dim = new Dimension(0, 0);
        int nmembers = target.getComponentCount();
        boolean firstVisibleComponent = true;
        boolean useBaseline = getAlignOnBaseline();
        int maxAscent = 0;
        int maxDescent = 0;

        for (int i = 0 ; i < nmembers ; i++) {
            Component m = target.getComponent(i);
            if (m.isVisible()) {
                Dimension d = m.getPreferredSize();
                dim.height = Math.max(dim.height, d.height);
                if (firstVisibleComponent) {
                    firstVisibleComponent = false;
                } else {
                    dim.width += hgap;
                }
                dim.width += d.width;
                if (useBaseline) {
                    int baseline = m.getBaseline(d.width, d.height);
                    if (baseline >= 0) {
                        maxAscent = Math.max(maxAscent, baseline);
                        maxDescent = Math.max(maxDescent, d.height - baseline);
                    }
                }
            }
        }
        if (useBaseline) {
            dim.height = Math.max(maxAscent + maxDescent, dim.height);
        }
        Insets insets = target.getInsets();
        dim.width += insets.left + insets.right + hgap*2;
        dim.height += insets.top + insets.bottom + vgap*2;
        return dim;
      }
    }

    /**
     * Returns the minimum dimensions needed to layout the <i>visible</i>
     * components contained in the specified target container.
     * @param target the container that needs to be laid out
     * @return    the minimum dimensions to lay out the
     *            subcomponents of the specified container
     * @see #preferredLayoutSize
     * @see       java.awt.Container
     * @see       java.awt.Container#doLayout
     */
    public Dimension minimumLayoutSize(Container target) {
      synchronized (target.getTreeLock()) {
        boolean useBaseline = getAlignOnBaseline();
        Dimension dim = new Dimension(0, 0);
        int nmembers = target.getComponentCount();
        int maxAscent = 0;
        int maxDescent = 0;
        boolean firstVisibleComponent = true;

        for (int i = 0 ; i < nmembers ; i++) {
            Component m = target.getComponent(i);
            if (m.visible) {
                Dimension d = m.getMinimumSize();
                dim.height = Math.max(dim.height, d.height);
                if (firstVisibleComponent) {
                    firstVisibleComponent = false;
                } else {
                    dim.width += hgap;
                }
                dim.width += d.width;
                if (useBaseline) {
                    int baseline = m.getBaseline(d.width, d.height);
                    if (baseline >= 0) {
                        maxAscent = Math.max(maxAscent, baseline);
                        maxDescent = Math.max(maxDescent,
                                              dim.height - baseline);
                    }
                }
}
}

        if (useBaseline) {
            dim.height = Math.max(maxAscent + maxDescent, dim.height);
        }

        Insets insets = target.getInsets();
        dim.width += insets.left + insets.right + hgap*2;
        dim.height += insets.top + insets.bottom + vgap*2;
        return dim;





      }
    }

    /**
     * Centers the elements in the specified row, if there is any slack.
     * @param target the component which needs to be moved
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width dimensions
     * @param height the height dimensions
     * @param rowStart the beginning of the row
     * @param rowEnd the ending of the row
     * @param useBaseline Whether or not to align on baseline.
     * @param ascent Ascent for the components. This is only valid if
     *               useBaseline is true.
     * @param descent Ascent for the components. This is only valid if
     *               useBaseline is true.
     * @return actual row height
     */
    private int moveComponents(Container target, int x, int y, int width, int height,
                                int rowStart, int rowEnd, boolean ltr,
                                boolean useBaseline, int[] ascent,
                                int[] descent) {
        switch (newAlign) {
        case LEFT:
            x += ltr ? 0 : width;
            break;
        case CENTER:
            x += width / 2;
            break;
        case RIGHT:
            x += ltr ? width : 0;
            break;
        case LEADING:
            break;
        case TRAILING:
            x += width;
            break;
        }
        int maxAscent = 0;
        int nonbaselineHeight = 0;
        int baselineOffset = 0;
        if (useBaseline) {
            int maxDescent = 0;
            for (int i = rowStart ; i < rowEnd ; i++) {
                Component m = target.getComponent(i);
                if (m.visible) {
                    if (ascent[i] >= 0) {
                        maxAscent = Math.max(maxAscent, ascent[i]);
                        maxDescent = Math.max(maxDescent, descent[i]);
                    }
                    else {
                        nonbaselineHeight = Math.max(m.getHeight(),
                                                     nonbaselineHeight);
                    }
                }
            }
            height = Math.max(maxAscent + maxDescent, nonbaselineHeight);
            baselineOffset = (height - maxAscent - maxDescent) / 2;
        }
        for (int i = rowStart ; i < rowEnd ; i++) {
            Component m = target.getComponent(i);
            if (m.isVisible()) {
                int cy;
                if (useBaseline && ascent[i] >= 0) {
                    cy = y + baselineOffset + maxAscent - ascent[i];
                }
                else {
                    cy = y + (height - m.height) / 2;
                }
                if (ltr) {
                    m.setLocation(x, cy);
                } else {
                    m.setLocation(target.width - x - m.width, cy);
                }
                x += m.width + hgap;
            }
        }
        return height;
    }

    /**
     * Lays out the container. This method lets each
     * <i>visible</i> component take
     * its preferred size by reshaping the components in the
     * target container in order to satisfy the alignment of
     * this {@code FlowLayout} object.
     *
     * @param target the specified component being laid out
     * @see Container
     * @see       java.awt.Container#doLayout
     */
    public void layoutContainer(Container target) {
      synchronized (target.getTreeLock()) {
        Insets insets = target.getInsets();
        int maxwidth = target.width - (insets.left + insets.right + hgap*2);
        int nmembers = target.getComponentCount();
        int x = 0, y = insets.top + vgap;
        int rowh = 0, start = 0;

        boolean ltr = target.getComponentOrientation().isLeftToRight();

        boolean useBaseline = getAlignOnBaseline();
        int[] ascent = null;
        int[] descent = null;

        if (useBaseline) {
            ascent = new int[nmembers];
            descent = new int[nmembers];
        }

        for (int i = 0 ; i < nmembers ; i++) {
            Component m = target.getComponent(i);
            if (m.isVisible()) {
                Dimension d = m.getPreferredSize();
                m.setSize(d.width, d.height);

                if (useBaseline) {
                    int baseline = m.getBaseline(d.width, d.height);
                    if (baseline >= 0) {
                        ascent[i] = baseline;
                        descent[i] = d.height - baseline;
                    }
                    else {
                        ascent[i] = -1;
                    }
                }
                if ((x == 0) || ((x + d.width) <= maxwidth)) {
                    if (x > 0) {
                        x += hgap;
                    }
                    x += d.width;
                    rowh = Math.max(rowh, d.height);
                } else {
                    rowh = moveComponents(target, insets.left + hgap, y,
                                   maxwidth - x, rowh, start, i, ltr,
                                   useBaseline, ascent, descent);
                    x = d.width;
                    y += vgap + rowh;
                    rowh = d.height;
                    start = i;
                }
            }
        }
        moveComponents(target, insets.left + hgap, y, maxwidth - x, rowh,
                       start, nmembers, ltr, useBaseline, ascent, descent);
      }
    }

    //
    // the internal serial version which says which version was written
    // - 0 (default) for versions before the Java 2 platform, v1.2
    // - 1 for version >= Java 2 platform v1.2, which includes "newAlign" field
    //
    private static final int currentSerialVersion = 1;
    /**
     * This represent the {@code currentSerialVersion}
     * which is bein used.  It will be one of two values:
     * {@code 0} versions before Java 2 platform v1.2,
     * {@code 1} versions after  Java 2 platform v1.2.
     *
     * @serial
     * @since 1.2
     */
    private int serialVersionOnStream = currentSerialVersion;

    /**
     * Reads this object out of a serialization stream, handling
     * objects written by older versions of the class that didn't contain all
     * of the fields we use now..
     *
     * @param  stream the {@code ObjectInputStream} to read
     * @throws ClassNotFoundException if the class of a serialized object could
     *         not be found
     * @throws IOException if an I/O error occurs
     */
    @Serial
    private void readObject(ObjectInputStream stream)
         throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();

        if (serialVersionOnStream < 1) {
            // "newAlign" field wasn't present, so use the old "align" field.
            setAlignment(this.align);
        }
        serialVersionOnStream = currentSerialVersion;
    }

    /**
     * Returns a string representation of this {@code FlowLayout}
     * object and its values.
     * @return     a string representation of this layout
     */
    public String toString() {
        String str = "";
        switch (align) {
          case LEFT:        str = ",align=left"; break;
          case CENTER:      str = ",align=center"; break;
          case RIGHT:       str = ",align=right"; break;
          case LEADING:     str = ",align=leading"; break;
          case TRAILING:    str = ",align=trailing"; break;
        }
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + str + "]";
    }


}
