/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * $Id: URIDereferencer.java,v 1.5 2005/05/10 15:47:42 mullan Exp $
 */
package javax.xml.crypto;

/**
 * A dereferencer of {@link URIReference}s.
 * <p>
 * The result of dereferencing a <code>URIReference</code> is either an
 * instance of {@link OctetStreamData} or {@link NodeSetData}. Unless the
 * <code>URIReference</code> is a <i>same-document reference</i> as defined
 * in section 4.2 of the W3C Recommendation for XML-Signature Syntax and
 * Processing, the result of dereferencing the <code>URIReference</code>
 * MUST be an <code>OctetStreamData</code>.
 *
 * @author Sean Mullan
 * @author Joyce Leung
 * @author JSR 105 Expert Group
 * @since 1.6
 * @see XMLCryptoContext#setURIDereferencer(URIDereferencer)
 * @see XMLCryptoContext#getURIDereferencer
 */
public interface URIDereferencer {

    /**
     * Dereferences the specified <code>URIReference</code> and returns the
     * dereferenced data.
     *
     * @param uriReference the <code>URIReference</code>
     * @param context an <code>XMLCryptoContext</code> that may
     *    contain additional useful information for dereferencing the URI. This
     *    implementation should dereference the specified
     *    <code>URIReference</code> against the context's <code>baseURI</code>
     *    parameter, if specified.
     * @return the dereferenced data
     * @throws NullPointerException if <code>uriReference</code> or
     *    <code>context</code> are <code>null</code>
     * @throws URIReferenceException if an exception occurs while
     *    dereferencing the specified <code>uriReference</code>
     */
    Data dereference(URIReference uriReference, XMLCryptoContext context)
        throws URIReferenceException;
}
