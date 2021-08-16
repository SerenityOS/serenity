/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.spi.DateFormatProvider;
import java.text.spi.NumberFormatProvider;
import java.util.Date;
import java.util.Locale;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleEditableText;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleText;
import javax.accessibility.AccessibleValue;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.SpinnerUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.DocumentFilter;
import javax.swing.text.NumberFormatter;

import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleResources;

/**
 * A single line input field that lets the user select a
 * number or an object value from an ordered sequence. Spinners typically
 * provide a pair of tiny arrow buttons for stepping through the elements
 * of the sequence. The keyboard up/down arrow keys also cycle through the
 * elements. The user may also be allowed to type a (legal) value directly
 * into the spinner. Although combo boxes provide similar functionality,
 * spinners are sometimes preferred because they don't require a drop down list
 * that can obscure important data.
 * <p>
 * A <code>JSpinner</code>'s sequence value is defined by its
 * <code>SpinnerModel</code>.
 * The <code>model</code> can be specified as a constructor argument and
 * changed with the <code>model</code> property.  <code>SpinnerModel</code>
 * classes for some common types are provided: <code>SpinnerListModel</code>,
 * <code>SpinnerNumberModel</code>, and <code>SpinnerDateModel</code>.
 * <p>
 * A <code>JSpinner</code> has a single child component that's
 * responsible for displaying
 * and potentially changing the current element or <i>value</i> of
 * the model, which is called the <code>editor</code>.  The editor is created
 * by the <code>JSpinner</code>'s constructor and can be changed with the
 * <code>editor</code> property.  The <code>JSpinner</code>'s editor stays
 * in sync with the model by listening for <code>ChangeEvent</code>s. If the
 * user has changed the value displayed by the <code>editor</code> it is
 * possible for the <code>model</code>'s value to differ from that of
 * the <code>editor</code>. To make sure the <code>model</code> has the same
 * value as the editor use the <code>commitEdit</code> method, eg:
 * <pre>
 *   try {
 *       spinner.commitEdit();
 *   }
 *   catch (ParseException pe) {
 *       // Edited value is invalid, spinner.getValue() will return
 *       // the last valid value, you could revert the spinner to show that:
 *       JComponent editor = spinner.getEditor();
 *       if (editor instanceof DefaultEditor) {
 *           ((DefaultEditor)editor).getTextField().setValue(spinner.getValue());
 *       }
 *       // reset the value to some known value:
 *       spinner.setValue(fallbackValue);
 *       // or treat the last valid value as the current, in which
 *       // case you don't need to do anything.
 *   }
 *   return spinner.getValue();
 * </pre>
 * <p>
 * For information and examples of using spinner see
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/spinner.html">How to Use Spinners</a>,
 * a section in <em>The Java Tutorial.</em>
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
 * @see SpinnerModel
 * @see AbstractSpinnerModel
 * @see SpinnerListModel
 * @see SpinnerNumberModel
 * @see SpinnerDateModel
 * @see JFormattedTextField
 *
 * @author Hans Muller
 * @author Lynn Monsanto (accessibility)
 * @since 1.4
 */
@JavaBean(defaultProperty = "UI", description = "A single line input field that lets the user select a number or an object value from an ordered set.")
@SwingContainer(false)
@SuppressWarnings("serial") // Same-version serialization only
public class JSpinner extends JComponent implements Accessible
{
    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "SpinnerUI";

    private static final Action DISABLED_ACTION = new DisabledAction();

    private SpinnerModel model;
    private JComponent editor;
    private ChangeListener modelListener;
    private transient ChangeEvent changeEvent;
    private boolean editorExplicitlySet = false;


    /**
     * Constructs a spinner for the given model. The spinner has
     * a set of previous/next buttons, and an editor appropriate
     * for the model.
     *
     * @param model  a model for the new spinner
     * @throws NullPointerException if the model is {@code null}
     */
    public JSpinner(SpinnerModel model) {
        if (model == null) {
            throw new NullPointerException("model cannot be null");
        }
        this.model = model;
        this.editor = createEditor(model);
        setUIProperty("opaque",true);
        updateUI();
    }


    /**
     * Constructs a spinner with an <code>Integer SpinnerNumberModel</code>
     * with initial value 0 and no minimum or maximum limits.
     */
    public JSpinner() {
        this(new SpinnerNumberModel());
    }


    /**
     * Returns the look and feel (L&amp;F) object that renders this component.
     *
     * @return the <code>SpinnerUI</code> object that renders this component
     */
    public SpinnerUI getUI() {
        return (SpinnerUI)ui;
    }


    /**
     * Sets the look and feel (L&amp;F) object that renders this component.
     *
     * @param ui  the <code>SpinnerUI</code> L&amp;F object
     * @see UIDefaults#getUI
     */
    public void setUI(SpinnerUI ui) {
        super.setUI(ui);
    }


    /**
     * Returns the suffix used to construct the name of the look and feel
     * (L&amp;F) class used to render this component.
     *
     * @return the string "SpinnerUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false)
    public String getUIClassID() {
        return uiClassID;
    }



    /**
     * Resets the UI property with the value from the current look and feel.
     *
     * @see UIManager#getUI
     */
    public void updateUI() {
        setUI((SpinnerUI)UIManager.getUI(this));
        invalidate();
    }


    /**
     * This method is called by the constructors to create the
     * <code>JComponent</code>
     * that displays the current value of the sequence.  The editor may
     * also allow the user to enter an element of the sequence directly.
     * An editor must listen for <code>ChangeEvents</code> on the
     * <code>model</code> and keep the value it displays
     * in sync with the value of the model.
     * <p>
     * Subclasses may override this method to add support for new
     * <code>SpinnerModel</code> classes.  Alternatively one can just
     * replace the editor created here with the <code>setEditor</code>
     * method.  The default mapping from model type to editor is:
     * <ul>
     * <li> <code>SpinnerNumberModel =&gt; JSpinner.NumberEditor</code>
     * <li> <code>SpinnerDateModel =&gt; JSpinner.DateEditor</code>
     * <li> <code>SpinnerListModel =&gt; JSpinner.ListEditor</code>
     * <li> <i>all others</i> =&gt; <code>JSpinner.DefaultEditor</code>
     * </ul>
     *
     * @return a component that displays the current value of the sequence
     * @param model the value of getModel
     * @see #getModel
     * @see #setEditor
     */
    protected JComponent createEditor(SpinnerModel model) {
        if (model instanceof SpinnerDateModel) {
            return new DateEditor(this);
        }
        else if (model instanceof SpinnerListModel) {
            return new ListEditor(this);
        }
        else if (model instanceof SpinnerNumberModel) {
            return new NumberEditor(this);
        }
        else {
            return new DefaultEditor(this);
        }
    }


    /**
     * Changes the model that represents the value of this spinner.
     * If the editor property has not been explicitly set,
     * the editor property is (implicitly) set after the <code>"model"</code>
     * <code>PropertyChangeEvent</code> has been fired.  The editor
     * property is set to the value returned by <code>createEditor</code>,
     * as in:
     * <pre>
     * setEditor(createEditor(model));
     * </pre>
     *
     * @param model the new <code>SpinnerModel</code>
     * @see #getModel
     * @see #getEditor
     * @see #setEditor
     * @throws IllegalArgumentException if model is <code>null</code>
     */
    @BeanProperty(visualUpdate = true, description
            = "Model that represents the value of this spinner.")
    public void setModel(SpinnerModel model) {
        if (model == null) {
            throw new IllegalArgumentException("null model");
        }
        if (!model.equals(this.model)) {
            SpinnerModel oldModel = this.model;
            this.model = model;
            if (modelListener != null) {
                oldModel.removeChangeListener(modelListener);
                this.model.addChangeListener(modelListener);
            }
            firePropertyChange("model", oldModel, model);
            if (!editorExplicitlySet) {
                setEditor(createEditor(model)); // sets editorExplicitlySet true
                editorExplicitlySet = false;
            }
            repaint();
            revalidate();
        }
    }


