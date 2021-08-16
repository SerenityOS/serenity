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
package com.sun.org.apache.xml.internal.security.c14n.implementations;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sun.org.apache.xml.internal.security.c14n.CanonicalizationException;
import com.sun.org.apache.xml.internal.security.c14n.helper.C14nHelper;
import com.sun.org.apache.xml.internal.security.parser.XMLParserException;
import com.sun.org.apache.xml.internal.security.signature.XMLSignatureInput;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Implements <A HREF="http://www.w3.org/TR/2001/REC-xml-c14n-20010315">Canonical
 * XML Version 1.0</A>, a W3C Recommendation from 15 March 2001.
 *
 */
public abstract class Canonicalizer20010315 extends CanonicalizerBase {

    private boolean firstCall = true;

    private final XmlAttrStack xmlattrStack;
    private final boolean c14n11;

    /**
     * Constructor Canonicalizer20010315
     *
     * @param includeComments
     */
    public Canonicalizer20010315(boolean includeComments) {
        this(includeComments, false);
    }

    /**
     * Constructor Canonicalizer20010315
     *
     * @param includeComments
     * @param c14n11 Whether this is a Canonical XML 1.1 implementation or not
     */
    public Canonicalizer20010315(boolean includeComments, boolean c14n11) {
        super(includeComments);
        xmlattrStack = new XmlAttrStack(c14n11);
        this.c14n11 = c14n11;
    }


    /**
     * Always throws a CanonicalizationException because this is inclusive c14n.
     *
     * @param xpathNodeSet
     * @param inclusiveNamespaces
     * @param writer OutputStream to write the canonicalization result
     * @throws CanonicalizationException always
     */
    public void engineCanonicalizeXPathNodeSet(Set<Node> xpathNodeSet, String inclusiveNamespaces, OutputStream writer)
        throws CanonicalizationException {

        /** $todo$ well, should we throw UnsupportedOperationException ? */
        throw new CanonicalizationException("c14n.Canonicalizer.UnsupportedOperation");
    }

    /**
     * Always throws a CanonicalizationException because this is inclusive c14n.
     *
     * @param rootNode
     * @param inclusiveNamespaces
     * @param writer OutputStream to write the canonicalization result
     * @throws CanonicalizationException
     */
    public void engineCanonicalizeSubTree(Node rootNode, String inclusiveNamespaces, OutputStream writer)
        throws CanonicalizationException {

        /** $todo$ well, should we throw UnsupportedOperationException ? */
        throw new CanonicalizationException("c14n.Canonicalizer.UnsupportedOperation");
    }

    /**
     * Always throws a CanonicalizationException because this is inclusive c14n.
     *
     * @param rootNode
     * @param inclusiveNamespaces
     * @param writer OutputStream to write the canonicalization result
     * @throws CanonicalizationException
     */
    public void engineCanonicalizeSubTree(
            Node rootNode, String inclusiveNamespaces, boolean propagateDefaultNamespace, OutputStream writer)
            throws CanonicalizationException {

        /** $todo$ well, should we throw UnsupportedOperationException ? */
        throw new CanonicalizationException("c14n.Canonicalizer.UnsupportedOperation");
    }

