/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing;

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import javax.swing.text.TextAction;

/**
 * <code>JTextField</code> is a lightweight component that allows the editing
 * of a single line of text.
 * For information on and examples of using text fields,
 * see
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/textfield.html">How to Use Text Fields</a>
 * in <em>The Java Tutorial.</em>
 *
 * <p>
 * <code>JTextField</code> is intended to be source-compatible
 * with <code>java.awt.TextField</code> where it is reasonable to do so.  This
 * component has capabilities not found in the <code>java.awt.TextField</code>
 * class.  The superclass should be consulted for additional capabilities.
 * <p>
 * <code>JTextField</code> has a method to establish the string used as the
 * command string for the action event that gets fired.  The
 * <code>java.awt.TextField</code> used the text of the field as the command
 * string for the <code>ActionEvent</code>.
 * <code>JTextField</code> will use the command
 * string set with the <code>setActionCommand</code> method if not <code>null</code>,
 * otherwise it will use the text of the field as a compatibility with
 * <code>java.awt.TextField</code>.
 * <p>
 * The method <code>setEchoChar</code> and <code>getEchoChar</code>
 * are not provided directly to avoid a new implementation of a
 * pluggable look-and-feel inadvertently exposing password characters.
 * To provide password-like services a separate class <code>JPasswordField</code>
 * extends <code>JTextField</code> to provide this service with an independently
 * pluggable look-and-feel.
 * <p>
 * The <code>java.awt.TextField</code> could be monitored for changes by adding
 * a <code>TextListener</code> for <code>TextEvent</code>'s.
 * In the <code>JTextComponent</code> based
 * components, changes are broadcasted from the model via a
 * <code>DocumentEvent</code> to <code>DocumentListeners</code>.
 * The <code>DocumentEvent</code> gives
 * the location of the change and the kind of change if desired.
 * The code fragment might look something like:
 * <pre><code>
 * &nbsp;   DocumentListener myListener = ??;
 * &nbsp;   JTextField myArea = ??;
 * &nbsp;   myArea.getDocument().addDocumentListener(myListener);
 * </code></pre>
 * <p>
 * The horizontal alignment of <code>JTextField</code> can be set to be left
 * justified, leading justified, centered, right justified or trailing justified.
 * Right/trailing justification is useful if the required size
 * of the field text is smaller than the size allocated to it.
 * This is determined by the <code>setHorizontalAlignment</code>
 * and <code>getHorizontalAlignment</code> methods.  The default
 * is to be leading justified.
 * <p>
 * How the text field consumes VK_ENTER events depends
 * on whether the text field has any action listeners.
 * If so, then VK_ENTER results in the listeners
 * getting an ActionEvent,
 * and the VK_ENTER event is consumed.
 * This is compatible with how AWT text fields handle VK_ENTER events.
 * If the text field has no action listeners, then as of v 1.3 the VK_ENTER
 * event is not consumed.  Instead, the bindings of ancestor components
 * are processed, which enables the default button feature of
 * JFC/Swing to work.
 * <p>
 * Customized fields can easily be created by extending the model and
 * changing the default model provided.  For example, the following piece
 * of code will create a field that holds only upper case characters.  It
 * will work even if text is pasted into from the clipboard or it is altered via
 * programmatic changes.
 * <pre><code>

&nbsp;public class UpperCaseField extends JTextField {
&nbsp;
&nbsp;    public UpperCaseField(int cols) {
&nbsp;        super(cols);
&nbsp;    }
&nbsp;
&nbsp;    protected Document createDefaultModel() {
&nbsp;        return new UpperCaseDocument();
&nbsp;    }
&nbsp;
&nbsp;    static class UpperCaseDocument extends PlainDocument {
&nbsp;
&nbsp;        public void insertString(int offs, String str, AttributeSet a)
&nbsp;            throws BadLocationException {
&nbsp;
&nbsp;            if (str == null) {
&nbsp;                return;
&nbsp;            }
&nbsp;            char[] upper = str.toCharArray();
&nbsp;            for (int i = 0; i &lt; upper.length; i++) {
&nbsp;                upper[i] = Character.toUpperCase(upper[i]);
&nbsp;            }
&nbsp;            super.insertString(offs, new String(upper), a);
&nbsp;        }
&nbsp;    }
&nbsp;}

 * </code></pre>
 * <p>
 * <strong>Warning:</strong> Swing is not thread safe. For more
 * information see <a
 * href="package-summary.html#threading">Swing's Threading
 * Policy</a>.
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
 * @author  Timothy Prinzing
 * @see #setActionCommand
 * @see JPasswordField
 * @see #addActionListener
 * @since 1.2
 */
