/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package validation;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.IssueTracker30
 * @run testng/othervm validation.IssueTracker30
 * @summary Test maxOccurs validation.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class IssueTracker30 {

    boolean errorFound;

    @Test
    public void testMaxOccursErrorNoOptimization() throws Exception {

        // Parsing this document should result in an error
        try {
                if (!testMaxOccurs("IssueTracker30_occurs-error.xml", "IssueTracker30_occurs.xsd"))
                        Assert.fail("Expected validation error not reported");
        } catch (SAXException ex) {
            Assert.fail(ex.getMessage(), ex);
        }
    }

    @Test
    public void testMaxOccursOKNoOptimization() throws Exception {

        // Parsing this document should *not* result in an error
        try {
                if (testMaxOccurs("IssueTracker30_occurs-ok.xml", "IssueTracker30_occurs.xsd"))
                        Assert.fail("Unexpected validation error reported");
        } catch (SAXException ex) {
            Assert.fail(ex.getMessage(), ex);
        }
    }

    @Test
    public void testMaxOccursErrorOptimization() throws Exception {

        // Parsing this document should result in an error
        try {
                if (!testMaxOccurs("IssueTracker30_occurs-optimize-error.xml", "IssueTracker30_occurs-optimize.xsd"))
                        Assert.fail("Expected validation error not reported");
        } catch (SAXException ex) {
            Assert.fail(ex.getMessage(), ex);
        }
    }

    @Test
    public void testMaxOccursOKOptimization() throws Exception {

        // Parsing this document should *not* result in an error
        try {
                if (testMaxOccurs("IssueTracker30_occurs-optimize-ok.xml", "IssueTracker30_occurs-optimize.xsd"))
                        Assert.fail("Unexpected validation error reported");
        } catch (SAXException ex) {
            Assert.fail(ex.getMessage(), ex);
        }
    }


    private boolean testMaxOccurs(String xmlFileName, String xsdFileName) throws Exception {
        File xmlFile = new File(getClass().getResource(xmlFileName).getFile());

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new File(getClass().getResource(xsdFileName).getFile()));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setSchema(schema);

        DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
        documentBuilder.setErrorHandler(new ErrorHandler() {
            public void error(SAXParseException e) throws SAXException {
                System.out.println("Error: " + e.getMessage());
                errorFound = true;
            }

            public void fatalError(SAXParseException e) throws SAXException {
                System.out.println("Fatal error: " + e.getMessage());
                throw e;
            }

            public void warning(SAXParseException e) throws SAXException {
                System.out.println("Warning: " + e.getMessage());
            }
        });

        errorFound = false;
        documentBuilder.parse(xmlFile);
        return errorFound;
    }

}