    /**
     * Output the Attr[]s for the given element.
     * <br>
     * The code of this method is a copy of
     * {@link #outputAttributes(Element, NameSpaceSymbTable, Map)},
     * whereas it takes into account that subtree-c14n is -- well -- subtree-based.
     * So if the element in question isRoot of c14n, it's parent is not in the
     * node set, as well as all other ancestors.
     *
     * @param element
     * @param ns
     * @param cache
     * @param writer OutputStream to write the canonicalization result
     * @throws CanonicalizationException, DOMException, IOException
     */
    @Override
    protected void outputAttributesSubtree(Element element, NameSpaceSymbTable ns,
                                           Map<String, byte[]> cache, OutputStream writer)
        throws CanonicalizationException, DOMException, IOException {
        if (!element.hasAttributes() && !firstCall) {
            return;
        }
        // result will contain the attrs which have to be output
        SortedSet<Attr> result = new TreeSet<>(COMPARE);

        if (element.hasAttributes()) {
            NamedNodeMap attrs = element.getAttributes();
            int attrsLength = attrs.getLength();

            for (int i = 0; i < attrsLength; i++) {
                Attr attribute = (Attr) attrs.item(i);
                String NUri = attribute.getNamespaceURI();
                String NName = attribute.getLocalName();
                String NValue = attribute.getValue();

                if (!XMLNS_URI.equals(NUri)) {
                    //It's not a namespace attr node. Add to the result and continue.
                    result.add(attribute);
                } else if (!(XML.equals(NName) && XML_LANG_URI.equals(NValue))) {
                    //The default mapping for xml must not be output.
                    Node n = ns.addMappingAndRender(NName, NValue, attribute);

                    if (n != null) {
                        //Render the ns definition
                        result.add((Attr)n);
                        if (C14nHelper.namespaceIsRelative(attribute)) {
                            Object[] exArgs = { element.getTagName(), NName, attribute.getNodeValue() };
                            throw new CanonicalizationException(
                                "c14n.Canonicalizer.RelativeNamespace", exArgs
                            );
                        }
                    }
                }
            }
        }

        if (firstCall) {
            //It is the first node of the subtree
            //Obtain all the namespaces defined in the parents, and added to the output.
            ns.getUnrenderedNodes(result);
            //output the attributes in the xml namespace.
            xmlattrStack.getXmlnsAttr(result);
            firstCall = false;
        }

        //we output all Attrs which are available
        for (Attr attr : result) {
            outputAttrToWriter(attr.getNodeName(), attr.getNodeValue(), writer, cache);
        }
    }

    /**
     * Output the Attr[]s for the given element.
     * <br>
     * IMPORTANT: This method expects to work on a modified DOM tree, i.e. a DOM which has
     * been prepared using {@link com.sun.org.apache.xml.internal.security.utils.XMLUtils#circumventBug2650(
     * org.w3c.dom.Document)}.
     *
     * @param element
     * @param ns
     * @param cache
     * @param writer OutputStream to write the canonicalization result
     * @throws CanonicalizationException, DOMException, IOException
     */
    @Override
    protected void outputAttributes(Element element, NameSpaceSymbTable ns,
                                    Map<String, byte[]> cache, OutputStream writer)
        throws CanonicalizationException, DOMException, IOException {
        // result will contain the attrs which have to be output
        xmlattrStack.push(ns.getLevel());
        boolean isRealVisible = isVisibleDO(element, ns.getLevel()) == 1;
        SortedSet<Attr> result = new TreeSet<>(COMPARE);

        if (element.hasAttributes()) {
            NamedNodeMap attrs = element.getAttributes();
            int attrsLength = attrs.getLength();

            for (int i = 0; i < attrsLength; i++) {
                Attr attribute = (Attr) attrs.item(i);
                String NUri = attribute.getNamespaceURI();
                String NName = attribute.getLocalName();
                String NValue = attribute.getValue();

                if (!XMLNS_URI.equals(NUri)) {
                    //A non namespace definition node.
                    if (XML_LANG_URI.equals(NUri)) {
                        if (c14n11 && "id".equals(NName)) {
                            if (isRealVisible) {
                                // treat xml:id like any other attribute
                                // (emit it, but don't inherit it)
                                result.add(attribute);
                            }
                        } else {
                            xmlattrStack.addXmlnsAttr(attribute);
                        }
                    } else if (isRealVisible) {
                        //The node is visible add the attribute to the list of output attributes.
                        result.add(attribute);
                    }
                } else if (!XML.equals(NName) || !XML_LANG_URI.equals(NValue)) {
                    /* except omit namespace node with local name xml, which defines
                     * the xml prefix, if its string value is http://www.w3.org/XML/1998/namespace.
                     */
                    //add the prefix binding to the ns symb table.
                    if (isVisible(attribute))  {
                        if (isRealVisible || !ns.removeMappingIfRender(NName)) {
                            //The xpath select this node output it if needed.
                            Node n = ns.addMappingAndRender(NName, NValue, attribute);
                            if (n != null) {
                                result.add((Attr)n);
                                if (C14nHelper.namespaceIsRelative(attribute)) {
                                    Object[] exArgs = { element.getTagName(), NName, attribute.getNodeValue() };
                                    throw new CanonicalizationException(
                                        "c14n.Canonicalizer.RelativeNamespace", exArgs
                                    );
                                }
                            }
                        }
                    } else {
                        if (isRealVisible && !XMLNS.equals(NName)) {
                            ns.removeMapping(NName);
                        } else {
                            ns.addMapping(NName, NValue, attribute);
                        }
                    }
                }
            }
        }
        if (isRealVisible) {
            //The element is visible, handle the xmlns definition
            Attr xmlns = element.getAttributeNodeNS(XMLNS_URI, XMLNS);
            Node n = null;
            if (xmlns == null) {
                //No xmlns def just get the already defined.
                n = ns.getMapping(XMLNS);
            } else if (!isVisible(xmlns)) {
                //There is a definition but the xmlns is not selected by the xpath.
                //then xmlns=""
                n = ns.addMappingAndRender(
                        XMLNS, "", getNullNode(xmlns.getOwnerDocument()));
            }
            //output the xmlns def if needed.
            if (n != null) {
                result.add((Attr)n);
            }
            //Float all xml:* attributes of the unselected parent elements to this one.
            xmlattrStack.getXmlnsAttr(result);
            ns.getUnrenderedNodes(result);
        }

        //we output all Attrs which are available
        for (Attr attr : result) {
            outputAttrToWriter(attr.getNodeName(), attr.getNodeValue(), writer, cache);
        }
    }

