/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Font;
import java.awt.Color;

/**
 * Interface for a generic styled document.
 *
 * @author  Timothy Prinzing
 */
public interface StyledDocument extends Document {

    /**
     * Adds a new style into the logical style hierarchy.  Style attributes
     * resolve from bottom up so an attribute specified in a child
     * will override an attribute specified in the parent.
     *
     * @param nm   the name of the style (must be unique within the
     *   collection of named styles).  The name may be null if the style
     *   is unnamed, but the caller is responsible
     *   for managing the reference returned as an unnamed style can't
     *   be fetched by name.  An unnamed style may be useful for things
     *   like character attribute overrides such as found in a style
     *   run.
     * @param parent the parent style.  This may be null if unspecified
     *   attributes need not be resolved in some other style.
     * @return the style
     */
    public Style addStyle(String nm, Style parent);

    /**
     * Removes a named style previously added to the document.
     *
     * @param nm  the name of the style to remove
     */
    public void removeStyle(String nm);

    /**
     * Fetches a named style previously added.
     *
     * @param nm  the name of the style
     * @return the style
     */
    public Style getStyle(String nm);

    /**
     * Changes the content element attributes used for the given range of
     * existing content in the document.  All of the attributes
     * defined in the given Attributes argument are applied to the
     * given range.  This method can be used to completely remove
     * all content level attributes for the given range by
     * giving an Attributes argument that has no attributes defined
     * and setting replace to true.
     *
     * @param offset the start of the change &gt;= 0
     * @param length the length of the change &gt;= 0
     * @param s    the non-null attributes to change to.  Any attributes
     *  defined will be applied to the text for the given range.
     * @param replace indicates whether or not the previous
     *  attributes should be cleared before the new attributes
     *  as set.  If true, the operation will replace the
     *  previous attributes entirely.  If false, the new
     *  attributes will be merged with the previous attributes.
     */
    public void setCharacterAttributes(int offset, int length, AttributeSet s, boolean replace);

    /**
     * Sets paragraph attributes.
     *
     * @param offset the start of the change &gt;= 0
     * @param length the length of the change &gt;= 0
     * @param s    the non-null attributes to change to.  Any attributes
     *  defined will be applied to the text for the given range.
     * @param replace indicates whether or not the previous
     *  attributes should be cleared before the new attributes
     *  are set.  If true, the operation will replace the
     *  previous attributes entirely.  If false, the new
     *  attributes will be merged with the previous attributes.
     */
    public void setParagraphAttributes(int offset, int length, AttributeSet s, boolean replace);

    /**
     * Sets the logical style to use for the paragraph at the
     * given position.  If attributes aren't explicitly set
     * for character and paragraph attributes they will resolve
     * through the logical style assigned to the paragraph, which
     * in turn may resolve through some hierarchy completely
     * independent of the element hierarchy in the document.
     *
     * @param pos the starting position &gt;= 0
     * @param s the style to set
     */
    public void setLogicalStyle(int pos, Style s);

    /**
     * Gets a logical style for a given position in a paragraph.
     *
     * @param p the position &gt;= 0
     * @return the style
     */
    public Style getLogicalStyle(int p);

    /**
     * Gets the element that represents the paragraph that
     * encloses the given offset within the document.
     *
     * @param pos the offset &gt;= 0
     * @return the element
     */
    public Element getParagraphElement(int pos);

    /**
     * Gets the element that represents the character that
     * is at the given offset within the document.
     *
     * @param pos the offset &gt;= 0
     * @return the element
     */
    public Element getCharacterElement(int pos);


    /**
     * Takes a set of attributes and turn it into a foreground color
     * specification.  This might be used to specify things
     * like brighter, more hue, etc.
     *
     * @param attr the set of attributes
     * @return the color
     */
    public Color getForeground(AttributeSet attr);

    /**
     * Takes a set of attributes and turn it into a background color
     * specification.  This might be used to specify things
     * like brighter, more hue, etc.
     *
     * @param attr the set of attributes
     * @return the color
     */
    public Color getBackground(AttributeSet attr);

    /**
     * Takes a set of attributes and turn it into a font
     * specification.  This can be used to turn things like
     * family, style, size, etc into a font that is available
     * on the system the document is currently being used on.
     *
     * @param attr the set of attributes
     * @return the font
     */
    public Font getFont(AttributeSet attr);

}
