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

package validation;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/*
 * @test
 * @bug 5072946
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug5072946
 * @run testng/othervm validation.Bug5072946
 * @summary Test Validator.validate(DOMSource,DOMResult) outputs to the result.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug5072946 {

    @Test
    public void test1() throws Exception {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder parser = dbf.newDocumentBuilder();
        Document dom = parser.parse(Bug5072946.class.getResourceAsStream("Bug5072946.xml"));

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema s = sf.newSchema(Bug5072946.class.getResource("Bug5072946.xsd"));
        Validator v = s.newValidator();

        DOMResult r = new DOMResult();
        // r.setNode(dbf.newDocumentBuilder().newDocument());
        v.validate(new DOMSource(dom), r);

        Node node = r.getNode();
        Assert.assertNotNull(node);
        Node fc = node.getFirstChild();
        Assert.assertTrue(fc instanceof Element);
        Element e = (Element) fc;

        Assert.assertEquals("value", e.getAttribute("foo"));
    }

    /**
     * Tests if the identity transformer correctly sets the output node.
     */
    @Test
    public void test2() throws Exception {
        SAXTransformerFactory sf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler th = sf.newTransformerHandler();
        DOMResult r = new DOMResult();
        th.setResult(r);

        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setContentHandler(th);
        reader.parse(new InputSource(Bug5072946.class.getResourceAsStream("Bug5072946.xml")));

        Assert.assertNotNull(r.getNode());
    }

    @Test
    public void test3() throws Exception {
        SAXTransformerFactory sf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        Transformer t = sf.newTransformer();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder parser = dbf.newDocumentBuilder();
        Document dom = parser.parse(Bug5072946.class.getResourceAsStream("Bug5072946.xml"));

        DOMResult r = new DOMResult();

        t.transform(new DOMSource(dom), r);
        Assert.assertNotNull(r.getNode());

        Node n = r.getNode().getFirstChild();
        r.setNode(n);
        t.transform(new DOMSource(dom), r);
        Assert.assertNotNull(r.getNode());
        Assert.assertSame(r.getNode(), n);

        r.setNextSibling(r.getNode().getFirstChild());
        t.transform(new DOMSource(dom), r);
        Assert.assertNotNull(r.getNode());
        Assert.assertSame(r.getNode(), n);
    }
}
