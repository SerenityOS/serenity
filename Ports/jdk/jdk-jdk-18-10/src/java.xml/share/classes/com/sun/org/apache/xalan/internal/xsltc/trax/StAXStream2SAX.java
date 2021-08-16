/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.org.apache.xalan.internal.xsltc.trax;

import java.io.IOException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.ext.Locator2;
import org.xml.sax.helpers.AttributesImpl;
import com.sun.org.apache.xalan.internal.xsltc.dom.SAXImpl;



import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;



/**
 * @author Padmaja Vedula
 * @author Sunitha Reddy
 */
public class StAXStream2SAX implements XMLReader, Locator {

    //private final static String EMPTYSTRING = "";
    //private static final String XMLNS_PREFIX = "xmlns";

    // StAX Stream source
    private final XMLStreamReader staxStreamReader;

    //private Node _dom = null;
    private ContentHandler _sax = null;
    private LexicalHandler _lex = null;
    private SAXImpl _saxImpl = null;

    public StAXStream2SAX(XMLStreamReader staxSrc) {
            staxStreamReader = staxSrc;
    }

    public ContentHandler getContentHandler() {
        return _sax;
    }

    public void setContentHandler(ContentHandler handler) throws
        NullPointerException
    {
        _sax = handler;
        if (handler instanceof LexicalHandler) {
            _lex = (LexicalHandler) handler;
        }

        if (handler instanceof SAXImpl) {
            _saxImpl = (SAXImpl)handler;
        }
    }


    public void parse(InputSource unused) throws IOException, SAXException {
        try {
            bridge();
        } catch (XMLStreamException e) {
            throw new SAXException(e);
        }
    }


    //Main Work Starts Here.
    public void parse() throws IOException, SAXException, XMLStreamException {
        bridge();
    }


   /**
     * This class is only used internally so this method should never
     * be called.
     */
    public void parse(String sysId) throws IOException, SAXException {
        throw new IOException("This method is not yet implemented.");
    }


