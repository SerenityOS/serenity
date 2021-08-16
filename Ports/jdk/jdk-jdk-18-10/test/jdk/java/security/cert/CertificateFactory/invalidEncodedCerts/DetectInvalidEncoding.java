/*
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4776466 8032573
 * @summary check that CertificateFactory rejects invalid encoded X.509 certs
 */

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
import javax.security.auth.x500.X500Principal;
import java.security.GeneralSecurityException;
import java.security.cert.*;

public class DetectInvalidEncoding {

    // Originally found in the test file:
    // java/security/cert/CertificateFactory/invalidEncodedCerts/invalidcert.pem
    // The first character of the PEM encoding has been changed from "M" to
    // "X" to force a failure during decoding.
    private static final String INVALID_CERT =
        "-----BEGIN CERTIFICATE-----\n" +
        "XIICJjCCAdCgAwIBAgIBITANBgkqhkiG9w0BAQQFADCBqTELMAkGA1UEBhMCVVMx\n" +
        "EzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBGcmFuY2lzY28xFTAT\n" +
        "BgNVBAoTDEJFQSBXZWJMb2dpYzERMA8GA1UECxMIU2VjdXJpdHkxIzAhBgNVBAMT\n" +
        "GkRlbW8gQ2VydGlmaWNhdGUgQXV0aG9yaXR5MR4wHAYJKoZIhvcNAQkBFg9zdXBw\n" +
        "b3J0QGJlYS5jb20wHhcNMDAwNTMwMjEzODAxWhcNMDQwNTEzMjEzODAxWjCBjDEL\n" +
        "MAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBG\n" +
        "cmFuY2lzY28xFTATBgNVBAoTDEJFQSBXZWJMb2dpYzEZMBcGA1UEAxMQd2VibG9n\n" +
        "aWMuYmVhLmNvbTEeMBwGCSqGSIb3DQEJARYPc3VwcG9ydEBiZWEuY29tMFwwDQYJ\n" +
        "KoZIhvcNAQEBBQADSwAwSAJBALdsXEHqKHgs6zj0hU5sXMAUHzoT8kgWXmNkKHXH\n" +
        "79qbPh6EfdlriW9G/AbRF/pKrCQu7hhllAxREbqTuSlf2EMCAwEAATANBgkqhkiG\n" +
        "9w0BAQQFAANBACgmqflL5m5LNeJGpWx9aIoABCiuDcpw1fFyegsqGX7CBhffcruS\n" +
        "1p8h5vkHVbMu1frD1UgGnPlOO/K7Ig/KrsU=\n" +
        "-----END CERTIFICATE-----";

    // Created with keytool:
    // keytool -genkeypair -keyalg rsa -keysize 2048 -keystore <KS_FILE>
    //      -alias root -sigalg SHA256withRSA -dname "CN=Root, O=SomeCompany"
    //      -validity 730 -ext bc:critical=ca:true
    //      -ext ku:critical=keyCertSign,cRLSign
    private static final String SINGLE_ROOT_CERT =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDCjCCAfKgAwIBAgIEDUiw+DANBgkqhkiG9w0BAQsFADAlMRQwEgYDVQQKEwtT\n" +
        "b21lQ29tcGFueTENMAsGA1UEAxMEUm9vdDAeFw0xNDA4MjgyMTI5MjZaFw0xNjA4\n" +
        "MjcyMTI5MjZaMCUxFDASBgNVBAoTC1NvbWVDb21wYW55MQ0wCwYDVQQDEwRSb290\n" +
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0VFecSNdH6CJhPOSG127\n" +
        "tuvld4y7GGJ0kQf3Q0b8qgprsXAmn0/bQR+YX7PfS408cFW+q2SWXeY2kC/3chvi\n" +
        "2syMsGdUJrDzuMbYsbvKPKyuJ2GJskX3mSbLMJj5Tzhg4qmwbzDTFIJ51yGa1Wmh\n" +
        "i2+4PhltqT0TohvSVJlBrOWNhmvwv5UWsF4e2i04rebDZQoWkmD3MpImZXF/HYre\n" +
        "9P8NP97vN0xZmh5PySHy2ILXN3ZhTn3tq0YxNSQTaMUfhgoyzWFvZKAnm/tZIh/1\n" +
        "oswwEQPIZJ25AUTm9r3YPQXl1hsNdLU0asEVYRsgzGSTX5gCuUY+KzhStzisOcUY\n" +
        "uQIDAQABo0IwQDAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBBjAdBgNV\n" +
        "HQ4EFgQUz1FBNixG/KCgcn6FOWzxP1hujG0wDQYJKoZIhvcNAQELBQADggEBAL60\n" +
        "ZaNc6eIMbKntGVE/pdxxyKwPdDyAAeEevX23KRWoLbQjHXo5jrfDPhI5k45ztlyU\n" +
        "+tIQbc81LlCl88I4dIx0fvEbxjNaAYhFNXwwSQBs2CuEAdRK8hodXbRcEeI+G10F\n" +
        "ARIVs2C7JNm/RhxskCWgj6tFIOGaTZ9gHyvlQUEM18sr5fXZlXTqspZCmz3t5XPi\n" +
        "5/wYLv6vk7k3G8WzMHbBE0bYI+61cCc8rbMHldtymbwSwiqfKC9y7oPEfRCbzVUe\n" +
        "fgrKcOyVWDuw0y0hhsQL/oONjPp4uK/bl9B7T84t4+ihxdocWKx6eyhFvOvZH9t2\n" +
        "kUylb9yBUYStwGExMHg=\n" +
        "-----END CERTIFICATE-----";

