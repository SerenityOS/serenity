/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jarsigner;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * This class defines a content signing service.
 * Implementations must be instantiable using a zero-argument constructor.
 *
 * @since 1.5
 * @author Vincent Ryan
 * @deprecated This class has been deprecated.
 */

@Deprecated(since="9", forRemoval=true)
public abstract class ContentSigner {

    /**
     * Generates a PKCS #7 signed data message.
     * This method is used when the signature has already been generated.
     * The signature, the signer's details, and optionally a signature
     * timestamp and the content that was signed, are all packaged into a
     * signed data message.
     *
     * @param parameters The non-null input parameters.
     * @param omitContent true if the content should be omitted from the
     *         signed data message. Otherwise the content is included.
     * @param applyTimestamp true if the signature should be timestamped.
     *         Otherwise timestamping is not performed.
     * @return A PKCS #7 signed data message.
     * @throws NoSuchAlgorithmException The exception is thrown if the signature
     *         algorithm is unrecognised.
     * @throws CertificateException The exception is thrown if an error occurs
     *         while processing the signer's certificate or the TSA's
     *         certificate.
     * @throws IOException The exception is thrown if an error occurs while
     *         generating the signature timestamp or while generating the signed
     *         data message.
     * @throws NullPointerException The exception is thrown if parameters is
     *         null.
     */
    @SuppressWarnings("removal")
    public abstract byte[] generateSignedData(
        ContentSignerParameters parameters, boolean omitContent,
        boolean applyTimestamp)
            throws NoSuchAlgorithmException, CertificateException, IOException;
}
