/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.xml.internal.stream.events;

import com.sun.org.apache.xerces.internal.impl.PropertyManager;
import java.util.List;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import com.sun.org.apache.xerces.internal.util.NamespaceContextWrapper;
import com.sun.org.apache.xerces.internal.util.NamespaceSupport;
import javax.xml.stream.util.XMLEventConsumer;

/**
 * Implementation of XMLEvent Allocator.
 *
 * @author Neeraj bajaj, k venugopal
 */
public class XMLEventAllocatorImpl implements XMLEventAllocator {

    /**
     * Creates a new instance of XMLEventAllocator
     */
    public XMLEventAllocatorImpl() {
    }

    public XMLEvent allocate(XMLStreamReader xMLStreamReader) throws XMLStreamException {
        if (xMLStreamReader == null) {
            throw new XMLStreamException("Reader cannot be null");
        }
        //        allocate is not supposed to change the state of the reader so we shouldn't be calling next.
        //        return getNextEvent(xMLStreamReader);
        return getXMLEvent(xMLStreamReader);
    }

    public void allocate(XMLStreamReader xMLStreamReader, XMLEventConsumer xMLEventConsumer)
            throws XMLStreamException {
        XMLEvent currentEvent = getXMLEvent(xMLStreamReader);
        if (currentEvent != null) {
            xMLEventConsumer.add(currentEvent);
        }

        return;
    }

    public javax.xml.stream.util.XMLEventAllocator newInstance() {
        return new XMLEventAllocatorImpl();
    }

