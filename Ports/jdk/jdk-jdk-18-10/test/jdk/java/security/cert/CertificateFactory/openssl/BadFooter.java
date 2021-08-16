/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6948781
 * @summary CertificateFactory.generateCertificate doesn't throw
 * CertificateException for malformed certificate
 */

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;

public class BadFooter {
    public static void main(String[] args) throws Exception {
        // The two sections below are identical, a self-signed cert generated
        // for a fake principal:
        // CN=Me, OU=Office, O=A-B-C, L=Backside, ST=Moon, C=EA
        String cert =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDGDCCAtWgAwIBAgIERgH/AjALBgcqhkjOOAQDBQAwXTELMAkGA1UEBhMCRUExDTALBgNVBAgT\n" +
        "BE1vb24xETAPBgNVBAcTCEJhY2tzaWRlMQ4wDAYDVQQKEwVBLUItQzEPMA0GA1UECxMGT2ZmaWNl\n" +
        "MQswCQYDVQQDEwJNZTAeFw0wNzAzMjIwMzU4NThaFw0wNzA2MjAwMzU4NThaMF0xCzAJBgNVBAYT\n" +
        "AkVBMQ0wCwYDVQQIEwRNb29uMREwDwYDVQQHEwhCYWNrc2lkZTEOMAwGA1UEChMFQS1CLUMxDzAN\n" +
        "BgNVBAsTBk9mZmljZTELMAkGA1UEAxMCTWUwggG4MIIBLAYHKoZIzjgEATCCAR8CgYEA/X9TgR11\n" +
        "EilS30qcLuzk5/YRt1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZg\n" +
        "t2uZUKWkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/y\n" +
        "IgMZndFIAccCFQCXYFCPFSMLzLKSuYKi64QL8Fgc9QKBgQD34aCF1ps93su8q1w2uFe5eZSvu/o6\n" +
        "6oL5V0wLPQeCZ1FZV4661FlP5nEHEIGAtEkWcSPoTCgWE7fPCTKMyKbhPBZ6i1R8jSjgo64eK7Om\n" +
        "dZFuo38L+iE1YvH7YnoBJDvMpPG+qFGQiaiD3+Fa5Z8GkotmXoB7VSVkAUw7/s9JKgOBhQACgYEA\n" +
        "xc7ovvDeJ5yIkiEoz6U4jcFf5ZDSC+rUEsqGuARXHUF0PlIth7h2e9KV12cwdjVH++mGvwU/m/Ju\n" +
        "OpaaWOEFRHgCMe5fZ2xE0pWPcmKkPicc85SKHguYTMCc9D0XbTbkoBIEAeQ4nr2GmXuEQ5tYaO/O\n" +
        "PYXjk9EfGhikHlnKgC6jITAfMB0GA1UdDgQWBBTtv4rKVwXtXJpyZWlswQL4MAKkazALBgcqhkjO\n" +
        "OAQDBQADMAAwLQIVAIU4pnnUcMjh2CUvh/B0PSZZTHHvAhQVMhAdwNHOGPSL6sCL19q6UjoN9w==\n" +
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDGDCCAtWgAwIBAgIERgH/AjALBgcqhkjOOAQDBQAwXTELMAkGA1UEBhMCRUExDTALBgNVBAgT\n" +
        "BE1vb24xETAPBgNVBAcTCEJhY2tzaWRlMQ4wDAYDVQQKEwVBLUItQzEPMA0GA1UECxMGT2ZmaWNl\n" +
        "MQswCQYDVQQDEwJNZTAeFw0wNzAzMjIwMzU4NThaFw0wNzA2MjAwMzU4NThaMF0xCzAJBgNVBAYT\n" +
        "AkVBMQ0wCwYDVQQIEwRNb29uMREwDwYDVQQHEwhCYWNrc2lkZTEOMAwGA1UEChMFQS1CLUMxDzAN\n" +
        "BgNVBAsTBk9mZmljZTELMAkGA1UEAxMCTWUwggG4MIIBLAYHKoZIzjgEATCCAR8CgYEA/X9TgR11\n" +
        "EilS30qcLuzk5/YRt1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZg\n" +
        "t2uZUKWkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/y\n" +
        "IgMZndFIAccCFQCXYFCPFSMLzLKSuYKi64QL8Fgc9QKBgQD34aCF1ps93su8q1w2uFe5eZSvu/o6\n" +
        "6oL5V0wLPQeCZ1FZV4661FlP5nEHEIGAtEkWcSPoTCgWE7fPCTKMyKbhPBZ6i1R8jSjgo64eK7Om\n" +
        "dZFuo38L+iE1YvH7YnoBJDvMpPG+qFGQiaiD3+Fa5Z8GkotmXoB7VSVkAUw7/s9JKgOBhQACgYEA\n" +
        "xc7ovvDeJ5yIkiEoz6U4jcFf5ZDSC+rUEsqGuARXHUF0PlIth7h2e9KV12cwdjVH++mGvwU/m/Ju\n" +
        "OpaaWOEFRHgCMe5fZ2xE0pWPcmKkPicc85SKHguYTMCc9D0XbTbkoBIEAeQ4nr2GmXuEQ5tYaO/O\n" +
        "PYXjk9EfGhikHlnKgC6jITAfMB0GA1UdDgQWBBTtv4rKVwXtXJpyZWlswQL4MAKkazALBgcqhkjO\n" +
        "OAQDBQADMAAwLQIVAIU4pnnUcMjh2CUvh/B0PSZZTHHvAhQVMhAdwNHOGPSL6sCL19q6UjoN9w==\n" +
        "-----END CERTIFICATE-----\n";
        try {
            CertificateFactory.getInstance("X509").generateCertificates(
                    new ByteArrayInputStream(cert.getBytes()));
            throw new Exception("Fail. certificate generation should fail");
        } catch (CertificateException ce) {
            ce.printStackTrace();
            // This is the correct result
        }
    }
}
