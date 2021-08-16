/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.impl.xs.traversers;

import com.sun.org.apache.xerces.internal.impl.xs.opti.SchemaDOMParser;
import com.sun.org.apache.xerces.internal.util.JAXPNamespaceContextWrapper;
import com.sun.org.apache.xerces.internal.util.StAXLocationWrapper;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.XMLAttributesImpl;
import com.sun.org.apache.xerces.internal.util.XMLStringBuffer;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.w3c.dom.Document;

/**
 * <p>StAXSchemaParser reads StAX events, converts them into XNI events
 * and passes them directly to the SchemaDOMParser.</p>
 *
 * @xerces.internal
 *
 * @LastModified: Oct 2017
 */
final class StAXSchemaParser {

    /** Chunk size (1024). */
    private static final int CHUNK_SIZE = (1 << 10);

    /** Chunk mask (CHUNK_SIZE - 1). */
    private static final int CHUNK_MASK = CHUNK_SIZE - 1;

    /** Array for holding character data. **/
    private final char [] fCharBuffer = new char[CHUNK_SIZE];

    /** Symbol table **/
    private SymbolTable fSymbolTable;

    /** SchemaDOMParser, events will be delegated to SchemaDOMParser to pass */
    private SchemaDOMParser fSchemaDOMParser;

    /** XML Locator wrapper for SAX. **/
    private final StAXLocationWrapper fLocationWrapper = new StAXLocationWrapper();

    /** The namespace context of this document: stores namespaces in scope */
    private final JAXPNamespaceContextWrapper fNamespaceContext = new JAXPNamespaceContextWrapper(fSymbolTable);

    /** Fields for start element, end element and characters. */
    private final QName fElementQName = new QName();
    private final QName fAttributeQName = new QName();
    private final XMLAttributesImpl fAttributes = new XMLAttributesImpl();
    private final XMLString fTempString = new XMLString();
    private final List<String> fDeclaredPrefixes = new ArrayList<>();
    private final XMLStringBuffer fStringBuffer = new XMLStringBuffer();
    private int fDepth;

    public StAXSchemaParser() {
        fNamespaceContext.setDeclaredPrefixes(fDeclaredPrefixes);
    }

    public void reset(SchemaDOMParser schemaDOMParser, SymbolTable symbolTable) {
        fSchemaDOMParser = schemaDOMParser;
        fSymbolTable = symbolTable;
        fNamespaceContext.setSymbolTable(fSymbolTable);
        fNamespaceContext.reset();
    }

    public Document getDocument() {
        return fSchemaDOMParser.getDocument();
    }

