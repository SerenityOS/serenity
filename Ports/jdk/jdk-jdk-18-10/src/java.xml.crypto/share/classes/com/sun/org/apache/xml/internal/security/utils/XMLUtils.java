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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.org.apache.xml.internal.security.c14n.CanonicalizationException;
import com.sun.org.apache.xml.internal.security.c14n.Canonicalizer;
import com.sun.org.apache.xml.internal.security.c14n.InvalidCanonicalizerException;
import com.sun.org.apache.xml.internal.security.parser.XMLParser;
import com.sun.org.apache.xml.internal.security.parser.XMLParserException;
import com.sun.org.apache.xml.internal.security.parser.XMLParserImpl;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * DOM and XML accessibility and comfort functions.
 *
 */
public final class XMLUtils {

    @SuppressWarnings("removal")
    private static boolean ignoreLineBreaks =
            AccessController.doPrivileged(
                    (PrivilegedAction<Boolean>) () -> Boolean.getBoolean("com.sun.org.apache.xml.internal.security.ignoreLineBreaks"));

    private static final com.sun.org.slf4j.internal.Logger LOG =
            com.sun.org.slf4j.internal.LoggerFactory.getLogger(XMLUtils.class);

    @SuppressWarnings("removal")
    private static XMLParser xmlParserImpl =
            AccessController.doPrivileged(
                    (PrivilegedAction<XMLParser>) () -> {
                        String xmlParserClass = System.getProperty("com.sun.org.apache.xml.internal.security.XMLParser");
                        if (xmlParserClass != null) {
                            try {
                                return (XMLParser) JavaUtils.newInstanceWithEmptyConstructor(
                                        ClassLoaderUtils.loadClass(xmlParserClass, XMLUtils.class));
                            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                                LOG.error("Error instantiating XMLParser. Falling back to XMLParserImpl");
                            }
                        }
                        return new XMLParserImpl();
                    });

    private static volatile String dsPrefix = "ds";
    private static volatile String ds11Prefix = "dsig11";
    private static volatile String xencPrefix = "xenc";
    private static volatile String xenc11Prefix = "xenc11";

    /**
     * Constructor XMLUtils
     *
     */
    private XMLUtils() {
        // we don't allow instantiation
    }

    /**
     * Set the prefix for the digital signature namespace
     * @param prefix the new prefix for the digital signature namespace
     * @throws SecurityException if a security manager is installed and the
     *    caller does not have permission to set the prefix
     */
    public static void setDsPrefix(String prefix) {
        JavaUtils.checkRegisterPermission();
        dsPrefix = prefix;
    }

    /**
     * Set the prefix for the digital signature 1.1 namespace
     * @param prefix the new prefix for the digital signature 1.1 namespace
     * @throws SecurityException if a security manager is installed and the
     *    caller does not have permission to set the prefix
     */
    public static void setDs11Prefix(String prefix) {
        JavaUtils.checkRegisterPermission();
        ds11Prefix = prefix;
    }

    /**
     * Set the prefix for the encryption namespace
     * @param prefix the new prefix for the encryption namespace
     * @throws SecurityException if a security manager is installed and the
     *    caller does not have permission to set the prefix
     */
    public static void setXencPrefix(String prefix) {
        JavaUtils.checkRegisterPermission();
        xencPrefix = prefix;
    }

    /**
     * Set the prefix for the encryption namespace 1.1
     * @param prefix the new prefix for the encryption namespace 1.1
     * @throws SecurityException if a security manager is installed and the
     *    caller does not have permission to set the prefix
     */
    public static void setXenc11Prefix(String prefix) {
        JavaUtils.checkRegisterPermission();
        xenc11Prefix = prefix;
    }

