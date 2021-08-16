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
 * Class {@code AccessibleRole} determines the role of a component. The role of
 * a component describes its generic function. (E.G., "push button," "table," or
 * "list.")
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
 * @author Lynn Monsanto
 */
public class AccessibleRole extends AccessibleBundle {

    // If you add or remove anything from here, make sure you
    // update AccessibleResourceBundle.java.

    /**
     * Object is used to alert the user about something.
     */
    public static final AccessibleRole ALERT
            = new AccessibleRole("alert");

    /**
     * The header for a column of data.
     */
    public static final AccessibleRole COLUMN_HEADER
            = new AccessibleRole("columnheader");

    /**
     * Object that can be drawn into and is used to trap events.
     *
     * @see #FRAME
     * @see #GLASS_PANE
     * @see #LAYERED_PANE
     */
    public static final AccessibleRole CANVAS
            = new AccessibleRole("canvas");

    /**
     * A list of choices the user can select from. Also optionally allows the
     * user to enter a choice of their own.
     */
    public static final AccessibleRole COMBO_BOX
            = new AccessibleRole("combobox");

    /**
     * An iconified internal frame in a {@code DESKTOP_PANE}.
     *
     * @see #DESKTOP_PANE
     * @see #INTERNAL_FRAME
     */
    public static final AccessibleRole DESKTOP_ICON
            = new AccessibleRole("desktopicon");

    /**
     * An object containing a collection of {@code Accessibles} that together
     * represents {@code HTML} content. The child {@code Accessibles} would
     * include objects implementing {@code AccessibleText},
     * {@code AccessibleHypertext}, {@code AccessibleIcon}, and other
     * interfaces.
     *
     * @see #HYPERLINK
     * @see AccessibleText
     * @see AccessibleHypertext
     * @see AccessibleHyperlink
     * @see AccessibleIcon
     * @since 1.6
     */
    public static final AccessibleRole HTML_CONTAINER
            = new AccessibleRole("htmlcontainer");

    /**
     * A frame-like object that is clipped by a desktop pane. The desktop pane,
     * internal frame, and desktop icon objects are often used to create
     * multiple document interfaces within an application.
     *
     * @see #DESKTOP_ICON
     * @see #DESKTOP_PANE
     * @see #FRAME
     */
    public static final AccessibleRole INTERNAL_FRAME
            = new AccessibleRole("internalframe");

    /**
     * A pane that supports internal frames and iconified versions of those
     * internal frames.
     *
     * @see #DESKTOP_ICON
     * @see #INTERNAL_FRAME
     */
    public static final AccessibleRole DESKTOP_PANE
            = new AccessibleRole("desktoppane");

    /**
     * A specialized pane whose primary use is inside a {@code DIALOG}.
     *
     * @see #DIALOG
     */
    public static final AccessibleRole OPTION_PANE
            = new AccessibleRole("optionpane");

    /**
     * A top level window with no title or border.
     *
     * @see #FRAME
     * @see #DIALOG
     */
    public static final AccessibleRole WINDOW
            = new AccessibleRole("window");

    /**
     * A top level window with a title bar, border, menu bar, etc. It is often
     * used as the primary window for an application.
     *
     * @see #DIALOG
     * @see #CANVAS
     * @see #WINDOW
     */
    public static final AccessibleRole FRAME
            = new AccessibleRole("frame");

    /**
     * A top level window with title bar and a border. A dialog is similar to a
     * frame, but it has fewer properties and is often used as a secondary
     * window for an application.
     *
     * @see #FRAME
     * @see #WINDOW
     */
    public static final AccessibleRole DIALOG
            = new AccessibleRole("dialog");

    /**
     * A specialized pane that lets the user choose a color.
     */
    public static final AccessibleRole COLOR_CHOOSER
            = new AccessibleRole("colorchooser");


    /**
     * A pane that allows the user to navigate through and select the contents
     * of a directory. May be used by a file chooser.
     *
     * @see #FILE_CHOOSER
     */
    public static final AccessibleRole DIRECTORY_PANE
            = new AccessibleRole("directorypane");

    /**
     * A specialized dialog that displays the files in the directory and lets
     * the user select a file, browse a different directory, or specify a
     * filename. May use the directory pane to show the contents of a directory.
     *
     * @see #DIRECTORY_PANE
     */
    public static final AccessibleRole FILE_CHOOSER
            = new AccessibleRole("filechooser");

    /**
     * An object that fills up space in a user interface. It is often used in
     * interfaces to tweak the spacing between components, but serves no other
     * purpose.
     */
    public static final AccessibleRole FILLER
            = new AccessibleRole("filler");

