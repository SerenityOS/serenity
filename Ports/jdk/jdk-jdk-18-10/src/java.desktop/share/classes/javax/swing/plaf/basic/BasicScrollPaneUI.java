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

package javax.swing.plaf.basic;

import sun.swing.DefaultLookup;
import sun.swing.UIAction;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.plaf.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Insets;
import java.awt.Graphics;
import java.awt.event.*;

/**
 * A default L&amp;F implementation of ScrollPaneUI.
 *
 * @author Hans Muller
 */
public class BasicScrollPaneUI
    extends ScrollPaneUI implements ScrollPaneConstants
{
    /**
     * The instance of {@code JScrollPane}.
     */
    protected JScrollPane scrollpane;

    /**
     * {@code ChangeListener} installed on the vertical scrollbar.
     */
    protected ChangeListener vsbChangeListener;

    /**
     * {@code ChangeListener} installed on the horizontal scrollbar.
     */
    protected ChangeListener hsbChangeListener;

    /**
     * {@code ChangeListener} installed on the viewport.
     */
    protected ChangeListener viewportChangeListener;

    /**
     * {@code PropertyChangeListener} installed on the scroll pane.
     */
    protected PropertyChangeListener spPropertyChangeListener;
    private MouseWheelListener mouseScrollListener;
    private int oldExtent = Integer.MIN_VALUE;

    /**
     * {@code PropertyChangeListener} installed on the vertical scrollbar.
     */
    private PropertyChangeListener vsbPropertyChangeListener;

    /**
     * {@code PropertyChangeListener} installed on the horizontal scrollbar.
     */
    private PropertyChangeListener hsbPropertyChangeListener;

    private Handler handler;

    /**
     * State flag that shows whether setValue() was called from a user program
     * before the value of "extent" was set in right-to-left component
     * orientation.
     */
    private boolean setValueCalled = false;

    /**
     * Constructs a {@code BasicScrollPaneUI}.
     */
    public BasicScrollPaneUI() {}

    /**
     * Returns a new instance of {@code BasicScrollPaneUI}.
     *
     * @param x a component.
     * @return a new instance of {@code BasicScrollPaneUI}
     */
    public static ComponentUI createUI(JComponent x) {
        return new BasicScrollPaneUI();
    }

    static void loadActionMap(LazyActionMap map) {
        map.put(new Actions(Actions.SCROLL_UP));
        map.put(new Actions(Actions.SCROLL_DOWN));
        map.put(new Actions(Actions.SCROLL_HOME));
        map.put(new Actions(Actions.SCROLL_END));
        map.put(new Actions(Actions.UNIT_SCROLL_UP));
        map.put(new Actions(Actions.UNIT_SCROLL_DOWN));
        map.put(new Actions(Actions.SCROLL_LEFT));
        map.put(new Actions(Actions.SCROLL_RIGHT));
        map.put(new Actions(Actions.UNIT_SCROLL_RIGHT));
        map.put(new Actions(Actions.UNIT_SCROLL_LEFT));
    }



    public void paint(Graphics g, JComponent c) {
        Border vpBorder = scrollpane.getViewportBorder();
        if (vpBorder != null) {
            Rectangle r = scrollpane.getViewportBorderBounds();
            vpBorder.paintBorder(scrollpane, g, r.x, r.y, r.width, r.height);
        }
    }


    /**
     * @return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE)
     */
    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    /**
     * Installs default properties.
     *
     * @param scrollpane an instance of {@code JScrollPane}
     */
    protected void installDefaults(JScrollPane scrollpane)
    {
        LookAndFeel.installBorder(scrollpane, "ScrollPane.border");
        LookAndFeel.installColorsAndFont(scrollpane,
            "ScrollPane.background",
            "ScrollPane.foreground",
            "ScrollPane.font");

        Border vpBorder = scrollpane.getViewportBorder();
        if ((vpBorder == null) ||( vpBorder instanceof UIResource)) {
            vpBorder = UIManager.getBorder("ScrollPane.viewportBorder");
            scrollpane.setViewportBorder(vpBorder);
        }
        LookAndFeel.installProperty(scrollpane, "opaque", Boolean.TRUE);
    }

    /**
     * Registers listeners.
     *
     * @param c an instance of {@code JScrollPane}
     */
    protected void installListeners(JScrollPane c)
    {
        vsbChangeListener = createVSBChangeListener();
        vsbPropertyChangeListener = createVSBPropertyChangeListener();
        hsbChangeListener = createHSBChangeListener();
        hsbPropertyChangeListener = createHSBPropertyChangeListener();
        viewportChangeListener = createViewportChangeListener();
        spPropertyChangeListener = createPropertyChangeListener();

        JViewport viewport = scrollpane.getViewport();
        JScrollBar vsb = scrollpane.getVerticalScrollBar();
        JScrollBar hsb = scrollpane.getHorizontalScrollBar();

        if (viewport != null) {
            viewport.addChangeListener(viewportChangeListener);
        }
        if (vsb != null) {
            vsb.getModel().addChangeListener(vsbChangeListener);
            vsb.addPropertyChangeListener(vsbPropertyChangeListener);
        }
        if (hsb != null) {
            hsb.getModel().addChangeListener(hsbChangeListener);
            hsb.addPropertyChangeListener(hsbPropertyChangeListener);
        }

        scrollpane.addPropertyChangeListener(spPropertyChangeListener);

    mouseScrollListener = createMouseWheelListener();
    scrollpane.addMouseWheelListener(mouseScrollListener);

    }

    /**
     * Registers keyboard actions.
     *
     * @param c an instance of {@code JScrollPane}
     */
    protected void installKeyboardActions(JScrollPane c) {
        InputMap inputMap = getInputMap(JComponent.
                                  WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        SwingUtilities.replaceUIInputMap(c, JComponent.
                               WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inputMap);

        LazyActionMap.installLazyActionMap(c, BasicScrollPaneUI.class,
                                           "ScrollPane.actionMap");
    }

    InputMap getInputMap(int condition) {
        if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) {
            InputMap keyMap = (InputMap)DefaultLookup.get(scrollpane, this,
                                        "ScrollPane.ancestorInputMap");
            InputMap rtlKeyMap;

            if (scrollpane.getComponentOrientation().isLeftToRight() ||
                    ((rtlKeyMap = (InputMap)DefaultLookup.get(scrollpane, this,
                    "ScrollPane.ancestorInputMap.RightToLeft")) == null)) {
                return keyMap;
            } else {
                rtlKeyMap.setParent(keyMap);
                return rtlKeyMap;
            }
        }
        return null;
    }

    public void installUI(JComponent x) {
        scrollpane = (JScrollPane)x;
        installDefaults(scrollpane);
        installListeners(scrollpane);
        installKeyboardActions(scrollpane);
    }

    /**
     * Uninstalls default properties.
     *
     * @param c an instance of {@code JScrollPane}
     */
    protected void uninstallDefaults(JScrollPane c) {
        LookAndFeel.uninstallBorder(scrollpane);

        if (scrollpane.getViewportBorder() instanceof UIResource) {
            scrollpane.setViewportBorder(null);
        }
    }

    /**
     * Unregisters listeners.
     *
     * @param c a component
     */
    protected void uninstallListeners(JComponent c) {
        JViewport viewport = scrollpane.getViewport();
        JScrollBar vsb = scrollpane.getVerticalScrollBar();
        JScrollBar hsb = scrollpane.getHorizontalScrollBar();

        if (viewport != null) {
            viewport.removeChangeListener(viewportChangeListener);
        }
        if (vsb != null) {
            vsb.getModel().removeChangeListener(vsbChangeListener);
            vsb.removePropertyChangeListener(vsbPropertyChangeListener);
        }
        if (hsb != null) {
            hsb.getModel().removeChangeListener(hsbChangeListener);
            hsb.removePropertyChangeListener(hsbPropertyChangeListener);
        }

        scrollpane.removePropertyChangeListener(spPropertyChangeListener);

    if (mouseScrollListener != null) {
        scrollpane.removeMouseWheelListener(mouseScrollListener);
    }

        vsbChangeListener = null;
        hsbChangeListener = null;
        viewportChangeListener = null;
        spPropertyChangeListener = null;
        mouseScrollListener = null;
        handler = null;
    }

    /**
     * Unregisters keyboard actions.
     *
     * @param c an instance of {@code JScrollPane}
     */
    protected void uninstallKeyboardActions(JScrollPane c) {
        SwingUtilities.replaceUIActionMap(c, null);
        SwingUtilities.replaceUIInputMap(c, JComponent.
                           WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null);
    }


    public void uninstallUI(JComponent c) {
        uninstallDefaults(scrollpane);
        uninstallListeners(scrollpane);
        uninstallKeyboardActions(scrollpane);
        scrollpane = null;
    }

    private Handler getHandler() {
        if (handler == null) {
            handler = new Handler();
        }
        return handler;
    }

    /**
     * Synchronizes the {@code JScrollPane} with {@code Viewport}.
     */
    protected void syncScrollPaneWithViewport()
    {
        JViewport viewport = scrollpane.getViewport();
        JScrollBar vsb = scrollpane.getVerticalScrollBar();
        JScrollBar hsb = scrollpane.getHorizontalScrollBar();
        JViewport rowHead = scrollpane.getRowHeader();
        JViewport colHead = scrollpane.getColumnHeader();
        boolean ltr = scrollpane.getComponentOrientation().isLeftToRight();

        if (viewport != null) {
            Dimension extentSize = viewport.getExtentSize();
            Dimension viewSize = viewport.getViewSize();
            Point viewPosition = viewport.getViewPosition();

            if (vsb != null) {
                int extent = extentSize.height;
                int max = viewSize.height;
                int value = Math.max(0, Math.min(viewPosition.y, max - extent));
                vsb.setValues(value, extent, 0, max);
            }

            if (hsb != null) {
                int extent = extentSize.width;
                int max = viewSize.width;
                int value;

                if (ltr) {
                    value = Math.max(0, Math.min(viewPosition.x, max - extent));
                } else {
                    int currentValue = hsb.getValue();

                    /* Use a particular formula to calculate "value"
                     * until effective x coordinate is calculated.
                     */
                    if (setValueCalled && ((max - currentValue) == viewPosition.x)) {
                        value = Math.max(0, Math.min(max - extent, currentValue));
                        /* After "extent" is set, turn setValueCalled flag off.
                         */
                        if (extent != 0) {
                            setValueCalled = false;
                        }
                    } else {
                        if (extent > max) {
                            viewPosition.x = max - extent;
                            viewport.setViewPosition(viewPosition);
                            value = 0;
                        } else {
                           /* The following line can't handle a small value of
                            * viewPosition.x like Integer.MIN_VALUE correctly
                            * because (max - extent - viewPositoiin.x) causes
                            * an overflow. As a result, value becomes zero.
                            * (e.g. setViewPosition(Integer.MAX_VALUE, ...)
                            *       in a user program causes a overflow.
                            *       Its expected value is (max - extent).)
                            * However, this seems a trivial bug and adding a
                            * fix makes this often-called method slow, so I'll
                            * leave it until someone claims.
                            */
                            value = Math.max(0, Math.min(max - extent, max - extent - viewPosition.x));
                            if (oldExtent > extent) {
                                value -= oldExtent - extent;
                            }
                        }
                    }
                }
                oldExtent = extent;
                hsb.setValues(value, extent, 0, max);
            }

            if (rowHead != null) {
                Point p = rowHead.getViewPosition();
                p.y = viewport.getViewPosition().y;
                p.x = 0;
                rowHead.setViewPosition(p);
            }

            if (colHead != null) {
                Point p = colHead.getViewPosition();
                if (ltr) {
                    p.x = viewport.getViewPosition().x;
                } else {
                    p.x = Math.max(0, viewport.getViewPosition().x);
                }
                p.y = 0;
                colHead.setViewPosition(p);
            }
        }
    }

    /**
     * Returns the baseline.
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @see javax.swing.JComponent#getBaseline(int, int)
     * @since 1.6
     */
    public int getBaseline(JComponent c, int width, int height) {
        if (c == null) {
            throw new NullPointerException("Component must be non-null");
        }

        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Width and height must be >= 0");
        }

        JViewport viewport = scrollpane.getViewport();
        Insets spInsets = scrollpane.getInsets();
        int y = spInsets.top;
        height = height - spInsets.top - spInsets.bottom;
        width = width - spInsets.left - spInsets.right;
        JViewport columnHeader = scrollpane.getColumnHeader();
        if (columnHeader != null && columnHeader.isVisible()) {
            Component header = columnHeader.getView();
            if (header != null && header.isVisible()) {
                // Header is always given it's preferred size.
                Dimension headerPref = header.getPreferredSize();
                int baseline = header.getBaseline(headerPref.width,
                                                  headerPref.height);
                if (baseline >= 0) {
                    return y + baseline;
                }
            }
            Dimension columnPref = columnHeader.getPreferredSize();
            height -= columnPref.height;
            y += columnPref.height;
        }
        Component view = (viewport == null) ? null : viewport.getView();
        if (view != null && view.isVisible() &&
                view.getBaselineResizeBehavior() ==
                Component.BaselineResizeBehavior.CONSTANT_ASCENT) {
            Border viewportBorder = scrollpane.getViewportBorder();
            if (viewportBorder != null) {
                Insets vpbInsets = viewportBorder.getBorderInsets(scrollpane);
                y += vpbInsets.top;
                height = height - vpbInsets.top - vpbInsets.bottom;
                width = width - vpbInsets.left - vpbInsets.right;
            }
            if (view.getWidth() > 0 && view.getHeight() > 0) {
                Dimension min = view.getMinimumSize();
                width = Math.max(min.width, view.getWidth());
                height = Math.max(min.height, view.getHeight());
            }
            if (width > 0 && height > 0) {
                int baseline = view.getBaseline(width, height);
                if (baseline > 0) {
                    return y + baseline;
                }
            }
        }
        return -1;
    }

    /**
     * Returns an enum indicating how the baseline of the component
     * changes as the size changes.
     *
     * @throws NullPointerException {@inheritDoc}
     * @see javax.swing.JComponent#getBaseline(int, int)
     * @since 1.6
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(
            JComponent c) {
        super.getBaselineResizeBehavior(c);
        // Baseline is either from the header, in which case it's always
        // the same size and therefor can be created as CONSTANT_ASCENT.
        // If the header doesn't have a baseline than the baseline will only
        // be valid if it's BaselineResizeBehavior is
        // CONSTANT_ASCENT, so, return CONSTANT_ASCENT.
        return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
    }


    /**
     * Listener for viewport events.
     * This class exists only for backward compatibility.
     * All its functionality has been moved into Handler.
     * @deprecated
     */
    @Deprecated(since = "17")
    public class ViewportChangeHandler implements ChangeListener
    {
        /**
         * Constructs a {@code ViewportChangeHandler}.
         */
        public ViewportChangeHandler() {}

        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.

        public void stateChanged(ChangeEvent e) {
            getHandler().stateChanged(e);
        }
    }

    /**
     * Returns an instance of viewport {@code ChangeListener}.
     *
     * @return an instance of viewport {@code ChangeListener}
     */
    protected ChangeListener createViewportChangeListener() {
        return getHandler();
    }


    /**
     * Horizontal scrollbar listener.
     * This class exists only for backward compatibility.
     * All its functionality has been moved into Handler.
     * @deprecated
     */
    @Deprecated(since = "17")
    public class HSBChangeListener implements ChangeListener
    {
        /**
         * Constructs a {@code HSBChangeListener}.
         */
        public HSBChangeListener() {}

        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.

        public void stateChanged(ChangeEvent e)
        {
            getHandler().stateChanged(e);
        }
    }

    /**
     * Returns a <code>PropertyChangeListener</code> that will be installed
     * on the horizontal <code>JScrollBar</code>.
     */
    private PropertyChangeListener createHSBPropertyChangeListener() {
        return getHandler();
    }

    /**
     * Returns an instance of horizontal scroll bar {@code ChangeListener}.
     *
     * @return an instance of horizontal scroll bar {@code ChangeListener}
     */
    protected ChangeListener createHSBChangeListener() {
        return getHandler();
    }


    /**
     * Vertical scrollbar listener.
     * This class exists only for backward compatibility.
     * All its functionality has been moved into Handler.
     * @deprecated
     */
    @Deprecated(since = "17")
    public class VSBChangeListener implements ChangeListener
    {
        /**
         * Constructs a {@code VSBChangeListener}.
         */
        public VSBChangeListener() {}

        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.

        public void stateChanged(ChangeEvent e)
        {
            getHandler().stateChanged(e);
        }
    }


    /**
     * Returns a <code>PropertyChangeListener</code> that will be installed
     * on the vertical <code>JScrollBar</code>.
     */
    private PropertyChangeListener createVSBPropertyChangeListener() {
        return getHandler();
    }

    /**
     * Returns an instance of vertical scroll bar {@code ChangeListener}.
     *
     * @return an instance of vertical scroll bar {@code ChangeListener}
     */
    protected ChangeListener createVSBChangeListener() {
        return getHandler();
    }

    /**
     * MouseWheelHandler is an inner class which implements the
     * MouseWheelListener interface.  MouseWheelHandler responds to
     * MouseWheelEvents by scrolling the JScrollPane appropriately.
     * If the scroll pane's
     * <code>isWheelScrollingEnabled</code>
     * method returns false, no scrolling occurs.
     *
     * @see javax.swing.JScrollPane#isWheelScrollingEnabled
     * @see #createMouseWheelListener
     * @see java.awt.event.MouseWheelListener
     * @see java.awt.event.MouseWheelEvent
     * @since 1.4
     */
    protected class MouseWheelHandler implements MouseWheelListener {

        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.

        /**
         * Constructs a {@code MouseWheelHandler}.
         */
        protected MouseWheelHandler() {}

        /**
         * Called when the mouse wheel is rotated while over a
         * JScrollPane.
         *
         * @param e     MouseWheelEvent to be handled
         * @since 1.4
         */
        public void mouseWheelMoved(MouseWheelEvent e) {
            getHandler().mouseWheelMoved(e);
        }
    }

    /**
     * Creates an instance of MouseWheelListener, which is added to the
     * JScrollPane by installUI().  The returned MouseWheelListener is used
     * to handle mouse wheel-driven scrolling.
     *
     * @return      MouseWheelListener which implements wheel-driven scrolling
     * @see #installUI
     * @see MouseWheelHandler
     * @since 1.4
     */
    protected MouseWheelListener createMouseWheelListener() {
        return getHandler();
    }

    /**
     * Updates a scroll bar display policy.
     *
     * @param e the property change event
     */
    protected void updateScrollBarDisplayPolicy(PropertyChangeEvent e) {
        scrollpane.revalidate();
        scrollpane.repaint();
    }

    /**
     * Updates viewport.
     *
     * @param e the property change event
     */
    protected void updateViewport(PropertyChangeEvent e)
    {
        JViewport oldViewport = (JViewport)(e.getOldValue());
        JViewport newViewport = (JViewport)(e.getNewValue());

        if (oldViewport != null) {
            oldViewport.removeChangeListener(viewportChangeListener);
        }

        if (newViewport != null) {
            Point p = newViewport.getViewPosition();
            if (scrollpane.getComponentOrientation().isLeftToRight()) {
                p.x = Math.max(p.x, 0);
            } else {
                int max = newViewport.getViewSize().width;
                int extent = newViewport.getExtentSize().width;
                if (extent > max) {
                    p.x = max - extent;
                } else {
                    p.x = Math.max(0, Math.min(max - extent, p.x));
                }
            }
            p.y = Math.max(p.y, 0);
            newViewport.setViewPosition(p);
            newViewport.addChangeListener(viewportChangeListener);
        }
    }

    /**
     * Updates row header.
     *
     * @param e the property change event
     */
    protected void updateRowHeader(PropertyChangeEvent e)
    {
        JViewport newRowHead = (JViewport)(e.getNewValue());
        if (newRowHead != null) {
            JViewport viewport = scrollpane.getViewport();
            Point p = newRowHead.getViewPosition();
            p.y = (viewport != null) ? viewport.getViewPosition().y : 0;
            newRowHead.setViewPosition(p);
        }
    }

    /**
     * Updates column header.
     *
     * @param e the property change event
     */
    protected void updateColumnHeader(PropertyChangeEvent e)
    {
        JViewport newColHead = (JViewport)(e.getNewValue());
        if (newColHead != null) {
            JViewport viewport = scrollpane.getViewport();
            Point p = newColHead.getViewPosition();
            if (viewport == null) {
                p.x = 0;
            } else {
                if (scrollpane.getComponentOrientation().isLeftToRight()) {
                    p.x = viewport.getViewPosition().x;
                } else {
                    p.x = Math.max(0, viewport.getViewPosition().x);
                }
            }
            newColHead.setViewPosition(p);
            scrollpane.add(newColHead, COLUMN_HEADER);
        }
    }

    private void updateHorizontalScrollBar(PropertyChangeEvent pce) {
        updateScrollBar(pce, hsbChangeListener, hsbPropertyChangeListener);
    }

    private void updateVerticalScrollBar(PropertyChangeEvent pce) {
        updateScrollBar(pce, vsbChangeListener, vsbPropertyChangeListener);
    }

    private void updateScrollBar(PropertyChangeEvent pce, ChangeListener cl,
                                 PropertyChangeListener pcl) {
        JScrollBar sb = (JScrollBar)pce.getOldValue();
        if (sb != null) {
            if (cl != null) {
                sb.getModel().removeChangeListener(cl);
            }
            if (pcl != null) {
                sb.removePropertyChangeListener(pcl);
            }
        }
        sb = (JScrollBar)pce.getNewValue();
        if (sb != null) {
            if (cl != null) {
                sb.getModel().addChangeListener(cl);
            }
            if (pcl != null) {
                sb.addPropertyChangeListener(pcl);
            }
        }
    }

    /**
     * Property change handler.
     * This class exists only for backward compatibility.
     * All its functionality has been moved into Handler.
     * @deprecated
     */
    @Deprecated(since = "17")
    public class PropertyChangeHandler implements PropertyChangeListener
    {
        /**
         * Constructs a {@code PropertyChangeHandler}.
         */
        public PropertyChangeHandler() {}

        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.

        /**
         * {@inheritDoc}
         */
        public void propertyChange(PropertyChangeEvent e)
        {
            getHandler().propertyChange(e);
        }
    }



    /**
     * Creates an instance of {@code PropertyChangeListener} that's added to
     * the {@code JScrollPane} by {@code installUI()}. Subclasses can override
     * this method to return a custom {@code PropertyChangeListener}, e.g.
     * <pre>
     * class MyScrollPaneUI extends BasicScrollPaneUI {
     *    protected PropertyChangeListener <b>createPropertyChangeListener</b>() {
     *        return new MyPropertyChangeListener();
     *    }
     *    public class MyPropertyChangeListener extends PropertyChangeListener {
     *        public void propertyChange(PropertyChangeEvent e) {
     *            if (e.getPropertyName().equals("viewport")) {
     *                // do some extra work when the viewport changes
     *            }
     *            super.propertyChange(e);
     *        }
     *    }
     * }
     * </pre>
     *
     * @return an instance of {@code PropertyChangeListener}
     *
     * @see java.beans.PropertyChangeListener
     * @see #installUI
     */
    protected PropertyChangeListener createPropertyChangeListener() {
        return getHandler();
    }


    private static class Actions extends UIAction {
        private static final String SCROLL_UP = "scrollUp";
        private static final String SCROLL_DOWN = "scrollDown";
        private static final String SCROLL_HOME = "scrollHome";
        private static final String SCROLL_END = "scrollEnd";
        private static final String UNIT_SCROLL_UP = "unitScrollUp";
        private static final String UNIT_SCROLL_DOWN = "unitScrollDown";
        private static final String SCROLL_LEFT = "scrollLeft";
        private static final String SCROLL_RIGHT = "scrollRight";
        private static final String UNIT_SCROLL_LEFT = "unitScrollLeft";
        private static final String UNIT_SCROLL_RIGHT = "unitScrollRight";


        Actions(String key) {
            super(key);
        }

        public void actionPerformed(ActionEvent e) {
            JScrollPane scrollPane = (JScrollPane)e.getSource();
            boolean ltr = scrollPane.getComponentOrientation().isLeftToRight();
            String key = getName();

            if (key == SCROLL_UP) {
                scroll(scrollPane, SwingConstants.VERTICAL, -1, true);
            }
            else if (key == SCROLL_DOWN) {
                scroll(scrollPane, SwingConstants.VERTICAL, 1, true);
            }
            else if (key == SCROLL_HOME) {
                scrollHome(scrollPane);
            }
            else if (key == SCROLL_END) {
                scrollEnd(scrollPane);
            }
            else if (key == UNIT_SCROLL_UP) {
                scroll(scrollPane, SwingConstants.VERTICAL, -1, false);
            }
            else if (key == UNIT_SCROLL_DOWN) {
                scroll(scrollPane, SwingConstants.VERTICAL, 1, false);
            }
            else if (key == SCROLL_LEFT) {
                scroll(scrollPane, SwingConstants.HORIZONTAL, ltr ? -1 : 1,
                       true);
            }
            else if (key == SCROLL_RIGHT) {
                scroll(scrollPane, SwingConstants.HORIZONTAL, ltr ? 1 : -1,
                       true);
            }
            else if (key == UNIT_SCROLL_LEFT) {
                scroll(scrollPane, SwingConstants.HORIZONTAL, ltr ? -1 : 1,
                       false);
            }
            else if (key == UNIT_SCROLL_RIGHT) {
                scroll(scrollPane, SwingConstants.HORIZONTAL, ltr ? 1 : -1,
                       false);
            }
        }

        private void scrollEnd(JScrollPane scrollpane) {
            JViewport vp = scrollpane.getViewport();
            Component view;
            if (vp != null && (view = vp.getView()) != null) {
                Rectangle visRect = vp.getViewRect();
                Rectangle bounds = view.getBounds();
                if (scrollpane.getComponentOrientation().isLeftToRight()) {
                    vp.setViewPosition(new Point(bounds.width - visRect.width,
                                             bounds.height - visRect.height));
                } else {
                    vp.setViewPosition(new Point(0,
                                             bounds.height - visRect.height));
                }
            }
        }

        private void scrollHome(JScrollPane scrollpane) {
            JViewport vp = scrollpane.getViewport();
            Component view;
            if (vp != null && (view = vp.getView()) != null) {
                if (scrollpane.getComponentOrientation().isLeftToRight()) {
                    vp.setViewPosition(new Point(0, 0));
                } else {
                    Rectangle visRect = vp.getViewRect();
                    Rectangle bounds = view.getBounds();
                    vp.setViewPosition(new Point(bounds.width - visRect.width, 0));
                }
            }
        }

        private void scroll(JScrollPane scrollpane, int orientation,
                            int direction, boolean block) {
            JViewport vp = scrollpane.getViewport();
            Component view;
            if (vp != null && (view = vp.getView()) != null) {
                Rectangle visRect = vp.getViewRect();
                Dimension vSize = view.getSize();
                int amount;

                if (view instanceof Scrollable) {
                    if (block) {
                        amount = ((Scrollable)view).getScrollableBlockIncrement
                                 (visRect, orientation, direction);
                    }
                    else {
                        amount = ((Scrollable)view).getScrollableUnitIncrement
                                 (visRect, orientation, direction);
                    }
                }
                else {
                    if (block) {
                        if (orientation == SwingConstants.VERTICAL) {
                            amount = visRect.height;
                        }
                        else {
                            amount = visRect.width;
                        }
                    }
                    else {
                        amount = 10;
                    }
                }
                if (orientation == SwingConstants.VERTICAL) {
                    visRect.y += (amount * direction);
                    if ((visRect.y + visRect.height) > vSize.height) {
                        visRect.y = Math.max(0, vSize.height - visRect.height);
                    }
                    else if (visRect.y < 0) {
                        visRect.y = 0;
                    }
                }
                else {
                    if (scrollpane.getComponentOrientation().isLeftToRight()) {
                        visRect.x += (amount * direction);
                        if ((visRect.x + visRect.width) > vSize.width) {
                            visRect.x = Math.max(0, vSize.width - visRect.width);
                        } else if (visRect.x < 0) {
                            visRect.x = 0;
                        }
                    } else {
                        visRect.x -= (amount * direction);
                        if (visRect.width > vSize.width) {
                            visRect.x = vSize.width - visRect.width;
                        } else {
                            visRect.x = Math.max(0, Math.min(vSize.width - visRect.width, visRect.x));
                        }
                    }
                }
                vp.setViewPosition(visRect.getLocation());
            }
        }
    }


    class Handler implements ChangeListener, PropertyChangeListener, MouseWheelListener {
        //
        // MouseWheelListener
        //
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (scrollpane.isWheelScrollingEnabled() &&
                e.getWheelRotation() != 0) {
                JScrollBar toScroll = scrollpane.getVerticalScrollBar();
                int direction = e.getWheelRotation() < 0 ? -1 : 1;
                int orientation = SwingConstants.VERTICAL;

                // find which scrollbar to scroll, or return if none
                if (toScroll == null || !toScroll.isVisible()
                        || e.isShiftDown()) {
                    toScroll = scrollpane.getHorizontalScrollBar();
                    if (toScroll == null || !toScroll.isVisible()) {
                        return;
                    }
                    orientation = SwingConstants.HORIZONTAL;
                }

                e.consume();

                if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                    JViewport vp = scrollpane.getViewport();
                    if (vp == null) { return; }
                    Component comp = vp.getView();
                    int units = Math.abs(e.getUnitsToScroll());

                    // When the scrolling speed is set to maximum, it's possible
                    // for a single wheel click to scroll by more units than
                    // will fit in the visible area.  This makes it
                    // hard/impossible to get to certain parts of the scrolling
                    // Component with the wheel.  To make for more accurate
                    // low-speed scrolling, we limit scrolling to the block
                    // increment if the wheel was only rotated one click.
                    boolean limitScroll = Math.abs(e.getWheelRotation()) == 1;

                    // Check if we should use the visibleRect trick
                    Object fastWheelScroll = toScroll.getClientProperty(
                                               "JScrollBar.fastWheelScrolling");
                    if (Boolean.TRUE == fastWheelScroll &&
                        comp instanceof Scrollable) {
                        // 5078454: Under maximum acceleration, we may scroll
                        // by many 100s of units in ~1 second.
                        //
                        // BasicScrollBarUI.scrollByUnits() can bog down the EDT
                        // with repaints in this situation.  However, the
                        // Scrollable interface allows us to pass in an
                        // arbitrary visibleRect.  This allows us to accurately
                        // calculate the total scroll amount, and then update
                        // the GUI once.  This technique provides much faster
                        // accelerated wheel scrolling.
                        Scrollable scrollComp = (Scrollable) comp;
                        Rectangle viewRect = vp.getViewRect();
                        int startingX = viewRect.x;
                        boolean leftToRight =
                                 comp.getComponentOrientation().isLeftToRight();
                        int scrollMin = toScroll.getMinimum();
                        int scrollMax = toScroll.getMaximum() -
                                        toScroll.getModel().getExtent();

                        if (limitScroll) {
                            int blockIncr =
                                scrollComp.getScrollableBlockIncrement(viewRect,
                                                                    orientation,
                                                                    direction);
                            if (direction < 0) {
                                scrollMin = Math.max(scrollMin,
                                               toScroll.getValue() - blockIncr);
                            }
                            else {
                                scrollMax = Math.min(scrollMax,
                                               toScroll.getValue() + blockIncr);
                            }
                        }

                        for (int i = 0; i < units; i++) {
                            int unitIncr =
                                scrollComp.getScrollableUnitIncrement(viewRect,
                                                        orientation, direction);
                            // Modify the visible rect for the next unit, and
                            // check to see if we're at the end already.
                            if (orientation == SwingConstants.VERTICAL) {
                                if (direction < 0) {
                                    viewRect.y -= unitIncr;
                                    if (viewRect.y <= scrollMin) {
                                        viewRect.y = scrollMin;
                                        break;
                                    }
                                }
                                else { // (direction > 0
                                    viewRect.y += unitIncr;
                                    if (viewRect.y >= scrollMax) {
                                        viewRect.y = scrollMax;
                                        break;
                                    }
                                }
                            }
                            else {
                                // Scroll left
                                if ((leftToRight && direction < 0) ||
                                    (!leftToRight && direction > 0)) {
                                    viewRect.x -= unitIncr;
                                    if (leftToRight) {
                                        if (viewRect.x < scrollMin) {
                                            viewRect.x = scrollMin;
                                            break;
                                        }
                                    }
                                }
                                // Scroll right
                                else if ((leftToRight && direction > 0) ||
                                    (!leftToRight && direction < 0)) {
                                    viewRect.x += unitIncr;
                                    if (leftToRight) {
                                        if (viewRect.x > scrollMax) {
                                            viewRect.x = scrollMax;
                                            break;
                                        }
                                    }
                                }
                                else {
                                    assert false : "Non-sensical ComponentOrientation / scroll direction";
                                }
                            }
                        }
                        // Set the final view position on the ScrollBar
                        if (orientation == SwingConstants.VERTICAL) {
                            toScroll.setValue(viewRect.y);
                        }
                        else {
                            if (leftToRight) {
                                toScroll.setValue(viewRect.x);
                            }
                            else {
                                // rightToLeft scrollbars are oriented with
                                // minValue on the right and maxValue on the
                                // left.
                                int newPos = toScroll.getValue() -
                                                       (viewRect.x - startingX);
                                if (newPos < scrollMin) {
                                    newPos = scrollMin;
                                }
                                else if (newPos > scrollMax) {
                                    newPos = scrollMax;
                                }
                                toScroll.setValue(newPos);
                            }
                        }
                    }
                    else {
                        // Viewport's view is not a Scrollable, or fast wheel
                        // scrolling is not enabled.
                        BasicScrollBarUI.scrollByUnits(toScroll, direction,
                                                       units, limitScroll);
                    }
                }
                else if (e.getScrollType() ==
                         MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
                    BasicScrollBarUI.scrollByBlock(toScroll, direction);
                }
            }
        }

        //
        // ChangeListener: This is added to the vieport, and hsb/vsb models.
        //
        public void stateChanged(ChangeEvent e) {
            JViewport viewport = scrollpane.getViewport();

            if (viewport != null) {
                if (e.getSource() == viewport) {
                    syncScrollPaneWithViewport();
                }
                else {
                    JScrollBar hsb = scrollpane.getHorizontalScrollBar();
                    if (hsb != null && e.getSource() == hsb.getModel()) {
                        hsbStateChanged(viewport, e);
                    }
                    else {
                        JScrollBar vsb = scrollpane.getVerticalScrollBar();
                        if (vsb != null && e.getSource() == vsb.getModel()) {
                            vsbStateChanged(viewport, e);
                        }
                    }
                }
            }
        }

        private void vsbStateChanged(JViewport viewport, ChangeEvent e) {
            BoundedRangeModel model = (BoundedRangeModel)(e.getSource());
            Point p = viewport.getViewPosition();
            p.y = model.getValue();
            viewport.setViewPosition(p);
        }

        private void hsbStateChanged(JViewport viewport, ChangeEvent e) {
            BoundedRangeModel model = (BoundedRangeModel)(e.getSource());
            Point p = viewport.getViewPosition();
            int value = model.getValue();
            if (scrollpane.getComponentOrientation().isLeftToRight()) {
                p.x = value;
            } else {
                int max = viewport.getViewSize().width;
                int extent = viewport.getExtentSize().width;
                int oldX = p.x;

                /* Set new X coordinate based on "value".
                 */
                p.x = max - extent - value;

                /* If setValue() was called before "extent" was fixed,
                 * turn setValueCalled flag on.
                 */
                if ((extent == 0) && (value != 0) && (oldX == max)) {
                    setValueCalled = true;
                } else {
                    /* When a pane without a horizontal scroll bar was
                     * reduced and the bar appeared, the viewport should
                     * show the right side of the view.
                     */
                    if ((extent != 0) && (oldX < 0) && (p.x == 0)) {
                        p.x += value;
                    }
                }
            }
            viewport.setViewPosition(p);
        }

        //
        // PropertyChangeListener: This is installed on both the JScrollPane
        // and the horizontal/vertical scrollbars.
        //

        // Listens for changes in the model property and reinstalls the
        // horizontal/vertical PropertyChangeListeners.
        public void propertyChange(PropertyChangeEvent e) {
            if (e.getSource() == scrollpane) {
                scrollPanePropertyChange(e);
            }
            else {
                sbPropertyChange(e);
            }
        }

        private void scrollPanePropertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();

            if (propertyName == "verticalScrollBarDisplayPolicy") {
                updateScrollBarDisplayPolicy(e);
            }
            else if (propertyName == "horizontalScrollBarDisplayPolicy") {
                updateScrollBarDisplayPolicy(e);
            }
            else if (propertyName == "viewport") {
                updateViewport(e);
            }
            else if (propertyName == "rowHeader") {
                updateRowHeader(e);
            }
            else if (propertyName == "columnHeader") {
                updateColumnHeader(e);
            }
            else if (propertyName == "verticalScrollBar") {
                updateVerticalScrollBar(e);
            }
            else if (propertyName == "horizontalScrollBar") {
                updateHorizontalScrollBar(e);
            }
            else if (propertyName == "componentOrientation") {
                scrollpane.revalidate();
                scrollpane.repaint();
            }
        }

        // PropertyChangeListener for the horizontal and vertical scrollbars.
        private void sbPropertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            Object source = e.getSource();

            if ("model" == propertyName) {
                JScrollBar sb = scrollpane.getVerticalScrollBar();
                BoundedRangeModel oldModel = (BoundedRangeModel)e.
                                     getOldValue();
                ChangeListener cl = null;

                if (source == sb) {
                    cl = vsbChangeListener;
                }
                else if (source == scrollpane.getHorizontalScrollBar()) {
                    sb = scrollpane.getHorizontalScrollBar();
                    cl = hsbChangeListener;
                }
                if (cl != null) {
                    if (oldModel != null) {
                        oldModel.removeChangeListener(cl);
                    }
                    if (sb.getModel() != null) {
                        sb.getModel().addChangeListener(cl);
                    }
                }
            }
            else if ("componentOrientation" == propertyName) {
                if (source == scrollpane.getHorizontalScrollBar()) {
                    JScrollBar hsb = scrollpane.getHorizontalScrollBar();
                    JViewport viewport = scrollpane.getViewport();
                    Point p = viewport.getViewPosition();
                    if (scrollpane.getComponentOrientation().isLeftToRight()) {
                        p.x = hsb.getValue();
                    } else {
                        p.x = viewport.getViewSize().width - viewport.getExtentSize().width - hsb.getValue();
                    }
                    viewport.setViewPosition(p);
                }
            }
        }
    }
}
