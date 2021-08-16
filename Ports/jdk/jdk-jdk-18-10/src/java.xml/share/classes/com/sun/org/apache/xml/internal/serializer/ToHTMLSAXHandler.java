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

import javax.xml.transform.Result;

import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 * This class accepts SAX-like calls, then sends true SAX calls to a
 * wrapped SAX handler.  There is optimization done knowing that the ultimate
 * output is HTML.
 *
 * This class is not a public API.
 *
 * @xsl.usage internal
 */
public final class ToHTMLSAXHandler extends ToSAXHandler
{
        /**
         *  Handle document type declaration (for first element only)
         */
        private boolean m_dtdHandled = false;

    /**
     * Keeps track of whether output escaping is currently enabled
     */
    protected boolean m_escapeSetting = true;

    /**
     * Returns null.
     * @return null
     * @see Serializer#getOutputFormat()
     */
    public Properties getOutputFormat()
    {
        return null;
    }

    /**
     * Reurns null
     * @return null
     * @see Serializer#getOutputStream()
     */
    public OutputStream getOutputStream()
    {
        return null;
    }

    /**
     * Returns null
     * @return null
     * @see Serializer#getWriter()
     */
    public Writer getWriter()
    {
        return null;
    }

    /**
     * Does nothing.
     *
     */
    public void indent(int n) throws SAXException
    {
    }

    /**
     * Does nothing.
     * @see DOMSerializer#serialize(Node)
     */
    public void serialize(Node node) throws IOException
    {
        return;
    }

    /**
     * Turns special character escaping on/off.
     *
     *
     * @param escape true if escaping is to be set on.
     *
     * @see SerializationHandler#setEscaping(boolean)
     */
    public boolean setEscaping(boolean escape) throws SAXException
    {
        boolean oldEscapeSetting = m_escapeSetting;
        m_escapeSetting = escape;

        if (escape) {
            processingInstruction(Result.PI_ENABLE_OUTPUT_ESCAPING, "");
        } else {
            processingInstruction(Result.PI_DISABLE_OUTPUT_ESCAPING, "");
        }

        return oldEscapeSetting;
    }

    /**
     * Does nothing
     * @param indent the number of spaces to indent per indentation level
     * (ignored)
     * @see SerializationHandler#setIndent(boolean)
     */
    public void setIndent(boolean indent)
    {
    }

    /**
     * Does nothing.
     * @param format this parameter is not used
     * @see Serializer#setOutputFormat(Properties)
     */
    public void setOutputFormat(Properties format)
    {
    }

    /**
     * Does nothing.
     * @param output this parameter is ignored
     * @see Serializer#setOutputStream(OutputStream)
     */
    public void setOutputStream(OutputStream output)
    {
    }


    /**
     * Does nothing.
     * @param writer this parameter is ignored.
     * @see Serializer#setWriter(Writer)
     */
    public void setWriter(Writer writer)
    {
    }

    /**
     * @see org.xml.sax.ext.DeclHandler#attributeDecl(String, String, String, String, String)
     */
    /**
     * Does nothing.
     *
     * @param eName this parameter is ignored
     * @param aName this parameter is ignored
     * @param type this parameter is ignored
     * @param valueDefault this parameter is ignored
     * @param value this parameter is ignored
     * @see org.xml.sax.ext.DeclHandler#attributeDecl(String, String, String,String,String)
     */
    public void attributeDecl(
        String eName,
        String aName,
        String type,
        String valueDefault,
        String value)
        throws SAXException
    {
    }


    /**
     * Does nothing.
     * @see org.xml.sax.ext.DeclHandler#elementDecl(String, String)
     */
    public void elementDecl(String name, String model) throws SAXException
    {
        return;
    }

    /**
     * @see org.xml.sax.ext.DeclHandler#externalEntityDecl(String, String, String)
     */
    public void externalEntityDecl(String arg0, String arg1, String arg2)
        throws SAXException
    {
    }

    /**
     * Does nothing.
     *
     * @see org.xml.sax.DTDHandler#unparsedEntityDecl
     */
    public void internalEntityDecl(String name, String value)
        throws SAXException
    {
    }

