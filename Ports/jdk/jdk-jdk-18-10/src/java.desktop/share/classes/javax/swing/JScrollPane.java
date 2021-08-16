/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.Transient;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRelation;
import javax.accessibility.AccessibleRole;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ScrollPaneUI;
import javax.swing.plaf.UIResource;

/**
 * Provides a scrollable view of a lightweight component.
 * A <code>JScrollPane</code> manages a viewport, optional
 * vertical and horizontal scroll bars, and optional row and
 * column heading viewports.
 * You can find task-oriented documentation of <code>JScrollPane</code> in
 *  <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/scrollpane.html">How to Use Scroll Panes</a>,
 * a section in <em>The Java Tutorial</em>.  Note that
 * <code>JScrollPane</code> does not support heavyweight components.
 *
 * <div style="float:right;text-align:center">
 *   <p><b>Example:</b>
 *   <p><img src="doc-files/JScrollPane-1.gif"
 *       alt="The following text describes this image."
 *       width="256" height="248">
 * </div>
 * The <code>JViewport</code> provides a window,
 * or &quot;viewport&quot; onto a data
 * source -- for example, a text file. That data source is the
 * &quot;scrollable client&quot; (aka data model) displayed by the
 * <code>JViewport</code> view.
 * A <code>JScrollPane</code> basically consists of <code>JScrollBar</code>s,
 * a <code>JViewport</code>, and the wiring between them,
 * as shown in the diagram at right.
 * <p>
 * In addition to the scroll bars and viewport,
 * a <code>JScrollPane</code> can have a
 * column header and a row header. Each of these is a
 * <code>JViewport</code> object that
 * you specify with <code>setRowHeaderView</code>,
 * and <code>setColumnHeaderView</code>.
 * The column header viewport automatically scrolls left and right, tracking
 * the left-right scrolling of the main viewport.
 * (It never scrolls vertically, however.)
 * The row header acts in a similar fashion.
 * <p>
 * Where two scroll bars meet, the row header meets the column header,
 * or a scroll bar meets one of the headers, both components stop short
 * of the corner, leaving a rectangular space which is, by default, empty.
 * These spaces can potentially exist in any number of the four corners.
 * In the previous diagram, the top right space is present and identified
 * by the label "corner component".
 * <p>
 * Any number of these empty spaces can be replaced by using the
 * <code>setCorner</code> method to add a component to a particular corner.
 * (Note: The same component cannot be added to multiple corners.)
 * This is useful if there's
 * some extra decoration or function you'd like to add to the scroll pane.
 * The size of each corner component is entirely determined by the size of the
 * headers and/or scroll bars that surround it.
 * <p>
 * A corner component will only be visible if there is an empty space in that
 * corner for it to exist in. For example, consider a component set into the
 * top right corner of a scroll pane with a column header. If the scroll pane's
 * vertical scrollbar is not present, perhaps because the view component hasn't
 * grown large enough to require it, then the corner component will not be
 * shown (since there is no empty space in that corner created by the meeting
 * of the header and vertical scroll bar). Forcing the scroll bar to always be
 * shown, using
 * <code>setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS)</code>,
 * will ensure that the space for the corner component always exists.
 * <p>
 * To add a border around the main viewport,
 * you can use <code>setViewportBorder</code>.
 * (Of course, you can also add a border around the whole scroll pane using
 * <code>setBorder</code>.)
 * <p>
 * A common operation to want to do is to set the background color that will
 * be used if the main viewport view is smaller than the viewport, or is
 * not opaque. This can be accomplished by setting the background color
 * of the viewport, via <code>scrollPane.getViewport().setBackground()</code>.
 * The reason for setting the color of the viewport and not the scrollpane
 * is that by default <code>JViewport</code> is opaque
 * which, among other things, means it will completely fill
 * in its background using its background color.  Therefore when
 * <code>JScrollPane</code> draws its background the viewport will
 * usually draw over it.
 * <p>
 * By default <code>JScrollPane</code> uses <code>ScrollPaneLayout</code>
 * to handle the layout of its child Components. <code>ScrollPaneLayout</code>
 * determines the size to make the viewport view in one of two ways:
 * <ol>
 *   <li>If the view implements <code>Scrollable</code>
 *       a combination of <code>getPreferredScrollableViewportSize</code>,
 *       <code>getScrollableTracksViewportWidth</code> and
 *       <code>getScrollableTracksViewportHeight</code>is used, otherwise
 *   <li><code>getPreferredSize</code> is used.
 * </ol>
 * <p>
 * <strong>Warning:</strong> Swing is not thread safe. For more
 * information see <a
 * href="package-summary.html#threading">Swing's Threading
 * Policy</a>.
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
 * @see JScrollBar
 * @see JViewport
 * @see ScrollPaneLayout
 * @see Scrollable
 * @see Component#getPreferredSize
 * @see #setViewportView
 * @see #setRowHeaderView
 * @see #setColumnHeaderView
 * @see #setCorner
 * @see #setViewportBorder
 *
 * @author Hans Muller
 * @since 1.2
 */
@JavaBean(defaultProperty = "UI", description = "A specialized container that manages a viewport, optional scrollbars and headers")
@SwingContainer(delegate = "getViewport")
@SuppressWarnings("serial") // Same-version serialization only
public class JScrollPane extends JComponent implements ScrollPaneConstants, Accessible
{
    private Border viewportBorder;

    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "ScrollPaneUI";

    /**
     * The display policy for the vertical scrollbar.
     * The default is
     * <code>ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED</code>.
     * @see #setVerticalScrollBarPolicy
     */
    protected int verticalScrollBarPolicy = VERTICAL_SCROLLBAR_AS_NEEDED;


    /**
     * The display policy for the horizontal scrollbar.
     * The default is
     * <code>ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED</code>.
     * @see #setHorizontalScrollBarPolicy
     */
    protected int horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_AS_NEEDED;


