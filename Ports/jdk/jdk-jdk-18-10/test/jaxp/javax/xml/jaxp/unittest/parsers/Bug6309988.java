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

package parsers;

import static jaxp.library.JAXPTestUtilities.clearSystemProperty;
import static jaxp.library.JAXPTestUtilities.setSystemProperty;

import java.io.File;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

/*
 * @test
 * @bug 6309988
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.Bug6309988
 * @run testng/othervm parsers.Bug6309988
 * @summary Test elementAttributeLimit, maxOccurLimit, entityExpansionLimit.
 */
@Test(singleThreaded = true)
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6309988 {

    DocumentBuilderFactory dbf = null;

    /*
     * Given XML document has more than 10000 attributes. Exception is expected
     */
    public void testDOMParserElementAttributeLimit() {
        try {
            dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = dbf.newDocumentBuilder();
            Document doc = parser.parse(this.getClass().getResourceAsStream("DosTest.xml"));
            Assert.fail("SAXParserException is expected, as given XML document contains more than 10000 attributes");
        } catch (SAXParseException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            Assert.fail("Exception " + e.getMessage());
        }
    }

    /*
     * Given XML document has more than 10000 attributes. It should report an
     * error.
     */
    public void testDOMNSParserElementAttributeLimit() {
        try {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder parser = dbf.newDocumentBuilder();
            Document doc = parser.parse(this.getClass().getResourceAsStream("DosTest.xml"));
            Assert.fail("SAXParserException is expected, as given XML document contains more than 10000 attributes");
        } catch (SAXParseException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            Assert.fail("Exception " + e.getMessage());
        }
    }

    /*
     * Given XML document has more than 10000 attributes. Parsing this XML
     * document in non-secure mode, should not report any error.
     */
    public void testDOMNSParserElementAttributeLimitWithoutSecureProcessing() {
        if (isSecureMode())
            return; // jaxp secure feature can not be turned off when security
                    // manager is present
        try {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            dbf.setNamespaceAware(true);
            DocumentBuilder parser = dbf.newDocumentBuilder();
            Document doc = parser.parse(this.getClass().getResourceAsStream("DosTest.xml"));

        } catch (SAXParseException e) {
            Assert.fail(e.getMessage());
        } catch (Exception e) {
            Assert.fail("Exception " + e.getMessage());
        }
    }

    /*
     * Before 8014530: Given XML document has 3 attributes and System property
     * is set to 2. Parsing this XML document in non-secure mode, should not
     * report an error.
     * After 8014530: System properties will override FSP, the result of this
     * test should be the same as
     * testSystemElementAttributeLimitWithSecureProcessing
     */
    public void testSystemElementAttributeLimitWithoutSecureProcessing() {
        if (isSecureMode())
            return; // jaxp secure feature can not be turned off when security
                    // manager is present
        try {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            dbf.setNamespaceAware(true);
            setSystemProperty("elementAttributeLimit", "2");
            DocumentBuilder parser = dbf.newDocumentBuilder();
            Document doc = parser.parse(this.getClass().getResourceAsStream("DosTest3.xml"));

            Assert.fail("SAXParserException is expected, as given XML document contains more than 2 attributes");
        } catch (Exception e) {
            String errMsg = e.getMessage();
            Throwable cause = e.getCause();
            if (cause != null) {
                errMsg += cause.getMessage();
            }
            if (errMsg.contains("JAXP0001")) {
                // expected
            } else {
                Assert.fail("Unexpected error: " + e.getMessage());
            }
        } finally {
            clearSystemProperty("elementAttributeLimit");
        }
    }

    /*
     * Given XML document has 3 attributes and System property is set to 2.
     * Parsing this XML document in secure mode, should report an error.
     */
    public void testSystemElementAttributeLimitWithSecureProcessing() {
        try {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            setSystemProperty("elementAttributeLimit", "2");
            DocumentBuilder parser = dbf.newDocumentBuilder();
            Document doc = parser.parse(this.getClass().getResourceAsStream("DosTest3.xml"));
            Assert.fail("SAXParserException is expected, as given XML document contains more than 2 attributes");
        } catch (SAXParseException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            Assert.fail("Exception " + e.getMessage());
        } finally {
            setSystemProperty("elementAttributeLimit", "");
        }
    }

    /*
     * Default value for secure processing feature should be true.
     */
    public void testDOMSecureProcessingDefaultValue() {
        try {
            dbf = DocumentBuilderFactory.newInstance();
            Assert.assertTrue(dbf.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING), "Default value for secureProcessing feature should be true");

        } catch (Exception e) {
            Assert.fail("Exception " + e.getMessage());
        }
    }

    /*
     * Default value for secure processing feature should be true.
     */
    public void testSAXSecureProcessingDefaultValue() {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            Assert.assertTrue(spf.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING), "Default value for secureProcessing feature should be true");

        } catch (Exception e) {
            Assert.fail("Exception " + e.getMessage());
        }
    }

    /*
     * This method sets system property for maxOccurLimit=2 and secure process
     * feature is off. Given doument contains more than 2 elements and hence an
     * error should be reported.
     */
    public void testSystemMaxOccurLimitWithoutSecureProcessing() {
        if (isSecureMode())
            return; // jaxp secure feature can not be turned off when security
                    // manager is present
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            spf.setValidating(true);
            setSystemProperty("maxOccurLimit", "2");
            // Set the properties for Schema Validation
            String SCHEMA_LANG = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
            String SCHEMA_TYPE = "http://www.w3.org/2001/XMLSchema";
            // Get the Schema location as a File object
            File schemaFile = new File(this.getClass().getResource("toys.xsd").toURI());
            // Get the parser
            SAXParser parser = spf.newSAXParser();
            parser.setProperty(SCHEMA_LANG, SCHEMA_TYPE);
            parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", schemaFile);

            InputStream is = this.getClass().getResourceAsStream("toys.xml");
            MyErrorHandler eh = new MyErrorHandler();
            parser.parse(is, eh);
            Assert.assertFalse(eh.errorOccured, "Not Expected Error");
            setSystemProperty("maxOccurLimit", "");
        } catch (Exception e) {
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    /*
     * This test will take longer time to execute( abt 120sec). This method
     * tries to validate a document. This document contains an element whose
     * maxOccur is '3002'. Since secure processing feature is off, document
     * should be parsed without any errors.
     */
    public void testValidMaxOccurLimitWithOutSecureProcessing() {
        if (isSecureMode())
            return; // jaxp secure feature can not be turned off when security
                    // manager is present
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            spf.setValidating(true);
            // Set the properties for Schema Validation
            String SCHEMA_LANG = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
            String SCHEMA_TYPE = "http://www.w3.org/2001/XMLSchema";
            // Get the Schema location as a File object
            File schemaFile = new File(this.getClass().getResource("toys3002.xsd").toURI());
            // Get the parser
            SAXParser parser = spf.newSAXParser();
            parser.setProperty(SCHEMA_LANG, SCHEMA_TYPE);
            parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", schemaFile);

            InputStream is = this.getClass().getResourceAsStream("toys.xml");
            MyErrorHandler eh = new MyErrorHandler();
            parser.parse(is, eh);
            Assert.assertFalse(eh.errorOccured, "Expected Error as maxOccurLimit is exceeded");

        } catch (Exception e) {
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    /*
     * Before 8014530: System property is set to 2. Given XML document has more
     * than 2 entity references. Parsing this document in non-secure mode,
     * should *not* report an error.
     * After 8014530: System properties will override FSP, the result of this
     * test should be the same as
     * testSystemElementAttributeLimitWithSecureProcessing
     */
    public void testSystemEntityExpansionLimitWithOutSecureProcessing() {
        if (isSecureMode())
            return; // jaxp secure feature can not be turned off when security
                    // manager is present
        try {
            setSystemProperty("entityExpansionLimit", "2");
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            dbf.setValidating(true);
            DocumentBuilder parser = dbf.newDocumentBuilder();
            Document doc = parser.parse(this.getClass().getResourceAsStream("entity.xml"));
            Assert.fail("SAXParserException is expected, as given XML document contains more 2 entity references");
        } catch (Exception e) {
            String errMsg = e.getMessage();
            Throwable cause = e.getCause();
            if (cause != null) {
                errMsg += cause.getMessage();
            }
            if (errMsg.contains("JAXP0001")) {
                // expected
            } else {
                Assert.fail("Unexpected error: " + e.getMessage());
            }
        } finally {
            clearSystemProperty("entityExpansionLimit");
        }
    }

    /*
     * System property is set to 2. Given XML document has more than 2 entity
     * references. Parsing this document in secure mode, should report an error.
     */
    public void testSystemEntityExpansionLimitWithSecureProcessing() {
        try {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(true);
            setSystemProperty("entityExpansionLimit", "2");
            DocumentBuilder parser = dbf.newDocumentBuilder();
            Document doc = parser.parse(this.getClass().getResourceAsStream("entity.xml"));
            Assert.fail("SAXParserException is expected, as given XML document contains more 2 entity references");

        } catch (SAXParseException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            Assert.fail("Exception " + e.getMessage());
        } finally {
            setSystemProperty("entityExpansionLimit", "");
        }
    }

    /*
     * Given XML document has more than 64000 entity references. Parsing this
     * document in secure mode, should report an error.
     */
    public void testEntityExpansionLimitWithSecureProcessing() {
        try {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(true);
            DocumentBuilder parser = dbf.newDocumentBuilder();
            Document doc = parser.parse(this.getClass().getResourceAsStream("entity64K.xml"));
            Assert.fail("SAXParserException is expected, as given XML document contains more 2 entity references");

        } catch (SAXParseException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            Assert.fail("Exception " + e.getMessage());
        } finally {
            setSystemProperty("entityExpansionLimit", "");
        }
    }

    /*
     * Given XML document has more than 64000 entity references. Parsing this
     * document in non-secure mode, should not report any error.
     */
    public void testEntityExpansionLimitWithOutSecureProcessing() {
        if (isSecureMode())
            return; // jaxp secure feature can not be turned off when security
                    // manager is present
        try {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            dbf.setValidating(true);
            DocumentBuilder parser = dbf.newDocumentBuilder();
            Document doc = parser.parse(this.getClass().getResourceAsStream("entity64K.xml"));

        } catch (SAXParseException e) {
            Assert.fail("Exception " + e.getMessage());
        } catch (Exception e) {
            Assert.fail("Exception " + e.getMessage());
        } finally {
            setSystemProperty("entityExpansionLimit", "");
        }
    }

    private boolean isSecureMode() {
        return System.getSecurityManager() != null;
    }
}
