/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package test.auctionportal;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.stream.Collectors;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX2 event handlers.
 * This SAX2 ContentHandler receives callback event then print whole document
 * that is parsed.
 */
public class XInclHandler extends DefaultHandler implements LexicalHandler {
    /**
     * Print writer.
     */
    private final PrintWriter fOut;

    /**
     * Canonical output.
     */
    private volatile boolean fCanonical;

    /**
     * Element depth.
     */
    private volatile int fElementDepth;

    /**
     * Sets whether output is canonical.
     *
     * @param canonical if the output is canonical format.
     */
    public void setCanonical(boolean canonical) {
        fCanonical = canonical;
    }

    /**
     * Sets the output stream for printing.
     * @param stream OutputStream for message output.
     * @param encoding File encoding for message output.
     * @throws UnsupportedEncodingException if given encoding is an unsupported
     *         encoding name or invalid encoding name.
     */
    public XInclHandler(OutputStream stream, String encoding)
            throws UnsupportedEncodingException {
        // At least set one encoding.
        if (encoding == null) {
            encoding = "UTF8";
        }

        fOut = new PrintWriter(new OutputStreamWriter(stream, encoding), false);
    }

    /**
     * Receive notification of the beginning of the document. Write the start
     * document tag if it's not canonical mode.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     */
    @Override
    public void startDocument() throws SAXException {
        fElementDepth = 0;

        if (!fCanonical) {
            writeFlush("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        }
    }

    /**
     * Receive notification of a processing instruction.
     * @param target The processing instruction target.
     * @param data The processing instruction data, or null if
     *             none is supplied.
     * @exception SAXException Any SAX exception, possibly wrapping another
     *            exception.
     */
    @Override
    public void processingInstruction (String target, String data)
        throws SAXException {
        if (fElementDepth > 0) {
            StringBuilder instruction = new StringBuilder("<?").append(target);
            if (data != null && data.length() > 0) {
                instruction.append(' ').append(data);
            }
            instruction.append("?>");
            writeFlush(instruction.toString());
        }
    }

    /**
     * Receive notification of the start of an element then write the normalized
     * output to the file.
     * @param uri The Namespace URI, or the empty string if the
     *        element has no Namespace URI or if Namespace
     *        processing is not being performed.
     * @param local The local name (without prefix), or the
     *        empty string if Namespace processing is not being
     *        performed.
     * @param raw The qualified name (with prefix), or the
     *        empty string if qualified names are not available.
     * @param attrs The attributes attached to the element.  If
     *        there are no attributes, it shall be an empty
     *        Attributes object.
     * @throws SAXException Any SAX exception, possibly wrapping another
     *         exception.
     */
    @Override
    public void startElement(String uri, String local, String raw,
            Attributes attrs) throws SAXException {
        fElementDepth++;
        StringBuilder start = new StringBuilder().append('<').append(raw);
        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                start.append(' ').append(attrs.getQName(i)).append("=\"").
                    append(normalizeAndPrint(attrs.getValue(i))).append('"');
            }
        }
        start.append('>');
        writeFlush(start.toString());
    }

    /**
     * Receive notification of character data inside an element and write
     * normalized characters to file.
     * @param ch The characters.
     * @param start The start position in the character array.
     * @param length The number of characters to use from the
     *               character array.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     */
    @Override
    public void characters(char ch[], int start, int length)
            throws SAXException {
        writeFlush(normalizeAndPrint(ch, start, length));
    }

    /**
     * Receiving notification of ignorable whitespace in element content and
     * writing normalized ignorable characters to file.
     * @param ch The characters.
     * @param start The start position in the character array.
     * @param length The number of characters to use from the
     *               character array.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     */
    @Override
    public void ignorableWhitespace(char ch[], int start, int length)
            throws SAXException {
        characters(ch, start, length);
    }

    /**
     * Receive notification of the end of an element and print end element.
     *
     * @param uri The Namespace URI, or the empty string if the
     *        element has no Namespace URI or if Namespace
     *        processing is not being performed.
     * @param local The local name (without prefix), or the
     *        empty string if Namespace processing is not being
     *        performed.
     * @param raw The qualified name (with prefix), or the
     *        empty string if qualified names are not available.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     */
    @Override
    public void endElement(String uri, String local, String raw)
            throws SAXException {
        fElementDepth--;
        writeFlush("</" + raw + ">");
    }

    /**
     * Receive notification of a parser warning and print it out.
     * @param ex The warning information encoded as an exception.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     */
    @Override
    public void warning(SAXParseException ex) throws SAXException {
        printError("Warning", ex);
    }

