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
 * @bug 6827999
 * @run main/othervm B6827999
 * @summary Fix for 6827999
 */

import java.net.*;
import java.io.*;

/**
 */

public class B6827999
{
    public static void main(String[] args) throws Exception {

        URL[] urls = new URL[] {new URL("http://foobar.jar") };
        MyURLClassLoader ucl = new MyURLClassLoader(urls);

        ucl.addURL(new URL("http://foo/bar.jar"));
        urls = ucl.getURLs();

        if (urls.length != 2)
            throw new RuntimeException("Failed:(1)");
        ucl.close();

        ucl.addURL(new URL("http://foo.bar/bar.jar"));

        if (ucl.getURLs().length != 2) {
            throw new RuntimeException("Failed:(2)");
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
