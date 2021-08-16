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
 * The <code>Node</code> interface is the primary datatype for the entire
 * Document Object Model. It represents a single node in the document tree.
 * While all objects implementing the <code>Node</code> interface expose
 * methods for dealing with children, not all objects implementing the
 * <code>Node</code> interface may have children. For example,
 * <code>Text</code> nodes may not have children, and adding children to
 * such nodes results in a <code>DOMException</code> being raised.
 * <p>The attributes <code>nodeName</code>, <code>nodeValue</code> and
 * <code>attributes</code> are included as a mechanism to get at node
 * information without casting down to the specific derived interface. In
 * cases where there is no obvious mapping of these attributes for a
 * specific <code>nodeType</code> (e.g., <code>nodeValue</code> for an
 * <code>Element</code> or <code>attributes</code> for a <code>Comment</code>
 * ), this returns <code>null</code>. Note that the specialized interfaces
 * may contain additional and more convenient mechanisms to get and set the
 * relevant information.
 * <p>The values of <code>nodeName</code>,
 * <code>nodeValue</code>, and <code>attributes</code> vary according to the
 * node type as follows:
 * <table class="striped">
 * <caption style="display:none">Interface table</caption>
 * <thead>
 * <tr>
 * <th scope="col">Interface</th>
 * <th scope="col">nodeName</th>
 * <th scope="col">nodeValue</th>
 * <th scope="col">attributes</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <th scope="row">
 * <code>Attr</code></th>
 * <td>same as <code>Attr.name</code></td>
 * <td>same as
 * <code>Attr.value</code></td>
 * <td><code>null</code></td>
 * </tr>
 * <tr>
 * <th scope="row"><code>CDATASection</code></th>
 * <td>
 * <code>"#cdata-section"</code></td>
 * <td>same as <code>CharacterData.data</code>, the
 * content of the CDATA Section</td>
 * <td><code>null</code></td>
 * </tr>
 * <tr>
 * <th scope="row"><code>Comment</code></th>
 * <td>
 * <code>"#comment"</code></td>
 * <td>same as <code>CharacterData.data</code>, the
 * content of the comment</td>
 * <td><code>null</code></td>
 * </tr>
 * <tr>
 * <th scope="row"><code>Document</code></th>
 * <td>
 * <code>"#document"</code></td>
 * <td><code>null</code></td>
 * <td><code>null</code></td>
 * </tr>
 * <tr>
 * <th scope="row">
 * <code>DocumentFragment</code></th>
 * <td><code>"#document-fragment"</code></td>
 * <td>
 * <code>null</code></td>
 * <td><code>null</code></td>
 * </tr>
 * <tr>
 * <th scope="row"><code>DocumentType</code></th>
 * <td>same as
 * <code>DocumentType.name</code></td>
 * <td><code>null</code></td>
 * <td><code>null</code></td>
 * </tr>
 * <tr>
 * <th scope="row">
 * <code>Element</code></th>
 * <td>same as <code>Element.tagName</code></td>
 * <td><code>null</code></td>
 * <td>
 * <code>NamedNodeMap</code></td>
 * </tr>
 * <tr>
 * <th scope="row"><code>Entity</code></th>
 * <td>entity name</td>
 * <td><code>null</code></td>
 * <td>
 * <code>null</code></td>
 * </tr>
 * <tr>
 * <th scope="row"><code>EntityReference</code></th>
 * <td>name of entity referenced</td>
 * <td>
 * <code>null</code></td>
 * <td><code>null</code></td>
 * </tr>
 * <tr>
 * <th scope="row"><code>Notation</code></th>
 * <td>notation name</td>
 * <td>
 * <code>null</code></td>
 * <td><code>null</code></td>
 * </tr>
 * <tr>
 * <th scope="row"><code>ProcessingInstruction</code></th>
 * <td>same
 * as <code>ProcessingInstruction.target</code></td>
 * <td>same as
 * <code>ProcessingInstruction.data</code></td>
 * <td><code>null</code></td>
 * </tr>
 * <tr>
 * <th scope="row"><code>Text</code></th>
 * <td>
 * <code>"#text"</code></td>
 * <td>same as <code>CharacterData.data</code>, the content
 * of the text node</td>
 * <td><code>null</code></td>
 * </tr>
 * </tbody>
 * </table>
 * <p>See also the <a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407'>Document Object Model (DOM) Level 3 Core Specification</a>.
 */
