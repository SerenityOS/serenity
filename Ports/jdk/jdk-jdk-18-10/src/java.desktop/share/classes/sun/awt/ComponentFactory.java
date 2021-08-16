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

package sun.awt;

import java.awt.AWTException;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxMenuItem;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.HeadlessException;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.PopupMenu;
import java.awt.ScrollPane;
import java.awt.Scrollbar;
import java.awt.Taskbar;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.Window;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.awt.peer.ButtonPeer;
import java.awt.peer.CanvasPeer;
import java.awt.peer.CheckboxMenuItemPeer;
import java.awt.peer.CheckboxPeer;
import java.awt.peer.ChoicePeer;
import java.awt.peer.DesktopPeer;
import java.awt.peer.DialogPeer;
import java.awt.peer.FileDialogPeer;
import java.awt.peer.FontPeer;
import java.awt.peer.FramePeer;
import java.awt.peer.LabelPeer;
import java.awt.peer.LightweightPeer;
import java.awt.peer.ListPeer;
import java.awt.peer.MenuBarPeer;
import java.awt.peer.MenuItemPeer;
import java.awt.peer.MenuPeer;
import java.awt.peer.MouseInfoPeer;
import java.awt.peer.PanelPeer;
import java.awt.peer.PopupMenuPeer;
import java.awt.peer.RobotPeer;
import java.awt.peer.ScrollPanePeer;
import java.awt.peer.ScrollbarPeer;
import java.awt.peer.TaskbarPeer;
import java.awt.peer.TextAreaPeer;
import java.awt.peer.TextFieldPeer;
import java.awt.peer.WindowPeer;

import sun.awt.datatransfer.DataTransferer;

final class LightweightPeerHolder {
    static final LightweightPeer lightweightMarker = new NullComponentPeer();

    private LightweightPeerHolder() {
    }
}

/**
 * Interface for component creation support in toolkits.
 */
public interface ComponentFactory {

    /**
     * Creates a peer for a component or container. This peer is windowless and
     * allows the Component and Container classes to be extended directly to
     * create windowless components that are defined entirely in java.
     *
     * @param  target The Component to be created
     * @return the peer for the specified component
     */
    default LightweightPeer createComponent(Component target) {
        return LightweightPeerHolder.lightweightMarker;
    }

