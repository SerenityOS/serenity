/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6460701 6431651
 * @run main/othervm AddURLTest
 * @summary Fixes for 6460701 & 6431651.
 */

import java.net.*;
import java.io.*;

/**
 * 6460701 : URLClassLoader:addURL RI behavior inconsistent with a spec in case duplicate URLs
 * 6431651 : No description for addURL(URL url) method of URLClassLoader class in case null url
 */

public class AddURLTest
{
    public static void main(String[] args) throws Exception {

        URL[] urls = new URL[] {new URL("http://foobar.jar") };
        MyURLClassLoader ucl = new MyURLClassLoader(urls);

        ucl.addURL(null);
        ucl.addURL(new URL("http://foobar.jar"));
        ucl.addURL(null);
        ucl.addURL(new URL("http://foobar.jar"));
        ucl.addURL(null);
        ucl.addURL(new URL("http://foobar.jar"));

        urls = ucl.getURLs();

        if (urls.length != 1)
            throw new RuntimeException(
                "Failed: There should only be 1 url in the list of search URLs");

        URL url;
        for (int i=0; i<urls.length; i++) {
            url = urls[i];
            if (url == null || !url.equals(new URL("http://foobar.jar")))
                throw new RuntimeException(
                        "Failed: The url should not be null and should be http://foobar.jar");

        }
    }

    static class MyURLClassLoader extends URLClassLoader {
        public MyURLClassLoader(URL[] urls) {
            super(urls);
        }
        public void addURL(URL url) {
            super.addURL(url);
        }
    }

}
