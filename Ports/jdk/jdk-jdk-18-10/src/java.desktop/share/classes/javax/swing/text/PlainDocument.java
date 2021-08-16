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
package javax.swing.text;

import java.util.Vector;

/**
 * A plain document that maintains no character attributes.  The
 * default element structure for this document is a map of the lines in
 * the text.  The Element returned by getDefaultRootElement is
 * a map of the lines, and each child element represents a line.
 * This model does not maintain any character level attributes,
 * but each line can be tagged with an arbitrary set of attributes.
 * Line to offset, and offset to line translations can be quickly
 * performed using the default root element.  The structure information
 * of the DocumentEvent's fired by edits will indicate the line
 * structure changes.
 * <p>
 * The default content storage management is performed by a
 * gapped buffer implementation (GapContent).  It supports
 * editing reasonably large documents with good efficiency when
 * the edits are contiguous or clustered, as is typical.
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
 * @see     Document
 * @see     AbstractDocument
 */
@SuppressWarnings("serial") // Same-version serialization only
public class PlainDocument extends AbstractDocument {

    /**
     * Name of the attribute that specifies the tab
     * size for tabs contained in the content.  The
     * type for the value is Integer.
     */
    public static final String tabSizeAttribute = "tabSize";

    /**
     * Name of the attribute that specifies the maximum
     * length of a line, if there is a maximum length.
     * The type for the value is Integer.
     */
    public static final String lineLimitAttribute = "lineLimit";

    /**
     * Constructs a plain text document.  A default model using
     * <code>GapContent</code> is constructed and set.
     */
    public PlainDocument() {
        this(new GapContent());
    }

    /**
     * Constructs a plain text document.  A default root element is created,
     * and the tab size set to 8.
     *
     * @param c  the container for the content
     */
    public PlainDocument(Content c) {
        super(c);
        putProperty(tabSizeAttribute, Integer.valueOf(8));
        defaultRoot = createDefaultRoot();
    }

