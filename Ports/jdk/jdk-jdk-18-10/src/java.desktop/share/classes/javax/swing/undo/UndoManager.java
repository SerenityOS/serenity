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

package javax.swing.undo;

import javax.swing.event.*;
import javax.swing.UIManager;
import java.util.*;
import sun.swing.text.UndoableEditLockSupport;

/**
 * {@code UndoManager} manages a list of {@code UndoableEdits},
 * providing a way to undo or redo the appropriate edits.  There are
 * two ways to add edits to an <code>UndoManager</code>.  Add the edit
 * directly using the <code>addEdit</code> method, or add the
 * <code>UndoManager</code> to a bean that supports
 * <code>UndoableEditListener</code>.  The following examples creates
 * an <code>UndoManager</code> and adds it as an
 * <code>UndoableEditListener</code> to a <code>JTextField</code>:
 * <pre>
 *   UndoManager undoManager = new UndoManager();
 *   JTextField tf = ...;
 *   tf.getDocument().addUndoableEditListener(undoManager);
 * </pre>
 * <p>
 * <code>UndoManager</code> maintains an ordered list of edits and the
 * index of the next edit in that list. The index of the next edit is
 * either the size of the current list of edits, or if
 * <code>undo</code> has been invoked it corresponds to the index
 * of the last significant edit that was undone. When
 * <code>undo</code> is invoked all edits from the index of the next
 * edit to the last significant edit are undone, in reverse order.
 * For example, consider an <code>UndoManager</code> consisting of the
 * following edits: <b>A</b> <i>b</i> <i>c</i> <b>D</b>.  Edits with a
 * upper-case letter in bold are significant, those in lower-case
 * and italicized are insignificant.
 * <p>
 * <a id="figure1"></a>
 * <table class="borderless">
 * <caption style="display:none">Figure 1</caption>
 * <tr><td>
 *     <img src="doc-files/UndoManager-1.gif" alt="">
 * <tr><td style="text-align:center">Figure 1
 * </table>
 * <p>
 * As shown in <a href="#figure1">figure 1</a>, if <b>D</b> was just added, the
 * index of the next edit will be 4. Invoking <code>undo</code>
 * results in invoking <code>undo</code> on <b>D</b> and setting the
 * index of the next edit to 3 (edit <i>c</i>), as shown in the following
 * figure.
 * <p>
 * <a id="figure2"></a>
 * <table class="borderless">
 * <caption style="display:none">Figure 2</caption>
 * <tr><td>
 *     <img src="doc-files/UndoManager-2.gif" alt="">
 * <tr><td style="text-align:center">Figure 2
 * </table>
 * <p>
 * The last significant edit is <b>A</b>, so that invoking
 * <code>undo</code> again invokes <code>undo</code> on <i>c</i>,
 * <i>b</i>, and <b>A</b>, in that order, setting the index of the
 * next edit to 0, as shown in the following figure.
 * <p>
 * <a id="figure3"></a>
 * <table class="borderless">
 * <caption style="display:none">Figure 3</caption>
 * <tr><td>
 *     <img src="doc-files/UndoManager-3.gif" alt="">
 * <tr><td style="text-align:center">Figure 3
 * </table>
 * <p>
 * Invoking <code>redo</code> results in invoking <code>redo</code> on
 * all edits between the index of the next edit and the next
 * significant edit (or the end of the list).  Continuing with the previous
 * example if <code>redo</code> were invoked, <code>redo</code> would in
 * turn be invoked on <b>A</b>, <i>b</i> and <i>c</i>.  In addition
 * the index of the next edit is set to 3 (as shown in <a
 * href="#figure2">figure 2</a>).
 * <p>
 * Adding an edit to an <code>UndoManager</code> results in
 * removing all edits from the index of the next edit to the end of
 * the list.  Continuing with the previous example, if a new edit,
 * <i>e</i>, is added the edit <b>D</b> is removed from the list
 * (after having <code>die</code> invoked on it).  If <i>c</i> is not
 * incorporated by the next edit
 * (<code><i>c</i>.addEdit(<i>e</i>)</code> returns true), or replaced
 * by it (<code><i>e</i>.replaceEdit(<i>c</i>)</code> returns true),
 * the new edit is added after <i>c</i>, as shown in the following
 * figure.
 * <p>
 * <a id="figure4"></a>
 * <table class="borderless">
 * <caption style="display:none">Figure 4</caption>
 * <tr><td>
 *     <img src="doc-files/UndoManager-4.gif" alt="">
 * <tr><td style="text-align:center">Figure 4
 * </table>
 * <p>
 * Once <code>end</code> has been invoked on an <code>UndoManager</code>
 * the superclass behavior is used for all <code>UndoableEdit</code>
 * methods.  Refer to <code>CompoundEdit</code> for more details on its
 * behavior.
 * <p>
 * Unlike the rest of Swing, this class is thread safe.
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
 * @author Ray Ryan
 */
