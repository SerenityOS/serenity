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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The Document interface represents the entire HTML or XML document.
 * Conceptually, it is the root of the document tree, and provides the
 * primary access to the document's data.
 * <P>
 * Since elements, text nodes, comments, processing instructions,
 * etc. cannot exist outside the context of a Document, the Document
 * interface also contains the factory methods needed to create these
 * objects. The Node objects created have a ownerDocument attribute
 * which associates them with the Document within whose context they
 * were created.
 *
 * @xerces.internal
 *
 * @since  PR-DOM-Level-1-19980818.
 * @LastModified: May 2019
 */
public class DeferredDocumentImpl
    extends DocumentImpl
    implements DeferredNode {

    //
    // Constants
    //

    /** Serialization version. */
    static final long serialVersionUID = 5186323580749626857L;

    // debugging

    /** To include code for printing the ref count tables. */
    private static final boolean DEBUG_PRINT_REF_COUNTS = false;

    /** To include code for printing the internal tables. */
    private static final boolean DEBUG_PRINT_TABLES = false;

    /** To debug identifiers set to true and recompile. */
    private static final boolean DEBUG_IDS = false;

    // protected

    /** Chunk shift. */
    protected static final int CHUNK_SHIFT = 8;           // 2^8 = 256

    /** Chunk size. */
    protected static final int CHUNK_SIZE = (1 << CHUNK_SHIFT);

    /** Chunk mask. */
    protected static final int CHUNK_MASK = CHUNK_SIZE - 1;

    /** Initial chunk size. */
    protected static final int INITIAL_CHUNK_COUNT = (1 << (13 - CHUNK_SHIFT));   // 32

    //
    // Data
    //

    // lazy-eval information
    // To maximize memory consumption the actual semantic of these fields vary
    // depending on the node type.

    /** Node count. */
    protected transient int fNodeCount = 0;

    /** Node types. */
    protected transient int fNodeType[][];

    /** Node names. */
    protected transient Object fNodeName[][];

    /** Node values. */
    protected transient Object fNodeValue[][];

    /** Node parents. */
    protected transient int fNodeParent[][];

    /** Node first children. */
    protected transient int fNodeLastChild[][];

    /** Node prev siblings. */
    protected transient int fNodePrevSib[][];

    /** Node namespace URI. */
    protected transient Object fNodeURI[][];

    /** Extra data. */
    protected transient int fNodeExtra[][];

    /** Identifier count. */
    protected transient int fIdCount;

    /** Identifier name indexes. */
    protected transient String fIdName[];

    /** Identifier element indexes. */
    protected transient int fIdElement[];

    /** DOM2: For namespace support in the deferred case.
     */
    // Implementation Note: The deferred element and attribute must know how to
    // interpret the int representing the qname.
    protected boolean fNamespacesEnabled = false;

    //
    // private data
    //
    private transient final StringBuilder fBufferStr = new StringBuilder();
    private transient final List<String> fStrChunks = new ArrayList<>();

    //
    // Constructors
    //

    /**
     * NON-DOM: Actually creating a Document is outside the DOM's spec,
     * since it has to operate in terms of a particular implementation.
     */
    public DeferredDocumentImpl() {
        this(false);
    } // <init>()

    /**
     * NON-DOM: Actually creating a Document is outside the DOM's spec,
     * since it has to operate in terms of a particular implementation.
     */
    public DeferredDocumentImpl(boolean namespacesEnabled) {
        this(namespacesEnabled, false);
    } // <init>(boolean)

    /** Experimental constructor. */
    public DeferredDocumentImpl(boolean namespaces, boolean grammarAccess) {
        super(grammarAccess);

        needsSyncData(true);
        needsSyncChildren(true);

        fNamespacesEnabled = namespaces;

    } // <init>(boolean,boolean)

    //
    // Public methods
    //

    /**
     * Retrieve information describing the abilities of this particular
     * DOM implementation. Intended to support applications that may be
     * using DOMs retrieved from several different sources, potentially
     * with different underlying representations.
     */
    public DOMImplementation getImplementation() {
        // Currently implemented as a singleton, since it's hardcoded
        // information anyway.
        return DeferredDOMImplementationImpl.getDOMImplementation();
    }

    /** Returns the cached parser.getNamespaces() value.*/
    boolean getNamespacesEnabled() {
        return fNamespacesEnabled;
    }

    void setNamespacesEnabled(boolean enable) {
        fNamespacesEnabled = enable;
    }

    // internal factory methods

    /** Creates a document node in the table. */
    public int createDeferredDocument() {
        int nodeIndex = createNode(Node.DOCUMENT_NODE);
        return nodeIndex;
    }

    /** Creates a doctype. */
    public int createDeferredDocumentType(String rootElementName,
                                          String publicId, String systemId) {

        // create node
        int nodeIndex = createNode(Node.DOCUMENT_TYPE_NODE);
        int chunk     = nodeIndex >> CHUNK_SHIFT;
        int index     = nodeIndex & CHUNK_MASK;

        // save name, public id, system id
        setChunkValue(fNodeName, rootElementName, chunk, index);
        setChunkValue(fNodeValue, publicId, chunk, index);
        setChunkValue(fNodeURI, systemId, chunk, index);

        // return node index
        return nodeIndex;

    } // createDeferredDocumentType(String,String,String):int

    public void setInternalSubset(int doctypeIndex, String subset) {
        int chunk     = doctypeIndex >> CHUNK_SHIFT;
        int index     = doctypeIndex & CHUNK_MASK;

        // create extra data node to store internal subset
        int extraDataIndex = createNode(Node.DOCUMENT_TYPE_NODE);
        int echunk = extraDataIndex >> CHUNK_SHIFT;
        int eindex = extraDataIndex & CHUNK_MASK;
        setChunkIndex(fNodeExtra, extraDataIndex, chunk, index);
        setChunkValue(fNodeValue, subset, echunk, eindex);
    }

    /** Creates a notation in the table. */
    public int createDeferredNotation(String notationName,
                                      String publicId, String systemId, String baseURI) {

        // create node
        int nodeIndex = createNode(Node.NOTATION_NODE);
        int chunk     = nodeIndex >> CHUNK_SHIFT;
        int index     = nodeIndex & CHUNK_MASK;


        // create extra data node
        int extraDataIndex = createNode(Node.NOTATION_NODE);
        int echunk = extraDataIndex >> CHUNK_SHIFT;
        int eindex = extraDataIndex & CHUNK_MASK;

        // save name, public id, system id, and notation name
        setChunkValue(fNodeName, notationName, chunk, index);
        setChunkValue(fNodeValue, publicId, chunk, index);
        setChunkValue(fNodeURI, systemId, chunk, index);

        // in extra data node set baseURI value
        setChunkIndex(fNodeExtra, extraDataIndex, chunk, index);
        setChunkValue(fNodeName, baseURI, echunk, eindex);

        // return node index
        return nodeIndex;

    } // createDeferredNotation(String,String,String):int

    /** Creates an entity in the table. */
    public int createDeferredEntity(String entityName, String publicId,
                                    String systemId, String notationName,
                                    String baseURI) {
        // create node
        int nodeIndex = createNode(Node.ENTITY_NODE);
        int chunk     = nodeIndex >> CHUNK_SHIFT;
        int index     = nodeIndex & CHUNK_MASK;

        // create extra data node
        int extraDataIndex = createNode(Node.ENTITY_NODE);
        int echunk = extraDataIndex >> CHUNK_SHIFT;
        int eindex = extraDataIndex & CHUNK_MASK;

        // save name, public id, system id, and notation name
        setChunkValue(fNodeName, entityName, chunk, index);
        setChunkValue(fNodeValue, publicId, chunk, index);
        setChunkValue(fNodeURI, systemId, chunk, index);
        setChunkIndex(fNodeExtra, extraDataIndex, chunk, index);
        // set other values in the extra chunk
        // notation
        setChunkValue(fNodeName, notationName, echunk, eindex);
        // version  L3
        setChunkValue(fNodeValue, null, echunk, eindex);
        // encoding L3
        setChunkValue(fNodeURI, null, echunk, eindex);


        int extraDataIndex2 = createNode(Node.ENTITY_NODE);
        int echunk2 = extraDataIndex2 >> CHUNK_SHIFT;
        int eindex2 = extraDataIndex2 & CHUNK_MASK;

        setChunkIndex(fNodeExtra, extraDataIndex2, echunk, eindex);

        // baseURI
        setChunkValue(fNodeName, baseURI, echunk2, eindex2);

        // return node index
        return nodeIndex;

    } // createDeferredEntity(String,String,String,String):int

    public String getDeferredEntityBaseURI (int entityIndex){
        if (entityIndex != -1) {
            int extraDataIndex = getNodeExtra(entityIndex, false);
            extraDataIndex = getNodeExtra(extraDataIndex, false);
            return getNodeName (extraDataIndex, false);
        }
        return null;
    }

    // DOM Level 3: setting encoding and version
    public void setEntityInfo(int currentEntityDecl,
                              String version, String encoding){
        int eNodeIndex = getNodeExtra(currentEntityDecl, false);
        if (eNodeIndex !=-1) {
            int echunk = eNodeIndex >> CHUNK_SHIFT;
            int eindex = eNodeIndex & CHUNK_MASK;
            setChunkValue(fNodeValue, version, echunk, eindex);
            setChunkValue(fNodeURI, encoding, echunk, eindex);
        }
    }

    // DOM Level 3: sets element TypeInfo
    public void setTypeInfo(int elementNodeIndex, Object type) {
        int elementChunk     = elementNodeIndex >> CHUNK_SHIFT;
        int elementIndex     = elementNodeIndex & CHUNK_MASK;
        setChunkValue(fNodeValue, type, elementChunk, elementIndex);
    }

    /**
     * DOM Internal
     *
     * An attribute specifying the actual encoding of this document. This is
     * <code>null</code> otherwise.
     * <br> This attribute represents the property [character encoding scheme]
     * defined in .
     */
    public void setInputEncoding(int currentEntityDecl, String value){
        // get first extra data chunk
        int nodeIndex = getNodeExtra(currentEntityDecl, false);
        // get second extra data chunk
        int extraDataIndex = getNodeExtra(nodeIndex, false);

        int echunk = extraDataIndex >> CHUNK_SHIFT;
        int eindex = extraDataIndex & CHUNK_MASK;

        setChunkValue(fNodeValue, value, echunk, eindex);

    }

    /** Creates an entity reference node in the table. */
    public int createDeferredEntityReference(String name, String baseURI) {

        // create node
        int nodeIndex = createNode(Node.ENTITY_REFERENCE_NODE);
        int chunk     = nodeIndex >> CHUNK_SHIFT;
        int index     = nodeIndex & CHUNK_MASK;
        setChunkValue(fNodeName, name, chunk, index);
        setChunkValue(fNodeValue, baseURI, chunk, index);

        // return node index
        return nodeIndex;

    } // createDeferredEntityReference(String):int


    /**
     * Creates an element node with a URI in the table and type information.
     * @deprecated
     */
    @Deprecated
    public int createDeferredElement(String elementURI, String elementName,
                                      Object type) {

        // create node
        int elementNodeIndex = createNode(Node.ELEMENT_NODE);
        int elementChunk     = elementNodeIndex >> CHUNK_SHIFT;
        int elementIndex     = elementNodeIndex & CHUNK_MASK;
        setChunkValue(fNodeName, elementName, elementChunk, elementIndex);
        setChunkValue(fNodeURI, elementURI, elementChunk, elementIndex);
        setChunkValue(fNodeValue, type, elementChunk, elementIndex);

        // return node index
        return elementNodeIndex;

    } // createDeferredElement(String,String,Object):int

    /**
     * Creates an element node in the table.
     * @deprecated
     */
    @Deprecated
    public int createDeferredElement(String elementName) {
        return createDeferredElement(null, elementName);
    }

    /**
     * Creates an element node with a URI in the table.
     */
    public int createDeferredElement(String elementURI, String elementName) {

        // create node
        int elementNodeIndex = createNode(Node.ELEMENT_NODE);
        int elementChunk     = elementNodeIndex >> CHUNK_SHIFT;
        int elementIndex     = elementNodeIndex & CHUNK_MASK;
        setChunkValue(fNodeName, elementName, elementChunk, elementIndex);
        setChunkValue(fNodeURI, elementURI, elementChunk, elementIndex);

        // return node index
        return elementNodeIndex;

    } // createDeferredElement(String,String):int


        /**
         * This method is used by the DOMParser to create attributes.
         * @param elementNodeIndex
         * @param attrName
         * @param attrURI
         * @param attrValue
         * @param specified
         * @param id
         * @param type
         * @return int
         */
        public int setDeferredAttribute(int elementNodeIndex,
                                        String attrName,
                                        String attrURI,
                                        String attrValue,
                                        boolean specified,
                                        boolean id,
                                        Object type) {

                // create attribute
                int attrNodeIndex = createDeferredAttribute(attrName, attrURI, attrValue, specified);
                int attrChunk = attrNodeIndex >> CHUNK_SHIFT;
                int attrIndex = attrNodeIndex & CHUNK_MASK;
                // set attribute's parent to element
                setChunkIndex(fNodeParent, elementNodeIndex, attrChunk, attrIndex);

                int elementChunk = elementNodeIndex >> CHUNK_SHIFT;
                int elementIndex = elementNodeIndex & CHUNK_MASK;

                // get element's last attribute
                int lastAttrNodeIndex = getChunkIndex(fNodeExtra, elementChunk, elementIndex);
                if (lastAttrNodeIndex != 0) {
                        // add link from new attribute to last attribute
                        setChunkIndex(fNodePrevSib, lastAttrNodeIndex, attrChunk, attrIndex);
                }
                // add link from element to new last attribute
                setChunkIndex(fNodeExtra, attrNodeIndex, elementChunk, elementIndex);

                int extra = getChunkIndex(fNodeExtra, attrChunk, attrIndex);
                if (id) {
                        extra = extra | ID;
                        setChunkIndex(fNodeExtra, extra, attrChunk, attrIndex);
                        String value = getChunkValue(fNodeValue, attrChunk, attrIndex);
                        putIdentifier(value, elementNodeIndex);
                }
                // store type information
                if (type != null) {
                        int extraDataIndex = createNode(DeferredNode.TYPE_NODE);
                        int echunk = extraDataIndex >> CHUNK_SHIFT;
                        int eindex = extraDataIndex & CHUNK_MASK;

                        setChunkIndex(fNodeLastChild, extraDataIndex, attrChunk, attrIndex);
                        setChunkValue(fNodeValue, type, echunk, eindex);
                }

                // return node index
                return attrNodeIndex;
        }

    /** Creates an attribute in the table. */
    public int createDeferredAttribute(String attrName, String attrValue,
                                       boolean specified) {
        return createDeferredAttribute(attrName, null, attrValue, specified);
    }

    /** Creates an attribute with a URI in the table. */
    public int createDeferredAttribute(String attrName, String attrURI,
                                       String attrValue, boolean specified) {

        // create node
        int nodeIndex = createNode(NodeImpl.ATTRIBUTE_NODE);
        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(fNodeName, attrName, chunk, index);
        setChunkValue(fNodeURI, attrURI, chunk, index);
        setChunkValue(fNodeValue, attrValue, chunk, index);
        int extra = specified ? SPECIFIED : 0;
        setChunkIndex(fNodeExtra, extra, chunk, index);

        // return node index
        return nodeIndex;

    } // createDeferredAttribute(String,String,String,boolean):int

    /** Creates an element definition in the table.*/
    public int createDeferredElementDefinition(String elementName) {

        // create node
        int nodeIndex = createNode(NodeImpl.ELEMENT_DEFINITION_NODE);
        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(fNodeName, elementName, chunk, index);

        // return node index
        return nodeIndex;

    } // createDeferredElementDefinition(String):int

    /** Creates a text node in the table. */
    public int createDeferredTextNode(String data,
                                      boolean ignorableWhitespace) {

        // create node
        int nodeIndex = createNode(Node.TEXT_NODE);
        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(fNodeValue, data, chunk, index);
        // use extra to store ignorableWhitespace info
        setChunkIndex(fNodeExtra, ignorableWhitespace ?  1 : 0, chunk, index);

        // return node index
        return nodeIndex;

    } // createDeferredTextNode(String,boolean):int

    /** Creates a CDATA section node in the table. */
    public int createDeferredCDATASection(String data) {

        // create node
        int nodeIndex = createNode(Node.CDATA_SECTION_NODE);
        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(fNodeValue, data, chunk, index);

        // return node index
        return nodeIndex;

    } // createDeferredCDATASection(String):int

    /** Creates a processing instruction node in the table. */
    public int createDeferredProcessingInstruction(String target,
                                                   String data) {
        // create node
        int nodeIndex = createNode(Node.PROCESSING_INSTRUCTION_NODE);
        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(fNodeName, target, chunk, index);
        setChunkValue(fNodeValue, data, chunk, index);
        // return node index
        return nodeIndex;

    } // createDeferredProcessingInstruction(String,String):int

    /** Creates a comment node in the table. */
    public int createDeferredComment(String data) {

        // create node
        int nodeIndex = createNode(Node.COMMENT_NODE);
        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        setChunkValue(fNodeValue, data, chunk, index);

        // return node index
        return nodeIndex;

    } // createDeferredComment(String):int

    /** Creates a clone of the specified node. */
    public int cloneNode(int nodeIndex, boolean deep) {

        // clone immediate node

        int nchunk = nodeIndex >> CHUNK_SHIFT;
        int nindex = nodeIndex & CHUNK_MASK;
        int nodeType = fNodeType[nchunk][nindex];
        int cloneIndex = createNode((short)nodeType);
        int cchunk = cloneIndex >> CHUNK_SHIFT;
        int cindex = cloneIndex & CHUNK_MASK;
        setChunkValue(fNodeName, fNodeName[nchunk][nindex], cchunk, cindex);
        setChunkValue(fNodeValue, fNodeValue[nchunk][nindex], cchunk, cindex);
        setChunkValue(fNodeURI, fNodeURI[nchunk][nindex], cchunk, cindex);
        int extraIndex = fNodeExtra[nchunk][nindex];
        if (extraIndex != -1) {
            if (nodeType != Node.ATTRIBUTE_NODE && nodeType != Node.TEXT_NODE) {
                extraIndex = cloneNode(extraIndex, false);
            }
            setChunkIndex(fNodeExtra, extraIndex, cchunk, cindex);
        }

        // clone and attach children
        if (deep) {
            int prevIndex = -1;
            int childIndex = getLastChild(nodeIndex, false);
            while (childIndex != -1) {
                int clonedChildIndex = cloneNode(childIndex, deep);
                insertBefore(cloneIndex, clonedChildIndex, prevIndex);
                prevIndex = clonedChildIndex;
                childIndex = getRealPrevSibling(childIndex, false);
            }


        }

        // return cloned node index
        return cloneIndex;

    } // cloneNode(int,boolean):int

    /** Appends a child to the specified parent in the table. */
    public void appendChild(int parentIndex, int childIndex) {

        // append parent index
        int pchunk = parentIndex >> CHUNK_SHIFT;
        int pindex = parentIndex & CHUNK_MASK;
        int cchunk = childIndex >> CHUNK_SHIFT;
        int cindex = childIndex & CHUNK_MASK;
        setChunkIndex(fNodeParent, parentIndex, cchunk, cindex);

        // set previous sibling of new child
        int olast = getChunkIndex(fNodeLastChild, pchunk, pindex);
        setChunkIndex(fNodePrevSib, olast, cchunk, cindex);

        // update parent's last child
        setChunkIndex(fNodeLastChild, childIndex, pchunk, pindex);

    } // appendChild(int,int)

    /** Adds an attribute node to the specified element. */
    public int setAttributeNode(int elemIndex, int attrIndex) {

        int echunk = elemIndex >> CHUNK_SHIFT;
        int eindex = elemIndex & CHUNK_MASK;
        int achunk = attrIndex >> CHUNK_SHIFT;
        int aindex = attrIndex & CHUNK_MASK;

        // see if this attribute is already here
        String attrName = getChunkValue(fNodeName, achunk, aindex);
        int oldAttrIndex = getChunkIndex(fNodeExtra, echunk, eindex);
        int nextIndex = -1;
        int oachunk = -1;
        int oaindex = -1;
        while (oldAttrIndex != -1) {
            oachunk = oldAttrIndex >> CHUNK_SHIFT;
            oaindex = oldAttrIndex & CHUNK_MASK;
            String oldAttrName = getChunkValue(fNodeName, oachunk, oaindex);
            if (oldAttrName.equals(attrName)) {
                break;
            }
            nextIndex = oldAttrIndex;
            oldAttrIndex = getChunkIndex(fNodePrevSib, oachunk, oaindex);
        }

        // remove old attribute
        if (oldAttrIndex != -1) {

            // patch links
            int prevIndex = getChunkIndex(fNodePrevSib, oachunk, oaindex);
            if (nextIndex == -1) {
                setChunkIndex(fNodeExtra, prevIndex, echunk, eindex);
            }
            else {
                int pchunk = nextIndex >> CHUNK_SHIFT;
                int pindex = nextIndex & CHUNK_MASK;
                setChunkIndex(fNodePrevSib, prevIndex, pchunk, pindex);
            }

            // remove connections to siblings
            clearChunkIndex(fNodeType, oachunk, oaindex);
            clearChunkValue(fNodeName, oachunk, oaindex);
            clearChunkValue(fNodeValue, oachunk, oaindex);
            clearChunkIndex(fNodeParent, oachunk, oaindex);
            clearChunkIndex(fNodePrevSib, oachunk, oaindex);
            int attrTextIndex =
                clearChunkIndex(fNodeLastChild, oachunk, oaindex);
            int atchunk = attrTextIndex >> CHUNK_SHIFT;
            int atindex = attrTextIndex & CHUNK_MASK;
            clearChunkIndex(fNodeType, atchunk, atindex);
            clearChunkValue(fNodeValue, atchunk, atindex);
            clearChunkIndex(fNodeParent, atchunk, atindex);
            clearChunkIndex(fNodeLastChild, atchunk, atindex);
        }

        // add new attribute
        int prevIndex = getChunkIndex(fNodeExtra, echunk, eindex);
        setChunkIndex(fNodeExtra, attrIndex, echunk, eindex);
        setChunkIndex(fNodePrevSib, prevIndex, achunk, aindex);

        // return
        return oldAttrIndex;

    } // setAttributeNode(int,int):int


    /** Adds an attribute node to the specified element. */
    public void setIdAttributeNode(int elemIndex, int attrIndex) {

        int chunk = attrIndex >> CHUNK_SHIFT;
        int index = attrIndex & CHUNK_MASK;
        int extra = getChunkIndex(fNodeExtra, chunk, index);
        extra = extra | ID;
        setChunkIndex(fNodeExtra, extra, chunk, index);

        String value = getChunkValue(fNodeValue, chunk, index);
        putIdentifier(value, elemIndex);
    }


    /** Sets type of attribute */
    public void setIdAttribute(int attrIndex) {

        int chunk = attrIndex >> CHUNK_SHIFT;
        int index = attrIndex & CHUNK_MASK;
        int extra = getChunkIndex(fNodeExtra, chunk, index);
        extra = extra | ID;
        setChunkIndex(fNodeExtra, extra, chunk, index);
    }

    /** Inserts a child before the specified node in the table. */
    public int insertBefore(int parentIndex, int newChildIndex, int refChildIndex) {

        if (refChildIndex == -1) {
            appendChild(parentIndex, newChildIndex);
            return newChildIndex;
        }

        int nchunk = newChildIndex >> CHUNK_SHIFT;
        int nindex = newChildIndex & CHUNK_MASK;
        int rchunk = refChildIndex >> CHUNK_SHIFT;
        int rindex = refChildIndex & CHUNK_MASK;
        int previousIndex = getChunkIndex(fNodePrevSib, rchunk, rindex);
        setChunkIndex(fNodePrevSib, newChildIndex, rchunk, rindex);
        setChunkIndex(fNodePrevSib, previousIndex, nchunk, nindex);

        return newChildIndex;

    } // insertBefore(int,int,int):int

    /** Sets the last child of the parentIndex to childIndex. */
    public void setAsLastChild(int parentIndex, int childIndex) {
        int pchunk = parentIndex >> CHUNK_SHIFT;
        int pindex = parentIndex & CHUNK_MASK;
        setChunkIndex(fNodeLastChild, childIndex, pchunk, pindex);
    } // setAsLastChild(int,int)

    /**
     * Returns the parent node of the given node.
     * <em>Calling this method does not free the parent index.</em>
     */
    public int getParentNode(int nodeIndex) {
        return getParentNode(nodeIndex, false);
    }

    /**
     * Returns the parent node of the given node.
     * @param free True to free parent node.
     */
    public int getParentNode(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return -1;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkIndex(fNodeParent, chunk, index)
                    : getChunkIndex(fNodeParent, chunk, index);

    } // getParentNode(int):int

    /** Returns the last child of the given node. */
    public int getLastChild(int nodeIndex) {
        return getLastChild(nodeIndex, true);
    }

    /**
     * Returns the last child of the given node.
     * @param free True to free child index.
     */
    public int getLastChild(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return -1;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkIndex(fNodeLastChild, chunk, index)
                    : getChunkIndex(fNodeLastChild, chunk, index);

    } // getLastChild(int,boolean):int

    /**
     * Returns the prev sibling of the given node.
     * This is post-normalization of Text Nodes.
     */
    public int getPrevSibling(int nodeIndex) {
        return getPrevSibling(nodeIndex, true);
    }

    /**
     * Returns the prev sibling of the given node.
     * @param free True to free sibling index.
     */
    public int getPrevSibling(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return -1;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        int type = getChunkIndex(fNodeType, chunk, index);
        if (type == Node.TEXT_NODE) {
            do {
                nodeIndex = getChunkIndex(fNodePrevSib, chunk, index);
                if (nodeIndex == -1) {
                    break;
                }
                chunk = nodeIndex >> CHUNK_SHIFT;
                index = nodeIndex & CHUNK_MASK;
                type = getChunkIndex(fNodeType, chunk, index);
            } while (type == Node.TEXT_NODE);
        }
        else {
            nodeIndex = getChunkIndex(fNodePrevSib, chunk, index);
        }

        return nodeIndex;

    } // getPrevSibling(int,boolean):int

    /**
     * Returns the <i>real</i> prev sibling of the given node,
     * directly from the data structures. Used by TextImpl#getNodeValue()
     * to normalize values.
     */
    public int getRealPrevSibling(int nodeIndex) {
        return getRealPrevSibling(nodeIndex, true);
    }

    /**
     * Returns the <i>real</i> prev sibling of the given node.
     * @param free True to free sibling index.
     */
    public int getRealPrevSibling(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return -1;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkIndex(fNodePrevSib, chunk, index)
                    : getChunkIndex(fNodePrevSib, chunk, index);

    } // getReadPrevSibling(int,boolean):int

    /**
     * Returns the index of the element definition in the table
     * with the specified name index, or -1 if no such definition
     * exists.
     */
    public int lookupElementDefinition(String elementName) {

        if (fNodeCount > 1) {

            // find doctype
            int docTypeIndex = -1;
            int nchunk = 0;
            int nindex = 0;
            for (int index = getChunkIndex(fNodeLastChild, nchunk, nindex);
                 index != -1;
                 index = getChunkIndex(fNodePrevSib, nchunk, nindex)) {

                nchunk = index >> CHUNK_SHIFT;
                nindex = index  & CHUNK_MASK;
                if (getChunkIndex(fNodeType, nchunk, nindex) == Node.DOCUMENT_TYPE_NODE) {
                    docTypeIndex = index;
                    break;
                }
            }

            // find element definition
            if (docTypeIndex == -1) {
                return -1;
            }
            nchunk = docTypeIndex >> CHUNK_SHIFT;
            nindex = docTypeIndex & CHUNK_MASK;
            for (int index = getChunkIndex(fNodeLastChild, nchunk, nindex);
                 index != -1;
                 index = getChunkIndex(fNodePrevSib, nchunk, nindex)) {

                nchunk = index >> CHUNK_SHIFT;
                nindex = index & CHUNK_MASK;
                if (getChunkIndex(fNodeType, nchunk, nindex) ==
                                           NodeImpl.ELEMENT_DEFINITION_NODE
                 && getChunkValue(fNodeName, nchunk, nindex) == elementName) {
                    return index;
                }
            }
        }

        return -1;

    } // lookupElementDefinition(String):int

    /** Instantiates the requested node object. */
    public DeferredNode getNodeObject(int nodeIndex) {

        // is there anything to do?
        if (nodeIndex == -1) {
            return null;
        }

        // get node type
        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        int type = getChunkIndex(fNodeType, chunk, index);
        if (type != Node.TEXT_NODE && type != Node.CDATA_SECTION_NODE) {
            clearChunkIndex(fNodeType, chunk, index);
        }

        // create new node
        DeferredNode node = null;
        switch (type) {

            //
            // Standard DOM node types
            //

            case Node.ATTRIBUTE_NODE: {
                if (fNamespacesEnabled) {
                    node = new DeferredAttrNSImpl(this, nodeIndex);
                } else {
                    node = new DeferredAttrImpl(this, nodeIndex);
                }
                break;
            }

            case Node.CDATA_SECTION_NODE: {
                node = new DeferredCDATASectionImpl(this, nodeIndex);
                break;
            }

            case Node.COMMENT_NODE: {
                node = new DeferredCommentImpl(this, nodeIndex);
                break;
            }

            // NOTE: Document fragments can never be "fast".
            //
            //       The parser will never ask to create a document
            //       fragment during the parse. Document fragments
            //       are used by the application *after* the parse.
            //
            // case Node.DOCUMENT_FRAGMENT_NODE: { break; }
            case Node.DOCUMENT_NODE: {
                // this node is never "fast"
                node = this;
                break;
            }

            case Node.DOCUMENT_TYPE_NODE: {
                node = new DeferredDocumentTypeImpl(this, nodeIndex);
                // save the doctype node
                docType = (DocumentTypeImpl)node;
                break;
            }

            case Node.ELEMENT_NODE: {

                if (DEBUG_IDS) {
                    System.out.println("getNodeObject(ELEMENT_NODE): "+nodeIndex);
                }

                // create node
                if (fNamespacesEnabled) {
                    node = new DeferredElementNSImpl(this, nodeIndex);
                } else {
                    node = new DeferredElementImpl(this, nodeIndex);
                }

                // check to see if this element needs to be
                // registered for its ID attributes
                if (fIdElement != null) {
                    int idIndex = binarySearch(fIdElement, 0,
                                               fIdCount-1, nodeIndex);
                    while (idIndex != -1) {

                        if (DEBUG_IDS) {
                            System.out.println("  id index: "+idIndex);
                            System.out.println("  fIdName["+idIndex+
                                               "]: "+fIdName[idIndex]);
                        }

                        // register ID
                        String name = fIdName[idIndex];
                        if (name != null) {
                            if (DEBUG_IDS) {
                                System.out.println("  name: "+name);
                                System.out.print("getNodeObject()#");
                            }
                            putIdentifier0(name, (Element)node);
                            fIdName[idIndex] = null;
                        }

                        // continue if there are more IDs for
                        // this element
                        if (idIndex + 1 < fIdCount &&
                            fIdElement[idIndex + 1] == nodeIndex) {
                            idIndex++;
                        }
                        else {
                            idIndex = -1;
                        }
                    }
                }
                break;
            }

            case Node.ENTITY_NODE: {
                node = new DeferredEntityImpl(this, nodeIndex);
                break;
            }

            case Node.ENTITY_REFERENCE_NODE: {
                node = new DeferredEntityReferenceImpl(this, nodeIndex);
                break;
            }

            case Node.NOTATION_NODE: {
                node = new DeferredNotationImpl(this, nodeIndex);
                break;
            }

            case Node.PROCESSING_INSTRUCTION_NODE: {
                node = new DeferredProcessingInstructionImpl(this, nodeIndex);
                break;
            }

            case Node.TEXT_NODE: {
                node = new DeferredTextImpl(this, nodeIndex);
                break;
            }

            //
            // non-standard DOM node types
            //

            case NodeImpl.ELEMENT_DEFINITION_NODE: {
                node = new DeferredElementDefinitionImpl(this, nodeIndex);
                break;
            }

            default: {
                throw new IllegalArgumentException("type: "+type);
            }

        } // switch node type

        // store and return
        if (node != null) {
            return node;
        }

        // error
        throw new IllegalArgumentException();

    } // createNodeObject(int):Node

    /** Returns the name of the given node. */
    public String getNodeName(int nodeIndex) {
        return getNodeName(nodeIndex, true);
    } // getNodeNameString(int):String

    /**
     * Returns the name of the given node.
     * @param free True to free the string index.
     */
    public String getNodeName(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return null;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkValue(fNodeName, chunk, index)
                    : getChunkValue(fNodeName, chunk, index);

    } // getNodeName(int,boolean):String

    /** Returns the real value of the given node. */
    public String getNodeValueString(int nodeIndex) {
        return getNodeValueString(nodeIndex, true);
    } // getNodeValueString(int):String

    /**
     * Returns the real value of the given node.
     * @param free True to free the string index.
     */
    public String getNodeValueString(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return null;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        String value = free ? clearChunkValue(fNodeValue, chunk, index)
                            : getChunkValue(fNodeValue, chunk, index);
        if (value == null) {
            return null;
        }

        int type  = getChunkIndex(fNodeType, chunk, index);
        if (type == Node.TEXT_NODE) {
            int prevSib = getRealPrevSibling(nodeIndex);
            if (prevSib != -1 &&
                getNodeType(prevSib, false) == Node.TEXT_NODE) {
                // append data that is stored in fNodeValue
                // REVISIT: for text nodes it works differently than for CDATA
                //          nodes.
                fStrChunks.add(value);
                do {
                    // go in reverse order: find last child, then
                    // its previous sibling, etc
                    chunk = prevSib >> CHUNK_SHIFT;
                    index = prevSib & CHUNK_MASK;
                    value = getChunkValue(fNodeValue, chunk, index);
                    fStrChunks.add(value);
                    prevSib = getChunkIndex(fNodePrevSib, chunk, index);
                    if (prevSib == -1) {
                        break;
                    }
                } while (getNodeType(prevSib, false) == Node.TEXT_NODE);

                int chunkCount = fStrChunks.size();

                // add to the buffer in the correct order.
                for (int i = chunkCount - 1; i >= 0; i--) {
                    fBufferStr.append(fStrChunks.get(i));
                }

                value = fBufferStr.toString();
                fStrChunks.clear();
                fBufferStr.setLength(0);
                return value;
            }
        }
        else if (type == Node.CDATA_SECTION_NODE) {
            // find if any other data stored in children
            int child = getLastChild(nodeIndex, false);
            if (child !=-1) {
                // append data that is stored in fNodeValue
                fBufferStr.append(value);
                while (child !=-1) {
                    // go in reverse order: find last child, then
                    // its previous sibling, etc
                   chunk = child >> CHUNK_SHIFT;
                    index = child & CHUNK_MASK;
                    value = getChunkValue(fNodeValue, chunk, index);
                    fStrChunks.add(value);
                    child = getChunkIndex(fNodePrevSib, chunk, index);
                }
                // add to the buffer in the correct order.
                for (int i=fStrChunks.size()-1; i>=0; i--) {
                     fBufferStr.append(fStrChunks.get(i));
                }

                value = fBufferStr.toString();
                fStrChunks.clear();
                fBufferStr.setLength(0);
                return value;
            }
        }

        return value;

    } // getNodeValueString(int,boolean):String

    /**
     * Returns the value of the given node.
     */
    public String getNodeValue(int nodeIndex) {
        return getNodeValue(nodeIndex, true);
    }

        /**
         * Clears the type info that is stored in the fNodeValue array
         * @param nodeIndex
         * @return Object - type information for the attribute/element node
         */
    public Object getTypeInfo(int nodeIndex) {
        if (nodeIndex == -1) {
            return null;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;


        Object value = fNodeValue[chunk] != null ? fNodeValue[chunk][index] : null;
        if (value != null) {
            fNodeValue[chunk][index] = null;
            RefCount c = (RefCount) fNodeValue[chunk][CHUNK_SIZE];
            c.fCount--;
            if (c.fCount == 0) {
                fNodeValue[chunk] = null;
            }
        }
        return value;
    }

    /**
     * Returns the value of the given node.
     * @param free True to free the value index.
     */
    public String getNodeValue(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return null;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkValue(fNodeValue, chunk, index)
                    : getChunkValue(fNodeValue, chunk, index);

    } // getNodeValue(int,boolean):String

    /**
     * Returns the extra info of the given node.
     * Used by AttrImpl to store specified value (1 == true).
     */
    public int getNodeExtra(int nodeIndex) {
        return getNodeExtra(nodeIndex, true);
    }

    /**
     * Returns the extra info of the given node.
     * @param free True to free the value index.
     */
    public int getNodeExtra(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return -1;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkIndex(fNodeExtra, chunk, index)
                    : getChunkIndex(fNodeExtra, chunk, index);

    } // getNodeExtra(int,boolean):int

    /** Returns the type of the given node. */
    public short getNodeType(int nodeIndex) {
        return getNodeType(nodeIndex, true);
    }

    /**
     * Returns the type of the given node.
     * @param free True to free type index.
     */
    public short getNodeType(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return -1;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        return free ? (short)clearChunkIndex(fNodeType, chunk, index)
                    : (short)getChunkIndex(fNodeType, chunk, index);

    } // getNodeType(int):int

    /** Returns the attribute value of the given name. */
    public String getAttribute(int elemIndex, String name) {
        if (elemIndex == -1 || name == null) {
            return null;
        }
        int echunk = elemIndex >> CHUNK_SHIFT;
        int eindex = elemIndex & CHUNK_MASK;
        int attrIndex = getChunkIndex(fNodeExtra, echunk, eindex);
        while (attrIndex != -1) {
            int achunk = attrIndex >> CHUNK_SHIFT;
            int aindex = attrIndex & CHUNK_MASK;
            if (getChunkValue(fNodeName, achunk, aindex) == name) {
                return getChunkValue(fNodeValue, achunk, aindex);
            }
            attrIndex = getChunkIndex(fNodePrevSib, achunk, aindex);
        }
        return null;
    }

    /** Returns the URI of the given node. */
    public String getNodeURI(int nodeIndex) {
        return getNodeURI(nodeIndex, true);
    }

    /**
     * Returns the URI of the given node.
     * @param free True to free URI index.
     */
    public String getNodeURI(int nodeIndex, boolean free) {

        if (nodeIndex == -1) {
            return null;
        }

        int chunk = nodeIndex >> CHUNK_SHIFT;
        int index = nodeIndex & CHUNK_MASK;
        return free ? clearChunkValue(fNodeURI, chunk, index)
                    : getChunkValue(fNodeURI, chunk, index);

    } // getNodeURI(int,int):String

    // identifier maintenance

    /** Registers an identifier name with a specified element node. */
    public void putIdentifier(String name, int elementNodeIndex) {

        if (DEBUG_IDS) {
            System.out.println("putIdentifier(" + name + ", "
                               + elementNodeIndex + ')' + " // " +
                               getChunkValue(fNodeName,
                                             elementNodeIndex >> CHUNK_SHIFT,
                                             elementNodeIndex & CHUNK_MASK));
        }

        // initialize arrays
        if (fIdName == null) {
            fIdName    = new String[64];
            fIdElement = new int[64];
        }

        // resize arrays
        if (fIdCount == fIdName.length) {
            String idName[] = new String[fIdCount * 2];
            System.arraycopy(fIdName, 0, idName, 0, fIdCount);
            fIdName = idName;

            int idElement[] = new int[idName.length];
            System.arraycopy(fIdElement, 0, idElement, 0, fIdCount);
            fIdElement = idElement;
        }

        // store identifier
        fIdName[fIdCount] = name;
        fIdElement[fIdCount] = elementNodeIndex;
        fIdCount++;

    } // putIdentifier(String,int)

    //
    // DEBUG
    //

    /** Prints out the tables. */
    public void print() {

        if (DEBUG_PRINT_REF_COUNTS) {
            System.out.print("num\t");
            System.out.print("type\t");
            System.out.print("name\t");
            System.out.print("val\t");
            System.out.print("par\t");
            System.out.print("lch\t");
            System.out.print("psib");
            System.out.println();
            for (int i = 0; i < fNodeType.length; i++) {
                if (fNodeType[i] != null) {
                    // separator
                    System.out.print("--------");
                    System.out.print("--------");
                    System.out.print("--------");
                    System.out.print("--------");
                    System.out.print("--------");
                    System.out.print("--------");
                    System.out.print("--------");
                    System.out.println();

                    // ref count
                    System.out.print(i);
                    System.out.print('\t');
                    switch (fNodeType[i][CHUNK_SIZE]) {
                        case DocumentImpl.ELEMENT_DEFINITION_NODE: { System.out.print("EDef"); break; }
                        case Node.DOCUMENT_NODE: { System.out.print("Doc"); break; }
                        case Node.DOCUMENT_TYPE_NODE: { System.out.print("DType"); break; }
                        case Node.COMMENT_NODE: { System.out.print("Com"); break; }
                        case Node.PROCESSING_INSTRUCTION_NODE: { System.out.print("PI"); break; }
                        case Node.ELEMENT_NODE: { System.out.print("Elem"); break; }
                        case Node.ENTITY_NODE: { System.out.print("Ent"); break; }
                        case Node.ENTITY_REFERENCE_NODE: { System.out.print("ERef"); break; }
                        case Node.TEXT_NODE: { System.out.print("Text"); break; }
                        case Node.ATTRIBUTE_NODE: { System.out.print("Attr"); break; }
                        case DeferredNode.TYPE_NODE: { System.out.print("TypeInfo"); break; }
                        default: { System.out.print("?"+fNodeType[i][CHUNK_SIZE]); }
                    }
                    System.out.print('\t');
                    System.out.print(fNodeName[i][CHUNK_SIZE]);
                    System.out.print('\t');
                    System.out.print(fNodeValue[i][CHUNK_SIZE]);
                    System.out.print('\t');
                    System.out.print(fNodeURI[i][CHUNK_SIZE]);
                    System.out.print('\t');
                    System.out.print(fNodeParent[i][CHUNK_SIZE]);
                    System.out.print('\t');
                    System.out.print(fNodeLastChild[i][CHUNK_SIZE]);
                    System.out.print('\t');
                    System.out.print(fNodePrevSib[i][CHUNK_SIZE]);
                    System.out.print('\t');
                    System.out.print(fNodeExtra[i][CHUNK_SIZE]);
                    System.out.println();
                }
            }
        }

        if (DEBUG_PRINT_TABLES) {
            // This assumes that the document is small
            System.out.println("# start table");
            for (int i = 0; i < fNodeCount; i++) {
                int chunk = i >> CHUNK_SHIFT;
                int index = i & CHUNK_MASK;
                if (i % 10 == 0) {
                    System.out.print("num\t");
                    System.out.print("type\t");
                    System.out.print("name\t");
                    System.out.print("val\t");
                    System.out.print("uri\t");
                    System.out.print("par\t");
                    System.out.print("lch\t");
                    System.out.print("psib\t");
                    System.out.print("xtra");
                    System.out.println();
                }
                System.out.print(i);
                System.out.print('\t');
                switch (getChunkIndex(fNodeType, chunk, index)) {
                    case DocumentImpl.ELEMENT_DEFINITION_NODE: { System.out.print("EDef"); break; }
                    case Node.DOCUMENT_NODE: { System.out.print("Doc"); break; }
                    case Node.DOCUMENT_TYPE_NODE: { System.out.print("DType"); break; }
                    case Node.COMMENT_NODE: { System.out.print("Com"); break; }
                    case Node.PROCESSING_INSTRUCTION_NODE: { System.out.print("PI"); break; }
                    case Node.ELEMENT_NODE: { System.out.print("Elem"); break; }
                    case Node.ENTITY_NODE: { System.out.print("Ent"); break; }
                    case Node.ENTITY_REFERENCE_NODE: { System.out.print("ERef"); break; }
                    case Node.TEXT_NODE: { System.out.print("Text"); break; }
                    case Node.ATTRIBUTE_NODE: { System.out.print("Attr"); break; }
                    case DeferredNode.TYPE_NODE: { System.out.print("TypeInfo"); break; }
                    default: { System.out.print("?"+getChunkIndex(fNodeType, chunk, index)); }
                }
                System.out.print('\t');
                System.out.print(getChunkValue(fNodeName, chunk, index));
                System.out.print('\t');
                System.out.print(getNodeValue(chunk, index));
                System.out.print('\t');
                System.out.print(getChunkValue(fNodeURI, chunk, index));
                System.out.print('\t');
                System.out.print(getChunkIndex(fNodeParent, chunk, index));
                System.out.print('\t');
                System.out.print(getChunkIndex(fNodeLastChild, chunk, index));
                System.out.print('\t');
                System.out.print(getChunkIndex(fNodePrevSib, chunk, index));
                System.out.print('\t');
                System.out.print(getChunkIndex(fNodeExtra, chunk, index));
                System.out.println();
            }
            System.out.println("# end table");
        }

    } // print()

    //
    // DeferredNode methods
    //

    /** Returns the node index. */
    public int getNodeIndex() {
        return 0;
    }

    //
    // Protected methods
    //

    /** Synchronizes the node's data. */
    protected void synchronizeData() {

        // no need to sync in the future
        needsSyncData(false);

        // fluff up enough nodes to fill identifiers hash
        if (fIdElement != null) {

            // REVISIT: There has to be a more efficient way of
            //          doing this. But keep in mind that the
            //          tree can have been altered and re-ordered
            //          before all of the element nodes with ID
            //          attributes have been registered. For now
            //          this is reasonable and safe. -Ac

            IntVector path = new IntVector();
            for (int i = 0; i < fIdCount; i++) {

                // ignore if it's already been registered
                int elementNodeIndex = fIdElement[i];
                String idName      = fIdName[i];
                if (idName == null) {
                    continue;
                }

                // find path from this element to the root
                path.removeAllElements();
                int index = elementNodeIndex;
                do {
                    path.addElement(index);
                    int pchunk = index >> CHUNK_SHIFT;
                    int pindex = index & CHUNK_MASK;
                    index = getChunkIndex(fNodeParent, pchunk, pindex);
                } while (index != -1);

                // Traverse path (backwards), fluffing the elements
                // along the way. When this loop finishes, "place"
                // will contain the reference to the element node
                // we're interested in. -Ac
                Node place = this;
                for (int j = path.size() - 2; j >= 0; j--) {
                    index = path.elementAt(j);
                    Node child = place.getLastChild();
                    while (child != null) {
                        if (child instanceof DeferredNode) {
                            int nodeIndex =
                                ((DeferredNode)child).getNodeIndex();
                            if (nodeIndex == index) {
                                place = child;
                                break;
                            }
                        }
                        child = child.getPreviousSibling();
                    }
                }

                // register the element
                Element element = (Element)place;
                putIdentifier0(idName, element);
                fIdName[i] = null;

                // see if there are more IDs on this element
                while (i + 1 < fIdCount &&
                    fIdElement[i + 1] == elementNodeIndex) {
                    idName = fIdName[++i];
                    if (idName == null) {
                        continue;
                    }
                    putIdentifier0(idName, element);
                }
            }

        } // if identifiers

    } // synchronizeData()

    /**
     * Synchronizes the node's children with the internal structure.
     * Fluffing the children at once solves a lot of work to keep
     * the two structures in sync. The problem gets worse when
     * editing the tree -- this makes it a lot easier.
     */
    protected void synchronizeChildren() {

        if (needsSyncData()) {
            synchronizeData();
            /*
             * when we have elements with IDs this method is being recursively
             * called from synchronizeData, in which case we've already gone
             * through the following and we can now simply stop here.
             */
            if (!needsSyncChildren()) {
                return;
            }
        }

        // we don't want to generate any event for this so turn them off
        boolean orig = mutationEvents;
        mutationEvents = false;

        // no need to sync in the future
        needsSyncChildren(false);

        getNodeType(0);

        // create children and link them as siblings
        ChildNode first = null;
        ChildNode last = null;
        for (int index = getLastChild(0);
             index != -1;
             index = getPrevSibling(index)) {

            ChildNode node = (ChildNode)getNodeObject(index);
            if (last == null) {
                last = node;
            }
            else {
                first.previousSibling = node;
            }
            node.ownerNode = this;
            node.isOwned(true);
            node.nextSibling = first;
            first = node;

            // save doctype and document type
            int type = node.getNodeType();
            if (type == Node.ELEMENT_NODE) {
                docElement = (ElementImpl)node;
            }
            else if (type == Node.DOCUMENT_TYPE_NODE) {
                docType = (DocumentTypeImpl)node;
            }
        }

        if (first != null) {
            firstChild = first;
            first.isFirstChild(true);
            lastChild(last);
        }

        // set mutation events flag back to its original value
        mutationEvents = orig;

    } // synchronizeChildren()

    /**
     * Synchronizes the node's children with the internal structure.
     * Fluffing the children at once solves a lot of work to keep
     * the two structures in sync. The problem gets worse when
     * editing the tree -- this makes it a lot easier.
     * This is not directly used in this class but this method is
     * here so that it can be shared by all deferred subclasses of AttrImpl.
     */
    protected final void synchronizeChildren(AttrImpl a, int nodeIndex) {

        // we don't want to generate any event for this so turn them off
        boolean orig = getMutationEvents();
        setMutationEvents(false);

        // no need to sync in the future
        a.needsSyncChildren(false);

        // create children and link them as siblings or simply store the value
        // as a String if all we have is one piece of text
        int last = getLastChild(nodeIndex);
        int prev = getPrevSibling(last);
        if (prev == -1) {
            a.value = getNodeValueString(nodeIndex);
            a.hasStringValue(true);
        }
        else {
            ChildNode firstNode = null;
            ChildNode lastNode = null;
            for (int index = last; index != -1;
                 index = getPrevSibling(index)) {

                ChildNode node = (ChildNode) getNodeObject(index);
                if (lastNode == null) {
                    lastNode = node;
                }
                else {
                    firstNode.previousSibling = node;
                }
                node.ownerNode = a;
                node.isOwned(true);
                node.nextSibling = firstNode;
                firstNode = node;
            }
            if (lastNode != null) {
                a.value = firstNode; // firstChild = firstNode
                firstNode.isFirstChild(true);
                a.lastChild(lastNode);
            }
            a.hasStringValue(false);
        }

        // set mutation events flag back to its original value
        setMutationEvents(orig);

    } // synchronizeChildren(AttrImpl,int):void


    /**
     * Synchronizes the node's children with the internal structure.
     * Fluffing the children at once solves a lot of work to keep
     * the two structures in sync. The problem gets worse when
     * editing the tree -- this makes it a lot easier.
     * This is not directly used in this class but this method is
     * here so that it can be shared by all deferred subclasses of ParentNode.
     */
    protected final void synchronizeChildren(ParentNode p, int nodeIndex) {

        // we don't want to generate any event for this so turn them off
        boolean orig = getMutationEvents();
        setMutationEvents(false);

        // no need to sync in the future
        p.needsSyncChildren(false);

        // create children and link them as siblings
        ChildNode firstNode = null;
        ChildNode lastNode = null;
        for (int index = getLastChild(nodeIndex);
             index != -1;
             index = getPrevSibling(index)) {

            ChildNode node = (ChildNode) getNodeObject(index);
            if (lastNode == null) {
                lastNode = node;
            }
            else {
                firstNode.previousSibling = node;
            }
            node.ownerNode = p;
            node.isOwned(true);
            node.nextSibling = firstNode;
            firstNode = node;
        }
        if (lastNode != null) {
            p.firstChild = firstNode;
            firstNode.isFirstChild(true);
            p.lastChild(lastNode);
        }

        // set mutation events flag back to its original value
        setMutationEvents(orig);

    } // synchronizeChildren(ParentNode,int):void

    // utility methods

    /** Ensures that the internal tables are large enough. */
    protected void ensureCapacity(int chunk) {
        if (fNodeType == null) {
            // create buffers
            fNodeType       = new int[INITIAL_CHUNK_COUNT][];
            fNodeName       = new Object[INITIAL_CHUNK_COUNT][];
            fNodeValue      = new Object[INITIAL_CHUNK_COUNT][];
            fNodeParent     = new int[INITIAL_CHUNK_COUNT][];
            fNodeLastChild  = new int[INITIAL_CHUNK_COUNT][];
            fNodePrevSib    = new int[INITIAL_CHUNK_COUNT][];
            fNodeURI        = new Object[INITIAL_CHUNK_COUNT][];
            fNodeExtra      = new int[INITIAL_CHUNK_COUNT][];
        }
        else if (fNodeType.length <= chunk) {
            // resize the tables
            int newsize = chunk * 2;

            int[][] newArray = new int[newsize][];
            System.arraycopy(fNodeType, 0, newArray, 0, chunk);
            fNodeType = newArray;

            Object[][] newStrArray = new Object[newsize][];
            System.arraycopy(fNodeName, 0, newStrArray, 0, chunk);
            fNodeName = newStrArray;

            newStrArray = new Object[newsize][];
            System.arraycopy(fNodeValue, 0, newStrArray, 0, chunk);
            fNodeValue = newStrArray;

            newArray = new int[newsize][];
            System.arraycopy(fNodeParent, 0, newArray, 0, chunk);
            fNodeParent = newArray;

            newArray = new int[newsize][];
            System.arraycopy(fNodeLastChild, 0, newArray, 0, chunk);
            fNodeLastChild = newArray;

            newArray = new int[newsize][];
            System.arraycopy(fNodePrevSib, 0, newArray, 0, chunk);
            fNodePrevSib = newArray;

            newStrArray = new Object[newsize][];
            System.arraycopy(fNodeURI, 0, newStrArray, 0, chunk);
            fNodeURI = newStrArray;

            newArray = new int[newsize][];
            System.arraycopy(fNodeExtra, 0, newArray, 0, chunk);
            fNodeExtra = newArray;
        }
        else if (fNodeType[chunk] != null) {
            // Done - there's sufficient capacity
            return;
        }

        // create new chunks
        createChunk(fNodeType, chunk);
        createChunk(fNodeName, chunk);
        createChunk(fNodeValue, chunk);
        createChunk(fNodeParent, chunk);
        createChunk(fNodeLastChild, chunk);
        createChunk(fNodePrevSib, chunk);
        createChunk(fNodeURI, chunk);
        createChunk(fNodeExtra, chunk);

        // Done
        return;

    } // ensureCapacity(int,int)

    /** Creates a node of the specified type. */
    protected int createNode(short nodeType) {
        // ensure tables are large enough
        int chunk = fNodeCount >> CHUNK_SHIFT;
        int index = fNodeCount & CHUNK_MASK;
        ensureCapacity(chunk);

        // initialize node
        setChunkIndex(fNodeType, nodeType, chunk, index);

        // return node index number
        return fNodeCount++;

    } // createNode(short):int

    /**
     * Performs a binary search for a target value in an array of
     * values. The array of values must be in ascending sorted order
     * before calling this method and all array values must be
     * non-negative.
     *
     * @param values  The array of values to search.
     * @param start   The starting offset of the search.
     * @param end     The ending offset of the search.
     * @param target  The target value.
     *
     * @return This function will return the <i>first</i> occurrence
     *         of the target value, or -1 if the target value cannot
     *         be found.
     */
    protected static int binarySearch(final int values[],
                                      int start, int end, int target) {

        if (DEBUG_IDS) {
            System.out.println("binarySearch(), target: "+target);
        }

        // look for target value
        while (start <= end) {

            // is this the one we're looking for?
            int middle = (start + end) >>> 1;
            int value  = values[middle];
            if (DEBUG_IDS) {
                System.out.print("  value: "+value+", target: "+target+" // ");
                print(values, start, end, middle, target);
            }
            if (value == target) {
                while (middle > 0 && values[middle - 1] == target) {
                    middle--;
                }
                if (DEBUG_IDS) {
                    System.out.println("FOUND AT "+middle);
                }
                return middle;
            }

            // is this point higher or lower?
            if (value > target) {
                end = middle - 1;
            }
            else {
                start = middle + 1;
            }

        } // while

        // not found
        if (DEBUG_IDS) {
            System.out.println("NOT FOUND!");
        }
        return -1;

    } // binarySearch(int[],int,int,int):int

    //
    // Private methods
    //
    private static final int[] INIT_ARRAY = new int[CHUNK_SIZE + 1];
    static {
        for (int i = 0; i < CHUNK_SIZE; i++) {
            INIT_ARRAY[i] = -1;
        }
    }
    /** Creates the specified chunk in the given array of chunks. */
    private final void createChunk(int data[][], int chunk) {
        data[chunk] = new int[CHUNK_SIZE + 1];
        System.arraycopy(INIT_ARRAY, 0, data[chunk], 0, CHUNK_SIZE);
    }

    static final class RefCount {
        int fCount;
    }

    private final void createChunk(Object data[][], int chunk) {
        data[chunk] = new Object[CHUNK_SIZE + 1];
        data[chunk][CHUNK_SIZE] = new RefCount();
    }

    /**
     * Sets the specified value in the given of data at the chunk and index.
     *
     * @return Returns the old value.
     */
    private final int setChunkIndex(int data[][], int value,
                                    int chunk, int index) {
        if (value == -1) {
            return clearChunkIndex(data, chunk, index);
        }
        int [] dataChunk = data[chunk];
        // Re-create chunk if it was deleted.
        if (dataChunk == null) {
            createChunk(data, chunk);
            dataChunk = data[chunk];
        }
        int ovalue = dataChunk[index];
        if (ovalue == -1) {
            dataChunk[CHUNK_SIZE]++;
        }
        dataChunk[index] = value;
        return ovalue;
    }
    private final String setChunkValue(Object data[][], Object value,
                                       int chunk, int index) {
        if (value == null) {
            return clearChunkValue(data, chunk, index);
        }
        Object [] dataChunk = data[chunk];
        // Re-create chunk if it was deleted.
        if (dataChunk == null) {
            createChunk(data, chunk);
            dataChunk = data[chunk];
        }
        String ovalue = (String) dataChunk[index];
        if (ovalue == null) {
            RefCount c = (RefCount) dataChunk[CHUNK_SIZE];
            c.fCount++;
        }
        dataChunk[index] = value;
        return ovalue;
    }

    /**
     * Returns the specified value in the given data at the chunk and index.
     */
    private final int getChunkIndex(int data[][], int chunk, int index) {
        return data[chunk] != null ? data[chunk][index] : -1;
    }
    private final String getChunkValue(Object data[][], int chunk, int index) {
        return data[chunk] != null ? (String) data[chunk][index] : null;
    }
    private final String getNodeValue(int chunk, int index) {
        Object data = fNodeValue[chunk][index];
        if (data == null){
            return null;
        }
        else if (data instanceof String){
            return (String)data;
        }
        else {
            // type information
            return data.toString();
        }
    }


    /**
     * Clears the specified value in the given data at the chunk and index.
     * Note that this method will clear the given chunk if the reference
     * count becomes zero.
     *
     * @return Returns the old value.
     */
    private final int clearChunkIndex(int data[][], int chunk, int index) {
        int value = data[chunk] != null ? data[chunk][index] : -1;
        if (value != -1) {
            data[chunk][CHUNK_SIZE]--;
            data[chunk][index] = -1;
            if (data[chunk][CHUNK_SIZE] == 0) {
                data[chunk] = null;
            }
        }
        return value;
    }
    private final String clearChunkValue(Object data[][],
                                         int chunk, int index) {
        String value = data[chunk] != null ? (String)data[chunk][index] : null;
        if (value != null) {
            data[chunk][index] = null;
            RefCount c = (RefCount) data[chunk][CHUNK_SIZE];
            c.fCount--;
            if (c.fCount == 0) {
                data[chunk] = null;
            }
        }
        return value;
    }

    /**
     * This version of putIdentifier is needed to avoid fluffing
     * all of the paths to ID attributes when a node object is
     * created that contains an ID attribute.
     */
    private final void putIdentifier0(String idName, Element element) {

        if (DEBUG_IDS) {
            System.out.println("putIdentifier0("+
                               idName+", "+
                               element+')');
        }

        // create Map
        if (identifiers == null) {
            identifiers = new HashMap<>();
        }

        // save ID and its associated element
        identifiers.put(idName, element);

    } // putIdentifier0(String,Element)

    /** Prints the ID array. */
    private static void print(int values[], int start, int end,
                              int middle, int target) {

        if (DEBUG_IDS) {
            System.out.print(start);
            System.out.print(" [");
            for (int i = start; i < end; i++) {
                if (middle == i) {
                    System.out.print("!");
                }
                System.out.print(values[i]);
                if (values[i] == target) {
                    System.out.print("*");
                }
                if (i < end - 1) {
                    System.out.print(" ");
                }
            }
            System.out.println("] "+end);
        }

    } // print(int[],int,int,int,int)

    //
    // Classes
    //

    /**
     * A simple integer vector.
     */
    static final class IntVector {

        //
        // Data
        //

        /** Data. */
        private int data[];

        /** Size. */
        private int size;

        //
        // Public methods
        //

        /** Returns the length of this vector. */
        public int size() {
            return size;
        }

        /** Returns the element at the specified index. */
        public int elementAt(int index) {
            return data[index];
        }

        /** Appends an element to the end of the vector. */
        public void addElement(int element) {
            ensureCapacity(size + 1);
            data[size++] = element;
        }

        /** Clears the vector. */
        public void removeAllElements() {
            size = 0;
        }

        //
        // Private methods
        //

        /** Makes sure that there is enough storage. */
        private void ensureCapacity(int newsize) {

            if (data == null) {
                data = new int[newsize + 15];
            }
            else if (newsize > data.length) {
                int newdata[] = new int[newsize + 15];
                System.arraycopy(data, 0, newdata, 0, data.length);
                data = newdata;
            }

        } // ensureCapacity(int)

    } // class IntVector

} // class DeferredDocumentImpl
