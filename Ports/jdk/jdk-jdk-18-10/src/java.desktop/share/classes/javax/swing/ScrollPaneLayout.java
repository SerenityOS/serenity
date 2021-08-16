/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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


import javax.swing.border.*;

import java.awt.LayoutManager;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Insets;
import java.io.Serializable;


/**
 * The layout manager used by <code>JScrollPane</code>.
 * <code>JScrollPaneLayout</code> is
 * responsible for nine components: a viewport, two scrollbars,
 * a row header, a column header, and four "corner" components.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @see JScrollPane
 * @see JViewport
 *
 * @author Hans Muller
 * @since 1.2
 */
@SuppressWarnings("serial") // Same-version serialization only
public class ScrollPaneLayout
    implements LayoutManager, ScrollPaneConstants, Serializable
{

    /**
     * The scrollpane's viewport child.
     * Default is an empty <code>JViewport</code>.
     * @see JScrollPane#setViewport
     */
    protected JViewport viewport;


    /**
     * The scrollpane's vertical scrollbar child.
     * Default is a <code>JScrollBar</code>.
     * @see JScrollPane#setVerticalScrollBar
     */
    protected JScrollBar vsb;


    /**
     * The scrollpane's horizontal scrollbar child.
     * Default is a <code>JScrollBar</code>.
     * @see JScrollPane#setHorizontalScrollBar
     */
    protected JScrollBar hsb;


    /**
     * The row header child.  Default is <code>null</code>.
     * @see JScrollPane#setRowHeader
     */
    protected JViewport rowHead;


    /**
     * The column header child.  Default is <code>null</code>.
     * @see JScrollPane#setColumnHeader
     */
    protected JViewport colHead;


    /**
     * The component to display in the lower left corner.
     * Default is <code>null</code>.
     * @see JScrollPane#setCorner
     */
    protected Component lowerLeft;


    /**
     * The component to display in the lower right corner.
     * Default is <code>null</code>.
     * @see JScrollPane#setCorner
     */
    protected Component lowerRight;


    /**
     * The component to display in the upper left corner.
     * Default is <code>null</code>.
     * @see JScrollPane#setCorner
     */
    protected Component upperLeft;


    /**
     * The component to display in the upper right corner.
     * Default is <code>null</code>.
     * @see JScrollPane#setCorner
     */
    protected Component upperRight;


    /**
     * The display policy for the vertical scrollbar.
     * The default is <code>ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED</code>.
     * <p>
     * This field is obsolete, please use the <code>JScrollPane</code> field instead.
     *
     * @see JScrollPane#setVerticalScrollBarPolicy
     */
    protected int vsbPolicy = VERTICAL_SCROLLBAR_AS_NEEDED;


    /**
     * The display policy for the horizontal scrollbar.
     * The default is <code>ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED</code>.
     * <p>
     * This field is obsolete, please use the <code>JScrollPane</code> field instead.
     *
     * @see JScrollPane#setHorizontalScrollBarPolicy
     */
    protected int hsbPolicy = HORIZONTAL_SCROLLBAR_AS_NEEDED;

    /**
     * Constructs a {@code ScrollPaneLayout}.
     */
    public ScrollPaneLayout() {}

    /**
     * This method is invoked after the ScrollPaneLayout is set as the
     * LayoutManager of a <code>JScrollPane</code>.
     * It initializes all of the internal fields that
     * are ordinarily set by <code>addLayoutComponent</code>.  For example:
     * <pre>
     * ScrollPaneLayout mySPLayout = new ScrollPanelLayout() {
     *     public void layoutContainer(Container p) {
     *         super.layoutContainer(p);
     *         // do some extra work here ...
     *     }
     * };
     * scrollpane.setLayout(mySPLayout):
     * </pre>
     *
     * @param sp an instance of the {@code JScrollPane}
     */
    public void syncWithScrollPane(JScrollPane sp) {
        viewport = sp.getViewport();
        vsb = sp.getVerticalScrollBar();
        hsb = sp.getHorizontalScrollBar();
        rowHead = sp.getRowHeader();
        colHead = sp.getColumnHeader();
        lowerLeft = sp.getCorner(LOWER_LEFT_CORNER);
        lowerRight = sp.getCorner(LOWER_RIGHT_CORNER);
        upperLeft = sp.getCorner(UPPER_LEFT_CORNER);
        upperRight = sp.getCorner(UPPER_RIGHT_CORNER);
        vsbPolicy = sp.getVerticalScrollBarPolicy();
        hsbPolicy = sp.getHorizontalScrollBarPolicy();
    }


    /**
     * Removes an existing component.  When a new component, such as
     * the left corner, or vertical scrollbar, is added, the old one,
     * if it exists, must be removed.
     * <p>
     * This method returns <code>newC</code>. If <code>oldC</code> is
     * not equal to <code>newC</code> and is non-<code>null</code>,
     * it will be removed from its parent.
     *
     * @param oldC the <code>Component</code> to replace
     * @param newC the <code>Component</code> to add
     * @return the <code>newC</code>
     */
    protected Component addSingletonComponent(Component oldC, Component newC)
    {
        if ((oldC != null) && (oldC != newC)) {
            oldC.getParent().remove(oldC);
        }
        return newC;
    }


    /**
     * Adds the specified component to the layout. The layout is
     * identified using one of:
     * <ul>
     * <li>ScrollPaneConstants.VIEWPORT
     * <li>ScrollPaneConstants.VERTICAL_SCROLLBAR
     * <li>ScrollPaneConstants.HORIZONTAL_SCROLLBAR
     * <li>ScrollPaneConstants.ROW_HEADER
     * <li>ScrollPaneConstants.COLUMN_HEADER
     * <li>ScrollPaneConstants.LOWER_LEFT_CORNER
     * <li>ScrollPaneConstants.LOWER_RIGHT_CORNER
     * <li>ScrollPaneConstants.UPPER_LEFT_CORNER
     * <li>ScrollPaneConstants.UPPER_RIGHT_CORNER
     * </ul>
     *
     * @param s the component identifier
     * @param c the component to be added
     * @exception IllegalArgumentException if <code>s</code> is an invalid key
     */
    public void addLayoutComponent(String s, Component c)
    {
        if (s.equals(VIEWPORT)) {
            viewport = (JViewport)addSingletonComponent(viewport, c);
        }
        else if (s.equals(VERTICAL_SCROLLBAR)) {
            vsb = (JScrollBar)addSingletonComponent(vsb, c);
        }
        else if (s.equals(HORIZONTAL_SCROLLBAR)) {
            hsb = (JScrollBar)addSingletonComponent(hsb, c);
        }
        else if (s.equals(ROW_HEADER)) {
            rowHead = (JViewport)addSingletonComponent(rowHead, c);
        }
        else if (s.equals(COLUMN_HEADER)) {
            colHead = (JViewport)addSingletonComponent(colHead, c);
        }
        else if (s.equals(LOWER_LEFT_CORNER)) {
            lowerLeft = addSingletonComponent(lowerLeft, c);
        }
        else if (s.equals(LOWER_RIGHT_CORNER)) {
            lowerRight = addSingletonComponent(lowerRight, c);
        }
        else if (s.equals(UPPER_LEFT_CORNER)) {
            upperLeft = addSingletonComponent(upperLeft, c);
        }
        else if (s.equals(UPPER_RIGHT_CORNER)) {
            upperRight = addSingletonComponent(upperRight, c);
        }
        else {
            throw new IllegalArgumentException("invalid layout key " + s);
        }
    }


    /**
     * Removes the specified component from the layout.
     *
     * @param c the component to remove
     */
    public void removeLayoutComponent(Component c)
    {
        if (c == viewport) {
            viewport = null;
        }
        else if (c == vsb) {
            vsb = null;
        }
        else if (c == hsb) {
            hsb = null;
        }
        else if (c == rowHead) {
            rowHead = null;
        }
        else if (c == colHead) {
            colHead = null;
        }
        else if (c == lowerLeft) {
            lowerLeft = null;
        }
        else if (c == lowerRight) {
            lowerRight = null;
        }
        else if (c == upperLeft) {
            upperLeft = null;
        }
        else if (c == upperRight) {
            upperRight = null;
        }
    }


    /**
     * Returns the vertical scrollbar-display policy.
     *
     * @return an integer giving the display policy
     * @see #setVerticalScrollBarPolicy
     */
    public int getVerticalScrollBarPolicy() {
        return vsbPolicy;
    }


    /**
     * Sets the vertical scrollbar-display policy. The options
     * are:
     * <ul>
     * <li>ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
     * <li>ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
     * <li>ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
     * </ul>
     * Note: Applications should use the <code>JScrollPane</code> version
     * of this method.  It only exists for backwards compatibility
     * with the Swing 1.0.2 (and earlier) versions of this class.
     *
     * @param x an integer giving the display policy
     * @exception IllegalArgumentException if <code>x</code> is an invalid
     *          vertical scroll bar policy, as listed above
     */
    public void setVerticalScrollBarPolicy(int x) {
        switch (x) {
        case VERTICAL_SCROLLBAR_AS_NEEDED:
        case VERTICAL_SCROLLBAR_NEVER:
        case VERTICAL_SCROLLBAR_ALWAYS:
                vsbPolicy = x;
                break;
        default:
            throw new IllegalArgumentException("invalid verticalScrollBarPolicy");
        }
    }


    /**
     * Returns the horizontal scrollbar-display policy.
     *
     * @return an integer giving the display policy
     * @see #setHorizontalScrollBarPolicy
     */
    public int getHorizontalScrollBarPolicy() {
        return hsbPolicy;
    }

    /**
     * Sets the horizontal scrollbar-display policy.
     * The options are:<ul>
     * <li>ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
     * <li>ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
     * <li>ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
     * </ul>
     * Note: Applications should use the <code>JScrollPane</code> version
     * of this method.  It only exists for backwards compatibility
     * with the Swing 1.0.2 (and earlier) versions of this class.
     *
     * @param x an int giving the display policy
     * @exception IllegalArgumentException if <code>x</code> is not a valid
     *          horizontal scrollbar policy, as listed above
     */
    public void setHorizontalScrollBarPolicy(int x) {
        switch (x) {
        case HORIZONTAL_SCROLLBAR_AS_NEEDED:
        case HORIZONTAL_SCROLLBAR_NEVER:
        case HORIZONTAL_SCROLLBAR_ALWAYS:
                hsbPolicy = x;
                break;
        default:
            throw new IllegalArgumentException("invalid horizontalScrollBarPolicy");
        }
    }


    /**
     * Returns the <code>JViewport</code> object that displays the
     * scrollable contents.
     * @return the <code>JViewport</code> object that displays the scrollable contents
     * @see JScrollPane#getViewport
     */
    public JViewport getViewport() {
        return viewport;
    }


    /**
     * Returns the <code>JScrollBar</code> object that handles horizontal scrolling.
     * @return the <code>JScrollBar</code> object that handles horizontal scrolling
     * @see JScrollPane#getHorizontalScrollBar
     */
    public JScrollBar getHorizontalScrollBar() {
        return hsb;
    }

    /**
     * Returns the <code>JScrollBar</code> object that handles vertical scrolling.
     * @return the <code>JScrollBar</code> object that handles vertical scrolling
     * @see JScrollPane#getVerticalScrollBar
     */
    public JScrollBar getVerticalScrollBar() {
        return vsb;
    }


    /**
     * Returns the <code>JViewport</code> object that is the row header.
     * @return the <code>JViewport</code> object that is the row header
     * @see JScrollPane#getRowHeader
     */
    public JViewport getRowHeader() {
        return rowHead;
    }


    /**
     * Returns the <code>JViewport</code> object that is the column header.
     * @return the <code>JViewport</code> object that is the column header
     * @see JScrollPane#getColumnHeader
     */
    public JViewport getColumnHeader() {
        return colHead;
    }


    /**
     * Returns the <code>Component</code> at the specified corner.
     * @param key the <code>String</code> specifying the corner
     * @return the <code>Component</code> at the specified corner, as defined in
     *         {@link ScrollPaneConstants}; if <code>key</code> is not one of the
     *          four corners, <code>null</code> is returned
     * @see JScrollPane#getCorner
     */
    public Component getCorner(String key) {
        if (key.equals(LOWER_LEFT_CORNER)) {
            return lowerLeft;
        }
        else if (key.equals(LOWER_RIGHT_CORNER)) {
            return lowerRight;
        }
        else if (key.equals(UPPER_LEFT_CORNER)) {
            return upperLeft;
        }
        else if (key.equals(UPPER_RIGHT_CORNER)) {
            return upperRight;
        }
        else {
            return null;
        }
    }


    /**
     * The preferred size of a <code>ScrollPane</code> is the size of the insets,
     * plus the preferred size of the viewport, plus the preferred size of
     * the visible headers, plus the preferred size of the scrollbars
     * that will appear given the current view and the current
     * scrollbar displayPolicies.
     * <p>Note that the rowHeader is calculated as part of the preferred width
     * and the colHeader is calculated as part of the preferred size.
     *
     * @param parent the <code>Container</code> that will be laid out
     * @return a <code>Dimension</code> object specifying the preferred size of the
     *         viewport and any scrollbars
     * @see ViewportLayout
     * @see LayoutManager
     */
    public Dimension preferredLayoutSize(Container parent)
    {
        /* Sync the (now obsolete) policy fields with the
         * JScrollPane.
         */
        JScrollPane scrollPane = (JScrollPane)parent;
        vsbPolicy = scrollPane.getVerticalScrollBarPolicy();
        hsbPolicy = scrollPane.getHorizontalScrollBarPolicy();

        Insets insets = parent.getInsets();
        int prefWidth = insets.left + insets.right;
        int prefHeight = insets.top + insets.bottom;

        /* Note that viewport.getViewSize() is equivalent to
         * viewport.getView().getPreferredSize() modulo a null
         * view or a view whose size was explicitly set.
         */

        Dimension extentSize = null;
        Dimension viewSize = null;
        Component view = null;

        if (viewport != null) {
            extentSize = viewport.getPreferredSize();
            view = viewport.getView();
            if (view != null) {
                viewSize = view.getPreferredSize();
            } else {
                viewSize = new Dimension(0, 0);
            }
        }

        /* If there's a viewport add its preferredSize.
         */

        if (extentSize != null) {
            prefWidth += extentSize.width;
            prefHeight += extentSize.height;
        }

        /* If there's a JScrollPane.viewportBorder, add its insets.
         */

        Border viewportBorder = scrollPane.getViewportBorder();
        if (viewportBorder != null) {
            Insets vpbInsets = viewportBorder.getBorderInsets(parent);
            prefWidth += vpbInsets.left + vpbInsets.right;
            prefHeight += vpbInsets.top + vpbInsets.bottom;
        }

        /* If a header exists and it's visible, factor its
         * preferred size in.
         */

        if ((rowHead != null) && rowHead.isVisible()) {
            prefWidth += rowHead.getPreferredSize().width;
        }

        if ((colHead != null) && colHead.isVisible()) {
            prefHeight += colHead.getPreferredSize().height;
        }

        /* If a scrollbar is going to appear, factor its preferred size in.
         * If the scrollbars policy is AS_NEEDED, this can be a little
         * tricky:
         *
         * - If the view is a Scrollable then scrollableTracksViewportWidth
         * and scrollableTracksViewportHeight can be used to effectively
         * disable scrolling (if they're true) in their respective dimensions.
         *
         * - Assuming that a scrollbar hasn't been disabled by the
         * previous constraint, we need to decide if the scrollbar is going
         * to appear to correctly compute the JScrollPanes preferred size.
         * To do this we compare the preferredSize of the viewport (the
         * extentSize) to the preferredSize of the view.  Although we're
         * not responsible for laying out the view we'll assume that the
         * JViewport will always give it its preferredSize.
         */

        if ((vsb != null) && (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {
            if (vsbPolicy == VERTICAL_SCROLLBAR_ALWAYS) {
                prefWidth += vsb.getPreferredSize().width;
            }
            else if ((viewSize != null) && (extentSize != null)) {
                boolean canScroll = true;
                if (view instanceof Scrollable) {
                    canScroll = !((Scrollable)view).getScrollableTracksViewportHeight();
                }
                if (canScroll && (viewSize.height > extentSize.height)) {
                    prefWidth += vsb.getPreferredSize().width;
                }
            }
        }

        if ((hsb != null) && (hsbPolicy != HORIZONTAL_SCROLLBAR_NEVER)) {
            if (hsbPolicy == HORIZONTAL_SCROLLBAR_ALWAYS) {
                prefHeight += hsb.getPreferredSize().height;
            }
            else if ((viewSize != null) && (extentSize != null)) {
                boolean canScroll = true;
                if (view instanceof Scrollable) {
                    canScroll = !((Scrollable)view).getScrollableTracksViewportWidth();
                }
                if (canScroll && (viewSize.width > extentSize.width)) {
                    prefHeight += hsb.getPreferredSize().height;
                }
            }
        }

        return new Dimension(prefWidth, prefHeight);
    }


    /**
     * The minimum size of a <code>ScrollPane</code> is the size of the insets
     * plus minimum size of the viewport, plus the scrollpane's
     * viewportBorder insets, plus the minimum size
     * of the visible headers, plus the minimum size of the
     * scrollbars whose displayPolicy isn't NEVER.
     *
     * @param parent the <code>Container</code> that will be laid out
     * @return a <code>Dimension</code> object specifying the minimum size
     */
    public Dimension minimumLayoutSize(Container parent)
    {
        /* Sync the (now obsolete) policy fields with the
         * JScrollPane.
         */
        JScrollPane scrollPane = (JScrollPane)parent;
        vsbPolicy = scrollPane.getVerticalScrollBarPolicy();
        hsbPolicy = scrollPane.getHorizontalScrollBarPolicy();

        Insets insets = parent.getInsets();
        int minWidth = insets.left + insets.right;
        int minHeight = insets.top + insets.bottom;

        /* If there's a viewport add its minimumSize.
         */

        if (viewport != null) {
            Dimension size = viewport.getMinimumSize();
            minWidth += size.width;
            minHeight += size.height;
        }

        /* If there's a JScrollPane.viewportBorder, add its insets.
         */

        Border viewportBorder = scrollPane.getViewportBorder();
        if (viewportBorder != null) {
            Insets vpbInsets = viewportBorder.getBorderInsets(parent);
            minWidth += vpbInsets.left + vpbInsets.right;
            minHeight += vpbInsets.top + vpbInsets.bottom;
        }

        /* If a header exists and it's visible, factor its
         * minimum size in.
         */

        if ((rowHead != null) && rowHead.isVisible()) {
            Dimension size = rowHead.getMinimumSize();
            minWidth += size.width;
            minHeight = Math.max(minHeight, size.height);
        }

        if ((colHead != null) && colHead.isVisible()) {
            Dimension size = colHead.getMinimumSize();
            minWidth = Math.max(minWidth, size.width);
            minHeight += size.height;
        }

        /* If a scrollbar might appear, factor its minimum
         * size in.
         */

        if ((vsb != null) && (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {
            Dimension size = vsb.getMinimumSize();
            minWidth += size.width;
            minHeight = Math.max(minHeight, size.height);
        }

        if ((hsb != null) && (hsbPolicy != HORIZONTAL_SCROLLBAR_NEVER)) {
            Dimension size = hsb.getMinimumSize();
            minWidth = Math.max(minWidth, size.width);
            minHeight += size.height;
        }

        return new Dimension(minWidth, minHeight);
    }


    /**
     * Lays out the scrollpane. The positioning of components depends on
     * the following constraints:
     * <ul>
     * <li> The row header, if present and visible, gets its preferred
     * width and the viewport's height.
     *
     * <li> The column header, if present and visible, gets its preferred
     * height and the viewport's width.
     *
     * <li> If a vertical scrollbar is needed, i.e. if the viewport's extent
     * height is smaller than its view height or if the <code>displayPolicy</code>
     * is ALWAYS, it's treated like the row header with respect to its
     * dimensions and is made visible.
     *
     * <li> If a horizontal scrollbar is needed, it is treated like the
     * column header (see the paragraph above regarding the vertical scrollbar).
     *
     * <li> If the scrollpane has a non-<code>null</code>
     * <code>viewportBorder</code>, then space is allocated for that.
     *
     * <li> The viewport gets the space available after accounting for
     * the previous constraints.
     *
     * <li> The corner components, if provided, are aligned with the
     * ends of the scrollbars and headers. If there is a vertical
     * scrollbar, the right corners appear; if there is a horizontal
     * scrollbar, the lower corners appear; a row header gets left
     * corners, and a column header gets upper corners.
     * </ul>
     *
     * @param parent the <code>Container</code> to lay out
     */
    public void layoutContainer(Container parent)
    {
        /* Sync the (now obsolete) policy fields with the
         * JScrollPane.
         */
        JScrollPane scrollPane = (JScrollPane)parent;
        vsbPolicy = scrollPane.getVerticalScrollBarPolicy();
        hsbPolicy = scrollPane.getHorizontalScrollBarPolicy();

        Rectangle availR = scrollPane.getBounds();
        availR.x = availR.y = 0;

        Insets insets = parent.getInsets();
        availR.x = insets.left;
        availR.y = insets.top;
        availR.width -= insets.left + insets.right;
        availR.height -= insets.top + insets.bottom;

        /* Get the scrollPane's orientation.
         */
        boolean leftToRight = SwingUtilities.isLeftToRight(scrollPane);

        /* If there's a visible column header remove the space it
         * needs from the top of availR.  The column header is treated
         * as if it were fixed height, arbitrary width.
         */

        Rectangle colHeadR = new Rectangle(0, availR.y, 0, 0);

        if ((colHead != null) && (colHead.isVisible())) {
            int colHeadHeight = Math.min(availR.height,
                                         colHead.getPreferredSize().height);
            colHeadR.height = colHeadHeight;
            availR.y += colHeadHeight;
            availR.height -= colHeadHeight;
        }

        /* If there's a visible row header remove the space it needs
         * from the left or right of availR.  The row header is treated
         * as if it were fixed width, arbitrary height.
         */

        Rectangle rowHeadR = new Rectangle(0, 0, 0, 0);

        if ((rowHead != null) && (rowHead.isVisible())) {
            int rowHeadWidth = Math.min(availR.width,
                                        rowHead.getPreferredSize().width);
            rowHeadR.width = rowHeadWidth;
            availR.width -= rowHeadWidth;
            if ( leftToRight ) {
                rowHeadR.x = availR.x;
                availR.x += rowHeadWidth;
            } else {
                rowHeadR.x = availR.x + availR.width;
            }
        }

        /* If there's a JScrollPane.viewportBorder, remove the
         * space it occupies for availR.
         */

        Border viewportBorder = scrollPane.getViewportBorder();
        Insets vpbInsets;
        if (viewportBorder != null) {
            vpbInsets = viewportBorder.getBorderInsets(parent);
            availR.x += vpbInsets.left;
            availR.y += vpbInsets.top;
            availR.width -= vpbInsets.left + vpbInsets.right;
            availR.height -= vpbInsets.top + vpbInsets.bottom;
        }
        else {
            vpbInsets = new Insets(0,0,0,0);
        }


        /* At this point availR is the space available for the viewport
         * and scrollbars. rowHeadR is correct except for its height and y
         * and colHeadR is correct except for its width and x.  Once we're
         * through computing the dimensions  of these three parts we can
         * go back and set the dimensions of rowHeadR.height, rowHeadR.y,
         * colHeadR.width, colHeadR.x and the bounds for the corners.
         *
         * We'll decide about putting up scrollbars by comparing the
         * viewport views preferred size with the viewports extent
         * size (generally just its size).  Using the preferredSize is
         * reasonable because layout proceeds top down - so we expect
         * the viewport to be laid out next.  And we assume that the
         * viewports layout manager will give the view it's preferred
         * size.  One exception to this is when the view implements
         * Scrollable and Scrollable.getViewTracksViewport{Width,Height}
         * methods return true.  If the view is tracking the viewports
         * width we don't bother with a horizontal scrollbar, similarly
         * if view.getViewTracksViewport(Height) is true we don't bother
         * with a vertical scrollbar.
         */

        Component view = (viewport != null) ? viewport.getView() : null;
        Dimension viewPrefSize =
            (view != null) ? view.getPreferredSize()
                           : new Dimension(0,0);

        Dimension extentSize =
            (viewport != null) ? viewport.toViewCoordinates(availR.getSize())
                               : new Dimension(0,0);

        boolean viewTracksViewportWidth = false;
        boolean viewTracksViewportHeight = false;
        boolean isEmpty = (availR.width < 0 || availR.height < 0);
        Scrollable sv;
        // Don't bother checking the Scrollable methods if there is no room
        // for the viewport, we aren't going to show any scrollbars in this
        // case anyway.
        if (!isEmpty && view instanceof Scrollable) {
            sv = (Scrollable)view;
            viewTracksViewportWidth = sv.getScrollableTracksViewportWidth();
            viewTracksViewportHeight = sv.getScrollableTracksViewportHeight();
        }
        else {
            sv = null;
        }

        /* If there's a vertical scrollbar and we need one, allocate
         * space for it (we'll make it visible later). A vertical
         * scrollbar is considered to be fixed width, arbitrary height.
         */

        Rectangle vsbR = new Rectangle(0, availR.y - vpbInsets.top, 0, 0);

        boolean vsbNeeded;
        if (vsbPolicy == VERTICAL_SCROLLBAR_ALWAYS) {
            vsbNeeded = true;
        }
        else if (vsbPolicy == VERTICAL_SCROLLBAR_NEVER) {
            vsbNeeded = false;
        }
        else {  // vsbPolicy == VERTICAL_SCROLLBAR_AS_NEEDED
            vsbNeeded = !viewTracksViewportHeight && (viewPrefSize.height > extentSize.height);
        }


        if ((vsb != null) && vsbNeeded) {
            adjustForVSB(true, availR, vsbR, vpbInsets, leftToRight);
            extentSize = viewport.toViewCoordinates(availR.getSize());
        }

        /* If there's a horizontal scrollbar and we need one, allocate
         * space for it (we'll make it visible later). A horizontal
         * scrollbar is considered to be fixed height, arbitrary width.
         */

        Rectangle hsbR = new Rectangle(availR.x - vpbInsets.left, 0, 0, 0);
        boolean hsbNeeded;
        if (hsbPolicy == HORIZONTAL_SCROLLBAR_ALWAYS) {
            hsbNeeded = true;
        }
        else if (hsbPolicy == HORIZONTAL_SCROLLBAR_NEVER) {
            hsbNeeded = false;
        }
        else {  // hsbPolicy == HORIZONTAL_SCROLLBAR_AS_NEEDED
            hsbNeeded = !viewTracksViewportWidth && (viewPrefSize.width > extentSize.width);
        }

        if ((hsb != null) && hsbNeeded) {
            adjustForHSB(true, availR, hsbR, vpbInsets);

            /* If we added the horizontal scrollbar then we've implicitly
             * reduced  the vertical space available to the viewport.
             * As a consequence we may have to add the vertical scrollbar,
             * if that hasn't been done so already.  Of course we
             * don't bother with any of this if the vsbPolicy is NEVER.
             */
            if ((vsb != null) && !vsbNeeded &&
                (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {

                extentSize = viewport.toViewCoordinates(availR.getSize());
                vsbNeeded = viewPrefSize.height > extentSize.height;

                if (vsbNeeded) {
                    adjustForVSB(true, availR, vsbR, vpbInsets, leftToRight);
                }
            }
        }

        /* Set the size of the viewport first, and then recheck the Scrollable
         * methods. Some components base their return values for the Scrollable
         * methods on the size of the Viewport, so that if we don't
         * ask after resetting the bounds we may have gotten the wrong
         * answer.
         */

        if (viewport != null) {
            viewport.setBounds(availR);

            if (sv != null) {
                extentSize = viewport.toViewCoordinates(availR.getSize());

                boolean oldHSBNeeded = hsbNeeded;
                boolean oldVSBNeeded = vsbNeeded;
                viewTracksViewportWidth = sv.
                                          getScrollableTracksViewportWidth();
                viewTracksViewportHeight = sv.
                                          getScrollableTracksViewportHeight();
                if (vsb != null && vsbPolicy == VERTICAL_SCROLLBAR_AS_NEEDED) {
                    boolean newVSBNeeded = !viewTracksViewportHeight &&
                                     (viewPrefSize.height > extentSize.height);
                    if (newVSBNeeded != vsbNeeded) {
                        vsbNeeded = newVSBNeeded;
                        adjustForVSB(vsbNeeded, availR, vsbR, vpbInsets,
                                     leftToRight);
                        extentSize = viewport.toViewCoordinates
                                              (availR.getSize());
                    }
                }
                if (hsb != null && hsbPolicy ==HORIZONTAL_SCROLLBAR_AS_NEEDED){
                    boolean newHSBbNeeded = !viewTracksViewportWidth &&
                                       (viewPrefSize.width > extentSize.width);
                    if (newHSBbNeeded != hsbNeeded) {
                        hsbNeeded = newHSBbNeeded;
                        adjustForHSB(hsbNeeded, availR, hsbR, vpbInsets);
                        if ((vsb != null) && !vsbNeeded &&
                            (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {

                            extentSize = viewport.toViewCoordinates
                                         (availR.getSize());
                            vsbNeeded = viewPrefSize.height >
                                        extentSize.height;

                            if (vsbNeeded) {
                                adjustForVSB(true, availR, vsbR, vpbInsets,
                                             leftToRight);
                            }
                        }
                    }
                }
                if (oldHSBNeeded != hsbNeeded ||
                    oldVSBNeeded != vsbNeeded) {
                    viewport.setBounds(availR);
                    // You could argue that we should recheck the
                    // Scrollable methods again until they stop changing,
                    // but they might never stop changing, so we stop here
                    // and don't do any additional checks.
                }
            }
        }

        /* We now have the final size of the viewport: availR.
         * Now fixup the header and scrollbar widths/heights.
         */
        vsbR.height = availR.height + vpbInsets.top + vpbInsets.bottom;
        hsbR.width = availR.width + vpbInsets.left + vpbInsets.right;
        rowHeadR.height = availR.height + vpbInsets.top + vpbInsets.bottom;
        rowHeadR.y = availR.y - vpbInsets.top;
        colHeadR.width = availR.width + vpbInsets.left + vpbInsets.right;
        colHeadR.x = availR.x - vpbInsets.left;

        /* Set the bounds of the remaining components.  The scrollbars
         * are made invisible if they're not needed.
         */

        if (rowHead != null) {
            rowHead.setBounds(rowHeadR);
        }

        if (colHead != null) {
            colHead.setBounds(colHeadR);
        }

        if (vsb != null) {
            if (vsbNeeded) {
                if (colHead != null &&
                    UIManager.getBoolean("ScrollPane.fillUpperCorner"))
                {
                    if ((leftToRight && upperRight == null) ||
                        (!leftToRight && upperLeft == null))
                    {
                        // This is used primarily for GTK L&F, which needs to
                        // extend the vertical scrollbar to fill the upper
                        // corner near the column header.  Note that we skip
                        // this step (and use the default behavior) if the
                        // user has set a custom corner component.
                        vsbR.y = colHeadR.y;
                        vsbR.height += colHeadR.height;
                    }
                }
                vsb.setVisible(true);
                vsb.setBounds(vsbR);
            }
            else {
                vsb.setVisible(false);
            }
        }

        if (hsb != null) {
            if (hsbNeeded) {
                if (rowHead != null &&
                    UIManager.getBoolean("ScrollPane.fillLowerCorner"))
                {
                    if ((leftToRight && lowerLeft == null) ||
                        (!leftToRight && lowerRight == null))
                    {
                        // This is used primarily for GTK L&F, which needs to
                        // extend the horizontal scrollbar to fill the lower
                        // corner near the row header.  Note that we skip
                        // this step (and use the default behavior) if the
                        // user has set a custom corner component.
                        if (leftToRight) {
                            hsbR.x = rowHeadR.x;
                        }
                        hsbR.width += rowHeadR.width;
                    }
                }
                hsb.setVisible(true);
                hsb.setBounds(hsbR);
            }
            else {
                hsb.setVisible(false);
            }
        }

        if (lowerLeft != null) {
            lowerLeft.setBounds(leftToRight ? rowHeadR.x : vsbR.x,
                                hsbR.y,
                                leftToRight ? rowHeadR.width : vsbR.width,
                                hsbR.height);
        }

        if (lowerRight != null) {
            lowerRight.setBounds(leftToRight ? vsbR.x : rowHeadR.x,
                                 hsbR.y,
                                 leftToRight ? vsbR.width : rowHeadR.width,
                                 hsbR.height);
        }

        if (upperLeft != null) {
            upperLeft.setBounds(leftToRight ? rowHeadR.x : vsbR.x,
                                colHeadR.y,
                                leftToRight ? rowHeadR.width : vsbR.width,
                                colHeadR.height);
        }

        if (upperRight != null) {
            upperRight.setBounds(leftToRight ? vsbR.x : rowHeadR.x,
                                 colHeadR.y,
                                 leftToRight ? vsbR.width : rowHeadR.width,
                                 colHeadR.height);
        }
    }

    /**
     * Adjusts the <code>Rectangle</code> <code>available</code> based on if
     * the vertical scrollbar is needed (<code>wantsVSB</code>).
     * The location of the vsb is updated in <code>vsbR</code>, and
     * the viewport border insets (<code>vpbInsets</code>) are used to offset
     * the vsb. This is only called when <code>wantsVSB</code> has
     * changed, eg you shouldn't invoke adjustForVSB(true) twice.
     */
    private void adjustForVSB(boolean wantsVSB, Rectangle available,
                              Rectangle vsbR, Insets vpbInsets,
                              boolean leftToRight) {
        int oldWidth = vsbR.width;
        if (wantsVSB) {
            int vsbWidth = Math.max(0, Math.min(vsb.getPreferredSize().width,
                                                available.width));

            available.width -= vsbWidth;
            vsbR.width = vsbWidth;

            if( leftToRight ) {
                vsbR.x = available.x + available.width + vpbInsets.right;
            } else {
                vsbR.x = available.x - vpbInsets.left;
                available.x += vsbWidth;
            }
        }
        else {
            available.width += oldWidth;
        }
    }

    /**
     * Adjusts the <code>Rectangle</code> <code>available</code> based on if
     * the horizontal scrollbar is needed (<code>wantsHSB</code>).
     * The location of the hsb is updated in <code>hsbR</code>, and
     * the viewport border insets (<code>vpbInsets</code>) are used to offset
     * the hsb.  This is only called when <code>wantsHSB</code> has
     * changed, eg you shouldn't invoked adjustForHSB(true) twice.
     */
    private void adjustForHSB(boolean wantsHSB, Rectangle available,
                              Rectangle hsbR, Insets vpbInsets) {
        int oldHeight = hsbR.height;
        if (wantsHSB) {
            int hsbHeight = Math.max(0, Math.min(available.height,
                                              hsb.getPreferredSize().height));

            available.height -= hsbHeight;
            hsbR.y = available.y + available.height + vpbInsets.bottom;
            hsbR.height = hsbHeight;
        }
        else {
            available.height += oldHeight;
        }
    }



    /**
     * Returns the bounds of the border around the specified scroll pane's
     * viewport.
     *
     * @param scrollpane an instance of the {@code JScrollPane}
     * @return the size and position of the viewport border
     * @deprecated As of JDK version Swing1.1
     *    replaced by <code>JScrollPane.getViewportBorderBounds()</code>.
     */
    @Deprecated
    public Rectangle getViewportBorderBounds(JScrollPane scrollpane) {
        return scrollpane.getViewportBorderBounds();
    }

    /**
     * The UI resource version of <code>ScrollPaneLayout</code>.
     */
    public static class UIResource extends ScrollPaneLayout implements javax.swing.plaf.UIResource {
        /**
         * Constructs a {@code UIResource}.
         */
        public UIResource() {}
    }
}
