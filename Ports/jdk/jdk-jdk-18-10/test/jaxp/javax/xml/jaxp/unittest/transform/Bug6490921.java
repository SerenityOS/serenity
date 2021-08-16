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

package transform;

import static jaxp.library.JAXPTestUtilities.setSystemProperty;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/*
 * @test
 * @bug 6490921
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.Bug6490921
 * @run testng/othervm transform.Bug6490921
 * @summary Test property org.xml.sax.driver is always applied in transformer API.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6490921 {

    public static class ReaderStub extends XMLFilterImpl {
        static boolean used = false;

        public ReaderStub() throws ParserConfigurationException, SAXException {
            super();
            super.setParent(SAXParserFactory.newInstance().newSAXParser().getXMLReader());
        }

        public void parse(InputSource input) throws SAXException, IOException {
            used = true;
            super.parse(input);
        }

        public void parse(String systemId) throws SAXException, IOException {
            used = true;
            super.parse(systemId);
        }
    }

    @Test
    public void test01() {
        String xml = "<?xml version='1.0'?><root/>";
        ReaderStub.used = false;
        setSystemProperty("org.xml.sax.driver", "");

        // Don't set 'org.xml.sax.driver' here, just use default
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();
            InputSource in = new InputSource(new StringReader(xml));
            SAXSource source = new SAXSource(in);
            StreamResult result = new StreamResult(new StringWriter());
            transformer.transform(source, result);
            Assert.assertTrue(!printWasReaderStubCreated());
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void test02() {
        String xml = "<?xml version='1.0'?><root/>";
        ReaderStub.used = false;
        setSystemProperty("org.xml.sax.driver", ReaderStub.class.getName());
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();
            InputSource in = new InputSource(new StringReader(xml));
            SAXSource source = new SAXSource(in);
            StreamResult result = new StreamResult(new StringWriter());
            transformer.transform(source, result);
            Assert.assertTrue(printWasReaderStubCreated());
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void test03() {
        String xsl = "<?xml version='1.0'?>\n" + "<xsl:stylesheet" + " xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" + " version='1.0'>\n"
                + "   <xsl:template match='/'>Hello World!</xsl:template>\n" + "</xsl:stylesheet>\n";

        ReaderStub.used = false;
        setSystemProperty("org.xml.sax.driver", ReaderStub.class.getName());
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();
            if (transFactory.getFeature(SAXTransformerFactory.FEATURE) == false) {
                System.out.println("SAXTransformerFactory not supported");
            }
            InputSource in = new InputSource(new StringReader(xsl));
            SAXSource source = new SAXSource(in);

            transFactory.newTransformer(source);
            Assert.assertTrue(printWasReaderStubCreated());
        } catch (TransformerException e) {
            Assert.fail(e.getMessage());
        }
    }

    private static boolean printWasReaderStubCreated() {
        if (ReaderStub.used) {
            System.out.println("\tReaderStub is used.");
            return ReaderStub.used;
        } else {
            System.out.println("\tReaderStub is not used.");
            return ReaderStub.used;
        }
    }
}
