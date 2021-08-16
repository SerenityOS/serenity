/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8226374
 * @library /javax/net/ssl/templates
 * @summary Restrict signature algorithms and named groups
 * @run main/othervm RestrictSignatureScheme
 */
import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLException;

public class RestrictSignatureScheme extends SSLSocketTemplate {

    private static volatile int index;
    private static final String[][][] protocols = {
        {{"TLSv1.3"}, {"TLSv1.3"}},
        {{"TLSv1.3", "TLSv1.2"}, {"TLSv1.2"}},
        {{"TLSv1.3", "TLSv1.2"}, {"TLSv1.2"}},
        {{"TLSv1.2"}, {"TLSv1.3", "TLSv1.2"}},
        {{"TLSv1.2"}, {"TLSv1.2"}}
    };

    private final SSLContext context;
    RestrictSignatureScheme() throws Exception {
        this.context = createSSLContext();
    }

    @Override
    protected SSLContext createClientSSLContext() throws Exception {
        return context;
    }

    @Override
    protected SSLContext createServerSSLContext() throws Exception {
        return context;
    }

    // Servers are configured before clients, increment test case after.
    @Override
    protected void configureClientSocket(SSLSocket socket) {
        String[] ps = protocols[index][0];

        System.out.print("Setting client protocol(s): ");
        Arrays.stream(ps).forEachOrdered(System.out::print);
        System.out.println();

        socket.setEnabledProtocols(ps);
        socket.setEnabledCipherSuites(new String[] {
            "TLS_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"});
    }

    @Override
    protected void configureServerSocket(SSLServerSocket serverSocket) {
        String[] ps = protocols[index][1];

        System.out.print("Setting server protocol(s): ");
        Arrays.stream(ps).forEachOrdered(System.out::print);
        System.out.println();

        serverSocket.setEnabledProtocols(ps);
        serverSocket.setEnabledCipherSuites(new String[] {
            "TLS_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"});
    }

    /*
     * Run the test case.
     */
    public static void main(String[] args) throws Exception {
        Security.setProperty("jdk.tls.disabledAlgorithms", "RSASSA-PSS");

        for (index = 0; index < protocols.length; index++) {
            try {
                (new RestrictSignatureScheme()).run();
            } catch (SSLException | IllegalStateException ssle) {
                // The named group should be restricted.
                continue;
            }

            throw new Exception("The test case should be disabled");
        }
    }


