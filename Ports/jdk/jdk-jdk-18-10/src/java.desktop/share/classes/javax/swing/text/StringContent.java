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
package javax.swing.text;

import java.util.Vector;
import java.io.Serializable;
import javax.swing.undo.*;
import javax.swing.SwingUtilities;

/**
 * An implementation of the AbstractDocument.Content interface that is
 * a brute force implementation that is useful for relatively small
 * documents and/or debugging.  It manages the character content
 * as a simple character array.  It is also quite inefficient.
 * <p>
 * It is generally recommended that the gap buffer or piece table
 * implementations be used instead.  This buffer does not scale up
 * to large sizes.
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
 * @author  Timothy Prinzing
 */
@SuppressWarnings("serial") // Same-version serialization only
public final class StringContent implements AbstractDocument.Content, Serializable {

    /**
     * Creates a new StringContent object.  Initial size defaults to 10.
     */
    public StringContent() {
        this(10);
    }

    /**
     * Creates a new StringContent object, with the initial
     * size specified.  If the length is &lt; 1, a size of 1 is used.
     *
     * @param initialLength the initial size
     */
    public StringContent(int initialLength) {
        if (initialLength < 1) {
            initialLength = 1;
        }
        data = new char[initialLength];
        data[0] = '\n';
        count = 1;
    }

    /**
     * Returns the length of the content.
     *
     * @return the length &gt;= 1
     * @see AbstractDocument.Content#length
     */
    public int length() {
        return count;
    }

    /**
     * Inserts a string into the content.
     *
     * @param where the starting position &gt;= 0 &amp;&amp; &lt; length()
     * @param str the non-null string to insert
     * @return an UndoableEdit object for undoing
     * @exception BadLocationException if the specified position is invalid
     * @see AbstractDocument.Content#insertString
     */
    public UndoableEdit insertString(int where, String str) throws BadLocationException {
        if (where >= count || where < 0) {
            throw new BadLocationException("Invalid location", count);
        }
        char[] chars = str.toCharArray();
        replace(where, 0, chars, 0, chars.length);
        if (marks != null) {
            updateMarksForInsert(where, str.length());
        }
        return new InsertUndo(where, str.length());
    }

    /**
     * Removes part of the content.  where + nitems must be &lt; length().
     *
     * @param where the starting position &gt;= 0
     * @param nitems the number of characters to remove &gt;= 0
     * @return an UndoableEdit object for undoing
     * @exception BadLocationException if the specified position is invalid
     * @see AbstractDocument.Content#remove
     */
    public UndoableEdit remove(int where, int nitems) throws BadLocationException {
        if (where + nitems >= count) {
            throw new BadLocationException("Invalid range", count);
        }
        String removedString = getString(where, nitems);
        UndoableEdit edit = new RemoveUndo(where, removedString);
        replace(where, nitems, empty, 0, 0);
        if (marks != null) {
            updateMarksForRemove(where, nitems);
        }
        return edit;

    }

    /**
     * Retrieves a portion of the content.  where + len must be &lt;= length().
     *
     * @param where the starting position &gt;= 0
     * @param len the length to retrieve &gt;= 0
     * @return a string representing the content; may be empty
     * @exception BadLocationException if the specified position is invalid
     * @see AbstractDocument.Content#getString
     */
    public String getString(int where, int len) throws BadLocationException {
        if (where + len > count) {
            throw new BadLocationException("Invalid range", count);
        }
        return new String(data, where, len);
    }

    /**
     * Retrieves a portion of the content.  where + len must be &lt;= length()
     *
     * @param where the starting position &gt;= 0
     * @param len the number of characters to retrieve &gt;= 0
     * @param chars the Segment object to return the characters in
     * @exception BadLocationException if the specified position is invalid
     * @see AbstractDocument.Content#getChars
     */
    public void getChars(int where, int len, Segment chars) throws BadLocationException {
        if (where + len > count) {
            throw new BadLocationException("Invalid location", count);
        }
        chars.array = data;
        chars.offset = where;
        chars.count = len;
    }

    /**
     * Creates a position within the content that will
     * track change as the content is mutated.
     *
     * @param offset the offset to create a position for &gt;= 0
     * @return the position
     * @exception BadLocationException if the specified position is invalid
     */
    public Position createPosition(int offset) throws BadLocationException {
        // some small documents won't have any sticky positions
        // at all, so the buffer is created lazily.
        if (marks == null) {
            marks = new Vector<PosRec>();
        }
        return new StickyPosition(offset);
    }

