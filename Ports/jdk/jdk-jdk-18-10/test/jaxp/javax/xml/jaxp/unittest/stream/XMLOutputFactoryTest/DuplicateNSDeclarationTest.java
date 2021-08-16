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

package stream.XMLOutputFactoryTest;

import java.io.ByteArrayOutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamResult;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLOutputFactoryTest.DuplicateNSDeclarationTest
 * @run testng/othervm stream.XMLOutputFactoryTest.DuplicateNSDeclarationTest
 * @summary Test the writing of duplicate namespace declarations when IS_REPAIRING_NAMESPACES is ture.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class DuplicateNSDeclarationTest {

    @Test
    public void testDuplicateNSDeclaration() {

        // expect only 1 Namespace Declaration
        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" ?>" + "<ns1:foo" + " xmlns:ns1=\"http://example.com/\">" + "</ns1:foo>";

        // have XMLOutputFactory repair Namespaces
        XMLOutputFactory ofac = XMLOutputFactory.newInstance();
        ofac.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, new Boolean(true));

        // send output to a Stream
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        StreamResult sr = new StreamResult(buffer);
        XMLStreamWriter w = null;

        // write a duplicate Namespace Declaration
        try {
            w = ofac.createXMLStreamWriter(sr);
            w.writeStartDocument();
            w.writeStartElement("ns1", "foo", "http://example.com/");
            w.writeNamespace("ns1", "http://example.com/");
            w.writeNamespace("ns1", "http://example.com/");
            w.writeEndElement();
            w.writeEndDocument();
            w.close();
        } catch (XMLStreamException xmlStreamException) {
            xmlStreamException.printStackTrace();
            Assert.fail(xmlStreamException.toString());
        }

        // debugging output for humans
        System.out.println();
        System.out.println("actual:   \"" + buffer.toString() + "\"");
        System.out.println("expected: \"" + EXPECTED_OUTPUT + "\"");

        // are results as expected?
        Assert.assertEquals(EXPECTED_OUTPUT, buffer.toString());
    }
}
