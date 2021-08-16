/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6712755
 * @summary jarsigner fails to sign itextasian.jar since 1.5.0_b14,
 *          it works with 1.5.0_13
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;

import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class EmptyManifest {

    public static void main(String[] args) throws Exception {

        try (FileOutputStream fos = new FileOutputStream("em.jar");
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            zos.write(new byte[]{'\r', '\n'});
            zos.putNextEntry(new ZipEntry("A"));
            zos.write(new byte[10]);
            zos.putNextEntry(new ZipEntry("B"));
            zos.write(new byte[0]);
        }

        SecurityTools.keytool("-keystore ks -storepass changeit "
                + "-keypass changeit -alias a -dname CN=a -keyalg rsa "
                + "-genkey -validity 300");

        SecurityTools.jarsigner("-keystore ks -storepass changeit em.jar a")
                .shouldHaveExitValue(0);
        SecurityTools.jarsigner("-keystore ks -storepass changeit -verify "
                + "-debug -strict em.jar")
                .shouldHaveExitValue(0);
    }
}
