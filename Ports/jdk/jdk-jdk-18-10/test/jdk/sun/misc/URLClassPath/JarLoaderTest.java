/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright (c) 2013 IBM Corporation
 */

/* @test
 * @bug 7183373
 * @summary URLClassLoader fails to close handles to Jar files opened during
 *          getResource()
 */

import java.io.*;
import java.net.*;
import java.util.zip.*;

public class JarLoaderTest {
    public static void main(String[] args) throws Exception {
        // Create a JAR file
        File f = new File("urlcl" + 1 + ".jar");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(f));

        // add a file
        zos.putNextEntry(new ZipEntry("TestResource"));
        byte[] b = "This is a test resource".getBytes();
        zos.write(b, 0, b.length);
        zos.close();

        // Load the file using cl.getResource()
        URLClassLoader cl = new URLClassLoader(new URL[] { new URL("jar:" +
            f.toURI().toURL() + "!/")}, null);
        cl.getResource("TestResource");

        // Close the class loader - this should free up all of its Closeables,
        // including the JAR file
        cl.close();

        // Try to delete the JAR file
        f.delete();

        // Check to see if the file was deleted
        if (f.exists()) {
            System.out.println(
                "Test FAILED: Closeables failed to close handle to jar file");
            // Delete the jar using a workaround
            for (URL u : cl.getURLs()) {
                if (u.getProtocol().equals("jar")) {
                    ((JarURLConnection)u.openConnection()).getJarFile().close();
                }
                f.delete();
            }
            throw new RuntimeException("File could not be deleted");
        } else {
            System.out.println("Test PASSED");
        }
    }
}
