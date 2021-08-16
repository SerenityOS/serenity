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

import java.util.Iterator;
import java.util.Objects;

import javax.xml.namespace.NamespaceContext;

import org.w3c.dom.Node;

import static javax.xml.XMLConstants.DEFAULT_NS_PREFIX;
import static javax.xml.XMLConstants.NULL_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_PREFIX;
import static javax.xml.XMLConstants.XML_NS_URI;

/**
 * This class adapts the {@link Node} namespace/prefix lookup API to that of {@link NamespaceContext}.
 * There are some differences:
 * <table>
 *     <tr>
 *         <th>Function</th>
 *         <th>{@link NamespaceContext} API</th>
 *         <th>{@link Node} API</th>
 *     </tr>
 *     <tr>
 *         <td>Look up the prefix for a given namespace URI.</td>
 *         <td>{@link NamespaceContext#getPrefix(String)}</td>
 *         <td>{@link Node#lookupPrefix(String)}</td>
 *     </tr>
 *     <tr>
 *         <td>Look up all the prefixes for a given namespace URI.</td>
 *         <td>{@link NamespaceContext#getPrefixes(String)}</td>
 *         <td>/</td>
 *     </tr>
 *     <tr>
 *         <td>Look up the namespace URI for a given prefix.</td>
 *         <td>{@link NamespaceContext#getNamespaceURI(String)}</td>
 *         <td>{@link Node#lookupNamespaceURI(String)}</td>
 *     </tr>
 *     <tr>
 *         <td>The default prefix.</td>
 *         <td>{@link javax.xml.XMLConstants#DEFAULT_NS_PREFIX}</td>
 *         <td>{@code null}</td>
 *     </tr>
 *     <tr>
 *         <td>The default namespace URI.</td>
 *         <td>{@link javax.xml.XMLConstants#NULL_NS_URI}</td>
 *         <td>{@code null}</td>
 *     </tr>
 * </table>
 */
public class DOMNamespaceContext implements NamespaceContext {

    private Node context;

    public DOMNamespaceContext(Node context) {
        setContext(context);
    }

    public void setContext(Node context) {
        this.context = context;
    }

    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("prefix is null");
        }
        if (prefix.equals(DEFAULT_NS_PREFIX)) {
            prefix = null;
        }
        if (context != null) {
            String namespaceURI = context.lookupNamespaceURI(prefix);
            if (namespaceURI != null) {
                return namespaceURI;
            }
        }
        if (prefix == null) {
            return NULL_NS_URI;
        } else if (prefix.equals(XML_NS_PREFIX)) {
            return XML_NS_URI;
        } else if (prefix.equals(XMLNS_ATTRIBUTE)) {
            return XMLNS_ATTRIBUTE_NS_URI;
        }
        return NULL_NS_URI;
    }

    public String getPrefix(String namespaceURI) {
        if (namespaceURI == null) {
            throw new IllegalArgumentException("namespace URI is null");
        }
        if (namespaceURI.equals(NULL_NS_URI)) {
            namespaceURI = null;
        }
        if (context != null) {
            String prefix = context.lookupPrefix(namespaceURI);
            if (prefix != null) {
                return prefix;
            } else if (Objects.equals(context.lookupNamespaceURI(null), namespaceURI)) {
                // context.lookupPrefix(namespaceURI) returns null when a namespace URI is unbound but also when it is
                // bound to the default prefix.
                // To distinguish the case of an unbound namespace URI from a bound one to the default prefix,
                // we look up the namespace URI for the default prefix (null) and if it matches, we return the default
                // prefix.
                return DEFAULT_NS_PREFIX;
            }
        }
        if (namespaceURI == null) {
            return context.lookupNamespaceURI(null) != null ? null : DEFAULT_NS_PREFIX;
        } else if (namespaceURI.equals(XML_NS_URI)) {
            return XML_NS_PREFIX;
        } else if (namespaceURI.equals(XMLNS_ATTRIBUTE_NS_URI)) {
            return XMLNS_ATTRIBUTE;
        }
        return null;
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     */
    public Iterator<String> getPrefixes(String namespaceURI) {
        throw new UnsupportedOperationException();
    }
}
