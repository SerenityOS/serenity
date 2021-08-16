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

import java.io.StringReader;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.TypeInfoProvider;
import javax.xml.validation.ValidatorHandler;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 4969732
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug4969732
 * @run testng/othervm validation.Bug4969732
 * @summary Test TypeInfoProvider.getElementTypeInfo() throws IllegalStateException
 * in case the method is not called from method startElement() or endElement().
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug4969732 {

    public static final String XSD = "<?xml version='1.0'?>\n" + "<schema xmlns='http://www.w3.org/2001/XMLSchema'\n" + "        xmlns:test='jaxp13_test'\n"
            + "        targetNamespace='jaxp13_test'\n" + "        elementFormDefault='qualified'>\n" + "    <element name='test' type='string'/>\n"
            + "</schema>\n";

    public static final String XML = "<?xml version='1.0'?>\n" + "<ns:test xmlns:ns='jaxp13_test'>1234abc</ns:test>\n";

    private ValidatorHandler createValidatorHandler(String xsd) throws SAXException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

        StringReader reader = new StringReader(xsd);
        StreamSource xsdSource = new StreamSource(reader);

        Schema schema = schemaFactory.newSchema(xsdSource);
        return schema.newValidatorHandler();
    }

    private XMLReader createXMLReader() throws Exception {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);

        return parserFactory.newSAXParser().getXMLReader();
    }

    private void parse(XMLReader xmlReader, String xml) throws Exception {
        StringReader reader = new StringReader(xml);
        InputSource inSource = new InputSource(reader);

        xmlReader.parse(inSource);
    }

    @Test
    public void test1() throws Exception {
        XMLReader xmlReader = createXMLReader();
        final ValidatorHandler validatorHandler = createValidatorHandler(XSD);
        xmlReader.setContentHandler(validatorHandler);

        DefaultHandler handler = new DefaultHandler() {
            public void characters(char[] ch, int start, int length) throws SAXException {
                TypeInfoProvider infoProvider = null;
                synchronized (validatorHandler) {
                    infoProvider = validatorHandler.getTypeInfoProvider();
                }
                if (infoProvider == null) {
                    Assert.fail("Can't obtain TypeInfo object.");
                }

                try {
                    infoProvider.getElementTypeInfo();
                    Assert.fail("IllegalStateException was not thrown.");
                } catch (IllegalStateException e) {
                    // as expected
                    System.out.println("OK");
                }
            }
        };
        validatorHandler.setContentHandler(handler);

        parse(xmlReader, XML);
    }
}