public interface Node {
    // NodeType
    /**
     * The node is an <code>Element</code>.
     */
    public static final short ELEMENT_NODE              = 1;
    /**
     * The node is an <code>Attr</code>.
     */
    public static final short ATTRIBUTE_NODE            = 2;
    /**
     * The node is a <code>Text</code> node.
     */
    public static final short TEXT_NODE                 = 3;
    /**
     * The node is a <code>CDATASection</code>.
     */
    public static final short CDATA_SECTION_NODE        = 4;
    /**
     * The node is an <code>EntityReference</code>.
     */
    public static final short ENTITY_REFERENCE_NODE     = 5;
    /**
     * The node is an <code>Entity</code>.
     */
    public static final short ENTITY_NODE               = 6;
    /**
     * The node is a <code>ProcessingInstruction</code>.
     */
    public static final short PROCESSING_INSTRUCTION_NODE = 7;
    /**
     * The node is a <code>Comment</code>.
     */
    public static final short COMMENT_NODE              = 8;
    /**
     * The node is a <code>Document</code>.
     */
    public static final short DOCUMENT_NODE             = 9;
    /**
     * The node is a <code>DocumentType</code>.
     */
    public static final short DOCUMENT_TYPE_NODE        = 10;
    /**
     * The node is a <code>DocumentFragment</code>.
     */
    public static final short DOCUMENT_FRAGMENT_NODE    = 11;
    /**
     * The node is a <code>Notation</code>.
     */
    public static final short NOTATION_NODE             = 12;

    /**
     * The name of this node, depending on its type; see the table above.
     */
    public String getNodeName();

    /**
     * The value of this node, depending on its type; see the table above.
     * When it is defined to be <code>null</code>, setting it has no effect,
     * including if the node is read-only.
     * @exception DOMException
     *   DOMSTRING_SIZE_ERR: Raised when it would return more characters than
     *   fit in a <code>DOMString</code> variable on the implementation
     *   platform.
     */
    public String getNodeValue()
                              throws DOMException;
    /**
     * The value of this node, depending on its type; see the table above.
     * When it is defined to be <code>null</code>, setting it has no effect,
     * including if the node is read-only.
     * @exception DOMException
     *   NO_MODIFICATION_ALLOWED_ERR: Raised when the node is readonly and if
     *   it is not defined to be <code>null</code>.
     */
    public void setNodeValue(String nodeValue)
                              throws DOMException;

    /**
     * A code representing the type of the underlying object, as defined above.
     */
    public short getNodeType();

    /**
     * The parent of this node. All nodes, except <code>Attr</code>,
     * <code>Document</code>, <code>DocumentFragment</code>,
     * <code>Entity</code>, and <code>Notation</code> may have a parent.
     * However, if a node has just been created and not yet added to the
     * tree, or if it has been removed from the tree, this is
     * <code>null</code>.
     */
    public Node getParentNode();

    /**
     * A <code>NodeList</code> that contains all children of this node. If
     * there are no children, this is a <code>NodeList</code> containing no
     * nodes.
     */
    public NodeList getChildNodes();

    /**
     * The first child of this node. If there is no such node, this returns
     * <code>null</code>.
     */
    public Node getFirstChild();

    /**
     * The last child of this node. If there is no such node, this returns
     * <code>null</code>.
     */
    public Node getLastChild();

    /**
     * The node immediately preceding this node. If there is no such node,
     * this returns <code>null</code>.
     */
    public Node getPreviousSibling();

    /**
     * The node immediately following this node. If there is no such node,
     * this returns <code>null</code>.
     */
    public Node getNextSibling();

    /**
     * A <code>NamedNodeMap</code> containing the attributes of this node (if
     * it is an <code>Element</code>) or <code>null</code> otherwise.
     */
    public NamedNodeMap getAttributes();

    /**
     * The <code>Document</code> object associated with this node. This is
     * also the <code>Document</code> object used to create new nodes. When
     * this node is a <code>Document</code> or a <code>DocumentType</code>
     * which is not used with any <code>Document</code> yet, this is
     * <code>null</code>.
     *
     * @since 1.4, DOM Level 2
     */
    public Document getOwnerDocument();

