/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/*
 * @test
 * @bug 8074931
 * @summary CertPathEncodingTest tests the ability of the CertPath and
 *          CertificateFactory to encode and decode CertPaths.
 */
public final class CertPathEncodingTest {
    /*
            Certificate:
                Data:
                    Version: 3 (0x2)
                    Serial Number: 935438132 (0x37c1a734)
                Signature Algorithm: dsaWithSHA1
                    Issuer: C=us, O=sun, OU=east, OU=bcn, CN=yassir
                    Validity
                        Not Before: Aug 23 19:55:32 1999 GMT
                        Not After : Aug 22 19:55:32 2000 GMT
                    Subject: C=us, O=sun, OU=east, OU=bcn
                    Subject Public Key Info:
                        Public Key Algorithm: dsaEncryption
                            pub:
                                63:47:4f:f6:29:e5:98:a2:21:fd:da:97:9e:3f:ca:
                                b0:17:49:8d:8a:a7:06:0d:a6:78:97:39:59:33:72:
                                a2:a5:74:d5:3a:ef:e6:7c:07:d7:8e:8e:d1:66:73:
                                99:14:04:96:f5:31:d6:72:ee:d2:53:f8:90:b5:f3:
                                c3:f1:64:ba:1a:9e:c0:0a:da:92:48:c5:d3:84:7e:
                                48:09:66:d9:51:ba:74:56:5a:77:8a:8c:9a:9c:f6:
                                84:12:61:12:51:dc:c6:4f:84:94:ec:cb:78:51:83:
                                8c:20:8a:53:7b:d2:b6:36:df:50:35:95:1f:cb:50:
                                55:8b:3f:fb:e2:77:cb
                            P:
                                00:fd:7f:53:81:1d:75:12:29:52:df:4a:9c:2e:ec:
                                e4:e7:f6:11:b7:52:3c:ef:44:00:c3:1e:3f:80:b6:
                                51:26:69:45:5d:40:22:51:fb:59:3d:8d:58:fa:bf:
                                c5:f5:ba:30:f6:cb:9b:55:6c:d7:81:3b:80:1d:34:
                                6f:f2:66:60:b7:6b:99:50:a5:a4:9f:9f:e8:04:7b:
                                10:22:c2:4f:bb:a9:d7:fe:b7:c6:1b:f8:3b:57:e7:
                                c6:a8:a6:15:0f:04:fb:83:f6:d3:c5:1e:c3:02:35:
                                54:13:5a:16:91:32:f6:75:f3:ae:2b:61:d7:2a:ef:
                                f2:22:03:19:9d:d1:48:01:c7
                            Q:
                                00:97:60:50:8f:15:23:0b:cc:b2:92:b9:82:a2:eb:
                                84:0b:f0:58:1c:f5
                            G:
                                00:f7:e1:a0:85:d6:9b:3d:de:cb:bc:ab:5c:36:b8:
                                57:b9:79:94:af:bb:fa:3a:ea:82:f9:57:4c:0b:3d:
                                07:82:67:51:59:57:8e:ba:d4:59:4f:e6:71:07:10:
                                81:80:b4:49:16:71:23:e8:4c:28:16:13:b7:cf:09:
                                32:8c:c8:a6:e1:3c:16:7a:8b:54:7c:8d:28:e0:a3:
                                ae:1e:2b:b3:a6:75:91:6e:a3:7f:0b:fa:21:35:62:
                                f1:fb:62:7a:01:24:3b:cc:a4:f1:be:a8:51:90:89:
                                a8:83:df:e1:5a:e5:9f:06:92:8b:66:5e:80:7b:55:
                                25:64:01:4c:3b:fe:cf:49:2a
                    X509v3 extensions:
                        X509v3 Key Usage: critical
                            Digital Signature, Key Encipherment, Certificate Sign
                Signature Algorithm: dsaWithSHA1
                     r:
                         52:80:52:2b:2c:3d:02:66:58:b4:dc:ef:52:26:70:
                         1b:53:ca:b3:7d
                     s:
                         62:03:b2:ab:3e:18:2a:66:09:b6:ce:d4:05:a5:8e:
                         a5:7a:0d:55:67
    */
    private static final String cert1 =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIICzTCCAougAwIBAgIEN8GnNDALBgcqhkjOOAQDBQAwSTELMAkGA1UEBhMCdXMx\n" +
            "DDAKBgNVBAoTA3N1bjENMAsGA1UECxMEZWFzdDEMMAoGA1UECxMDYmNuMQ8wDQYD\n" +
            "VQQDEwZ5YXNzaXIwHhcNOTkwODIzMTk1NTMyWhcNMDAwODIyMTk1NTMyWjA4MQsw\n" +
            "CQYDVQQGEwJ1czEMMAoGA1UEChMDc3VuMQ0wCwYDVQQLEwRlYXN0MQwwCgYDVQQL\n" +
            "EwNiY24wggG1MIIBLAYHKoZIzjgEATCCAR8CgYEA/X9TgR11EilS30qcLuzk5/YR\n" +
            "t1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZ\n" +
            "UKWkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOu\n" +
            "K2HXKu/yIgMZndFIAccCFQCXYFCPFSMLzLKSuYKi64QL8Fgc9QKBgQD34aCF1ps9\n" +
            "3su8q1w2uFe5eZSvu/o66oL5V0wLPQeCZ1FZV4661FlP5nEHEIGAtEkWcSPoTCgW\n" +
            "E7fPCTKMyKbhPBZ6i1R8jSjgo64eK7OmdZFuo38L+iE1YvH7YnoBJDvMpPG+qFGQ\n" +
            "iaiD3+Fa5Z8GkotmXoB7VSVkAUw7/s9JKgOBggACf2NHT/Yp5ZiiIf3al54/yrAX\n" +
            "SY2KpwYNpniXOVkzcqKldNU67+Z8B9eOjtFmc5kUBJb1MdZy7tJT+JC188PxZLoa\n" +
            "nsAK2pJIxdOEfkgJZtlRunRWWneKjJqc9oQSYRJR3MZPhJTsy3hRg4wgilN70rY2\n" +
            "31A1lR/LUFWLP/vid8ujEzARMA8GA1UdDwEB/wQFAwMHpAAwCwYHKoZIzjgEAwUA\n" +
            "Ay8AMCwCFFKAUissPQJmWLTc71ImcBtTyrN9AhRiA7KrPhgqZgm2ztQFpY6leg1V\n" +
            "Zw==\n" +
            "-----END CERTIFICATE-----\n" +
            "";

