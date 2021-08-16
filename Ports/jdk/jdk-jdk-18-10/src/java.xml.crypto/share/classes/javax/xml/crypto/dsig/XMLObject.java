/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * ===========================================================================
 *
 * (C) Copyright IBM Corp. 2003 All Rights Reserved.
 *
 * ===========================================================================
 */
/*
 * $Id: XMLObject.java,v 1.5 2005/05/10 16:03:48 mullan Exp $
 */
package javax.xml.crypto.dsig;

import java.util.List;
import javax.xml.crypto.XMLStructure;

/**
 * A representation of the XML <code>Object</code> element as defined in
 * the <a href="http://www.w3.org/TR/xmldsig-core/">
 * W3C Recommendation for XML-Signature Syntax and Processing</a>.
 * An <code>XMLObject</code> may contain any data and may include optional
 * MIME type, ID, and encoding attributes. The XML Schema Definition is
 * defined as:
 *
 * <pre><code>
 * &lt;element name="Object" type="ds:ObjectType"/&gt;
 * &lt;complexType name="ObjectType" mixed="true"&gt;
 *   &lt;sequence minOccurs="0" maxOccurs="unbounded"&gt;
 *     &lt;any namespace="##any" processContents="lax"/&gt;
 *   &lt;/sequence&gt;
 *   &lt;attribute name="Id" type="ID" use="optional"/&gt;
 *   &lt;attribute name="MimeType" type="string" use="optional"/&gt;
 *   &lt;attribute name="Encoding" type="anyURI" use="optional"/&gt;
 * &lt;/complexType&gt;
 * </code></pre>
 *
 * A <code>XMLObject</code> instance may be created by invoking the
 * {@link XMLSignatureFactory#newXMLObject newXMLObject} method of the
 * {@link XMLSignatureFactory} class; for example:
 *
 * <pre>
 *   XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
 *   Manifest manifest = fac.newManifest(references);
 *   List&lt;XMLStructure&gt; content = Collections.singletonList(manifest);
 *   XMLObject object = factory.newXMLObject(content, "object-1", null, null);
 * </pre>
 *
 * <p>Note that this class is named <code>XMLObject</code> rather than
 * <code>Object</code> to avoid naming clashes with the existing
 * {@link java.lang.Object java.lang.Object} class.
 *
 * @author Sean Mullan
 * @author JSR 105 Expert Group
 * @author Joyce L. Leung
 * @since 1.6
 * @see XMLSignatureFactory#newXMLObject(List, String, String, String)
 */
public interface XMLObject extends XMLStructure {

    /**
     * URI that identifies the <code>Object</code> element (this can be
     * specified as the value of the <code>type</code> parameter of the
     * {@link Reference} class to identify the referent's type).
     */
    static final String TYPE = "http://www.w3.org/2000/09/xmldsig#Object";

    /**
     * Returns an {@link java.util.Collections#unmodifiableList unmodifiable
     * list} of {@link XMLStructure}s contained in this <code>XMLObject</code>,
     * which represent elements from any namespace.
     *
     *<p>If there is a public subclass representing the type of
     * <code>XMLStructure</code>, it is returned as an instance of that class
     * (ex: a <code>SignatureProperties</code> element would be returned
     * as an instance of {@link javax.xml.crypto.dsig.SignatureProperties}).
     *
     * @return an unmodifiable list of <code>XMLStructure</code>s (may be empty
     *    but never <code>null</code>)
     */
    List<XMLStructure> getContent();

    /**
     * Returns the Id of this <code>XMLObject</code>.
     *
     * @return the Id (or <code>null</code> if not specified)
     */
    String getId();

    /**
     * Returns the mime type of this <code>XMLObject</code>. The
     * mime type is an optional attribute which describes the data within this
     * <code>XMLObject</code> (independent of its encoding).
     *
     * @return the mime type (or <code>null</code> if not specified)
     */
    String getMimeType();

    /**
     * Returns the encoding URI of this <code>XMLObject</code>. The encoding
     * URI identifies the method by which the object is encoded.
     *
     * @return the encoding URI (or <code>null</code> if not specified)
     */
    String getEncoding();
}
