/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import static jaxp.library.JAXPTestUtilities.setSystemProperty;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8152530 8202426 7148925
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @modules java.xml
 * @modules java.xml/com.sun.org.apache.xerces.internal.impl
 * @modules java.xml/com.sun.org.apache.xerces.internal.xni.parser
 * @modules java.xml/com.sun.xml.internal.stream
 * @clean MyXMLInputFactoryImpl MyXMLStreamReader
 * @build MyXMLInputFactoryImpl MyXMLStreamReader
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.StAXSourceTest
 * @run testng/othervm transform.StAXSourceTest
 * @summary Test parsing from StAXSource.
 */
@Listeners({jaxp.library.JAXPTestPolicy.class})
public class StAXSourceTest {
    @DataProvider(name = "xml")
    public Object[][] getData() throws Exception {
        // from 6715417, all other data were from 7148925
        String xmlDT = "<?xml version=\"1.0\" encoding=\"utf-8\"?> "
                + "<!DOCTYPE bookstore [ "
                + "<!ELEMENT bookstore (book)*> "
                + "<!ELEMENT book (title,author,price)> "
                + "<!ATTLIST book genre CDATA #REQUIRED> "
                + "<!ELEMENT title (#PCDATA)> "
                + "<!ELEMENT author (#PCDATA)> "
                + "<!ELEMENT price (#PCDATA)> ]> "
                + "<bookstore> "
                + "<book genre=\"fantasy\" > "
                + "<title>Oberon's Legacy</title> "
                + "<author>Corets, Eva</author> "
                + "<price>5.95</price> "
                + "</book> "
                + "</bookstore>";
        return new Object[][]{
            {"<root/>"},
            {"<!DOCTYPE root [<!ENTITY et 'Come Home'>]><root et='&et;'/>"},
            {"<?xml-stylesheet href='show.xsl' type='text/html'?><root/>"},
            {"<?xml version='1.0'?><?xml-stylesheet href='show.xsl' type='text/html'?><root/>"},
            {"<?xml version='1.0'?><?xml-stylesheet href='show.xsl' type='text/html'?>"
                + "<!DOCTYPE root><root/>"},
            {"<?xml version='1.0'?><!DOCTYPE root [<!ELEMENT greeting (#PCDATA)>]><root/>"},
            {"<?xml version='1.0'?><?xml-stylesheet href='show.xsl' type='text/html'?>"
                + "<!DOCTYPE root [<!ELEMENT greeting (#PCDATA)>]><root/>"},
            {xmlDT},
        };
    }

    /**
     * @bug 7148925 6715417
     *
     * Verifies that the transformation is successful with a StreamSource.
     *
     * @param xml the xml
     * @throws Exception if the test fails
     */
    @Test(dataProvider = "xml")
    public void parseStreamSource(String xml) throws Exception {
        Source source = new StreamSource(new StringReader(xml));
        transform(source, xml);
    }

    /**
     * @bug 7148925 6715417
     *
     * Verifies that the transformation is successful with a StAXSource created
     * out of a StreamReader.
     *
     * Note that the patch fixes the Exception, but does not include any improvement
     * over the current. The result may differ from that of StreamSource.
     *
     * @param xml the xml
     * @throws Exception if the test fails
     */
    @Test(dataProvider = "xml")
    public void parseSSSR(String xml) throws Exception {
        XMLInputFactory xif = XMLInputFactory.newDefaultFactory();
        XMLStreamReader sr = xif.createXMLStreamReader(new StringReader(xml));
        StAXSource source = new StAXSource(sr);
        transform(source, xml);
    }

    /**
     * @bug 7148925 6715417
     *
     * Verifies that the transformation is successful with a StAXSource created
     * out of an EventReader.
     *
     * Note that the patch fixes the Exception, but does not include any improvement
     * over the current. The result may differ from that of StreamSource.
     *
     * @param xml the xml
     * @throws Exception if the test fails
     */
    @Test(dataProvider = "xml")
    public void parseSSER(String xml) throws Exception {
        XMLInputFactory xif = XMLInputFactory.newDefaultFactory();
        XMLEventReader er = xif.createXMLEventReader(new StringReader(xml));
        StAXSource source = new StAXSource(er);
        transform(source, xml);
    }

    private void transform(Source source, String sourceXml) throws Exception{
        StringWriter sw = new StringWriter();
        Result result = new StreamResult(sw);
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.newTransformer().transform(source, result);
        System.out.printf("%n%s:%nSource: %s%nResult: %s%n", source.getClass().getSimpleName(), sourceXml, sw);
    }

