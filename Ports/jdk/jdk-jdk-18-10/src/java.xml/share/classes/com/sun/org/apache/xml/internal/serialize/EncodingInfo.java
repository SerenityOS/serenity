/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xml.internal.serialize;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import com.sun.org.apache.xerces.internal.util.EncodingMap;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * This class represents an encoding.
 *
 * @deprecated As of JDK 9, Xerces 2.9.0, Xerces DOM L3 Serializer implementation
 * is replaced by that of Xalan. Main class
 * {@link com.sun.org.apache.xml.internal.serialize.DOMSerializerImpl} is replaced
 * by {@link com.sun.org.apache.xml.internal.serializer.dom3.LSSerializerImpl}.
 */
@Deprecated
public class EncodingInfo {

    // name of encoding as registered with IANA;
    // preferably a MIME name, but aliases are fine too.
    String ianaName;
    String javaName;
    int lastPrintable;

    // The CharsetEncoder with which we test unusual characters.
    CharsetEncoder fCharsetEncoder = null;

    // Is the charset encoder usable or available.
    boolean fHaveTriedCharsetEncoder = false;

    /**
     * Creates new <code>EncodingInfo</code> instance.
     */
    public EncodingInfo(String ianaName, String javaName, int lastPrintable) {
        this.ianaName = ianaName;
        this.javaName = EncodingMap.getIANA2JavaMapping(ianaName);
        this.lastPrintable = lastPrintable;
    }

    /**
     * Returns a MIME charset name of this encoding.
     */
    public String getIANAName() {
        return this.ianaName;
    }

    /**
     * Returns a writer for this encoding based on
     * an output stream.
     *
     * @return A suitable writer
     * @exception UnsupportedEncodingException There is no convertor
     *  to support this encoding
     */
    public Writer getWriter(OutputStream output)
        throws UnsupportedEncodingException {
        // this should always be true!
        if (javaName != null)
            return new OutputStreamWriter(output, javaName);
        javaName = EncodingMap.getIANA2JavaMapping(ianaName);
        if(javaName == null)
            // use UTF-8 as preferred encoding
            return new OutputStreamWriter(output, "UTF8");
        return new OutputStreamWriter(output, javaName);
    }

    /**
     * Checks whether the specified character is printable or not in this encoding.
     *
     * @param ch a code point (0-0x10ffff)
     */
    public boolean isPrintable(char ch) {
        if (ch <= this.lastPrintable) {
            return true;
        }
        return isPrintable0(ch);
    }

    /**
     * Checks whether the specified character is printable or not in this encoding.
     * This method accomplishes this using a java.nio.CharsetEncoder. If NIO isn't
     * available it will attempt use a sun.io.CharToByteConverter.
     *
     * @param ch a code point (0-0x10ffff)
     */
    private boolean isPrintable0(char ch) {

        // Attempt to get a CharsetEncoder for this encoding.
        if (fCharsetEncoder == null && !fHaveTriedCharsetEncoder) {
            // try and create the CharsetEncoder
            try {
                Charset charset = java.nio.charset.Charset.forName(javaName);
                if (charset.canEncode()) {
                    fCharsetEncoder = charset.newEncoder();
                }
                // This charset cannot be used for encoding, don't try it again...
                else {
                    fHaveTriedCharsetEncoder = true;
                }
            }
            catch (Exception e) {
                // don't try it again...
                fHaveTriedCharsetEncoder = true;
            }
        }
        // Attempt to use the CharsetEncoder to determine whether the character is printable.
        if (fCharsetEncoder != null) {
            try {
                return fCharsetEncoder.canEncode(ch);
            }
            catch (Exception e) {
                // obviously can't use this charset encoder; possibly a JDK bug
                fCharsetEncoder = null;
                fHaveTriedCharsetEncoder = false;
            }
        }

        return false;
    }

    // is this an encoding name recognized by this JDK?
    // if not, will throw UnsupportedEncodingException
    public static void testJavaEncodingName(String name)  throws UnsupportedEncodingException {
        final byte [] bTest = {(byte)'v', (byte)'a', (byte)'l', (byte)'i', (byte)'d'};
        String s = new String(bTest, name);
    }

}