@JavaBean(defaultProperty = "UIClassID", description = "A component which allows for the editing of a single line of text.")
@SwingContainer(false)
@SuppressWarnings("serial") // Same-version serialization only
public class JTextField extends JTextComponent implements SwingConstants {

    /**
     * Constructs a new <code>TextField</code>.  A default model is created,
     * the initial string is <code>null</code>,
     * and the number of columns is set to 0.
     */
    public JTextField() {
        this(null, null, 0);
    }

    /**
     * Constructs a new <code>TextField</code> initialized with the
     * specified text. A default model is created and the number of
     * columns is 0.
     *
     * @param text the text to be displayed, or <code>null</code>
     */
    public JTextField(String text) {
        this(null, text, 0);
    }

    /**
     * Constructs a new empty <code>TextField</code> with the specified
     * number of columns.
     * A default model is created and the initial string is set to
     * <code>null</code>.
     *
     * @param columns  the number of columns to use to calculate
     *   the preferred width; if columns is set to zero, the
     *   preferred width will be whatever naturally results from
     *   the component implementation
     */
    public JTextField(int columns) {
        this(null, null, columns);
    }

    /**
     * Constructs a new <code>TextField</code> initialized with the
     * specified text and columns.  A default model is created.
     *
     * @param text the text to be displayed, or <code>null</code>
     * @param columns  the number of columns to use to calculate
     *   the preferred width; if columns is set to zero, the
     *   preferred width will be whatever naturally results from
     *   the component implementation
     */
    public JTextField(String text, int columns) {
        this(null, text, columns);
    }

    /**
     * Constructs a new <code>JTextField</code> that uses the given text
     * storage model and the given number of columns.
     * This is the constructor through which the other constructors feed.
     * If the document is <code>null</code>, a default model is created.
     *
     * @param doc  the text storage to use; if this is <code>null</code>,
     *          a default will be provided by calling the
     *          <code>createDefaultModel</code> method
     * @param text  the initial string to display, or <code>null</code>
     * @param columns  the number of columns to use to calculate
     *   the preferred width &gt;= 0; if <code>columns</code>
     *   is set to zero, the preferred width will be whatever
     *   naturally results from the component implementation
     * @exception IllegalArgumentException if <code>columns</code> &lt; 0
     */
    public JTextField(Document doc, String text, int columns) {
        if (columns < 0) {
            throw new IllegalArgumentException("columns less than zero.");
        }
        visibility = new DefaultBoundedRangeModel();
        visibility.addChangeListener(new ScrollRepainter());
        this.columns = columns;
        if (doc == null) {
            doc = createDefaultModel();
        }
        setDocument(doc);
        if (text != null) {
            setText(text);
        }
    }

    /**
     * Gets the class ID for a UI.
     *
     * @return the string "TextFieldUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false)
    public String getUIClassID() {
        return uiClassID;
    }


    /**
     * Associates the editor with a text document.
     * The currently registered factory is used to build a view for
     * the document, which gets displayed by the editor after revalidation.
     * A PropertyChange event ("document") is propagated to each listener.
     *
     * @param doc  the document to display/edit
     * @see #getDocument
     */
    @BeanProperty(expert = true, description
            = "the text document model")
    public void setDocument(Document doc) {
        if (doc != null) {
            doc.putProperty("filterNewlines", Boolean.TRUE);
        }
        super.setDocument(doc);
    }

    /**
     * Calls to <code>revalidate</code> that come from within the
     * textfield itself will
     * be handled by validating the textfield, unless the textfield
     * is contained within a <code>JViewport</code>,
     * in which case this returns false.
     *
     * @return if the parent of this textfield is a <code>JViewPort</code>
     *          return false, otherwise return true
     *
     * @see JComponent#revalidate
     * @see JComponent#isValidateRoot
     * @see java.awt.Container#isValidateRoot
     */
    @Override
    public boolean isValidateRoot() {
        return !(SwingUtilities.getUnwrappedParent(this) instanceof JViewport);
    }


