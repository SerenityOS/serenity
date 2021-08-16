/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.org.apache.xerces.internal.dom.AttrImpl;
import com.sun.org.apache.xerces.internal.dom.DocumentImpl;
import com.sun.org.apache.xerces.internal.dom.ElementImpl;
import com.sun.org.apache.xerces.internal.dom.events.MutationEventImpl;
import com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;

/*
 * @test
 * @bug 8213117 8222743
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @modules java.xml
 * @modules java.xml/com.sun.org.apache.xerces.internal.dom
 * @modules java.xml/com.sun.org.apache.xerces.internal.dom.events
 * @modules java.xml/com.sun.org.apache.xerces.internal.jaxp
 * @run testng dom.DocumentTest
 * @summary Tests functionalities for Document.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class DocumentTest {
    static final int DOC1 = 1;
    static final int DOC2 = 2;

    /**
     * Verifies the adoptNode method. Before a node from a deferred DOM can be
     * adopted, it needs to be fully expanded.
     */
    @Test
    public void testAdoptNode() throws Exception {
        String xml1 = "<root><oldNode oldAttrib1=\"old value 1\" oldAttrib2=\"old value 2\"></oldNode></root>";
        String xml2 = "<root><newNode newAttrib=\"new value\"></newNode></root>";

        Document doc1 = getDocument(xml1);
        Document doc2 = getDocument(xml2);

        Element newNode = (Element) doc2.getFirstChild().getFirstChild();
        Element replacementNode = (Element) doc1.adoptNode(newNode);

        Node oldNode = doc1.getFirstChild().getFirstChild();
        doc1.getDocumentElement().replaceChild(replacementNode, oldNode);

        String attrValue = doc1.getFirstChild().getFirstChild().getAttributes()
                .getNamedItem("newAttrib").getNodeValue();
        Assert.assertEquals(attrValue, "new value");
    }

    /**
     * Verifies that the lookupNamespaceURI method returns null (not empty string)
     * for unbound prefix.
     *
     * Specification for lookupNamespaceURI:
     * Returns the associated namespace URI or null if none is found.
     *
     * @throws Exception
     */
    @Test
    public void testUnboundNamespaceURI() throws Exception {
        String xml = "<?xml version='1.1'?>"
                + "<root><e1 xmlns='' xmlns:p1='' xmlns:p2='uri2'><e2/></e1></root>";

        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = getDocument(xml);
        Element e1 = doc.getDocumentElement();
        Element e2 = (Element)e1.getFirstChild().getFirstChild();

        Assert.assertEquals(e1.lookupNamespaceURI(null), null);
        Assert.assertEquals(e2.lookupNamespaceURI(null), null);

        Assert.assertEquals(e1.lookupNamespaceURI("p1"), null);
        Assert.assertEquals(e2.lookupNamespaceURI("p1"), null);

        Assert.assertEquals(e1.lookupNamespaceURI("p2"), null);
        Assert.assertEquals(e2.lookupNamespaceURI("p2"), "uri2");
    }

    /**
     * Verifies that calling namespace methods on an empty document won't result
     * in a NPE.
     * @throws Exception
     */
    @Test
    public void testNamespaceNPE() throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        document.lookupNamespaceURI("prefix");
        document.lookupPrefix("uri");
        document.isDefaultNamespace("uri");
    }

    /**
     * Verifies that manipulating an independent document from within a mutation
     * listener does not modify the original event object.
     */
    @Test
    public void testMutation() throws Exception {
        String xml1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<root><a a_attr=\"a_attr_value\"/></root>";
        String xml2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<root><b b_attr=\"b_attr_value\"/></root>";
        DocumentBuilder db = DocumentBuilderFactoryImpl.newInstance().newDocumentBuilder();
        DocumentImpl doc1 = (DocumentImpl)getDocument(xml1);
        DocumentImpl doc2 = (DocumentImpl)getDocument(xml2);
        ElementImpl a = (ElementImpl) doc1.getDocumentElement().getFirstChild();
        AttrImpl attr = (AttrImpl) a.getAttributeNode("a_attr");
        attr.addEventListener(MutationEventImpl.DOM_NODE_REMOVED, new MyEventListener(DOC1, doc2), false);
        doc2.addEventListener(MutationEventImpl.DOM_ATTR_MODIFIED, new MyEventListener(DOC2), true);

        // make a change to doc1 to trigger the event
        Element a1 = (Element) doc1.getDocumentElement().getFirstChild();
        a1.setAttribute("a_attr", "a_attr_newvalue");
    }

    private static Document getDocument(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputStream a = new ByteArrayInputStream(xml.getBytes());
        Document out = db.parse(a);
        return out;
    }

    // EventListener that mutates an unrelated document when an event is received.
    static class MyEventListener implements EventListener {

        private int docId;
        private Document doc = null;

        public MyEventListener(int docId) {
            this.docId = docId;
        }

        public MyEventListener(int docId, Document doc) {
            this.docId = docId;
            this.doc = doc;
        }

        public void handleEvent(Event evt) {
            if (docId == DOC1) {
                //check the related node before making changes
                checkRelatedNode(evt, "a_attr_value");
                //make a change to doc2
                Element ele = (Element)doc.getDocumentElement().getFirstChild();
                ele.setAttribute("b_attr", "value for b_attr in doc2");
                //check the related node again after the change
                checkRelatedNode(evt, "a_attr_value");
            } else { //DOC2
                checkRelatedNode(evt, "value for b_attr in doc2");
            }
        }

    }

    // Utility method to display an event
    public static void checkRelatedNode(Event evt, String expected) {
        //System.out.println(" Event: " + evt + ", on " + evt.getTarget());
        if (evt instanceof MutationEventImpl) {
            MutationEventImpl mutation = (MutationEventImpl) evt;
            //System.out.println(" + Related: " + mutation.getRelatedNode());
            Assert.assertTrue(mutation.getRelatedNode().toString().contains(expected));
        }
    }
}
