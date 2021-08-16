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

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6394074
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamWriterTest.UnprefixedNameTest
 * @run testng/othervm stream.XMLStreamWriterTest.UnprefixedNameTest
 * @summary Test XMLStreamWriter namespace prefix with writeDefaultNamespace.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class UnprefixedNameTest {

    @Test
    public void testUnboundPrefix() throws Exception {

        try {
            XMLOutputFactory xof = XMLOutputFactory.newInstance();
            XMLStreamWriter w = xof.createXMLStreamWriter(System.out);
            // here I'm trying to write
            // <bar xmlns="foo" />
            w.writeStartDocument();
            w.writeStartElement("foo", "bar");
            w.writeDefaultNamespace("foo");
            w.writeCharacters("---");
            w.writeEndElement();
            w.writeEndDocument();
            w.close();

            // Unexpected success
            String FAIL_MSG = "Unexpected success.  Expected: " + "XMLStreamException - " + "if the namespace URI has not been bound to a prefix "
                    + "and javax.xml.stream.isPrefixDefaulting has not been " + "set to true";
            System.err.println(FAIL_MSG);
            Assert.fail(FAIL_MSG);
        } catch (XMLStreamException xmlStreamException) {
            // Expected Exception
            System.out.println("Expected XMLStreamException: " + xmlStreamException.toString());
        }
    }

    @Test
    public void testBoundPrefix() throws Exception {

        try {
            XMLOutputFactory xof = XMLOutputFactory.newInstance();
            XMLStreamWriter w = xof.createXMLStreamWriter(System.out);
            // here I'm trying to write
            // <bar xmlns="foo" />
            w.writeStartDocument();
            w.writeStartElement("foo", "bar", "http://namespace");
            w.writeCharacters("---");
            w.writeEndElement();
            w.writeEndDocument();
            w.close();

            // Expected success
            System.out.println("Expected success.");
        } catch (Exception exception) {
            // Unexpected Exception
            String FAIL_MSG = "Unexpected Exception: " + exception.toString();
            System.err.println(FAIL_MSG);
            Assert.fail(FAIL_MSG);
        }
    }

    @Test
    public void testRepairingPrefix() throws Exception {

        try {

            // repair namespaces
            // use new XMLOutputFactory as changing its property settings
            XMLOutputFactory xof = XMLOutputFactory.newInstance();
            xof.setProperty(xof.IS_REPAIRING_NAMESPACES, new Boolean(true));
            XMLStreamWriter w = xof.createXMLStreamWriter(System.out);

            // here I'm trying to write
            // <bar xmlns="foo" />
            w.writeStartDocument();
            w.writeStartElement("foo", "bar");
            w.writeDefaultNamespace("foo");
            w.writeCharacters("---");
            w.writeEndElement();
            w.writeEndDocument();
            w.close();

            // Expected success
            System.out.println("Expected success.");
        } catch (Exception exception) {
            // Unexpected Exception
            String FAIL_MSG = "Unexpected Exception: " + exception.toString();
            System.err.println(FAIL_MSG);
            Assert.fail(FAIL_MSG);
        }
    }
}
