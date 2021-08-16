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

import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XNIException;

/**
 * A parsing exception. This exception is different from the standard
 * XNI exception in that it stores the location in the document (or
 * its entities) where the exception occurred.
 *
 * @author Andy Clark, IBM
 *
 */
public class XMLParseException
    extends XNIException {

    /** Serialization version. */
    static final long serialVersionUID = 1732959359448549967L;

    //
    // Data
    //

    /** Public identifier. */
    protected String fPublicId;

    /** literal System identifier. */
    protected String fLiteralSystemId;

    /** expanded System identifier. */
    protected String fExpandedSystemId;

    /** Base system identifier. */
    protected String fBaseSystemId;

    /** Line number. */
    protected int fLineNumber = -1;

    /** Column number. */
    protected int fColumnNumber = -1;

    /** Character offset. */
    protected int fCharacterOffset = -1;

    //
    // Constructors
    //

    /** Constructs a parse exception. */
    public XMLParseException(XMLLocator locator, String message) {
        super(message);
        if (locator != null) {
            fPublicId = locator.getPublicId();
            fLiteralSystemId = locator.getLiteralSystemId();
            fExpandedSystemId = locator.getExpandedSystemId();
            fBaseSystemId = locator.getBaseSystemId();
            fLineNumber = locator.getLineNumber();
            fColumnNumber = locator.getColumnNumber();
            fCharacterOffset = locator.getCharacterOffset();
        }
    } // <init>(XMLLocator,String)

    /** Constructs a parse exception. */
    public XMLParseException(XMLLocator locator,
                             String message, Exception exception) {
        super(message, exception);
        if (locator != null) {
            fPublicId = locator.getPublicId();
            fLiteralSystemId = locator.getLiteralSystemId();
            fExpandedSystemId = locator.getExpandedSystemId();
            fBaseSystemId = locator.getBaseSystemId();
            fLineNumber = locator.getLineNumber();
            fColumnNumber = locator.getColumnNumber();
            fCharacterOffset = locator.getCharacterOffset();
        }
    } // <init>(XMLLocator,String,Exception)

    //
    // Public methods
    //

    /** Returns the public identifier. */
    public String getPublicId() {
        return fPublicId;
    } // getPublicId():String

    /** Returns the expanded system identifier. */
    public String getExpandedSystemId() {
        return fExpandedSystemId;
    } // getExpandedSystemId():String

    /** Returns the literal system identifier. */
    public String getLiteralSystemId() {
        return fLiteralSystemId;
    } // getLiteralSystemId():String

    /** Returns the base system identifier. */
    public String getBaseSystemId() {
        return fBaseSystemId;
    } // getBaseSystemId():String

    /** Returns the line number. */
    public int getLineNumber() {
        return fLineNumber;
    } // getLineNumber():int

    /** Returns the row number. */
    public int getColumnNumber() {
        return fColumnNumber;
    } // getRowNumber():int

    /** Returns the character offset. */
    public int getCharacterOffset() {
        return fCharacterOffset;
    } // getCharacterOffset():int

    //
    // Object methods
    //

    /** Returns a string representation of this object. */
    public String toString() {

        StringBuffer str = new StringBuffer();
        if (fPublicId != null) {
            str.append(fPublicId);
        }
        str.append(':');
        if (fLiteralSystemId != null) {
            str.append(fLiteralSystemId);
        }
        str.append(':');
        if (fExpandedSystemId != null) {
            str.append(fExpandedSystemId);
        }
        str.append(':');
        if (fBaseSystemId != null) {
            str.append(fBaseSystemId);
        }
        str.append(':');
        str.append(fLineNumber);
        str.append(':');
        str.append(fColumnNumber);
        str.append(':');
        str.append(fCharacterOffset);
        str.append(':');
        String message = getMessage();
        if (message == null) {
            Exception exception = getException();
            if (exception != null) {
                message = exception.getMessage();
            }
        }
        if (message != null) {
            str.append(message);
        }
        return str.toString();

    } // toString():String

} // XMLParseException
