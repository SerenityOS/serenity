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

package com.sun.xml.internal.stream.writers;

import java.util.Iterator;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author Neeraj Bajaj, Sun Microsystems.
 *
 */
public class XMLEventWriterImpl implements XMLEventWriter {

    //delegate everything to XMLStreamWriter..
    private final XMLStreamWriter fStreamWriter;
    private static final boolean DEBUG = false;

    /**
     * Constructs an XMLEventWriterImpl that implements the standard XMLStreamWriter
     * interface.
     * @param streamWriter
     */
    public XMLEventWriterImpl(XMLStreamWriter streamWriter) {
        fStreamWriter = streamWriter;
    }

    /**
     *
     * @param xMLEventReader
     * @throws XMLStreamException
     */
    public void add(XMLEventReader xMLEventReader) throws XMLStreamException {
        if (xMLEventReader == null) {
            throw new XMLStreamException("Event reader shouldn't be null");
        }
        while (xMLEventReader.hasNext()) {
            add(xMLEventReader.nextEvent());
        }
    }

    /**
     *
     * @param xMLEvent
     * @throws XMLStreamException
     */
    public void add(XMLEvent xMLEvent) throws XMLStreamException {
        int type = xMLEvent.getEventType();
        switch (type) {
            case XMLEvent.DTD: {
                DTD dtd = (DTD) xMLEvent;
                if (DEBUG) {
                    System.out.println("Adding DTD = " + dtd.toString());
                }
                fStreamWriter.writeDTD(dtd.getDocumentTypeDeclaration());
                break;
            }
            case XMLEvent.START_DOCUMENT: {
                StartDocument startDocument = (StartDocument) xMLEvent;
                if (DEBUG) {
                    System.out.println("Adding StartDocument = " + startDocument.toString());
                }
                try {
                    if (XMLStreamWriterBase.class.isAssignableFrom(fStreamWriter.getClass())) {
                        // internal impl uses the extended interface
                        ((XMLStreamWriterBase)fStreamWriter).writeStartDocument(
                                startDocument.getCharacterEncodingScheme(),
                                startDocument.getVersion(),
                                startDocument.isStandalone(),
                                startDocument.standaloneSet());
                    } else {
                        fStreamWriter.writeStartDocument(
                                startDocument.getCharacterEncodingScheme(),
                                startDocument.getVersion());
                    }
                } catch (XMLStreamException e) {
                    fStreamWriter.writeStartDocument(startDocument.getVersion());
                }
                break;
            }
            case XMLEvent.START_ELEMENT: {
                StartElement startElement = xMLEvent.asStartElement();
                if (DEBUG) {
                    System.out.println("Adding startelement = " + startElement.toString());
                }
                QName qname = startElement.getName();
                fStreamWriter.writeStartElement(qname.getPrefix(), qname.getLocalPart(),
                        qname.getNamespaceURI());

                /*
                  getNamespaces() Returns an Iterator of namespaces declared on this element.
                This Iterator does not contain previously declared namespaces unless they
                appear on the current START_ELEMENT. Therefore this list may contain redeclared
                namespaces and duplicate namespace declarations. Use the getNamespaceContext()
                method to get the current context of namespace declarations. We should be
                using getNamespaces() to write namespace declarations for this START_ELEMENT
                */
                Iterator<? extends Namespace> iterator = startElement.getNamespaces();
                while (iterator.hasNext()) {
                    Namespace namespace = iterator.next();
                    fStreamWriter.writeNamespace(namespace.getPrefix(), namespace.getNamespaceURI());
                }
                //REVISIT: What about writing attributes ?
                Iterator<? extends Attribute> attributes = startElement.getAttributes();
                while (attributes.hasNext()) {
                    Attribute attribute = attributes.next();
                    QName aqname = attribute.getName();
                    fStreamWriter.writeAttribute(aqname.getPrefix(), aqname.getNamespaceURI(),
                            aqname.getLocalPart(), attribute.getValue());
                }
                break;
            }
            case XMLEvent.NAMESPACE: {
                Namespace namespace = (Namespace) xMLEvent;
                if (DEBUG) {
                    System.out.println("Adding namespace = " + namespace.toString());
                }
                fStreamWriter.writeNamespace(namespace.getPrefix(), namespace.getNamespaceURI());
                break;
            }
            case XMLEvent.COMMENT: {
                Comment comment = (Comment) xMLEvent;
                if (DEBUG) {
                    System.out.println("Adding comment = " + comment.toString());
                }
                fStreamWriter.writeComment(comment.getText());
                break;
            }
            case XMLEvent.PROCESSING_INSTRUCTION: {
                ProcessingInstruction processingInstruction = (ProcessingInstruction) xMLEvent;
                if (DEBUG) {
                    System.out.println("Adding processing instruction = " + processingInstruction.toString());
                }
                fStreamWriter.writeProcessingInstruction(processingInstruction.getTarget(),
                        processingInstruction.getData());
                break;
            }
            case XMLEvent.CHARACTERS: {
                Characters characters = xMLEvent.asCharacters();
                if (DEBUG) {
                    System.out.println("Adding characters = " + characters.toString());
                }
                //check if the CHARACTERS are CDATA
                if (characters.isCData()) {
                    fStreamWriter.writeCData(characters.getData());
                } else {
                    fStreamWriter.writeCharacters(characters.getData());
                }
                break;
            }
            case XMLEvent.ENTITY_REFERENCE: {
                EntityReference entityReference = (EntityReference) xMLEvent;
                if (DEBUG) {
                    System.out.println("Adding Entity Reference = " + entityReference.toString());
                }
                fStreamWriter.writeEntityRef(entityReference.getName());
                break;
            }
            case XMLEvent.ATTRIBUTE: {
                Attribute attribute = (Attribute) xMLEvent;
                if (DEBUG) {
                    System.out.println("Adding Attribute = " + attribute.toString());
                }
                QName qname = attribute.getName();
                fStreamWriter.writeAttribute(qname.getPrefix(), qname.getNamespaceURI(),
                        qname.getLocalPart(), attribute.getValue());
                break;
            }
            case XMLEvent.CDATA: {
                //there is no separate CDATA datatype but CDATA event can be reported
                //by using vendor specific CDATA property.
                Characters characters = (Characters) xMLEvent;
                if (DEBUG) {
                    System.out.println("Adding characters = " + characters.toString());
                }
                if (characters.isCData()) {
                    fStreamWriter.writeCData(characters.getData());
                }
                break;
            }
            //xxx: Why there isn't any event called Notation.
            //case XMLEvent.NOTATION_DECLARATION:{
            //}

            case XMLEvent.END_ELEMENT: {
                fStreamWriter.writeEndElement();
                break;
            }
            case XMLEvent.END_DOCUMENT: {
                fStreamWriter.writeEndDocument();
                break;
            }
            //throw new XMLStreamException("Unknown Event type = " + type);
        };

    }

