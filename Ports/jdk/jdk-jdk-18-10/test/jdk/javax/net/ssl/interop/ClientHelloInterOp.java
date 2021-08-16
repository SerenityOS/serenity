/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.io.*;
import java.nio.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.KeyFactory;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.*;
import java.util.Base64;

public abstract class ClientHelloInterOp {

    /*
     * Certificates and keys used in the test.
     */
    // Trusted certificates.
    private final static String[] trustedCertStrs = {
        // SHA256withECDSA, curve prime256v1
        // Validity
        //     Not Before: Nov  9 03:24:05 2016 GMT
        //     Not After : Oct 20 03:24:05 2037 GMT
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICHDCCAcGgAwIBAgIJAM83C/MVp9F5MAoGCCqGSM49BAMCMDsxCzAJBgNVBAYT\n" +
        "AlVTMQ0wCwYDVQQKEwRKYXZhMR0wGwYDVQQLExRTdW5KU1NFIFRlc3QgU2VyaXZj\n" +
        "ZTAeFw0xNjExMDkwMzI0MDVaFw0zNzEwMjAwMzI0MDVaMDsxCzAJBgNVBAYTAlVT\n" +
        "MQ0wCwYDVQQKEwRKYXZhMR0wGwYDVQQLExRTdW5KU1NFIFRlc3QgU2VyaXZjZTBZ\n" +
        "MBMGByqGSM49AgEGCCqGSM49AwEHA0IABGeQXwyeNyU4UAATfwUbMO5zaREI21Wh\n" +
        "bds6WDu+PmfK8SWsTgsgpYxBRui+fZtYqSmbdjkurvAQ3j2fvN++BtWjga0wgaow\n" +
        "HQYDVR0OBBYEFDF/OeJ82qBSRkAm1rdZUPbWfDzyMGsGA1UdIwRkMGKAFDF/OeJ8\n" +
        "2qBSRkAm1rdZUPbWfDzyoT+kPTA7MQswCQYDVQQGEwJVUzENMAsGA1UEChMESmF2\n" +
        "YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2Y2WCCQDPNwvzFafReTAPBgNV\n" +
        "HRMBAf8EBTADAQH/MAsGA1UdDwQEAwIBBjAKBggqhkjOPQQDAgNJADBGAiEAlHQY\n" +
        "QFPlODOsjLVQYSxgeSUvYzMp0vP8naeVB9bfFG8CIQCFfrKZvhq9z3bOtlYKxs2a\n" +
        "EWUjUZ82a1JTqkP+lgHY5A==\n" +
        "-----END CERTIFICATE-----",

        // SHA256withRSA, 2048 bits
        // Validity
        //     Not Before: Nov  9 03:24:16 2016 GMT
        //     Not After : Oct 20 03:24:16 2037 GMT
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDpzCCAo+gAwIBAgIJAJAYpR2aIlA1MA0GCSqGSIb3DQEBCwUAMDsxCzAJBgNV\n" +
        "BAYTAlVTMQ0wCwYDVQQKEwRKYXZhMR0wGwYDVQQLExRTdW5KU1NFIFRlc3QgU2Vy\n" +
        "aXZjZTAeFw0xNjExMDkwMzI0MTZaFw0zNzEwMjAwMzI0MTZaMDsxCzAJBgNVBAYT\n" +
        "AlVTMQ0wCwYDVQQKEwRKYXZhMR0wGwYDVQQLExRTdW5KU1NFIFRlc3QgU2VyaXZj\n" +
        "ZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL+F/FTPODYzsU0Pakfp\n" +
        "lsh88YoQWZPjABhCU+HPsCTMYc8UBkaiduUzregwwVBW3D7kmec2K408krGQsxdy\n" +
        "oKJA12GL/XX1YgzDEsyBRk/gvex5lPaBIZiJ5IZlUfjLuRDGxPjtRelBTpZ7SUet\n" +
        "PJVZz6zV6hMPGO6kQzCtbzzET515EE0okIS40LkAmtWoOmVm3gRldomaZTrZ0V2L\n" +
        "MMaJGzrXYqk0SX+PYul8v+2EEHeMuaXG/XpK5xsg9gZvzpKqFQcBOdENoJHB07go\n" +
        "jCmRC328ALqr+bMyktKAuYfB+mhjmN2AU8TQx72WPpvNTXxFDYcwo+8254cCAVKB\n" +
        "e98CAwEAAaOBrTCBqjAdBgNVHQ4EFgQUlJQlQTbi8YIyiNf+SqF7LtH+gicwawYD\n" +
        "VR0jBGQwYoAUlJQlQTbi8YIyiNf+SqF7LtH+giehP6Q9MDsxCzAJBgNVBAYTAlVT\n" +
        "MQ0wCwYDVQQKEwRKYXZhMR0wGwYDVQQLExRTdW5KU1NFIFRlc3QgU2VyaXZjZYIJ\n" +
        "AJAYpR2aIlA1MA8GA1UdEwEB/wQFMAMBAf8wCwYDVR0PBAQDAgEGMA0GCSqGSIb3\n" +
        "DQEBCwUAA4IBAQAI0lTY0YAKQ2VdoIQ6dnqolphLVWdNGiC9drHEYSn7+hmAD2r2\n" +
        "v1U/9m752TkcT74a65xKbEVuVtleD/w6i+QjALW2PYt6ivjOnnY0a9Y9a9UCa00j\n" +
        "C9415sCw84Tp9VoKtuYqzhN87bBUeABOw5dsW3z32C2N/YhprkqeF/vdx4JxulPr\n" +
        "PKze5BREXnKLA1ISoDioCPphvNMKrSpkAofb1rTCwtgt5V/WFls283L52ORmpRGO\n" +
        "Ja88ztXOz00ZGu0RQLwlmpN7m8tNgA/5MPrldyYIwegP4RSkkJlF/8+hxvvqfJhK\n" +
        "FFDa0HHQSJfR2b9628Iniw1UHOMMT6qx5EHr\n" +
        "-----END CERTIFICATE-----"
        };

