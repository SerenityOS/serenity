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

package com.sun.org.apache.xerces.internal.util;

import com.sun.org.apache.xerces.internal.impl.xs.util.SimpleLocator;
import com.sun.org.apache.xerces.internal.jaxp.validation.WrappedSAXException;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XMLDocumentHandler;
import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentSource;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Receves SAX {@link ContentHandler} events
 * and produces the equivalent {@link XMLDocumentHandler} events.
 *
 * @author
 *     Kohsuke Kawaguchi
 */
public class SAX2XNI implements ContentHandler, XMLDocumentSource {
    public SAX2XNI( XMLDocumentHandler core ) {
        this.fCore = core;
    }

    private XMLDocumentHandler fCore;

    private final NamespaceSupport nsContext = new NamespaceSupport();
    private final SymbolTable symbolTable = new SymbolTable();


    public void setDocumentHandler(XMLDocumentHandler handler) {
        fCore = handler;
    }

    public XMLDocumentHandler getDocumentHandler() {
        return fCore;
    }


    //
    //
    // ContentHandler implementation
    //
    //
    public void startDocument() throws SAXException {
        try {
            nsContext.reset();

            XMLLocator xmlLocator;
            if(locator==null)
                // some SAX source doesn't provide a locator,
                // in which case we assume no line information is available
                // and use a dummy locator. With this, downstream components
                // can always assume that they will get a non-null Locator.
                xmlLocator=new SimpleLocator(null,null,-1,-1);
            else
                xmlLocator=new LocatorWrapper(locator);

            fCore.startDocument(
                    xmlLocator,
                    null,
                    nsContext,
                    null);
        } catch( WrappedSAXException e ) {
            throw e.exception;
        }
    }

    public void endDocument() throws SAXException {
        try {
            fCore.endDocument(null);
        } catch( WrappedSAXException e ) {
            throw e.exception;
        }
    }

    public void startElement( String uri, String local, String qname, Attributes att ) throws SAXException {
        try {
            fCore.startElement(createQName(uri,local,qname),createAttributes(att),null);
        } catch( WrappedSAXException e ) {
            throw e.exception;
        }
    }

    public void endElement( String uri, String local, String qname ) throws SAXException {
        try {
            fCore.endElement(createQName(uri,local,qname),null);
        } catch( WrappedSAXException e ) {
            throw e.exception;
        }
    }

    public void characters( char[] buf, int offset, int len ) throws SAXException {
        try {
            fCore.characters(new XMLString(buf,offset,len),null);
        } catch( WrappedSAXException e ) {
            throw e.exception;
        }
    }

    public void ignorableWhitespace( char[] buf, int offset, int len ) throws SAXException {
        try {
            fCore.ignorableWhitespace(new XMLString(buf,offset,len),null);
        } catch( WrappedSAXException e ) {
            throw e.exception;
        }
    }

    public void startPrefixMapping( String prefix, String uri ) {
        nsContext.pushContext();
        nsContext.declarePrefix(prefix,uri);
    }

    public void endPrefixMapping( String prefix ) {
        nsContext.popContext();
    }

    public void processingInstruction( String target, String data ) throws SAXException {
        try {
            fCore.processingInstruction(
                    symbolize(target),createXMLString(data),null);
        } catch( WrappedSAXException e ) {
            throw e.exception;
        }
    }

    public void skippedEntity( String name ) {
    }

    private Locator locator;
    public void setDocumentLocator( Locator _loc ) {
        this.locator = _loc;
    }

    /** Creates a QName object. */
    private QName createQName(String uri, String local, String raw) {

        int idx = raw.indexOf(':');

        if( local.length()==0 ) {
            // if naemspace processing is turned off, local could be "".
            // in that case, treat everything to be in the no namespace.
            uri = "";
            if(idx<0)
                local = raw;
            else
                local = raw.substring(idx+1);
        }

        String prefix;
        if (idx < 0)
            prefix = null;
        else
            prefix = raw.substring(0, idx);

        if (uri != null && uri.length() == 0)
            uri = null; // XNI uses null whereas SAX uses the empty string

        return new QName(symbolize(prefix), symbolize(local), symbolize(raw), symbolize(uri));
    }

    /** Symbolizes the specified string. */
    private String symbolize(String s) {
        if (s == null)
            return null;
        else
            return symbolTable.addSymbol(s);
    }

    private XMLString createXMLString(String str) {
        // with my patch
        // return new XMLString(str);

        // for now
        return new XMLString(str.toCharArray(), 0, str.length());
    }


    /** only one instance of XMLAttributes is used. */
    private final XMLAttributes xa = new XMLAttributesImpl();

    /** Creates an XMLAttributes object. */
    private XMLAttributes createAttributes(Attributes att) {
        xa.removeAllAttributes();
        int len = att.getLength();
        for (int i = 0; i < len; i++)
            xa.addAttribute(
                    createQName(att.getURI(i), att.getLocalName(i), att.getQName(i)),
                    att.getType(i),
                    att.getValue(i));
        return xa;
    }
}