    /**
     * Inserts the node <code>newChild</code> before the existing child node
     * <code>refChild</code>. If <code>refChild</code> is <code>null</code>,
     * insert <code>newChild</code> at the end of the list of children.
     * <br>If <code>newChild</code> is a <code>DocumentFragment</code> object,
     * all of its children are inserted, in the same order, before
     * <code>refChild</code>. If the <code>newChild</code> is already in the
     * tree, it is first removed.
     * <p ><b>Note:</b>  Inserting a node before itself is implementation
     * dependent.
     * @param newChild The node to insert.
     * @param refChild The reference node, i.e., the node before which the
     *   new node must be inserted.
     * @return The node being inserted.
     * @exception DOMException
     *   HIERARCHY_REQUEST_ERR: Raised if this node is of a type that does not
     *   allow children of the type of the <code>newChild</code> node, or if
     *   the node to insert is one of this node's ancestors or this node
     *   itself, or if this node is of type <code>Document</code> and the
     *   DOM application attempts to insert a second
     *   <code>DocumentType</code> or <code>Element</code> node.
     *   <br>WRONG_DOCUMENT_ERR: Raised if <code>newChild</code> was created
     *   from a different document than the one that created this node.
     *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly or
     *   if the parent of the node being inserted is readonly.
     *   <br>NOT_FOUND_ERR: Raised if <code>refChild</code> is not a child of
     *   this node.
     *   <br>NOT_SUPPORTED_ERR: if this node is of type <code>Document</code>,
     *   this exception might be raised if the DOM implementation doesn't
     *   support the insertion of a <code>DocumentType</code> or
     *   <code>Element</code> node.
     *
     * @since 1.4, DOM Level 3
     */
    public Node insertBefore(Node newChild,
                             Node refChild)
                             throws DOMException;

    /**
     * Replaces the child node <code>oldChild</code> with <code>newChild</code>
     *  in the list of children, and returns the <code>oldChild</code> node.
     * <br>If <code>newChild</code> is a <code>DocumentFragment</code> object,
     * <code>oldChild</code> is replaced by all of the
     * <code>DocumentFragment</code> children, which are inserted in the
     * same order. If the <code>newChild</code> is already in the tree, it
     * is first removed.
     * <p ><b>Note:</b>  Replacing a node with itself is implementation
     * dependent.
     * @param newChild The new node to put in the child list.
     * @param oldChild The node being replaced in the list.
     * @return The node replaced.
     * @exception DOMException
     *   HIERARCHY_REQUEST_ERR: Raised if this node is of a type that does not
     *   allow children of the type of the <code>newChild</code> node, or if
     *   the node to put in is one of this node's ancestors or this node
     *   itself, or if this node is of type <code>Document</code> and the
     *   result of the replacement operation would add a second
     *   <code>DocumentType</code> or <code>Element</code> on the
     *   <code>Document</code> node.
     *   <br>WRONG_DOCUMENT_ERR: Raised if <code>newChild</code> was created
     *   from a different document than the one that created this node.
     *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this node or the parent of
     *   the new node is readonly.
     *   <br>NOT_FOUND_ERR: Raised if <code>oldChild</code> is not a child of
     *   this node.
     *   <br>NOT_SUPPORTED_ERR: if this node is of type <code>Document</code>,
     *   this exception might be raised if the DOM implementation doesn't
     *   support the replacement of the <code>DocumentType</code> child or
     *   <code>Element</code> child.
     *
     * @since 1.4, DOM Level 3
     */
    public Node replaceChild(Node newChild,
                             Node oldChild)
                             throws DOMException;

    /**
     * Removes the child node indicated by <code>oldChild</code> from the list
     * of children, and returns it.
     * @param oldChild The node being removed.
     * @return The node removed.
     * @exception DOMException
     *   NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     *   <br>NOT_FOUND_ERR: Raised if <code>oldChild</code> is not a child of
     *   this node.
     *   <br>NOT_SUPPORTED_ERR: if this node is of type <code>Document</code>,
     *   this exception might be raised if the DOM implementation doesn't
     *   support the removal of the <code>DocumentType</code> child or the
     *   <code>Element</code> child.
     *
     * @since 1.4, DOM Level 3
     */
    public Node removeChild(Node oldChild)
                            throws DOMException;

    /**
     * Adds the node <code>newChild</code> to the end of the list of children
     * of this node. If the <code>newChild</code> is already in the tree, it
     * is first removed.
     * @param newChild The node to add.If it is a
     *   <code>DocumentFragment</code> object, the entire contents of the
     *   document fragment are moved into the child list of this node
     * @return The node added.
     * @exception DOMException
     *   HIERARCHY_REQUEST_ERR: Raised if this node is of a type that does not
     *   allow children of the type of the <code>newChild</code> node, or if
     *   the node to append is one of this node's ancestors or this node
     *   itself, or if this node is of type <code>Document</code> and the
     *   DOM application attempts to append a second
     *   <code>DocumentType</code> or <code>Element</code> node.
     *   <br>WRONG_DOCUMENT_ERR: Raised if <code>newChild</code> was created
     *   from a different document than the one that created this node.
     *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly or
     *   if the previous parent of the node being inserted is readonly.
     *   <br>NOT_SUPPORTED_ERR: if the <code>newChild</code> node is a child
     *   of the <code>Document</code> node, this exception might be raised
     *   if the DOM implementation doesn't support the removal of the
     *   <code>DocumentType</code> child or <code>Element</code> child.
     *
     * @since 1.4, DOM Level 3
     */
    public Node appendChild(Node newChild)
                            throws DOMException;