    protected void circumventBugIfNeeded(XMLSignatureInput input)
        throws XMLParserException, IOException {
        if (!input.isNeedsToBeExpanded()) {
            return;
        }
        Document doc = null;
        if (input.getSubNode() != null) {
            doc = XMLUtils.getOwnerDocument(input.getSubNode());
        } else {
            doc = XMLUtils.getOwnerDocument(input.getNodeSet());
        }
        XMLUtils.circumventBug2650(doc);
    }

    @Override
    protected void handleParent(Element e, NameSpaceSymbTable ns) {
        if (!e.hasAttributes() && e.getNamespaceURI() == null) {
            return;
        }
        xmlattrStack.push(-1);
        NamedNodeMap attrs = e.getAttributes();
        int attrsLength = attrs.getLength();
        for (int i = 0; i < attrsLength; i++) {
            Attr attribute = (Attr) attrs.item(i);
            String NName = attribute.getLocalName();
            String NValue = attribute.getNodeValue();

            if (XMLNS_URI.equals(attribute.getNamespaceURI())) {
                if (!XML.equals(NName) || !XML_LANG_URI.equals(NValue)) {
                    ns.addMapping(NName, NValue, attribute);
                }
            } else if (XML_LANG_URI.equals(attribute.getNamespaceURI())
                && (!c14n11 || !"id".equals(NName))) {
                xmlattrStack.addXmlnsAttr(attribute);
            }
        }
        if (e.getNamespaceURI() != null) {
            String NName = e.getPrefix();
            String NValue = e.getNamespaceURI();
            String Name;
            if (NName == null || NName.isEmpty()) {
                NName = "xmlns";
                Name = "xmlns";
            } else {
                Name = "xmlns:" + NName;
            }
            Attr n = e.getOwnerDocument().createAttributeNS("http://www.w3.org/2000/xmlns/", Name);
            n.setValue(NValue);
            ns.addMapping(NName, NValue, n);
        }
    }
}
