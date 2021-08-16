/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Class {@code AccessibleState} describes a component's particular state. The
 * actual state of the component is defined as an {@code AccessibleStateSet},
 * which is a composed set of {@code AccessibleStates}.
 * <p>
 * The {@link #toDisplayString()} method allows you to obtain the localized
 * string for a locale independent key from a predefined {@code ResourceBundle}
 * for the keys defined in this class.
 * <p>
 * The constants in this class present a strongly typed enumeration of common
 * object roles. A public constructor for this class has been purposely omitted
 * and applications should use one of the constants from this class. If the
 * constants in this class are not sufficient to describe the role of an object,
 * a subclass should be generated from this class and it should provide
 * constants in a similar manner.
 *
 * @author Willie Walker
 * @author Peter Korn
 */
public class AccessibleState extends AccessibleBundle {

    // If you add or remove anything from here, make sure you
    // update AccessibleResourceBundle.java.

    /**
     * Indicates a window is currently the active window. This includes windows,
     * dialogs, frames, etc. In addition, this state is used to indicate the
     * currently active child of a component such as a list, table, or tree. For
     * example, the active child of a list is the child that is drawn with a
     * rectangle around it.
     *
     * @see AccessibleRole#WINDOW
     * @see AccessibleRole#FRAME
     * @see AccessibleRole#DIALOG
     */
    public static final AccessibleState ACTIVE
            = new AccessibleState("active");

    /**
     * Indicates this object is currently pressed. This is usually associated
     * with buttons and indicates the user has pressed a mouse button while the
     * pointer was over the button and has not yet released the mouse button.
     *
     * @see AccessibleRole#PUSH_BUTTON
     */
    public static final AccessibleState PRESSED
            = new AccessibleState("pressed");

    /**
     * Indicates that the object is armed. This is usually used on buttons that
     * have been pressed but not yet released, and the mouse pointer is still
     * over the button.
     *
     * @see AccessibleRole#PUSH_BUTTON
     */
    public static final AccessibleState ARMED
            = new AccessibleState("armed");

    /**
     * Indicates the current object is busy. This is usually used on objects
     * such as progress bars, sliders, or scroll bars to indicate they are in a
     * state of transition.
     *
     * @see AccessibleRole#PROGRESS_BAR
     * @see AccessibleRole#SCROLL_BAR
     * @see AccessibleRole#SLIDER
     */
    public static final AccessibleState BUSY
            = new AccessibleState("busy");

    /**
     * Indicates this object is currently checked. This is usually used on
     * objects such as toggle buttons, radio buttons, and check boxes.
     *
     * @see AccessibleRole#TOGGLE_BUTTON
     * @see AccessibleRole#RADIO_BUTTON
     * @see AccessibleRole#CHECK_BOX
     */
    public static final AccessibleState CHECKED
            = new AccessibleState("checked");

    /**
     * Indicates the user can change the contents of this object. This is
     * usually used primarily for objects that allow the user to enter text.
     * Other objects, such as scroll bars and sliders, are automatically
     * editable if they are enabled.
     *
     * @see #ENABLED
     */
    public static final AccessibleState EDITABLE
            = new AccessibleState("editable");

    /**
     * Indicates this object allows progressive disclosure of its children. This
     * is usually used with hierarchical objects such as trees and is often
     * paired with the {@code EXPANDED} or {@code COLLAPSED} states.
     *
     * @see #EXPANDED
     * @see #COLLAPSED
     * @see AccessibleRole#TREE
     */
    public static final AccessibleState EXPANDABLE
            = new AccessibleState("expandable");

    /**
     * Indicates this object is collapsed. This is usually paired with the
     * {@code EXPANDABLE} state and is used on objects that provide progressive
     * disclosure such as trees.
     *
     * @see #EXPANDABLE
     * @see #EXPANDED
     * @see AccessibleRole#TREE
     */
    public static final AccessibleState COLLAPSED
            = new AccessibleState("collapsed");

    /**
     * Indicates this object is expanded. This is usually paired with the
     * {@code EXPANDABLE} state and is used on objects that provide progressive
     * disclosure such as trees.
     *
     * @see #EXPANDABLE
     * @see #COLLAPSED
     * @see AccessibleRole#TREE
     */
    public static final AccessibleState EXPANDED
            = new AccessibleState("expanded");

    /**
     * Indicates this object is enabled. The absence of this state from an
     * object's state set indicates this object is not enabled. An object that
     * is not enabled cannot be manipulated by the user. In a graphical display,
     * it is usually grayed out.
     */
    public static final AccessibleState ENABLED
            = new AccessibleState("enabled");

    /**
     * Indicates this object can accept keyboard focus, which means all events
     * resulting from typing on the keyboard will normally be passed to it when
     * it has focus.
     *
     * @see #FOCUSED
     */
    public static final AccessibleState FOCUSABLE
            = new AccessibleState("focusable");

    /**
     * Indicates this object currently has the keyboard focus.
     *
     * @see #FOCUSABLE
     */
    public static final AccessibleState FOCUSED
            = new AccessibleState("focused");

    /**
     * Indicates this object is minimized and is represented only by an icon.
     * This is usually only associated with frames and internal frames.
     *
     * @see AccessibleRole#FRAME
     * @see AccessibleRole#INTERNAL_FRAME
     */
    public static final AccessibleState ICONIFIED
            = new AccessibleState("iconified");

    /**
     * Indicates something must be done with this object before the user can
     * interact with an object in a different window. This is usually associated
     * only with dialogs.
     *
     * @see AccessibleRole#DIALOG
     */
    public static final AccessibleState MODAL
            = new AccessibleState("modal");

    /**
     * Indicates this object paints every pixel within its rectangular region. A
     * non-opaque component paints only some of its pixels, allowing the pixels
     * underneath it to "show through". A component that does not fully paint
     * its pixels therefore provides a degree of transparency.
     *
     * @see Accessible#getAccessibleContext
     * @see AccessibleContext#getAccessibleComponent
     * @see AccessibleComponent#getBounds
     */
    public static final AccessibleState OPAQUE
            = new AccessibleState("opaque");

    /**
     * Indicates the size of this object is not fixed.
     *
     * @see Accessible#getAccessibleContext
     * @see AccessibleContext#getAccessibleComponent
     * @see AccessibleComponent#getSize
     * @see AccessibleComponent#setSize
     */
    public static final AccessibleState RESIZABLE
            = new AccessibleState("resizable");


    /**
     * Indicates this object allows more than one of its children to be selected
     * at the same time.
     *
     * @see Accessible#getAccessibleContext
     * @see AccessibleContext#getAccessibleSelection
     * @see AccessibleSelection
     */
    public static final AccessibleState MULTISELECTABLE
            = new AccessibleState("multiselectable");

    /**
     * Indicates this object is the child of an object that allows its children
     * to be selected, and that this child is one of those children that can be
     * selected.
     *
     * @see #SELECTED
     * @see Accessible#getAccessibleContext
     * @see AccessibleContext#getAccessibleSelection
     * @see AccessibleSelection
     */
    public static final AccessibleState SELECTABLE
            = new AccessibleState("selectable");

    /**
     * Indicates this object is the child of an object that allows its children
     * to be selected, and that this child is one of those children that has
     * been selected.
     *
     * @see #SELECTABLE
     * @see Accessible#getAccessibleContext
     * @see AccessibleContext#getAccessibleSelection
     * @see AccessibleSelection
     */
    public static final AccessibleState SELECTED
            = new AccessibleState("selected");

    /**
     * Indicates this object, the object's parent, the object's parent's parent,
     * and so on, are all visible. Note that this does not necessarily mean the
     * object is painted on the screen. It might be occluded by some other
     * showing object.
     *
     * @see #VISIBLE
     */
    public static final AccessibleState SHOWING
            = new AccessibleState("showing");

    /**
     * Indicates this object is visible. Note: this means that the object
     * intends to be visible; however, it may not in fact be showing on the
     * screen because one of the objects that this object is contained by is not
     * visible.
     *
     * @see #SHOWING
     */
    public static final AccessibleState VISIBLE
            = new AccessibleState("visible");

    /**
     * Indicates the orientation of this object is vertical. This is usually
     * associated with objects such as scrollbars, sliders, and progress bars.
     *
     * @see #VERTICAL
     * @see AccessibleRole#SCROLL_BAR
     * @see AccessibleRole#SLIDER
     * @see AccessibleRole#PROGRESS_BAR
     */
    public static final AccessibleState VERTICAL
            = new AccessibleState("vertical");

    /**
     * Indicates the orientation of this object is horizontal. This is usually
     * associated with objects such as scrollbars, sliders, and progress bars.
     *
     * @see #HORIZONTAL
     * @see AccessibleRole#SCROLL_BAR
     * @see AccessibleRole#SLIDER
     * @see AccessibleRole#PROGRESS_BAR
     */
    public static final AccessibleState HORIZONTAL
            = new AccessibleState("horizontal");

    /**
     * Indicates this (text) object can contain only a single line of text.
     */
    public static final AccessibleState SINGLE_LINE
            = new AccessibleState("singleline");

    /**
     * Indicates this (text) object can contain multiple lines of text.
     */
    public static final AccessibleState MULTI_LINE
            = new AccessibleState("multiline");

    /**
     * Indicates this object is transient. An assistive technology should not
     * add a {@code PropertyChange} listener to an object with transient state,
     * as that object will never generate any events. Transient objects are
     * typically created to answer Java Accessibility method queries, but
     * otherwise do not remain linked to the underlying object (for example,
     * those objects underneath lists, tables, and trees in Swing, where only
     * one actual {@code UI Component} does shared rendering duty for all of the
     * data objects underneath the actual list/table/tree elements).
     *
     * @since 1.5
     */
    public static final AccessibleState TRANSIENT
            = new AccessibleState("transient");

    /**
     * Indicates this object is responsible for managing its subcomponents. This
     * is typically used for trees and tables that have a large number of
     * subcomponents and where the objects are created only when needed and
     * otherwise remain virtual. The application should not manage the
     * subcomponents directly.
     *
     * @since 1.5
     */
    public static final AccessibleState MANAGES_DESCENDANTS
            = new AccessibleState ("managesDescendants");

    /**
     * Indicates that the object state is indeterminate. An example is selected
     * text that is partially bold and partially not bold. In this case the
     * attributes associated with the selected text are indeterminate.
     *
     * @since 1.5
     */
    public static final AccessibleState INDETERMINATE
           = new AccessibleState ("indeterminate");

    /**
     * A state indicating that text is truncated by a bounding rectangle and
     * that some of the text is not displayed on the screen. An example is text
     * in a spreadsheet cell that is truncated by the bounds of the cell.
     *
     * @since 1.5
     */
    public static final AccessibleState TRUNCATED
           =  new AccessibleState("truncated");

    /**
     * Creates a new {@code AccessibleState} using the given locale independent
     * key. This should not be a public method. Instead, it is used to create
     * the constants in this file to make it a strongly typed enumeration.
     * Subclasses of this class should enforce similar policy.
     * <p>
     * The key {@code String} should be a locale independent key for the state.
     * It is not intended to be used as the actual {@code String} to display to
     * the user. To get the localized string, use {@link #toDisplayString()}.
     *
     * @param  key the locale independent name of the state
     * @see AccessibleBundle#toDisplayString
     */
    protected AccessibleState(String key) {
        this.key = key;
    }
}
