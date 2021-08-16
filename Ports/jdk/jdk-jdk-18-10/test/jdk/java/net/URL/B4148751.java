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
 * @bug 4148751
 * @summary URL.sameFile return false on URL's, that are equal modulo url-encoding
 */

import java.net.*;

public class B4148751
{

    // unencoded parameters

    final static String scheme = "http";
    final static String auth = "web2.javasoft.com";
    final static String path = "/some file.html";
    final static String unencoded = "http://web2.javasoft.com/some file.html";

    // encoded URL / URI
    final static String encoded = "http://web2.javasoft.com/some%20file.html";

    public static void main(String args[]) throws URISyntaxException,
        MalformedURLException {

        URL url = null;
        URL url1 = null;

        try {
            url = new URL(unencoded);
            url1 = new URL(encoded);
        }
        catch(Exception e) {
            System.out.println("Unexpected exception :" + e);
            System.exit(-1);
        }

        if(url.sameFile(url1)) {
            throw new RuntimeException ("URL does not understand escaping");
        }

        /* check decoding of a URL */

        URI uri = url1.toURI();
        if (!uri.getPath().equals (path)) {
            throw new RuntimeException ("Got: " + uri.getPath() + " expected: " +
                path);
        }

        /* check encoding of a URL */

        URI uri1 = new URI (scheme, auth, path);
        url = uri.toURL();
        if (!url.toString().equals (encoded)) {
            throw new RuntimeException ("Got: " + url.toString() + " expected: " +
                encoded);
        }
    }
}
