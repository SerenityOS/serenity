/*
 * Copyright (c) 2004, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5105410
 * @run main/othervm B5105410
 * @summary ZipFile$ZipFileInputStream doesn't close handle to zipfile
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class B5105410 {
    public static void main (String[] args) throws Exception {
        setup();
        URL url = new URL("jar:file:./foo2.jar!/bar.txt");
        URLConnection urlc = url.openConnection();
        urlc.setUseCaches(false);
        InputStream is = urlc.getInputStream();
        is.read();
        is.close();
        File file = new File("foo2.jar");
        if (!file.delete()) {
            throw new RuntimeException("Could not delete foo2.jar");
        }
        if (file.exists()) {
            throw new RuntimeException("foo2.jar still exists");
        }
    }

    static void setup() throws IOException {
        Files.copy(Paths.get(System.getProperty("test.src"), "foo2.jar"),
                   Paths.get(".", "foo2.jar"), REPLACE_EXISTING);
    }
}

