/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Writer;
import java.io.IOException;
import java.util.Enumeration;

/**
 * AbstractWriter is an abstract class that actually
 * does the work of writing out the element tree
 * including the attributes.  In terms of how much is
 * written out per line, the writer defaults to 100.
 * But this value can be set by subclasses.
 *
 * @author Sunita Mani
 */

public abstract class AbstractWriter {

    private ElementIterator it;
    private Writer out;
    private int indentLevel = 0;
    private int indentSpace = 2;
    private Document doc = null;
    private int maxLineLength = 100;
    private int currLength = 0;
    private int startOffset = 0;
    private int endOffset = 0;
    // If (indentLevel * indentSpace) becomes >= maxLineLength, this will
    // get incremened instead of indentLevel to avoid indenting going greater
    // than line length.
    private int offsetIndent = 0;

    /**
     * String used for end of line. If the Document has the property
     * EndOfLineStringProperty, it will be used for newlines. Otherwise
     * the System property line.separator will be used. The line separator
     * can also be set.
     */
    private String lineSeparator;

    /**
     * True indicates that when writing, the line can be split, false
     * indicates that even if the line is > than max line length it should
     * not be split.
     */
    private boolean canWrapLines;

    /**
     * True while the current line is empty. This will remain true after
     * indenting.
     */
    private boolean isLineEmpty;

    /**
     * Used when indenting. Will contain the spaces.
     */
    private char[] indentChars;

    /**
     * Used when writing out a string.
     */
    private char[] tempChars;

    /**
     * This is used in <code>writeLineSeparator</code> instead of
     * tempChars. If tempChars were used it would mean write couldn't invoke
     * <code>writeLineSeparator</code> as it might have been passed
     * tempChars.
     */
    private char[] newlineChars;

    /**
     * Used for writing text.
     */
    private Segment segment;

    /**
     * How the text packages models newlines.
     * @see #getLineSeparator
     */
    protected static final char NEWLINE = '\n';


    /**
     * Creates a new AbstractWriter.
     * Initializes the ElementIterator with the default
     * root of the document.
     *
     * @param w a Writer.
     * @param doc a Document
     */
    protected AbstractWriter(Writer w, Document doc) {
        this(w, doc, 0, doc.getLength());
    }

    /**
     * Creates a new AbstractWriter.
     * Initializes the ElementIterator with the
     * element passed in.
     *
     * @param w a Writer
     * @param doc an Element
     * @param pos The location in the document to fetch the
     *   content.
     * @param len The amount to write out.
     */
    protected AbstractWriter(Writer w, Document doc, int pos, int len) {
        this.doc = doc;
        it = new ElementIterator(doc.getDefaultRootElement());
        out = w;
        startOffset = pos;
        endOffset = pos + len;
        Object docNewline = doc.getProperty(DefaultEditorKit.
                                       EndOfLineStringProperty);
        if (docNewline instanceof String) {
            setLineSeparator((String)docNewline);
        }
        else {
            String newline = System.lineSeparator();
            if (newline == null) {
                // Should not get here, but if we do it means we could not
                // find a newline string, use \n in this case.
                newline = "\n";
            }
            setLineSeparator(newline);
        }
        canWrapLines = true;
    }

    /**
     * Creates a new AbstractWriter.
     * Initializes the ElementIterator with the
     * element passed in.
     *
     * @param w a Writer
     * @param root an Element
     */
    protected AbstractWriter(Writer w, Element root) {
        this(w, root, 0, root.getEndOffset());
    }

    /**
     * Creates a new AbstractWriter.
     * Initializes the ElementIterator with the
     * element passed in.
     *
     * @param w a Writer
     * @param root an Element
     * @param pos The location in the document to fetch the
     *   content.
     * @param len The amount to write out.
     */
    protected AbstractWriter(Writer w, Element root, int pos, int len) {
        this.doc = root.getDocument();
        it = new ElementIterator(root);
        out = w;
        startOffset = pos;
        endOffset = pos + len;
        canWrapLines = true;
    }

    /**
     * Returns the first offset to be output.
     * @return the first offset to be output
     * @since 1.3
     */
    public int getStartOffset() {
        return startOffset;
    }

    /**
     * Returns the last offset to be output.
     * @return the last offset to be output
     * @since 1.3
     */
    public int getEndOffset() {
        return endOffset;
    }

    /**
     * Fetches the ElementIterator.
     *
     * @return the ElementIterator.
     */
    protected ElementIterator getElementIterator() {
        return it;
    }

    /**
     * Returns the Writer that is used to output the content.
     * @return the Writer that is used to output the content
     * @since 1.3
     */
    protected Writer getWriter() {
        return out;
    }

