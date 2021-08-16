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

import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

/**
 * ParentNode inherits from ChildNode and adds the capability of having child
 * nodes. Not every node in the DOM can have children, so only nodes that can
 * should inherit from this class and pay the price for it.
 * <P>
 * ParentNode, just like NodeImpl, also implements NodeList, so it can
 * return itself in response to the getChildNodes() query. This eliminiates
 * the need for a separate ChildNodeList object. Note that this is an
 * IMPLEMENTATION DETAIL; applications should _never_ assume that
 * this identity exists. On the other hand, subclasses may need to override
 * this, in case of conflicting names. This is the case for the classes
 * HTMLSelectElementImpl and HTMLFormElementImpl of the HTML DOM.
 * <P>
 * While we have a direct reference to the first child, the last child is
 * stored as the previous sibling of the first child. First child nodes are
 * marked as being so, and getNextSibling hides this fact.
 * <P>Note: Not all parent nodes actually need to also be a child. At some
 * point we used to have ParentNode inheriting from NodeImpl and another class
 * called ChildAndParentNode that inherited from ChildNode. But due to the lack
 * of multiple inheritance a lot of code had to be duplicated which led to a
 * maintenance nightmare. At the same time only a few nodes (Document,
 * DocumentFragment, Entity, and Attribute) cannot be a child so the gain in
 * memory wasn't really worth it. The only type for which this would be the
 * case is Attribute, but we deal with there in another special way, so this is
 * not applicable.
 * <p>
 * This class doesn't directly support mutation events, however, it notifies
 * the document when mutations are performed so that the document class do so.
 *
 * <p><b>WARNING</b>: Some of the code here is partially duplicated in
 * AttrImpl, be careful to keep these two classes in sync!
 *
 * @xerces.internal
 *
 * @author Arnaud  Le Hors, IBM
 * @author Joe Kesselman, IBM
 * @author Andy Clark, IBM
 * @LastModified: Apr 2019
 */
