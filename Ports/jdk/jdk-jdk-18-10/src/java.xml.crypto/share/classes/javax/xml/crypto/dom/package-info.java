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

/**
 * DOM-specific classes for the {@link javax.xml.crypto} package.
 * Only users who are using DOM-based XML cryptographic implementations (ex:
 * {@link javax.xml.crypto.dsig.XMLSignatureFactory XMLSignatureFactory} or
 * {@link javax.xml.crypto.dsig.keyinfo.KeyInfoFactory})
 * should need to make direct use of this package.
 *
 * <h2>Package Specification</h2>
 *
 * <ul>
 * <li>
 * <a href="http://www.w3.org/TR/xmldsig-core/">
 * XML-Signature Syntax and Processing: W3C Recommendation</a>
 * <li>
 * <a href="http://www.ietf.org/rfc/rfc3275.txt">
 * RFC 3275: XML-Signature Syntax and Processing</a>
 * </ul>
 *
 * @since 1.6
 */

package javax.xml.crypto.dom;
