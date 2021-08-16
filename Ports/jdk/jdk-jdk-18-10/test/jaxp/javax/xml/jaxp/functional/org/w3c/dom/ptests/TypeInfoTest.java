/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
package org.w3c.dom.ptests;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.testng.Assert.assertEquals;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.TypeInfo;
import org.xml.sax.InputSource;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.w3c.dom.ptests.TypeInfoTest
 * @run testng/othervm org.w3c.dom.ptests.TypeInfoTest
 * @summary Test getTypeName and getTypeNamespace methods of TypeInfo interface
 */
@Listeners({jaxp.library.BasePolicy.class})
public class TypeInfoTest {
    /*
     * Get the TypeInfo of the root element, and verify it.
     */
    @Test
    public void test() throws Exception {
        TypeInfo typeInfo = getTypeOfRoot(SCHEMA_INSTANCE, "<?xml version='1.0'?>\n" + "<test1 xmlns=\"testNS\"><code/></test1>\n");

        assertEquals(typeInfo.getTypeName(), "Test");
        assertEquals(typeInfo.getTypeNamespace(), "testNS");

    }

    private TypeInfo getTypeOfRoot(String schemaText, String docText) throws Exception {
        Element root = getRoot(schemaText, docText);
        return root.getSchemaTypeInfo();
    }

    private Element getRoot(String schemaText, String docText) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        InputSource inSchema = new InputSource(new StringReader(schemaText));
        inSchema.setSystemId("schema.xsd");
        dbf.setNamespaceAware(true);
        dbf.setValidating(true);
        dbf.setAttribute(SCHEMA_LANGUAGE, W3C_XML_SCHEMA_NS_URI);
        dbf.setAttribute(SCHEMA_SOURCE, inSchema);

        DocumentBuilder parser = dbf.newDocumentBuilder();

        InputSource inSource = new InputSource(new StringReader(docText));
        inSource.setSystemId("doc.xml");
        Document document = parser.parse(inSource);

        return document.getDocumentElement();
    }

    private static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    private static final String SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    /*
     * Schema instance
     */
    private static final String SCHEMA_INSTANCE =
            "<?xml version=\"1.0\"?>\n"
            + "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n"
            + "            xmlns:testNS=\"testNS\"\n"
            + "            targetNamespace=\"testNS\" elementFormDefault=\"qualified\">\n"
            + "    <xsd:element name=\"test1\" type=\"testNS:Test\"/>\n"
            + "    \n"
            + "    <xsd:complexType name=\"Test\">\n"
            + "        <xsd:sequence>\n"
            + "            <xsd:element name=\"description\" minOccurs=\"0\"/>\n"
            + "            <xsd:element name=\"code\"/>\n"
            + "        </xsd:sequence>\n"
            + "    </xsd:complexType>\n"
            + "\n"
            + "    <xsd:element name=\"test2\">\n"
            + "        <xsd:complexType>\n"
            + "            <xsd:sequence>\n"
            + "                <xsd:element name=\"description\" minOccurs=\"0\"/>\n"
            + "                <xsd:element name=\"code\"/>\n"
            + "            </xsd:sequence>\n"
            + "        </xsd:complexType>\n"
            + "    </xsd:element>\n"
            + "\n"
            + "    <xsd:element name=\"test3\" type=\"xsd:string\"/>\n"
            + "\n"
            + "    <xsd:element name=\"test4\" type=\"testNS:Test1\"/>\n"
            + "\n"
            + "    <xsd:simpleType name=\"Test1\">\n"
            + "        <xsd:restriction base=\"xsd:string\"/>\n"
            + "    </xsd:simpleType>\n"
            + "\n"
            + "    <xsd:element name=\"test5\">\n"
            + "        <xsd:simpleType>\n"
            + "            <xsd:restriction base=\"xsd:string\"/>\n"
            + "        </xsd:simpleType>\n"
            + "    </xsd:element>\n"
            + "\n"
            + "    <xsd:element name=\"test6\">\n"
            + "        <xsd:complexType>\n"
            + "            <xsd:complexContent>\n"
            + "                <xsd:extension base=\"testNS:Test\">\n"
            + "                    <xsd:attribute name=\"attr\" type=\"xsd:string\"/>\n"
            + "                </xsd:extension>\n"
            + "            </xsd:complexContent>\n"
            + "        </xsd:complexType>\n"
            + "    </xsd:element>\n"
            + "\n"
            + "</xsd:schema>\n";


}