public abstract class ParentNode
    extends ChildNode {

    /** Serialization version. */
    static final long serialVersionUID = 2815829867152120872L;

    /** Owner document. */
    protected CoreDocumentImpl ownerDocument;

    /** First child. */
    protected ChildNode firstChild = null;

    // transients

    /** NodeList cache */
    protected transient NodeListCache fNodeListCache = null;

    //
    // Constructors
    //

    /**
     * No public constructor; only subclasses of ParentNode should be
     * instantiated, and those normally via a Document's factory methods
     */
    protected ParentNode(CoreDocumentImpl ownerDocument) {
        super(ownerDocument);
        this.ownerDocument = ownerDocument;
    }

    /** Constructor for serialization. */
    public ParentNode() {}

    //
    // NodeList methods
    //

    /**
     * Returns a duplicate of a given node. You can consider this a
     * generic "copy constructor" for nodes. The newly returned object should
     * be completely independent of the source object's subtree, so changes
     * in one after the clone has been made will not affect the other.
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

        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        ParentNode newnode = (ParentNode) super.cloneNode(deep);

        // set owner document
        newnode.ownerDocument = ownerDocument;

        // Need to break the association w/ original kids
        newnode.firstChild      = null;

        // invalidate cache for children NodeList
        newnode.fNodeListCache = null;

        // Then, if deep, clone the kids too.
        if (deep) {
            for (ChildNode child = firstChild;
                 child != null;
                 child = child.nextSibling) {
                newnode.appendChild(child.cloneNode(true));
            }
        }

        return newnode;

    } // cloneNode(boolean):Node

    /**
     * Find the Document that this Node belongs to (the document in
     * whose context the Node was created). The Node may or may not
     * currently be part of that Document's actual contents.
     */
    public Document getOwnerDocument() {
        return ownerDocument;
    }

    /**
     * same as above but returns internal type and this one is not overridden
     * by CoreDocumentImpl to return null
     */
    CoreDocumentImpl ownerDocument() {
        return ownerDocument;
    }

    /**
     * NON-DOM
     * set the ownerDocument of this node and its children
     */
    protected void setOwnerDocument(CoreDocumentImpl doc) {
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        super.setOwnerDocument(doc);
        ownerDocument = doc;
        for (ChildNode child = firstChild;
        child != null; child = child.nextSibling) {
            child.setOwnerDocument(doc);
        }
    }

    /**
     * Test whether this node has any children. Convenience shorthand
     * for (Node.getFirstChild()!=null)
     */
    public boolean hasChildNodes() {
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        return firstChild != null;
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

        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        return this;

    } // getChildNodes():NodeList

    /** The first child of this Node, or null if none. */
    public Node getFirstChild() {

        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        return firstChild;

    }   // getFirstChild():Node

    /** The last child of this Node, or null if none. */
    public Node getLastChild() {

        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        return lastChild();

    } // getLastChild():Node

    final ChildNode lastChild() {
        // last child is stored as the previous sibling of first child
        return firstChild != null ? firstChild.previousSibling : null;
    }

    final void lastChild(ChildNode node) {
        // store lastChild as previous sibling of first child
        if (firstChild != null) {
            firstChild.previousSibling = node;
        }
    }

    /**
     * Move one or more node(s) to our list of children. Note that this
     * implicitly removes them from their previous parent.
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
        // Tail-call; optimizer should be able to do good things with.
        return internalInsertBefore(newChild, refChild, false);
    } // insertBefore(Node,Node):Node

    /** NON-DOM INTERNAL: Within DOM actions,we sometimes need to be able
     * to control which mutation events are spawned. This version of the
     * insertBefore operation allows us to do so. It is not intended
     * for use by application programs.
     */
    Node internalInsertBefore(Node newChild, Node refChild, boolean replace)
        throws DOMException {

        boolean errorChecking = ownerDocument.errorChecking;

        if (newChild.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE) {
            // SLOW BUT SAFE: We could insert the whole subtree without
            // juggling so many next/previous pointers. (Wipe out the
            // parent's child-list, patch the parent pointers, set the
            // ends of the list.) But we know some subclasses have special-
            // case behavior they add to insertBefore(), so we don't risk it.
            // This approch also takes fewer bytecodes.

            // NOTE: If one of the children is not a legal child of this
            // node, throw HIERARCHY_REQUEST_ERR before _any_ of the children
            // have been transferred. (Alternative behaviors would be to
            // reparent up to the first failure point or reparent all those
            // which are acceptable to the target node, neither of which is
            // as robust. PR-DOM-0818 isn't entirely clear on which it
            // recommends?????

            // No need to check kids for right-document; if they weren't,
            // they wouldn't be kids of that DocFrag.
            if (errorChecking) {
                for (Node kid = newChild.getFirstChild(); // Prescan
                     kid != null; kid = kid.getNextSibling()) {

                    if (!ownerDocument.isKidOK(this, kid)) {
                        throw new DOMException(
                              DOMException.HIERARCHY_REQUEST_ERR,
                              DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "HIERARCHY_REQUEST_ERR", null));
                    }
                }
            }

            while (newChild.hasChildNodes()) {
                insertBefore(newChild.getFirstChild(), refChild);
            }
            return newChild;
        }

        if (newChild == refChild) {
            // stupid case that must be handled as a no-op triggering events...
            refChild = refChild.getNextSibling();
            removeChild(newChild);
            insertBefore(newChild, refChild);
            return newChild;
        }

        if (needsSyncChildren()) {
            synchronizeChildren();
        }

        if (errorChecking) {
            if (isReadOnly()) {
                throw new DOMException(
                              DOMException.NO_MODIFICATION_ALLOWED_ERR,
                              DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null));
            }
            if (newChild.getOwnerDocument() != ownerDocument && newChild != ownerDocument) {
                throw new DOMException(DOMException.WRONG_DOCUMENT_ERR,
                            DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
            if (!ownerDocument.isKidOK(this, newChild)) {
                throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,
                            DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "HIERARCHY_REQUEST_ERR", null));
            }
            // refChild must be a child of this node (or null)
            if (refChild != null && refChild.getParentNode() != this) {
                throw new DOMException(DOMException.NOT_FOUND_ERR,
                            DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_FOUND_ERR", null));
            }

            // Prevent cycles in the tree
            // newChild cannot be ancestor of this Node,
            // and actually cannot be this
            boolean treeSafe = true;
            for (NodeImpl a = this; treeSafe && a != null; a = a.parentNode())
            {
                treeSafe = newChild != a;
            }
            if(!treeSafe) {
                throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR,
                            DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "HIERARCHY_REQUEST_ERR", null));
            }
        }

        // notify document
        ownerDocument.insertingNode(this, replace);

        // Convert to internal type, to avoid repeated casting
        ChildNode newInternal = (ChildNode)newChild;

        Node oldparent = newInternal.parentNode();
        if (oldparent != null) {
            oldparent.removeChild(newInternal);
        }

        // Convert to internal type, to avoid repeated casting
        ChildNode refInternal = (ChildNode)refChild;

        // Attach up
        newInternal.ownerNode = this;
        newInternal.isOwned(true);

        // Attach before and after
        // Note: firstChild.previousSibling == lastChild!!
        if (firstChild == null) {
            // this our first and only child
            firstChild = newInternal;
            newInternal.isFirstChild(true);
            newInternal.previousSibling = newInternal;
        }
        else {
            if (refInternal == null) {
                // this is an append
                ChildNode lastChild = firstChild.previousSibling;
                lastChild.nextSibling = newInternal;
                newInternal.previousSibling = lastChild;
                firstChild.previousSibling = newInternal;
            }
            else {
                // this is an insert
                if (refChild == firstChild) {
                    // at the head of the list
                    firstChild.isFirstChild(false);
                    newInternal.nextSibling = firstChild;
                    newInternal.previousSibling = firstChild.previousSibling;
                    firstChild.previousSibling = newInternal;
                    firstChild = newInternal;
                    newInternal.isFirstChild(true);
                }
                else {
                    // somewhere in the middle
                    ChildNode prev = refInternal.previousSibling;
                    newInternal.nextSibling = refInternal;
                    prev.nextSibling = newInternal;
                    refInternal.previousSibling = newInternal;
                    newInternal.previousSibling = prev;
                }
            }
        }

        changed();

        // update cached length if we have any
        if (fNodeListCache != null) {
            if (fNodeListCache.fLength != -1) {
                fNodeListCache.fLength++;
            }
            if (fNodeListCache.fChildIndex != -1) {
                // if we happen to insert just before the cached node, update
                // the cache to the new node to match the cached index
                if (fNodeListCache.fChild == refInternal) {
                    fNodeListCache.fChild = newInternal;
                } else {
                    // otherwise just invalidate the cache
                    fNodeListCache.fChildIndex = -1;
                }
            }
        }

        // notify document
        ownerDocument.insertedNode(this, newInternal, replace);

        checkNormalizationAfterInsert(newInternal);

        return newChild;

    } // internalInsertBefore(Node,Node,boolean):Node

    /**
     * Remove a child from this Node. The removed child's subtree
     * remains intact so it may be re-inserted elsewhere.
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
        // Tail-call, should be optimizable
        return internalRemoveChild(oldChild, false);
    } // removeChild(Node) :Node

    /** NON-DOM INTERNAL: Within DOM actions,we sometimes need to be able
     * to control which mutation events are spawned. This version of the
     * removeChild operation allows us to do so. It is not intended
     * for use by application programs.
     */
    Node internalRemoveChild(Node oldChild, boolean replace)
        throws DOMException {

        CoreDocumentImpl ownerDocument = ownerDocument();
        if (ownerDocument.errorChecking) {
            if (isReadOnly()) {
                throw new DOMException(
                            DOMException.NO_MODIFICATION_ALLOWED_ERR,
                            DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null));
            }
            if (oldChild != null && oldChild.getParentNode() != this) {
                throw new DOMException(DOMException.NOT_FOUND_ERR,
                            DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_FOUND_ERR", null));
            }
        }

        ChildNode oldInternal = (ChildNode) oldChild;

        // notify document
        ownerDocument.removingNode(this, oldInternal, replace);

        // Save previous sibling for normalization checking.
        final ChildNode oldPreviousSibling = oldInternal.previousSibling();

        // update cached length if we have any
        if (fNodeListCache != null) {
            if (fNodeListCache.fLength != -1) {
                fNodeListCache.fLength--;
            }
            if (fNodeListCache.fChildIndex != -1) {
                // if the removed node is the cached node
                // move the cache to its (soon former) previous sibling
                if (fNodeListCache.fChild == oldInternal) {
                    fNodeListCache.fChildIndex--;
                    fNodeListCache.fChild = oldPreviousSibling;
                } else {
                    // otherwise just invalidate the cache
                    fNodeListCache.fChildIndex = -1;
                }
            }
        }

        // Patch linked list around oldChild
        // Note: lastChild == firstChild.previousSibling
        if (oldInternal == firstChild) {
            // removing first child
            oldInternal.isFirstChild(false);
            firstChild = oldInternal.nextSibling;
            if (firstChild != null) {
                firstChild.isFirstChild(true);
                firstChild.previousSibling = oldInternal.previousSibling;
            }
        } else {
            ChildNode prev = oldInternal.previousSibling;
            ChildNode next = oldInternal.nextSibling;
            prev.nextSibling = next;
            if (next == null) {
                // removing last child
                firstChild.previousSibling = prev;
            } else {
                // removing some other child in the middle
                next.previousSibling = prev;
            }
        }

        // Remove oldInternal's references to tree
        oldInternal.ownerNode       = ownerDocument;
        oldInternal.isOwned(false);
        oldInternal.nextSibling     = null;
        oldInternal.previousSibling = null;

        changed();

        // notify document
        ownerDocument.removedNode(this, replace);

        checkNormalizationAfterRemove(oldPreviousSibling);

        return oldInternal;

    } // internalRemoveChild(Node,boolean):Node

    /**
     * Make newChild occupy the location that oldChild used to
     * have. Note that newChild will first be removed from its previous
     * parent, if any. Equivalent to inserting newChild before oldChild,
     * then removing oldChild.
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
        // If Mutation Events are being generated, this operation might
        // throw aggregate events twice when modifying an Attr -- once
        // on insertion and once on removal. DOM Level 2 does not specify
        // this as either desirable or undesirable, but hints that
        // aggregations should be issued only once per user request.

        // notify document
        ownerDocument.replacingNode(this);

        internalInsertBefore(newChild, oldChild, true);
        if (newChild != oldChild) {
            internalRemoveChild(oldChild, true);
        }

        // notify document
        ownerDocument.replacedNode(this);

        return oldChild;
    }

    /*
     * Get Node text content
     * @since DOM Level 3
     */
    public String getTextContent() throws DOMException {
        Node child = getFirstChild();
        if (child != null) {
            Node next = child.getNextSibling();
            if (next == null) {
                return hasTextContent(child) ? ((NodeImpl) child).getTextContent() : "";
            }
            StringBuilder buf = new StringBuilder();
            getTextContent(buf);
            return buf.toString();
        }
        return "";
    }

    // internal method taking a StringBuilder in parameter
    void getTextContent(StringBuilder buf) throws DOMException {
        Node child = getFirstChild();
        while (child != null) {
            if (hasTextContent(child)) {
                ((NodeImpl) child).getTextContent(buf);
            }
            child = child.getNextSibling();
        }
    }

    // internal method returning whether to take the given node's text content
    final boolean hasTextContent(Node child) {
        return child.getNodeType() != Node.COMMENT_NODE &&
            child.getNodeType() != Node.PROCESSING_INSTRUCTION_NODE &&
            (child.getNodeType() != Node.TEXT_NODE ||
             ((TextImpl) child).isIgnorableWhitespace() == false);
    }

    /*
     * Set Node text content
     * @since DOM Level 3
     */
    public void setTextContent(String textContent)
        throws DOMException {
        // get rid of any existing children
        Node child;
        while ((child = getFirstChild()) != null) {
            removeChild(child);
        }
        // create a Text node to hold the given content
        if (textContent != null && textContent.length() != 0){
            appendChild(ownerDocument().createTextNode(textContent));
        }
    }

    //
    // NodeList methods
    //

    /**
     * Count the immediate children of this node.  Use to implement
     * NodeList.getLength().
     * @return int
     */
    private int nodeListGetLength() {

        if (fNodeListCache == null) {
            if (needsSyncChildren()) {
                synchronizeChildren();
            }
            // get rid of trivial cases
            if (firstChild == null) {
                return 0;
            }
            if (firstChild == lastChild()) {
                return 1;
            }
            // otherwise request a cache object
            fNodeListCache = ownerDocument.getNodeListCache(this);
        }
        if (fNodeListCache.fLength == -1) { // is the cached length invalid ?
            int l;
            ChildNode n;
            // start from the cached node if we have one
            if (fNodeListCache.fChildIndex != -1 &&
                fNodeListCache.fChild != null) {
                l = fNodeListCache.fChildIndex;
                n = fNodeListCache.fChild;
            } else {
                n = firstChild;
                l = 0;
            }
            while (n != null) {
                l++;
                n = n.nextSibling;
            }
            fNodeListCache.fLength = l;
        }

        return fNodeListCache.fLength;

    } // nodeListGetLength():int

    /**
     * NodeList method: Count the immediate children of this node
     * @return int
     */
    public int getLength() {
        return nodeListGetLength();
    }

    /**
     * Return the Nth immediate child of this node, or null if the index is
     * out of bounds.  Use to implement NodeList.item().
     * @param index int
     */
    private Node nodeListItem(int index) {

        if (fNodeListCache == null) {
            if (needsSyncChildren()) {
                synchronizeChildren();
            }
            // get rid of trivial case
            if (firstChild == lastChild()) {
                return index == 0 ? firstChild : null;
            }
            // otherwise request a cache object
            fNodeListCache = ownerDocument.getNodeListCache(this);
        }
        int i = fNodeListCache.fChildIndex;
        ChildNode n = fNodeListCache.fChild;
        boolean firstAccess = true;
        // short way
        if (i != -1 && n != null) {
            firstAccess = false;
            if (i < index) {
                while (i < index && n != null) {
                    i++;
                    n = n.nextSibling;
                }
            }
            else if (i > index) {
                while (i > index && n != null) {
                    i--;
                    n = n.previousSibling();
                }
            }
        }
        else {
            // long way
            if (index < 0) {
                return null;
            }
            n = firstChild;
            for (i = 0; i < index && n != null; i++) {
                n = n.nextSibling;
            }
        }

        // release cache if reaching last child or first child
        if (!firstAccess && (n == firstChild || n == lastChild())) {
            fNodeListCache.fChildIndex = -1;
            fNodeListCache.fChild = null;
            ownerDocument.freeNodeListCache(fNodeListCache);
            // we can keep using the cache until it is actually reused
            // fNodeListCache will be nulled by the pool (document) if that
            // happens.
            // fNodeListCache = null;
        }
        else {
            // otherwise update it
            fNodeListCache.fChildIndex = i;
            fNodeListCache.fChild = n;
        }
        return n;

    } // nodeListItem(int):Node

    /**
     * NodeList method: Return the Nth immediate child of this node, or
     * null if the index is out of bounds.
     * @return org.w3c.dom.Node
     * @param index int
     */
    public Node item(int index) {
        return nodeListItem(index);
    } // item(int):Node

    /**
     * Create a NodeList to access children that is use by subclass elements
     * that have methods named getLength() or item(int).  ChildAndParentNode
     * optimizes getChildNodes() by implementing NodeList itself.  However if
     * a subclass Element implements methods with the same name as the NodeList
     * methods, they will override the actually methods in this class.
     * <p>
     * To use this method, the subclass should implement getChildNodes() and
     * have it call this method.  The resulting NodeList instance maybe
     * shared and cached in a transient field, but the cached value must be
     * cleared if the node is cloned.
     */
    protected final NodeList getChildNodesUnoptimized() {
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        return new NodeList() {
                /**
                 * @see NodeList.getLength()
                 */
                public int getLength() {
                    return nodeListGetLength();
                } // getLength():int

                /**
                 * @see NodeList.item(int)
                 */
                public Node item(int index) {
                    return nodeListItem(index);
                } // item(int):Node
            };
    } // getChildNodesUnoptimized():NodeList

    //
    // DOM2: methods, getters, setters
    //

    /**
     * Override default behavior to call normalize() on this Node's
     * children. It is up to implementors or Node to override normalize()
     * to take action.
     */
    public void normalize() {
        // No need to normalize if already normalized.
        if (isNormalized()) {
            return;
        }
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        ChildNode kid;
        for (kid = firstChild; kid != null; kid = kid.nextSibling) {
            kid.normalize();
        }
        isNormalized(true);
    }

    /**
     * DOM Level 3 WD- Experimental.
     * Override inherited behavior from NodeImpl to support deep equal.
     */
    public boolean isEqualNode(Node arg) {
        if (!super.isEqualNode(arg)) {
            return false;
        }
        // there are many ways to do this test, and there isn't any way
        // better than another. Performance may vary greatly depending on
        // the implementations involved. This one should work fine for us.
        Node child1 = getFirstChild();
        Node child2 = arg.getFirstChild();
        while (child1 != null && child2 != null) {
            if (!child1.isEqualNode(child2)) {
                return false;
            }
            child1 = child1.getNextSibling();
            child2 = child2.getNextSibling();
        }
        if (child1 != child2) {
            return false;
        }
        return true;
    }

    //
    // Public methods
    //

    /**
     * Override default behavior so that if deep is true, children are also
     * toggled.
     * @see Node
     * <P>
     * Note: this will not change the state of an EntityReference or its
     * children, which are always read-only.
     */
    public void setReadOnly(boolean readOnly, boolean deep) {

        super.setReadOnly(readOnly, deep);

        if (deep) {

            if (needsSyncChildren()) {
                synchronizeChildren();
            }

            // Recursively set kids
            for (ChildNode mykid = firstChild;
                 mykid != null;
                 mykid = mykid.nextSibling) {
                if (mykid.getNodeType() != Node.ENTITY_REFERENCE_NODE) {
                    mykid.setReadOnly(readOnly,true);
                }
            }
        }
    } // setReadOnly(boolean,boolean)

    //
    // Protected methods
    //

    /**
     * Override this method in subclass to hook in efficient
     * internal data structure.
     */
    protected void synchronizeChildren() {
        // By default just change the flag to avoid calling this method again
        needsSyncChildren(false);
    }

    /**
     * Checks the normalized state of this node after inserting a child.
     * If the inserted child causes this node to be unnormalized, then this
     * node is flagged accordingly.
     * The conditions for changing the normalized state are:
     * <ul>
     * <li>The inserted child is a text node and one of its adjacent siblings
     * is also a text node.
     * <li>The inserted child is is itself unnormalized.
     * </ul>
     *
     * @param insertedChild the child node that was inserted into this node
     *
     * @throws NullPointerException if the inserted child is <code>null</code>
     */
    void checkNormalizationAfterInsert(ChildNode insertedChild) {
        // See if insertion caused this node to be unnormalized.
        if (insertedChild.getNodeType() == Node.TEXT_NODE) {
            ChildNode prev = insertedChild.previousSibling();
            ChildNode next = insertedChild.nextSibling;
            // If an adjacent sibling of the new child is a text node,
            // flag this node as unnormalized.
            if ((prev != null && prev.getNodeType() == Node.TEXT_NODE) ||
                (next != null && next.getNodeType() == Node.TEXT_NODE)) {
                isNormalized(false);
            }
        }
        else {
            // If the new child is not normalized,
            // then this node is inherently not normalized.
            if (!insertedChild.isNormalized()) {
                isNormalized(false);
            }
        }
    } // checkNormalizationAfterInsert(ChildNode)

    /**
     * Checks the normalized of this node after removing a child.
     * If the removed child causes this node to be unnormalized, then this
     * node is flagged accordingly.
     * The conditions for changing the normalized state are:
     * <ul>
     * <li>The removed child had two adjacent siblings that were text nodes.
     * </ul>
     *
     * @param previousSibling the previous sibling of the removed child, or
     * <code>null</code>
     */
    void checkNormalizationAfterRemove(ChildNode previousSibling) {
        // See if removal caused this node to be unnormalized.
        // If the adjacent siblings of the removed child were both text nodes,
        // flag this node as unnormalized.
        if (previousSibling != null &&
            previousSibling.getNodeType() == Node.TEXT_NODE) {

            ChildNode next = previousSibling.nextSibling;
            if (next != null && next.getNodeType() == Node.TEXT_NODE) {
                isNormalized(false);
            }
        }
    } // checkNormalizationAfterRemove(Node)

    //
    // Serialization methods
    //

    /** Serialize object. */
    private void writeObject(ObjectOutputStream out) throws IOException {

        // synchronize children
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        // write object
        out.defaultWriteObject();

    } // writeObject(ObjectOutputStream)

    /** Deserialize object. */
    private void readObject(ObjectInputStream ois)
        throws ClassNotFoundException, IOException {

        // perform default deseralization
        ois.defaultReadObject();

        // hardset synchildren - so we don't try to sync - it does not make any
        // sense to try to synchildren when we just deserialize object.
        needsSyncChildren(false);

    } // readObject(ObjectInputStream)

    /*
     * a class to store some user data along with its handler
     */
    class UserDataRecord implements Serializable {
        /** Serialization version. */
        private static final long serialVersionUID = 3258126977134310455L;

        Object fData;
        UserDataHandler fHandler;
        UserDataRecord(Object data, UserDataHandler handler) {
            fData = data;
            fHandler = handler;
        }
    }
} // class ParentNode