    /**
     * Returns whether this node has any children.
     * @return Returns <code>true</code> if this node has any children,
     *   <code>false</code> otherwise.
     */
    public boolean hasChildNodes();

    /**
     * Returns a duplicate of this node, i.e., serves as a generic copy
     * constructor for nodes. The duplicate node has no parent (
     * <code>parentNode</code> is <code>null</code>) and no user data. User
     * data associated to the imported node is not carried over. However, if
     * any <code>UserDataHandlers</code> has been specified along with the
     * associated data these handlers will be called with the appropriate
     * parameters before this method returns.
     * <br>Cloning an <code>Element</code> copies all attributes and their
     * values, including those generated by the XML processor to represent
     * defaulted attributes, but this method does not copy any children it
     * contains unless it is a deep clone. This includes text contained in
     * an the <code>Element</code> since the text is contained in a child
     * <code>Text</code> node. Cloning an <code>Attr</code> directly, as
     * opposed to be cloned as part of an <code>Element</code> cloning
     * operation, returns a specified attribute (<code>specified</code> is
     * <code>true</code>). Cloning an <code>Attr</code> always clones its
     * children, since they represent its value, no matter whether this is a
     * deep clone or not. Cloning an <code>EntityReference</code>
     * automatically constructs its subtree if a corresponding
     * <code>Entity</code> is available, no matter whether this is a deep
     * clone or not. Cloning any other type of node simply returns a copy of
     * this node.
     * <br>Note that cloning an immutable subtree results in a mutable copy,
     * but the children of an <code>EntityReference</code> clone are readonly
     * . In addition, clones of unspecified <code>Attr</code> nodes are
     * specified. And, cloning <code>Document</code>,
     * <code>DocumentType</code>, <code>Entity</code>, and
     * <code>Notation</code> nodes is implementation dependent.
     * @param deep If <code>true</code>, recursively clone the subtree under
     *   the specified node; if <code>false</code>, clone only the node
     *   itself (and its attributes, if it is an <code>Element</code>).
     * @return The duplicate node.
     */
    public Node cloneNode(boolean deep);

    /**
     *  Puts all <code>Text</code> nodes in the full depth of the sub-tree
     * underneath this <code>Node</code>, including attribute nodes, into a
     * "normal" form where only structure (e.g., elements, comments,
     * processing instructions, CDATA sections, and entity references)
     * separates <code>Text</code> nodes, i.e., there are neither adjacent
     * <code>Text</code> nodes nor empty <code>Text</code> nodes. This can
     * be used to ensure that the DOM view of a document is the same as if
     * it were saved and re-loaded, and is useful when operations (such as
     * XPointer [<a href='http://www.w3.org/TR/2003/REC-xptr-framework-20030325/'>XPointer</a>]
     *  lookups) that depend on a particular document tree structure are to
     * be used. If the parameter "normalize-characters" of the
     * <code>DOMConfiguration</code> object attached to the
     * <code>Node.ownerDocument</code> is <code>true</code>, this method
     * will also fully normalize the characters of the <code>Text</code>
     * nodes.
     * <p ><b>Note:</b> In cases where the document contains
     * <code>CDATASections</code>, the normalize operation alone may not be
     * sufficient, since XPointers do not differentiate between
     * <code>Text</code> nodes and <code>CDATASection</code> nodes.
     *
     * @since 1.4, DOM Level 3
     */
    public void normalize();

    /**
     *  Tests whether the DOM implementation implements a specific feature and
     * that feature is supported by this node, as specified in .
     * @param feature  The name of the feature to test.
     * @param version  This is the version number of the feature to test.
     * @return Returns <code>true</code> if the specified feature is
     *   supported on this node, <code>false</code> otherwise.
     *
     * @since 1.4, DOM Level 2
     */
    public boolean isSupported(String feature,
                               String version);