    /**
     * Fetches the document.
     *
     * @return the Document.
     */
    protected Document getDocument() {
        return doc;
    }

    /**
     * This method determines whether the current element
     * is in the range specified.  When no range is specified,
     * the range is initialized to be the entire document.
     * inRange() returns true if the range specified intersects
     * with the element's range.
     *
     * @param  next an Element.
     * @return boolean that indicates whether the element
     *         is in the range.
     */
    protected boolean inRange(Element next) {
        int startOffset = getStartOffset();
        int endOffset = getEndOffset();
        if ((next.getStartOffset() >= startOffset &&
             next.getStartOffset()  < endOffset) ||
            (startOffset >= next.getStartOffset() &&
             startOffset < next.getEndOffset())) {
            return true;
        }
        return false;
    }

    /**
     * This abstract method needs to be implemented
     * by subclasses.  Its responsibility is to
     * iterate over the elements and use the write()
     * methods to generate output in the desired format.
     * @throws IOException if an I/O problem has occurred
     * @throws BadLocationException for an invalid location within
     * the document
     */
    protected abstract void write() throws IOException, BadLocationException;

    /**
     * Returns the text associated with the element.
     * The assumption here is that the element is a
     * leaf element.  Throws a BadLocationException
     * when encountered.
     *
     * @param     elem an <code>Element</code>
     * @exception BadLocationException if pos represents an invalid
     *            location within the document
     * @return    the text as a <code>String</code>
     */
    protected String getText(Element elem) throws BadLocationException {
        return doc.getText(elem.getStartOffset(),
                           elem.getEndOffset() - elem.getStartOffset());
    }


    /**
     * Writes out text.  If a range is specified when the constructor
     * is invoked, then only the appropriate range of text is written
     * out.
     *
     * @param     elem an Element.
     * @exception IOException on any I/O error
     * @exception BadLocationException if pos represents an invalid
     *            location within the document.
     */
    protected void text(Element elem) throws BadLocationException,
                                             IOException {
        int start = Math.max(getStartOffset(), elem.getStartOffset());
        int end = Math.min(getEndOffset(), elem.getEndOffset());
        if (start < end) {
            if (segment == null) {
                segment = new Segment();
            }
            getDocument().getText(start, end - start, segment);
            if (segment.count > 0) {
                write(segment.array, segment.offset, segment.count);
            }
        }
    }

    /**
     * Enables subclasses to set the number of characters they
     * want written per line.   The default is 100.
     *
     * @param l the maximum line length.
     */
    protected void setLineLength(int l) {
        maxLineLength = l;
    }

    /**
     * Returns the maximum line length.
     * @return the maximum line length
     * @since 1.3
     */
    protected int getLineLength() {
        return maxLineLength;
    }

    /**
     * Sets the current line length.
     * @param length the new line length
     * @since 1.3
     */
    protected void setCurrentLineLength(int length) {
        currLength = length;
        isLineEmpty = (currLength == 0);
    }

    /**
     * Returns the current line length.
     * @return the current line length
     * @since 1.3
     */
    protected int getCurrentLineLength() {
        return currLength;
    }

    /**
     * Returns true if the current line should be considered empty. This
     * is true when <code>getCurrentLineLength</code> == 0 ||
     * <code>indent</code> has been invoked on an empty line.
     * @return true if the current line should be considered empty
     * @since 1.3
     */
    protected boolean isLineEmpty() {
        return isLineEmpty;
    }

    /**
     * Sets whether or not lines can be wrapped. This can be toggled
     * during the writing of lines. For example, outputting HTML might
     * set this to false when outputting a quoted string.
     * @param newValue new value for line wrapping
     * @since 1.3
     */
    protected void setCanWrapLines(boolean newValue) {
        canWrapLines = newValue;
    }

    /**
     * Returns whether or not the lines can be wrapped. If this is false
     * no lineSeparator's will be output.
     * @return whether or not the lines can be wrapped
     * @since 1.3
     */
    protected boolean getCanWrapLines() {
        return canWrapLines;
    }

    /**
     * Enables subclasses to specify how many spaces an indent
     * maps to. When indentation takes place, the indent level
     * is multiplied by this mapping.  The default is 2.
     *
     * @param space an int representing the space to indent mapping.
     */
    protected void setIndentSpace(int space) {
        indentSpace = space;
    }

    /**
     * Returns the amount of space to indent.
     * @return the amount of space to indent
     * @since 1.3
     */
    protected int getIndentSpace() {
        return indentSpace;
    }

    /**
     * Sets the String used to represent newlines. This is initialized
     * in the constructor from either the Document, or the System property
     * line.separator.
     * @param value the new line separator
     * @since 1.3
     */
    public void setLineSeparator(String value) {
        lineSeparator = value;
    }

