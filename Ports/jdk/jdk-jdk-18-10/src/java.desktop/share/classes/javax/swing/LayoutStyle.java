/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Container;
import javax.swing.plaf.ComponentUI;
import sun.awt.AppContext;

/**
 * <code>LayoutStyle</code> provides information about how to position
 * components.  This class is primarily useful for visual tools and
 * layout managers.  Most developers will not need to use this class.
 * <p>
 * You typically don't set or create a
 * <code>LayoutStyle</code>.  Instead use the static method
 * <code>getInstance</code> to obtain the current instance.
 *
 * @since 1.6
 */
public abstract class LayoutStyle {
    /**
     * Sets the shared instance of <code>LayoutStyle</code>.  Specifying
     * <code>null</code> results in using the <code>LayoutStyle</code> from
     * the current <code>LookAndFeel</code>.
     *
     * @param style the <code>LayoutStyle</code>, or <code>null</code>
     * @see #getInstance
     */
    public static void setInstance(LayoutStyle style) {
        synchronized(LayoutStyle.class) {
            if (style == null) {
                AppContext.getAppContext().remove(LayoutStyle.class);
            }
            else {
                AppContext.getAppContext().put(LayoutStyle.class, style);
            }
        }
    }

    /**
     * Returns the shared instance of <code>LayoutStyle</code>.  If an instance
     * has not been specified in <code>setInstance</code>, this will return
     * the <code>LayoutStyle</code> from the current <code>LookAndFeel</code>.
     *
     * @see LookAndFeel#getLayoutStyle
     * @return the shared instance of <code>LayoutStyle</code>
     */
    public static LayoutStyle getInstance() {
        LayoutStyle style;
        synchronized(LayoutStyle.class) {
            style = (LayoutStyle)AppContext.getAppContext().
                    get(LayoutStyle.class);
        }
        if (style == null) {
            return UIManager.getLookAndFeel().getLayoutStyle();
        }
        return style;
    }


    /**
     * <code>ComponentPlacement</code> is an enumeration of the
     * possible ways two components can be placed relative to each
     * other.  <code>ComponentPlacement</code> is used by the
     * <code>LayoutStyle</code> method <code>getPreferredGap</code>.  Refer to
     * <code>LayoutStyle</code> for more information.
     *
     * @see LayoutStyle#getPreferredGap(JComponent,JComponent,
     *      ComponentPlacement,int,Container)
     * @since 1.6
     */
    public enum ComponentPlacement {
        /**
         * Enumeration value indicating the two components are
         * visually related and will be placed in the same parent.
         * For example, a <code>JLabel</code> providing a label for a
         * <code>JTextField</code> is typically visually associated
         * with the <code>JTextField</code>; the constant <code>RELATED</code>
         * is used for this.
         */
        RELATED,

        /**
         * Enumeration value indicating the two components are
         * visually unrelated and will be placed in the same parent.
         * For example, groupings of components are usually visually
         * separated; the constant <code>UNRELATED</code> is used for this.
         */
        UNRELATED,

        /**
         * Enumeration value indicating the distance to indent a component
         * is being requested.  For example, often times the children of
         * a label will be horizontally indented from the label.  To determine
         * the preferred distance for such a gap use the
         * <code>INDENT</code> type.
         * <p>
         * This value is typically only useful with a direction of
         * <code>EAST</code> or <code>WEST</code>.
         */
        INDENT;
    }


    /**
     * Creates a new <code>LayoutStyle</code>.  You generally don't
     * create a <code>LayoutStyle</code>.  Instead use the method
     * <code>getInstance</code> to obtain the current
     * <code>LayoutStyle</code>.
     */
    public LayoutStyle() {
    }

    /**
     * Returns the amount of space to use between two components.
     * The return value indicates the distance to place
     * <code>component2</code> relative to <code>component1</code>.
     * For example, the following returns the amount of space to place
     * between <code>component2</code> and <code>component1</code>
     * when <code>component2</code> is placed vertically above
     * <code>component1</code>:
     * <pre>
     *   int gap = getPreferredGap(component1, component2,
     *                             ComponentPlacement.RELATED,
     *                             SwingConstants.NORTH, parent);
     * </pre>
     * The <code>type</code> parameter indicates the relation between
     * the two components.  If the two components will be contained in
     * the same parent and are showing similar logically related
     * items, use <code>RELATED</code>.  If the two components will be
     * contained in the same parent but show logically unrelated items
     * use <code>UNRELATED</code>.  Some look and feels may not
     * distinguish between the <code>RELATED</code> and
     * <code>UNRELATED</code> types.
     * <p>
     * The return value is not intended to take into account the
     * current size and position of <code>component2</code> or
     * <code>component1</code>.  The return value may take into
     * consideration various properties of the components.  For
     * example, the space may vary based on font size, or the preferred
     * size of the component.
     *
     * @param component1 the <code>JComponent</code>
     *               <code>component2</code> is being placed relative to
     * @param component2 the <code>JComponent</code> being placed
     * @param position the position <code>component2</code> is being placed
     *        relative to <code>component1</code>; one of
     *        <code>SwingConstants.NORTH</code>,
     *        <code>SwingConstants.SOUTH</code>,
     *        <code>SwingConstants.EAST</code> or
     *        <code>SwingConstants.WEST</code>
     * @param type how the two components are being placed
     * @param parent the parent of <code>component2</code>; this may differ
     *        from the actual parent and it may be <code>null</code>
     * @return the amount of space to place between the two components
     * @throws NullPointerException if <code>component1</code>,
     *         <code>component2</code> or <code>type</code> is
     *         <code>null</code>
     * @throws IllegalArgumentException if <code>position</code> is not
     *         one of <code>SwingConstants.NORTH</code>,
     *         <code>SwingConstants.SOUTH</code>,
     *         <code>SwingConstants.EAST</code> or
     *         <code>SwingConstants.WEST</code>
     * @see LookAndFeel#getLayoutStyle
     * @since 1.6
     */
    public abstract int getPreferredGap(JComponent component1,
                                        JComponent component2,
                                        ComponentPlacement type, int position,
                                        Container parent);

    /**
     * Returns the amount of space to place between the component and specified
     * edge of its parent.
     *
     * @param component the <code>JComponent</code> being positioned
     * @param position the position <code>component</code> is being placed
     *        relative to its parent; one of
     *        <code>SwingConstants.NORTH</code>,
     *        <code>SwingConstants.SOUTH</code>,
     *        <code>SwingConstants.EAST</code> or
     *        <code>SwingConstants.WEST</code>
     * @param parent the parent of <code>component</code>; this may differ
     *        from the actual parent and may be <code>null</code>
     * @return the amount of space to place between the component and specified
     *         edge
     * @throws IllegalArgumentException if <code>position</code> is not
     *         one of <code>SwingConstants.NORTH</code>,
     *         <code>SwingConstants.SOUTH</code>,
     *         <code>SwingConstants.EAST</code> or
     *         <code>SwingConstants.WEST</code>
     */
    public abstract int getContainerGap(JComponent component, int position,
                                        Container parent);
}
