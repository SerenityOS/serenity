/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8189131
 * @summary Interoperability tests with Actalis CA
 * @build ValidatePathWithParams
 * @run main/othervm/timeout=180 -Djava.security.debug=certpath ActalisCA OCSP
 * @run main/othervm/timeout=180 -Djava.security.debug=certpath ActalisCA CRL
 */

 /*
 * Obtain test artifacts for Actalis CA from:
 *
 * Test web site with *active *TLS Server certificate:
 * https://ssltest-a.actalis.it:8443
 * If doesn't work then use certificate of https://www.actalis.it
 *
 * Test web site with *revoked *TLS Server certificate:
 * https://ssltest-r.actalis.it:8444
 *
 * Test web site with *expired *TLS Server certificate:
 * https://ssltest-e.actalis.it:8445
 */
public class ActalisCA {

    // Owner: CN=Actalis Extended Validation Server CA G1,
    // O=Actalis S.p.A./03358520967, L=Milano, ST=Milano, C=IT
    // Issuer: CN=Actalis Authentication Root CA, O=Actalis S.p.A./03358520967,
    // L=Milan, C=IT
    private static final String INT_VALID = "-----BEGIN CERTIFICATE-----\n"
            + "MIIGTDCCBDSgAwIBAgIIMtYr/GdQGsswDQYJKoZIhvcNAQELBQAwazELMAkGA1UE\n"
            + "BhMCSVQxDjAMBgNVBAcMBU1pbGFuMSMwIQYDVQQKDBpBY3RhbGlzIFMucC5BLi8w\n"
            + "MzM1ODUyMDk2NzEnMCUGA1UEAwweQWN0YWxpcyBBdXRoZW50aWNhdGlvbiBSb290\n"
            + "IENBMB4XDTE1MDUxNDA3MDAzOFoXDTMwMDUxNDA3MDAzOFowgYcxCzAJBgNVBAYT\n"
            + "AklUMQ8wDQYDVQQIDAZNaWxhbm8xDzANBgNVBAcMBk1pbGFubzEjMCEGA1UECgwa\n"
            + "QWN0YWxpcyBTLnAuQS4vMDMzNTg1MjA5NjcxMTAvBgNVBAMMKEFjdGFsaXMgRXh0\n"
            + "ZW5kZWQgVmFsaWRhdGlvbiBTZXJ2ZXIgQ0EgRzEwggEiMA0GCSqGSIb3DQEBAQUA\n"
            + "A4IBDwAwggEKAoIBAQD1Ygc1CwmqXqjd3dTEKMLUwGdb/3+00ytg0uBb4RB+89/O\n"
            + "4K/STFZcGUjcCq6Job5cmxZBGyRRBYfCEn4vg8onedFztkO0NvD04z4wLFyxjSRT\n"
            + "bcMm2d+/Xci5XLA3Q9wG8TGzHTVQKmdvFpQ7b7EsmOc0uXA7w3UGhLjb2EYpu/Id\n"
            + "uZ1LUTyEOHc3XHXI3a3udkRBDs/bObTcbte80DPbNetRFB+jHbIw5sH171IeBFGN\n"
            + "PB92Iebp01yE8g3X9RqPXrrV7ririEtwFMYp+KgA8BRHxsoNV3xZmhdzJm0AMzC2\n"
            + "waLM3H562xPM0UntAYh2pRrAUUtgURRizCT1kr6tAgMBAAGjggHVMIIB0TBBBggr\n"
            + "BgEFBQcBAQQ1MDMwMQYIKwYBBQUHMAGGJWh0dHA6Ly9vY3NwMDUuYWN0YWxpcy5p\n"
            + "dC9WQS9BVVRILVJPT1QwHQYDVR0OBBYEFGHB5IYeTW10dLzZlzsxcXjLP5/cMA8G\n"
            + "A1UdEwEB/wQFMAMBAf8wHwYDVR0jBBgwFoAUUtiIOsifeGbtifN7OHCUyQICNtAw\n"
            + "RQYDVR0gBD4wPDA6BgRVHSAAMDIwMAYIKwYBBQUHAgEWJGh0dHBzOi8vd3d3LmFj\n"
            + "dGFsaXMuaXQvYXJlYS1kb3dubG9hZDCB4wYDVR0fBIHbMIHYMIGWoIGToIGQhoGN\n"
            + "bGRhcDovL2xkYXAwNS5hY3RhbGlzLml0L2NuJTNkQWN0YWxpcyUyMEF1dGhlbnRp\n"
            + "Y2F0aW9uJTIwUm9vdCUyMENBLG8lM2RBY3RhbGlzJTIwUy5wLkEuJTJmMDMzNTg1\n"
            + "MjA5NjcsYyUzZElUP2NlcnRpZmljYXRlUmV2b2NhdGlvbkxpc3Q7YmluYXJ5MD2g\n"
            + "O6A5hjdodHRwOi8vY3JsMDUuYWN0YWxpcy5pdC9SZXBvc2l0b3J5L0FVVEgtUk9P\n"
            + "VC9nZXRMYXN0Q1JMMA4GA1UdDwEB/wQEAwIBBjANBgkqhkiG9w0BAQsFAAOCAgEA\n"
            + "OD8D2Z2fw76+GIu+mDEgygH/y7F9K4I6rZOc3LqGBecO3C0fGcIuuG7APtxGGk7Y\n"
            + "nk97Qt+3pDoek9EP65/1u128pRncZcjEAeMgKb7UuJxwoR6Sj5zhOadotKcCQqmF\n"
            + "Si99ExNo6dTq5Eyp1KrqepLmezbO9owx4Q44mtNpfKLMgzDqOn/dwNMo/pGYbMfP\n"
            + "DjhxEnta1HXgcEcgCk1Au16xkdzapwY4sXpKuwB24phfWF+cveKAQ0Rncmvrm34i\n"
            + "9B6leZUkSHDe4mRkbO5nObhKHYRmVSr0Q/wvGCmTgGTKuw/Gj8+RFb5MEkOKEcJn\n"
            + "I32CPohpiW/jlpeLaFBIgJnXuZTxmfTX55sqtXDlKxRxFwq1W3kML4UfGZsgjx1l\n"
            + "hX5fQ1QlEZeO9CyPpgGO5Py2KXXKhUxCtF7tawAYimWwslxvPCjHDND/WhM1Fz9e\n"
            + "2yqwHcSQAOUVv5mk9uYc6/NSLwLb5in3R728GNEpHHhbx5QZhtdqR8mb56uJUDKI\n"
            + "AwnnZckcR+SLGL2Agx7hY7YCMOQhSsO6PA81M/mGW2hGCiZw3GULJe9ejL/vdS0I\n"
            + "PWrp7YLnXUa6mtXVSBKGrVrlbpJaN10+fB4Yrlk4O2sF4WNUAHMBn9T+zOXaBAhj\n"
            + "vNlMU7+elLkTcKIB7qJJuSZChxzoevM2ciO3BpGuRxg=\n"
            + "-----END CERTIFICATE-----";

