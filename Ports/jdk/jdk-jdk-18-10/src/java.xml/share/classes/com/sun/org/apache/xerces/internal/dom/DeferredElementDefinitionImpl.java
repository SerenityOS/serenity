/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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

import org.w3c.dom.Node;

/**
 * NON-DOM CLASS: Describe one of the Elements (and its associated
 * Attributes) defined in this Document Type.
 * <p>
 * I've included this in Level 1 purely as an anchor point for default
 * attributes. In Level 2 it should enable the ChildRule support.
 *
 * @xerces.internal
 *
 */
public class DeferredElementDefinitionImpl
    extends ElementDefinitionImpl
    implements DeferredNode {

    //
    // Constants
    //

    /** Serialization version. */
    static final long serialVersionUID = 6703238199538041591L;

    //
    // Data
    //

    /** Node index. */
    protected transient int fNodeIndex;

    //
    // Constructors
    //

    /**
     * This is the deferred constructor. Only the fNodeIndex is given here.
     * All other data, can be requested from the ownerDocument via the index.
     */
    DeferredElementDefinitionImpl(DeferredDocumentImpl ownerDocument,
                                  int nodeIndex) {
        super(ownerDocument, null);

        fNodeIndex = nodeIndex;
        needsSyncData(true);
        needsSyncChildren(true);

    } // <init>(DeferredDocumentImpl,int)

    //
    // DeferredNode methods
    //

    /** Returns the node index. */
    public int getNodeIndex() {
        return fNodeIndex;
    }

    //
    // Protected methods
    //

    /** Synchronizes the data (name and value) for fast nodes. */
    protected void synchronizeData() {

        // no need to sync in the future
        needsSyncData(false);

        // fluff data
        DeferredDocumentImpl ownerDocument =
            (DeferredDocumentImpl)this.ownerDocument;
        name = ownerDocument.getNodeName(fNodeIndex);

    } // synchronizeData()

    /** Synchronizes the default attribute values. */
    protected void synchronizeChildren() {

        // we don't want to generate any event for this so turn them off
        boolean orig = ownerDocument.getMutationEvents();
        ownerDocument.setMutationEvents(false);

        // attributes are now synced
        needsSyncChildren(false);

        // create attributes node map
        DeferredDocumentImpl ownerDocument =
            (DeferredDocumentImpl)this.ownerDocument;
        attributes = new NamedNodeMapImpl(ownerDocument);

        // Default attributes dangle as children of the element
        // definition "node" in the internal fast table.
        for (int nodeIndex = ownerDocument.getLastChild(fNodeIndex);
             nodeIndex != -1;
             nodeIndex = ownerDocument.getPrevSibling(nodeIndex)) {
            Node attr = ownerDocument.getNodeObject(nodeIndex);
            attributes.setNamedItem(attr);
        }

        // set mutation events flag back to its original value
        ownerDocument.setMutationEvents(orig);

    } // synchronizeChildren()

} // class DeferredElementDefinitionImpl
