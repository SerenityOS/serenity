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

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

/** This class implements the TreeWalker interface.
 *
 * @xerces.internal
 *
 */

public class TreeWalkerImpl implements TreeWalker {

    //
    // Data
    //

    /** When TRUE, the children of entites references are returned in the iterator. */
    private boolean fEntityReferenceExpansion = false;
    /** The whatToShow mask. */
    int fWhatToShow = NodeFilter.SHOW_ALL;
    /** The NodeFilter reference. */
    NodeFilter fNodeFilter;
    /** The current Node. */
    Node fCurrentNode;
    /** The root Node. */
    Node fRoot;

    //
    // Implementation Note: No state is kept except the data above
    // (fWhatToShow, fNodeFilter, fCurrentNode, fRoot) such that
    // setters could be created for these data values and the
    // implementation will still work.


    //
    // Constructor
    //

    /** Public constructor */
    public TreeWalkerImpl(Node root,
                          int whatToShow,
                          NodeFilter nodeFilter,
                          boolean entityReferenceExpansion) {
        fCurrentNode = root;
        fRoot = root;
        fWhatToShow = whatToShow;
        fNodeFilter = nodeFilter;
        fEntityReferenceExpansion = entityReferenceExpansion;
    }

    public Node getRoot() {
        return fRoot;
    }

    /** Return the whatToShow value */
    public int                getWhatToShow() {
        return fWhatToShow;
    }

    public void setWhatShow(int whatToShow){
        fWhatToShow = whatToShow;
    }
    /** Return the NodeFilter */
    public NodeFilter         getFilter() {
        return fNodeFilter;
    }

    /** Return whether children entity references are included in the iterator. */
    public boolean            getExpandEntityReferences() {
        return fEntityReferenceExpansion;
    }

    /** Return the current Node. */
    public Node               getCurrentNode() {
        return fCurrentNode;
    }
    /** Return the current Node. */
    public void               setCurrentNode(Node node) {
        if (node == null) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
              throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
        }

