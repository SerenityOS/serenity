/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8038043
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm common.EncodingErrorsReportingTest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow common.EncodingErrorsReportingTest
 * @summary Verifies that parsers reports location of wrong UTF-8 symbols in
 *          XML files parsed and included via xi:include element
 */
package common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.Assert;

@Listeners({jaxp.library.BasePolicy.class})
public class EncodingErrorsReportingTest implements EntityResolver {

    /*
     * Test reporting of wrong UTF8 byte sequence location by SAX and DOM parsers
     */
    @Test(dataProvider = "invalidUTF8BytesInXml")
    public void testMalformedByteException(Function<byte[], Exception> parseF,
                                           byte[] xmlData, int expLine, int expColumn) {
        // Check if data was generated without errors
        Assert.assertNotNull(xmlData, "Error in xml test data generation");

        // Execute supplier to get parse exception
        Exception caughtEx = parseF.apply(xmlData);

        // Check if exception was thrown
        Assert.assertNotNull(caughtEx, "No caught exception");
        boolean isSPE = caughtEx instanceof SAXParseException;
        Assert.assertTrue(isSPE, "Caught exception is not SAXParseException");
        SAXParseException spe = (SAXParseException) caughtEx;

        // Check if cause is properly set
        Throwable cause = spe.getCause();
        Assert.assertNotNull(cause, "Cause is null");
        Assert.assertEquals("com.sun.org.apache.xerces.internal" +
                            ".impl.io.MalformedByteSequenceException",
                            cause.getClass().getName(),
                            "Cause is not MalformedByteSequenceException");

        // Check error locator parameters
        int column_number = spe.getColumnNumber();
        int line_number = spe.getLineNumber();
        Assert.assertEquals(line_number, expLine, "Wrong line number reported");
        Assert.assertEquals(column_number, expColumn, "Wrong column number reported");
    }

    // Provider with supplier functions that process XML content with different parsers
    @DataProvider(name = "invalidUTF8BytesInXml")
    public Object[][] parsersResultsSupplier() {
        return new Object[][]{
                // Tests for invalid UTF-8 byte in xml element
                {(Function<byte[], Exception>) this::parseWithSAX,
                        invalidByteInXmlElement(), 3, 15},
                {(Function<byte[], Exception>) this::parseWithDOM,
                        invalidByteInXmlElement(), 3, 15},
                // Tests for invalid UTF-8 byte in xml attribute
                {(Function<byte[], Exception>) this::parseWithSAX,
                        invalidByteInXmlAttribute(), 4, 21},
                {(Function<byte[], Exception>) this::parseWithDOM,
                        invalidByteInXmlAttribute(), 4, 21},
                // Tests for invalid UTF-8 byte in xml version string
                {(Function<byte[], Exception>) this::parseWithSAX,
                        invalidByteInXmlVersionDecl(), 1, 16},
                {(Function<byte[], Exception>) this::parseWithDOM,
                        invalidByteInXmlVersionDecl(), 1, 16},
                // Tests for invalid byte in XML file included
                // into parsed XML file with xi:include element
                {(Function<byte[], Exception>) this::parseSaxAndXinclude,
                        XINCLUDE_TEST_XML.getBytes(), 5, 53},
                {(Function<byte[], Exception>) this::parseDomAndXinclude,
                        XINCLUDE_TEST_XML.getBytes(), 5, 53},
        };
    }

    // Parse constructed XML with SAXParser and save the observed exception
    private Exception parseWithSAX(byte[] data) {
        Exception caughtEx = null;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            InputStream inputStream = new ByteArrayInputStream(data);
            saxParser.parse(inputStream, new DefaultHandler());
        } catch (Exception e) {
            caughtEx = e;
        }
        return caughtEx;
    }

    // Parse constructed XML with DOMParser and save the observed exception
    private Exception parseWithDOM(byte[] data) {
        Exception caughtEx = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputStream inputStream = new ByteArrayInputStream(data);
            db.parse(inputStream);
        } catch (Exception e) {
            caughtEx = e;
        }
        return caughtEx;
    }

    // Parse XML content that includes faulty XML content with xi:include element.
    // XML data is parsed by SAX parser
    private Exception parseSaxAndXinclude(byte[] data) {
        Exception caughtEx = null;
        try {
            // Create SAX parser factory and make it xi:include aware
            SAXParserFactory spf = SAXParserFactory.newDefaultInstance();
            spf.setNamespaceAware(true);
            spf.setXIncludeAware(true);
            // Set this test class as entity resolver
            XMLReader reader = spf.newSAXParser().getXMLReader();
            reader.setEntityResolver(this);
            // Parse XML
            InputStream inputStream = new ByteArrayInputStream(data);
            reader.parse(new InputSource(inputStream));
        } catch (Exception e) {
            caughtEx = e;
        }
        return caughtEx;
    }

    // Parse XML content that includes faulty XML content with xi:include element.
    // XML data is parsed by DOM parser
    private Exception parseDomAndXinclude(byte[] data) {
        Exception caughtEx = null;
        try {
            // Create DOM builder factory and make it xi:include aware
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setXIncludeAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            // Set this test class as entity resolver
            db.setEntityResolver(this);
            InputStream inputStream = new ByteArrayInputStream(data);
            // Parse XML
            db.parse(inputStream);
        } catch (Exception e) {
            caughtEx = e;
        }
        return caughtEx;
    }

    // EntityResolver method to intercept load of test XML file content and
    // redirect it to ByteArrayInputStream
    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
        if (systemId != null && systemId.endsWith(XINCLUDE_TEST_FN)) {
            return new InputSource(
                    new ByteArrayInputStream(
                            generateXmlBytes("Wrong byte is ", "here", 0xFE)
                    )
            );
        }
        return null;
    }

    // Construct XML content with invalid byte in xml element
    private static byte[] invalidByteInXmlElement() {
        final String prefix = "<?xml version=\"1.0\"?>\n<test>\n<bad-encoding>";
        final String postfix = "</bad-encoding></test>";
        return generateXmlBytes(prefix, postfix, 0xFA);
    }

    // Construct XML content with invalid byte in xml version declaration
    private static byte[] invalidByteInXmlVersionDecl() {
        final String prefix = "<?xml version=\"";
        final String postfix = "1.0\"?><test><bad-encoding></bad-encoding></test>";
        return generateXmlBytes(prefix, postfix, 0xFB);
    }

    // Construct XML content with invalid byte in xml attribute
    private static byte[] invalidByteInXmlAttribute() {
        final String prefix = "<?xml version=\"1.0\"?>\n<test>\n\n<bad-att attribute=\"";
        final String postfix = "\"></bad-att></test>";
        return generateXmlBytes(prefix, postfix, 0xFC);
    }

    // Test helper function to generate XML text with invalid UTF-8 byte inside
    private static byte[] generateXmlBytes(String prefix, String postfix, int b) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(prefix.getBytes());
            baos.write(b);
            baos.write(postfix.getBytes());
            return baos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    // XML file name to be included with xi:include directive
    private final static String XINCLUDE_TEST_FN = "xincludeTestFile.xml";

    // xi:include test XML file that includes xml content with invalid byte
    private final static String XINCLUDE_TEST_XML =
            "<?xml version=\"1.0\"?>\n" +
                    "<!DOCTYPE testXInclude [\n" +
                    "<!ENTITY xincludeTestFile \""+XINCLUDE_TEST_FN+"\">]>\n" +
                    "<testXInclude xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n" +
                    "<xi:include href=\"&xincludeTestFile;\" parse=\"text\"/>\n" +
                    "</testXInclude>";
}
