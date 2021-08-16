/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.security.jgss;

/**
 * Attribute types that can be specified as an argument of
 * {@link com.sun.security.jgss.ExtendedGSSContext#inquireSecContext}
 */
public enum InquireType {
    /**
     * Attribute type for retrieving the session key of an established
     * Kerberos 5 security context. The returned object is an instance of
     * {@link java.security.Key}, which has the following properties:
     *    <ul>
     *    <li>Algorithm: enctype as a string, where
     *        enctype is defined in RFC 3961, section 8.
     *    <li>Format: "RAW"
     *    <li>Encoded form: the raw key bytes, not in any ASN.1 encoding
     *    </ul>
     * @deprecated as of 9, replaced by {@link #KRB5_GET_SESSION_KEY_EX}
     * which returns an instance of
     * {@link javax.security.auth.kerberos.EncryptionKey}
     * that implements the {@link javax.crypto.SecretKey} interface and
     * has similar methods with {@link javax.security.auth.kerberos.KerberosKey}.
     */
    @Deprecated
    KRB5_GET_SESSION_KEY,
    /**
     * Attribute type for retrieving the session key of an
     * established Kerberos 5 security context. The return value is an
     * instance of {@link javax.security.auth.kerberos.EncryptionKey}.
     *
     * @since 9
     */
    KRB5_GET_SESSION_KEY_EX,
    /**
     * Attribute type for retrieving the service ticket flags of an
     * established Kerberos 5 security context. The returned object is
     * a boolean array for the service ticket flags, which is long enough
     * to contain all true bits. This means if the user wants to get the
     * <em>n</em>'th bit but the length of the returned array is less than
     * <em>n</em>, it is regarded as false.
     */
    KRB5_GET_TKT_FLAGS,
    /**
     * Attribute type for retrieving the authorization data in the
     * service ticket of an established Kerberos 5 security context.
     * Only supported on the acceptor side.
     */
    KRB5_GET_AUTHZ_DATA,
    /**
     * Attribute type for retrieving the authtime in the service ticket
     * of an established Kerberos 5 security context. The returned object
     * is a String object in the standard KerberosTime format defined in
     * RFC 4120 Section 5.2.3.
     */
    KRB5_GET_AUTHTIME,
    /**
     * Attribute type for retrieving the KRB_CRED message that an initiator
     * is about to send to an acceptor. The return type is an instance of
     * {@link javax.security.auth.kerberos.KerberosCredMessage}.
     *
     * @since 9
     */
    KRB5_GET_KRB_CRED,
}