    public void parse(XMLEventReader input) throws XMLStreamException, XNIException {
        XMLEvent currentEvent = input.peek();
        if (currentEvent != null) {
            int eventType = currentEvent.getEventType();
            if (eventType != XMLStreamConstants.START_DOCUMENT &&
                eventType != XMLStreamConstants.START_ELEMENT) {
                throw new XMLStreamException();
            }
            fLocationWrapper.setLocation(currentEvent.getLocation());
            fSchemaDOMParser.startDocument(fLocationWrapper, null, fNamespaceContext, null);
            loop: while (input.hasNext()) {
                currentEvent = input.nextEvent();
                eventType = currentEvent.getEventType();
                switch (eventType) {
                case XMLStreamConstants.START_ELEMENT:
                    ++fDepth;
                    StartElement start = currentEvent.asStartElement();
                    fillQName(fElementQName, start.getName());
                    fLocationWrapper.setLocation(start.getLocation());
                    fNamespaceContext.setNamespaceContext(start.getNamespaceContext());
                    fillXMLAttributes(start);
                    fillDeclaredPrefixes(start);
                    addNamespaceDeclarations();
                    fNamespaceContext.pushContext();
                    fSchemaDOMParser.startElement(fElementQName, fAttributes, null);
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    EndElement end = currentEvent.asEndElement();
                    fillQName(fElementQName, end.getName());
                    fillDeclaredPrefixes(end);
                    fLocationWrapper.setLocation(end.getLocation());
                    fSchemaDOMParser.endElement(fElementQName, null);
                    fNamespaceContext.popContext();
                    --fDepth;
                    if (fDepth <= 0) {
                        break loop;
                    }
                    break;
                case XMLStreamConstants.CHARACTERS:
                    sendCharactersToSchemaParser(currentEvent.asCharacters().getData(), false);
                    break;
                case XMLStreamConstants.SPACE:
                    sendCharactersToSchemaParser(currentEvent.asCharacters().getData(), true);
                    break;
                case XMLStreamConstants.CDATA:
                    fSchemaDOMParser.startCDATA(null);
                    sendCharactersToSchemaParser(currentEvent.asCharacters().getData(), false);
                    fSchemaDOMParser.endCDATA(null);
                    break;
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                    ProcessingInstruction pi = (ProcessingInstruction)currentEvent;
                    fillProcessingInstruction(pi.getData());
                    fSchemaDOMParser.processingInstruction(pi.getTarget(), fTempString, null);
                    break;
                case XMLStreamConstants.DTD:
                    /* There shouldn't be a DTD in the schema */
                    break;
                case XMLStreamConstants.ENTITY_REFERENCE:
                    /* Not needed for schemas */
                    break;
                case XMLStreamConstants.COMMENT:
                    /* No point in sending comments */
                    break;
                case XMLStreamConstants.START_DOCUMENT:
                    fDepth++;
                    /* We automatically call startDocument before the loop */
                    break;
                case XMLStreamConstants.END_DOCUMENT:
                    /* We automatically call endDocument after the loop */
                    break;
                }
            }
            fLocationWrapper.setLocation(null);
            fNamespaceContext.setNamespaceContext(null);
            fSchemaDOMParser.endDocument(null);
        }
    }

    public void parse(XMLStreamReader input) throws XMLStreamException, XNIException {
        if (input.hasNext()) {
            int eventType = input.getEventType();
            if (eventType != XMLStreamConstants.START_DOCUMENT &&
                eventType != XMLStreamConstants.START_ELEMENT) {
                throw new XMLStreamException();
            }
            fLocationWrapper.setLocation(input.getLocation());
            fSchemaDOMParser.startDocument(fLocationWrapper, null, fNamespaceContext, null);
            boolean first = true;
            loop: while (input.hasNext()) {
                if (!first) {
                    eventType = input.next();
                }
                else {
                    first = false;
                }
                switch (eventType) {
                case XMLStreamConstants.START_ELEMENT:
                    ++fDepth;
                    fLocationWrapper.setLocation(input.getLocation());
                    fNamespaceContext.setNamespaceContext(input.getNamespaceContext());
                    fillQName(fElementQName, input.getNamespaceURI(),
                        input.getLocalName(), input.getPrefix());
                    fillXMLAttributes(input);
                    fillDeclaredPrefixes(input);
                    addNamespaceDeclarations();
                    fNamespaceContext.pushContext();
                    fSchemaDOMParser.startElement(fElementQName, fAttributes, null);
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    fLocationWrapper.setLocation(input.getLocation());
                    fNamespaceContext.setNamespaceContext(input.getNamespaceContext());
                    fillQName(fElementQName, input.getNamespaceURI(),
                        input.getLocalName(), input.getPrefix());
                    fillDeclaredPrefixes(input);
                    fSchemaDOMParser.endElement(fElementQName, null);
                    fNamespaceContext.popContext();
                    --fDepth;
                    if (fDepth <= 0) {
                        break loop;
                    }
                    break;
                case XMLStreamConstants.CHARACTERS:
                    fTempString.setValues(input.getTextCharacters(),
                        input.getTextStart(), input.getTextLength());
                    fSchemaDOMParser.characters(fTempString, null);
                    break;
                case XMLStreamConstants.SPACE:
                    fTempString.setValues(input.getTextCharacters(),
                        input.getTextStart(), input.getTextLength());
                    fSchemaDOMParser.ignorableWhitespace(fTempString, null);
                    break;
                case XMLStreamConstants.CDATA:
                    fSchemaDOMParser.startCDATA(null);
                    fTempString.setValues(input.getTextCharacters(),
                        input.getTextStart(), input.getTextLength());
                    fSchemaDOMParser.characters(fTempString, null);
                    fSchemaDOMParser.endCDATA(null);
                    break;
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                    fillProcessingInstruction(input.getPIData());
                    fSchemaDOMParser.processingInstruction(input.getPITarget(), fTempString, null);
                    break;
                case XMLStreamConstants.DTD:
                    /* There shouldn't be a DTD in the schema */
                    break;
                case XMLStreamConstants.ENTITY_REFERENCE:
                    /* Not needed for schemas */
                    break;
                case XMLStreamConstants.COMMENT:
                    /* No point in sending comments */
                    break;
                case XMLStreamConstants.START_DOCUMENT:
                    ++fDepth;
                    /* We automatically call startDocument before the loop */
                    break;
                case XMLStreamConstants.END_DOCUMENT:
                    /* We automatically call endDocument after the loop */
                    break;
                }
            }
            fLocationWrapper.setLocation(null);
            fNamespaceContext.setNamespaceContext(null);
            fSchemaDOMParser.endDocument(null);
        }
    }

