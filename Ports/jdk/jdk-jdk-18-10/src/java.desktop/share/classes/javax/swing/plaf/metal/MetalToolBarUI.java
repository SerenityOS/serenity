/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.metal;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.lang.ref.WeakReference;
import java.util.*;

import java.beans.PropertyChangeListener;

import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

/**
 * A Metal Look and Feel implementation of ToolBarUI.  This implementation
 * is a "combined" view/controller.
 *
 * @author Jeff Shapiro
 */
public class MetalToolBarUI extends BasicToolBarUI
{
    /**
     * An array of WeakReferences that point to JComponents. This will contain
     * instances of JToolBars and JMenuBars and is used to find
     * JToolBars/JMenuBars that border each other.
     */
    private static List<WeakReference<JComponent>> components = new ArrayList<WeakReference<JComponent>>();

    /**
     * This protected field is implementation specific. Do not access directly
     * or override. Use the create method instead.
     *
     * @see #createContainerListener
     */
    protected ContainerListener contListener;

    /**
     * This protected field is implementation specific. Do not access directly
     * or override. Use the create method instead.
     *
     * @see #createRolloverListener
     */
    protected PropertyChangeListener rolloverListener;

    private static Border nonRolloverBorder;

    /**
     * Last menubar the toolbar touched.  This is only useful for ocean.
     */
    private JMenuBar lastMenuBar;

    /**
     * Constructs a {@code MetalToolBarUI}.
     */
    public MetalToolBarUI() {}

    /**
     * Registers the specified component.
     */
    static synchronized void register(JComponent c) {
        if (c == null) {
            // Exception is thrown as convenience for callers that are
            // typed to throw an NPE.
            throw new NullPointerException("JComponent must be non-null");
        }
        components.add(new WeakReference<JComponent>(c));
    }

    /**
     * Unregisters the specified component.
     */
    static synchronized void unregister(JComponent c) {
        for (int counter = components.size() - 1; counter >= 0; counter--) {
            // Search for the component, removing any flushed references
            // along the way.
            JComponent target = components.get(counter).get();

            if (target == c || target == null) {
                components.remove(counter);
            }
        }
    }

    /**
     * Finds a previously registered component of class <code>target</code>
     * that shares the JRootPane ancestor of <code>from</code>.
     */
    static synchronized Object findRegisteredComponentOfType(JComponent from,
                                                             Class<?> target) {
        JRootPane rp = SwingUtilities.getRootPane(from);
        if (rp != null) {
            for (int counter = components.size() - 1; counter >= 0; counter--){
                Object component = ((WeakReference)components.get(counter)).
                                   get();

                if (component == null) {
                    // WeakReference has gone away, remove the WeakReference
                    components.remove(counter);
                }
                else if (target.isInstance(component) && SwingUtilities.
                         getRootPane((Component)component) == rp) {
                    return component;
                }
            }
        }
        return null;
    }

    /**
     * Returns true if the passed in JMenuBar is above a horizontal
     * JToolBar.
     */
    static boolean doesMenuBarBorderToolBar(JMenuBar c) {
        JToolBar tb = (JToolBar)MetalToolBarUI.
                    findRegisteredComponentOfType(c, JToolBar.class);
        if (tb != null && tb.getOrientation() == JToolBar.HORIZONTAL) {
            JRootPane rp = SwingUtilities.getRootPane(c);
            Point point = new Point(0, 0);
            point = SwingUtilities.convertPoint(c, point, rp);
            int menuX = point.x;
            int menuY = point.y;
            point.x = point.y = 0;
            point = SwingUtilities.convertPoint(tb, point, rp);
            return (point.x == menuX && menuY + c.getHeight() == point.y &&
                    c.getWidth() == tb.getWidth());
        }
        return false;
    }

    /**
     * Constructs an instance of {@code MetalToolBarUI}.
     *
     * @param c a component
     * @return an instance of {@code MetalToolBarUI}
     */
    public static ComponentUI createUI( JComponent c )
    {
        return new MetalToolBarUI();
    }

