/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * $Id: CanonicalizationMethod.java,v 1.6 2005/05/10 16:03:45 mullan Exp $
 */
package javax.xml.crypto.dsig;

import java.security.spec.AlgorithmParameterSpec;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;

/**
 * A representation of the XML <code>CanonicalizationMethod</code>
 * element as defined in the
 * <a href="http://www.w3.org/TR/xmldsig-core/">
 * W3C Recommendation for XML-Signature Syntax and Processing</a>. The XML
 * Schema Definition is defined as:
 * <pre>
 *   &lt;element name="CanonicalizationMethod" type="ds:CanonicalizationMethodType"/&gt;
 *     &lt;complexType name="CanonicalizationMethodType" mixed="true"&gt;
 *       &lt;sequence&gt;
 *         &lt;any namespace="##any" minOccurs="0" maxOccurs="unbounded"/&gt;
 *           &lt;!-- (0,unbounded) elements from (1,1) namespace --&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="Algorithm" type="anyURI" use="required"/&gt;
 *     &lt;/complexType&gt;
 * </pre>
 *
 * A <code>CanonicalizationMethod</code> instance may be created by invoking
 * the {@link XMLSignatureFactory#newCanonicalizationMethod
 * newCanonicalizationMethod} method of the {@link XMLSignatureFactory} class.
 *
 * @author Sean Mullan
 * @author JSR 105 Expert Group
 * @since 1.6
 * @see XMLSignatureFactory#newCanonicalizationMethod(String, C14NMethodParameterSpec)
 */
public interface CanonicalizationMethod extends Transform {

    /**
     * The <a href="http://www.w3.org/TR/2001/REC-xml-c14n-20010315">Canonical
     * XML (without comments)</a> canonicalization method algorithm URI.
     */
    static final String INCLUSIVE =
        "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";

    /**
     * The
     * <a href="http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments">
     * Canonical XML with comments</a> canonicalization method algorithm URI.
     */
    static final String INCLUSIVE_WITH_COMMENTS =
        "http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments";

    /**
     * The <a href="http://www.w3.org/2001/10/xml-exc-c14n#">Exclusive
     * Canonical XML (without comments)</a> canonicalization method algorithm
     * URI.
     */
    static final String EXCLUSIVE =
        "http://www.w3.org/2001/10/xml-exc-c14n#";

    /**
     * The <a href="http://www.w3.org/2001/10/xml-exc-c14n#WithComments">
     * Exclusive Canonical XML with comments</a> canonicalization method
     * algorithm URI.
     */
    static final String EXCLUSIVE_WITH_COMMENTS =
        "http://www.w3.org/2001/10/xml-exc-c14n#WithComments";

    /**
     * The <a href="https://www.w3.org/TR/xml-c14n11/">Canonical XML 1.1
     * (without comments)</a> canonicalization method algorithm URI.
     *
     * @since 13
     */
    static final String INCLUSIVE_11 = "http://www.w3.org/2006/12/xml-c14n11";

    /**
     * The <a href="https://www.w3.org/TR/xml-c14n11/#WithComments">
     * Canonical XML 1.1 with comments</a> canonicalization method algorithm
     * URI.
     *
     * @since 13
     */
    static final String INCLUSIVE_11_WITH_COMMENTS =
        "http://www.w3.org/2006/12/xml-c14n11#WithComments";

    /**
     * Returns the algorithm-specific input parameters associated with this
     * <code>CanonicalizationMethod</code>.
     *
     * <p>The returned parameters can be typecast to a
     * {@link C14NMethodParameterSpec} object.
     *
     * @return the algorithm-specific input parameters (may be
     *    <code>null</code> if not specified)
     */
    AlgorithmParameterSpec getParameterSpec();
}