    // --- local methods ---------------------------------------

    /**
     * Replaces some of the characters in the array
     * @param offset  offset into the array to start the replace
     * @param length  number of characters to remove
     * @param replArray replacement array
     * @param replOffset offset into the replacement array
     * @param replLength number of character to use from the
     *   replacement array.
     */
    void replace(int offset, int length,
                 char[] replArray, int replOffset, int replLength) {
        int delta = replLength - length;
        int src = offset + length;
        int nmove = count - src;
        int dest = src + delta;
        if ((count + delta) >= data.length) {
            // need to grow the array
            int newLength = Math.max(2*data.length, count + delta);
            char[] newData = new char[newLength];
            System.arraycopy(data, 0, newData, 0, offset);
            System.arraycopy(replArray, replOffset, newData, offset, replLength);
            System.arraycopy(data, src, newData, dest, nmove);
            data = newData;
        } else {
            // patch the existing array
            System.arraycopy(data, src, data, dest, nmove);
            System.arraycopy(replArray, replOffset, data, offset, replLength);
        }
        count = count + delta;
    }

    void resize(int ncount) {
        char[] ndata = new char[ncount];
        System.arraycopy(data, 0, ndata, 0, Math.min(ncount, count));
        data = ndata;
    }

    synchronized void updateMarksForInsert(int offset, int length) {
        if (offset == 0) {
            // zero is a special case where we update only
            // marks after it.
            offset = 1;
        }
        int n = marks.size();
        for (int i = 0; i < n; i++) {
            PosRec mark = marks.elementAt(i);
            if (mark.unused) {
                // this record is no longer used, get rid of it
                marks.removeElementAt(i);
                i -= 1;
                n -= 1;
            } else if (mark.offset >= offset) {
                mark.offset += length;
            }
        }
    }

    synchronized void updateMarksForRemove(int offset, int length) {
        int n = marks.size();
        for (int i = 0; i < n; i++) {
            PosRec mark = marks.elementAt(i);
            if (mark.unused) {
                // this record is no longer used, get rid of it
                marks.removeElementAt(i);
                i -= 1;
                n -= 1;
            } else if (mark.offset >= (offset + length)) {
                mark.offset -= length;
            } else if (mark.offset >= offset) {
                mark.offset = offset;
            }
        }
    }

    /**
     * Returns a Vector containing instances of UndoPosRef for the
     * Positions in the range
     * <code>offset</code> to <code>offset</code> + <code>length</code>.
     * If <code>v</code> is not null the matching Positions are placed in
     * there. The vector with the resulting Positions are returned.
     * <p>
     * This is meant for internal usage, and is generally not of interest
     * to subclasses.
     *
     * @param v the Vector to use, with a new one created on null
     * @param offset the starting offset &gt;= 0
     * @param length the length &gt;= 0
     * @return the set of instances
     */
    @SuppressWarnings({"rawtypes", "unchecked"}) // UndoPosRef type cannot be exposed
    protected Vector getPositionsInRange(Vector v, int offset,
                                         int length) {
        int n = marks.size();
        int end = offset + length;
        Vector placeIn = (v == null) ? new Vector() : v;
        for (int i = 0; i < n; i++) {
            PosRec mark = marks.elementAt(i);
            if (mark.unused) {
                // this record is no longer used, get rid of it
                marks.removeElementAt(i);
                i -= 1;
                n -= 1;
            } else if(mark.offset >= offset && mark.offset <= end)
                placeIn.addElement(new UndoPosRef(mark));
        }
        return placeIn;
    }

    /**
     * Resets the location for all the UndoPosRef instances
     * in <code>positions</code>.
     * <p>
     * This is meant for internal usage, and is generally not of interest
     * to subclasses.
     *
     * @param positions the positions of the instances
     */
    @SuppressWarnings("rawtypes") // UndoPosRef type cannot be exposed
    protected void updateUndoPositions(Vector positions) {
        for(int counter = positions.size() - 1; counter >= 0; counter--) {
            UndoPosRef ref = (UndoPosRef) positions.elementAt(counter);
            // Check if the Position is still valid.
            if(ref.rec.unused) {
                positions.removeElementAt(counter);
            }
            else
                ref.resetLocation();
        }
    }

    private static final char[] empty = new char[0];
    private char[] data;
    private int count;
    transient Vector<PosRec> marks;

