/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.accessibility;

import java.awt.IllegalComponentStateException;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Locale;

import sun.awt.AWTAccessor;
import sun.awt.AppContext;

/**
 * {@code AccessibleContext} represents the minimum information all accessible
 * objects return. This information includes the accessible name, description,
 * role, and state of the object, as well as information about its parent and
 * children. {@code AccessibleContext} also contains methods for obtaining more
 * specific accessibility information about a component. If the component
 * supports them, these methods will return an object that implements one or
 * more of the following interfaces:
 * <ul>
 *   <li>{@link AccessibleAction} - the object can perform one or more actions.
 *   This interface provides the standard mechanism for an assistive technology
 *   to determine what those actions are and tell the object to perform them.
 *   Any object that can be manipulated should support this interface.
 *   <li>{@link AccessibleComponent} - the object has a graphical
 *   representation. This interface provides the standard mechanism for an
 *   assistive technology to determine and set the graphical representation of
 *   the object. Any object that is rendered on the screen should support this
 *   interface.
 *   <li>{@link AccessibleSelection} - the object allows its children to be
 *   selected. This interface provides the standard mechanism for an assistive
 *   technology to determine the currently selected children of the object as
 *   well as modify its selection set. Any object that has children that can be
 *   selected should support this interface.
 *   <li>{@link AccessibleText} - the object presents editable textual
 *   information on the display. This interface provides the standard mechanism
 *   for an assistive technology to access that text via its content,
 *   attributes, and spatial location. Any object that contains editable text
 *   should support this interface.
 *   <li>{@link AccessibleValue} - the object supports a numerical value. This
 *   interface provides the standard mechanism for an assistive technology to
 *   determine and set the current value of the object, as well as obtain its
 *   minimum and maximum values. Any object that supports a numerical value
 *   should support this interface.
 * </ul>
 *
 * @author Peter Korn
 * @author Hans Muller
 * @author Willie Walker
 * @author Lynn Monsanto
 */
@JavaBean(description = "Minimal information that all accessible objects return")
public abstract class AccessibleContext {

    /**
     * Constructor for subclasses to call.
     */
    protected AccessibleContext() {}

    /**
     * The {@code AppContext} that should be used to dispatch events for this
     * {@code AccessibleContext}.
     */
    private volatile AppContext targetAppContext;

    static {
        AWTAccessor.setAccessibleContextAccessor(new AWTAccessor.AccessibleContextAccessor() {
            @Override
            public void setAppContext(AccessibleContext accessibleContext, AppContext appContext) {
                accessibleContext.targetAppContext = appContext;
            }

            @Override
            public AppContext getAppContext(AccessibleContext accessibleContext) {
                return accessibleContext.targetAppContext;
            }

            @Override
            public Object getNativeAXResource(AccessibleContext accessibleContext) {
                return accessibleContext.nativeAXResource;
            }

            @Override
            public void setNativeAXResource(AccessibleContext accessibleContext, Object value) {
                accessibleContext.nativeAXResource = value;
            }
        });
    }

    /**
     * Constant used to determine when the {@link #accessibleName} property has
     * changed. The old value in the {@code PropertyChangeEvent} will be the old
     * {@code accessibleName} and the new value will be the new
     * {@code accessibleName}.
     *
     * @see #getAccessibleName
     * @see #addPropertyChangeListener
     */
    public static final String ACCESSIBLE_NAME_PROPERTY = "AccessibleName";

    /**
     * Constant used to determine when the {@link #accessibleDescription}
     * property has changed. The old value in the {@code PropertyChangeEvent}
     * will be the old {@code accessibleDescription} and the new value will be
     * the new {@code accessibleDescription}.
     *
     * @see #getAccessibleDescription
     * @see #addPropertyChangeListener
     */
    public static final String ACCESSIBLE_DESCRIPTION_PROPERTY = "AccessibleDescription";

    /**
     * Constant used to determine when the {@code accessibleStateSet} property
     * has changed. The old value will be the old {@code AccessibleState} and
     * the new value will be the new {@code AccessibleState} in the
     * {@code accessibleStateSet}. For example, if a component that supports the
     * vertical and horizontal states changes its orientation from vertical to
     * horizontal, the old value will be {@code AccessibleState.VERTICAL} and
     * the new value will be {@code AccessibleState.HORIZONTAL}. Please note
     * that either value can also be {@code null}. For example, when a component
     * changes from being enabled to disabled, the old value will be
     * {@code AccessibleState.ENABLED} and the new value will be {@code null}.
     *
     * @see #getAccessibleStateSet
     * @see AccessibleState
     * @see AccessibleStateSet
     * @see #addPropertyChangeListener
     */
    public static final String ACCESSIBLE_STATE_PROPERTY = "AccessibleState";

