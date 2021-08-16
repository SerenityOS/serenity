/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.xml.sax.helpers;

import java.io.IOException;
import java.util.Locale;

import org.xml.sax.Parser;      // deprecated
import org.xml.sax.Locator;
import org.xml.sax.InputSource;
import org.xml.sax.AttributeList; // deprecated
import org.xml.sax.EntityResolver;
import org.xml.sax.DTDHandler;
import org.xml.sax.DocumentHandler; // deprecated
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXNotSupportedException;


/**
 * Adapt a SAX2 XMLReader as a SAX1 Parser.
 *
 * <p>This class wraps a SAX2 {@link org.xml.sax.XMLReader XMLReader}
 * and makes it act as a SAX1 {@link org.xml.sax.Parser Parser}.  The XMLReader
 * must support a true value for the
 * http://xml.org/sax/features/namespace-prefixes property or parsing will fail
 * with a {@link org.xml.sax.SAXException SAXException}; if the XMLReader
 * supports a false value for the http://xml.org/sax/features/namespaces
 * property, that will also be used to improve efficiency.</p>
 *
 * @since 1.4, SAX 2.0
 * @author David Megginson
 * @see org.xml.sax.Parser
 * @see org.xml.sax.XMLReader
 */
@SuppressWarnings("deprecation")
public class XMLReaderAdapter implements Parser, ContentHandler
{


    ////////////////////////////////////////////////////////////////////
    // Constructor.
    ////////////////////////////////////////////////////////////////////


    /**
     * Create a new adapter.
     *
     * <p>Use the "org.xml.sax.driver" property to locate the SAX2
     * driver to embed.</p>
     *
     * @throws org.xml.sax.SAXException If the embedded driver
     *            cannot be instantiated or if the
     *            org.xml.sax.driver property is not specified.
     */
    public XMLReaderAdapter ()
      throws SAXException
    {
        setup(XMLReaderFactory.createXMLReader());
    }


    /**
     * Create a new adapter.
     *
     * <p>Create a new adapter, wrapped around a SAX2 XMLReader.
     * The adapter will make the XMLReader act like a SAX1
     * Parser.</p>
     *
     * @param xmlReader The SAX2 XMLReader to wrap.
     * @throws java.lang.NullPointerException If the argument is null.
     */
    public XMLReaderAdapter (XMLReader xmlReader)
    {
        setup(xmlReader);
    }



    /**
     * Internal setup.
     *
     * @param xmlReader The embedded XMLReader.
     */
    private void setup (XMLReader xmlReader)
    {
        if (xmlReader == null) {
            throw new NullPointerException("XMLReader must not be null");
        }
        this.xmlReader = xmlReader;
        qAtts = new AttributesAdapter();
    }



    ////////////////////////////////////////////////////////////////////
    // Implementation of org.xml.sax.Parser.
    ////////////////////////////////////////////////////////////////////


    /**
     * Set the locale for error reporting.
     *
     * <p>This is not supported in SAX2, and will always throw
     * an exception.</p>
     *
     * @param locale the locale for error reporting.
     * @see org.xml.sax.Parser#setLocale
     * @throws org.xml.sax.SAXException Thrown unless overridden.
     */
    public void setLocale (Locale locale)
        throws SAXException
    {
        throw new SAXNotSupportedException("setLocale not supported");
    }


    /**
     * Register the entity resolver.
     *
     * @param resolver The new resolver.
     * @see org.xml.sax.Parser#setEntityResolver
     */
    public void setEntityResolver (EntityResolver resolver)
    {
        xmlReader.setEntityResolver(resolver);
    }


    /**
     * Register the DTD event handler.
     *
     * @param handler The new DTD event handler.
     * @see org.xml.sax.Parser#setDTDHandler
     */
    public void setDTDHandler (DTDHandler handler)
    {
        xmlReader.setDTDHandler(handler);
    }


    /**
     * Register the SAX1 document event handler.
     *
     * <p>Note that the SAX1 document handler has no Namespace
     * support.</p>
     *
     * @param handler The new SAX1 document event handler.
     * @see org.xml.sax.Parser#setDocumentHandler
     */
    public void setDocumentHandler (DocumentHandler handler)
    {
        documentHandler = handler;
    }


