/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6559064 6942504
 *
 * @summary Verify DOM L3 Node APIs behave as per Image I/O spec.
 *
 * @run main DOML3Node
 */

import javax.imageio.metadata.IIOMetadataNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.DOMException;
import org.w3c.dom.UserDataHandler;


public class DOML3Node {

    public static void main(String args[]) {
        IIOMetadataNode node = new IIOMetadataNode("node");

        try {
            node.setIdAttribute("name", true);
            throw new RuntimeException("No expected DOM exception");
        } catch (DOMException e) {
        }

        try {
            node.setIdAttributeNS("namespaceURI", "localName", true);
            throw new RuntimeException("No expected DOM exception");
        } catch (DOMException e) {
        }

        try {
            node.setIdAttributeNode((Attr)null, true);
            throw new RuntimeException("No expected DOM exception");
        } catch (DOMException e) {
        }

        try {
            node.getSchemaTypeInfo();
            throw new RuntimeException("No expected DOM exception");
        } catch (DOMException e) {
        }

        try {
            node.setUserData("key", null, (UserDataHandler)null);
            throw new RuntimeException("No expected DOM exception");
        } catch (DOMException e) {
        }

        try {
            node.getUserData("key");
            throw new RuntimeException("No expected DOM exception");
        } catch (DOMException e) {
        }

        try {
            node.getFeature("feature", "version");
            throw new RuntimeException("No expected DOM exception");
        } catch (DOMException e) {
        }

        try {
            node.isSameNode((Node)null);
            throw new RuntimeException("No expected DOM exception");
        } catch (DOMException e) {
        }

        try {
            node.isEqualNode((Node)null);
            throw new RuntimeException("No expected DOM exception");
        } catch (DOMException e) {
        }

        try {
            node.lookupNamespaceURI("prefix");
            throw new RuntimeException("No expected DOM exception");
        } catch (DOMException e) {
        }

        try {
            node.isDefaultNamespace("namespaceURI");
            throw new RuntimeException("No expected DOM exception");
        } catch (DOMException e) {
        }

        try {
            node.lookupPrefix("namespaceURI");
            throw new RuntimeException("No expected DOM exception");
        } catch (DOMException e) {
        }

        try {
            node.getTextContent();
            throw new RuntimeException("No expected DOM exception");
        } catch (DOMException e) {
        }

        try {
            node.setTextContent("textContent");
            throw new RuntimeException("No expected DOM exception");
        } catch (DOMException e) {
        }

        try {
            node.compareDocumentPosition((Node)null);
            throw new RuntimeException("No expected DOM exception");
        } catch (DOMException e) {
        }

        try {
            node.getBaseURI();
            throw new RuntimeException("No expected DOM exception");
        } catch (DOMException e) {
        }
    }
}