    // Created with keytool:
    // keytool -genkeypair -keyalg rsa -keysize 2048 -keystore <KS_FILE>
    //      -alias root -sigalg SHA256withRSA
    //      -dname "CN=Intermed, O=SomeCompany" -validity 730
    //      -ext bc:critical=ca:true -ext ku:critical=keyCertSign,cRLSign
    // keytool -certreq -keystore <KS_FILE> -sigalg SHA256withRSA
    //      -alias intermed -dname "CN=Intermed, O=SomeCompany"
    // keytool -gencert -keystore <KS_FILE> -alias intermed
    //      -sigalg SHA256withRSA -validity 730
    //      -ext bc:critical=ca:true -ext ku:critical=keyCertSign,cRLSign
    private static final String INTERMED_CA_CERT =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDLzCCAhegAwIBAgIEIIgOyDANBgkqhkiG9w0BAQsFADAlMRQwEgYDVQQKEwtT\n" +
        "b21lQ29tcGFueTENMAsGA1UEAxMEUm9vdDAeFw0xNDA4MjgyMjUyNDJaFw0xNjA4\n" +
        "MDcyMjUyNDJaMCkxFDASBgNVBAoTC1NvbWVDb21wYW55MREwDwYDVQQDEwhJbnRl\n" +
        "cm1lZDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJEecvTWla8kdWx+\n" +
        "HHu5ryfBpJ95I7V4MEajnmzJVZcwvKhDjlDgABDMuVwFEUUSyeOdbWJF3DLKnyMD\n" +
        "KTx6/58kuVak3NX2TJ8cmmIlKf1upFbdrEtjYViSnNrApprfO8B3ORdBbO6QDYza\n" +
        "IkAWdI5GllFnVkb4yhMUBg3zfhglF+bl3D3lVRlp9bCrUZoNRs+mZjhVbcMn22ej\n" +
        "TfG5Y3VpNM4SN8dFIxPQLLk/aao+cmWEQdbQ0R6ydemRukqrw170olSVLeoGGala\n" +
        "3D4oJckde8EgNPcghcsdQ6tpGhkpFhmoyzEsuToR7Gq9UT5V2kkqJneiKXqQg4wz\n" +
        "vMAlUGECAwEAAaNjMGEwHwYDVR0jBBgwFoAUOw+92bevFoJz96pR1DrAkPPUKb0w\n" +
        "DwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYEFLbnErBs\n" +
        "q/Mhci5XElfjjLZp3GRyMA0GCSqGSIb3DQEBCwUAA4IBAQAq8y2DpkSV31IXZ1vr\n" +
        "/Ye+Nj/2NvBydFeHVRGMAN1LJv6/Q42TCSXbr6cDQ4NWQUtPm90yZBYJSznkbShx\n" +
        "HOJEE6R8PRJvoUtMm7fJrNtkybTt6jX4j50Lw8gdYB/rgZb4z8ZQZVEo/0zpW4HV\n" +
        "Gs+q4z8TkdmLR18hl39sUEsxt99AOBk8NtKKVNfBWq9b0QDhRkXfmqhyeXdDsHOV\n" +
        "8ksulsa7hseheHhdjziEOpQugh8qzSea2kFPrLB53VjWfa4qDzEPaNhahho9piCu\n" +
        "82XDnOrcEk9KyHWM7sa7vtK7++W+0MXD/p9nkZ6NHrJXweLriU0DXO6ZY3mzNKJK\n" +
        "435M\n" +
        "-----END CERTIFICATE-----";