    /**
     * Constant used to determine when the {@code accessibleValue} property has
     * changed. The old value in the {@code PropertyChangeEvent} will be a
     * {@code Number} representing the old value and the new value will be a
     * {@code Number} representing the new value.
     *
     * @see #getAccessibleValue
     * @see #addPropertyChangeListener
     */
    public static final String ACCESSIBLE_VALUE_PROPERTY = "AccessibleValue";

    /**
     * Constant used to determine when the {@code accessibleSelection} has
     * changed. The old and new values in the {@code PropertyChangeEvent} are
     * currently reserved for future use.
     *
     * @see #getAccessibleSelection
     * @see #addPropertyChangeListener
     */
    public static final String ACCESSIBLE_SELECTION_PROPERTY = "AccessibleSelection";

    /**
     * Constant used to determine when the {@code accessibleText} caret has
     * changed. The old value in the {@code PropertyChangeEvent} will be an
     * integer representing the old caret position, and the new value will be an
     * integer representing the new/current caret position.
     *
     * @see #addPropertyChangeListener
     */
    public static final String ACCESSIBLE_CARET_PROPERTY = "AccessibleCaret";

    /**
     * Constant used to determine when the visual appearance of the object has
     * changed. The old and new values in the {@code PropertyChangeEvent} are
     * currently reserved for future use.
     *
     * @see #addPropertyChangeListener
     */
    public static final String ACCESSIBLE_VISIBLE_DATA_PROPERTY = "AccessibleVisibleData";

    /**
     * Constant used to determine when {@code Accessible} children are
     * added/removed from the object. If an {@code Accessible} child is being
     * added, the old value will be {@code null} and the new value will be the
     * {@code Accessible} child. If an {@code Accessible} child is being
     * removed, the old value will be the {@code Accessible} child, and the new
     * value will be {@code null}.
     *
     * @see #addPropertyChangeListener
     */
    public static final String ACCESSIBLE_CHILD_PROPERTY = "AccessibleChild";

    /**
     * Constant used to determine when the active descendant of a component has
     * changed. The active descendant is used for objects such as list, tree,
     * and table, which may have transient children. When the active descendant
     * has changed, the old value of the property change event will be the
     * {@code Accessible} representing the previous active child, and the new
     * value will be the {@code Accessible} representing the current active
     * child.
     *
     * @see #addPropertyChangeListener
     */
    public static final String ACCESSIBLE_ACTIVE_DESCENDANT_PROPERTY = "AccessibleActiveDescendant";

    /**
     * Constant used to indicate that the table caption has changed. The old
     * value in the {@code PropertyChangeEvent} will be an {@code Accessible}
     * representing the previous table caption and the new value will be an
     * {@code Accessible} representing the new table caption.
     *
     * @see Accessible
     * @see AccessibleTable
     */
    public static final String ACCESSIBLE_TABLE_CAPTION_CHANGED =
        "accessibleTableCaptionChanged";

    /**
     * Constant used to indicate that the table summary has changed. The old
     * value in the {@code PropertyChangeEvent} will be an {@code Accessible}
     * representing the previous table summary and the new value will be an
     * {@code Accessible} representing the new table summary.
     *
     * @see Accessible
     * @see AccessibleTable
     */
    public static final String ACCESSIBLE_TABLE_SUMMARY_CHANGED =
        "accessibleTableSummaryChanged";

    /**
     * Constant used to indicate that table data has changed. The old value in
     * the {@code PropertyChangeEvent} will be {@code null} and the new value
     * will be an {@code AccessibleTableModelChange} representing the table
     * change.
     *
     * @see AccessibleTable
     * @see AccessibleTableModelChange
     */
    public static final String ACCESSIBLE_TABLE_MODEL_CHANGED =
        "accessibleTableModelChanged";

