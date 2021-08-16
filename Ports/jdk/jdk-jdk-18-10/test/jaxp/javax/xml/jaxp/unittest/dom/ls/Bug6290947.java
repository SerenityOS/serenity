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

package dom.ls;

import java.io.StringBufferInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.ls.LSSerializerFilter;
import org.w3c.dom.traversal.NodeFilter;

/*
 * @test
 * @bug 6290947
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.ls.Bug6290947
 * @run testng/othervm dom.ls.Bug6290947
 * @summary Test LSSerializer writes the XML declaration when LSSerializerFilter is set that rejects all nodes and
 * LSSerializer's configuration set parameter "xml-declaration" to "true".
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6290947 {

    private static String XML_STRING = "<?xml version=\"1.0\"?><ROOT><ELEMENT1><CHILD1/><CHILD1><COC1/></CHILD1></ELEMENT1><ELEMENT2>test1<CHILD2/></ELEMENT2></ROOT>";
    private static DOMImplementationLS implLS;
    private final String XML_FILE_INTERNAL_DTD = "note_in_dtd.xml";

    @Test
    public void testStringSourceWithXmlDecl() {
        String result = prepare(XML_STRING, true);
        System.out.println("testStringSource: output: " + result);
        Assert.assertTrue(result.indexOf("<?xml")>-1, "XML Declaration expected in output");
    }

    @Test
    public void testStringSourceWithOutXmlDecl() {
        String result = prepare(XML_STRING, false);
        System.out.println("testStringSource: output: " + result);
        Assert.assertTrue(result.indexOf("<?xml")==-1, "XML Declaration is not expected in output");
    }

    @Test
    public void testXmlWithInternalDTD1() {
        String result = prepare(XML_FILE_INTERNAL_DTD, true);
        System.out.println("testStringSource: output: " + result);
        Assert.assertTrue(result.indexOf("<!DOCTYPE")>0, "XML Declaration and DTD are expected in output");
    }

    @Test
    public void testXmlWithInternalDTD2() {
        String result = prepare(XML_FILE_INTERNAL_DTD, false);
        System.out.println("testStringSource: output: " + result);
        Assert.assertTrue(result.indexOf("<!DOCTYPE")>-1, "DTD is expected in output");
    }

    private String prepare(String source, boolean xmlDeclFlag) {
        Document startDoc = null;
        DocumentBuilder domParser = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            domParser = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }

        final StringBufferInputStream is = new StringBufferInputStream(XML_STRING);
        try {
            startDoc = domParser.parse(is);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }

        DOMImplementation impl = startDoc.getImplementation();
        implLS = (DOMImplementationLS) impl.getFeature("LS", "3.0");
        LSParser parser = implLS.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, "http://www.w3.org/2001/XMLSchema");

        LSInput src = getXmlSource(source);

        LSSerializer writer = implLS.createLSSerializer();

        DOMConfiguration conf = writer.getDomConfig();
        conf.setParameter("xml-declaration", Boolean.valueOf(xmlDeclFlag));

        // set filter
        writer.setFilter(new LSSerializerFilter() {
            public short acceptNode(Node enode) {
                return FILTER_REJECT;

            }

            public int getWhatToShow() {
                return NodeFilter.SHOW_ELEMENT;
            }
        });

        Document doc = parser.parse(src);
        return writer.writeToString(doc);
    }

    private LSInput getXmlSource(String xml1) {
        LSInput src = implLS.createLSInput();
        try {
            if (xml1.endsWith(".xml"))
                src.setByteStream(this.getClass().getResourceAsStream(XML_FILE_INTERNAL_DTD));
            else
                src.setStringData(xml1);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
        return src;
    }
}
