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
package com.sun.org.apache.xml.internal.security.utils;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An implementation for XPath evaluation that uses the JDK API.
 */
class JDKXPathAPI implements XPathAPI {

    private XPathFactory xpf;

    private String xpathStr;

    private XPathExpression xpathExpression;

    /**
     *  Use an XPath string to select a nodelist.
     *  XPath namespace prefixes are resolved from the namespaceNode.
     *
     *  @param contextNode The node to start searching from.
     *  @param xpathnode
     *  @param str
     *  @param namespaceNode The node from which prefixes in the XPath will be resolved to namespaces.
     *  @return A NodeIterator, should never be null.
     *
     * @throws TransformerException
     */
    public NodeList selectNodeList(
        Node contextNode, Node xpathnode, String str, Node namespaceNode
    ) throws TransformerException {
        if (!str.equals(xpathStr) || xpathExpression == null) {
            if (xpf == null) {
                xpf = XPathFactory.newInstance();
                try {
                    xpf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
                } catch (XPathFactoryConfigurationException ex) {
                    throw new TransformerException(ex);
                }
            }
            XPath xpath = xpf.newXPath();
            xpath.setNamespaceContext(new DOMNamespaceContext(namespaceNode));
            xpathStr = str;
            try {
                xpathExpression = xpath.compile(xpathStr);
            } catch (XPathExpressionException ex) {
                throw new TransformerException(ex);
            }
        }
        try {
            return (NodeList)xpathExpression.evaluate(contextNode, XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
            throw new TransformerException(ex);
        }
    }

    /**
     * Evaluate an XPath string and return true if the output is to be included or not.
     *  @param contextNode The node to start searching from.
     *  @param xpathnode The XPath node
     *  @param str The XPath expression
     *  @param namespaceNode The node from which prefixes in the XPath will be resolved to namespaces.
     */
    public boolean evaluate(Node contextNode, Node xpathnode, String str, Node namespaceNode)
        throws TransformerException {
        if (!str.equals(xpathStr) || xpathExpression == null) {
            if (xpf == null) {
                xpf = XPathFactory.newInstance();
                try {
                    xpf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
                } catch (XPathFactoryConfigurationException ex) {
                    throw new TransformerException(ex);
                }
            }
            XPath xpath = xpf.newXPath();
            xpath.setNamespaceContext(new DOMNamespaceContext(namespaceNode));
            xpathStr = str;
            try {
                xpathExpression = xpath.compile(xpathStr);
            } catch (XPathExpressionException ex) {
                throw new TransformerException(ex);
            }
        }
        try {
            return (Boolean)xpathExpression.evaluate(contextNode, XPathConstants.BOOLEAN);
        } catch (XPathExpressionException ex) {
            throw new TransformerException(ex);
        }
    }

    /**
     * Clear any context information from this object
     */
    public void clear() {
        xpathStr = null;
        xpathExpression = null;
        xpf = null;
    }

}
