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
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamReaderTest.NamespaceTest
 * @run testng/othervm stream.XMLStreamReaderTest.NamespaceTest
 * @summary Test StAX parser processes namespace.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class NamespaceTest {

    String namespaceURI = "foobar.com";
    String rootElement = "foo";
    String childElement = "foochild";
    String prefix = "a";

    // Add test methods here, they have to start with 'test' name.
    // for example:
    // public void testHello() {}

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

    @Test
    public void testRootElementNamespace() {
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
            InputStream is = new java.io.ByteArrayInputStream(getXML().getBytes());
            XMLStreamReader sr = xif.createXMLStreamReader(is);
            while (sr.hasNext()) {
                int eventType = sr.next();
                if (eventType == XMLStreamConstants.START_ELEMENT) {
                    if (sr.getLocalName().equals(rootElement)) {
                        Assert.assertTrue(sr.getNamespacePrefix(0).equals(prefix) && sr.getNamespaceURI(0).equals(namespaceURI));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void testChildElementNamespace() {
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
            InputStream is = new java.io.ByteArrayInputStream(getXML().getBytes());
            XMLStreamReader sr = xif.createXMLStreamReader(is);
            while (sr.hasNext()) {
                int eventType = sr.next();
                if (eventType == XMLStreamConstants.START_ELEMENT) {
                    if (sr.getLocalName().equals(childElement)) {
                        QName qname = sr.getName();
                        Assert.assertTrue(qname.getPrefix().equals(prefix) && qname.getNamespaceURI().equals(namespaceURI)
                                && qname.getLocalPart().equals(childElement));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void testNamespaceContext() {
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
            InputStream is = new java.io.ByteArrayInputStream(getXML().getBytes());
            XMLStreamReader sr = xif.createXMLStreamReader(is);
            while (sr.hasNext()) {
                int eventType = sr.next();
                if (eventType == XMLStreamConstants.START_ELEMENT) {
                    if (sr.getLocalName().equals(childElement)) {
                        NamespaceContext context = sr.getNamespaceContext();
                        Assert.assertTrue(context.getPrefix(namespaceURI).equals(prefix));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void testNamespaceCount() {
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
            InputStream is = new java.io.ByteArrayInputStream(getXML().getBytes());
            XMLStreamReader sr = xif.createXMLStreamReader(is);
            while (sr.hasNext()) {
                int eventType = sr.next();
                if (eventType == XMLStreamConstants.START_ELEMENT) {
                    if (sr.getLocalName().equals(rootElement)) {
                        int count = sr.getNamespaceCount();
                        Assert.assertTrue(count == 1);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