    /**
     * The scrollpane's viewport child.  Default is an empty
     * <code>JViewport</code>.
     * @see #setViewport
     */
    protected JViewport viewport;


    /**
     * The scrollpane's vertical scrollbar child.
     * Default is a <code>JScrollBar</code>.
     * @see #setVerticalScrollBar
     */
    protected JScrollBar verticalScrollBar;


    /**
     * The scrollpane's horizontal scrollbar child.
     * Default is a <code>JScrollBar</code>.
     * @see #setHorizontalScrollBar
     */
    protected JScrollBar horizontalScrollBar;


    /**
     * The row header child.  Default is <code>null</code>.
     * @see #setRowHeader
     */
    protected JViewport rowHeader;


    /**
     * The column header child.  Default is <code>null</code>.
     * @see #setColumnHeader
     */
    protected JViewport columnHeader;


    /**
     * The component to display in the lower left corner.
     * Default is <code>null</code>.
     * @see #setCorner
     */
    protected Component lowerLeft;


    /**
     * The component to display in the lower right corner.
     * Default is <code>null</code>.
     * @see #setCorner
     */
    protected Component lowerRight;


    /**
     * The component to display in the upper left corner.
     * Default is <code>null</code>.
     * @see #setCorner
     */
    protected Component upperLeft;


    /**
     * The component to display in the upper right corner.
     * Default is <code>null</code>.
     * @see #setCorner
     */
    protected Component upperRight;

    /*
     * State flag for mouse wheel scrolling
     */
    private boolean wheelScrollState = true;

    /**
     * Creates a <code>JScrollPane</code> that displays the view
     * component in a viewport
     * whose view position can be controlled with a pair of scrollbars.
     * The scrollbar policies specify when the scrollbars are displayed,
     * For example, if <code>vsbPolicy</code> is
     * <code>VERTICAL_SCROLLBAR_AS_NEEDED</code>
     * then the vertical scrollbar only appears if the view doesn't fit
     * vertically. The available policy settings are listed at
     * {@link #setVerticalScrollBarPolicy} and
     * {@link #setHorizontalScrollBarPolicy}.
     *
     * @see #setViewportView
     *
     * @param view the component to display in the scrollpanes viewport
     * @param vsbPolicy an integer that specifies the vertical
     *          scrollbar policy
     * @param hsbPolicy an integer that specifies the horizontal
     *          scrollbar policy
     */
    public JScrollPane(Component view, int vsbPolicy, int hsbPolicy)
    {
        setLayout(new ScrollPaneLayout.UIResource());
        setVerticalScrollBarPolicy(vsbPolicy);
        setHorizontalScrollBarPolicy(hsbPolicy);
        setViewport(createViewport());
        setVerticalScrollBar(createVerticalScrollBar());
        setHorizontalScrollBar(createHorizontalScrollBar());
        if (view != null) {
            setViewportView(view);
        }
        setUIProperty("opaque",true);
        updateUI();

        if (!this.getComponentOrientation().isLeftToRight()) {
            viewport.setViewPosition(new Point(Integer.MAX_VALUE, 0));
        }
    }