    private static final String trustedCertStr =
        /**
        * Signature Algorithm: rsassaPss
        * Issuer: CN = localhost
        * Validity Not Before: Jun 6 07:11:00 2018 GMT
        * Not After : Jun 1 07:11:00 2038 GMT
        * Subject: CN = localhost
        * Public Key Algorithm: rsassaPss
        */
       "-----BEGIN CERTIFICATE-----\n"
       + "MIIDZjCCAh2gAwIBAgIUHxwPs3eAgJ057nJwiLgWZWeNqdgwPgYJKoZIhvcNAQEK\n"
       + "MDGgDTALBglghkgBZQMEAgGhGjAYBgkqhkiG9w0BAQgwCwYJYIZIAWUDBAIBogQC\n"
       + "AgDeMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDAeFw0xODA2MDYwNzExMDBaFw0zODA2\n"
       + "MDEwNzExMDBaMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDCCASAwCwYJKoZIhvcNAQEK\n"
       + "A4IBDwAwggEKAoIBAQCl8r4Qrg27BYUO/1Va2Ix8QPGzN/lvzmKvP5Ff26ovNW4v\n"
       + "RUx68HzAhhiWtcl+PwLSbJqJreEkTlle7PnRAypby3fO7ZAK0Y3YiHquaBg7d+7Y\n"
       + "FhhHwv8gG0lZcyA0BkXFJHqdq76qar0xHC6DVezXm0K3mcceymGtFR9BzWmAj+7D\n"
       + "YsSwvtTQ7WNoQmf0cdDMSM71IwaTwIwvT2wzX1vv5hcdDyXdr64WFqWSA9sNJ2K6\n"
       + "arxaaU1klwKSgDokF6njafWQ4UxdR67d5W1MYoiioDs2Yy3utsMpO2OUzZVBZNdT\n"
       + "gkr1jsJhIurpz/5K51lwJIRQBezEFSb+60AFVoMJAgMBAAGjUDBOMB0GA1UdDgQW\n"
       + "BBQfFit5ilWJmZgCX4QY0HsaI9iIDDAfBgNVHSMEGDAWgBQfFit5ilWJmZgCX4QY\n"
       + "0HsaI9iIDDAMBgNVHRMEBTADAQH/MD4GCSqGSIb3DQEBCjAxoA0wCwYJYIZIAWUD\n"
       + "BAIBoRowGAYJKoZIhvcNAQEIMAsGCWCGSAFlAwQCAaIEAgIA3gOCAQEAa4yUQ3gh\n"
       + "d1YWPdEa1sv2hdkhtenw6m5yxbmaQl2+nIKSpk4RfpXC7K1EYwBF8TdfFbD8hGGh\n"
       + "5n81BT0/dn1R9SRGCv7KTxx4lfQt31frlsw/tVciwyXQtcUZ6DqfnLP0/aRVLNgx\n"
       + "zaP542JUHFYLTC3EGz2zUgv70ZUTlIsPG3/p8YO1iXdnYGQyzOuQPUBpI7nS7UtR\n"
       + "Ug8VE9ACpBxxI3qChMahFZGHlXCCSjSmxpQa6UO4SQl8q5tPNnqdzWwvAW8qkCy4\n"
       + "6barRQ4sMcGayhHh/uSTx7bcl0FMJpcI1ygbw7/Pc03zKtw0gMTBMns7q4yXjb/u\n"
       + "ef47nW0t+LRAAg==\n"
       + "-----END CERTIFICATE-----\n";

    private static final String keyCertStr = trustedCertStr;

