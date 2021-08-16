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
/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 */
package org.jcp.xml.dsig.internal.dom;

import java.security.spec.AlgorithmParameterSpec;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.spec.ExcC14NParameterSpec;
import javax.xml.crypto.dsig.spec.XPathFilter2ParameterSpec;
import javax.xml.crypto.dsig.spec.XPathFilterParameterSpec;
import javax.xml.crypto.dsig.spec.XPathType;
import javax.xml.crypto.dsig.spec.XSLTTransformParameterSpec;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Useful static DOM utility methods.
 *
 */
public final class DOMUtils {

    // class cannot be instantiated
    private DOMUtils() {}

    /**
     * Returns the owner document of the specified node.
     *
     * @param node the node
     * @return the owner document
     */
    public static Document getOwnerDocument(Node node) {
        if (node.getNodeType() == Node.DOCUMENT_NODE) {
            return (Document)node;
        } else {
            return node.getOwnerDocument();
        }
    }

    /**
     * Create a QName string from a prefix and local name.
     *
     * @param prefix    The prefix, if any. Can be either null or empty.
     * @param localName The local name.
     *
     * @return The string for the qName, for example, "xsd:element".
     */
    public static String getQNameString(String prefix, String localName) {
        String qName = prefix == null || prefix.length() == 0
                ? localName : prefix + ":" + localName;

        return qName;
    }

    /**
     * Creates an element in the specified namespace, with the specified tag
     * and namespace prefix.
     *
     * @param doc the owner document
     * @param tag the tag
     * @param nsURI the namespace URI
     * @param prefix the namespace prefix
     * @return the newly created element
     */
    public static Element createElement(Document doc, String tag,
                                        String nsURI, String prefix)
    {
        String qName = (prefix == null || prefix.length() == 0)
                       ? tag : prefix + ":" + tag;
        return doc.createElementNS(nsURI, qName);
    }

    /**
     * Sets an element's attribute (using DOM level 2) with the
     * specified value and namespace prefix.
     *
     * @param elem the element to set the attribute on
     * @param name the name of the attribute
     * @param value the attribute value. If null, no attribute is set.
     */
    public static void setAttribute(Element elem, String name, String value) {
        if (value == null) {
            return;
        }
        elem.setAttributeNS(null, name, value);
    }

    /**
     * Sets an element's attribute (using DOM level 2) with the
     * specified value and namespace prefix AND registers the ID value with
     * the specified element. This is for resolving same-document
     * ID references.
     *
     * @param elem the element to set the attribute on
     * @param name the name of the attribute
     * @param value the attribute value. If null, no attribute is set.
     */
    public static void setAttributeID(Element elem, String name, String value) {
        if (value == null) {
            return;
        }
        elem.setAttributeNS(null, name, value);
        elem.setIdAttributeNS(null, name, true);
    }

    /**
     * Returns the first child element of the specified node, or null if there
     * is no such element.
     *
     * @param node the node
     * @return the first child element of the specified node, or null if there
     *    is no such element
     * @throws NullPointerException if {@code node == null}
     */
    public static Element getFirstChildElement(Node node) {
        Node child = node.getFirstChild();
        while (child != null && child.getNodeType() != Node.ELEMENT_NODE) {
            child = child.getNextSibling();
        }
        return (Element)child;
    }

    /**
     * Returns the first child element of the specified node and checks that
     * the local name is equal to {@code localName} and the namespace is equal to
     * {@code namespaceURI}
     *
     * @param node the node
     * @return the first child element of the specified node
     * @throws NullPointerException if {@code node == null}
     * @throws MarshalException if no such element or the local name is not
     *    equal to {@code localName}
     */
    public static Element getFirstChildElement(Node node, String localName, String namespaceURI)
        throws MarshalException
    {
        return verifyElement(getFirstChildElement(node), localName, namespaceURI);
    }

    private static Element verifyElement(Element elem, String localName, String namespaceURI)
        throws MarshalException
    {
        if (elem == null) {
            throw new MarshalException("Missing " + localName + " element");
        }
        String name = elem.getLocalName();
        String namespace = elem.getNamespaceURI();
        if (!name.equals(localName) || namespace == null && namespaceURI != null
            || namespace != null && !namespace.equals(namespaceURI)) {
            throw new MarshalException("Invalid element name: " +
                namespace + ":" + name + ", expected " + namespaceURI + ":" + localName);
        }
        return elem;
    }

