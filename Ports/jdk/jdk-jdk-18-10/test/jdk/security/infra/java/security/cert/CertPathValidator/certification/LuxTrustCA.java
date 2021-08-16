/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8232019 8256895
 * @summary Interoperability tests with LuxTrust Global Root 2 CA
 * @build ValidatePathWithParams
 * @run main/othervm -Djava.security.debug=certpath LuxTrustCA OCSP
 * @run main/othervm -Djava.security.debug=certpath LuxTrustCA CRL
 */

/*
 * Obtain TLS test artifacts for LuxTrust CAs from:
 *
 * LuxTrust Global Root 2 CA sent test certificates as attachment
 */
public class LuxTrustCA {

    // Owner: CN=LuxTrust Global Qualified CA 3, O=LuxTrust S.A., C=LU
    // Issuer: CN=LuxTrust Global Root 2, O=LuxTrust S.A., C=LU
    // Serial number: 413dea1a28c2253845558e047f3e2a8b5b9baeae
    // Valid from: Fri Mar 06 06:12:15 PST 2015 until: Mon Mar 05 05:21:57 PST 2035
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGcjCCBFqgAwIBAgIUQT3qGijCJThFVY4Efz4qi1ubrq4wDQYJKoZIhvcNAQEL\n" +
            "BQAwRjELMAkGA1UEBhMCTFUxFjAUBgNVBAoMDUx1eFRydXN0IFMuQS4xHzAdBgNV\n" +
            "BAMMFkx1eFRydXN0IEdsb2JhbCBSb290IDIwHhcNMTUwMzA2MTQxMjE1WhcNMzUw\n" +
            "MzA1MTMyMTU3WjBOMQswCQYDVQQGEwJMVTEWMBQGA1UECgwNTHV4VHJ1c3QgUy5B\n" +
            "LjEnMCUGA1UEAwweTHV4VHJ1c3QgR2xvYmFsIFF1YWxpZmllZCBDQSAzMIICIjAN\n" +
            "BgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAuZ5iXSmFbP80gWb0kieYsImcyIo3\n" +
            "QYg+XA3NlwH6QtI0PgZEG9dSo8pM7VMIzE5zq8tgJ50HnPdYflvfhkEKvAW2NuNX\n" +
            "6hi/6HK4Nye+kB+INjpfAHmLft3GT95e+frk/t7hJNorK44xzqfWZKLNGysEHIri\n" +
            "ddcePWOk3J/VMc9CsSemeZbmeZW1/xXeqolMS7JIDZ3+0DgVCYsKIK+b3sAQ8iqX\n" +
            "bQlQyvymG6QyoQoJbuEP23iawRMWKNWk+sjzOkPAAQDtgEEVdggzzudLSM04C5Cj\n" +
            "eLlLYuXgljler9bKRk9wW8nkareLZsn9uCDihGXGyC5m9jseGY1KAnlV8usLjBFA\n" +
            "iW5OCnzcOg+CPsVucoRhS6uvXcu7VtHRGo5yLysJVv7sj6cx5lMvQKAMLviVi3kp\n" +
            "hZKYfqVLAVFJpXTpunY2GayVGf/uOpzNoiSRpcxxYjmAlPKNeTgXVl5Mc0zojgT/\n" +
            "MZTGFN7ov7n01yodN6OhfTADacvaKfj2C2CwdCJvMqvlUuCKrvuXbdZrtRm3BZXr\n" +
            "ghGhuQmG0Tir7VVCI0WZjVjyHs2rpUcCQ6+D1WymKhzp0mrXdaFzYRce7FrEk69J\n" +
            "WzWVp/9/GKnnb0//camavEaI4V64MVxYAir5AL/j7d4JIOqhPPU14ajxmC6dEH84\n" +
            "guVs0Lo/dwVTUzsCAwEAAaOCAU4wggFKMBIGA1UdEwEB/wQIMAYBAf8CAQAwQwYD\n" +
            "VR0gBDwwOjA4BggrgSsBAQEKAzAsMCoGCCsGAQUFBwIBFh5odHRwczovL3JlcG9z\n" +
            "aXRvcnkubHV4dHJ1c3QubHUwagYIKwYBBQUHAQEEXjBcMCsGCCsGAQUFBzABhh9o\n" +
            "dHRwOi8vbHRncm9vdC5vY3NwLmx1eHRydXN0Lmx1MC0GCCsGAQUFBzAChiFodHRw\n" +
            "Oi8vY2EubHV4dHJ1c3QubHUvTFRHUkNBMi5jcnQwDgYDVR0PAQH/BAQDAgEGMB8G\n" +
            "A1UdIwQYMBaAFP8YKHb5SAUsoa7xKxsrslP4S3yzMDMGA1UdHwQsMCowKKAmoCSG\n" +
            "Imh0dHA6Ly9jcmwubHV4dHJ1c3QubHUvTFRHUkNBMi5jcmwwHQYDVR0OBBYEFGOP\n" +
            "wosDsauO2FNHlh2ZqH32rKh1MA0GCSqGSIb3DQEBCwUAA4ICAQADB6M/edbOO9iJ\n" +
            "COnVxayJ1NBk08/BVKlHwe7HBYAzT6Kmo3TbMUwOpcGI2e/NBCR3F4wTzXOVvFmv\n" +
            "dBl7sdS6uMSLBTrav+5LChcFDBQj26X5VQDcXkA8b/u6J4Ve7CwoSesYg9H0fsJ3\n" +
            "v12QrmGUUao9gbamKP1TFriO+XiIaDLYectruusRktIke9qy8MCpNSarZqr3oD3c\n" +
            "/+N5D3lDlGpaz1IL8TpbubFEQHPCr6JiwR+qSqGRfxv8vIvOOAVxe7np5QhtwmCk\n" +
            "XdMOPQ/XOOuEA06bez+zHkASX64at7dXru+4JUEbpijjMA+1jbFZr20OeBIQZL7o\n" +
            "Est+FF8lFuvmucC9TS9QnlF28WJExvpIknjS7LhFMGXB9w380q38ZOuKjPZpoztY\n" +
            "eyUpf8gxzV7fE5Q1okhnsDZ+12vBzBruzJcwtNuXyLyIh3fVN0LunVd+NP2kGjB2\n" +
            "t9WD2Y0CaKxWx8snDdrSbAi46TpNoe04eroWgZOvdN0hEmf2d8tYBSJ/XZekU9sC\n" +
            "Aww5vxHnXJi6CZHhjt8f1mMhyE2gBvmpk4CFetViO2sG0n/nsxCQNpnclsax/eJu\n" +
            "XmGiZ3OPCIRijI5gy3pLRgnbgLyktWoOkmT/gxtWDLfVZwEt52JL8d550KIgttyR\n" +
            "qX81LJWGSDdpnzeRVQEnzAt6+RebAQ==\n" +
            "-----END CERTIFICATE-----";

