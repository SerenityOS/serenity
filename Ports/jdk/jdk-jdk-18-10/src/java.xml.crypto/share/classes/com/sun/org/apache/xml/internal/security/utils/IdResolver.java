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

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Purpose of this class is to enable the XML Parser to keep track of ID
 * attributes. This is done by 'registering' attributes of type ID at the
 * IdResolver.
 * @deprecated
 */
@Deprecated
public final class IdResolver {

    private IdResolver() {
        // we don't allow instantiation
    }

    /**
     * Method registerElementById
     *
     * @param element the element to register
     * @param id the ID attribute
     */
    public static void registerElementById(Element element, Attr id) {
        element.setIdAttributeNode(id, true);
    }

    /**
     * Method getElementById
     *
     * @param doc the document
     * @param id the value of the ID
     * @return the element obtained by the id, or null if it is not found.
     */
    public static Element getElementById(Document doc, String id) {
        return doc.getElementById(id);
    }

}
