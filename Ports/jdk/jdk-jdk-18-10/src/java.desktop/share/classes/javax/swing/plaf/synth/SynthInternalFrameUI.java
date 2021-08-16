/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import java.beans.*;


/**
 * Provides the Synth L&amp;F UI delegate for
 * {@link javax.swing.JInternalFrame}.
 *
 * @author David Kloba
 * @author Joshua Outwater
 * @author Rich Schiavi
 * @since 1.7
 */
public class SynthInternalFrameUI extends BasicInternalFrameUI
                                  implements SynthUI, PropertyChangeListener {
    private SynthStyle style;

    /**
     * Creates a new UI object for the given component.
     *
     * @param b component to create UI object for
     * @return the UI object
     */
    public static ComponentUI createUI(JComponent b) {
        return new SynthInternalFrameUI((JInternalFrame)b);
    }

    /**
     * Constructs a {@code SynthInternalFrameUI}.
     * @param b an internal frame
     */
    protected SynthInternalFrameUI(JInternalFrame b) {
        super(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void installDefaults() {
        frame.setLayout(internalFrameLayout = createLayoutManager());
        updateStyle(frame);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installListeners() {
        super.installListeners();
        frame.addPropertyChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallComponents() {
        if (frame.getComponentPopupMenu() instanceof UIResource) {
            frame.setComponentPopupMenu(null);
        }
        super.uninstallComponents();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallListeners() {
        frame.removePropertyChangeListener(this);
        super.uninstallListeners();
    }

    private void updateStyle(JComponent c) {
        SynthContext context = getContext(c, ENABLED);
        SynthStyle oldStyle = style;

        style = SynthLookAndFeel.updateStyle(context, this);
        if (style != oldStyle) {
            Icon frameIcon = frame.getFrameIcon();
            if (frameIcon == null || frameIcon instanceof UIResource) {
                frame.setFrameIcon(context.getStyle().getIcon(
                                   context, "InternalFrame.icon"));
            }
            if (oldStyle != null) {
                uninstallKeyboardActions();
                installKeyboardActions();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallDefaults() {
        SynthContext context = getContext(frame, ENABLED);
        style.uninstallDefaults(context);
        style = null;
        if(frame.getLayout() == internalFrameLayout) {
            frame.setLayout(null);
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
     * {@inheritDoc}
     */
    @Override
    protected JComponent createNorthPane(JInternalFrame w) {
        titlePane = new SynthInternalFrameTitlePane(w);
        titlePane.setName("InternalFrame.northPane");
        return titlePane;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ComponentListener createComponentListener() {
        if (UIManager.getBoolean("InternalFrame.useTaskBar")) {
            return new ComponentHandler() {
                @Override public void componentResized(ComponentEvent e) {
                    if (frame != null && frame.isMaximum()) {
                        JDesktopPane desktop = (JDesktopPane)e.getSource();
                        for (Component comp : desktop.getComponents()) {
                            if (comp instanceof SynthDesktopPaneUI.TaskBar) {
                                frame.setBounds(0, 0,
                                                desktop.getWidth(),
                                                desktop.getHeight() - comp.getHeight());
                                frame.revalidate();
                                break;
                            }
                        }
                    }

                    // Update the new parent bounds for next resize, but don't
                    // let the super method touch this frame
                    JInternalFrame f = frame;
                    frame = null;
                    super.componentResized(e);
                    frame = f;
                }
            };
        } else {
            return super.createComponentListener();
        }
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
        context.getPainter().paintInternalFrameBackground(context,
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintBorder(SynthContext context, Graphics g, int x,
                            int y, int w, int h) {
        context.getPainter().paintInternalFrameBorder(context,
                                                            g, x, y, w, h);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        SynthStyle oldStyle = style;
        JInternalFrame f = (JInternalFrame)evt.getSource();
        String prop = evt.getPropertyName();

        if (SynthLookAndFeel.shouldUpdateStyle(evt)) {
            updateStyle(f);
        }

        if (style == oldStyle &&
            (prop == JInternalFrame.IS_MAXIMUM_PROPERTY ||
             prop == JInternalFrame.IS_SELECTED_PROPERTY)) {
            // Border (and other defaults) may need to change
            SynthContext context = getContext(f, ENABLED);
            style.uninstallDefaults(context);
            style.installDefaults(context, this);
        }
    }
}
