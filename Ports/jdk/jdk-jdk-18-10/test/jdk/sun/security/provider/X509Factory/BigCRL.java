/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 7099399
 * @summary cannot deal with CRL file larger than 16MB
 * @modules java.base/sun.security.x509
 * @run main/othervm -Xshare:off -Xmx1024m BigCRL
 */

import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.PrivateKey;
import java.security.cert.X509CRLEntry;
import java.util.Arrays;
import java.util.Date;
import sun.security.x509.*;
import java.security.cert.CertificateFactory;
import java.io.ByteArrayInputStream;

public class BigCRL {

    public static void main(String[] args) throws Exception {
        int n = 500000;
        String ks = System.getProperty("test.src", ".")
                + "/../../../../javax/net/ssl/etc/keystore";
        String pass = "passphrase";
        String alias = "dummy";

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(ks), pass.toCharArray());
        Certificate signerCert = keyStore.getCertificate(alias);
        byte[] encoded = signerCert.getEncoded();
        X509CertImpl signerCertImpl = new X509CertImpl(encoded);
        X509CertInfo signerCertInfo = (X509CertInfo)signerCertImpl.get(
                X509CertImpl.NAME + "." + X509CertImpl.INFO);
        X500Name owner = (X500Name)signerCertInfo.get(X509CertInfo.SUBJECT + "."
                + X509CertInfo.DN_NAME);

        Date date = new Date();
        PrivateKey privateKey = (PrivateKey)
                keyStore.getKey(alias, pass.toCharArray());
        String sigAlgName = signerCertImpl.getSigAlgOID();

        X509CRLEntry[] badCerts = new X509CRLEntry[n];
        CRLExtensions ext = new CRLExtensions();
        ext.set("Reason", new CRLReasonCodeExtension(1));
        for (int i = 0; i < n; i++) {
            badCerts[i] = new X509CRLEntryImpl(
                    BigInteger.valueOf(i), date, ext);
        }
        X509CRLImpl crl = new X509CRLImpl(owner, date, date, badCerts);
        crl.sign(privateKey, sigAlgName);
        byte[] data = crl.getEncodedInternal();

        // Make sure the CRL is big enough
        if ((data[1]&0xff) != 0x84) {
            throw new Exception("The file should be big enough?");
        }

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        cf.generateCRL(new ByteArrayInputStream(data));
    }
}

