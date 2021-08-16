/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4709003
 * @summary incompatibility between jdk 1.3 and jdk1.4 for URL(URL context, String spec)
 */

import java.net.URI;
import java.net.URL;
public class RelativePath {
    public static void main(String[] args) throws Exception {
        String uri1 = "http://h/../d1/";
        String uri2 = "../d/i.htm";
        String expected = "http://h/../d/i.htm";

        URI uri = new URI(uri1);
        String s1 = uri.resolve(uri2).toString();
        URL url = new URL(uri1);
        URL url2 = new URL(url, uri2);
        String s2 = url2.toString();
        if (!(expected.equalsIgnoreCase(s1)))
            throw new RuntimeException("URI.resolve didn't return expected result [" + s1 + " versus " + expected + "]");
        if (!(expected.equalsIgnoreCase(s2)))
            throw new RuntimeException("URL(url, String) didn't return expected result [" + s2 + " versus " + expected + "]");

    }
}
