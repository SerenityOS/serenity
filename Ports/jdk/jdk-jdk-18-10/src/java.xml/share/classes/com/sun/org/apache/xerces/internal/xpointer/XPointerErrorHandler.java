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

package com.sun.org.apache.xerces.internal.xpointer;

import java.io.PrintWriter;

import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;

/**
 * The Default XPointer error handler used by the XInclude implementation.
 * XPointer error's are thrown so that they may be caught by the XInclude
 * implementation and reported as resource errors.
 *
 */
final class XPointerErrorHandler implements XMLErrorHandler {

    //
    // Data
    //

    /** Print writer. */
    protected PrintWriter fOut;

    //
    // Constructors
    //

    /**
     * Constructs an error handler that prints error messages to
     * <code>System.err</code>.
     */
    public XPointerErrorHandler() {
        this(new PrintWriter(System.err));
    } // <init>()

    /**
     * Constructs an error handler that prints error messages to the
     * specified <code>PrintWriter</code.
     */
    public XPointerErrorHandler(PrintWriter out) {
        fOut = out;
    } // <init>(PrintWriter)

    //
    // ErrorHandler methods
    //

    /** Warning. */
    public void warning(String domain, String key, XMLParseException ex)
            throws XNIException {
        printError("Warning", ex);
    } // warning(XMLParseException)

    /** Error. */
    public void error(String domain, String key, XMLParseException ex)
            throws XNIException {
        printError("Error", ex);
        //throw ex;
    } // error(XMLParseException)

    /** Fatal error. */
    public void fatalError(String domain, String key, XMLParseException ex)
            throws XNIException {
        printError("Fatal Error", ex);
        throw ex;
    } // fatalError(XMLParseException)

    //
    // Private methods
    //

    /** Prints the error message. */
    private void printError(String type, XMLParseException ex) {

        fOut.print("[");
        fOut.print(type);
        fOut.print("] ");
        String systemId = ex.getExpandedSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');
            if (index != -1)
                systemId = systemId.substring(index + 1);
            fOut.print(systemId);
        }
        fOut.print(':');
        fOut.print(ex.getLineNumber());
        fOut.print(':');
        fOut.print(ex.getColumnNumber());
        fOut.print(": ");
        fOut.print(ex.getMessage());
        fOut.println();
        fOut.flush();

    } // printError(String,SAXParseException)

} // class DefaultErrorHandler
