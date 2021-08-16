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
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamWriterTest.EmptyElementTest
 * @run testng/othervm stream.XMLStreamWriterTest.EmptyElementTest
 * @summary Test XMLStreamWriter writes namespace and attribute after writeEmptyElement.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class EmptyElementTest {

    // expected output
    private static final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<hello xmlns=\"http://hello\">"
            + "<world xmlns=\"http://world\" prefixes=\"foo bar\"/>" + "</hello>";

    XMLStreamWriter xmlStreamWriter;
    ByteArrayOutputStream byteArrayOutputStream;
    XMLOutputFactory xmlOutputFactory;

    @Test
    public void testWriterOnLinux() throws Exception {

        // setup XMLStreamWriter
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            xmlOutputFactory = XMLOutputFactory.newInstance();
            xmlOutputFactory.setProperty(xmlOutputFactory.IS_REPAIRING_NAMESPACES, new Boolean(true));
            xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(byteArrayOutputStream);
        } catch (Exception e) {
            System.err.println("Unexpected Exception: " + e.toString());
            e.printStackTrace();
            Assert.fail(e.toString());
        }

        // create & write a document
        try {
            xmlStreamWriter.writeStartDocument();
            xmlStreamWriter.writeStartElement("hello");
            xmlStreamWriter.writeDefaultNamespace("http://hello");
            xmlStreamWriter.writeEmptyElement("world");
            xmlStreamWriter.writeDefaultNamespace("http://world");
            xmlStreamWriter.writeAttribute("prefixes", "foo bar");
            xmlStreamWriter.writeEndElement();
            xmlStreamWriter.writeEndDocument();
            xmlStreamWriter.flush();
            String actualOutput = byteArrayOutputStream.toString();
            Assert.assertEquals(EXPECTED_OUTPUT, actualOutput);
        } catch (Exception e) {
            System.err.println("Unexpected Exception: " + e.toString());
            e.printStackTrace();
            Assert.fail(e.toString());
        }
    }
}
