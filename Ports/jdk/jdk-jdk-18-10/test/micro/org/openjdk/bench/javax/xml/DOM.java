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
package org.openjdk.bench.javax.xml;

import org.openjdk.jmh.annotations.Benchmark;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class DOM extends AbstractXMLMicro {

    @Benchmark
    public Document testBuild() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        byte[] bytes = getFileBytesFromResource(doc);
        InputSource source = new InputSource();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

        source.setByteStream(bais);
        return buildDocument(dbf, source);
    }

    @Benchmark
    public Document testModify() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        byte[] bytes = getFileBytesFromResource(doc);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        InputSource source = new InputSource(bais);
        Document doc1 = buildDocument(dbf, source);

        modifyElementRecursive(doc1.getDocumentElement());
        return doc1;
    }

    @Benchmark
    public Document testWalk() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        byte[] bytes = getFileBytesFromResource(doc);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        InputSource source = new InputSource(bais);
        Document doc1 = buildDocument(dbf, source);

        walkElementRecursive(doc1.getDocumentElement());
        return doc1;
    }

    private Document buildDocument(DocumentBuilderFactory dbf, InputSource source)
            throws ParserConfigurationException, SAXException, IOException {
        dbf.setValidating(false);
        dbf.setNamespaceAware(true);
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        return docBuilder.parse(source);
    }

    // TODO Fix so it isn't recursive?
    private static void walkElementRecursive(Element element) {
        // loop through children
        if (element.hasChildNodes()) {
            Node child = element.getFirstChild();
            while (child != null) {

                // handle child by type
                int type = child.getNodeType();
                if (type == Node.ELEMENT_NODE) {
                    walkElementRecursive((Element) child);
                }
                child = child.getNextSibling();
            }
        }
    }

    // TODO Fix so it isn't recursive?
    private void modifyElementRecursive(Element element) {

        // check for children present
        if (element.hasChildNodes()) {

            // loop through child nodes
            boolean content = false;

            // Should not be null since we already have made a .hasChildNodes()
            // check.
            Node child = element.getFirstChild();

            do {
                // Handle child by node type.
                if (child.getNodeType() == Node.TEXT_NODE) {
                    String trimmed = child.getNodeValue().trim();
                    if (trimmed.length() == 0) {
                        // delete child if nothing but whitespace
                        element.removeChild(child);
                    } else {
                        // make sure we have the parent element information
                        content = true;
                        Document doc = element.getOwnerDocument();
                        String uri = element.getNamespaceURI();
                        String prefix = element.getPrefix();
                        content = true;

                        // Create a "text" element matching parent namespace.
                        Element text = (uri == null) ? doc.createElement("text") : doc.createElementNS(uri, prefix
                                + ":text");

                        // wrap the trimmed content with new element
                        text.appendChild(doc.createTextNode(trimmed));
                        element.replaceChild(text, child);

                    }
                } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                    modifyElementRecursive((Element) child);
                }

            } while ((child = child.getNextSibling()) != null);

            // check if we've seen any non-whitespace content for element
            if (content) {
                String prefix = element.getPrefix();
                String uri = element.getNamespaceURI();
                // add attribute flagging content found
                if (prefix == null || prefix.length() == 0) {
                    element.setAttribute("text", "true");
                } else {
                    element.setAttributeNS(uri, prefix + ":text", "true");
                }

            }
        }
    }

}
