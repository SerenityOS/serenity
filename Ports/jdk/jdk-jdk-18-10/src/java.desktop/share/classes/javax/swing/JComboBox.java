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
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.IllegalComponentStateException;
import java.awt.ItemSelectable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.Transient;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleEditableText;
import javax.accessibility.AccessibleIcon;
import javax.accessibility.AccessibleRelationSet;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleTable;
import javax.accessibility.AccessibleText;
import javax.accessibility.AccessibleValue;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.ComboBoxUI;

/**
 * A component that combines a button or editable field and a drop-down list.
 * The user can select a value from the drop-down list, which appears at the
 * user's request. If you make the combo box editable, then the combo box
 * includes an editable field into which the user can type a value.
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
 * <p>
 * See <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/combobox.html">How to Use Combo Boxes</a>
 * in <a href="https://docs.oracle.com/javase/tutorial/"><em>The Java Tutorial</em></a>
 * for further information.
 *
 * @see ComboBoxModel
 * @see DefaultComboBoxModel
 *
 * @param <E> the type of the elements of this combo box
 *
 * @author Arnaud Weber
 * @author Mark Davidson
 * @since 1.2
 */
@JavaBean(defaultProperty = "UI", description = "A combination of a text field and a drop-down list.")
@SwingContainer(false)
@SuppressWarnings("serial") // Same-version serialization only
public class JComboBox<E> extends JComponent
implements ItemSelectable,ListDataListener,ActionListener, Accessible {
    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "ComboBoxUI";

    /**
     * This protected field is implementation specific. Do not access directly
     * or override. Use the accessor methods instead.
     *
     * @see #getModel
     * @see #setModel
     */
    protected ComboBoxModel<E>    dataModel;
    /**
     * This protected field is implementation specific. Do not access directly
     * or override. Use the accessor methods instead.
     *
     * @see #getRenderer
     * @see #setRenderer
     */
    protected ListCellRenderer<? super E> renderer;
    /**
     * This protected field is implementation specific. Do not access directly
     * or override. Use the accessor methods instead.
     *
     * @see #getEditor
     * @see #setEditor
     */
    protected ComboBoxEditor       editor;
    /**
     * This protected field is implementation specific. Do not access directly
     * or override. Use the accessor methods instead.
     *
     * @see #getMaximumRowCount
     * @see #setMaximumRowCount
     */
    protected int maximumRowCount = 8;

    /**
     * This protected field is implementation specific. Do not access directly
     * or override. Use the accessor methods instead.
     *
     * @see #isEditable
     * @see #setEditable
     */
    protected boolean isEditable  = false;
    /**
     * This protected field is implementation specific. Do not access directly
     * or override. Use the accessor methods instead.
     *
     * @see #setKeySelectionManager
     * @see #getKeySelectionManager
     */
    protected KeySelectionManager keySelectionManager = null;
    /**
     * This protected field is implementation specific. Do not access directly
     * or override. Use the accessor methods instead.
     *
     * @see #setActionCommand
     * @see #getActionCommand
     */
    protected String actionCommand = "comboBoxChanged";
    /**
     * This protected field is implementation specific. Do not access directly
     * or override. Use the accessor methods instead.
     *
     * @see #setLightWeightPopupEnabled
     * @see #isLightWeightPopupEnabled
     */
    protected boolean lightWeightPopupEnabled = JPopupMenu.getDefaultLightWeightPopupEnabled();

    /**
     * This protected field is implementation specific. Do not access directly
     * or override.
     */
    protected Object selectedItemReminder = null;

    private E prototypeDisplayValue;

    // Flag to ensure that infinite loops do not occur with ActionEvents.
    private boolean firingActionEvent = false;

    // Flag to ensure the we don't get multiple ActionEvents on item selection.
    private boolean selectingItem = false;

    // Flag to indicate UI update is in progress
    private transient boolean updateInProgress;

    /**
     * Creates a <code>JComboBox</code> that takes its items from an
     * existing <code>ComboBoxModel</code>.  Since the
     * <code>ComboBoxModel</code> is provided, a combo box created using
     * this constructor does not create a default combo box model and
     * may impact how the insert, remove and add methods behave.
     *
     * @param aModel the <code>ComboBoxModel</code> that provides the
     *          displayed list of items
     * @see DefaultComboBoxModel
     */
    public JComboBox(ComboBoxModel<E> aModel) {
        super();
        setModel(aModel);
        init();
    }

    /**
     * Creates a <code>JComboBox</code> that contains the elements
     * in the specified array.  By default the first item in the array
     * (and therefore the data model) becomes selected.
     *
     * @param items  an array of objects to insert into the combo box
     * @see DefaultComboBoxModel
     */
    public JComboBox(E[] items) {
        super();
        setModel(new DefaultComboBoxModel<E>(items));
        init();
    }

    /**
     * Creates a <code>JComboBox</code> that contains the elements
     * in the specified Vector.  By default the first item in the vector
     * (and therefore the data model) becomes selected.
     *
     * @param items  an array of vectors to insert into the combo box
     * @see DefaultComboBoxModel
     */
    public JComboBox(Vector<E> items) {
        super();
        setModel(new DefaultComboBoxModel<E>(items));
        init();
    }

    /**
     * Creates a <code>JComboBox</code> with a default data model.
     * The default data model is an empty list of objects.
     * Use <code>addItem</code> to add items.  By default the first item
     * in the data model becomes selected.
     *
     * @see DefaultComboBoxModel
     */
    public JComboBox() {
        super();
        setModel(new DefaultComboBoxModel<E>());
        init();
    }

    private void init() {
        installAncestorListener();
        setUIProperty("opaque",true);
        updateUI();
    }

    /**
     * Registers ancestor listener so that it will receive
     * {@code AncestorEvents} when it or any of its ancestors
     * move or are made visible or invisible.
     * Events are also sent when the component or its ancestors are added
     * or removed from the containment hierarchy.
     */
    protected void installAncestorListener() {
        addAncestorListener(new AncestorListener(){
                                public void ancestorAdded(AncestorEvent event){ hidePopup();}
                                public void ancestorRemoved(AncestorEvent event){ hidePopup();}
                                public void ancestorMoved(AncestorEvent event){
                                    if (event.getSource() != JComboBox.this)
                                        hidePopup();
                                }});
    }

    /**
     * Sets the L&amp;F object that renders this component.
     *
     * @param ui  the <code>ComboBoxUI</code> L&amp;F object
     * @see UIDefaults#getUI
     */
    @BeanProperty(hidden = true, visualUpdate = true, description
            = "The UI object that implements the Component's LookAndFeel.")
    public void setUI(ComboBoxUI ui) {
        super.setUI(ui);
    }

    /**
     * Resets the UI property to a value from the current look and feel.
     *
     * @see JComponent#updateUI
     */
    public void updateUI() {
        if (!updateInProgress) {
            updateInProgress = true;
            try {
                setUI((ComboBoxUI)UIManager.getUI(this));

                ListCellRenderer<? super E> renderer = getRenderer();
                if (renderer instanceof Component) {
                    SwingUtilities.updateComponentTreeUI((Component)renderer);
                }
            } finally {
                updateInProgress = false;
            }
        }
    }


    /**
     * Returns the name of the L&amp;F class that renders this component.
     *
     * @return the string "ComboBoxUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false)
    public String getUIClassID() {
        return uiClassID;
    }


    /**
     * Returns the L&amp;F object that renders this component.
     *
     * @return the ComboBoxUI object that renders this component
     */
    public ComboBoxUI getUI() {
        return(ComboBoxUI)ui;
    }

    /**
     * Sets the data model that the <code>JComboBox</code> uses to obtain
     * the list of items.
     *
     * @param aModel the <code>ComboBoxModel</code> that provides the
     *  displayed list of items
     */
    @BeanProperty(description
            = "Model that the combo box uses to get data to display.")
    public void setModel(ComboBoxModel<E> aModel) {
        ComboBoxModel<E> oldModel = dataModel;
        if (oldModel != null) {
            oldModel.removeListDataListener(this);
        }
        dataModel = aModel;
        dataModel.addListDataListener(this);

        // set the current selected item.
        selectedItemReminder = dataModel.getSelectedItem();

        firePropertyChange( "model", oldModel, dataModel);
    }

    /**
     * Returns the data model currently used by the <code>JComboBox</code>.
     *
     * @return the <code>ComboBoxModel</code> that provides the displayed
     *                  list of items
     */
    public ComboBoxModel<E> getModel() {
        return dataModel;
    }

    /*
     * Properties
     */

    /**
     * Sets the <code>lightWeightPopupEnabled</code> property, which
     * provides a hint as to whether or not a lightweight
     * <code>Component</code> should be used to contain the
     * <code>JComboBox</code>, versus a heavyweight
     * <code>Component</code> such as a <code>Panel</code>
     * or a <code>Window</code>.  The decision of lightweight
     * versus heavyweight is ultimately up to the
     * <code>JComboBox</code>.  Lightweight windows are more
     * efficient than heavyweight windows, but lightweight
     * and heavyweight components do not mix well in a GUI.
     * If your application mixes lightweight and heavyweight
     * components, you should disable lightweight popups.
     * The default value for the <code>lightWeightPopupEnabled</code>
     * property is <code>true</code>, unless otherwise specified
     * by the look and feel.  Some look and feels always use
     * heavyweight popups, no matter what the value of this property.
     * <p>
     * See the article <a href="http://www.oracle.com/technetwork/articles/java/mixing-components-433992.html">Mixing Heavy and Light Components</a>
     * This method fires a property changed event.
     *
     * @param aFlag if <code>true</code>, lightweight popups are desired
     */
    @BeanProperty(expert = true, description
            = "Set to <code>false</code> to require heavyweight popups.")
    public void setLightWeightPopupEnabled(boolean aFlag) {
        boolean oldFlag = lightWeightPopupEnabled;
        lightWeightPopupEnabled = aFlag;
        firePropertyChange("lightWeightPopupEnabled", oldFlag, lightWeightPopupEnabled);
    }

    /**
     * Gets the value of the <code>lightWeightPopupEnabled</code>
     * property.
     *
     * @return the value of the <code>lightWeightPopupEnabled</code>
     *    property
     * @see #setLightWeightPopupEnabled
     */
    public boolean isLightWeightPopupEnabled() {
        return lightWeightPopupEnabled;
    }

    /**
     * Determines whether the <code>JComboBox</code> field is editable.
     * An editable <code>JComboBox</code> allows the user to type into the
     * field or selected an item from the list to initialize the field,
     * after which it can be edited. (The editing affects only the field,
     * the list item remains intact.) A non editable <code>JComboBox</code>
     * displays the selected item in the field,
     * but the selection cannot be modified.
     *
     * @param aFlag a boolean value, where true indicates that the
     *                  field is editable
     */
    @BeanProperty(preferred = true, description
            = "If true, the user can type a new value in the combo box.")
    public void setEditable(boolean aFlag) {
        boolean oldFlag = isEditable;
        isEditable = aFlag;
        firePropertyChange( "editable", oldFlag, isEditable );
    }

    /**
     * Returns true if the <code>JComboBox</code> is editable.
     * By default, a combo box is not editable.
     *
     * @return true if the <code>JComboBox</code> is editable, else false
     */
    public boolean isEditable() {
        return isEditable;
    }

    /**
     * Sets the maximum number of rows the <code>JComboBox</code> displays.
     * If the number of objects in the model is greater than count,
     * the combo box uses a scrollbar.
     *
     * @param count an integer specifying the maximum number of items to
     *              display in the list before using a scrollbar
     */
    @BeanProperty(preferred = true, description
            = "The maximum number of rows the popup should have")
    public void setMaximumRowCount(int count) {
        int oldCount = maximumRowCount;
        maximumRowCount = count;
        firePropertyChange( "maximumRowCount", oldCount, maximumRowCount );
    }

    /**
     * Returns the maximum number of items the combo box can display
     * without a scrollbar
     *
     * @return an integer specifying the maximum number of items that are
     *         displayed in the list before using a scrollbar
     */
    public int getMaximumRowCount() {
        return maximumRowCount;
    }

    /**
     * Sets the renderer that paints the list items and the item selected from the list in
     * the JComboBox field. The renderer is used if the JComboBox is not
     * editable. If it is editable, the editor is used to render and edit
     * the selected item.
     * <p>
     * The default renderer displays a string or an icon.
     * Other renderers can handle graphic images and composite items.
     * <p>
     * To display the selected item,
     * <code>aRenderer.getListCellRendererComponent</code>
     * is called, passing the list object and an index of -1.
     *
     * @param aRenderer  the <code>ListCellRenderer</code> that
     *                  displays the selected item
     * @see #setEditor
     */
    @BeanProperty(expert = true, description
            = "The renderer that paints the item selected in the list.")
    public void setRenderer(ListCellRenderer<? super E> aRenderer) {
        ListCellRenderer<? super E> oldRenderer = renderer;
        renderer = aRenderer;
        firePropertyChange( "renderer", oldRenderer, renderer );
        invalidate();
    }

    /**
     * Returns the renderer used to display the selected item in the
     * <code>JComboBox</code> field.
     *
     * @return  the <code>ListCellRenderer</code> that displays
     *                  the selected item.
     */
    public ListCellRenderer<? super E> getRenderer() {
        return renderer;
    }

    /**
     * Sets the editor used to paint and edit the selected item in the
     * <code>JComboBox</code> field.  The editor is used only if the
     * receiving <code>JComboBox</code> is editable. If not editable,
     * the combo box uses the renderer to paint the selected item.
     *
     * @param anEditor  the <code>ComboBoxEditor</code> that
     *                  displays the selected item
     * @see #setRenderer
     */
    @BeanProperty(expert = true, description
            = "The editor that combo box uses to edit the current value")
    public void setEditor(ComboBoxEditor anEditor) {
        ComboBoxEditor oldEditor = editor;

        if ( editor != null ) {
            editor.removeActionListener(this);
        }
        editor = anEditor;
        if ( editor != null ) {
            editor.addActionListener(this);
        }
        firePropertyChange( "editor", oldEditor, editor );
    }

    /**
     * Returns the editor used to paint and edit the selected item in the
     * <code>JComboBox</code> field.
     *
     * @return the <code>ComboBoxEditor</code> that displays the selected item
     */
    public ComboBoxEditor getEditor() {
        return editor;
    }

    //
    // Selection
    //

    /**
     * Sets the selected item in the combo box display area to the object in
     * the argument.
     * If <code>anObject</code> is in the list, the display area shows
     * <code>anObject</code> selected.
     * <p>
     * If <code>anObject</code> is <i>not</i> in the list and the combo box is
     * uneditable, it will not change the current selection. For editable
     * combo boxes, the selection will change to <code>anObject</code>.
     * <p>
     * If this constitutes a change in the selected item,
     * <code>ItemListener</code>s added to the combo box will be notified with
     * one or two <code>ItemEvent</code>s.
     * If there is a current selected item, an <code>ItemEvent</code> will be
     * fired and the state change will be <code>ItemEvent.DESELECTED</code>.
     * If <code>anObject</code> is in the list and is not currently selected
     * then an <code>ItemEvent</code> will be fired and the state change will
     * be <code>ItemEvent.SELECTED</code>.
     * <p>
     * <code>ActionListener</code>s added to the combo box will be notified
     * with an <code>ActionEvent</code> when this method is called.
     *
     * @param anObject  the list object to select; use <code>null</code> to
                        clear the selection
     */
    @BeanProperty(bound = false, preferred = true, description
            = "Sets the selected item in the JComboBox.")
    public void setSelectedItem(Object anObject) {
        Object oldSelection = selectedItemReminder;
        Object objectToSelect = anObject;
        if (oldSelection == null || !oldSelection.equals(anObject)) {

            if (anObject != null && !isEditable()) {
                // For non editable combo boxes, an invalid selection
                // will be rejected.
                boolean found = false;
                for (int i = 0; i < dataModel.getSize(); i++) {
                    E element = dataModel.getElementAt(i);
                    if (anObject.equals(element)) {
                        found = true;
                        objectToSelect = element;
                        break;
                    }
                }
                if (!found) {
                    return;
                }

                getEditor().setItem(anObject);
            }

            // Must toggle the state of this flag since this method
            // call may result in ListDataEvents being fired.
            selectingItem = true;
            dataModel.setSelectedItem(objectToSelect);
            selectingItem = false;

            if (selectedItemReminder != dataModel.getSelectedItem()) {
                // in case a users implementation of ComboBoxModel
                // doesn't fire a ListDataEvent when the selection
                // changes.
                selectedItemChanged();
            }
        }
        fireActionEvent();
    }

    /**
     * Returns the current selected item.
     * <p>
     * If the combo box is editable, then this value may not have been added
     * to the combo box with <code>addItem</code>, <code>insertItemAt</code>
     * or the data constructors.
     *
     * @return the current selected Object
     * @see #setSelectedItem
     */
    public Object getSelectedItem() {
        return dataModel.getSelectedItem();
    }

    /**
     * Selects the item at index <code>anIndex</code>.
     *
     * @param anIndex an integer specifying the list item to select,
     *                  where 0 specifies the first item in the list and -1 indicates no selection
     * @exception IllegalArgumentException if <code>anIndex</code> &lt; -1 or
     *                  <code>anIndex</code> is greater than or equal to size
     */
    @BeanProperty(bound = false, preferred = true, description
            = "The item at index is selected.")
    public void setSelectedIndex(int anIndex) {
        int size = dataModel.getSize();

        if ( anIndex == -1 ) {
            setSelectedItem( null );
        } else if ( anIndex < -1 || anIndex >= size ) {
            throw new IllegalArgumentException("setSelectedIndex: " + anIndex + " out of bounds");
        } else {
            setSelectedItem(dataModel.getElementAt(anIndex));
        }
    }

    /**
     * Returns the first item in the list that matches the given item.
     * The result is not always defined if the <code>JComboBox</code>
     * allows selected items that are not in the list.
     * Returns -1 if there is no selected item or if the user specified
     * an item which is not in the list.

     * @return an integer specifying the currently selected list item,
     *                  where 0 specifies
     *                  the first item in the list;
     *                  or -1 if no item is selected or if
     *                  the currently selected item is not in the list
     */
    @Transient
    public int getSelectedIndex() {
        Object sObject = dataModel.getSelectedItem();
        int i,c;
        E obj;

        for ( i=0,c=dataModel.getSize();i<c;i++ ) {
            obj = dataModel.getElementAt(i);
            if ( obj != null && obj.equals(sObject) )
                return i;
        }
        return -1;
    }

    /**
     * Returns the "prototypical display" value - an Object used
     * for the calculation of the display height and width.
     *
     * @return the value of the <code>prototypeDisplayValue</code> property
     * @see #setPrototypeDisplayValue
     * @since 1.4
     */
    public E getPrototypeDisplayValue() {
        return prototypeDisplayValue;
    }

    /**
     * Sets the prototype display value used to calculate the size of the display
     * for the UI portion.
     * <p>
     * If a prototype display value is specified, the preferred size of
     * the combo box is calculated by configuring the renderer with the
     * prototype display value and obtaining its preferred size. Specifying
     * the preferred display value is often useful when the combo box will be
     * displaying large amounts of data. If no prototype display value has
     * been specified, the renderer must be configured for each value from
     * the model and its preferred size obtained, which can be
     * relatively expensive.
     *
     * @param prototypeDisplayValue the prototype display value
     * @see #getPrototypeDisplayValue
     * @since 1.4
     */
    @BeanProperty(visualUpdate = true, description
            = "The display prototype value, used to compute display width and height.")
    public void setPrototypeDisplayValue(E prototypeDisplayValue) {
        Object oldValue = this.prototypeDisplayValue;
        this.prototypeDisplayValue = prototypeDisplayValue;
        firePropertyChange("prototypeDisplayValue", oldValue, prototypeDisplayValue);
    }

    /**
     * Adds an item to the item list.
     * This method works only if the <code>JComboBox</code> uses a
     * mutable data model.
     * <p>
     * <strong>Warning:</strong>
     * Focus and keyboard navigation problems may arise if you add duplicate
     * String objects. A workaround is to add new objects instead of String
     * objects and make sure that the toString() method is defined.
     * For example:
     * <pre>
     *   comboBox.addItem(makeObj("Item 1"));
     *   comboBox.addItem(makeObj("Item 1"));
     *   ...
     *   private Object makeObj(final String item)  {
     *     return new Object() { public String toString() { return item; } };
     *   }
     * </pre>
     *
     * @param item the item to add to the list
     * @see MutableComboBoxModel
     */
    public void addItem(E item) {
        checkMutableComboBoxModel();
        ((MutableComboBoxModel<E>)dataModel).addElement(item);
    }

    /**
     * Inserts an item into the item list at a given index.
     * This method works only if the <code>JComboBox</code> uses a
     * mutable data model.
     *
     * @param item the item to add to the list
     * @param index    an integer specifying the position at which
     *                  to add the item
     * @see MutableComboBoxModel
     */
    public void insertItemAt(E item, int index) {
        checkMutableComboBoxModel();
        ((MutableComboBoxModel<E>)dataModel).insertElementAt(item,index);
    }

    /**
     * Removes an item from the item list.
     * This method works only if the <code>JComboBox</code> uses a
     * mutable data model.
     *
     * @param anObject  the object to remove from the item list
     * @see MutableComboBoxModel
     */
    public void removeItem(Object anObject) {
        checkMutableComboBoxModel();
        ((MutableComboBoxModel)dataModel).removeElement(anObject);
    }

    /**
     * Removes the item at <code>anIndex</code>
     * This method works only if the <code>JComboBox</code> uses a
     * mutable data model.
     *
     * @param anIndex  an int specifying the index of the item to remove,
     *                  where 0
     *                  indicates the first item in the list
     * @see MutableComboBoxModel
     */
    public void removeItemAt(int anIndex) {
        checkMutableComboBoxModel();
        ((MutableComboBoxModel<E>)dataModel).removeElementAt( anIndex );
    }

    /**
     * Removes all items from the item list.
     */
    public void removeAllItems() {
        checkMutableComboBoxModel();
        MutableComboBoxModel<E> model = (MutableComboBoxModel<E>)dataModel;
        int size = model.getSize();

        if ( model instanceof DefaultComboBoxModel ) {
            ((DefaultComboBoxModel)model).removeAllElements();
        }
        else {
            for ( int i = 0; i < size; ++i ) {
                E element = model.getElementAt( 0 );
                model.removeElement( element );
            }
        }
        selectedItemReminder = null;
        if (isEditable()) {
            editor.setItem(null);
        }
    }

    /**
     * Checks that the <code>dataModel</code> is an instance of
     * <code>MutableComboBoxModel</code>.  If not, it throws an exception.
     * @exception RuntimeException if <code>dataModel</code> is not an
     *          instance of <code>MutableComboBoxModel</code>.
     */
    void checkMutableComboBoxModel() {
        if ( !(dataModel instanceof MutableComboBoxModel) )
            throw new RuntimeException("Cannot use this method with a non-Mutable data model.");
    }

    /**
     * Causes the combo box to display its popup window.
     * @see #setPopupVisible
     */
    public void showPopup() {
        setPopupVisible(true);
    }

    /**
     * Causes the combo box to close its popup window.
     * @see #setPopupVisible
     */
    public void hidePopup() {
        setPopupVisible(false);
    }

    /**
     * Sets the visibility of the popup.
     *
     * @param v if {@code true} shows the popup, otherwise, hides the popup.
     */
    public void setPopupVisible(boolean v) {
        getUI().setPopupVisible(this, v);
    }

    /**
     * Determines the visibility of the popup.
     *
     * @return true if the popup is visible, otherwise returns false
     */
    public boolean isPopupVisible() {
        return getUI().isPopupVisible(this);
    }

    /** Selection **/

    /**
     * Adds an <code>ItemListener</code>.
     * <p>
     * <code>aListener</code> will receive one or two <code>ItemEvent</code>s when
     * the selected item changes.
     *
     * @param aListener the <code>ItemListener</code> that is to be notified
     * @see #setSelectedItem
     */
    public void addItemListener(ItemListener aListener) {
        listenerList.add(ItemListener.class,aListener);
    }

    /** Removes an <code>ItemListener</code>.
     *
     * @param aListener  the <code>ItemListener</code> to remove
     */
    public void removeItemListener(ItemListener aListener) {
        listenerList.remove(ItemListener.class,aListener);
    }

    /**
     * Returns an array of all the <code>ItemListener</code>s added
     * to this JComboBox with addItemListener().
     *
     * @return all of the <code>ItemListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    @BeanProperty(bound = false)
    public ItemListener[] getItemListeners() {
        return listenerList.getListeners(ItemListener.class);
    }

    /**
     * Adds an <code>ActionListener</code>.
     * <p>
     * The <code>ActionListener</code> will receive an <code>ActionEvent</code>
     * when a selection has been made. If the combo box is editable, then
     * an <code>ActionEvent</code> will be fired when editing has stopped.
     *
     * @param l  the <code>ActionListener</code> that is to be notified
     * @see #setSelectedItem
     */
    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class,l);
    }

    /** Removes an <code>ActionListener</code>.
     *
     * @param l  the <code>ActionListener</code> to remove
     */
    public void removeActionListener(ActionListener l) {
        if ((l != null) && (getAction() == l)) {
            setAction(null);
        } else {
            listenerList.remove(ActionListener.class, l);
        }
    }

    /**
     * Returns an array of all the <code>ActionListener</code>s added
     * to this JComboBox with addActionListener().
     *
     * @return all of the <code>ActionListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    @BeanProperty(bound = false)
    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    /**
     * Adds a <code>PopupMenu</code> listener which will listen to notification
     * messages from the popup portion of the combo box.
     * <p>
     * For all standard look and feels shipped with Java, the popup list
     * portion of combo box is implemented as a <code>JPopupMenu</code>.
     * A custom look and feel may not implement it this way and will
     * therefore not receive the notification.
     *
     * @param l  the <code>PopupMenuListener</code> to add
     * @since 1.4
     */
    public void addPopupMenuListener(PopupMenuListener l) {
        listenerList.add(PopupMenuListener.class,l);
    }

    /**
     * Removes a <code>PopupMenuListener</code>.
     *
     * @param l  the <code>PopupMenuListener</code> to remove
     * @see #addPopupMenuListener
     * @since 1.4
     */
    public void removePopupMenuListener(PopupMenuListener l) {
        listenerList.remove(PopupMenuListener.class,l);
    }

    /**
     * Returns an array of all the <code>PopupMenuListener</code>s added
     * to this JComboBox with addPopupMenuListener().
     *
     * @return all of the <code>PopupMenuListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    @BeanProperty(bound = false)
    public PopupMenuListener[] getPopupMenuListeners() {
        return listenerList.getListeners(PopupMenuListener.class);
    }

    /**
     * Notifies <code>PopupMenuListener</code>s that the popup portion of the
     * combo box will become visible.
     * <p>
     * This method is public but should not be called by anything other than
     * the UI delegate.
     * @see #addPopupMenuListener
     * @since 1.4
     */
    public void firePopupMenuWillBecomeVisible() {
        Object[] listeners = listenerList.getListenerList();
        PopupMenuEvent e=null;
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==PopupMenuListener.class) {
                if (e == null)
                    e = new PopupMenuEvent(this);
                ((PopupMenuListener)listeners[i+1]).popupMenuWillBecomeVisible(e);
            }
        }
    }

    /**
     * Notifies <code>PopupMenuListener</code>s that the popup portion of the
     * combo box has become invisible.
     * <p>
     * This method is public but should not be called by anything other than
     * the UI delegate.
     * @see #addPopupMenuListener
     * @since 1.4
     */
    public void firePopupMenuWillBecomeInvisible() {
        Object[] listeners = listenerList.getListenerList();
        PopupMenuEvent e=null;
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==PopupMenuListener.class) {
                if (e == null)
                    e = new PopupMenuEvent(this);
                ((PopupMenuListener)listeners[i+1]).popupMenuWillBecomeInvisible(e);
            }
        }
    }

    /**
     * Notifies <code>PopupMenuListener</code>s that the popup portion of the
     * combo box has been canceled.
     * <p>
     * This method is public but should not be called by anything other than
     * the UI delegate.
     * @see #addPopupMenuListener
     * @since 1.4
     */
    public void firePopupMenuCanceled() {
        Object[] listeners = listenerList.getListenerList();
        PopupMenuEvent e=null;
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==PopupMenuListener.class) {
                if (e == null)
                    e = new PopupMenuEvent(this);
                ((PopupMenuListener)listeners[i+1]).popupMenuCanceled(e);
            }
        }
    }

    /**
     * Sets the action command that should be included in the event
     * sent to action listeners.
     *
     * @param aCommand  a string containing the "command" that is sent
     *                  to action listeners; the same listener can then
     *                  do different things depending on the command it
     *                  receives
     */
    public void setActionCommand(String aCommand) {
        actionCommand = aCommand;
    }

    /**
     * Returns the action command that is included in the event sent to
     * action listeners.
     *
     * @return  the string containing the "command" that is sent
     *          to action listeners.
     */
    public String getActionCommand() {
        return actionCommand;
    }

    private Action action;
    private PropertyChangeListener actionPropertyChangeListener;

    /**
     * Sets the <code>Action</code> for the <code>ActionEvent</code> source.
     * The new <code>Action</code> replaces any previously set
     * <code>Action</code> but does not affect <code>ActionListeners</code>
     * independently added with <code>addActionListener</code>.
     * If the <code>Action</code> is already a registered
     * <code>ActionListener</code> for the <code>ActionEvent</code> source,
     * it is not re-registered.
     * <p>
     * Setting the <code>Action</code> results in immediately changing
     * all the properties described in <a href="Action.html#buttonActions">
     * Swing Components Supporting <code>Action</code></a>.
     * Subsequently, the combobox's properties are automatically updated
     * as the <code>Action</code>'s properties change.
     * <p>
     * This method uses three other methods to set
     * and help track the <code>Action</code>'s property values.
     * It uses the <code>configurePropertiesFromAction</code> method
     * to immediately change the combobox's properties.
     * To track changes in the <code>Action</code>'s property values,
     * this method registers the <code>PropertyChangeListener</code>
     * returned by <code>createActionPropertyChangeListener</code>. The
     * default {@code PropertyChangeListener} invokes the
     * {@code actionPropertyChanged} method when a property in the
     * {@code Action} changes.
     *
     * @param a the <code>Action</code> for the <code>JComboBox</code>,
     *                  or <code>null</code>.
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
     * <code>ActionEvent</code> source, or <code>null</code> if no
     * <code>Action</code> is set.
     *
     * @return the <code>Action</code> for this <code>ActionEvent</code>
     *          source; or <code>null</code>
     * @since 1.3
     * @see Action
     * @see #setAction
     */
    public Action getAction() {
        return action;
    }

    /**
     * Sets the properties on this combobox to match those in the specified
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
     * Creates and returns a <code>PropertyChangeListener</code> that is
     * responsible for listening for changes from the specified
     * <code>Action</code> and updating the appropriate properties.
     * <p>
     * <b>Warning:</b> If you subclass this do not create an anonymous
     * inner class.  If you do the lifetime of the combobox will be tied to
     * that of the <code>Action</code>.
     *
     * @param a the combobox's action
     * @return the {@code PropertyChangeListener}
     * @since 1.3
     * @see Action
     * @see #setAction
     */
    protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
        return new ComboBoxActionPropertyChangeListener(this, a);
    }

    /**
     * Updates the combobox's state in response to property changes in
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
     * @param action the <code>Action</code> associated with this combobox
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
        } else if (Action.SHORT_DESCRIPTION == propertyName) {
            AbstractAction.setToolTipTextFromAction(this, action);
        }
    }

    private void setActionCommandFromAction(Action a) {
        setActionCommand((a != null) ?
                             (String)a.getValue(Action.ACTION_COMMAND_KEY) :
                             null);
    }


    private static class ComboBoxActionPropertyChangeListener
                 extends ActionPropertyChangeListener<JComboBox<?>> {
        ComboBoxActionPropertyChangeListener(JComboBox<?> b, Action a) {
            super(b, a);
        }
        protected void actionPropertyChanged(JComboBox<?> cb,
                                             Action action,
                                             PropertyChangeEvent e) {
            if (AbstractAction.shouldReconfigure(e)) {
                cb.configurePropertiesFromAction(action);
            } else {
                cb.actionPropertyChanged(action, e.getPropertyName());
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.
     * @param e  the event of interest
     *
     * @see EventListenerList
     */
    protected void fireItemStateChanged(ItemEvent e) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for ( int i = listeners.length-2; i>=0; i-=2 ) {
            if ( listeners[i]==ItemListener.class ) {
                // Lazily create the event:
                // if (changeEvent == null)
                // changeEvent = new ChangeEvent(this);
                ((ItemListener)listeners[i+1]).itemStateChanged(e);
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.
     *
     * @see EventListenerList
     */
    @SuppressWarnings("deprecation")
    protected void fireActionEvent() {
        if (!firingActionEvent) {
            // Set flag to ensure that an infinite loop is not created
            firingActionEvent = true;
            ActionEvent e = null;
            // Guaranteed to return a non-null array
            Object[] listeners = listenerList.getListenerList();
            long mostRecentEventTime = EventQueue.getMostRecentEventTime();
            int modifiers = 0;
            AWTEvent currentEvent = EventQueue.getCurrentEvent();
            if (currentEvent instanceof InputEvent) {
                modifiers = ((InputEvent)currentEvent).getModifiers();
            } else if (currentEvent instanceof ActionEvent) {
                modifiers = ((ActionEvent)currentEvent).getModifiers();
            }
            try {
                // Process the listeners last to first, notifying
                // those that are interested in this event
                for ( int i = listeners.length-2; i>=0; i-=2 ) {
                    if ( listeners[i]==ActionListener.class ) {
                        // Lazily create the event:
                        if ( e == null )
                            e = new ActionEvent(this,ActionEvent.ACTION_PERFORMED,
                                                getActionCommand(),
                                                mostRecentEventTime, modifiers);
                        ((ActionListener)listeners[i+1]).actionPerformed(e);
                    }
                }
            } finally {
                firingActionEvent = false;
            }
        }
    }

    /**
     * This protected method is implementation specific. Do not access directly
     * or override.
     */
    protected void selectedItemChanged() {
        if (selectedItemReminder != null ) {
            fireItemStateChanged(new ItemEvent(this,ItemEvent.ITEM_STATE_CHANGED,
                                               selectedItemReminder,
                                               ItemEvent.DESELECTED));
        }

        // set the new selected item.
        selectedItemReminder = dataModel.getSelectedItem();

        if (selectedItemReminder != null ) {
            fireItemStateChanged(new ItemEvent(this,ItemEvent.ITEM_STATE_CHANGED,
                                               selectedItemReminder,
                                               ItemEvent.SELECTED));
        }
    }

    /**
     * Returns an array containing the selected item.
     * This method is implemented for compatibility with
     * <code>ItemSelectable</code>.
     *
     * @return an array of <code>Objects</code> containing one
     *          element -- the selected item
     */
    @BeanProperty(bound = false)
    public Object[] getSelectedObjects() {
        Object selectedObject = getSelectedItem();
        if ( selectedObject == null )
            return new Object[0];
        else {
            Object[] result = new Object[1];
            result[0] = selectedObject;
            return result;
        }
    }

    /**
     * This method is public as an implementation side effect.
     * do not call or override.
     */
    public void actionPerformed(ActionEvent e) {
        setPopupVisible(false);
        getModel().setSelectedItem(getEditor().getItem());
        String oldCommand = getActionCommand();
        setActionCommand("comboBoxEdited");
        fireActionEvent();
        setActionCommand(oldCommand);
    }

    /**
     * This method is public as an implementation side effect.
     * do not call or override.
     */
    public void contentsChanged(ListDataEvent e) {
        Object oldSelection = selectedItemReminder;
        Object newSelection = dataModel.getSelectedItem();
        if (oldSelection == null || !oldSelection.equals(newSelection)) {
            selectedItemChanged();
            if (!selectingItem) {
                fireActionEvent();
            }
        }
    }

    /**
     * This method is public as an implementation side effect.
     * do not call or override.
     */
    public void intervalAdded(ListDataEvent e) {
        if (selectedItemReminder != dataModel.getSelectedItem()) {
            selectedItemChanged();
        }
    }

    /**
     * This method is public as an implementation side effect.
     * do not call or override.
     */
    public void intervalRemoved(ListDataEvent e) {
        contentsChanged(e);
    }

    /**
     * Selects the list item that corresponds to the specified keyboard
     * character and returns true, if there is an item corresponding
     * to that character.  Otherwise, returns false.
     *
     * @param keyChar a char, typically this is a keyboard key
     *                  typed by the user
     * @return {@code true} if there is an item corresponding to that character.
     *         Otherwise, returns {@code false}.
     */
    public boolean selectWithKeyChar(char keyChar) {
        int index;

        if ( keySelectionManager == null )
            keySelectionManager = createDefaultKeySelectionManager();

        index = keySelectionManager.selectionForKey(keyChar,getModel());
        if ( index != -1 ) {
            setSelectedIndex(index);
            return true;
        }
        else
            return false;
    }

    /**
     * Enables the combo box so that items can be selected. When the
     * combo box is disabled, items cannot be selected and values
     * cannot be typed into its field (if it is editable).
     *
     * @param b a boolean value, where true enables the component and
     *          false disables it
     */
    @BeanProperty(preferred = true, description
            = "The enabled state of the component.")
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        firePropertyChange( "enabled", !isEnabled(), isEnabled() );
    }

    /**
     * Initializes the editor with the specified item.
     *
     * @param anEditor the <code>ComboBoxEditor</code> that displays
     *                  the list item in the
     *                  combo box field and allows it to be edited
     * @param anItem   the object to display and edit in the field
     */
    public void configureEditor(ComboBoxEditor anEditor, Object anItem) {
        anEditor.setItem(anItem);
    }

    /**
     * Handles <code>KeyEvent</code>s, looking for the Tab key.
     * If the Tab key is found, the popup window is closed.
     *
     * @param e  the <code>KeyEvent</code> containing the keyboard
     *          key that was pressed
     */
    public void processKeyEvent(KeyEvent e) {
        if ( e.getKeyCode() == KeyEvent.VK_TAB ) {
            hidePopup();
        }
        super.processKeyEvent(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        if (super.processKeyBinding(ks, e, condition, pressed)) {
            return true;
        }

        if (!isEditable() || condition != WHEN_FOCUSED || getEditor() == null
                || !Boolean.TRUE.equals(getClientProperty("JComboBox.isTableCellEditor"))) {
            return false;
        }

        Component editorComponent = getEditor().getEditorComponent();
        if (editorComponent instanceof JComponent) {
            JComponent component = (JComponent) editorComponent;
            return component.processKeyBinding(ks, e, WHEN_FOCUSED, pressed);
        }
        return false;
    }

    /**
     * Sets the object that translates a keyboard character into a list
     * selection. Typically, the first selection with a matching first
     * character becomes the selected item.
     *
     * @param aManager a key selection manager
     */
    @BeanProperty(bound = false, expert = true, description
            = "The objects that changes the selection when a key is pressed.")
    public void setKeySelectionManager(KeySelectionManager aManager) {
        keySelectionManager = aManager;
    }

    /**
     * Returns the list's key-selection manager.
     *
     * @return the <code>KeySelectionManager</code> currently in use
     */
    public KeySelectionManager getKeySelectionManager() {
        return keySelectionManager;
    }

    /* Accessing the model */
    /**
     * Returns the number of items in the list.
     *
     * @return an integer equal to the number of items in the list
     */
    @BeanProperty(bound = false)
    public int getItemCount() {
        return dataModel.getSize();
    }

    /**
     * Returns the list item at the specified index.  If <code>index</code>
     * is out of range (less than zero or greater than or equal to size)
     * it will return <code>null</code>.
     *
     * @param index  an integer indicating the list position, where the first
     *               item starts at zero
     * @return the item at that list position; or
     *                  <code>null</code> if out of range
     */
    public E getItemAt(int index) {
        return dataModel.getElementAt(index);
    }

    /**
     * Returns an instance of the default key-selection manager.
     *
     * @return the <code>KeySelectionManager</code> currently used by the list
     * @see #setKeySelectionManager
     */
    protected KeySelectionManager createDefaultKeySelectionManager() {
        return new DefaultKeySelectionManager();
    }


    /**
     * The interface that defines a <code>KeySelectionManager</code>.
     * To qualify as a <code>KeySelectionManager</code>,
     * the class needs to implement the method
     * that identifies the list index given a character and the
     * combo box data model.
     */
    public interface KeySelectionManager {
        /** Given <code>aKey</code> and the model, returns the row
         *  that should become selected. Return -1 if no match was
         *  found.
         *
         * @param  aKey  a char value, usually indicating a keyboard key that
         *               was pressed
         * @param aModel a ComboBoxModel -- the component's data model, containing
         *               the list of selectable items
         * @return an int equal to the selected row, where 0 is the
         *         first item and -1 is none.
         */
        int selectionForKey(char aKey,ComboBoxModel<?> aModel);
    }

    class DefaultKeySelectionManager implements KeySelectionManager, Serializable {
        public int selectionForKey(char aKey,ComboBoxModel<?> aModel) {
            int i,c;
            int currentSelection = -1;
            Object selectedItem = aModel.getSelectedItem();
            String v;
            String pattern;

            if ( selectedItem != null ) {
                for ( i=0,c=aModel.getSize();i<c;i++ ) {
                    if ( selectedItem == aModel.getElementAt(i) ) {
                        currentSelection  =  i;
                        break;
                    }
                }
            }

            pattern = ("" + aKey).toLowerCase();
            aKey = pattern.charAt(0);

            for ( i = ++currentSelection, c = aModel.getSize() ; i < c ; i++ ) {
                Object elem = aModel.getElementAt(i);
                if (elem != null && elem.toString() != null) {
                    v = elem.toString().toLowerCase();
                    if ( v.length() > 0 && v.charAt(0) == aKey )
                        return i;
                }
            }

            for ( i = 0 ; i < currentSelection ; i ++ ) {
                Object elem = aModel.getElementAt(i);
                if (elem != null && elem.toString() != null) {
                    v = elem.toString().toLowerCase();
                    if ( v.length() > 0 && v.charAt(0) == aKey )
                        return i;
                }
            }
            return -1;
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
     * Returns a string representation of this <code>JComboBox</code>.
     * This method is intended to be used only for debugging purposes,
     * and the content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this <code>JComboBox</code>
     */
    protected String paramString() {
        String selectedItemReminderString = (selectedItemReminder != null ?
                                             selectedItemReminder.toString() :
                                             "");
        String isEditableString = (isEditable ? "true" : "false");
        String lightWeightPopupEnabledString = (lightWeightPopupEnabled ?
                                                "true" : "false");

        return super.paramString() +
        ",isEditable=" + isEditableString +
        ",lightWeightPopupEnabled=" + lightWeightPopupEnabledString +
        ",maximumRowCount=" + maximumRowCount +
        ",selectedItemReminder=" + selectedItemReminderString;
    }


///////////////////
// Accessibility support
///////////////////

    /**
     * Gets the AccessibleContext associated with this JComboBox.
     * For combo boxes, the AccessibleContext takes the form of an
     * AccessibleJComboBox.
     * A new AccessibleJComboBox instance is created if necessary.
     *
     * @return an AccessibleJComboBox that serves as the
     *         AccessibleContext of this JComboBox
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if ( accessibleContext == null ) {
            accessibleContext = new AccessibleJComboBox();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JComboBox</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to Combo Box user-interface elements.
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
    protected class AccessibleJComboBox extends AccessibleJComponent
    implements AccessibleAction, AccessibleSelection {


        private JList<?> popupList; // combo box popup list
        private Accessible previousSelectedAccessible = null;

        /**
         * Returns an AccessibleJComboBox instance
         * @since 1.4
         */
        public AccessibleJComboBox() {
            // set the combo box editor's accessible name and description
            JComboBox.this.addPropertyChangeListener(new AccessibleJComboBoxPropertyChangeListener());
            setEditorNameAndDescription();

            // Get the popup list
            Accessible a = getUI().getAccessibleChild(JComboBox.this, 0);
            if (a instanceof javax.swing.plaf.basic.ComboPopup) {
                // Listen for changes to the popup menu selection.
                popupList = ((javax.swing.plaf.basic.ComboPopup)a).getList();
                popupList.addListSelectionListener(
                    new AccessibleJComboBoxListSelectionListener());
            }
            // Listen for popup menu show/hide events
            JComboBox.this.addPopupMenuListener(
              new AccessibleJComboBoxPopupMenuListener());
        }

        /*
         * JComboBox PropertyChangeListener
         */
        private class AccessibleJComboBoxPropertyChangeListener
            implements PropertyChangeListener {

            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName() == "editor") {
                    // set the combo box editor's accessible name
                    // and description
                    setEditorNameAndDescription();
                }
            }
        }

        /*
         * Sets the combo box editor's accessible name and descripton
         */
        private void setEditorNameAndDescription() {
            ComboBoxEditor editor = JComboBox.this.getEditor();
            if (editor != null) {
                Component comp = editor.getEditorComponent();
                if (comp instanceof Accessible) {
                    AccessibleContext ac = comp.getAccessibleContext();
                    if (ac != null) { // may be null
                        ac.setAccessibleName(getAccessibleName());
                        ac.setAccessibleDescription(getAccessibleDescription());
                    }
                }
            }
        }

        /*
         * Listener for combo box popup menu
         * TIGER - 4669379 4894434
         */
        private class AccessibleJComboBoxPopupMenuListener
            implements PopupMenuListener {

            /**
             *  This method is called before the popup menu becomes visible
             */
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // save the initial selection
                if (popupList == null) {
                    return;
                }
                int selectedIndex = popupList.getSelectedIndex();
                if (selectedIndex < 0) {
                    return;
                }
                previousSelectedAccessible =
                    popupList.getAccessibleContext().getAccessibleChild(selectedIndex);
            }

            /**
             * This method is called before the popup menu becomes invisible
             * Note that a JPopupMenu can become invisible any time
             */
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                // ignore
            }

            /**
             * This method is called when the popup menu is canceled
             */
            public void popupMenuCanceled(PopupMenuEvent e) {
                // ignore
            }
        }

        /*
         * Handles changes to the popup list selection.
         * TIGER - 4669379 4894434 4933143
         */
        private class AccessibleJComboBoxListSelectionListener
            implements ListSelectionListener {

            public void valueChanged(ListSelectionEvent e) {
                if (popupList == null) {
                    return;
                }

                // Get the selected popup list item.
                int selectedIndex = popupList.getSelectedIndex();
                if (selectedIndex < 0) {
                    return;
                }
                Accessible selectedAccessible =
                    popupList.getAccessibleContext().getAccessibleChild(selectedIndex);
                if (selectedAccessible == null) {
                    return;
                }

                // Fire a FOCUSED lost PropertyChangeEvent for the
                // previously selected list item.
                PropertyChangeEvent pce;

                if (previousSelectedAccessible != null) {
                    pce = new PropertyChangeEvent(previousSelectedAccessible,
                        AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                        AccessibleState.FOCUSED, null);
                    firePropertyChange(AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                       null, pce);
                }
                // Fire a FOCUSED gained PropertyChangeEvent for the
                // currently selected list item.
                pce = new PropertyChangeEvent(selectedAccessible,
                    AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                    null, AccessibleState.FOCUSED);
                firePropertyChange(AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                                   null, pce);

                // Fire the ACCESSIBLE_ACTIVE_DESCENDANT_PROPERTY event
                // for the combo box.
                firePropertyChange(AccessibleContext.ACCESSIBLE_ACTIVE_DESCENDANT_PROPERTY,
                                   previousSelectedAccessible, selectedAccessible);

                // Save the previous selection.
                previousSelectedAccessible = selectedAccessible;
            }
        }


        /**
         * Returns the number of accessible children in the object.  If all
         * of the children of this object implement Accessible, than this
         * method should return the number of children of this object.
         *
         * @return the number of accessible children in the object.
         */
        public int getAccessibleChildrenCount() {
            // Always delegate to the UI if it exists
            if (ui != null) {
                return ui.getAccessibleChildrenCount(JComboBox.this);
            } else {
                return super.getAccessibleChildrenCount();
            }
        }

        /**
         * Returns the nth Accessible child of the object.
         * The child at index zero represents the popup.
         * If the combo box is editable, the child at index one
         * represents the editor.
         *
         * @param i zero-based index of child
         * @return the nth Accessible child of the object
         */
        public Accessible getAccessibleChild(int i) {
            // Always delegate to the UI if it exists
            if (ui != null) {
                return ui.getAccessibleChild(JComboBox.this, i);
            } else {
               return super.getAccessibleChild(i);
            }
        }

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.COMBO_BOX;
        }

        /**
         * Gets the state set of this object.  The AccessibleStateSet of
         * an object is composed of a set of unique AccessibleStates.
         * A change in the AccessibleStateSet of an object will cause a
         * PropertyChangeEvent to be fired for the ACCESSIBLE_STATE_PROPERTY
         * property.
         *
         * @return an instance of AccessibleStateSet containing the
         * current state set of the object
         * @see AccessibleStateSet
         * @see AccessibleState
         * @see #addPropertyChangeListener
         *
         */
        public AccessibleStateSet getAccessibleStateSet() {
            // TIGER - 4489748
            AccessibleStateSet ass = super.getAccessibleStateSet();
            if (ass == null) {
                ass = new AccessibleStateSet();
            }
            if (JComboBox.this.isPopupVisible()) {
                ass.add(AccessibleState.EXPANDED);
            } else {
                ass.add(AccessibleState.COLLAPSED);
            }
            return ass;
        }

        /**
         * Get the AccessibleAction associated with this object.  In the
         * implementation of the Java Accessibility API for this class,
         * return this object, which is responsible for implementing the
         * AccessibleAction interface on behalf of itself.
         *
         * @return this object
         */
        public AccessibleAction getAccessibleAction() {
            return this;
        }

        /**
         * Return a description of the specified action of the object.
         *
         * @param i zero-based index of the actions
         */
        public String getAccessibleActionDescription(int i) {
            if (i == 0) {
                return UIManager.getString("ComboBox.togglePopupText");
            }
            else {
                return null;
            }
        }

        /**
         * Returns the number of Actions available in this object.  The
         * default behavior of a combo box is to have one action.
         *
         * @return 1, the number of Actions in this object
         */
        public int getAccessibleActionCount() {
            return 1;
        }

        /**
         * Perform the specified Action on the object
         *
         * @param i zero-based index of actions
         * @return true if the action was performed; else false.
         */
        public boolean doAccessibleAction(int i) {
            if (i == 0) {
                setPopupVisible(!isPopupVisible());
                return true;
            }
            else {
                return false;
            }
        }


        /**
         * Get the AccessibleSelection associated with this object.  In the
         * implementation of the Java Accessibility API for this class,
         * return this object, which is responsible for implementing the
         * AccessibleSelection interface on behalf of itself.
         *
         * @return this object
         */
        public AccessibleSelection getAccessibleSelection() {
            return this;
        }

        /**
         * Returns the number of Accessible children currently selected.
         * If no children are selected, the return value will be 0.
         *
         * @return the number of items currently selected.
         * @since 1.3
         */
        public int getAccessibleSelectionCount() {
            Object o = JComboBox.this.getSelectedItem();
            if (o != null) {
                return 1;
            } else {
                return 0;
            }
        }

        /**
         * Returns an Accessible representing the specified selected child
         * in the popup.  If there isn't a selection, or there are
         * fewer children selected than the integer passed in, the return
         * value will be null.
         * <p>Note that the index represents the i-th selected child, which
         * is different from the i-th child.
         *
         * @param i the zero-based index of selected children
         * @return the i-th selected child
         * @see #getAccessibleSelectionCount
         * @since 1.3
         */
        public Accessible getAccessibleSelection(int i) {
            // Get the popup
            Accessible a =
                JComboBox.this.getUI().getAccessibleChild(JComboBox.this, 0);
            if (a != null &&
                a instanceof javax.swing.plaf.basic.ComboPopup) {

                // get the popup list
                JList<?> list = ((javax.swing.plaf.basic.ComboPopup)a).getList();

                // return the i-th selection in the popup list
                AccessibleContext ac = list.getAccessibleContext();
                if (ac != null) {
                    AccessibleSelection as = ac.getAccessibleSelection();
                    if (as != null) {
                        return as.getAccessibleSelection(i);
                    }
                }
            }
            return null;
        }

        /**
         * Determines if the current child of this object is selected.
         *
         * @return true if the current child of this object is selected;
         *              else false
         * @param i the zero-based index of the child in this Accessible
         * object.
         * @see AccessibleContext#getAccessibleChild
         * @since 1.3
         */
        public boolean isAccessibleChildSelected(int i) {
            return JComboBox.this.getSelectedIndex() == i;
        }

        /**
         * Adds the specified Accessible child of the object to the object's
         * selection.  If the object supports multiple selections,
         * the specified child is added to any existing selection, otherwise
         * it replaces any existing selection in the object.  If the
         * specified child is already selected, this method has no effect.
         *
         * @param i the zero-based index of the child
         * @see AccessibleContext#getAccessibleChild
         * @since 1.3
         */
        public void addAccessibleSelection(int i) {
            // TIGER - 4856195
            clearAccessibleSelection();
            JComboBox.this.setSelectedIndex(i);
        }

        /**
         * Removes the specified child of the object from the object's
         * selection.  If the specified item isn't currently selected, this
         * method has no effect.
         *
         * @param i the zero-based index of the child
         * @see AccessibleContext#getAccessibleChild
         * @since 1.3
         */
        public void removeAccessibleSelection(int i) {
            if (JComboBox.this.getSelectedIndex() == i) {
                clearAccessibleSelection();
            }
        }

        /**
         * Clears the selection in the object, so that no children in the
         * object are selected.
         * @since 1.3
         */
        public void clearAccessibleSelection() {
            JComboBox.this.setSelectedIndex(-1);
        }

        /**
         * Causes every child of the object to be selected
         * if the object supports multiple selections.
         * @since 1.3
         */
        public void selectAllAccessibleSelection() {
            // do nothing since multiple selection is not supported
        }