    /**
     * Returns the <code>SpinnerModel</code> that defines
     * this spinners sequence of values.
     *
     * @return the value of the model property
     * @see #setModel
     */
    public SpinnerModel getModel() {
        return model;
    }


    /**
     * Returns the current value of the model, typically
     * this value is displayed by the <code>editor</code>. If the
     * user has changed the value displayed by the <code>editor</code> it is
     * possible for the <code>model</code>'s value to differ from that of
     * the <code>editor</code>, refer to the class level javadoc for examples
     * of how to deal with this.
     * <p>
     * This method simply delegates to the <code>model</code>.
     * It is equivalent to:
     * <pre>
     * getModel().getValue()
     * </pre>
     *
     * @return the current value of the model
     * @see #setValue
     * @see SpinnerModel#getValue
     */
    public Object getValue() {
        return getModel().getValue();
    }


    /**
     * Changes current value of the model, typically
     * this value is displayed by the <code>editor</code>.
     * If the <code>SpinnerModel</code> implementation
     * doesn't support the specified value then an
     * <code>IllegalArgumentException</code> is thrown.
     * <p>
     * This method simply delegates to the <code>model</code>.
     * It is equivalent to:
     * <pre>
     * getModel().setValue(value)
     * </pre>
     *
     * @param value  new value for the spinner
     * @throws IllegalArgumentException if <code>value</code> isn't allowed
     * @see #getValue
     * @see SpinnerModel#setValue
     */
    public void setValue(Object value) {
        getModel().setValue(value);
    }


    /**
     * Returns the object in the sequence that comes after the object returned
     * by <code>getValue()</code>. If the end of the sequence has been reached
     * then return <code>null</code>.
     * Calling this method does not effect <code>value</code>.
     * <p>
     * This method simply delegates to the <code>model</code>.
     * It is equivalent to:
     * <pre>
     * getModel().getNextValue()
     * </pre>
     *
     * @return the next legal value or <code>null</code> if one doesn't exist
     * @see #getValue
     * @see #getPreviousValue
     * @see SpinnerModel#getNextValue
     */
    @BeanProperty(bound = false)
    public Object getNextValue() {
        return getModel().getNextValue();
    }


    /**
     * We pass <code>Change</code> events along to the listeners with the
     * the slider (instead of the model itself) as the event source.
     */
    private class ModelListener implements ChangeListener, Serializable {
        public void stateChanged(ChangeEvent e) {
            fireStateChanged();
        }
    }


    /**
     * Adds a listener to the list that is notified each time a change
     * to the model occurs.  The source of <code>ChangeEvents</code>
     * delivered to <code>ChangeListeners</code> will be this
     * <code>JSpinner</code>.  Note also that replacing the model
     * will not affect listeners added directly to JSpinner.
     * Applications can add listeners to  the model directly.  In that
     * case is that the source of the event would be the
     * <code>SpinnerModel</code>.
     *
     * @param listener the <code>ChangeListener</code> to add
     * @see #removeChangeListener
     * @see #getModel
     */
    public void addChangeListener(ChangeListener listener) {
        if (modelListener == null) {
            modelListener = new ModelListener();
            getModel().addChangeListener(modelListener);
        }
        listenerList.add(ChangeListener.class, listener);
    }



