/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * $Id: Reference.java,v 1.9 2005/05/10 16:03:46 mullan Exp $
 */
package javax.xml.crypto.dsig;

import javax.xml.crypto.Data;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.XMLStructure;
import java.io.InputStream;
import java.util.List;

/**
 * A representation of the <code>Reference</code> element as defined in the
 * <a href="http://www.w3.org/TR/xmldsig-core/">
 * W3C Recommendation for XML-Signature Syntax and Processing</a>.
 * The XML schema is defined as:
 * <pre>
 * &lt;element name="Reference" type="ds:ReferenceType"/&gt;
 * &lt;complexType name="ReferenceType"&gt;
 *   &lt;sequence&gt;
 *     &lt;element ref="ds:Transforms" minOccurs="0"/&gt;
 *     &lt;element ref="ds:DigestMethod"/&gt;
 *     &lt;element ref="ds:DigestValue"/&gt;
 *   &lt;/sequence&gt;
 *   &lt;attribute name="Id" type="ID" use="optional"/&gt;
 *   &lt;attribute name="URI" type="anyURI" use="optional"/&gt;
 *   &lt;attribute name="Type" type="anyURI" use="optional"/&gt;
 * &lt;/complexType&gt;
 *
 * &lt;element name="DigestValue" type="ds:DigestValueType"/&gt;
 * &lt;simpleType name="DigestValueType"&gt;
 *   &lt;restriction base="base64Binary"/&gt;
 * &lt;/simpleType&gt;
 * </pre>
 *
 * <p>A <code>Reference</code> instance may be created by invoking one of the
 * {@link XMLSignatureFactory#newReference newReference} methods of the
 * {@link XMLSignatureFactory} class; for example:
 *
 * <pre>
 *   XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
 *   Reference ref = factory.newReference
 *     ("http://www.ietf.org/rfc/rfc3275.txt",
 *      factory.newDigestMethod(DigestMethod.SHA1, null));
 * </pre>
 *
 * @author Sean Mullan
 * @author Erwin van der Koogh
 * @author JSR 105 Expert Group
 * @since 1.6
 * @see XMLSignatureFactory#newReference(String, DigestMethod)
 * @see XMLSignatureFactory#newReference(String, DigestMethod, List, String, String)
 */
public interface Reference extends URIReference, XMLStructure {

    /**
     * Returns an {@link java.util.Collections#unmodifiableList unmodifiable
     * list} of {@link Transform}s that are contained in this
     * <code>Reference</code>.
     *
     * @return an unmodifiable list of <code>Transform</code>s
     *    (may be empty but never <code>null</code>)
     */
    List<Transform> getTransforms();

    /**
     * Returns the digest method of this <code>Reference</code>.
     *
     * @return the digest method
     */
    DigestMethod getDigestMethod();

    /**
     * Returns the optional <code>Id</code> attribute of this
     * <code>Reference</code>, which permits this reference to be
     * referenced from elsewhere.
     *
     * @return the <code>Id</code> attribute (may be <code>null</code> if not
     *    specified)
     */
    String getId();

    /**
     * Returns the digest value of this <code>Reference</code>.
     *
     * @return the raw digest value, or <code>null</code> if this reference has
     *    not been digested yet. Each invocation of this method returns a new
     *    clone to protect against subsequent modification.
     */
    byte[] getDigestValue();

    /**
     * Returns the calculated digest value of this <code>Reference</code>
     * after a validation operation. This method is useful for debugging if
     * the reference fails to validate.
     *
     * @return the calculated digest value, or <code>null</code> if this
     *    reference has not been validated yet. Each invocation of this method
     *    returns a new clone to protect against subsequent modification.
     */
    byte[] getCalculatedDigestValue();

    /**
     * Validates this reference. This method verifies the digest of this
     * reference.
     *
     * <p>This method only validates the reference the first time it is
     * invoked. On subsequent invocations, it returns a cached result.
     *
     * @return <code>true</code> if this reference was validated successfully;
     *    <code>false</code> otherwise
     * @param validateContext the validating context
     * @throws NullPointerException if <code>validateContext</code> is
     *    <code>null</code>
     * @throws XMLSignatureException if an unexpected exception occurs while
     *    validating the reference
     */
    boolean validate(XMLValidateContext validateContext)
        throws XMLSignatureException;

    /**
     * Returns the dereferenced data, if
     * <a href="XMLSignContext.html#SupportedProperties">reference caching</a>
     * is enabled. This is the result of dereferencing the URI of this
     * reference during a validation or generation operation.
     *
     * @return the dereferenced data, or <code>null</code> if reference
     *    caching is not enabled or this reference has not been generated or
     *    validated
     */
    Data getDereferencedData();

    /**
     * Returns the pre-digested input stream, if
     * <a href="XMLSignContext.html#SupportedProperties">reference caching</a>
     * is enabled. This is the input to the digest operation during a
     * validation or signing operation.
     *
     * @return an input stream containing the pre-digested input, or
     *    <code>null</code> if reference caching is not enabled or this
     *    reference has not been generated or validated
     */
    InputStream getDigestInputStream();
}
