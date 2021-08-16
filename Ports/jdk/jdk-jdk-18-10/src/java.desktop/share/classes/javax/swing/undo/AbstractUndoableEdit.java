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

package javax.swing.undo;

import java.io.Serializable;
import javax.swing.UIManager;

/**
 * An abstract implementation of <code>UndoableEdit</code>,
 * implementing simple responses to all boolean methods in
 * that interface.
 *
 * @author Ray Ryan
 */
@SuppressWarnings("serial") // Same-version serialization only
public class AbstractUndoableEdit implements UndoableEdit, Serializable {

    /**
     * String returned by <code>getUndoPresentationName</code>;
     * as of Java 2 platform v1.3.1 this field is no longer used. This value
     * is now localized and comes from the defaults table with key
     * <code>AbstractUndoableEdit.undoText</code>.
     *
     * @see javax.swing.UIDefaults
     */
    protected static final String UndoName = "Undo";

    /**
     * String returned by <code>getRedoPresentationName</code>;
     * as of Java 2 platform v1.3.1 this field is no longer used. This value
     * is now localized and comes from the defaults table with key
     * <code>AbstractUndoableEdit.redoText</code>.
     *
     * @see javax.swing.UIDefaults
     */
    protected static final String RedoName = "Redo";

    /**
     * Defaults to true; becomes false if this edit is undone, true
     * again if it is redone.
     */
    boolean hasBeenDone;

    /**
     * True if this edit has not received <code>die</code>; defaults
     * to <code>true</code>.
     */
    boolean alive;

    /**
     * Creates an <code>AbstractUndoableEdit</code> which defaults
     * <code>hasBeenDone</code> and <code>alive</code> to <code>true</code>.
     */
    public AbstractUndoableEdit() {
        super();

        hasBeenDone = true;
        alive = true;
    }

    /**
     * Sets <code>alive</code> to false. Note that this
     * is a one way operation; dead edits cannot be resurrected.
     * Sending <code>undo</code> or <code>redo</code> to
     * a dead edit results in an exception being thrown.
     *
     * <p>Typically an edit is killed when it is consolidated by
     * another edit's <code>addEdit</code> or <code>replaceEdit</code>
     * method, or when it is dequeued from an <code>UndoManager</code>.
     */
    public void die() {
        alive = false;
    }

    /**
     * Throws <code>CannotUndoException</code> if <code>canUndo</code>
     * returns <code>false</code>. Sets <code>hasBeenDone</code>
     * to <code>false</code>. Subclasses should override to undo the
     * operation represented by this edit. Override should begin with
     * a call to super.
     *
     * @exception CannotUndoException if <code>canUndo</code>
     *    returns <code>false</code>
     * @see     #canUndo
     */
    public void undo() throws CannotUndoException {
        if (!canUndo()) {
            throw new CannotUndoException();
        }
        hasBeenDone = false;
    }

    /**
     * Returns true if this edit is <code>alive</code>
     * and <code>hasBeenDone</code> is <code>true</code>.
     *
     * @return true if this edit is <code>alive</code>
     *    and <code>hasBeenDone</code> is <code>true</code>
     *
     * @see     #die
     * @see     #undo
     * @see     #redo
     */
    public boolean canUndo() {
        return alive && hasBeenDone;
    }

    /**
     * Throws <code>CannotRedoException</code> if <code>canRedo</code>
     * returns false. Sets <code>hasBeenDone</code> to <code>true</code>.
     * Subclasses should override to redo the operation represented by
     * this edit. Override should begin with a call to super.
     *
     * @exception CannotRedoException if <code>canRedo</code>
     *     returns <code>false</code>
     * @see     #canRedo
     */
    public void redo() throws CannotRedoException {
        if (!canRedo()) {
            throw new CannotRedoException();
        }
        hasBeenDone = true;
    }

    /**
     * Returns <code>true</code> if this edit is <code>alive</code>
     * and <code>hasBeenDone</code> is <code>false</code>.
     *
     * @return <code>true</code> if this edit is <code>alive</code>
     *   and <code>hasBeenDone</code> is <code>false</code>
     * @see     #die
     * @see     #undo
     * @see     #redo
     */
    public boolean canRedo() {
        return alive && !hasBeenDone;
    }

    /**
     * This default implementation returns false.
     *
     * @param anEdit the edit to be added
     * @return false
     *
     * @see UndoableEdit#addEdit
     */
    public boolean addEdit(UndoableEdit anEdit) {
        return false;
    }

    /**
     * This default implementation returns false.
     *
     * @param anEdit the edit to replace
     * @return false
     *
     * @see UndoableEdit#replaceEdit
     */
    public boolean replaceEdit(UndoableEdit anEdit) {
        return false;
    }

    /**
     * This default implementation returns true.
     *
     * @return true
     * @see UndoableEdit#isSignificant
     */
    public boolean isSignificant() {
        return true;
    }

    /**
     * This default implementation returns "". Used by
     * <code>getUndoPresentationName</code> and
     * <code>getRedoPresentationName</code> to
     * construct the strings they return. Subclasses should override to
     * return an appropriate description of the operation this edit
     * represents.
     *
     * @return the empty string ""
     *
     * @see     #getUndoPresentationName
     * @see     #getRedoPresentationName
     */
    public String getPresentationName() {
        return "";
    }

    /**
     * Retreives the value from the defaults table with key
     * <code>AbstractUndoableEdit.undoText</code> and returns
     * that value followed by a space, followed by
     * <code>getPresentationName</code>.
     * If <code>getPresentationName</code> returns "",
     * then the defaults value is returned alone.
     *
     * @return the value from the defaults table with key
     *    <code>AbstractUndoableEdit.undoText</code>, followed
     *    by a space, followed by <code>getPresentationName</code>
     *    unless <code>getPresentationName</code> is "" in which
     *    case, the defaults value is returned alone.
     * @see #getPresentationName
     */
    public String getUndoPresentationName() {
        String name = getPresentationName();
        if (!"".equals(name)) {
            name = UIManager.getString("AbstractUndoableEdit.undoText") +
                " " + name;
        } else {
            name = UIManager.getString("AbstractUndoableEdit.undoText");
        }

        return name;
    }

    /**
     * Retreives the value from the defaults table with key
     * <code>AbstractUndoableEdit.redoText</code> and returns
     * that value followed by a space, followed by
     * <code>getPresentationName</code>.
     * If <code>getPresentationName</code> returns "",
     * then the defaults value is returned alone.
     *
     * @return the value from the defaults table with key
     *    <code>AbstractUndoableEdit.redoText</code>, followed
     *    by a space, followed by <code>getPresentationName</code>
     *    unless <code>getPresentationName</code> is "" in which
     *    case, the defaults value is returned alone.
     * @see #getPresentationName
     */
    public String getRedoPresentationName() {
        String name = getPresentationName();
        if (!"".equals(name)) {
            name = UIManager.getString("AbstractUndoableEdit.redoText") +
                " " + name;
        } else {
            name = UIManager.getString("AbstractUndoableEdit.redoText");
        }

        return name;
    }

    /**
     * Returns a string that displays and identifies this
     * object's properties.
     *
     * @return a String representation of this object
     */
    public String toString()
    {
        return super.toString()
            + " hasBeenDone: " + hasBeenDone
            + " alive: " + alive;
    }
}
