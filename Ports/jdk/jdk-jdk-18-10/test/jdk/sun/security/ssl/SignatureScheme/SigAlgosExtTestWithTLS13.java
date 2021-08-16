/*
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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
 * @test 8263188
 * @summary If TLS the server and client has no common signature algorithms,
 *     the connection should fail fast with "No supported signature algorithm".
 *     This test only covers TLS 1.3, but doesn't cover client authentication.
 *
 * @library /test/lib
 *          /javax/net/ssl/templates
 *
 * @run main/othervm
 *     -Djdk.tls.server.SignatureSchemes=ecdsa_secp384r1_sha384
 *     -Djdk.tls.client.SignatureSchemes=ecdsa_secp256r1_sha256,ecdsa_secp384r1_sha384
 *     -Dtest.expectFail=false
 *     SigAlgosExtTestWithTLS13
 * @run main/othervm
 *     -Djdk.tls.server.SignatureSchemes=ecdsa_secp384r1_sha384
 *     -Djdk.tls.client.SignatureSchemes=ecdsa_secp256r1_sha256
 *     -Dtest.expectFail=true
 *     SigAlgosExtTestWithTLS13
 */

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;

public class SigAlgosExtTestWithTLS13 extends SSLSocketTemplate {

    @Override
    protected SSLContext createServerSSLContext() throws Exception {
        return createSSLContext(
                new Cert[] { Cert.CA_ECDSA_SECP256R1, Cert.CA_ECDSA_SECP384R1 },
                new Cert[] { Cert.EE_ECDSA_SECP256R1, Cert.EE_ECDSA_SECP384R1 },
                getServerContextParameters());
    }

    @Override
    protected SSLContext createClientSSLContext() throws Exception {
        return createSSLContext(
                new Cert[] { Cert.CA_ECDSA_SECP256R1, Cert.CA_ECDSA_SECP384R1 },
                new Cert[] { Cert.EE_ECDSA_SECP256R1, Cert.EE_ECDSA_SECP384R1 },
                getClientContextParameters());
    }

    @Override
    protected void configureClientSocket(SSLSocket socket) {
        socket.setEnabledProtocols(new String[] { "TLSv1.3" });
    }

    public static void main(String[] args) throws Exception {
        boolean expectFail = Boolean.getBoolean("test.expectFail");
        try {
            new SigAlgosExtTestWithTLS13().run();
            if (expectFail) {
                throw new RuntimeException(
                        "Expected SSLHandshakeException wasn't thrown");
            }
        } catch (SSLHandshakeException e) {
            if (expectFail && e.getMessage().equals(
                    "No supported signature algorithm")) {
                System.out.println("Expected SSLHandshakeException");
            } else {
                throw e;
            }
        }
    }
}
