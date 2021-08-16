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
package javax.xml.transform.ptests;

import static javax.xml.transform.ptests.TransformerTestConst.XML_DIR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * URIResolver should be invoked when transform happens.
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.transform.ptests.URIResolverTest
 * @run testng/othervm javax.xml.transform.ptests.URIResolverTest
 */
@Listeners({jaxp.library.FilePolicy.class})
public class URIResolverTest implements URIResolver {
    /**
     * System ID constant.
     */
    private final static String SYSTEM_ID = "file:///" + XML_DIR;

    /**
     * XML file include link file.
     */
    private final static String XSL_INCLUDE_FILE = XML_DIR + "citiesinclude.xsl";

    /**
     * XML file import link file.
     */
    private final static String XSL_IMPORT_FILE = XML_DIR + "citiesimport.xsl";

    /**
     * TEMP XML file.
     */
    private final static String XSL_TEMP_FILE = "temp/cities.xsl";

    /**
     * expected HREF.
     */
    private final String validateHref;

    /**
     * expected Base URI.
     */
    private final String validateBase;

    /**
     * Default constructor for testng invocation.
     */
    public URIResolverTest(){
        validateHref = null;
        validateBase = null;
    }

    /**
     * Constructor for setting expected Href and expected Base URI.
     * @param validateHref expected Href
     * @param validateBase expected Base URI
     */
    public URIResolverTest(String validateHref, String validateBase){
        this.validateHref = validateHref;
        this.validateBase = validateBase;
    }

    /**
     * Called by the processor when it encounters an xsl:include, xsl:import,
     * or document() function.
     * @param href An href attribute, which may be relative or absolute.
     * @param base The base URI against which the first argument will be made
     * absolute if the absolute URI is required.
     * @return null always.
     */
    @Override
    public Source resolve(String href, String base) {
        assertEquals(href, validateHref);
        assertEquals(base, validateBase);
        return null;
    }

    /**
     * This is to test the URIResolver.resolve() method when a transformer is
     * created using StreamSource. style-sheet file has xsl:include in it.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public static void resolver01() throws Exception {
        try (FileInputStream fis = new FileInputStream(XSL_INCLUDE_FILE)) {
            TransformerFactory tfactory = TransformerFactory.newInstance();
            URIResolverTest resolver = new URIResolverTest(XSL_TEMP_FILE, SYSTEM_ID);
            tfactory.setURIResolver(resolver);

            StreamSource streamSource = new StreamSource(fis);
            streamSource.setSystemId(SYSTEM_ID);
            assertNotNull(tfactory.newTransformer(streamSource));
        }
    }

    /**
     * This is to test the URIResolver.resolve() method when a transformer is
     * created using DOMSource. style-sheet file has xsl:include in it.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public static void resolver02() throws Exception {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        URIResolverTest resolver = new URIResolverTest(XSL_TEMP_FILE, SYSTEM_ID);
        tfactory.setURIResolver(resolver);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(XSL_INCLUDE_FILE);
        DOMSource domSource = new DOMSource(document, SYSTEM_ID);

        assertNotNull(tfactory.newTransformer(domSource));
    }

    /**
     * This is to test the URIResolver.resolve() method when a transformer is
     * created using SAXSource. style-sheet file has xsl:include in it.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public static void resolver03() throws Exception {
        try (FileInputStream fis = new FileInputStream(XSL_INCLUDE_FILE)){
            URIResolverTest resolver = new URIResolverTest(XSL_TEMP_FILE, SYSTEM_ID);
            TransformerFactory tfactory = TransformerFactory.newInstance();
            tfactory.setURIResolver(resolver);
            InputSource is = new InputSource(fis);
            is.setSystemId(SYSTEM_ID);
            SAXSource saxSource = new SAXSource(is);
            assertNotNull(tfactory.newTransformer(saxSource));
        }
    }

    /**
     * This is to test the URIResolver.resolve() method when a transformer is
     * created using StreamSource. style-sheet file has xsl:import in it.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public static void resolver04() throws Exception {
        try (FileInputStream fis = new FileInputStream(XSL_IMPORT_FILE)) {
            URIResolverTest resolver = new URIResolverTest(XSL_TEMP_FILE, SYSTEM_ID);
            TransformerFactory tfactory = TransformerFactory.newInstance();
            tfactory.setURIResolver(resolver);
            StreamSource streamSource = new StreamSource(fis);
            streamSource.setSystemId(SYSTEM_ID);
            assertNotNull(tfactory.newTransformer(streamSource));
        }
    }

    /**
     * This is to test the URIResolver.resolve() method when a transformer is
     * created using DOMSource. style-sheet file has xsl:import in it.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public static void resolver05() throws Exception {
        URIResolverTest resolver = new URIResolverTest(XSL_TEMP_FILE, SYSTEM_ID);
        TransformerFactory tfactory = TransformerFactory.newInstance();
        tfactory.setURIResolver(resolver);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(new File(XSL_IMPORT_FILE));
        DOMSource domSource = new DOMSource(document, SYSTEM_ID);
        assertNotNull(tfactory.newTransformer(domSource));
    }

    /**
     * This is to test the URIResolver.resolve() method when a transformer is
     * created using SAXSource. style-sheet file has xsl:import in it.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public static void resolver06() throws Exception {
        try (FileInputStream fis = new FileInputStream(XSL_IMPORT_FILE)){
            URIResolverTest resolver = new URIResolverTest(XSL_TEMP_FILE, SYSTEM_ID);
            TransformerFactory tfactory = TransformerFactory.newInstance();
            tfactory.setURIResolver(resolver);
            InputSource is = new InputSource(fis);
            is.setSystemId(SYSTEM_ID);
            SAXSource saxSource = new SAXSource(is);
            assertNotNull(tfactory.newTransformer(saxSource));
        }
    }

    /**
     * This is to test the URIResolver.resolve() method when there is an error
     * in the file.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public static void docResolver01() throws Exception {
        try (FileInputStream fis = new FileInputStream(XML_DIR + "doctest.xsl")) {
            URIResolverTest resolver = new URIResolverTest("temp/colors.xml", SYSTEM_ID);
            StreamSource streamSource = new StreamSource(fis);
            streamSource.setSystemId(SYSTEM_ID);

            Transformer transformer = TransformerFactory.newInstance().newTransformer(streamSource);
            transformer.setURIResolver(resolver);

            File f = new File(XML_DIR + "myFake.xml");
            Document document = DocumentBuilderFactory.newInstance().
                    newDocumentBuilder().parse(f);

            // Use a Transformer for output
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(System.err);
            // No exception is expected because resolver resolve wrong URI.
            transformer.transform(source, result);
        }
    }
}