    /**
     * holds the data for a mark... separately from
     * the real mark so that the real mark can be
     * collected if there are no more references to
     * it.... the update table holds only a reference
     * to this grungy thing.
     */
    final class PosRec {

        PosRec(int offset) {
            this.offset = offset;
        }

        int offset;
        boolean unused;
    }

    /**
     * This really wants to be a weak reference but
     * in 1.1 we don't have a 100% pure solution for
     * this... so this class trys to hack a solution
     * to causing the marks to be collected.
     */
    final class StickyPosition implements Position {

        StickyPosition(int offset) {
            rec = new PosRec(offset);
            marks.addElement(rec);
        }

        public int getOffset() {
            return rec.offset;
        }

        @SuppressWarnings("deprecation")
        protected void finalize() throws Throwable {
            // schedule the record to be removed later
            // on another thread.
            rec.unused = true;
        }

        public String toString() {
            return Integer.toString(getOffset());
        }

        PosRec rec;
    }

    /**
     * Used to hold a reference to a Position that is being reset as the
     * result of removing from the content.
     */
    final class UndoPosRef {
        UndoPosRef(PosRec rec) {
            this.rec = rec;
            this.undoLocation = rec.offset;
        }

        /**
         * Resets the location of the Position to the offset when the
         * receiver was instantiated.
         */
        protected void resetLocation() {
            rec.offset = undoLocation;
        }

        /** Location to reset to when resetLocatino is invoked. */
        protected int undoLocation;
        /** Position to reset offset. */
        protected PosRec rec;
    }

    /**
     * UnoableEdit created for inserts.
     */
    class InsertUndo extends AbstractUndoableEdit {
        protected InsertUndo(int offset, int length) {
            super();
            this.offset = offset;
            this.length = length;
        }

        public void undo() throws CannotUndoException {
            super.undo();
            try {
                synchronized(StringContent.this) {
                    // Get the Positions in the range being removed.
                    if(marks != null)
                        posRefs = getPositionsInRange(null, offset, length);
                    string = getString(offset, length);
                    remove(offset, length);
                }
            } catch (BadLocationException bl) {
              throw new CannotUndoException();
            }
        }

        public void redo() throws CannotRedoException {
            super.redo();
            try {
                synchronized(StringContent.this) {
                    insertString(offset, string);
                    string = null;
                    // Update the Positions that were in the range removed.
                    if(posRefs != null) {
                        updateUndoPositions(posRefs);
                        posRefs = null;
                    }
              }
            } catch (BadLocationException bl) {
              throw new CannotRedoException();
            }
        }

        // Where the string goes.
        protected int offset;
        // Length of the string.
        protected int length;
        // The string that was inserted. To cut down on space needed this
        // will only be valid after an undo.
        protected String string;
        // An array of instances of UndoPosRef for the Positions in the
        // range that was removed, valid after undo.
        @SuppressWarnings("rawtypes") // UndoPosRef type cannot be exposed
        protected Vector posRefs;
    }


    /**
     * UndoableEdit created for removes.
     */
    class RemoveUndo extends AbstractUndoableEdit {
        @SuppressWarnings("unchecked")
        protected RemoveUndo(int offset, String string) {
            super();
            this.offset = offset;
            this.string = string;
            this.length = string.length();
            if(marks != null)
                posRefs = getPositionsInRange(null, offset, length);
        }

        public void undo() throws CannotUndoException {
            super.undo();
            try {
                synchronized(StringContent.this) {
                    insertString(offset, string);
                    // Update the Positions that were in the range removed.
                    if(posRefs != null) {
                        updateUndoPositions(posRefs);
                        posRefs = null;
                    }
                    string = null;
                }
            } catch (BadLocationException bl) {
              throw new CannotUndoException();
            }
        }

        @SuppressWarnings("unchecked")
        public void redo() throws CannotRedoException {
            super.redo();
            try {
                synchronized(StringContent.this) {
                    string = getString(offset, length);
                    // Get the Positions in the range being removed.
                    if(marks != null)
                        posRefs = getPositionsInRange(null, offset, length);
                    remove(offset, length);
                }
            } catch (BadLocationException bl) {
              throw new CannotRedoException();
            }
        }

        // Where the string goes.
        protected int offset;
        // Length of the string.
        protected int length;
        // The string that was inserted. This will be null after an undo.
        protected String string;
        // An array of instances of UndoPosRef for the Positions in the
        // range that was removed, valid before undo.
        protected Vector<UndoPosRef> posRefs;
    }
}
