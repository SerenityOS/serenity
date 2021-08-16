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

package test.gaptest;

import static jaxp.library.JAXPTestUtilities.setSystemProperty;
import static jaxp.library.JAXPTestUtilities.clearSystemProperty;

import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/*
 * @test
 * @bug 4515660
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow test.gaptest.Bug4515660
 * @run testng/othervm test.gaptest.Bug4515660
 * @summary verify property org.xml.sax.driver is used by SAXTransformerFactory
 */
@Test(singleThreaded = true)
@Listeners({jaxp.library.BasePolicy.class})
public class Bug4515660 {

    @BeforeClass
    public void setSaxDrier() {
        setSystemProperty("org.xml.sax.driver", ReaderStub.class.getName());
    }

    @AfterClass
    public void clearSaxDrier() {
        clearSystemProperty("org.xml.sax.driver");
    }

    @Test
    public void testTransformer() throws TransformerException {
        String xml = "<?xml version='1.0'?><root/>";
        ReaderStub.used = false;

        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer transformer = transFactory.newTransformer();
        InputSource in = new InputSource(new StringReader(xml));
        SAXSource source = new SAXSource(in);
        StreamResult result = new StreamResult(new StringWriter());

        transformer.transform(source, result);

        assertTrue(ReaderStub.used);

    }

    @Test
    public void testSAXTransformerFactory() throws TransformerConfigurationException {
        final String xsl = "<?xml version='1.0'?>\n" + "<xsl:stylesheet" + " xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" + " version='1.0'>\n"
                + "   <xsl:template match='/'>Hello World!</xsl:template>\n" + "</xsl:stylesheet>\n";

        ReaderStub.used = false;

        TransformerFactory transFactory = TransformerFactory.newInstance();
        assertTrue(transFactory.getFeature(SAXTransformerFactory.FEATURE));

        InputSource in = new InputSource(new StringReader(xsl));
        SAXSource source = new SAXSource(in);

        transFactory.newTransformer(source);
        assertTrue(ReaderStub.used);

    }

    public static class ReaderStub extends XMLFilterImpl {
        static boolean used = false;

        public ReaderStub() throws ParserConfigurationException, SAXException {
            super();
            super.setParent(SAXParserFactory.newInstance().newSAXParser().getXMLReader());
            used = true;
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

}