    //REVISIT: shouldn't we be using XMLEventFactory to create events.
    XMLEvent getXMLEvent(XMLStreamReader streamReader) {
        XMLEvent event = null;
        //returns the current event
        int eventType = streamReader.getEventType();
        switch (eventType) {

            case XMLEvent.START_ELEMENT: {
                StartElementEvent startElementEvent = new StartElementEvent(getQName(streamReader));
                fillAttributes(startElementEvent, streamReader);
                //we might have different XMLStreamReader so check every time for
                //the namespace aware property. we should be setting namespace
                //related values only when isNamespaceAware is 'true'
                if (((Boolean) streamReader.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE))) {
                    fillNamespaceAttributes(startElementEvent, streamReader);
                    setNamespaceContext(startElementEvent, streamReader);
                }

                startElementEvent.setLocation(streamReader.getLocation());
                event = startElementEvent;
                break;
            }
            case XMLEvent.END_ELEMENT: {
                EndElementEvent endElementEvent = new EndElementEvent(getQName(streamReader));
                endElementEvent.setLocation(streamReader.getLocation());

                if (((Boolean) streamReader.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE))) {
                    fillNamespaceAttributes(endElementEvent, streamReader);
                }
                event = endElementEvent;
                break;
            }
            case XMLEvent.PROCESSING_INSTRUCTION: {
                ProcessingInstructionEvent piEvent = new ProcessingInstructionEvent(
                        streamReader.getPITarget(), streamReader.getPIData());
                piEvent.setLocation(streamReader.getLocation());
                event = piEvent;
                break;
            }
            case XMLEvent.CHARACTERS: {
                CharacterEvent cDataEvent = new CharacterEvent(streamReader.getText());
                cDataEvent.setLocation(streamReader.getLocation());
                event = cDataEvent;
                break;
            }
            case XMLEvent.COMMENT: {
                CommentEvent commentEvent = new CommentEvent(streamReader.getText());
                commentEvent.setLocation(streamReader.getLocation());
                event = commentEvent;
                break;
            }
            case XMLEvent.START_DOCUMENT: {
                StartDocumentEvent sdEvent = new StartDocumentEvent();
                sdEvent.setVersion(streamReader.getVersion());
                sdEvent.setEncoding(streamReader.getEncoding());
                if (streamReader.getCharacterEncodingScheme() != null) {
                    sdEvent.setDeclaredEncoding(true);
                } else {
                    sdEvent.setDeclaredEncoding(false);
                }
                sdEvent.setStandalone(streamReader.isStandalone(), streamReader.standaloneSet());
                sdEvent.setLocation(streamReader.getLocation());
                event = sdEvent;
                break;
            }
            case XMLEvent.END_DOCUMENT: {
                EndDocumentEvent endDocumentEvent = new EndDocumentEvent();
                endDocumentEvent.setLocation(streamReader.getLocation());
                event = endDocumentEvent;
                break;
            }
            case XMLEvent.ENTITY_REFERENCE: {
                EntityReferenceEvent entityEvent = new EntityReferenceEvent(streamReader.getLocalName(),
                        new EntityDeclarationImpl(streamReader.getLocalName(), streamReader.getText()));
                entityEvent.setLocation(streamReader.getLocation());
                event = entityEvent;
                break;

            }
            case XMLEvent.ATTRIBUTE: {
                event = null;
                break;
            }
            case XMLEvent.DTD: {
                DTDEvent dtdEvent = new DTDEvent(streamReader.getText());
                dtdEvent.setLocation(streamReader.getLocation());
                @SuppressWarnings("unchecked")
                List<EntityDeclaration> entities = (List<EntityDeclaration>)
                        streamReader.getProperty(PropertyManager.STAX_ENTITIES);
                if (entities != null && entities.size() != 0) {
                    dtdEvent.setEntities(entities);
                }
                @SuppressWarnings("unchecked")
                List<NotationDeclaration> notations = (List<NotationDeclaration>)
                        streamReader.getProperty(PropertyManager.STAX_NOTATIONS);
                if (notations != null && !notations.isEmpty()) {
                    dtdEvent.setNotations(notations);
                }
                event = dtdEvent;
                break;
            }
            case XMLEvent.CDATA: {
                CharacterEvent cDataEvent = new CharacterEvent(streamReader.getText(), true);
                cDataEvent.setLocation(streamReader.getLocation());
                event = cDataEvent;
                break;
            }
            case XMLEvent.SPACE: {
                CharacterEvent spaceEvent = new CharacterEvent(streamReader.getText(), false, true);
                spaceEvent.setLocation(streamReader.getLocation());
                event = spaceEvent;
                break;
            }
        }
        return event;
    }

    //this function is not used..
    protected XMLEvent getNextEvent(XMLStreamReader streamReader) throws XMLStreamException {
        //advance the reader to next event.
        streamReader.next();
        return getXMLEvent(streamReader);
    }

    protected void fillAttributes(StartElementEvent event, XMLStreamReader xmlr) {

        int len = xmlr.getAttributeCount();
        QName qname = null;
        AttributeImpl attr = null;
        NamespaceImpl nattr = null;
        for (int i = 0; i < len; i++) {
            qname = xmlr.getAttributeName(i);
            //this method doesn't include namespace declarations
            //so we can be sure that there wont be any namespace declaration as part of this function call
            //we can avoid this check - nb.
            /**
             * prefix = qname.getPrefix(); localpart = qname.getLocalPart(); if
             * (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE) ) { attr = new
             * NamespaceImpl(localpart,xmlr.getAttributeValue(i)); }else if
             * (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)){ attr = new
             * NamespaceImpl(xmlr.getAttributeValue(i)); }else{ attr = new
             * AttributeImpl(); attr.setName(qname); }
             *
             */
            attr = new AttributeImpl();
            attr.setName(qname);
            attr.setAttributeType(xmlr.getAttributeType(i));
            attr.setSpecified(xmlr.isAttributeSpecified(i));
            attr.setValue(xmlr.getAttributeValue(i));
            event.addAttribute(attr);
        }
    }

    protected void fillNamespaceAttributes(StartElementEvent event, XMLStreamReader xmlr) {
        int count = xmlr.getNamespaceCount();
        String uri = null;
        String prefix = null;
        NamespaceImpl attr = null;
        for (int i = 0; i < count; i++) {
            uri = xmlr.getNamespaceURI(i);
            prefix = xmlr.getNamespacePrefix(i);
            if (prefix == null) {
                prefix = XMLConstants.DEFAULT_NS_PREFIX;
            }
            attr = new NamespaceImpl(prefix, uri);
            event.addNamespaceAttribute(attr);
        }
    }

    protected void fillNamespaceAttributes(EndElementEvent event, XMLStreamReader xmlr) {
        int count = xmlr.getNamespaceCount();
        String uri = null;
        String prefix = null;
        NamespaceImpl attr = null;
        for (int i = 0; i < count; i++) {
            uri = xmlr.getNamespaceURI(i);
            prefix = xmlr.getNamespacePrefix(i);
            if (prefix == null) {
                prefix = XMLConstants.DEFAULT_NS_PREFIX;
            }
            attr = new NamespaceImpl(prefix, uri);
            event.addNamespace(attr);
        }
    }

    //Revisit : Creating a new Namespacecontext for now.
    //see if we can do better job.
    private void setNamespaceContext(StartElementEvent event, XMLStreamReader xmlr) {
        NamespaceContextWrapper contextWrapper = (NamespaceContextWrapper) xmlr.getNamespaceContext();
        NamespaceSupport ns = new NamespaceSupport(contextWrapper.getNamespaceContext());
        event.setNamespaceContext(new NamespaceContextWrapper(ns));
    }

    private QName getQName(XMLStreamReader xmlr) {
        return new QName(xmlr.getNamespaceURI(), xmlr.getLocalName(),
                xmlr.getPrefix());
    }
}