    /**
     * Returns the last child element of the specified node, or null if there
     * is no such element.
     *
     * @param node the node
     * @return the last child element of the specified node, or null if there
     *    is no such element
     * @throws NullPointerException if {@code node == null}
     */
    public static Element getLastChildElement(Node node) {
        Node child = node.getLastChild();
        while (child != null && child.getNodeType() != Node.ELEMENT_NODE) {
            child = child.getPreviousSibling();
        }
        return (Element)child;
    }

    /**
     * Returns the next sibling element of the specified node, or null if there
     * is no such element.
     *
     * @param node the node
     * @return the next sibling element of the specified node, or null if there
     *    is no such element
     * @throws NullPointerException if {@code node == null}
     */
    public static Element getNextSiblingElement(Node node) {
        Node sibling = node.getNextSibling();
        while (sibling != null && sibling.getNodeType() != Node.ELEMENT_NODE) {
            sibling = sibling.getNextSibling();
        }
        return (Element)sibling;
    }

    /**
     * Returns the next sibling element of the specified node and checks that
     * the local name is equal to {@code localName} and the namespace is equal to
     * {@code namespaceURI}
     *
     * @param node the node
     * @return the next sibling element of the specified node
     * @throws NullPointerException if {@code node == null}
     * @throws MarshalException if no such element or the local name is not
     * equal to {@code localName}
     */
    public static Element getNextSiblingElement(Node node, String localName, String namespaceURI)
        throws MarshalException
    {
        return verifyElement(getNextSiblingElement(node), localName, namespaceURI);
    }

    /**
     * Returns the attribute value for the attribute with the specified name.
     * Returns null if there is no such attribute, or
     * the empty string if the attribute value is empty.
     *
     * <p>This works around a limitation of the DOM
     * {@code Element.getAttributeNode} method, which does not distinguish
     * between an unspecified attribute and an attribute with a value of
     * "" (it returns "" for both cases).
     *
     * @param elem the element containing the attribute
     * @param name the name of the attribute
     * @return the attribute value (may be null if unspecified)
     */
    public static String getAttributeValue(Element elem, String name) {
        Attr attr = elem.getAttributeNodeNS(null, name);
        return (attr == null) ? null : attr.getValue();
    }

    /**
     * Returns the attribute value for the attribute with the specified name.
     * Returns null if there is no such attribute, or
     * the empty string if the attribute value is empty.
     *
     * <p>This works around a limitation of the DOM
     * {@code Element.getAttributeNode} method, which does not distinguish
     * between an unspecified attribute and an attribute with a value of
     * "" (it returns "" for both cases).
     *
     * @param elem the element containing the attribute
     * @param name the name of the attribute
     * @return the attribute value (may be null if unspecified)
     */
    public static <N> String getIdAttributeValue(Element elem, String name) {
        Attr attr = elem.getAttributeNodeNS(null, name);
        if (attr != null && !attr.isId()) {
            elem.setIdAttributeNode(attr, true);
        }
        return (attr == null) ? null : attr.getValue();
    }

    /**
     * Returns the prefix associated with the specified namespace URI
     *
     * @param context contains the namespace map
     * @param nsURI the namespace URI
     * @return the prefix associated with the specified namespace URI, or
     *    null if not set
     */
    public static String getNSPrefix(XMLCryptoContext context, String nsURI) {
        if (context != null) {
            return context.getNamespacePrefix
                (nsURI, context.getDefaultNamespacePrefix());
        } else {
            return null;
        }
    }

    /**
     * Returns the prefix associated with the XML Signature namespace URI
     *
     * @param context contains the namespace map
     * @return the prefix associated with the specified namespace URI, or
     *    null if not set
     */
    public static String getSignaturePrefix(XMLCryptoContext context) {
        return getNSPrefix(context, XMLSignature.XMLNS);
    }