    /**
     * The namespace URI of this node, or <code>null</code> if it is
     * unspecified (see ).
     * <br>This is not a computed value that is the result of a namespace
     * lookup based on an examination of the namespace declarations in
     * scope. It is merely the namespace URI given at creation time.
     * <br>For nodes of any type other than <code>ELEMENT_NODE</code> and
     * <code>ATTRIBUTE_NODE</code> and nodes created with a DOM Level 1
     * method, such as <code>Document.createElement()</code>, this is always
     * <code>null</code>.
     * <p ><b>Note:</b> Per the <em>Namespaces in XML</em> Specification [<a href='http://www.w3.org/TR/1999/REC-xml-names-19990114/'>XML Namespaces</a>]
     *  an attribute does not inherit its namespace from the element it is
     * attached to. If an attribute is not explicitly given a namespace, it
     * simply has no namespace.
     *
     * @since 1.4, DOM Level 2
     */
    public String getNamespaceURI();

    /**
     * The namespace prefix of this node, or <code>null</code> if it is
     * unspecified. When it is defined to be <code>null</code>, setting it
     * has no effect, including if the node is read-only.
     * <br>Note that setting this attribute, when permitted, changes the
     * <code>nodeName</code> attribute, which holds the qualified name, as
     * well as the <code>tagName</code> and <code>name</code> attributes of
     * the <code>Element</code> and <code>Attr</code> interfaces, when
     * applicable.
     * <br>Setting the prefix to <code>null</code> makes it unspecified,
     * setting it to an empty string is implementation dependent.
     * <br>Note also that changing the prefix of an attribute that is known to
     * have a default value, does not make a new attribute with the default
     * value and the original prefix appear, since the
     * <code>namespaceURI</code> and <code>localName</code> do not change.
     * <br>For nodes of any type other than <code>ELEMENT_NODE</code> and
     * <code>ATTRIBUTE_NODE</code> and nodes created with a DOM Level 1
     * method, such as <code>createElement</code> from the
     * <code>Document</code> interface, this is always <code>null</code>.
     *
     * @since 1.4, DOM Level 2
     */
    public String getPrefix();
    /**
     * The namespace prefix of this node, or <code>null</code> if it is
     * unspecified. When it is defined to be <code>null</code>, setting it
     * has no effect, including if the node is read-only.
     * <br>Note that setting this attribute, when permitted, changes the
     * <code>nodeName</code> attribute, which holds the qualified name, as
     * well as the <code>tagName</code> and <code>name</code> attributes of
     * the <code>Element</code> and <code>Attr</code> interfaces, when
     * applicable.
     * <br>Setting the prefix to <code>null</code> makes it unspecified,
     * setting it to an empty string is implementation dependent.
     * <br>Note also that changing the prefix of an attribute that is known to
     * have a default value, does not make a new attribute with the default
     * value and the original prefix appear, since the
     * <code>namespaceURI</code> and <code>localName</code> do not change.
     * <br>For nodes of any type other than <code>ELEMENT_NODE</code> and
     * <code>ATTRIBUTE_NODE</code> and nodes created with a DOM Level 1
     * method, such as <code>createElement</code> from the
     * <code>Document</code> interface, this is always <code>null</code>.
     * @exception DOMException
     *   INVALID_CHARACTER_ERR: Raised if the specified prefix contains an
     *   illegal character according to the XML version in use specified in
     *   the <code>Document.xmlVersion</code> attribute.
     *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
     *   <br>NAMESPACE_ERR: Raised if the specified <code>prefix</code> is
     *   malformed per the Namespaces in XML specification, if the
     *   <code>namespaceURI</code> of this node is <code>null</code>, if the
     *   specified prefix is "xml" and the <code>namespaceURI</code> of this
     *   node is different from "<a href='http://www.w3.org/XML/1998/namespace'>
     *   http://www.w3.org/XML/1998/namespace</a>", if this node is an attribute and the specified prefix is "xmlns" and
     *   the <code>namespaceURI</code> of this node is different from "<a href='http://www.w3.org/2000/xmlns/'>http://www.w3.org/2000/xmlns/</a>", or if this node is an attribute and the <code>qualifiedName</code> of
     *   this node is "xmlns" [<a href='http://www.w3.org/TR/1999/REC-xml-names-19990114/'>XML Namespaces</a>]
     *   .
     *
     * @since 1.4, DOM Level 2
     */
    public void setPrefix(String prefix)
                               throws DOMException;

    /**
     * Returns the local part of the qualified name of this node.
     * <br>For nodes of any type other than <code>ELEMENT_NODE</code> and
     * <code>ATTRIBUTE_NODE</code> and nodes created with a DOM Level 1
     * method, such as <code>Document.createElement()</code>, this is always
     * <code>null</code>.
     *
     * @since 1.4, DOM Level 2
     */
    public String getLocalName();

