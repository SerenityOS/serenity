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
package com.sun.org.apache.xml.internal.utils;

import com.sun.org.apache.xml.internal.dtm.ref.DTMNodeProxy;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


/**
 * This class provides a DOM level 2 "helper", which provides several services.
 *
 * The original class extended DOMHelper that was deprecated and then removed.
 */
public final class DOM2Helper {

    /**
     * Construct an instance.
     */
    private DOM2Helper() {
    }

    /**
     * Returns the local name of the given node, as defined by the XML
     * Namespaces specification. This is prepared to handle documents built
     * using DOM Level 1 methods by falling back upon explicitly parsing the
     * node name.
     *
     * @param n Node to be examined
     *
     * @return String containing the local name, or null if the node was not
     * assigned a Namespace.
     */
    public static String getLocalNameOfNode(Node n) {

        String name = n.getLocalName();

        return (null == name) ? getLocalNameOfNodeFallback(n) : name;
    }

    /**
     * Returns the local name of the given node. If the node's name begins with
     * a namespace prefix, this is the part after the colon; otherwise it's the
     * full node name.
     *
     * This method is copied from
     * com.sun.org.apache.xml.internal.utils.DOMHelper
     *
     * @param n the node to be examined.
     *
     * @return String containing the Local Name
     */
    private static String getLocalNameOfNodeFallback(Node n) {

        String qname = n.getNodeName();
        int index = qname.indexOf(':');

        return (index < 0) ? qname : qname.substring(index + 1);
    }

    /**
     * Returns the Namespace Name (Namespace URI) for the given node. In a Level
     * 2 DOM, you can ask the node itself. Note, however, that doing so
     * conflicts with our decision in getLocalNameOfNode not to trust the that
     * the DOM was indeed created using the Level 2 methods. If Level 1 methods
     * were used, these two functions will disagree with each other.
     * <p>
     * TODO: Reconcile with getLocalNameOfNode.
     *
     * @param n Node to be examined
     *
     * @return String containing the Namespace URI bound to this DOM node at the
     * time the Node was created.
     */
    public static String getNamespaceOfNode(Node n) {
        return n.getNamespaceURI();
    }

    /**
     * Figure out whether node2 should be considered as being later in the
     * document than node1, in Document Order as defined by the XPath model.
     * This may not agree with the ordering defined by other XML applications.
     * <p>
     * There are some cases where ordering isn't defined, and neither are the
     * results of this function -- though we'll generally return true.
     *
     * @param node1 DOM Node to perform position comparison on.
     * @param node2 DOM Node to perform position comparison on .
     *
     * @return false if node2 comes before node1, otherwise return true. You can
     * think of this as
     * {@code (node1.documentOrderPosition &lt;= node2.documentOrderPosition)}.
     */
    public static boolean isNodeAfter(Node node1, Node node2) {
        if (node1 == node2 || isNodeTheSame(node1, node2)) {
            return true;
        }

        // Default return value, if there is no defined ordering
        boolean isNodeAfter = true;

        Node parent1 = getParentOfNode(node1);
        Node parent2 = getParentOfNode(node2);

        // Optimize for most common case
        if (parent1 == parent2 || isNodeTheSame(parent1, parent2)) // then we know they are siblings
        {
            if (null != parent1) {
                isNodeAfter = isNodeAfterSibling(parent1, node1, node2);
            }
        } else {
            // General strategy: Figure out the lengths of the two
            // ancestor chains, reconcile the lengths, and look for
            // the lowest common ancestor. If that ancestor is one of
            // the nodes being compared, it comes before the other.
            // Otherwise perform a sibling compare.
            //
            // NOTE: If no common ancestor is found, ordering is undefined
            // and we return the default value of isNodeAfter.
            // Count parents in each ancestor chain
            int nParents1 = 2, nParents2 = 2;  // include node & parent obtained above

            while (parent1 != null) {
                nParents1++;

                parent1 = getParentOfNode(parent1);
            }

            while (parent2 != null) {
                nParents2++;

                parent2 = getParentOfNode(parent2);
            }

            // Initially assume scan for common ancestor starts with
            // the input nodes.
            Node startNode1 = node1, startNode2 = node2;

            // If one ancestor chain is longer, adjust its start point
            // so we're comparing at the same depths
            if (nParents1 < nParents2) {
                // Adjust startNode2 to depth of startNode1
                int adjust = nParents2 - nParents1;

                for (int i = 0; i < adjust; i++) {
                    startNode2 = getParentOfNode(startNode2);
                }
            } else if (nParents1 > nParents2) {
                // adjust startNode1 to depth of startNode2
                int adjust = nParents1 - nParents2;

                for (int i = 0; i < adjust; i++) {
                    startNode1 = getParentOfNode(startNode1);
                }
            }

            Node prevChild1 = null, prevChild2 = null;  // so we can "back up"

            // Loop up the ancestor chain looking for common parent
            while (null != startNode1) {
                if (startNode1 == startNode2 || isNodeTheSame(startNode1, startNode2)) // common parent?
                {
                    if (null == prevChild1) // first time in loop?
                    {

                        // Edge condition: one is the ancestor of the other.
                        isNodeAfter = (nParents1 < nParents2) ? true : false;

                        break;  // from while loop
                    } else {
                        // Compare ancestors below lowest-common as siblings
                        isNodeAfter = isNodeAfterSibling(startNode1, prevChild1,
                                prevChild2);

                        break;  // from while loop
                    }
                }  // end if(startNode1 == startNode2)

                // Move up one level and try again
                prevChild1 = startNode1;
                startNode1 = getParentOfNode(startNode1);
                prevChild2 = startNode2;
                startNode2 = getParentOfNode(startNode2);
            }  // end while(parents exist to examine)
        }  // end big else (not immediate siblings)

        return isNodeAfter;
    }  // end isNodeAfter(Node node1, Node node2)

