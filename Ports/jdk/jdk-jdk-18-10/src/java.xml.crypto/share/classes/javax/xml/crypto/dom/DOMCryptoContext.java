/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
/*
 * $Id: DOMCryptoContext.java,v 1.3 2005/05/09 18:33:26 mullan Exp $
 */
package javax.xml.crypto.dom;

import javax.xml.crypto.KeySelector;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.XMLCryptoContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.w3c.dom.Element;

/**
 * This class provides a DOM-specific implementation of the
 * {@link XMLCryptoContext} interface. It also includes additional
 * methods that are specific to a DOM-based implementation for registering
 * and retrieving elements that contain attributes of type ID.
 *
 * @author Sean Mullan
 * @author JSR 105 Expert Group
 * @since 1.6
 */
public class DOMCryptoContext implements XMLCryptoContext {

    private HashMap<String,String> nsMap = new HashMap<>();
    private HashMap<String,Element> idMap = new HashMap<>();
    private HashMap<Object,Object> objMap = new HashMap<>();
    private String baseURI;
    private KeySelector ks;
    private URIDereferencer dereferencer;
    private HashMap<String,Object> propMap = new HashMap<>();
    private String defaultPrefix;

    /**
     * Default constructor. (For invocation by subclass constructors).
     */
    protected DOMCryptoContext() {}

    /**
     * This implementation uses an internal {@link HashMap} to get the prefix
     * that the specified URI maps to. It returns the <code>defaultPrefix</code>
     * if it maps to <code>null</code>.
     *
     * @throws NullPointerException {@inheritDoc}
     */
    public String getNamespacePrefix(String namespaceURI,
        String defaultPrefix) {
        if (namespaceURI == null) {
            throw new NullPointerException("namespaceURI cannot be null");
        }
        String prefix = nsMap.get(namespaceURI);
        return (prefix != null ? prefix : defaultPrefix);
    }

    /**
     * This implementation uses an internal {@link HashMap} to map the URI
     * to the specified prefix.
     *
     * @throws NullPointerException {@inheritDoc}
     */
    public String putNamespacePrefix(String namespaceURI, String prefix) {
        if (namespaceURI == null) {
            throw new NullPointerException("namespaceURI is null");
        }
        return nsMap.put(namespaceURI, prefix);
    }

    public String getDefaultNamespacePrefix() {
        return defaultPrefix;
    }

    public void setDefaultNamespacePrefix(String defaultPrefix) {
        this.defaultPrefix = defaultPrefix;
    }

    public String getBaseURI() {
        return baseURI;
    }

    /**
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public void setBaseURI(String baseURI) {
        if (baseURI != null) {
            java.net.URI.create(baseURI);
        }
        this.baseURI = baseURI;
    }

    public URIDereferencer getURIDereferencer() {
        return dereferencer;
    }

    public void setURIDereferencer(URIDereferencer dereferencer) {
        this.dereferencer = dereferencer;
    }

    /**
     * This implementation uses an internal {@link HashMap} to get the object
     * that the specified name maps to.
     *
     * @throws NullPointerException {@inheritDoc}
     */
    public Object getProperty(String name) {
        if (name == null) {
            throw new NullPointerException("name is null");
        }
        return propMap.get(name);
    }

    /**
     * This implementation uses an internal {@link HashMap} to map the name
     * to the specified object.
     *
     * @throws NullPointerException {@inheritDoc}
     */
    public Object setProperty(String name, Object value) {
        if (name == null) {
            throw new NullPointerException("name is null");
        }
        return propMap.put(name, value);
    }

    public KeySelector getKeySelector() {
        return ks;
    }

    public void setKeySelector(KeySelector ks) {
        this.ks = ks;
    }

    /**
     * Returns the <code>Element</code> with the specified ID attribute value.
     *
     * <p>This implementation uses an internal {@link HashMap} to get the
     * element that the specified attribute value maps to.
     *
     * @param idValue the value of the ID
     * @return the <code>Element</code> with the specified ID attribute value,
     *    or <code>null</code> if none.
     * @throws NullPointerException if <code>idValue</code> is <code>null</code>
     * @see #setIdAttributeNS
     */
    public Element getElementById(String idValue) {
        if (idValue == null) {
            throw new NullPointerException("idValue is null");
        }
        return idMap.get(idValue);
    }

    /**
     * Registers the element's attribute specified by the namespace URI and
     * local name to be of type ID. The attribute must have a non-empty value.
     *
     * <p>This implementation uses an internal {@link HashMap} to map the
     * attribute's value to the specified element.
     *
     * @param element the element
     * @param namespaceURI the namespace URI of the attribute (specify
     *    <code>null</code> if not applicable)
     * @param localName the local name of the attribute
     * @throws IllegalArgumentException if <code>localName</code> is not an
     *    attribute of the specified element or it does not contain a specific
     *    value
     * @throws NullPointerException if <code>element</code> or
     *    <code>localName</code> is <code>null</code>
     * @see #getElementById
     */
    public void setIdAttributeNS(Element element, String namespaceURI,
        String localName) {
        if (element == null) {
            throw new NullPointerException("element is null");
        }
        if (localName == null) {
            throw new NullPointerException("localName is null");
        }
        String idValue = element.getAttributeNS(namespaceURI, localName);
        if (idValue == null || idValue.length() == 0) {
            throw new IllegalArgumentException(localName + " is not an " +
                "attribute");
        }
        idMap.put(idValue, element);
    }

    /**
     * Returns a read-only iterator over the set of Id/Element mappings of
     * this <code>DOMCryptoContext</code>. Attempts to modify the set via the
     * {@link Iterator#remove} method throw an
     * <code>UnsupportedOperationException</code>. The mappings are returned
     * in no particular order. Each element in the iteration is represented as a
     * {@link java.util.Map.Entry}. If the <code>DOMCryptoContext</code> is
     * modified while an iteration is in progress, the results of the
     * iteration are undefined.
     *
     * @return a read-only iterator over the set of mappings
     */
    public Iterator<Map.Entry<String, Element>> iterator() {
        return Collections.unmodifiableMap(idMap).entrySet().iterator();
    }

    /**
     * This implementation uses an internal {@link HashMap} to get the object
     * that the specified key maps to.
     */
    public Object get(Object key) {
        return objMap.get(key);
    }

    /**
     * This implementation uses an internal {@link HashMap} to map the key
     * to the specified object.
     *
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public Object put(Object key, Object value) {
        return objMap.put(key, value);
    }
}
