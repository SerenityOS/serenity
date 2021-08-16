/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.basic;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.ButtonModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.SpinnerUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.InternationalFormatter;

import sun.swing.DefaultLookup;

/**
 * The default Spinner UI delegate.
 *
 * @author Hans Muller
 * @since 1.4
 */
public class BasicSpinnerUI extends SpinnerUI
{
    /**
     * The spinner that we're a UI delegate for.  Initialized by
     * the <code>installUI</code> method, and reset to null
     * by <code>uninstallUI</code>.
     *
     * @see #installUI
     * @see #uninstallUI
     */
    protected JSpinner spinner;
    private Handler handler;


    /**
     * The mouse/action listeners that are added to the spinner's
     * arrow buttons.  These listeners are shared by all
     * spinner arrow buttons.
     *
     * @see #createNextButton
     * @see #createPreviousButton
     */
    private static final ArrowButtonHandler nextButtonHandler = new ArrowButtonHandler("increment", true);
    private static final ArrowButtonHandler previousButtonHandler = new ArrowButtonHandler("decrement", false);
    private PropertyChangeListener propertyChangeListener;


    /**
     * Used by the default LayoutManager class - SpinnerLayout for
     * missing (null) editor/nextButton/previousButton children.
     */
    private static final Dimension zeroSize = new Dimension(0, 0);

    /**
     * Constructs a {@code BasicSpinnerUI}.
     */
    public BasicSpinnerUI() {}

    /**
     * Returns a new instance of BasicSpinnerUI.  SpinnerListUI
     * delegates are allocated one per JSpinner.
     *
     * @param c the JSpinner (not used)
     * @see ComponentUI#createUI
     * @return a new BasicSpinnerUI object
     */
    public static ComponentUI createUI(JComponent c) {
        return new BasicSpinnerUI();
    }


    private void maybeAdd(Component c, String s) {
        if (c != null) {
            spinner.add(c, s);
        }
    }


    /**
     * Calls <code>installDefaults</code>, <code>installListeners</code>,
     * and then adds the components returned by <code>createNextButton</code>,
     * <code>createPreviousButton</code>, and <code>createEditor</code>.
     *
     * @param c the JSpinner
     * @see #installDefaults
     * @see #installListeners
     * @see #createNextButton
     * @see #createPreviousButton
     * @see #createEditor
     */
    public void installUI(JComponent c) {
        this.spinner = (JSpinner)c;
        installDefaults();
        installListeners();
        maybeAdd(createNextButton(), "Next");
        maybeAdd(createPreviousButton(), "Previous");
        maybeAdd(createEditor(), "Editor");
        updateEnabledState();
        installKeyboardActions();
    }


    /**
     * Calls <code>uninstallDefaults</code>, <code>uninstallListeners</code>,
     * and then removes all of the spinners children.
     *
     * @param c the JSpinner (not used)
     */
    public void uninstallUI(JComponent c) {
        uninstallDefaults();
        uninstallListeners();
        this.spinner = null;
        c.removeAll();
    }


