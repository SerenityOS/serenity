/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.apple.laf;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicRootPaneUI;

import com.apple.laf.AquaUtils.RecyclableSingleton;
import com.apple.laf.AquaUtils.RecyclableSingletonFromDefaultConstructor;

/**
 * From AquaRootPaneUI.java
 *
 * The JRootPane manages the default button.  There can be only one active rootpane,
 * and one default button, so we need only one timer
 *
 * AquaRootPaneUI is a singleton object
 */
public class AquaRootPaneUI extends BasicRootPaneUI implements AncestorListener, WindowListener, ContainerListener {
    private static final RecyclableSingleton<AquaRootPaneUI> sRootPaneUI = new RecyclableSingletonFromDefaultConstructor<AquaRootPaneUI>(AquaRootPaneUI.class);

    static final int kDefaultButtonPaintDelayBetweenFrames = 50;
    JButton fCurrentDefaultButton = null;
    Timer fTimer = null;
    static final boolean sUseScreenMenuBar = AquaMenuBarUI.getScreenMenuBarProperty();

    public static ComponentUI createUI(final JComponent c) {
        return sRootPaneUI.get();
    }

    public void installUI(final JComponent c) {
        super.installUI(c);
        c.addAncestorListener(this);

        if (c.isShowing() && c.isEnabled()) {
            updateDefaultButton((JRootPane)c);
        }

        // for <rdar://problem/3689020> REGR: Realtime LAF updates no longer work
        //
        // because the JFrame parent has a LAF background set (why without a UI element I don't know!)
        // we have to set it from the root pane so when we are coming from metal we will set it to
        // the aqua color.
        // This is because the aqua color is a magical color that gets the background of the window,
        // so for most other LAFs the root pane changing is enough since it would be opaque, but for us
        // it is not since we are going to grab the one that was set on the JFrame. :(
        final Component parent = c.getParent();

        if (parent != null && parent instanceof JFrame) {
            final JFrame frameParent = (JFrame)parent;
            final Color bg = frameParent.getBackground();
            if (bg == null || bg instanceof UIResource) {
                frameParent.setBackground(UIManager.getColor("Panel.background"));
            }
        }

        // for <rdar://problem/3750909> OutOfMemoryError swapping menus.
        // Listen for layered pane/JMenuBar updates if the screen menu bar is active.
        if (sUseScreenMenuBar) {
            final JRootPane root = (JRootPane)c;
            root.addContainerListener(this);
            root.getLayeredPane().addContainerListener(this);
        }
    }

    public void uninstallUI(final JComponent c) {
        stopTimer();
        c.removeAncestorListener(this);

        if (sUseScreenMenuBar) {
            final JRootPane root = (JRootPane)c;
            root.removeContainerListener(this);
            root.getLayeredPane().removeContainerListener(this);
        }

        super.uninstallUI(c);
    }

    /**
     * If the screen menu bar is active we need to listen to the layered pane of the root pane
     * because it holds the JMenuBar.  So, if a new layered pane was added, listen to it.
     * If a new JMenuBar was added, tell the menu bar UI, because it will need to update the menu bar.
     */
    public void componentAdded(final ContainerEvent e) {
        if (e.getContainer() instanceof JRootPane) {
            final JRootPane root = (JRootPane)e.getContainer();
            if (e.getChild() == root.getLayeredPane()) {
                final JLayeredPane layered = root.getLayeredPane();
                layered.addContainerListener(this);
            }
        } else {
            if (e.getChild() instanceof JMenuBar) {
                final JMenuBar jmb = (JMenuBar)e.getChild();
                final MenuBarUI mbui = jmb.getUI();

                if (mbui instanceof AquaMenuBarUI) {
                    final Window owningWindow = SwingUtilities.getWindowAncestor(jmb);

                    // Could be a JDialog, and may have been added to a JRootPane not yet in a window.
                    if (owningWindow != null && owningWindow instanceof JFrame) {
                        ((AquaMenuBarUI)mbui).setScreenMenuBar((JFrame)owningWindow);
                    }
                }
            }
        }
    }

    /**
     * Likewise, when the layered pane is removed from the root pane, stop listening to it.
     * If the JMenuBar is removed, tell the menu bar UI to clear the menu bar.
     */
    public void componentRemoved(final ContainerEvent e) {
        if (e.getContainer() instanceof JRootPane) {
            final JRootPane root = (JRootPane)e.getContainer();
            if (e.getChild() == root.getLayeredPane()) {
                final JLayeredPane layered = root.getLayeredPane();
                layered.removeContainerListener(this);
            }
        } else {
            if (e.getChild() instanceof JMenuBar) {
                final JMenuBar jmb = (JMenuBar)e.getChild();
                final MenuBarUI mbui = jmb.getUI();

                if (mbui instanceof AquaMenuBarUI) {
                    final Window owningWindow = SwingUtilities.getWindowAncestor(jmb);

                    // Could be a JDialog, and may have been added to a JRootPane not yet in a window.
                    if (owningWindow != null && owningWindow instanceof JFrame) {
                        ((AquaMenuBarUI)mbui).clearScreenMenuBar((JFrame)owningWindow);
                    }
                }
            }
        }
    }

