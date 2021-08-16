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

import java.io.ByteArrayOutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamWriterTest.EncodingTest
 * @run testng/othervm stream.XMLStreamWriterTest.EncodingTest
 * @summary Test XMLStreamWriter writes a document with encoding setting.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class EncodingTest {

    private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newInstance();

    /*
     * Tests writing a document with UTF-8 encoding, by setting UTF-8 on writer.
     */
    @Test
    public void testWriteStartDocumentUTF8() {

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root></root>";
        XMLStreamWriter writer = null;
        ByteArrayOutputStream byteArrayOutputStream = null;

        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            writer = XML_OUTPUT_FACTORY.createXMLStreamWriter(byteArrayOutputStream, "UTF-8");

            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("root");
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();

            String actualOutput = byteArrayOutputStream.toString();
            Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.toString());
        }

    }

    /*
     * Tests writing a document with UTF-8 encoding on default enocding writer.
     * This scenario should result in an exception as default encoding is ASCII.
     */
    @Test
    public void testWriteStartDocumentUTF8Fail() {

        XMLStreamWriter writer = null;
        ByteArrayOutputStream byteArrayOutputStream = null;

        // pick a different encoding to use v. default encoding
        String defaultCharset = java.nio.charset.Charset.defaultCharset().name();
        String useCharset = "UTF-8";
        if (useCharset.equals(defaultCharset)) {
            useCharset = "US-ASCII";
        }

        System.out.println("defaultCharset = " + defaultCharset + ", useCharset = " + useCharset);

        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            writer = XML_OUTPUT_FACTORY.createXMLStreamWriter(byteArrayOutputStream);

            writer.writeStartDocument(useCharset, "1.0");
            writer.writeStartElement("root");
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();

            Assert.fail("Expected XMLStreamException as default underlying stream encoding of " + defaultCharset
                    + " differs from explicitly specified encoding of " + useCharset);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
