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
 * Provides interfaces for Diffie-Hellman keys as defined in RSA
 * Laboratories' PKCS #3.
 *
 * <P>Note that these interfaces are intended only for key
 * implementations whose key material is accessible and
 * available. These interfaces are not intended for key implementations
 * whose key material resides in inaccessible, protected storage (such
 * as in a hardware device).
 *
 * <P>For more developer information on how to use these interfaces,
 * including information on how to design <code>Key</code> classes for
 * hardware devices, please refer to the cryptographic provider
 * developer guide:
 *
 * <ul>
 *   <li> {@extLink security_guide_impl_provider
 *     How to Implement a Provider in the Java Cryptography Architecture}</li>
 * </ul>
 *
 * <h2>Package Specification</h2>
 *
 * <ul>
 *   <li>PKCS #3: Diffie-Hellman Key-Agreement Standard, Version 1.4,
 *       November 1993.</li>
 * </ul>
 *
 * <h2>Related Documentation</h2>
 *
 * For further documentation, please see:
 * <ul>
 *   <li>
 *     {@extLink security_guide_jca
 *       Java Cryptography Architecture (JCA) Reference Guide}</li>
 * </ul>
 *
 * @since 1.4
 */
package javax.crypto.interfaces;
