/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
 */
 /*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.dom;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

/**
 * NodeImpl provides the basic structure of a DOM tree. It is never used
 * directly, but instead is subclassed to add type and data
 * information, and additional methods, appropriate to each node of
 * the tree. Only its subclasses should be instantiated -- and those,
 * with the exception of Document itself, only through a specific
 * Document's factory methods.
 * <P>
 * The Node interface provides shared behaviors such as siblings and
 * children, both for consistancy and so that the most common tree
 * operations may be performed without constantly having to downcast
 * to specific node types. When there is no obvious mapping for one of
 * these queries, it will respond with null.
 * Note that the default behavior is that children are forbidden. To
 * permit them, the subclass ParentNode overrides several methods.
 * <P>
 * NodeImpl also implements NodeList, so it can return itself in
 * response to the getChildNodes() query. This eliminiates the need
 * for a separate ChildNodeList object. Note that this is an
 * IMPLEMENTATION DETAIL; applications should _never_ assume that
 * this identity exists.
 * <P>
 * All nodes in a single document must originate
 * in that document. (Note that this is much tighter than "must be
 * same implementation") Nodes are all aware of their ownerDocument,
 * and attempts to mismatch will throw WRONG_DOCUMENT_ERR.
 * <P>
 * However, to save memory not all nodes always have a direct reference
 * to their ownerDocument. When a node is owned by another node it relies
 * on its owner to store its ownerDocument. Parent nodes always store it
 * though, so there is never more than one level of indirection.
 * And when a node doesn't have an owner, ownerNode refers to its
 * ownerDocument.
 * <p>
 * This class doesn't directly support mutation events, however, it still
 * implements the EventTarget interface and forward all related calls to the
 * document so that the document class do so.
 *
 * @xerces.internal
 *
 * @author Arnaud  Le Hors, IBM
 * @author Joe Kesselman, IBM
 * @since  PR-DOM-Level-1-19980818.
 * @LastModified: Apr 2019
 */
