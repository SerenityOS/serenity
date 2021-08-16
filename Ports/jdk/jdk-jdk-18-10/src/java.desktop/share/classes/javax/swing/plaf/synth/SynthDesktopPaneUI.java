/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

import javax.swing.DefaultDesktopManager;
import javax.swing.DesktopManager;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicDesktopPaneUI;

/**
 * Provides the Synth L&amp;F UI delegate for
 * {@link javax.swing.JDesktopPane}.
 *
 * @author Joshua Outwater
 * @author Steve Wilson
 * @since 1.7
 */
public class SynthDesktopPaneUI extends BasicDesktopPaneUI implements
                  PropertyChangeListener, SynthUI {
    private SynthStyle style;
    private TaskBar taskBar;
    private DesktopManager oldDesktopManager;

    /**
     *
     * Constructs a {@code SynthDesktopPaneUI}.
     */
    public SynthDesktopPaneUI() {}

    /**
     * Creates a new UI object for the given component.
     *
     * @param c component to create UI object for
     * @return the UI object
     */
    public static ComponentUI createUI(JComponent c) {
        return new SynthDesktopPaneUI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installListeners() {
        super.installListeners();
        desktop.addPropertyChangeListener(this);
        if (taskBar != null) {
            // Listen for desktop being resized
            desktop.addComponentListener(taskBar);
            // Listen for frames being added to desktop
            desktop.addContainerListener(taskBar);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installDefaults() {
        updateStyle(desktop);

        if (UIManager.getBoolean("InternalFrame.useTaskBar")) {
            taskBar = new TaskBar();

            for (Component comp : desktop.getComponents()) {
                JInternalFrame.JDesktopIcon desktopIcon;

                if (comp instanceof JInternalFrame.JDesktopIcon) {
                    desktopIcon = (JInternalFrame.JDesktopIcon)comp;
                } else if (comp instanceof JInternalFrame) {
                    desktopIcon = ((JInternalFrame)comp).getDesktopIcon();
                } else {
                    continue;
                }
                // Move desktopIcon from desktop to taskBar
                if (desktopIcon.getParent() == desktop) {
                    desktop.remove(desktopIcon);
                }
                if (desktopIcon.getParent() != taskBar) {
                    taskBar.add(desktopIcon);
                    desktopIcon.getInternalFrame().addComponentListener(
                        taskBar);
                }
            }
            taskBar.setBackground(desktop.getBackground());
            desktop.add(taskBar,
                Integer.valueOf(JLayeredPane.PALETTE_LAYER.intValue() + 1));
            if (desktop.isShowing()) {
                taskBar.adjustSize();
            }
        }
    }

    private void updateStyle(JDesktopPane c) {
        SynthStyle oldStyle = style;
        SynthContext context = getContext(c, ENABLED);
        style = SynthLookAndFeel.updateStyle(context, this);
        if (oldStyle != null) {
            uninstallKeyboardActions();
            installKeyboardActions();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallListeners() {
        if (taskBar != null) {
            desktop.removeComponentListener(taskBar);
            desktop.removeContainerListener(taskBar);
        }
        desktop.removePropertyChangeListener(this);
        super.uninstallListeners();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallDefaults() {
        SynthContext context = getContext(desktop, ENABLED);

        style.uninstallDefaults(context);
        style = null;

        if (taskBar != null) {
            for (Component comp : taskBar.getComponents()) {
                JInternalFrame.JDesktopIcon desktopIcon =
                    (JInternalFrame.JDesktopIcon)comp;
                taskBar.remove(desktopIcon);
                desktopIcon.setPreferredSize(null);
                JInternalFrame f = desktopIcon.getInternalFrame();
                if (f.isIcon()) {
                    desktop.add(desktopIcon);
                }
                f.removeComponentListener(taskBar);
            }
            desktop.remove(taskBar);
            taskBar = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installDesktopManager() {
        if (UIManager.getBoolean("InternalFrame.useTaskBar")) {
            desktopManager = oldDesktopManager = desktop.getDesktopManager();
            if (!(desktopManager instanceof SynthDesktopManager)) {
                desktopManager = new SynthDesktopManager();
                desktop.setDesktopManager(desktopManager);
            }
        } else {
            super.installDesktopManager();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallDesktopManager() {
        if (oldDesktopManager != null && !(oldDesktopManager instanceof UIResource)) {
            desktopManager = desktop.getDesktopManager();
            if (desktopManager == null || desktopManager instanceof UIResource) {
                desktop.setDesktopManager(oldDesktopManager);
            }
        }
        oldDesktopManager = null;
        super.uninstallDesktopManager();
    }

    /**
     * The {@code TaskBar} installed by the UI.
     */
    @SuppressWarnings("serial") // Same-version serialization only and
                                // internal anonymous classes
    static class TaskBar extends JPanel implements ComponentListener, ContainerListener {
        TaskBar() {
            setOpaque(true);
            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0) {
                public void layoutContainer(Container target) {
                    // First shrink buttons to fit
                    Component[] comps = target.getComponents();
                    int n = comps.length;
                    if (n > 0) {
                        // Start with the largest preferred width
                        int prefWidth = 0;
                        for (Component c : comps) {
                            c.setPreferredSize(null);
                            Dimension prefSize = c.getPreferredSize();
                            if (prefSize.width > prefWidth) {
                                prefWidth = prefSize.width;
                            }
                        }
                        // Shrink equally to fit if needed
                        Insets insets = target.getInsets();
                        int tw = target.getWidth() - insets.left - insets.right;
                        int w = Math.min(prefWidth, Math.max(10, tw/n));
                        for (Component c : comps) {
                            Dimension prefSize = c.getPreferredSize();
                            c.setPreferredSize(new Dimension(w, prefSize.height));
                        }
                    }
                    super.layoutContainer(target);
                }
            });

            // PENDING: This should be handled by the painter
            setBorder(new BevelBorder(BevelBorder.RAISED) {
                protected void paintRaisedBevel(Component c, Graphics g,
                                                int x, int y, int w, int h)  {
                    Color oldColor = g.getColor();
                    g.translate(x, y);
                    g.setColor(getHighlightOuterColor(c));
                    g.drawLine(0, 0, 0, h-2);
                    g.drawLine(1, 0, w-2, 0);
                    g.setColor(getShadowOuterColor(c));
                    g.drawLine(0, h-1, w-1, h-1);
                    g.drawLine(w-1, 0, w-1, h-2);
                    g.translate(-x, -y);
                    g.setColor(oldColor);
                }
            });
        }

        void adjustSize() {
            JDesktopPane desktop = (JDesktopPane)getParent();
            if (desktop != null) {
                int height = getPreferredSize().height;
                Insets insets = getInsets();
                if (height == insets.top + insets.bottom) {
                    if (getHeight() <= height) {
                        // Initial size, because we have no buttons yet
                        height += 21;
                    } else {
                        // We already have a good height
                        height = getHeight();
                    }
                }
                setBounds(0, desktop.getHeight() - height, desktop.getWidth(), height);
                revalidate();
                repaint();
            }
        }

        // ComponentListener interface

        public void componentResized(ComponentEvent e) {
            if (e.getSource() instanceof JDesktopPane) {
                adjustSize();
            }
        }

        public void componentMoved(ComponentEvent e){}

        public void componentShown(ComponentEvent e) {
            if (e.getSource() instanceof JInternalFrame) {
                adjustSize();
            }
        }

        public void componentHidden(ComponentEvent e) {
            if (e.getSource() instanceof JInternalFrame) {
                ((JInternalFrame)e.getSource()).getDesktopIcon().setVisible(false);
                revalidate();
            }
        }

        // ContainerListener interface

        public void componentAdded(ContainerEvent e) {
            if (e.getChild() instanceof JInternalFrame) {
                JDesktopPane desktop = (JDesktopPane)e.getSource();
                JInternalFrame f = (JInternalFrame)e.getChild();
                JInternalFrame.JDesktopIcon desktopIcon = f.getDesktopIcon();
                for (Component comp : getComponents()) {
                    if (comp == desktopIcon) {
                        // We have it already
                        return;
                    }
                }
                add(desktopIcon);
                f.addComponentListener(this);
                if (getComponentCount() == 1) {
                    adjustSize();
                }
            }
        }

        public void componentRemoved(ContainerEvent e) {
            if (e.getChild() instanceof JInternalFrame) {
                JInternalFrame f = (JInternalFrame)e.getChild();
                if (!f.isIcon()) {
                    // Frame was removed without using setClosed(true)
                    remove(f.getDesktopIcon());
                    f.removeComponentListener(this);
                    revalidate();
                    repaint();
                }
            }
        }
    }

    /**
     * The default {@code DesktopManager} installed by the UI.
     */
    @SuppressWarnings("serial") // Same-version serialization only
    class SynthDesktopManager extends DefaultDesktopManager implements UIResource {

        public void maximizeFrame(JInternalFrame f) {
            if (f.isIcon()) {
                try {
                    f.setIcon(false);
                } catch (PropertyVetoException e2) {
                }
            } else {
                f.setNormalBounds(f.getBounds());
                Component desktop = f.getParent();
                setBoundsForFrame(f, 0, 0,
                                  desktop.getWidth(),
                                  desktop.getHeight() - taskBar.getHeight());
            }

            try {
                f.setSelected(true);
            } catch (PropertyVetoException e2) {
            }
        }

        public void iconifyFrame(JInternalFrame f) {
            JInternalFrame.JDesktopIcon desktopIcon;
            Container c = f.getParent();
            JDesktopPane d = f.getDesktopPane();
            boolean findNext = f.isSelected();
            if (c == null || d == null) {
                return;
            }
            desktopIcon = f.getDesktopIcon();
            if (!wasIcon(f)) {
                Rectangle r = getBoundsForIconOf(f);
                desktopIcon.setBounds(r.x, r.y, r.width, r.height);
                desktopIcon.revalidate();
                setWasIcon(f, Boolean.TRUE);
            }

            c.remove(f);
            c.repaint(f.getX(), f.getY(), f.getWidth(), f.getHeight());
            try {
                f.setSelected(false);
            } catch (PropertyVetoException e2) {
            }

            // Get topmost of the remaining frames
            if (findNext) {
                for (Component comp : c.getComponents()) {
                    if (comp instanceof JInternalFrame) {
                        try {
                            ((JInternalFrame)comp).setSelected(true);
                        } catch (PropertyVetoException e2) {
                        }
                        ((JInternalFrame)comp).moveToFront();
                        return;
                    }
                }
            }
        }


        public void deiconifyFrame(JInternalFrame f) {
            JInternalFrame.JDesktopIcon desktopIcon = f.getDesktopIcon();
            Container c = desktopIcon.getParent();
            if (c != null) {
                c = c.getParent();
                if (c != null) {
                    c.add(f);
                    if (f.isMaximum()) {
                        int w = c.getWidth();
                        int h = c.getHeight() - taskBar.getHeight();
                        if (f.getWidth() != w || f.getHeight() != h) {
                            setBoundsForFrame(f, 0, 0, w, h);
                        }
                    }
                    if (f.isSelected()) {
                        f.moveToFront();
                    } else {
                        try {
                            f.setSelected(true);
                        } catch (PropertyVetoException e2) {
                        }
                    }
                }
            }
        }

        protected void removeIconFor(JInternalFrame f) {
            super.removeIconFor(f);
            taskBar.validate();
        }

        public void setBoundsForFrame(JComponent f, int newX, int newY, int newWidth, int newHeight) {
            super.setBoundsForFrame(f, newX, newY, newWidth, newHeight);
            if (taskBar != null && newY >= taskBar.getY()) {
                f.setLocation(f.getX(), taskBar.getY()-f.getInsets().top);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SynthContext getContext(JComponent c) {
        return getContext(c, getComponentState(c));
    }

    private SynthContext getContext(JComponent c, int state) {
        return SynthContext.getContext(c, style, state);
    }

    private int getComponentState(JComponent c) {
        return SynthLookAndFeel.getComponentState(c);
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
        context.getPainter().paintDesktopPaneBackground(context, g, 0, 0,
                                                  c.getWidth(), c.getHeight());
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintBorder(SynthContext context, Graphics g, int x,
                            int y, int w, int h) {
        context.getPainter().paintDesktopPaneBorder(context, g, x, y, w, h);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (SynthLookAndFeel.shouldUpdateStyle(evt)) {
            updateStyle((JDesktopPane)evt.getSource());
        }
        if (evt.getPropertyName() == "ancestor" && taskBar != null) {
            taskBar.adjustSize();
        }
    }
}