    /** Send characters to the validator in CHUNK_SIZE character chunks. */
    private void sendCharactersToSchemaParser(String str, boolean whitespace) {
        if (str != null) {
            final int length = str.length();
            final int remainder = length & CHUNK_MASK;
            if (remainder > 0) {
                str.getChars(0, remainder, fCharBuffer, 0);
                fTempString.setValues(fCharBuffer, 0, remainder);
                if (whitespace) {
                    fSchemaDOMParser.ignorableWhitespace(fTempString, null);
                }
                else {
                    fSchemaDOMParser.characters(fTempString, null);
                }
            }
            int i = remainder;
            while (i < length) {
                str.getChars(i, i += CHUNK_SIZE, fCharBuffer, 0);
                fTempString.setValues(fCharBuffer, 0, CHUNK_SIZE);
                if (whitespace) {
                    fSchemaDOMParser.ignorableWhitespace(fTempString, null);
                }
                else {
                    fSchemaDOMParser.characters(fTempString, null);
                }
            }
        }
    }

    // processing instructions must be sent all in one chunk
    private void fillProcessingInstruction(String data) {
        final int dataLength = data.length();
        char [] charBuffer = fCharBuffer;
        if (charBuffer.length < dataLength) {
            // toCharArray() creates a newly allocated array, so it's okay
            // to keep a reference to it.
            charBuffer = data.toCharArray();
        }
        else {
            data.getChars(0, dataLength, charBuffer, 0);
        }
        fTempString.setValues(charBuffer, 0, dataLength);
    }

    private void fillXMLAttributes(StartElement event) {
        fAttributes.removeAllAttributes();
        final Iterator<Attribute> attrs = event.getAttributes();
        while (attrs.hasNext()) {
            Attribute attr = attrs.next();
            fillQName(fAttributeQName, attr.getName());
            String type = attr.getDTDType();
            int idx = fAttributes.getLength();
            fAttributes.addAttributeNS(fAttributeQName,
                    (type != null) ? type : XMLSymbols.fCDATASymbol, attr.getValue());
            fAttributes.setSpecified(idx, attr.isSpecified());
        }
    }

    private void fillXMLAttributes(XMLStreamReader input) {
        fAttributes.removeAllAttributes();
        final int len = input.getAttributeCount();
        for (int i = 0; i < len; ++i) {
            fillQName(fAttributeQName, input.getAttributeNamespace(i),
                input.getAttributeLocalName(i), input.getAttributePrefix(i));
            String type = input.getAttributeType(i);
            fAttributes.addAttributeNS(fAttributeQName,
                    (type != null) ? type : XMLSymbols.fCDATASymbol, input.getAttributeValue(i));
            fAttributes.setSpecified(i, input.isAttributeSpecified(i));
        }
    }

