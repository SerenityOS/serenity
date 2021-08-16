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
 * Objects implementing the <code>NamedNodeMap</code> interface are used to
 * represent collections of nodes that can be accessed by name. Note that
 * <code>NamedNodeMap</code> does not inherit from <code>NodeList</code>;
 * <code>NamedNodeMaps</code> are not maintained in any particular order.
 * Objects contained in an object implementing <code>NamedNodeMap</code> may
 * also be accessed by an ordinal index, but this is simply to allow
 * convenient enumeration of the contents of a <code>NamedNodeMap</code>,
 * and does not imply that the DOM specifies an order to these Nodes.
 * <p><code>NamedNodeMap</code> objects in the DOM are live.
 * <p>See also the <a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407'>Document Object Model (DOM) Level 3 Core Specification</a>.
 */
public interface NamedNodeMap {
    /**
     * Retrieves a node specified by name.
     * @param name The <code>nodeName</code> of a node to retrieve.
     * @return A <code>Node</code> (of any type) with the specified
     *   <code>nodeName</code>, or <code>null</code> if it does not identify
     *   any node in this map.
     */
    public Node getNamedItem(String name);

    /**
     * Adds a node using its <code>nodeName</code> attribute. If a node with
     * that name is already present in this map, it is replaced by the new
     * one. Replacing a node by itself has no effect.
     * <br>As the <code>nodeName</code> attribute is used to derive the name
     * which the node must be stored under, multiple nodes of certain types
     * (those that have a "special" string value) cannot be stored as the
     * names would clash. This is seen as preferable to allowing nodes to be
     * aliased.
     * @param arg A node to store in this map. The node will later be
     *   accessible using the value of its <code>nodeName</code> attribute.
     * @return If the new <code>Node</code> replaces an existing node the
     *   replaced <code>Node</code> is returned, otherwise <code>null</code>
     *   is returned.
     * @exception DOMException
     *   WRONG_DOCUMENT_ERR: Raised if <code>arg</code> was created from a
     *   different document than the one that created this map.
     *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this map is readonly.
     *   <br>INUSE_ATTRIBUTE_ERR: Raised if <code>arg</code> is an
     *   <code>Attr</code> that is already an attribute of another
     *   <code>Element</code> object. The DOM user must explicitly clone
     *   <code>Attr</code> nodes to re-use them in other elements.
     *   <br>HIERARCHY_REQUEST_ERR: Raised if an attempt is made to add a node
     *   doesn't belong in this NamedNodeMap. Examples would include trying
     *   to insert something other than an Attr node into an Element's map
     *   of attributes, or a non-Entity node into the DocumentType's map of
     *   Entities.
     */
    public Node setNamedItem(Node arg)
                             throws DOMException;

    /**
     * Removes a node specified by name. When this map contains the attributes
     * attached to an element, if the removed attribute is known to have a
     * default value, an attribute immediately appears containing the
     * default value as well as the corresponding namespace URI, local name,
     * and prefix when applicable.
     * @param name The <code>nodeName</code> of the node to remove.
     * @return The node removed from this map if a node with such a name
     *   exists.
     * @exception DOMException
     *   NOT_FOUND_ERR: Raised if there is no node named <code>name</code> in
     *   this map.
     *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this map is readonly.
     */
    public Node removeNamedItem(String name)
                                throws DOMException;

    /**
     * Returns the <code>index</code>th item in the map. If <code>index</code>
     * is greater than or equal to the number of nodes in this map, this
     * returns <code>null</code>.
     * @param index Index into this map.
     * @return The node at the <code>index</code>th position in the map, or
     *   <code>null</code> if that is not a valid index.
     */
    public Node item(int index);

    /**
     * The number of nodes in this map. The range of valid child node indices
     * is <code>0</code> to <code>length-1</code> inclusive.
     */
    public int getLength();

