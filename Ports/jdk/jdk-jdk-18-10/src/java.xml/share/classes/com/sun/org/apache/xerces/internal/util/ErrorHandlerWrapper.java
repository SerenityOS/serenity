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

import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class wraps a SAX error handler in an XNI error handler.
 *
 * @see ErrorHandler
 *
 * @author Andy Clark, IBM
 *
 */
public class ErrorHandlerWrapper
    implements XMLErrorHandler {

    //
    // Data
    //

    /** The SAX error handler. */
    protected ErrorHandler fErrorHandler;

    //
    // Constructors
    //

    /** Default constructor. */
    public ErrorHandlerWrapper() {}

    /** Wraps the specified SAX error handler. */
    public ErrorHandlerWrapper(ErrorHandler errorHandler) {
        setErrorHandler(errorHandler);
    } // <init>(ErrorHandler)

    //
    // Public methods
    //

    /** Sets the SAX error handler. */
    public void setErrorHandler(ErrorHandler errorHandler) {
        fErrorHandler = errorHandler;
    } // setErrorHandler(ErrorHandler)

    /** Returns the SAX error handler. */
    public ErrorHandler getErrorHandler() {
        return fErrorHandler;
    } // getErrorHandler():ErrorHandler

    //
    // XMLErrorHandler methods
    //

    /**
     * Reports a warning. Warnings are non-fatal and can be safely ignored
     * by most applications.
     *
     * @param domain    The domain of the warning. The domain can be any
     *                  string but is suggested to be a valid URI. The
     *                  domain can be used to conveniently specify a web
     *                  site location of the relevent specification or
     *                  document pertaining to this warning.
     * @param key       The warning key. This key can be any string and
     *                  is implementation dependent.
     * @param exception Exception.
     *
     * @throws XNIException Thrown to signal that the parser should stop
     *                      parsing the document.
     */
    public void warning(String domain, String key,
                        XMLParseException exception) throws XNIException {

        if (fErrorHandler != null) {
                SAXParseException saxException = createSAXParseException(exception);

                try {
                        fErrorHandler.warning(saxException);
                }
                catch (SAXParseException e) {
                        throw createXMLParseException(e);
                }
                catch (SAXException e) {
                        throw createXNIException(e);
                }
        }

    } // warning(String,String,XMLParseException)

    /**
     * Reports an error. Errors are non-fatal and usually signify that the
     * document is invalid with respect to its grammar(s).
     *
     * @param domain    The domain of the error. The domain can be any
     *                  string but is suggested to be a valid URI. The
     *                  domain can be used to conveniently specify a web
     *                  site location of the relevent specification or
     *                  document pertaining to this error.
     * @param key       The error key. This key can be any string and
     *                  is implementation dependent.
     * @param exception Exception.
     *
     * @throws XNIException Thrown to signal that the parser should stop
     *                      parsing the document.
     */
    public void error(String domain, String key,
                      XMLParseException exception) throws XNIException {

        if (fErrorHandler != null) {
                SAXParseException saxException = createSAXParseException(exception);

                try {
                        fErrorHandler.error(saxException);
                }
                catch (SAXParseException e) {
                        throw createXMLParseException(e);
                }
                catch (SAXException e) {
                        throw createXNIException(e);
                }
        }

    } // error(String,String,XMLParseException)

    /**
     * Report a fatal error. Fatal errors usually occur when the document
     * is not well-formed and signifies that the parser cannot continue
     * normal operation.
     * <p>
     * <strong>Note:</strong> The error handler should <em>always</em>
     * throw an <code>XNIException</code> from this method. This exception
     * can either be the same exception that is passed as a parameter to
     * the method or a new XNI exception object. If the registered error
     * handler fails to throw an exception, the continuing operation of
     * the parser is undetermined.
     *
     * @param domain    The domain of the fatal error. The domain can be
     *                  any string but is suggested to be a valid URI. The
     *                  domain can be used to conveniently specify a web
     *                  site location of the relevent specification or
     *                  document pertaining to this fatal error.
     * @param key       The fatal error key. This key can be any string
     *                  and is implementation dependent.
     * @param exception Exception.
     *
     * @throws XNIException Thrown to signal that the parser should stop
     *                      parsing the document.
     */
    public void fatalError(String domain, String key,
                           XMLParseException exception) throws XNIException {

        if (fErrorHandler != null) {
                SAXParseException saxException = createSAXParseException(exception);

                try {
                        fErrorHandler.fatalError(saxException);
                }
                catch (SAXParseException e) {
                        throw createXMLParseException(e);
                }
                catch (SAXException e) {
                        throw createXNIException(e);
                }
        }

    } // fatalError(String,String,XMLParseException)

    //
    // Protected methods
    //

    /** Creates a SAXParseException from an XMLParseException. */
    protected static SAXParseException createSAXParseException(XMLParseException exception) {
        return new SAXParseException(exception.getMessage(),
                                     exception.getPublicId(),
                                     exception.getExpandedSystemId(),
                                     exception.getLineNumber(),
                                     exception.getColumnNumber(),
                                     exception.getException());
    } // createSAXParseException(XMLParseException):SAXParseException

    /** Creates an XMLParseException from a SAXParseException. */
    protected static XMLParseException createXMLParseException(SAXParseException exception) {
        final String fPublicId = exception.getPublicId();
        final String fExpandedSystemId = exception.getSystemId();
        final int fLineNumber = exception.getLineNumber();
        final int fColumnNumber = exception.getColumnNumber();
        XMLLocator location = new XMLLocator() {
            public String getPublicId() { return fPublicId; }
            public String getExpandedSystemId() { return fExpandedSystemId; }
            public String getBaseSystemId() { return null; }
            public String getLiteralSystemId() { return null; }
            public int getColumnNumber() { return fColumnNumber; }
            public int getLineNumber() { return fLineNumber; }
            public int getCharacterOffset() { return -1; }
            public String getEncoding() { return null; }
            public String getXMLVersion() { return null; }
        };
        return new XMLParseException(location, exception.getMessage(),exception);
    } // createXMLParseException(SAXParseException):XMLParseException

    /** Creates an XNIException from a SAXException.
        NOTE:  care should be taken *not* to call this with a SAXParseException; this will
        lose information!!! */
    protected static XNIException createXNIException(SAXException exception) {
        return new XNIException(exception.getMessage(),exception);
    } // createXNIException(SAXException):XMLParseException
} // class ErrorHandlerWrapper
