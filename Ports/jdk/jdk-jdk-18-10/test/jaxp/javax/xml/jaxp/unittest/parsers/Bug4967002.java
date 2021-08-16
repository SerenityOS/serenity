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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;

/*
 * @test
 * @bug 4967002
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.Bug4967002
 * @run testng/othervm parsers.Bug4967002
 * @summary Test DocumentBuilderFactory.newDocumentBuilder() throws ParserConfigurationException
 * when it uses the "http://java.sun.com/xml/jaxp/properties/schemaSource" property
 * and/or the "http://java.sun.com/xml/jaxp/properties/schemaLanguage" property
 * in conjunction with setting a Schema object.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug4967002 {
    String schemaSource = "<?xml version='1.0'?>\n" + "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'>\n" + "  <xsd:element name='test101'>\n"
            + "    <xsd:complexType>\n" + "      <xsd:attribute name='attr'/>\n" + "      <xsd:attribute name='attr2' default='DEF'/>\n"
            + "    </xsd:complexType>\n" + "  </xsd:element>\n" + "</xsd:schema>\n";

    Schema createSchema() {
        SchemaFactory schFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema sch = schFactory.newSchema(new StreamSource(new StringReader(schemaSource)));
            return sch;
        } catch (Exception se) {
            throw new IllegalStateException("No Schema : " + se);
        }
    }

    @Test
    public void test1() {
        setAttr(true);
    }

    @Test
    public void test2() {
        setAttr(false);
    }

    void setAttr(boolean setSrc) {
        DocumentBuilderFactory docBFactory = DocumentBuilderFactory.newInstance();
        Schema sch = createSchema();
        docBFactory.setSchema(sch);
        docBFactory.setNamespaceAware(true);
        docBFactory.setValidating(true);

        final String aSchemaLanguage = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
        final String aSchemaSource = "http://java.sun.com/xml/jaxp/properties/schemaSource";

        docBFactory.setAttribute(aSchemaLanguage, "http://www.w3.org/2001/XMLSchema");
        // System.out.println("---- Set schemaLanguage: " +
        // docBFactory.getAttribute(aSchemaLanguage));
        if (setSrc) {
            docBFactory.setAttribute(aSchemaSource, new InputSource(new StringReader(schemaSource)));
            // System.out.println("---- Set schemaSource: " +
            // docBFactory.getAttribute(aSchemaSource));
        }

        try {
            docBFactory.newDocumentBuilder();
            Assert.fail("ParserConfigurationException expected");
        } catch (ParserConfigurationException pce) {
            return; // success
        }
    }
}