    // End entity certificate.
    private final static String[] endEntityCertStrs = {
        // SHA256withECDSA, curve prime256v1
        // Validity
        //     Not Before: Nov  9 03:24:05 2016 GMT
        //     Not After : Jul 27 03:24:05 2036 GMT
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIB1DCCAXmgAwIBAgIJAKVa+4dIUjaLMAoGCCqGSM49BAMCMDsxCzAJBgNVBAYT\n" +
        "AlVTMQ0wCwYDVQQKEwRKYXZhMR0wGwYDVQQLExRTdW5KU1NFIFRlc3QgU2VyaXZj\n" +
        "ZTAeFw0xNjExMDkwMzI0MDVaFw0zNjA3MjcwMzI0MDVaMFIxCzAJBgNVBAYTAlVT\n" +
        "MQ0wCwYDVQQKDARKYXZhMR0wGwYDVQQLDBRTdW5KU1NFIFRlc3QgU2VyaXZjZTEV\n" +
        "MBMGA1UEAwwMSW50ZXJPcCBUZXN0MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE\n" +
        "h4vXNUJzULq4e7fAOvF0WiWU6cllOAMus1GqTFvcnRPOChl8suZsvksO0CpZqL3h\n" +
        "jXmVX9dp1FV/rUBGLo1aG6NPME0wCwYDVR0PBAQDAgPoMB0GA1UdDgQWBBSO8V5+\n" +
        "bj0ik0T9BtJc4jLJt7m6wjAfBgNVHSMEGDAWgBQxfznifNqgUkZAJta3WVD21nw8\n" +
        "8jAKBggqhkjOPQQDAgNJADBGAiEAk7MF+L9bFRwUsbPsBCbCqH9DMdzBQR+kFDNf\n" +
        "lfn8Rs4CIQD9qWvBXd+EJqwraxiX6cftaFchn+T2HpvMboy+irMFow==\n" +
        "-----END CERTIFICATE-----",

        // SHA256withRSA, 2048 bits
        // Validity
        //     Not Before: Nov  9 03:24:16 2016 GMT
        //     Not After : Jul 27 03:24:16 2036 GMT
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDczCCAlugAwIBAgIJAPhM2oUKx0aJMA0GCSqGSIb3DQEBCwUAMDsxCzAJBgNV\n" +
        "BAYTAlVTMQ0wCwYDVQQKEwRKYXZhMR0wGwYDVQQLExRTdW5KU1NFIFRlc3QgU2Vy\n" +
        "aXZjZTAeFw0xNjExMDkwMzI0MTZaFw0zNjA3MjcwMzI0MTZaMFIxCzAJBgNVBAYT\n" +
        "AlVTMQ0wCwYDVQQKDARKYXZhMR0wGwYDVQQLDBRTdW5KU1NFIFRlc3QgU2VyaXZj\n" +
        "ZTEVMBMGA1UEAwwMSW50ZXJPcCBUZXN0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A\n" +
        "MIIBCgKCAQEA36tJaXfJ2B/AFvES+tnueyQPSNABVu9nfMdU+NEPamJ+FH7cEF8Z\n" +
        "1Spr1vlQgNzCpDUVrfnmT75rCapgz5ldA9+y+3hdfUyHjZBzzfx+6GHXLB4u6eU2\n" +
        "NATa7vqSLNbcLcfZ7/QmkFqg4JRJbX4F42kKkRJrWdKZ8UoCYC8WXWvDaZ3nUs05\n" +
        "XHe+mBJ8qMNPTbYST1jpzXPyH5CljlFGYi2mKJDTImDhwht7mu2+zvwvbJ81Gj2X\n" +
        "JUSTSf9fu0zxFcCk6RmJPw9nSVqePVlOwtNNBodfKN+k4yr+gOz1v8NmMtmEtklV\n" +
        "Sulr/J4QxI+E2Zar/C+4XjxkvstIS+PNKQIDAQABo2MwYTALBgNVHQ8EBAMCA+gw\n" +
        "HQYDVR0OBBYEFHt19CItAz0VOF0WKGWwaT4DtEsSMB8GA1UdIwQYMBaAFJSUJUE2\n" +
        "4vGCMojX/kqhey7R/oInMBIGA1UdEQEB/wQIMAaHBH8AAAEwDQYJKoZIhvcNAQEL\n" +
        "BQADggEBACKYZWvo9B9IEpCCdBba2sNo4X1NI/VEY3fyUx1lkw+Kna+1d2Ab+RCZ\n" +
        "cf3Y85fcwv03hNE///wNBp+Nde4NQRDK/oiQARzWwWslfinm5d83eQwzC3cpSzt+\n" +
        "7ts6M5UlOblGsLXZI7THWO1tkgoEra9p+zezxLMmf/2MpNyZMZlVoJPM2YGxU9cN\n" +
        "ws0AyeY1gpBEdT21vjsBPdxxj6qklXVMnzS3zF8YwXyOndDYQWdjmFEknRK/qmQ2\n" +
        "gkLHrzpSpyCziecna5mGuDRdCU2dpsWiq1npEPXTq+PQGwWYcoaFTtXF8DDqhfPC\n" +
        "4Abe8gPm6MfzerdmS3RFTj9b/DIIENM=\n" +
        "-----END CERTIFICATE-----"
        };