    /**
     * Register the error event handler.
     *
     * @param handler The new error event handler.
     * @see org.xml.sax.Parser#setErrorHandler
     */
    public void setErrorHandler (ErrorHandler handler)
    {
        xmlReader.setErrorHandler(handler);
    }


    /**
     * Parse the document.
     *
     * <p>This method will throw an exception if the embedded
     * XMLReader does not support the
     * http://xml.org/sax/features/namespace-prefixes property.</p>
     *
     * @param systemId The absolute URL of the document.
     * @throws java.io.IOException If there is a problem reading
     *            the raw content of the document.
     * @throws org.xml.sax.SAXException If there is a problem
     *            processing the document.
     * @see #parse(org.xml.sax.InputSource)
     * @see org.xml.sax.Parser#parse(java.lang.String)
     */
    public void parse (String systemId)
        throws IOException, SAXException
    {
        parse(new InputSource(systemId));
    }


    /**
     * Parse the document.
     *
     * <p>This method will throw an exception if the embedded
     * XMLReader does not support the
     * http://xml.org/sax/features/namespace-prefixes property.</p>
     *
     * @param input An input source for the document.
     * @throws java.io.IOException If there is a problem reading
     *            the raw content of the document.
     * @throws org.xml.sax.SAXException If there is a problem
     *            processing the document.
     * @see #parse(java.lang.String)
     * @see org.xml.sax.Parser#parse(org.xml.sax.InputSource)
     */
    public void parse (InputSource input)
        throws IOException, SAXException
    {
        setupXMLReader();
        xmlReader.parse(input);
    }


    /**
     * Set up the XML reader.
     */
    private void setupXMLReader ()
        throws SAXException
    {
        xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
        try {
            xmlReader.setFeature("http://xml.org/sax/features/namespaces",
                                 false);
        } catch (SAXException e) {
            // NO OP: it's just extra information, and we can ignore it
        }
        xmlReader.setContentHandler(this);
    }



    ////////////////////////////////////////////////////////////////////
    // Implementation of org.xml.sax.ContentHandler.
    ////////////////////////////////////////////////////////////////////


    /**
     * Set a document locator.
     *
     * @param locator The document locator.
     * @see org.xml.sax.ContentHandler#setDocumentLocator
     */
    public void setDocumentLocator (Locator locator)
    {
        if (documentHandler != null)
            documentHandler.setDocumentLocator(locator);
    }


    /**
     * Start document event.
     *
     * @throws org.xml.sax.SAXException The client may raise a
     *            processing exception.
     * @see org.xml.sax.ContentHandler#startDocument
     */
    public void startDocument ()
        throws SAXException
    {
        if (documentHandler != null)
            documentHandler.startDocument();
    }


    /**
     * End document event.
     *
     * @throws org.xml.sax.SAXException The client may raise a
     *            processing exception.
     * @see org.xml.sax.ContentHandler#endDocument
     */
    public void endDocument ()
        throws SAXException
    {
        if (documentHandler != null)
            documentHandler.endDocument();
    }


    /**
     * Adapt a SAX2 start prefix mapping event.
     *
     * @param prefix The prefix being mapped.
     * @param uri The Namespace URI being mapped to.
     * @see org.xml.sax.ContentHandler#startPrefixMapping
     */
    public void startPrefixMapping (String prefix, String uri)
    {
    }


    /**
     * Adapt a SAX2 end prefix mapping event.
     *
     * @param prefix The prefix being mapped.
     * @see org.xml.sax.ContentHandler#endPrefixMapping
     */
    public void endPrefixMapping (String prefix)
    {
    }


    /**
     * Adapt a SAX2 start element event.
     *
     * @param uri The Namespace URI.
     * @param localName The Namespace local name.
     * @param qName The qualified (prefixed) name.
     * @param atts The SAX2 attributes.
     * @throws org.xml.sax.SAXException The client may raise a
     *            processing exception.
     * @see org.xml.sax.ContentHandler#endDocument
     */
    public void startElement (String uri, String localName,
                              String qName, Attributes atts)
        throws SAXException
    {
        if (documentHandler != null) {
            qAtts.setAttributes(atts);
            documentHandler.startElement(qName, qAtts);
        }
    }