    /**
     * Returns the string used to represent newlines.
     * @return the string used to represent newlines
     * @since 1.3
     */
    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * Increments the indent level. If indenting would cause
     * <code>getIndentSpace()</code> *<code>getIndentLevel()</code> to be &gt;
     * than <code>getLineLength()</code> this will not cause an indent.
     */
    protected void incrIndent() {
        // Only increment to a certain point.
        if (offsetIndent > 0) {
            offsetIndent++;
        }
        else {
            if (++indentLevel * getIndentSpace() >= getLineLength()) {
                offsetIndent++;
                --indentLevel;
            }
        }
    }

    /**
     * Decrements the indent level.
     */
    protected void decrIndent() {
        if (offsetIndent > 0) {
            --offsetIndent;
        }
        else {
            indentLevel--;
        }
    }

    /**
     * Returns the current indentation level. That is, the number of times
     * <code>incrIndent</code> has been invoked minus the number of times
     * <code>decrIndent</code> has been invoked.
     * @return the current indentation level
     * @since 1.3
     */
    protected int getIndentLevel() {
        return indentLevel;
    }

    /**
     * Does indentation. The number of spaces written
     * out is indent level times the space to map mapping. If the current
     * line is empty, this will not make it so that the current line is
     * still considered empty.
     *
     * @exception IOException on any I/O error
     */
    protected void indent() throws IOException {
        int max = getIndentLevel() * getIndentSpace();
        if (indentChars == null || max > indentChars.length) {
            indentChars = new char[max];
            for (int counter = 0; counter < max; counter++) {
                indentChars[counter] = ' ';
            }
        }
        int length = getCurrentLineLength();
        boolean wasEmpty = isLineEmpty();
        output(indentChars, 0, max);
        if (wasEmpty && length == 0) {
            isLineEmpty = true;
        }
    }

    /**
     * Writes out a character. This is implemented to invoke
     * the <code>write</code> method that takes a char[].
     *
     * @param     ch a char.
     * @exception IOException on any I/O error
     */
    protected void write(char ch) throws IOException {
        if (tempChars == null) {
            tempChars = new char[128];
        }
        tempChars[0] = ch;
        write(tempChars, 0, 1);
    }

    /**
     * Writes out a string. This is implemented to invoke the
     * <code>write</code> method that takes a char[].
     *
     * @param     content a String.
     * @exception IOException on any I/O error
     */
    protected void write(String content) throws IOException {
        if (content == null) {
            return;
        }
        int size = content.length();
        if (tempChars == null || tempChars.length < size) {
            tempChars = new char[size];
        }
        content.getChars(0, size, tempChars, 0);
        write(tempChars, 0, size);
    }

    /**
     * Writes the line separator. This invokes <code>output</code> directly
     * as well as setting the <code>lineLength</code> to 0.
     * @throws IOException on any I/O error
     * @since 1.3
     */
    protected void writeLineSeparator() throws IOException {
        String newline = getLineSeparator();
        int length = newline.length();
        if (newlineChars == null || newlineChars.length < length) {
            newlineChars = new char[length];
        }
        newline.getChars(0, length, newlineChars, 0);
        output(newlineChars, 0, length);
        setCurrentLineLength(0);
    }