    // Private key in the format of PKCS#8.
    private final static String[] endEntityPrivateKeys = {
        //
        // EC private key related to cert endEntityCertStrs[0].
        //
        "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgA3pmS+OrIjGyUv2F\n" +
        "K/PkyayJIePM2RTFYxNoQqmJGnihRANCAASHi9c1QnNQurh7t8A68XRaJZTpyWU4\n" +
        "Ay6zUapMW9ydE84KGXyy5my+Sw7QKlmoveGNeZVf12nUVX+tQEYujVob",

        //
        // RSA private key related to cert endEntityCertStrs[1].
        //
        "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDfq0lpd8nYH8AW\n" +
        "8RL62e57JA9I0AFW72d8x1T40Q9qYn4UftwQXxnVKmvW+VCA3MKkNRWt+eZPvmsJ\n" +
        "qmDPmV0D37L7eF19TIeNkHPN/H7oYdcsHi7p5TY0BNru+pIs1twtx9nv9CaQWqDg\n" +
        "lEltfgXjaQqREmtZ0pnxSgJgLxZda8NpnedSzTlcd76YEnyow09NthJPWOnNc/If\n" +
        "kKWOUUZiLaYokNMiYOHCG3ua7b7O/C9snzUaPZclRJNJ/1+7TPEVwKTpGYk/D2dJ\n" +
        "Wp49WU7C000Gh18o36TjKv6A7PW/w2Yy2YS2SVVK6Wv8nhDEj4TZlqv8L7hePGS+\n" +
        "y0hL480pAgMBAAECggEBAJyP1zk+IkloIBtu7+wrdCU6HoDHKMjjlzrehHoOTI4Z\n" +
        "F0vdaMkE6J4vrYCyz0kEPjKW/e/jxvT2wxHm8xEdtuApS61+mWJFmXTcMlNzdJnR\n" +
        "Mr6s+gW67fAHngA94OgGFeTtyX2PFxdgeM/6vFMqLZD7S+w0SnR7WEpvla4iB7On\n" +
        "lXqhJKVQeVc+IpByg/S4MmJb91jck73GltCaCL/b6BTrsz+zc/AY5tb8JInxjMZ9\n" +
        "jmjmA+s6l7tnBrFQfJHlF9a374lxCOtZTxyxVJjD7tQcGpsUpSHXZGdpDcT34qYT\n" +
        "UGh0yp2Mc/1PfWni5gS/6UGLrYmT57RRCn5YJBJTEkkCgYEA/XPCNehFaOMSxOZh\n" +
        "OGBVhQ+eRAmdpJfMhSUsDdEdQLZyWGmZsMTHjZZrwevBX/D0dxQYDv/sAl0GZomJ\n" +
        "d6iRCHlscycwx5Q0U/EpacsgRlYHz1nMRzXqS3Ry+8O8qQlliqCLUM7SfVgzdI5/\n" +
        "ll9JMrng9NnRl8ccjEdOGK8g/MMCgYEA4eriKMfRslGY4uOQoTPbuEJSMMwQ2X4k\n" +
        "lPj1p+xSQfU9QBaWJake67oBj3vpCxqN7/VkvCIeC6LCjhLpWHCn4EkdGiqkEdWz\n" +
        "m5CHzpzVIgznzWnbt0rCVL2KdL+ihgY8KPDdsZ6tZrABHuYhsWkAu10wyvuQYM88\n" +
        "3u6yOIQn36MCgYEAk5qR1UEzAxWTPbaJkgKQa5Cf9DHBbDS3eCcg098f8SsPxquh\n" +
        "RRAkwzGCCgqZsJ0sUhkStdGXifzRGHAq7dPuuwe0ABAn2WNXYjeFjcYtQqkhnUFH\n" +
        "tYURsOXdfQAOZEdDqos691GrxjHSraO7bECL6Y3VE+Oyq3jbCFsSgU+kn28CgYBT\n" +
        "mrXZO6FJqVK33FlAns1YEgsSjeJKapklHEDkxNroF9Zz6ifkhgKwX6SGMefbORd/\n" +
        "zsNZsBKIYdI3+52pIf+uS8BeV5tiEkCmeEUZ3AYv1LDP3rX1zc++xmn/rI97o8EN\n" +
        "sZ2JRtyK3OV9RtL/MYmYzPLqm1Ah02+GXLVNnvKWmwKBgE8Ble8CzrXYuuPdGxXz\n" +
        "BZU6HnXQrmTUcgeze0tj8SDHzCfsGsaG6pHrVNkT7CKsRuCHTZLM0kXmUijLFKuP\n" +
        "5xyE257z4IbbEbs+tcbB3p28n4/47MzZkSR3kt8+FrsEMZq5oOHbFTGzgp9dhZCC\n" +
        "dKUqlw5BPHdbxoWB/JpSHGCV"
        };

