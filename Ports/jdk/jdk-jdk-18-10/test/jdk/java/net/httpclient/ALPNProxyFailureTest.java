/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary This test will timeout if the ALPN CF is not completed
 *          when a 'Connection reset by peer' exception is raised
 *          during the handshake.
 * @bug 8217094
 * @library /test/lib http2/server
 * @build jdk.test.lib.net.SimpleSSLContext HttpServerAdapters DigestEchoServer
 *        ALPNFailureTest ALPNProxyFailureTest
 * @modules java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          java.logging
 *          java.base/sun.net.www.http
 *          java.base/sun.net.www
 *          java.base/sun.net
 * @build ALPNFailureTest
 * @run main/othervm -Djdk.internal.httpclient.debug=true -Dtest.nolinger=true ALPNProxyFailureTest HTTP_1_1
 * @run main/othervm -Dtest.nolinger=true ALPNProxyFailureTest HTTP_2
 */
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import jdk.test.lib.net.SimpleSSLContext;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.http.HttpClient;

public class ALPNProxyFailureTest extends ALPNFailureTest {

    static final SSLContext context;
    static {
        try {
            context = new SimpleSSLContext().get();
            SSLContext.setDefault(context);
        } catch (Exception x) {
            throw new ExceptionInInitializerError(x);
        }
    }

    public static void main(String[] args) throws Exception{
        if (args == null || args.length == 0) {
            args = new String[] {HttpClient.Version.HTTP_1_1.name()};
        }
        ServerSocket socket = ServerSocketFactory.getDefault()
                .createServerSocket(0, 10, InetAddress.getLoopbackAddress());

        DigestEchoServer.TunnelingProxy proxy = DigestEchoServer.createHttpsProxyTunnel(
                DigestEchoServer.HttpAuthSchemeType.NONE);
        ProxySelector ps = ProxySelector.of(proxy.getProxyAddress());

        try {
            test(socket, context, ps, args);
        } finally {
            proxy.stop();
        }
    }

}