    /**
     * Returns the horizontal alignment of the text.
     * Valid keys are:
     * <ul>
     * <li><code>JTextField.LEFT</code>
     * <li><code>JTextField.CENTER</code>
     * <li><code>JTextField.RIGHT</code>
     * <li><code>JTextField.LEADING</code>
     * <li><code>JTextField.TRAILING</code>
     * </ul>
     *
     * @return the horizontal alignment
     */
    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    /**
     * Sets the horizontal alignment of the text.
     * Valid keys are:
     * <ul>
     * <li><code>JTextField.LEFT</code>
     * <li><code>JTextField.CENTER</code>
     * <li><code>JTextField.RIGHT</code>
     * <li><code>JTextField.LEADING</code>
     * <li><code>JTextField.TRAILING</code>
     * </ul>
     * <code>invalidate</code> and <code>repaint</code> are called when the
     * alignment is set,
     * and a <code>PropertyChange</code> event ("horizontalAlignment") is fired.
     *
     * @param alignment the alignment
     * @exception IllegalArgumentException if <code>alignment</code>
     *  is not a valid key
     */
     @BeanProperty(preferred = true, enumerationValues = {
             "JTextField.LEFT",
             "JTextField.CENTER",
             "JTextField.RIGHT",
             "JTextField.LEADING",
             "JTextField.TRAILING"}, description
             = "Set the field alignment to LEFT, CENTER, RIGHT, LEADING (the default) or TRAILING")
     public void setHorizontalAlignment(int alignment) {
        if (alignment == horizontalAlignment) return;
        int oldValue = horizontalAlignment;
        if ((alignment == LEFT) || (alignment == CENTER) ||
            (alignment == RIGHT)|| (alignment == LEADING) ||
            (alignment == TRAILING)) {
            horizontalAlignment = alignment;
        } else {
            throw new IllegalArgumentException("horizontalAlignment");
        }
        firePropertyChange("horizontalAlignment", oldValue, horizontalAlignment);
        invalidate();
        repaint();
    }

    /**
     * Creates the default implementation of the model
     * to be used at construction if one isn't explicitly
     * given.  An instance of <code>PlainDocument</code> is returned.
     *
     * @return the default model implementation
     */
    protected Document createDefaultModel() {
        return new PlainDocument();
    }

    /**
     * Returns the number of columns in this <code>TextField</code>.
     *
     * @return the number of columns &gt;= 0
     */
    public int getColumns() {
        return columns;
    }

    /**
     * Sets the number of columns in this <code>TextField</code>,
     * and then invalidate the layout.
     *
     * @param columns the number of columns &gt;= 0
     * @exception IllegalArgumentException if <code>columns</code>
     *          is less than 0
     */
    @BeanProperty(bound = false, description
            = "the number of columns preferred for display")
    public void setColumns(int columns) {
        int oldVal = this.columns;
        if (columns < 0) {
            throw new IllegalArgumentException("columns less than zero.");
        }
        if (columns != oldVal) {
            this.columns = columns;
            invalidate();
        }
    }

    /**
     * Returns the column width.
     * The meaning of what a column is can be considered a fairly weak
     * notion for some fonts.  This method is used to define the width
     * of a column.  By default this is defined to be the width of the
     * character <em>m</em> for the font used.  This method can be
     * redefined to be some alternative amount
     *
     * @return the column width &gt;= 1
     */
    protected int getColumnWidth() {
        if (columnWidth == 0) {
            FontMetrics metrics = getFontMetrics(getFont());
            columnWidth = metrics.charWidth('m');
        }
        return columnWidth;
    }