    // Owner: T=Private Person, SERIALNUMBER=00100978855105608536,
    // GIVENNAME=TokenPRIActive, SURNAME=Test, CN=TokenPRIActive Test, C=DE
    // Issuer: CN=LuxTrust Global Qualified CA 3, O=LuxTrust S.A., C=LU
    // Serial number: 3814b6
    // Valid from: Wed Jul 10 04:36:12 PDT 2019 until: Sun Jul 10 04:36:12 PDT 2022
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIG/jCCBOagAwIBAgIDOBS2MA0GCSqGSIb3DQEBCwUAME4xCzAJBgNVBAYTAkxV\n" +
            "MRYwFAYDVQQKDA1MdXhUcnVzdCBTLkEuMScwJQYDVQQDDB5MdXhUcnVzdCBHbG9i\n" +
            "YWwgUXVhbGlmaWVkIENBIDMwHhcNMTkwNzEwMTEzNjEyWhcNMjIwNzEwMTEzNjEy\n" +
            "WjCBizELMAkGA1UEBhMCREUxHDAaBgNVBAMTE1Rva2VuUFJJQWN0aXZlIFRlc3Qx\n" +
            "DTALBgNVBAQTBFRlc3QxFzAVBgNVBCoTDlRva2VuUFJJQWN0aXZlMR0wGwYDVQQF\n" +
            "ExQwMDEwMDk3ODg1NTEwNTYwODUzNjEXMBUGA1UEDBMOUHJpdmF0ZSBQZXJzb24w\n" +
            "ggGiMA0GCSqGSIb3DQEBAQUAA4IBjwAwggGKAoIBgQDb8l2RJNS7iA9hJFj8aR25\n" +
            "kpU/ZQTHl8Z9yrTLhr4VcMWMxqeOQUcUU27SgIuFvU9s/68OuaIhxyu6eohaGCLC\n" +
            "wzFFRg8OlsUYuI1QtUEliIjmHOMDqSNIt093+SDV64osnHw5fpfy8V0zehEkd7QR\n" +
            "t7Aq38ixCQyxCmNIDJeDCKJT+wwdLaKuw/4SEpR9sygSxZ3kG6kF4icsgYuiOCRx\n" +
            "+DrS1wP9kcrQVWQ0bJbGzwxLZXCHaJsWE1Y17mQAO4Iv/9icqDkP3bZBU5GCgbNT\n" +
            "JEP2GiUUPU3nL41Tlq03+iDmkS2bpWCtFZmTgUg+1nJEb7PSCJ9VcoflOOFgX/ku\n" +
            "TQCJWwhsgyOneEZAg7PpzOj2msxA9RWI88FzRnX/zyjWEpdUCVJ85hFw8u+UZ7k1\n" +
            "eF37oOpgNxQMJ+/ey7huneTzyhpFz/TqJpfMmwaGbPL6zmPLAMQalIPQj+68zlcX\n" +
            "qyeKVbZU74Vm051kXb/3qs6CeUpT4HrY3UmHWLvOdNkCAwEAAaOCAiUwggIhMB8G\n" +
            "A1UdIwQYMBaAFGOPwosDsauO2FNHlh2ZqH32rKh1MGYGCCsGAQUFBwEBBFowWDAn\n" +
            "BggrBgEFBQcwAYYbaHR0cDovL3FjYS5vY3NwLmx1eHRydXN0Lmx1MC0GCCsGAQUF\n" +
            "BzAChiFodHRwOi8vY2EubHV4dHJ1c3QubHUvTFRHUUNBMy5jcnQwggEuBgNVHSAE\n" +
            "ggElMIIBITCCARMGC4g3AQOBKwEBCgMFMIIBAjAqBggrBgEFBQcCARYeaHR0cHM6\n" +
            "Ly9yZXBvc2l0b3J5Lmx1eHRydXN0Lmx1MIHTBggrBgEFBQcCAjCBxgyBw0x1eFRy\n" +
            "dXN0IENlcnRpZmljYXRlIG5vdCBvbiBTU0NEIGNvbXBsaWFudCB3aXRoIEVUU0kg\n" +
            "VFMgMTAyIDA0MiBOQ1AgY2VydGlmaWNhdGUgcG9saWN5LiBLZXkgR2VuZXJhdGlv\n" +
            "biBieSBDU1AuIFNvbGUgQXV0aG9yaXNlZCBVc2FnZTogU2lnbmF0dXJlLCBEYXRh\n" +
            "IG9yIEVudGl0eSBBdXRoZW50aWNhdGlvbiBhbmQgRGF0YSBFbmNyeXB0aW9uLjAI\n" +
            "BgYEAI96AQEwMwYDVR0fBCwwKjAooCagJIYiaHR0cDovL2NybC5sdXh0cnVzdC5s\n" +
            "dS9MVEdRQ0EzLmNybDARBgNVHQ4ECgQISND+8GZyXrcwDgYDVR0PAQH/BAQDAgTw\n" +
            "MAwGA1UdEwEB/wQCMAAwDQYJKoZIhvcNAQELBQADggIBAA54w2kGy+hJsYSyrQ5C\n" +
            "ft0rasUHQviEiy31H2Z1lh4yEPLiuUsaepdzG4bov/J1RewX1fL7fvErraKK7nNr\n" +
            "ioAXNElHtC0wfxGx0xGaCz7xsZIDFgpzyPqS+vd8VKbRCOY66AI+3aPiatCsk+BM\n" +
            "Hp9GwW3B1e5EOgXiWVNxzYFtav5QSAj28IEV7ZuN2BIiU+phawRaoFy+4glMB7zE\n" +
            "J5AM/Zfi50Q85ljy1kWUueFE3VNDafAUGOF5gTHvkKqj6LznUkqcT8m96Wd0IbF2\n" +
            "BLYjnKPF6lGJsivErGqMwQIhlUUMkRQ13/hftL12rIiSjC1C/6cnbxOjWEOGnler\n" +
            "Qn2zu2OTGnnrYxp/hojdZggb5Yt9mkM3EmyuqP1W4g0xtMv9q97swm/fHz/rDh8T\n" +
            "MqrEOJzz284IM0DXjXq1wkmsZ/6/ueCyf0oBN0csvYspZKmLAydZ+jZmjdKKxX+N\n" +
            "dreauHgOq1knLHkMb/YIyA+Oh6SBlNXL4Iae8APQcRGnylHQ1lc/YHTqWh8N1tmn\n" +
            "no5r1kVJBYYtkI3oufaLtP7JIazteZlqTN+tubMJhO4xGgt6bqEpQiid9r3UnIjR\n" +
            "esLYxXS5qRwSoOSleXT98H75+Ok1WR3ciD4exBR8/KcUtDITvDJhkBHnRHm40jFs\n" +
            "5UbHFf98S6G9dqzsqW8+2Bpn\n" +
            "-----END CERTIFICATE-----";

