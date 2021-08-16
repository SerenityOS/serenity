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

package com.sun.org.apache.xerces.internal.util;

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import org.xml.sax.AttributeList;
import org.xml.sax.ext.Attributes2;

/**
 * Wraps {@link XMLAttributes} and makes it look like
 * {@link AttributeList} and {@link Attributes}.
 *
 * @author Arnaud Le Hors, IBM
 * @author Andy Clark, IBM
 *
 */
@SuppressWarnings("deprecation")
public final class AttributesProxy
    implements AttributeList, Attributes2 {

    //
    // Data
    //

    /** XML attributes. */
    private XMLAttributes fAttributes;

    //
    // Constructors
    //

    public AttributesProxy(XMLAttributes attributes) {
        fAttributes = attributes;
    }

    //
    // Public methods
    //

    /** Sets the XML attributes to be wrapped. */
    public void setAttributes(XMLAttributes attributes) {
        fAttributes = attributes;
    } // setAttributes(XMLAttributes)

    public XMLAttributes getAttributes() {
        return fAttributes;
    }

    /*
     * Attributes methods
     */

    public int getLength() {
        return fAttributes.getLength();
    }

    public String getQName(int index) {
        return fAttributes.getQName(index);
    }

    public String getURI(int index) {
        // This hides the fact that internally we use null instead of empty string
        // SAX requires the URI to be a string or an empty string
        String uri = fAttributes.getURI(index);
        return uri != null ? uri : XMLSymbols.EMPTY_STRING;
    }

    public String getLocalName(int index) {
        return fAttributes.getLocalName(index);
    }

    public String getType(int i) {
        return fAttributes.getType(i);
    }

    public String getType(String name) {
        return fAttributes.getType(name);
    }

    public String getType(String uri, String localName) {
        return uri.equals(XMLSymbols.EMPTY_STRING) ?
                fAttributes.getType(null, localName) :
                    fAttributes.getType(uri, localName);
    }

    public String getValue(int i) {
        return fAttributes.getValue(i);
    }

    public String getValue(String name) {
        return fAttributes.getValue(name);
    }

    public String getValue(String uri, String localName) {
        return uri.equals(XMLSymbols.EMPTY_STRING) ?
                fAttributes.getValue(null, localName) :
                    fAttributes.getValue(uri, localName);
    }

    public int getIndex(String qName) {
        return fAttributes.getIndex(qName);
    }

    public int getIndex(String uri, String localPart) {
        return uri.equals(XMLSymbols.EMPTY_STRING) ?
                fAttributes.getIndex(null, localPart) :
                    fAttributes.getIndex(uri, localPart);
    }

    /*
     * Attributes2 methods
     */

    public boolean isDeclared(int index) {
        if (index < 0 || index >= fAttributes.getLength()) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return Boolean.TRUE.equals(
            fAttributes.getAugmentations(index).getItem(
            Constants.ATTRIBUTE_DECLARED));
    }

    public boolean isDeclared(String qName) {
        int index = getIndex(qName);
        if (index == -1) {
            throw new IllegalArgumentException(qName);
        }
        return Boolean.TRUE.equals(
            fAttributes.getAugmentations(index).getItem(
            Constants.ATTRIBUTE_DECLARED));
    }

    public boolean isDeclared(String uri, String localName) {
        int index = getIndex(uri, localName);
        if (index == -1) {
            throw new IllegalArgumentException(localName);
        }
        return Boolean.TRUE.equals(
            fAttributes.getAugmentations(index).getItem(
            Constants.ATTRIBUTE_DECLARED));
    }

    public boolean isSpecified(int index) {
        if (index < 0 || index >= fAttributes.getLength()) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return fAttributes.isSpecified(index);
    }

    public boolean isSpecified(String qName) {
        int index = getIndex(qName);
        if (index == -1) {
            throw new IllegalArgumentException(qName);
        }
        return fAttributes.isSpecified(index);
    }

    public boolean isSpecified(String uri, String localName) {
        int index = getIndex(uri, localName);
        if (index == -1) {
            throw new IllegalArgumentException(localName);
        }
        return fAttributes.isSpecified(index);
    }

    /*
     * AttributeList methods
     */

    public String getName(int i) {
        return fAttributes.getQName(i);
    }

}
