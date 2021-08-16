/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.impl.xs.util;

import com.sun.org.apache.xerces.internal.util.SymbolHash;
import com.sun.org.apache.xerces.internal.xs.XSNamedMap;
import com.sun.org.apache.xerces.internal.xs.XSObject;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * Containts the map between qnames and XSObject's.
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 *
 * @LastModified: Oct 2017
 */
public class XSNamedMapImpl extends AbstractMap<QName, XSObject> implements XSNamedMap {

    /**
     * An immutable empty map.
     */
    public static final XSNamedMapImpl EMPTY_MAP = new XSNamedMapImpl(new XSObject[0], 0);

    // components of these namespaces are stored in this map
    final String[] fNamespaces;
    // number of namespaces
    final int fNSNum;
    // each entry contains components in one namespace
    final SymbolHash[] fMaps;
    // store all components from all namespace.
    // used when this map is accessed as a list.
    XSObject[] fArray = null;
    // store the number of components.
    // used when this map is accessed as a list.
    int fLength = -1;
    // Set of Map.Entry<QName,XSObject> for the java.util.Map methods
    private Set<Map.Entry<QName,XSObject>> fEntrySet = null;

    /**
     * Construct an XSNamedMap implementation for one namespace
     *
     * @param namespace the namespace to which the components belong
     * @param map       the map from local names to components
     */
    public XSNamedMapImpl(String namespace, SymbolHash map) {
        fNamespaces = new String[] {namespace};
        fMaps = new SymbolHash[] {map};
        fNSNum = 1;
    }

    /**
     * Construct an XSNamedMap implementation for a list of namespaces
     *
     * @param namespaces the namespaces to which the components belong
     * @param maps       the maps from local names to components
     * @param num        the number of namespaces
     */
    public XSNamedMapImpl(String[] namespaces, SymbolHash[] maps, int num) {
        fNamespaces = namespaces;
        fMaps = maps;
        fNSNum = num;
    }

    /**
     * Construct an XSNamedMap implementation one namespace from an array
     *
     * @param array     containing all components
     * @param length    number of components
     */
    public XSNamedMapImpl(XSObject[] array, int length) {
        if (length == 0) {
            fNamespaces = null;
            fMaps = null;
            fNSNum = 0;
            fArray = array;
            fLength = 0;
            return;
        }
        // because all components are from the same target namesapce,
        // get the namespace from the first one.
        fNamespaces = new String[]{array[0].getNamespace()};
        fMaps = null;
        fNSNum = 1;
        // copy elements to the Vector
        fArray = array;
        fLength = length;
    }

    /**
     * The number of <code>XSObjects</code> in the <code>XSObjectList</code>.
     * The range of valid child object indices is 0 to <code>length-1</code>
     * inclusive.
     */
    public synchronized int getLength() {
        if (fLength == -1) {
            fLength = 0;
            for (int i = 0; i < fNSNum; i++) {
                fLength += fMaps[i].getLength();
            }
        }
        return fLength;
    }

    /**
     * Retrieves an <code>XSObject</code> specified by local name and
     * namespace URI.
     * <br>Per XML Namespaces, applications must use the value <code>null</code> as the
     * <code>namespace</code> parameter for methods if they wish to specify
     * no namespace.
     * @param namespace The namespace URI of the <code>XSObject</code> to
     *   retrieve, or <code>null</code> if the <code>XSObject</code> has no
     *   namespace.
     * @param localName The local name of the <code>XSObject</code> to
     *   retrieve.
     * @return A <code>XSObject</code> (of any type) with the specified local
     *   name and namespace URI, or <code>null</code> if they do not
     *   identify any object in this map.
     */
    public XSObject itemByName(String namespace, String localName) {
        for (int i = 0; i < fNSNum; i++) {
            if (isEqual(namespace, fNamespaces[i])) {
                // when this map is created from SymbolHash's
                // get the component from SymbolHash
                if (fMaps != null) {
                    return (XSObject)fMaps[i].get(localName);
                }
                // Otherwise (it's created from an array)
                // go through the array to find a matching name
                XSObject ret;
                for (int j = 0; j < fLength; j++) {
                    ret = fArray[j];
                    if (ret.getName().equals(localName)) {
                        return ret;
                    }
                }
                return null;
            }
        }
        return null;
    }

