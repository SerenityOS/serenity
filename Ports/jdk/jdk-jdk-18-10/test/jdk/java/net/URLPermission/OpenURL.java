/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8029354
 * @library /test/lib
 * @run main/othervm -Djava.security.manager=allow OpenURL
 */

import java.net.*;
import java.io.*;
import jdk.test.lib.net.URIBuilder;
import static java.net.Proxy.NO_PROXY;

public class OpenURL {

    public static void main (String[] args) throws Exception {

        System.setSecurityManager(new SecurityManager());

        try {
            URL url = URIBuilder.newBuilder()
                .scheme("http")
                .userInfo("joe")
                .loopback()
                .path("/a/b")
                .toURL();
            System.out.println("URL: " + url);
            HttpURLConnection urlc = (HttpURLConnection)url.openConnection(NO_PROXY);
            InputStream is = urlc.getInputStream();
            // error will throw exception other than SecurityException
        } catch (SecurityException e) {
            System.out.println("OK");
        }
    }
}
