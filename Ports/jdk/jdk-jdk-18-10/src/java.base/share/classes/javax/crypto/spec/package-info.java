/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Provides classes and interfaces for key specifications and
 * algorithm parameter specifications.
 *
 * <p>A key specification is a transparent representation of the key
 * material that constitutes a key. A key may be specified in an
 * algorithm-specific way, or in an algorithm-independent encoding
 * format (such as ASN.1).  This package contains key specifications
 * for Diffie-Hellman public and private keys, as well as key
 * specifications for DES, Triple DES, and PBE secret keys.
 *
 * <p>An algorithm parameter specification is a transparent
 * representation of the sets of parameters used with an
 * algorithm. This package contains algorithm parameter specifications
 * for parameters used with the Diffie-Hellman, DES, Triple DES, PBE,
 * RC2 and RC5 algorithms.
 *
 *
 * <ul>
 * <li>PKCS #1: RSA Cryptography Specifications, Version 2.2 (RFC 8017)</li>
 * <li>PKCS #3: Diffie-Hellman Key-Agreement Standard, Version 1.4,
 * November 1993.</li>
 * <li>PKCS #5: Password-Based Encryption Standard, Version 1.5,
 * November 1993.</li>
 * <li>Federal Information Processing Standards Publication (FIPS PUB) 46-2:
 * Data Encryption Standard (DES) </li>
 * </ul>
 *
 * <h2>Related Documentation</h2>
 *
 * For documentation that includes information about algorithm
 * parameter and key specifications, please see:
 *
 * <ul>
 * <li>
 *    {@extLink security_guide_jca
 *      Java Cryptography Architecture (JCA) Reference Guide} </li>
 * <li>
 *    {@extLink security_guide_impl_provider
 *      How to Implement a Provider in the Java Cryptography Architecture}</li>
 * </ul>
 *
 * @since 1.4
 */
package javax.crypto.spec;
