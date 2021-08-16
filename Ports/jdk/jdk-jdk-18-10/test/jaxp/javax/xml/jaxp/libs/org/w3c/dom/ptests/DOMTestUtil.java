/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.w3c.dom.ptests;

import static jaxp.library.JAXPTestUtilities.FILE_SEP;
import static jaxp.library.JAXPTestUtilities.getPathByClassName;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/*
 * This class defines the path constant and common method
 */
public class DOMTestUtil {
    /*
     * XML source file directory.
     */
    public static final String XML_DIR = getPathByClassName(DOMTestUtil.class, ".." + FILE_SEP + "xmlfiles");

    /*
     * Golden validation files directory.
     */
    public static final String GOLDEN_DIR = getPathByClassName(DOMTestUtil.class, ".." + FILE_SEP + "xmlfiles" + FILE_SEP + "out");

    /*
     * Error Message for DOMException being expected.
     */
    public static final String DOMEXCEPTION_EXPECTED = "Should throw DOMException";

    /*
     * Create DOM Document from an xml file.
     */
    public static Document createDOM(String xmlFileName) throws SAXException, IOException, ParserConfigurationException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(XML_DIR + xmlFileName));
    }

    /*
     * Create DOM Document from an xml file with setNamespaceAware(true).
     */
    public static Document createDOMWithNS(String xmlFileName) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder().parse(new File(XML_DIR + xmlFileName));
    }

    /*
     * Create a new DOM Document.
     */
    public static Document createNewDocument() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.newDocument();
    }
}
