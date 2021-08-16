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
import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.ranges.Range;
import org.w3c.dom.ranges.RangeException;


/** The RangeImpl class implements the org.w3c.dom.range.Range interface.
 *  <p> Please see the API documentation for the interface classes
 *  and use the interfaces in your client programs.
 *
 * @xerces.internal
 *
 * @LastModified: Oct 2017
 */
public class RangeImpl  implements Range {

    //
    // Constants
    //


    //
    // Data
    //

    DocumentImpl fDocument;
    Node fStartContainer;
    Node fEndContainer;
    int fStartOffset;
    int fEndOffset;
    boolean fIsCollapsed;
    boolean fDetach = false;
    Node fInsertNode = null;
    Node fDeleteNode = null;
    Node fSplitNode = null;
    // Was the Node inserted from the Range or the Document
    boolean fInsertedFromRange = false;

    /** The constructor. Clients must use DocumentRange.createRange(),
     *  because it registers the Range with the document, so it can
     *  be fixed-up.
     */
    public RangeImpl(DocumentImpl document) {
        fDocument = document;
        fStartContainer = document;
        fEndContainer = document;
        fStartOffset = 0;
        fEndOffset = 0;
        fDetach = false;
    }

    public Node getStartContainer() {
        if ( fDetach ) {
            throw new DOMException(
                DOMException.INVALID_STATE_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        return fStartContainer;
    }

    public int getStartOffset() {
        if ( fDetach ) {
            throw new DOMException(
                DOMException.INVALID_STATE_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        return fStartOffset;
    }

    public Node getEndContainer() {
        if ( fDetach ) {
            throw new DOMException(
                DOMException.INVALID_STATE_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        return fEndContainer;
    }

    public int getEndOffset() {
        if ( fDetach ) {
            throw new DOMException(
                DOMException.INVALID_STATE_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        return fEndOffset;
    }

    public boolean getCollapsed() {
        if ( fDetach ) {
            throw new DOMException(
                DOMException.INVALID_STATE_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        return (fStartContainer == fEndContainer
             && fStartOffset == fEndOffset);
    }

    public Node getCommonAncestorContainer() {
        if ( fDetach ) {
            throw new DOMException(
                DOMException.INVALID_STATE_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        List<Node> startV = new ArrayList<>();
        Node node;
        for (node=fStartContainer; node != null;
             node=node.getParentNode())
        {
            startV.add(node);
        }
        List<Node> endV = new ArrayList<>();
        for (node=fEndContainer; node != null;
             node=node.getParentNode())
        {
            endV.add(node);
        }
        int s = startV.size()-1;
        int e = endV.size()-1;
        Node result = null;
        while (s>=0 && e>=0) {
            if (startV.get(s) == endV.get(e)) {
                result = startV.get(s);
            } else {
                break;
            }
            --s;
            --e;
        }
        return result;
    }


    public void setStart(Node refNode, int offset)
                         throws RangeException, DOMException
    {
        if (fDocument.errorChecking) {
            if ( fDetach) {
                throw new DOMException(
                        DOMException.INVALID_STATE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if ( !isLegalContainer(refNode)) {
                throw new RangeExceptionImpl(
                        RangeException.INVALID_NODE_TYPE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
            if ( fDocument != refNode.getOwnerDocument() && fDocument != refNode) {
                throw new DOMException(
                        DOMException.WRONG_DOCUMENT_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }

        checkIndex(refNode, offset);

        fStartContainer = refNode;
        fStartOffset = offset;

        // If one boundary-point of a Range is set to have a root container
        // other
        // than the current one for the Range, the Range should be collapsed to
        // the new position.
        // The start position of a Range should never be after the end position.
        if (getCommonAncestorContainer() == null
                || (fStartContainer == fEndContainer && fEndOffset < fStartOffset)) {
            collapse(true);
        }
    }

    public void setEnd(Node refNode, int offset)
                       throws RangeException, DOMException
    {
        if (fDocument.errorChecking) {
            if (fDetach) {
                throw new DOMException(
                        DOMException.INVALID_STATE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if ( !isLegalContainer(refNode)) {
                throw new RangeExceptionImpl(
                        RangeException.INVALID_NODE_TYPE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
            if ( fDocument != refNode.getOwnerDocument() && fDocument != refNode) {
                throw new DOMException(
                        DOMException.WRONG_DOCUMENT_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }

        checkIndex(refNode, offset);

        fEndContainer = refNode;
        fEndOffset = offset;

        // If one boundary-point of a Range is set to have a root container
        // other
        // than the current one for the Range, the Range should be collapsed to
        // the new position.
        // The start position of a Range should never be after the end position.
        if (getCommonAncestorContainer() == null
                || (fStartContainer == fEndContainer && fEndOffset < fStartOffset)) {
            collapse(false);
        }
    }

    public void setStartBefore(Node refNode)
        throws RangeException
    {
        if (fDocument.errorChecking) {
            if (fDetach) {
                throw new DOMException(
                        DOMException.INVALID_STATE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if ( !hasLegalRootContainer(refNode) ||
                    !isLegalContainedNode(refNode) )
            {
                throw new RangeExceptionImpl(
                        RangeException.INVALID_NODE_TYPE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
            if ( fDocument != refNode.getOwnerDocument() && fDocument != refNode) {
                throw new DOMException(
                        DOMException.WRONG_DOCUMENT_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }

        fStartContainer = refNode.getParentNode();
        int i = 0;
        for (Node n = refNode; n!=null; n = n.getPreviousSibling()) {
            i++;
        }
        fStartOffset = i-1;

        // If one boundary-point of a Range is set to have a root container
        // other
        // than the current one for the Range, the Range should be collapsed to
        // the new position.
        // The start position of a Range should never be after the end position.
        if (getCommonAncestorContainer() == null
                || (fStartContainer == fEndContainer && fEndOffset < fStartOffset)) {
            collapse(true);
        }
    }

    public void setStartAfter(Node refNode)
        throws RangeException
    {
        if (fDocument.errorChecking) {
            if (fDetach) {
                throw new DOMException(
                        DOMException.INVALID_STATE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if ( !hasLegalRootContainer(refNode) ||
                    !isLegalContainedNode(refNode)) {
                throw new RangeExceptionImpl(
                        RangeException.INVALID_NODE_TYPE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
            if ( fDocument != refNode.getOwnerDocument() && fDocument != refNode) {
                throw new DOMException(
                        DOMException.WRONG_DOCUMENT_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }
        fStartContainer = refNode.getParentNode();
        int i = 0;
        for (Node n = refNode; n!=null; n = n.getPreviousSibling()) {
            i++;
        }
        fStartOffset = i;

        // If one boundary-point of a Range is set to have a root container
        // other
        // than the current one for the Range, the Range should be collapsed to
        // the new position.
        // The start position of a Range should never be after the end position.
        if (getCommonAncestorContainer() == null
                || (fStartContainer == fEndContainer && fEndOffset < fStartOffset)) {
            collapse(true);
        }
    }

    public void setEndBefore(Node refNode)
        throws RangeException
    {
        if (fDocument.errorChecking) {
            if (fDetach) {
                throw new DOMException(
                        DOMException.INVALID_STATE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if ( !hasLegalRootContainer(refNode) ||
                    !isLegalContainedNode(refNode)) {
                throw new RangeExceptionImpl(
                        RangeException.INVALID_NODE_TYPE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
            if ( fDocument != refNode.getOwnerDocument() && fDocument != refNode) {
                throw new DOMException(
                        DOMException.WRONG_DOCUMENT_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }
        fEndContainer = refNode.getParentNode();
        int i = 0;
        for (Node n = refNode; n!=null; n = n.getPreviousSibling()) {
            i++;
        }
        fEndOffset = i-1;

        // If one boundary-point of a Range is set to have a root container
        // other
        // than the current one for the Range, the Range should be collapsed to
        // the new position.
        // The start position of a Range should never be after the end position.
        if (getCommonAncestorContainer() == null
                || (fStartContainer == fEndContainer && fEndOffset < fStartOffset)) {
            collapse(false);
        }
    }

    public void setEndAfter(Node refNode)
        throws RangeException
    {
        if (fDocument.errorChecking) {
            if( fDetach) {
                throw new DOMException(
                        DOMException.INVALID_STATE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if ( !hasLegalRootContainer(refNode) ||
                    !isLegalContainedNode(refNode)) {
                throw new RangeExceptionImpl(
                        RangeException.INVALID_NODE_TYPE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
            if ( fDocument != refNode.getOwnerDocument() && fDocument != refNode) {
                throw new DOMException(
                        DOMException.WRONG_DOCUMENT_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }
        fEndContainer = refNode.getParentNode();
        int i = 0;
        for (Node n = refNode; n!=null; n = n.getPreviousSibling()) {
            i++;
        }
        fEndOffset = i;

        // If one boundary-point of a Range is set to have a root container
        // other
        // than the current one for the Range, the Range should be collapsed to
        // the new position.
        // The start position of a Range should never be after the end position.
        if (getCommonAncestorContainer() == null
                || (fStartContainer == fEndContainer && fEndOffset < fStartOffset)) {
            collapse(false);
        }
    }

    public void collapse(boolean toStart) {

        if( fDetach) {
                throw new DOMException(
                DOMException.INVALID_STATE_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }

        if (toStart) {
            fEndContainer = fStartContainer;
            fEndOffset = fStartOffset;
        } else {
            fStartContainer = fEndContainer;
            fStartOffset = fEndOffset;
        }
    }

    public void selectNode(Node refNode)
        throws RangeException
    {
        if (fDocument.errorChecking) {
            if (fDetach) {
                throw new DOMException(
                        DOMException.INVALID_STATE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if ( !isLegalContainer( refNode.getParentNode() ) ||
                    !isLegalContainedNode( refNode ) ) {
                throw new RangeExceptionImpl(
                        RangeException.INVALID_NODE_TYPE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
            if ( fDocument != refNode.getOwnerDocument() && fDocument != refNode) {
                throw new DOMException(
                        DOMException.WRONG_DOCUMENT_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }
        Node parent = refNode.getParentNode();
        if (parent != null ) // REVIST: what to do if it IS null?
        {
            fStartContainer = parent;
            fEndContainer = parent;
            int i = 0;
            for (Node n = refNode; n!=null; n = n.getPreviousSibling()) {
                i++;
            }
            fStartOffset = i-1;
            fEndOffset = fStartOffset+1;
        }
    }

    public void selectNodeContents(Node refNode)
        throws RangeException
    {
        if (fDocument.errorChecking) {
            if( fDetach) {
                throw new DOMException(
                        DOMException.INVALID_STATE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if ( !isLegalContainer(refNode)) {
                throw new RangeExceptionImpl(
                        RangeException.INVALID_NODE_TYPE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
            if ( fDocument != refNode.getOwnerDocument() && fDocument != refNode) {
                throw new DOMException(
                        DOMException.WRONG_DOCUMENT_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }
        fStartContainer = refNode;
        fEndContainer = refNode;
        Node first = refNode.getFirstChild();
        fStartOffset = 0;
        if (first == null) {
            fEndOffset = 0;
        } else {
            int i = 0;
            for (Node n = first; n!=null; n = n.getNextSibling()) {
                i++;
            }
            fEndOffset = i;
        }

    }

    public short compareBoundaryPoints(short how, Range sourceRange)
        throws DOMException
    {
        if (fDocument.errorChecking) {
            if( fDetach) {
                throw new DOMException(
                        DOMException.INVALID_STATE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            // WRONG_DOCUMENT_ERR: Raised if the two Ranges are not in the same Document or DocumentFragment.
            if ((fDocument != sourceRange.getStartContainer().getOwnerDocument()
                    && fDocument != sourceRange.getStartContainer()
                    && sourceRange.getStartContainer() != null)
                    || (fDocument != sourceRange.getEndContainer().getOwnerDocument()
                            && fDocument != sourceRange.getEndContainer()
                            && sourceRange.getStartContainer() != null)) {
                throw new DOMException(DOMException.WRONG_DOCUMENT_ERR,
                        DOMMessageFormatter.formatMessage( DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }
        }

        Node endPointA;
        Node endPointB;
        int offsetA;
        int offsetB;

        if (how == START_TO_START) {
            endPointA = sourceRange.getStartContainer();
            endPointB = fStartContainer;
            offsetA = sourceRange.getStartOffset();
            offsetB = fStartOffset;
        } else
        if (how == START_TO_END) {
            endPointA = sourceRange.getStartContainer();
            endPointB = fEndContainer;
            offsetA = sourceRange.getStartOffset();
            offsetB = fEndOffset;
        } else
        if (how == END_TO_START) {
            endPointA = sourceRange.getEndContainer();
            endPointB = fStartContainer;
            offsetA = sourceRange.getEndOffset();
            offsetB = fStartOffset;
        } else {
            endPointA = sourceRange.getEndContainer();
            endPointB = fEndContainer;
            offsetA = sourceRange.getEndOffset();
            offsetB = fEndOffset;
        }

        // The DOM Spec outlines four cases that need to be tested
        // to compare two range boundary points:
        //   case 1: same container
        //   case 2: Child C of container A is ancestor of B
        //   case 3: Child C of container B is ancestor of A
        //   case 4: preorder traversal of context tree.

        // case 1: same container
        if (endPointA == endPointB) {
            if (offsetA < offsetB) return 1;
            if (offsetA == offsetB) return 0;
            return -1;
        }
        // case 2: Child C of container A is ancestor of B
        // This can be quickly tested by walking the parent chain of B
        for ( Node c = endPointB, p = c.getParentNode();
             p != null;
             c = p, p = p.getParentNode())
        {
            if (p == endPointA) {
                int index = indexOf(c, endPointA);
                if (offsetA <= index) return 1;
                return -1;
            }
        }

        // case 3: Child C of container B is ancestor of A
        // This can be quickly tested by walking the parent chain of A
        for ( Node c = endPointA, p = c.getParentNode();
             p != null;
             c = p, p = p.getParentNode())
        {
            if (p == endPointB) {
                int index = indexOf(c, endPointB);
                if (index < offsetB) return 1;
                return -1;
            }
        }

        // case 4: preorder traversal of context tree.
        // Instead of literally walking the context tree in pre-order,
        // we use relative node depth walking which is usually faster

        int depthDiff = 0;
        for ( Node n = endPointA; n != null; n = n.getParentNode() )
            depthDiff++;
        for ( Node n = endPointB; n != null; n = n.getParentNode() )
            depthDiff--;
        while (depthDiff > 0) {
            endPointA = endPointA.getParentNode();
            depthDiff--;
        }
        while (depthDiff < 0) {
            endPointB = endPointB.getParentNode();
            depthDiff++;
        }
        for (Node pA = endPointA.getParentNode(),
             pB = endPointB.getParentNode();
             pA != pB;
             pA = pA.getParentNode(), pB = pB.getParentNode() )
        {
            endPointA = pA;
            endPointB = pB;
        }
        for ( Node n = endPointA.getNextSibling();
             n != null;
             n = n.getNextSibling() )
        {
            if (n == endPointB) {
                return 1;
            }
        }
        return -1;
    }

    public void deleteContents()
        throws DOMException
    {
        traverseContents(DELETE_CONTENTS);
    }

    public DocumentFragment extractContents()
        throws DOMException
    {
        return traverseContents(EXTRACT_CONTENTS);
    }

    public DocumentFragment cloneContents()
        throws DOMException
    {
        return traverseContents(CLONE_CONTENTS);
    }

    public void insertNode(Node newNode)
        throws DOMException, RangeException
    {
        if ( newNode == null ) return; //throw exception?

        int type = newNode.getNodeType();

        if (fDocument.errorChecking) {
            if (fDetach) {
                throw new DOMException(
                        DOMException.INVALID_STATE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if ( fDocument != newNode.getOwnerDocument() ) {
                throw new DOMException(DOMException.WRONG_DOCUMENT_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null));
            }

            if (type == Node.ATTRIBUTE_NODE
                    || type == Node.ENTITY_NODE
                    || type == Node.NOTATION_NODE
                    || type == Node.DOCUMENT_NODE)
            {
                throw new RangeExceptionImpl(
                        RangeException.INVALID_NODE_TYPE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
        }
        Node cloneCurrent;
        Node current;
        int currentChildren = 0;
        fInsertedFromRange = true;

        //boolean MULTIPLE_MODE = false;
        if (fStartContainer.getNodeType() == Node.TEXT_NODE) {

            Node parent = fStartContainer.getParentNode();
            currentChildren = parent.getChildNodes().getLength(); //holds number of kids before insertion
            // split text node: results is 3 nodes..
            cloneCurrent = fStartContainer.cloneNode(false);
            ((TextImpl)cloneCurrent).setNodeValueInternal(
                    (cloneCurrent.getNodeValue()).substring(fStartOffset));
            ((TextImpl)fStartContainer).setNodeValueInternal(
                    (fStartContainer.getNodeValue()).substring(0,fStartOffset));
            Node next = fStartContainer.getNextSibling();
            if (next != null) {
                    if (parent !=  null) {
                        parent.insertBefore(newNode, next);
                        parent.insertBefore(cloneCurrent, next);
                    }
            } else {
                    if (parent != null) {
                        parent.appendChild(newNode);
                        parent.appendChild(cloneCurrent);
                    }
            }
             //update ranges after the insertion
             if ( fEndContainer == fStartContainer) {
                  fEndContainer = cloneCurrent; //endContainer is the new Node created
                  fEndOffset -= fStartOffset;
             }
             else if ( fEndContainer == parent ) {    //endContainer was not a text Node.
                  //endOffset + = number_of_children_added
                   fEndOffset += (parent.getChildNodes().getLength() - currentChildren);
             }

             // signal other Ranges to update their start/end containers/offsets
             signalSplitData(fStartContainer, cloneCurrent, fStartOffset);


        } else { // ! TEXT_NODE
            if ( fEndContainer == fStartContainer )      //need to remember number of kids
                currentChildren= fEndContainer.getChildNodes().getLength();

            current = fStartContainer.getFirstChild();
            int i = 0;
            for(i = 0; i < fStartOffset && current != null; i++) {
                current=current.getNextSibling();
            }
            if (current != null) {
                fStartContainer.insertBefore(newNode, current);
            } else {
                fStartContainer.appendChild(newNode);
            }
            //update fEndOffset. ex:<body><p/></body>. Range(start;end): body,0; body,1
            // insert <h1>: <body></h1><p/></body>. Range(start;end): body,0; body,2
            if ( fEndContainer == fStartContainer && fEndOffset != 0 ) {     //update fEndOffset if not 0
                fEndOffset += (fEndContainer.getChildNodes().getLength() - currentChildren);
            }
        }
        fInsertedFromRange = false;
    }

    public void surroundContents(Node newParent)
        throws DOMException, RangeException
    {
        if (newParent==null) return;
        int type = newParent.getNodeType();

        if (fDocument.errorChecking) {
            if (fDetach) {
                throw new DOMException(
                        DOMException.INVALID_STATE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
            }
            if (type == Node.ATTRIBUTE_NODE
                    || type == Node.ENTITY_NODE
                    || type == Node.NOTATION_NODE
                    || type == Node.DOCUMENT_TYPE_NODE
                    || type == Node.DOCUMENT_NODE
                    || type == Node.DOCUMENT_FRAGMENT_NODE)
            {
                throw new RangeExceptionImpl(
                        RangeException.INVALID_NODE_TYPE_ERR,
                        DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_NODE_TYPE_ERR", null));
            }
        }

        Node realStart = fStartContainer;
        Node realEnd = fEndContainer;
        if (fStartContainer.getNodeType() == Node.TEXT_NODE) {
            realStart = fStartContainer.getParentNode();
        }
        if (fEndContainer.getNodeType() == Node.TEXT_NODE) {
            realEnd = fEndContainer.getParentNode();
        }

        if (realStart != realEnd) {
                throw new RangeExceptionImpl(
                RangeException.BAD_BOUNDARYPOINTS_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "BAD_BOUNDARYPOINTS_ERR", null));
        }

        DocumentFragment frag = extractContents();
        insertNode(newParent);
        newParent.appendChild(frag);
        selectNode(newParent);
    }

    public Range cloneRange(){
        if( fDetach) {
                throw new DOMException(
                DOMException.INVALID_STATE_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }

        Range range = fDocument.createRange();
        range.setStart(fStartContainer, fStartOffset);
        range.setEnd(fEndContainer, fEndOffset);
        return range;
    }

    public String toString(){
        if( fDetach) {
                throw new DOMException(
                DOMException.INVALID_STATE_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }

        Node node = fStartContainer;
        Node stopNode = fEndContainer;
        StringBuffer sb = new StringBuffer();
        if (fStartContainer.getNodeType() == Node.TEXT_NODE
         || fStartContainer.getNodeType() == Node.CDATA_SECTION_NODE
        ) {
            if (fStartContainer == fEndContainer) {
                sb.append(fStartContainer.getNodeValue().substring(fStartOffset, fEndOffset));
                return sb.toString();
            }
            sb.append(fStartContainer.getNodeValue().substring(fStartOffset));
            node=nextNode (node,true); //fEndContainer!=fStartContainer

        }
        else {  //fStartContainer is not a TextNode
            node=node.getFirstChild();
            if (fStartOffset>0) { //find a first node within a range, specified by fStartOffset
               int counter=0;
               while (counter<fStartOffset && node!=null) {
                   node=node.getNextSibling();
                   counter++;
               }
            }
            if (node == null) {
                   node = nextNode(fStartContainer,false);
            }
        }
        if ( fEndContainer.getNodeType()!= Node.TEXT_NODE &&
             fEndContainer.getNodeType()!= Node.CDATA_SECTION_NODE ){
             int i=fEndOffset;
             stopNode = fEndContainer.getFirstChild();
             while( i>0 && stopNode!=null ){
                 --i;
                 stopNode = stopNode.getNextSibling();
             }
             if ( stopNode == null )
                 stopNode = nextNode( fEndContainer, false );
         }
         while (node != stopNode) {  //look into all kids of the Range
             if (node == null) break;
             if (node.getNodeType() == Node.TEXT_NODE
             ||  node.getNodeType() == Node.CDATA_SECTION_NODE) {
                 sb.append(node.getNodeValue());
             }

             node = nextNode(node, true);
         }

        if (fEndContainer.getNodeType() == Node.TEXT_NODE
         || fEndContainer.getNodeType() == Node.CDATA_SECTION_NODE) {
            sb.append(fEndContainer.getNodeValue().substring(0,fEndOffset));
        }
        return sb.toString();
    }

    public void detach() {
        if( fDetach) {
            throw new DOMException(
            DOMException.INVALID_STATE_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }
        fDetach = true;
        fDocument.removeRange(this);
    }

    //
    // Mutation functions
    //

    /** Signal other Ranges to update their start/end
     *  containers/offsets. The data has already been split
     *  into the two Nodes.
     */
    void signalSplitData(Node node, Node newNode, int offset) {
        fSplitNode = node;
        // notify document
        fDocument.splitData(node, newNode, offset);
        fSplitNode = null;
    }

    /** Fix up this Range if another Range has split a Text Node
     *  into 2 Nodes.
     */
    void receiveSplitData(Node node, Node newNode, int offset) {
        if (node == null || newNode == null) return;
        if (fSplitNode == node) return;

        if (node == fStartContainer
        && fStartContainer.getNodeType() == Node.TEXT_NODE) {
            if (fStartOffset > offset) {
                fStartOffset = fStartOffset - offset;
                fStartContainer = newNode;
            }
        }
        if (node == fEndContainer
        && fEndContainer.getNodeType() == Node.TEXT_NODE) {
            if (fEndOffset > offset) {
                fEndOffset = fEndOffset-offset;
                fEndContainer = newNode;
            }
        }

    }

    /** This function inserts text into a Node and invokes
     *  a method to fix-up all other Ranges.
     */
    void deleteData(CharacterData node, int offset, int count) {
        fDeleteNode = node;
        node.deleteData( offset,  count);
        fDeleteNode = null;
    }


    /** This function is called from DOM.
     *  The  text has already beeen inserted.
     *  Fix-up any offsets.
     */
    void receiveDeletedText(Node node, int offset, int count) {
        if (node == null) return;
        if (fDeleteNode == node) return;
        if (node == fStartContainer
        && fStartContainer.getNodeType() == Node.TEXT_NODE) {
            if (fStartOffset > offset+count) {
                fStartOffset = offset+(fStartOffset-(offset+count));
            } else
            if (fStartOffset > offset) {
                fStartOffset = offset;
            }
        }
        if (node == fEndContainer
        && fEndContainer.getNodeType() == Node.TEXT_NODE) {
            if (fEndOffset > offset+count) {
                fEndOffset = offset+(fEndOffset-(offset+count));
            } else
            if (fEndOffset > offset) {
                fEndOffset = offset;
            }
        }

    }

    /** This function inserts text into a Node and invokes
     *  a method to fix-up all other Ranges.
     */
    void insertData(CharacterData node, int index, String insert) {
        fInsertNode = node;
        node.insertData( index,  insert);
        fInsertNode = null;
    }


    /** This function is called from DOM.
     *  The  text has already beeen inserted.
     *  Fix-up any offsets.
     */
    void receiveInsertedText(Node node, int index, int len) {
        if (node == null) return;
        if (fInsertNode == node) return;
        if (node == fStartContainer
        && fStartContainer.getNodeType() == Node.TEXT_NODE) {
            if (index < fStartOffset) {
                fStartOffset = fStartOffset+len;
            }
        }
        if (node == fEndContainer
        && fEndContainer.getNodeType() == Node.TEXT_NODE) {
            if (index < fEndOffset) {
                fEndOffset = fEndOffset+len;
            }
        }

    }

    /** This function is called from DOM.
     *  The  text has already beeen replaced.
     *  Fix-up any offsets.
     */
    void receiveReplacedText(Node node) {
        if (node == null) return;
        if (node == fStartContainer
        && fStartContainer.getNodeType() == Node.TEXT_NODE) {
            fStartOffset = 0;
        }
        if (node == fEndContainer
        && fEndContainer.getNodeType() == Node.TEXT_NODE) {
            fEndOffset = 0;
        }

    }

    /** This function is called from the DOM.
     *  This node has already been inserted into the DOM.
     *  Fix-up any offsets.
     */
    public void insertedNodeFromDOM(Node node) {
        if (node == null) return;
        if (fInsertNode == node) return;
        if (fInsertedFromRange) return; // Offsets are adjusted in Range.insertNode

        Node parent = node.getParentNode();

        if (parent == fStartContainer) {
            int index = indexOf(node, fStartContainer);
            if (index < fStartOffset) {
                fStartOffset++;
            }
        }

        if (parent == fEndContainer) {
            int index = indexOf(node, fEndContainer);
            if (index < fEndOffset) {
                fEndOffset++;
            }
        }

    }

    /** This function is called within Range
     *  instead of Node.removeChild,
     *  so that the range can remember that it is actively
     *  removing this child.
     */

    Node fRemoveChild = null;
    Node removeChild(Node parent, Node child) {
        fRemoveChild = child;
        Node n = parent.removeChild(child);
        fRemoveChild = null;
        return n;
    }

    /** This function must be called by the DOM _BEFORE_
     *  a node is deleted, because at that time it is
     *  connected in the DOM tree, which we depend on.
     */
    void removeNode(Node node) {
        if (node == null) return;
        if (fRemoveChild == node) return;

        Node parent = node.getParentNode();

        if (parent == fStartContainer) {
            int index = indexOf(node, fStartContainer);
            if (index < fStartOffset) {
                fStartOffset--;
            }
        }

        if (parent == fEndContainer) {
            int index = indexOf(node, fEndContainer);
            if (index < fEndOffset) {
                fEndOffset--;
            }
        }
        //startContainer or endContainer or both is/are the ancestor(s) of the Node to be deleted
        if (parent != fStartContainer
        ||  parent != fEndContainer) {
            if (isAncestorOf(node, fStartContainer)) {
                fStartContainer = parent;
                fStartOffset = indexOf( node, parent);
            }
            if (isAncestorOf(node, fEndContainer)) {
                fEndContainer = parent;
                fEndOffset = indexOf( node, parent);
            }
        }

    }

    //
    // Utility functions.
    //

    // parameters for traverseContents(int)
    //REVIST: use boolean, since there are only 2 now...
    static final int EXTRACT_CONTENTS = 1;
    static final int CLONE_CONTENTS = 2;
    static final int DELETE_CONTENTS = 3;

    /**
     * This is the master routine invoked to visit the nodes
     * selected by this range.  For each such node, different
     * actions are taken depending on the value of the
     * <code>how</code> argument.
     *
     * @param how    Specifies what type of traversal is being
     *               requested (extract, clone, or delete).
     *               Legal values for this argument are:
     *
     *               <ol>
     *               <li><code>EXTRACT_CONTENTS</code> - will produce
     *               a document fragment containing the range's content.
     *               Partially selected nodes are copied, but fully
     *               selected nodes are moved.
     *
     *               <li><code>CLONE_CONTENTS</code> - will leave the
     *               context tree of the range undisturbed, but sill
     *               produced cloned content in a document fragment
     *
     *               <li><code>DELETE_CONTENTS</code> - will delete from
     *               the context tree of the range, all fully selected
     *               nodes.
     *               </ol>
     *
     * @return Returns a document fragment containing any
     *         copied or extracted nodes.  If the <code>how</code>
     *         parameter was <code>DELETE_CONTENTS</code>, the
     *         return value is null.
     */
    private DocumentFragment traverseContents( int how )
        throws DOMException
    {
        if (fStartContainer == null || fEndContainer == null) {
            return null; // REVIST: Throw exception?
        }

        //Check for a detached range.
        if( fDetach) {
            throw new DOMException(
                DOMException.INVALID_STATE_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_STATE_ERR", null));
        }

        /*
          Traversal is accomplished by first determining the
          relationship between the endpoints of the range.
          For each of four significant relationships, we will
          delegate the traversal call to a method that
          can make appropriate assumptions.
         */

        // case 1: same container
        if ( fStartContainer == fEndContainer )
            return traverseSameContainer( how );


        // case 2: Child C of start container is ancestor of end container
        // This can be quickly tested by walking the parent chain of
        // end container
        int endContainerDepth = 0;
        for ( Node c = fEndContainer, p = c.getParentNode();
             p != null;
             c = p, p = p.getParentNode())
        {
            if (p == fStartContainer)
                return traverseCommonStartContainer( c, how );
            ++endContainerDepth;
        }

        // case 3: Child C of container B is ancestor of A
        // This can be quickly tested by walking the parent chain of A
        int startContainerDepth = 0;
        for ( Node c = fStartContainer, p = c.getParentNode();
             p != null;
             c = p, p = p.getParentNode())
        {
            if (p == fEndContainer)
                return traverseCommonEndContainer( c, how );
            ++startContainerDepth;
        }

        // case 4: There is a common ancestor container.  Find the
        // ancestor siblings that are children of that container.
        int depthDiff = startContainerDepth - endContainerDepth;

        Node startNode = fStartContainer;
        while (depthDiff > 0) {
            startNode = startNode.getParentNode();
            depthDiff--;
        }

        Node endNode = fEndContainer;
        while (depthDiff < 0) {
            endNode = endNode.getParentNode();
            depthDiff++;
        }

        // ascend the ancestor hierarchy until we have a common parent.
        for( Node sp = startNode.getParentNode(), ep = endNode.getParentNode();
             sp!=ep;
             sp = sp.getParentNode(), ep = ep.getParentNode() )
        {
            startNode = sp;
            endNode = ep;
        }
        return traverseCommonAncestors( startNode, endNode, how );
    }

    /**
     * Visits the nodes selected by this range when we know
     * a-priori that the start and end containers are the same.
     * This method is invoked by the generic <code>traverse</code>
     * method.
     *
     * @param how    Specifies what type of traversal is being
     *               requested (extract, clone, or delete).
     *               Legal values for this argument are:
     *
     *               <ol>
     *               <li><code>EXTRACT_CONTENTS</code> - will produce
     *               a document fragment containing the range's content.
     *               Partially selected nodes are copied, but fully
     *               selected nodes are moved.
     *
     *               <li><code>CLONE_CONTENTS</code> - will leave the
     *               context tree of the range undisturbed, but sill
     *               produced cloned content in a document fragment
     *
     *               <li><code>DELETE_CONTENTS</code> - will delete from
     *               the context tree of the range, all fully selected
     *               nodes.
     *               </ol>
     *
     * @return Returns a document fragment containing any
     *         copied or extracted nodes.  If the <code>how</code>
     *         parameter was <code>DELETE_CONTENTS</code>, the
     *         return value is null.
     */
    private DocumentFragment traverseSameContainer( int how )
    {
        DocumentFragment frag = null;
        if ( how!=DELETE_CONTENTS)
            frag = fDocument.createDocumentFragment();

        // If selection is empty, just return the fragment
        if ( fStartOffset==fEndOffset )
            return frag;

        // Text node needs special case handling
        if ( fStartContainer.getNodeType()==Node.TEXT_NODE )
        {
            // get the substring
            String s = fStartContainer.getNodeValue();
            String sub = s.substring( fStartOffset, fEndOffset );

            // set the original text node to its new value
            if ( how != CLONE_CONTENTS )
            {
                ((TextImpl)fStartContainer).deleteData(fStartOffset,
                     fEndOffset-fStartOffset) ;
                // Nothing is partially selected, so collapse to start point
                collapse( true );
            }
            if ( how==DELETE_CONTENTS)
                return null;
            frag.appendChild( fDocument.createTextNode(sub) );
            return frag;
        }

        // Copy nodes between the start/end offsets.
        Node n = getSelectedNode( fStartContainer, fStartOffset );
        int cnt = fEndOffset - fStartOffset;
        while( cnt > 0 )
        {
            Node sibling = n.getNextSibling();
            Node xferNode = traverseFullySelected( n, how );
            if ( frag!=null )
                frag.appendChild( xferNode );
            --cnt;
            n = sibling;
        }

        // Nothing is partially selected, so collapse to start point
        if ( how != CLONE_CONTENTS )
            collapse( true );
        return frag;
    }

    /**
     * Visits the nodes selected by this range when we know
     * a-priori that the start and end containers are not the
     * same, but the start container is an ancestor of the
     * end container. This method is invoked by the generic
     * <code>traverse</code> method.
     *
     * @param endAncestor
     *               The ancestor of the end container that is a direct child
     *               of the start container.
     *
     * @param how    Specifies what type of traversal is being
     *               requested (extract, clone, or delete).
     *               Legal values for this argument are:
     *
     *               <ol>
     *               <li><code>EXTRACT_CONTENTS</code> - will produce
     *               a document fragment containing the range's content.
     *               Partially selected nodes are copied, but fully
     *               selected nodes are moved.
     *
     *               <li><code>CLONE_CONTENTS</code> - will leave the
     *               context tree of the range undisturbed, but sill
     *               produced cloned content in a document fragment
     *
     *               <li><code>DELETE_CONTENTS</code> - will delete from
     *               the context tree of the range, all fully selected
     *               nodes.
     *               </ol>
     *
     * @return Returns a document fragment containing any
     *         copied or extracted nodes.  If the <code>how</code>
     *         parameter was <code>DELETE_CONTENTS</code>, the
     *         return value is null.
     */
    private DocumentFragment
        traverseCommonStartContainer( Node endAncestor, int how )
    {
        DocumentFragment frag = null;
        if ( how!=DELETE_CONTENTS)
            frag = fDocument.createDocumentFragment();
        Node n = traverseRightBoundary( endAncestor, how );
        if ( frag!=null )
            frag.appendChild( n );

        int endIdx = indexOf( endAncestor, fStartContainer );
        int cnt = endIdx - fStartOffset;
        if ( cnt <=0 )
        {
            // Collapse to just before the endAncestor, which
            // is partially selected.
            if ( how != CLONE_CONTENTS )
            {
                setEndBefore( endAncestor );
                collapse( false );
            }
            return frag;
        }

        n = endAncestor.getPreviousSibling();
        while( cnt > 0 )
        {
            Node sibling = n.getPreviousSibling();
            Node xferNode = traverseFullySelected( n, how );
            if ( frag!=null )
                frag.insertBefore( xferNode, frag.getFirstChild() );
            --cnt;
            n = sibling;
        }
        // Collapse to just before the endAncestor, which
        // is partially selected.
        if ( how != CLONE_CONTENTS )
        {
            setEndBefore( endAncestor );
            collapse( false );
        }
        return frag;
    }

    /**
     * Visits the nodes selected by this range when we know
     * a-priori that the start and end containers are not the
     * same, but the end container is an ancestor of the
     * start container. This method is invoked by the generic
     * <code>traverse</code> method.
     *
     * @param startAncestor
     *               The ancestor of the start container that is a direct
     *               child of the end container.
     *
     * @param how    Specifies what type of traversal is being
     *               requested (extract, clone, or delete).
     *               Legal values for this argument are:
     *
     *               <ol>
     *               <li><code>EXTRACT_CONTENTS</code> - will produce
     *               a document fragment containing the range's content.
     *               Partially selected nodes are copied, but fully
     *               selected nodes are moved.
     *
     *               <li><code>CLONE_CONTENTS</code> - will leave the
     *               context tree of the range undisturbed, but sill
     *               produced cloned content in a document fragment
     *
     *               <li><code>DELETE_CONTENTS</code> - will delete from
     *               the context tree of the range, all fully selected
     *               nodes.
     *               </ol>
     *
     * @return Returns a document fragment containing any
     *         copied or extracted nodes.  If the <code>how</code>
     *         parameter was <code>DELETE_CONTENTS</code>, the
     *         return value is null.
     */
    private DocumentFragment
        traverseCommonEndContainer( Node startAncestor, int how )
    {
        DocumentFragment frag = null;
        if ( how!=DELETE_CONTENTS)
            frag = fDocument.createDocumentFragment();
        Node n = traverseLeftBoundary( startAncestor, how );
        if ( frag!=null )
            frag.appendChild( n );
        int startIdx = indexOf( startAncestor, fEndContainer );
        ++startIdx;  // Because we already traversed it....

        int cnt = fEndOffset - startIdx;
        n = startAncestor.getNextSibling();
        while( cnt > 0 )
        {
            Node sibling = n.getNextSibling();
            Node xferNode = traverseFullySelected( n, how );
            if ( frag!=null )
                frag.appendChild( xferNode );
            --cnt;
            n = sibling;
        }

        if ( how != CLONE_CONTENTS )
        {
            setStartAfter( startAncestor );
            collapse( true );
        }

        return frag;
    }

    /**
     * Visits the nodes selected by this range when we know
     * a-priori that the start and end containers are not
     * the same, and we also know that neither the start
     * nor end container is an ancestor of the other.
     * This method is invoked by
     * the generic <code>traverse</code> method.
     *
     * @param startAncestor
     *               Given a common ancestor of the start and end containers,
     *               this parameter is the ancestor (or self) of the start
     *               container that is a direct child of the common ancestor.
     *
     * @param endAncestor
     *               Given a common ancestor of the start and end containers,
     *               this parameter is the ancestor (or self) of the end
     *               container that is a direct child of the common ancestor.
     *
     * @param how    Specifies what type of traversal is being
     *               requested (extract, clone, or delete).
     *               Legal values for this argument are:
     *
     *               <ol>
     *               <li><code>EXTRACT_CONTENTS</code> - will produce
     *               a document fragment containing the range's content.
     *               Partially selected nodes are copied, but fully
     *               selected nodes are moved.
     *
     *               <li><code>CLONE_CONTENTS</code> - will leave the
     *               context tree of the range undisturbed, but sill
     *               produced cloned content in a document fragment
     *
     *               <li><code>DELETE_CONTENTS</code> - will delete from
     *               the context tree of the range, all fully selected
     *               nodes.
     *               </ol>
     *
     * @return Returns a document fragment containing any
     *         copied or extracted nodes.  If the <code>how</code>
     *         parameter was <code>DELETE_CONTENTS</code>, the
     *         return value is null.
     */
    private DocumentFragment
        traverseCommonAncestors( Node startAncestor, Node endAncestor, int how )
    {
        DocumentFragment frag = null;
        if ( how!=DELETE_CONTENTS)
            frag = fDocument.createDocumentFragment();

        Node n = traverseLeftBoundary( startAncestor, how );
        if ( frag!=null )
            frag.appendChild( n );

        Node commonParent = startAncestor.getParentNode();
        int startOffset = indexOf( startAncestor, commonParent );
        int endOffset = indexOf( endAncestor, commonParent );
        ++startOffset;

        int cnt = endOffset - startOffset;
        Node sibling = startAncestor.getNextSibling();

        while( cnt > 0 )
        {
            Node nextSibling = sibling.getNextSibling();
            n = traverseFullySelected( sibling, how );
            if ( frag!=null )
                frag.appendChild( n );
            sibling = nextSibling;
            --cnt;
        }

        n = traverseRightBoundary( endAncestor, how );
        if ( frag!=null )
            frag.appendChild( n );

        if ( how != CLONE_CONTENTS )
        {
            setStartAfter( startAncestor );
            collapse( true );
        }
        return frag;
    }

    /**
     * Traverses the "right boundary" of this range and
     * operates on each "boundary node" according to the
     * <code>how</code> parameter.  It is a-priori assumed
     * by this method that the right boundary does
     * not contain the range's start container.
     * <p>
     * A "right boundary" is best visualized by thinking
     * of a sample tree:<pre>
     *                 A
     *                /|\
     *               / | \
     *              /  |  \
     *             B   C   D
     *            /|\     /|\
     *           E F G   H I J
     * </pre>
     * Imagine first a range that begins between the
     * "E" and "F" nodes and ends between the
     * "I" and "J" nodes.  The start container is
     * "B" and the end container is "D".  Given this setup,
     * the following applies:
     * <p>
     * Partially Selected Nodes: B, D<br>
     * Fully Selected Nodes: F, G, C, H, I
     * <p>
     * The "right boundary" is the highest subtree node
     * that contains the ending container.  The root of
     * this subtree is always partially selected.
     * <p>
     * In this example, the nodes that are traversed
     * as "right boundary" nodes are: H, I, and D.
     *
     * @param root   The node that is the root of the "right boundary" subtree.
     *
     * @param how    Specifies what type of traversal is being
     *               requested (extract, clone, or delete).
     *               Legal values for this argument are:
     *
     *               <ol>
     *               <li><code>EXTRACT_CONTENTS</code> - will produce
     *               a node containing the boundaries content.
     *               Partially selected nodes are copied, but fully
     *               selected nodes are moved.
     *
     *               <li><code>CLONE_CONTENTS</code> - will leave the
     *               context tree of the range undisturbed, but will
     *               produced cloned content.
     *
     *               <li><code>DELETE_CONTENTS</code> - will delete from
     *               the context tree of the range, all fully selected
     *               nodes within the boundary.
     *               </ol>
     *
     * @return Returns a node that is the result of visiting nodes.
     *         If the traversal operation is
     *         <code>DELETE_CONTENTS</code> the return value is null.
     */
    private Node traverseRightBoundary( Node root, int how )
    {
        Node next = getSelectedNode( fEndContainer, fEndOffset-1 );
        boolean isFullySelected = ( next!=fEndContainer );

        if ( next==root )
            return traverseNode( next, isFullySelected, false, how );

        Node parent = next.getParentNode();
        Node clonedParent = traverseNode( parent, false, false, how );

        while( parent!=null )
        {
            while( next!=null )
            {
                Node prevSibling = next.getPreviousSibling();
                Node clonedChild =
                    traverseNode( next, isFullySelected, false, how );
                if ( how!=DELETE_CONTENTS )
                {
                    clonedParent.insertBefore(
                        clonedChild,
                        clonedParent.getFirstChild()
                    );
                }
                isFullySelected = true;
                next = prevSibling;
            }
            if ( parent==root )
                return clonedParent;

            next = parent.getPreviousSibling();
            parent = parent.getParentNode();
            Node clonedGrandParent = traverseNode( parent, false, false, how );
            if ( how!=DELETE_CONTENTS )
                clonedGrandParent.appendChild( clonedParent );
            clonedParent = clonedGrandParent;

        }

        // should never occur
        return null;
    }

    /**
     * Traverses the "left boundary" of this range and
     * operates on each "boundary node" according to the
     * <code>how</code> parameter.  It is a-priori assumed
     * by this method that the left boundary does
     * not contain the range's end container.
     * <p>
     * A "left boundary" is best visualized by thinking
     * of a sample tree:<pre>
     *
     *                 A
     *                /|\
     *               / | \
     *              /  |  \
     *             B   C   D
     *            /|\     /|\
     *           E F G   H I J
     * </pre>
     * Imagine first a range that begins between the
     * "E" and "F" nodes and ends between the
     * "I" and "J" nodes.  The start container is
     * "B" and the end container is "D".  Given this setup,
     * the following applies:
     * <p>
     * Partially Selected Nodes: B, D<br>
     * Fully Selected Nodes: F, G, C, H, I
     * <p>
     * The "left boundary" is the highest subtree node
     * that contains the starting container.  The root of
     * this subtree is always partially selected.
     * <p>
     * In this example, the nodes that are traversed
     * as "left boundary" nodes are: F, G, and B.
     *
     * @param root   The node that is the root of the "left boundary" subtree.
     *
     * @param how    Specifies what type of traversal is being
     *               requested (extract, clone, or delete).
     *               Legal values for this argument are:
     *
     *               <ol>
     *               <li><code>EXTRACT_CONTENTS</code> - will produce
     *               a node containing the boundaries content.
     *               Partially selected nodes are copied, but fully
     *               selected nodes are moved.
     *
     *               <li><code>CLONE_CONTENTS</code> - will leave the
     *               context tree of the range undisturbed, but will
     *               produced cloned content.
     *
     *               <li><code>DELETE_CONTENTS</code> - will delete from
     *               the context tree of the range, all fully selected
     *               nodes within the boundary.
     *               </ol>
     *
     * @return Returns a node that is the result of visiting nodes.
     *         If the traversal operation is
     *         <code>DELETE_CONTENTS</code> the return value is null.
     */
    private Node traverseLeftBoundary( Node root, int how )
    {
        Node next = getSelectedNode( getStartContainer(), getStartOffset() );
        boolean isFullySelected = ( next!=getStartContainer() );

        if ( next==root )
            return traverseNode( next, isFullySelected, true, how );

        Node parent = next.getParentNode();
        Node clonedParent = traverseNode( parent, false, true, how );

        while( parent!=null )
        {
            while( next!=null )
            {
                Node nextSibling = next.getNextSibling();
                Node clonedChild =
                    traverseNode( next, isFullySelected, true, how );
                if ( how!=DELETE_CONTENTS )
                    clonedParent.appendChild(clonedChild);
                isFullySelected = true;
                next = nextSibling;
            }
            if ( parent==root )
                return clonedParent;

            next = parent.getNextSibling();
            parent = parent.getParentNode();
            Node clonedGrandParent = traverseNode( parent, false, true, how );
            if ( how!=DELETE_CONTENTS )
                clonedGrandParent.appendChild( clonedParent );
            clonedParent = clonedGrandParent;

        }

        // should never occur
        return null;

    }

    /**
     * Utility method for traversing a single node.
     * Does not properly handle a text node containing both the
     * start and end offsets.  Such nodes should
     * have been previously detected and been routed to traverseTextNode.
     *
     * @param n      The node to be traversed.
     *
     * @param isFullySelected
     *               Set to true if the node is fully selected.  Should be
     *               false otherwise.
     *               Note that although the DOM 2 specification says that a
     *               text node that is boththe start and end container is not
     *               selected, we treat it here as if it were partially
     *               selected.
     *
     * @param isLeft Is true if we are traversing the node as part of navigating
     *               the "left boundary" of the range.  If this value is false,
     *               it implies we are navigating the "right boundary" of the
     *               range.
     *
     * @param how    Specifies what type of traversal is being
     *               requested (extract, clone, or delete).
     *               Legal values for this argument are:
     *
     *               <ol>
     *               <li><code>EXTRACT_CONTENTS</code> - will simply
     *               return the original node.
     *
     *               <li><code>CLONE_CONTENTS</code> - will leave the
     *               context tree of the range undisturbed, but will
     *               return a cloned node.
     *
     *               <li><code>DELETE_CONTENTS</code> - will delete the
     *               node from it's parent, but will return null.
     *               </ol>
     *
     * @return Returns a node that is the result of visiting the node.
     *         If the traversal operation is
     *         <code>DELETE_CONTENTS</code> the return value is null.
     */
    private Node traverseNode( Node n, boolean isFullySelected, boolean isLeft, int how )
    {
        if ( isFullySelected )
            return traverseFullySelected( n, how );
        if ( n.getNodeType()==Node.TEXT_NODE )
            return traverseTextNode( n, isLeft, how );
        return traversePartiallySelected( n, how );
    }

    /**
     * Utility method for traversing a single node when
     * we know a-priori that the node if fully
     * selected.
     *
     * @param n      The node to be traversed.
     *
     * @param how    Specifies what type of traversal is being
     *               requested (extract, clone, or delete).
     *               Legal values for this argument are:
     *
     *               <ol>
     *               <li><code>EXTRACT_CONTENTS</code> - will simply
     *               return the original node.
     *
     *               <li><code>CLONE_CONTENTS</code> - will leave the
     *               context tree of the range undisturbed, but will
     *               return a cloned node.
     *
     *               <li><code>DELETE_CONTENTS</code> - will delete the
     *               node from it's parent, but will return null.
     *               </ol>
     *
     * @return Returns a node that is the result of visiting the node.
     *         If the traversal operation is
     *         <code>DELETE_CONTENTS</code> the return value is null.
     */
    private Node traverseFullySelected( Node n, int how )
    {
        switch( how )
        {
        case CLONE_CONTENTS:
            return n.cloneNode( true );
        case EXTRACT_CONTENTS:
            if ( n.getNodeType()==Node.DOCUMENT_TYPE_NODE )
            {
                // TBD: This should be a HIERARCHY_REQUEST_ERR
                throw new DOMException(
                        DOMException.HIERARCHY_REQUEST_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "HIERARCHY_REQUEST_ERR", null));
            }
            return n;
        case DELETE_CONTENTS:
            n.getParentNode().removeChild(n);
            return null;
        }
        return null;
    }

    /**
     * Utility method for traversing a single node when
     * we know a-priori that the node if partially
     * selected and is not a text node.
     *
     * @param n      The node to be traversed.
     *
     * @param how    Specifies what type of traversal is being
     *               requested (extract, clone, or delete).
     *               Legal values for this argument are:
     *
     *               <ol>
     *               <li><code>EXTRACT_CONTENTS</code> - will simply
     *               return the original node.
     *
     *               <li><code>CLONE_CONTENTS</code> - will leave the
     *               context tree of the range undisturbed, but will
     *               return a cloned node.
     *
     *               <li><code>DELETE_CONTENTS</code> - will delete the
     *               node from it's parent, but will return null.
     *               </ol>
     *
     * @return Returns a node that is the result of visiting the node.
     *         If the traversal operation is
     *         <code>DELETE_CONTENTS</code> the return value is null.
     */
    private Node traversePartiallySelected( Node n, int how )
    {
        switch( how )
        {
        case DELETE_CONTENTS:
            return null;
        case CLONE_CONTENTS:
        case EXTRACT_CONTENTS:
            return n.cloneNode( false );
        }
        return null;
    }

    /**
     * Utility method for traversing a text node that we know
     * a-priori to be on a left or right boundary of the range.
     * This method does not properly handle text nodes that contain
     * both the start and end points of the range.
     *
     * @param n      The node to be traversed.
     *
     * @param isLeft Is true if we are traversing the node as part of navigating
     *               the "left boundary" of the range.  If this value is false,
     *               it implies we are navigating the "right boundary" of the
     *               range.
     *
     * @param how    Specifies what type of traversal is being
     *               requested (extract, clone, or delete).
     *               Legal values for this argument are:
     *
     *               <ol>
     *               <li><code>EXTRACT_CONTENTS</code> - will simply
     *               return the original node.
     *
     *               <li><code>CLONE_CONTENTS</code> - will leave the
     *               context tree of the range undisturbed, but will
     *               return a cloned node.
     *
     *               <li><code>DELETE_CONTENTS</code> - will delete the
     *               node from it's parent, but will return null.
     *               </ol>
     *
     * @return Returns a node that is the result of visiting the node.
     *         If the traversal operation is
     *         <code>DELETE_CONTENTS</code> the return value is null.
     */
    private Node traverseTextNode( Node n, boolean isLeft, int how )
    {
        String txtValue = n.getNodeValue();
        String newNodeValue;
        String oldNodeValue;

        if ( isLeft )
        {
            int offset = getStartOffset();
            newNodeValue = txtValue.substring( offset );
            oldNodeValue = txtValue.substring( 0, offset );
        }
        else
        {
            int offset = getEndOffset();
            newNodeValue = txtValue.substring( 0, offset );
            oldNodeValue = txtValue.substring( offset );
        }

        if ( how != CLONE_CONTENTS )
            n.setNodeValue( oldNodeValue );
        if ( how==DELETE_CONTENTS )
            return null;
        Node newNode = n.cloneNode( false );
        newNode.setNodeValue( newNodeValue );
        return newNode;
    }

    void checkIndex(Node refNode, int offset) throws DOMException
    {
        if (offset < 0) {
            throw new DOMException(
                DOMException.INDEX_SIZE_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INDEX_SIZE_ERR", null));
        }

        int type = refNode.getNodeType();

        // If the node contains text, ensure that the
        // offset of the range is <= to the length of the text
        if (type == Node.TEXT_NODE
            || type == Node.CDATA_SECTION_NODE
            || type == Node.COMMENT_NODE
            || type == Node.PROCESSING_INSTRUCTION_NODE) {
            if (offset > refNode.getNodeValue().length()) {
                throw new DOMException(DOMException.INDEX_SIZE_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INDEX_SIZE_ERR", null));
            }
        }
        else {
            // Since the node is not text, ensure that the offset
            // is valid with respect to the number of child nodes
            if (offset > refNode.getChildNodes().getLength()) {
                throw new DOMException(DOMException.INDEX_SIZE_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INDEX_SIZE_ERR", null));
            }
        }
    }

        /**
         * Given a node, calculate what the Range's root container
         * for that node would be.
         */
        private Node getRootContainer( Node node )
        {
                if ( node==null )
                        return null;

                while( node.getParentNode()!=null )
                        node = node.getParentNode();
                return node;
        }

        /**
         * Returns true IFF the given node can serve as a container
         * for a range's boundary points.
         */
        private boolean isLegalContainer( Node node )
        {
                if ( node==null )
                        return false;

                while( node!=null )
                {
                        switch( node.getNodeType() )
                        {
                        case Node.ENTITY_NODE:
                        case Node.NOTATION_NODE:
                        case Node.DOCUMENT_TYPE_NODE:
                                return false;
                        }
                        node = node.getParentNode();
                }

                return true;
        }


        /**
         * Finds the root container for the given node and determines
         * if that root container is legal with respect to the
         * DOM 2 specification.  At present, that means the root
         * container must be either an attribute, a document,
         * or a document fragment.
         */
        private boolean hasLegalRootContainer( Node node )
        {
                if ( node==null )
                        return false;

                Node rootContainer = getRootContainer( node );
                switch( rootContainer.getNodeType() )
                {
                case Node.ATTRIBUTE_NODE:
                case Node.DOCUMENT_NODE:
                case Node.DOCUMENT_FRAGMENT_NODE:
                        return true;
                }
                return false;
        }

        /**
         * Returns true IFF the given node can be contained by
         * a range.
         */
        private boolean isLegalContainedNode( Node node )
        {
                if ( node==null )
                        return false;
                switch( node.getNodeType() )
                {
                case Node.DOCUMENT_NODE:
                case Node.DOCUMENT_FRAGMENT_NODE:
                case Node.ATTRIBUTE_NODE:
                case Node.ENTITY_NODE:
                case Node.NOTATION_NODE:
                        return false;
                }
                return true;
        }

    Node nextNode(Node node, boolean visitChildren) {

        if (node == null) return null;

        Node result;
        if (visitChildren) {
            result = node.getFirstChild();
            if (result != null) {
                return result;
            }
        }

        // if hasSibling, return sibling
        result = node.getNextSibling();
        if (result != null) {
            return result;
        }


        // return parent's 1st sibling.
        Node parent = node.getParentNode();
        while (parent != null
               && parent != fDocument
                ) {
            result = parent.getNextSibling();
            if (result != null) {
                return result;
            } else {
                parent = parent.getParentNode();
            }

        } // while (parent != null && parent != fRoot) {

        // end of list, return null
        return null;
    }

    /** is a an ancestor of b ? */
    boolean isAncestorOf(Node a, Node b) {
        for (Node node=b; node != null; node=node.getParentNode()) {
            if (node == a) return true;
        }
        return false;
    }

    /** what is the index of the child in the parent */
    int indexOf(Node child, Node parent) {
        if (child.getParentNode() != parent) return -1;
        int i = 0;
        for(Node node = parent.getFirstChild(); node!= child; node=node.getNextSibling()) {
            i++;
        }
        return i;
    }

    /**
     * Utility method to retrieve a child node by index.  This method
     * assumes the caller is trying to find out which node is
     * selected by the given index.  Note that if the index is
     * greater than the number of children, this implies that the
     * first node selected is the parent node itself.
     *
     * @param container A container node
     *
     * @param offset    An offset within the container for which a selected node should
     *                  be computed.  If the offset is less than zero, or if the offset
     *                  is greater than the number of children, the container is returned.
     *
     * @return Returns either a child node of the container or the
     *         container itself.
     */
    private Node getSelectedNode( Node container, int offset )
    {
        if ( container.getNodeType() == Node.TEXT_NODE )
            return container;

        // This case is an important convenience for
        // traverseRightBoundary()
        if ( offset<0 )
            return container;

        Node child = container.getFirstChild();
        while( child!=null && offset > 0 )
        {
            --offset;
            child = child.getNextSibling();
        }
        if ( child!=null )
            return child;
        return container;
    }

}
