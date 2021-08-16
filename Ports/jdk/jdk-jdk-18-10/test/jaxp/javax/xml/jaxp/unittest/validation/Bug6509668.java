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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.TypeInfoProvider;
import javax.xml.validation.ValidatorHandler;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.TypeInfo;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 6509668
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug6509668
 * @run testng/othervm validation.Bug6509668
 * @summary Test TypeInfoProvider.getElementTypeInfo() for union type when startElement and endElement.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6509668 {

    public static final String XSD = "<?xml version='1.0'?>\n" + "<schema xmlns='http://www.w3.org/2001/XMLSchema'\n"
            + "  xmlns:ns='http://example.org/jaxp13_test'\n" + "    targetNamespace='http://example.org/jaxp13_test'\n" + "    elementFormDefault='qualified'>\n"
            + "  <simpleType name='intOrString'>\n" + "    <union memberTypes='int string'/>\n" + "  </simpleType>\n"
            + "  <element name='test' type='ns:intOrString'/>\n" + "</schema>\n";

    public static final String XML = "<?xml version='1.0'?>\n" + "<ns:test xmlns:ns='http://example.org/jaxp13_test'>abc</ns:test>\n";

    private ValidatorHandler createValidatorHandler(String xsd) throws SAXException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(xsd.getBytes()));
        StreamSource xsdSource = new StreamSource(reader);

        Schema schema = schemaFactory.newSchema(xsdSource);
        return schema.newValidatorHandler();
    }

    private XMLReader createXMLReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        if (!parserFactory.isNamespaceAware()) {
            parserFactory.setNamespaceAware(true);
        }

        return parserFactory.newSAXParser().getXMLReader();
    }

    private void parse(XMLReader xmlReader, String xml) throws SAXException, IOException {
        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(xml.getBytes()));
        InputSource inSource = new InputSource(reader);

        xmlReader.parse(inSource);
    }

    @Test
    public void testGetElementTypeInfo() throws ParserConfigurationException, SAXException, IOException {
        XMLReader xmlReader;
        xmlReader = createXMLReader();

        final ValidatorHandler validatorHandler;
        validatorHandler = createValidatorHandler(XSD);

        xmlReader.setContentHandler(validatorHandler);

        DefaultHandler handler = new DefaultHandler() {
            public void startElement(String uri, String localName, String qName, Attributes attr) throws SAXException {
                TypeInfoProvider infoProvider = null;
                synchronized (validatorHandler) {
                    infoProvider = validatorHandler.getTypeInfoProvider();
                }
                if (infoProvider == null) {
                    throw new SAXException("Can't obtain TypeInfoProvider object.");
                }

                try {
                    TypeInfo typeInfo = infoProvider.getElementTypeInfo();
                    Assert.assertEquals(typeInfo.getTypeName(), "intOrString");
                } catch (IllegalStateException e) {
                    System.out.println(e);
                    throw new SAXException("Unexpected IllegalStateException was thrown.");
                }
            }

            public void endElement(String uri, String localName, String qName) throws SAXException {
                TypeInfoProvider infoProvider = null;
                synchronized (validatorHandler) {
                    infoProvider = validatorHandler.getTypeInfoProvider();
                }
                if (infoProvider == null) {
                    throw new SAXException("Can't obtain TypeInfoProvider object.");
                }

                try {
                    TypeInfo typeInfo = infoProvider.getElementTypeInfo();
                    Assert.assertEquals(typeInfo.getTypeName(), "string");
                } catch (IllegalStateException e) {
                    System.out.println(e);
                    throw new SAXException("Unexpected IllegalStateException was thrown.");
                }
            }
        };
        validatorHandler.setContentHandler(handler);

        parse(xmlReader, XML);
    }
}
