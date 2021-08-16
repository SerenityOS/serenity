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

package parsers;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 6594813
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.Bug6594813
 * @run testng/othervm parsers.Bug6594813
 * @summary Test SAXParser output is wellformed with name space.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6594813 {

    public Bug6594813(String name) {
    }

    private static final String TESTXML = "<?xml version='1.0' ?>\n"
            + "<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/' xmlns:ns1='http://faulttestservice.org/wsdl'>\n"
            + "<soapenv:Body>\n" + "<soapenv:Fault xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/'>\n" + "<faultcode>\n"
            + "soapenv:Server</faultcode>\n" + "<faultstring>\n" + "com.sun.ts.tests.jaxws.sharedwebservices.faultservice.DummyException</faultstring>\n"
            + "<detail>\n" + "<ns1:DummyException>\n" + "<dummyField1>\n" + "dummyString1</dummyField1>\n" + "<dummyField2>\n" + "dummyString2</dummyField2>\n"
            + "</ns1:DummyException>\n" + "</detail>\n" + "</soapenv:Fault>\n" + "</soapenv:Body>\n" + "</soapenv:Envelope>\n";

    // simplest XML to re-declare same prefix/namespace mappings
    private static final String SIMPLE_TESTXML = "<?xml version='1.0' ?>\n" + "<prefix:ElementName xmlns:prefix='URI'>\n"
            + "<prefix:ElementName xmlns:prefix='URI'>\n" + "</prefix:ElementName>\n" + "</prefix:ElementName>\n";

    private String runTransform(SAXParser sp) throws Exception {
        // Run identity transform using SAX parser
        SAXSource src = new SAXSource(sp.getXMLReader(), new InputSource(new StringReader(TESTXML)));
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter sw = new StringWriter();
        transformer.transform(src, new StreamResult(sw));

        String result = sw.getBuffer().toString();
        // System.out.println(result);
        return result;
    }

    private void checkWellFormedness(String xml) throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true); // Same as default
        spf.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
        SAXParser sp = spf.newSAXParser();

        // Re-parse output to make sure that it is well formed
        sp.parse(new InputSource(new StringReader(xml)), new DefaultHandler());
    }

    /**
     * Test an identity transform of an XML document with NS decls using a
     * non-ns-aware parser. Output result to a StreamSource. Set ns-awareness to
     * FALSE and prefixes to FALSE.
     */
    @Test
    public void testXMLNoNsAwareStreamResult1() {
        try {
            // Create SAX parser *without* enabling ns
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(false); // Same as default
            spf.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
            SAXParser sp = spf.newSAXParser();

            // Make sure that the output is well formed
            String xml = runTransform(sp);
            checkWellFormedness(xml);
        } catch (Throwable ex) {
            Assert.fail(ex.toString());
        }
    }

    /**
     * Test an identity transform of an XML document with NS decls using a
     * non-ns-aware parser. Output result to a StreamSource. Set ns-awareness to
     * FALSE and prefixes to TRUE.
     */
    @Test
    public void testXMLNoNsAwareStreamResult2() {
        try {
            // Create SAX parser *without* enabling ns
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(false); // Same as default
            spf.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
            SAXParser sp = spf.newSAXParser();

            // Make sure that the output is well formed
            String xml = runTransform(sp);
            checkWellFormedness(xml);
        } catch (Throwable ex) {
            Assert.fail(ex.toString());
        }
    }

    /**
     * Test an identity transform of an XML document with NS decls using a
     * non-ns-aware parser. Output result to a StreamSource. Set ns-awareness to
     * TRUE and prefixes to FALSE.
     */
    @Test
    public void testXMLNoNsAwareStreamResult3() {
        try {
            // Create SAX parser *without* enabling ns
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true); // Same as default
            spf.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
            SAXParser sp = spf.newSAXParser();

            // Make sure that the output is well formed
            String xml = runTransform(sp);
            checkWellFormedness(xml);
        } catch (Throwable ex) {
            Assert.fail(ex.toString());
        }
    }

    /**
     * Test an identity transform of an XML document with NS decls using a
     * non-ns-aware parser. Output result to a StreamSource. Set ns-awareness to
     * TRUE and prefixes to TRUE.
     */
    @Test
    public void testXMLNoNsAwareStreamResult4() {
        try {
            // Create SAX parser *without* enabling ns
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true); // Same as default
            spf.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
            SAXParser sp = spf.newSAXParser();

            // Make sure that the output is well formed
            String xml = runTransform(sp);
            checkWellFormedness(xml);
        } catch (Throwable ex) {
            Assert.fail(ex.toString());
        }
    }

}
