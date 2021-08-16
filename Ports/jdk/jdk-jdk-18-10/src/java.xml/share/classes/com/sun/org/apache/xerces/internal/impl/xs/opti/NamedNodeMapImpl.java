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

import org.w3c.dom.Node;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;

import org.w3c.dom.DOMException;


/**
 * @xerces.internal
 *
 * @author Rahul Srivastava, Sun Microsystems Inc.
 *
 */
public class NamedNodeMapImpl implements NamedNodeMap {

        Attr[] attrs;

        public NamedNodeMapImpl(Attr[] attrs) {
                this.attrs = attrs;
        }

        public Node getNamedItem(String name) {
                for (int i=0; i<attrs.length; i++) {
                        if (attrs[i].getName().equals(name)) {
                                return attrs[i];
                        }
                }
                return null;
        }

        public Node item(int index) {
                if (index < 0 && index > getLength()) {
                        return null;
                }
                return attrs[index];
        }

        public int getLength() {
                return attrs.length;
        }

        public Node getNamedItemNS(String namespaceURI, String localName) {
                for (int i=0; i<attrs.length; i++) {
                        if (attrs[i].getName().equals(localName) && attrs[i].getNamespaceURI().equals(namespaceURI)) {
                                return attrs[i];
                        }
                }
                return null;
        }

        public Node setNamedItemNS(Node arg) throws DOMException {
                throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
        }

        public Node setNamedItem(Node arg) throws DOMException {
                throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
        }

        public Node removeNamedItem(String name) throws DOMException {
                throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
        }

        public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException {
                throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Method not supported");
        }
}
