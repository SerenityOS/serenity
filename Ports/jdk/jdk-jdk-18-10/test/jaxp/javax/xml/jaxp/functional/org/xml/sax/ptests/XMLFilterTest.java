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
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Unit test for XMLFilter.
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.xml.sax.ptests.XMLFilterTest
 * @run testng/othervm org.xml.sax.ptests.XMLFilterTest
 */
@Listeners({jaxp.library.FilePolicy.class})
public class XMLFilterTest {
    /**
     * name spaces constant.
     */
    private static final String NAMESPACES =
                "http://xml.org/sax/features/namespaces";

    /**
     * name spaces prefixes constant.
     */
    private static final String NAMESPACE_PREFIXES =
                "http://xml.org/sax/features/namespace-prefixes";

    /**
     * No exception expected when set a correct content handler.
     */
    @Test
    public void contentHandler01() {
        XMLFilterImpl xmlFilter = new XMLFilterImpl();
        xmlFilter.setContentHandler(xmlFilter);
        assertNotNull(xmlFilter.getContentHandler());
    }

    /**
     * No exception is expected when set content handler as null.
     */
    @Test
    public void contentHandler02() {
        new XMLFilterImpl().setContentHandler(null);
    }

    /**
     * No exception expected when set a correct entity solver.
     */
    @Test
    public void entity01() {
        XMLFilterImpl xmlFilter = new XMLFilterImpl();
        xmlFilter.setEntityResolver(xmlFilter);
        assertNotNull(xmlFilter.getEntityResolver());
    }

    /**
     * No exception is expected when set entity resolver as null.
     */
    @Test
    public void entity02() {
        new XMLFilterImpl().setEntityResolver(null);
    }

    /**
     * No exception expected when set a correct DTD handler.
     */
    @Test
    public void dtdHandler01() {
        XMLFilterImpl xmlFilter = new XMLFilterImpl();
        xmlFilter.setDTDHandler(xmlFilter);
        assertNotNull(xmlFilter.getDTDHandler());
    }

    /**
     * No exception is expected when set DTD handler as null.
     */
    @Test
    public void dtdHandler02() {
        new XMLFilterImpl().setDTDHandler(null);
    }

    /**
     * No exception expected when set a correct error handler.
     */
    @Test
    public void errorHandler01() {
        XMLFilterImpl xmlFilter = new XMLFilterImpl();
        xmlFilter.setErrorHandler(xmlFilter);
        assertNotNull(xmlFilter.getErrorHandler());
    }

    /**
     * No exception is expected when set error handler as null.
     */
    @Test
    public void errorHandler02() {
        new XMLFilterImpl().setErrorHandler(null);
    }

    /**
     * By default true is expected get namespaces feature.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void getFeature01() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        XMLReader xmlReader = spf.newSAXParser().getXMLReader();

        XMLFilterImpl xmlFilter = new XMLFilterImpl();
        xmlFilter.setParent(xmlReader);
        assertTrue(xmlFilter.getFeature(NAMESPACES));
    }

    /**
     * By default false is expected get namespaces-prefix feature.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void getFeature02() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        XMLFilterImpl xmlFilter = new XMLFilterImpl();
        xmlFilter.setParent(spf.newSAXParser().getXMLReader());
        assertFalse(xmlFilter.getFeature(NAMESPACE_PREFIXES));
    }

    /**
     * SAXNotRecognizedException is expected when get a feature by an invalid
     * feature name.
     *
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = SAXNotRecognizedException.class)
    public void getFeature03() throws Exception {
        new XMLFilterImpl().getFeature("no-meaning-feature");
    }

    /**
     * Set namespaces feature to a value to XMLFilter. it's expected same when
     * obtain it again.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void setFeature01() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);

        XMLFilterImpl xmlFilter = new XMLFilterImpl();
        xmlFilter.setParent(spf.newSAXParser().getXMLReader());
        xmlFilter.setFeature(NAMESPACES, false);
        assertFalse(xmlFilter.getFeature(NAMESPACES));
        xmlFilter.setFeature(NAMESPACES, true);
        assertTrue(xmlFilter.getFeature(NAMESPACES));
    }

    /**
     * Set namespaces-prefix feature to a value to XMLFilter. it's expected same
     * when obtain it again.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void setFeature02() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);

        XMLFilterImpl xmlFilter = new XMLFilterImpl();
        xmlFilter.setParent(spf.newSAXParser().getXMLReader());
        xmlFilter.setFeature(NAMESPACE_PREFIXES, false);
        assertFalse(xmlFilter.getFeature(NAMESPACE_PREFIXES));
        xmlFilter.setFeature(NAMESPACE_PREFIXES, true);
        assertTrue(xmlFilter.getFeature(NAMESPACE_PREFIXES));
    }

    /**
     * NullPointerException is expected when parse a null InputSource.
     *
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void parse01() throws Exception {
        new XMLFilterImpl().parse((InputSource)null);
    }

    /**
     * SAXException is expected when parsing a invalid formatted XML file.
     *
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void parse02() throws Exception {
        try(FileInputStream fis = new FileInputStream(XML_DIR + "invalid.xml")) {
            new XMLFilterImpl().parse(new InputSource(fis));
        }
    }

    /**
     * No exception when parse a normal XML file.
     *
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void parse03() throws Exception {
        try(FileInputStream fis = new FileInputStream(XML_DIR + "correct2.xml")) {
            new XMLFilterImpl().parse(new InputSource(fis));
        }
    }
}
