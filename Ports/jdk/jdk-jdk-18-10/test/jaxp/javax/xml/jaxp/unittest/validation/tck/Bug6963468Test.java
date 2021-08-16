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

package validation.tck;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 6963468
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.tck.Bug6963468Test
 * @run testng/othervm validation.tck.Bug6963468Test
 * @summary Test Validation allows element a is a union type and element b specifies a as its substitution group and b type is or is derived from one of the member types of the union.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6963468Test {
    static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    @Test
    public void test() {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            Schema schema = schemaFactory.newSchema(new StreamSource(Bug6963468Test.class.getResourceAsStream("Bug6963468.xsd")));

            Validator validator = schema.newValidator();
            validator.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            validator.setErrorHandler(new ErrorHandler() {
                public void error(SAXParseException exception) throws SAXException {
                    exception.printStackTrace();
                }

                public void fatalError(SAXParseException exception) throws SAXException {
                    exception.printStackTrace();
                }

                public void warning(SAXParseException exception) throws SAXException {
                    exception.printStackTrace();
                }
            });

            validator.validate(new StreamSource(Bug6963468Test.class.getResourceAsStream("Bug6963468.xml")));

        } catch (SAXException e) {
            System.out.println(e.getMessage());
            // fail(e.getMessage());

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            // fail(e.getMessage());
        }
    }

    @Test
    public void testInstance() throws ParserConfigurationException, SAXException, IOException {
        System.out.println(Bug6963468Test.class.getResource("Bug6963468.xsd").getPath());
        File schemaFile = new File(Bug6963468Test.class.getResource("Bug6963468.xsd").getPath());
        SAXParser parser = createParser(schemaFile);

        try {
            parser.parse(Bug6963468Test.class.getResource("Bug6963468.xml").getPath(), new DefaultHandler());
        } catch (SAXException e) {
            e.printStackTrace();
            Assert.fail("Fatal Error: " + strException(e));
        }

    }

    protected SAXParser createParser(File schema) throws ParserConfigurationException, SAXException {

        // create and initialize the parser
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        spf.setValidating(true);
        SAXParser parser = spf.newSAXParser();
        parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");

        // set schemaLocation if possible
        try {
            parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", schema);
        } catch (SAXNotRecognizedException e) {
            System.out.println("Warning: Property 'http://java.sun.com/xml/jaxp/properties/schemaSource' is not recognized.");
        } catch (SAXNotSupportedException e) {
            System.out.println("Warning: Property 'http://java.sun.com/xml/jaxp/properties/schemaSource' is not supported.");
        }

        return parser;
    }

    protected static String strException(Exception ex) {
        StringBuffer sb = new StringBuffer();

        while (ex != null) {
            if (ex instanceof SAXParseException) {
                SAXParseException e = (SAXParseException) ex;
                sb.append("" + e.getSystemId() + "(" + e.getLineNumber() + "," + e.getColumnNumber() + "): " + e.getMessage());
                ex = e.getException();
            } else {
                sb.append(ex);
                ex = null;
            }
        }
        return sb.toString();
    }

}
