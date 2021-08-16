/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
 * @LastModified: Oct 2017
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

import com.sun.org.apache.xalan.internal.xsltc.dom.SAXImpl;
import java.io.IOException;
import java.util.Iterator;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
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


/**
 * @author Suresh Kumar
 * @author Sunitha Reddy
 * @since 1.6
 */
public class StAXEvent2SAX implements XMLReader, Locator {

    //private final static String EMPTYSTRING = "";
    //private static final String XMLNS_PREFIX = "xmlns";

    // StAX event source
    private final XMLEventReader staxEventReader;

    //private Node _dom = null;
    private ContentHandler _sax = null;
    private LexicalHandler _lex = null;
    private SAXImpl _saxImpl = null;
    private String version = null;
    private String encoding = null;


    public StAXEvent2SAX(XMLEventReader staxCore) {
        staxEventReader = staxCore;
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


    /*  public void parse() throws IOException, SAXException {
        if (_dom != null) {
            boolean isIncomplete =
                (_dom.getNodeType() != org.w3c.dom.Node.DOCUMENT_NODE);

            if (isIncomplete) {
                _sax.startDocument();
                parse(_dom);
                _sax.endDocument();
            }
            else {
                parse(_dom);
            }
        }
    }
    */

    /*
     * @see StAXReaderToContentHandler#bridge()
     */
    private void bridge() throws XMLStreamException {

        try {
            // remembers the nest level of elements to know when we are done.
            int depth=0;
            boolean startedAtDocument = false;

            XMLEvent event = staxEventReader.peek();

            if (!event.isStartDocument() && !event.isStartElement()) {
                throw new IllegalStateException();
            }

            if (event.getEventType() == XMLStreamConstants.START_DOCUMENT){
                startedAtDocument = true;
                version = ((StartDocument)event).getVersion();
                if (((StartDocument)event).encodingSet())
                    encoding = ((StartDocument)event).getCharacterEncodingScheme();
                event=staxEventReader.nextEvent(); // that gets the one we peeked at
                event=staxEventReader.nextEvent(); // that really gets the next one
            }

            handleStartDocument(event);

            // Handle the prolog: http://www.w3.org/TR/REC-xml/#NT-prolog
            while (event.getEventType() != XMLStreamConstants.START_ELEMENT) {
                switch (event.getEventType()) {
                    case XMLStreamConstants.CHARACTERS :
                        handleCharacters(event.asCharacters());
                        break;
                    case XMLStreamConstants.PROCESSING_INSTRUCTION :
                        handlePI((ProcessingInstruction)event);
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
                event=staxEventReader.nextEvent();
            }

            // Process the (document) element
            do {
                // These are all of the events listed in the javadoc for
                // XMLEvent.
                // The spec only really describes 11 of them.
                switch (event.getEventType()) {
                    case XMLStreamConstants.START_ELEMENT :
                        depth++;
                        handleStartElement(event.asStartElement());
                        break;
                    case XMLStreamConstants.END_ELEMENT :
                        handleEndElement(event.asEndElement());
                        depth--;
                        break;
                    case XMLStreamConstants.CHARACTERS :
                        handleCharacters(event.asCharacters());
                        break;
                    case XMLStreamConstants.ENTITY_REFERENCE :
                        handleEntityReference();
                        break;
                    case XMLStreamConstants.PROCESSING_INSTRUCTION :
                        handlePI((ProcessingInstruction)event);
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

                event=staxEventReader.nextEvent();
            } while (depth!=0);

            if (startedAtDocument) {
                // Handle the Misc (http://www.w3.org/TR/REC-xml/#NT-Misc) that can follow the document element
                while (event.getEventType() != XMLStreamConstants.END_DOCUMENT) {
                    switch (event.getEventType()) {
                        case XMLStreamConstants.CHARACTERS :
                            handleCharacters(event.asCharacters());
                            break;
                        case XMLStreamConstants.PROCESSING_INSTRUCTION :
                            handlePI((ProcessingInstruction)event);
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
                    event=staxEventReader.nextEvent();
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

    private void handleStartDocument(final XMLEvent event) throws SAXException {
        _sax.setDocumentLocator(new Locator2() {
            public int getColumnNumber() {
                return event.getLocation().getColumnNumber();
            }
            public int getLineNumber() {
                return event.getLocation().getLineNumber();
            }
            public String getPublicId() {
                return event.getLocation().getPublicId();
            }
            public String getSystemId() {
                return event.getLocation().getSystemId();
            }
            public String getXMLVersion(){
                return version;
            }
            public String getEncoding(){
                return encoding;
            }

        });
        _sax.startDocument();
    }

    private void handlePI(ProcessingInstruction event)
        throws XMLStreamException {
        try {
            _sax.processingInstruction(
                event.getTarget(),
                event.getData());
        } catch (SAXException e) {
            throw new XMLStreamException(e);
        }
    }

    private void handleCharacters(Characters event) throws XMLStreamException {
        try {
            _sax.characters(
                event.getData().toCharArray(),
                0,
                event.getData().length());
        } catch (SAXException e) {
            throw new XMLStreamException(e);
        }
    }

    private void handleEndElement(EndElement event) throws XMLStreamException {
        QName qName = event.getName();

        //construct prefix:localName from qName
        String qname = "";
        if (qName.getPrefix() != null && qName.getPrefix().trim().length() != 0){
            qname = qName.getPrefix() + ":";
        }
        qname += qName.getLocalPart();

        try {
            // fire endElement
            _sax.endElement(
                qName.getNamespaceURI(),
                qName.getLocalPart(),
                qname);

            // end namespace bindings
            for( Iterator<Namespace> i = event.getNamespaces(); i.hasNext();) {
                String prefix = (i.next()).getPrefix();
                if( prefix == null ) { // true for default namespace
                    prefix = "";
                }
                _sax.endPrefixMapping(prefix);
            }
        } catch (SAXException e) {
            throw new XMLStreamException(e);
        }
    }

    private void handleStartElement(StartElement event)
        throws XMLStreamException {
        try {
            // start namespace bindings
            for (Iterator<Namespace> i = event.getNamespaces(); i.hasNext();) {
                String prefix = (i.next()).getPrefix();
                if (prefix == null) { // true for default namespace
                    prefix = "";
                }
                _sax.startPrefixMapping(
                    prefix,
                    event.getNamespaceURI(prefix));
            }

            // fire startElement
            QName qName = event.getName();
            String prefix = qName.getPrefix();
            String rawname;
            if (prefix == null || prefix.length() == 0) {
                rawname = qName.getLocalPart();
            } else {
                rawname = prefix + ':' + qName.getLocalPart();
            }

            Attributes saxAttrs = getAttributes(event);
            _sax.startElement(
                qName.getNamespaceURI(),
                qName.getLocalPart(),
                rawname,
                saxAttrs);
        } catch (SAXException e) {
            throw new XMLStreamException(e);
        }
    }

    /**
     * Get the attributes associated with the given START_ELEMENT StAXevent.
     *
     * @return the StAX attributes converted to an org.xml.sax.Attributes
     */
    private Attributes getAttributes(StartElement event) {
        AttributesImpl attrs = new AttributesImpl();

        if ( !event.isStartElement() ) {
            throw new InternalError(
                "getAttributes() attempting to process: " + event);
        }

        // in SAX, namespace declarations are not part of attributes by default.
        // (there's a property to control that, but as far as we are concerned
        // we don't use it.) So don't add xmlns:* to attributes.

        // gather non-namespace attrs
        for (Iterator<Attribute> i = event.getAttributes(); i.hasNext();) {
            Attribute staxAttr = i.next();

            String uri = staxAttr.getName().getNamespaceURI();
            if (uri == null) {
                uri = "";
            }
            String localName = staxAttr.getName().getLocalPart();
            String prefix = staxAttr.getName().getPrefix();
            String qName;
            if (prefix == null || prefix.length() == 0) {
                qName = localName;
            } else {
                qName = prefix + ':' + localName;
            }
            String type = staxAttr.getDTDType();
            String value = staxAttr.getValue();

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
    public void parse(String sysId) throws IOException, SAXException {
        throw new IOException("This method is not yet implemented.");
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
