/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.CharacterIterator;
import java.text.DateFormat;
import java.text.Format;
import java.text.Format.Field;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ActionMap;
import javax.swing.ButtonModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.SpinnerUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.InternationalFormatter;

import apple.laf.JRSUIConstants.BooleanValue;
import apple.laf.JRSUIConstants.Size;
import apple.laf.JRSUIConstants.State;
import apple.laf.JRSUIState;
import apple.laf.JRSUIStateFactory;
import com.apple.laf.AquaUtils.RecyclableSingleton;
import com.apple.laf.AquaUtils.RecyclableSingletonFromDefaultConstructor;

/**
 * This is originally derived from BasicSpinnerUI, but they made everything
 * private so we can't subclass!
 */
public class AquaSpinnerUI extends SpinnerUI {

    private static final RecyclableSingleton<? extends PropertyChangeListener> propertyChangeListener
            = new RecyclableSingletonFromDefaultConstructor<>(PropertyChangeHandler.class);

    static PropertyChangeListener getPropertyChangeListener() {
        return propertyChangeListener.get();
    }

    private static final RecyclableSingleton<ArrowButtonHandler> nextButtonHandler
            = new RecyclableSingleton<ArrowButtonHandler>() {
                @Override
                protected ArrowButtonHandler getInstance() {
                    return new ArrowButtonHandler("increment", true);
                }
            };

    static ArrowButtonHandler getNextButtonHandler() {
        return nextButtonHandler.get();
    }
    private static final RecyclableSingleton<ArrowButtonHandler> previousButtonHandler
            = new RecyclableSingleton<ArrowButtonHandler>() {
                @Override
                protected ArrowButtonHandler getInstance() {
                    return new ArrowButtonHandler("decrement", false);
                }
            };

    static ArrowButtonHandler getPreviousButtonHandler() {
        return previousButtonHandler.get();
    }

    private JSpinner spinner;
    private SpinPainter spinPainter;
    private TransparentButton next;
    private TransparentButton prev;

    public static ComponentUI createUI(final JComponent c) {
        return new AquaSpinnerUI();
    }

    private void maybeAdd(final Component c, final String s) {
        if (c != null) {
            spinner.add(c, s);
        }
    }

    boolean wasOpaque;

    @Override
    public void installUI(final JComponent c) {
        this.spinner = (JSpinner) c;
        installDefaults();
        installListeners();
        next = createNextButton();
        prev = createPreviousButton();
        spinPainter = new SpinPainter(next, prev);

        maybeAdd(next, "Next");
        maybeAdd(prev, "Previous");
        maybeAdd(createEditor(), "Editor");
        maybeAdd(spinPainter, "Painter");

        updateEnabledState();
        installKeyboardActions();

        // this doesn't work because JSpinner calls setOpaque(true) directly in it's constructor
        //    LookAndFeel.installProperty(spinner, "opaque", Boolean.FALSE);
        // ...so we have to handle the is/was opaque ourselves
        wasOpaque = spinner.isOpaque();
        spinner.setOpaque(false);
    }

    @Override
    public void uninstallUI(final JComponent c) {
        uninstallDefaults();
        uninstallListeners();
        spinner.setOpaque(wasOpaque);
        spinPainter = null;
        spinner = null;
        // AquaButtonUI install some listeners to all parents, which means that
        // we need to uninstall UI here to remove those listeners, because after
        // we remove them from spinner we lost the latest reference to them,
        // and our standard uninstallUI machinery will not call them.
        next.getUI().uninstallUI(next);
        prev.getUI().uninstallUI(prev);
        next = null;
        prev = null;
        c.removeAll();
    }

