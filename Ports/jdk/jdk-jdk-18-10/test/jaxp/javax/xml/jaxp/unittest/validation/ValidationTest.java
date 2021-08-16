/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.io.FileInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLFilter;
import org.xml.sax.helpers.XMLFilterImpl;

/*
 * @test
 * @bug 8220818 8176447
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm validation.ValidationTest
 * @summary Runs validations with schemas and sources
 */
@Listeners({jaxp.library.FilePolicy.class})
public class ValidationTest {
    static final String FILE_PATH = "files/";
    /*
     DataProvider: valid xml
     */
    @DataProvider(name = "valid")
    Object[][] getValid() {
        return new Object[][]{
            {"JDK8220818a.xsd", "JDK8220818a_Valid.xml"},
            {"JDK8220818a.xsd", "JDK8220818a_Valid1.xml"},
            {"JDK8220818b.xsd", "JDK8220818b_Valid.xml"},
        };
    }

    /*
     DataProvider: invalid xml
     */
    @DataProvider(name = "invalid")
    Object[][] getInvalid() {
        return new Object[][]{
            {"JDK8220818a.xsd", "JDK8220818a_Invalid.xml"},
            {"JDK8220818b.xsd", "JDK8220818b_Invalid.xml"},
        };
    }

    /*
     DataProvider: uniqueness
     */
    @DataProvider(name = "uniqueness")
    Object[][] getUniqueData() {
        return new Object[][]{
            {"JDK8176447a.xsd", "JDK8176447a.xml"},
            {"JDK8176447b.xsd", "JDK8176447b.xml"},
        };
    }

    @Test(dataProvider = "invalid", expectedExceptions = SAXParseException.class)
    public void testValidateRefType(String xsd, String xml) throws Exception {
        validate(xsd, xml);
    }

    @Test(dataProvider = "valid")
    public void testValidateRefType1(String xsd, String xml) throws Exception {
        validate(xsd, xml);
    }

    /**
     * @bug 8176447
     * Verifies that the uniqueness constraint is checked.
     * @param xsd the XSD
     * @param xml the XML
     * @throws Exception expected when the uniqueness constraint is validated
     * correctly.
     */
    @Test(dataProvider = "uniqueness", expectedExceptions = SAXException.class)
    public void testUnique(String xsd, String xml) throws Exception {
        validate(xsd, xml);
    }

    /**
     * @bug 8068376
     * Verifies that validation performs normally with externally provided string
     * parameters.
     * @throws Exception if the test fails
     */
    @Test
    public void testJDK8068376() throws Exception {

        String xsdFile = getClass().getResource(FILE_PATH + "JDK8068376.xsd").getFile();
        String xmlFile = getClass().getResource(FILE_PATH + "JDK8068376.xml").getFile();
        String targetNamespace = getTargetNamespace(xsdFile);

        XMLFilter namespaceFilter = new XMLFilterImpl(SAXParserFactory.newDefaultNSInstance().newSAXParser().getXMLReader()) {
        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            uri = targetNamespace; // overwriting the uri with our own choice
            super.startElement(uri, localName, qName, atts);
        }
        };

        Source xmlSource = new SAXSource(namespaceFilter, new InputSource(xmlFile));
        Source schemaSource = new StreamSource(xsdFile);
        validate(schemaSource, xmlSource);

    }

    private static String getTargetNamespace(String xsdFile) throws Exception {
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new FileInputStream(xsdFile));
        while (reader.hasNext()) {
            int event = reader.next();

            // Get the root element's "targetNamespace" attribute
            if (event == XMLEvent.START_ELEMENT) {
                // validation fails before patch
                String value = reader.getAttributeValue(null, "targetNamespace"); // fails validation
                // validation passes due to a reference comparison in the original code
                // String value = "mynamespace";
                return value;
            }
        }
        return null;
    }

    private void validate(String xsd, String xml) throws Exception {
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(
                XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final Schema schema = schemaFactory.newSchema(
                new File(getClass().getResource(FILE_PATH + xsd).getFile()));
        final Validator validator = schema.newValidator();
        validator.validate(new StreamSource(
                new File(getClass().getResource(FILE_PATH + xml).getFile())));
    }

    private void validate(Source xsd, Source xml) throws Exception {
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(
                XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final Schema schema = schemaFactory.newSchema(xsd);
        final Validator validator = schema.newValidator();
        validator.validate(xml);
    }

}
