/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/*
 * @test
 * @bug 8048621 8133090 8167371 8236671
 * @summary Test basic operations with keystores (jks, jceks, pkcs12)
 * @author Yu-Ching Valerie PENG
 */
public class TestKeyStoreBasic {

    private static final String PRIVATE_KEY_PKCS8_BASE64 = ""
        + "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCpyz97liuWPDYcLH9TX8BiT78o"
        + "lCmAfmevvch6ncXUVuCzbdaKuKXwn4EVbDszsVJLoK5zdtP+X3iDhutj+IgKmLhuczF3M9VIcWr+"
        + "JJUyTH4+3h/RT8cjCDZOmk9iXkb5ifruVsLqzb9g+Vp140Oz7leikne7KmclHvTfvFd0WDI7Gb9v"
        + "o4f5rT717BXJ/n+M6pNk8DLpLiEu6eziYvXRv5x+t5Go3x0eCXdaxEQUf2j876Wfr2qHRJK7lDfF"
        + "e1DDsMg/KpKGiILYZ+g2qtVMZSxtp5BZEtfB5qV/IE5kWO+mCIAGpXSZIdbERR6pZUq8GLEe1T9e"
        + "+sO6H24w2F19AgMBAAECggEBAId/12187dO6wUPCjumuJA1QrrBnbKdKONyai36uoc1Od4s5QFj7"
        + "+hEIeS7rbGNYQuBvnkgusAbzkW0FIpxpHce3EJez/emux6pEOKoP77BwMt9gy+txyu0+BHi91FQg"
        + "AGvrnQDO5EYVY4Cz/WjOsJzKu8zVLg+DS0Toa2qRFwmUe9mVAXPNOCZ3Oae/Q6tCDsaINNw0fmjj"
        + "jn6uohPbS+n6xENG3FkQXB36getXy310xTGED2J27cmAQH6gLR6Kl2iROzNPbbpBqbuemI9kbcld"
        + "EwBS1jRfZWeaPstYA1niVrE9UgUBzemnoh4TDkG076sYthHMr5QFGjPswnwtJ4ECgYEA0sURQ5+v"
        + "baH4tdaemI3qpnknXTlzSpuZZmAoyvY0Id0mlduwKwmZ3Y5989wHfnnhFfyNO4IkTKjI2Wp97qP5"
        + "4eqUNpA7FtNU7KUzMcFDTtwtNZuRYMrKlqo2lLbA+gVrAYpYZFL4b7tcwtX4DnYorDsmude6W8sG"
        + "4Mx2VdFJC9UCgYEAzjsdXCYH5doWUHb0dvn9ID7IikffEMRM720MRjrnnnVbpzx6ACntkPDNZg7p"
        + "TRE/mx7iBz81ZaUWE+V0wd0JvCHEdpAz3mksyvDFhU4Bgs6xzf2pSul5muhsx3hHcvvPezz5Bnxs"
        + "faJlzkxfwotyGmvWN15GA/pyfsZjsbbTpwkCgYAO6NnbysQCIV8SnegCKqfatt9N/O5m7LLhRxQb"
        + "p2bwrlA4cZ34rWkw/w9x3LK7A6wkfgUPnJkswxPSLXJTG05l6M4rPfCwIKr1Qopojp9QSMr569NQ"
        + "4YeLOOc7heIIzbFQHpU6I5Rncv2Q2sn9W+ZsqJKIuvX34FjQNiZ406EzMQKBgHSxOGS61D84DuZK"
        + "2Ps1awhC3kB4eHzJRms3vflDPWoJJ+pSKwpKrzUTPHXiPBqyhtYkPGszVeiE6CAr9sv3YZnFVaBs"
        + "6hyQUJsob+uE/w/gGvXe8VsFDx0bJOodYfhrCbTHBHWqE81nBcocpxayxsayfAzqWB3KKd0YLrMR"
        + "K2PZAoGAcZa8915R2m0KZ6HVJUt/JDR85jCbN71kcVDFY2XSFkOJvOdFoHNfRckfLzjq9Y2MSSTV"
        + "+QDWbDo2doUQCejJUTaN8nP79tfyir24X5uVPvQaeVoGTKYb+LfUqK0F60lStmjuddIGSZH55y3v"
        + "+9XjmxbVERtd1lqgQg3VlmKlEXY=";