    protected void installListeners() {
        spinner.addPropertyChangeListener(getPropertyChangeListener());
        JComponent editor = spinner.getEditor();
        if (editor instanceof DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor)editor).getTextField();
            if (tf != null) {
                tf.addFocusListener(getNextButtonHandler());
                tf.addFocusListener(getPreviousButtonHandler());
            }
        }
    }

    protected void uninstallListeners() {
        JComponent editor = spinner.getEditor();
        if (editor instanceof DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            if (tf != null) {
                tf.removeFocusListener(getNextButtonHandler());
                tf.removeFocusListener(getPreviousButtonHandler());
            }
        }
        spinner.removePropertyChangeListener(getPropertyChangeListener());
    }

    protected void installDefaults() {
        spinner.setLayout(createLayout());
        LookAndFeel.installBorder(spinner, "Spinner.border");
        LookAndFeel.installColorsAndFont(spinner, "Spinner.background", "Spinner.foreground", "Spinner.font");
    }

    protected void uninstallDefaults() {
        LookAndFeel.uninstallBorder(spinner);
        spinner.setLayout(null);
    }

    protected LayoutManager createLayout() {
        return new SpinnerLayout();
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }

    protected TransparentButton createPreviousButton() {
        final TransparentButton b = new TransparentButton();
        b.addActionListener(getPreviousButtonHandler());
        b.addMouseListener(getPreviousButtonHandler());
        b.setInheritsPopupMenu(true);
        return b;
    }

    protected TransparentButton createNextButton() {
        final TransparentButton b = new TransparentButton();
        b.addActionListener(getNextButtonHandler());
        b.addMouseListener(getNextButtonHandler());
        b.setInheritsPopupMenu(true);
        return b;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        JComponent editor = spinner.getEditor();
        Insets insets = spinner.getInsets();
        width = width - insets.left - insets.right;
        height = height - insets.top - insets.bottom;
        if (width >= 0 && height >= 0) {
            int baseline = editor.getBaseline(width, height);
            if (baseline >= 0) {
                return insets.top + baseline;
            }
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(
            JComponent c) {
        super.getBaselineResizeBehavior(c);
        return spinner.getEditor().getBaselineResizeBehavior();
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    class TransparentButton extends JButton implements SwingConstants {

        boolean interceptRepaints = false;

        public TransparentButton() {
            super();
            setFocusable(false);
            // only intercept repaints if we are after this has been initialized
            // otherwise we can't talk to our containing class
            interceptRepaints = true;
        }

        @Override
        public void paint(final Graphics g) {
        }

        @Override
        public void repaint() {
            // only intercept repaints if we are after this has been initialized
            // otherwise we can't talk to our containing class
            if (interceptRepaints) {
                if (spinPainter == null) {
                    return;
                }
                spinPainter.repaint();
            }
            super.repaint();
        }
    }

    protected JComponent createEditor() {
        final JComponent editor = spinner.getEditor();
        fixupEditor(editor);
        return editor;
    }

    protected void replaceEditor(final JComponent oldEditor, final JComponent newEditor) {
        spinner.remove(oldEditor);
        fixupEditor(newEditor);
        spinner.add(newEditor, "Editor");
    }

    protected void fixupEditor(final JComponent editor) {
        if (!(editor instanceof DefaultEditor)) {
            return;
        }

        editor.setOpaque(false);
        editor.setInheritsPopupMenu(true);

        if (editor.getFont() instanceof UIResource) {
            Font font = spinner.getFont();
            editor.setFont(font == null ? null : new FontUIResource(font));
        }

        final JFormattedTextField editorTextField = ((DefaultEditor) editor).getTextField();
        if (editorTextField.getFont() instanceof UIResource) {
            Font font = spinner.getFont();
            editorTextField.setFont(font == null ? null : new FontUIResource(font));
        }
        final InputMap spinnerInputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        final InputMap editorInputMap = editorTextField.getInputMap();
        final KeyStroke[] keys = spinnerInputMap.keys();
        for (final KeyStroke k : keys) {
            editorInputMap.put(k, spinnerInputMap.get(k));
        }
    }

    void updateEnabledState() {
        updateEnabledState(spinner, spinner.isEnabled());
    }

    private void updateEnabledState(final Container c, final boolean enabled) {
        for (int counter = c.getComponentCount() - 1; counter >= 0; counter--) {
            final Component child = c.getComponent(counter);

            child.setEnabled(enabled);
            if (child instanceof Container) {
                updateEnabledState((Container) child, enabled);
            }
        }
    }

    private void installKeyboardActions() {
        final InputMap iMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        SwingUtilities.replaceUIInputMap(spinner, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, iMap);
        SwingUtilities.replaceUIActionMap(spinner, getActionMap());
    }

    private InputMap getInputMap(final int condition) {
        if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) {
            return (InputMap) UIManager.get("Spinner.ancestorInputMap");
        }
        return null;
    }

    private ActionMap getActionMap() {
        ActionMap map = (ActionMap) UIManager.get("Spinner.actionMap");

        if (map == null) {
            map = createActionMap();
            if (map != null) {
                UIManager.getLookAndFeelDefaults().put("Spinner.actionMap", map);
            }
        }
        return map;
    }

    private ActionMap createActionMap() {
        final ActionMap map = new ActionMapUIResource();
        map.put("increment", getNextButtonHandler());
        map.put("decrement", getPreviousButtonHandler());
        return map;
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private static class ArrowButtonHandler extends AbstractAction implements FocusListener, MouseListener {

        final javax.swing.Timer autoRepeatTimer;
        final boolean isNext;
        JSpinner spinner = null;
        JButton arrowButton = null;

        ArrowButtonHandler(final String name, final boolean isNext) {
            super(name);

            this.isNext = isNext;
            autoRepeatTimer = new javax.swing.Timer(60, this);
            autoRepeatTimer.setInitialDelay(300);
        }

        private JSpinner eventToSpinner(final AWTEvent e) {
            Object src = e.getSource();
            while ((src instanceof Component) && !(src instanceof JSpinner)) {
                src = ((Component) src).getParent();
            }
            return (src instanceof JSpinner) ? (JSpinner) src : null;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            if (!(e.getSource() instanceof javax.swing.Timer)) {
                // Most likely resulting from being in ActionMap.
                spinner = eventToSpinner(e);
                if (e.getSource() instanceof JButton) {
                    arrowButton = (JButton)e.getSource();
                }
            } else {
                if (arrowButton != null && !arrowButton.getModel().isPressed()
                    && autoRepeatTimer.isRunning()) {
                    autoRepeatTimer.stop();
                    spinner = null;
                    arrowButton = null;
                }
            }

            if (spinner != null) {

                try {
                    final int calendarField = getCalendarField(spinner);
                    spinner.commitEdit();
                    if (calendarField != -1) {
                        ((SpinnerDateModel) spinner.getModel()).setCalendarField(calendarField);
                    }
                    final Object value = (isNext) ? spinner.getNextValue() : spinner.getPreviousValue();
                    if (value != null) {
                        spinner.setValue(value);
                        select(spinner);
                    }
                } catch (final IllegalArgumentException iae) {
                    UIManager.getLookAndFeel().provideErrorFeedback(spinner);
                } catch (final ParseException pe) {
                    UIManager.getLookAndFeel().provideErrorFeedback(spinner);
                }
            }
        }

        /**
         * If the spinner's editor is a DateEditor, this selects the field
         * associated with the value that is being incremented.
         */
        private void select(final JSpinner spinnerComponent) {
            if (spinnerComponent == null) {
                return;
            }
            final JComponent editor = spinnerComponent.getEditor();
            if (!(editor instanceof JSpinner.DateEditor)) {
                return;
            }

            final JSpinner.DateEditor dateEditor = (JSpinner.DateEditor) editor;
            final JFormattedTextField ftf = dateEditor.getTextField();
            final Format format = dateEditor.getFormat();
            Object value;
            if (format == null || (value = spinnerComponent.getValue()) == null) {
                return;
            }

            final SpinnerDateModel model = dateEditor.getModel();
            final DateFormat.Field field = DateFormat.Field.ofCalendarField(model.getCalendarField());
            if (field == null) {
                return;
            }

            try {
                final AttributedCharacterIterator iterator = format.formatToCharacterIterator(value);
                if (!select(ftf, iterator, field) && field == DateFormat.Field.HOUR0) {
                    select(ftf, iterator, DateFormat.Field.HOUR1);
                }
            } catch (final IllegalArgumentException iae) {
            }
        }

        /**
         * Selects the passed in field, returning true if it is found, false
         * otherwise.
         */
        private boolean select(final JFormattedTextField ftf, final AttributedCharacterIterator iterator, final DateFormat.Field field) {
            final int max = ftf.getDocument().getLength();

            iterator.first();
            do {
                final Map<Attribute, Object> attrs = iterator.getAttributes();
                if (attrs == null || !attrs.containsKey(field)) {
                    continue;
                }

                final int start = iterator.getRunStart(field);
                final int end = iterator.getRunLimit(field);
                if (start != -1 && end != -1 && start <= max && end <= max) {
                    ftf.select(start, end);
                }

                return true;
            } while (iterator.next() != CharacterIterator.DONE);
            return false;
        }

        /**
         * Returns the calendarField under the start of the selection, or -1 if
         * there is no valid calendar field under the selection (or the spinner
         * isn't editing dates.
         */
        private int getCalendarField(final JSpinner spinnerComponent) {
            final JComponent editor = spinnerComponent.getEditor();
            if (!(editor instanceof JSpinner.DateEditor)) {
                return -1;
            }

            final JSpinner.DateEditor dateEditor = (JSpinner.DateEditor) editor;
            final JFormattedTextField ftf = dateEditor.getTextField();
            final int start = ftf.getSelectionStart();
            final JFormattedTextField.AbstractFormatter formatter = ftf.getFormatter();
            if (!(formatter instanceof InternationalFormatter)) {
                return -1;
            }

            final Format.Field[] fields = ((InternationalFormatter) formatter).getFields(start);
            for (final Field element : fields) {
                if (!(element instanceof DateFormat.Field)) {
                    continue;
                }
                int calendarField;

                if (element == DateFormat.Field.HOUR1) {
                    calendarField = Calendar.HOUR;
                } else {
                    calendarField = ((DateFormat.Field) element).getCalendarField();
                }

                if (calendarField != -1) {
                    return calendarField;
                }
            }
            return -1;
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e) || !e.getComponent().isEnabled()) {
                return;
            }
            spinner = eventToSpinner(e);
            autoRepeatTimer.start();

            focusSpinnerIfNecessary();
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            autoRepeatTimer.stop();
            arrowButton = null;
            spinner = null;
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
        }

        @Override
        public void mouseEntered(final MouseEvent e) {
            if (spinner != null && !autoRepeatTimer.isRunning() && spinner == eventToSpinner(e)) {
                autoRepeatTimer.start();
            }
        }

        @Override
        public void mouseExited(final MouseEvent e) {
            if (autoRepeatTimer.isRunning()) {
                autoRepeatTimer.stop();
            }
        }

        /**
         * Requests focus on a child of the spinner if the spinner doesn't have
         * focus.
         */
        private void focusSpinnerIfNecessary() {
            final Component fo = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (!spinner.isRequestFocusEnabled() || (fo != null && (SwingUtilities.isDescendingFrom(fo, spinner)))) {
                return;
            }
            Container root = spinner;

            if (!root.isFocusCycleRoot()) {
                root = root.getFocusCycleRootAncestor();
            }

            if (root == null) {
                return;
            }
            final FocusTraversalPolicy ftp = root.getFocusTraversalPolicy();
            final Component child = ftp.getComponentAfter(root, spinner);

            if (child != null && SwingUtilities.isDescendingFrom(child, spinner)) {
                child.requestFocus();
            }
        }

        public void focusGained(FocusEvent e) {
        }

        public void focusLost(FocusEvent e) {
            if (spinner == eventToSpinner(e)) {
                if (autoRepeatTimer.isRunning()) {
                    autoRepeatTimer.stop();
                }
                spinner = null;
                if (arrowButton != null) {
                    ButtonModel model = arrowButton.getModel();
                    model.setPressed(false);
                    arrowButton = null;
                }
            }
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    class SpinPainter extends JComponent {

        final AquaPainter<JRSUIState> painter = AquaPainter.create(JRSUIStateFactory.getSpinnerArrows());

        ButtonModel fTopModel;
        ButtonModel fBottomModel;

        boolean fPressed = false;
        boolean fTopPressed = false;

        Dimension kPreferredSize = new Dimension(15, 24); // 19,27 before trimming

        public SpinPainter(final AbstractButton top, final AbstractButton bottom) {
            if (top != null) {
                fTopModel = top.getModel();
            }

            if (bottom != null) {
                fBottomModel = bottom.getModel();
            }
            setFocusable(false);
        }

        @Override
        public void paint(final Graphics g) {
            if (spinner.isOpaque()) {
                g.setColor(spinner.getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            AquaUtilControlSize.applySizeForControl(spinner, painter);

            if (isEnabled()) {
                if (fTopModel != null && fTopModel.isPressed()) {
                    painter.state.set(State.PRESSED);
                    painter.state.set(BooleanValue.NO);
                } else if (fBottomModel != null && fBottomModel.isPressed()) {
                    painter.state.set(State.PRESSED);
                    painter.state.set(BooleanValue.YES);
                } else {
                    painter.state.set(State.ACTIVE);
                }
            } else {
                painter.state.set(State.DISABLED);
            }

            final Rectangle bounds = getBounds();
            painter.paint(g, spinner, 0, 0, bounds.width, bounds.height);
        }

        @Override
        public Dimension getPreferredSize() {
            final Size size = AquaUtilControlSize.getUserSizeFrom(this);

            if (size == Size.MINI) {
                return new Dimension(kPreferredSize.width, kPreferredSize.height - 8);
            }

            return kPreferredSize;
        }
    }

    /**
     * A simple layout manager for the editor and the next/previous buttons. See
     * the AquaSpinnerUI javadoc for more information about exactly how the
     * components are arranged.
     */
    static class SpinnerLayout implements LayoutManager {

        private Component nextButton = null;
        private Component previousButton = null;
        private Component editor = null;
        private Component painter = null;

        @Override
        public void addLayoutComponent(final String name, final Component c) {
            if ("Next".equals(name)) {
                nextButton = c;
            } else if ("Previous".equals(name)) {
                previousButton = c;
            } else if ("Editor".equals(name)) {
                editor = c;
            } else if ("Painter".equals(name)) {
                painter = c;
            }
        }

        @Override
        public void removeLayoutComponent(Component c) {
            if (c == nextButton) {
                c = null;
            } else if (c == previousButton) {
                previousButton = null;
            } else if (c == editor) {
                editor = null;
            } else if (c == painter) {
                painter = null;
            }
        }

        private Dimension preferredSize(final Component c) {
            return (c == null) ? new Dimension(0, 0) : c.getPreferredSize();
        }

        @Override
        public Dimension preferredLayoutSize(final Container parent) {
//            Dimension nextD = preferredSize(nextButton);
//            Dimension previousD = preferredSize(previousButton);
            final Dimension editorD = preferredSize(editor);
            final Dimension painterD = preferredSize(painter);

            /* Force the editors height to be a multiple of 2
             */
            editorD.height = ((editorD.height + 1) / 2) * 2;

            final Dimension size = new Dimension(editorD.width, Math.max(painterD.height, editorD.height));
            size.width += painterD.width; //Math.max(nextD.width, previousD.width);
            final Insets insets = parent.getInsets();
            size.width += insets.left + insets.right;
            size.height += insets.top + insets.bottom;
            return size;
        }

        @Override
        public Dimension minimumLayoutSize(final Container parent) {
            return preferredLayoutSize(parent);
        }

        private void setBounds(final Component c, final int x, final int y, final int width, final int height) {
            if (c != null) {
                c.setBounds(x, y, width, height);
            }
        }

        @Override
        public void layoutContainer(final Container parent) {
            final Insets insets = parent.getInsets();
            final int availWidth = parent.getWidth() - (insets.left + insets.right);
            final int availHeight = parent.getHeight() - (insets.top + insets.bottom);

            final Dimension painterD = preferredSize(painter);
//            Dimension nextD = preferredSize(nextButton);
//            Dimension previousD = preferredSize(previousButton);
            final int nextHeight = availHeight / 2;
            final int previousHeight = availHeight - nextHeight;
            final int buttonsWidth = painterD.width; //Math.max(nextD.width, previousD.width);
            final int editorWidth = availWidth - buttonsWidth;

            /* Deal with the spinners componentOrientation property.
             */
            int editorX, buttonsX;
            if (parent.getComponentOrientation().isLeftToRight()) {
                editorX = insets.left;
                buttonsX = editorX + editorWidth;
            } else {
                buttonsX = insets.left;
                editorX = buttonsX + buttonsWidth;
            }

            final int previousY = insets.top + nextHeight;
            final int painterTop = previousY - (painterD.height / 2);
            setBounds(editor, editorX, insets.top, editorWidth, availHeight);
            setBounds(nextButton, buttonsX, insets.top, buttonsWidth, nextHeight);
            setBounds(previousButton, buttonsX, previousY, buttonsWidth, previousHeight);
            setBounds(painter, buttonsX, painterTop, buttonsWidth, painterD.height);
        }
    }

    /**
     * Detect JSpinner property changes we're interested in and delegate.
     * Subclasses shouldn't need to replace the default propertyChangeListener
     * (although they can by overriding createPropertyChangeListener) since all
     * of the interesting property changes are delegated to protected methods.
     */
    static class PropertyChangeHandler implements PropertyChangeListener {

        @Override
        public void propertyChange(final PropertyChangeEvent e) {
            final String propertyName = e.getPropertyName();
            final JSpinner spinner = (JSpinner) (e.getSource());
            final SpinnerUI spinnerUI = spinner.getUI();

            if (spinnerUI instanceof AquaSpinnerUI) {
                final AquaSpinnerUI ui = (AquaSpinnerUI) spinnerUI;

                if ("editor".equals(propertyName)) {
                    final JComponent oldEditor = (JComponent) e.getOldValue();
                    final JComponent newEditor = (JComponent) e.getNewValue();
                    ui.replaceEditor(oldEditor, newEditor);
                    ui.updateEnabledState();
                    if (oldEditor instanceof JSpinner.DefaultEditor) {
                        JTextField tf = ((JSpinner.DefaultEditor)oldEditor).getTextField();
                        if (tf != null) {
                            tf.removeFocusListener(getNextButtonHandler());
                            tf.removeFocusListener(getPreviousButtonHandler());
                        }
                    }
                    if (newEditor instanceof JSpinner.DefaultEditor) {
                        JTextField tf = ((JSpinner.DefaultEditor)newEditor).getTextField();
                        if (tf != null) {
                            if (tf.getFont() instanceof UIResource) {
                                Font font = spinner.getFont();
                                tf.setFont(font == null ? null : new FontUIResource(font));
                            }
                            tf.addFocusListener(getNextButtonHandler());
                            tf.addFocusListener(getPreviousButtonHandler());
                        }
                    }
                } else if ("componentOrientation".equals(propertyName)) {
                    ComponentOrientation o
                            = (ComponentOrientation) e.getNewValue();
                    if (o != e.getOldValue()) {
                        JComponent editor = spinner.getEditor();
                        if (editor != null) {
                            editor.applyComponentOrientation(o);
                        }
                        spinner.revalidate();
                        spinner.repaint();
                    }
                } else if ("enabled".equals(propertyName)) {
                    ui.updateEnabledState();
                } else if (JComponent.TOOL_TIP_TEXT_KEY.equals(propertyName)) {
                    ui.updateToolTipTextForChildren(spinner);
                } else if ("font".equals(propertyName)) {
                    JComponent editor = spinner.getEditor();
                    if (editor instanceof JSpinner.DefaultEditor) {
                        JTextField tf
                                = ((JSpinner.DefaultEditor) editor).getTextField();
                        if (tf != null) {
                            if (tf.getFont() instanceof UIResource) {
                                Font font = spinner.getFont();
                                tf.setFont(font == null ? null : new FontUIResource(font));
                            }
                        }
                    }
                }
            }
        }
    }

    // Syncronizes the ToolTip text for the components within the spinner
    // to be the same value as the spinner ToolTip text.
    void updateToolTipTextForChildren(final JComponent spinnerComponent) {
        final String toolTipText = spinnerComponent.getToolTipText();
        final Component[] children = spinnerComponent.getComponents();
        for (final Component element : children) {
            if (element instanceof JSpinner.DefaultEditor) {
                final JTextField tf = ((JSpinner.DefaultEditor) element).getTextField();
                if (tf != null) {
                    tf.setToolTipText(toolTipText);
                }
            } else if (element instanceof JComponent) {
                ((JComponent) element).setToolTipText(toolTipText);
            }
        }
    }
}