    /**
     * Returns whether this node (if it is an element) has any attributes.
     * @return Returns <code>true</code> if this node has any attributes,
     *   <code>false</code> otherwise.
     *
     * @since 1.4, DOM Level 2
     */
    public boolean hasAttributes();

    /**
     * The absolute base URI of this node or <code>null</code> if the
     * implementation wasn't able to obtain an absolute URI. This value is
     * computed as described in . However, when the <code>Document</code>
     * supports the feature "HTML" [<a href='http://www.w3.org/TR/2003/REC-DOM-Level-2-HTML-20030109'>DOM Level 2 HTML</a>]
     * , the base URI is computed using first the value of the href
     * attribute of the HTML BASE element if any, and the value of the
     * <code>documentURI</code> attribute from the <code>Document</code>
     * interface otherwise.
     *
     * @since 1.5, DOM Level 3
     */
    public String getBaseURI();

    // DocumentPosition
    /**
     * The two nodes are disconnected. Order between disconnected nodes is
     * always implementation-specific.
     */
    public static final short DOCUMENT_POSITION_DISCONNECTED = 0x01;
    /**
     * The second node precedes the reference node.
     */
    public static final short DOCUMENT_POSITION_PRECEDING = 0x02;
    /**
     * The node follows the reference node.
     */
    public static final short DOCUMENT_POSITION_FOLLOWING = 0x04;
    /**
     * The node contains the reference node. A node which contains is always
     * preceding, too.
     */
    public static final short DOCUMENT_POSITION_CONTAINS = 0x08;
    /**
     * The node is contained by the reference node. A node which is contained
     * is always following, too.
     */
    public static final short DOCUMENT_POSITION_CONTAINED_BY = 0x10;
    /**
     * The determination of preceding versus following is
     * implementation-specific.
     */
    public static final short DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC = 0x20;

    /**
     * Compares the reference node, i.e. the node on which this method is
     * being called, with a node, i.e. the one passed as a parameter, with
     * regard to their position in the document and according to the
     * document order.
     * @param other The node to compare against the reference node.
     * @return Returns how the node is positioned relatively to the reference
     *   node.
     * @exception DOMException
     *   NOT_SUPPORTED_ERR: when the compared nodes are from different DOM
     *   implementations that do not coordinate to return consistent
     *   implementation-specific results.
     *
     * @since 1.5, DOM Level 3
     */
    public short compareDocumentPosition(Node other)
                                         throws DOMException;

    /**
     * This attribute returns the text content of this node and its
     * descendants. When it is defined to be <code>null</code>, setting it
     * has no effect. On setting, any possible children this node may have
     * are removed and, if it the new string is not empty or
     * <code>null</code>, replaced by a single <code>Text</code> node
     * containing the string this attribute is set to.
     * <br> On getting, no serialization is performed, the returned string
     * does not contain any markup. No whitespace normalization is performed
     * and the returned string does not contain the white spaces in element
     * content (see the attribute
     * <code>Text.isElementContentWhitespace</code>). Similarly, on setting,
     * no parsing is performed either, the input string is taken as pure
     * textual content.
     * <br>The string returned is made of the text content of this node
     * depending on its type, as defined below:
     * <table class="striped">
     * <caption style="display:none">Node/Content table</caption>
     * <thead>
     * <tr>
     * <th scope="col">Node type</th>
     * <th scope="col">Content</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <th scope="row">
     * ELEMENT_NODE, ATTRIBUTE_NODE, ENTITY_NODE, ENTITY_REFERENCE_NODE,
     * DOCUMENT_FRAGMENT_NODE</th>
     * <td>concatenation of the <code>textContent</code>
     * attribute value of every child node, excluding COMMENT_NODE and
     * PROCESSING_INSTRUCTION_NODE nodes. This is the empty string if the
     * node has no children.</td>
     * </tr>
     * <tr>
     * <th scope="row">TEXT_NODE, CDATA_SECTION_NODE, COMMENT_NODE,
     * PROCESSING_INSTRUCTION_NODE</th>
     * <td><code>nodeValue</code></td>
     * </tr>
     * <tr>
     * <th scope="row">DOCUMENT_NODE,
     * DOCUMENT_TYPE_NODE, NOTATION_NODE</th>
     * <td><em>null</em></td>
     * </tr>
     * </tbody>
     * </table>
     * @exception DOMException
     *   DOMSTRING_SIZE_ERR: Raised when it would return more characters than
     *   fit in a <code>DOMString</code> variable on the implementation
     *   platform.
     *
     * @since 1.5, DOM Level 3
     */
    public String getTextContent()
                                         throws DOMException;
    /**
     * This attribute returns the text content of this node and its
     * descendants. When it is defined to be <code>null</code>, setting it
     * has no effect. On setting, any possible children this node may have
     * are removed and, if it the new string is not empty or
     * <code>null</code>, replaced by a single <code>Text</code> node
     * containing the string this attribute is set to.
     * <br> On getting, no serialization is performed, the returned string
     * does not contain any markup. No whitespace normalization is performed
     * and the returned string does not contain the white spaces in element
     * content (see the attribute
     * <code>Text.isElementContentWhitespace</code>). Similarly, on setting,
     * no parsing is performed either, the input string is taken as pure
     * textual content.
     * <br>The string returned is made of the text content of this node
     * depending on its type, as defined below:
     * <table class="striped">
     * <caption style="display:none">Node/Content table</caption>
     * <thead>
     * <tr>
     * <th scope="col">Node type</th>
     * <th scope="col">Content</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <th scope="row">
     * ELEMENT_NODE, ATTRIBUTE_NODE, ENTITY_NODE, ENTITY_REFERENCE_NODE,
     * DOCUMENT_FRAGMENT_NODE</th>
     * <td>concatenation of the <code>textContent</code>
     * attribute value of every child node, excluding COMMENT_NODE and
     * PROCESSING_INSTRUCTION_NODE nodes. This is the empty string if the
     * node has no children.</td>
     * </tr>
     * <tr>
     * <th scope="row">TEXT_NODE, CDATA_SECTION_NODE, COMMENT_NODE,
     * PROCESSING_INSTRUCTION_NODE</th>
     * <td><code>nodeValue</code></td>
     * </tr>
     * <tr>
     * <th scope="row">DOCUMENT_NODE,
     * DOCUMENT_TYPE_NODE, NOTATION_NODE</th>
     * <td><em>null</em></td>
     * </tr>
     * </tbody>
     * </table>
     * @exception DOMException
     *   NO_MODIFICATION_ALLOWED_ERR: Raised when the node is readonly.
     *
     * @since 1.5, DOM Level 3
     */
    public void setTextContent(String textContent)
                                         throws DOMException;

