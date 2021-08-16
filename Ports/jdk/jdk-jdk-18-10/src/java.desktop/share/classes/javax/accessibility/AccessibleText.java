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

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.text.AttributeSet;

/**
 * The {@code AccessibleText} interface should be implemented by all classes
 * that present textual information on the display. This interface provides the
 * standard mechanism for an assistive technology to access that text via its
 * content, attributes, and spatial location. Applications can determine if an
 * object supports the {@code AccessibleText} interface by first obtaining its
 * {@code AccessibleContext} (see {@link Accessible}) and then calling the
 * {@link AccessibleContext#getAccessibleText} method of
 * {@code AccessibleContext}. If the return value is not {@code null}, the
 * object supports this interface.
 *
 * @author Peter Korn
 * @see Accessible
 * @see Accessible#getAccessibleContext
 * @see AccessibleContext
 * @see AccessibleContext#getAccessibleText
 */
public interface AccessibleText {

    /**
     * Constant used to indicate that the part of the text that should be
     * retrieved is a character.
     *
     * @see #getAtIndex
     * @see #getAfterIndex
     * @see #getBeforeIndex
     */
    public static final int CHARACTER = 1;

    /**
     * Constant used to indicate that the part of the text that should be
     * retrieved is a word.
     *
     * @see #getAtIndex
     * @see #getAfterIndex
     * @see #getBeforeIndex
     */
    public static final int WORD = 2;

    /**
     * Constant used to indicate that the part of the text that should be
     * retrieved is a sentence.
     * <p>
     * A sentence is a string of words which expresses an assertion, a question,
     * a command, a wish, an exclamation, or the performance of an action. In
     * English locales, the string usually begins with a capital letter and
     * concludes with appropriate end punctuation; such as a period, question or
     * exclamation mark. Other locales may use different capitalization and/or
     * punctuation.
     *
     * @see #getAtIndex
     * @see #getAfterIndex
     * @see #getBeforeIndex
     */
    public static final int SENTENCE = 3;

    /**
     * Given a point in local coordinates, return the zero-based index of the
     * character under that point. If the point is invalid, this method returns
     * -1.
     *
     * @param  p the point in local coordinates
     * @return the zero-based index of the character under {@code Point p}; if
     *         point is invalid return -1.
     */
    public int getIndexAtPoint(Point p);

    /**
     * Determines the bounding box of the character at the given index into the
     * string. The bounds are returned in local coordinates. If the index is
     * invalid an empty rectangle is returned.
     *
     * @param  i the index into the string
     * @return the screen coordinates of the character's bounding box, if index
     *         is invalid return an empty rectangle.
     */
    public Rectangle getCharacterBounds(int i);

    /**
     * Returns the number of characters (valid indicies).
     *
     * @return the number of characters
     */
    public int getCharCount();

    /**
     * Returns the zero-based offset of the caret.
     * <p>
     * Note: That to the right of the caret will have the same index value as
     * the offset (the caret is between two characters).
     *
     * @return the zero-based offset of the caret
     */
    public int getCaretPosition();

    /**
     * Returns the {@code String} at a given index.
     *
     * @param  part the CHARACTER, WORD, or SENTENCE to retrieve
     * @param  index an index within the text
     * @return the letter, word, or sentence
     */
    public String getAtIndex(int part, int index);

    /**
     * Returns the {@code String} after a given index.
     *
     * @param  part the CHARACTER, WORD, or SENTENCE to retrieve
     * @param  index an index within the text
     * @return the letter, word, or sentence
     */
    public String getAfterIndex(int part, int index);

    /**
     * Returns the {@code String} before a given index.
     *
     * @param  part the CHARACTER, WORD, or SENTENCE to retrieve
     * @param  index an index within the text
     * @return the letter, word, or sentence
     */
    public String getBeforeIndex(int part, int index);

    /**
     * Returns the {@code AttributeSet} for a given character at a given index.
     *
     * @param  i the zero-based index into the text
     * @return the {@code AttributeSet} of the character
     */
    public AttributeSet getCharacterAttribute(int i);

    /**
     * Returns the start offset within the selected text. If there is no
     * selection, but there is a caret, the start and end offsets will be the
     * same.
     *
     * @return the index into the text of the start of the selection
     */
    public int getSelectionStart();

    /**
     * Returns the end offset within the selected text. If there is no
     * selection, but there is a caret, the start and end offsets will be the
     * same.
     *
     * @return the index into the text of the end of the selection
     */
    public int getSelectionEnd();

    /**
     * Returns the portion of the text that is selected.
     *
     * @return the {@code String} portion of the text that is selected
     */
    public String getSelectedText();
}
