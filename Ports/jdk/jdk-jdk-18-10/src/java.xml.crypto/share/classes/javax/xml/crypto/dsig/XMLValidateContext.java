/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * $Id: XMLValidateContext.java,v 1.8 2005/05/10 16:03:49 mullan Exp $
 */
package javax.xml.crypto.dsig;

import javax.xml.crypto.XMLCryptoContext;

/**
 * Contains context information for validating XML Signatures. This interface
 * is primarily intended for type-safety.
 *
 * <p>Note that <code>XMLValidateContext</code> instances can contain
 * information and state specific to the XML signature structure it is
 * used with. The results are unpredictable if an
 * <code>XMLValidateContext</code> is used with different signature structures
 * (for example, you should not use the same <code>XMLValidateContext</code>
 * instance to validate two different {@link XMLSignature} objects).
 * <p>
 * <b><a id="SupportedProperties"></a>Supported Properties</b>
 * <p>The following properties can be set by an application using the
 * {@link #setProperty setProperty} method.
 * <ul>
 *   <li><code>javax.xml.crypto.dsig.cacheReference</code>: value must be a
 *      {@link Boolean}. This property controls whether or not the
 *      {@link Reference#validate Reference.validate} method will cache the
 *      dereferenced content and pre-digested input for subsequent retrieval via
 *      the {@link Reference#getDereferencedData Reference.getDereferencedData}
 *      and {@link Reference#getDigestInputStream
 *      Reference.getDigestInputStream} methods. The default value if not
 *      specified is <code>Boolean.FALSE</code>.
 * </ul>
 *
 * @author Sean Mullan
 * @author JSR 105 Expert Group
 * @since 1.6
 * @see XMLSignature#validate(XMLValidateContext)
 * @see Reference#validate(XMLValidateContext)
 */
public interface XMLValidateContext extends XMLCryptoContext {}