    /**
     * Receive notification of the end of an element.
     *
     * <p>The SAX parser will invoke this method at the end of every
     * element in the XML document; there will be a corresponding
     * startElement() event for every endElement() event (even when the
     * element is empty).</p>
     *
     * <p>If the element name has a namespace prefix, the prefix will
     * still be attached to the name.</p>
     *
     *
     * @param uri The Namespace URI, or the empty string if the
     *        element has no Namespace URI or if Namespace
     *        processing is not being performed.
     * @param localName The local name (without prefix), or the
     *        empty string if Namespace processing is not being
     *        performed.
     * @param qName The qualified name (with prefix), or the
     *        empty string if qualified names are not available.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#endElement(String, String, String)
     */
    public void endElement(String uri, String localName, String qName)
        throws SAXException
    {
        flushPending();
        m_saxHandler.endElement(uri, localName, qName);

        // time to fire off endElement event
        if (m_tracer != null)
            super.fireEndElem(qName);
    }

    /**
     * Does nothing.
     */
    public void endPrefixMapping(String prefix) throws SAXException
    {
    }

    /**
     * Does nothing.
     * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
     */
    public void ignorableWhitespace(char[] ch, int start, int length)
        throws SAXException
    {
    }

    /**
     * Receive notification of a processing instruction.
     *
     * <p>The Parser will invoke this method once for each processing
     * instruction found: note that processing instructions may occur
     * before or after the main document element.</p>
     *
     * <p>A SAX parser should never report an XML declaration (XML 1.0,
     * section 2.8) or a text declaration (XML 1.0, section 4.3.1)
     * using this method.</p>
     *
     * @param target The processing instruction target.
     * @param data The processing instruction data, or null if
     *        none was supplied.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     *
     * @throws org.xml.sax.SAXException
     * @see org.xml.sax.ContentHandler#processingInstruction(String, String)
     */
    public void processingInstruction(String target, String data)
        throws SAXException
    {
        flushPending();
        m_saxHandler.processingInstruction(target,data);

                // time to fire off processing instruction event

        if (m_tracer != null)
                    super.fireEscapingEvent(target,data);
    }

    /**
     * Does nothing.
     * @see org.xml.sax.ContentHandler#setDocumentLocator(Locator)
     */
    public void setDocumentLocator(Locator arg0)
    {
        super.setDocumentLocator(arg0);
    }

    /**
     * Does nothing.
     * @see org.xml.sax.ContentHandler#skippedEntity(String)
     */
    public void skippedEntity(String arg0) throws SAXException
    {
    }

    /**
     * Receive notification of the beginning of an element, although this is a
     * SAX method additional namespace or attribute information can occur before
     * or after this call, that is associated with this element.
     *
     *
     * @param namespaceURI The Namespace URI, or the empty string if the
     *        element has no Namespace URI or if Namespace
     *        processing is not being performed.
     * @param localName The local name (without prefix), or the
     *        empty string if Namespace processing is not being
     *        performed.
     * @param qName The elements name.
     * @param atts The attributes attached to the element, if any.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#startElement
     * @see org.xml.sax.ContentHandler#endElement
     * @see org.xml.sax.AttributeList
     *
     * @throws org.xml.sax.SAXException
     *
     * @see org.xml.sax.ContentHandler#startElement(String, String, String, Attributes)
     */
    public void startElement(
        String namespaceURI,
        String localName,
        String qName,
        Attributes atts)
        throws SAXException
    {
        flushPending();
        super.startElement(namespaceURI, localName, qName, atts);
        m_saxHandler.startElement(namespaceURI, localName, qName, atts);
        m_elemContext.m_startTagOpen = false;
    }

    /**
     * Receive notification of a comment anywhere in the document. This callback
     * will be used for comments inside or outside the document element.
     * @param ch An array holding the characters in the comment.
     * @param start The starting position in the array.
     * @param length The number of characters to use from the array.
     * @throws org.xml.sax.SAXException The application may raise an exception.
     *
     * @see org.xml.sax.ext.LexicalHandler#comment(char[], int, int)
     */
    public void comment(char[] ch, int start, int length) throws SAXException
    {
        flushPending();
        if (m_lexHandler != null)
            m_lexHandler.comment(ch, start, length);

        // time to fire off comment event
        if (m_tracer != null)
            super.fireCommentEvent(ch, start, length);
        return;
    }

    /**
     * Does nothing.
     * @see org.xml.sax.ext.LexicalHandler#endCDATA()
     */
    public void endCDATA() throws SAXException
    {
        return;
    }

    /**
     * Does nothing.
     * @see org.xml.sax.ext.LexicalHandler#endDTD()
     */
    public void endDTD() throws SAXException
    {
    }

    /**
     * Does nothing.
     * @see org.xml.sax.ext.LexicalHandler#startCDATA()
     */
    public void startCDATA() throws SAXException
    {
    }