    // Owner: OID.1.3.6.1.4.1.311.60.2.1.3=IT, STREET=Via S. Clemente 53,
    // OID.2.5.4.15=Private Organization, CN=www.actalis.it,
    // SERIALNUMBER=03358520967, O=Actalis S.p.A., L=Ponte San Pietro, ST=Bergamo, C=IT
    // Issuer: CN=Actalis Extended Validation Server CA G1,
    // O=Actalis S.p.A./03358520967, L=Milano, ST=Milano, C=IT
    // Serial number: eeeee6d6463bde2
    // Valid from: Sat Jun 17 05:59:17 PDT 2017 until: Mon Jun 17 05:59:17 PDT 2019
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n"
            + "MIIHwTCCBqmgAwIBAgIIDu7ubWRjveIwDQYJKoZIhvcNAQELBQAwgYcxCzAJBgNV\n"
            + "BAYTAklUMQ8wDQYDVQQIDAZNaWxhbm8xDzANBgNVBAcMBk1pbGFubzEjMCEGA1UE\n"
            + "CgwaQWN0YWxpcyBTLnAuQS4vMDMzNTg1MjA5NjcxMTAvBgNVBAMMKEFjdGFsaXMg\n"
            + "RXh0ZW5kZWQgVmFsaWRhdGlvbiBTZXJ2ZXIgQ0EgRzEwHhcNMTcwNjE3MTI1OTE3\n"
            + "WhcNMTkwNjE3MTI1OTE3WjCB0zELMAkGA1UEBhMCSVQxEDAOBgNVBAgMB0Jlcmdh\n"
            + "bW8xGTAXBgNVBAcMEFBvbnRlIFNhbiBQaWV0cm8xFzAVBgNVBAoMDkFjdGFsaXMg\n"
            + "Uy5wLkEuMRQwEgYDVQQFEwswMzM1ODUyMDk2NzEXMBUGA1UEAwwOd3d3LmFjdGFs\n"
            + "aXMuaXQxHTAbBgNVBA8MFFByaXZhdGUgT3JnYW5pemF0aW9uMRswGQYDVQQJDBJW\n"
            + "aWEgUy4gQ2xlbWVudGUgNTMxEzARBgsrBgEEAYI3PAIBAxMCSVQwggEiMA0GCSqG\n"
            + "SIb3DQEBAQUAA4IBDwAwggEKAoIBAQCwZ3++4pQYGfhXSqin1CKRJ6SOqkTcX3O0\n"
            + "6b4jZbSNomyqyn6aHOz6ztOlj++fPzxmIzErEySOTd3G0pr+iwpYQVdeg1Y27KL8\n"
            + "OiwwUrlV4ZMa8KKXr4BnWlDbFIo+eIcSew5V7CiodDyxpj9zjqJK497LF1jxgXtr\n"
            + "IoMRwrh2Y0NbJCZGUCL30sQr/W4xBnO1+pi2DbCieGe/XoK8yEtx9FdnEFvyT9qn\n"
            + "zYyrXvnTvfVSwzwtEIn+akjomI4WfCFLBF0M7v4dAHypfnPAAoW1c0BBqNB32zf0\n"
            + "rYwNnD7UwZlcDihEYlgC70Dfy7bPsdq2spmOMk/VUqb3U0LHRVM3AgMBAAGjggPh\n"
            + "MIID3TB9BggrBgEFBQcBAQRxMG8wOgYIKwYBBQUHMAKGLmh0dHA6Ly9jYWNlcnQu\n"
            + "YWN0YWxpcy5pdC9jZXJ0cy9hY3RhbGlzLWF1dGV2ZzEwMQYIKwYBBQUHMAGGJWh0\n"
            + "dHA6Ly9vY3NwMDUuYWN0YWxpcy5pdC9WQS9BVVRIRVYtRzEwHQYDVR0OBBYEFK9y\n"
            + "954QoY/5XV6TayD1gWVy0gQOMAwGA1UdEwEB/wQCMAAwHwYDVR0jBBgwFoAUYcHk\n"
            + "hh5NbXR0vNmXOzFxeMs/n9wwUAYDVR0gBEkwRzA8BgYrgR8BEQEwMjAwBggrBgEF\n"
            + "BQcCARYkaHR0cHM6Ly93d3cuYWN0YWxpcy5pdC9hcmVhLWRvd25sb2FkMAcGBWeB\n"
            + "DAEBMIHvBgNVHR8EgecwgeQwgaKggZ+ggZyGgZlsZGFwOi8vbGRhcDA1LmFjdGFs\n"
            + "aXMuaXQvY24lM2RBY3RhbGlzJTIwRXh0ZW5kZWQlMjBWYWxpZGF0aW9uJTIwU2Vy\n"
            + "dmVyJTIwQ0ElMjBHMSxvJTNkQWN0YWxpcyUyMFMucC5BLi8wMzM1ODUyMDk2Nyxj\n"
            + "JTNkSVQ/Y2VydGlmaWNhdGVSZXZvY2F0aW9uTGlzdDtiaW5hcnkwPaA7oDmGN2h0\n"
            + "dHA6Ly9jcmwwNS5hY3RhbGlzLml0L1JlcG9zaXRvcnkvQVVUSEVWLUcxL2dldExh\n"
            + "c3RDUkwwDgYDVR0PAQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEF\n"
            + "BQcDAjAZBgNVHREEEjAQgg53d3cuYWN0YWxpcy5pdDCCAX4GCisGAQQB1nkCBAIE\n"
            + "ggFuBIIBagFoAHYApLkJkLQYWBSHuxOizGdwCjw1mAT5G9+443fNDsgN3BAAAAFc\n"
            + "tiwHywAABAMARzBFAiEA7GC5/kja3l8cBw1/wBpHl/AKH6eL1MKpmICtf5G09c4C\n"
            + "IBM887DQEwD2E4Xx/IP+33NMvUOhSwZ4XODgqFVXsz0wAHYA7ku9t3XOYLrhQmkf\n"
            + "q+GeZqMPfl+wctiDAMR7iXqo/csAAAFctiwIqwAABAMARzBFAiEAwwiR95ozXdKs\n"
            + "+uULfrzgENbHc2rLgGIac6ZMv0xHDLACIFLQVpvQBRQfys2KVRGHQKGxqAeghQZw\n"
            + "9nJL+U5huzfaAHYA3esdK3oNT6Ygi4GtgWhwfi6OnQHVXIiNPRHEzbbsvswAAAFc\n"
            + "tiwMqwAABAMARzBFAiEAifV9ocxbO6b3I22jb2zxBvG2e83hXHitOhYXkHdSmZkC\n"
            + "IDJLuPvGOczF9axgphImlUbT9dX3wRpjEi5IeV+pxMiYMA0GCSqGSIb3DQEBCwUA\n"
            + "A4IBAQB5U6k1Onv9Y7POHGnUOI0ATHevbpbS/7r68DZQ6cRmDIpsZyjW6PxYs9nc\n"
            + "3ob3Pjomm+S7StDl9ehI7rYLlZC52QlXlsq1fzEQ9xSkf+VSD70A91dPIFAdI/jQ\n"
            + "aWvIUvQEbhfUZc0ihIple0VyWGH5bza0DLW+C8ttF8KqICUfL8S8mZgjbXvVg2fY\n"
            + "HLW9lWR/Pkco2yRc8gZyr9FGkXOcmJ8aFaCuJnGm/IVRCieYp60If4DoAKz49xpF\n"
            + "CF6RjOAJ//UGSp/ySjHMmT8PLO7NvhsT4XDDGTSeIYYpO++tbEIcLcjW9m2k5Gnh\n"
            + "kmEenr0hdcpeLgsP3Fsy7JxyQNpL\n"
            + "-----END CERTIFICATE-----";

