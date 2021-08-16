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

package sax;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.ParserAdapter;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow sax.DefaultHandler2Test
 * @run testng/othervm sax.DefaultHandler2Test
 * @summary Test DefaultHandler2.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class DefaultHandler2Test {

    @Test
    public void testParse01() {
        System.out.println("===in testParse01===");
        try {
            DefaultHandler handler = new MyDefaultHandler2();
            SAXParserFactory saxFac = SAXParserFactory.newInstance();
            System.out.println(saxFac.getFeature("http://xml.org/sax/features/use-locator2"));

            // set use-entity-resolver2 as FALSE to use EntityResolver firstly.
            saxFac.setFeature("http://xml.org/sax/features/use-entity-resolver2", false);
            saxFac.setValidating(true);

            SAXParser parser = saxFac.newSAXParser();
            parser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            parser.setProperty("http://xml.org/sax/properties/declaration-handler", handler);

            parser.parse(this.getClass().getResource("toys.xml").getFile(), handler);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            Assert.fail("ParserConfigurationException in testParse01()");
        } catch (SAXException e) {
            e.printStackTrace();
            Assert.fail("SAXException in testParse01()");
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("IOException in testParse01()");
        }
    }

    @Test
    public void testParse02() {
        System.out.println("===in testParse02===");
        try {
            DefaultHandler handler = new MyDefaultHandler2();
            SAXParserFactory saxFac = SAXParserFactory.newInstance();
            System.out.println(saxFac.getFeature("http://xml.org/sax/features/use-locator2"));

            // Enable namespace parsing
            System.out.println(saxFac.getFeature("http://xml.org/sax/features/namespaces"));
            saxFac.setNamespaceAware(true);

            saxFac.setValidating(true);
            SAXParser parser = saxFac.newSAXParser();
            parser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            parser.setProperty("http://xml.org/sax/properties/declaration-handler", handler);

            parser.parse(this.getClass().getResource("toys.xml").getFile(), handler);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            Assert.fail("ParserConfigurationException in testParse02()");
        } catch (SAXException e) {
            e.printStackTrace();
            Assert.fail("SAXException in testParse02()");
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("IOException in testParse02()");
        }
    }

    @Test
    public void testParse03() {
        System.out.println("===in testParse03===");
        try {
            DefaultHandler handler = new MyDefaultHandler2();

            XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            xmlReader.setProperty("http://xml.org/sax/properties/declaration-handler", handler);
            System.out.println("XMLReader : " + xmlReader.getProperty("http://xml.org/sax/properties/declaration-handler"));

            SAXParserFactory saxFac = SAXParserFactory.newInstance();
            SAXParser parser = saxFac.newSAXParser();
            parser.setProperty("http://xml.org/sax/properties/declaration-handler", handler);
            System.out.println("SAXParser : " + parser.getProperty("http://xml.org/sax/properties/declaration-handler"));

            // From https://docs.oracle.com/javase/7/docs/api,
            // ParserAdapter.setProperty() and ParserAdapter.getProperty() does
            // not support any property currently.
            try {
                ParserAdapter adapter = new ParserAdapter(parser.getParser());
                System.out.println("ParserAdapter : " + adapter.getProperty("http://xml.org/sax/properties/declaration-handler"));
            } catch (SAXNotRecognizedException e) {
                System.out.println("Expected  SAXNotRecognizedException since ParserAdapter.getProperty() does not support any property currently");
            }
            try {
                ParserAdapter adapter = new ParserAdapter(parser.getParser());
                adapter.setProperty("http://xml.org/sax/properties/declaration-handler", handler);
            } catch (SAXNotRecognizedException e) {
                System.out.println("Expected  SAXNotRecognizedException since ParserAdapter.setProperty() does not support any property currently");
            }
        } catch (SAXException e) {
            e.printStackTrace();
            Assert.fail("SAXException in testParse03()");
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            Assert.fail("ParserConfigurationException in testParse03()");
        }

    }

    @Test
    public void testParse04() {
        System.out.println("===in testParse04===");
        try {
            DefaultHandler handler = new MyDefaultHandler2();
            XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            System.out.println(xmlReader.getFeature("http://xml.org/sax/features/namespaces"));
            xmlReader.setProperty("http://xml.org/sax/properties/declaration-handler", handler);
            xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            xmlReader.setContentHandler(handler);

            xmlReader.parse(this.getClass().getResource("toys.xml").getFile());

        } catch (SAXException e) {
            e.printStackTrace();
            Assert.fail("SAXException in testParse04()");
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("IOException in testParse04()");
        }
    }

    @Test
    public void testParse05() {
        System.out.println("===in testParse05===");
        try {
            DefaultHandler handler = new MyDefaultHandler2();
            XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            XMLFilterImpl filterImpl = new XMLFilterImpl(xmlReader);
            System.out.println(xmlReader.getFeature("http://xml.org/sax/features/namespaces"));
            filterImpl.setProperty("http://xml.org/sax/properties/declaration-handler", handler);
            filterImpl.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            filterImpl.setContentHandler(handler);

            filterImpl.parse(this.getClass().getResource("toys.xml").getFile());

        } catch (SAXException e) {
            e.printStackTrace();
            Assert.fail("SAXException in testParse05()");
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("IOException in testParse05()");
        }
    }

    @Test
    public void testParse06() {
        System.out.println("===in testParse06===");
        try {
            DefaultHandler handler = new MyDefaultHandler2();
            XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            XMLFilterImpl filterImpl = new XMLFilterImpl(xmlReader);
            System.out.println(xmlReader.getFeature("http://xml.org/sax/features/namespaces"));
            filterImpl.setProperty("http://xml.org/sax/properties/declaration-handler", handler);
            filterImpl.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            filterImpl.setContentHandler(handler);

            AssertJUnit.assertTrue(filterImpl.getProperty("http://xml.org/sax/properties/declaration-handler") instanceof DefaultHandler2);

            // filterImpl.setFeature("http://xml.org/sax/features/external-general-entities",
            // false) ;
            // filterImpl.setFeature("http://xml.org/sax/features/external-parameter-entities",
            // false) ;
            filterImpl.skippedEntity("name2");

            filterImpl.parse(this.getClass().getResource("toys.xml").getFile());
        } catch (SAXException e) {
            e.printStackTrace();
            Assert.fail("SAXException in testParse06()");
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("IOException in testParse06()");
        }
    }

    @Test
    public void testParse07() {
        System.out.println("===in testParse07===");
        try {
            DefaultHandler handler = new MyDefaultHandler2();
            XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            XMLFilterImpl filterImpl = new XMLFilterImpl(xmlReader);
            System.out.println(xmlReader.getFeature("http://xml.org/sax/features/namespaces"));
            filterImpl.setProperty("http://xml.org/sax/properties/declaration-handler", handler);
            filterImpl.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            filterImpl.setContentHandler(handler);
            filterImpl.setErrorHandler(handler);
            AssertJUnit.assertTrue(filterImpl.getProperty("http://xml.org/sax/properties/declaration-handler") instanceof DefaultHandler2);

            filterImpl.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
            filterImpl.parse(this.getClass().getResource("toys_error.xml").getFile());
        } catch (SAXException e) {
            e.printStackTrace();
            Assert.fail("SAXException in testParse07()");
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("IOException in testParse07()");
        }
    }
}