    /**
     * Adapt a SAX2 end element event.
     *
     * @param uri The Namespace URI.
     * @param localName The Namespace local name.
     * @param qName The qualified (prefixed) name.
     * @throws org.xml.sax.SAXException The client may raise a
     *            processing exception.
     * @see org.xml.sax.ContentHandler#endElement
     */
    public void endElement (String uri, String localName,
                            String qName)
        throws SAXException
    {
        if (documentHandler != null)
            documentHandler.endElement(qName);
    }


    /**
     * Adapt a SAX2 characters event.
     *
     * @param ch An array of characters.
     * @param start The starting position in the array.
     * @param length The number of characters to use.
     * @throws org.xml.sax.SAXException The client may raise a
     *            processing exception.
     * @see org.xml.sax.ContentHandler#characters
     */
    public void characters (char ch[], int start, int length)
        throws SAXException
    {
        if (documentHandler != null)
            documentHandler.characters(ch, start, length);
    }


    /**
     * Adapt a SAX2 ignorable whitespace event.
     *
     * @param ch An array of characters.
     * @param start The starting position in the array.
     * @param length The number of characters to use.
     * @throws org.xml.sax.SAXException The client may raise a
     *            processing exception.
     * @see org.xml.sax.ContentHandler#ignorableWhitespace
     */
    public void ignorableWhitespace (char ch[], int start, int length)
        throws SAXException
    {
        if (documentHandler != null)
            documentHandler.ignorableWhitespace(ch, start, length);
    }


    /**
     * Adapt a SAX2 processing instruction event.
     *
     * @param target The processing instruction target.
     * @param data The remainder of the processing instruction
     * @throws org.xml.sax.SAXException The client may raise a
     *            processing exception.
     * @see org.xml.sax.ContentHandler#processingInstruction
     */
    public void processingInstruction (String target, String data)
        throws SAXException
    {
        if (documentHandler != null)
            documentHandler.processingInstruction(target, data);
    }


    /**
     * Adapt a SAX2 skipped entity event.
     *
     * @param name The name of the skipped entity.
     * @see org.xml.sax.ContentHandler#skippedEntity
     * @throws org.xml.sax.SAXException Throwable by subclasses.
     */
    public void skippedEntity (String name)
        throws SAXException
    {
    }



    ////////////////////////////////////////////////////////////////////
    // Internal state.
    ////////////////////////////////////////////////////////////////////

    XMLReader xmlReader;
    DocumentHandler documentHandler;
    AttributesAdapter qAtts;



    ////////////////////////////////////////////////////////////////////
    // Internal class.
    ////////////////////////////////////////////////////////////////////


    /**
     * Internal class to wrap a SAX2 Attributes object for SAX1.
     */
    final class AttributesAdapter implements AttributeList
    {
        AttributesAdapter ()
        {
        }


        /**
         * Set the embedded Attributes object.
         *
         * @param The embedded SAX2 Attributes.
         */
        void setAttributes (Attributes attributes)
        {
            this.attributes = attributes;
        }


        /**
         * Return the number of attributes.
         *
         * @return The length of the attribute list.
         * @see org.xml.sax.AttributeList#getLength
         */
        public int getLength ()
        {
            return attributes.getLength();
        }


        /**
         * Return the qualified (prefixed) name of an attribute by position.
         *
         * @return The qualified name.
         * @see org.xml.sax.AttributeList#getName
         */
        public String getName (int i)
        {
            return attributes.getQName(i);
        }


        /**
         * Return the type of an attribute by position.
         *
         * @return The type.
         * @see org.xml.sax.AttributeList#getType(int)
         */
        public String getType (int i)
        {
            return attributes.getType(i);
        }


        /**
         * Return the value of an attribute by position.
         *
         * @return The value.
         * @see org.xml.sax.AttributeList#getValue(int)
         */
        public String getValue (int i)
        {
            return attributes.getValue(i);
        }


        /**
         * Return the type of an attribute by qualified (prefixed) name.
         *
         * @return The type.
         * @see org.xml.sax.AttributeList#getType(java.lang.String)
         */
        public String getType (String qName)
        {
            return attributes.getType(qName);
        }


        /**
         * Return the value of an attribute by qualified (prefixed) name.
         *
         * @return The value.
         * @see org.xml.sax.AttributeList#getValue(java.lang.String)
         */
        public String getValue (String qName)
        {
            return attributes.getValue(qName);
        }

        private Attributes attributes;
    }

}

// end of XMLReaderAdapter.java