    /**
     * A hypertext anchor.
     */
    public static final AccessibleRole HYPERLINK
            = new AccessibleRole("hyperlink");

    /**
     * A small fixed size picture, typically used to decorate components.
     */
    public static final AccessibleRole ICON
            = new AccessibleRole("icon");

    /**
     * An object used to present an icon or short string in an interface.
     */
    public static final AccessibleRole LABEL
            = new AccessibleRole("label");

    /**
     * A specialized pane that has a glass pane and a layered pane as its
     * children.
     *
     * @see #GLASS_PANE
     * @see #LAYERED_PANE
     */
    public static final AccessibleRole ROOT_PANE
            = new AccessibleRole("rootpane");

    /**
     * A pane that is guaranteed to be painted on top of all panes beneath it.
     *
     * @see #ROOT_PANE
     * @see #CANVAS
     */
    public static final AccessibleRole GLASS_PANE
            = new AccessibleRole("glasspane");

    /**
     * A specialized pane that allows its children to be drawn in layers,
     * providing a form of stacking order. This is usually the pane that holds
     * the menu bar as well as the pane that contains most of the visual
     * components in a window.
     *
     * @see #GLASS_PANE
     * @see #ROOT_PANE
     */
    public static final AccessibleRole LAYERED_PANE
            = new AccessibleRole("layeredpane");

    /**
     * An object that presents a list of objects to the user and allows the user
     * to select one or more of them. A list is usually contained within a
     * scroll pane.
     *
     * @see #SCROLL_PANE
     * @see #LIST_ITEM
     */
    public static final AccessibleRole LIST
            = new AccessibleRole("list");

    /**
     * An object that presents an element in a list. A list is usually contained
     * within a scroll pane.
     *
     * @see #SCROLL_PANE
     * @see #LIST
     */
    public static final AccessibleRole LIST_ITEM
            = new AccessibleRole("listitem");

    /**
     * An object usually drawn at the top of the primary dialog box of an
     * application that contains a list of menus the user can choose from. For
     * example, a menu bar might contain menus for "File," "Edit," and "Help."
     *
     * @see #MENU
     * @see #POPUP_MENU
     * @see #LAYERED_PANE
     */
    public static final AccessibleRole MENU_BAR
            = new AccessibleRole("menubar");

    /**
     * A temporary window that is usually used to offer the user a list of
     * choices, and then hides when the user selects one of those choices.
     *
     * @see #MENU
     * @see #MENU_ITEM
     */
    public static final AccessibleRole POPUP_MENU
            = new AccessibleRole("popupmenu");

    /**
     * An object usually found inside a menu bar that contains a list of actions
     * the user can choose from. A menu can have any object as its children, but
     * most often they are menu items, other menus, or rudimentary objects such
     * as radio buttons, check boxes, or separators. For example, an application
     * may have an "Edit" menu that contains menu items for "Cut" and "Paste."
     *
     * @see #MENU_BAR
     * @see #MENU_ITEM
     * @see #SEPARATOR
     * @see #RADIO_BUTTON
     * @see #CHECK_BOX
     * @see #POPUP_MENU
     */
    public static final AccessibleRole MENU
            = new AccessibleRole("menu");

    /**
     * An object usually contained in a menu that presents an action the user
     * can choose. For example, the "Cut" menu item in an "Edit" menu would be
     * an action the user can select to cut the selected area of text in a
     * document.
     *
     * @see #MENU_BAR
     * @see #SEPARATOR
     * @see #POPUP_MENU
     */
    public static final AccessibleRole MENU_ITEM
            = new AccessibleRole("menuitem");

    /**
     * An object usually contained in a menu to provide a visual and logical
     * separation of the contents in a menu. For example, the "File" menu of an
     * application might contain menu items for "Open," "Close," and "Exit," and
     * will place a separator between "Close" and "Exit" menu items.
     *
     * @see #MENU
     * @see #MENU_ITEM
     */
    public static final AccessibleRole SEPARATOR
            = new AccessibleRole("separator");

    /**
     * An object that presents a series of panels (or page tabs), one at a time,
     * through some mechanism provided by the object. The most common mechanism
     * is a list of tabs at the top of the panel. The children of a page tab
     * list are all page tabs.
     *
     * @see #PAGE_TAB
     */
    public static final AccessibleRole PAGE_TAB_LIST
            = new AccessibleRole("pagetablist");

    /**
     * An object that is a child of a page tab list. Its sole child is the panel
     * that is to be presented to the user when the user selects the page tab
     * from the list of tabs in the page tab list.
     *
     * @see #PAGE_TAB_LIST
     */
    public static final AccessibleRole PAGE_TAB
            = new AccessibleRole("pagetab");