    // Private key names of endEntityPrivateKeys.
    private final static String[] endEntityPrivateKeyNames = {
        "EC",
        "RSA"
        };

    /*
     * Run the test case.
     */
    public void run() throws Exception {
        SSLEngine serverEngine = createServerEngine();

        //
        // Create and size the buffers appropriately.
        //
        SSLSession session = serverEngine.getSession();
        ByteBuffer serverAppInbound =
            ByteBuffer.allocate(session.getApplicationBufferSize());
        ByteBuffer clientHello =
            ByteBuffer.allocate(session.getPacketBufferSize());

        //
        // Generate a ClientHello message, and check if the server
        // engine can read it or not.
        //
        clientHello.put(createClientHelloMessage());
        clientHello.flip();

        SSLEngineResult serverResult =
                serverEngine.unwrap(clientHello, serverAppInbound);
        log("Server unwrap: ", serverResult);
        runDelegatedTasks(serverResult, serverEngine);

        //
        // Generate server responses to the ClientHello request.
        //
        ByteBuffer clientNetInbound =
            ByteBuffer.allocate(session.getPacketBufferSize());
        ByteBuffer clientAppInbound =
            ByteBuffer.wrap("Hello Client, I'm Server".getBytes());

        serverResult = serverEngine.wrap(clientAppInbound, clientNetInbound);
        log("Server wrap: ", serverResult);
        runDelegatedTasks(serverResult, serverEngine);
    }

