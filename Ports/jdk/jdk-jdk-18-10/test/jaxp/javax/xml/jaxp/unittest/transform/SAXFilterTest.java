/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package transform;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/*
 * @test
 * @bug 8237456
 * @modules java.xml
 * @run testng transform.SAXFilterTest
 * @summary Verifies that reference entities are not written out when the element
 * is skipped through a filter.
 */
public class SAXFilterTest {

    static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<thing1 xmlns=\"things\">\n"
            + "    <name>This &amp; That</name>\n"
            + "    <thing2>\n"
            + "        <name>The Other</name>\n"
            + "    </thing2>\n"
            + "    <thing2>\n"
            + "        <name>Whatever</name>\n"
            + "    </thing2>\n"
            + "</thing1>";

    static final String UNEXPECTED = "&amp;";

    @Test
    public void test() throws Exception {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        XMLFilter filter = new Thing2Filter(getParser());
        StringWriter sw = new StringWriter();
        t.transform(new SAXSource(filter, new InputSource(new StringReader(XML))),
                new StreamResult(sw));
        Assert.assertFalse(sw.toString().contains(UNEXPECTED));
        System.out.println(sw.toString());
    }

    private XMLReader getParser() {
        XMLReader reader = null;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();

            String saxParserFactoryClassName = factory.getClass().getName();
            System.out.println("SAXParserFactory class: " + saxParserFactoryClassName);

            factory.setValidating(false);
            factory.setNamespaceAware(true);

            SAXParser parser = factory.newSAXParser();
            reader = parser.getXMLReader();
            System.out.println("XmlReader class: " + reader.getClass().getName());
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (SAXException se) {
            se.printStackTrace();
        }
        return reader;
    }

    class Thing2Filter extends XMLFilterImpl {

        private boolean inMatch = false;
        private int elementLocator;
        private boolean doneMatching = false;

        public Thing2Filter(XMLReader parent) {
            super(parent);
        }

        @Override
        public void startDocument() throws SAXException {
            doneMatching = false;
            super.startDocument();
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes attrs) throws SAXException {
            if (localName.equals("thing2") && !doneMatching) { // start matching when the first thing2 is hit
                inMatch = true;
            }

            if (inMatch) {
                super.startElement(namespaceURI, localName, qName, attrs);
            }
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (inMatch) {
                super.endElement(namespaceURI, localName, qName);
            }
            if (localName.equals("thing2")) { // match is over once end of first thing2 is hit
                inMatch = false;
                doneMatching = true;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (inMatch) {
                super.characters(ch, start, length);
            }
        }

        @Override
        public void startPrefixMapping(java.lang.String prefix, java.lang.String uri) throws SAXException {
            super.startPrefixMapping(prefix, uri);
        }

        @Override
        public void endPrefixMapping(java.lang.String prefix) throws SAXException {
            super.endPrefixMapping(prefix);
        }
    }
}
