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

import java.io.IOException;
import java.io.StringBufferInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSParserFilter;
import org.w3c.dom.traversal.NodeFilter;
import org.xml.sax.SAXException;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.ls.LSParserTCKTest
 * @run testng/othervm dom.ls.LSParserTCKTest
 * @summary Test Specifications and Descriptions for LSParser.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class LSParserTCKTest {

    DOMImplementationLS implLS = null;
    public String xml1 = "<?xml version=\"1.0\"?><ROOT><ELEMENT1><CHILD1/><CHILD1><COC1/></CHILD1></ELEMENT1><ELEMENT2>test1<CHILD2/></ELEMENT2></ROOT>";

    /**
     * Equivalence class partitioning
     * with state, input and output values orientation
     * for public Document parse(LSInput is),
     * <br><b>pre-conditions</b>: set filter that REJECTs any CHILD* node,
     * <br><b>is</b>: xml1
     * <br><b>output</b>: XML document with ELEMNENT1 and ELEMENT2 only.
     */
    @Test
    public void testfilter0001() {
        LSParser parser = createLSParser();
        if (parser == null) {
            Assert.fail("Unable to create LSParser!");
        }
        // set filter
        parser.setFilter(new LSParserFilter() {
            public short startElement(Element elt) {
                return FILTER_ACCEPT;
            }

            public short acceptNode(Node enode) {
                if (enode.getNodeName().startsWith("CHILD")) {
                    return FILTER_REJECT;
                }
                return FILTER_ACCEPT;
            }

            public int getWhatToShow() {
                return NodeFilter.SHOW_ALL;
            }
        });
        String expected = "<?xml version=\"1.0\"?><ROOT><ELEMENT1></ELEMENT1><ELEMENT2>test1</ELEMENT2></ROOT>";
        Document doc = parser.parse(getXmlSource(xml1));
        if (!match(expected, doc)) {
            Assert.fail("DOM structure after parsing is not equal to a structure of XML document, that being parsed");
        }

        System.out.println("OKAY");
    }

    public LSParserTCKTest(String name) {
        init();
    }

    protected void init() {
        Document doc = null;
        DocumentBuilder parser = null;
        try {
            parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        StringBufferInputStream is = new StringBufferInputStream(xml1);
        try {
            doc = parser.parse(is);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        DOMImplementation impl = doc.getImplementation();
        implLS = (DOMImplementationLS) impl.getFeature("LS", "3.0");
    }

    public LSInput getXmlSource(String xmldoc) {
        LSInput srcdoc = createLSInput();
        srcdoc.setStringData(xmldoc);
        return srcdoc;
    }

    public LSInput createLSInput() {
        return implLS.createLSInput();
    }

    public LSParser createLSParser() {
        return implLS.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, "http://www.w3.org/2001/XMLSchema");
    }

    public boolean match(String template, Node source) {
        LSParser dp = createLSParser();
        if (dp == null) {
            System.out.println("Can not create LSParser.");
            return false;
        }
        LSInput src = getXmlSource(template);
        Document doc = dp.parse(src);
        return checkXMLs(doc, source);
    }

    // ========================== XML comparison code ==============
    public boolean checkXMLs(Node template, Node source) {
        if (source == null || template == null) {
            return template == source;
        }
        String tname = template.getLocalName();
        String tvalue = template.getNodeValue();
        NamedNodeMap tattr = template.getAttributes();
        NodeList tchildren = template.getChildNodes();

        String sname = source.getLocalName();
        String svalue = source.getNodeValue();
        NamedNodeMap sattr = source.getAttributes();
        NodeList schildren = source.getChildNodes();
        if (tname != null && !tname.equals(sname)) {
            return false;
        }
        if (tvalue != null && !tvalue.equals(svalue)) {
            return false;
        }
        if (tattr != null && sattr != null) {
            if (sattr.getLength() != tattr.getLength()) {
                return false;
            }
            for (int i = 0; i < tattr.getLength(); i++) {
                Attr t = (Attr) tattr.item(i);
                Attr s = (Attr) sattr.getNamedItem(t.getName());
                if (!checkXMLAttrs(t, s)) {
                    // ref.println(sname+": [expected attr: " + t +
                    // "; actual attr: " +s+"]");
                    return false;
                }
            }
        } else if (tattr != null || sattr != null) {
            return false;
        }

        for (int i = 0; i < tchildren.getLength(); i++) {
            if (!checkXMLs(tchildren.item(i), schildren.item(i))) {
                // ref.println(sname+": [expected node: "+tchildren.item(i)
                // +"; actual node: "+schildren.item(i)+"]");
                return false;
            }
        }
        return true;
    }

    public boolean checkXMLAttrs(Attr template, Attr source) {
        if (source == null || template == null) {
            return template == source;
        }
        String tname = template.getName();
        String tvalue = template.getValue();
        String sname = source.getName();
        String svalue = source.getValue();
        System.out.println("Attr:" + tname + "?" + sname);
        if (tname != null && !tname.equals(sname)) {
            // ref.println("Attr Name:" + tname + "!=" + sname);
            return false;
        }
        if (tvalue != null && !tvalue.equals(svalue)) {
            // ref.println("Attr value:" + tvalue + "!=" + svalue);
            return false;
        }
        // ref.println("Attr:" + tname + ":" + tvalue + "=" + sname + ":" +
        // svalue);
        return true;
    }

    /**
     * Equivalence class partitioning with state, input and output values
     * orientation for public Document parse(LSInput is), <br>
     * <b>pre-conditions</b>: set filter that SKIPs ELEMENT1 node, <br>
     * <b>is</b>: xml1 <br>
     * <b>output</b>: XML document with CHILD1 and ELEMENT2 only.
     */
    @Test
    public void testFilter0002() {
        LSParser parser = createLSParser();
        if (parser == null) {
            Assert.fail("Unable to create LSParser!");
        }
        // set filter
        parser.setFilter(new LSParserFilter() {
            public short startElement(Element elt) {
                return FILTER_ACCEPT;
            }

            public short acceptNode(Node enode) {
                if (enode.getNodeName().startsWith("ELEMENT1")) {
                    return FILTER_SKIP;
                }
                return FILTER_ACCEPT;
            }

            public int getWhatToShow() {
                return NodeFilter.SHOW_ALL;
            }
        });
        String expected = "<?xml version=\"1.0\"?><ROOT><CHILD1/><CHILD1><COC1/></CHILD1><ELEMENT2>test1<CHILD2/></ELEMENT2></ROOT>";
        Document doc = parser.parse(getXmlSource(xml1));
        if (!match(expected, doc)) {
            Assert.fail("DOM structure after parsing is not equal to a structure of XML document, that being parsed");
        }
        System.out.println("OKAY");
    }

    /**
     * Equivalence class partitioning with state, input and output values
     * orientation for public Document parse(LSInput is), <br>
     * <b>pre-conditions</b>: set filter that SKIPs ELEMENT1 node, <br>
     * <b>is</b>: xml1 <br>
     * <b>output</b>: XML document with ELEMENT1 only.
     */
    @Test
    public void testFilter0003() {
        LSParser parser = createLSParser();
        if (parser == null) {
            Assert.fail("Unable to create LSParser!");
        }
        // set filter
        parser.setFilter(new LSParserFilter() {
            public short startElement(Element elt) {
                return FILTER_ACCEPT;
            }

            public short acceptNode(Node enode) {
                if (enode.getNodeName().startsWith("ELEMENT2")) {
                    return FILTER_INTERRUPT;
                }
                return FILTER_ACCEPT;
            }

            public int getWhatToShow() {
                return NodeFilter.SHOW_ALL;
            }
        });
        String expected = "<ROOT><ELEMENT1><CHILD1/><CHILD1><COC1/></CHILD1></ELEMENT1></ROOT>";
        Document doc = parser.parse(getXmlSource(xml1));
        if (!match(expected, doc)) {
            Assert.fail("DOM structure after parsing is not equal to a structure of XML document, that being parsed");
        }
        System.out.println("OKAY");
    }

    /**
     * Equivalence class partitioning with state, input and output values
     * orientation for public Document parse(LSInput is), <br>
     * <b>pre-conditions</b>: set filter that accepts all, <br>
     * <b>is</b>: xml1 <br>
     * <b>output</b>: full XML document.
     */
    @Test
    public void testFilter0004() {
        LSParser parser = createLSParser();
        if (parser == null) {
            Assert.fail("Unable to create LSParser!");
        }
        // set filter
        parser.setFilter(new LSParserFilter() {
            public short startElement(Element elt) {
                return FILTER_ACCEPT;
            }

            public short acceptNode(Node enode) {
                return FILTER_ACCEPT;
            }

            public int getWhatToShow() {
                return NodeFilter.SHOW_ALL;
            }
        });
        String expected = "<ROOT><ELEMENT1><CHILD1/><CHILD1><COC1/></CHILD1></ELEMENT1><ELEMENT2>test1<CHILD2/></ELEMENT2></ROOT>";
        Document doc = parser.parse(getXmlSource(xml1));
        if (!match(expected, doc)) {
            Assert.fail("DOM structure after parsing is not equal to a structure of XML document, that being parsed");
        }
        System.out.println("OKAY");
    }

    /**
     * Equivalence class partitioning with state, input and output values
     * orientation for public Document parse(LSInput is), <br>
     * <b>pre-conditions</b>: set filter that REJECTs all, <br>
     * <b>is</b>: xml1 <br>
     * <b>output</b>: empty XML document.
     */
    @Test
    public void testFilter0005() {
        LSParser parser = createLSParser();
        if (parser == null) {
            Assert.fail("Unable to create LSParser!");
        }
        // set filter
        parser.setFilter(new LSParserFilter() {
            public short startElement(Element elt) {
                return FILTER_ACCEPT;
            }

            public short acceptNode(Node enode) {
                return FILTER_REJECT;
            }

            public int getWhatToShow() {
                return NodeFilter.SHOW_ALL;
            }
        });
        Document doc = parser.parse(getXmlSource(xml1));
        NodeList children = doc.getDocumentElement().getChildNodes();
        if (children.getLength() != 0) {
            Assert.fail("Not all children skipped");
        }
        System.out.println("OKAY");
    }

    /**
     * Equivalence class partitioning with state, input and output values
     * orientation for public Document parse(LSInput is), <br>
     * <b>pre-conditions</b>: set filter that SKIPs all, <br>
     * <b>is</b>: xml1 <br>
     * <b>output</b>: empty XML document.
     */
    @Test
    public void testFilter0006() {
        LSParser parser = createLSParser();
        if (parser == null) {
            Assert.fail("Unable to create LSParser!");
        }
        // set filter
        parser.setFilter(new LSParserFilter() {
            public short startElement(Element elt) {
                return FILTER_ACCEPT;
            }

            public short acceptNode(Node enode) {
                return FILTER_SKIP;
            }

            public int getWhatToShow() {
                return NodeFilter.SHOW_ALL;
            }
        });
        Document doc = parser.parse(getXmlSource(xml1));
        NodeList children = doc.getDocumentElement().getChildNodes();
        if (children.getLength() != 0) {
            Assert.fail("Not all children skipped");
        }
        System.out.println("OKAY");
    }

    /**
     * Equivalence class partitioning with state, input and output values
     * orientation for public Document parse(LSInput is), <br>
     * <b>pre-conditions</b>: set filter that REJECTs any CHILD* start element, <br>
     * <b>is</b>: xml1 <br>
     * <b>output</b>: XML document with ELEMENT1 and ELEMENT2 only.
     */
    @Test
    public void testFilter0007() {
        LSParser parser = createLSParser();
        if (parser == null) {
            Assert.fail("Unable to create LSParser!");
        }
        // set filter
        parser.setFilter(new LSParserFilter() {
            public short startElement(Element elt) {
                if (elt.getTagName().startsWith("CHILD")) {
                    return FILTER_REJECT;
                }
                return FILTER_ACCEPT;
            }

            public short acceptNode(Node enode) {
                return FILTER_ACCEPT;
            }

            public int getWhatToShow() {
                return NodeFilter.SHOW_ALL;
            }
        });
        String expected = "<?xml version=\"1.0\"?><ROOT><ELEMENT1></ELEMENT1><ELEMENT2>test1</ELEMENT2></ROOT>";
        Document doc = parser.parse(getXmlSource(xml1));
        if (!match(expected, doc)) {
            Assert.fail("DOM structure after parsing is not equal to a structure of XML document, that being parsed");
        }
        System.out.println("OKAY");
    }

    /**
     * Equivalence class partitioning with state, input and output values
     * orientation for public Document parse(LSInput is), <br>
     * <b>pre-conditions</b>: set filter that SKIPs ELEMENT1 start element, <br>
     * <b>is</b>: xml1 <br>
     * <b>output</b>: XML document with CHILD1 and ELEMENT2 only.
     */
    @Test
    public void testFilter0008() {
        LSParser parser = createLSParser();
        if (parser == null) {
            Assert.fail("Unable to create LSParser!");
        }
        // set filter
        parser.setFilter(new LSParserFilter() {
            public short startElement(Element elt) {
                if (elt.getTagName().equals("ELEMENT1")) {
                    return FILTER_SKIP;
                }
                return FILTER_ACCEPT;
            }

            public short acceptNode(Node enode) {
                return FILTER_ACCEPT;
            }

            public int getWhatToShow() {
                return NodeFilter.SHOW_ALL;
            }
        });
        String expected = "<?xml version=\"1.0\"?><ROOT><CHILD1/><CHILD1><COC1/></CHILD1><ELEMENT2>test1<CHILD2/></ELEMENT2></ROOT>";
        Document doc = parser.parse(getXmlSource(xml1));
        if (!match(expected, doc)) {
            Assert.fail("DOM structure after parsing is not equal to a structure of XML document, that being parsed");
        }
        System.out.println("OKAY");
    }

    /**
     * Equivalence class partitioning with state, input and output values
     * orientation for public Document parse(LSInput is), <br>
     * <b>pre-conditions</b>: set filter that SKIPs ELEMENT1 start element, <br>
     * <b>is</b>: xml1 <br>
     * <b>output</b>: XML document with ELEMENT1 only.
     */
    @Test
    public void testFilter0009() {
        LSParser parser = createLSParser();
        if (parser == null) {
            Assert.fail("Unable to create LSParser!");
        }
        // set filter
        parser.setFilter(new LSParserFilter() {
            public short startElement(Element elt) {
                if (elt.getTagName().startsWith("ELEMENT2")) {
                    return FILTER_INTERRUPT;
                }
                return FILTER_ACCEPT;
            }

            public short acceptNode(Node enode) {
                return FILTER_ACCEPT;
            }

            public int getWhatToShow() {
                return NodeFilter.SHOW_ALL;
            }
        });
        String expected = "<ROOT><ELEMENT1><CHILD1/><CHILD1><COC1/></CHILD1></ELEMENT1></ROOT>";
        Document doc = parser.parse(getXmlSource(xml1));
        if (!match(expected, doc)) {
            Assert.fail("DOM structure after parsing is not equal to a structure of XML document, that being parsed");
        }
        System.out.println("OKAY");
    }

    /**
     * Equivalence class partitioning with state, input and output values
     * orientation for public Document parse(LSInput is), <br>
     * <b>pre-conditions</b>: set filter that REJECTs all start element, <br>
     * <b>is</b>: xml1 <br>
     * <b>output</b>: empty XML document.
     */
    @Test
    public void testFilter0010() {
        LSParser parser = createLSParser();
        if (parser == null) {
            Assert.fail("Unable to create LSParser");
        }
        // set filter
        parser.setFilter(new LSParserFilter() {
            public short startElement(Element elt) {
                return FILTER_REJECT;
            }

            public short acceptNode(Node enode) {
                return FILTER_ACCEPT;
            }

            public int getWhatToShow() {
                return NodeFilter.SHOW_ALL;
            }
        });
        Document doc = parser.parse(getXmlSource(xml1));
        NodeList children = doc.getDocumentElement().getChildNodes();
        if (children.getLength() != 0) {
            Assert.fail("Not all children skipped");
        }
        System.out.println("OKAY");
    }

    /**
     * Equivalence class partitioning with state, input and output values
     * orientation for public Document parse(LSInput is), <br>
     * <b>pre-conditions</b>: set filter that SKIPs all, <br>
     * <b>is</b>: xml1 <br>
     * <b>output</b>: empty XML document.
     */
    @Test
    public void testFilter0011() {
        LSParser parser = createLSParser();
        if (parser == null) {
            Assert.fail("Unable to create LSParser");
        }
        // set filter
        parser.setFilter(new LSParserFilter() {
            public short startElement(Element elt) {
                return FILTER_SKIP;
            }

            public short acceptNode(Node enode) {
                return FILTER_ACCEPT;
            }

            public int getWhatToShow() {
                return NodeFilter.SHOW_ALL;
            }
        });
        Document doc = parser.parse(getXmlSource(xml1));
        NodeList children = doc.getDocumentElement().getChildNodes();
        if (children.getLength() != 1) {
            Assert.fail("Not all Element nodes skipped");
        }
        System.out.println("OKAY");
    }

}
