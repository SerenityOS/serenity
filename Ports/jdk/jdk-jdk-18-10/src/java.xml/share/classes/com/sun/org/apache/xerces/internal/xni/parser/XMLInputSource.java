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

package com.sun.org.apache.xerces.internal.xni.parser;

import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import java.io.InputStream;
import java.io.Reader;
import org.xml.sax.InputSource;

/**
 * This class represents an input source for an XML document. The
 * basic properties of an input source are the following:
 * <ul>
 *  <li>public identifier</li>
 *  <li>system identifier</li>
 *  <li>byte stream or character stream</li>
 *  <li>
 * </ul>
 *
 * @author Andy Clark, IBM
 *
 */
public class XMLInputSource {

    //
    // Data
    //

    /** Public identifier. */
    protected String fPublicId;

    /** System identifier. */
    protected String fSystemId;

    /** Base system identifier. */
    protected String fBaseSystemId;

    /** Byte stream. */
    protected InputStream fByteStream;

    /** Character stream. */
    protected Reader fCharStream;

    /** Encoding. */
    protected String fEncoding;

    //indicates whether the source is created by a resolver
    boolean fIsCreatedByResolver = false;
    //
    // Constructors
    //

    /**
     * Constructs an input source from just the public and system
     * identifiers, leaving resolution of the entity and opening of
     * the input stream up to the caller.
     *
     * @param publicId     The public identifier, if known.
     * @param systemId     The system identifier. This value should
     *                     always be set, if possible, and can be
     *                     relative or absolute. If the system identifier
     *                     is relative, then the base system identifier
     *                     should be set.
     * @param baseSystemId The base system identifier. This value should
     *                     always be set to the fully expanded URI of the
     *                     base system identifier, if possible.
     * @param isCreatedByResolver a flag to indicate whether the source is
     * created by a resolver
     */
    public XMLInputSource(String publicId, String systemId,
                          String baseSystemId, boolean isCreatedByResolver) {
        fPublicId = publicId;
        fSystemId = systemId;
        fBaseSystemId = baseSystemId;
        fIsCreatedByResolver = isCreatedByResolver;
    } // <init>(String,String,String)

    /**
     * Constructs an input source from a XMLResourceIdentifier
     * object, leaving resolution of the entity and opening of
     * the input stream up to the caller.
     *
     * @param resourceIdentifier    the XMLResourceIdentifier containing the information
     */
    public XMLInputSource(XMLResourceIdentifier resourceIdentifier) {

        fPublicId = resourceIdentifier.getPublicId();
        fSystemId = resourceIdentifier.getLiteralSystemId();
        fBaseSystemId = resourceIdentifier.getBaseSystemId();
    } // <init>(XMLResourceIdentifier)

    /**
     * Constructs an input source from a SAX InputSource
     * object.
     *
     * @param inputSource  a SAX InputSource
     * @param isCreatedByResolver a flag to indicate whether the source is
     * created by a resolver
     */
    public XMLInputSource(InputSource inputSource, boolean isCreatedByResolver) {
        fPublicId = inputSource.getPublicId();
        fSystemId = inputSource.getSystemId();
        fByteStream = inputSource.getByteStream();
        fCharStream = inputSource.getCharacterStream();
        fEncoding = inputSource.getEncoding();
        fIsCreatedByResolver = isCreatedByResolver;
    }

    /**
     * Constructs an input source from a byte stream.
     *
     * @param publicId     The public identifier, if known.
     * @param systemId     The system identifier. This value should
     *                     always be set, if possible, and can be
     *                     relative or absolute. If the system identifier
     *                     is relative, then the base system identifier
     *                     should be set.
     * @param baseSystemId The base system identifier. This value should
     *                     always be set to the fully expanded URI of the
     *                     base system identifier, if possible.
     * @param byteStream   The byte stream.
     * @param encoding     The encoding of the byte stream, if known.
     */
    public XMLInputSource(String publicId, String systemId,
                          String baseSystemId, InputStream byteStream,
                          String encoding) {
        fPublicId = publicId;
        fSystemId = systemId;
        fBaseSystemId = baseSystemId;
        fByteStream = byteStream;
        fEncoding = encoding;
    } // <init>(String,String,String,InputStream,String)