    /**
     * Creates a <code>JScrollPane</code> that displays the
     * contents of the specified
     * component, where both horizontal and vertical scrollbars appear
     * whenever the component's contents are larger than the view.
     *
     * @see #setViewportView
     * @param view the component to display in the scrollpane's viewport
     */
    public JScrollPane(Component view) {
        this(view, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }


    /**
     * Creates an empty (no viewport view) <code>JScrollPane</code>
     * with specified
     * scrollbar policies. The available policy settings are listed at
     * {@link #setVerticalScrollBarPolicy} and
     * {@link #setHorizontalScrollBarPolicy}.
     *
     * @see #setViewportView
     *
     * @param vsbPolicy an integer that specifies the vertical
     *          scrollbar policy
     * @param hsbPolicy an integer that specifies the horizontal
     *          scrollbar policy
     */
    public JScrollPane(int vsbPolicy, int hsbPolicy) {
        this(null, vsbPolicy, hsbPolicy);
    }


    /**
     * Creates an empty (no viewport view) <code>JScrollPane</code>
     * where both horizontal and vertical scrollbars appear when needed.
     */
    public JScrollPane() {
        this(null, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }


    /**
     * Returns the look and feel (L&amp;F) object that renders this component.
     *
     * @return the <code>ScrollPaneUI</code> object that renders this
     *                          component
     * @see #setUI
     */
    @BeanProperty(hidden = true, visualUpdate = true, description
            = "The UI object that implements the Component's LookAndFeel.")
    public ScrollPaneUI getUI() {
        return (ScrollPaneUI)ui;
    }


    /**
     * Sets the <code>ScrollPaneUI</code> object that provides the
     * look and feel (L&amp;F) for this component.
     *
     * @param ui the <code>ScrollPaneUI</code> L&amp;F object
     * @see #getUI
     */
    public void setUI(ScrollPaneUI ui) {
        super.setUI(ui);
    }


    /**
     * Replaces the current <code>ScrollPaneUI</code> object with a version
     * from the current default look and feel.
     * To be called when the default look and feel changes.
     *
     * @see JComponent#updateUI
     * @see UIManager#getUI
     */
    public void updateUI() {
        setUI((ScrollPaneUI)UIManager.getUI(this));
    }


    /**
     * Returns the suffix used to construct the name of the L&amp;F class used to
     * render this component.
     *
     * @return the string "ScrollPaneUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false, hidden = true)
    public String getUIClassID() {
        return uiClassID;
    }



    /**
     * Sets the layout manager for this <code>JScrollPane</code>.
     * This method overrides <code>setLayout</code> in
     * <code>java.awt.Container</code> to ensure that only
     * <code>LayoutManager</code>s which
     * are subclasses of <code>ScrollPaneLayout</code> can be used in a
     * <code>JScrollPane</code>. If <code>layout</code> is non-null, this
     * will invoke <code>syncWithScrollPane</code> on it.
     *
     * @param layout the specified layout manager
     * @exception ClassCastException if layout is not a
     *                  <code>ScrollPaneLayout</code>
     * @see java.awt.Container#getLayout
     * @see java.awt.Container#setLayout
     */
    public void setLayout(LayoutManager layout) {
        if (layout instanceof ScrollPaneLayout) {
            super.setLayout(layout);
            ((ScrollPaneLayout)layout).syncWithScrollPane(this);
        }
        else if (layout == null) {
            super.setLayout(layout);
        }
        else {
            String s = "layout of JScrollPane must be a ScrollPaneLayout";
            throw new ClassCastException(s);
        }
    }

    /**
     * Overridden to return true so that any calls to <code>revalidate</code>
     * on any descendants of this <code>JScrollPane</code> will cause the
     * entire tree beginning with this <code>JScrollPane</code> to be
     * validated.
     *
     * @return true
     * @see java.awt.Container#validate
     * @see JComponent#revalidate
     * @see JComponent#isValidateRoot
     * @see java.awt.Container#isValidateRoot
     */
    @Override
    @BeanProperty(hidden = true)
    public boolean isValidateRoot() {
        return true;
    }


    /**
     * Returns the vertical scroll bar policy value.
     * @return the <code>verticalScrollBarPolicy</code> property
     * @see #setVerticalScrollBarPolicy
     */
    public int getVerticalScrollBarPolicy() {
        return verticalScrollBarPolicy;
    }


    /**
     * Determines when the vertical scrollbar appears in the scrollpane.
     * Legal values are:
     * <ul>
     * <li><code>ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED</code>
     * <li><code>ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER</code>
     * <li><code>ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS</code>
     * </ul>
     *
     * @param policy one of the three values listed above
     * @exception IllegalArgumentException if <code>policy</code>
     *                          is not one of the legal values shown above
     * @see #getVerticalScrollBarPolicy
     */
    @BeanProperty(preferred = true, enumerationValues = {
            "ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED",
            "ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER",
            "ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS"}, description
            = "The scrollpane vertical scrollbar policy")
    public void setVerticalScrollBarPolicy(int policy) {
        switch (policy) {
        case VERTICAL_SCROLLBAR_AS_NEEDED:
        case VERTICAL_SCROLLBAR_NEVER:
        case VERTICAL_SCROLLBAR_ALWAYS:
                break;
        default:
            throw new IllegalArgumentException("invalid verticalScrollBarPolicy");
        }
        int old = verticalScrollBarPolicy;
        verticalScrollBarPolicy = policy;
        firePropertyChange("verticalScrollBarPolicy", old, policy);
        revalidate();
        repaint();
    }


    /**
     * Returns the horizontal scroll bar policy value.
     * @return the <code>horizontalScrollBarPolicy</code> property
     * @see #setHorizontalScrollBarPolicy
     */
    public int getHorizontalScrollBarPolicy() {
        return horizontalScrollBarPolicy;
    }


    /**
     * Determines when the horizontal scrollbar appears in the scrollpane.
     * The options are:<ul>
     * <li><code>ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED</code>
     * <li><code>ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER</code>
     * <li><code>ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS</code>
     * </ul>
     *
     * @param policy one of the three values listed above
     * @exception IllegalArgumentException if <code>policy</code>
     *                          is not one of the legal values shown above
     * @see #getHorizontalScrollBarPolicy
     */
    @BeanProperty(preferred = true, enumerationValues = {
            "ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED",
            "ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER",
            "ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS"}, description
            = "The scrollpane scrollbar policy")
    public void setHorizontalScrollBarPolicy(int policy) {
        switch (policy) {
        case HORIZONTAL_SCROLLBAR_AS_NEEDED:
        case HORIZONTAL_SCROLLBAR_NEVER:
        case HORIZONTAL_SCROLLBAR_ALWAYS:
                break;
        default:
            throw new IllegalArgumentException("invalid horizontalScrollBarPolicy");
        }
        int old = horizontalScrollBarPolicy;
        horizontalScrollBarPolicy = policy;
        firePropertyChange("horizontalScrollBarPolicy", old, policy);
        revalidate();
        repaint();
    }


    /**
     * Returns the <code>Border</code> object that surrounds the viewport.
     *
     * @return the <code>viewportBorder</code> property
     * @see #setViewportBorder
     */
    public Border getViewportBorder() {
        return viewportBorder;
    }


    /**
     * Adds a border around the viewport.  Note that the border isn't
     * set on the viewport directly, <code>JViewport</code> doesn't support
     * the <code>JComponent</code> border property.
     * Similarly setting the <code>JScrollPane</code>s
     * viewport doesn't affect the <code>viewportBorder</code> property.
     * <p>
     * The default value of this property is computed by the look
     * and feel implementation.
     *
     * @param viewportBorder the border to be added
     * @see #getViewportBorder
     * @see #setViewport
     */
    @BeanProperty(preferred = true, description
            = "The border around the viewport.")
    public void setViewportBorder(Border viewportBorder) {
        Border oldValue = this.viewportBorder;
        this.viewportBorder = viewportBorder;
        firePropertyChange("viewportBorder", oldValue, viewportBorder);
    }


    /**
     * Returns the bounds of the viewport's border.
     *
     * @return a <code>Rectangle</code> object specifying the viewport border
     */
    @BeanProperty(bound = false)
    public Rectangle getViewportBorderBounds()
    {
        Rectangle borderR = new Rectangle(getSize());

        Insets insets = getInsets();
        borderR.x = insets.left;
        borderR.y = insets.top;
        borderR.width -= insets.left + insets.right;
        borderR.height -= insets.top + insets.bottom;

        boolean leftToRight = SwingUtilities.isLeftToRight(this);

        /* If there's a visible column header remove the space it
         * needs from the top of borderR.
         */

        JViewport colHead = getColumnHeader();
        if ((colHead != null) && (colHead.isVisible())) {
            int colHeadHeight = colHead.getHeight();
            borderR.y += colHeadHeight;
            borderR.height -= colHeadHeight;
        }

        /* If there's a visible row header remove the space it needs
         * from the left of borderR.
         */

        JViewport rowHead = getRowHeader();
        if ((rowHead != null) && (rowHead.isVisible())) {
            int rowHeadWidth = rowHead.getWidth();
            if ( leftToRight ) {
                borderR.x += rowHeadWidth;
            }
            borderR.width -= rowHeadWidth;
        }

        /* If there's a visible vertical scrollbar remove the space it needs
         * from the width of borderR.
         */
        JScrollBar vsb = getVerticalScrollBar();
        if ((vsb != null) && (vsb.isVisible())) {
            int vsbWidth = vsb.getWidth();
            if ( !leftToRight ) {
                borderR.x += vsbWidth;
            }
            borderR.width -= vsbWidth;
        }

        /* If there's a visible horizontal scrollbar remove the space it needs
         * from the height of borderR.
         */
        JScrollBar hsb = getHorizontalScrollBar();
        if ((hsb != null) && (hsb.isVisible())) {
            borderR.height -= hsb.getHeight();
        }

        return borderR;
    }


    /**
     * By default <code>JScrollPane</code> creates scrollbars
     * that are instances
     * of this class.  <code>Scrollbar</code> overrides the
     * <code>getUnitIncrement</code> and <code>getBlockIncrement</code>
     * methods so that, if the viewport's view is a <code>Scrollable</code>,
     * the view is asked to compute these values. Unless
     * the unit/block increment have been explicitly set.
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
     * @see Scrollable
     * @see JScrollPane#createVerticalScrollBar
     * @see JScrollPane#createHorizontalScrollBar
     */
    @SuppressWarnings("serial") // Same-version serialization only
    protected class ScrollBar extends JScrollBar implements UIResource
    {
        /**
         * Set to true when the unit increment has been explicitly set.
         * If this is false the viewport's view is obtained and if it
         * is an instance of <code>Scrollable</code> the unit increment
         * from it is used.
         */
        private boolean unitIncrementSet;
        /**
         * Set to true when the block increment has been explicitly set.
         * If this is false the viewport's view is obtained and if it
         * is an instance of <code>Scrollable</code> the block increment
         * from it is used.
         */
        private boolean blockIncrementSet;

        /**
         * Creates a scrollbar with the specified orientation.
         * The options are:
         * <ul>
         * <li><code>ScrollPaneConstants.VERTICAL</code>
         * <li><code>ScrollPaneConstants.HORIZONTAL</code>
         * </ul>
         *
         * @param orientation  an integer specifying one of the legal
         *      orientation values shown above
         * @since 1.4
         */
        public ScrollBar(int orientation) {
            super(orientation);
            this.putClientProperty("JScrollBar.fastWheelScrolling",
                                   Boolean.TRUE);
        }

        /**
         * Messages super to set the value, and resets the
         * <code>unitIncrementSet</code> instance variable to true.
         *
         * @param unitIncrement the new unit increment value, in pixels
         */
        public void setUnitIncrement(int unitIncrement) {
            unitIncrementSet = true;
            this.putClientProperty("JScrollBar.fastWheelScrolling", null);
            super.setUnitIncrement(unitIncrement);
        }

        /**
         * Computes the unit increment for scrolling if the viewport's
         * view is a <code>Scrollable</code> object.
         * Otherwise return <code>super.getUnitIncrement</code>.
         *
         * @param direction less than zero to scroll up/left,
         *      greater than zero for down/right
         * @return an integer, in pixels, containing the unit increment
         * @see Scrollable#getScrollableUnitIncrement
         */
        public int getUnitIncrement(int direction) {
            JViewport vp = getViewport();
            if (!unitIncrementSet && (vp != null) &&
                (vp.getView() instanceof Scrollable)) {
                Scrollable view = (Scrollable)(vp.getView());
                Rectangle vr = vp.getViewRect();
                return view.getScrollableUnitIncrement(vr, getOrientation(), direction);
            }
            else {
                return super.getUnitIncrement(direction);
            }
        }

        /**
         * Messages super to set the value, and resets the
         * <code>blockIncrementSet</code> instance variable to true.
         *
         * @param blockIncrement the new block increment value, in pixels
         */
        public void setBlockIncrement(int blockIncrement) {
            blockIncrementSet = true;
            this.putClientProperty("JScrollBar.fastWheelScrolling", null);
            super.setBlockIncrement(blockIncrement);
        }

        /**
         * Computes the block increment for scrolling if the viewport's
         * view is a <code>Scrollable</code> object.  Otherwise
         * the <code>blockIncrement</code> equals the viewport's width
         * or height.  If there's no viewport return
         * <code>super.getBlockIncrement</code>.
         *
         * @param direction less than zero to scroll up/left,
         *      greater than zero for down/right
         * @return an integer, in pixels, containing the block increment
         * @see Scrollable#getScrollableBlockIncrement
         */
        public int getBlockIncrement(int direction) {
            JViewport vp = getViewport();
            if (blockIncrementSet || vp == null) {
                return super.getBlockIncrement(direction);
            }
            else if (vp.getView() instanceof Scrollable) {
                Scrollable view = (Scrollable)(vp.getView());
                Rectangle vr = vp.getViewRect();
                return view.getScrollableBlockIncrement(vr, getOrientation(), direction);
            }
            else if (getOrientation() == VERTICAL) {
                return vp.getExtentSize().height;
            }
            else {
                return vp.getExtentSize().width;
            }
        }

    }


    /**
     * Returns a <code>JScrollPane.ScrollBar</code> by default.
     * Subclasses may override this method to force <code>ScrollPaneUI</code>
     * implementations to use a <code>JScrollBar</code> subclass.
     * Used by <code>ScrollPaneUI</code> implementations to
     * create the horizontal scrollbar.
     *
     * @return a <code>JScrollBar</code> with a horizontal orientation
     * @see JScrollBar
     */
    public JScrollBar createHorizontalScrollBar() {
        return new ScrollBar(JScrollBar.HORIZONTAL);
    }


    /**
     * Returns the horizontal scroll bar that controls the viewport's
     * horizontal view position.
     *
     * @return the <code>horizontalScrollBar</code> property
     * @see #setHorizontalScrollBar
     */
    @Transient
    public JScrollBar getHorizontalScrollBar() {
        return horizontalScrollBar;
    }


    /**
     * Adds the scrollbar that controls the viewport's horizontal view
     * position to the scrollpane.
     * This is usually unnecessary, as <code>JScrollPane</code> creates
     * horizontal and vertical scrollbars by default.
     *
     * @param horizontalScrollBar the horizontal scrollbar to be added
     * @see #createHorizontalScrollBar
     * @see #getHorizontalScrollBar
     */
    @BeanProperty(expert = true, description
            = "The horizontal scrollbar.")
    public void setHorizontalScrollBar(JScrollBar horizontalScrollBar) {
        JScrollBar old = getHorizontalScrollBar();
        this.horizontalScrollBar = horizontalScrollBar;
        if (horizontalScrollBar != null) {
            add(horizontalScrollBar, HORIZONTAL_SCROLLBAR);
        }
        else if (old != null) {
            remove(old);
        }
        firePropertyChange("horizontalScrollBar", old, horizontalScrollBar);

        revalidate();
        repaint();
    }


    /**
     * Returns a <code>JScrollPane.ScrollBar</code> by default.  Subclasses
     * may override this method to force <code>ScrollPaneUI</code>
     * implementations to use a <code>JScrollBar</code> subclass.
     * Used by <code>ScrollPaneUI</code> implementations to create the
     * vertical scrollbar.
     *
     * @return a <code>JScrollBar</code> with a vertical orientation
     * @see JScrollBar
     */
    public JScrollBar createVerticalScrollBar() {
        return new ScrollBar(JScrollBar.VERTICAL);
    }


    /**
     * Returns the vertical scroll bar that controls the viewports
     * vertical view position.
     *
     * @return the <code>verticalScrollBar</code> property
     * @see #setVerticalScrollBar
     */
    @Transient
    public JScrollBar getVerticalScrollBar() {
        return verticalScrollBar;
    }


    /**
     * Adds the scrollbar that controls the viewports vertical view position
     * to the scrollpane.  This is usually unnecessary,
     * as <code>JScrollPane</code> creates vertical and
     * horizontal scrollbars by default.
     *
     * @param verticalScrollBar the new vertical scrollbar to be added
     * @see #createVerticalScrollBar
     * @see #getVerticalScrollBar
     */
    @BeanProperty(expert = true, description
            = "The vertical scrollbar.")
    public void setVerticalScrollBar(JScrollBar verticalScrollBar) {
        JScrollBar old = getVerticalScrollBar();
        this.verticalScrollBar = verticalScrollBar;
        add(verticalScrollBar, VERTICAL_SCROLLBAR);
        firePropertyChange("verticalScrollBar", old, verticalScrollBar);

        revalidate();
        repaint();
    }


    /**
     * Returns a new <code>JViewport</code> by default.
     * Used to create the
     * viewport (as needed) in <code>setViewportView</code>,
     * <code>setRowHeaderView</code>, and <code>setColumnHeaderView</code>.
     * Subclasses may override this method to return a subclass of
     * <code>JViewport</code>.
     *
     * @return a new <code>JViewport</code>
     */
    protected JViewport createViewport() {
        return new JViewport();
    }


    /**
     * Returns the current <code>JViewport</code>.
     *
     * @see #setViewport
     * @return the <code>viewport</code> property
     */
    public JViewport getViewport() {
        return viewport;
    }


    /**
     * Removes the old viewport (if there is one); forces the
     * viewPosition of the new viewport to be in the +x,+y quadrant;
     * syncs up the row and column headers (if there are any) with the
     * new viewport; and finally syncs the scrollbars and
     * headers with the new viewport.
     * <p>
     * Most applications will find it more convenient to use
     * <code>setViewportView</code>
     * to add a viewport and a view to the scrollpane.
     *
     * @param viewport the new viewport to be used; if viewport is
     *          <code>null</code>, the old viewport is still removed
     *          and the new viewport is set to <code>null</code>
     * @see #createViewport
     * @see #getViewport
     * @see #setViewportView
     */
    @BeanProperty(expert = true, visualUpdate = true, description
            = "The viewport child for this scrollpane")
    public void setViewport(JViewport viewport) {
        JViewport old = getViewport();
        this.viewport = viewport;
        if (viewport != null) {
            add(viewport, VIEWPORT);
        }
        else if (old != null) {
            remove(old);
        }
        firePropertyChange("viewport", old, viewport);

        if (accessibleContext != null) {
            ((AccessibleJScrollPane)accessibleContext).resetViewPort();
        }

        revalidate();
        repaint();
    }


    /**
     * Creates a viewport if necessary and then sets its view.  Applications
     * that don't provide the view directly to the <code>JScrollPane</code>
     * constructor
     * should use this method to specify the scrollable child that's going
     * to be displayed in the scrollpane. For example:
     * <pre>
     * JScrollPane scrollpane = new JScrollPane();
     * scrollpane.setViewportView(myBigComponentToScroll);
     * </pre>
     * Applications should not add children directly to the scrollpane.
     *
     * @param view the component to add to the viewport
     * @see #setViewport
     * @see JViewport#setView
     */
    public void setViewportView(Component view) {
        if (getViewport() == null) {
            setViewport(createViewport());
        }
        getViewport().setView(view);
    }



    /**
     * Returns the row header.
     * @return the <code>rowHeader</code> property
     * @see #setRowHeader
     */
    @Transient
    public JViewport getRowHeader() {
        return rowHeader;
    }


    /**
     * Removes the old rowHeader, if it exists; if the new rowHeader
     * isn't <code>null</code>, syncs the y coordinate of its
     * viewPosition with
     * the viewport (if there is one) and then adds it to the scroll pane.
     * <p>
     * Most applications will find it more convenient to use
     * <code>setRowHeaderView</code>
     * to add a row header component and its viewport to the scroll pane.
     *
     * @param rowHeader the new row header to be used; if <code>null</code>
     *          the old row header is still removed and the new rowHeader
     *          is set to <code>null</code>
     * @see #getRowHeader
     * @see #setRowHeaderView
     */
    @BeanProperty(expert = true, description
            = "The row header child for this scrollpane")
    public void setRowHeader(JViewport rowHeader) {
        JViewport old = getRowHeader();
        this.rowHeader = rowHeader;
        if (rowHeader != null) {
            add(rowHeader, ROW_HEADER);
        }
        else if (old != null) {
            remove(old);
        }
        firePropertyChange("rowHeader", old, rowHeader);
        revalidate();
        repaint();
    }


    /**
     * Creates a row-header viewport if necessary, sets
     * its view and then adds the row-header viewport
     * to the scrollpane.  For example:
     * <pre>
     * JScrollPane scrollpane = new JScrollPane();
     * scrollpane.setViewportView(myBigComponentToScroll);
     * scrollpane.setRowHeaderView(myBigComponentsRowHeader);
     * </pre>
     *
     * @see #setRowHeader
     * @see JViewport#setView
     * @param view the component to display as the row header
     */
    public void setRowHeaderView(Component view) {
        if (getRowHeader() == null) {
            setRowHeader(createViewport());
        }
        getRowHeader().setView(view);
    }



    /**
     * Returns the column header.
     * @return the <code>columnHeader</code> property
     * @see #setColumnHeader
     */
    @Transient
    public JViewport getColumnHeader() {
        return columnHeader;
    }


    /**
     * Removes the old columnHeader, if it exists; if the new columnHeader
     * isn't <code>null</code>, syncs the x coordinate of its viewPosition
     * with the viewport (if there is one) and then adds it to the scroll pane.
     * <p>
     * Most applications will find it more convenient to use
     * <code>setColumnHeaderView</code>
     * to add a column header component and its viewport to the scroll pane.
     *
     * @param columnHeader  a {@code JViewport} which is the new column header
     * @see #getColumnHeader
     * @see #setColumnHeaderView
     */
    @BeanProperty(visualUpdate = true, description
            = "The column header child for this scrollpane")
    public void setColumnHeader(JViewport columnHeader) {
        JViewport old = getColumnHeader();
        this.columnHeader = columnHeader;
        if (columnHeader != null) {
            add(columnHeader, COLUMN_HEADER);
        }
        else if (old != null) {
            remove(old);
        }
        firePropertyChange("columnHeader", old, columnHeader);

        revalidate();
        repaint();
    }



    /**
     * Creates a column-header viewport if necessary, sets
     * its view, and then adds the column-header viewport
     * to the scrollpane.  For example:
     * <pre>
     * JScrollPane scrollpane = new JScrollPane();
     * scrollpane.setViewportView(myBigComponentToScroll);
     * scrollpane.setColumnHeaderView(myBigComponentsColumnHeader);
     * </pre>
     *
     * @see #setColumnHeader
     * @see JViewport#setView
     *
     * @param view the component to display as the column header
     */
    public void setColumnHeaderView(Component view) {
        if (getColumnHeader() == null) {
            setColumnHeader(createViewport());
        }
        getColumnHeader().setView(view);
    }


    /**
     * Returns the component at the specified corner. The
     * <code>key</code> value specifying the corner is one of:
     * <ul>
     * <li>ScrollPaneConstants.LOWER_LEFT_CORNER
     * <li>ScrollPaneConstants.LOWER_RIGHT_CORNER
     * <li>ScrollPaneConstants.UPPER_LEFT_CORNER
     * <li>ScrollPaneConstants.UPPER_RIGHT_CORNER
     * <li>ScrollPaneConstants.LOWER_LEADING_CORNER
     * <li>ScrollPaneConstants.LOWER_TRAILING_CORNER
     * <li>ScrollPaneConstants.UPPER_LEADING_CORNER
     * <li>ScrollPaneConstants.UPPER_TRAILING_CORNER
     * </ul>
     *
     * @param key one of the values as shown above
     * @return the corner component (which may be <code>null</code>)
     *         identified by the given key, or <code>null</code>
     *         if the key is invalid
     * @see #setCorner
     */
    public Component getCorner(String key) {
        boolean isLeftToRight = getComponentOrientation().isLeftToRight();
        if (key.equals(LOWER_LEADING_CORNER)) {
            key = isLeftToRight ? LOWER_LEFT_CORNER : LOWER_RIGHT_CORNER;
        } else if (key.equals(LOWER_TRAILING_CORNER)) {
            key = isLeftToRight ? LOWER_RIGHT_CORNER : LOWER_LEFT_CORNER;
        } else if (key.equals(UPPER_LEADING_CORNER)) {
            key = isLeftToRight ? UPPER_LEFT_CORNER : UPPER_RIGHT_CORNER;
        } else if (key.equals(UPPER_TRAILING_CORNER)) {
            key = isLeftToRight ? UPPER_RIGHT_CORNER : UPPER_LEFT_CORNER;
        }
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
     * Adds a child that will appear in one of the scroll panes
     * corners, if there's room.   For example with both scrollbars
     * showing (on the right and bottom edges of the scrollpane)
     * the lower left corner component will be shown in the space
     * between ends of the two scrollbars. Legal values for
     * the <b>key</b> are:
     * <ul>
     * <li>ScrollPaneConstants.LOWER_LEFT_CORNER
     * <li>ScrollPaneConstants.LOWER_RIGHT_CORNER
     * <li>ScrollPaneConstants.UPPER_LEFT_CORNER
     * <li>ScrollPaneConstants.UPPER_RIGHT_CORNER
     * <li>ScrollPaneConstants.LOWER_LEADING_CORNER
     * <li>ScrollPaneConstants.LOWER_TRAILING_CORNER
     * <li>ScrollPaneConstants.UPPER_LEADING_CORNER
     * <li>ScrollPaneConstants.UPPER_TRAILING_CORNER
     * </ul>
     * <p>
     * Although "corner" doesn't match any beans property
     * signature, <code>PropertyChange</code> events are generated with the
     * property name set to the corner key.
     *
     * @param key identifies which corner the component will appear in
     * @param corner one of the following components:
     * <ul>
     * <li>lowerLeft
     * <li>lowerRight
     * <li>upperLeft
     * <li>upperRight
     * </ul>
     * @exception IllegalArgumentException if corner key is invalid
     */
    public void setCorner(String key, Component corner)
    {
        Component old;
        boolean isLeftToRight = getComponentOrientation().isLeftToRight();
        if (key.equals(LOWER_LEADING_CORNER)) {
            key = isLeftToRight ? LOWER_LEFT_CORNER : LOWER_RIGHT_CORNER;
        } else if (key.equals(LOWER_TRAILING_CORNER)) {
            key = isLeftToRight ? LOWER_RIGHT_CORNER : LOWER_LEFT_CORNER;
        } else if (key.equals(UPPER_LEADING_CORNER)) {
            key = isLeftToRight ? UPPER_LEFT_CORNER : UPPER_RIGHT_CORNER;
        } else if (key.equals(UPPER_TRAILING_CORNER)) {
            key = isLeftToRight ? UPPER_RIGHT_CORNER : UPPER_LEFT_CORNER;
        }
        if (key.equals(LOWER_LEFT_CORNER)) {
            old = lowerLeft;
            lowerLeft = corner;
        }
        else if (key.equals(LOWER_RIGHT_CORNER)) {
            old = lowerRight;
            lowerRight = corner;
        }
        else if (key.equals(UPPER_LEFT_CORNER)) {
            old = upperLeft;
            upperLeft = corner;
        }
        else if (key.equals(UPPER_RIGHT_CORNER)) {
            old = upperRight;
            upperRight = corner;
        }
        else {
            throw new IllegalArgumentException("invalid corner key");
        }
        if (old != null) {
            remove(old);
        }
        if (corner != null) {
            add(corner, key);
        }
        firePropertyChange(key, old, corner);
        revalidate();
        repaint();
    }

    /**
     * Sets the orientation for the vertical and horizontal
     * scrollbars as determined by the
     * <code>ComponentOrientation</code> argument.
     *
     * @param  co one of the following values:
     * <ul>
     * <li>java.awt.ComponentOrientation.LEFT_TO_RIGHT
     * <li>java.awt.ComponentOrientation.RIGHT_TO_LEFT
     * <li>java.awt.ComponentOrientation.UNKNOWN
     * </ul>
     * @see java.awt.ComponentOrientation
     */
    public void setComponentOrientation( ComponentOrientation co ) {
        super.setComponentOrientation( co );
        if( verticalScrollBar != null )
            verticalScrollBar.setComponentOrientation( co );
        if( horizontalScrollBar != null )
            horizontalScrollBar.setComponentOrientation( co );
    }

    /**
     * Indicates whether or not scrolling will take place in response to the
     * mouse wheel.  Wheel scrolling is enabled by default.
     *
     * @return true if mouse wheel scrolling is enabled, false otherwise
     * @see #setWheelScrollingEnabled
     * @since 1.4
     */
    @BeanProperty(description
            = "Flag for enabling/disabling mouse wheel scrolling")
    public boolean isWheelScrollingEnabled() {return wheelScrollState;}

    /**
     * Enables/disables scrolling in response to movement of the mouse wheel.
     * Wheel scrolling is enabled by default.
     *
     * @param handleWheel   <code>true</code> if scrolling should be done
     *                      automatically for a MouseWheelEvent,
     *                      <code>false</code> otherwise.
     * @see #isWheelScrollingEnabled
     * @see java.awt.event.MouseWheelEvent
     * @see java.awt.event.MouseWheelListener
     * @since 1.4
     */
    @BeanProperty(description
            = "Flag for enabling/disabling mouse wheel scrolling")
    public void setWheelScrollingEnabled(boolean handleWheel) {
        boolean old = wheelScrollState;
        wheelScrollState = handleWheel;
        firePropertyChange("wheelScrollingEnabled", old, handleWheel);
    }

    /**
     * See <code>readObject</code> and <code>writeObject</code> in
     * <code>JComponent</code> for more
     * information about serialization in Swing.
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        if (getUIClassID().equals(uiClassID)) {
            byte count = JComponent.getWriteObjCounter(this);
            JComponent.setWriteObjCounter(this, --count);
            if (count == 0 && ui != null) {
                ui.installUI(this);
            }
        }
    }


    /**
     * Returns a string representation of this <code>JScrollPane</code>.
     * This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this <code>JScrollPane</code>.
     */
    protected String paramString() {
        String viewportBorderString = (viewportBorder != null ?
                                       viewportBorder.toString() : "");
        String viewportString = (viewport != null ?
                                 viewport.toString() : "");
        String verticalScrollBarPolicyString;
        if (verticalScrollBarPolicy == VERTICAL_SCROLLBAR_AS_NEEDED) {
            verticalScrollBarPolicyString = "VERTICAL_SCROLLBAR_AS_NEEDED";
        } else if (verticalScrollBarPolicy == VERTICAL_SCROLLBAR_NEVER) {
            verticalScrollBarPolicyString = "VERTICAL_SCROLLBAR_NEVER";
        } else if (verticalScrollBarPolicy == VERTICAL_SCROLLBAR_ALWAYS) {
            verticalScrollBarPolicyString = "VERTICAL_SCROLLBAR_ALWAYS";
        } else verticalScrollBarPolicyString = "";
        String horizontalScrollBarPolicyString;
        if (horizontalScrollBarPolicy == HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            horizontalScrollBarPolicyString = "HORIZONTAL_SCROLLBAR_AS_NEEDED";
        } else if (horizontalScrollBarPolicy == HORIZONTAL_SCROLLBAR_NEVER) {
            horizontalScrollBarPolicyString = "HORIZONTAL_SCROLLBAR_NEVER";
        } else if (horizontalScrollBarPolicy == HORIZONTAL_SCROLLBAR_ALWAYS) {
            horizontalScrollBarPolicyString = "HORIZONTAL_SCROLLBAR_ALWAYS";
        } else horizontalScrollBarPolicyString = "";
        String horizontalScrollBarString = (horizontalScrollBar != null ?
                                            horizontalScrollBar.toString()
                                            : "");
        String verticalScrollBarString = (verticalScrollBar != null ?
                                          verticalScrollBar.toString() : "");
        String columnHeaderString = (columnHeader != null ?
                                     columnHeader.toString() : "");
        String rowHeaderString = (rowHeader != null ?
                                  rowHeader.toString() : "");
        String lowerLeftString = (lowerLeft != null ?
                                  lowerLeft.toString() : "");
        String lowerRightString = (lowerRight != null ?
                                  lowerRight.toString() : "");
        String upperLeftString = (upperLeft != null ?
                                  upperLeft.toString() : "");
        String upperRightString = (upperRight != null ?
                                  upperRight.toString() : "");

        return super.paramString() +
        ",columnHeader=" + columnHeaderString +
        ",horizontalScrollBar=" + horizontalScrollBarString +
        ",horizontalScrollBarPolicy=" + horizontalScrollBarPolicyString +
        ",lowerLeft=" + lowerLeftString +
        ",lowerRight=" + lowerRightString +
        ",rowHeader=" + rowHeaderString +
        ",upperLeft=" + upperLeftString +
        ",upperRight=" + upperRightString +
        ",verticalScrollBar=" + verticalScrollBarString +
        ",verticalScrollBarPolicy=" + verticalScrollBarPolicyString +
        ",viewport=" + viewportString +
        ",viewportBorder=" + viewportBorderString;
    }

/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this JScrollPane.
     * For scroll panes, the AccessibleContext takes the form of an
     * AccessibleJScrollPane.
     * A new AccessibleJScrollPane instance is created if necessary.
     *
     * @return an AccessibleJScrollPane that serves as the
     *         AccessibleContext of this JScrollPane
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJScrollPane();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JScrollPane</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to scroll pane user-interface
     * elements.
     * <p>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.  As of 1.4, support for long term storage
     * of all JavaBeans
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    @SuppressWarnings("serial") // Same-version serialization only
    protected class AccessibleJScrollPane extends AccessibleJComponent
        implements ChangeListener, PropertyChangeListener {

        /**
         * this {@code JScrollPane}'s current {@code JViewport}
         */
        protected JViewport viewPort = null;

        /**
         * Resets the viewport ChangeListener and PropertyChangeListener
         */
        public void resetViewPort() {
            if (viewPort != null) {
                viewPort.removeChangeListener(this);
                viewPort.removePropertyChangeListener(this);
            }
            viewPort = JScrollPane.this.getViewport();
            if (viewPort != null) {
                viewPort.addChangeListener(this);
                viewPort.addPropertyChangeListener(this);
            }
        }

        /**
         * AccessibleJScrollPane constructor
         */
        public AccessibleJScrollPane() {
            super();

            resetViewPort();

            // initialize the AccessibleRelationSets for the JScrollPane
            // and JScrollBar(s)
            JScrollBar scrollBar = getHorizontalScrollBar();
            if (scrollBar != null) {
                setScrollBarRelations(scrollBar);
            }
            scrollBar = getVerticalScrollBar();
            if (scrollBar != null) {
                setScrollBarRelations(scrollBar);
            }
        }

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SCROLL_PANE;
        }

        /**
         * Invoked when the target of the listener has changed its state.
         *
         * @param e  a <code>ChangeEvent</code> object. Must not be null.
         *
         * @throws NullPointerException if the parameter is null.
         */
        public void stateChanged(ChangeEvent e) {
            if (e == null) {
                throw new NullPointerException();
            }
            firePropertyChange(ACCESSIBLE_VISIBLE_DATA_PROPERTY,
                               Boolean.valueOf(false),
                               Boolean.valueOf(true));
        }

        /**
         * This method gets called when a bound property is changed.
         * @param e A <code>PropertyChangeEvent</code> object describing
         * the event source and the property that has changed. Must not be null.
         *
         * @throws NullPointerException if the parameter is null.
         * @since 1.5
         */
        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if (propertyName == "horizontalScrollBar" ||
                propertyName == "verticalScrollBar") {

                if (e.getNewValue() instanceof JScrollBar) {
                    setScrollBarRelations((JScrollBar)e.getNewValue());
                }
            }
        }


        /*
         * Sets the CONTROLLER_FOR and CONTROLLED_BY AccessibleRelations for
         * the JScrollPane and JScrollBar. JScrollBar must not be null.
         */
        void setScrollBarRelations(JScrollBar scrollBar) {
            /*
             * The JScrollBar is a CONTROLLER_FOR the JScrollPane.
             * The JScrollPane is CONTROLLED_BY the JScrollBar.
             */
            AccessibleRelation controlledBy =
                new AccessibleRelation(AccessibleRelation.CONTROLLED_BY,
                                       scrollBar);
            AccessibleRelation controllerFor =
                new AccessibleRelation(AccessibleRelation.CONTROLLER_FOR,
                                       JScrollPane.this);

            // set the relation set for the scroll bar
            AccessibleContext ac = scrollBar.getAccessibleContext();
            ac.getAccessibleRelationSet().add(controllerFor);

            // set the relation set for the scroll pane
            getAccessibleRelationSet().add(controlledBy);
        }
    }
}