    /**
     * Initializes <code>PropertyChangeListener</code> with
     * a shared object that delegates interesting PropertyChangeEvents
     * to protected methods.
     * <p>
     * This method is called by <code>installUI</code>.
     *
     * @see #replaceEditor
     * @see #uninstallListeners
     */
    protected void installListeners() {
        propertyChangeListener = createPropertyChangeListener();
        spinner.addPropertyChangeListener(propertyChangeListener);
        if (DefaultLookup.getBoolean(spinner, this,
            "Spinner.disableOnBoundaryValues", false)) {
            spinner.addChangeListener(getHandler());
        }
        JComponent editor = spinner.getEditor();
        if (editor != null && editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor)editor).getTextField();
            if (tf != null) {
                tf.addFocusListener(nextButtonHandler);
                tf.addFocusListener(previousButtonHandler);
            }
        }
    }


    /**
     * Removes the <code>PropertyChangeListener</code> added
     * by installListeners.
     * <p>
     * This method is called by <code>uninstallUI</code>.
     *
     * @see #installListeners
     */
    protected void uninstallListeners() {
        spinner.removePropertyChangeListener(propertyChangeListener);
        spinner.removeChangeListener(handler);
        JComponent editor = spinner.getEditor();
        removeEditorBorderListener(editor);
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor)editor).getTextField();
            if (tf != null) {
                tf.removeFocusListener(nextButtonHandler);
                tf.removeFocusListener(previousButtonHandler);
            }
        }
        propertyChangeListener = null;
        handler = null;
    }


    /**
     * Initialize the <code>JSpinner</code> <code>border</code>,
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
    protected void installDefaults() {
        spinner.setLayout(createLayout());
        LookAndFeel.installBorder(spinner, "Spinner.border");
        LookAndFeel.installColorsAndFont(spinner, "Spinner.background", "Spinner.foreground", "Spinner.font");
        LookAndFeel.installProperty(spinner, "opaque", Boolean.TRUE);

        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            if (tf != null) {
                if (tf.getFont() instanceof UIResource) {
                    Font font = spinner.getFont();
                    tf.setFont(font == null ? null : new FontUIResource(font));
                }
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
    protected void uninstallDefaults() {
        LookAndFeel.uninstallBorder(spinner);
        spinner.setLayout(null);
    }


    private Handler getHandler() {
        if (handler == null) {
            handler = new Handler();
        }
        return handler;
    }


    /**
     * Installs the necessary listeners on the next button, <code>c</code>,
     * to update the <code>JSpinner</code> in response to a user gesture.
     *
     * @param c Component to install the listeners on
     * @throws NullPointerException if <code>c</code> is null.
     * @see #createNextButton
     * @since 1.5
     */
    protected void installNextButtonListeners(Component c) {
        installButtonListeners(c, nextButtonHandler);
    }

    /**
     * Installs the necessary listeners on the previous button, <code>c</code>,
     * to update the <code>JSpinner</code> in response to a user gesture.
     *
     * @param c Component to install the listeners on.
     * @throws NullPointerException if <code>c</code> is null.
     * @see #createPreviousButton
     * @since 1.5
     */
    protected void installPreviousButtonListeners(Component c) {
        installButtonListeners(c, previousButtonHandler);
    }

    private void installButtonListeners(Component c,
                                        ArrowButtonHandler handler) {
        if (c instanceof JButton) {
            ((JButton)c).addActionListener(handler);
        }
        c.addMouseListener(handler);
    }

    /**
     * Creates a <code>LayoutManager</code> that manages the <code>editor</code>,
     * <code>nextButton</code>, and <code>previousButton</code>
     * children of the JSpinner.  These three children must be
     * added with a constraint that identifies their role:
     * "Editor", "Next", and "Previous". The default layout manager
     * can handle the absence of any of these children.
     *
     * @return a LayoutManager for the editor, next button, and previous button.
     * @see #createNextButton
     * @see #createPreviousButton
     * @see #createEditor
     */
    protected LayoutManager createLayout() {
        return getHandler();
    }


    /**
     * Creates a <code>PropertyChangeListener</code> that can be
     * added to the JSpinner itself.  Typically, this listener
     * will call replaceEditor when the "editor" property changes,
     * since it's the <code>SpinnerUI's</code> responsibility to
     * add the editor to the JSpinner (and remove the old one).
     * This method is called by <code>installListeners</code>.
     *
     * @return A PropertyChangeListener for the JSpinner itself
     * @see #installListeners
     */
    protected PropertyChangeListener createPropertyChangeListener() {
        return getHandler();
    }


    /**
     * Creates a decrement button, i.e. component that replaces the spinner
     * value with the object returned by <code>spinner.getPreviousValue</code>.
     * By default the <code>previousButton</code> is a {@code JButton}. If the
     * decrement button is not needed this method should return {@code null}.
     *
     * @return a component that will replace the spinner's value with the
     *     previous value in the sequence, or {@code null}
     * @see #installUI
     * @see #createNextButton
     * @see #installPreviousButtonListeners
     */
    protected Component createPreviousButton() {
        Component c = createArrowButton(SwingConstants.SOUTH);
        c.setName("Spinner.previousButton");
        installPreviousButtonListeners(c);
        return c;
    }


    /**
     * Creates an increment button, i.e. component that replaces the spinner
     * value with the object returned by <code>spinner.getNextValue</code>.
     * By default the <code>nextButton</code> is a {@code JButton}. If the
     * increment button is not needed this method should return {@code null}.
     *
     * @return a component that will replace the spinner's value with the
     *     next value in the sequence, or {@code null}
     * @see #installUI
     * @see #createPreviousButton
     * @see #installNextButtonListeners
     */
    protected Component createNextButton() {
        Component c = createArrowButton(SwingConstants.NORTH);
        c.setName("Spinner.nextButton");
        installNextButtonListeners(c);
        return c;
    }

    private Component createArrowButton(int direction) {
        JButton b = new BasicArrowButton(direction);
        Border buttonBorder = UIManager.getBorder("Spinner.arrowButtonBorder");
        if (buttonBorder instanceof UIResource) {
            // Wrap the border to avoid having the UIResource be replaced by
            // the ButtonUI. This is the opposite of using BorderUIResource.
            b.setBorder(new CompoundBorder(buttonBorder, null));
        } else {
            b.setBorder(buttonBorder);
        }
        b.setInheritsPopupMenu(true);
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
    protected JComponent createEditor() {
        JComponent editor = spinner.getEditor();
        maybeRemoveEditorBorder(editor);
        installEditorBorderListener(editor);
        editor.setInheritsPopupMenu(true);
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
     * @param oldEditor an old instance of editor
     * @param newEditor a new instance of editor
     * @see #createEditor
     * @see #createPropertyChangeListener
     */
    protected void replaceEditor(JComponent oldEditor, JComponent newEditor) {
        spinner.remove(oldEditor);
        maybeRemoveEditorBorder(newEditor);
        installEditorBorderListener(newEditor);
        newEditor.setInheritsPopupMenu(true);
        spinner.add(newEditor, "Editor");
    }

    private void updateEditorAlignment(JComponent editor) {
        if (editor instanceof JSpinner.DefaultEditor) {
            // if editor alignment isn't set in LAF, we get 0 (CENTER) here
            int alignment = UIManager.getInt("Spinner.editorAlignment");
            JTextField text = ((JSpinner.DefaultEditor)editor).getTextField();
            text.setHorizontalAlignment(alignment);
        }
    }

    /**
     * Remove the border around the inner editor component for LaFs
     * that install an outside border around the spinner,
     */
    private void maybeRemoveEditorBorder(JComponent editor) {
        if (!UIManager.getBoolean("Spinner.editorBorderPainted")) {
            if (editor instanceof JPanel &&
                editor.getBorder() == null &&
                editor.getComponentCount() > 0) {

                editor = (JComponent)editor.getComponent(0);
            }

            if (editor != null && editor.getBorder() instanceof UIResource) {
                editor.setBorder(null);
            }
        }
    }

    /**
     * Remove the border around the inner editor component for LaFs
     * that install an outside border around the spinner,
     */
    private void installEditorBorderListener(JComponent editor) {
        if (!UIManager.getBoolean("Spinner.editorBorderPainted")) {
            if (editor instanceof JPanel &&
                editor.getBorder() == null &&
                editor.getComponentCount() > 0) {

                editor = (JComponent)editor.getComponent(0);
            }
            if (editor != null &&
                (editor.getBorder() == null ||
                 editor.getBorder() instanceof UIResource)) {
                editor.addPropertyChangeListener(getHandler());
            }
        }
    }

    private void removeEditorBorderListener(JComponent editor) {
        if (!UIManager.getBoolean("Spinner.editorBorderPainted")) {
            if (editor instanceof JPanel &&
                editor.getComponentCount() > 0) {

                editor = (JComponent)editor.getComponent(0);
            }
            if (editor != null) {
                editor.removePropertyChangeListener(getHandler());
            }
        }
    }


    /**
     * Updates the enabled state of the children Components based on the
     * enabled state of the <code>JSpinner</code>.
     */
    private void updateEnabledState() {
        updateEnabledState(spinner, spinner.isEnabled());
    }


    /**
     * Recursively updates the enabled state of the child
     * <code>Component</code>s of <code>c</code>.
     */
    private void updateEnabledState(Container c, boolean enabled) {
        for (int counter = c.getComponentCount() - 1; counter >= 0;counter--) {
            Component child = c.getComponent(counter);

            if (DefaultLookup.getBoolean(spinner, this,
                "Spinner.disableOnBoundaryValues", false)) {
                SpinnerModel model = spinner.getModel();
                if (child.getName() == "Spinner.nextButton" &&
                    model.getNextValue() == null) {
                    child.setEnabled(false);
                }
                else if (child.getName() == "Spinner.previousButton" &&
                         model.getPreviousValue() == null) {
                    child.setEnabled(false);
                }
                else {
                    child.setEnabled(enabled);
                }
            }
            else {
                child.setEnabled(enabled);
            }
            if (child instanceof Container) {
                updateEnabledState((Container)child, enabled);
            }
        }
    }


    /**
     * Installs the keyboard Actions onto the JSpinner.
     *
     * @since 1.5
     */
    protected void installKeyboardActions() {
        InputMap iMap = getInputMap(JComponent.
                                   WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        SwingUtilities.replaceUIInputMap(spinner, JComponent.
                                         WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                                         iMap);

        LazyActionMap.installLazyActionMap(spinner, BasicSpinnerUI.class,
                "Spinner.actionMap");
    }

    /**
     * Returns the InputMap to install for <code>condition</code>.
     */
    private InputMap getInputMap(int condition) {
        if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) {
            return (InputMap)DefaultLookup.get(spinner, this,
                    "Spinner.ancestorInputMap");
        }
        return null;
    }

    static void loadActionMap(LazyActionMap map) {
        map.put("increment", nextButtonHandler);
        map.put("decrement", previousButtonHandler);
    }

    /**
     * Returns the baseline.
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @see javax.swing.JComponent#getBaseline(int, int)
     * @since 1.6
     */
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
     * Returns an enum indicating how the baseline of the component
     * changes as the size changes.
     *
     * @throws NullPointerException {@inheritDoc}
     * @see javax.swing.JComponent#getBaseline(int, int)
     * @since 1.6
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(
            JComponent c) {
        super.getBaselineResizeBehavior(c);
        return spinner.getEditor().getBaselineResizeBehavior();
    }

    /**
     * A handler for spinner arrow button mouse and action events.  When
     * a left mouse pressed event occurs we look up the (enabled) spinner
     * that's the source of the event and start the autorepeat timer.  The
     * timer fires action events until any button is released at which
     * point the timer is stopped and the reference to the spinner cleared.
     * The timer doesn't start until after a 300ms delay, so often the
     * source of the initial (and final) action event is just the button
     * logic for mouse released - which means that we're relying on the fact
     * that our mouse listener runs after the buttons mouse listener.
     * <p>
     * Note that one instance of this handler is shared by all slider previous
     * arrow buttons and likewise for all of the next buttons,
     * so it doesn't have any state that persists beyond the limits
     * of a single button pressed/released gesture.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private static class ArrowButtonHandler extends AbstractAction
                                            implements FocusListener, MouseListener, UIResource {
        final javax.swing.Timer autoRepeatTimer;
        final boolean isNext;
        JSpinner spinner = null;
        JButton arrowButton = null;

        ArrowButtonHandler(String name, boolean isNext) {
            super(name);
            this.isNext = isNext;
            autoRepeatTimer = new javax.swing.Timer(60, this);
            autoRepeatTimer.setInitialDelay(300);
        }

        private JSpinner eventToSpinner(AWTEvent e) {
            Object src = e.getSource();
            while ((src instanceof Component) && !(src instanceof JSpinner)) {
                src = ((Component)src).getParent();
            }
            return (src instanceof JSpinner) ? (JSpinner)src : null;
        }

        public void actionPerformed(ActionEvent e) {
            JSpinner spinner = this.spinner;

            if (!(e.getSource() instanceof javax.swing.Timer)) {
                // Most likely resulting from being in ActionMap.
                spinner = eventToSpinner(e);
                if (e.getSource() instanceof JButton) {
                    arrowButton = (JButton)e.getSource();
                }
            } else {
                if (arrowButton!=null && !arrowButton.getModel().isPressed()
                    && autoRepeatTimer.isRunning()) {
                    autoRepeatTimer.stop();
                    spinner = null;
                    arrowButton = null;
                }
            }
            if (spinner != null) {
                try {
                    int calendarField = getCalendarField(spinner);
                    spinner.commitEdit();
                    if (calendarField != -1) {
                        ((SpinnerDateModel)spinner.getModel()).
                                 setCalendarField(calendarField);
                    }
                    Object value = (isNext) ? spinner.getNextValue() :
                               spinner.getPreviousValue();
                    if (value != null) {
                        spinner.setValue(value);
                        select(spinner);
                    }
                } catch (IllegalArgumentException iae) {
                    UIManager.getLookAndFeel().provideErrorFeedback(spinner);
                } catch (ParseException pe) {
                    UIManager.getLookAndFeel().provideErrorFeedback(spinner);
                }
            }
        }

        /**
         * If the spinner's editor is a DateEditor, this selects the field
         * associated with the value that is being incremented.
         */
        private void select(JSpinner spinner) {
            JComponent editor = spinner.getEditor();

            if (editor instanceof JSpinner.DateEditor) {
                JSpinner.DateEditor dateEditor = (JSpinner.DateEditor)editor;
                JFormattedTextField ftf = dateEditor.getTextField();
                Format format = dateEditor.getFormat();
                Object value;

                if (format != null && (value = spinner.getValue()) != null) {
                    SpinnerDateModel model = dateEditor.getModel();
                    DateFormat.Field field = DateFormat.Field.ofCalendarField(
                        model.getCalendarField());

                    if (field != null) {
                        try {
                            AttributedCharacterIterator iterator = format.
                                formatToCharacterIterator(value);
                            if (!select(ftf, iterator, field) &&
                                       field == DateFormat.Field.HOUR0) {
                                select(ftf, iterator, DateFormat.Field.HOUR1);
                            }
                        }
                        catch (IllegalArgumentException iae) {}
                    }
                }
            }
        }

        /**
         * Selects the passed in field, returning true if it is found,
         * false otherwise.
         */
        private boolean select(JFormattedTextField ftf,
                               AttributedCharacterIterator iterator,
                               DateFormat.Field field) {
            int max = ftf.getDocument().getLength();

            iterator.first();
            do {
                Map<?, ?> attrs = iterator.getAttributes();

                if (attrs != null && attrs.containsKey(field)){
                    int start = iterator.getRunStart(field);
                    int end = iterator.getRunLimit(field);

                    if (start != -1 && end != -1 && start <= max &&
                                       end <= max) {
                        ftf.select(start, end);
                    }
                    return true;
                }
            } while (iterator.next() != CharacterIterator.DONE);
            return false;
        }

        /**
         * Returns the calendarField under the start of the selection, or
         * -1 if there is no valid calendar field under the selection (or
         * the spinner isn't editing dates.
         */
        private int getCalendarField(JSpinner spinner) {
            JComponent editor = spinner.getEditor();

            if (editor instanceof JSpinner.DateEditor) {
                JSpinner.DateEditor dateEditor = (JSpinner.DateEditor)editor;
                JFormattedTextField ftf = dateEditor.getTextField();
                int start = ftf.getSelectionStart();
                JFormattedTextField.AbstractFormatter formatter =
                                    ftf.getFormatter();

                if (formatter instanceof InternationalFormatter) {
                    Format.Field[] fields = ((InternationalFormatter)
                                             formatter).getFields(start);

                    for (int counter = 0; counter < fields.length; counter++) {
                        if (fields[counter] instanceof DateFormat.Field) {
                            int calendarField;

                            if (fields[counter] == DateFormat.Field.HOUR1) {
                                calendarField = Calendar.HOUR;
                            }
                            else {
                                calendarField = ((DateFormat.Field)
                                        fields[counter]).getCalendarField();
                            }
                            if (calendarField != -1) {
                                return calendarField;
                            }
                        }
                    }
                }
            }
            return -1;
        }

        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && e.getComponent().isEnabled()) {
                spinner = eventToSpinner(e);
                autoRepeatTimer.start();

                focusSpinnerIfNecessary();
            }
        }

        public void mouseReleased(MouseEvent e) {
            autoRepeatTimer.stop();
            arrowButton = null;
            spinner = null;
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
            if (spinner != null && !autoRepeatTimer.isRunning() && spinner == eventToSpinner(e)) {
                autoRepeatTimer.start();
            }
        }

        public void mouseExited(MouseEvent e) {
            if (autoRepeatTimer.isRunning()) {
                autoRepeatTimer.stop();
            }
        }

        /**
         * Requests focus on a child of the spinner if the spinner doesn't
         * have focus.
         */
        private void focusSpinnerIfNecessary() {
            Component fo = KeyboardFocusManager.
                              getCurrentKeyboardFocusManager().getFocusOwner();
            if (spinner.isRequestFocusEnabled() && (
                        fo == null ||
                        !SwingUtilities.isDescendingFrom(fo, spinner))) {
                Container root = spinner;

                if (!root.isFocusCycleRoot()) {
                    root = root.getFocusCycleRootAncestor();
                }
                if (root != null) {
                    FocusTraversalPolicy ftp = root.getFocusTraversalPolicy();
                    Component child = ftp.getComponentAfter(root, spinner);

                    if (child != null && SwingUtilities.isDescendingFrom(
                                                        child, spinner)) {
                        child.requestFocus();
                    }
                }
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
                    model.setArmed(false);
                    arrowButton = null;
                }
            }
        }
    }


    private static class Handler implements LayoutManager,
            PropertyChangeListener, ChangeListener {
        //
        // LayoutManager
        //
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
            return (c == null) ? zeroSize : c.getPreferredSize();
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
            int width  = parent.getWidth();
            int height = parent.getHeight();

            Insets insets = parent.getInsets();

            if (nextButton == null && previousButton == null) {
                setBounds(editor, insets.left,  insets.top, width - insets.left - insets.right,
                        height - insets.top - insets.bottom);

                return;
            }

            Dimension nextD = preferredSize(nextButton);
            Dimension previousD = preferredSize(previousButton);
            int buttonsWidth = Math.max(nextD.width, previousD.width);
            int editorHeight = height - (insets.top + insets.bottom);

            // The arrowButtonInsets value is used instead of the JSpinner's
            // insets if not null. Defining this to be (0, 0, 0, 0) causes the
            // buttons to be aligned with the outer edge of the spinner's
            // border, and leaving it as "null" places the buttons completely
            // inside the spinner's border.
            Insets buttonInsets = UIManager.getInsets("Spinner.arrowButtonInsets");
            if (buttonInsets == null) {
                buttonInsets = insets;
            }

            /* Deal with the spinner's componentOrientation property.
             */
            int editorX, editorWidth, buttonsX;
            if (parent.getComponentOrientation().isLeftToRight()) {
                editorX = insets.left;
                editorWidth = width - insets.left - buttonsWidth - buttonInsets.right;
                buttonsX = width - buttonsWidth - buttonInsets.right;
            } else {
                buttonsX = buttonInsets.left;
                editorX = buttonsX + buttonsWidth;
                editorWidth = width - buttonInsets.left - buttonsWidth - insets.right;
            }

            int nextY = buttonInsets.top;
            int nextHeight = (height / 2) + (height % 2) - nextY;
            int previousY = buttonInsets.top + nextHeight;
            int previousHeight = height - previousY - buttonInsets.bottom;

            setBounds(editor,         editorX,  insets.top, editorWidth, editorHeight);
            setBounds(nextButton,     buttonsX, nextY,      buttonsWidth, nextHeight);
            setBounds(previousButton, buttonsX, previousY,  buttonsWidth, previousHeight);
        }


        //
        // PropertyChangeListener
        //
        public void propertyChange(PropertyChangeEvent e)
        {
            String propertyName = e.getPropertyName();
            if (e.getSource() instanceof JSpinner) {
                JSpinner spinner = (JSpinner)(e.getSource());
                SpinnerUI spinnerUI = spinner.getUI();

                if (spinnerUI instanceof BasicSpinnerUI) {
                    BasicSpinnerUI ui = (BasicSpinnerUI)spinnerUI;

                    if ("editor".equals(propertyName)) {
                        JComponent oldEditor = (JComponent)e.getOldValue();
                        JComponent newEditor = (JComponent)e.getNewValue();
                        ui.replaceEditor(oldEditor, newEditor);
                        ui.updateEnabledState();
                        if (oldEditor instanceof JSpinner.DefaultEditor) {
                            JTextField tf =
                                ((JSpinner.DefaultEditor)oldEditor).getTextField();
                            if (tf != null) {
                                tf.removeFocusListener(nextButtonHandler);
                                tf.removeFocusListener(previousButtonHandler);
                            }
                        }
                        if (newEditor instanceof JSpinner.DefaultEditor) {
                            JTextField tf =
                                ((JSpinner.DefaultEditor)newEditor).getTextField();
                            if (tf != null) {
                                if (tf.getFont() instanceof UIResource) {
                                    Font font = spinner.getFont();
                                    tf.setFont(font == null ? null : new FontUIResource(font));
                                }
                                tf.addFocusListener(nextButtonHandler);
                                tf.addFocusListener(previousButtonHandler);
                            }
                        }
                    }
                    else if ("enabled".equals(propertyName) ||
                             "model".equals(propertyName)) {
                        ui.updateEnabledState();
                    }
                    else if ("font".equals(propertyName)) {
                        JComponent editor = spinner.getEditor();
                        if (editor instanceof JSpinner.DefaultEditor) {
                            JTextField tf =
                                ((JSpinner.DefaultEditor)editor).getTextField();
                            if (tf != null) {
                                if (tf.getFont() instanceof UIResource) {
                                    Font font = spinner.getFont();
                                    tf.setFont(font == null ? null : new FontUIResource(font));
                                }
                            }
                        }
                    }
                    else if (JComponent.TOOL_TIP_TEXT_KEY.equals(propertyName)) {
                        updateToolTipTextForChildren(spinner);
                    } else if ("componentOrientation".equals(propertyName)) {
                        ComponentOrientation o
                                = (ComponentOrientation) e.getNewValue();
                        if (o != (ComponentOrientation) e.getOldValue()) {
                            JComponent editor = spinner.getEditor();
                            if (editor != null) {
                                editor.applyComponentOrientation(o);
                            }
                            spinner.revalidate();
                            spinner.repaint();
                        }
                    }
                }
            } else if (e.getSource() instanceof JComponent) {
                JComponent c = (JComponent)e.getSource();
                if ((c.getParent() instanceof JPanel) &&
                    (c.getParent().getParent() instanceof JSpinner) &&
                    "border".equals(propertyName)) {

                    JSpinner spinner = (JSpinner)c.getParent().getParent();
                    SpinnerUI spinnerUI = spinner.getUI();
                    if (spinnerUI instanceof BasicSpinnerUI) {
                        BasicSpinnerUI ui = (BasicSpinnerUI)spinnerUI;
                        ui.maybeRemoveEditorBorder(c);
                    }
                }
            }
        }

        // Syncronizes the ToolTip text for the components within the spinner
        // to be the same value as the spinner ToolTip text.
        private void updateToolTipTextForChildren(JComponent spinner) {
            String toolTipText = spinner.getToolTipText();
            Component[] children = spinner.getComponents();
            for (int i = 0; i < children.length; i++) {
                if (children[i] instanceof JSpinner.DefaultEditor) {
                    JTextField tf = ((JSpinner.DefaultEditor)children[i]).getTextField();
                    if (tf != null) {
                        tf.setToolTipText(toolTipText);
                    }
                } else if (children[i] instanceof JComponent) {
                    ((JComponent)children[i]).setToolTipText( spinner.getToolTipText() );
                }
            }
        }

        public void stateChanged(ChangeEvent e) {
            if (e.getSource() instanceof JSpinner) {
                JSpinner spinner = (JSpinner)e.getSource();
                SpinnerUI spinnerUI = spinner.getUI();
                if (DefaultLookup.getBoolean(spinner, spinnerUI,
                    "Spinner.disableOnBoundaryValues", false) &&
                    spinnerUI instanceof BasicSpinnerUI) {
                    BasicSpinnerUI ui = (BasicSpinnerUI)spinnerUI;
                    ui.updateEnabledState();
                }
            }
        }
    }
}
