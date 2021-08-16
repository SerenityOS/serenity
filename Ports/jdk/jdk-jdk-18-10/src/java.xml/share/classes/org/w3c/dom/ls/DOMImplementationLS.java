/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file and, per its terms, should not be removed:
 *
 * Copyright (c) 2004 World Wide Web Consortium,
 *
 * (Massachusetts Institute of Technology, European Research Consortium for
 * Informatics and Mathematics, Keio University). All Rights Reserved. This
 * work is distributed under the W3C(r) Software License [1] in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * [1] http://www.w3.org/Consortium/Legal/2002/copyright-software-20021231
 */

package org.w3c.dom.ls;

import org.w3c.dom.DOMException;

/**
 *  <code>DOMImplementationLS</code> contains the factory methods for creating
 * Load and Save objects.
 * <p> The expectation is that an instance of the
 * <code>DOMImplementationLS</code> interface can be obtained by using
 * binding-specific casting methods on an instance of the
 * <code>DOMImplementation</code> interface or, if the <code>Document</code>
 * supports the feature <code>"Core"</code> version <code>"3.0"</code>
 * defined in
 * [<a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407'>DOM Level 3 Core</a>]
 * , by using the method <code>DOMImplementation.getFeature</code> with
 * parameter values <code>"LS"</code> (or <code>"LS-Async"</code>) and
 * <code>"3.0"</code> (respectively).
 * <p>See also the <a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-LS-20040407'>
Document Object Model (DOM) Level 3 Load and Save Specification</a>.
 *
 * @since 1.5
 */
public interface DOMImplementationLS {
    // DOMImplementationLSMode
    /**
     * Create a synchronous <code>LSParser</code>.
     */
    public static final short MODE_SYNCHRONOUS          = 1;
    /**
     * Create an asynchronous <code>LSParser</code>.
     */
    public static final short MODE_ASYNCHRONOUS         = 2;

    /**
     * Create a new <code>LSParser</code>. The newly constructed parser may
     * then be configured by means of its <code>DOMConfiguration</code>
     * object, and used to parse documents by means of its <code>parse</code>
     *  method.
     * @param mode  The <code>mode</code> argument is either
     *   <code>MODE_SYNCHRONOUS</code> or <code>MODE_ASYNCHRONOUS</code>, if
     *   <code>mode</code> is <code>MODE_SYNCHRONOUS</code> then the
     *   <code>LSParser</code> that is created will operate in synchronous
     *   mode, if it's <code>MODE_ASYNCHRONOUS</code> then the
     *   <code>LSParser</code> that is created will operate in asynchronous
     *   mode.
     * @param schemaType  An absolute URI representing the type of the schema
     *   language used during the load of a <code>Document</code> using the
     *   newly created <code>LSParser</code>. Note that no lexical checking
     *   is done on the absolute URI. In order to create a
     *   <code>LSParser</code> for any kind of schema types (i.e. the
     *   LSParser will be free to use any schema found), use the value
     *   <code>null</code>.
     * <p ><b>Note:</b>    For W3C XML Schema
     * [<a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/'>XML Schema Part 1</a>]
     *   , applications must use the value
     *   <code>"http://www.w3.org/2001/XMLSchema"</code>. For XML DTD
     * [<a href='http://www.w3.org/TR/2004/REC-xml-20040204'>XML 1.0</a>],
     *   applications must use the value
     *   <code>"http://www.w3.org/TR/REC-xml"</code>. Other Schema languages
     *   are outside the scope of the W3C and therefore should recommend an
     *   absolute URI in order to use this method.
     * @return  The newly created <code>LSParser</code> object. This
     *   <code>LSParser</code> is either synchronous or asynchronous
     *   depending on the value of the <code>mode</code> argument.
     * <p ><b>Note:</b>    By default, the newly created <code>LSParser</code>
     *   does not contain a <code>DOMErrorHandler</code>, i.e. the value of
     *   the "<a href='https://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#parameter-error-handler'>error-handler</a>"
     *   configuration parameter is <code>null</code>. However, implementations
     *   may provide a default error handler at creation time. In that case,
     *   the initial value of the <code>"error-handler"</code> configuration
     *   parameter on the new <code>LSParser</code> object contains a
     *   reference to the default error handler.
     * @exception DOMException
     *    NOT_SUPPORTED_ERR: Raised if the requested mode or schema type is
     *   not supported.
     */
    public LSParser createLSParser(short mode,
                                   String schemaType)
                                   throws DOMException;

    /**
     *  Create a new <code>LSSerializer</code> object.
     * @return The newly created <code>LSSerializer</code> object.
     * <p ><b>Note:</b>    By default, the newly created
     *   <code>LSSerializer</code> has no <code>DOMErrorHandler</code>, i.e.
     *   the value of the <code>"error-handler"</code> configuration
     *   parameter is <code>null</code>. However, implementations may
     *   provide a default error handler at creation time. In that case, the
     *   initial value of the <code>"error-handler"</code> configuration
     *   parameter on the new <code>LSSerializer</code> object contains a
     *   reference to the default error handler.
     */
    public LSSerializer createLSSerializer();

    /**
     *  Create a new empty input source object where
     * <code>LSInput.characterStream</code>, <code>LSInput.byteStream</code>
     * , <code>LSInput.stringData</code> <code>LSInput.systemId</code>,
     * <code>LSInput.publicId</code>, <code>LSInput.baseURI</code>, and
     * <code>LSInput.encoding</code> are null, and
     * <code>LSInput.certifiedText</code> is false.
     * @return  The newly created input object.
     */
    public LSInput createLSInput();

    /**
     *  Create a new empty output destination object where
     * <code>LSOutput.characterStream</code>,
     * <code>LSOutput.byteStream</code>, <code>LSOutput.systemId</code>,
     * <code>LSOutput.encoding</code> are null.
     * @return  The newly created output object.
     */
    public LSOutput createLSOutput();

}
