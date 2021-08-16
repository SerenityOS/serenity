/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static org.testng.Assert.assertEquals;
import static org.w3c.dom.ptests.DOMTestUtil.createNewDocument;

import javax.xml.parsers.ParserConfigurationException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.w3c.dom.ptests.DomImplementationTest
 * @run testng/othervm org.w3c.dom.ptests.DomImplementationTest
 * @summary Test DomImplementation API
 */
@Listeners({jaxp.library.BasePolicy.class})
public class DomImplementationTest {
    /*
     * Test createDocument method with a namespace uri, qualified name and null
     * for the doctype
     */
    @Test
    public void testCreateDocument() throws ParserConfigurationException {
        final String nsURI = "http://www.document.com";
        final String name = "document:localName";
        DOMImplementation domImpl = getDOMImplementation();
        Document document = domImpl.createDocument(nsURI, name, null);
        assertEquals(document.getDocumentElement().getNamespaceURI(), nsURI);
        assertEquals(document.getDocumentElement().getNodeName(), name);
    }

    /*
     * Test createDocumentType method with name, public id and system id.
     */
    @Test
    public void testCreateDocumentType01() throws ParserConfigurationException {
        final String name = "document:localName";
        final String publicId = "pubid";
        final String systemId = "sysid";

        DOMImplementation domImpl = getDOMImplementation();
        DocumentType documentType = domImpl.createDocumentType(name, publicId, systemId);
        verifyDocumentType(documentType, name, publicId, systemId);
    }


    /*
     * Test createDocument method using a DocumentType, verify the document will
     * take that Doctype.
     */
    @Test
    public void testCreateDocumentType02() throws ParserConfigurationException {
        final String name = "document:localName";
        final String publicId = "-//W3C//DTD HTML 4.0 Transitional//EN";
        final String systemId = "http://www.w3.org/TR/REC-html40/loose.dtd";
        DOMImplementation domImpl = getDOMImplementation();

        DocumentType documentType = domImpl.createDocumentType(name, publicId, systemId);
        Document document = domImpl.createDocument("http://www.document.com", "document:localName", documentType);
        verifyDocumentType(document.getDoctype(), name, publicId, systemId);
    }

    @DataProvider(name = "feature-supported")
    public Object[][] getFeatureSupportedList() throws ParserConfigurationException {
        DOMImplementation impl = getDOMImplementation();
        return new Object[][] {
                { impl, "XML", "2.0", true },
                { impl, "HTML", "2.0", false },
                { impl, "Views", "2.0", false },
                { impl, "StyleSheets", "2.0", false },
                { impl, "CSS", "2.0", false },
                { impl, "CSS2", "2.0", false },
                { impl, "Events", "2.0", true },
                { impl, "UIEvents", "2.0", false },
                { impl, "MouseEvents", "2.0", false },
                { impl, "HTMLEvents", "2.0", false },
                { impl, "Traversal", "2.0", true },
                { impl, "Range", "2.0", true },
                { impl, "Core", "2.0", true },
                { impl, "XML", "", true } };
    }


    /*
     * Verify DOMImplementation for feature supporting.
     */
    @Test(dataProvider = "feature-supported")
    public void testHasFeature(DOMImplementation impl, String feature, String version, boolean isSupported) {
        assertEquals(impl.hasFeature(feature,version), isSupported);
    }


    private DOMImplementation getDOMImplementation() throws ParserConfigurationException {
        return createNewDocument().getImplementation();
    }


    private void verifyDocumentType(DocumentType documentType, String name, String publicId, String systemId) {
        assertEquals(documentType.getPublicId(), publicId);
        assertEquals(documentType.getSystemId(), systemId);
        assertEquals(documentType.getName(), name);
    }
}
