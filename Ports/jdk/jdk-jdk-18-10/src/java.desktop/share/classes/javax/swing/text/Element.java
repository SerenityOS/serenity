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

/**
 * Interface to describe a structural piece of a document.  It
 * is intended to capture the spirit of an SGML element.
 *
 * @author  Timothy Prinzing
 */
public interface Element {

    /**
     * Fetches the document associated with this element.
     *
     * @return the document
     */
    public Document getDocument();

    /**
     * Fetches the parent element.  If the element is a root level
     * element returns <code>null</code>.
     *
     * @return the parent element
     */
    public Element getParentElement();

    /**
     * Fetches the name of the element.  If the element is used to
     * represent some type of structure, this would be the type
     * name.
     *
     * @return the element name
     */
    public String getName();

    /**
     * Fetches the collection of attributes this element contains.
     *
     * @return the attributes for the element
     */
    public AttributeSet getAttributes();

    /**
     * Fetches the offset from the beginning of the document
     * that this element begins at.  If this element has
     * children, this will be the offset of the first child.
     * As a document position, there is an implied forward bias.
     *
     * @return the starting offset &gt;= 0 and &lt; getEndOffset();
     * @see Document
     * @see AbstractDocument
     */
    public int getStartOffset();

    /**
     * Fetches the offset from the beginning of the document
     * that this element ends at.  If this element has
     * children, this will be the end offset of the last child.
     * As a document position, there is an implied backward bias.
     * <p>
     * All the default <code>Document</code> implementations
     * descend from <code>AbstractDocument</code>.
     * <code>AbstractDocument</code> models an implied break at the end of
     * the document. As a result of this, it is possible for this to
     * return a value greater than the length of the document.
     *
     * @return the ending offset &gt; getStartOffset() and
     *     &lt;= getDocument().getLength() + 1
     * @see Document
     * @see AbstractDocument
     */
    public int getEndOffset();

    /**
     * Gets the child element index closest to the given offset.
     * The offset is specified relative to the beginning of the
     * document.  Returns <code>-1</code> if the
     * <code>Element</code> is a leaf, otherwise returns
     * the index of the <code>Element</code> that best represents
     * the given location.  Returns <code>0</code> if the location
     * is less than the start offset. Returns
     * <code>getElementCount() - 1</code> if the location is
     * greater than or equal to the end offset.
     *
     * @param offset the specified offset &gt;= 0
     * @return the element index &gt;= 0
     */
    public int getElementIndex(int offset);

    /**
     * Gets the number of child elements contained by this element.
     * If this element is a leaf, a count of zero is returned.
     *
     * @return the number of child elements &gt;= 0
     */
    public int getElementCount();

    /**
     * Fetches the child element at the given index.
     *
     * @param index the specified index &gt;= 0
     * @return the child element
     */
    public Element getElement(int index);

    /**
     * Is this element a leaf element? An element that
     * <i>may</i> have children, even if it currently
     * has no children, would return <code>false</code>.
     *
     * @return true if a leaf element else false
     */
    public boolean isLeaf();


}
