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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;

/**
 * Class contains the test cases for SAXParser API
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.parsers.ptests.SAXParserTest02
 * @run testng/othervm javax.xml.parsers.ptests.SAXParserTest02
 */
@Listeners({jaxp.library.BasePolicy.class})
public class SAXParserTest02 {
    private static final String DOM_NODE = "http://xml.org/sax/properties/dom-node";
    private static final String XML_STRING = "http://xml.org/sax/properties/xml-string";
    private static final String DECL_HANDLER = "http://xml.org/sax/properties/declaration-handler";
    private static final String LEXICAL_HANDLER = "http://xml.org/sax/properties/lexical-handler";

    /**
     * Provide SAXParser.
     *
     * @return a data provider contains a SAXParser instance.
     * @throws Exception If any errors occur.
     */
    @DataProvider(name = "parser-provider")
    public Object[][] getParser() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser saxparser = spf.newSAXParser();
        return new Object[][] { { saxparser } };
    }

    /**
     * Test to test the default functionality (No validation) of the parser.
     *
     * @param saxparser a SAXParser instance.
     */
    @Test(dataProvider = "parser-provider")
    public void testValidate01(SAXParser saxparser) {
        assertFalse(saxparser.isValidating());
    }

    /**
     * Test to test the functionality of setValidating and isValidating
     * methods.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testValidate02() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setValidating(true);
        spf.newSAXParser();
        assertTrue(spf.isValidating());
    }

    /**
     * Test isNamespaceAware() method. By default, namespaces are
     * not supported.
     *
     * @param saxparser a SAXParser instance.
     */
    @Test(dataProvider = "parser-provider")
    public void testNamespace01(SAXParser saxparser) {
        assertFalse(saxparser.isNamespaceAware());
    }

    /**
     * Test case to test setnamespaceAware() method.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testNamespace02() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxparser = spf.newSAXParser();
        assertTrue(saxparser.isNamespaceAware());
    }

    /**
     * Test case to test if the getParser() method returns instance of Parser.
     *
     * @param saxparser a SAXParser instance.
     * @throws SAXException If any parse errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testParser01(SAXParser saxparser) throws SAXException {
        assertNotNull(saxparser.getParser());
    }

    /**
     * Test case to test if the getXMLReader() method returns instance of
     * XMLReader.
     *
     * @param saxparser a SAXParser instance.
     * @throws SAXException If any parse errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testXmlReader01(SAXParser saxparser) throws SAXException {
        assertNotNull(saxparser.getXMLReader());
    }

    /**
     * Test whether the xml-string property is not supported.
     *
     * @param saxparser a SAXParser instance.
     * @throws SAXException If any parse errors occur.
     */
    @Test(expectedExceptions = SAXNotSupportedException.class,
            dataProvider = "parser-provider")
    public void testProperty01(SAXParser saxparser) throws SAXException {
        saxparser.getProperty(XML_STRING);
    }

    /**
     * Test whether the dom-node property is not supported.
     *
     * @param saxparser a SAXParser instance.
     * @throws SAXException If any parse errors occur.
     */
    @Test(expectedExceptions = SAXNotSupportedException.class,
            dataProvider = "parser-provider")
    public void testProperty02(SAXParser saxparser) throws SAXException {
        saxparser.getProperty(DOM_NODE);
    }

    /**
     * Test the default lexical-handler not exists.
     *
     * @param saxparser a SAXParser instance.
     * @throws SAXException If any parse errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testProperty03(SAXParser saxparser) throws SAXException {
        assertNull(saxparser.getProperty(LEXICAL_HANDLER));
    }

    /**
     * Test the default declaration-handler not exists.
     *
     * @param saxparser a SAXParser instance.
     * @throws SAXException If any parse errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testProperty04(SAXParser saxparser) throws SAXException {
        assertNull(saxparser.getProperty(DECL_HANDLER));
    }

    /**
     * Test to set and get the lexical-handler.
     *
     * @param saxparser a SAXParser instance.
     * @throws SAXException If any parse errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testProperty05(SAXParser saxparser) throws SAXException {
        MyLexicalHandler myLexicalHandler = new MyLexicalHandler();
        saxparser.setProperty(LEXICAL_HANDLER, myLexicalHandler);
        assertTrue(saxparser.getProperty(LEXICAL_HANDLER) instanceof LexicalHandler);
    }

    /**
     * Test to set and get the declaration-handler.
     *
     * @param saxparser a SAXParser instance.
     * @throws SAXException If any parse errors occur.
     */
    @Test(dataProvider = "parser-provider")
    public void testProperty06(SAXParser saxparser) throws SAXException {
        MyDeclHandler myDeclHandler = new MyDeclHandler();
        saxparser.setProperty(DECL_HANDLER, myDeclHandler);
        assertTrue(saxparser.getProperty(DECL_HANDLER) instanceof DeclHandler);
    }

    /**
     * Customized LexicalHandler used for test. An empty implementation for
     * LexicalHandler.
     */
    private class MyLexicalHandler implements LexicalHandler {

        @Override
        public void comment(char[] ch, int start, int length) {
        }

        @Override
        public void endCDATA() {
        }

        @Override
        public void endDTD() {
        }

        @Override
        public void endEntity(String name) {
        }

        @Override
        public void startCDATA() {
        }

        @Override
        public void startDTD(String name, String publicId, String systemId) {
        }

        @Override
        public void startEntity(String name) {
        }
    }

    /**
     * Customized DeclHandler used for test. An empty implementation for
     * DeclHandler.
     */
    private class MyDeclHandler implements DeclHandler {

        @Override
        public void attributeDecl(String eName, String aName, String type, String valueDefault, String value) {
        }

        @Override
        public void elementDecl(String name, String model) {
        }

        @Override
        public void externalEntityDecl(String name, String publicId, String systemId) {
        }

        @Override
        public void internalEntityDecl(String name, String value) {
        }
    }
}
