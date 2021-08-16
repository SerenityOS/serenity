/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.util.xml.impl;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import jdk.internal.util.xml.XMLStreamException;
import jdk.internal.util.xml.XMLStreamWriter;

/**
 * Implementation of a reduced version of XMLStreamWriter
 *
 * @author Joe Wang
 */
public class XMLStreamWriterImpl implements XMLStreamWriter {
    //Document state

    static final int STATE_XML_DECL = 1;
    static final int STATE_PROLOG = 2;
    static final int STATE_DTD_DECL = 3;
    static final int STATE_ELEMENT = 4;
    //Element state
    static final int ELEMENT_STARTTAG_OPEN = 10;
    static final int ELEMENT_STARTTAG_CLOSE = 11;
    static final int ELEMENT_ENDTAG_OPEN = 12;
    static final int ELEMENT_ENDTAG_CLOSE = 13;
    public static final char CLOSE_START_TAG = '>';
    public static final char OPEN_START_TAG = '<';
    public static final String OPEN_END_TAG = "</";
    public static final char CLOSE_END_TAG = '>';
    public static final String START_CDATA = "<![CDATA[";
    public static final String END_CDATA = "]]>";
    public static final String CLOSE_EMPTY_ELEMENT = "/>";
    public static final String ENCODING_PREFIX = "&#x";
    public static final char SPACE = ' ';
    public static final char AMPERSAND = '&';
    public static final char DOUBLEQUOT = '"';
    public static final char SEMICOLON = ';';
    //current state
    private int _state = 0;
    private Element _currentEle;
    private XMLWriter _writer;
    private Charset _charset;
    /**
     * This flag can be used to turn escaping off for content. It does
     * not apply to attribute content.
     */
    boolean _escapeCharacters = true;
    //pretty print by default
    private boolean _doIndent = true;
    //The system line separator for writing out line breaks.
    private char[] _lineSep = System.lineSeparator().toCharArray();

    public XMLStreamWriterImpl(OutputStream os) throws XMLStreamException {
        this(os, XMLStreamWriter.DEFAULT_CHARSET);
    }

    public XMLStreamWriterImpl(OutputStream os, Charset cs)
        throws XMLStreamException
    {
        if (cs == null) {
            _charset = XMLStreamWriter.DEFAULT_CHARSET;
        } else {
            try {
                _charset = checkCharset(cs);
            } catch (UnsupportedEncodingException e) {
                throw new XMLStreamException(e);
            }
        }

        _writer = new XMLWriter(os, null, _charset);
    }

    /**
     * Write the XML Declaration. Defaults the XML version to 1.0, and the
     * encoding to utf-8.
     *
     * @throws XMLStreamException
     */
    public void writeStartDocument() throws XMLStreamException {
        writeStartDocument(_charset.name(), XMLStreamWriter.DEFAULT_XML_VERSION);
    }

    /**
     * Write the XML Declaration. Defaults the encoding to utf-8
     *
     * @param version version of the xml document
     * @throws XMLStreamException
     */
    public void writeStartDocument(String version) throws XMLStreamException {
        writeStartDocument(_charset.name(), version, null);
    }

