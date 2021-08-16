/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Provides the classes and interfaces for cryptographic
 * operations. The cryptographic operations defined in this package
 * include encryption, key generation and key agreement, and Message
 * Authentication Code (MAC) generation.
 *
 * <p>Support for encryption includes symmetric, asymmetric, block,
 * and stream ciphers. This package also supports secure streams and
 * sealed objects.
 *
 * <p>Many of the classes provided in this package are provider-based.
 * The class itself defines a programming interface to which
 * applications may write.  The implementations themselves may then be
 * written by independent third-party vendors and plugged in
 * seamlessly as needed.  Therefore application developers may take
 * advantage of any number of provider-based implementations without
 * having to add or rewrite code.
 *
 * <ul>
 *   <li><a href="{@docRoot}/../specs/security/standard-names.html">
 *     <b>Java Security Standard Algorithm Names Specification
 *     </b></a></li>
 * </ul>
 *
 * <h2>Related Documentation</h2>
 *
 * For further documentation, please see:
 * <ul>
 *   <li>
 *     {@extLink security_guide_jca
 *       Java Cryptography Architecture (JCA) Reference Guide}</li>
 *   <li>
 *     {@extLink security_guide_impl_provider
 *       How to Implement a Provider in the Java Cryptography Architecture}</li>
 * </ul>
 *
 * @since 1.4
 */
package javax.crypto;
