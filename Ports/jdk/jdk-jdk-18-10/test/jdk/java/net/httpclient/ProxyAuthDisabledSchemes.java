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

/**
 * @test
 * @summary this test verifies that a client may provides authorization
 *          headers directly when connecting with a server, and
 *          it verifies that the client honor the jdk.http.auth.*.disabledSchemes
 *          net properties.
 * @bug 8087112
 * @library /test/lib http2/server
 * @build jdk.test.lib.net.SimpleSSLContext DigestEchoServer DigestEchoClient
 *        ReferenceTracker ProxyAuthDisabledSchemes
 * @modules java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 *          java.base/sun.net.www.http
 *          java.base/sun.net.www
 *          java.base/sun.net
 * @run main/othervm -Djdk.http.auth.proxying.disabledSchemes=Basic,Digest
 *                   -Djdk.http.auth.tunneling.disabledSchemes=Digest,Basic
 *                   ProxyAuthDisabledSchemes
 * @run main/othervm -Djdk.http.auth.proxying.disabledSchemes=Basic
 *                   -Djdk.http.auth.tunneling.disabledSchemes=Basic
 *                   ProxyAuthDisabledSchemes CLEAR PROXY
 * @run main/othervm -Djdk.http.auth.proxying.disabledSchemes=Digest
 *                   -Djdk.http.auth.tunneling.disabledSchemes=Digest
 *                   ProxyAuthDisabledSchemes CLEAR PROXY
 */

public class ProxyAuthDisabledSchemes {
    public static void main(String[] args) throws Exception {
        DigestEchoClient.main(args);
    }
}
