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
import java.io.InputStreamReader;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.validation.ValidatorHandler;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 5011500
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug5011500
 * @run testng/othervm validation.Bug5011500
 * @summary Test ValidatorHanlder and Validator can work for the xml document.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug5011500 {

    public static final String XSD = "<?xml version='1.0'?>\n" + "<schema xmlns='http://www.w3.org/2001/XMLSchema'\n" + "        xmlns:test='jaxp13_test'\n"
            + "        targetNamespace='jaxp13_test'>\n" + "    <element name='test'>\n" + "        <complexType>\n" + "            <sequence>\n"
            + "                <element name='child' type='string'/>\n" + "            </sequence>\n" + "            <attribute name='id' type='ID'/>\n"
            + "        </complexType>\n" + "    </element>\n" + "</schema>\n";

    public static final String XML = "<?xml version='1.0'?>\n" + "<?test v01?>\n" + "<ns:test xmlns:ns='jaxp13_test' id='i001'>\n"
            + "  <child>123abc</child>\n" + "</ns:test>\n";

    private ValidatorHandler validatorHandler;
    private Validator validator;

    private XMLReader createXMLReader() throws Exception {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);

        return parserFactory.newSAXParser().getXMLReader();
    }

    private void parse(XMLReader xmlReader, String xml) throws Exception {
        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(xml.getBytes()));
        InputSource inSource = new InputSource(reader);

        xmlReader.parse(inSource);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(XSD.getBytes()));
        StreamSource xsdSource = new StreamSource(reader);

        Schema schema = schemaFactory.newSchema(xsdSource);

        this.validatorHandler = schema.newValidatorHandler();
        this.validator = schema.newValidator();
    }

    @Test
    public void test1() throws Exception {
        DefaultHandler contentHandler = new DefaultHandler();
        validatorHandler.setContentHandler(contentHandler);
        validatorHandler.setErrorHandler(contentHandler);

        XMLReader xmlReader = createXMLReader();
        xmlReader.setContentHandler(validatorHandler);
        parse(xmlReader, XML);
    }

    @Test
    public void test2() throws Exception {
        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(XML.getBytes()));
        StreamSource xmlSource = new StreamSource(reader);

        validator.validate(xmlSource);
    }
}