    // Subordinate cert created using keytool, both certs exported to
    // files individually, then use openssl to place in a PKCS#7:
    // openssl crl2pkcs7 -nocrl -certfile <INTERMED-CERT-PEM>
    //      -certfile <ROOT-CERT-PEM> -out <P7-DEST-PEM-FILE>
    private static final String PKCS7_INTERMED_ROOT_CERTS =
        "-----BEGIN PKCS7-----\n" +
        "MIIGbgYJKoZIhvcNAQcCoIIGXzCCBlsCAQExADALBgkqhkiG9w0BBwGgggZBMIID\n" +
        "LzCCAhegAwIBAgIEIIgOyDANBgkqhkiG9w0BAQsFADAlMRQwEgYDVQQKEwtTb21l\n" +
        "Q29tcGFueTENMAsGA1UEAxMEUm9vdDAeFw0xNDA4MjgyMjUyNDJaFw0xNjA4MDcy\n" +
        "MjUyNDJaMCkxFDASBgNVBAoTC1NvbWVDb21wYW55MREwDwYDVQQDEwhJbnRlcm1l\n" +
        "ZDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJEecvTWla8kdWx+HHu5\n" +
        "ryfBpJ95I7V4MEajnmzJVZcwvKhDjlDgABDMuVwFEUUSyeOdbWJF3DLKnyMDKTx6\n" +
        "/58kuVak3NX2TJ8cmmIlKf1upFbdrEtjYViSnNrApprfO8B3ORdBbO6QDYzaIkAW\n" +
        "dI5GllFnVkb4yhMUBg3zfhglF+bl3D3lVRlp9bCrUZoNRs+mZjhVbcMn22ejTfG5\n" +
        "Y3VpNM4SN8dFIxPQLLk/aao+cmWEQdbQ0R6ydemRukqrw170olSVLeoGGala3D4o\n" +
        "Jckde8EgNPcghcsdQ6tpGhkpFhmoyzEsuToR7Gq9UT5V2kkqJneiKXqQg4wzvMAl\n" +
        "UGECAwEAAaNjMGEwHwYDVR0jBBgwFoAUOw+92bevFoJz96pR1DrAkPPUKb0wDwYD\n" +
        "VR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYEFLbnErBsq/Mh\n" +
        "ci5XElfjjLZp3GRyMA0GCSqGSIb3DQEBCwUAA4IBAQAq8y2DpkSV31IXZ1vr/Ye+\n" +
        "Nj/2NvBydFeHVRGMAN1LJv6/Q42TCSXbr6cDQ4NWQUtPm90yZBYJSznkbShxHOJE\n" +
        "E6R8PRJvoUtMm7fJrNtkybTt6jX4j50Lw8gdYB/rgZb4z8ZQZVEo/0zpW4HVGs+q\n" +
        "4z8TkdmLR18hl39sUEsxt99AOBk8NtKKVNfBWq9b0QDhRkXfmqhyeXdDsHOV8ksu\n" +
        "lsa7hseheHhdjziEOpQugh8qzSea2kFPrLB53VjWfa4qDzEPaNhahho9piCu82XD\n" +
        "nOrcEk9KyHWM7sa7vtK7++W+0MXD/p9nkZ6NHrJXweLriU0DXO6ZY3mzNKJK435M\n" +
        "MIIDCjCCAfKgAwIBAgIEdffjKTANBgkqhkiG9w0BAQsFADAlMRQwEgYDVQQKEwtT\n" +
        "b21lQ29tcGFueTENMAsGA1UEAxMEUm9vdDAeFw0xNDA4MjgyMjQ2MzZaFw0xNjA4\n" +
        "MjcyMjQ2MzZaMCUxFDASBgNVBAoTC1NvbWVDb21wYW55MQ0wCwYDVQQDEwRSb290\n" +
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhnXc8Avv54Gk2xjVa2yA\n" +
        "lBL/Cug1nyvKl5wqmN+foT6cMOX6bneCkJOJ4lSbch3gvl4ctlX/9hm3pB/+HhSr\n" +
        "em2NcLQrLEq8l9Ar4RnqfoXQR4Uy+4P6wj9OcVV7e/v/+ZPnStOoEAtb5nAwsR2b\n" +
        "hOC/tIFNwflrsmsmtMSoOiNftpYLFF4eOAdpDrXYMrqNu6ZxZsOQ7WZl4SsVOx1N\n" +
        "/IINXwBLyoHJDzLZ0iJEV0O6mh846s0n6QXeK1P5d0uLcoZaZ1k8Q4sRcdoLA6rS\n" +
        "e1WffipBFMvIuoDIigkHZIKVYRLG828rO+PFnRah0ybybkVsN6s3oLxfhswZDvut\n" +
        "OwIDAQABo0IwQDAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBBjAdBgNV\n" +
        "HQ4EFgQUOw+92bevFoJz96pR1DrAkPPUKb0wDQYJKoZIhvcNAQELBQADggEBACBN\n" +
        "wEaV70FKKBINHtNwesd7TB6fgSaVgDZOO08aseHbXnm7AUhtDV3P5rQR2AsKtbg4\n" +
        "COhlKw2/Ki18D4DfdCccFKFTRZBjqj2PxNmn6C68l1/bT4PuUXuM7rW++53RcOA7\n" +
        "TbgLuzA25kSz7XinRvR8L4VwHtppu5tSYEthMIMgLZLGGV9r7kBfpY8lXdxQM8vb\n" +
        "xZUIysasvVtVUFPOTV6g2dfn8QCoqLOmxyzTLdXe4M6acP6f7lmhgr3LMqDtB6K9\n" +
        "pN+OImr77zNdZ+jTB+5e9a8gAvc5ZfG7Nk5RfwUatYTAFZ6Uggy2cKmIRpXCia18\n" +
        "If78mc7goS1+lHkGCs2hADEA\n" +
        "-----END PKCS7-----";

