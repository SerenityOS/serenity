/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.parsers.ptests;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.parsers.ptests.ParserTestConst.GOLDEN_DIR;
import static javax.xml.parsers.ptests.ParserTestConst.XML_DIR;
import static jaxp.library.JAXPTestUtilities.USER_DIR;
import static jaxp.library.JAXPTestUtilities.compareWithGold;
import static jaxp.library.JAXPTestUtilities.filenameToURL;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertNotSame;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;

import jaxp.library.JAXPDataProvider;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @bug 8080907 8169778
 * This checks the methods of DocumentBuilderFactoryImpl.
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.parsers.ptests.DocumentBuilderFactoryTest
 * @run testng/othervm javax.xml.parsers.ptests.DocumentBuilderFactoryTest
 */
@Listeners({jaxp.library.FilePolicy.class})
public class DocumentBuilderFactoryTest {

    /**
     * DocumentBuilderFactory builtin system-default implementation class name.
     */
    private static final String DEFAULT_IMPL_CLASS =
        "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl";

    /**
     * DocumentBuilderFactory implementation class name.
     */
    private static final String DOCUMENT_BUILDER_FACTORY_CLASSNAME = DEFAULT_IMPL_CLASS;

    /**
     * Provide valid DocumentBuilderFactory instantiation parameters.
     *
     * @return a data provider contains DocumentBuilderFactory instantiation parameters.
     */
    @DataProvider(name = "parameters")
    public Object[][] getValidateParameters() {
        return new Object[][] { { DOCUMENT_BUILDER_FACTORY_CLASSNAME, null }, { DOCUMENT_BUILDER_FACTORY_CLASSNAME, this.getClass().getClassLoader() } };
    }

