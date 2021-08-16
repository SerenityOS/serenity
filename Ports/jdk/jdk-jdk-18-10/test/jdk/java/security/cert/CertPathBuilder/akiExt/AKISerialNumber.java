/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8025708
 * @summary make sure a PKIX CertPathBuilder can build a path when an
 *     intermediate CA certificate contains an AKI extension with a key
 *     identifier and no serial number and the end-entity certificate contains
 *     an AKI extension with both a key identifier and a serial number.
 */

import java.io.ByteArrayInputStream;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;

public class AKISerialNumber {

    private static final String ROOT_CERT =
        "MIICfTCCAeagAwIBAgIBATANBgkqhkiG9w0BAQUFADB3MQ0wCwYDVQQDEwRSb290\n" +
        "MRYwFAYDVQQLEw1UZXN0IE9yZyBVbml0MREwDwYDVQQKEwhUZXN0IE9yZzEWMBQG\n" +
        "A1UEBxMNVGVzdCBMb2NhbGl0eTEWMBQGA1UECBMNTWFzc2FjaHVzZXR0czELMAkG\n" +
        "A1UEBhMCVVMwHhcNMTQwMjAxMDUwMDAwWhcNMjQwMjAxMDUwMDAwWjB3MQ0wCwYD\n" +
        "VQQDEwRSb290MRYwFAYDVQQLEw1UZXN0IE9yZyBVbml0MREwDwYDVQQKEwhUZXN0\n" +
        "IE9yZzEWMBQGA1UEBxMNVGVzdCBMb2NhbGl0eTEWMBQGA1UECBMNTWFzc2FjaHVz\n" +
        "ZXR0czELMAkGA1UEBhMCVVMwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAJvL\n" +
        "cZu6Rzf9IrduEDjJxEFv5uBvUNMlIAph7NhfmFH9puPW3Ksci4a5yTCzxI9VeVf3\n" +
        "oYZ/UrZdF+mNZmS23RUh71X5tjMO+xew196M1xNpCRLbjcZ6i4tNdZYkdRIe8ejN\n" +
        "sbBoD7OAvPbQqTygeG4jYjK6ODofSrba3BndNoFxAgMBAAGjGTAXMBUGA1UdEwEB\n" +
        "/wQLMAkBAf8CBH////8wDQYJKoZIhvcNAQEFBQADgYEATvCqn69pNHv0zLiZAXk7\n" +
        "3AKwAoza0wa+1S2rVuZGfBWbV7CxmBHbgcDDbU7/I8pQVkCwOHNkVFnBgNpMuAvU\n" +
        "aDyrHSNS/av5d1yk5WAuGX2B9mSwZdhnAvtz2fsV1q9NptdF54EkIiKtQQmTGnr9\n" +
        "TID8CFEk/qje+AB272B1UJw=\n";

    /**
     * This certificate contains an AuthorityKeyIdentifier with only the
     * keyIdentifier field filled in.
     */
    private static final String INT_CERT_WITH_KEYID_AKI =
        "MIICqTCCAhKgAwIBAgIBAjANBgkqhkiG9w0BAQUFADB3MQ0wCwYDVQQDEwRSb290\n" +
        "MRYwFAYDVQQLEw1UZXN0IE9yZyBVbml0MREwDwYDVQQKEwhUZXN0IE9yZzEWMBQG\n" +
        "A1UEBxMNVGVzdCBMb2NhbGl0eTEWMBQGA1UECBMNTWFzc2FjaHVzZXR0czELMAkG\n" +
        "A1UEBhMCVVMwHhcNMTQwMjAxMDUwMDAwWhcNMjQwMjAxMDUwMDAwWjCBhDEaMBgG\n" +
        "A1UEAxMRSW50ZXJtZWRpYXRlIENBIDIxFjAUBgNVBAsTDVRlc3QgT3JnIFVuaXQx\n" +
        "ETAPBgNVBAoTCFRlc3QgT3JnMRYwFAYDVQQHEw1UZXN0IExvY2FsaXR5MRYwFAYD\n" +
        "VQQIEw1NYXNzYWNodXNldHRzMQswCQYDVQQGEwJVUzCBnzANBgkqhkiG9w0BAQEF\n" +
        "AAOBjQAwgYkCgYEAwKTZekCqb9F9T54s2IXjkQbmLIjQamMpkUlZNrpjjNq9CpTT\n" +
        "POkfxv2UPwzTz3Ij4XFL/kJFBLm8NUOsS5xPJ62pGoZBPw9R0iMTsTce+Fpukqnr\n" +
        "I+8jTRaAvr0tR3pqrE6uHKg7dWYN2SsWesDia/LHhwEN38yyWtSuTTLo4hcCAwEA\n" +
        "AaM3MDUwHwYDVR0jBBgwFoAU6gZP1pO8v7+i8gsFf1gWTf/j3PkwEgYDVR0TAQH/\n" +
        "BAgwBgEB/wIBADANBgkqhkiG9w0BAQUFAAOBgQAQxeQruav4AqQM4gmEfrHr5hOq\n" +
        "mB2CNJ1ZqVfpDZ8GHijncKTpjNoXzzQtV23Ge+39JHOVBNWtk+aghB3iu6xGq7Qn\n" +
        "HlBhg9meqHFqd3igDDD/jhABL2/bEo/M9rv6saYWDFZ8nCIEE6iTLTpRRko4W2Xb\n" +
        "DyzMzMsO1kPNrJaxRg==\n";

