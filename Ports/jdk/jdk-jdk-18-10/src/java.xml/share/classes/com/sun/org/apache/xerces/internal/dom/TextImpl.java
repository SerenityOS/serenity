/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Text nodes hold the non-markup, non-Entity content of
 * an Element or Attribute.
 * <P>
 * When a document is first made available to the DOM, there is only
 * one Text object for each block of adjacent plain-text. Users (ie,
 * applications) may create multiple adjacent Texts during editing --
 * see {@link org.w3c.dom.Element#normalize} for discussion.
 * <P>
 * Note that CDATASection is a subclass of Text. This is conceptually
 * valid, since they're really just two different ways of quoting
 * characters when they're written out as part of an XML stream.
 *
 * @xerces.internal
 *
 * @since  PR-DOM-Level-1-19980818.
 * @LastModified: Apr 2019
 */
public class TextImpl
    extends CharacterDataImpl
    implements CharacterData, Text {

    //
    // Private Data members
    //


    //
    // Constants
    //

    /** Serialization version. */
    static final long serialVersionUID = -5294980852957403469L;

    //
    // Constructors
    //

    /** Default constructor */
    public TextImpl(){}

    /** Factory constructor. */
    public TextImpl(CoreDocumentImpl ownerDoc, String data) {
        super(ownerDoc, data);
    }

    /**
     * NON-DOM: resets node and sets specified values for the current node
     *
     * @param ownerDoc
     * @param data
     */
    public void setValues(CoreDocumentImpl ownerDoc, String data){

        flags=0;
        nextSibling = null;
        previousSibling=null;
        setOwnerDocument(ownerDoc);
        super.data = data;
    }
    //
    // Node methods
    //

    /**
     * A short integer indicating what type of node this is. The named
     * constants for this value are defined in the org.w3c.dom.Node interface.
     */
    public short getNodeType() {
        return Node.TEXT_NODE;
    }

    /** Returns the node name. */
    public String getNodeName() {
        return "#text";
    }

    /**
     * NON-DOM: Set whether this Text is ignorable whitespace.
     */
    public void setIgnorableWhitespace(boolean ignore) {

        if (needsSyncData()) {
            synchronizeData();
        }
        isIgnorableWhitespace(ignore);

    } // setIgnorableWhitespace(boolean)


    /**
     * DOM L3 Core CR - Experimental
     *
     * Returns whether this text node contains
     * element content whitespace</a>, often abusively called "ignorable whitespace".
     * The text node is determined to contain whitespace in element content
     * during the load of the document or if validation occurs while using
     * <code>Document.normalizeDocument()</code>.
     * @since DOM Level 3
     */
    public boolean isElementContentWhitespace() {
        // REVISIT: is this implemenation correct?
        if (needsSyncData()) {
            synchronizeData();
        }
        return internalIsIgnorableWhitespace();
    }


    /**
     * DOM Level 3 WD - Experimental.
     * Returns all text of <code>Text</code> nodes logically-adjacent text
     * nodes to this node, concatenated in document order.
     * @since DOM Level 3
     */
    public String getWholeText(){

        if (needsSyncData()) {
            synchronizeData();
        }

        StringBuilder buffer = new StringBuilder();
        if (data != null && data.length() != 0) {
            buffer.append(data);
        }

        // concatenate text of logically adjacent text nodes to the left of this node in the tree
        getWholeTextBackward(this.getPreviousSibling(), buffer, this.getParentNode());
        String temp = buffer.toString();

        // clear buffer
        buffer.setLength(0);

        // concatenate text of logically adjacent text nodes to the right of this node in the tree
        getWholeTextForward(this.getNextSibling(), buffer, this.getParentNode());

        return temp + buffer.toString();

    }

    /**
     * internal method taking a StringBuilder in parameter and inserts the
     * text content at the start of the buffer
     *
     * @param buf
     */
    protected void insertTextContent(StringBuilder buf) throws DOMException {
         String content = getNodeValue();
         if (content != null) {
             buf.insert(0, content);
         }
     }

    /**
     * Concatenates the text of all logically-adjacent text nodes to the
     * right of this node
     * @param node
     * @param buffer
     * @param parent
     * @return true - if execution was stopped because the type of node
     *         other than EntityRef, Text, CDATA is encountered, otherwise
     *         return false
     */
    private boolean getWholeTextForward(Node node, StringBuilder buffer, Node parent){
        // boolean to indicate whether node is a child of an entity reference
        boolean inEntRef = false;

        if (parent!=null) {
                inEntRef = parent.getNodeType()==Node.ENTITY_REFERENCE_NODE;
        }

        while (node != null) {
            short type = node.getNodeType();
            if (type == Node.ENTITY_REFERENCE_NODE) {
                if (getWholeTextForward(node.getFirstChild(), buffer, node)){
                    return true;
                }
            }
            else if (type == Node.TEXT_NODE ||
                     type == Node.CDATA_SECTION_NODE) {
                ((NodeImpl)node).getTextContent(buffer);
            }
            else {
                return true;
            }

            node = node.getNextSibling();
        }

        // if the parent node is an entity reference node, must
        // check nodes to the right of the parent entity reference node for logically adjacent
        // text nodes
        if (inEntRef) {
            getWholeTextForward(parent.getNextSibling(), buffer, parent.getParentNode());
                        return true;
        }

        return false;
    }

    /**
     * Concatenates the text of all logically-adjacent text nodes to the left of
     * the node
     * @param node
     * @param buffer
     * @param parent
     * @return true - if execution was stopped because the type of node
     *         other than EntityRef, Text, CDATA is encountered, otherwise
     *         return false
     */
    private boolean getWholeTextBackward(Node node, StringBuilder buffer, Node parent){

        // boolean to indicate whether node is a child of an entity reference
        boolean inEntRef = false;
        if (parent!=null) {
                inEntRef = parent.getNodeType()==Node.ENTITY_REFERENCE_NODE;
        }

        while (node != null) {
            short type = node.getNodeType();
            if (type == Node.ENTITY_REFERENCE_NODE) {
                if (getWholeTextBackward(node.getLastChild(), buffer, node)){
                    return true;
                }
            }
            else if (type == Node.TEXT_NODE ||
                     type == Node.CDATA_SECTION_NODE) {
                ((TextImpl)node).insertTextContent(buffer);
            }
            else {
                return true;
            }

            node = node.getPreviousSibling();
        }

        // if the parent node is an entity reference node, must
        // check nodes to the left of the parent entity reference node for logically adjacent
        // text nodes
        if (inEntRef) {
                getWholeTextBackward(parent.getPreviousSibling(), buffer, parent.getParentNode());
            return true;
        }

        return false;
    }

    /**
     * Replaces the text of the current node and all logically-adjacent text
     * nodes with the specified text. All logically-adjacent text nodes are
     * removed including the current node unless it was the recipient of the
     * replacement text.
     *
     * @param content
     *            The content of the replacing Text node.
     * @return text - The Text node created with the specified content.
     * @since DOM Level 3
     */
    public Text replaceWholeText(String content) throws DOMException {

        if (needsSyncData()) {
            synchronizeData();
        }

        //if the content is null
        Node parent = this.getParentNode();
        if (content == null || content.length() == 0) {
            // remove current node
            if (parent != null) { // check if node in the tree
                parent.removeChild(this);
            }
            return null;
        }

        // make sure we can make the replacement
        if (ownerDocument().errorChecking) {
            if (!canModifyPrev(this)) {
                throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR,
                        DOMMessageFormatter.formatMessage(
                                DOMMessageFormatter.DOM_DOMAIN,
                                "NO_MODIFICATION_ALLOWED_ERR", null));
            }

            // make sure we can make the replacement
            if (!canModifyNext(this)) {
                throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR,
                        DOMMessageFormatter.formatMessage(
                                DOMMessageFormatter.DOM_DOMAIN,
                                "NO_MODIFICATION_ALLOWED_ERR", null));
            }
        }

        //replace the text node
        Text currentNode = null;
        if (isReadOnly()) {
            Text newNode = this.ownerDocument().createTextNode(content);
            if (parent != null) { // check if node in the tree
                parent.insertBefore(newNode, this);
                parent.removeChild(this);
                currentNode = newNode;
            } else {
                return newNode;
            }
        } else {
            this.setData(content);
            currentNode = this;
        }

        //check logically-adjacent text nodes
        Node prev = currentNode.getPreviousSibling();
        while (prev != null) {
            //If the logically-adjacent next node can be removed
            //remove it. A logically adjacent node can be removed if
            //it is a Text or CDATASection node or an EntityReference with
            //Text and CDATA only children.
            if ((prev.getNodeType() == Node.TEXT_NODE)
                    || (prev.getNodeType() == Node.CDATA_SECTION_NODE)
                    || (prev.getNodeType() == Node.ENTITY_REFERENCE_NODE && hasTextOnlyChildren(prev))) {
                parent.removeChild(prev);
                prev = currentNode;
            } else {
                break;
            }
            prev = prev.getPreviousSibling();
        }

        //check logically-adjacent text nodes
        Node next = currentNode.getNextSibling();
        while (next != null) {
            //If the logically-adjacent next node can be removed
            //remove it. A logically adjacent node can be removed if
            //it is a Text or CDATASection node or an EntityReference with
            //Text and CDATA only children.
            if ((next.getNodeType() == Node.TEXT_NODE)
                    || (next.getNodeType() == Node.CDATA_SECTION_NODE)
                    || (next.getNodeType() == Node.ENTITY_REFERENCE_NODE && hasTextOnlyChildren(next))) {
                parent.removeChild(next);
                next = currentNode;
            } else {
                break;
            }
            next = next.getNextSibling();
        }

        return currentNode;
    }

    /**
     * If any EntityReference to be removed has descendants that are not
     * EntityReference, Text, or CDATASection nodes, the replaceWholeText method
     * must fail before performing any modification of the document, raising a
     * DOMException with the code NO_MODIFICATION_ALLOWED_ERR. Traverse previous
     * siblings of the node to be replaced. If a previous sibling is an
     * EntityReference node, get it's last child. If the last child was a Text
     * or CDATASection node and its previous siblings are neither a replaceable
     * EntityReference or Text or CDATASection nodes, return false. IF the last
     * child was neither Text nor CDATASection nor a replaceable EntityReference
     * Node, then return true. If the last child was a Text or CDATASection node
     * any its previous sibling was not or was an EntityReference that did not
     * contain only Text or CDATASection nodes, return false. Check this
     * recursively for EntityReference nodes.
     *
     * @param node
     * @return true - can replace text false - can't replace exception must be
     *         raised
     */
    private boolean canModifyPrev(Node node) {
        boolean textLastChild = false;

        Node prev = node.getPreviousSibling();

        while (prev != null) {

            short type = prev.getNodeType();

            if (type == Node.ENTITY_REFERENCE_NODE) {
                //If the previous sibling was entityreference
                //check if its content is replaceable
                Node lastChild = prev.getLastChild();

                //if the entity reference has no children
                //return false
                if (lastChild == null) {
                    return false;
                }

                //The replacement text of the entity reference should
                //be either only text,cadatsections or replaceable entity
                //reference nodes or the last child should be neither of these
                while (lastChild != null) {
                    short lType = lastChild.getNodeType();

                    if (lType == Node.TEXT_NODE
                            || lType == Node.CDATA_SECTION_NODE) {
                        textLastChild = true;
                    } else if (lType == Node.ENTITY_REFERENCE_NODE) {
                        if (!canModifyPrev(lastChild)) {
                            return false;
                        } else {
                            //If the EntityReference child contains
                            //only text, or non-text or ends with a
                            //non-text node.
                            textLastChild = true;
                        }
                    } else {
                        //If the last child was replaceable and others are not
                        //Text or CDataSection or replaceable EntityRef nodes
                        //return false.
                        if (textLastChild) {
                            return false;
                        } else {
                            return true;
                        }
                    }
                    lastChild = lastChild.getPreviousSibling();
                }
            } else if (type == Node.TEXT_NODE
                    || type == Node.CDATA_SECTION_NODE) {
                //If the previous sibling was text or cdatasection move to next
            } else {
                //If the previous sibling was anything but text or
                //cdatasection or an entity reference, stop search and
                //return true
                return true;
            }

            prev = prev.getPreviousSibling();
        }

        return true;
    }

    /**
     * If any EntityReference to be removed has descendants that are not
     * EntityReference, Text, or CDATASection nodes, the replaceWholeText method
     * must fail before performing any modification of the document, raising a
     * DOMException with the code NO_MODIFICATION_ALLOWED_ERR. Traverse previous
     * siblings of the node to be replaced. If a previous sibling is an
     * EntityReference node, get it's last child. If the first child was a Text
     * or CDATASection node and its next siblings are neither a replaceable
     * EntityReference or Text or CDATASection nodes, return false. IF the first
     * child was neither Text nor CDATASection nor a replaceable EntityReference
     * Node, then return true. If the first child was a Text or CDATASection
     * node any its next sibling was not or was an EntityReference that did not
     * contain only Text or CDATASection nodes, return false. Check this
     * recursively for EntityReference nodes.
     *
     * @param node
     * @return true - can replace text false - can't replace exception must be
     *         raised
     */
    private boolean canModifyNext(Node node) {
        boolean textFirstChild = false;

        Node next = node.getNextSibling();
        while (next != null) {

            short type = next.getNodeType();

            if (type == Node.ENTITY_REFERENCE_NODE) {
                //If the previous sibling was entityreference
                //check if its content is replaceable
                Node firstChild = next.getFirstChild();

                //if the entity reference has no children
                //return false
                if (firstChild == null) {
                    return false;
                }

                //The replacement text of the entity reference should
                //be either only text,cadatsections or replaceable entity
                //reference nodes or the last child should be neither of these
                while (firstChild != null) {
                    short lType = firstChild.getNodeType();

                    if (lType == Node.TEXT_NODE
                            || lType == Node.CDATA_SECTION_NODE) {
                        textFirstChild = true;
                    } else if (lType == Node.ENTITY_REFERENCE_NODE) {
                        if (!canModifyNext(firstChild)) {
                            return false;
                        } else {
                            //If the EntityReference child contains
                            //only text, or non-text or ends with a
                            //non-text node.
                            textFirstChild = true;
                        }
                    } else {
                        //If the first child was replaceable text and next
                        //children are not, then return false
                        if (textFirstChild) {
                            return false;
                        } else {
                            return true;
                        }
                    }
                    firstChild = firstChild.getNextSibling();
                }
            } else if (type == Node.TEXT_NODE
                    || type == Node.CDATA_SECTION_NODE) {
                //If the previous sibling was text or cdatasection move to next
            } else {
                //If the next sibling was anything but text or
                //cdatasection or an entity reference, stop search and
                //return true
                return true;
            }

            next = next.getNextSibling();
        }

        return true;
    }

    /**
     * Check if an EntityReference node has Text Only child nodes
     *
     * @param node
     * @return true - Contains text only children
     */
    private boolean hasTextOnlyChildren(Node node) {

        Node child = node;

        if (child == null) {
            return false;
        }

        child = child.getFirstChild();
        while (child != null) {
            int type = child.getNodeType();

            if (type == Node.ENTITY_REFERENCE_NODE) {
                return hasTextOnlyChildren(child);
            }
            else if (type != Node.TEXT_NODE
                    && type != Node.CDATA_SECTION_NODE
                    && type != Node.ENTITY_REFERENCE_NODE) {
                return false;
            }
            child = child.getNextSibling();
        }
        return true;
    }


    /**
     * NON-DOM: Returns whether this Text is ignorable whitespace.
     */
    public boolean isIgnorableWhitespace() {

        if (needsSyncData()) {
            synchronizeData();
        }
        return internalIsIgnorableWhitespace();

    } // isIgnorableWhitespace():boolean


    //
    // Text methods
    //

    /**
     * Break a text node into two sibling nodes. (Note that if the current node
     * has no parent, they won't wind up as "siblings" -- they'll both be
     * orphans.)
     *
     * @param offset
     *            The offset at which to split. If offset is at the end of the
     *            available data, the second node will be empty.
     *
     * @return A reference to the new node (containing data after the offset
     *         point). The original node will contain data up to that point.
     *
     * @throws DOMException(INDEX_SIZE_ERR)
     *             if offset is <0 or >length.
     *
     * @throws DOMException(NO_MODIFICATION_ALLOWED_ERR)
     *             if node is read-only.
     */
    public Text splitText(int offset)
        throws DOMException {

        if (isReadOnly()) {
            throw new DOMException(
            DOMException.NO_MODIFICATION_ALLOWED_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null));
        }

        if (needsSyncData()) {
            synchronizeData();
        }
        if (offset < 0 || offset > data.length() ) {
            throw new DOMException(DOMException.INDEX_SIZE_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INDEX_SIZE_ERR", null));
        }

        // split text into two separate nodes
        Text newText =
            getOwnerDocument().createTextNode(data.substring(offset));
        setNodeValue(data.substring(0, offset));

        // insert new text node
        Node parentNode = getParentNode();
        if (parentNode != null) {
            parentNode.insertBefore(newText, nextSibling);
        }

        return newText;

    } // splitText(int):Text


    /**
     * NON-DOM (used by DOMParser): Reset data for the node.
     */
    public void replaceData (String value){
        data = value;
    }


    /**
     * NON-DOM (used by DOMParser: Sets data to empty string.
     *  Returns the value the data was set to.
     */
    public String removeData (){
        String olddata=data;
        data = "";
        return olddata;
    }

} // class TextImpl
