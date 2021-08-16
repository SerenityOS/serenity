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
 * Classes for parsing and processing {@link
 * javax.xml.crypto.dsig.keyinfo.KeyInfo KeyInfo} elements and structures.
 * <code>KeyInfo</code> is an optional element that enables the recipient(s) to
 * obtain the key needed to validate an {@link
 * javax.xml.crypto.dsig.XMLSignature XMLSignature}. <code>KeyInfo</code> may
 * contain keys, names, certificates and other public key management
 * information, such as in-band key distribution or key agreement data. This
 * package contains classes representing types defined in the W3C specification
 * for XML Signatures, such as
 * {@link javax.xml.crypto.dsig.keyinfo.KeyName KeyName},
 * {@link javax.xml.crypto.dsig.keyinfo.KeyValue KeyValue},
 * {@link javax.xml.crypto.dsig.keyinfo.RetrievalMethod RetrievalMethod},
 * {@link javax.xml.crypto.dsig.keyinfo.X509Data X509Data},
 * {@link javax.xml.crypto.dsig.keyinfo.X509IssuerSerial X509IssuerSerial}, and
 * {@link javax.xml.crypto.dsig.keyinfo.PGPData PGPData}.
 * {@link javax.xml.crypto.dsig.keyinfo.KeyInfoFactory KeyInfoFactory} is an
 * abstract factory that creates <code>KeyInfo</code> objects from scratch.
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

package javax.xml.crypto.dsig.keyinfo;
