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

package dom;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.TypeInfo;

/*
 * @test
 * @bug 4966143
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.Bug4966143
 * @run testng/othervm dom.Bug4966143
 * @summary Test isDerivedFrom(...) returns true only if the parameter is DERIVATION_EXTENSION,
 * in case TypeInfo instance refers to a complex type derived from another complex type by extension.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug4966143 {

    static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    @Test
    public void test1() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(true);
        dbf.setAttribute(SCHEMA_LANGUAGE, XMLConstants.W3C_XML_SCHEMA_NS_URI);
        dbf.setAttribute(SCHEMA_SOURCE, Bug4966143.class.getResource("Bug4966143.xsd").toExternalForm());

        Document document = dbf.newDocumentBuilder().parse(Bug4966143.class.getResource("Bug4966143.xml").toExternalForm());

        TypeInfo type = document.getDocumentElement().getSchemaTypeInfo();

        Assert.assertFalse(type.isDerivedFrom("testNS", "Test", TypeInfo.DERIVATION_UNION));
        Assert.assertFalse(type.isDerivedFrom("testNS", "Test", TypeInfo.DERIVATION_LIST));
        Assert.assertFalse(type.isDerivedFrom("testNS", "Test", TypeInfo.DERIVATION_RESTRICTION));
        Assert.assertTrue(type.isDerivedFrom("testNS", "Test", TypeInfo.DERIVATION_EXTENSION));
        Assert.assertTrue(type.isDerivedFrom("testNS", "Test", 0));
    }
}