    /**
     * Removes a <code>ChangeListener</code> from this spinner.
     *
     * @param listener the <code>ChangeListener</code> to remove
     * @see #fireStateChanged
     * @see #addChangeListener
     */
    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }


    /**
     * Returns an array of all the <code>ChangeListener</code>s added
     * to this JSpinner with addChangeListener().
     *
     * @return all of the <code>ChangeListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    @BeanProperty(bound = false)
    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }


    /**
     * Sends a <code>ChangeEvent</code>, whose source is this
     * <code>JSpinner</code>, to each <code>ChangeListener</code>.
     * When a <code>ChangeListener</code> has been added
     * to the spinner, this method is called each time
     * a <code>ChangeEvent</code> is received from the model.
     *
     * @see #addChangeListener
     * @see #removeChangeListener
     * @see EventListenerList
     */
    protected void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener)listeners[i+1]).stateChanged(changeEvent);
            }
        }
    }


    /**
     * Returns the object in the sequence that comes
     * before the object returned by <code>getValue()</code>.
     * If the end of the sequence has been reached then
     * return <code>null</code>. Calling this method does
     * not effect <code>value</code>.
     * <p>
     * This method simply delegates to the <code>model</code>.
     * It is equivalent to:
     * <pre>
     * getModel().getPreviousValue()
     * </pre>
     *
     * @return the previous legal value or <code>null</code>
     *   if one doesn't exist
     * @see #getValue
     * @see #getNextValue
     * @see SpinnerModel#getPreviousValue
     */
    @BeanProperty(bound = false)
    public Object getPreviousValue() {
        return getModel().getPreviousValue();
    }


    /**
     * Changes the <code>JComponent</code> that displays the current value
     * of the <code>SpinnerModel</code>.  It is the responsibility of this
     * method to <i>disconnect</i> the old editor from the model and to
     * connect the new editor.  This may mean removing the
     * old editors <code>ChangeListener</code> from the model or the
     * spinner itself and adding one for the new editor.
     *
     * @param editor the new editor
     * @see #getEditor
     * @see #createEditor
     * @see #getModel
     * @throws IllegalArgumentException if editor is <code>null</code>
     */
    @BeanProperty(visualUpdate = true, description
            = "JComponent that displays the current value of the model")
    public void setEditor(JComponent editor) {
        if (editor == null) {
            throw new IllegalArgumentException("null editor");
        }
        if (!editor.equals(this.editor)) {
            JComponent oldEditor = this.editor;
            this.editor = editor;
            if (oldEditor instanceof DefaultEditor) {
                ((DefaultEditor)oldEditor).dismiss(this);
            }
            editorExplicitlySet = true;
            firePropertyChange("editor", oldEditor, editor);
            revalidate();
            repaint();
        }
    }


    /**
     * Returns the component that displays and potentially
     * changes the model's value.
     *
     * @return the component that displays and potentially
     *    changes the model's value
     * @see #setEditor
     * @see #createEditor
     */
    public JComponent getEditor() {
        return editor;
    }


    /**
     * Commits the currently edited value to the <code>SpinnerModel</code>.
     * <p>
     * If the editor is an instance of <code>DefaultEditor</code>, the
     * call if forwarded to the editor, otherwise this does nothing.
     *
     * @throws ParseException if the currently edited value couldn't
     *         be committed.
     */
    public void commitEdit() throws ParseException {
        JComponent editor = getEditor();
        if (editor instanceof DefaultEditor) {
            ((DefaultEditor)editor).commitEdit();
        }
    }


    /*
     * See readObject and writeObject in JComponent for more
     * information about serialization in Swing.
     *
     * @param s Stream to write to
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
     * A simple base class for more specialized editors
     * that displays a read-only view of the model's current
     * value with a <code>JFormattedTextField</code>.  Subclasses
     * can configure the <code>JFormattedTextField</code> to create
     * an editor that's appropriate for the type of model they
     * support and they may want to override
     * the <code>stateChanged</code> and <code>propertyChanged</code>
     * methods, which keep the model and the text field in sync.
     * <p>
     * This class defines a <code>dismiss</code> method that removes the
     * editors <code>ChangeListener</code> from the <code>JSpinner</code>
     * that it's part of.   The <code>setEditor</code> method knows about
     * <code>DefaultEditor.dismiss</code>, so if the developer
     * replaces an editor that's derived from <code>JSpinner.DefaultEditor</code>
     * its <code>ChangeListener</code> connection back to the
     * <code>JSpinner</code> will be removed.  However after that,
     * it's up to the developer to manage their editor listeners.
     * Similarly, if a subclass overrides <code>createEditor</code>,
     * it's up to the subclasser to deal with their editor
     * subsequently being replaced (with <code>setEditor</code>).
     * We expect that in most cases, and in editor installed
     * with <code>setEditor</code> or created by a <code>createEditor</code>
     * override, will not be replaced anyway.
     * <p>
     * This class is the <code>LayoutManager</code> for it's single
     * <code>JFormattedTextField</code> child.   By default the
     * child is just centered with the parents insets.
     * @since 1.4
     */
    public static class DefaultEditor extends JPanel
        implements ChangeListener, PropertyChangeListener, LayoutManager
    {
        /**
         * Constructs an editor component for the specified <code>JSpinner</code>.
         * This <code>DefaultEditor</code> is it's own layout manager and
         * it is added to the spinner's <code>ChangeListener</code> list.
         * The constructor creates a single <code>JFormattedTextField</code> child,
         * initializes it's value to be the spinner model's current value
         * and adds it to <code>this</code> <code>DefaultEditor</code>.
         *
         * @param spinner the spinner whose model <code>this</code> editor will monitor
         * @see #getTextField
         * @see JSpinner#addChangeListener
         */
        public DefaultEditor(JSpinner spinner) {
            super(null);

            JFormattedTextField ftf = new JFormattedTextField();
            ftf.setName("Spinner.formattedTextField");
            ftf.setValue(spinner.getValue());
            ftf.addPropertyChangeListener(this);
            ftf.setEditable(false);
            ftf.setInheritsPopupMenu(true);

            String toolTipText = spinner.getToolTipText();
            if (toolTipText != null) {
                ftf.setToolTipText(toolTipText);
            }

            add(ftf);

            setLayout(this);
            spinner.addChangeListener(this);

            // We want the spinner's increment/decrement actions to be
            // active vs those of the JFormattedTextField. As such we
            // put disabled actions in the JFormattedTextField's actionmap.
            // A binding to a disabled action is treated as a nonexistant
            // binding.
            ActionMap ftfMap = ftf.getActionMap();

            if (ftfMap != null) {
                ftfMap.put("increment", DISABLED_ACTION);
                ftfMap.put("decrement", DISABLED_ACTION);
            }
        }


        /**
         * Disconnect <code>this</code> editor from the specified
         * <code>JSpinner</code>.  By default, this method removes
         * itself from the spinners <code>ChangeListener</code> list.
         *
         * @param spinner the <code>JSpinner</code> to disconnect this
         *    editor from; the same spinner as was passed to the constructor.
         */
        public void dismiss(JSpinner spinner) {
            spinner.removeChangeListener(this);
        }


        /**
         * Returns the <code>JSpinner</code> ancestor of this editor or
         * <code>null</code> if none of the ancestors are a
         * <code>JSpinner</code>.
         * Typically the editor's parent is a <code>JSpinner</code> however
         * subclasses of <code>JSpinner</code> may override the
         * the <code>createEditor</code> method and insert one or more containers
         * between the <code>JSpinner</code> and it's editor.
         *
         * @return <code>JSpinner</code> ancestor; <code>null</code>
         *         if none of the ancestors are a <code>JSpinner</code>
         *
         * @see JSpinner#createEditor
         */
        public JSpinner getSpinner() {
            for (Component c = this; c != null; c = c.getParent()) {
                if (c instanceof JSpinner) {
                    return (JSpinner)c;
                }
            }
            return null;
        }


        /**
         * Returns the <code>JFormattedTextField</code> child of this
         * editor.  By default the text field is the first and only
         * child of editor.
         *
         * @return the <code>JFormattedTextField</code> that gives the user
         *     access to the <code>SpinnerDateModel's</code> value.
         * @see #getSpinner
         * @see #getModel
         */
        public JFormattedTextField getTextField() {
            return (JFormattedTextField)getComponent(0);
        }


        /**
         * This method is called when the spinner's model's state changes.
         * It sets the <code>value</code> of the text field to the current
         * value of the spinners model.
         *
         * @param e the <code>ChangeEvent</code> whose source is the
         * <code>JSpinner</code> whose model has changed.
         * @see #getTextField
         * @see JSpinner#getValue
         */
        public void stateChanged(ChangeEvent e) {
            JSpinner spinner = (JSpinner)(e.getSource());
            getTextField().setValue(spinner.getValue());
        }


        /**
         * Called by the <code>JFormattedTextField</code>
         * <code>PropertyChangeListener</code>.  When the <code>"value"</code>
         * property changes, which implies that the user has typed a new
         * number, we set the value of the spinners model.
         * <p>
         * This class ignores <code>PropertyChangeEvents</code> whose
         * source is not the <code>JFormattedTextField</code>, so subclasses
         * may safely make <code>this</code> <code>DefaultEditor</code> a
         * <code>PropertyChangeListener</code> on other objects.
         *
         * @param e the <code>PropertyChangeEvent</code> whose source is
         *    the <code>JFormattedTextField</code> created by this class.
         * @see #getTextField
         */
        public void propertyChange(PropertyChangeEvent e)
        {
            JSpinner spinner = getSpinner();

            if (spinner == null) {
                // Indicates we aren't installed anywhere.
                return;
            }

            Object source = e.getSource();
            String name = e.getPropertyName();
            if (source instanceof JFormattedTextField) {
                if ("value".equals(name)) {
                    Object lastValue = spinner.getValue();

                    // Try to set the new value
                    try {
                        spinner.setValue(getTextField().getValue());
                    } catch (IllegalArgumentException iae) {
                        // SpinnerModel didn't like new value, reset
                        try {
                            ((JFormattedTextField)source).setValue(lastValue);
                        } catch (IllegalArgumentException iae2) {
                            // Still bogus, nothing else we can do, the
                            // SpinnerModel and JFormattedTextField are now out
                            // of sync.
                        }
                    }
                } else if ("font".equals(name)) {
                    Object newfont = e.getNewValue();
                    if (newfont instanceof UIResource) {
                        // The text field font must match the JSpinner font if
                        // the text field font was not set by the user
                        Font font = spinner.getFont();
                        if (!newfont.equals(font)) {
                            getTextField().setFont(font == null ? null : new FontUIResource(font));
                        }
                    }
                }
            }
        }


        /**
         * This <code>LayoutManager</code> method does nothing.  We're
         * only managing a single child and there's no support
         * for layout constraints.
         *
         * @param name ignored
         * @param child ignored
         */
        public void addLayoutComponent(String name, Component child) {
        }


        /**
         * This <code>LayoutManager</code> method does nothing.  There
         * isn't any per-child state.
         *
         * @param child ignored
         */
        public void removeLayoutComponent(Component child) {
        }


        /**
         * Returns the size of the parents insets.
         */
        private Dimension insetSize(Container parent) {
            Insets insets = parent.getInsets();
            int w = insets.left + insets.right;
            int h = insets.top + insets.bottom;
            return new Dimension(w, h);
        }


        /**
         * Returns the preferred size of first (and only) child plus the
         * size of the parents insets.
         *
         * @param parent the Container that's managing the layout
         * @return the preferred dimensions to lay out the subcomponents
         *          of the specified container.
         */
        public Dimension preferredLayoutSize(Container parent) {
            Dimension preferredSize = insetSize(parent);
            if (parent.getComponentCount() > 0) {
                Dimension childSize = getComponent(0).getPreferredSize();
                preferredSize.width += childSize.width;
                preferredSize.height += childSize.height;
            }
            return preferredSize;
        }


        /**
         * Returns the minimum size of first (and only) child plus the
         * size of the parents insets.
         *
         * @param parent the Container that's managing the layout
         * @return  the minimum dimensions needed to lay out the subcomponents
         *          of the specified container.
         */
        public Dimension minimumLayoutSize(Container parent) {
            Dimension minimumSize = insetSize(parent);
            if (parent.getComponentCount() > 0) {
                Dimension childSize = getComponent(0).getMinimumSize();
                minimumSize.width += childSize.width;
                minimumSize.height += childSize.height;
            }
            return minimumSize;
        }


        /**
         * Resize the one (and only) child to completely fill the area
         * within the parents insets.
         */
        public void layoutContainer(Container parent) {
            if (parent.getComponentCount() > 0) {
                Insets insets = parent.getInsets();
                int w = parent.getWidth() - (insets.left + insets.right);
                int h = parent.getHeight() - (insets.top + insets.bottom);
                getComponent(0).setBounds(insets.left, insets.top, w, h);
            }
        }

        /**
         * Pushes the currently edited value to the <code>SpinnerModel</code>.
         * <p>
         * The default implementation invokes <code>commitEdit</code> on the
         * <code>JFormattedTextField</code>.
         *
         * @throws ParseException if the edited value is not legal
         */
        public void commitEdit()  throws ParseException {
            // If the value in the JFormattedTextField is legal, this will have
            // the result of pushing the value to the SpinnerModel
            // by way of the <code>propertyChange</code> method.
            JFormattedTextField ftf = getTextField();

            ftf.commitEdit();
        }

        /**
         * Returns the baseline.
         *
         * @throws IllegalArgumentException {@inheritDoc}
         * @see javax.swing.JComponent#getBaseline(int,int)
         * @see javax.swing.JComponent#getBaselineResizeBehavior()
         * @since 1.6
         */
        public int getBaseline(int width, int height) {
            // check size.
            super.getBaseline(width, height);
            Insets insets = getInsets();
            width = width - insets.left - insets.right;
            height = height - insets.top - insets.bottom;
            int baseline = getComponent(0).getBaseline(width, height);
            if (baseline >= 0) {
                return baseline + insets.top;
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
        public BaselineResizeBehavior getBaselineResizeBehavior() {
            return getComponent(0).getBaselineResizeBehavior();
        }
    }




    /**
     * This subclass of javax.swing.DateFormatter maps the minimum/maximum
     * properties to the start/end properties of a SpinnerDateModel.
     */
    private static class DateEditorFormatter extends DateFormatter {
        private final SpinnerDateModel model;

        DateEditorFormatter(SpinnerDateModel model, DateFormat format) {
            super(format);
            this.model = model;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMinimum(Comparable<?> min) {
            model.setStart((Comparable<Date>)min);
        }

        @Override
        public Comparable<Date> getMinimum() {
            return  model.getStart();
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMaximum(Comparable<?> max) {
            model.setEnd((Comparable<Date>)max);
        }

        @Override
        public Comparable<Date> getMaximum() {
            return model.getEnd();
        }
    }


    /**
     * An editor for a <code>JSpinner</code> whose model is a
     * <code>SpinnerDateModel</code>.  The value of the editor is
     * displayed with a <code>JFormattedTextField</code> whose format
     * is defined by a <code>DateFormatter</code> instance whose
     * <code>minimum</code> and <code>maximum</code> properties
     * are mapped to the <code>SpinnerDateModel</code>.
     * @since 1.4
     */
    // PENDING(hmuller): more example javadoc
    public static class DateEditor extends DefaultEditor
    {
        // This is here until SimpleDateFormat gets a constructor that
        // takes a Locale: 4923525
        private static String getDefaultPattern(Locale loc) {
            LocaleProviderAdapter adapter = LocaleProviderAdapter.getAdapter(DateFormatProvider.class, loc);
            LocaleResources lr = adapter.getLocaleResources(loc);
            if (lr == null) {
                lr = LocaleProviderAdapter.forJRE().getLocaleResources(loc);
            }
            return lr.getDateTimePattern(DateFormat.SHORT, DateFormat.SHORT, null);
        }

        /**
         * Construct a <code>JSpinner</code> editor that supports displaying
         * and editing the value of a <code>SpinnerDateModel</code>
         * with a <code>JFormattedTextField</code>.  <code>This</code>
         * <code>DateEditor</code> becomes both a <code>ChangeListener</code>
         * on the spinner and a <code>PropertyChangeListener</code>
         * on the new <code>JFormattedTextField</code>.
         *
         * @param spinner the spinner whose model <code>this</code> editor will monitor
         * @exception IllegalArgumentException if the spinners model is not
         *     an instance of <code>SpinnerDateModel</code>
         *
         * @see #getModel
         * @see #getFormat
         * @see SpinnerDateModel
         */
        public DateEditor(JSpinner spinner) {
            this(spinner, getDefaultPattern(spinner.getLocale()));
        }


        /**
         * Construct a <code>JSpinner</code> editor that supports displaying
         * and editing the value of a <code>SpinnerDateModel</code>
         * with a <code>JFormattedTextField</code>.  <code>This</code>
         * <code>DateEditor</code> becomes both a <code>ChangeListener</code>
         * on the spinner and a <code>PropertyChangeListener</code>
         * on the new <code>JFormattedTextField</code>.
         *
         * @param spinner the spinner whose model <code>this</code> editor will monitor
         * @param dateFormatPattern the initial pattern for the
         *     <code>SimpleDateFormat</code> object that's used to display
         *     and parse the value of the text field.
         * @exception IllegalArgumentException if the spinners model is not
         *     an instance of <code>SpinnerDateModel</code>
         *
         * @see #getModel
         * @see #getFormat
         * @see SpinnerDateModel
         * @see java.text.SimpleDateFormat
         */
        public DateEditor(JSpinner spinner, String dateFormatPattern) {
            this(spinner, new SimpleDateFormat(dateFormatPattern,
                                               spinner.getLocale()));
        }

        /**
         * Construct a <code>JSpinner</code> editor that supports displaying
         * and editing the value of a <code>SpinnerDateModel</code>
         * with a <code>JFormattedTextField</code>.  <code>This</code>
         * <code>DateEditor</code> becomes both a <code>ChangeListener</code>
         * on the spinner and a <code>PropertyChangeListener</code>
         * on the new <code>JFormattedTextField</code>.
         *
         * @param spinner the spinner whose model <code>this</code> editor
         *        will monitor
         * @param format <code>DateFormat</code> object that's used to display
         *     and parse the value of the text field.
         * @exception IllegalArgumentException if the spinners model is not
         *     an instance of <code>SpinnerDateModel</code>
         *
         * @see #getModel
         * @see #getFormat
         * @see SpinnerDateModel
         * @see java.text.SimpleDateFormat
         */
        private DateEditor(JSpinner spinner, DateFormat format) {
            super(spinner);
            if (!(spinner.getModel() instanceof SpinnerDateModel)) {
                throw new IllegalArgumentException(
                                 "model not a SpinnerDateModel");
            }

            SpinnerDateModel model = (SpinnerDateModel)spinner.getModel();
            DateFormatter formatter = new DateEditorFormatter(model, format);
            DefaultFormatterFactory factory = new DefaultFormatterFactory(
                                                  formatter);
            JFormattedTextField ftf = getTextField();
            ftf.setEditable(true);
            ftf.setFormatterFactory(factory);

            /* TBD - initializing the column width of the text field
             * is imprecise and doing it here is tricky because
             * the developer may configure the formatter later.
             */
            try {
                String maxString = formatter.valueToString(model.getStart());
                String minString = formatter.valueToString(model.getEnd());
                ftf.setColumns(Math.max(maxString.length(),
                                        minString.length()));
            }
            catch (ParseException e) {
                // PENDING: hmuller
            }
        }

        /**
         * Returns the <code>java.text.SimpleDateFormat</code> object the
         * <code>JFormattedTextField</code> uses to parse and format
         * numbers.
         *
         * @return the value of <code>getTextField().getFormatter().getFormat()</code>.
         * @see #getTextField
         * @see java.text.SimpleDateFormat
         */
        public SimpleDateFormat getFormat() {
            return (SimpleDateFormat)((DateFormatter)(getTextField().getFormatter())).getFormat();
        }


        /**
         * Return our spinner ancestor's <code>SpinnerDateModel</code>.
         *
         * @return <code>getSpinner().getModel()</code>
         * @see #getSpinner
         * @see #getTextField
         */
        public SpinnerDateModel getModel() {
            return (SpinnerDateModel)(getSpinner().getModel());
        }
    }


    /**
     * This subclass of javax.swing.NumberFormatter maps the minimum/maximum
     * properties to a SpinnerNumberModel and initializes the valueClass
     * of the NumberFormatter to match the type of the initial models value.
     */
    private static class NumberEditorFormatter extends NumberFormatter {
        private final SpinnerNumberModel model;

        NumberEditorFormatter(SpinnerNumberModel model, NumberFormat format) {
            super(format);
            this.model = model;
            setValueClass(model.getValue().getClass());
        }

        @Override
        public void setMinimum(Comparable<?> min) {
            model.setMinimum(min);
        }

        @Override
        public Comparable<?> getMinimum() {
            return  model.getMinimum();
        }

        @Override
        public void setMaximum(Comparable<?> max) {
            model.setMaximum(max);
        }

        @Override
        public Comparable<?> getMaximum() {
            return model.getMaximum();
        }
    }



    /**
     * An editor for a <code>JSpinner</code> whose model is a
     * <code>SpinnerNumberModel</code>.  The value of the editor is
     * displayed with a <code>JFormattedTextField</code> whose format
     * is defined by a <code>NumberFormatter</code> instance whose
     * <code>minimum</code> and <code>maximum</code> properties
     * are mapped to the <code>SpinnerNumberModel</code>.
     * @since 1.4
     */
    // PENDING(hmuller): more example javadoc
    public static class NumberEditor extends DefaultEditor
    {
        // This is here until DecimalFormat gets a constructor that
        // takes a Locale: 4923525
        private static String getDefaultPattern(Locale locale) {
            // Get the pattern for the default locale.
            LocaleProviderAdapter adapter;
            adapter = LocaleProviderAdapter.getAdapter(NumberFormatProvider.class,
                                                       locale);
            LocaleResources lr = adapter.getLocaleResources(locale);
            if (lr == null) {
                lr = LocaleProviderAdapter.forJRE().getLocaleResources(locale);
            }
            String[] all = lr.getNumberPatterns();
            return all[0];
        }

        /**
         * Construct a <code>JSpinner</code> editor that supports displaying
         * and editing the value of a <code>SpinnerNumberModel</code>
         * with a <code>JFormattedTextField</code>.  <code>This</code>
         * <code>NumberEditor</code> becomes both a <code>ChangeListener</code>
         * on the spinner and a <code>PropertyChangeListener</code>
         * on the new <code>JFormattedTextField</code>.
         *
         * @param spinner the spinner whose model <code>this</code> editor will monitor
         * @exception IllegalArgumentException if the spinners model is not
         *     an instance of <code>SpinnerNumberModel</code>
         *
         * @see #getModel
         * @see #getFormat
         * @see SpinnerNumberModel
         */
        public NumberEditor(JSpinner spinner) {
            this(spinner, getDefaultPattern(spinner.getLocale()));
        }

        /**
         * Construct a <code>JSpinner</code> editor that supports displaying
         * and editing the value of a <code>SpinnerNumberModel</code>
         * with a <code>JFormattedTextField</code>.  <code>This</code>
         * <code>NumberEditor</code> becomes both a <code>ChangeListener</code>
         * on the spinner and a <code>PropertyChangeListener</code>
         * on the new <code>JFormattedTextField</code>.
         *
         * @param spinner the spinner whose model <code>this</code> editor will monitor
         * @param decimalFormatPattern the initial pattern for the
         *     <code>DecimalFormat</code> object that's used to display
         *     and parse the value of the text field.
         * @exception IllegalArgumentException if the spinners model is not
         *     an instance of <code>SpinnerNumberModel</code> or if
         *     <code>decimalFormatPattern</code> is not a legal
         *     argument to <code>DecimalFormat</code>
         *
         * @see #getTextField
         * @see SpinnerNumberModel
         * @see java.text.DecimalFormat
         */
        public NumberEditor(JSpinner spinner, String decimalFormatPattern) {
            this(spinner, new DecimalFormat(decimalFormatPattern));
        }


        /**
         * Construct a <code>JSpinner</code> editor that supports displaying
         * and editing the value of a <code>SpinnerNumberModel</code>
         * with a <code>JFormattedTextField</code>.  <code>This</code>
         * <code>NumberEditor</code> becomes both a <code>ChangeListener</code>
         * on the spinner and a <code>PropertyChangeListener</code>
         * on the new <code>JFormattedTextField</code>.
         *
         * @param spinner the spinner whose model <code>this</code> editor will monitor
         * @param format the initial pattern for the
         *     <code>DecimalFormat</code> object that's used to display
         *     and parse the value of the text field.
         * @exception IllegalArgumentException if the spinners model is not
         *     an instance of <code>SpinnerNumberModel</code>
         *
         * @see #getTextField
         * @see SpinnerNumberModel
         * @see java.text.DecimalFormat
         */
        private NumberEditor(JSpinner spinner, DecimalFormat format) {
            super(spinner);
            if (!(spinner.getModel() instanceof SpinnerNumberModel)) {
                throw new IllegalArgumentException(
                          "model not a SpinnerNumberModel");
            }

            SpinnerNumberModel model = (SpinnerNumberModel)spinner.getModel();
            NumberFormatter formatter = new NumberEditorFormatter(model,
                                                                  format);
            DefaultFormatterFactory factory = new DefaultFormatterFactory(
                                                  formatter);
            JFormattedTextField ftf = getTextField();
            ftf.setEditable(true);
            ftf.setFormatterFactory(factory);
            // Change the text orientation for the NumberEditor
            ftf.setHorizontalAlignment(JTextField.RIGHT);

            /* TBD - initializing the column width of the text field
             * is imprecise and doing it here is tricky because
             * the developer may configure the formatter later.
             */
            try {
                String maxString = formatter.valueToString(model.getMinimum());
                String minString = formatter.valueToString(model.getMaximum());
                ftf.setColumns(Math.max(maxString.length(),
                                        minString.length()));
            }
            catch (ParseException e) {
                // TBD should throw a chained error here
            }

        }


        /**
         * Returns the <code>java.text.DecimalFormat</code> object the
         * <code>JFormattedTextField</code> uses to parse and format
         * numbers.
         *
         * @return the value of <code>getTextField().getFormatter().getFormat()</code>.
         * @see #getTextField
         * @see java.text.DecimalFormat
         */
        public DecimalFormat getFormat() {
            return (DecimalFormat)((NumberFormatter)(getTextField().getFormatter())).getFormat();
        }


        /**
         * Return our spinner ancestor's <code>SpinnerNumberModel</code>.
         *
         * @return <code>getSpinner().getModel()</code>
         * @see #getSpinner
         * @see #getTextField
         */
        public SpinnerNumberModel getModel() {
            return (SpinnerNumberModel)(getSpinner().getModel());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setComponentOrientation(ComponentOrientation o) {
            super.setComponentOrientation(o);
            getTextField().setHorizontalAlignment(
                    o.isLeftToRight() ? JTextField.RIGHT : JTextField.LEFT);
        }
    }


    /**
     * An editor for a <code>JSpinner</code> whose model is a
     * <code>SpinnerListModel</code>.
     * @since 1.4
     */
    public static class ListEditor extends DefaultEditor
    {
        /**
         * Construct a <code>JSpinner</code> editor that supports displaying
         * and editing the value of a <code>SpinnerListModel</code>
         * with a <code>JFormattedTextField</code>.  <code>This</code>
         * <code>ListEditor</code> becomes both a <code>ChangeListener</code>
         * on the spinner and a <code>PropertyChangeListener</code>
         * on the new <code>JFormattedTextField</code>.
         *
         * @param spinner the spinner whose model <code>this</code> editor will monitor
         * @exception IllegalArgumentException if the spinners model is not
         *     an instance of <code>SpinnerListModel</code>
         *
         * @see #getModel
         * @see SpinnerListModel
         */
        public ListEditor(JSpinner spinner) {
            super(spinner);
            if (!(spinner.getModel() instanceof SpinnerListModel)) {
                throw new IllegalArgumentException("model not a SpinnerListModel");
            }
            getTextField().setEditable(true);
            getTextField().setFormatterFactory(new
                              DefaultFormatterFactory(new ListFormatter()));
        }

        /**
         * Return our spinner ancestor's <code>SpinnerNumberModel</code>.
         *
         * @return <code>getSpinner().getModel()</code>
         * @see #getSpinner
         * @see #getTextField
         */
        public SpinnerListModel getModel() {
            return (SpinnerListModel)(getSpinner().getModel());
        }


        /**
         * ListFormatter provides completion while text is being input
         * into the JFormattedTextField. Completion is only done if the
         * user is inserting text at the end of the document. Completion
         * is done by way of the SpinnerListModel method findNextMatch.
         */
        private class ListFormatter extends
                          JFormattedTextField.AbstractFormatter {
            private DocumentFilter filter;

            public String valueToString(Object value) throws ParseException {
                if (value == null) {
                    return "";
                }
                return value.toString();
            }

            public Object stringToValue(String string) throws ParseException {
                return string;
            }

            protected DocumentFilter getDocumentFilter() {
                if (filter == null) {
                    filter = new Filter();
                }
                return filter;
            }


            private class Filter extends DocumentFilter {
                public void replace(FilterBypass fb, int offset, int length,
                                    String string, AttributeSet attrs) throws
                                           BadLocationException {
                    if (string != null && (offset + length) ==
                                          fb.getDocument().getLength()) {
                        Object next = getModel().findNextMatch(
                                         fb.getDocument().getText(0, offset) +
                                         string);
                        String value = (next != null) ? next.toString() : null;

                        if (value != null) {
                            fb.remove(0, offset + length);
                            fb.insertString(0, value, null);
                            getFormattedTextField().select(offset +
                                                           string.length(),
                                                           value.length());
                            return;
                        }
                    }
                    super.replace(fb, offset, length, string, attrs);
                }

                public void insertString(FilterBypass fb, int offset,
                                     String string, AttributeSet attr)
                       throws BadLocationException {
                    replace(fb, offset, 0, string, attr);
                }
            }
        }
    }


    /**
     * An Action implementation that is always disabled.
     */
    private static class DisabledAction implements Action {
        public Object getValue(String key) {
            return null;
        }
        public void putValue(String key, Object value) {
        }
        public void setEnabled(boolean b) {
        }
        public boolean isEnabled() {
            return false;
        }
        public void addPropertyChangeListener(PropertyChangeListener l) {
        }
        public void removePropertyChangeListener(PropertyChangeListener l) {
        }
        public void actionPerformed(ActionEvent ae) {
        }
    }

    /////////////////
    // Accessibility support
    ////////////////

    /**
     * Gets the <code>AccessibleContext</code> for the <code>JSpinner</code>
     *
     * @return the <code>AccessibleContext</code> for the <code>JSpinner</code>
     * @since 1.5
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJSpinner();
        }
        return accessibleContext;
    }

    /**
     * <code>AccessibleJSpinner</code> implements accessibility
     * support for the <code>JSpinner</code> class.
     * @since 1.5
     */
    protected class AccessibleJSpinner extends AccessibleJComponent
        implements AccessibleValue, AccessibleAction, AccessibleText,
                   AccessibleEditableText, ChangeListener {

        private Object oldModelValue = null;

        /**
         * AccessibleJSpinner constructor
         */
        protected AccessibleJSpinner() {
            // model is guaranteed to be non-null
            oldModelValue = model.getValue();
            JSpinner.this.addChangeListener(this);
        }

        /**
         * Invoked when the target of the listener has changed its state.
         *
         * @param e  a <code>ChangeEvent</code> object. Must not be null.
         * @throws NullPointerException if the parameter is null.
         */
        public void stateChanged(ChangeEvent e) {
            if (e == null) {
                throw new NullPointerException();
            }
            Object newModelValue = model.getValue();
            firePropertyChange(ACCESSIBLE_VALUE_PROPERTY,
                               oldModelValue,
                               newModelValue);
            firePropertyChange(ACCESSIBLE_TEXT_PROPERTY,
                               null,
                               0); // entire text may have changed

            oldModelValue = newModelValue;
        }

        /* ===== Begin AccessibleContext methods ===== */

        /**
         * Gets the role of this object.  The role of the object is the generic
         * purpose or use of the class of this object.  For example, the role
         * of a push button is AccessibleRole.PUSH_BUTTON.  The roles in
         * AccessibleRole are provided so component developers can pick from
         * a set of predefined roles.  This enables assistive technologies to
         * provide a consistent interface to various tweaked subclasses of
         * components (e.g., use AccessibleRole.PUSH_BUTTON for all components
         * that act like a push button) as well as distinguish between subclasses
         * that behave differently (e.g., AccessibleRole.CHECK_BOX for check boxes
         * and AccessibleRole.RADIO_BUTTON for radio buttons).
         * <p>Note that the AccessibleRole class is also extensible, so
         * custom component developers can define their own AccessibleRole's
         * if the set of predefined roles is inadequate.
         *
         * @return an instance of AccessibleRole describing the role of the object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SPIN_BOX;
        }

        /**
         * Returns the number of accessible children of the object.
         *
         * @return the number of accessible children of the object.
         */
        public int getAccessibleChildrenCount() {
            // the JSpinner has one child, the editor
            if (editor.getAccessibleContext() != null) {
                return 1;
            }
            return 0;
        }

        /**
         * Returns the specified Accessible child of the object.  The Accessible
         * children of an Accessible object are zero-based, so the first child
         * of an Accessible child is at index 0, the second child is at index 1,
         * and so on.
         *
         * @param i zero-based index of child
         * @return the Accessible child of the object
         * @see #getAccessibleChildrenCount
         */
        public Accessible getAccessibleChild(int i) {
            // the JSpinner has one child, the editor
            if (i != 0) {
                return null;
            }
            if (editor.getAccessibleContext() != null) {
                return (Accessible)editor;
            }
            return null;
        }

        /* ===== End AccessibleContext methods ===== */

        /**
         * Gets the AccessibleAction associated with this object that supports
         * one or more actions.
         *
         * @return AccessibleAction if supported by object; else return null
         * @see AccessibleAction
         */
        public AccessibleAction getAccessibleAction() {
            return this;
        }

        /**
         * Gets the AccessibleText associated with this object presenting
         * text on the display.
         *
         * @return AccessibleText if supported by object; else return null
         * @see AccessibleText
         */
        public AccessibleText getAccessibleText() {
            return this;
        }

        /*
         * Returns the AccessibleContext for the JSpinner editor
         */
        private AccessibleContext getEditorAccessibleContext() {
            if (editor instanceof DefaultEditor) {
                JTextField textField = ((DefaultEditor)editor).getTextField();
                if (textField != null) {
                    return textField.getAccessibleContext();
                }
            } else if (editor instanceof Accessible) {
                return editor.getAccessibleContext();
            }
            return null;
        }

        /*
         * Returns the AccessibleText for the JSpinner editor
         */
        private AccessibleText getEditorAccessibleText() {
            AccessibleContext ac = getEditorAccessibleContext();
            if (ac != null) {
                return ac.getAccessibleText();
            }
            return null;
        }

        /*
         * Returns the AccessibleEditableText for the JSpinner editor
         */
        private AccessibleEditableText getEditorAccessibleEditableText() {
            AccessibleText at = getEditorAccessibleText();
            if (at instanceof AccessibleEditableText) {
                return (AccessibleEditableText)at;
            }
            return null;
        }

        /**
         * Gets the AccessibleValue associated with this object.
         *
         * @return AccessibleValue if supported by object; else return null
         * @see AccessibleValue
         *
         */
        public AccessibleValue getAccessibleValue() {
            return this;
        }

        /* ===== Begin AccessibleValue impl ===== */

        /**
         * Get the value of this object as a Number.  If the value has not been
         * set, the return value will be null.
         *
         * @return value of the object
         * @see #setCurrentAccessibleValue
         */
        public Number getCurrentAccessibleValue() {
            Object o = model.getValue();
            if (o instanceof Number) {
                return (Number)o;
            }
            return null;
        }

        /**
         * Set the value of this object as a Number.
         *
         * @param n the value to set for this object
         * @return true if the value was set; else False
         * @see #getCurrentAccessibleValue
         */
        public boolean setCurrentAccessibleValue(Number n) {
            // try to set the new value
            try {
                model.setValue(n);
                return true;
            } catch (IllegalArgumentException iae) {
                // SpinnerModel didn't like new value
            }
            return false;
        }

        /**
         * Get the minimum value of this object as a Number.
         *
         * @return Minimum value of the object; null if this object does not
         * have a minimum value
         * @see #getMaximumAccessibleValue
         */
        public Number getMinimumAccessibleValue() {
            if (model instanceof SpinnerNumberModel) {
                SpinnerNumberModel numberModel = (SpinnerNumberModel)model;
                Object o = numberModel.getMinimum();
                if (o instanceof Number) {
                    return (Number)o;
                }
            }
            return null;
        }

        /**
         * Get the maximum value of this object as a Number.
         *
         * @return Maximum value of the object; null if this object does not
         * have a maximum value
         * @see #getMinimumAccessibleValue
         */
        public Number getMaximumAccessibleValue() {
            if (model instanceof SpinnerNumberModel) {
                SpinnerNumberModel numberModel = (SpinnerNumberModel)model;
                Object o = numberModel.getMaximum();
                if (o instanceof Number) {
                    return (Number)o;
                }
            }
            return null;
        }

        /* ===== End AccessibleValue impl ===== */

        /* ===== Begin AccessibleAction impl ===== */

        /**
         * Returns the number of accessible actions available in this object
         * If there are more than one, the first one is considered the "default"
         * action of the object.
         *
         * Two actions are supported: AccessibleAction.INCREMENT which
         * increments the spinner value and AccessibleAction.DECREMENT
         * which decrements the spinner value
         *
         * @return the zero-based number of Actions in this object
         */
        public int getAccessibleActionCount() {
            return 2;
        }

        /**
         * Returns a description of the specified action of the object.
         *
         * @param i zero-based index of the actions
         * @return a String description of the action
         * @see #getAccessibleActionCount
         */
        public String getAccessibleActionDescription(int i) {
            if (i == 0) {
                return AccessibleAction.INCREMENT;
            } else if (i == 1) {
                return AccessibleAction.DECREMENT;
            }
            return null;
        }

        /**
         * Performs the specified Action on the object
         *
         * @param i zero-based index of actions. The first action
         * (index 0) is AccessibleAction.INCREMENT and the second
         * action (index 1) is AccessibleAction.DECREMENT.
         * @return true if the action was performed; otherwise false.
         * @see #getAccessibleActionCount
         */
        public boolean doAccessibleAction(int i) {
            if (i < 0 || i > 1) {
                return false;
            }
            Object o;
            if (i == 0) {
                o = getNextValue(); // AccessibleAction.INCREMENT
            } else {
                o = getPreviousValue(); // AccessibleAction.DECREMENT
            }
            // try to set the new value
            try {
                model.setValue(o);
                return true;
            } catch (IllegalArgumentException iae) {
                // SpinnerModel didn't like new value
            }
            return false;
        }

        /* ===== End AccessibleAction impl ===== */

        /* ===== Begin AccessibleText impl ===== */

        /*
         * Returns whether source and destination components have the
         * same window ancestor
         */
        private boolean sameWindowAncestor(Component src, Component dest) {
            if (src == null || dest == null) {
                return false;
            }
            return SwingUtilities.getWindowAncestor(src) ==
                SwingUtilities.getWindowAncestor(dest);
        }

        /**
         * Given a point in local coordinates, return the zero-based index
         * of the character under that Point.  If the point is invalid,
         * this method returns -1.
         *
         * @param p the Point in local coordinates
         * @return the zero-based index of the character under Point p; if
         * Point is invalid return -1.
         */
        public int getIndexAtPoint(Point p) {
            AccessibleText at = getEditorAccessibleText();
            if (at != null && sameWindowAncestor(JSpinner.this, editor)) {
                // convert point from the JSpinner bounds (source) to
                // editor bounds (destination)
                Point editorPoint = SwingUtilities.convertPoint(JSpinner.this,
                                                                p,
                                                                editor);
                if (editorPoint != null) {
                    return at.getIndexAtPoint(editorPoint);
                }
            }
            return -1;
        }

        /**
         * Determines the bounding box of the character at the given
         * index into the string.  The bounds are returned in local
         * coordinates.  If the index is invalid an empty rectangle is
         * returned.
         *
         * @param i the index into the String
         * @return the screen coordinates of the character's bounding box,
         * if index is invalid return an empty rectangle.
         */
        public Rectangle getCharacterBounds(int i) {
            AccessibleText at = getEditorAccessibleText();
            if (at != null ) {
                Rectangle editorRect = at.getCharacterBounds(i);
                if (editorRect != null &&
                    sameWindowAncestor(JSpinner.this, editor)) {
                    // return rectangle in the JSpinner bounds
                    return SwingUtilities.convertRectangle(editor,
                                                           editorRect,
                                                           JSpinner.this);
                }
            }
            return null;
        }

        /**
         * Returns the number of characters (valid indicies)
         *
         * @return the number of characters
         */
        public int getCharCount() {
            AccessibleText at = getEditorAccessibleText();
            if (at != null) {
                return at.getCharCount();
            }
            return -1;
        }

        /**
         * Returns the zero-based offset of the caret.
         *
         * Note: That to the right of the caret will have the same index
         * value as the offset (the caret is between two characters).
         * @return the zero-based offset of the caret.
         */
        public int getCaretPosition() {
            AccessibleText at = getEditorAccessibleText();
            if (at != null) {
                return at.getCaretPosition();
            }
            return -1;
        }

        /**
         * Returns the String at a given index.
         *
         * @param part the CHARACTER, WORD, or SENTENCE to retrieve
         * @param index an index within the text
         * @return the letter, word, or sentence
         */
        public String getAtIndex(int part, int index) {
            AccessibleText at = getEditorAccessibleText();
            if (at != null) {
                return at.getAtIndex(part, index);
            }
            return null;
        }

        /**
         * Returns the String after a given index.
         *
         * @param part the CHARACTER, WORD, or SENTENCE to retrieve
         * @param index an index within the text
         * @return the letter, word, or sentence
         */
        public String getAfterIndex(int part, int index) {
            AccessibleText at = getEditorAccessibleText();
            if (at != null) {
                return at.getAfterIndex(part, index);
            }
            return null;
        }

        /**
         * Returns the String before a given index.
         *
         * @param part the CHARACTER, WORD, or SENTENCE to retrieve
         * @param index an index within the text
         * @return the letter, word, or sentence
         */
        public String getBeforeIndex(int part, int index) {
            AccessibleText at = getEditorAccessibleText();
            if (at != null) {
                return at.getBeforeIndex(part, index);
            }
            return null;
        }

        /**
         * Returns the AttributeSet for a given character at a given index
         *
         * @param i the zero-based index into the text
         * @return the AttributeSet of the character
         */
        public AttributeSet getCharacterAttribute(int i) {
            AccessibleText at = getEditorAccessibleText();
            if (at != null) {
                return at.getCharacterAttribute(i);
            }
            return null;
        }

        /**
         * Returns the start offset within the selected text.
         * If there is no selection, but there is
         * a caret, the start and end offsets will be the same.
         *
         * @return the index into the text of the start of the selection
         */
        public int getSelectionStart() {
            AccessibleText at = getEditorAccessibleText();
            if (at != null) {
                return at.getSelectionStart();
            }
            return -1;
        }

        /**
         * Returns the end offset within the selected text.
         * If there is no selection, but there is
         * a caret, the start and end offsets will be the same.
         *
         * @return the index into the text of the end of the selection
         */
        public int getSelectionEnd() {
            AccessibleText at = getEditorAccessibleText();
            if (at != null) {
                return at.getSelectionEnd();
            }
            return -1;
        }

        /**
         * Returns the portion of the text that is selected.
         *
         * @return the String portion of the text that is selected
         */
        public String getSelectedText() {
            AccessibleText at = getEditorAccessibleText();
            if (at != null) {
                return at.getSelectedText();
            }
            return null;
        }

        /* ===== End AccessibleText impl ===== */


        /* ===== Begin AccessibleEditableText impl ===== */

        /**
         * Sets the text contents to the specified string.
         *
         * @param s the string to set the text contents
         */
        public void setTextContents(String s) {
            AccessibleEditableText at = getEditorAccessibleEditableText();
            if (at != null) {
                at.setTextContents(s);
            }
        }

        /**
         * Inserts the specified string at the given index/
         *
         * @param index the index in the text where the string will
         * be inserted
         * @param s the string to insert in the text
         */
        public void insertTextAtIndex(int index, String s) {
            AccessibleEditableText at = getEditorAccessibleEditableText();
            if (at != null) {
                at.insertTextAtIndex(index, s);
            }
        }

        /**
         * Returns the text string between two indices.
         *
         * @param startIndex the starting index in the text
         * @param endIndex the ending index in the text
         * @return the text string between the indices
         */
        public String getTextRange(int startIndex, int endIndex) {
            AccessibleEditableText at = getEditorAccessibleEditableText();
            if (at != null) {
                return at.getTextRange(startIndex, endIndex);
            }
            return null;
        }

        /**
         * Deletes the text between two indices
         *
         * @param startIndex the starting index in the text
         * @param endIndex the ending index in the text
         */
        public void delete(int startIndex, int endIndex) {
            AccessibleEditableText at = getEditorAccessibleEditableText();
            if (at != null) {
                at.delete(startIndex, endIndex);
            }
        }

        /**
         * Cuts the text between two indices into the system clipboard.
         *
         * @param startIndex the starting index in the text
         * @param endIndex the ending index in the text
         */
        public void cut(int startIndex, int endIndex) {
            AccessibleEditableText at = getEditorAccessibleEditableText();
            if (at != null) {
                at.cut(startIndex, endIndex);
            }
        }

        /**
         * Pastes the text from the system clipboard into the text
         * starting at the specified index.
         *
         * @param startIndex the starting index in the text
         */
        public void paste(int startIndex) {
            AccessibleEditableText at = getEditorAccessibleEditableText();
            if (at != null) {
                at.paste(startIndex);
            }
        }

        /**
         * Replaces the text between two indices with the specified
         * string.
         *
         * @param startIndex the starting index in the text
         * @param endIndex the ending index in the text
         * @param s the string to replace the text between two indices
         */
        public void replaceText(int startIndex, int endIndex, String s) {
            AccessibleEditableText at = getEditorAccessibleEditableText();
            if (at != null) {
                at.replaceText(startIndex, endIndex, s);
            }
        }

        /**
         * Selects the text between two indices.
         *
         * @param startIndex the starting index in the text
         * @param endIndex the ending index in the text
         */
        public void selectText(int startIndex, int endIndex) {
            AccessibleEditableText at = getEditorAccessibleEditableText();
            if (at != null) {
                at.selectText(startIndex, endIndex);
            }
        }

        /**
         * Sets attributes for the text between two indices.
         *
         * @param startIndex the starting index in the text
         * @param endIndex the ending index in the text
         * @param as the attribute set
         * @see AttributeSet
         */
        public void setAttributes(int startIndex, int endIndex, AttributeSet as) {
            AccessibleEditableText at = getEditorAccessibleEditableText();
            if (at != null) {
                at.setAttributes(startIndex, endIndex, as);
            }
        }
    }  /* End AccessibleJSpinner */
}