    /**
     * Returns the preferred size <code>Dimensions</code> needed for this
     * <code>TextField</code>.  If a non-zero number of columns has been
     * set, the width is set to the columns multiplied by
     * the column width.
     *
     * @return the dimension of this textfield
     */
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        if (columns != 0) {
            Insets insets = getInsets();
            size.width = columns * getColumnWidth() +
                insets.left + insets.right;
        }
        return size;
    }

    /**
     * Sets the current font.  This removes cached row height and column
     * width so the new font will be reflected.
     * <code>revalidate</code> is called after setting the font.
     *
     * @param f the new font
     */
    public void setFont(Font f) {
        super.setFont(f);
        columnWidth = 0;
    }

    /**
     * Adds the specified action listener to receive
     * action events from this textfield.
     *
     * @param l the action listener to be added
     */
    public synchronized void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    /**
     * Removes the specified action listener so that it no longer
     * receives action events from this textfield.
     *
     * @param l the action listener to be removed
     */
    public synchronized void removeActionListener(ActionListener l) {
        if ((l != null) && (getAction() == l)) {
            setAction(null);
        } else {
            listenerList.remove(ActionListener.class, l);
        }
    }

    /**
     * Returns an array of all the <code>ActionListener</code>s added
     * to this JTextField with addActionListener().
     *
     * @return all of the <code>ActionListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    @BeanProperty(bound = false)
    public synchronized ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created.
     * The listener list is processed in last to
     * first order.
     * @see EventListenerList
     */
    @SuppressWarnings("deprecation")
    protected void fireActionPerformed() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        int modifiers = 0;
        AWTEvent currentEvent = EventQueue.getCurrentEvent();
        if (currentEvent instanceof InputEvent) {
            modifiers = ((InputEvent)currentEvent).getModifiers();
        } else if (currentEvent instanceof ActionEvent) {
            modifiers = ((ActionEvent)currentEvent).getModifiers();
        }
        ActionEvent e =
            new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                            (command != null) ? command : getText(),
                            EventQueue.getMostRecentEventTime(), modifiers);

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ActionListener.class) {
                ((ActionListener)listeners[i+1]).actionPerformed(e);
            }
        }
    }

    /**
     * Sets the command string used for action events.
     *
     * @param command the command string
     */
    public void setActionCommand(String command) {
        this.command = command;
    }

    private Action action;
    private PropertyChangeListener actionPropertyChangeListener;

    /**
     * Sets the <code>Action</code> for the <code>ActionEvent</code> source.
     * The new <code>Action</code> replaces
     * any previously set <code>Action</code> but does not affect
     * <code>ActionListeners</code> independently
     * added with <code>addActionListener</code>.
     * If the <code>Action</code> is already a registered
     * <code>ActionListener</code>
     * for the <code>ActionEvent</code> source, it is not re-registered.
     * <p>
     * Setting the <code>Action</code> results in immediately changing
     * all the properties described in <a href="Action.html#buttonActions">
     * Swing Components Supporting <code>Action</code></a>.
     * Subsequently, the textfield's properties are automatically updated
     * as the <code>Action</code>'s properties change.
     * <p>
     * This method uses three other methods to set
     * and help track the <code>Action</code>'s property values.
     * It uses the <code>configurePropertiesFromAction</code> method
     * to immediately change the textfield's properties.
     * To track changes in the <code>Action</code>'s property values,
     * this method registers the <code>PropertyChangeListener</code>
     * returned by <code>createActionPropertyChangeListener</code>. The
     * default {@code PropertyChangeListener} invokes the
     * {@code actionPropertyChanged} method when a property in the
     * {@code Action} changes.
     *
     * @param a the <code>Action</code> for the <code>JTextField</code>,
     *          or <code>null</code>
     * @since 1.3
     * @see Action
     * @see #getAction
     * @see #configurePropertiesFromAction
     * @see #createActionPropertyChangeListener
     * @see #actionPropertyChanged
     */
    @BeanProperty(visualUpdate = true, description
            = "the Action instance connected with this ActionEvent source")
    public void setAction(Action a) {
        Action oldValue = getAction();
        if (action==null || !action.equals(a)) {
            action = a;
            if (oldValue!=null) {
                removeActionListener(oldValue);
                oldValue.removePropertyChangeListener(actionPropertyChangeListener);
                actionPropertyChangeListener = null;
            }
            configurePropertiesFromAction(action);
            if (action!=null) {
                // Don't add if it is already a listener
                if (!isListener(ActionListener.class, action)) {
                    addActionListener(action);
                }
                // Reverse linkage:
                actionPropertyChangeListener = createActionPropertyChangeListener(action);
                action.addPropertyChangeListener(actionPropertyChangeListener);
            }
            firePropertyChange("action", oldValue, action);
        }
    }

    private boolean isListener(Class<?> c, ActionListener a) {
        boolean isListener = false;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==c && listeners[i+1]==a) {
                    isListener=true;
            }
        }
        return isListener;
    }

    /**
     * Returns the currently set <code>Action</code> for this
     * <code>ActionEvent</code> source, or <code>null</code>
     * if no <code>Action</code> is set.
     *
     * @return the <code>Action</code> for this <code>ActionEvent</code> source,
     *          or <code>null</code>
     * @since 1.3
     * @see Action
     * @see #setAction
     */
    public Action getAction() {
        return action;
    }

    /**
     * Sets the properties on this textfield to match those in the specified
     * <code>Action</code>.  Refer to <a href="Action.html#buttonActions">
     * Swing Components Supporting <code>Action</code></a> for more
     * details as to which properties this sets.
     *
     * @param a the <code>Action</code> from which to get the properties,
     *          or <code>null</code>
     * @since 1.3
     * @see Action
     * @see #setAction
     */
    protected void configurePropertiesFromAction(Action a) {
        AbstractAction.setEnabledFromAction(this, a);
        AbstractAction.setToolTipTextFromAction(this, a);
        setActionCommandFromAction(a);
    }

    /**
     * Updates the textfield's state in response to property changes in
     * associated action. This method is invoked from the
     * {@code PropertyChangeListener} returned from
     * {@code createActionPropertyChangeListener}. Subclasses do not normally
     * need to invoke this. Subclasses that support additional {@code Action}
     * properties should override this and
     * {@code configurePropertiesFromAction}.
     * <p>
     * Refer to the table at <a href="Action.html#buttonActions">
     * Swing Components Supporting <code>Action</code></a> for a list of
     * the properties this method sets.
     *
     * @param action the <code>Action</code> associated with this textfield
     * @param propertyName the name of the property that changed
     * @since 1.6
     * @see Action
     * @see #configurePropertiesFromAction
     */
    protected void actionPropertyChanged(Action action, String propertyName) {
        if (propertyName == Action.ACTION_COMMAND_KEY) {
            setActionCommandFromAction(action);
        } else if (propertyName == "enabled") {
            AbstractAction.setEnabledFromAction(this, action);
        } else if (propertyName == Action.SHORT_DESCRIPTION) {
            AbstractAction.setToolTipTextFromAction(this, action);
        }
    }

    private void setActionCommandFromAction(Action action) {
        setActionCommand((action == null) ? null :
                         (String)action.getValue(Action.ACTION_COMMAND_KEY));
    }

    /**
     * Creates and returns a <code>PropertyChangeListener</code> that is
     * responsible for listening for changes from the specified
     * <code>Action</code> and updating the appropriate properties.
     * <p>
     * <b>Warning:</b> If you subclass this do not create an anonymous
     * inner class.  If you do the lifetime of the textfield will be tied to
     * that of the <code>Action</code>.
     *
     * @param a the textfield's action
     * @return a {@code PropertyChangeListener} that is responsible for
     *         listening for changes from the specified {@code Action} and
     *         updating the appropriate properties
     * @since 1.3
     * @see Action
     * @see #setAction
     */
    protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
        return new TextFieldActionPropertyChangeListener(this, a);
    }

    private static class TextFieldActionPropertyChangeListener extends
                         ActionPropertyChangeListener<JTextField> {
        TextFieldActionPropertyChangeListener(JTextField tf, Action a) {
            super(tf, a);
        }

        protected void actionPropertyChanged(JTextField textField,
                                             Action action,
                                             PropertyChangeEvent e) {
            if (AbstractAction.shouldReconfigure(e)) {
                textField.configurePropertiesFromAction(action);
            } else {
                textField.actionPropertyChanged(action, e.getPropertyName());
            }
        }
    }

    /**
     * Fetches the command list for the editor.  This is
     * the list of commands supported by the plugged-in UI
     * augmented by the collection of commands that the
     * editor itself supports.  These are useful for binding
     * to events, such as in a keymap.
     *
     * @return the command list
     */
    @BeanProperty(bound = false)
    public Action[] getActions() {
        return TextAction.augmentList(super.getActions(), defaultActions);
    }

    /**
     * Processes action events occurring on this textfield by
     * dispatching them to any registered <code>ActionListener</code> objects.
     * This is normally called by the controller registered with
     * textfield.
     */
    public void postActionEvent() {
        fireActionPerformed();
    }

    // --- Scrolling support -----------------------------------

    /**
     * Gets the visibility of the text field.  This can
     * be adjusted to change the location of the visible
     * area if the size of the field is greater than
     * the area that was allocated to the field.
     *
     * <p>
     * The fields look-and-feel implementation manages
     * the values of the minimum, maximum, and extent
     * properties on the <code>BoundedRangeModel</code>.
     *
     * @return the visibility
     * @see BoundedRangeModel
     */
    @BeanProperty(bound = false)
    public BoundedRangeModel getHorizontalVisibility() {
        return visibility;
    }

    /**
     * Gets the scroll offset, in pixels.
     *
     * @return the offset &gt;= 0
     */
    public int getScrollOffset() {
        return visibility.getValue();
    }

    /**
     * Sets the scroll offset, in pixels.
     *
     * @param scrollOffset the offset &gt;= 0
     */
    public void setScrollOffset(int scrollOffset) {
        visibility.setValue(scrollOffset);
    }

    /**
     * Scrolls the field left or right.
     *
     * @param r the region to scroll
     */
    public void scrollRectToVisible(Rectangle r) {
        // convert to coordinate system of the bounded range
        Insets i = getInsets();
        int x0 = r.x + visibility.getValue() - i.left;
        int x1 = x0 + r.width;
        if (x0 < visibility.getValue()) {
            // Scroll to the left
            visibility.setValue(x0);
        } else if(x1 > visibility.getValue() + visibility.getExtent()) {
            // Scroll to the right
            visibility.setValue(x1 - visibility.getExtent());
        }
    }

    /**
     * Returns true if the receiver has an <code>ActionListener</code>
     * installed.
     */
    boolean hasActionListener() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ActionListener.class) {
                return true;
            }
        }
        return false;
    }

    // --- variables -------------------------------------------

    /**
     * Name of the action to send notification that the
     * contents of the field have been accepted.  Typically
     * this is bound to a carriage-return.
     */
    public static final String notifyAction = "notify-field-accept";

    private BoundedRangeModel visibility;
    private int horizontalAlignment = LEADING;
    private int columns;
    private int columnWidth;
    private String command;

    private static final Action[] defaultActions = {
        new NotifyAction()
    };

    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "TextFieldUI";

    // --- Action implementations -----------------------------------

    // Note that JFormattedTextField.CommitAction extends this
    static class NotifyAction extends TextAction {

        NotifyAction() {
            super(notifyAction);
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getFocusedComponent();
            if (target instanceof JTextField) {
                JTextField field = (JTextField) target;
                field.postActionEvent();
            }
        }

        public boolean isEnabled() {
            JTextComponent target = getFocusedComponent();
            if (target instanceof JTextField) {
                return ((JTextField)target).hasActionListener();
            }
            return false;
        }
    }

    class ScrollRepainter implements ChangeListener, Serializable {

        public void stateChanged(ChangeEvent e) {
            repaint();
        }

    }


    /**
     * See <code>readObject</code> and <code>writeObject</code> in
     * <code>JComponent</code> for more
     * information about serialization in Swing.
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        if (getUIClassID().equals(uiClassID)) {
            byte count = JComponent.getWriteObjCounter(this);
            JComponent.setWriteObjCounter(this, --count);
            if (count == 0 && ui != null) {
                ui.installUI(this);
            }
        }
    }


    /**
     * Returns a string representation of this <code>JTextField</code>.
     * This method is intended to be used only for debugging purposes,
     * and the content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this <code>JTextField</code>
     */
    protected String paramString() {
        String horizontalAlignmentString;
        if (horizontalAlignment == LEFT) {
            horizontalAlignmentString = "LEFT";
        } else if (horizontalAlignment == CENTER) {
            horizontalAlignmentString = "CENTER";
        } else if (horizontalAlignment == RIGHT) {
            horizontalAlignmentString = "RIGHT";
        } else if (horizontalAlignment == LEADING) {
            horizontalAlignmentString = "LEADING";
        } else if (horizontalAlignment == TRAILING) {
            horizontalAlignmentString = "TRAILING";
        } else horizontalAlignmentString = "";
        String commandString = (command != null ?
                                command : "");

        return super.paramString() +
        ",columns=" + columns +
        ",columnWidth=" + columnWidth +
        ",command=" + commandString +
        ",horizontalAlignment=" + horizontalAlignmentString;
    }


/////////////////
// Accessibility support
////////////////


    /**
     * Gets the <code>AccessibleContext</code> associated with this
     * <code>JTextField</code>. For <code>JTextFields</code>,
     * the <code>AccessibleContext</code> takes the form of an
     * <code>AccessibleJTextField</code>.
     * A new <code>AccessibleJTextField</code> instance is created
     * if necessary.
     *
     * @return an <code>AccessibleJTextField</code> that serves as the
     *         <code>AccessibleContext</code> of this <code>JTextField</code>
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJTextField();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JTextField</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to text field user-interface
     * elements.
     * <p>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.  As of 1.4, support for long term storage
     * of all JavaBeans
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    @SuppressWarnings("serial") // Same-version serialization only
    protected class AccessibleJTextField extends AccessibleJTextComponent {

        /**
         * Constructs an {@code AccessibleJTextField}.
         */
        protected AccessibleJTextField() {}

        /**
         * Gets the state set of this object.
         *
         * @return an instance of AccessibleStateSet describing the states
         * of the object
         * @see AccessibleState
         */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states = super.getAccessibleStateSet();
            states.add(AccessibleState.SINGLE_LINE);
            return states;
        }
    }
}