    private static final String privateKey =
        "MIIEuwIBADALBgkqhkiG9w0BAQoEggSnMIIEowIBAAKCAQEApfK+EK4NuwWFDv9V\n"
        + "WtiMfEDxszf5b85irz+RX9uqLzVuL0VMevB8wIYYlrXJfj8C0myaia3hJE5ZXuz5\n"
        + "0QMqW8t3zu2QCtGN2Ih6rmgYO3fu2BYYR8L/IBtJWXMgNAZFxSR6nau+qmq9MRwu\n"
        + "g1Xs15tCt5nHHsphrRUfQc1pgI/uw2LEsL7U0O1jaEJn9HHQzEjO9SMGk8CML09s\n"
        + "M19b7+YXHQ8l3a+uFhalkgPbDSdiumq8WmlNZJcCkoA6JBep42n1kOFMXUeu3eVt\n"
        + "TGKIoqA7NmMt7rbDKTtjlM2VQWTXU4JK9Y7CYSLq6c/+SudZcCSEUAXsxBUm/utA\n"
        + "BVaDCQIDAQABAoIBAAc4vRS0vlw5LUUtz2UYr2Ro3xvRf8Vh0eGWfpkRUiKjzJu6\n"
        + "BE4FUSh/rWpBlvcrfs/xcfgz3OxbjIAZB/YUkS9Vd21F4VLXM7kMl2onlYZg/b/h\n"
        + "lkTpM3kONu7xl6Er9LVTlRJveuinpHwSoeONRbVMSGb9BjFM1VtW4/lVGxZBG05D\n"
        + "y9i/o4vCZqULn9cAumOwicKuCyTcS58XcMJ+puSPfRA71PYLxqFkASAoJsUwCXpo\n"
        + "gs39lLsIFgrfO8mBO1ux/SE+QaRc+9XqFSHHKD1XqF/9zSYBgWjE910EcpdYEdZx\n"
        + "GEkwea7Fn4brO5OpIrHY/45naqbUOBzv6gufMAECgYEAz7PHCdcrQvmOb8EiNbQH\n"
        + "uvSimwObWJFeN1ykp6mfRbSnkXw7p8+M4Tc8HFi8QLpoq63Ev2AwoaQCQvHbFC2Y\n"
        + "1Cz0EkC0aOp+tZP7U2AUBdkcDesZAJQTad0zV6KesyIUXdxZXDG8JJ1XSNWfTJV4\n"
        + "QD+BjLZ0jiAyCIfVYvWQqYkCgYEAzIln1nKTixLMPr5CldSmR7ZarEtPJU+hHwVg\n"
        + "dV/Lc6d2Yy9JgunOXRo4BXB1TEo8JFbK3HBQH6tS8li4qDr7WK5wyYfh8qb4WZyu\n"
        + "lc562f2WVYntcN8/Ojb+Vyrt7lk9sq/8KoVHxEAWd6mqL9VTPYuAu1Vw9fTGIZfB\n"
        + "lDeELYECgYAvdzU4UXzofGGJtohb332YwwlaBZP9xJLUcg6K5l+orWVSASMc8XiP\n"
        + "i3DoRXsYC8GZ4kdBOPlEJ1gA9oaLcPQpIPDSLwlLpLM6Scw4vI822uvnXl/DWxOo\n"
        + "sM1n7Jj59QLUhGPDhvYpI+/rjC4wcUQe4qR3hMbUKBVnD6u7RsU9iQKBgQCQ17VK\n"
        + "7bSCRfuRaxaoGADww7gOTv5rQ6qr1xjpxb7D1hFGR9Rc+smCsPB/GZZXQjK44SWj\n"
        + "WX3ED4Ubzaxmpe4cbNu+O5XMSmWQwB36RFBHUwdE5/nXdqDFzu/qNqJrqZLBmVKP\n"
        + "ofaiiWffsaytVvotmT6+atElvAMbAua42V+nAQKBgHtIn3mYMHLriYGhQzpkFEA2\n"
        + "8YcAMlKppueOMAKVy8nLu2r3MidmLAhMiKJQKG45I3Yg0/t/25tXLiOPJlwrOebh\n"
        + "xQqUBI/JUOIpGAEnr48jhOXnCS+i+z294G5U/RgjXrlR4bCPvrtCmwzWwe0h79w2\n"
        + "Q2hO5ZTW6UD9CVA85whf";

    private static SSLContext createSSLContext() throws Exception {
        // Generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // Create a key store
        KeyStore ts = KeyStore.getInstance("PKCS12");
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ts.load(null, null);
        ks.load(null, null);
        char passphrase[] = "passphrase".toCharArray();

        // Import the trusted cert
        ts.setCertificateEntry("trusted-cert-RSASSA-PSS",
                cf.generateCertificate(new ByteArrayInputStream(
                        trustedCertStr.getBytes())));

        boolean hasKeyMaterials = keyCertStr != null && privateKey != null;
        if (hasKeyMaterials) {

            // Generate the private key.
            PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec(
                    Base64.getMimeDecoder().decode(privateKey));
            KeyFactory kf = KeyFactory.getInstance("RSASSA-PSS");
            PrivateKey priKey = kf.generatePrivate(priKeySpec);

            // Generate certificate chain
            Certificate keyCert = cf.generateCertificate(
                    new ByteArrayInputStream(keyCertStr.getBytes()));
            Certificate[] chain = new Certificate[]{keyCert};

            // Import the key entry.
            ks.setKeyEntry("cert-RSASSA-PSS", priKey, passphrase, chain);
        }

        // Create SSL context
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(ts);

        SSLContext context = SSLContext.getInstance("TLS");
        if (hasKeyMaterials) {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("NewSunX509");
            kmf.init(ks, passphrase);
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } else {
            context.init(null, tmf.getTrustManagers(), null);
        }

        return context;
    }
}