    /**
     * Returns whether this node is the same node as the given one.
     * <br>This method provides a way to determine whether two
     * <code>Node</code> references returned by the implementation reference
     * the same object. When two <code>Node</code> references are references
     * to the same object, even if through a proxy, the references may be
     * used completely interchangeably, such that all attributes have the
     * same values and calling the same DOM method on either reference
     * always has exactly the same effect.
     * @param other The node to test against.
     * @return Returns <code>true</code> if the nodes are the same,
     *   <code>false</code> otherwise.
     *
     * @since 1.5, DOM Level 3
     */
    public boolean isSameNode(Node other);

    /**
     * Look up the prefix associated to the given namespace URI, starting from
     * this node. The default namespace declarations are ignored by this
     * method.
     * <br>See  for details on the algorithm used by this method.
     * @param namespaceURI The namespace URI to look for.
     * @return Returns an associated namespace prefix if found or
     *   <code>null</code> if none is found. If more than one prefix are
     *   associated to the namespace prefix, the returned namespace prefix
     *   is implementation dependent.
     *
     * @since 1.5, DOM Level 3
     */
    public String lookupPrefix(String namespaceURI);

    /**
     *  This method checks if the specified <code>namespaceURI</code> is the
     * default namespace or not.
     * @param namespaceURI The namespace URI to look for.
     * @return Returns <code>true</code> if the specified
     *   <code>namespaceURI</code> is the default namespace,
     *   <code>false</code> otherwise.
     *
     * @since 1.5, DOM Level 3
     */
    public boolean isDefaultNamespace(String namespaceURI);

    /**
     * Look up the namespace URI associated to the given prefix, starting from
     * this node.
     * <br>See  for details on the algorithm used by this method.
     * @param prefix The prefix to look for. If this parameter is
     *   <code>null</code>, the method will return the default namespace URI
     *   if any.
     * @return Returns the associated namespace URI or <code>null</code> if
     *   none is found.
     *
     * @since 1.5, DOM Level 3
     */
    public String lookupNamespaceURI(String prefix);