    /**
     * A generic container that is often used to group objects.
     */
    public static final AccessibleRole PANEL
            = new AccessibleRole("panel");

    /**
     * An object used to indicate how much of a task has been completed.
     */
    public static final AccessibleRole PROGRESS_BAR
            = new AccessibleRole("progressbar");

    /**
     * A text object used for passwords, or other places where the text contents
     * is not shown visibly to the user.
     */
    public static final AccessibleRole PASSWORD_TEXT
            = new AccessibleRole("passwordtext");

    /**
     * An object the user can manipulate to tell the application to do
     * something.
     *
     * @see #CHECK_BOX
     * @see #TOGGLE_BUTTON
     * @see #RADIO_BUTTON
     */
    public static final AccessibleRole PUSH_BUTTON
            = new AccessibleRole("pushbutton");

    /**
     * A specialized push button that can be checked or unchecked, but does not
     * provide a separate indicator for the current state.
     *
     * @see #PUSH_BUTTON
     * @see #CHECK_BOX
     * @see #RADIO_BUTTON
     */
    public static final AccessibleRole TOGGLE_BUTTON
            = new AccessibleRole("togglebutton");

    /**
     * A choice that can be checked or unchecked and provides a separate
     * indicator for the current state.
     *
     * @see #PUSH_BUTTON
     * @see #TOGGLE_BUTTON
     * @see #RADIO_BUTTON
     */
    public static final AccessibleRole CHECK_BOX
            = new AccessibleRole("checkbox");

    /**
     * A specialized check box that will cause other radio buttons in the same
     * group to become unchecked when this one is checked.
     *
     * @see #PUSH_BUTTON
     * @see #TOGGLE_BUTTON
     * @see #CHECK_BOX
     */
    public static final AccessibleRole RADIO_BUTTON
            = new AccessibleRole("radiobutton");

    /**
     * The header for a row of data.
     */
    public static final AccessibleRole ROW_HEADER
            = new AccessibleRole("rowheader");

    /**
     * An object that allows a user to incrementally view a large amount of
     * information. Its children can include scroll bars and a viewport.
     *
     * @see #SCROLL_BAR
     * @see #VIEWPORT
     */
    public static final AccessibleRole SCROLL_PANE
            = new AccessibleRole("scrollpane");

    /**
     * An object usually used to allow a user to incrementally view a large
     * amount of data. Usually used only by a scroll pane.
     *
     * @see #SCROLL_PANE
     */
    public static final AccessibleRole SCROLL_BAR
            = new AccessibleRole("scrollbar");

    /**
     * An object usually used in a scroll pane. It represents the portion of the
     * entire data that the user can see. As the user manipulates the scroll
     * bars, the contents of the viewport can change.
     *
     * @see #SCROLL_PANE
     */
    public static final AccessibleRole VIEWPORT
            = new AccessibleRole("viewport");

    /**
     * An object that allows the user to select from a bounded range. For
     * example, a slider might be used to select a number between 0 and 100.
     */
    public static final AccessibleRole SLIDER
            = new AccessibleRole("slider");

    /**
     * A specialized panel that presents two other panels at the same time.
     * Between the two panels is a divider the user can manipulate to make one
     * panel larger and the other panel smaller.
     */
    public static final AccessibleRole SPLIT_PANE
            = new AccessibleRole("splitpane");

    /**
     * An object used to present information in terms of rows and columns. An
     * example might include a spreadsheet application.
     */
    public static final AccessibleRole TABLE
            = new AccessibleRole("table");

    /**
     * An object that presents text to the user. The text is usually editable by
     * the user as opposed to a label.
     *
     * @see #LABEL
     */
    public static final AccessibleRole TEXT
            = new AccessibleRole("text");

    /**
     * An object used to present hierarchical information to the user. The
     * individual nodes in the tree can be collapsed and expanded to provide
     * selective disclosure of the tree's contents.
     */
    public static final AccessibleRole TREE
            = new AccessibleRole("tree");

    /**
     * A bar or palette usually composed of push buttons or toggle buttons. It
     * is often used to provide the most frequently used functions for an
     * application.
     */
    public static final AccessibleRole TOOL_BAR
            = new AccessibleRole("toolbar");

    /**
     * An object that provides information about another object. The
     * {@code accessibleDescription} property of the tool tip is often displayed
     * to the user in a small "help bubble" when the user causes the mouse to
     * hover over the object associated with the tool tip.
     */
    public static final AccessibleRole TOOL_TIP
            = new AccessibleRole("tooltip");

    /**
     * An AWT component, but nothing else is known about it.
     *
     * @see #SWING_COMPONENT
     * @see #UNKNOWN
     */
    public static final AccessibleRole AWT_COMPONENT
            = new AccessibleRole("awtcomponent");