    /*
            Certificate:
                Data:
                    Version: 3 (0x2)
                    Serial Number: 935095671 (0x37bc6d77)
                Signature Algorithm: dsaWithSHA1
                    Issuer: C=us, O=sun, OU=east, OU=bcn, CN=yassir
                    Validity
                        Not Before: Aug 19 20:47:51 1999 GMT
                        Not After : Aug 18 20:47:51 2000 GMT
                    Subject: C=us, O=sun, OU=east, OU=bcn, CN=yassir
                    Subject Public Key Info:
                        Public Key Algorithm: dsaEncryption
                            pub:
                                0a:cc:a4:ec:d6:88:45:c2:24:6b:0d:78:f1:82:f3:
                                5e:3e:31:5d:fb:64:d5:06:5e:39:16:f1:0a:85:d1:
                                ff:d1:a4:74:c5:e6:b0:ba:93:1c:ee:69:51:be:3b:
                                a6:66:44:50:b4:f0:5e:0e:dd:9f:08:71:fe:a1:91:
                                2e:d4:9e:6b:b2:c0:82:3c:91:6c:18:b0:d9:bc:a3:
                                48:91:3f:8b:59:01:61:00:02:ab:22:31:bc:7c:6c:
                                0d:9f:ed:be:33:e6:5c:44:9e:62:30:95:f8:6d:22:
                                d7:e5:85:4c:b0:98:6e:ad:cc:ca:3b:ad:cb:fa:f7:
                                9f:37:13:f7:ca:e2:22:ba
                            P:
                                00:fd:7f:53:81:1d:75:12:29:52:df:4a:9c:2e:ec:
                                e4:e7:f6:11:b7:52:3c:ef:44:00:c3:1e:3f:80:b6:
                                51:26:69:45:5d:40:22:51:fb:59:3d:8d:58:fa:bf:
                                c5:f5:ba:30:f6:cb:9b:55:6c:d7:81:3b:80:1d:34:
                                6f:f2:66:60:b7:6b:99:50:a5:a4:9f:9f:e8:04:7b:
                                10:22:c2:4f:bb:a9:d7:fe:b7:c6:1b:f8:3b:57:e7:
                                c6:a8:a6:15:0f:04:fb:83:f6:d3:c5:1e:c3:02:35:
                                54:13:5a:16:91:32:f6:75:f3:ae:2b:61:d7:2a:ef:
                                f2:22:03:19:9d:d1:48:01:c7
                            Q:
                                00:97:60:50:8f:15:23:0b:cc:b2:92:b9:82:a2:eb:
                                84:0b:f0:58:1c:f5
                            G:
                                00:f7:e1:a0:85:d6:9b:3d:de:cb:bc:ab:5c:36:b8:
                                57:b9:79:94:af:bb:fa:3a:ea:82:f9:57:4c:0b:3d:
                                07:82:67:51:59:57:8e:ba:d4:59:4f:e6:71:07:10:
                                81:80:b4:49:16:71:23:e8:4c:28:16:13:b7:cf:09:
                                32:8c:c8:a6:e1:3c:16:7a:8b:54:7c:8d:28:e0:a3:
                                ae:1e:2b:b3:a6:75:91:6e:a3:7f:0b:fa:21:35:62:
                                f1:fb:62:7a:01:24:3b:cc:a4:f1:be:a8:51:90:89:
                                a8:83:df:e1:5a:e5:9f:06:92:8b:66:5e:80:7b:55:
                                25:64:01:4c:3b:fe:cf:49:2a
                    X509v3 extensions:
                        X509v3 Key Usage: critical
                            Digital Signature, Key Encipherment, Certificate Sign
                        X509v3 Basic Constraints: critical
                            CA:TRUE, pathlen:5
                Signature Algorithm: dsaWithSHA1
                     r:
                         2f:88:46:37:94:92:b2:02:07:5b:8d:76:e5:81:23:
                         85:7f:bc:8d:b9
                     s:
                         00:8b:d7:41:fa:11:c7:ab:27:92:5d:0a:03:98:56:
                         36:42:5f:f5:1f:9d
    */
    private static final String cert2 =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIC9TCCArKgAwIBAgIEN7xtdzALBgcqhkjOOAQDBQAwSTELMAkGA1UEBhMCdXMx\n" +
            "DDAKBgNVBAoTA3N1bjENMAsGA1UECxMEZWFzdDEMMAoGA1UECxMDYmNuMQ8wDQYD\n" +
            "VQQDEwZ5YXNzaXIwHhcNOTkwODE5MjA0NzUxWhcNMDAwODE4MjA0NzUxWjBJMQsw\n" +
            "CQYDVQQGEwJ1czEMMAoGA1UEChMDc3VuMQ0wCwYDVQQLEwRlYXN0MQwwCgYDVQQL\n" +
            "EwNiY24xDzANBgNVBAMTBnlhc3NpcjCCAbcwggEsBgcqhkjOOAQBMIIBHwKBgQD9\n" +
            "f1OBHXUSKVLfSpwu7OTn9hG3UjzvRADDHj+AtlEmaUVdQCJR+1k9jVj6v8X1ujD2\n" +
            "y5tVbNeBO4AdNG/yZmC3a5lQpaSfn+gEexAiwk+7qdf+t8Yb+DtX58aophUPBPuD\n" +
            "9tPFHsMCNVQTWhaRMvZ1864rYdcq7/IiAxmd0UgBxwIVAJdgUI8VIwvMspK5gqLr\n" +
            "hAvwWBz1AoGBAPfhoIXWmz3ey7yrXDa4V7l5lK+7+jrqgvlXTAs9B4JnUVlXjrrU\n" +
            "WU/mcQcQgYC0SRZxI+hMKBYTt88JMozIpuE8FnqLVHyNKOCjrh4rs6Z1kW6jfwv6\n" +
            "ITVi8ftiegEkO8yk8b6oUZCJqIPf4VrlnwaSi2ZegHtVJWQBTDv+z0kqA4GEAAKB\n" +
            "gArMpOzWiEXCJGsNePGC814+MV37ZNUGXjkW8QqF0f/RpHTF5rC6kxzuaVG+O6Zm\n" +
            "RFC08F4O3Z8Icf6hkS7UnmuywII8kWwYsNm8o0iRP4tZAWEAAqsiMbx8bA2f7b4z\n" +
            "5lxEnmIwlfhtItflhUywmG6tzMo7rcv69583E/fK4iK6oycwJTAPBgNVHQ8BAf8E\n" +
            "BQMDB6QAMBIGA1UdEwEB/wQIMAYBAf8CAQUwCwYHKoZIzjgEAwUAAzAAMC0CFC+I\n" +
            "RjeUkrICB1uNduWBI4V/vI25AhUAi9dB+hHHqyeSXQoDmFY2Ql/1H50=\n" +
            "-----END CERTIFICATE-----\n" +
            "";

