/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4064962 8202708
 * @summary openStream should work even when not using proxies and
 *          UnknownHostException is thrown as expected.
 */

import java.io.*;
import java.net.*;


public class OpenStream {

    private static final String badHttp = "http://foo.bar.baz/";
    private static final String badUnc = "file://h7qbp368oix47/not-exist.txt";

    public static void main(String[] args) throws IOException {
        testHttp();
        testUnc();
    }

    static void testHttp() throws IOException {
        checkThrows(badHttp);
    }

    static void testUnc() throws IOException {
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        if (isWindows) {
            checkThrows(badUnc);
        }
    }

    static void checkThrows(String url) throws IOException {
        URL u = new URL(url);
        try {
            InputStream in = u.openConnection(Proxy.NO_PROXY).getInputStream();
        } catch (UnknownHostException x) {
            System.out.println("UnknownHostException is thrown as expected.");
            return;
        }
        throw new RuntimeException("Expected UnknownHostException to be " +
                "thrown for " + url);

    }
}

