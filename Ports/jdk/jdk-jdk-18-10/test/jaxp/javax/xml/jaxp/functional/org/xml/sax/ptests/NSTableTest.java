/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
package org.xml.sax.ptests;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.XMLReader;

/**
 * Class containing the test cases for Namespace Table defined at
 * http://www.megginson.com/SAX/Java/namespaces.html
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.xml.sax.ptests.NSTableTest
 * @run testng/othervm org.xml.sax.ptests.NSTableTest
 */
@Listeners({jaxp.library.BasePolicy.class})
public class NSTableTest {
    private static final String NAMESPACES =
                        "http://xml.org/sax/features/namespaces";
    private static final String NAMESPACE_PREFIXES =
                        "http://xml.org/sax/features/namespace-prefixes";

    /**
     * Here namespace processing and namespace-prefixes are enabled.
     * The testcase tests XMLReader for this.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void xrNSTable01() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();

        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setFeature(NAMESPACE_PREFIXES, true);

        assertTrue(xmlReader.getFeature(NAMESPACES));
        assertTrue(xmlReader.getFeature(NAMESPACE_PREFIXES));
    }

    /**
     * Here namespace processing is enabled. This will make namespace-prefixes
     * disabled. The testcase tests XMLReader for this.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void xrNSTable02() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);

        XMLReader xmlReader = spf.newSAXParser().getXMLReader();
        assertTrue(xmlReader.getFeature(NAMESPACES));
        assertFalse(xmlReader.getFeature(NAMESPACE_PREFIXES));
    }

    /**
     * Here namespace processing is disabled. This will make namespace-prefixes
     * enabled. The testcase tests XMLReader for this.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void xrNSTable03() throws Exception {
        XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        assertFalse(xmlReader.getFeature(NAMESPACES));
        assertTrue(xmlReader.getFeature(NAMESPACE_PREFIXES));
    }

    /**
     * Here namespace processing is disabled, and namespace-prefixes is
     * disabled. This will make namespace processing on.The testcase tests
     * XMLReader for this.  This behavior only apply to crimson, not
     * XERCES.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void xrNSTable04() throws Exception {
        XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        xmlReader.setFeature(NAMESPACE_PREFIXES, false);
        assertFalse(xmlReader.getFeature(NAMESPACE_PREFIXES));
    }

    /**
     * Here namespace processing and namespace-prefixes are enabled.
     * The testcase tests SAXParserFactory for this.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void spNSTable01() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        spf.setFeature(NAMESPACE_PREFIXES,true);
        assertTrue(spf.getFeature(NAMESPACES));
        assertTrue(spf.getFeature(NAMESPACE_PREFIXES));
    }

    /**
     * Here namespace processing is enabled. This will make namespace-prefixes
     * disabled. The testcase tests SAXParserFactory for this.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void spNSTable02() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        assertTrue(spf.getFeature(NAMESPACES));
        assertFalse(spf.getFeature(NAMESPACE_PREFIXES));
    }

    /**
     * Here namespace processing is disabled. This will make namespace-prefixes
     * enabled. The testcase tests SAXParserFactory for this.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void spNSTable03() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        assertFalse(spf.getFeature(NAMESPACES));
        assertTrue(spf.getFeature(NAMESPACE_PREFIXES));
    }
    /**
     * Here namespace processing is disabled, and namespace-prefixes is
     * disabled. This will make namespace processing on.The testcase tests
     * SAXParserFactory for this.  This behavior only apply to crimson,
     * not xerces.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void spNSTable04() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature(NAMESPACE_PREFIXES, false);
        assertFalse(spf.getFeature(NAMESPACE_PREFIXES));
    }
}
