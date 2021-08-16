/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sun.org.apache.xml.internal.security.transforms.implementations;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.transform.TransformerException;

import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;
import com.sun.org.apache.xml.internal.security.signature.NodeFilter;
import com.sun.org.apache.xml.internal.security.signature.XMLSignatureInput;
import com.sun.org.apache.xml.internal.security.transforms.TransformSpi;
import com.sun.org.apache.xml.internal.security.transforms.TransformationException;
import com.sun.org.apache.xml.internal.security.transforms.Transforms;
import com.sun.org.apache.xml.internal.security.transforms.params.XPath2FilterContainer;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import com.sun.org.apache.xml.internal.security.utils.XPathAPI;
import com.sun.org.apache.xml.internal.security.utils.XPathFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implements the <I>XML Signature XPath Filter v2.0</I>
 *
 * @see <A HREF="http://www.w3.org/TR/xmldsig-filter2/">XPath Filter v2.0 (TR)</A>
 */
public class TransformXPath2Filter extends TransformSpi {

    /**
     * {@inheritDoc}
     */
    @Override
    protected String engineGetURI() {
        return Transforms.TRANSFORM_XPATH2FILTER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected XMLSignatureInput enginePerformTransform(
        XMLSignatureInput input, OutputStream os, Element transformElement,
        String baseURI, boolean secureValidation
    ) throws TransformationException {
        try {
            List<NodeList> unionNodes = new ArrayList<>();
            List<NodeList> subtractNodes = new ArrayList<>();
            List<NodeList> intersectNodes = new ArrayList<>();

            Element[] xpathElements =
                XMLUtils.selectNodes(
                    transformElement.getFirstChild(),
                    XPath2FilterContainer.XPathFilter2NS,
                    XPath2FilterContainer._TAG_XPATH2
                );
            if (xpathElements.length == 0) {
                Object[] exArgs = { Transforms.TRANSFORM_XPATH2FILTER, "XPath" };

                throw new TransformationException("xml.WrongContent", exArgs);
            }

            Document inputDoc = null;
            if (input.getSubNode() != null) {
                inputDoc = XMLUtils.getOwnerDocument(input.getSubNode());
            } else {
                inputDoc = XMLUtils.getOwnerDocument(input.getNodeSet());
            }

            XPathFactory xpathFactory = XPathFactory.newInstance();
            for (int i = 0; i < xpathElements.length; i++) {
                Element xpathElement = xpathElements[i];

                XPath2FilterContainer xpathContainer =
                    XPath2FilterContainer.newInstance(xpathElement, input.getSourceURI());

                String str =
                    XMLUtils.getStrFromNode(xpathContainer.getXPathFilterTextNode());

                XPathAPI xpathAPIInstance = xpathFactory.newXPathAPI();

                NodeList subtreeRoots =
                    xpathAPIInstance.selectNodeList(
                        inputDoc,
                        xpathContainer.getXPathFilterTextNode(),
                        str,
                        xpathContainer.getElement());
                if (xpathContainer.isIntersect()) {
                    intersectNodes.add(subtreeRoots);
                } else if (xpathContainer.isSubtract()) {
                    subtractNodes.add(subtreeRoots);
                } else if (xpathContainer.isUnion()) {
                    unionNodes.add(subtreeRoots);
                }
            }

            input.addNodeFilter(
                new XPath2NodeFilter(unionNodes, subtractNodes, intersectNodes)
            );
            input.setNodeSet(true);
            return input;
        } catch (TransformerException | DOMException | XMLSecurityException | IOException ex) {
            throw new TransformationException(ex);
        }
    }
}

class XPath2NodeFilter implements NodeFilter {

    private final boolean hasUnionFilter;
    private final boolean hasSubtractFilter;
    private final boolean hasIntersectFilter;
    private final Set<Node> unionNodes;
    private final Set<Node> subtractNodes;
    private final Set<Node> intersectNodes;
    private int inSubtract = -1;
    private int inIntersect = -1;
    private int inUnion = -1;

    XPath2NodeFilter(List<NodeList> unionNodes, List<NodeList> subtractNodes,
                     List<NodeList> intersectNodes) {
        hasUnionFilter = !unionNodes.isEmpty();
        this.unionNodes = convertNodeListToSet(unionNodes);
        hasSubtractFilter = !subtractNodes.isEmpty();
        this.subtractNodes = convertNodeListToSet(subtractNodes);
        hasIntersectFilter = !intersectNodes.isEmpty();
        this.intersectNodes = convertNodeListToSet(intersectNodes);
    }

    /**
     * @see com.sun.org.apache.xml.internal.security.signature.NodeFilter#isNodeInclude(org.w3c.dom.Node)
     */
    public int isNodeInclude(Node currentNode) {
        int result = 1;

        if (hasSubtractFilter && rooted(currentNode, subtractNodes)) {
            result = -1;
        } else if (hasIntersectFilter && !rooted(currentNode, intersectNodes)) {
            result = 0;
        }

        //TODO OPTIMIZE
        if (result == 1) {
            return 1;
        }
        if (hasUnionFilter) {
            if (rooted(currentNode, unionNodes)) {
                return 1;
            }
            result = 0;
        }
        return result;
    }

    public int isNodeIncludeDO(Node n, int level) {
        int result = 1;
        if (hasSubtractFilter) {
            if (inSubtract == -1 || level <= inSubtract) {
                if (inList(n, subtractNodes)) {
                    inSubtract = level;
                } else {
                    inSubtract = -1;
                }
            }
            if (inSubtract != -1){
                result = -1;
            }
        }
        if (result != -1 && hasIntersectFilter
            && (inIntersect == -1 || level <= inIntersect)) {
            if (!inList(n, intersectNodes)) {
                inIntersect = -1;
                result = 0;
            } else {
                inIntersect = level;
            }
        }

        if (level <= inUnion) {
            inUnion = -1;
        }
        if (result == 1) {
            return 1;
        }
        if (hasUnionFilter) {
            if (inUnion == -1 && inList(n, unionNodes)) {
                inUnion = level;
            }
            if (inUnion != -1) {
                return 1;
            }
            result = 0;
        }

        return result;
    }

    /**
     * Method rooted
     * @param currentNode
     * @param nodeList
     *
     * @return if rooted bye the rootnodes
     */
    static boolean rooted(Node currentNode, Set<Node> nodeList) {
        if (nodeList.isEmpty()) {
            return false;
        }
        if (nodeList.contains(currentNode)) {
            return true;
        }
        for (Node rootNode : nodeList) {
            if (XMLUtils.isDescendantOrSelf(rootNode, currentNode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method rooted
     * @param currentNode
     * @param nodeList
     *
     * @return if rooted bye the rootnodes
     */
    static boolean inList(Node currentNode, Set<Node> nodeList) {
        return nodeList.contains(currentNode);
    }

    private static Set<Node> convertNodeListToSet(List<NodeList> l) {
        Set<Node> result = new HashSet<>();
        for (NodeList rootNodes : l) {
            int length = rootNodes.getLength();

            for (int i = 0; i < length; i++) {
                Node rootNode = rootNodes.item(i);
                result.add(rootNode);
            }
        }
        return result;
    }
}
