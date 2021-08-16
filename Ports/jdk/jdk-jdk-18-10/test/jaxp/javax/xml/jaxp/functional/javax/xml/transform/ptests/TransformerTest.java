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
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Basic test cases for Transformer API
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.transform.ptests.TransformerTest
 * @run testng/othervm javax.xml.transform.ptests.TransformerTest
 */
@Listeners({jaxp.library.FilePolicy.class})
public class TransformerTest {
    /**
     * XSLT file serves every test method.
     */
    private final static String TEST_XSL = XML_DIR + "cities.xsl";

    /**
     * This tests if newTransformer(StreamSource) method returns Transformer.
     * @throws TransformerConfigurationException If for some reason the
     *         TransformerHandler can not be created.
     */
    @Test
    public void transformer01() throws TransformerConfigurationException {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        StreamSource streamSource = new StreamSource(
                                    new File(TEST_XSL));
        Transformer transformer = tfactory.newTransformer(streamSource);
        assertNotNull(transformer);
    }

    /**
     * This tests if newTransformer(SAXSource) method returns Transformer.
     * @throws Exception If any errors occur.
     */
    @Test
    public void transformer02() throws Exception {
        try (FileInputStream fis = new FileInputStream(TEST_XSL)) {
            TransformerFactory tfactory = TransformerFactory.newInstance();
            SAXSource saxSource = new SAXSource(new InputSource(fis));
            Transformer transformer = tfactory.newTransformer(saxSource);
            assertNotNull(transformer);
        }
    }

    /**
     * This tests if newTransformer(DOMSource) method returns Transformer.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void transformer03() throws Exception {
        TransformerFactory tfactory = TransformerFactory.newInstance();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(new File(TEST_XSL));
        DOMSource domSource = new DOMSource(document);

        Transformer transformer = tfactory.newTransformer(domSource);
        assertNotNull(transformer);
    }

    /**
     * This tests set/get ErrorListener methods of Transformer.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void transformer04() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(new File(TEST_XSL));
        DOMSource domSource = new DOMSource(document);

        Transformer transformer = TransformerFactory.newInstance()
                .newTransformer(domSource);
        transformer.setErrorListener(new MyErrorListener());
        assertNotNull(transformer.getErrorListener());
        assertTrue(transformer.getErrorListener() instanceof MyErrorListener);
    }

    /**
     * This tests getOutputProperties() method of Transformer.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void transformer05() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(new File(TEST_XSL));
        DOMSource domSource = new DOMSource(document);

        Transformer transformer = TransformerFactory.newInstance().
                newTransformer(domSource);
        Properties prop = transformer.getOutputProperties();

        assertEquals(prop.getProperty("indent"), "yes");
        assertEquals(prop.getProperty("method"), "xml");
        assertEquals(prop.getProperty("encoding"), "UTF-8");
        assertEquals(prop.getProperty("standalone"), "no");
        assertEquals(prop.getProperty("version"), "1.0");
        assertEquals(prop.getProperty("omit-xml-declaration"), "no");
    }

    /**
     * This tests getOutputProperty() method of Transformer.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void transformer06() throws Exception {
        TransformerFactory tfactory = TransformerFactory.newInstance();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(new File(TEST_XSL));
        DOMSource domSource = new DOMSource(document);

        Transformer transformer = tfactory.newTransformer(domSource);
        assertEquals(transformer.getOutputProperty("method"), "xml");
    }
}

/**
 * Simple ErrorListener print out all exception.
 */
class MyErrorListener implements ErrorListener {
    /**
     * Prints exception when notification of a recoverable error.
     * @param e exception of a recoverable error.
     */
    @Override
    public void error (TransformerException e) {
        System.out.println(" In error" + e);
    }

    /**
     * Prints exception when notification of a warning.
     * @param e exception of a warning.
     */
    @Override
    public void warning (TransformerException e) {
        System.out.println(" In warning");
    }

    /**
     * Prints exception when notification of a fatal error.
     * @param e exception of a fatal error.
     */
    @Override
    public void fatalError (TransformerException e) throws
                TransformerException {
        System.out.println(" In fatal");
    }
}