@SuppressWarnings("serial") // Same-version serialization only
public class UndoManager extends CompoundEdit implements UndoableEditListener {
    private enum Action {
        UNDO,
        REDO,
        ANY
    }
    int indexOfNextAdd;
    int limit;

    /**
     * Creates a new <code>UndoManager</code>.
     */
    public UndoManager() {
        super();
        indexOfNextAdd = 0;
        limit = 100;
        edits.ensureCapacity(limit);
    }

    /**
     * Returns the maximum number of edits this {@code UndoManager}
     * holds. A value less than 0 indicates the number of edits is not
     * limited.
     *
     * @return the maximum number of edits this {@code UndoManager} holds
     * @see #addEdit
     * @see #setLimit
     */
    public synchronized int getLimit() {
        return limit;
    }

    /**
     * Empties the undo manager sending each edit a <code>die</code> message
     * in the process.
     *
     * @see AbstractUndoableEdit#die
     */
    public synchronized void discardAllEdits() {
        for (UndoableEdit e : edits) {
            e.die();
        }
        edits = new Vector<UndoableEdit>();
        indexOfNextAdd = 0;
        // PENDING(rjrjr) when vector grows a removeRange() method
        // (expected in JDK 1.2), trimEdits() will be nice and
        // efficient, and this method can call that instead.
    }

    /**
     * Reduces the number of queued edits to a range of size limit,
     * centered on the index of the next edit.
     */
    protected void trimForLimit() {
        if (limit >= 0) {
            int size = edits.size();
//          System.out.print("limit: " + limit +
//                           " size: " + size +
//                           " indexOfNextAdd: " + indexOfNextAdd +
//                           "\n");

            if (size > limit) {
                int halfLimit = limit/2;
                int keepFrom = indexOfNextAdd - 1 - halfLimit;
                int keepTo   = indexOfNextAdd - 1 + halfLimit;

                // These are ints we're playing with, so dividing by two
                // rounds down for odd numbers, so make sure the limit was
                // honored properly. Note that the keep range is
                // inclusive.

                if (keepTo - keepFrom + 1 > limit) {
                    keepFrom++;
                }

                // The keep range is centered on indexOfNextAdd,
                // but odds are good that the actual edits Vector
                // isn't. Move the keep range to keep it legal.

                if (keepFrom < 0) {
                    keepTo -= keepFrom;
                    keepFrom = 0;
                }
                if (keepTo >= size) {
                    int delta = size - keepTo - 1;
                    keepTo += delta;
                    keepFrom += delta;
                }

//              System.out.println("Keeping " + keepFrom + " " + keepTo);
                trimEdits(keepTo+1, size-1);
                trimEdits(0, keepFrom-1);
            }
        }
    }

    /**
     * Removes edits in the specified range.
     * All edits in the given range (inclusive, and in reverse order)
     * will have <code>die</code> invoked on them and are removed from
     * the list of edits. This has no effect if
     * <code>from</code> &gt; <code>to</code>.
     *
     * @param from the minimum index to remove
     * @param to the maximum index to remove
     */
    protected void trimEdits(int from, int to) {
        if (from <= to) {
//          System.out.println("Trimming " + from + " " + to + " with index " +
//                           indexOfNextAdd);
            for (int i = to; from <= i; i--) {
                UndoableEdit e = edits.elementAt(i);
//              System.out.println("JUM: Discarding " +
//                                 e.getUndoPresentationName());
                e.die();
                // PENDING(rjrjr) when Vector supports range deletion (JDK
                // 1.2) , we can optimize the next line considerably.
                edits.removeElementAt(i);
            }

            if (indexOfNextAdd > to) {
//              System.out.print("...right...");
                indexOfNextAdd -= to-from+1;
            } else if (indexOfNextAdd >= from) {
//              System.out.println("...mid...");
                indexOfNextAdd = from;
            }

//          System.out.println("new index " + indexOfNextAdd);
        }
    }

