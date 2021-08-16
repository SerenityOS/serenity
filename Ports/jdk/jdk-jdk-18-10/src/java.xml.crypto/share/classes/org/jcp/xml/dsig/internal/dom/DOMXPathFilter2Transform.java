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
 * ===========================================================================
 *
 * (C) Copyright IBM Corp. 2003 All Rights Reserved.
 *
 * ===========================================================================
 */
/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 */
package org.jcp.xml.dsig.internal.dom;

import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.crypto.dsig.spec.XPathFilter2ParameterSpec;
import javax.xml.crypto.dsig.spec.XPathType;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * DOM-based implementation of XPath Filter 2.0 Transform.
 * (Uses Apache XML-Sec Transform implementation)
 *
 */
public final class DOMXPathFilter2Transform extends ApacheTransform {

    public void init(TransformParameterSpec params)
        throws InvalidAlgorithmParameterException
    {
        if (params == null) {
            throw new InvalidAlgorithmParameterException("params are required");
        } else if (!(params instanceof XPathFilter2ParameterSpec)) {
            throw new InvalidAlgorithmParameterException
                ("params must be of type XPathFilter2ParameterSpec");
        }
        this.params = params;
    }

    public void init(XMLStructure parent, XMLCryptoContext context)
        throws InvalidAlgorithmParameterException
    {
        super.init(parent, context);
        try {
            unmarshalParams(DOMUtils.getFirstChildElement(transformElem));
        } catch (MarshalException me) {
            throw new InvalidAlgorithmParameterException(me);
        }
    }

    private void unmarshalParams(Element curXPathElem) throws MarshalException
    {
        List<XPathType> list = new ArrayList<>();
        Element currentElement = curXPathElem;
        while (currentElement != null) {
            String xPath = currentElement.getFirstChild().getNodeValue();
            String filterVal = DOMUtils.getAttributeValue(currentElement,
                                                          "Filter");
            if (filterVal == null) {
                throw new MarshalException("filter cannot be null");
            }
            XPathType.Filter filter = null;
            if ("intersect".equals(filterVal)) {
                filter = XPathType.Filter.INTERSECT;
            } else if ("subtract".equals(filterVal)) {
                filter = XPathType.Filter.SUBTRACT;
            } else if ("union".equals(filterVal)) {
                filter = XPathType.Filter.UNION;
            } else {
                throw new MarshalException("Unknown XPathType filter type" +
                                           filterVal);
            }
            NamedNodeMap attributes = currentElement.getAttributes();
            if (attributes != null) {
                int length = attributes.getLength();
                Map<String, String> namespaceMap =
                    new HashMap<>(length);
                for (int i = 0; i < length; i++) {
                    Attr attr = (Attr)attributes.item(i);
                    String prefix = attr.getPrefix();
                    if (prefix != null && "xmlns".equals(prefix)) {
                        namespaceMap.put(attr.getLocalName(), attr.getValue());
                    }
                }
                list.add(new XPathType(xPath, filter, namespaceMap));
            } else {
                list.add(new XPathType(xPath, filter));
            }

            currentElement = DOMUtils.getNextSiblingElement(currentElement);
        }
        this.params = new XPathFilter2ParameterSpec(list);
    }

    public void marshalParams(XMLStructure parent, XMLCryptoContext context)
        throws MarshalException
    {
        super.marshalParams(parent, context);
        XPathFilter2ParameterSpec xp =
            (XPathFilter2ParameterSpec)getParameterSpec();
        String prefix = DOMUtils.getNSPrefix(context, Transform.XPATH2);
        String qname = prefix == null || prefix.length() == 0
                       ? "xmlns" : "xmlns:" + prefix;
        @SuppressWarnings("unchecked")
        List<XPathType> xpathList = xp.getXPathList();
        for (XPathType xpathType : xpathList) {
            Element elem = DOMUtils.createElement(ownerDoc, "XPath",
                                                  Transform.XPATH2, prefix);
            elem.appendChild
                (ownerDoc.createTextNode(xpathType.getExpression()));
            DOMUtils.setAttribute(elem, "Filter",
                                  xpathType.getFilter().toString());
            elem.setAttributeNS("http://www.w3.org/2000/xmlns/", qname,
                                Transform.XPATH2);

            // add namespace attributes, if necessary
            @SuppressWarnings("unchecked")
            Set<Map.Entry<String, String>> entries =
                xpathType.getNamespaceMap().entrySet();
            for (Map.Entry<String, String> entry : entries) {
                elem.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:" +
                                    entry.getKey(),
                                    entry.getValue());
            }

            transformElem.appendChild(elem);
        }
    }
}