    // Owner:  CN=Actalis Authentication CA G3, O=Actalis S.p.A./03358520967, L=Milano, ST=Milano, C=IT
    // Issuer: CN=Actalis Authentication Root CA, O=Actalis S.p.A./03358520967, L=Milan, C=IT
    // SN:     741d584a 72fc06bc
    // Valid from: Wed Feb 12 22:32:23 PST 2014
    // Valid till: Mon Feb 12 22:32:23 PST 2024
    private static final String INT_REVOKED = "-----BEGIN CERTIFICATE-----\n"
            + "MIIGTTCCBDWgAwIBAgIIdB1YSnL8BrwwDQYJKoZIhvcNAQELBQAwazELMAkGA1UE\n"
            + "BhMCSVQxDjAMBgNVBAcMBU1pbGFuMSMwIQYDVQQKDBpBY3RhbGlzIFMucC5BLi8w\n"
            + "MzM1ODUyMDk2NzEnMCUGA1UEAwweQWN0YWxpcyBBdXRoZW50aWNhdGlvbiBSb290\n"
            + "IENBMB4XDTE0MDIxMzE1MDIyM1oXDTI0MDIxMzE1MDIyM1owezELMAkGA1UEBhMC\n"
            + "SVQxDzANBgNVBAgMBk1pbGFubzEPMA0GA1UEBwwGTWlsYW5vMSMwIQYDVQQKDBpB\n"
            + "Y3RhbGlzIFMucC5BLi8wMzM1ODUyMDk2NzElMCMGA1UEAwwcQWN0YWxpcyBBdXRo\n"
            + "ZW50aWNhdGlvbiBDQSBHMzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n"
            + "AMzhDjmhNDym6ze3PegbIKmiavXpAjgVCZ344k1DOtdSCV6k3h3rqfHqFn3mrayA\n"
            + "btmJ0NeC886WxUUsJwHJ3bOnNBQZIHxLV+1RVD/6TQqb6/bPJu4rDwEfhbJSmErc\n"
            + "29wUJWqxXMhSAWTHi3Pq0vrkx59e5KTEyfB2kHo6InlR72sCCRdtCL9aDuDm8nYK\n"
            + "pTSAJr36ultwME5NyCNSyN2JIK0wYbEi7MVNbp5KN9MusTp3cOMDoVBreYulmnEu\n"
            + "TNazmoAv0K8oLS7iX7c9x+zGjUUAucFEuSlRn3sL6hFAiKjy4PDClvnyqQHBBdZr\n"
            + "/3JOxAcgXv7aZ4/STeXeDXsCAwEAAaOCAeMwggHfMEEGCCsGAQUFBwEBBDUwMzAx\n"
            + "BggrBgEFBQcwAYYlaHR0cDovL3BvcnRhbC5hY3RhbGlzLml0L1ZBL0FVVEgtUk9P\n"
            + "VDAdBgNVHQ4EFgQUqqr9yowdTfEug+EG/PqO6g4jrj0wDwYDVR0TAQH/BAUwAwEB\n"
            + "/zAfBgNVHSMEGDAWgBRS2Ig6yJ94Zu2J83s4cJTJAgI20DBUBgNVHSAETTBLMEkG\n"
            + "BFUdIAAwQTA/BggrBgEFBQcCARYzaHR0cHM6Ly9wb3J0YWwuYWN0YWxpcy5pdC9S\n"
            + "ZXBvc2l0b3J5L1BvbGljeS9TU0wvQ1BTMIHiBgNVHR8EgdowgdcwgZSggZGggY6G\n"
            + "gYtsZGFwOi8vbGRhcC5hY3RhbGlzLml0L2NuJTNkQWN0YWxpcyUyMEF1dGhlbnRp\n"
            + "Y2F0aW9uJTIwUm9vdCUyMENBLG8lM2RBY3RhbGlzJTIwUy5wLkEuJTJmMDMzNTg1\n"
            + "MjA5NjcsYyUzZElUP2NlcnRpZmljYXRlUmV2b2NhdGlvbkxpc3Q7YmluYXJ5MD6g\n"
            + "PKA6hjhodHRwOi8vcG9ydGFsLmFjdGFsaXMuaXQvUmVwb3NpdG9yeS9BVVRILVJP\n"
            + "T1QvZ2V0TGFzdENSTDAOBgNVHQ8BAf8EBAMCAQYwDQYJKoZIhvcNAQELBQADggIB\n"
            + "ABP93l+9QBgzHF0Clf3gMAelGqwXT25DwZVFIkBw6YyqOPcaqzw1XKHJJEMQ8xOp\n"
            + "8uuiPLP/ObxEXBBvH7ofNW7nRUIzGsuLPhzdfJhdzilCVAvz4WRsX44nWOQS4Qu0\n"
            + "npo7dbq/KxFUCUO9yNEJp6YxNloy8XFIlazkHFTKGJqoUpsGoc7B9YmPchhE2FPb\n"
            + "OZiOCg4Y2Qp43UJfnENgZ3gJFh16juQE1uS8Q/JJI7ZzJfJ/W0uQoDnCprOPUpLF\n"
            + "G03e0asFxwQqhL84Jvf7rJZaWvwydHP4hH47nzpHWEGXwfJLXXoO7LHgqVB7K9Ar\n"
            + "Zf3pY0S/3Fs+AN/PrEY3Z3rb7ypQLRiot1oJLl8matiGEF4aFL5DDkr9wfRAZ8S8\n"
            + "WT69vN68ENGgEwyeZSlQxn+4g6quHRav0fmF2fGnLaq7tteSPVocT7XaMEpkHqNs\n"
            + "x1q/PJbr39s/1QVZtS9CrdoCr0QAnBaX//PPB6ansSLFcvEqM9QcV9xQZex88ToX\n"
            + "nk3TcHtA0ezWJlCkg626MhdQZrhHbkauHfIGSOmCkn3zHp0BZQ6Vo7UOdRMT7QS7\n"
            + "y7AkET9Qmapwh2CFUdCJSXklVRd+06XhhOB37NQU0pGJQJ3xjEPrILZ8kLhW3Tyq\n"
            + "Iv30LW7MXZ4yQn/JHEZbuiOOb4R45hsPZxe6gOq/e+sf\n"
            + "-----END CERTIFICATE-----";

