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

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicDesktopIconUI;

/**
 * Provides the Synth L&amp;F UI delegate for a minimized internal frame on a desktop.
 *
 * @author Joshua Outwater
 * @since 1.7
 */
public class SynthDesktopIconUI extends BasicDesktopIconUI
                                implements SynthUI, PropertyChangeListener {
    private SynthStyle style;
    private Handler handler = new Handler();

    /**
     *
     * Constructs a {@code SynthDesktopIconUI}.
     */
    public SynthDesktopIconUI() {}

    /**
     * Creates a new UI object for the given component.
     *
     * @param c component to create UI object for
     * @return the UI object
     */
    public static ComponentUI createUI(JComponent c)    {
        return new SynthDesktopIconUI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installComponents() {
        if (UIManager.getBoolean("InternalFrame.useTaskBar")) {
            @SuppressWarnings("serial") // anonymous class
            JToggleButton tmp = new JToggleButton(frame.getTitle(), frame.getFrameIcon()) {
                @Override public String getToolTipText() {
                    return getText();
                }

                @Override public JPopupMenu getComponentPopupMenu() {
                    return frame.getComponentPopupMenu();
                }
            };
            iconPane = tmp;
            ToolTipManager.sharedInstance().registerComponent(iconPane);
            iconPane.setFont(desktopIcon.getFont());
            iconPane.setBackground(desktopIcon.getBackground());
            iconPane.setForeground(desktopIcon.getForeground());
        } else {
            iconPane = new SynthInternalFrameTitlePane(frame);
            iconPane.setName("InternalFrame.northPane");
        }
        desktopIcon.setLayout(new BorderLayout());
        desktopIcon.add(iconPane, BorderLayout.CENTER);
    }

    @Override
    protected void uninstallComponents() {
        // Uninstall the listeners here because the iconPane will be set to null
        // in the super.uninstallComponents()
        if (iconPane instanceof JToggleButton) {
            ((JToggleButton) iconPane).removeActionListener(handler);
            frame.removePropertyChangeListener(this);
        } else if (iconPane instanceof SynthInternalFrameTitlePane) {
            // Uninstall the listeners added by the  SynthInternalFrameTitlePane
            ((SynthInternalFrameTitlePane) iconPane).uninstallListeners();
        }
        super.uninstallComponents();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installListeners() {
        super.installListeners();
        desktopIcon.addPropertyChangeListener(this);

        if (iconPane instanceof JToggleButton) {
            frame.addPropertyChangeListener(this);
            ((JToggleButton)iconPane).addActionListener(handler);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallListeners() {
        desktopIcon.removePropertyChangeListener(this);
        super.uninstallListeners();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installDefaults() {
        updateStyle(desktopIcon);
    }

    private void updateStyle(JComponent c) {
        SynthContext context = getContext(c, ENABLED);
        style = SynthLookAndFeel.updateStyle(context, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallDefaults() {
        SynthContext context = getContext(desktopIcon, ENABLED);
        style.uninstallDefaults(context);
        style = null;
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
        context.getPainter().paintDesktopIconBackground(context, g, 0, 0,
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
        context.getPainter().paintDesktopIconBorder(context, g, x, y, w, h);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() instanceof JInternalFrame.JDesktopIcon) {
            if (SynthLookAndFeel.shouldUpdateStyle(evt)) {
                updateStyle((JInternalFrame.JDesktopIcon)evt.getSource());
            }
        } else if (evt.getSource() instanceof JInternalFrame) {
            JInternalFrame frame = (JInternalFrame)evt.getSource();
            if (iconPane instanceof JToggleButton) {
                JToggleButton button = (JToggleButton)iconPane;
                String prop = evt.getPropertyName();
                if (prop == "title") {
                    button.setText((String)evt.getNewValue());
                } else if (prop == "frameIcon") {
                    button.setIcon((Icon)evt.getNewValue());
                } else if (prop == JInternalFrame.IS_ICON_PROPERTY ||
                           prop == JInternalFrame.IS_SELECTED_PROPERTY) {
                    button.setSelected(!frame.isIcon() && frame.isSelected());
                }
            }
        }
    }

    private final class Handler implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
            if (evt.getSource() instanceof JToggleButton) {
                // Either iconify the frame or deiconify and activate it.
                JToggleButton button = (JToggleButton)evt.getSource();
                try {
                    boolean selected = button.isSelected();
                    if (!selected && !frame.isIconifiable()) {
                        button.setSelected(true);
                    } else {
                        frame.setIcon(!selected);
                        if (selected) {
                            frame.setSelected(true);
                        }
                    }
                } catch (PropertyVetoException e2) {
                }
            }
        }
    }
}
