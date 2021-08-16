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

/*
 * @test
 * @bug 8212261
 * @summary Add SSLSession accessors to HttpsURLConnection and
 *          SecureCacheResponse
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.SecureCacheResponse;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

public class DefaultCacheResponse extends SecureCacheResponse {

    public static void main(String[] args) throws Exception {
        DefaultCacheResponse defaultImpl = new DefaultCacheResponse();

        Optional<SSLSession> sslSession = defaultImpl.getSSLSession();
        if (sslSession.isPresent()) {
            throw new Exception(
                "The default SecureCacheResponse.getSSLSession " +
                "implementation should return an empty Optional");
        }
    }

    @Override
    public String getCipherSuite() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Certificate> getLocalCertificateChain() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Certificate> getServerCertificateChain()
            throws SSLPeerUnverifiedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Principal getLocalPrincipal() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, List<String>> getHeaders() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public InputStream getBody() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
