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


/**
 * Basic interface for SAX error handlers.
 *
 * <p>If a SAX application needs to implement customized error
 * handling, it must implement this interface and then register an
 * instance with the XML reader using the
 * {@link org.xml.sax.XMLReader#setErrorHandler setErrorHandler}
 * method.  The parser will then report all errors and warnings
 * through this interface.</p>
 *
 * <p><strong>WARNING:</strong> If an application does <em>not</em>
 * register an ErrorHandler, XML parsing errors will go unreported,
 * except that <em>SAXParseException</em>s will be thrown for fatal errors.
 * In order to detect validity errors, an ErrorHandler that does something
 * with {@link #error error()} calls must be registered.</p>
 *
 * <p>For XML processing errors, a SAX driver must use this interface
 * in preference to throwing an exception: it is up to the application
 * to decide whether to throw an exception for different types of
 * errors and warnings.  Note, however, that there is no requirement that
 * the parser continue to report additional errors after a call to
 * {@link #fatalError fatalError}.  In other words, a SAX driver class
 * may throw an exception after reporting any fatalError.
 * Also parsers may throw appropriate exceptions for non-XML errors.
 * For example, {@link XMLReader#parse XMLReader.parse()} would throw
 * an IOException for errors accessing entities or the document.</p>
 *
 * @since 1.4, SAX 1.0
 * @author David Megginson
 * @see org.xml.sax.XMLReader#setErrorHandler
 * @see org.xml.sax.SAXParseException
 */
public interface ErrorHandler {


    /**
     * Receive notification of a warning.
     *
     * <p>SAX parsers will use this method to report conditions that
     * are not errors or fatal errors as defined by the XML
     * recommendation.  The default behaviour is to take no
     * action.</p>
     *
     * <p>The SAX parser must continue to provide normal parsing events
     * after invoking this method: it should still be possible for the
     * application to process the document through to the end.</p>
     *
     * <p>Filters may use this method to report other, non-XML warnings
     * as well.</p>
     *
     * @param exception The warning information encapsulated in a
     *                  SAX parse exception.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.SAXParseException
     */
    public abstract void warning (SAXParseException exception)
        throws SAXException;


    /**
     * Receive notification of a recoverable error.
     *
     * <p>This corresponds to the definition of "error" in section 1.2
     * of the W3C XML 1.0 Recommendation.  For example, a validating
     * parser would use this callback to report the violation of a
     * validity constraint.  The default behaviour is to take no
     * action.</p>
     *
     * <p>The SAX parser must continue to provide normal parsing
     * events after invoking this method: it should still be possible
     * for the application to process the document through to the end.
     * If the application cannot do so, then the parser should report
     * a fatal error even if the XML recommendation does not require
     * it to do so.</p>
     *
     * <p>Filters may use this method to report other, non-XML errors
     * as well.</p>
     *
     * @param exception The error information encapsulated in a
     *                  SAX parse exception.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.SAXParseException
     */
    public abstract void error (SAXParseException exception)
        throws SAXException;


    /**
     * Receive notification of a non-recoverable, fatal error.
     *
     * <p>
     * As defined in section 1.2 of the W3C XML 1.0 Recommendation, fatal errors
     * are those that would make it impossible for a parser to continue normal
     * processing. These include violation of a well-formedness constraint,
     * invalid encoding, and forbidden structural errors as described in the
     * W3C XML 1.0 Recommendation.
     *
     * @apiNote An application must assume that the parser can no longer perform
     * normal processing after reporting a fatal error and may stop by throwing
     * a {@link SAXException} without calling {@link ContentHandler#endDocument()}.
     * In addition, the parser cannot be expected to be able to return accurate
     * information about the logical structure on the rest of the document even
     * if it may be able to resume parsing.
     *
     * @implNote After invoking this method, the parser may stop processing by
     * throwing a {@link SAXException}, or implement a feature that can direct
     * it to continue after a fatal error. In the later case, it may report
     * events on the rest of the document without any guarantee of correctness.
     *
     * @param exception The error information encapsulated in a
     *                  {@link SAXParseException}.
     * @throws SAXException if the application chooses to discontinue the parsing
     */
    public abstract void fatalError (SAXParseException exception)
        throws SAXException;

}

// end of ErrorHandler.java
