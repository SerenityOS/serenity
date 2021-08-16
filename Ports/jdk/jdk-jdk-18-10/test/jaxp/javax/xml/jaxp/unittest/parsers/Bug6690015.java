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

package parsers;

import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/*
 * @test
 * @bug 6518733
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.Bug6690015
 * @run testng/othervm parsers.Bug6690015
 * @summary Test SAX parser handles several attributes with newlines.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6690015 {

    public Bug6690015() {
    }

    @Test
    public void test() {
        try {
            FileInputStream fis = new FileInputStream(getClass().getResource("bug6690015.xml").getFile());

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(fis));
            Element root = doc.getDocumentElement();
            NodeList textnodes = root.getElementsByTagName("text");
            int len = textnodes.getLength();
            int index = 0;
            int attindex = 0;
            int attrlen = 0;
            NamedNodeMap attrs = null;

            while (index < len) {
                Element te = (Element) textnodes.item(index);
                attrs = te.getAttributes();
                attrlen = attrs.getLength();
                attindex = 0;
                Node node = null;

                while (attindex < attrlen) {
                    node = attrs.item(attindex);
                    System.out.println("attr: " + node.getNodeName() + " is shown holding value: " + node.getNodeValue());
                    attindex++;
                }
                index++;
                System.out.println("-------------");
            }
            fis.close();
        } catch (Exception e) {
            Assert.fail("Exception: " + e.getMessage());
        }
    }

}