    private void addNamespaceDeclarations() {
        String prefix = null;
        String localpart = null;
        String rawname = null;
        String nsPrefix = null;
        String nsURI = null;

        final Iterator<String> iter = fDeclaredPrefixes.iterator();
        while (iter.hasNext()) {
            nsPrefix = iter.next();
            nsURI = fNamespaceContext.getURI(nsPrefix);
            if (nsPrefix.length() > 0) {
                prefix = XMLSymbols.PREFIX_XMLNS;
                localpart = nsPrefix;
                fStringBuffer.clear();
                fStringBuffer.append(prefix);
                fStringBuffer.append(':');
                fStringBuffer.append(localpart);
                rawname = fSymbolTable.addSymbol(fStringBuffer.ch, fStringBuffer.offset, fStringBuffer.length);
            }
            else {
                prefix = XMLSymbols.EMPTY_STRING;
                localpart = XMLSymbols.PREFIX_XMLNS;
                rawname = XMLSymbols.PREFIX_XMLNS;
            }
            fAttributeQName.setValues(prefix, localpart, rawname, NamespaceContext.XMLNS_URI);
            fAttributes.addAttribute(fAttributeQName, XMLSymbols.fCDATASymbol,
                    (nsURI != null) ? nsURI : XMLSymbols.EMPTY_STRING);
        }
    }

    /** Fills in the list of declared prefixes. */
    private void fillDeclaredPrefixes(StartElement event) {
        fillDeclaredPrefixes(event.getNamespaces());
    }

    /** Fills in the list of declared prefixes. */
    private void fillDeclaredPrefixes(EndElement event) {
        fillDeclaredPrefixes(event.getNamespaces());
    }

    /** Fills in the list of declared prefixes. */
    private void fillDeclaredPrefixes(Iterator<Namespace> namespaces) {
        fDeclaredPrefixes.clear();
        while (namespaces.hasNext()) {
            Namespace ns = namespaces.next();
            String prefix = ns.getPrefix();
            fDeclaredPrefixes.add(prefix != null ? prefix : "");
        }
    }

    /** Fills in the list of declared prefixes. */
    private void fillDeclaredPrefixes(XMLStreamReader reader) {
        fDeclaredPrefixes.clear();
        final int len = reader.getNamespaceCount();
        for (int i = 0; i < len; ++i) {
            String prefix = reader.getNamespacePrefix(i);
            fDeclaredPrefixes.add(prefix != null ? prefix : "");
        }
    }

    /** Fills in a QName object. */
    private void fillQName(QName toFill, javax.xml.namespace.QName toCopy) {
        fillQName(toFill, toCopy.getNamespaceURI(), toCopy.getLocalPart(), toCopy.getPrefix());
    }

    /** Fills in a QName object. */
    final void fillQName(QName toFill, String uri, String localpart, String prefix) {
        uri = (uri != null && uri.length() > 0) ? fSymbolTable.addSymbol(uri) : null;
        localpart = (localpart != null) ? fSymbolTable.addSymbol(localpart) : XMLSymbols.EMPTY_STRING;
        prefix = (prefix != null && prefix.length() > 0) ? fSymbolTable.addSymbol(prefix) : XMLSymbols.EMPTY_STRING;
        String raw = localpart;
        if (prefix != XMLSymbols.EMPTY_STRING) {
            fStringBuffer.clear();
            fStringBuffer.append(prefix);
            fStringBuffer.append(':');
            fStringBuffer.append(localpart);
            raw = fSymbolTable.addSymbol(fStringBuffer.ch, fStringBuffer.offset, fStringBuffer.length);
        }
        toFill.setValues(prefix, localpart, raw, uri);
    }

} // StAXSchemaParser
