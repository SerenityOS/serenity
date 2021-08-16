/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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
import com.sun.org.apache.xerces.internal.util.NamespaceSupport;
import com.sun.org.apache.xerces.internal.util.SAXLocatorWrapper;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.XMLAttributesImpl;
import com.sun.org.apache.xerces.internal.util.XMLStringBuffer;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.LocatorImpl;

/**
 * <p>SchemaContentHandler converts SAX events into XNI
 * and passes them directly to the SchemaDOMParser.</p>
 *
 * @xerces.internal
 *
 * @author Michael Glavassevich, IBM
 * @author Jack Z. Wang, IBM
 *
 */
final class SchemaContentHandler implements ContentHandler {

    /** Symbol table **/
    private SymbolTable fSymbolTable;

    /** SchemaDOMParser, events will be delegated to SchemaDOMParser to pass */
    private SchemaDOMParser fSchemaDOMParser;

    /** XML Locator wrapper for SAX. **/
    private final SAXLocatorWrapper fSAXLocatorWrapper = new SAXLocatorWrapper();

    /** The namespace context of this document: stores namespaces in scope */
    private NamespaceSupport fNamespaceContext = new NamespaceSupport();

    /** Indicate if push NamespaceContest is needed */
    private boolean fNeedPushNSContext;

    /** Flag used to track whether namespace declarations are reported as attributes. */
    private boolean fNamespacePrefixes = false;

    /** Flag used to track whether XML names and Namespace URIs have been internalized. */
    private boolean fStringsInternalized = false;

    /** Fields for start element, end element and characters. */
    private final QName fElementQName = new QName();
    private final QName fAttributeQName = new QName();
    private final XMLAttributesImpl fAttributes = new XMLAttributesImpl();
    private final XMLString fTempString = new XMLString();
    private final XMLStringBuffer fStringBuffer = new XMLStringBuffer();

    /**
     * <p>Constructs an SchemaContentHandler.</p>
     */
    public SchemaContentHandler() {}

    /*
     * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
     */
    public Document getDocument() {
        return fSchemaDOMParser.getDocument();
    }

    /*
     * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
     */
    public void setDocumentLocator(Locator locator) {
        fSAXLocatorWrapper.setLocator(locator);
    }

    /*
     * @see org.xml.sax.ContentHandler#startDocument()
     */
    public void startDocument() throws SAXException {
        fNeedPushNSContext = true;
        fNamespaceContext.reset();
        try {
            fSchemaDOMParser.startDocument(fSAXLocatorWrapper, null, fNamespaceContext, null);
        }
        catch (XMLParseException e) {
            convertToSAXParseException(e);
        }
        catch (XNIException e) {
            convertToSAXException(e);
        }
    }

