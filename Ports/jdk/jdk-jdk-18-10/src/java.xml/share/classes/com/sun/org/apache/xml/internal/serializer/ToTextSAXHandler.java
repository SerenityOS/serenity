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

package com.sun.org.apache.xml.internal.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;

import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 * This class converts SAX-like event to SAX events for
 * xsl:output method "text".
 *
 * This class is only to be used internally. This class is not a public API.
 *
 * @xsl.usage internal
 */
public final class ToTextSAXHandler extends ToSAXHandler
{
    /**
     * From XSLTC
     * @see ExtendedContentHandler#endElement(String)
     */
    public void endElement(String elemName) throws SAXException
    {
        if (m_tracer != null)
            super.fireEndElem(elemName);
    }

    /**
     * @see org.xml.sax.ContentHandler#endElement(String, String, String)
     */
    public void endElement(String arg0, String arg1, String arg2)
        throws SAXException
    {
                if (m_tracer != null)
            super.fireEndElem(arg2);
    }

    public ToTextSAXHandler(ContentHandler hdlr, LexicalHandler lex, String encoding)
    {
        super(hdlr, lex, encoding);
    }

        /**
     * From XSLTC
     */
    public ToTextSAXHandler(ContentHandler handler, String encoding)
    {
        super(handler,encoding);
    }

    public void comment(char ch[], int start, int length)
        throws org.xml.sax.SAXException
    {
        if (m_tracer != null)
            super.fireCommentEvent(ch, start, length);
    }

    public void comment(String data) throws org.xml.sax.SAXException
    {
        final int length = data.length();
        if (length > m_charsBuff.length)
        {
            m_charsBuff = new char[length*2 + 1];
        }
        data.getChars(0, length, m_charsBuff, 0);
        comment(m_charsBuff, 0, length);
    }

    /**
     * @see Serializer#getOutputFormat()
     */
    public Properties getOutputFormat()
    {
        return null;
    }

    /**
     * @see Serializer#getOutputStream()
     */
    public OutputStream getOutputStream()
    {
        return null;
    }

    /**
     * @see Serializer#getWriter()
     */
    public Writer getWriter()
    {
        return null;
    }

    /**
     * Does nothing because
     * the indent attribute is ignored for text output.
     *
     */
    public void indent(int n) throws SAXException
    {
    }

    /**
     * @see Serializer#reset()
     */
    public boolean reset()
    {
        return false;
    }

    /**
     * @see DOMSerializer#serialize(Node)
     */
    public void serialize(Node node) throws IOException
    {
    }

    /**
     * @see SerializationHandler#setEscaping(boolean)
     */
    public boolean setEscaping(boolean escape)
    {
        return false;
    }

    /**
     * @see SerializationHandler#setIndent(boolean)
     */
    public void setIndent(boolean indent)
    {
    }

    /**
     * @see Serializer#setOutputFormat(Properties)
     */
    public void setOutputFormat(Properties format)
    {
    }

    /**
     * @see Serializer#setOutputStream(OutputStream)
     */
    public void setOutputStream(OutputStream output)
    {
    }

    /**
     * @see Serializer#setWriter(Writer)
     */
    public void setWriter(Writer writer)
    {
    }

    /**
     * @see ExtendedContentHandler#addAttribute(String, String, String, String, String)
     */
    public void addAttribute(
        String uri,
        String localName,
        String rawName,
        String type,
        String value,
        boolean XSLAttribute)
    {
    }

    /**
     * @see org.xml.sax.ext.DeclHandler#attributeDecl(String, String, String, String, String)
     */
    public void attributeDecl(
        String arg0,
        String arg1,
        String arg2,
        String arg3,
        String arg4)
        throws SAXException
    {
    }

    /**
     * @see org.xml.sax.ext.DeclHandler#elementDecl(String, String)
     */
    public void elementDecl(String arg0, String arg1) throws SAXException
    {
    }

    /**
     * @see org.xml.sax.ext.DeclHandler#externalEntityDecl(String, String, String)
     */
    public void externalEntityDecl(String arg0, String arg1, String arg2)
        throws SAXException
    {
    }

