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

import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 6974551
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.tck.Bug6974551Test
 * @run testng/othervm validation.tck.Bug6974551Test
 * @summary Test Validation for SAXParser can expose whitespace facet for xs:anySimpleType.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6974551Test {
    static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    static String _xml = Bug6974551Test.class.getResource("Bug6974551.xml").getPath();
    static String _xsd = Bug6974551Test.class.getResource("Bug6974551.xsd").getPath();

    @Test
    public void testSAX() {
        try {
            Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new StreamSource(_xsd));
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setValidating(true);
            spf.setSchema(schema);
            SAXParser parser = spf.newSAXParser();
            MyErrorHandler errorHandler = new MyErrorHandler();
            parser.parse(_xml, errorHandler);
            if (!errorHandler.errorOccured) {
                Assert.fail("should report error");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testValidationAPI() {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            Schema schema = schemaFactory.newSchema(new StreamSource(_xsd));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(_xml));

            Assert.fail("should report error");
        } catch (SAXException e) {
            // expected, pass
            System.out.println(e.getMessage());
        } catch (IOException e) {
            Assert.fail(e.getMessage());
            System.out.println(e.getMessage());
        }
    }

    class MyErrorHandler extends DefaultHandler {

        public boolean errorOccured = false;

        public void error(SAXParseException e) throws SAXException {

            System.err.println("Error: " + "[[" + e.getPublicId() + "]" + "[" + e.getSystemId() + "]]" + "[[" + e.getLineNumber() + "]" + "["
                    + e.getColumnNumber() + "]] " + e);

            errorOccured = true;
        }

        public void fatalError(SAXParseException e) throws SAXException {

            System.err.println("Fatal Error: " + e);

            errorOccured = true;
        }

        public void warning(SAXParseException e) throws SAXException {

            System.err.println("Warning: " + e);

            errorOccured = true;
        }
    }

}
