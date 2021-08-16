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

package org.xml.sax;

import java.io.IOException;
import java.io.Reader;
import java.io.InputStream;

/**
 * A single input source for an XML entity.
 *
 * <p>This class allows a SAX application to encapsulate information
 * about an input source in a single object, which may include
 * a public identifier, a system identifier, a byte stream (possibly
 * with a specified encoding), and/or a character stream.</p>
 *
 * <p>There are two places that the application can deliver an
 * input source to the parser: as the argument to the Parser.parse
 * method, or as the return value of the EntityResolver.resolveEntity
 * method.</p>
 *
 * <p>The SAX parser will use the InputSource object to determine how
 * to read XML input.  If there is a character stream available, the
 * parser will read that stream directly, disregarding any text
 * encoding declaration found in that stream.
 * If there is no character stream, but there is
 * a byte stream, the parser will use that byte stream, using the
 * encoding specified in the InputSource or else (if no encoding is
 * specified) autodetecting the character encoding using an algorithm
 * such as the one in the XML specification.  If neither a character
 * stream nor a
 * byte stream is available, the parser will attempt to open a URI
 * connection to the resource identified by the system
 * identifier.</p>
 *
 * <p>An InputSource object belongs to the application: the SAX parser
 * shall never modify it in any way (it may modify a copy if
 * necessary).  However, standard processing of both byte and
 * character streams is to close them on as part of end-of-parse cleanup,
 * so applications should not attempt to re-use such streams after they
 * have been handed to a parser.  </p>
 *
 * @since 1.4, SAX 1.0
 * @author David Megginson
 * @see org.xml.sax.XMLReader#parse(org.xml.sax.InputSource)
 * @see org.xml.sax.EntityResolver#resolveEntity
 * @see java.io.InputStream
 * @see java.io.Reader
 */
public class InputSource {

    /**
     * Zero-argument default constructor.
     *
     * @see #setPublicId
     * @see #setSystemId
     * @see #setByteStream
     * @see #setCharacterStream
     * @see #setEncoding
     */
    public InputSource ()
    {
    }


    /**
     * Create a new input source with a system identifier.
     *
     * <p>Applications may use setPublicId to include a
     * public identifier as well, or setEncoding to specify
     * the character encoding, if known.</p>
     *
     * <p>If the system identifier is a URL, it must be fully
     * resolved (it may not be a relative URL).</p>
     *
     * @param systemId The system identifier (URI).
     * @see #setPublicId
     * @see #setSystemId
     * @see #setByteStream
     * @see #setEncoding
     * @see #setCharacterStream
     */
    public InputSource (String systemId)
    {
        setSystemId(systemId);
    }


    /**
     * Create a new input source with a byte stream.
     *
     * <p>Application writers should use setSystemId() to provide a base
     * for resolving relative URIs, may use setPublicId to include a
     * public identifier, and may use setEncoding to specify the object's
     * character encoding.</p>
     *
     * @param byteStream The raw byte stream containing the document.
     * @see #setPublicId
     * @see #setSystemId
     * @see #setEncoding
     * @see #setByteStream
     * @see #setCharacterStream
     */
    public InputSource (InputStream byteStream)
    {
        setByteStream(byteStream);
    }


    /**
     * Create a new input source with a character stream.
     *
     * <p>Application writers should use setSystemId() to provide a base
     * for resolving relative URIs, and may use setPublicId to include a
     * public identifier.</p>
     *
     * <p>The character stream shall not include a byte order mark.</p>
     *
     * @param characterStream the character stream
     * @see #setPublicId
     * @see #setSystemId
     * @see #setByteStream
     * @see #setCharacterStream
     */
    public InputSource (Reader characterStream)
    {
        setCharacterStream(characterStream);
    }


    /**
     * Set the public identifier for this input source.
     *
     * <p>The public identifier is always optional: if the application
     * writer includes one, it will be provided as part of the
     * location information.</p>
     *
     * @param publicId The public identifier as a string.
     * @see #getPublicId
     * @see org.xml.sax.Locator#getPublicId
     * @see org.xml.sax.SAXParseException#getPublicId
     */
    public void setPublicId (String publicId)
    {
        this.publicId = publicId;
    }


    /**
     * Get the public identifier for this input source.
     *
     * @return The public identifier, or null if none was supplied.
     * @see #setPublicId
     */
    public String getPublicId ()
    {
        return publicId;
    }


    /**
     * Set the system identifier for this input source.
     *
     * <p>The system identifier is optional if there is a byte stream
     * or a character stream, but it is still useful to provide one,
     * since the application can use it to resolve relative URIs
     * and can include it in error messages and warnings (the parser
     * will attempt to open a connection to the URI only if
     * there is no byte stream or character stream specified).</p>
     *
     * <p>If the application knows the character encoding of the
     * object pointed to by the system identifier, it can register
     * the encoding using the setEncoding method.</p>
     *
     * <p>If the system identifier is a URL, it must be fully
     * resolved (it may not be a relative URL).</p>
     *
     * @param systemId The system identifier as a string.
     * @see #setEncoding
     * @see #getSystemId
     * @see org.xml.sax.Locator#getSystemId
     * @see org.xml.sax.SAXParseException#getSystemId
     */
    public void setSystemId (String systemId)
    {
        this.systemId = systemId;
    }


