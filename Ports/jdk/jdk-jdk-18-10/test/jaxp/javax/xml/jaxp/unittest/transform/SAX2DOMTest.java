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

package transform;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.SAX2DOMTest
 * @run testng/othervm transform.SAX2DOMTest
 * @summary Test Transforming from SAX to DOM.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class SAX2DOMTest {

    @Test
    public void test() throws Exception {
        SAXParserFactory fac = SAXParserFactory.newInstance();
        fac.setNamespaceAware(true);
        SAXParser saxParser = fac.newSAXParser();

        StreamSource sr = new StreamSource(this.getClass().getResourceAsStream("SAX2DOMTest.xml"));
        InputSource is = SAXSource.sourceToInputSource(sr);
        RejectDoctypeSaxFilter rf = new RejectDoctypeSaxFilter(saxParser);
        SAXSource src = new SAXSource(rf, is);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        DOMResult result = new DOMResult();
        transformer.transform(src, result);

        Document doc = (Document) result.getNode();
        System.out.println("Name" + doc.getDocumentElement().getLocalName());

        String id = "XWSSGID-11605791027261938254268";
        Element selement = doc.getElementById(id);
        if (selement == null) {
            System.out.println("getElementById returned null");
        }

    }

    public static class RejectDoctypeSaxFilter extends XMLFilterImpl implements XMLReader, LexicalHandler {

        /** Standard SAX 2.0 ext property */
        static final String LEXICAL_HANDLER_PROP = "http://xml.org/sax/properties/lexical-handler";

        static final String WSU_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd".intern();
        static final String SIGNATURE_LNAME = "Signature".intern();
        static final String ENCRYPTED_DATA_LNAME = "EncryptedData".intern();
        static final String DSIG_NS = "http://www.w3.org/2000/09/xmldsig#".intern();
        static final String XENC_NS = "http://www.w3.org/2001/04/xmlenc#".intern();
        static final String ID_NAME = "ID".intern();

        /** LexicalHandler to forward events to, if any */
        private LexicalHandler lexicalHandler;

        public RejectDoctypeSaxFilter(SAXParser saxParser) throws Exception {
            XMLReader xmlReader;
            try {
                xmlReader = saxParser.getXMLReader();
            } catch (Exception e) {
                throw new Exception("Couldn't get an XMLReader while constructing a RejectDoctypeSaxFilter", e);
            }

            // Set ourselves up to be the SAX LexicalHandler
            try {
                xmlReader.setProperty(LEXICAL_HANDLER_PROP, this);
            } catch (Exception e) {
                throw new Exception("Couldn't set the lexical handler property while constructing a RejectDoctypeSaxFilter", e);
            }

            // Set the parent XMLReader of this SAX filter
            setParent(xmlReader);
        }

        /*
         * Override setProperty() to capture any LexicalHandler that is set for
         * forwarding of events.
         */
        public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
            if (LEXICAL_HANDLER_PROP.equals(name)) {
                lexicalHandler = (LexicalHandler) value;
            } else {
                super.setProperty(name, value);
            }
        }

        //
        // Beginning of SAX LexicalHandler callbacks...
        //

        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            throw new SAXException("Document Type Declaration is not allowed");
        }

        public void endDTD() throws SAXException {
        }

        public void startEntity(String name) throws SAXException {
            if (lexicalHandler != null) {
                lexicalHandler.startEntity(name);
            }
        }

        public void endEntity(String name) throws SAXException {
            if (lexicalHandler != null) {
                lexicalHandler.endEntity(name);
            }
        }

        public void startCDATA() throws SAXException {
            if (lexicalHandler != null) {
                lexicalHandler.startCDATA();
            }
        }

        public void endCDATA() throws SAXException {
            if (lexicalHandler != null) {
                lexicalHandler.endCDATA();
            }
        }

        public void comment(char[] ch, int start, int length) throws SAXException {
            if (lexicalHandler != null) {
                lexicalHandler.comment(ch, start, length);
            }
        }

        //
        // End of SAX LexicalHandler callbacks
        //

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (atts != null) {
                boolean eos = false;
                if (namespaceURI == DSIG_NS || XENC_NS == namespaceURI) {
                    eos = true;
                }
                int length = atts.getLength();
                AttributesImpl attrImpl = new AttributesImpl();
                for (int i = 0; i < length; i++) {
                    String name = atts.getLocalName(i);
                    if (name != null && (name.equals("Id"))) {
                        if (eos || atts.getURI(i) == WSU_NS) {
                            attrImpl.addAttribute(atts.getURI(i), atts.getLocalName(i), atts.getQName(i), ID_NAME, atts.getValue(i));
                        } else {
                            attrImpl.addAttribute(atts.getURI(i), atts.getLocalName(i), atts.getQName(i), atts.getType(i), atts.getValue(i));
                        }
                    } else {
                        attrImpl.addAttribute(atts.getURI(i), atts.getLocalName(i), atts.getQName(i), atts.getType(i), atts.getValue(i));
                    }
                }
                super.startElement(namespaceURI, localName, qName, attrImpl);
            } else {
                super.startElement(namespaceURI, localName, qName, atts);
            }
        }
    }
}