    /**
     * Removes all children nodes from the specified node.
     *
     * @param node the parent node whose children are to be removed
     */
    public static void removeAllChildren(Node node) {
        Node firstChild = node.getFirstChild();
        while (firstChild != null) {
            Node nodeToRemove = firstChild;
            firstChild = firstChild.getNextSibling();
            node.removeChild(nodeToRemove);
        }
    }

    /**
     * Compares 2 nodes for equality.
     */
    public static boolean nodesEqual(Node thisNode, Node otherNode) {
        return thisNode.isEqualNode(otherNode);
    }

    /**
     * Checks if child element has same owner document before
     * appending to the parent, and imports it to the parent's document
     * if necessary.
     */
    public static void appendChild(Node parent, Node child) {
        Document ownerDoc = getOwnerDocument(parent);
        if (child.getOwnerDocument() != ownerDoc) {
            parent.appendChild(ownerDoc.importNode(child, true));
        } else {
            parent.appendChild(child);
        }
    }

    public static boolean paramsEqual(AlgorithmParameterSpec spec1,
        AlgorithmParameterSpec spec2) {
        if (spec1 == spec2) {
            return true;
        }
        if (spec1 instanceof XPathFilter2ParameterSpec &&
            spec2 instanceof XPathFilter2ParameterSpec) {
            return paramsEqual((XPathFilter2ParameterSpec)spec1,
                               (XPathFilter2ParameterSpec)spec2);
        }
        if (spec1 instanceof ExcC14NParameterSpec &&
            spec2 instanceof ExcC14NParameterSpec) {
            return paramsEqual((ExcC14NParameterSpec) spec1,
                               (ExcC14NParameterSpec)spec2);
        }
        if (spec1 instanceof XPathFilterParameterSpec &&
            spec2 instanceof XPathFilterParameterSpec) {
            return paramsEqual((XPathFilterParameterSpec)spec1,
                               (XPathFilterParameterSpec)spec2);
        }
        if (spec1 instanceof XSLTTransformParameterSpec &&
            spec2 instanceof XSLTTransformParameterSpec) {
            return paramsEqual((XSLTTransformParameterSpec)spec1,
                               (XSLTTransformParameterSpec)spec2);
        }
        return false;
    }

    private static boolean paramsEqual(XPathFilter2ParameterSpec spec1,
                                       XPathFilter2ParameterSpec spec2)
    {
        @SuppressWarnings("unchecked")
        List<XPathType> types = spec1.getXPathList();
        @SuppressWarnings("unchecked")
        List<XPathType> otypes = spec2.getXPathList();
        int size = types.size();
        if (size != otypes.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            XPathType type = types.get(i);
            XPathType otype = otypes.get(i);
            if (!type.getExpression().equals(otype.getExpression()) ||
                !type.getNamespaceMap().equals(otype.getNamespaceMap()) ||
                type.getFilter() != otype.getFilter()) {
                return false;
            }
        }
        return true;
    }

    private static boolean paramsEqual(ExcC14NParameterSpec spec1,
                                       ExcC14NParameterSpec spec2)
    {
        return spec1.getPrefixList().equals(spec2.getPrefixList());
    }

    private static boolean paramsEqual(XPathFilterParameterSpec spec1,
                                       XPathFilterParameterSpec spec2)
    {
        return spec1.getXPath().equals(spec2.getXPath()) &&
                spec1.getNamespaceMap().equals(spec2.getNamespaceMap());
    }

    private static boolean paramsEqual(XSLTTransformParameterSpec spec1,
                                       XSLTTransformParameterSpec spec2)
    {

        XMLStructure ostylesheet = spec2.getStylesheet();
        if (!(ostylesheet instanceof javax.xml.crypto.dom.DOMStructure)) {
            return false;
        }
        Node ostylesheetElem =
            ((javax.xml.crypto.dom.DOMStructure) ostylesheet).getNode();
        XMLStructure stylesheet = spec1.getStylesheet();
        Node stylesheetElem =
            ((javax.xml.crypto.dom.DOMStructure) stylesheet).getNode();
        return nodesEqual(stylesheetElem, ostylesheetElem);
    }

    public static boolean isNamespace(Node node)
    {
        if (Node.ATTRIBUTE_NODE == node.getNodeType()) {
            final String namespaceURI = node.getNamespaceURI();
            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI);
        }
        return false;
    }
}