    /**
     * Get the system identifier for this input source.
     *
     * <p>The getEncoding method will return the character encoding
     * of the object pointed to, or null if unknown.</p>
     *
     * <p>If the system ID is a URL, it will be fully resolved.</p>
     *
     * @return The system identifier, or null if none was supplied.
     * @see #setSystemId
     * @see #getEncoding
     */
    public String getSystemId ()
    {
        return systemId;
    }


    /**
     * Set the byte stream for this input source.
     *
     * <p>The SAX parser will ignore this if there is also a character
     * stream specified, but it will use a byte stream in preference
     * to opening a URI connection itself.</p>
     *
     * <p>If the application knows the character encoding of the
     * byte stream, it should set it with the setEncoding method.</p>
     *
     * @param byteStream A byte stream containing an XML document or
     *        other entity.
     * @see #setEncoding
     * @see #getByteStream
     * @see #getEncoding
     * @see java.io.InputStream
     */
    public void setByteStream (InputStream byteStream)
    {
        this.byteStream = byteStream;
    }


    /**
     * Get the byte stream for this input source.
     *
     * <p>The getEncoding method will return the character
     * encoding for this byte stream, or null if unknown.</p>
     *
     * @return The byte stream, or null if none was supplied.
     * @see #getEncoding
     * @see #setByteStream
     */
    public InputStream getByteStream ()
    {
        return byteStream;
    }


    /**
     * Set the character encoding, if known.
     *
     * <p>The encoding must be a string acceptable for an
     * XML encoding declaration (see section 4.3.3 of the XML 1.0
     * recommendation).</p>
     *
     * <p>This method has no effect when the application provides a
     * character stream.</p>
     *
     * @param encoding A string describing the character encoding.
     * @see #setSystemId
     * @see #setByteStream
     * @see #getEncoding
     */
    public void setEncoding (String encoding)
    {
        this.encoding = encoding;
    }


    /**
     * Get the character encoding for a byte stream or URI.
     * This value will be ignored when the application provides a
     * character stream.
     *
     * @return The encoding, or null if none was supplied.
     * @see #setByteStream
     * @see #getSystemId
     * @see #getByteStream
     */
    public String getEncoding ()
    {
        return encoding;
    }


    /**
     * Set the character stream for this input source.
     *
     * <p>If there is a character stream specified, the SAX parser
     * will ignore any byte stream and will not attempt to open
     * a URI connection to the system identifier.</p>
     *
     * @param characterStream The character stream containing the
     *        XML document or other entity.
     * @see #getCharacterStream
     * @see java.io.Reader
     */
    public void setCharacterStream (Reader characterStream)
    {
        this.characterStream = characterStream;
    }


    /**
     * Get the character stream for this input source.
     *
     * @return The character stream, or null if none was supplied.
     * @see #setCharacterStream
     */
    public Reader getCharacterStream ()
    {
        return characterStream;
    }

    /**
     * Indicates whether the {@code InputSource} object is empty. Empty is
     * defined as follows:
     * <ul>
     * <li>All of the input sources, including the public identifier, system
     * identifier, byte stream, and character stream, are {@code null}.
     * </li>
     * <li>The public identifier and system identifier are  {@code null}, and
     * byte and character stream are either  {@code null} or contain no byte
     * or character.
     * <p>
     * Note that this method will reset the byte stream if it is provided, or
     * the character stream if the byte stream is not provided.
     * </li>
     * </ul>
     * <p>
     * In case of error while checking the byte or character stream, the method
     * will return false to allow the XML processor to handle the error.
     *
     * @return true if the {@code InputSource} object is empty, false otherwise
     */
    public boolean isEmpty() {
        return (publicId == null && systemId == null && isStreamEmpty());
    }

    private boolean isStreamEmpty() {
        boolean empty = true;
        try {
            if (byteStream != null) {
                byteStream.reset();
                int bytesRead = byteStream.available();
                if (bytesRead > 0) {
                    return false;
                }
            }

            if (characterStream != null) {
                characterStream.reset();
                int c = characterStream.read();
                characterStream.reset();
                if (c != -1) {
                    return false;
                }
            }
        } catch (IOException ex) {
            //in case of error, return false
            return false;
        }

        return empty;
    }
    ////////////////////////////////////////////////////////////////////
    // Internal state.
    ////////////////////////////////////////////////////////////////////

    private String publicId;
    private String systemId;
    private InputStream byteStream;
    private String encoding;
    private Reader characterStream;

}

// end of InputSource.java