    /**
     * Use DTMNodeProxy to determine whether two nodes are the same.
     *
     * @param node1 The first DOM node to compare.
     * @param node2 The second DOM node to compare.
     * @return true if the two nodes are the same.
     */
    public static boolean isNodeTheSame(Node node1, Node node2) {
        if (node1 instanceof DTMNodeProxy && node2 instanceof DTMNodeProxy) {
            return ((DTMNodeProxy) node1).equals((DTMNodeProxy) node2);
        } else {
            return (node1 == node2);
        }
    }

    /**
     * Get the XPath-model parent of a node. This version takes advantage of the
     * DOM Level 2 Attr.ownerElement() method; the base version we would
     * otherwise inherit is prepared to fall back on exhaustively walking the
     * document to find an Attr's parent.
     *
     * @param node Node to be examined
     *
     * @return the DOM parent of the input node, if there is one, or the
     * ownerElement if the input node is an Attr, or null if the node is a
     * Document, a DocumentFragment, or an orphan.
     */
    public static Node getParentOfNode(Node node) {
        Node parent = node.getParentNode();
        if (parent == null && (Node.ATTRIBUTE_NODE == node.getNodeType())) {
            parent = ((Attr) node).getOwnerElement();
        }
        return parent;
    }

    /**
     * Figure out if child2 is after child1 in document order.
     * <p>
     * Warning: Some aspects of "document order" are not well defined. For
     * example, the order of attributes is considered meaningless in XML, and
     * the order reported by our model will be consistent for a given invocation
     * but may not match that of either the source file or the serialized
     * output.
     *
     * @param parent Must be the parent of both child1 and child2.
     * @param child1 Must be the child of parent and not equal to child2.
     * @param child2 Must be the child of parent and not equal to child1.
     * @return true if child 2 is after child1 in document order.
     */
    private static boolean isNodeAfterSibling(Node parent, Node child1,
            Node child2) {

        boolean isNodeAfterSibling = false;
        short child1type = child1.getNodeType();
        short child2type = child2.getNodeType();

        if ((Node.ATTRIBUTE_NODE != child1type)
                && (Node.ATTRIBUTE_NODE == child2type)) {

            // always sort attributes before non-attributes.
            isNodeAfterSibling = false;
        } else if ((Node.ATTRIBUTE_NODE == child1type)
                && (Node.ATTRIBUTE_NODE != child2type)) {

            // always sort attributes before non-attributes.
            isNodeAfterSibling = true;
        } else if (Node.ATTRIBUTE_NODE == child1type) {
            NamedNodeMap children = parent.getAttributes();
            int nNodes = children.getLength();
            boolean found1 = false, found2 = false;

            // Count from the start until we find one or the other.
            for (int i = 0; i < nNodes; i++) {
                Node child = children.item(i);

                if (child1 == child || isNodeTheSame(child1, child)) {
                    if (found2) {
                        isNodeAfterSibling = false;

                        break;
                    }

                    found1 = true;
                } else if (child2 == child || isNodeTheSame(child2, child)) {
                    if (found1) {
                        isNodeAfterSibling = true;

                        break;
                    }

                    found2 = true;
                }
            }
        } else {
            // TODO: Check performance of alternate solution:
            // There are two choices here: Count from the start of
            // the document until we find one or the other, or count
            // from one until we find or fail to find the other.
            // Either can wind up scanning all the siblings in the worst
            // case, which on a wide document can be a lot of work but
            // is more typically is a short list.
            // Scanning from the start involves two tests per iteration,
            // but it isn't clear that scanning from the middle doesn't
            // yield more iterations on average.
            // We should run some testcases.
            Node child = parent.getFirstChild();
            boolean found1 = false, found2 = false;

            while (null != child) {

                // Node child = children.item(i);
                if (child1 == child || isNodeTheSame(child1, child)) {
                    if (found2) {
                        isNodeAfterSibling = false;

                        break;
                    }

                    found1 = true;
                } else if (child2 == child || isNodeTheSame(child2, child)) {
                    if (found1) {
                        isNodeAfterSibling = true;

                        break;
                    }

                    found2 = true;
                }

                child = child.getNextSibling();
            }
        }

        return isNodeAfterSibling;
    }  // end isNodeAfterSibling(Node parent, Node child1, Node child2)
}