    public static Element getNextElement(Node el) {
        Node node = el;
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
            node = node.getNextSibling();
        }
        return (Element)node;
    }

    /**
     * @param rootNode
     * @param result
     * @param exclude
     * @param com whether comments or not
     */
    public static void getSet(Node rootNode, Set<Node> result, Node exclude, boolean com) {
        if (exclude != null && isDescendantOrSelf(exclude, rootNode)) {
            return;
        }
        getSetRec(rootNode, result, exclude, com);
    }

    @SuppressWarnings("fallthrough")
    private static void getSetRec(final Node rootNode, final Set<Node> result,
                                final Node exclude, final boolean com) {
        if (rootNode == exclude) {
            return;
        }
        switch (rootNode.getNodeType()) { //NOPMD
        case Node.ELEMENT_NODE:
            result.add(rootNode);
            Element el = (Element)rootNode;
            if (el.hasAttributes()) {
                NamedNodeMap nl = el.getAttributes();
                int length = nl.getLength();
                for (int i = 0; i < length; i++) {
                    result.add(nl.item(i));
                }
            }
            //no return keep working
        case Node.DOCUMENT_NODE:
            for (Node r = rootNode.getFirstChild(); r != null; r = r.getNextSibling()) {
                if (r.getNodeType() == Node.TEXT_NODE) {
                    result.add(r);
                    while (r != null && r.getNodeType() == Node.TEXT_NODE) {
                        r = r.getNextSibling();
                    }
                    if (r == null) {
                        return;
                    }
                }
                getSetRec(r, result, exclude, com);
            }
            break;
        case Node.COMMENT_NODE:
            if (com) {
                result.add(rootNode);
            }
            break;
        case Node.DOCUMENT_TYPE_NODE:
            break;
        default:
            result.add(rootNode);
        }
    }


    /**
     * Outputs a DOM tree to an {@link OutputStream}.
     *
     * @param contextNode root node of the DOM tree
     * @param os the {@link OutputStream}
     */
    public static void outputDOM(Node contextNode, OutputStream os) {
        XMLUtils.outputDOM(contextNode, os, false);
    }

    /**
     * Outputs a DOM tree to an {@link OutputStream}. <I>If an Exception is
     * thrown during execution, it's StackTrace is output to System.out, but the
     * Exception is not re-thrown.</I>
     *
     * @param contextNode root node of the DOM tree
     * @param os the {@link OutputStream}
     * @param addPreamble
     */
    public static void outputDOM(Node contextNode, OutputStream os, boolean addPreamble) {
        try {
            if (addPreamble) {
                os.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            Canonicalizer.getInstance(
                Canonicalizer.ALGO_ID_C14N_PHYSICAL).canonicalizeSubtree(contextNode, os);
        } catch (IOException | InvalidCanonicalizerException | CanonicalizationException ex) {
            LOG.debug(ex.getMessage(), ex);
        }
    }

    /**
     * Serializes the {@code contextNode} into the OutputStream, <I>but
     * suppresses all Exceptions</I>.
     * <p></p>
     * NOTE: <I>This should only be used for debugging purposes,
     * NOT in a production environment; this method ignores all exceptions,
     * so you won't notice if something goes wrong. If you're asking what is to
     * be used in a production environment, simply use the code inside the
     * {@code try{}} statement, but handle the Exceptions appropriately.</I>
     *
     * @param contextNode
     * @param os
     */
    public static void outputDOMc14nWithComments(Node contextNode, OutputStream os) {
        try {
            Canonicalizer.getInstance(
                Canonicalizer.ALGO_ID_C14N_WITH_COMMENTS).canonicalizeSubtree(contextNode, os);
        } catch (InvalidCanonicalizerException | CanonicalizationException ex) {
            LOG.debug(ex.getMessage(), ex);
            // throw new RuntimeException(ex.getMessage());
        }
    }

    /**
     * Method getFullTextChildrenFromNode
     *
     * @param node
     * @return the string of children
     */
    public static String getFullTextChildrenFromNode(Node node) {
        StringBuilder sb = new StringBuilder();

        Node child = node.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.TEXT_NODE) {
                sb.append(((Text)child).getData());
            }
            child = child.getNextSibling();
        }

        return sb.toString();
    }

    /**
     * Creates an Element in the XML Signature specification namespace.
     *
     * @param doc the factory Document
     * @param elementName the local name of the Element
     * @return the Element
     */
    public static Element createElementInSignatureSpace(Document doc, String elementName) {
        if (doc == null) {
            throw new RuntimeException("Document is null");
        }

        if (dsPrefix == null || dsPrefix.length() == 0) {
            return doc.createElementNS(Constants.SignatureSpecNS, elementName);
        }
        return doc.createElementNS(Constants.SignatureSpecNS, dsPrefix + ":" + elementName);
    }

    /**
     * Creates an Element in the XML Signature 1.1 specification namespace.
     *
     * @param doc the factory Document
     * @param elementName the local name of the Element
     * @return the Element
     */
    public static Element createElementInSignature11Space(Document doc, String elementName) {
        if (doc == null) {
            throw new RuntimeException("Document is null");
        }

        if (ds11Prefix == null || ds11Prefix.length() == 0) {
            return doc.createElementNS(Constants.SignatureSpec11NS, elementName);
        }
        return doc.createElementNS(Constants.SignatureSpec11NS, ds11Prefix + ":" + elementName);
    }

    /**
     * Creates an Element in the XML Encryption specification namespace.
     *
     * @param doc the factory Document
     * @param elementName the local name of the Element
     * @return the Element
     */
    public static Element createElementInEncryptionSpace(Document doc, String elementName) {
        if (doc == null) {
            throw new RuntimeException("Document is null");
        }

        if (xencPrefix == null || xencPrefix.length() == 0) {
            return doc.createElementNS(EncryptionConstants.EncryptionSpecNS, elementName);
        }
        return
            doc.createElementNS(
                EncryptionConstants.EncryptionSpecNS, xencPrefix + ":" + elementName
            );
    }

    /**
     * Creates an Element in the XML Encryption 1.1 specification namespace.
     *
     * @param doc the factory Document
     * @param elementName the local name of the Element
     * @return the Element
     */
    public static Element createElementInEncryption11Space(Document doc, String elementName) {
        if (doc == null) {
            throw new RuntimeException("Document is null");
        }

        if (xenc11Prefix == null || xenc11Prefix.length() == 0) {
            return doc.createElementNS(EncryptionConstants.EncryptionSpec11NS, elementName);
        }
        return
            doc.createElementNS(
                EncryptionConstants.EncryptionSpec11NS, xenc11Prefix + ":" + elementName
            );
    }

    /**
     * Returns true if the element is in XML Signature namespace and the local
     * name equals the supplied one.
     *
     * @param element
     * @param localName
     * @return true if the element is in XML Signature namespace and the local name equals
     * the supplied one
     */
    public static boolean elementIsInSignatureSpace(Element element, String localName) {
        if (element == null){
            return false;
        }

        return Constants.SignatureSpecNS.equals(element.getNamespaceURI())
            && element.getLocalName().equals(localName);
    }

    /**
     * Returns true if the element is in XML Signature 1.1 namespace and the local
     * name equals the supplied one.
     *
     * @param element
     * @param localName
     * @return true if the element is in XML Signature namespace and the local name equals
     * the supplied one
     */
    public static boolean elementIsInSignature11Space(Element element, String localName) {
        if (element == null) {
            return false;
        }

        return Constants.SignatureSpec11NS.equals(element.getNamespaceURI())
            && element.getLocalName().equals(localName);
    }

    /**
     * Returns true if the element is in XML Encryption namespace and the local
     * name equals the supplied one.
     *
     * @param element
     * @param localName
     * @return true if the element is in XML Encryption namespace and the local name
     * equals the supplied one
     */
    public static boolean elementIsInEncryptionSpace(Element element, String localName) {
        if (element == null){
            return false;
        }
        return EncryptionConstants.EncryptionSpecNS.equals(element.getNamespaceURI())
            && element.getLocalName().equals(localName);
    }

    /**
     * Returns true if the element is in XML Encryption 1.1 namespace and the local
     * name equals the supplied one.
     *
     * @param element
     * @param localName
     * @return true if the element is in XML Encryption 1.1 namespace and the local name
     * equals the supplied one
     */
    public static boolean elementIsInEncryption11Space(Element element, String localName) {
        if (element == null){
            return false;
        }
        return EncryptionConstants.EncryptionSpec11NS.equals(element.getNamespaceURI())
            && element.getLocalName().equals(localName);
    }

    /**
     * This method returns the owner document of a particular node.
     * This method is necessary because it <I>always</I> returns a
     * {@link Document}. {@link Node#getOwnerDocument} returns {@code null}
     * if the {@link Node} is a {@link Document}.
     *
     * @param node
     * @return the owner document of the node
     */
    public static Document getOwnerDocument(Node node) {
        if (node.getNodeType() == Node.DOCUMENT_NODE) {
            return (Document) node;
        }
        try {
            return node.getOwnerDocument();
        } catch (NullPointerException npe) {
            throw new NullPointerException(I18n.translate("endorsed.jdk1.4.0")
                                           + " Original message was \""
                                           + npe.getMessage() + "\"");
        }
    }

    /**
     * This method returns the first non-null owner document of the Nodes in this Set.
     * This method is necessary because it <I>always</I> returns a
     * {@link Document}. {@link Node#getOwnerDocument} returns {@code null}
     * if the {@link Node} is a {@link Document}.
     *
     * @param xpathNodeSet
     * @return the owner document
     */
    public static Document getOwnerDocument(Set<Node> xpathNodeSet) {
        NullPointerException npe = null;
        for (Node node : xpathNodeSet) {
            int nodeType = node.getNodeType();
            if (nodeType == Node.DOCUMENT_NODE) {
                return (Document) node;
            }
            try {
                if (nodeType == Node.ATTRIBUTE_NODE) {
                    return ((Attr)node).getOwnerElement().getOwnerDocument();
                }
                return node.getOwnerDocument();
            } catch (NullPointerException e) {
                npe = e;
            }
        }

        throw new NullPointerException(I18n.translate("endorsed.jdk1.4.0")
                                       + " Original message was \""
                                       + (npe == null ? "" : npe.getMessage()) + "\"");
    }

    /**
     * Method addReturnToElement
     *
     * @param e
     */
    public static void addReturnToElement(Element e) {
        if (!ignoreLineBreaks) {
            Document doc = e.getOwnerDocument();
            e.appendChild(doc.createTextNode("\n"));
        }
    }

    public static void addReturnToElement(Document doc, HelperNodeList nl) {
        if (!ignoreLineBreaks) {
            nl.appendChild(doc.createTextNode("\n"));
        }
    }

    public static void addReturnBeforeChild(Element e, Node child) {
        if (!ignoreLineBreaks) {
            Document doc = e.getOwnerDocument();
            e.insertBefore(doc.createTextNode("\n"), child);
        }
    }

    public static String encodeToString(byte[] bytes) {
        if (ignoreLineBreaks) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        return Base64.getMimeEncoder().encodeToString(bytes);
    }

    public static byte[] decode(String encodedString) {
        return Base64.getMimeDecoder().decode(encodedString);
    }

    public static byte[] decode(byte[] encodedBytes) {
        return Base64.getMimeDecoder().decode(encodedBytes);
    }

    public static boolean isIgnoreLineBreaks() {
        return ignoreLineBreaks;
    }

    /**
     * Method convertNodelistToSet
     *
     * @param xpathNodeSet
     * @return the set with the nodelist
     */
    public static Set<Node> convertNodelistToSet(NodeList xpathNodeSet) {
        if (xpathNodeSet == null) {
            return new HashSet<>();
        }

        int length = xpathNodeSet.getLength();
        Set<Node> set = new HashSet<>(length);

        for (int i = 0; i < length; i++) {
            set.add(xpathNodeSet.item(i));
        }

        return set;
    }

    /**
     * This method spreads all namespace attributes in a DOM document to their
     * children. This is needed because the XML Signature XPath transform
     * must evaluate the XPath against all nodes in the input, even against
     * XPath namespace nodes. Through a bug in XalanJ2, the namespace nodes are
     * not fully visible in the Xalan XPath model, so we have to do this by
     * hand in DOM spaces so that the nodes become visible in XPath space.
     *
     * @param doc
     * @see <A HREF="http://nagoya.apache.org/bugzilla/show_bug.cgi?id=2650">
     * Namespace axis resolution is not XPath compliant </A>
     */
    public static void circumventBug2650(Document doc) {

        Element documentElement = doc.getDocumentElement();

        // if the document element has no xmlns definition, we add xmlns=""
        Attr xmlnsAttr =
            documentElement.getAttributeNodeNS(Constants.NamespaceSpecNS, "xmlns");

        if (xmlnsAttr == null) {
            documentElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns", "");
        }

        XMLUtils.circumventBug2650internal(doc);
    }

    /**
     * This is the work horse for {@link #circumventBug2650}.
     *
     * @param node
     * @see <A HREF="http://nagoya.apache.org/bugzilla/show_bug.cgi?id=2650">
     * Namespace axis resolution is not XPath compliant </A>
     */
    @SuppressWarnings("fallthrough")
    private static void circumventBug2650internal(Node node) {
        Node parent = null;
        Node sibling = null;
        final String namespaceNs = Constants.NamespaceSpecNS;
        do {
            switch (node.getNodeType()) {
            case Node.ELEMENT_NODE :
                Element element = (Element) node;
                if (!element.hasChildNodes()) {
                    break;
                }
                if (element.hasAttributes()) {
                    NamedNodeMap attributes = element.getAttributes();
                    int attributesLength = attributes.getLength();

                    for (Node child = element.getFirstChild(); child!=null;
                        child = child.getNextSibling()) {

                        if (child.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        Element childElement = (Element) child;

                        for (int i = 0; i < attributesLength; i++) {
                            Attr currentAttr = (Attr) attributes.item(i);
                            if (!namespaceNs.equals(currentAttr.getNamespaceURI())) {
                                continue;
                            }
                            if (childElement.hasAttributeNS(namespaceNs,
                                                            currentAttr.getLocalName())) {
                                continue;
                            }
                            childElement.setAttributeNS(namespaceNs,
                                                        currentAttr.getName(),
                                                        currentAttr.getNodeValue());
                        }
                    }
                }
            case Node.ENTITY_REFERENCE_NODE :
            case Node.DOCUMENT_NODE :
                parent = node;
                sibling = node.getFirstChild();
                break;
            }
            while (sibling == null && parent != null) {
                sibling = parent.getNextSibling();
                parent = parent.getParentNode();
            }
            if (sibling == null) {
                return;
            }

            node = sibling;
            sibling = node.getNextSibling();
        } while (true);
    }

    /**
     * @param sibling
     * @param nodeName
     * @param number
     * @return nodes with the constraint
     */
    public static Element selectDsNode(Node sibling, String nodeName, int number) {
        while (sibling != null) {
            if (Constants.SignatureSpecNS.equals(sibling.getNamespaceURI())
                && sibling.getLocalName().equals(nodeName)) {
                if (number == 0) {
                    return (Element)sibling;
                }
                number--;
            }
            sibling = sibling.getNextSibling();
        }
        return null;
    }

    /**
     * @param sibling
     * @param nodeName
     * @param number
     * @return nodes with the constraint
     */
    public static Element selectDs11Node(Node sibling, String nodeName, int number) {
        while (sibling != null) {
            if (Constants.SignatureSpec11NS.equals(sibling.getNamespaceURI())
                && sibling.getLocalName().equals(nodeName)) {
                if (number == 0) {
                    return (Element)sibling;
                }
                number--;
            }
            sibling = sibling.getNextSibling();
        }
        return null;
    }

    /**
     * @param sibling
     * @param nodeName
     * @param number
     * @return nodes with the constrain
     */
    public static Element selectXencNode(Node sibling, String nodeName, int number) {
        while (sibling != null) {
            if (EncryptionConstants.EncryptionSpecNS.equals(sibling.getNamespaceURI())
                && sibling.getLocalName().equals(nodeName)) {
                if (number == 0){
                    return (Element)sibling;
                }
                number--;
            }
            sibling = sibling.getNextSibling();
        }
        return null;
    }

    /**
     * @param sibling
     * @param uri
     * @param nodeName
     * @param number
     * @return nodes with the constrain
     */
    public static Element selectNode(Node sibling, String uri, String nodeName, int number) {
        while (sibling != null) {
            if (sibling.getNamespaceURI() != null && sibling.getNamespaceURI().equals(uri)
                && sibling.getLocalName().equals(nodeName)) {
                if (number == 0) {
                    return (Element)sibling;
                }
                number--;
            }
            sibling = sibling.getNextSibling();
        }
        return null;
    }

    /**
     * @param sibling
     * @param nodeName
     * @return nodes with the constrain
     */
    public static Element[] selectDsNodes(Node sibling, String nodeName) {
        return selectNodes(sibling, Constants.SignatureSpecNS, nodeName);
    }

    /**
     * @param sibling
     * @param nodeName
     * @return nodes with the constrain
     */
    public static Element[] selectDs11Nodes(Node sibling, String nodeName) {
        return selectNodes(sibling, Constants.SignatureSpec11NS, nodeName);
    }

    /**
     * @param sibling
     * @param uri
     * @param nodeName
     * @return nodes with the constraint
     */
    public static Element[] selectNodes(Node sibling, String uri, String nodeName) {
        List<Element> list = new ArrayList<>();
        while (sibling != null) {
            if (sibling.getNamespaceURI() != null && sibling.getNamespaceURI().equals(uri)
                && sibling.getLocalName().equals(nodeName)) {
                list.add((Element)sibling);
            }
            sibling = sibling.getNextSibling();
        }
        return list.toArray(new Element[list.size()]);
    }

    /**
     * @param signatureElement
     * @param inputSet
     * @return nodes with the constrain
     */
    public static Set<Node> excludeNodeFromSet(Node signatureElement, Set<Node> inputSet) {
        return inputSet.stream().filter((inputNode) ->
                !XMLUtils.isDescendantOrSelf(signatureElement, inputNode)).collect(Collectors.toSet());
    }

    /**
     * Method getStrFromNode
     *
     * @param xpathnode
     * @return the string for the node.
     */
    public static String getStrFromNode(Node xpathnode) {
        if (xpathnode.getNodeType() == Node.TEXT_NODE) {
            // we iterate over all siblings of the context node because eventually,
            // the text is "polluted" with pi's or comments
            StringBuilder sb = new StringBuilder();

            for (Node currentSibling = xpathnode.getParentNode().getFirstChild();
                currentSibling != null;
                currentSibling = currentSibling.getNextSibling()) {
                if (currentSibling.getNodeType() == Node.TEXT_NODE) {
                    sb.append(((Text) currentSibling).getData());
                }
            }

            return sb.toString();
        } else if (xpathnode.getNodeType() == Node.ATTRIBUTE_NODE) {
            return xpathnode.getNodeValue();
        } else if (xpathnode.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
            return xpathnode.getNodeValue();
        }

        return null;
    }

    /**
     * Returns true if the descendantOrSelf is on the descendant-or-self axis
     * of the context node.
     *
     * @param ctx
     * @param descendantOrSelf
     * @return true if the node is descendant
     */
    public static boolean isDescendantOrSelf(Node ctx, Node descendantOrSelf) {
        if (ctx == descendantOrSelf) {
            return true;
        }

        Node parent = descendantOrSelf;

        while (true) {
            if (parent == null) {
                return false;
            }

            if (parent == ctx) {
                return true;
            }

            if (parent.getNodeType() == Node.ATTRIBUTE_NODE) {
                parent = ((Attr) parent).getOwnerElement();
            } else {
                parent = parent.getParentNode();
            }
        }
    }

    public static boolean ignoreLineBreaks() {
        return ignoreLineBreaks;
    }

    /**
     * This method is a tree-search to help prevent against wrapping attacks. It checks that no
     * two Elements have ID Attributes that match the "value" argument, if this is the case then
     * "false" is returned. Note that a return value of "true" does not necessarily mean that
     * a matching Element has been found, just that no wrapping attack has been detected.
     */
    public static boolean protectAgainstWrappingAttack(Node startNode, String value) {
        String id = value.trim();
        if (!id.isEmpty() && id.charAt(0) == '#') {
            id = id.substring(1);
        }

        Node startParent = null;
        Node processedNode = null;
        Element foundElement = null;
        if (startNode != null) {
            startParent = startNode.getParentNode();
        }

        while (startNode != null) {
            if (startNode.getNodeType() == Node.ELEMENT_NODE) {
                Element se = (Element) startNode;

                NamedNodeMap attributes = se.getAttributes();
                if (attributes != null) {
                    int length = attributes.getLength();
                    for (int i = 0; i < length; i++) {
                        Attr attr = (Attr)attributes.item(i);
                        if (attr.isId() && id.equals(attr.getValue())) {
                            if (foundElement == null) {
                                // Continue searching to find duplicates
                                foundElement = attr.getOwnerElement();
                            } else {
                                LOG.debug("Multiple elements with the same 'Id' attribute value!");
                                return false;
                            }
                        }
                    }
                }
            }

            processedNode = startNode;
            startNode = startNode.getFirstChild();

            // no child, this node is done.
            if (startNode == null) {
                // close node processing, get sibling
                startNode = processedNode.getNextSibling();
            }

            // no more siblings, get parent, all children
            // of parent are processed.
            while (startNode == null) {
                processedNode = processedNode.getParentNode();
                if (processedNode == startParent) {
                    return true;
                }
                // close parent node processing (processed node now)
                startNode = processedNode.getNextSibling();
            }
        }
        return true;
    }

    /**
     * This method is a tree-search to help prevent against wrapping attacks. It checks that no other
     * Element than the given "knownElement" argument has an ID attribute that matches the "value"
     * argument, which is the ID value of "knownElement". If this is the case then "false" is returned.
     */
    public static boolean protectAgainstWrappingAttack(
        Node startNode, Element knownElement, String value
    ) {
        String id = value.trim();
        if (!id.isEmpty() && id.charAt(0) == '#') {
            id = id.substring(1);
        }

        Node startParent = null;
        Node processedNode = null;
        if (startNode != null) {
            startParent = startNode.getParentNode();
        }

        while (startNode != null) {
            if (startNode.getNodeType() == Node.ELEMENT_NODE) {
                Element se = (Element) startNode;

                NamedNodeMap attributes = se.getAttributes();
                if (attributes != null) {
                    int length = attributes.getLength();
                    for (int i = 0; i < length; i++) {
                        Attr attr = (Attr)attributes.item(i);
                        if (attr.isId() && id.equals(attr.getValue()) && se != knownElement) {
                            LOG.debug("Multiple elements with the same 'Id' attribute value!");
                            return false;
                        }
                    }
                }
            }

            processedNode = startNode;
            startNode = startNode.getFirstChild();

            // no child, this node is done.
            if (startNode == null) {
                // close node processing, get sibling
                startNode = processedNode.getNextSibling();
            }

            // no more siblings, get parent, all children
            // of parent are processed.
            while (startNode == null) {
                processedNode = processedNode.getParentNode();
                if (processedNode == startParent) {
                    return true;
                }
                // close parent node processing (processed node now)
                startNode = processedNode.getNextSibling();
            }
        }
        return true;
    }

    public static Document read(InputStream inputStream, boolean disallowDocTypeDeclarations) throws XMLParserException {
        // Delegate to XMLParser implementation
        return xmlParserImpl.parse(inputStream, disallowDocTypeDeclarations);
    }

    /**
     * Returns a byte-array representation of a {@code {@link BigInteger}}.
     * No sign-bit is output.
     *
     * <b>N.B.:</B> {@code {@link BigInteger}}'s toByteArray
     * returns eventually longer arrays because of the leading sign-bit.
     *
     * @param big {@code BigInteger} to be converted
     * @param bitlen {@code int} the desired length in bits of the representation
     * @return a byte array with {@code bitlen} bits of {@code big}
     */
    public static byte[] getBytes(BigInteger big, int bitlen) {

        //round bitlen
        bitlen = ((bitlen + 7) >> 3) << 3;

        if (bitlen < big.bitLength()) {
            throw new IllegalArgumentException(I18n.translate("utils.Base64.IllegalBitlength"));
        }

        byte[] bigBytes = big.toByteArray();

        if (big.bitLength() % 8 != 0
            && big.bitLength() / 8 + 1 == bitlen / 8) {
            return bigBytes;
        }

        // some copying needed
        int startSrc = 0;    // no need to skip anything
        int bigLen = bigBytes.length;    //valid length of the string

        if (big.bitLength() % 8 == 0) {    // correct values
            startSrc = 1;    // skip sign bit

            bigLen--;    // valid length of the string
        }

        int startDst = bitlen / 8 - bigLen;    //pad with leading nulls
        byte[] resizedBytes = new byte[bitlen / 8];

        System.arraycopy(bigBytes, startSrc, resizedBytes, startDst, bigLen);

        return resizedBytes;
    }



}
