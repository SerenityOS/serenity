/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicPanelUI;
import java.awt.*;
import java.beans.*;

/**
 * Provides the Synth L&amp;F UI delegate for
 * {@link javax.swing.JPanel}.
 *
 * @author Steve Wilson
 * @since 1.7
 */
public class SynthPanelUI extends BasicPanelUI
                          implements PropertyChangeListener, SynthUI {
    private SynthStyle style;

    /**
     *
     * Constructs a {@code SynthPanelUI}.
     */
    public SynthPanelUI() {}

    /**
     * Creates a new UI object for the given component.
     *
     * @param c component to create UI object for
     * @return the UI object
     */
    public static ComponentUI createUI(JComponent c) {
        return new SynthPanelUI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void installUI(JComponent c) {
        JPanel p = (JPanel)c;

        super.installUI(c);
        installListeners(p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void uninstallUI(JComponent c) {
        JPanel p = (JPanel)c;

        uninstallListeners(p);
        super.uninstallUI(c);
    }

    /**
     * Installs listeners into the panel.
     *
     * @param p the {@code JPanel} object
     */
    protected void installListeners(JPanel p) {
        p.addPropertyChangeListener(this);
    }

    /**
     * Uninstalls listeners from the panel.
     *
     * @param p the {@code JPanel} object
     */
    protected void uninstallListeners(JPanel p) {
        p.removePropertyChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installDefaults(JPanel p) {
        updateStyle(p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallDefaults(JPanel p) {
        SynthContext context = getContext(p, ENABLED);

        style.uninstallDefaults(context);
        style = null;
    }

    private void updateStyle(JPanel c) {
        SynthContext context = getContext(c, ENABLED);
        style = SynthLookAndFeel.updateStyle(context, this);
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
        context.getPainter().paintPanelBackground(context,
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
        // do actual painting
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintBorder(SynthContext context, Graphics g, int x,
                            int y, int w, int h) {
        context.getPainter().paintPanelBorder(context, g, x, y, w, h);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent pce) {
        if (SynthLookAndFeel.shouldUpdateStyle(pce)) {
            updateStyle((JPanel)pce.getSource());
        }
    }
}
