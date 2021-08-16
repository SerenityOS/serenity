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

package stream.XMLStreamReaderTest;

import java.io.InputStream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6472982
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamReaderTest.Bug6472982Test
 * @run testng/othervm stream.XMLStreamReaderTest.Bug6472982Test
 * @summary Test XMLStreamReader.getNamespaceContext().getPrefix("") won't throw IllegalArgumentException.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6472982Test {
    String namespaceURI = "foobar.com";
    String rootElement = "foo";
    String childElement = "foochild";
    String prefix = "a";

    @Test
    public void testNamespaceContext() {
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
            InputStream is = new java.io.ByteArrayInputStream(getXML().getBytes());
            XMLStreamReader sr = xif.createXMLStreamReader(is);
            NamespaceContext context = sr.getNamespaceContext();
            Assert.assertTrue(context.getPrefix("") == null);

        } catch (IllegalArgumentException iae) {
            Assert.fail("NamespacePrefix#getPrefix() should not throw an IllegalArgumentException for empty uri. ");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    String getXML() {
        StringBuffer sbuffer = new StringBuffer();
        sbuffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sbuffer.append("<" + rootElement + " xmlns:");
        sbuffer.append(prefix);
        sbuffer.append("=\"" + namespaceURI + "\">");
        sbuffer.append("<" + prefix + ":" + childElement + ">");
        sbuffer.append("blahblah");
        sbuffer.append("</" + prefix + ":" + childElement + ">");
        sbuffer.append("</" + rootElement + ">");
        // System.out.println("XML = " + sbuffer.toString()) ;
        return sbuffer.toString();
    }
}
