/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.io.*;
import java.net.*;
import sun.hotspot.WhiteBox;

public class Hello {
    public static void main(String args[]) throws Exception {
        String path = args[0];
        URL url = new File(path).toURI().toURL();
        URL[] urls = new URL[] {url};
        System.out.println(path);
        System.out.println(url);

        URLClassLoader urlClassLoader = new URLClassLoader(urls);
        Class c = urlClassLoader.loadClass("CustomLoadee");
        System.out.println(c);
        System.out.println(c.getClassLoader());

        // [1] Check that CustomLoadee is defined by the correct loader
        if (c.getClassLoader() != urlClassLoader) {
            throw new RuntimeException("c.getClassLoader() == " + c.getClassLoader() +
                                       ", expected == " + urlClassLoader);
        }

        // [2] Check that CustomLoadee is loaded from shared archive.
        WhiteBox wb = WhiteBox.getWhiteBox();
        if (wb.isSharedClass(Hello.class)) {
            if (!wb.isSharedClass(c)) {
                throw new RuntimeException("wb.isSharedClass(c) should be true");
            }
        }
    }
}
