/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.EventObject;
import javax.swing.event.*;

/**
 * This interface defines the methods any general editor should be able
 * to implement. <p>
 *
 * Having this interface enables complex components (the client of the
 * editor) such as <code>JTree</code> and
 * <code>JTable</code> to allow any generic editor to
 * edit values in a table cell, or tree cell, etc.  Without this generic
 * editor interface, <code>JTable</code> would have to know about specific editors,
 * such as <code>JTextField</code>, <code>JCheckBox</code>, <code>JComboBox</code>,
 * etc.  In addition, without this interface, clients of editors such as
 * <code>JTable</code> would not be able
 * to work with any editors developed in the future by the user
 * or a 3rd party ISV. <p>
 *
 * To use this interface, a developer creating a new editor can have the
 * new component implement the interface.  Or the developer can
 * choose a wrapper based approach and provide a companion object which
 * implements the <code>CellEditor</code> interface (See
 * <code>DefaultCellEditor</code> for example).  The wrapper approach
 * is particularly useful if the user want to use a 3rd party ISV
 * editor with <code>JTable</code>, but the ISV didn't implement the
 * <code>CellEditor</code> interface.  The user can simply create an object
 * that contains an instance of the 3rd party editor object and "translate"
 * the <code>CellEditor</code> API into the 3rd party editor's API.
 *
 * @see javax.swing.event.CellEditorListener
 *
 * @author Alan Chung
 * @since 1.2
 */
public interface CellEditor {

    /**
     * Returns the value contained in the editor.
     * @return the value contained in the editor
     */
    public Object getCellEditorValue();

    /**
     * Asks the editor if it can start editing using <code>anEvent</code>.
     * <code>anEvent</code> is in the invoking component coordinate system.
     * The editor can not assume the Component returned by
     * <code>getCellEditorComponent</code> is installed.  This method
     * is intended for the use of client to avoid the cost of setting up
     * and installing the editor component if editing is not possible.
     * If editing can be started this method returns true.
     *
     * @param   anEvent         the event the editor should use to consider
     *                          whether to begin editing or not
     * @return  true if editing can be started
     * @see #shouldSelectCell
     */
    public boolean isCellEditable(EventObject anEvent);

    /**
     * Returns true if the editing cell should be selected, false otherwise.
     * Typically, the return value is true, because is most cases the editing
     * cell should be selected.  However, it is useful to return false to
     * keep the selection from changing for some types of edits.
     * eg. A table that contains a column of check boxes, the user might
     * want to be able to change those checkboxes without altering the
     * selection.  (See Netscape Communicator for just such an example)
     * Of course, it is up to the client of the editor to use the return
     * value, but it doesn't need to if it doesn't want to.
     *
     * @param   anEvent         the event the editor should use to start
     *                          editing
     * @return  true if the editor would like the editing cell to be selected;
     *    otherwise returns false
     * @see #isCellEditable
     */
    public boolean shouldSelectCell(EventObject anEvent);

    /**
     * Tells the editor to stop editing and accept any partially edited
     * value as the value of the editor.  The editor returns false if
     * editing was not stopped; this is useful for editors that validate
     * and can not accept invalid entries.
     *
     * @return  true if editing was stopped; false otherwise
     */
    public boolean stopCellEditing();

    /**
     * Tells the editor to cancel editing and not accept any partially
     * edited value.
     */
    public void cancelCellEditing();

    /**
     * Adds a listener to the list that's notified when the editor
     * stops, or cancels editing.
     *
     * @param   l               the CellEditorListener
     */
    public void addCellEditorListener(CellEditorListener l);

    /**
     * Removes a listener from the list that's notified
     *
     * @param   l               the CellEditorListener
     */
    public void removeCellEditorListener(CellEditorListener l);
}
