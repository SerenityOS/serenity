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

package dom;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/*
 * @test
 * @bug 6521260
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.Bug6521260
 * @run testng/othervm dom.Bug6521260
 * @summary Test setAttributeNS doesn't result in an unsorted internal list of attributes.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6521260 {

    @Test
    public void test() throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        String docStr = "<system systemId='http://www.w3.org/2001/rddl/rddl-xhtml.dtd'" + " uri='/cache/data/xrc36316.bin'"
                + " xmlns:xr='urn:oasis:names:tc:entity:xmlns:xml:catalog'" + " xr:systemId='http://www.w3.org/2001/rddl/rddl-xhtml.dtd'"
                + " xmlns:NS1='http://xmlresolver.org/ns/catalog'" + " NS1:time='1170267571097'/>";

        ByteArrayInputStream bais = new ByteArrayInputStream(docStr.getBytes());

        Document doc = builder.parse(bais);

        Element root = doc.getDocumentElement();

        String systemId = root.getAttribute("systemId");

        // Change the prefix on the "time" attribute so that the list would
        // become unsorted
        // before my fix to
        // xml-xerces/java/src/com/sun/org/apache/xerces/internal/dom/ElementImpl.java
        root.setAttributeNS("http://xmlresolver.org/ns/catalog", "xc:time", "100");

        String systemId2 = root.getAttribute("systemId");

        Assert.assertEquals(systemId, systemId2);
    }
}