    /**
     * Tests whether two nodes are equal.
     * <br>This method tests for equality of nodes, not sameness (i.e.,
     * whether the two nodes are references to the same object) which can be
     * tested with <code>Node.isSameNode()</code>. All nodes that are the
     * same will also be equal, though the reverse may not be true.
     * <br>Two nodes are equal if and only if the following conditions are
     * satisfied:
     * <ul>
     * <li>The two nodes are of the same type.
     * </li>
     * <li>The following string
     * attributes are equal: <code>nodeName</code>, <code>localName</code>,
     * <code>namespaceURI</code>, <code>prefix</code>, <code>nodeValue</code>
     * . This is: they are both <code>null</code>, or they have the same
     * length and are character for character identical.
     * </li>
     * <li>The
     * <code>attributes</code> <code>NamedNodeMaps</code> are equal. This
     * is: they are both <code>null</code>, or they have the same length and
     * for each node that exists in one map there is a node that exists in
     * the other map and is equal, although not necessarily at the same
     * index.
     * </li>
     * <li>The <code>childNodes</code> <code>NodeLists</code> are equal.
     * This is: they are both <code>null</code>, or they have the same
     * length and contain equal nodes at the same index. Note that
     * normalization can affect equality; to avoid this, nodes should be
     * normalized before being compared.
     * </li>
     * </ul>
     * <br>For two <code>DocumentType</code> nodes to be equal, the following
     * conditions must also be satisfied:
     * <ul>
     * <li>The following string attributes
     * are equal: <code>publicId</code>, <code>systemId</code>,
     * <code>internalSubset</code>.
     * </li>
     * <li>The <code>entities</code>
     * <code>NamedNodeMaps</code> are equal.
     * </li>
     * <li>The <code>notations</code>
     * <code>NamedNodeMaps</code> are equal.
     * </li>
     * </ul>
     * <br>On the other hand, the following do not affect equality: the
     * <code>ownerDocument</code>, <code>baseURI</code>, and
     * <code>parentNode</code> attributes, the <code>specified</code>
     * attribute for <code>Attr</code> nodes, the <code>schemaTypeInfo</code>
     *  attribute for <code>Attr</code> and <code>Element</code> nodes, the
     * <code>Text.isElementContentWhitespace</code> attribute for
     * <code>Text</code> nodes, as well as any user data or event listeners
     * registered on the nodes.
     * <p ><b>Note:</b>  As a general rule, anything not mentioned in the
     * description above is not significant in consideration of equality
     * checking. Note that future versions of this specification may take
     * into account more attributes and implementations conform to this
     * specification are expected to be updated accordingly.
     * @param arg The node to compare equality with.
     * @return Returns <code>true</code> if the nodes are equal,
     *   <code>false</code> otherwise.
     *
     * @since 1.5, DOM Level 3
     */
    public boolean isEqualNode(Node arg);

    /**
     *  This method returns a specialized object which implements the
     * specialized APIs of the specified feature and version, as specified
     * in . The specialized object may also be obtained by using
     * binding-specific casting methods but is not necessarily expected to,
     * as discussed in . This method also allow the implementation to
     * provide specialized objects which do not support the <code>Node</code>
     *  interface.
     * @param feature  The name of the feature requested. Note that any plus
     *   sign "+" prepended to the name of the feature will be ignored since
     *   it is not significant in the context of this method.
     * @param version  This is the version number of the feature to test.
     * @return  Returns an object which implements the specialized APIs of
     *   the specified feature and version, if any, or <code>null</code> if
     *   there is no object which implements interfaces associated with that
     *   feature. If the <code>DOMObject</code> returned by this method
     *   implements the <code>Node</code> interface, it must delegate to the
     *   primary core <code>Node</code> and not return results inconsistent
     *   with the primary core <code>Node</code> such as attributes,
     *   childNodes, etc.
     *
     * @since 1.5, DOM Level 3
     */
    public Object getFeature(String feature,
                             String version);

    /**
     * Associate an object to a key on this node. The object can later be
     * retrieved from this node by calling <code>getUserData</code> with the
     * same key.
     * @param key The key to associate the object to.
     * @param data The object to associate to the given key, or
     *   <code>null</code> to remove any existing association to that key.
     * @param handler The handler to associate to that key, or
     *   <code>null</code>.
     * @return Returns the <code>DOMUserData</code> previously associated to
     *   the given key on this node, or <code>null</code> if there was none.
     *
     * @since 1.5, DOM Level 3
     */
    public Object setUserData(String key,
                              Object data,
                              UserDataHandler handler);

    /**
     * Retrieves the object associated to a key on a this node. The object
     * must first have been set to this node by calling
     * <code>setUserData</code> with the same key.
     * @param key The key the object is associated to.
     * @return Returns the <code>DOMUserData</code> associated to the given
     *   key on this node, or <code>null</code> if there was none.
     *
     * @since 1.5, DOM Level 3
     */
    public Object getUserData(String key);

}