    /**
     * A Swing component, but nothing else is known about it.
     *
     * @see #AWT_COMPONENT
     * @see #UNKNOWN
     */
    public static final AccessibleRole SWING_COMPONENT
            = new AccessibleRole("swingcomponent");

    /**
     * The object contains some {@code Accessible} information, but its role is
     * not known.
     *
     * @see #AWT_COMPONENT
     * @see #SWING_COMPONENT
     */
    public static final AccessibleRole UNKNOWN
            = new AccessibleRole("unknown");

    /**
     * A {@code STATUS_BAR} is an simple component that can contain multiple
     * labels of status information to the user.
     */
    public static final AccessibleRole STATUS_BAR
        = new AccessibleRole("statusbar");

    /**
     * A {@code DATE_EDITOR} is a component that allows users to edit
     * {@code java.util.Date} and {@code java.util.Time} objects.
     */
    public static final AccessibleRole DATE_EDITOR
        = new AccessibleRole("dateeditor");

    /**
     * A {@code SPIN_BOX} is a simple spinner component and its main use is for
     * simple numbers.
     */
    public static final AccessibleRole SPIN_BOX
        = new AccessibleRole("spinbox");

    /**
     * A {@code FONT_CHOOSER} is a component that lets the user pick various
     * attributes for fonts.
     */
    public static final AccessibleRole FONT_CHOOSER
        = new AccessibleRole("fontchooser");

    /**
     * A {@code GROUP_BOX} is a simple container that contains a border around
     * it and contains components inside it.
     */
    public static final AccessibleRole GROUP_BOX
        = new AccessibleRole("groupbox");

    /**
     * A text header.
     *
     * @since 1.5
     */
    public static final AccessibleRole HEADER =
        new AccessibleRole("header");

    /**
     * A text footer.
     *
     * @since 1.5
     */
    public static final AccessibleRole FOOTER =
        new AccessibleRole("footer");

    /**
     * A text paragraph.
     *
     * @since 1.5
     */
    public static final AccessibleRole PARAGRAPH =
        new AccessibleRole("paragraph");

    /**
     * A ruler is an object used to measure distance.
     *
     * @since 1.5
     */
    public static final AccessibleRole RULER =
        new AccessibleRole("ruler");

    /**
     * A role indicating the object acts as a formula for calculating a value.
     * An example is a formula in a spreadsheet cell.
     *
     * @since 1.5
     */
    public static final AccessibleRole EDITBAR =
        new AccessibleRole("editbar");

    /**
     * A role indicating the object monitors the progress of some operation.
     *
     * @since 1.5
     */
    public static final AccessibleRole PROGRESS_MONITOR =
        new AccessibleRole("progressMonitor");


// The following are all under consideration for potential future use.

//    public static final AccessibleRole APPLICATION
//            = new AccessibleRole("application");

//    public static final AccessibleRole BORDER
//            = new AccessibleRole("border");

//    public static final AccessibleRole CHECK_BOX_MENU_ITEM
//            = new AccessibleRole("checkboxmenuitem");

//    public static final AccessibleRole CHOICE
//            = new AccessibleRole("choice");

//    public static final AccessibleRole COLUMN
//            = new AccessibleRole("column");

//    public static final AccessibleRole CURSOR
//            = new AccessibleRole("cursor");

//    public static final AccessibleRole DOCUMENT
//            = new AccessibleRole("document");

//    public static final AccessibleRole IMAGE
//            = new AccessibleRole("Image");

//    public static final AccessibleRole INDICATOR
//            = new AccessibleRole("indicator");

//    public static final AccessibleRole RADIO_BUTTON_MENU_ITEM
//            = new AccessibleRole("radiobuttonmenuitem");

//    public static final AccessibleRole ROW
//            = new AccessibleRole("row");

//    public static final AccessibleRole TABLE_CELL
//          = new AccessibleRole("tablecell");

//    public static final AccessibleRole TREE_NODE
//            = new AccessibleRole("treenode");

    /**
     * Creates a new {@code AccessibleRole} using the given locale independent
     * key. This should not be a public method. Instead, it is used to create
     * the constants in this file to make it a strongly typed enumeration.
     * Subclasses of this class should enforce similar policy.
     * <p>
     * The key {@code String} should be a locale independent key for the role.
     * It is not intended to be used as the actual {@code String} to display to
     * the user. To get the localized string, use {@link #toDisplayString()}.
     *
     * @param  key the locale independent name of the role
     * @see AccessibleBundle#toDisplayString
     */
    protected AccessibleRole(String key) {
        this.key = key;
    }
}