    /**
     * Sets the maximum number of edits this <code>UndoManager</code>
     * holds. A value less than 0 indicates the number of edits is not
     * limited. If edits need to be discarded to shrink the limit,
     * <code>die</code> will be invoked on them in the reverse
     * order they were added.  The default is 100.
     *
     * @param l the new limit
     * @throws RuntimeException if this {@code UndoManager} is not in progress
     *                          ({@code end} has been invoked)
     * @see #isInProgress
     * @see #end
     * @see #addEdit
     * @see #getLimit
     */
    public synchronized void setLimit(int l) {
        if (!inProgress) throw new RuntimeException("Attempt to call UndoManager.setLimit() after UndoManager.end() has been called");
        limit = l;
        trimForLimit();
    }


    /**
     * Returns the next significant edit to be undone if <code>undo</code>
     * is invoked. This returns <code>null</code> if there are no edits
     * to be undone.
     *
     * @return the next significant edit to be undone
     */
    protected UndoableEdit editToBeUndone() {
        int i = indexOfNextAdd;
        while (i > 0) {
            UndoableEdit edit = edits.elementAt(--i);
            if (edit.isSignificant()) {
                return edit;
            }
        }

        return null;
    }

    /**
     * Returns the next significant edit to be redone if <code>redo</code>
     * is invoked. This returns <code>null</code> if there are no edits
     * to be redone.
     *
     * @return the next significant edit to be redone
     */
    protected UndoableEdit editToBeRedone() {
        int count = edits.size();
        int i = indexOfNextAdd;

        while (i < count) {
            UndoableEdit edit = edits.elementAt(i++);
            if (edit.isSignificant()) {
                return edit;
            }
        }

        return null;
    }

    /**
     * Undoes all changes from the index of the next edit to
     * <code>edit</code>, updating the index of the next edit appropriately.
     *
     * @param edit the edit to be undo to
     * @throws CannotUndoException if one of the edits throws
     *         <code>CannotUndoException</code>
     */
    protected void undoTo(UndoableEdit edit) throws CannotUndoException {
        boolean done = false;
        while (!done) {
            UndoableEdit next = edits.elementAt(--indexOfNextAdd);
            next.undo();
            done = next == edit;
        }
    }

    /**
     * Redoes all changes from the index of the next edit to
     * <code>edit</code>, updating the index of the next edit appropriately.
     *
     * @param edit the edit to be redo to
     * @throws CannotRedoException if one of the edits throws
     *         <code>CannotRedoException</code>
     */
    protected void redoTo(UndoableEdit edit) throws CannotRedoException {
        boolean done = false;
        while (!done) {
            UndoableEdit next = edits.elementAt(indexOfNextAdd++);
            next.redo();
            done = next == edit;
        }
    }

    /**
     * Convenience method that invokes one of <code>undo</code> or
     * <code>redo</code>. If any edits have been undone (the index of
     * the next edit is less than the length of the edits list) this
     * invokes <code>redo</code>, otherwise it invokes <code>undo</code>.
     *
     * @see #canUndoOrRedo
     * @see #getUndoOrRedoPresentationName
     * @throws CannotUndoException if one of the edits throws
     *         <code>CannotUndoException</code>
     * @throws CannotRedoException if one of the edits throws
     *         <code>CannotRedoException</code>
     */
    public void undoOrRedo() throws CannotRedoException, CannotUndoException {
        tryUndoOrRedo(Action.ANY);
    }

    /**
     * Returns true if it is possible to invoke <code>undo</code> or
     * <code>redo</code>.
     *
     * @return true if invoking <code>canUndoOrRedo</code> is valid
     * @see #undoOrRedo
     */
    public synchronized boolean canUndoOrRedo() {
        if (indexOfNextAdd == edits.size()) {
            return canUndo();
        } else {
            return canRedo();
        }
    }

    /**
     * Undoes the appropriate edits.  If <code>end</code> has been
     * invoked this calls through to the superclass, otherwise
     * this invokes <code>undo</code> on all edits between the
     * index of the next edit and the last significant edit, updating
     * the index of the next edit appropriately.
     *
     * @throws CannotUndoException if one of the edits throws
     *         <code>CannotUndoException</code> or there are no edits
     *         to be undone
     * @see CompoundEdit#end
     * @see #canUndo
     * @see #editToBeUndone
     */
    public void undo() throws CannotUndoException {
        tryUndoOrRedo(Action.UNDO);
    }

