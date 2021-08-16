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
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 6809409
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow sax.IssueTracker56Test
 * @run testng/othervm sax.IssueTracker56Test
 * @summary Test SAXException has Cause.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class IssueTracker56Test {

    @Test
    public void testException() {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser parser = spf.newSAXParser();
            String xmlToParse = "<root>Issue 56: SAXException does not do the exception chaining properly</root>";
            InputSource source = new InputSource(new StringReader(xmlToParse));
            parser.parse(source, new MyHandler());
        } catch (SAXException ex) {
            System.out.println(ex.getCause());
            if (ex.getCause() == null)
                Assert.fail("failed chaining exception properly.");
            // ex.printStackTrace(); //will not print out root cause without the
            // fix
        } catch (IOException ex) {
            // shouldn't happen
        } catch (ParserConfigurationException ex) {
            // shouldn't happen
        }
    }

    @Test
    public void testWorkAround() throws Exception {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser parser = spf.newSAXParser();
            String xmlToParse = "<root>Issue 56: SAXException does not do the exception chaining properly</root>";
            InputSource source = new InputSource(new StringReader(xmlToParse));
            parser.parse(source, new MyHandler1());
        } catch (SAXException ex) {
            System.out.println(ex.getCause());
            // ex.printStackTrace(); //will print out root cause
        } catch (IOException ex) {
            // shouldn't happen
        } catch (ParserConfigurationException ex) {
            // shouldn't happen
        }

    }

    public class MyHandler extends DefaultHandler implements ErrorHandler {

        public void startDocument() throws SAXException {
        }

        public void endDocument() throws SAXException {
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            try {
                System.out.println(uri);
                System.out.println(uri.charAt(56));
            } catch (Exception e) {
                throw new SAXException(e);
            }

        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
        }

        public void characters(char ch[], int start, int length) throws SAXException {
        }

    }

    public class MyHandler1 extends DefaultHandler implements ErrorHandler {

        public void startDocument() throws SAXException {
        }

        public void endDocument() throws SAXException {
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXExceptionExt {
            try {
                System.out.println(uri);
                System.out.println(uri.charAt(56));
            } catch (Exception e) {
                throw new SAXExceptionExt(e);
            }

        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
        }

        public void characters(char ch[], int start, int length) throws SAXException {
        }

    }
}