    /**
     * @see org.xml.sax.ext.DeclHandler#internalEntityDecl(String, String)
     */
    public void internalEntityDecl(String arg0, String arg1)
        throws SAXException
    {
    }

    /**
     * @see org.xml.sax.ContentHandler#endPrefixMapping(String)
     */
    public void endPrefixMapping(String arg0) throws SAXException
    {
    }

    /**
     * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
     */
    public void ignorableWhitespace(char[] arg0, int arg1, int arg2)
        throws SAXException
    {
    }

    /**
     * From XSLTC
     * @see org.xml.sax.ContentHandler#processingInstruction(String, String)
     */
    public void processingInstruction(String arg0, String arg1)
        throws SAXException
    {
        if (m_tracer != null)
            super.fireEscapingEvent(arg0, arg1);
    }

    /**
     * @see org.xml.sax.ContentHandler#setDocumentLocator(Locator)
     */
    public void setDocumentLocator(Locator arg0)
    {
        super.setDocumentLocator(arg0);
    }

    /**
     * @see org.xml.sax.ContentHandler#skippedEntity(String)
     */
    public void skippedEntity(String arg0) throws SAXException
    {
    }

    /**
     * @see org.xml.sax.ContentHandler#startElement(String, String, String, Attributes)
     */
    public void startElement(
        String arg0,
        String arg1,
        String arg2,
        Attributes arg3)
        throws SAXException
    {
        flushPending();
        super.startElement(arg0, arg1, arg2, arg3);
    }

    /**
     * @see org.xml.sax.ext.LexicalHandler#endCDATA()
     */
    public void endCDATA() throws SAXException
    {
    }

    /**
     * @see org.xml.sax.ext.LexicalHandler#endDTD()
     */
    public void endDTD() throws SAXException
    {
    }

    /**
     * @see org.xml.sax.ext.LexicalHandler#startCDATA()
     */
    public void startCDATA() throws SAXException
    {
    }


    /**
     * @see org.xml.sax.ext.LexicalHandler#startEntity(String)
     */
    public void startEntity(String arg0) throws SAXException
    {
    }


    /**
     * From XSLTC
     * @see ExtendedContentHandler#startElement(String)
     */
    public void startElement(
    String elementNamespaceURI,
    String elementLocalName,
    String elementName) throws SAXException
    {
        super.startElement(elementNamespaceURI, elementLocalName, elementName);
    }

    public void startElement(
    String elementName) throws SAXException
    {
        super.startElement(elementName);
    }


    /**
     * From XSLTC
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    public void endDocument() throws SAXException {

        flushPending();
        m_saxHandler.endDocument();

        if (m_tracer != null)
            super.fireEndDoc();
    }

    /**
         *
     * @see ExtendedContentHandler#characters(String)
     */
    public void characters(String characters)
    throws SAXException
    {
        final int length = characters.length();
        if (length > m_charsBuff.length)
        {
            m_charsBuff = new char[length*2 + 1];
        }
        characters.getChars(0, length, m_charsBuff, 0);

        m_saxHandler.characters(m_charsBuff, 0, length);

    }
    /**
         * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] characters, int offset, int length)
    throws SAXException
    {

        m_saxHandler.characters(characters, offset, length);

        // time to fire off characters event
                if (m_tracer != null)
            super.fireCharEvent(characters, offset, length);
    }

    /**
     * From XSLTC
     */
    public void addAttribute(String name, String value)
    {
        // do nothing
    }


    public boolean startPrefixMapping(
        String prefix,
        String uri,
        boolean shouldFlush)
        throws SAXException
    {
        // no namespace support for HTML
        return false;
    }


    public void startPrefixMapping(String prefix, String uri)
        throws org.xml.sax.SAXException
    {
        // no namespace support for HTML
    }


    public void namespaceAfterStartElement(
        final String prefix,
        final String uri)
        throws SAXException
    {
        // no namespace support for HTML
    }

}