    public void installUI( JComponent c )
    {
        super.installUI( c );
        register(c);
    }

    public void uninstallUI( JComponent c )
    {
        super.uninstallUI( c );
        nonRolloverBorder = null;
        unregister(c);
    }

    protected void installListeners() {
        super.installListeners();

        contListener = createContainerListener();
        if (contListener != null) {
            toolBar.addContainerListener(contListener);
        }
        rolloverListener = createRolloverListener();
        if (rolloverListener != null) {
            toolBar.addPropertyChangeListener(rolloverListener);
        }
    }

    protected void uninstallListeners() {
        super.uninstallListeners();

        if (contListener != null) {
            toolBar.removeContainerListener(contListener);
        }
        rolloverListener = createRolloverListener();
        if (rolloverListener != null) {
            toolBar.removePropertyChangeListener(rolloverListener);
        }
    }

    protected Border createRolloverBorder() {
        return super.createRolloverBorder();
    }

    protected Border createNonRolloverBorder() {
        return super.createNonRolloverBorder();
    }


    /**
     * Creates a non rollover border for Toggle buttons in the toolbar.
     */
    private Border createNonRolloverToggleBorder() {
        return createNonRolloverBorder();
    }

    protected void setBorderToNonRollover(Component c) {
        if (c instanceof JToggleButton && !(c instanceof JCheckBox)) {
            // 4735514, 4886944: The method createNonRolloverToggleBorder() is
            // private in BasicToolBarUI so we can't override it. We still need
            // to call super from this method so that it can save away the
            // original border and then we install ours.

            // Before calling super we get a handle to the old border, because
            // super will install a non-UIResource border that we can't
            // distinguish from one provided by an application.
            JToggleButton b = (JToggleButton)c;
            Border border = b.getBorder();
            super.setBorderToNonRollover(c);
            if (border instanceof UIResource) {
                if (nonRolloverBorder == null) {
                    nonRolloverBorder = createNonRolloverToggleBorder();
                }
                b.setBorder(nonRolloverBorder);
            }
        } else {
            super.setBorderToNonRollover(c);
        }
    }


    /**
     * Creates a container listener that will be added to the JToolBar.
     * If this method returns null then it will not be added to the
     * toolbar.
     *
     * @return an instance of a <code>ContainerListener</code> or null
     */
    protected ContainerListener createContainerListener() {
        return null;
    }

    /**
     * Creates a property change listener that will be added to the JToolBar.
     * If this method returns null then it will not be added to the
     * toolbar.
     *
     * @return an instance of a <code>PropertyChangeListener</code> or null
     */
    protected PropertyChangeListener createRolloverListener() {
        return null;
    }

    protected MouseInputListener createDockingListener( )
    {
        return new MetalDockingListener( toolBar );
    }

    /**
     * Sets the offset of the mouse cursor inside the DragWindow.
     *
     * @param p the offset
     */
    protected void setDragOffset(Point p) {
        if (!GraphicsEnvironment.isHeadless()) {
            if (dragWindow == null) {
                dragWindow = createDragWindow(toolBar);
            }
            dragWindow.setOffset(p);
        }
    }