public abstract class NodeImpl
    implements Node, NodeList, EventTarget, Cloneable, Serializable{

    //
    // Constants
    //


    // TreePosition Constants.
    // Taken from DOM L3 Node interface.
    /**
     * The node precedes the reference node.
     */
    public static final short TREE_POSITION_PRECEDING   = 0x01;
    /**
     * The node follows the reference node.
     */
    public static final short TREE_POSITION_FOLLOWING   = 0x02;
    /**
     * The node is an ancestor of the reference node.
     */
    public static final short TREE_POSITION_ANCESTOR    = 0x04;
    /**
     * The node is a descendant of the reference node.
     */
    public static final short TREE_POSITION_DESCENDANT  = 0x08;
    /**
     * The two nodes have an equivalent position. This is the case of two
     * attributes that have the same <code>ownerElement</code>, and two
     * nodes that are the same.
     */
    public static final short TREE_POSITION_EQUIVALENT  = 0x10;
    /**
     * The two nodes are the same. Two nodes that are the same have an
     * equivalent position, though the reverse may not be true.
     */
    public static final short TREE_POSITION_SAME_NODE   = 0x20;
    /**
     * The two nodes are disconnected, they do not have any common ancestor.
     * This is the case of two nodes that are not in the same document.
     */
    public static final short TREE_POSITION_DISCONNECTED = 0x00;

    /** Serialization version. */
    static final long serialVersionUID = -6316591992167219696L;

    // public

    /** Element definition node type. */
    public static final short ELEMENT_DEFINITION_NODE = 21;

    //
    // Data
    //

    // links

    protected NodeImpl ownerNode; // typically the parent but not always!

    // data

    protected short flags;

    protected final static short READONLY     = 0x1<<0;
    protected final static short SYNCDATA     = 0x1<<1;
    protected final static short SYNCCHILDREN = 0x1<<2;
    protected final static short OWNED        = 0x1<<3;
    protected final static short FIRSTCHILD   = 0x1<<4;
    protected final static short SPECIFIED    = 0x1<<5;
    protected final static short IGNORABLEWS  = 0x1<<6;
    protected final static short HASSTRING    = 0x1<<7;
    protected final static short NORMALIZED = 0x1<<8;
    protected final static short ID           = 0x1<<9;

    //
    // Constructors
    //

    /**
     * No public constructor; only subclasses of Node should be
     * instantiated, and those normally via a Document's factory methods
     * <p>
     * Every Node knows what Document it belongs to.
     */
    protected NodeImpl(CoreDocumentImpl ownerDocument) {
        // as long as we do not have any owner, ownerNode is our ownerDocument
        ownerNode = ownerDocument;
    } // <init>(CoreDocumentImpl)

    /** Constructor for serialization. */
    public NodeImpl() {}

    //
    // Node methods
    //

    /**
     * A short integer indicating what type of node this is. The named
     * constants for this value are defined in the org.w3c.dom.Node interface.
     */
    public abstract short getNodeType();

    /**
     * the name of this node.
     */
    public abstract String getNodeName();

    /**
     * Returns the node value.
     * @throws DOMException(DOMSTRING_SIZE_ERR)
     */
    public String getNodeValue()
        throws DOMException {
        return null;            // overridden in some subclasses
    }

    /**
     * Sets the node value.
     * @throws DOMException(NO_MODIFICATION_ALLOWED_ERR)
     */
    public void setNodeValue(String x)
        throws DOMException {
        // Default behavior is to do nothing, overridden in some subclasses
    }

    /**
     * Adds a child node to the end of the list of children for this node.
     * Convenience shorthand for insertBefore(newChild,null).
     * @see #insertBefore(Node, Node)
     * <P>
     * By default we do not accept any children, ParentNode overrides this.
     * @see ParentNode
     *
     * @return newChild, in its new state (relocated, or emptied in the case of
     * DocumentNode.)
     *
     * @throws DOMException(HIERARCHY_REQUEST_ERR) if newChild is of a
     * type that shouldn't be a child of this node.
     *
     * @throws DOMException(WRONG_DOCUMENT_ERR) if newChild has a
     * different owner document than we do.
     *
     * @throws DOMException(NO_MODIFICATION_ALLOWED_ERR) if this node is
     * read-only.
     */
    public Node appendChild(Node newChild) throws DOMException {
        return insertBefore(newChild, null);
    }

    /**
     * Returns a duplicate of a given node. You can consider this a
     * generic "copy constructor" for nodes. The newly returned object should
     * be completely independent of the source object's subtree, so changes
     * in one after the clone has been made will not affect the other.
     * <P>
     * Note: since we never have any children deep is meaningless here,
     * ParentNode overrides this behavior.
     * @see ParentNode
     *
     * <p>
     * Example: Cloning a Text node will copy both the node and the text it
     * contains.
     * <p>
     * Example: Cloning something that has children -- Element or Attr, for
     * example -- will _not_ clone those children unless a "deep clone"
     * has been requested. A shallow clone of an Attr node will yield an
     * empty Attr of the same name.
     * <p>
     * NOTE: Clones will always be read/write, even if the node being cloned
     * is read-only, to permit applications using only the DOM API to obtain
     * editable copies of locked portions of the tree.
     */
    public Node cloneNode(boolean deep) {

        if (needsSyncData()) {
            synchronizeData();
        }

        NodeImpl newnode;
        try {
            newnode = (NodeImpl)clone();
        }
        catch (CloneNotSupportedException e) {
            // if we get here we have an error in our program we may as well
            // be vocal about it, so that people can take appropriate action.
            throw new RuntimeException("**Internal Error**" + e);
        }

        // Need to break the association w/ original kids
        newnode.ownerNode      = ownerDocument();
        newnode.isOwned(false);

        // By default we make all clones readwrite,
        // this is overriden in readonly subclasses
        newnode.isReadOnly(false);

        ownerDocument().callUserDataHandlers(this, newnode,
                                             UserDataHandler.NODE_CLONED);

        return newnode;

    } // cloneNode(boolean):Node

    /**
     * Find the Document that this Node belongs to (the document in
     * whose context the Node was created). The Node may or may not
     * currently be part of that Document's actual contents.
     */
    public Document getOwnerDocument() {
        // if we have an owner simply forward the request
        // otherwise ownerNode is our ownerDocument
        if (isOwned()) {
            return ownerNode.ownerDocument();
        } else {
            return (Document) ownerNode;
        }
    }

    /**
     * same as above but returns internal type and this one is not overridden
     * by CoreDocumentImpl to return null
     */
    CoreDocumentImpl ownerDocument() {
        // if we have an owner simply forward the request
        // otherwise ownerNode is our ownerDocument
        if (isOwned()) {
            return ownerNode.ownerDocument();
        } else {
            return (CoreDocumentImpl) ownerNode;
        }
    }

    /**
     * NON-DOM
     * set the ownerDocument of this node
     */
    protected void setOwnerDocument(CoreDocumentImpl doc) {
        if (needsSyncData()) {
            synchronizeData();
        }
        // if we have an owner we rely on it to have it right
        // otherwise ownerNode is our ownerDocument
        if (!isOwned()) {
            ownerNode = doc;
        }
    }

    /**
     * Returns the node number
     */
    protected int getNodeNumber() {
        int nodeNumber;
        CoreDocumentImpl cd = (CoreDocumentImpl)(this.getOwnerDocument());
        nodeNumber = cd.getNodeNumber(this);
        return nodeNumber;
    }

    /**
     * Obtain the DOM-tree parent of this node, or null if it is not
     * currently active in the DOM tree (perhaps because it has just been
     * created or removed). Note that Document, DocumentFragment, and
     * Attribute will never have parents.
     */
    public Node getParentNode() {
        return null;            // overriden by ChildNode
    }

    /*
     * same as above but returns internal type
     */
    NodeImpl parentNode() {
        return null;
    }

    /** The next child of this node's parent, or null if none */
    public Node getNextSibling() {
        return null;            // default behavior, overriden in ChildNode
    }

    /** The previous child of this node's parent, or null if none */
    public Node getPreviousSibling() {
        return null;            // default behavior, overriden in ChildNode
    }

    ChildNode previousSibling() {
        return null;            // default behavior, overriden in ChildNode
    }

    /**
     * Return the collection of attributes associated with this node,
     * or null if none. At this writing, Element is the only type of node
     * which will ever have attributes.
     *
     * @see ElementImpl
     */
    public NamedNodeMap getAttributes() {
        return null; // overridden in ElementImpl
    }

    /**
     *  Returns whether this node (if it is an element) has any attributes.
     * @return <code>true</code> if this node has any attributes,
     *   <code>false</code> otherwise.
     * @since DOM Level 2
     * @see ElementImpl
     */
    public boolean hasAttributes() {
        return false;           // overridden in ElementImpl
    }

    /**
     * Test whether this node has any children. Convenience shorthand
     * for (Node.getFirstChild()!=null)
     * <P>
     * By default we do not have any children, ParentNode overrides this.
     * @see ParentNode
     */
    public boolean hasChildNodes() {
        return false;
    }

    /**
     * Obtain a NodeList enumerating all children of this node. If there
     * are none, an (initially) empty NodeList is returned.
     * <p>
     * NodeLists are "live"; as children are added/removed the NodeList
     * will immediately reflect those changes. Also, the NodeList refers
     * to the actual nodes, so changes to those nodes made via the DOM tree
     * will be reflected in the NodeList and vice versa.
     * <p>
     * In this implementation, Nodes implement the NodeList interface and
     * provide their own getChildNodes() support. Other DOMs may solve this
     * differently.
     */
    public NodeList getChildNodes() {
        return this;
    }

    /** The first child of this Node, or null if none.
     * <P>
     * By default we do not have any children, ParentNode overrides this.
     * @see ParentNode
     */
    public Node getFirstChild() {
        return null;
    }

    /** The first child of this Node, or null if none.
     * <P>
     * By default we do not have any children, ParentNode overrides this.
     * @see ParentNode
     */
    public Node getLastChild() {
        return null;
    }

    /**
     * Move one or more node(s) to our list of children. Note that this
     * implicitly removes them from their previous parent.
     * <P>
     * By default we do not accept any children, ParentNode overrides this.
     * @see ParentNode
     *
     * @param newChild The Node to be moved to our subtree. As a
     * convenience feature, inserting a DocumentNode will instead insert
     * all its children.
     *
     * @param refChild Current child which newChild should be placed
     * immediately before. If refChild is null, the insertion occurs
     * after all existing Nodes, like appendChild().
     *
     * @return newChild, in its new state (relocated, or emptied in the case of
     * DocumentNode.)
     *
     * @throws DOMException(HIERARCHY_REQUEST_ERR) if newChild is of a
     * type that shouldn't be a child of this node, or if newChild is an
     * ancestor of this node.
     *
     * @throws DOMException(WRONG_DOCUMENT_ERR) if newChild has a
     * different owner document than we do.
     *
     * @throws DOMException(NOT_FOUND_ERR) if refChild is not a child of
     * this node.
     *
     * @throws DOMException(NO_MODIFICATION_ALLOWED_ERR) if this node is
     * read-only.
     */
    public Node insertBefore(Node newChild, Node refChild)
        throws DOMException {
        throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,
              DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN,
                 "HIERARCHY_REQUEST_ERR", null));
    }

    /**
     * Remove a child from this Node. The removed child's subtree
     * remains intact so it may be re-inserted elsewhere.
     * <P>
     * By default we do not have any children, ParentNode overrides this.
     * @see ParentNode
     *
     * @return oldChild, in its new state (removed).
     *
     * @throws DOMException(NOT_FOUND_ERR) if oldChild is not a child of
     * this node.
     *
     * @throws DOMException(NO_MODIFICATION_ALLOWED_ERR) if this node is
     * read-only.
     */
    public Node removeChild(Node oldChild)
                throws DOMException {
        throw new DOMException(DOMException.NOT_FOUND_ERR,
              DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN,
                 "NOT_FOUND_ERR", null));
    }

    /**
     * Make newChild occupy the location that oldChild used to
     * have. Note that newChild will first be removed from its previous
     * parent, if any. Equivalent to inserting newChild before oldChild,
     * then removing oldChild.
     * <P>
     * By default we do not have any children, ParentNode overrides this.
     * @see ParentNode
     *
     * @return oldChild, in its new state (removed).
     *
     * @throws DOMException(HIERARCHY_REQUEST_ERR) if newChild is of a
     * type that shouldn't be a child of this node, or if newChild is
     * one of our ancestors.
     *
     * @throws DOMException(WRONG_DOCUMENT_ERR) if newChild has a
     * different owner document than we do.
     *
     * @throws DOMException(NOT_FOUND_ERR) if oldChild is not a child of
     * this node.
     *
     * @throws DOMException(NO_MODIFICATION_ALLOWED_ERR) if this node is
     * read-only.
     */
    public Node replaceChild(Node newChild, Node oldChild)
        throws DOMException {
        throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,
              DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN,
                 "HIERARCHY_REQUEST_ERR", null));
    }

    //
    // NodeList methods
    //

    /**
     * NodeList method: Count the immediate children of this node
     * <P>
     * By default we do not have any children, ParentNode overrides this.
     * @see ParentNode
     *
     * @return int
     */
    public int getLength() {
        return 0;
    }

    /**
     * NodeList method: Return the Nth immediate child of this node, or
     * null if the index is out of bounds.
     * <P>
     * By default we do not have any children, ParentNode overrides this.
     * @see ParentNode
     *
     * @return org.w3c.dom.Node
     * @param index int
     */
    public Node item(int index) {
        return null;
    }

    //
    // DOM2: methods, getters, setters
    //

    /**
     * Puts all <code>Text</code> nodes in the full depth of the sub-tree
     * underneath this <code>Node</code>, including attribute nodes, into a
     * "normal" form where only markup (e.g., tags, comments, processing
     * instructions, CDATA sections, and entity references) separates
     * <code>Text</code> nodes, i.e., there are no adjacent <code>Text</code>
     * nodes.  This can be used to ensure that the DOM view of a document is
     * the same as if it were saved and re-loaded, and is useful when
     * operations (such as XPointer lookups) that depend on a particular
     * document tree structure are to be used.In cases where the document
     * contains <code>CDATASections</code>, the normalize operation alone may
     * not be sufficient, since XPointers do not differentiate between
     * <code>Text</code> nodes and <code>CDATASection</code> nodes.
     * <p>
     * Note that this implementation simply calls normalize() on this Node's
     * children. It is up to implementors or Node to override normalize()
     * to take action.
     */
    public void normalize() {
        /* by default we do not have any children,
           ParentNode overrides this behavior */
    }

    /**
     * Introduced in DOM Level 2. <p>
     * Tests whether the DOM implementation implements a specific feature and
     * that feature is supported by this node.
     * @param feature The package name of the feature to test. This is the same
     * name as what can be passed to the method hasFeature on
     * DOMImplementation.
     * @param version This is the version number of the package name to
     * test. In Level 2, version 1, this is the string "2.0". If the version is
     * not specified, supporting any version of the feature will cause the
     * method to return true.
     * @return boolean Returns true if this node defines a subtree within which
     * the specified feature is supported, false otherwise.
     * @since WD-DOM-Level-2-19990923
     */
    public boolean isSupported(String feature, String version)
    {
        return ownerDocument().getImplementation().hasFeature(feature,
                                                              version);
    }

    /**
     * Introduced in DOM Level 2. <p>
     *
     * The namespace URI of this node, or null if it is unspecified. When this
     * node is of any type other than ELEMENT_NODE and ATTRIBUTE_NODE, this is
     * always null and setting it has no effect. <p>
     *
     * This is not a computed value that is the result of a namespace lookup
     * based on an examination of the namespace declarations in scope. It is
     * merely the namespace URI given at creation time.<p>
     *
     * For nodes created with a DOM Level 1 method, such as createElement
     * from the Document interface, this is null.
     * @since WD-DOM-Level-2-19990923
     * @see AttrNSImpl
     * @see ElementNSImpl
     */
    public String getNamespaceURI()
    {
        return null;
    }

    /**
     * Introduced in DOM Level 2. <p>
     *
     * The namespace prefix of this node, or null if it is unspecified. When
     * this node is of any type other than ELEMENT_NODE and ATTRIBUTE_NODE this
     * is always null and setting it has no effect.<p>
     *
     * For nodes created with a DOM Level 1 method, such as createElement
     * from the Document interface, this is null. <p>
     *
     * @since WD-DOM-Level-2-19990923
     * @see AttrNSImpl
     * @see ElementNSImpl
     */
    public String getPrefix()
    {
        return null;
    }

    /**
     *  Introduced in DOM Level 2. <p>
     *
     *  The namespace prefix of this node, or null if it is unspecified. When
     *  this node is of any type other than ELEMENT_NODE and ATTRIBUTE_NODE
     *  this is always null and setting it has no effect.<p>
     *
     *  For nodes created with a DOM Level 1 method, such as createElement from
     *  the Document interface, this is null.<p>
     *
     *  Note that setting this attribute changes the nodeName attribute, which
     *  holds the qualified name, as well as the tagName and name attributes of
     *  the Element and Attr interfaces, when applicable.<p>
     *
     * @throws INVALID_CHARACTER_ERR Raised if the specified
     *  prefix contains an invalid character.
     *
     * @since WD-DOM-Level-2-19990923
     * @see AttrNSImpl
     * @see ElementNSImpl
     */
    public void setPrefix(String prefix)
        throws DOMException
    {
        throw new DOMException(DOMException.NAMESPACE_ERR,
              DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN,
                 "NAMESPACE_ERR", null));
    }

    /**
     * Introduced in DOM Level 2. <p>
     *
     * Returns the local part of the qualified name of this node.
     * For nodes created with a DOM Level 1 method, such as createElement
     * from the Document interface, and for nodes of any type other than
     * ELEMENT_NODE and ATTRIBUTE_NODE this is the same as the nodeName
     * attribute.
     * @since WD-DOM-Level-2-19990923
     * @see AttrNSImpl
     * @see ElementNSImpl
     */
    public String             getLocalName()
    {
        return null;
    }

    //
    // EventTarget support
    //

    public void addEventListener(String type, EventListener listener,
                                 boolean useCapture) {
        // simply forward to Document
        ownerDocument().addEventListener(this, type, listener, useCapture);
    }

    public void removeEventListener(String type, EventListener listener,
                                    boolean useCapture) {
        // simply forward to Document
        ownerDocument().removeEventListener(this, type, listener, useCapture);
    }

    public boolean dispatchEvent(Event event) {
        // simply forward to Document
        return ownerDocument().dispatchEvent(this, event);
    }

    //
    // Public DOM Level 3 methods
    //

    /**
     * The absolute base URI of this node or <code>null</code> if undefined.
     * This value is computed according to . However, when the
     * <code>Document</code> supports the feature "HTML" , the base URI is
     * computed using first the value of the href attribute of the HTML BASE
     * element if any, and the value of the <code>documentURI</code>
     * attribute from the <code>Document</code> interface otherwise.
     * <br> When the node is an <code>Element</code>, a <code>Document</code>
     * or a a <code>ProcessingInstruction</code>, this attribute represents
     * the properties [base URI] defined in . When the node is a
     * <code>Notation</code>, an <code>Entity</code>, or an
     * <code>EntityReference</code>, this attribute represents the
     * properties [declaration base URI] in the . How will this be affected
     * by resolution of relative namespace URIs issue?It's not.Should this
     * only be on Document, Element, ProcessingInstruction, Entity, and
     * Notation nodes, according to the infoset? If not, what is it equal to
     * on other nodes? Null? An empty string? I think it should be the
     * parent's.No.Should this be read-only and computed or and actual
     * read-write attribute?Read-only and computed (F2F 19 Jun 2000 and
     * teleconference 30 May 2001).If the base HTML element is not yet
     * attached to a document, does the insert change the Document.baseURI?
     * Yes. (F2F 26 Sep 2001)
     * @since DOM Level 3
     */
    public String getBaseURI() {
        return null;
    }

    /**
     * Compares a node with this node with regard to their position in the
     * tree and according to the document order. This order can be extended
     * by module that define additional types of nodes.
     * @param other The node to compare against this node.
     * @return Returns how the given node is positioned relatively to this
     *   node.
     * @since DOM Level 3
     * @deprecated
     */
    @Deprecated
    public short compareTreePosition(Node other) {
        // Questions of clarification for this method - to be answered by the
        // DOM WG.   Current assumptions listed - LM
        //
        // 1. How do ENTITY nodes compare?
        //    Current assumption: TREE_POSITION_DISCONNECTED, as ENTITY nodes
        //    aren't really 'in the tree'
        //
        // 2. How do NOTATION nodes compare?
        //    Current assumption: TREE_POSITION_DISCONNECTED, as NOTATION nodes
        //    aren't really 'in the tree'
        //
        // 3. Are TREE_POSITION_ANCESTOR and TREE_POSITION_DESCENDANT
        //    only relevant for nodes that are "part of the document tree"?
        //     <outer>
        //         <inner  myattr="true"/>
        //     </outer>
        //    Is the element node "outer" considered an ancestor of "myattr"?
        //    Current assumption: No.
        //
        // 4. How do children of ATTRIBUTE nodes compare (with eachother, or
        //    with children of other attribute nodes with the same element)
        //    Current assumption: Children of ATTRIBUTE nodes are treated as if
        //    they they are the attribute node itself, unless the 2 nodes
        //    are both children of the same attribute.
        //
        // 5. How does an ENTITY_REFERENCE node compare with it's children?
        //    Given the DOM, it should precede its children as an ancestor.
        //    Given "document order",  does it represent the same position?
        //    Current assumption: An ENTITY_REFERENCE node is an ancestor of its
        //    children.
        //
        // 6. How do children of a DocumentFragment compare?
        //    Current assumption: If both nodes are part of the same document
        //    fragment, there are compared as if they were part of a document.


        // If the nodes are the same...
        if (this==other)
          return (TREE_POSITION_SAME_NODE | TREE_POSITION_EQUIVALENT);

        // If either node is of type ENTITY or NOTATION, compare as disconnected
        short thisType = this.getNodeType();
        short otherType = other.getNodeType();

        // If either node is of type ENTITY or NOTATION, compare as disconnected
        if (thisType == Node.ENTITY_NODE ||
            thisType == Node.NOTATION_NODE ||
            otherType == Node.ENTITY_NODE ||
            otherType == Node.NOTATION_NODE ) {
          return TREE_POSITION_DISCONNECTED;
        }

        // Find the ancestor of each node, and the distance each node is from
        // its ancestor.
        // During this traversal, look for ancestor/descendent relationships
        // between the 2 nodes in question.
        // We do this now, so that we get this info correct for attribute nodes
        // and their children.

        Node node;
        Node thisAncestor = this;
        Node otherAncestor = other;
        int thisDepth=0;
        int otherDepth=0;
        for (node=this; node != null; node = node.getParentNode()) {
            thisDepth +=1;
            if (node == other)
              // The other node is an ancestor of this one.
              return (TREE_POSITION_ANCESTOR | TREE_POSITION_PRECEDING);
            thisAncestor = node;
        }

        for (node=other; node!=null; node=node.getParentNode()) {
            otherDepth +=1;
            if (node == this)
              // The other node is a descendent of the reference node.
              return (TREE_POSITION_DESCENDANT | TREE_POSITION_FOLLOWING);
            otherAncestor = node;
        }


        Node thisNode = this;
        Node otherNode = other;

        int thisAncestorType = thisAncestor.getNodeType();
        int otherAncestorType = otherAncestor.getNodeType();

        // if the ancestor is an attribute, get owning element.
        // we are now interested in the owner to determine position.

        if (thisAncestorType == Node.ATTRIBUTE_NODE)  {
           thisNode = ((AttrImpl)thisAncestor).getOwnerElement();
        }
        if (otherAncestorType == Node.ATTRIBUTE_NODE) {
           otherNode = ((AttrImpl)otherAncestor).getOwnerElement();
        }

        // Before proceeding, we should check if both ancestor nodes turned
        // out to be attributes for the same element
        if (thisAncestorType == Node.ATTRIBUTE_NODE &&
            otherAncestorType == Node.ATTRIBUTE_NODE &&
            thisNode==otherNode)
            return TREE_POSITION_EQUIVALENT;

        // Now, find the ancestor of the owning element, if the original
        // ancestor was an attribute

        // Note:  the following 2 loops are quite close to the ones above.
        // May want to common them up.  LM.
        if (thisAncestorType == Node.ATTRIBUTE_NODE) {
            thisDepth=0;
            for (node=thisNode; node != null; node=node.getParentNode()) {
                thisDepth +=1;
                if (node == otherNode)
                  // The other node is an ancestor of the owning element
                  {
                  return TREE_POSITION_PRECEDING;
                  }
                thisAncestor = node;
            }
        }

        // Now, find the ancestor of the owning element, if the original
        // ancestor was an attribute
        if (otherAncestorType == Node.ATTRIBUTE_NODE) {
            otherDepth=0;
            for (node=otherNode; node != null; node=node.getParentNode()) {
                otherDepth +=1;
                if (node == thisNode)
                  // The other node is a descendent of the reference
                  // node's element
                  return TREE_POSITION_FOLLOWING;
                otherAncestor = node;
            }
        }

        // thisAncestor and otherAncestor must be the same at this point,
        // otherwise, we are not in the same tree or document fragment
        if (thisAncestor != otherAncestor)
          return TREE_POSITION_DISCONNECTED;


        // Go up the parent chain of the deeper node, until we find a node
        // with the same depth as the shallower node

        if (thisDepth > otherDepth) {
          for (int i=0; i<thisDepth - otherDepth; i++)
            thisNode = thisNode.getParentNode();
          // Check if the node we have reached is in fact "otherNode". This can
          // happen in the case of attributes.  In this case, otherNode
          // "precedes" this.
          if (thisNode == otherNode)
            return TREE_POSITION_PRECEDING;
        }

        else {
          for (int i=0; i<otherDepth - thisDepth; i++)
            otherNode = otherNode.getParentNode();
          // Check if the node we have reached is in fact "thisNode".  This can
          // happen in the case of attributes.  In this case, otherNode
          // "follows" this.
          if (otherNode == thisNode)
            return TREE_POSITION_FOLLOWING;
        }

        // We now have nodes at the same depth in the tree.  Find a common
        // ancestor.
        Node thisNodeP, otherNodeP;
        for (thisNodeP=thisNode.getParentNode(),
                  otherNodeP=otherNode.getParentNode();
             thisNodeP!=otherNodeP;) {
             thisNode = thisNodeP;
             otherNode = otherNodeP;
             thisNodeP = thisNodeP.getParentNode();
             otherNodeP = otherNodeP.getParentNode();
        }

        // At this point, thisNode and otherNode are direct children of
        // the common ancestor.
        // See whether thisNode or otherNode is the leftmost

        for (Node current=thisNodeP.getFirstChild();
                  current!=null;
                  current=current.getNextSibling()) {
               if (current==otherNode) {
                 return TREE_POSITION_PRECEDING;
               }
               else if (current==thisNode) {
                 return TREE_POSITION_FOLLOWING;
               }
        }
        // REVISIT:  shouldn't get here.   Should probably throw an
        // exception
        return 0;

    }
    /**
     * Compares a node with this node with regard to their position in the
     * document.
     * @param other The node to compare against this node.
     * @return Returns how the given node is positioned relatively to this
     *   node.
     * @since DOM Level 3
     */
    public short compareDocumentPosition(Node other) throws DOMException {

        // If the nodes are the same, no flags should be set
        if (this==other)
          return 0;

        // check if other is from a different implementation
        if (other != null && !(other instanceof NodeImpl)) {
            // other comes from a different implementation
            String msg = DOMMessageFormatter.formatMessage(
               DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
            throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
        }

        Document thisOwnerDoc, otherOwnerDoc;
        // get the respective Document owners.
        if (this.getNodeType() == Node.DOCUMENT_NODE)
          thisOwnerDoc = (Document)this;
        else
          thisOwnerDoc = this.getOwnerDocument();
        if (other.getNodeType() == Node.DOCUMENT_NODE)
          otherOwnerDoc = (Document)other;
        else
          otherOwnerDoc = other.getOwnerDocument();

        // If from different documents, we know they are disconnected.
        // and have an implementation dependent order
        if (thisOwnerDoc != otherOwnerDoc &&
            thisOwnerDoc !=null &&
            otherOwnerDoc !=null)
 {
          int otherDocNum = ((CoreDocumentImpl)otherOwnerDoc).getNodeNumber();
          int thisDocNum = ((CoreDocumentImpl)thisOwnerDoc).getNodeNumber();
          if (otherDocNum > thisDocNum)
            return DOCUMENT_POSITION_DISCONNECTED |
                   DOCUMENT_POSITION_FOLLOWING |
                   DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC;
          else
            return DOCUMENT_POSITION_DISCONNECTED |
                   DOCUMENT_POSITION_PRECEDING |
                   DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC;

        }

        // Find the ancestor of each node, and the distance each node is from
        // its ancestor.
        // During this traversal, look for ancestor/descendent relationships
        // between the 2 nodes in question.
        // We do this now, so that we get this info correct for attribute nodes
        // and their children.

        Node node;
        Node thisAncestor = this;
        Node otherAncestor = other;

        int thisDepth=0;
        int otherDepth=0;
        for (node=this; node != null; node = node.getParentNode()) {
            thisDepth +=1;
            if (node == other)
              // The other node is an ancestor of this one.
              return (DOCUMENT_POSITION_CONTAINS |
                      DOCUMENT_POSITION_PRECEDING);
            thisAncestor = node;
        }

        for (node=other; node!=null; node=node.getParentNode()) {
            otherDepth +=1;
            if (node == this)
              // The other node is a descendent of the reference node.
              return (DOCUMENT_POSITION_CONTAINED_BY |
                      DOCUMENT_POSITION_FOLLOWING);
            otherAncestor = node;
        }



        int thisAncestorType = thisAncestor.getNodeType();
        int otherAncestorType = otherAncestor.getNodeType();
        Node thisNode = this;
        Node otherNode = other;

        // Special casing for ENTITY, NOTATION, DOCTYPE and ATTRIBUTES
        // LM:  should rewrite this.
        switch (thisAncestorType) {
          case Node.NOTATION_NODE:
          case Node.ENTITY_NODE: {
            DocumentType container = thisOwnerDoc.getDoctype();
            if (container == otherAncestor) return
                   (DOCUMENT_POSITION_CONTAINS | DOCUMENT_POSITION_PRECEDING);
            switch (otherAncestorType) {
              case Node.NOTATION_NODE:
              case Node.ENTITY_NODE:  {
                if (thisAncestorType != otherAncestorType)
                 // the nodes are of different types
                 return ((thisAncestorType>otherAncestorType) ?
                    DOCUMENT_POSITION_PRECEDING:DOCUMENT_POSITION_FOLLOWING);
                else {
                 // the nodes are of the same type.  Find order.
                 if (thisAncestorType == Node.NOTATION_NODE)

                     if (((NamedNodeMapImpl)container.getNotations()).precedes(otherAncestor,thisAncestor))
                       return (DOCUMENT_POSITION_PRECEDING |
                               DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC);
                     else
                       return (DOCUMENT_POSITION_FOLLOWING |
                               DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC);
                 else
                     if (((NamedNodeMapImpl)container.getEntities()).precedes(otherAncestor,thisAncestor))
                       return (DOCUMENT_POSITION_PRECEDING |
                               DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC);
                     else
                       return (DOCUMENT_POSITION_FOLLOWING |
                               DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC);
                }
              }
            }
            thisNode = thisAncestor = thisOwnerDoc;
            break;
          }
          case Node.DOCUMENT_TYPE_NODE: {
            if (otherNode == thisOwnerDoc)
              return (DOCUMENT_POSITION_PRECEDING |
                      DOCUMENT_POSITION_CONTAINS);
            else if (thisOwnerDoc!=null && thisOwnerDoc==otherOwnerDoc)
              return (DOCUMENT_POSITION_FOLLOWING);
            break;
          }
          case Node.ATTRIBUTE_NODE: {
            thisNode = ((AttrImpl)thisAncestor).getOwnerElement();
            if (otherAncestorType==Node.ATTRIBUTE_NODE) {
              otherNode = ((AttrImpl)otherAncestor).getOwnerElement();
              if (otherNode == thisNode) {
                if (((NamedNodeMapImpl)thisNode.getAttributes()).precedes(other,this))
                  return (DOCUMENT_POSITION_PRECEDING |
                          DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC);
                else
                  return (DOCUMENT_POSITION_FOLLOWING |
                          DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC);
              }
            }

            // Now, find the ancestor of the element
            thisDepth=0;
            for (node=thisNode; node != null; node=node.getParentNode()) {
                thisDepth +=1;
                if (node == otherNode)
                  {
                  // The other node is an ancestor of the owning element
                  return (DOCUMENT_POSITION_CONTAINS |
                          DOCUMENT_POSITION_PRECEDING);
                  }
                thisAncestor = node;
            }
          }
        }
        switch (otherAncestorType) {
          case Node.NOTATION_NODE:
          case Node.ENTITY_NODE: {
          DocumentType container = thisOwnerDoc.getDoctype();
            if (container == this) return (DOCUMENT_POSITION_CONTAINED_BY |
                                          DOCUMENT_POSITION_FOLLOWING);
            otherNode = otherAncestor = thisOwnerDoc;
            break;
          }
          case Node.DOCUMENT_TYPE_NODE: {
            if (thisNode == otherOwnerDoc)
              return (DOCUMENT_POSITION_FOLLOWING |
                      DOCUMENT_POSITION_CONTAINED_BY);
            else if (otherOwnerDoc!=null && thisOwnerDoc==otherOwnerDoc)
              return (DOCUMENT_POSITION_PRECEDING);
            break;
          }
          case Node.ATTRIBUTE_NODE: {
            otherDepth=0;
            otherNode = ((AttrImpl)otherAncestor).getOwnerElement();
            for (node=otherNode; node != null; node=node.getParentNode()) {
                otherDepth +=1;
                if (node == thisNode)
                  // The other node is a descendent of the reference
                  // node's element
                  return DOCUMENT_POSITION_FOLLOWING |
                         DOCUMENT_POSITION_CONTAINED_BY;
                otherAncestor = node;
            }

          }
        }

        // thisAncestor and otherAncestor must be the same at this point,
        // otherwise, the original nodes are disconnected
        if (thisAncestor != otherAncestor) {
          int thisAncestorNum, otherAncestorNum;
          thisAncestorNum = ((NodeImpl)thisAncestor).getNodeNumber();
          otherAncestorNum = ((NodeImpl)otherAncestor).getNodeNumber();

          if (thisAncestorNum > otherAncestorNum)
            return DOCUMENT_POSITION_DISCONNECTED |
                   DOCUMENT_POSITION_FOLLOWING |
                   DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC;
          else
            return DOCUMENT_POSITION_DISCONNECTED |
                   DOCUMENT_POSITION_PRECEDING |
                   DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC;
        }


        // Go up the parent chain of the deeper node, until we find a node
        // with the same depth as the shallower node

        if (thisDepth > otherDepth) {
          for (int i=0; i<thisDepth - otherDepth; i++)
            thisNode = thisNode.getParentNode();
          // Check if the node we have reached is in fact "otherNode". This can
          // happen in the case of attributes.  In this case, otherNode
          // "precedes" this.
          if (thisNode == otherNode)
{
            return DOCUMENT_POSITION_PRECEDING;
          }
        }

        else {
          for (int i=0; i<otherDepth - thisDepth; i++)
            otherNode = otherNode.getParentNode();
          // Check if the node we have reached is in fact "thisNode".  This can
          // happen in the case of attributes.  In this case, otherNode
          // "follows" this.
          if (otherNode == thisNode)
            return DOCUMENT_POSITION_FOLLOWING;
        }

        // We now have nodes at the same depth in the tree.  Find a common
        // ancestor.
        Node thisNodeP, otherNodeP;
        for (thisNodeP=thisNode.getParentNode(),
                  otherNodeP=otherNode.getParentNode();
             thisNodeP!=otherNodeP;) {
             thisNode = thisNodeP;
             otherNode = otherNodeP;
             thisNodeP = thisNodeP.getParentNode();
             otherNodeP = otherNodeP.getParentNode();
        }

        // At this point, thisNode and otherNode are direct children of
        // the common ancestor.
        // See whether thisNode or otherNode is the leftmost

        for (Node current=thisNodeP.getFirstChild();
                  current!=null;
                  current=current.getNextSibling()) {
               if (current==otherNode) {
                 return DOCUMENT_POSITION_PRECEDING;
               }
               else if (current==thisNode) {
                 return DOCUMENT_POSITION_FOLLOWING;
               }
        }
        // REVISIT:  shouldn't get here.   Should probably throw an
        // exception
        return 0;

    }

    /**
     * This attribute returns the text content of this node and its
     * descendants. When it is defined to be null, setting it has no effect.
     * When set, any possible children this node may have are removed and
     * replaced by a single <code>Text</code> node containing the string
     * this attribute is set to. On getting, no serialization is performed,
     * the returned string does not contain any markup. No whitespace
     * normalization is performed, the returned string does not contain the
     * element content whitespaces . Similarly, on setting, no parsing is
     * performed either, the input string is taken as pure textual content.
     * <br>The string returned is made of the text content of this node
     * depending on its type, as defined below:
     * <table border='1'>
     * <tr>
     * <th>Node type</th>
     * <th>Content</th>
     * </tr>

    /**
     * This attribute returns the text content of this node and its
     * descendants. When it is defined to be null, setting it has no effect.
     * When set, any possible children this node may have are removed and
     * replaced by a single <code>Text</code> node containing the string
     * this attribute is set to. On getting, no serialization is performed,
     * the returned string does not contain any markup. No whitespace
     * normalization is performed, the returned string does not contain the
     * element content whitespaces . Similarly, on setting, no parsing is
     * performed either, the input string is taken as pure textual content.
     * <br>The string returned is made of the text content of this node
     * depending on its type, as defined below:
     * <table border='1'>
     * <tr>
     * <th>Node type</th>
     * <th>Content</th>
     * </tr>
     * <tr>
     * <td valign='top' rowspan='1' colspan='1'>
     * ELEMENT_NODE, ENTITY_NODE, ENTITY_REFERENCE_NODE,
     * DOCUMENT_FRAGMENT_NODE</td>
     * <td valign='top' rowspan='1' colspan='1'>concatenation of the <code>textContent</code>
     * attribute value of every child node, excluding COMMENT_NODE and
     * PROCESSING_INSTRUCTION_NODE nodes</td>
     * </tr>
     * <tr>
     * <td valign='top' rowspan='1' colspan='1'>ATTRIBUTE_NODE, TEXT_NODE,
     * CDATA_SECTION_NODE, COMMENT_NODE, PROCESSING_INSTRUCTION_NODE</td>
     * <td valign='top' rowspan='1' colspan='1'>
     * <code>nodeValue</code></td>
     * </tr>
     * <tr>
     * <td valign='top' rowspan='1' colspan='1'>DOCUMENT_NODE, DOCUMENT_TYPE_NODE, NOTATION_NODE</td>
     * <td valign='top' rowspan='1' colspan='1'>
     * null</td>
     * </tr>
     * </table>
     * @exception DOMException
     *   NO_MODIFICATION_ALLOWED_ERR: Raised when the node is readonly.
     * @exception DOMException
     *   DOMSTRING_SIZE_ERR: Raised when it would return more characters than
     *   fit in a <code>DOMString</code> variable on the implementation
     *   platform.
     * @since DOM Level 3
     */
    public String getTextContent() throws DOMException {
        return getNodeValue();  // overriden in some subclasses
    }

    // internal method taking a StringBuilder in parameter
    void getTextContent(StringBuilder buf) throws DOMException {
        String content = getNodeValue();
        if (content != null) {
            buf.append(content);
        }
    }

    /**
     * This attribute returns the text content of this node and its
     * descendants. When it is defined to be null, setting it has no effect.
     * When set, any possible children this node may have are removed and
     * replaced by a single <code>Text</code> node containing the string
     * this attribute is set to. On getting, no serialization is performed,
     * the returned string does not contain any markup. No whitespace
     * normalization is performed, the returned string does not contain the
     * element content whitespaces . Similarly, on setting, no parsing is
     * performed either, the input string is taken as pure textual content.
     * <br>The string returned is made of the text content of this node
     * depending on its type, as defined below:
     * <table border='1'>
     * <tr>
     * <th>Node type</th>
     * <th>Content</th>
     * </tr>
     * <tr>
     * <td valign='top' rowspan='1' colspan='1'>
     * ELEMENT_NODE, ENTITY_NODE, ENTITY_REFERENCE_NODE,
     * DOCUMENT_FRAGMENT_NODE</td>
     * <td valign='top' rowspan='1' colspan='1'>concatenation of the <code>textContent</code>
     * attribute value of every child node, excluding COMMENT_NODE and
     * PROCESSING_INSTRUCTION_NODE nodes</td>
     * </tr>
     * <tr>
     * <td valign='top' rowspan='1' colspan='1'>ATTRIBUTE_NODE, TEXT_NODE,
     * CDATA_SECTION_NODE, COMMENT_NODE, PROCESSING_INSTRUCTION_NODE</td>
     * <td valign='top' rowspan='1' colspan='1'>
     * <code>nodeValue</code></td>
     * </tr>
     * <tr>
     * <td valign='top' rowspan='1' colspan='1'>DOCUMENT_NODE, DOCUMENT_TYPE_NODE, NOTATION_NODE</td>
     * <td valign='top' rowspan='1' colspan='1'>
     * null</td>
     * </tr>
     * </table>
     * @exception DOMException
     *   NO_MODIFICATION_ALLOWED_ERR: Raised when the node is readonly.
     * @exception DOMException
     *   DOMSTRING_SIZE_ERR: Raised when it would return more characters than
     *   fit in a <code>DOMString</code> variable on the implementation
     *   platform.
     * @since DOM Level 3
     */
    public void setTextContent(String textContent)
        throws DOMException {
        setNodeValue(textContent);
    }

    /**
     * Returns whether this node is the same node as the given one.
     * <br>This method provides a way to determine whether two
     * <code>Node</code> references returned by the implementation reference
     * the same object. When two <code>Node</code> references are references
     * to the same object, even if through a proxy, the references may be
     * used completely interchangably, such that all attributes have the
     * same values and calling the same DOM method on either reference
     * always has exactly the same effect.
     * @param other The node to test against.
     * @return Returns <code>true</code> if the nodes are the same,
     *   <code>false</code> otherwise.
     * @since DOM Level 3
     */
    public boolean isSameNode(Node other) {
        // we do not use any wrapper so the answer is obvious
        return this == other;
    }




    /**
     *  DOM Level 3: Experimental
     *  This method checks if the specified <code>namespaceURI</code> is the
     *  default namespace or not.
     *  @param namespaceURI The namespace URI to look for.
     *  @return  <code>true</code> if the specified <code>namespaceURI</code>
     *   is the default namespace, <code>false</code> otherwise.
     * @since DOM Level 3
     */
    public boolean isDefaultNamespace(String namespaceURI){
        // REVISIT: remove casts when DOM L3 becomes REC.
        short type = this.getNodeType();
        switch (type) {
        case Node.ELEMENT_NODE: {
            String namespace = this.getNamespaceURI();
            String prefix = this.getPrefix();

            // REVISIT: is it possible that prefix is empty string?
            if (prefix == null || prefix.length() == 0) {
                if (namespaceURI == null) {
                    return (namespace == namespaceURI);
                }
                return namespaceURI.equals(namespace);
            }
            if (this.hasAttributes()) {
                ElementImpl elem = (ElementImpl)this;
                NodeImpl attr = (NodeImpl)elem.getAttributeNodeNS("http://www.w3.org/2000/xmlns/", "xmlns");
                if (attr != null) {
                    String value = attr.getNodeValue();
                    if (namespaceURI == null) {
                        return (namespace == value);
                    }
                    return namespaceURI.equals(value);
                }
            }

            NodeImpl ancestor = (NodeImpl)getElementAncestor(this);
            if (ancestor != null) {
                return ancestor.isDefaultNamespace(namespaceURI);
            }
            return false;
        }
        case Node.DOCUMENT_NODE:{
                Element docElement = ((Document)this).getDocumentElement();
                if (docElement != null) {
                    return docElement.isDefaultNamespace(namespaceURI);
                }
                return false;
            }

        case Node.ENTITY_NODE :
        case Node.NOTATION_NODE:
        case Node.DOCUMENT_FRAGMENT_NODE:
        case Node.DOCUMENT_TYPE_NODE:
            // type is unknown
            return false;
        case Node.ATTRIBUTE_NODE:{
                if (this.ownerNode.getNodeType() == Node.ELEMENT_NODE) {
                    return ownerNode.isDefaultNamespace(namespaceURI);

                }
                return false;
            }
        default:{
                NodeImpl ancestor = (NodeImpl)getElementAncestor(this);
                if (ancestor != null) {
                    return ancestor.isDefaultNamespace(namespaceURI);
                }
                return false;
            }

        }


    }


    /**
     *
     * DOM Level 3 - Experimental:
     * Look up the prefix associated to the given namespace URI, starting from this node.
     *
     * @param namespaceURI
     * @return the prefix for the namespace
     */
    public String lookupPrefix(String namespaceURI){

        // REVISIT: When Namespaces 1.1 comes out this may not be true
        // Prefix can't be bound to null namespace
        if (namespaceURI == null) {
            return null;
        }

        short type = this.getNodeType();

        switch (type) {
        case Node.ELEMENT_NODE: {
                this.getNamespaceURI(); // to flip out children
                return lookupNamespacePrefix(namespaceURI, (ElementImpl)this);
            }
        case Node.DOCUMENT_NODE:{
                Element docElement = ((Document)this).getDocumentElement();
                if (docElement != null) {
                    return docElement.lookupPrefix(namespaceURI);
                }
                return null;
            }

        case Node.ENTITY_NODE :
        case Node.NOTATION_NODE:
        case Node.DOCUMENT_FRAGMENT_NODE:
        case Node.DOCUMENT_TYPE_NODE:
            // type is unknown
            return null;
        case Node.ATTRIBUTE_NODE:{
                if (this.ownerNode.getNodeType() == Node.ELEMENT_NODE) {
                    return ownerNode.lookupPrefix(namespaceURI);

                }
                return null;
            }
        default:{
                NodeImpl ancestor = (NodeImpl)getElementAncestor(this);
                if (ancestor != null) {
                    return ancestor.lookupPrefix(namespaceURI);
                }
                return null;
            }

        }
    }
    /**
     * DOM Level 3 - Experimental:
     * Look up the namespace URI associated to the given prefix, starting from this node.
     * Use lookupNamespaceURI(null) to lookup the default namespace
     *
     * @param specifiedPrefix
     * @return the URI for the namespace
     * @since DOM Level 3
     */
    public String lookupNamespaceURI(String specifiedPrefix) {
        short type = this.getNodeType();
        switch (type) {
        case Node.ELEMENT_NODE : {

                String namespace = this.getNamespaceURI();
                String prefix = this.getPrefix();
                if (namespace !=null) {
                    // REVISIT: is it possible that prefix is empty string?
                    if (specifiedPrefix== null && prefix==specifiedPrefix) {
                        // looking for default namespace
                        return namespace;
                    } else if (prefix != null && prefix.equals(specifiedPrefix)) {
                        // non default namespace
                        return namespace;
                    }
                }
                if (this.hasAttributes()) {
                    NamedNodeMap map = this.getAttributes();
                    int length = map.getLength();
                    for (int i=0;i<length;i++) {
                        Node attr = map.item(i);
                        namespace = attr.getNamespaceURI();
                        if (namespace !=null && namespace.equals("http://www.w3.org/2000/xmlns/")) {
                            String attrPrefix = attr.getPrefix();
                            String value = attr.getNodeValue();
                            // at this point we are dealing with DOM Level 2 nodes only
                            if (specifiedPrefix == null &&
                                attr.getNodeName().equals("xmlns")) {
                                // default namespace
                                return value.length() > 0 ? value : null;
                            } else if (attrPrefix !=null &&
                                       attrPrefix.equals("xmlns") &&
                                       attr.getLocalName().equals(specifiedPrefix)) {
                                // non default namespace
                                return value.length() > 0 ? value : null;
                            }
                        }
                    }
                }
                NodeImpl ancestor = (NodeImpl)getElementAncestor(this);
                if (ancestor != null) {
                    return ancestor.lookupNamespaceURI(specifiedPrefix);
                }

                return null;


            }
        case Node.DOCUMENT_NODE : {
                Element docElement = ((Document)this).getDocumentElement();
                if (docElement != null) {
                    return docElement.lookupNamespaceURI(specifiedPrefix);
                }
                return null;
            }
        case Node.ENTITY_NODE :
        case Node.NOTATION_NODE:
        case Node.DOCUMENT_FRAGMENT_NODE:
        case Node.DOCUMENT_TYPE_NODE:
            // type is unknown
            return null;
        case Node.ATTRIBUTE_NODE:{
                if (this.ownerNode.getNodeType() == Node.ELEMENT_NODE) {
                    return ownerNode.lookupNamespaceURI(specifiedPrefix);

                }
                return null;
            }
        default:{
                NodeImpl ancestor = (NodeImpl)getElementAncestor(this);
                if (ancestor != null) {
                    return ancestor.lookupNamespaceURI(specifiedPrefix);
                }
                return null;
            }

        }
    }

    Node getElementAncestor(Node currentNode) {
        Node parent = currentNode.getParentNode();
        while (parent != null) {
            short type = parent.getNodeType();
            if (type == Node.ELEMENT_NODE) {
                return parent;
            }
            parent = parent.getParentNode();
        }
        return null;
    }

    String lookupNamespacePrefix(String namespaceURI, ElementImpl el){
        String namespace = this.getNamespaceURI();
        // REVISIT: if no prefix is available is it null or empty string, or
        //          could be both?
        String prefix = this.getPrefix();

        if (namespace!=null && namespace.equals(namespaceURI)) {
            if (prefix != null) {
                String foundNamespace =  el.lookupNamespaceURI(prefix);
                if (foundNamespace !=null && foundNamespace.equals(namespaceURI)) {
                    return prefix;
                }

            }
        }
        if (this.hasAttributes()) {
            NamedNodeMap map = this.getAttributes();
            int length = map.getLength();
            for (int i=0;i<length;i++) {
                Node attr = map.item(i);
                namespace = attr.getNamespaceURI();
                if (namespace !=null && namespace.equals("http://www.w3.org/2000/xmlns/")) {
                    String attrPrefix = attr.getPrefix();
                    String value = attr.getNodeValue();
                    // DOM Level 2 nodes
                    if (((attr.getNodeName().equals("xmlns")) ||
                         (attrPrefix !=null && attrPrefix.equals("xmlns")) &&
                         value.equals(namespaceURI))) {

                        String localname= attr.getLocalName();
                        String foundNamespace = el.lookupNamespaceURI(localname);
                        if (foundNamespace !=null && foundNamespace.equals(namespaceURI)) {
                            return localname;
                        }
                    }


                }
            }
        }
        NodeImpl ancestor = (NodeImpl)getElementAncestor(this);

        if (ancestor != null) {
            return ancestor.lookupNamespacePrefix(namespaceURI, el);
        }
        return null;
    }

    /**
     * Tests whether two nodes are equal.
     * <br>This method tests for equality of nodes, not sameness (i.e.,
     * whether the two nodes are references to the same object) which can be
     * tested with <code>Node.isSameNode</code>. All nodes that are the same
     * will also be equal, though the reverse may not be true.
     * <br>Two nodes are equal if and only if the following conditions are
     * satisfied: The two nodes are of the same type.The following string
     * attributes are equal: <code>nodeName</code>, <code>localName</code>,
     * <code>namespaceURI</code>, <code>prefix</code>, <code>nodeValue</code>
     * , <code>baseURI</code>. This is: they are both <code>null</code>, or
     * they have the same length and are character for character identical.
     * The <code>attributes</code> <code>NamedNodeMaps</code> are equal.
     * This is: they are both <code>null</code>, or they have the same
     * length and for each node that exists in one map there is a node that
     * exists in the other map and is equal, although not necessarily at the
     * same index.The <code>childNodes</code> <code>NodeLists</code> are
     * equal. This is: they are both <code>null</code>, or they have the
     * same length and contain equal nodes at the same index. This is true
     * for <code>Attr</code> nodes as for any other type of node. Note that
     * normalization can affect equality; to avoid this, nodes should be
     * normalized before being compared.
     * <br>For two <code>DocumentType</code> nodes to be equal, the following
     * conditions must also be satisfied: The following string attributes
     * are equal: <code>publicId</code>, <code>systemId</code>,
     * <code>internalSubset</code>.The <code>entities</code>
     * <code>NamedNodeMaps</code> are equal.The <code>notations</code>
     * <code>NamedNodeMaps</code> are equal.
     * <br>On the other hand, the following do not affect equality: the
     * <code>ownerDocument</code> attribute, the <code>specified</code>
     * attribute for <code>Attr</code> nodes, the
     * <code>isWhitespaceInElementContent</code> attribute for
     * <code>Text</code> nodes, as well as any user data or event listeners
     * registered on the nodes.
     * @param arg The node to compare equality with.
     * @return If the nodes, and possibly subtrees are equal,
     *   <code>true</code> otherwise <code>false</code>.
     * @since DOM Level 3
     */
    public boolean isEqualNode(Node arg) {
        if (arg == this) {
            return true;
        }
        if (arg.getNodeType() != getNodeType()) {
            return false;
        }
        // in theory nodeName can't be null but better be careful
        // who knows what other implementations may be doing?...
        if (getNodeName() == null) {
            if (arg.getNodeName() != null) {
                return false;
            }
        }
        else if (!getNodeName().equals(arg.getNodeName())) {
            return false;
        }

        if (getLocalName() == null) {
            if (arg.getLocalName() != null) {
                return false;
            }
        }
        else if (!getLocalName().equals(arg.getLocalName())) {
            return false;
        }

        if (getNamespaceURI() == null) {
            if (arg.getNamespaceURI() != null) {
                return false;
            }
        }
        else if (!getNamespaceURI().equals(arg.getNamespaceURI())) {
            return false;
        }

        if (getPrefix() == null) {
            if (arg.getPrefix() != null) {
                return false;
            }
        }
        else if (!getPrefix().equals(arg.getPrefix())) {
            return false;
        }

        if (getNodeValue() == null) {
            if (arg.getNodeValue() != null) {
                return false;
            }
        }
        else if (!getNodeValue().equals(arg.getNodeValue())) {
            return false;
        }


        return true;
    }

    /**
     * @since DOM Level 3
     */
    public Object getFeature(String feature, String version) {
        // we don't have any alternate node, either this node does the job
        // or we don't have anything that does
        return isSupported(feature, version) ? this : null;
    }

    /**
     * Associate an object to a key on this node. The object can later be
     * retrieved from this node by calling <code>getUserData</code> with the
     * same key.
     * @param key The key to associate the object to.
     * @param data The object to associate to the given key, or
     *   <code>null</code> to remove any existing association to that key.
     * @param handler The handler to associate to that key, or
     *   <code>null</code>.
     * @return Returns the <code>DOMObject</code> previously associated to
     *   the given key on this node, or <code>null</code> if there was none.
     * @since DOM Level 3
     */
    public Object setUserData(String key,
                              Object data,
                              UserDataHandler handler) {
        return ownerDocument().setUserData(this, key, data, handler);
    }

    /**
     * Retrieves the object associated to a key on a this node. The object
     * must first have been set to this node by calling
     * <code>setUserData</code> with the same key.
     * @param key The key the object is associated to.
     * @return Returns the <code>DOMObject</code> associated to the given key
     *   on this node, or <code>null</code> if there was none.
     * @since DOM Level 3
     */
    public Object getUserData(String key) {
        return ownerDocument().getUserData(this, key);
    }

    protected Map<String, ParentNode.UserDataRecord> getUserDataRecord(){
        return ownerDocument().getUserDataRecord(this);
        }

    //
    // Public methods
    //

    /**
     * NON-DOM: PR-DOM-Level-1-19980818 mentions readonly nodes in conjunction
     * with Entities, but provides no API to support this.
     * <P>
     * Most DOM users should not touch this method. Its anticpated use
     * is during construction of EntityRefernces, where it will be used to
     * lock the contents replicated from Entity so they can't be casually
     * altered. It _could_ be published as a DOM extension, if desired.
     * <P>
     * Note: since we never have any children deep is meaningless here,
     * ParentNode overrides this behavior.
     * @see ParentNode
     *
     * @param readOnly True or false as desired.
     * @param deep If true, children are also toggled. Note that this will
     *  not change the state of an EntityReference or its children,
     *  which are always read-only.
     */
    public void setReadOnly(boolean readOnly, boolean deep) {

        if (needsSyncData()) {
            synchronizeData();
        }
        isReadOnly(readOnly);

    } // setReadOnly(boolean,boolean)

    /**
     * NON-DOM: Returns true if this node is read-only. This is a
     * shallow check.
     */
    public boolean getReadOnly() {

        if (needsSyncData()) {
            synchronizeData();
        }
        return isReadOnly();

    } // getReadOnly():boolean

    /**
     * NON-DOM: As an alternative to subclassing the DOM, this implementation
     * has been extended with the ability to attach an object to each node.
     * (If you need multiple objects, you can attach a collection such as a
     * List or Map, then attach your application information to that.)
     * <p><b>Important Note:</b> You are responsible for removing references
     * to your data on nodes that are no longer used. Failure to do so will
     * prevent the nodes, your data is attached to, to be garbage collected
     * until the whole document is.
     *
     * @param data the object to store or null to remove any existing reference
     */
    public void setUserData(Object data) {
        ownerDocument().setUserData(this, data);
    }

    /**
     * NON-DOM:
     * Returns the user data associated to this node.
     */
    public Object getUserData() {
        return ownerDocument().getUserData(this);
    }

    //
    // Protected methods
    //

    /**
     * Denotes that this node has changed.
     */
    protected void changed() {
        // we do not actually store this information on every node, we only
        // have a global indicator on the Document. Doing otherwise cost us too
        // much for little gain.
        ownerDocument().changed();
    }

    /**
     * Returns the number of changes to this node.
     */
    protected int changes() {
        // we do not actually store this information on every node, we only
        // have a global indicator on the Document. Doing otherwise cost us too
        // much for little gain.
        return ownerDocument().changes();
    }

    /**
     * Override this method in subclass to hook in efficient
     * internal data structure.
     */
    protected void synchronizeData() {
        // By default just change the flag to avoid calling this method again
        needsSyncData(false);
    }

    /**
     * For non-child nodes, the node which "points" to this node.
     * For example, the owning element for an attribute
     */
    protected Node getContainer() {
       return null;
    }


    /*
     * Flags setters and getters
     */

    final boolean isReadOnly() {
        return (flags & READONLY) != 0;
    }

    final void isReadOnly(boolean value) {
        flags = (short) (value ? flags | READONLY : flags & ~READONLY);
    }

    final boolean needsSyncData() {
        return (flags & SYNCDATA) != 0;
    }

    final void needsSyncData(boolean value) {
        flags = (short) (value ? flags | SYNCDATA : flags & ~SYNCDATA);
    }

    final boolean needsSyncChildren() {
        return (flags & SYNCCHILDREN) != 0;
    }

    public final void needsSyncChildren(boolean value) {
        flags = (short) (value ? flags | SYNCCHILDREN : flags & ~SYNCCHILDREN);
    }

    final boolean isOwned() {
        return (flags & OWNED) != 0;
    }

    final void isOwned(boolean value) {
        flags = (short) (value ? flags | OWNED : flags & ~OWNED);
    }

    final boolean isFirstChild() {
        return (flags & FIRSTCHILD) != 0;
    }

    final void isFirstChild(boolean value) {
        flags = (short) (value ? flags | FIRSTCHILD : flags & ~FIRSTCHILD);
    }

    final boolean isSpecified() {
        return (flags & SPECIFIED) != 0;
    }

    final void isSpecified(boolean value) {
        flags = (short) (value ? flags | SPECIFIED : flags & ~SPECIFIED);
    }

    // inconsistent name to avoid clash with public method on TextImpl
    final boolean internalIsIgnorableWhitespace() {
        return (flags & IGNORABLEWS) != 0;
    }

    final void isIgnorableWhitespace(boolean value) {
        flags = (short) (value ? flags | IGNORABLEWS : flags & ~IGNORABLEWS);
    }

    final boolean hasStringValue() {
        return (flags & HASSTRING) != 0;
    }

    final void hasStringValue(boolean value) {
        flags = (short) (value ? flags | HASSTRING : flags & ~HASSTRING);
    }

    final boolean isNormalized() {
        return (flags & NORMALIZED) != 0;
    }

    final void isNormalized(boolean value) {
        // See if flag should propagate to parent.
        if (!value && isNormalized() && ownerNode != null) {
            ownerNode.isNormalized(false);
        }
        flags = (short) (value ?  flags | NORMALIZED : flags & ~NORMALIZED);
    }

    final boolean isIdAttribute() {
        return (flags & ID) != 0;
    }

    final void isIdAttribute(boolean value) {
        flags = (short) (value ? flags | ID : flags & ~ID);
    }

    //
    // Object methods
    //

    /** NON-DOM method for debugging convenience. */
    public String toString() {
        return "["+getNodeName()+": "+getNodeValue()+"]";
    }

    //
    // Serialization methods
    //

    /** Serialize object. */
    private void writeObject(ObjectOutputStream out) throws IOException {

        // synchronize data
        if (needsSyncData()) {
            synchronizeData();
        }
        // write object
        out.defaultWriteObject();

    } // writeObject(ObjectOutputStream)

} // class NodeImpl