    /**
     * Write the XML Declaration. Note that the encoding parameter does not set
     * the actual encoding of the underlying output. That must be set when the
     * instance of the XMLStreamWriter is created
     *
     * @param encoding encoding of the xml declaration
     * @param version version of the xml document
     * @throws XMLStreamException If given encoding does not match encoding of the
     * underlying stream
     */
    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        writeStartDocument(encoding, version, null);
    }

    /**
     * Write the XML Declaration. Note that the encoding parameter does not set
     * the actual encoding of the underlying output. That must be set when the
     * instance of the XMLStreamWriter is created
     *
     * @param encoding encoding of the xml declaration
     * @param version version of the xml document
     * @param standalone indicate if the xml document is standalone
     * @throws XMLStreamException If given encoding does not match encoding of the
     * underlying stream
     */
    public void writeStartDocument(String encoding, String version, String standalone)
        throws XMLStreamException
    {
        if (_state > 0) {
            throw new XMLStreamException("XML declaration must be as the first line in the XML document.");
        }
        _state = STATE_XML_DECL;
        String enc = encoding;
        if (enc == null) {
            enc = _charset.name();
        } else {
            //check if the encoding is supported
            try {
                getCharset(encoding);
            } catch (UnsupportedEncodingException e) {
                throw new XMLStreamException(e);
            }
        }

        if (version == null) {
            version = XMLStreamWriter.DEFAULT_XML_VERSION;
        }

        _writer.write("<?xml version=\"");
        _writer.write(version);
        _writer.write(DOUBLEQUOT);

        if (enc != null) {
            _writer.write(" encoding=\"");
            _writer.write(enc);
            _writer.write(DOUBLEQUOT);
        }

        if (standalone != null) {
            _writer.write(" standalone=\"");
            _writer.write(standalone);
            _writer.write(DOUBLEQUOT);
        }
        _writer.write("?>");
        writeLineSeparator();
    }

    /**
     * Write a DTD section.  This string represents the entire doctypedecl production
     * from the XML 1.0 specification.
     *
     * @param dtd the DTD to be written
     * @throws XMLStreamException
     */
    public void writeDTD(String dtd) throws XMLStreamException {
        if (_currentEle != null && _currentEle.getState() == ELEMENT_STARTTAG_OPEN) {
            closeStartTag();
        }
        _writer.write(dtd);
        writeLineSeparator();
    }

    /**
     * Writes a start tag to the output.
     * @param localName local name of the tag, may not be null
     * @throws XMLStreamException
     */
    public void writeStartElement(String localName) throws XMLStreamException {
        if (localName == null || localName.isEmpty()) {
            throw new XMLStreamException("Local Name cannot be null or empty");
        }

        _state = STATE_ELEMENT;
        if (_currentEle != null && _currentEle.getState() == ELEMENT_STARTTAG_OPEN) {
            closeStartTag();
        }

        _currentEle = new Element(_currentEle, localName, false);
        openStartTag();

        _writer.write(localName);
    }

    /**
     * Writes an empty element tag to the output
     * @param localName local name of the tag, may not be null
     * @throws XMLStreamException
     */
    public void writeEmptyElement(String localName) throws XMLStreamException {
        if (_currentEle != null && _currentEle.getState() == ELEMENT_STARTTAG_OPEN) {
            closeStartTag();
        }

        _currentEle = new Element(_currentEle, localName, true);

        openStartTag();
        _writer.write(localName);
    }

    /**
     * Writes an attribute to the output stream without a prefix.
     * @param localName the local name of the attribute
     * @param value the value of the attribute
     * @throws IllegalStateException if the current state does not allow Attribute writing
     * @throws XMLStreamException
     */
    public void writeAttribute(String localName, String value) throws XMLStreamException {
        if (_currentEle.getState() != ELEMENT_STARTTAG_OPEN) {
            throw new XMLStreamException(
                    "Attribute not associated with any element");
        }

        _writer.write(SPACE);
        _writer.write(localName);
        _writer.write("=\"");
        writeXMLContent(
                value,
                true, // true = escapeChars
                true);  // true = escapeDoubleQuotes
        _writer.write(DOUBLEQUOT);
    }

    public void writeEndDocument() throws XMLStreamException {
        if (_currentEle != null && _currentEle.getState() == ELEMENT_STARTTAG_OPEN) {
            closeStartTag();
        }

        /**
         * close unclosed elements if any
         */
        while (_currentEle != null) {

            if (!_currentEle.isEmpty()) {
                _writer.write(OPEN_END_TAG);
                _writer.write(_currentEle.getLocalName());
                _writer.write(CLOSE_END_TAG);
            }

            _currentEle = _currentEle.getParent();
        }
    }

    public void writeEndElement() throws XMLStreamException {
        if (_currentEle != null && _currentEle.getState() == ELEMENT_STARTTAG_OPEN) {
            closeStartTag();
        }

        if (_currentEle == null) {
            throw new XMLStreamException("No element was found to write");
        }

        if (_currentEle.isEmpty()) {
            return;
        }

        _writer.write(OPEN_END_TAG);
        _writer.write(_currentEle.getLocalName());
        _writer.write(CLOSE_END_TAG);
        writeLineSeparator();

        _currentEle = _currentEle.getParent();
    }

    public void writeCData(String cdata) throws XMLStreamException {
        if (cdata == null) {
            throw new XMLStreamException("cdata cannot be null");
        }

        if (_currentEle != null && _currentEle.getState() == ELEMENT_STARTTAG_OPEN) {
            closeStartTag();
        }

        _writer.write(START_CDATA);
        _writer.write(cdata);
        _writer.write(END_CDATA);
    }

    public void writeCharacters(String data) throws XMLStreamException {
        if (_currentEle != null && _currentEle.getState() == ELEMENT_STARTTAG_OPEN) {
            closeStartTag();
        }

        writeXMLContent(data);
    }

    public void writeCharacters(char[] data, int start, int len)
            throws XMLStreamException {
        if (_currentEle != null && _currentEle.getState() == ELEMENT_STARTTAG_OPEN) {
            closeStartTag();
        }

        writeXMLContent(data, start, len, _escapeCharacters);
    }

    /**
     * Close this XMLStreamWriter by closing underlying writer.
     */
    public void close() throws XMLStreamException {
        if (_writer != null) {
            _writer.close();
        }
        _writer = null;
        _currentEle = null;
        _state = 0;
    }

    /**
     * Flush this XMLStreamWriter by flushing underlying writer.
     */
    public void flush() throws XMLStreamException {
        if (_writer != null) {
            _writer.flush();
        }
    }

    /**
     * Set the flag to indicate if the writer should add line separator
     * @param doIndent
     */
    public void setDoIndent(boolean doIndent) {
        _doIndent = doIndent;
    }

    /**
     * Writes XML content to underlying writer. Escapes characters unless
     * escaping character feature is turned off.
     */
    private void writeXMLContent(char[] content, int start, int length, boolean escapeChars)
        throws XMLStreamException
    {
        if (!escapeChars) {
            _writer.write(content, start, length);
            return;
        }

        // Index of the next char to be written
        int startWritePos = start;

        final int end = start + length;

        for (int index = start; index < end; index++) {
            char ch = content[index];

            if (!_writer.canEncode(ch)) {
                _writer.write(content, startWritePos, index - startWritePos);

                // Escape this char as underlying encoder cannot handle it
                _writer.write(ENCODING_PREFIX);
                _writer.write(Integer.toHexString(ch));
                _writer.write(SEMICOLON);
                startWritePos = index + 1;
                continue;
            }

            switch (ch) {
                case OPEN_START_TAG:
                    _writer.write(content, startWritePos, index - startWritePos);
                    _writer.write("&lt;");
                    startWritePos = index + 1;

                    break;

                case AMPERSAND:
                    _writer.write(content, startWritePos, index - startWritePos);
                    _writer.write("&amp;");
                    startWritePos = index + 1;

                    break;

                case CLOSE_START_TAG:
                    _writer.write(content, startWritePos, index - startWritePos);
                    _writer.write("&gt;");
                    startWritePos = index + 1;

                    break;
            }
        }

        // Write any pending data
        _writer.write(content, startWritePos, end - startWritePos);
    }

    private void writeXMLContent(String content) throws XMLStreamException {
        if (content != null && !content.isEmpty()) {
            writeXMLContent(content,
                    _escapeCharacters, // boolean = escapeChars
                    false);             // false = escapeDoubleQuotes
        }
    }

    /**
     * Writes XML content to underlying writer. Escapes characters unless
     * escaping character feature is turned off.
     */
    private void writeXMLContent(
            String content,
            boolean escapeChars,
            boolean escapeDoubleQuotes)
        throws XMLStreamException
    {

        if (!escapeChars) {
            _writer.write(content);

            return;
        }

        // Index of the next char to be written
        int startWritePos = 0;

        final int end = content.length();

        for (int index = 0; index < end; index++) {
            char ch = content.charAt(index);

            if (!_writer.canEncode(ch)) {
                _writer.write(content, startWritePos, index - startWritePos);

                // Escape this char as underlying encoder cannot handle it
                _writer.write(ENCODING_PREFIX);
                _writer.write(Integer.toHexString(ch));
                _writer.write(SEMICOLON);
                startWritePos = index + 1;
                continue;
            }

            switch (ch) {
                case OPEN_START_TAG:
                    _writer.write(content, startWritePos, index - startWritePos);
                    _writer.write("&lt;");
                    startWritePos = index + 1;

                    break;

                case AMPERSAND:
                    _writer.write(content, startWritePos, index - startWritePos);
                    _writer.write("&amp;");
                    startWritePos = index + 1;

                    break;

                case CLOSE_START_TAG:
                    _writer.write(content, startWritePos, index - startWritePos);
                    _writer.write("&gt;");
                    startWritePos = index + 1;

                    break;

                case DOUBLEQUOT:
                    _writer.write(content, startWritePos, index - startWritePos);
                    if (escapeDoubleQuotes) {
                        _writer.write("&quot;");
                    } else {
                        _writer.write(DOUBLEQUOT);
                    }
                    startWritePos = index + 1;

                    break;
            }
        }

        // Write any pending data
        _writer.write(content, startWritePos, end - startWritePos);
    }

    /**
     * marks open of start tag and writes the same into the writer.
     */
    private void openStartTag() throws XMLStreamException {
        _currentEle.setState(ELEMENT_STARTTAG_OPEN);
        _writer.write(OPEN_START_TAG);
    }

    /**
     * marks close of start tag and writes the same into the writer.
     */
    private void closeStartTag() throws XMLStreamException {
        if (_currentEle.isEmpty()) {
            _writer.write(CLOSE_EMPTY_ELEMENT);
        } else {
            _writer.write(CLOSE_START_TAG);

        }

        if (_currentEle.getParent() == null) {
            writeLineSeparator();
        }

        _currentEle.setState(ELEMENT_STARTTAG_CLOSE);

    }

    /**
     * Write a line separator
     * @throws XMLStreamException
     */
    private void writeLineSeparator() throws XMLStreamException {
        if (_doIndent) {
            _writer.write(_lineSep, 0, _lineSep.length);
        }
    }

    /**
     * Returns a charset object for the specified encoding
     * @param encoding
     * @return a charset object
     * @throws UnsupportedEncodingException if the encoding is not supported
     */
    private Charset getCharset(String encoding) throws UnsupportedEncodingException {
        if (encoding.equalsIgnoreCase("UTF-32")) {
            throw new UnsupportedEncodingException("The basic XMLWriter does "
                    + "not support " + encoding);
        }

        Charset cs;
        try {
            cs = Charset.forName(encoding);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException ex) {
            throw new UnsupportedEncodingException(encoding);
        }
        return cs;
    }

    /**
     * Checks for charset support.
     * @param charset the specified charset
     * @return the charset
     * @throws UnsupportedEncodingException if the charset is not supported
     */
    private Charset checkCharset(Charset charset) throws UnsupportedEncodingException {
        if (charset.name().equalsIgnoreCase("UTF-32")) {
            throw new UnsupportedEncodingException("The basic XMLWriter does "
                    + "not support " + charset.name());
        }
        return charset;
    }

    /*
     * Start of Internal classes.
     *
     */
    protected static class Element {

        /**
         * the parent element
         */
        protected Element _parent;
        /**
         * The size of the stack.
         */
        protected short _Depth;
        /**
         * indicate if an element is an empty one
         */
        boolean _isEmptyElement = false;
        String _localpart;
        int _state;

        /**
         * Default constructor.
         */
        public Element() {
        }

        /**
         * @param parent the parent of the element
         * @param localpart name of the element
         * @param isEmpty indicate if the element is an empty one
         */
        public Element(Element parent, String localpart, boolean isEmpty) {
            _parent = parent;
            _localpart = localpart;
            _isEmptyElement = isEmpty;
        }

        public Element getParent() {
            return _parent;
        }

        public String getLocalName() {
            return _localpart;
        }

        /**
         * get the state of the element
         */
        public int getState() {
            return _state;
        }

        /**
         * Set the state of the element
         *
         * @param state the state of the element
         */
        public void setState(int state) {
            _state = state;
        }

        public boolean isEmpty() {
            return _isEmptyElement;
        }
    }
}
