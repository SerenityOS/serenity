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
 * DOM-based implementation of SignatureProperty.
 *
 */
public final class DOMSignatureProperty extends DOMStructure
    implements SignatureProperty {

    private final String id;
    private final String target;
    private final List<XMLStructure> content;

    /**
     * Creates a {@code SignatureProperty} from the specified parameters.
     *
     * @param content a list of one or more {@link XMLStructure}s. The list
     *    is defensively copied to protect against subsequent modification.
     * @param target the target URI
     * @param id the Id (may be {@code null})
     * @throws ClassCastException if {@code content} contains any
     *    entries that are not of type {@link XMLStructure}
     * @throws IllegalArgumentException if {@code content} is empty
     * @throws NullPointerException if {@code content} or
     *    {@code target} is {@code null}
     */
    public DOMSignatureProperty(List<? extends XMLStructure> content,
                                String target, String id)
    {
        if (target == null) {
            throw new NullPointerException("target cannot be null");
        } else if (content == null) {
            throw new NullPointerException("content cannot be null");
        } else if (content.isEmpty()) {
            throw new IllegalArgumentException("content cannot be empty");
        } else {
            this.content = Collections.unmodifiableList(
                new ArrayList<>(content));
            for (int i = 0, size = this.content.size(); i < size; i++) {
                if (!(this.content.get(i) instanceof XMLStructure)) {
                    throw new ClassCastException
                        ("content["+i+"] is not a valid type");
                }
            }
        }
        this.target = target;
        this.id = id;
    }

    /**
     * Creates a {@code DOMSignatureProperty} from an element.
     *
     * @param propElem a SignatureProperty element
     */
    public DOMSignatureProperty(Element propElem)
        throws MarshalException
    {
        // unmarshal attributes
        target = DOMUtils.getAttributeValue(propElem, "Target");
        if (target == null) {
            throw new MarshalException("target cannot be null");
        }
        Attr attr = propElem.getAttributeNodeNS(null, "Id");
        if (attr != null) {
            id = attr.getValue();
            propElem.setIdAttributeNode(attr, true);
        } else {
            id = null;
        }

        List<XMLStructure> newContent = new ArrayList<>();
        Node firstChild = propElem.getFirstChild();
        while (firstChild != null) {
            newContent.add(new javax.xml.crypto.dom.DOMStructure(firstChild));
            firstChild = firstChild.getNextSibling();
        }
        if (newContent.isEmpty()) {
            throw new MarshalException("content cannot be empty");
        } else {
            this.content = Collections.unmodifiableList(newContent);
        }
    }

    public List<XMLStructure> getContent() {
        return content;
    }

    public String getId() {
        return id;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public void marshal(Node parent, String dsPrefix, DOMCryptoContext context)
        throws MarshalException
    {
        Document ownerDoc = DOMUtils.getOwnerDocument(parent);
        Element propElem = DOMUtils.createElement(ownerDoc, "SignatureProperty",
                                                  XMLSignature.XMLNS, dsPrefix);

        // set attributes
        DOMUtils.setAttributeID(propElem, "Id", id);
        DOMUtils.setAttribute(propElem, "Target", target);

        // create and append any elements and mixed content
        for (XMLStructure property : content) {
            DOMUtils.appendChild(propElem,
                ((javax.xml.crypto.dom.DOMStructure)property).getNode());
        }

        parent.appendChild(propElem);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof SignatureProperty)) {
            return false;
        }
        SignatureProperty osp = (SignatureProperty)o;

        boolean idsEqual = id == null ? osp.getId() == null
                                       : id.equals(osp.getId());

        @SuppressWarnings("unchecked")
        List<XMLStructure> ospContent = osp.getContent();
        return equalsContent(content, ospContent) &&
                target.equals(osp.getTarget()) && idsEqual;
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (id != null) {
            result = 31 * result + id.hashCode();
        }
        result = 31 * result + target.hashCode();
        result = 31 * result + content.hashCode();

        return result;
    }

}
