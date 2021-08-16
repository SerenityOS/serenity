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

import java.util.ListResourceBundle;

/**
 * A resource bundle containing the localized strings in the accessibility
 * package. This is meant only for internal use by Java Accessibility and is not
 * meant to be used by assistive technologies or applications.
 *
 * @author Willie Walker
 * @deprecated This class is deprecated as of version 1.3 of the Java Platform
 */
@Deprecated
public class AccessibleResourceBundle extends ListResourceBundle {

    /**
     * Constructs an {@code AccessibleResourceBundle}.
     */
    public AccessibleResourceBundle() {}

    /**
     * Returns the mapping between the programmatic keys and the localized
     * display strings.
     *
     * @return an array of an {@code Object} array representing a key-value pair
     */
    public Object[][] getContents() {
        // The table holding the mapping between the programmatic keys
        // and the display strings for the en_US locale.
        return new Object[][] {

        // LOCALIZE THIS
        // Role names
//        { "application","application" },
//        { "border","border" },
//        { "checkboxmenuitem","check box menu item" },
//        { "choice","choice" },
//        { "column","column" },
//        { "cursor","cursor" },
//        { "document","document" },
//        { "grouping","grouping" },
//        { "image","image" },
//        { "indicator","indicator" },
//        { "radiobuttonmenuitem","radio button menu item" },
//        { "row","row" },
//        { "tablecell","table cell" },
//        { "treenode","tree node" },
        { "alert","alert" },
        { "awtcomponent","AWT component" },
        { "checkbox","check box" },
        { "colorchooser","color chooser" },
        { "columnheader","column header" },
        { "combobox","combo box" },
        { "canvas","canvas" },
        { "desktopicon","desktop icon" },
        { "desktoppane","desktop pane" },
        { "dialog","dialog" },
        { "directorypane","directory pane" },
        { "glasspane","glass pane" },
        { "filechooser","file chooser" },
        { "filler","filler" },
        { "frame","frame" },
        { "internalframe","internal frame" },
        { "label","label" },
        { "layeredpane","layered pane" },
        { "list","list" },
        { "listitem","list item" },
        { "menubar","menu bar" },
        { "menu","menu" },
        { "menuitem","menu item" },
        { "optionpane","option pane" },
        { "pagetab","page tab" },
        { "pagetablist","page tab list" },
        { "panel","panel" },
        { "passwordtext","password text" },
        { "popupmenu","popup menu" },
        { "progressbar","progress bar" },
        { "pushbutton","push button" },
        { "radiobutton","radio button" },
        { "rootpane","root pane" },
        { "rowheader","row header" },
        { "scrollbar","scroll bar" },
        { "scrollpane","scroll pane" },
        { "separator","separator" },
        { "slider","slider" },
        { "splitpane","split pane" },
        { "swingcomponent","swing component" },
        { "table","table" },
        { "text","text" },
        { "tree","tree" },
        { "togglebutton","toggle button" },
        { "toolbar","tool bar" },
        { "tooltip","tool tip" },
        { "unknown","unknown" },
        { "viewport","viewport" },
        { "window","window" },
        // Relations
        { "labelFor","label for" },
        { "labeledBy","labeled by" },
        { "memberOf","member of" },
        { "controlledBy","controlledBy" },
        { "controllerFor","controllerFor" },
        // State modes
        { "active","active" },
        { "armed","armed" },
        { "busy","busy" },
        { "checked","checked" },
        { "collapsed", "collapsed" },
        { "editable","editable" },
        { "expandable", "expandable" },
        { "expanded", "expanded" },
        { "enabled","enabled" },
        { "focusable","focusable" },
        { "focused","focused" },
        { "iconified", "iconified" },
        { "modal", "modal" },
        { "multiline", "multiple line" },
        { "multiselectable","multiselectable" },
        { "opaque", "opaque" },
        { "pressed","pressed" },
        { "resizable", "resizable" },
        { "selectable","selectable" },
        { "selected","selected" },
        { "showing","showing" },
        { "singleline", "single line" },
        { "transient", "transient" },
        { "visible","visible" },
        { "vertical","vertical" },
        { "horizontal","horizontal" }
    // END OF MATERIAL TO LOCALIZE
        };
    }
}