    // Empty PKCS#7 in DER form can be created with openssl:
    // openssl crl2pkcs7 -nocrl -outform DER
    private static final byte[] PKCS7_BER_EMPTY = {
          48,   39,    6,    9,   42, -122,   72, -122,
          -9,   13,    1,    7,    2,  -96,   26,   48,
          24,    2,    1,    1,   49,    0,   48,   11,
           6,    9,   42, -122,   72, -122,   -9,   13,
           1,    7,    1,  -96,    0,  -95,    0,   49,
           0
    };

    private static final String JTEST_ROOT_CRL =
        "-----BEGIN X509 CRL-----\n" +
        "MIICoTCBigIBATANBgkqhkiG9w0BAQsFADA1MQ4wDAYDVQQKEwVKVGVzdDELMAkG\n" +
        "A1UECxMCSVQxFjAUBgNVBAMTDUpUZXN0IFJvb3QgQ0EXDTE0MDkwNDE4NDIyMVqg\n" +
        "MDAuMB8GA1UdIwQYMBaAFO6bllCV6kctH77MfqAtefNeRdsmMAsGA1UdFAQEAgIA\n" +
        "jjANBgkqhkiG9w0BAQsFAAOCAgEAmp8ihtiRthknDC+VzehmlQw5u8MftMZYQYk5\n" +
        "EI04SwyzY9JTL8QHb4u7fXjnZAyN89aYPypI5OSyDsyyGP/JDNsBt2Um/fl0aaCl\n" +
        "Z4Np6x+dB9+oIU1XY7y2+uyQUC5MHivQ5ddbGPoAvK/msbugTGAjHvZpM+l0okiV\n" +
        "3SofDrii5BSosFEkXfkf2oG9ZLO3YamsFMEZaOj/eWDyGhTyJMGsq2/8NeTF21Tp\n" +
        "YkeDcTHqR5KHoYXjOIaS7NjmErm+uDpKH9Lq+JUcYrbUhmjnq5z04EsPF2F2L7Vb\n" +
        "THI+awQAUQit16lXGuz7fFRZi2vPyiaRP5n2QT5D+ac1dAs+oWLDJw6Tf2v9KVTe\n" +
        "OmW62yd6zQqCwBg+n57UcNu3sv/Sq3t7iRuN0AmWlIhu659POPQv7Np6bEo6dIpp\n" +
        "u7Ze6D2KPtM177ETHYlCx2a3g9VEZYKrVhQ2749St0Cp5szVq691jFZAWYOzcfEO\n" +
        "XfK1y25pmlBjvhNIIVRlU+T5rjNb8GaleYKVYnKOcv700K32QxFzcPf7nbNKwW99\n" +
        "tcaNHFNP+LW/XP8I3CJ8toXLLcOITKVwMA+0GlO5eL7eX5POc+vE9+7IzGuybmU4\n" +
        "uslxoLdJ0NSZWpYmf6a6qrJ67cj5i3706H+eBsWQcShfSYreh+TyWQaGk+fkEiUV\n" +
        "iy4QdJ0=\n" +
        "-----END X509 CRL-----";