    /**
     * Returns true if edits may be undone.  If <code>end</code> has
     * been invoked, this returns the value from super.  Otherwise
     * this returns true if there are any edits to be undone
     * (<code>editToBeUndone</code> returns non-<code>null</code>).
     *
     * @return true if there are edits to be undone
     * @see CompoundEdit#canUndo
     * @see #editToBeUndone
     */
    public synchronized boolean canUndo() {
        if (inProgress) {
            UndoableEdit edit = editToBeUndone();
            return edit != null && edit.canUndo();
        } else {
            return super.canUndo();
        }
    }

    /**
     * Redoes the appropriate edits.  If <code>end</code> has been
     * invoked this calls through to the superclass.  Otherwise
     * this invokes <code>redo</code> on all edits between the
     * index of the next edit and the next significant edit, updating
     * the index of the next edit appropriately.
     *
     * @throws CannotRedoException if one of the edits throws
     *         <code>CannotRedoException</code> or there are no edits
     *         to be redone
     * @see CompoundEdit#end
     * @see #canRedo
     * @see #editToBeRedone
     */
    public void redo() throws CannotRedoException {
        tryUndoOrRedo(Action.REDO);
    }

    private void tryUndoOrRedo(Action action) {
        UndoableEditLockSupport lockSupport = null;
        boolean undo;
        synchronized (this) {
            if (action == Action.ANY) {
                undo = indexOfNextAdd == edits.size();
            } else {
                undo = action == Action.UNDO;
            }
            if (inProgress) {
                UndoableEdit edit = undo ? editToBeUndone() : editToBeRedone();
                if (edit == null) {
                    throw undo ? new CannotUndoException() :
                            new CannotRedoException();
                }
                lockSupport = getEditLockSupport(edit);
                if (lockSupport == null) {
                    if (undo) {
                        undoTo(edit);
                    } else {
                        redoTo(edit);
                    }
                    return;
                }
            } else {
                if (undo) {
                    super.undo();
                } else {
                    super.redo();
                }
                return;
            }
        }
        // the edit synchronization is required
        while (true) {
            lockSupport.lockEdit();
            UndoableEditLockSupport editLockSupport = null;
            try {
                synchronized (this) {
                    if (action == Action.ANY) {
                        undo = indexOfNextAdd == edits.size();
                    }
                    if (inProgress) {
                        UndoableEdit edit = undo ? editToBeUndone() :
                                editToBeRedone();
                        if (edit == null) {
                            throw undo ? new CannotUndoException() :
                                    new CannotRedoException();
                        }
                        editLockSupport = getEditLockSupport(edit);
                        if (editLockSupport == null ||
                                editLockSupport == lockSupport) {
                            if (undo) {
                                undoTo(edit);
                            } else {
                                redoTo(edit);
                            }
                            return;
                        }
                    } else {
                        if (undo) {
                            super.undo();
                        } else {
                            super.redo();
                        }
                        return;
                    }
                }
            } finally {
                if (lockSupport != null) {
                    lockSupport.unlockEdit();
                }
                lockSupport = editLockSupport;
            }
        }
    }

    private UndoableEditLockSupport getEditLockSupport(UndoableEdit anEdit) {
        return anEdit instanceof UndoableEditLockSupport ?
                (UndoableEditLockSupport)anEdit : null;
    }

    /**
     * Returns true if edits may be redone.  If <code>end</code> has
     * been invoked, this returns the value from super.  Otherwise,
     * this returns true if there are any edits to be redone
     * (<code>editToBeRedone</code> returns non-<code>null</code>).
     *
     * @return true if there are edits to be redone
     * @see CompoundEdit#canRedo
     * @see #editToBeRedone
     */
    public synchronized boolean canRedo() {
        if (inProgress) {
            UndoableEdit edit = editToBeRedone();
            return edit != null && edit.canRedo();
        } else {
            return super.canRedo();
        }
    }

