/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/*
 * @test
 * @bug 8135283 8138721
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.ElementTraversal
 * @run testng/othervm dom.ElementTraversal
 * @summary Tests for the Element Traversal interface.
 */

@Listeners({jaxp.library.FilePolicy.class})
public class ElementTraversal {
    /*
       Verifies that ElementTraversal is supported.
     */
    @Test(dataProvider = "doc")
    public void testHasFeature(Document doc) {
        DOMImplementation di = doc.getImplementation();

        //return false if feasure == null
        Assert.assertFalse(di.hasFeature(null, null));

        //A feature is supported without specifying version
        Assert.assertTrue(di.hasFeature("ElementTraversal", null));
        Assert.assertTrue(di.hasFeature("ElementTraversal", ""));

        //ElementTraversal Version 1.0 is supported
        Assert.assertTrue(di.hasFeature("ElementTraversal", "1.0"));
    }

    /*
       Verifies the ElementTraversal interface by exercising all of its five
       methods while reading through the xml document.
     */
    @Test(dataProvider = "doc")
    public void test(Document doc) {
        org.w3c.dom.ElementTraversal et = (org.w3c.dom.ElementTraversal)doc.getDocumentElement();
        //4 toys are listed
        Assert.assertEquals(et.getChildElementCount(), 4);

        //The 1st is the Martian
        Element toy1 = et.getFirstElementChild();
        verify(toy1, "1", "The Martian");

        //toy1 has no previous element
        Element noE = ((org.w3c.dom.ElementTraversal)toy1).getPreviousElementSibling();
        Assert.assertEquals(noE, null);

        //The 1st toy's next element is toy2, the Doll
        Element toy2 = ((org.w3c.dom.ElementTraversal)toy1).getNextElementSibling();
        verify(toy2, "2", "The Doll");

        //The last toy is toy4, the Spaceship
        Element toy4 = et.getLastElementChild();
        verify(toy4, "4", "The Spaceship");

        //toy4 has no next element
        noE = ((org.w3c.dom.ElementTraversal)toy4).getNextElementSibling();
        Assert.assertEquals(noE, null);

        //toy4's previous element is toy3, Transformer X
        //toy3 is also an EntityReference
        Element toy3 = ((org.w3c.dom.ElementTraversal)toy4).getPreviousElementSibling();
        verify(toy3, "3", "Transformer X");
    }

    /**
     * Verifies that the values matches the specified element.
     * @param id the value of the id attribute
     * @param name the value of its name element
     */
    void verify(Element e, String id, String name) {
        Assert.assertEquals(e.getAttribute("id"), id);
        Element toyName = ((org.w3c.dom.ElementTraversal)e).getFirstElementChild();
        Assert.assertEquals(toyName.getTextContent(), name);
    }


    /*
     * DataProvider: a Document object
     */
    @DataProvider(name = "doc")
    public Object[][] getXPath() {
        return new Object[][]{{getDoc()}};
    }
    Document getDoc() {
        InputStream xmlFile = getClass().getResourceAsStream("ElementTraversal.xml");
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(xmlFile);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.out.println("fail: " + e.getMessage());
        }

        return doc;
    }
}
