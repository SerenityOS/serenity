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

package com.sun.org.apache.xalan.internal.xsltc.trax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author Sunitha Reddy
 */
public class SAX2StAXEventWriter extends SAX2StAXBaseWriter {

    private XMLEventWriter writer;

    private XMLEventFactory eventFactory;

    private List<Collection<Namespace>> namespaceStack = new ArrayList<>();

    private boolean needToCallStartDocument = false;

    public SAX2StAXEventWriter() {
        eventFactory = XMLEventFactory.newInstance();
    }

    public SAX2StAXEventWriter(XMLEventWriter writer) {
        this.writer = writer;
        eventFactory = XMLEventFactory.newInstance();
    }

    public SAX2StAXEventWriter(XMLEventWriter writer,
            XMLEventFactory factory) {

        this.writer = writer;
        if (factory != null) {
            this.eventFactory = factory;
        } else {
            eventFactory = XMLEventFactory.newInstance();
        }
    }

    public XMLEventWriter getEventWriter() {
        return writer;
    }

    public void setEventWriter(XMLEventWriter writer) {
        this.writer = writer;
    }

    public XMLEventFactory getEventFactory() {
        return eventFactory;
    }

    public void setEventFactory(XMLEventFactory factory) {
        this.eventFactory = factory;
    }

    public void startDocument() throws SAXException {
        super.startDocument();
        namespaceStack.clear();
        eventFactory.setLocation(getCurrentLocation());

        // Encoding and version info will be available only after startElement
        // is called for first time. So, defer START_DOCUMENT event of StAX till
        // that point of time.
        needToCallStartDocument = true;
    }

    void writeStartDocument() throws SAXException {
        super.writeStartDocument();
        try {
            writer.add(eventFactory.createStartDocument(encoding, xmlVersion));
        } catch (XMLStreamException e) {
            throw new SAXException(e);
        }
        needToCallStartDocument = false;
    }

    public void endDocument() throws SAXException {
        eventFactory.setLocation(getCurrentLocation());

        try {
            writer.add(eventFactory.createEndDocument());
        } catch (XMLStreamException e) {
            throw new SAXException(e);
        }

        super.endDocument();

        // clear the namespaces
        namespaceStack.clear();

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (needToCallStartDocument) {
            writeStartDocument();
        }

        // set document location
        eventFactory.setLocation(getCurrentLocation());

        // create attribute and namespace events
        Collection[] events = {null, null};
        createStartEvents(attributes, events);

        namespaceStack.add(events[0]);

        try {
            String[] qname = {null, null};
            parseQName(qName, qname);

            writer.add(eventFactory.createStartElement(qname[0], uri,
                    qname[1], events[1].iterator(), events[0].iterator()));
        } catch (XMLStreamException e) {
            throw new SAXException(e);
        } finally {
            super.startElement(uri, localName, qName, attributes);
        }

    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {

        super.endElement(uri, localName, qName);

        eventFactory.setLocation(getCurrentLocation());

        // parse name
        String[] qname = {null, null};
        parseQName(qName, qname);

        // get namespaces
        Collection<Namespace> nsList = namespaceStack.remove(namespaceStack.size() - 1);
        Iterator<Namespace> nsIter = nsList.iterator();

        try {
            writer.add(eventFactory.createEndElement(qname[0], uri, qname[1],
                    nsIter));
        } catch (XMLStreamException e) {
            throw new SAXException(e);
        }
    }

    public void comment(char[] ch, int start, int length) throws SAXException {
        if (needToCallStartDocument) {
            // Drat. We were trying to postpone this until the first element so that we could get
            // the locator, but we can't output a comment before the start document, so we're just
            // going to have to do without the locator if it hasn't been set yet.
            writeStartDocument();
        }

        super.comment(ch, start, length);

        eventFactory.setLocation(getCurrentLocation());
        try {
            writer.add(eventFactory.createComment(new String(ch, start,
                    length)));
        } catch (XMLStreamException e) {
            throw new SAXException(e);
        }
    }

    public void characters(char[] ch, int start, int length)
            throws SAXException {

        super.characters(ch, start, length);

        try {
            if (!isCDATA) {
                eventFactory.setLocation(getCurrentLocation());
                writer.add(eventFactory.createCharacters(new String(ch,
                        start, length)));
            }

        } catch (XMLStreamException e) {
            throw new SAXException(e);
        }
    }

    public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException {

        super.ignorableWhitespace(ch, start, length);
        characters(ch, start, length);
    }

    public void processingInstruction(String target, String data)
            throws SAXException {

        if (needToCallStartDocument) {
            // Drat. We were trying to postpone this until the first element so that we could get
            // the locator, but we can't output a PI before the start document, so we're just
            // going to have to do without the locator if it hasn't been set yet.
            writeStartDocument();
        }

        super.processingInstruction(target, data);
        try {
            writer.add(eventFactory.createProcessingInstruction(target, data));
        } catch (XMLStreamException e) {
            throw new SAXException(e);
        }
    }

    public void endCDATA() throws SAXException {

        eventFactory.setLocation(getCurrentLocation());
        try {
            writer.add(eventFactory.createCData(CDATABuffer.toString()));
        } catch (XMLStreamException e) {
            throw new SAXException(e);
        }

        super.endCDATA();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void createStartEvents(Attributes attributes, Collection<Attribute>[] events) {

        Map<String, Attribute> nsMap = null;
        List<Attribute> attrs = null;

        // create namespaces
        if (namespaces != null) {
            final int nDecls = namespaces.size();
            for (int i = 0; i < nDecls; i++) {
                final String prefix = namespaces.get(i++);
                String uri = namespaces.get(i);
                Namespace ns = createNamespace(prefix, uri);
                if (nsMap == null) {
                    nsMap = new HashMap<>();
                }
                nsMap.put(prefix, ns);
            }
        }

        // create attributes
        String[] qname = {null, null};
        for (int i = 0, s = attributes.getLength(); i < s; i++) {

            parseQName(attributes.getQName(i), qname);

            String attrPrefix = qname[0];
            String attrLocal = qname[1];

            String attrQName = attributes.getQName(i);
            String attrValue = attributes.getValue(i);
            String attrURI = attributes.getURI(i);

            if ("xmlns".equals(attrQName) || "xmlns".equals(attrPrefix)) {
                // namespace declaration disguised as an attribute. If the
                // namespace has already been declared, skip it, otherwise
                // write it as an namespace
                if (nsMap == null) {
                    nsMap = new HashMap<>();
                }

                if (!nsMap.containsKey(attrLocal)) {
                    Namespace ns = createNamespace(attrLocal, attrValue);
                    nsMap.put(attrLocal, ns);
                }

            } else {
                Attribute attribute;
                if (attrPrefix.length() > 0) {
                    attribute = eventFactory.createAttribute(attrPrefix,
                            attrURI, attrLocal, attrValue);
                } else {
                    attribute = eventFactory.createAttribute(attrLocal,
                            attrValue);
                }

                if (attrs == null) {
                    attrs = new ArrayList<>();
                }
                attrs.add(attribute);
            }
        }

        events[0] = (nsMap == null ? Collections.EMPTY_LIST : nsMap.values());
        events[1] = (attrs == null ? Collections.EMPTY_LIST : attrs);
    }

    protected Namespace createNamespace(String prefix, String uri) {
        if (prefix == null || prefix.length() == 0) {
            return eventFactory.createNamespace(uri);
        } else {
            return eventFactory.createNamespace(prefix, uri);
        }
    }
}