    /**
     * Creates this toolkit's implementation of the {@code Desktop} using the
     * specified peer interface.
     *
     * @param  target the desktop to be implemented
     * @return this toolkit's implementation of the {@code Desktop}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.Desktop
     * @see java.awt.peer.DesktopPeer
     * @since 1.6
     */
    default DesktopPeer createDesktopPeer(Desktop target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of the {@code Taskbar} using the
     * specified peer interface.
     *
     * @param  target the taskbar to be implemented
     * @return this toolkit's implementation of the {@code Taskbar}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.Taskbar
     * @see java.awt.peer.TaskbarPeer
     * @since 9
     */
    default TaskbarPeer createTaskbarPeer(Taskbar target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code Button} using the
     * specified peer interface.
     *
     * @param  target the button to be implemented
     * @return this toolkit's implementation of {@code Button}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.Button
     * @see java.awt.peer.ButtonPeer
     */
    default ButtonPeer createButton(Button target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code TextField} using the
     * specified peer interface.
     *
     * @param  target the text field to be implemented
     * @return this toolkit's implementation of {@code TextField}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.TextField
     * @see java.awt.peer.TextFieldPeer
     */
    default TextFieldPeer createTextField(TextField target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code Label} using the
     * specified peer interface.
     *
     * @param  target the label to be implemented
     * @return this toolkit's implementation of {@code Label}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.Label
     * @see java.awt.peer.LabelPeer
     */
    default LabelPeer createLabel(Label target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code List} using the specified
     * peer interface.
     *
     * @param  target the list to be implemented
     * @return this toolkit's implementation of {@code List}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.List
     * @see java.awt.peer.ListPeer
     */
    default ListPeer createList(java.awt.List target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code Checkbox} using the
     * specified peer interface.
     *
     * @param  target the check box to be implemented
     * @return this toolkit's implementation of {@code Checkbox}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.Checkbox
     * @see java.awt.peer.CheckboxPeer
     */
    default CheckboxPeer createCheckbox(Checkbox target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code Scrollbar} using the
     * specified peer interface.
     *
     * @param  target the scroll bar to be implemented
     * @return this toolkit's implementation of {@code Scrollbar}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.Scrollbar
     * @see java.awt.peer.ScrollbarPeer
     */
    default ScrollbarPeer createScrollbar(Scrollbar target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code ScrollPane} using the
     * specified peer interface.
     *
     * @param  target the scroll pane to be implemented
     * @return this toolkit's implementation of {@code ScrollPane}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.ScrollPane
     * @see java.awt.peer.ScrollPanePeer
     * @since 1.1
     */
    default ScrollPanePeer createScrollPane(ScrollPane target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code TextArea} using the
     * specified peer interface.
     *
     * @param  target the text area to be implemented
     * @return this toolkit's implementation of {@code TextArea}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.TextArea
     * @see java.awt.peer.TextAreaPeer
     */
    default TextAreaPeer createTextArea(TextArea target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code Choice} using the
     * specified peer interface.
     *
     * @param  target the choice to be implemented
     * @return this toolkit's implementation of {@code Choice}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.Choice
     * @see java.awt.peer.ChoicePeer
     */
    default ChoicePeer createChoice(Choice target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code Frame} using the
     * specified peer interface.
     *
     * @param  target the frame to be implemented
     * @return this toolkit's implementation of {@code Frame}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.Frame
     * @see java.awt.peer.FramePeer
     */
    default FramePeer createFrame(Frame target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code Canvas} using the
     * specified peer interface.
     *
     * @param  target the canvas to be implemented
     * @return this toolkit's implementation of {@code Canvas}
     * @see java.awt.Canvas
     * @see java.awt.peer.CanvasPeer
     */
    default CanvasPeer createCanvas(Canvas target) {
        return (CanvasPeer) createComponent(target);
    }

    /**
     * Creates this toolkit's implementation of {@code Panel} using the
     * specified peer interface.
     *
     * @param  target the panel to be implemented
     * @return this toolkit's implementation of {@code Panel}
     * @see java.awt.Panel
     * @see java.awt.peer.PanelPeer
     */
    default PanelPeer createPanel(Panel target) {
        return (PanelPeer) createComponent(target);
    }

    /**
     * Creates this toolkit's implementation of {@code Window} using the
     * specified peer interface.
     *
     * @param  target the window to be implemented
     * @return this toolkit's implementation of {@code Window}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.Window
     * @see java.awt.peer.WindowPeer
     */
    default WindowPeer createWindow(Window target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code Dialog} using the
     * specified peer interface.
     *
     * @param  target the dialog to be implemented
     * @return this toolkit's implementation of {@code Dialog}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.Dialog
     * @see java.awt.peer.DialogPeer
     */
    default DialogPeer createDialog(Dialog target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code MenuBar} using the
     * specified peer interface.
     *
     * @param  target the menu bar to be implemented
     * @return this toolkit's implementation of {@code MenuBar}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.MenuBar
     * @see java.awt.peer.MenuBarPeer
     */
    default MenuBarPeer createMenuBar(MenuBar target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code Menu} using the specified
     * peer interface.
     *
     * @param  target the menu to be implemented
     * @return this toolkit's implementation of {@code Menu}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.Menu
     * @see java.awt.peer.MenuPeer
     */
    default MenuPeer createMenu(Menu target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code PopupMenu} using the
     * specified peer interface.
     *
     * @param  target the popup menu to be implemented
     * @return this toolkit's implementation of {@code PopupMenu}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.PopupMenu
     * @see java.awt.peer.PopupMenuPeer
     * @since 1.1
     */
    default PopupMenuPeer createPopupMenu(PopupMenu target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code MenuItem} using the
     * specified peer interface.
     *
     * @param  target the menu item to be implemented
     * @return this toolkit's implementation of {@code MenuItem}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.MenuItem
     * @see java.awt.peer.MenuItemPeer
     */
    default MenuItemPeer createMenuItem(MenuItem target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code FileDialog} using the
     * specified peer interface.
     *
     * @param  target the file dialog to be implemented
     * @return this toolkit's implementation of {@code FileDialog}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.FileDialog
     * @see java.awt.peer.FileDialogPeer
     */
    default FileDialogPeer createFileDialog(FileDialog target) {
        throw new HeadlessException();
    }

    /**
     * Creates this toolkit's implementation of {@code CheckboxMenuItem} using
     * the specified peer interface.
     *
     * @param  target the checkbox menu item to be implemented
     * @return this toolkit's implementation of {@code CheckboxMenuItem}
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     *         true
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.CheckboxMenuItem
     * @see java.awt.peer.CheckboxMenuItemPeer
     */
    default CheckboxMenuItemPeer createCheckboxMenuItem(CheckboxMenuItem target) {
        throw new HeadlessException();
    }

    /**
     * Creates the peer for a DragSourceContext. Always throws
     * InvalidDndOperationException if GraphicsEnvironment.isHeadless() returns
     * true.
     *
     * @param  dge the {@code DragGestureEvent}
     * @return the peer created
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    default DragSourceContextPeer createDragSourceContextPeer(DragGestureEvent dge) {
        throw new InvalidDnDOperationException("Headless environment");
    }

    /**
     * Creates this toolkit's implementation of {@code Font} using the specified
     * peer interface.
     *
     * @param name the font to be implemented
     * @param style the style of the font, such as {@code PLAIN}, {@code BOLD},
     *        {@code ITALIC}, or a combination
     * @return this toolkit's implementation of {@code Font}
     * @see java.awt.Font
     * @see java.awt.peer.FontPeer
     * @see java.awt.GraphicsEnvironment#getAllFonts
     */
    default FontPeer getFontPeer(String name, int style) {
        return null;
    }

    /**
     * Creates the peer for a Robot.
     *
     * @param  screen the GraphicsDevice indicating the coordinate system the
     *         Robot will operate in
     * @return the peer created
     * @throws AWTException if the platform configuration does not allow
     *         low-level input control
     */
    default RobotPeer createRobot(GraphicsDevice screen) throws AWTException {
        throw new AWTException(String.format("Unsupported device: %s", screen));
    }

    default DataTransferer getDataTransferer() {
        return null;
    }

    /**
     * Obtains this toolkit's implementation of helper class for {@code
     * MouseInfo} operations.
     *
     * @return this toolkit's implementation of helper for {@code MouseInfo}
     * @throws UnsupportedOperationException if this operation is not
     *         implemented
     * @see java.awt.peer.MouseInfoPeer
     * @see java.awt.MouseInfo
     * @since 1.5
     */
    default MouseInfoPeer getMouseInfoPeer() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
