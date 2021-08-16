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

import static jaxp.library.JAXPTestUtilities.getSystemProperty;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jaxp.library.JAXPTestUtilities;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/*
 * @test
 * @bug 6582545
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.Bug6582545Test
 * @run testng/othervm dom.Bug6582545Test
 * @summary Test the value is correct when iterating attributes.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6582545Test {
    private DocumentBuilder xmlParser = null;
    private Document document = null;
    private String FWS1 = "FWS1";
    private String KEY_ARROW_UP = "KEY_ARROW_UP";
    private String VALUE_ARROW_UP = "root%LRM%Tmp_CPIOM-C1%VLIN_For_ECP%ECP_IN_Port_1%IOM-A7_Msg_cd30%FDS_1_ECP_to_FWS-1%A31_ECP_ARROW_UP";

    @Test
    public void testAttributeCaching() {

        File xmlFile = new File(getClass().getResource("Bug6582545.xml").getFile());

        try {
            DocumentBuilderFactory aDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
            xmlParser = aDocumentBuilderFactory.newDocumentBuilder();

            // works fine with JDK 1.4.2, 1.5
            // does not work with JDK 1.6
            document = xmlParser.parse(xmlFile);
            printNode(FWS1);
        } catch (SAXException saxException) {
            saxException.printStackTrace();
        } catch (ParserConfigurationException parserConfigurationException) {
            parserConfigurationException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (IllegalArgumentException illegalArgumentException) {
            illegalArgumentException.printStackTrace();
        }
    }

    private void printNode(String aNode) {
        boolean error = true;
        NodeList nodeList;
        NamedNodeMap attributes;

        nodeList = document.getElementsByTagName(aNode);
        attributes = nodeList.item(0).getAttributes();

        String name;
        String value;
        // Print all nodes
        for (int k = 0; k < attributes.getLength(); k++) {
            name = attributes.item(k).getNodeName();
            value = attributes.item(k).getNodeValue();
            System.out.println(name + "=" + value);
        }

        // Test specifique a node
        String javaSpecificationVersion = getSystemProperty("java.specification.version");
        for (int k = 0; k < attributes.getLength(); k++) {
            name = attributes.item(k).getNodeName();
            value = attributes.item(k).getNodeValue();
            if (KEY_ARROW_UP.equals(name)) {
                if (VALUE_ARROW_UP.equals(value)) {
                    // Parser OK
                    System.out.println("Parser in Java " + javaSpecificationVersion + " returned correct value.");
                    error = false;
                } else {
                    // Parser NOK
                    System.out.println("Parser in Java " + javaSpecificationVersion + " returned wrong value");
                }
                System.out.println("for node         = " + KEY_ARROW_UP);
                System.out.println("expecting value  =" + VALUE_ARROW_UP);
                System.out.println("value from parser=" + value);
            }
        }

        Assert.assertTrue(!error);
    }

}
