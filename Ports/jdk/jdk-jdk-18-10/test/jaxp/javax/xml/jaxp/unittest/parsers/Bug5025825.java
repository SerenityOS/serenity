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

import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/*
 * @test
 * @bug 5025825
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.Bug5025825
 * @run testng/othervm parsers.Bug5025825
 * @summary Test if SAXParserFactory set a Schema object, when SAXParser sets "http://java.sun.com/xml/jaxp/properties/schemaSource" property
 * and/or "http://java.sun.com/xml/jaxp/properties/schemaLanguage" property, it shall throw SAXException.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug5025825 {

    String schemaSource = "<?xml version='1.0'?>\n" + "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'>\n" + "  <xsd:element name='test101'>\n"
            + "    <xsd:complexType>\n" + "      <xsd:attribute name='attr'/>\n" + "      <xsd:attribute name='attr2' default='DEF'/>\n"
            + "    </xsd:complexType>\n" + "  </xsd:element>\n" + "</xsd:schema>\n";

    private Schema createSchema() throws SAXException {
        SchemaFactory schFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return schFactory.newSchema(new StreamSource(new StringReader(schemaSource)));
    }

    @Test
    public void test1() throws Exception {
        Schema sch = createSchema();
        Assert.assertNotNull(sch);

        SAXParserFactory spFactory = SAXParserFactory.newInstance();
        spFactory.setNamespaceAware(true);
        spFactory.setValidating(true);
        spFactory.setSchema(sch);

        SAXParser sParser = spFactory.newSAXParser();

        final String aSchemaLanguage = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
        final String aSchemaSource = "http://java.sun.com/xml/jaxp/properties/schemaSource";

        try {
            sParser.setProperty(aSchemaLanguage, "http://www.w3.org/2001/XMLSchema");
            Assert.fail("---- Set schemaLanguage: " + sParser.getProperty(aSchemaLanguage));
        } catch (SAXException e) {
            ; // as expected
        }

        try {
            sParser.setProperty(aSchemaSource, new InputSource(new StringReader(schemaSource)));
            Assert.fail("---- Set schemaSource: " + sParser.getProperty(aSchemaSource));
        } catch (SAXException e) {
            ; // as expected
        }
    }
}