    /**
     * Invoked when a property changes on the root pane. If the event
     * indicates the {@code defaultButton} has changed, this will
     * update the animation.
     * If the enabled state changed, it will start or stop the animation
     */
    public void propertyChange(final PropertyChangeEvent e) {
        super.propertyChange(e);

        final String prop = e.getPropertyName();
        if ("defaultButton".equals(prop) || "temporaryDefaultButton".equals(prop)) {
            // Change the animating button if this root is showing and enabled
            // otherwise do nothing - someone else may be active
            final JRootPane root = (JRootPane)e.getSource();

            if (root.isShowing() && root.isEnabled()) {
                updateDefaultButton(root);
            }
        } else if ("enabled".equals(prop) || AquaFocusHandler.FRAME_ACTIVE_PROPERTY.equals(prop)) {
            final JRootPane root = (JRootPane)e.getSource();
            if (root.isShowing()) {
                if (((Boolean)e.getNewValue()).booleanValue()) {
                    updateDefaultButton((JRootPane)e.getSource());
                } else {
                    stopTimer();
                }
            }
        }
    }

    synchronized void stopTimer() {
        if (fTimer != null) {
            fTimer.stop();
            fTimer = null;
        }
    }

    synchronized void updateDefaultButton(final JRootPane root) {
        final JButton button = root.getDefaultButton();
        //System.err.println("in updateDefaultButton button = " + button);
        fCurrentDefaultButton = button;
        stopTimer();
        if (button != null) {
            fTimer = new Timer(kDefaultButtonPaintDelayBetweenFrames, new DefaultButtonPainter(root));
            fTimer.start();
        }
    }

    class DefaultButtonPainter implements ActionListener {
        JRootPane root;

        public DefaultButtonPainter(final JRootPane root) {
            this.root = root;
        }

        public void actionPerformed(final ActionEvent e) {
            final JButton defaultButton = root.getDefaultButton();
            if ((defaultButton != null) && defaultButton.isShowing()) {
                if (defaultButton.isEnabled()) {
                    defaultButton.repaint();
                }
            } else {
                stopTimer();
            }
        }
    }

    /**
     * This is sort of like viewDidMoveToWindow:.  When the root pane is put into a window
     * this method gets called for the notification.
     * We need to set up the listener relationship so we can pick up activation events.
     * And, if a JMenuBar was added before the root pane was added to the window, we now need
     * to notify the menu bar UI.
     */
    public void ancestorAdded(final AncestorEvent event) {
        // this is so we can handle window activated and deactivated events so
        // our swing controls can color/enable/disable/focus draw correctly
        final Container ancestor = event.getComponent();
        final Window owningWindow = SwingUtilities.getWindowAncestor(ancestor);

        if (owningWindow != null) {
            // We get this message even when a dialog is opened and the owning window is a window
            // that could already be listened to. We should only be a listener once.
            // adding multiple listeners was the cause of <rdar://problem/3534047>
            // but the incorrect removal of them caused <rdar://problem/3617848>
            owningWindow.removeWindowListener(this);
            owningWindow.addWindowListener(this);
        }

        // The root pane has been added to the hierarchy.  If it's enabled update the default
        // button to start the throbbing.  Since the UI is a singleton make sure the root pane
        // we are checking has a default button before calling update otherwise we will stop
        // throbbing the current default button.
        final JComponent comp = event.getComponent();
        if (comp instanceof JRootPane) {
            final JRootPane rp = (JRootPane)comp;
            if (rp.isEnabled() && rp.getDefaultButton() != null) {
                updateDefaultButton((JRootPane)comp);
            }
        }
    }

    /**
     * If the JRootPane was removed from the window we should clear the screen menu bar.
     * That's a non-trivial problem, because you need to know which window the JRootPane was in
     * before it was removed.  By the time ancestorRemoved was called, the JRootPane has already been removed
     */

    public void ancestorRemoved(final AncestorEvent event) { }
    public void ancestorMoved(final AncestorEvent event) { }

    public void windowActivated(final WindowEvent e) {
        updateComponentTreeUIActivation((Component)e.getSource(), Boolean.TRUE);
    }

    public void windowDeactivated(final WindowEvent e) {
        updateComponentTreeUIActivation((Component)e.getSource(), Boolean.FALSE);
    }

    public void windowOpened(final WindowEvent e) { }
    public void windowClosing(final WindowEvent e) { }

    public void windowClosed(final WindowEvent e) {
        // We know the window is closed so remove the listener.
        final Window w = e.getWindow();
        w.removeWindowListener(this);
    }

    public void windowIconified(final WindowEvent e) { }
    public void windowDeiconified(final WindowEvent e) { }
    public void windowStateChanged(final WindowEvent e) { }
    public void windowGainedFocus(final WindowEvent e) { }
    public void windowLostFocus(final WindowEvent e) { }

    private static void updateComponentTreeUIActivation(final Component c, Object active) {
        if (c instanceof javax.swing.JInternalFrame) {
            active = (((JInternalFrame)c).isSelected() ? Boolean.TRUE : Boolean.FALSE);
        }

        if (c instanceof javax.swing.JComponent) {
            ((javax.swing.JComponent)c).putClientProperty(AquaFocusHandler.FRAME_ACTIVE_PROPERTY, active);
        }

        Component[] children = null;

        if (c instanceof javax.swing.JMenu) {
            children = ((javax.swing.JMenu)c).getMenuComponents();
        } else if (c instanceof Container) {
            children = ((Container)c).getComponents();
        }

        if (children == null) return;

        for (final Component element : children) {
            updateComponentTreeUIActivation(element, active);
        }
    }

    @Override
    public final void update(final Graphics g, final JComponent c) {
        if (c.isOpaque()) {
            AquaUtils.fillRect(g, c);
        }
        paint(g, c);
    }
}