    private static final String JTEST_INTERMED_CRL =
        "-----BEGIN X509 CRL-----\n" +
        "MIICzzCBuAIBATANBgkqhkiG9w0BAQsFADA/MQ4wDAYDVQQKEwVKVGVzdDELMAkG\n" +
        "A1UECxMCSVQxIDAeBgNVBAMTF0pUZXN0IEludGVybWVkaWF0ZSBDQSAxFw0xNDA5\n" +
        "MDQyMjE2NTRaMCIwIAIBBhcNMTQwOTA0MjIxNjU0WjAMMAoGA1UdFQQDCgEFoDAw\n" +
        "LjAfBgNVHSMEGDAWgBSvRdjbkSMJ3A7s5H6EWghQ+lkw/zALBgNVHRQEBAICAJsw\n" +
        "DQYJKoZIhvcNAQELBQADggIBALJmikMwil8oywhenoO8o9xxCOIU0xrt3KdfiSXw\n" +
        "8MtQXZHT9d1C6tlLAsYkWAfmfTvM2OU6wquFCLLsFmDZszbbCqmn4JhYBSKQMqlm\n" +
        "IHnsiOFPvITW2FU08fWNLM+FtQzPnTFmx/CJo+wfGpq5tZMIbsccsCJ5uvZVAWGh\n" +
        "0KbPmYcJG/O384+kzr/2H2IaoZoMMABec5c5FEF/tpp8jawzY+0VFyaVrumKWdan\n" +
        "+3OvRQxT1wLxfNi2vdxB2rmNPo423qanXZAoVv260um3LYlmXBNK1jwQ9lp78jkT\n" +
        "B7zMVa4hOUWVxdWc/LE6fUYgPsNqZd+hWy/PolIRp5TS21B5hkc5K87LT59GkexK\n" +
        "vNVKQennOLGtH+Q7htK4UeY4Gm/W7UydOQ0k7hZzyfMDkCfLfNfK0l63qKwUku36\n" +
        "UdeI1LXqulPEvb/d7rRAAM9p5Sm+RsECj2bcrZBMdIGXcSo26A5tzZpTEC79i4S1\n" +
        "yxYIooeBnouUkDJ9+VBsJTSKY5fpU8JSkQPRyHKt+trGAkBt2Ka5MqrHtITzQ1vP\n" +
        "5q4tNr45JGEXllH83NlBpWURfsdtkDHa3lxTD/pkrywOCyzz7wQ22D8Kul7EN8nT\n" +
        "7LDbN+O3G9GHICxvWlJHp6HMsqGTuH1MIUR+5uZFOJa1S0IzorUIEieLncDUPgzO\n" +
        "M4JA\n" +
        "-----END X509 CRL-----";

