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

import java.io.InputStream;
import java.io.Reader;

import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * <p>An <code>XMLInputSource</code> analogue to <code>javax.xml.transform.sax.SAXSource</code>.</p>
 *
 */
public final class SAXInputSource extends XMLInputSource {

    private XMLReader fXMLReader;
    private InputSource fInputSource;

    public SAXInputSource() {
        this(null);
    }

    public SAXInputSource(InputSource inputSource) {
        this(null, inputSource);
    }

    public SAXInputSource(XMLReader reader, InputSource inputSource) {
        super(inputSource != null ? inputSource.getPublicId() : null,
                inputSource != null ? inputSource.getSystemId() : null, null,
                false);
        if (inputSource != null) {
            setByteStream(inputSource.getByteStream());
            setCharacterStream(inputSource.getCharacterStream());
            setEncoding(inputSource.getEncoding());
        }
        fInputSource = inputSource;
        fXMLReader = reader;
    }

    public void setXMLReader(XMLReader reader) {
        fXMLReader = reader;
    }

    public XMLReader getXMLReader() {
        return fXMLReader;
    }

    public void setInputSource(InputSource inputSource) {
        if (inputSource != null) {
            setPublicId(inputSource.getPublicId());
            setSystemId(inputSource.getSystemId());
            setByteStream(inputSource.getByteStream());
            setCharacterStream(inputSource.getCharacterStream());
            setEncoding(inputSource.getEncoding());
        }
        else {
            setPublicId(null);
            setSystemId(null);
            setByteStream(null);
            setCharacterStream(null);
            setEncoding(null);
        }
        fInputSource = inputSource;
    }

    public InputSource getInputSource() {
        return fInputSource;
    }

    /**
     * Sets the public identifier.
     *
     * @param publicId The new public identifier.
     */
    public void setPublicId(String publicId) {
        super.setPublicId(publicId);
        if (fInputSource == null) {
            fInputSource = new InputSource();
        }
        fInputSource.setPublicId(publicId);
    } // setPublicId(String)

    /**
     * Sets the system identifier.
     *
     * @param systemId The new system identifier.
     */
    public void setSystemId(String systemId) {
        super.setSystemId(systemId);
        if (fInputSource == null) {
            fInputSource = new InputSource();
        }
        fInputSource.setSystemId(systemId);
    } // setSystemId(String)

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
        super.setByteStream(byteStream);
        if (fInputSource == null) {
            fInputSource = new InputSource();
        }
        fInputSource.setByteStream(byteStream);
    } // setByteStream(InputStream)

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
        super.setCharacterStream(charStream);
        if (fInputSource == null) {
            fInputSource = new InputSource();
        }
        fInputSource.setCharacterStream(charStream);
    } // setCharacterStream(Reader)

    /**
     * Sets the encoding of the stream.
     *
     * @param encoding The new encoding.
     */
    public void setEncoding(String encoding) {
        super.setEncoding(encoding);
        if (fInputSource == null) {
            fInputSource = new InputSource();
        }
        fInputSource.setEncoding(encoding);
    } // setEncoding(String)

} // SAXInputSource