    private static final String pkcs7path =
            "MIIF9QYJKoZIhvcNAQcCoIIF5jCCBeICAQExADALBgkqhkiG9w0BBwGgggXKMIICzTCCAougAwIB\n" +
            "AgIEN8GnNDALBgcqhkjOOAQDBQAwSTELMAkGA1UEBhMCdXMxDDAKBgNVBAoTA3N1bjENMAsGA1UE\n" +
            "CxMEZWFzdDEMMAoGA1UECxMDYmNuMQ8wDQYDVQQDEwZ5YXNzaXIwHhcNOTkwODIzMTk1NTMyWhcN\n" +
            "MDAwODIyMTk1NTMyWjA4MQswCQYDVQQGEwJ1czEMMAoGA1UEChMDc3VuMQ0wCwYDVQQLEwRlYXN0\n" +
            "MQwwCgYDVQQLEwNiY24wggG1MIIBLAYHKoZIzjgEATCCAR8CgYEA/X9TgR11EilS30qcLuzk5/YR\n" +
            "t1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUKWkn5/oBHsQ\n" +
            "IsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/yIgMZndFIAccCFQCX\n" +
            "YFCPFSMLzLKSuYKi64QL8Fgc9QKBgQD34aCF1ps93su8q1w2uFe5eZSvu/o66oL5V0wLPQeCZ1FZ\n" +
            "V4661FlP5nEHEIGAtEkWcSPoTCgWE7fPCTKMyKbhPBZ6i1R8jSjgo64eK7OmdZFuo38L+iE1YvH7\n" +
            "YnoBJDvMpPG+qFGQiaiD3+Fa5Z8GkotmXoB7VSVkAUw7/s9JKgOBggACf2NHT/Yp5ZiiIf3al54/\n" +
            "yrAXSY2KpwYNpniXOVkzcqKldNU67+Z8B9eOjtFmc5kUBJb1MdZy7tJT+JC188PxZLoansAK2pJI\n" +
            "xdOEfkgJZtlRunRWWneKjJqc9oQSYRJR3MZPhJTsy3hRg4wgilN70rY231A1lR/LUFWLP/vid8uj\n" +
            "EzARMA8GA1UdDwEB/wQFAwMHpAAwCwYHKoZIzjgEAwUAAy8AMCwCFFKAUissPQJmWLTc71ImcBtT\n" +
            "yrN9AhRiA7KrPhgqZgm2ztQFpY6leg1VZzCCAvUwggKyoAMCAQICBDe8bXcwCwYHKoZIzjgEAwUA\n" +
            "MEkxCzAJBgNVBAYTAnVzMQwwCgYDVQQKEwNzdW4xDTALBgNVBAsTBGVhc3QxDDAKBgNVBAsTA2Jj\n" +
            "bjEPMA0GA1UEAxMGeWFzc2lyMB4XDTk5MDgxOTIwNDc1MVoXDTAwMDgxODIwNDc1MVowSTELMAkG\n" +
            "A1UEBhMCdXMxDDAKBgNVBAoTA3N1bjENMAsGA1UECxMEZWFzdDEMMAoGA1UECxMDYmNuMQ8wDQYD\n" +
            "VQQDEwZ5YXNzaXIwggG3MIIBLAYHKoZIzjgEATCCAR8CgYEA/X9TgR11EilS30qcLuzk5/YRt1I8\n" +
            "70QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUKWkn5/oBHsQIsJP\n" +
            "u6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/yIgMZndFIAccCFQCXYFCP\n" +
            "FSMLzLKSuYKi64QL8Fgc9QKBgQD34aCF1ps93su8q1w2uFe5eZSvu/o66oL5V0wLPQeCZ1FZV466\n" +
            "1FlP5nEHEIGAtEkWcSPoTCgWE7fPCTKMyKbhPBZ6i1R8jSjgo64eK7OmdZFuo38L+iE1YvH7YnoB\n" +
            "JDvMpPG+qFGQiaiD3+Fa5Z8GkotmXoB7VSVkAUw7/s9JKgOBhAACgYAKzKTs1ohFwiRrDXjxgvNe\n" +
            "PjFd+2TVBl45FvEKhdH/0aR0xeawupMc7mlRvjumZkRQtPBeDt2fCHH+oZEu1J5rssCCPJFsGLDZ\n" +
            "vKNIkT+LWQFhAAKrIjG8fGwNn+2+M+ZcRJ5iMJX4bSLX5YVMsJhurczKO63L+vefNxP3yuIiuqMn\n" +
            "MCUwDwYDVR0PAQH/BAUDAwekADASBgNVHRMBAf8ECDAGAQH/AgEFMAsGByqGSM44BAMFAAMwADAt\n" +
            "AhQviEY3lJKyAgdbjXblgSOFf7yNuQIVAIvXQfoRx6snkl0KA5hWNkJf9R+dMQA=\n" +
            "";

    // Runs test of CertPath encoding and decoding.
    public static void main(String[] args) throws Exception {
        // Make the CertPath whose encoded form has already been stored
        CertificateFactory certFac = CertificateFactory.getInstance("X509");

        final List<Certificate> certs = new ArrayList<>();
        certs.add(certFac.generateCertificate(new ByteArrayInputStream(cert1.getBytes())));
        certs.add(certFac.generateCertificate(new ByteArrayInputStream(cert2.getBytes())));

        CertPath cp = certFac.generateCertPath(certs);

        // Get the encoded form of the CertPath we made
        byte[] encoded = cp.getEncoded("PKCS7");

        // check if it matches the encoded value
        if (!Arrays.equals(encoded, Base64.getMimeDecoder().decode(pkcs7path.getBytes()))) {
            throw new RuntimeException("PKCS#7 encoding doesn't match stored value");
        }

        // Generate a CertPath from the encoded value and check if it equals
        // the CertPath generated from the certificates
        CertPath decodedCP = certFac.generateCertPath(new ByteArrayInputStream(encoded), "PKCS7");
        if (!decodedCP.equals(cp)) {
            throw new RuntimeException("CertPath decoded from PKCS#7 isn't equal to original");
        }
    }
}
