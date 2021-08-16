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

import javax.xml.crypto.*;
import javax.xml.crypto.dom.DOMCryptoContext;
import javax.xml.crypto.dsig.*;

import java.util.*;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * DOM-based implementation of SignatureProperties.
 *
 */
public final class DOMSignatureProperties extends DOMStructure
    implements SignatureProperties {

    private final String id;
    private final List<SignatureProperty> properties;

    /**
     * Creates a {@code DOMSignatureProperties} from the specified
     * parameters.
     *
     * @param properties a list of one or more {@link SignatureProperty}s. The
     *    list is defensively copied to protect against subsequent modification.
     * @param id the Id (may be {@code null})
     * @throws ClassCastException if {@code properties} contains any
     *    entries that are not of type {@link SignatureProperty}
     * @throws IllegalArgumentException if {@code properties} is empty
     * @throws NullPointerException if {@code properties}
     */
    public DOMSignatureProperties(List<? extends SignatureProperty> properties,
                                  String id)
    {
        if (properties == null) {
            throw new NullPointerException("properties cannot be null");
        } else if (properties.isEmpty()) {
            throw new IllegalArgumentException("properties cannot be empty");
        } else {
            this.properties = Collections.unmodifiableList(
                new ArrayList<>(properties));
            for (int i = 0, size = this.properties.size(); i < size; i++) {
                if (!(this.properties.get(i) instanceof SignatureProperty)) {
                    throw new ClassCastException
                        ("properties["+i+"] is not a valid type");
                }
            }
        }
        this.id = id;
    }

    /**
     * Creates a {@code DOMSignatureProperties} from an element.
     *
     * @param propsElem a SignatureProperties element
     * @throws MarshalException if a marshalling error occurs
     */
    public DOMSignatureProperties(Element propsElem)
        throws MarshalException
    {
        // unmarshal attributes
        Attr attr = propsElem.getAttributeNodeNS(null, "Id");
        if (attr != null) {
            id = attr.getValue();
            propsElem.setIdAttributeNode(attr, true);
        } else {
            id = null;
        }

        List<SignatureProperty> newProperties = new ArrayList<>();
        Node firstChild = propsElem.getFirstChild();
        while (firstChild != null) {
            if (firstChild.getNodeType() == Node.ELEMENT_NODE) {
                String name = firstChild.getLocalName();
                String namespace = firstChild.getNamespaceURI();
                if (!"SignatureProperty".equals(name) || !XMLSignature.XMLNS.equals(namespace)) {
                    throw new MarshalException("Invalid element name: " + namespace + ":" + name +
                                               ", expected SignatureProperty");
                }
                newProperties.add(new DOMSignatureProperty((Element)firstChild));
            }
            firstChild = firstChild.getNextSibling();
        }
        if (newProperties.isEmpty()) {
            throw new MarshalException("properties cannot be empty");
        } else {
            this.properties = Collections.unmodifiableList(newProperties);
        }
    }

    public List<SignatureProperty> getProperties() {
        return properties;
    }

    public String getId() {
        return id;
    }

    @Override
    public void marshal(Node parent, String dsPrefix, DOMCryptoContext context)
        throws MarshalException
    {
        Document ownerDoc = DOMUtils.getOwnerDocument(parent);
        Element propsElem = DOMUtils.createElement(ownerDoc,
                                                   "SignatureProperties",
                                                   XMLSignature.XMLNS,
                                                   dsPrefix);

        // set attributes
        DOMUtils.setAttributeID(propsElem, "Id", id);

        // create and append any properties
        for (SignatureProperty property : properties) {
            ((DOMSignatureProperty)property).marshal(propsElem, dsPrefix,
                                                     context);
        }

        parent.appendChild(propsElem);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof SignatureProperties)) {
            return false;
        }
        SignatureProperties osp = (SignatureProperties)o;

        boolean idsEqual = id == null ? osp.getId() == null
                                       : id.equals(osp.getId());

        return properties.equals(osp.getProperties()) && idsEqual;
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (id != null) {
            result = 31 * result + id.hashCode();
        }
        result = 31 * result + properties.hashCode();

        return result;
    }
}
