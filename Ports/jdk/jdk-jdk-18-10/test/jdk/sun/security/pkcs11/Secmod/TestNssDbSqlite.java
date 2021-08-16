/*
 * Copyright (c) 2017, 2018, Red Hat, Inc. and/or its affiliates.
 *
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
 * @bug 8165996
 * @summary Test NSS DB Sqlite
 * @comment There is no NSS on Aix.
 * @requires os.family != "aix"
 * @library /test/lib ../
 * @modules java.base/sun.security.rsa
 *          java.base/sun.security.provider
 *          java.base/sun.security.jca
 *          java.base/sun.security.tools.keytool
 *          java.base/sun.security.x509
 *          java.base/com.sun.crypto.provider
 *          jdk.crypto.cryptoki/sun.security.pkcs11:+open
 * @run main/othervm/timeout=120 TestNssDbSqlite
 * @author Martin Balao (mbalao@redhat.com)
 */

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Signature;

import sun.security.rsa.SunRsaSign;
import sun.security.jca.ProviderList;
import sun.security.jca.Providers;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

public final class TestNssDbSqlite extends SecmodTest {

    private static final boolean enableDebug = true;

    private static Provider sunPKCS11NSSProvider;
    private static Provider sunRsaSignProvider;
    private static Provider sunJCEProvider;
    private static KeyStore ks;
    private static char[] passphrase = "test12".toCharArray();
    private static PrivateKey privateKey;
    private static Certificate certificate;

    public static void main(String[] args) throws Exception {

        if (!initialize()) {
            return;
        }

        if (enableDebug) {
            System.out.println("SunPKCS11 provider: " +
                sunPKCS11NSSProvider);
        }

        testRetrieveKeysFromKeystore();

        System.out.println("Test PASS - OK");
    }

    private static void testRetrieveKeysFromKeystore() throws Exception {

        String plainText = "known plain text";

        ks.setKeyEntry("root_ca_1", privateKey, passphrase,
                new Certificate[]{certificate});
        PrivateKey k1 = (PrivateKey) ks.getKey("root_ca_1", passphrase);

        Signature sS = Signature.getInstance(
                "SHA256withRSA", sunPKCS11NSSProvider);
        sS.initSign(k1);
        sS.update(plainText.getBytes());
        byte[] generatedSignature = sS.sign();

        if (enableDebug) {
            System.out.println("Generated signature: ");
            for (byte b : generatedSignature) {
                System.out.printf("0x%02x, ", (int)(b) & 0xFF);
            }
            System.out.println("");
        }

        Signature sV = Signature.getInstance("SHA256withRSA", sunRsaSignProvider);
        sV.initVerify(certificate);
        sV.update(plainText.getBytes());
        if(!sV.verify(generatedSignature)){
            throw new Exception("Couldn't verify signature");
        }
    }

    private static boolean initialize() throws Exception {
        return initializeProvider();
    }

    private static boolean initializeProvider() throws Exception {
        useSqlite(true);
        if (!initSecmod()) {
            System.out.println("Cannot init security module database, skipping");
            return false;
        }

        sunPKCS11NSSProvider = getSunPKCS11(BASE + SEP + "nss-sqlite.cfg");
        sunJCEProvider = new com.sun.crypto.provider.SunJCE();
        sunRsaSignProvider = new SunRsaSign();
        Providers.setProviderList(ProviderList.newList(
                sunJCEProvider, sunPKCS11NSSProvider,
                new sun.security.provider.Sun(), sunRsaSignProvider));

        ks = KeyStore.getInstance("PKCS11-NSS-Sqlite", sunPKCS11NSSProvider);
        ks.load(null, passphrase);

        CertAndKeyGen gen = new CertAndKeyGen("RSA", "SHA256withRSA");
        gen.generate(2048);
        privateKey = gen.getPrivateKey();
        certificate = gen.getSelfCertificate(new X500Name("CN=Me"), 365);

        return true;
    }
}
