/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import sun.hotspot.WhiteBox;

public class OldClassApp {
    public static void main(String args[]) throws Exception {
        String path = args[0];
        URL url = new File(path).toURI().toURL();
        URL[] urls = new URL[] {url};
        System.out.println(path);
        System.out.println(url);

        boolean inArchive = false;
        if (args[1].equals("true")) {
            inArchive = true;
        } else if (args[1].equals("false")) {
            inArchive = false;
        } else {
            throw new RuntimeException("args[1] can only be either \"true\" or \"false\", actual " + args[1]);
        }

        URLClassLoader urlClassLoader =
            new URLClassLoader("OldClassAppClassLoader", urls, null);

        for (int i = 2; i < args.length; i++) {
            Class c = urlClassLoader.loadClass(args[i]);
            System.out.println(c);
            System.out.println(c.getClassLoader());

            // [1] Check that class is defined by the correct loader
            if (c.getClassLoader() != urlClassLoader) {
                throw new RuntimeException("c.getClassLoader() == " + c.getClassLoader() +
                                           ", expected == " + urlClassLoader);
            }

            // [2] Check that class is loaded from shared static archive.
            if (inArchive) {
                WhiteBox wb = WhiteBox.getWhiteBox();
                if (wb.isSharedClass(OldClassApp.class)) {
                    if (!wb.isSharedClass(c)) {
                        throw new RuntimeException("wb.isSharedClass(c) should be true");
                    }
                }
            }
        }
    }
}
