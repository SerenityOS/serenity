/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * AttributeMap inherits from NamedNodeMapImpl and extends it to deal with the
 * specifics of storing attributes. These are:
 * <ul>
 *  <li>managing ownership of attribute nodes
 *  <li>managing default attributes
 *  <li>firing mutation events
 * </ul>
 * <p>
 * This class doesn't directly support mutation events, however, it notifies
 * the document when mutations are performed so that the document class do so.
 *
 * @xerces.internal
 *
 * @LastModified: Oct 2017
 */
public class AttributeMap extends NamedNodeMapImpl {

    /** Serialization version. */
    static final long serialVersionUID = 8872606282138665383L;

    //
    // Constructors
    //

    /** Constructs a named node map. */
    protected AttributeMap(ElementImpl ownerNode, NamedNodeMapImpl defaults) {
        super(ownerNode);
        if (defaults != null) {
            // initialize map with the defaults
            cloneContent(defaults);
            if (nodes != null) {
                hasDefaults(true);
            }
        }
    }

    /**
     * Adds an attribute using its nodeName attribute.
     * @see org.w3c.dom.NamedNodeMap#setNamedItem
     * @return If the new Node replaces an existing node the replaced Node is
     *      returned, otherwise null is returned.
     * @param arg
     *      An Attr node to store in this map.
     * @exception org.w3c.dom.DOMException The exception description.
     */
    public Node setNamedItem(Node arg)
    throws DOMException {

        boolean errCheck = ownerNode.ownerDocument().errorChecking;
        if (errCheck) {
            if (isReadOnly()) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
                throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, msg);
            }
            if (arg.getOwnerDocument() != ownerNode.ownerDocument()) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null);
                throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, msg);
            }
            if (arg.getNodeType() != Node.ATTRIBUTE_NODE) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "HIERARCHY_REQUEST_ERR", null);
                throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, msg);
            }
        }
        AttrImpl argn = (AttrImpl)arg;

        if (argn.isOwned()){
            if (errCheck && argn.getOwnerElement() != ownerNode) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INUSE_ATTRIBUTE_ERR", null);
                throw new DOMException(DOMException.INUSE_ATTRIBUTE_ERR, msg);
            }
            // replacing an Attribute with itself does nothing
            return arg;
        }


        // set owner
        argn.ownerNode = ownerNode;
        argn.isOwned(true);

        int i = findNamePoint(argn.getNodeName(),0);
        AttrImpl previous = null;
        if (i >= 0) {
            previous = (AttrImpl) nodes.get(i);
            nodes.set(i, arg);
            previous.ownerNode = ownerNode.ownerDocument();
            previous.isOwned(false);
            // make sure it won't be mistaken with defaults in case it's reused
            previous.isSpecified(true);
        } else {
            i = -1 - i; // Insert point (may be end of list)
            if (null == nodes) {
                nodes = new ArrayList<>(5);
            }
            nodes.add(i, arg);
        }

        // notify document
        ownerNode.ownerDocument().setAttrNode(argn, previous);

        // If the new attribute is not normalized,
        // the owning element is inherently not normalized.
        if (!argn.isNormalized()) {
            ownerNode.isNormalized(false);
        }
        return previous;

    } // setNamedItem(Node):Node

    /**
     * Adds an attribute using its namespaceURI and localName.
     * @see org.w3c.dom.NamedNodeMap#setNamedItem
     * @return If the new Node replaces an existing node the replaced Node is
     *      returned, otherwise null is returned.
     * @param arg A node to store in a named node map.
     */
    public Node setNamedItemNS(Node arg)
    throws DOMException {

        boolean errCheck = ownerNode.ownerDocument().errorChecking;
        if (errCheck) {
            if (isReadOnly()) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
                throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, msg);
            }
            if(arg.getOwnerDocument() != ownerNode.ownerDocument()) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null);
                throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, msg);
            }
            if (arg.getNodeType() != Node.ATTRIBUTE_NODE) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "HIERARCHY_REQUEST_ERR", null);
                throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, msg);
            }
        }
        AttrImpl argn = (AttrImpl)arg;

        if (argn.isOwned()){
            if (errCheck && argn.getOwnerElement() != ownerNode) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INUSE_ATTRIBUTE_ERR", null);
                throw new DOMException(DOMException.INUSE_ATTRIBUTE_ERR, msg);
            }
            // replacing an Attribute with itself does nothing
            return arg;
        }

        // set owner
        argn.ownerNode = ownerNode;
        argn.isOwned(true);

        int i = findNamePoint(argn.getNamespaceURI(), argn.getLocalName());
        AttrImpl previous = null;
        if (i >= 0) {
            previous = (AttrImpl) nodes.get(i);
            nodes.set(i, arg);
            previous.ownerNode = ownerNode.ownerDocument();
            previous.isOwned(false);
            // make sure it won't be mistaken with defaults in case it's reused
            previous.isSpecified(true);
        } else {
            // If we can't find by namespaceURI, localName, then we find by
            // nodeName so we know where to insert.
            i = findNamePoint(arg.getNodeName(),0);
            if (i >=0) {
                previous = (AttrImpl) nodes.get(i);
                nodes.add(i, arg);
            } else {
                i = -1 - i; // Insert point (may be end of list)
                if (null == nodes) {
                    nodes = new ArrayList<>(5);
                }
                nodes.add(i, arg);
            }
        }
        //      changed(true);

        // notify document
        ownerNode.ownerDocument().setAttrNode(argn, previous);

        // If the new attribute is not normalized,
        // the owning element is inherently not normalized.
        if (!argn.isNormalized()) {
            ownerNode.isNormalized(false);
        }
        return previous;

    } // setNamedItemNS(Node):Node

    /**
     * Removes an attribute specified by name.
     * @param name
     *      The name of a node to remove. If the
     *      removed attribute is known to have a default value, an
     *      attribute immediately appears containing the default value
     *      as well as the corresponding namespace URI, local name,
     *      and prefix when applicable.
     * @return The node removed from the map if a node with such a name exists.
     * @throws              NOT_FOUND_ERR: Raised if there is no node named
     *                      name in the map.
     */
    /***/
    public Node removeNamedItem(String name)
        throws DOMException {
        return internalRemoveNamedItem(name, true);
    }

    /**
     * Same as removeNamedItem except that it simply returns null if the
     * specified name is not found.
     */
    Node safeRemoveNamedItem(String name) {
        return internalRemoveNamedItem(name, false);
    }


    /**
     * NON-DOM: Remove the node object
     *
     * NOTE: Specifically removes THIS NODE -- not the node with this
     * name, nor the node with these contents. If node does not belong to
     * this named node map, we throw a DOMException.
     *
     * @param item       The node to remove
     * @param addDefault true -- magically add default attribute
     * @return Removed node
     * @exception DOMException
     */
    protected Node removeItem(Node item, boolean addDefault)
        throws DOMException {

        int index = -1;
        if (nodes != null) {
            final int size = nodes.size();
            for (int i = 0; i < size; ++i) {
                if (nodes.get(i) == item) {
                    index = i;
                    break;
                }
            }
        }
        if (index < 0) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_FOUND_ERR", null);
            throw new DOMException(DOMException.NOT_FOUND_ERR, msg);
        }

        return remove((AttrImpl)item, index, addDefault);
    }

    /**
     * Internal removeNamedItem method allowing to specify whether an exception
     * must be thrown if the specified name is not found.
     */
    final protected Node internalRemoveNamedItem(String name, boolean raiseEx){
        if (isReadOnly()) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
                throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, msg);
        }
        int i = findNamePoint(name,0);
        if (i < 0) {
            if (raiseEx) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_FOUND_ERR", null);
                throw new DOMException(DOMException.NOT_FOUND_ERR, msg);
            } else {
                return null;
            }
        }

        return remove((AttrImpl)nodes.get(i), i, true);

    } // internalRemoveNamedItem(String,boolean):Node

    private final Node remove(AttrImpl attr, int index,
                              boolean addDefault) {

        CoreDocumentImpl ownerDocument = ownerNode.ownerDocument();
        String name = attr.getNodeName();
        if (attr.isIdAttribute()) {
            ownerDocument.removeIdentifier(attr.getValue());
        }

        if (hasDefaults() && addDefault) {
            // If there's a default, add it instead
            NamedNodeMapImpl defaults =
                ((ElementImpl) ownerNode).getDefaultAttributes();

            Node d;
            if (defaults != null &&
                (d = defaults.getNamedItem(name)) != null &&
                findNamePoint(name, index+1) < 0) {
                    NodeImpl clone = (NodeImpl)d.cloneNode(true);
                    if (d.getLocalName() !=null){
                            // we must rely on the name to find a default attribute
                            // ("test:attr"), but while copying it from the DOCTYPE
                            // we should not loose namespace URI that was assigned
                            // to the attribute in the instance document.
                            ((AttrNSImpl)clone).namespaceURI = attr.getNamespaceURI();
                    }
                    clone.ownerNode = ownerNode;
                    clone.isOwned(true);
                    clone.isSpecified(false);

                    nodes.set(index, clone);
                    if (attr.isIdAttribute()) {
                        ownerDocument.putIdentifier(clone.getNodeValue(),
                                                (ElementImpl)ownerNode);
                    }
            } else {
                nodes.remove(index);
            }
        } else {
            nodes.remove(index);
        }

        //        changed(true);

        // remove reference to owner
        attr.ownerNode = ownerDocument;
        attr.isOwned(false);

        // make sure it won't be mistaken with defaults in case it's
        // reused
        attr.isSpecified(true);
        attr.isIdAttribute(false);

        // notify document
        ownerDocument.removedAttrNode(attr, ownerNode, name);

        return attr;
    }

    /**
     * Introduced in DOM Level 2. <p>
     * Removes an attribute specified by local name and namespace URI.
     * @param namespaceURI
     *                      The namespace URI of the node to remove.
     *                      When it is null or an empty string, this
     *                      method behaves like removeNamedItem.
     * @param name          The local name of the node to remove. If the
     *                      removed attribute is known to have a default
     *                      value, an attribute immediately appears
     *                      containing the default value.
     * @return Node         The node removed from the map if a node with such
     *                      a local name and namespace URI exists.
     * @throws              NOT_FOUND_ERR: Raised if there is no node named
     *                      name in the map.
     */
    public Node removeNamedItemNS(String namespaceURI, String name)
        throws DOMException {
        return internalRemoveNamedItemNS(namespaceURI, name, true);
    }

    /**
     * Same as removeNamedItem except that it simply returns null if the
     * specified local name and namespace URI is not found.
     */
    Node safeRemoveNamedItemNS(String namespaceURI, String name) {
        return internalRemoveNamedItemNS(namespaceURI, name, false);
    }

    /**
     * Internal removeNamedItemNS method allowing to specify whether an
     * exception must be thrown if the specified local name and namespace URI
     * is not found.
     */
    final protected Node internalRemoveNamedItemNS(String namespaceURI,
            String name,
            boolean raiseEx) {

        CoreDocumentImpl ownerDocument = ownerNode.ownerDocument();
        if (ownerDocument.errorChecking && isReadOnly()) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
            throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, msg);
        }
        int i = findNamePoint(namespaceURI, name);
        if (i < 0) {
            if (raiseEx) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_FOUND_ERR", null);
                throw new DOMException(DOMException.NOT_FOUND_ERR, msg);
            } else {
                return null;
            }
        }

        AttrImpl n = (AttrImpl)nodes.get(i);

        if (n.isIdAttribute()) {
            ownerDocument.removeIdentifier(n.getValue());
        }
        // If there's a default, add it instead
        String nodeName = n.getNodeName();
        if (hasDefaults()) {
            NamedNodeMapImpl defaults = ((ElementImpl) ownerNode).getDefaultAttributes();
            Node d;
            if (defaults != null
                    && (d = defaults.getNamedItem(nodeName)) != null)
            {
                int j = findNamePoint(nodeName,0);
                if (j>=0 && findNamePoint(nodeName, j+1) < 0) {
                    NodeImpl clone = (NodeImpl)d.cloneNode(true);
                    clone.ownerNode = ownerNode;
                    if (d.getLocalName() != null) {
                        // we must rely on the name to find a default attribute
                        // ("test:attr"), but while copying it from the DOCTYPE
                        // we should not loose namespace URI that was assigned
                        // to the attribute in the instance document.
                        ((AttrNSImpl)clone).namespaceURI = namespaceURI;
                    }
                    clone.isOwned(true);
                    clone.isSpecified(false);
                    nodes.set(i, clone);
                    if (clone.isIdAttribute()) {
                        ownerDocument.putIdentifier(clone.getNodeValue(),
                                (ElementImpl)ownerNode);
                    }
                } else {
                    nodes.remove(i);
                }
            } else {
                nodes.remove(i);
            }
        } else {
            nodes.remove(i);
        }

        //        changed(true);

        // remove reference to owner
        n.ownerNode = ownerDocument;
        n.isOwned(false);
        // make sure it won't be mistaken with defaults in case it's
        // reused
        n.isSpecified(true);
        // update id table if needed
        n.isIdAttribute(false);

        // notify document
        ownerDocument.removedAttrNode(n, ownerNode, name);

        return n;

    } // internalRemoveNamedItemNS(String,String,boolean):Node

    //
    // Public methods
    //

    /**
     * Cloning a NamedNodeMap is a DEEP OPERATION; it always clones
     * all the nodes contained in the map.
     */

    public NamedNodeMapImpl cloneMap(NodeImpl ownerNode) {
        AttributeMap newmap =
            new AttributeMap((ElementImpl) ownerNode, null);
        newmap.hasDefaults(hasDefaults());
        newmap.cloneContent(this);
        return newmap;
    } // cloneMap():AttributeMap

    /**
     * Override parent's method to set the ownerNode correctly
     */
    protected void cloneContent(NamedNodeMapImpl srcmap) {
        List<Node> srcnodes = srcmap.nodes;
        if (srcnodes != null) {
            int size = srcnodes.size();
            if (size != 0) {
                if (nodes == null) {
                    nodes = new ArrayList<>(size);
                }
                else {
                    nodes.clear();
                }
                for (int i = 0; i < size; ++i) {
                    NodeImpl n = (NodeImpl) srcnodes.get(i);
                    NodeImpl clone = (NodeImpl) n.cloneNode(true);
                    clone.isSpecified(n.isSpecified());
                    nodes.add(clone);
                    clone.ownerNode = ownerNode;
                    clone.isOwned(true);
                }
            }
        }
    } // cloneContent():AttributeMap


    /**
     * Move specified attributes from the given map to this one
     */
    void moveSpecifiedAttributes(AttributeMap srcmap) {
        int nsize = (srcmap.nodes != null) ? srcmap.nodes.size() : 0;
        for (int i = nsize - 1; i >= 0; i--) {
            AttrImpl attr = (AttrImpl) srcmap.nodes.get(i);
            if (attr.isSpecified()) {
                srcmap.remove(attr, i, false);
                if (attr.getLocalName() != null) {
                    setNamedItem(attr);
                }
                else {
                    setNamedItemNS(attr);
                }
            }
        }
    } // moveSpecifiedAttributes(AttributeMap):void


    /**
     * Get this AttributeMap in sync with the given "defaults" map.
     * @param defaults The default attributes map to sync with.
     */
    protected void reconcileDefaults(NamedNodeMapImpl defaults) {

        // remove any existing default
        int nsize = (nodes != null) ? nodes.size() : 0;
        for (int i = nsize - 1; i >= 0; --i) {
            AttrImpl attr = (AttrImpl) nodes.get(i);
            if (!attr.isSpecified()) {
                remove(attr, i, false);
            }
        }
        // add the new defaults
        if (defaults == null) {
            return;
        }
        if (nodes == null || nodes.size() == 0) {
            cloneContent(defaults);
        }
        else {
            int dsize = defaults.nodes.size();
            for (int n = 0; n < dsize; ++n) {
                AttrImpl d = (AttrImpl) defaults.nodes.get(n);
                int i = findNamePoint(d.getNodeName(), 0);
                if (i < 0) {
                        i = -1 - i;
                    NodeImpl clone = (NodeImpl) d.cloneNode(true);
                    clone.ownerNode = ownerNode;
                    clone.isOwned(true);
                    clone.isSpecified(false);
                        nodes.add(i, clone);
                }
            }
        }

    } // reconcileDefaults()

    protected final int addItem (Node arg) {

        final AttrImpl argn = (AttrImpl) arg;

        // set owner
        argn.ownerNode = ownerNode;
        argn.isOwned(true);

        int i = findNamePoint(argn.getNamespaceURI(), argn.getLocalName());
        if (i >= 0) {
            nodes.set(i, arg);
        }
        else {
            // If we can't find by namespaceURI, localName, then we find by
            // nodeName so we know where to insert.
            i = findNamePoint(argn.getNodeName(),0);
            if (i >= 0) {
                nodes.add(i, arg);
            }
            else {
                i = -1 - i; // Insert point (may be end of list)
                if (null == nodes) {
                    nodes = new ArrayList<>(5);
                }
                nodes.add(i, arg);
            }
        }

        // notify document
        ownerNode.ownerDocument().setAttrNode(argn, null);
        return i;
    }

} // class AttributeMap
