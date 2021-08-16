/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * @test
 * @bug 8242335
 * @summary OpenSSL generated compatibility test with RSASSA-PSS Java.
 * @run main PSSKeyCompatibility
 */
public class PSSKeyCompatibility {

    private static final String ALGO = "RSASSA-PSS";
    private static final String OID = "1.2.840.113549.1.1.10";
    private static final String PROVIDER = "SunRsaSign";

    public static void main(String[] args) {

        boolean result = true;
        for (String algo : new String[]{ALGO, OID}) {
            System.out.println("With : " + algo);
            result &= validateCert(algo, PROVIDER, PUBLIC_256);
            result &= validateCert(algo, PROVIDER, PUBLIC_384);
            result &= validateCert(algo, PROVIDER, PUBLIC_512);

            result &= validatePrivate(algo, PROVIDER, PRIVATE);
        }
        if (!result) {
            throw new RuntimeException("Some test cases failed");
        }
    }

    private static boolean validatePrivate(String algorithm, String provider,
            String type) {

        try {
            KeyFactory kf = KeyFactory.getInstance(algorithm, provider);
            PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(
                    Base64.getMimeDecoder().decode(type));
            PrivateKey priv = kf.generatePrivate(privSpec);

            RSAPrivateCrtKey crtKey = (RSAPrivateCrtKey) priv;
            PrivateKey priv1 = kf.generatePrivate(new RSAPrivateCrtKeySpec(
                    crtKey.getModulus(),
                    crtKey.getPublicExponent(),
                    crtKey.getPrivateExponent(),
                    crtKey.getPrimeP(),
                    crtKey.getPrimeQ(),
                    crtKey.getPrimeExponentP(),
                    crtKey.getPrimeExponentQ(),
                    crtKey.getCrtCoefficient(),
                    crtKey.getParams()
            ));
            equals(priv, priv1);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException
                | NoSuchProviderException e) {
            e.printStackTrace(System.out);
            return false;
        }
        System.out.println("PASSED - validatePrivate");
        return true;
    }