    /**
     * @bug 8202426
     * Verifies that a null Attribute type is handled. NPE was thrown before the fix.
     */
    @Test
    public final void testAttributeTypeNull() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
            "<t:test xmlns:t=\"http://www.example.org/Test\" attr=\"value\" /> ";
        setSystemProperty("javax.xml.stream.XMLInputFactory", "transform.MyXMLInputFactoryImpl");
        XMLInputFactory xif = XMLInputFactory.newInstance();
        XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(xml));
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();

        while (xsr.hasNext()) {
            xsr.next();
            if (xsr.getEventType() == XMLStreamConstants.START_ELEMENT) {
                t.reset();
                DOMResult result = new DOMResult();
                t.transform(new StAXSource(xsr), result);
            }
        }
    }

    /**
     * @bug 8152530
     * Verifies that StAXSource handles empty namespace properly. NPE was thrown
     * before the fix.
     * @throws Exception if the test fails
     */
    @Test
    public final void testStAXSourceWEmptyNS() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<EntityList>\n"
            + "  <Entity xmlns=\"\">\n"
            + "  </Entity>\n"
            + "  <Entity xmlns=\"\">\n"
            + "  </Entity>\n"
            + "</EntityList> ";

        XMLInputFactory xif = XMLInputFactory.newInstance();
        XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(xml));
        xsr.nextTag();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        while (xsr.nextTag() == XMLStreamConstants.START_ELEMENT && xsr.getLocalName().equals("Entity")) {
            StringWriter stringResult = new StringWriter();
            t.transform(new StAXSource(xsr), new StreamResult(stringResult));
            System.out.println("result: \n" + stringResult.toString());
        }
    }

    @Test
    public final void testStAXSource() throws XMLStreamException {
        XMLInputFactory ifactory = XMLInputFactory.newInstance();
        XMLOutputFactory ofactory = XMLOutputFactory.newInstance();

        String xslStylesheet = "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>"
                + "  <xsl:output method='xml' encoding='utf-8' indent='no'/>" + "  <xsl:preserve-space elements='*'/>" + "  <xsl:template match='*'>"
                + "    <xsl:copy><xsl:copy-of select='@*'/><xsl:apply-templates/></xsl:copy>" + "  </xsl:template>"
                + "  <xsl:template match='comment()|processing-instruction()|text()'>" + "    <xsl:copy/>" + "  </xsl:template>" + "</xsl:stylesheet>";
        StringReader xslStringReader = new StringReader(xslStylesheet);
        StringReader xmlStringReader = new StringReader(xslStylesheet); // identity
                                                                        // on
                                                                        // itself,
        StringWriter xmlStringWriter = new StringWriter();

        XMLEventReader styleReader = ifactory.createXMLEventReader(xslStringReader);
        XMLEventReader docReader = ifactory.createXMLEventReader(xmlStringReader);
        XMLEventWriter writer = ofactory.createXMLEventWriter(xmlStringWriter);

        StAXSource stylesheet = new StAXSource(styleReader);
        StAXSource document = new StAXSource(docReader);
        StAXResult result = new StAXResult(writer);

        try {
            document.setSystemId("sourceSystemId");
        } catch (UnsupportedOperationException e) {
            System.out.println("Expected UnsupportedOperationException in StAXSource.setSystemId()");
        } catch (Exception e) {
            Assert.fail("StAXSource.setSystemId() does not throw java.lang.UnsupportedOperationException");
        }

        TransformerFactory tfactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = tfactory.newTransformer(stylesheet);
            transformer.transform(document, result);
        } catch (TransformerConfigurationException tce) {
            throw new XMLStreamException(tce);
        } catch (TransformerException te) {
            throw new XMLStreamException(te);
        } finally {
            styleReader.close();
            docReader.close();
            writer.close();
        }

        try {
            result.setSystemId("systemId");
        } catch (UnsupportedOperationException e) {
            System.out.println("Expected UnsupportedOperationException in StAXResult.setSystemId()");
        } catch (Exception e) {
            Assert.fail("StAXResult.setSystemId() does not throw java.lang.UnsupportedOperationException");
        }

        if (result.getSystemId() != null) {
            Assert.fail("StAXResult.getSystemId() does not return null");
        }
    }

    @Test
    public final void testStAXSource2() throws XMLStreamException {
        XMLInputFactory ifactory = XMLInputFactory.newInstance();
        ifactory.setProperty("javax.xml.stream.supportDTD", Boolean.TRUE);

        StAXSource ss = new StAXSource(ifactory.createXMLStreamReader(getClass().getResource("5368141.xml").toString(),
                getClass().getResourceAsStream("5368141.xml")));
        DOMResult dr = new DOMResult();

        TransformerFactory tfactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = tfactory.newTransformer();
            transformer.transform(ss, dr);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
