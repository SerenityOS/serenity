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

/**
 *  This interface represents an input source for data.
 * <p> This interface allows an application to encapsulate information about
 * an input source in a single object, which may include a public
 * identifier, a system identifier, a byte stream (possibly with a specified
 * encoding), a base URI, and/or a character stream.
 * <p> The exact definitions of a byte stream and a character stream are
 * binding dependent.
 * <p> The application is expected to provide objects that implement this
 * interface whenever such objects are needed. The application can either
 * provide its own objects that implement this interface, or it can use the
 * generic factory method <code>DOMImplementationLS.createLSInput()</code>
 * to create objects that implement this interface.
 * <p> The <code>LSParser</code> will use the <code>LSInput</code> object to
 * determine how to read data. The <code>LSParser</code> will look at the
 * different inputs specified in the <code>LSInput</code> in the following
 * order to know which one to read from, the first one that is not null and
 * not an empty string will be used:
 * <ol>
 * <li> <code>LSInput.characterStream</code>
 * </li>
 * <li>
 * <code>LSInput.byteStream</code>
 * </li>
 * <li> <code>LSInput.stringData</code>
 * </li>
 * <li>
 * <code>LSInput.systemId</code>
 * </li>
 * <li> <code>LSInput.publicId</code>
 * </li>
 * </ol>
 * <p> If all inputs are null, the <code>LSParser</code> will report a
 * <code>DOMError</code> with its <code>DOMError.type</code> set to
 * <code>"no-input-specified"</code> and its <code>DOMError.severity</code>
 * set to <code>DOMError.SEVERITY_FATAL_ERROR</code>.
 * <p> <code>LSInput</code> objects belong to the application. The DOM
 * implementation will never modify them (though it may make copies and
 * modify the copies, if necessary).
 * <p>See also the <a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-LS-20040407'>Document Object Model (DOM) Level 3 Load
and Save Specification</a>.
 *
 * @since 1.5
 */
public interface LSInput {
    /**
     *  An attribute of a language and binding dependent type that represents
     * a stream of 16-bit units. The application must encode the stream
     * using UTF-16 (defined in [Unicode] and in [ISO/IEC 10646]). It is not a requirement to have an XML declaration when
     * using character streams. If an XML declaration is present, the value
     * of the encoding attribute will be ignored.
     */
    public java.io.Reader getCharacterStream();
    /**
     *  An attribute of a language and binding dependent type that represents
     * a stream of 16-bit units. The application must encode the stream
     * using UTF-16 (defined in [Unicode] and in [ISO/IEC 10646]). It is not a requirement to have an XML declaration when
     * using character streams. If an XML declaration is present, the value
     * of the encoding attribute will be ignored.
     */
    public void setCharacterStream(java.io.Reader characterStream);

    /**
     *  An attribute of a language and binding dependent type that represents
     * a stream of bytes.
     * <br> If the application knows the character encoding of the byte
     * stream, it should set the encoding attribute. Setting the encoding in
     * this way will override any encoding specified in an XML declaration
     * in the data.
     */
    public java.io.InputStream getByteStream();
    /**
     *  An attribute of a language and binding dependent type that represents
     * a stream of bytes.
     * <br> If the application knows the character encoding of the byte
     * stream, it should set the encoding attribute. Setting the encoding in
     * this way will override any encoding specified in an XML declaration
     * in the data.
     */
    public void setByteStream(java.io.InputStream byteStream);

    /**
     *  String data to parse. If provided, this will always be treated as a
     * sequence of 16-bit units (UTF-16 encoded characters). It is not a
     * requirement to have an XML declaration when using
     * <code>stringData</code>. If an XML declaration is present, the value
     * of the encoding attribute will be ignored.
     */
    public String getStringData();
    /**
     *  String data to parse. If provided, this will always be treated as a
     * sequence of 16-bit units (UTF-16 encoded characters). It is not a
     * requirement to have an XML declaration when using
     * <code>stringData</code>. If an XML declaration is present, the value
     * of the encoding attribute will be ignored.
     */
    public void setStringData(String stringData);

