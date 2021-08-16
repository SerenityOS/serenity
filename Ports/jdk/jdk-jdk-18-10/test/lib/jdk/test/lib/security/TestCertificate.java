/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.security;

import java.io.ByteArrayInputStream;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.List;

// Certificates taken from old ValWithAnchorByName testcase ***
public enum TestCertificate {
    // Subject: CN=SSLCertificate, O=SomeCompany
    // Issuer: CN=Intermediate CA Cert, O=SomeCompany
    // Validity: Tue Aug 30 14:37:19 PDT 2016 to Wed Aug 30 14:37:19 PDT 2017
    ONE("1000",
        "CN=SSLCertificate, O=SomeCompany",
        "CN=Intermediate CA Cert, O=SomeCompany",
        -1063259762,
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDnTCCAoWgAwIBAgICEAAwDQYJKoZIhvcNAQELBQAwNTEUMBIGA1UEChMLU29t\n" +
        "ZUNvbXBhbnkxHTAbBgNVBAMTFEludGVybWVkaWF0ZSBDQSBDZXJ0MB4XDTE2MDgz\n" +
        "MDIxMzcxOVoXDTE3MDgzMDIxMzcxOVowLzEUMBIGA1UEChMLU29tZUNvbXBhbnkx\n" +
        "FzAVBgNVBAMTDlNTTENlcnRpZmljYXRlMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A\n" +
        "MIIBCgKCAQEAjgv8KKE4CO0rbCjRLA1hXjRiSq30jeusCJ8frbRG+QOBgQ3j6jgc\n" +
        "vk5wG1aTu7R4AFn0/HRDMzP9ZbRlZVIbJUTd8YiaNyZeyWapPnxHWrPCd5e1xopk\n" +
        "ElieDdEH5FiLGtIrWy56CGA1hfQb1vUVYegyeY+TTtMFVHt0PrmMk4ZRgj/GtVNp\n" +
        "BQQYIzaYAcrcWMeCn30ZrhaGAL1hsdgmEVV1wsTD4JeNMSwLwMYem7fg8ondGZIR\n" +
        "kZuGtuSdOHu4Xz+mgDNXTeX/Bp/dQFucxCG+FOOM9Hoz72RY2W8YqgL38RlnwYWp\n" +
        "nUNxhXWFH6vyINRQVEu3IgahR6HXjxM7LwIDAQABo4G8MIG5MBQGA1UdEQQNMAuC\n" +
        "CWxvY2FsaG9zdDAyBggrBgEFBQcBAQQmMCQwIgYIKwYBBQUHMAGGFmh0dHA6Ly9s\n" +
        "b2NhbGhvc3Q6NDIzMzMwHwYDVR0jBBgwFoAUYT525lwHCI4CmuWs8a7poaeKRJ4w\n" +
        "HQYDVR0OBBYEFCaQnOX4L1ovqyfeKuoay+kI+lXgMA4GA1UdDwEB/wQEAwIFoDAd\n" +
        "BgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwDQYJKoZIhvcNAQELBQADggEB\n" +
        "AD8dqQIqFasJcL8lm4mPTsBl0JgNiN8tQcXM7VCvcH+yDvEyh9vudDjuhpSORqPq\n" +
        "f1o/EvJ+gfs269mBnYQujYRvmSd6EAcBntv5zn6amOh03o6PqTY9KaUC/mL9hB84\n" +
        "Y5/LYioP16sME7egKnlrGUgKh0ZvGzm7c3SYx3Z5YoeFBOkZajc7Jm+cBw/uBQkF\n" +
        "a9mLEczIvOgkq1wto8vr2ptH1gEuvFRcorN3muvq34bk40G08+AHlP3fCLFpI3FA\n" +
        "IStJLJZRcO+Ib4sOcKuaBGnuMo/QVOCEMDUs6RgiWtSd93OZKFIUOASVp6YIkcSs\n" +
        "5/rmc06sICqBjLfPEB68Jjw=\n" +
        "-----END CERTIFICATE-----"),
    // Subject: CN=Intermediate CA Cert, O=SomeCompany
    // Issuer: CN=Root CA Cert, O=SomeCompany
    // Validity: Sun Aug 07 14:37:19 PDT 2016 to Tue Aug 07 14:37:19 PDT 2018
    TWO("64",
        "CN=Intermediate CA Cert, O=SomeCompany",
        "CN=Root CA Cert, O=SomeCompany",
        -927189373,
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDdjCCAl6gAwIBAgIBZDANBgkqhkiG9w0BAQsFADAtMRQwEgYDVQQKEwtTb21l\n" +
        "Q29tcGFueTEVMBMGA1UEAxMMUm9vdCBDQSBDZXJ0MB4XDTE2MDgwNzIxMzcxOVoX\n" +
        "DTE4MDgwNzIxMzcxOVowNTEUMBIGA1UEChMLU29tZUNvbXBhbnkxHTAbBgNVBAMT\n" +
        "FEludGVybWVkaWF0ZSBDQSBDZXJ0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB\n" +
        "CgKCAQEAnJR5CnE7GKlQjigExSJ6hHu302mc0PcA6TDgsIitPYD/r8RBbBuE51OQ\n" +
        "7IP7AXmfPUV3/+pO/uxx6mgY5O6XeUl7KadhVPtPcL0BVVevCSOdTMVa3iV4zRpa\n" +
        "C6Uy2ouUFnafKnDtlbieggyETUoNgVNJYA9L0XNhtSnENoLHC4Pq0v8OsNtsOWFR\n" +
        "NiMTOA49NNDBw85WgPyFAxjqO4z0J0zxdWq3W4rSMB8xrkulv2Rvj3GcfYJK/ab8\n" +
        "V1IJ6PMWCpujASY3BzvYPnN7BKuBjbWJPgZdPYfX1cxeG80u0tOuMfWWiNONSMSA\n" +
        "7m9y304QA0gKqlrFFn9U4hU89kv1IwIDAQABo4GYMIGVMA8GA1UdEwEB/wQFMAMB\n" +
        "Af8wMgYIKwYBBQUHAQEEJjAkMCIGCCsGAQUFBzABhhZodHRwOi8vbG9jYWxob3N0\n" +
        "OjM5MTM0MB8GA1UdIwQYMBaAFJNMsejEyJUB9tiWycVczvpiMVQZMB0GA1UdDgQW\n" +
        "BBRhPnbmXAcIjgKa5azxrumhp4pEnjAOBgNVHQ8BAf8EBAMCAYYwDQYJKoZIhvcN\n" +
        "AQELBQADggEBAE4nOFdW9OirPnRvxihQXYL9CXLuGQz5tr0XgN8wSY6Un9b6CRiK\n" +
        "7obgIGimVdhvUC1qdRcwJqgOfJ2/jR5/5Qo0TVp+ww4dHNdUoj73tagJ7jTu0ZMz\n" +
        "5Zdp0uwd4RD/syvTeVcbPc3m4awtgEvRgzpDMcSeKPZWInlo7fbnowKSAUAfO8de\n" +
        "0cDkxEBkzPIzGNu256cdLZOqOK9wLJ9mQ0zKgi/2NsldNc2pl/6jkGpA6uL5lJsm\n" +
        "fo9sDusWNHV1YggqjDQ19hrf40VuuC9GFl/qAW3marMuEzY/NiKVUxty1q1s48SO\n" +
        "g5LoEPDDkbygOt7ICL3HYG1VufhC1Q2YY9c=\n" +
        "-----END CERTIFICATE-----"),
    // Subject: CN=Root CA Cert, O=SomeCompany
    // Issuer: CN=Root CA Cert, O=SomeCompany
    // Validity: Fri Jul 08 14:37:18 PDT 2016 to Fri Jun 28 14:37:18 PDT 2019
    ROOT_CA("1",
        "CN=Root CA Cert, O=SomeCompany",
        "CN=Root CA Cert, O=SomeCompany",
        -1299818863,
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDODCCAiCgAwIBAgIBATANBgkqhkiG9w0BAQsFADAtMRQwEgYDVQQKEwtTb21l\n" +
        "Q29tcGFueTEVMBMGA1UEAxMMUm9vdCBDQSBDZXJ0MB4XDTE2MDcwODIxMzcxOFoX\n" +
        "DTE5MDYyODIxMzcxOFowLTEUMBIGA1UEChMLU29tZUNvbXBhbnkxFTATBgNVBAMT\n" +
        "DFJvb3QgQ0EgQ2VydDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIlN\n" +
        "M3WYEqkU2elXEZrV9QSDbDKwyaLEHafLFciH8Edoag3q/7jEzFJxI7JZ831tdbWQ\n" +
        "Bm6Hgo+8pvetOFW1BckL8eIjyOONP2CKfFaeMaozsWi1cgxa+rjpU/Rekc+zBqvv\n" +
        "y4Sr97TwT6nQiLlgjC1nCfR1SVpO51qoDChS7n785rsKEZxw/p+kkVWSZffU7zN9\n" +
        "c645cPg//L/kjiyeKMkaquGQOYS68gQgy8YZXQv1E3l/8e8Ci1s1DYA5wpCbaBqg\n" +
        "Tw84Rr4zlUEQBgXzQlRt+mPzeaDpdG1EeGkXrcdkZ+0EMELoOVXOEn6VNsz6vT3I\n" +
        "KrnvQBSnN06xq/iWwC0CAwEAAaNjMGEwDwYDVR0TAQH/BAUwAwEB/zAfBgNVHSME\n" +
        "GDAWgBSTTLHoxMiVAfbYlsnFXM76YjFUGTAdBgNVHQ4EFgQUk0yx6MTIlQH22JbJ\n" +
        "xVzO+mIxVBkwDgYDVR0PAQH/BAQDAgGGMA0GCSqGSIb3DQEBCwUAA4IBAQAAi+Nl\n" +
        "sxP9t2IhiZIHRJGSBZuQlXIjwYIwbq3ZWc/ApZ+0oxtl7DYQi5uRNt8/opcGNCHc\n" +
        "IY0fG93SbkDubXbxPYBW6D/RUjbz59ZryaP5ym55p1MjHTOqy+AM8g41xNTJikc3\n" +
        "UUFXXnckeFbawijCsb7vf71owzKuxgBXi9n1rmXXtncKoA/LrUVXoUlKefdgDnsU\n" +
        "sl3Q29eibE3HSqziMMoAOLm0jjekFGWIgLeTtyRYR1d0dNaUwsHTrQpPjxxUTn1x\n" +
        "sAPpXKfzPnsYAZeeiaaE75GwbWlHzrNinvxdZQd0zctpfBJfVqD/+lWANlw+rOaK\n" +
        "J2GyCaJINsyaI/I2\n" +
        "-----END CERTIFICATE-----");