//        public Accessible getAccessibleAt(Point p) {
//            Accessible a = getAccessibleChild(1);
//            if ( a != null ) {
//                return a; // the editor
//            }
//            else {
//                return getAccessibleChild(0); // the list
//            }
//        }
        private EditorAccessibleContext editorAccessibleContext = null;

        private class AccessibleEditor implements Accessible {
            public AccessibleContext getAccessibleContext() {
                if (editorAccessibleContext == null) {
                    Component c = JComboBox.this.getEditor().getEditorComponent();
                    if (c instanceof Accessible) {
                        editorAccessibleContext =
                            new EditorAccessibleContext((Accessible)c);
                    }
                }
                return editorAccessibleContext;
            }
        }

        /*
         * Wrapper class for the AccessibleContext implemented by the
         * combo box editor.  Delegates all method calls except
         * getAccessibleIndexInParent to the editor.  The
         * getAccessibleIndexInParent method returns the selected
         * index in the combo box.
         */
        private class EditorAccessibleContext extends AccessibleContext {

            private AccessibleContext ac;

            private EditorAccessibleContext() {
            }

            /*
             * @param a the AccessibleContext implemented by the
             * combo box editor
             */
            EditorAccessibleContext(Accessible a) {
                this.ac = a.getAccessibleContext();
            }

            /**
             * Gets the accessibleName property of this object.  The accessibleName
             * property of an object is a localized String that designates the purpose
             * of the object.  For example, the accessibleName property of a label
             * or button might be the text of the label or button itself.  In the
             * case of an object that doesn't display its name, the accessibleName
             * should still be set.  For example, in the case of a text field used
             * to enter the name of a city, the accessibleName for the en_US locale
             * could be 'city.'
             *
             * @return the localized name of the object; null if this
             * object does not have a name
             *
             * @see #setAccessibleName
             */
            public String getAccessibleName() {
                return ac.getAccessibleName();
            }

            /**
             * Sets the localized accessible name of this object.  Changing the
             * name will cause a PropertyChangeEvent to be fired for the
             * ACCESSIBLE_NAME_PROPERTY property.
             *
             * @param s the new localized name of the object.
             *
             * @see #getAccessibleName
             * @see #addPropertyChangeListener
             */
            @BeanProperty(preferred = true, description
                    = "Sets the accessible name for the component.")
            public void setAccessibleName(String s) {
                ac.setAccessibleName(s);
            }

            /**
             * Gets the accessibleDescription property of this object.  The
             * accessibleDescription property of this object is a short localized
             * phrase describing the purpose of the object.  For example, in the
             * case of a 'Cancel' button, the accessibleDescription could be
             * 'Ignore changes and close dialog box.'
             *
             * @return the localized description of the object; null if
             * this object does not have a description
             *
             * @see #setAccessibleDescription
             */
            public String getAccessibleDescription() {
                return ac.getAccessibleDescription();
            }

            /**
             * Sets the accessible description of this object.  Changing the
             * name will cause a PropertyChangeEvent to be fired for the
             * ACCESSIBLE_DESCRIPTION_PROPERTY property.
             *
             * @param s the new localized description of the object
             *
             * @see #setAccessibleName
             * @see #addPropertyChangeListener
             */
            @BeanProperty(preferred = true, description
                    = "Sets the accessible description for the component.")
            public void setAccessibleDescription(String s) {
                ac.setAccessibleDescription(s);
            }

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
                return ac.getAccessibleRole();
            }

            /**
             * Gets the state set of this object.  The AccessibleStateSet of an object
             * is composed of a set of unique AccessibleStates.  A change in the
             * AccessibleStateSet of an object will cause a PropertyChangeEvent to
             * be fired for the ACCESSIBLE_STATE_PROPERTY property.
             *
             * @return an instance of AccessibleStateSet containing the
             * current state set of the object
             * @see AccessibleStateSet
             * @see AccessibleState
             * @see #addPropertyChangeListener
             */
            public AccessibleStateSet getAccessibleStateSet() {
                return ac.getAccessibleStateSet();
            }

            /**
             * Gets the Accessible parent of this object.
             *
             * @return the Accessible parent of this object; null if this
             * object does not have an Accessible parent
             */
            public Accessible getAccessibleParent() {
                return ac.getAccessibleParent();
            }

            /**
             * Sets the Accessible parent of this object.  This is meant to be used
             * only in the situations where the actual component's parent should
             * not be treated as the component's accessible parent and is a method
             * that should only be called by the parent of the accessible child.
             *
             * @param a - Accessible to be set as the parent
             */
            public void setAccessibleParent(Accessible a) {
                ac.setAccessibleParent(a);
            }

            /**
             * Gets the 0-based index of this object in its accessible parent.
             *
             * @return the 0-based index of this object in its parent; -1 if this
             * object does not have an accessible parent.
             *
             * @see #getAccessibleParent
             * @see #getAccessibleChildrenCount
             * @see #getAccessibleChild
             */
            public int getAccessibleIndexInParent() {
                return JComboBox.this.getSelectedIndex();
            }

            /**
             * Returns the number of accessible children of the object.
             *
             * @return the number of accessible children of the object.
             */
            public int getAccessibleChildrenCount() {
                return ac.getAccessibleChildrenCount();
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
                return ac.getAccessibleChild(i);
            }

            /**
             * Gets the locale of the component. If the component does not have a
             * locale, then the locale of its parent is returned.
             *
             * @return this component's locale.  If this component does not have
             * a locale, the locale of its parent is returned.
             *
             * @exception IllegalComponentStateException
             * If the Component does not have its own locale and has not yet been
             * added to a containment hierarchy such that the locale can be
             * determined from the containing parent.
             */
            public Locale getLocale() throws IllegalComponentStateException {
                return ac.getLocale();
            }

            /**
             * Adds a PropertyChangeListener to the listener list.
             * The listener is registered for all Accessible properties and will
             * be called when those properties change.
             *
             * @see #ACCESSIBLE_NAME_PROPERTY
             * @see #ACCESSIBLE_DESCRIPTION_PROPERTY
             * @see #ACCESSIBLE_STATE_PROPERTY
             * @see #ACCESSIBLE_VALUE_PROPERTY
             * @see #ACCESSIBLE_SELECTION_PROPERTY
             * @see #ACCESSIBLE_TEXT_PROPERTY
             * @see #ACCESSIBLE_VISIBLE_DATA_PROPERTY
             *
             * @param listener  The PropertyChangeListener to be added
             */
            public void addPropertyChangeListener(PropertyChangeListener listener) {
                ac.addPropertyChangeListener(listener);
            }

            /**
             * Removes a PropertyChangeListener from the listener list.
             * This removes a PropertyChangeListener that was registered
             * for all properties.
             *
             * @param listener  The PropertyChangeListener to be removed
             */
            public void removePropertyChangeListener(PropertyChangeListener listener) {
                ac.removePropertyChangeListener(listener);
            }

            /**
             * Gets the AccessibleAction associated with this object that supports
             * one or more actions.
             *
             * @return AccessibleAction if supported by object; else return null
             * @see AccessibleAction
             */
            public AccessibleAction getAccessibleAction() {
                return ac.getAccessibleAction();
            }

            /**
             * Gets the AccessibleComponent associated with this object that has a
             * graphical representation.
             *
             * @return AccessibleComponent if supported by object; else return null
             * @see AccessibleComponent
             */
            public AccessibleComponent getAccessibleComponent() {
                return ac.getAccessibleComponent();
            }

            /**
             * Gets the AccessibleSelection associated with this object which allows its
             * Accessible children to be selected.
             *
             * @return AccessibleSelection if supported by object; else return null
             * @see AccessibleSelection
             */
            public AccessibleSelection getAccessibleSelection() {
                return ac.getAccessibleSelection();
            }

            /**
             * Gets the AccessibleText associated with this object presenting
             * text on the display.
             *
             * @return AccessibleText if supported by object; else return null
             * @see AccessibleText
             */
            public AccessibleText getAccessibleText() {
                return ac.getAccessibleText();
            }

            /**
             * Gets the AccessibleEditableText associated with this object
             * presenting editable text on the display.
             *
             * @return AccessibleEditableText if supported by object; else return null
             * @see AccessibleEditableText
             */
            public AccessibleEditableText getAccessibleEditableText() {
                return ac.getAccessibleEditableText();
            }

            /**
             * Gets the AccessibleValue associated with this object that supports a
             * Numerical value.
             *
             * @return AccessibleValue if supported by object; else return null
             * @see AccessibleValue
             */
            public AccessibleValue getAccessibleValue() {
                return ac.getAccessibleValue();
            }

            /**
             * Gets the AccessibleIcons associated with an object that has
             * one or more associated icons
             *
             * @return an array of AccessibleIcon if supported by object;
             * otherwise return null
             * @see AccessibleIcon
             */
            public AccessibleIcon [] getAccessibleIcon() {
                return ac.getAccessibleIcon();
            }

            /**
             * Gets the AccessibleRelationSet associated with an object
             *
             * @return an AccessibleRelationSet if supported by object;
             * otherwise return null
             * @see AccessibleRelationSet
             */
            public AccessibleRelationSet getAccessibleRelationSet() {
                return ac.getAccessibleRelationSet();
            }

            /**
             * Gets the AccessibleTable associated with an object
             *
             * @return an AccessibleTable if supported by object;
             * otherwise return null
             * @see AccessibleTable
             */
            public AccessibleTable getAccessibleTable() {
                return ac.getAccessibleTable();
            }

            /**
             * Support for reporting bound property changes.  If oldValue and
             * newValue are not equal and the PropertyChangeEvent listener list
             * is not empty, then fire a PropertyChange event to each listener.
             * In general, this is for use by the Accessible objects themselves
             * and should not be called by an application program.
             * @param propertyName  The programmatic name of the property that
             * was changed.
             * @param oldValue  The old value of the property.
             * @param newValue  The new value of the property.
             * @see java.beans.PropertyChangeSupport
             * @see #addPropertyChangeListener
             * @see #removePropertyChangeListener
             * @see #ACCESSIBLE_NAME_PROPERTY
             * @see #ACCESSIBLE_DESCRIPTION_PROPERTY
             * @see #ACCESSIBLE_STATE_PROPERTY
             * @see #ACCESSIBLE_VALUE_PROPERTY
             * @see #ACCESSIBLE_SELECTION_PROPERTY
             * @see #ACCESSIBLE_TEXT_PROPERTY
             * @see #ACCESSIBLE_VISIBLE_DATA_PROPERTY
             */
            public void firePropertyChange(String propertyName,
                                           Object oldValue,
                                           Object newValue) {
                ac.firePropertyChange(propertyName, oldValue, newValue);
            }
        }

    } // innerclass AccessibleJComboBox
}