    /**
     *  The system identifier, a URI reference [<a href='http://www.ietf.org/rfc/rfc2396.txt'>IETF RFC 2396</a>], for this
     * input source. The system identifier is optional if there is a byte
     * stream, a character stream, or string data. It is still useful to
     * provide one, since the application will use it to resolve any
     * relative URIs and can include it in error messages and warnings. (The
     * LSParser will only attempt to fetch the resource identified by the
     * URI reference if there is no other input available in the input
     * source.)
     * <br> If the application knows the character encoding of the object
     * pointed to by the system identifier, it can set the encoding using
     * the <code>encoding</code> attribute.
     * <br> If the specified system ID is a relative URI reference (see
     * section 5 in [<a href='http://www.ietf.org/rfc/rfc2396.txt'>IETF RFC 2396</a>]), the DOM
     * implementation will attempt to resolve the relative URI with the
     * <code>baseURI</code> as the base, if that fails, the behavior is
     * implementation dependent.
     */
    public String getSystemId();
    /**
     *  The system identifier, a URI reference [<a href='http://www.ietf.org/rfc/rfc2396.txt'>IETF RFC 2396</a>], for this
     * input source. The system identifier is optional if there is a byte
     * stream, a character stream, or string data. It is still useful to
     * provide one, since the application will use it to resolve any
     * relative URIs and can include it in error messages and warnings. (The
     * LSParser will only attempt to fetch the resource identified by the
     * URI reference if there is no other input available in the input
     * source.)
     * <br> If the application knows the character encoding of the object
     * pointed to by the system identifier, it can set the encoding using
     * the <code>encoding</code> attribute.
     * <br> If the specified system ID is a relative URI reference (see
     * section 5 in [<a href='http://www.ietf.org/rfc/rfc2396.txt'>IETF RFC 2396</a>]), the DOM
     * implementation will attempt to resolve the relative URI with the
     * <code>baseURI</code> as the base, if that fails, the behavior is
     * implementation dependent.
     */
    public void setSystemId(String systemId);

    /**
     *  The public identifier for this input source. This may be mapped to an
     * input source using an implementation dependent mechanism (such as
     * catalogues or other mappings). The public identifier, if specified,
     * may also be reported as part of the location information when errors
     * are reported.
     */
    public String getPublicId();
    /**
     *  The public identifier for this input source. This may be mapped to an
     * input source using an implementation dependent mechanism (such as
     * catalogues or other mappings). The public identifier, if specified,
     * may also be reported as part of the location information when errors
     * are reported.
     */
    public void setPublicId(String publicId);

    /**
     *  The base URI to be used (see section 5.1.4 in [<a href='http://www.ietf.org/rfc/rfc2396.txt'>IETF RFC 2396</a>]) for
     * resolving a relative <code>systemId</code> to an absolute URI.
     * <br> If, when used, the base URI is itself a relative URI, an empty
     * string, or null, the behavior is implementation dependent.
     */
    public String getBaseURI();
    /**
     *  The base URI to be used (see section 5.1.4 in [<a href='http://www.ietf.org/rfc/rfc2396.txt'>IETF RFC 2396</a>]) for
     * resolving a relative <code>systemId</code> to an absolute URI.
     * <br> If, when used, the base URI is itself a relative URI, an empty
     * string, or null, the behavior is implementation dependent.
     */
    public void setBaseURI(String baseURI);

    /**
     *  The character encoding, if known. The encoding must be a string
     * acceptable for an XML encoding declaration ([<a href='http://www.w3.org/TR/2004/REC-xml-20040204'>XML 1.0</a>] section
     * 4.3.3 "Character Encoding in Entities").
     * <br> This attribute has no effect when the application provides a
     * character stream or string data. For other sources of input, an
     * encoding specified by means of this attribute will override any
     * encoding specified in the XML declaration or the Text declaration, or
     * an encoding obtained from a higher level protocol, such as HTTP [<a href='http://www.ietf.org/rfc/rfc2616.txt'>IETF RFC 2616</a>].
     */
    public String getEncoding();
    /**
     *  The character encoding, if known. The encoding must be a string
     * acceptable for an XML encoding declaration ([<a href='http://www.w3.org/TR/2004/REC-xml-20040204'>XML 1.0</a>] section
     * 4.3.3 "Character Encoding in Entities").
     * <br> This attribute has no effect when the application provides a
     * character stream or string data. For other sources of input, an
     * encoding specified by means of this attribute will override any
     * encoding specified in the XML declaration or the Text declaration, or
     * an encoding obtained from a higher level protocol, such as HTTP [<a href='http://www.ietf.org/rfc/rfc2616.txt'>IETF RFC 2616</a>].
     */
    public void setEncoding(String encoding);

    /**
     *  If set to true, assume that the input is certified (see section 2.13
     * in [<a href='http://www.w3.org/TR/2004/REC-xml11-20040204/'>XML 1.1</a>]) when
     * parsing [<a href='http://www.w3.org/TR/2004/REC-xml11-20040204/'>XML 1.1</a>].
     */
    public boolean getCertifiedText();
    /**
     *  If set to true, assume that the input is certified (see section 2.13
     * in [<a href='http://www.w3.org/TR/2004/REC-xml11-20040204/'>XML 1.1</a>]) when
     * parsing [<a href='http://www.w3.org/TR/2004/REC-xml11-20040204/'>XML 1.1</a>].
     */
    public void setCertifiedText(boolean certifiedText);

}