    /**
     * Test if newDefaultInstance() method returns an instance
     * of the expected factory.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testDefaultInstance() throws Exception {
        DocumentBuilderFactory dbf1 = DocumentBuilderFactory.newDefaultInstance();
        DocumentBuilderFactory dbf2 = DocumentBuilderFactory.newInstance();
        assertNotSame(dbf1, dbf2, "same instance returned:");
        assertSame(dbf1.getClass(), dbf2.getClass(),
                  "unexpected class mismatch for newDefaultInstance():");
        assertEquals(dbf1.getClass().getName(), DEFAULT_IMPL_CLASS);
    }

    /**
     * Test for DocumentBuilderFactory.newInstance(java.lang.String
     * factoryClassName, java.lang.ClassLoader classLoader) factoryClassName
     * points to correct implementation of
     * javax.xml.parsers.DocumentBuilderFactory , should return newInstance of
     * DocumentBuilderFactory
     *
     * @param factoryClassName
     * @param classLoader
     * @throws ParserConfigurationException
     */
    @Test(dataProvider = "parameters")
    public void testNewInstance(String factoryClassName, ClassLoader classLoader) throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(factoryClassName, classLoader);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        assertNotNull(builder);
    }

    /**
     * test for DocumentBuilderFactory.newInstance(java.lang.String
     * factoryClassName, java.lang.ClassLoader classLoader) factoryClassName is
     * null , should throw FactoryConfigurationError
     *
     * @param factoryClassName
     * @param classLoader
     */
    @Test(expectedExceptions = FactoryConfigurationError.class, dataProvider = "new-instance-neg", dataProviderClass = JAXPDataProvider.class)
    public void testNewInstanceNeg(String factoryClassName, ClassLoader classLoader) {
        DocumentBuilderFactory.newInstance(factoryClassName, classLoader);
    }

    /**
     * Test the default functionality of schema support method.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckSchemaSupport1() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(true);
        dbf.setNamespaceAware(true);
        dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                W3C_XML_SCHEMA_NS_URI);
        MyErrorHandler eh = MyErrorHandler.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setErrorHandler(eh);
        db.parse(new File(XML_DIR, "test.xml"));
        assertFalse(eh.isErrorOccured());
    }

    @DataProvider(name = "schema-source")
    public Object[][] getSchemaSource() throws FileNotFoundException {
        return new Object[][] {
                { new FileInputStream(new File(XML_DIR, "test.xsd")) },
                { new InputSource(filenameToURL(XML_DIR + "test.xsd")) } };
    }

    /**
     * Test the default functionality of schema support method. In
     * this case the schema source property is set.
     * @throws Exception If any errors occur.
     */
    @Test(dataProvider = "schema-source")
    public void testCheckSchemaSupport2(Object schemaSource) throws Exception {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(true);
            dbf.setNamespaceAware(true);
            dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                    W3C_XML_SCHEMA_NS_URI);
            dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", schemaSource);
            MyErrorHandler eh = MyErrorHandler.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(eh);
            db.parse(new File(XML_DIR, "test1.xml"));
            assertFalse(eh.isErrorOccured());
        } finally {
            if (schemaSource instanceof Closeable) {
                ((Closeable) schemaSource).close();
            }
        }

    }

    /**
     * Test the default functionality of schema support method. In
     * this case the schema source property is set.
     * @throws Exception If any errors occur.
     */
    @Test(dataProvider = "schema-source")
    public void testCheckSchemaSupport3(Object schemaSource) throws Exception {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setValidating(true);
            spf.setNamespaceAware(true);
            SAXParser sp = spf.newSAXParser();
            sp.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                    W3C_XML_SCHEMA_NS_URI);
            sp.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", schemaSource);
            DefaultHandler dh = new DefaultHandler();
            // Not expect any unrecoverable error here.
            sp.parse(new File(XML_DIR, "test1.xml"), dh);
        } finally {
            if (schemaSource instanceof Closeable) {
                ((Closeable) schemaSource).close();
            }
        }
    }

    /**
     * Test the default functionality of newInstance method. To test
     * the isCoalescing method and setCoalescing This checks to see if the CDATA
     * and text nodes got combined In that case it will print "&lt;xml&gt;This
     * is not parsed&lt;/xml&gt; yet".
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckDocumentBuilderFactory02() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setCoalescing(true);
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        Document doc = docBuilder.parse(new File(XML_DIR, "DocumentBuilderFactory01.xml"));
        Element e = (Element) doc.getElementsByTagName("html").item(0);
        NodeList nl = e.getChildNodes();
        assertEquals(nl.getLength(), 1);
    }

    /**
     * Test the isIgnoringComments. By default it is false.
     */
    @Test
    public void testCheckDocumentBuilderFactory03() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        assertFalse(dbf.isIgnoringComments());
    }

    /**
     * Test the isValidating. By default it is false, set it to true and then
     * use a document which is not valid. It should throw a warning or
     * an error at least. The test passes in case retval 0 is set in the error
     * method .
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckDocumentBuilderFactory04() throws Exception {
        MyErrorHandler eh = MyErrorHandler.newInstance();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setErrorHandler(eh);
        db.parse(new File(XML_DIR, "DocumentBuilderFactory05.xml"));
        assertTrue(eh.isErrorOccured());
    }

    /**
     * Test the setValidating. By default it is false, use a
     * document which is not valid. It should not throw a warning or an error.
     * The test passes in case the return value equals 1.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckDocumentBuilderFactory16() throws Exception {
        MyErrorHandler eh = MyErrorHandler.newInstance();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setErrorHandler(eh);
        db.parse(new File(XML_DIR, "DocumentBuilderFactory05.xml"));
        assertFalse(eh.isErrorOccured());
    }

    /**
     * Test the setValidating. By default it is false, use a
     * document which is valid. It should not throw a warning or an error. The
     * test passes in case the return value equals 1.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckDocumentBuilderFactory17() throws Exception {
        MyErrorHandler eh = MyErrorHandler.newInstance();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setErrorHandler(eh);
        db.parse(new File(XML_DIR, "DocumentBuilderFactory04.xml"));
        assertFalse(eh.isErrorOccured());
    }

    /**
     * Test the isExpandEntityReferences. By default it is true.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckDocumentBuilderFactory05() throws Exception {
        try(FileInputStream fis = new FileInputStream(new File(
                XML_DIR, "DocumentBuilderFactory02.xml"))) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(fis);
            Element e = (Element) doc.getElementsByTagName("title").item(0);
            NodeList nl = e.getChildNodes();
            assertTrue(dbf.isExpandEntityReferences());
            assertEquals(nl.item(0).getNodeValue().trim().charAt(0), 'W');
        }
    }

    /**
     * Test the default functionality of setValidating method. The
     * XML file has a DTD which has namespaces defined. The parser takes care to
     * check if the namespaces using elements and defined attributes are there
     * or not.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckDocumentBuilderFactory06() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        MyErrorHandler eh = MyErrorHandler.newInstance();
        db.setErrorHandler(eh);
        Document doc = db.parse(new File(XML_DIR, "DocumentBuilderFactory04.xml"));
        assertTrue(doc instanceof Document);
        assertFalse(eh.isErrorOccured());
    }

    /**
     * Test the setExpandEntityReferences.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckDocumentBuilderFactory07() throws Exception {
        try (FileInputStream fis = new FileInputStream(new File(
                XML_DIR, "DocumentBuilderFactory02.xml"))) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setExpandEntityReferences(true);
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(fis);
            Element e = (Element) doc.getElementsByTagName("title").item(0);
            NodeList nl = e.getChildNodes();
            assertTrue(dbf.isExpandEntityReferences());
            assertEquals(nl.item(0).getNodeValue().trim().charAt(0), 'W');
        }
    }

    /**
     * Test the setExpandEntityReferences.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckDocumentBuilderFactory08() throws Exception {
        try (FileInputStream fis = new FileInputStream(new File(
                XML_DIR, "DocumentBuilderFactory02.xml"))) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setExpandEntityReferences(false);
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(fis);
            Element e = (Element) doc.getElementsByTagName("title").item(0);
            NodeList nl = e.getChildNodes();
            assertNull(nl.item(0).getNodeValue());
        }
    }

    /**
     * Test the setIgnoringComments. By default it is set to false.
     * explicitly setting it to false, it recognizes the comment which is in
     * Element Node Hence the Element's child node is not null.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckDocumentBuilderFactory09() throws Exception {
        try (FileInputStream fis = new FileInputStream(new File(
                XML_DIR, "DocumentBuilderFactory07.xml"))) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringComments(false);
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(fis);
            Element e = (Element) doc.getElementsByTagName("body").item(0);
            NodeList nl = e.getChildNodes();
            assertNotNull(nl.item(0).getNodeValue());
        }
    }

    /**
     * This tests for the parse(InputSource).
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckDocumentBuilderFactory10() throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(new File(
                XML_DIR, "DocumentBuilderFactory07.xml")))) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(new InputSource(br));
            assertNotNull(doc);
        }
    }

    /**
     * This tests for the parse InputStream with SystemID as a second parameter.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckDocumentBuilderFactory11() throws Exception {
        try (FileInputStream fis = new FileInputStream(new File(
                XML_DIR, "dbf10import.xsl"))) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(fis, new File(XML_DIR).toURI()
                    .toASCIIString());
            assertNotNull(doc);
        }
    }

    /**
     * This tests for the parse InputStream with empty SystemID as a second
     * parameter.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckDocumentBuilderFactory12() throws Exception {
        try (FileInputStream fis = new FileInputStream(new File(
                XML_DIR, "dbf10import.xsl"))) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(fis, " ");
            assertNotNull(doc);
        }
    }

    /**
     * This tests for the parse(uri).
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckDocumentBuilderFactory13() throws Exception {
        // Accesing default working directory.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        Document doc = docBuilder.parse(new File(XML_DIR + "dbf10import.xsl")
                .toURI().toASCIIString());
        assertNotNull(doc);
    }

    /**
     * This tests for the parse(uri) with empty string as parameter should
     * throw Sax Exception.
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = SAXException.class)
    public void testCheckDocumentBuilderFactory14() throws Exception {
        // Accesing default working directory.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        docBuilder.parse("");
    }

    /**
     * This tests for the parse (uri) with null uri as parameter should throw
     * IllegalArgumentException.
     * @throws Exception If any errors occur.
     *
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCheckDocumentBuilderFactory15() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        String uri = null;
        docBuilder.parse(uri);
    }

    /**
     * Test the setIgnoringComments. By default it is set to false,
     * setting this to true, It does not recognize the comment, Here the
     * nodelist has a length 0 because the ignoring comments is true.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckIgnoringComments() throws Exception {
        try (FileInputStream fis = new FileInputStream(new File(
                XML_DIR, "DocumentBuilderFactory08.xml"))) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringComments(true);
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(fis);
            Element e = (Element) doc.getElementsByTagName("body").item(0);
            NodeList nl = e.getChildNodes();
            assertEquals(nl.getLength(), 0);
        }
    }

    /**
     * Test the default behaviour of setIgnoringComments. By default
     * it is set to false, this is similar to case 9 but not setIgnoringComments
     * explicitly, it does not recognize the comment.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckIgnoringComments1() throws Exception {
        try (FileInputStream fis = new FileInputStream(new File(
                XML_DIR, "DocumentBuilderFactory07.xml"))) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(fis);
            Element e = (Element) doc.getElementsByTagName("body").item(0);
            NodeList nl = e.getChildNodes();
            assertFalse(dbf.isIgnoringComments());
            assertNotNull(nl.item(0).getNodeValue());
        }
    }

    /**
     * Test for the isIgnoringElementContentWhitespace and the
     * setIgnoringElementContentWhitespace. The xml file has all kinds of
     * whitespace,tab and newline characters, it uses the MyNSContentHandler
     * which does not invoke the characters callback when this
     * setIgnoringElementContentWhitespace is set to true.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckElementContentWhitespace() throws Exception {
        String goldFile = GOLDEN_DIR + "dbfactory02GF.out";
        String outputFile = USER_DIR + "dbfactory02.out";
        MyErrorHandler eh = MyErrorHandler.newInstance();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(true);
        assertFalse(dbf.isIgnoringElementContentWhitespace());
        dbf.setIgnoringElementContentWhitespace(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setErrorHandler(eh);
        Document doc = db.parse(new File(XML_DIR, "DocumentBuilderFactory06.xml"));
        assertFalse(eh.isErrorOccured());
        DOMSource domSource = new DOMSource(doc);
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer transformer = tfactory.newTransformer();
        SAXResult saxResult = new SAXResult();
        try(MyCHandler handler = MyCHandler.newInstance(new File(outputFile))) {
            saxResult.setHandler(handler);
            transformer.transform(domSource, saxResult);
        }
        assertTrue(compareWithGold(goldFile, outputFile));
    }
}
