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

package com.sun.org.apache.xerces.internal.impl.xs.opti;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.TypeInfo;

/**
 * This class represents a single attribute.
 *
 * @author Rahul Srivastava, Sun Microsystems Inc.
 *
 */
public class AttrImpl extends NodeImpl
                      implements Attr {

    Element element;
    String value;

    /** Default Constructor */
    public AttrImpl() {
        nodeType = Node.ATTRIBUTE_NODE;
    }

    /**
     * Constructs an attribute.
     *
     * @param element Element which owns this attribute
     * @param prefix The QName prefix.
     * @param localpart The QName localpart.
     * @param rawname The QName rawname.
     * @param uri The uri binding for the associated prefix.
     * @param value The value of the attribute.
     */
    public AttrImpl(Element element, String prefix, String localpart, String rawname, String uri, String value) {
        super(prefix, localpart, rawname, uri, Node.ATTRIBUTE_NODE);
        this.element = element;
        this.value = value;
    }

    public String getName() {
        return rawname;
    }

    public boolean getSpecified() {
        return true;
    }

    public String getValue() {
        return value;
    }

    public String getNodeValue() {
        return getValue();
    }

    public Element getOwnerElement() {
        return element;
    }

    public Document getOwnerDocument() {
        return element.getOwnerDocument();
    }

    public void setValue(String value) throws DOMException {
        this.value = value;
    }

    /**
     * @since DOM Level 3
     */
    public boolean isId(){
        return false;
    }

    /**
     * Method getSchemaTypeInfo.
     * @return TypeInfo
     */
    public TypeInfo getSchemaTypeInfo(){
      return null;
    }

    /** NON-DOM method for debugging convenience */
    public String toString() {
        return getName() + "=" + "\"" + getValue() + "\"";
    }
}
