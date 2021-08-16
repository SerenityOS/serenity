/*
 * Copyright (c) 2002, 2014, Oracle and/or its affiliates. All rights reserved.
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
import javax.swing.text.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicTextFieldUI;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;


/**
 * Provides the Synth L&amp;F UI delegate for {@link javax.swing.JTextField}.
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
 * @author  Shannon Hickey
 * @since 1.7
 */
@SuppressWarnings("serial") // Same-version serialization only
public class SynthTextFieldUI extends BasicTextFieldUI implements SynthUI {
    private Handler handler = new Handler();
    private SynthStyle style;
    private boolean updateKBAction = true;

    /**
     *
     * Constructs a {@code SynthTextFieldUI}.
     */
    public SynthTextFieldUI() {}

    /**
     * Creates a UI for a JTextField.
     *
     * @param c the text field
     * @return the UI object
     */
    public static ComponentUI createUI(JComponent c) {
        return new SynthTextFieldUI();
    }

    private void updateStyle(JTextComponent comp, boolean updateKBAction) {
        SynthContext context = getContext(comp, ENABLED);
        SynthStyle oldStyle = style;

        style = SynthLookAndFeel.updateStyle(context, this);

        if (style != oldStyle) {
            SynthTextFieldUI.updateStyle(comp, context, getPropertyPrefix());

            if (oldStyle != null && updateKBAction) {
                uninstallKeyboardActions();
                installKeyboardActions();
            }
        }
    }

    static void updateStyle(JTextComponent comp, SynthContext context,
            String prefix) {
        SynthStyle style = context.getStyle();

        Color color = comp.getCaretColor();
        if (color == null || color instanceof UIResource) {
            comp.setCaretColor(
                (Color)style.get(context, prefix + ".caretForeground"));
        }

        Color fg = comp.getForeground();
        if (fg == null || fg instanceof UIResource) {
            fg = style.getColorForState(context, ColorType.TEXT_FOREGROUND);
            if (fg != null) {
                comp.setForeground(fg);
            }
        }

        Object ar = style.get(context, prefix + ".caretAspectRatio");
        if (ar instanceof Number) {
            comp.putClientProperty("caretAspectRatio", ar);
        }

        context.setComponentState(SELECTED | FOCUSED);

        Color s = comp.getSelectionColor();
        if (s == null || s instanceof UIResource) {
            comp.setSelectionColor(
                style.getColor(context, ColorType.TEXT_BACKGROUND));
        }

        Color sfg = comp.getSelectedTextColor();
        if (sfg == null || sfg instanceof UIResource) {
            comp.setSelectedTextColor(
                style.getColor(context, ColorType.TEXT_FOREGROUND));
        }

        context.setComponentState(DISABLED);

        Color dfg = comp.getDisabledTextColor();
        if (dfg == null || dfg instanceof UIResource) {
            comp.setDisabledTextColor(
                style.getColor(context, ColorType.TEXT_FOREGROUND));
        }

        Insets margin = comp.getMargin();
        if (margin == null || margin instanceof UIResource) {
            margin = (Insets)style.get(context, prefix + ".margin");

            if (margin == null) {
                // Some places assume margins are non-null.
                margin = SynthLookAndFeel.EMPTY_UIRESOURCE_INSETS;
            }
            comp.setMargin(margin);
        }

        Caret caret = comp.getCaret();
        if (caret instanceof UIResource) {
            Object o = style.get(context, prefix + ".caretBlinkRate");
            if (o != null && o instanceof Integer) {
                Integer rate = (Integer)o;
                caret.setBlinkRate(rate.intValue());
            }
        }
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
        paintBackground(context, g, c);
        paint(context, g);
    }

    /**
     * Paints the specified component.
     * <p>This is routed to the {@link #paintSafely} method under
     * the guarantee that the model does not change from the view of this
     * thread while it is rendering (if the associated model is
     * derived from {@code AbstractDocument}).  This enables the
     * model to potentially be updated asynchronously.
     *
     * @param context context for the component being painted
     * @param g the {@code Graphics} object used for painting
     * @see #update(Graphics,JComponent)
     */
    protected void paint(SynthContext context, Graphics g) {
        super.paint(g, getComponent());
    }

    void paintBackground(SynthContext context, Graphics g, JComponent c) {
        context.getPainter().paintTextFieldBackground(context, g, 0, 0,
                                                c.getWidth(), c.getHeight());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintBorder(SynthContext context, Graphics g, int x,
                            int y, int w, int h) {
        context.getPainter().paintTextFieldBorder(context, g, x, y, w, h);
    }

    /**
     * {@inheritDoc}
     * Overridden to do nothing.
     */
    @Override
    protected void paintBackground(Graphics g) {
        // Overriden to do nothing, all our painting is done from update/paint.
    }

    /**
     * This method gets called when a bound property is changed
     * on the associated JTextComponent.  This is a hook
     * which UI implementations may change to reflect how the
     * UI displays bound properties of JTextComponent subclasses.
     * This is implemented to do nothing (i.e. the response to
     * properties in JTextComponent itself are handled prior
     * to calling this method).
     *
     * @param evt the property change event
     */
    @Override
    protected void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("keymap")) {
            if (evt.getNewValue() != null)
            {
                updateKBAction = false;
            } else {
                updateKBAction = true;
            }
        }
        if (SynthLookAndFeel.shouldUpdateStyle(evt)) {
            updateStyle((JTextComponent)evt.getSource(), updateKBAction);
        }
        super.propertyChange(evt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installDefaults() {
        // Installs the text cursor on the component
        super.installDefaults();
        updateStyle(getComponent(), true);
        getComponent().addFocusListener(handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallDefaults() {
        SynthContext context = getContext(getComponent(), ENABLED);

        getComponent().putClientProperty("caretAspectRatio", null);
        getComponent().removeFocusListener(handler);

        style.uninstallDefaults(context);
        style = null;
        super.uninstallDefaults();
    }

    private final class Handler implements FocusListener {
        public void focusGained(FocusEvent e) {
            getComponent().repaint();
        }

        public void focusLost(FocusEvent e) {
            getComponent().repaint();
        }
    }
}
