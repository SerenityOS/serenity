/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6737819
 * @run main/othervm B6737819
 * @summary sun.misc.net.DefaultProxySelector doesn't use proxy setting to localhost
 */

/* Run in othervm mode since the test sets HTTP proxy system properties that
 * are read once and cached by the protocol handler. A previous test using the
 * HTTP handler may run and these system properties may be ignored for this test.
 */

import java.net.ProxySelector;
import java.net.Proxy;
import java.net.URI;

public class B6737819 {
    private static String[] uris = {
        "http://localhost/index.html",
        "http://127.0.0.1/index.html",
        "http://127.2/index.html",
        "http://[::1]/index.html"
    };
    public static void main(String[] args) throws Exception {
        System.setProperty("http.proxyHost", "myproxy");
        System.setProperty("http.proxyPort", "8080");
        ProxySelector sel = ProxySelector.getDefault();
        java.util.List<Proxy> l;
        // Default value for http.nonProxyHots should exclude all this uris
        // from going through the HTTP proxy
        for (String s : uris) {
            l = sel.select(new URI(s));
            if (l.size() == 1 && l.get(0).type() != Proxy.Type.DIRECT) {
                throw new RuntimeException("ProxySelector returned the wrong proxy for " + s);
            }
        }
        // Let's override the default nonProxyHosts and make sure we now get a
        // HTTP proxy
        System.setProperty("http.nonProxyHosts", "");
        for (String s : uris) {
            l = sel.select(new URI(s));
            if (l.size() == 1 && l.get(0).type() != Proxy.Type.HTTP) {
                throw new RuntimeException("ProxySelector returned the wrong proxy for " + s);
            }
        }
    }
}