    /*
     * Certificate:
     * Data:
     *     Version: 3 (0x2)
     *     Serial Number: 7 (0x7)
     * Signature Algorithm: sha512WithRSAEncryption
     *     Issuer: CN=Root
     *     Validity
     *         Not Before: Sep  1 18:03:59 2015 GMT
     *         Not After : Jan 17 18:03:59 2043 GMT
     *     Subject: CN=EE
     */
    private static final String CERTIFICATE = ""
        + "-----BEGIN CERTIFICATE-----\n"
        + "MIIDHTCCAgWgAwIBAgIBBzANBgkqhkiG9w0BAQ0FADAPMQ0wCwYDVQQDDARSb290\n"
        + "MB4XDTE1MDkwMTE4MDM1OVoXDTQzMDExNzE4MDM1OVowDTELMAkGA1UEAwwCRUUw\n"
        + "ggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCpyz97liuWPDYcLH9TX8Bi\n"
        + "T78olCmAfmevvch6ncXUVuCzbdaKuKXwn4EVbDszsVJLoK5zdtP+X3iDhutj+IgK\n"
        + "mLhuczF3M9VIcWr+JJUyTH4+3h/RT8cjCDZOmk9iXkb5ifruVsLqzb9g+Vp140Oz\n"
        + "7leikne7KmclHvTfvFd0WDI7Gb9vo4f5rT717BXJ/n+M6pNk8DLpLiEu6eziYvXR\n"
        + "v5x+t5Go3x0eCXdaxEQUf2j876Wfr2qHRJK7lDfFe1DDsMg/KpKGiILYZ+g2qtVM\n"
        + "ZSxtp5BZEtfB5qV/IE5kWO+mCIAGpXSZIdbERR6pZUq8GLEe1T9e+sO6H24w2F19\n"
        + "AgMBAAGjgYUwgYIwNAYDVR0fBC0wKzApoCegJYYjbGRhcDovL2xkYXAuaG9zdC5m\n"
        + "b3IuY3JsZHAvbWFpbi5jcmwwSgYIKwYBBQUHAQEEPjA8MDoGCCsGAQUFBzAChi5s\n"
        + "ZGFwOi8vbGRhcC5ob3N0LmZvci5haWEvZGM9Um9vdD9jQUNlcnRpZmljYXRlMA0G\n"
        + "CSqGSIb3DQEBDQUAA4IBAQBWDfZHpuUx0yn5d3+BuztFqoks1MkGdk+USlH0TB1/\n"
        + "gWWBd+4S4PCKlpSur0gj2rMW4fP5HQfNlHci8JV8/bG4KuKRAXW56dg1818Hl3pc\n"
        + "iIrUSRn8uUjH3p9qb+Rb/u3mmVQRyJjN2t/zceNsO8/+Dd808OB9aEwGs8lMT0nn\n"
        + "ZYaaAqYz1GIY/Ecyx1vfEZEQ1ljo6i/r70C3igbypBUShxSiGsleiVTLOGNA+MN1\n"
        + "/a/Qh0bkaQyTGqK3bwvzzMeQVqWu2EWTBD/PmND5ExkpRICdv8LBVXfLnpoBr4lL\n"
        + "hnxn9+e0Ah+t8dS5EKfn44w5bI5PCu2bqxs6RCTxNjcY\n"
        + "-----END CERTIFICATE-----\n";

    private static final char[] PASSWD2 = new char[] {
            'b', 'o', 'r', 'e', 'd'
    };
    private static final char[] PASSWDK = "cannot be null"
            .toCharArray();
    private static final String[] KS_Type = {
            "jks", "jceks", "pkcs12", "PKCS11KeyStore"
    };
    private static final String[] PROVIDERS = {
            "SUN", "SunJCE", "SunJSSE"
    };
    private static final String ALIAS_HEAD = "test";

    private static final String CRYPTO_ALG = "PBEWithHmacSHA256AndAES_128";

    public static void main(String args[]) throws Exception {
        TestKeyStoreBasic jstest = new TestKeyStoreBasic();
        jstest.run();
    }

    public void run() throws Exception {
        for (String provider : PROVIDERS) {
            runTest(provider);
            System.out.println("Test with provider " + provider + " passed");
        }
    }