    // PKCS#7 CRL Set containing JTEST root and intermediate CRLs
    private static final String PKCS7_CRL_SET =
        "-----BEGIN PKCS7-----\n" +
        "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\n" +
        "1D4MzjOCQDEA\n" +
        "-----END PKCS7-----";

    public static void main(String[] args) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        List<DecodeTest> validTests = new LinkedList<>();
        List<DecodeTest> invalidTests = new LinkedList<>();

        // Load up positive test cases (for sanity checks)
        StringBuilder sb = new StringBuilder();

        validTests.add(new GenMultiCertTest("Single, valid certificate",
                    SINGLE_ROOT_CERT.getBytes(), null,
                    new X500Principal("CN=Root, O=SomeCompany")));
        validTests.add(new GenMultiCertTest("PEM-encoded PKCS#7 chain",
                    PKCS7_INTERMED_ROOT_CERTS.getBytes(), null,
                    new X500Principal("CN=Intermed, O=SomeCompany"),
                    new X500Principal("CN=Root, O=SomeCompany")));
        validTests.add(new GenMultiCertTest("Two PEM-encoded X509 certs",
                    (INTERMED_CA_CERT + "\n" + SINGLE_ROOT_CERT).getBytes(),
                    null,
                    new X500Principal("CN=Intermed, O=SomeCompany"),
                    new X500Principal("CN=Root, O=SomeCompany")));
        validTests.add(new GenMultiCertTest("Empty data", new byte[0], null));

        sb.append("Certificate 1: CN=Root, O=SomeCompany\n");
        sb.append(SINGLE_ROOT_CERT).append("\n");
        sb.append("Certificate 2: CN=Intermed, O=SomeCompany\n");
        sb.append(INTERMED_CA_CERT).append("\n");
        sb.append("Extra trailing data\n");
        validTests.add(new GenMultiCertTest(
                    "Two PEM-encoded certs with leading/trailing " +
                    "text data around each.", sb.toString().getBytes(), null,
                    new X500Principal("CN=Root, O=SomeCompany"),
                    new X500Principal("CN=Intermed, O=SomeCompany")));
        validTests.add(new GenMultiCertTest(
                    "BER-encoded PKCS#7 with empty certificates segment",
                    PKCS7_BER_EMPTY, null));
        validTests.add(new GenMultiCRLTest(
                    "CRL with leading and trailing text data",
                    ("This is a CRL\n" + JTEST_ROOT_CRL +
                     "\nSee? Told you so\n\n").getBytes(), null,
                    new X500Principal("CN=JTest Root CA,OU=IT,O=JTest")));
        validTests.add(new GenMultiCRLTest(
                    "Two CRLs, one after the other with leading/trailing text",
                    ("This is a CRL\n" + JTEST_ROOT_CRL +
                     "\nAnd this is another CRL\n" + JTEST_INTERMED_CRL +
                     "\nAnd this is trailing text\n").getBytes(), null,
                    new X500Principal("CN=JTest Root CA,OU=IT,O=JTest"),
                    new X500Principal(
                        "CN=JTest Intermediate CA 1,OU=IT,O=JTest")));
        validTests.add(new GenMultiCRLTest("Two CRLs in a PKCS#7 CRL set",
                PKCS7_CRL_SET.getBytes(), null,
                new X500Principal("CN=JTest Root CA,OU=IT,O=JTest"),
                new X500Principal("CN=JTest Intermediate CA 1,OU=IT,O=JTest")));

