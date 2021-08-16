/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8074935 8208602
 * @summary X.509 cert PEM format read
 * @modules java.base/sun.security.provider
 */

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Base64;

import sun.security.provider.X509Factory;
import java.security.cert.CertificateFactory;
import java.io.ByteArrayInputStream;

public class BadPem {

    public static void main(String[] args) throws Exception {
        String ks = System.getProperty("test.src", ".")
                + "/../../../../javax/net/ssl/etc/keystore";
        String pass = "passphrase";
        String alias = "dummy";

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(ks), pass.toCharArray());
        byte[] cert = keyStore.getCertificate(alias).getEncoded();

        // 8074935
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream pout = new PrintStream(bout);
        byte[] CRLF = new byte[] {'\r', '\n'};
        pout.println(X509Factory.BEGIN_CERT);
        for (int i=0; i<cert.length; i += 48) {
            int blockLen = (cert.length > i + 48) ? 48 : (cert.length - i);
            pout.println("!" + Base64.getEncoder()
                    .encodeToString(Arrays.copyOfRange(cert, i, i + blockLen)));
        }
        pout.println(X509Factory.END_CERT);

        try {
            cf.generateCertificate(new ByteArrayInputStream(bout.toByteArray()));
            throw new Exception("Should fail");
        } catch (CertificateException e) {
            // Good
        }

        // 8208602
        bout.reset();
        pout.println(X509Factory.BEGIN_CERT + "  ");
        pout.println(Base64.getMimeEncoder().encodeToString(cert));
        pout.println(X509Factory.END_CERT + "    ");

        cf.generateCertificate(new ByteArrayInputStream(bout.toByteArray()));
    }
}