    public void runTest(String provider) throws Exception {

        // load private key
        // all keystore types should support private keys
        KeySpec spec = new PKCS8EncodedKeySpec(
                Base64.getMimeDecoder().decode(PRIVATE_KEY_PKCS8_BASE64));
        PrivateKey privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(spec);

        // load x509 certificate
        Certificate cert;
        try (InputStream is = new BufferedInputStream(
                new ByteArrayInputStream(CERTIFICATE.getBytes()))) {
            cert = CertificateFactory.getInstance("X.509")
                    .generateCertificate(is);
        }

        int numEntries = 5;
        String type = null;
        for (int i = 0; i < PROVIDERS.length; i++) {
            if (provider.compareTo(PROVIDERS[i]) == 0) {
                type = KS_Type[i];
                break;
            }
        }

        System.out.printf("Test %s provider and %s keystore%n", provider, type);
        KeyStore ks = KeyStore.getInstance(type, provider);
        KeyStore ks2 = KeyStore.getInstance(type, ks.getProvider().getName());

        // create an empty key store
        ks.load(null, null);

        // unit test - test with null password
        try {
            ks.setKeyEntry(ALIAS_HEAD, privateKey, null,
                new Certificate[] { cert });
        } catch (KeyStoreException e) {
            if (!e.getMessage().contains("password can\'t be null")) {
                throw new RuntimeException("Unexpected message:" + e.getMessage());
            }
            // expected
        }

        // store the secret keys
        for (int j = 0; j < numEntries; j++) {
            ks.setKeyEntry(ALIAS_HEAD + j, privateKey, PASSWDK,
                    new Certificate[] { cert });
        }

        // initialize the 2nd key store object with the 1st one
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ks.store(baos, PASSWDK);
        byte[] bArr = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(bArr);
        ks2.load(bais, null);

        // check 2nd key store type
        checkType(ks2, type);
        // check the existing aliases for the 2nd key store
        checkAlias(ks2, numEntries);

        // compare the creation date of the 2 key stores for all aliases
        compareCreationDate(ks, ks2, numEntries);
        // remove the last entry from the 2nd key store
        numEntries--;
        ks2.deleteEntry(ALIAS_HEAD + numEntries);

        // re-initialize the 1st key store with the 2nd key store
        baos.reset();
        ks2.store(baos, PASSWD2);
        bais = new ByteArrayInputStream(baos.toByteArray());
        try {
            // expect an exception since the password is incorrect
            ks.load(bais, PASSWDK);
            throw new RuntimeException(
                    "ERROR: passed the loading with incorrect password");
        } catch (IOException ex) {
            System.out.println("Expected exception: " + ex);
            if (!causedBy(ex, UnrecoverableKeyException.class)) {
                ex.printStackTrace(System.out);
                throw new RuntimeException("Unexpected cause");
            }
            System.out.println("Expected cause: "
                    + UnrecoverableKeyException.class.getName());

            bais.reset();
            ks.load(bais, PASSWD2);
            bais.reset();
            ks.load(bais, null);
        }

        // check key store type
        checkType(ks, type);

        // check the existing aliases
        checkAlias(ks, numEntries);

        // compare the creation date of the 2 key stores for all aliases
        compareCreationDate(ks, ks2, numEntries);

        // check setEntry/getEntry with a password protection algorithm
        if ("PKCS12".equalsIgnoreCase(ks.getType())) {
            System.out.println(
                "Skipping the setEntry/getEntry check for PKCS12 keystore...");
            return;
        }
        String alias = ALIAS_HEAD + ALIAS_HEAD;
        KeyStore.PasswordProtection pw =
            new KeyStore.PasswordProtection(PASSWD2, CRYPTO_ALG, null);
        KeyStore.PrivateKeyEntry entry =
            new KeyStore.PrivateKeyEntry(privateKey, new Certificate[]{ cert });
        checkSetEntry(ks, alias, pw, entry);
        ks.setEntry(alias, entry, new KeyStore.PasswordProtection(PASSWD2));
        checkGetEntry(ks, alias, pw);
    }

    // check setEntry with a password protection algorithm
    private void checkSetEntry(KeyStore ks, String alias,
        KeyStore.PasswordProtection pw, KeyStore.Entry entry) throws Exception {
        try {
            ks.setEntry(alias, entry, pw);
            throw new Exception(
                "ERROR: expected KeyStore.setEntry to throw an exception");
        } catch (KeyStoreException e) {
            // ignore the expected exception
        }
    }

    // check getEntry with a password protection algorithm
    private void checkGetEntry(KeyStore ks, String alias,
        KeyStore.PasswordProtection pw) throws Exception {
        try {
            ks.getEntry(alias, pw);
            throw new Exception(
                "ERROR: expected KeyStore.getEntry to throw an exception");
        } catch (KeyStoreException e) {
            // ignore the expected exception
        }
    }

    // check key store type
    private void checkType(KeyStore obj, String type) {
        if (!obj.getType().equals(type)) {
            throw new RuntimeException("ERROR: wrong key store type");
        }
    }

    // check the existing aliases
    private void checkAlias(KeyStore obj, int range) throws KeyStoreException {
        for (int k = 0; k < range; k++) {
            if (!obj.containsAlias(ALIAS_HEAD + k)) {
                throw new RuntimeException("ERROR: alias (" + k
                        + ") should exist");
            }
        }
    }

    // compare the creation dates - true if all the same
    private void compareCreationDate(KeyStore o1, KeyStore o2, int range)
            throws KeyStoreException {
        String alias;
        for (int k = 0; k < range; k++) {
            alias = ALIAS_HEAD + k;
            if (!o1.getCreationDate(alias).equals(o2.getCreationDate(alias))) {
                throw new RuntimeException("ERROR: entry creation time (" + k
                        + ") differs");
            }
        }
    }

    // checks if an exception was caused by specified exception class
    private static boolean causedBy(Exception e, Class klass) {
        Throwable cause = e;
        while ((cause = cause.getCause()) != null) {
            if (cause.getClass().equals(klass)) {
                return true;
            }
        }
        return false;
    }

}