    /**
     * All write methods call into this one. If <code>getCanWrapLines()</code>
     * returns false, this will call <code>output</code> with each sequence
     * of <code>chars</code> that doesn't contain a NEWLINE, followed
     * by a call to <code>writeLineSeparator</code>. On the other hand,
     * if <code>getCanWrapLines()</code> returns true, this will split the
     * string, as necessary, so <code>getLineLength</code> is honored.
     * The only exception is if the current string contains no whitespace,
     * and won't fit in which case the line length will exceed
     * <code>getLineLength</code>.
     *
     * @param chars characters to output
     * @param startIndex starting index
     * @param length length of output
     * @throws IOException on any I/O error
     * @since 1.3
     */
    protected void write(char[] chars, int startIndex, int length)
                   throws IOException {
        if (!getCanWrapLines()) {
            // We can not break string, just track if a newline
            // is in it.
            int lastIndex = startIndex;
            int endIndex = startIndex + length;
            int newlineIndex = indexOf(chars, NEWLINE, startIndex, endIndex);
            while (newlineIndex != -1) {
                if (newlineIndex > lastIndex) {
                    output(chars, lastIndex, newlineIndex - lastIndex);
                }
                writeLineSeparator();
                lastIndex = newlineIndex + 1;
                newlineIndex = indexOf(chars, '\n', lastIndex, endIndex);
            }
            if (lastIndex < endIndex) {
                output(chars, lastIndex, endIndex - lastIndex);
            }
        }
        else {
            // We can break chars if the length exceeds maxLength.
            int lastIndex = startIndex;
            int endIndex = startIndex + length;
            int lineLength = getCurrentLineLength();
            int maxLength = getLineLength();

            while (lastIndex < endIndex) {
                int newlineIndex = indexOf(chars, NEWLINE, lastIndex,
                                           endIndex);
                boolean needsNewline = false;
                boolean forceNewLine = false;

                lineLength = getCurrentLineLength();
                if (newlineIndex != -1 && (lineLength +
                              (newlineIndex - lastIndex)) < maxLength) {
                    if (newlineIndex > lastIndex) {
                        output(chars, lastIndex, newlineIndex - lastIndex);
                    }
                    lastIndex = newlineIndex + 1;
                    forceNewLine = true;
                }
                else if (newlineIndex == -1 && (lineLength +
                                (endIndex - lastIndex)) < maxLength) {
                    if (endIndex > lastIndex) {
                        output(chars, lastIndex, endIndex - lastIndex);
                    }
                    lastIndex = endIndex;
                }
                else {
                    // Need to break chars, find a place to split chars at,
                    // from lastIndex to endIndex,
                    // or maxLength - lineLength whichever is smaller
                    int breakPoint = -1;
                    int maxBreak = Math.min(endIndex - lastIndex,
                                            maxLength - lineLength - 1);
                    int counter = 0;
                    while (counter < maxBreak) {
                        if (Character.isWhitespace(chars[counter +
                                                        lastIndex])) {
                            breakPoint = counter;
                        }
                        counter++;
                    }
                    if (breakPoint != -1) {
                        // Found a place to break at.
                        breakPoint += lastIndex + 1;
                        output(chars, lastIndex, breakPoint - lastIndex);
                        lastIndex = breakPoint;
                        needsNewline = true;
                    }
                    else {
                        // No where good to break.

                        // find the next whitespace, or write out the
                        // whole string.
                            // maxBreak will be negative if current line too
                            // long.
                            counter = Math.max(0, maxBreak);
                            maxBreak = endIndex - lastIndex;
                            while (counter < maxBreak) {
                                if (Character.isWhitespace(chars[counter +
                                                                lastIndex])) {
                                    breakPoint = counter;
                                    break;
                                }
                                counter++;
                            }
                            if (breakPoint == -1) {
                                output(chars, lastIndex, endIndex - lastIndex);
                                breakPoint = endIndex;
                            }
                            else {
                                breakPoint += lastIndex;
                                if (chars[breakPoint] == NEWLINE) {
                                    output(chars, lastIndex, breakPoint++ -
                                           lastIndex);
                                forceNewLine = true;
                                }
                                else {
                                    output(chars, lastIndex, ++breakPoint -
                                              lastIndex);
                                needsNewline = true;
                                }
                            }
                            lastIndex = breakPoint;
                        }
                    }
                if (forceNewLine || needsNewline || lastIndex < endIndex) {
                    writeLineSeparator();
                    if (lastIndex < endIndex || !forceNewLine) {
                        indent();
                    }
                }
            }
        }
    }

    /**
     * Writes out the set of attributes as " &lt;name&gt;=&lt;value&gt;"
     * pairs. It throws an IOException when encountered.
     *
     * @param     attr an AttributeSet.
     * @exception IOException on any I/O error
     */
    protected void writeAttributes(AttributeSet attr) throws IOException {

        Enumeration<?> names = attr.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            write(" " + name + "=" + attr.getAttribute(name));
        }
    }

    /**
     * The last stop in writing out content. All the write methods eventually
     * make it to this method, which invokes <code>write</code> on the
     * Writer.
     * <p>This method also updates the line length based on
     * <code>length</code>. If this is invoked to output a newline, the
     * current line length will need to be reset as will no longer be
     * valid. If it is up to the caller to do this. Use
     * <code>writeLineSeparator</code> to write out a newline, which will
     * property update the current line length.
     *
     * @param content characters to output
     * @param start starting index
     * @param length length of output
     * @throws IOException on any I/O error
     * @since 1.3
     */
    protected void output(char[] content, int start, int length)
                   throws IOException {
        getWriter().write(content, start, length);
        setCurrentLineLength(getCurrentLineLength() + length);
    }

    /**
     * Support method to locate an occurrence of a particular character.
     */
    private int indexOf(char[] chars, char sChar, int startIndex,
                        int endIndex) {
        while(startIndex < endIndex) {
            if (chars[startIndex] == sChar) {
                return startIndex;
            }
            startIndex++;
        }
        return -1;
    }
}