    /*
     * Create a ClientHello message.
     */
    abstract protected byte[] createClientHelloMessage();

    /*
     * Create an instance of SSLContext for client use.
     */
    protected SSLContext createClientSSLContext() throws Exception {
        return createSSLContext(trustedCertStrs, null, null, null);
    }

    /*
     * Create an instance of SSLContext for server use.
     */
    protected SSLContext createServerSSLContext() throws Exception {
        return createSSLContext(null,
                endEntityCertStrs, endEntityPrivateKeys,
                endEntityPrivateKeyNames);
    }

    /*
     * Create an instance of SSLContext with the specified trust/key materials.
     */
    protected SSLContext createSSLContext(
            String[] trustedMaterials,
            String[] keyMaterialCerts,
            String[] keyMaterialKeys,
            String[] keyMaterialKeyAlgs) throws Exception {

        KeyStore ts = null;     // trust store
        KeyStore ks = null;     // key store
        char passphrase[] = "passphrase".toCharArray();

        // Generate certificate from cert string.
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // Import the trused certs.
        ByteArrayInputStream is;
        if (trustedMaterials != null && trustedMaterials.length != 0) {
            ts = KeyStore.getInstance("JKS");
            ts.load(null, null);

            Certificate[] trustedCert =
                    new Certificate[trustedMaterials.length];
            for (int i = 0; i < trustedMaterials.length; i++) {
                String trustedCertStr = trustedMaterials[i];

                is = new ByteArrayInputStream(trustedCertStr.getBytes());
                try {
                    trustedCert[i] = cf.generateCertificate(is);
                } finally {
                    is.close();
                }

                ts.setCertificateEntry("trusted-cert-" + i, trustedCert[i]);
            }
        }

        // Import the key materials.
        //
        // Note that certification pathes bigger than one are not supported yet.
        boolean hasKeyMaterials =
            (keyMaterialCerts != null) && (keyMaterialCerts.length != 0) &&
            (keyMaterialKeys != null) && (keyMaterialKeys.length != 0) &&
            (keyMaterialKeyAlgs != null) && (keyMaterialKeyAlgs.length != 0) &&
            (keyMaterialCerts.length == keyMaterialKeys.length) &&
            (keyMaterialCerts.length == keyMaterialKeyAlgs.length);
        if (hasKeyMaterials) {
            ks = KeyStore.getInstance("JKS");
            ks.load(null, null);

            for (int i = 0; i < keyMaterialCerts.length; i++) {
                String keyCertStr = keyMaterialCerts[i];

                // generate the private key.
                PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec(
                    Base64.getMimeDecoder().decode(keyMaterialKeys[i]));
                KeyFactory kf =
                    KeyFactory.getInstance(keyMaterialKeyAlgs[i]);
                PrivateKey priKey = kf.generatePrivate(priKeySpec);

                // generate certificate chain
                is = new ByteArrayInputStream(keyCertStr.getBytes());
                Certificate keyCert = null;
                try {
                    keyCert = cf.generateCertificate(is);
                } finally {
                    is.close();
                }

                Certificate[] chain = new Certificate[] { keyCert };

                // import the key entry.
                ks.setKeyEntry("cert-" + i, priKey, passphrase, chain);
            }
        }

        // Create an SSLContext object.
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(ts);

        SSLContext context = SSLContext.getInstance("TLS");
        if (hasKeyMaterials && ks != null) {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("NewSunX509");
            kmf.init(ks, passphrase);

            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } else {
            context.init(null, tmf.getTrustManagers(), null);
        }

        return context;
    }