    private static boolean validateCert(String algorithm, String provider,
            String type) {

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert = cf.generateCertificate(
                    new ByteArrayInputStream(type.getBytes()));
            System.out.println(cert);
            KeyFactory kf = KeyFactory.getInstance(algorithm, provider);
            X509EncodedKeySpec pubSpec = kf.getKeySpec(
                    cert.getPublicKey(), X509EncodedKeySpec.class);
            PublicKey pub = kf.generatePublic(pubSpec);
            PublicKey pub1 = kf.generatePublic(new RSAPublicKeySpec(
                    ((RSAPublicKey) pub).getModulus(),
                    ((RSAPublicKey) pub).getPublicExponent(),
                    ((RSAPublicKey) pub).getParams()));
            equals(cert.getPublicKey(), pub);
            equals(pub, pub1);
        } catch (CertificateException | NoSuchAlgorithmException
                | InvalidKeySpecException | NoSuchProviderException e) {
            e.printStackTrace(System.out);
            return false;
        }
        System.out.println("PASSED - validateCert");
        return true;
    }

    private static void equals(Key orig, Key gen) {
        if (!orig.equals(gen) && orig.hashCode() != gen.hashCode()
                && !Arrays.equals(orig.getEncoded(), gen.getEncoded())) {
            throw new RuntimeException("Key mismatch found");
        }
    }

    //rsa_pss_pss_sha256
    private static final String PRIVATE
            = "MIIEvAIBADALBgkqhkiG9w0BAQoEggSoMIIEpAIBAAKCAQEAu1qb8PZ8vMrX08Gf\n"
            + "y9mx7c5NHymdPIpdDvaiYkpRfYGXp3Jpx7A0Hq01QY0OUu+0sCd5IbiVoVGqM4cq\n"
            + "r2e4eyYnbgJEF7Tg8Ipu70cOUCZLj/fYNAjvFjv4+lxQYRCZHUH+lWPDPtJWKELx\n"
            + "iIsAL5tglfyrQrdWLaOiZKlJ49DrYKU6PYqELxdQ1lw3r8iBbgGJP2podGD0rMWw\n"
            + "nGX4pl9C7dYA+FV2yDirpH+OMNGOqB5QCe2WcsMLMzLPxJxOpqU8lCuscXR0VZuV\n"
            + "krgztGJcq4J0eqp05jvMWii4vW/KSIh9bndVmS2QIU7YArI8RMXtbSHdE0hXAkh+\n"
            + "Phb6/QIDAQABAoIBAQC4gbJDKquLTYQhYXTaT4h/toSS5OuZfHXKz675d1ErdZ2B\n"
            + "ZRaxdmDNuSxSYvSxTqm2NRmA0QRiu0cPudSaq12twdRg7VBbvGEt4lb/xA1fd2dA\n"
            + "4AcGr6mtTuCSxqjN/oebnat3OalFS+VXfx3Yp3NGbxE+hHewm1b+WUELOwCunhYw\n"
            + "WJxs5dR0APiqzknveFgkOSDRbMYhwN6ZIrAmZH0wkGI7ufssnp9LEVDkoQCaFHlW\n"
            + "bUpBHV1YxMCgAD/Azoo7MtedoO/+qnu1h26VhMVMCQL1DymZAnWd5kXumP9PG9j9\n"
            + "z2JwIdYc7wkLVoSHJmjuXn/Sa/X7YCTGNk5Qwp/tAoGBAPJIWN3b6FPasnAevD2O\n"
            + "04l1nnNrjNmu7aMGTTH5CrDseI7y/nqbSC18pOivRLmvhkON26I/Gu8GPKBteZAV\n"
            + "OHKPc4RM11nvv9CyN4yDp0g76pPXLPXRRN/HV0RfBkmaiE6rpS07ue8FDUZmqb9+\n"
            + "T8LV2eCYL7gYnIxsctzEQ8tXAoGBAMX2H7wpZVqHlGW94FF2pLp82q2cP80PBD+Z\n"
            + "TglUVHy957EGPqEzxAWf3saORMDXyme7o0eSHJ1tikNTqAb+//zg5JexNEZSv6cR\n"
            + "trAxuUT7kgjdJaD2i2BjlJyGG6fiXHcxC8lBvnFiWrC+qihTKDPdwWXdEOwzqCdL\n"
            + "0eBbKAvLAoGAKDjah/p6F3G3LeXsWkvb0nY0V/UC7SCdUvM43ZL6s2SOnyy4EqK0\n"
            + "2NhYiEiQoEMDhzOFwum3Dvd6GSgThlf/hwVJqC0Zk1S6A2uSzUEOBG/uAZ03WZfk\n"
            + "V0JAupkL8iw1dNoKEfhYZdXw3j8s7x2JIE9gXGjngyiS1L0sVHpAxwECgYB78csS\n"
            + "23RLB0JhpU2yk6812ABu3LqRoEpPq6PRcYxogdpz2u4RrkCYKO2psd/YQgPHiRMF\n"
            + "N7VU2AXOe61jm/sZEJHvbBLHyP2YFB4nGSrfxwc7J4Ns0ZCYbCDbE5hzN+Ye9oVj\n"
            + "oBcmFKelq+sLzm0IdFqndY8n5HvvBqjEaS6cmwKBgQDM5VsMKnGuqy5pozamgABu\n"
            + "/z3f8ATzPVr85LiEWP7qB9Y1JIFuTma3IVlULtab2S4rhrHqQNy6qA6Be9fKKPwE\n"
            + "TCmM/SDdolcz2d0rC2VDO+pc1RPluDpB/Ag8aHkV58azQASHHvAKBckIe7fay2t2\n"
            + "j4FaKzM/ieY3WSapIbjf3w==";

    /*
     * Certificate: Data: Version: 3 (0x2)
     * Serial Number: 11:4c:35:8c:63:47:91:1d:c1:c8:0f:c2:6f:d0:bd:8b:8f:89:e3:6c
     * Signature Algorithm: rsassaPss
     * Hash Algorithm: sha256
     * Mask Algorithm: mgf1 with sha256
     * Salt Length: 0xDE
     * Trailer Field: 0xBC (default)
     * Issuer: CN = localhost
     * Validity Not Before: Apr 8 06:01:37 2020 GMT
     * Not After : Apr 3 06:01:37 2040 GMT
     * Subject: CN = localhost
     * Subject Public Key Info: Public
     * Key Algorithm: rsassaPss
     * RSA-PSS Public-Key: (2048 bit)
     */
    private static final String PUBLIC_256 = "-----BEGIN CERTIFICATE-----\n"
            + "MIIDaTCCAiCgAwIBAgIUe9ijWtZJGfoH6whOTEIc+J/T1vswPgYJKoZIhvcNAQEK\n"
            + "MDGgDTALBglghkgBZQMEAgGhGjAYBgkqhkiG9w0BAQgwCwYJYIZIAWUDBAIBogQC\n"
            + "AgDeMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDAeFw0yMDAzMTcwNjM4MDdaFw00MDAz\n"
            + "MTIwNjM4MDdaMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDCCASAwCwYJKoZIhvcNAQEK\n"
            + "A4IBDwAwggEKAoIBAQC7Wpvw9ny8ytfTwZ/L2bHtzk0fKZ08il0O9qJiSlF9gZen\n"
            + "cmnHsDQerTVBjQ5S77SwJ3khuJWhUaozhyqvZ7h7JiduAkQXtODwim7vRw5QJkuP\n"
            + "99g0CO8WO/j6XFBhEJkdQf6VY8M+0lYoQvGIiwAvm2CV/KtCt1Yto6JkqUnj0Otg\n"
            + "pTo9ioQvF1DWXDevyIFuAYk/amh0YPSsxbCcZfimX0Lt1gD4VXbIOKukf44w0Y6o\n"
            + "HlAJ7ZZywwszMs/EnE6mpTyUK6xxdHRVm5WSuDO0YlyrgnR6qnTmO8xaKLi9b8pI\n"
            + "iH1ud1WZLZAhTtgCsjxExe1tId0TSFcCSH4+Fvr9AgMBAAGjUzBRMB0GA1UdDgQW\n"
            + "BBSDV090I9jEWvpjZ7fgO+GGocVgaDAfBgNVHSMEGDAWgBSDV090I9jEWvpjZ7fg\n"
            + "O+GGocVgaDAPBgNVHRMBAf8EBTADAQH/MD4GCSqGSIb3DQEBCjAxoA0wCwYJYIZI\n"
            + "AWUDBAIBoRowGAYJKoZIhvcNAQEIMAsGCWCGSAFlAwQCAaIEAgIA3gOCAQEAVl99\n"
            + "g2F0H9YzEtvG5NjSGq8uCW5dLQd5DcXNfyfSLlUBwCTaZXncrc5/3DLYN1mWRQm2\n"
            + "pCwmoGVzslwcLNENldTYogCc0Pc3YeG81wTBq0Tt6zS8RsDR3jhCFSDTVOjOoe0R\n"
            + "kdYRd9d2pLg2ZOzAJXa6GLrFA+3Vv3dFFh8FhGB9CcVsyPQDzWhXQ0IwukHK+AMY\n"
            + "6x1h12/CGQfrzBhrUtwbV+9iZN3lVsBYEFNKVz8Ca7H80YC4bsEHAHeR5nIUFk82\n"
            + "kYuOBhcfC10oz+NdM1KbyAX8/4Uf7S3aBca27GTr1vP6tkmybonRHnZRoELNo1RQ\n"
            + "wM0XPciACllEAJCVrQ==\n"
            + "-----END CERTIFICATE-----";

    /*
     * Certificate: Data: Version: 3 (0x2)
     *  Serial Number: 32:f5:cf:23:71:d3:7f:16:10:5d:6e:c7:25:82:ee:7f:a8:ec:27:80
     *  Signature Algorithm: rsassaPss
     *  Hash Algorithm: sha384
     *  Mask Algorithm: mgf1 with sha384
     *  Salt Length: 0xCE
     *  Trailer Field: 0xBC (default)
     *  Issuer: CN = localhost
     *  Validity Not Before: Apr 8 06:01:37 2020 GMT
     *  Not After : Apr 3 06:01:37 2040 GMT
     *  Subject: CN = localhost
     *  Subject Public Key Info: Public
     *  Key Algorithm: rsassaPss
     *  RSA-PSS Public-Key: (2048 bit)
     */
    private static final String PUBLIC_384 = "-----BEGIN CERTIFICATE-----\n"
            + "MIIDaTCCAiCgAwIBAgIUAeOnPMUidJHBqZbvhJWcH/05h0MwPgYJKoZIhvcNAQEK\n"
            + "MDGgDTALBglghkgBZQMEAgKhGjAYBgkqhkiG9w0BAQgwCwYJYIZIAWUDBAICogQC\n"
            + "AgDOMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDAeFw0yMDAzMTcwNzI2MzFaFw00MDAz\n"
            + "MTIwNzI2MzFaMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDCCASAwCwYJKoZIhvcNAQEK\n"
            + "A4IBDwAwggEKAoIBAQDPgUMdvdYOeVahvAp92RNG55plAlUyEdowNmIpEbyZOlEM\n"
            + "Jc+7VqMt1K/+ZX1MkAGrFjV635p3c0NqI6qyv57cXA7VT92aYp9S0l4t7Cb2DQ6Y\n"
            + "D+1jPNYTpYoMoI8ZPA486RGpnBtmRp9KRSkAoLS6AngCABE7OxuE0MrYKhbJ/8Lq\n"
            + "Ss627FDXK+7aLCbEdLbr5G9BAIMEQDJAomHcqBMz5+EnEXWHc8drHFVIniHByFv3\n"
            + "HmzDhFEMKCV9PbBXjgKdpMIAJsRXG3t1CBE/pEzILomgg3i4OHSUvEIzTApwTJvg\n"
            + "UqtXi0UJqPohPViCQFeWLMa2N0pOAx1FMfdJIutLAgMBAAGjUzBRMB0GA1UdDgQW\n"
            + "BBQBEi9rWGXrZObncP4StBKXB3baODAfBgNVHSMEGDAWgBQBEi9rWGXrZObncP4S\n"
            + "tBKXB3baODAPBgNVHRMBAf8EBTADAQH/MD4GCSqGSIb3DQEBCjAxoA0wCwYJYIZI\n"
            + "AWUDBAICoRowGAYJKoZIhvcNAQEIMAsGCWCGSAFlAwQCAqIEAgIAzgOCAQEADIQ6\n"
            + "2ktTyS9+POWWe8yGEqW/q9DxL0NAqc0n4rYm5hs/8MKk1NMaqIku2xjE3T/16iFj\n"
            + "3WEtj51yoSIUN0VxXPUoj3Yv5xR03huBk8gAwTpQc9psRQuGpLt9BBq0dyErQ8XR\n"
            + "88SshQRpDEZ2yR4Tb+U5XfbWe70uCGfeG3iDMtZPAx2GnYBD+u3JaN/m7sr0cB8V\n"
            + "Y8GuxWNh40aaIR0iaWbIC4b9N3wYDOa1yd8PqAKnLIs1F5CinJM6i5LmbkQpd+cK\n"
            + "t13iaFYN26HuD3AywDQDvyYTwV7q5jcoEGAd35+pmKCdatEHlo0uLzbTGZw31Gfo\n"
            + "BeSEh3vmXa1Q7SOpTQ==\n"
            + "-----END CERTIFICATE-----";
    /*
     * Certificate: Data: Version: 3 (0x2)
     *  Serial Number: 32:f5:cf:23:71:d3:7f:16:10:5d:6e:c7:25:82:ee:7f:a8:ec:27:80
     *  Signature Algorithm: rsassaPss
     *  Hash Algorithm: sha512
     *  Mask Algorithm: mgf1 with sha512
     *  Salt Length: 0xBE
     *  Trailer Field: 0xBC (default)
     *  Issuer: CN = localhost
     *  Validity Not Before: Apr 8 06:01:37 2020 GMT
     *  Not After : Apr 3 06:01:37 2040 GMT
     *  Subject: CN = localhost
     *  Subject Public Key Info: Public
     *  Key Algorithm: rsassaPss
     *  RSA-PSS Public-Key: (2048 bit)
     */
    private static final String PUBLIC_512 = "-----BEGIN CERTIFICATE-----\n"
            + "MIIDaTCCAiCgAwIBAgIUMvXPI3HTfxYQXW7HJYLuf6jsJ4AwPgYJKoZIhvcNAQEK\n"
            + "MDGgDTALBglghkgBZQMEAgOhGjAYBgkqhkiG9w0BAQgwCwYJYIZIAWUDBAIDogQC\n"
            + "AgC+MBQxEjAQBgNVBAMMCWxvY2FsaG9zdDAeFw0yMDAzMTcwNzI4MjZaFw00MDAz\n"
            + "MTIwNzI4MjZaMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDCCASAwCwYJKoZIhvcNAQEK\n"
            + "A4IBDwAwggEKAoIBAQCzuWpxs8c0JPgSykN9LM+2k0RlexrxCAlkgHRpfLI8XpV4\n"
            + "Ak3hx9a045Ym1yyskNw7FjZVfWNgmx5Z8qQZvBykCL2iwDoMLEfoJTcE3cZEppaz\n"
            + "3PqRoOVhuUGqA4jOW8WGbMi7aq/9UfTQGikxMBD7aS/ExILtAcd0N173ZARWcP0s\n"
            + "68bRDLmTYAclZTWDZee0gAl8MHMnXSFFPotSbZOEWz4RqhpCa49tcx1BHgto3lyc\n"
            + "ofzOerHpilZ/zAqOVRF2qHoZKlYTsTcSK0mE2MAfV7fk40qHYkyKbKLJVj8L8Lmc\n"
            + "AFUNTx07bLYymgtqa07ei+kaVTJdlzDWiREgN8MNAgMBAAGjUzBRMB0GA1UdDgQW\n"
            + "BBRlbX8E0L89iIOjkgLpbL/WSbuxmTAfBgNVHSMEGDAWgBRlbX8E0L89iIOjkgLp\n"
            + "bL/WSbuxmTAPBgNVHRMBAf8EBTADAQH/MD4GCSqGSIb3DQEBCjAxoA0wCwYJYIZI\n"
            + "AWUDBAIDoRowGAYJKoZIhvcNAQEIMAsGCWCGSAFlAwQCA6IEAgIAvgOCAQEAaRTy\n"
            + "CmQxYkS5qCGQeJun/lFVLVE83Sl2kCBaJCMJdBYw38H+6DknJx/sjZwD1vO+OGj6\n"
            + "1yyzQF1dv2Y5qOUrJIgw1ODkxTLMCrdotVqClazX02VGvyRe7efbjii96/9hqtxt\n"
            + "TZwN7+8wUX6sP91z1vXVYD5sfl/qum/cWAVJEyw32h7RpUeB0rCJcIUrNqnbBziw\n"
            + "SRkZof1Q2b02JRO0Pb3jV3H1MV5Agt3cFCCdsmvVq595rmYRwVMtyzCxXHb8jm+N\n"
            + "8Fzhl9pxCCd4KIOGDAvngFZAloVsCHt+BG8jPhSxOldnFM7xGrGss2lLJnmf3YSe\n"
            + "EPDF7NvA9wKPz4oyRg==\n"
            + "-----END CERTIFICATE-----";

}