    /**
     * Does nothing.
     * @see org.xml.sax.ext.LexicalHandler#startEntity(String)
     */
    public void startEntity(String arg0) throws SAXException
    {
    }

    /**
     * Receive notification of the end of a document.
     *
     * <p>The SAX parser will invoke this method only once, and it will
     * be the last method invoked during the parse.  The parser shall
     * not invoke this method until it has either abandoned parsing
     * (because of an unrecoverable error) or reached the end of
     * input.</p>
     *
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     *
     * @throws org.xml.sax.SAXException
     *
     *
     */
    public void endDocument() throws SAXException
    {
        flushPending();

        // Close output document
        m_saxHandler.endDocument();

        if (m_tracer != null)
            super.fireEndDoc();
    }

    /**
     * This method is called when all the data needed for a call to the
     * SAX handler's startElement() method has been gathered.
     */
    protected void closeStartTag() throws SAXException
    {

        m_elemContext.m_startTagOpen = false;

        // Now is time to send the startElement event
        m_saxHandler.startElement(
            EMPTYSTRING,
            m_elemContext.m_elementName,
            m_elemContext.m_elementName,
            m_attributes);
        m_attributes.clear();

    }

    /**
     * Do nothing.
     * @see SerializationHandler#close()
     */
    public void close()
    {
        return;
    }

    /**
     * Receive notification of character data.
     *
     * @param chars The string of characters to process.
     *
     * @throws org.xml.sax.SAXException
     *
     * @see ExtendedContentHandler#characters(String)
     */
    public void characters(final String chars) throws SAXException
    {
        final int length = chars.length();
        if (length > m_charsBuff.length)
        {
            m_charsBuff = new char[length * 2 + 1];
        }
        chars.getChars(0, length, m_charsBuff, 0);
        this.characters(m_charsBuff, 0, length);
    }


    /**
     * A constructor
     * @param handler the wrapped SAX content handler
     * @param encoding the encoding of the output HTML document
     */
    public ToHTMLSAXHandler(ContentHandler handler, String encoding)
    {
        super(handler,encoding);
    }
    /**
     * A constructor.
     * @param handler the wrapped SAX content handler
     * @param lex the wrapped lexical handler
     * @param encoding the encoding of the output HTML document
     */
    public ToHTMLSAXHandler(
        ContentHandler handler,
        LexicalHandler lex,
        String encoding)
    {
        super(handler,lex,encoding);
    }

    /**
     * An element starts, but attributes are not fully known yet.
     *
     * @param elementNamespaceURI the URI of the namespace of the element
     * (optional)
     * @param elementLocalName the element name, but without prefix
     * (optional)
     * @param elementName the element name, with prefix, if any (required)
     *
     * @see ExtendedContentHandler#startElement(String)
     */
    public void startElement(
        String elementNamespaceURI,
        String elementLocalName,
        String elementName) throws SAXException
    {

        super.startElement(elementNamespaceURI, elementLocalName, elementName);

        flushPending();

        // Handle document type declaration (for first element only)
        if (!m_dtdHandled)
        {
            String doctypeSystem = getDoctypeSystem();
            String doctypePublic = getDoctypePublic();
            if ((doctypeSystem != null) || (doctypePublic != null)) {
                if (m_lexHandler != null)
                    m_lexHandler.startDTD(
                        elementName,
                        doctypePublic,
                        doctypeSystem);
            }
                        m_dtdHandled = true;
        }
        m_elemContext = m_elemContext.push(elementNamespaceURI, elementLocalName, elementName);
    }
    /**
     * An element starts, but attributes are not fully known yet.
     *
     * @param elementName the element name, with prefix, if any
     *
     * @see ExtendedContentHandler#startElement(String)
     */
    public void startElement(String elementName) throws SAXException
    {
        this.startElement(null,null, elementName);
    }

    /**
     * Receive notification of the end of an element.
     * @param elementName The element type name
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *     wrapping another exception.
     *
     * @see ExtendedContentHandler#endElement(String)
     */
    public void endElement(String elementName) throws SAXException
    {
        flushPending();
        m_saxHandler.endElement(EMPTYSTRING, elementName, elementName);

        // time to fire off endElement event
                if (m_tracer != null)
            super.fireEndElem(elementName);
    }

