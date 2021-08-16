/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
 */

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.impl;

import com.sun.org.apache.xerces.internal.impl.XMLScanner.NameType;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.util.XML11Char;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.util.XMLStringBuffer;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityManager.Limit;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import java.io.IOException;

/**
 * Implements the entity scanner methods in
 * the context of XML 1.1.
 *
 * @xerces.internal
 *
 * @author Michael Glavassevich, IBM
 * @author Neil Graham, IBM
 *
 * @LastModified: Apr 2021
 */

public class XML11EntityScanner
    extends XMLEntityScanner {

    //
    // Constructors
    //

    /** Default constructor. */
    public XML11EntityScanner() {
        super();
    } // <init>()

    //
    // XMLEntityScanner methods
    //

    /**
     * Returns the next character on the input.
     * <p>
     * <strong>Note:</strong> The character is <em>not</em> consumed.
     *
     * @throws IOException  Thrown if i/o error occurs.
     * @throws EOFException Thrown on end of file.
     */
    public int peekChar() throws IOException {

        // load more characters, if needed
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true, true);
        }

        // peek at character
        int c = fCurrentEntity.ch[fCurrentEntity.position];

        // return peeked character
        if (fCurrentEntity.isExternal()) {
            return (c != '\r' && c != 0x85 && c != 0x2028) ? c : '\n';
        }
        else {
            return c;
        }

    } // peekChar():int

    /**
     * Returns the next character on the input.
     * <p>
     * <strong>Note:</strong> The character is consumed.
     *
     * @throws IOException  Thrown if i/o error occurs.
     * @throws EOFException Thrown on end of file.
     */
    protected int scanChar(NameType nt) throws IOException {

        // load more characters, if needed
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true, true);
        }

        // scan character
        int offset = fCurrentEntity.position;
        int c = fCurrentEntity.ch[fCurrentEntity.position++];
        boolean external = false;
        if (c == '\n' ||
            ((c == '\r' || c == 0x85 || c == 0x2028) && (external = fCurrentEntity.isExternal()))) {
            fCurrentEntity.lineNumber++;
            fCurrentEntity.columnNumber = 1;
            if (fCurrentEntity.position == fCurrentEntity.count) {
                invokeListeners(1);
                fCurrentEntity.ch[0] = (char)c;
                load(1, false, false);
                offset = 0;
            }
            if (c == '\r' && external) {
                int cc = fCurrentEntity.ch[fCurrentEntity.position++];
                if (cc != '\n' && cc != 0x85) {
                    fCurrentEntity.position--;
                }
            }
            c = '\n';
        }

        // return character that was scanned
        fCurrentEntity.columnNumber++;
        if (!detectingVersion) {
            checkEntityLimit(nt, fCurrentEntity, offset, fCurrentEntity.position - offset);
        }
        return c;

    } // scanChar():int

    /**
     * Returns a string matching the NMTOKEN production appearing immediately
     * on the input as a symbol, or null if NMTOKEN Name string is present.
     * <p>
     * <strong>Note:</strong> The NMTOKEN characters are consumed.
     * <p>
     * <strong>Note:</strong> The string returned must be a symbol. The
     * SymbolTable can be used for this purpose.
     *
     * @throws IOException  Thrown if i/o error occurs.
     * @throws EOFException Thrown on end of file.
     *
     * @see com.sun.org.apache.xerces.internal.util.SymbolTable
     * @see com.sun.org.apache.xerces.internal.util.XML11Char#isXML11Name
     */
    protected String scanNmtoken() throws IOException {
        // load more characters, if needed
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true, true);
        }

        // scan nmtoken
        int offset = fCurrentEntity.position;

        do {
            char ch = fCurrentEntity.ch[fCurrentEntity.position];
            if (XML11Char.isXML11Name(ch)) {
                if (++fCurrentEntity.position == fCurrentEntity.count) {
                    int length = fCurrentEntity.position - offset;
                    invokeListeners(length);
                    if (length == fCurrentEntity.ch.length) {
                        // bad luck we have to resize our buffer
                        char[] tmp = new char[fCurrentEntity.ch.length << 1];
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         tmp, 0, length);
                        fCurrentEntity.ch = tmp;
                    }
                    else {
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         fCurrentEntity.ch, 0, length);
                    }
                    offset = 0;
                    if (load(length, false, false)) {
                        break;
                    }
                }
            }
            else if (XML11Char.isXML11NameHighSurrogate(ch)) {
                if (++fCurrentEntity.position == fCurrentEntity.count) {
                    int length = fCurrentEntity.position - offset;
                    invokeListeners(length);
                    if (length == fCurrentEntity.ch.length) {
                        // bad luck we have to resize our buffer
                        char[] tmp = new char[fCurrentEntity.ch.length << 1];
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         tmp, 0, length);
                        fCurrentEntity.ch = tmp;
                    }
                    else {
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         fCurrentEntity.ch, 0, length);
                    }
                    offset = 0;
                    if (load(length, false, false)) {
                        --fCurrentEntity.startPosition;
                        --fCurrentEntity.position;
                        break;
                    }
                }
                char ch2 = fCurrentEntity.ch[fCurrentEntity.position];
                if ( !XMLChar.isLowSurrogate(ch2) ||
                     !XML11Char.isXML11Name(XMLChar.supplemental(ch, ch2)) ) {
                    --fCurrentEntity.position;
                    break;
                }
                if (++fCurrentEntity.position == fCurrentEntity.count) {
                    int length = fCurrentEntity.position - offset;
                    invokeListeners(length);
                    if (length == fCurrentEntity.ch.length) {
                        // bad luck we have to resize our buffer
                        char[] tmp = new char[fCurrentEntity.ch.length << 1];
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         tmp, 0, length);
                        fCurrentEntity.ch = tmp;
                    }
                    else {
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         fCurrentEntity.ch, 0, length);
                    }
                    offset = 0;
                    if (load(length, false, false)) {
                        break;
                    }
                }
            }
            else {
                break;
            }
        }
        while (true);

        int length = fCurrentEntity.position - offset;
        fCurrentEntity.columnNumber += length;

        // return nmtoken
        String symbol = null;
        if (length > 0) {
            symbol = fSymbolTable.addSymbol(fCurrentEntity.ch, offset, length);
        }
        return symbol;

    } // scanNmtoken():String

    /**
     * Returns a string matching the Name production appearing immediately
     * on the input as a symbol, or null if no Name string is present.
     * <p>
     * <strong>Note:</strong> The Name characters are consumed.
     * <p>
     * <strong>Note:</strong> The string returned must be a symbol. The
     * SymbolTable can be used for this purpose.
     *
     * @param nt The type of the name (element or attribute)
     *
     * @throws IOException  Thrown if i/o error occurs.
     * @throws EOFException Thrown on end of file.
     *
     * @see com.sun.org.apache.xerces.internal.util.SymbolTable
     * @see com.sun.org.apache.xerces.internal.util.XML11Char#isXML11Name
     * @see com.sun.org.apache.xerces.internal.util.XML11Char#isXML11NameStart
     */
    protected String scanName(NameType nt) throws IOException {
        // load more characters, if needed
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true, true);
        }

        // scan name
        int offset = fCurrentEntity.position;
        char ch = fCurrentEntity.ch[offset];

        if (XML11Char.isXML11NameStart(ch)) {
            if (++fCurrentEntity.position == fCurrentEntity.count) {
                invokeListeners(1);
                fCurrentEntity.ch[0] = ch;
                offset = 0;
                if (load(1, false, false)) {
                    fCurrentEntity.columnNumber++;
                    String symbol = fSymbolTable.addSymbol(fCurrentEntity.ch, 0, 1);
                    return symbol;
                }
            }
        }
        else if (XML11Char.isXML11NameHighSurrogate(ch)) {
            if (++fCurrentEntity.position == fCurrentEntity.count) {
                invokeListeners(1);
                fCurrentEntity.ch[0] = ch;
                offset = 0;
                if (load(1, false, false)) {
                    --fCurrentEntity.position;
                    --fCurrentEntity.startPosition;
                    return null;
                }
            }
            char ch2 = fCurrentEntity.ch[fCurrentEntity.position];
            if ( !XMLChar.isLowSurrogate(ch2) ||
                 !XML11Char.isXML11NameStart(XMLChar.supplemental(ch, ch2)) ) {
                --fCurrentEntity.position;
                return null;
            }
            if (++fCurrentEntity.position == fCurrentEntity.count) {
                invokeListeners(2);
                fCurrentEntity.ch[0] = ch;
                fCurrentEntity.ch[1] = ch2;
                offset = 0;
                if (load(2, false, false)) {
                    fCurrentEntity.columnNumber += 2;
                    String symbol = fSymbolTable.addSymbol(fCurrentEntity.ch, 0, 2);
                    return symbol;
                }
            }
        }
        else {
            return null;
        }

        int length = 0;
        do {
            ch = fCurrentEntity.ch[fCurrentEntity.position];
            if (XML11Char.isXML11Name(ch)) {
                if ((length = checkBeforeLoad(fCurrentEntity, offset, offset)) > 0) {
                    offset = 0;
                    if (load(length, false, false)) {
                        break;
                    }
                }
            }
            else if (XML11Char.isXML11NameHighSurrogate(ch)) {
                if ((length = checkBeforeLoad(fCurrentEntity, offset, offset)) > 0) {
                    offset = 0;
                    if (load(length, false, false)) {
                        --fCurrentEntity.position;
                        --fCurrentEntity.startPosition;
                        break;
                    }
                }
                char ch2 = fCurrentEntity.ch[fCurrentEntity.position];
                if ( !XMLChar.isLowSurrogate(ch2) ||
                     !XML11Char.isXML11Name(XMLChar.supplemental(ch, ch2)) ) {
                    --fCurrentEntity.position;
                    break;
                }
                if ((length = checkBeforeLoad(fCurrentEntity, offset, offset)) > 0) {
                    offset = 0;
                    if (load(length, false, false)) {
                        break;
                    }
                }
            }
            else {
                break;
            }
        }
        while (true);

        length = fCurrentEntity.position - offset;
        fCurrentEntity.columnNumber += length;

        // return name
        String symbol = null;
        if (length > 0) {
            checkLimit(Limit.MAX_NAME_LIMIT, fCurrentEntity, offset, length);
            checkEntityLimit(nt, fCurrentEntity, offset, length);
            symbol = fSymbolTable.addSymbol(fCurrentEntity.ch, offset, length);
        }
        return symbol;

    } // scanName():String

    /**
     * Returns a string matching the NCName production appearing immediately
     * on the input as a symbol, or null if no NCName string is present.
     * <p>
     * <strong>Note:</strong> The NCName characters are consumed.
     * <p>
     * <strong>Note:</strong> The string returned must be a symbol. The
     * SymbolTable can be used for this purpose.
     *
     * @throws IOException  Thrown if i/o error occurs.
     * @throws EOFException Thrown on end of file.
     *
     * @see com.sun.org.apache.xerces.internal.util.SymbolTable
     * @see com.sun.org.apache.xerces.internal.util.XML11Char#isXML11NCName
     * @see com.sun.org.apache.xerces.internal.util.XML11Char#isXML11NCNameStart
     */
    protected String scanNCName() throws IOException {

        // load more characters, if needed
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true, true);
        }

        // scan name
        int offset = fCurrentEntity.position;
        char ch = fCurrentEntity.ch[offset];

        if (XML11Char.isXML11NCNameStart(ch)) {
            if (++fCurrentEntity.position == fCurrentEntity.count) {
                invokeListeners(1);
                fCurrentEntity.ch[0] = ch;
                offset = 0;
                if (load(1, false, false)) {
                    fCurrentEntity.columnNumber++;
                    String symbol = fSymbolTable.addSymbol(fCurrentEntity.ch, 0, 1);
                    return symbol;
                }
            }
        }
        else if (XML11Char.isXML11NameHighSurrogate(ch)) {
            if (++fCurrentEntity.position == fCurrentEntity.count) {
                invokeListeners(1);
                fCurrentEntity.ch[0] = ch;
                offset = 0;
                if (load(1, false, false)) {
                    --fCurrentEntity.position;
                    --fCurrentEntity.startPosition;
                    return null;
                }
            }
            char ch2 = fCurrentEntity.ch[fCurrentEntity.position];
            if ( !XMLChar.isLowSurrogate(ch2) ||
                 !XML11Char.isXML11NCNameStart(XMLChar.supplemental(ch, ch2)) ) {
                --fCurrentEntity.position;
                return null;
            }
            if (++fCurrentEntity.position == fCurrentEntity.count) {
                invokeListeners(2);
                fCurrentEntity.ch[0] = ch;
                fCurrentEntity.ch[1] = ch2;
                offset = 0;
                if (load(2, false, false)) {
                    fCurrentEntity.columnNumber += 2;
                    String symbol = fSymbolTable.addSymbol(fCurrentEntity.ch, 0, 2);
                    return symbol;
                }
            }
        }
        else {
            return null;
        }

        do {
            ch = fCurrentEntity.ch[fCurrentEntity.position];
            if (XML11Char.isXML11NCName(ch)) {
                if (++fCurrentEntity.position == fCurrentEntity.count) {
                    int length = fCurrentEntity.position - offset;
                    invokeListeners(length);
                    if (length == fCurrentEntity.ch.length) {
                        // bad luck we have to resize our buffer
                        char[] tmp = new char[fCurrentEntity.ch.length << 1];
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         tmp, 0, length);
                        fCurrentEntity.ch = tmp;
                    }
                    else {
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         fCurrentEntity.ch, 0, length);
                    }
                    offset = 0;
                    if (load(length, false, false)) {
                        break;
                    }
                }
            }
            else if (XML11Char.isXML11NameHighSurrogate(ch)) {
                if (++fCurrentEntity.position == fCurrentEntity.count) {
                    int length = fCurrentEntity.position - offset;
                    invokeListeners(length);
                    if (length == fCurrentEntity.ch.length) {
                        // bad luck we have to resize our buffer
                        char[] tmp = new char[fCurrentEntity.ch.length << 1];
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         tmp, 0, length);
                        fCurrentEntity.ch = tmp;
                    }
                    else {
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         fCurrentEntity.ch, 0, length);
                    }
                    offset = 0;
                    if (load(length, false, false)) {
                        --fCurrentEntity.startPosition;
                        --fCurrentEntity.position;
                        break;
                    }
                }
                char ch2 = fCurrentEntity.ch[fCurrentEntity.position];
                if ( !XMLChar.isLowSurrogate(ch2) ||
                     !XML11Char.isXML11NCName(XMLChar.supplemental(ch, ch2)) ) {
                    --fCurrentEntity.position;
                    break;
                }
                if (++fCurrentEntity.position == fCurrentEntity.count) {
                    int length = fCurrentEntity.position - offset;
                    invokeListeners(length);
                    if (length == fCurrentEntity.ch.length) {
                        // bad luck we have to resize our buffer
                        char[] tmp = new char[fCurrentEntity.ch.length << 1];
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         tmp, 0, length);
                        fCurrentEntity.ch = tmp;
                    }
                    else {
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         fCurrentEntity.ch, 0, length);
                    }
                    offset = 0;
                    if (load(length, false, false)) {
                        break;
                    }
                }
            }
            else {
                break;
            }
        }
        while (true);

        int length = fCurrentEntity.position - offset;
        fCurrentEntity.columnNumber += length;

        // return name
        String symbol = null;
        if (length > 0) {
            symbol = fSymbolTable.addSymbol(fCurrentEntity.ch, offset, length);
        }
        return symbol;

    } // scanNCName():String

    /**
     * Scans a qualified name from the input, setting the fields of the
     * QName structure appropriately.
     * <p>
     * <strong>Note:</strong> The qualified name characters are consumed.
     * <p>
     * <strong>Note:</strong> The strings used to set the values of the
     * QName structure must be symbols. The SymbolTable can be used for
     * this purpose.
     *
     * @param qname The qualified name structure to fill.
     * @param nt The type of the name (element or attribute)
     *
     * @return Returns true if a qualified name appeared immediately on
     *         the input and was scanned, false otherwise.
     *
     * @throws IOException  Thrown if i/o error occurs.
     * @throws EOFException Thrown on end of file.
     *
     * @see com.sun.org.apache.xerces.internal.util.SymbolTable
     * @see com.sun.org.apache.xerces.internal.util.XML11Char#isXML11Name
     * @see com.sun.org.apache.xerces.internal.util.XML11Char#isXML11NameStart
     */
    protected boolean scanQName(QName qname, XMLScanner.NameType nt) throws IOException {

        // load more characters, if needed
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true, true);
        }

        // scan qualified name
        int offset = fCurrentEntity.position;
        char ch = fCurrentEntity.ch[offset];

        if (XML11Char.isXML11NCNameStart(ch)) {
            if (++fCurrentEntity.position == fCurrentEntity.count) {
                invokeListeners(1);
                fCurrentEntity.ch[0] = ch;
                offset = 0;
                if (load(1, false, false)) {
                    fCurrentEntity.columnNumber++;
                    String name = fSymbolTable.addSymbol(fCurrentEntity.ch, 0, 1);
                    qname.setValues(null, name, name, null);
                    checkEntityLimit(nt, fCurrentEntity, 0, 1);
                    return true;
                }
            }
        }
        else if (XML11Char.isXML11NameHighSurrogate(ch)) {
            if (++fCurrentEntity.position == fCurrentEntity.count) {
                invokeListeners(1);
                fCurrentEntity.ch[0] = ch;
                offset = 0;
                if (load(1, false, false)) {
                    --fCurrentEntity.startPosition;
                    --fCurrentEntity.position;
                    return false;
                }
            }
            char ch2 = fCurrentEntity.ch[fCurrentEntity.position];
            if ( !XMLChar.isLowSurrogate(ch2) ||
                 !XML11Char.isXML11NCNameStart(XMLChar.supplemental(ch, ch2)) ) {
                --fCurrentEntity.position;
                return false;
            }
            if (++fCurrentEntity.position == fCurrentEntity.count) {
                invokeListeners(2);
                fCurrentEntity.ch[0] = ch;
                fCurrentEntity.ch[1] = ch2;
                offset = 0;
                if (load(2, false, false)) {
                    fCurrentEntity.columnNumber += 2;
                    String name = fSymbolTable.addSymbol(fCurrentEntity.ch, 0, 2);
                    qname.setValues(null, name, name, null);
                    checkEntityLimit(nt, fCurrentEntity, 0, 2);
                    return true;
                }
            }
        }
        else {
            return false;
        }

        int index = -1;
        int length = 0;
        boolean sawIncompleteSurrogatePair = false;
        do {
            ch = fCurrentEntity.ch[fCurrentEntity.position];
            if (XML11Char.isXML11Name(ch)) {
                if (ch == ':') {
                    if (index != -1) {
                        break;
                    }
                    index = fCurrentEntity.position;
                    //check prefix before further read
                    checkLimit(Limit.MAX_NAME_LIMIT, fCurrentEntity, offset, index - offset);
                }
                if ((length = checkBeforeLoad(fCurrentEntity, offset, index)) > 0) {
                    if (index != -1) {
                        index = index - offset;
                    }
                    offset = 0;
                    if (load(length, false, false)) {
                        break;
                    }
                }
            }
            else if (XML11Char.isXML11NameHighSurrogate(ch)) {
                if ((length = checkBeforeLoad(fCurrentEntity, offset, index)) > 0) {
                    if (index != -1) {
                        index = index - offset;
                    }
                    offset = 0;
                    if (load(length, false, false)) {
                        sawIncompleteSurrogatePair = true;
                        --fCurrentEntity.startPosition;
                        --fCurrentEntity.position;
                        break;
                    }
                }
                char ch2 = fCurrentEntity.ch[fCurrentEntity.position];
                if ( !XMLChar.isLowSurrogate(ch2) ||
                     !XML11Char.isXML11Name(XMLChar.supplemental(ch, ch2)) ) {
                    sawIncompleteSurrogatePair = true;
                    --fCurrentEntity.position;
                    break;
                }
                if ((length = checkBeforeLoad(fCurrentEntity, offset, index)) > 0) {
                    if (index != -1) {
                        index = index - offset;
                    }
                    offset = 0;
                    if (load(length, false, false)) {
                        break;
                    }
                }
            }
            else {
                break;
            }
        }
        while (true);

        length = fCurrentEntity.position - offset;
        fCurrentEntity.columnNumber += length;

        if (length > 0) {
            String prefix = null;
            String localpart = null;
            String rawname = fSymbolTable.addSymbol(fCurrentEntity.ch,
                                                    offset, length);
            if (index != -1) {
                int prefixLength = index - offset;
                //check the result: prefix
                checkLimit(Limit.MAX_NAME_LIMIT, fCurrentEntity, offset, prefixLength);
                prefix = fSymbolTable.addSymbol(fCurrentEntity.ch,
                                                    offset, prefixLength);
                int len = length - prefixLength - 1;
                int startLocal = index +1;
                if (!XML11Char.isXML11NCNameStart(fCurrentEntity.ch[startLocal]) &&
                    (!XML11Char.isXML11NameHighSurrogate(fCurrentEntity.ch[startLocal]) ||
                    sawIncompleteSurrogatePair)){
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                               "IllegalQName",
                                               new Object[]{rawname},
                                               XMLErrorReporter.SEVERITY_FATAL_ERROR);
                }
                //check the result: localpart
                checkLimit(Limit.MAX_NAME_LIMIT, fCurrentEntity, index + 1, len);
                localpart = fSymbolTable.addSymbol(fCurrentEntity.ch,
                                                   index + 1, len);

            }
            else {
                localpart = rawname;
                //check the result: localpart
                checkLimit(Limit.MAX_NAME_LIMIT, fCurrentEntity, offset, length);
            }
            qname.setValues(prefix, localpart, rawname, null);
            checkEntityLimit(nt, fCurrentEntity, offset, length);
            return true;
        }
        return false;

    } // scanQName(QName):boolean

    /**
     * Scans a range of parsed character data, setting the fields of the
     * XMLString structure, appropriately.
     * <p>
     * <strong>Note:</strong> The characters are consumed.
     * <p>
     * <strong>Note:</strong> This method does not guarantee to return
     * the longest run of parsed character data. This method may return
     * before markup due to reaching the end of the input buffer or any
     * other reason.
     * <p>
     * <strong>Note:</strong> The fields contained in the XMLString
     * structure are not guaranteed to remain valid upon subsequent calls
     * to the entity scanner. Therefore, the caller is responsible for
     * immediately using the returned character data or making a copy of
     * the character data.
     *
     * @param content The content structure to fill.
     *
     * @return Returns the next character on the input, if known. This
     *         value may be -1 but this does <em>note</em> designate
     *         end of file.
     *
     * @throws IOException  Thrown if i/o error occurs.
     * @throws EOFException Thrown on end of file.
     */
    protected int scanContent(XMLString content) throws IOException {

        // load more characters, if needed
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true, true);
        }
        else if (fCurrentEntity.position == fCurrentEntity.count - 1) {
            invokeListeners(1);
            fCurrentEntity.ch[0] = fCurrentEntity.ch[fCurrentEntity.count - 1];
            load(1, false, false);
            fCurrentEntity.position = 0;
            fCurrentEntity.startPosition = 0;
        }

        // normalize newlines
        int offset = fCurrentEntity.position;
        int c = fCurrentEntity.ch[offset];
        int newlines = 0;
        boolean counted = false;
        boolean external = fCurrentEntity.isExternal();
        if (c == '\n' || ((c == '\r' || c == 0x85 || c == 0x2028) && external)) {
            do {
                c = fCurrentEntity.ch[fCurrentEntity.position++];
                if ((c == '\r' ) && external) {
                    newlines++;
                    fCurrentEntity.lineNumber++;
                    fCurrentEntity.columnNumber = 1;
                    if (fCurrentEntity.position == fCurrentEntity.count) {
                        checkEntityLimit(null, fCurrentEntity, offset, newlines);
                        offset = 0;
                        fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition);
                        fCurrentEntity.position = newlines;
                        fCurrentEntity.startPosition = newlines;
                        if (load(newlines, false, true)) {
                            counted = true;
                            break;
                        }
                    }
                    int cc = fCurrentEntity.ch[fCurrentEntity.position];
                    if (cc == '\n' || cc == 0x85) {
                        fCurrentEntity.position++;
                        offset++;
                    }
                    /*** NEWLINE NORMALIZATION ***/
                    else {
                        newlines++;
                    }
                }
                else if (c == '\n' || ((c == 0x85 || c == 0x2028) && external)) {
                    newlines++;
                    fCurrentEntity.lineNumber++;
                    fCurrentEntity.columnNumber = 1;
                    if (fCurrentEntity.position == fCurrentEntity.count) {
                        checkEntityLimit(null, fCurrentEntity, offset, newlines);
                        offset = 0;
                        fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition);
                        fCurrentEntity.position = newlines;
                        fCurrentEntity.startPosition = newlines;
                        if (load(newlines, false, true)) {
                            counted = true;
                            break;
                        }
                    }
                }
                else {
                    fCurrentEntity.position--;
                    break;
                }
            } while (fCurrentEntity.position < fCurrentEntity.count - 1);
            for (int i = offset; i < fCurrentEntity.position; i++) {
                fCurrentEntity.ch[i] = '\n';
            }
            int length = fCurrentEntity.position - offset;
            if (fCurrentEntity.position == fCurrentEntity.count - 1) {
                checkEntityLimit(null, fCurrentEntity, offset, length);
                content.setValues(fCurrentEntity.ch, offset, length);
                return -1;
            }
        }

        // inner loop, scanning for content
        if (external) {
            while (fCurrentEntity.position < fCurrentEntity.count) {
                c = fCurrentEntity.ch[fCurrentEntity.position++];
                if (!XML11Char.isXML11Content(c) || c == 0x85 || c == 0x2028) {
                    fCurrentEntity.position--;
                    break;
                }
            }
        }
        else {
            while (fCurrentEntity.position < fCurrentEntity.count) {
                c = fCurrentEntity.ch[fCurrentEntity.position++];
                // In internal entities control characters are allowed to appear unescaped.
                if (!XML11Char.isXML11InternalEntityContent(c)) {
                    fCurrentEntity.position--;
                    break;
                }
            }
        }
        int length = fCurrentEntity.position - offset;
        fCurrentEntity.columnNumber += length - newlines;
        if (!counted) {
            checkEntityLimit(null, fCurrentEntity, offset, length);
        }
        content.setValues(fCurrentEntity.ch, offset, length);

        // return next character
        if (fCurrentEntity.position != fCurrentEntity.count) {
            c = fCurrentEntity.ch[fCurrentEntity.position];
            // REVISIT: Does this need to be updated to fix the
            //          #x0D ^#x0A newline normalization problem? -Ac
            if ((c == '\r' || c == 0x85 || c == 0x2028) && external) {
                c = '\n';
            }
        }
        else {
            c = -1;
        }
        return c;

    } // scanContent(XMLString):int

    /**
     * Scans a range of attribute value data, setting the fields of the
     * XMLString structure, appropriately.
     * <p>
     * <strong>Note:</strong> The characters are consumed.
     * <p>
     * <strong>Note:</strong> This method does not guarantee to return
     * the longest run of attribute value data. This method may return
     * before the quote character due to reaching the end of the input
     * buffer or any other reason.
     * <p>
     * <strong>Note:</strong> The fields contained in the XMLString
     * structure are not guaranteed to remain valid upon subsequent calls
     * to the entity scanner. Therefore, the caller is responsible for
     * immediately using the returned character data or making a copy of
     * the character data.
     *
     * @param quote   The quote character that signifies the end of the
     *                attribute value data.
     * @param content The content structure to fill.
     * @param isNSURI a flag indicating whether the content is a Namespace URI
     *
     * @return Returns the next character on the input, if known. This
     *         value may be -1 but this does <em>note</em> designate
     *         end of file.
     *
     * @throws IOException  Thrown if i/o error occurs.
     * @throws EOFException Thrown on end of file.
     */
    protected int scanLiteral(int quote, XMLString content, boolean isNSURI)
        throws IOException {
        // load more characters, if needed
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true, true);
        }
        else if (fCurrentEntity.position == fCurrentEntity.count - 1) {
            invokeListeners(1);
            fCurrentEntity.ch[0] = fCurrentEntity.ch[fCurrentEntity.count - 1];
            load(1, false, false);
            fCurrentEntity.startPosition = 0;
            fCurrentEntity.position = 0;
        }

        // normalize newlines
        int offset = fCurrentEntity.position;
        int c = fCurrentEntity.ch[offset];
        int newlines = 0;
        boolean external = fCurrentEntity.isExternal();
        if (c == '\n' || ((c == '\r' || c == 0x85 || c == 0x2028) && external)) {
            do {
                c = fCurrentEntity.ch[fCurrentEntity.position++];
                if ((c == '\r' ) && external) {
                    newlines++;
                    fCurrentEntity.lineNumber++;
                    fCurrentEntity.columnNumber = 1;
                    if (fCurrentEntity.position == fCurrentEntity.count) {
                        offset = 0;
                        fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition);
                        fCurrentEntity.position = newlines;
                        fCurrentEntity.startPosition = newlines;
                        if (load(newlines, false, true)) {
                            break;
                        }
                    }
                    int cc = fCurrentEntity.ch[fCurrentEntity.position];
                    if (cc == '\n' || cc == 0x85) {
                        fCurrentEntity.position++;
                        offset++;
                    }
                    /*** NEWLINE NORMALIZATION ***/
                    else {
                        newlines++;
                    }
                }
                else if (c == '\n' || ((c == 0x85 || c == 0x2028) && external)) {
                    newlines++;
                    fCurrentEntity.lineNumber++;
                    fCurrentEntity.columnNumber = 1;
                    if (fCurrentEntity.position == fCurrentEntity.count) {
                        offset = 0;
                        fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition);
                        fCurrentEntity.position = newlines;
                        fCurrentEntity.startPosition = newlines;
                        if (load(newlines, false, true)) {
                            break;
                        }
                    }
                }
                else {
                    fCurrentEntity.position--;
                    break;
                }
            } while (fCurrentEntity.position < fCurrentEntity.count - 1);
            for (int i = offset; i < fCurrentEntity.position; i++) {
                fCurrentEntity.ch[i] = '\n';
            }
            int length = fCurrentEntity.position - offset;
            if (fCurrentEntity.position == fCurrentEntity.count - 1) {
                content.setValues(fCurrentEntity.ch, offset, length);
                return -1;
            }
        }

        // scan literal value
        if (external) {
            while (fCurrentEntity.position < fCurrentEntity.count) {
                c = fCurrentEntity.ch[fCurrentEntity.position++];
                if (c == quote || c == '%' || !XML11Char.isXML11Content(c)
                    || c == 0x85 || c == 0x2028) {
                    fCurrentEntity.position--;
                    break;
                }
            }
        }
        else {
            while (fCurrentEntity.position < fCurrentEntity.count) {
                c = fCurrentEntity.ch[fCurrentEntity.position++];
                // In internal entities control characters are allowed to appear unescaped.
                if ((c == quote && !fCurrentEntity.literal)
                    || c == '%' || !XML11Char.isXML11InternalEntityContent(c)) {
                    fCurrentEntity.position--;
                    break;
                }
            }
        }
        int length = fCurrentEntity.position - offset;
        fCurrentEntity.columnNumber += length - newlines;

        checkEntityLimit(null, fCurrentEntity, offset, length);
        if (isNSURI) {
            checkLimit(Limit.MAX_NAME_LIMIT, fCurrentEntity, offset, length);
        }
        content.setValues(fCurrentEntity.ch, offset, length);

        // return next character
        if (fCurrentEntity.position != fCurrentEntity.count) {
            c = fCurrentEntity.ch[fCurrentEntity.position];
            // NOTE: We don't want to accidentally signal the
            //       end of the literal if we're expanding an
            //       entity appearing in the literal. -Ac
            if (c == quote && fCurrentEntity.literal) {
                c = -1;
            }
        }
        else {
            c = -1;
        }
        return c;

    } // scanLiteral(int,XMLString):int

    /**
     * Scans a range of character data up to the specicied delimiter,
     * setting the fields of the XMLString structure, appropriately.
     * <p>
     * <strong>Note:</strong> The characters are consumed.
     * <p>
     * <strong>Note:</strong> This assumes that the internal buffer is
     * at least the same size, or bigger, than the length of the delimiter
     * and that the delimiter contains at least one character.
     * <p>
     * <strong>Note:</strong> This method does not guarantee to return
     * the longest run of character data. This method may return before
     * the delimiter due to reaching the end of the input buffer or any
     * other reason.
     * <p>
     * <strong>Note:</strong> The fields contained in the XMLString
     * structure are not guaranteed to remain valid upon subsequent calls
     * to the entity scanner. Therefore, the caller is responsible for
     * immediately using the returned character data or making a copy of
     * the character data.
     *
     * @param delimiter The string that signifies the end of the character
     *                  data to be scanned.
     * @param buffer      The data structure to fill.
     * @param chunkLimit the size limit of the data to be scanned
     *
     * @return Returns true if there is more data to scan, false otherwise.
     *
     * @throws IOException  Thrown if i/o error occurs.
     */
    protected boolean scanData(String delimiter, XMLStringBuffer buffer, int chunkLimit)
        throws IOException {

        boolean done = false;
        int delimLen = delimiter.length();
        char charAt0 = delimiter.charAt(0);
        boolean external = fCurrentEntity.isExternal();
        do {
            // load more characters, if needed
            if (fCurrentEntity.position == fCurrentEntity.count) {
                load(0, true, false);
            }

            boolean bNextEntity = false;

            while ((fCurrentEntity.position >= fCurrentEntity.count - delimLen)
                && (!bNextEntity))
            {
              System.arraycopy(fCurrentEntity.ch,
                               fCurrentEntity.position,
                               fCurrentEntity.ch,
                               0,
                               fCurrentEntity.count - fCurrentEntity.position);

              bNextEntity = load(fCurrentEntity.count - fCurrentEntity.position, false, false);
              fCurrentEntity.position = 0;
              fCurrentEntity.startPosition = 0;
            }

            if (fCurrentEntity.position >= fCurrentEntity.count - delimLen) {
                // something must be wrong with the input:  e.g., file ends  an unterminated comment
                int length = fCurrentEntity.count - fCurrentEntity.position;
                checkEntityLimit(NameType.COMMENT, fCurrentEntity, fCurrentEntity.position, length);
                buffer.append (fCurrentEntity.ch, fCurrentEntity.position, length);
                fCurrentEntity.columnNumber += fCurrentEntity.count;
                fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition);
                fCurrentEntity.position = fCurrentEntity.count;
                fCurrentEntity.startPosition = fCurrentEntity.count;
                load(0,true, false);
                return false;
            }

            // normalize newlines
            int offset = fCurrentEntity.position;
            int c = fCurrentEntity.ch[offset];
            int newlines = 0;
            if (c == '\n' || ((c == '\r' || c == 0x85 || c == 0x2028) && external)) {
                do {
                    c = fCurrentEntity.ch[fCurrentEntity.position++];
                    if ((c == '\r' ) && external) {
                        newlines++;
                        fCurrentEntity.lineNumber++;
                        fCurrentEntity.columnNumber = 1;
                        if (fCurrentEntity.position == fCurrentEntity.count) {
                            offset = 0;
                            fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition);
                            fCurrentEntity.position = newlines;
                            fCurrentEntity.startPosition = newlines;
                            if (load(newlines, false, true)) {
                                break;
                            }
                        }
                        int cc = fCurrentEntity.ch[fCurrentEntity.position];
                        if (cc == '\n' || cc == 0x85) {
                            fCurrentEntity.position++;
                            offset++;
                        }
                        /*** NEWLINE NORMALIZATION ***/
                        else {
                            newlines++;
                        }
                    }
                    else if (c == '\n' || ((c == 0x85 || c == 0x2028) && external)) {
                        newlines++;
                        fCurrentEntity.lineNumber++;
                        fCurrentEntity.columnNumber = 1;
                        if (fCurrentEntity.position == fCurrentEntity.count) {
                            offset = 0;
                            fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition);
                            fCurrentEntity.position = newlines;
                            fCurrentEntity.startPosition = newlines;
                            fCurrentEntity.count = newlines;
                            if (load(newlines, false, true)) {
                                break;
                            }
                        }
                    }
                    else {
                        fCurrentEntity.position--;
                        break;
                    }
                } while (fCurrentEntity.position < fCurrentEntity.count - 1);
                for (int i = offset; i < fCurrentEntity.position; i++) {
                    fCurrentEntity.ch[i] = '\n';
                }
                int length = fCurrentEntity.position - offset;
                if (fCurrentEntity.position == fCurrentEntity.count - 1) {
                    checkEntityLimit(NameType.COMMENT, fCurrentEntity, offset, length);
                    buffer.append(fCurrentEntity.ch, offset, length);
                    return true;
                }
            }

            // iterate over buffer looking for delimiter
            OUTER: while (fCurrentEntity.position < fCurrentEntity.count) {
                c = fCurrentEntity.ch[fCurrentEntity.position++];
                if (c == charAt0) {
                    // looks like we just hit the delimiter
                    int delimOffset = fCurrentEntity.position - 1;
                    for (int i = 1; i < delimLen; i++) {
                        if (fCurrentEntity.position == fCurrentEntity.count) {
                            fCurrentEntity.position -= i;
                            break OUTER;
                        }
                        c = fCurrentEntity.ch[fCurrentEntity.position++];
                        if (delimiter.charAt(i) != c) {
                            fCurrentEntity.position--;
                            break;
                        }
                     }
                     if (fCurrentEntity.position == delimOffset + delimLen) {
                        done = true;
                        break;
                     }
                }
                else if ((external && (c == '\n' || c == '\r' || c == 0x85 || c == 0x2028))
                        || (!external && c == '\n')) {
                    fCurrentEntity.position--;
                    break;
                }
                // In external entities control characters cannot appear
                // as literals so do not skip over them.
                else if ((external && !XML11Char.isXML11ValidLiteral(c))
                        // Control characters are allowed to appear as literals in internal entities.
                        || (!external && !XML11Char.isXML11Valid(c))) {
                    fCurrentEntity.position--;
                    int length = fCurrentEntity.position - offset;
                    fCurrentEntity.columnNumber += length - newlines;
                    checkEntityLimit(NameType.COMMENT, fCurrentEntity, offset, length);
                    buffer.append(fCurrentEntity.ch, offset, length);
                    return true;
                }
                if (chunkLimit > 0 &&
                        (buffer.length + fCurrentEntity.position - offset) >= chunkLimit) {
                    break;
                }
            }

            int length = fCurrentEntity.position - offset;
            fCurrentEntity.columnNumber += length - newlines;
            checkEntityLimit(NameType.COMMENT, fCurrentEntity, offset, length);
            if (done) {
                length -= delimLen;
            }
            buffer.append(fCurrentEntity.ch, offset, length);

            if (chunkLimit > 0 && buffer.length >= chunkLimit) {
                break;
            }
        } while (!done && chunkLimit == 0);
        return !done;

    } // scanData(String,XMLString)

    /**
     * Skips a character appearing immediately on the input.
     * <p>
     * <strong>Note:</strong> The character is consumed only if it matches
     * the specified character.
     *
     * @param c The character to skip.
     *
     * @return Returns true if the character was skipped.
     *
     * @throws IOException  Thrown if i/o error occurs.
     * @throws EOFException Thrown on end of file.
     */
    protected boolean skipChar(int c, NameType nt) throws IOException {

        // load more characters, if needed
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true, true);
        }

        // skip character
        int offset = fCurrentEntity.position;
        int cc = fCurrentEntity.ch[fCurrentEntity.position];
        if (cc == c) {
            fCurrentEntity.position++;
            if (c == '\n') {
                fCurrentEntity.lineNumber++;
                fCurrentEntity.columnNumber = 1;
            }
            else {
                fCurrentEntity.columnNumber++;
            }
            checkEntityLimit(nt, fCurrentEntity, offset, fCurrentEntity.position - offset);
            return true;
        }
        else if (c == '\n' && ((cc == 0x2028 || cc == 0x85) && fCurrentEntity.isExternal())) {
            fCurrentEntity.position++;
            fCurrentEntity.lineNumber++;
            fCurrentEntity.columnNumber = 1;
            checkEntityLimit(nt, fCurrentEntity, offset, fCurrentEntity.position - offset);
            return true;
        }
        else if (c == '\n' && (cc == '\r' ) && fCurrentEntity.isExternal()) {
            // handle newlines
            if (fCurrentEntity.position == fCurrentEntity.count) {
                invokeListeners(1);
                fCurrentEntity.ch[0] = (char)cc;
                load(1, false, false);
            }
            int ccc = fCurrentEntity.ch[++fCurrentEntity.position];
            if (ccc == '\n' || ccc == 0x85) {
                fCurrentEntity.position++;
            }
            fCurrentEntity.lineNumber++;
            fCurrentEntity.columnNumber = 1;
            checkEntityLimit(nt, fCurrentEntity, offset, fCurrentEntity.position - offset);
            return true;
        }

        // character was not skipped
        return false;

    } // skipChar(int):boolean

    /**
     * Skips space characters appearing immediately on the input.
     * <p>
     * <strong>Note:</strong> The characters are consumed only if they are
     * space characters.
     *
     * @return Returns true if at least one space character was skipped.
     *
     * @throws IOException  Thrown if i/o error occurs.
     * @throws EOFException Thrown on end of file.
     *
     * @see com.sun.org.apache.xerces.internal.util.XMLChar#isSpace
     * @see com.sun.org.apache.xerces.internal.util.XML11Char#isXML11Space
     */
    protected boolean skipSpaces() throws IOException {

        // load more characters, if needed
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true, true);
        }


        //we are doing this check only in skipSpace() because it is called by
        //fMiscDispatcher and we want the parser to exit gracefully when document
        //is well-formed.
        //it is possible that end of document is reached and
        //fCurrentEntity becomes null
        //nothing was read so entity changed  'false' should be returned.
        if(fCurrentEntity == null){
            return false ;
        }

        // skip spaces
        int c = fCurrentEntity.ch[fCurrentEntity.position];
        int offset = fCurrentEntity.position - 1;
        // External --  Match: S + 0x85 + 0x2028, and perform end of line normalization
        if (fCurrentEntity.isExternal()) {
            if (XML11Char.isXML11Space(c)) {
                do {
                    boolean entityChanged = false;
                    // handle newlines
                    if (c == '\n' || c == '\r' || c == 0x85 || c == 0x2028) {
                        fCurrentEntity.lineNumber++;
                        fCurrentEntity.columnNumber = 1;
                        if (fCurrentEntity.position == fCurrentEntity.count - 1) {
                            invokeListeners(1);
                            fCurrentEntity.ch[0] = (char)c;
                            entityChanged = load(1, true, false);
                            if (!entityChanged) {
                                // the load change the position to be 1,
                                // need to restore it when entity not changed
                                fCurrentEntity.startPosition = 0;
                                fCurrentEntity.position = 0;
                            } else if(fCurrentEntity == null){
                                return true ;
                            }

                        }
                        if (c == '\r') {
                            // REVISIT: Does this need to be updated to fix the
                            //          #x0D ^#x0A newline normalization problem? -Ac
                            int cc = fCurrentEntity.ch[++fCurrentEntity.position];
                            if (cc != '\n' && cc != 0x85 ) {
                                fCurrentEntity.position--;
                            }
                        }
                    }
                    else {
                        fCurrentEntity.columnNumber++;
                    }

                    //If this is a general entity, spaces within a start element should be counted
                    checkEntityLimit(null, fCurrentEntity, offset, fCurrentEntity.position - offset);
                    offset = fCurrentEntity.position;

                    // load more characters, if needed
                    if (!entityChanged)
                        fCurrentEntity.position++;
                    if (fCurrentEntity.position == fCurrentEntity.count) {
                        load(0, true, true);

                        if(fCurrentEntity == null){
                        return true ;
                        }

                    }
                } while (XML11Char.isXML11Space(c = fCurrentEntity.ch[fCurrentEntity.position]));
                return true;
            }
        }
        // Internal -- Match: S (only)
        else if (XMLChar.isSpace(c)) {
            do {
                boolean entityChanged = false;
                // handle newlines
                if (c == '\n') {
                    fCurrentEntity.lineNumber++;
                    fCurrentEntity.columnNumber = 1;
                    if (fCurrentEntity.position == fCurrentEntity.count - 1) {
                        invokeListeners(1);
                        fCurrentEntity.ch[0] = (char)c;
                        entityChanged = load(1, true, false);
                        if (!entityChanged) {
                            // the load change the position to be 1,
                            // need to restore it when entity not changed
                            fCurrentEntity.startPosition = 0;
                            fCurrentEntity.position = 0;
                        } else if(fCurrentEntity == null){
                        return true ;
                        }
                    }
                }
                else {
                    fCurrentEntity.columnNumber++;
                }

                //If this is a general entity, spaces within a start element should be counted
                checkEntityLimit(null, fCurrentEntity, offset, fCurrentEntity.position - offset);
                offset = fCurrentEntity.position;

                // load more characters, if needed
                if (!entityChanged)
                    fCurrentEntity.position++;
                if (fCurrentEntity.position == fCurrentEntity.count) {
                    load(0, true, true);

                    if(fCurrentEntity == null){
                        return true ;
                    }

                }
            } while (XMLChar.isSpace(c = fCurrentEntity.ch[fCurrentEntity.position]));
            return true;
        }

        // no spaces were found
        return false;

    } // skipSpaces():boolean

    /**
     * Skips the specified string appearing immediately on the input.
     * <p>
     * <strong>Note:</strong> The characters are consumed only if they are
     * space characters.
     *
     * @param s The string to skip.
     *
     * @return Returns true if the string was skipped.
     *
     * @throws IOException  Thrown if i/o error occurs.
     * @throws EOFException Thrown on end of file.
     */
    protected boolean skipString(String s) throws IOException {

        // load more characters, if needed
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true, true);
        }

        // skip string
        final int length = s.length();
        final int beforeSkip = fCurrentEntity.position ;
        for (int i = 0; i < length; i++) {
            char c = fCurrentEntity.ch[fCurrentEntity.position++];
            if (c != s.charAt(i)) {
                fCurrentEntity.position -= i + 1;
                return false;
            }
            if (i < length - 1 && fCurrentEntity.position == fCurrentEntity.count) {
                invokeListeners(0);
                System.arraycopy(fCurrentEntity.ch, fCurrentEntity.count - i - 1, fCurrentEntity.ch, 0, i + 1);
                // REVISIT: Can a string to be skipped cross an
                //          entity boundary? -Ac
                if (load(i + 1, false, false)) {
                    fCurrentEntity.startPosition -= i + 1;
                    fCurrentEntity.position -= i + 1;
                    return false;
                }
            }
        }
        fCurrentEntity.columnNumber += length;
        if (!detectingVersion) {
            checkEntityLimit(null, fCurrentEntity, beforeSkip, length);
        }
        return true;

    } // skipString(String):boolean

} // class XML11EntityScanner
