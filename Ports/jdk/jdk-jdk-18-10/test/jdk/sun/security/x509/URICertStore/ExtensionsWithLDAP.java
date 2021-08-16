/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8134708
 * @summary Check if LDAP resources from CRLDP and AIA extensions can be loaded
 * @run main/othervm -Djdk.net.hosts.file=${test.src}/CRLDP
 *      -Dcom.sun.security.enableCRLDP=true
 *      ExtensionsWithLDAP CRLDP ldap.host.for.crldp
 * @modules jdk.security.auth
 * @run main/othervm -Djdk.net.hosts.file=${test.src}/AIA
 *      -Dcom.sun.security.enableAIAcaIssuers=true
 *      ExtensionsWithLDAP AIA ldap.host.for.aia
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ExtensionsWithLDAP {

    /*
     *  Certificate:
     *  Data:
     *    Version: 3 (0x2)
     *    Serial Number: 11174053930990688938 (0x9b1236d8f9c1daaa)
     *  Signature Algorithm: sha512WithRSAEncryption
     *    Issuer: CN=Root
     *    Validity
     *        Not Before: Sep  1 18:03:59 2015 GMT
     *        Not After : Jan 17 18:03:59 2043 GMT
     *    Subject: CN=Root
     */
    private static final String CA_CERT = ""
        + "-----BEGIN CERTIFICATE-----\n"
        + "MIIC8TCCAdmgAwIBAgIJAJsSNtj5wdqqMA0GCSqGSIb3DQEBDQUAMA8xDTALBgNV\n"
        + "BAMMBFJvb3QwHhcNMTUwOTAxMTgwMzU5WhcNNDMwMTE3MTgwMzU5WjAPMQ0wCwYD\n"
        + "VQQDDARSb290MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvj892vPm\n"
        + "bB++x9QqqyBveP+ZqQ2B1stV7vh5JmDnOTevkZUOcemp3SXu/esNLSbpL+fARYXH\n"
        + "V5ubnrfip6RbvcxPfVIIDJrRTLIIsU6W7M6/LJLbLkEVGy4ZV4IHkOw9W2O92rcv\n"
        + "BkoqhzZnOTGR6uT3rRcKx4RevEKBKhZO+OPPf//lnckOybmYL7t7yQrajzHro76b\n"
        + "QTXYjAUq/DKhglXfC7vF/JzlAvG2IunGmIfjGcnuDo/9X3Bxef/q5TxCS35fvb7t\n"
        + "svC+g2QhTcBkQh4uNW2jSjlTIVp1uErCfP5aCjLaez5mqmb1hxPIlcvsNR23HwU6\n"
        + "bQO7z7NBo9Do6QIDAQABo1AwTjAdBgNVHQ4EFgQUmLZNOBBkqdYoElyxklPYHmAb\n"
        + "QXIwHwYDVR0jBBgwFoAUmLZNOBBkqdYoElyxklPYHmAbQXIwDAYDVR0TBAUwAwEB\n"
        + "/zANBgkqhkiG9w0BAQ0FAAOCAQEAYV4fOhDi5q7+XNXCxO8Eil2frR9jqdP4LaQp\n"
        + "3L0evW0gvPX68s2WmkPWzIu4TJcpdGFQqxyQFSXuKBXjthyiln77QItGTHWeafES\n"
        + "q5ESrKdSaJZq1bTIrrReCIP74f+fY/F4Tnb3dCqzaljXfzpdbeRsIW6gF71xcOUQ\n"
        + "nnPEjGVPLUegN+Wn/jQpeLxxIB7FmNXncdRUfMfZ43xVSKuMCy1UUYqJqTa/pXZj\n"
        + "jCMeRPThRjRqHlJ69jStfWUQATbLyj9KN09rUaJxzmUSt61UqJi7sjcGySaCjAJc\n"
        + "IcCdVmX/DmRLsdv8W36O3MgrvpT1zR3kaAlv2d8HppnBqcL3xg==\n"
        + "-----END CERTIFICATE-----";

    /*
     *  Certificate:
     *  Data:
     *    Version: 3 (0x2)
     *    Serial Number: 7 (0x7)
     *  Signature Algorithm: sha512WithRSAEncryption
     *    Issuer: CN=Root
     *    Validity
     *       Not Before: Sep  1 18:03:59 2015 GMT
     *       Not After : Jan 17 18:03:59 2043 GMT
     *    Subject: CN=EE
     *    ...
     *  X509v3 extensions:
     *       X509v3 CRL Distribution Points:
     *           Full Name:
     *             URI:ldap://ldap.host.for.crldp/main.crl
     *       Authority Information Access:
     *           CA Issuers - URI:ldap://ldap.host.for.aia/dc=Root?cACertificate
     */
    private static final String EE_CERT = ""
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
        + "-----END CERTIFICATE-----";

    public static void main(String[] args) throws Exception {
        String extension = args[0];
        String targetHost = args[1];

        X509Certificate trustedCert = loadCertificate(CA_CERT);
        X509Certificate eeCert = loadCertificate(EE_CERT);

        Set<TrustAnchor> trustedCertsSet = new HashSet<>();
        trustedCertsSet.add(new TrustAnchor(trustedCert, null));

        CertPath cp = (CertPath) CertificateFactory.getInstance("X509")
                .generateCertPath(Arrays.asList(eeCert));

        // CertPath validator should try to parse CRLDP and AIA extensions,
        // and load CRLs/certs which they point to.
        // If proxy server catches requests for resolving host names
        // which extensions contain, then it means that CertPath validator
        // tried to load CRLs/certs which they point to.
        List<String> hosts = new ArrayList<>();
        Consumer<Socket> socketConsumer = (Socket socket) -> {
            InetSocketAddress remoteAddress
                    = (InetSocketAddress) socket.getRemoteSocketAddress();
            hosts.add(remoteAddress.getHostName());
        };
        try (SocksProxy proxy = SocksProxy.startProxy(socketConsumer)) {
            CertPathValidator.getInstance("PKIX").validate(cp,
                    new PKIXParameters(trustedCertsSet));
            throw new RuntimeException("CertPathValidatorException not thrown");
        } catch (CertPathValidatorException cpve) {
            System.out.println("Expected exception: " + cpve);
        }

        if (!hosts.contains(targetHost)) {
            throw new RuntimeException(
                    String.format("The %s from %s extension is not requested",
                            targetHost, extension));
        }

        System.out.println("Test passed");
    }

    // load a X509 certificate
    public static X509Certificate loadCertificate(String s)
            throws IOException, CertificateException {

        try (ByteArrayInputStream is = new ByteArrayInputStream(s.getBytes())) {
            return (X509Certificate) CertificateFactory.getInstance("X509")
                    .generateCertificate(is);
        }
    }
}
