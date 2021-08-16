/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * The {@code PKIXReason} enumerates the potential PKIX-specific reasons
 * that an X.509 certification path may be invalid according to the PKIX
 * (RFC 5280) standard. These reasons are in addition to those of the
 * {@code CertPathValidatorException.BasicReason} enumeration.
 *
 * @since 1.7
 */
public enum PKIXReason implements CertPathValidatorException.Reason {
    /**
     * The certificate does not chain correctly.
     */
    NAME_CHAINING,

    /**
     * The certificate's key usage is invalid.
     */
    INVALID_KEY_USAGE,

    /**
     * The policy constraints have been violated.
     */
    INVALID_POLICY,

    /**
     * No acceptable trust anchor found.
     */
    NO_TRUST_ANCHOR,

    /**
     * The certificate contains one or more unrecognized critical
     * extensions.
     */
    UNRECOGNIZED_CRIT_EXT,

    /**
     * The certificate is not a CA certificate.
     */
    NOT_CA_CERT,

    /**
     * The path length constraint has been violated.
     */
    PATH_TOO_LONG,

    /**
     * The name constraints have been violated.
     */
    INVALID_NAME
}