    /**
     * Retrieves a node specified by local name and namespace URI.
     * <br>Per [<a href='http://www.w3.org/TR/1999/REC-xml-names-19990114/'>XML Namespaces</a>]
     * , applications must use the value null as the namespaceURI parameter
     * for methods if they wish to have no namespace.
     * @param namespaceURI The namespace URI of the node to retrieve.
     * @param localName The local name of the node to retrieve.
     * @return A <code>Node</code> (of any type) with the specified local
     *   name and namespace URI, or <code>null</code> if they do not
     *   identify any node in this map.
     * @exception DOMException
     *   NOT_SUPPORTED_ERR: May be raised if the implementation does not
     *   support the feature "XML" and the language exposed through the
     *   Document does not support XML Namespaces (such as [<a href='http://www.w3.org/TR/1999/REC-html401-19991224/'>HTML 4.01</a>]).
     * @since 1.4, DOM Level 2
     */
    public Node getNamedItemNS(String namespaceURI,
                               String localName)
                               throws DOMException;

    /**
     * Adds a node using its <code>namespaceURI</code> and
     * <code>localName</code>. If a node with that namespace URI and that
     * local name is already present in this map, it is replaced by the new
     * one. Replacing a node by itself has no effect.
     * <br>Per [<a href='http://www.w3.org/TR/1999/REC-xml-names-19990114/'>XML Namespaces</a>]
     * , applications must use the value null as the namespaceURI parameter
     * for methods if they wish to have no namespace.
     * @param arg A node to store in this map. The node will later be
     *   accessible using the value of its <code>namespaceURI</code> and
     *   <code>localName</code> attributes.
     * @return If the new <code>Node</code> replaces an existing node the
     *   replaced <code>Node</code> is returned, otherwise <code>null</code>
     *   is returned.
     * @exception DOMException
     *   WRONG_DOCUMENT_ERR: Raised if <code>arg</code> was created from a
     *   different document than the one that created this map.
     *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this map is readonly.
     *   <br>INUSE_ATTRIBUTE_ERR: Raised if <code>arg</code> is an
     *   <code>Attr</code> that is already an attribute of another
     *   <code>Element</code> object. The DOM user must explicitly clone
     *   <code>Attr</code> nodes to re-use them in other elements.
     *   <br>HIERARCHY_REQUEST_ERR: Raised if an attempt is made to add a node
     *   doesn't belong in this NamedNodeMap. Examples would include trying
     *   to insert something other than an Attr node into an Element's map
     *   of attributes, or a non-Entity node into the DocumentType's map of
     *   Entities.
     *   <br>NOT_SUPPORTED_ERR: May be raised if the implementation does not
     *   support the feature "XML" and the language exposed through the
     *   Document does not support XML Namespaces (such as [<a href='http://www.w3.org/TR/1999/REC-html401-19991224/'>HTML 4.01</a>]).
     * @since 1.4, DOM Level 2
     */
    public Node setNamedItemNS(Node arg)
                               throws DOMException;

    /**
     * Removes a node specified by local name and namespace URI. A removed
     * attribute may be known to have a default value when this map contains
     * the attributes attached to an element, as returned by the attributes
     * attribute of the <code>Node</code> interface. If so, an attribute
     * immediately appears containing the default value as well as the
     * corresponding namespace URI, local name, and prefix when applicable.
     * <br>Per [<a href='http://www.w3.org/TR/1999/REC-xml-names-19990114/'>XML Namespaces</a>]
     * , applications must use the value null as the namespaceURI parameter
     * for methods if they wish to have no namespace.
     * @param namespaceURI The namespace URI of the node to remove.
     * @param localName The local name of the node to remove.
     * @return The node removed from this map if a node with such a local
     *   name and namespace URI exists.
     * @exception DOMException
     *   NOT_FOUND_ERR: Raised if there is no node with the specified
     *   <code>namespaceURI</code> and <code>localName</code> in this map.
     *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this map is readonly.
     *   <br>NOT_SUPPORTED_ERR: May be raised if the implementation does not
     *   support the feature "XML" and the language exposed through the
     *   Document does not support XML Namespaces (such as [<a href='http://www.w3.org/TR/1999/REC-html401-19991224/'>HTML 4.01</a>]).
     * @since 1.4, DOM Level 2
     */
    public Node removeNamedItemNS(String namespaceURI,
                                  String localName)
                                  throws DOMException;

}
