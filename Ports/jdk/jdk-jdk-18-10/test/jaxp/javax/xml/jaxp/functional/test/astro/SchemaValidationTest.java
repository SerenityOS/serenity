/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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
package test.astro;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static test.astro.AstroConstants.ASTROCAT;
import static test.astro.AstroConstants.JAXP_SCHEMA_LANGUAGE;
import static test.astro.AstroConstants.JAXP_SCHEMA_SOURCE;

import java.io.File;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow test.astro.SchemaValidationTest
 * @run testng/othervm test.astro.SchemaValidationTest
 * @summary test parser sets schema related properties to do validation
 */
@Listeners({jaxp.library.FilePolicy.class})
public class SchemaValidationTest {
    /*
     * Only set the schemaLanguage, without setting schemaSource. It should
     * work.
     */
    @Test
    public void testSchemaValidation() throws Exception {
        SAXParser sp = getValidatingParser();
        sp.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA_NS_URI);
        sp.parse(new File(ASTROCAT), new DefaultHandler());
    }

    /*
     * Test SAXException shall be thrown if schemaSource is set but
     * schemaLanguage is not set.
     */
    @Test(expectedExceptions = SAXException.class)
    public void testSchemaValidationNeg() throws Exception {
        SAXParser sp = getValidatingParser();
        sp.setProperty(JAXP_SCHEMA_SOURCE, "catalog.xsd");
        sp.parse(new File(ASTROCAT), new DefaultHandler());
    }

    private SAXParser getValidatingParser() throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        spf.setValidating(true);
        return spf.newSAXParser();
    }
}