    /**
     * Constant used to indicate that the row header has changed. The old value
     * in the {@code PropertyChangeEvent} will be {@code null} and the new value
     * will be an {@code AccessibleTableModelChange} representing the header
     * change.
     *
     * @see AccessibleTable
     * @see AccessibleTableModelChange
     */
    public static final String ACCESSIBLE_TABLE_ROW_HEADER_CHANGED =
        "accessibleTableRowHeaderChanged";

    /**
     * Constant used to indicate that the row description has changed. The old
     * value in the {@code PropertyChangeEvent} will be {@code null} and the new
     * value will be an {@code Integer} representing the row index.
     *
     * @see AccessibleTable
     */
    public static final String ACCESSIBLE_TABLE_ROW_DESCRIPTION_CHANGED =
        "accessibleTableRowDescriptionChanged";

    /**
     * Constant used to indicate that the column header has changed. The old
     * value in the {@code PropertyChangeEvent} will be {@code null} and the new
     * value will be an {@code AccessibleTableModelChange} representing the
     * header change.
     *
     * @see AccessibleTable
     * @see AccessibleTableModelChange
     */
    public static final String ACCESSIBLE_TABLE_COLUMN_HEADER_CHANGED =
        "accessibleTableColumnHeaderChanged";

    /**
     * Constant used to indicate that the column description has changed. The
     * old value in the {@code PropertyChangeEvent} will be {@code null} and the
     * new value will be an {@code Integer} representing the column index.
     *
     * @see AccessibleTable
     */
    public static final String ACCESSIBLE_TABLE_COLUMN_DESCRIPTION_CHANGED =
        "accessibleTableColumnDescriptionChanged";

    /**
     * Constant used to indicate that the supported set of actions has changed.
     * The old value in the {@code PropertyChangeEvent} will be an
     * {@code Integer} representing the old number of actions supported and the
     * new value will be an {@code Integer} representing the new number of
     * actions supported.
     *
     * @see AccessibleAction
     */
    public static final String ACCESSIBLE_ACTION_PROPERTY =
        "accessibleActionProperty";

    /**
     * Constant used to indicate that a hypertext element has received focus.
     * The old value in the {@code PropertyChangeEvent} will be an
     * {@code Integer} representing the start index in the document of the
     * previous element that had focus and the new value will be an
     * {@code Integer} representing the start index in the document of the
     * current element that has focus. A value of -1 indicates that an element
     * does not or did not have focus.
     *
     * @see AccessibleHyperlink
     */
    public static final String ACCESSIBLE_HYPERTEXT_OFFSET =
        "AccessibleHypertextOffset";

    /**
     * {@code PropertyChangeEvent} which indicates that text has changed.
     * <br>
     * For text insertion, the {@code oldValue} is {@code null} and the
     * {@code newValue} is an {@code AccessibleTextSequence} specifying the text
     * that was inserted.
     * <br>
     * For text deletion, the {@code oldValue} is an
     * {@code AccessibleTextSequence} specifying the text that was deleted and
     * the {@code newValue} is {@code null}.
     * <br>
     * For text replacement, the {@code oldValue} is an
     * {@code AccessibleTextSequence} specifying the old text and the
     * {@code newValue} is an {@code AccessibleTextSequence} specifying the new
     * text.
     *
     * @see #getAccessibleText
     * @see #addPropertyChangeListener
     * @see AccessibleTextSequence
     */
    public static final String ACCESSIBLE_TEXT_PROPERTY
        = "AccessibleText";

    /**
     * {@code PropertyChangeEvent} which indicates that a significant change has
     * occurred to the children of a component like a tree or text. This change
     * notifies the event listener that it needs to reacquire the state of the
     * subcomponents. The {@code oldValue} is {@code null} and the
     * {@code newValue} is the component whose children have become invalid.
     *
     * @see #getAccessibleText
     * @see #addPropertyChangeListener
     * @see AccessibleTextSequence
     * @since 1.5
     */
    public static final String ACCESSIBLE_INVALIDATE_CHILDREN =
        "accessibleInvalidateChildren";