    /**
     * This certificate contains an AuthorityKeyIdentifier with all 3 fields
     * (keyIdentifier, authorityCertIssuer, and authorityCertSerialNumber)
     * filled in.
     */
    private static final String EE_CERT_WITH_FULL_AKI =
        "MIIDLjCCApegAwIBAgIBAzANBgkqhkiG9w0BAQUFADCBhDEaMBgGA1UEAxMRSW50\n" +
        "ZXJtZWRpYXRlIENBIDIxFjAUBgNVBAsTDVRlc3QgT3JnIFVuaXQxETAPBgNVBAoT\n" +
        "CFRlc3QgT3JnMRYwFAYDVQQHEw1UZXN0IExvY2FsaXR5MRYwFAYDVQQIEw1NYXNz\n" +
        "YWNodXNldHRzMQswCQYDVQQGEwJVUzAeFw0xNDAyMDEwNTAwMDBaFw0yNDAyMDEw\n" +
        "NTAwMDBaMH0xEzARBgNVBAMTCkVuZCBFbnRpdHkxFjAUBgNVBAsTDVRlc3QgT3Jn\n" +
        "IFVuaXQxETAPBgNVBAoTCFRlc3QgT3JnMRYwFAYDVQQHEw1UZXN0IExvY2FsaXR5\n" +
        "MRYwFAYDVQQIEw1NYXNzYWNodXNldHRzMQswCQYDVQQGEwJVUzCBnzANBgkqhkiG\n" +
        "9w0BAQEFAAOBjQAwgYkCgYEAqady46PdwlKHVP1iaP11CxVyL6cDlPjpwhHCcIUv\n" +
        "nKHbzdamqmHebDcWVBNN/I0TLNCl3ga7n8KyygSN379fG7haU8SNjpy4IDAXM0/x\n" +
        "mwTWNTbKfJEkSoiqx1WUy2JTzRUMhgYPguQNECPxBXAdQrthZ7wQosv6Ro2ySP9O\n" +
        "YqsCAwEAAaOBtTCBsjCBoQYDVR0jBIGZMIGWgBQdeoKxTvlTgW2KgprD69vgHV4X\n" +
        "kKF7pHkwdzENMAsGA1UEAxMEUm9vdDEWMBQGA1UECxMNVGVzdCBPcmcgVW5pdDER\n" +
        "MA8GA1UEChMIVGVzdCBPcmcxFjAUBgNVBAcTDVRlc3QgTG9jYWxpdHkxFjAUBgNV\n" +
        "BAgTDU1hc3NhY2h1c2V0dHMxCzAJBgNVBAYTAlVTggECMAwGA1UdEwEB/wQCMAAw\n" +
        "DQYJKoZIhvcNAQEFBQADgYEAuG4mM1nLF7STQWwmceELZEl49ntapH/RVoekknmd\n" +
        "aNzcL4XQf6BTl8KFUXuThHaukQnGIzFbSZV0hrpSQ5fTN2cSZgD4Fji+HuNURmmd\n" +
        "+Kayl0piHyO1FSbrty0TFhlVNvzKXjmMp6Jdn42KyGOSCoROQcvUWN6xkV3Hvrei\n" +
        "0ZE=\n";

    private static Base64.Decoder b64Decoder = Base64.getMimeDecoder();
    private static CertificateFactory cf;

    public static void main(String[] args) throws Exception {

        cf = CertificateFactory.getInstance("X.509");

        X509Certificate rootCert = getCertFromMimeEncoding(ROOT_CERT);
        TrustAnchor anchor = new TrustAnchor(rootCert, null);

        X509Certificate eeCert = getCertFromMimeEncoding(EE_CERT_WITH_FULL_AKI);
        X509Certificate intCert = getCertFromMimeEncoding(INT_CERT_WITH_KEYID_AKI);

        X509CertSelector sel = new X509CertSelector();
        sel.setCertificate(eeCert);
        PKIXBuilderParameters params = new PKIXBuilderParameters
            (Collections.singleton(anchor), sel);
        params.setRevocationEnabled(false);

        ArrayList<X509Certificate> certs = new ArrayList<>();
        certs.add(intCert);
        certs.add(eeCert);
        CollectionCertStoreParameters ccsp =
            new CollectionCertStoreParameters(certs);
        CertStore cs = CertStore.getInstance("Collection", ccsp);
        params.addCertStore(cs);

        CertPathBuilder cpb = CertPathBuilder.getInstance("PKIX");
        CertPathBuilderResult res = cpb.build(params);
    }

    private static X509Certificate getCertFromMimeEncoding(String encoded)
        throws CertificateException
    {
        byte[] bytes = b64Decoder.decode(encoded);
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        return (X509Certificate)cf.generateCertificate(stream);
    }
}
