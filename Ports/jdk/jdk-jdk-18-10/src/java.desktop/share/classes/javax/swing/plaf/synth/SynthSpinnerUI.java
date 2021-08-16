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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.beans.*;

/**
 * Provides the Synth L&amp;F UI delegate for
 * {@link javax.swing.JSpinner}.
 *
 * @author Hans Muller
 * @author Joshua Outwater
 * @since 1.7
 */
public class SynthSpinnerUI extends BasicSpinnerUI
                            implements PropertyChangeListener, SynthUI {
    private SynthStyle style;
    /**
     * A FocusListener implementation which causes the entire spinner to be
     * repainted whenever the editor component (typically a text field) becomes
     * focused, or loses focus. This is necessary because since SynthSpinnerUI
     * is composed of an editor and two buttons, it is necessary that all three
     * components indicate that they are "focused" so that they can be drawn
     * appropriately. The repaint is used to ensure that the buttons are drawn
     * in the new focused or unfocused state, mirroring that of the editor.
     */
    private EditorFocusHandler editorFocusHandler = new EditorFocusHandler();

    /**
     *
     * Constructs a {@code SynthSpinnerUI}.
     */
    public SynthSpinnerUI() {}

    /**
     * Returns a new instance of SynthSpinnerUI.
     *
     * @param c the JSpinner (not used)
     * @see ComponentUI#createUI
     * @return a new SynthSpinnerUI object
     */
    public static ComponentUI createUI(JComponent c) {
        return new SynthSpinnerUI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installListeners() {
        super.installListeners();
        spinner.addPropertyChangeListener(this);
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor)editor).getTextField();
            if (tf != null) {
                tf.addFocusListener(editorFocusHandler);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();
        spinner.removePropertyChangeListener(this);
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor)editor).getTextField();
            if (tf != null) {
                tf.removeFocusListener(editorFocusHandler);
            }
        }
    }

    /**
     * Initializes the <code>JSpinner</code> <code>border</code>,
     * <code>foreground</code>, and <code>background</code>, properties
     * based on the corresponding "Spinner.*" properties from defaults table.
     * The <code>JSpinners</code> layout is set to the value returned by
     * <code>createLayout</code>.  This method is called by <code>installUI</code>.
     *
     * @see #uninstallDefaults
     * @see #installUI
     * @see #createLayout
     * @see LookAndFeel#installBorder
     * @see LookAndFeel#installColors
     */
    @Override
    protected void installDefaults() {
        LayoutManager layout = spinner.getLayout();

        if (layout == null || layout instanceof UIResource) {
            spinner.setLayout(createLayout());
        }
        updateStyle(spinner);
    }


    private void updateStyle(JSpinner c) {
        SynthContext context = getContext(c, ENABLED);
        SynthStyle oldStyle = style;
        style = SynthLookAndFeel.updateStyle(context, this);
        if (style != oldStyle) {
            if (oldStyle != null) {
                // Only call installKeyboardActions as uninstall is not
                // public.
                installKeyboardActions();
            }
        }
    }


    /**
     * Sets the <code>JSpinner's</code> layout manager to null.  This
     * method is called by <code>uninstallUI</code>.
     *
     * @see #installDefaults
     * @see #uninstallUI
     */
    @Override
    protected void uninstallDefaults() {
        if (spinner.getLayout() instanceof UIResource) {
            spinner.setLayout(null);
        }

        SynthContext context = getContext(spinner, ENABLED);

        style.uninstallDefaults(context);
        style = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LayoutManager createLayout() {
        return new SpinnerLayout();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected Component createPreviousButton() {
        JButton b = new SynthArrowButton(SwingConstants.SOUTH);
        b.setName("Spinner.previousButton");
        installPreviousButtonListeners(b);
        return b;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected Component createNextButton() {
        JButton b = new SynthArrowButton(SwingConstants.NORTH);
        b.setName("Spinner.nextButton");
        installNextButtonListeners(b);
        return b;
    }


    /**
     * This method is called by installUI to get the editor component
     * of the <code>JSpinner</code>.  By default it just returns
     * <code>JSpinner.getEditor()</code>.  Subclasses can override
     * <code>createEditor</code> to return a component that contains
     * the spinner's editor or null, if they're going to handle adding
     * the editor to the <code>JSpinner</code> in an
     * <code>installUI</code> override.
     * <p>
     * Typically this method would be overridden to wrap the editor
     * with a container with a custom border, since one can't assume
     * that the editors border can be set directly.
     * <p>
     * The <code>replaceEditor</code> method is called when the spinners
     * editor is changed with <code>JSpinner.setEditor</code>.  If you've
     * overriden this method, then you'll probably want to override
     * <code>replaceEditor</code> as well.
     *
     * @return the JSpinners editor JComponent, spinner.getEditor() by default
     * @see #installUI
     * @see #replaceEditor
     * @see JSpinner#getEditor
     */
    @Override
    protected JComponent createEditor() {
        JComponent editor = spinner.getEditor();
        editor.setName("Spinner.editor");
        updateEditorAlignment(editor);
        return editor;
    }


    /**
     * Called by the <code>PropertyChangeListener</code> when the
     * <code>JSpinner</code> editor property changes.  It's the responsibility
     * of this method to remove the old editor and add the new one.  By
     * default this operation is just:
     * <pre>
     * spinner.remove(oldEditor);
     * spinner.add(newEditor, "Editor");
     * </pre>
     * The implementation of <code>replaceEditor</code> should be coordinated
     * with the <code>createEditor</code> method.
     *
     * @see #createEditor
     * @see #createPropertyChangeListener
     */
    @Override
    protected void replaceEditor(JComponent oldEditor, JComponent newEditor) {
        spinner.remove(oldEditor);
        spinner.add(newEditor, "Editor");
        if (oldEditor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor)oldEditor).getTextField();
            if (tf != null) {
                tf.removeFocusListener(editorFocusHandler);
            }
        }
        if (newEditor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor)newEditor).getTextField();
            if (tf != null) {
                tf.addFocusListener(editorFocusHandler);
            }
        }
    }

    private void updateEditorAlignment(JComponent editor) {
        if (editor instanceof JSpinner.DefaultEditor) {
            SynthContext context = getContext(spinner);
            Integer alignment = (Integer)context.getStyle().get(
                    context, "Spinner.editorAlignment");
            JTextField text = ((JSpinner.DefaultEditor)editor).getTextField();
            if (alignment != null) {
                text.setHorizontalAlignment(alignment);

            }
            // copy across the sizeVariant property to the editor
            text.putClientProperty("JComponent.sizeVariant",
                    spinner.getClientProperty("JComponent.sizeVariant"));
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
        context.getPainter().paintSpinnerBackground(context,
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
        context.getPainter().paintSpinnerBorder(context, g, x, y, w, h);
    }

    /**
     * A simple layout manager for the editor and the next/previous buttons.
     * See the SynthSpinnerUI javadoc for more information about exactly
     * how the components are arranged.
     */
    private static class SpinnerLayout implements LayoutManager, UIResource
    {
        private Component nextButton = null;
        private Component previousButton = null;
        private Component editor = null;

        public void addLayoutComponent(String name, Component c) {
            if ("Next".equals(name)) {
                nextButton = c;
            }
            else if ("Previous".equals(name)) {
                previousButton = c;
            }
            else if ("Editor".equals(name)) {
                editor = c;
            }
        }

        public void removeLayoutComponent(Component c) {
            if (c == nextButton) {
                nextButton = null;
            }
            else if (c == previousButton) {
                previousButton = null;
            }
            else if (c == editor) {
                editor = null;
            }
        }

        private Dimension preferredSize(Component c) {
            return (c == null) ? new Dimension(0, 0) : c.getPreferredSize();
        }

        public Dimension preferredLayoutSize(Container parent) {
            Dimension nextD = preferredSize(nextButton);
            Dimension previousD = preferredSize(previousButton);
            Dimension editorD = preferredSize(editor);

            /* Force the editors height to be a multiple of 2
             */
            editorD.height = ((editorD.height + 1) / 2) * 2;

            Dimension size = new Dimension(editorD.width, editorD.height);
            size.width += Math.max(nextD.width, previousD.width);
            Insets insets = parent.getInsets();
            size.width += insets.left + insets.right;
            size.height += insets.top + insets.bottom;
            return size;
        }

        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        private void setBounds(Component c, int x, int y, int width, int height) {
            if (c != null) {
                c.setBounds(x, y, width, height);
            }
        }

        public void layoutContainer(Container parent) {
            Insets insets = parent.getInsets();
            int availWidth = parent.getWidth() - (insets.left + insets.right);
            int availHeight = parent.getHeight() - (insets.top + insets.bottom);
            Dimension nextD = preferredSize(nextButton);
            Dimension previousD = preferredSize(previousButton);
            int nextHeight = availHeight / 2;
            int previousHeight = availHeight - nextHeight;
            int buttonsWidth = Math.max(nextD.width, previousD.width);
            int editorWidth = availWidth - buttonsWidth;

            /* Deal with the spinners componentOrientation property.
             */
            int editorX, buttonsX;
            if (parent.getComponentOrientation().isLeftToRight()) {
                editorX = insets.left;
                buttonsX = editorX + editorWidth;
            }
            else {
                buttonsX = insets.left;
                editorX = buttonsX + buttonsWidth;
            }

            int previousY = insets.top + nextHeight;
            setBounds(editor, editorX, insets.top, editorWidth, availHeight);
            setBounds(nextButton, buttonsX, insets.top, buttonsWidth, nextHeight);
            setBounds(previousButton, buttonsX, previousY, buttonsWidth, previousHeight);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        JSpinner spinner = (JSpinner)(e.getSource());
        SpinnerUI spinnerUI = spinner.getUI();

        if (spinnerUI instanceof SynthSpinnerUI) {
            SynthSpinnerUI ui = (SynthSpinnerUI)spinnerUI;

            if (SynthLookAndFeel.shouldUpdateStyle(e)) {
                ui.updateStyle(spinner);
            }
        }
    }

    /** Listen to editor text field focus changes and repaint whole spinner */
    private class EditorFocusHandler implements FocusListener{
        /** Invoked when a editor text field gains the keyboard focus. */
        @Override public void focusGained(FocusEvent e) {
            spinner.repaint();
        }

        /** Invoked when a editor text field loses the keyboard focus. */
        @Override public void focusLost(FocusEvent e) {
            spinner.repaint();
        }
    }
}
