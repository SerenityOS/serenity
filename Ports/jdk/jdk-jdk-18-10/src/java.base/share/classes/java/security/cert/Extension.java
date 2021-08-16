/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.security.cert;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * This interface represents an X.509 extension.
 *
 * <p>
 * Extensions provide a means of associating additional attributes with users
 * or public keys and for managing a certification hierarchy.  The extension
 * format also allows communities to define private extensions to carry
 * information unique to those communities.
 *
 * <p>
 * Each extension contains an object identifier, a criticality setting
 * indicating whether it is a critical or a non-critical extension, and
 * an ASN.1 DER-encoded value. Its ASN.1 definition is:
 *
 * <pre>
 *
 *     Extension ::= SEQUENCE {
 *         extnId        OBJECT IDENTIFIER,
 *         critical      BOOLEAN DEFAULT FALSE,
 *         extnValue     OCTET STRING
 *                 -- contains a DER encoding of a value
 *                 -- of the type registered for use with
 *                 -- the extnId object identifier value
 *     }
 *
 * </pre>
 *
 * <p>
 * This interface is designed to provide access to a single extension,
 * unlike {@link java.security.cert.X509Extension} which is more suitable
 * for accessing a set of extensions.
 *
 * @since 1.7
 */
public interface Extension {

    /**
     * Gets the extensions's object identifier.
     *
     * @return the object identifier as a String
     */
    String getId();

    /**
     * Gets the extension's criticality setting.
     *
     * @return true if this is a critical extension.
     */
    boolean isCritical();

    /**
     * Gets the extensions's DER-encoded value. Note, this is the bytes
     * that are encoded as an OCTET STRING. It does not include the OCTET
     * STRING tag and length.
     *
     * @return a copy of the extension's value, or {@code null} if no
     *    extension value is present.
     */
    byte[] getValue();

    /**
     * Generates the extension's DER encoding and writes it to the output
     * stream.
     *
     * @param out the output stream
     * @throws    IOException on encoding or output error.
     * @throws    NullPointerException if {@code out} is {@code null}.
     */
    void encode(OutputStream out) throws IOException;
}