        fCurrentNode = node;
    }

    /** Return the parent Node from the current node,
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               parentNode() {

        if (fCurrentNode == null) return null;

        Node node = getParentNode(fCurrentNode);
        if (node !=null) {
            fCurrentNode = node;
        }
        return node;

    }

    /** Return the first child Node from the current node,
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               firstChild() {

        if (fCurrentNode == null) return null;

        Node node = getFirstChild(fCurrentNode);
        if (node !=null) {
            fCurrentNode = node;
        }
        return node;
    }
    /** Return the last child Node from the current node,
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               lastChild() {

        if (fCurrentNode == null) return null;

        Node node = getLastChild(fCurrentNode);
        if (node !=null) {
            fCurrentNode = node;
        }
        return node;
    }

    /** Return the previous sibling Node from the current node,
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               previousSibling() {

        if (fCurrentNode == null) return null;

        Node node = getPreviousSibling(fCurrentNode);
        if (node !=null) {
            fCurrentNode = node;
        }
        return node;
    }

    /** Return the next sibling Node from the current node,
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               nextSibling(){
        if (fCurrentNode == null) return null;

        Node node = getNextSibling(fCurrentNode);
        if (node !=null) {
            fCurrentNode = node;
        }
        return node;
    }

    /** Return the previous Node from the current node,
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               previousNode() {
        Node result;

        if (fCurrentNode == null) return null;

        // get sibling
        result = getPreviousSibling(fCurrentNode);
        if (result == null) {
            result = getParentNode(fCurrentNode);
            if (result != null) {
                fCurrentNode = result;
                return fCurrentNode;
            }
            return null;
        }

        // get the lastChild of result.
        Node lastChild  = getLastChild(result);

        Node prev = lastChild ;
        while (lastChild != null) {
          prev = lastChild ;
          lastChild = getLastChild(prev) ;
        }

        lastChild = prev ;

        // if there is a lastChild which passes filters return it.
        if (lastChild != null) {
            fCurrentNode = lastChild;
            return fCurrentNode;
        }

        // otherwise return the previous sibling.
        if (result != null) {
            fCurrentNode = result;
            return fCurrentNode;
        }

        // otherwise return null.
        return null;
    }

    /** Return the next Node from the current node,
     *  after applying filter, whatToshow.
     *  If result is not null, set the current Node.
     */
    public Node               nextNode() {

        if (fCurrentNode == null) return null;

        Node result = getFirstChild(fCurrentNode);

        if (result != null) {
            fCurrentNode = result;
            return result;
        }

        result = getNextSibling(fCurrentNode);

        if (result != null) {
            fCurrentNode = result;
            return result;
        }

        // return parent's 1st sibling.
        Node parent = getParentNode(fCurrentNode);
        while (parent != null) {
            result = getNextSibling(parent);
            if (result != null) {
                fCurrentNode = result;
                return result;
            } else {
                parent = getParentNode(parent);
            }
        }

        // end , return null
        return null;
    }

    /** Internal function.
     *  Return the parent Node, from the input node
     *  after applying filter, whatToshow.
     *  The current node is not consulted or set.
     */
    Node getParentNode(Node node) {

        if (node == null || node == fRoot) return null;

        Node newNode = node.getParentNode();
        if (newNode == null)  return null;

        int accept = acceptNode(newNode);

        if (accept == NodeFilter.FILTER_ACCEPT)
            return newNode;
        else
        //if (accept == NodeFilter.SKIP_NODE) // and REJECT too.
        {
            return getParentNode(newNode);
        }


    }

    /** Internal function.
     *  Return the nextSibling Node, from the input node
     *  after applying filter, whatToshow.
     *  The current node is not consulted or set.
     */
    Node getNextSibling(Node node) {
                return getNextSibling(node, fRoot);
        }

    /** Internal function.
     *  Return the nextSibling Node, from the input node
     *  after applying filter, whatToshow.
     *  NEVER TRAVERSES ABOVE THE SPECIFIED ROOT NODE.
     *  The current node is not consulted or set.
     */
    Node getNextSibling(Node node, Node root) {

        if (node == null || node == root) return null;

        Node newNode = node.getNextSibling();
        if (newNode == null) {

            newNode = node.getParentNode();

            if (newNode == null || newNode == root)  return null;

            int parentAccept = acceptNode(newNode);

            if (parentAccept==NodeFilter.FILTER_SKIP) {
                return getNextSibling(newNode, root);
            }

            return null;
        }

        int accept = acceptNode(newNode);

        if (accept == NodeFilter.FILTER_ACCEPT)
            return newNode;
        else
        if (accept == NodeFilter.FILTER_SKIP) {
            Node fChild = getFirstChild(newNode);
            if (fChild == null) {
                return getNextSibling(newNode, root);
            }
            return fChild;
        }
        else
        //if (accept == NodeFilter.REJECT_NODE)
        {
            return getNextSibling(newNode, root);
        }

    } // getNextSibling(Node node) {

    /** Internal function.
     *  Return the previous sibling Node, from the input node
     *  after applying filter, whatToshow.
     *  The current node is not consulted or set.
     */
    Node getPreviousSibling(Node node) {
                return getPreviousSibling(node, fRoot);
        }

    /** Internal function.
     *  Return the previousSibling Node, from the input node
     *  after applying filter, whatToshow.
         *  NEVER TRAVERSES ABOVE THE SPECIFIED ROOT NODE.
     *  The current node is not consulted or set.
     */
    Node getPreviousSibling(Node node, Node root) {

        if (node == null || node == root) return null;

        Node newNode = node.getPreviousSibling();
        if (newNode == null) {

            newNode = node.getParentNode();
            if (newNode == null || newNode == root)  return null;

            int parentAccept = acceptNode(newNode);

            if (parentAccept==NodeFilter.FILTER_SKIP) {
                return getPreviousSibling(newNode, root);
            }

            return null;
        }

        int accept = acceptNode(newNode);

        if (accept == NodeFilter.FILTER_ACCEPT)
            return newNode;
        else
        if (accept == NodeFilter.FILTER_SKIP) {
            Node fChild =  getLastChild(newNode);
            if (fChild == null) {
                return getPreviousSibling(newNode, root);
            }
            return fChild;
        }
        else
        //if (accept == NodeFilter.REJECT_NODE)
        {
            return getPreviousSibling(newNode, root);
        }

    } // getPreviousSibling(Node node) {

    /** Internal function.
     *  Return the first child Node, from the input node
     *  after applying filter, whatToshow.
     *  The current node is not consulted or set.
     */
    Node getFirstChild(Node node) {
        if (node == null) return null;

        if ( !fEntityReferenceExpansion
             && node.getNodeType() == Node.ENTITY_REFERENCE_NODE)
            return null;
        Node newNode = node.getFirstChild();
        if (newNode == null)  return null;
        int accept = acceptNode(newNode);

        if (accept == NodeFilter.FILTER_ACCEPT)
            return newNode;
        else
        if (accept == NodeFilter.FILTER_SKIP
            && newNode.hasChildNodes())
        {
            Node fChild = getFirstChild(newNode);

            if (fChild == null) {
                return getNextSibling(newNode, node);
            }
            return fChild;
        }
        else
        //if (accept == NodeFilter.REJECT_NODE)
        {
            return getNextSibling(newNode, node);
        }


    }

    /** Internal function.
     *  Return the last child Node, from the input node
     *  after applying filter, whatToshow.
     *  The current node is not consulted or set.
     */
    Node getLastChild(Node node) {

        if (node == null) return null;

        if ( !fEntityReferenceExpansion
             && node.getNodeType() == Node.ENTITY_REFERENCE_NODE)
            return null;

        Node newNode = node.getLastChild();
        if (newNode == null)  return null;

        int accept = acceptNode(newNode);

        if (accept == NodeFilter.FILTER_ACCEPT)
            return newNode;
        else
        if (accept == NodeFilter.FILTER_SKIP
            && newNode.hasChildNodes())
        {
            Node lChild = getLastChild(newNode);
            if (lChild == null) {
                return getPreviousSibling(newNode, node);
            }
            return lChild;
        }
        else
        //if (accept == NodeFilter.REJECT_NODE)
        {
            return getPreviousSibling(newNode, node);
        }


    }

    /** Internal function.
     *  The node whatToShow and the filter are combined into one result. */
    short acceptNode(Node node) {
        /***
         7.1.2.4. Filters and whatToShow flags

         Iterator and TreeWalker apply whatToShow flags before applying Filters. If a node is rejected by the
         active whatToShow flags, a Filter will not be called to evaluate that node. When a node is rejected by
         the active whatToShow flags, children of that node will still be considered, and Filters may be called to
         evaluate them.
         ***/

        if (fNodeFilter == null) {
            if ( ( fWhatToShow & (1 << node.getNodeType()-1)) != 0) {
                return NodeFilter.FILTER_ACCEPT;
            } else {
                return NodeFilter.FILTER_SKIP;
            }
        } else {
            if ((fWhatToShow & (1 << node.getNodeType()-1)) != 0 ) {
                return fNodeFilter.acceptNode(node);
            } else {
                // What to show has failed. See above excerpt from spec.
                // Equivalent to FILTER_SKIP.
                return NodeFilter.FILTER_SKIP;
            }
        }
    }
}