    /**
     * {@code PropertyChangeEvent} which indicates that text attributes have
     * changed.
     * <br>
     * For attribute insertion, the {@code oldValue} is {@code null} and the
     * {@code newValue} is an {@code AccessibleAttributeSequence} specifying the
     * attributes that were inserted.
     * <br>
     * For attribute deletion, the {@code oldValue} is an
     * {@code AccessibleAttributeSequence} specifying the attributes that were
     * deleted and the {@code newValue} is {@code null}.
     * <br>
     * For attribute replacement, the {@code oldValue} is an
     * {@code AccessibleAttributeSequence} specifying the old attributes and the
     * {@code newValue} is an {@code AccessibleAttributeSequence} specifying the
     * new attributes.
     *
     * @see #getAccessibleText
     * @see #addPropertyChangeListener
     * @see AccessibleAttributeSequence
     * @since 1.5
     */
    public static final String ACCESSIBLE_TEXT_ATTRIBUTES_CHANGED =
        "accessibleTextAttributesChanged";

    /**
     * {@code PropertyChangeEvent} which indicates that a change has occurred in
     * a component's bounds. The {@code oldValue} is the old component bounds
     * and the {@code newValue} is the new component bounds.
     *
     * @see #addPropertyChangeListener
     * @since 1.5
     */
    public static final String ACCESSIBLE_COMPONENT_BOUNDS_CHANGED =
        "accessibleComponentBoundsChanged";

    /**
     * The accessible parent of this object.
     *
     * @see #getAccessibleParent
     * @see #setAccessibleParent
     */
    protected Accessible accessibleParent = null;

    /**
     * A localized String containing the name of the object.
     *
     * @see #getAccessibleName
     * @see #setAccessibleName
     */
    protected String accessibleName = null;

    /**
     * A localized String containing the description of the object.
     *
     * @see #getAccessibleDescription
     * @see #setAccessibleDescription
     */
    protected String accessibleDescription = null;

    /**
     * Used to handle the listener list for property change events.
     *
     * @see #addPropertyChangeListener
     * @see #removePropertyChangeListener
     * @see #firePropertyChange
     */
    private PropertyChangeSupport accessibleChangeSupport = null;

    /**
     * Used to represent the context's relation set.
     *
     * @see #getAccessibleRelationSet
     */
    private AccessibleRelationSet relationSet
        = new AccessibleRelationSet();

    private Object nativeAXResource;

    /**
     * Gets the {@code accessibleName} property of this object. The
     * {@code accessibleName} property of an object is a localized
     * {@code String} that designates the purpose of the object. For example,
     * the {@code accessibleName} property of a label or button might be the
     * text of the label or button itself. In the case of an object that doesn't
     * display its name, the {@code accessibleName} should still be set. For
     * example, in the case of a text field used to enter the name of a city,
     * the {@code accessibleName} for the {@code en_US} locale could be 'city.'
     *
     * @return the localized name of the object; {@code null} if this object
     *         does not have a name
     * @see #setAccessibleName
     */
    public String getAccessibleName() {
        return accessibleName;
    }

    /**
     * Sets the localized accessible name of this object. Changing the name will
     * cause a {@code PropertyChangeEvent} to be fired for the
     * {@code ACCESSIBLE_NAME_PROPERTY} property.
     *
     * @param  s the new localized name of the object
     * @see #getAccessibleName
     * @see #addPropertyChangeListener
     */
    @BeanProperty(preferred = true, description
            = "Sets the accessible name for the component.")
    public void setAccessibleName(String s) {
        String oldName = accessibleName;
        accessibleName = s;
        firePropertyChange(ACCESSIBLE_NAME_PROPERTY,oldName,accessibleName);
    }

    /**
     * Gets the {@code accessibleDescription} property of this object. The
     * {@code accessibleDescription} property of this object is a short
     * localized phrase describing the purpose of the object. For example, in
     * the case of a 'Cancel' button, the {@code accessibleDescription} could be
     * 'Ignore changes and close dialog box.'
     *
     * @return the localized description of the object; {@code null} if this
     *         object does not have a description
     * @see #setAccessibleDescription
     */
    public String getAccessibleDescription() {
        return accessibleDescription;
    }

    /**
     * Sets the accessible description of this object. Changing the name will
     * cause a {@code PropertyChangeEvent} to be fired for the
     * {@code ACCESSIBLE_DESCRIPTION_PROPERTY} property.
     *
     * @param  s the new localized description of the object
     * @see #setAccessibleName
     * @see #addPropertyChangeListener
     */
    @BeanProperty(preferred = true, description
            = "Sets the accessible description for the component.")
    public void setAccessibleDescription(String s) {
        String oldDescription = accessibleDescription;
        accessibleDescription = s;
        firePropertyChange(ACCESSIBLE_DESCRIPTION_PROPERTY,
                           oldDescription,accessibleDescription);
    }

