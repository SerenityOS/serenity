/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file and, per its terms, should not be removed:
 *
 * Copyright (c) 2004 World Wide Web Consortium,
 *
 * (Massachusetts Institute of Technology, European Research Consortium for
 * Informatics and Mathematics, Keio University). All Rights Reserved. This
 * work is distributed under the W3C(r) Software License [1] in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * [1] http://www.w3.org/Consortium/Legal/2002/copyright-software-20021231
 */

package org.w3c.dom;

/**
 *  The <code>NameList</code> interface provides the abstraction of an ordered
 * collection of parallel pairs of name and namespace values (which could be
 * null values), without defining or constraining how this collection is
 * implemented. The items in the <code>NameList</code> are accessible via an
 * integral index, starting from 0.
 * <p>See also the <a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407'>Document Object Model (DOM) Level 3 Core Specification</a>.
 * @since 1.5, DOM Level 3
 */
public interface NameList {
    /**
     *  Returns the <code>index</code>th name item in the collection.
     * @param index Index into the collection.
     * @return  The name at the <code>index</code>th position in the
     *   <code>NameList</code>, or <code>null</code> if there is no name for
     *   the specified index or if the index is out of range.
     */
    public String getName(int index);

    /**
     *  Returns the <code>index</code>th namespaceURI item in the collection.
     * @param index Index into the collection.
     * @return  The namespace URI at the <code>index</code>th position in the
     *   <code>NameList</code>, or <code>null</code> if there is no name for
     *   the specified index or if the index is out of range.
     */
    public String getNamespaceURI(int index);

    /**
     *  The number of pairs (name and namespaceURI) in the list. The range of
     * valid child node indices is 0 to <code>length-1</code> inclusive.
     */
    public int getLength();

    /**
     *  Test if a name is part of this <code>NameList</code>.
     * @param str  The name to look for.
     * @return  <code>true</code> if the name has been found,
     *   <code>false</code> otherwise.
     */
    public boolean contains(String str);

    /**
     *  Test if the pair namespaceURI/name is part of this
     * <code>NameList</code>.
     * @param namespaceURI  The namespace URI to look for.
     * @param name  The name to look for.
     * @return  <code>true</code> if the pair namespaceURI/name has been
     *   found, <code>false</code> otherwise.
     */
    public boolean containsNS(String namespaceURI,
                              String name);

}
