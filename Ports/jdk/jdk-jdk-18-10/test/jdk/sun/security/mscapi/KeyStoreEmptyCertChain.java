/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8172244
 * @summary Verify that no exception is thrown with empty cert chain
 *    in MSCAPI.
 * @requires os.family == "windows"
 * @modules java.base/sun.security.tools.keytool java.base/sun.security.x509
 * @run main/othervm --add-opens java.base/java.security=ALL-UNNAMED
 *      KeyStoreEmptyCertChain
 */

import java.security.KeyStore;
import java.security.cert.Certificate;
import sun.security.x509.X500Name;
import sun.security.tools.keytool.CertAndKeyGen;
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.KeyStoreSpi;
import java.lang.reflect.*;

public class KeyStoreEmptyCertChain {

    public static void main(String[] args) {

        try {

            KeyStore keyStore = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
            keyStore.load(null, null);

            // Generate a certificate to use for testing
            CertAndKeyGen gen = new CertAndKeyGen("RSA", "SHA256withRSA");
            gen.generate(2048);
            Certificate cert =
                gen.getSelfCertificate(new X500Name("CN=test"), 3600);
            String alias = "JDK-8172244";
            char[] password = "password".toCharArray();
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");

            // generate a private key for the certificate
            kpg.initialize(2048);
            KeyPair keyPair = kpg.generateKeyPair();
            PrivateKey privKey = keyPair.getPrivate();
            // need to bypass checks to store the private key without the cert
            Field spiField = KeyStore.class.getDeclaredField("keyStoreSpi");
            spiField.setAccessible(true);
            KeyStoreSpi spi = (KeyStoreSpi) spiField.get(keyStore);
            spi.engineSetKeyEntry(alias, privKey, password, new Certificate[0]);
            keyStore.store(null, null);

            keyStore.getCertificateAlias(cert);
            keyStore.deleteEntry(alias);
            // test passes if no exception is thrown
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
