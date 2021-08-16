/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package common;

import static jaxp.library.JAXPTestUtilities.setSystemProperty;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;

/*
 * @test
 * @bug 8144593
 * @key intermittent
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @compile -XDignore.symbol.file TestSAXDriver.java
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow common.ValidationWarningsTest
 * @run testng/othervm common.ValidationWarningsTest
 * @summary Check that warnings about unsupported properties from SAX
 *  parsers are suppressed during the xml validation process.
 */
@Listeners({jaxp.library.InternalAPIPolicy.class})
public class ValidationWarningsTest extends WarningsTestBase {

    @BeforeClass
    public void setup() {
        //Set test SAX driver implementation.
        setSystemProperty("org.xml.sax.driver", "common.TestSAXDriver");
    }

    @Test
    public void testValidation() throws Exception {
        startTest();
    }

    //One iteration of xml validation test case. It will be called from each
    //TestWorker task defined in WarningsTestBase class.
    void doOneTestIteration() throws Exception {
        Source src = new StreamSource(new StringReader(xml));
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        SAXSource xsdSource = new SAXSource(new InputSource(new ByteArrayInputStream(xsd.getBytes())));
        Schema schema = schemaFactory.newSchema(xsdSource);
        Validator v = schema.newValidator();
        v.validate(src);
    }

    //Xsd and Xml contents used in the validation test
    private static final String xsd = "<?xml version='1.0'?>"
            + " <xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>"
            + " <xs:element name='test' type='xs:string'/>\n"
            + " </xs:schema>";
    private static final String xml = "<?xml version='1.0'?><test>Element</test>";

}