    // Owner: T=Private Person, SERIALNUMBER=00100918135105608625,
    // GIVENNAME=TokenPRIREV, SURNAME=Test, CN=TokenPRIREV Test, C=LU
    // Issuer: CN=LuxTrust Global Qualified CA 3, O=LuxTrust S.A., C=LU
    // Serial number: 3814b8
    // Valid from: Wed Jul 10 04:36:48 PDT 2019 until: Sun Jul 10 04:36:48 PDT 2022
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIG+DCCBOCgAwIBAgIDOBS4MA0GCSqGSIb3DQEBCwUAME4xCzAJBgNVBAYTAkxV\n" +
            "MRYwFAYDVQQKDA1MdXhUcnVzdCBTLkEuMScwJQYDVQQDDB5MdXhUcnVzdCBHbG9i\n" +
            "YWwgUXVhbGlmaWVkIENBIDMwHhcNMTkwNzEwMTEzNjQ4WhcNMjIwNzEwMTEzNjQ4\n" +
            "WjCBhTELMAkGA1UEBhMCTFUxGTAXBgNVBAMTEFRva2VuUFJJUkVWIFRlc3QxDTAL\n" +
            "BgNVBAQTBFRlc3QxFDASBgNVBCoTC1Rva2VuUFJJUkVWMR0wGwYDVQQFExQwMDEw\n" +
            "MDkxODEzNTEwNTYwODYyNTEXMBUGA1UEDBMOUHJpdmF0ZSBQZXJzb24wggGiMA0G\n" +
            "CSqGSIb3DQEBAQUAA4IBjwAwggGKAoIBgQCcm7y4c/D58u6g3m6HGdfiqDXa2yEl\n" +
            "H2cAeSb85fsAX08iXfa/U/kmFqqycwp2nsJdfor6HEEqHsmozyjjIWHDEsq+cUre\n" +
            "SO6d2Ag29MrxsAWZ1XAol40FcxNN+yEL9Xs5doqqcbz3OoKdxkoWVdYq3D7peizF\n" +
            "OER4M2XA0KSLiKXDapDCfTVLE6qRG6Cn5mqnlqbUtkI6vSsda5mWLSNe4Qw/PIMw\n" +
            "v7ZDn5dHeHoV6UpZC95Ole5vMQfjAOsy4nRc1zofQz7iPw4ClNzDQSuonaAKSk3Y\n" +
            "1KjWPmHshb6BoANL+ce1KuWESKV3D5lBkVVLTeoBkWQu7ViJviF2HE5UoPRSGijO\n" +
            "nmGOTZRsjOJXPe7/pEq9SQ477EufnSsoCj1cPCtaowbsO7oswzV/axKMhhZf6nU7\n" +
            "0wd9xUuMgMRKBfi026mYK7pdxJ85qE8qKlqeNprje+g1sjxMDbMHARA427Px0IUJ\n" +
            "mzIJk0ysAQvbqQVe8QQM/f+PH3mUkXR02H8CAwEAAaOCAiUwggIhMB8GA1UdIwQY\n" +
            "MBaAFGOPwosDsauO2FNHlh2ZqH32rKh1MGYGCCsGAQUFBwEBBFowWDAnBggrBgEF\n" +
            "BQcwAYYbaHR0cDovL3FjYS5vY3NwLmx1eHRydXN0Lmx1MC0GCCsGAQUFBzAChiFo\n" +
            "dHRwOi8vY2EubHV4dHJ1c3QubHUvTFRHUUNBMy5jcnQwggEuBgNVHSAEggElMIIB\n" +
            "ITCCARMGC4g3AQOBKwEBCgMFMIIBAjAqBggrBgEFBQcCARYeaHR0cHM6Ly9yZXBv\n" +
            "c2l0b3J5Lmx1eHRydXN0Lmx1MIHTBggrBgEFBQcCAjCBxgyBw0x1eFRydXN0IENl\n" +
            "cnRpZmljYXRlIG5vdCBvbiBTU0NEIGNvbXBsaWFudCB3aXRoIEVUU0kgVFMgMTAy\n" +
            "IDA0MiBOQ1AgY2VydGlmaWNhdGUgcG9saWN5LiBLZXkgR2VuZXJhdGlvbiBieSBD\n" +
            "U1AuIFNvbGUgQXV0aG9yaXNlZCBVc2FnZTogU2lnbmF0dXJlLCBEYXRhIG9yIEVu\n" +
            "dGl0eSBBdXRoZW50aWNhdGlvbiBhbmQgRGF0YSBFbmNyeXB0aW9uLjAIBgYEAI96\n" +
            "AQEwMwYDVR0fBCwwKjAooCagJIYiaHR0cDovL2NybC5sdXh0cnVzdC5sdS9MVEdR\n" +
            "Q0EzLmNybDARBgNVHQ4ECgQIS0KUXpWyku0wDgYDVR0PAQH/BAQDAgTwMAwGA1Ud\n" +
            "EwEB/wQCMAAwDQYJKoZIhvcNAQELBQADggIBAFSnezuyeRO0sh9e8/1N+2RE6Uhb\n" +
            "RIdLKmaS8hMOyUNBapnHfJAdOn7j767qWQjRop5VNCcv0zDOxAqApxFiz4gJdzBY\n" +
            "FVrEVwYos8a3BHLXNxfwIWEJ6EjlqI2qI3NjqK8m4M8LTq4G94V2/MOFVpXeCLju\n" +
            "r0s+XZep2Sk9J4ofUOc8Gp7IZNhPzIlfKQ+KhnWovde4bpL3zRpp4u7Y580XsBuN\n" +
            "kow2Eg84tRzSVizmgLPuRbySHuMo1jGIP7F9FdtOC8VVSjntfCXSEQqOvpH4YZ8S\n" +
            "V4qP17CQHPWW1kOHAyXpkAjU+6SOlmF76Adv9nQFTZ6DAnKqiuxmi8EVCv96aFD7\n" +
            "Ih+zBF7kj7fghPjUzsVdB6gI4VwuFCXEaAfWlxJS67s1hKnsCyqX3cu+Gnq9aRt+\n" +
            "08iaTVEdrKL95AYYobVbnGJ7bH87SpenjLL+CDctXNNDlpJZ8eRYcQe+Q4dg+8L8\n" +
            "X8tkXBeRbiZD1U7XwVBnKF6sJmhA4F/h/EJzwX0lp7EU6EO91bSiwD2NFVs+64UR\n" +
            "9lftfFFm5In2N3vjDR/3nrCf3Jq9f0g7bTrNJmo+hc0+fD+zlAhZAx+ii2xE1cY1\n" +
            "KLH2zXNzPUgIqYGdVQwn1TUFJN8JgGKsXwc+P51nEpgf6JVyK1m7EtVGtr9gF7DI\n" +
            "P+4VSqTbTp4/l5n0\n" +
            "-----END CERTIFICATE-----";

    public static void main(String[] args) throws Exception {

        System.setProperty("jdk.security.certpath.ocspNonce", "true");
        ValidatePathWithParams pathValidator = new ValidatePathWithParams(null);

        if (args.length >= 1 && "CRL".equalsIgnoreCase(args[0])) {
            pathValidator.enableCRLCheck();
        } else {
            // OCSP check by default
            pathValidator.enableOCSPCheck();
        }

        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Wed Jul 10 04:48:49 PDT 2019", System.out);
    }
}
