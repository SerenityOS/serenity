/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * test
 * @bug 6377058
 * @summary SunJCE depends on sun.security.provider.SignatureImpl
 * behaviour, BC can't load into 1st slot.
 * @author Brad R. Wetmore
 */

import java.io.*;
import java.util.*;
import java.security.cert.*;
import java.security.cert.CertificateException;

public class MyCertificateFactory extends CertificateFactorySpi {

    CertificateFactory cf;

    public MyCertificateFactory() {
        try {
            cf = CertificateFactory.getInstance("X.509", "SUN");
        } catch (Exception e) {
            throw new RuntimeException(
                "Couldn't create the Sun X.509 CertificateFactory");
        }
    }

    public Certificate engineGenerateCertificate(InputStream inStream)
        throws CertificateException {

        Certificate cert = cf.generateCertificate(inStream);
        if (!(cert instanceof X509Certificate)) {
            throw new RuntimeException("Not an X509Certificate");
        }
        return new MyX509CertImpl((X509Certificate)cert);
    }

    public CertPath engineGenerateCertPath(InputStream inStream)
        throws CertificateException {
        return cf.generateCertPath(inStream);
    }

    public CertPath engineGenerateCertPath(InputStream inStream,
        String encoding)
        throws CertificateException {
        return cf.generateCertPath(inStream, encoding);
    }

    public CertPath
        engineGenerateCertPath(List<? extends Certificate> certificates)
        throws CertificateException {
        return cf.generateCertPath(certificates);
    }

    public Iterator<String> engineGetCertPathEncodings() {
        return cf.getCertPathEncodings();
    }

    public Collection<? extends Certificate>
            engineGenerateCertificates(InputStream inStream)
            throws CertificateException {
        return cf.generateCertificates(inStream);
    }

    public CRL engineGenerateCRL(InputStream inStream)
        throws CRLException {
        return cf.generateCRL(inStream);
    }

    public Collection<? extends CRL> engineGenerateCRLs
            (InputStream inStream) throws CRLException {
        return cf.generateCRLs(inStream);
    }
}
