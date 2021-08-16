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

import java.security.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMCryptoContext;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * DOM-based implementation of KeyInfo.
 *
 */
public final class DOMKeyInfo extends DOMStructure implements KeyInfo {

    private final String id;
    private final List<XMLStructure> keyInfoTypes;

    /**
     * A utility function to suppress casting warnings.
     * @param ki
     * @return the content of a KeyInfo Object
     */
    @SuppressWarnings("unchecked")
    public static List<XMLStructure> getContent(KeyInfo ki) {
        return ki.getContent();
    }

    /**
     * Creates a {@code DOMKeyInfo}.
     *
     * @param content a list of one or more {@link XMLStructure}s representing
     *    key information types. The list is defensively copied to protect
     *    against subsequent modification.
     * @param id an ID attribute
     * @throws NullPointerException if {@code content} is {@code null}
     * @throws IllegalArgumentException if {@code content} is empty
     * @throws ClassCastException if {@code content} contains any entries
     *    that are not of type {@link XMLStructure}
     */
    public DOMKeyInfo(List<? extends XMLStructure> content, String id) {
        if (content == null) {
            throw new NullPointerException("content cannot be null");
        }
        this.keyInfoTypes =
            Collections.unmodifiableList(new ArrayList<>(content));
        if (this.keyInfoTypes.isEmpty()) {
            throw new IllegalArgumentException("content cannot be empty");
        }
        for (int i = 0, size = this.keyInfoTypes.size(); i < size; i++) {
            if (!(this.keyInfoTypes.get(i) instanceof XMLStructure)) {
                throw new ClassCastException
                    ("content["+i+"] is not a valid KeyInfo type");
            }
        }
        this.id = id;
    }

    /**
     * Creates a {@code DOMKeyInfo} from XML.
     *
     * @param kiElem KeyInfo element
     */
    public DOMKeyInfo(Element kiElem, XMLCryptoContext context,
                      Provider provider)
        throws MarshalException
    {
        // get Id attribute, if specified
        Attr attr = kiElem.getAttributeNodeNS(null, "Id");
        if (attr != null) {
            id = attr.getValue();
            kiElem.setIdAttributeNode(attr, true);
        } else {
            id = null;
        }

        // get all children nodes
        List<XMLStructure> content = new ArrayList<>();
        Node firstChild = kiElem.getFirstChild();
        if (firstChild == null) {
            throw new MarshalException("KeyInfo must contain at least one type");
        }
        while (firstChild != null) {
            if (firstChild.getNodeType() == Node.ELEMENT_NODE) {
                Element childElem = (Element)firstChild;
                String localName = childElem.getLocalName();
                String namespace = childElem.getNamespaceURI();
                if ("X509Data".equals(localName) && XMLSignature.XMLNS.equals(namespace)) {
                    content.add(new DOMX509Data(childElem));
                } else if ("KeyName".equals(localName) && XMLSignature.XMLNS.equals(namespace)) {
                    content.add(new DOMKeyName(childElem));
                } else if ("KeyValue".equals(localName) && XMLSignature.XMLNS.equals(namespace)) {
                    content.add(DOMKeyValue.unmarshal(childElem));
                } else if ("RetrievalMethod".equals(localName) && XMLSignature.XMLNS.equals(namespace)) {
                    content.add(new DOMRetrievalMethod(childElem,
                                                       context, provider));
                } else if ("PGPData".equals(localName) && XMLSignature.XMLNS.equals(namespace)) {
                    content.add(new DOMPGPData(childElem));
                } else { //may be MgmtData, SPKIData or element from other namespace
                    content.add(new javax.xml.crypto.dom.DOMStructure(childElem));
                }
            }
            firstChild = firstChild.getNextSibling();
        }
        keyInfoTypes = Collections.unmodifiableList(content);
    }

    public String getId() {
        return id;
    }

    public List<XMLStructure> getContent() {
        return keyInfoTypes;
    }

    public void marshal(XMLStructure parent, XMLCryptoContext context)
        throws MarshalException
    {
        if (parent == null) {
            throw new NullPointerException("parent is null");
        }
        if (!(parent instanceof javax.xml.crypto.dom.DOMStructure)) {
            throw new ClassCastException("parent must be of type DOMStructure");
        }

        Node pNode = ((javax.xml.crypto.dom.DOMStructure)parent).getNode();
        String dsPrefix = DOMUtils.getSignaturePrefix(context);
        Element kiElem = DOMUtils.createElement
            (DOMUtils.getOwnerDocument(pNode), "KeyInfo",
             XMLSignature.XMLNS, dsPrefix);
        if (dsPrefix == null || dsPrefix.length() == 0) {
            kiElem.setAttributeNS("http://www.w3.org/2000/xmlns/",
                                  "xmlns", XMLSignature.XMLNS);
        } else {
            kiElem.setAttributeNS("http://www.w3.org/2000/xmlns/",
                                  "xmlns:" + dsPrefix, XMLSignature.XMLNS);
        }

        Node nextSibling = null;
        if (context instanceof DOMSignContext) {
            nextSibling = ((DOMSignContext)context).getNextSibling();
        }
        marshal(pNode, kiElem, nextSibling, dsPrefix, (DOMCryptoContext)context);
    }

    @Override
    public void marshal(Node parent, String dsPrefix,
                        DOMCryptoContext context)
        throws MarshalException
    {
        marshal(parent, null, dsPrefix, context);
    }

    public void marshal(Node parent, Node nextSibling, String dsPrefix,
                        DOMCryptoContext context)
        throws MarshalException
    {
        Document ownerDoc = DOMUtils.getOwnerDocument(parent);
        Element kiElem = DOMUtils.createElement(ownerDoc, "KeyInfo",
                                                XMLSignature.XMLNS, dsPrefix);
        marshal(parent, kiElem, nextSibling, dsPrefix, context);
    }

    private void marshal(Node parent, Element kiElem, Node nextSibling,
                         String dsPrefix, DOMCryptoContext context)
        throws MarshalException
    {
        // create and append KeyInfoType elements
        for (XMLStructure kiType : keyInfoTypes) {
            if (kiType instanceof DOMStructure) {
                ((DOMStructure)kiType).marshal(kiElem, dsPrefix, context);
            } else {
                DOMUtils.appendChild(kiElem,
                    ((javax.xml.crypto.dom.DOMStructure)kiType).getNode());
            }
        }

        // append id attribute
        DOMUtils.setAttributeID(kiElem, "Id", id);

        parent.insertBefore(kiElem, nextSibling);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof KeyInfo)) {
            return false;
        }
        KeyInfo oki = (KeyInfo)o;

        boolean idsEqual = id == null ? oki.getId() == null
                                       : id.equals(oki.getId());

        return keyInfoTypes.equals(oki.getContent()) && idsEqual;
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (id != null) {
            result = 31 * result + id.hashCode();
        }
        result = 31 * result + keyInfoTypes.hashCode();

        return result;
    }
}