   public void bridge() throws XMLStreamException {

        try {
            // remembers the nest level of elements to know when we are done.
            int depth=0;
            boolean startedAtDocument = false;

            // skip over START_DOCUMENT
            int event = staxStreamReader.getEventType();
            if (event == XMLStreamConstants.START_DOCUMENT) {
                startedAtDocument = true;
                event = staxStreamReader.next();
            }

            handleStartDocument();

            // Handle the prolog: http://www.w3.org/TR/REC-xml/#NT-prolog
            while (event != XMLStreamConstants.START_ELEMENT) {
                switch (event) {
                    case XMLStreamConstants.CHARACTERS :
                        handleCharacters();
                        break;
                    case XMLStreamConstants.PROCESSING_INSTRUCTION :
                        handlePI();
                        break;
                    case XMLStreamConstants.COMMENT :
                        handleComment();
                        break;
                    case XMLStreamConstants.DTD :
                        handleDTD();
                        break;
                    case XMLStreamConstants.SPACE :
                        handleSpace();
                        break;
                    default :
                        throw new InternalError("processing prolog event: " + event);
                }
                event=staxStreamReader.next();
            }

            do {
                // These are all of the events listed in the javadoc for
                // XMLEvent.
                // The spec only really describes 11 of them.
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT :
                        depth++;
                        handleStartElement();
                        break;
                    case XMLStreamConstants.END_ELEMENT :
                        handleEndElement();
                        depth--;
                        break;
                    case XMLStreamConstants.CHARACTERS :
                        handleCharacters();
                        break;
                    case XMLStreamConstants.ENTITY_REFERENCE :
                        handleEntityReference();
                        break;
                    case XMLStreamConstants.PROCESSING_INSTRUCTION :
                        handlePI();
                        break;
                    case XMLStreamConstants.COMMENT :
                        handleComment();
                        break;
                    case XMLStreamConstants.DTD :
                        handleDTD();
                        break;
                    case XMLStreamConstants.ATTRIBUTE :
                        handleAttribute();
                        break;
                    case XMLStreamConstants.NAMESPACE :
                        handleNamespace();
                        break;
                    case XMLStreamConstants.CDATA :
                        handleCDATA();
                        break;
                    case XMLStreamConstants.ENTITY_DECLARATION :
                        handleEntityDecl();
                        break;
                    case XMLStreamConstants.NOTATION_DECLARATION :
                        handleNotationDecl();
                        break;
                    case XMLStreamConstants.SPACE :
                        handleSpace();
                        break;
                    default :
                        throw new InternalError("processing event: " + event);
                }

                event=staxStreamReader.next();
            } while (depth!=0);

            if (startedAtDocument) {
                // Handle the Misc (http://www.w3.org/TR/REC-xml/#NT-Misc) that can follow the document element
                while (event != XMLStreamConstants.END_DOCUMENT) {
                    switch (event) {
                        case XMLStreamConstants.CHARACTERS :
                            handleCharacters();
                            break;
                        case XMLStreamConstants.PROCESSING_INSTRUCTION :
                            handlePI();
                            break;
                        case XMLStreamConstants.COMMENT :
                            handleComment();
                            break;
                        case XMLStreamConstants.SPACE :
                            handleSpace();
                            break;
                        default :
                            throw new InternalError("processing misc event after document element: " + event);
                    }
                    event=staxStreamReader.next();
                }
            }

            handleEndDocument();
        } catch (SAXException e) {
            throw new XMLStreamException(e);
        }
    }

    private void handleEndDocument() throws SAXException {
        _sax.endDocument();
    }

    private void handleStartDocument() throws SAXException {
        _sax.setDocumentLocator(new Locator2() {
            public int getColumnNumber() {
                return staxStreamReader.getLocation().getColumnNumber();
            }
            public int getLineNumber() {
                return staxStreamReader.getLocation().getLineNumber();
            }
            public String getPublicId() {
                return staxStreamReader.getLocation().getPublicId();
            }
            public String getSystemId() {
                return staxStreamReader.getLocation().getSystemId();
            }
            public String getXMLVersion() {
                return staxStreamReader.getVersion();
            }
            public String getEncoding() {
                return staxStreamReader.getEncoding();
            }
         });
        _sax.startDocument();
    }

    private void handlePI() throws XMLStreamException {
        try {
            _sax.processingInstruction(
                staxStreamReader.getPITarget(),
                staxStreamReader.getPIData());
        } catch (SAXException e) {
            throw new XMLStreamException(e);
        }
    }

    private void handleCharacters() throws XMLStreamException {

        // workaround for bugid 5046319 - switch over to commented section
        // below when it is fixed.
        int textLength = staxStreamReader.getTextLength();
        char[] chars = new char[textLength];

        staxStreamReader.getTextCharacters(0, chars, 0, textLength);

        try {
            _sax.characters(chars, 0, chars.length);
        } catch (SAXException e) {
            throw new XMLStreamException(e);
        }


//        int start = 0;
//        int len;
//        do {
//            len = staxStreamReader.getTextCharacters(start, buf, 0, buf.length);
//            start += len;
//            try {
//                _sax.characters(buf, 0, len);
//            } catch (SAXException e) {
//                throw new XMLStreamException(e);
//            }
//        } while (len == buf.length);
    }

    private void handleEndElement() throws XMLStreamException {
        QName qName = staxStreamReader.getName();

        try {
            //construct prefix:localName from qName
            String qname = "";
            if (qName.getPrefix() != null && qName.getPrefix().trim().length() != 0){
                qname = qName.getPrefix() + ":";
            }
            qname += qName.getLocalPart();

            // fire endElement
            _sax.endElement(
                qName.getNamespaceURI(),
                qName.getLocalPart(),
                qname);

            // end namespace bindings
            int nsCount = staxStreamReader.getNamespaceCount();
            for (int i = nsCount - 1; i >= 0; i--) {
                String prefix = staxStreamReader.getNamespacePrefix(i);
                if (prefix == null) { // true for default namespace
                    prefix = "";
                }
                _sax.endPrefixMapping(prefix);
            }
        } catch (SAXException e) {
            throw new XMLStreamException(e);
        }
    }

    private void handleStartElement() throws XMLStreamException {

        try {
            // start namespace bindings
            int nsCount = staxStreamReader.getNamespaceCount();
            for (int i = 0; i < nsCount; i++) {
                String prefix = staxStreamReader.getNamespacePrefix(i);
                if (prefix == null) { // true for default namespace
                    prefix = "";
                }
                String uri = staxStreamReader.getNamespaceURI(i);
                if (uri == null && prefix.isEmpty()) { // true for default namespace
                    uri = "";
                }

                _sax.startPrefixMapping(prefix, uri);
            }

            // fire startElement
            QName qName = staxStreamReader.getName();
            String prefix = qName.getPrefix();
            String rawname;
            if(prefix==null || prefix.length()==0)
                rawname = qName.getLocalPart();
            else
                rawname = prefix + ':' + qName.getLocalPart();
            Attributes attrs = getAttributes();
            _sax.startElement(
                qName.getNamespaceURI(),
                qName.getLocalPart(),
                rawname,
                attrs);
        } catch (SAXException e) {
            throw new XMLStreamException(e);
        }
    }

    /**
     * Get the attributes associated with the given START_ELEMENT or ATTRIBUTE
     * StAXevent.
     *
     * @return the StAX attributes converted to an org.xml.sax.Attributes
     */
    private Attributes getAttributes() {
        AttributesImpl attrs = new AttributesImpl();

        int eventType = staxStreamReader.getEventType();
        if (eventType != XMLStreamConstants.ATTRIBUTE
            && eventType != XMLStreamConstants.START_ELEMENT) {
            throw new InternalError(
                "getAttributes() attempting to process: " + eventType);
        }

        // in SAX, namespace declarations are not part of attributes by default.
        // (there's a property to control that, but as far as we are concerned
        // we don't use it.) So don't add xmlns:* to attributes.

        // gather non-namespace attrs
        for (int i = 0; i < staxStreamReader.getAttributeCount(); i++) {
            String uri = staxStreamReader.getAttributeNamespace(i);
            if(uri==null)   uri="";
            String localName = staxStreamReader.getAttributeLocalName(i);
            String prefix = staxStreamReader.getAttributePrefix(i);
            String qName;
            if(prefix==null || prefix.length()==0)
                qName = localName;
            else
                qName = prefix + ':' + localName;
            String type = staxStreamReader.getAttributeType(i);
            String value = staxStreamReader.getAttributeValue(i);

            attrs.addAttribute(uri, localName, qName, type, value);
        }

        return attrs;
    }

    private void handleNamespace() {
        // no-op ???
        // namespace events don't normally occur outside of a startElement
        // or endElement
    }

    private void handleAttribute() {
        // no-op ???
        // attribute events don't normally occur outside of a startElement
        // or endElement
    }

    private void handleDTD() {
        // no-op ???
        // it seems like we need to pass this info along, but how?
    }

    private void handleComment() {
        // no-op ???
    }

    private void handleEntityReference() {
        // no-op ???
    }

    private void handleSpace() {
        // no-op ???
        // this event is listed in the javadoc, but not in the spec.
    }

    private void handleNotationDecl() {
        // no-op ???
        // this event is listed in the javadoc, but not in the spec.
    }

    private void handleEntityDecl() {
        // no-op ???
        // this event is listed in the javadoc, but not in the spec.
    }

    private void handleCDATA() {
        // no-op ???
        // this event is listed in the javadoc, but not in the spec.
    }


    /**
     * This class is only used internally so this method should never
     * be called.
     */
    public DTDHandler getDTDHandler() {
        return null;
    }

    /**
     * This class is only used internally so this method should never
     * be called.
     */
    public ErrorHandler getErrorHandler() {
        return null;
    }

    /**
     * This class is only used internally so this method should never
     * be called.
     */
    public boolean getFeature(String name) throws SAXNotRecognizedException,
        SAXNotSupportedException
    {
        return false;
    }

    /**
     * This class is only used internally so this method should never
     * be called.
     */
    public void setFeature(String name, boolean value) throws
        SAXNotRecognizedException, SAXNotSupportedException
    {
    }

    /**
     * This class is only used internally so this method should never
     * be called.
     */
    public void setDTDHandler(DTDHandler handler) throws NullPointerException {
    }

    /**
     * This class is only used internally so this method should never
     * be called.
     */
    public void setEntityResolver(EntityResolver resolver) throws
        NullPointerException
    {
    }

    /**
     * This class is only used internally so this method should never
     * be called.
     */
    public EntityResolver getEntityResolver() {
        return null;
    }

    /**
     * This class is only used internally so this method should never
     * be called.
     */
    public void setErrorHandler(ErrorHandler handler) throws
        NullPointerException
    {
    }

    /**
     * This class is only used internally so this method should never
     * be called.
     */
    public void setProperty(String name, Object value) throws
        SAXNotRecognizedException, SAXNotSupportedException {
    }

    /**
     * This class is only used internally so this method should never
     * be called.
     */
    public Object getProperty(String name) throws SAXNotRecognizedException,
        SAXNotSupportedException
    {
        return null;
    }

    /**
     * This class is only used internally so this method should never
     * be called.
     */
    public int getColumnNumber() {
        return 0;
    }

    /**
     * This class is only used internally so this method should never
     * be called.
     */
    public int getLineNumber() {
        return 0;
    }

    /**
     * This class is only used internally so this method should never
     * be called.
     */
    public String getPublicId() {
        return null;
    }

    /**
     * This class is only used internally so this method should never
     * be called.
     */
    public String getSystemId() {
        return null;
    }

}
