/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.undo;

import javax.swing.event.*;
import java.util.*;

/**
 * A support class used for managing <code>UndoableEdit</code> listeners.
 *
 * @author Ray Ryan
 */
public class UndoableEditSupport {
    /**
     * The update level.
     */
    protected int updateLevel;
    /**
     * The compound edit.
     */
    protected CompoundEdit compoundEdit;
    /**
     * The list of listeners.
     */
    protected Vector<UndoableEditListener> listeners;
    /**
     * The real source.
     */
    protected Object realSource;

    /**
     * Constructs an <code>UndoableEditSupport</code> object.
     */
    public UndoableEditSupport() {
        this(null);
    }

    /**
     * Constructs an <code>UndoableEditSupport</code> object.
     *
     * @param r  an <code>Object</code>
     */
    public UndoableEditSupport(Object r) {
        realSource = r == null ? this : r;
        updateLevel = 0;
        compoundEdit = null;
        listeners = new Vector<UndoableEditListener>();
    }

    /**
     * Registers an <code>UndoableEditListener</code>.
     * The listener is notified whenever an edit occurs which can be undone.
     *
     * @param l  an <code>UndoableEditListener</code> object
     * @see #removeUndoableEditListener
     */
    public synchronized void addUndoableEditListener(UndoableEditListener l) {
        listeners.addElement(l);
    }

    /**
     * Removes an <code>UndoableEditListener</code>.
     *
     * @param l  the <code>UndoableEditListener</code> object to be removed
     * @see #addUndoableEditListener
     */
    public synchronized void removeUndoableEditListener(UndoableEditListener l)
    {
        listeners.removeElement(l);
    }

    /**
     * Returns an array of all the <code>UndoableEditListener</code>s added
     * to this UndoableEditSupport with addUndoableEditListener().
     *
     * @return all of the <code>UndoableEditListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    public synchronized UndoableEditListener[] getUndoableEditListeners() {
        return listeners.toArray(new UndoableEditListener[0]);
    }

    /**
     * Called only from <code>postEdit</code> and <code>endUpdate</code>. Calls
     * <code>undoableEditHappened</code> in all listeners. No synchronization
     * is performed here, since the two calling methods are synchronized.
     *
     * @param e edit to be verified
     */
    protected void _postEdit(UndoableEdit e) {
        UndoableEditEvent ev = new UndoableEditEvent(realSource, e);
        @SuppressWarnings("unchecked")
        Enumeration<UndoableEditListener> cursor =
            ((Vector<UndoableEditListener>)listeners.clone()).elements();
        while (cursor.hasMoreElements()) {
            cursor.nextElement().undoableEditHappened(ev);
        }
    }

    /**
     * DEADLOCK WARNING: Calling this method may call
     * <code>undoableEditHappened</code> in all listeners.
     * It is unwise to call this method from one of its listeners.
     *
     * @param e edit to be posted
     */
    public synchronized void postEdit(UndoableEdit e) {
        if (updateLevel == 0) {
            _postEdit(e);
        } else {
            // PENDING(rjrjr) Throw an exception if this fails?
            compoundEdit.addEdit(e);
        }
    }

    /**
     * Returns the update level value.
     *
     * @return an integer representing the update level
     */
    public int getUpdateLevel() {
        return updateLevel;
    }

    /**
     *
     */
    public synchronized void beginUpdate() {
        if (updateLevel == 0) {
            compoundEdit = createCompoundEdit();
        }
        updateLevel++;
    }

    /**
     * Called only from <code>beginUpdate</code>.
     * Exposed here for subclasses' use.
     *
     * @return new created {@code CompoundEdit} object
     */
    protected CompoundEdit createCompoundEdit() {
        return new CompoundEdit();
    }

    /**
     * DEADLOCK WARNING: Calling this method may call
     * <code>undoableEditHappened</code> in all listeners.
     * It is unwise to call this method from one of its listeners.
     */
    public synchronized void endUpdate() {
        updateLevel--;
        if (updateLevel == 0) {
            compoundEdit.end();
            _postEdit(compoundEdit);
            compoundEdit = null;
        }
    }

    /**
     * Returns a string that displays and identifies this
     * object's properties.
     *
     * @return a <code>String</code> representation of this object
     */
    public String toString() {
        return super.toString() +
            " updateLevel: " + updateLevel +
            " listeners: " + listeners +
            " compoundEdit: " + compoundEdit;
    }
}
