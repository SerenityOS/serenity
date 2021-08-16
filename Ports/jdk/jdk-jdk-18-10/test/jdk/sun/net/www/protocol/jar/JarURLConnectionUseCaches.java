/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6947916
 * @summary  JarURLConnection does not handle useCaches correctly
 * @run main/othervm JarURLConnectionUseCaches
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class JarURLConnectionUseCaches {
    public static void main( String[] args ) throws IOException {
        JarOutputStream out = new JarOutputStream(
                new FileOutputStream("usecache.jar"));
        out.putNextEntry(new JarEntry("test.txt"));
        out.write("Test txt file".getBytes());
        out.closeEntry();
        out.close();

        URL url = new URL("jar:"
            + new File(".").toURI().toString()
            + "/usecache.jar!/test.txt");

        JarURLConnection c1 = (JarURLConnection)url.openConnection();
        c1.setDefaultUseCaches( false );
        c1.setUseCaches( true );
        c1.connect();

        JarURLConnection c2 = (JarURLConnection)url.openConnection();
        c2.setDefaultUseCaches( false );
        c2.setUseCaches( true );
        c2.connect();

        c1.getInputStream().close();
        c2.getInputStream().read();
        c2.getInputStream().close();
    }
}