    /**
     *
     * @throws XMLStreamException
     */
    public void close() throws XMLStreamException {
        fStreamWriter.close();
    }

    /**
     *
     * @throws XMLStreamException will inturn call flush on the stream to which
     * data is being written.
     */
    public void flush() throws XMLStreamException {
        fStreamWriter.flush();
    }

    /**
     *
     * @return
     */
    public NamespaceContext getNamespaceContext() {
        return fStreamWriter.getNamespaceContext();
    }

    /**
     *
     * @param namespaceURI Namespace URI
     * @throws XMLStreamException
     * @return prefix associated with the URI.
     */
    public String getPrefix(String namespaceURI) throws XMLStreamException {
        return fStreamWriter.getPrefix(namespaceURI);
    }

    /**
     *
     * @param uri Namespace URI
     * @throws XMLStreamException
     */
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        fStreamWriter.setDefaultNamespace(uri);
    }

    /**
     *
     * @param namespaceContext Namespace Context
     * @throws XMLStreamException
     */
    public void setNamespaceContext(NamespaceContext namespaceContext)
            throws XMLStreamException {
        fStreamWriter.setNamespaceContext(namespaceContext);
    }

    /**
     *
     * @param prefix namespace prefix associated with the uri.
     * @param uri Namespace URI
     * @throws XMLStreamException
     */
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        fStreamWriter.setPrefix(prefix, uri);
    }

}//XMLEventWriterImpl