    /*
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    public void endDocument() throws SAXException {
        fSAXLocatorWrapper.setLocator(null);
        try {
            fSchemaDOMParser.endDocument(null);
        }
        catch (XMLParseException e) {
            convertToSAXParseException(e);
        }
        catch (XNIException e) {
            convertToSAXException(e);
        }
    }

    /*
     * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
     */
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (fNeedPushNSContext) {
            fNeedPushNSContext = false;
            fNamespaceContext.pushContext();
        }
        if (!fStringsInternalized) {
            prefix = (prefix != null) ? fSymbolTable.addSymbol(prefix) : XMLSymbols.EMPTY_STRING;
            uri = (uri != null && uri.length() > 0) ? fSymbolTable.addSymbol(uri) : null;
        }
        else {
            if (prefix == null) {
                prefix = XMLSymbols.EMPTY_STRING;
            }
            if (uri != null && uri.length() == 0) {
                uri = null;
            }
        }
        fNamespaceContext.declarePrefix(prefix, uri);
    }

    /*
     * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
     */
    public void endPrefixMapping(String prefix) throws SAXException {
        // do nothing
    }

    /*
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (fNeedPushNSContext) {
            fNamespaceContext.pushContext();
        }
        fNeedPushNSContext = true;

        // Fill element QName and XMLAttributes
        fillQName(fElementQName, uri, localName, qName);
        fillXMLAttributes(atts);

        // Add namespace declarations if necessary
        if (!fNamespacePrefixes) {
            final int prefixCount = fNamespaceContext.getDeclaredPrefixCount();
            if (prefixCount > 0) {
                addNamespaceDeclarations(prefixCount);
            }
        }

        try {
            fSchemaDOMParser.startElement(fElementQName, fAttributes, null);
        }
        catch (XMLParseException e) {
            convertToSAXParseException(e);
        }
        catch (XNIException e) {
            convertToSAXException(e);
        }
    }

    /*
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    public void endElement(String uri, String localName, String qName) throws SAXException {
        fillQName(fElementQName, uri, localName, qName);
        try {
            fSchemaDOMParser.endElement(fElementQName, null);
        }
        catch (XMLParseException e) {
            convertToSAXParseException(e);
        }
        catch (XNIException e) {
            convertToSAXException(e);
        }
        finally {
            fNamespaceContext.popContext();
        }
    }

    /*
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
        try {
            fTempString.setValues(ch, start, length);
            fSchemaDOMParser.characters(fTempString, null);
        }
        catch (XMLParseException e) {
            convertToSAXParseException(e);
        }
        catch (XNIException e) {
            convertToSAXException(e);
        }
    }

    /*
     * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
     */
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        try {
            fTempString.setValues(ch, start, length);
            fSchemaDOMParser.ignorableWhitespace(fTempString, null);
        }
        catch (XMLParseException e) {
            convertToSAXParseException(e);
        }
        catch (XNIException e) {
            convertToSAXException(e);
        }
    }

    /*
     * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
     */
    public void processingInstruction(String target, String data) throws SAXException {
        try {
            fTempString.setValues(data.toCharArray(), 0, data.length());
            fSchemaDOMParser.processingInstruction(target, fTempString, null);
        }
        catch (XMLParseException e) {
            convertToSAXParseException(e);
        }
        catch (XNIException e) {
            convertToSAXException(e);
        }
    }

    /*
     * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
     */
    public void skippedEntity(String arg) throws SAXException {
        // do-nothing
    }

    /*
     * Other methods
     */

    private void fillQName(QName toFill, String uri, String localpart, String rawname) {
        if (!fStringsInternalized) {
            uri = (uri != null && uri.length() > 0) ? fSymbolTable.addSymbol(uri) : null;
            localpart = (localpart != null) ? fSymbolTable.addSymbol(localpart) : XMLSymbols.EMPTY_STRING;
            rawname = (rawname != null) ? fSymbolTable.addSymbol(rawname) : XMLSymbols.EMPTY_STRING;
        }
        else {
            if (uri != null && uri.length() == 0) {
                uri = null;
            }
            if (localpart == null) {
                localpart = XMLSymbols.EMPTY_STRING;
            }
            if (rawname == null) {
                rawname = XMLSymbols.EMPTY_STRING;
            }
        }
        String prefix = XMLSymbols.EMPTY_STRING;
        int prefixIdx = rawname.indexOf(':');
        if (prefixIdx != -1) {
            prefix = fSymbolTable.addSymbol(rawname.substring(0, prefixIdx));
            // local part may be an empty string if this is a namespace declaration
            if (localpart == XMLSymbols.EMPTY_STRING) {
                localpart = fSymbolTable.addSymbol(rawname.substring(prefixIdx + 1));
            }
        }
        // local part may be an empty string if this is a namespace declaration
        else if (localpart == XMLSymbols.EMPTY_STRING) {
            localpart = rawname;
        }
        toFill.setValues(prefix, localpart, rawname, uri);
    }

    private void fillXMLAttributes(Attributes atts) {
        fAttributes.removeAllAttributes();
        final int attrCount = atts.getLength();
        for (int i = 0; i < attrCount; ++i) {
            fillQName(fAttributeQName, atts.getURI(i), atts.getLocalName(i), atts.getQName(i));
            String type = atts.getType(i);
            fAttributes.addAttributeNS(fAttributeQName, (type != null) ? type : XMLSymbols.fCDATASymbol, atts.getValue(i));
            fAttributes.setSpecified(i, true);
        }
    }

    private void addNamespaceDeclarations(final int prefixCount) {
        String prefix = null;
        String localpart = null;
        String rawname = null;
        String nsPrefix = null;
        String nsURI = null;
        for (int i = 0; i < prefixCount; ++i) {
            nsPrefix = fNamespaceContext.getDeclaredPrefixAt(i);
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

    public void reset(SchemaDOMParser schemaDOMParser, SymbolTable symbolTable,
            boolean namespacePrefixes, boolean stringsInternalized) {
        fSchemaDOMParser = schemaDOMParser;
        fSymbolTable = symbolTable;
        fNamespacePrefixes = namespacePrefixes;
        fStringsInternalized = stringsInternalized;
    }

    /*
     * Static methods
     */

    static void convertToSAXParseException(XMLParseException e) throws SAXException {
        Exception ex = e.getException();
        if (ex == null) {
            // must be a parser exception; mine it for locator info and throw
            // a SAXParseException
            LocatorImpl locatorImpl = new LocatorImpl();
            locatorImpl.setPublicId(e.getPublicId());
            locatorImpl.setSystemId(e.getExpandedSystemId());
            locatorImpl.setLineNumber(e.getLineNumber());
            locatorImpl.setColumnNumber(e.getColumnNumber());
            throw new SAXParseException(e.getMessage(), locatorImpl);
        }
        if (ex instanceof SAXException) {
            // why did we create an XMLParseException?
            throw (SAXException) ex;
        }
        throw new SAXException(ex);
    }

    static void convertToSAXException(XNIException e) throws SAXException {
        Exception ex = e.getException();
        if (ex == null) {
            throw new SAXException(e.getMessage());
        }
        if (ex instanceof SAXException) {
            throw (SAXException) ex;
        }
        throw new SAXException(ex);
    }

} // SchemaContentHandler