    /**
     * Gets the role of this object. The role of the object is the generic
     * purpose or use of the class of this object. For example, the role of a
     * push button is {@code AccessibleRole.PUSH_BUTTON}. The roles in
     * {@code AccessibleRole} are provided so component developers can pick from
     * a set of predefined roles. This enables assistive technologies to provide
     * a consistent interface to various tweaked subclasses of components (e.g.,
     * use {@code AccessibleRole.PUSH_BUTTON} for all components that act like a
     * push button) as well as distinguish between subclasses that behave
     * differently (e.g., {@code AccessibleRole.CHECK_BOX} for check boxes and
     * {@code AccessibleRole.RADIO_BUTTON} for radio buttons).
     * <p>
     * Note that the {@code AccessibleRole} class is also extensible, so custom
     * component developers can define their own {@code AccessibleRole}'s if the
     * set of predefined roles is inadequate.
     *
     * @return an instance of {@code AccessibleRole} describing the role of the
     *         object
     * @see AccessibleRole
     */
    public abstract AccessibleRole getAccessibleRole();

    /**
     * Gets the state set of this object. The {@code AccessibleStateSet} of an
     * object is composed of a set of unique {@code AccessibleStates}. A change
     * in the {@code AccessibleStateSet} of an object will cause a
     * {@code PropertyChangeEvent} to be fired for the
     * {@code ACCESSIBLE_STATE_PROPERTY} property.
     *
     * @return an instance of {@code AccessibleStateSet} containing the current
     *         state set of the object
     * @see AccessibleStateSet
     * @see AccessibleState
     * @see #addPropertyChangeListener
     */
    public abstract AccessibleStateSet getAccessibleStateSet();

    /**
     * Gets the {@code Accessible} parent of this object.
     *
     * @return the {@code Accessible} parent of this object; {@code null} if
     *         this object does not have an {@code Accessible} parent
     */
    public Accessible getAccessibleParent() {
        return accessibleParent;
    }

    /**
     * Sets the {@code Accessible} parent of this object. This is meant to be
     * used only in the situations where the actual component's parent should
     * not be treated as the component's accessible parent and is a method that
     * should only be called by the parent of the accessible child.
     *
     * @param  a - {@code Accessible} to be set as the parent
     */
    public void setAccessibleParent(Accessible a) {
        accessibleParent = a;
    }

    /**
     * Gets the 0-based index of this object in its accessible parent.
     *
     * @return the 0-based index of this object in its parent; -1 if this object
     *         does not have an accessible parent.
     * @see #getAccessibleParent
     * @see #getAccessibleChildrenCount
     * @see #getAccessibleChild
     */
    public abstract int getAccessibleIndexInParent();

    /**
     * Returns the number of accessible children of the object.
     *
     * @return the number of accessible children of the object.
     */
    public abstract int getAccessibleChildrenCount();

    /**
     * Returns the specified {@code Accessible} child of the object. The
     * {@code Accessible} children of an {@code Accessible} object are
     * zero-based, so the first child of an {@code Accessible} child is at index
     * 0, the second child is at index 1, and so on.
     *
     * @param  i zero-based index of child
     * @return the {@code Accessible} child of the object
     * @see #getAccessibleChildrenCount
     */
    public abstract Accessible getAccessibleChild(int i);

    /**
     * Gets the locale of the component. If the component does not have a
     * locale, then the locale of its parent is returned.
     *
     * @return this component's locale. If this component does not have a
     *         locale, the locale of its parent is returned.
     * @throws IllegalComponentStateException If the component does not have its
     *         own locale and has not yet been added to a containment hierarchy
     *         such that the locale can be determined from the containing
     *         parent
     */
    public abstract Locale getLocale() throws IllegalComponentStateException;

