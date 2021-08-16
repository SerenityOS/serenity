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
 * @test
 * @bug 8021788
 * @summary JarInputStream doesn't provide certificates for some file under META-INF
 * @modules java.base/sun.security.tools.keytool
 *          jdk.jartool/sun.security.tools.jarsigner
 */

import java.util.jar.*;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExtraFileInMetaInf {
    public static void main(String args[]) throws Exception {

        // Create a zip file with 2 entries
        try (ZipOutputStream zos =
                     new ZipOutputStream(new FileOutputStream("x.jar"))) {
            zos.putNextEntry(new ZipEntry("META-INF/SUB/file"));
            zos.write(new byte[10]);
            zos.putNextEntry(new ZipEntry("x"));
            zos.write(new byte[10]);
            zos.close();
        }

        // Sign it
        new File("ks").delete();
        sun.security.tools.keytool.Main.main(
                ("-keystore ks -storepass changeit -keypass changeit " +
                        "-keyalg rsa -alias a -dname CN=A -genkeypair").split(" "));
        sun.security.tools.jarsigner.Main.main(
                "-keystore ks -storepass changeit x.jar a".split(" "));

        // Check if the entries are signed
        try (JarInputStream jis =
                     new JarInputStream(new FileInputStream("x.jar"))) {
            JarEntry je;
            while ((je = jis.getNextJarEntry()) != null) {
                String name = je.toString();
                if (name.equals("META-INF/SUB/file") || name.equals("x")) {
                    while (jis.read(new byte[1000]) >= 0);
                    if (je.getCertificates() == null) {
                        throw new Exception(name + " not signed");
                    }
                }
            }
        }
    }
}