    /**
     * Receive notification of character data.
     *
     * <p>The Parser will call this method to report each chunk of
     * character data.  SAX parsers may return all contiguous character
     * data in a single chunk, or they may split it into several
     * chunks; however, all of the characters in any single event
     * must come from the same external entity, so that the Locator
     * provides useful information.</p>
     *
     * <p>The application must not attempt to read from the array
     * outside of the specified range.</p>
     *
     * <p>Note that some parsers will report whitespace using the
     * ignorableWhitespace() method rather than this one (validating
     * parsers must do so).</p>
     *
     * @param ch The characters from the XML document.
     * @param off The start position in the array.
     * @param len The number of characters to read from the array.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see #ignorableWhitespace
     * @see org.xml.sax.Locator
     *
     * @throws org.xml.sax.SAXException
     *
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int off, int len) throws SAXException
    {

        flushPending();
        m_saxHandler.characters(ch, off, len);

        // time to fire off characters event
                if (m_tracer != null)
            super.fireCharEvent(ch, off, len);
    }

    /**
     * This method flushes any pending events, which can be startDocument()
     * closing the opening tag of an element, or closing an open CDATA section.
     */
    public void flushPending() throws SAXException
    {
                if (m_needToCallStartDocument)
                {
                        startDocumentInternal();
                        m_needToCallStartDocument = false;
                }
        // Close any open element
        if (m_elemContext.m_startTagOpen)
        {
            closeStartTag();
            m_elemContext.m_startTagOpen = false;
        }
    }
    /**
     * Handle a prefix/uri mapping, which is associated with a startElement()
     * that is soon to follow. Need to close any open start tag to make
     * sure than any name space attributes due to this event are associated wih
     * the up comming element, not the current one.
     * @see ExtendedContentHandler#startPrefixMapping
     *
     * @param prefix The Namespace prefix being declared.
     * @param uri The Namespace URI the prefix is mapped to.
     * @param shouldFlush true if any open tags need to be closed first, this
     * will impact which element the mapping applies to (open parent, or its up
     * comming child)
     * @return returns true if the call made a change to the current
     * namespace information, false if it did not change anything, e.g. if the
     * prefix/namespace mapping was already in scope from before.
     *
     * @throws org.xml.sax.SAXException The client may throw
     *            an exception during processing.
     */
    public boolean startPrefixMapping(
        String prefix,
        String uri,
        boolean shouldFlush)
        throws SAXException
    {
        // no namespace support for HTML
        if (shouldFlush)
            flushPending();
        m_saxHandler.startPrefixMapping(prefix,uri);
        return false;
    }

    /**
     * Begin the scope of a prefix-URI Namespace mapping
     * just before another element is about to start.
     * This call will close any open tags so that the prefix mapping
     * will not apply to the current element, but the up comming child.
     *
     * @see org.xml.sax.ContentHandler#startPrefixMapping
     *
     * @param prefix The Namespace prefix being declared.
     * @param uri The Namespace URI the prefix is mapped to.
     *
     * @throws org.xml.sax.SAXException The client may throw
     *            an exception during processing.
     *
     */
    public void startPrefixMapping(String prefix, String uri)
        throws org.xml.sax.SAXException
    {
        startPrefixMapping(prefix,uri,true);
    }

    /**
     * This method is used when a prefix/uri namespace mapping
     * is indicated after the element was started with a
     * startElement() and before and endElement().
     * startPrefixMapping(prefix,uri) would be used before the
     * startElement() call.
     * @param prefix the prefix associated with the given URI.
     * @param uri the URI of the namespace
     *
     * @see ExtendedContentHandler#namespaceAfterStartElement(String, String)
     */
    public void namespaceAfterStartElement(
        final String prefix,
        final String uri)
        throws SAXException
    {
        // hack for XSLTC with finding URI for default namespace
        if (m_elemContext.m_elementURI == null)
        {
            String prefix1 = getPrefixPart(m_elemContext.m_elementName);
            if (prefix1 == null && EMPTYSTRING.equals(prefix))
            {
                // the elements URI is not known yet, and it
                // doesn't have a prefix, and we are currently
                // setting the uri for prefix "", so we have
                // the uri for the element... lets remember it
                m_elemContext.m_elementURI = uri;
            }
        }
        startPrefixMapping(prefix,uri,false);
    }

    /**
     * Try's to reset the super class and reset this class for
     * re-use, so that you don't need to create a new serializer
     * (mostly for performance reasons).
     *
     * @return true if the class was successfuly reset.
     * @see Serializer#reset()
     */
    public boolean reset()
    {
        boolean wasReset = false;
        if (super.reset())
        {
            resetToHTMLSAXHandler();
            wasReset = true;
        }
        return wasReset;
    }

    /**
     * Reset all of the fields owned by ToHTMLSAXHandler class
     *
     */
    private void resetToHTMLSAXHandler()
    {
        this.m_escapeSetting = true;
    }
}