    /**
     * Receive notification of a parser error and print it out.
     * @param ex The error information encoded as an exception.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     */
    @Override
    public void error(SAXParseException ex) throws SAXException {
        printError("Error", ex);
    }

    /**
     * Receive notification of a parser fatal error. Throw out fatal error
     * following print fatal error message.
     * @param ex The fatal error information encoded as an exception.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.

     */
    @Override
    public void fatalError(SAXParseException ex) throws SAXException {
        printError("Fatal Error", ex);
        throw ex;
    }

    /**
     * Do nothing on start DTD.
     * @param name The document type name.
     * @param publicId The declared public identifier for the
     *        external DTD subset, or null if none was declared.
     * @param systemId The declared system identifier for the
     *        external DTD subset, or null if none was declared.
     *        (Note that this is not resolved against the document
     *        base URI.)
     * @exception SAXException The application may raise an
     *            exception.
     */
    @Override
    public void startDTD(String name, String publicId, String systemId)
        throws SAXException {
    }

    /**
     * Do nothing on end DTD.
     * @exception SAXException The application may raise an exception.
     */
    @Override
    public void endDTD() throws SAXException {
    }

    /**
     * Do nothing on start entity.
     * @param name The name of the entity.  If it is a parameter
     *        entity, the name will begin with '%', and if it is the
     *        external DTD subset, it will be "[dtd]".
     * @exception SAXException The application may raise an exception.
     */
    @Override
    public void startEntity(String name) throws SAXException {
    }

    /**
     * Do nothing on end entity.
     * @param name The name of the entity.  If it is a parameter
     *        entity, the name will begin with '%', and if it is the
     *        external DTD subset, it will be "[dtd]".
     * @exception SAXException The application may raise an exception.
     */
    @Override
    public void endEntity(String name) throws SAXException {
    }

    /**
     * Do nothing on start CDATA section.
     * @exception SAXException The application may raise an exception.
     */
    @Override
    public void startCDATA() throws SAXException {
    }

    /**
     * Do nothing on end CDATA section.
     * @exception SAXException The application may raise an exception.
     */
    @Override
    public void endCDATA() throws SAXException {
    }

    /**
     * Report an normalized XML comment when receive a comment in the document.
     *
     * @param ch An array holding the characters in the comment.
     * @param start The starting position in the array.
     * @param length The number of characters to use from the array.
     * @exception SAXException The application may raise an exception.
     */
    @Override
    public void comment(char ch[], int start, int length) throws SAXException {
        if (!fCanonical && fElementDepth > 0) {
            writeFlush("<!--" + normalizeAndPrint(ch, start, length) + "-->");
        }
    }

    /**
     * Normalizes and prints the given string.
     * @param s String to be normalized
     */
    private String normalizeAndPrint(String s) {
        return s.chars().mapToObj(c -> normalizeAndPrint((char)c)).
                collect(Collectors.joining());
    }

    /**
     * Normalizes and prints the given array of characters.
     * @param ch The characters to be normalized.
     * @param start The start position in the character array.
     * @param length The number of characters to use from the
     *               character array.
     */
    private String normalizeAndPrint(char[] ch, int offset, int length) {
        return normalizeAndPrint(new String(ch, offset, length));
    }

    /**
     * Normalizes given character.
     * @param c char to be normalized.
     */
    private String normalizeAndPrint(char c) {
        switch (c) {
            case '<':
                return "&lt;";
            case '>':
                return "&gt;";
            case '&':
                return "&amp;";
            case '"':
                return "&quot;";
            case '\r':
            case '\n':
                return fCanonical ? "&#" + Integer.toString(c) + ";" : String.valueOf(c);
            default:
                return String.valueOf(c);
        }
    }

    /**
     * Prints the error message.
     * @param type error type
     * @param ex exception that need to be printed
     */
    private void printError(String type, SAXParseException ex) {
        System.err.print("[" + type + "] ");
        String systemId = ex.getSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');
            if (index != -1)
                systemId = systemId.substring(index + 1);
            System.err.print(systemId);
        }
        System.err.print(':' + ex.getLineNumber());
        System.err.print(':' + ex.getColumnNumber());
        System.err.println(": " + ex.getMessage());
        System.err.flush();
    }

    /**
     * Write out and flush.
     * @param out string to be written.
     */
    private void writeFlush(String out) {
        fOut.print(out);
        fOut.flush();
    }
}