    /**
     * Adds an <code>UndoableEdit</code> to this
     * <code>UndoManager</code>, if it's possible.  This removes all
     * edits from the index of the next edit to the end of the edits
     * list.  If <code>end</code> has been invoked the edit is not added
     * and <code>false</code> is returned.  If <code>end</code> hasn't
     * been invoked this returns <code>true</code>.
     *
     * @param anEdit the edit to be added
     * @return true if <code>anEdit</code> can be incorporated into this
     *              edit
     * @see CompoundEdit#end
     * @see CompoundEdit#addEdit
     */
    public synchronized boolean addEdit(UndoableEdit anEdit) {
        boolean retVal;

        // Trim from the indexOfNextAdd to the end, as we'll
        // never reach these edits once the new one is added.
        trimEdits(indexOfNextAdd, edits.size()-1);

        retVal = super.addEdit(anEdit);
        if (inProgress) {
          retVal = true;
        }

        // Maybe super added this edit, maybe it didn't (perhaps
        // an in progress compound edit took it instead. Or perhaps
        // this UndoManager is no longer in progress). So make sure
        // the indexOfNextAdd is pointed at the right place.
        indexOfNextAdd = edits.size();

        // Enforce the limit
        trimForLimit();

        return retVal;
    }


    /**
     * Turns this <code>UndoManager</code> into a normal
     * <code>CompoundEdit</code>.  This removes all edits that have
     * been undone.
     *
     * @see CompoundEdit#end
     */
    public synchronized void end() {
        super.end();
        this.trimEdits(indexOfNextAdd, edits.size()-1);
    }

    /**
     * Convenience method that returns either
     * <code>getUndoPresentationName</code> or
     * <code>getRedoPresentationName</code>.  If the index of the next
     * edit equals the size of the edits list,
     * <code>getUndoPresentationName</code> is returned, otherwise
     * <code>getRedoPresentationName</code> is returned.
     *
     * @return undo or redo name
     */
    public synchronized String getUndoOrRedoPresentationName() {
        if (indexOfNextAdd == edits.size()) {
            return getUndoPresentationName();
        } else {
            return getRedoPresentationName();
        }
    }

    /**
     * Returns a description of the undoable form of this edit.
     * If <code>end</code> has been invoked this calls into super.
     * Otherwise if there are edits to be undone, this returns
     * the value from the next significant edit that will be undone.
     * If there are no edits to be undone and <code>end</code> has not
     * been invoked this returns the value from the <code>UIManager</code>
     * property "AbstractUndoableEdit.undoText".
     *
     * @return a description of the undoable form of this edit
     * @see     #undo
     * @see     CompoundEdit#getUndoPresentationName
     */
    public synchronized String getUndoPresentationName() {
        if (inProgress) {
            if (canUndo()) {
                return editToBeUndone().getUndoPresentationName();
            } else {
                return UIManager.getString("AbstractUndoableEdit.undoText");
            }
        } else {
            return super.getUndoPresentationName();
        }
    }

    /**
     * Returns a description of the redoable form of this edit.
     * If <code>end</code> has been invoked this calls into super.
     * Otherwise if there are edits to be redone, this returns
     * the value from the next significant edit that will be redone.
     * If there are no edits to be redone and <code>end</code> has not
     * been invoked this returns the value from the <code>UIManager</code>
     * property "AbstractUndoableEdit.redoText".
     *
     * @return a description of the redoable form of this edit
     * @see     #redo
     * @see     CompoundEdit#getRedoPresentationName
     */
    public synchronized String getRedoPresentationName() {
        if (inProgress) {
            if (canRedo()) {
                return editToBeRedone().getRedoPresentationName();
            } else {
                return UIManager.getString("AbstractUndoableEdit.redoText");
            }
        } else {
            return super.getRedoPresentationName();
        }
    }

    /**
     * An <code>UndoableEditListener</code> method. This invokes
     * <code>addEdit</code> with <code>e.getEdit()</code>.
     *
     * @param e the <code>UndoableEditEvent</code> the
     *        <code>UndoableEditEvent</code> will be added from
     * @see #addEdit
     */
    public void undoableEditHappened(UndoableEditEvent e) {
        addEdit(e.getEdit());
    }

    /**
     * Returns a string that displays and identifies this
     * object's properties.
     *
     * @return a String representation of this object
     */
    public String toString() {
        return super.toString() + " limit: " + limit +
            " indexOfNextAdd: " + indexOfNextAdd;
    }
}
