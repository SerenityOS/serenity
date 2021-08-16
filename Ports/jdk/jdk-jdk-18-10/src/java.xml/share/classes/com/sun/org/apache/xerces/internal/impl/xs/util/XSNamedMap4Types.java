/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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
import com.sun.org.apache.xerces.internal.xs.XSObject;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;


/**
 * Containts the map between qnames and XSObject's.
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 *
 */
public final class XSNamedMap4Types extends XSNamedMapImpl {

    // the type of component stored here: complex or simple type
    private final short fType;

    /**
     * Construct an XSNamedMap implementation for one namespace
     *
     * @param namespace the namespace to which the components belong
     * @param map       the map from local names to components
     * @param type      the type of components
     */
    public XSNamedMap4Types(String namespace, SymbolHash map, short type) {
        super(namespace, map);
        fType = type;
    }

    /**
     * Construct an XSNamedMap implementation for a list of namespaces
     *
     * @param namespaces the namespaces to which the components belong
     * @param maps       the maps from local names to components
     * @param num        the number of namespaces
     * @param type      the type of components
     */
    public XSNamedMap4Types(String[] namespaces, SymbolHash[] maps, int num, short type) {
        super(namespaces, maps, num);
        fType = type;
    }

    /**
     * The number of <code>XSObjects</code> in the <code>XSObjectList</code>. The
     * range of valid child node indices is 0 to <code>length-1</code>
     * inclusive.
     */
    public synchronized int getLength() {
        if (fLength == -1) {
            // first get the number of components for all types
            int length = 0;
            for (int i = 0; i < fNSNum; i++) {
                length += fMaps[i].getLength();
            }
            // then copy all types to an temporary array
            int pos = 0;
            XSObject[] array = new XSObject[length];
            for (int i = 0; i < fNSNum; i++) {
                pos += fMaps[i].getValues(array, pos);
            }
            // then copy either simple or complex types to fArray,
            // depending on which kind is required
            fLength = 0;
            fArray = new XSObject[length];
            XSTypeDefinition type;
            for (int i = 0; i < length; i++) {
                type = (XSTypeDefinition)array[i];
                if (type.getTypeCategory() == fType) {
                    fArray[fLength++] = type;
                }
            }
        }
        return fLength;
    }

    /**
     * Retrieves an <code>XSObject</code> specified by local name and namespace
     * URI.
     * @param namespace The namespace URI of the <code>XSObject</code> to
     *   retrieve.
     * @param localName The local name of the <code>XSObject</code> to retrieve.
     * @return A <code>XSObject</code> (of any type) with the specified local
     *   name and namespace URI, or <code>null</code> if they do not
     *   identify any <code>XSObject</code> in this map.
     */
    public XSObject itemByName(String namespace, String localName) {
        for (int i = 0; i < fNSNum; i++) {
            if (isEqual(namespace, fNamespaces[i])) {
                XSTypeDefinition type = (XSTypeDefinition)fMaps[i].get(localName);
                // only return it if it matches the required type
                if (type != null && type.getTypeCategory() == fType) {
                    return type;
                }
                return null;
            }
        }
        return null;
    }

    /**
     * Returns the <code>index</code>th item in the map. The index starts at
     * 0. If <code>index</code> is greater than or equal to the number of
     * nodes in the list, this returns <code>null</code>.
     * @param index The position in the map from which the item is to be
     *   retrieved.
     * @return The <code>XSObject</code> at the <code>index</code>th position
     *   in the <code>XSNamedMap</code>, or <code>null</code> if that is
     *   not a valid index.
     */
    public synchronized XSObject item(int index) {
        if (fArray == null) {
            getLength();
        }
        if (index < 0 || index >= fLength) {
            return null;
        }
        return fArray[index];
    }

} // class XSNamedMapImpl
