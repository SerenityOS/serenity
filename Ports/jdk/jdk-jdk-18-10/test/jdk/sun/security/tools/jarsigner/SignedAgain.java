/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8215922
 * @summary jar spec is not precise when describing jar file re-signing
 * @library /test/lib
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.util.JarUtils;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static jdk.test.lib.SecurityTools.*;

public class SignedAgain {
    public static void main(String[] args) throws Exception {

        String opt = "-storepass changeit -keystore ks";

        keytool(opt + " -genkeypair -alias a -dname CN=A -keyalg RSA");
        keytool(opt + " -genkeypair -alias b -dname CN=B -keyalg RSA");

        JarUtils.createJar("a.jar", "f1");

        // as.jar: signed by a
        jarsigner(opt + " -signedjar as.jar a.jar a");

        JarUtils.updateJar("as.jar", "b.jar", "f2");

        // bs.jar: signed again by b
        jarsigner(opt + " -signedjar bs.jar b.jar b");

        // verified
        jarsigner(opt + " -verify -strict -verbose -certs bs.jar")
                .shouldHaveExitValue(0);

        try (JarFile ja = new JarFile("as.jar");
             JarFile jb = new JarFile("bs.jar");
             InputStream ma = ja.getInputStream(
                     new JarEntry("META-INF/MANIFEST.MF"));
             InputStream sa = jb.getInputStream(new JarEntry("META-INF/A.SF"));
             InputStream mb = jb.getInputStream(
                     new JarEntry("META-INF/MANIFEST.MF"));
             InputStream sb = jb.getInputStream(new JarEntry("META-INF/B.SF"))) {

            // Hash of manifest for 2 signed JAR files
            String da = Base64.getEncoder().encodeToString(MessageDigest
                    .getInstance("SHA-256").digest(ma.readAllBytes()));
            String db = Base64.getEncoder().encodeToString(MessageDigest
                    .getInstance("SHA-256").digest(mb.readAllBytes()));

            // They are not the same
            Asserts.assertNotEquals(da, db);

            // Digest-Manifest in A.SF matches da
            Asserts.assertEQ(new Manifest(sa).getMainAttributes()
                    .getValue("SHA-256-Digest-Manifest"), da);

            // Digest-Manifest in B.SF matches db
            Asserts.assertEQ(new Manifest(sb).getMainAttributes()
                    .getValue("SHA-256-Digest-Manifest"), db);
        }
    }
}