    /**
     * Constructs an input source from a character stream.
     *
     * @param publicId     The public identifier, if known.
     * @param systemId     The system identifier. This value should
     *                     always be set, if possible, and can be
     *                     relative or absolute. If the system identifier
     *                     is relative, then the base system identifier
     *                     should be set.
     * @param baseSystemId The base system identifier. This value should
     *                     always be set to the fully expanded URI of the
     *                     base system identifier, if possible.
     * @param charStream   The character stream.
     * @param encoding     The original encoding of the byte stream
     *                     used by the reader, if known.
     */
    public XMLInputSource(String publicId, String systemId,
                          String baseSystemId, Reader charStream,
                          String encoding) {
        fPublicId = publicId;
        fSystemId = systemId;
        fBaseSystemId = baseSystemId;
        fCharStream = charStream;
        fEncoding = encoding;
    } // <init>(String,String,String,Reader,String)

    //
    // Public methods
    //

    /**
     * Sets the public identifier.
     *
     * @param publicId The new public identifier.
     */
    public void setPublicId(String publicId) {
        fPublicId = publicId;
    } // setPublicId(String)

    /** Returns the public identifier. */
    public String getPublicId() {
        return fPublicId;
    } // getPublicId():String

    /**
     * Sets the system identifier.
     *
     * @param systemId The new system identifier.
     */
    public void setSystemId(String systemId) {
        fSystemId = systemId;
    } // setSystemId(String)

    /** Returns the system identifier. */
    public String getSystemId() {
        return fSystemId;
    } // getSystemId():String

    /**
     * Sets the base system identifier.
     *
     * @param baseSystemId The new base system identifier.
     */
    public void setBaseSystemId(String baseSystemId) {
        fBaseSystemId = baseSystemId;
    } // setBaseSystemId(String)

    /** Returns the base system identifier. */
    public String getBaseSystemId() {
        return fBaseSystemId;
    } // getBaseSystemId():String

    /**
     * Sets the byte stream. If the byte stream is not already opened
     * when this object is instantiated, then the code that opens the
     * stream should also set the byte stream on this object. Also, if
     * the encoding is auto-detected, then the encoding should also be
     * set on this object.
     *
     * @param byteStream The new byte stream.
     */
    public void setByteStream(InputStream byteStream) {
        fByteStream = byteStream;
    } // setByteStream(InputSource)

    /** Returns the byte stream. */
    public InputStream getByteStream() {
        return fByteStream;
    } // getByteStream():InputStream

    /**
     * Sets the character stream. If the character stream is not already
     * opened when this object is instantiated, then the code that opens
     * the stream should also set the character stream on this object.
     * Also, the encoding of the byte stream used by the reader should
     * also be set on this object, if known.
     *
     * @param charStream The new character stream.
     *
     * @see #setEncoding
     */
    public void setCharacterStream(Reader charStream) {
        fCharStream = charStream;
    } // setCharacterStream(Reader)

    /** Returns the character stream. */
    public Reader getCharacterStream() {
        return fCharStream;
    } // getCharacterStream():Reader

    /**
     * Sets the encoding of the stream.
     *
     * @param encoding The new encoding.
     */
    public void setEncoding(String encoding) {
        fEncoding = encoding;
    } // setEncoding(String)

    /** Returns the encoding of the stream, or null if not known. */
    public String getEncoding() {
        return fEncoding;
    } // getEncoding():String

    /**
     * Sets the flag to indicate whether this source is created by a resolver
     * @param createdByResolver the flag
     */
    public void setCreatedByResolver(boolean createdByResolver) {
        fIsCreatedByResolver = createdByResolver;
    }
    /**
     * Returns a boolean to indicate whether this source is created by a resolver.
     * @return true if the source is created by a resolver, false otherwise
     */
    public boolean isCreatedByResolver() {
        return fIsCreatedByResolver;
    }

} // class XMLInputSource
