/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package stream.XMLStreamWriterTest;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamWriterTest.AttributeEscapeTest
 * @run testng/othervm stream.XMLStreamWriterTest.AttributeEscapeTest
 * @summary Test XMLStreamWriter shall escape the illegal characters.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class AttributeEscapeTest {

    /**
     * XML content for testing the escaping of <, >, &, ', ".
     */
    private static final String XML_CONTENT = "Testing escaping: lt=<, gt=>, amp=&, apos=', dquote=\"";

    @Test
    public void testCR6420953() {

        try {
            XMLOutputFactory xof = XMLOutputFactory.newInstance();
            StringWriter sw = new StringWriter();
            XMLStreamWriter w = xof.createXMLStreamWriter(sw);

            w.writeStartDocument();
            w.writeStartElement("element");

            w.writeDefaultNamespace(XML_CONTENT);
            w.writeNamespace("prefix", XML_CONTENT);

            w.writeAttribute("attribute", XML_CONTENT);
            w.writeAttribute(XML_CONTENT, "attribute2", XML_CONTENT);
            w.writeAttribute("prefix", XML_CONTENT, "attribute3", XML_CONTENT);

            w.writeCharacters("\n");
            w.writeCharacters(XML_CONTENT);
            w.writeCharacters("\n");
            w.writeCharacters(XML_CONTENT.toCharArray(), 0, XML_CONTENT.length());
            w.writeCharacters("\n");

            w.writeEndElement();
            w.writeEndDocument();
            w.flush();

            System.out.println(sw);

            // make sure that the generated XML parses
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.newDocumentBuilder().parse(new InputSource(new StringReader(sw.toString())));
        } catch (XMLStreamException xmlStreamException) {
            xmlStreamException.printStackTrace();
            Assert.fail(xmlStreamException.toString());
        } catch (SAXException saxException) {
            saxException.printStackTrace();
            Assert.fail(saxException.toString());
        } catch (ParserConfigurationException parserConfigurationException) {
            parserConfigurationException.printStackTrace();
            Assert.fail(parserConfigurationException.toString());
        } catch (IOException ioException) {
            ioException.printStackTrace();
            Assert.fail(ioException.toString());
        }
    }
}
