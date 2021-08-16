/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

//
// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 8225766
 * @summary Curve in certificate should not affect signature scheme
 *          when using TLSv1.3
 * @library /javax/net/ssl/templates
 * @run main/othervm Tls13NamedGroups
 */

import java.net.*;
import java.io.*;
import javax.net.ssl.*;
import java.security.*;
import java.security.cert.*;
import java.security.spec.*;
import java.security.interfaces.*;
import java.util.Base64;

public class Tls13NamedGroups extends SSLSocketTemplate {

    public static void main(String[] args) throws Exception {
        // Limit the supported named group to secp521r1.
        System.setProperty("jdk.tls.namedGroups", "secp521r1");

        new Tls13NamedGroups().run();
    }

    @Override
    protected SSLContext createServerSSLContext() throws Exception {
        return generateSSLContext();
    }

    @Override
    protected void configureServerSocket(SSLServerSocket socket) {
        socket.setNeedClientAuth(true);
    }

    @Override
    protected SSLContext createClientSSLContext() throws Exception {
        return generateSSLContext();
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */

    // Certificates and key used in the test.
    //
    // Trusted Certificate.
    static String trustedCertStr =
        // SHA256withECDSA, curve prime256v1
        // Validity
        //     Not Before: May 22 07:18:16 2018 GMT
        //     Not After : May 17 07:18:16 2038 GMT
        // Subject Key Identifier:
        //     60:CF:BD:73:FF:FA:1A:30:D2:A4:EC:D3:49:71:46:EF:1A:35:A0:86
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIBvjCCAWOgAwIBAgIJAIvFG6GbTroCMAoGCCqGSM49BAMCMDsxCzAJBgNVBAYT\n" +
        "AlVTMQ0wCwYDVQQKDARKYXZhMR0wGwYDVQQLDBRTdW5KU1NFIFRlc3QgU2VyaXZj\n" +
        "ZTAeFw0xODA1MjIwNzE4MTZaFw0zODA1MTcwNzE4MTZaMDsxCzAJBgNVBAYTAlVT\n" +
        "MQ0wCwYDVQQKDARKYXZhMR0wGwYDVQQLDBRTdW5KU1NFIFRlc3QgU2VyaXZjZTBZ\n" +
        "MBMGByqGSM49AgEGCCqGSM49AwEHA0IABBz1WeVb6gM2mh85z3QlvaB/l11b5h0v\n" +
        "LIzmkC3DKlVukZT+ltH2Eq1oEkpXuf7QmbM0ibrUgtjsWH3mULfmcWmjUDBOMB0G\n" +
        "A1UdDgQWBBRgz71z//oaMNKk7NNJcUbvGjWghjAfBgNVHSMEGDAWgBRgz71z//oa\n" +
        "MNKk7NNJcUbvGjWghjAMBgNVHRMEBTADAQH/MAoGCCqGSM49BAMCA0kAMEYCIQCG\n" +
        "6wluh1r2/T6L31mZXRKf9JxeSf9pIzoLj+8xQeUChQIhAJ09wAi1kV8yePLh2FD9\n" +
        "2YEHlSQUAbwwqCDEVB5KxaqP\n" +
        "-----END CERTIFICATE-----";
        // -----BEGIN PRIVATE KEY-----
        // MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg/HcHdoLJCdq3haVd
        // XZTSKP00YzM3xX97l98vGL/RI1KhRANCAAQc9VnlW+oDNpofOc90Jb2gf5ddW+Yd
        // LyyM5pAtwypVbpGU/pbR9hKtaBJKV7n+0JmzNIm61ILY7Fh95lC35nFp
        // -----END PRIVATE KEY-----

    // End entity certificate.
    static String targetCertStr =
        // SHA256withECDSA, curve prime256v1
        // Validity
        //     Not Before: May 22 07:18:16 2018 GMT
        //     Not After : May 17 07:18:16 2038 GMT
        // Authority Key Identifier:
        //     60:CF:BD:73:FF:FA:1A:30:D2:A4:EC:D3:49:71:46:EF:1A:35:A0:86
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIBqjCCAVCgAwIBAgIJAPLY8qZjgNRAMAoGCCqGSM49BAMCMDsxCzAJBgNVBAYT\n" +
        "AlVTMQ0wCwYDVQQKDARKYXZhMR0wGwYDVQQLDBRTdW5KU1NFIFRlc3QgU2VyaXZj\n" +
        "ZTAeFw0xODA1MjIwNzE4MTZaFw0zODA1MTcwNzE4MTZaMFUxCzAJBgNVBAYTAlVT\n" +
        "MQ0wCwYDVQQKDARKYXZhMR0wGwYDVQQLDBRTdW5KU1NFIFRlc3QgU2VyaXZjZTEY\n" +
        "MBYGA1UEAwwPUmVncmVzc2lvbiBUZXN0MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcD\n" +
        "QgAEb+9n05qfXnfHUb0xtQJNS4JeSi6IjOfW5NqchvKnfJey9VkJzR7QHLuOESdf\n" +
        "xlR7q8YIWgih3iWLGfB+wxHiOqMjMCEwHwYDVR0jBBgwFoAUYM+9c//6GjDSpOzT\n" +
        "SXFG7xo1oIYwCgYIKoZIzj0EAwIDSAAwRQIgWpRegWXMheiD3qFdd8kMdrkLxRbq\n" +
        "1zj8nQMEwFTUjjQCIQDRIrAjZX+YXHN9b0SoWWLPUq0HmiFIi8RwMnO//wJIGQ==\n" +
        "-----END CERTIFICATE-----";

    // Private key in the format of PKCS#8.
    static String targetPrivateKey =
        //
        // EC private key related to cert endEntityCertStrs[0].
        //
        "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgn5K03bpTLjEtFQRa\n" +
        "JUtx22gtmGEvvSUSQdimhGthdtihRANCAARv72fTmp9ed8dRvTG1Ak1Lgl5KLoiM\n" +
        "59bk2pyG8qd8l7L1WQnNHtAcu44RJ1/GVHurxghaCKHeJYsZ8H7DEeI6";

    static char passphrase[] = "passphrase".toCharArray();

    // Create the SSLContext instance.
    private static SSLContext generateSSLContext() throws Exception {

        // generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // create a key store
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);

        // import the trused cert
        X509Certificate trusedCert = null;
        ByteArrayInputStream is =
                new ByteArrayInputStream(trustedCertStr.getBytes());
        trusedCert = (X509Certificate)cf.generateCertificate(is);
        is.close();

        ks.setCertificateEntry("Trusted EC Signer", trusedCert);

        // generate the private key.
        PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec(
                            Base64.getMimeDecoder().decode(targetPrivateKey));
        KeyFactory kf = KeyFactory.getInstance("EC");
        ECPrivateKey priKey =
                (ECPrivateKey)kf.generatePrivate(priKeySpec);

        // generate certificate chain
        is = new ByteArrayInputStream(targetCertStr.getBytes());
        X509Certificate keyCert = (X509Certificate)cf.generateCertificate(is);
        is.close();

        X509Certificate[] chain = new X509Certificate[2];
        chain[0] = keyCert;
        chain[1] = trusedCert;

        // import the key entry and the chain
        ks.setKeyEntry("TheKey", priKey, passphrase, chain);

        // create SSL context
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(ks);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("NewSunX509");
        kmf.init(ks, passphrase);

        SSLContext ctx = SSLContext.getInstance("TLSv1.3");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        ks = null;

        return ctx;
    }
}