    /**
     * If necessary paints the background of the component, then invokes
     * <code>paint</code>.
     *
     * @param g Graphics to paint to
     * @param c JComponent painting on
     * @throws NullPointerException if <code>g</code> or <code>c</code> is
     *         null
     * @see javax.swing.plaf.ComponentUI#update
     * @see javax.swing.plaf.ComponentUI#paint
     * @since 1.5
     */
    public void update(Graphics g, JComponent c) {
        if (g == null) {
            throw new NullPointerException("graphics must be non-null");
        }
        if (c.isOpaque() && (c.getBackground() instanceof UIResource) &&
                            ((JToolBar)c).getOrientation() ==
                      JToolBar.HORIZONTAL && UIManager.get(
                     "MenuBar.gradient") != null) {
            JRootPane rp = SwingUtilities.getRootPane(c);
            JMenuBar mb = (JMenuBar)findRegisteredComponentOfType(
                                    c, JMenuBar.class);
            if (mb != null && mb.isOpaque() &&
                              (mb.getBackground() instanceof UIResource)) {
                Point point = new Point(0, 0);
                point = SwingUtilities.convertPoint(c, point, rp);
                int x = point.x;
                int y = point.y;
                point.x = point.y = 0;
                point = SwingUtilities.convertPoint(mb, point, rp);
                if (point.x == x && y == point.y + mb.getHeight() &&
                     mb.getWidth() == c.getWidth() &&
                     MetalUtils.drawGradient(c, g, "MenuBar.gradient",
                     0, -mb.getHeight(), c.getWidth(), c.getHeight() +
                     mb.getHeight(), true)) {
                    setLastMenuBar(mb);
                    paint(g, c);
                    return;
                }
            }
            if (MetalUtils.drawGradient(c, g, "MenuBar.gradient",
                           0, 0, c.getWidth(), c.getHeight(), true)) {
                setLastMenuBar(null);
                paint(g, c);
                return;
            }
        }
        setLastMenuBar(null);
        super.update(g, c);
    }

    private void setLastMenuBar(JMenuBar lastMenuBar) {
        if (MetalLookAndFeel.usingOcean()) {
            if (this.lastMenuBar != lastMenuBar) {
                // The menubar we previously touched has changed, force it
                // to repaint.
                if (this.lastMenuBar != null) {
                    this.lastMenuBar.repaint();
                }
                if (lastMenuBar != null) {
                    lastMenuBar.repaint();
                }
                this.lastMenuBar = lastMenuBar;
            }
        }
    }

    /**
     * No longer used. The class cannot be removed for compatibility reasons.
     */
    protected class MetalContainerListener
        extends BasicToolBarUI.ToolBarContListener {
        /**
         * Constructs a {@code MetalContainerListener}.
         */
        protected MetalContainerListener() {}
    }

    /**
     * No longer used. The class cannot be removed for compatibility reasons.
     */
    protected class MetalRolloverListener
        extends BasicToolBarUI.PropertyListener {
        /**
         * Constructs a {@code MetalRolloverListener}.
         */
        protected MetalRolloverListener() {}
    }

    /**
     * {@code DockingListener} for {@code MetalToolBarUI}.
     */
    protected class MetalDockingListener extends DockingListener {
        private boolean pressedInBumps = false;

        /**
         * Constructs the {@code MetalDockingListener}.
         *
         * @param t an instance of {@code JToolBar}
         */
        public MetalDockingListener(JToolBar t) {
            super(t);
        }

        public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
            if (!toolBar.isEnabled()) {
                return;
            }
            pressedInBumps = false;
            Rectangle bumpRect = new Rectangle();

            if (toolBar.getOrientation() == JToolBar.HORIZONTAL) {
                int x = MetalUtils.isLeftToRight(toolBar) ? 0 : toolBar.getSize().width-14;
                bumpRect.setBounds(x, 0, 14, toolBar.getSize().height);
            } else {  // vertical
                bumpRect.setBounds(0, 0, toolBar.getSize().width, 14);
            }
            if (bumpRect.contains(e.getPoint())) {
                pressedInBumps = true;
                Point dragOffset = e.getPoint();
                if (!MetalUtils.isLeftToRight(toolBar)) {
                    dragOffset.x -= (toolBar.getSize().width
                                     - toolBar.getPreferredSize().width);
                }
                setDragOffset(dragOffset);
            }
        }

        public void mouseDragged(MouseEvent e) {
            if (pressedInBumps) {
                super.mouseDragged(e);
            }
        }
    } // end class MetalDockingListener
}