        // Load up all test cases where we expect failures
        invalidTests.add(new GenSingleCertTest("Invalid PEM encoding",
                    INVALID_CERT.getBytes(),
                    new CertificateParsingException()));
        invalidTests.add(new GenMultiCertTest("Invalid PEM encoding",
                    INVALID_CERT.getBytes(),
                    new CertificateParsingException()));
        invalidTests.add(new GenMultiCertTest(
                    "Two cert sequence, one valid and one invalid",
                    (INTERMED_CA_CERT + "\n" + INVALID_CERT).getBytes(),
                    new CertificateParsingException()));
        invalidTests.add(new GenMultiCertTest("Non-certificate text",
                    "This is not a certificate".getBytes(),
                    new CertificateException()));
        invalidTests.add(new GenMultiCertTest(
                    "Non-certificate text with partial PEM header (4 hyphens)",
                    "----This is not a valid x509 certificate".getBytes(),
                    new CertificateException()));
        invalidTests.add(new GenMultiCertTest(
                    "Leading non-certificate text plus valid PEM header, " +
                    "but not on new line",
                    "This is not valid -----BEGIN CERTIFICATE-----".getBytes(),
                    new CertificateException()));
        byte[] emptyCString = {0};
        invalidTests.add(new GenMultiCertTest("Empty C-style string",
                    emptyCString, new CertificateException()));
        invalidTests.add(new GenMultiCRLTest("Non-CRL text",
                    "This is not a CRL".getBytes(), new CRLException()));
        invalidTests.add(new GenMultiCRLTest("Valid headers, but not a CRL",
                    INTERMED_CA_CERT.getBytes(), new CRLException()));

        System.out.println("===== Valid Tests =====");
        for (DecodeTest dt : validTests) {
            dt.passTest();
        }
        System.out.print("\n");