    /*
     * Create an instance of SSLEngine in client mode.
     */
    protected SSLEngine createClientEngine() throws Exception {
        return createClientEngine(createClientSSLContext());
    }

    /*
     * Create an instance of SSLEngine in client mode with the
     * specified SSLContext object.
     */
    protected SSLEngine createClientEngine(
        SSLContext context) throws Exception {

        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(true);

        /*
         * Customize the SSLEngine object.
         */
        // blank

        return engine;
    }

    /*
     * Create an instance of SSLEngine in server mode.
     */
    protected SSLEngine createServerEngine() throws Exception {
        return createServerEngine(createServerSSLContext());
    }

    /*
     * Create an instance of SSLEngine in server mode with the
     * specified SSLContext object.
     */
    protected SSLEngine createServerEngine(
        SSLContext context) throws Exception {

        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(false);

        /*
         * Customize the SSLEngine object.
         */
        engine.setNeedClientAuth(false);

        return engine;
    }

    /*
     * Run the delagayed tasks if any.
     *
     * If the result indicates that we have outstanding tasks to do,
     * go ahead and run them in this thread.
     */
    protected static void runDelegatedTasks(SSLEngineResult result,
            SSLEngine engine) throws Exception {

        if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = engine.getDelegatedTask()) != null) {
                log("\trunning delegated task...");
                runnable.run();
            }
            HandshakeStatus hsStatus = engine.getHandshakeStatus();
            if (hsStatus == HandshakeStatus.NEED_TASK) {
                throw new Exception(
                    "handshake shouldn't need additional tasks");
            }
            log("\tnew HandshakeStatus: " + hsStatus);
        }
    }

    /*
     * Logging the specificated message and the SSLEngine operation result.
     */
    protected static void log(String str, SSLEngineResult result) {
        HandshakeStatus hsStatus = result.getHandshakeStatus();
        log(str +
            result.getStatus() + "/" + hsStatus + ", consumed: " +
            result.bytesConsumed() + "/produced: " + result.bytesProduced() +
            " bytes");

        if (hsStatus == HandshakeStatus.FINISHED) {
            log("\t...ready for application data");
        }
    }

    /*
     * Logging the specificated message.
     */
    protected static void log(String str) {
        System.out.println(str);
    }
}
