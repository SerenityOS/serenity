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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.xml.sax.ptests.SAXTestConst.XML_DIR;

import java.io.FileInputStream;

import javax.xml.parsers.SAXParserFactory;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.ParserAdapter;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderAdapter;


/**
 * Unit test cases for ParserAdapter API. By default the only features recognized
 * are namespaces and namespace-prefixes.
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.xml.sax.ptests.ParserAdapterTest
 * @run testng/othervm org.xml.sax.ptests.ParserAdapterTest
 */
@Listeners({jaxp.library.FilePolicy.class})
public class ParserAdapterTest {
    /**
     * namespaces feature name.
     */
    private static final String NAMESPACES =
                "http://xml.org/sax/features/namespaces";

    /**
     * namespaces-prefiexs feature name.
     */
    private static final String NAMESPACE_PREFIXES =
                "http://xml.org/sax/features/namespace-prefixes";

    /**
     * ParserAdapter instance to share by all tests.
     */
    private final ParserAdapter parserAdapter;

    /**
     * Initiate ParserAdapter.
     * @throws Exception If any errors occur.
     */
    ParserAdapterTest() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        XMLReader xmlReader = spf.newSAXParser().getXMLReader();
        XMLReaderAdapter xmlReaderAdapter = new XMLReaderAdapter(xmlReader);
        parserAdapter = new ParserAdapter(xmlReaderAdapter);
    }

    /**
     * Verifies parserAdapter.getContentHandler()
     */
    @Test
    public void contentHandler01() {
        ContentHandler contentHandler = new XMLFilterImpl();
        parserAdapter.setContentHandler(contentHandler);
        assertNotNull(parserAdapter.getContentHandler());
    }

    /**
     * No exception is expected when set content handler as null.
     */
    @Test
    public void contentHandler02() {
        parserAdapter.setContentHandler(null);
    }

    /**
     * Verifies parserAdapter.getEntityResolver()
     */
    @Test
    public void entity01() {
        XMLFilterImpl xmlFilter = new XMLFilterImpl();
        parserAdapter.setEntityResolver(xmlFilter);
        assertNotNull(parserAdapter.getEntityResolver());
    }

    /**
     * No exception is expected when set entity resolver as null.
     */
    @Test
    public void entity02() {
        parserAdapter.setEntityResolver(null);
    }

    /**
     * Verifies parserAdapter.getDTDHandler()
     */
    @Test
    public void dtdHandler01() {
        XMLFilterImpl xmlFilter = new XMLFilterImpl();
        parserAdapter.setDTDHandler(xmlFilter);
        assertNotNull(parserAdapter.getDTDHandler());
    }

    /**
     * No exception is expected when set DTD handler as null.
     */
    @Test
    public void dtdHandler02() {
        parserAdapter.setDTDHandler(null);
    }

    /**
     * Verifies parserAdapter.getErrorHandler()
     */
    @Test
    public void errorHandler01() {
        XMLFilterImpl eHandler = new XMLFilterImpl();
        parserAdapter.setErrorHandler(eHandler);
        assertNotNull(parserAdapter.getErrorHandler());
    }

    /**
     * No exception is expected when set error handler as null.
     */
    @Test
    public void errorHandler02() {
        parserAdapter.setErrorHandler(null);
    }

    /**
     * parserAdapter.getFeature(NAMESPACES) returns true be default.
     *
     * @exception Exception If any errors occur.
     */
    @Test
    public void getFeature01() throws Exception {
        assertTrue(parserAdapter.getFeature(NAMESPACES));
    }

    /**
     * parserAdapter.getFeature(NAMESPACE_PREFIXES) returns true be default.
     *
     * @exception Exception If any errors occur.
     */
    @Test
    public void getFeature02() throws Exception {
        assertFalse(parserAdapter.getFeature(NAMESPACE_PREFIXES));
    }

    /**
     * SAXNotRecognizedException thrown when feature name is not known one.
     *
     * @exception Exception If any errors occur.
     */
    @Test(expectedExceptions = SAXNotRecognizedException.class)
    public void getFeature03() throws Exception {
        parserAdapter.getFeature("no-meaning-feature");
    }

    /**
     * Obtain getFeature after it's set returns set value.
     *
     * @exception Exception If any errors occur.
     */
    @Test
    public void setFeature01() throws Exception {
        parserAdapter.setFeature(NAMESPACES, false);
        assertFalse(parserAdapter.getFeature(NAMESPACES));
    }

    /**
     * Obtain getFeature after it's set returns set value.
     *
     * @exception Exception If any errors occur.
     */
    @Test
    public void setFeature02() throws Exception {
        parserAdapter.setFeature(NAMESPACE_PREFIXES, false);
        assertFalse(parserAdapter.getFeature(NAMESPACE_PREFIXES));
    }

    /**
     * Obtain getFeature after it's set returns set value.
     *
     * @exception Exception If any errors occur.
     */
    @Test
    public void setFeature03() throws Exception {
        parserAdapter.setFeature(NAMESPACES, true);
        assertTrue(parserAdapter.getFeature(NAMESPACES));
    }

    /**
     * Obtain getFeature after it's set returns set value.
     *
     * @exception Exception If any errors occur.
     */
    @Test
    public void setFeature04() throws Exception {
        parserAdapter.setFeature(NAMESPACE_PREFIXES, true);
        assertTrue(parserAdapter.getFeature(NAMESPACE_PREFIXES));
    }

    /**
     * NPE expected when parsing a null object by ParserAdapter.
     *
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void parse01() throws Exception {
        parserAdapter.parse((InputSource)null);
    }

    /**
     * SAXException expected when parsing a wrong-formatter XML with ParserAdapter.
     *
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = SAXException.class)
    public void parse02() throws Exception {
        try(FileInputStream fis = new FileInputStream(XML_DIR + "invalid.xml")) {
            InputSource is = new InputSource(fis);
            parserAdapter.parse(is);
        }
    }

    /**
     * Parse a well-formatter XML with ParserAdapter.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void parse03() throws Exception {
        try(FileInputStream fis = new FileInputStream(XML_DIR + "correct.xml")) {
            InputSource is = new InputSource(fis);
            parserAdapter.parse(is);
        }
    }
}
