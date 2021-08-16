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

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamResult;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 7037352
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamWriterTest.Bug7037352Test
 * @run testng/othervm stream.XMLStreamWriterTest.Bug7037352Test
 * @summary Test XMLStreamWriter.getNamespaceContext().getPrefix with XML_NS_URI and XMLNS_ATTRIBUTE_NS_URI.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug7037352Test {

    @Test
    public void test() {
        try {
            XMLOutputFactory xof = XMLOutputFactory.newInstance();
            StreamResult sr = new StreamResult();
            XMLStreamWriter xsw = xof.createXMLStreamWriter(sr);
            NamespaceContext nc = xsw.getNamespaceContext();
            System.out.println(nc.getPrefix(XMLConstants.XML_NS_URI));
            System.out.println("  expected result: " + XMLConstants.XML_NS_PREFIX);
            System.out.println(nc.getPrefix(XMLConstants.XMLNS_ATTRIBUTE_NS_URI));
            System.out.println("  expected result: " + XMLConstants.XMLNS_ATTRIBUTE);

            Assert.assertTrue(nc.getPrefix(XMLConstants.XML_NS_URI) == XMLConstants.XML_NS_PREFIX);
            Assert.assertTrue(nc.getPrefix(XMLConstants.XMLNS_ATTRIBUTE_NS_URI) == XMLConstants.XMLNS_ATTRIBUTE);

        } catch (Throwable ex) {
            Assert.fail(ex.toString());
        }
    }

}