    // Owner:  CN=ssltest-r.actalis.it, O=Actalis S.p.A., L=Ponte San Pietro, ST=Bergamo, C=IT
    // Issuer: CN=Actalis Authentication CA G3, O=Actalis S.p.A./03358520967, L=Milano, ST=Milano, C=IT
    // SN:     0455de97 5c71c96f
    // Valid from: Thu Jan 28 16:23:52 PST 2016
    // Valid till: Mon Jan 28 16:23:52 PST 2019
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n"
            + "MIIFmDCCBICgAwIBAgIIBFXel1xxyW8wDQYJKoZIhvcNAQELBQAwezELMAkGA1UE\n"
            + "BhMCSVQxDzANBgNVBAgMBk1pbGFubzEPMA0GA1UEBwwGTWlsYW5vMSMwIQYDVQQK\n"
            + "DBpBY3RhbGlzIFMucC5BLi8wMzM1ODUyMDk2NzElMCMGA1UEAwwcQWN0YWxpcyBB\n"
            + "dXRoZW50aWNhdGlvbiBDQSBHMzAeFw0xNjAxMjkwODUzNTJaFw0xOTAxMjkwODUz\n"
            + "NTJaMHIxCzAJBgNVBAYTAklUMRAwDgYDVQQIDAdCZXJnYW1vMRkwFwYDVQQHDBBQ\n"
            + "b250ZSBTYW4gUGlldHJvMRcwFQYDVQQKDA5BY3RhbGlzIFMucC5BLjEdMBsGA1UE\n"
            + "AwwUc3NsdGVzdC1yLmFjdGFsaXMuaXQwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw\n"
            + "ggEKAoIBAQClbzoXCvD21FD7Oy/TKZu4fmDFJrISrNfasLlC3krLHkgb1vg23Z1P\n"
            + "+7rIymDgrJSzjvYmisl+VM7xXxTsyI2pp9Qp/uzTMAMML9ISd/s0LaMBiNN5iPyj\n"
            + "W91gGzGe30Jc319afKwFBaveSv7NO3DWsmHw9koezWkKUug2dnQCVXk1uTSdobnq\n"
            + "wOgwxdd86LpZnFLxBIYdU68S4vogAQZjdja/S1+tF6JnfvY6o/xRJmQckVtNmUs6\n"
            + "Dj3KoN2o/8BEgSCYcJz8tfoZcVazVkWOp/u6moUnm1/IKSYNgtHnB1ub0fB2AttW\n"
            + "Vi7cs3SG/tDMMP8yc1kWScWf8CYj/AI1AgMBAAGjggInMIICIzA/BggrBgEFBQcB\n"
            + "AQQzMDEwLwYIKwYBBQUHMAGGI2h0dHA6Ly9vY3NwMDMuYWN0YWxpcy5pdC9WQS9B\n"
            + "VVRILUczMB0GA1UdDgQWBBRIKN5WmrjivlnT1rDzsH1WZ+PuvTAMBgNVHRMBAf8E\n"
            + "AjAAMB8GA1UdIwQYMBaAFKqq/cqMHU3xLoPhBvz6juoOI649MGAGA1UdIARZMFcw\n"
            + "SwYGK4EfARQBMEEwPwYIKwYBBQUHAgEWM2h0dHBzOi8vcG9ydGFsLmFjdGFsaXMu\n"
            + "aXQvUmVwb3NpdG9yeS9Qb2xpY3kvU1NML0NQUzAIBgZngQwBAgIwgd8GA1UdHwSB\n"
            + "1zCB1DCBlKCBkaCBjoaBi2xkYXA6Ly9sZGFwMDMuYWN0YWxpcy5pdC9jbiUzZEFj\n"
            + "dGFsaXMlMjBBdXRoZW50aWNhdGlvbiUyMENBJTIwRzMsbyUzZEFjdGFsaXMlMjBT\n"
            + "LnAuQS4lMmYwMzM1ODUyMDk2NyxjJTNkSVQ/Y2VydGlmaWNhdGVSZXZvY2F0aW9u\n"
            + "TGlzdDtiaW5hcnkwO6A5oDeGNWh0dHA6Ly9jcmwwMy5hY3RhbGlzLml0L1JlcG9z\n"
            + "aXRvcnkvQVVUSC1HMy9nZXRMYXN0Q1JMMA4GA1UdDwEB/wQEAwIFoDAdBgNVHSUE\n"
            + "FjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwHwYDVR0RBBgwFoIUc3NsdGVzdC1yLmFj\n"
            + "dGFsaXMuaXQwDQYJKoZIhvcNAQELBQADggEBAHZLND53/CZoMlDtfln0ZByCEhoF\n"
            + "/XtA9cYy2azRGgS/VY4WUccvg99MM50cwn5GPRsJpoaFXeDrjV3DkOUK1jERzjx4\n"
            + "5y83K/AkCGe7uU17aS+tweETizBAfHNj78oHmZDmkDSEY2STaeuHNDJ9ft0v3QTb\n"
            + "VW54R5W3OBU7L/sJoEUdRxzGN7vO82PboGvyApMCWDRLKE7bPP4genQtF3XPcaFl\n"
            + "ekuSiEVYS+KnM2v9tCWHqw6x7raWHFB9w1kAKNwv0hbEJkeC+a2bCdPwv8hs//sa\n"
            + "gUF4p61mIpf+5qmQ6gcZOClPWyrbYdQdfCvKgbEdKhwB0v5KS0NIRRn41SE=\n"
            + "-----END CERTIFICATE-----";

    public static void main(String[] args) throws Exception {

        ValidatePathWithParams pathValidator = new ValidatePathWithParams(null);
        boolean ocspEnabled = false;

        if (args.length >= 1 && "CRL".equalsIgnoreCase(args[0])) {
            pathValidator.enableCRLCheck();
        } else {
            // OCSP check by default
            pathValidator.enableOCSPCheck();
            ocspEnabled = true;
        }

        // Validate valid
        pathValidator.validate(new String[]{VALID, INT_VALID},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Revoked certificate is using SHA1 signature
        if (ocspEnabled) {
            // Revoked test certificate is expired
            // and backdated revocation check is only possible with OCSP
            pathValidator.setValidationDate("July 01, 2016");
        }

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT_REVOKED},
                ValidatePathWithParams.Status.REVOKED,
                "Fri Jan 29 01:06:42 PST 2016", System.out);

        // reset validation date back to current date
        pathValidator.resetValidationDate();
    }
}
