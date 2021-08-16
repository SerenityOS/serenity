/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package com.sun.org.apache.xerces.internal.dom;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.DOMImplementation;

public class DocumentImpl implements Document, Node {

    public short getNodeType() {
        return 9; //DOCUMENT_NODE = 9
    }

    public org.w3c.dom.Document getOwnerDocument() {
        return null;
    }

    public Node getFirstChild() {
        return null;
    }

    public String getPrefix() {
        return "TestPrefix";
    }

    public String getLocalName() {
        return "LocalName";
    }

    public boolean hasAttributes() {
        return false;
    }

    public Node renameNode(Node n, String namespaceURI, String name) {
        return n;
    }

    public org.w3c.dom.DocumentType getDoctype() {
        return null;
    }

    public DOMImplementation getImplementation() {
        return DOMImplementationImpl.getDOMImplementation();
    }

}