        System.out.println("===== Invalid Tests =====");
        for (DecodeTest dt : invalidTests) {
            dt.failTest();
        }
    }

    public static abstract class DecodeTest {
        protected String testName;
        protected byte[] testData;
        protected Throwable expectedException;
        protected X500Principal[] principals;
        protected CertificateFactory cf;

        /**
         * Construct a DecodeTest
         *
         * @param name The test name
         * @param input A byte array consisting of the input for this test
         * @param failType An exception whose class should match the expected
         *        exception that will be thrown when this test is run
         * @param princs Zero of more X500Principals which will be used
         *        to compare the output in a success case.
         */
        DecodeTest(String name, byte[] input, Throwable failType,
                X500Principal... princs) throws CertificateException {
            testName = name;
            testData = input.clone();
            expectedException = failType;
            principals = princs;
            cf = CertificateFactory.getInstance("X.509");
        }

        public abstract void passTest() throws GeneralSecurityException;

        public abstract void failTest() throws GeneralSecurityException;
    }

    public static class GenMultiCertTest extends DecodeTest {
        public GenMultiCertTest(String name, byte[] input, Throwable failType,
                X500Principal... princs) throws CertificateException {
            super(name, input, failType, princs);
        }

        @Override
        public void passTest() throws GeneralSecurityException {
            Collection<? extends Certificate> certs;

            System.out.println("generateCertificates(): " + testName);
            certs = cf.generateCertificates(new ByteArrayInputStream(testData));

            // Walk the certs Collection and do a comparison of subject names
            int i = 0;
            if (certs.size() == principals.length) {
                for (Certificate crt : certs) {
                    X509Certificate xc = (X509Certificate)crt;
                    if (!xc.getSubjectX500Principal().equals(
                                principals[i])) {
                        throw new RuntimeException("Name mismatch: " +
                                "cert: " + xc.getSubjectX500Principal() +
                                ", expected: " + principals[i]);
                    }
                    i++;
                }
            } else {
                throw new RuntimeException("Size mismatch: certs = " +
                        certs.size() + ", expected = " +
                        principals.length);
            }
        }

        @Override
        public void failTest() throws GeneralSecurityException {
            Throwable caughtException = null;
            Collection<? extends Certificate> certs = null;

            System.out.println("generateCertificates(): " + testName);
            if (expectedException == null) {
                throw new RuntimeException("failTest requires non-null " +
                        "expectedException");
            }

            try {
                certs =
                    cf.generateCertificates(new ByteArrayInputStream(testData));
            } catch (CertificateException ce) {
                caughtException = ce;
            }

            if (caughtException != null) {
                // It has to be the right kind of exception though...
                if (!caughtException.getClass().equals(
                        expectedException.getClass())) {
                    System.err.println("Unexpected exception thrown. " +
                            "Received: " + caughtException + ", Expected: " +
                            expectedException.getClass());
                    throw new RuntimeException(caughtException);
                }
            } else {
                // For a failure test, we'd expect some kind of exception
                // to be thrown.
                throw new RuntimeException("Failed to catch expected " +
                        "exception " + expectedException.getClass());
            }
        }
    }

    public static class GenSingleCertTest extends DecodeTest {
        public GenSingleCertTest(String name, byte[] input, Throwable failType,
                X500Principal... princs) throws CertificateException {
            super(name, input, failType, princs);
        }

        @Override
        public void passTest() throws GeneralSecurityException {
            X509Certificate cert;

            System.out.println("generateCertificate(): " + testName);
            cert = (X509Certificate)cf.generateCertificate(
                    new ByteArrayInputStream(testData));

            // Compare the cert's subject name against the expected value
            // provided in the test.  If multiple X500Principals were provided
            // just use the first one as the expected value.
            if (!cert.getSubjectX500Principal().equals(principals[0])) {
                throw new RuntimeException("Name mismatch: " +
                        "cert: " + cert.getSubjectX500Principal() +
                        ", expected: " + principals[0]);
            }
        }

        @Override
        public void failTest() throws GeneralSecurityException {
            Throwable caughtException = null;
            X509Certificate cert = null;
            System.out.println("generateCertificate(): " + testName);

            if (expectedException == null) {
                throw new RuntimeException("failTest requires non-null " +
                        "expectedException");
            }

            try {
                cert = (X509Certificate)cf.generateCertificate(
                        new ByteArrayInputStream(testData));
            } catch (CertificateException e) {
                caughtException = e;
            }

            if (caughtException != null) {
                // It has to be the right kind of exception though...
                if (!caughtException.getClass().equals(
                        expectedException.getClass())) {
                    System.err.println("Unexpected exception thrown. " +
                            "Received: " + caughtException + ", Expected: " +
                            expectedException.getClass());
                    throw new RuntimeException(caughtException);
                }
            } else {
                // For a failure test, we'd expect some kind of exception
                // to be thrown.
                throw new RuntimeException("Failed to catch expected " +
                        "exception " + expectedException.getClass());
            }
        }
    }

    public static class GenMultiCRLTest extends DecodeTest {
        public GenMultiCRLTest(String name, byte[] input, Throwable failType,
                X500Principal... princs) throws CertificateException {
            super(name, input, failType, princs);
        }

        @Override
        public void passTest() throws GeneralSecurityException {
            Collection<? extends CRL> crls;

            System.out.println("generateCRLs(): " + testName);
            crls = cf.generateCRLs(new ByteArrayInputStream(testData));

            // Walk the crls Collection and do a comparison of issuer names
            int i = 0;
            if (crls.size() == principals.length) {
                for (CRL revlist : crls) {
                    X509CRL xc = (X509CRL)revlist;
                    if (!xc.getIssuerX500Principal().equals(principals[i])) {
                        throw new RuntimeException("Name mismatch: " +
                                "CRL: " + xc.getIssuerX500Principal() +
                                ", expected: " + principals[i]);
                    }
                    i++;
                }
            } else {
                throw new RuntimeException("Size mismatch: crls = " +
                        crls.size() + ", expected = " +
                        principals.length);
            }
        }

        @Override
        public void failTest() throws GeneralSecurityException {
            Throwable caughtException = null;
            Collection<? extends CRL> crls = null;

            System.out.println("generateCRLs(): " + testName);
            if (expectedException == null) {
                throw new RuntimeException("failTest requires non-null " +
                        "expectedException");
            }

            try {
                crls =
                    cf.generateCRLs(new ByteArrayInputStream(testData));
            } catch (CRLException e) {
                caughtException = e;
            }

            if (caughtException != null) {
                // It has to be the right kind of exception though...
                if (!caughtException.getClass().equals(
                        expectedException.getClass())) {
                    System.err.println("Unexpected exception thrown. " +
                            "Received: " + caughtException + ", Expected: " +
                            expectedException.getClass());
                    throw new RuntimeException(caughtException);
                }
            } else {
                // For a failure test, we'd expect some kind of exception
                // to be thrown.
                throw new RuntimeException("Failed to catch expected " +
                        "exception " + expectedException.getClass());
            }
        }
    }
}