    /**
     * Adds a {@code PropertyChangeListener} to the listener list. The listener
     * is registered for all {@code Accessible} properties and will be called
     * when those properties change.
     *
     * @param  listener The PropertyChangeListener to be added
     * @see #ACCESSIBLE_NAME_PROPERTY
     * @see #ACCESSIBLE_DESCRIPTION_PROPERTY
     * @see #ACCESSIBLE_STATE_PROPERTY
     * @see #ACCESSIBLE_VALUE_PROPERTY
     * @see #ACCESSIBLE_SELECTION_PROPERTY
     * @see #ACCESSIBLE_TEXT_PROPERTY
     * @see #ACCESSIBLE_VISIBLE_DATA_PROPERTY
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (accessibleChangeSupport == null) {
            accessibleChangeSupport = new PropertyChangeSupport(this);
        }
        accessibleChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes a {@code PropertyChangeListener} from the listener list. This
     * removes a {@code PropertyChangeListener} that was registered for all
     * properties.
     *
     * @param  listener The PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (accessibleChangeSupport != null) {
            accessibleChangeSupport.removePropertyChangeListener(listener);
        }
    }

    /**
     * Gets the {@code AccessibleAction} associated with this object that
     * supports one or more actions.
     *
     * @return {@code AccessibleAction} if supported by object; else return
     *         {@code null}
     * @see AccessibleAction
     */
    public AccessibleAction getAccessibleAction() {
        return null;
    }

    /**
     * Gets the {@code AccessibleComponent} associated with this object that has
     * a graphical representation.
     *
     * @return {@code AccessibleComponent} if supported by object; else return
     *         {@code null}
     * @see AccessibleComponent
     */
    public AccessibleComponent getAccessibleComponent() {
        return null;
    }

    /**
     * Gets the {@code AccessibleSelection} associated with this object which
     * allows its {@code Accessible} children to be selected.
     *
     * @return {@code AccessibleSelection} if supported by object; else return
     *         {@code null}
     * @see AccessibleSelection
     */
    public AccessibleSelection getAccessibleSelection() {
        return null;
    }

    /**
     * Gets the {@code AccessibleText} associated with this object presenting
     * text on the display.
     *
     * @return {@code AccessibleText} if supported by object; else return
     *         {@code null}
     * @see AccessibleText
     */
    public AccessibleText getAccessibleText() {
        return null;
    }

    /**
     * Gets the {@code AccessibleEditableText} associated with this object
     * presenting editable text on the display.
     *
     * @return {@code AccessibleEditableText} if supported by object; else
     *         return {@code null}
     * @see AccessibleEditableText
     * @since 1.4
     */
    public AccessibleEditableText getAccessibleEditableText() {
        return null;
    }

    /**
     * Gets the {@code AccessibleValue} associated with this object that
     * supports a {@code Numerical} value.
     *
     * @return {@code AccessibleValue} if supported by object; else return
     *         {@code null}
     * @see AccessibleValue
     */
    public AccessibleValue getAccessibleValue() {
        return null;
    }

    /**
     * Gets the {@code AccessibleIcons} associated with an object that has one
     * or more associated icons.
     *
     * @return an array of {@code AccessibleIcon} if supported by object;
     *         otherwise return {@code null}
     * @see AccessibleIcon
     * @since 1.3
     */
    public AccessibleIcon [] getAccessibleIcon() {
        return null;
    }

    /**
     * Gets the {@code AccessibleRelationSet} associated with an object.
     *
     * @return an {@code AccessibleRelationSet} if supported by object;
     *         otherwise return {@code null}
     * @see AccessibleRelationSet
     * @since 1.3
     */
    public AccessibleRelationSet getAccessibleRelationSet() {
        return relationSet;
    }

    /**
     * Gets the {@code AccessibleTable} associated with an object.
     *
     * @return an {@code AccessibleTable} if supported by object; otherwise return
     *         {@code null}
     * @see AccessibleTable
     * @since 1.3
     */
    public AccessibleTable getAccessibleTable() {
        return null;
    }

    /**
     * Support for reporting bound property changes. If {@code oldValue} and
     * {@code newValue} are not equal and the {@code PropertyChangeEvent}
     * listener list is not empty, then fire a {@code PropertyChange} event to
     * each listener. In general, this is for use by the {@code Accessible}
     * objects themselves and should not be called by an application program.
     *
     * @param  propertyName The programmatic name of the property that was
     *         changed
     * @param  oldValue The old value of the property
     * @param  newValue The new value of the property
     * @see PropertyChangeSupport
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
        if (accessibleChangeSupport != null) {
            if (newValue instanceof PropertyChangeEvent) {
                PropertyChangeEvent pce = (PropertyChangeEvent)newValue;
                accessibleChangeSupport.firePropertyChange(pce);
            } else {
                accessibleChangeSupport.firePropertyChange(propertyName,
                                                           oldValue,
                                                           newValue);
            }
        }
    }
}