    /**
     * Returns the <code>index</code>th item in the collection or
     * <code>null</code> if <code>index</code> is greater than or equal to
     * the number of objects in the list. The index starts at 0.
     * @param index  index into the collection.
     * @return  The <code>XSObject</code> at the <code>index</code>th
     *   position in the <code>XSObjectList</code>, or <code>null</code> if
     *   the index specified is not valid.
     */
    public synchronized XSObject item(int index) {
        if (fArray == null) {
            // calculate the total number of elements
            getLength();
            fArray = new XSObject[fLength];
            int pos = 0;
            // get components from all SymbolHashes
            for (int i = 0; i < fNSNum; i++) {
                pos += fMaps[i].getValues(fArray, pos);
            }
        }
        if (index < 0 || index >= fLength) {
            return null;
        }
        return fArray[index];
    }

    static boolean isEqual(String one, String two) {
        return (one != null) ? one.equals(two) : (two == null);
    }

    /*
     * java.util.Map methods
     */

    public boolean containsKey(Object key) {
        return (get(key) != null);
    }

    public XSObject get(Object key) {
        if (key instanceof QName) {
            final QName name = (QName) key;
            String namespaceURI = name.getNamespaceURI();
            if (XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
                namespaceURI = null;
            }
            String localPart = name.getLocalPart();
            return itemByName(namespaceURI, localPart);
        }
        return null;
    }

    public int size() {
        return getLength();
    }

    public synchronized Set<Map.Entry<QName,XSObject>> entrySet() {
        // Defer creation of the entry set until it is actually needed.
        if (fEntrySet == null) {
            final int length = getLength();
            final XSNamedMapEntry[] entries = new XSNamedMapEntry[length];
            for (int i = 0; i < length; ++i) {
                XSObject xso = item(i);
                entries[i] = new XSNamedMapEntry(new QName(xso.getNamespace(), xso.getName()), xso);
            }
            // Create a view of this immutable map.
            fEntrySet = new AbstractSet<Map.Entry<QName,XSObject>>() {
                public Iterator<Map.Entry<QName,XSObject>> iterator() {
                    return new Iterator<Map.Entry<QName,XSObject>>() {
                        private int index = 0;
                        public boolean hasNext() {
                            return (index < length);
                        }
                        public Map.Entry<QName,XSObject> next() {
                            if (index < length) {
                                return entries[index++];
                            }
                            throw new NoSuchElementException();
                        }
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
                public int size() {
                    return length;
                }
            };
        }
        return fEntrySet;
    }

    /** An entry in the XSNamedMap. **/
    private static final class XSNamedMapEntry implements Map.Entry<QName, XSObject> {
        private final QName key;
        private final XSObject value;
        public XSNamedMapEntry(QName key, XSObject value) {
            this.key = key;
            this.value = value;
        }
        public QName getKey() {
            return key;
        }
        public XSObject getValue() {
            return value;
        }
        public XSObject setValue(XSObject value) {
            throw new UnsupportedOperationException();
        }
        public boolean equals(XSNamedMapEntry o) {
            if (o instanceof Map.Entry) {
                Map.Entry<QName, XSObject> e = (Map.Entry<QName, XSObject>) o;
                QName otherKey = e.getKey();
                XSObject otherValue = e.getValue();
                return (key == null ? otherKey == null : key.equals(otherKey)) &&
                    (value == null ? otherValue == null : value.equals(otherValue));
            }
            return false;
        }
        public int hashCode() {
            return (key == null ? 0 : key.hashCode())
                ^ (value == null ? 0 : value.hashCode());
        }
        public String toString() {
            StringBuilder buffer = new StringBuilder();
            buffer.append(String.valueOf(key));
            buffer.append('=');
            buffer.append(String.valueOf(value));
            return buffer.toString();
        }
    }

} // class XSNamedMapImpl