    /**
     * Inserts some content into the document.
     * Inserting content causes a write lock to be held while the
     * actual changes are taking place, followed by notification
     * to the observers on the thread that grabbed the write lock.
     * <p>
     * This method is thread safe, although most Swing methods
     * are not. Please see
     * <A HREF="https://docs.oracle.com/javase/tutorial/uiswing/concurrency/index.html">Concurrency
     * in Swing</A> for more information.
     *
     * @param offs the starting offset &gt;= 0
     * @param str the string to insert; does nothing with null/empty strings
     * @param a the attributes for the inserted content
     * @exception BadLocationException  the given insert position is not a valid
     *   position within the document
     * @see Document#insertString
     */
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        // fields don't want to have multiple lines.  We may provide a field-specific
        // model in the future in which case the filtering logic here will no longer
        // be needed.
        Object filterNewlines = getProperty("filterNewlines");
        if ((filterNewlines instanceof Boolean) && filterNewlines.equals(Boolean.TRUE)) {
            if ((str != null) && (str.indexOf('\n') >= 0)) {
                StringBuilder filtered = new StringBuilder(str);
                int n = filtered.length();
                for (int i = 0; i < n; i++) {
                    if (filtered.charAt(i) == '\n') {
                        filtered.setCharAt(i, ' ');
                    }
                }
                str = filtered.toString();
            }
        }
        super.insertString(offs, str, a);
    }

    /**
     * Gets the default root element for the document model.
     *
     * @return the root
     * @see Document#getDefaultRootElement
     */
    public Element getDefaultRootElement() {
        return defaultRoot;
    }

    /**
     * Creates the root element to be used to represent the
     * default document structure.
     *
     * @return the element base
     */
    protected AbstractElement createDefaultRoot() {
        BranchElement map = (BranchElement) createBranchElement(null, null);
        Element line = createLeafElement(map, null, 0, 1);
        Element[] lines = new Element[1];
        lines[0] = line;
        map.replace(0, 0, lines);
        return map;
    }

    /**
     * Get the paragraph element containing the given position.  Since this
     * document only models lines, it returns the line instead.
     */
    public Element getParagraphElement(int pos){
        Element lineMap = getDefaultRootElement();
        return lineMap.getElement( lineMap.getElementIndex( pos ) );
    }

    /**
     * Updates document structure as a result of text insertion.  This
     * will happen within a write lock.  Since this document simply
     * maps out lines, we refresh the line map.
     *
     * @param chng the change event describing the dit
     * @param attr the set of attributes for the inserted text
     */
    protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attr) {
        removed.removeAllElements();
        added.removeAllElements();
        BranchElement lineMap = (BranchElement) getDefaultRootElement();
        int offset = chng.getOffset();
        int length = chng.getLength();
        if (offset > 0) {
          offset -= 1;
          length += 1;
        }
        int index = lineMap.getElementIndex(offset);
        Element rmCandidate = lineMap.getElement(index);
        int rmOffs0 = rmCandidate.getStartOffset();
        int rmOffs1 = rmCandidate.getEndOffset();
        int lastOffset = rmOffs0;
        try {
            if (s == null) {
                s = new Segment();
            }
            getContent().getChars(offset, length, s);
            boolean hasBreaks = false;
            for (int i = 0; i < length; i++) {
                char c = s.array[s.offset + i];
                if (c == '\n') {
                    int breakOffset = offset + i + 1;
                    added.addElement(createLeafElement(lineMap, null, lastOffset, breakOffset));
                    lastOffset = breakOffset;
                    hasBreaks = true;
                }
            }
            if (hasBreaks) {
                removed.addElement(rmCandidate);
                if ((offset + length == rmOffs1) && (lastOffset != rmOffs1) &&
                    ((index+1) < lineMap.getElementCount())) {
                    Element e = lineMap.getElement(index+1);
                    removed.addElement(e);
                    rmOffs1 = e.getEndOffset();
                }
                if (lastOffset < rmOffs1) {
                    added.addElement(createLeafElement(lineMap, null, lastOffset, rmOffs1));
                }

                Element[] aelems = new Element[added.size()];
                added.copyInto(aelems);
                Element[] relems = new Element[removed.size()];
                removed.copyInto(relems);
                ElementEdit ee = new ElementEdit(lineMap, index, relems, aelems);
                chng.addEdit(ee);
                lineMap.replace(index, relems.length, aelems);
            }
            if (Utilities.isComposedTextAttributeDefined(attr)) {
                insertComposedTextUpdate(chng, attr);
            }
        } catch (BadLocationException e) {
            throw new Error("Internal error: " + e.toString());
        }
        super.insertUpdate(chng, attr);
    }

    /**
     * Updates any document structure as a result of text removal.
     * This will happen within a write lock. Since the structure
     * represents a line map, this just checks to see if the
     * removal spans lines.  If it does, the two lines outside
     * of the removal area are joined together.
     *
     * @param chng the change event describing the edit
     */
    protected void removeUpdate(DefaultDocumentEvent chng) {
        removed.removeAllElements();
        BranchElement map = (BranchElement) getDefaultRootElement();
        int offset = chng.getOffset();
        int length = chng.getLength();
        int line0 = map.getElementIndex(offset);
        int line1 = map.getElementIndex(offset + length);
        if (line0 != line1) {
            // a line was removed
            for (int i = line0; i <= line1; i++) {
                removed.addElement(map.getElement(i));
            }
            int p0 = map.getElement(line0).getStartOffset();
            int p1 = map.getElement(line1).getEndOffset();
            Element[] aelems = new Element[1];
            aelems[0] = createLeafElement(map, null, p0, p1);
            Element[] relems = new Element[removed.size()];
            removed.copyInto(relems);
            ElementEdit ee = new ElementEdit(map, line0, relems, aelems);
            chng.addEdit(ee);
            map.replace(line0, relems.length, aelems);
        } else {
            //Check for the composed text element
            Element line = map.getElement(line0);
            if (!line.isLeaf()) {
                Element leaf = line.getElement(line.getElementIndex(offset));
                if (Utilities.isComposedTextElement(leaf)) {
                    Element[] aelem = new Element[1];
                    aelem[0] = createLeafElement(map, null,
                        line.getStartOffset(), line.getEndOffset());
                    Element[] relem = new Element[1];
                    relem[0] = line;
                    ElementEdit ee = new ElementEdit(map, line0, relem, aelem);
                    chng.addEdit(ee);
                    map.replace(line0, 1, aelem);
                }
            }
        }
        super.removeUpdate(chng);
    }

    //
    // Inserts the composed text of an input method. The line element
    // where the composed text is inserted into becomes an branch element
    // which contains leaf elements of the composed text and the text
    // backing store.
    //
    private void insertComposedTextUpdate(DefaultDocumentEvent chng, AttributeSet attr) {
        added.removeAllElements();
        BranchElement lineMap = (BranchElement) getDefaultRootElement();
        int offset = chng.getOffset();
        int length = chng.getLength();
        int index = lineMap.getElementIndex(offset);
        Element elem = lineMap.getElement(index);
        int elemStart = elem.getStartOffset();
        int elemEnd = elem.getEndOffset();
        BranchElement[] abelem = new BranchElement[1];
        abelem[0] = (BranchElement) createBranchElement(lineMap, null);
        Element[] relem = new Element[1];
        relem[0] = elem;
        if (elemStart != offset)
            added.addElement(createLeafElement(abelem[0], null, elemStart, offset));
        added.addElement(createLeafElement(abelem[0], attr, offset, offset+length));
        if (elemEnd != offset+length)
            added.addElement(createLeafElement(abelem[0], null, offset+length, elemEnd));
        Element[] alelem = new Element[added.size()];
        added.copyInto(alelem);
        ElementEdit ee = new ElementEdit(lineMap, index, relem, abelem);
        chng.addEdit(ee);

        abelem[0].replace(0, 0, alelem);
        lineMap.replace(index, 1, abelem);
    }

    private AbstractElement defaultRoot;
    private Vector<Element> added = new Vector<Element>();
    private Vector<Element> removed = new Vector<Element>();
    private transient Segment s;
}
