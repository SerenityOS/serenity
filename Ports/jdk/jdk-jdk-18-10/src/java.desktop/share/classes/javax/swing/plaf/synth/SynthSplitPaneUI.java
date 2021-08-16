/*
 * Copyright (c) 2002, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.synth;


import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;


/**
 * Provides the Synth L&amp;F UI delegate for
 * {@link javax.swing.JSplitPane}.
 *
 * @author Scott Violet
 * @since 1.7
 */
public class SynthSplitPaneUI extends BasicSplitPaneUI
                              implements PropertyChangeListener, SynthUI {
    /**
     * Keys to use for forward focus traversal when the JComponent is
     * managing focus.
     */
    private Set<KeyStroke> managingFocusForwardTraversalKeys;

    /**
     * Keys to use for backward focus traversal when the JComponent is
     * managing focus.
     */
    private Set<KeyStroke> managingFocusBackwardTraversalKeys;

    /**
     * Style for the JSplitPane.
     */
    private SynthStyle style;
    /**
     * Style for the divider.
     */
    private SynthStyle dividerStyle;

    /**
     *
     * Constructs a {@code SynthSplitPaneUI}.
     */
    public SynthSplitPaneUI() {}

    /**
     * Creates a new SynthSplitPaneUI instance
     *
     * @param x component to create UI object for
     * @return the UI object
     */
    public static ComponentUI createUI(JComponent x) {
        return new SynthSplitPaneUI();
    }

    /**
     * Installs the UI defaults.
     */
    @Override
    @SuppressWarnings("deprecation")
    protected void installDefaults() {
        updateStyle(splitPane);

        setOrientation(splitPane.getOrientation());
        setContinuousLayout(splitPane.isContinuousLayout());

        resetLayoutManager();

        /* Install the nonContinuousLayoutDivider here to avoid having to
        add/remove everything later. */
        if(nonContinuousLayoutDivider == null) {
            setNonContinuousLayoutDivider(
                                createDefaultNonContinuousLayoutDivider(),
                                true);
        } else {
            setNonContinuousLayoutDivider(nonContinuousLayoutDivider, true);
        }

        // focus forward traversal key
        if (managingFocusForwardTraversalKeys==null) {
            managingFocusForwardTraversalKeys = new HashSet<KeyStroke>();
            managingFocusForwardTraversalKeys.add(
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        }
        splitPane.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                                        managingFocusForwardTraversalKeys);
        // focus backward traversal key
        if (managingFocusBackwardTraversalKeys==null) {
            managingFocusBackwardTraversalKeys = new HashSet<KeyStroke>();
            managingFocusBackwardTraversalKeys.add(
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
        }
        splitPane.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                                        managingFocusBackwardTraversalKeys);
    }

    private void updateStyle(JSplitPane splitPane) {
        SynthContext context = getContext(splitPane, Region.SPLIT_PANE_DIVIDER,
                                          ENABLED);
        SynthStyle oldDividerStyle = dividerStyle;
        dividerStyle = SynthLookAndFeel.updateStyle(context, this);

        context = getContext(splitPane, ENABLED);
        SynthStyle oldStyle = style;

        style = SynthLookAndFeel.updateStyle(context, this);

        if (style != oldStyle) {
            Object value = style.get(context, "SplitPane.size");
            if (value == null) {
                value = Integer.valueOf(6);
            }
            LookAndFeel.installProperty(splitPane, "dividerSize", value);
            dividerSize = ((Number)value).intValue();

            value = style.get(context, "SplitPane.oneTouchExpandable");
            if (value != null) {
                LookAndFeel.installProperty(splitPane, "oneTouchExpandable", value);
            }

            if (divider != null) {
                splitPane.remove(divider);
                divider.setDividerSize(splitPane.getDividerSize());
            }
            if (oldStyle != null) {
                uninstallKeyboardActions();
                installKeyboardActions();
            }
        }
        if (style != oldStyle || dividerStyle != oldDividerStyle) {
            // Only way to force BasicSplitPaneDivider to reread the
            // necessary properties.
            if (divider != null) {
                splitPane.remove(divider);
            }
            divider = createDefaultDivider();
            divider.setBasicSplitPaneUI(this);
            splitPane.add(divider, JSplitPane.DIVIDER);
        }
    }

    /**
     * Installs the event listeners for the UI.
     */
    @Override
    protected void installListeners() {
        super.installListeners();
        splitPane.addPropertyChangeListener(this);
    }

    /**
     * Uninstalls the UI defaults.
     */
    @Override
    protected void uninstallDefaults() {
        SynthContext context = getContext(splitPane, ENABLED);

        style.uninstallDefaults(context);
        style = null;

        context = getContext(splitPane, Region.SPLIT_PANE_DIVIDER, ENABLED);
        dividerStyle.uninstallDefaults(context);
        dividerStyle = null;

        super.uninstallDefaults();
    }


    /**
     * Uninstalls the event listeners from the UI.
     */
    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();
        splitPane.removePropertyChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SynthContext getContext(JComponent c) {
        return getContext(c, SynthLookAndFeel.getComponentState(c));
    }

    private SynthContext getContext(JComponent c, int state) {
        return SynthContext.getContext(c, style, state);
    }

    SynthContext getContext(JComponent c, Region region) {
        return getContext(c, region, getComponentState(c, region));
    }

    private SynthContext getContext(JComponent c, Region region, int state) {
        if (region == Region.SPLIT_PANE_DIVIDER) {
            return SynthContext.getContext(c, region, dividerStyle, state);
        }
        return SynthContext.getContext(c, region, style, state);
    }

    private int getComponentState(JComponent c, Region subregion) {
        int state = SynthLookAndFeel.getComponentState(c);

        if (divider.isMouseOver()) {
            state |= MOUSE_OVER;
        }
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (SynthLookAndFeel.shouldUpdateStyle(e)) {
            updateStyle((JSplitPane)e.getSource());
        }
    }

    /**
     * Creates the default divider.
     */
    @Override
    public BasicSplitPaneDivider createDefaultDivider() {
        SynthSplitPaneDivider divider = new SynthSplitPaneDivider(this);

        divider.setDividerSize(splitPane.getDividerSize());
        return divider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("serial") // anonymous class
    protected Component createDefaultNonContinuousLayoutDivider() {
        return new Canvas() {
            public void paint(Graphics g) {
                paintDragDivider(g, 0, 0, getWidth(), getHeight());
            }
        };
    }

    /**
     * Notifies this UI delegate to repaint the specified component.
     * This method paints the component background, then calls
     * the {@link #paint(SynthContext,Graphics)} method.
     *
     * <p>In general, this method does not need to be overridden by subclasses.
     * All Look and Feel rendering code should reside in the {@code paint} method.
     *
     * @param g the {@code Graphics} object used for painting
     * @param c the component being painted
     * @see #paint(SynthContext,Graphics)
     */
    @Override
    public void update(Graphics g, JComponent c) {
        SynthContext context = getContext(c);

        SynthLookAndFeel.update(context, g);
        context.getPainter().paintSplitPaneBackground(context,
                          g, 0, 0, c.getWidth(), c.getHeight());
        paint(context, g);
    }

    /**
     * Paints the specified component according to the Look and Feel.
     * <p>This method is not used by Synth Look and Feel.
     * Painting is handled by the {@link #paint(SynthContext,Graphics)} method.
     *
     * @param g the {@code Graphics} object used for painting
     * @param c the component being painted
     * @see #paint(SynthContext,Graphics)
     */
    @Override
    public void paint(Graphics g, JComponent c) {
        SynthContext context = getContext(c);

        paint(context, g);
    }

    /**
     * Paints the specified component. This implementation does nothing.
     *
     * @param context context for the component being painted
     * @param g the {@code Graphics} object used for painting
     * @see #update(Graphics,JComponent)
     */
    protected void paint(SynthContext context, Graphics g) {
        // This is done to update package private variables in
        // BasicSplitPaneUI
        super.paint(g, splitPane);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintBorder(SynthContext context, Graphics g, int x,
                            int y, int w, int h) {
        context.getPainter().paintSplitPaneBorder(context, g, x, y, w, h);
    }

    private void paintDragDivider(Graphics g, int x, int y, int w, int h) {
        SynthContext context = getContext(splitPane,Region.SPLIT_PANE_DIVIDER);
        context.setComponentState(((context.getComponentState() | MOUSE_OVER) ^
                                   MOUSE_OVER) | PRESSED);
        Shape oldClip = g.getClip();
        g.clipRect(x, y, w, h);
        context.getPainter().paintSplitPaneDragDivider(context, g, x, y, w, h,
                                           splitPane.getOrientation());
        g.setClip(oldClip);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishedPaintingChildren(JSplitPane jc, Graphics g) {
        if(jc == splitPane && getLastDragLocation() != -1 &&
                              !isContinuousLayout() && !draggingHW) {
            if(jc.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                paintDragDivider(g, getLastDragLocation(), 0, dividerSize - 1,
                                 splitPane.getHeight() - 1);
            } else {
                paintDragDivider(g, 0, getLastDragLocation(),
                                 splitPane.getWidth() - 1, dividerSize - 1);
            }
        }
    }
}