    private static final CertificateFactory CERTIFICATE_FACTORY = getCertificateFactory();

    public String serialNumber;
    public String algorithm;
    public String subject;
    public String issuer;
    public String keyType;
    public long certId;
    public int keyLength;
    public String encoded;

    TestCertificate(String serialNumber, String subject, String issuer,
                    long certId, String encoded) {
        this.serialNumber = serialNumber;
        this.subject = subject;
        this.issuer = issuer;
        this.algorithm = "SHA256withRSA";
        this.encoded = encoded;
        this.certId = certId;
        this.keyType = "RSA";
        this.keyLength = 2048;
    }

    private static CertificateFactory getCertificateFactory() {
        try {
            return CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public X509Certificate certificate() throws CertificateException {
        ByteArrayInputStream is = new ByteArrayInputStream(encoded.getBytes());
        return (X509Certificate) CERTIFICATE_FACTORY.generateCertificate(is);
    }

    public static void generateChain(boolean selfSignedTest, boolean trustAnchorCert) throws Exception {
        // Do path validation as if it is always Tue, 06 Sep 2016 22:12:21 GMT
        // This value is within the lifetimes of all certificates.
        Date testDate = new Date(1473199941000L);

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate c1 = TestCertificate.ONE.certificate();
        X509Certificate c2 = TestCertificate.TWO.certificate();
        X509Certificate ca = TestCertificate.ROOT_CA.certificate();

        TrustAnchor ta;
        if (trustAnchorCert) {
            ta = new TrustAnchor(ca, null);
        } else {
            ta = new TrustAnchor(ca.getIssuerX500Principal(), ca.getPublicKey(), null);
        }
        CertPathValidator validator = CertPathValidator.getInstance("PKIX");

        PKIXParameters params = new PKIXParameters(Collections.singleton(ta));
        params.setRevocationEnabled(false);
        params.setDate(testDate);
        if (!selfSignedTest) {
            CertPath path = cf.generateCertPath(List.of(c1, c2));
            validator.validate(path, params);
        } else {
            CertPath path = cf.generateCertPath(List.of(ca));
            validator.validate(path, params);
        }
    }
}